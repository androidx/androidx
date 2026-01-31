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
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.List;

/** Draw curved text on a circle. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DrawTextOnCircle extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.DRAW_TEXT_ON_CIRCLE;
    private static final String CLASS_NAME = "DrawTextOnCircle";

    public int mTextId;
    float mCenterX;
    float mCenterY;
    float mRadius;
    float mStartAngle;
    float mWarpRadiusOffset;
    Alignment mAlignment;
    Placement mPlacement;

    public enum Alignment {
        START,
        CENTER,
        END,
    }

    public enum Placement {
        OUTSIDE,
        INSIDE,
    }

    public DrawTextOnCircle(int textId, float centerX, float centerY, float radius,
            float startAngle, float warpRadiusOffset, @NonNull Alignment alignment,
            @NonNull Placement placement) {
        mTextId = textId;
        mCenterX = centerX;
        mCenterY = centerY;
        mRadius = radius;
        mStartAngle = startAngle;
        mWarpRadiusOffset = warpRadiusOffset;
        mAlignment = alignment;
        mPlacement = placement;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mTextId, this);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mTextId, mCenterX, mCenterY, mRadius, mStartAngle, mWarpRadiusOffset,
                mAlignment, mPlacement);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawTextOnCircle ["
                + mTextId
                + "]";
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int textId = buffer.readInt();
        float centerX = buffer.readFloat();
        float centerY = buffer.readFloat();
        float radius = buffer.readFloat();
        float startAngle = buffer.readFloat();
        float warpRadiusOffset = buffer.readFloat();
        Alignment alignment = Alignment.values()[buffer.readByte()];
        Placement placement = Placement.values()[buffer.readByte()];

        operations.add(
                new DrawTextOnCircle(textId, centerX, centerY, radius, startAngle, warpRadiusOffset,
                        alignment, placement));
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "DrawTextOnCircle";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.DRAW_TEXT_ON_CIRCLE;
    }

    /**
     * add a draw text on circle operation to the buffer
     */
    public static void apply(
            @NonNull WireBuffer buffer, int textId, float centerX, float centerY, float radius,
            float startAngle, float warpRadiusOffset, @NonNull Alignment alignment,
            @NonNull Placement placement) {
        buffer.start(OP_CODE);
        buffer.writeInt(textId);
        buffer.writeFloat(centerX);
        buffer.writeFloat(centerY);
        buffer.writeFloat(radius);
        buffer.writeFloat(startAngle);
        buffer.writeFloat(warpRadiusOffset);
        buffer.writeByte(alignment.ordinal());
        buffer.writeByte(placement.ordinal());
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Text Operations", OP_CODE, CLASS_NAME)
                .additionalDocumentation("draw_text_on_circle")
                .description("Draw text along a circle")
                .field(DocumentedOperation.INT, "textId", "The ID of the text")
                .field(DocumentedOperation.FLOAT, "centerX", "The x coordinate of the center")
                .field(DocumentedOperation.FLOAT, "centerY", "The y coordinate of the center")
                .field(DocumentedOperation.FLOAT, "radius", "The radius of the circle")
                .field(DocumentedOperation.FLOAT, "startAngle", "The start angle in degrees")
                .field(DocumentedOperation.FLOAT, "warpRadiusOffset", "The warp radius offset")
                .field(DocumentedOperation.INT, "alignment", "The alignment of the text")
                .possibleValues("START", Alignment.START.ordinal())
                .possibleValues("CENTER", Alignment.CENTER.ordinal())
                .possibleValues("END", Alignment.END.ordinal())
                .field(DocumentedOperation.INT, "placement", "The placement of the text")
                .possibleValues("OUTSIDE", Placement.OUTSIDE.ordinal())
                .possibleValues("INSIDE", Placement.INSIDE.ordinal());
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        throw new UnsupportedOperationException("DrawTextOnCircle is not supported");
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("textId", mTextId)
                .add("centerX", mCenterX)
                .add("centerY", mCenterY)
                .add("radius", mRadius)
                .add("startAngle", mStartAngle)
                .add("warpRadiusOffset", mWarpRadiusOffset)
                .add("alignment", mAlignment)
                .add("placement", mPlacement)
        ;
    }
}
