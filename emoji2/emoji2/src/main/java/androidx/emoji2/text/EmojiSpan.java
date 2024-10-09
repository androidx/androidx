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
package androidx.emoji2.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Base span class for the emoji replacement. When an emoji is found and needs to be replaced in a
 * CharSequence, an instance of this class is added to the CharSequence.
 */
public abstract class EmojiSpan extends ReplacementSpan {

    /**
     * Temporary object to calculate the size of the span.
     */
    private final Paint.FontMetricsInt mTmpFontMetrics = new Paint.FontMetricsInt();

    /**
     * Information about emoji. This is not parcelled since we do not want multiple objects
     * representing same emoji to be in memory. When unparcelled, EmojiSpan tries to set it back
     * using the singleton EmojiCompat instance.
     */
    private final @NonNull TypefaceEmojiRasterizer mRasterizer;

    /**
     * Cached width of the span. Width is calculated according to the font metrics.
     */
    private short mWidth = -1;

    /**
     * Cached height of the span. Height is calculated according to the font metrics.
     */
    private short mHeight = -1;

    /**
     * Cached ratio of current font height to emoji image height.
     */
    private float mRatio = 1.0f;

    /**
     * Default constructor.
     *
     * @param rasterizer information about the emoji, cannot be {@code null}
     *
     */
    @RestrictTo(LIBRARY)
    EmojiSpan(final @NonNull TypefaceEmojiRasterizer rasterizer) {
        Preconditions.checkNotNull(rasterizer, "rasterizer cannot be null");
        mRasterizer = rasterizer;
    }

    @Override
    public int getSize(final @NonNull Paint paint,
            @SuppressLint("UnknownNullness") @SuppressWarnings("MissingNullability")
            final CharSequence text,
            final int start,
            final int end,
            final Paint.@Nullable FontMetricsInt fm) {
        paint.getFontMetricsInt(mTmpFontMetrics);
        final int fontHeight = Math.abs(mTmpFontMetrics.descent - mTmpFontMetrics.ascent);

        mRatio = fontHeight * 1.0f / mRasterizer.getHeight();
        mHeight = (short) (mRasterizer.getHeight() * mRatio);
        mWidth = (short) (mRasterizer.getWidth() * mRatio);

        if (fm != null) {
            fm.ascent = mTmpFontMetrics.ascent;
            fm.descent = mTmpFontMetrics.descent;
            fm.top = mTmpFontMetrics.top;
            fm.bottom = mTmpFontMetrics.bottom;
        }

        return mWidth;
    }

    /**
     * Get the rasterizer that draws this emoji.
     *
     * @return rasterizer to draw emoji
     */
    public final @NonNull TypefaceEmojiRasterizer getTypefaceRasterizer() {
        return mRasterizer;
    }

    /**
     * @return width of the span
     *
     */
    @RestrictTo(LIBRARY)
    final int getWidth() {
        return mWidth;
    }

    /**
     * @return height of the span
     *
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public final int getHeight() {
        return mHeight;
    }

    /**
     */
    @RestrictTo(LIBRARY)
    final float getRatio() {
        return mRatio;
    }

    /**
     * @return unique id for the emoji that this EmojiSpan is used for
     *
     */
    @RestrictTo(LIBRARY)
    @VisibleForTesting
    public final int getId() {
        return getTypefaceRasterizer().getId();
    }
}
