/*
 * Copyright 2018 The Android Open Source Project
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

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Span that displays text in the given Typeface. In Android Framework, TypefaceSpan that accepts
 * a Typeface as constructor argument was added in API 28, therefore was not usable before 28.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class TypefaceSpan extends MetricAffectingSpan {

    @NonNull
    private final Typeface mTypeface;

    /**
     * Constructs a {@link android.text.style.TypefaceSpan} from a {@link Typeface}. The previous
     * style of the TextPaint is overridden and the style of the typeface is used.
     *
     * @param typeface Typeface to render the text with
     */
    public TypefaceSpan(@NonNull Typeface typeface) {
        mTypeface = typeface;
    }

    /**
     * Returns the typeface set in the span.
     *
     * @return the typeface set
     * @see #TypefaceSpan(Typeface)
     */
    @NonNull
    public Typeface getTypeface() {
        return mTypeface;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        updateTypeface(ds);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        updateTypeface(paint);
    }

    private void updateTypeface(@NonNull Paint paint) {
        if (mTypeface != null) {
            paint.setTypeface(mTypeface);
        }
    }
}
