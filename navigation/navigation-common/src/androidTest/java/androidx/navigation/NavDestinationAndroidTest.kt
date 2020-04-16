/*
 * Copyright 2018 The Android Open Source Project
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
import android.os.Bundle
import androidx.navigation.test.intArgument
import androidx.navigation.test.stringArgument
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

@SmallTest
class NavDestinationAndroidTest {
    @Test
    fun matchDeepLink() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users/{id}")

        val match = destination.matchDeepLink(
            Uri.parse("https://www.example.com/users/43"))

        assertWithMessage("Deep link should match")
            .that(match)
            .isNotNull()

        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
    }

    @Test
    fun matchDeepLinkBestMatchExact() {
        val destination = NoOpNavigator().createDestination()

        destination.addDeepLink("www.example.com/users/index.html")

        destination.addArgument("id", stringArgument())
        destination.addDeepLink("www.example.com/users/{name}")

        val match = destination.matchDeepLink(
            Uri.parse("https://www.example.com/users/index.html"))

        assertWithMessage("Deep link should match")
            .that(match)
            .isNotNull()
        assertWithMessage("Deep link should pick the exact match")
            .that(match?.matchingArgs?.size())
            .isEqualTo(0)
    }

    @Test
    fun matchDotStar() {
        val destination = NoOpNavigator().createDestination()

        destination.addDeepLink("www.example.com/.*")
        destination.addDeepLink("www.example.com/{name}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/foo"))
        assertWithMessage("Deep link should match")
            .that(match)
            .isNotNull()
        assertWithMessage("Deep link should pick name over .*")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
    }

    @Test
    fun matchDeepLinkBestMatch() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users/{id}")

        destination.addArgument("postId", intArgument())
        destination.addDeepLink("www.example.com/users/{id}/posts/{postId}")

        val match = destination.matchDeepLink(
            Uri.parse("https://www.example.com/users/43/posts/99"))

        assertWithMessage("Deep link should match")
            .that(match)
            .isNotNull()
        assertWithMessage("Deep link should pick the argument with more matching arguments")
            .that(match?.matchingArgs?.size())
            .isEqualTo(2)
        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
        assertWithMessage("Deep link should extract postId argument correctly")
            .that(match?.matchingArgs?.getInt("postId"))
            .isEqualTo(99)
    }

    @Test
    fun matchDeepLinkBestMimeType() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("deeplink1", stringArgument())
        destination.addDeepLink(NavDeepLink("www.example.com/users/{deeplink1}",
            null, "*/*"))

        destination.addArgument("deeplink2", stringArgument())
        destination.addDeepLink(NavDeepLink("www.example.com/users/{deeplink2}",
            null, "image/*"))

        val match = destination.matchDeepLink(
            NavDeepLinkRequest(Uri.parse("https://www.example.com/users/result"), null,
                "image/jpg"))

        assertWithMessage("Deep link should match")
            .that(match)
            .isNotNull()
        assertWithMessage("Deep link matching arg should be deeplink2")
            .that(match?.matchingArgs?.getString("deeplink2"))
            .isEqualTo("result")
    }

    @Test
    fun testIsValidDeepLinkValidLinkExact() {
        val destination = NoOpNavigator().createDestination()
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")
        destination.addDeepLink(deepLink.toString())

        assertWithMessage("Deep link should match")
            .that(destination.hasDeepLink(deepLink)).isTrue()
    }

    @Test
    fun testIsValidDeepLinkValidLinkPattern() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("testString", stringArgument())
        destination.addDeepLink("android-app://androidx.navigation.test/{testString}")
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")
        destination.addDeepLink(deepLink.toString())

        assertWithMessage("Deep link should match")
            .that(destination.hasDeepLink(deepLink)).isTrue()
    }

    @Test
    fun testIsValidDeepLinkInvalidLink() {
        val destination = NoOpNavigator().createDestination()

        val deepLink = Uri.parse("android-app://androidx.navigation.test/invalid")

        assertWithMessage("Deep link should not match")
            .that(destination.hasDeepLink(deepLink)).isFalse()
    }

    @Test
    fun addInDefaultArgs() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("stringArg", stringArgument("aaa"))
        destination.addArgument("intArg", intArgument(123))

        val bundle = destination.addInDefaultArgs(Bundle().apply {
            putString("stringArg", "bbb")
        })
        assertThat(bundle?.getString("stringArg")).isEqualTo("bbb")
        assertThat(bundle?.getInt("intArg")).isEqualTo(123)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addInDefaultArgsWrong() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("stringArg", stringArgument("aaa"))
        destination.addArgument("intArg", intArgument(123))

        destination.addInDefaultArgs(Bundle().apply {
            putInt("stringArg", 123)
        })
    }
}
