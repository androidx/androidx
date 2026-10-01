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

package androidx.camera.core.impl

import android.util.Range
import androidx.annotation.RestrictTo

/**
 * Result of querying stream specifications for surfaces.
 *
 * @param useCaseStreamSpecs A map of [UseCaseConfig] to its corresponding [StreamSpec].
 * @param attachedSurfaceStreamSpecs A map of [AttachedSurfaceInfo] to its corresponding
 *   [StreamSpec], representing existing UseCases.
 * @param supportedFrameRateRanges The supported frame rate ranges during the stream spec query, or
 *   `null` if they were not calculated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class SurfaceStreamSpecQueryResult
@JvmOverloads
constructor(
    val useCaseStreamSpecs: Map<UseCaseConfig<*>, StreamSpec>,
    val attachedSurfaceStreamSpecs: Map<AttachedSurfaceInfo, StreamSpec>,
    val supportedFrameRateRanges: Set<Range<Int>>? = null,
)
