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
package androidx.emoji.text;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;

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
    private final EmojiMetadata mMetadata;

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
     * @param metadata information about the emoji, cannot be {@code null}
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    EmojiSpan(@NonNull final EmojiMetadata metadata) {
        Preconditions.checkNotNull(metadata, "metadata cannot be null");
        mMetadata = metadata;
    }

    @Override
    public int getSize(@NonNull final Paint paint, final CharSequence text, final int start,
            final int end, final Paint.FontMetricsInt fm) {
        paint.getFontMetricsInt(mTmpFontMetrics);
        final int fontHeight = Math.abs(mTmpFontMetrics.descent - mTmpFontMetrics.ascent);

        mRatio = fontHeight * 1.0f / mMetadata.getHeight();
        mHeight = (short) (mMetadata.getHeight() * mRatio);
        mWidth = (short) (mMetadata.getWidth() * mRatio);

        if (fm != null) {
            fm.ascent = mTmpFontMetrics.ascent;
            fm.descent = mTmpFontMetrics.descent;
            fm.top = mTmpFontMetrics.top;
            fm.bottom = mTmpFontMetrics.bottom;
        }

        return mWidth;
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final EmojiMetadata getMetadata() {
        return mMetadata;
    }

    /**
     * @return width of the span
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final int getWidth() {
        return mWidth;
    }

    /**
     * @return height of the span
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final int getHeight() {
        return mHeight;
    }

    /**
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    final float getRatio() {
        return mRatio;
    }

    /**
     * @return unique id for the emoji that this EmojiSpan is used for
     *
     */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    public final int getId() {
        return getMetadata().getId();
    }
}
