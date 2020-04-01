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

package androidx.ui.text.platform

import android.graphics.text.LineBreaker
import android.text.Layout
import android.text.Layout.Alignment
import android.text.TextDirectionHeuristic
import android.text.TextDirectionHeuristics
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/**
 * LayoutCompat class which provides all supported attributes by framework, and also defines
 * default value of those attributes for Compose.
 *
 * @suppress
 */
object LayoutCompat {
    const val ALIGN_NORMAL = 0
    const val ALIGN_OPPOSITE = 1
    const val ALIGN_CENTER = 2
    const val ALIGN_LEFT = 3
    const val ALIGN_RIGHT = 4
    /**
     * @suppress
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        ALIGN_NORMAL,
        ALIGN_CENTER,
        ALIGN_OPPOSITE,
        ALIGN_LEFT,
        ALIGN_RIGHT
    )
    internal annotation class TextLayoutAlignment

    const val JUSTIFICATION_MODE_NONE = LineBreaker.JUSTIFICATION_MODE_NONE
    const val JUSTIFICATION_MODE_INTER_WORD = LineBreaker.JUSTIFICATION_MODE_INTER_WORD
    /**
     * @suppress
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(JUSTIFICATION_MODE_NONE, JUSTIFICATION_MODE_INTER_WORD)
    internal annotation class JustificationMode

    const val HYPHENATION_FREQUENCY_NORMAL = Layout.HYPHENATION_FREQUENCY_NORMAL
    const val HYPHENATION_FREQUENCY_FULL = Layout.HYPHENATION_FREQUENCY_FULL
    const val HYPHENATION_FREQUENCY_NONE = Layout.HYPHENATION_FREQUENCY_NONE
    /**
     * @suppress
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        HYPHENATION_FREQUENCY_NORMAL,
        HYPHENATION_FREQUENCY_FULL,
        HYPHENATION_FREQUENCY_NONE
    )
    internal annotation class HyphenationFrequency

    const val BREAK_STRATEGY_SIMPLE = LineBreaker.BREAK_STRATEGY_SIMPLE
    const val BREAK_STRATEGY_HIGH_QUALITY = LineBreaker.BREAK_STRATEGY_HIGH_QUALITY
    const val BREAK_STRATEGY_BALANCED = LineBreaker.BREAK_STRATEGY_BALANCED
    /**
     * @suppress
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        BREAK_STRATEGY_SIMPLE,
        BREAK_STRATEGY_HIGH_QUALITY,
        BREAK_STRATEGY_BALANCED
    )
    internal annotation class BreakStrategy

    const val TEXT_DIRECTION_LTR = 0
    const val TEXT_DIRECTION_RTL = 1
    const val TEXT_DIRECTION_FIRST_STRONG_LTR = 2
    const val TEXT_DIRECTION_FIRST_STRONG_RTL = 3
    const val TEXT_DIRECTION_ANY_RTL_LTR = 4
    const val TEXT_DIRECTION_LOCALE = 5
    /**
     * @suppress
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        TEXT_DIRECTION_LTR,
        TEXT_DIRECTION_RTL,
        TEXT_DIRECTION_FIRST_STRONG_LTR,
        TEXT_DIRECTION_FIRST_STRONG_RTL,
        TEXT_DIRECTION_ANY_RTL_LTR,
        TEXT_DIRECTION_LOCALE
    )
    internal annotation class TextDirection

    const val DEFAULT_ALIGNMENT = ALIGN_NORMAL

    const val DEFAULT_TEXT_DIRECTION = TEXT_DIRECTION_FIRST_STRONG_LTR

    const val DEFAULT_LINESPACING_MULTIPLIER = 1.0f

    const val DEFAULT_LINESPACING_EXTRA = 0.0f

    const val DEFAULT_INCLUDE_PADDING = true

    const val DEFAULT_MAX_LINES = Integer.MAX_VALUE

    const val DEFAULT_BREAK_STRATEGY = BREAK_STRATEGY_SIMPLE

    const val DEFAULT_HYPHENATION_FREQUENCY = HYPHENATION_FREQUENCY_NONE

    const val DEFAULT_JUSTIFICATION_MODE = JUSTIFICATION_MODE_NONE

    const val DEFAULT_FALLBACK_LINE_SPACING = true

    val DEFAULT_LAYOUT_ALIGNMENT = Alignment.ALIGN_NORMAL

    val DEFAULT_TEXT_DIRECTION_HEURISTIC: TextDirectionHeuristic =
        TextDirectionHeuristics.FIRSTSTRONG_LTR
}