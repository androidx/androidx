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

import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.ImageOutputConfig.RotationValue;

/**
 * An interface for retrieving camera information.
 *
 * <p>Contains methods for retrieving characteristics for a specific camera.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public interface CameraInfo {

    /**
     * Returns the LensFacing of this camera.
     *
     * @return One of {@link LensFacing#FRONT}, {@link LensFacing#BACK}, or <code>null</code> if the
     * LensFacing does not fall into one of these two categories.
     */
    // TODO(b/122975195): Remove @Nullable and null return type once we have a LensFacing type which
    // can be used to represent non-BACK or FRONT facing lenses.
    @Nullable
    LensFacing getLensFacing();

    /**
     * Returns the sensor rotation, in degrees, relative to the device's "natural" rotation.
     *
     * @return The sensor orientation in degrees.
     * @see Surface#ROTATION_0, the natural orientation of the device.
     */
    int getSensorRotationDegrees();

    /**
     * Returns the sensor rotation, in degrees, relative to the given rotation value.
     *
     * <p>Valid values for the relative rotation are {@link Surface#ROTATION_0}, {@link
     * Surface#ROTATION_90}, {@link Surface#ROTATION_180}, {@link Surface#ROTATION_270}.
     *
     * @param relativeRotation The rotation relative to which the output will be calculated.
     * @return The sensor orientation in degrees.
     */
    int getSensorRotationDegrees(@RotationValue int relativeRotation);
}
