/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Simple HTTP server that accepts requests. Can be used to test proxying or as an end destination
 * server.
 */
class HttpServer(
    port: Int,
    private val handlerFactory: RequestHandlerFactory,
    private val callback: OnRequestCallback?,
) {

    /** This interface offers a callback that is run when a request is served. */
    fun interface OnRequestCallback {
        /** Called when this proxy serves a request. */
        fun onRequestServed()
    }

    /**
     * Provider of request handlers.
     *
     * Typically just a static reference to either {@code HttpServer.EchoRequestHandler::new} or
     * {@code HttpServer.ProxyRequestHandler::new}
     */
    fun interface RequestHandlerFactory {
        /** Called to initialize a new RequestHandler */
        fun create(socket: Socket): RequestHandler
    }

    private var running = false
    private val requestCount = AtomicInteger()
    private val threadPool = Executors.newCachedThreadPool()
    private val listenExecutor = Executors.newSingleThreadExecutor()
    private val serverSocket: ServerSocket = ServerSocket(port)

    fun getPort(): Int {
        return serverSocket.localPort
    }

    fun getRequestCount(): Int {
        return requestCount.get()
    }

    /** Start listening for requests. */
    fun start() {
        if (!running) {
            if (listenExecutor.isShutdown) {
                throw IllegalStateException("You can not restart a shutdown server.")
            }
            running = true
            listenExecutor.submit(::listenLoop)
        }
    }

    private fun listenLoop() {
        runCatching { listen() }.onFailure { it.printStackTrace() }
        listenExecutor.submit(::listenLoop)
    }

    private fun listen() {
        val socket = serverSocket.accept()
        requestCount.getAndIncrement()
        callback?.onRequestServed()
        val handler = handlerFactory.create(socket)
        threadPool.submit(handler)
    }

    /** Shutdown. */
    fun shutdown() {
        if (running) {
            running = false
            listenExecutor.shutdown()
            threadPool.shutdown()
            listenExecutor.awaitTermination(5, TimeUnit.SECONDS)
            threadPool.awaitTermination(5, TimeUnit.SECONDS)
            serverSocket.close()
        }
    }

    abstract class RequestHandler(private val socket: Socket) : Runnable {

        private lateinit var reader: BufferedReader
        private lateinit var writer: BufferedWriter

        init {
            runCatching {
                    socket.soTimeout = TIMEOUT_MILLIS
                    reader = BufferedReader(InputStreamReader(socket.getInputStream(), UTF_8))
                    writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), UTF_8))
                }
                .onFailure { it.printStackTrace() }
        }

        override fun run() {
            runCatching {
                    // we could probably use reader.readText() instead of a StringBuilder
                    // here, but it was returning an empty string? I have no idea why.
                    val stringBuild = StringBuilder()
                    var s = reader.readLine()
                    while (!s.isNullOrEmpty()) {
                        stringBuild.append(s).append("\n")
                        s = reader.readLine()
                    }
                    writeResponse(stringBuild.toString(), writer)
                }
                .onFailure { it.printStackTrace() }
        }

        abstract fun writeResponse(request: String, responseWriter: BufferedWriter)
    }

    class EchoRequestHandler(socket: Socket, private val activity: Context) :
        RequestHandler(socket) {
        override fun writeResponse(request: String, responseWriter: BufferedWriter) {
            responseWriter.write("HTTP/1.0 200 OK\nContent-Type: text/html\n\r\n")
            responseWriter.write(
                activity.resources
                    .openRawResource(R.raw.echo_request_handler_template)
                    .readText()
                    .format(request)
            )
            responseWriter.flush()
        }
    }

    class ProxyRequestHandler(socket: Socket, private val activity: AppCompatActivity) :
        RequestHandler(socket) {
        override fun writeResponse(request: String, responseWriter: BufferedWriter) {
            responseWriter.write("HTTP/1.0 200 OK\nUser-Agent: Proxy\n\r\n")
            responseWriter.write(
                activity.resources
                    .openRawResource(R.raw.proxy_request_handler_template)
                    .readText()
                    .format(request)
            )
            responseWriter.flush()
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5000
    }
}
