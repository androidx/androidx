/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.text.LineBreaker;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.LocaleSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * Renders text onto a canvas.
 *
 * <p>Watch faces can use TextRenderer to render text on a canvas within specified bounds. This is
 * intended for use when rendering complications. The layout is only recalculated if the text, the
 * bounds, or the drawing properties change, allowing this to be called efficiently on every frame.
 *
 * <p>TextRenderer also ensures that a minimum number of characters are always displayed, by
 * shrinking the text size if necessary to make the characters fit. The default is to always show at
 * least 7 characters, which matches the requirement for the 'short text' fields of complications.
 *
 * <p>To use, instantiate a TextRenderer object and set the {@link TextPaint} that should be used.
 * Only instantiate and set the paint once on each text renderer - you should not do this within the
 * draw method. Each distinct piece of text should have its own TextRenderer - so for example you
 * might need one for the 'short text' field, and one for the 'short title' field:
 *
 * <pre>
 *   mTitleRenderer = new TextRenderer();
 *   mTitleRenderer.setPaint(mTitlePaint);
 *   mTextRenderer = new TextRenderer();
 *   mTextRenderer.setPaint(mTextPaint);</pre>
 *
 * <p>When drawing a frame, set the current text value on the text renderer (using the current time
 * so that any time-dependent text has an up-to-date value), and then call draw on the renderer with
 * the bounds you need:
 *
 * <pre>
 *   // Set the text every time you draw, even if the ComplicationData has not changed, in case
 *   // the text includes a time-dependent value.
 *   mTitleRenderer.setText(
 *       ComplicationText.getText(context, complicationData.getShortTitle(), currentTimeMillis));
 *   mTextRenderer.setText(
 *       ComplicationText.getText(context, complicationData.getShortText(), currentTimeMillis));
 *
 *   // Assuming both the title and text exist.
 *   mTitleRenderer.draw(canvas, titleBounds);
 *   mTextRenderer.draw(canvas, textBounds);</pre>
 *
 * <p>The layout of the text may be customised by calling methods such as {@link #setAlignment},
 * {@link #setGravity}, and {@link #setRelativePadding}. These methods may be called on every frame
 * - the layout will only be recalculated if necessary.
 *
 * <p>If the properties of the TextPaint or content of the text have changed, then the TextRenderer
 * may not be aware that the layout needs to be updated. In this case a layout update should be
 * forced by calling {@link #requestUpdateLayout()}.
 *
 * <p>TextRenderer also hides characters and styling that may not be suitable for display in ambient
 * mode - for example full color emoji.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class TextRenderer {

    private static final String TAG = "TextRenderer";
    private static final int TEXT_SIZE_STEP_SIZE = 1;
    private static final int DEFAULT_MINIMUM_CHARACTERS_SHOWN = 7;
    private static final int SPACE_CHARACTER = 0x20;

    private static final Class<?>[] SPAN_ALLOW_LIST =
            new Class<?>[]{
                    ForegroundColorSpan.class,
                    LocaleSpan.class,
                    SubscriptSpan.class,
                    SuperscriptSpan.class,
                    StrikethroughSpan.class,
                    StyleSpan.class,
                    TypefaceSpan.class,
                    UnderlineSpan.class
            };

    private final Rect mBounds = new Rect();
    private TextPaint mPaint;

    @Nullable private String mAmbientModeText;
    @Nullable private CharSequence mOriginalText;
    @Nullable private CharSequence mText;

    private float mRelativePaddingStart;
    private float mRelativePaddingEnd;
    private float mRelativePaddingTop;
    private float mRelativePaddingBottom;

    private StaticLayout mStaticLayout;

    private int mGravity = Gravity.CENTER;
    private int mMaxLines = 1;
    private int mMinCharactersShown = DEFAULT_MINIMUM_CHARACTERS_SHOWN;
    private TextUtils.TruncateAt mEllipsize = TextUtils.TruncateAt.END;
    private Layout.Alignment mAlignment = Layout.Alignment.ALIGN_CENTER;

    private final Rect mWorkingRect = new Rect();
    private final Rect mOutputRect = new Rect();

    private boolean mInAmbientMode = false;
    private boolean mNeedUpdateLayout;
    private boolean mNeedCalculateBounds;

    /**
     * Draws the text onto the {@code canvas} within the specified {@code bounds}.
     *
     * <p>This will only lay the text out again if the width of the bounds has changed or if the
     * parameters have changed. If the properties of the paint have changed, and {@link #setPaint}
     * has not been called, {@link #requestUpdateLayout} must be called.
     *
     * @param canvas the canvas that the text will be drawn onto
     * @param bounds boundaries of the text within specified canvas
     */
    public void draw(@NonNull Canvas canvas, @NonNull Rect bounds) {
        if (TextUtils.isEmpty(mText)) {
            return;
        }

        if (mNeedUpdateLayout || !mBounds.equals(bounds)) {
            updateLayout(bounds.width(), bounds.height());
            mNeedUpdateLayout = false;
            mNeedCalculateBounds = true;
        }

        if (mNeedCalculateBounds || !mBounds.equals(bounds)) {
            mBounds.set(bounds);
            calculateBounds();
            mNeedCalculateBounds = false;
        }
        canvas.save();
        canvas.translate(mOutputRect.left, mOutputRect.top);
        mStaticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * Requests that the text be laid out again when draw is next called. This must be called if the
     * properties of the paint or the content of the text have changed but {@link #setPaint} has not
     * been called.
     */
    public void requestUpdateLayout() {
        mNeedUpdateLayout = true;
    }

    /**
     * Sets the text that will be drawn by this renderer.
     *
     * <p>If the text contains spans, some of them may not be rendered by this class. Supported
     * spans are {@link ForegroundColorSpan}, {@link LocaleSpan}, {@link SubscriptSpan}, {@link
     * SuperscriptSpan}, {@link StrikethroughSpan}, {@link TypefaceSpan} and {@link UnderlineSpan}.
     */
    public void setText(@Nullable CharSequence text) {
        if (mOriginalText == text) {
            return;
        }

        mOriginalText = text;
        mText = applySpanAllowlist(mOriginalText);
        mNeedUpdateLayout = true;
    }

    @NonNull
    @VisibleForTesting
    CharSequence applySpanAllowlist(@NonNull CharSequence text) {
        if (text instanceof Spanned) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            Object[] spans = builder.getSpans(0, text.length(), Object.class);
            for (Object span : spans) {
                if (!isSpanAllowed(span)) {
                    builder.removeSpan(span);
                    Log.w(TAG,
                            "Removing unsupported span of type " + span.getClass().getSimpleName());
                }
            }
            return builder;
        } else {
            return text;
        }
    }

    private boolean isSpanAllowed(@NonNull Object span) {
        for (Class<?> spanClass : SPAN_ALLOW_LIST) {
            if (spanClass.isInstance(span)) {
                return true;
            }
        }
        return false;
    }

    /** Sets the paint that will be used to draw the text. */
    public void setPaint(@NonNull TextPaint paint) {
        mPaint = paint;
        mNeedUpdateLayout = true;
    }

    /**
     * Sets the padding which will be applied to the bounds before the text is drawn. The {@code
     * start} and {@code end} parameters should be given as a proportion of the width of the bounds,
     * and the {@code top} and {@code bottom} parameters should be given as a proportion of the
     * height of the bounds.
     */
    public void setRelativePadding(float start, float top, float end, float bottom) {
        if (mRelativePaddingStart == start
                && mRelativePaddingTop == top
                && mRelativePaddingEnd == end
                && mRelativePaddingBottom == bottom) {
            return;
        }

        mRelativePaddingStart = start;
        mRelativePaddingTop = top;
        mRelativePaddingEnd = end;
        mRelativePaddingBottom = bottom;
        mNeedUpdateLayout = true;
    }

    /**
     * Sets the gravity that will be used to position the text within the bounds.
     *
     * <p>Note that the text should be considered to fill the available width, which means that this
     * cannot be used to position the text horizontally. Use {@link #setAlignment} instead for
     * horizontal position.
     *
     * <p>If not called, the default is {@link Gravity#CENTER}.
     *
     * @param gravity Gravity to position text, should be one of the constants specified in {@link
     * android.view.Gravity} class.
     */
    public void setGravity(int gravity) {
        if (mGravity == gravity) {
            return;
        }
        mGravity = gravity;
        mNeedCalculateBounds = true;
    }

    /**
     * Sets the maximum number of lines of text that will be drawn. The default is 1 and if a
     * non-positive number is given, it will be ignored.
     */
    public void setMaxLines(int maxLines) {
        if (mMaxLines == maxLines || maxLines <= 0) {
            return;
        }
        mMaxLines = maxLines;
        mNeedUpdateLayout = true;
    }

    /**
     * Sets the minimum number of characters to be shown. If available space is too narrow, font
     * size may be decreased to ensure showing at least this many characters. The default is 7 which
     * is the number of characters that should always be displayed without truncation when rendering
     * the short text or short title fields of a complication.
     */
    public void setMinimumCharactersShown(int minCharactersShown) {
        if (mMinCharactersShown == minCharactersShown) {
            return;
        }
        mMinCharactersShown = minCharactersShown;
        mNeedUpdateLayout = true;
    }

    /**
     * Sets how the text should be ellipsized if it does not fit in the specified number of lines.
     * The default is {@link TextUtils.TruncateAt#END}.
     *
     * <p>Pass in {@code null} to cause text to be truncated but not ellipsized.
     */
    public void setEllipsize(@Nullable TextUtils.TruncateAt ellipsize) {
        if (mEllipsize == ellipsize) {
            return;
        }
        mEllipsize = ellipsize;
        mNeedUpdateLayout = true;
    }

    /** Sets the alignment of the text. */
    public void setAlignment(@Nullable Layout.Alignment alignment) {
        if (mAlignment == alignment) {
            return;
        }
        mAlignment = alignment;
        mNeedUpdateLayout = true;
    }

    /** Returns true if text has been set on this renderer. */
    public boolean hasText() {
        return !TextUtils.isEmpty(mText);
    }

    /** Returns true if the text will be drawn by this renderer in a left-to-right direction. */
    public boolean isLtr() {
        return mStaticLayout.getParagraphDirection(0) == Layout.DIR_LEFT_TO_RIGHT;
    }

    /**
     * Sets if the watch face is in ambient mode. Watch faces should call this function while going
     * in and out of ambient mode. This allows TextRenderer to remove special characters and styling
     * that may not be suitable for displaying in ambient mode.
     */
    public void setInAmbientMode(boolean inAmbientMode) {
        if (mInAmbientMode == inAmbientMode) {
            return;
        }
        mInAmbientMode = inAmbientMode;
        if (!TextUtils.equals(mAmbientModeText, mText)) {
            mNeedUpdateLayout = true;
        }
    }

    @SuppressWarnings("InlinedApi") // Spurious complaint about setBreakStrategy, saying API 29.
    private void updateLayout(int width, int height) {
        if (mPaint == null) {
            setPaint(new TextPaint());
        }

        int availableWidth = (int) (width * (1 - mRelativePaddingStart - mRelativePaddingEnd));

        TextPaint paint = new TextPaint(mPaint);
        // Reduce text size to prevent vertical overflow
        paint.setTextSize(Math.min(height / mMaxLines, paint.getTextSize()));
        // Check if current text fits
        float textWidth = paint.measureText(mText, 0, mText.length());
        if (textWidth > availableWidth) {
            // Decrease text size until first mMinCharactersShown characters fit
            int charactersShown = mMinCharactersShown;
            // Add one character if ellipsizing is enabled and it's not marquee
            if (mEllipsize != null && mEllipsize != TextUtils.TruncateAt.MARQUEE) {
                charactersShown = charactersShown + 1;
            }
            // Text may be shorter than character count to be shown
            charactersShown = Math.min(charactersShown, mText.length());
            CharSequence textToFit = mText.subSequence(0, charactersShown);
            textWidth = paint.measureText(textToFit, 0, textToFit.length());
            while (textWidth > availableWidth) {
                paint.setTextSize(paint.getTextSize() - TEXT_SIZE_STEP_SIZE);
                textWidth = paint.measureText(textToFit, 0, textToFit.length());
            }
        }

        CharSequence text = mText;
        if (mInAmbientMode) {
            mAmbientModeText = EmojiHelper.replaceEmoji(mText, SPACE_CHARACTER);
            text = mAmbientModeText;
        }

        StaticLayout.Builder builder =
                StaticLayout.Builder.obtain(text, 0, text.length(), paint, availableWidth);
        builder.setBreakStrategy(LineBreaker.BREAK_STRATEGY_HIGH_QUALITY);
        builder.setEllipsize(mEllipsize);
        builder.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_FULL);
        builder.setMaxLines(mMaxLines);
        builder.setAlignment(mAlignment);
        mStaticLayout = builder.build();
    }

    private void calculateBounds() {
        int layoutDirection = isLtr() ? View.LAYOUT_DIRECTION_LTR : View.LAYOUT_DIRECTION_RTL;
        int leftPadding =
                (int) (mBounds.width() * (isLtr() ? mRelativePaddingStart : mRelativePaddingEnd));
        int rightPadding =
                (int) (mBounds.width() * (isLtr() ? mRelativePaddingEnd : mRelativePaddingStart));
        int topPadding = (int) (mBounds.height() * mRelativePaddingTop);
        int bottomPadding = (int) (mBounds.height() * mRelativePaddingBottom);
        mWorkingRect.set(
                mBounds.left + leftPadding,
                mBounds.top + topPadding,
                mBounds.right - rightPadding,
                mBounds.bottom - bottomPadding);
        Gravity.apply(
                mGravity,
                mStaticLayout.getWidth(),
                mStaticLayout.getHeight(),
                mWorkingRect,
                mOutputRect,
                layoutDirection);
    }
}
