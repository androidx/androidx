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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.annotation.AnyThread;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Information about a single emoji.
 *
 * To draw this emoji on a canvas using use draw.
 *
 * To draw this emoji using custom code, use getCodepointAt and getTypeface to access the
 * underlying emoji font and look up the glyph.
 *
 * @see TypefaceEmojiRasterizer#draw
 * @see TypefaceEmojiRasterizer#getCodepointAt
 * @see TypefaceEmojiRasterizer#getTypeface
 *
 */
@AnyThread
@RequiresApi(19)
public class TypefaceEmojiRasterizer {
    /**
     * Defines whether the system can render the emoji.
     * @hide
     */
    @IntDef({HAS_GLYPH_UNKNOWN, HAS_GLYPH_ABSENT, HAS_GLYPH_EXISTS})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY_GROUP)
    public @interface HasGlyph {
    }

    /**
     * Not calculated on device yet.
     * @hide
     */
    @RestrictTo(LIBRARY)
    static final int HAS_GLYPH_UNKNOWN = 0;

    /**
     * Device cannot render the emoji.
     * @hide
     */
    @RestrictTo(LIBRARY)
    static final int HAS_GLYPH_ABSENT = 1;

    /**
     * Device can render the emoji.
     * @hide
     */
    @RestrictTo(LIBRARY)
    static final int HAS_GLYPH_EXISTS = 2;

    /**
     * @see #getMetadataItem()
     */
    private static final ThreadLocal<MetadataItem> sMetadataItem = new ThreadLocal<>();

    /**
     * Index of the TypefaceEmojiRasterizer in {@link MetadataList}.
     */
    private final int mIndex;

    /**
     * MetadataRepo that holds this instance.
     */
    @NonNull
    private final MetadataRepo mMetadataRepo;

    /**
     * Stores hasGlyph as well as exclusion values
     *
     * mCache & 0b0011 is hasGlyph result
     * mCache & 0b0100 is exclusion value
     */
    private volatile int mCache = 0;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    TypefaceEmojiRasterizer(@NonNull final MetadataRepo metadataRepo,
            @IntRange(from = 0) final int index) {
        mMetadataRepo = metadataRepo;
        mIndex = index;
    }

    /**
     * Draws the emoji onto a canvas with origin at (x,y), using the specified paint.
     *
     * @param canvas Canvas to be drawn
     * @param x x-coordinate of the origin of the emoji being drawn
     * @param y y-coordinate of the baseline of the emoji being drawn
     * @param paint Paint used for the text (e.g. color, size, style)
     */
    public void draw(@NonNull final Canvas canvas, final float x, final float y,
            @NonNull final Paint paint) {
        final Typeface typeface = mMetadataRepo.getTypeface();
        final Typeface oldTypeface = paint.getTypeface();
        paint.setTypeface(typeface);
        // MetadataRepo.getEmojiCharArray() is a continuous array of chars that is used to store the
        // chars for emojis. since all emojis are mapped to a single codepoint, and since it is 2
        // chars wide, we assume that the start index of the current emoji is mIndex * 2, and it is
        // 2 chars long.
        final int charArrayStartIndex = mIndex * 2;
        canvas.drawText(mMetadataRepo.getEmojiCharArray(), charArrayStartIndex, 2, x, y, paint);
        paint.setTypeface(oldTypeface);
    }

    /**
     * @return return typeface to be used to render
     */
    @NonNull
    public Typeface getTypeface() {
        return mMetadataRepo.getTypeface();
    }

    /**
     * @return a ThreadLocal instance of MetadataItem for this TypefaceEmojiRasterizer
     */
    private MetadataItem getMetadataItem() {
        MetadataItem result = sMetadataItem.get();
        if (result == null) {
            result = new MetadataItem();
            sMetadataItem.set(result);
        }
        // MetadataList is a wrapper around the metadata ByteBuffer. MetadataItem is a wrapper with
        // an index (pointer) on this ByteBuffer that represents a single emoji. Both are FlatBuffer
        // classes that wraps a ByteBuffer and gives access to the information in it. In order not
        // to create a wrapper class for each TypefaceEmojiRasterizer, we use mIndex as the index
        // of the MetadataItem in the ByteBuffer. We need to reiniitalize the current thread
        // local instance by executing the statement below. All the statement does is to set an
        // int index in MetadataItem. the same instance is used by all TypefaceEmojiRasterizer
        // classes in the same thread.
        mMetadataRepo.getMetadataList().list(result, mIndex);
        return result;
    }

    /**
     * Unique id for the emoji, as loaded from the font file.
     *
     * @return unique id for the emoji
     * @hide
     */
    @RestrictTo(LIBRARY)
    public int getId() {
        return getMetadataItem().id();
    }

    /**
     * @return width of the emoji image
     */
    public int getWidth() {
        return getMetadataItem().width();
    }

    /**
     * @return height of the emoji image
     */
    public int getHeight() {
        return getMetadataItem().height();
    }

    /**
     * @return in which metadata version the emoji was added
     * @hide
     */
    @RestrictTo(LIBRARY)
    public short getCompatAdded() {
        return getMetadataItem().compatAdded();
    }

    /**
     * @return first SDK that the support for this emoji was added
     * @hide
     */
    @RestrictTo(LIBRARY)
    public short getSdkAdded() {
        return getMetadataItem().sdkAdded();
    }

    /**
     * Returns the value set by setHasGlyph
     *
     * This is intended to be used as a cache on this emoji to avoid repeatedly calling
     * PaintCompat#hasGlyph on the same codepoint sequence, which is expensive.
     *
     * @see TypefaceEmojiRasterizer#setHasGlyph
     * @return the set value of hasGlyph for this metadata item
     * @hide
     */
    @HasGlyph
    @SuppressLint("KotlinPropertyAccess")
    @RestrictTo(LIBRARY)
    public int getHasGlyph() {
        return (int) (mCache & 0b0011);
    }

    /**
     * Reset any cached values of hasGlyph on this metadata.
     *
     * This is only useful for testing, and will make the next display of this emoji slower.
     *
     * @hide
     */
    @VisibleForTesting
    public void resetHasGlyphCache() {
        boolean willExclude = isPreferredSystemRender();
        if (willExclude) {
            mCache = 0b0100;
        } else {
            mCache = 0b0000;
        }
    }

    /**
     * Set whether the system can render the emoji.
     *
     * @see PaintCompat#hasGlyph
     * @param hasGlyph {@code true} if system can render the emoji
     * @hide
     */
    @SuppressLint("KotlinPropertyAccess")
    @RestrictTo(LIBRARY)
    public void setHasGlyph(boolean hasGlyph) {
        int newValue = mCache & 0b0100; /* keep the exclusion bit */
        if (hasGlyph) {
            newValue |= 0b0010;
        } else {
            newValue |= 0b0001;
        }
        mCache =  newValue;
    }

    /**
     * If this emoji is excluded due to CodepointExclusions.getExcludedCodpoints()
     *
     * @param exclude if the emoji should never be rendered by emojicompat
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void setExclusion(boolean exclude) {
        int hasGlyphBits = getHasGlyph();
        if (exclude) {
            mCache = hasGlyphBits | 0b0100;
        } else {
            mCache = hasGlyphBits;
        }
    }

    /**
     * If the platform requested that this emoji not be rendered using emojicompat.
     *
     * @return true if this emoji should be drawn by the system instead of this renderer
     */
    public boolean isPreferredSystemRender() {
        return (mCache & 0b0100) > 0;
    }

    /**
     * @return whether the emoji is in Emoji Presentation by default (without emoji
     *         style selector 0xFE0F)
     */
    public boolean isDefaultEmoji() {
        return getMetadataItem().emojiStyle();
    }

    /**
     * @param index index of the codepoint
     *
     * @return the codepoint at index
     */
    public int getCodepointAt(int index) {
        return getMetadataItem().codepoints(index);
    }

    /**
     * @return the length of the codepoints for this emoji
     */
    public int getCodepointsLength() {
        return getMetadataItem().codepointsLength();
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(super.toString());
        builder.append(", id:");
        builder.append(Integer.toHexString(getId()));
        builder.append(", codepoints:");
        final int codepointsLength = getCodepointsLength();
        for (int i = 0; i < codepointsLength; i++) {
            builder.append(Integer.toHexString(getCodepointAt(i)));
            builder.append(" ");
        }
        return builder.toString();
    }
}
