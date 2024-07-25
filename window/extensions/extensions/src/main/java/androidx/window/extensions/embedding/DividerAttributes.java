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

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.RequiresVendorApiLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * The attributes of the divider layout and behavior.
 *
 * @see SplitAttributes.Builder#setDividerAttributes(DividerAttributes)
 */
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
     * A special value to indicate that the ratio is unset. which means the system will choose a
     * default value based on the display size and form factor.
     *
     * @see #getPrimaryMinRatio()
     * @see #getPrimaryMaxRatio()
     */
    public static final float RATIO_SYSTEM_DEFAULT = -1.0f;

    /**
     * A special value to indicate that the width is unset. which means the system will choose a
     * default value based on the display size and form factor.
     *
     * @see #getWidthDp()
     */
    public static final int WIDTH_SYSTEM_DEFAULT = -1;

    /** The {@link DividerType}. */
    private final @DividerType int mDividerType;

    /**
     * The divider width in dp. It defaults to {@link #WIDTH_SYSTEM_DEFAULT}, which means the system
     * will choose a default value based on the display size and form factor.
     */
    private final @Dimension int mWidthDp;

    /**
     * The min split ratio for the primary container. It defaults to {@link #RATIO_SYSTEM_DEFAULT},
     * the system will choose a default value based on the display size and form factor. Will only
     * be used when the divider type is {@link #DIVIDER_TYPE_DRAGGABLE}.
     *
     * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
     */
    private final float mPrimaryMinRatio;

    /**
     * The max split ratio for the primary container. It defaults to {@link #RATIO_SYSTEM_DEFAULT},
     * the system will choose a default value based on the display size and form factor. Will only
     * be used when the divider type is {@link #DIVIDER_TYPE_DRAGGABLE}.
     *
     * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
     */
    private final float mPrimaryMaxRatio;

    /** The color of the divider. */
    private final @ColorInt int mDividerColor;

    /**
     * Constructor of {@link DividerAttributes}.
     *
     * @param dividerType                   the divider type. See {@link DividerType}.
     * @param widthDp                       the width of the divider.
     * @param primaryMinRatio               the min split ratio for the primary container.
     * @param primaryMaxRatio               the max split ratio for the primary container.
     * @param dividerColor                  the color of the divider.
     * @throws IllegalStateException if the provided values are invalid.
     */
    private DividerAttributes(
            @DividerType int dividerType,
            @Dimension int widthDp,
            float primaryMinRatio,
            float primaryMaxRatio,
            @ColorInt int dividerColor) {
        if (dividerType == DIVIDER_TYPE_FIXED
                && (primaryMinRatio != RATIO_SYSTEM_DEFAULT
                || primaryMaxRatio != RATIO_SYSTEM_DEFAULT)) {
            throw new IllegalStateException(
                    "primaryMinRatio and primaryMaxRatio must be RATIO_SYSTEM_DEFAULT for "
                            + "DIVIDER_TYPE_FIXED.");
        }
        if (primaryMinRatio != RATIO_SYSTEM_DEFAULT && primaryMaxRatio != RATIO_SYSTEM_DEFAULT
                && primaryMinRatio > primaryMaxRatio) {
            throw new IllegalStateException(
                    "primaryMinRatio must be less than or equal to primaryMaxRatio");
        }
        mDividerType = dividerType;
        mWidthDp = widthDp;
        mPrimaryMinRatio = primaryMinRatio;
        mPrimaryMaxRatio = primaryMaxRatio;
        mDividerColor = dividerColor;
    }

    /**
     * Returns the divider type.
     *
     * @see #DIVIDER_TYPE_FIXED
     * @see #DIVIDER_TYPE_DRAGGABLE
     */
    @RequiresVendorApiLevel(level = 6)
    public @DividerType int getDividerType() {
        return mDividerType;
    }

    /**
     * Returns the width of the divider. It defaults to {@link #WIDTH_SYSTEM_DEFAULT}, which means
     * the system will choose a default value based on the display size and form factor.
     */
    @RequiresVendorApiLevel(level = 6)
    public @Dimension int getWidthDp() {
        return mWidthDp;
    }

    /**
     * Returns the min split ratio for the primary container the divider can be dragged to. It
     * defaults to {@link #RATIO_SYSTEM_DEFAULT}, which means the system will choose a default value
     * based on the display size and form factor. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}.
     *
     * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
     */
    @RequiresVendorApiLevel(level = 6)
    public float getPrimaryMinRatio() {
        return mPrimaryMinRatio;
    }

    /**
     * Returns the max split ratio for the primary container the divider can be dragged to. It
     * defaults to {@link #RATIO_SYSTEM_DEFAULT}, which means the system will choose a default value
     * based on the display size and form factor. Will only be used when the divider type is
     * {@link #DIVIDER_TYPE_DRAGGABLE}.
     *
     * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
     */
    @RequiresVendorApiLevel(level = 6)
    public float getPrimaryMaxRatio() {
        return mPrimaryMaxRatio;
    }

    /** Returns the color of the divider. */
    @RequiresVendorApiLevel(level = 6)
    public @ColorInt int getDividerColor() {
        return mDividerColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DividerAttributes)) return false;
        final DividerAttributes other = (DividerAttributes) obj;
        return mDividerType == other.mDividerType
                && mWidthDp == other.mWidthDp
                && mPrimaryMinRatio == other.mPrimaryMinRatio
                && mPrimaryMaxRatio == other.mPrimaryMaxRatio
                && mDividerColor == other.mDividerColor;
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
                + ", dividerColor=" + mDividerColor
                + "}";
    }

    /** The {@link DividerAttributes} builder. */
    public static final class Builder {

        private final @DividerType int mDividerType;

        private @Dimension int mWidthDp = WIDTH_SYSTEM_DEFAULT;

        private float mPrimaryMinRatio = RATIO_SYSTEM_DEFAULT;

        private float mPrimaryMaxRatio = RATIO_SYSTEM_DEFAULT;

        private @ColorInt int mDividerColor = Color.BLACK;

        /**
         * The {@link DividerAttributes} builder constructor.
         *
         * @param dividerType the divider type, possible values are {@link #DIVIDER_TYPE_FIXED} and
         *                    {@link #DIVIDER_TYPE_DRAGGABLE}.
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
            mDividerColor = original.mDividerColor;
        }

        /**
         * Sets the divider width. It defaults to {@link #WIDTH_SYSTEM_DEFAULT}, which means the
         * system will choose a default value based on the display size and form factor.
         *
         * @throws IllegalArgumentException if the provided value is invalid.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setWidthDp(@Dimension int widthDp) {
            if (widthDp != WIDTH_SYSTEM_DEFAULT && widthDp < 0) {
                throw new IllegalArgumentException(
                        "widthDp must be greater than or equal to 0 or WIDTH_SYSTEM_DEFAULT.");
            }
            mWidthDp = widthDp;
            return this;
        }

        /**
         * Sets the min split ratio for the primary container. It defaults to
         * {@link #RATIO_SYSTEM_DEFAULT}, which means the system will choose a default value based
         * on the display size and form factor. Will only be used when the divider type is
         * {@link #DIVIDER_TYPE_DRAGGABLE}.
         *
         * @param primaryMinRatio the min ratio for the primary container. Must be in range
         *                        [0.0, 1.0) or {@link #RATIO_SYSTEM_DEFAULT}.
         * @throws IllegalArgumentException if the provided value is invalid.
         * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setPrimaryMinRatio(float primaryMinRatio) {
            if (primaryMinRatio != RATIO_SYSTEM_DEFAULT
                    && (primaryMinRatio >= 1.0 || primaryMinRatio < 0.0)) {
                throw new IllegalArgumentException(
                        "primaryMinRatio must be in [0.0, 1.0) or RATIO_SYSTEM_DEFAULT.");
            }
            mPrimaryMinRatio = primaryMinRatio;
            return this;
        }

        /**
         * Sets the max split ratio for the primary container. It defaults to
         * {@link #RATIO_SYSTEM_DEFAULT}, which means the system will choose a default value
         * based on the display size and form factor. Will only be used when the divider type is
         * {@link #DIVIDER_TYPE_DRAGGABLE}.
         *
         * @param primaryMaxRatio the max ratio for the primary container. Must be in range
         *                        (0.0, 1.0] or {@link #RATIO_SYSTEM_DEFAULT}.
         * @throws IllegalArgumentException if the provided value is invalid.
         * @see SplitAttributes.SplitType.RatioSplitType#getRatio()
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setPrimaryMaxRatio(float primaryMaxRatio) {
            if (primaryMaxRatio != RATIO_SYSTEM_DEFAULT
                    && (primaryMaxRatio > 1.0 || primaryMaxRatio <= 0.0)) {
                throw new IllegalArgumentException(
                        "primaryMaxRatio must be in (0.0, 1.0] or RATIO_SYSTEM_DEFAULT.");
            }
            mPrimaryMaxRatio = primaryMaxRatio;
            return this;
        }

        /**
         * Sets the color of the divider. If not set, the default color {@link Color#BLACK} is
         * used.
         */
        @RequiresVendorApiLevel(level = 6)
        @NonNull
        public Builder setDividerColor(@ColorInt int dividerColor) {
            mDividerColor = dividerColor;
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
                    mPrimaryMaxRatio, mDividerColor);
        }
    }
}
