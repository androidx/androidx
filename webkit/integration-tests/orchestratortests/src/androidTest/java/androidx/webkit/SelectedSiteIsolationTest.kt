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
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebViewOnUiThread.WaitForLoadedClient
import androidx.webkit.test.common.WebkitUtils
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for [Profile.configureIsolatedSites] and [Profile.canConfigureIsolatedSites].
 *
 * These tests need to run in a separate test process via AndroidX Test Orchestrator because the
 * configureIsolatedSites API may generally only be used once per profile per session.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectedSiteIsolationTest {

    private lateinit var profile: Profile
    private val createdWebViews = mutableListOf<WebViewOnUiThread>()

    @Before
    fun setUp() {
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_SELECTED_SITE_ISOLATION_EXPERIMENTAL_V1)

        profile =
            WebkitUtils.onMainThreadSync<Profile> {
                ProfileStore.getInstance().getOrCreateProfile(Profile.DEFAULT_PROFILE_NAME)
            }
    }

    @After
    fun tearDown() {
        for (webView in createdWebViews) {
            webView.cleanUp()
        }
        createdWebViews.clear()
    }

    @Test
    fun configureIsolatedSites_isolatesRenderProcesses() {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER)

        WebkitUtils.onMainThreadSync {
            assertTrue(profile.canConfigureIsolatedSites())
            profile.configureIsolatedSites(setOf(ISOLATED_SITE))
        }

        val isolatedWebView = createConfiguredWebView()
        val isolatedSubdomainWebView = createConfiguredWebView()
        val unisolatedWebView = createConfiguredWebView()

        isolatedWebView.loadUrlAndWaitForCompletion("$ISOLATED_SITE/index.html")
        isolatedSubdomainWebView.loadUrlAndWaitForCompletion(
            "$ISOLATED_SUBDOMAIN_ORIGIN/index.html"
        )
        unisolatedWebView.loadUrlAndWaitForCompletion("$UNISOLATED_SITE/index.html")

        val isolatedRenderer =
            WebkitUtils.onMainThreadSync<WebViewRenderProcess?> {
                WebViewCompat.getWebViewRenderProcess(isolatedWebView.webViewOnCurrentThread)
            }
        val isolatedSubdomainRenderer =
            WebkitUtils.onMainThreadSync<WebViewRenderProcess?> {
                WebViewCompat.getWebViewRenderProcess(
                    isolatedSubdomainWebView.webViewOnCurrentThread
                )
            }
        val unisolatedRenderer =
            WebkitUtils.onMainThreadSync<WebViewRenderProcess?> {
                WebViewCompat.getWebViewRenderProcess(unisolatedWebView.webViewOnCurrentThread)
            }

        assertNotNull(isolatedRenderer)
        assertNotNull(isolatedSubdomainRenderer)
        assertNotNull(unisolatedRenderer)
        assertEquals(isolatedRenderer, isolatedSubdomainRenderer)
        assertNotEquals(isolatedRenderer, unisolatedRenderer)
    }

    @Test
    fun withoutConfigureIsolatedSites_sharesRenderProcess() {
        WebkitUtils.checkFeature(WebViewFeature.GET_WEB_VIEW_RENDERER)

        val firstWebView = createConfiguredWebView()
        val secondWebView = createConfiguredWebView()

        firstWebView.loadUrlAndWaitForCompletion("$ISOLATED_SITE/index.html")
        secondWebView.loadUrlAndWaitForCompletion("$UNISOLATED_SITE/index.html")

        val firstRenderer =
            WebkitUtils.onMainThreadSync<WebViewRenderProcess?> {
                WebViewCompat.getWebViewRenderProcess(firstWebView.webViewOnCurrentThread)
            }
        val secondRenderer =
            WebkitUtils.onMainThreadSync<WebViewRenderProcess?> {
                WebViewCompat.getWebViewRenderProcess(secondWebView.webViewOnCurrentThread)
            }

        assertNotNull(firstRenderer)
        assertNotNull(secondRenderer)
        assertEquals(firstRenderer, secondRenderer)
    }

    @Test
    fun configureIsolatedSites_throwsOnEmptySet() {
        WebkitUtils.onMainThreadSync {
            assertThrows(IllegalArgumentException::class.java) {
                profile.configureIsolatedSites(emptySet())
            }
        }
    }

    @Test
    fun configureIsolatedSites_throwsOnInvalidSite() {
        WebkitUtils.onMainThreadSync {
            assertThrows(IllegalArgumentException::class.java) {
                profile.configureIsolatedSites(setOf("not a valid site"))
            }
        }
    }

    @Test
    fun configureIsolatedSites_throwsWhenCalledTwice() {
        WebkitUtils.onMainThreadSync {
            assertTrue(profile.canConfigureIsolatedSites())
            profile.configureIsolatedSites(setOf(ISOLATED_SITE))
            assertFalse(profile.canConfigureIsolatedSites())
            assertThrows(IllegalStateException::class.java) {
                profile.configureIsolatedSites(setOf(UNISOLATED_SITE))
            }
        }
    }

    @Test
    fun configureIsolatedSites_throwsAfterRenderProcessUsed() {
        WebkitUtils.onMainThreadSync { assertTrue(profile.canConfigureIsolatedSites()) }

        val webView = createConfiguredWebView()
        webView.loadUrlAndWaitForCompletion("$UNISOLATED_SITE/index.html")

        WebkitUtils.onMainThreadSync {
            assertFalse(profile.canConfigureIsolatedSites())
            assertThrows(IllegalStateException::class.java) {
                profile.configureIsolatedSites(setOf(ISOLATED_SITE))
            }
        }
    }

    private fun createConfiguredWebView(): WebViewOnUiThread {
        val webViewOnUiThread = WebViewOnUiThread()
        createdWebViews.add(webViewOnUiThread)
        WebkitUtils.onMainThreadSync {
            WebViewCompat.setProfile(webViewOnUiThread.webViewOnCurrentThread, profile.name)
        }
        // Avoid making requests to actual example.com/example.org sites.
        webViewOnUiThread.webViewClient =
            object : WaitForLoadedClient(webViewOnUiThread) {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest,
                ): WebResourceResponse =
                    WebResourceResponse(
                        "text/html",
                        "utf-8",
                        200,
                        "OK",
                        emptyMap(),
                        ByteArrayInputStream(HTML_BODY.toByteArray(StandardCharsets.UTF_8)),
                    )
            }
        return webViewOnUiThread
    }

    companion object {
        private const val ISOLATED_SITE = "https://example.com"
        private const val ISOLATED_SUBDOMAIN_ORIGIN = "https://sub.example.com"
        private const val UNISOLATED_SITE = "https://example.org"
        private const val HTML_BODY = "<html><body>Hello</body></html>"
    }
}
