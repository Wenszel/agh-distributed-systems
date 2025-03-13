package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOSTNAME = "localhost";
    public static final Set<SocketHandler> clientsSockets = new HashSet<>();
    public static final Map<SocketHandler, Thread> clientsThreads = new HashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("TCP server is listening on port: " + SERVER_PORT);
            while (true) {
                Socket clientSocket = acceptClient(serverSocket);

                SocketHandler socketHandler = new SocketHandler(clientSocket);
                clientsSockets.add(socketHandler);

                runChatThreadForClient(socketHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runChatThreadForClient(SocketHandler socketHandler) {
        Thread chatThread = new ChatThread(socketHandler, clientsSockets);
        clientsThreads.put(socketHandler, chatThread);
        chatThread.start();
    }


    private static Socket acceptClient(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected");
        return clientSocket;
    }
}