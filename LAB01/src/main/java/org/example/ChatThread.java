package org.example;

import java.io.IOException;
import java.util.Map;

public class ChatThread extends Thread {
    private final TCPSocketHandler TCPSocketHandler;
    private final Map<TCPSocketHandler, String> clients;

    public ChatThread(TCPSocketHandler TCPSocketHandler, Map<TCPSocketHandler, String> clients) {
        this.TCPSocketHandler = TCPSocketHandler;
        this.clients = clients;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String messageFromClient = TCPSocketHandler.receive();
                System.out.println(messageFromClient);
                sendToClients(messageFromClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void sendToClients(String messageFromClient) {
        clients.keySet().forEach((TCPSocketHandler) -> {
            try {
                if (!TCPSocketHandler.equals(this.TCPSocketHandler)) TCPSocketHandler.send(clients.get(this.TCPSocketHandler) +  ">> " + messageFromClient);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}