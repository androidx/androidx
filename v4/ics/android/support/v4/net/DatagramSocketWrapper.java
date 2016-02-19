/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/** {@hide} */
class DatagramSocketWrapper extends Socket {
    public DatagramSocketWrapper(DatagramSocket socket, FileDescriptor fd) throws SocketException {
        super(new DatagramSocketImplWrapper(socket, fd));
    }

    /**
     * Empty implementation which wires in the given {@link FileDescriptor}.
     */
    private static class DatagramSocketImplWrapper extends SocketImpl {
        public DatagramSocketImplWrapper(DatagramSocket socket, FileDescriptor fd) {
            super();
            this.localport = socket.getLocalPort();
            this.fd = fd;
        }

        @Override
        protected void accept(SocketImpl newSocket) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected int available() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void bind(InetAddress address, int port) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void connect(String host, int port) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void connect(InetAddress address, int port) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void create(boolean isStreaming) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void listen(int backlog) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void connect(SocketAddress remoteAddr, int timeout) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void sendUrgentData(int value) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setOption(int optID, Object val) throws SocketException {
            throw new UnsupportedOperationException();
        }
    }
}
