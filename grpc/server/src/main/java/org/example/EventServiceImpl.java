package org.example;

import io.grpc.stub.StreamObserver;
import subscription.Subscription;
import subscription.EventServiceGrpc;

import java.util.*;
import java.util.concurrent.*;

public class EventServiceImpl extends EventServiceGrpc.EventServiceImplBase {
    private final Map<String, SubscriptionData> activeSubscriptions = new ConcurrentHashMap<>();

    static class SubscriptionData {
        Queue<Subscription.EventNotification> buffer = new ConcurrentLinkedQueue<>();
        Set<String> cities;
        Set<Subscription.EventType> eventTypes;
        volatile StreamObserver<Subscription.EventNotification> observer;
        ScheduledExecutorService executor;

        SubscriptionData(Set<String> cities, Set<Subscription.EventType> eventTypes, ScheduledExecutorService executor) {
            this.cities = cities;
            this.eventTypes = eventTypes;
            this.executor = executor;
        }
    }

    @Override
    public void subscribe(Subscription.SubscriptionRequest request, StreamObserver<Subscription.EventNotification> responseObserver) {
        String subscriptionId = UUID.randomUUID().toString();

        SubscriptionData subscriptionData = new SubscriptionData(
                new HashSet<>(request.getCitiesList()),
                new HashSet<>(request.getEventTypesList()),
                Executors.newSingleThreadScheduledExecutor()
        );
        subscriptionData.observer = responseObserver;

        activeSubscriptions.put(subscriptionId, subscriptionData);

        EventGenerator eventGenerator = new EventGenerator(event -> sendEvent(subscriptionData, event));
        eventGenerator.start();

        subscriptionData.executor.scheduleAtFixedRate(() -> {
            Subscription.EventNotification notification = generateEvent();
            sendEvent(subscriptionData, notification);
        }, 0, 3, TimeUnit.SECONDS);
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

    private Subscription.EventNotification generateEvent() {
        List<String> cities = List.of("Kraków", "Warszawa", "Gdańsk", "Poznań", "Wrocław");
        List<Subscription.EventType> eventTypes = List.of(
                Subscription.EventType.SPORTS,
                Subscription.EventType.MUSIC,
                Subscription.EventType.WEATHER
        );
        Random random = new Random();

        String city = cities.get(random.nextInt(cities.size()));
        Subscription.EventType eventType = eventTypes.get(random.nextInt(eventTypes.size()));

        return Subscription.EventNotification.newBuilder()
                .setCity(city)
                .setEventType(eventType)
                .build();
    }
}
