/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.utils.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;

import androidx.constraintlayout.motion.widget.Debug;
import androidx.constraintlayout.motion.widget.FloatLayout;
import androidx.constraintlayout.widget.R;
import androidx.core.widget.TextViewCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * This class is designed to create complex animated single line text in MotionLayout.
 * Its API are designed with animation in mine.
 * for example it uses setTextPanX(float x) where 0 is centered -1 is left +1 is right
 *
 * It supports the following features:
 * <ul>
 * <li>color outlines</li>
 * <li>Textured text</li>
 * <li>Blured Textured Text</li>
 * <li>Scrolling of Texture in text</li>
 * <li>PanX, PanY instead of Gravity</li>
 * </ul>
 */
public class MotionLabel extends View implements FloatLayout {
    static final String TAG = "MotionLabel";
    TextPaint mPaint = new TextPaint();
    Path mPath = new Path();
    private int mTextFillColor = 0xFFFF;
    private int mTextOutlineColor = 0xFFFF;
    private boolean mUseOutline = false;
    private float mRoundPercent = 0; // rounds the corners as a percent
    private float mRound = Float.NaN; // rounds the corners in dp if NaN RoundPercent is in effect
    ViewOutlineProvider mViewOutlineProvider;
    RectF mRect;

    private float mTextSize = 48;

    private float mBaseTextSize = Float.NaN;
    private int mStyleIndex;
    private int mTypefaceIndex;
    private float mTextOutlineThickness = 0;
    private String mText = "Hello World";
    boolean mNotBuilt = true;
    private Rect mTextBounds = new Rect();
    private int mPaddingLeft = 1;
    private int mPaddingRight = 1;
    private int mPaddingTop = 1;
    private int mPaddingBottom = 1;
    private String mFontFamily;
    //    private StaticLayout mStaticLayout;
    private Layout mLayout;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;
    private int mGravity = Gravity.TOP | Gravity.START;
    private int mAutoSizeTextType = TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    private boolean mAutoSize = false; // decided during measure
    private float mDeltaLeft;
    private float mFloatWidth, mFloatHeight;
    private Drawable mTextBackground;
    Matrix mOutlinePositionMatrix;
    private Bitmap mTextBackgroundBitmap;
    private BitmapShader mTextShader;
    private Matrix mTextShaderMatrix;
    private float mTextureHeight = Float.NaN;
    private float mTextureWidth = Float.NaN;
    private float mTextPanX = 0;
    private float mTextPanY = 0;
    Paint mPaintCache = new Paint();
    private int mTextureEffect = 0;

    public MotionLabel(Context context) {
        super(context);
        init(context, null);
    }

    public MotionLabel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MotionLabel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setUpTheme(context);

