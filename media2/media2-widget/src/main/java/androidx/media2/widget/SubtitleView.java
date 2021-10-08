/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;

/** Copied from frameworks/base/core/java/com/android/internal/widget/SubtitleView.java */
class SubtitleView extends View {
    // Ratio of inner padding to font size.
    private static final float INNER_PADDING_RATIO = 0.125f;

    // Styled dimensions.
    private final float mCornerRadius;
    private final float mOutlineWidth;
    private final float mShadowRadius;
    private final float mShadowOffsetX;
    private final float mShadowOffsetY;

    /** Temporary rectangle used for computing line bounds. */
    private final RectF mLineBounds = new RectF();

    /** Reusable spannable string builder used for holding text. */
    private final SpannableStringBuilder mText = new SpannableStringBuilder();

    private Alignment mAlignment;
    private TextPaint mTextPaint;
    private Paint mPaint;

    private int mForegroundColor;
    private int mBackgroundColor;
    private int mEdgeColor;
    private int mEdgeType;

    private boolean mHasMeasurements;
    private int mLastMeasuredWidth;
    private StaticLayout mLayout;

    private float mSpacingMult = 1;
    private float mSpacingAdd = 0;
    private int mInnerPaddingX = 0;

    SubtitleView(Context context) {
        this(context, null);
    }

    SubtitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SubtitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Set up density-dependent properties.
        // TODO: Move these to a default style.
        final Resources res = getContext().getResources();
        mCornerRadius = res
                .getDimensionPixelSize(R.dimen.media2_widget_subtitle_corner_radius);
        mOutlineWidth = res
                .getDimensionPixelSize(R.dimen.media2_widget_subtitle_outline_width);
        mShadowRadius = res
                .getDimensionPixelSize(R.dimen.media2_widget_subtitle_shadow_radius);
        mShadowOffsetX = res
                .getDimensionPixelSize(R.dimen.media2_widget_subtitle_shadow_offset);
        mShadowOffsetY = mShadowOffsetX;

        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setSubpixelText(true);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    public void setText(int resId) {
        final CharSequence text = getContext().getText(resId);
        setText(text);
    }

    public void setText(CharSequence text) {
        mText.clear();
        mText.append(text);

        mHasMeasurements = false;

        requestLayout();
        invalidate();
    }

    public void setForegroundColor(int color) {
        mForegroundColor = color;

        invalidate();
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;

        invalidate();
    }

    public void setEdgeType(int edgeType) {
        mEdgeType = edgeType;

        invalidate();
    }

    public void setEdgeColor(int color) {
        mEdgeColor = color;

        invalidate();
    }

