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

package androidx.wear.widget;

import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.lang.Math.sin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.wear.R;

/**
 * CurvedTextView is a component allowing developers to easily write curved text following
 * the curvature of the largest circle that can be inscribed in the view. ArcLayout could be
 * used to concatenate multiple curved texts, also layout together with other widgets such as icons.
 */
public class CurvedTextView extends View implements ArcLayout.Widget {
    private static final float UNSET_ANCHOR_DEGREE = -1f;
    private static final int UNSET_ANCHOR_TYPE = -1;
    private static final float MIN_SWEEP_DEGREE = 0f;
    private static final float MAX_SWEEP_DEGREE = 359.9f;
    private static final float DEFAULT_TEXT_SIZE = 24f;
    @ColorInt
    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_STYLE = Typeface.NORMAL;
    private static final boolean DEFAULT_CLOCKWISE = true;
    private static final int FONT_WEIGHT_MAX = 1000;
    private static final float ITALIC_SKEW_X = -0.25f;
    // make 0 degree at 12 o'clock, since canvas assumes 0 degree is 3 o'clock
    private static final float ANCHOR_DEGREE_OFFSET = -90f;

    private final Path mPath = new Path();
    private final Path mBgPath = new Path();
    private final TextPaint mPaint = new TextPaint();
    private final Rect mBounds = new Rect();
    private final Rect mBgBounds = new Rect();
    private boolean mDirty = true;
    private String mTextToDraw = "";
    private float mPathRadius = 0f;
    private float mTextSweepDegrees = 0f;
    private float mBackgroundSweepDegrees = MAX_SWEEP_DEGREE;
    private int mLastUsedTextAlignment = -1;
    private float mLocalRotateAngle = 0f;

    @ArcLayout.AnchorType
    private int mAnchorType;
    private float mAnchorAngleDegrees;
    private float mMinSweepDegrees;
    private float mMaxSweepDegrees;
    private String mText = "";
    private float mTextSize = DEFAULT_TEXT_SIZE;
    @Nullable
    private Typeface mTypeface = null;
    private boolean mClockwise = DEFAULT_CLOCKWISE;
    @ColorInt
    private int mTextColor = DEFAULT_TEXT_COLOR;
    @Nullable
    private TextUtils.TruncateAt mEllipsize = null;
    private float mLetterSpacing = 0f;
    @Nullable
    private String mFontFeatureSettings = null;
    @Nullable
    private String mFontVariationSettings = null;

    // If true, it means we got the touch_down event and are receiving the touch events that follow.
    private boolean mHandlingTouch = false;


    public CurvedTextView(@NonNull Context context) {
        this(context, null);
    }

