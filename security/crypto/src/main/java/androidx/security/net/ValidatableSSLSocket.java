/*
 * Copyright 2018 The Android Open Source Project
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


package androidx.security.net;

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.RestrictTo;
import androidx.security.SecureConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/**
 * A custom implementation of SSLSocket which forces TLS, and handles automatically doing
 * certificate validity checks.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ValidatableSSLSocket extends SSLSocket {

    private static final String TAG = "ValidatableSSLSocket";

    private SSLSocket mSslSocket;
    private String mHostname;
    private SecureURL mSecureURL;
    private boolean mHandshakeStarted = false;
    private SecureConfig mSecureConfig;

    ValidatableSSLSocket(SecureURL secureURL, Socket sslSocket, SecureConfig secureConfig)
            throws IOException {
        this.mSecureURL = secureURL;
        this.mHostname = secureURL.getHostname();
        this.mSslSocket = (SSLSocket) sslSocket;
        this.mSecureConfig = secureConfig;
        setSecureCiphers();
        isValid();
    }

    private void setSecureCiphers() {
        if (mSecureConfig.getUseStrongSSLCiphersEnabled()) {
            this.mSslSocket.setEnabledCipherSuites(mSecureConfig.getStrongSSLCiphers());
        }
    }

    private void isValid() throws IOException {
        startHandshake();
        try {
            if (!mSecureURL.isValid(this.mHostname, this.mSslSocket)) {
                throw new IOException("Found invalid certificate");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IOException("Found invalid certificate");
        }
    }

    @Override
    public void startHandshake() throws IOException {
        if (!mHandshakeStarted) {
            mSslSocket.startHandshake();
            mHandshakeStarted = true;
        }
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mSslSocket.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return mSslSocket.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        mSslSocket.setEnabledCipherSuites(suites);
    }

    @Override
    public String[] getSupportedProtocols() {
        return mSslSocket.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return mSslSocket.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        mSslSocket.setEnabledProtocols(protocols);
    }

    @Override
    public SSLSession getSession() {
        return mSslSocket.getSession();
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        mSslSocket.addHandshakeCompletedListener(listener);
    }

    @Override
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        mSslSocket.removeHandshakeCompletedListener(listener);
    }

    @Override
    public void setUseClientMode(boolean mode) {
        mSslSocket.setUseClientMode(mode);
    }

    @Override
    public boolean getUseClientMode() {
        return mSslSocket.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        mSslSocket.setNeedClientAuth(need);
    }

    @Override
    public boolean getNeedClientAuth() {
        return mSslSocket.getNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean want) {
        mSslSocket.setWantClientAuth(want);
    }

    @Override
    public boolean getWantClientAuth() {
        return mSslSocket.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        mSslSocket.setEnableSessionCreation(flag);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return mSslSocket.getEnableSessionCreation();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public SSLSession getHandshakeSession() {
        return mSslSocket.getHandshakeSession();
    }

    @Override
    public SSLParameters getSSLParameters() {
        return mSslSocket.getSSLParameters();
    }

    @Override
    public void setSSLParameters(SSLParameters params) {
        mSslSocket.setSSLParameters(params);
    }

    @Override
    public String toString() {
        return mSslSocket.toString();
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        mSslSocket.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        mSslSocket.connect(endpoint, timeout);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        mSslSocket.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
        return mSslSocket.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return mSslSocket.getLocalAddress();
    }

    @Override
    public int getPort() {
        return mSslSocket.getPort();
    }

    @Override
    public int getLocalPort() {
        return mSslSocket.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return mSslSocket.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return mSslSocket.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return mSslSocket.getChannel();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return mSslSocket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return mSslSocket.getOutputStream();
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        mSslSocket.setTcpNoDelay(on);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return mSslSocket.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        mSslSocket.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return mSslSocket.getSoLinger();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        mSslSocket.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        mSslSocket.setOOBInline(on);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return mSslSocket.getOOBInline();
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        mSslSocket.setSoTimeout(timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return mSslSocket.getSoTimeout();
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        mSslSocket.setSendBufferSize(size);
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return mSslSocket.getSendBufferSize();
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        mSslSocket.setReceiveBufferSize(size);
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return mSslSocket.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        mSslSocket.setKeepAlive(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return mSslSocket.getKeepAlive();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        mSslSocket.setTrafficClass(tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return mSslSocket.getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        mSslSocket.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return mSslSocket.getReuseAddress();
    }

    @Override
    public synchronized void close() throws IOException {
        mSslSocket.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        mSslSocket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        mSslSocket.shutdownOutput();
    }

    @Override
    public boolean isConnected() {
        return mSslSocket.isConnected();
    }

    @Override
    public boolean isBound() {
        return mSslSocket.isBound();
    }

    @Override
    public boolean isClosed() {
        return mSslSocket.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return mSslSocket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return mSslSocket.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        mSslSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }
}
