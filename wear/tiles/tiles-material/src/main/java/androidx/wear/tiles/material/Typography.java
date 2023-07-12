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

import static androidx.annotation.Dimension.DP;
import static androidx.annotation.Dimension.SP;
import static androidx.wear.tiles.material.Helper.checkNotNull;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Typography styles, currently set up to match Wear's styling.
 *
 * @deprecated Use the new class {@link androidx.wear.protolayout.material.Typography} which
 *     provides the same API and functionality.
 */
@Deprecated
@SuppressWarnings("deprecation")
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
    /**
     * Returns the {@link androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder} for the given
     * androidx.wear.tiles.LayoutElementBuilders.FontStyle code with the recommended size, weight
     * and letter spacing. Font will be scalable.
     */
    @NonNull
    static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder getFontStyleBuilder(
            @TypographyName int fontStyleCode, @NonNull Context context) {
        return getFontStyleBuilder(fontStyleCode, context, true);
    }

    private Typography() {}

    /**
     * Returns the {@link androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder} for the given
     * Typography code with the recommended size, weight and letter spacing, with the option to make
     * this font not scalable.
     */
    @NonNull
    static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder getFontStyleBuilder(
            @TypographyName int typographyCode, @NonNull Context context, boolean isScalable) {
        switch (typographyCode) {
            case TYPOGRAPHY_BODY1:
                return body1(isScalable, context);
            case TYPOGRAPHY_BODY2:
                return body2(isScalable, context);
            case TYPOGRAPHY_BUTTON:
                return button(isScalable, context);
            case TYPOGRAPHY_CAPTION1:
                return caption1(isScalable, context);
            case TYPOGRAPHY_CAPTION2:
                return caption2(isScalable, context);
            case TYPOGRAPHY_CAPTION3:
                return caption3(isScalable, context);
            case TYPOGRAPHY_DISPLAY1:
                return display1(isScalable, context);
            case TYPOGRAPHY_DISPLAY2:
                return display2(isScalable, context);
            case TYPOGRAPHY_DISPLAY3:
                return display3(isScalable, context);
            case TYPOGRAPHY_TITLE1:
                return title1(isScalable, context);
            case TYPOGRAPHY_TITLE2:
                return title2(isScalable, context);
            case TYPOGRAPHY_TITLE3:
                return title3(isScalable, context);
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
    static androidx.wear.tiles.DimensionBuilders.SpProp getLineHeightForTypography(
            @TypographyName int typography) {
        if (!TYPOGRAPHY_TO_LINE_HEIGHT_SP.containsKey(typography)) {
            throw new IllegalArgumentException("Typography " + typography + " doesn't exist.");
        }
        return androidx.wear.tiles.DimensionBuilders.sp(
                checkNotNull(TYPOGRAPHY_TO_LINE_HEIGHT_SP.get(typography)).intValue());
    }

    @NonNull
    @SuppressLint("ResourceType")
    // This is a helper function to make the font not scalable. It should interpret in value as DP
    // and convert it to SP which is needed to be passed in as a font size. However, we will pass an
    // SP object to it, because the default style is defined in it, but for the case when the font
    // size on device in 1, so the DP is equal to SP.
    // TODO(b/267744228): Remove the warning suppression.
    @SuppressWarnings("deprecation")
    private static androidx.wear.tiles.DimensionBuilders.SpProp dpToSp(
            @NonNull Context context, @Dimension(unit = DP) float valueDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float scaledSp = (valueDp / metrics.scaledDensity) * metrics.density;
        return androidx.wear.tiles.DimensionBuilders.sp(scaledSp);
    }

    // The @Dimension(unit = SP) on androidx.wear.tiles.DimensionBuilders.sp() is seemingly being
    // ignored, so lint complains that we're passing SP to something expecting PX. Just suppress the
    // warning for now.
    @SuppressLint("ResourceType")
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder
            createFontStyleBuilder(
                    @Dimension(unit = SP) int size,
                    @androidx.wear.tiles.LayoutElementBuilders.FontWeight int weight,
                    @androidx.wear.tiles.LayoutElementBuilders.FontVariant int variant,
                    float letterSpacing,
                    boolean isScalable,
                    @NonNull Context context) {
        return new androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder()
                .setSize(
                        isScalable
                                ? androidx.wear.tiles.DimensionBuilders.sp(size)
                                : dpToSp(context, size))
                .setLetterSpacing(androidx.wear.tiles.DimensionBuilders.em(letterSpacing))
                .setVariant(variant)
                .setWeight(weight);
    }

    /** Font style for large display text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder display1(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                40,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for medium display text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder display2(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                34,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.03f,
                isScalable,
                context);
    }

    /** Font style for small display text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder display3(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                30,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.03f,
                isScalable,
                context);
    }

    /** Font style for large title text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder title1(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                24,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.008f,
                isScalable,
                context);
    }

    /** Font style for medium title text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder title2(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                20,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for small title text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder title3(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                16,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_TITLE,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for normal body text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder body1(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                16,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_NORMAL,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for small body text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder body2(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                14,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_NORMAL,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.014f,
                isScalable,
                context);
    }

    /** Font style for bold button text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder button(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                15,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_BOLD,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.03f,
                isScalable,
                context);
    }

    /** Font style for large caption text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder caption1(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                14,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for medium caption text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder caption2(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                12,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.01f,
                isScalable,
                context);
    }

    /** Font style for small caption text. */
    @NonNull
    private static androidx.wear.tiles.LayoutElementBuilders.FontStyle.Builder caption3(
            boolean isScalable, @NonNull Context context) {
        return createFontStyleBuilder(
                10,
                androidx.wear.tiles.LayoutElementBuilders.FONT_WEIGHT_MEDIUM,
                androidx.wear.tiles.LayoutElementBuilders.FONT_VARIANT_BODY,
                0.01f,
                isScalable,
                context);
    }
}