    public CurvedTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public CurvedTextView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public CurvedTextView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyle,
            int defStyleRes) {
        super(context, attrs, defStyle, defStyleRes);

        mPaint.setAntiAlias(true);

        TextAppearanceAttributes attributes = new TextAppearanceAttributes();
        attributes.mTextColor = ColorStateList.valueOf(DEFAULT_TEXT_COLOR);

        final Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.TextViewAppearance, defStyle, defStyleRes);

        TypedArray appearance = null;
        int ap = a.getResourceId(R.styleable.TextViewAppearance_android_textAppearance, -1);
        a.recycle();

        if (ap != -1) {
            appearance = theme.obtainStyledAttributes(ap, R.styleable.TextAppearance);
        }
        if (appearance != null) {
            readTextAppearance(appearance, attributes, true);
            appearance.recycle();
        }

        a = context.obtainStyledAttributes(
                attrs, R.styleable.CurvedTextView, defStyle, defStyleRes);
        // overrride the value in the appearance with explicitly specified attribute values
        readTextAppearance(a, attributes, false);

        // read the other supported TextView attributes
        if (a.hasValue(R.styleable.CurvedTextView_android_text)) {
            mText = a.getString(R.styleable.CurvedTextView_android_text);
        }

        int textEllipsize = a.getInt(R.styleable.CurvedTextView_android_ellipsize, 0);
        switch (textEllipsize) {
            case 1:
                mEllipsize = TextUtils.TruncateAt.START;
                break;
            case 2:
                mEllipsize = TextUtils.TruncateAt.MIDDLE;
                break;
            case 3:
                mEllipsize = TextUtils.TruncateAt.END;
                break;
            default:
                mEllipsize = null;
        }

        // read the custom CurvedTextView attributes
        mMaxSweepDegrees =
                a.getFloat(R.styleable.CurvedTextView_maxSweepDegrees, MAX_SWEEP_DEGREE);
        mMaxSweepDegrees = min(mMaxSweepDegrees, MAX_SWEEP_DEGREE);
        mMinSweepDegrees =
                a.getFloat(R.styleable.CurvedTextView_minSweepDegrees, MIN_SWEEP_DEGREE);
        if (mMinSweepDegrees > mMaxSweepDegrees) {
            throw new IllegalArgumentException(
                    "MinSweepDegrees cannot be bigger than MaxSweepDegrees"
            );
        }
        mAnchorType = a.getInt(R.styleable.CurvedTextView_anchorPosition, UNSET_ANCHOR_TYPE);
        mAnchorAngleDegrees = a.getFloat(
                R.styleable.CurvedTextView_anchorAngleDegrees, UNSET_ANCHOR_DEGREE
        );
        mAnchorAngleDegrees = mAnchorAngleDegrees % 360f;
        mClockwise = a.getBoolean(R.styleable.CurvedTextView_clockwise, DEFAULT_CLOCKWISE);

        a.recycle();

        applyTextAppearance(attributes);

        mPaint.setTextSize(mTextSize);
    }

    @Override
    @FloatRange(from = 0.0f, to = 360.0f, toInclusive = true)
    public float getSweepAngleDegrees() {
        return mBackgroundSweepDegrees;
    }

    @Override
    @Px
    public int getThickness() {
        return round(mPaint.getFontMetrics().descent - mPaint.getFontMetrics().ascent);
    }

    /**
     * @throws IllegalArgumentException if the anchorType and/or anchorAngleDegrees attributes
     *                                  were set for a widget in ArcLayout
     */
    @Override
    public void checkInvalidAttributeAsChild() {
        if (mAnchorType != UNSET_ANCHOR_TYPE) {
            throw new IllegalArgumentException(
                    "CurvedTextView shall not set anchorType value when added into"
                            + "ArcLayout"
            );
        }

        if (mAnchorAngleDegrees != UNSET_ANCHOR_DEGREE) {
            throw new IllegalArgumentException(
                    "CurvedTextView shall not set anchorAngleDegrees value when added into "
                            + "ArcLayout"
            );
        }
    }

    /**
     * See {@link ArcLayout.Widget#isPointInsideClickArea(float, float)}
     */
    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        float radius2 = min(getWidth(), getHeight()) / 2f
                - (mClockwise ? getPaddingTop() : getPaddingBottom());
        float radius1 =
                radius2 - mPaint.getFontMetrics().descent + mPaint.getFontMetrics().ascent;

        float dx = x - getWidth() / 2;
        float dy = y - getHeight() / 2;

        float r2 = dx * dx + dy * dy;
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false;
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        float angle = (float) Math.toDegrees(Math.atan2(Math.abs(dx), -dy));
        return angle < mBackgroundSweepDegrees / 2;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        doUpdate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mPaint.getTextBounds(mText, 0, mText.length(), mBounds);

        // Note that ascent is negative.
        mPathRadius = min(getMeasuredWidth(), getMeasuredHeight()) / 2f
                + (mClockwise ? mPaint.getFontMetrics().ascent - getPaddingTop() :
                -mPaint.getFontMetrics().descent - getPaddingBottom());
        mTextSweepDegrees = min(
                getWidthSelf() / mPathRadius / (float) Math.PI * 180f,
                MAX_SWEEP_DEGREE);
        mBackgroundSweepDegrees = max(min(mMaxSweepDegrees, mTextSweepDegrees), mMinSweepDegrees);
    }

    private float getWidthSelf() {
        return (float) mBounds.width() + getPaddingLeft() + getPaddingRight();
    }

    private String ellipsize(int ellipsizedWidth) {
        StaticLayout.Builder layoutBuilder =
                StaticLayout.Builder.obtain(mText, 0, mText.length(), mPaint, ellipsizedWidth);
        layoutBuilder.setEllipsize(mEllipsize);
        layoutBuilder.setMaxLines(1);
        StaticLayout layout = layoutBuilder.build();

        // Cut text that it's too big even if no ellipsize mode is provided.
        if (mEllipsize == null) {
            return mText.substring(0, layout.getLineEnd(0));
        }

        int ellipsisCount = layout.getEllipsisCount(0);
        if (ellipsisCount == 0) {
            return mText;
        }

        int ellipsisStart = layout.getEllipsisStart(0);
        char[] textToDrawArray = mText.toCharArray();
        textToDrawArray[ellipsisStart] = '\u2026'; // ellipsis "..."
        for (int i = ellipsisStart + 1; i < ellipsisStart + ellipsisCount; i++) {
            if (i >= 0 && i < mText.length()) {
                textToDrawArray[i] = '\uFEFF'; // 0-width space
            }
        }
        return new String(textToDrawArray);
    }

    private void updatePathsIfNeeded(boolean withBackground) {
        // The dirty flag is not set when properties we inherit from View are modified
        if (!mDirty && ((int) getTextAlignment() == mLastUsedTextAlignment)) {
            return;
        }

        mDirty = false;
        mLastUsedTextAlignment = (int) getTextAlignment();

        if (mTextSweepDegrees <= mMaxSweepDegrees) {
            mTextToDraw = mText;
        } else {
            mTextToDraw = ellipsize(
                    (int) (mMaxSweepDegrees / 180f * Math.PI * mPathRadius) - getPaddingLeft()
                            - getPaddingRight()
            );
            mTextSweepDegrees = mMaxSweepDegrees;
        }

        float clockwiseFactor = mClockwise ? 1f : -1f;

        float alignmentFactor = 0.5f;
        switch (getTextAlignment()) {
            case TEXT_ALIGNMENT_TEXT_START:
            case TEXT_ALIGNMENT_VIEW_START:
                alignmentFactor = 0f;
                break;
            case TEXT_ALIGNMENT_TEXT_END:
            case TEXT_ALIGNMENT_VIEW_END:
                alignmentFactor = 1f;
                break;
            default:
                alignmentFactor = 0.5f; // TEXT_ALIGNMENT_CENTER
        }

        float anchorTypeFactor;
        switch (mAnchorType) {
            case ArcLayout.ANCHOR_START:
                anchorTypeFactor = 0.5f;
                break;
            case ArcLayout.ANCHOR_END:
                anchorTypeFactor = -0.5f;
                break;
            case ArcLayout.ANCHOR_CENTER: // Center is the default.
            default:
                anchorTypeFactor = 0f;
        }

        mLocalRotateAngle = (mAnchorAngleDegrees == UNSET_ANCHOR_DEGREE ? 0f : mAnchorAngleDegrees)
                + clockwiseFactor * anchorTypeFactor * mBackgroundSweepDegrees;

        // Always draw the curved text on top center, then rotate the canvas to the right position
        float backgroundStartAngle =
                -clockwiseFactor * 0.5f * mBackgroundSweepDegrees + ANCHOR_DEGREE_OFFSET;

        float textStartAngle =
                backgroundStartAngle + clockwiseFactor * (float) (
                        alignmentFactor * (mBackgroundSweepDegrees - mTextSweepDegrees)
                                + getPaddingLeft() / mPathRadius / Math.PI * 180);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        mPath.reset();
        mPath.addArc(
                centerX - mPathRadius,
                centerY - mPathRadius,
                centerX + mPathRadius,
                centerY + mPathRadius,
                textStartAngle,
                clockwiseFactor * mTextSweepDegrees
        );

        if (withBackground) {
            mBgPath.reset();
            // NOTE: Ensure that if the code to compute these radius* change, containsPoint() is
            // also updated.
            float radius1 = mPathRadius - clockwiseFactor * mPaint.getFontMetrics().descent;
            float radius2 = mPathRadius - clockwiseFactor * mPaint.getFontMetrics().ascent;
            mBgPath.arcTo(
                    centerX - radius2,
                    centerY - radius2,
                    centerX + radius2,
                    centerY + radius2,
                    backgroundStartAngle,
                    clockwiseFactor * mBackgroundSweepDegrees, false
            );
            mBgPath.arcTo(
                    centerX - radius1,
                    centerY - radius1,
                    centerX + radius1,
                    centerY + radius1,
                    backgroundStartAngle + clockwiseFactor * mBackgroundSweepDegrees,
                    -clockwiseFactor * mBackgroundSweepDegrees, false
            );
            mBgPath.close();

            float angle = backgroundStartAngle;
            float x0 = (float) (centerX + radius2 * cos(angle * Math.PI / 180));
            float x1 = (float) (centerX + radius1 * cos(angle * Math.PI / 180));
            float y0 = (float) (centerY + radius2 * sin(angle * Math.PI / 180));
            float y1 = (float) (centerY + radius1 * sin(angle * Math.PI / 180));
            angle = backgroundStartAngle + clockwiseFactor * mBackgroundSweepDegrees;
            float x2 = (float) (centerX + radius2 * cos(angle * Math.PI / 180));
            float x3 = (float) (centerX + radius1 * cos(angle * Math.PI / 180));
            // Background axis-aligned bounding box calculation. Note that, we always center the
            // text on the top-center of the view.
            // top: always will be centerY - outerRadius
            // bottom: the max y of end points of the outer and inner arc contains the text
            // left: if over -90 degrees, centerX - outerRadius, otherwise the min x of start,
            // end points of the outer and inner arc contains the text
            // right: if over 90 degrees, centerX + outerRadius, otherwise the max x of start,
            // end points of the outer and inner arc contains the text
            float outerRadius = max(radius1, radius2);
            mBgBounds.top = (int) (centerY - outerRadius);
            mBgBounds.bottom = (int) max(y0, y1);
            mBgBounds.left =
                    mBackgroundSweepDegrees >= 180.0f
                            ? (int) (centerX - outerRadius)
                            : (int) min(x0, min(x1, min(x2, x3)));
            mBgBounds.right =
                    mBackgroundSweepDegrees >= 180.0f
                            ? (int) (centerX + outerRadius)
                            : (int) max(x0, max(x1, max(x2, x3)));
        }
    }

    @Override
    // We only filter events and defer to super.onTouchEvent()
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (!mHandlingTouch && event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }

        float x0 = event.getX() - getWidth() / 2;
        float y0 = event.getY() - getHeight() / 2;

        double rotAngle = -Math.toRadians(mLocalRotateAngle);

        float tempX = (float)
                ((x0 * cos(rotAngle) - y0 * sin(rotAngle)) + getWidth() / 2);
        y0 = (float) ((x0 * sin(rotAngle) + y0 * cos(rotAngle)) + getHeight() / 2);
        x0 = tempX;

        // Should we start handling the touch events?
        if (!mHandlingTouch && isPointInsideClickArea(x0, y0)) {
            mHandlingTouch = true;
        }

        // We just started or are in the middle of handling events, forward to View to handle.
        if (mHandlingTouch) {
            if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                // We should end handling events now
                mHandlingTouch = false;
            }
            event.offsetLocation(x0 - event.getX(), y0 - event.getY());
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();

        boolean withBackground = getBackground() != null;
        updatePathsIfNeeded(withBackground);
        canvas.rotate(
                mLocalRotateAngle,
                getWidth() / 2f,
                getHeight() / 2f);

        if (withBackground) {
            canvas.clipPath(mBgPath);
            getBackground().setBounds(mBgBounds);
        }
        super.draw(canvas);

        canvas.restore();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mPaint.setColor(mTextColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawTextOnPath(mTextToDraw, mPath, 0f, 0f, mPaint);
    }

    /**
     * Sets the Typeface taking into account the given attributes.
     *
     * @param familyName    Family name string, e.g. "serif"
     * @param typefaceIndex An index of the typeface enum, e.g. SANS, SERIF.
     * @param style         A typeface style
     * @param weight        A weight value for the Typeface or -1 if not specified.
     */
    private void setTypefaceFromAttrs(
            @Nullable String familyName,
            int typefaceIndex,
            int style,
            int weight
    ) {
        // typeface is ignored when font family is set
        if (mTypeface == null && familyName != null) {
            // Lookup normal Typeface from system font map.
            Typeface normalTypeface = Typeface.create(familyName, Typeface.NORMAL);
            resolveStyleAndSetTypeface(normalTypeface, style, weight);
        } else if (mTypeface != null) {
            resolveStyleAndSetTypeface(mTypeface, style, weight);
        } else { // both typeface and familyName is null.
            switch (typefaceIndex) {
                case 1:
                    resolveStyleAndSetTypeface(Typeface.SANS_SERIF, style, weight);
                    break;
                case 2:
                    resolveStyleAndSetTypeface(Typeface.SERIF, style, weight);
                    break;
                case 3:
                    resolveStyleAndSetTypeface(Typeface.MONOSPACE, style, weight);
                    break;
                default:
                    resolveStyleAndSetTypeface(null, style, weight);
            }
        }
    }

    private void resolveStyleAndSetTypeface(@Nullable Typeface tf, int style, int weight) {
        if (weight >= 0 && Build.VERSION.SDK_INT >= 28) {
            int clampedWeight = min(FONT_WEIGHT_MAX, weight);
            boolean italic = (style & Typeface.ITALIC) != 0;
            mTypeface = Api28Impl.createTypeface(tf, clampedWeight, italic);
            mPaint.setTypeface(mTypeface);
        } else {
            setTypeface(tf, style);
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed, and turns on the fake
     * bold and italic bits in the Paint if the Typeface that you provided does not have all the
     * bits in the style that you specified.
     */
    public void setTypeface(@Nullable Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }
            if (!tf.equals(mPaint.getTypeface())) {
                mPaint.setTypeface(tf);
                mTypeface = tf;
            }
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mPaint.setTextSkewX(((need & Typeface.ITALIC) != 0) ? ITALIC_SKEW_X : 0f);
        } else {
            mPaint.setFakeBoldText(false);
            mPaint.setTextSkewX(0f);
            if ((tf != null && !tf.equals(mPaint.getTypeface()))
                    || (tf == null && mPaint.getTypeface() != null)) {
                mPaint.setTypeface(tf);
                mTypeface = tf;
            }
        }
        doUpdate();
    }

    /**
     * Set of attribute that can be defined in a Text Appearance.
     */
    private static class TextAppearanceAttributes {
        @Nullable
        ColorStateList mTextColor = null;
        float mTextSize = DEFAULT_TEXT_SIZE;
        @Nullable
        String mFontFamily = null;
        boolean mFontFamilyExplicit = false;
        int mTypefaceIndex = -1;
        int mTextStyle = DEFAULT_TEXT_STYLE;
        int mFontWeight = -1;
        float mLetterSpacing = 0f;
        @Nullable
        String mFontFeatureSettings = null;
        @Nullable
        String mFontVariationSettings = null;

        TextAppearanceAttributes() {
        }
    }

    /**
     * Sets the textColor, size, style, font etc from the specified TextAppearanceAttributes
     */
    private void applyTextAppearance(TextAppearanceAttributes attributes) {
        if (attributes.mTextColor != null) {
            mTextColor = attributes.mTextColor.getDefaultColor();
        }

        if (attributes.mTextSize != -1f) {
            mTextSize = attributes.mTextSize;
        }

        setTypefaceFromAttrs(
                attributes.mFontFamily,
                attributes.mTypefaceIndex,
                attributes.mTextStyle,
                attributes.mFontWeight
        );

        mPaint.setLetterSpacing(attributes.mLetterSpacing);
        mLetterSpacing = attributes.mLetterSpacing;
        mPaint.setFontFeatureSettings(attributes.mFontFeatureSettings);
        mFontFeatureSettings = attributes.mFontFeatureSettings;
        if (Build.VERSION.SDK_INT >= 26) {
            Api26Impl.paintSetFontVariationSettings(mPaint, attributes.mFontVariationSettings);
        }
        mFontVariationSettings = attributes.mFontVariationSettings;
    }

    /**
     * Read the Text Appearance attributes from a given TypedArray and set its values to the
     * given set. If the TypedArray contains a value that already set in the given attributes,
     * that will be overridden.
     */
    private void readTextAppearance(
            TypedArray appearance,
            TextAppearanceAttributes attributes,
            boolean isTextAppearance
    ) {
        int attrIndex = isTextAppearance ? R.styleable.TextAppearance_android_textColor :
                R.styleable.CurvedTextView_android_textColor;
        if (appearance.hasValue(attrIndex)) {
            attributes.mTextColor = appearance.getColorStateList(attrIndex);
        }

        attributes.mTextSize = appearance.getDimensionPixelSize(
                isTextAppearance ? R.styleable.TextAppearance_android_textSize :
                        R.styleable.CurvedTextView_android_textSize,
                (int) attributes.mTextSize
        );

        attributes.mTextStyle = appearance.getInt(
                isTextAppearance ? R.styleable.TextAppearance_android_textStyle :
                        R.styleable.CurvedTextView_android_textStyle,
                attributes.mTextStyle
        );

        // make sure that the typeface attribute is read before fontFamily attribute
        attributes.mTypefaceIndex = appearance.getInt(
                isTextAppearance ? R.styleable.TextAppearance_android_typeface :
                        R.styleable.CurvedTextView_android_typeface,
                attributes.mTypefaceIndex
        );
        if (attributes.mTypefaceIndex != -1 && !attributes.mFontFamilyExplicit) {
            attributes.mFontFamily = null;
        }

        attrIndex = isTextAppearance ? R.styleable.TextAppearance_android_fontFamily :
                R.styleable.CurvedTextView_android_fontFamily;
        if (appearance.hasValue(attrIndex)) {
            attributes.mFontFamily = appearance.getString(attrIndex);
            attributes.mFontFamilyExplicit = !isTextAppearance;
        }

        attributes.mFontWeight = appearance.getInt(
                isTextAppearance ? R.styleable.TextAppearance_android_textFontWeight :
                        R.styleable.CurvedTextView_android_textFontWeight,
                attributes.mFontWeight
        );

        attributes.mLetterSpacing = appearance.getFloat(
                isTextAppearance ? R.styleable.TextAppearance_android_letterSpacing :
                        R.styleable.CurvedTextView_android_letterSpacing,
                attributes.mLetterSpacing
        );

        attrIndex = isTextAppearance ? R.styleable.TextAppearance_android_fontFeatureSettings :
                R.styleable.CurvedTextView_android_fontFeatureSettings;
        if (appearance.hasValue(attrIndex)) {
            attributes.mFontFeatureSettings = appearance.getString(attrIndex);
        }

        attrIndex = isTextAppearance ? R.styleable.TextAppearance_android_fontVariationSettings :
                R.styleable.CurvedTextView_android_fontVariationSettings;
        if (appearance.hasValue(attrIndex)) {
            attributes.mFontVariationSettings = appearance.getString(attrIndex);
        }
    }

    private void doUpdate() {
        mDirty = true;
        requestLayout();
        postInvalidate();
    }

    private void doRedraw() {
        mDirty = true;
        postInvalidate();
    }

    /** Returns the anchor type for positioning the curved text */
    @ArcLayout.AnchorType
    public int getAnchorType() {
        return mAnchorType;
    }

    /**
     * Sets the anchor type for positioning the curved text.
     * @param value the anchor type,  one of {ANCHOR_START, ANCHOR_CENTER, ANCHOR_END}
     */
    public void setAnchorType(@ArcLayout.AnchorType int value) {
        mAnchorType = value;
        doUpdate();
    }

    /** Returns the anchor angle used for positioning the text, in degrees. */
    @FloatRange(from = 0f, to = 360f, toInclusive = true)
    public float getAnchorAngleDegrees() {
        return mAnchorAngleDegrees;
    }

    /** Sets the anchor angle used for positioning the text, in degrees. */
    public void setAnchorAngleDegrees(
            @FloatRange(from = 0f, to = 360f, toInclusive = true) float value) {
        mAnchorAngleDegrees = value;
        doRedraw();
    }

    /** Sets the minimum and maximum sweep angle in degrees for rendering the text.
     * @param minSweep Ensure the text takes at least this angle (in degrees) in the arc. Use 0f if
     *                 you don't want to specify a minimum.
     * @param maxSweep Limit the maximum angle (in degrees) that this curved text can take. Use
     *                 360f if you don't want to specify a maximum.
     */
    public void setSweepRangeDegrees(
            @FloatRange(from = 0f, to = 360f, toInclusive = true) float minSweep,
            @FloatRange(from = 0f, to = 360f, toInclusive = true) float maxSweep) {
        if (minSweep > maxSweep) {
            throw new IllegalArgumentException(
                    "MaxSweepDegrees cannot be smaller than MinSweepDegrees"
            );
        }
        mMinSweepDegrees = min(max(minSweep, MIN_SWEEP_DEGREE), MAX_SWEEP_DEGREE);
        mMaxSweepDegrees = min(maxSweep, MAX_SWEEP_DEGREE);
        doUpdate();
    }

    /** Returns the sweep angle in degrees for rendering the text */
    @FloatRange(from = 0f, to = 360f, toInclusive = true)
    public float getMinSweepDegrees() {
        return mMinSweepDegrees;
    }

    /** Returns the maximum sweep angle in degrees for rendering the text */
    @FloatRange(from = 0f, to = 360f, toInclusive = true)
    public float getMaxSweepDegrees() {
        return mMaxSweepDegrees;
    }

    /** Returns the text to be rendered */
    @Nullable
    public String getText() {
        return mText;
    }

    /** Sets the text to be rendered */
    public void setText(@Nullable String value) {
        mText = value == null ? "" : value;
        doUpdate();
    }

    /** Returns the text size for rendering the text */
    public float getTextSize() {
        return mTextSize;
    }

    /** Sets the text size for rendering the text */
    public void setTextSize(float value) {
        mTextSize = value;
        mPaint.setTextSize(mTextSize);
        doUpdate();
    }

    /** Gets the current Typeface that is used to style the text. */
    @Nullable
    public Typeface getTypeface() {
        return mTypeface;
    }

    /**
     * Sets the typeface and style in which the text should be displayed. Note that not all
     * Typeface families actually have bold and italic variants
     */
    public void setTypeface(@Nullable Typeface value) {
        mTypeface = value;
        doUpdate();
    }

    /** Returns the curved text layout direction */
    public boolean isClockwise() {
        return mClockwise;
    }

    /** Sets the curved text layout direction */
    public void setClockwise(boolean value) {
        mClockwise = value;
        doUpdate();
    }

    /** Returns the color for rendering the text */
    @ColorInt
    public int getTextColor() {
        return mTextColor;
    }

    /** Sets the color for rendering the text */
    public void setTextColor(@ColorInt int value) {
        mTextColor = value;
        doRedraw();
    }

    /**
     * Returns where, if anywhere, words that are longer than the view is wide should be
     * ellipsized.
     */
    @Nullable
    public TextUtils.TruncateAt getEllipsize() {
        return mEllipsize;
    }

    /**
     * Causes words in the text that are longer than the view's width to be ellipsized. Use null
     * to turn off ellipsizing.
     */
    public void setEllipsize(@Nullable TextUtils.TruncateAt value) {
        mEllipsize = value;
        doRedraw();
    }

    /**
     * Gets the text letter-space value, which determines the spacing between characters. The
     * value returned is in ems. Normally, this value is 0.0.
     * @return The text letter-space value in ems.
     */
    public float getLetterSpacing() {
        return mLetterSpacing;
    }

    /**
     * Sets text letter-spacing in ems. Typical values for slight expansion will be around 0.05.
     * Negative values tighten text.
     * @param value A text letter-space value in ems.
     */
    public void setLetterSpacing(float value) {
        mLetterSpacing = value;
        doUpdate();
    }

    /**
     * Returns the font feature settings. The format is the same as the CSS font-feature-settings
     * attribute: https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @return The currently set font feature settings. Default is null.
     */
    @Nullable
    public String getFontFeatureSettings() {
        return mFontFeatureSettings;
    }

    /**
     * Sets font feature settings. The format is the same as the CSS font-feature-settings
     * attribute: https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop
     * @param value Font feature settings represented as CSS compatible string. This value may be
     *             null.
     */
    public void setFontFeatureSettings(@Nullable String value) {
        mFontFeatureSettings = value;
        doUpdate();
    }

    /** Returns TrueType or OpenType font variation settings. */
    @Nullable
    public String getFontVariationSettings() {
        return mFontVariationSettings;
    }

    /**
     * Sets TrueType or OpenType font variation settings.
     * @param value Font variation settings. You can pass null or empty string as no variation
     *              settings. This value may be null
     */
    public void setFontVariationSettings(@Nullable String value) {
        mFontVariationSettings = value;
        doUpdate();
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setText(mText);
    }

    @Override
    public void onPopulateAccessibilityEvent(@NonNull AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mText);
    }

    /**
     * Nested class to avoid verification errors for methods induces in API level 26
     */
    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {
        }

        static void paintSetFontVariationSettings(
                Paint paint,
                @Nullable String fontVariationSettings) {
            paint.setFontVariationSettings(fontVariationSettings);
        }
    }

    /**
     * Nested class to avoid verification errors for methods induces in API level 28
     */
    @RequiresApi(28)
    private static class Api28Impl {
        private Api28Impl() {
        }

        static Typeface createTypeface(@Nullable Typeface family, int weight, boolean italic) {
            return Typeface.create(family, weight, italic);
        }
    }

}
