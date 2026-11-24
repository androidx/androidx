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

import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebViewOnUiThread.WaitForLoadedClient
import androidx.webkit.test.common.WebkitUtils
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [Profile.addHeaderInterceptor] and
 * [WebViewCompat.addHeaderInterceptor].
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class HeaderInterceptorTest {

    private lateinit var webViewOnUiThread: WebViewOnUiThread
    private lateinit var defaultProfile: Profile
    private lateinit var server: MockWebServer
    private val registeredHandles = mutableListOf<HeaderInterceptorHandle>()

    @Before
    fun setUp() {
        WebkitUtils.checkFeature(WebViewFeature.HEADER_INTERCEPTOR)
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)

        webViewOnUiThread = WebViewOnUiThread()
        defaultProfile =
            WebkitUtils.onMainThreadSync(
                Callable { ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)!! }
            )

        server = createMockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        WebkitUtils.onMainThreadSync {
            for (handle in registeredHandles) {
                handle.unregister()
            }
        }
        registeredHandles.clear()
        if (::webViewOnUiThread.isInitialized) {
            webViewOnUiThread.cleanUp()
        }
        if (::server.isInitialized) {
            server.shutdown()
        }
    }

    @Test
    fun addHeaderInterceptor_runsOffMainThread() {
        val isBackgroundThreadQueue = LinkedBlockingQueue<Boolean>()
        val matcher = RequestMatcher.forUrlPatterns(setOf(server.url("/*").toString()))
        val testUrl = server.url(TEST_PATH).toString()

        registerProfileInterceptor(matcher) {
            isBackgroundThreadQueue.add(Looper.myLooper() != Looper.getMainLooper())
            null
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl)
        val ranOffMainThread = WebkitUtils.waitForNextQueueElement(isBackgroundThreadQueue)

        assertTrue(ranOffMainThread)
    }

    @Test
    fun profileAddHeaderInterceptor_dynamicallyAddsHeaders() {
        val requestCounter = AtomicInteger(0)
        val testUrl = server.url(TEST_PATH).toString()
        val matcher = RequestMatcher.forUrlPatterns(setOf(testUrl))

        registerProfileInterceptor(matcher) {
            HeaderUpdates.Builder()
                .setHeader("X-Request-Number", requestCounter.incrementAndGet().toString())
                .build()
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl)
        val firstRequest = takeRequestForPath(server, TEST_PATH)
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl)
        val secondRequest = takeRequestForPath(server, TEST_PATH)

        assertEquals("1", firstRequest.getHeader("X-Request-Number"))
        assertEquals("2", secondRequest.getHeader("X-Request-Number"))
    }

    @Test
    fun profileAddHeaderInterceptor_removesExistingHeaders() {
        val seenHeadersQueue = LinkedBlockingQueue<Map<String, String>>()
        val testUrl = server.url(TEST_PATH).toString()
        val matcher = RequestMatcher.forUrlPatterns(setOf(testUrl))
        val additionalHeaders = mapOf("X-Keep" to "kept-value", "X-Remove" to "to-be-removed")

        registerProfileInterceptor(matcher) { request ->
            seenHeadersQueue.add(caseInsensitiveCopy(request.requestHeaders))
            HeaderUpdates.Builder().setHeaderRemoved("X-Remove").build()
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl, additionalHeaders)
        val seenHeaders = WebkitUtils.waitForNextQueueElement(seenHeadersQueue)
        val recordedRequest = takeRequestForPath(server, TEST_PATH)

        assertEquals("kept-value", seenHeaders["X-Keep"])
        assertEquals("to-be-removed", seenHeaders["X-Remove"])
        assertEquals("kept-value", recordedRequest.getHeader("X-Keep"))
        assertNull(recordedRequest.getHeader("X-Remove"))
    }

    @Test
    fun profileAddHeaderInterceptor_nullReturnLeavesHeadersUnmodified() {
        val interceptorCalledLatch = CountDownLatch(1)
        val testUrl = server.url(TEST_PATH).toString()
        val matcher = RequestMatcher.forUrlPatterns(setOf(testUrl))
        val additionalHeaders = mapOf("X-Existing" to "kept-value")

        registerProfileInterceptor(matcher) {
            interceptorCalledLatch.countDown()
            null
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl, additionalHeaders)
        val recordedRequest = takeRequestForPath(server, TEST_PATH)

        assertTrue(interceptorCalledLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        assertEquals("kept-value", recordedRequest.getHeader("X-Existing"))
    }

    @Test
    fun profileAddHeaderInterceptor_matchesOnlyConfiguredUrlPatterns() {
        val matcher =
            RequestMatcher.forUrlPatterns(
                setOf(
                    server.url("/matched-a/*").toString(),
                    server.url("/matched-b/*").toString(),
                )
            )
        val matchedUrlA = server.url("/matched-a/page").toString()
        val matchedUrlB = server.url("/matched-b/page").toString()
        val unmatchedUrl = server.url("/unmatched/page").toString()

        registerProfileInterceptor(matcher) {
            HeaderUpdates.Builder().setHeader("X-Matched", "true").build()
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(matchedUrlA)
        val matchedRequestA = takeRequestForPath(server, "/matched-a/page")
        webViewOnUiThread.loadUrlAndWaitForCompletion(matchedUrlB)
        val matchedRequestB = takeRequestForPath(server, "/matched-b/page")
        webViewOnUiThread.loadUrlAndWaitForCompletion(unmatchedUrl)
        val unmatchedRequest = takeRequestForPath(server, "/unmatched/page")

        assertEquals("true", matchedRequestA.getHeader("X-Matched"))
        assertEquals("true", matchedRequestB.getHeader("X-Matched"))
        assertNull(unmatchedRequest.getHeader("X-Matched"))
    }

    @Test
    fun profileAddHeaderInterceptor_forAnyUrl_matchesAnyUrl() {
        val firstUrl = server.url("/first-path").toString()
        val secondUrl = server.url("/nested/second-path?query=1").toString()

        registerProfileInterceptor(RequestMatcher.forAnyUrl()) {
            HeaderUpdates.Builder().setHeader("X-Any-Url", "matched").build()
        }
        webViewOnUiThread.loadUrlAndWaitForCompletion(firstUrl)
        val firstRequest = takeRequestForPath(server, "/first-path")
        webViewOnUiThread.loadUrlAndWaitForCompletion(secondUrl)
        val secondRequest = takeRequestForPath(server, "/nested/second-path?query=1")

        assertEquals("matched", firstRequest.getHeader("X-Any-Url"))
        assertEquals("matched", secondRequest.getHeader("X-Any-Url"))
    }

    @Test
    fun profileAddHeaderInterceptor_unregisterStopsInterception() {
        val invocationCount = AtomicInteger(0)
        val testUrl = server.url(TEST_PATH).toString()
        val matcher = RequestMatcher.forUrlPatterns(setOf(testUrl))

        val handle =
            registerProfileInterceptor(matcher) {
                invocationCount.incrementAndGet()
                HeaderUpdates.Builder().setHeader("X-Dynamic", "present").build()
            }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl)
        val firstRequest = takeRequestForPath(server, TEST_PATH)
        WebkitUtils.onMainThreadSync { handle.unregister() }
        webViewOnUiThread.loadUrlAndWaitForCompletion(testUrl)
        val secondRequest = takeRequestForPath(server, TEST_PATH)

        assertEquals("present", firstRequest.getHeader("X-Dynamic"))
        assertNull(secondRequest.getHeader("X-Dynamic"))
        assertEquals(1, invocationCount.get())
    }

    @Test
    fun webViewAddHeaderInterceptor_onlyAppliesToTargetWebView() {
        WebViewOnUiThread().use { otherWebViewOnUiThread ->
            val matcher = RequestMatcher.forUrlPatterns(setOf(server.url("/*").toString()))
            val targetUrl = server.url("/target").toString()
            val otherUrl = server.url("/other").toString()

            registerWebViewInterceptor(webViewOnUiThread, matcher) {
                HeaderUpdates.Builder().setHeader("X-WebView-Specific", "target-only").build()
            }
            webViewOnUiThread.loadUrlAndWaitForCompletion(targetUrl)
            val targetRequest = takeRequestForPath(server, "/target")
            otherWebViewOnUiThread.loadUrlAndWaitForCompletion(otherUrl)
            val otherRequest = takeRequestForPath(server, "/other")

            assertEquals("target-only", targetRequest.getHeader("X-WebView-Specific"))
            assertNull(otherRequest.getHeader("X-WebView-Specific"))
        }
    }

    @Test
    fun webViewAddHeaderInterceptor_unregisterStopsInterception() {
        val invocationCount = AtomicInteger(0)
        val targetUrl = server.url("/target").toString()
        val matcher = RequestMatcher.forUrlPatterns(setOf(targetUrl))

        val handle =
            registerWebViewInterceptor(webViewOnUiThread, matcher) {
                invocationCount.incrementAndGet()
                HeaderUpdates.Builder().setHeader("X-WebView-Specific", "present").build()
            }
        webViewOnUiThread.loadUrlAndWaitForCompletion(targetUrl)
        val firstRequest = takeRequestForPath(server, "/target")
        WebkitUtils.onMainThreadSync { handle.unregister() }
        webViewOnUiThread.loadUrlAndWaitForCompletion(targetUrl)
        val secondRequest = takeRequestForPath(server, "/target")

        assertEquals("present", firstRequest.getHeader("X-WebView-Specific"))
        assertNull(secondRequest.getHeader("X-WebView-Specific"))
        assertEquals(1, invocationCount.get())
    }

    @Test
    fun addHeaderInterceptor_unregisterIsIdempotent() {
        val matcher = RequestMatcher.forAnyUrl()
        val profileHandle = registerProfileInterceptor(matcher) { null }
        val webViewHandle = registerWebViewInterceptor(webViewOnUiThread, matcher) { null }

        WebkitUtils.onMainThreadSync {
            profileHandle.unregister()
            profileHandle.unregister()
            webViewHandle.unregister()
            webViewHandle.unregister()
        }
    }

    @Test
    fun addHeaderInterceptor_invalidUrlPattern_throwsIllegalArgumentException() {
        val invalidMatcher = RequestMatcher.forUrlPatterns(setOf("not a valid url"))

        WebkitUtils.onMainThreadSync {
            assertThrows(IllegalArgumentException::class.java) {
                defaultProfile.addHeaderInterceptor(invalidMatcher) { null }
            }
            assertThrows(IllegalArgumentException::class.java) {
                WebViewCompat.addHeaderInterceptor(
                    webViewOnUiThread.webViewOnCurrentThread,
                    invalidMatcher,
                ) {
                    null
                }
            }
        }
    }

    @Test
    fun requestMatcherExamples_anyPathOnExactDomain() {
        assertUrlPatternMatches(
            pattern = "https://example.com",
            matchingUrls =
                listOf(
                    "https://example.com",
                    "https://example.com/",
                    "https://example.com/foo/bar?baz=1",
                ),
            nonMatchingUrls =
                listOf(
                    "https://sub.example.com/",
                    "https://otherexample.com/",
                    "https://example.com:8443/",
                ),
        )
    }

    @Test
    fun requestMatcherExamples_anyPathOnDomainOrSubdomains() {
        assertUrlPatternMatches(
            pattern = "https://{*.}?example.com",
            matchingUrls =
                listOf(
                    "https://example.com/",
                    "https://example.com/page",
                    "https://sub.example.com/page",
                    "https://a.b.example.com/page",
                ),
            nonMatchingUrls =
                listOf(
                    "https://otherexample.com/page",
                    "https://example.org/page",
                ),
        )
    }

    @Test
    fun requestMatcherExamples_exactPath() {
        assertUrlPatternMatches(
            pattern = "https://www.example.com/path",
            matchingUrls =
                listOf(
                    "https://www.example.com/path",
                    "https://www.example.com:443/path",
                    "https://www.example.com/path?foo=bar",
                ),
            nonMatchingUrls =
                listOf(
                    "https://www.example.com/",
                    "https://www.example.com/path/",
                    "https://www.example.com/path/subpath",
                    "https://www.example.com:8443/path",
                    "http://www.example.com/path",
                ),
        )
    }

    @Test
    fun requestMatcherExamples_pathOrSubpath() {
        assertUrlPatternMatches(
            pattern = "https://www.example.com/api{/*}?",
            matchingUrls =
                listOf(
                    "https://www.example.com/api",
                    "https://www.example.com/api?v=1",
                    "https://www.example.com/api/",
                    "https://www.example.com/api/v1/items",
                ),
            nonMatchingUrls =
                listOf(
                    "https://www.example.com/apiv2",
                    "https://www.example.com/other/api",
                ),
        )
    }

    @Test
    fun requestMatcherExamples_queryParameter() {
        assertUrlPatternMatches(
            pattern = "https://www.example.com/path?{*&}?foo=bar{&*}?",
            matchingUrls =
                listOf(
                    "https://www.example.com/path?foo=bar",
                    "https://www.example.com/path?a=1&foo=bar",
                    "https://www.example.com/path?foo=bar&b=2",
                    "https://www.example.com/path?a=1&foo=bar&b=2",
                ),
            nonMatchingUrls =
                listOf(
                    "https://www.example.com/path",
                    "https://www.example.com/path?myfoo=bar2",
                    "https://www.example.com/path?foo=bar2",
                    "https://www.example.com/path?myfoo=bar",
                ),
        )
    }

    /**
     * Verifies which URLs match [pattern] on WebView by capturing outgoing request headers in
     * [WebViewClient.shouldInterceptRequest] (which runs after [HeaderInterceptor]s) and serving a
     * local synthetic response so tests do not depend on external DNS or network access.
     */
    private fun assertUrlPatternMatches(
        pattern: String,
        matchingUrls: List<String>,
        nonMatchingUrls: List<String>,
    ) {
        val mainFrameHeadersQueue = LinkedBlockingQueue<Map<String, String>>()
        webViewOnUiThread.setWebViewClient(
            object : WaitForLoadedClient(webViewOnUiThread) {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse {
                    if (request.isForMainFrame) {
                        mainFrameHeadersQueue.add(caseInsensitiveCopy(request.requestHeaders))
                    }
                    return WebResourceResponse(
                        "text/html",
                        "utf-8",
                        200,
                        "OK",
                        mapOf("Cache-Control" to "no-store"),
                        ByteArrayInputStream("<html></html>".toByteArray(StandardCharsets.UTF_8)),
                    )
                }
            }
        )

        val handle =
            registerProfileInterceptor(RequestMatcher.forUrlPatterns(setOf(pattern))) {
                HeaderUpdates.Builder().setHeader(PATTERN_MATCH_HEADER, "true").build()
            }

        try {
            for (url in matchingUrls) {
                webViewOnUiThread.loadUrlAndWaitForCompletion(url)
                val headers = WebkitUtils.waitForNextQueueElement(mainFrameHeadersQueue)
                assertEquals(
                    "Expected pattern '$pattern' to match '$url'",
                    "true",
                    headers[PATTERN_MATCH_HEADER],
                )
            }
            for (url in nonMatchingUrls) {
                webViewOnUiThread.loadUrlAndWaitForCompletion(url)
                val headers = WebkitUtils.waitForNextQueueElement(mainFrameHeadersQueue)
                assertNull(
                    "Expected pattern '$pattern' NOT to match '$url'",
                    headers[PATTERN_MATCH_HEADER],
                )
            }
        } finally {
            WebkitUtils.onMainThreadSync { handle.unregister() }
            registeredHandles.remove(handle)
        }
    }

    private fun createMockWebServer(): MockWebServer {
        val mockServer = MockWebServer()
        mockServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    return MockResponse()
                        .setBody("<html><body>hello</body></html>")
                        .addHeader("Content-Type", "text/html")
                        .addHeader("Cache-Control", "no-store")
                }
            }
        return mockServer
    }

    private fun caseInsensitiveCopy(headers: Map<String, String>): Map<String, String> =
        TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER).apply { putAll(headers) }

    private fun registerProfileInterceptor(
        matcher: RequestMatcher,
        interceptor: HeaderInterceptor,
    ): HeaderInterceptorHandle {
        val handle =
            WebkitUtils.onMainThreadSync(
                Callable { defaultProfile.addHeaderInterceptor(matcher, interceptor) }
            )
        registeredHandles.add(handle)
        return handle
    }

    private fun registerWebViewInterceptor(
        targetWebView: WebViewOnUiThread,
        matcher: RequestMatcher,
        interceptor: HeaderInterceptor,
    ): HeaderInterceptorHandle {
        val handle =
            WebkitUtils.onMainThreadSync(
                Callable {
                    WebViewCompat.addHeaderInterceptor(
                        targetWebView.webViewOnCurrentThread,
                        matcher,
                        interceptor,
                    )
                }
            )
        registeredHandles.add(handle)
        return handle
    }

    private fun takeRequestForPath(
        targetServer: MockWebServer,
        expectedPath: String,
    ): RecordedRequest {
        while (true) {
            val request = targetServer.takeRequest(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertNotNull("Timed out waiting for request to $expectedPath", request)
            if (request!!.path == expectedPath) {
                return request
            }
        }
    }

    companion object {
        private const val TEST_PATH = "/index.html"
        private const val PATTERN_MATCH_HEADER = "X-Pattern-Matched"
        private const val TIMEOUT_SECONDS = 5L
    }
}
