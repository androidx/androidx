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

import static java.lang.Math.abs;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32;
import androidx.wear.protolayout.expression.DynamicDataKey;
import androidx.wear.protolayout.expression.PlatformHealthSources;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicDataProto.DynamicDataValue;
import androidx.wear.protolayout.expression.proto.DynamicProto.AnimatableFixedInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.DurationPartType;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetDurationPartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.GetZonedDateTimePartOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32SourceType;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.ZonedDateTimePartType;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.function.Function;

/** Dynamic data nodes which yield integers. */
class Int32Nodes {

    private Int32Nodes() {}

    /** Dynamic integer node that has a fixed value. */
    static class FixedInt32Node implements DynamicDataSourceNode<Integer> {
        private final int mValue;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;

        FixedInt32Node(
                FixedInt32 protoNode, DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
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

        @Override
        public int getCost() {
            return FIXED_NODE_COST;
        }
    }

    /** Dynamic integer node that gets value from the platform source. */
    static class LegacyPlatformInt32SourceNode extends StateSourceNode<Integer> {

        LegacyPlatformInt32SourceNode(
                PlatformDataStore dataStore,
                PlatformInt32Source protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    dataStore,
                    getDataKey(protoNode.getSourceType()),
                    getStateExtractor(protoNode.getSourceType()),
                    downstream);
        }

        @NonNull
        private static DynamicDataKey<?> getDataKey(PlatformInt32SourceType type) {
            if (type == PlatformInt32SourceType.PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE) {
                return PlatformHealthSources.Keys.HEART_RATE_BPM;
            }

            if (type == PlatformInt32SourceType.PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT) {
                return PlatformHealthSources.Keys.DAILY_STEPS;
            }

            throw new IllegalArgumentException(
                    "Unknown DynamicInt32 platform source type: " + type);
        }

        @NonNull
        private static Function<DynamicDataValue, Integer> getStateExtractor(
                PlatformInt32SourceType type) {
            if (type == PlatformInt32SourceType.PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE) {
                return se -> (int) se.getFloatVal().getValue();
            }

            if (type == PlatformInt32SourceType.PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT) {
                return se -> se.getInt32Val().getValue();
            }

            throw new IllegalArgumentException(
                    "Unknown DynamicInt32 platform source type: " + type);
        }
    }

    /** Dynamic integer node that supports arithmetic operations. */
    static class ArithmeticInt32Node extends DynamicDataBiTransformNode<Integer, Integer, Integer> {
        private static final String TAG = "ArithmeticInt32Node";

        ArithmeticInt32Node(
                ArithmeticInt32Op protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    downstream,
                    (lhs, rhs) -> {
                        try {
                            switch (protoNode.getOperationType()) {
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
                            Log.e(TAG, "ArithmeticException in ArithmeticInt32Node", ex);
                            return null;
                        }

                        throw new IllegalArgumentException(
                                "Unknown operation type in ArithmeticInt32Node: "
                                        + protoNode.getOperationType());
                    });
        }
    }

    /** Dynamic integer node that gets value from the state. */
    static class StateInt32SourceNode extends StateSourceNode<Integer> {

