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

package androidx.xr.compose

import kotlin.jvm.JvmField

/**
 * Configuration flags to enable or disable specific features or refactorings within the spatial
 * compose module. These are typically enabled by default but can be turned off by module consumers
 * to mitigate potential regressions.
 *
 * These flags are temporary and subject to removal in future releases. Consumers who disable a flag
 * to fix a regression are advised to report the issue promptly.
 *
 * **Usage:**
 *
 * To disable a feature, modify the flag value early in the application lifecycle, such as in the
 * `Application.onCreate` method. Altering flag values after Compose has initialized might lead to
 * unpredictable behavior.
 *
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         SpatialComposeFlags.isSomeFeatureEnabled = false
 *         super.onCreate()
 *     }
 * }
 * ```
 */
@ExperimentalSpatialComposeApi
public object SpatialComposeFlags {

    /**
     * Determines whether the main panel returns to its default 2D window configuration when
     * switching back from 3D to 2D content after the final Subspace is removed. If set to false,
     * the main panel may remain hidden upon disposal of the last Subspace.
     */
    // TODO: b/554100206
    @field:Suppress("MutableBareField")
    @JvmField
    public var isMainPanelResetOnSubspaceDisposeEnabled: Boolean = true
}
