/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.javascriptengine;

import androidx.annotation.VisibleForTesting;
import androidx.javascriptengine.common.MessagePortInternal;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Represents a single message port in a message channel.
 * <p>
 * A message port is created as part of a message channel, which consists of two entangled
 * message ports. When one port sends a message, it is received by the other port, and vice versa.
 * These ports can exist in separate processes. For example, a port in a Java-based Android
 * application can send and receive messages to and from a port in a JavaScript isolate.
 * <p>
 * A message channel between the app and an isolate can be established using
 * {@link JavaScriptIsolate#createMessageChannel(String, Executor, MessagePortClient)}, which returns
 * the app-side MessagePort.
 * <p>
 * A reference to the MessagePort is maintained so long as the message channel remains alive.
 * <p>
 * This class is thread-safe. Messages can be posted from any thread.
 */
@ThreadSafe
public final class MessagePort implements AutoCloseable {
    private final MessagePortInternal mMessagePortInternal;

    /**
     * Package-private constructor to create a MessagePort with an internal implementation.
     *
     * @param messagePortInternal The internal message port implementation.
     */
    MessagePort(@NonNull MessagePortInternal messagePortInternal) {
        mMessagePortInternal = messagePortInternal;
    }

    /**
     * Package-private constructor for an empty MessagePort.
     * Used to create message port in an already closed-like state.
     */
    MessagePort() {
        mMessagePortInternal = new MessagePortInternal();
    }

    @VisibleForTesting
    MessagePortInternal getMessagePortInternalForTest() {
        return mMessagePortInternal;
    }

    /**
     * Posts a message over the channel to the other port.
     * <p>
     * This method does not block on the message being processed on the receiving side and can be
     * called from any thread.
     * <p>
     * Messages sent via a closed or dead port/channel are silently discarded.
     * @param message The {@link Message} to send.
     */
    public void postMessage(@NonNull Message message) {
        Objects.requireNonNull(message);
        switch (message.getType()) {
            case Message.TYPE_STRING:
                mMessagePortInternal.postString(message.getString());
                break;

            case Message.TYPE_ARRAY_BUFFER:
                mMessagePortInternal.postArrayBuffer(message.getArrayBuffer());
                break;

            default:
                // This case should be unreachable if all Message.Type enum values are handled.
                // Reaching here indicates a bug in the JavaScriptEngine library.
                throw new UnsupportedOperationException(
                        "Unsupported message type: " + message.getType());
        }
    }

    /**
     * Closes the message port to free resources.
     * <p>
     * Once the port is closed, sent and received messages are silently discarded.
     */
    @Override
    public void close() {
        mMessagePortInternal.close();
    }
}
