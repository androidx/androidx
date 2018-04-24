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

package androidx.core.text;

import android.os.Build;
import android.text.Layout;
import android.text.PrecomputedText;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.os.BuildCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;

/**
 * A text which has the character metrics data.
 *
 * A text object that contains the character metrics data and can be used to improve the performance
 * of text layout operations. When a PrecomputedTextCompat is created with a given
 * {@link CharSequence}, it will measure the text metrics during the creation. This PrecomputedText
 * instance can be set on {@link android.widget.TextView} or {@link StaticLayout}. Since the text
 * layout information will be included in this instance, {@link android.widget.TextView} or
 * {@link StaticLayout} will not have to recalculate this information.
 *
 * On API 28 or later, there is full PrecomputedText support by framework. From API 21 to API 27,
 * PrecomputedTextCompat relies on internal text layout cache. PrecomputedTextCompat immediately
 * computes the text layout in the constuctor to warm up the internal text layout cache. On API 20
 * or before, PrecomputedTextCompat does nothing.
 *
 * Note that any {@link android.text.NoCopySpan} attached to the original text won't be passed to
 * PrecomputedText.
 */
public class PrecomputedTextCompat implements Spannable {
    private static final char LINE_FEED = '\n';

    /**
     * The information required for building {@link PrecomputedTextCompat}.
     *
     * Contains information required for precomputing text measurement metadata, so it can be done
     * in isolation of a {@link android.widget.TextView} or {@link StaticLayout}, when final layout
     * constraints are not known.
     */
    public static final class Params {
        private final @NonNull TextPaint mPaint;

        // null on API 17 or before, non null on API 18 or later.
        private final @Nullable TextDirectionHeuristic mTextDir;

        private final int mBreakStrategy;

        private final int mHyphenationFrequency;

        private final PrecomputedText.Params mWrapped;

        /**
         * A builder for creating {@link Params}.
         */
        public static class Builder {
            // The TextPaint used for measurement.
            private final @NonNull TextPaint mPaint;

            // The requested text direction.
            private TextDirectionHeuristic mTextDir;

            // The break strategy for this measured text.
            private int mBreakStrategy;

            // The hyphenation frequency for this measured text.
            private int mHyphenationFrequency;

            /**
             * Builder constructor.
             *
             * @param paint the paint to be used for drawing
             */
            public Builder(@NonNull TextPaint paint) {
                mPaint = paint;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mBreakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY;
                    mHyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NORMAL;
                } else {
                    mBreakStrategy = mHyphenationFrequency = 0;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
                } else {
                    mTextDir = null;
                }
            }

            /**
             * Set the line break strategy.
             *
             * The default value is {@link Layout#BREAK_STRATEGY_HIGH_QUALITY}.
             *
             * On API 22 and below, this has no effect as there is no line break strategy.
             *
             * @param strategy the break strategy
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setBreakStrategy
             * @see android.widget.TextView#setBreakStrategy
             */
            @RequiresApi(23)
            public Builder setBreakStrategy(int strategy) {
                mBreakStrategy = strategy;
                return this;
            }

            /**
             * Set the hyphenation frequency.
             *
             * The default value is {@link Layout#HYPHENATION_FREQUENCY_NORMAL}.
             *
             * On API 22 and below, this has no effect as there is no hyphenation frequency.
             *
             * @param frequency the hyphenation frequency
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setHyphenationFrequency
             * @see android.widget.TextView#setHyphenationFrequency
             */
            @RequiresApi(23)
            public Builder setHyphenationFrequency(int frequency) {
                mHyphenationFrequency = frequency;
                return this;
            }

            /**
             * Set the text direction heuristic.
             *
             * The default value is {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
             *
             * On API 17 or before, text direction heuristics cannot be modified, so this method
             * does nothing.
             *
             * @param textDir the text direction heuristic for resolving bidi behavior
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setTextDirection
             */
            @RequiresApi(18)
            public Builder setTextDirection(@NonNull TextDirectionHeuristic textDir) {
                mTextDir = textDir;
                return this;
            }

