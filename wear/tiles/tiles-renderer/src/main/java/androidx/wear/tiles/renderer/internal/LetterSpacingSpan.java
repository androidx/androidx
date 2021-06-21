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

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;

/** LetterSpacingSpan class used to apply custom spacing between letters. */
public class LetterSpacingSpan extends MetricAffectingSpan {

    private final float mLetterSpacingEm;

    /** @param letterSpacingEm letter-spacing for text. */
    public LetterSpacingSpan(float letterSpacingEm) {
        this.mLetterSpacingEm = letterSpacingEm;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint textPaint) {
        updateTextPaint(textPaint);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        updateTextPaint(textPaint);
    }

    private void updateTextPaint(@NonNull TextPaint textPaint) {
        textPaint.setLetterSpacing(mLetterSpacingEm);
    }
}
