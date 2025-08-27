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

import androidx.compose.runtime.mutableStateListOf
import androidx.kruth.assertThat
import androidx.kruth.assertThrows
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class NavBackStackSerializerTest {

    @Test
    fun encodeDecode_withDefaultConfigAndMixedSubtypes_preservesValues() {
        val backStack = mutableStateListOf(Home("root"), Details(42L), Settings(dark = true))

        val serializer =
            NavBackStackSerializer<Screen>(configuration = SavedStateConfiguration.DEFAULT)

        val encoded = encodeToSavedState(serializer, backStack)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(backStack).isEqualTo(decoded)
    }

    @Test
    fun encodeDecode_withCustomModuleAndRegisteredSubtypes_preservesValues() {
        val configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(Screen::class) {
                    subclass(Home.serializer())
                    subclass(Details.serializer())
                }
            }
        }

        val backStack = mutableStateListOf(Home("root"), Details(7L))

        val serializer = NavBackStackSerializer<Screen>(configuration = configuration)
        val encoded = encodeToSavedState(serializer, backStack, configuration)
        val decoded = decodeFromSavedState(serializer, encoded, configuration)

        assertThat(backStack).isEqualTo(decoded)
    }

    @Test
    fun encode_withCustomModuleAndMissingSubtype_throwsSerializationException() {
        val configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(Screen::class) { subclass(Home.serializer()) }
            }
        }

        val backStack =
            mutableStateListOf(
                Home("root"),
                Details(7L), // not registered
            )

        val serializer = NavBackStackSerializer<Screen>(configuration = configuration)

        assertThrows<SerializationException> { encodeToSavedState(serializer, backStack) }
    }

    @Test
    fun encodeDecode_withEmptyBackStack_returnsEmptyList() {
        val backStack = mutableStateListOf<Screen>()
        val serializer =
            NavBackStackSerializer<Screen>(configuration = SavedStateConfiguration.DEFAULT)

        val encoded = encodeToSavedState(serializer, backStack)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(decoded).isEmpty()
    }

    @Test
    fun encodeDecode_withDefaultConfig_preservesOrder() {
        val backStack = mutableStateListOf(Home("a"), Details(1L), Home("b"), Details(2L))

        val serializer =
            NavBackStackSerializer<Screen>(configuration = SavedStateConfiguration.DEFAULT)

        val encoded = encodeToSavedState(serializer, backStack)
        val decoded = decodeFromSavedState(serializer, encoded)

        assertThat(backStack).isEqualTo(decoded)
    }
}

private interface Screen

@Serializable private data class Home(val id: String) : Screen

@Serializable private data class Details(val itemId: Long) : Screen

@Serializable private data class Settings(val dark: Boolean) : Screen
