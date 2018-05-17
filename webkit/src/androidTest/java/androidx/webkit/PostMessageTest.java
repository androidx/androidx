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

import static org.junit.Assume.assumeTrue;

import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.webkit.WebMessagePortCompat.WebMessageCallbackCompat;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@MediumTest
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

    // Post a string message to main frame and make sure it is received.
    @Test
    public void testSimpleMessageToMainFrame() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse(BASE_URI));
    }

    // Post a string message to main frame passing a wildcard as target origin
    @Test
    public void testWildcardOriginMatchesAnything() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse("*"));
    }

    // Post a string message to main frame passing an empty string as target origin
    @Test
    public void testEmptyStringOriginMatchesAnything() throws Throwable {
        verifyPostMessageToOrigin(Uri.parse(""));
    }

    private void verifyPostMessageToOrigin(Uri origin) throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));

        loadPage(TITLE_FROM_POST_MESSAGE);
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE);
        mOnUiThread.postWebMessageCompat(message, origin);
        waitForTitle(WEBVIEW_MESSAGE);
    }

    // Post multiple messages to main frame and make sure they are received in
    // correct order.
    @Test
    public void testMultipleMessagesToMainFrame() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));

        loadPage(TITLE_FROM_POST_MESSAGE);
        for (int i = 0; i < 10; i++) {
            mOnUiThread.postWebMessageCompat(new WebMessageCompat(Integer.toString(i)),
                    Uri.parse(BASE_URI));
        }
        waitForTitle("0123456789");
    }

    // Create a message channel and make sure it can be used for data transfer to/from js.
    @Test
    public void testMessageChannel() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK));

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 3;
        final CountDownLatch latch = new CountDownLatch(messageCount);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < messageCount; i++) {
                    channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE + i));
                }
                channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                    @Override
                    public void onMessage(@NonNull WebMessagePortCompat port,
                            WebMessageCompat message) {
                        int i = messageCount - (int) latch.getCount();
                        Assert.assertEquals(WEBVIEW_MESSAGE + i + i, message.getData());
                        latch.countDown();
                    }
                });
            }
        });
        // Wait for all the responses to arrive.
        Assert.assertTrue(latch.await(TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    // Test that a message port that is closed cannot used to send a message
    @Test
    public void testClose() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_CLOSE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE));

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    channel[0].close();
                    channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
                } catch (IllegalStateException ex) {
                    // expect to receive an exception
                    return;
                }
                Assert.fail("A closed port cannot be used to transfer messages");
            }
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

    // Test a message port created in JS can be received and used for message transfer.
    @Test
    public void testReceiveMessagePort() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE));

        final String hello = "HELLO";
        loadPage(CHANNEL_FROM_JS);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message =
                new WebMessageCompat(WEBVIEW_MESSAGE, new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                    @Override
                    public void onMessage(@NonNull WebMessagePortCompat port,
                            WebMessageCompat message) {
                        message.getPorts()[0].postMessage(new WebMessageCompat(hello));
                    }
                });
            }
        });
        waitForTitle(hello);
    }

    // Ensure the callback is invoked on the correct Handler.
    @Test
    public void testWebMessageHandler() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK));

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE, new
                WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 1;
        final CountDownLatch latch = new CountDownLatch(messageCount);

        // Create a new thread for the WebMessageCallbackCompat.
        final HandlerThread messageHandlerThread = new HandlerThread("POST_MESSAGE_THREAD");
        messageHandlerThread.start();
        final Handler messageHandler = new Handler(messageHandlerThread.getLooper());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
                channel[0].setWebMessageCallback(messageHandler, new WebMessageCallbackCompat() {
                    @Override
                    public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
                        Assert.assertTrue(messageHandlerThread.getLooper().isCurrentThread());
                        latch.countDown();
                    }
                });
            }
        });
        // Wait for all the responses to arrive.
        Assert.assertTrue(latch.await(TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    // Ensure the callback is invoked on the MainLooper by default.
    @Test
    public void testWebMessageDefaultHandler() throws Throwable {
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.POST_WEB_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_PORT_POST_MESSAGE));
        assumeTrue(WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_PORT_SET_MESSAGE_CALLBACK));

        loadPage(CHANNEL_MESSAGE);
        final WebMessagePortCompat[] channel = mOnUiThread.createWebMessageChannelCompat();
        WebMessageCompat message = new WebMessageCompat(WEBVIEW_MESSAGE,
                new WebMessagePortCompat[]{channel[1]});
        mOnUiThread.postWebMessageCompat(message, Uri.parse(BASE_URI));
        final int messageCount = 1;
        final CountDownLatch latch = new CountDownLatch(messageCount);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                channel[0].postMessage(new WebMessageCompat(WEBVIEW_MESSAGE));
                channel[0].setWebMessageCallback(new WebMessageCallbackCompat() {
                    @Override
                    public void onMessage(WebMessagePortCompat port, WebMessageCompat message) {
                        Assert.assertTrue(Looper.getMainLooper().isCurrentThread());
                        latch.countDown();
                    }
                });
            }
        });
        // Wait for all the responses to arrive.
        Assert.assertTrue(latch.await(TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS));
    }
}
