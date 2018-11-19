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
import android.os.Bundle

import androidx.navigation.test.R
import androidx.navigation.test.TestEnum
import androidx.navigation.testing.TestNavigatorProvider
import androidx.test.InstrumentationRegistry
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

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
        val navInflater = NavInflater(context, TestNavigatorProvider(context))
        val graph = navInflater.inflate(R.navigation.nav_simple)

        assertNotNull(graph)
        assertEquals(R.id.start_test, graph.startDestination)
    }

    @Test
    fun testInflateDeepLinkWithApplicationId() {
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider(context))
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

        assertEquals(12, defaultArguments.getInt("test_int"))
    }

    @Test
    fun testDefaultArgumentsDimen() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        val context = InstrumentationRegistry.getTargetContext()
        val expectedValue = context.resources.getDimensionPixelSize(R.dimen.test_dimen_arg)

        assertEquals(expectedValue, defaultArguments.getInt("test_dimen"))
    }

    @Test
    fun testDefaultArgumentsFloat() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(3.14f, defaultArguments.getFloat("test_float"))
    }

    @Test
    fun testDefaultArgumentsBoolean() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(true, defaultArguments.getBoolean("test_boolean"))
        assertEquals(false, defaultArguments.getBoolean("test_boolean2"))
        assertEquals(true, defaultArguments.getBoolean("test_boolean3"))
        assertEquals(false, defaultArguments.getBoolean("test_boolean4"))
    }

    @Test
    fun testDefaultArgumentsLong() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(456789013456L, defaultArguments.getLong("test_long"))
        assertEquals(456789013456L, defaultArguments.getLong("test_long2"))
        assertEquals(123L, defaultArguments.getLong("test_long3"))
    }

    @Test
    fun testDefaultArgumentsEnum() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(TestEnum.VALUE_ONE, defaultArguments.getSerializable("test_enum") as TestEnum)
        assertNull(defaultArguments.getSerializable("test_enum2"))
    }

    @Test
    fun testDefaultArgumentsString() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals("abc", defaultArguments.getString("test_string"))
        assertEquals("true", defaultArguments.getString("test_string2"))
        assertEquals("123L", defaultArguments.getString("test_string3"))
        assertEquals("123", defaultArguments.getString("test_string4"))
        assertFalse(defaultArguments.containsKey("test_string_no_default"))
    }

    @Test
    fun testDefaultArgumentsReference() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()

        assertEquals(R.style.AppTheme, defaultArguments.getInt("test_reference"))
    }

    @Test
    fun testRelativeClassName() {
        val defaultArguments = inflateDefaultArgumentsFromGraph()
        assertEquals(TestEnum.VALUE_TWO,
            defaultArguments.getSerializable("test_relative_classname"))
    }

    private fun inflateDefaultArgumentsFromGraph(): Bundle {
        val context = InstrumentationRegistry.getTargetContext()
        val navInflater = NavInflater(context, TestNavigatorProvider(context))
        val graph = navInflater.inflate(R.navigation.nav_default_arguments)

        val startDestination = graph.findNode(graph.startDestination)
        val defaultArguments = startDestination?.defaultArguments

        assertNotNull(defaultArguments)
        return defaultArguments!!
    }
}
