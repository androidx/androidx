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

import android.os.Build;
import android.text.PrecomputedText;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;

import androidx.annotation.RequiresApi;
import androidx.core.text.PrecomputedTextCompat;

import org.jspecify.annotations.NonNull;

import java.util.stream.IntStream;

/**
 * Spannable that will delegate to a passed spannable for all query operations without allocation.
 *
 * If delegating to a PrecomputedText, the delegate will be swapped for a SpannableString prior
 * to any modifications.
 */
class UnprecomputeTextOnModificationSpannable implements Spannable {


    /**
     * True when mDelegate is safe to write, otherwise mDelegate will need wrapped before mutation
     */
    private boolean mSafeToWrite = false;

    private @NonNull Spannable mDelegate;

    UnprecomputeTextOnModificationSpannable(@NonNull Spannable delegate) {
        mDelegate = delegate;
    }

    UnprecomputeTextOnModificationSpannable(@NonNull Spanned delegate) {
        mDelegate = new SpannableString(delegate);
    }

    UnprecomputeTextOnModificationSpannable(@NonNull CharSequence delegate) {
        mDelegate = new SpannableString(delegate);
    }

    private void ensureSafeWrites() {
        Spannable old = mDelegate;
        if (!mSafeToWrite && precomputedTextDetector().isPrecomputedText(old)) {
            mDelegate = new SpannableString(old);
        }
        mSafeToWrite = true;
    }

    Spannable getUnwrappedSpannable() {
        return mDelegate;
    }

    @Override
    public void setSpan(Object o, int i, int i1, int i2) {
        ensureSafeWrites();
        mDelegate.setSpan(o, i, i1, i2);
    }

    @Override
    public void removeSpan(Object o) {
        ensureSafeWrites();
        mDelegate.removeSpan(o);
    }

    @Override
    public <T> T[] getSpans(int i, int i1, Class<T> aClass) {
        return mDelegate.getSpans(i, i1, aClass);
    }

    @Override
    public int getSpanStart(Object o) {
        return mDelegate.getSpanStart(o);
    }

    @Override
    public int getSpanEnd(Object o) {
        return mDelegate.getSpanEnd(o);
    }

    @Override
    public int getSpanFlags(Object o) {
        return mDelegate.getSpanFlags(o);
    }

    @Override
    public int nextSpanTransition(int i, int i1, Class aClass) {
        return mDelegate.nextSpanTransition(i, i1, aClass);
    }

    @Override
    public int length() {
        return mDelegate.length();
    }

    @Override
    public char charAt(int i) {
        return mDelegate.charAt(i);
    }

    @Override
    public @NonNull CharSequence subSequence(int i, int i1) {
        return mDelegate.subSequence(i, i1);
    }

    @Override
    public @NonNull String toString() {
        return mDelegate.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public @NonNull IntStream chars() {
        return CharSequenceHelper_API24.chars(mDelegate);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public @NonNull IntStream codePoints() {
        return CharSequenceHelper_API24.codePoints(mDelegate);
    }

    @RequiresApi(24)
    private static class CharSequenceHelper_API24 {
        private CharSequenceHelper_API24() {
            // not constructable
        }

        static IntStream codePoints(CharSequence charSequence) {
            return charSequence.codePoints();
        }

        static IntStream chars(CharSequence charSequence) {
            return charSequence.chars();
        }
    }

    static PrecomputedTextDetector precomputedTextDetector() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
                ? new PrecomputedTextDetector() : new PrecomputedTextDetector_28();
    }

    static class PrecomputedTextDetector {

        boolean isPrecomputedText(CharSequence text) {
            return text instanceof PrecomputedTextCompat;
        }
    }

    @RequiresApi(28)
    static class PrecomputedTextDetector_28 extends PrecomputedTextDetector {

        @Override
        boolean isPrecomputedText(CharSequence text) {
            return text instanceof PrecomputedText || text instanceof PrecomputedTextCompat;
        }
    }
}


