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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Trace;
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

import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

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
 * On API 29 or later, there is full PrecomputedText support by framework. From API 23 to API 27,
 * PrecomputedTextCompat relies on internal text layout cache. PrecomputedTextCompat immediately
 * computes the text layout in the constructor to warm up the internal text layout cache.
 *
 * Note that any {@link android.text.NoCopySpan} attached to the original text won't be passed to
 * PrecomputedText.
 */
public class PrecomputedTextCompat implements Spannable {
    private static final char LINE_FEED = '\n';

    private static final Object sLock = new Object();
    @GuardedBy("sLock") private static @NonNull Executor sExecutor = null;

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

        final PrecomputedText.Params mWrapped;

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
                mBreakStrategy = Layout.BREAK_STRATEGY_HIGH_QUALITY;
                mHyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NORMAL;
                mTextDir = TextDirectionHeuristics.FIRSTSTRONG_LTR;
            }

            /**
             * Set the line break strategy.
             *
             * The default value is {@link Layout#BREAK_STRATEGY_HIGH_QUALITY}.
             *
             * @param strategy the break strategy
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setBreakStrategy
             * @see android.widget.TextView#setBreakStrategy
             */
            public Builder setBreakStrategy(int strategy) {
                mBreakStrategy = strategy;
                return this;
            }

            /**
             * Set the hyphenation frequency.
             *
             * The default value is {@link Layout#HYPHENATION_FREQUENCY_NORMAL}.
             *
             * @param frequency the hyphenation frequency
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setHyphenationFrequency
             * @see android.widget.TextView#setHyphenationFrequency
             */
            public Builder setHyphenationFrequency(int frequency) {
                mHyphenationFrequency = frequency;
                return this;
            }

            /**
             * Set the text direction heuristic.
             *
             * The default value is {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
             *
             * @param textDir the text direction heuristic for resolving bidi behavior
             * @return PrecomputedTextCompat.Builder instance
             * @see StaticLayout.Builder#setTextDirection
             */
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

        Params(@NonNull TextPaint paint, @NonNull TextDirectionHeuristic textDir,
                int strategy, int frequency) {
            if (Build.VERSION.SDK_INT >= 29) {
                mWrapped = new PrecomputedText.Params.Builder(paint)
                        .setBreakStrategy(strategy)
                        .setHyphenationFrequency(frequency)
                        .setTextDirection(textDir)
                        .build();
            } else {
                mWrapped = null;
            }
            mPaint = paint;
            mTextDir = textDir;
            mBreakStrategy = strategy;
            mHyphenationFrequency = frequency;
        }

        @RequiresApi(28)
        public Params(PrecomputedText.@NonNull Params wrapped) {
            mPaint = wrapped.getTextPaint();
            mTextDir = wrapped.getTextDirection();
            mBreakStrategy = wrapped.getBreakStrategy();
            mHyphenationFrequency = wrapped.getHyphenationFrequency();
            mWrapped = (Build.VERSION.SDK_INT >= 29) ? wrapped : null;
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
         * @return the {@link TextDirectionHeuristic}
         */
        public @Nullable TextDirectionHeuristic getTextDirection() {
            return mTextDir;
        }

        /**
         * Returns the break strategy for this text.
         *
         * @return the line break strategy
         */
        public int getBreakStrategy() {
            return mBreakStrategy;
        }

        /**
         * Returns the hyphenation frequency for this text.
         *
         * @return the hyphenation frequency
         */
        public int getHyphenationFrequency() {
            return mHyphenationFrequency;
        }


        /**
         * Similar to equals but don't compare text direction
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public boolean equalsWithoutTextDirection(@NonNull Params other) {
            if (mBreakStrategy != other.getBreakStrategy()) {
                return false;
            }
            if (mHyphenationFrequency != other.getHyphenationFrequency()) {
                return false;
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
            if (mPaint.getLetterSpacing() != other.getTextPaint().getLetterSpacing()) {
                return false;
            }
            if (!TextUtils.equals(mPaint.getFontFeatureSettings(),
                    other.getTextPaint().getFontFeatureSettings())) {
                return false;
            }
            if (mPaint.getFlags() != other.getTextPaint().getFlags()) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (!mPaint.getTextLocales().equals(other.getTextPaint().getTextLocales())) {
                    return false;
                }
            } else {
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
            if (!(o instanceof Params)) {
                return false;
            }
            Params other = (Params) o;
            if (!equalsWithoutTextDirection(other)) {
                return false;
            }
            return mTextDir == other.getTextDirection();
        }

        @Override
        public int hashCode() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getLetterSpacing(), mPaint.getFlags(),
                        mPaint.getTextLocales(), mPaint.getTypeface(), mPaint.isElegantTextHeight(),
                        mTextDir, mBreakStrategy, mHyphenationFrequency);
            } else {
                return ObjectsCompat.hash(mPaint.getTextSize(), mPaint.getTextScaleX(),
                        mPaint.getTextSkewX(), mPaint.getLetterSpacing(), mPaint.getFlags(),
                        mPaint.getTextLocale(), mPaint.getTypeface(), mPaint.isElegantTextHeight(),
                        mTextDir, mBreakStrategy, mHyphenationFrequency);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("textSize=" + mPaint.getTextSize());
            sb.append(", textScaleX=" + mPaint.getTextScaleX());
            sb.append(", textSkewX=" + mPaint.getTextSkewX());
            sb.append(", letterSpacing=" + mPaint.getLetterSpacing());
            sb.append(", elegantTextHeight=" + mPaint.isElegantTextHeight());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sb.append(", textLocale=" + mPaint.getTextLocales());
            } else {
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
    private final int @NonNull [] mParagraphEnds;

    // null on API 27 or before. Non-null on API 29 or later
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
    @SuppressLint("WrongConstant")
    public static PrecomputedTextCompat create(@NonNull CharSequence text, @NonNull Params params) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(params);

        try {
            Trace.beginSection("PrecomputedText");

            if (Build.VERSION.SDK_INT >= 29 && params.mWrapped != null) {
                return new PrecomputedTextCompat(
                        PrecomputedText.create(text, params.mWrapped), params);
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
            StaticLayout.Builder.obtain(text, 0, text.length(), params.getTextPaint(),
                    Integer.MAX_VALUE)
                    .setBreakStrategy(params.getBreakStrategy())
                    .setHyphenationFrequency(params.getHyphenationFrequency())
                    .setTextDirection(params.getTextDirection())
                    .build();

            return new PrecomputedTextCompat(text, params, result);
        } finally {
            Trace.endSection();
        }
    }

    // Use PrecomputedText.create instead.
    private PrecomputedTextCompat(@NonNull CharSequence text, @NonNull Params params,
            int @NonNull [] paraEnds) {
        mText = new SpannableString(text);
        mParams = params;
        mParagraphEnds = paraEnds;
        mWrapped = null;
    }

    @RequiresApi(28)
    private PrecomputedTextCompat(@NonNull PrecomputedText precomputed, @NonNull Params params) {
        mText = Api28Impl.castToSpannable(precomputed);
        mParams = params;
        mParagraphEnds = null;
        mWrapped = (Build.VERSION.SDK_INT >= 29) ? precomputed : null;
    }

    /**
     * Returns the underlying original text if the text is PrecomputedText.
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @RequiresApi(28)
    public @Nullable PrecomputedText getPrecomputedText() {
        if (mText instanceof PrecomputedText) {
            return (PrecomputedText) mText;
        } else {
            return null;
        }
    }

    /**
     * Returns the parameters used to measure this text.
     */
    public @NonNull Params getParams() {
        return mParams;
    }

    /**
     * Returns the count of paragraphs.
     */
    public @IntRange(from = 0) int getParagraphCount() {
        if (Build.VERSION.SDK_INT >= 29) {
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
        if (Build.VERSION.SDK_INT >= 29) {
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
        if (Build.VERSION.SDK_INT >= 29) {
            return mWrapped.getParagraphEnd(paraIndex);
        } else {
            return mParagraphEnds[paraIndex];
        }
    }

    /**
     * A helper class for computing text layout in background
     */
    private static class PrecomputedTextFutureTask extends FutureTask<PrecomputedTextCompat> {
        private static class PrecomputedTextCallback implements Callable<PrecomputedTextCompat> {
            private PrecomputedTextCompat.Params mParams;
            private CharSequence mText;

            PrecomputedTextCallback(final PrecomputedTextCompat.@NonNull Params params,
                    final @NonNull CharSequence cs) {
                mParams = params;
                mText = cs;
            }

            @Override
            public PrecomputedTextCompat call() throws Exception {
                return PrecomputedTextCompat.create(mText, mParams);
            }
        }

        PrecomputedTextFutureTask(final PrecomputedTextCompat.@NonNull Params params,
                final @NonNull CharSequence text) {
            super(new PrecomputedTextCallback(params, text));
        }
    }

    /**
     * Helper for PrecomputedText that returns a future to be used with
     * {@link androidx.appcompat.widget.AppCompatTextView#setTextFuture}.
     *
     * PrecomputedText is suited to compute on a background thread, but when TextView properties are
     * dynamic, it's common to configure text properties and text at the same time, when binding a
     * View. For example, in a RecyclerView Adapter:
     * <pre>
     *     void onBindViewHolder(ViewHolder vh, int position) {
     *         ItemData data = getData(position);
     *
     *         vh.textView.setTextSize(...);
     *         vh.textView.setFontVariationSettings(...);
     *         vh.textView.setText(data.text);
     *     }
     * </pre>
     * In such cases, using PrecomputedText is difficult, since it isn't safe to defer the setText()
     * code arbitrarily - a layout pass may happen before computation finishes, and will be
     * incorrect if the text isn't ready yet.
     * <p>
     * With {@code getTextFuture()}, you can block on the result of the precomputation safely
     * before the result is needed. AppCompatTextView provides
     * {@link androidx.appcompat.widget.AppCompatTextView#setTextFuture} for exactly this
     * use case. With the following code, the app's layout work is largely done on a background
     * thread:
     * <pre>
     *     void onBindViewHolder(ViewHolder vh, int position) {
     *         ItemData data = getData(position);
     *
     *         vh.textView.setTextSize(...);
     *         vh.textView.setFontVariationSettings(...);
     *
     *         // start precompute
     *         Future<PrecomputedTextCompat> future = PrecomputedTextCompat.getTextFuture(
     *                 data.text, vh.textView.getTextMetricsParamsCompat(), myExecutor);
     *
     *         // and pass future to TextView, which awaits result before measuring
     *         vh.textView.setTextFuture(future);
     *     }
     * </pre>
     * Because RecyclerView
     * {@link androidx.recyclerview.widget.RecyclerView.LayoutManager#isItemPrefetchEnabled
     * prefetches} bind multiple frames in advance while scrolling, the text work generally has
     * plenty of time to complete before measurement occurs.
     * </p>
     * <p class="note">
     *     <strong>Note:</strong> all TextView layout properties must be set before creating the
     *     Params object. If they are changed during the precomputation, this can cause a
     *     {@link IllegalArgumentException} when the precomputed value is consumed during measure,
     *     and doesn't reflect the TextView's current state.
     * </p>
     * @param charSequence the text to be displayed
     * @param params the parameters to be used for displaying text
     * @param executor the executor to be process the text layout. If null is passed, the default
     *                single threaded pool will be used.
     * @return a future of the precomputed text
     *
     * @see androidx.appcompat.widget.AppCompatTextView#setTextFuture
     */
    @UiThread
    public static Future<PrecomputedTextCompat> getTextFuture(
            final @NonNull CharSequence charSequence, PrecomputedTextCompat.@NonNull Params params,
            @Nullable Executor executor) {
        PrecomputedTextFutureTask task = new PrecomputedTextFutureTask(params, charSequence);
        if (executor == null) {
            synchronized (sLock) {
                if (sExecutor == null) {
                    sExecutor = Executors.newFixedThreadPool(1);
                }
                executor = sExecutor;
            }
        }
        executor.execute(task);
        return task;
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
        if (Build.VERSION.SDK_INT >= 29) {
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
        if (Build.VERSION.SDK_INT >= 29) {
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
        if (Build.VERSION.SDK_INT >= 29) {
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
    public @NonNull String toString() {
        return mText.toString();
    }

    @RequiresApi(28)
    static class Api28Impl {
        private Api28Impl() {
            // This class is not instantiable.
        }

        static Spannable castToSpannable(PrecomputedText precomputedText) {
            return precomputedText;
        }
    }
}
