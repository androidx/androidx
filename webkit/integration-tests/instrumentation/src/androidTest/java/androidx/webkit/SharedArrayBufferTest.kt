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

package androidx.webkit

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.TestWebMessageListener
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebViewOnUiThread.WaitForLoadedClient
import androidx.webkit.test.common.WebkitUtils
import java.io.ByteArrayInputStream
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [SharedArrayBuffer] and for sending it to web content. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SharedArrayBufferTest {

    private lateinit var webViewOnUiThread: WebViewOnUiThread
    private lateinit var listener: TestWebMessageListener

    @Before
    fun setUp() {
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_SHARED_ARRAY_BUFFER)
        WebkitUtils.checkFeature(WebViewFeature.WEB_MESSAGE_LISTENER)
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)
        WebkitUtils.checkFeature(WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST)

        listener = TestWebMessageListener()

        webViewOnUiThread = WebViewOnUiThread()
        webViewOnUiThread.settings.javaScriptEnabled = true
        webViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, setOf("*"), listener)
    }

    @After
    fun tearDown() {
        if (this::webViewOnUiThread.isInitialized) {
            webViewOnUiThread.cleanUp()
        }
        if (this::listener.isInitialized) {
            setCrossOriginIsolatedAllowList(emptySet())
        }
    }

    private fun setCrossOriginIsolatedAllowList(allowList: Set<String>) {
        WebkitUtils.onMainThreadSync {
            ProfileStore.getInstance()
                .getProfile(Profile.DEFAULT_PROFILE_NAME)!!
                .crossOriginIsolatedAllowlist = allowList
        }
    }

    /** Serves an isolated page, which is a prerequisite for using a SharedArrayBuffer. */
    private class InterceptClient(webViewOnUiThread: WebViewOnUiThread, private val html: String) :
        WaitForLoadedClient(webViewOnUiThread) {
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest,
        ): WebResourceResponse =
            WebResourceResponse(
                /*mimeType=*/ "text/html",
                /*encoding=*/ "utf-8",
                /*statusCode=*/ 200,
                /*reasonPhrase=*/ "OK",
                /*responseHeaders=*/ mapOf(
                    DOCUMENT_ISOLATION_POLICY_HEADER to DOCUMENT_ISOLATION_POLICY_VALUE
                ),
                /*data=*/ ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8)),
            )
    }

    @Test
    fun testCreatedBufferIsSharedBetweenMappings() {
        val sharedArrayBuffer = SharedArrayBuffer.allocate(BUFFER_NAME, BUFFER_SIZE)
        assertEquals(BUFFER_SIZE, sharedArrayBuffer.size)

        val mapping = sharedArrayBuffer.mapAndCreateByteBuffer()
        assertEquals(BUFFER_SIZE.toInt(), mapping.capacity())
        mapping.putInt(0, TRANSFERRED_VALUE)

        // Every mapping of the same buffer observes the same memory.
        val otherMapping = sharedArrayBuffer.mapAndCreateByteBuffer()
        assertEquals(TRANSFERRED_VALUE, otherMapping.getInt(0))

        assertNotSame(mapping, otherMapping)
    }

    @Test
    fun testMappedBufferUsesNativeByteOrder() {
        val sharedArrayBuffer = SharedArrayBuffer.allocate(BUFFER_NAME, BUFFER_SIZE)

        // JavaScript typed arrays use the platform byte order, so the mapping has to match it.
        assertEquals(ByteOrder.nativeOrder(), sharedArrayBuffer.mapAndCreateByteBuffer().order())
    }

    @Test
    fun testAllocateRejectsSizeOutsideOfSupportedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            SharedArrayBuffer.allocate(BUFFER_NAME, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SharedArrayBuffer.allocate(BUFFER_NAME, Int.MAX_VALUE + 1L)
        }
    }

    @Test
    fun testBufferSentToWebContentIsReadableByJavaScript() {
        setCrossOriginIsolatedAllowList(setOf(BASE_ORIGIN))
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, INDEX_HTML)
        webViewOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL)

        val sharedArrayBuffer = SharedArrayBuffer.allocate(BUFFER_NAME, BUFFER_SIZE)
        sharedArrayBuffer.mapAndCreateByteBuffer().putInt(0, TRANSFERRED_VALUE)

        webViewOnUiThread.postWebMessageCompat(
            WebMessageCompat(sharedArrayBuffer),
            Uri.parse(BASE_ORIGIN),
        )

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals(TRANSFERRED_VALUE.toString(), result.mMessage.data)
    }

    @Test
    fun testWebContentWritesThroughReplyProxyBufferAreVisibleToTheApp() {
        setCrossOriginIsolatedAllowList(setOf(BASE_ORIGIN))
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, REPLY_PROXY_HTML)
        webViewOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL)

        val sharedArrayBuffer = SharedArrayBuffer.allocate(BUFFER_NAME, BUFFER_SIZE)
        val mapping = sharedArrayBuffer.mapAndCreateByteBuffer()
        mapping.putInt(0, TRANSFERRED_VALUE)

        // The page announces itself, and the buffer is handed back as a reply to that message.
        val ready: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("ready", ready.mMessage.data)
        WebkitUtils.onMainThreadSync(
            Runnable { ready.mReplyProxy!!.postMessage(sharedArrayBuffer) }
        )

        // Once the page reports back, its write has to be visible through the mapping the app
        // already holds, without anything being copied back.
        val done: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("done", done.mMessage.data)
        assertEquals(TRANSFERRED_VALUE * 2, mapping.getInt(0))
    }

    companion object {
        private const val JS_OBJECT_NAME = "testListener"

        private const val BASE_ORIGIN = "https://www.example.com"
        private const val INDEX_URL = "$BASE_ORIGIN/index.html"

        private const val BUFFER_NAME = "shared-array-buffer-test"
        private const val BUFFER_SIZE = 16L
        private const val TRANSFERRED_VALUE = 42

        private const val DOCUMENT_ISOLATION_POLICY_HEADER = "Document-Isolation-Policy"
        private const val DOCUMENT_ISOLATION_POLICY_VALUE = "isolate-and-credentialless"

        // Reads the first int out of the SharedArrayBuffer posted by the app and reports it back.
        private const val INDEX_HTML =
            """
            <html>
            <body>
            <script>
                window.addEventListener('message', e => {
                    const view = new Int32Array(e.data);
                    ${JS_OBJECT_NAME}.postMessage(String(view[0]));
                });
            </script>
            </body>
            </html>
            """

        // Asks the app for a SharedArrayBuffer, doubles the first int in place, and reports back.
        private const val REPLY_PROXY_HTML =
            """
            <html>
            <body>
            <script>
                ${JS_OBJECT_NAME}.onmessage = e => {
                    const view = new Int32Array(e.data);
                    view[0] = view[0] * 2;
                    ${JS_OBJECT_NAME}.postMessage('done');
                };
                ${JS_OBJECT_NAME}.postMessage('ready');
            </script>
            </body>
            </html>
            """
    }
}
