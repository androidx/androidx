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

package androidx.camera.core.impl.utils;

import static androidx.camera.core.internal.utils.SizeUtil.getArea;

import android.graphics.RectF;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.core.util.Preconditions;

import java.util.Comparator;

/**
 * Utility class for aspect ratio related operations.
 */
public final class AspectRatioUtil {
    public static final Rational ASPECT_RATIO_4_3 = new Rational(4, 3);
    public static final Rational ASPECT_RATIO_3_4 = new Rational(3, 4);
    public static final Rational ASPECT_RATIO_16_9 = new Rational(16, 9);
    public static final Rational ASPECT_RATIO_9_16 = new Rational(9, 16);

    private static final int ALIGN16 = 16;

    private AspectRatioUtil() {
    }

    /**
     * Returns true if the supplied resolution is a mod16 matching with the supplied aspect ratio.
     *
     * <p>A default lower bound resolution {@link SizeUtil#RESOLUTION_VGA} is adopted. That means
     * only do mod 16 calculation if the size is equal to or larger than
     * {@link SizeUtil#RESOLUTION_VGA}. It is because the aspect ratio will be affected
     * critically by mod 16 calculation if the size is small. It may result in unexpected outcome
     * such like 256x144 will be considered as 18.5:9.
     */
    public static boolean hasMatchingAspectRatio(@NonNull Size resolution,
            @Nullable Rational aspectRatio) {
        return hasMatchingAspectRatio(resolution, aspectRatio, SizeUtil.RESOLUTION_VGA);
    }

    /**
     * Returns true if the supplied resolution is a mod16 matching with the supplied aspect ratio.
     *
     * <p>Mod 16 calculation take effects only when the input resolution is smaller than
     * {@code mod16ResolutionLowerBound}.
     */
    public static boolean hasMatchingAspectRatio(@NonNull Size resolution,
            @Nullable Rational aspectRatio, @NonNull Size mod16ResolutionLowerBound) {
        boolean isMatch = false;
        if (aspectRatio == null) {
            isMatch = false;
        } else if (aspectRatio.equals(
                new Rational(resolution.getWidth(), resolution.getHeight()))) {
            isMatch = true;
        } else if (getArea(resolution) >= getArea(mod16ResolutionLowerBound)) {
            isMatch = isPossibleMod16FromAspectRatio(resolution, aspectRatio);
        }
        return isMatch;
    }

    /**
     * For codec performance improvement, OEMs may make the supported sizes to be mod16 alignment
     * . It means that the width or height of the supported size will be multiple of 16. The
     * result number after applying mod16 alignment can be the larger or smaller number that is
     * multiple of 16 and is closest to the original number. For example, a standard 16:9
     * supported size is 1920x1080. It may become 1920x1088 on some devices because 1088 is
     * multiple of 16. This function uses the target aspect ratio to calculate the possible
     * original width or height inversely. And then, checks whether the possibly original width or
     * height is in the range that the mod16 aligned height or width can support.
     */
    private static boolean isPossibleMod16FromAspectRatio(@NonNull Size resolution,
            @NonNull Rational aspectRatio) {
        int width = resolution.getWidth();
        int height = resolution.getHeight();

        Rational invAspectRatio = new Rational(/* numerator= */aspectRatio.getDenominator(),
                /* denominator= */aspectRatio.getNumerator());
        if (width % 16 == 0 && height % 16 == 0) {
            return ratioIntersectsMod16Segment(Math.max(0, height - ALIGN16), width, aspectRatio)
                    || ratioIntersectsMod16Segment(Math.max(0, width - ALIGN16), height,
                    invAspectRatio);
        } else if (width % 16 == 0) {
            return ratioIntersectsMod16Segment(height, width, aspectRatio);
        } else if (height % 16 == 0) {
            return ratioIntersectsMod16Segment(width, height, invAspectRatio);
        }
        return false;
    }

    private static boolean ratioIntersectsMod16Segment(int height, int mod16Width,
            Rational aspectRatio) {
        Preconditions.checkArgument(mod16Width % 16 == 0);
        double aspectRatioWidth =
                height * aspectRatio.getNumerator() / (double) aspectRatio.getDenominator();
        return aspectRatioWidth > Math.max(0, mod16Width - ALIGN16) && aspectRatioWidth < (
                mod16Width + ALIGN16);
    }

