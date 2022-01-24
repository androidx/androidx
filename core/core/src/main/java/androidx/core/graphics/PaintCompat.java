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

import static androidx.core.graphics.BlendModeUtils.obtainBlendModeFromCompat;
import static androidx.core.graphics.BlendModeUtils.obtainPorterDuffFromCompat;

import android.graphics.BlendMode;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
            return Api23Impl.hasGlyph(paint, string);
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
            Api29Impl.setBlendMode(paint,
                    blendMode != null ? obtainBlendModeFromCompat(blendMode) : null);
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

    private PaintCompat() {
    }

    @RequiresApi(23)
    static class Api23Impl {
        private Api23Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean hasGlyph(Paint paint, String string) {
            return paint.hasGlyph(string);
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void setBlendMode(Paint paint, BlendMode blendmode) {
            paint.setBlendMode(blendmode);
        }
    }
}
