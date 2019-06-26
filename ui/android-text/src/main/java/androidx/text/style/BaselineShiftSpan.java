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
import androidx.annotation.RestrictTo;

/**
 * Span which shifts the vertical position of baseline.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class BaselineShiftSpan extends MetricAffectingSpan {
    private final float mMultiplier;

    /**
     * Create a {@link BaselineShiftSpan}.
     * @param multiplier shift the baseline above by (baseline - ascent) * multiplier pixels.
     */
    public BaselineShiftSpan(float multiplier) {
        mMultiplier = multiplier;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.baselineShift += textPaint.ascent() * mMultiplier;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.baselineShift += textPaint.ascent() * mMultiplier;
    }

    public float getMultiplier() {
        return mMultiplier;
    }
}
