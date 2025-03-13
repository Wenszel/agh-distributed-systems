package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketHandler {
    private final Socket socket;

    public SocketHandler(Socket socket) {
        this.socket = socket;
    }

    public void send(String message) throws IOException {
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println(message);
    }

    public String receive() throws IOException {
        BufferedReader messageReader= new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return messageReader.readLine();
    }
}
