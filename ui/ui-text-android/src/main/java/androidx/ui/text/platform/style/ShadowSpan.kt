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
package androidx.ui.text.platform.style

import android.text.TextPaint
import android.text.style.CharacterStyle
import androidx.annotation.RestrictTo

/**
 * A span which applies a shadow effect to the covered text.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ShadowSpan(
    val color: Int,
    val offsetX: Float,
    val offsetY: Float,
    val radius: Float
) : CharacterStyle() {
    override fun updateDrawState(tp: TextPaint) {
        tp.setShadowLayer(radius, offsetX, offsetY, color)
    }
}