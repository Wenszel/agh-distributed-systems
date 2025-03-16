package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import static org.example.Server.SERVER_HOSTNAME;
import static org.example.Server.SERVER_PORT;

public class Client {
    private static SocketState state = SocketState.TCP;
    private enum SocketState {
        UDP, TCP
    }
    public static void main(String[] args) throws SocketException {
        Scanner scanner = new Scanner(System.in);
        DatagramSocket datagramSocket = new DatagramSocket();
        try (Socket serverSocket = new Socket(SERVER_HOSTNAME, SERVER_PORT)) {
            TCPSocketHandler TCPSocketHandler = new TCPSocketHandler(serverSocket);
            setUsername(scanner, TCPSocketHandler);
            listenToNewMessagesFromServer(TCPSocketHandler);
            listenToUserNewMessages(scanner, TCPSocketHandler, datagramSocket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listenToUserNewMessages(Scanner scanner, TCPSocketHandler TCPSocketHandler, DatagramSocket datagramSocket) throws IOException {
        while (true) {
            String message = scanner.nextLine();
            if (message.equals("U")) {
                System.out.println("Switched to UDP communication");
                state = SocketState.UDP;
                continue;
            }
            if (state.equals(SocketState.TCP)) TCPSocketHandler.send(message);
            else {
                byte[] buff = message.getBytes();
                DatagramPacket dp = new DatagramPacket(buff, buff.length);
                datagramSocket.send(dp);
                state = SocketState.TCP;
            }
            if (message.equals(":wq")) break;
        }
    }

    private static void listenToNewMessagesFromServer(TCPSocketHandler TCPSocketHandler) {
        new Thread(() -> {
            try {
                while(true) System.out.println(TCPSocketHandler.receive());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private static void setUsername(Scanner scanner, TCPSocketHandler TCPSocketHandler) throws IOException {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        TCPSocketHandler.send(username);
    }
}