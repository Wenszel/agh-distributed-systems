package org.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class Main{
    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50051)
                .addService(new EventServiceImpl())
                .build();

        server.start();
        System.out.println("Server started");
        server.awaitTermination();
    }
}
