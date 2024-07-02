package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class Request {

    private final String method;
    private final InputStream body;
    private final String path;
    private final Map<String, String> headers;

    private Request(String method, InputStream body, String path, Map<String, String> headers) {
        this.method = method;
        this.body = body;
        this.path = path;
        this.headers = headers;
    }

    public static Request fromInputStream(InputStream in) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(in));

        final var requestLine = reader.readLine();
        final var list = requestLine.split(" ");

        if (list.length != 3) {
            throw new IOException("Error!!!");
        }

        var method = list[0];
        var path = list[1];

        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while (!(headerLine = reader.readLine()).equals("")) {
            var i = headerLine.indexOf(":");
            var key = headerLine.substring(0, i);
            var value = headerLine.substring(i + 2);
            headers.put(key, value);
        }
        return new Request(method, in, path, headers);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public InputStream getBody() {
        return body;
    }

    public String getPath() {
        return path;
    }
}
