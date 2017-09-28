/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.support.text.emoji;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.AnyThread;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.text.emoji.flatbuffer.MetadataItem;
import android.support.text.emoji.flatbuffer.MetadataList;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Information about a single emoji.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@AnyThread
@RequiresApi(19)
public class EmojiMetadata {
    /**
     * Defines whether the system can render the emoji.
     */
    @IntDef({HAS_GLYPH_UNKNOWN, HAS_GLYPH_ABSENT, HAS_GLYPH_EXISTS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HasGlyph {
    }

    /**
     * Not calculated on device yet.
     */
    public static final int HAS_GLYPH_UNKNOWN = 0;

    /**
     * Device cannot render the emoji.
     */
    public static final int HAS_GLYPH_ABSENT = 1;

    /**
     * Device can render the emoji.
     */
    public static final int HAS_GLYPH_EXISTS = 2;

    /**
     * @see #getMetadataItem()
     */
    private static final ThreadLocal<MetadataItem> sMetadataItem = new ThreadLocal<>();

    /**
     * Index of the EmojiMetadata in {@link MetadataList}.
     */
    private final int mIndex;

    /**
     * MetadataRepo that holds this instance.
     */
    private final MetadataRepo mMetadataRepo;

    /**
     * Whether the system can render the emoji. Calculated at runtime on the device.
     */
    @HasGlyph
    private volatile int mHasGlyph = HAS_GLYPH_UNKNOWN;

    EmojiMetadata(@NonNull final MetadataRepo metadataRepo, @IntRange(from = 0) final int index) {
        mMetadataRepo = metadataRepo;
        mIndex = index;
    }

    /**
     * Draws the emoji represented by this EmojiMetadata onto a canvas with origin at (x,y), using
     * the specified paint.
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
        // MetadataRepo.getEmojiCharArray() is a continous array of chars that is used to store the
        // chars for emojis. since all emojis are mapped to a single codepoint, and since it is 2
        // chars wide, we assume that the start index of the current emoji is mIndex * 2, and it is
        // 2 chars long.
        final int charArrayStartIndex = mIndex * 2;
        canvas.drawText(mMetadataRepo.getEmojiCharArray(), charArrayStartIndex, 2, x, y, paint);
        paint.setTypeface(oldTypeface);
    }

    /**
     * @return return typeface to be used to render this metadata
     */
    public Typeface getTypeface() {
        return mMetadataRepo.getTypeface();
    }

    /**
     * @return a ThreadLocal instance of MetadataItem for this EmojiMetadata
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
        // to create a wrapper class for each EmojiMetadata, we use mIndex as the index of the
        // MetadataItem in the ByteBuffer. We need to reiniitalize the current thread local instance
        // by executing the statement below. All the statement does is to set an int index in
        // MetadataItem. the same instance is used by all EmojiMetadata classes in the same thread.
        mMetadataRepo.getMetadataList().list(result, mIndex);
        return result;
    }

    /**
     * @return unique id for the emoji
     */
    public int getId() {
        return getMetadataItem().id();
    }

    /**
     * @return width of the emoji image
     */
    public short getWidth() {
        return getMetadataItem().width();
    }

    /**
     * @return height of the emoji image
     */
    public short getHeight() {
        return getMetadataItem().height();
    }

    /**
     * @return in which metadata version the emoji was added to metadata
     */
    public short getCompatAdded() {
        return getMetadataItem().compatAdded();
    }

    /**
     * @return first SDK that the support for this emoji was added
     */
    public short getSdkAdded() {
        return getMetadataItem().sdkAdded();
    }

    /**
     * @return whether the emoji is in Emoji Presentation by default (without emoji
     * style selector 0xFE0F)
     */
    @HasGlyph
    public int getHasGlyph() {
        return mHasGlyph;
    }

    /**
     * Set whether the system can render the emoji.
     *
     * @param hasGlyph {@code true} if system can render the emoji
     */
    public void setHasGlyph(boolean hasGlyph) {
        mHasGlyph = hasGlyph ? HAS_GLYPH_EXISTS : HAS_GLYPH_ABSENT;
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
