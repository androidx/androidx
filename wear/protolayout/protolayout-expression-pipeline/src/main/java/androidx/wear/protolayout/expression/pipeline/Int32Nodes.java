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
import static java.lang.Math.abs;

import android.animation.ValueAnimator;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.PlatformDataSource;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.SensorGatewayPlatformDataSource;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.DurationPartType;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32SourceType;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetDurationPartOp;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;
import java.time.Duration;

/** Dynamic data nodes which yield integers. */
class Int32Nodes {
    private Int32Nodes() {}

    /** Dynamic integer node that has a fixed value. */
    static class FixedInt32Node implements DynamicDataSourceNode<Integer> {
        private final int mValue;
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        FixedInt32Node(FixedInt32 protoNode, DynamicTypeValueReceiver<Integer> downstream) {
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

    /** Dynamic integer node that gets value from the platform source. */
    static class PlatformInt32SourceNode implements DynamicDataSourceNode<Integer> {
        private static final String TAG = "PlatformInt32SourceNode";

        @Nullable private final SensorGatewayPlatformDataSource mSensorGatewaySource;
        private final PlatformInt32Source mProtoNode;
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        PlatformInt32SourceNode(
                PlatformInt32Source protoNode,
                @Nullable SensorGatewayPlatformDataSource sensorGatewaySource,
                DynamicTypeValueReceiver<Integer> downstream) {
            this.mProtoNode = protoNode;
            this.mSensorGatewaySource = sensorGatewaySource;
            this.mDownstream = downstream;
        }

        @Override
        @UiThread
        public void preInit() {
            if (platformInt32SourceTypeToPlatformDataSource(mProtoNode.getSourceType()) != null) {
                mDownstream.onPreUpdate();
            }
        }

        @Override
        @UiThread
        public void init() {
            PlatformDataSource dataSource =
                    platformInt32SourceTypeToPlatformDataSource(mProtoNode.getSourceType());
            if (dataSource != null) {
                dataSource.registerForData(mProtoNode.getSourceType(), mDownstream);
            } else {
                mDownstream.onInvalidated();
            }
        }

        @Override
        @UiThread
        public void destroy() {
            PlatformDataSource dataSource =
                    platformInt32SourceTypeToPlatformDataSource(mProtoNode.getSourceType());
            if (dataSource != null) {
                dataSource.unregisterForData(mProtoNode.getSourceType(), mDownstream);
            }
        }

        @Nullable
        private PlatformDataSource platformInt32SourceTypeToPlatformDataSource(
                PlatformInt32SourceType sourceType) {
            switch (sourceType) {
                case UNRECOGNIZED:
                case PLATFORM_INT32_SOURCE_TYPE_UNDEFINED:
                    Log.w(TAG, "Unknown PlatformInt32SourceType");
                    return null;
                case PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE:
                case PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT:
                    return mSensorGatewaySource;
            }
            Log.w(TAG, "Unknown PlatformInt32SourceType");
            return null;
        }
    }

    /** Dynamic integer node that supports arithmetic operations. */
    static class ArithmeticInt32Node extends DynamicDataBiTransformNode<Integer, Integer, Integer> {
        private static final String TAG = "ArithmeticInt32Node";

        ArithmeticInt32Node(
                ArithmeticInt32Op protoNode, DynamicTypeValueReceiver<Integer> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        try {
                            switch (protoNode.getOperationType()) {
                                case ARITHMETIC_OP_TYPE_UNDEFINED:
                                case UNRECOGNIZED:
                                    Log.e(TAG, "Unknown operation type in ArithmeticInt32Node");
                                    return 0;
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
                            Log.e(TAG, "ArithmeticException in ArithmeticInt32Node", ex);
                            return 0;
                        }

                        Log.e(TAG, "Unknown operation type in ArithmeticInt32Node");
                        return 0;
                    });
        }
    }

    /** Dynamic integer node that gets value from the state. */
    static class StateInt32SourceNode extends StateSourceNode<Integer> {

        StateInt32SourceNode(
                ObservableStateStore observableStateStore,
                StateInt32Source protoNode,
                DynamicTypeValueReceiver<Integer> downstream) {
            super(
                    observableStateStore,
                    protoNode.getSourceKey(),
                    se -> se.getInt32Val().getValue(),
                    downstream);
        }
    }

