package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
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
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>());
        handlers.get(method).put(path, handler);
    }

    public void connectionProcessing(Socket socket){
        try (socket;
             final var in = socket.getInputStream();
             final var out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final var requestLine = Request.createRequest(in, out);
            assert requestLine != null;
            final var paths = handlers.get(requestLine.getMethod());
            if (paths == null) {
                Request.badRequest(out);
                return;
            }
            final var handler = paths.get(requestLine.getPath());
            if (handler == null) {
                Request.badRequest(out);
                return;
            }
            handler.handle(requestLine,out);
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}