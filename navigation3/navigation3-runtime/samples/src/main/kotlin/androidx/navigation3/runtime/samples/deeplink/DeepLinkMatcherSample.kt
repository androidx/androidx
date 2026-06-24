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

private const val SAMPLE_BASE_PATH = "www.nav3deeplinksample.com"

@Sampled
fun staticKeyDeepLinkMatcherSample() {
    // declare a matcher based on mime type
    val mimeType = "image/jpg"
    val mimeTypeFilter = DeepLinkMatcher.mimeTypeFilter(mimeType)
    val matcher = StaticKeyDeepLinkMatcher(ImageKey, listOf(mimeTypeFilter))

    // when handling a request
    val request =
        DeepLinkRequest(
            uri = DeepLinkUri("$SAMPLE_BASE_PATH/images/"),
            extras = DeepLinkRequest.mimeTypeExtra(mimeType),
        )
    // returns a valid result as long as mimeType matches
    val matchResult: DeepLinkMatcher.MatchResult<ImageKey>? = matcher.match(request)
}
