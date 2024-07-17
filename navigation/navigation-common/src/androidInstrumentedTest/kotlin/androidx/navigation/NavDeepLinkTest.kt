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
import androidx.navigation.NavType.Companion.BoolListType
import androidx.navigation.NavType.Companion.FloatListType
import androidx.navigation.NavType.Companion.IntListType
import androidx.navigation.NavType.Companion.LongListType
import androidx.navigation.serialization.generateRouteWithArgs
import androidx.navigation.test.booleanArgument
import androidx.navigation.test.intArgument
import androidx.navigation.test.intArgumentUnknownDefault
import androidx.navigation.test.intListArgument
import androidx.navigation.test.nullableStringArgument
import androidx.navigation.test.nullableStringArgumentUnknownDefault
import androidx.navigation.test.stringArgument
import androidx.navigation.test.stringArrayArgument
import androidx.navigation.test.stringListArgument
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.UnsupportedEncodingException
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Test

@SmallTest
class NavDeepLinkTest {

    companion object {
        private const val DEEP_LINK_EXACT_NO_SCHEME = "www.example.com"
        private const val DEEP_LINK_EXACT_HTTP = "http://$DEEP_LINK_EXACT_NO_SCHEME"
        private const val DEEP_LINK_EXACT_HTTPS = "https://$DEEP_LINK_EXACT_NO_SCHEME"
    }

    @Test
    fun deepLinkNoUriNoMatch() {
        val deepLink = NavDeepLink(null, "test.action", null)

        assertWithMessage("NavDeepLink shouldn't match with null Uri")
            .that(deepLink.matches(Uri.parse(DEEP_LINK_EXACT_HTTP)))
            .isFalse()
        assertWithMessage("NavDeepLink shouldn't find matching arguments with null Uri")
            .that(deepLink.getMatchingArguments(Uri.parse(DEEP_LINK_EXACT_HTTP), mapOf()))
            .isNull()
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

        assertThat(deepLink.matches(Uri.parse(deepLinkString))).isTrue()
    }

    @Test
    fun deepLinkExactMatchWithPlus() {
        val deepLinkString = "android+app://com.example"
        val deepLink = NavDeepLink(deepLinkString)

        assertThat(deepLink.matches(Uri.parse(deepLinkString))).isTrue()
    }

