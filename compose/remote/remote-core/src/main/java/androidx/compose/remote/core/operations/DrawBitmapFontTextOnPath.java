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

/** Draw bitmap font text on a path. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawBitmapFontTextOnPath extends PaintOperation implements VariableSupport {
    private static final int OP_CODE = Operations.DRAW_BITMAP_FONT_TEXT_RUN_ON_PATH;
    private static final String CLASS_NAME = "DrawBitmapFontTextOnPath";
    int mTextID;
    int mBitmapFontID;
    int mPathID;
    int mStart;
    int mEnd;
    float mYAdj;
    float mOutYAdj;

    public DrawBitmapFontTextOnPath(
            int textID, int bitmapFontID, int pathID, int start, int end, float yAdj) {
        mTextID = textID;
        mBitmapFontID = bitmapFontID;
        mPathID = pathID;
        mStart = start;
        mEnd = end;
        mYAdj = yAdj;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextID, mBitmapFontID, mPathID, mStart, mEnd, mYAdj);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutYAdj = Float.isNaN(mYAdj) ? context.getFloat(Utils.idFromNan(mYAdj)) : mYAdj;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mYAdj)) {
            context.listensTo(Utils.idFromNan(mYAdj), this);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawBitmapFontTextOnPath ["
                + mTextID
                + "] "
                + mBitmapFontID
                + ", "
                + mPathID
                + ", "
                + mStart
                + ", "
                + mEnd
                + ", "
                + mYAdj;
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
        int path = buffer.readInt();
        int start = buffer.readInt();
        int end = buffer.readInt();
        float yAdj = buffer.readFloat();
        DrawBitmapFontTextOnPath op =
                new DrawBitmapFontTextOnPath(text, bitmapFont, path, start, end, yAdj);

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
     * @param pathID id of the bitmap font
     * @param start Start position
     * @param end end position
     * @param yAdj position of where to draw
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int textId,
            int bitmapFontID,
            int pathID,
            int start,
            int end,
            float yAdj) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeInt(bitmapFontID);
        buffer.writeInt(pathID);
        buffer.writeInt(start);
        buffer.writeInt(end);
        buffer.writeFloat(yAdj);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", id(), CLASS_NAME)
                .description("Draw a run of bitmap font text, all in a single direction")
                .field(DocumentedOperation.INT, "textId", "id of bitmap")
                .field(DocumentedOperation.INT, "bitmapFontId", "id of the bitmap font")
                .field(DocumentedOperation.INT, "pathId", "id of the path")
                .field(
                        DocumentedOperation.INT,
                        "start",
                        "The start of the text to render. -1=end of string")
                .field(DocumentedOperation.INT, "end", "The end of the text to render")
                .field(
                        DocumentedOperation.INT,
                        "contextStart",
                        "the index of the start of the shaping context")
                .field(
                        DocumentedOperation.INT,
                        "contextEnd",
                        "the index of the end of the shaping context")
                .field(
                        DocumentedOperation.FLOAT,
                        "yAdj",
                        "the index of the end of the shaping context");
    }

    private int measureWidth(String text, BitmapFontData bitmapFont) {
        int pos = 0;
        int width = 0;
        String prevGlyph = "";
        while (pos < text.length()) {
            BitmapFontData.Glyph glyph = bitmapFont.lookupGlyph(text, pos);
            if (glyph == null) {
                pos++;
                prevGlyph = "";
                continue;
            }

            pos += glyph.mChars.length();
            if (glyph.mBitmapId == -1) {
                // Space is represented by a glyph of -1.
                width += glyph.mMarginLeft + glyph.mMarginRight;
                prevGlyph = "";
                continue;
            }

            width += glyph.mMarginLeft;
            Short kerningAdjustment = bitmapFont.mKerningTable.get(prevGlyph + glyph.mChars);
            if (kerningAdjustment != null) {
                width += kerningAdjustment;
            }

            width += glyph.mBitmapWidth + glyph.mMarginRight;
            prevGlyph = glyph.mChars;
        }

        return width;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        String textToPaint = remoteContext.getText(mTextID);
        if (textToPaint == null) {
            return;
        }
        if (mEnd == -1) {
            if (mStart != 0) {
                textToPaint = textToPaint.substring(mStart);
            }
        } else if (mEnd > textToPaint.length()) {
            textToPaint = textToPaint.substring(mStart);
        } else {
            textToPaint = textToPaint.substring(mStart, mEnd);
        }

        BitmapFontData bitmapFont = (BitmapFontData) remoteContext.getObject(mBitmapFontID);
        if (bitmapFont == null) {
            return;
        }

        float width = (float) measureWidth(textToPaint, bitmapFont);
        float progress = 0f;
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
                // Space is represented by a glyph of -1.
                progress += glyph.mMarginLeft + glyph.mMarginRight;
                prevGlyph = "";
                continue;
            }

            progress += glyph.mMarginLeft;
            Short kerningAdjustment = bitmapFont.mKerningTable.get(prevGlyph + glyph.mChars);
            if (kerningAdjustment != null) {
                progress += kerningAdjustment;
            }

            float halfGlyphWidth = 0.5f * (float) glyph.mBitmapWidth;
            float fractionAtMiddleOfGlyph = (progress + halfGlyphWidth) / width;
            context.save();
            context.matrixFromPath(mPathID, fractionAtMiddleOfGlyph, 0, 3);
            context.drawBitmap(
                    glyph.mBitmapId,
                    -halfGlyphWidth,
                    mOutYAdj + glyph.mMarginTop,
                    halfGlyphWidth,
                    mOutYAdj + glyph.mBitmapHeight + glyph.mMarginTop);
            progress += glyph.mBitmapWidth + glyph.mMarginRight;
            prevGlyph = glyph.mChars;
            context.restore();
        }
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("textId", mTextID)
                .add("bitmapFontId", mBitmapFontID)
                .add("path", mPathID)
                .add("start", mStart)
                .add("end", mEnd);
    }
}
