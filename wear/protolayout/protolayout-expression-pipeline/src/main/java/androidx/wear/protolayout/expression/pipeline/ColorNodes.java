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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
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
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        FixedColorNode(FixedColor protoNode, DynamicTypeValueReceiver<Integer> downstream) {
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
                ObservableStateStore observableStateStore,
                StateColorSource protoNode,
                DynamicTypeValueReceiver<Integer> downstream) {
            super(
                    observableStateStore,
                    protoNode.getSourceKey(),
                    se -> se.getColorVal().getArgb(),
                    downstream);
        }
    }

    /** Dynamic color node that gets animatable value from fixed source. */
    static class AnimatableFixedColorNode extends AnimatableNode
            implements DynamicDataSourceNode<Integer> {

        private final AnimatableFixedColor mProtoNode;
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        AnimatableFixedColorNode(
                AnimatableFixedColor protoNode,
                DynamicTypeValueReceiver<Integer> mDownstream,
                QuotaManager quotaManager) {
            super(quotaManager);
            this.mProtoNode = protoNode;
            this.mDownstream = mDownstream;
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
                    ValueAnimator.ofArgb(mProtoNode.getFromArgb(), mProtoNode.getToArgb());
            animator.addUpdateListener(a -> mDownstream.onData((Integer) a.getAnimatedValue()));

            applyAnimationSpecToAnimator(animator, mProtoNode.getAnimationSpec());

            mQuotaAwareAnimator.updateAnimator(animator);
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

        final DynamicTypeValueReceiver<Integer> mDownstream;
        private final DynamicTypeValueReceiver<Integer> mInputCallback;

        @Nullable Integer mCurrentValue = null;
        int mPendingCalls = 0;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedColorNode(
                DynamicTypeValueReceiver<Integer> downstream,
                @NonNull AnimationSpec spec,
                QuotaManager quotaManager) {
            super(quotaManager);
            this.mDownstream = downstream;
            this.mInputCallback =
                    new DynamicTypeValueReceiver<Integer>() {
                        @Override
                        public void onPreUpdate() {
                            mPendingCalls++;

                            if (mPendingCalls == 1) {
                                mDownstream.onPreUpdate();

                                mQuotaAwareAnimator.resetAnimator();
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
                                    ValueAnimator animator =
                                            ValueAnimator.ofArgb(mCurrentValue, newData);

                                    applyAnimationSpecToAnimator(animator, spec);
                                    animator.addUpdateListener(
                                            a -> {
                                                if (mPendingCalls == 0) {
                                                    mCurrentValue = (Integer) a.getAnimatedValue();
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

        public DynamicTypeValueReceiver<Integer> getInputCallback() {
            return mInputCallback;
        }
    }
}
