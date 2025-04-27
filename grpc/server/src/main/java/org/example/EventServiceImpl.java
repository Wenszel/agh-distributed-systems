package org.example;

import io.grpc.stub.StreamObserver;
import subscription.Subscription;
import subscription.EventServiceGrpc;

import java.util.*;
import java.util.concurrent.*;

public class EventServiceImpl extends EventServiceGrpc.EventServiceImplBase {
    private final Map<String, SubscriptionData> activeSubscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService eventGeneratorExecutor = Executors.newSingleThreadScheduledExecutor();

    static class SubscriptionData {
        Queue<Subscription.EventNotification> buffer = new ConcurrentLinkedQueue<>();
        Set<String> cities;
        Set<Subscription.EventType> eventTypes;
        volatile StreamObserver<Subscription.EventNotification> observer;

        SubscriptionData(Set<String> cities, Set<Subscription.EventType> eventTypes) {
            this.cities = cities;
            this.eventTypes = eventTypes;
        }
    }

    public EventServiceImpl() {
        eventGeneratorExecutor.scheduleAtFixedRate(this::generateAndNotifyEvent, 0, 3, TimeUnit.SECONDS);
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
        List<String> participants = List.of("John Doe", "Jane Smith", "Max Mustermann");

        long timestamp = System.currentTimeMillis();

        return Subscription.EventNotification.newBuilder()
                .setCity(city)
                .setEventType(eventType)
                .addAllParticipants(participants)
                .setTimestamp(timestamp)
                .build();
    }
}
