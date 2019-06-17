/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.graphics;

import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

/**
 * Helper for accessing features in {@link Paint}.
 */
public final class PaintCompat {
    // U+DFFFD which is very end of unassigned plane.
    private static final String TOFU_STRING = "\uDB3F\uDFFD";
    private static final String EM_STRING = "m";

    private static final ThreadLocal<Pair<Rect, Rect>> sRectThreadLocal = new ThreadLocal<>();

    /**
     * Determine whether the typeface set on the paint has a glyph supporting the
     * string in a backwards compatible way.
     *
     * @param paint the paint instance to check
     * @param string the string to test whether there is glyph support
     * @return true if the typeface set on the given paint has a glyph for the string
     */
    public static boolean hasGlyph(@NonNull Paint paint, @NonNull String string) {
        if (Build.VERSION.SDK_INT >= 23) {
            return paint.hasGlyph(string);
        }
        final int length = string.length();

        if (length == 1 && Character.isWhitespace(string.charAt(0))) {
            // measureText + getTextBounds skips whitespace so we need to special case it here
            return true;
        }

        final float missingGlyphWidth = paint.measureText(TOFU_STRING);
        final float emGlyphWidth = paint.measureText(EM_STRING);

        final float width = paint.measureText(string);

        if (width == 0f) {
            // If the string width is 0, it can't be rendered
            return false;
        }

        if (string.codePointCount(0, string.length()) > 1) {
            // Heuristic to detect fallback glyphs for ligatures like flags and ZWJ sequences
            // Return false if string is rendered too widely
            if (width > 2 * emGlyphWidth) {
                return false;
            }

            // Heuristic to detect fallback glyphs for ligatures like flags and ZWJ sequences (2).
            // If width is greater than or equal to the sum of width of each code point, it is very
            // likely that the system is using fallback fonts to draw {@code string} in two or more
            // glyphs instead of a single ligature glyph. (hasGlyph returns false in this case.)
            // False detections are possible (the ligature glyph may happen to have the same width
            // as the sum width), but there are no good way to avoid them.
            // NOTE: This heuristic does not work with proportional glyphs.
            // NOTE: This heuristic does not work when a ZWJ sequence is partially combined.
            // E.g. If system has a glyph for "A ZWJ B" and not for "A ZWJ B ZWJ C", this heuristic
            // returns true for "A ZWJ B ZWJ C".
            float sumWidth = 0;
            int i = 0;
            while (i < length) {
                int charCount = Character.charCount(string.codePointAt(i));
                sumWidth += paint.measureText(string, i, i + charCount);
                i += charCount;
            }
            if (width >= sumWidth) {
                return false;
            }
        }

        if (width != missingGlyphWidth) {
            // If the widths are different then its not tofu
            return true;
        }

        // If the widths are the same, lets check the bounds. The chance of them being
        // different chars with the same bounds is extremely small
        final Pair<Rect, Rect> rects = obtainEmptyRects();
        paint.getTextBounds(TOFU_STRING, 0, TOFU_STRING.length(), rects.first);
        paint.getTextBounds(string, 0, length, rects.second);
        return !rects.first.equals(rects.second);
    }

    /**
     * Configure the corresponding BlendMode on the given paint. If the Android platform supports
     * the blend mode natively, it will fall back on the framework implementation of either
     * BlendMode or PorterDuff mode. If it is not supported then this method is a no-op
     * @param paint target Paint to which the BlendMode will be applied
     * @param blendMode BlendMode to configure on the paint if it is supported by the platform
     *                  version. A value of null removes the BlendMode from the Paint and restores
     *                  it to the default
     * @return true if the specified BlendMode as applied successfully, false if the platform
     * version does not support this BlendMode. If the BlendMode is not supported, this falls
     * back to the default BlendMode
     */
    public static boolean setBlendMode(@NonNull Paint paint, @Nullable BlendModeCompat blendMode) {
        if (Build.VERSION.SDK_INT >= 29) {
            paint.setBlendMode(blendMode != null ? obtainBlendModeFromCompat(blendMode) : null);
            // All blend modes supported in Q
            return true;
        } else if (blendMode != null) {
            PorterDuff.Mode mode = obtainPorterDuffFromCompat(blendMode);
            paint.setXfermode(mode != null ? new PorterDuffXfermode(mode) : null);
            // If the BlendMode has an equivalent PorterDuff mode, return true,
            // otherwise return false
            return mode != null;
        } else {
            // Configuration of a null BlendMode falls back to the default which is supported in
            // all platform levels
            paint.setXfermode(null);
            return true;
        }
    }

