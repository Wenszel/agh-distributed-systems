package org.example;

import java.io.IOException;
import java.util.Map;

public class ChatThread extends Thread {
    private final SocketHandler socketHandler;
    private final Map<SocketHandler, String> clients;

    public ChatThread(SocketHandler socketHandler, Map<SocketHandler, String> clients) {
        this.socketHandler = socketHandler;
        this.clients = clients;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String messageFromClient = socketHandler.receive();
                System.out.println(messageFromClient);
                sendToClients(messageFromClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendToClients(String messageFromClient) {
        clients.keySet().forEach((socketHandler) -> {
            try {
                if (!socketHandler.equals(this.socketHandler)) socketHandler.send(clients.get(socketHandler) +  ">> " + messageFromClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}