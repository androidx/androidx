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

package androidx.navigation3.runtime

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class UriDeepLinkMatcherTest {
    @Test
    fun matchRequest_schemeMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestArgKey>())
        val request = DeepLinkRequest.fromUriString("https://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_authorityMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.org/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_pathSegmentShorterMismatch() {
        val pattern = DeepLinkUri("http://example.com/path/subpath")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }

    @Test
    fun matchRequest_pathSegmentLongerMismatch() {
        val pattern = DeepLinkUri("http://example.com/path")
        val matcher = UriDeepLinkMatcher(pattern, serializer<TestArgKey>())
        val request = DeepLinkRequest.fromUriString("http://example.com/path/extra")

        val result = matcher.match(request)

        assertThat(result).isNull()
    }
}

@Serializable private data class TestArgKey(val name: String = "test")
