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
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.Utils;
import androidx.compose.remote.core.operations.utilities.Matrix;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/** this evaluates a matrix * vector and outputs a vector */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class MatrixVectorMath extends Operation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.MATRIX_VECTOR_MATH;
    private static final String CLASS_NAME = "MatrixVectorMath";
    private final int[] mOutputs;
    private final short mType;
    private final float[] mInputs;
    private final float[] mOutInputs;
    private final float[] mTempOut;
    Matrix mMatrix = new Matrix();
    public int mMatrixId;

    public MatrixVectorMath(
            short type, int @NonNull [] outputs, int matrixId, float @NonNull [] inputs) {
        mType = type;
        this.mMatrixId = matrixId;
        this.mOutputs = outputs;
        mOutInputs = new float[outputs.length];
        this.mInputs = inputs;
        mTempOut = new float[outputs.length];
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {

        for (int i = 0; i < mInputs.length; i++) {
            float v = mInputs[i];
            if (Float.isNaN(v)) {

                float newValue = context.getFloat(Utils.idFromNan(v));
                mOutInputs[i] = newValue;
            } else {
                mOutInputs[i] = mInputs[i];
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        context.listensTo(mMatrixId, this);
        for (int i = 0; i < mInputs.length; i++) {
            float v = mInputs[i];
            if (Float.isNaN(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mType, mOutputs, mMatrixId, mInputs);
    }

    @NonNull
    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < mInputs.length; i++) {
            str += " " + Utils.floatToString(mInputs[i], mOutInputs[i]);
        }
        return "MatrixVectorMath " + Arrays.toString(mOutputs) + " " + mMatrixId + " *" + str;
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
     * @param type the type of the operation
     * @param outputs the ids to write the output vector
     * @param matrixId the id
     * @param inputs input vector
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            short type,
            int @NonNull [] outputs,
            int matrixId,
            float @NonNull [] inputs) {
        buffer.start(OP_CODE);
        buffer.writeShort(type);
        buffer.writeInt(matrixId);
        buffer.writeInt(outputs.length);
        for (int i = 0; i < outputs.length; i++) {
            buffer.writeInt(outputs[i]);
        }

        buffer.writeInt(inputs.length);
        for (int i = 0; i < inputs.length; i++) {
            buffer.writeFloat(inputs[i]);
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        short type = (short) buffer.readShort();
        int matrixId = buffer.readInt();
        int lenOut = buffer.readInt();
        if (lenOut > 4 || lenOut < 1) {
            throw new IllegalArgumentException("Invalid Length " + lenOut + " corrupt buffer");
        }
        int[] out = new int[lenOut];
        for (int i = 0; i < out.length; i++) {
            out[i] = buffer.readInt();
        }

        int lenIn = buffer.readInt();
        if (lenIn > 4 || lenIn < 1) {
            throw new IllegalArgumentException("Invalid Length " + lenOut + " corrupt buffer");
        }
        float[] in = new float[lenIn];
        for (int i = 0; i < in.length; i++) {
            in[i] = buffer.readFloat();
        }

        operations.add(new MatrixVectorMath(type, out, matrixId, in));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Expressions Operations", OP_CODE, CLASS_NAME)
                .description("A float and its associated id")
                .field(DocumentedOperation.INT, "matrixId", "id of Matrix")
                .field(DocumentedOperation.SHORT, "opType", "The type of op 0=multiply")
                .field(DocumentedOperation.INT, "outLength", "The length of the output vector")
                .field(FLOAT, "value", "outLength", "32-bit float value")
                .field(DocumentedOperation.INT, "inLength", "The length of the input vector")
                .field(FLOAT, "value", "inLength", "32-bit float value");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        MatrixAccess m = (MatrixAccess) context.getObject(mMatrixId);
        mMatrix.copyFrom(m.get());
        if (mType == 0) {
            mMatrix.multiply(mOutInputs, mTempOut);
        } else {
            mMatrix.evalPerspective(mOutInputs, mTempOut);
        }
        for (int i = 0; i < mOutputs.length; i++) {
            context.loadFloat(mOutputs[i], mTempOut[i]);
        }
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
                .addFloatExpressionSrc("input", mInputs)
                .add("output", Arrays.toString(mOutputs));
    }
}
