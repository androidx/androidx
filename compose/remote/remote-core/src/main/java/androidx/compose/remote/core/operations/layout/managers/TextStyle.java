/*
 * Copyright 2026 The Android Open Source Project
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

import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.PA_FLOAT;
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.PA_INT;
import static androidx.compose.remote.core.operations.utilities.touch.CommandParameters.param;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.ComponentData;
import androidx.compose.remote.core.operations.utilities.touch.CommandParameters;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Text style implementation
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TextStyle extends Operation implements ComponentData {
    public static final float DEFAULT_FONT_SIZE = 36f;
    public static final float DEFAULT_FONT_WEIGHT = 400f;

    public static final byte P_ID = 1;
    public static final byte P_ANIMATION_ID = 2;
    public static final byte P_COLOR = 3;
    public static final byte P_COLOR_ID = 4;
    public static final byte P_FONT_SIZE = 5;
    public static final byte P_FONT_STYLE = 6;
    public static final byte P_FONT_WEIGHT = 7;
    public static final byte P_FONT_FAMILY = 8;
    public static final byte P_TEXT_ALIGN = 9;
    public static final byte P_OVERFLOW = 10;
    public static final byte P_MAX_LINES = 11;
    public static final byte P_LETTER_SPACING = 12;
    public static final byte P_LINE_HEIGHT_ADD = 13;
    public static final byte P_LINE_HEIGHT_MULTIPLIER = 14;
    public static final byte P_BREAK_STRATEGY = 15;
    public static final byte P_HYPHENATION_FREQUENCY = 16;
    public static final byte P_JUSTIFICATION_MODE = 17;
    public static final byte P_UNDERLINE = 18;
    public static final byte P_STRIKETHROUGH = 19;
    public static final byte P_FONT_AXIS = 20;
    public static final byte P_FONT_AXIS_VALUES = 21;
    public static final byte P_AUTOSIZE = 22;
    public static final byte P_FLAGS = 23;
    public static final byte P_PARENT_ID = 24;
    public static final byte P_MIN_FONT_SIZE = 25;
    public static final byte P_MAX_FONT_SIZE = 26;

    public static final CommandParameters PARAMETERS = new CommandParameters(
            param("id", P_ID, -1),
            param("animationId", P_ANIMATION_ID, -1),
            param("color", P_COLOR, 0xFF000000),
            param("colorId", P_COLOR_ID, -1),
            param("fontSize", P_FONT_SIZE, DEFAULT_FONT_SIZE),
            param("fontStyle", P_FONT_STYLE, 0),
            param("fontWeight", P_FONT_WEIGHT, DEFAULT_FONT_WEIGHT),
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
            param("parentId", P_PARENT_ID, -1),
            param("minFontSize", P_MIN_FONT_SIZE, -1f),
            param("maxFontSize", P_MAX_FONT_SIZE, -1f)
    );

    @Nullable Integer mId = null;
    @Nullable Integer mColor = null;
    @Nullable Integer mColorId = null;
    @Nullable Float mFontSize = null;
    @Nullable Float mMinFontSize = null;
    @Nullable Float mMaxFontSize = null;
    @Nullable Integer mFontStyle = null;
    @Nullable Float mFontWeight = null;
    @Nullable Integer mFontFamilyId = null;
    @Nullable Integer mTextAlign = null;
    @Nullable Integer mOverflow = null;
    @Nullable Integer mMaxLines = null;
    @Nullable Float mLetterSpacing = null;
    @Nullable Float mLineHeightAdd = null;
    @Nullable Float mLineHeightMultiplier = null;
    @Nullable Integer mLineBreakStrategy = null;
    @Nullable Integer mHyphenationFrequency = null;
    @Nullable Integer mJustificationMode = null;
    @Nullable Boolean mUnderline = null;
    @Nullable Boolean mStrikethrough = null;
    int @Nullable [] mFontAxis;
    float @Nullable [] mFontAxisValues;
    @Nullable Boolean mAutosize = null;
    @Nullable Integer mParentId = null;

    public TextStyle() {
    }

    @SuppressWarnings("AutoBoxing")
    public TextStyle(int id, @Nullable Integer color, @Nullable Integer colorId,
            @Nullable Float fontSize, @Nullable Float minFontSize, @Nullable Float maxFontSize,
            @Nullable Integer fontStyle, @Nullable Float fontWeight, @Nullable Integer fontFamilyId,
            @Nullable Integer textAlign, @Nullable Integer overflow, @Nullable Integer maxLines,
            @Nullable Float letterSpacing, @Nullable Float lineHeightAdd,
            @Nullable Float lineHeightMultiplier, @Nullable Integer lineBreakStrategy,
            @Nullable Integer hyphenationFrequency, @Nullable Integer justificationMode,
            @Nullable Boolean underline, @Nullable Boolean strikethrough, int @Nullable [] fontAxis,
            float @Nullable [] fontAxisValues, @Nullable Boolean autosize,
            @Nullable Integer parentId) {
        this.mId = id;
        this.mColor = color;
        this.mColorId = colorId;
        this.mFontSize = fontSize;
        this.mMinFontSize = minFontSize;
        this.mMaxFontSize = maxFontSize;
        this.mFontStyle = fontStyle;
        this.mFontWeight = fontWeight;
        this.mFontFamilyId = fontFamilyId;
        this.mTextAlign = textAlign;
        this.mOverflow = overflow;
        this.mMaxLines = maxLines;
        this.mLetterSpacing = letterSpacing;
        this.mLineHeightAdd = lineHeightAdd;
        this.mLineHeightMultiplier = lineHeightMultiplier;
        this.mLineBreakStrategy = lineBreakStrategy;
        this.mHyphenationFrequency = hyphenationFrequency;
        this.mJustificationMode = justificationMode;
        this.mUnderline = underline;
        this.mStrikethrough = strikethrough;
        this.mFontAxis = fontAxis;
        this.mFontAxisValues = fontAxisValues;
        this.mAutosize = autosize;
        this.mParentId = parentId;
    }

    public TextStyle(int id, int color, int colorId, float fontSize, float minFontSize,
            float maxFontSize, int fontStyle, float fontWeight, int fontFamilyId, int textAlign,
            int overflow, int maxLines, float letterSpacing, float lineHeightAdd,
            float lineHeightMultiplier, int lineBreakStrategy, int hyphenationFrequency,
            int justificationMode, boolean underline, boolean strikethrough,
            int @Nullable [] fontAxis, float @Nullable [] fontAxisValues, boolean autosize) {
        this(id, color, colorId, fontSize, minFontSize, maxFontSize, fontStyle, fontWeight,
                fontFamilyId, textAlign, overflow, maxLines, letterSpacing, lineHeightAdd,
                lineHeightMultiplier, lineBreakStrategy, hyphenationFrequency, justificationMode,
                underline, strikethrough, fontAxis, fontAxisValues, autosize, -1);
    }

    public TextStyle(int id, int color, int colorId, float fontSize, float minFontSize,
            float maxFontSize, int fontStyle, float fontWeight, int fontFamilyId, int textAlign,
            int overflow, int maxLines, float letterSpacing, float lineHeightAdd,
            float lineHeightMultiplier, int lineBreakStrategy, int hyphenationFrequency,
            int justificationMode, boolean underline, boolean strikethrough,
            int @Nullable [] fontAxis, float @Nullable [] fontAxisValues, boolean autosize,
            int parentId) {
        this.mId = id;
        this.mColor = color;
        this.mColorId = colorId;
        this.mFontSize = fontSize;
        this.mMinFontSize = minFontSize;
        this.mMaxFontSize = maxFontSize;
        this.mFontStyle = fontStyle;
        this.mFontWeight = fontWeight;
        this.mFontFamilyId = fontFamilyId;
        this.mTextAlign = textAlign;
        this.mOverflow = overflow;
        this.mMaxLines = maxLines;
        this.mLetterSpacing = letterSpacing;
        this.mLineHeightAdd = lineHeightAdd;
        this.mLineHeightMultiplier = lineHeightMultiplier;
        this.mLineBreakStrategy = lineBreakStrategy;
        this.mHyphenationFrequency = hyphenationFrequency;
        this.mJustificationMode = justificationMode;
        this.mUnderline = underline;
        this.mStrikethrough = strikethrough;
        this.mFontAxis = fontAxis;
        this.mFontAxisValues = fontAxisValues;
        this.mAutosize = autosize;
        this.mParentId = parentId;
    }

    /**
     * Apply the style from another style
     */
    public void applyStyle(@NonNull TextStyle style) {
        if (mColor == null) mColor = style.mColor;
        if (mColorId == null) mColorId = style.mColorId;
        if (mFontSize == null) mFontSize = style.mFontSize;
        if (mMinFontSize == null) mMinFontSize = style.mMinFontSize;
        if (mMaxFontSize == null) mMaxFontSize = style.mMaxFontSize;
        if (mFontStyle == null) mFontStyle = style.mFontStyle;
        if (mFontWeight == null) mFontWeight = style.mFontWeight;
        if (mFontFamilyId == null) mFontFamilyId = style.mFontFamilyId;
        if (mTextAlign == null) mTextAlign = style.mTextAlign;
        if (mOverflow == null) mOverflow = style.mOverflow;
        if (mMaxLines == null) mMaxLines = style.mMaxLines;
        if (mLetterSpacing == null) mLetterSpacing = style.mLetterSpacing;
        if (mLineHeightAdd == null) mLineHeightAdd = style.mLineHeightAdd;
        if (mLineHeightMultiplier == null) {
            mLineHeightMultiplier = style.mLineHeightMultiplier;
        }
        if (mLineBreakStrategy == null) {
            mLineBreakStrategy = style.mLineBreakStrategy;
        }
        if (mHyphenationFrequency == null) {
            mHyphenationFrequency = style.mHyphenationFrequency;
        }
        if (mJustificationMode == null) {
            mJustificationMode = style.mJustificationMode;
        }
        if (mUnderline == null) mUnderline = style.mUnderline;
        if (mStrikethrough == null) mStrikethrough = style.mStrikethrough;
        if (mFontAxis == null) {
            mFontAxis = style.mFontAxis;
            mFontAxisValues = style.mFontAxisValues;
        }
        if (mAutosize == null) mAutosize = style.mAutosize;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mColor, mColorId, mFontSize, mMinFontSize, mMaxFontSize, mFontStyle,
                mFontWeight, mFontFamilyId, mTextAlign, mOverflow, mMaxLines, mLetterSpacing,
                mLineHeightAdd, mLineHeightMultiplier, mLineBreakStrategy, mHyphenationFrequency,
                mJustificationMode, mUnderline, mStrikethrough, mFontAxis, mFontAxisValues,
                mAutosize, mParentId != -1 ? mParentId : null);
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        if (mParentId != null && mParentId != -1) {
            Object parent = context.getObject(mParentId);
            if (parent instanceof TextStyle) {
                applyStyle((TextStyle) parent);
            }
        }
        context.putObject(mId, this);
    }

    /**
     * Return the operation id
     */
    public static int id() {
        return Operations.TEXT_STYLE;
    }

    /**
     * Apply the style to the text.
     */
    public static void apply(@NonNull WireBuffer buffer, int id, @Nullable Integer color,
            @Nullable Integer colorId, @Nullable Float fontSize, @Nullable Float minFontSize,
            @Nullable Float maxFontSize, @Nullable Integer fontStyle, @Nullable Float fontWeight,
            @Nullable Integer fontFamilyId, @Nullable Integer textAlign, @Nullable Integer overflow,
            @Nullable Integer maxLines, @Nullable Float letterSpacing,
            @Nullable Float lineHeightAdd, @Nullable Float lineHeightMultiplier,
            @Nullable Integer lineBreakStrategy, @Nullable Integer hyphenationFrequency,
            @Nullable Integer justificationMode, @Nullable Boolean underline,
            @Nullable Boolean strikethrough, int @Nullable [] fontAxis,
            float @Nullable [] fontAxisValues, @Nullable Boolean autosize,
            @Nullable Integer parentId) {
        buffer.start(Operations.TEXT_STYLE);
        int count = 0;
        if (id != -1) {
            count++;
        }
        if (color != null) {
            count++;
        }
        if (colorId != null) {
            count++;
        }
        if (fontSize != null) {
            count++;
        }
        if (minFontSize != null) {
            count++;
        }
        if (maxFontSize != null) {
            count++;
        }
        if (fontStyle != null) {
            count++;
        }
        if (fontWeight != null) {
            count++;
        }
        if (fontFamilyId != null) {
            count++;
        }
        if (textAlign != null) {
            count++;
        }
        if (overflow != null) {
            count++;
        }
        if (maxLines != null) {
            count++;
        }
        if (letterSpacing != null) {
            count++;
        }
        if (lineHeightAdd != null) {
            count++;
        }
        if (lineHeightMultiplier != null) {
            count++;
        }
        if (lineBreakStrategy != null) {
            count++;
        }
        if (hyphenationFrequency != null) {
            count++;
        }
        if (justificationMode != null) {
            count++;
        }
        if (underline != null) {
            count++;
        }
        if (strikethrough != null) {
            count++;
        }
        if (autosize != null) {
            count++;
        }
        if (parentId != null) {
            count++;
        }

        boolean hasFontAxis = fontAxis != null && fontAxis.length > 0;
        if (hasFontAxis) {
            count += 2;
        }
        buffer.writeShort(count);
        if (id != -1) {
            buffer.writeByte(P_ID);
            buffer.writeInt(id);
        }
        if (color != null) {
            buffer.writeByte(P_COLOR);
            buffer.writeInt(color);
        }
        if (colorId != null) {
            buffer.writeByte(P_COLOR_ID);
            buffer.writeInt(colorId);
        }
        if (fontSize != null) {
            buffer.writeByte(P_FONT_SIZE);
            buffer.writeFloat(fontSize);
        }
        if (minFontSize != null) {
            buffer.writeByte(P_MIN_FONT_SIZE);
            buffer.writeFloat(minFontSize);
        }
        if (maxFontSize != null) {
            buffer.writeByte(P_MAX_FONT_SIZE);
            buffer.writeFloat(maxFontSize);
        }
        if (fontStyle != null) {
            buffer.writeByte(P_FONT_STYLE);
            buffer.writeInt(fontStyle);
        }
        if (fontWeight != null) {
            buffer.writeByte(P_FONT_WEIGHT);
            buffer.writeFloat(fontWeight);
        }
        if (fontFamilyId != null) {
            buffer.writeByte(P_FONT_FAMILY);
            buffer.writeInt(fontFamilyId);
        }
        if (textAlign != null) {
            buffer.writeByte(P_TEXT_ALIGN);
            buffer.writeInt(textAlign);
        }
        if (overflow != null) {
            buffer.writeByte(P_OVERFLOW);
            buffer.writeInt(overflow);
        }
        if (maxLines != null) {
            buffer.writeByte(P_MAX_LINES);
            buffer.writeInt(maxLines);
        }
        if (letterSpacing != null) {
            buffer.writeByte(P_LETTER_SPACING);
            buffer.writeFloat(letterSpacing);
        }
        if (lineHeightAdd != null) {
            buffer.writeByte(P_LINE_HEIGHT_ADD);
            buffer.writeFloat(lineHeightAdd);
        }
        if (lineHeightMultiplier != null) {
            buffer.writeByte(P_LINE_HEIGHT_MULTIPLIER);
            buffer.writeFloat(lineHeightMultiplier);
        }
        if (lineBreakStrategy != null) {
            buffer.writeByte(P_BREAK_STRATEGY);
            buffer.writeInt(lineBreakStrategy);
        }
        if (hyphenationFrequency != null) {
            buffer.writeByte(P_HYPHENATION_FREQUENCY);
            buffer.writeInt(hyphenationFrequency);
        }
        if (justificationMode != null) {
            buffer.writeByte(P_JUSTIFICATION_MODE);
            buffer.writeInt(justificationMode);
        }
        if (underline != null) {
            buffer.writeByte(P_UNDERLINE);
            buffer.writeBoolean(underline);
        }
        if (strikethrough != null) {
            buffer.writeByte(P_STRIKETHROUGH);
            buffer.writeBoolean(strikethrough);
        }
        if (hasFontAxis) {
            buffer.writeByte(P_FONT_AXIS);
            buffer.writeShort(fontAxis.length);
            for (int axis : fontAxis) {
                buffer.writeInt(axis);
            }
            buffer.writeByte(P_FONT_AXIS_VALUES);
            buffer.writeShort(fontAxisValues.length);
            for (float v : fontAxisValues) {
                buffer.writeFloat(v);
            }
        }
        if (autosize != null) {
            buffer.writeByte(P_AUTOSIZE);
            buffer.writeBoolean(autosize);
        }
        if (parentId != null) {
            buffer.writeByte(P_PARENT_ID);
            buffer.writeInt(parentId);
        }
    }

    /**
     * Read the style from the buffer.
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int paramsLength = buffer.readShort();
        final Integer[] intAttributes = new Integer[15];
        final Float[] floatAttributes = new Float[7];
        final Boolean[] boolAttributes = new Boolean[3];
        final ArrayList<Integer> fontAxisList = new ArrayList<>();
        final ArrayList<Float> fontAxisValuesList = new ArrayList<>();

        for (int i = 0; i < paramsLength; i++) {
            PARAMETERS.read(buffer, new CommandParameters.Callback() {
                @Override
                public void value(int id, int value) {
                    switch (id) {
                        case P_ID:
                            intAttributes[0] = value;
                            break;
                        case P_COLOR:
                            intAttributes[1] = value;
                            break;
                        case P_COLOR_ID:
                            intAttributes[2] = value;
                            break;
                        case P_FONT_STYLE:
                            intAttributes[3] = value;
                            break;
                        case P_FONT_FAMILY:
                            intAttributes[4] = value;
                            break;
                        case P_TEXT_ALIGN:
                            intAttributes[5] = value;
                            break;
                        case P_OVERFLOW:
                            intAttributes[6] = value;
                            break;
                        case P_MAX_LINES:
                            intAttributes[7] = value;
                            break;
                        case P_BREAK_STRATEGY:
                            intAttributes[8] = value;
                            break;
                        case P_HYPHENATION_FREQUENCY:
                            intAttributes[9] = value;
                            break;
                        case P_JUSTIFICATION_MODE:
                            intAttributes[10] = value;
                            break;
                        case P_PARENT_ID:
                            intAttributes[11] = value;
                            break;
                        case P_ANIMATION_ID:
                            intAttributes[12] = value;
                            break;
                        case P_FLAGS:
                            intAttributes[13] = value;
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
                            boolAttributes[0] = value;
                            break;
                        case P_STRIKETHROUGH:
                            boolAttributes[1] = value;
                            break;
                        case P_AUTOSIZE:
                            boolAttributes[2] = value;
                            break;
                    }
                }

                @Override
                public void value(int id, @NonNull String value) {
                }

                @Override
                public void value(int id, int @NonNull [] value) {
                    if (id == P_FONT_AXIS) {
                        for (int axis : value) {
                            fontAxisList.add(axis);
                        }
                    }
                }

                @Override
                public void value(int id, float @NonNull [] value) {
                    if (id == P_FONT_AXIS_VALUES) {
                        for (float v : value) {
                            fontAxisValuesList.add(v);
                        }
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
                new TextStyle(intAttributes[0] == null ? -1 : intAttributes[0], intAttributes[1],
                        intAttributes[2], floatAttributes[0], floatAttributes[1],
                        floatAttributes[2], intAttributes[3], floatAttributes[3], intAttributes[4],
                        intAttributes[5], intAttributes[6], intAttributes[7], floatAttributes[4],
                        floatAttributes[5], floatAttributes[6], intAttributes[8], intAttributes[9],
                        intAttributes[10], boolAttributes[0], boolAttributes[1], fontAxis,
                        fontAxisValues, boolAttributes[2], intAttributes[11]));
    }

    /**
     * Return the operation name
     * @return
     */
    @NonNull
    public static String name() {
        return "TextStyle";
    }

    @Override
    @NonNull
    public String deepToString(@NonNull String indent) {
        return indent + name() + "\n";
    }

    /**
     * Documentation for the operation
     * @param doc
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Text Operations", id(), name()).description(
                "Text style implementation").field(DocumentedOperation.INT, "id",
                "The ID of the text style").field(DocumentedOperation.INT, "color",
                "The text color (ARGB)").field(DocumentedOperation.INT, "colorId",
                "The ID of the color variable").field(DocumentedOperation.FLOAT, "fontSize",
                "The font size in pixels").field(DocumentedOperation.INT, "fontStyle",
                "The font style (0=normal, 1=italic)").field(DocumentedOperation.FLOAT,
                "fontWeight", "The font weight [1..1000]").field(DocumentedOperation.INT,
                "fontFamilyId", "The ID of the font family name string").field(
                DocumentedOperation.INT, "textAlign", "The text alignment and flags").field(
                DocumentedOperation.INT, "overflow", "The text overflow strategy").field(
                DocumentedOperation.INT, "maxLines",
                "The maximum number of lines to display").field(DocumentedOperation.FLOAT,
                "letterSpacing", "The letter spacing in ems").field(DocumentedOperation.FLOAT,
                "lineHeightAdd", "The line height addition").field(DocumentedOperation.FLOAT,
                "lineHeightMultiplier", "The line height multiplier").field(DocumentedOperation.INT,
                "lineBreakStrategy", "The line break strategy").field(DocumentedOperation.INT,
                "hyphenationFrequency", "The hyphenation frequency").field(DocumentedOperation.INT,
                "justificationMode", "The justification mode").field(DocumentedOperation.BOOLEAN,
                "underline", "Whether to underline the text").field(DocumentedOperation.BOOLEAN,
                "strikethrough", "Whether to strike through the text").field(
                DocumentedOperation.INT_ARRAY, "fontAxis", "Font axis tags").field(
                DocumentedOperation.FLOAT_ARRAY, "fontAxisValues", "Font axis values").field(
                DocumentedOperation.BOOLEAN, "autosize", "Whether to enable autosize").field(
                DocumentedOperation.INT, "parentId", "The ID of the parent text style");
    }
}
