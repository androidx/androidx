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
import android.net.Uri

import androidx.navigation.test.R
import androidx.navigation.test.TestEnum
import androidx.navigation.testing.TestNavigatorProvider
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

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
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertNotNull(graph)
        assertEquals(R.id.start_test, graph.startDestination)
    }

    @Test
    fun testInflateDeepLinkWithApplicationId() {
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertNotNull(graph)
        val expectedUri = Uri.parse("android-app://" +
                instrumentation.targetContext.packageName + "/test")
        val result = graph.matchDeepLink(expectedUri)
        assertNotNull(result)
        assertNotNull(result?.first)
        assertEquals(R.id.second_test, result?.first?.id ?: 0)
    }

    @Test
    fun testDefaultArgumentsInteger() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(12, defaultArguments.get("test_int")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_int")?.type)
        assertEquals(2, defaultArguments.get("test_int2")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_int2")?.type)
    }

    @Test
    fun testDefaultArgumentsDimen() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        val context = InstrumentationRegistry.getTargetContext()
        val expectedValue = context.resources.getDimensionPixelSize(R.dimen.test_dimen_arg)

        assertEquals(expectedValue, defaultArguments.get("test_dimen")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_dimen")?.type)
        assertEquals(expectedValue, defaultArguments.get("test_dimen2")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_dimen2")?.type)
        assertEquals(R.dimen.test_dimen_arg, defaultArguments.get("test_dimen3")?.defaultValue)
        assertEquals(NavType.ReferenceType, defaultArguments.get("test_dimen3")?.type)
    }

    @Test
    fun testDefaultArgumentsColor() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        val context = InstrumentationRegistry.getTargetContext()
        val expectedValue = context.resources.getColor(R.color.test_color_arg)

        assertEquals(expectedValue, defaultArguments.get("test_color")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_color")?.type)
    }

    @Test
    fun testDefaultArgumentsFloat() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(3.14f, defaultArguments.get("test_float")?.defaultValue)
        assertEquals(NavType.FloatType, defaultArguments.get("test_float")?.type)
    }

    @Test
    fun testDefaultArgumentsBoolean() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(true, defaultArguments.get("test_boolean")?.defaultValue)
        assertEquals(false, defaultArguments.get("test_boolean2")?.defaultValue)
        assertEquals(true, defaultArguments.get("test_boolean3")?.defaultValue)
        assertEquals(false, defaultArguments.get("test_boolean4")?.defaultValue)
        assertEquals(true, defaultArguments.get("test_boolean5")?.defaultValue)
        assertEquals(NavType.BoolType, defaultArguments.get("test_boolean")?.type)
        assertEquals(NavType.BoolType, defaultArguments.get("test_boolean2")?.type)
        assertEquals(NavType.BoolType, defaultArguments.get("test_boolean3")?.type)
        assertEquals(NavType.BoolType, defaultArguments.get("test_boolean4")?.type)
        assertEquals(NavType.BoolType, defaultArguments.get("test_boolean5")?.type)
    }

    @Test
    fun testDefaultArgumentsLong() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(456789013456L, defaultArguments.get("test_long")?.defaultValue)
        assertEquals(456789013456L, defaultArguments.get("test_long2")?.defaultValue)
        assertEquals(123L, defaultArguments.get("test_long3")?.defaultValue)
        assertEquals(NavType.LongType, defaultArguments.get("test_long")?.type)
        assertEquals(NavType.LongType, defaultArguments.get("test_long2")?.type)
        assertEquals(NavType.LongType, defaultArguments.get("test_long3")?.type)
    }

    @Test
    fun testDefaultArgumentsEnum() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(TestEnum.VALUE_ONE, defaultArguments.get("test_enum")?.defaultValue)
        assertEquals(NavType.EnumType(TestEnum::class.java),
            defaultArguments.get("test_enum")?.type)
    }

    @Test
    fun testDefaultArgumentsString() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals("abc", defaultArguments.get("test_string")?.defaultValue)
        assertEquals("true", defaultArguments.get("test_string2")?.defaultValue)
        assertEquals("123L", defaultArguments.get("test_string3")?.defaultValue)
        assertEquals("123", defaultArguments.get("test_string4")?.defaultValue)
        assertEquals("test string", defaultArguments.get("test_string5")?.defaultValue)
        assertEquals("test string", defaultArguments.get("test_string6")?.defaultValue)
        assertTrue(defaultArguments.containsKey("test_string_no_default"))
        assertEquals(false, defaultArguments.get("test_string_no_default")?.isDefaultValuePresent)

        assertEquals(NavType.StringType, defaultArguments.get("test_string")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string2")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string3")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string4")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string5")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string6")?.type)
        assertEquals(NavType.StringType, defaultArguments.get("test_string_no_default")?.type)
    }

    @Test
    fun testDefaultArgumentsReference() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(R.style.AppTheme, defaultArguments.get("test_reference")?.defaultValue)
        assertEquals(NavType.IntType, defaultArguments.get("test_reference")?.type)
        assertEquals(R.dimen.test_dimen_arg, defaultArguments.get("test_reference2")?.defaultValue)
        assertEquals(NavType.ReferenceType, defaultArguments.get("test_reference2")?.type)
        assertEquals(R.integer.test_integer_arg,
            defaultArguments.get("test_reference3")?.defaultValue)
        assertEquals(NavType.ReferenceType, defaultArguments.get("test_reference3")?.type)
        assertEquals(R.string.test_string_arg,
            defaultArguments.get("test_reference4")?.defaultValue)
        assertEquals(NavType.ReferenceType, defaultArguments.get("test_reference4")?.type)
        assertEquals(R.bool.test_bool_arg, defaultArguments.get("test_reference5")?.defaultValue)
        assertEquals(NavType.ReferenceType, defaultArguments.get("test_reference5")?.type)
    }

    @Test
    fun testRelativeClassName() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        assertEquals(TestEnum.VALUE_TWO,
            defaultArguments.get("test_relative_classname")?.defaultValue)
    }

    @Test
    fun testActionArguments() {
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_default_arguments)
        val startDestination = graph.findNode(graph.startDestination)
        val action = startDestination?.getAction(R.id.my_action)
        assertEquals(123L, action?.defaultArguments?.get("test_action_arg"))
    }

    private fun inflateDefaultArgumentsFromGraph(): Map<String, NavArgument> {
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider())
        val graph = navInflater.inflate(R.navigation.nav_default_arguments)

        val startDestination = graph.findNode(graph.startDestination)
        val defaultArguments = startDestination?.arguments

        assertNotNull(defaultArguments)
        return defaultArguments!!
    }
}
