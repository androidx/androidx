/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.sdkservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** A bound service which is exported so Health Data Platform can bind back to its clients. */
public final class HealthDataSdkService extends Service {
    private static final String TAG = HealthDataSdkService.class.getSimpleName();

    @VisibleForTesting
    static final String BIND_ACTION = "androidx.health.platform.client.ACTION_BIND_SDK_SERVICE";

    @Override
    public @Nullable IBinder onBind(@NonNull Intent intent) {
        String action = intent.getAction();
        if (!BIND_ACTION.equals(action)) {
            Log.i(TAG, String.format("Bind request with an invalid action [%s]", action));
            return null;
        }
        Executor executor =
                Executors.newSingleThreadExecutor(
                        new ThreadFactoryBuilder()
                                .setNameFormat("HealthData-HealthDataSdkService-%d")
                                .build());
        // Pass application context to avoid leaking the service.
        return new HealthDataSdkServiceStubImpl(this.getApplicationContext(), executor);
    }
}
