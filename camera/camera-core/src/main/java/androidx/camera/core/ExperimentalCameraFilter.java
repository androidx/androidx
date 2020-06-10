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
 * Denotes that the annotated method uses an experimental API that customizes camera filters to
 * select a specific camera.
 *
 * <p>{@link CameraSelector.Builder#addCameraFilter(CameraFilter)} can be used to add
 * customized {@link CameraFilter} implementations to the camera selector. Those camera filters
 * will be applied with the internal camera filters. If the {@link CameraFilter}s result in no
 * available camera, the camera selector will thrown an IllegalArgumentException.
 */
@Retention(CLASS)
@Experimental
public @interface ExperimentalCameraFilter {
}
