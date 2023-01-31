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

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.EpochTimePlatformDataSource;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.PlatformDataSource;
import androidx.wear.protolayout.expression.pipeline.PlatformDataSources.SensorGatewayPlatformDataSource;
import androidx.wear.protolayout.expression.proto.DynamicProto.ArithmeticInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatToInt32Op;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32Source;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32SourceType;
import androidx.wear.protolayout.expression.proto.DynamicProto.StateInt32Source;
import androidx.wear.protolayout.expression.proto.FixedProto.FixedInt32;

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
        @Nullable private final EpochTimePlatformDataSource mEpochTimePlatformDataSource;
        private final PlatformInt32Source mProtoNode;
        private final DynamicTypeValueReceiver<Integer> mDownstream;

        PlatformInt32SourceNode(
                PlatformInt32Source protoNode,
                @Nullable EpochTimePlatformDataSource epochTimePlatformDataSource,
                @Nullable SensorGatewayPlatformDataSource sensorGatewaySource,
                DynamicTypeValueReceiver<Integer> downstream) {
            this.mProtoNode = protoNode;
            this.mEpochTimePlatformDataSource = epochTimePlatformDataSource;
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
                case PLATFORM_INT32_SOURCE_TYPE_EPOCH_TIME_SECONDS:
                    return mEpochTimePlatformDataSource;
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
}
