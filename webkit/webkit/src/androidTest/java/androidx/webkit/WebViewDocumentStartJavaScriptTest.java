/*
 * Copyright 2020 The Android Open Source Project
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
 * Test {@link WebViewCompat#addDocumentStartJavaScript()}
 *
 * Test in Chromium JsJavaInteractionTest.java for these APIs are more comprehensive.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewDocumentStartJavaScriptTest {
    private static final String BASE_URI = "http://www.example.com";
    private static final String JS_OBJECT_NAME = "myObject";
    private static final String BASIC_USAGE = "<!DOCTYPE html><html><body></body></html>";
    private static final String BASIC_SCRIPT = "myObject.postMessage('hello');";
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
        WebkitUtils.checkFeature(WebViewFeature.DOCUMENT_START_SCRIPT);
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
    public void testAddDocumentStartJavaScriptBasicUsage() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);
        mWebViewOnUiThread.addDocumentStartJavaScript(BASIC_SCRIPT, MATCH_EXAMPLE_COM);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddDocumentStartJavaScriptRemoveScript() throws Exception {
        mWebViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, MATCH_EXAMPLE_COM, mListener);
        ScriptHandler scriptHandler =
                mWebViewOnUiThread.addDocumentStartJavaScript(BASIC_SCRIPT, MATCH_EXAMPLE_COM);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        WebkitUtils.onMainThread(scriptHandler::remove);
        loadHtmlSync(BASIC_USAGE);

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
