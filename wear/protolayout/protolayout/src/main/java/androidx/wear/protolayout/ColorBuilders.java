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

package androidx.wear.protolayout;

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DimensionBuilders.DegreesProp;
import androidx.wear.protolayout.DimensionBuilders.OffsetDimension;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.ColorProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builders for color utilities for layout elements. */
public final class ColorBuilders {
    private ColorBuilders() {}

    /** Shortcut for building a {@link ColorProp} using an ARGB value. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull ColorProp argb(@ColorInt int colorArgb) {
        return new ColorProp.Builder(colorArgb).build();
    }

    /** A property defining a color. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ColorProp {
        private final ColorProto.ColorProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ColorProp(ColorProto.ColorProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static color value, in ARGB format. If a dynamic value is also set and the
         * renderer supports dynamic values for the corresponding field, this static value will be
         * ignored. If the static value is not specified, zero (equivalent to {@link
         * Color#TRANSPARENT}) will be used instead.
         */
        @ColorInt
        public int getArgb() {
            return mImpl.getArgb();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         */
        public @Nullable DynamicColor getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ColorProp fromProto(
                ColorProto.@NonNull ColorProp proto, @Nullable Fingerprint fingerprint) {
            return new ColorProp(proto, fingerprint);
        }

        static @NonNull ColorProp fromProto(ColorProto.@NonNull ColorProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ColorProto.@NonNull ColorProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ColorProp{" + "argb=" + getArgb() + ", dynamicValue=" + getDynamicValue() + "}";
        }

        /** Builder for {@link ColorProp} */
        public static final class Builder {
            private final ColorProto.ColorProp.Builder mImpl = ColorProto.ColorProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1955659823);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicColor)} can be used to provide a dynamic value.
             */
            public Builder(@ColorInt int staticValue) {
                setArgb(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(int)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static color value, in ARGB format. If a dynamic value is also set and the
             * renderer supports dynamic values for the corresponding field, this static value will
             * be ignored. If the static value is not specified, zero (equivalent to {@link
             * Color#TRANSPARENT}) will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setArgb(@ColorInt int argb) {
                mImpl.setArgb(argb);
                mFingerprint.recordPropertyUpdate(1, argb);
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value. If
             * {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setDynamicValue(@NonNull DynamicColor dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicColorProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicColor)} but neither {@link #Builder(int)} nor {@link
             *     #setArgb(int)} is used to provide a static value.
             */
            public @NonNull ColorProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasArgb()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new ColorProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A color and an offset, determining a color position in a gradient. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final class ColorStop {
        private final ColorProto.ColorStop mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ColorStop(ColorProto.ColorStop impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the color for this stop. Only opaque colors are supported. Any transparent colors
         * will have their alpha component set to 0xFF (opaque).
         */
        public @NonNull ColorProp getColor() {
            return ColorProp.fromProto(mImpl.getColor());
        }

        /**
         * Gets the relative offset for this color, between 0 and 1. This determines where the color
         * is positioned relative to a gradient space.
         */
        public @Nullable FloatProp getOffset() {
            if (mImpl.hasOffset()) {
                return FloatProp.fromProto(mImpl.getOffset());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ColorStop fromProto(
                ColorProto.@NonNull ColorStop proto, @Nullable Fingerprint fingerprint) {
            return new ColorStop(proto, fingerprint);
        }

        static @NonNull ColorStop fromProto(ColorProto.@NonNull ColorStop proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ColorProto.@NonNull ColorStop toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ColorStop{" + "color=" + getColor() + ", offset=" + getOffset() + "}";
        }

        /** Builder for {@link ColorStop} */
        public static final class Builder {
            private final ColorProto.ColorStop.Builder mImpl = ColorProto.ColorStop.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-468737254);

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param color the color for this stop. Only opaque colors are supported. Any
             *     transparent colors will have their alpha component set to 0xFF (opaque).
             * @param offset the relative offset for this color, between 0 and 1. This determines
             *     where the color is positioned relative to a gradient space.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public Builder(@NonNull ColorProp color, @NonNull FloatProp offset) {
                setColor(color);
                setOffset(offset);
            }

            @RequiresSchemaVersion(major = 1, minor = 300)
            Builder() {}

            /**
             * Sets the color for this stop. Only opaque colors are supported. Any transparent
             * colors will have their alpha component set to 0xFF (opaque).
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the relative offset for this color, between 0 and 1. This determines where the
             * color is positioned relative to a gradient space.
             *
             * <p>Note that this field only supports static values.
             *
             * @throws IllegalArgumentException if the offset value is outside of range [0,1].
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @NonNull Builder setOffset(@NonNull FloatProp offset) {
                float value = offset.getValue();
                if (value < 0f || value > 1f) {
                    throw new IllegalArgumentException(
                            "Offset must be between 0 and 1. Got " + offset);
                }
                mImpl.setOffset(offset.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(offset.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ColorStop build() {
                return new ColorStop(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A sweep gradient with the given colors dispersed around its center with offsets defined in
     * each color stop. The sweep begins at the parent's base angle plus the given angular shift and
     * continues clockwise until it reaches the starting position again.
     *
     * <p>The gradient center corresponds to center of the parent element.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final class SweepGradient implements Brush {
        private final ColorProto.SweepGradient mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SweepGradient(ColorProto.SweepGradient impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the color stops defining how the colors are distributed around the gradient center.
         * The color sequence starts at the start angle and spans 360 degrees clockwise, finishing
         * at the same angle.
         *
         * <p>A color stop is a pair of a color and its offset in the gradient. The offset is the
         * relative position of the color, beginning with 0 from the start angle and ending with 1.0
         * at the end angle, spanning clockwise.
         *
         * <p>There must be at least 2 colors and at most 10 colors.
         *
         * <p>If offset values are not set, the colors are evenly distributed in the gradient.
         */
        public @NonNull List<ColorStop> getColorStops() {
            List<ColorStop> list = new ArrayList<>();
            for (ColorProto.ColorStop item : mImpl.getColorStopsList()) {
                list.add(ColorStop.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the start angle of the gradient relative to the element's base angle. If not set,
         * defaults to zero.
         *
         * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base angle is
         * the angle where the line starts. The value represents a relative position in the line's
         * length span. Values greater than 360 degrees correspond to upper layers of the arc line
         * as it wraps over itself.
         */
        public @NonNull DegreesProp getStartAngle() {
            if (mImpl.hasStartAngle()) {
                return DegreesProp.fromProto(mImpl.getStartAngle());
            } else {
                return new DegreesProp.Builder(0f).build();
            }
        }

        /**
         * Gets the end angle of the gradient, relative to the element's base angle. If not set,
         * defaults to 360 degrees.
         *
         * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base angle is
         * the angle where the line starts. The value represents a relative position in the line's
         * length span. Values greater than 360 degrees correspond to upper layers of the arc line
         * as it wraps over itself.
         */
        public @NonNull DegreesProp getEndAngle() {
            if (mImpl.hasEndAngle()) {
                return DegreesProp.fromProto(mImpl.getEndAngle());
            } else {
                return new DegreesProp.Builder(360f).build();
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SweepGradient fromProto(
                ColorProto.@NonNull SweepGradient proto, @Nullable Fingerprint fingerprint) {
            return new SweepGradient(proto, fingerprint);
        }

        static @NonNull SweepGradient fromProto(ColorProto.@NonNull SweepGradient proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ColorProto.@NonNull SweepGradient toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ColorProto.@NonNull Brush toBrushProto() {
            return ColorProto.Brush.newBuilder().setSweepGradient(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "SweepGradient{"
                    + "colorStops="
                    + getColorStops()
                    + ", startAngle="
                    + getStartAngle()
                    + ", endAngle="
                    + getEndAngle()
                    + "}";
        }

        /** Builder for {@link SweepGradient}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Brush.Builder {
            private final ColorProto.SweepGradient.Builder mImpl =
                    ColorProto.SweepGradient.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1243672050);

            /**
             * Adds one item to the color stops defining how the colors are distributed around the
             * gradient center. The color sequence starts at the start angle and spans 360 degrees
             * clockwise, finishing at the same angle.
             *
             * <p>A color stop is a pair of a color and its offset in the gradient. The offset is
             * the relative position of the color, beginning with 0 from the start angle and ending
             * with 1.0 at the end angle, spanning clockwise.
             *
             * <p>There must be at least 2 colors and at most 10 colors.
             *
             * <p>If offset values are not set, the colors are evenly distributed in the gradient.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            private @NonNull Builder addColorStop(@NonNull ColorStop colorStop) {
                mImpl.addColorStops(colorStop.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(colorStop.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the start angle of the gradient relative to the element's base angle. If not
             * set, defaults to zero.
             *
             * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base
             * angle is the angle where the line starts. The value represents a relative position in
             * the line's length span. Values greater than 360 degrees correspond to upper layers of
             * the arc line as it wraps over itself.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setStartAngle(@NonNull DegreesProp startAngle) {
                mImpl.setStartAngle(startAngle.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(startAngle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the end angle of the gradient, relative to the element's base angle. If not set,
             * defaults to 360 degrees.
             *
             * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base
             * angle is the angle where the line starts. The value represents a relative position in
             * the line's length span. Values greater than 360 degrees correspond to upper layers of
             * the arc line as it wraps over itself.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setEndAngle(@NonNull DegreesProp endAngle) {
                mImpl.setEndAngle(endAngle.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(endAngle.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param colorStops The color stops defining how the colors are distributed around the
             *     gradient center.
             *     <p>A color stop is composed of a color and its offset in the gradient. The offset
             *     is the relative position of the color, beginning with 0 from the start angle and
             *     ending with 1.0 at the end angle, spanning clockwise.
             * @throws IllegalArgumentException if the number of colors is less than 2 or larger
             *     than 10.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @SafeVarargs
            public Builder(ColorStop @NonNull ... colorStops) {
                if (colorStops.length < 2 || colorStops.length > 10) {
                    throw new IllegalArgumentException(
                            "Size of colorStops must not be less than 2 or greater than 10. Got "
                                    + colorStops.length);
                }
                for (ColorStop colorStop : colorStops) {
                    addColorStop(colorStop);
                }
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * <p>The colors are evenly distributed in the gradient.
             *
             * @param colors The color sequence to be distributed around the gradient center. The
             *     color sequence is distributed between the gradient's start and end angles.
             * @throws IllegalArgumentException if the number of colors is less than 2 or larger
             *     than 10.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @SafeVarargs
            public Builder(ColorProp @NonNull ... colors) {
                if (colors.length < 2 || colors.length > 10) {
                    throw new IllegalArgumentException(
                            "Size of colors must not be less than 2 or greater than 10. Got "
                                    + colors.length);
                }
                for (ColorProp colorProp : colors) {
                    ColorStop stop = new ColorStop.Builder().setColor(colorProp).build();
                    addColorStop(stop);
                }
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if size of colorStops is less than 2 or greater than
             *     10.
             */
            @Override
            public @NonNull SweepGradient build() {
                int colorStopsCount = mImpl.getColorStopsCount();
                if (colorStopsCount < 2 || colorStopsCount > 10) {
                    throw new IllegalStateException(
                            "Size of colorStops must not be less than 2 or greater than 10");
                }
                return new SweepGradient(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A linear gradient with the provided colors based on defined start and end coordinates.
     *
     * <p>The colors are dispersed at the offsets defined in each color stop.
     */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public static final class LinearGradient implements Brush {
        private final ColorProto.LinearGradient mImpl;
        private final @Nullable Fingerprint mFingerprint;

        LinearGradient(ColorProto.LinearGradient impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the color stops defining how the colors are distributed from the start to the end
         * coordinates.
         *
         * <p>A color stop is a pair of a color and its offset in the gradient. The offset is the
         * relative position of the color, beginning with 0 at the start coordinate and ending with
         * 1.0 at the end coordinate.
         *
         * <p>There must be at least 2 colors and at most 10 colors.
         *
         * <p>If offset values are not set, the colors are evenly distributed in the gradient.
         */
        public @NonNull List<ColorStop> getColorStops() {
            List<ColorStop> list = new ArrayList<>();
            for (ColorProto.ColorStop item : mImpl.getColorStopsList()) {
                list.add(ColorStop.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the starting x position of the linear gradient. Defaults to the left side of the
         * element.
         */
        public @Nullable OffsetDimension getStartX() {
            if (mImpl.hasStartX()) {
                return DimensionBuilders.offsetDimensionFromProto(mImpl.getStartX());
            } else {
                return null;
            }
        }

        /**
         * Gets the starting y position of the linear gradient. Defaults to the top side of the
         * element.
         */
        public @Nullable OffsetDimension getStartY() {
            if (mImpl.hasStartY()) {
                return DimensionBuilders.offsetDimensionFromProto(mImpl.getStartY());
            } else {
                return null;
            }
        }

        /**
         * Gets the ending y position of the linear gradient. Defaults to the right side of the
         * element.
         */
        public @Nullable OffsetDimension getEndX() {
            if (mImpl.hasEndX()) {
                return DimensionBuilders.offsetDimensionFromProto(mImpl.getEndX());
            } else {
                return null;
            }
        }

        /**
         * Gets the ending y position of the linear gradient. Defaults to the top side of the
         * element.
         */
        public @Nullable OffsetDimension getEndY() {
            if (mImpl.hasEndY()) {
                return DimensionBuilders.offsetDimensionFromProto(mImpl.getEndY());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull LinearGradient fromProto(
                ColorProto.@NonNull LinearGradient proto, @Nullable Fingerprint fingerprint) {
            return new LinearGradient(proto, fingerprint);
        }

        static @NonNull LinearGradient fromProto(ColorProto.@NonNull LinearGradient proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        ColorProto.@NonNull LinearGradient toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ColorProto.@NonNull Brush toBrushProto() {
            return ColorProto.Brush.newBuilder().setLinearGradient(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "LinearGradient{"
                    + "colorStops="
                    + getColorStops()
                    + ", startX="
                    + getStartX()
                    + ", startY="
                    + getStartY()
                    + ", endX="
                    + getEndX()
                    + ", endY="
                    + getEndY()
                    + "}";
        }

        /** Builder for {@link LinearGradient}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements Brush.Builder {
            private final ColorProto.LinearGradient.Builder mImpl =
                    ColorProto.LinearGradient.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1075994710);

            /**
             * Adds one item to the color stops defining how the colors are distributed from the
             * start to the end coordinates.
             *
             * <p>A color stop is a pair of a color and its offset in the gradient. The offset is
             * the relative position of the color, beginning with 0 at the start coordinate and
             * ending with 1.0 at the end coordinate.
             *
             * <p>There must be at least 2 colors and at most 10 colors.
             *
             * <p>If offset values are not set, the colors are evenly distributed in the gradient.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            private @NonNull Builder addColorStop(@NonNull ColorStop colorStop) {
                mImpl.addColorStops(colorStop.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(colorStop.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the starting x position of the linear gradient. Defaults to the left side of the
             * element.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setStartX(@NonNull OffsetDimension startX) {
                mImpl.setStartX(startX.toOffsetDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(startX.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the starting y position of the linear gradient. Defaults to the top side of the
             * element.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setStartY(@NonNull OffsetDimension startY) {
                mImpl.setStartY(startY.toOffsetDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(startY.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the ending y position of the linear gradient. Defaults to the right side of the
             * element.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setEndX(@NonNull OffsetDimension endX) {
                mImpl.setEndX(endX.toOffsetDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(endX.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the ending y position of the linear gradient. Defaults to the top side of the
             * element.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setEndY(@NonNull OffsetDimension endY) {
                mImpl.setEndY(endY.toOffsetDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(endY.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param colorStops The color stops defining how the colors are distributed from the
             *     start to the end coordinates.
             *     <p>A color stop is a pair of a color and its offset in the gradient. The offset
             *     is the relative position of the color, beginning with 0 from the start coordinate
             *     and ending with 1.0 at the end coordinate.
             * @throws IllegalArgumentException if the number of colors is less than 2 or larger
             *     than 10.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            @SafeVarargs
            public Builder(ColorStop @NonNull ... colorStops) {
                if (colorStops.length < 2 || colorStops.length > 10) {
                    throw new IllegalArgumentException(
                            "Size of colorStops must not be less than 2 or greater than 10. Got "
                                    + colorStops.length);
                }
                for (ColorStop colorStop : colorStops) {
                    addColorStop(colorStop);
                }
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * <p>The colors are evenly distributed in the gradient.
             *
             * @param colors The color sequence to be distributed between the gradient's start and
             *     end coordinates.
             * @throws IllegalArgumentException if the number of colors is less than 2 or larger
             *     than 10.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            @SafeVarargs
            public Builder(ColorProp @NonNull ... colors) {
                if (colors.length < 2 || colors.length > 10) {
                    throw new IllegalArgumentException(
                            "Size of colors must not be less than 2 or greater than 10. Got "
                                    + colors.length);
                }
                for (ColorProp colorProp : colors) {
                    ColorStop stop = new ColorStop.Builder().setColor(colorProp).build();
                    addColorStop(stop);
                }
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if size of colorStops is less than 2 or greater than
             *     10.
             */
            @Override
            public @NonNull LinearGradient build() {
                int colorStopsCount = mImpl.getColorStopsCount();
                if (colorStopsCount < 2 || colorStopsCount > 10) {
                    throw new IllegalStateException(
                            "Size of colorStops must not be less than 2 or greater than 10");
                }
                return new LinearGradient(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a {@link Brush} describes how something is drawn on screen. It determines
     * the color(s) that are drawn in the drawing area.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public interface Brush {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ColorProto.@NonNull Brush toBrushProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link Brush} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull Brush build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull Brush brushFromProto(
            ColorProto.@NonNull Brush proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasSweepGradient()) {
            return SweepGradient.fromProto(proto.getSweepGradient(), fingerprint);
        }
        if (proto.hasLinearGradient()) {
            return LinearGradient.fromProto(proto.getLinearGradient(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of Brush");
    }

    static @NonNull Brush brushFromProto(ColorProto.@NonNull Brush proto) {
        return brushFromProto(proto, null);
    }
}
