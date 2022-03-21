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

package androidx.wear.tiles.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.tiles.DimensionBuilders.degrees;
import static androidx.wear.tiles.DimensionBuilders.dp;
import static androidx.wear.tiles.material.Helper.checkNotNull;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_COLOR;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_END_ANGLE;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_START_ANGLE;
import static androidx.wear.tiles.material.ProgressIndicatorDefaults.DEFAULT_STROKE_WIDTH;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.DimensionBuilders.DegreesProp;
import androidx.wear.tiles.DimensionBuilders.DpProp;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.LayoutElementBuilders.Arc;
import androidx.wear.tiles.LayoutElementBuilders.ArcLine;
import androidx.wear.tiles.LayoutElementBuilders.ArcSpacer;
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement;
import androidx.wear.tiles.ModifiersBuilders.Modifiers;
import androidx.wear.tiles.ModifiersBuilders.Padding;
import androidx.wear.tiles.ModifiersBuilders.Semantics;
import androidx.wear.tiles.proto.LayoutElementProto;

/**
 * Tiles component {@link CircularProgressIndicator} that represents circular progress indicator
 * which supports a gap in the circular track between startAngle and endAngle. [Progress Indicator
 * doc] (https://developer.android.com/training/wearables/components/progress-indicator)
 *
 * <p>The CircularProgressIndicator is a colored arc around the edge of the screen with the given
 * start and end angles, which can describe a full or partial circle. Behind it is an arc with
 * optional gap representing full progress. The recommended sizes are defined in {@link
 * ProgressIndicatorDefaults}. Unless specified, the CircularProgressIndicator will have the full
 * length.
 *
 * <p>The recommended set of {@link ProgressIndicatorColors} can be obtained from {@link
 * ProgressIndicatorDefaults}, e.g. {@link ProgressIndicatorDefaults#DEFAULT_COLOR} to get a default
 * color scheme for a {@link CircularProgressIndicator}.
 */
public class CircularProgressIndicator implements LayoutElement {
    @NonNull private final Arc mElement;
    @NonNull private final ArcLine mProgress;
    @NonNull private final ArcLine mBackground;

    CircularProgressIndicator(@NonNull Arc element) {
        this.mElement = element;
        this.mBackground = (ArcLine) element.getContents().get(0);
        this.mProgress = (ArcLine) element.getContents().get(2);
    }

    /** Builder class for {@link CircularProgressIndicator} */
    public static final class Builder implements LayoutElement.Builder {
        @NonNull private ProgressIndicatorColors mCircularProgressIndicatorColors = DEFAULT_COLOR;
        @NonNull private DpProp mStrokeWidth = DEFAULT_STROKE_WIDTH;
        @NonNull private CharSequence mContentDescription = "";
        @NonNull private DegreesProp mStartAngle = degrees(DEFAULT_START_ANGLE);
        @NonNull private DegreesProp mEndAngle = degrees(DEFAULT_END_ANGLE);

        @FloatRange(from = 0, to = 1)
        private float mProgress = 0;

        /** Creates a builder for the {@link CircularProgressIndicator}. */
        public Builder() {}

        /**
         * Sets the progress of the {@link CircularProgressIndicator}. Progress should be percentage
         * from 0 to 1. Progress will be colored in {@link ProgressIndicatorColors#getTrackColor}.
         * If not set, 0 will be used.
         */
        @NonNull
        public Builder setProgress(@FloatRange(from = 0, to = 1) float progressPercentage) {
            this.mProgress = progressPercentage;
            return this;
        }

        /**
         * Sets the start angle of the {@link CircularProgressIndicator}'s background arc, where
         * angle 0 is 12 o'clock. Start angle doesn't need to be within 0-360 range. I.e. -90 is to
         * start arc from the 9 o'clock. If not set 0 will be used and the indicator will have full
         * length.
         */
        @NonNull
        public Builder setStartAngle(float startAngle) {
            this.mStartAngle = degrees(startAngle);
            return this;
        }

        /**
         * Sets the end angle of the {@link CircularProgressIndicator}'s background arc, where angle
         * 0 is 12 o'clock. End angle doesn't need to be within 0-360 range, but it must be larger
         * than start angle. If not set 360 will be used and the indicator will have full length.
         */
        @NonNull
        public Builder setEndAngle(float endAngle) {
            this.mEndAngle = degrees(endAngle);
            return this;
        }

