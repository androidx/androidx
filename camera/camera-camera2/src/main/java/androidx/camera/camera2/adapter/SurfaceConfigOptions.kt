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

package androidx.camera.camera2.adapter

import androidx.camera.core.impl.DeferrableSurface

/** Consolidated custom configuration options associated with a [DeferrableSurface]. */
public data class SurfaceConfigOptions(
    val streamUseCase: Long? = null,
    val streamUseHint: Long? = null,
    val surfaceGroupId: Int? = null,
    val timestampBase: Int? = null,
    val dynamicRangeProfile: Long? = null,
    val mirrorMode: Int? = null,
    val sensorPixelModes: Set<Int>? = null,
)
