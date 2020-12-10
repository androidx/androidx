/*
 * Copyright 2019 The Android Open Source Project
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
import android.webkit.WebView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Test {@link WebViewCompat#addWebMessageListener()} and {@link
 * WebViewCompat#removeWebMessageListener()} basic usages.
 *
 * Test in Chromium tree JsJavaInteractionTest.java for these APIs are more comprehensive.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewWebMessageListenerTest {
    private static final String BASE_URI = "http://www.example.com";
    private static final String JS_OBJECT_NAME = "myObject";
    private static final String BASIC_USAGE = "<!DOCTYPE html><html><body>"
            + "    <script>"
            + "        myObject.postMessage('hello');"
            + "    </script>"
            + "</body></html>";
    private static final String REPLY_PROXY = "<!DOCTYPE html><html><body>"
            + "    <script>"
            + "        myObject.onmessage = function(event) {"
            + "             window.replyReceived = event.data;"
            + "        };"
            + "        myObject.postMessage('hello');"
            + "    </script>"
            + "</body></html>";
    private static final Set<String> MATCH_EXAMPLE_COM = new HashSet<>(Arrays.asList(BASE_URI));

    private WebViewOnUiThread mWebViewOnUiThread;
    private TestWebMessageListener mListener = new TestWebMessageListener();

    private static class TestWebMessageListener implements WebViewCompat.WebMessageListener {
        private BlockingQueue<Data> mQueue = new LinkedBlockingQueue<>();

        public static class Data {
            public WebMessageCompat mMessage;
            public Uri mSourceOrigin;
            public boolean mIsMainFrame;
            public JavaScriptReplyProxy mReplyProxy;

            Data(WebMessageCompat message, Uri sourceOrigin, boolean isMainFrame,
                    JavaScriptReplyProxy replyProxy) {
                mMessage = message;
                mSourceOrigin = sourceOrigin;
                mIsMainFrame = isMainFrame;
                mReplyProxy = replyProxy;
            }
        }

        @Override
        public void onPostMessage(WebView webView, WebMessageCompat message, Uri sourceOrigin,
                boolean isMainFrame, JavaScriptReplyProxy replyProxy) {
            mQueue.add(new Data(message, sourceOrigin, isMainFrame, replyProxy));
        }

        public Data waitForOnPostMessage() throws Exception {
            return WebkitUtils.waitForNextQueueElement(mQueue);
        }

        public boolean hasNoMoreOnPostMessage() {
            return mQueue.isEmpty();
        }
    }

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_LISTENER);
        mWebViewOnUiThread = new WebViewOnUiThread();
        mWebViewOnUiThread.getSettings().setJavaScriptEnabled(true);
    }

    @After
    public void tearDown() {
        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    @Test
    public void testAddWebMessageListenerBasicUsage() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // BASIC_USAGE html page will call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        Assert.assertTrue(
                "Should have no more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAfterRemoveWebMessageListener_JsObjectExistWithoutAnotherPageLoad()
            throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // BASIC_USAGE html page will call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.removeWebMessageListener(JS_OBJECT_NAME);
        Assert.assertEquals("\"object\"",
                mWebViewOnUiThread.evaluateJavascriptSync("typeof " + JS_OBJECT_NAME + ";"));
    }

    @Test
    public void testAfterRemoveWebMessageListener_NextPageLoadRemovesJsObject() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // BASIC_USAGE html page will call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.removeWebMessageListener(JS_OBJECT_NAME);

        // We won't inject the JavaScript object for next load.
        loadHtmlSync(BASIC_USAGE);
        Assert.assertEquals("\"undefined\"",
                mWebViewOnUiThread.evaluateJavascriptSync("typeof " + JS_OBJECT_NAME + ";"));
    }

    @Test
    public void testAfterRemoveWebMessageListener_OnPostMessageWontBeCalledEvenJsObjectExists()
            throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // BASIC_USAGE html page will call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.removeWebMessageListener(JS_OBJECT_NAME);
        Assert.assertEquals("\"object\"",
                mWebViewOnUiThread.evaluateJavascriptSync("typeof " + JS_OBJECT_NAME + ";"));

        // With the current implementation, evaluateJavascript() and myObject.postMessage() are
        // in the same IPC channel and the order of them are guaranteed. So if we call
        // myObject.postMessage() by using evaluateJavascript(), we will send postMessage() first
        // then the callback of evaluateJavascript() will be called second. When
        // evaluateJavascriptSync() returns, if there is a postMessage() call reached to the
        // browser side, we should have already received that. We expect no message to reach
        // to the browser side in this test, therefore mListener.hasNoMoreOnPostMessage() should be
        // true.

        // Message sent after removeWebMessageListener() won't reach to WebView.
        mWebViewOnUiThread.evaluateJavascriptSync(JS_OBJECT_NAME + ".postMessage('hello2')");
        Assert.assertTrue(
                "Message should not reach to the app side after removeWebMessageListener()",
                mListener.hasNoMoreOnPostMessage());
    }

    // Test that for one injection of the JavaScript object, we receive the same
    // JavaScriptReplyProxy from different onPostMessage() calls.
    @Test
    public void testWebMessageListenerReplyProxyIsIsomorphic() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // BASIC_USAGE html page will call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.evaluateJavascript(JS_OBJECT_NAME + ".postMessage('hello again')", null);
        TestWebMessageListener.Data data2 = mListener.waitForOnPostMessage();
        Assert.assertSame(data.mReplyProxy, data2.mReplyProxy);
    }

    @Test
    public void testJavaScriptReplyProxyBasicUsage() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);

        // REPLY_PROXY html page will set myObject.onmessage to save the message to
        // window.replyReceived and call myObject.postMessage('hello'); in JavaScript.
        loadHtmlSync(REPLY_PROXY);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        final String message = "reply from Java";
        data.mReplyProxy.postMessage(message);
        Assert.assertEquals("\"" + message + "\"",
                mWebViewOnUiThread.evaluateJavascriptSync("window.replyReceived"));
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
