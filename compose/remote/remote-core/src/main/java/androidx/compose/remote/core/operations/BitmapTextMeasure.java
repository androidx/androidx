/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.PaintContext.TEXT_MEASURE_FONT_HEIGHT;
import static androidx.compose.remote.core.PaintContext.TEXT_MEASURE_MONOSPACE_WIDTH;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Operation to Measure Text data */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BitmapTextMeasure extends PaintOperation {
    private static final int OP_CODE = Operations.BITMAP_TEXT_MEASURE;
    private static final String CLASS_NAME = "BitmapTextMeasure";
    private final int mBitmapFontId;
    public int mId;
    public int mTextId;
    public int mType;

    public static final int MEASURE_WIDTH = 0;
    public static final int MEASURE_HEIGHT = 1;
    public static final int MEASURE_LEFT = 2;
    public static final int MEASURE_RIGHT = 3;
    public static final int MEASURE_TOP = 4;
    public static final int MEASURE_BOTTOM = 5;

    /** a << 8 shifted {@link PaintContext#getTextBounds} */
    public static final int MEASURE_MONOSPACE_FLAG = TEXT_MEASURE_MONOSPACE_WIDTH << 8;

    public static final int MEASURE_MAX_HEIGHT_FLAG = TEXT_MEASURE_FONT_HEIGHT << 8;

    public BitmapTextMeasure(int id, int textId, int bitmapFontId, int type) {
        mId = id;
        mTextId = textId;
        mBitmapFontId = bitmapFontId;
        mType = type;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mTextId, mBitmapFontId, mType);
    }

    @Override
    public @NonNull String toString() {
        return "FloatConstant[" + mId + "] = " + mTextId + " " + mBitmapFontId + " " + mType;
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    public static @NonNull String name() {
        return CLASS_NAME;
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return OP_CODE;
    }

    /**
     * Writes out the operation to the buffer
     *
     * @param buffer write command to this buffer
     * @param id the id
     * @param textId the id
     * @param bitmapFontId the id of the bitmap font
     * @param type the value of the float
     */
    public static void apply(
            @NonNull WireBuffer buffer, int id, int textId, int bitmapFontId, int type) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(textId);
        buffer.writeInt(bitmapFontId);
        buffer.writeInt(type);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int textId = buffer.readInt();
        int bitmapFontId = buffer.readInt();
        int type = buffer.readInt();
        operations.add(new BitmapTextMeasure(id, textId, bitmapFontId, type));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Expressions Operations", OP_CODE, CLASS_NAME)
                .description("Measure text")
                .field(INT, "id", "id of float result of the measure")
                .field(INT, "textId", "id of text")
                .field(INT, "bitmapFontId", "id of bitmapFont")
                .field(INT, "type", "type: measure 0=width,1=height");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    float @NonNull [] mBounds = new float[4];

    private void measure(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        BitmapFontData bitmapFont = (BitmapFontData) remoteContext.getObject(mBitmapFontId);
        String textToMeasure = remoteContext.getText(mTextId);

        float xMin = 0;
        float yMin = 1000;
        float xMax = 0;
        float yMax = -Float.MAX_VALUE;
        float xPos = 0;
        int pos = 0;
        int len = textToMeasure.length();
        String prevGlyph = "";
        while (pos < len) {
            BitmapFontData.Glyph glyph = bitmapFont.lookupGlyph(textToMeasure, pos);

            if (glyph == null) {
                pos++;
                prevGlyph = "";
                continue;
            }

            pos += glyph.mChars.length();
            xPos += glyph.mMarginLeft + glyph.mMarginRight;
            if (glyph.mBitmapId != -1) {
                // Space is represented by a glyph of -1.
                xPos += glyph.mBitmapWidth;
            }

            Short kerningAdjustment = bitmapFont.mKerningTable.get(prevGlyph + glyph.mChars);
            if (kerningAdjustment != null) {
                xPos += kerningAdjustment;
            }

            xMax = xPos;
            yMax = Math.max(yMax, glyph.mBitmapHeight + glyph.mMarginTop + glyph.mMarginBottom);
            yMin = Math.min(yMin, glyph.mMarginTop);
            prevGlyph = glyph.mChars;
        }

        mBounds[0] = xMin;
        mBounds[1] = yMin;
        mBounds[2] = xMax;
        mBounds[3] = yMax;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        int val = mType & 255;
        // int flags = mType >> 8;
        measure(context);

        switch (val) {
            case MEASURE_WIDTH:
                context.getContext().loadFloat(mId, mBounds[2] - mBounds[0]);
                break;
            case MEASURE_HEIGHT:
                context.getContext().loadFloat(mId, mBounds[3] - mBounds[1]);
                break;
            case MEASURE_LEFT:
                context.getContext().loadFloat(mId, mBounds[0]);
                break;
            case MEASURE_TOP:
                context.getContext().loadFloat(mId, mBounds[1]);
                break;
            case MEASURE_RIGHT:
                context.getContext().loadFloat(mId, mBounds[2]);
                break;
            case MEASURE_BOTTOM:
                context.getContext().loadFloat(mId, mBounds[3]);
                break;
        }
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mId)
                .add("textId", mTextId)
                .add("bitmapFontId", mBitmapFontId)
                .add("measureType", typeToString());
    }

    private String typeToString() {
        int val = mType & 255;
        switch (val) {
            case MEASURE_WIDTH:
                return "MEASURE_WIDTH";
            case MEASURE_HEIGHT:
                return "MEASURE_HEIGHT";
            case MEASURE_LEFT:
                return "MEASURE_LEFT";
            case MEASURE_TOP:
                return "MEASURE_TOP";
            case MEASURE_RIGHT:
                return "MEASURE_RIGHT";
            case MEASURE_BOTTOM:
                return "MEASURE_BOTTOM";
            default:
                return "INVALID_TYPE";
        }
    }
}