            /**
             * Build the {@link Params}.
             *
             * @return the layout parameter
             */
            public @NonNull Params build() {
                return new Params(mPaint, mTextDir, mBreakStrategy, mHyphenationFrequency);
            }
        }

        private Params(@NonNull TextPaint paint, @NonNull TextDirectionHeuristic textDir,
                int strategy, int frequency) {
            if (BuildCompat.isAtLeastP()) {
                mWrapped = new PrecomputedText.Params.Builder(paint).setBreakStrategy(strategy)
                        .setHyphenationFrequency(frequency).setTextDirection(textDir).build();
            } else {
                mWrapped = null;
            }
            mPaint = paint;
            mTextDir = textDir;
            mBreakStrategy = strategy;
            mHyphenationFrequency = frequency;
        }

        @RequiresApi(28)
        public Params(@NonNull PrecomputedText.Params wrapped) {
            mPaint = wrapped.getTextPaint();
            mTextDir = wrapped.getTextDirection();
            mBreakStrategy = wrapped.getBreakStrategy();
            mHyphenationFrequency = wrapped.getHyphenationFrequency();
            mWrapped = wrapped;

        }

        /**
         * Returns the {@link TextPaint} for this text.
         *
         * @return A {@link TextPaint}
         */
        public @NonNull TextPaint getTextPaint() {
            return mPaint;
        }

        /**
         * Returns the {@link TextDirectionHeuristic} for this text.
         *
         * On API 17 and below, this returns null, otherwise returns non-null
         * TextDirectionHeuristic.
         *
         * @return the {@link TextDirectionHeuristic}
         */
        @RequiresApi(18)
        public @Nullable TextDirectionHeuristic getTextDirection() {
            return mTextDir;
        }

        /**
         * Returns the break strategy for this text.
         *
         * On API 22 and below, this returns 0.
         *
         * @return the line break strategy
         */
        @RequiresApi(23)
        public int getBreakStrategy() {
            return mBreakStrategy;
        }

        /**
         * Returns the hyphenation frequency for this text.
         *
         * On API 22 and below, this returns 0.
         *
         * @return the hyphenation frequency
         */
        @RequiresApi(23)
        public int getHyphenationFrequency() {
            return mHyphenationFrequency;
        }

