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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Details
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Home
import androidx.navigation3.runtime.samples.RememberNavBackStackSamples.Screen
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

object RememberNavBackStackSamples {

    @Serializable open class Screen : NavKey

    @Serializable open class Home(val id: String) : Screen()

    @Serializable open class Details(val itemId: Long) : Screen()
}

@Composable
@Sampled
fun rememberNavBackStack_withReflection() {
    // Works on Android (uses reflection internally).
    rememberNavBackStack(Home("start"), Details(42))
}

@Composable
@Sampled
fun rememberNavBackStack_withSerializersModule() {
    val config = SavedStateConfiguration {
        // Register subtypes for open polymorphism or multiplatform use.
        serializersModule = SerializersModule {
            polymorphic(baseClass = Screen::class) {
                subclass(serializer = Home.serializer())
                subclass(serializer = Details.serializer())
            }
        }
    }

    // Pass the configuration so encoding/decoding works consistently.
    rememberNavBackStack(Home("start"), configuration = config)
}
