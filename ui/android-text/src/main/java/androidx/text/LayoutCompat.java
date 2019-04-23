/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.Layout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * LayoutCompat class which provides all supported attributes by framework, and also defines
 * default value of those attributes for Crane.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class LayoutCompat {
    private LayoutCompat() { }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ALIGN_NORMAL = 0;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ALIGN_OPPOSITE = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ALIGN_CENTER = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ALIGN_LEFT = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int ALIGN_RIGHT = 4;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int JUSTIFICATION_MODE_NONE = Layout.JUSTIFICATION_MODE_NONE;
    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int JUSTIFICATION_MODE_INTER_WORD = Layout.JUSTIFICATION_MODE_INTER_WORD;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int HYPHENATION_FREQUENCY_NORMAL = Layout.HYPHENATION_FREQUENCY_NORMAL;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int HYPHENATION_FREQUENCY_FULL = Layout.HYPHENATION_FREQUENCY_FULL;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int HYPHENATION_FREQUENCY_NONE = Layout.HYPHENATION_FREQUENCY_NONE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BREAK_STRATEGY_SIMPLE = Layout.BREAK_STRATEGY_SIMPLE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BREAK_STRATEGY_HIGH_QUALITY = Layout.BREAK_STRATEGY_HIGH_QUALITY;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int BREAK_STRATEGY_BALANCED = Layout.BREAK_STRATEGY_BALANCED;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_LTR = 0;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_RTL = 1;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 2;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 3;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_ANY_RTL_LTR = 4;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int TEXT_DIRECTION_LOCALE = 5;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            ALIGN_NORMAL,
            ALIGN_CENTER,
            ALIGN_OPPOSITE,
            ALIGN_LEFT,
            ALIGN_RIGHT
    })
    public @interface TextLayoutAlignment { }


    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            JUSTIFICATION_MODE_NONE,
            JUSTIFICATION_MODE_INTER_WORD
    })
    public @interface JustificationMode { }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            BREAK_STRATEGY_SIMPLE,
            BREAK_STRATEGY_HIGH_QUALITY,
            BREAK_STRATEGY_BALANCED
    })
    public @interface BreakStrategy { }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            HYPHENATION_FREQUENCY_NORMAL,
            HYPHENATION_FREQUENCY_FULL,
            HYPHENATION_FREQUENCY_NONE
    })
    public @interface HyphenationFrequency { }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            TEXT_DIRECTION_LTR,
            TEXT_DIRECTION_RTL,
            TEXT_DIRECTION_FIRST_STRONG_LTR,
            TEXT_DIRECTION_FIRST_STRONG_RTL,
            TEXT_DIRECTION_ANY_RTL_LTR,
            TEXT_DIRECTION_LOCALE
    })
    public @interface TextDirection { }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_ALIGNMENT = ALIGN_NORMAL;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static final Layout.Alignment DEFAULT_LAYOUT_ALIGNMENT = Layout.Alignment.ALIGN_NORMAL;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_TEXT_DIRECTION = TEXT_DIRECTION_FIRST_STRONG_LTR;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    static final TextDirectionHeuristic DEFAULT_TEXT_DIRECTION_HEURISTIC =
            TextDirectionHeuristics.FIRSTSTRONG_LTR;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final float DEFAULT_LINESPACING_MULTIPLIER = 1.0f;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final float DEFAULT_LINESPACING_EXTRA = 0.0f;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final boolean DEFAULT_INCLUDE_PADDING = true;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final boolean DEFAULT_FALLBACK_LINE_SPACING = true;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_MAX_LINES = Integer.MAX_VALUE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_BREAK_STRATEGY = BREAK_STRATEGY_SIMPLE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_HYPHENATION_FREQUENCY = HYPHENATION_FREQUENCY_NONE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final int DEFAULT_JUSTIFICATION_MODE = JUSTIFICATION_MODE_NONE;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static final boolean DEFAULT_ADD_LAST_LINE_LINE_SPCAING = false;
}
