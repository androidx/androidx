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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.DimensionBuilders.DegreesProp;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicColor;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.ColorProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builders for color utilities for layout elements. */
public final class ColorBuilders {
    private ColorBuilders() {}

    /**
     * Shortcut for building a {@link ColorProp} using an ARGB value.
     *
     * @since 1.0
     */
    @NonNull
    public static ColorProp argb(@ColorInt int colorArgb) {
        return new ColorProp.Builder(colorArgb).build();
    }

    /**
     * A property defining a color.
     *
     * @since 1.0
     */
    public static final class ColorProp {
        private final ColorProto.ColorProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ColorProp(ColorProto.ColorProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static color value, in ARGB format. If a dynamic value is also set and the
         * renderer supports dynamic values for the corresponding field, this static value will be
         * ignored. If the static value is not specified, zero (equivalent to {@link
         * Color#TRANSPARENT}) will be used instead.
         *
         * @since 1.0
         */
        @ColorInt
        public int getArgb() {
            return mImpl.getArgb();
        }

        /**
         * Gets the dynamic value. Note that when setting this value, the static value is still
         * required to be set to support older renderers that only read the static value. If {@code
         * dynamicValue} has an invalid result, the provided static value will be used instead.
         *
         * @since 1.2
         */
        @Nullable
        public DynamicColor getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicColorFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ColorProp fromProto(
                @NonNull ColorProto.ColorProp proto, @Nullable Fingerprint fingerprint) {
            return new ColorProp(proto, fingerprint);
        }

        @NonNull
        static ColorProp fromProto(@NonNull ColorProto.ColorProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ColorProto.ColorProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "ColorProp{" + "argb=" + getArgb() + ", dynamicValue=" + getDynamicValue() + "}";
        }

        /** Builder for {@link ColorProp} */
        public static final class Builder {
            private final ColorProto.ColorProp.Builder mImpl = ColorProto.ColorProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1955659823);

            /**
             * @deprecated Use {@link #Builder(int)} instead.
             */
            @Deprecated
            public Builder() {}

            public Builder(@ColorInt int argb) {
                setArgb(argb);
            }

            /**
             * Sets the static color value, in ARGB format. If a dynamic value is also set and the
             * renderer supports dynamic values for the corresponding field, this static value will
             * be ignored. If the static value is not specified, zero (equivalent to {@link
             * Color#TRANSPARENT}) will be used instead.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setArgb(@ColorInt int argb) {
                mImpl.setArgb(argb);
                mFingerprint.recordPropertyUpdate(1, argb);
                return this;
            }

            /**
             * Sets the dynamic value. Note that when setting this value, the static value is still
             * required to be set to support older renderers that only read the static value. If
             * {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicColor dynamicValue) {
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
            @NonNull
            public ColorProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasArgb()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new ColorProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A color and an offset, determining a color position in a gradient.
     *
     * @since 1.3
     */
    public static final class ColorStop {
        private final ColorProto.ColorStop mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ColorStop(ColorProto.ColorStop impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the color for this stop.
         *
         * @since 1.3
         */
        @Nullable
        public ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets the relative offset for this color, between 0 and 1. This determines where the color
         * is positioned relative to a gradient space.
         *
         * @since 1.3
         */
        @Nullable
        public FloatProp getOffset() {
            if (mImpl.hasOffset()) {
                return FloatProp.fromProto(mImpl.getOffset());
            } else {
                return null;
            }
        }

        /**
         * Constructor for {@link ColorStop}.
         *
         * @param color the color for this stop.
         *     <p>Note that this parameter only supports static values.
         * @since 1.3
         */
        public ColorStop(@NonNull ColorProp color) {
            ColorStop inst = new Builder().setColor(color).build();
            this.mImpl = inst.mImpl;
            this.mFingerprint = inst.mFingerprint;
        }

        /**
         * Constructor for {@link ColorStop}.
         *
         * <p>Note that all parameters only support static values.
         *
         * @param color the color for this stop.
         * @param offset the relative offset for this color, between 0 and 1. This determines where
         *     the color is positioned relative to a gradient space.
         * @since 1.3
         */
        public ColorStop(@NonNull ColorProp color, @NonNull FloatProp offset) {
            ColorStop inst = new Builder().setColor(color).setOffset(offset).build();
            this.mImpl = inst.mImpl;
            this.mFingerprint = inst.mFingerprint;
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ColorStop fromProto(
                @NonNull ColorProto.ColorStop proto, @Nullable Fingerprint fingerprint) {
            return new ColorStop(proto, fingerprint);
        }

        @NonNull
        static ColorStop fromProto(@NonNull ColorProto.ColorStop proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ColorProto.ColorStop toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "ColorStop{" + "color=" + getColor() + ", offset=" + getOffset() + "}";
        }

        /** Builder for {@link ColorStop} */
        static final class Builder {
            private final ColorProto.ColorStop.Builder mImpl = ColorProto.ColorStop.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-468737254);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the color for this stop.
             *
             * <p>Note that this field only supports static values.
             *
             * @since 1.3
             */
            @NonNull
            Builder setColor(@NonNull ColorProp color) {
                if (color.getDynamicValue() != null) {
                    throw new IllegalArgumentException("setColor doesn't support dynamic values.");
                }
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
             * @since 1.3
             */
            @NonNull
            Builder setOffset(@NonNull FloatProp offset) {
                if (offset.getDynamicValue() != null) {
                    throw new IllegalArgumentException("setOffset doesn't support dynamic values.");
                }
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
            @NonNull
            public ColorStop build() {
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
     *
     * @since 1.3
     */
    public static final class SweepGradient implements Brush {
        private final ColorProto.SweepGradient mImpl;
        @Nullable private final Fingerprint mFingerprint;

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
         * after spanning 360 degrees clockwise.
         *
         * <p>There must be at least 2 colors.
         *
         * <p>If offset values are not set, the colors are evenly distributed in the gradient.
         *
         * <p>If the offset values are not monotonic, the drawing may produce unexpected results.
         *
         * @since 1.3
         */
        @NonNull
        public List<ColorStop> getColorStops() {
            List<ColorStop> list = new ArrayList<>();
            for (ColorProto.ColorStop item : mImpl.getColorStopsList()) {
                list.add(ColorStop.fromProto(item));
            }
            return Collections.unmodifiableList(list);
        }

        /**
         * Gets the angular shift relative to the element's parent base angle. This is used to shift
         * the start angle of the gradient.
         *
         * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base angle is
         * the angle where the line starts.
         *
         * @since 1.3
         */
        @Nullable
        public DegreesProp getAngularShift() {
            if (mImpl.hasAngularShift()) {
                return DegreesProp.fromProto(mImpl.getAngularShift());
            } else {
                return null;
            }
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SweepGradient fromProto(
                @NonNull ColorProto.SweepGradient proto, @Nullable Fingerprint fingerprint) {
            return new SweepGradient(proto, fingerprint);
        }

        @NonNull
        static SweepGradient fromProto(@NonNull ColorProto.SweepGradient proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @NonNull
        ColorProto.SweepGradient toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ColorProto.Brush toBrushProto() {
            return ColorProto.Brush.newBuilder().setSweepGradient(mImpl).build();
        }

        @Override
        @NonNull
        public String toString() {
            return "SweepGradient{"
                    + "colorStops="
                    + getColorStops()
                    + ", angularShift="
                    + getAngularShift()
                    + "}";
        }

        /** Builder for {@link SweepGradient}. */
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
             * with 1.0 after spanning 360 degrees clockwise.
             *
             * <p>There must be at least 2 colors.
             *
             * <p>If offset values are not set, the colors are evenly distributed in the gradient.
             *
             * <p>If the offset values are not monotonic, the drawing may produce unexpected
             * results.
             *
             * @since 1.3
             */
            @NonNull
            private Builder addColorStop(@NonNull ColorStop colorStop) {
                mImpl.addColorStops(colorStop.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(colorStop.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the angular shift relative to the element's parent base angle. This is used to
             * shift the start angle of the gradient.
             *
             * <p>For {@link androidx.wear.protolayout.LayoutElementBuilders.ArcLine}, the base
             * angle is the angle where the line starts.
             *
             * <p>Note that this field only supports static values.
             *
             * @since 1.3
             */
            @NonNull
            public Builder setAngularShift(@NonNull DegreesProp angularShift) {
                if (angularShift.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "setAngularShift doesn't support dynamic values.");
                }
                mImpl.setAngularShift(angularShift.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(angularShift.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param colorStops The color stops defining how the colors are distributed around the
             *     gradient center. The color sequence starts at the start angle and spans 360
             *     degrees clockwise, finishing at the same angle.
             *     <p>A color stop is composed of a color and its offset in the gradient. The offset
             *     is the relative position of the color, beginning with 0 from the start angle and
             *     ending with 1.0 after spanning 360 degrees clockwise.
             *     <p>If offsets are not set, the colors are evenly distributed in the gradient.
             *     <p>If the offset values are not monotonic, the drawing may produce unexpected
             *     results.
             * @throws IllegalArgumentException if the number of colors is less than 2.
             * @throws IllegalArgumentException if offsets in {@code colorStops} are partially set.
             *     Either all or none of the {@link ColorStop} parameters should have an offset.
             * @since 1.3
             */
            @SafeVarargs
            public Builder(@NonNull ColorStop... colorStops) {
                if (colorStops.length < 2) {
                    throw new IllegalArgumentException(
                            "There must be at least 2 colors. Got " + colorStops.length);
                }
                boolean offsetsShouldBePresent = colorStops[0].getOffset() != null;
                for (ColorStop colorStop : colorStops) {
                    boolean stopHasOffset = colorStop.getOffset() != null;
                    if (offsetsShouldBePresent != stopHasOffset) {
                        throw new IllegalArgumentException(
                                "Either all or none of the colorStops should have an offset.");
                    }
                    addColorStop(colorStop);
                }
            }

            /** Builds an instance from accumulated values. */
            @Override
            @NonNull
            public SweepGradient build() {
                return new SweepGradient(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a {@link Brush} describes how something is drawn on screen. It determines
     * the color(s) that are drawn in the drawing area.
     *
     * @since 1.3
     */
    public interface Brush {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        ColorProto.Brush toBrushProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link Brush} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            Brush build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static Brush brushFromProto(
            @NonNull ColorProto.Brush proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasSweepGradient()) {
            return SweepGradient.fromProto(proto.getSweepGradient(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of Brush");
    }

    @NonNull
    static Brush brushFromProto(@NonNull ColorProto.Brush proto) {
        return brushFromProto(proto, null);
    }
}
