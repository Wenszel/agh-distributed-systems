package org.example;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import static org.example.Server.SERVER_HOSTNAME;
import static org.example.Server.SERVER_PORT;

public class Client {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try (Socket serverSocket = new Socket(SERVER_HOSTNAME, SERVER_PORT)) {
            SocketHandler socketHandler = new SocketHandler(serverSocket);
            setUsername(scanner, socketHandler);
            listenToNewMessagesFromServer(socketHandler);
            listenToUserNewMessages(scanner, socketHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listenToUserNewMessages(Scanner scanner, SocketHandler socketHandler) throws IOException {
        while (true) {
            String message = scanner.nextLine();
            socketHandler.send(message);
            if (message.equals(":wq")) break;
        }
    }

    private static void listenToNewMessagesFromServer(SocketHandler socketHandler) {
        new Thread(() -> {
            try {
                while(true) System.out.println(socketHandler.receive());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void setUsername(Scanner scanner, SocketHandler socketHandler) throws IOException {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        socketHandler.send(username);
    }
}