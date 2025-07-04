/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.lifecycle.serialization

import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateHandleDelegateTest {

    @get:Rule val activityTestRuleScenario = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun simpleRestore() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var value by viewModel.savedStateHandle.saved { 1 }
            assertThat(value).isEqualTo(1)
            value = 2
            assertThat(value).isEqualTo(2)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var value: Int by
                viewModel.savedStateHandle.saved { error("Unexpected initializer call") }
            assertThat(value).isEqualTo(2)
        }
    }

    @Test
    fun explicitKey() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var value by viewModel.savedStateHandle.saved(key = "foo") { 1 }
            assertThat(value).isEqualTo(1)
            value = 2
            assertThat(value).isEqualTo(2)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var value: Int by
                viewModel.savedStateHandle.saved(key = "foo") {
                    error("Unexpected initializer call")
                }
            assertThat(value).isEqualTo(2)
        }
    }

    @Test
    fun explicitSerializer() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value by viewModel.savedStateHandle.saved(serializer = Int.serializer()) { 1 }
            assertThat(value).isEqualTo(1)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value: Int by
                viewModel.savedStateHandle.saved(serializer = Int.serializer()) {
                    error("Unexpected initializer call")
                }
            assertThat(value).isEqualTo(1)
        }
    }

    @Test
    fun explicitConfig() {
        data class User(val name: String)
        class UserSerializer : KSerializer<User> {
            override val descriptor: SerialDescriptor =
                PrimitiveSerialDescriptor("User", PrimitiveKind.STRING)

            override fun serialize(encoder: Encoder, value: User) {
                encoder.encodeString(value.name)
            }

            override fun deserialize(decoder: Decoder): User {
                return User(decoder.decodeString())
            }
        }

        val config = SavedStateConfiguration {
            serializersModule = SerializersModule { contextual(User::class, UserSerializer()) }
        }

        val user = User("foo")
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value by viewModel.savedStateHandle.saved(configuration = config) { user }
            assertThat(value).isEqualTo(user)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value: User by
                viewModel.savedStateHandle.saved(configuration = config) {
                    error("Unexpected initializer call")
                }
            assertThat(value).isEqualTo(user)
        }
    }

    @Test
    fun explicitKeyAndSerializer() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value by
                viewModel.savedStateHandle.saved(key = "foo", serializer = Int.serializer()) { 1 }
            assertThat(value).isEqualTo(1)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val value: Int by
                viewModel.savedStateHandle.saved(key = "foo", serializer = Int.serializer()) {
                    error("Unexpected initializer call")
                }
            assertThat(value).isEqualTo(1)
        }
    }

    @Test
    fun duplicateKeys() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val serializable by viewModel.savedStateHandle.saved(key = "foo") { 1 }
            assertThat(serializable).isEqualTo(1)
            val duplicate by viewModel.savedStateHandle.saved(key = "foo") { 2 }
            assertThat(duplicate).isEqualTo(2)
            // The value is from the initializer.
            assertThat(serializable).isEqualTo(1)
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            val serializable: Int by
                viewModel.savedStateHandle.saved(key = "foo") {
                    error("Unexpected initializer call")
                }
            assertThat(serializable).isEqualTo(2)
            val duplicate: Int by
                viewModel.savedStateHandle.saved(key = "foo") {
                    error("Unexpected initializer call")
                }
            assertThat(duplicate).isEqualTo(2)
            assertThat(serializable).isEqualTo(2)
        }
    }

    @Test
    fun emptyValueStaysEmpty() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var serializable: List<Double> by
                viewModel.savedStateHandle.saved { listOf(1.0, 2.0, 3.0) }
            assertThat(serializable).isEqualTo(listOf(1.0, 2.0, 3.0))
            serializable = emptyList()
            assertThat(serializable).isEqualTo(emptyList<Double>())
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var serializable: List<Double> by
                viewModel.savedStateHandle.saved { error("Unexpected initializer call") }
            assertThat(serializable).isEqualTo(emptyList<Double>())
        }
    }

    @Test
    fun setBeforeGetShouldNotCallInit() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var serializable: Int by
                viewModel.savedStateHandle.saved(serializer = noDeserializeSerializer()) {
                    error("Unexpected initializer call")
                }
            serializable = 2
            assertThat(serializable).isEqualTo(2)
        }
    }

    @Test
    fun setBeforeGetShouldNotLoadFromSavedState() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var serializable: Int by
                viewModel.savedStateHandle.saved(serializer = noDeserializeSerializer()) {
                    error("Unexpected initializer call")
                }
            serializable = 2
        }
        activityTestRuleScenario.scenario.recreate()
        activityTestRuleScenario.scenario.onActivity { activity ->
            val viewModel: MyViewModel = ViewModelProvider(activity)[MyViewModel::class]
            var serializable: Int by
                viewModel.savedStateHandle.saved(serializer = noDeserializeSerializer()) {
                    error("Unexpected initializer call")
                }
            serializable = 3
            assertThat(serializable).isEqualTo(3)
        }
    }
}

class MyViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()

private inline fun <reified T> noDeserializeSerializer(): KSerializer<T> =
    object : KSerializer<T> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NoDeserialize")

        override fun serialize(encoder: Encoder, value: T) {
            serializer<T>().serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): T {
            error("Unexpected deserialize call")
        }
    }
