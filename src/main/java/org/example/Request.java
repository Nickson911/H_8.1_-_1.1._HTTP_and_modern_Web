package org.example;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final InputStream in;
    private final MultiMap queryParams;
    private final MultiMap postParams;
    private final static String GET = "GET";
    private final static String POST = "POST";

    private Request(String method, String path, List<String> headers, MultiMap queryParams,
                    MultiMap postParams, InputStream in) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
        this.postParams = postParams;
        this.in = in;
    }

    public static Request createRequest(InputStream inputStream, BufferedOutputStream out) throws IOException, URISyntaxException {
        final var allowedMethods = List.of(GET, POST);
        final var limit = 4096;
        final var in = new BufferedInputStream(inputStream);

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequest(out);
        }

        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest(out);
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest(out);
        }

        final var pathWithQuery = requestLine[1];
        if (!pathWithQuery.startsWith("/")) {
            badRequest(out);
        }
        final String path;
        final MultiMap query;

        if (pathWithQuery.contains("?")) {
            String[] value = pathWithQuery.split("\\?");
            path = value[0];
            String queryLine = value[1];
            query = getQueryParams(queryLine);
        } else {
            path = pathWithQuery;
            query = null;
        }

        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequest(out);
            return null;
        }

        in.reset();

        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        MultiMap post = null;
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                final var body = new String(bodyBytes);
                System.out.println(body);

                if (body.contains("=")) {
                    post = getQueryParams(body);
                }
            }
        }
        return new Request(method, path, headers, query, post, in);
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    public static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static MultiMap getQueryParams(String url) {
        MultiMap parameter = new MultiValueMap();
        var params = URLEncodedUtils.parse(url, Charset.defaultCharset(), '&');
        for (NameValuePair param : params) {
            if (param.getName() != null && param.getValue() != null)
                parameter.put(param.getName(), param.getValue());
        }
        return parameter;
    }

    public InputStream getIn() {
        return in;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
