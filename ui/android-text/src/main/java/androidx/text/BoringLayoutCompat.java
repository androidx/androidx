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

import static androidx.text.LayoutCompat.DEFAULT_INCLUDE_PADDING;
import static androidx.text.LayoutCompat.DEFAULT_LAYOUT_ALIGNMENT;

import android.text.BoringLayout;
import android.text.Layout;
import android.text.TextDirectionHeuristic;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Factory Class for BoringLayout
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BoringLayoutCompat {
    private BoringLayoutCompat() { }

    /**
     * Builder class for BoringLayout.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static class Builder {
        @NonNull
        private CharSequence mText;

        @NonNull
        private TextPaint mPaint;

        @IntRange(from = 0)
        private int mWidth;

        @NonNull
        private BoringLayout.Metrics mMetrics;

        @NonNull
        private Layout.Alignment mAlignment;

        private boolean mIncludePadding;

        @Nullable
        private TextUtils.TruncateAt mEllipsize;

        @IntRange(from = 0)
        private int mEllipsizedWidth;

        public Builder(
                @NonNull CharSequence text,
                @NonNull TextPaint paint,
                @IntRange(from = 0) int width,
                @NonNull BoringLayout.Metrics metrics) {
            mText = Preconditions.checkNotNull(text, "Text can't be null");
            mPaint = Preconditions.checkNotNull(paint, "Paint can't be null");
            mWidth = Preconditions.checkArgumentNonnegative(width, "Width can't be negative");
            mMetrics = Preconditions.checkNotNull(metrics, "Metrics can't be null");
            mAlignment = DEFAULT_LAYOUT_ALIGNMENT;
            mIncludePadding = DEFAULT_INCLUDE_PADDING;
            mEllipsize = null;
            mEllipsizedWidth = width;
        }

        /**
         * Set the text. Only useful when re-using the builder.
         *
         * @param text The text to be displayed
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setText(@NonNull CharSequence text) {
            Preconditions.checkNotNull(text, "Text can't be null");
            mText = text;
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
         * Set the metric. Internal for reuse cases only.
         *
         * @param metrics the metrics result computed by
         *                {@link #isBoring(CharSequence, TextPaint, TextDirectionHeuristic)}
         * @return this builder, useful for chaining
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setMetrics(@NonNull BoringLayout.Metrics metrics) {
            mMetrics = Preconditions.checkNotNull(metrics);
            return this;
        }

        /**
         * Set the alignment.
         *
         * @param align The width in pixels
         * @return this builder, useful for chaining
         */
        @NonNull
        public Builder setAlignment(@NonNull Layout.Alignment align) {
            mAlignment = Preconditions.checkNotNull(align, "Alignment can't be null");
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
         * Set ellipsizing on the layout. Causes words that are longer than the view
         * is wide, {@link android.text.TextUtils.TruncateAt#MARQUEE}, to be ellipsized instead
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
         * passed to {@link #Builder(CharSequence, TextPaint, int, BoringLayout.Metrics)}.
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
         * Create a BoringLayout with the specified values.
         *
         * @return a BoringLayout created with given values.
         */
        @NonNull
        public BoringLayout build() {
            if (mEllipsize == null) {
                return new BoringLayout(
                        mText,
                        mPaint,
                        mWidth,
                        mAlignment,
                        LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
                        LayoutCompat.DEFAULT_LINESPACING_EXTRA,
                        mMetrics,
                        mIncludePadding
                );
            }
            return new BoringLayout(
                    mText,
                    mPaint,
                    mWidth,
                    mAlignment,
                    LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
                    LayoutCompat.DEFAULT_LINESPACING_EXTRA,
                    mMetrics,
                    mIncludePadding,
                    mEllipsize,
                    mEllipsizedWidth
            );
        }
    }

    /**
     * Check if the text can be laid out by BoringLayout with provided paint and text direction.
     *
     * @param text the text to analyze
     * @param paint TextPaint object
     * @param textDir text direction heuristics
     * @return null if not boring; the width, ascent, and descent in a BoringLayout.Metrics object
     */
    @Nullable
    public static BoringLayout.Metrics isBoring(CharSequence text, TextPaint paint,
            TextDirectionHeuristic textDir) {
        if (!textDir.isRtl(text, 0, text.length())) {
            return BoringLayout.isBoring(text, paint, null /* metrics */);
        }
        return null;
    }
}
