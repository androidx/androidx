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

import static androidx.annotation.Dimension.DP;
import static androidx.annotation.Dimension.SP;
import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import androidx.annotation.Dimension;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.ExperimentalProtoLayoutExtensionApi;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.DimensionProto;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Builders for dimensions for layout elements. */
public final class DimensionBuilders {
    private DimensionBuilders() {}

    private static final ExpandedDimensionProp EXPAND = new ExpandedDimensionProp.Builder().build();
    private static final WrappedDimensionProp WRAP = new WrappedDimensionProp.Builder().build();

    /** Shortcut for building a {@link DpProp} using a measurement in DP. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull DpProp dp(@Dimension(unit = DP) float valueDp) {
        return new DpProp.Builder(valueDp).build();
    }

    /** Shortcut for building a {@link SpProp} using a measurement in SP. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull SpProp sp(@Dimension(unit = SP) float valueSp) {
        return new SpProp.Builder().setValue(valueSp).build();
    }

    /** Shortcut for building a {@link EmProp} using a measurement in EM. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull EmProp em(int valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /** Shortcut for building a {@link EmProp} using a measurement in EM. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull EmProp em(float valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /** Shortcut for building an {@link DegreesProp} using a measurement in degrees. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull DegreesProp degrees(float valueDegrees) {
        return new DegreesProp.Builder(valueDegrees).build();
    }

    /**
     * Shortcut for building an {@link ExpandedDimensionProp} that will expand to the size of its
     * parent.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull ExpandedDimensionProp expand() {
        return EXPAND;
    }

    /**
     * Shortcut for building an {@link ExpandedDimensionProp} with weight (a dimensionless scalar
     * value).
     *
     * <p>This will only affect the width of children of a {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Row} or the height of children of a {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Column}, otherwise it will expand to the size
     * of its parent. Where applicable, the remaining space in the width or height left from the
     * children with fixed or wrapped dimension will be proportionally split across children with
     * expand dimension, meaning that the width or height of the element is proportional to the sum
     * of the weights of its weighted siblings. For the siblings that don't have weight set, but
     * they are expanded, defaults to 1.
     */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static @NonNull ExpandedDimensionProp weight(@FloatRange(from = 0.0) float weight) {
        return new ExpandedDimensionProp.Builder()
                .setLayoutWeight(new FloatProp.Builder(weight).build())
                .build();
    }

