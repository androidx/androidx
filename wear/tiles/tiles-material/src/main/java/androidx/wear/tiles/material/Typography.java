/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.tiles.material;

import static androidx.annotation.Dimension.SP;
import static androidx.wear.tiles.DimensionBuilders.sp;
import static androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD;
import static androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM;
import static androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_NORMAL;
import static androidx.wear.tiles.material.Helper.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.wear.tiles.DimensionBuilders;
import androidx.wear.tiles.DimensionBuilders.SpProp;
import androidx.wear.tiles.LayoutElementBuilders.FontStyle;
import androidx.wear.tiles.LayoutElementBuilders.FontWeight;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/** Typography styles, currently set up to match Wear's styling. */
public class Typography {
    /** Typography for large display text. */
    public static final int TYPOGRAPHY_DISPLAY1 = 1;

    /** Typography for medium display text. */
    public static final int TYPOGRAPHY_DISPLAY2 = 2;

    /** Typography for small display text. */
    public static final int TYPOGRAPHY_DISPLAY3 = 3;

    /** Typography for large title text. */
    public static final int TYPOGRAPHY_TITLE1 = 4;

    /** Typography for medium title text. */
    public static final int TYPOGRAPHY_TITLE2 = 5;

    /** Typography for small title text. */
    public static final int TYPOGRAPHY_TITLE3 = 6;

    /** Typography for large body text. */
    public static final int TYPOGRAPHY_BODY1 = 7;

    /** Typography for medium body text. */
    public static final int TYPOGRAPHY_BODY2 = 8;

    /** Typography for bold button text. */
    public static final int TYPOGRAPHY_BUTTON = 9;

    /** Typography for large caption text. */
    public static final int TYPOGRAPHY_CAPTION1 = 10;

    /** Typography for medium caption text. */
    public static final int TYPOGRAPHY_CAPTION2 = 11;

