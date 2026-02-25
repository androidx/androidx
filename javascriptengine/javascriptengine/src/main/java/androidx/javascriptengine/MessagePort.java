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

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.javascriptengine.common.MessagePortInternal;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Represents a single message port in a message channel.
 * <p>
 * This class is used to send and receive messages between two different execution contexts,
 * for example, between a Java environment and a JavaScript environment. It encapsulates the
 * underlying mechanism for message passing, providing a simplified API for sending and receiving
 * messages.
 * <p>
 * A {@link MessagePort} is created as part of a message channel, which consists of two entangled
 * message ports. When one port sends a message, it is received by the other port, and vice versa.
 * <p>
 * The type of the message is encapsulated in the {@link Message} class.
 * <p>
 * This class is thread-safe. Messages can be posted from any thread. The callback for receiving
 * messages is executed on the provided {@link Executor}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * Posts a message to the other end of the channel.
     * <p>
     * This method is non-blocking and can be called from any thread. It does not wait for the
     * remote end to receive the message.
     * <p>
     * Does not support unpaired surrogate strings.
     * <p>
     * Messages sent to a closed or dead port are silently discarded.
     * @param message The {@link Message} to send.
     */
    public void postMessage(@NonNull Message message) {
        Objects.requireNonNull(message);
        switch (message.getType()) {
            case Message.Type.STRING:
                mMessagePortInternal.postString(message.getString());
                break;

            case Message.Type.ARRAY_BUFFER:
                mMessagePortInternal.postArrayBuffer(message.getArrayBuffer());
                break;

            default:
                // This case should be unreachable if all Message.Type enum values are handled.
                // Reaching here indicates a bug in the JavaScriptEngine library.
                throw new RuntimeException("Unsupported message type: " + message.getType());
        }
    }

    /**
     * Closes the message port.
     * <p>
     * Once the port is closed, received and sent messages are silently discarded.
     */
    @Override
    public void close() {
        mMessagePortInternal.close();
    }
}