    /**
     * Comparator based on how close they are to the target aspect ratio by comparing the
     * transformed mapping area in the full FOV ratio space.
     *
     * The mapping area will be the region that the images of the specific aspect ratio cropped
     * from the full FOV images. Therefore, we can compare the mapping areas to know which one is
     * closer to the mapping area of the target aspect ratio setting.
     */
    public static final class CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace implements
            Comparator<Rational> {
        private final Rational mTargetRatio;
        private final RectF mTransformedMappingArea;
        private final Rational mFullFovRatio;

        public CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                @NonNull Rational targetRatio, @Nullable Rational fullFovRatio) {
            mTargetRatio = targetRatio;
            mFullFovRatio = fullFovRatio != null ? fullFovRatio : new Rational(4, 3);
            mTransformedMappingArea = getTransformedMappingArea(mTargetRatio);
        }

        @Override
        public int compare(Rational lhs, Rational rhs) {
            if (lhs.equals(rhs)) {
                return 0;
            }

            RectF lhsMappingArea = getTransformedMappingArea(lhs);
            RectF rhsMappingArea = getTransformedMappingArea(rhs);

            boolean isCoveredByLhs = isMappingAreaCovered(lhsMappingArea,
                    mTransformedMappingArea);
            boolean isCoveredByRhs = isMappingAreaCovered(rhsMappingArea,
                    mTransformedMappingArea);

            if (isCoveredByLhs && isCoveredByRhs) {
                // When both ratios can cover the transformed target aspect mapping area in the
                // full FOV space, checks which area is smaller to determine which ratio is
                // closer to the target aspect ratio.
                return (int) Math.signum(
                        getMappingAreaSize(lhsMappingArea) - getMappingAreaSize(rhsMappingArea));
            } else if (isCoveredByLhs) {
                return -1;
            } else if (isCoveredByRhs) {
                return 1;
            } else {
                // When both ratios can't cover the transformed target aspect mapping area in the
                // full FOV space, checks which overlapping area is larger to determine which
                // ratio is closer to the target aspect ratio.
                float lhsOverlappingArea = getOverlappingAreaSize(lhsMappingArea,
                        mTransformedMappingArea);
                float rhsOverlappingArea = getOverlappingAreaSize(rhsMappingArea,
                        mTransformedMappingArea);
                return -((int) Math.signum(lhsOverlappingArea - rhsOverlappingArea));
            }
        }

        /**
         * Returns the rectangle after transforming the input rational into full FOV aspect ratio
         * space.
         */
        private RectF getTransformedMappingArea(Rational ratio) {
            if (ratio.floatValue() == mFullFovRatio.floatValue()) {
                return new RectF(0, 0, mFullFovRatio.getNumerator(),
                        mFullFovRatio.getDenominator());
            } else if (ratio.floatValue() > mFullFovRatio.floatValue()) {
                return new RectF(0, 0, mFullFovRatio.getNumerator(),
                        (float) ratio.getDenominator() * (float) mFullFovRatio.getNumerator()
                                / (float) ratio.getNumerator());
            } else {
                return new RectF(0, 0,
                        (float) ratio.getNumerator() * (float) mFullFovRatio.getDenominator()
                                / (float) ratio.getDenominator(), mFullFovRatio.getDenominator());
            }
        }

        /**
         * Returns {@code true} if the source transformed mapping area can fully cover the target
         * transformed mapping area. Otherwise, returns {@code false};
         */
        private boolean isMappingAreaCovered(RectF sourceMappingArea, RectF targetMappingArea) {
            return sourceMappingArea.width() >= targetMappingArea.width()
                    && sourceMappingArea.height() >= targetMappingArea.height();
        }

        /**
         * Returns the input mapping area's size value.
         */
        private float getMappingAreaSize(RectF mappingArea) {
            return mappingArea.width() * mappingArea.height();
        }

        /**
         * Returns the overlapping area value between the input two mapping areas in the full FOV
         * space.
         */
        private float getOverlappingAreaSize(RectF mappingArea1, RectF mappingArea2) {
            float overlappingAreaWidth = mappingArea1.width() < mappingArea2.width()
                    ? mappingArea1.width() : mappingArea2.width();
            float overlappingAreaHeight = mappingArea1.height() < mappingArea2.height()
                    ? mappingArea1.height() : mappingArea2.height();
            return overlappingAreaWidth * overlappingAreaHeight;
        }
    }
}
