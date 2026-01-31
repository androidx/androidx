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
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.PA_FLOAT;
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.PA_INT;
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.param;

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

    private static final byte P_COMPONENT_ID = 1;
    private static final byte P_ANIMATION_ID = 2;

    private static final byte P_COLOR = 3;
    private static final byte P_COLOR_ID = 4;
    private static final byte P_FONT_SIZE = 5;
    private static final byte P_FONT_STYLE = 6;
    private static final byte P_FONT_WEIGHT = 7;
    private static final byte P_FONT_FAMILY = 8;
    private static final byte P_TEXT_ALIGN = 9;
    private static final byte P_OVERFLOW = 10;
    private static final byte P_MAX_LINES = 11;
    private static final byte P_LETTER_SPACING = 12;
    private static final byte P_LINE_HEIGHT_ADD = 13;
    private static final byte P_LINE_HEIGHT_MULTIPLIER = 14;
    private static final byte P_BREAK_STRATEGY = 15;
    private static final byte P_HYPHENATION_FREQUENCY = 16;
    private static final byte P_JUSTIFICATION_MODE = 17;
    private static final byte P_UNDERLINE = 18;
    private static final byte P_STRIKETHROUGH = 19;
    private static final byte P_FONT_AXIS = 20;
    private static final byte P_FONT_AXIS_VALUES = 21;
    private static final byte P_AUTOSIZE = 22;
    private static final byte P_FLAGS = 23;
    private static final byte P_TEXT_STYLE_ID = 24;

    private static final byte P_MIN_FONT_SIZE = 25;
    private static final byte P_MAX_FONT_SIZE = 26;
    private static final CommandParameters PARAMETERS =
            new CommandParameters(
                    param("componentId", P_COMPONENT_ID, -1),
                    param("animationId", P_ANIMATION_ID, -1),
                    param("color", P_COLOR, 0xFF000000),
                    param("color", P_COLOR_ID, -1),
                    param("fontSize", P_FONT_SIZE, 16f),
                    param("fontStyle", P_FONT_STYLE, 0),
                    param("fontWeight", P_FONT_WEIGHT, 400f),
                    param("fontFamily", P_FONT_FAMILY, -1),
                    param("textAlign", P_TEXT_ALIGN, 1),
                    param("overflow", P_OVERFLOW, 1),
                    param("maxLines", P_MAX_LINES, Integer.MAX_VALUE),
                    param("letterSpacing", P_LETTER_SPACING, 0f),
                    param("lineHeightAdd", P_LINE_HEIGHT_ADD, 0f),
                    param("lineHeightMultiplier", P_LINE_HEIGHT_MULTIPLIER, 1f),
                    param("lineBreakStrategy", P_BREAK_STRATEGY, 0),
                    param("hyphenationFrequency", P_HYPHENATION_FREQUENCY, 0),
                    param("justificationMode", P_JUSTIFICATION_MODE, 0),
                    param("underline", P_UNDERLINE, false),
                    param("strikethrough", P_STRIKETHROUGH, false),
                    param("autosize", P_AUTOSIZE, false),
                    param("fontAxis", P_FONT_AXIS, PA_INT),
                    param("fontAxisValues", P_FONT_AXIS_VALUES, PA_FLOAT),
                    param("flags", P_FLAGS, 0),
                    param("textStyleId", P_TEXT_STYLE_ID, -1),
                    param("minFontSize", P_MIN_FONT_SIZE, -1f),
                    param("maxFontSize", P_MAX_FONT_SIZE, -1f)
            );

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
    private float mFontSize = 16f;
    private float mMinFontSize = -1f;
    private float mMaxFontSize = -1f;
    private float mFontSizeValue = 16f;
    private int mFontStyle = 0;
    private float mFontWeight = 400f;
    private float mFontWeightValue = 400f;
    private int mFontFamilyId = -1;
    private int mTextAlign = 1;
    private int mTextAlignValue = -1;
    private int mOverflow = 1;
    private int mMaxLines = Integer.MAX_VALUE;

    private int mLineBreakStrategy = 0;
    private int mHyphenationFrequency = 0;
    private int mJustificationMode = 0;

    private int mType = -1;
    private float mTextX;
    private float mTextY;
    private float mTextW = -1;
    private float mTextH = -1;

    private float mBaseline = 0f;

    private final Size mCachedSize = new Size(0f, 0f);


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
            mColorValue = context.getColor(mColorId);
        } else {
            mColorValue = mColor;
        }

        String cachedString = context.getText(mTextId);
        if (cachedString != null && cachedString.equalsIgnoreCase(mCachedString) && mType != -1) {
            if (mMeasureFontSize != mFontSizeValue
                    || mFontWeightValue != mFontWeight) {
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
            int flags) {
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
            int flags) {
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
                flags);
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

    private float mMeasureFontSize = 16f;

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
        context.savePaint();
        mPaint.reset();
        mPaint.setTextSize(mMeasureFontSize);
        mPaint.setTextStyle(mType == -1 ? 0 : mType, (int) mFontWeightValue, mFontStyle == 1);
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
    public float minIntrinsicHeight(@Nullable RemoteContext context) {
        return mTextH;
    }

    @Override
    public float minIntrinsicWidth(@Nullable RemoteContext context) {
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
            int flags) {
        buffer.start(id());
        buffer.writeInt(textId);
        int count = 0;
        count += PARAMETERS.countIfNotDefault(P_COMPONENT_ID, componentId);
        count += PARAMETERS.countIfNotDefault(P_ANIMATION_ID, animationId);
        count += PARAMETERS.countIfNotDefault(P_COLOR, color);
        count += PARAMETERS.countIfNotDefault(P_COLOR_ID, colorId);
        count += PARAMETERS.countIfNotDefault(P_FONT_SIZE, fontSize);
        count += PARAMETERS.countIfNotDefault(P_MIN_FONT_SIZE, minFontSize);
        count += PARAMETERS.countIfNotDefault(P_MAX_FONT_SIZE, maxFontSize);
        count += PARAMETERS.countIfNotDefault(P_FONT_STYLE, fontStyle);
        count += PARAMETERS.countIfNotDefault(P_FONT_WEIGHT, fontWeight);
        count += PARAMETERS.countIfNotDefault(P_FONT_FAMILY, fontFamilyId);
        count += PARAMETERS.countIfNotDefault(P_TEXT_ALIGN, textAlign);
        count += PARAMETERS.countIfNotDefault(P_OVERFLOW, overflow);
        count += PARAMETERS.countIfNotDefault(P_MAX_LINES, maxLines);
        count += PARAMETERS.countIfNotDefault(P_LETTER_SPACING, letterSpacing);
        count += PARAMETERS.countIfNotDefault(P_LINE_HEIGHT_ADD, lineHeightAdd);
        count += PARAMETERS.countIfNotDefault(P_LINE_HEIGHT_MULTIPLIER, lineHeightMultiplier);
        count += PARAMETERS.countIfNotDefault(P_BREAK_STRATEGY, lineBreakStrategy);
        count += PARAMETERS.countIfNotDefault(P_HYPHENATION_FREQUENCY, hyphenationFrequency);
        count += PARAMETERS.countIfNotDefault(P_JUSTIFICATION_MODE, justificationMode);
        count += PARAMETERS.countIfNotDefault(P_UNDERLINE, underline);
        count += PARAMETERS.countIfNotDefault(P_STRIKETHROUGH, strikethrough);
        count += PARAMETERS.countIfNotDefault(P_AUTOSIZE, autosize);
        boolean hasFontAxis = fontAxis != null && fontAxis.length > 0;

        if (fontAxis != null && fontAxis.length != fontAxisValues.length) {
            throw new IllegalStateException("fontAxis and fontAxisValues must be the same length");
        }

        if (hasFontAxis) {
            count += 2;
        }
        count += PARAMETERS.countIfNotDefault(P_FLAGS, flags);
        // TODO: text style id
        buffer.writeShort(count);
        PARAMETERS.write(buffer, P_COMPONENT_ID, componentId);
        PARAMETERS.write(buffer, P_ANIMATION_ID, animationId);
        PARAMETERS.write(buffer, P_COLOR, color);
        PARAMETERS.write(buffer, P_COLOR_ID, colorId);
        PARAMETERS.write(buffer, P_FONT_SIZE, fontSize);
        PARAMETERS.write(buffer, P_MIN_FONT_SIZE, minFontSize);
        PARAMETERS.write(buffer, P_MAX_FONT_SIZE, maxFontSize);
        PARAMETERS.write(buffer, P_FONT_STYLE, fontStyle);
        PARAMETERS.write(buffer, P_FONT_WEIGHT, fontWeight);
        PARAMETERS.write(buffer, P_FONT_FAMILY, fontFamilyId);
        PARAMETERS.write(buffer, P_TEXT_ALIGN, textAlign);
        PARAMETERS.write(buffer, P_OVERFLOW, overflow);
        PARAMETERS.write(buffer, P_MAX_LINES, maxLines);
        PARAMETERS.write(buffer, P_LETTER_SPACING, letterSpacing);
        PARAMETERS.write(buffer, P_LINE_HEIGHT_ADD, lineHeightAdd);
        PARAMETERS.write(buffer, P_LINE_HEIGHT_MULTIPLIER, lineHeightMultiplier);
        PARAMETERS.write(buffer, P_BREAK_STRATEGY, lineBreakStrategy);
        PARAMETERS.write(buffer, P_HYPHENATION_FREQUENCY, hyphenationFrequency);
        PARAMETERS.write(buffer, P_JUSTIFICATION_MODE, justificationMode);
        PARAMETERS.write(buffer, P_UNDERLINE, underline);
        PARAMETERS.write(buffer, P_STRIKETHROUGH, strikethrough);
        if (hasFontAxis) {
            PARAMETERS.write(buffer, P_FONT_AXIS, fontAxis);
            PARAMETERS.write(buffer, P_FONT_AXIS_VALUES, fontAxisValues);
        }
        PARAMETERS.write(buffer, P_AUTOSIZE, autosize);
        PARAMETERS.write(buffer, P_FLAGS, flags);
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
                {-1, -1, 0xFF000000, -1, 0, -1, 1, 1, Integer.MAX_VALUE, 0, 0, 0, 0, 0, 0, 0};
        final float[] floatAttributes = {16f, -1f, -1f, 400f, 0f, 0f, 1f};
        final ArrayList<Integer> fontAxisList = new ArrayList<>();
        final ArrayList<Float> fontAxisValuesList = new ArrayList<>();
        for (int i = 0; i < paramsLength; i++) {
            PARAMETERS.read(buffer, new CommandParameters.Callback() {
                @Override
                public void value(int id, int value) {
                    switch (id) {
                        case P_COMPONENT_ID:
                            intAttributes[0] = value;
                            break;
                        case P_ANIMATION_ID:
                            intAttributes[1] = value;
                            break;
                        case P_COLOR:
                            intAttributes[2] = value;
                            break;
                        case P_COLOR_ID:
                            intAttributes[3] = value;
                            break;
                        case P_FONT_STYLE:
                            intAttributes[4] = value;
                            break;
                        case P_FONT_FAMILY:
                            intAttributes[5] = value;
                            break;
                        case P_TEXT_ALIGN:
                            intAttributes[6] = value;
                            break;
                        case P_OVERFLOW:
                            intAttributes[7] = value;
                            break;
                        case P_MAX_LINES:
                            intAttributes[8] = value;
                            break;
                        case P_BREAK_STRATEGY:
                            intAttributes[9] = value;
                            break;
                        case P_HYPHENATION_FREQUENCY:
                            intAttributes[10] = value;
                            break;
                        case P_JUSTIFICATION_MODE:
                            intAttributes[11] = value;
                            break;
                        case P_FLAGS:
                            intAttributes[15] = value;
                            break;
                    }
                }

                @Override
                public void value(int id, float value) {
                    switch (id) {
                        case P_FONT_SIZE:
                            floatAttributes[0] = value;
                            break;
                        case P_MIN_FONT_SIZE:
                            floatAttributes[1] = value;
                            break;
                        case P_MAX_FONT_SIZE:
                            floatAttributes[2] = value;
                            break;
                        case P_FONT_WEIGHT:
                            floatAttributes[3] = value;
                            break;
                        case P_LETTER_SPACING:
                            floatAttributes[4] = value;
                            break;
                        case P_LINE_HEIGHT_ADD:
                            floatAttributes[5] = value;
                            break;
                        case P_LINE_HEIGHT_MULTIPLIER:
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
                        case P_UNDERLINE:
                            intAttributes[12] = value ? 1 : 0;
                            break;
                        case P_STRIKETHROUGH:
                            intAttributes[13] = value ? 1 : 0;
                            break;
                        case P_AUTOSIZE:
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
                        case P_FONT_AXIS:
                            for (int axis : value) {
                                fontAxisList.add(axis);
                            }
                            break;
                    }
                }

                @Override
                public void value(int id, float @NonNull [] value) {
                    switch (id) {
                        case P_FONT_AXIS_VALUES:
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
                        intAttributes[15]));
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
                .field(INT, "flags", "Behavior flags");
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
                null,
                null,
                mAutosize,
                mFlags);
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
