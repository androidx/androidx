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

package androidx.window.extensions.embedding;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.window.extensions.RequiresVendorApiLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/** The attributes of the divider layout and behavior. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class DividerAttributes {

    /**
     * A divider type that draws a static line between the primary and secondary containers.
     */
    public static final int DIVIDER_TYPE_FIXED = 1;

    /**
     * A divider type that draws a line between the primary and secondary containers with a drag
     * handle that the user can drag and resize the containers.
     */
    public static final int DIVIDER_TYPE_DRAGGABLE = 2;

    @IntDef({DIVIDER_TYPE_FIXED, DIVIDER_TYPE_DRAGGABLE})
    @Retention(RetentionPolicy.SOURCE)
    @interface DividerType {
    }

    /**
     * A special value to indicate that the ratio is unset. System default value will be used.
     *
     * See {@link #getPrimaryMinRatio} and {@link #getPrimaryMaxRatio}.
     */
    public static final float RATIO_UNSET = -1.0f;

    /**
     * A special value to indicate that the width is unset. System default value will be used.
     *
     * See {@link #getWidthDp()}.
     */
    public static final int WIDTH_UNSET = -1;

    /** The {@link DividerType}. */
    private final @DividerType int mDividerType;

    /**
     * The divider width in dp. If equal to {@link #WIDTH_UNSET}, the system default value will
     * be used.
     */
    private final @Dimension int mWidthDp;

    /**
     * The min split ratio for the primary container. If equal to {@link #RATIO_UNSET}, the
     * system default value will be used. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}. See
     * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
     */
    private final float mPrimaryMinRatio;

    /**
     * The max split ratio for the primary container. If equal to {@link #RATIO_UNSET}, the
     * system default value will be used. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}. See
     * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
     */
    private final float mPrimaryMaxRatio;

    /**
     * Constructor of {@link DividerAttributes}.
     *
     * @param dividerType     the divider type. See {@link DividerType}.
     * @param widthDp         the width of the divider.
     * @param primaryMinRatio the min split ratio for the primary container.
     * @param primaryMaxRatio the max split ratio for the primary container.
     * @throws IllegalArgumentException if the provided values are invalid.
     */
    private DividerAttributes(
            @DividerType int dividerType,
            @Dimension int widthDp,
            float primaryMinRatio,
            float primaryMaxRatio) {
        if (dividerType == DIVIDER_TYPE_FIXED
                && (primaryMinRatio != RATIO_UNSET || primaryMaxRatio != RATIO_UNSET)) {
            throw new IllegalArgumentException(
                    "primaryMinRatio and primaryMaxRatio must be RATIO_UNSET for "
                            + "DIVIDER_TYPE_FIXED.");
        }
        if (primaryMinRatio != RATIO_UNSET && (primaryMinRatio >= 1.0 || primaryMinRatio < 0.0)) {
            throw new IllegalArgumentException(
                    "primaryMinRatio must be in [0.0, 1.0) or RATIO_UNSET.");
        }
        if (primaryMaxRatio != RATIO_UNSET && (primaryMaxRatio > 1.0 || primaryMaxRatio <= 0.0)) {
            throw new IllegalArgumentException(
                    "primaryMaxRatio must be in (0.0, 1.0] or RATIO_UNSET.");
        }
        if (primaryMinRatio != RATIO_UNSET && primaryMaxRatio != RATIO_UNSET
                && primaryMinRatio > primaryMaxRatio) {
            throw new IllegalArgumentException(
                    "primaryMinRatio must be less than or equal to primaryMaxRatio");
        }
        if (widthDp != WIDTH_UNSET && widthDp < 0) {
            throw new IllegalArgumentException(
                    "widthDp must be greater than or equal to 0 or WIDTH_UNSET.");
        }
        mDividerType = dividerType;
        mWidthDp = widthDp;
        mPrimaryMinRatio = primaryMinRatio;
        mPrimaryMaxRatio = primaryMaxRatio;
    }

    /** Returns the divider type. See {@link DividerType}. */
    @RequiresVendorApiLevel(level = 6)
    public @DividerType int getDividerType() {
        return mDividerType;
    }

    /**
     * Returns the width of the divider. If equal to {@link #WIDTH_UNSET}, the system default
     * value will be used.
     */
    @RequiresVendorApiLevel(level = 6)
    public @Dimension int getWidthDp() {
        return mWidthDp;
    }

    /**
     * Returns the min split ratio for the primary container. If equal to {@link #RATIO_UNSET},
     * the system default value will be used. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}. See
     * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
     */
    @RequiresVendorApiLevel(level = 6)
    public float getPrimaryMinRatio() {
        return mPrimaryMinRatio;
    }

    /**
     * Returns the max split ratio for the primary container. If equal to {@link #RATIO_UNSET},
     * the system default value will be used. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}. See
     * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
     */
    @RequiresVendorApiLevel(level = 6)
    public float getPrimaryMaxRatio() {
        return mPrimaryMaxRatio;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DividerAttributes)) return false;
        final DividerAttributes other = (DividerAttributes) obj;
        return mDividerType == other.mDividerType
                && mWidthDp == other.mWidthDp
                && mPrimaryMinRatio == other.mPrimaryMinRatio
                && mPrimaryMaxRatio == other.mPrimaryMaxRatio;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDividerType, mWidthDp, mPrimaryMinRatio, mPrimaryMaxRatio);
    }

    @NonNull
    @Override
    public String toString() {
        return DividerAttributes.class.getSimpleName() + "{"
                + "dividerType=" + mDividerType
                + ", width=" + mWidthDp
                + ", minPrimaryRatio=" + mPrimaryMinRatio
                + ", maxPrimaryRatio=" + mPrimaryMaxRatio
                + "}";
    }

    /** The {@link DividerAttributes} builder. */
    public static final class Builder {

        private final @DividerType int mDividerType;

        private @Dimension int mWidthDp = WIDTH_UNSET;

        private float mPrimaryMinRatio = RATIO_UNSET;

        private float mPrimaryMaxRatio = RATIO_UNSET;

        /**
         * The {@link DividerAttributes} builder constructor.
         *
         * @param dividerType the {@link DividerType}.
         */
        @RequiresVendorApiLevel(level = 6)
        public Builder(@DividerType int dividerType) {
            mDividerType = dividerType;
        }

        /**
         * The {@link DividerAttributes} builder constructor initialized by an existing
         * {@link DividerAttributes}.
         *
         * @param original the original {@link DividerAttributes} to initialize the {@link Builder}.
         */
        @RequiresVendorApiLevel(level = 6)
        public Builder(@NonNull DividerAttributes original) {
            Objects.requireNonNull(original);
            mDividerType = original.mDividerType;
            mWidthDp = original.mWidthDp;
            mPrimaryMinRatio = original.mPrimaryMinRatio;
            mPrimaryMaxRatio = original.mPrimaryMaxRatio;
        }

        /**
         * Sets the divider width. If equal to {@link #WIDTH_UNSET}, the system default value
         * will be used.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setWidthDp(@Dimension int widthDp) {
            mWidthDp = widthDp;
            return this;
        }

        /**
         * Sets the min split ratio for the primary container. If equal to {@link #RATIO_UNSET},
         * the system default value will be used. Will only be used when the divider type is
         * {@link #DIVIDER_TYPE_DRAGGABLE}. See
         * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
         *
         * @param primaryMinRatio the min ratio for the primary container. Must be in range
         *                        [0.0, 1.0) or {@link #RATIO_UNSET}.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setPrimaryMinRatio(float primaryMinRatio) {
            mPrimaryMinRatio = primaryMinRatio;
            return this;
        }

        /**
         * Sets the max split ratio for the primary container. If equal to {@link #RATIO_UNSET},
         * the system default value will be used. Will only be used when the divider type is
         * {@link #DIVIDER_TYPE_DRAGGABLE}. See
         * {@link SplitAttributes.SplitType.RatioSplitType#getRatio()}.
         *
         * @param primaryMaxRatio the max ratio for the primary container. Must be in range
         *                        (0.0, 1.0] or {@link #RATIO_UNSET}.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setPrimaryMaxRatio(float primaryMaxRatio) {
            mPrimaryMaxRatio = primaryMaxRatio;
            return this;
        }

        /**
         * Builds a {@link DividerAttributes} instance.
         *
         * @return a {@link DividerAttributes} instance.
         * @throws IllegalArgumentException if the provided values are invalid.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public DividerAttributes build() {
            return new DividerAttributes(mDividerType, mWidthDp, mPrimaryMinRatio,
                    mPrimaryMaxRatio);
        }
    }
}
