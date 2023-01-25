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

import static androidx.wear.protolayout.expression.pipeline.AnimationsHelper.applyAnimationSpecToAnimator;

import android.animation.ValueAnimator;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticFloatOp;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedFloat;

/** Dynamic data nodes which yield floats. */
class FloatNodes {

    private FloatNodes() {}

    /** Dynamic float node that has a fixed value. */
    static class FixedFloatNode implements DynamicDataSourceNode<Float> {
        private final float mValue;
        private final DynamicTypeValueReceiver<Float> mDownstream;

        FixedFloatNode(FixedFloat protoNode, DynamicTypeValueReceiver<Float> downstream) {
            this.mValue = protoNode.getValue();
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
            mDownstream.onData(mValue);
        }

        @Override
        @UiThread
        public void destroy() {}
    }

    /** Dynamic float node that gets value from the state. */
    static class StateFloatNode extends StateSourceNode<Float> {
        StateFloatNode(
                ObservableStateStore observableStateStore,
                String bindKey,
                DynamicTypeValueReceiver<Float> downstream) {
            super(observableStateStore, bindKey, se -> se.getFloatVal().getValue(), downstream);
        }
    }

    /** Dynamic float node that supports arithmetic operations. */
    static class ArithmeticFloatNode extends DynamicDataBiTransformNode<Float, Float, Float> {
        private static final String TAG = "ArithmeticFloatNode";

        ArithmeticFloatNode(
                ArithmeticFloatOp protoNode, DynamicTypeValueReceiver<Float> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        try {
                            switch (protoNode.getOperationType()) {
                                case ARITHMETIC_OP_TYPE_UNDEFINED:
                                case UNRECOGNIZED:
                                    Log.e(TAG, "Unknown operation type in ArithmeticFloatNode");
                                    return Float.NaN;
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
                            }
                        } catch (ArithmeticException ex) {
                            Log.e(TAG, "ArithmeticException in ArithmeticFloatNode", ex);
                            return Float.NaN;
                        }

                        Log.e(TAG, "Unknown operation type in ArithmeticFloatNode");
                        return Float.NaN;
                    });
        }
    }

    /** Dynamic float node that gets value from INTEGER. */
    static class Int32ToFloatNode extends DynamicDataTransformNode<Integer, Float> {

        Int32ToFloatNode(DynamicTypeValueReceiver<Float> downstream) {
            super(downstream, i -> (float) i);
        }
    }

    /** Dynamic float node that gets animatable value from fixed source. */
    static class AnimatableFixedFloatNode extends AnimatableNode
            implements DynamicDataSourceNode<Float> {

        private final AnimatableFixedFloat mProtoNode;
        private final DynamicTypeValueReceiver<Float> mDownstream;

        AnimatableFixedFloatNode(
                AnimatableFixedFloat protoNode,
                DynamicTypeValueReceiver<Float> downstream,
                QuotaManager quotaManager) {
            super(quotaManager);
            this.mProtoNode = protoNode;
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
            ValueAnimator animator =
                    ValueAnimator.ofFloat(mProtoNode.getFromValue(), mProtoNode.getToValue());
            animator.addUpdateListener(a -> mDownstream.onData((float) a.getAnimatedValue()));

            applyAnimationSpecToAnimator(animator, mProtoNode.getSpec());

            mQuotaAwareAnimator.updateAnimator(animator);
            startOrSkipAnimator();
        }

        @Override
        @UiThread
        public void destroy() {
            mQuotaAwareAnimator.stopAnimator();
        }
    }

    /** Dynamic float node that gets animatable value from dynamic source. */
    static class DynamicAnimatedFloatNode extends AnimatableNode implements DynamicDataNode<Float> {

        final DynamicTypeValueReceiver<Float> mDownstream;
        private final DynamicTypeValueReceiver<Float> mInputCallback;

        @Nullable Float mCurrentValue = null;
        int mPendingCalls = 0;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedFloatNode(
                DynamicTypeValueReceiver<Float> downstream,
                @NonNull AnimationSpec spec,
                QuotaManager quotaManager) {
            super(quotaManager);
            this.mDownstream = downstream;
            this.mInputCallback =
                    new DynamicTypeValueReceiver<Float>() {
                        @Override
                        public void onPreUpdate() {
                            mPendingCalls++;

                            if (mPendingCalls == 1) {
                                mDownstream.onPreUpdate();

                                mQuotaAwareAnimator.resetAnimator();
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
                                    ValueAnimator animator =
                                            ValueAnimator.ofFloat(mCurrentValue, newData);

                                    applyAnimationSpecToAnimator(animator, spec);

                                    animator.addUpdateListener(
                                            a -> {
                                                if (mPendingCalls == 0) {
                                                    mCurrentValue = (float) a.getAnimatedValue();
                                                    mDownstream.onData(mCurrentValue);
                                                }
                                            });

                                    mQuotaAwareAnimator.updateAnimator(animator);
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

        public DynamicTypeValueReceiver<Float> getInputCallback() {
            return mInputCallback;
        }
    }
}
