/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import android.graphics.Paint.FontMetricsInt;
import android.text.style.LineHeightSpan;

import androidx.annotation.Px;

// LineHeightSpan.Standard is only available on API 29+, but the implementation is trivial. Just //
// re-implement it here.
class StandardLineHeightSpan implements LineHeightSpan {
    @Px private final int mLineHeightPx;

    StandardLineHeightSpan(int lineHeightPx) {
        this.mLineHeightPx = lineHeightPx;
    }

    @Override
    public void chooseHeight(
            CharSequence text,
            int start,
            int end,
            int spanstartv,
            int lineHeight,
            FontMetricsInt fm) {
        final int originHeight = fm.descent - fm.ascent;
        // If original height is not positive, do nothing.
        if (originHeight <= 0) {
            return;
        }
        final float ratio = mLineHeightPx * 1.0f / originHeight;
        fm.descent = Math.round(fm.descent * ratio);
        fm.ascent = fm.descent - mLineHeightPx;
    }

    public int getLineHeight() {
        return mLineHeightPx;
    }
}
