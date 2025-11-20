/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static androidx.compose.remote.core.operations.Utils.floatToString;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Draw Text */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawBitmapTextAnchored extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.DRAW_BITMAP_TEXT_ANCHORED;
    private static final String CLASS_NAME = "DrawBitmapTextAnchored";
    int mTextID;
    int mBitmapFontID;
    float mStart;
    float mOutStart;
    float mEnd;
    float mOutEnd;
    float mX;
    float mY;
    float mOutX;
    float mOutY;
    float mPanX;
    float mPanY;
    float mOutPanX;
    float mOutPanY;

    public DrawBitmapTextAnchored(
            int textId,
            int bitmapFontID,
            float start,
            float end,
            float x,
            float y,
            float panX,
            float panY) {
        mTextID = textId;
        mBitmapFontID = bitmapFontID;
        mOutStart = mStart = start;
        mOutEnd = mEnd = end;
        mOutX = mX = x;
        mOutY = mY = y;
        mOutPanX = mPanX = panX;
        mOutPanY = mPanY = panY;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutX = Float.isNaN(mX) ? context.getFloat(Utils.idFromNan(mX)) : mX;
        mOutY = Float.isNaN(mY) ? context.getFloat(Utils.idFromNan(mY)) : mY;
        mOutPanX = Float.isNaN(mPanX) ? context.getFloat(Utils.idFromNan(mPanX)) : mPanX;
        mOutPanY = Float.isNaN(mPanY) ? context.getFloat(Utils.idFromNan(mPanY)) : mPanY;
        mOutStart = Float.isNaN(mStart) ? context.getFloat(Utils.idFromNan(mStart)) : mStart;
        mOutEnd = Float.isNaN(mEnd) ? context.getFloat(Utils.idFromNan(mEnd)) : mEnd;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mX)) {
            context.listensTo(Utils.idFromNan(mX), this);
        }
        if (Float.isNaN(mY)) {
            context.listensTo(Utils.idFromNan(mY), this);
        }
        if (Float.isNaN(mPanX)) {
            context.listensTo(Utils.idFromNan(mPanX), this);
        }
        if (Float.isNaN(mPanY)) {
            context.listensTo(Utils.idFromNan(mPanY), this);
        }
        if (Float.isNaN(mStart)) {
            context.listensTo(Utils.idFromNan(mStart), this);
        }
        if (Float.isNaN(mEnd)) {
            context.listensTo(Utils.idFromNan(mEnd), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextID, mBitmapFontID, mStart, mEnd, mX, mY, mPanX, mPanY);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawBitmapFontText ["
                + mTextID
                + "] "
                + mBitmapFontID
                + ", "
                + mStart
                + ", "
                + mEnd
                + ", "
                + floatToString(mX, mOutX)
                + ", "
                + floatToString(mY, mOutY);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int text = buffer.readInt();
        int bitmapFont = buffer.readInt();
        float start = buffer.readFloat();
        float end = buffer.readFloat();
        float x = buffer.readFloat();
        float y = buffer.readFloat();
        float panX = buffer.readFloat();
        float panY = buffer.readFloat();

        DrawBitmapTextAnchored op =
                new DrawBitmapTextAnchored(text, bitmapFont, start, end, x, y, panX, panY);

        operations.add(op);
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
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
     * @param buffer write the command to the buffer
     * @param textId id of the text
     * @param bitmapFontID id of the bitmap font
     * @param start Start position
     * @param end end position
     * @param x position of where to draw
     * @param y position of where to draw
     * @param panX panX
     * @param panY panY
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int textId,
            int bitmapFontID,
            float start,
            float end,
            float x,
            float y,
            float panX,
            float panY) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeInt(bitmapFontID);
        buffer.writeFloat(start);
        buffer.writeFloat(end);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
        buffer.writeFloat(panX);
        buffer.writeFloat(panY);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", id(), CLASS_NAME)
                .description("Draw a bitmap font text, all in a single direction")
                .field(DocumentedOperation.INT, "textId", "id of bitmap")
                .field(DocumentedOperation.INT, "bitmapFontId", "id of the bitmap font")
                .field(
                        DocumentedOperation.FLOAT,
                        "start",
                        "The start of the text to render. -1=end of string")
                .field(DocumentedOperation.FLOAT, "end", "The end of the text to render")
                .field(
                        DocumentedOperation.FLOAT,
                        "x",
                        "The x anchor point to which to draw the text")
                .field(
                        DocumentedOperation.FLOAT,
                        "y",
                        "The y anchor point to which to draw the text")
                .field(
                        DocumentedOperation.FLOAT,
                        "panX",
                        "The x position relative to the anchor point")
                .field(
                        DocumentedOperation.FLOAT,
                        "panY",
                        "The y position relative to the anchor point");
    }

    float @NonNull [] mBounds = new float[4];

    private void measure(BitmapFontData bitmapFont, String textToMeasure) {
        float xMin = 0;
        float yMin = 1000;
        float xMax = 0;
        float yMax = -Float.MAX_VALUE;
        float xPos = 0;
        int pos = 0;
        while (pos < textToMeasure.length()) {
            BitmapFontData.Glyph glyph = bitmapFont.lookupGlyph(textToMeasure, pos);
            if (glyph == null) {
                pos++;
                continue;
            }

            pos += glyph.mChars.length();
            xPos += glyph.mMarginLeft + glyph.mMarginRight;
            if (glyph.mBitmapId != -1) {
                // Space is represented by a glyph of -1.
                xPos += glyph.mBitmapWidth;
            }
            xMax = xPos;
            yMax = Math.max(yMax, glyph.mBitmapHeight + glyph.mMarginTop + glyph.mMarginBottom);
            yMin = Math.min(yMin, glyph.mMarginTop);
        }

        mBounds[0] = xMin;
        mBounds[1] = yMin;
        mBounds[2] = xMax;
        mBounds[3] = yMax;
    }

    private float getHorizontalOffset() {
        // TODO scale  TextSize / BaseTextSize;
        float scale = 1.0f;

        float textWidth = scale * (mBounds[2] - mBounds[0]);
        float boxWidth = 0;
        return (boxWidth - textWidth) * (1 + mOutPanX) / 2.f - (scale * mBounds[0]);
    }

    private float getVerticalOffset() {
        // TODO scale TextSize / BaseTextSize;
        float scale = 1.0f;
        float boxHeight = 0;
        float textHeight = scale * (mBounds[3] - mBounds[1]);
        return (boxHeight - textHeight) * (1 - mOutPanY) / 2 - (scale * mBounds[1]);
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        String textToPaint = remoteContext.getText(mTextID);
        if (textToPaint == null) {
            return;
        }
        int end = (int) mOutEnd;
        int start = (int) mOutStart;

        textToPaint =
                textToPaint.substring(
                        Math.max(start, 0),
                        (end < 0 || end > textToPaint.length()) ? textToPaint.length() : end);
        BitmapFontData bitmapFont = (BitmapFontData) remoteContext.getObject(mBitmapFontID);
        if (bitmapFont == null) {
            return;
        }
        measure(bitmapFont, textToPaint);

        float xPos = mOutX + getHorizontalOffset();
        float yPos = mOutY + getVerticalOffset();

        int pos = 0;
        String prevGlyph = "";
        while (pos < textToPaint.length()) {
            BitmapFontData.Glyph glyph = bitmapFont.lookupGlyph(textToPaint, pos);
            if (glyph == null) {
                pos++;
                prevGlyph = "";
                continue;
            }

            pos += glyph.mChars.length();
            if (glyph.mBitmapId == -1) {
                xPos += glyph.mMarginLeft + glyph.mMarginRight;
                prevGlyph = glyph.mChars;
                continue;
            }

            xPos += glyph.mMarginLeft;
            Short kerningAdjustment = bitmapFont.mKerningTable.get(prevGlyph + glyph.mChars);
            if (kerningAdjustment != null) {
                xPos += kerningAdjustment;
            }

            float xPos2 = xPos + glyph.mBitmapWidth;
            context.drawBitmap(
                    glyph.mBitmapId,
                    xPos,
                    yPos + glyph.mMarginTop,
                    xPos2,
                    yPos + glyph.mBitmapHeight + glyph.mMarginTop);
            xPos = xPos2 + glyph.mMarginRight;
            prevGlyph = glyph.mChars;
        }
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("textId", mTextID)
                .add("bitmapFontId", mBitmapFontID)
                .add("start", mStart)
                .add("end", mEnd)
                .add("x", mX, mOutX)
                .add("y", mY, mOutY);
    }
}
