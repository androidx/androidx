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
import org.junit.Test

@SmallTest
class NavDeepLinkActionTest {

    companion object {
        private const val DEEP_LINK_ACTION = "android.intent.action.EXAMPLE_ACTION"
        private const val DEEP_LINK_EXACT_HTTPS = "https://www.example.com"
    }

    @Test
    fun deepLinkActionNoMatch() {
        val deepLink = NavDeepLink(DEEP_LINK_EXACT_HTTPS, DEEP_LINK_ACTION, null)

        assertWithMessage("The actions should not have matched")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        Uri.parse(DEEP_LINK_EXACT_HTTPS),
                        null, null
                    )
                )
            )
            .isFalse()
    }

    @Test
    fun deepLinkActionMatch() {
        val deepLink = NavDeepLink(DEEP_LINK_EXACT_HTTPS, DEEP_LINK_ACTION, null)

        assertWithMessage("The actions should have matched")
            .that(
                deepLink.matches(
                    NavDeepLinkRequest(
                        Uri.parse(DEEP_LINK_EXACT_HTTPS),
                        DEEP_LINK_ACTION, null
                    )
                )
            )
            .isTrue()
    }

    @Test
    fun deepLinkActionMatchNullUri() {
        val deepLink = NavDeepLink(null, DEEP_LINK_ACTION, null)

        assertWithMessage("The actions should have matched")
            .that(deepLink.matches(NavDeepLinkRequest(null, DEEP_LINK_ACTION, null)))
            .isTrue()
    }

    @Test
    fun deepLinkActionNoMatchDifferent() {
        val deepLink = NavDeepLink(null, "bad.action", null)

        assertWithMessage("The actions should have not matched")
            .that(deepLink.matches(NavDeepLinkRequest(null, DEEP_LINK_ACTION, null)))
            .isFalse()
    }

    @Test
    fun deepLinkEmptyFromAction() {
        try {
            NavDeepLink.Builder.fromAction("").build()
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "The NavDeepLink cannot have an empty action."
                )
        }
    }

    @Test
    fun deepLinkEmptySetAction() {
        try {
            NavDeepLink.Builder.fromUriPattern(DEEP_LINK_EXACT_HTTPS).setAction("").build()
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat().contains(
                    "The NavDeepLink cannot have an empty action."
                )
        }
    }
}
