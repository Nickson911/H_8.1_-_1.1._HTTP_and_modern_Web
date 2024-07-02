package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    Map<String,Map<String,Handler>> handlers;
    ExecutorService executorService;

    public Server(int port, int sizePool) {
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(sizePool);
        this.handlers = new ConcurrentHashMap<>();
    }

    private final Handler notFound = (request, out) -> {
        try {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    };

    public void start() {
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                executorService.submit(()-> connectionProcessing(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler){
        if (handlers.get(method) == null) {
            handlers.put(method, new ConcurrentHashMap<>());
        }
        handlers.get(method).put(path, handler);
    }

    public void connectionProcessing(Socket socket){
        try (socket;
             final var in = socket.getInputStream();
             final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestLine = Request.fromInputStream(in);
            final var paths = handlers.get(requestLine.getMethod());
            if (paths == null) {
                notFound.handler(requestLine,out);
                return;
            }
            final var handler = paths.get(requestLine.getPath());
            if (handler == null) {
                notFound.handler(requestLine,out);
                return;
            }
            handler.handler(requestLine,out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}