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
import androidx.core.net.toUri
import androidx.navigation.NavDestination.Companion.createRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.test.intArgument
import androidx.navigation.test.nullableStringArgument
import androidx.navigation.test.stringArgument
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import org.junit.Test

@SmallTest
class NavDestinationAndroidTest {
    companion object {
        private const val DESTINATION_ROUTE = "test"
        private const val URI_PATTERN = "uriPattern"
        private const val TEST_ACTION_ID = 1
        private const val TEST_ACTION_DESTINATION_ID = 2
        private const val TEST_ARG_KEY = "stringArg"
        private const val TEST_ARG_VALUE_AAA = "aaa"
    }

    @Test
    fun setBlankRoute() {
        val destination = NoOpNavigator().createDestination()
        assertThat(destination.route).isNull()
        assertThat(destination.id).isEqualTo(0)

        try {
            destination.route = ""
        } catch (e: IllegalArgumentException) {
            assertWithMessage("setting blank route should throw an error")
                .that(e)
                .hasMessageThat()
                .contains("Cannot have an empty route")
        }
    }

    @Test
    fun setNullRoute() {
        val destination = NoOpNavigator().createDestination()
        assertThat(destination.route).isNull()
        assertThat(destination.id).isEqualTo(0)

        destination.route = "route"
        destination.id = 1
        assertThat(destination.route).isEqualTo("route")
        assertThat(destination.id).isEqualTo(1)
        assertThat(destination.hasDeepLink(createRoute("route").toUri())).isFalse()

        destination.route = null
        assertThat(destination.route).isNull()
        assertThat(destination.id).isEqualTo(0)
        assertThat(destination.hasDeepLink(createRoute("route").toUri())).isFalse()
    }

    @Test
    fun setRouteChangesId() {
        val destination = NoOpNavigator().createDestination()
        destination.id = 1
        assertThat(destination.id).isEqualTo(1)

        destination.route = "test"
        assertThat(destination.route).isEqualTo("test")
        assertThat(destination.id).isNotEqualTo(1)
    }

    @Test
    fun setIdKeepsRoute() {
        val destination = NoOpNavigator().createDestination()
        destination.route = "test"
        assertThat(destination.route).isEqualTo("test")

        destination.id = 1
        assertThat(destination.id).isEqualTo(1)
        assertThat(destination.route).isEqualTo("test")
    }

    @Test
    fun addDeepLinkNullableArgumentNotRequired() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("myArg", nullableStringArgument())
        destination.addDeepLink("www.example.com/users?myArg={myArg}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users?"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
    }

    @Test
    fun matchDeepLink() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users/{id}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users/43"))