        StateInt32SourceNode(
                DataStore dataStore,
                StateInt32Source protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    dataStore,
                    StateSourceNode.<DynamicInt32>createKey(
                            protoNode.getSourceNamespace(), protoNode.getSourceKey()),
                    se -> se.getInt32Val().getValue(),
                    downstream);
        }
    }

    /** Dynamic integer node that gets value from float. */
    static class FloatToInt32Node extends DynamicDataTransformNode<Float, Integer> {

        FloatToInt32Node(
                FloatToInt32Op protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    downstream,
                    x -> {
                        switch (protoNode.getRoundMode()) {
                            case ROUND_MODE_UNDEFINED:
                                // Round mode defaults to floor.
                            case ROUND_MODE_FLOOR:
                                return (int) Math.floor(x);
                            case ROUND_MODE_ROUND:
                                return Math.round(x);
                            case ROUND_MODE_CEILING:
                                return (int) Math.ceil(x);
                            case UNRECOGNIZED:
                                break;
                        }
                        throw new IllegalArgumentException(
                                "Unknown rounding mode:" + protoNode.getRoundMode());
                    },
                    x -> x - 1 < Integer.MAX_VALUE && x >= Integer.MIN_VALUE);
        }
    }

    /** Dynamic integer node that gets duration part from a duration. */
    static class GetDurationPartOpNode extends DynamicDataTransformNode<Duration, Integer> {
        private static final String TAG = "GetDurationPartOpNode";

        GetDurationPartOpNode(
                GetDurationPartOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(
                    downstream,
                    duration -> (int) getDurationPart(duration, protoNode.getDurationPart()));
        }

        private static long getDurationPart(Duration duration, DurationPartType durationPartType) {
            switch (durationPartType) {
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
                case DURATION_PART_TYPE_UNDEFINED:
                case UNRECOGNIZED:
                    break;
            }
            throw new IllegalArgumentException("Unknown duration part: " + durationPartType);
        }
    }

    /** Dynamic int32 node that gets animatable value from fixed source. */
    static class AnimatableFixedInt32Node extends AnimatableNode
            implements DynamicDataSourceNode<Integer> {

        private final AnimatableFixedInt32 mProtoNode;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;
        private boolean mFirstUpdateFromAnimatorDone = false;

        AnimatableFixedInt32Node(
                AnimatableFixedInt32 protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream,
                QuotaManager quotaManager) {
            super(quotaManager, protoNode.getAnimationSpec(), AnimatableNode.INT_EVALUATOR);
            this.mProtoNode = protoNode;
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> {
                        // The onPreUpdate has already been called once before the first update.
                        if (mFirstUpdateFromAnimatorDone) {
                            mDownstream.onPreUpdate();
                        }
                        mDownstream.onData((Integer) animatedValue);
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
            mQuotaAwareAnimator.setIntValues(mProtoNode.getFromValue(), mProtoNode.getToValue());
            // For the first update from the animator with the above from & to values, the
            // onPreUpdate has already been called.
            mFirstUpdateFromAnimatorDone = false;
            startOrSkipAnimator();
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

    /** Dynamic int32 node that gets animatable value from dynamic source. */
    static class DynamicAnimatedInt32Node extends AnimatableNode
            implements DynamicDataNode<Integer> {

        final DynamicTypeValueReceiverWithPreUpdate<Integer> mDownstream;
        private final DynamicTypeValueReceiverWithPreUpdate<Integer> mInputCallback;

        @Nullable Integer mCurrentValue = null;
        int mPendingCalls = 0;
        private boolean mFirstUpdateFromAnimatorDone = false;

        // Static analysis complains about calling methods of parent class AnimatableNode under
        // initialization but mInputCallback is only used after the constructor is finished.
        @SuppressWarnings("method.invocation.invalid")
        DynamicAnimatedInt32Node(
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream,
                @NonNull AnimationSpec spec,
                QuotaManager quotaManager) {
            super(quotaManager, spec, AnimatableNode.INT_EVALUATOR);
            this.mDownstream = downstream;
            mQuotaAwareAnimator.addUpdateCallback(
                    animatedValue -> {
                        if (mPendingCalls == 0) {
                            // The onPreUpdate has already been called once before the first update.
                            if (mFirstUpdateFromAnimatorDone) {
                                mDownstream.onPreUpdate();
                            }
                            mCurrentValue = (Integer) animatedValue;
                            mDownstream.onData(mCurrentValue);
                            mFirstUpdateFromAnimatorDone = true;
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

        public DynamicTypeValueReceiverWithPreUpdate<Integer> getInputCallback() {
            return mInputCallback;
        }

        @Override
        public int getCost() {
            return DEFAULT_NODE_COST;
        }
    }

    /** Dynamic integer node that gets date-time part from a zoned date-time. */
    static class GetZonedDateTimePartOpNode
            extends DynamicDataTransformNode<ZonedDateTime, Integer> {
        private static final String TAG = "GetZonedDateTimePartOpNode";

        GetZonedDateTimePartOpNode(
                GetZonedDateTimePartOp protoNode,
                DynamicTypeValueReceiverWithPreUpdate<Integer> downstream) {
            super(downstream, zdt -> (int) getZonedDateTimePart(zdt, protoNode.getPartType()));
        }

        private static long getZonedDateTimePart(ZonedDateTime zdt, ZonedDateTimePartType type) {
            switch (type) {
                case ZONED_DATE_TIME_PART_SECOND:
                    return zdt.getSecond();
                case ZONED_DATE_TIME_PART_MINUTE:
                    return zdt.getMinute();
                case ZONED_DATE_TIME_PART_HOUR_24H:
                    return zdt.getHour();
                case ZONED_DATE_TIME_PART_DAY_OF_WEEK:
                    return zdt.getDayOfWeek().getValue();
                case ZONED_DATE_TIME_PART_DAY_OF_MONTH:
                    return zdt.getDayOfMonth();
                case ZONED_DATE_TIME_PART_MONTH:
                    return zdt.getMonth().getValue();
                case ZONED_DATE_TIME_PART_YEAR:
                    return zdt.getYear();
                case ZONED_DATE_TIME_PART_UNDEFINED:
                case UNRECOGNIZED:
                    break;
            }
            throw new IllegalArgumentException("Unknown ZonedDateTime part: " + type);
        }
    }
}
