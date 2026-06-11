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

package androidx.navigation3.runtime.samples.deeplink

import androidx.annotation.Sampled
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.StaticKeyDeepLinkMatcher
import kotlinx.serialization.Serializable

@Serializable object ImageKey : NavKey

private class MimeTypeFilter(private val mimeType: String) : DeepLinkMatcher.Filter {
    override fun filterRequest(request: DeepLinkRequest): Boolean {
        return mimeType == request.mimeType
    }
}

private const val SAMPLE_BASE_PATH = "www.nav3deeplinksample.com"

@Sampled
fun staticKeyDeepLinkMatcherSample() {
    // declare a matcher based on mime type
    val mimeTypeFilter = MimeTypeFilter("image/jpg")
    val matcher = StaticKeyDeepLinkMatcher(ImageKey, listOf(mimeTypeFilter))

    // when handling a request
    val request =
        DeepLinkRequest.fromUri(
            uri = DeepLinkUri("$SAMPLE_BASE_PATH/images/"),
            mimeType = "image/jpg",
        )
    // returns a valid result as long as mimeType matches
    val matchResult: DeepLinkMatcher.MatchResult<ImageKey>? = matcher.match(request)
}
