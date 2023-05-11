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
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

class FakeSensorGateway implements SensorGateway {
    final List<Consumer> registeredConsumers = new ArrayList<>();

    @Override
    public void enableUpdates() {
    }

    @Override
    public void disableUpdates() {
    }

    @Override
    public void registerSensorGatewayConsumer(
            @NonNull PlatformDataKey<?> key, @NonNull Consumer consumer) {
        registeredConsumers.add(consumer);
    }

    @Override
    public void registerSensorGatewayConsumer(
            @NonNull PlatformDataKey<?> key,
            @NonNull Executor executor,
            @NonNull Consumer consumer) {
        registerSensorGatewayConsumer(key, consumer);
    }

    @Override
    public void unregisterSensorGatewayConsumer(
            @NonNull PlatformDataKey<?> key, @NonNull Consumer consumer) {
        registeredConsumers.remove(consumer);
    }
}