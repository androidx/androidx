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
import androidx.compose.remote.core.PaintContext;
import androidx.compose.remote.core.PaintOperation;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.VariableSupport;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.documentation.DocumentationBuilder;
import androidx.compose.remote.core.documentation.DocumentedOperation;
import androidx.compose.remote.core.operations.layout.Container;
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.NanMap;
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
public class ParticlesCompare extends PaintOperation implements VariableSupport, Container {
    private static final int OP_CODE = Operations.PARTICLE_COMPARE;
    private static final String CLASS_NAME = "ParticlesCompare";
    private final int mId;
    private final float[] mExpression;
    private final float[] mOutExpression;

    private final float[][] mEquations1;
    private final float[][] mOutEquations1;
    private final float[][] mEquations2;
    private final float[][] mOutEquations2;
    private final short mFlags;
    private final float mMin;
    private final float mMax;
    private float mOutMin;
    private float mOutMax;
    private int[] mVarId;
    private float[][] mParticles;
    private static final int MAX_FLOAT_ARRAY = 2000;
    private static final int MAX_EQU_LENGTH = 46;
    ParticlesCreate mParticlesSource;

    @NonNull
    @Override
    public ArrayList<Operation> getList() {
        return mList;
    }

    @NonNull
    private final ArrayList<Operation> mList = new ArrayList<>();

    @NonNull
    AnimatedFloatExpression mExp = new AnimatedFloatExpression();

    /**
     * Create a new ParticlesLoop operation
     *
     * @param id         of the createParticles
     * @param flags      configuration flags
     * @param min        the min index to process
     * @param max        the max index to process
     * @param expression the first expression
     * @param result1    the first result
     * @param result2    the second result
     */
    public ParticlesCompare(int id,
                            short flags,
                            float min,
                            float max,
                            float @Nullable [] expression,
                            float @Nullable [][] result1,
                            float @Nullable [][] result2
    ) {
        mId = id;
        mFlags = flags;
        mOutMin = mMin = min;
        mOutMax = mMax = max;

        mExpression = expression;
        mOutExpression = copy(expression);


        mEquations1 = result1;
        mOutEquations1 = copy(result1);

        mEquations2 = result2;
        mOutEquations2 = copy(result2);

    }

    private float[] copy(float[] expression) {
        if (expression != null) {
            float[] tmp = new float[expression.length];
            System.arraycopy(expression, 0, tmp, 0, expression.length);
            return tmp;
        }
        return null;
    }

    private float[][] copy(float[][] result) {
        if (result == null) {
            return null;
        }
        float[][] tmp = new float[result.length][];
        for (int i = 0; i < result.length; i++) {
            tmp[i] = new float[result[i].length];
            System.arraycopy(result[i], 0, tmp[i], 0, result[i].length);
        }
        return tmp;
    }


    private void update(RemoteContext context, float[] expression, float[] outExpression) {
        if (outExpression != null) {
            for (int i = 0; i < expression.length; i++) {
                float v = expression[i];
                outExpression[i] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;
            }
        }
    }

    private void update(@NonNull RemoteContext context,
                        float[][] equations,
                        float[][] outEquations) {
        if (equations == null) {
            return;
        }
        for (int i = 0; i < equations.length; i++) {
            float[] equation = equations[i];
            for (int j = 0; j < equation.length; j++) {
                float v = equation[j];
                outEquations[i][j] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        mOutMin = Float.isNaN(mMin) ? context.getFloat(Utils.idFromNan(mMin)) : mMin;
        mOutMax = Float.isNaN(mMax) ? context.getFloat(Utils.idFromNan(mMax)) : mMax;

        update(context, mExpression, mOutExpression);

        update(context, mEquations1, mOutEquations1);
        update(context, mEquations2, mOutEquations2);
    }

    private void register(RemoteContext context, float[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            float v = values[i];
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
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
        register(context, mExpression);
        if (mEquations1 != null) {
            for (int i = 0; i < mEquations1.length; i++) {
                register(context, mEquations1[i]);
            }
        }
        if (mEquations2 != null) {
            for (int i = 0; i < mEquations2.length; i++) {
                register(context, mEquations2[i]);
            }
        }
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer,
                mId,
                mFlags,
                mMin,
                mMax,
                mExpression,
                mEquations1,
                mEquations2);
    }

    @NonNull
    @Override
    public String toString() {
        String str = "ParticlesLoop[" + Utils.idString(mId) + "] ";
        return str;
    }

    /**
     * Write the operation on the buffer
     *
     * @param buffer     the buffer to write to
     * @param id         the id of the particle system
     * @param flags      configuration flags
     * @param min        the min index to process
     * @param max        the max index to process
     * @param compare    the first compare
     * @param equations1 the first equations
     * @param equations2 the second equations
     */

    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            short flags,
            float min,
            float max,
            float @Nullable [] compare,
            float @Nullable [][] equations1,
            float @Nullable [][] equations2
    ) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);
        buffer.writeShort(flags);
        buffer.writeFloat(min);
        buffer.writeFloat(max);
        writeFloats(buffer, compare);


