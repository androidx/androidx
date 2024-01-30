/*
 * Copyright 2022 The Android Open Source Project
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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test {@link WebMessagePortCompat#postMessage} and {@link
 * WebMessagePortCompat#setWebMessageCallback} basic usages.
 *
 * Test in Chromium tree PostMessageTest.java for these APIs are more comprehensive.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class WebViewWebMessageCompatTest {
    private static final String BASE_URI = "http://www.example.com";
    private static final String ECHO_MESSAGE = "<!DOCTYPE html><html><body>"
            + "    <script>"
            + "        var port;"
            + "        function echo(e) {"
            + "            port.postMessage(e.data);"
            + "            port.postMessage(e.data, [e.data]);"
            + "        }"
            + "        window.onmessage = (e) => {"
            + "            if (e.ports[0]) { "
            + "                port = e.ports[0]; "
            + "                port.onmessage = (event) => echo(event);"
            + "                return; "
            + "            }"
            + "            echo(e);"
            + "        }"
            + "    </script>"
            + "</body></html>";

    private WebViewOnUiThread mWebViewOnUiThread;
    private final TestWebMessageListener mListener = new TestWebMessageListener();
    private final byte[] mBytes = new byte[1000];

    private static class TestWebMessageListener extends
            WebMessagePortCompat.WebMessageCallbackCompat {
        private final BlockingQueue<WebMessageCompat> mQueue = new LinkedBlockingQueue<>();

        @Override
        public void onMessage(@NonNull WebMessagePortCompat port,
                @Nullable WebMessageCompat message) {
            mQueue.add(message);
        }

        public WebMessageCompat waitForOnPostMessage() throws Exception {
            return WebkitUtils.waitForNextQueueElement(mQueue);
        }
    }

    @Before
    public void setUp() {
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebViewOnUiThread.getSettings().setJavaScriptEnabled(true);

        final Random random = new Random(42);
        random.nextBytes(mBytes);
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    private WebMessagePortCompat preparePort() {
        final WebMessagePortCompat[] port = mWebViewOnUiThread.createWebMessageChannelCompat();
        mWebViewOnUiThread.postWebMessageCompat(new WebMessageCompat("setup",
                new WebMessagePortCompat[]{port[1]}), Uri.EMPTY);
        port[0].setWebMessageCallback(new Handler(Looper.getMainLooper()), mListener);
        return port[0];
    }

    private void assertArrayBufferMessage() throws Exception {
        final WebMessageCompat message1 = mListener.waitForOnPostMessage();
        Assert.assertEquals(WebMessageCompat.TYPE_ARRAY_BUFFER, message1.getType());
        Assert.assertArrayEquals(mBytes, message1.getArrayBuffer());

        final WebMessageCompat message2 = mListener.waitForOnPostMessage();
        Assert.assertEquals(WebMessageCompat.TYPE_ARRAY_BUFFER, message2.getType());
        Assert.assertArrayEquals(mBytes, message2.getArrayBuffer());
    }

    @Test
    public void testArrayBufferOverPort() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER);
        loadHtmlSync(ECHO_MESSAGE);
        final WebMessagePortCompat port = preparePort();
        port.postMessage(new WebMessageCompat(mBytes));
        assertArrayBufferMessage();
    }

    @Test
    public void testArrayBufferToMainFrame() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.POST_WEB_MESSAGE);
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER);
        loadHtmlSync(ECHO_MESSAGE);
        preparePort();
        mWebViewOnUiThread.postWebMessageCompat(new WebMessageCompat(mBytes), Uri.EMPTY);
        assertArrayBufferMessage();
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