    /**
     * Shortcut for building an {@link WrappedDimensionProp} that will shrink to the size of its
     * children.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static @NonNull WrappedDimensionProp wrap() {
        return WRAP;
    }

    /** A type for linear dimensions, measured in dp. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @OptIn(markerClass = ExperimentalProtoLayoutExtensionApi.class)
    public static final class DpProp
            implements AngularDimension,
                    ContainerDimension,
                    ImageDimension,
                    SpacerDimension,
                    ExtensionDimension,
                    PivotDimension,
                    OffsetDimension {
        private final DimensionProto.DpProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        DpProp(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value, in dp. If a dynamic value is also set and the renderer supports
         * dynamic values for the corresponding field, this static value will be ignored. If the
         * static value is not specified, zero will be used instead.
         */
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value, in dp. Note that when setting this value, the static value is
         * still required to be set to support older renderers that only read the static value. If
         * {@code dynamicValue} has an invalid result, the provided static value will be used
         * instead.
         */
        public @Nullable DynamicFloat getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getDynamicValue());
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
        public static @NonNull DpProp fromProto(
                DimensionProto.@NonNull DpProp proto, @Nullable Fingerprint fingerprint) {
            return new DpProp(proto, fingerprint);
        }

        static @NonNull DpProp fromProto(DimensionProto.@NonNull DpProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull DpProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull AngularDimension toAngularDimensionProto() {
            return DimensionProto.AngularDimension.newBuilder().setDp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull SpacerDimension toSpacerDimensionProto() {
            return DimensionProto.SpacerDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @ExperimentalProtoLayoutExtensionApi
        public DimensionProto.@NonNull ExtensionDimension toExtensionDimensionProto() {
            return DimensionProto.ExtensionDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull PivotDimension toPivotDimensionProto() {
            return DimensionProto.PivotDimension.newBuilder().setOffsetDp(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull OffsetDimension toOffsetDimensionProto() {
            return DimensionProto.OffsetDimension.newBuilder().setOffsetDp(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "DpProp{" + "value=" + getValue() + ", dynamicValue=" + getDynamicValue() + "}";
        }

        /** Builder for {@link DpProp}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder
                implements AngularDimension.Builder,
                        ContainerDimension.Builder,
                        ImageDimension.Builder,
                        SpacerDimension.Builder,
                        ExtensionDimension.Builder,
                        PivotDimension.Builder,
                        OffsetDimension.Builder {
            private final DimensionProto.DpProp.Builder mImpl = DimensionProto.DpProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(756413087);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicFloat)} can be used to provide a dynamic value.
             */
            public Builder(@Dimension(unit = DP) float staticValue) {
                setValue(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(float)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static value, in dp. If a dynamic value is also set and the renderer
             * supports dynamic values for the corresponding field, this static value will be
             * ignored. If the static value is not specified, zero will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Sets the dynamic value, in dp. Note that when setting this value, the static value is
             * still required to be set to support older renderers that only read the static value.
             * If {@code dynamicValue} has an invalid result, the provided static value will be used
             * instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setDynamicValue(@NonNull DynamicFloat dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicFloat)} but neither {@link #Builder(float)} nor {@link
             *     #setValue(float)} is used to provide a static value.
             */
            @Override
            public @NonNull DpProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new DpProp(mImpl.build(), mFingerprint);
            }
        }
    }

    private static class DpPropLayoutConstraint {
        protected final DimensionProto.DpProp mImpl;
        protected final @Nullable Fingerprint mFingerprint;

        protected DpPropLayoutConstraint(
                DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value to use when laying out components which can have a dynamic value.
         * Constrains the layout so that components are not changing size or location regardless of
         * the dynamic value that is being provided.
         */
        @SuppressWarnings("Unused")
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValueForLayout();
        }

        @SuppressWarnings("Unused")
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @SuppressWarnings("Unused")
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull SpacerDimension toSpacerDimensionProto() {
            return DimensionProto.SpacerDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        /** Builder for {@link DpPropLayoutConstraint}. */
        protected static class Builder {
            protected final DimensionProto.DpProp.Builder mImpl =
                    DimensionProto.DpProp.newBuilder();
            protected final Fingerprint mFingerprint = new Fingerprint(756413088);

            /**
             * Creates a new builder for {@link DpPropLayoutConstraint}.
             *
             * @param value Sets the value to use when laying out components which can have a
             *     dynamic value. Constrains the layout so that components are not changing size or
             *     location regardless of the dynamic value that is being provided.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            protected Builder(@Dimension(unit = DP) float value) {
                setValue(value);
            }

            /**
             * Sets the value to use when laying out components which can have a dynamic value.
             * Constrains the layout so that components are not changing size or location regardless
             * of the dynamic value that is being provided.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            private @NonNull Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValueForLayout(value);
                mFingerprint.recordPropertyUpdate(3, Float.floatToIntBits(value));
                return this;
            }
        }
    }

    /**
     * A type for specifying horizontal layout constraints when using {@link DpProp} on a data
     * bindable layout element.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @SuppressWarnings("PrivateSuperclass")
    public static final class HorizontalLayoutConstraint extends DpPropLayoutConstraint {
        HorizontalLayoutConstraint(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            super(impl, fingerprint);
        }

        /**
         * Gets the horizontal alignment of the actual content within the space reserved by value.
         */
        @LayoutElementBuilders.HorizontalAlignment
        public int getHorizontalAlignment() {
            return mImpl.getHorizontalAlignmentForLayoutValue();
        }

        static @NonNull HorizontalLayoutConstraint fromProto(DimensionProto.@NonNull DpProp proto) {
            return new HorizontalLayoutConstraint(proto, null);
        }

        /** Builder for {@link HorizontalLayoutConstraint}. */
        public static final class Builder extends DpPropLayoutConstraint.Builder {
            /**
             * Creates a new builder for {@link HorizontalLayoutConstraint}.
             *
             * @param value Sets the value to use when laying out components which can have a
             *     dynamic value. Constrains the layout so that components are not changing size or
             *     location regardless of the dynamic value that is being provided.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder(@Dimension(unit = DP) float value) {
                super(value);
            }

            /**
             * Sets the horizontal alignment of the actual content within the space reserved by
             * value. If not specified, defaults to center alignment.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setHorizontalAlignment(
                    @LayoutElementBuilders.HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignmentForLayoutValue(horizontalAlignment);
                mFingerprint.recordPropertyUpdate(5, horizontalAlignment);
                return this;
            }

            /** Builds an instance of {@link HorizontalLayoutConstraint}. */
            public @NonNull HorizontalLayoutConstraint build() {
                return new HorizontalLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for specifying vertical layout constraints when using {@link DpProp} on a data
     * bindable layout element.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @SuppressWarnings("PrivateSuperclass")
    public static final class VerticalLayoutConstraint extends DpPropLayoutConstraint {
        VerticalLayoutConstraint(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            super(impl, fingerprint);
        }

        /** Gets the vertical alignment of the actual content within the space reserved by value. */
        @LayoutElementBuilders.VerticalAlignment
        public int getVerticalAlignment() {
            return mImpl.getVerticalAlignmentForLayoutValue();
        }

        static @NonNull VerticalLayoutConstraint fromProto(DimensionProto.@NonNull DpProp proto) {
            return new VerticalLayoutConstraint(proto, null);
        }

        /** Builder for {@link VerticalLayoutConstraint}. */
        public static final class Builder extends DpPropLayoutConstraint.Builder {
            /**
             * Creates a new builder for {@link VerticalLayoutConstraint}.
             *
             * @param value Sets the value to use when laying out components which can have a
             *     dynamic value. Constrains the layout so that components are not changing size or
             *     location regardless of the dynamic value that is being provided.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder(@Dimension(unit = DP) float value) {
                super(value);
            }

            /**
             * Sets the vertical alignment of the actual content within the space reserved by value.
             * If not specified, defaults to center alignment.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setVerticalAlignment(
                    @LayoutElementBuilders.VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignmentForLayoutValue(verticalAlignment);
                mFingerprint.recordPropertyUpdate(4, verticalAlignment);
                return this;
            }

            /** Builds an instance of {@link VerticalLayoutConstraint}. */
            public @NonNull VerticalLayoutConstraint build() {
                return new VerticalLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A type for font sizes, measured in sp. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class SpProp {
        private final DimensionProto.SpProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SpProp(DimensionProto.SpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value, in sp. */
        @Dimension(unit = SP)
        public float getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SpProp fromProto(
                DimensionProto.@NonNull SpProp proto, @Nullable Fingerprint fingerprint) {
            return new SpProp(proto, fingerprint);
        }

        static @NonNull SpProp fromProto(DimensionProto.@NonNull SpProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull SpProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "SpProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link SpProp} */
        public static final class Builder {
            private final DimensionProto.SpProp.Builder mImpl = DimensionProto.SpProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(631793260);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value, in sp. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(@Dimension(unit = SP) float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull SpProp build() {
                return new SpProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A type for font spacing, measured in em. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class EmProp {
        private final DimensionProto.EmProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        EmProp(DimensionProto.EmProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the value, in em. */
        public float getValue() {
            return mImpl.getValue();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull EmProp fromProto(
                DimensionProto.@NonNull EmProp proto, @Nullable Fingerprint fingerprint) {
            return new EmProp(proto, fingerprint);
        }

        static @NonNull EmProp fromProto(DimensionProto.@NonNull EmProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull EmProp toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "EmProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link EmProp} */
        public static final class Builder {
            private final DimensionProto.EmProp.Builder mImpl = DimensionProto.EmProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-659639046);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the value, in em. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull EmProp build() {
                return new EmProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining the length of an arc element. */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public interface AngularDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull AngularDimension toAngularDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link AngularDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull AngularDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull AngularDimension angularDimensionFromProto(
            DimensionProto.@NonNull AngularDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasDegrees()) {
            return DegreesProp.fromProto(proto.getDegrees(), fingerprint);
        }
        if (proto.hasDp()) {
            return DpProp.fromProto(proto.getDp(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of AngularDimension");
    }

    static @NonNull AngularDimension angularDimensionFromProto(
            DimensionProto.@NonNull AngularDimension proto) {
        return angularDimensionFromProto(proto, null);
    }

    /** A type for angular dimensions, measured in degrees. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class DegreesProp implements AngularDimension {
        private final DimensionProto.DegreesProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        DegreesProp(DimensionProto.DegreesProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value, in degrees. If a dynamic value is also set and the renderer
         * supports dynamic values for the corresponding field, this static value will be ignored.
         * If the static value is not specified, zero will be used instead.
         */
        public float getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value, in degrees. Note that when setting this value, the static value
         * is still required to be set to support older renderers that only read the static value.
         * If {@code dynamicValue} has an invalid result, the provided static value will be used
         * instead.
         */
        public @Nullable DynamicFloat getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getDynamicValue());
            } else {
                return null;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull DegreesProp fromProto(
                DimensionProto.@NonNull DegreesProp proto, @Nullable Fingerprint fingerprint) {
            return new DegreesProp(proto, fingerprint);
        }

        static @NonNull DegreesProp fromProto(DimensionProto.@NonNull DegreesProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        DimensionProto.@NonNull DegreesProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull AngularDimension toAngularDimensionProto() {
            return DimensionProto.AngularDimension.newBuilder().setDegrees(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "DegreesProp{"
                    + "value="
                    + getValue()
                    + ", dynamicValue="
                    + getDynamicValue()
                    + "}";
        }

        /** Builder for {@link DegreesProp} */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements AngularDimension.Builder {
            private final DimensionProto.DegreesProp.Builder mImpl =
                    DimensionProto.DegreesProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1927567665);

            /**
             * Creates an instance of {@link Builder} from the given static value. {@link
             * #setDynamicValue(DynamicFloat)} can be used to provide a dynamic value.
             */
            public Builder(float staticValue) {
                setValue(staticValue);
            }

            /**
             * Creates an instance of {@link Builder}.
             *
             * @deprecated use {@link #Builder(float)}
             */
            @Deprecated
            public Builder() {}

            /**
             * Sets the static value, in degrees. If a dynamic value is also set and the renderer
             * supports dynamic values for the corresponding field, this static value will be
             * ignored. If the static value is not specified, zero will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Sets the dynamic value, in degrees. Note that when setting this value, the static
             * value is still required to be set to support older renderers that only read the
             * static value. If {@code dynamicValue} has an invalid result, the provided static
             * value will be used instead.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setDynamicValue(@NonNull DynamicFloat dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Builds an instance from accumulated values.
             *
             * @throws IllegalStateException if a dynamic value is set using {@link
             *     #setDynamicValue(DynamicFloat)} but neither {@link #Builder(float)} nor {@link
             *     #setValue(float)} is used to provide a static value.
             */
            @Override
            public @NonNull DegreesProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new DegreesProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for specifying layout constraints when using {@link DegreesProp} on a data bindable
     * layout element.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final class AngularLayoutConstraint {
        private final DimensionProto.DegreesProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AngularLayoutConstraint(
                DimensionProto.DegreesProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fixed value to reserve the space when used on a layout-changing data bind. If
         * not set defaults to the static value of the associated {@link DegreesProp} field.
         */
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValueForLayout();
        }

        /** Gets angular alignment of the actual content within the space reserved by value. */
        @LayoutElementBuilders.AngularAlignment
        public int getAngularAlignment() {
            return mImpl.getAngularAlignmentForLayoutValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull DegreesProp toProto() {
            return mImpl;
        }

        static @NonNull AngularLayoutConstraint fromProto(
                DimensionProto.@NonNull DegreesProp proto) {
            return new AngularLayoutConstraint(proto, null);
        }

        /** Builder for {@link AngularLayoutConstraint}. */
        public static final class Builder {
            private final DimensionProto.DegreesProp.Builder mImpl =
                    DimensionProto.DegreesProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1927567664);

            /**
             * Creates a new builder for {@link AngularLayoutConstraint}.
             *
             * @param value Sets the fixed value to reserve the space when used on a layout-changing
             *     data bind.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public Builder(@Dimension(unit = DP) float value) {
                setValue(value);
            }

            /**
             * Sets the fixed value to reserve the space when used on a layout-changing data bind.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            private @NonNull Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValueForLayout(value);
                mFingerprint.recordPropertyUpdate(3, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Sets angular alignment of the actual content within the space reserved by value. If
             * not specified, defaults to center alignment.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAngularAlignment(
                    @LayoutElementBuilders.AngularAlignment int angularAlignment) {
                mImpl.setAngularAlignmentForLayoutValue(angularAlignment);
                mFingerprint.recordPropertyUpdate(4, angularAlignment);
                return this;
            }

            /** Builds an instance of {@link AngularLayoutConstraint}. */
            public @NonNull AngularLayoutConstraint build() {
                return new AngularLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for a dimension that fills all the space it can (i.e. MATCH_PARENT in Android
     * parlance).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ExpandedDimensionProp
            implements ContainerDimension, ImageDimension, SpacerDimension {
        private final DimensionProto.ExpandedDimensionProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ExpandedDimensionProp(
                DimensionProto.ExpandedDimensionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the layout weight (a dimensionless scalar value) for this element. This will only
         * affect the width of children of a {@link
         * androidx.wear.protolayout.LayoutElementBuilders.Row} or the height of children of a
         * {@link androidx.wear.protolayout.LayoutElementBuilders.Column}. By default, all children
         * have equal weight. Where applicable, the width or height of the element is proportional
         * to the sum of the weights of its siblings.
         *
         * <p>Note that negative values are not supported and it can lead to unexpected behaviour.
         */
        public @Nullable FloatProp getLayoutWeight() {
            if (mImpl.hasLayoutWeight()) {
                return FloatProp.fromProto(mImpl.getLayoutWeight());
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
        public static @NonNull ExpandedDimensionProp fromProto(
                DimensionProto.@NonNull ExpandedDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ExpandedDimensionProp(proto, fingerprint);
        }

        static @NonNull ExpandedDimensionProp fromProto(
                DimensionProto.@NonNull ExpandedDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull ExpandedDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder()
                    .setExpandedDimension(mImpl)
                    .build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder().setExpandedDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull SpacerDimension toSpacerDimensionProto() {
            return DimensionProto.SpacerDimension.newBuilder().setExpandedDimension(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "ExpandedDimensionProp{" + "layoutWeight=" + getLayoutWeight() + "}";
        }

        /** Builder for {@link ExpandedDimensionProp}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder
                implements ContainerDimension.Builder,
                        ImageDimension.Builder,
                        SpacerDimension.Builder {
            private final DimensionProto.ExpandedDimensionProp.Builder mImpl =
                    DimensionProto.ExpandedDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-997720604);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the layout weight (a dimensionless scalar value) for this element. This will
             * only affect the width of children of a {@link
             * androidx.wear.protolayout.LayoutElementBuilders.Row} or the height of children of a
             * {@link androidx.wear.protolayout.LayoutElementBuilders.Column}. By default, all
             * children have equal weight. Where applicable, the width or height of the element is
             * proportional to the sum of the weights of its siblings.
             *
             * <p>Note that negative values are not supported and it can lead to unexpected
             * behaviour.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setLayoutWeight(@NonNull FloatProp layoutWeight) {
                if (layoutWeight.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "ExpandedDimensionProp.Builder.setLayoutWeight doesn't support dynamic"
                                    + " values.");
                }
                mImpl.setLayoutWeight(layoutWeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(layoutWeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ExpandedDimensionProp build() {
                return new ExpandedDimensionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for a dimension that sizes itself to the size of its children (i.e. WRAP_CONTENT in
     * Android parlance).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class WrappedDimensionProp implements ContainerDimension {
        private final DimensionProto.WrappedDimensionProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        WrappedDimensionProp(
                DimensionProto.WrappedDimensionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the minimum size of this dimension. If not set, then there is no minimum size. */
        public @Nullable DpProp getMinimumSize() {
            if (mImpl.hasMinimumSize()) {
                return DpProp.fromProto(mImpl.getMinimumSize());
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
        public static @NonNull WrappedDimensionProp fromProto(
                DimensionProto.@NonNull WrappedDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new WrappedDimensionProp(proto, fingerprint);
        }

        static @NonNull WrappedDimensionProp fromProto(
                DimensionProto.@NonNull WrappedDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull WrappedDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder()
                    .setWrappedDimension(mImpl)
                    .build();
        }

        @Override
        public @NonNull String toString() {
            return "WrappedDimensionProp{" + "minimumSize=" + getMinimumSize() + "}";
        }

        /** Builder for {@link WrappedDimensionProp}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ContainerDimension.Builder {
            private final DimensionProto.WrappedDimensionProp.Builder mImpl =
                    DimensionProto.WrappedDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1118918114);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the minimum size of this dimension. If not set, then there is no minimum size.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setMinimumSize(@NonNull DpProp minimumSize) {
                if (minimumSize.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "WrappedDimensionProp.Builder.setMinimumSize doesn't support dynamic"
                                    + " values.");
                }
                mImpl.setMinimumSize(minimumSize.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(minimumSize.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull WrappedDimensionProp build() {
                return new WrappedDimensionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for a dimension that scales itself proportionally to another dimension such that the
     * aspect ratio defined by the given width and height values is preserved.
     *
     * <p>Note that the width and height are unitless; only their ratio is relevant. This allows for
     * specifying an element's size using common ratios (e.g. width=4, height=3), or to allow an
     * element to be resized proportionally based on the size of an underlying asset (e.g. an
     * 800x600 image being added to a smaller container and resized accordingly).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ProportionalDimensionProp implements ImageDimension {
        private final DimensionProto.ProportionalDimensionProp mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ProportionalDimensionProp(
                DimensionProto.ProportionalDimensionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the width to be used when calculating the aspect ratio to preserve. */
        @IntRange(from = 0)
        public int getAspectRatioWidth() {
            return mImpl.getAspectRatioWidth();
        }

        /** Gets the height to be used when calculating the aspect ratio ratio to preserve. */
        @IntRange(from = 0)
        public int getAspectRatioHeight() {
            return mImpl.getAspectRatioHeight();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ProportionalDimensionProp fromProto(
                DimensionProto.@NonNull ProportionalDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ProportionalDimensionProp(proto, fingerprint);
        }

        static @NonNull ProportionalDimensionProp fromProto(
                DimensionProto.@NonNull ProportionalDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull ProportionalDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder()
                    .setProportionalDimension(mImpl)
                    .build();
        }

        @Override
        public @NonNull String toString() {
            return "ProportionalDimensionProp{"
                    + "aspectRatioWidth="
                    + getAspectRatioWidth()
                    + ", aspectRatioHeight="
                    + getAspectRatioHeight()
                    + "}";
        }

        /** Builder for {@link ProportionalDimensionProp}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements ImageDimension.Builder {
            private final DimensionProto.ProportionalDimensionProp.Builder mImpl =
                    DimensionProto.ProportionalDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1725027476);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the width to be used when calculating the aspect ratio to preserve. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAspectRatioWidth(@IntRange(from = 0) int aspectRatioWidth) {
                mImpl.setAspectRatioWidth(aspectRatioWidth);
                mFingerprint.recordPropertyUpdate(1, aspectRatioWidth);
                return this;
            }

            /** Sets the height to be used when calculating the aspect ratio ratio to preserve. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setAspectRatioHeight(
                    @IntRange(from = 0) int aspectRatioHeight) {
                mImpl.setAspectRatioHeight(aspectRatioHeight);
                mFingerprint.recordPropertyUpdate(2, aspectRatioHeight);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull ProportionalDimensionProp build() {
                return new ProportionalDimensionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining a dimension that can be applied to a container. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface ContainerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull ContainerDimension toContainerDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link ContainerDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull ContainerDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull ContainerDimension containerDimensionFromProto(
            DimensionProto.@NonNull ContainerDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        if (proto.hasExpandedDimension()) {
            return ExpandedDimensionProp.fromProto(proto.getExpandedDimension(), fingerprint);
        }
        if (proto.hasWrappedDimension()) {
            return WrappedDimensionProp.fromProto(proto.getWrappedDimension(), fingerprint);
        }
        throw new IllegalStateException(
                "Proto was not a recognised instance of ContainerDimension");
    }

    static @NonNull ContainerDimension containerDimensionFromProto(
            DimensionProto.@NonNull ContainerDimension proto) {
        return containerDimensionFromProto(proto, null);
    }

    /** Interface defining a dimension that can be applied to an image. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface ImageDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull ImageDimension toImageDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link ImageDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull ImageDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull ImageDimension imageDimensionFromProto(
            DimensionProto.@NonNull ImageDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        if (proto.hasExpandedDimension()) {
            return ExpandedDimensionProp.fromProto(proto.getExpandedDimension(), fingerprint);
        }
        if (proto.hasProportionalDimension()) {
            return ProportionalDimensionProp.fromProto(
                    proto.getProportionalDimension(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of ImageDimension");
    }

    static @NonNull ImageDimension imageDimensionFromProto(
            DimensionProto.@NonNull ImageDimension proto) {
        return imageDimensionFromProto(proto, null);
    }

    /** Interface defining a dimension that can be applied to a spacer. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public interface SpacerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull SpacerDimension toSpacerDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link SpacerDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull SpacerDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull SpacerDimension spacerDimensionFromProto(
            DimensionProto.@NonNull SpacerDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        if (proto.hasExpandedDimension()) {
            return ExpandedDimensionProp.fromProto(proto.getExpandedDimension(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of SpacerDimension");
    }

    static @NonNull SpacerDimension spacerDimensionFromProto(
            DimensionProto.@NonNull SpacerDimension proto) {
        return spacerDimensionFromProto(proto, null);
    }

    /**
     * Interface defining a dimension that can be applied to a {@link
     * androidx.wear.protolayout.LayoutElementBuilders.ExtensionLayoutElement} element.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    @ExperimentalProtoLayoutExtensionApi
    public interface ExtensionDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull ExtensionDimension toExtensionDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link ExtensionDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull ExtensionDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull ExtensionDimension extensionDimensionFromProto(
            DimensionProto.@NonNull ExtensionDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        throw new IllegalStateException(
                "Proto was not a recognised instance of ExtensionDimension");
    }

    static @NonNull ExtensionDimension extensionDimensionFromProto(
            DimensionProto.@NonNull ExtensionDimension proto) {
        return extensionDimensionFromProto(proto, null);
    }

    /** Provide a position representation proportional to the bounding box width/height. */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public static final class BoundingBoxRatio implements PivotDimension, OffsetDimension {
        private final DimensionProto.BoundingBoxRatio mImpl;
        private final @Nullable Fingerprint mFingerprint;

        BoundingBoxRatio(DimensionProto.BoundingBoxRatio impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the ratio proportional to the bounding box width/height. Value 0 represents the
         * location at the top / start of the bounding box, value 1 represents the location at the
         * bottom / end of the bounding box, and value 0.5 represents the middle of the bounding
         * box. Values outside [0, 1] are also valid. Dynamic value is supported.
         */
        public @NonNull FloatProp getRatio() {
            return FloatProp.fromProto(mImpl.getRatio());
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull BoundingBoxRatio fromProto(
                DimensionProto.@NonNull BoundingBoxRatio proto, @Nullable Fingerprint fingerprint) {
            return new BoundingBoxRatio(proto, fingerprint);
        }

        static @NonNull BoundingBoxRatio fromProto(DimensionProto.@NonNull BoundingBoxRatio proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        DimensionProto.@NonNull BoundingBoxRatio toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull PivotDimension toPivotDimensionProto() {
            return DimensionProto.PivotDimension.newBuilder().setLocationRatio(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public DimensionProto.@NonNull OffsetDimension toOffsetDimensionProto() {
            return DimensionProto.OffsetDimension.newBuilder().setLocationRatio(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "BoundingBoxRatio{" + "ratio=" + getRatio() + "}";
        }

        /** Builder for {@link BoundingBoxRatio}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder
                implements PivotDimension.Builder, OffsetDimension.Builder {
            private final DimensionProto.BoundingBoxRatio.Builder mImpl =
                    DimensionProto.BoundingBoxRatio.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1387873430);

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param ratio the ratio proportional to the bounding box width/height. Value 0
             *     represents the location at the top / start of the bounding box, value 1
             *     represents the location at the bottom / end of the bounding box, and value 0.5
             *     represents the middle of the bounding box. Values outside [0, 1] are also valid.
             *     Dynamic value is supported.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public Builder(@NonNull FloatProp ratio) {
                setRatio(ratio);
            }

            @RequiresSchemaVersion(major = 1, minor = 400)
            Builder() {}

            /**
             * Sets the ratio proportional to the bounding box width/height. Value 0 represents the
             * location at the top / start of the bounding box, value 1 represents the location at
             * the bottom / end of the bounding box, and value 0.5 represents the middle of the
             * bounding box. Values outside [0, 1] are also valid. Dynamic value is supported.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setRatio(@NonNull FloatProp ratio) {
                mImpl.setRatio(ratio.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(ratio.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull BoundingBoxRatio build() {
                return new BoundingBoxRatio(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dimension that can be applied to a pivot location for scale and rotate
     * transformations.
     */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public interface PivotDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull PivotDimension toPivotDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link PivotDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull PivotDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull PivotDimension pivotDimensionFromProto(
            DimensionProto.@NonNull PivotDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasOffsetDp()) {
            return DpProp.fromProto(proto.getOffsetDp(), fingerprint);
        }
        if (proto.hasLocationRatio()) {
            return BoundingBoxRatio.fromProto(proto.getLocationRatio(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of PivotDimension");
    }

    static @NonNull PivotDimension pivotDimensionFromProto(
            DimensionProto.@NonNull PivotDimension proto) {
        return pivotDimensionFromProto(proto, null);
    }

    /**
     * Interface defining a dimension that represents an offset relative to the element's position.
     */
    @RequiresSchemaVersion(major = 1, minor = 500)
    public interface OffsetDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        DimensionProto.@NonNull OffsetDimension toOffsetDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link OffsetDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull OffsetDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static @NonNull OffsetDimension offsetDimensionFromProto(
            DimensionProto.@NonNull OffsetDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasOffsetDp()) {
            return DpProp.fromProto(proto.getOffsetDp(), fingerprint);
        }
        if (proto.hasLocationRatio()) {
            return BoundingBoxRatio.fromProto(proto.getLocationRatio(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of OffsetDimension");
    }

    static @NonNull OffsetDimension offsetDimensionFromProto(
            DimensionProto.@NonNull OffsetDimension proto) {
        return offsetDimensionFromProto(proto, null);
    }
}
