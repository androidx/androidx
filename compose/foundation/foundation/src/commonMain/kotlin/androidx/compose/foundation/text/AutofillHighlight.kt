/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * Returns the color used to indicate Autofill has been performed on fillable components.
 *
 * See [LocalAutofillHighlightColor] to provide new values for this throughout the hierarchy.
 */
internal expect fun autofillHighlightColor(): Color

/** CompositionLocal used to change the highlight [Brush] used for autofilled components. */
val LocalAutofillHighlightBrush =
    compositionLocalOf<Brush> {
        // The default is a solid color brush using the original color.
        SolidColor(autofillHighlightColor())
    }

/**
 * CompositionLocal used to change the [autofillHighlightColor] used by components in the hierarchy.
 */
@Deprecated(
    message =
        "Use LocalAutofillHighlightBrush instead. To provide a solid color, " +
            "use SolidColor(yourColor).",
    replaceWith =
        ReplaceWith(
            "LocalAutofillHighlightBrush",
            "androidx.compose.foundation.text.LocalAutofillHighlightBrush",
        ),
    level = DeprecationLevel.WARNING,
)
val LocalAutofillHighlightColor = compositionLocalOf { autofillHighlightColor() }

/**
 * Resolves the highlight brush based on the provided brush and color, giving precedence to the
 * color if it has been overridden.
 */
internal fun resolveAutofillHighlight(brush: Brush, color: Color, defaultColor: Color): Brush {
    val isColorOverridden = color != defaultColor
    return if (isColorOverridden) {
        SolidColor(color)
    } else {
        brush
    }
}
