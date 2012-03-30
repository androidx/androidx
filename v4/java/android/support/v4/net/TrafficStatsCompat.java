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

package android.support.v4.net;

import android.os.Build;

import java.net.Socket;
import java.net.SocketException;

/**
 * Helper for accessing features in TrafficStats introduced after API level 14
 * in a backwards compatible fashion.
 */
public class TrafficStatsCompat {

    interface TrafficStatsCompatImpl {
        void clearThreadStatsTag();
        int getThreadStatsTag();
        void incrementOperationCount(int operationCount);
        void incrementOperationCount(int tag, int operationCount);
        void setThreadStatsTag(int tag);
        void tagSocket(Socket socket) throws SocketException;
        void untagSocket(Socket socket) throws SocketException;
    }

    static class BaseTrafficStatsCompatImpl implements TrafficStatsCompatImpl {
        private static class SocketTags {
            public int statsTag = -1;
        }

        private ThreadLocal<SocketTags> mThreadSocketTags = new ThreadLocal<SocketTags>() {
            @Override
            protected SocketTags initialValue() {
                return new SocketTags();
            }
        };

        @Override
        public void clearThreadStatsTag() {
            mThreadSocketTags.get().statsTag = -1;
        }

        @Override
        public int getThreadStatsTag() {
            return mThreadSocketTags.get().statsTag;
        }

        @Override
        public void incrementOperationCount(int operationCount) {
        }

        @Override
        public void incrementOperationCount(int tag, int operationCount) {
        }

        @Override
        public void setThreadStatsTag(int tag) {
            mThreadSocketTags.get().statsTag = tag;
        }

        @Override
        public void tagSocket(Socket socket) {
        }

        @Override
        public void untagSocket(Socket socket) {
        }
    }

    static class IcsTrafficStatsCompatImpl implements TrafficStatsCompatImpl {
        @Override
        public void clearThreadStatsTag() {
            TrafficStatsCompatIcs.clearThreadStatsTag();
        }

        @Override
        public int getThreadStatsTag() {
            return TrafficStatsCompatIcs.getThreadStatsTag();
        }

        @Override
        public void incrementOperationCount(int operationCount) {
            TrafficStatsCompatIcs.incrementOperationCount(operationCount);
        }

        @Override
        public void incrementOperationCount(int tag, int operationCount) {
            TrafficStatsCompatIcs.incrementOperationCount(tag, operationCount);
        }

        @Override
        public void setThreadStatsTag(int tag) {
            TrafficStatsCompatIcs.setThreadStatsTag(tag);
        }

        @Override
        public void tagSocket(Socket socket) throws SocketException {
            TrafficStatsCompatIcs.tagSocket(socket);
        }

        @Override
        public void untagSocket(Socket socket) throws SocketException {
            TrafficStatsCompatIcs.untagSocket(socket);
        }
    }

    private static final TrafficStatsCompatImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 14) {
            IMPL = new IcsTrafficStatsCompatImpl();
        } else {
            IMPL = new BaseTrafficStatsCompatImpl();
        }
    }

    /**
     * Clear active tag used when accounting {@link Socket} traffic originating
     * from the current thread.
     */
    public static void clearThreadStatsTag() {
        IMPL.clearThreadStatsTag();
    }

    /**
     * Get the active tag used when accounting {@link Socket} traffic originating
     * from the current thread. Only one active tag per thread is supported.
     * {@link #tagSocket(Socket)}.
     */
    public static int getThreadStatsTag() {
        return IMPL.getThreadStatsTag();
    }

    /**
     * Increment count of network operations performed under the accounting tag
     * currently active on the calling thread. This can be used to derive
     * bytes-per-operation.
     *
     * @param operationCount Number of operations to increment count by.
     */
    public static void incrementOperationCount(int operationCount) {
        IMPL.incrementOperationCount(operationCount);
    }

    /**
     * Increment count of network operations performed under the given
     * accounting tag. This can be used to derive bytes-per-operation.
     *
     * @param tag Accounting tag used in {@link #setThreadStatsTag(int)}.
     * @param operationCount Number of operations to increment count by.
     */
    public static void incrementOperationCount(int tag, int operationCount) {
        IMPL.incrementOperationCount(tag, operationCount);
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
     */
    public static void setThreadStatsTag(int tag) {
        IMPL.setThreadStatsTag(tag);
    }

    /**
     * Tag the given {@link Socket} with any statistics parameters active for
     * the current thread. Subsequent calls always replace any existing
     * parameters. When finished, call {@link #untagSocket(Socket)} to remove
     * statistics parameters.
     *
     * @see #setThreadStatsTag(int)
     */
    public static void tagSocket(Socket socket) throws SocketException {
        IMPL.tagSocket(socket);
    }

    /**
     * Remove any statistics parameters from the given {@link Socket}.
     */
    public static void untagSocket(Socket socket) throws SocketException {
        IMPL.untagSocket(socket);
    }
}