        /**
         * Sets the content description of the {@link CircularProgressIndicator} to be used for
         * accessibility support.
         */
        @NonNull
        public Builder setContentDescription(@NonNull CharSequence contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        // TODO(b/203078514): Add getting color from the current Theme (from XML).
        /**
         * Sets the colors for the {@link CircularProgressIndicator}. If set, {@link
         * ProgressIndicatorColors#getIndicatorColor()} will be used for a progress that has been
         * made, while {@link ProgressIndicatorColors#getTrackColor()} will be used for a background
         * full size arc. If not set, {@link ProgressIndicatorDefaults#DEFAULT_COLOR} will be used.
         */
        @NonNull
        public Builder setCircularProgressIndicatorColors(
                @NonNull ProgressIndicatorColors circularProgressIndicatorColors) {
            this.mCircularProgressIndicatorColors = circularProgressIndicatorColors;
            return this;
        }

        /**
         * Sets the stroke width of the {@link CircularProgressIndicator}. Strongly recommended
         * value is {@link ProgressIndicatorDefaults#DEFAULT_STROKE_WIDTH}.
         */
        @NonNull
        public Builder setStrokeWidth(@NonNull DpProp strokeWidth) {
            this.mStrokeWidth = strokeWidth;
            return this;
        }

        /**
         * Sets the stroke width of the {@link CircularProgressIndicator}. Strongly recommended
         * value is {@link ProgressIndicatorDefaults#DEFAULT_STROKE_WIDTH}.
         */
        @NonNull
        public Builder setStrokeWidth(@Dimension(unit = DP) float strokeWidth) {
            this.mStrokeWidth = dp(strokeWidth);
            return this;
        }

        /**
         * Constructs and returns {@link CircularProgressIndicator} with the provided field and
         * look.
         */
        @NonNull
        @Override
        public CircularProgressIndicator build() {
            checkAngles();

            DegreesProp length = getLength();
            Arc.Builder element =
                    new Arc.Builder()
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                            .setAnchorAngle(mStartAngle)
                            .setModifiers(
                                    new Modifiers.Builder()
                                            .setSemantics(
                                                    new Semantics.Builder()
                                                            .setContentDescription(
                                                                    mContentDescription.toString())
                                                            .build())
                                            .setPadding(
                                                    new Padding.Builder()
                                                            .setAll(DEFAULT_PADDING)
                                                            .build())
                                            .build())
                            .addContent(
                                    new ArcLine.Builder()
                                            .setColor(
                                                    mCircularProgressIndicatorColors
                                                            .getTrackColor())
                                            .setThickness(mStrokeWidth)
                                            .setLength(length)
                                            .build())
                            .addContent(
                                    // Fill in the space to make a full circle, so that progress is
                                    // correctly aligned.
                                    new ArcSpacer.Builder()
                                            .setLength(degrees(360 - length.getValue()))
                                            .build())
                            .addContent(
                                    new ArcLine.Builder()
                                            .setColor(
                                                    mCircularProgressIndicatorColors
                                                            .getIndicatorColor())
                                            .setThickness(mStrokeWidth)
                                            .setLength(degrees(mProgress * length.getValue()))
                                            .build());
            return new CircularProgressIndicator(element.build());
        }

        private void checkAngles() {
            if (mEndAngle.getValue() < mStartAngle.getValue()) {
                throw new IllegalArgumentException("End angle must be bigger than start angle.");
            }
        }

        @NonNull
        private DegreesProp getLength() {
            float startAngle = mStartAngle.getValue();
            float endAngle = mEndAngle.getValue();
            if (endAngle <= startAngle) {
                endAngle += 360;
            }
            return degrees(endAngle - startAngle);
        }
    }

    /** Returns angle representing progressed part of this CircularProgressIndicator. */
    @NonNull
    public DegreesProp getProgress() {
        return checkNotNull(mProgress.getLength());
    }

    /** Returns stroke width of this CircularProgressIndicator. */
    @NonNull
    public DpProp getStrokeWidth() {
        return checkNotNull(mProgress.getThickness());
    }

    /** Returns start angle of this CircularProgressIndicator. */
    @NonNull
    public DegreesProp getStartAngle() {
        return checkNotNull(mElement.getAnchorAngle());
    }

    /** Returns start angle of this CircularProgressIndicator. */
    @NonNull
    public DegreesProp getEndAngle() {
        float backArcLength = checkNotNull(mBackground.getLength()).getValue();
        return degrees(getStartAngle().getValue() + backArcLength);
    }

    /** Returns main arc color of this CircularProgressIndicator. */
    @NonNull
    public ProgressIndicatorColors getCircularProgressIndicatorColors() {
        return new ProgressIndicatorColors(
                checkNotNull(mProgress.getColor()), checkNotNull(mBackground.getColor()));
    }

    /** Returns content description of this CircularProgressIndicator. */
    @NonNull
    public String getContentDescription() {
        return checkNotNull(checkNotNull(mElement.getModifiers()).getSemantics())
                .getContentDescription();
    }

    /** @hide */
    @NonNull
    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }
}
