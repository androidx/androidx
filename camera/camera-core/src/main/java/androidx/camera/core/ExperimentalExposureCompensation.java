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

import androidx.annotation.experimental.Experimental;

import java.lang.annotation.Retention;

/**
 * Denotes that the annotated method uses the experimental ExposureCompensation APIs that can
 * control the exposure compensation of the camera.
 *
 * <p>The feature allow the user to control the exposure compensation of the camera, it includes a
 * setter in {@link androidx.camera.core.CameraControl} and a getter in
 * {@link androidx.camera.core.CameraInfo}.
 */
@Retention(CLASS)
@Experimental
public @interface ExperimentalExposureCompensation {
}
