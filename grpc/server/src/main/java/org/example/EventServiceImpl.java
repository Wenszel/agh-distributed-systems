package org.example;

import io.grpc.stub.StreamObserver;
import subscription.EventServiceGrpc;
import subscription.Subscription;

import java.util.*;
import java.util.concurrent.*;

public class EventServiceImpl extends EventServiceGrpc.EventServiceImplBase {

    static class SubscriptionData {
        StreamObserver<Subscription.EventNotification> observer;
        Set<String> cities;
        Set<Subscription.EventType> eventTypes;
        ScheduledExecutorService executor;

        SubscriptionData(StreamObserver<Subscription.EventNotification> observer, Set<String> cities, Set<Subscription.EventType> eventTypes, ScheduledExecutorService executor) {
            this.observer = observer;
            this.cities = cities;
            this.eventTypes = eventTypes;
            this.executor = executor;
        }
    }

    private final Map<String, SubscriptionData> activeSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void subscribe(Subscription.SubscriptionRequest request, StreamObserver<Subscription.EventNotification> responseObserver) {
        String subscriptionId = UUID.randomUUID().toString();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        SubscriptionData subscriptionData = new SubscriptionData(
                responseObserver,
                new HashSet<>(request.getCitiesList()),
                new HashSet<>(request.getEventTypesList()),
                executor
        );

        activeSubscriptions.put(subscriptionId, subscriptionData);

        executor.scheduleAtFixedRate(() -> {
            Subscription.EventNotification notification = Subscription.EventNotification.newBuilder()
                    .setCity("Warsaw")
                    .setEventType(Subscription.EventType.MUSIC)
                    .setDescription("Concert in Warsaw!")
                    .addAllParticipants(Arrays.asList("Band A", "Band B"))
                    .setTimestamp(System.currentTimeMillis())
                    .build();

            if (subscriptionData.cities.contains(notification.getCity()) &&
                    subscriptionData.eventTypes.contains(notification.getEventType())) {
                try {
                    subscriptionData.observer.onNext(notification);
                } catch (Exception e) {
                    System.err.println("Failed to send notification: " + e.getMessage());
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void unsubscribe(Subscription.UnsubscribeRequest request, StreamObserver<Subscription.UnsubscribeResponse> responseObserver) {
        String subscriptionId = request.getSubscriptionId();
        SubscriptionData subscriptionData = activeSubscriptions.get(subscriptionId);

        var responseBuilder = Subscription.UnsubscribeResponse.newBuilder();

        if (subscriptionData != null) {
            request.getCitiesList().forEach(subscriptionData.cities::remove);
            request.getEventTypesList().forEach(subscriptionData.eventTypes::remove);

            if (subscriptionData.cities.isEmpty() && subscriptionData.eventTypes.isEmpty()) {
                subscriptionData.executor.shutdown();
                subscriptionData.observer.onCompleted();
                activeSubscriptions.remove(subscriptionId);
            }

            responseBuilder.setSuccess(true);
        } else {
            responseBuilder.setSuccess(false);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
