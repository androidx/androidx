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
import androidx.webkit.internal.HeaderInterceptorAdapter
import com.google.common.truth.Truth.assertThat
import org.chromium.support_lib_boundary.RequestMatcherConfig
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Unit tests for [RequestMatcher], [HeaderUpdates], [HeaderInterceptorHandle], and adapter. */
@RunWith(JUnit4::class)
class HeaderInterceptorUnitTest {

    @Test
    fun requestMatcher_forUrlPatterns_emptyThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            RequestMatcher.forUrlPatterns(emptySet())
        }
    }

    @Test
    fun requestMatcher_forUrlPatterns_serializesPatterns() {
        val serializedConfigs = mutableMapOf<Int, Any>()
        val matcher =
            RequestMatcher.forUrlPatterns(
                setOf("https://example.com/*", "https://*.example.org/api/*")
            )

        matcher.serializeToBoundaryLayer { key, value -> serializedConfigs[key] = value }

        assertThat(serializedConfigs).containsKey(RequestMatcherConfig.URL_PATTERNS)
        assertThat(serializedConfigs[RequestMatcherConfig.URL_PATTERNS] as List<*>)
            .containsExactly("https://example.com/*", "https://*.example.org/api/*")
    }

    @Test
    fun requestMatcher_forAnyUrl_serializesUniversalWildcardPattern() {
        val serializedConfigs = mutableMapOf<Int, Any>()
        val matcher = RequestMatcher.forAnyUrl()

        matcher.serializeToBoundaryLayer { key, value -> serializedConfigs[key] = value }

        assertThat(serializedConfigs)
            .containsExactly(RequestMatcherConfig.URL_PATTERNS, listOf("*://*:*"))
    }

    @Test
    fun headerUpdatesBuilder_setsAndRemovesHeaders() {
        val builder =
            HeaderUpdates.Builder().setHeader("X-Add", "value-1").setHeaderRemoved("X-Remove")

        val updates = builder.build()

        assertThat(updates.toBoundaryMap()).containsExactly("X-Add", "value-1", "X-Remove", null)
    }

    @Test
    fun headerUpdates_gettersReflectSetRemovedAndAbsentHeadersCaseInsensitively() {
        val builder =
            HeaderUpdates.Builder().setHeader("X-Custom", "value-1").setHeaderRemoved("X-Removed")

        val updates = builder.build()

        assertThat(updates.hasHeader("x-custom")).isTrue()
        assertThat(updates.getHeader("x-custom")).isEqualTo("value-1")
        assertThat(updates.isHeaderRemoved("x-custom")).isFalse()
        assertThat(updates.hasHeader("x-removed")).isTrue()
        assertThat(updates.getHeader("x-removed")).isNull()
        assertThat(updates.isHeaderRemoved("x-removed")).isTrue()
        assertThat(updates.hasHeader("x-absent")).isFalse()
        assertThat(updates.getHeader("x-absent")).isNull()
        assertThat(updates.isHeaderRemoved("x-absent")).isFalse()
    }

    @Test
    fun headerUpdatesBuilder_caseInsensitiveLastCallWinsIncludingKeyCasing() {
        val setThenRemoveBuilder =
            HeaderUpdates.Builder()
                .setHeader("X-Custom-Header", "initial")
                .setHeaderRemoved("x-custom-header")
        val removeThenSetBuilder =
            HeaderUpdates.Builder()
                .setHeaderRemoved("x-custom-header")
                .setHeader("X-Custom-Header", "updated")

        val setThenRemove = setThenRemoveBuilder.build()
        val removeThenSet = removeThenSetBuilder.build()

        assertThat(setThenRemove.toBoundaryMap()).containsExactly("x-custom-header", null)
        assertThat(removeThenSet.toBoundaryMap()).containsExactly("X-Custom-Header", "updated")
    }

    @Test
    fun headerInterceptorHandle_unregisterInvokesRunnable() {
        var unregisterCount = 0
        val handle = HeaderInterceptorHandle { unregisterCount++ }

        handle.unregister()

        assertThat(unregisterCount).isEqualTo(1)
    }

    @Test
    fun headerInterceptorAdapter_nullUpdatesReturnsEmptyMap() {
        val fakeRequest = createFakeWebResourceRequest()
        val nullAdapter = HeaderInterceptorAdapter { null }

        val result = nullAdapter.apply(fakeRequest)

        assertThat(result).isEmpty()
    }

    @Test
    fun headerInterceptorAdapter_convertsUpdatesToBoundaryMap() {
        val fakeRequest = createFakeWebResourceRequest()
        val updatesAdapter = HeaderInterceptorAdapter {
            HeaderUpdates.Builder()
                .setHeader("X-Added", "123")
                .setHeaderRemoved("X-Removed")
                .build()
        }

        val result = updatesAdapter.apply(fakeRequest)

        assertThat(result).containsExactly("X-Added", "123", "X-Removed", null)
    }

    private fun createFakeWebResourceRequest(): WebResourceRequest =
        object : WebResourceRequest {
            override fun getUrl(): Uri = throw UnsupportedOperationException()

            override fun isForMainFrame(): Boolean = true

            override fun isRedirect(): Boolean = false

            override fun hasGesture(): Boolean = false

            override fun getMethod(): String = "GET"

            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
}
