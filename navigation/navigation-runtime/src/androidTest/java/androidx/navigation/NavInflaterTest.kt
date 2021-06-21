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

import android.app.Instrumentation
import android.content.Context
import android.net.Uri
import androidx.navigation.test.R
import androidx.navigation.test.TestEnum
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.TestNavigatorProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavInflaterTest {
    private lateinit var instrumentation: Instrumentation

    @Before
    fun getInstrumentation() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @Test
    fun testInflateSimple() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        assertThat(graph.startDestinationId)
            .isEqualTo(R.id.start_test)
    }

    @Test
    fun testEmptyLabel() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        assertThat(graph.label)
            .isEqualTo("")
    }

    @Test(expected = RuntimeException::class)
    fun testInflateInvalidArgumentArgType() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        navInflater.inflate(R.navigation.nav_invalid_argument_arg_type)
    }

    @Test(expected = RuntimeException::class)
    fun testInflateInvalidArgumentDefaultValue() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        navInflater.inflate(R.navigation.nav_invalid_argument_default_value)
    }

    @Test
    fun testInflateDeepLinkWithApplicationId() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        val expectedUri = Uri.parse(
            "android-app://" +
                instrumentation.targetContext.packageName + "/test"
        )
        val expectedDeepLinkRequest = NavDeepLinkRequest.Builder.fromUri(expectedUri).build()
        val result = graph.matchDeepLink(expectedDeepLinkRequest)
        assertThat(result)
            .isNotNull()
        assertThat(result?.destination)
            .isNotNull()
        assertThat(result?.destination?.id).isEqualTo(R.id.second_test)
    }

    @Test
    fun testInflateDeepLinkWithApplicationIdAction() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        val expectedDeepLinkRequest = NavDeepLinkRequest.Builder.fromAction("test.action").build()
        val result = graph.matchDeepLink(expectedDeepLinkRequest)
        assertThat(result)
            .isNotNull()
        assertThat(result?.destination)
            .isNotNull()
        assertThat(result?.destination?.id).isEqualTo(R.id.second_test)
    }

    @Test
    fun testInflateDeepLinkWithApplicationIdEmptyAction() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        val expectedDeepLinkRequest = NavDeepLinkRequest.Builder
            .fromUri(
                Uri.parse(
                    "android-app://" +
                        instrumentation.targetContext.packageName + "/test/param1/param2"
                )
            ).build()
        val result = graph.matchDeepLink(expectedDeepLinkRequest)
        assertThat(result)
            .isNotNull()
        assertThat(result?.destination)
            .isNotNull()
        assertThat(result?.destination?.id).isEqualTo(R.id.second_test)
    }

    @Test
    fun testInflateDeepLinkWithApplicationIdMimeType() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertThat(graph).isNotNull()
        val expectedDeepLinkRequest = NavDeepLinkRequest.Builder.fromMimeType("type/test").build()
        val result = graph.matchDeepLink(expectedDeepLinkRequest)
        assertThat(result)
            .isNotNull()
        assertThat(result?.destination)
            .isNotNull()
        assertThat(result?.destination?.id).isEqualTo(R.id.second_test)
    }

    @Test
    fun testDefaultArgumentsInteger() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_int"]?.run { type to defaultValue })
            .isEqualTo(NavType.IntType to 12)
    }

    @Test
    fun testDefaultArgumentsFloat() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_float"]?.run { type to defaultValue })
            .isEqualTo(NavType.FloatType to 3.14f)
    }

    @Test
    fun testDefaultIntArgumentsFloat() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_int_as_float"]?.run { type to defaultValue })
            .isEqualTo(NavType.FloatType to 3f)
    }

    @Test
    fun testDefaultArgumentsBoolean() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_boolean"]?.run { type to defaultValue })
            .isEqualTo(NavType.BoolType to true)
        assertThat(defaultArguments["test_boolean_false"]?.run { type to defaultValue })
            .isEqualTo(NavType.BoolType to false)
        assertThat(defaultArguments["test_boolean_with_argType"]?.run { type to defaultValue })
            .isEqualTo(NavType.BoolType to true)
        assertThat(
            defaultArguments["test_boolean_with_argType_false"]?.run {
                type to defaultValue
            }
        ).isEqualTo(NavType.BoolType to false)
    }

    @Test
    fun testDefaultArgumentsLong() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_long"]?.run { type to defaultValue })
            .isEqualTo(NavType.LongType to 456789013456L)
        assertThat(defaultArguments["test_long_with_argType"]?.run { type to defaultValue })
            .isEqualTo(NavType.LongType to 456789013456L)
        assertThat(defaultArguments["test_long_short"]?.run { type to defaultValue })
            .isEqualTo(NavType.LongType to 123L)
    }

    @Test
    fun testDefaultArgumentsEnum() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_enum"]?.run { type to defaultValue })
            .isEqualTo(NavType.EnumType(TestEnum::class.java) to TestEnum.VALUE_ONE)
    }

    @Test
    fun testDefaultArgumentsString() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_string"]?.run { type to defaultValue })
            .isEqualTo(NavType.StringType to "abc")
        assertThat(defaultArguments["test_string_bool"]?.run { type to defaultValue })
            .isEqualTo(NavType.StringType to "true")
        assertThat(defaultArguments["test_string_long"]?.run { type to defaultValue })
            .isEqualTo(NavType.StringType to "123L")
        assertThat(defaultArguments["test_string_integer"]?.run { type to defaultValue })
            .isEqualTo(NavType.StringType to "123")

        assertThat(
            defaultArguments["test_string_no_default"]?.run {
                type to isDefaultValuePresent
            }
        ).isEqualTo(NavType.StringType to false)
    }

    @Test
    fun testDefaultArgumentsStringArray() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_string_array"]?.run { type to defaultValue })
            .isEqualTo(NavType.StringArrayType to null)
    }

    @Test
    fun testDefaultArgumentsReference() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertThat(defaultArguments["test_reference"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.style.AppTheme)
        assertThat(defaultArguments["test_reference_dimen"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.dimen.test_dimen_arg)
        assertThat(defaultArguments["test_reference_integer"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.integer.test_integer_arg)
        assertThat(defaultArguments["test_reference_string"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.string.test_string_arg)
        assertThat(defaultArguments["test_reference_bool"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.bool.test_bool_arg)
        assertThat(defaultArguments["test_reference_color"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to R.color.test_color_arg)
        assertThat(defaultArguments["test_reference_zero_default"]?.run { type to defaultValue })
            .isEqualTo(NavType.ReferenceType to 0)
    }

    @Test
    fun testRelativeClassName() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        assertThat(defaultArguments["test_relative_classname"]?.run { type to defaultValue })
            .isEqualTo(NavType.EnumType(TestEnum::class.java) to TestEnum.VALUE_TWO)
    }

    @Test
    fun testActionArguments() {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_default_arguments)
        val startDestination = graph.findNode(graph.startDestinationId)
        val action = startDestination?.getAction(R.id.my_action)
        assertThat(action?.defaultArguments?.get("test_action_arg"))
            .isEqualTo(123L)
    }

    private fun inflateDefaultArgumentsFromGraph(): Map<String, NavArgument> {
        val context = ApplicationProvider.getApplicationContext() as Context
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_default_arguments)

        val startDestination = graph.findNode(graph.startDestinationId)
        val defaultArguments = startDestination?.arguments

        assertThat(defaultArguments).isNotNull()
        return defaultArguments as Map<String, NavArgument>
    }
}
