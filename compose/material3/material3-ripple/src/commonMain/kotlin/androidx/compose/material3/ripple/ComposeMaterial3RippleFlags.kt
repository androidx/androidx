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

package androidx.compose.material3.ripple

import kotlin.jvm.JvmField

/**
 * The object holding the **feature flags** for Material 3 ripple.
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
 *         ComposeMaterial3RippleFlags.someFeatureEnabled = true
 *         super.onCreate()
 *     }
 * }
 * ```
 */
@ExperimentalMaterial3RippleApi
public object ComposeMaterial3RippleFlags {
    /**
     * When this flag is `true`, the inset focus ring
     * ([RippleNodeConfiguration.FocusConfiguration.InsetRing]) is drawn using
     * [androidx.compose.foundation.BorderRenderer]. When `false`, it is drawn using a copy of the
     * border drawing logic that predates [androidx.compose.foundation.BorderRenderer].
     *
     * Both implementations draw the same geometry, but can differ slightly in the anti-aliasing of
     * edge pixels. Additionally, the legacy implementation does not apply the inset for simple
     * rounded rectangles whose corner radius, reduced by the inset, is smaller than half of the
     * stroke width.
     */
    // TODO: b/583834505 - remove this flag and the legacy BorderLogic once the BorderRenderer based
    //  focus ring has soaked.
    @field:Suppress("MutableBareField")
    @JvmField
    public var isBorderRendererFocusRingEnabled: Boolean = false
}
