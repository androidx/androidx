/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticFloatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateFloatSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;

/** Dynamic data nodes which yield floats. */
class FloatNodes {

    private FloatNodes() {}

    /** Dynamic float node that has a fixed value. */
    static class FixedFloatNode implements DynamicDataSourceNode<Float> {
        @Nullable private final Float mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Float> mDownstream;

        FixedFloatNode(
                FixedFloat protoNode, DynamicTypeValueReceiverWithPreUpdate<Float> downstream) {
            this.mValue = getValidValueOrNull(protoNode.getValue());
            this.mDownstream = downstream;
        }

        @Override
        @UiThread
        public void preInit() {
            mDownstream.onPreUpdate();
        }

        @Override
        @UiThread
        public void init() {
            if (mValue == null) {
                mDownstream.onInvalidated();
            } else {
                mDownstream.onData(mValue);
            }
        }

        @Override
        @UiThread
        public void destroy() {}

        @Override
        public int getCost() {
            return FIXED_NODE_COST;
        }
    }

    /** Dynamic float node that gets value from the state. */
    static class StateFloatSourceNode extends StateSourceNode<Float> {
        StateFloatSourceNode(
                DataStore dataStore,
                StateFloatSource protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Float> downstream) {
            super(
                    dataStore,
                    StateSourceNode.<DynamicFloat>createKey(
                            protoNode.getSourceNamespace(), protoNode.getSourceKey()),
                    se -> getValidValueOrNull(se.getFloatVal().getValue()),
                    downstream);
        }
    }

    /** Dynamic float node that supports arithmetic operations. */
    static class ArithmeticFloatNode extends DynamicDataBiTransformNode<Float, Float, Float> {
        private static final String TAG = "ArithmeticFloatNode";

        ArithmeticFloatNode(
                ArithmeticFloatOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Float> downstream) {
            super(
                    downstream,
                    (lhs, rhs) ->
                            getValidValueOrNull(
                                    computeResult(protoNode.getOperationType(), lhs, rhs)));
        }

        private static float computeResult(
                DynamicProto.ArithmeticOpType opType, float lhs, float rhs) {
            try {
                switch (opType) {
                    case ARITHMETIC_OP_TYPE_ADD:
                        return lhs + rhs;
                    case ARITHMETIC_OP_TYPE_SUBTRACT:
                        return lhs - rhs;
                    case ARITHMETIC_OP_TYPE_MULTIPLY:
                        return lhs * rhs;
                    case ARITHMETIC_OP_TYPE_DIVIDE:
                        return lhs / rhs;
                    case ARITHMETIC_OP_TYPE_MODULO:
                        return lhs % rhs;
                    case ARITHMETIC_OP_TYPE_UNDEFINED:
                    case UNRECOGNIZED:
                        break;
                }
            } catch (ArithmeticException ex) {
                Log.e(TAG, "ArithmeticException in ArithmeticFloatNode", ex);
                return Float.NaN;
            }
            throw new IllegalArgumentException(
                    "Unknown operation type in ArithmeticFloatNode: " + opType);
        }
    }

    /** Dynamic float node that gets value from INTEGER. */
    static class Int32ToFloatNode extends DynamicDataTransformNode<Integer, Float> {

        Int32ToFloatNode(DynamicTypeValueReceiverWithPreUpdate<Float> downstream) {
            super(downstream, i -> (float) i);
        }
    }

    /** Dynamic float node that gets animatable value from fixed source. */
    static class AnimatableFixedFloatNode extends AnimatableNode
            implements DynamicDataSourceNode<Float> {

        private final AnimatableFixedFloat mProtoNode;
        private final DynamicTypeValueReceiverWithPreUpdate<Float> mDownstream;
        private boolean mFirstUpdateFromAnimatorDone = false;

        AnimatableFixedFloatNode(
                AnimatableFixedFloat protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Float> downstream,
                QuotaManager quotaManager) {

            super(quotaManager, protoNode.getAnimationSpec(), AnimatableNode.FLOAT_EVALUATOR);
            this.mProtoNode = protoNode;
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> {
                        // The onPreUpdate has already been called once before the first update.
                        if (mFirstUpdateFromAnimatorDone) {
                            mDownstream.onPreUpdate();
                        }
                        mDownstream.onData((Float) animatedValue);
                        mFirstUpdateFromAnimatorDone = true;
                    });
        }

