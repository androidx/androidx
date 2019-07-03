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
import androidx.text.Preconditions;

/**
 * Span that change font feature settings for font.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class FontFeatureSpan extends MetricAffectingSpan {
    private final String mFontFeatureSettings;
    public FontFeatureSpan(@NonNull String fontFeatureSettings) {
        mFontFeatureSettings = Preconditions.checkNotNull(fontFeatureSettings);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint textPaint) {
        textPaint.setFontFeatureSettings(mFontFeatureSettings);
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setFontFeatureSettings(mFontFeatureSettings);
    }

    /**
     * @return a font feature setting string this span specifies.
     */
    public String getFontFeatureSettings() {
        return mFontFeatureSettings;
    }
}
