/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.testapp.twoWayIpc

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Messenger
import androidx.datastore.testapp.twoWayIpc.IpcLogger.log
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * A bus that can be used across processes to make IPC calls.
 *
 * You wouldn't use this directly, instead, use [TwoWayIpcSubject] combined with [IpcAction].
 */
class TwoWayIpcBus(val executionScope: CoroutineScope, val handler: suspend (Bundle?) -> Bundle?) {
    private val pendingMessages = mutableMapOf<String, CompletableDeferred<Bundle?>>()
    val incomingMessenger =
        Messenger(
            object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    val copy = Message.obtain().also { it.copyFrom(msg) }
                    copy.data?.classLoader = TwoWayIpcBus::class.java.classLoader
                    executionScope.launch { handleIncomingMessage(copy) }
                }
            }
        )

    private lateinit var outgoingMessenger: Messenger

    private suspend fun handleIncomingMessage(msg: Message) {
        log("incoming message")
        val uuid = msg.data.getString(KEY_UUID) ?: error("no uuid in message")
        log("id: $uuid, what: ${msg.what}")
        when (msg.what) {
            MSG_EXECUTE_ACTION -> {
                val payload = msg.data.getBundle(KEY_PAYLOAD)
                val responseMessage = Message.obtain()
                responseMessage.data.putString(KEY_UUID, uuid)
                try {
                    val handlerResponse = handler(payload)
                    responseMessage.what = MSG_ACTION_RESPONSE
                    responseMessage.data.putBundle(KEY_PAYLOAD, handlerResponse)
                } catch (th: Throwable) {
                    log("error while handling message, ${th.stackTraceToString()}")
                    responseMessage.what = MSG_EXCEPTION
                    responseMessage.data.putString(KEY_STACKTRACE, th.stackTraceToString())
                }
                msg.replyTo.send(responseMessage)
            }
            MSG_ACTION_RESPONSE -> {
                val responseHandle =
                    synchronized(pendingMessages) { pendingMessages.remove(uuid) }
                        ?: error("no response handle for $uuid")
                responseHandle.complete(msg.data.getBundle(KEY_PAYLOAD))
            }
            MSG_EXCEPTION -> {
                val responseHandle =
                    synchronized(pendingMessages) { pendingMessages.remove(uuid) }
                        ?: error("no response handle for $uuid")
                val exceptionMessage = msg.data.getString(KEY_STACKTRACE)
                responseHandle.completeExceptionally(
                    RuntimeException("exception in remote process: $exceptionMessage")
                )
            }
            else -> {
                // respond with error
                msg.replyTo.send(
                    Message.obtain().also {
                        it.what = MSG_EXCEPTION
                        it.data.putString(KEY_STACKTRACE, "unknown message what: ${msg.what}")
                    }
                )
            }
        }
    }

    fun setOutgoingMessenger(messenger: Messenger) {
        outgoingMessenger = messenger
    }

    suspend fun sendMessage(payload: Bundle?): Bundle? {
        val uuid = UUID.randomUUID().toString()
        log("sending message $uuid")
        val response = CompletableDeferred<Bundle?>()
        synchronized(pendingMessages) { pendingMessages[uuid] = response }

        val message = Message.obtain()
        message.what = MSG_EXECUTE_ACTION
        message.data.putBundle(KEY_PAYLOAD, payload)
        message.data.putString(KEY_UUID, uuid)
        message.replyTo = incomingMessenger
        message.data?.classLoader = TwoWayIpcBus::class.java.classLoader
        outgoingMessenger.send(message)
        log("sent message $uuid")
        return withTimeout(TIMEOUT) { response.await() }.also { log("received response for $uuid") }
    }

    companion object {
        private val TIMEOUT = 5.seconds
        private const val MSG_EXECUTE_ACTION = 1
        private const val MSG_ACTION_RESPONSE = 2
        private const val MSG_EXCEPTION = 3
        private const val KEY_UUID = "ipc_uuid"
        private const val KEY_PAYLOAD = "ipc_payload"
        private const val KEY_STACKTRACE = "ipc_stacktrace"
    }
}
