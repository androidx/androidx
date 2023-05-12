/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.PlatformHealthSources;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;

/** This provider provides sensor data as state value. */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class SensorGatewaySingleDataProvider implements PlatformDataProvider {
    @NonNull private final SensorGateway mSensorGateway;
    @NonNull final PlatformDataKey<?> mSupportedKey;
    @Nullable private SensorGateway.Consumer mSensorGatewayConsumer = null;

    @NonNull Function<Double, DynamicDataValue> mConvertFunc;

    public SensorGatewaySingleDataProvider(
            @NonNull SensorGateway sensorGateway,
            @NonNull PlatformDataKey<?> supportedKey
    ) {
        this.mSensorGateway = sensorGateway;
        this.mSupportedKey = supportedKey;

        if (mSupportedKey.equals(PlatformHealthSources.HEART_RATE_BPM)) {
            mConvertFunc = value -> DynamicDataValue.fromFloat(value.floatValue());
        } else { // mSupportedKey.equals(PlatformHealthSources.DAILY_STEPS)
            mConvertFunc = value -> DynamicDataValue.fromInt(value.intValue());
        }
    }

    @Override
    @SuppressWarnings("HiddenTypeParameters")
    public void registerForData(
            @NonNull Executor executor, @NonNull PlatformDataReceiver callback) {
        SensorGateway.Consumer sensorConsumer =
                new SensorGateway.Consumer() {
                    @Override
                    public void onData(double value) {
                        executor.execute(() -> callback.onData(
                                Map.of(mSupportedKey, mConvertFunc.apply(value)))
                        );
                    }

                    @Override
                    public void onInvalidated() {
                        executor.execute(() -> callback.onInvalidated(
                                Collections.singleton(mSupportedKey)));
                    }
                };
        mSensorGatewayConsumer = sensorConsumer;
        mSensorGateway.registerSensorGatewayConsumer(mSupportedKey, sensorConsumer);
    }

    @Override
    public void unregisterForData() {
        if (mSensorGatewayConsumer != null) {
            mSensorGateway.unregisterSensorGatewayConsumer(mSupportedKey, mSensorGatewayConsumer);
            mSensorGatewayConsumer = null;
        }
    }
}
