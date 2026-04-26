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
public class DrawText extends PaintOperation implements VariableSupport, ComponentData {
    private static final int OP_CODE = Operations.DRAW_TEXT_RUN;
    private static final String CLASS_NAME = "DrawText";
    public int mTextID;
    public int mStart = 0;
    public int mEnd = 0;
    public int mContextStart = 0;
    public int mContextEnd = 0;
    public float mX = 0f;
    public float mY = 0f;
    public float mOutX = 0f;
    public float mOutY = 0f;
    public boolean mRtl = false;

    public DrawText(
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {
        mTextID = textId;
        mStart = start;
        mEnd = end;
        mContextStart = contextStart;
        mContextEnd = contextEnd;
        mOutX = mX = x;
        mOutY = mY = y;
        mRtl = rtl;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        context.drawTextRun(mTextID, mStart, mEnd, mContextStart, mContextEnd, mOutX, mOutY, mRtl);
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutX = Float.isNaN(mX) ? context.getFloat(Utils.idFromNan(mX)) : mX;
        mOutY = Float.isNaN(mY) ? context.getFloat(Utils.idFromNan(mY)) : mY;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mTextID, this);
        if (Float.isNaN(mX)) {
            context.listensTo(Utils.idFromNan(mX), this);
        }
        if (Float.isNaN(mY)) {
            context.listensTo(Utils.idFromNan(mY), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextID, mStart, mEnd, mContextStart, mContextEnd, mX, mY, mRtl);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawTextRun ["
                + mTextID
                + "] "
                + mStart
                + ", "
                + mEnd
                + ", "
                + floatToString(mX, mOutX)
                + ", "
                + floatToString(mY, mOutY);
    }

    /**
     * write DrawText to the buffer
     *
     * @param buffer
     * @param textId
     * @param start
     * @param end
     * @param contextStart
     * @param contextEnd
     * @param x
     * @param y
     * @param rtl
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int textId,
            int start,
            int end,
            int contextStart,
            int contextEnd,
            float x,
            float y,
            boolean rtl) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeInt(start);
        buffer.writeInt(end);
        buffer.writeInt(contextStart);
        buffer.writeInt(contextEnd);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
        buffer.writeBoolean(rtl);
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to the remap context
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int text = buffer.readId();
        int start = buffer.readInt();
        int end = buffer.readInt();
        int contextStart = buffer.readInt();
        int contextEnd = buffer.readInt();
        float x = buffer.readNanId();
        float y = buffer.readNanId();
        boolean rtl = buffer.readBoolean();
        DrawText op = new DrawText(text, start, end, contextStart, contextEnd, x, y, rtl);
        operations.add(op);
    }

    /**
     * Documentation of the operation
     *
     * @param doc
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Text Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("draw_text")
                .description("Draw a run of text, all in a single direction")
                .field(DocumentedOperation.INT, "textId", "The ID of the text to render")
                .field(DocumentedOperation.INT, "start", "The start index of the text to render")
                .field(DocumentedOperation.INT, "end", "The end index of the text to render")
                .field(
                        DocumentedOperation.INT,
                        "contextStart",
                        "The index of the start of the shaping context")
                .field(
                        DocumentedOperation.INT,
                        "contextEnd",
                        "The index of the end of the shaping context")
                .field(DocumentedOperation.FLOAT, "x", "The x position at which to draw the text")
                .field(DocumentedOperation.FLOAT, "y", "The y position at which to draw the text")
                .field(DocumentedOperation.BOOLEAN, "rtl", "Whether the run is in RTL direction");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("textId", mTextID)
                .add("start", mStart)
                .add("end", mEnd)
                .add("x", mX)
                .add("y", mY);
    }
}