    /**
     * Configure the tintable color filter on the given paint. If the Android platform supports
     * the blend mode natively, it will fall back on the framework implementation of either
     * BlendModeColorFilter or PorterDuffColorFilter. If it is not supported then this method is
     * a no-op
     * @param paint target Paint to which the ColorFilter will be applied
     * @param color color which to apply the blend mode with
     * @param blendModeCompat BlendMode to configure on the color filter if it is supported by the
     *                        platform. A value of null removes the ColorFilter from the paint
     * @return true if the color filter was applied successfully with the provided BlendMode, false
     * if the platform version does not support this BlendMode. if the BlendMode is not supported
     * the ColorFilter is removed from the Paint
     */
    public static boolean setBlendModeColorFilter(@NonNull Paint paint, @ColorInt int color,
                                                     @Nullable BlendModeCompat blendModeCompat) {
        if (Build.VERSION.SDK_INT >= 29) {
            BlendMode blendMode = blendModeCompat != null
                    ? obtainBlendModeFromCompat(blendModeCompat) : null;
            if (blendMode != null) {
                paint.setColorFilter(new BlendModeColorFilter(color, blendMode));
            } else {
                paint.setColorFilter(null);
            }
            // All blend modes supported in Q
            return true;
        } else if (blendModeCompat != null) {
            PorterDuff.Mode porterDuffMode = obtainPorterDuffFromCompat(blendModeCompat);
            paint.setColorFilter(porterDuffMode != null
                    ? new PorterDuffColorFilter(color, porterDuffMode) : null);
            // If the BlendMode has an equivalent PorterDuff mode, apply the corresponding
            // PorterDuffColorFilter, otherwise remove the existing ColorFilter
            return porterDuffMode != null;
        } else {
            // Passing in null removes the ColorFilter on the paint
            paint.setColorFilter(null);
            return true;
        }
    }

    @RequiresApi(29)
    @VisibleForTesting
    /* package */ static @Nullable BlendMode obtainBlendModeFromCompat(
            @NonNull BlendModeCompat blendModeCompat) {
        switch (blendModeCompat) {
            case CLEAR:
                return BlendMode.CLEAR;
            case SRC:
                return BlendMode.SRC;
            case DST:
                return BlendMode.DST;
            case SRC_OVER:
                return BlendMode.SRC_OVER;
            case DST_OVER:
                return BlendMode.DST_OVER;
            case SRC_IN:
                return BlendMode.SRC_IN;
            case DST_IN:
                return BlendMode.DST_IN;
            case SRC_OUT:
                return BlendMode.SRC_OUT;
            case DST_OUT:
                return BlendMode.DST_OUT;
            case SRC_ATOP:
                return BlendMode.SRC_ATOP;
            case DST_ATOP:
                return BlendMode.DST_ATOP;
            case XOR:
                return BlendMode.XOR;
            case PLUS:
                return BlendMode.PLUS;
            case MODULATE:
                return BlendMode.MODULATE;
            case SCREEN:
                return BlendMode.SCREEN;
            case OVERLAY:
                return BlendMode.OVERLAY;
            case DARKEN:
                return BlendMode.DARKEN;
            case LIGHTEN:
                return BlendMode.LIGHTEN;
            case COLOR_DODGE:
                return BlendMode.COLOR_DODGE;
            case COLOR_BURN:
                return BlendMode.COLOR_BURN;
            case HARD_LIGHT:
                return BlendMode.HARD_LIGHT;
            case SOFT_LIGHT:
                return BlendMode.SOFT_LIGHT;
            case DIFFERENCE:
                return BlendMode.DIFFERENCE;
            case EXCLUSION:
                return BlendMode.EXCLUSION;
            case MULTIPLY:
                return BlendMode.MULTIPLY;
            case HUE:
                return BlendMode.HUE;
            case SATURATION:
                return BlendMode.SATURATION;
            case COLOR:
                return BlendMode.COLOR;
            case LUMINOSITY:
                return BlendMode.LUMINOSITY;
            default:
                return null;
        }
    }

    @VisibleForTesting
    /* package */ static @Nullable PorterDuff.Mode obtainPorterDuffFromCompat(
            @Nullable BlendModeCompat blendModeCompat) {
        if (blendModeCompat != null) {
            switch (blendModeCompat) {
                case CLEAR:
                    return PorterDuff.Mode.CLEAR;
                case SRC:
                    return PorterDuff.Mode.SRC;
                case DST:
                    return PorterDuff.Mode.DST;
                case SRC_OVER:
                    return PorterDuff.Mode.SRC_OVER;
                case DST_OVER:
                    return PorterDuff.Mode.DST_OVER;
                case SRC_IN:
                    return PorterDuff.Mode.SRC_IN;
                case DST_IN:
                    return PorterDuff.Mode.DST_IN;
                case SRC_OUT:
                    return PorterDuff.Mode.SRC_OUT;
                case DST_OUT:
                    return PorterDuff.Mode.DST_OUT;
                case SRC_ATOP:
                    return PorterDuff.Mode.SRC_ATOP;
                case DST_ATOP:
                    return PorterDuff.Mode.DST_ATOP;
                case XOR:
                    return PorterDuff.Mode.XOR;
                case PLUS:
                    return PorterDuff.Mode.ADD;
                // b/73224934 PorterDuff Multiply maps to Skia Modulate
                case MODULATE:
                    return PorterDuff.Mode.MULTIPLY;
                case SCREEN:
                    return PorterDuff.Mode.SCREEN;
                case OVERLAY:
                    return PorterDuff.Mode.OVERLAY;
                case DARKEN:
                    return PorterDuff.Mode.DARKEN;
                case LIGHTEN:
                    return PorterDuff.Mode.LIGHTEN;
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    private static Pair<Rect, Rect> obtainEmptyRects() {
        Pair<Rect, Rect> rects = sRectThreadLocal.get();
        if (rects == null) {
            rects = new Pair<>(new Rect(), new Rect());
            sRectThreadLocal.set(rects);
        } else {
            rects.first.setEmpty();
            rects.second.setEmpty();
        }
        return rects;
    }

    private PaintCompat() {}
}
