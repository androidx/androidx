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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 * EmojiSpan subclass used to render emojis using Typeface.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(19)
public final class TypefaceEmojiSpan extends EmojiSpan {

    /**
     * Paint object used to draw a background in debug mode.
     */
    private static Paint sDebugPaint;

    /**
     * Default constructor.
     *
     * @param metadata metadata representing the emoji that this span will draw
     */
    public TypefaceEmojiSpan(final EmojiMetadata metadata) {
        super(metadata);
    }

    @Override
    public void draw(@NonNull final Canvas canvas, final CharSequence text,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end, final float x,
            final int top, final int y, final int bottom, @NonNull final Paint paint) {
        if (EmojiCompat.get().isEmojiSpanIndicatorEnabled()) {
            canvas.drawRect(x, top , x + getWidth(), bottom, getDebugPaint());
        }
        getMetadata().draw(canvas, x, y, paint);
    }

    private static Paint getDebugPaint() {
        if (sDebugPaint == null) {
            sDebugPaint = new TextPaint();
            sDebugPaint.setColor(EmojiCompat.get().getEmojiSpanIndicatorColor());
            sDebugPaint.setStyle(Paint.Style.FILL);
        }
        return sDebugPaint;
    }


}
