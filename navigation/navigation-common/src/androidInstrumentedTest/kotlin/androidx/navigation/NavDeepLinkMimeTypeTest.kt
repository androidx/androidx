/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation

import android.net.Uri
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test

@SmallTest
class NavDeepLinkMimeTypeTest {
    companion object {
        private const val DEEP_LINK_EXACT_HTTPS = "https://www.example.com"
    }

    @Test
    fun deepLinkInvalidMimeType() {
        val mimeType = "noSlash"
        try {
            NavDeepLink(null, null, mimeType)
            fail("NavDeepLink must throw")
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "The given mimeType $mimeType does not match to required \"type/subtype\" " +
                        "format"
                )
        }
    }

    @Test
    fun deepLinkMimeTypeMatch() {
        val mimeType = "*/*"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, mimeType
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchWithUri() {
        val mimeType = "*/*"
        val deepLink = NavDeepLink(DEEP_LINK_EXACT_HTTPS, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        Uri.parse(DEEP_LINK_EXACT_HTTPS), null, mimeType
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchDeepLinkWildCard() {
        val mimeType = "*/*"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "type/subtype"
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchTypeDeepLinkWildCard() {
        val mimeType = "*/subtype"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "type/subtype"
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchSubtypeDeepLinkWildCard() {
        val mimeType = "type/*"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "type/subtype"
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchRequestWildCard() {
        val mimeType = "type/subtype"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "*/*"
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchSubtypeRequestWildCard() {
        val mimeType = "type/subtype"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "type/*"
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkMimeTypeMatchTypeRequestWildCard() {
        val mimeType = "type/subtype"
        val deepLink = NavDeepLink(null, null, mimeType)

        assertWithMessage("The mimeTypes should match")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        null, null, "*/subtype"
                    )
                )
            )
            .isTrue()
    }
}
