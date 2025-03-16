package org.example;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Server {
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOSTNAME = "localhost";
    public static final HashMap<TCPSocketHandler, String> clients = new HashMap<>();
    public static final Map<TCPSocketHandler, Thread> clientsThreads = new HashMap<>();


    public static void main(String[] args) {
        Thread UDPServerThread = new Thread(()-> {
            try (DatagramSocket datagramSocket = new DatagramSocket(SERVER_PORT)) {
                System.out.println("UDP server is listening on port: " + SERVER_PORT);
                byte[] receiveBuffer = new byte[1024];
                Arrays.fill(receiveBuffer, (byte)0);
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                datagramSocket.receive(receivePacket);
                String msg = new String(receivePacket.getData());
                System.out.println("received msg: " + msg);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        Thread TCPServerThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
                System.out.println("TCP server is listening on port: " + SERVER_PORT);
                while (true) {
                    Socket clientSocket = acceptClient(serverSocket);
                    TCPSocketHandler TCPSocketHandler = new TCPSocketHandler(clientSocket);
                    String username = TCPSocketHandler.receive();
                    clients.put(TCPSocketHandler, username);
                    runChatThreadForClient(TCPSocketHandler);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        TCPServerThread.start();
        UDPServerThread.start();
    }


    private static void runChatThreadForClient(TCPSocketHandler TCPSocketHandler) {
        Thread chatThread = new ChatThread(TCPSocketHandler, clients);
        clientsThreads.put(TCPSocketHandler, chatThread);
        chatThread.start();
    }


    private static Socket acceptClient(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected");
        return clientSocket;
    }
}