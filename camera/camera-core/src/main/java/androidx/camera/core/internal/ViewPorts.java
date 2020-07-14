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

package androidx.camera.core.internal;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LayoutDirection;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.camera.core.UseCase;
import androidx.camera.core.ViewPort;
import androidx.camera.core.internal.utils.ImageUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for calculating viewports.
 */
public class ViewPorts {
    private ViewPorts() {

    }

    /**
     * Calculate a set of ViewPorts based on the combination of the camera, viewport, and use cases.
     *
     * @param fullSensorRect        The full size of the viewport.
     * @param viewPortAspectRatio   The aspect ratio of the viewport.
     * @param outputRotationDegrees Clockwise rotation to correct the surfaces to display
     *                              rotation.
     * @param scaleType             The scale type to calculate
     * @param layoutDirection       The direction of layout.
     * @param useCaseSizes          The resolutions of the UseCases
     * @return The set of Viewports that should be set for each UseCase
     */
    @NonNull
    public static Map<UseCase, Rect> calculateViewPortRects(
            @NonNull Rect fullSensorRect,
            @NonNull Rational viewPortAspectRatio,
            @IntRange(from = 0, to = 359) int outputRotationDegrees,
            @ViewPort.ScaleType int scaleType,
            @ViewPort.LayoutDirection int layoutDirection,
            @NonNull Map<UseCase, Size> useCaseSizes) {
        // Transform aspect ratio to sensor orientation. The the rest of the method is in sensor
        // orientation.
        Rational rotatedViewPortAspectRatio = ImageUtil.getRotatedAspectRatio(
                outputRotationDegrees, viewPortAspectRatio);
        RectF fullSensorRectF = new RectF(fullSensorRect);

        // Calculate the transformation for each UseCase.
        Map<UseCase, Matrix> useCaseToSensorTransformations = new HashMap<>();
        RectF sensorIntersectionRect = new RectF(fullSensorRect);
        for (Map.Entry<UseCase, Size> entry : useCaseSizes.entrySet()) {
            // Calculate the transformation from UseCase to sensor.
            Matrix useCaseToSensorTransformation = new Matrix();
            RectF srcRect = new RectF(0, 0, entry.getValue().getWidth(),
                    entry.getValue().getHeight());
            useCaseToSensorTransformation.setRectToRect(srcRect, fullSensorRectF,
                    Matrix.ScaleToFit.CENTER);
            useCaseToSensorTransformations.put(entry.getKey(), useCaseToSensorTransformation);

            // Calculate the UseCase intersection in sensor coordinates.
            RectF useCaseSensorRect = new RectF();
            useCaseToSensorTransformation.mapRect(useCaseSensorRect, srcRect);
            sensorIntersectionRect.intersect(useCaseSensorRect);
        }

        // Get the shared sensor rect by the given aspect ratio.
        RectF viewPortRect = getScaledRect(
                sensorIntersectionRect, rotatedViewPortAspectRatio, scaleType, layoutDirection,
                outputRotationDegrees);

        // Map the max shared sensor rect to UseCase coordinates.
        Map<UseCase, Rect> useCaseOutputRects = new HashMap<>();
        RectF useCaseOutputRect = new RectF();
        Matrix sensorToUseCaseTransformation = new Matrix();
        for (Map.Entry<UseCase, Matrix> entry : useCaseToSensorTransformations.entrySet()) {
            // Transform the sensor crop rect to UseCase coordinates.
            entry.getValue().invert(sensorToUseCaseTransformation);
            sensorToUseCaseTransformation.mapRect(useCaseOutputRect, viewPortRect);
            Rect outputCropRect = new Rect();
            useCaseOutputRect.round(outputCropRect);
            useCaseOutputRects.put(entry.getKey(), outputCropRect);
        }
        return useCaseOutputRects;
    }

    /**
     * Returns the container rect that the given rect fills.
     *
     * <p> For FILL types, returns the largest container rect that is smaller than the view port.
     * The returned rectangle is also required to 1) have the view port's aspect ratio and 2) be
     * in the surface coordinates.
     *
     * <p> For FIT, returns the largest possible rect shared by all use cases.
     */
    @SuppressLint("SwitchIntDef")
    @NonNull
    public static RectF getScaledRect(
            @NonNull RectF fittingRect,
            @NonNull Rational containerAspectRatio,
            @ViewPort.ScaleType int scaleType,
            @ViewPort.LayoutDirection int layoutDirection,
            @IntRange(from = 0, to = 359) int rotationDegrees) {
        if (scaleType == ViewPort.FIT) {
            // Return the fitting rect if the rect is full covered by the container.
            return fittingRect;
        }
        // Using Matrix' convenience methods fill the rect into the containing rect with given
        // aspect ratio.
        // NOTE: By using the Matrix#setRectToRect, we assume the "start" is always (0, 0) and
        // the "end" is always (w, h), which is NOT always true depending on rotation and layout
        // orientation. We need to correct the rect based on rotation and layout direction.
        Matrix viewPortToSurfaceTransformation = new Matrix();
        RectF viewPortRect = new RectF(0, 0, containerAspectRatio.getNumerator(),
                containerAspectRatio.getDenominator());
        switch (scaleType) {
            case ViewPort.FILL_CENTER:
                viewPortToSurfaceTransformation.setRectToRect(
                        viewPortRect, fittingRect, Matrix.ScaleToFit.CENTER);
                break;
            case ViewPort.FILL_START:
                viewPortToSurfaceTransformation.setRectToRect(
                        viewPortRect, fittingRect, Matrix.ScaleToFit.START);
                break;
            case ViewPort.FILL_END:
                viewPortToSurfaceTransformation.setRectToRect(
                        viewPortRect, fittingRect, Matrix.ScaleToFit.END);
                break;
            default:
                throw new IllegalStateException("Unexpected scale type: " + scaleType);
        }

        RectF viewPortRectInSurfaceCoordinates = new RectF();
        viewPortToSurfaceTransformation.mapRect(viewPortRectInSurfaceCoordinates, viewPortRect);

        // Correct the crop rect based on rotation and layout direction.
        return correctStartOrEnd(layoutDirection, rotationDegrees, fittingRect,
                viewPortRectInSurfaceCoordinates);
    }

