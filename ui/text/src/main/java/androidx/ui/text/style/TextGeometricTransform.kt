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

import androidx.ui.lerp

/**
 * Apply a geometric transformation on text.
 */
data class TextGeometricTransform(
    /**
     * Scale text in horizontal direction.
     */
    val scaleX: Float? = null,
    /**
     * Shear the text in horizontal direction. A pixel at (x, y), where y is the distance above
     * baseline, will be transformed to (x + y * skewX, y). Notice that paragraph won't consider
     * the extra room caused by skewX, And text may be overlapped if skewX is too big.
     */
    val skewX: Float? = null
) {
    companion object {
        internal val None = TextGeometricTransform(1.0f, 0.0f)
    }
}

fun lerp(
    a: TextGeometricTransform,
    b: TextGeometricTransform,
    t: Float
): TextGeometricTransform {
    val scaleX = if (a.scaleX == null && b.scaleX == null) {
        null
    } else {
        lerp(a.scaleX ?: 1.0f, b.scaleX ?: 1.0f, t)
    }
    val skewX = if (a.skewX == null && b.skewX == null) {
        null
    } else {
        lerp(a.skewX ?: 0.0f, b.skewX ?: 0.0f, t)
    }
    return TextGeometricTransform(scaleX, skewX)
}