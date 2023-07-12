/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles;

import static androidx.annotation.Dimension.DP;
import static androidx.annotation.Dimension.SP;

import android.annotation.SuppressLint;

import androidx.annotation.Dimension;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.DimensionProto;

/**
 * Builders for dimensions for layout elements.
 *
 * @deprecated Use {@link androidx.wear.protolayout.DimensionBuilders} instead.
 */
@Deprecated
public final class DimensionBuilders {
    private DimensionBuilders() {}

    private static final ExpandedDimensionProp EXPAND = new ExpandedDimensionProp.Builder().build();
    private static final WrappedDimensionProp WRAP = new WrappedDimensionProp.Builder().build();

    /** Shortcut for building a {@link DpProp} using a measurement in DP. */
    @NonNull
    public static DpProp dp(@Dimension(unit = DP) float valueDp) {
        return new DpProp.Builder().setValue(valueDp).build();
    }

    /** Shortcut for building a {@link SpProp} using a measurement in SP. */
    @NonNull
    public static SpProp sp(@Dimension(unit = SP) float valueSp) {
        return new SpProp.Builder().setValue(valueSp).build();
    }

    /** Shortcut for building a {@link EmProp} using a measurement in EM. */
    @NonNull
    public static EmProp em(int valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /** Shortcut for building a {@link EmProp} using a measurement in EM. */
    @NonNull
    public static EmProp em(float valueEm) {
        return new EmProp.Builder().setValue(valueEm).build();
    }

    /** Shortcut for building an {@link DegreesProp} using a measurement in degrees. */
    @NonNull
    public static DegreesProp degrees(float valueDegrees) {
        return new DegreesProp.Builder().setValue(valueDegrees).build();
    }

    /**
     * Shortcut for building an {@link ExpandedDimensionProp} that will expand to the size of its
     * parent.
     */
    @NonNull
    public static ExpandedDimensionProp expand() {
        return EXPAND;
    }

    /**
     * Shortcut for building an {@link WrappedDimensionProp} that will shrink to the size of its
     * children.
     */
    @NonNull
    public static WrappedDimensionProp wrap() {
        return WRAP;
    }

    /** A type for linear dimensions, measured in dp. */
    public static final class DpProp
            implements ContainerDimension, ImageDimension, SpacerDimension {
        private final DimensionProto.DpProp mImpl;

        private DpProp(DimensionProto.DpProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value, in dp. Intended for testing purposes only. */
        @Dimension(unit = DP)
        public float getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static DpProp fromProto(@NonNull DimensionProto.DpProp proto) {
            return new DpProp(proto);
        }

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

        /** Builder for {@link DpProp}. */
        public static final class Builder
                implements ContainerDimension.Builder,
                        ImageDimension.Builder,
                        SpacerDimension.Builder {
            private final DimensionProto.DpProp.Builder mImpl = DimensionProto.DpProp.newBuilder();

            public Builder() {}

            /** Sets the value, in dp. */
            @NonNull
            public Builder setValue(@Dimension(unit = DP) float value) {
                mImpl.setValue(value);
                return this;
            }

            @Override
            @NonNull
            public DpProp build() {
                return DpProp.fromProto(mImpl.build());
            }
        }
    }

    /** A type for font sizes, measured in sp. */
    public static final class SpProp {
        private final DimensionProto.SpProp mImpl;

        private SpProp(DimensionProto.SpProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value, in sp. Intended for testing purposes only. */
        @Dimension(unit = SP)
        public float getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SpProp fromProto(@NonNull DimensionProto.SpProp proto) {
            return new SpProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.SpProp toProto() {
            return mImpl;
        }

        /** Builder for {@link SpProp} */
        public static final class Builder {
            private final DimensionProto.SpProp.Builder mImpl = DimensionProto.SpProp.newBuilder();

            public Builder() {}

            /** Sets the value, in sp. */
            @NonNull
            public Builder setValue(@Dimension(unit = SP) float value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpProp build() {
                return SpProp.fromProto(mImpl.build());
            }
        }
    }

    /** A type for font spacing, measured in em. */
    public static final class EmProp {
        private final DimensionProto.EmProp mImpl;

        private EmProp(DimensionProto.EmProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value, in em. Intended for testing purposes only. */
        public float getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static EmProp fromProto(@NonNull DimensionProto.EmProp proto) {
            return new EmProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.EmProp toProto() {
            return mImpl;
        }

        /** Builder for {@link EmProp} */
        public static final class Builder {
            private final DimensionProto.EmProp.Builder mImpl = DimensionProto.EmProp.newBuilder();

            public Builder() {}

            /** Sets the value, in em. */
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public EmProp build() {
                return EmProp.fromProto(mImpl.build());
            }
        }
    }

    /** A type for angular dimensions, measured in degrees. */
    public static final class DegreesProp {
        private final DimensionProto.DegreesProp mImpl;

        private DegreesProp(DimensionProto.DegreesProp impl) {
            this.mImpl = impl;
        }

        /** Gets the value, in degrees. Intended for testing purposes only. */
        public float getValue() {
            return mImpl.getValue();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static DegreesProp fromProto(@NonNull DimensionProto.DegreesProp proto) {
            return new DegreesProp(proto);
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public DimensionProto.DegreesProp toProto() {
            return mImpl;
        }

        /** Builder for {@link DegreesProp} */
        public static final class Builder {
            private final DimensionProto.DegreesProp.Builder mImpl =
                    DimensionProto.DegreesProp.newBuilder();

            public Builder() {}

            /** Sets the value, in degrees. */
            @NonNull
            public Builder setValue(float value) {
                mImpl.setValue(value);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public DegreesProp build() {
                return DegreesProp.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A type for a dimension that fills all the space it can (i.e. MATCH_PARENT in Android
     * parlance).
     */
    public static final class ExpandedDimensionProp implements ContainerDimension, ImageDimension {
        private final DimensionProto.ExpandedDimensionProp mImpl;

        private ExpandedDimensionProp(DimensionProto.ExpandedDimensionProp impl) {
            this.mImpl = impl;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ExpandedDimensionProp fromProto(
                @NonNull DimensionProto.ExpandedDimensionProp proto) {
            return new ExpandedDimensionProp(proto);
        }

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

            public Builder() {}

            @Override
            @NonNull
            public ExpandedDimensionProp build() {
                return ExpandedDimensionProp.fromProto(mImpl.build());
            }
        }
    }

    /**
     * A type for a dimension that sizes itself to the size of its children (i.e. WRAP_CONTENT in
     * Android parlance).
     */
    public static final class WrappedDimensionProp implements ContainerDimension {
        private final DimensionProto.WrappedDimensionProp mImpl;

        private WrappedDimensionProp(DimensionProto.WrappedDimensionProp impl) {
            this.mImpl = impl;
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static WrappedDimensionProp fromProto(
                @NonNull DimensionProto.WrappedDimensionProp proto) {
            return new WrappedDimensionProp(proto);
        }

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

        /** Builder for {@link WrappedDimensionProp}. */
        public static final class Builder implements ContainerDimension.Builder {
            private final DimensionProto.WrappedDimensionProp.Builder mImpl =
                    DimensionProto.WrappedDimensionProp.newBuilder();

            public Builder() {}

            @Override
            @NonNull
            public WrappedDimensionProp build() {
                return WrappedDimensionProp.fromProto(mImpl.build());
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
    public static final class ProportionalDimensionProp implements ImageDimension {
        private final DimensionProto.ProportionalDimensionProp mImpl;

        private ProportionalDimensionProp(DimensionProto.ProportionalDimensionProp impl) {
            this.mImpl = impl;
        }

        /**
         * Gets the width to be used when calculating the aspect ratio to preserve. Intended for
         * testing purposes only.
         */
        @IntRange(from = 0)
        public int getAspectRatioWidth() {
            return mImpl.getAspectRatioWidth();
        }

        /**
         * Gets the height to be used when calculating the aspect ratio ratio to preserve. Intended
         * for testing purposes only.
         */
        @IntRange(from = 0)
        public int getAspectRatioHeight() {
            return mImpl.getAspectRatioHeight();
        }

        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ProportionalDimensionProp fromProto(
                @NonNull DimensionProto.ProportionalDimensionProp proto) {
            return new ProportionalDimensionProp(proto);
        }

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

        /** Builder for {@link ProportionalDimensionProp}. */
        public static final class Builder implements ImageDimension.Builder {
            private final DimensionProto.ProportionalDimensionProp.Builder mImpl =
                    DimensionProto.ProportionalDimensionProp.newBuilder();

            public Builder() {}

            /** Sets the width to be used when calculating the aspect ratio to preserve. */
            @NonNull
            public Builder setAspectRatioWidth(@IntRange(from = 0) int aspectRatioWidth) {
                mImpl.setAspectRatioWidth(aspectRatioWidth);
                return this;
            }

            /** Sets the height to be used when calculating the aspect ratio ratio to preserve. */
            @NonNull
            public Builder setAspectRatioHeight(@IntRange(from = 0) int aspectRatioHeight) {
                mImpl.setAspectRatioHeight(aspectRatioHeight);
                return this;
            }

            @Override
            @NonNull
            public ProportionalDimensionProp build() {
                return ProportionalDimensionProp.fromProto(mImpl.build());
            }
        }
    }

    /** Interface defining a dimension that can be applied to a container. */
    public interface ContainerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ContainerDimension toContainerDimensionProto();

        /**
         * Return an instance of one of this object's subtypes, from the protocol buffer
         * representation.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static ContainerDimension fromContainerDimensionProto(
                @NonNull DimensionProto.ContainerDimension proto) {
            if (proto.hasLinearDimension()) {
                return DpProp.fromProto(proto.getLinearDimension());
            }
            if (proto.hasExpandedDimension()) {
                return ExpandedDimensionProp.fromProto(proto.getExpandedDimension());
            }
            if (proto.hasWrappedDimension()) {
                return WrappedDimensionProp.fromProto(proto.getWrappedDimension());
            }
            throw new IllegalStateException(
                    "Proto was not a recognised instance of ContainerDimension");
        }

        /** Builder to create {@link ContainerDimension} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ContainerDimension build();
        }
    }

    /** Interface defining a dimension that can be applied to an image. */
    public interface ImageDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.ImageDimension toImageDimensionProto();

        /**
         * Return an instance of one of this object's subtypes, from the protocol buffer
         * representation.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static ImageDimension fromImageDimensionProto(
                @NonNull DimensionProto.ImageDimension proto) {
            if (proto.hasLinearDimension()) {
                return DpProp.fromProto(proto.getLinearDimension());
            }
            if (proto.hasExpandedDimension()) {
                return ExpandedDimensionProp.fromProto(proto.getExpandedDimension());
            }
            if (proto.hasProportionalDimension()) {
                return ProportionalDimensionProp.fromProto(proto.getProportionalDimension());
            }
            throw new IllegalStateException(
                    "Proto was not a recognised instance of ImageDimension");
        }

        /** Builder to create {@link ImageDimension} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            ImageDimension build();
        }
    }

    /** Interface defining a dimension that can be applied to a spacer. */
    public interface SpacerDimension {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        DimensionProto.SpacerDimension toSpacerDimensionProto();

        /**
         * Return an instance of one of this object's subtypes, from the protocol buffer
         * representation.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        static SpacerDimension fromSpacerDimensionProto(
                @NonNull DimensionProto.SpacerDimension proto) {
            if (proto.hasLinearDimension()) {
                return DpProp.fromProto(proto.getLinearDimension());
            }
            throw new IllegalStateException(
                    "Proto was not a recognised instance of SpacerDimension");
        }

        /** Builder to create {@link SpacerDimension} objects. */
        @SuppressLint("StaticFinalBuilder")
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            SpacerDimension build();
        }
    }
}
