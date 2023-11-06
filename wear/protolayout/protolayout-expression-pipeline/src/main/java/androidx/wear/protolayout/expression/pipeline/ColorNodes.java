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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateColorSource;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedColor;

/** Dynamic data nodes which yield colors. */
class ColorNodes {
    private ColorNodes() {}

    /** Dynamic color node that has a fixed value. */
    static class FixedColorNode implements DynamicDataSourceNode<Integer> {
        private final int mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;

        FixedColorNode(
                FixedColor protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            this.mValue = protoNode.getArgb();
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
        public void destroy() {}
    }

    /** Dynamic color node that gets value from the platform source. */
    static class StateColorSourceNode extends StateSourceNode<Integer> {
        StateColorSourceNode(
                DataStore dataStore,
                StateColorSource protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    dataStore,
                    StateSourceNode.<DynamicColor>createKey(
                            protoNode.getSourceNamespace(), protoNode.getSourceKey()),
                    se -> se.getColorVal().getArgb(),
                    downstream);
        }
    }

    /** Dynamic color node that gets animatable value from fixed source. */
    static class AnimatableFixedColorNode extends AnimatableNode
            implements DynamicDataSourceNode<Integer> {

        private final AnimatableFixedColor mProtoNode;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;

        AnimatableFixedColorNode(
                AnimatableFixedColor protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream,
                QuotaManager quotaManager) {

            super(quotaManager, protoNode.getAnimationSpec(), ARGB_EVALUATOR);
            this.mProtoNode = protoNode;
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> mDownstream.onData((Integer) animatedValue));
        }

        @Override
        @UiThread
        public void preInit() {
            mDownstream.onPreUpdate();
        }

        @Override
        @UiThread
        public void init() {
            mQuotaAwareAnimator.setIntValues(mProtoNode.getFromArgb(), mProtoNode.getToArgb());
            startOrSkipAnimator();
        }

        @Override
        @UiThread
        public void destroy() {
            mQuotaAwareAnimator.stopAnimator();
        }
    }

    /** Dynamic color node that gets animatable value from dynamic source. */
    static class DynamicAnimatedColorNode extends AnimatableNode
            implements DynamicDataNode<Integer> {

        final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mInputCallback;

        @Nullable Integer mCurrentValue = null;
        int mPendingCalls = 0;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedColorNode(
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream,
                @NonNull AnimationSpec spec,
                QuotaManager quotaManager) {

            super(quotaManager, spec, ARGB_EVALUATOR);
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> {
                        if (mPendingCalls == 0) {
                            mCurrentValue = (Integer) animatedValue;
                            mDownstream.onData(mCurrentValue);
                        }
                    });
            this.mInputCallback =
                    new DynamicTypeValueReceiverWithPreUpdate<Integer>() {
                        @Override
                        public void onPreUpdate() {
                            mPendingCalls++;

                            if (mPendingCalls == 1) {
                                mDownstream.onPreUpdate();
                            }
                        }

                        @Override
                        public void onData(@NonNull Integer newData) {
                            if (mPendingCalls > 0) {
                                mPendingCalls--;
                            }

                            if (mPendingCalls == 0) {
                                if (mCurrentValue == null) {
                                    mCurrentValue = newData;
                                    mDownstream.onData(mCurrentValue);
                                } else {
                                    mQuotaAwareAnimator.setIntValues(mCurrentValue, newData);
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

        public DynamicTypeValueReceiverWithPreUpdate<Integer> getInputCallback() {
            return mInputCallback;
        }
    }
}
