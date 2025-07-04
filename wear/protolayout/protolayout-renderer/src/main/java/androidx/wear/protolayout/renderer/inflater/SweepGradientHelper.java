/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.PropHelpers.handleProp;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;

import androidx.annotation.ColorInt;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.proto.ColorProto;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline.PipelineMaker;
import androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.CapPosition;

import com.google.common.primitives.Floats;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Helper class for dealing with dynamic sweep gradients. */
class SweepGradientHelper {

    private class AngularColorStop {
        float offset;
        @ColorInt int color;

        AngularColorStop(float offset, int color) {
            this.offset = offset;
            this.color = color;
        }

        void updateColor(@ColorInt int color) {
            this.color = makeOpaque(color);
            invalidateCallback.run();
        }

        void updateOffset(float offset) {
            this.offset = offset;
            invalidateCallback.run();
        }

        float getAngle() {
            return definitionStartAngle + offset * (definitionEndAngle - definitionStartAngle);
        }
    }

    private static final int MIN_COLOR_STOPS = 2;
    private static final int MAX_COLOR_STOPS = 10;

    /**
     * The size of the sector with a constant color, equivalent to 90 degrees, where the cap is
     * drawn. It's used to ensure that the cap is drawn with the adjacent color in the line.
     */
    private static final float CAP_COLOR_SHADER_OFFSET_SIZE = 0.25f;

    private final @NonNull List<AngularColorStop> colorStops;

    private float definitionStartAngle;
    private float definitionEndAngle;

    private final Runnable invalidateCallback;

    /**
     * Creates a SweepGradientHelper instance.
     *
     * <p>All colors will have their alpha channel set to 0xFF (opaque).
     *
     * <p>Dynamic data will be registered to the pipeline when initializing.
     *
     * <p>The input color stops will be sorted based on their initial offset if all color stops are
     * static.
     */
    public static @NonNull SweepGradientHelper create(
            ColorProto.@NonNull SweepGradient sweepGradProto,
            @NonNull String posId,
            Optional<PipelineMaker> pipelineMaker,
            @NonNull Runnable invalidateCallback) {
        SweepGradientHelper instance = new SweepGradientHelper(sweepGradProto, invalidateCallback);
        for (int i = 0; i < sweepGradProto.getColorStopsCount(); i++) {
            ColorProto.ColorStop colorStop = sweepGradProto.getColorStops(i);
            final AngularColorStop angularColorStop = instance.colorStops.get(i);
            handleProp(colorStop.getColor(), angularColorStop::updateColor, posId, pipelineMaker);
            if (colorStop.hasOffset()) {
                handleProp(
                        colorStop.getOffset(),
                        angularColorStop::updateOffset,
                        posId,
                        pipelineMaker);
            }
        }

        boolean allStatic =
                sweepGradProto.getColorStopsList().stream()
                        .noneMatch(
                                stop ->
                                        stop.getColor().hasDynamicValue()
                                                || stop.getOffset().hasDynamicValue());
        if (allStatic && sweepGradProto.getColorStops(0).hasOffset()) {
            instance.colorStops.sort((a, b) -> Float.compare(a.offset, b.offset));
        }

        if (sweepGradProto.hasStartAngle()) {
            handleProp(
                    sweepGradProto.getStartAngle(),
                    instance::updateStartAngle,
                    posId,
                    pipelineMaker);
        }
        if (sweepGradProto.hasEndAngle()) {
            handleProp(
                    sweepGradProto.getEndAngle(), instance::updateEndAngle, posId, pipelineMaker);
        }

        return instance;
    }

    private SweepGradientHelper(
            ColorProto.@NonNull SweepGradient sweepGradProto,
            @NonNull Runnable invalidateCallback) {
        this.invalidateCallback = invalidateCallback;
        int numColors = sweepGradProto.getColorStopsCount();
        if (numColors < MIN_COLOR_STOPS || numColors > MAX_COLOR_STOPS) {
            throw new IllegalArgumentException(
                    "SweepGradient color count must be >= "
                            + MIN_COLOR_STOPS
                            + "and <= "
                            + MAX_COLOR_STOPS);
        }

        definitionStartAngle = sweepGradProto.getStartAngle().getValue();
        definitionEndAngle =
                sweepGradProto.getEndAngle().hasValue()
                        ? sweepGradProto.getEndAngle().getValue()
                        : 360f;

        // Use the first color stop to check for offsets to be present or absent.
        boolean offsetsRequired = sweepGradProto.getColorStops(0).hasOffset();

        colorStops = new ArrayList<>(numColors);
        for (int i = 0; i < numColors; i++) {
            ColorProto.ColorStop stop = sweepGradProto.getColorStops(i);
            if (offsetsRequired ^ stop.hasOffset()) {
                throw new IllegalArgumentException(
                        "Either all or none of the color stops should contain an offset.");
            }
            float offset =
                    stop.hasOffset() ? stop.getOffset().getValue() : (float) i / (numColors - 1);
            colorStops.add(new AngularColorStop(offset, makeOpaque(stop.getColor().getArgb())));
        }
    }

    private void updateStartAngle(float angle) {
        definitionStartAngle = angle;
        invalidateCallback.run();
    }

    private void updateEndAngle(float angle) {
        definitionEndAngle = angle;
        invalidateCallback.run();
    }

