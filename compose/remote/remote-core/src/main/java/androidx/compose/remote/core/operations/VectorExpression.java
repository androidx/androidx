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
package androidx.compose.remote.core.operations;

import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.REPEATED_FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.operations.utilities.VectorRpn;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A vector-valued expression. It evaluates an RPN program (see {@link VectorRpn}) producing a
 * vector
 * of {@code dimensionality} components and writes those components to <b>consecutive</b> scalar
 * variable ids {@code id, id+1, …, id+dimensionality-1}. Every existing consumer (draw ops,
 * transforms, color/scalar expressions) reads those component ids as ordinary scalars — so this
 * op is
 * a compact <i>producer</i> and nothing downstream needs to know vectors exist.
 *
 * <p>Mirrors {@link FloatExpression} in spirit but is a <b>simplified</b> version: it deliberately
 * carries no animation/spring/easing — animation is compositional (feed it animated input
 * variables).
 * Like {@code FloatExpression}, {@link #updateVariables} resolves the variable references in the
 * program (except arrays) to current values; {@link #apply} then runs the pure {@link VectorRpn}
 * kernel and scatters the result. See {@code VECTOR_EXPRESSION_PLAN.md}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VectorExpression extends Operation implements VariableSupport {
    private static final int OP_CODE = Operations.VECTOR_EXPRESSION;
    private static final String CLASS_NAME = "VectorExpression";

    private final int mId;
    private final int mDimension;
    private final int mFlags;
    private final float @NonNull [] mSrcValue;
    private final float @NonNull [] mPreCalc;

    @NonNull
    private final VectorRpn mRpn = new VectorRpn();
    private final float[] mOut = new float[VectorRpn.MAX_DIM];

    public VectorExpression(int id, int dimension, int flags, float @NonNull [] value) {
        mId = id;
        mDimension = dimension;
        mFlags = flags;
        mSrcValue = value;
        mPreCalc = new float[value.length];
        System.arraycopy(value, 0, mPreCalc, 0, value.length);
    }

    /** Base output variable id (components land in {@code id .. id + dimension - 1}). */
    public int getId() {
        return mId;
    }

    /** Number of components this expression produces (vec2/3/4). Used for scalar-vs-vector
     * typing. */
    public int getDimension() {
        return mDimension;
    }

    /** Option/reserved flag bits. */
    public int getFlags() {
        return mFlags;
    }

    /** The raw RPN program array. */
    public float @NonNull [] getSrcValue() {
        return mSrcValue;
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        for (int i = 0; i < mSrcValue.length; i++) {
            float v = mSrcValue[i];
            // Resolve variable references to their current values; leave operators and array
            // references (resolved at eval time) untouched.
            mPreCalc[i] =
                    (Float.isNaN(v)
                            && !AnimatedFloatExpression.isMathOperator(v)
                            && !NanMap.isDataVariable(v))
                            ? context.getFloat(Utils.idFromNan(v))
                            : v;
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        // Register as the object owning these ids so other ops can discover this is a vector
        // (and its
        // dimensionality via getDimension()). See VECTOR_EXPRESSION_PLAN.md open question on
        // typing.
        context.putObject(mId, this);
        for (float v : mSrcValue) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    @Override
    public void apply(@NonNull RemoteContext context) {
        int lanes = mRpn.apply(mPreCalc, mPreCalc.length, mOut);
        if (!allFinite(lanes)) {
            // A divide/normalize hit (near-)zero: retry with the soft-domain denominator.
            mRpn.mSoftDomain = true;
            try {
                lanes = mRpn.apply(mPreCalc, mPreCalc.length, mOut);
            } finally {
                mRpn.mSoftDomain = false;
            }
        }
        for (int k = 0; k < mDimension; k++) {
            float c = (k < lanes) ? mOut[k] : 0f;
            if (Float.isNaN(c) || Float.isInfinite(c)) {
                c = 0f; // never store a non-finite value (it would corrupt downstream reads)
            }
            context.loadFloat(mId + k, c);
        }
    }

    private boolean allFinite(int lanes) {
        for (int k = 0; k < lanes; k++) {
            float c = mOut[k];
            if (Float.isNaN(c) || Float.isInfinite(c)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mDimension, mFlags, mSrcValue);
    }

    /**
     * Serialize the operation.
     *
     * @param buffer    the buffer to write to
     * @param id        base output variable id (components land in id … id+dimensionality-1)
     * @param dimension number of components
     * @param flags     option/reserved bits
     * @param value     the vector RPN program
     */
    public static void apply(
            @NonNull WireBuffer buffer, int id, int dimension, int flags, float @NonNull [] value) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeByte(dimension);
        buffer.writeByte(flags);
        buffer.writeShort(value.length);
        for (float v : value) {
            buffer.writeFloat(v);
        }
    }

    /**
     * Read this operation and add it to the list of operations.
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.declareId();
        int dimension = buffer.readByte();
        int flags = buffer.readByte();
        int len = buffer.readShort();
        float[] values = new float[len];
        for (int i = 0; i < len; i++) {
            values[i] = buffer.readNanId();
        }
        operations.add(new VectorExpression(id, dimension, flags, values));
    }

    /**
     * Populate the documentation with a description of this operation.
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Logic & Expressions Operations", OP_CODE, CLASS_NAME)
                .description(
                        "A vec2/3/4-valued expression; scatters its components to id..id+dim-1")
                .field(INT, "id", "Base output variable id")
                .field(INT, "dimensionality", "Number of components (1 byte)")
                .field(INT, "flags", "Option/reserved bits (1 byte)")
                .field(SHORT, "length", "Number of floats in the program")
                .field(REPEATED_FLOAT, "operations", "The vector RPN program");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + this;
    }

    @NonNull
    @Override
    public String toString() {
        return "VectorExpression[" + mId + "] dim=" + mDimension;
    }
}
