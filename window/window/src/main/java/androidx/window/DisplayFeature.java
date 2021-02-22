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

package androidx.window;

import android.graphics.Rect;

import androidx.annotation.NonNull;

/**
 * Description of a physical feature on the display.
 *
 * <p>A display feature is a distinctive physical attribute located within the display panel of
 * the device. It can intrude into the application window space and create a visual distortion,
 * visual or touch discontinuity, make some area invisible or create a logical divider or separation
 * in the screen space.
 */
public interface DisplayFeature {

    /**
     * The bounding rectangle of the feature within the application window
     * in the window coordinate space.
     *
     * @return bounds of display feature.
     */
    @NonNull
    Rect getBounds();

    /**
     * @deprecated Will be removed in the next alpha. Cast to a {@link FoldingFeature}.
     *
     * @return TYPE_FOLD or TYPE_HINGE depending on the feature returned.
     */
    @Deprecated
    int getType();

    /**
     * @deprecated Will be removed in the next alpha. See {@link FoldingFeature}.
     */
    @Deprecated
    int TYPE_FOLD = 1;

    /**
     * @deprecated Will be removed in the next alpha. See {@link FoldingFeature}
     */
    @Deprecated
    int TYPE_HINGE = 2;

    /**
     * @deprecated Will be removed in the next alpha.
     */
    @Deprecated
    class Builder {
        private Rect mBounds = new Rect();
        private int mType = 0;

        /**
         * Update the bounds in the builder.
         *
         * @param bounds for the {@link DisplayFeature}
         * @return {@code this} with the bounds updated.
         */
        @NonNull
        public Builder setBounds(@NonNull Rect bounds) {
            mBounds = bounds;
            return this;
        }

        /**
         * Update the type in the builder.
         *
         * @param type for the {@link DisplayFeature}
         * @return {@code this} with the type updated.
         */
        @NonNull
        public Builder setType(int type) {
            mType = type;
            return this;
        }

        /**
         * @return {@link DisplayFeature} with the bounds and type from the builder.
         */
        @NonNull
        public DisplayFeature build() {
            return new DisplayFeatureCompat(mBounds, mType);
        }
    }
}
