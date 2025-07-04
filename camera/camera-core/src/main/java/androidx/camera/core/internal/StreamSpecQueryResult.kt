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

package androidx.camera.core.internal

import androidx.annotation.RestrictTo
import androidx.camera.core.UseCase
import androidx.camera.core.impl.StreamSpec

/**
 * Result of querying stream specifications for different use cases.
 *
 * @param streamSpecs A map of [UseCase] to its corresponding [StreamSpec].
 * @param maxSupportedFrameRate The maximum supported frame rate during the stream spec query.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class StreamSpecQueryResult(
    val streamSpecs: Map<UseCase, StreamSpec> = emptyMap(),
    val maxSupportedFrameRate: Int = Int.MAX_VALUE,
)
