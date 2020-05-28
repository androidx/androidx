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
     * @param outputRotationDegrees The output rotation of the viewport
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
        sensorIntersectionRect = getScaledRect(
                sensorIntersectionRect, rotatedViewPortAspectRatio, scaleType);

        // Map the max shared sensor rect to UseCase coordinates.
        Map<UseCase, Rect> useCaseOutputRects = new HashMap<>();
        RectF useCaseOutputRect = new RectF();
        Matrix sensorToUseCaseTransformation = new Matrix();
        for (Map.Entry<UseCase, Matrix> entry : useCaseToSensorTransformations.entrySet()) {
            // Transform the sensor crop rect to UseCase coordinates.
            entry.getValue().invert(sensorToUseCaseTransformation);
            sensorToUseCaseTransformation.mapRect(useCaseOutputRect, sensorIntersectionRect);
            correctOutputRectForRtl(layoutDirection, useCaseSizes.get(entry.getKey()),
                    useCaseOutputRect);
            Rect outputCropRect = new Rect();
            useCaseOutputRect.round(outputCropRect);
            useCaseOutputRects.put(entry.getKey(), outputCropRect);
        }
        return useCaseOutputRects;
    }

    /**
     * Flips and corrects the Rect for LTR layout direction.
     *
     * <p> The definition of "start" and "end" in {@link ViewPort.ScaleType} depends on
     * {@link LayoutDirection}. For {@link LayoutDirection#LTR}, the start is (0,0) and the
     * end is (width, height); for {@link LayoutDirection#RTL}, the start is (width, 0) and
     * the end is (0, height). The output rect needs to be transformed to match View properties.
     */
    static void correctOutputRectForRtl(
            @ViewPort.LayoutDirection int layoutDirection,
            Size size,
            RectF rect) {
        if (layoutDirection == LayoutDirection.LTR) {
            // No transformation needed for the default LTR.
            return;
        }

        // Flip based on the middle line of the Surface.
        Matrix rtlTransformation = new Matrix();
        // Create a transformation that mirror the Surface.
        rtlTransformation.setPolyToPoly(
                new float[]{0, 0, size.getWidth(), 0, size.getWidth(), size.getHeight(), 0,
                        size.getHeight()},
                0,
                new float[]{size.getWidth(), 0, 0, 0, 0, size.getHeight(), size.getWidth(),
                        size.getHeight()},
                0,
                4);
        rtlTransformation.mapRect(rect);

        // The order of the vertices are mirrored too. Rearrange them based on value.
        float newLeft = Math.min(rect.left, rect.right);
        float newRight = Math.max(rect.left, rect.right);
        float newTop = Math.min(rect.top, rect.bottom);
        float newBottom = Math.max(rect.top, rect.bottom);
        rect.set(newLeft, newTop, newRight, newBottom);
    }

    /**
     * Returns the scaled surface rect that fits/fills the given view port aspect ratio.
     *
     * <p> Scale type represents the transformation from surface to view port. For FIT types,
     * this method returns the smallest rectangle that is larger the surface; For FILL types,
     * returns the largest rectangle that is smaller than the view port. The returned rectangle
     * is also required to 1) have the view port's aspect ratio and 2) be in the surface
     * coordinates.
     */
    @SuppressLint("SwitchIntDef")
    @NonNull
    public static RectF getScaledRect(
            @NonNull RectF surfaceRect,
            @NonNull Rational viewPortAspectRatio,
            @ViewPort.ScaleType int scaleType) {
        Matrix viewPortToSurfaceTransformation = new Matrix();
        RectF viewPortRect = new RectF(0, 0, viewPortAspectRatio.getNumerator(),
                viewPortAspectRatio.getDenominator());
        if (scaleType == ViewPort.FIT_CENTER || scaleType == ViewPort.FIT_END
                || scaleType == ViewPort.FIT_START) {
            Matrix surfaceToViewPortTransformation = new Matrix();
            switch (scaleType) {
                // To workaround the limitation that Matrix doesn't not support FILL types
                // natively, use inverted backward FIT mapping to achieve forward FILL mapping.
                case ViewPort.FIT_CENTER:
                    surfaceToViewPortTransformation.setRectToRect(
                            surfaceRect, viewPortRect, Matrix.ScaleToFit.CENTER);
                    break;
                case ViewPort.FIT_START:
                    surfaceToViewPortTransformation.setRectToRect(
                            surfaceRect, viewPortRect, Matrix.ScaleToFit.START);
                    break;
                case ViewPort.FIT_END:
                    surfaceToViewPortTransformation.setRectToRect(
                            surfaceRect, viewPortRect, Matrix.ScaleToFit.END);
                    break;
            }
            surfaceToViewPortTransformation.invert(viewPortToSurfaceTransformation);
        } else if (scaleType == ViewPort.FILL_CENTER || scaleType == ViewPort.FILL_END
                || scaleType == ViewPort.FILL_START) {
            switch (scaleType) {
                case ViewPort.FILL_CENTER:
                    viewPortToSurfaceTransformation.setRectToRect(
                            viewPortRect, surfaceRect, Matrix.ScaleToFit.CENTER);
                    break;
                case ViewPort.FILL_START:
                    viewPortToSurfaceTransformation.setRectToRect(
                            viewPortRect, surfaceRect, Matrix.ScaleToFit.START);
                    break;
                case ViewPort.FILL_END:
                    viewPortToSurfaceTransformation.setRectToRect(
                            viewPortRect, surfaceRect, Matrix.ScaleToFit.END);
                    break;
            }
        } else {
            throw new IllegalStateException("Unexpected scale type: " + scaleType);
        }

        RectF viewPortRectInSurfaceCoordinates = new RectF();
        viewPortToSurfaceTransformation.mapRect(viewPortRectInSurfaceCoordinates, viewPortRect);
        return viewPortRectInSurfaceCoordinates;
    }
}
