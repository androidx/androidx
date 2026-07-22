/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime.deeplink

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DeepLinkSerializerTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun serialize() {
        val serializer = TestPersonSerializer
        val person = TestPerson("Alice", 30)

        val serialized = serializer.serialize(person)

        assertThat(serialized).isEqualTo("Alice--30")
    }

    @Test
    fun deserialize() {
        val serializer = TestPersonSerializer

        val deserialized = serializer.deserialize("Alice--30")

        assertThat(deserialized).isInstanceOf<TestPerson>()
        assertThat(deserialized.name).isEqualTo("Alice")
        assertThat(deserialized.age).isEqualTo(30)
    }

    @Test
    fun rememberSerializable_restoreValue() {
        var person: TestPerson? = null
        restorationTester.setContent {
            person =
                rememberSerializable(serializer = TestPersonSerializer) { TestPerson("Alice", 30) }
        }

        assertThat(person).isInstanceOf<TestPerson>()
        assertThat(person?.name).isEqualTo("Alice")
        assertThat(person?.age).isEqualTo(30)

        rule.runOnUiThread {
            // Null it out to ensure recomposition / restoration happens from saved state
            person = null
        }

        assertThat(person).isNull()

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(person).isInstanceOf<TestPerson>()
        assertThat(person?.name).isEqualTo("Alice")
        assertThat(person?.age).isEqualTo(30)
    }

    @Test
    fun rememberSerializable_restoreMutableState() {
        var personState: MutableState<TestPerson>? = null
        restorationTester.setContent {
            personState =
                rememberSerializable(stateSerializer = TestPersonSerializer) {
                    mutableStateOf(TestPerson("Alice", 30))
                }
        }

        assertThat(personState?.value).isInstanceOf<TestPerson>()
        assertThat(personState?.value?.name).isEqualTo("Alice")
        assertThat(personState?.value?.age).isEqualTo(30)

        rule.runOnUiThread { personState?.value = TestPerson("Bob", 40) }

        assertThat(personState?.value).isInstanceOf<TestPerson>()
        assertThat(personState?.value?.name).isEqualTo("Bob")
        assertThat(personState?.value?.age).isEqualTo(40)

        rule.runOnUiThread {
            // Null it out to ensure recomposition / restoration happens from saved state
            personState = null
        }

        assertThat(personState).isNull()

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(personState?.value).isInstanceOf<TestPerson>()
        assertThat(personState?.value?.name).isEqualTo("Bob")
        assertThat(personState?.value?.age).isEqualTo(40)
    }

    private data class TestPerson(val name: String, val age: Int)

    private object TestPersonSerializer : DeepLinkSerializer<TestPerson>() {
        override val serialName: String = "androidx.navigation3.runtime.deeplink.TestPerson"

        override fun serialize(value: TestPerson): String = "${value.name}--${value.age}"

        override fun deserialize(value: String): TestPerson {
            val parts = value.split("--")
            return TestPerson(parts[0], parts[1].toInt())
        }
    }
}
