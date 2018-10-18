/*
 * Copyright (C) 2017 The Android Open Source Project
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
import java.io.UnsupportedEncodingException

@SmallTest
class NavDeepLinkTest {

    companion object {
        private const val DEEP_LINK_EXACT_NO_SCHEME = "www.example.com"
        private const val DEEP_LINK_EXACT_HTTP = "http://$DEEP_LINK_EXACT_NO_SCHEME"
        private const val DEEP_LINK_EXACT_HTTPS = "https://$DEEP_LINK_EXACT_NO_SCHEME"
    }

    @Test
    fun deepLinkExactMatch() {
        val deepLink = NavDeepLink(DEEP_LINK_EXACT_HTTP)

        assertWithMessage("HTTP link should match HTTP")
            .that(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTP)))
            .isTrue()
        assertWithMessage("HTTP link should not match HTTPS")
            .that(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTPS)))
            .isFalse()
    }

    @Test
    fun deepLinkExactMatchWithHyphens() {
        val deepLinkString = "android-app://com.example"
        val deepLink = NavDeepLink(deepLinkString)

        assertThat(deepLink.matches(Uri.parse(deepLinkString)))
            .isTrue()
    }

    @Test
    fun deepLinkExactMatchWithPlus() {
        val deepLinkString = "android+app://com.example"
        val deepLink = NavDeepLink(deepLinkString)

        assertThat(deepLink.matches(Uri.parse(deepLinkString)))
            .isTrue()
    }

    @Test
    fun deepLinkExactMatchWithPeriods() {
        val deepLinkString = "android.app://com.example"
        val deepLink = NavDeepLink(deepLinkString)

        assertThat(deepLink.matches(Uri.parse(deepLinkString)))
            .isTrue()
    }

    @Test
    fun deepLinkExactMatchNoScheme() {
        val deepLink = NavDeepLink(DEEP_LINK_EXACT_NO_SCHEME)

        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTP)))
            .isTrue()
        assertWithMessage("No scheme deep links should match https")
            .that(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTPS)))
            .isTrue()
    }

    @Test
    fun deepLinkArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "2"
        val matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id)))
        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("id"))
            .isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "2"
        val matchArgs = deepLink.getMatchingArguments(
            Uri.parse(deepLinkArgument.replace("{id}", id)))
        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("id"))
            .isEqualTo(id)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun deepLinkArgumentMatchEncoded() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{name}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val name = "John Doe"
        val matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", Uri.encode(name))))

        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the name")
            .that(matchArgs?.getString("name"))
            .isEqualTo(name)
    }

    @Test
    fun deepLinkMultipleArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts/{postId}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "2"
        val postId = "42"
        val matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id).replace("{postId}", postId)))
        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("id"))
            .isEqualTo(id)
        assertWithMessage("Args should contain the postId")
            .that(matchArgs?.getString("postId"))
            .isEqualTo(postId)
    }

    @Test
    fun deepLinkEmptyArgumentNoMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        assertThat(deepLink.matches(Uri.parse(deepLinkArgument.replace("{id}", ""))))
            .isFalse()
    }

    @Test
    fun deepLinkPrefixMatch() {
        val deepLinkPrefix = "$DEEP_LINK_EXACT_HTTPS/posts/.*"
        val deepLink = NavDeepLink(deepLinkPrefix)

        assertThat(deepLink.matches(Uri.parse(deepLinkPrefix.replace(".*", "test"))))
            .isTrue()
    }

    @Test
    fun deepLinkWildcardMatch() {
        val deepLinkWildcard = "$DEEP_LINK_EXACT_HTTPS/posts/.*/new"
        val deepLink = NavDeepLink(deepLinkWildcard)

        assertThat(deepLink.matches(Uri.parse(deepLinkWildcard.replace(".*", "test"))))
            .isTrue()
    }

    @Test
    fun deepLinkWildcardBeforeArgumentMatch() {
        val deepLinkMultiple = "$DEEP_LINK_EXACT_HTTPS/users/.*/posts/{postId}"
        val deepLink = NavDeepLink(deepLinkMultiple)

        val postId = "2"
        val matchArgs = deepLink.getMatchingArguments(
            Uri.parse(deepLinkMultiple
                .replace(".*", "test")
                .replace("{postId}", postId)))
        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the postId")
            .that(matchArgs?.getString("postId"))
            .isEqualTo(postId)
    }

    @Test
    fun deepLinkMultipleMatch() {
        val deepLinkMultiple = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts/.*"
        val deepLink = NavDeepLink(deepLinkMultiple)

        val id = "2"
        val matchArgs = deepLink.getMatchingArguments(
                Uri.parse(deepLinkMultiple
                    .replace("{id}", id)
                    .replace(".*", "test")))
        assertWithMessage("Args should not be null")
            .that(matchArgs)
            .isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("id"))
            .isEqualTo(id)
    }
}