    /**
     * Correct viewport based on rotation and layout direction.
     *
     * <p> Both rotation and layout direction change the definition of the "start" and "end" in
     * scale type. For rotation, since the value is clockwise rotation should be applied to the
     * output buffer, the start/end point should be rotated counterclockwisely. For RTL layout
     * direction, the start/end point should be mirrored based on the upright direction of the
     * image.
     */
    private static RectF correctStartOrEnd(@ViewPort.LayoutDirection int layoutDirection,
            @IntRange(from = 0, to = 359) int rotationDegrees,
            RectF containerRect,
            RectF cropRect) {
        // For each scenario there is an illustration of the output buffer without correction.
        // The arrow represents the opposite direction of gravity. The start/end point should
        // rotate counterclockwisely based on rotationDegrees, and mirror along the line of the
        // arrow if layout direction is RTL.

        //
        // Start +-----+
        //       |  ^  |
        //       +-----+  End
        //
        boolean ltrRotation0 = rotationDegrees == 0 && layoutDirection == LayoutDirection.LTR;
        //
        // Start +-----+     90°     +-----+ End  RTL  Start +-----+
        //       |  ^  |    ===>     |  <  |      ==>        |  <  |
        //       +-----+ End   Start +-----+                 +-----+ End
        //
        boolean rtlRotation90 = rotationDegrees == 90 && layoutDirection == LayoutDirection.RTL;
        if (ltrRotation0 || rtlRotation90) {
            return cropRect;
        }

        //
        // Start +-----+    RTL    +-----+ Start
        //       |  ^  |   ===>    |  ^  |
        //       +-----+ End   End +-----+
        //
        boolean rtlRotation0 = rotationDegrees == 0 && layoutDirection == LayoutDirection.RTL;
        //
        // Start +-----+   270°   +-----+ Start
        //       |  ^  |   ===>   |  >  |
        //       +-----+ End  End +-----+
        //
        boolean ltrRotation270 = rotationDegrees == 270 && layoutDirection == LayoutDirection.LTR;
        if (rtlRotation0 || ltrRotation270) {
            return flipHorizontally(cropRect, containerRect.centerX());
        }

        //
        // Start +-----+    90°     +-----+ End
        //       |  ^  |   ===>     |  <  |
        //       +-----+ End  Start +-----+
        //
        boolean ltrRotation90 = rotationDegrees == 90 && layoutDirection == LayoutDirection.LTR;
        //
        // Start +-----+   180°  End +-----+               +-----+ End
        //       |  ^  |   ===>      |  v  |     ==>       |  v  |
        //       +-----+ End         +-----+ Start   Start +-----+
        //
        boolean rtlRotation180 = rotationDegrees == 180 && layoutDirection == LayoutDirection.RTL;
        if (ltrRotation90 || rtlRotation180) {
            return flipVertically(cropRect, containerRect.centerY());
        }

        //
        // Start +-----+   180°  End +-----+
        //       |  ^  |   ===>      |  v  |
        //       +-----+ End         +-----+ Start
        //
        boolean ltrRotation180 = rotationDegrees == 180 && layoutDirection == LayoutDirection.LTR;
        //
        // Start +-----+   270°   +-----+ Start  RTL  End +-----+
        //       |  ^  |   ===>   |  >  |        ==>      |  >  |
        //       +-----+ End  End +-----+                 +-----+ Start
        //
        boolean rtlRotation270 = rotationDegrees == 270 && layoutDirection == LayoutDirection.RTL;
        if (ltrRotation180 || rtlRotation270) {
            return flipHorizontally(flipVertically(cropRect, containerRect.centerY()),
                    containerRect.centerX());
        }

        throw new IllegalArgumentException("Invalid argument: direction" + layoutDirection + " "
                + "rotation " + rotationDegrees);
    }

    private static RectF flipHorizontally(RectF original, float flipLineX) {
        return new RectF(
                flipX(original.right, flipLineX),
                original.top,
                flipX(original.left, flipLineX),
                original.bottom);
    }

    private static RectF flipVertically(RectF original, float flipLineY) {
        return new RectF(
                original.left,
                flipY(original.bottom, flipLineY),
                original.right,
                flipY(original.top, flipLineY));
    }

    private static float flipX(float x, float flipLineX) {
        return flipLineX + flipLineX - x;
    }

    private static float flipY(float y, float flipLineY) {
        return flipLineY + flipLineY - y;
    }
}
