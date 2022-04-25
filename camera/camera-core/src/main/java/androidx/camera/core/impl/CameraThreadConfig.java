/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.util.concurrent.Executor;

/**
 * Configuration options for threads used by the camera stack implementation.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class CameraThreadConfig {

    /**
     * Creates a thread configuration given an executor and a scheduling handler.
     * @param cameraExecutor Executor used to run all camera-related tasks.
     * @param schedulerHandler Handler used for scheduling future tasks (such as with a delay)
     *                          and for legacy APIs that require a handler. Tasks that are
     *                          scheduled with this handler should always be executed by
     *                          cameraExecutor. No business logic should be executed directly by
     *                          this handler.
     * @return the camera thread configuration.
     */
    @NonNull
    public static CameraThreadConfig create(@NonNull Executor cameraExecutor,
            @NonNull Handler schedulerHandler) {
        return new AutoValue_CameraThreadConfig(cameraExecutor, schedulerHandler);
    }

    /**
     * Returns the executor used to run all camera-related tasks.
     */
    @NonNull
    public abstract Executor getCameraExecutor();

    /**
     * Returns the handler used for scheduling future tasks (such as with a delay).
     *
     * <p>This scheduler may also be used for legacy APIs which require a {@link Handler}. Tasks
     * that are scheduled with this handler should always be executed by cameraExecutor. No
     * business logic should be executed directly by this handler.
     */
    @NonNull
    public abstract Handler getSchedulerHandler();
}
