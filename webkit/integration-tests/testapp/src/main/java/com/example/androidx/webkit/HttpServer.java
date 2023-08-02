/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidx.webkit;

import static java.nio.charset.StandardCharsets.UTF_8;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple HTTP server that accepts requests.
 * Can be used to test proxying or as a end destination server.
 */
public class HttpServer {

    /**
     * This interface offers a callback that is run when a request is served.
     */
    public interface OnRequestCallback {

        /**
         * Called when this proxy serves a request.
         */
        void onRequestServed();
    }

    /**
     * Provider of request handlers.
     *
     * Typically just a static reference to either {@code HttpServer.EchoRequestHandler::new} or
     * {@code HttpServer.ProxyRequestHandler::new}
     */
    public interface RequestHandlerFactory {
        /**
         * Called to initialize a new RequestHandler
         */
        @NonNull
        RequestHandler create(@NonNull Socket socket);
    }

    private static final int TIMEOUT_MILLIS = 5000;
    private boolean mRunning = false;
    private OnRequestCallback mCallback;
    private RequestHandlerFactory mHandlerFactory;
    private ServerSocket mServerSocket;
    private List<Thread> mThreadsList;
    private int mRequestCount;

    /**
     * Create a server using provided port number.
     *
     * @param port port number
     * @param handlerFactory Provider of request handler
     * @param callback callback run when this a request is served
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public HttpServer(int port,
            @NonNull RequestHandlerFactory handlerFactory,
            @Nullable OnRequestCallback callback) {
        mRequestCount = 0;
        mCallback = callback;
        mHandlerFactory = handlerFactory;
        mThreadsList = new ArrayList<>();
        try {
            mServerSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get port number.
     */
    public int getPort() {
        return mServerSocket.getLocalPort();
    }

    /**
     * Get the number of requests this server has served.
     */
    public int getRequestCount() {
        return mRequestCount;
    }

    /**
     * Start listening for requests.
     */
    public void start() {
        if (mRunning) return;
        mRunning = true;
        new Thread(() -> {
            while (mRunning) {
                listen();
            }
        }).start();
    }

    @SuppressWarnings("CatchAndPrintStackTrace")
    private void listen() {
        try {
            Socket socket = mServerSocket.accept();
            mRequestCount++;
            if (mCallback != null) {
                mCallback.onRequestServed();
            }
            Thread thread = new Thread(mHandlerFactory.create(socket));
            mThreadsList.add(thread);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdown.
     */
    @SuppressWarnings("CatchAndPrintStackTrace")
    public void shutdown() {
        if (!mRunning) return;
        mRunning = false;
        for (Thread thread : mThreadsList) {
            if (thread.isAlive()) {
                try {
                    thread.join(TIMEOUT_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mThreadsList.clear();
        try {
            mServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request handler whose response can be controlled through the
     * {@link #writeResponse(String, BufferedWriter)} method.
     */
    abstract static class RequestHandler implements Runnable {
        private Socket mSocket;
        private BufferedReader mReader;
        private BufferedWriter mWriter;

        @SuppressWarnings("CatchAndPrintStackTrace")
        RequestHandler(Socket socket) {
            mSocket = socket;
            try {
                mSocket.setSoTimeout(TIMEOUT_MILLIS);
                mReader = new BufferedReader(
                        new InputStreamReader(mSocket.getInputStream(), UTF_8));
                mWriter = new BufferedWriter(
                        new OutputStreamWriter(mSocket.getOutputStream(), UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        @SuppressWarnings("CatchAndPrintStackTrace")
        public void run() {
            try {
                StringBuilder sb = new StringBuilder();
                String s = mReader.readLine();
                while (s != null && !s.trim().isEmpty()) {
                    sb.append(s);
                    sb.append("\n");
                    s = mReader.readLine();
                }
                String request = sb.toString();
                writeResponse(request, mWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        abstract void writeResponse(String request, BufferedWriter responseWriter)
                throws IOException;
    }

    /**
     * Request handler that responds with a HTML document containing the received request header.
     */
    static class EchoRequestHandler extends RequestHandler {
        EchoRequestHandler(Socket socket) {
            super(socket);
        }

        @Override
        void writeResponse(String request, BufferedWriter responseWriter) throws IOException {
            responseWriter.write("HTTP/1.0 200 OK\nContent-Type: text/html\n\r\n");
            responseWriter.write("<html><head><title>HttpServer</title></head>"
                    + "<body>Server handled this request:<br><pre>"
                    + request + "</pre></body></html>");
            responseWriter.flush();
        }
    }

    /**
     * Respond with text that indicates that the request was handled by a proxy.
     */
    static class ProxyRequestHandler extends RequestHandler {

        ProxyRequestHandler(Socket socket) {
            super(socket);
        }

        @Override
        void writeResponse(String request, BufferedWriter responseWriter) throws IOException {
            responseWriter.write("HTTP/1.0 200 OK\nUser-Agent: Proxy\n\r\n");
            responseWriter.write("<html><head><title>Proxy</title></head>"
                    + "<body>Proxy handled this request:<br><br>"
                    + request + "</body></html>");
            responseWriter.flush();
        }
    }
}