    /**
     * Interpolates colors linearly. Color interpolation needs to be done accordingly to the
     * underlying SweepGradient shader implementation so that all color transitions are smooth and
     * static.
     *
     * <p>The ArgbEvaluator class applies gamma correction to colors which results in a different
     * behavior compared to the shader's native implementation.
     */
    @ColorInt
    @VisibleForTesting
    int interpolateColors(
            int startColor, float startAngle, int endColor, float endAngle, float targetAngle) {
        if (startAngle == endAngle) {
            return startColor;
        }
        float fraction = (targetAngle - startAngle) / (endAngle - startAngle);
        if (Float.isInfinite(fraction)) {
            return startColor;
        }

        float startA = Color.alpha(startColor);
        float startR = Color.red(startColor);
        float startG = Color.green(startColor);
        float startB = Color.blue(startColor);

        float endA = Color.alpha(endColor);
        float endR = Color.red(endColor);
        float endG = Color.green(endColor);
        float endB = Color.blue(endColor);

        int a = (int) (startA + fraction * (endA - startA));
        int r = (int) (startR + fraction * (endR - startR));
        int g = (int) (startG + fraction * (endG - startG));
        int b = (int) (startB + fraction * (endB - startB));
        return Color.argb(a, r, g, b);
    }

    /**
     * Gets the color for a specific angle in the gradient. The color is an interpolation between
     * the color stops adjacent to the given {@code angle}.
     */
    @ColorInt
    int getColor(float angle) {
        for (int i = 0; i < colorStops.size(); i++) {
            float stopAngle = colorStops.get(i).getAngle();
            if (stopAngle >= angle) {
                int stopColor = colorStops.get(i).color;
                if (i == 0) {
                    return stopColor;
                }
                float prevAngle = colorStops.get(i - 1).getAngle();
                int prevColor = colorStops.get(i - 1).color;
                return interpolateColors(prevColor, prevAngle, stopColor, stopAngle, angle);
            }
        }

        // If no color was returned till here, return the last color in the gradient.
        return colorStops.get(colorStops.size() - 1).color;
    }

    /**
     * Gets a SweepGradient Shader object using colors present between gradStartAngle and
     * gradEndAngle, which are angles corresponding to the Brush proto definition. The rotationAngle
     * is applied to the generated Shader object.
     *
     * @param bounds the bounds of the drawing area for the arc
     * @param gradStartAngle the start angle position in the gradient, defining to the start color
     * @param gradEndAngle the end angle position in the gradient, defining to the end color
     * @param rotationAngle the angle to rotate the shader, defining the position of the start color
     * @param capPosition the position of the stroke cap.
     */
    @NonNull Shader getShader(
            @NonNull RectF bounds,
            float gradStartAngle,
            float gradEndAngle,
            float rotationAngle,
            CapPosition capPosition) {
        if (Math.abs(gradEndAngle - gradStartAngle) > 360f) {
            throw new IllegalArgumentException(
                    "Start and End angles must span at most 360 degrees");
        }

        boolean isClockwise = gradEndAngle >= gradStartAngle;
        if (!isClockwise) {
            gradStartAngle = Math.abs(gradStartAngle);
            gradEndAngle = Math.abs(gradEndAngle);
        }

        List<Integer> colors = new ArrayList<>();
        List<Float> offsets = new ArrayList<>();

        // Start Color
        int startColor = getColor(gradStartAngle);
        colors.add(startColor);
        offsets.add(0f);

        // Colors within range.
        for (int i = 0; i < colorStops.size(); i++) {
            float stopAngle = colorStops.get(i).getAngle();
            if (stopAngle <= gradStartAngle) {
                continue;
            }
            if (stopAngle >= gradEndAngle) {
                break;
            }

            colors.add(colorStops.get(i).color);
            offsets.add((stopAngle - gradStartAngle) / 360f);
        }

        // End Color
        int endColor = getColor(gradEndAngle);
        float endOffset = (gradEndAngle - gradStartAngle) / 360f;
        colors.add(endColor);
        offsets.add(endOffset);

        // Draw the Cap with a solid color.
        // The Cap must have the same color as its adjacent position in the ArcLine. So a new color
        // stop is added to the Shader to make sure the Cap region has a single color.
        if (capPosition == CapPosition.START) {
            colors.add(startColor);
            offsets.add(1f - CAP_COLOR_SHADER_OFFSET_SIZE);
        } else if (capPosition == CapPosition.END) {
            colors.add(endColor);
            offsets.add(endOffset + CAP_COLOR_SHADER_OFFSET_SIZE);
        }

        // Invert gradient if angle span is counter-clockwise.
        if (!isClockwise) {
            offsets.replaceAll(o -> 1f - o);
            Collections.reverse(offsets);
            Collections.reverse(colors);
        }

        float centerX = (bounds.left + bounds.right) / 2f;
        float centerY = (bounds.top + bounds.bottom) / 2f;
        SweepGradient shader =
                new SweepGradient(
                        centerX,
                        centerY,
                        colors.stream().mapToInt(Integer::intValue).toArray(),
                        Floats.toArray(offsets));
        Matrix matrix = new Matrix();
        matrix.setRotate(rotationAngle, centerX, centerY);
        shader.setLocalMatrix(matrix);
        return shader;
    }

    private static final int FULLY_OPAQUE_COLOR_MASK = 0xFF000000;

    /** Changes the alpha channel of the color to 0xFF (fully opaque). */
    static int makeOpaque(int color) {
        return color | FULLY_OPAQUE_COLOR_MASK;
    }
}
