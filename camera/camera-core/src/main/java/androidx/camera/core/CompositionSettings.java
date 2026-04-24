/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig;
import androidx.core.util.Pair;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Composition settings for dual concurrent camera. It includes alpha value for blending,
 * offset in x, y coordinates, scale of width and height, and styling options like rounded corners
 * and borders. The offset, and scale of width and height are specified in normalized device
 * coordinates(NDCs). The offset is applied after scale.
 * The origin of normalized device coordinates is at the center of the viewing volume. The positive
 * X-axis extends to the right, the positive Y-axis extends upwards.The x, y values range from -1
 * to 1. E.g. scale with {@code (0.5f, 0.5f)} and offset with {@code (0.5f, 0.5f)} is the
 * bottom-right quadrant of the output device.
 *
 * <p>Styling options like rounded corner ratio and border width ratio are normalized from 0 to 1,
 * where a ratio of 1 is relative to half of the smaller dimension of the frame.
 *
 * <p>Composited dual camera frames preview and recording can be supported using
 * {@link CompositionSettings} and {@link SingleCameraConfig}. The z-order of composition is
 * determined by the order of camera configs to bind. Currently the background color will be black
 * by default. The resolution of camera frames for preview and recording will be determined by
 * resolution selection strategy configured for each use case and the scale of width and height set
 * in {@link CompositionSettings}, so it is recommended to use 16:9 aspect ratio strategy for
 * preview if 16:9 quality selector is configured for video capture. The mirroring and rotation of
 * the camera frame will be applied after composition because both cameras are using the same use
 * cases.
 *
 * <p>The following code snippet demonstrates how to display in Picture-in-Picture mode:
 * <pre>
 *                        16
 *         --------------------------------
 *         |               c0             |
 *         |                              |
 *         |                              |
 *         |                              |
 *         |  ---------                   |  9
 *         |  |       |                   |
 *         |  |   c1  |                   |
 *         |  |       |                   |
 *         |  ---------                   |
 *         --------------------------------
 *         c0: primary camera
 *         c1: secondary camera
 *     {@code
 *         ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
 *                 .setAspectRatioStrategy(
 *                         AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
 *                 .build();
 *         Preview preview = new Preview.Builder()
 *                 .setResolutionSelector(resolutionSelector)
 *                 .build();
 *         preview.setSurfaceProvider(mSinglePreviewView.getSurfaceProvider());
 *         UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
 *                 .addUseCase(preview)
 *                 .addUseCase(mVideoCapture)
 *                 .build();
 *         SingleCameraConfig primary = new SingleCameraConfig(
 *                 cameraSelectorPrimary,
 *                 useCaseGroup,
 *                 new CompositionSettings.Builder()
 *                         .setAlpha(1.0f)
 *                         .setOffset(0.0f, 0.0f)
 *                         .setScale(1.0f, 1.0f)
 *                         .build(),
 *                 lifecycleOwner);
 *         SingleCameraConfig secondary = new SingleCameraConfig(
 *                 cameraSelectorSecondary,
 *                 useCaseGroup,
 *                 new CompositionSettings.Builder()
 *                         .setAlpha(1.0f)
 *                         .setOffset(-0.3f, -0.4f)
 *                         .setScale(0.3f, 0.3f)
 *                         .build(),
 *                 lifecycleOwner);
 *         cameraProvider.bindToLifecycle(ImmutableList.of(primary, secondary));
 * }</pre>
 */
public class CompositionSettings {

    /**
     * Default composition settings, which will display in full screen with no offset and scale.
     */
    public static final CompositionSettings DEFAULT = new Builder()
            .setAlpha(1.0f)
            .setOffset(0.0f, 0.0f)
            .setScale(1.0f, 1.0f)
            .setRoundedCornerRatio(0.0f)
            .setBorderWidthRatio(0.0f)
            .setBorderColor(Color.WHITE)
            .setZOrder(0)
            .build();

    private final float mAlpha;
    private final Pair<Float, Float> mOffset;
    private final Pair<Float, Float> mScale;
    private final float mRoundedCornerRatio;
    private final float mBorderWidthRatio;
    private final int mBorderColor;
    private final int mZOrder;

    private CompositionSettings(
            float alpha,
            Pair<Float, Float> offset,
            Pair<Float, Float> scale,
            float roundedCornerRatio,
            float borderWidthRatio,
            int borderColor,
            int zOrder) {
        mAlpha = alpha;
        mOffset = offset;
        mScale = scale;
        mRoundedCornerRatio = roundedCornerRatio;
        mBorderWidthRatio = borderWidthRatio;
        mBorderColor = borderColor;
        mZOrder = zOrder;
    }

    /**
     * Gets the alpha.
     *
     * @return alpha value.
     */
    public float getAlpha() {
        return mAlpha;
    }

    /**
     * Gets the offset.
     *
     * @return offset value.
     */
    public @NonNull Pair<Float, Float> getOffset() {
        return mOffset;
    }

    /**
     * Gets the scale. Negative value means mirroring in X or Y direction.
     *
     * @return scale value.
     */
    public @NonNull Pair<Float, Float> getScale() {
        return mScale;
    }

    /**
     * Gets the rounded corner ratio.
     *
     * <p>The value is normalized from 0 to 1. 0 means no rounding (sharp corners). 1 means fully
     * rounded (the radius of the rounded corners equals half of the smaller dimension of the
     * frame, making the frame a circle if it's square, or pill-shaped if it's rectangular).
     *
     * @return rounded corner ratio value.
     */
    public @FloatRange(from = 0, to = 1) float getRoundedCornerRatio() {
        return mRoundedCornerRatio;
    }

    /**
     * Gets the border width as a normalized ratio from 0 to 1.
     *
     * <p>0 means no border. 1 means the border width equals half of the smaller dimension of
     * the frame, which causes the borders to meet at the center and fill the smaller dimension.
     * The ratio is relative to half of the smaller dimension of the frame, consistent with
     * {@link #getRoundedCornerRatio()}.
     *
     * @return border width ratio value.
     */
    public @FloatRange(from = 0, to = 1) float getBorderWidthRatio() {
        return mBorderWidthRatio;
    }

    /**
     * Gets the border color.
     *
     * @return border color value.
     */
    public @ColorInt int getBorderColor() {
        return mBorderColor;
    }

    /**
     * Gets the z-order.
     *
     * @return z-order value.
     */
    public @IntRange(from = 0) int getZOrder() {
        return mZOrder;
    }

    /** A builder for {@link CompositionSettings} instances. */
    public static final class Builder {
        private float mAlpha;
        private Pair<Float, Float> mOffset;
        private Pair<Float, Float> mScale;
        private float mRoundedCornerRatio;
        private float mBorderWidthRatio;
        private int mBorderColor;
        private int mZOrder;

        /**
         * Creates a new {@link Builder}.
         *
         * <p>The default alpha is 1.0f, the default offset is (0.0f, 0.0f), the default scale is
         * (1.0f, 1.0f), the default rounded corner ratio is 0.0f, the default border width ratio
         * is 0, and the default border color is {@link Color#WHITE}.
         */
        public Builder() {
            mAlpha = 1.0f;
            mOffset = Pair.create(0.0f, 0.0f);
            mScale = Pair.create(1.0f, 1.0f);
            mRoundedCornerRatio = 0.0f;
            mBorderWidthRatio = 0.0f;
            mBorderColor = Color.WHITE;
            mZOrder = 0;
        }

        /**
         * Sets the alpha. 0 means fully transparent, 1 means fully opaque.
         *
         * @param alpha alpha value.
         * @return Builder instance.
         */
        public @NonNull Builder setAlpha(@FloatRange(from = 0, to = 1) float alpha) {
            mAlpha = alpha;
            return this;
        }

        /**
         * Sets the offset.
         *
         * @param offsetX offset X value.
         * @param offsetY offset Y value.
         * @return Builder instance.
         */
        public @NonNull Builder setOffset(
                @FloatRange(from = -1, to = 1) float offsetX,
                @FloatRange(from = -1, to = 1) float offsetY) {
            mOffset = Pair.create(offsetX, offsetY);
            return this;
        }

        /**
         * Sets the scale.
         *
         * @param scaleX scale X value.
         * @param scaleY scale Y value.
         * @return Builder instance.
         */
        public @NonNull Builder setScale(float scaleX, float scaleY) {
            mScale = Pair.create(scaleX, scaleY);
            return this;
        }

        /**
         * Sets the rounded corner ratio as a normalized value from 0 to 1.
         *
         * <p>0 means no rounding (sharp corners). 1 means fully rounded (the radius of the rounded
         * corners equals half of the smaller dimension of the frame, making the frame a circle
         * if it's square, or pill-shaped if it's rectangular). Values in between provide partial
         * rounding proportional to half of the smaller dimension of the frame.
         *
         * @param roundedCornerRatio rounded corner ratio value.
         * @return Builder instance.
         */
        public @NonNull Builder setRoundedCornerRatio(
                @FloatRange(from = 0, to = 1) float roundedCornerRatio) {
            mRoundedCornerRatio = roundedCornerRatio;
            return this;
        }

        /**
         * Sets the border width as a normalized ratio from 0 to 1.
         *
         * <p>0 means no border. 1 means the border width equals half of the smaller dimension of
         * the frame, which causes the borders to meet at the center and fill the smaller dimension.
         * The ratio is relative to half of the smaller dimension of the frame, consistent with
         * {@link #setRoundedCornerRatio(float)}.
         *
         * @param borderWidthRatio border width ratio value.
         * @return Builder instance.
         */
        public @NonNull Builder setBorderWidthRatio(
                @FloatRange(from = 0, to = 1) float borderWidthRatio) {
            mBorderWidthRatio = borderWidthRatio;
            return this;
        }

        /**
         * Sets the border color.
         *
         * @param borderColor border color value.
         * @return Builder instance.
         */
        public @NonNull Builder setBorderColor(@ColorInt int borderColor) {
            mBorderColor = borderColor;
            return this;
        }

        /**
         * Sets the z-order. Larger z-order means rendered later (appears on top). Must be
         * non-negative.
         *
         * @param zOrder z-order value.
         * @return Builder instance.
         */
        public @NonNull Builder setZOrder(@IntRange(from = 0) int zOrder) {
            Preconditions.checkArgument(zOrder >= 0, "z-order must be non-negative.");
            mZOrder = zOrder;
            return this;
        }

        /**
         * Builds the {@link CompositionSettings}.
         *
         * @return {@link CompositionSettings}.
         */
        public @NonNull CompositionSettings build() {
            return new CompositionSettings(
                    mAlpha,
                    mOffset,
                    mScale,
                    mRoundedCornerRatio,
                    mBorderWidthRatio,
                    mBorderColor,
                    mZOrder);
        }
    }
}
