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

import android.graphics.PointF;
import android.view.Display;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraInfoInternal;

/**
 * A {@link MeteringPointFactory} that can convert a {@link View} (x, y) into a
 * {@link MeteringPoint} which can then be used to construct a {@link FocusMeteringAction} to
 * start a focus and metering action.
 *
 * <p>For apps showing full camera preview in a View without any scaling, cropping or
 * rotating applied, they can simply use view width and height to create the
 * {@link DisplayOrientedMeteringPointFactory} and then pass {@link View} (x, y) to create a
 * {@link MeteringPoint}. This factory will convert the (x, y) into the sensor (x, y) based on
 * display rotation and lensFacing.
 *
 * <p>If camera preview is scaled, cropped or rotated in the {@link View}, it is applications'
 * duty to transform the coordinates properly so that the width and height of this
 * factory represents the full Preview FOV and also the (x,y) passed to create
 * {@link MeteringPoint} needs to be adjusted by apps to the  coordinates left-top (0,0) -
 * right-bottom (width, height). For example, if the preview is scaled to 2X from the center and
 * is cropped in a {@link View}. Assuming that the dimension of View is (240, 320), then the
 * width/height of this {@link DisplayOrientedMeteringPointFactory} should be (480, 640).  And
 * the (x, y) from the {@link View} should be converted to (x + (480-240)/2, y + (640 - 320)/2)
 * first.
 *
 * @see MeteringPoint
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class DisplayOrientedMeteringPointFactory extends MeteringPointFactory {
    /** The logical width of FoV in current display orientation */
    private final float mWidth;
    /** The logical height of FoV in current display orientation */
    private final float mHeight;

    /** {@link Display} used for detecting display orientation */
    @NonNull
    private final Display mDisplay;
    @NonNull
    private final CameraInfo mCameraInfo;

    /**
     * Creates a {@link DisplayOrientedMeteringPointFactory} for converting View (x, y) into a
     * {@link MeteringPoint} based on the current display's rotation and {@link CameraInfo}.
     *
     * <p>The width/height of this factory forms a coordinate left-top (0, 0) - right-bottom
     * (width, height) which represents the full camera preview FOV in the display's
     * orientation. For apps showing full camera preview in a {@link View}, it is as simple as
     * passing View's width/height and passing View (x, y) directly to create a
     * {@link MeteringPoint}. Otherwise the (x, y) passed to
     * {@link MeteringPointFactory#createPoint(float, float)} should be adjusted to this
     * coordinate system first.
     *
     * @param display        {@link Display} to get the orientation from. This should be the
     *                       current display where camera preview is showing.
     * @param cameraInfo     the information for the {@link Camera} to generate the metering
     *                       point for
     * @param width          the width of the coordinate which are mapped to the full camera preview
     *                       FOV in given display's orientation.
     * @param height         the height of the coordinate which are mapped to the full camera
     *                       preview
     *                       FOV in given display's orientation.
     */
    public DisplayOrientedMeteringPointFactory(@NonNull Display display,
            @NonNull CameraInfo cameraInfo, float width, float height) {
        mWidth = width;
        mHeight = height;
        mDisplay = display;
        mCameraInfo = cameraInfo;
    }

    @Nullable
    private Integer getLensFacing() {
        // This assumes CameraInfo is an instance of CameraInfoInternal which contains lens
        // facing information. A Camera may not be simply of a single lens facing type so that is
        // why it isn't exposed directly through CameraInfo.
        if (mCameraInfo instanceof CameraInfoInternal) {
            return ((CameraInfoInternal) mCameraInfo).getLensFacing();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    protected PointF convertPoint(float x, float y) {
        float width = mWidth;
        float height = mHeight;

        final Integer lensFacing = getLensFacing();
        boolean compensateForMirroring =
                (lensFacing != null && lensFacing == CameraSelector.LENS_FACING_FRONT);
        int relativeCameraOrientation = getRelativeCameraOrientation(compensateForMirroring);
        float outputX = x;
        float outputY = y;
        float outputWidth = width;
        float outputHeight = height;

        if (relativeCameraOrientation == 90 || relativeCameraOrientation == 270) {
            // We're horizontal. Swap width/height. Swap x/y.
            outputX = y;
            outputY = x;
            outputWidth = height;
            outputHeight = width;
        }

        switch (relativeCameraOrientation) {
            // Map to correct coordinates according to relativeCameraOrientation
            case 90:
                outputY = outputHeight - outputY;
                break;
            case 180:
                outputX = outputWidth - outputX;
                outputY = outputHeight - outputY;
                break;
            case 270:
                outputX = outputWidth - outputX;
                break;
            default:
                break;
        }

        // Swap x if it's a mirrored preview
        if (compensateForMirroring) {
            outputX = outputWidth - outputX;
        }

        // Normalized it to [0, 1]
        outputX = outputX / outputWidth;
        outputY = outputY / outputHeight;

        return new PointF(outputX, outputY);
    }

    private int getRelativeCameraOrientation(boolean compensateForMirroring) {
        int rotationDegrees;
        try {
            int displayRotation = mDisplay.getRotation();
            rotationDegrees = mCameraInfo.getSensorRotationDegrees(displayRotation);
            if (compensateForMirroring) {
                rotationDegrees = (360 - rotationDegrees) % 360;
            }
        } catch (Exception e) {
            rotationDegrees = 0;
        }
        return rotationDegrees;
    }
}
