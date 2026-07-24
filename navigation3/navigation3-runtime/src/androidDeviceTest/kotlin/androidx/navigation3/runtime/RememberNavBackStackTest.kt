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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.navigation3.runtime.deeplink.DeepLinkSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
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
        restorationTester.setContent { backStack = rememberNavBackStack() }

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
        restorationTester.setContent { backStack = rememberNavBackStack() }

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
    fun simpleClassRestore_deepLinkSerializer() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        val key = TestNonPrimitiveArgClass(TestArg(12, 2.1))

        rule.runOnUiThread {
            backStack?.add(key)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()
        val expected = backStack?.get(0) as? TestNonPrimitiveArgClass
        assertThat(expected).isNotNull()
        assertThat(expected!!.arg.age).isEqualTo(12)
        assertThat(expected.arg.height).isEqualTo(2.1)
    }

    @Test
    fun simpleDataClassRestore() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

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
    fun simpleDataClassRestore_deepLinkSerializer() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        val key = TestNonPrimitiveArgDataClass(TestArg(12, 2.1))

        rule.runOnUiThread {
            backStack?.add(key)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()
        val expected = backStack?.get(0) as? TestNonPrimitiveArgDataClass
        assertThat(expected).isNotNull()
        assertThat(expected!!.arg.age).isEqualTo(12)
        assertThat(expected.arg.height).isEqualTo(2.1)
    }

    @Test
    fun noSerializerFail() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        rule.runOnUiThread {
            backStack?.add(TestNoSerializer)
            // we null it to ensure recomposition happened
            backStack = null
        }

        assertThrows<SerializationException> {
                restorationTester.emulateSavedInstanceStateRestore()
            }
            .hasMessageThat()
            .contains("Serializer for class 'TestNoSerializer' is not found")
    }

    @Test
    fun sealedClassRestore() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        rule.runOnUiThread {
            backStack?.add(TestSealedClass.Key1)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(backStack).contains(TestSealedClass.Key1)
    }

    @Test
    fun configurationRestore() {
        var backStack: NavBackStack<NavKey>? = null
        // Explicitly pass the custom Configuration object
        restorationTester.setContent {
            backStack = rememberNavBackStack(configuration = Configuration)
        }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        rule.runOnUiThread {
            backStack?.add(TestObject)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        // We assert that an object defined in that configuration is restored
        assertThat(backStack).contains(TestObject)
    }

    @Test
    fun defaultConfigurationFails() {
        assertThrows<IllegalArgumentException> {
                rule.setContent {
                    rememberNavBackStack(configuration = SavedStateConfiguration.DEFAULT)
                }
            }
            .hasMessageThat()
            .contains(
                "You must pass a `SavedStateConfiguration.serializersModule` configured to " +
                    "handle `NavKey` open polymorphism."
            )
    }

    @Test
    fun polymorphicClassRestore_deepLinkSerializer() {
        var backStack: NavBackStack<NavKey>? = null
        restorationTester.setContent { backStack = rememberNavBackStack() }

        assertThat(backStack).isEqualTo(NavBackStack<NavKey>())

        val key = TestNonPrimitiveArgAbstractImpl(TestArg(12, 2.1))

        rule.runOnUiThread {
            backStack?.add(key)
            // we null it to ensure recomposition happened
            backStack = null
        }

        restorationTester.emulateSavedInstanceStateRestore()
        val expected = backStack?.get(0) as? TestNonPrimitiveArgAbstractImpl
        assertThat(expected).isNotNull()
        assertThat(expected!!.arg.age).isEqualTo(12)
        assertThat(expected.arg.height).isEqualTo(2.1)
    }
}

private object TestNoSerializer : NavKey

@Serializable private object TestObject : NavKey

@Serializable private class TestClass : NavKey

@Serializable private data class TestDataClass(var value: Int) : NavKey

@Serializable
private sealed class TestSealedClass : NavKey {
    @Serializable data object Key1 : TestSealedClass()
}

private val Configuration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(TestObject::class)
            subclass(TestClass::class)
            subclass(TestDataClass::class)
            subclass(TestSealedClass::class)
            subclass(TestSealedClass.Key1::class)
        }
    }
}

private class TestArg(val age: Int, val height: Double)

@Serializable
private class TestNonPrimitiveArgClass(
    val arg: @Serializable(with = TestArgSerializer::class) TestArg
) : NavKey

@Serializable
private data class TestNonPrimitiveArgDataClass(
    val arg: @Serializable(with = TestArgSerializer::class) TestArg
) : NavKey

private abstract class TestAbstractClass : NavKey

@Serializable
private data class TestNonPrimitiveArgAbstractImpl(
    val arg: @Serializable(with = TestArgSerializer::class) TestArg
) : TestAbstractClass()

private class TestArgSerializer : DeepLinkSerializer<TestArg>() {
    override val serialName = "androidx.navigation3.runtime.TestArg"

    override fun deserialize(value: String): TestArg {
        val delimited = value.split("--")
        return TestArg(delimited[0].toInt(), delimited[1].toDouble())
    }

    override fun serialize(value: TestArg): String {
        return "${value.age}--${value.height}"
    }
}
