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

package androidx.text.style;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;


/**
 * A span which increase the space between words by given pixels.
 *
 * @hide
 */

@RequiresApi(29)
@RestrictTo(LIBRARY_GROUP)
public class WordSpacingSpan extends MetricAffectingSpan {
    private final float mWordSpacing;

    /**
     * Create a {@link WordSpacingSpan}
     *
     * @param wordSpacing the extra space added between words, in pixel
     */
    public WordSpacingSpan(float wordSpacing) {
        mWordSpacing = wordSpacing;
    }

    /**
     * @return the extra space added between words, in pixel
     */
    public float getWordSpacing() {
        return mWordSpacing;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        textPaint.setWordSpacing(mWordSpacing);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.setWordSpacing(mWordSpacing);
    }
}
