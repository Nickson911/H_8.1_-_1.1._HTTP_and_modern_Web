package org.example;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(9999, 64);
        server.addHandler("GET", "/classic.html", (request, out) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                System.out.println("classic.html: " + request.getPath());
                final var template = Files.readString(filePath);
                final var content = template.replace("{time}",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                        .getBytes();
                outWrite(mimeType, content, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.addHandler("POST", "/events.html", (request, out) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                System.out.println("events.html: " + request.getPath());
                final var content = Files.readAllBytes(filePath);
                outWrite(mimeType, content, out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.start();
    }

    private static void outWrite(String mimeType, byte[] content, BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.write(content);
        out.flush();
    }
}
