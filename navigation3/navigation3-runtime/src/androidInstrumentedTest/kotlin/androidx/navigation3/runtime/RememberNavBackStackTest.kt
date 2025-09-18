/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class RememberNavBackStackTest {

    @get:Rule val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun simpleObjectRestore() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack<NavKey>() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        rule.runOnUiThread {
            backStack?.add(TestObject)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(backStack).contains(TestObject)
    }

    @Test
    fun simpleClassRestore() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack<NavKey>() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        val testClass = TestClass()

        rule.runOnUiThread {
            backStack?.add(testClass)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(backStack?.get(0)).isInstanceOf<TestClass>()
    }

    @Test
    fun simpleDataClassRestore() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack<NavKey>() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        val testDataClass = TestDataClass(1)

        rule.runOnUiThread {
            backStack?.add(testDataClass)
            testDataClass.value = 2
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        val restoredDataClass = backStack?.get(0)
        assertThat(restoredDataClass).isInstanceOf<TestDataClass>()

        assertThat((restoredDataClass as TestDataClass).value).isEqualTo(2)
    }

    @Test
    fun noSerializerFail() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack<NavKey>() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        rule.runOnUiThread {
            backStack?.add(TestNoSerializer)
            // we null it to ensure recomposition happened
            backStack = null
        }
        try {
            restorationTester.emulateSavedInstanceStateRestore()
        } catch (e: SerializationException) {
            assertThat(e)
                .hasMessageThat()
                .contains("Serializer for class 'TestNoSerializer' is not found")
        }
    }
}

internal object TestNoSerializer : NavKey

@Serializable internal object TestObject : NavKey

@Serializable internal class TestClass : NavKey

@Serializable internal data class TestDataClass(var value: Int) : NavKey
