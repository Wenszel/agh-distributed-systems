package org.example;

import java.io.IOException;
import java.util.Set;

public class ChatThread extends Thread {
    private final SocketHandler socketHandler;
    private final Set<SocketHandler> clients;

    public ChatThread(SocketHandler socketHandler, Set<SocketHandler> clients) {
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
        clients.forEach((socketHandler) -> {
            try {
                if (!socketHandler.equals(this.socketHandler))
                socketHandler.send(messageFromClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}