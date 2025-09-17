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
package androidx.compose.remote.core.operations.matrix;

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.MatrixAccess;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/** This is for a constant matrix */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MatrixConstant extends Operation implements Serializable, MatrixAccess {
    private static final int OP_CODE = Operations.MATRIX_CONSTANT;
    private static final String CLASS_NAME = "MatrixConstant";
    private final int mMatrixId;
    private final int mType;
    private float @NonNull [] mValues;

    public MatrixConstant(int matrixId, int type, float @NonNull [] values) {
        this.mMatrixId = matrixId;
        this.mType = type;
        this.mValues = values;
    }

    /**
     * Copy the value from another operation
     *
     * @param from value to copy from
     */
    public void update(@NonNull MatrixConstant from) {
        mValues = from.mValues;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mMatrixId, mType, mValues);
    }

    @NonNull
    @Override
    public String toString() {
        return "FloatConstant[" + mMatrixId + "] = " + Arrays.toString(mValues);
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
     * @param buffer write command to this buffer
     * @param matrixId the id
     * @param type the type of matrix it is
     * @param values the value of the float
     */
    public static void apply(
            @NonNull WireBuffer buffer, int matrixId, int type, float @NonNull [] values) {
        buffer.start(OP_CODE);
        buffer.writeInt(matrixId);
        buffer.writeInt(type);
        buffer.writeInt(values.length);
        for (int i = 0; i < values.length; i++) {
            buffer.writeFloat(values[i]);
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        int type = buffer.readInt();
        int len = buffer.readInt();
        if (len > 16 || len < 0) {
            throw new RuntimeException("Invalid matrix length: " + len + " corrupt buffer");
        }
        float[] matrix = new float[len];
        for (int i = 0; i < len; i++) {
            matrix[i] = buffer.readFloat();
        }
        operations.add(new MatrixConstant(id, type, matrix));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Expressions Operations", OP_CODE, CLASS_NAME)
                .description("A float and its associated id")
                .field(DocumentedOperation.INT, "id", "id of float")
                .field(FLOAT, "value", "32-bit float value");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        context.putObject(mMatrixId, this);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("matrix", mMatrixId)
                .add("type", mType)
                .addFloatExpressionSrc("value", mValues);
    }

    @Override
    public float @NonNull [] get() {
        return mValues;
    }
}