        assertWithMessage("Deep link should match").that(match).isNotNull()

        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
    }

    @Test
    fun matchDeepLinkWithQueryParams() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users?id={id}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users?id=43"))

        assertWithMessage("Deep link should match").that(match).isNotNull()

        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
    }

    @Test
    fun matchDeepLinkWithNonMatchingQueryParams() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users?userId={id}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users?userId=43"))

        assertWithMessage("Deep link should match").that(match).isNotNull()

        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
    }

    @Test
    fun matchDeepLinkWithSingleQueryParamAndFrag() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("id", intArgument())
        destination.addDeepLink("www.example.com/users?{id}#{myFrag}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users?43#theFrag"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getInt("id"))
            .isEqualTo(43)
    }

    @Test
    fun matchDeepLinkFragExactMatch() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("frag", nullableStringArgument())
        destination.addDeepLink("www.example.com/users")
        destination.addDeepLink("www.example.com/users#{frag}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users#testFrag"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with frag")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract frag argument correctly")
            .that(match?.matchingArgs?.getString("frag"))
            .isEqualTo("testFrag")
    }

    @Test
    fun matchDeepLinkFragBestMatchSingleArg() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("frag", nullableStringArgument())
        destination.addArgument("frag2", nullableStringArgument())
        destination.addDeepLink("www.example.com/users#{frag1}&{frag2}")
        destination.addDeepLink("www.example.com/users#{frag}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users#testFrag"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with link with single frag")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract frag argument correctly")
            .that(match?.matchingArgs?.getString("frag"))
            .isEqualTo("testFrag")
    }

    @Test
    fun matchDeepLinkFragBestMatchMultiArg() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("frag", nullableStringArgument())
        destination.addArgument("frag2", nullableStringArgument())
        destination.addDeepLink("www.example.com/users#{frag1}_{frag2}")
        destination.addDeepLink("www.example.com/users#{frag}")

        val match =
            destination.matchDeepLink(
                Uri.parse("https://www.example.com/users#testFrag1_testFrag2")
            )

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with link with multiple frags")
            .that(match?.matchingArgs?.size())
            .isEqualTo(2)
        assertWithMessage("Deep link should extract first frag argument correctly")
            .that(match?.matchingArgs?.getString("frag1"))
            .isEqualTo("testFrag1")
        assertWithMessage("Deep link should extract second frag argument correctly")
            .that(match?.matchingArgs?.getString("frag2"))
            .isEqualTo("testFrag2")
    }

    @Test
    fun matchDeepLinkWithOptionalFragMatch() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("frag", nullableStringArgument())
        destination.addDeepLink("www.example.com/users")
        destination.addDeepLink("www.example.com/users#param1={value1}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users#myFrag"))
        // fragment is optional here, the fragment not matching should not stop it from
        // matching with the link without fragment
        assertWithMessage("Deep link should match with the link without fragment")
            .that(match)
            .isNotNull()
        assertWithMessage("Deep link should not match with frag")
            .that(match?.matchingArgs?.size())
            .isEqualTo(0)
    }

    @Test
    fun matchDeepLinkWithRequiredFragNoMatch() {
        val destination = NoOpNavigator().createDestination()

        destination.addDeepLink("www.example.com/users")
        destination.addArgument("value1", stringArgument())
        destination.addDeepLink("www.example.com/users#param1={value1}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users"))
        // the fragment is a required argument in this case so the requested link should
        // not match with the exact path
        assertWithMessage("Deep link should not match").that(match).isNull()
    }

    @Test
    fun matchDeepLinkBestMatchExact() {
        val destination = NoOpNavigator().createDestination()

        destination.addDeepLink("www.example.com/users/index.html")

        destination.addArgument("name", nullableStringArgument(null))
        destination.addDeepLink("www.example.com/users/{name}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users/index.html"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should pick the exact match")
            .that(match?.matchingArgs?.size())
            .isEqualTo(0)
    }

    @Test
    fun matchDeepLinkBestMatchExactWithQuery() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("tab", stringArgument())
        destination.addDeepLink("www.example.com/users/anonymous?tab={tab}")

        destination.addArgument("name", nullableStringArgument(null))
        destination.addDeepLink("www.example.com/users/{name}?tab={tab}")

        val match =
            destination.matchDeepLink(
                Uri.parse("https://www.example.com/users/anonymous?tab=favorite")
            )

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should pick the exact match with query")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract tab argument correctly")
            .that(match?.matchingArgs?.getString("tab"))
            .isEqualTo("favorite")
    }

    @Test
    fun matchDeepLinkBestMatchExactPathSegments() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("user", stringArgument("testUser"))
        destination.addArgument("name", stringArgument("testName"))

        destination.addDeepLink("www.test.com/{user}/{name}")
        destination.addDeepLink("www.test.com/testUser/{name}")

        val match = destination.matchDeepLink(Uri.parse("https://www.test.com/testUser/testName"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with most exact path segments")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract name argument correctly")
            .that(match?.matchingArgs?.getString("name"))
            .isEqualTo("testName")
    }

    @Test
    fun matchDeepLinkBestMatchExactPathSegmentsWithQueryParams() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("user", stringArgument("testUser"))
        destination.addArgument("name", stringArgument("testName"))
        destination.addArgument("param", stringArgument("testArg"))

        destination.addDeepLink("www.test.com/{user}/{name}?param={arg}")
        destination.addDeepLink("www.test.com/testUser/{name}?param={arg}")

        val match =
            destination.matchDeepLink(
                Uri.parse("https://www.test.com/testUser/testName?param=testArg")
            )

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with most exact path segments")
            .that(match?.matchingArgs?.size())
            .isEqualTo(2)
    }

    @Test
    fun matchDeepLinkBestMatchNonconsecutiveExactPathSegments() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("two", stringArgument("two"))
        destination.addArgument("three", stringArgument("three"))
        destination.addArgument("four", stringArgument("four"))

        destination.addDeepLink("www.test.com/one/two/{three}/{four}")
        destination.addDeepLink("www.test.com/one/{two}/three/four")

        val match = destination.matchDeepLink(Uri.parse("https://www.test.com/one/two/three/four"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should match with most exact path segments")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract 'two' argument correctly")
            .that(match?.matchingArgs?.getString("two"))
            .isEqualTo("two")
    }

    @Test
    fun matchDeepLinkBestMatchNullableQueryParam() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("tab", stringArgument())
        destination.addDeepLink("www.example.com/users/anonymous?tab={tab}")

        // optional query param, meaning the match/requested URI does not have to contain both args
        destination.addArgument("tab2", nullableStringArgument())
        destination.addDeepLink("www.example.com/users/anonymous?tab={tab}&tab2={tab2}")

        val match =
            destination.matchDeepLink(
                Uri.parse("https://www.example.com/users/anonymous?tab=favorite")
            )

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should pick the exact match with query")
            .that(match?.matchingArgs?.size())
            .isEqualTo(1)
        assertWithMessage("Deep link should extract tab argument correctly")
            .that(match?.matchingArgs?.getString("tab"))
            .isEqualTo("favorite")
    }

    @Test
    fun matchDeepLinkBestMatchRequiredQueryParam() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("tab", stringArgument())
        destination.addDeepLink("www.example.com/users/anonymous?tab={tab}")

        destination.addArgument("tab2", stringArgument())
        destination.addDeepLink("www.example.com/users/anonymous?tab={tab}&tab2={tab2}")

        // both query params are required, this URI with only one arg should not match at all
        val match =
            destination.matchDeepLink(
                Uri.parse("https://www.example.com/users/anonymous?tab=favorite")
            )

        assertWithMessage("Deep link should not match").that(match).isNull()
    }

    @Test
    fun matchDotStar() {
        val destination = NoOpNavigator().createDestination()

        destination.addDeepLink("www.example.com/.*")
        destination.addDeepLink("www.example.com/{name}")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/foo"))
        assertWithMessage("Deep link should match").that(match).isNotNull()
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

        val match =
            destination.matchDeepLink(Uri.parse("https://www.example.com/users/43/posts/99"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
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
    fun matchDeepLinkBestMatchPathTail() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("id", stringArgument())
        destination.addDeepLink("www.example.com/users/{id}")
        destination.addDeepLink("www.example.com/users/{id}/posts")

        val match = destination.matchDeepLink(Uri.parse("https://www.example.com/users/u43/posts"))

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link should extract id argument correctly")
            .that(match?.matchingArgs?.getString("id"))
            .isEqualTo("u43")
    }

    @Test
    fun matchDeepLinkBestMimeType() {
        val destination = NoOpNavigator().createDestination()

        destination.addArgument("deeplink1", nullableStringArgument(null))
        destination.addDeepLink(NavDeepLink("www.example.com/users/{deeplink1}", null, "*/*"))

        destination.addArgument("deeplink2", nullableStringArgument(null))
        destination.addDeepLink(NavDeepLink("www.example.com/users/{deeplink2}", null, "image/*"))

        val match =
            destination.matchDeepLink(
                NavDeepLinkRequest(
                    Uri.parse("https://www.example.com/users/result"),
                    null,
                    "image/jpg"
                )
            )

        assertWithMessage("Deep link should match").that(match).isNotNull()
        assertWithMessage("Deep link matching arg should be deeplink2")
            .that(match?.matchingArgs?.getString("deeplink2"))
            .isEqualTo("result")
    }

    @Test
    fun testIsValidDeepLinkValidLinkExact() {
        val destination = NoOpNavigator().createDestination()
        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")
        destination.addDeepLink(deepLink.toString())

        assertWithMessage("Deep link should match").that(destination.hasDeepLink(deepLink)).isTrue()
    }

    @Test
    fun testRouteCreatesValidDeepLink() {
        val destination = NoOpNavigator().createDestination()
        destination.route = "route"
        val deepLink = Uri.parse("android-app://androidx.navigation.test/route")
        destination.addDeepLink(deepLink.toString())

        assertWithMessage("Deep link should match").that(destination.hasDeepLink(deepLink)).isTrue()
    }

    @Test
    fun testIsValidDeepLinkValidLinkPattern() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("testString", stringArgument())
        destination.addDeepLink("android-app://androidx.navigation.test/{testString}")

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test")
        assertWithMessage("Deep link should match").that(destination.hasDeepLink(deepLink)).isTrue()
    }

    @Test
    fun testIsValidDeepLinkInvalidLink() {
        val destination = NoOpNavigator().createDestination()

        val deepLink = Uri.parse("android-app://androidx.navigation.test/invalid")

        assertWithMessage("Deep link should not match")
            .that(destination.hasDeepLink(deepLink))
            .isFalse()
    }

    @Test
    fun testIsValidDeepLinkInvalidLinkPathTail() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("testString", stringArgument())
        destination.addDeepLink("android-app://androidx.navigation.test/{testString}")

        val deepLink = Uri.parse("android-app://androidx.navigation.test/test/extra")

        assertWithMessage("Deep link should not match")
            .that(destination.hasDeepLink(deepLink))
            .isFalse()
    }

    @Test
    fun addInDefaultArgs() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("stringArg", stringArgument("aaa"))
        destination.addArgument("intArg", intArgument(123))

        val bundle = destination.addInDefaultArgs(Bundle().apply { putString("stringArg", "bbb") })
        assertThat(bundle?.getString("stringArg")).isEqualTo("bbb")
        assertThat(bundle?.getInt("intArg")).isEqualTo(123)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addInDefaultArgsWrong() {
        val destination = NoOpNavigator().createDestination()
        destination.addArgument("stringArg", stringArgument("aaa"))
        destination.addArgument("intArg", intArgument(123))

        destination.addInDefaultArgs(Bundle().apply { putInt("stringArg", 123) })
    }

    @Test
    fun equalsNull() {
        val destination = NoOpNavigator().createDestination()

        assertThat(destination).isNotEqualTo(null)
    }

    @Test
    fun equals() {
        val destination = NoOpNavigator().createDestination()
        destination.route = DESTINATION_ROUTE
        destination.addDeepLink(URI_PATTERN)
        destination.addArgument(TEST_ARG_KEY, stringArgument(TEST_ARG_VALUE_AAA))
        destination.putAction(TEST_ACTION_ID, NavAction(TEST_ACTION_DESTINATION_ID))

        val destination2 = NoOpNavigator().createDestination()
        destination2.route = DESTINATION_ROUTE
        destination2.addDeepLink(URI_PATTERN)
        destination2.addArgument(TEST_ARG_KEY, stringArgument(TEST_ARG_VALUE_AAA))
        destination2.putAction(TEST_ACTION_ID, NavAction(TEST_ACTION_DESTINATION_ID))

        assertThat(destination).isEqualTo(destination2)
    }

    @Test
    fun equalsDifferentDeepLink() {
        val destination = NoOpNavigator().createDestination()
        destination.route = DESTINATION_ROUTE
        destination.addDeepLink(URI_PATTERN)
        destination.addArgument(TEST_ARG_KEY, stringArgument(TEST_ARG_VALUE_AAA))

        val destination2 = NoOpNavigator().createDestination()
        destination2.route = DESTINATION_ROUTE
        destination2.addDeepLink("differentPattern")
        destination2.addArgument(TEST_ARG_KEY, stringArgument(TEST_ARG_VALUE_AAA))

        assertThat(destination).isNotEqualTo(destination2)
    }

    @Test
    fun equalsDifferentArgumentValues() {
        val destination = NoOpNavigator().createDestination()
        destination.route = DESTINATION_ROUTE
        destination.addDeepLink(URI_PATTERN)
        destination.addArgument(TEST_ARG_KEY, stringArgument(TEST_ARG_VALUE_AAA))

        val destination2 = NoOpNavigator().createDestination()
        destination2.route = DESTINATION_ROUTE
        destination2.addDeepLink(URI_PATTERN)
        destination2.addArgument(TEST_ARG_KEY, stringArgument("bbb"))

        assertThat(destination).isNotEqualTo(destination2)
    }

    @Test
    fun hasRoute() {
        @Serializable class TestClass

        val destination =
            NavDestinationBuilder(NoOpNavigator(), TestClass::class, emptyMap()).build()
        assertThat(destination.hasRoute<TestClass>()).isTrue()
    }

    @Test
    fun hasRouteArgs() {
        @Serializable class TestClass(val arg: Int)

        val destination =
            NavDestinationBuilder(NoOpNavigator(), TestClass::class, emptyMap()).build()
        assertThat(destination.hasRoute<TestClass>()).isTrue()
    }

    @Test
    fun hasRouteArgsCustomType() {
        @Serializable class TestArg
        val testArg =
            object : NavType<TestArg>(true) {
                override fun put(bundle: Bundle, key: String, value: TestArg) {}

                override fun get(bundle: Bundle, key: String): TestArg? = null

                override fun parseValue(value: String) = TestArg()
            }

        @Serializable class TestClass(val arg: TestArg)

        val destination =
            NavDestinationBuilder(
                    NoOpNavigator(),
                    TestClass::class,
                    mapOf(typeOf<TestArg>() to testArg)
                )
                .build()
        assertThat(destination.hasRoute<TestClass>()).isTrue()
    }

    @Test
    fun hasRouteWrongClass() {
        @Serializable class TestClass(val arg: Int)
        @Serializable class WrongClass(val arg: Int)

        val destination =
            NavDestinationBuilder(NoOpNavigator(), TestClass::class, emptyMap()).build()
        assertThat(destination.hasRoute<WrongClass>()).isFalse()
    }

    @Test
    fun hasRouteWrongId() {
        @Serializable class TestClass(val arg: Int)

        val destination =
            NavDestinationBuilder(NoOpNavigator(), TestClass::class, emptyMap()).build()

        destination.id = 0

        assertThat(destination.hasRoute<TestClass>()).isFalse()
    }

    @Test
    fun routeNotAddedToDeepLink() {
        val destination = NoOpNavigator().createDestination()
        assertThat(destination.route).isNull()

        destination.route = "route"
        assertThat(destination.route).isEqualTo("route")
        assertThat(destination.hasDeepLink(createRoute("route").toUri())).isFalse()
    }

    @Test
    fun matchRoute() {
        val destination = NoOpNavigator().createDestination()

        destination.route = "route"
        assertThat(destination.route).isEqualTo("route")

        val match = destination.matchRoute("route")
        assertThat(match).isNotNull()
        assertThat(match!!.destination).isEqualTo(destination)
    }

    @Test
    fun matchRouteAfterSetNewRoute() {
        val destination = NoOpNavigator().createDestination()

        destination.route = "route"
        assertThat(destination.route).isEqualTo("route")

        val match = destination.matchRoute("route")
        assertThat(match).isNotNull()
        assertThat(match!!.destination).isEqualTo(destination)

        destination.route = "newRoute"
        assertThat(destination.route).isEqualTo("newRoute")
        val match2 = destination.matchRoute("newRoute")
        assertThat(match2).isNotNull()
        assertThat(match2!!.destination).isEqualTo(destination)
    }
}