    /** Dynamic integer node that gets value from float. */
    static class FloatToInt32Node extends DynamicDataTransformNode<Float, Integer> {

        FloatToInt32Node(FloatToInt32Op protoNode, DynamicTypeValueReceiver<Integer> downstream) {
            super(
                    downstream,
                    x -> {
                        switch (protoNode.getRoundMode()) {
                            case ROUND_MODE_UNDEFINED:
                            case ROUND_MODE_FLOOR:
                                return (int) Math.floor(x);
                            case ROUND_MODE_ROUND:
                                return Math.round(x);
                            case ROUND_MODE_CEILING:
                                return (int) Math.ceil(x);
                            default:
                                throw new IllegalArgumentException("Unknown rounding mode");
                        }
                    });
        }
    }

    /** Dynamic integer node that gets duration part from a duration. */
    static class GetDurationPartOpNode extends DynamicDataTransformNode<Duration, Integer> {

        private static final String TAG = "GetDurationPartOpNode";

        GetDurationPartOpNode(
            GetDurationPartOp protoNode, DynamicTypeValueReceiver<Integer> downstream) {
            super(downstream,
                duration -> (int) getDurationPart(duration, protoNode.getDurationPart()));
        }

        private static long getDurationPart(Duration duration, DurationPartType durationPartType) {
            switch (durationPartType) {
                case DURATION_PART_TYPE_UNDEFINED:
                case UNRECOGNIZED:
                    Log.e(TAG, "Unknown duration part type in GetDurationPartOpNode");
                    return 0;
                case DURATION_PART_TYPE_DAYS:
                    return abs(duration.getSeconds() / (3600 * 24));
                case DURATION_PART_TYPE_HOURS:
                    return abs((duration.getSeconds() / 3600) % 24);
                case DURATION_PART_TYPE_MINUTES:
                    return abs((duration.getSeconds() / 60) % 60);
                case DURATION_PART_TYPE_SECONDS:
                    return abs(duration.getSeconds() % 60);
                case DURATION_PART_TYPE_TOTAL_DAYS:
                    return duration.toDays();
                case DURATION_PART_TYPE_TOTAL_HOURS:
                    return duration.toHours();
                case DURATION_PART_TYPE_TOTAL_MINUTES:
                    return duration.toMinutes();
                case DURATION_PART_TYPE_TOTAL_SECONDS:
                    return duration.getSeconds();
            }
            throw new IllegalArgumentException("Unknown duration part");
        }
    }

    /** Dynamic int32 node that gets animatable value from fixed source. */
    static class AnimatableFixedInt32Node extends AnimatableNode
            implements DynamicDataSourceNode<Integer> {

        private final AnimatableFixedInt32 mProtoNode;
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        AnimatableFixedInt32Node(
                AnimatableFixedInt32 protoNode,
                DynamicTypeValueReceiver<Integer> downstream,
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
                    ValueAnimator.ofInt(mProtoNode.getFromValue(), mProtoNode.getToValue());
            applyAnimationSpecToAnimator(animator, mProtoNode.getAnimationSpec());
            animator.addUpdateListener(a -> mDownstream.onData((Integer) a.getAnimatedValue()));
            mQuotaAwareAnimator.updateAnimator(animator);
            startOrSkipAnimator();
        }

        @Override
        @UiThread
        public void destroy() {
            mQuotaAwareAnimator.stopAnimator();
        }
    }

    /** Dynamic int32 node that gets animatable value from dynamic source. */
    static class DynamicAnimatedInt32Node extends AnimatableNode implements
            DynamicDataNode<Integer> {

        final DynamicTypeValueReceiver<Integer> mDownstream;
        private final DynamicTypeValueReceiver<Integer> mInputCallback;

        @Nullable
        Integer mCurrentValue = null;
        int mPendingCalls = 0;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedInt32Node(
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
                        public void onData(Integer newData) {
                            if (mPendingCalls > 0) {
                                mPendingCalls--;
                            }

                            if (mPendingCalls == 0) {
                                if (mCurrentValue == null) {
                                    mCurrentValue = newData;
                                    mDownstream.onData(mCurrentValue);
                                } else {
                                    ValueAnimator animator = ValueAnimator.ofInt(mCurrentValue,
                                            newData);

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
