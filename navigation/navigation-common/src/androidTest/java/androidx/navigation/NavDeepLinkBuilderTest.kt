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

import androidx.navigation.NavOptionsTest.TestClass
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.Serializable
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavDeepLinkBuilderTest {

    @Test
    fun buildDeepLinkAllSet() {
        val expectedUri = "example.com"
        val expectedAction = "test.action"
        val expectedMimeType = "test/type"
        val navDeepLink = navDeepLink {
            uriPattern = expectedUri
            action = expectedAction
            mimeType = expectedMimeType
        }
        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo(expectedUri)
        assertWithMessage("NavDeepLink should have action set")
            .that(navDeepLink.action)
            .isEqualTo(expectedAction)
        assertWithMessage("NavDeepLink should have mimeType set")
            .that(navDeepLink.mimeType)
            .isEqualTo(expectedMimeType)
    }

    @Test
    fun buildDeepLinkNoneSet() {
        try {
            navDeepLink {}
            fail("NavDeepLink must throw when attempting to build an empty builder.")
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat()
                .contains("The NavDeepLink must have an uri, action, and/or mimeType.")
        }
    }

    @Test
    fun buildDeepLinkEmptyAction() {
        try {
            navDeepLink { action = "" }
            fail("NavDeepLink must throw when attempting to build with an empty action.")
        } catch (e: IllegalArgumentException) {
            assertThat(e).hasMessageThat().contains("The NavDeepLink cannot have an empty action.")
        }
    }

    @Test
    fun buildDeepLinkDoubleActionSetNull() {
        val expectedUri = "www.example.com"
        val navDeepLink = navDeepLink {
            uriPattern = expectedUri
            action = "blah"
            action = null
        }

        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo(expectedUri)
        assertWithMessage("NavDeepLink should have action set").that(navDeepLink.action).isNull()
    }

    @Test
    fun buildDeepLinkAllSetKClass() {
        val expectedUri = "example.com"
        val expectedAction = "test.action"
        val expectedMimeType = "test/type"
        val navDeepLink =
            navDeepLink<TestClass>(expectedUri) {
                action = expectedAction
                mimeType = expectedMimeType
            }
        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo(expectedUri)
        assertWithMessage("NavDeepLink should have action set")
            .that(navDeepLink.action)
            .isEqualTo(expectedAction)
        assertWithMessage("NavDeepLink should have mimeType set")
            .that(navDeepLink.mimeType)
            .isEqualTo(expectedMimeType)
    }

    @Test
    fun buildDeepLinkAllSetKClassWithPathArgs() {
        @Serializable class TestClass(val arg: Int, val arg2: String)

        val expectedUri = "example.com"
        val expectedAction = "test.action"
        val expectedMimeType = "test/type"
        val navDeepLink =
            navDeepLink<TestClass>(expectedUri) {
                action = expectedAction
                mimeType = expectedMimeType
            }
        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo("$expectedUri/{arg}/{arg2}")
        assertWithMessage("NavDeepLink should have action set")
            .that(navDeepLink.action)
            .isEqualTo(expectedAction)
        assertWithMessage("NavDeepLink should have mimeType set")
            .that(navDeepLink.mimeType)
            .isEqualTo(expectedMimeType)
    }

    @Test
    fun buildDeepLinkAllSetKClassWithQueryArgs() {
        @Serializable class TestClass(val arg: Int, val arg2: String = "default")

        val expectedUri = "example.com"
        val expectedAction = "test.action"
        val expectedMimeType = "test/type"
        val navDeepLink =
            navDeepLink<TestClass>(expectedUri) {
                action = expectedAction
                mimeType = expectedMimeType
            }
        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo("$expectedUri/{arg}?arg2={arg2}")
        assertWithMessage("NavDeepLink should have action set")
            .that(navDeepLink.action)
            .isEqualTo(expectedAction)
        assertWithMessage("NavDeepLink should have mimeType set")
            .that(navDeepLink.mimeType)
            .isEqualTo(expectedMimeType)
    }

    @Test
    fun buildDeepLinkEmptyUriKClass() {
        var exception: IllegalArgumentException? = null
        try {
            navDeepLink<TestClass>("") { action = "action" }
            fail("NavDeepLink must throw when attempting to build an empty builder.")
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertThat(exception?.message)
            .isEqualTo("The basePath for NavDeepLink from KClass cannot be empty")
    }

    @Test
    fun buildDeepLinkEmptyActionKClass() {
        var exception: IllegalArgumentException? = null
        try {
            navDeepLink<TestClass>("base") { action = "" }
            fail("NavDeepLink must throw when attempting to build with an empty action.")
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertThat(exception?.message).isEqualTo("The NavDeepLink cannot have an empty action.")
    }

    @Test
    fun buildDeepLinkDoubleActionSetNullKClass() {
        val expectedUri = "www.example.com"
        val navDeepLink =
            navDeepLink<TestClass>(expectedUri) {
                action = "blah"
                action = null
            }

        assertWithMessage("NavDeepLink should have uri pattern set")
            .that(navDeepLink.uriPattern)
            .isEqualTo(expectedUri)
        assertWithMessage("NavDeepLink should have action set").that(navDeepLink.action).isNull()
    }
}
