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

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.webkit.test.common.TestWebMessageListener
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebViewOnUiThread.WaitForLoadedClient
import androidx.webkit.test.common.WebkitUtils
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [Profile.setCrossOriginIsolatedAllowlist] and [Profile.getCrossOriginIsolatedAllowlist].
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class WebViewCrossOriginIsolatedAllowlistTest {

    private lateinit var defaultProfile: Profile
    private lateinit var webViewOnUiThread: WebViewOnUiThread
    private lateinit var listener: TestWebMessageListener

    @Before
    fun setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)
        WebkitUtils.checkFeature(WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST)

        defaultProfile =
            WebkitUtils.onMainThreadSync<Profile> {
                ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)
            }

        listener = TestWebMessageListener()

        webViewOnUiThread = WebViewOnUiThread()
        webViewOnUiThread.settings.javaScriptEnabled = true
        webViewOnUiThread.addWebMessageListener(JS_OBJECT_NAME, setOf("*"), listener)
    }

    @After
    fun cleanUp() {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST)) {
            defaultProfile.crossOriginIsolatedAllowlist = emptySet()
        }
    }

    private class InterceptClient(
        webViewOnUiThread: WebViewOnUiThread,
        val headers: Map<String, String>,
    ) : WaitForLoadedClient(webViewOnUiThread) {
        // Only return content for INDEX_URL, ISOLATED_INDEX_URL and WORKER_URL, deny all other
        // requests.
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest,
        ): WebResourceResponse =
            when (request.url.toString()) {
                INDEX_URL ->
                    WebResourceResponse(
                        /* mimeType = */ "text/html",
                        /* encoding = */ "utf-8",
                        /* statusCode = */ 200,
                        /* reasonPhrase = */ "OK",
                        /* responseHeaders = */ headers,
                        /* data = */ ByteArrayInputStream(
                            INDEX_HTML.toByteArray(StandardCharsets.UTF_8)
                        ),
                    )
                ISOLATE_INDEX_URL ->
                    WebResourceResponse(
                        /* mimeType = */ "text/html",
                        /* encoding = */ "utf-8",
                        /* statusCode = */ 200,
                        /* reasonPhrase = */ "OK",
                        /* responseHeaders = */ headers,
                        /* data = */ ByteArrayInputStream(
                            ISOLATE_INDEX_HTML.toByteArray(StandardCharsets.UTF_8)
                        ),
                    )
                WORKER_URL ->
                    WebResourceResponse(
                        /* mimeType = */ "application/javascript",
                        /* encoding = */ "utf-8",
                        /* statusCode = */ 200,
                        /* reasonPhrase = */ "OK",
                        /* responseHeaders = */ headers,
                        /* data = */ ByteArrayInputStream(
                            WORKER_JS.toByteArray(StandardCharsets.UTF_8)
                        ),
                    )
                else ->
                    WebResourceResponse(
                        /* mimeType = */ "text/plain",
                        /* encoding = */ "utf-8",
                        /* statusCode = */ 404,
                        /* reasonPhrase = */ "Not Found",
                        /* responseHeaders = */ headers,
                        /* data = */ null,
                    )
            }
    }

    @Test
    fun testGetterAndSetterWork() {
        defaultProfile.crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)

        // getCrossOriginIsolatedAllowlist returns origin rules with ports explicitly written.
        assertEquals(setOf(BASE_ORIGIN_WITH_PORT), defaultProfile.crossOriginIsolatedAllowlist)
    }

    @Test
    fun testSharedArrayBufferInWorker() {
        val headers: Map<String, String> =
            mapOf(DOCUMENT_ISOLATION_POLICY_HEADER to DOCUMENT_ISOLATION_POLICY_VALUE)
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, headers)
        defaultProfile.crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)

        webViewOnUiThread.loadUrlAndWaitForCompletion(INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("success", result.mMessage.data)
    }

    @Test
    fun testAllowListWithHeadersEnablesIsolationApis() {
        val headers: Map<String, String> =
            mapOf(DOCUMENT_ISOLATION_POLICY_HEADER to DOCUMENT_ISOLATION_POLICY_VALUE)
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, headers)
        defaultProfile.crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)

        webViewOnUiThread.loadUrlAndWaitForCompletion(ISOLATE_INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("true", result.mMessage.data)
    }

    @Test
    fun testAllowListWithLegacyHeadersDoesNotEnableIsolationApis() {
        val headers: Map<String, String> =
            mapOf(
                CROSS_ORIGIN_OPENER_POLICY_HEADER to CROSS_ORIGIN_OPENER_POLICY_VALUE,
                CROSS_ORIGIN_EMBEDDER_POLICY_HEADER to CROSS_ORIGIN_EMBEDDER_POLICY_VALUE,
            )
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, headers)
        defaultProfile.crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)

        webViewOnUiThread.loadUrlAndWaitForCompletion(ISOLATE_INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("false", result.mMessage.data)
    }

    @Test
    fun testAllowListWithoutHeaderDoesNotEnableIsolationApis() {
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, mapOf())
        defaultProfile.crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)

        webViewOnUiThread.loadUrlAndWaitForCompletion(ISOLATE_INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("false", result.mMessage.data)
    }

    @Test
    fun testMismatchedOriginAllowedDoesNotEnableIsolationApis() {
        val headers: Map<String, String> =
            mapOf(DOCUMENT_ISOLATION_POLICY_HEADER to DOCUMENT_ISOLATION_POLICY_VALUE)
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, headers)
        defaultProfile.crossOriginIsolatedAllowlist = setOf(DIFFERENT_BASE_ORIGIN)

        webViewOnUiThread.loadUrlAndWaitForCompletion(ISOLATE_INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("false", result.mMessage.data)
    }

    @Test
    fun testHeaderWithoutAllowListDoesNotEnableIsolationApis() {
        val headers: Map<String, String> =
            mapOf(DOCUMENT_ISOLATION_POLICY_HEADER to DOCUMENT_ISOLATION_POLICY_VALUE)
        webViewOnUiThread.webViewClient = InterceptClient(webViewOnUiThread, headers)

        webViewOnUiThread.loadUrlAndWaitForCompletion(ISOLATE_INDEX_URL)

        val result: TestWebMessageListener.Data = listener.waitForOnPostMessage()
        assertEquals("false", result.mMessage.data)
    }

    companion object {

        private const val JS_OBJECT_NAME = "testListener"

        private const val BASE_ORIGIN = "https://www.example.com"
        private const val BASE_ORIGIN_WITH_PORT = "$BASE_ORIGIN:443"

        private const val DIFFERENT_BASE_ORIGIN = "https://www.notexample.com"
        private const val BASE_URL = "$BASE_ORIGIN/"

        private const val INDEX_URL = BASE_URL + "index.html"
        private const val ISOLATE_INDEX_URL = BASE_URL + "isolate.html"

        private const val WORKER_URL = BASE_URL + "worker.js"
        private const val INDEX_HTML =
            """
            <html>
            <body>
                <script>

                    const sab = new SharedArrayBuffer(16);
                    const view = new Int32Array(sab);
                    view[0] = 42;
                    const myWorker = new Worker('worker.js');
                    myWorker.addEventListener('message',
                        event => ${JS_OBJECT_NAME}.postMessage("success"));
                    myWorker.postMessage(sab);
                </script>
            </body>
            </html>
            """

        private const val ISOLATE_INDEX_HTML: String =
            """
            <html>
            <body>
            <script>
                ${JS_OBJECT_NAME}.postMessage(window.crossOriginIsolated ? "true" : "false");
            </script>
            </body>
            </html>
            """

        private const val WORKER_JS =
            """
                self.addEventListener('message', e => {
                    const view = new Int32Array(e.data);
                    if (view[0] === 42) {
                        self.postMessage('success');
                    }
                });
            """

        private const val DOCUMENT_ISOLATION_POLICY_HEADER = "Document-Isolation-Policy"
        private const val DOCUMENT_ISOLATION_POLICY_VALUE = "isolate-and-credentialless"

        private const val CROSS_ORIGIN_OPENER_POLICY_HEADER = "Cross-Origin-Opener-Policy"
        private const val CROSS_ORIGIN_OPENER_POLICY_VALUE = "same-origin"

        private const val CROSS_ORIGIN_EMBEDDER_POLICY_HEADER = "Cross-Origin-Embedder-Policy"
        private const val CROSS_ORIGIN_EMBEDDER_POLICY_VALUE = "credentialless"
    }
}
