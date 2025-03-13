package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int PORT = 8080;
    public static final String STOP = "STOP";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("TCP server is listening on port: " + PORT);
            while (true) {
                Socket clientSocket = acceptClient(serverSocket);
                sendToClient(clientSocket, "Hello world from server");
                String messageFromClient = receiveFromClient(clientSocket);
                if (checkIfStop(messageFromClient)) break;
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

    private static void sendToClient(Socket clientSocket, String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
        printWriter.println(message);
    }

    private static String receiveFromClient(Socket clientSocket) throws IOException {
        BufferedReader clientMessagesReader= new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String message = clientMessagesReader.readLine();
        System.out.println("received msg: " + message);
        return message;
    }

    private static boolean checkIfStop(String message) {
        return message.equals(STOP);
    }
}