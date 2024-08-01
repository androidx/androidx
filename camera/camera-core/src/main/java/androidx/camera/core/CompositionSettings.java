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

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;

/**
 * Composition settings for dual concurrent camera. It includes alpha value for blending,
 * offset in x, y coordinates, scale of width and height. The offset, width and height are specified
 * in normalized device coordinates.
 *
 * @see <a href="https://learnopengl.com/Getting-started/Coordinate-Systems">Normalized Device Coordinates</a>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CompositionSettings {

    public static final CompositionSettings DEFAULT = new Builder()
            .setAlpha(1.0f)
            .setOffset(0.0f, 0.0f)
            .setScale(1.0f, 1.0f)
            .build();

    private final float mAlpha;
    private final Pair<Float, Float> mOffset;
    private final Pair<Float, Float> mScale;

    private CompositionSettings(
            float alpha,
            Pair<Float, Float> offset,
            Pair<Float, Float> scale) {
        mAlpha = alpha;
        mOffset = offset;
        mScale = scale;
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
    @NonNull
    public Pair<Float, Float> getOffset() {
        return mOffset;
    }

    /**
     * Gets the scale. Negative value means mirroring in X or Y direction.
     *
     * @return scale value.
     */
    @NonNull
    public Pair<Float, Float> getScale() {
        return mScale;
    }

    /** A builder for {@link CompositionSettings} instances. */
    public static final class Builder {
        private float mAlpha;
        private Pair<Float, Float> mOffset;
        private Pair<Float, Float> mScale;

        /** Creates a new {@link Builder}. */
        public Builder() {
            mAlpha = 1.0f;
            mOffset = Pair.create(0.0f, 0.0f);
            mScale = Pair.create(1.0f, 1.0f);
        }

        /**
         * Sets the alpha.
         *
         * @param alpha alpha value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setAlpha(@FloatRange(from = 0, to = 1) float alpha) {
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
        @NonNull
        public Builder setOffset(
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
        @NonNull
        public Builder setScale(
                @FloatRange(from = -1, to = 1) float scaleX,
                @FloatRange(from = -1, to = 1) float scaleY) {
            mScale = Pair.create(scaleX, scaleY);
            return this;
        }

        /**
         * Builds the {@link CompositionSettings}.
         *
         * @return {@link CompositionSettings}.
         */
        @NonNull
        public CompositionSettings build() {
            return new CompositionSettings(
                    mAlpha,
                    mOffset,
                    mScale);
        }
    }
}
