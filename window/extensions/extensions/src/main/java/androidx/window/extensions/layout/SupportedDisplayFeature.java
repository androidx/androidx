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

package androidx.window.extensions.layout;

import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.window.extensions.RequiresVendorApiLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * An abstract base class to represent a physical feature on a display that may intersect a window
 * in a meaningful way. The existence of a feature does not mean it will always be present, for
 * example a fold on a book like device that is fully closed. If a feature is present on the device
 * it should be reported through {@link WindowLayoutComponent#getSupportedDisplayFeatures()}. Use
 * {@link WindowLayoutComponent#addWindowLayoutInfoListener(Context, Consumer)} to observe which
 * features interact with a window.
 *
 * The possible feature list includes: [FoldDisplayFeature]
 */
public abstract class SupportedDisplayFeature {

    private SupportedDisplayFeature() { }

    /**
     * Represents a fold on a display that may intersect a window. The presence of a fold does not
     * imply that it intersects the window an {@link android.app.Activity} is running in. For
     * example, on a device that can fold like a book and has an outer screen, the fold should be
     * reported regardless of the folding state, or which screen is on to indicate that there may
     * be a fold when the user open the device.
     *
     * All folds support a flat state and a closed state. Not all devices support a half-opened
     * state. To check if a fold supports the half-opened state use
     * {@link ScreenFoldDisplayFeature#isHalfOpenedSupported()}.
     */
    public static final class ScreenFoldDisplayFeature extends SupportedDisplayFeature {

        /**
         * The type of fold is a physical hinge separating two displays.
         */
        public static final int TYPE_HINGE = 0;

        /**
         * The type of fold is a screen that folds from 0-180.
         */
        public static final int TYPE_SCREEN_FOLD_IN = 1;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {TYPE_HINGE, TYPE_SCREEN_FOLD_IN})
        public @interface FoldType { }

        @FoldType
        private final int mType;

        private final boolean mIsHalfOpenedSupported;

        /**
         * Creates an instance of [FoldDisplayFeature].
         * @param type the type of fold, either [FoldDisplayFeature.TYPE_HINGE] or
         *             [FoldDisplayFeature.TYPE_FOLDABLE_SCREEN]
         * @param isHalfOpenedSupported states if the fold feature supports half-opened.
         */
        ScreenFoldDisplayFeature(@FoldType int type, boolean isHalfOpenedSupported) {
            mType = type;
            mIsHalfOpenedSupported = isHalfOpenedSupported;
        }

        /**
         * Returns the type of fold that is either a hinge or a fold.
         */
        @RequiresVendorApiLevel(level = 6)
        @FoldType
        public int getType() {
            return mType;
        }

        /**
         * Returns the set of states supported by the {@link ScreenFoldDisplayFeature}.
         */
        @RequiresVendorApiLevel(level = 6)
        public boolean isHalfOpenedSupported() {
            return mIsHalfOpenedSupported;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScreenFoldDisplayFeature that = (ScreenFoldDisplayFeature) o;
            return mType == that.mType && mIsHalfOpenedSupported == that.mIsHalfOpenedSupported;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mType, mIsHalfOpenedSupported);
        }

        @Override
        @NonNull
        public String toString() {
            return "ScreenFoldDisplayFeature{"
                    + "mType=" + mType
                    + ", mIsHalfOpenedSupported=" + mIsHalfOpenedSupported
                    + '}';
        }

        /**
         * A builder to construct an instance of {@link ScreenFoldDisplayFeature}.
         */
        public static final class Builder {

            @FoldType
            private int mType;

            private boolean mIsHalfOpenedSupported;

            /**
             * Constructs a builder to create an instance of {@link ScreenFoldDisplayFeature}.
             *
             * @param type the type of hinge for the {@link ScreenFoldDisplayFeature}.
             * @param isHalfOpenedSupported if the fold feature supports half-opened mode.
             * @see ScreenFoldDisplayFeature.FoldType
             */
            @RequiresVendorApiLevel(level = 6)
            public Builder(@FoldType int type, boolean isHalfOpenedSupported) {
                mType = type;
                mIsHalfOpenedSupported = isHalfOpenedSupported;
            }

            /**
             * Returns an instance of {@link ScreenFoldDisplayFeature}.
             */
            @RequiresVendorApiLevel(level = 6)
            @NonNull
            public ScreenFoldDisplayFeature build() {
                return new ScreenFoldDisplayFeature(mType, mIsHalfOpenedSupported);
            }
        }
    }
}
