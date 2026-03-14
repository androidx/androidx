/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.core.operations.layout.managers;

import static androidx.compose.remote.core.documentation.DocumentedOperation.BOOLEAN;
import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT_ARRAY;

import static java.lang.Math.floor;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.RcPlatformServices;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.measure.ComponentMeasure;
import androidx.compose.remote.core.operations.layout.measure.MeasurePass;
import androidx.compose.remote.core.operations.layout.measure.Size;
import androidx.compose.remote.core.operations.paint.PaintBundle;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.operations.utilities.touch.CommandParameters;
import androidx.compose.remote.core.semantics.AccessibleComponent;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CoreText extends LayoutManager implements VariableSupport, AccessibleComponent {

    // Alignment values
    public static final int TEXT_ALIGN_LEFT = 1;
    public static final int TEXT_ALIGN_RIGHT = 2;
    public static final int TEXT_ALIGN_CENTER = 3;
    public static final int TEXT_ALIGN_JUSTIFY = 4;
    public static final int TEXT_ALIGN_START = 5;
    public static final int TEXT_ALIGN_END = 6;

    // Overflow behavior
    public static final int OVERFLOW_CLIP = 1;
    public static final int OVERFLOW_VISIBLE = 2;
    public static final int OVERFLOW_ELLIPSIS = 3;
    public static final int OVERFLOW_START_ELLIPSIS = 4;
    public static final int OVERFLOW_MIDDLE_ELLIPSIS = 5;

    // Linebreak quality
    public static final int BREAK_STRATEGY_SIMPLE = 0;
    public static final int BREAK_STRATEGY_HIGH_QUALITY = 1;
    public static final int BREAK_STRATEGY_BALANCED = 2;

    // Hyphenation frequency
    public static final int HYPHENATION_FREQUENCY_NONE = 0;
    public static final int HYPHENATION_FREQUENCY_NORMAL = 1;
    public static final int HYPHENATION_FREQUENCY_FULL = 2;
    public static final int HYPHENATION_FREQUENCY_NORMAL_FAST = 3;
    public static final int HYPHENATION_FREQUENCY_FULL_FAST = 4;

    // Justification behavior
    public static final int JUSTIFICATION_MODE_NONE = 0;
    public static final int JUSTIFICATION_MODE_INTER_WORD = 1;
    public static final int JUSTIFICATION_MODE_INTER_CHARACTER = 2;

    private static final boolean DEBUG = false;
    private int[] mFontAxis;
    private float[] mFontAxisValues;

    private boolean mIsDynamicColorEnabled;

    private float mLetterSpacing = 0f;
    private float mLineHeightAdd = 0f;
    private float mLineHeightMultiplier = 1f;
    private boolean mAutosize = false;
    private boolean mUnderline = false;
    private boolean mStrikethrough = false;

    private int mTextId = -1;
    private int mColor = 0;
    private int mColorId = -1;
    private int mColorValue = -1;
    private float mFontSize = TextStyle.DEFAULT_FONT_SIZE;
    private float mMinFontSize = -1f;
    private float mMaxFontSize = -1f;
    private float mFontSizeValue = TextStyle.DEFAULT_FONT_SIZE;
    private int mFontStyle = 0;
    private float mFontWeight = TextStyle.DEFAULT_FONT_WEIGHT;
    private float mFontWeightValue = TextStyle.DEFAULT_FONT_WEIGHT;
    private int mFontFamilyId = -1;
    private int mTextAlign = 1;
    private int mTextAlignValue = -1;
    private int mOverflow = 1;
    private int mMaxLines = Integer.MAX_VALUE;

    private int mLineBreakStrategy = 0;
    private int mHyphenationFrequency = 0;
    private int mJustificationMode = 0;

    private int mTextStyleId = -1;

    private int mType = -1;
    private float mTextX;
    private float mTextY;
    private float mTextW = -1;
    private float mTextH = -1;

    private float mBaseline = 0f;

    private final Size mCachedSize = new Size(0f, 0f);

    /**
     * Apply the style to the component
     */
    public void applyStyle(@NonNull TextStyle style) {
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_COLOR, mColor)
                && style.mColor != null) {
            mColor = style.mColor;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_COLOR_ID, mColorId)
                && style.mColorId != null) {
            mColorId = style.mColorId;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_FONT_SIZE, mFontSize)
                && style.mFontSize != null) {
            mFontSize = style.mFontSize;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_MIN_FONT_SIZE, mMinFontSize)
                && style.mMinFontSize != null) {
            mMinFontSize = style.mMinFontSize;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_MAX_FONT_SIZE, mMaxFontSize)
                && style.mMaxFontSize != null) {
            mMaxFontSize = style.mMaxFontSize;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_FONT_STYLE, mFontStyle)
                && style.mFontStyle != null) {
            mFontStyle = style.mFontStyle;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_FONT_WEIGHT, mFontWeight)
                && style.mFontWeight != null) {
            mFontWeight = style.mFontWeight;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_FONT_FAMILY, mFontFamilyId)
                && style.mFontFamilyId != null) {
            mFontFamilyId = style.mFontFamilyId;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_TEXT_ALIGN, mTextAlign)
                && style.mTextAlign != null) {
            mTextAlign = style.mTextAlign;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_OVERFLOW, mOverflow)
                && style.mOverflow != null) {
            mOverflow = style.mOverflow;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_MAX_LINES, mMaxLines)
                && style.mMaxLines != null) {
            mMaxLines = style.mMaxLines;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_LETTER_SPACING, mLetterSpacing)
                && style.mLetterSpacing != null) {
            mLetterSpacing = style.mLetterSpacing;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_LINE_HEIGHT_ADD, mLineHeightAdd)
                && style.mLineHeightAdd != null) {
            mLineHeightAdd = style.mLineHeightAdd;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_LINE_HEIGHT_MULTIPLIER,
                mLineHeightMultiplier) && style.mLineHeightMultiplier != null) {
            mLineHeightMultiplier = style.mLineHeightMultiplier;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_BREAK_STRATEGY, mLineBreakStrategy)
                && style.mLineBreakStrategy != null) {
            mLineBreakStrategy = style.mLineBreakStrategy;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_HYPHENATION_FREQUENCY,
                mHyphenationFrequency) && style.mHyphenationFrequency != null) {
            mHyphenationFrequency = style.mHyphenationFrequency;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_JUSTIFICATION_MODE, mJustificationMode)
                && style.mJustificationMode != null) {
            mJustificationMode = style.mJustificationMode;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_UNDERLINE, mUnderline)
                && style.mUnderline != null) {
            mUnderline = style.mUnderline;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_STRIKETHROUGH, mStrikethrough)
                && style.mStrikethrough != null) {
            mStrikethrough = style.mStrikethrough;
        }
        if (mFontAxis == null && style.mFontAxis != null) {
            mFontAxis = style.mFontAxis;
            mFontAxisValues = style.mFontAxisValues;
        }
        if (TextStyle.PARAMETERS.isDefault(TextStyle.P_AUTOSIZE, mAutosize)
                && style.mAutosize != null) {
            mAutosize = style.mAutosize;
        }
        mIsDynamicColorEnabled = mColorId != -1;
    }


    @Nullable
    private String mCachedString;
    @Nullable
    private String mNewString;

    private int mFlags;

    RcPlatformServices.ComputedTextLayout mComputedTextLayout;

    @Nullable
    @Override
    public Integer getTextId() {
        return mTextId;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (mTextId != -1) {
            context.listensTo(mTextId, this);
        }
        if (Float.isNaN(mFontSize)) {
            context.listensTo(Utils.idFromNan(mFontSize), this);
        }
        if (Float.isNaN(mFontWeight)) {
            context.listensTo(Utils.idFromNan(mFontWeight), this);
        }
        if (mIsDynamicColorEnabled) {
            context.listensTo(mColorId, this);
        }
    }

    private static short getFlagsFromTextAlign(int textAlign) {
        return (short) (textAlign >>> 16);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mTextStyleId != -1) {
            Object styleObj = context.getObject(mTextStyleId);
            if (styleObj instanceof TextStyle) {
                applyStyle((TextStyle) styleObj);
            }
        }
        mFontSizeValue =
                Float.isNaN(mFontSize)
                        ? context.getFloat(Utils.idFromNan(mFontSize))
                        : mFontSize;
        mFontWeightValue =
                Float.isNaN(mFontWeight)
                        ? context.getFloat(Utils.idFromNan(mFontWeight))
                        : mFontWeight;
        mTextAlignValue = (short) (mTextAlign & 0xFFFF);
        if (mIsDynamicColorEnabled) {
            int color = context.getColor(mColorId);
            if (color != mColorValue)  {
                invalidateMeasure();
            }
            mColorValue = color;
        } else {
            mColorValue = mColor;
        }

        String cachedString = context.getText(mTextId);
        if (cachedString != null && cachedString.equalsIgnoreCase(mCachedString) && mType != -1) {
            if (mMeasureFontSize != mFontSizeValue
                    || mMeasureFontWeight != mFontWeightValue) {
                invalidateMeasure();
            }
            return;
        }
        mNewString = cachedString;
        if (mType == -1) {
            if (mFontFamilyId != -1) {
                String fontFamily = context.getText(mFontFamilyId);
                if (fontFamily != null) {
                    if (fontFamily.equalsIgnoreCase("default")) {
                        mType = 0;
                    } else if (fontFamily.equalsIgnoreCase("sans-serif")) {
                        mType = 1;
                    } else if (fontFamily.equalsIgnoreCase("serif")) {
                        mType = 2;
                    } else if (fontFamily.equalsIgnoreCase("monospace")) {
                        mType = 3;
                    } else {
                        mType = mFontFamilyId;
                    }
                }
            } else {
                mType = 0;
            }
        }

        if (mHorizontalScrollDelegate != null) {
            mHorizontalScrollDelegate.reset();
        }
        if (mVerticalScrollDelegate != null) {
            mVerticalScrollDelegate.reset();
        }
        invalidateMeasure();
    }

    public CoreText(
            @Nullable Component parent,
            int componentId,
            int animationId,
            float x,
            float y,
            float width,
            float height,
            int textId,
            int color,
            int colorId,
            float fontSize,
            float minFontSize,
            float maxFontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            int textAlign,
            int overflow,
            int maxLines,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean useUnderline,
            boolean strikethrough,
            int @Nullable [] fontAxis,
            float @Nullable [] fontAxisValues,
            boolean autosize,
            int flags,
            int textStyleId) {
        super(parent, componentId, animationId, x, y, width, height);
        mTextId = textId;
        mColor = color;
        mColorId = colorId;
        mFontSize = fontSize;
        mMinFontSize = minFontSize;
        mMaxFontSize = maxFontSize;
        if (!Float.isNaN(mFontSize)) {
            mFontSizeValue = fontSize;
        }
        mIsDynamicColorEnabled = mColorId != -1;
        if (!mIsDynamicColorEnabled) {
            mColorValue = color;
        }
        mFontStyle = fontStyle;
        mFontWeight = fontWeight;
        mFontWeightValue = fontWeight;
        mFontFamilyId = fontFamilyId;
        mTextAlign = textAlign;
        mOverflow = overflow;
        mMaxLines = maxLines;
        mLetterSpacing = letterSpacing;
        mLineHeightAdd = lineHeightAdd;
        mLineHeightMultiplier = lineHeightMultiplier;
        mLineBreakStrategy = lineBreakStrategy;
        mHyphenationFrequency = hyphenationFrequency;
        mJustificationMode = justificationMode;
        mUnderline = useUnderline;
        mStrikethrough = strikethrough;
        mAutosize = autosize;
        mFontAxis = fontAxis;
        mFontAxisValues = fontAxisValues;
        mFlags = flags;
        mTextStyleId = textStyleId;
    }

    public CoreText(
            @Nullable Component parent,
            int componentId,
            int animationId,
            int textId,
            int color,
            int colorId,
            float fontSize,
            float minFontSize,
            float maxFontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            int textAlign,
            int overflow,
            int maxLines,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean useUnderline,
            boolean strikethrough,
            int @Nullable [] fontAxis,
            float @Nullable [] fontAxisValues,
            boolean autosize,
            int flags,
            int textStyleId) {
        this(
                parent,
                componentId,
                animationId,
                0,
                0,
                0,
                0,
                textId,
                color,
                colorId,
                fontSize,
                minFontSize,
                maxFontSize,
                fontStyle,
                fontWeight,
                fontFamilyId,
                textAlign,
                overflow,
                maxLines,
                letterSpacing,
                lineHeightAdd,
                lineHeightMultiplier,
                lineBreakStrategy,
                hyphenationFrequency,
                justificationMode,
                useUnderline,
                strikethrough,
                fontAxis,
                fontAxisValues,
                autosize,
                flags,
                textStyleId);
    }

    @NonNull
    public PaintBundle mPaint = new PaintBundle();

    @Override
    public float getAlignValue(@NonNull PaintContext context, float line) {
        if (Float.isNaN(line)) {
            int id = Utils.idFromNan(line);
            if (id == RemoteContext.ID_FIRST_BASELINE) {
                return mBaseline;
            }
            if (id == RemoteContext.ID_LAST_BASELINE) {
                // TODO add support for last baseline
                return mBaseline;
            }
            if (Utils.isVariable(line)) {
                return context.getContext().getFloat(Utils.idFromNan(line));
            }
            // unrecognized line value
            return 0f;
        }
        return line;
    }

    @Override
    public void paintingComponent(@NonNull PaintContext context) {
        Component prev = context.getContext().mLastComponent;
        RemoteContext remoteContext = context.getContext();
        remoteContext.mLastComponent = this;

        context.save();
        context.translate(mX, mY);
        if (mGraphicsLayerModifier != null) {
            context.startGraphicsLayer((int) getWidth(), (int) getHeight());
            mCachedAttributes.clear();
            mGraphicsLayerModifier.fillInAttributes(mCachedAttributes);
            context.setGraphicsLayer(mCachedAttributes);
        }
        mComponentModifiers.paint(context);
        float tx = mPaddingLeft;
        float ty = mPaddingTop;
        context.translate(tx, ty);

        //////////////////////////////////////////////////////////
        // Text content
        //////////////////////////////////////////////////////////
        context.savePaint();
        mPaint.reset();
        mPaint.setStyle(PaintBundle.STYLE_FILL);
        mPaint.setColor(mColorValue);
        mPaint.setTextSize(mMeasureFontSize);
        mPaint.setTextStyle(mType, (int) mFontWeightValue, mFontStyle == 1);
        if (mFontAxis != null && mFontAxis.length > 0) {
            float[] values = new float[mFontAxis.length];
            for (int i = 0; i < mFontAxis.length; i++) {
                values[i] = mFontAxisValues[i];
                if (Utils.isVariable(values[i])) {
                    values[i] = context.getContext().getFloat(Utils.idFromNan(values[i]));
                }
            }
            mPaint.setTextAxis(mFontAxis, values);
        }
        context.replacePaint(mPaint);
        if (mCachedString == null) {
            return;
        }
        int length = mCachedString.length();
        if (mComputedTextLayout != null) {
            if (mOverflow != OVERFLOW_VISIBLE) {
                context.save();
                context.clipRect(
                        0f,
                        0f,
                        mWidth - mPaddingLeft - mPaddingRight,
                        mHeight - mPaddingTop - mPaddingBottom);
                context.translate(getScrollX(), getScrollY());
                context.drawComplexText(mComputedTextLayout);
                context.restore();
            } else {
                context.drawComplexText(mComputedTextLayout);
            }
        } else {
            float px = mTextX;
            switch (mTextAlignValue) {
                case TEXT_ALIGN_CENTER:
                    px = (mWidth - mPaddingLeft - mPaddingRight - mTextW) / 2f;
                    break;
                case TEXT_ALIGN_RIGHT:
                case TEXT_ALIGN_END:
                    px = (mWidth - mPaddingLeft - mPaddingRight - mTextW);
                    break;
                case TEXT_ALIGN_LEFT:
                case TEXT_ALIGN_START:
                default:
            }

            if (mOverflow != OVERFLOW_VISIBLE || mTextW > (mWidth - mPaddingLeft - mPaddingRight)) {
                context.save();
                context.clipRect(
                        0f,
                        0f,
                        mWidth - mPaddingLeft - mPaddingRight,
                        mHeight - mPaddingTop - mPaddingBottom);
                context.translate(getScrollX(), getScrollY());
                context.drawTextRun(mTextId, 0, length, 0, 0, px, mTextY, false);
                context.restore();
            } else {
                context.drawTextRun(mTextId, 0, length, 0, 0, px, mTextY, false);
            }
        }
        if (DEBUG) {
            mPaint.setStyle(PaintBundle.STYLE_FILL_AND_STROKE);
            mPaint.setColor(1f, 1F, 1F, 1F);
            mPaint.setStrokeWidth(3f);
            context.applyPaint(mPaint);
            context.drawLine(0f, 0f, mWidth, mHeight);
            context.drawLine(0f, mHeight, mWidth, 0f);
            mPaint.setColor(1f, 0F, 0F, 1F);
            mPaint.setStrokeWidth(1f);
            context.applyPaint(mPaint);
            context.drawLine(0f, 0f, mWidth, mHeight);
            context.drawLine(0f, mHeight, mWidth, 0f);
        }
        context.restorePaint();
        //////////////////////////////////////////////////////////

        if (mGraphicsLayerModifier != null) {
            context.endGraphicsLayer();
        }

        context.translate(-tx, -ty);
        context.restore();
        context.getContext().mLastComponent = prev;
    }

    @NonNull
    @Override
    public String toString() {
        return "CORE_TEXT ["
                + mComponentId
                + ":"
                + mAnimationId
                + "] ("
                + mX
                + ", "
                + mY
                + " - "
                + mWidth
                + " x "
                + mHeight
                + ") "
                + Visibility.toString(mVisibility);
    }

    @NonNull
    @Override
    protected String getSerializedName() {
        return "CORE_TEXT";
    }

    @Override
    public void serializeToString(int indent, @NonNull StringSerializer serializer) {
        serializer.append(
                indent,
                getSerializedName()
                        + " ["
                        + mComponentId
                        + ":"
                        + mAnimationId
                        + "] = "
                        + "["
                        + mX
                        + ", "
                        + mY
                        + ", "
                        + mWidth
                        + ", "
                        + mHeight
                        + "] "
                        + Visibility.toString(mVisibility)
                        + " ("
                        + mTextId
                        + ":\""
                        + mCachedString
                        + "\")");
    }

    @Override
    public void computeSize(
            @NonNull PaintContext context,
            float minWidth,
            float maxWidth,
            float minHeight,
            float maxHeight,
            @NonNull MeasurePass measure) {
        super.computeSize(context, minWidth, maxWidth, minHeight, maxHeight, measure);
        computeWrapSize(context, minWidth, maxWidth, minHeight, maxHeight,
                true, true, measure, mCachedSize);
        ComponentMeasure m = measure.get(this);
        m.setW(mCachedSize.getWidth());
        m.setH(mCachedSize.getHeight());
    }

    private float mMeasureFontSize = TextStyle.DEFAULT_FONT_SIZE;
    private float mMeasureFontWeight = TextStyle.DEFAULT_FONT_WEIGHT;

    @Override
    public void computeWrapSize(
            @NonNull PaintContext context,
            float minWidth, float maxWidth,
            float minHeight, float maxHeight,
            boolean horizontalWrap,
            boolean verticalWrap,
            @NonNull MeasurePass measure,
            @NonNull Size size) {
        mMeasureFontSize = mFontSizeValue;
        mMeasureFontWeight = mFontWeightValue;
        context.savePaint();
        mPaint.reset();
        mPaint.setTextSize(mMeasureFontSize);
        mPaint.setTextStyle(mType == -1 ? 0 : mType, (int) mMeasureFontWeight, mFontStyle == 1);
        if (mFontAxis != null && mFontAxis.length > 0) {
            float[] values = new float[mFontAxis.length];
            for (int i = 0; i < mFontAxis.length; i++) {
                values[i] = mFontAxisValues[i];
                if (Utils.isVariable(values[i])) {
                    values[i] = context.getContext().getFloat(Utils.idFromNan(values[i]));
                }
            }
            mPaint.setTextAxis(mFontAxis, mFontAxisValues);
        }
        mPaint.setColor(mColorValue);
        context.replacePaint(mPaint);
        float[] bounds = new float[4];
        if (mNewString != null && !mNewString.equals(mCachedString)) {
            mCachedString = mNewString;
        }
        if (mCachedString == null) {
            return;
        }

        if (mAutosize) {
            float stepSize = 0.5f;
            float minFontSize = mMinFontSize <= 0 ? 4f : mMinFontSize;
            float maxFontSize = mMaxFontSize <= 0 ? 400f : mMaxFontSize;
            float min = minFontSize;
            float max = maxFontSize;
            float current = (min + max) / 2f;
            while (max - min >= stepSize) {
                mPaint.setTextSize(current);
                context.replacePaint(mPaint);
                textLayout(context, maxWidth, maxHeight, bounds, true, true);
                boolean hasHyphenation =
                        mComputedTextLayout != null && mComputedTextLayout.isHyphenatedText();
                boolean invalid = mHyphenationFrequency == 0 && hasHyphenation;
                float h = bounds[3] - bounds[1];
                if (invalid || h >= maxHeight) {
                    max = current;
                } else {
                    min = current;
                }
                current = (min + max) / 2f;
            }
            current = (float) (floor((min - minFontSize) / stepSize) * stepSize + minFontSize);
            if ((current + stepSize) < maxFontSize) {
                mPaint.setTextSize(current + stepSize);
                context.replacePaint(mPaint);
                textLayout(context, maxWidth, maxHeight, bounds, true, true);
                boolean hasHyphenation =
                        mComputedTextLayout != null && mComputedTextLayout.isHyphenatedText();
                boolean invalid = mHyphenationFrequency == 0 && hasHyphenation;
                float h = bounds[3] - bounds[1];
                if (!invalid && h < maxHeight) {
                    current += stepSize;
                }
            }
            mFontSizeValue = current;
            mMeasureFontSize = current;
            mPaint.setTextSize(mFontSizeValue);
            context.replacePaint(mPaint);
            textLayout(context, maxWidth, maxHeight, bounds, true, false);
        } else {
            textLayout(context, maxWidth, maxHeight, bounds);
        }

        context.restorePaint();
        float w = bounds[2] - bounds[0];
        float h = bounds[3] - bounds[1];
        size.setWidth(Math.min(maxWidth, w));
        mTextX = -bounds[0];
        size.setHeight(Math.min(maxHeight, h));
        mTextY = -bounds[1];
        mTextW = w;
        mTextH = h;
    }

    private void textLayout(@NonNull PaintContext context, float maxWidth,
            float maxHeight, float @NonNull [] bounds) {
        textLayout(context, maxWidth, maxHeight, bounds, false, false);
    }

    private void textLayout(@NonNull PaintContext context, float maxWidth, float maxHeight,
            float @NonNull [] bounds, boolean forceComplex, boolean inAutosize) {
        if (maxWidth < 0 || maxHeight < 0) {
            return;
        }
        int flags = PaintContext.TEXT_MEASURE_FONT_HEIGHT | PaintContext.TEXT_MEASURE_SPACES;
        if (forceComplex) {
            flags |= PaintContext.TEXT_COMPLEX;
        }
        if (mOverflow == OVERFLOW_START_ELLIPSIS
                || mOverflow == OVERFLOW_MIDDLE_ELLIPSIS
                || mOverflow == OVERFLOW_ELLIPSIS) {
            flags |= PaintContext.TEXT_COMPLEX;
            forceComplex = true;
        }
        if (mLetterSpacing != 0f || mLineHeightMultiplier != 1f || mLineHeightAdd > 0f
                || mUnderline || mStrikethrough
                || mJustificationMode > 0 || mLineBreakStrategy > 0
                || mHyphenationFrequency > 0) {
            flags |= PaintContext.TEXT_COMPLEX;
            forceComplex = true;
        }
        if ((flags & PaintContext.TEXT_COMPLEX) != PaintContext.TEXT_COMPLEX) {
            for (int i = 0; i < mCachedString.length(); i++) {
                char c = mCachedString.charAt(i);
                if ((c == '\n') || (c == '\t')) {
                    flags |= PaintContext.TEXT_COMPLEX;
                    forceComplex = true;
                    break;
                }
            }
        }
        if (!forceComplex) {
            context.getTextBounds(mTextId, 0, mCachedString.length(), flags, bounds);
            mBaseline = -bounds[1];
        }
        if (forceComplex || (bounds[2] - bounds[1] > maxWidth && mMaxLines > 1 && maxWidth > 0f)) {
            if (inAutosize) {
                flags |= PaintContext.TEXT_MEASURE_AUTOSIZE;
            }
            boolean done = false;
            int maxLines = mMaxLines;
            while (!done) {
                mComputedTextLayout =
                        context.layoutComplexText(
                                mTextId,
                                0,
                                mCachedString.length(),
                                mTextAlign,
                                mOverflow,
                                maxLines,
                                maxWidth,
                                maxHeight,
                                mLetterSpacing,
                                mLineHeightAdd,
                                mLineHeightMultiplier,
                                mLineBreakStrategy,
                                mHyphenationFrequency,
                                mJustificationMode,
                                mUnderline,
                                mStrikethrough,
                                flags);
                if (mComputedTextLayout != null) {
                    bounds[0] = 0f;
                    bounds[1] = 0f;
                    bounds[2] = mComputedTextLayout.getWidth();
                    bounds[3] = mComputedTextLayout.getHeight();
                }
                if (mComputedTextLayout != null
                        && !inAutosize
                        && mComputedTextLayout.getHeight() > maxHeight
                        && mOverflow == CoreText.OVERFLOW_ELLIPSIS) {
                    // If the text is bigger than the available space *and* we have
                    // OVERFLOW_ELLIPSIS, let's recompute the maxLines in order
                    // to show the ellipsis.
                    // Note: on Android, maxLines doesn't seem to apply when using
                    // OVERFLOW_START_ELLIPSIS or OVERFLOW_MIDDLE_ELLIPSIS -- those seem
                    // to only work with maxLines = 1.
                    if (mComputedTextLayout.getVisibleLineCount() != maxLines) {
                        maxLines = mComputedTextLayout.getVisibleLineCount();
                    } else {
                        maxLines--;
                    }
                    if (maxLines < 1) {
                        done = true;
                    }
                } else {
                    done = true;
                }
            }
        } else {
            mComputedTextLayout = null;
        }
    }

    @Override
    public float minIntrinsicHeight(@NonNull RemoteContext context) {
        return mTextH;
    }

    @Override
    public float minIntrinsicWidth(@NonNull RemoteContext context) {
        return mTextW;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "CoreText";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.CORE_TEXT;
    }

    /**
     * Write the operation in the buffer
     *
     * @param buffer       the WireBuffer we write on
     * @param componentId  the component id
     * @param animationId  the animation id (-1 if not set)
     * @param textId       the text id
     * @param color        the text color
     * @param fontSize     the font size
     * @param fontStyle    the font style
     * @param fontWeight   the font weight
     * @param fontFamilyId the font family id
     * @param textAlign    the alignment rules
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int componentId,
            int animationId,
            int textId,
            int color,
            int colorId,
            float fontSize,
            float minFontSize,
            float maxFontSize,
            int fontStyle,
            float fontWeight,
            int fontFamilyId,
            int textAlign,
            int overflow,
            int maxLines,
            float letterSpacing,
            float lineHeightAdd,
            float lineHeightMultiplier,
            int lineBreakStrategy,
            int hyphenationFrequency,
            int justificationMode,
            boolean underline,
            boolean strikethrough,
            int @Nullable [] fontAxis,
            float @Nullable [] fontAxisValues,
            boolean autosize,
            int flags,
            int textStyleId) {
        buffer.start(id());
        buffer.writeInt(textId);
        int count = 0;
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_ID, componentId);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_ANIMATION_ID, animationId);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_COLOR, color);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_COLOR_ID, colorId);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_FONT_SIZE, fontSize);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_MIN_FONT_SIZE, minFontSize);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_MAX_FONT_SIZE, maxFontSize);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_FONT_STYLE, fontStyle);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_FONT_WEIGHT, fontWeight);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_FONT_FAMILY, fontFamilyId);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_TEXT_ALIGN, textAlign);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_OVERFLOW, overflow);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_MAX_LINES, maxLines);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_LETTER_SPACING, letterSpacing);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_LINE_HEIGHT_ADD, lineHeightAdd);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_LINE_HEIGHT_MULTIPLIER,
                lineHeightMultiplier);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_BREAK_STRATEGY,
                lineBreakStrategy);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_HYPHENATION_FREQUENCY,
                hyphenationFrequency);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_JUSTIFICATION_MODE,
                justificationMode);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_UNDERLINE, underline);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_STRIKETHROUGH, strikethrough);
        boolean hasFontAxis = fontAxis != null && fontAxis.length > 0;

        if (fontAxis != null && fontAxis.length != fontAxisValues.length) {
            throw new IllegalStateException("fontAxis and fontAxisValues must be the same length");
        }

        if (hasFontAxis) {
            count += 2;
        }
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_AUTOSIZE, autosize);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_FLAGS, flags);
        count += TextStyle.PARAMETERS.countIfNotDefault(TextStyle.P_PARENT_ID, textStyleId);
        buffer.writeShort(count);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_ID, componentId);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_ANIMATION_ID, animationId);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_COLOR, color);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_COLOR_ID, colorId);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_SIZE, fontSize);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_MIN_FONT_SIZE, minFontSize);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_MAX_FONT_SIZE, maxFontSize);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_STYLE, fontStyle);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_WEIGHT, fontWeight);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_FAMILY, fontFamilyId);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_TEXT_ALIGN, textAlign);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_OVERFLOW, overflow);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_MAX_LINES, maxLines);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_LETTER_SPACING, letterSpacing);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_LINE_HEIGHT_ADD, lineHeightAdd);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_LINE_HEIGHT_MULTIPLIER,
                lineHeightMultiplier);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_BREAK_STRATEGY, lineBreakStrategy);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_HYPHENATION_FREQUENCY,
                hyphenationFrequency);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_JUSTIFICATION_MODE, justificationMode);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_UNDERLINE, underline);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_STRIKETHROUGH, strikethrough);
        if (hasFontAxis) {
            TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_AXIS, fontAxis);
            TextStyle.PARAMETERS.write(buffer, TextStyle.P_FONT_AXIS_VALUES, fontAxisValues);
        }
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_AUTOSIZE, autosize);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_FLAGS, flags);
        TextStyle.PARAMETERS.write(buffer, TextStyle.P_PARENT_ID, textStyleId);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int textId = buffer.readInt();
        int paramsLength = buffer.readShort();
        final int[] intAttributes =
                {-1, -1, 0xFF000000, -1, 0, -1, 1, 1, Integer.MAX_VALUE, 0, 0, 0, 0, 0, 0, 0, -1};
        final float[] floatAttributes =
                {TextStyle.DEFAULT_FONT_SIZE, -1f, -1f, TextStyle.DEFAULT_FONT_WEIGHT, 0f, 0f, 1f};
        final ArrayList<Integer> fontAxisList = new ArrayList<>();
        final ArrayList<Float> fontAxisValuesList = new ArrayList<>();
        for (int i = 0; i < paramsLength; i++) {
            TextStyle.PARAMETERS.read(buffer, new CommandParameters.Callback() {
                @Override
                public void value(int id, int value) {
                    switch (id) {
                        case TextStyle.P_ID:
                            intAttributes[0] = value;
                            break;
                        case TextStyle.P_ANIMATION_ID:
                            intAttributes[1] = value;
                            break;
                        case TextStyle.P_COLOR:
                            intAttributes[2] = value;
                            break;
                        case TextStyle.P_COLOR_ID:
                            intAttributes[3] = value;
                            break;
                        case TextStyle.P_FONT_STYLE:
                            intAttributes[4] = value;
                            break;
                        case TextStyle.P_FONT_FAMILY:
                            intAttributes[5] = value;
                            break;
                        case TextStyle.P_TEXT_ALIGN:
                            intAttributes[6] = value;
                            break;
                        case TextStyle.P_OVERFLOW:
                            intAttributes[7] = value;
                            break;
                        case TextStyle.P_MAX_LINES:
                            intAttributes[8] = value;
                            break;
                        case TextStyle.P_BREAK_STRATEGY:
                            intAttributes[9] = value;
                            break;
                        case TextStyle.P_HYPHENATION_FREQUENCY:
                            intAttributes[10] = value;
                            break;
                        case TextStyle.P_JUSTIFICATION_MODE:
                            intAttributes[11] = value;
                            break;
                        case TextStyle.P_FLAGS:
                            intAttributes[15] = value;
                            break;
                        case TextStyle.P_PARENT_ID:
                            intAttributes[16] = value;
                            break;
                    }
                }

                @Override
                public void value(int id, float value) {
                    switch (id) {
                        case TextStyle.P_FONT_SIZE:
                            floatAttributes[0] = value;
                            break;
                        case TextStyle.P_MIN_FONT_SIZE:
                            floatAttributes[1] = value;
                            break;
                        case TextStyle.P_MAX_FONT_SIZE:
                            floatAttributes[2] = value;
                            break;
                        case TextStyle.P_FONT_WEIGHT:
                            floatAttributes[3] = value;
                            break;
                        case TextStyle.P_LETTER_SPACING:
                            floatAttributes[4] = value;
                            break;
                        case TextStyle.P_LINE_HEIGHT_ADD:
                            floatAttributes[5] = value;
                            break;
                        case TextStyle.P_LINE_HEIGHT_MULTIPLIER:
                            floatAttributes[6] = value;
                            break;
                    }
                }

                @Override
                public void value(int id, short value) {

                }

                @Override
                public void value(int id, byte value) {

                }

                @Override
                public void value(int id, boolean value) {
                    switch (id) {
                        case TextStyle.P_UNDERLINE:
                            intAttributes[12] = value ? 1 : 0;
                            break;
                        case TextStyle.P_STRIKETHROUGH:
                            intAttributes[13] = value ? 1 : 0;
                            break;
                        case TextStyle.P_AUTOSIZE:
                            intAttributes[14] = value ? 1 : 0;
                            break;
                    }
                }

                @Override
                public void value(int id, @NonNull String value) {

                }

                @Override
                public void value(int id, int @NonNull [] value) {
                    switch (id) {
                        case TextStyle.P_FONT_AXIS:
                            for (int axis : value) {
                                fontAxisList.add(axis);
                            }
                            break;
                    }
                }

                @Override
                public void value(int id, float @NonNull [] value) {
                    switch (id) {
                        case TextStyle.P_FONT_AXIS_VALUES:
                            for (float v : value) {
                                fontAxisValuesList.add(v);
                            }
                            break;
                    }
                }
            });
        }
        int[] fontAxis = null;
        float[] fontAxisValues = null;
        if (!fontAxisList.isEmpty() && fontAxisValuesList.size() == fontAxisList.size()) {
            fontAxis = new int[fontAxisList.size()];
            fontAxisValues = new float[fontAxisList.size()];
            for (int i = 0; i < fontAxisList.size(); i++) {
                fontAxis[i] = fontAxisList.get(i);
                fontAxisValues[i] = fontAxisValuesList.get(i);
            }
        }
        operations.add(
                new CoreText(
                        null,
                        intAttributes[0],
                        intAttributes[1],
                        0f, 0f, 0f, 0f,
                        textId,
                        intAttributes[2],
                        intAttributes[3],
                        floatAttributes[0],
                        floatAttributes[1],
                        floatAttributes[2],
                        intAttributes[4],
                        floatAttributes[3],
                        intAttributes[5],
                        intAttributes[6],
                        intAttributes[7],
                        intAttributes[8],
                        floatAttributes[4],
                        floatAttributes[5],
                        floatAttributes[6],
                        intAttributes[9],
                        intAttributes[10],
                        intAttributes[11],
                        intAttributes[12] == 1,
                        intAttributes[13] == 1,
                        fontAxis,
                        fontAxisValues,
                        intAttributes[14] == 1,
                        intAttributes[15],
                        intAttributes[16]));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Layout Managers", id(), name())
                .addedVersion(7)
                .experimental(true)
                .additionalDocumentation("core_text")
                .description("Core text layout implementation with advanced styling")
                .field(INT, "textId", "The ID of the text to display")
                .field(INT, "componentId", "Unique ID for this component")
                .field(INT, "animationId", "ID for animation purposes")
                .field(INT, "color", "The text color (ARGB)")
                .field(INT, "colorId", "The ID of the color variable")
                .field(FLOAT, "fontSize", "The font size")
                .field(FLOAT, "minFontSize", "Minimum font size for autosize")
                .field(FLOAT, "maxFontSize", "Maximum font size for autosize")
                .field(INT, "fontStyle", "The font style")
                .field(FLOAT, "fontWeight", "The font weight")
                .field(INT, "fontFamily", "The ID of the font family")
                .field(INT, "textAlign", "Text alignment")
                .field(INT, "overflow", "Overflow behavior")
                .field(INT, "maxLines", "Maximum number of lines")
                .field(FLOAT, "letterSpacing", "Letter spacing")
                .field(FLOAT, "lineHeightAdd", "Line height addition")
                .field(FLOAT, "lineHeightMultiplier", "Line height multiplier")
                .field(INT, "lineBreakStrategy", "Line break strategy")
                .field(INT, "hyphenationFrequency", "Hyphenation frequency")
                .field(INT, "justificationMode", "Justification mode")
                .field(BOOLEAN, "underline", "Whether to underline")
                .field(BOOLEAN, "strikethrough", "Whether to strikethrough")
                .field(INT_ARRAY, "fontAxis", "Font axis tags")
                .field(DocumentedOperation.FLOAT_ARRAY, "fontAxisValues", "Font axis values")
                .field(BOOLEAN, "autosize", "Whether to enable autosize")
                .field(INT, "flags", "Behavior flags")
                .field(INT, "textStyleId", "The ID of the text style to apply");
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(
                buffer,
                mComponentId,
                mAnimationId,
                mTextId,
                mColor,
                mColorId,
                mFontSize,
                mMinFontSize,
                mMaxFontSize,
                mFontStyle,
                mFontWeight,
                mFontFamilyId,
                mTextAlign,
                mOverflow,
                mMaxLines,
                mLetterSpacing,
                mLineHeightAdd,
                mLineHeightMultiplier,
                mLineBreakStrategy,
                mHyphenationFrequency,
                mJustificationMode,
                mUnderline,
                mStrikethrough,
                mFontAxis,
                mFontAxisValues,
                mAutosize,
                mFlags,
                mTextStyleId);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        super.serialize(serializer);
        serializer.add("textId", mTextId);
        short flags = getFlagsFromTextAlign(mTextAlign);
        serializer.add("flags", flags);
        if (mIsDynamicColorEnabled) {
            serializer.add("color", (float) mColor, (float) mColorValue);
        } else {
            serializer.add("color", Utils.colorInt(mColorValue));
        }
        serializer.add("fontSize", mFontSize, mFontSizeValue);
        serializer.add("fontStyle", mFontStyle);
        serializer.add("fontWeight", mFontWeight);
        serializer.add("fontFamilyId", mFontFamilyId);
        serializer.add("textAlign", mTextAlign);
    }
}
