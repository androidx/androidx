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

package androidx.text;

import static androidx.text.LayoutCompat.DEFAULT_ADD_LAST_LINE_LINE_SPCAING;
import static androidx.text.LayoutCompat.DEFAULT_BREAK_STRATEGY;
import static androidx.text.LayoutCompat.DEFAULT_FALLBACK_LINE_SPACING;
import static androidx.text.LayoutCompat.DEFAULT_HYPHENATION_FREQUENCY;
import static androidx.text.LayoutCompat.DEFAULT_INCLUDE_PADDING;
import static androidx.text.LayoutCompat.DEFAULT_JUSTIFICATION_MODE;
import static androidx.text.LayoutCompat.DEFAULT_LAYOUT_ALIGNMENT;
import static androidx.text.LayoutCompat.DEFAULT_LINESPACING_EXTRA;
import static androidx.text.LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER;
import static androidx.text.LayoutCompat.DEFAULT_MAX_LINES;
import static androidx.text.LayoutCompat.DEFAULT_TEXT_DIRECTION_HEURISTIC;

import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.text.LayoutCompat.BreakStrategy;
import androidx.text.LayoutCompat.HyphenationFrequency;
import androidx.text.LayoutCompat.JustificationMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StaticLayoutCompat {
    private StaticLayoutCompat() {}

    /**
     * Builder class for StaticLayout.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class Builder {
        private static final String TAG = "StaticLayoutCompat";
        private static boolean sIsInitialized = false;
        private static Constructor sStaticLayoutConstructor = null;

        private static void obtainStaticLayoutConstructor() {
            if (sIsInitialized) return;
            sIsInitialized = true;
            try {
                sStaticLayoutConstructor = StaticLayout.class.getConstructor(
                        CharSequence.class,
                        int.class, /* start */
                        int.class, /* end */
                        TextPaint.class,
                        int.class, /* width */
                        Layout.Alignment.class,
                        TextDirectionHeuristic.class,
                        float.class, /* lineSpacingMultiplier */
                        float.class, /* lineSpacingExtra */
                        boolean.class, /* includePadding */
                        TextUtils.TruncateAt.class,
                        int.class, /* ellipsizeWidth */
                        int.class /* maxLines */);
            } catch (NoSuchMethodException e) {
                sStaticLayoutConstructor = null;
                Log.e(TAG, "unable to collect necessary constructor.");
            }
        }

        @NonNull
        private CharSequence mText;

        @IntRange(from = 0)
        private int mStart;

        @IntRange(from = 0)
        private int mEnd;

        @NonNull
        private TextPaint mPaint;

        @IntRange(from = 0)
        private int mWidth;

        @NonNull
        private Layout.Alignment mAlignment;

        @NonNull
        private TextDirectionHeuristic mTextDir;

        private float mLineSpacingExtra;

        @FloatRange(from = 0.0f)
        float mLineSpacingMultiplier;

        boolean mIncludePadding;

        boolean mFallbackLineSpacing;

        @Nullable
        TextUtils.TruncateAt mEllipsize;

        @IntRange(from = 0)
        int mEllipsizedWidth;

        @IntRange(from = 0)
        int mMaxLines;

        @BreakStrategy
        int mBreakStrategy;

        @HyphenationFrequency
        int mHyphenationFrequency;

        @JustificationMode
        int mJustificationMode;

        @Nullable
        int[] mLeftIndents;

        @Nullable
        int[] mRightIndents;

        boolean mAddLastLineLineSpacing;

        /**
         * Builder for StaticLayout.
         *
         * @param text The text to be laid out, optionally with spans
         * @param paint The base paint used for layout
         * @param width The width in pixels
         */
        public Builder(
                @NonNull CharSequence text,
                @NonNull TextPaint paint,
                @IntRange(from = 0) int width) {
            mText = Preconditions.checkNotNull(text, "Text can't be null");
            mStart = 0;
            mEnd = mText.length();
            mPaint = Preconditions.checkNotNull(paint, "Paint can't be null");
            mWidth = Preconditions.checkArgumentNonnegative(width, "Width can't be negative");
            mAlignment = DEFAULT_LAYOUT_ALIGNMENT;
            mTextDir = DEFAULT_TEXT_DIRECTION_HEURISTIC;
            mLineSpacingMultiplier = DEFAULT_LINESPACING_MULTIPLIER;
            mLineSpacingExtra = DEFAULT_LINESPACING_EXTRA;
            mIncludePadding = DEFAULT_INCLUDE_PADDING;
            mFallbackLineSpacing = DEFAULT_FALLBACK_LINE_SPACING;
            mEllipsize = null;
            mEllipsizedWidth = width;
            mMaxLines = DEFAULT_MAX_LINES;
            mBreakStrategy = DEFAULT_BREAK_STRATEGY;
            mHyphenationFrequency = DEFAULT_HYPHENATION_FREQUENCY;
            mJustificationMode = DEFAULT_JUSTIFICATION_MODE;
            mAddLastLineLineSpacing = DEFAULT_ADD_LAST_LINE_LINE_SPCAING;
        }

        /**
         * Set the text. Only useful when re-using the builder.
         *
         * @param text  The text to be laid out, optionally with spans
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setText(@NonNull CharSequence text) {
            Preconditions.checkNotNull(text, "Text can't be null");
            setText(text, 0, text.length());
            return this;
        }

        /**
         * Set the text. Only useful when re-using the builder.
         *
         * @param text  The text to be laid out, optionally with spans
         * @param start The index of the start of the text
         * @param end   The index + 1 of the end of the text
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setText(@NonNull CharSequence text,
                @IntRange(from = 0) int start, @IntRange(from = 0) int end) {
            mText = Preconditions.checkNotNull(text, "Text can't be null");
            mStart = Preconditions.checkArgumentInRange(start, 0, text.length(), "Start");
            mEnd = Preconditions.checkArgumentInRange(end, 0, text.length(), "End");
            return this;
        }

        /**
         * Set the start of the text. Should be in range of [0, end],
         * which is checked in {@link Builder#build}
         *
         * @param start The index of the start of the text
         * @return this builder, useful for chaining
         */
        public Builder setStart(@IntRange(from = 0) int start) {
            mStart = Preconditions.checkArgumentNonnegative(start, "Start can't be negative");
            return this;
        }

        /**
         * Set the end of the text. Should be in range of [start, text.length],
         * which is checked in {@link Builder#build}.
         *
         * @param end The index + 1 of the end of the text.
         * @return this builder, useful for chaining
         */
        public Builder setEnd(@IntRange(from = 0) int end) {
            mEnd = Preconditions.checkArgumentNonnegative(end, "End can't be negative");
            return this;
        }

        /**
         * Set the paint. Internal for reuse cases only.
         *
         * @param paint The base paint used for layout
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setPaint(@NonNull TextPaint paint) {
            mPaint = Preconditions.checkNotNull(paint, "Paint can't be null");
            return this;
        }

        /**
         * Set the width. Internal for reuse cases only.
         *
         * @param width The width in pixels
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setWidth(@IntRange(from = 0) int width) {
            mWidth = Preconditions.checkArgumentNonnegative(width, "Width can't be negative");
            return this;
        }

        /**
         * Set the alignment. The default is {@link Layout.Alignment#ALIGN_NORMAL}.
         *
         * @param alignment Alignment for the resulting {@link StaticLayout}
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setAlignment(@NonNull Layout.Alignment alignment) {
            mAlignment = Preconditions.checkNotNull(alignment, "Alignment can't be null");
            return this;
        }

        /**
         * Set the text direction heuristic. The text direction heuristic is used to
         * resolve text direction per-paragraph based on the input text. The default is
         * {@link TextDirectionHeuristics#FIRSTSTRONG_LTR}.
         *
         * @param textDir text direction heuristic for resolving bidi behavior.
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setTextDirection(@NonNull TextDirectionHeuristic textDir) {
            mTextDir = Preconditions.checkNotNull(textDir, "TextDir can't be null");
            return this;
        }

        /**
         * Set line spacing extra. Each line will have its line spacing increased by
         * {@code lineSpacingExtra}. The default value is 0.0f.
         *
         * @param lineSpacingExtra the amount of line spacing addition
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setLineSpacing
         */
        @NonNull
        public Builder setLineSpacingExtra(float lineSpacingExtra) {
            mLineSpacingExtra = lineSpacingExtra;
            return this;
        }

        /**
         * Set line spacing multiplier. Each line will have its line spacing multiplied by
         * {@code lineSpacingMultiplier}. The default value is 1.0f, and lineSpacingMultiplier
         * should be non-negative.
         *
         * @param lineSpacingMultiplier the line spacing multiplier
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setLineSpacing
         */
        @NonNull
        public Builder setLineSpacingMultiplier(
                @FloatRange(from = 0.0f) float lineSpacingMultiplier) {
            mLineSpacingMultiplier = Preconditions.checkArgumentInRange(lineSpacingMultiplier,
                    0.0f, Float.MAX_VALUE, "LineSpacingMultiplier");
            return this;
        }

        /**
         * Set whether to include extra space beyond font ascent and descent (which is
         * needed to avoid clipping in some languages, such as Arabic and Kannada). The
         * default is {@code true}.
         *
         * @param includePad whether to include padding
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setIncludeFontPadding
         */
        @NonNull
        public Builder setIncludePad(boolean includePad) {
            mIncludePadding = includePad;
            return this;
        }

        /**
         * Set whether to respect the ascent and descent of the fallback fonts that are used in
         * displaying the text (which is needed to avoid text from consecutive lines running into
         * each other). If set, fallback fonts that end up getting used can increase the ascent
         * and descent of the lines that they are used on.
         *
         * <p>For backward compatibility reasons, the default is {@code false}, but setting this to
         * true is strongly recommended. It is required to be true if text could be in languages
         * like Burmese or Tibetan where text is typically much taller or deeper than Latin text.
         *
         * @param useLineSpacingFromFallbacks whether to expand linespacing based on fallback fonts
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setUseLineSpacingFromFallbacks(boolean useLineSpacingFromFallbacks) {
            mFallbackLineSpacing = useLineSpacingFromFallbacks;
            return this;
        }

        /**
         * Set ellipsizing on the layout. Causes words that are longer than the view
         * is wide, or exceeding the number of lines (see #setMaxLines) in the case
         * of {@link android.text.TextUtils.TruncateAt#END} or
         * {@link android.text.TextUtils.TruncateAt#MARQUEE}, to be ellipsized instead
         * of broken. The default is {@code null}, indicating no ellipsis is to be applied.
         *
         * @param ellipsize type of ellipsis behavior
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setEllipsize
         */
        @NonNull
        public Builder setEllipsize(@Nullable TextUtils.TruncateAt ellipsize) {
            mEllipsize = ellipsize;
            return this;
        }

        /**
         * Set the width as used for ellipsizing purposes, if it differs from the
         * normal layout width. The default is the {@code width}
         * passed to {@link #Builder(CharSequence, TextPaint, int)}.
         *
         * @param ellipsizedWidth width used for ellipsizing, in pixels
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setEllipsize
         */
        @NonNull
        public Builder setEllipsizedWidth(@IntRange(from = 0) int ellipsizedWidth) {
            mEllipsizedWidth = Preconditions.checkArgumentNonnegative(ellipsizedWidth,
                    "EllipsizedWidth can't be negative");
            return this;
        }

        /**
         * Set maximum number of lines. This is particularly useful in the case of
         * ellipsizing, where it changes the layout of the last line. The default is
         * unlimited.
         *
         * @param maxLines maximum number of lines in the layout
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setMaxLines
         */
        @NonNull
        public Builder setMaxLines(@IntRange(from = 0) int maxLines) {
            mMaxLines = Preconditions.checkArgumentNonnegative(maxLines,
                    "MaxLines can't be negative");
            return this;
        }

        /**
         * Set break strategy, useful for selecting high quality or balanced paragraph
         * layout options. The default is {@link Layout#BREAK_STRATEGY_SIMPLE}.
         * <p/>
         * Enabling hyphenation with either using {@link Layout#HYPHENATION_FREQUENCY_NORMAL} or
         * {@link Layout#HYPHENATION_FREQUENCY_FULL} while line breaking is set to one of
         * {@link Layout#BREAK_STRATEGY_BALANCED}, {@link Layout#BREAK_STRATEGY_HIGH_QUALITY}
         * improves the structure of text layout however has performance impact and requires more
         * time to do the text layout.
         *
         * @param breakStrategy break strategy for paragraph layout
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setBreakStrategy
         * @see #setHyphenationFrequency(int)
         */
        @NonNull
        public Builder setBreakStrategy(@BreakStrategy int breakStrategy) {
            mBreakStrategy = breakStrategy;
            return this;
        }

        /**
         * Set hyphenation frequency, to control the amount of automatic hyphenation used. The
         * possible values are defined in {@link Layout}, by constants named with the pattern
         * {@code HYPHENATION_FREQUENCY_*}. The default is
         * {@link Layout#HYPHENATION_FREQUENCY_NONE}.
         * <p/>
         * Enabling hyphenation with either using {@link Layout#HYPHENATION_FREQUENCY_NORMAL} or
         * {@link Layout#HYPHENATION_FREQUENCY_FULL} while line breaking is set to one of
         * {@link Layout#BREAK_STRATEGY_BALANCED}, {@link Layout#BREAK_STRATEGY_HIGH_QUALITY}
         * improves the structure of text layout however has performance impact and requires more
         * time to do the text layout.
         *
         * @param hyphenationFrequency hyphenation frequency for the paragraph
         * @return this builder, useful for chaining
         * @see android.widget.TextView#setHyphenationFrequency
         * @see #setBreakStrategy(int)
         */
        @NonNull
        public Builder setHyphenationFrequency(@HyphenationFrequency int hyphenationFrequency) {
            mHyphenationFrequency = hyphenationFrequency;
            return this;
        }

        /**
         * Set indents. Arguments are arrays holding an indent amount, one per line, measured in
         * pixels. For lines past the last element in the array, the last element repeats.
         *
         * @param leftIndents  array of indent values for left margin, in pixels
         * @param rightIndents array of indent values for right margin, in pixels
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setIndents(@Nullable int[] leftIndents, @Nullable int[] rightIndents) {
            mLeftIndents = leftIndents;
            mRightIndents = rightIndents;
            return this;
        }

        /**
         * Set paragraph justification mode. The default value is
         * {@link Layout#JUSTIFICATION_MODE_NONE}. If the last line is too short for justification,
         * the last line will be displayed with the alignment set by {@link #setAlignment}.
         *
         * @param justificationMode justification mode for the paragraph.
         * @return this builder, useful for chaining.
         */
        @NonNull
        public Builder setJustificationMode(@JustificationMode int justificationMode) {
            mJustificationMode = justificationMode;
            return this;
        }

        /**
         * Sets whether the line spacing should be applied for the last line. Default value is
         * {@code false}.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setAddLastLineLineSpacing(boolean value) {
            mAddLastLineLineSpacing = value;
            return this;
        }

        /**
         * Build the {@link StaticLayout} after options have been set.
         *
         * @return the newly constructed {@link StaticLayout} object
         */
        @NonNull
        public StaticLayout build() {
            Preconditions.checkArgumentInRange(mEnd, 0, mText.length(), "End");
            Preconditions.checkArgumentInRange(mStart, 0, mEnd, "Start");

            if (Build.VERSION.SDK_INT >= 23) {
                return createWithBuilder();
            }
            StaticLayout layout = createWithReflection();
            if (layout != null) return layout;
            return createWithConstructor();
        }

        @RequiresApi(23)
        @NonNull
        private StaticLayout createWithBuilder() {
            StaticLayout.Builder builder = StaticLayout.Builder.obtain(
                    mText,
                    mStart,
                    mEnd,
                    mPaint,
                    mWidth)
                    .setAlignment(mAlignment)
                    .setTextDirection(mTextDir)
                    .setLineSpacing(mLineSpacingExtra, mLineSpacingMultiplier)
                    .setIncludePad(mIncludePadding)
                    .setEllipsize(mEllipsize)
                    .setEllipsizedWidth(mEllipsizedWidth)
                    .setMaxLines(mMaxLines)
                    .setBreakStrategy(mBreakStrategy)
                    .setHyphenationFrequency(mHyphenationFrequency)
                    .setIndents(mLeftIndents, mRightIndents);

            if (Build.VERSION.SDK_INT >= 26) {
                builder.setJustificationMode(mJustificationMode);
            }
//            if (Build.VERSION.SDK_INT >= 28) {
//                    TODO(Migration/siyamed): last line spacing is required for editable text,
//                    otherwise we will need tricks
//                    builder.setAddLastLineLineSpacing(builder.mAddLastLineLineSpacing);
//                    builder.setUseLineSpacingFromFallbacks(mFallbackLineSpacing);
//            }
            return builder.build();
        }

        @Nullable
        private StaticLayout createWithReflection() {
            obtainStaticLayoutConstructor();
            if (sStaticLayoutConstructor != null) {
                try {
                    return (StaticLayout) sStaticLayoutConstructor.newInstance(
                            mText,
                            mStart,
                            mEnd,
                            mPaint,
                            mWidth,
                            mAlignment,
                            mTextDir,
                            mLineSpacingMultiplier,
                            mLineSpacingExtra,
                            mIncludePadding,
                            mEllipsize,
                            mEllipsizedWidth,
                            mMaxLines);

                } catch (IllegalAccessException
                        | InstantiationException
                        | InvocationTargetException e) {
                    sStaticLayoutConstructor = null;
                    Log.e(TAG, "unable to call constructor");
                }
            }
            return null;
        }

        @SuppressWarnings("deprecation")
        @NonNull
        private StaticLayout createWithConstructor() {
            return new StaticLayout(
                    mText,
                    mStart,
                    mEnd,
                    mPaint,
                    mWidth,
                    mAlignment,
                    mLineSpacingMultiplier,
                    mLineSpacingExtra,
                    mIncludePadding,
                    mEllipsize,
                    mEllipsizedWidth);
        }
    }
}
