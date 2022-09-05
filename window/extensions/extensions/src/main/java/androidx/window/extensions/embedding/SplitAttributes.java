/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.SuppressLint;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.window.extensions.layout.FoldingFeature;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Attributes to describe how the task bounds are split, which includes information on how the task
 * bounds are split between the activity containers:
 * <ul>
 *   <li>Layout directions</li>
 *   <li>Whether the task bounds are split vertically horizontally</li>
 *   <li>The position of the primary and the secondary activity containers</li>
 * </ul>
 * Attributes can be configured in the following ways:
 * <ul>
 *   <li> Set the default {@code SplitAttributes} via
 *     {@link SplitPairRule.Builder#setDefaultSplitAttributes(SplitAttributes)} and
 *     {@link SplitPlaceholderRule.Builder#setDefaultSplitAttributes(SplitAttributes)} </li>
 *   <li> Used in {@link SplitAttributesCalculator#computeSplitAttributesForParams(
 *     SplitAttributesCalculator.SplitAttributesCalculatorParams)} to customize the
 *     {@link SplitAttributes} for a given device and window state. </li>
 * </ul>
 *
 * @see SplitAttributes#getSplitType()
 * @see SplitType
 * @see LayoutDirection
 *
 * @since {@link androidx.window.extensions.WindowExtensions#VENDOR_API_LEVEL_2}
 */
public class SplitAttributes {
    /**
     * Defines how the Task should be split between the primary and the secondary containers.
     *
     * @see RatioSplitType
     * @see ExpandContainersSplitType
     * @see HingeSplitType
     */
    public static class SplitType {
        @NonNull
        private final String mDescription;

        SplitType(@NonNull String description) {
            mDescription = description;
        }

        @Override
        public int hashCode() {
            return mDescription.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SplitType)) {
                return false;
            }
            final SplitType that = (SplitType) obj;
            return mDescription.equals(that.mDescription);
        }

        @NonNull
        @Override
        public String toString() {
            return mDescription;
        }

        /**
         * Defines what activity container should be given to the primary part of the task
         * bounds. Values in range (0.0, 1.0) define the size of the primary container of the
         * split relative to the corresponding task dimension size.
         */
        public static final class RatioSplitType extends SplitType {
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
            private final float mRatio;

            public RatioSplitType(
                    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
                    float ratio) {
                super("ratio:" + ratio);
                if (ratio <= 0.0f || ratio >= 1.0f) {
                    throw new IllegalArgumentException("Ratio must be in range (0.0, 1.0). "
                            + " Use SplitType.ExpandContainersSplitType() instead of 0 or 1.");
                }
                mRatio = ratio;
            }

            /**
             * Returns the {@code float} value ratio of the primary container of the split
             * relative to the corresponding task dimension size.
             */
            @FloatRange(from = 0.0, to = 1.0, fromInclusive = false, toInclusive = false)
            public float getRatio() {
                return mRatio;
            }

            /**
             * Indicate that the primary and secondary container share an equal split. It is also
             * the default {@link #getSplitType()} if
             * {@link SplitAttributes.Builder#setSplitType(SplitType)} is not specified.
             */
            @NonNull
            public static RatioSplitType splitEqually() {
                return new RatioSplitType(0.5f);
            }
        }

        /**
         * A special value of {@link #getSplitType()}. Indicates that the split ratio follows hinge
         * area position. This value will only be applied if:
         * <ol>
         *   <li> the host Task is not in multi-window mode(ex: split-screen, picture-in-picture).
         *   </li>
         *   <li> the device has hinge reported by {@link FoldingFeature#getBounds()}.</li>
         *   <li> the hinge area orientation matches how the task bounds are split:
         *     <ul>
         *       <li> the hinge area orientation is vertical, and the task bounds are also split
         *       vertically.
         *       </li>
         *       <li> the hinge area orientation is horizontal, and the tasks bounds are also split
         *       horizontally. </li>
         *     </ul>
         *   </li>
         * </ol>
         * Otherwise, it fallbacks to use {@link #getFallbackSplitType()}
         */
        public static final class HingeSplitType extends SplitType {
            @NonNull
            private final SplitType mFallbackSplitType;

            public HingeSplitType(@NonNull SplitType fallbackSplitType) {
                super("hinge, fallbackType=" + fallbackSplitType);
                mFallbackSplitType = fallbackSplitType;
            }

            /**
             * Returns the fallback {@link SplitType} if the host task <b>cannot</b> be split
             * by the hinge area position with the current window and device state.
             */
            @NonNull
            public SplitType getFallbackSplitType() {
                return mFallbackSplitType;
            }
        }

        /**
         * Indicate that both primary and secondary activity containers are expanded to fill the
         * task parent container, and the secondary container occludes the primary one. It is useful
         * to make the apps occupy the full task bounds in some device states.
         */
        public static final class ExpandContainersSplitType extends SplitType {
            public ExpandContainersSplitType() {
                super("expandContainers");
            }
        }
    }

    /** A class to define split layout directions. */
    public static final class LayoutDirection {
        // TODO(b/241043844): Add the illustration below in DAC.
        // -------------------------
        // |           |           |
        // |  Primary  | Secondary |
        // |           |           |
        // -------------------------
        /**
         * A value of {@link #getLayoutDirection()}:
         * It splits the task bounds vertically, puts the primary container on the left portion,
         * and the secondary container on the right portion.
         */
        // Must match {@link LayoutDirection#LTR} for backwards compatibility with prior versions
        // of Extensions.
        public static final int LEFT_TO_RIGHT = 0;
        // TODO(b/241043844): Add the illustration below in DAC.
        //            -------------------------
        //            |           |           |
        //            | Secondary |  Primary  |
        //            |           |           |
        //            -------------------------
        /**
         * A value of {@link #getLayoutDirection()}:
         * It splits the task bounds vertically, puts the primary container on the right portion,
         * and the secondary container on the left portion.
         */
        // Must match {@link LayoutDirection#RTL} for backwards compatibility with prior versions
        // of Extensions.
        public static final int RIGHT_TO_LEFT = 1;
        /**
         * A value of {@link #getLayoutDirection()}:
         * It splits the task bounds vertically and the direction is deduced from the
         * language script of locale. The direction can be either {@link #LEFT_TO_RIGHT}
         * or {@link #RIGHT_TO_LEFT}.
         */
        // Must match {@link LayoutDirection#LOCALE} for backwards compatibility with prior
        // versions of Extensions.
        public static final int LOCALE = 3;
        // TODO(b/241043844): Add the illustration below in DAC.
        //            -------------
        //            |           |
        //            |  Primary  |
        //            |           |
        //            -------------
        //            |           |
        //            | Secondary |
        //            |           |
        //            -------------
        /**
         * A value of {@link #getLayoutDirection()}:
         * It splits the task bounds horizontally, puts the primary container on the top portion,
         * and the secondary container on the bottom portion.
         */
        public static final int TOP_TO_BOTTOM = 4;
        // TODO(b/241043844): Add the illustration below in DAC.
        //            -------------
        //            |           |
        //            | Secondary |
        //            |           |
        //            -------------
        //            |           |
        //            |  Primary  |
        //            |           |
        //            -------------
        /**
         * A value of {@link #getLayoutDirection()}:
         * It splits the task bounds horizontally, puts the primary container on the bottom
         * portion, and the secondary container on the top portion.
         */
        public static final int BOTTOM_TO_TOP = 5;

        private LayoutDirection() {}
    }

    @IntDef({
            LayoutDirection.LEFT_TO_RIGHT,
            LayoutDirection.RIGHT_TO_LEFT,
            LayoutDirection.LOCALE,
            LayoutDirection.TOP_TO_BOTTOM,
            LayoutDirection.BOTTOM_TO_TOP,
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface LayoutDir {}

    @LayoutDir
    private final int mLayoutDirection;

    private final SplitType mSplitType;

    SplitAttributes(@NonNull SplitType splitType, @LayoutDir int layoutDirection) {
        mSplitType = splitType;
        mLayoutDirection = layoutDirection;
    }

    /** Returns {@link SplitAttributes.LayoutDirection} for the {@link SplitAttributes}. */
    @LayoutDir
    public int getLayoutDirection() {
        return mLayoutDirection;
    }

    /** Returns {@link SplitType} for the {@link SplitAttributes}. */
    @NonNull
    public SplitType getSplitType() {
        return mSplitType;
    }

    /** Builders for {@link SplitAttributes} */
    public static final class Builder {
        @NonNull
        private SplitType mSplitType =  new SplitType.RatioSplitType(0.5f);
        @LayoutDir
        private int mLayoutDirection = LayoutDirection.LOCALE;

        /**
         * Sets the {@link SplitType} of this {@link SplitAttributes}. The default value is
         * {@link SplitType.RatioSplitType#splitEqually()} if other value is not requested
         * explicitly.
         */
        @NonNull
        public Builder setSplitType(@NonNull SplitType splitType) {
            mSplitType = splitType;
            return this;
        }

        /**
         * Sets the {@link LayoutDirection} of this {@link SplitAttributes}. The default value is
         * {@link LayoutDirection#LOCALE} if other value is not requested explicitly.
         * Must be one of:
         * <ul>
         *     <li>{@link LayoutDirection#LEFT_TO_RIGHT}</li>
         *     <li>{@link LayoutDirection#RIGHT_TO_LEFT}</li>
         *     <li>{@link LayoutDirection#LOCALE}</li>
         *     <li>{@link LayoutDirection#TOP_TO_BOTTOM}</li>
         *     <li>{@link LayoutDirection#BOTTOM_TO_TOP}</li>
         * </ul>
         */
        @SuppressLint("WrongConstant") // To compat with android.util.LayoutDirection APIs
        @NonNull
        public Builder setLayoutDirection(@LayoutDir int layoutDirection) {
            mLayoutDirection = layoutDirection;
            return this;
        }

        /** Builds {@link SplitAttributes} instance. */
        @NonNull
        public SplitAttributes build() {
            return new SplitAttributes(mSplitType, mLayoutDirection);
        }
    }

    @Override
    public int hashCode() {
        int result = 17 * mSplitType.hashCode();
        result = result + 31 * mLayoutDirection;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SplitAttributes)) {
            return false;
        }
        final SplitAttributes otherLayout = (SplitAttributes) other;
        return mLayoutDirection == otherLayout.mLayoutDirection
                && mSplitType.equals(otherLayout.mSplitType);
    }

    @NonNull
    @Override
    public String toString() {
        return SplitAttributes.class.getSimpleName() + "{"
                + "layoutDir=" + layoutDirectionToString()
                + ", ratio=" + mSplitType
                + "}";
    }

    @NonNull
    private String layoutDirectionToString() {
        switch(mLayoutDirection) {
            case LayoutDirection.LEFT_TO_RIGHT:
                return "LEFT_TO_RIGHT";
            case LayoutDirection.RIGHT_TO_LEFT:
                return "RIGHT_TO_LEFT";
            case LayoutDirection.LOCALE:
                return "LOCALE";
            case LayoutDirection.TOP_TO_BOTTOM:
                return "TOP_TO_BOTTOM";
            case LayoutDirection.BOTTOM_TO_TOP:
                return "BOTTOM_TO_TOP";
            default:
                throw new IllegalArgumentException("Invalid layout direction:" + mLayoutDirection);
        }
    }
}
