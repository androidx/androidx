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

import android.graphics.RectF
import androidx.pdf.annotation.models.PathPdfObject.PathInput
import kotlin.math.max
import kotlin.math.min

fun List<PathInput>.computeBounds(): RectF {
    val left = this.fold(Float.Companion.MAX_VALUE) { acc, input -> min(acc, input.x) }
    val top = this.fold(Float.Companion.MAX_VALUE) { acc, input -> min(acc, input.y) }
    val right = this.fold(Float.Companion.MIN_VALUE) { acc, input -> max(acc, input.x) }
    val bottom = this.fold(Float.Companion.MIN_VALUE) { acc, input -> max(acc, input.y) }
    return RectF(left, top, right, bottom)
}

fun RectF.merge(other: RectF): RectF =
    RectF(
        /* left = */ min(this.left, other.left),
        /* top = */ min(this.top, other.top),
        /* right = */ max(this.right, other.right),
        /* bottom = */ max(this.bottom, other.bottom),
    )
