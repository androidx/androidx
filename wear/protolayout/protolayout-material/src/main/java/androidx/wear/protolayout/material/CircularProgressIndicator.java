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

package androidx.wear.protolayout.material;

import static androidx.annotation.Dimension.DP;
import static androidx.wear.protolayout.DimensionBuilders.degrees;
import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_COLORS;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_END_ANGLE;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_PADDING;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_START_ANGLE;
import static androidx.wear.protolayout.material.ProgressIndicatorDefaults.DEFAULT_STROKE_WIDTH;
import static androidx.wear.protolayout.materialcore.Helper.checkNotNull;
import static androidx.wear.protolayout.materialcore.Helper.checkTag;
import static androidx.wear.protolayout.materialcore.Helper.getMetadataTagName;
import static androidx.wear.protolayout.materialcore.Helper.getTagBytes;
import static androidx.wear.protolayout.materialcore.Helper.staticFloat;

import static java.lang.Math.max;
import static java.lang.Math.min;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DimensionBuilders.AngularLayoutConstraint;
import androidx.wear.protolayout.DimensionBuilders.DegreesProp;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.LayoutElementBuilders.Arc;
import androidx.wear.protolayout.LayoutElementBuilders.ArcLine;
import androidx.wear.protolayout.LayoutElementBuilders.ArcSpacer;
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement;
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata;
import androidx.wear.protolayout.ModifiersBuilders.Modifiers;
import androidx.wear.protolayout.ModifiersBuilders.Padding;
import androidx.wear.protolayout.ModifiersBuilders.Semantics;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * ProtoLayout component {@link CircularProgressIndicator} that represents circular progress
 * indicator which supports a gap in the circular track between startAngle and endAngle.
 *
 * <p>The CircularProgressIndicator is a colored arc around the edge of the screen with the given
 * start and end angles, which can describe a full or partial circle. Behind it is an arc with
 * optional gap representing full progress. The recommended sizes are defined in {@link
 * ProgressIndicatorDefaults}. Unless specified, the CircularProgressIndicator will have the full
 * length.
 *
 * <p>The recommended set of {@link ProgressIndicatorColors} can be obtained from {@link
 * ProgressIndicatorDefaults}, e.g. {@link ProgressIndicatorDefaults#DEFAULT_COLORS} to get a
 * default color scheme for a {@link CircularProgressIndicator}.
 *
 * <p>When accessing the contents of a container for testing, note that this element can't be simply
 * casted back to the original type, i.e.:
 *
 * <pre>{@code
 * CircularProgressIndicator cpi = new CircularProgressIndicator...
 * Box box = new Box.Builder().addContent(cpi).build();
 *
 * CircularProgressIndicator myCpi = (CircularProgressIndicator) box.getContents().get(0);
 * }</pre>
 *
 * will fail.
 *
 * <p>To be able to get {@link CircularProgressIndicator} object from any layout element, {@link
 * #fromLayoutElement} method should be used, i.e.:
 *
 * <pre>{@code
 * CircularProgressIndicator myCpi =
 *   CircularProgressIndicator.fromLayoutElement(box.getContents().get(0));
 * }</pre>
 */
public class CircularProgressIndicator implements LayoutElement {
    /**
     * Tool tag for Metadata in Modifiers, so we know that Arc is actually a
     * CircularProgressIndicator.
     */
    static final String METADATA_TAG = "CPI";

    private final @NonNull Arc mElement;
    private final @NonNull ArcLine mProgress;
    private final @NonNull ArcLine mBackground;

    CircularProgressIndicator(@NonNull Arc element) {
        this.mElement = element;
        this.mBackground = (ArcLine) element.getContents().get(0);
        this.mProgress = (ArcLine) element.getContents().get(2);
    }

    /** Builder class for {@link CircularProgressIndicator} */
    @SuppressWarnings("HiddenSuperclass")
    public static final class Builder implements LayoutElement.Builder {
        private @NonNull ProgressIndicatorColors mCircularProgressIndicatorColors = DEFAULT_COLORS;
        private @NonNull DpProp mStrokeWidth = DEFAULT_STROKE_WIDTH;
        private @Nullable StringProp mContentDescription;
        private @NonNull DegreesProp mStartAngle = degrees(DEFAULT_START_ANGLE);
        private @NonNull DegreesProp mEndAngle = degrees(DEFAULT_END_ANGLE);
        private @NonNull FloatProp mProgress = staticFloat(0f);
        private boolean mIsMarginApplied = true;

        /** Creates a builder for the {@link CircularProgressIndicator}. */
        public Builder() {}

        /**
         * Sets the progress of the {@link CircularProgressIndicator}. Progress ratio should be a
         * value between 0 and 1. If not set, 0 will be used. Progress will be colored in {@link
         * ProgressIndicatorColors#getIndicatorColor()}.
         */
        public @NonNull Builder setProgress(@FloatRange(from = 0, to = 1) float progressRatio) {
            this.mProgress = staticFloat(progressRatio);
            return this;
        }

        /**
         * Sets the progress of the {@link CircularProgressIndicator}. If not set, static value
         * provided using {@link #setProgress(float)} will be used, or 0. Progress will be colored
         * in {@link ProgressIndicatorColors#getIndicatorColor()}.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         *
         * @param progressRatio The progress between 0 and 1. This field supports setting a dynamic
         *     value. The static value of {@code progressRatio} will be considered as 0 if it's
         *     smaller than zero and as 1 if it's larger than one.
         */
        public @NonNull Builder setProgress(@NonNull FloatProp progressRatio) {
            this.mProgress = progressRatio;
            return this;
        }

        /**
         * Sets the start angle of the {@link CircularProgressIndicator}'s background arc, where
         * angle 0 is 12 o'clock. Start angle doesn't need to be within 0-360 range. I.e. -90 is to
         * start arc from the 9 o'clock. If not set 0 will be used and the indicator will have full
         * length.
         */
        public @NonNull Builder setStartAngle(float startAngle) {
            this.mStartAngle = degrees(startAngle);
            return this;
        }

        /**
         * Sets the end angle of the {@link CircularProgressIndicator}'s background arc, where angle
         * 0 is 12 o'clock. End angle doesn't need to be within 0-360 range, but it must be larger
         * than start angle. If not set 360 will be used and the indicator will have full length.
         */
        public @NonNull Builder setEndAngle(float endAngle) {
            this.mEndAngle = degrees(endAngle);
            return this;
        }

        /**
         * Sets the static content description of the {@link CircularProgressIndicator} to be used
         * for accessibility support.
         */
        public @NonNull Builder setContentDescription(@NonNull CharSequence contentDescription) {
            this.mContentDescription =
                    new StringProp.Builder(contentDescription.toString()).build();
            return this;
        }

        /**
         * Sets the content description of the {@link CircularProgressIndicator} to be used for
         * accessibility support.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @NonNull Builder setContentDescription(@NonNull StringProp contentDescription) {
            this.mContentDescription = contentDescription;
            return this;
        }

        /**
         * Sets the colors for the {@link CircularProgressIndicator}. If set, {@link
         * ProgressIndicatorColors#getIndicatorColor()} will be used for a progress that has been
         * made, while {@link ProgressIndicatorColors#getTrackColor()} will be used for a background
         * full size arc. If not set, {@link ProgressIndicatorDefaults#DEFAULT_COLORS} will be used.
         */
        public @NonNull Builder setCircularProgressIndicatorColors(
                @NonNull ProgressIndicatorColors circularProgressIndicatorColors) {
            this.mCircularProgressIndicatorColors = circularProgressIndicatorColors;
            return this;
        }

        /**
         * Sets the stroke width of the {@link CircularProgressIndicator}. Strongly recommended
         * value is {@link ProgressIndicatorDefaults#DEFAULT_STROKE_WIDTH}.
         */
        public @NonNull Builder setStrokeWidth(@NonNull DpProp strokeWidth) {
            this.mStrokeWidth = strokeWidth;
            return this;
        }

        /**
         * Sets the stroke width of the {@link CircularProgressIndicator}. Strongly recommended
         * value is {@link ProgressIndicatorDefaults#DEFAULT_STROKE_WIDTH}.
         */
        public @NonNull Builder setStrokeWidth(@Dimension(unit = DP) float strokeWidth) {
            this.mStrokeWidth = dp(strokeWidth);
            return this;
        }

        /**
         * Sets whether this {@link CircularProgressIndicator} should have outer margin or not.
         *
         * <p>If this indicator is used as a smaller element, use this method to remove an
         * additional margin around it by setting it to {@code false}.
         *
         * <p>Otherwise, if this indicator is used as a full screen one or in {@link
         * androidx.wear.protolayout.material.layouts.EdgeContentLayout}, it's strongly recommended
         * to set this to {@code true}.
         *
         * <p>If not set, defaults to true.
         */
        public @NonNull Builder setOuterMarginApplied(boolean isApplied) {
            this.mIsMarginApplied = isApplied;
            return this;
        }

        /**
         * Constructs and returns {@link CircularProgressIndicator} with the provided field and
         * look.
         */
        @Override
        public @NonNull CircularProgressIndicator build() {
            checkAngles();

            DegreesProp length = getLength();
            Modifiers.Builder modifiers =
                    new Modifiers.Builder()
                            .setMetadata(
                                    new ElementMetadata.Builder()
                                            .setTagData(getTagBytes(METADATA_TAG))
                                            .build());

            if (mIsMarginApplied) {
                modifiers.setPadding(
                        new Padding.Builder().setRtlAware(true).setAll(DEFAULT_PADDING).build());
            }

            if (mContentDescription != null) {
                modifiers.setSemantics(
                        new Semantics.Builder().setContentDescription(mContentDescription).build());
            }

            ArcLine.Builder progressArcLineBuilder =
                    new ArcLine.Builder()
                            .setArcDirection(LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE)
                            .setColor(mCircularProgressIndicatorColors.getIndicatorColor())
                            .setThickness(mStrokeWidth);
            applyCorrectValue(progressArcLineBuilder);

            Arc.Builder element =
                    new Arc.Builder()
                            .setArcDirection(LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE)
                            .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
                            .setAnchorAngle(mStartAngle)
                            .setModifiers(modifiers.build())
                            .addContent(
                                    new ArcLine.Builder()
                                            .setArcDirection(
                                                    LayoutElementBuilders.ARC_DIRECTION_CLOCKWISE)
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
                            .addContent(progressArcLineBuilder.build());

            return new CircularProgressIndicator(element.build());
        }

        private void applyCorrectValue(ArcLine.Builder builder) {
            float length = getLength().getValue();
            float staticValue = mProgress.getValue();
            staticValue = max(0, staticValue);
            staticValue = min(1, staticValue);
            if (mProgress.getDynamicValue() != null) {
                DynamicFloat progressRatio = mProgress.getDynamicValue();
                DynamicFloat dynamicLength =
                        DynamicBuilders.dynamicFloatFromProto(
                                        progressRatio.toDynamicFloatProto(),
                                        mProgress.getFingerprint())
                                .times(length);
                builder.setLength(
                        new DegreesProp.Builder(staticValue)
                                .setDynamicValue(dynamicLength)
                                .build());
                builder.setLayoutConstraintsForDynamicLength(
                        new AngularLayoutConstraint.Builder(length)
                                .setAngularAlignment(LayoutElementBuilders.ANGULAR_ALIGNMENT_START)
                                .build());
            } else {
                builder.setLength(degrees(staticValue * length));
            }
        }

        private void checkAngles() {
            if (mEndAngle.getValue() < mStartAngle.getValue()) {
                throw new IllegalArgumentException("End angle must be bigger than start angle.");
            }
        }

        private @NonNull DegreesProp getLength() {
            float startAngle = mStartAngle.getValue();
            float endAngle = mEndAngle.getValue();
            if (endAngle <= startAngle) {
                endAngle += 360;
            }
            return degrees(endAngle - startAngle);
        }
    }

    /** Returns angle representing progressed part of this CircularProgressIndicator. */
    public @NonNull DegreesProp getProgress() {
        return checkNotNull(mProgress.getLength());
    }

    /** Returns stroke width of this CircularProgressIndicator. */
    public @NonNull DpProp getStrokeWidth() {
        return checkNotNull(mProgress.getThickness());
    }

    /** Returns start angle of this CircularProgressIndicator. */
    public @NonNull DegreesProp getStartAngle() {
        return checkNotNull(mElement.getAnchorAngle());
    }

    /** Returns start angle of this CircularProgressIndicator. */
    public @NonNull DegreesProp getEndAngle() {
        float backArcLength = checkNotNull(mBackground.getLength()).getValue();
        return degrees(getStartAngle().getValue() + backArcLength);
    }

    /** Returns main arc color of this CircularProgressIndicator. */
    public @NonNull ProgressIndicatorColors getCircularProgressIndicatorColors() {
        return new ProgressIndicatorColors(
                checkNotNull(mProgress.getColor()), checkNotNull(mBackground.getColor()));
    }

    /** Returns content description of this CircularProgressIndicator. */
    public @Nullable StringProp getContentDescription() {
        Semantics semantics = checkNotNull(mElement.getModifiers()).getSemantics();
        if (semantics == null) {
            return null;
        }
        return semantics.getContentDescription();
    }

    /**
     * Returns metadata tag set to this CircularProgressIndicator, which should be {@link
     * #METADATA_TAG}.
     */
    @NonNull String getMetadataTag() {
        return getMetadataTagName(
                checkNotNull(checkNotNull(mElement.getModifiers()).getMetadata()));
    }

    /** Returns whether there is a margin around this indicator or not. */
    public boolean isOuterMarginApplied() {
        return this.mElement.getModifiers() != null
                && this.mElement.getModifiers().getPadding() != null;
    }

    /**
     * Returns CircularProgressIndicator object from the given LayoutElement (e.g. one retrieved
     * from a container's content with {@code container.getContents().get(index)}) if that element
     * can be converted to CircularProgressIndicator. Otherwise, it will return null.
     */
    public static @Nullable CircularProgressIndicator fromLayoutElement(
            @NonNull LayoutElement element) {
        if (element instanceof CircularProgressIndicator) {
            return (CircularProgressIndicator) element;
        }
        if (!(element instanceof Arc)) {
            return null;
        }
        Arc arcElement = (Arc) element;
        if (!checkTag(arcElement.getModifiers(), METADATA_TAG)) {
            return null;
        }
        // Now we are sure that this element is a CircularProgressIndicator.
        return new CircularProgressIndicator(arcElement);
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public LayoutElementProto.@NonNull LayoutElement toLayoutElementProto() {
        return mElement.toLayoutElementProto();
    }

    @Override
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @Nullable Fingerprint getFingerprint() {
        return mElement.getFingerprint();
    }
}
