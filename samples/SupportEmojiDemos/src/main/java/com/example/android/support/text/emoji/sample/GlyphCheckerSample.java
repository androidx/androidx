/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.android.support.text.emoji;

import android.os.Build;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.core.graphics.PaintCompat;
import androidx.emoji2.text.EmojiCompat;

class GlyphCheckerSample {

    private GlyphCheckerSample() {
    }

    // BEGIN_INCLUDE(glyphchecker)
    static class MyGlyphChecker implements EmojiCompat.GlyphChecker {
        private TextPaint mTextPaint = new TextPaint();

        @Override
        public boolean hasGlyph(@NonNull CharSequence charSequence, int start, int end,
                int sdkAdded) {
            if (isOnDeviceX()) {
                // if on this specific device we only rely on sdkAdded
                return sdkAdded < Build.VERSION.SDK_INT;
            } else {
                String string = createString(charSequence, start, end);
                return PaintCompat.hasGlyph(mTextPaint, string);
            }
        }

    }
    // END_INCLUDE(glyphchecker)

    public static String createString(@NonNull CharSequence charSequence, int start, int end) {
        final StringBuilder builder = new StringBuilder();
        builder.setLength(0);

        while (start < end) {
            builder.append(charSequence.charAt(start));
            start++;
        }
        return builder.toString();
    }

    public static boolean isOnDeviceX() {
        // sample code returns false
        return false;
    }
}