    /** Typography for small caption text. */
    public static final int TYPOGRAPHY_CAPTION3 = 12;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        TYPOGRAPHY_DISPLAY1,
        TYPOGRAPHY_DISPLAY2,
        TYPOGRAPHY_DISPLAY3,
        TYPOGRAPHY_TITLE1,
        TYPOGRAPHY_TITLE2,
        TYPOGRAPHY_TITLE3,
        TYPOGRAPHY_BODY1,
        TYPOGRAPHY_BODY2,
        TYPOGRAPHY_BUTTON,
        TYPOGRAPHY_CAPTION1,
        TYPOGRAPHY_CAPTION2,
        TYPOGRAPHY_CAPTION3
    })
    @interface TypographyName {}

    /** Mapping for line height for different typography. */
    @NonNull
    private static final Map<Integer, Float> TYPOGRAPHY_TO_LINE_HEIGHT_SP = new HashMap<>();

    static {
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_DISPLAY1, 46f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_DISPLAY2, 40f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_DISPLAY3, 36f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_TITLE1, 28f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_TITLE2, 24f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_TITLE3, 20f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_BODY1, 20f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_BODY2, 18f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_BUTTON, 19f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_CAPTION1, 18f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_CAPTION2, 16f);
        TYPOGRAPHY_TO_LINE_HEIGHT_SP.put(TYPOGRAPHY_CAPTION3, 14f);
    }

    private Typography() {}

    /**
     * Returns the {@link FontStyle.Builder} for the given Typography code with the recommended
     * size, weight and letter spacing.
     */
    @NonNull
    static FontStyle.Builder getFontStyleBuilder(@TypographyName int typographyCode) {
        switch (typographyCode) {
            case TYPOGRAPHY_BODY1:
                return body1();
            case TYPOGRAPHY_BODY2:
                return body2();
            case TYPOGRAPHY_BUTTON:
                return button();
            case TYPOGRAPHY_CAPTION1:
                return caption1();
            case TYPOGRAPHY_CAPTION2:
                return caption2();
            case TYPOGRAPHY_CAPTION3:
                return caption3();
            case TYPOGRAPHY_DISPLAY1:
                return display1();
            case TYPOGRAPHY_DISPLAY2:
                return display2();
            case TYPOGRAPHY_DISPLAY3:
                return display3();
            case TYPOGRAPHY_TITLE1:
                return title1();
            case TYPOGRAPHY_TITLE2:
                return title2();
            case TYPOGRAPHY_TITLE3:
                return title3();
            default:
                // Shouldn't happen.
                throw new IllegalArgumentException(
                        "Typography " + typographyCode + " doesn't exist.");
        }
    }

    /**
     * Returns the recommended line height for the given Typography to be added to the Text
     * component.
     */
    @NonNull
    static SpProp getLineHeightForTypography(@TypographyName int typography) {
        if (!TYPOGRAPHY_TO_LINE_HEIGHT_SP.containsKey(typography)) {
            throw new IllegalArgumentException("Typography " + typography + " doesn't exist.");
        }
        return sp(checkNotNull(TYPOGRAPHY_TO_LINE_HEIGHT_SP.get(typography)).intValue());
    }

    // The @Dimension(unit = SP) on sp() is seemingly being ignored, so lint complains that we're
    // passing SP to something expecting PX. Just suppress the warning for now.
    @SuppressLint("ResourceType")
    private static FontStyle.Builder createFontStyleBuilder(
            @Dimension(unit = SP) int size, @FontWeight int weight, float letterSpacing) {
        return new FontStyle.Builder()
                .setSize(DimensionBuilders.sp(size))
                .setLetterSpacing(DimensionBuilders.em(letterSpacing))
                .setWeight(weight);
    }

    /** Font style for large display text. */
    @NonNull
    private static FontStyle.Builder display1() {
        return createFontStyleBuilder(40, FONT_WEIGHT_MEDIUM, 0.01f);
    }

    /** Font style for medium display text. */
    @NonNull
    private static FontStyle.Builder display2() {
        return createFontStyleBuilder(34, FONT_WEIGHT_MEDIUM, 0.03f);
    }

    /** Font style for small display text. */
    @NonNull
    private static FontStyle.Builder display3() {
        return createFontStyleBuilder(30, FONT_WEIGHT_MEDIUM, 0.03f);
    }

    /** Font style for large title text. */
    @NonNull
    private static FontStyle.Builder title1() {
        return createFontStyleBuilder(24, FONT_WEIGHT_MEDIUM, 0.008f);
    }

    /** Font style for medium title text. */
    @NonNull
    private static FontStyle.Builder title2() {
        return createFontStyleBuilder(20, FONT_WEIGHT_MEDIUM, 0.01f);
    }

    /** Font style for small title text. */
    @NonNull
    private static FontStyle.Builder title3() {
        return createFontStyleBuilder(16, FONT_WEIGHT_MEDIUM, 0.01f);
    }

    /** Font style for normal body text. */
    @NonNull
    private static FontStyle.Builder body1() {
        return createFontStyleBuilder(16, FONT_WEIGHT_NORMAL, 0.01f);
    }

    /** Font style for small body text. */
    @NonNull
    private static FontStyle.Builder body2() {
        return createFontStyleBuilder(14, FONT_WEIGHT_NORMAL, 0.014f);
    }

    /** Font style for bold button text. */
    @NonNull
    private static FontStyle.Builder button() {
        return createFontStyleBuilder(15, FONT_WEIGHT_BOLD, 0.03f);
    }

    /** Font style for large caption text. */
    @NonNull
    private static FontStyle.Builder caption1() {
        return createFontStyleBuilder(14, FONT_WEIGHT_MEDIUM, 0.01f);
    }

    /** Font style for medium caption text. */
    @NonNull
    private static FontStyle.Builder caption2() {
        return createFontStyleBuilder(12, FONT_WEIGHT_MEDIUM, 0.01f);
    }

    /** Font style for small caption text. */
    @NonNull
    private static FontStyle.Builder caption3() {
        return createFontStyleBuilder(10, FONT_WEIGHT_MEDIUM, 0.01f);
    }
}
