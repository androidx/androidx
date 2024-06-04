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

/**
 * Layout settings for dual concurrent camera. It includes alpha value for blending,
 * offset in x, y coordinates, width and height. The offset, width and height are specified
 * in normalized device coordinates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LayoutSettings {

    public static final LayoutSettings DEFAULT = new Builder()
            .setAlpha(1.0f)
            .setOffsetX(0.0f)
            .setOffsetY(0.0f)
            .setWidth(1.0f)
            .setHeight(1.0f)
            .build();

    private final float mAlpha;
    private final float mOffsetX;
    private final float mOffsetY;
    private final float mWidth;
    private final float mHeight;

    private LayoutSettings(
            float alpha,
            float offsetX,
            float offsetY,
            float width,
            float height) {
        this.mAlpha = alpha;
        this.mOffsetX = offsetX;
        this.mOffsetY = offsetY;
        this.mWidth = width;
        this.mHeight = height;
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
     * Gets the offset X.
     *
     * @return offset X value.
     */
    public float getOffsetX() {
        return mOffsetX;
    }

    /**
     * Gets the offset Y.
     *
     * @return offset Y value.
     */
    public float getOffsetY() {
        return mOffsetY;
    }

    /**
     * Gets the width.
     *
     * @return width.
     */
    public float getWidth() {
        return mWidth;
    }

    /**
     * Gets the height.
     *
     * @return height.
     */
    public float getHeight() {
        return mHeight;
    }

    /** A builder for {@link LayoutSettings} instances. */
    public static final class Builder {
        private float mAlpha;
        private float mOffsetX;
        private float mOffsetY;
        private float mWidth;
        private float mHeight;

        /** Creates a new {@link Builder}. */
        public Builder() {
            mAlpha = 1.0f;
            mOffsetX = 0.0f;
            mOffsetY = 0.0f;
            mWidth = 0.0f;
            mHeight = 0.0f;
        }

        /**
         * Sets the alpha.
         *
         * @param alpha alpha value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setAlpha(@FloatRange(from = 0, to = 1) float alpha) {
            this.mAlpha = alpha;
            return this;
        }

        /**
         * Sets the offset X.
         *
         * @param offsetX offset X value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setOffsetX(@FloatRange(from = -1, to = 1) float offsetX) {
            this.mOffsetX = offsetX;
            return this;
        }

        /**
         * Sets the offset Y.
         *
         * @param offsetY offset Y value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setOffsetY(@FloatRange(from = -1, to = 1) float offsetY) {
            this.mOffsetY = offsetY;
            return this;
        }

        /**
         * Sets the width.
         *
         * @param width width value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setWidth(@FloatRange(from = -1, to = 1) float width) {
            this.mWidth = width;
            return this;
        }

        /**
         * Sets the height.
         *
         * @param height height value.
         * @return Builder instance.
         */
        @NonNull
        public Builder setHeight(@FloatRange(from = -1, to = 1) float height) {
            this.mHeight = height;
            return this;
        }

        /**
         * Builds the {@link LayoutSettings}.
         *
         * @return {@link LayoutSettings}.
         */
        @NonNull
        public LayoutSettings build() {
            return new LayoutSettings(
                    mAlpha,
                    mOffsetX,
                    mOffsetY,
                    mWidth,
                    mHeight);
        }
    }
}
