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

package androidx.camera.core;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses an experimental API that configures CameraX to
 * limit the available cameras applications can use in order to optimize the initialization
 * latency.
 *
 * <p>Once the configuration is enabled, only cameras selected by this CameraSelector can be used
 * in the applications. If the application binds the use cases with a CameraSelector that selects
 * a unavailable camera, a {@link IllegalArgumentException} will be thrown.
 *
 * <p>CameraX initialization performs tasks including enumerating cameras, querying
 * CameraCharacteristics and retrieving properties preparing for resolution determination. On
 * some low end devices, these could take significant amount of time. Using the API can avoid the
 * initialization of unnecessary cameras and speed up the time for camera start-up. For example,
 * if the application uses only back cameras, it can set this configuration by
 * CameraSelector.DEFAULT_BACK_CAMERA and then CameraX will avoid initializing front cameras to
 * reduce the latency.
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalAvailableCamerasLimiter {
}
