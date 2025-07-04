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

@file:JvmName("LocalSavedStateRegistryOwnerKt")

package androidx.savedstate.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.savedstate.SavedStateRegistryOwner

/**
 * The CompositionLocal containing the current [LocalSavedStateRegistryOwner].
 *
 * **Important:** For backward compatibility with Compose <= 1.7, we will use reflection to access
 * the [SavedStateRegistryOwner] from the older `androidx.compose.ui.platform` package on Android
 * targets. This will be cached for efficiency and has a included custom Proguard rule to prevent
 * obfuscation issues.
 *
 * When using Compose >= 1.8, the [SavedStateRegistryOwner] is directly accessed from the new
 * package.
 *
 * Please note that backward compatibility reflection may be removed once Compose >= 1.8 is stable.
 * A Gradle dependency constraint will be put in place to ensure smooth migration for clients.
 */
public actual val LocalSavedStateRegistryOwner:
    ProvidableCompositionLocal<SavedStateRegistryOwner> =
    run {
        val compositionLocalFromComposeUi = runCatching {
            // Use the CompositionLocal class to find the `classLoader` from the `Application`.
            val classLoader = SavedStateRegistryOwner::class.java.classLoader!!
            // Top-level class name from Compose UI 1.6.* that holds the old CompositionLocal.
            val className = "androidx.compose.ui.platform.AndroidCompositionLocals_androidKt"
            // The Java getter used when accessing the CompositionLocal property in Kotlin.
            val methodName = "getLocalSavedStateRegistryOwner"

            val methodRef = classLoader.loadClass(className).getMethod(methodName)
            if (methodRef.annotations.any { it is Deprecated }) {
                // If the method IS deprecated, we are running with Compose 1.7.*.
                // The new CompositionLocal is available, no reflection needed.
                null
            } else {
                // If the method IS NOT deprecated, we are running with Compose 1.6.*.
                // We use reflection to access the older CompositionLocal from `compose-ui`.
                @Suppress("UNCHECKED_CAST", "BanUncheckedReflection")
                methodRef.invoke(null) as? ProvidableCompositionLocal<SavedStateRegistryOwner>
            }
        }

        return@run compositionLocalFromComposeUi.getOrNull()
            ?: staticCompositionLocalOf<SavedStateRegistryOwner> {
                error("CompositionLocal LocalSavedStateRegistryOwner not present")
            }
    }
