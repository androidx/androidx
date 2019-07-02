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

import androidx.ui.core.Px
import androidx.ui.core.px
import androidx.ui.core.lerp

/**
 * Specify how much a paragraph is indented.
 *
 * @param firstLine the amount of indentation applied to the first line.
 * @param restLine the amount of indentation applied to every line except the first line.
 */
data class TextIndent(
    val firstLine: Px = 0.px,
    val restLine: Px = 0.px
) {
    companion object {
        val NONE = TextIndent(0.px, 0.px)
    }
}

fun lerp(a: TextIndent, b: TextIndent, t: Float): TextIndent {
    return TextIndent(
        lerp(a.firstLine, b.firstLine, t),
        lerp(a.restLine, b.restLine, t)
    )
}