/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.NonNull;

/**
 * The camera interface. It is used to control the flow of data to use cases, control the
 * camera via the {@link CameraControl}, and publish the state of the camera via {@link CameraInfo}.
 */
public interface Camera {

    /**
     * Returns a control which can be used to drive this camera's settings.
     *
     * @return the {@link CameraControl}.
     */
    @NonNull
    CameraControl getCameraControl();

    /**
     * Returns information about this camera.
     *
     * <p>The returned information can be used to query static camera
     * characteristics or observe the runtime state of the camera.
     *
     * @return the {@link CameraInfo}.
     */
    @NonNull
    CameraInfo getCameraInfo();
}