    /**
     * Sets the text size in pixels.
     *
     * @param size the text size in pixels
     */
    public void setTextSize(float size) {
        if (mTextPaint.getTextSize() != size) {
            mTextPaint.setTextSize(size);
            mInnerPaddingX = (int) (size * INNER_PADDING_RATIO + 0.5f);

            mHasMeasurements = false;

            requestLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        if (typeface != null && !typeface.equals(mTextPaint.getTypeface())) {
            mTextPaint.setTypeface(typeface);

            mHasMeasurements = false;

            requestLayout();
            invalidate();
        }
    }

    public void setAlignment(Alignment textAlignment) {
        if (mAlignment != textAlignment) {
            mAlignment = textAlignment;

            mHasMeasurements = false;

            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthSpec = MeasureSpec.getSize(widthMeasureSpec);

        if (computeMeasurements(widthSpec)) {
            final StaticLayout layout = mLayout;

            // Account for padding.
            final int paddingX = getPaddingLeft() + getPaddingRight() + mInnerPaddingX * 2;
            final int width = layout.getWidth() + paddingX;
            final int height = layout.getHeight() + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, height);
        } else {
            setMeasuredDimension(MEASURED_STATE_TOO_SMALL, MEASURED_STATE_TOO_SMALL);
        }
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;

        computeMeasurements(width);
    }

    @SuppressWarnings("deprecation")  // The constructor of StaticLayout is deprecated.
    private boolean computeMeasurements(int maxWidth) {
        if (mHasMeasurements && maxWidth == mLastMeasuredWidth) {
            return true;
        }

        // Account for padding.
        final int paddingX = getPaddingLeft() + getPaddingRight() + mInnerPaddingX * 2;
        maxWidth -= paddingX;
        if (maxWidth <= 0) {
            return false;
        }

        // TODO: Implement minimum-difference line wrapping. Adding the results
        // of Paint.getTextWidths() seems to return different values than
        // StaticLayout.getWidth(), so this is non-trivial.
        mHasMeasurements = true;
        mLastMeasuredWidth = maxWidth;
        if (Build.VERSION.SDK_INT >= 23) {
            StaticLayout.Builder builder = Api23Impl.obtainStaticLayoutBuilder(mText, 0,
                    mText.length(), mTextPaint, maxWidth);
            Api23Impl.setAlignment(builder, mAlignment);
            Api23Impl.setLineSpacing(builder, mSpacingAdd, mSpacingMult);
            if (Build.VERSION.SDK_INT >= 28) {
                Api28Impl.setUseLineSpacingFromFallbacks(builder, true);
            }
            mLayout = Api23Impl.build(builder);
        } else {
            mLayout = new StaticLayout(mText, 0, mText.length(), mTextPaint, maxWidth, mAlignment,
                    mSpacingMult, mSpacingAdd, true);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas c) {
        final StaticLayout layout = mLayout;
        if (layout == null) {
            return;
        }

        final int saveCount = c.save();
        final int innerPaddingX = mInnerPaddingX;
        c.translate(getPaddingLeft() + innerPaddingX, getPaddingTop());

        final int lineCount = layout.getLineCount();
        final Paint textPaint = mTextPaint;
        final Paint paint = mPaint;
        final RectF bounds = mLineBounds;

        if (Color.alpha(mBackgroundColor) > 0) {
            final float cornerRadius = mCornerRadius;
            float previousBottom = layout.getLineTop(0);

            paint.setColor(mBackgroundColor);
            paint.setStyle(Style.FILL);

            for (int i = 0; i < lineCount; i++) {
                bounds.left = layout.getLineLeft(i) - innerPaddingX;
                bounds.right = layout.getLineRight(i) + innerPaddingX;
                bounds.top = previousBottom;
                bounds.bottom = layout.getLineBottom(i);
                previousBottom = bounds.bottom;

                c.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
            }
        }

        final int edgeType = mEdgeType;
        if (edgeType == CaptionStyle.EDGE_TYPE_OUTLINE) {
            textPaint.setStrokeJoin(Join.ROUND);
            textPaint.setStrokeWidth(mOutlineWidth);
            textPaint.setColor(mEdgeColor);
            textPaint.setStyle(Style.FILL_AND_STROKE);

            layout.draw(c);

        } else if (edgeType == CaptionStyle.EDGE_TYPE_DROP_SHADOW) {
            textPaint.setShadowLayer(mShadowRadius, mShadowOffsetX, mShadowOffsetY, mEdgeColor);
        } else if (edgeType == CaptionStyle.EDGE_TYPE_RAISED
                || edgeType == CaptionStyle.EDGE_TYPE_DEPRESSED) {
            final boolean raised = edgeType == CaptionStyle.EDGE_TYPE_RAISED;
            final int colorUp = raised ? Color.WHITE : mEdgeColor;
            final int colorDown = raised ? mEdgeColor : Color.WHITE;
            final float offset = mShadowRadius / 2f;

            textPaint.setColor(mForegroundColor);
            textPaint.setStyle(Style.FILL);
            textPaint.setShadowLayer(mShadowRadius, -offset, -offset, colorUp);

            layout.draw(c);

            textPaint.setShadowLayer(mShadowRadius, offset, offset, colorDown);
        }

        textPaint.setColor(mForegroundColor);
        textPaint.setStyle(Style.FILL);

        layout.draw(c);

        textPaint.setShadowLayer(0, 0, 0, 0);
        c.restoreToCount(saveCount);
    }

    @RequiresApi(23)
    static class Api23Impl {

        @DoNotInline
        static StaticLayout build(StaticLayout.Builder builder) {
            return builder.build();
        }

        @DoNotInline
        static StaticLayout.Builder obtainStaticLayoutBuilder(CharSequence source, int start,
                int end, TextPaint paint, int width) {
            return StaticLayout.Builder.obtain(source, start, end, paint, width);
        }

        @DoNotInline
        static void setAlignment(StaticLayout.Builder builder,
                Alignment alignment) {
            builder.setAlignment(alignment);
        }

        @DoNotInline
        static void setLineSpacing(StaticLayout.Builder builder, float spacingAdd,
                float spacingMult) {
            builder.setLineSpacing(spacingAdd, spacingMult);
        }

        private Api23Impl() {}
    }

    @RequiresApi(28)
    static class Api28Impl {

        @DoNotInline
        static void setUseLineSpacingFromFallbacks(StaticLayout.Builder builder,
                boolean useLineSpacingFromFallbacks) {
            builder.setUseLineSpacingFromFallbacks(useLineSpacingFromFallbacks);
        }

        private Api28Impl() {}
    }
}
