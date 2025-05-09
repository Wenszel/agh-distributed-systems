package org.example;

import io.grpc.stub.StreamObserver;
import subscription.Subscription;
import subscription.EventServiceGrpc;

import java.util.*;
import java.util.concurrent.*;

public class EventServiceImpl extends EventServiceGrpc.EventServiceImplBase {
    private final Map<String, SubscriptionData> activeSubscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService eventGeneratorExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService subscriptionCleanerExecutor = Executors.newSingleThreadScheduledExecutor();

    static class SubscriptionData {
        Queue<Subscription.EventNotification> buffer = new ConcurrentLinkedQueue<>();
        Set<String> cities;
        Set<Subscription.EventType> eventTypes;
        volatile StreamObserver<Subscription.EventNotification> observer;
        volatile long lastActiveTime;

        SubscriptionData(Set<String> cities, Set<Subscription.EventType> eventTypes) {
            this.cities = cities;
            this.eventTypes = eventTypes;
            this.lastActiveTime = System.currentTimeMillis();
        }

        void updateLastActiveTime() {
            this.lastActiveTime = System.currentTimeMillis();
        }
    }

    public EventServiceImpl() {
        eventGeneratorExecutor.scheduleAtFixedRate(this::generateAndNotifyEvent, 0, 3, TimeUnit.SECONDS);
        subscriptionCleanerExecutor.scheduleAtFixedRate(this::cleanUpInactiveSubscriptions, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void subscribe(Subscription.SubscriptionRequest request, StreamObserver<Subscription.EventNotification> responseObserver) {
        String subscriptionId = UUID.randomUUID().toString();

        SubscriptionData subscriptionData = new SubscriptionData(
                new HashSet<>(request.getCitiesList()),
                new HashSet<>(request.getEventTypesList())
        );
        subscriptionData.observer = responseObserver;

        activeSubscriptions.put(subscriptionId, subscriptionData);

        System.out.println("New subscription: " + subscriptionId);
    }

    @Override
    public void unsubscribe(Subscription.UnsubscribeRequest request, StreamObserver<Subscription.UnsubscribeResponse> responseObserver) {
        String subscriptionId = request.getSubscriptionId();
        SubscriptionData subscriptionData = activeSubscriptions.remove(subscriptionId);

        if (subscriptionData != null && subscriptionData.observer != null) {
            subscriptionData.observer.onCompleted();
        }

        responseObserver.onNext(Subscription.UnsubscribeResponse.newBuilder().setSuccess(true).build());
        responseObserver.onCompleted();

        System.out.println("Unsubscribed: " + subscriptionId);
    }

    private void generateAndNotifyEvent() {
        Subscription.EventNotification event = generateEvent();
        System.out.println("Generated event: " + event.getCity() + " " + event.getEventType().getNumber());

        for (SubscriptionData subscriptionData : activeSubscriptions.values()) {
            sendEvent(subscriptionData, event);
        }
    }

    private void sendEvent(SubscriptionData subscriptionData, Subscription.EventNotification event) {
        if (subscriptionData.cities.contains(event.getCity()) &&
                subscriptionData.eventTypes.contains(event.getEventType())) {
            if (subscriptionData.observer != null) {
                subscriptionData.observer.onNext(event);
                subscriptionData.updateLastActiveTime();
            } else {
                subscriptionData.buffer.add(event);
            }
        }
    }

    private Subscription.EventNotification generateEvent() {
        List<String> cities = List.of("Kraków", "Warszawa");
        List<Subscription.EventType> eventTypes = List.of(
                Subscription.EventType.SPORTS,
                Subscription.EventType.MUSIC,
                Subscription.EventType.WEATHER
        );
        Random random = new Random();

        String city = cities.get(random.nextInt(cities.size()));
        Subscription.EventType eventType = eventTypes.get(random.nextInt(eventTypes.size()));

        List<String> participantNames = List.of("John Doe", "Jane Smith", "Max Mustermann");

        List<Subscription.Participant> participants = new ArrayList<>();
        for (String name : participantNames) {
            participants.add(Subscription.Participant.newBuilder()
                    .setName(name)
                    .build());
        }

        long timestamp = System.currentTimeMillis();

        return Subscription.EventNotification.newBuilder()
                .setCity(city)
                .setEventType(eventType)
                .addAllParticipants(participants)
                .setTimestamp(timestamp)
                .build();
    }

    private void cleanUpInactiveSubscriptions() {
        long currentTime = System.currentTimeMillis();
        long inactiveThreshold = 1 * 60 * 1000;

        Iterator<Map.Entry<String, SubscriptionData>> iterator = activeSubscriptions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, SubscriptionData> entry = iterator.next();
            SubscriptionData subscriptionData = entry.getValue();
            if (currentTime - subscriptionData.lastActiveTime > inactiveThreshold) {
                System.out.println("Cleaning up inactive subscription: " + entry.getKey());
                iterator.remove();
                if (subscriptionData.observer != null) {
                    subscriptionData.observer.onCompleted();
                }
            }
        }
    }
}
