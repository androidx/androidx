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

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.IntRange;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * EmojiSpan subclass used to render emojis using Typeface.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TypefaceEmojiSpan extends EmojiSpan {

    /**
     * Paint object used to draw a background in debug mode.
     */
    private static @Nullable Paint sDebugPaint;
    private @Nullable TextPaint mWorkingPaint;

    /**
     * Default constructor.
     *
     * @param metadata metadata representing the emoji that this span will draw
     */
    public TypefaceEmojiSpan(final @NonNull TypefaceEmojiRasterizer metadata) {
        super(metadata);
    }

    @Override
    public void draw(final @NonNull Canvas canvas,
            @SuppressLint("UnknownNullness") final CharSequence text,
            @IntRange(from = 0) final int start, @IntRange(from = 0) final int end, final float x,
            final int top, final int y, final int bottom, final @NonNull Paint paint) {
        TextPaint textPaint = applyCharacterSpanStyles(text, start, end, paint);
        if (textPaint != null && textPaint.bgColor != 0) {
            drawBackground(canvas, textPaint, x, x + getWidth(), top, bottom);
        }
        if (EmojiCompat.get().isEmojiSpanIndicatorEnabled()) {
            canvas.drawRect(x, top , x + getWidth(), bottom, getDebugPaint());
        }
        getTypefaceRasterizer().draw(canvas, x, y, textPaint != null ? textPaint : paint);
    }

    // compat behavior with TextLine.java#handleText background drawing
    void drawBackground(Canvas c, TextPaint textPaint, float leftX, float rightX, float top,
            float bottom) {
        int previousColor = textPaint.getColor();
        Paint.Style previousStyle = textPaint.getStyle();

        textPaint.setColor(textPaint.bgColor);
        textPaint.setStyle(Paint.Style.FILL);
        c.drawRect(leftX, top, rightX, bottom, textPaint);

        textPaint.setStyle(previousStyle);
        textPaint.setColor(previousColor);
    }

    /**
     * This applies the CharacterSpanStyles that _would_ have been applied to this character by
     * StaticLayout.
     *
     * StaticLayout applies CharacterSpanStyles _after_ calling ReplacementSpan.draw, which means
     * BackgroundSpan will not be applied before draw is called.
     *
     * If any CharacterSpanStyles would impact _this_ location, apply them to a TextPaint to
     * determine if a background needs draw prior to the emoji.
     *
     * @param text text that this span is part of
     * @param start start position to replace
     * @param end end position to replace
     * @param paint paint (from TextLine)
     * @return TextPaint configured
     */
    private @Nullable TextPaint applyCharacterSpanStyles(@Nullable CharSequence text, int start,
            int end, Paint paint) {
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            CharacterStyle[] spans = spanned.getSpans(start, end, CharacterStyle.class);
            if (spans.length == 0 || (spans.length == 1 && spans[0] == this)) {
                if (paint instanceof TextPaint) {
                    // happy path goes here, retain color and bgColor from caller
                    return (TextPaint) paint;
                } else {
                    return null;
                }
            }
            // there are some non-TypefaceEmojiSpan character styles to apply, update a working
            // paint to apply each span style, like TextLine would have.
            TextPaint wp = mWorkingPaint;
            if (wp == null) {
                wp = new TextPaint();
                mWorkingPaint = wp;
            }
            wp.set(paint);
            //noinspection ForLoopReplaceableByForEach
            for (int pos = 0; pos < spans.length; pos++) {
                if (!(spans[pos] instanceof MetricAffectingSpan)) {
                    // we're in draw, so at this point we can't do anything to metrics don't try
                    spans[pos].updateDrawState(wp);
                }
            }
            return wp;
        } else {
            if (paint instanceof TextPaint) {
                // retain any color and bgColor from caller
                return (TextPaint) paint;
            } else {
                return null;
            }
        }

    }

    private static @NonNull Paint getDebugPaint() {
        if (sDebugPaint == null) {
            sDebugPaint = new TextPaint();
            sDebugPaint.setColor(EmojiCompat.get().getEmojiSpanIndicatorColor());
            sDebugPaint.setStyle(Paint.Style.FILL);
        }
        return sDebugPaint;
    }


}
