/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color

internal actual class CurvedTextDelegate {
    actual var textWidth = 0f
    actual var textHeight = 0f
    actual var baseLinePosition = 0f

    actual fun updateIfNeeded(
        text: String,
        clockwise: Boolean,
        fontSizePx: Float,
        arcPaddingPx: ArcPaddingPx
    ) {
        // TODO(b/194653251): Implement
        throw java.lang.RuntimeException("Not implemented")
    }

    actual fun doDraw(canvas: Canvas, size: Size, color: Color, background: Color) {
        // TODO(b/194653251): Implement
        throw java.lang.RuntimeException("Not implemented")
    }
}