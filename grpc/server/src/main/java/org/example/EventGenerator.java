package org.example;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import subscription.Subscription;

public class EventGenerator {

    private final Random random = new Random();
    private final List<String> cities = List.of("Kraków", "Warszawa", "Gdańsk", "Poznań", "Wrocław");
    private final List<Subscription.EventType> eventTypes = List.of(
            Subscription.EventType.SPORTS, 
            Subscription.EventType.MUSIC, 
            Subscription.EventType.WEATHER);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private EventListener listener;

    public interface EventListener {
        void onNewEvent(Subscription.EventNotification event);
    }

    public EventGenerator(EventListener listener) {
        this.listener = listener;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::generateEvent, 0, 2, TimeUnit.SECONDS); 
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void generateEvent() {
        String city = cities.get(random.nextInt(cities.size()));
        Subscription.EventType eventType = eventTypes.get(random.nextInt(eventTypes.size()));
        String eventId = UUID.randomUUID().toString();

        Subscription.EventNotification event = Subscription.EventNotification.newBuilder()
                .setCity(city)
                .setEventType(eventType)
                .build();

        listener.onNewEvent(event);
    }
}
