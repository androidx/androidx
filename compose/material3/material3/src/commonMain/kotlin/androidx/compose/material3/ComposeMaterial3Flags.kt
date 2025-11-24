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
package androidx.compose.material3

import kotlin.jvm.JvmField

/**
 * The object holding the **feature flags**.
 *
 * Developers can enable or disable experimental or migration-based features by modifying the
 * boolean values within this object. These flags are considered temporary. They are expected to be
 * removed and should not be relied upon as permanent configuration.
 *
 * **Usage:**
 *
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         ComposeMaterial3Flags.someFeatureEnabled = true
 *         super.onCreate()
 *     }
 * }
 * ```
 */
@ExperimentalMaterial3Api
object ComposeMaterial3Flags {
    /**
     * When this flag is true and a precision pointer is present, components are resized accordingly
     */
    @field:Suppress("MutableBareField")
    @JvmField
    var isPrecisionPointerComponentSizingEnabled: Boolean = false
}
