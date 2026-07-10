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

package androidx.javascriptengine;

import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import androidx.javascriptengine.common.LengthLimitExceededException;
import androidx.javascriptengine.common.MessagePortInternal;
import androidx.javascriptengine.common.Utils;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.base.Strings;

import org.chromium.android_webview.js_sandbox.common.IMessagePort;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link MessagePortInternal} class.
 * <p>
 * These tests do not involve real IPC or spin up a JsSandbox process.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class MessagePortInternalTest {
    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024; // 2Mi (bytes)

    private static final int MAX_BINDER_STRING_LENGTH = 32767; // 32Ki - 1 (chars)
    private static final int MAX_BINDER_ARRAY_BUFFER_SIZE = 65535; // 64Ki - 1 (bytes)

    private ExecutorService mThreadPoolExecutorService;

    private final class LoggingMessagePortIpcClient extends IMessagePort.Stub {
        private LinkedBlockingQueue<Object[]> mLog = new LinkedBlockingQueue<>();

        LoggingMessagePortIpcClient() {
        }

        @Override
        public void sendString(String string) {
            mLog.add(new Object[]{"sendString", string});
        }

        @Override
        public void sendStringOverFd(AssetFileDescriptor afd) {
            try {
                mLog.add(new Object[]{"sendStringOverFd",
                        Utils.readToString(afd, Integer.MAX_VALUE, false)});
            } catch (IOException | LengthLimitExceededException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void sendArrayBuffer(byte[] bytes) {
            mLog.add(new Object[]{"sendArrayBuffer", bytes});
        }

        @Override
        public void sendArrayBufferOverFd(AssetFileDescriptor afd) {
            try {
                mLog.add(new Object[]{"sendArrayBufferOverFd",
                        Utils.readToBytes(afd, Integer.MAX_VALUE, false)});
            } catch (IOException | LengthLimitExceededException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            mLog.add(new Object[]{"close"});
        }

        private Object[] pollLog() throws Throwable {
            Object[] item = mLog.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (item == null) {
                throw new AssertionError("log empty after timeout");
            }
            return item;
        }

        public void assertEmpty() throws Throwable {
            Assert.assertTrue(mLog.isEmpty());
        }

        public void assertNext(Object[] expected) throws Throwable {
            Object[] actual = pollLog();
            Assert.assertTrue(Arrays.deepEquals(expected, actual));
        }

        public void assertSendString(String string) throws Throwable {
            assertNext(new Object[]{"sendString", string});
        }

        public void assertSendStringOverFd(String string) throws Throwable {
            assertNext(new Object[]{"sendStringOverFd", string});
        }

        public void assertSendArrayBuffer(byte[] bytes) throws Throwable {
            assertNext(new Object[]{"sendArrayBuffer", bytes});
        }

        public void assertSendArrayBufferOverFd(byte[] bytes) throws Throwable {
            assertNext(new Object[]{"sendArrayBufferOverFd", bytes});
        }

        public void assertClose() throws Throwable {
            assertNext(new Object[]{"close"});
        }

        @Override
        public int getInterfaceVersion() {
            return super.VERSION;
        }
    }

    // Used when a test is expected to never use the executor service.
    // Note that this generates an AssertionError rather than a RejectedExecutionException, as
    // RejectedExecutionException does not imply a call to execute was illegal/unexpected.
    private static final class ImmediateFailExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable runnable) {
            Assert.fail("Executor service should not have been used.");
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }
    }

    @Before
    public void setup() {
        mThreadPoolExecutorService = Executors.newCachedThreadPool();
    }

    @After
    public void teardown() throws Throwable {
        if (mThreadPoolExecutorService != null) {
            mThreadPoolExecutorService.shutdown();
            if (!mThreadPoolExecutorService.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new AssertionError(
                        "Thread pool not terminated in time during teardown. Beware that this "
                                + "error might hide an earlier failure.");
            }
        }
    }

    @Test
    public void testAfdWithUnknownLength_illegalArgument() throws Throwable {
        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        IMessagePort messagePortClient = messagePort.getLocalIMessagePort();

        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        AssetFileDescriptor.UNKNOWN_LENGTH);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
    }

    @Test
    public void testAfdWithNegativeLength_illegalArgument() throws Throwable {
        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        IMessagePort messagePortClient = messagePort.getLocalIMessagePort();

        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0, -2);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0, -2);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
    }

    @Test
    public void testPostMessageExceedingMaxMessageSize_illegalArgument() throws Throwable {
        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        IMessagePort messagePortClient = messagePort.getLocalIMessagePort();

        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        MAX_MESSAGE_SIZE + 1);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        MAX_MESSAGE_SIZE + 1);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
    }

    @Test
    public void testPostMessageOfSizeWithIntegerLimits_illegalArgument() throws Throwable {
        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        IMessagePort messagePortClient = messagePort.getLocalIMessagePort();

        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        Integer.MAX_VALUE);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        Integer.MAX_VALUE);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        Long.MAX_VALUE);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        Long.MAX_VALUE);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
        // 0x1_0000_0000 if cast to an int would be 0. Make sure there's no early unchecked cast!
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        0x1_0000_0000L);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendStringOverFd(afd));
            }
        }
        {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (ParcelFileDescriptor readPipe = pipe[0];
                    ParcelFileDescriptor writePipe = pipe[1]) {
                AssetFileDescriptor afd = new AssetFileDescriptor(readPipe, 0,
                        0x1_0000_0000L);
                Assert.assertThrows(
                        IllegalArgumentException.class,
                        () -> messagePortClient.sendArrayBufferOverFd(afd));
            }
        }
    }

    @Test
    public void testPostMessage_asciiWithinBinderLimit_usesBinder() throws Throwable {
        final String stringMessage = Strings.repeat("x", MAX_BINDER_STRING_LENGTH);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendString(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_asciiExceedingBinderLimit_usesFd() throws Throwable {
        final String stringMessage = Strings.repeat("x", MAX_BINDER_STRING_LENGTH + 1);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                mThreadPoolExecutorService, MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendStringOverFd(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_bmpWithinBinderLimit_usesBinder() throws Throwable {
        final String stringMessage = Strings.repeat("\u03B1", MAX_BINDER_STRING_LENGTH);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendString(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_bmpExceedingBinderLimit_usesFd() throws Throwable {
        final String stringMessage = Strings.repeat("\u03B1", MAX_BINDER_STRING_LENGTH + 1);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                mThreadPoolExecutorService, MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendStringOverFd(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_smpWithinBinderLimit_usesBinder() throws Throwable {
        final String stringMessage = Strings.repeat("\uD83D\uDE00", MAX_BINDER_STRING_LENGTH / 2);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendString(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_smpExceedingBinderLimit_usesFd() throws Throwable {
        final String stringMessage = Strings.repeat("\uD83D\uDE00",
                MAX_BINDER_STRING_LENGTH / 2 + 1);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                mThreadPoolExecutorService, MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postString(stringMessage);
        messagePort.close();

        loggingRemote.assertSendStringOverFd(stringMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_arrayBufferWithinBinderLimit_usesBinder() throws Throwable {
        byte[] arrayMessage = new byte[MAX_BINDER_ARRAY_BUFFER_SIZE];
        Arrays.fill(arrayMessage, (byte) 123);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                new ImmediateFailExecutorService(), MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postArrayBuffer(arrayMessage);
        messagePort.close();

        loggingRemote.assertSendArrayBuffer(arrayMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }

    @Test
    public void testPostMessage_arrayBufferExceedingBinderLimit_usesFd() throws Throwable {
        byte[] arrayMessage = new byte[MAX_BINDER_ARRAY_BUFFER_SIZE + 1];
        Arrays.fill(arrayMessage, (byte) 123);
        LoggingMessagePortIpcClient loggingRemote = new LoggingMessagePortIpcClient();

        MessagePortInternal messagePort = new MessagePortInternal(
                mThreadPoolExecutorService, MAX_MESSAGE_SIZE);
        messagePort.setRemoteIMessagePort(loggingRemote);
        messagePort.postArrayBuffer(arrayMessage);
        messagePort.close();

        loggingRemote.assertSendArrayBufferOverFd(arrayMessage);
        loggingRemote.assertClose();
        loggingRemote.assertEmpty();
    }
}
