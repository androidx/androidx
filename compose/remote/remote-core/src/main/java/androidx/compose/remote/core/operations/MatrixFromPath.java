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

/** The rotate the rendering command */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MatrixFromPath extends PaintOperation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.MATRIX_FROM_PATH;
    private static final String CLASS_NAME = "MatrixFromPath";
    int mPathId;
    float mVOffset;
    float mFraction;
    float mOutVOffset;
    float mOutFraction;
    int mFlags;

    public static final int POSITION_MATRIX_FLAG = 0x01; // must match flags in SkPathMeasure.h
    public static final int TANGENT_MATRIX_FLAG = 0x02; // must match flags in SkPathMeasure.h

    public MatrixFromPath(int pathId, float percent, float vOffset, int flags) {
        mPathId = pathId;
        mOutFraction = mFraction = percent;
        mOutVOffset = mVOffset = vOffset;
        mFlags = flags;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutFraction =
                Float.isNaN(mFraction) ? context.getFloat(Utils.idFromNan(mFraction)) : mFraction;
        mOutVOffset =
                Float.isNaN(mVOffset) ? context.getFloat(Utils.idFromNan(mVOffset)) : mVOffset;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mFraction)) {
            context.listensTo(Utils.idFromNan(mFraction), this);
        }
        if (Float.isNaN(mVOffset)) {
            context.listensTo(Utils.idFromNan(mVOffset), this);
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mPathId, mFraction, mVOffset, mFlags);
    }

    @NonNull
    @Override
    public String toString() {
        return "DrawTextOnPath ["
                + mPathId
                + "] "
                + Utils.floatToString(mFraction, mOutFraction)
                + ", "
                + Utils.floatToString(mVOffset, mOutVOffset)
                + ", "
                + mFlags;
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {

        int pathId = buffer.readInt();
        float percent = buffer.readFloat();
        float vOffset = buffer.readFloat();
        int flags = buffer.readInt();
        MatrixFromPath op = new MatrixFromPath(pathId, percent, vOffset, flags);
        operations.add(op);
    }

    /**
     * The name of the class
     *
     * @return the name
     */
    @NonNull
    public static String name() {
        return "DrawTextOnPath";
    }

    /**
     * The OP_CODE for this command
     *
     * @return the opcode
     */
    public static int id() {
        return Operations.DRAW_TEXT_ON_PATH;
    }

    /**
     * set the Matrix relative to the path
     *
     * @param buffer the buffer to add to
     * @param pathId the id of the path
     * @param percent the position on path
     * @param vOffset the vertical offset to position the string
     * @param flags the flags to use
     */
    public static void apply(
            @NonNull WireBuffer buffer, int pathId, float percent, float vOffset, int flags) {
        buffer.start(OP_CODE);
        buffer.writeInt(pathId);
        buffer.writeFloat(percent);
        buffer.writeFloat(vOffset);
        buffer.writeInt(flags);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Draw Operations", OP_CODE, CLASS_NAME)
                .description("Draw text along path object")
                .field(DocumentedOperation.INT, "textId", "id of the text")
                .field(DocumentedOperation.INT, "pathId", "id of the path")
                .field(DocumentedOperation.FLOAT, "xOffset", "x Shift of the text")
                .field(DocumentedOperation.FLOAT, "yOffset", "y Shift of the text");
    }

    @Override
    public void paint(@NonNull PaintContext context) {

        context.matrixFromPath(mPathId, mOutFraction, mOutVOffset, mFlags);
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("pathId", mPathId)
                .add("vOffset", mVOffset, mOutVOffset)
                .add("hOffset", mFraction, mOutFraction);
    }
}
