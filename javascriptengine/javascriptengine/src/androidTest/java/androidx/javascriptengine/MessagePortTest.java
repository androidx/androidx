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

import androidx.javascriptengine.common.MessagePortInternal;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.testutils.PollingCheck;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for the {@link MessagePort} class.
 * <p>
 * These are the main end-to-end tests for message ports in JavaScriptEngine.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MessagePortTest {
    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_EVALUATION_RETURN_SIZE = 2 * 1024 * 1024; // 2MiB
    private static final String PORT_NAME = "TestPort";
    private static final String GET_PORT_CODE =
            "let port;"
                    + "(async () => {"
                    + "    port = await android.getNamedPort('TestPort');"
                    + "})()";
    private static final String ECHO_MESSAGE_CODE =
            "let port;"
                    + "(async () => {"
                    + "    port = await android.getNamedPort('TestPort');"
                    + "    port.onmessage = (event) => port.postMessage(event.data);"
                    + "})()";
    private static final String QUEUE_MESSAGES_CODE =
            "let port;"
                    + "const queuedMessages = [];"
                    + "const messagePromises = [];"
                    + "async function readMessage() {"
                    + "    if (queuedMessages.length > 0) {"
                    + "        return queuedMessages.shift();"
                    + "    } else {"
                    + "        const promise = new Promise((resolve) => {"
                    + "            messagePromises.push(resolve);"
                    + "        });"
                    + "        return await promise;"
                    + "    }"
                    + "}"
                    + "function writeMessage(message) {"
                    + "    port.postMessage(message);"
                    + "}"
                    + "async function expectMessage(expected) {"
                    + "    const message = await readMessage();"
                    + "    if (typeof(message) === 'string' && typeof(expected) === 'string') {"
                    + "        if (message !== expected) {"
                    + "            throw `Message was not as expected. Received '${message}'`;"
                    + "        }"
                    + "    } else if (message instanceof ArrayBuffer && expected instanceof "
                    + "ArrayBuffer) {"
                    + "        if (new Uint8Array(message).toHex() !== new Uint8Array(expected)"
                    + ".toHex()) {"
                    + "            throw `Message was not as expected. Received bytes ${new "
                    + "Uint8Array(message).toHex()}`;"
                    + "        }"
                    + "    } else {"
                    + "        throw `Message or expected has unsupported or mismatching type.`;"
                    + "    }"
                    + "}"
                    + "function expectNoMessages() {"
                    + "    if (queuedMessages.length > 0) {"
                    + "        throw `Message is present in the queue but none expected.`;"
                    + "    }"
                    + "}"
                    + "(async () => {"
                    + "    port = await android.getNamedPort('TestPort');"
                    + "    port.onmessage = (event) => {"
                    + "        if (messagePromises.length > 0) {"
                    + "            messagePromises.shift().resolve(event.data);"
                    + "        } else {"
                    + "            queuedMessages.push(event.data);"
                    + "        }"
                    + "    };"
                    + "})()";

    // "Small" is within the Binder message length threshold.
    private static final String SMALL_ASCII_STRING = generateAsciiString(16);
    private static final String SMALL_BMP_STRING = generateBmpString(16);
    private static final String SMALL_SMP_STRING = generateSmpString(16);
    private static final String SMALL_UNPAIRED_SURROGATES_STRING = generateUnpairedSurrogates(16);
    private static final byte[] SMALL_ARRAY = generateByteArray(16);

    // "Large" exceeds the Binder message length threshold, but not the isolate return size limit.
    private static final String LARGE_ASCII_STRING = generateAsciiString(131072);
    private static final String LARGE_BMP_STRING = generateBmpString(131072);
    private static final String LARGE_SMP_STRING = generateSmpString(131072);
    private static final String LARGE_UNPAIRED_SURROGATES_STRING =
            generateUnpairedSurrogates(131072);
    private static final byte[] LARGE_ARRAY = generateByteArray(131072);

    private static final MessagePortClient IGNORE_INCOMING_MESSAGES =
            message -> {};
    private static final MessagePortClient EXPECT_NO_INCOMING_MESSAGES =
            message -> Assert.fail("Message received when none were expected.");

    private JavaScriptSandbox mJsSandbox;
    private JavaScriptIsolate mJsIsolate;

    private static String generateString(int length, char startChar) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) (startChar + (i & 16)));
        }
        return sb.toString();
    }

    // Generate a repeating pattern string of "ABCDEFGHIJKLMNOP"...
    // (Uses letters to reduce potential confusion with integers.)
    private static String generateAsciiString(int length) {
        return generateString(length, 'A');
    }

    // Generate a repeating pattern string of "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠ"...
    // (Basic Multilingual Plane)
    private static String generateBmpString(int length) {
        return generateString(length, '\u0391');
    }

    // Generate a repeating pattern string of "😀😁😂😃😄😅😆😇😈😉😊😋😌😍😎😏"...
    // (Supplementary Multilingual Plane, which uses surrogate pairs.)
    private static String generateSmpString(int lengthInPairs) {
        StringBuilder sb = new StringBuilder(lengthInPairs * 2);
        for (int i = 0; i < lengthInPairs; i++) {
            sb.append('\uD83D').append((char) ('\uDE00' + (i & 16)));
        }
        return sb.toString();
    }

    // Generate a repeating pattern string of unpaired low surrogates.
    private static String generateUnpairedSurrogates(int length) {
        return generateString(length, '\uDE00');
    }

    // Generate a repeating pattern byte array like {0, 1, 2, ..., 254, 255, 0, 1, ...}
    private static byte[] generateByteArray(int length) {
        byte[] array = new byte[length];
        for (int i = 0; i < length; i++) {
            array[i] = (byte) i;
        }
        return array;
    }

    // Converts every character of a string into "\\uXXXX" equivalents.
    // For example, "Hi" becomes "\\u0048\\u0069", "😀" becomes "\\uD83D\\uDE00".
    private static String escape(String string) {
        StringBuilder sb = new StringBuilder(string.length() * 6);
        for (int i = 0; i < string.length(); i++) {
            sb.append(String.format("\\u%04X", (int) string.charAt(i)));
        }
        return sb.toString();
    }

    // Converts a byte array to a hexadecimal string.
    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            sb.append(String.format("%02x", aByte));
        }
        return sb.toString();
    }

    // Create code for inlining a JS ArrayBuffer into an evaluation.
    private static String jsArrayBuffer(byte[] bytes) {
        return "(Uint8Array.fromHex('" + hex(bytes) + "').buffer)";
    }

    private static List<Message> generateComplexMessageSequence() {
        // This will produce about 32 * (18 + 18 + 128Ki + 128Ki) bytes (~8194KiB) of message data
        // over 128 total messages.
        final int numRounds = 32;
        List<Message> messages = new ArrayList<>();
        for (int round = 0; round < numRounds; round++) {
            byte[] smallArrayBuffer = SMALL_ARRAY;
            smallArrayBuffer = smallArrayBuffer.clone();
            smallArrayBuffer[0] = (byte) round;
            byte[] largeArrayBuffer = LARGE_ARRAY;
            largeArrayBuffer = largeArrayBuffer.clone();
            largeArrayBuffer[0] = (byte) round;
            messages.add(Message.createStringMessage(round + SMALL_ASCII_STRING));
            messages.add(Message.createStringMessage(round + LARGE_ASCII_STRING));
            messages.add(Message.createArrayBufferMessage(smallArrayBuffer));
            messages.add(Message.createArrayBufferMessage(largeArrayBuffer));
        }
        // Shuffle so that there's more variety to the order of small and large messages.
        Collections.shuffle(messages, new Random(0));
        return messages;
    }

    @Before
    public void setup() throws Throwable {
        Assume.assumeTrue(JavaScriptSandbox.isSupported());
        ListenableFuture<JavaScriptSandbox> jsSandboxFuture =
                JavaScriptSandbox.createConnectedInstanceAsync(
                        ApplicationProvider.getApplicationContext());
        mJsSandbox = jsSandboxFuture.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assume.assumeTrue(
                mJsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_MESSAGE_PORTS));
        Assume.assumeTrue(
                mJsSandbox.isFeatureSupported(JavaScriptSandbox.JS_FEATURE_PROMISE_RETURN));
        Assume.assumeTrue(
                mJsSandbox.isFeatureSupported(
                        JavaScriptSandbox.JS_FEATURE_EVALUATE_WITHOUT_TRANSACTION_LIMIT));
        IsolateStartupParameters isolateStartupParameters = new IsolateStartupParameters();
        isolateStartupParameters.setMaxEvaluationReturnSizeBytes(MAX_EVALUATION_RETURN_SIZE);
        mJsIsolate = mJsSandbox.createIsolate(isolateStartupParameters);
    }

    @After
    public void teardown() {
        if (mJsIsolate != null) {
            mJsIsolate.close();
            mJsIsolate = null;
        }
        if (mJsSandbox != null) {
            mJsSandbox.close();
            mJsSandbox = null;
        }
    }

    @Test
    public void testProvideMessagePort_nullPortName_throws() throws Throwable {
        Assert.assertThrows(
                NullPointerException.class,
                () -> mJsIsolate.createMessageChannel(null, MoreExecutors.directExecutor(),
                        EXPECT_NO_INCOMING_MESSAGES));
    }

    @Test
    public void testProvideMessagePort_nullExecutor_throws() throws Throwable {
        Assert.assertThrows(
                NullPointerException.class,
                () -> mJsIsolate.createMessageChannel(PORT_NAME, null,
                        EXPECT_NO_INCOMING_MESSAGES));
    }

    @Test
    public void testProvideMessagePort_nullClient_throws() throws Throwable {
        Assert.assertThrows(
                NullPointerException.class,
                () -> mJsIsolate.createMessageChannel(PORT_NAME, MoreExecutors.directExecutor(),
                        null));
    }

    @Test
    public void testProvideMessagePort_portNameAlreadyUsed_throws() throws Throwable {
        MessagePort firstPort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertThrows(
                IllegalStateException.class,
                () -> mJsIsolate.createMessageChannel(PORT_NAME, MoreExecutors.directExecutor(),
                        EXPECT_NO_INCOMING_MESSAGES));
    }

    @Test
    public void testProvideMessagePort_closedIsolate_throws() throws Throwable {
        mJsIsolate.close();

        Assert.assertThrows(
                IllegalStateException.class,
                () -> mJsIsolate.createMessageChannel(PORT_NAME, MoreExecutors.directExecutor(),
                        EXPECT_NO_INCOMING_MESSAGES));
    }

    @Test
    public void testProvideMessagePort_liveIsolate_returnsEntangledPort() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertNotNull(messagePort.getMessagePortInternalForTest().mRemoteIMessagePort.get());
    }

    @Test
    public void testProvideMessagePort_closedSandbox_returnsDisentangledPort() throws Throwable {
        mJsSandbox.close();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertNull(messagePort.getMessagePortInternalForTest().mRemoteIMessagePort.get());
        // Messages posted here should be silently dropped and not crash.
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testProvideMessagePort_unboundSandbox_returnsDisentangledPort() throws Throwable {
        // Simulates a service death which hasn't yet been handled.
        mJsSandbox.unbindService();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertNull(messagePort.getMessagePortInternalForTest().mRemoteIMessagePort.get());
        // Messages posted here should be silently dropped and not crash.
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testProvideMessagePort_killedSandbox_returnsDisentangledPort() throws Throwable {
        // Kill the sandbox and make sure all isolates are placed into a dead environment state.
        mJsSandbox.killImmediatelyOnThread();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertNull(messagePort.getMessagePortInternalForTest().mRemoteIMessagePort.get());
        // Messages posted here should be silently dropped and not crash.
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_echoingIsolateAndProvidingPortBeforeEval_echoesMessage()
            throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(), messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));

        Assert.assertEquals(SMALL_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(SMALL_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertEquals(LARGE_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(LARGE_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
    }

    @Test
    public void testAppPostMessage_echoingIsolateAndProvidingPortAfterEval_echoesMessage()
            throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        final ListenableFuture<String> earlyEval =
                mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE);
        mJsIsolate.evaluateJavaScriptAsync("'Dummy eval used for synchronization'")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertFalse(earlyEval.isDone());
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(), messageQueue::add);
        earlyEval.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));

        Assert.assertEquals(SMALL_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(SMALL_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertEquals(LARGE_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(LARGE_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
    }

    @Test
    public void testAppPostMessage_isolateClosedWithoutGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_isolateClosedAfterGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_isolateClosedAfterSettingOnMessage_silentlyDiscards()
            throws Throwable {
        // Use IGNORE_INCOMING_MESSAGES rather than EXPECT_NO_INCOMING_MESSAGES, as closing an
        // isolate is not a synchronous operation. The isolate thread may continue to execute code
        // and process messages (sending and receiving) for a short while after being told to close.
        //
        // The most important thing to verify is that we do not throw or crash!
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                IGNORE_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxClosedWithoutGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsSandbox.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxClosedAfterGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsSandbox.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxClosedAfterSettingOnMessage_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsSandbox.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxUnboundWithoutGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        // Simulates a service death which hasn't yet been handled.
        mJsSandbox.unbindService();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxUnboundAfterGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Simulates a service death which hasn't yet been handled.
        mJsSandbox.unbindService();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxUnboundAfterSettingOnMessage_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Simulates a service death which hasn't yet been handled.
        mJsSandbox.unbindService();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxKilledWithoutGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        // Kill the sandbox and make sure all isolates are placed into a dead environment state.
        mJsSandbox.killImmediatelyOnThread();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxKilledAfterGettingPort_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Kill the sandbox and make sure all isolates are placed into a dead environment state.
        mJsSandbox.killImmediatelyOnThread();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testAppPostMessage_sandboxKilledAfterSettingOnMessage_silentlyDiscards()
            throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(ECHO_MESSAGE_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Kill the sandbox and make sure all isolates are placed into a dead environment state.
        mJsSandbox.killImmediatelyOnThread();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
    }

    @Test
    public void testPostMessage_appClosedPort_silentlyDiscards() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        messagePort.close();
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(" + jsArrayBuffer(SMALL_ARRAY) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(" + jsArrayBuffer(LARGE_ARRAY) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Note: this could technically pass due to races if the code was faulty.
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectNoMessages();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testPostMessage_isolateClosedPort_silentlyDiscards() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        mJsIsolate.evaluateJavaScriptAsync("port.close()")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(SMALL_ARRAY));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createArrayBufferMessage(LARGE_ARRAY));
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(" + jsArrayBuffer(SMALL_ARRAY) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(" + jsArrayBuffer(LARGE_ARRAY) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Note: this could technically pass due to races if the code was faulty.
        mJsIsolate.evaluateJavaScriptAsync("expectNoMessages();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testAppCloseIsolate_unclaimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even without the isolate claiming it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsIsolate.close();

        PollingCheck.waitFor(TIMEOUT_SECONDS * 1000,
                () -> messagePortInternal.mRemoteIMessagePort.get() == null);
    }

    @Test
    public void testAppCloseIsolate_claimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsIsolate.close();

        PollingCheck.waitFor(TIMEOUT_SECONDS * 1000,
                () -> messagePortInternal.mRemoteIMessagePort.get() == null);
    }

    @Test
    public void testAppKillSandbox_unclaimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even without the isolate claiming it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsSandbox.kill();

        PollingCheck.waitFor(TIMEOUT_SECONDS * 1000,
                () -> messagePortInternal.mRemoteIMessagePort.get() == null);
    }

    @Test
    public void testAppKillSandbox_claimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsSandbox.kill();

        PollingCheck.waitFor(TIMEOUT_SECONDS * 1000,
                () -> messagePortInternal.mRemoteIMessagePort.get() == null);
    }

    @Test
    public void testAppClose_unclaimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even without the isolate claiming it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        messagePort.close();

        Assert.assertNull(messagePortInternal.mRemoteIMessagePort.get());
    }

    @Test
    public void testAppClosePort_claimedByIsolate_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        messagePort.close();

        Assert.assertNull(messagePortInternal.mRemoteIMessagePort.get());
    }

    @Test
    public void testIsolateClosePort_unsetsRemote() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertNull(messagePortInternal.mRemoteIMessagePort.get());
    }

    @Test
    public void testMultipleClosePort_appFirst_noCrash() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        messagePort.close();
        messagePort.close();
        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertNull(messagePortInternal.mRemoteIMessagePort.get());
    }

    @Test
    public void testMultipleClosePort_isolateFirst_noCrash() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        MessagePortInternal messagePortInternal = messagePort.getMessagePortInternalForTest();
        // The remote should exist even before the isolate claims it.
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());
        mJsIsolate.evaluateJavaScriptAsync(GET_PORT_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertNotNull(messagePortInternal.mRemoteIMessagePort.get());

        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.close();
        messagePort.close();

        Assert.assertNull(messagePortInternal.mRemoteIMessagePort.get());
    }

    @Test
    public void testAppPostMessage_emptyFromApp_receivedInIsolate() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(""));
        messagePort.postMessage(Message.createArrayBufferMessage(new byte[0]));

        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage(new ArrayBuffer(0));")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testIsolatePostMessage_emptyFromIsolate_receivedInApp() throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(new ArrayBuffer(0));")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertEquals("",
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(new byte[0],
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
    }

    @Test
    public void testAppPostMessage_unicodeFromApp_receivedInIsolate() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(SMALL_ASCII_STRING));
        messagePort.postMessage(Message.createStringMessage(SMALL_BMP_STRING));
        messagePort.postMessage(Message.createStringMessage(SMALL_SMP_STRING));
        messagePort.postMessage(Message.createStringMessage(LARGE_ASCII_STRING));
        messagePort.postMessage(Message.createStringMessage(LARGE_BMP_STRING));
        messagePort.postMessage(Message.createStringMessage(LARGE_SMP_STRING));

        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(SMALL_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(SMALL_BMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(SMALL_SMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(LARGE_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(LARGE_BMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(LARGE_SMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testIsolatePostMessage_unicodeFromIsolate_receivedInApp() throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_BMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_SMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_ASCII_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_BMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_SMP_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertEquals(SMALL_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(SMALL_BMP_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(SMALL_SMP_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(LARGE_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(LARGE_BMP_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(LARGE_SMP_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
    }

    @Test
    public void testIsolatePostMessage_manyTimes_receivedInAppInOrder() throws Throwable {
        List<Message> messages = generateComplexMessageSequence();
        // Parse and buffer all message data into the sandbox up-front to minimize delays between
        // the actual posting of messages.
        // Evaluations must happen in order, so we don't immediately need to await using `get()`.
        mJsIsolate.evaluateJavaScriptAsync("const outbox = [];");
        for (Message message : messages) {
            switch (message.getType()) {
                case Message.TYPE_STRING:
                    mJsIsolate.evaluateJavaScriptAsync(
                            "outbox.push('" + escape(message.getString()) + "');");
                    break;
                case Message.TYPE_ARRAY_BUFFER:
                    mJsIsolate.evaluateJavaScriptAsync(
                            "outbox.push(" + jsArrayBuffer(message.getArrayBuffer()) + ");");
                    break;
                default:
                    Assert.fail("Unsupported message type");
            }
        }
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                messageQueue::add);
        // Use `get()` here to wait for all the evaluations to complete, so they can all be sent
        // together in as short a time as possible.
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "for (const message of outbox) writeMessage(message);")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Note that messages may still be in transit at this point.
        for (Message expected : messages) {
            Message actual = messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            switch (expected.getType()) {
                case Message.TYPE_STRING:
                    Assert.assertEquals(expected.getString(), actual.getString());
                    break;
                case Message.TYPE_ARRAY_BUFFER:
                    Assert.assertArrayEquals(expected.getArrayBuffer(), actual.getArrayBuffer());
                    break;
                default:
                    Assert.fail("Unsupported message type");
            }
        }
    }

    @Test
    public void testAppPostMessage_manyTimes_receivedInIsolateInOrder() throws Throwable {
        List<Message> messages = generateComplexMessageSequence();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        for (Message message : messages) {
            messagePort.postMessage(message);
        }

        // Note that messages may still be in transit at this point.
        for (Message expected : messages) {
            switch (expected.getType()) {
                case Message.TYPE_STRING:
                    mJsIsolate.evaluateJavaScriptAsync(
                                    "expectMessage('" + escape(expected.getString()) + "');")
                            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    break;
                case Message.TYPE_ARRAY_BUFFER:
                    mJsIsolate.evaluateJavaScriptAsync(
                                    "expectMessage(" + jsArrayBuffer(expected.getArrayBuffer())
                                            + ");")
                            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    break;
                default:
                    Assert.fail("Unsupported message type");
            }
        }
    }

    // We don't generally support unpaired surrogates, but we want to be aware of any changes in
    // behavior in case this presents compatibility problems.
    //
    // The direction (app to isolate vs isolate to app) and type of transport (Binder vs FD) can
    // result in different mangling (see tests for up-to-date specifics).
    //
    // Whilst both Java and V8 strings are UTF-16 [1] and can represent unpaired surrogates in
    // strings, but there are some intermediate conversions, including to/from UTF-8, that can
    // happen at different stages. Unpaired surrogates aren't allowed in UTF-8 [2].
    //
    // Depending on what touches the string first, invalid code points or code units might get
    // mangled into '?' or U+FFFD. The number of replacement characters may depend on whether it
    // mangles code points or code units (3 per surrogate if illegally encoded in UTF-8).
    //
    // - (App-to-isolate, Binder): 1x U+FFFD per unpaired surrogate
    //   1. Send Java String (UTF-16) via Binder
    //   2. Converted to UTF-8 in JNI call to C++
    //   3. Converted to V8 string by v8::String::NewFromUtf8
    //
    // - (App-to-isolate, FD): 1x '?' per unpaired surrogate
    //   1. Convert to UTF-8 in app
    //   2. Send UTF-8 over FD
    //   3. Read and decode into Java string (UTF-16)
    //   4. Converted to UTF-8 in JNI call to C++
    //   5. Converted to V8 string by v8::String::NewFromUtf8
    //
    // - (Isolate-to-app, Binder): 3x U+FFFD per unpaired surrogate
    //   1. gin::Converter::FromV8 to UTF-8
    //   2. Converted to Java String (UTF-16) in JNI call to Java
    //   3. Send Java String (UTF-16) via Binder
    //
    // - (Isolate-to-app, FD): 3x U+FFFD per unpaired surrogate
    //   1. gin::Converter::FromV8 to UTF-8
    //   2. Converted to Java String (UTF-16) in JNI call to Java
    //   3. Convert to UTF-8 in isolate Java
    //   4. Send UTF-8 over FD
    //   5. Read and decode into Java string (UTF-16)
    //
    // [1] Both Java and V8 Strings may opportunistically use one byte per character internally for
    // strings that contain only ASCII/Latin-1 characters. These are called Compact Strings in Java.
    //
    // [2] Technically, they're not allowed in UTF-16 either, but that doesn't stop anyone.
    @Test
    public void testAppPostMessage_unpairedSurrogatesFromApp_mangledInIsolate() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(SMALL_UNPAIRED_SURROGATES_STRING));
        messagePort.postMessage(Message.createStringMessage(LARGE_UNPAIRED_SURROGATES_STRING));

        // Binder and FD mechanisms result in differently mangled strings!
        final String smallReplacementString =
                Strings.repeat("\uFFFD", SMALL_UNPAIRED_SURROGATES_STRING.length());
        final String largeReplacementString =
                Strings.repeat("?", LARGE_UNPAIRED_SURROGATES_STRING.length());
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(smallReplacementString) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(largeReplacementString) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testIsolatePostMessage_unpairedSurrogatesFromIsolate_mangledInApp()
            throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(SMALL_UNPAIRED_SURROGATES_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(LARGE_UNPAIRED_SURROGATES_STRING) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertEquals(
                Strings.repeat("\uFFFD", SMALL_UNPAIRED_SURROGATES_STRING.length() * 3),
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(
                Strings.repeat("\uFFFD", LARGE_UNPAIRED_SURROGATES_STRING.length() * 3),
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
    }

    @Test
    public void testPostMessage_nullMessage_throws() throws Throwable {
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);

        Assert.assertThrows(
                NullPointerException.class,
                () -> messagePort.postMessage((Message) null));
    }

    @Test
    public void testAppPostMessageExceedingIsolateReturnSizeLimit_allowed() throws Throwable {
        final String asciiPayload = generateAsciiString(MAX_EVALUATION_RETURN_SIZE + 1);
        final String bmpPayload = generateBmpString(MAX_EVALUATION_RETURN_SIZE / 2 + 1);
        final byte[] arrayPayload = generateByteArray(MAX_EVALUATION_RETURN_SIZE + 1);
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        messagePort.postMessage(Message.createStringMessage(asciiPayload));
        messagePort.postMessage(Message.createStringMessage(bmpPayload));
        messagePort.postMessage(Message.createArrayBufferMessage(arrayPayload));

        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(asciiPayload) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage('" + escape(bmpPayload) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "expectMessage(" + jsArrayBuffer(arrayPayload) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void testIsolatePostMessageWithinIsolateReturnSizeLimit_allowed() throws Throwable {
        final String asciiPayload = generateAsciiString(MAX_EVALUATION_RETURN_SIZE);
        final String bmpPayload = generateBmpString(MAX_EVALUATION_RETURN_SIZE / 2);
        final byte[] arrayPayload = generateByteArray(MAX_EVALUATION_RETURN_SIZE);
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(asciiPayload) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage('" + escape(bmpPayload) + "');")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                        "writeMessage(" + jsArrayBuffer(arrayPayload) + ");")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        Assert.assertEquals(asciiPayload,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertEquals(bmpPayload,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(arrayPayload,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
    }

    @Test
    public void testIsolatePostAsciiStringExceedingIsolateReturnSizeLimit_killsIsolate()
            throws Throwable {
        final String asciiPayload = generateAsciiString(MAX_EVALUATION_RETURN_SIZE + 1);
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            mJsIsolate.evaluateJavaScriptAsync(
                            "writeMessage('" + escape(asciiPayload) + "');")
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.fail("Should have thrown.");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IsolateTerminatedException)) {
                throw e;
            }
        }
    }

    @Test
    public void testIsolatePostBmpStringExceedingIsolateReturnSizeLimit_killsIsolate()
            throws Throwable {
        final String bmpPayload = generateBmpString(MAX_EVALUATION_RETURN_SIZE / 2 + 1);
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            mJsIsolate.evaluateJavaScriptAsync(
                            "writeMessage('" + escape(bmpPayload) + "');")
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.fail("Should have thrown.");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IsolateTerminatedException)) {
                throw e;
            }
        }
    }

    @Test
    public void testIsolatePostArrayExceedingIsolateReturnSizeLimit_killsIsolate()
            throws Throwable {
        final byte[] arrayPayload = generateByteArray(MAX_EVALUATION_RETURN_SIZE + 1);
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME,
                MoreExecutors.directExecutor(),
                EXPECT_NO_INCOMING_MESSAGES);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        try {
            mJsIsolate.evaluateJavaScriptAsync(
                            "writeMessage(" + jsArrayBuffer(arrayPayload) + ");")
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            Assert.fail("Should have thrown.");
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof IsolateTerminatedException)) {
                throw e;
            }
        }
    }

    private static class EchoingMessagePortClient implements MessagePortClient {
        @NonNull
        private final String mPortName;
        @NonNull
        private final MessagePort mMessagePort;

        EchoingMessagePortClient(@NonNull JavaScriptIsolate isolate,
                @NonNull Executor executor, @NonNull String portName) {
            mPortName = portName;
            mMessagePort = isolate.createMessageChannel(portName, executor, this);
        }

        @Override
        public void onMessage(@NonNull Message message) {
            Assert.assertEquals(mPortName, message.getString());
            mMessagePort.postMessage(message);
        }
    }

    private static ListenableFuture<String> evaluateInIsolateForEchoingMessagePortClient(
            @NonNull JavaScriptIsolate isolate, @NonNull String portName) {
        return isolate.evaluateJavaScriptAsync(
                "(async () => {"
                        + "    let port = await android.getNamedPort('" + portName + "');"
                        + "    let promise = new Promise((resolve, reject) => {"
                        + "        port.onmessage = (event) => {"
                        + "            if (event.data !== '" + portName + "') {"
                        + "                reject('Unexpected message');"
                        + "            }"
                        + "            resolve(event.data);"
                        + "        };"
                        + "    });"
                        + "    port.postMessage('" + portName + "');"
                        + "    return await promise;"
                        + "})()");
    }

    @Test
    public void testMultipleMessagePortsInSingleIsolate_succeeds() throws Throwable {
        // Scramble the order of things for extra kicks.
        ListenableFuture<String> result4 =
                evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port4");
        new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port1");
        ListenableFuture<String> result1 =
                evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port1");
        new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port2");
        new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port3");
        ListenableFuture<String> result2 =
                evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port2");
        ListenableFuture<String> result3 =
                evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port3");
        new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port4");

        Assert.assertEquals("Port1", result1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertEquals("Port2", result2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertEquals("Port3", result3.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        Assert.assertEquals("Port4", result4.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleMessagePortsInMultipleIsolates_succeeds() throws Throwable {
        JavaScriptIsolate alternateIsolate = null;
        try {
            // Port   | Created in | Isolate 1 alive | Isolate 2 alive
            // -------|------------|-----------------|----------------
            // Port 1 | Isolate 1  | Yes             | No
            // Port 2 | Isolate 1  | Yes             | Yes
            // Port 3 | Isolate 2  | Yes             | Yes
            // Port 4 | Isolate 2  | No              | Yes
            new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port1");
            ListenableFuture<String> result1 =
                    evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port1");
            result1.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            alternateIsolate = mJsSandbox.createIsolate();
            new EchoingMessagePortClient(mJsIsolate, MoreExecutors.directExecutor(), "Port2");
            new EchoingMessagePortClient(alternateIsolate, MoreExecutors.directExecutor(), "Port3");
            ListenableFuture<String> result2 =
                    evaluateInIsolateForEchoingMessagePortClient(mJsIsolate, "Port2");
            ListenableFuture<String> result3 =
                    evaluateInIsolateForEchoingMessagePortClient(alternateIsolate, "Port3");
            result2.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            result3.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            mJsIsolate.close();
            new EchoingMessagePortClient(alternateIsolate, MoreExecutors.directExecutor(), "Port4");
            ListenableFuture<String> result4 =
                    evaluateInIsolateForEchoingMessagePortClient(alternateIsolate, "Port4");
            result4.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            alternateIsolate.close();

            Assert.assertEquals("Port1", result1.get());
            Assert.assertEquals("Port2", result2.get());
            Assert.assertEquals("Port3", result3.get());
            Assert.assertEquals("Port4", result4.get());
        } finally {
            if (alternateIsolate != null) {
                alternateIsolate.close();
            }
        }
    }


    private static final class ManualExecutorService extends AbstractExecutorService {
        private final LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();

        ManualExecutorService() {}

        public boolean runNextIfAvailable() {
            Runnable task = mQueue.poll();
            if (task == null) {
                return false;
            }
            task.run();
            return true;
        }

        @Override
        public void execute(Runnable runnable) {
            mQueue.add(runnable);
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
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testCloseAfterMessagesSent_messagesStillProcessed() throws Throwable {
        final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        final ManualExecutorService manualExecutorService = new ManualExecutorService();
        MessagePort messagePort = mJsIsolate.createMessageChannel(PORT_NAME, manualExecutorService,
                messageQueue::add);
        mJsIsolate.evaluateJavaScriptAsync(QUEUE_MESSAGES_CODE)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage('" + escape(SMALL_ASCII_STRING) + "');");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage(" + jsArrayBuffer(SMALL_ARRAY) + ");");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage('" + escape(LARGE_ASCII_STRING) + "');");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage(" + jsArrayBuffer(LARGE_ARRAY) + ");");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage('" + escape(SMALL_ASCII_STRING) + "');");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage(" + jsArrayBuffer(SMALL_ARRAY) + ");");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage('" + escape(LARGE_ASCII_STRING) + "');");
        mJsIsolate.evaluateJavaScriptAsync(
                "writeMessage(" + jsArrayBuffer(LARGE_ARRAY) + ");");
        mJsIsolate.evaluateJavaScriptAsync("port.close();")
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // At the moment, it doesn't matter which side closes, so close also on the app side to
        // help detect any changes in behavior. Furthermore, the isolate can even be closed (but not
        // killed), and any unprocessed data should continue to be transferred.
        messagePort.close();
        mJsIsolate.close();
        // Our executor service hasn't run anything yet, so the queue should be empty.
        // The library should have internally queued stuff up already.
        Assert.assertTrue(messageQueue.isEmpty());

        // When the executor finally runs, it should have no dependency on the port being open.
        // Formatted this way because the linter doesn't like empty-bodied while loops.
        while (true) {
            if (!manualExecutorService.runNextIfAvailable()) break;
        }

        Assert.assertEquals(SMALL_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(SMALL_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertEquals(LARGE_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(LARGE_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertEquals(SMALL_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(SMALL_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertEquals(LARGE_ASCII_STRING,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getString());
        Assert.assertArrayEquals(LARGE_ARRAY,
                messageQueue.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS).getArrayBuffer());
        Assert.assertTrue(messageQueue.isEmpty());
    }
}
