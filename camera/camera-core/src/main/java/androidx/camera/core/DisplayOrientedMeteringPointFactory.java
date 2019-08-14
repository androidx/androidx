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

import android.content.Context;
import android.graphics.PointF;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraX.LensFacing;

/**
 * A {@link MeteringPointFactory} that can create {@link MeteringPoint} by display oriented x, y.
 *
 * <p>This factory will consider the current display rotation and the lens facing to translate the
 * x/y correctly. Using this factory, apps do not need to handle the device rotation. They
 * can simply pass the x/y retrieved from their View. However if the camera preview is cropped,
 * scaled or rotated, it is apps' duty to transform the coordinates first.
 *
 * <p> The width/height of this factory is the logical width/height of the preview FoV and X/Y
 * is the logical XY inside the FOV. User can set the width and height to 1.0 which will make the
 * XY the normalized coordinates [0..1].
 */
public final class DisplayOrientedMeteringPointFactory extends MeteringPointFactory {
    /** The logical width of FoV in current display orientation */
    private final float mWidth;
    /** The logical height of FoV in current display orientation */
    private final float mHeight;
    /** Lens facing is required for correctly adjusted for front camera */
    private final LensFacing mLensFacing;
    /** {@link Display} used for detecting display orientation */
    @NonNull
    private final Display mDisplay;
    @NonNull
    private final CameraInfo mCameraInfo;

    /**
     * Creates the {@link MeteringPointFactory} with default display orientation.
     *
     * <p>The width/height is the logical width/height of the preview FoV and X/Y is the logical
     * XY inside the FOV. User can set the width and height to 1.0 which will make the XY the
     * normalized coordinates [0..1]. Or user can set the width/height to the View width/height and
     * then X/Y becomes the X/Y in the view.
     *
     * @param context    context to get the {@link WindowManager} for default display rotation.
     * @param lensFacing current lens facing.
     * @param width      the logical width of FoV in current display orientation.
     * @param height     the logical height of FoV in current display orientation.
     */
    public DisplayOrientedMeteringPointFactory(@NonNull Context context,
            @NonNull LensFacing lensFacing, float width, float height) {
        this(((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(),
                lensFacing, width, height);
    }

    /**
     * Creates the  {@link MeteringPointFactory}  with custom display orientation. This is used
     * in multi-display situation.
     *
     * <p>The width/height is the logical width/height of the preview FoV and X/Y is the logical
     * XY inside the FOV. User can set the width and height to 1.0 which will make the XY the
     * normalized coordinates [0..1]. Or user can set the width/height to the View width/height and
     * then X/Y becomes the X/Y in the view.
     * {@link Display} is used to dete
     * @param display    {@link Display} to get the orientation from.
     * @param lensFacing current lens facing.
     * @param width      the logical width of FoV in current display orientation.
     * @param height     the logical height of FoV in current display orientation.
     */
    public DisplayOrientedMeteringPointFactory(@NonNull Display display,
            @NonNull LensFacing lensFacing, float width, float height) {
        mWidth = width;
        mHeight = height;
        mLensFacing = lensFacing;
        mDisplay = display;
        try {
            String cameraId = CameraX.getCameraWithLensFacing(lensFacing);
            mCameraInfo = CameraX.getCameraInfo(cameraId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Can not find CameraInfo : " + lensFacing);
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected PointF translatePoint(float x, float y) {
        float width = mWidth;
        float height = mHeight;

        boolean compensateForMirroring = (mLensFacing == LensFacing.FRONT);
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
