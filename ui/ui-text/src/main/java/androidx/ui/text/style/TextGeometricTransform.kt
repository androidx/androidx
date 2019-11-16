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

package androidx.ui.text.style

import androidx.compose.Immutable
import androidx.ui.lerp

/**
 * Define a geometric transformation on text.
 *
 * @param scaleX The scale of the text on the horizontal direction.
 * @param skewX The shear of the text on the horizontal direction. A pixel at (x, y), where y is
 * the distance above baseline, will be transformed to (x + y * skewX, y).
 */
@Immutable
data class TextGeometricTransform(
    val scaleX: Float? = null,
    val skewX: Float? = null
) {
    companion object {
        internal val None = TextGeometricTransform(1.0f, 0.0f)
    }
}

fun lerp(
    start: TextGeometricTransform,
    stop: TextGeometricTransform,
    fraction: Float
): TextGeometricTransform {
    val scaleX = if (start.scaleX == null && stop.scaleX == null) {
        null
    } else {
        lerp(start.scaleX ?: 1.0f, stop.scaleX ?: 1.0f, fraction)
    }
    val skewX = if (start.skewX == null && stop.skewX == null) {
        null
    } else {
        lerp(start.skewX ?: 0.0f, stop.skewX ?: 0.0f, fraction)
    }
    return TextGeometricTransform(scaleX, skewX)
}
