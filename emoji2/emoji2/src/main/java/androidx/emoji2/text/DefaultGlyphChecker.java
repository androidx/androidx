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

package androidx.emoji2.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Build;
import android.text.TextPaint;

import androidx.annotation.AnyThread;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.PaintCompat;

import org.jspecify.annotations.NonNull;

/**
 * Utility class that checks if the system can render a given glyph.
 *
 */
@AnyThread
@RestrictTo(LIBRARY)
class DefaultGlyphChecker implements EmojiCompat.GlyphChecker {
    /**
     * Default text size for {@link #mTextPaint}.
     */
    private static final int PAINT_TEXT_SIZE = 10;

    /**
     * Used to create strings required by
     * {@link PaintCompat#hasGlyph(android.graphics.Paint, String)}.
     */
    private static final ThreadLocal<StringBuilder> sStringBuilder = new ThreadLocal<>();

    /**
     * TextPaint used during {@link PaintCompat#hasGlyph(android.graphics.Paint, String)} check.
     */
    private final TextPaint mTextPaint;

    DefaultGlyphChecker() {
        mTextPaint = new TextPaint();
        mTextPaint.setTextSize(PAINT_TEXT_SIZE);
    }

    @Override
    public boolean hasGlyph(
            @NonNull CharSequence charSequence,
            int start,
            int end,
            int sdkAdded
    ) {
        // For pre M devices, heuristic in PaintCompat can result in false positives. we are
        // adding another heuristic using the sdkAdded field. if the emoji was added to OS
        // at a later version we assume that the system probably cannot render it.
        if (Build.VERSION.SDK_INT < 23 && sdkAdded > Build.VERSION.SDK_INT) {
            return false;
        }

        final StringBuilder builder = getStringBuilder();
        builder.setLength(0);

        while (start < end) {
            builder.append(charSequence.charAt(start));
            start++;
        }

        return PaintCompat.hasGlyph(mTextPaint, builder.toString());
    }

    private static StringBuilder getStringBuilder() {
        if (sStringBuilder.get() == null) {
            sStringBuilder.set(new StringBuilder());
        }
        return sStringBuilder.get();
    }
}
