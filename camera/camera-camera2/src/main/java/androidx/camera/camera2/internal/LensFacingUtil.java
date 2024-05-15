/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import android.hardware.camera2.CameraMetadata;

import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalLensFacing;

/**
 * Contains utility methods related to lens facing.
 */
public class LensFacingUtil {

    // Do not allow instantiation.
    private LensFacingUtil() {

    }

    /**
     * Converts a lens facing direction from a {@link CameraMetadata} integer to a camera
     * selector lens facing.
     *
     * @param lensFacingInteger the lens facing integer, as defined in {@link CameraMetadata}.
     * @return the camera selector lens facing.
     * @throws IllegalArgumentException if the specified lens facing integer can not be recognized.
     */
    @OptIn(markerClass = ExperimentalLensFacing.class)
    @CameraSelector.LensFacing
    public static int getCameraSelectorLensFacing(int lensFacingInteger) {
        switch (lensFacingInteger) {
            case CameraMetadata.LENS_FACING_BACK:
                return CameraSelector.LENS_FACING_BACK;
            case CameraMetadata.LENS_FACING_FRONT:
                return CameraSelector.LENS_FACING_FRONT;
            case CameraMetadata.LENS_FACING_EXTERNAL:
                return CameraSelector.LENS_FACING_EXTERNAL;
            default:
                throw new IllegalArgumentException(
                        "The given lens facing integer: " + lensFacingInteger
                                + " can not be recognized.");
        }
    }

    /**
     * Converts a lens facing direction from a camera selector lens facing to a
     * {@link CameraMetadata} integer.
     *
     * @param lensFacing the camera selector lens facing.
     * @return The lens facing integer.
     * @throws IllegalArgumentException if the given lens facing can not be recognized.
     */
    @OptIn(markerClass = ExperimentalLensFacing.class)
    public static int getLensFacingInt(@CameraSelector.LensFacing int lensFacing) {
        switch (lensFacing) {
            case CameraSelector.LENS_FACING_BACK:
                return CameraMetadata.LENS_FACING_BACK;
            case CameraSelector.LENS_FACING_FRONT:
                return CameraMetadata.LENS_FACING_FRONT;
            case CameraSelector.LENS_FACING_EXTERNAL:
                return CameraMetadata.LENS_FACING_EXTERNAL;
            default:
                throw new IllegalArgumentException(
                        "The given lens facing: " + lensFacing + " can not be recognized.");
        }
    }
}
