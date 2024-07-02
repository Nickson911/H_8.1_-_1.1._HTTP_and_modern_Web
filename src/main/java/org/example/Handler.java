package org.example;

import java.io.BufferedOutputStream;

public interface Handler {
    void handler(Request request, BufferedOutputStream out);
}
