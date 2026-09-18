/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.text.vertical.testapp

import android.text.SpannableStringBuilder
import android.text.Spanned
import androidx.compose.ui.unit.Density
import androidx.text.vertical.AnnotationPosition
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.compose.VerticalTextScope
import androidx.text.vertical.compose.buildVerticalText

/**
 * Runs [block] and wraps the range it appends with an [EmphasisSpan] placed on the given
 * [position].
 *
 * [VerticalTextScope.withEmphasis] always places the mark on [EmphasisSpan.DEFAULT_POSITION]. This
 * helper builds the span itself, then appends the result. [VerticalTextScope.text] keeps the spans
 * of the appended [Spanned], so nested annotations stay intact.
 *
 * @param density resolves sp values inside [block]. Pass the density of the outer builder.
 * @param style the shape of the emphasis mark.
 * @param filled whether the emphasis mark is filled or outlined.
 * @param position the side of the base text on which the emphasis mark is placed.
 * @param scale the size of the emphasis mark relative to the base text size.
 * @param block the builder block for the text to annotate.
 *
 * TODO(b/559972132): delete this helper and call `withEmphasis(position = ...)` after the library
 *   adds a position parameter in the next minor version.
 */
internal fun VerticalTextScope.withEmphasis(
    density: Density,
    style: EmphasisStyle = EmphasisSpan.DEFAULT_EMPHASIS_STYLE,
    filled: Boolean = EmphasisSpan.DEFAULT_EMPHASIS_FILL,
    position: AnnotationPosition = EmphasisSpan.DEFAULT_POSITION,
    scale: Float = EmphasisSpan.DEFAULT_SCALE,
    block: VerticalTextScope.() -> Unit,
) {
    val emphasized = SpannableStringBuilder(buildVerticalText(density, block))
    emphasized.setSpan(
        EmphasisSpan(style, filled, position, scale),
        0,
        emphasized.length,
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
    )
    text(emphasized)
}
