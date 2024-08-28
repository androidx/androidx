/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.strokes.testing

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.StrokeInput
import kotlin.jvm.JvmOverloads

/**
 * Build a StrokeInputBatch from an array of [points] in the form [x1, y1, x2, y2...] and a
 * specified input [toolType] with STYLUS set by default.
 */
@JvmOverloads
@VisibleForTesting
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
public fun buildStrokeInputBatchFromPoints(
    points: FloatArray,
    toolType: InputToolType = InputToolType.STYLUS,
    startTime: Long = 0L,
): MutableStrokeInputBatch {
    val builder = MutableStrokeInputBatch()
    var time = startTime
    for (i in points.indices step 2) {
        builder.addOrThrow(
            StrokeInput().apply {
                update(
                    x = points[i],
                    y = points[i + 1],
                    elapsedTimeMillis = time++,
                    toolType = toolType
                )
            }
        )
    }
    return builder
}
