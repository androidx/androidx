/*
 * Copyright (C) 2012 The Android Open Source Project
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

package androidx.core.net;

import android.net.TrafficStats;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Helper for accessing features in {@link TrafficStats}.
 */
public final class TrafficStatsCompat {
    /**
     * Clear active tag used when accounting {@link Socket} traffic originating
     * from the current thread.
     *
     * @deprecated Use {@link TrafficStats#clearThreadStatsTag()} directly.
     */
    @Deprecated
    public static void clearThreadStatsTag() {
        TrafficStats.clearThreadStatsTag();
    }

    /**
     * Get the active tag used when accounting {@link Socket} traffic originating
     * from the current thread. Only one active tag per thread is supported.
     * {@link #tagSocket(Socket)}.
     *
     * @deprecated Use {@link TrafficStats#getThreadStatsTag()} directly.
     */
    @Deprecated
    public static int getThreadStatsTag() {
        return TrafficStats.getThreadStatsTag();
    }

    /**
     * Increment count of network operations performed under the accounting tag
     * currently active on the calling thread. This can be used to derive
     * bytes-per-operation.
     *
     * @param operationCount Number of operations to increment count by.
     *
     * @deprecated Use {@link TrafficStats#incrementOperationCount(int)} directly.
     */
    @Deprecated
    public static void incrementOperationCount(int operationCount) {
        TrafficStats.incrementOperationCount(operationCount);
    }

    /**
     * Increment count of network operations performed under the given
     * accounting tag. This can be used to derive bytes-per-operation.
     *
     * @param tag Accounting tag used in {@link #setThreadStatsTag(int)}.
     * @param operationCount Number of operations to increment count by.
     *
     * @deprecated Use {@link TrafficStats#incrementOperationCount(int, int)} directly.
     */
    @Deprecated
    public static void incrementOperationCount(int tag, int operationCount) {
        TrafficStats.incrementOperationCount(tag, operationCount);
    }

    /**
     * Set active tag to use when accounting {@link Socket} traffic originating
     * from the current thread. Only one active tag per thread is supported.
     * <p>
     * Changes only take effect during subsequent calls to
     * {@link #tagSocket(Socket)}.
     * <p>
     * Tags between {@code 0xFFFFFF00} and {@code 0xFFFFFFFF} are reserved and
     * used internally by system services like DownloadManager when performing
     * traffic on behalf of an application.
     *
     * @deprecated Use {@link TrafficStats#setThreadStatsTag(int)} directly.
     */
    @Deprecated
    public static void setThreadStatsTag(int tag) {
        TrafficStats.setThreadStatsTag(tag);
    }

    /**
     * Tag the given {@link Socket} with any statistics parameters active for
     * the current thread. Subsequent calls always replace any existing
     * parameters. When finished, call {@link #untagSocket(Socket)} to remove
     * statistics parameters.
     *
     * @see #setThreadStatsTag(int)
     *
     * @deprecated Use {@link TrafficStats#tagSocket(Socket)} directly.
     */
    @Deprecated
    public static void tagSocket(Socket socket) throws SocketException {
        TrafficStats.tagSocket(socket);
    }

    /**
     * Remove any statistics parameters from the given {@link Socket}.
     *
     * @deprecated Use {@link TrafficStats#untagSocket(Socket)} directly.
     */
    @Deprecated
    public static void untagSocket(Socket socket) throws SocketException {
        TrafficStats.untagSocket(socket);
    }

    /**
     * Tag the given {@link DatagramSocket} with any statistics parameters
     * active for the current thread. Subsequent calls always replace any
     * existing parameters. When finished, call
     * {@link #untagDatagramSocket(DatagramSocket)} to remove statistics
     * parameters.
     *
     * @see #setThreadStatsTag(int)
     */
    public static void tagDatagramSocket(@NonNull DatagramSocket socket) throws SocketException {
        if (Build.VERSION.SDK_INT >= 24) {
            TrafficStats.tagDatagramSocket(socket);
        } else {
            final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromDatagramSocket(socket);
            TrafficStats.tagSocket(new DatagramSocketWrapper(socket, pfd.getFileDescriptor()));
            // The developer is still using the FD, so we need to detach it to
            // prevent the PFD finalizer from closing it in their face. We had to
            // wait until after the tagging call above, since detaching clears out
            // the getFileDescriptor() result which tagging depends on.
            pfd.detachFd();
        }
    }

    /**
     * Remove any statistics parameters from the given {@link DatagramSocket}.
     */
    public static void untagDatagramSocket(@NonNull DatagramSocket socket) throws SocketException {
        if (Build.VERSION.SDK_INT >= 24) {
            TrafficStats.untagDatagramSocket(socket);
        } else {
            final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromDatagramSocket(socket);
            TrafficStats.untagSocket(new DatagramSocketWrapper(socket, pfd.getFileDescriptor()));
            // The developer is still using the FD, so we need to detach it to
            // prevent the PFD finalizer from closing it in their face. We had to
            // wait until after the tagging call above, since detaching clears out
            // the getFileDescriptor() result which tagging depends on.
            pfd.detachFd();
        }
    }

    private TrafficStatsCompat() {}
}
