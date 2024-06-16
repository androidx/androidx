/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "LocalVariableName")

package androidx.compose.runtime.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf

@Sampled
fun createCompositionLocal() {
    val ActiveUser = compositionLocalOf<User> { error("No active user found!") }
}

@Sampled
fun compositionLocalProvider() {
    @Composable
    fun App(user: User) {
        CompositionLocalProvider(ActiveUser provides user) { SomeScreen() }
    }
}

@Sampled
fun compositionLocalComputedByDefault() {
    val LocalBaseValue = compositionLocalOf { 10 }
    val LocalLargerValue = compositionLocalWithComputedDefaultOf {
        LocalBaseValue.currentValue + 10
    }
}

@Sampled
fun compositionLocalProvidedComputed() {
    val LocalValue = compositionLocalOf { 10 }
    val LocalLargerValue = compositionLocalOf { 12 }

    @Composable
    fun App() {
        CompositionLocalProvider(
            LocalLargerValue providesComputed { LocalValue.currentValue + 10 }
        ) {
            SomeScreen()
        }
    }
}

@Sampled
fun compositionLocalComputedAfterProvidingLocal() {
    val LocalValue = compositionLocalOf { 10 }
    val LocalLargerValue = compositionLocalOf { 12 }
    val LocalComputedValue = compositionLocalWithComputedDefaultOf { LocalValue.currentValue + 4 }

    // In this example `LocalLargerValue` needs to be re-provided
    // whenever `LocalValue` is provided to keep its value larger
    // then `LocalValue`. However, `LocalComputedValue` does not
    // need to be re-provided to stay larger than `LocalValue` as
    // it is calculated based on the currently provided value for
    // `LocalValue`. Whenever `LocalValue` is provided the value
    // of `LocalComputedValue` is computed based on the currently
    // provided value for `LocalValue`.

    @Composable
    fun App() {
        // Value is 10, the default value for LocalValue
        val value = LocalValue.current
        // Value is 12, the default value
        val largerValue = LocalLargerValue.current
        // Value is computed to be 14
        val computedValue = LocalComputedValue.current
        CompositionLocalProvider(LocalValue provides 20) {
            // Value is 20 provided above
            val nestedValue = LocalValue.current
            // Value is still 12 as an updated value was not re-provided
            val nestedLargerValue = LocalLargerValue.current
            // Values is computed to be 24; LocalValue.current + 4
            val nestedComputedValue = LocalComputedValue.current
            CompositionLocalProvider(LocalLargerValue provides LocalValue.current + 2) {
                // Value is 22 provided above
                val newLargerValue = LocalLargerValue.current

                CompositionLocalProvider(LocalValue provides 50) {
                    // Value is now 50 provided above
                    val finalValue = LocalValue.current
                    // Value is still 22
                    val finalLargerValue = LocalLargerValue.current
                    // Value is now computed to be 54
                    val finalComputed = LocalComputedValue.current
                }
            }
        }
    }
}

@Sampled
fun someScreenSample() {
    @Composable
    fun SomeScreen() {
        UserPhoto()
    }
}

@Sampled
fun consumeCompositionLocal() {
    @Composable
    fun UserPhoto() {
        val user = ActiveUser.current
        ProfileIcon(src = user.profilePhotoUrl)
    }
}

@Suppress("CompositionLocalNaming")
private val ActiveUser = compositionLocalOf<User> { error("No active user found!") }

@Composable private fun SomeScreen() {}

@Composable private fun UserPhoto() {}
