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

import android.graphics.Paint;
import android.os.Build;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A span which increase the space between words by given pixels.
 *
 * @hide
 */

@RequiresApi(26)
@RestrictTo(LIBRARY_GROUP)
public class WordSpacingSpan extends MetricAffectingSpan {
    private static final String TAG = "WordSpacingSpan";
    private static boolean sInitialized = false;
    private static Method sSetWordSpacingMethod = null;

    private static boolean hasWordSpacingSupport() {
        return Build.VERSION.SDK_INT >= 26 && Build.VERSION.SDK_INT <= 28;
    }

    // TODO(Migration/haoyuchang): This function exist after API 26 but StaticLayout doesn't work
    // correctly until Q. see b/122471618, try to support it in old versions.
    // Also it is supposed to be place in PaintCompat, see b/122840964,
    private static void obtainSetWordSpacingMethod() {
        if (sInitialized) return;
        sInitialized = true;
        if (hasWordSpacingSupport()) {
            try {
                sSetWordSpacingMethod = Paint.class.getMethod(
                        "setWordSpacing", float.class /* wordSpacing */);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "unable to collect necessary method.");
            }
        }
    }

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
    public void updateDrawState(TextPaint textPaint) {
        updatePaint(textPaint);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        updatePaint(textPaint);
    }

    private void updatePaint(TextPaint textPaint) {
        if (hasWordSpacingSupport()) {
            obtainSetWordSpacingMethod();
            if (sSetWordSpacingMethod == null) return;
            try {
                sSetWordSpacingMethod.invoke(textPaint, mWordSpacing);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, "unable to call setWordSpacing on Paint.");
            }
        } else {
            // TODO(Migration/haoyuchang): call Paint.setWordSpacing directly when it's available
        }
    }
}
