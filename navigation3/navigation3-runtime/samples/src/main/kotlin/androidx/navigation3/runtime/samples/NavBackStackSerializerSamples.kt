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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Details
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Home
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Screen
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object NavBackStackSerializerSamples {

    @Serializable open class Screen : NavKey

    @Serializable open class Home(val id: String) : Screen()

    @Serializable open class Details(val itemId: Long) : Screen()
}

@Composable
@Sampled
fun NavBackStackSerializer_withReflection() {
    // On Android, the no-argument overload uses reflection and requires no configuration.
    // This will throw a runtime exception on non-Android platforms during serialization.
    val serializer = NavBackStackSerializer<Screen>()

    val backStack = NavBackStack(Home("abc"), Details(42))
    val encoded = encodeToSavedState(serializer, backStack)
    val decoded = decodeFromSavedState(serializer, encoded)
}

@Composable
@Sampled
fun NavBackStackSerializer_withSerializersModule() {
    val module = SerializersModule {
        polymorphic(Screen::class) {
            subclass(Home.serializer())
            subclass(Details.serializer())
        }
    }
    val configuration = SavedStateConfiguration { serializersModule = module }

    val serializer = NavBackStackSerializer<Screen>()

    // Pass the same configuration (or at least its serializersModule) to encode/decode:
    val backStack = NavBackStack(Home("abc"), Details(42))
    val encoded = encodeToSavedState(serializer, backStack, configuration)
    val decoded = decodeFromSavedState(serializer, encoded, configuration)
}
