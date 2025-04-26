package org.example;

import io.grpc.stub.StreamObserver;
import subscription.EventServiceGrpc;
import subscription.Subscription;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventServiceImpl extends EventServiceGrpc.EventServiceImplBase {
    @Override
    public void subscribe(Subscription.SubscriptionRequest request, StreamObserver<Subscription.EventNotification> responseObserver) {
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            Subscription.EventNotification notification = Subscription.EventNotification.newBuilder()
                    .setCity("Warsaw")
                    .setEventType(Subscription.EventType.MUSIC)
                    .setDescription("Concert in Warsaw!")
                    .addAllParticipants(Arrays.asList("Band A", "Band B"))
                    .setTimestamp(System.currentTimeMillis())
                    .build();
            System.out.println(request.getCitiesList());

            if (request.getCitiesList().contains(notification.getCity()) &&
                    request.getEventTypesList().contains(notification.getEventType())) {
                responseObserver.onNext(notification);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }
}