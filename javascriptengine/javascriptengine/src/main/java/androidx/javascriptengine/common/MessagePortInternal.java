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

package androidx.javascriptengine.common;

import android.content.res.AssetFileDescriptor;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.common.util.concurrent.MoreExecutors;

import org.chromium.android_webview.js_sandbox.common.IMessagePort;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal implementation of a message port, responsible for the low-level mechanics of
 * message passing over binder, and implementing the IMessagePort aidl interface.
 * <p>
 * This class handles the logic for sending and receiving messages. If an instance of
 * this class is accessed from multiple threads, callers must ensure proper synchronization.
 * <p>
 * A MessagePortInternal is one of two ends of a message channel. The other end is in another
 * process. To establish the connection, each side creates a MessagePortInternal and sends its
 * local binder {@link #getLocalIMessagePort()} to the other side, which then sets it as the remote
 * binder using {@link #setRemoteIMessagePort(IMessagePort)}.
 * <p>
 * Small messages (<64KiB) are sent directly over binder IPC.
 * Large messages (>=64KiB) are sent through file descriptors to avoid binder transaction limits.
 * The file descriptor itself is sent over binder.
 * <p>
 * Messages received before a {@link MessagePortClient} is set via
 * {@link #setClient(MessagePortClient)} are queued.
 * <p>
 * The port state (open/closed) is given by the {@link #mRemoteIMessagePort} value.
 */
public final class MessagePortInternal {
    private static final int MAX_BINDER_STRING_LENGTH = 32767; // 32 KiB - 1
    private static final int MAX_BINDER_ARRAY_BUFFER_SIZE = 65535; // 64 KiB - 1

    // Local binder stub that receives calls from the remote process.
    // Responsible for receiving messages from the remote port.
    @NonNull
    public final MessagePortIpcClient mLocalIMessagePort;
    // Remote binder proxy to send messages to the other process.
    // Dictates the state of the message port. If null port is closed, if otherwise port is open.
    // Responsible for sending messages to the remote port.
    public final AtomicReference<IMessagePort> mRemoteIMessagePort =
            new AtomicReference<>(null);
    // Sandbox thread pool task executor.
    // Offloads messages sent over file descriptor.
    @NonNull
    public final ExecutorService mIoExecutor;
    // The client is an implementation logic of the functions that determine how the messages
    // are processed after they have been received.
    // This is not responsible for receiving messages. Is is responsible to process messages.
    @NonNull
    public MessagePortClient mMessagePortClient;
    // Maximum return size of the isolate.
    // This applies only to incoming messages on the app side.
    // The service side limit is set to an unreachable high value.
    private final int mMaxMessageSize;
    // Binder link to death. Link closeLocally to isolate/sandbox death.
    public IBinder.@Nullable DeathRecipient mDeathRecipient;

    private final class MessagePortIpcClient extends IMessagePort.Stub {
        MessagePortIpcClient() {}

        @Override
        public void sendString(String string) {
            Objects.requireNonNull(string);

            if (mRemoteIMessagePort.get() == null) return; // Silently discard the message

            long identity = Binder.clearCallingIdentity();
            try {
                mMessagePortClient.onString(string);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void sendStringOverFd(AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            boolean shouldClose = true;

            try {
                long length = afd.getLength();
                if (length < 0) {
                    throw new IllegalArgumentException("string has invalid length: " + length);
                } else if (length > mMaxMessageSize) {
                    throw new IllegalArgumentException("string is too large: " + length);
                }
                if (mRemoteIMessagePort.get() == null) return; // Silently discard the message

                long identity = Binder.clearCallingIdentity();
                try (InputStream inputStream = afd.createInputStream()) {
                    byte[] bytes = new byte[(int) length];
                    int bytesRead = Utils.readNBytes(inputStream, bytes, 0, bytes.length);
                    if (bytesRead != bytes.length) {
                        throw new EOFException("Incomplete data read from file descriptor."
                                + " Expected " + bytes.length + " bytes, but got " + bytesRead);
                    }

                    String string = new String(bytes, StandardCharsets.UTF_8);
                    mMessagePortClient.onString(string);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
                shouldClose = false;
            } finally {
                if (shouldClose) Utils.closeQuietly(afd);
            }
        }

        @Override
        public void sendArrayBuffer(byte[] bytes) {
            Objects.requireNonNull(bytes);

            if (mRemoteIMessagePort.get() == null) return; // Silently discard the message

            long identity = Binder.clearCallingIdentity();
            try {
                mMessagePortClient.onArrayBuffer(bytes);
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        @Override
        public void sendArrayBufferOverFd(AssetFileDescriptor afd) {
            Objects.requireNonNull(afd);
            boolean shouldClose = true;

            try {
                long length = afd.getLength();
                if (length < 0) {
                    throw new IllegalArgumentException("arrayBuffer has invalid length: " + length);
                } else if (length > mMaxMessageSize) {
                    throw new IllegalArgumentException("arrayBuffer is too large: " + length);
                }
                if (mRemoteIMessagePort.get() == null) return; // Silently discard the message

                long identity = Binder.clearCallingIdentity();
                try (InputStream inputStream = afd.createInputStream()) {
                    byte[] bytes = new byte[(int) length];
                    int bytesRead = Utils.readNBytes(inputStream, bytes, 0, bytes.length);
                    if (bytesRead != bytes.length) {
                        throw new EOFException("Incomplete data read from file descriptor."
                                + " Expected " + bytes.length + " bytes, but got " + bytesRead);
                    }

                    mMessagePortClient.onArrayBuffer(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    Binder.restoreCallingIdentity(identity);
                }
                shouldClose = false;
            } finally {
                if (shouldClose) Utils.closeQuietly(afd);
            }
        }

        @Override
        public void close() {
            MessagePortInternal.this.closeLocally();
        }

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
        }
    }

    /**
     * Interface for handling messages received through a MessagePortInternal.
     */
    public interface MessagePortClient {
        /**
         * Called when a String message is received.
         *
         * @param string The String message.
         */
        void onString(@NonNull String string);

        /**
         * Called when an ArrayBuffer message is received.
         *
         * @param arrayBuffer The ArrayBuffer message as a byte array.
         */
        void onArrayBuffer(byte @NonNull [] arrayBuffer);
    }

    /**
     * Creates a new MessagePortInternal that can later be entangled.
     *
     * @param executorService The ExecutorService that will run the pipe reading.
     * @param maxMessageSize The maximum size in bytes allowed for a received message.
     * @return a new MessagePortInternal.
     */
    public MessagePortInternal(@NonNull ExecutorService executorService, int maxMessageSize) {
        mLocalIMessagePort = new MessagePortIpcClient();
        mIoExecutor = executorService;
        mMessagePortClient = new MessagePortClient() {
            @Override
            public void onString(String string) {}

            @Override
            public void onArrayBuffer(byte[] arrayBuffer) {}
        };
        mMaxMessageSize = maxMessageSize;
    }

    /**
     * Creates an empty MessagePortInternal that cannot be used.
     *
     * @return an empty unusable MessagePortInternal.
     */
    public MessagePortInternal() {
        mLocalIMessagePort = new MessagePortIpcClient();
        mIoExecutor = MoreExecutors.newDirectExecutorService();
        mMessagePortClient = new MessagePortClient() {
            @Override
            public void onString(String string) {}

            @Override
            public void onArrayBuffer(byte[] arrayBuffer) {}
        };
        mMaxMessageSize = 0;
    }

    /**
     * The local IMessagePort used by the remote port to send messages.
     *
     * @return the local IMessagePort.
     */
    @NonNull
    public IMessagePort getLocalIMessagePort() {
        return mLocalIMessagePort;
    }

    /**
     * Sets the remote IMessagePort.
     *
     * Remote IMessagePort is responsible for sending messages.
     * <p>
     * Must be called exactly once after construction, and before doing
     * anything else with the object.
     *
     * @param iMessagePort the remote IMessagePort.
     */
    public void setRemoteIMessagePort(@NonNull IMessagePort iMessagePort) {
        mRemoteIMessagePort.set(iMessagePort);
        mDeathRecipient = new IBinder.DeathRecipient() {
            @Override
            public void binderDied() {
                closeLocally();
            }
        };
        try {
            iMessagePort.asBinder().linkToDeath(mDeathRecipient, 0);
        } catch (RemoteException e) {
            // Remote already dead. Close local port.
            closeLocally();
        }
    }

    /**
     * Sets the client for receiving messages.
     *
     * This should be set exactly once, before exposing the port to the other side.
     *
     * @param client the client to handle the messages.
     */
    public void setClient(@NonNull MessagePortClient client) {
        mMessagePortClient = client;
    }

    /**
     * Posts a String message to the remote port.
     *
     * @param string the String message to post.
     */
    public void postString(@NonNull String string) {
        IMessagePort remote = mRemoteIMessagePort.get();
        if (remote == null) return;

        try {
            if (string.length() <= MAX_BINDER_STRING_LENGTH) {
                remote.sendString(string);
            } else {
                // TODO(b/484314333):
                //  Mangles illegal code units into '?'. If we needed to change this to U+FFFD like
                //  most other parts of JavaScriptEngine, we could use a customized CharsetEncoder.
                byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
                try (AssetFileDescriptor afd = Utils.writeBytesIntoPipeAsync(bytes, mIoExecutor)) {
                    remote.sendStringOverFd(afd);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (RejectedExecutionException e) {
            // On the app side, this probably means the sandbox is closed, but it could also mean
            // something has gone horribly wrong (like exhausting a task queue limit), which should
            // be handled separately.
            //
            // At the very least, it's no longer valid to send messages on this port.
            closeLocally();
        } catch (DeadObjectException e) {
            // The remote process has died, so we can ignore this error.
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Posts an ArrayBuffer message to the remote port.
     *
     * @param bytes the ArrayBuffer message to post.
     */
    public void postArrayBuffer(byte @NonNull [] bytes) {
        IMessagePort remote = mRemoteIMessagePort.get();
        if (remote == null) return;

        try {
            if (bytes.length <= MAX_BINDER_ARRAY_BUFFER_SIZE) {
                remote.sendArrayBuffer(bytes);
            } else {
                try (AssetFileDescriptor afd = Utils.writeBytesIntoPipeAsync(bytes, mIoExecutor)) {
                    remote.sendArrayBufferOverFd(afd);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (RejectedExecutionException e) {
            // On the app side, this probably means the sandbox is closed, but it could also mean
            // something has gone horribly wrong (like exhausting a task queue limit), which should
            // be handled separately.
            //
            // At the very least, it's no longer valid to send messages on this port.
            closeLocally();
        } catch (DeadObjectException e) {
            // The remote process has died, so we can ignore this error.
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the message port, and notifies the remote port that this end is closed.
     *
     * Called by the API or on isolate/sandbox close and death.
     */
    public void close() {
        IMessagePort remoteIMessagePort = mRemoteIMessagePort.getAndSet(null);
        if (remoteIMessagePort == null) return;

        closeLocally();
        try {
            remoteIMessagePort.close();
        } catch (DeadObjectException e) {
            // The remote process has died, so we can ignore this error.
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles the closing of the local port and releases its resources.
     *
     * Called by the API or remote port.
     */
    private void closeLocally() {
        mRemoteIMessagePort.set(null);
    }
}
