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
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

/**
 * EmojiSpan subclass used to render emojis using Typeface.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class TypefaceEmojiSpan extends EmojiSpan {

    /**
     * Default constructor.
     *
     * @param metadata metadata representing the emoji that this span will draw
     */
    public TypefaceEmojiSpan(final EmojiMetadata metadata) {
        super(metadata);
    }

    @Override
    public void draw(@NonNull final Canvas canvas, final CharSequence text, final int start,
            final int end, final float x, final int top, final int y, final int bottom,
            @NonNull final Paint paint) {
        final Typeface typeface = EmojiCompat.get().getTypeface();
        final Typeface oldTypeface = paint.getTypeface();
        paint.setTypeface(typeface);
        getMetadata().draw(canvas, x, y, paint);
        paint.setTypeface(oldTypeface);
    }
}
