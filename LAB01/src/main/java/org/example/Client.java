package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static org.example.Server.SERVER_HOSTNAME;
import static org.example.Server.SERVER_PORT;

public class Client {

    public static void main(String[] args) {

        try (Socket serverSocket = new Socket(SERVER_HOSTNAME, SERVER_PORT)) {
            while(true) {
                SocketHandler socketHandler = new SocketHandler(serverSocket);
                Thread receiver = new Thread(() -> {
                    try {
                        while(true) System.out.println(socketHandler.receive());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                receiver.start();
                Scanner scanner = new Scanner(System.in);
                String message = scanner.nextLine();
                socketHandler.send(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
