package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOSTNAME = "localhost";
    public static final String GREETINGS_MESSAGE = "Hello world";
    public static final String STOP = "STOP";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("TCP server is listening on port: " + SERVER_PORT);
            while (true) {
                Socket clientSocket = acceptClient(serverSocket);
                SocketHandler socketHandler = new SocketHandler(clientSocket);
                socketHandler.send(GREETINGS_MESSAGE);
                String messageFromClient = socketHandler.receive();
                if (checkIfStop(messageFromClient)) break;
                else System.out.println(messageFromClient);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Socket acceptClient(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected");
        return clientSocket;
    }

    private static boolean checkIfStop(String message) {
        return message.equals(STOP);
    }
}