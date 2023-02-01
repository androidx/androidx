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

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.DoNotInline;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;
import androidx.wear.protolayout.expression.pipeline.TimeGateway.TimeCallback;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway.SensorDataType;
import androidx.wear.protolayout.expression.proto.DynamicProto.PlatformInt32SourceType;

import java.util.Map;
import java.util.concurrent.Executor;

/** Utility for various platform data sources. */
class PlatformDataSources {
    private PlatformDataSources() {}

    interface PlatformDataSource {
        void registerForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> consumer);

        void unregisterForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> consumer);
    }

    /** Utility for time data source. */
    static class EpochTimePlatformDataSource implements PlatformDataSource {
        private final Executor mUiExecutor;
        private final TimeGateway mGateway;
        private final SimpleArrayMap<DynamicTypeValueReceiver<Integer>, TimeCallback>
                mConsumerToTimeCallback = new SimpleArrayMap<>();

        EpochTimePlatformDataSource(Executor uiExecutor, TimeGateway gateway) {
            mUiExecutor = uiExecutor;
            mGateway = gateway;
        }

        @Override
        public void registerForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> consumer) {
            TimeCallback timeCallback =
                    new TimeCallback() {
                        @Override
                        public void onPreUpdate() {
                            consumer.onPreUpdate();
                        }

                        @Override
                        public void onData() {
                            long currentEpochTimeSeconds = System.currentTimeMillis() / 1000;
                            consumer.onData((int) currentEpochTimeSeconds);
                        }
                    };
            mGateway.registerForUpdates(mUiExecutor, timeCallback);
            mConsumerToTimeCallback.put(consumer, timeCallback);
        }

        @Override
        public void unregisterForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> consumer) {
            TimeCallback timeCallback = mConsumerToTimeCallback.remove(consumer);
            if (timeCallback != null) {
                mGateway.unregisterForUpdates(timeCallback);
            }
        }
    }

    /** Utility for sensor data source. */
    static class SensorGatewayPlatformDataSource implements PlatformDataSource {
        private static final String TAG = "SensorGtwPltDataSource";
        final Executor mUiExecutor;
        private final SensorGateway mSensorGateway;
        private final Map<DynamicTypeValueReceiver<Integer>, SensorGateway.Consumer>
                mCallbackToRegisteredSensorConsumer = new ArrayMap<>();

        SensorGatewayPlatformDataSource(Executor uiExecutor, SensorGateway sensorGateway) {
            this.mUiExecutor = uiExecutor;
            this.mSensorGateway = sensorGateway;
        }

        @SensorDataType
        private int mapSensorPlatformSource(PlatformInt32SourceType platformSource) {
            switch (platformSource) {
                case PLATFORM_INT32_SOURCE_TYPE_CURRENT_HEART_RATE:
                    return SensorGateway.SENSOR_DATA_TYPE_HEART_RATE;
                case PLATFORM_INT32_SOURCE_TYPE_DAILY_STEP_COUNT:
                    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
                        return Api29Impl.getSensorDataTypeDailyStepCount();
                    } else {
                        return SensorGateway.SENSOR_DATA_TYPE_INVALID;
                    }
                default:
                    throw new IllegalArgumentException("Unknown PlatformSourceType");
            }
        }

        @RequiresApi(VERSION_CODES.Q)
        private static class Api29Impl {
            @DoNotInline
            static int getSensorDataTypeDailyStepCount() {
                return SensorGateway.SENSOR_DATA_TYPE_DAILY_STEP_COUNT;
            }
        }

        @Override
        @SuppressWarnings("ExecutorTaskName")
        public void registerForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> callback) {
            @SensorDataType int sensorDataType = mapSensorPlatformSource(sourceType);
            SensorGateway.Consumer sensorConsumer =
                    new SensorGateway.Consumer() {
                        @Override
                        public void onData(double value) {
                            mUiExecutor.execute(() -> callback.onData((int) value));
                        }

                        @Override
                        @SensorDataType
                        public int getRequestedDataType() {
                            return sensorDataType;
                        }
                    };
            mCallbackToRegisteredSensorConsumer.put(callback, sensorConsumer);
            mSensorGateway.registerSensorGatewayConsumer(sensorConsumer);
        }

        @Override
        public void unregisterForData(
                PlatformInt32SourceType sourceType, DynamicTypeValueReceiver<Integer> consumer) {
            SensorGateway.Consumer sensorConsumer =
                    mCallbackToRegisteredSensorConsumer.get(consumer);
            if (sensorConsumer != null) {
                mSensorGateway.unregisterSensorGatewayConsumer(sensorConsumer);
            }
        }
    }
}
