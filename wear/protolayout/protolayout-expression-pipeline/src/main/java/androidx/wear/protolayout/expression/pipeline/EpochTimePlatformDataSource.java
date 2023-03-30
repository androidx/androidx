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

import androidx.collection.SimpleArrayMap;
import androidx.wear.protolayout.expression.pipeline.TimeGateway.TimeCallback;

import java.time.Instant;
import java.util.concurrent.Executor;

/** Utility for time data source. */
class EpochTimePlatformDataSource {
    private final Executor mUiExecutor;
    private final TimeGateway mGateway;
    private final SimpleArrayMap<DynamicTypeValueReceiver<Instant>, TimeCallback>
            mConsumerToTimeCallback = new SimpleArrayMap<>();

    EpochTimePlatformDataSource(Executor uiExecutor, TimeGateway gateway) {
        mUiExecutor = uiExecutor;
        mGateway = gateway;
    }

    public void registerForData(DynamicTypeValueReceiver<Instant> consumer) {
        TimeCallback timeCallback =
                new TimeCallback() {
                    @Override
                    public void onPreUpdate() {
                        consumer.onPreUpdate();
                    }

                    @Override
                    public void onData() {
                        consumer.onData(Instant.now());
                    }
                };
        mGateway.registerForUpdates(mUiExecutor, timeCallback);
        mConsumerToTimeCallback.put(consumer, timeCallback);
    }

    public void unregisterForData(DynamicTypeValueReceiver<Instant> consumer) {
        TimeCallback timeCallback = mConsumerToTimeCallback.remove(consumer);
        if (timeCallback != null) {
            mGateway.unregisterForUpdates(timeCallback);
        }
    }
}
