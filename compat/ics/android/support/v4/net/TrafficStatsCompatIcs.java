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

import android.net.TrafficStats;
import android.os.ParcelFileDescriptor;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementation of TrafficStatsCompat that can call ICS APIs.
 */
class TrafficStatsCompatIcs {
    public static void clearThreadStatsTag() {
        TrafficStats.clearThreadStatsTag();
    }

    public static int getThreadStatsTag() {
        return TrafficStats.getThreadStatsTag();
    }

    public static void incrementOperationCount(int operationCount) {
        TrafficStats.incrementOperationCount(operationCount);
    }

    public static void incrementOperationCount(int tag, int operationCount) {
        TrafficStats.incrementOperationCount(tag, operationCount);
    }

    public static void setThreadStatsTag(int tag) {
        TrafficStats.setThreadStatsTag(tag);
    }

    public static void tagSocket(Socket socket) throws SocketException {
        TrafficStats.tagSocket(socket);
    }

    public static void untagSocket(Socket socket) throws SocketException {
        TrafficStats.untagSocket(socket);
    }

    public static void tagDatagramSocket(DatagramSocket socket) throws SocketException {
        final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromDatagramSocket(socket);
        TrafficStats.tagSocket(new DatagramSocketWrapper(socket, pfd.getFileDescriptor()));
        // The developer is still using the FD, so we need to detach it to
        // prevent the PFD finalizer from closing it in their face. We had to
        // wait until after the tagging call above, since detaching clears out
        // the getFileDescriptor() result which tagging depends on.
        pfd.detachFd();
    }

    public static void untagDatagramSocket(DatagramSocket socket) throws SocketException {
        final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromDatagramSocket(socket);
        TrafficStats.untagSocket(new DatagramSocketWrapper(socket, pfd.getFileDescriptor()));
        // The developer is still using the FD, so we need to detach it to
        // prevent the PFD finalizer from closing it in their face. We had to
        // wait until after the tagging call above, since detaching clears out
        // the getFileDescriptor() result which tagging depends on.
        pfd.detachFd();
    }
}