        if (equations1 == null) {
            buffer.writeInt(0);
        } else {
            if (MAX_EQU_LENGTH < equations1.length) {
                throw new RuntimeException(equations1.length
                        + " map entries more than max = " + MAX_EQU_LENGTH);
            }
            buffer.writeInt(equations1.length);
            for (int i = 0; i < equations1.length; i++) {
                writeFloats(buffer, equations1[i]);
            }
        }
        if (equations2 == null) {
            buffer.writeInt(0);
        } else {
            buffer.writeInt(equations2.length);
            for (int i = 0; i < equations2.length; i++) {
                writeFloats(buffer, equations2[i]);
            }
        }
    }

    private static void writeFloats(@NonNull WireBuffer buffer, float @Nullable [] values) {
        if (values == null) {
            buffer.writeInt(0);
            return;
        }
        if (MAX_EQU_LENGTH < values.length) {
            throw new RuntimeException(values.length
                    + " map entries more than max = " + MAX_EQU_LENGTH);
        }
        buffer.writeInt(values.length);
        for (int i = 0; i < values.length; i++) {
            buffer.writeFloat(values[i]);
        }
    }

    /**
     * Read this operation and add it to the list of operations
     *
     * @param buffer     the buffer to read
     * @param operations the list of operations that will be added to
     */
    public static void read(@NonNull WireBuffer buffer, @NonNull List<Operation> operations) {
        int id = buffer.readInt();
        short flags = (short) buffer.readShort();
        float min = buffer.readFloat();
        float max = buffer.readFloat();
        float[] exp = readFloats(buffer);

        int result1Len = buffer.readInt();
        if (result1Len > MAX_FLOAT_ARRAY) {
            throw new RuntimeException(result1Len
                    + " map entries more than max = " + MAX_FLOAT_ARRAY);
        }

        float[][] equations1 = (result1Len == 0) ? null : new float[result1Len][];
        for (int i = 0; i < result1Len; i++) {
            equations1[i] = readFloats(buffer);
        }

        int result2Len = buffer.readInt();

        float[][] equations2 = (result2Len == 0) ? null : new float[result2Len][];
        for (int i = 0; i < result2Len; i++) {
            equations2[i] = readFloats(buffer);
        }
        ParticlesCompare data =
                new ParticlesCompare(id, flags, min, max, exp, equations1, equations2);
        operations.add(data);
    }

    static float[] readFloats(@NonNull WireBuffer buffer) {
        int len = buffer.readInt();
        if (len > MAX_EQU_LENGTH) {
            throw new RuntimeException(
                    len + " map entries more than max = " + MAX_EQU_LENGTH);
        }
        if (len == 0) {
            return null;
        }
        float[] ret = new float[len];
        for (int i = 0; i < len; i++) {
            ret[i] = buffer.readFloat();
        }
        return ret;
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Data Operations", OP_CODE, CLASS_NAME)
                .description("This evolves the particles & recycles them")
                .field(DocumentedOperation.INT, "id", "id of particle system")
                .field(
                        INT,
                        "recycleLen",
                        "the number of floats in restart equeation if 0 no restart")
                .field(FLOAT_ARRAY, "values", "recycleLen", "array of floats")
                .field(INT, "varLen", "the number of equations to follow")
                .field(INT, "equLen", "the number of equations to follow")
                .field(FLOAT_ARRAY, "values", "equLen", "floats for the equation");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    private void update2Body(RemoteContext context,
                             float[] expression,
                             float[] outExpression,
                             float[] particle1,
                             float[] particle2
    ) {
        if (outExpression != null) {

            for (int i = 0; i < expression.length; i++) {
                float v = expression[i]; // normal lookup first
                outExpression[i] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;

                if (i > expression.length - 1) {
                    break;
                }
                // special overwrite for particles with CMD1 & CMD2
                for (int k = 0; k < mVarId.length; k++) {
                    int id = mVarId[k];
                    if (Float.isNaN(v) && Utils.idFromNan(v) == id) {

                        if (Float.floatToRawIntBits(expression[i + 1])
                                == Float.floatToRawIntBits(AnimatedFloatExpression.CMD1)) {
                            outExpression[i] = particle1[k];
                            outExpression[i + 1] = AnimatedFloatExpression.NOP;
                            i++;

                        } else if (Float.floatToRawIntBits(expression[i + 1])
                                == Float.floatToRawIntBits(AnimatedFloatExpression.CMD2)) {
                            outExpression[i] = particle2[k];

                            outExpression[i + 1] = AnimatedFloatExpression.NOP;
                            i++;
                        }

                    }
                }
            }

        }
    }


    private void update2Body(@NonNull RemoteContext context,
                             float[][] equations,
                             float[][] outEquations,
                             float[] particle1,
                             float[] particle2,
                             boolean reverse) {
        if (equations == null) {
            return;
        }
        for (int i = 0; i < equations.length; i++) {
            float[] equation = equations[i];
            for (int j = 0; j < equation.length; j++) {
                float v = equation[j];
                outEquations[i][j] =
                        (Float.isNaN(v)
                                && !AnimatedFloatExpression.isMathOperator(v)
                                && !NanMap.isDataVariable(v))
                                ? context.getFloat(Utils.idFromNan(v))
                                : v;
                if (j == equation.length - 1) {
                    break;
                }
                if (Float.isNaN(v)) {
                    // special overwrite for particles with CMD1 & CMD2
                    for (int k = 0; k < mVarId.length; k++) {
                        int id = mVarId[k];
                        if (Utils.idFromNan(v) == id) {
                            if (Float.floatToRawIntBits(equation[j + 1])
                                    == Float.floatToRawIntBits(AnimatedFloatExpression.CMD1)) {
                                outEquations[i][j] = particle1[k];
                                outEquations[i][j + 1] = AnimatedFloatExpression.NOP;
                                j++;
                            } else if (Float.floatToRawIntBits(equation[j + 1])
                                    == Float.floatToRawIntBits(AnimatedFloatExpression.CMD2)) {
                                outEquations[i][j] = particle2[k];
                                outEquations[i][j + 1] = AnimatedFloatExpression.NOP;
                                j++;
                            } else {
                                outEquations[i][j] = reverse ? particle2[k] : particle1[k];
                            }
                        }
                    }
                }
            }
        }
    }

    private void condition2Body(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        CollectionsAccess ca = Objects.requireNonNull(remoteContext.getCollectionsAccess());
        boolean needsRepaint = false;
        int start = (mOutMin < 0) ? 0 : (int) mOutMin;
        int end = (mOutMax < 0) ? mParticles.length : (int) mOutMax;

        for (int k = start; k < end; k++) {
            float[] particle2 = mParticles[k];
            for (int i = k + 1; i < end; i++) {
                float[] particle1 = mParticles[i];
                setupForParticle(remoteContext, particle1);
                update2Body(remoteContext, mExpression, mOutExpression, particle1, particle2);

                float value = (mOutExpression == null) ? 0f
                        : mExp.eval(ca, mOutExpression, mOutExpression.length);
                remoteContext.incrementOpCount();
                if (0 < value) {

                    update2Body(remoteContext,
                            mEquations1,
                            mOutEquations1,
                            particle1,
                            particle2,
                            false);
                    setupForParticle(remoteContext, particle2);

                    update2Body(remoteContext,
                            mEquations2,
                            mOutEquations2,
                            particle1,
                            particle2,
                            true);

                    // Evaluate the update function

                    for (int j = 0; j < particle1.length; j++) {
                        float tmp = mExp.eval(ca, mOutEquations1[j], mOutEquations1[j].length);
                        particle1[j] = tmp;
                        remoteContext.loadFloat(mVarId[j], particle1[j]);
                    }

                    runChildren(context, remoteContext);

                    for (int j = 0; j < particle2.length; j++) {
                        float tmp = mExp.eval(ca, mOutEquations2[j], mOutEquations2[j].length);
                        particle2[j] = tmp;
                        remoteContext.loadFloat(mVarId[j], particle2[j]);
                    }
                    runChildren(context, remoteContext);

                    needsRepaint = true;
                }
            }
        }
        if (needsRepaint) {
            context.needsRepaint();
        }
    }

    private void condition1Body(@NonNull PaintContext context) {
        RemoteContext remoteContext = context.getContext();
        CollectionsAccess ca = Objects.requireNonNull(remoteContext.getCollectionsAccess());
        boolean needsRepaint = false;
        int start = (mOutMin < 0) ? 0 : (int) mOutMin;
        int end = (mOutMax < 0) ? mParticles.length : (int) mOutMax;

        for (int i = start; i < end; i++) {
            float[] particle = mParticles[i];
            setupForParticle(remoteContext, particle);
            update(remoteContext, mExpression, mOutExpression);

            float value1 = mExp.eval(ca, mOutExpression, mOutExpression.length);

            if (0 < value1) {
                update(remoteContext, mEquations1, mOutEquations1);
                // Evaluate the update function
                for (int j = 0; j < particle.length; j++) {
                    float tmp = mExp.eval(ca, mOutEquations1[j], mOutEquations1[j].length);
                    particle[j] = tmp;
                    remoteContext.loadFloat(mVarId[j], particle[j]);
                }
                runChildren(context, remoteContext);

                needsRepaint = true;
                remoteContext.incrementOpCount();
            }
        }
        if (needsRepaint) {
            context.needsRepaint();
        }
    }

    @Override
    public void paint(@NonNull PaintContext context) {
        if (mEquations1 != null && mEquations2 != null) {
            condition2Body(context);
            return;
        }
        condition1Body(context);
        if (true) {
            return;
        }

        RemoteContext remoteContext = context.getContext();
        CollectionsAccess ca = Objects.requireNonNull(remoteContext.getCollectionsAccess());
        boolean needsRepaint = false;
        int start = (mOutMin < 0) ? 0 : (int) mOutMin;
        int end = (mOutMax < 0) ? mParticles.length : (int) mOutMax;

        for (int i = start; i < end; i++) {
            float[] particle = mParticles[i];
            setupForParticle(remoteContext, particle);
            update(remoteContext, mExpression, mOutExpression);

            float value1 = 0f;

            float value2 = mExp.eval(
                    ca, mOutExpression, mOutExpression.length);

            if (value1 < value2) {
                update(remoteContext, mEquations1, mOutEquations1);
                update(remoteContext, mEquations2, mOutEquations2);

                // updates the vielues in particle and calls children
                for (int j = 0; j < particle.length; j++) {
                    float tmp = mExp.eval(ca, mOutEquations1[j], mOutEquations1[j].length);
                    particle[j] = tmp;
                    remoteContext.loadFloat(mVarId[j], particle[j]);
                }

                runChildren(context, remoteContext);
                needsRepaint = true;
                remoteContext.incrementOpCount();
            }
        }
        if (needsRepaint) {
            context.needsRepaint();
        }
    }


    /**
     * Setup with the i particle values
     * It writes mOutExpression1/2 & mOutEquations1/1
     * with values that represent particle i
     *
     * @param remoteContext the remote context
     * @param particle      the particle index
     */
    private void setupForParticle(RemoteContext remoteContext, float[] particle) {
        for (int j = 0; j < particle.length; j++) {
            remoteContext.loadFloat(mVarId[j], particle[j]);
        }
    }

    /**
     * Run the children of this operation.
     * But first update the to the current particles values
     */
    private void runChildren(@NonNull PaintContext context, RemoteContext remoteContext) {
        for (Operation op : mList) {
            if (op instanceof VariableSupport) {
                ((VariableSupport) op).updateVariables(context.getContext());
            }

            remoteContext.incrementOpCount();
            op.apply(context.getContext());
        }
    }


    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer.addType(CLASS_NAME).add("id", mId);
    }
}

