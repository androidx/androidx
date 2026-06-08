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
import androidx.compose.remote.core.Limits;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.loom.LoomWireBuffer;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.NanMap;
import androidx.compose.remote.core.operations.utilities.VectorOpCodes;
import androidx.compose.remote.core.operations.utilities.VectorRpn;
import androidx.compose.remote.core.serialize.MapSerializer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This provides the mechanism to evolve the particles It consist of a restart equation and a list
 * of equations particle restarts if restart equation > 0
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ParticlesLoop extends PaintOperation implements VariableSupport, Container {
    private static final int OP_CODE = Operations.PARTICLE_LOOP;
    private static final String CLASS_NAME = "ParticlesLoop";
    private int mId;
    private final float[] mRestart;
    private final float[] mOutRestart;
    private final float[][] mEquations;
    private final float[][] mOutEquations;
    private int[] mVarId;
    private float[][] mParticles;
    ParticlesCreate mParticlesSource;

    /** Dimensionality per equation: 1 = scalar (today), >1 = a vector equation spanning d slots. */
    private final int[] mEquDim;

    @NonNull
    private final VectorRpn mVecRpn = new VectorRpn();
    private final float[] mVecOut = new float[VectorRpn.MAX_DIM];

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @NonNull
    private final ArrayList<Operation> mList = new ArrayList<>();

    @NonNull AnimatedFloatExpression mExp = new AnimatedFloatExpression();

    /**
     * Create a new ParticlesLoop operation
     *
     * @param id      of the create
     * @param restart the restart equation kills and restart when positive
     * @param values  the loop equations
     */
    public ParticlesLoop(int id, float @Nullable [] restart, float @NonNull [][] values) {
        this(id, restart, values, null);
    }

    /**
     * @param dims dimensionality per equation (1 = scalar). null ⇒ all scalar (legacy). A vector
     *             equation of dim d produces d values written to d consecutive particle slots.
     */
    public ParticlesLoop(
            int id, float @Nullable [] restart, float @NonNull [][] values, int @Nullable [] dims) {
        mId = id;
        mRestart = restart;
        if (restart != null) {
            mOutRestart = new float[restart.length];
            System.arraycopy(restart, 0, mOutRestart, 0, restart.length);
        } else {
            mOutRestart = null;
        }

        mEquations = values;
        mOutEquations = new float[values.length][];
        for (int i = 0; i < values.length; i++) {
            mOutEquations[i] = new float[values[i].length];
            System.arraycopy(values[i], 0, mOutEquations[i], 0, values[i].length);
        }
        mEquDim = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            mEquDim[i] = (dims != null && dims[i] > 1) ? dims[i] : 1;
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mOutRestart != null) {
            for (int i = 0; i < mRestart.length; i++) {
                float v = mRestart[i];
                mOutRestart[i] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;
            }
        }
        for (int i = 0; i < mEquations.length; i++) {
            float[] mEquation = mEquations[i];
            for (int j = 0; j < mEquation.length; j++) {
                float v = mEquation[j];
                mOutEquations[i][j] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !VectorOpCodes.isVectorOp(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;
            }
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        mParticlesSource = (ParticlesCreate) context.getObject(mId);
        if (mParticlesSource != null) {
            mParticles = mParticlesSource.getParticles();
            mVarId = mParticlesSource.getVariableIds();
        }
        if (mRestart != null) {
            for (int i = 0; i < mRestart.length; i++) {
                float v = mRestart[i];
                if (Float.isNaN(v)
                        && !AnimatedFloatExpression.isMathOperator(v)
                        && !NanMap.isDataVariable(v)) {
                    context.listensTo(Utils.idFromNan(v), this);
                }
            }
        }
        for (int i = 0; i < mEquations.length; i++) {
            float[] mEquation = mEquations[i];
            for (float v : mEquation) {
                if (Float.isNaN(v)
                        && !AnimatedFloatExpression.isMathOperator(v)
                        && !VectorOpCodes.isVectorOp(v)
                        && !NanMap.isDataVariable(v)) {
                    context.listensTo(Utils.idFromNan(v), this);
                }
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mRestart, mEquations, mEquDim);
    }

    /** The raw per-variable update equations (RPN). For tooling/inspection. */
    public float @NonNull [][] getEquations() {
        return mEquations;
    }

    /** The raw restart equation (RPN), or null. For tooling/inspection. */
    public float @Nullable [] getRestart() {
        return mRestart;
    }

    @NonNull
    @Override
    public String toString() {
        return "ParticlesLoop[" + Utils.idString(mId) + "] ";
    }

    /**
     * Write the operation on the buffer
     *
     * @param buffer    the buffer to write to
     * @param id        the id of the particle system
     * @param restart   the restart equation
     * @param equations the equations to evolve the particles
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float @Nullable [] restart,
            float @NonNull [][] equations) {
        apply(buffer, id, restart, equations, null);
    }

    /**
     * Write the operation on the buffer.
     *
     * <p>Each equation's length is written as an int whose <b>high short</b> is a 0=scalar/1=vector
     * flag and <b>low short</b> is the length. A vector equation (dim &gt; 1) additionally writes a
     * {@code byte dimensionality} and {@code byte flags} before its floats. Since the length is
     * always
     * ≤ 32, the high short is 0 for every legacy document, which decodes identically.
     *
     * @param dims dimensionality per equation (1 = scalar); null ⇒ all scalar
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float @Nullable [] restart,
            float @NonNull [][] equations,
            int @Nullable [] dims) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        if (restart != null) {
            buffer.writeInt(restart.length);
            for (int i = 0; i < restart.length; i++) {
                buffer.writeFloat(restart[i]);
            }
        } else {
            buffer.writeInt(0);
        }
        buffer.writeInt(equations.length);
        for (int i = 0; i < equations.length; i++) {
            int len = equations[i].length;
            int dim = (dims != null && dims[i] > 1) ? dims[i] : 1;
            if (dim > 1) {
                buffer.writeInt((1 << 16) | len); // high short = vector flag
                buffer.writeByte(dim);
                buffer.writeByte(0); // flags (reserved)
            } else {
                buffer.writeInt(len);
            }
            for (int j = 0; j < len; j++) {
                buffer.writeFloat(equations[i][j]);
            }
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to mapping context for remapping
     *                   IDs
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readId();
        int restartLen = buffer.readInt();
        float[] restart = null;
        if (restartLen > 0) {
            if (restartLen > Limits.MAX_EXPRESSION_SIZE) {
                throw new RuntimeException(
                        restartLen + " map entries more than max = " + Limits.MAX_EXPRESSION_SIZE);
            }
            restart = new float[restartLen];
            for (int i = 0; i < restartLen; i++) {
                float v = buffer.readFloat();
                if (Float.isNaN(v)
                        && !AnimatedFloatExpression.isMathOperator(v)
                        && !NanMap.isDataVariable(v)) {
                    // Manual remapping since we already read it
                    if (buffer instanceof LoomWireBuffer) {
                        restart[i] = ((LoomWireBuffer) buffer).getRemapContext().resolveNanId(v);
                    } else {
                        restart[i] = v;
                    }
                } else {
                    restart[i] = v;
                }
            }
        }

        int varLen = buffer.readInt();
        if (varLen > Limits.MAX_PARTICLE_FLOAT_ARRAY_SIZE) {
            throw new RuntimeException(
                    varLen
                            + " map entries more than max = "
                            + Limits.MAX_PARTICLE_FLOAT_ARRAY_SIZE);
        }

        float[][] equations = new float[varLen][];
        int[] dims = new int[varLen];
        for (int i = 0; i < varLen; i++) {

            int equLen32 = buffer.readInt();
            int equLen = equLen32 & 0xFFFF;
            int vectorFlag = (equLen32 >> 16) & 0xFFFF;
            int dim = 1;
            if (vectorFlag != 0) {
                dim = buffer.readByte();
                buffer.readByte(); // flags (reserved)
            }
            dims[i] = dim;
            if (equLen > Limits.MAX_EXPRESSION_SIZE) {
                throw new RuntimeException(
                        equLen + " map entries more than max = " + Limits.MAX_EXPRESSION_SIZE);
            }
            equations[i] = new float[equLen];
            for (int j = 0; j < equations[i].length; j++) {
                float v = buffer.readFloat();
                if (Float.isNaN(v)
                        && !AnimatedFloatExpression.isMathOperator(v)
                        && !VectorOpCodes.isVectorOp(v)
                        && !NanMap.isDataVariable(v)) {
                    // Manual remapping since we already read it
                    if (buffer instanceof LoomWireBuffer) {
                        equations[i][j] =
                                ((LoomWireBuffer) buffer).getRemapContext().resolveNanId(v);
                    } else {
                        equations[i][j] = v;
                    }
                } else {
                    equations[i][j] = v;
                }
            }
        }
        ParticlesLoop data = new ParticlesLoop(id, restart, equations, dims);
        operations.add(data);
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Animation & Particles Operations", OP_CODE, CLASS_NAME)
                .description("Update and recycle particles in a system")
                .field(DocumentedOperation.INT, "id", "The ID of the particle system")
                .field(
                        INT,
                        "restartLen",
                        "The length of the restart equation (recycles particle if > 0)")
                .field(FLOAT_ARRAY, "restartEquation", "The restart equation (RPN)")
                .field(INT, "varCount", "The number of update equations")
                .field(INT, "equLen[0..n]", "The length of each update equation")
                .field(FLOAT_ARRAY, "equations[0..n]", "The update equations (RPN)");
    }

    private static boolean allFinite(float[] a, int n) {
        for (int k = 0; k < n; k++) {
            if (Float.isNaN(a[k]) || Float.isInfinite(a[k])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        CollectionsAccess ca = Objects.requireNonNull(remoteContext.getCollectionsAccess());

        for (int i = 0; i < mParticles.length; i++) {
            // Save the values to context TODO hand code the update
            for (int j = 0; j < mParticles[i].length; j++) {
                remoteContext.loadFloat(mVarId[j], mParticles[i][j]);
                updateVariables(remoteContext);
            }
            // Evaluate the update functions. A scalar equation fills one slot; a vector equation
            // (dim d) evaluates one VectorRpn program and fills d consecutive slots.
            int slot = 0;
            for (int g = 0; g < mEquations.length; g++) {
                int d = mEquDim[g];
                if (d == 1) {
                    float r = mExp.eval(ca, mOutEquations[g], mOutEquations[g].length);
                    if (!ParticlesCreate.isFiniteValue(r)) {
                        // Optimistic exact eval produced NaN/Inf (e.g. a tracer hit a singularity):
                        // retry with divide/modulo softened, then sanitize — a non-finite value, if
                        // stored, would be re-read as an opcode next frame and crash the evaluator.
                        r = mExp.softDomEval(ca, mOutEquations[g], mOutEquations[g].length);
                        if (!ParticlesCreate.isFiniteValue(r)) {
                            r =
                                    ParticlesCreate.onNonFiniteParticleValue(
                                            "ParticlesLoop[" + Utils.idString(mId) + "]",
                                            g,
                                            i,
                                            mOutEquations[g]);
                        }
                    }
                    mParticles[i][slot] = r;
                    remoteContext.loadFloat(mVarId[slot], r);
                    slot++;
                } else {
                    int lanes = mVecRpn.apply(mOutEquations[g], mOutEquations[g].length, mVecOut);
                    if (!allFinite(mVecOut, lanes)) {
                        mVecRpn.mSoftDomain = true;
                        try {
                            lanes = mVecRpn.apply(mOutEquations[g], mOutEquations[g].length,
                                    mVecOut);
                        } finally {
                            mVecRpn.mSoftDomain = false;
                        }
                    }
                    for (int k = 0; k < d; k++) {
                        float val = (k < lanes) ? mVecOut[k] : 0f;
                        if (Float.isNaN(val) || Float.isInfinite(val)) {
                            val = 0f;
                        }
                        mParticles[i][slot + k] = val;
                        remoteContext.loadFloat(mVarId[slot + k], val);
                    }
                    slot += d;
                }
            }
            // test for reset
            if (mOutRestart != null) {
                for (int k = 0; k < mRestart.length; k++) {
                    float v = mRestart[k];
                    mOutRestart[k] =
                            (Float.isNaN(v)
                                    && !AnimatedFloatExpression.isMathOperator(v)
                                    && !NanMap.isDataVariable(v))
                                    ? remoteContext.getFloat(Utils.idFromNan(v))
                                    : v;
                }
                if (mExp.eval(ca, mOutRestart, mOutRestart.length) > 0) {
                    mParticlesSource.initializeParticle(i);
                }
            }

            for (Operation op : mList) {
                if (op instanceof VariableSupport) {
                    ((VariableSupport) op).updateVariables(context.getContext());
                }

                remoteContext.incrementOpCount();
                op.apply(context.getContext());
            }
        }
        context.needsRepaint();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId);
    }

    /** Returns {@code mParticlesSource}. */
    public @Nullable ParticlesCreate getParticlesSource() {
        return mParticlesSource;
    }
}
