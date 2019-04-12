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

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraX.LensFacing;

import java.util.Set;

/**
 * The factory class that creates {@link BaseCamera} instances.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraFactory {

    /** Get the camera with the associated id. */
    BaseCamera getCamera(String cameraId);

    /** Get ids for all the available cameras. */
    Set<String> getAvailableCameraIds() throws CameraInfoUnavailableException;

    /** Get the id of the camera with the specified lens facing. */
    @Nullable
    String cameraIdForLensFacing(LensFacing lensFacing) throws CameraInfoUnavailableException;
}