        /**
         * Check if the same text layout.
         *
         * @return true if this and the given param result in the same text layout
         */
        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null || !(o instanceof Params)) {
                return false;
            }
            Params other = (Params) o;
            if (mWrapped != null) {
                return mWrapped.equals(other.mWrapped);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mBreakStrategy != other.getBreakStrategy()) {
                    return false;
                }
                if (mHyphenationFrequency != other.getHyphenationFrequency()) {
                    return false;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (mTextDir != other.getTextDirection()) {
                    return false;
                }
            }

            if (mPaint.getTextSize() != other.getTextPaint().getTextSize()) {
                return false;
            }
            if (mPaint.getTextScaleX() != other.getTextPaint().getTextScaleX()) {
                return false;
            }
            if (mPaint.getTextSkewX() != other.getTextPaint().getTextSkewX()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mPaint.getLetterSpacing() != other.getTextPaint().getLetterSpacing()) {
                    return false;
                }
                if (!TextUtils.equals(mPaint.getFontFeatureSettings(),
                        other.getTextPaint().getFontFeatureSettings())) {
                    return false;
                }
            }
            if (mPaint.getFlags() != other.getTextPaint().getFlags()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!mPaint.getTextLocales().equals(other.getTextPaint().getTextLocales())) {
                    return false;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                if (!mPaint.getTextLocale().equals(other.getTextPaint().getTextLocale())) {
                    return false;
                }
            }
            if (mPaint.getTypeface() == null) {
                if (other.getTextPaint().getTypeface() != null) {
                    return false;
                }
            } else if (!mPaint.getTypeface().equals(other.getTextPaint().getTypeface())) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getLetterSpacing(), mPaint.getFlags(),
                        mPaint.getTextLocales(), mPaint.getTypeface(), mPaint.isElegantTextHeight(),
                        mTextDir, mBreakStrategy, mHyphenationFrequency);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getLetterSpacing(), mPaint.getFlags(),
                        mPaint.getTextLocale(), mPaint.getTypeface(), mPaint.isElegantTextHeight(),
                        mTextDir, mBreakStrategy, mHyphenationFrequency);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getFlags(), mPaint.getTextLocale(),
                        mPaint.getTypeface(), mTextDir, mBreakStrategy, mHyphenationFrequency);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getFlags(), mPaint.getTextLocale(),
                        mPaint.getTypeface(), mTextDir, mBreakStrategy, mHyphenationFrequency);
            } else {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getFlags(), mPaint.getTypeface(), mTextDir,
                        mBreakStrategy, mHyphenationFrequency);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("textSize=" + mPaint.getTextSize());
            sb.append(", textScaleX=" + mPaint.getTextScaleX());
            sb.append(", textSkewX=" + mPaint.getTextSkewX());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sb.append(", letterSpacing=" + mPaint.getLetterSpacing());
                sb.append(", elegantTextHeight=" + mPaint.isElegantTextHeight());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sb.append(", textLocale=" + mPaint.getTextLocales());
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                sb.append(", textLocale=" + mPaint.getTextLocale());
            }
            sb.append(", typeface=" + mPaint.getTypeface());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                sb.append(", variationSettings=" + mPaint.getFontVariationSettings());
            }
            sb.append(", textDir=" + mTextDir);
            sb.append(", breakStrategy=" + mBreakStrategy);
            sb.append(", hyphenationFrequency=" + mHyphenationFrequency);
            sb.append("}");
            return sb.toString();
        }
    };

    // The original text.
    private final @NonNull Spannable mText;

    private final @NonNull Params mParams;

    // The list of measured paragraph info.
    private final @NonNull int[] mParagraphEnds;

    // null on API 27 or before. Non-null on API 28 or later
    private final @Nullable PrecomputedText mWrapped;

    /**
     * Create a new {@link PrecomputedText} which will pre-compute text measurement and glyph
     * positioning information.
     * <p>
     * This can be expensive, so computing this on a background thread before your text will be
     * presented can save work on the UI thread.
     * </p>
     *
     * Note that any {@link android.text.NoCopySpan} attached to the text won't be passed to the
     * created PrecomputedText.
     *
     * @param text the text to be measured
     * @param params parameters that define how text will be precomputed
     * @return A {@link PrecomputedText}
     */
    public static PrecomputedTextCompat create(@NonNull CharSequence text, @NonNull Params params) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(params);

        if (BuildCompat.isAtLeastP() && params.mWrapped != null) {
            return new PrecomputedTextCompat(PrecomputedText.create(text, params.mWrapped), params);
        }

        ArrayList<Integer> ends = new ArrayList<>();

        int paraEnd = 0;
        int end = text.length();
        for (int paraStart = 0; paraStart < end; paraStart = paraEnd) {
            paraEnd = TextUtils.indexOf(text, LINE_FEED, paraStart, end);
            if (paraEnd < 0) {
                // No LINE_FEED(U+000A) character found. Use end of the text as the paragraph
                // end.
                paraEnd = end;
            } else {
                paraEnd++;  // Includes LINE_FEED(U+000A) to the prev paragraph.
            }

            ends.add(paraEnd);
        }
        int[] result = new int[ends.size()];
        for (int i = 0; i < ends.size(); ++i) {
            result[i] = ends.get(i);
        }

        // No framework support for PrecomputedText
        // Compute text layout and throw away StaticLayout for the purpose of warming up the
        // internal text layout cache.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length(), params.getTextPaint(),
                    Integer.MAX_VALUE)
                    .setBreakStrategy(params.getBreakStrategy())
                    .setHyphenationFrequency(params.getHyphenationFrequency())
                    .setTextDirection(params.getTextDirection())
                    .build();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            new StaticLayout(text, params.getTextPaint(), Integer.MAX_VALUE,
                    Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        } else {
            // There is no way of precomputing text layout on API 20 or before
            // Do nothing
        }

        return new PrecomputedTextCompat(text, params, result);
    }

    // Use PrecomputedText.create instead.
    private PrecomputedTextCompat(@NonNull CharSequence text, @NonNull Params params,
            @NonNull int[] paraEnds) {
        mText = new SpannableString(text);
        mParams = params;
        mParagraphEnds = paraEnds;
        mWrapped = null;
    }

    @RequiresApi(28)
    private PrecomputedTextCompat(@NonNull PrecomputedText precomputed, @NonNull Params params) {
        mText = precomputed;
        mParams = params;
        mParagraphEnds = null;
        mWrapped = precomputed;
    }

    /**
     * Returns the layout parameters used to measure this text.
     */
    public @NonNull Params getParams() {
        return mParams;
    }

    /**
     * Returns the count of paragraphs.
     */
    public @IntRange(from = 0) int getParagraphCount() {
        if (BuildCompat.isAtLeastP()) {
            return mWrapped.getParagraphCount();
        } else {
            return mParagraphEnds.length;
        }
    }

    /**
     * Returns the paragraph start offset of the text.
     */
    public @IntRange(from = 0) int getParagraphStart(@IntRange(from = 0) int paraIndex) {
        Preconditions.checkArgumentInRange(paraIndex, 0, getParagraphCount(), "paraIndex");
        if (BuildCompat.isAtLeastP()) {
            return mWrapped.getParagraphStart(paraIndex);
        } else {
            return paraIndex == 0 ? 0 : mParagraphEnds[paraIndex - 1];
        }
    }

    /**
     * Returns the paragraph end offset of the text.
     */
    public @IntRange(from = 0) int getParagraphEnd(@IntRange(from = 0) int paraIndex) {
        Preconditions.checkArgumentInRange(paraIndex, 0, getParagraphCount(), "paraIndex");
        if (BuildCompat.isAtLeastP()) {
            return mWrapped.getParagraphEnd(paraIndex);
        } else {
            return mParagraphEnds[paraIndex];
        }
    }


    private int findParaIndex(@IntRange(from = 0) int pos) {
        for (int i = 0; i < mParagraphEnds.length; ++i) {
            if (pos < mParagraphEnds[i]) {
                return i;
            }
        }
        throw new IndexOutOfBoundsException(
                "pos must be less than " + mParagraphEnds[mParagraphEnds.length - 1]
                        + ", gave " + pos);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Spannable overrides
    //
    // Do not allow to modify MetricAffectingSpan

    /**
     * @throws IllegalArgumentException if {@link MetricAffectingSpan} is specified.
     */
    @Override
    public void setSpan(Object what, int start, int end, int flags) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be set to PrecomputedText.");
        }
        if (BuildCompat.isAtLeastP()) {
            mWrapped.setSpan(what, start, end, flags);
        } else {
            mText.setSpan(what, start, end, flags);
        }
    }

    /**
     * @throws IllegalArgumentException if {@link MetricAffectingSpan} is specified.
     */
    @Override
    public void removeSpan(Object what) {
        if (what instanceof MetricAffectingSpan) {
            throw new IllegalArgumentException(
                    "MetricAffectingSpan can not be removed from PrecomputedText.");
        }
        if (BuildCompat.isAtLeastP()) {
            mWrapped.removeSpan(what);
        } else {
            mText.removeSpan(what);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Spanned overrides
    //
    // Just proxy for underlying mText if appropriate.

    @Override
    public <T> T[] getSpans(int start, int end, Class<T> type) {
        if (BuildCompat.isAtLeastP()) {
            return mWrapped.getSpans(start, end, type);
        } else {
            return mText.getSpans(start, end, type);
        }

    }

    @Override
    public int getSpanStart(Object tag) {
        return mText.getSpanStart(tag);
    }

    @Override
    public int getSpanEnd(Object tag) {
        return mText.getSpanEnd(tag);
    }

    @Override
    public int getSpanFlags(Object tag) {
        return mText.getSpanFlags(tag);
    }

    @Override
    public int nextSpanTransition(int start, int limit, Class type) {
        return mText.nextSpanTransition(start, limit, type);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // CharSequence overrides.
    //
    // Just proxy for underlying mText.

    @Override
    public int length() {
        return mText.length();
    }

    @Override
    public char charAt(int index) {
        return mText.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return mText.subSequence(start, end);
    }

    @Override
    public String toString() {
        return mText.toString();
    }
}
