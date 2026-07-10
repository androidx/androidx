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

package androidx.navigation3.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.samples.NavBackStackSamples.Chat
import androidx.navigation3.runtime.samples.NavBackStackSamples.Home
import androidx.navigation3.runtime.samples.NavBackStackSamples.SealedKey
import androidx.navigation3.runtime.samples.NavBackStackSamples.Spaces
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer

object NavBackStackSamples {

    // --- Open Polymorphism ---
    @Serializable open class Home : NavKey

    @Serializable open class Chat : NavKey

    @Serializable open class Spaces : NavKey

    // --- Closed Polymorphism ---
    @Serializable
    sealed class SealedKey : NavKey {

        @Serializable class Inbox : SealedKey()

        @Serializable class Settings : SealedKey()
    }
}

@Composable
@Sampled
fun NavBackStack_OpenPolymorphism() {
    // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism
    rememberSerializable(
        serializer = serializer(),
        configuration =
            SavedStateConfiguration {
                serializersModule = SerializersModule {
                    polymorphic(baseClass = NavKey::class) {
                        subclass(clazz = Home::class)
                        subclass(clazz = Chat::class)
                        subclass(clazz = Spaces::class)
                    }
                }
            },
    ) {
        NavBackStack<NavKey>()
    }
}

@Composable
@Sampled
fun NavBackStack_ClosedPolymorphism() {
    // https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#closed-polymorphism
    rememberSerializable(serializer = serializer()) { NavBackStack<SealedKey>() }
}
