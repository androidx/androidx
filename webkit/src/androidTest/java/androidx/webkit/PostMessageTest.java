/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.webkit;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.webkit.WebMessagePortCompat.WebMessageCallbackCompat;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PostMessageTest {
    public static final long TIMEOUT = 6000L;

    private WebView mWebView;
    private WebViewOnUiThread mOnUiThread;

    private static final String WEBVIEW_MESSAGE = "from_webview";
    private static final String BASE_URI = "http://www.example.com";

    @Before
    public void setUp() throws Exception {
        mOnUiThread = new WebViewOnUiThread();
        mOnUiThread.getSettings().setJavaScriptEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        if (mOnUiThread != null) {
            mOnUiThread.cleanUp();
        }
    }

    private static final String TITLE_FROM_POST_MESSAGE =
            "<!DOCTYPE html><html><body>"
                    + "    <script>"
                    + "        var received = '';"
                    + "        onmessage = function (e) {"
                    + "            received += e.data;"
                    + "            document.title = received; };"
                    + "    </script>"
                    + "</body></html>";

    // Acks each received message from the message channel with a seq number.
    private static final String CHANNEL_MESSAGE =
            "<!DOCTYPE html><html><body>"
                    + "    <script>"
                    + "        var counter = 0;"
                    + "        onmessage = function (e) {"
                    + "            var myPort = e.ports[0];"
                    + "            myPort.onmessage = function (f) {"
                    + "                myPort.postMessage(f.data + counter++);"
                    + "            }"
                    + "        }"
                    + "   </script>"
                    + "</body></html>";

    private void loadPage(String data) {
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion(BASE_URI, data,
                "text/html", "UTF-8", null);
    }

    private void waitForTitle(final String title) {
        new PollingCheck(TIMEOUT) {
            @Override
            protected boolean check() {
                return mOnUiThread.getTitle().equals(title);
            }
        }.run();
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testSimpleMessageToMainFrame. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Post a string message to main frame and make sure it is received.
    @Test
    public void testSimpleMessageToMainFrame() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse(BASE_URI));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testWildcardOriginMatchesAnything. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Post a string message to main frame passing a wildcard as target origin
    @Test
    public void testWildcardOriginMatchesAnything() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse("*"));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testEmptyStringOriginMatchesAnything. Modifications to
     * this test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Post a string message to main frame passing an empty string as target origin
    @Test
    public void testEmptyStringOriginMatchesAnything() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse(""));
    }

    private void verifyPostMessageToOrigin(Uri origin) throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);

        loadPage(TITLE_FROM_POST_MESSAGE);
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE);
        mOnUiThread.postWebMessageCompat(message, origin);
        waitForTitle(WEBVIEW_MESSAGE);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testMultipleMessagesToMainFrame. Modifications to this
     * test should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Post multiple messages to main frame and make sure they are received in
    // correct order.
    @Test
    public void testMultipleMessagesToMainFrame() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);

        loadPage(TITLE_FROM_POST_MESSAGE);
        for (int i = 0; i < 10; i++) {
            mOnUiThread.postWebMessageCompat(new WebMessageCompat(Integer.toString(i)),
                    Uri.parse(BASE_URI));
        }
        waitForTitle("0123456789");
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testMessageChannel. Modifications to this test should be
     * reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Create a message channel and make sure it can be used for data transfer to/from js.
    @Test
    public void testMessageChannel() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL);
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 3;
        final BlockingQueue<String> queue = new ArrayBlockingQueue<>(messageCount);
        WebkitUtils.onMainThread(() -> {
            for (int i = 0; i < messageCount; i++) {
                channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE + i));
            }
            channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                @Override
                public void onMessage(@NonNull WebMessagePortCompat port,
                        WebMessageCompat message) {
                    queue.add(message.getData());
                }
            });
        });
        // Wait for all the responses to arrive.
        for (int i = 0; i < messageCount; i++) {
            // The JavaScript code simply appends an integer counter to the end of the message it
            // receives, which is why we have a second i on the end.
            String expectedMessageFromJavascript = WEBVIEW_MESSAGE + i + "" + i;
            Assert.assertEquals(expectedMessageFromJavascript,
                    WebkitUtils.waitForNextQueueElement(queue));
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testClose. Modifications to this test should be reflected
     * in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Test that a message port that is closed cannot used to send a message
    @Test
    public void testClose() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL);
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_CLOSE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE);

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        WebkitUtils.onMainThreadSync(() -> {
            try {
                channel[0].close();
                channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
            } catch (IllegalStateException ex) {
                // expect to receive an exception
                return;
            }
            Assert.fail("A closed port cannot be used to transfer messages");
        });
    }

    // Sends a new message channel from JS to Java.
    private static final String CHANNEL_FROM_JS =
            "<!DOCTYPE html><html><body>"
                    + "    <script>"
                    + "        var counter = 0;"
                    + "        var mc = new MessageChannel();"
                    + "        var received = '';"
                    + "        mc.port1.onmessage = function (e) {"
                    + "               received = e.data;"
                    + "               document.title = e.data;"
                    + "        };"
                    + "        onmessage = function (e) {"
                    + "            var myPort = e.ports[0];"
                    + "            myPort.postMessage('', [mc.port2]);"
                    + "        };"
                    + "   </script>"
                    + "</body></html>";

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testReceiveMessagePort. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Test a message port created in JS can be received and used for message transfer.
    @Test
    public void testReceiveMessagePort() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL);
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE);

        final String hello = "HELLO";
        loadPage(CHANNEL_FROM_JS);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        WebkitUtils.onMainThreadSync(() -> {
            channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                @Override
                public void onMessage(@NonNull WebMessagePortCompat port,
                        WebMessageCompat message) {
                    message.getPorts()[0].postMessage(new WebMessageCompat(hello));
                }
            });
        });
        waitForTitle(hello);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testWebMessageHandler. Modifications to this test should
     * be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Ensure the callback is invoked on the correct Handler.
    @Test
    public void testWebMessageHandler() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL);
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE, new
                WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 1;
        final ResolvableFuture<Boolean> messageHandlerThreadFuture = ResolvableFuture.create();

        // Create a new thread for the WebMessageCallbackCompat.
        final HandlerThread messageHandlerThread = new HandlerThread("POST_MESSAGE_THREAD");
        messageHandlerThread.start();
        final Handler messageHandler = new Handler(messageHandlerThread.getLooper());

        WebkitUtils.onMainThreadSync(() -> {
            channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
            channel[0].setWebMessageCallback(messageHandler, new WebMessageCallbackCompat() {
                @Override
                public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
                    messageHandlerThreadFuture.set(
                            messageHandlerThread.getLooper().isCurrentThread());
                }
            });
        });
        // Wait for all the responses to arrive and assert correct thread.
        Assert.assertTrue(WebkitUtils.waitForFuture(messageHandlerThreadFuture));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.PostMessageTest#testWebMessageDefaultHandler. Modifications to this test
     * should be reflected in that test as necessary. See http://go/modifying-webview-cts.
     */
    // Ensure the callback is invoked on the MainLooper by default.
    @Test
    public void testWebMessageDefaultHandler() throws Throwable {
        WebkitUtils.checkFeature(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL);
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK);

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE,
                new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 1;
        final ResolvableFuture<Boolean> messageMainLooperFuture = ResolvableFuture.create();

        WebkitUtils.onMainThread(() -> {
            channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
            channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                @Override
                public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
                    messageMainLooperFuture.set(Looper.getMainLooper().isCurrentThread());
                }
            });
        });
        // Wait for all the responses to arrive and assert correct thread.
        Assert.assertTrue(WebkitUtils.waitForFuture(messageMainLooperFuture));
    }
}
