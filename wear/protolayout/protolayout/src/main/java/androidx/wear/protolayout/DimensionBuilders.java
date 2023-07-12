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
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat;
import androidx.wear.protolayout.expression.ExperimentalProtoLayoutExtensionApi;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.DimensionProto;

/** Builders for dimensions for layout elements. */
public final class DimensionBuilders {
    private DimensionBuilders() {}

    private static final ExpandedDimensionProp EXPAND = new ExpandedDimensionProp.Builder().build();
    private static final WrappedDimensionProp WRAP = new WrappedDimensionProp.Builder().build();

    /** Shortcut for building a {@link DpProp} using a measurement in DP. */
    @NonNull
    public static DpProp dp(@Dimension(unit = DP) float valueDp) {
        return new DpProp.Builder(valueDp).build();
    }

    /** Shortcut for building a {@link SpProp} using a measurement in SP. */
    @NonNull
    public static SpProp sp(@Dimension(unit = SP) float valueSp) {
        return new SpProp.Builder().setValue(valueSp).build();
    }

    /**
     * Shortcut for building a {@link EmProp} using a measurement in EM.
     *
     * @since 1.0
     */
    @NonNull
    public static EmProp em(int valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /**
     * Shortcut for building a {@link EmProp} using a measurement in EM.
     *
     * @since 1.0
     */
    @NonNull
    public static EmProp em(float valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /**
     * Shortcut for building an {@link DegreesProp} using a measurement in degrees.
     *
     * @since 1.0
     */
    @NonNull
    public static DegreesProp degrees(float valueDegrees) {
        return new DegreesProp.Builder(valueDegrees).build();
    }

    /**
     * Shortcut for building an {@link ExpandedDimensionProp} that will expand to the size of its
     * parent.
     *
     * @since 1.0
     */
    @NonNull
    public static ExpandedDimensionProp expand() {
        return EXPAND;
    }

    /**
     * Shortcut for building an {@link WrappedDimensionProp} that will shrink to the size of its
     * children.
     *
     * @since 1.0
     */
    @NonNull
    public static WrappedDimensionProp wrap() {
        return WRAP;
    }

    @OptIn(markerClass = ExperimentalProtoLayoutExtensionApi.class)
    public static final class DpProp
            implements ContainerDimension, ImageDimension, SpacerDimension, ExtensionDimension {
        private final DimensionProto.DpProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        DpProp(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value, in dp.
         *
         * @since 1.0
         */
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value, in dp.
         *
         * @since 1.2
         */
        @Nullable
        public DynamicFloat getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getDynamicValue());
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
        public static DpProp fromProto(
                @NonNull DimensionProto.DpProp proto, @Nullable Fingerprint fingerprint) {
            return new DpProp(proto, fingerprint);
        }

        @NonNull
        static DpProp fromProto(@NonNull DimensionProto.DpProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.DpProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.SpacerDimension toSpacerDimensionProto() {
            return DimensionProto.SpacerDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @ExperimentalProtoLayoutExtensionApi
        public DimensionProto.ExtensionDimension toExtensionDimensionProto() {
            return DimensionProto.ExtensionDimension.newBuilder().setLinearDimension(mImpl).build();
        }

        @Override
        @NonNull
        public String toString() {
            return "DpProp{" + "value=" + getValue() + ", dynamicValue=" + getDynamicValue() + "}";
        }

        /** Builder for {@link DpProp}. */
        public static final class Builder
                implements ContainerDimension.Builder,
                        ImageDimension.Builder,
                        SpacerDimension.Builder,
                        ExtensionDimension.Builder {
            private final DimensionProto.DpProp.Builder mImpl = DimensionProto.DpProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(756413087);

            /**
             * @deprecated Use {@link #Builder(float)} instead.
             */
            @Deprecated
            public Builder() {}

            /**
             * Creates a instance of {@link Builder}.
             *
             * @param staticValue the static value, in dp.
             */
            public Builder(@Dimension(unit = DP) float staticValue) {
                setValue(staticValue);
            }

            /**
             * Sets the static value, in dp. If a dynamic value is also set and the renderer
             * supports dynamic values for the corresponding field, this static value will be
             * ignored.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(@Dimension(unit = DP) float staticValue) {
                mImpl.setValue(staticValue);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(staticValue));
                return this;
            }

            /**
             * Sets the dynamic value, in dp. Note that when setting this value, the static value is
             * still required to be set to support older renderers that only read the static value.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicFloat dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public DpProp build() {
                if (mImpl.hasDynamicValue() && !mImpl.hasValue()) {
                    throw new IllegalStateException("Static value is missing.");
                }
                return new DpProp(mImpl.build(), mFingerprint);
            }
        }
    }

    private static class DpPropLayoutConstraint {
        protected final DimensionProto.DpProp mImpl;
        @Nullable protected final Fingerprint mFingerprint;

        protected DpPropLayoutConstraint(
                DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value to use when laying out components which can have a dynamic value.
         * Constrains the layout so that components are not changing size or location regardless of
         * the dynamic value that is being provided.
         *
         * @since 1.2
         */
        @SuppressWarnings("Unused")
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValueForLayout();
        }

        @SuppressWarnings("Unused")
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @SuppressWarnings("Unused")
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.SpacerDimension toSpacerDimensionProto() {
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
             * @since 1.2
             */
            protected Builder(@Dimension(unit = DP) float value) {
                setValue(value);
            }

            /**
             * Sets the value to use when laying out components which can have a dynamic value.
             * Constrains the layout so that components are not changing size or location regardless
             * of the dynamic value that is being provided.
             *
             * @since 1.2
             */
            @NonNull
            private Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValueForLayout(value);
                mFingerprint.recordPropertyUpdate(3, Float.floatToIntBits(value));
                return this;
            }
        }
    }

    /**
     * A type for specifying horizontal layout constraints when using {@link DpProp} on a data
     * bindable layout element.
     *
     * @since 1.2
     */
    public static final class HorizontalLayoutConstraint extends DpPropLayoutConstraint {
        HorizontalLayoutConstraint(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            super(impl, fingerprint);
        }

        /**
         * Gets the horizontal alignment of the actual content within the space reserved by value.
         *
         * @since 1.2
         */
        @LayoutElementBuilders.HorizontalAlignment
        public int getHorizontalAlignment() {
            return mImpl.getHorizontalAlignmentForLayoutValue();
        }

        @NonNull
        static HorizontalLayoutConstraint fromProto(@NonNull DimensionProto.DpProp proto) {
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
             * @since 1.2
             */
            public Builder(@Dimension(unit = DP) float value) {
                super(value);
            }

            /**
             * Sets the horizontal alignment of the actual content within the space reserved by
             * value. If not specified, defaults to center alignment.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setHorizontalAlignment(
                    @LayoutElementBuilders.HorizontalAlignment int horizontalAlignment) {
                mImpl.setHorizontalAlignmentForLayoutValue(horizontalAlignment);
                mFingerprint.recordPropertyUpdate(5, horizontalAlignment);
                return this;
            }

            /** Builds an instance of {@link HorizontalLayoutConstraint}. */
            @NonNull
            public HorizontalLayoutConstraint build() {
                return new HorizontalLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for specifying vertical layout constraints when using {@link DpProp} on a data
     * bindable layout element.
     *
     * @since 1.2
     */
    public static final class VerticalLayoutConstraint extends DpPropLayoutConstraint {
        VerticalLayoutConstraint(DimensionProto.DpProp impl, @Nullable Fingerprint fingerprint) {
            super(impl, fingerprint);
        }

        /**
         * Gets the vertical alignment of the actual content within the space reserved by value.
         *
         * @since 1.2
         */
        @LayoutElementBuilders.VerticalAlignment
        public int getVerticalAlignment() {
            return mImpl.getVerticalAlignmentForLayoutValue();
        }

        @NonNull
        static VerticalLayoutConstraint fromProto(@NonNull DimensionProto.DpProp proto) {
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
             * @since 1.2
             */
            public Builder(@Dimension(unit = DP) float value) {
                super(value);
            }

            /**
             * Sets the vertical alignment of the actual content within the space reserved by value.
             * If not specified, defaults to center alignment.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setVerticalAlignment(
                    @LayoutElementBuilders.VerticalAlignment int verticalAlignment) {
                mImpl.setVerticalAlignmentForLayoutValue(verticalAlignment);
                mFingerprint.recordPropertyUpdate(4, verticalAlignment);
                return this;
            }

            /** Builds an instance of {@link VerticalLayoutConstraint}. */
            @NonNull
            public VerticalLayoutConstraint build() {
                return new VerticalLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for font sizes, measured in sp.
     *
     * @since 1.0
     */
    public static final class SpProp {
        private final DimensionProto.SpProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SpProp(DimensionProto.SpProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value, in sp.
         *
         * @since 1.0
         */
        @Dimension(unit = SP)
        public float getValue() {
            return mImpl.getValue();
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
        public static SpProp fromProto(
                @NonNull DimensionProto.SpProp proto, @Nullable Fingerprint fingerprint) {
            return new SpProp(proto, fingerprint);
        }

        @NonNull
        static SpProp fromProto(@NonNull DimensionProto.SpProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.SpProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "SpProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link SpProp} */
        public static final class Builder {
            private final DimensionProto.SpProp.Builder mImpl = DimensionProto.SpProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(631793260);

            public Builder() {}

            /**
             * Sets the value, in sp. If a dynamic value is also set and the renderer supports
             * dynamic values for the corresponding field, this static value will be ignored.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(@Dimension(unit = SP) float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(2, Float.floatToIntBits(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpProp build() {
                return new SpProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for font spacing, measured in em.
     *
     * @since 1.0
     */
    public static final class EmProp {
        private final DimensionProto.EmProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        EmProp(DimensionProto.EmProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the value, in em.
         *
         * @since 1.0
         */
        public float getValue() {
            return mImpl.getValue();
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
        public static EmProp fromProto(
                @NonNull DimensionProto.EmProp proto, @Nullable Fingerprint fingerprint) {
            return new EmProp(proto, fingerprint);
        }

        @NonNull
        static EmProp fromProto(@NonNull DimensionProto.EmProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.EmProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "EmProp{" + "value=" + getValue() + "}";
        }

        /** Builder for {@link EmProp} */
        public static final class Builder {
            private final DimensionProto.EmProp.Builder mImpl = DimensionProto.EmProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-659639046);

            public Builder() {}

            /**
             * Sets the value, in em.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(value));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public EmProp build() {
                return new EmProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for angular dimensions, measured in degrees.
     *
     * @since 1.0
     */
    public static final class DegreesProp {
        private final DimensionProto.DegreesProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        DegreesProp(DimensionProto.DegreesProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the static value, in degrees.
         *
         * @since 1.0
         */
        public float getValue() {
            return mImpl.getValue();
        }

        /**
         * Gets the dynamic value, in degrees.
         *
         * @since 1.2
         */
        @Nullable
        public DynamicFloat getDynamicValue() {
            if (mImpl.hasDynamicValue()) {
                return DynamicBuilders.dynamicFloatFromProto(mImpl.getDynamicValue());
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
        public static DegreesProp fromProto(
                @NonNull DimensionProto.DegreesProp proto, @Nullable Fingerprint fingerprint) {
            return new DegreesProp(proto, fingerprint);
        }

        @NonNull
        static DegreesProp fromProto(@NonNull DimensionProto.DegreesProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.DegreesProp toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "DegreesProp{"
                    + "value="
                    + getValue()
                    + ", dynamicValue="
                    + getDynamicValue()
                    + "}";
        }

        /** Builder for {@link DegreesProp} */
        public static final class Builder {
            private final DimensionProto.DegreesProp.Builder mImpl =
                    DimensionProto.DegreesProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1927567665);

            /**
             * @deprecated Use {@link #Builder(float)} instead.
             */
            @Deprecated
            public Builder() {}

            /**
             * Creates a instance of {@link Builder}.
             *
             * @param staticValue the static value, in degrees.
             */
            public Builder(float staticValue) {
                setValue(staticValue);
            }

            /**
             * Sets the static value, in degrees. If a dynamic value is also set and the renderer
             * supports dynamic values for the corresponding field, this static value will be
             * ignored.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setValue(float staticValue) {
                mImpl.setValue(staticValue);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(staticValue));
                return this;
            }

            /**
             * Sets the dynamic value, in degrees. Note that when setting this value, the static
             * value is still required to be set to support older renderers that only read the
             * static value.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDynamicValue(@NonNull DynamicFloat dynamicValue) {
                mImpl.setDynamicValue(dynamicValue.toDynamicFloatProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(dynamicValue.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public DegreesProp build() {
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
     *
     * @since 1.2
     */
    public static final class AngularLayoutConstraint {
        private final DimensionProto.DegreesProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AngularLayoutConstraint(
                DimensionProto.DegreesProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fixed value to reserve the space when used on a layout-changing data bind. If
         * not set defaults to the static value of the associated {@link DegreesProp} field.
         *
         * @since 1.2
         */
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValueForLayout();
        }

        /**
         * Gets angular alignment of the actual content within the space reserved by value.
         *
         * @since 1.2
         */
        @LayoutElementBuilders.AngularAlignment
        public int getAngularAlignment() {
            return mImpl.getAngularAlignmentForLayoutValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.DegreesProp toProto() {
            return mImpl;
        }

        @NonNull
        static AngularLayoutConstraint fromProto(@NonNull DimensionProto.DegreesProp proto) {
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
             * @since 1.2
             */
            public Builder(@Dimension(unit = DP) float value) {
                setValue(value);
            }

            /**
             * Sets the fixed value to reserve the space when used on a layout-changing data bind.
             *
             * @since 1.2
             */
            @NonNull
            private Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValueForLayout(value);
                mFingerprint.recordPropertyUpdate(3, Float.floatToIntBits(value));
                return this;
            }

            /**
             * Sets angular alignment of the actual content within the space reserved by value. If
             * not specified, defaults to center alignment.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAngularAlignment(
                    @LayoutElementBuilders.AngularAlignment int angularAlignment) {
                mImpl.setAngularAlignmentForLayoutValue(angularAlignment);
                mFingerprint.recordPropertyUpdate(4, angularAlignment);
                return this;
            }

            /** Builds an instance of {@link AngularLayoutConstraint}. */
            @NonNull
            public AngularLayoutConstraint build() {
                return new AngularLayoutConstraint(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for a dimension that fills all the space it can (i.e. MATCH_PARENT in Android
     * parlance).
     *
     * @since 1.0
     */
    public static final class ExpandedDimensionProp implements ContainerDimension, ImageDimension {
        private final DimensionProto.ExpandedDimensionProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

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
         * @since 1.2
         */
        @Nullable
        public FloatProp getLayoutWeight() {
            if (mImpl.hasLayoutWeight()) {
                return FloatProp.fromProto(mImpl.getLayoutWeight());
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
        public static ExpandedDimensionProp fromProto(
                @NonNull DimensionProto.ExpandedDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ExpandedDimensionProp(proto, fingerprint);
        }

        @NonNull
        static ExpandedDimensionProp fromProto(
                @NonNull DimensionProto.ExpandedDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ExpandedDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder()
                    .setExpandedDimension(mImpl)
                    .build();
        }

        /* */
        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder().setExpandedDimension(mImpl).build();
        }

        /** Builder for {@link ExpandedDimensionProp}. */
        public static final class Builder
                implements ContainerDimension.Builder, ImageDimension.Builder {
            private final DimensionProto.ExpandedDimensionProp.Builder mImpl =
                    DimensionProto.ExpandedDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-997720604);

            public Builder() {}

            /**
             * Sets the layout weight (a dimensionless scalar value) for this element. This will
             * only affect the width of children of a {@link
             * androidx.wear.protolayout.LayoutElementBuilders.Row} or the height of children of a
             * {@link androidx.wear.protolayout.LayoutElementBuilders.Column}. By default, all
             * children have equal weight. Where applicable, the width or height of the element is
             * proportional to the sum of the weights of its siblings.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setLayoutWeight(@NonNull FloatProp layoutWeight) {
                mImpl.setLayoutWeight(layoutWeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(layoutWeight.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public ExpandedDimensionProp build() {
                return new ExpandedDimensionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A type for a dimension that sizes itself to the size of its children (i.e. WRAP_CONTENT in
     * Android parlance).
     *
     * @since 1.0
     */
    public static final class WrappedDimensionProp implements ContainerDimension {
        private final DimensionProto.WrappedDimensionProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        WrappedDimensionProp(
                DimensionProto.WrappedDimensionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the minimum size of this dimension. If not set, then there is no minimum size.
         *
         * @since 1.2
         */
        @Nullable
        public DpProp getMinimumSize() {
            if (mImpl.hasMinimumSize()) {
                return DpProp.fromProto(mImpl.getMinimumSize());
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
        public static WrappedDimensionProp fromProto(
                @NonNull DimensionProto.WrappedDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new WrappedDimensionProp(proto, fingerprint);
        }

        @NonNull
        static WrappedDimensionProp fromProto(@NonNull DimensionProto.WrappedDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.WrappedDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ContainerDimension toContainerDimensionProto() {
            return DimensionProto.ContainerDimension.newBuilder()
                    .setWrappedDimension(mImpl)
                    .build();
        }

        @Override
        @NonNull
        public String toString() {
            return "WrappedDimensionProp{" + "minimumSize=" + getMinimumSize() + "}";
        }

        /** Builder for {@link WrappedDimensionProp}. */
        public static final class Builder implements ContainerDimension.Builder {
            private final DimensionProto.WrappedDimensionProp.Builder mImpl =
                    DimensionProto.WrappedDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1118918114);

            public Builder() {}

            /**
             * Sets the minimum size of this dimension. If not set, then there is no minimum size.
             *
             * <p>Note that this field only supports static values.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setMinimumSize(@NonNull DpProp minimumSize) {
                if (minimumSize.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "setMinimumSize doesn't support dynamic values.");
                }

                mImpl.setMinimumSize(minimumSize.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(minimumSize.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            @Override
            @NonNull
            public WrappedDimensionProp build() {
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
     *
     * @since 1.0
     */
    public static final class ProportionalDimensionProp implements ImageDimension {
        private final DimensionProto.ProportionalDimensionProp mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ProportionalDimensionProp(
                DimensionProto.ProportionalDimensionProp impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the width to be used when calculating the aspect ratio to preserve.
         *
         * @since 1.0
         */
        @IntRange(from = 0)
        public int getAspectRatioWidth() {
            return mImpl.getAspectRatioWidth();
        }

        /**
         * Gets the height to be used when calculating the aspect ratio ratio to preserve.
         *
         * @since 1.0
         */
        @IntRange(from = 0)
        public int getAspectRatioHeight() {
            return mImpl.getAspectRatioHeight();
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
        public static ProportionalDimensionProp fromProto(
                @NonNull DimensionProto.ProportionalDimensionProp proto,
                @Nullable Fingerprint fingerprint) {
            return new ProportionalDimensionProp(proto, fingerprint);
        }

        @NonNull
        static ProportionalDimensionProp fromProto(
                @NonNull DimensionProto.ProportionalDimensionProp proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ProportionalDimensionProp toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.ImageDimension toImageDimensionProto() {
            return DimensionProto.ImageDimension.newBuilder()
                    .setProportionalDimension(mImpl)
                    .build();
        }

        @Override
        @NonNull
        public String toString() {
            return "ProportionalDimensionProp{"
                    + "aspectRatioWidth="
                    + getAspectRatioWidth()
                    + ", aspectRatioHeight="
                    + getAspectRatioHeight()
                    + "}";
        }

        /** Builder for {@link ProportionalDimensionProp}. */
        public static final class Builder implements ImageDimension.Builder {
            private final DimensionProto.ProportionalDimensionProp.Builder mImpl =
                    DimensionProto.ProportionalDimensionProp.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1725027476);

            public Builder() {}

            /**
             * Sets the width to be used when calculating the aspect ratio to preserve.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setAspectRatioWidth(@IntRange(from = 0) int aspectRatioWidth) {
                mImpl.setAspectRatioWidth(aspectRatioWidth);
                mFingerprint.recordPropertyUpdate(1, aspectRatioWidth);
                return this;
            }

            /**
             * Sets the height to be used when calculating the aspect ratio ratio to preserve.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setAspectRatioHeight(@IntRange(from = 0) int aspectRatioHeight) {
                mImpl.setAspectRatioHeight(aspectRatioHeight);
                mFingerprint.recordPropertyUpdate(2, aspectRatioHeight);
                return this;
            }

            @Override
            @NonNull
            public ProportionalDimensionProp build() {
                return new ProportionalDimensionProp(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining a dimension that can be applied to a container.
     *
     * @since 1.0
     */
    public interface ContainerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ContainerDimension toContainerDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link ContainerDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ContainerDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static ContainerDimension containerDimensionFromProto(
            @NonNull DimensionProto.ContainerDimension proto, @Nullable Fingerprint fingerprint) {
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

    @NonNull
    static ContainerDimension containerDimensionFromProto(
            @NonNull DimensionProto.ContainerDimension proto) {
        return containerDimensionFromProto(proto, null);
    }

    /**
     * Interface defining a dimension that can be applied to an image.
     *
     * @since 1.0
     */
    public interface ImageDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ImageDimension toImageDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link ImageDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ImageDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static ImageDimension imageDimensionFromProto(
            @NonNull DimensionProto.ImageDimension proto, @Nullable Fingerprint fingerprint) {
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

    @NonNull
    static ImageDimension imageDimensionFromProto(@NonNull DimensionProto.ImageDimension proto) {
        return imageDimensionFromProto(proto, null);
    }

    /**
     * Interface defining a dimension that can be applied to a spacer.
     *
     * @since 1.0
     */
    public interface SpacerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.SpacerDimension toSpacerDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link SpacerDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            SpacerDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static SpacerDimension spacerDimensionFromProto(
            @NonNull DimensionProto.SpacerDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of SpacerDimension");
    }

    @NonNull
    static SpacerDimension spacerDimensionFromProto(@NonNull DimensionProto.SpacerDimension proto) {
        return spacerDimensionFromProto(proto, null);
    }

    /**
     * Interface defining a dimension that can be applied to a {@link
     * androidx.wear.protolayout.LayoutElementBuilders.ExtensionLayoutElement} element.
     *
     * @since 1.0
     */
    @ExperimentalProtoLayoutExtensionApi
    public interface ExtensionDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ExtensionDimension toExtensionDimensionProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link ExtensionDimension} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ExtensionDimension build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public static ExtensionDimension extensionDimensionFromProto(
            @NonNull DimensionProto.ExtensionDimension proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasLinearDimension()) {
            return DpProp.fromProto(proto.getLinearDimension(), fingerprint);
        }
        throw new IllegalStateException(
                "Proto was not a recognised instance of ExtensionDimension");
    }

    @NonNull
    static ExtensionDimension extensionDimensionFromProto(
            @NonNull DimensionProto.ExtensionDimension proto) {
        return extensionDimensionFromProto(proto, null);
    }
}