        if (attrs != null) {
            TypedArray a = getContext()
                    .obtainStyledAttributes(attrs, R.styleable.MotionLabel);
            final int count = a.getIndexCount();

            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MotionLabel_android_text) {
                    setText(a.getText(attr));
                } else if (attr == R.styleable.MotionLabel_android_fontFamily) {
                    mFontFamily = a.getString(attr);
                } else if (attr == R.styleable.MotionLabel_scaleFromTextSize) {
                    mBaseTextSize = a.getDimensionPixelSize(attr, (int) mBaseTextSize);
                } else if (attr == R.styleable.MotionLabel_android_textSize) {
                    mTextSize = a.getDimensionPixelSize(attr, (int) mTextSize);
                } else if (attr == R.styleable.MotionLabel_android_textStyle) {
                    mStyleIndex = a.getInt(attr, mStyleIndex);
                } else if (attr == R.styleable.MotionLabel_android_typeface) {
                    mTypefaceIndex = a.getInt(attr, mTypefaceIndex);
                } else if (attr == R.styleable.MotionLabel_android_textColor) {
                    mTextFillColor = a.getColor(attr, mTextFillColor);
                } else if (attr == R.styleable.MotionLabel_borderRound) {
                    mRound = a.getDimension(attr, mRound);
                    setRound(mRound);
                } else if (attr == R.styleable.MotionLabel_borderRoundPercent) {
                    mRoundPercent = a.getFloat(attr, mRoundPercent);
                    setRoundPercent(mRoundPercent);
                } else if (attr == R.styleable.MotionLabel_android_gravity) {
                    setGravity(a.getInt(attr, -1));
                } else if (attr == R.styleable.MotionLabel_android_autoSizeTextType) {
                    mAutoSizeTextType = a.getInt(attr, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                } else if (attr == R.styleable.MotionLabel_textOutlineColor) {
                    mTextOutlineColor = a.getInt(attr, mTextOutlineColor);
                    mUseOutline = true;
                } else if (attr == R.styleable.MotionLabel_textOutlineThickness) {
                    mTextOutlineThickness = a.getDimension(attr, mTextOutlineThickness);
                    mUseOutline = true;
                } else if (attr == R.styleable.MotionLabel_textBackground) {
                    mTextBackground = a.getDrawable(attr);
                    mUseOutline = true;
                } else if (attr == R.styleable.MotionLabel_textBackgroundPanX) {
                    mBackgroundPanX = a.getFloat(attr, mBackgroundPanX);
                } else if (attr == R.styleable.MotionLabel_textBackgroundPanY) {
                    mBackgroundPanY = a.getFloat(attr, mBackgroundPanY);
                } else if (attr == R.styleable.MotionLabel_textPanX) {
                    mTextPanX = a.getFloat(attr, mTextPanX);
                } else if (attr == R.styleable.MotionLabel_textPanY) {
                    mTextPanY = a.getFloat(attr, mTextPanY);
                } else if (attr == R.styleable.MotionLabel_textBackgroundRotate) {
                    mRotate = a.getFloat(attr, mRotate);
                } else if (attr == R.styleable.MotionLabel_textBackgroundZoom) {
                    mZoom = a.getFloat(attr, mZoom);
                } else if (attr == R.styleable.MotionLabel_textureHeight) {
                    mTextureHeight = a.getDimension(attr, mTextureHeight);
                } else if (attr == R.styleable.MotionLabel_textureWidth) {
                    mTextureWidth = a.getDimension(attr, mTextureWidth);
                } else if (attr == R.styleable.MotionLabel_textureEffect) {
                    mTextureEffect = a.getInt(attr, mTextureEffect);
                }
            }
            a.recycle();
        }

        setupTexture();
        setupPath();
    }

    Bitmap blur(Bitmap bitmapOriginal, int factor) {
        int w = bitmapOriginal.getWidth();
        int h = bitmapOriginal.getHeight();

        w /= 2;
        h /= 2;

        Bitmap ret = Bitmap.createScaledBitmap(bitmapOriginal,
                w, h, true);
        for (int i = 0; i < factor; i++) {
            if (w < 32 || h < 32) {
                break;
            }
            w /= 2;
            h /= 2;
            ret = Bitmap.createScaledBitmap(ret, w, h, true);
        }
        return ret;
    }

    private void setupTexture() {
        if (mTextBackground != null) {
            mTextShaderMatrix = new Matrix();
            int iw = mTextBackground.getIntrinsicWidth();
            int ih = mTextBackground.getIntrinsicHeight();
            if (iw <= 0) {
                int w = getWidth();
                if (w == 0) {
                    w = (Float.isNaN(mTextureWidth) ? 128 : (int) mTextureWidth);
                }
                iw = w;
            }
            if (ih <= 0) {
                int h = getHeight();
                if (h == 0) {
                    h = (Float.isNaN(mTextureHeight) ? 128 : (int) mTextureHeight);
                }
                ih = h;
            }

            if (mTextureEffect != 0) {
                iw /= 2;
                ih /= 2;
            }
            mTextBackgroundBitmap = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mTextBackgroundBitmap);

            mTextBackground.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            mTextBackground.setFilterBitmap(true);
            mTextBackground.draw(canvas);
            if (mTextureEffect != 0) {
                mTextBackgroundBitmap = blur(mTextBackgroundBitmap, 4);
            }
            mTextShader = new BitmapShader(mTextBackgroundBitmap,
                    Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        }
    }

    private void adjustTexture(float l, float t, float r, float b) {
        if (mTextShaderMatrix == null) {
            return;
        }

        mFloatWidth = r - l;
        mFloatHeight = b - t;
        updateShaderMatrix();
    }

    /**
     * Sets the horizontal alignment of the text and the
     * vertical gravity that will be used when there is extra space
     * in the TextView beyond what is required for the text itself.
     *
     * @attr ref android.R.styleable#TextView_gravity
     * @see android.view.Gravity
     */
    @SuppressLint("RtlHardcoded")
    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }
        @SuppressWarnings("unused")
        boolean newLayout = false;
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
                != (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)) {
            newLayout = true;
        }
        if (gravity != mGravity) {
            invalidate();
        }

        mGravity = gravity;
        switch (mGravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.TOP:
                mTextPanY = -1;
                break;
            case Gravity.BOTTOM:
                mTextPanY = 1;
                break;
            default:
                mTextPanY = 0;
        }

        switch (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
            case Gravity.START:
            case Gravity.LEFT:
                mTextPanX = -1;
                break;
            case Gravity.END:
            case Gravity.RIGHT:
                mTextPanX = 1;
                break;
            default:
                mTextPanX = 0;
        }
    }

    private float getHorizontalOffset() {
        float scale = Float.isNaN(mBaseTextSize) ? 1.0f : mTextSize / mBaseTextSize;

        float textWidth = scale * mPaint.measureText(mText, 0, mText.length());
        float boxWidth = (Float.isNaN(mFloatWidth) ? getMeasuredWidth() : mFloatWidth)
                - getPaddingLeft()
                - getPaddingRight();
        return (boxWidth - textWidth) * (1 + mTextPanX) / 2.f;
    }

    private float getVerticalOffset() {
        float scale = Float.isNaN(mBaseTextSize) ? 1.0f : mTextSize / mBaseTextSize;

        Paint.FontMetrics fm = mPaint.getFontMetrics();

        float boxHeight = (Float.isNaN(mFloatHeight) ? getMeasuredHeight() : mFloatHeight)
                - getPaddingTop()
                - getPaddingBottom();

        float textHeight = scale * (fm.descent - fm.ascent);
        return (boxHeight - textHeight) * (1 - mTextPanY) / 2 -  (scale * fm.ascent);
    }

    private void setUpTheme(Context context) {
        TypedValue typedValue = new TypedValue();
        final Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true);
        mPaint.setColor(mTextFillColor = typedValue.data);
    }

    /**
     * set text
     * @param text
     */
    public void setText(CharSequence text) {
        mText = text.toString();
        invalidate();
    }

    void setupPath() {
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();
        setTypefaceFromAttrs(mFontFamily, mTypefaceIndex, mStyleIndex);
        mPaint.setColor(mTextFillColor);
        mPaint.setStrokeWidth(mTextOutlineThickness);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setFlags(Paint.SUBPIXEL_TEXT_FLAG);
        setTextSize(mTextSize);
        mPaint.setAntiAlias(true);
    }

    void buildShape(float scale) {
        if (!mUseOutline && scale == 1.0f) {
            return;
        }
        mPath.reset();
        String str = mText;
        int len = str.length();
        mPaint.getTextBounds(str, 0, len, mTextBounds);
        mPaint.getTextPath(str, 0, len, 0, 0, mPath);
        if (scale != 1.0f) {
            Log.v(TAG, Debug.getLoc() + " scale " + scale);
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            mPath.transform(matrix);
        }
        mTextBounds.right--;
        mTextBounds.left++;
        mTextBounds.bottom++;
        mTextBounds.top--;

        RectF rect = new RectF();
        rect.bottom = getHeight();
        rect.right = getWidth();
        mNotBuilt = false;
    }

    Rect mTempRect;
    Paint mTempPaint;
    float mPaintTextSize;

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        boolean normalScale = Float.isNaN(mBaseTextSize);
        float scaleText = normalScale ? 1 : mTextSize / mBaseTextSize;
        mFloatWidth = r - l;
        mFloatHeight = b - t;
        if (mAutoSize) {

            if (mTempRect == null) {
                mTempPaint = new Paint();
                mTempRect = new Rect();
                mTempPaint.set(mPaint);
                mPaintTextSize = mTempPaint.getTextSize();
            }

            mTempPaint.getTextBounds(mText, 0, mText.length(), mTempRect);
            int tw = mTempRect.width();
            int th = (int) (1.3f * mTempRect.height());

            float vw = mFloatWidth - mPaddingRight - mPaddingLeft;
            float vh = mFloatHeight - mPaddingBottom - mPaddingTop;
            if (normalScale) {
                if (tw * vh > th * vw) { // width limited tw/vw > th/vh
                    mPaint.setTextSize((mPaintTextSize * vw) / tw);
                } else { // height limited
                    mPaint.setTextSize((mPaintTextSize * vh) / th);
                }
            } else {
                scaleText = (tw * vh > th * vw) ? vw / (float) tw : vh / (float) th;
            }
        }
        if (mUseOutline || !normalScale) {
            adjustTexture(l, t, r, b);
            buildShape(scaleText);
        }
    }

    @Override
    public void layout(float l, float t, float r, float b) {
        mDeltaLeft = l - (int) (0.5f + l);
        int w = (int) (0.5f + r) - (int) (0.5f + l);
        int h = (int) (0.5f + b) - (int) (0.5f + t);
        mFloatWidth = r - l;
        mFloatHeight = b - t;
        adjustTexture(l, t, r, b);
        if (getMeasuredHeight() != h || getMeasuredWidth() != w) {
            int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
            int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
            measure(widthMeasureSpec, heightMeasureSpec);
            super.layout((int) (0.5f + l), (int) (0.5f + t), (int) (0.5f + r), (int) (0.5f + b));
        } else {
            super.layout((int) (0.5f + l), (int) (0.5f + t), (int) (0.5f + r), (int) (0.5f + b));
        }
        if (mAutoSize) {
            if (mTempRect == null) {
                mTempPaint = new Paint();
                mTempRect = new Rect();
                mTempPaint.set(mPaint);
                mPaintTextSize = mTempPaint.getTextSize();
            }
            mFloatWidth = r - l;
            mFloatHeight = b - t;

            mTempPaint.getTextBounds(mText, 0, mText.length(), mTempRect);
            int tw = mTempRect.width();
            float th = 1.3f * mTempRect.height();
            float vw = r - l - mPaddingRight - mPaddingLeft;
            float vh = b - t - mPaddingBottom - mPaddingTop;
            if (tw * vh > th * vw) { // width limited tw/vw > th/vh
                mPaint.setTextSize((mPaintTextSize * vw) / tw);
            } else { // height limited
                mPaint.setTextSize((mPaintTextSize * vh) / th);
            }
            if (mUseOutline || !Float.isNaN(mBaseTextSize)) {
                buildShape(Float.isNaN(mBaseTextSize) ? 1.0f : mTextSize / mBaseTextSize);
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float scale = Float.isNaN(mBaseTextSize) ? 1.0f : mTextSize / mBaseTextSize;
        super.onDraw(canvas);
        if (!mUseOutline && scale == 1.0f) {
            float x = mPaddingLeft + getHorizontalOffset();
            float y = mPaddingTop + getVerticalOffset();
            canvas.drawText(mText, mDeltaLeft + x, y, mPaint);
            return;
        }
        if (mNotBuilt) {
            buildShape(scale);
        }
        if (mOutlinePositionMatrix == null) {
            mOutlinePositionMatrix = new Matrix();
        }
        if (mUseOutline) {
            mPaintCache.set(mPaint);
            mOutlinePositionMatrix.reset();
            float x = mPaddingLeft + getHorizontalOffset();
            float y = mPaddingTop + getVerticalOffset();
            mOutlinePositionMatrix.postTranslate(x, y);
            mOutlinePositionMatrix.preScale(scale, scale);
            mPath.transform(mOutlinePositionMatrix);

            if (mTextShader != null) {
                mPaint.setFilterBitmap(true);
                mPaint.setShader(mTextShader);
            } else {
                mPaint.setColor(mTextFillColor);
            }
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeWidth(mTextOutlineThickness);
            canvas.drawPath(mPath, mPaint);
            if (mTextShader != null) {
                mPaint.setShader(null);
            }
            mPaint.setColor(mTextOutlineColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mTextOutlineThickness);
            canvas.drawPath(mPath, mPaint);

            mOutlinePositionMatrix.reset();
            mOutlinePositionMatrix.postTranslate(-x, -y);
            mPath.transform(mOutlinePositionMatrix);
            mPaint.set(mPaintCache);
        } else {
            float x = mPaddingLeft + getHorizontalOffset();
            float y = mPaddingTop + getVerticalOffset();
            mOutlinePositionMatrix.reset();
            mOutlinePositionMatrix.preTranslate(x, y);
            mPath.transform(mOutlinePositionMatrix);
            mPaint.setColor(mTextFillColor);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeWidth(mTextOutlineThickness);
            canvas.drawPath(mPath, mPaint);
            mOutlinePositionMatrix.reset();
            mOutlinePositionMatrix.preTranslate(-x, -y);
            mPath.transform(mOutlinePositionMatrix);
        }
    }

    /**
     * Set outline thickness
     * @param width
     */
    public void setTextOutlineThickness(float width) {
        mTextOutlineThickness = width;
        mUseOutline = true;
        if (Float.isNaN(mTextOutlineThickness)) {
            mTextOutlineThickness = 1;
            mUseOutline = false;
        }
        invalidate();
    }

    /**
     * Set the color of the text.
     *
     * @param color the color of the text
     */
    public void setTextFillColor(int color) {
        mTextFillColor = color;
        invalidate();
    }

    /**
     * Sets the color of the text outline.
     *
     * @param color the color of the outline of the text
     */
    public void setTextOutlineColor(int color) {
        mTextOutlineColor = color;
        mUseOutline = true;
        invalidate();
    }

    private void setTypefaceFromAttrs(String familyName, int typefaceIndex, int styleIndex) {
        Typeface tf = null;
        if (familyName != null) {
            tf = Typeface.create(familyName, styleIndex);
            if (tf != null) {
                setTypeface(tf);
                return;
            }
        }
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;
            case SERIF:
                tf = Typeface.SERIF;
                break;
            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
        }

        if (styleIndex > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(styleIndex);
            } else {
                tf = Typeface.create(tf, styleIndex);
            }
            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = styleIndex & ~typefaceStyle;
            mPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mPaint.setFakeBoldText(false);
            mPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    /**
     * set the typeface
     * @param tf
     */
    public void setTypeface(Typeface tf) {
        if (!Objects.equals(mPaint.getTypeface(), tf)) {
            mPaint.setTypeface(tf);
            if (mLayout != null) {
                mLayout = null;
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * @return the current typeface and style in which the text is being
     * displayed.
     * @see #setTypeface(Typeface)
     */
    public Typeface getTypeface() {
        return mPaint.getTypeface();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int width = widthSize;
        int height = heightSize;

        mAutoSize = false;

        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();
        if (widthMode != View.MeasureSpec.EXACTLY || heightMode != View.MeasureSpec.EXACTLY) {
            mPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
            // WIDTH
            if (widthMode != View.MeasureSpec.EXACTLY) {
                width = (int) (mTextBounds.width() + 0.99999f);
            }
            width += mPaddingLeft + mPaddingRight;

            if (heightMode != View.MeasureSpec.EXACTLY) {
                int desired = (int) (mPaint.getFontMetricsInt(null) + 0.99999f);
                if (heightMode == View.MeasureSpec.AT_MOST) {
                    height = Math.min(height, desired);
                } else {
                    height = desired;
                }
                height += mPaddingTop + mPaddingBottom;
            }
        } else {
            if (mAutoSizeTextType != TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE) {
                mAutoSize = true;
            }

        }

        setMeasuredDimension(width, height);
    }

    //============================= rounding ==============================================

    /**
     * Set the corner radius of curvature  as a fraction of the smaller side.
     * For squares 1 will result in a circle
     *
     * @param round the radius of curvature as a fraction of the smaller width
     */
    public void setRoundPercent(float round) {
        boolean change = (mRoundPercent != round);
        mRoundPercent = round;
        if (mRoundPercent != 0.0f) {
            if (mPath == null) {
                mPath = new Path();
            }
            if (mRect == null) {
                mRect = new RectF();
            }
            if (mViewOutlineProvider == null) {
                mViewOutlineProvider = new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int w = getWidth();
                        int h = getHeight();
                        float r = Math.min(w, h) * mRoundPercent / 2;
                        outline.setRoundRect(0, 0, w, h, r);
                    }
                };
                setOutlineProvider(mViewOutlineProvider);
            }
            setClipToOutline(true);
            int w = getWidth();
            int h = getHeight();
            float r = Math.min(w, h) * mRoundPercent / 2;
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, r, r, Path.Direction.CW);
        } else {
            setClipToOutline(false);
        }
        if (change) {
            invalidateOutline();
        }
    }

    /**
     * Set the corner radius of curvature
     *
     * @param round the radius of curvature  NaN = default meaning roundPercent in effect
     */
    public void setRound(float round) {
        if (Float.isNaN(round)) {
            mRound = round;
            float tmp = mRoundPercent;
            mRoundPercent = -1;
            setRoundPercent(tmp); // force eval of roundPercent
            return;
        }
        boolean change = (mRound != round);
        mRound = round;

        if (mRound != 0.0f) {
            if (mPath == null) {
                mPath = new Path();
            }
            if (mRect == null) {
                mRect = new RectF();
            }
            if (mViewOutlineProvider == null) {
                mViewOutlineProvider = new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        int w = getWidth();
                        int h = getHeight();
                        outline.setRoundRect(0, 0, w, h, mRound);
                    }
                };
                setOutlineProvider(mViewOutlineProvider);
            }
            setClipToOutline(true);

            int w = getWidth();
            int h = getHeight();
            mRect.set(0, 0, w, h);
            mPath.reset();
            mPath.addRoundRect(mRect, mRound, mRound, Path.Direction.CW);
        } else {
            setClipToOutline(false);
        }
        if (change) {
            invalidateOutline();
        }

    }

    /**
     * Get the fractional corner radius of curvature.
     *
     * @return Fractional radius of curvature with respect to smallest size
     */
    public float getRoundPercent() {
        return mRoundPercent;
    }

    /**
     * Get the corner radius of curvature NaN = RoundPercent in effect.
     *
     * @return Radius of curvature
     */
    public float getRound() {
        return mRound;
    }
    //===========================================================================================

    /**
     * set text size
     *
     * @param size the size of the text
     * @see Paint#setTextSize(float)
     */
    public void setTextSize(float size) {
        mTextSize = size;
        mPaint.setTextSize(Float.isNaN(mBaseTextSize) ? size : mBaseTextSize);
        buildShape(Float.isNaN(mBaseTextSize) ? 1.0f : mTextSize / mBaseTextSize);
        requestLayout();
        invalidate();
    }

    public int getTextOutlineColor() {
        return mTextOutlineColor;
    }

    // ============================ TextureTransformLogic ===============================//
    float mBackgroundPanX = Float.NaN;
    float mBackgroundPanY = Float.NaN;
    float mZoom = Float.NaN;
    float mRotate = Float.NaN;

    /**
     * Gets the pan from the center
     * pan of 1 the image is "all the way to the right"
     * if the images width is greater than the screen width,
     * pan = 1 results in the left edge lining up
     * if the images width is less than the screen width,
     * pan = 1 results in the right edges lining up
     * if image width == screen width it does nothing
     *
     * @return the pan in X. Where 0 is centered = Float. NaN if not set
     */
    public float getTextBackgroundPanX() {
        return mBackgroundPanX;
    }

    /**
     * gets the pan from the center
     * pan of 1 the image is "all the way to the bottom"
     * if the images width is greater than the screen height,
     * pan = 1 results in the bottom edge lining up
     * if the images width is less than the screen height,
     * pan = 1 results in the top edges lining up
     * if image height == screen height it does nothing
     *
     * @return pan in y. Where 0 is centered NaN if not set
     */
    public float getTextBackgroundPanY() {
        return mBackgroundPanY;
    }

    /**
     * gets the zoom where 1 scales the image just enough to fill the view
     *
     * @return the zoom factor
     */
    public float getTextBackgroundZoom() {
        return mZoom;
    }

    /**
     * gets the rotation
     *
     * @return the rotation in degrees
     */
    public float getTextBackgroundRotate() {
        return mRotate;
    }

    /**
     * sets the pan from the center
     * pan of 1 the image is "all the way to the right"
     * if the images width is greater than the screen width,
     * pan = 1 results in the left edge lining up
     * if the images width is less than the screen width,
     * pan = 1 results in the right edges lining up
     * if image width == screen width it does nothing
     *
     * @param pan sets the pan in X. Where 0 is centered
     */
    public void setTextBackgroundPanX(float pan) {
        mBackgroundPanX = pan;
        updateShaderMatrix();
        invalidate();
    }

    /**
     * sets the pan from the center
     * pan of 1 the image is "all the way to the bottom"
     * if the images width is greater than the screen height,
     * pan = 1 results in the bottom edge lining up
     * if the images width is less than the screen height,
     * pan = 1 results in the top edges lining up
     * if image height == screen height it does nothing
     *
     * @param pan sets the pan in X. Where 0 is centered
     */
    public void setTextBackgroundPanY(float pan) {
        mBackgroundPanY = pan;
        updateShaderMatrix();
        invalidate();
    }

    /**
     * sets the zoom where 1 scales the image just enough to fill the view
     *
     * @param zoom the zoom factor
     */
    public void setTextBackgroundZoom(float zoom) {
        mZoom = zoom;
        updateShaderMatrix();
        invalidate();
    }

    /**
     * sets the rotation angle of the image in degrees
     *
     * @param rotation angle in degrees
     */
    public void setTextBackgroundRotate(float rotation) {
        mRotate = rotation;
        updateShaderMatrix();
        invalidate();
    }

    private void updateShaderMatrix() {
        float panX = Float.isNaN(mBackgroundPanX) ? 0 : mBackgroundPanX;
        float panY = Float.isNaN(mBackgroundPanY) ? 0 : mBackgroundPanY;
        float zoom = Float.isNaN(mZoom) ? 1 : mZoom;
        float rota = Float.isNaN(mRotate) ? 0 : mRotate;

        mTextShaderMatrix.reset();
        float iw = mTextBackgroundBitmap.getWidth();
        float ih = mTextBackgroundBitmap.getHeight();
        float sw = Float.isNaN(mTextureWidth) ? mFloatWidth : mTextureWidth;
        float sh = Float.isNaN(mTextureHeight) ? mFloatHeight : mTextureHeight;

        float scale = zoom * ((iw * sh < ih * sw) ? sw / iw : sh / ih);
        mTextShaderMatrix.postScale(scale, scale);
        float gapx = sw - scale * iw;
        float gapy = sh - scale * ih;
        if (!Float.isNaN(mTextureHeight)) {
            gapy = mTextureHeight / 2;
        }
        if (!Float.isNaN(mTextureWidth)) {
            gapx = mTextureWidth / 2;
        }
        float tx = 0.5f * (panX * gapx + sw - (scale * iw));
        float ty = 0.5f * (panY * gapy + sh - (scale * ih));

        mTextShaderMatrix.postTranslate(tx, ty);
        mTextShaderMatrix.postRotate(rota, sw / 2, sh / 2);
        mTextShader.setLocalMatrix(mTextShaderMatrix);
    }

    /**
     * Pan the Texture in the text in the x axis.
     *
     * @return pan of the Text -1 = left 0 = center +1 = right
     */
    public float getTextPanX() {
        return mTextPanX;
    }

    /**
     * Pan the Texture in the text in the x axis.
     *
     * @param textPanX pan of the Text -1 = left 0 = center +1 = right
     */
    public void setTextPanX(float textPanX) {
        mTextPanX = textPanX;
        invalidate();
    }

    /**
     * Pan the Texture in the text in the y axis.
     *
     * @return the pan value 0 being centered in the center of screen.
     */
    public float getTextPanY() {
        return mTextPanY;
    }

    /**
     * Pan the Texture in the text in the y axis.
     *
     * @param textPanY pan of the Text -1 = top 0 = center +1 = bottom
     */
    public void setTextPanY(float textPanY) {
        mTextPanY = textPanY;
        invalidate();
    }

    /**
     * Pan the Texture in the text in the y axis.
     *
     * @return pan of the Text -1 = top 0 = center +1 = bottom
     */
    public float getTextureHeight() {
        return mTextureHeight;
    }

    /**
     * set the height of the texture. Setting Float.NaN is the default Use the view size.
     *
     * @param mTextureHeight the height of the texture
     */
    public void setTextureHeight(float mTextureHeight) {
        this.mTextureHeight = mTextureHeight;
        updateShaderMatrix();
        invalidate();
    }

    /**
     * get the width of the texture. Setting Float.NaN is the default Use the view size.
     *
     * @return the width of the texture
     */
    public float getTextureWidth() {
        return mTextureWidth;
    }

    /**
     * set the width of the texture. Setting Float.NaN is the default Use the view size
     *
     * @param mTextureWidth set the width of the texture Float.NaN clears setting
     */
    public void setTextureWidth(float mTextureWidth) {
        this.mTextureWidth = mTextureWidth;
        updateShaderMatrix();
        invalidate();
    }

    /**
     * if set the font is rendered to polygons at this size and then scaled to the size set by
     * textSize.
     *
     * @return size to pre render font or NaN if not used.
     */
    public float getScaleFromTextSize() {
        return mBaseTextSize;
    }

    /**
     * if set the font is rendered to polygons at this size and then scaled to the size set by
     * textSize.
     * This allows smooth efficient animation of fonts size.
     *
     * @param size the size to pre render the font or NaN if not used.
     */
    public void setScaleFromTextSize(float size) {
        this.mBaseTextSize = size;
    }

}
