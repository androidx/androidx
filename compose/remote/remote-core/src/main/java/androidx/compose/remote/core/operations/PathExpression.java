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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.operations.utilities.PathGenerator;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Generates a path from expressions */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PathExpression extends Operation implements VariableSupport, Serializable {
    private static final int OP_CODE = Operations.PATH_EXPRESSION;
    private static final String CLASS_NAME = "PathExpression";
    private static final int MAX_EXPRESSION_LENGTH = 32;
    private final PathGenerator mPathGenerator = new PathGenerator();
    private final int mInstanceId;
    private float[] mOutputPath = new float[0];
    private final float[] mExpressionX;
    private final float[] mExpressionY;
    private final float[] mOutExpressionX;
    private final float[] mOutExpressionY;
    private final float mMin;
    private float mOutMin;
    private final float mMax;
    private float mOutMax;
    private float mCount;
    private final float mOutCount;
    private final int mFlags;
    private boolean mPathChanged = true;
    private final int mWinding;

    public static final int LOOP = 1;
    public static final int MONOTONIC = 2;
    public static final int LINEAR = 4;
    public static final int POLAR = 8;
    public static final int WINDING_MASK =  0x3000000;

    PathExpression(
            int instanceId,
            float[] expressionX,
            float[] expressionY,
            float min,
            float max,
            float count,
            int flags) {
        mInstanceId = instanceId;
        mExpressionX = expressionX;
        mExpressionY = expressionY;
        mOutExpressionX = new float[mExpressionX.length];
        mOutExpressionY = new float[mExpressionY.length];
        mMin = min;
        mOutMin = mMin;
        mMax = max;
        mOutMax = mMax;
        mCount = count;
        mOutCount = mCount;
        mFlags = flags;
        mWinding = (flags & WINDING_MASK) >> 24;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (Float.isNaN(mMax)) {
            mOutMax = context.getFloat(Utils.idFromNan(mMax));
        }
        if (Float.isNaN(mMin)) {
            mOutMin = context.getFloat(Utils.idFromNan(mMin));
        }
        if (Float.isNaN(mCount)) {
            mCount = context.getFloat(Utils.idFromNan(mCount));
        }
        for (int i = 0; i < mExpressionX.length; i++) {
            float v = mExpressionX[i];
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)
                    && !NanMap.isVar1(v)) {
                float newValue = context.getFloat(Utils.idFromNan(v));

                mOutExpressionX[i] = newValue;

            } else {
                mOutExpressionX[i] = mExpressionX[i];
            }
        }

        for (int i = 0; i < mExpressionY.length; i++) {
            float v = mExpressionY[i];
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)
                    && !NanMap.isVar1(v)) {
                float newValue = context.getFloat(Utils.idFromNan(v));

                mOutExpressionY[i] = newValue;

            } else {
                mOutExpressionY[i] = mExpressionY[i];
            }
        }
        mPathChanged = true;
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        if (Float.isNaN(mMax)) {
            context.listensTo(Utils.idFromNan(mMax), this);
        }
        if (Float.isNaN(mMin)) {
            context.listensTo(Utils.idFromNan(mMin), this);
        }
        if (Float.isNaN(mCount)) {
            context.listensTo(Utils.idFromNan(mCount), this);
        }

        for (float v : mExpressionX) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
        for (float v : mExpressionY) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mInstanceId, mExpressionX, mExpressionY, mMin, mMax, mCount, mFlags);
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        String[] labelsX = new String[mExpressionX.length];
        for (int i = 0; i < mExpressionX.length; i++) {
            if (Float.isNaN(mExpressionX[i])) {
                labelsX[i] = "[" + Utils.idStringFromNan(mExpressionX[i]) + "]";
            }
        }
        String[] labelsY = new String[mExpressionY.length];
        for (int i = 0; i < mExpressionY.length; i++) {
            if (Float.isNaN(mExpressionY[i])) {
                labelsY[i] = "[" + Utils.idStringFromNan(mExpressionY[i]) + "]";
            }
        }
        return indent
                + "PathExpression["
                + "id="
                + mInstanceId
                + ", flags="
                + mFlags
                + ", min="
                + mMin
                + ", max="
                + mMax
                + ", count="
                + mCount
                + ", expressionX="
                + AnimatedFloatExpression.toString(mExpressionX, labelsX)
                + ", expressionY="
                + AnimatedFloatExpression.toString(mExpressionY, labelsY)
                + "]";
    }

    @NonNull
    @Override
    public String toString() {
        return "PathData[" + mInstanceId + "] = " + "\"" + deepToString(" ") + "\"";
    }

    public static final int MOVE = 10;
    public static final int LINE = 11;
    public static final int QUADRATIC = 12;
    public static final int CONIC = 13;
    public static final int CUBIC = 14;
    public static final int CLOSE = 15;
    public static final int DONE = 16;
    private static final int MASK = -0x800000;
    public static final float MOVE_NAN = Float.intBitsToFloat(MOVE | MASK);
    public static final float LINE_NAN = Float.intBitsToFloat(LINE | MASK);
    public static final float QUADRATIC_NAN = Float.intBitsToFloat(QUADRATIC | MASK);
    public static final float CONIC_NAN = Float.intBitsToFloat(CONIC | MASK);
    public static final float CUBIC_NAN = Float.intBitsToFloat(CUBIC | MASK);
    public static final float CLOSE_NAN = Float.intBitsToFloat(CLOSE | MASK);
    public static final float DONE_NAN = Float.intBitsToFloat(DONE | MASK);

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
     * add this operation to the buffer
     *
     * @param buffer the buffer to add to
     * @param id the id of the image
     * @param expressionX the x expression
     * @param expressionY the y expression
     * @param min the min value of the expression
     * @param max the max value of the expression
     * @param count the number of points in the expression
     * @param flags the flags
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float @NonNull [] expressionX,
            float @Nullable [] expressionY,
            float min,
            float max,
            float count,
            int flags) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeInt(flags);
        buffer.writeFloat(min);
        buffer.writeFloat(max);
        buffer.writeFloat(count);
        buffer.writeInt(expressionX.length);
        for (float datum : expressionX) {
            buffer.writeFloat(datum);
        }
        if (expressionY == null) buffer.writeInt(0);
        else {
            buffer.writeInt(expressionY.length);
            for (float datum : expressionY) {
                buffer.writeFloat(datum);
            }
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int imageId = buffer.readInt();
        int flags = buffer.readInt();
        float min = buffer.readFloat();
        float max = buffer.readFloat();
        float count = buffer.readFloat();
        int len = buffer.readInt();
        if (len > MAX_EXPRESSION_LENGTH) {
            throw new RuntimeException("Path too long");
        }
        float[] expressionX = new float[len];
        for (int i = 0; i < len; i++) {
            expressionX[i] = buffer.readFloat();
        }

        len = buffer.readInt();
        if (len > MAX_EXPRESSION_LENGTH) {
            throw new RuntimeException("Path too long");
        }
        float[] expressionY = new float[len];
        for (int i = 0; i < len; i++) {
            expressionY[i] = buffer.readFloat();
        }
        operations.add(
                new PathExpression(imageId, expressionX, expressionY, min, max, count, flags));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("Encode a Path ")
                .field(DocumentedOperation.INT, "id", "id string")
                .field(INT, "length", "id string")
                .field(FLOAT_ARRAY, "pathData", "length", "path encoded as floats");
    }

    @Override
    public void apply(@NonNull RemoteContext context) {

        if (mPathChanged) {
            boolean loop = (mFlags & 0x1) == LOOP;
            int len = mPathGenerator.getReturnLength((int) mOutCount, loop);
            if (mOutputPath.length != len) {
                mOutputPath = new float[len];
            }
            if ((mFlags & POLAR) == POLAR) {
                mPathGenerator.getPolarPath(
                        mOutputPath,
                        mOutExpressionX,
                        mOutExpressionY,
                        mOutMin,
                        mOutMax,
                        (int) mOutCount,
                        (mFlags & 0x6),
                        loop,
                        Objects.requireNonNull(context.getCollectionsAccess()));
            } else {
                mPathGenerator.getPath(
                        mOutputPath,
                        mOutExpressionX,
                        mOutExpressionY,
                        mOutMin,
                        mOutMax,
                        (int) mOutCount,
                        (mFlags & 0x6),
                        loop,
                        context.getCollectionsAccess());
            }
            context.loadPathData(mInstanceId, mWinding,  mOutputPath);
        }
        mPathChanged = false;
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addType(CLASS_NAME)
                .add("id", mInstanceId)
                .add("flags", mFlags)
                .add("count", mCount)
                .add("min", mMin)
                .add("max", mMax)
                .addPath("expressionX", mExpressionX)
                .addPath("expressionY", mExpressionY);
    }
}
