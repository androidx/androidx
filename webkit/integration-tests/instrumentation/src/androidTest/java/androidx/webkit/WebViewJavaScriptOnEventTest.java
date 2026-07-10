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

package androidx.webkit;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.TestWebMessageListener;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Set;

/**
 * Test {@link WebViewCompat#addJavaScriptOnEvent}.
 *
 * <p>Tests in Chromium JsJavaInteractionTest.java for these APIs are more comprehensive.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewJavaScriptOnEventTest {
    private static final String BASE_URI = "http://www.example.com";
    private static final String JS_OBJECT_NAME = "myObject";
    private static final String BASIC_USAGE = "<!DOCTYPE html><html><body></body></html>";
    private static final String BASIC_SCRIPT = "myObject.postMessage('hello');";
    private static final Set<String> MATCH_EXAMPLE_COM = Collections.singleton(BASE_URI);

    private WebViewOnUiThread mWebViewOnUiThread;
    private final TestWebMessageListener mListener = new TestWebMessageListener();

    @Before
    public void setUp() {
        WebkitUtils.checkFeature(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD);
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
    public void testAddJavaScriptOnEvent_BasicUsage() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                BASIC_SCRIPT, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM,
                world);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddJavaScriptOnEvent_RemoveScript() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        ScriptHandler scriptHandler =
                mWebViewOnUiThread.addJavaScriptOnEvent(
                        BASIC_SCRIPT, WebViewCompat.INJECTION_EVENT_DOCUMENT_START,
                        MATCH_EXAMPLE_COM, world);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        WebkitUtils.onMainThreadSync(scriptHandler::remove);
        loadHtmlSync(BASIC_USAGE);

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddJavaScriptOnEvent_DocumentEnd() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        String script = "myObject.postMessage(document.getElementById('foo').tagName);";
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_END, MATCH_EXAMPLE_COM, world);

        String html = "<!DOCTYPE html><html><body><p id=\"foo\"></p></body></html>";
        loadHtmlSync(html);

        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("P", data.mMessage.getData());

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddJavaScriptOnEvent_MultipleScripts() throws Exception {
        final String secondScript = "myObject.postMessage('world');";
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                BASIC_SCRIPT, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM,
                world);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                secondScript, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM,
                world);

        loadHtmlSync(BASIC_USAGE);

        TestWebMessageListener.Data data1 = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data1.mMessage.getData());

        TestWebMessageListener.Data data2 = mListener.waitForOnPostMessage();
        Assert.assertEquals("world", data2.mMessage.getData());

        Assert.assertTrue("No more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
