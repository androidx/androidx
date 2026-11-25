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
 * Test {@link WebViewCompat#addWebMessageListener} and {@link
 * WebViewCompat#removeWebMessageListener} with {@link JavaScriptExecutionWorld}.
 *
 * <p>Test in Chromium tree JsJavaInteractionTest.java for these APIs are more comprehensive.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class WebViewWebMessageListenerInWorldTest {
    private static final String BASE_URI = "http://www.example.com";
    private static final String JS_OBJECT_NAME = "myWebMessageListener";
    private static final String JS_BASIC_USAGE = "myWebMessageListener.postMessage('hello');";
    private static final String JS_REPLY_PROXY_STORE_GLOBAL =
            "myWebMessageListener.onmessage = function(event) {"
                    + "             window.replyReceived = event.data;"
                    + "        };"
                    + "        myWebMessageListener.postMessage('hello');";
    private static final String BASIC_HTML = "<!DOCTYPE html><html><body></body></html>";
    private static final String BASIC_USAGE =
            "<!DOCTYPE html><html><body>"
                    + "    <script>"
                    + "        myWebMessageListener.postMessage('hello');"
                    + "    </script>"
                    + "</body></html>";

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
    public void testAddWebMessageListenerWithWorld_PageWorld() throws Exception {
        JavaScriptExecutionWorld world =
                mWebViewOnUiThread.getExecutionWorld(JavaScriptExecutionWorld.PAGE_WORLD_NAME);
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        Assert.assertTrue(
                "Should have no more message at this point.", mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddRemoveWebMessageListenerWithWorld_Isolated() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                "myWebMessageListener.postMessage('hello');",
                WebViewCompat.INJECTION_EVENT_DOCUMENT_START,
                MATCH_EXAMPLE_COM,
                world);

        loadHtmlSync(BASIC_HTML);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.removeWebMessageListener(JS_OBJECT_NAME, world);

        loadHtmlSync(BASIC_HTML);
        Assert.assertTrue(
                "No message should be received after removeWebMessageListener",
                mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testAddWebMessageListenerWithWorld_IsolatedSeparateFromPage() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("testWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);

        loadHtmlSync(BASIC_USAGE);
        Assert.assertTrue(
                "No message should be received because the listener is in a different world",
                mListener.hasNoMoreOnPostMessage());
    }

    @Test
    public void testRemoveWebMessageListenerWithWorld_JsObjectExistsWithoutPageLoad()
            throws Exception {
        JavaScriptExecutionWorld world =
                mWebViewOnUiThread.getExecutionWorld(JavaScriptExecutionWorld.PAGE_WORLD_NAME);
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);

        loadHtmlSync(BASIC_USAGE);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        mWebViewOnUiThread.removeWebMessageListener(JS_OBJECT_NAME, world);
        Assert.assertEquals(
                "\"object\"",
                mWebViewOnUiThread.evaluateJavascriptSync("typeof " + JS_OBJECT_NAME + ";"));
    }

    @Test
    public void testAddWebMessageListenerWithWorld_IsolatedWorldsAreIndependent() throws Exception {
        final TestWebMessageListener listener1 = new TestWebMessageListener();
        final TestWebMessageListener listener2 = new TestWebMessageListener();
        final String jsObjectName1 = "listener1";
        final String jsObjectName2 = "listener2";

        JavaScriptExecutionWorld world1 = mWebViewOnUiThread.getExecutionWorld("world1");
        JavaScriptExecutionWorld world2 = mWebViewOnUiThread.getExecutionWorld("world2");
        mWebViewOnUiThread.addWebMessageListener(
                jsObjectName1, MATCH_EXAMPLE_COM, world1, listener1);
        mWebViewOnUiThread.addWebMessageListener(
                jsObjectName2, MATCH_EXAMPLE_COM, world2, listener2);

        String script =
                "if (typeof listener1 !== 'undefined') listener1.postMessage('from1');        if"
                        + " (typeof listener2 !== 'undefined') listener2.postMessage('from2');";
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM, world1);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM, world2);

        loadHtmlSync(BASIC_HTML);

        TestWebMessageListener.Data data1 = listener1.waitForOnPostMessage();
        Assert.assertEquals("from1", data1.mMessage.getData());

        TestWebMessageListener.Data data2 = listener2.waitForOnPostMessage();
        Assert.assertEquals("from2", data2.mMessage.getData());

        Assert.assertTrue("No more messages in listener1", listener1.hasNoMoreOnPostMessage());
        Assert.assertTrue("No more messages in listener2", listener2.hasNoMoreOnPostMessage());
    }

    @Test
    public void testExecuteJavaScript_IsolatedWorld() throws Exception {
        JavaScriptExecutionWorld world = mWebViewOnUiThread.getExecutionWorld("isolatedWorld");
        mWebViewOnUiThread.addWebMessageListener(
                JS_OBJECT_NAME, MATCH_EXAMPLE_COM, world, mListener);
        String script = "window.isolatedWorldVar = 'isolatedValue';" + JS_BASIC_USAGE;
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM, world);

        loadHtmlSync(BASIC_HTML);
        TestWebMessageListener.Data data = mListener.waitForOnPostMessage();
        Assert.assertEquals("hello", data.mMessage.getData());

        // Set a variable in the isolated world via executeJavaScript
        String jsResult = executeJavascriptAndWaitForResult(data, "window.isolatedWorldVar");
        Assert.assertEquals("\"isolatedValue\"", jsResult);

        // Verify the variable does NOT exist in the page world
        Assert.assertEquals(
                "null", mWebViewOnUiThread.evaluateJavascriptSync("window.isolatedWorldVar"));
    }

    private static String executeJavascriptAndWaitForResult(
            TestWebMessageListener.Data data,
            String script) {
        return WebkitUtils.waitForFuture(
                WebkitUtils.<String, JavaScriptExecutionException>callbackToFuture(
                        receiver -> WebkitUtils.onMainThreadSync(
                                () -> {
                                    Assert.assertNotNull(data.mReplyProxy);
                                    data.mReplyProxy.executeJavaScript(
                                            script,
                                            receiver);
                                })));
    }

    @Test
    public void testExecuteJavaScript_InCorrectIframe() throws Exception {
        JavaScriptExecutionWorld world =
                mWebViewOnUiThread.getExecutionWorld(JavaScriptExecutionWorld.PAGE_WORLD_NAME);
        final TestWebMessageListener mainFrameListener = new TestWebMessageListener();
        final TestWebMessageListener iframeListener = new TestWebMessageListener();

        mWebViewOnUiThread.addWebMessageListener(
                "mainFrameListener", MATCH_EXAMPLE_COM, world, mainFrameListener);
        mWebViewOnUiThread.addWebMessageListener(
                "iframeListener", MATCH_EXAMPLE_COM, world, iframeListener);

        String html =
                "<!DOCTYPE html><html><body>"
                        + "    <script>"
                        + "        window.mainFrameMarker = 'main';"
                        + "        mainFrameListener.postMessage('fromMain');"
                        + "    </script>"
                        + "    <iframe srcdoc=\""
                        + "        <script>"
                        + "            window.iframeMarker = 'iframe';"
                        + "            iframeListener.postMessage('fromIframe');"
                        + "        </script>"
                        + "    \"></iframe>"
                        + "</body></html>";
        loadHtmlSync(html);

        TestWebMessageListener.Data mainData = mainFrameListener.waitForOnPostMessage();
        Assert.assertEquals("fromMain", mainData.mMessage.getData());

        TestWebMessageListener.Data iframeData = iframeListener.waitForOnPostMessage();
        Assert.assertEquals("fromIframe", iframeData.mMessage.getData());

        // Execute JavaScript in main frame and verify it can access mainFrameMarker
        String mainFrameMarker = executeJavascriptAndWaitForResult(mainData,
                "window.mainFrameMarker");
        Assert.assertEquals("\"main\"", mainFrameMarker);

        // Execute JavaScript in iframe and verify it can access iframeMarker
        String iframeMarker = executeJavascriptAndWaitForResult(iframeData, "window.iframeMarker");
        Assert.assertEquals("\"iframe\"", iframeMarker);

        // Verify main frame proxy cannot access iframe's variable
        String mainAccessIframeMarker = executeJavascriptAndWaitForResult(mainData,
                "window.iframeMarker");
        Assert.assertEquals("null", mainAccessIframeMarker);
    }

    @Test
    public void testExecuteJavaScript_IsolatedWorldsAreIndependent() throws Exception {
        final TestWebMessageListener listener1 = new TestWebMessageListener();
        final TestWebMessageListener listener2 = new TestWebMessageListener();

        JavaScriptExecutionWorld world1 = mWebViewOnUiThread.getExecutionWorld("world1");
        JavaScriptExecutionWorld world2 = mWebViewOnUiThread.getExecutionWorld("world2");

        mWebViewOnUiThread.addWebMessageListener("listener1", MATCH_EXAMPLE_COM, world1, listener1);
        mWebViewOnUiThread.addWebMessageListener("listener2", MATCH_EXAMPLE_COM, world2, listener2);

        String script =
                "if (typeof listener1 !== 'undefined') listener1.postMessage('from1');        if"
                        + " (typeof listener2 !== 'undefined') listener2.postMessage('from2');";
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM, world1);
        mWebViewOnUiThread.addJavaScriptOnEvent(
                script, WebViewCompat.INJECTION_EVENT_DOCUMENT_START, MATCH_EXAMPLE_COM, world2);

        loadHtmlSync(BASIC_HTML);

        TestWebMessageListener.Data data1 = listener1.waitForOnPostMessage();
        TestWebMessageListener.Data data2 = listener2.waitForOnPostMessage();

        // Set a variable in world1
        String setVar1Result = executeJavascriptAndWaitForResult(data1,
                "window.worldVar = 'value1'; 'done'");
        Assert.assertEquals("\"done\"", setVar1Result);

        // Verify world1 can read its own variable
        String getVar1Result = executeJavascriptAndWaitForResult(data1, "window.worldVar");
        Assert.assertEquals("\"value1\"", getVar1Result);

        // Verify world2 cannot read world1's variable
        String world2AccessResult = executeJavascriptAndWaitForResult(data2, "window.worldVar");
        Assert.assertEquals("null", world2AccessResult);
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
