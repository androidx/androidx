/*
 * Copyright (C) 2019 The Android Open Source Project
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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import java.util.Collection;

/**
 * The base camera interface. It is controlled by the change of state in use cases.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface BaseCamera extends UseCase.StateChangeListener,
        CameraControlInternal.ControlUpdateListener {
    /**
     * Open the camera asynchronously.
     *
     * <p>Once the camera has been opened use case state transitions can be used to control the
     * camera pipeline.
     */
    void open();

    /**
     * Close the camera asynchronously.
     *
     * <p>Once the camera is closed the camera will no longer produce data. The camera must be
     * reopened for it to produce data again.
     */
    void close();

    /**
     * Release the camera.
     *
     * <p>Once the camera is released it is permanently closed. A new instance must be created to
     * access the camera.
     */
    void release();

    /**
     * Sets the use case to be in the state where the capture session will be configured to handle
     * capture requests from the use cases.
     */
    void addOnlineUseCase(Collection<UseCase> useCases);

    /**
     * Removes the use case to be in the state where the capture session will be configured to
     * handle capture requests from the use cases.
     */
    void removeOnlineUseCase(Collection<UseCase> useCases);

    /** Returns the global CameraControlInternal attached to this camera. */
    CameraControlInternal getCameraControlInternal();

    /** Returns an interface to retrieve characteristics of the camera. */
    CameraInfo getCameraInfo() throws CameraInfoUnavailableException;
}