        @Override
        @UiThread
        public void preInit() {
            mDownstream.onPreUpdate();
        }

        @Override
        @UiThread
        public void init() {
            if (isValid(mProtoNode.getFromValue()) && isValid(mProtoNode.getToValue())) {
                mQuotaAwareAnimator.setFloatValues(
                        mProtoNode.getFromValue(), mProtoNode.getToValue());
                // For the first update from the animator with the above from & to values, the
                // onPreUpdate has already been called.
                mFirstUpdateFromAnimatorDone = false;
                startOrSkipAnimator();
            } else {
                mDownstream.onInvalidated();
            }
        }

        @Override
        @UiThread
        public void destroy() {
            mQuotaAwareAnimator.stopAnimator();
        }

        @Override
        public int getCost() {
            return DEFAULT_NODE_COST;
        }
    }

    /** Dynamic float node that gets animatable value from dynamic source. */
    static class DynamicAnimatedFloatNode extends AnimatableNode implements DynamicDataNode<Float> {

        final DynamicTypeValueReceiverWithPreUpdate<Float> mDownstream;
        private final DynamicTypeValueReceiverWithPreUpdate<Float> mInputCallback;

        @Nullable Float mCurrentValue = null;
        int mPendingCalls = 0;
        private boolean mFirstUpdateFromAnimatorDone = false;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedFloatNode(
                DynamicTypeValueReceiverWithPreUpdate<Float> downstream,
                @NonNull AnimationSpec spec,
                QuotaManager quotaManager) {

            super(quotaManager, spec, AnimatableNode.FLOAT_EVALUATOR);
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> {
                        if (mPendingCalls == 0) {
                            // The onPreUpdate has already been called once before the first update.
                            if (mFirstUpdateFromAnimatorDone) {
                                mDownstream.onPreUpdate();
                            }
                            mCurrentValue = (Float) animatedValue;
                            mDownstream.onData(mCurrentValue);
                            mFirstUpdateFromAnimatorDone = true;
                        }
                    });
            this.mInputCallback =
                    new DynamicTypeValueReceiverWithPreUpdate<Float>() {
                        @Override
                        public void onPreUpdate() {
                            mPendingCalls++;

                            if (mPendingCalls == 1) {
                                mDownstream.onPreUpdate();
                            }
                        }

                        @Override
                        public void onData(@NonNull Float newData) {
                            if (mPendingCalls > 0) {
                                mPendingCalls--;
                            }

                            if (mPendingCalls == 0) {
                                if (mCurrentValue == null) {
                                    mCurrentValue = newData;
                                    mDownstream.onData(mCurrentValue);
                                } else {
                                    mQuotaAwareAnimator.setFloatValues(mCurrentValue, newData);
                                    // For the first update from the animator with the above from &
                                    // to values, the onPreUpdate has already been called.
                                    mFirstUpdateFromAnimatorDone = false;
                                    startOrSkipAnimator();
                                }
                            }
                        }

                        @Override
                        public void onInvalidated() {
                            if (mPendingCalls > 0) {
                                mPendingCalls--;
                            }

                            if (mPendingCalls == 0) {
                                mCurrentValue = null;
                                mDownstream.onInvalidated();
                            }
                        }
                    };
        }

        public DynamicTypeValueReceiverWithPreUpdate<Float> getInputCallback() {
            return mInputCallback;
        }

        @Override
        public int getCost() {
            return DEFAULT_NODE_COST;
        }
    }

    private static boolean isValid(Float value) {
        return value != null && Float.isFinite(value);
    }

    @Nullable
    private static Float getValidValueOrNull(Float value) {
        return isValid(value) ? value : null;
    }
}
