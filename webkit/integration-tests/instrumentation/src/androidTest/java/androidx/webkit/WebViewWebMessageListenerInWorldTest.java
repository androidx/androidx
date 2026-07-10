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

import androidx.concurrent.futures.ResolvableFuture;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.TestWebMessageListener;
import androidx.webkit.test.common.WebViewOnUiThread;
import androidx.webkit.test.common.WebkitUtils;

import org.jspecify.annotations.NonNull;
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
        final ResolvableFuture<String> setVarFuture = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    data.mReplyProxy.executeJavaScript(
                            "window.isolatedWorldVar",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    setVarFuture.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    setVarFuture.setException(error);
                                }
                            });
                });
        Assert.assertEquals("\"isolatedValue\"", WebkitUtils.waitForFuture(setVarFuture));

        // Verify the variable does NOT exist in the page world
        Assert.assertEquals(
                "null", mWebViewOnUiThread.evaluateJavascriptSync("window.isolatedWorldVar"));
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
        final ResolvableFuture<String> mainFrameFuture = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    mainData.mReplyProxy.executeJavaScript(
                            "window.mainFrameMarker",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    mainFrameFuture.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    mainFrameFuture.setException(error);
                                }
                            });
                });
        Assert.assertEquals("\"main\"", WebkitUtils.waitForFuture(mainFrameFuture));

        // Execute JavaScript in iframe and verify it can access iframeMarker
        final ResolvableFuture<String> iframeFuture = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    iframeData.mReplyProxy.executeJavaScript(
                            "window.iframeMarker",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    iframeFuture.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    iframeFuture.setException(error);
                                }
                            });
                });
        Assert.assertEquals("\"iframe\"", WebkitUtils.waitForFuture(iframeFuture));

        // Verify main frame proxy cannot access iframe's variable
        final ResolvableFuture<String> mainAccessIframeFuture = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    mainData.mReplyProxy.executeJavaScript(
                            "window.iframeMarker",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    mainAccessIframeFuture.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    mainAccessIframeFuture.setException(error);
                                }
                            });
                });
        Assert.assertEquals("null", WebkitUtils.waitForFuture(mainAccessIframeFuture));
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
        final ResolvableFuture<String> setVar1Future = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    data1.mReplyProxy.executeJavaScript(
                            "window.worldVar = 'value1'; 'done'",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    setVar1Future.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    setVar1Future.setException(error);
                                }
                            });
                });
        Assert.assertEquals("\"done\"", WebkitUtils.waitForFuture(setVar1Future));

        // Verify world1 can read its own variable
        final ResolvableFuture<String> getVar1Future = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    data1.mReplyProxy.executeJavaScript(
                            "window.worldVar",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    getVar1Future.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    getVar1Future.setException(error);
                                }
                            });
                });
        Assert.assertEquals("\"value1\"", WebkitUtils.waitForFuture(getVar1Future));

        // Verify world2 cannot read world1's variable
        final ResolvableFuture<String> world2AccessFuture = ResolvableFuture.create();
        WebkitUtils.onMainThreadSync(
                () -> {
                    data2.mReplyProxy.executeJavaScript(
                            "window.worldVar",
                            new WebViewOutcomeReceiver<String, JavaScriptExecutionException>() {
                                @Override
                                public void onResult(String result) {
                                    world2AccessFuture.set(result);
                                }

                                @Override
                                public void onError(@NonNull JavaScriptExecutionException error) {
                                    world2AccessFuture.setException(error);
                                }
                            });
                });
        Assert.assertEquals("null", WebkitUtils.waitForFuture(world2AccessFuture));
    }

    private void loadHtmlSync(String html) {
        mWebViewOnUiThread.loadDataWithBaseURLAndWaitForCompletion(
                BASE_URI, html, "text/html", null, null);
    }
}
