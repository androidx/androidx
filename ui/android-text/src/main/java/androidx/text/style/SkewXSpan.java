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

import androidx.annotation.RestrictTo;

/**
 * Span which shear text in x direction. A pixel at (x, y) will be transfer to (x + y * skewX, y),
 * where y is the distant above baseline.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class SkewXSpan extends MetricAffectingSpan {
    private final float mSkewX;

    public SkewXSpan(float skewX) {
        mSkewX = skewX;
    }

    /**
     * @return the skew factor specified by this span.
     */
    public float getSkewX() {
        return mSkewX;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setTextSkewX(mSkewX + textPaint.getTextSkewX());
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        textPaint.setTextSkewX(mSkewX + textPaint.getTextSkewX());
    }
}