    @Test
    fun deepLinkExactMatchWithPeriods() {
        val deepLinkString = "android.app://com.example"
        val deepLink = NavDeepLink(deepLinkString)

        assertThat(deepLink.matches(Uri.parse(deepLinkString))).isTrue()
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
    fun deepLinkArgumentMatchWithoutArguments() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "2"
        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse(deepLinkArgument.replace("{id}", id)), mapOf())
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("id"))
            .isEqualTo(id)
    }

    @Test
    fun deepLinkArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id.toString())),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkArgumentInvalidMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "invalid"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id)),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should be null").that(matchArgs).isNull()
    }

    @Test
    fun deepLinkArgumentMatchWithQueryParams() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val myArg = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument.replace("{id}", id.toString()).replace("{myarg}", myArg)
                ),
                mapOf("id" to intArgument(), "myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(myArg)
    }

    // Ensure that arguments with multiple characters in the path get matched correctly.
    @Test
    fun deepLinkMultiCharacterArgumentMatchWithQueryParams() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 211
        val myArg = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument.replace("{id}", id.toString()).replace("{myarg}", myArg)
                ),
                mapOf("id" to intArgument(), "myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(myArg)
    }

    // Ensure that a question mark at the end of path params matches same as if there was no
    // question mark
    @Test
    fun deepLinkMultipleArgumentMatchQuestionMarkNoParams() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}?"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 211
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id.toString())),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkPathEmptyString() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{name}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", "")),
                mapOf("name" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("name"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyInt() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{name}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", "")),
                mapOf("name" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNull()
    }

    @Test
    fun deepLinkPathEmptyStringTrailingSlash() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{name}/"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", "")),
                mapOf("name" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("name"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringTrailingQuestionMark() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{name}?"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", "")),
                mapOf("name" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("name"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringFirstArg() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{first}", "").replace("{last}", "last")),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("last")
    }

    @Test
    fun deepLinkPathEmptyStringCentreArg() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{centre}/{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{first}", "first")
                        .replace("{centre}", "")
                        .replace("{last}", "last")
                ),
                mapOf(
                    "first" to stringArgument(),
                    "centre" to stringArgument(),
                    "last" to stringArgument()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("first")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("centre"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("last")
    }

    @Test
    fun deepLinkPathEmptyStringLastArg() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{first}", "first").replace("{last}", "")),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("first")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringMultiple() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{centre}/{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{first}", "")
                        .replace("{centre}", "centre")
                        .replace("{last}", "")
                ),
                mapOf(
                    "first" to stringArgument(),
                    "centre" to stringArgument(),
                    "last" to stringArgument()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("centre"))
            .isEqualTo("centre")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringConsecutive() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{centre}/{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{first}", "")
                        .replace("{centre}", "")
                        .replace("{last}", "")
                ),
                mapOf(
                    "first" to stringArgument(),
                    "centre" to stringArgument(),
                    "last" to stringArgument()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("centre"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringConsecutiveTrailingSlash() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{centre}/{last}/"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{first}", "")
                        .replace("{centre}", "")
                        .replace("{last}", "")
                ),
                mapOf(
                    "first" to stringArgument(),
                    "centre" to stringArgument(),
                    "last" to stringArgument()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("centre"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringConsecutiveTrailingQuestionMark() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}/{centre}/{last}?"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{first}", "")
                        .replace("{centre}", "")
                        .replace("{last}", "")
                ),
                mapOf(
                    "first" to stringArgument(),
                    "centre" to stringArgument(),
                    "last" to stringArgument()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("centre"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkPathEmptyStringWithQuery() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}?last={last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{first}", "").replace("{last}", "last")),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("last")
    }

    @Test
    fun deepLinkPathAndQueryEmptyString() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/{first}?last={last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{first}", "").replace("{last}", "")),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("first"))
            .isEqualTo("")
        assertWithMessage("Args should contain the id")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    // Ensure that path arguments between two literals matches appropriately
    @Test
    fun deepLinkMultiCharacterArgumentMiddleMatchWithQueryParams() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val myArg = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument.replace("{id}", id.toString()).replace("{myarg}", myArg)
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(myArg)
    }

    @Test
    fun deepLinkQueryParamArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id.toString())),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamArgumentInvalidMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = "invalid"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id)),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should be null").that(matchArgs).isNull()
    }

    @Test
    fun deepLinkQueryParamMultipleArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}&myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val myarg = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument.replace("{id}", id.toString()).replace("{myarg}", myarg)
                ),
                mapOf("id" to intArgument(), "myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(myarg)
    }

    @Test
    fun deepLinkQueryParamDefaultArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamNullableArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the argument")
            .that(matchArgs?.containsKey("myarg"))
            .isFalse()
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamDefaultArgumentMatchParamsInBraces() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkEmptyStringQueryParamArg() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myArg={arg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val arg = ""
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{arg}", arg)),
                mapOf("arg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.containsKey("arg")).isTrue()
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamNullableStringArgumentMatchParamsInBraces() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        // We allow {argName} values for String types
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo("{myarg}")
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamNullableStringCollectionArgumentMatchParamsInBraces() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("myarg" to stringListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        // We allow {argName} values for String types
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getStringArray("myarg"))
            .asList()
            .containsExactly("{myarg}")
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamNullableNonStringArgumentMatchParamsInBraces() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)
        val intArrayArg =
            NavArgument.Builder()
                .setType(NavType.IntArrayType)
                .setIsNullable(true)
                .setDefaultValue(null)
                .build()

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("myarg" to intArrayArg)
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        // For non-strings, {argName} values are invalid and considered lack of argument value
        assertWithMessage("Args should not contain the argument")
            .that(matchArgs?.containsKey("myarg"))
            .isFalse()
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamNullableNonStringCollectionArgumentMatchParamsInBraces() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)
        val intArrayArg =
            NavArgument.Builder().setType(NavType.IntArrayType).setIsNullable(true).build()

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("myarg" to intArrayArg)
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        // For non-strings, {argName} values are invalid and considered lack of argument value
        // If it's a collection type, it should default to emptyList when theres no default value
        assertWithMessage("Args should an empty collection").that(matchArgsIntArray).isEmpty()
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamNullableNonStringCollectionArgumentMatchParamsInBracesDeferToDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)
        val intArrayArg =
            NavArgument.Builder()
                .setType(NavType.IntArrayType)
                .setIsNullable(true)
                .setDefaultValue(intArrayOf(1, 2))
                .build()

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument),
                mapOf("myarg" to intArrayArg)
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    // Ensure case when matching the exact argument query (i.e. param names in braces) is handled
    @Test
    fun deepLinkQueryParamArgumentMatchParamsInBracesSameName() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{myarg}", "myarg")),
                mapOf(
                    "myarg" to
                        NavArgument.Builder()
                            .setType(NavType.StringType)
                            .setIsNullable(true)
                            .build()
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the argument and it should not be null")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo("myarg")
    }

    @Test
    fun deepLinkQueryParamMultipleArgumentMatchOptionalDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}&optional={optional}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val optional = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?id={id}".replace("{id}", id.toString())),
                mapOf("id" to intArgument(), "optional" to stringArgument(optional))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should not contain optional")
            .that(matchArgs?.containsKey("optional"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamMultipleArgumentReverseMatchOptionalDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}&optional={optional}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?optional={optional}&id={id}"
                        .replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument(), "optional" to stringArrayArgument(arrayOf("theArg")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should not contain optional")
            .that(matchArgs?.getStringArray("optional"))
            .isEqualTo(arrayOf("{optional}"))
    }

    @Test
    fun deepLinkQueryParamMultipleArgumentMatchOptionalNullable() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}&optional={optional}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?id={id}".replace("{id}", id.toString())),
                mapOf("id" to intArgument(), "optional" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should not contain optional")
            .that(matchArgs?.containsKey("optional"))
            .isFalse()
    }

    // Make sure we allow extra params that may not been part of the given deep link
    @Test
    fun deepLinkQueryParamArgumentIgnoreExtraParams() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users"
        val deepLink = NavDeepLink(deepLinkArgument)

        assertThat(
                deepLink.matches(Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?extraParam={extraParam}"))
            )
            .isTrue()
    }

    @Test
    fun deepLinkQueryParamArgumentMatchPathParamCorrectly() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{myarg}"
        val deepLinkArgumentWithExtraParam = "$deepLinkArgument?extraParam={extraParam}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val myarg = "test"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgumentWithExtraParam.replace("{myarg}", myarg)),
                mapOf("myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(myarg)
    }

    // Make sure we allow extra params that may not been part of the given deep link
    @Test
    fun deepLinkQueryParamArgumentMatchExtraParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?id={id}&extraParam={extraParam}"
                        .replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument())
            )

        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamArgumentMatchExtraParamOptionalDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?extraParam={extraParam}"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentMatchExtraParamOptionalNullable() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?id={id}&extraParam={extraParam}"),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the argument")
            .that(matchArgs?.containsKey("myarg"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentMatchDifferentParamName() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?string={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?string={id}".replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryNullableParamArgumentMatchDifferentParamName() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?string={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the argument")
            .that(matchArgs?.containsKey("myarg"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryDefaultParamArgumentMatchDifferentParamName() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?string={id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentMatchOnlyPartOfParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}L"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{id}", id.toString())),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryNullableParamArgumentMatchOnlyPartOfParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}L"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the argument")
            .that(matchArgs?.containsKey("myarg"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryDefaultParamArgumentMatchOnlyPartOfParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}L"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentMatchMultiArgsOneParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?name={first}_{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val first = "Jane"
        val last = "Doe"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{first}", first).replace("{last}", last)),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the first name")
            .that(matchArgs?.getString("first"))
            .isEqualTo(first)
        assertWithMessage("Args should contain the last name")
            .that(matchArgs?.getString("last"))
            .isEqualTo(last)
    }

    @Test
    fun deepLinkQueryDefaultParamArgumentMatchMultiArgsOneParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?name={first}_{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val first = "Jane"
        val last = "Doe"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("first" to stringArgument(first), "last" to stringArgument(last))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the first name")
            .that(matchArgs?.containsKey("first"))
            .isFalse()
        assertWithMessage("Args should not contain the last name")
            .that(matchArgs?.containsKey("last"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamOneDefaultArgumentMatchMultiArgsOneParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?name={first}_{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val first = "Jane"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?name=Jane_"),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the first name")
            .that(matchArgs?.getString("first"))
            .isEqualTo(first)
        assertWithMessage("Args should contain the empty last name")
            .that(matchArgs?.getString("last"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkQueryParamNoDefaultArgumentMatchMultiArgsNoParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?name={first}_{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val first = ""
        val last = ""
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?name=_"),
                mapOf("first" to stringArgument(), "last" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the empty first name")
            .that(matchArgs?.getString("first"))
            .isEqualTo(first)
        assertWithMessage("Args should contain the empty last name")
            .that(matchArgs?.getString("last"))
            .isEqualTo(last)
    }

    @Test
    fun deepLinkQueryNullableParamArgumentMatchMultiArgsOneParam() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?name={first}_{last}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf(
                    "first" to nullableStringArgument(null),
                    "last" to nullableStringArgument(null)
                )
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the first name")
            .that(matchArgs?.containsKey("first"))
            .isFalse()
        assertWithMessage("Args should not contain the last name")
            .that(matchArgs?.containsKey("last"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentWithWildCard() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId=.*-{id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?productId=wildCardMatch-{id}"
                        .replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamDefaultArgumentWithWildCard() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId=.*-{id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamNullableArgumentWithWildCard() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId=.*-{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?productId=wildCardMatch-{myarg}"),
                mapOf("myarg" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the argument")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo("{myarg}")
    }

    // Handle the case were the input is wild card and separator with no argument
    @Test
    fun deepLinkQueryParamDefaultArgumentWithWildCardOnly() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId=.*-{id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?productId=.*-"),
                mapOf("id" to intArgument(id))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the id")
            .that(matchArgs?.containsKey("id"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamArgumentWithStarInFront() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId=A*B{id}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?productId=A*B{id}".replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamArgumentWithStarInBack() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?productId={id}A*B"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    "$DEEP_LINK_EXACT_HTTPS/users?productId={id}A*B".replace("{id}", id.toString())
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkQueryParamArgumentWithRegex() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?path=go/to/{path}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val path = "directions"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?path=go/to/{path}".replace("{path}", path)),
                mapOf("path" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the path")
            .that(matchArgs?.getString("path"))
            .isEqualTo(path)
    }

    @Test
    fun deepLinkQueryParamDefaultArgumentWithRegex() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?path=go/to/{path}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val path = "directions"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("path" to stringArgument(path))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the path")
            .that(matchArgs?.containsKey("path"))
            .isFalse()
    }

    @Test
    fun deepLinkQueryParamNullableArgumentWithRegex() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?path=go/to/{path}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users"),
                mapOf("path" to nullableStringArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the path")
            .that(matchArgs?.containsKey("path"))
            .isFalse()
    }

    // Handle the case were the input could be entire path except for the argument
    @Test
    fun deepLinkQueryParamDefaultArgumentWithRegexOnly() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?path=go/to/{path}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?path=go/to/"),
                mapOf("path" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should not contain the path")
            .that(matchArgs?.getString("path"))
            .isEqualTo("")
    }

    @Test
    fun deepLinkFragmentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users#{frag}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users#testFrag"),
                mapOf("frag" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the fragment")
            .that(matchArgs?.getString("frag"))
            .isEqualTo("testFrag")
    }

    @Test
    fun deepLinkFragmentMatchWithQuery() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}#{frag}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?id=43#testFrag"),
                mapOf("id" to intArgument(), "frag" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the query")
            .that(matchArgs?.getInt("id"))
            .isEqualTo(43)
        assertWithMessage("Args should contain the fragment")
            .that(matchArgs?.getString("frag"))
            .isEqualTo("testFrag")
    }

    @Test
    fun deepLinkFragmentMatchWithOptionalQuery() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?id={id}#{frag}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users#testFrag"),
                mapOf("id" to nullableStringArgument(), "frag" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the fragment")
            .that(matchArgs?.getString("frag"))
            .isEqualTo("testFrag")
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun deepLinkArgumentMatchEncoded() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{name}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val name = "John Doe"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{name}", Uri.encode(name))),
                mapOf("name" to stringArgument())
            )

        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the name")
            .that(matchArgs?.getString("name"))
            .isEqualTo(name)
    }

    @Test
    fun deepLinkMultipleArgumentMatch() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts/{postId}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val postId = 42
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument
                        .replace("{id}", id.toString())
                        .replace("{postId}", postId.toString())
                ),
                mapOf("id" to intArgument(), "postId" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
        assertWithMessage("Args should contain the postId")
            .that(matchArgs?.getInt("postId"))
            .isEqualTo(postId)
    }

    @Test
    fun deepLinkEmptyArgumentMatches() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        assertThat(deepLink.matches(Uri.parse(deepLinkArgument.replace("{id}", "")))).isTrue()
    }

    @Test
    fun deepLinkPrefixMatch() {
        val deepLinkPrefix = "$DEEP_LINK_EXACT_HTTPS/posts/.*"
        val deepLink = NavDeepLink(deepLinkPrefix)

        assertThat(deepLink.matches(Uri.parse(deepLinkPrefix.replace(".*", "test")))).isTrue()
    }

    @Test
    fun deepLinkWildcardMatch() {
        val deepLinkWildcard = "$DEEP_LINK_EXACT_HTTPS/posts/.*/new"
        val deepLink = NavDeepLink(deepLinkWildcard)

        assertThat(deepLink.matches(Uri.parse(deepLinkWildcard.replace(".*", "test")))).isTrue()
    }

    @Test
    fun deepLinkWildcardBeforeArgumentMatch() {
        val deepLinkMultiple = "$DEEP_LINK_EXACT_HTTPS/users/.*/posts/{postId}"
        val deepLink = NavDeepLink(deepLinkMultiple)

        val postId = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkMultiple.replace(".*", "test").replace("{postId}", postId.toString())
                ),
                mapOf("postId" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the postId")
            .that(matchArgs?.getInt("postId"))
            .isEqualTo(postId)
    }

    @Test
    fun deepLinkMultipleMatch() {
        val deepLinkMultiple = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts/.*"
        val deepLink = NavDeepLink(deepLinkMultiple)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkMultiple.replace("{id}", id.toString()).replace(".*", "test")),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkCaseInsensitiveDomainWithPath() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("${DEEP_LINK_EXACT_HTTPS.uppercase()}/users/$id/posts"),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkCaseInsensitivePath() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{id}/posts"
        val deepLink = NavDeepLink(deepLinkArgument)

        val id = 2
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(
                    deepLinkArgument.replace("{id}", id.toString()).replace("users", "Users")
                ),
                mapOf("id" to intArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the id").that(matchArgs?.getInt("id")).isEqualTo(id)
    }

    @Test
    fun deepLinkCaseSensitiveQueryParams() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTP/?myParam={param}"
        val deepLink = NavDeepLink(deepLinkString)

        val param = 2
        val deepLinkUpper =
            deepLinkString.replace("myParam", "MYPARAM").replace("{param}", param.toString())
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkUpper),
                mapOf("param" to intArgument(0))
            )

        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args bundle should be empty").that(matchArgs?.isEmpty).isTrue()
    }

    @Test
    fun deepLinkNullableArgumentNotRequired() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkString)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkString),
                mapOf("myarg" to stringArrayArgument(arrayOf("theArg")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        // We allow {argName} values for String types
        assertWithMessage("Args bundle should contain arg value")
            .that(matchArgs?.getStringArray("myarg"))
            .isEqualTo(arrayOf("{myarg}"))
    }

    @Test
    fun ensureValueIsDecodedProperly() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkString)

        val value = "%555"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkString.replace("{myarg}", Uri.encode(value))),
                mapOf("myarg" to nullableStringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the value without additional decoding")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(value)
    }

    @Test
    fun ensureNewLineIsDecodedProperly() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkString)

        val value = "some\nthing"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkString.replace("{myarg}", Uri.encode(value))),
                mapOf("myarg" to nullableStringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the value without additional decoding")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo(value)
    }

    @Test
    fun deepLinkMissingRequiredArgumentUnknownDefault() {
        val deepLink = NavDeepLink("$DEEP_LINK_EXACT_HTTPS?myarg={myarg}")
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                // NavArgument with unknown default value
                mapOf("myarg" to intArgumentUnknownDefault())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkMissingNullableArgumentUnknownDefault() {
        val deepLink = NavDeepLink("$DEEP_LINK_EXACT_HTTPS?myarg={myarg}")
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                // NavArgument with unknown default value
                mapOf("myarg" to nullableStringArgumentUnknownDefault())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkMissingRequiredArgument() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTPS/greeting?title={title}&text={text}"
        val deepLink = NavDeepLink(deepLinkString)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/greeting?title=No%20text"),
                mapOf("title" to stringArgument(), "text" to stringArgument())
            )

        assertWithMessage("Args should be null").that(matchArgs).isNull()
    }

    @Test
    fun deepLinkMissingOptionalArgument() {
        val deepLinkString = "$DEEP_LINK_EXACT_HTTPS/greeting?text={text}"
        val deepLink = NavDeepLink(deepLinkString)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/greeting"),
                mapOf("text" to stringArgument("Default greeting"))
            )

        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args bundle should be empty").that(matchArgs?.isEmpty).isTrue()
    }

    @Test
    fun deepLinkArgumentDoesNotCrossPoundSign() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users/{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val args = "test#split"
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{myarg}", args)),
                mapOf("myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the arg")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo("test")
    }

    @Test
    fun deepLinkSingleQueryParamNoValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(deepLinkArgument.replace("{myarg}", "name")),
                mapOf("myarg" to stringArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        assertWithMessage("Args should contain the arg")
            .that(matchArgs?.getString("myarg"))
            .isEqualTo("name")
    }

    @Test
    fun deepLinkRepeatedQueryParamsMappedToArray() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?myarg=name1&myarg=name2"),
                mapOf("myarg" to stringArrayArgument(null))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain first arg")
            .that(matchArgsStringArray)
            .asList()
            .contains("name1")
        assertWithMessage("Args should contain second arg")
            .that(matchArgsStringArray)
            .asList()
            .contains("name2")
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun deepLinkRepeatedQueryParamsMappedToList() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val navArg = stringListArgument()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS/users?myarg=name1&myarg=name2"),
                mapOf("myarg" to navArg)
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringList = navArg.type[matchArgs!!, "myarg"] as List<String>
        assertWithMessage("Args list should not be null").that(matchArgsStringList).isNotNull()
        assertWithMessage("Args should contain first arg")
            .that(matchArgsStringList)
            .contains("name1")
        assertWithMessage("Args should contain second arg")
            .that(matchArgsStringList)
            .contains("name2")
    }

    @Test
    fun deepLinkNoRepeatedQueryParamsInPattern() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS/users?myarg={myarg}&myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)
        val message =
            assertFailsWith<IllegalArgumentException> {
                    // query params are parsed lazily, need to run getMatchingArguments to resolve
                    // it
                    deepLink.getMatchingArguments(Uri.parse(deepLinkArgument), emptyMap())
                }
                .message
        assertThat(message)
            .isEqualTo(
                "Query parameter myarg must only be present once in $deepLinkArgument. " +
                    "To support repeated query parameters, use an array type for your " +
                    "argument and the pattern provided in your URI will be used to " +
                    "parse each query parameter instance."
            )
    }

    @Test
    fun deepLinkStringListEmpty() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg="),
                mapOf("myarg" to stringListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain empty string")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("")
    }

    @Test
    fun deepLinkStringListMultipleEmpty() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=&myarg="),
                mapOf("myarg" to stringListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain empty string")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("", "")
    }

    @Test
    fun deepLinkStringListMissing() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                mapOf("myarg" to stringListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should be empty list").that(matchArgsStringArray).asList().isEmpty()
    }

    @Test
    fun deepLinkStringListMissingDeferToDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                mapOf("myarg" to stringListArgument(listOf("one", "two")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkStringListEmptyFirstValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=&myarg=two&myarg=three"),
                mapOf("myarg" to stringListArgument(listOf("default1", "default2")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain all values")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("", "two", "three")
            .inOrder()
    }

    @Test
    fun deepLinkStringListEmptyMiddleValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=one&myarg=&myarg=three"),
                mapOf("myarg" to stringListArgument(listOf("default1", "default2")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain all values")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("one", "", "three")
            .inOrder()
    }

    @Test
    fun deepLinkStringListEmptyLastValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=one&myarg=two&myarg="),
                mapOf("myarg" to stringListArgument(listOf("default1", "default2")))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should contain all values")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("one", "two", "")
            .inOrder()
    }

    @Test
    fun deepLinkStringListEmptySingleQueryParamValueOnly() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?"),
                mapOf("myarg" to stringListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsStringArray = matchArgs?.getStringArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsStringArray).isNotNull()
        assertWithMessage("Args should be empty list")
            .that(matchArgsStringArray)
            .asList()
            .containsExactly("")
    }

    @Test
    fun deepLinkIntListEmpty() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg="),
                mapOf("myarg" to intListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should be empty list").that(matchArgsIntArray).asList().isEmpty()
    }

    @Test
    fun deepLinkIntListMultipleEmpty() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=&myarg="),
                mapOf("myarg" to intListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should be empty list").that(matchArgsIntArray).asList().isEmpty()
    }

    @Test
    fun deepLinkIntListEmptyDeferToDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg="),
                mapOf("myarg" to intListArgument(listOf(1, 2)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkIntListMissing() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                mapOf("myarg" to intListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should be empty list").that(matchArgsIntArray).asList().isEmpty()
    }

    @Test
    fun deepLinkIntListMissingDeferToDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(DEEP_LINK_EXACT_HTTPS),
                mapOf("myarg" to intListArgument(listOf(1, 2)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkIntListEmptyFirstValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=&myarg=2&myarg=3"),
                mapOf("myarg" to intListArgument(listOf(-1, -1)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should contain non-empty values")
            .that(matchArgsIntArray)
            .asList()
            .containsExactly(2, 3)
            .inOrder()
    }

    @Test
    fun deepLinkIntListEmptyMiddleValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=1&myarg=&myarg=3"),
                mapOf("myarg" to intListArgument(listOf(-1, -1)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should contain non-empty values")
            .that(matchArgsIntArray)
            .asList()
            .containsExactly(1, 3)
            .inOrder()
    }

    @Test
    fun deepLinkIntListEmptyLastValue() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?myarg={myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("$DEEP_LINK_EXACT_HTTPS?myarg=1&myarg=2&myarg="),
                mapOf("myarg" to intListArgument(listOf(-1, -1)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should contain non-empty values")
            .that(matchArgsIntArray)
            .asList()
            .containsExactly(1, 2)
            .inOrder()
    }

    @Test
    fun deepLinkIntListMissingSingleQueryParamValueOnly() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("${DEEP_LINK_EXACT_HTTPS}?"),
                mapOf("myarg" to intListArgument())
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
        val matchArgsIntArray = matchArgs?.getIntArray("myarg")
        assertWithMessage("Args list should not be null").that(matchArgsIntArray).isNotNull()
        assertWithMessage("Args should be empty list").that(matchArgsIntArray).asList().isEmpty()
    }

    @Test
    fun deepLinkIntListMissingSingleQueryParamValueOnlyDeferToDefault() {
        val deepLinkArgument = "$DEEP_LINK_EXACT_HTTPS?{myarg}"
        val deepLink = NavDeepLink(deepLinkArgument)

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("${DEEP_LINK_EXACT_HTTPS}?"),
                mapOf("myarg" to intListArgument(listOf(1, 2)))
            )
        assertWithMessage("Args should not be null").that(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkExactMatchFromKClassNoScheme() {
        @Serializable class TestClass

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse("http://$uri")))
            .isTrue()
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse("https://$uri")))
            .isTrue()
    }

    @Test
    fun deepLinkExactMatchFromKClassPathArgNoScheme() {
        @Serializable class TestClass(val arg: Int)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val expected = "http://$uri/{arg}"
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(expected.replace("{arg}", "1")),
                mapOf("arg" to intArgument())
            )
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.getInt("arg")).isEqualTo(1)
    }

    @Test
    fun deepLinkExactMatchFromKClassQueryArgNoScheme() {
        @Serializable class TestClass(val arg: Int = 1)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val expected = "http://$uri?arg={arg}"
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(expected.replace("{arg}", "2")),
                mapOf("arg" to intArgumentUnknownDefault())
            )
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.getInt("arg")).isEqualTo(2)
    }

    @Test
    fun deepLinkExactMatchFromKClassPathQueryArgNoScheme() {
        @Serializable class TestClass(val arg: Int = 0, val arg2: Boolean)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val expected = "http://$uri/{arg2}?arg={arg}"
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(expected.replace("{arg}", "1").replace("{arg2}", "false")),
                mapOf("arg" to intArgumentUnknownDefault(), "arg2" to booleanArgument())
            )
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.getInt("arg")).isEqualTo(1)
        assertThat(matchArgs.getBoolean("arg2")).isEqualTo(false)
    }

    @Test
    fun deepLinkFromKClassMissingRequiredArgNoDefault() {
        @Serializable class TestClass(val arg: Int)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val matchArgs =
            deepLink.getMatchingArguments(
                // missing required arg in deeplink
                Uri.parse("http://$uri"),
                mapOf("arg" to intArgument())
            )
        // should not match since missing default value
        assertThat(matchArgs).isNull()
    }

    @Test
    fun deepLinkFromKClassMissingRequiredArgWithDefault() {
        @Serializable class TestClass(val arg: Int = 1)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val matchArgs =
            deepLink.getMatchingArguments(
                // missing required arg with default value
                Uri.parse("http://$uri"),
                mapOf("arg" to intArgumentUnknownDefault())
            )
        // should still match successfully since the string arg has default value
        assertThat(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkFromKClassMissingNullableArgNoDefault() {
        @Serializable class TestClass(val arg: String?)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("http://$uri"),
                mapOf("arg" to nullableStringArgument())
            )
        // should not match since missing nullable arg with no default value
        assertThat(matchArgs).isNull()
    }

    @Test
    fun deepLinkFromKClassMissingNullableArgWithDefault() {
        @Serializable class TestClass(val arg: String? = null)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse("http://$uri"),
                mapOf("arg" to nullableStringArgumentUnknownDefault())
            )
        assertThat(matchArgs).isNotNull()
    }

    @Test
    fun deepLinkFromKClassNullString() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: String?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val argName = "arg"
        val navArg = nullableStringArgument()
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(navArg.type[matchArgs, argName]).isNull()
    }

    @Test
    fun deepLinkFromKClassNullStringList() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: List<String>?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val argName = "arg"
        val navArg = stringListArgument()
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(matchArgs.getStringArray(argName)?.toList()).isEmpty()
    }

    @Test
    fun deepLinkFromKClassNullIntList() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: List<Int>?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val navArg = NavArgument.Builder().setType(IntListType).setIsNullable(true).build()

        val argName = "arg"
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(matchArgs.getIntArray(argName)?.toList()).isEmpty()
    }

    @Test
    fun deepLinkFromKClassNullBoolList() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: List<Boolean>?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val navArg = NavArgument.Builder().setType(BoolListType).setIsNullable(true).build()

        val argName = "arg"
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(matchArgs.getBooleanArray(argName)?.toList()).isEmpty()
    }

    @Test
    fun deepLinkFromKClassNullLongList() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: List<Long>?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val navArg = NavArgument.Builder().setType(LongListType).setIsNullable(true).build()

        val argName = "arg"
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(matchArgs.getLongArray(argName)?.toList()).isEmpty()
    }

    @Test
    fun deepLinkFromKClassNullFloatList() {
        @Serializable @SerialName("www.test.com") class TestClass(val arg: List<Float>?)

        val uri = "www.test.com"
        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val navArg = NavArgument.Builder().setType(FloatListType).setIsNullable(true).build()

        val argName = "arg"
        val route = generateRouteWithArgs(TestClass(null), mapOf(argName to navArg.type))

        val matchArgs =
            deepLink.getMatchingArguments(Uri.parse("http://$route"), mapOf(argName to navArg))
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.containsKey(argName)).isTrue()
        assertThat(matchArgs.getFloatArray(argName)?.toList()).isEmpty()
    }

    @Test
    fun deepLinkMatchFromKClassPartialMissingRequiredArgNoDefault() {
        @Serializable class TestClass(val arg: Int, val arg2: Boolean)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val expected = "http://$uri/{arg}/{arg2}"
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(expected.replace("{arg2}", "false")),
                mapOf("arg" to intArgument(), "arg2" to booleanArgument())
            )
        assertThat(matchArgs).isNull()
    }

    @Test
    fun deepLinkMatchFromKClassPartialMissingRequiredArgWithDefault() {
        @Serializable class TestClass(val arg: String? = "ffds", val arg2: Boolean)

        val uri = "www.test.com"

        val deepLink = NavDeepLink.Builder.fromUriPattern<TestClass>(uri).build()

        val expected = "http://$uri/{arg2}?arg={arg}"
        assertWithMessage("No scheme deep links should match http")
            .that(deepLink.matches(Uri.parse(expected)))
            .isTrue()
        val matchArgs =
            deepLink.getMatchingArguments(
                Uri.parse(expected.replace("{arg2}", "false")),
                mapOf("arg" to nullableStringArgumentUnknownDefault(), "arg2" to booleanArgument())
            )
        assertThat(matchArgs).isNotNull()
        assertThat(matchArgs!!.getBoolean("arg2")).isEqualTo(false)
    }
}
