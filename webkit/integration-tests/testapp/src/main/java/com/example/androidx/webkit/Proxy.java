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
 * Simple proxy that accepts requests.
 */
public class Proxy {

    /**
     * This interface offers a callback that is run when this proxy serves a request.
     */
    public interface ProxyRequestCallback {

        /**
         * Called when this proxy serves a request.
         */
        void onProxyRequestServed();
    }

    private static final int TIMEOUT_MILLIS = 5000;
    private boolean mRunning = false;
    private ProxyRequestCallback mCallback;
    private ServerSocket mServerSocket;
    private List<Thread> mThreadsList;
    private int mRequestCount;

    /**
     * Create a proxy using provided port number.
     *
     * @param port port number
     * @param callback callback run when this proxy serves a request
     */
    public Proxy(int port, ProxyRequestCallback callback) {
        mRequestCount = 0;
        mCallback = callback;
        mThreadsList = new ArrayList<>();
        try {
            mServerSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a proxy using provided port number.
     *
     * @param port port number
     */
    public Proxy(int port) {
        this (port, null);
    }

    /**
     * Create a proxy using an available port.
     */
    public Proxy() {
        this(0, null);
    }

    /**
     * Get port number.
     */
    public int getPort() {
        return mServerSocket.getLocalPort();
    }

    /**
     * Get the number of requests this proxy has served.
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

    private void listen() {
        try {
            Socket socket = mServerSocket.accept();
            mRequestCount++;
            if (mCallback != null) {
                mCallback.onProxyRequestServed();
            }
            Thread thread = new Thread(new RequestHandler(socket));
            mThreadsList.add(thread);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Shutdown.
     */
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

    private static class RequestHandler implements Runnable {
        private Socket mSocket;
        private BufferedReader mReader;
        private BufferedWriter mWriter;

        RequestHandler(Socket socket) {
            mSocket = socket;
            try {
                mSocket.setSoTimeout(TIMEOUT_MILLIS);
                mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                mWriter = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                StringBuilder sb = new StringBuilder();
                String s = mReader.readLine();
                while (s != null && !s.trim().isEmpty()) {
                    sb.append(s);
                    sb.append(" ");
                    s = mReader.readLine();
                }
                String request = sb.toString();
                mWriter.write("HTTP/1.0 200 OK\nUser-Agent: Proxy\n\r\n");
                mWriter.write("<html><head><title>Proxy</title></head>"
                        + "<body>Proxy handled this request:<br><br>"
                        + request + "</body></html>");
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
