/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.FLOAT_ARRAY;
import static androidx.compose.remote.core.documentation.DocumentedOperation.INT;
import static androidx.compose.remote.core.documentation.DocumentedOperation.SHORT;

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
import androidx.compose.remote.core.operations.utilities.easing.FloatAnimation;
import androidx.compose.remote.core.operations.utilities.easing.SpringStopEngine;
import androidx.compose.remote.core.serialize.MapSerializer;
import androidx.compose.remote.core.serialize.Serializable;
import androidx.compose.remote.core.serialize.SerializeTags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Operation to deal with AnimatedFloats This is designed to be an optimized calculation for things
 * like injecting the width of the component int draw rect As well as supporting generalized
 * animation floats. The floats represent a RPN style calculator
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FloatExpression extends Operation
        implements ComponentData, VariableSupport, Serializable {
    private static final int OP_CODE = Operations.ANIMATED_FLOAT;
    private static final String CLASS_NAME = "FloatExpression";
    public int mId;
    public float @NonNull [] mSrcValue;
    public float @Nullable [] mSrcAnimation;
    @Nullable public FloatAnimation mFloatAnimation;
    @Nullable private SpringStopEngine mSpring;
    public float @Nullable [] mPreCalcValue;
    private float mLastChange = Float.NaN;
    private float mLastCalculatedValue = Float.NaN;
    @NonNull AnimatedFloatExpression mExp = new AnimatedFloatExpression();
    public static final int MAX_EXPRESSION_SIZE = 32;

    public FloatExpression(int id, float @NonNull [] value, float @Nullable [] animation) {
        this.mId = id;
        this.mSrcValue = value;
        this.mSrcAnimation = animation;
        if (mSrcAnimation != null) {
            if (mSrcAnimation.length > 4 && mSrcAnimation[0] == 0) {
                mSpring = new SpringStopEngine(mSrcAnimation);
            } else {
                mFloatAnimation = new FloatAnimation(mSrcAnimation);
            }
        }
    }

    @Override
    public void updateVariables(@NonNull RemoteContext context) {
        if (mPreCalcValue == null || mPreCalcValue.length != mSrcValue.length) {
            mPreCalcValue = new float[mSrcValue.length];
        }

        boolean value_changed = false;
        for (int i = 0; i < mSrcValue.length; i++) {
            float v = mSrcValue[i];
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                int id = Utils.idFromNan(v);
                float newValue = context.getFloat(Utils.idFromNan(v));

                // TODO: rethink the lifecycle for variable updates
                if (id == RemoteContext.ID_DENSITY && newValue == 0f) {
                    newValue = 1f;
                }
                if (mFloatAnimation != null) {
                    if (mPreCalcValue[i] != newValue) {
                        value_changed = true;
                        mPreCalcValue[i] = newValue;
                    }
                } else if (mSpring != null) {
                    if (mPreCalcValue[i] != newValue) {
                        value_changed = true;
                        mPreCalcValue[i] = newValue;
                    }
                } else {
                    mPreCalcValue[i] = newValue;
                }
            } else {
                mPreCalcValue[i] = mSrcValue[i];
            }
        }
        float v = mLastCalculatedValue;
        if (value_changed) { // inputs changed check if output changed
            v = mExp.eval(mPreCalcValue, mPreCalcValue.length);
            if (v != mLastCalculatedValue) {
                mLastChange = context.getAnimationTime();
                mLastCalculatedValue = v;
            } else {
                value_changed = false;
            }
        }

        if (value_changed && mFloatAnimation != null) {
            if (Float.isNaN(mFloatAnimation.getTargetValue())) {
                mFloatAnimation.setInitialValue(v);
            } else {
                mFloatAnimation.setInitialValue(mFloatAnimation.getTargetValue());
            }
            mFloatAnimation.setTargetValue(v);
        } else if (value_changed && mSpring != null) {
            mSpring.setTargetValue(v);
        }
    }

    @Override
    public void registerListening(@NonNull RemoteContext context) {
        for (float v : mSrcValue) {
            if (Float.isNaN(v)
                    && !AnimatedFloatExpression.isMathOperator(v)
                    && !NanMap.isDataVariable(v)) {
                context.listensTo(Utils.idFromNan(v), this);
            }
        }
    }

    // Keep track of the last computed value when we are animated,
    // e.g. if FloatAnimation or Spring is used, so that we can
    // ask for a repaint.
    float mLastAnimatedValue = Float.NaN;

    @Override
    public void apply(@NonNull RemoteContext context) {
        float t = context.getAnimationTime();
        if (Float.isNaN(mLastChange)) {
            mLastChange = t;
        }
        if (mFloatAnimation != null) { // support animations
            if (Float.isNaN(mLastCalculatedValue)) { // startup
                try {
                    mLastCalculatedValue =
                            mExp.eval(
                                    Objects.requireNonNull(context.getCollectionsAccess()),
                                    mPreCalcValue,
                                    mPreCalcValue.length);
                    mFloatAnimation.setTargetValue(mLastCalculatedValue);
                    if (Float.isNaN(mFloatAnimation.getInitialValue())) {
                        mFloatAnimation.setInitialValue(mLastCalculatedValue);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(
                            this.toString() + " len = " + mPreCalcValue.length, e);
                }
            }
            float lastComputedValue = mFloatAnimation.get(t - mLastChange);

            if (lastComputedValue != mLastAnimatedValue
                    || t - mLastChange <= mFloatAnimation.getDuration()) {
                mLastAnimatedValue = lastComputedValue;
                context.loadFloat(mId, lastComputedValue);
                context.needsRepaint();
                markDirty();
            }
        } else if (mSpring != null) { // support damped spring animation
            float lastComputedValue = mSpring.get(t - mLastChange);
            float epsilon = 0.01f;
            if (lastComputedValue != mLastAnimatedValue
                    || Math.abs(mSpring.getTargetValue() - lastComputedValue) > epsilon) {
                mLastAnimatedValue = lastComputedValue;
                context.loadFloat(mId, lastComputedValue);
                context.needsRepaint();
            }
        } else { // no animation
            float v = 0;
            try {
                v =
                        mExp.eval(
                                Objects.requireNonNull(context.getCollectionsAccess()),
                                mPreCalcValue,
                                mPreCalcValue.length);
            } catch (Exception e) {
                throw new RuntimeException(this.toString() + " len = " + mPreCalcValue.length, e);
            }
            context.loadFloat(mId, v);
        }
    }

    /**
     * Evaluate the expression
     *
     * @param context current context
     * @return the resulting value
     */
    public float evaluate(@NonNull RemoteContext context) {
        updateVariables(context);
        float t = context.getAnimationTime();
        if (Float.isNaN(mLastChange)) {
            mLastChange = t;
        }
        return mExp.eval(
                Objects.requireNonNull(context.getCollectionsAccess()),
                mPreCalcValue,
                mPreCalcValue.length);
    }

    @Override
    public void write(@NonNull WireBuffer buffer) {
        apply(buffer, mId, mSrcValue, mSrcAnimation);
    }

    @NonNull
    @Override
    public String toString() {
        String[] labels = new String[mSrcValue.length];
        for (int i = 0; i < mSrcValue.length; i++) {
            if (Float.isNaN(mSrcValue[i])) {
                labels[i] = "[" + Utils.idStringFromNan(mSrcValue[i]) + "]";
            }
        }
        float[] toDisplay = mPreCalcValue != null ? mPreCalcValue : mSrcValue;
        return "FloatExpression["
                + mId
                + "] = ("
                + AnimatedFloatExpression.toString(toDisplay, labels)
                + ")";
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
     * @param buffer The buffer to write to
     * @param id the id of the resulting float
     * @param value the float expression array
     * @param animation the animation expression array
     */
    public static void apply(
            @NonNull WireBuffer buffer,
            int id,
            float @NonNull [] value,
            float @Nullable [] animation) {
        buffer.start(OP_CODE);
        buffer.writeInt(id);

        int len = value.length;
        if (len > MAX_EXPRESSION_SIZE) {
            throw new RuntimeException(AnimatedFloatExpression.toString(value, null) + " to long");
        }
        if (animation != null) {
            len |= (animation.length << 16);
        }
        buffer.writeInt(len);

        for (float v : value) {
            buffer.writeFloat(v);
        }
        if (animation != null) {
            for (float v : animation) {
                buffer.writeFloat(v);
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
        int id = buffer.readInt();
        int len = buffer.readInt();
        int valueLen = len & 0xFFFF;
        if (valueLen > MAX_EXPRESSION_SIZE) {
            throw new RuntimeException("Float expression too long");
        }
        int animLen = (len >> 16) & 0xFFFF;
        float[] values = new float[valueLen];
        for (int i = 0; i < values.length; i++) {
            values[i] = buffer.readFloat();
        }

        float[] animation;
        if (animLen != 0) {
            animation = new float[animLen];
            for (int i = 0; i < animation.length; i++) {
                animation[i] = buffer.readFloat();
            }
        } else {
            animation = null;
        }
        operations.add(new FloatExpression(id, values, animation));
    }

    /**
     * Populate the documentation with a description of this operation
     *
     * @param doc to append the description to.
     */
    public static void documentation(@NonNull DocumentationBuilder doc) {
        doc.operation("Expressions Operations", OP_CODE, CLASS_NAME)
                .description("A Float expression")
                .field(DocumentedOperation.INT, "id", "The id of the Color")
                .field(SHORT, "expression_length", "expression length")
                .field(SHORT, "animation_length", "animation description length")
                .field(
                        FLOAT_ARRAY,
                        "expression",
                        "expression_length",
                        "Sequence of Floats representing and expression")
                .field(
                        FLOAT_ARRAY,
                        "AnimationSpec",
                        "animation_length",
                        "Sequence of Floats representing animation curve")
                .field(FLOAT, "duration", "> time in sec")
                .field(INT, "bits", "> WRAP|INITALVALUE | TYPE ")
                .field(FLOAT_ARRAY, "spec", "> [SPEC PARAMETERS] ")
                .field(FLOAT, "initialValue", "> [Initial value] ")
                .field(FLOAT, "wrapValue", "> [Wrap value] ");
    }

    @NonNull
    @Override
    public String deepToString(@NonNull String indent) {
        return indent + toString();
    }

    @Override
    public void serialize(@NonNull MapSerializer serializer) {
        serializer
                .addTags(SerializeTags.EXPRESSION)
                .addType(CLASS_NAME)
                .add("id", mId)
                .addFloatExpressionSrc("srcValues", mSrcValue)
                .add("animation", mFloatAnimation);
    }
}
