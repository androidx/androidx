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

import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.proto.ModifiersProto;
import androidx.wear.protolayout.proto.TypesProto;
import androidx.wear.protolayout.protobuf.ByteString;

import java.util.Arrays;

/**
 * Builders for modifiers for composable layout elements.
 *
 * @deprecated Use {@link androidx.wear.protolayout.ModifiersBuilders} instead.
 */
@Deprecated
public final class ModifiersBuilders {
    private ModifiersBuilders() {}

    /**
     * A modifier for an element which can have associated Actions for click events. When an element
     * with a ClickableModifier is clicked it will fire the associated action.
     */
    public static final class Clickable {
        private final ModifiersProto.Clickable mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Clickable(ModifiersProto.Clickable impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the ID associated with this action. Intended for testing purposes only. */
        @NonNull
        public String getId() {
            return mImpl.getId();
        }

        /**
         * Gets the action to perform when the element this modifier is attached to is clicked.
         * Intended for testing purposes only.
         */
        @Nullable
        public ActionBuilders.Action getOnClick() {
            if (mImpl.hasOnClick()) {
                return ActionBuilders.actionFromProto(mImpl.getOnClick());
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

        @NonNull
        static Clickable fromProto(@NonNull ModifiersProto.Clickable proto) {
            return new Clickable(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Clickable toProto() {
            return mImpl;
        }

        /** Builder for {@link Clickable} */
        public static final class Builder {
            private final ModifiersProto.Clickable.Builder mImpl =
                    ModifiersProto.Clickable.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(595587995);

            public Builder() {}

            /** Sets the ID associated with this action. */
            @NonNull
            public Builder setId(@NonNull String id) {
                mImpl.setId(id);
                mFingerprint.recordPropertyUpdate(1, id.hashCode());
                return this;
            }

            /**
             * Sets the action to perform when the element this modifier is attached to is clicked.
             */
            @NonNull
            public Builder setOnClick(@NonNull ActionBuilders.Action onClick) {
                mImpl.setOnClick(onClick.toActionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(onClick.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Clickable build() {
                return new Clickable(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A modifier for an element which has accessibility semantics associated with it. This should
     * generally be used sparingly, and in most cases should only be applied to the top-level layout
     * element or to Clickables.
     */
    public static final class Semantics {
        private final ModifiersProto.Semantics mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Semantics(ModifiersProto.Semantics impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the content description associated with this element. This will be dictated when the
         * element is focused by the screen reader. Intended for testing purposes only.
         */
        @NonNull
        public String getContentDescription() {
            return mImpl.getObsoleteContentDescription();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static Semantics fromProto(@NonNull ModifiersProto.Semantics proto) {
            return new Semantics(proto, null);
        }

        @NonNull
        ModifiersProto.Semantics toProto() {
            return mImpl;
        }

        /** Builder for {@link Semantics} */
        public static final class Builder {
            private final ModifiersProto.Semantics.Builder mImpl =
                    ModifiersProto.Semantics.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1479823155);

            public Builder() {}

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             */
            @NonNull
            @SuppressWarnings(
                    "deprecation") // Updating a deprecated field for backward compatibility
            public Builder setContentDescription(@NonNull String contentDescription) {
                mImpl.setObsoleteContentDescription(contentDescription);
                mFingerprint.recordPropertyUpdate(4, contentDescription.hashCode());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Semantics build() {
                return new Semantics(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply padding around an element. */
    public static final class Padding {
        private final ModifiersProto.Padding mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Padding(ModifiersProto.Padding impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the padding on the end of the content, depending on the layout direction, in DP and
         * the value of "rtl_aware". Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getEnd() {
            if (mImpl.hasEnd()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getEnd());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding on the start of the content, depending on the layout direction, in DP
         * and the value of "rtl_aware". Intended for testing purposes only.
         */
        @Nullable
        public DimensionBuilders.DpProp getStart() {
            if (mImpl.hasStart()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getStart());
            } else {
                return null;
            }
        }

        /** Gets the padding at the top, in DP. Intended for testing purposes only. */
        @Nullable
        public DimensionBuilders.DpProp getTop() {
            if (mImpl.hasTop()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getTop());
            } else {
                return null;
            }
        }

        /** Gets the padding at the bottom, in DP. Intended for testing purposes only. */
        @Nullable
        public DimensionBuilders.DpProp getBottom() {
            if (mImpl.hasBottom()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getBottom());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the start/end padding is aware of RTL support. If true, the values for
         * start/end will follow the layout direction (i.e. start will refer to the right hand side
         * of the container if the device is using an RTL locale). If false, start/end will always
         * map to left/right, accordingly. Intended for testing purposes only.
         */
        @Nullable
        public TypeBuilders.BoolProp getRtlAware() {
            if (mImpl.hasRtlAware()) {
                return TypeBuilders.BoolProp.fromProto(mImpl.getRtlAware());
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

        @NonNull
        static Padding fromProto(@NonNull ModifiersProto.Padding proto) {
            return new Padding(proto, null);
        }

        @NonNull
        ModifiersProto.Padding toProto() {
            return mImpl;
        }

        /** Builder for {@link Padding} */
        public static final class Builder {
            private final ModifiersProto.Padding.Builder mImpl =
                    ModifiersProto.Padding.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1120275440);

            public Builder() {}

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             */
            @NonNull
            public Builder setEnd(@NonNull DimensionBuilders.DpProp end) {
                mImpl.setEnd(end.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(end.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             */
            @NonNull
            public Builder setStart(@NonNull DimensionBuilders.DpProp start) {
                mImpl.setStart(start.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(start.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the padding at the top, in DP. */
            @NonNull
            public Builder setTop(@NonNull DimensionBuilders.DpProp top) {
                mImpl.setTop(top.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(top.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the padding at the bottom, in DP. */
            @NonNull
            public Builder setBottom(@NonNull DimensionBuilders.DpProp bottom) {
                mImpl.setBottom(bottom.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(bottom.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the start/end padding is aware of RTL support. If true, the values for
             * start/end will follow the layout direction (i.e. start will refer to the right hand
             * side of the container if the device is using an RTL locale). If false, start/end will
             * always map to left/right, accordingly.
             */
            @NonNull
            public Builder setRtlAware(@NonNull TypeBuilders.BoolProp rtlAware) {
                mImpl.setRtlAware(rtlAware.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(rtlAware.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the start/end padding is aware of RTL support. If true, the values for
             * start/end will follow the layout direction (i.e. start will refer to the right hand
             * side of the container if the device is using an RTL locale). If false, start/end will
             * always map to left/right, accordingly.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRtlAware(boolean rtlAware) {
                mImpl.setRtlAware(TypesProto.BoolProp.newBuilder().setValue(rtlAware));
                mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(rtlAware));
                return this;
            }

            /** Sets the padding for all sides of the content, in DP. */
            @NonNull
            @SuppressLint("MissingGetterMatchingBuilder")
            public Builder setAll(@NonNull DimensionBuilders.DpProp value) {
                return setStart(value).setEnd(value).setTop(value).setBottom(value);
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Padding build() {
                return new Padding(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply a border around an element. */
    public static final class Border {
        private final ModifiersProto.Border mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Border(ModifiersProto.Border impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the width of the border, in DP. Intended for testing purposes only. */
        @Nullable
        public DimensionBuilders.DpProp getWidth() {
            if (mImpl.hasWidth()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /** Gets the color of the border. Intended for testing purposes only. */
        @Nullable
        public ColorBuilders.ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorBuilders.ColorProp.fromProto(mImpl.getColor());
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

        @NonNull
        static Border fromProto(@NonNull ModifiersProto.Border proto) {
            return new Border(proto, null);
        }

        @NonNull
        ModifiersProto.Border toProto() {
            return mImpl;
        }

        /** Builder for {@link Border} */
        public static final class Builder {
            private final ModifiersProto.Border.Builder mImpl = ModifiersProto.Border.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(2085330827);

            public Builder() {}

            /** Sets the width of the border, in DP. */
            @NonNull
            public Builder setWidth(@NonNull DimensionBuilders.DpProp width) {
                mImpl.setWidth(width.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the color of the border. */
            @NonNull
            public Builder setColor(@NonNull ColorBuilders.ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Border build() {
                return new Border(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The corner of a {@link androidx.wear.tiles.LayoutElementBuilders.Box} element. */
    public static final class Corner {
        private final ModifiersProto.Corner mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Corner(ModifiersProto.Corner impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the radius of the corner in DP. Intended for testing purposes only. */
        @Nullable
        public DimensionBuilders.DpProp getRadius() {
            if (mImpl.hasRadius()) {
                return DimensionBuilders.DpProp.fromProto(mImpl.getRadius());
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

        @NonNull
        static Corner fromProto(@NonNull ModifiersProto.Corner proto) {
            return new Corner(proto, null);
        }

        @NonNull
        ModifiersProto.Corner toProto() {
            return mImpl;
        }

        /** Builder for {@link Corner} */
        public static final class Builder {
            private final ModifiersProto.Corner.Builder mImpl = ModifiersProto.Corner.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-623478338);

            public Builder() {}

            /** Sets the radius of the corner in DP. */
            @NonNull
            public Builder setRadius(@NonNull DimensionBuilders.DpProp radius) {
                mImpl.setRadius(radius.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(radius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Corner build() {
                return new Corner(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply a background to an element. */
    public static final class Background {
        private final ModifiersProto.Background mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Background(ModifiersProto.Background impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the background color for this element. If not defined, defaults to being
         * transparent. Intended for testing purposes only.
         */
        @Nullable
        public ColorBuilders.ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorBuilders.ColorProp.fromProto(mImpl.getColor());
            } else {
                return null;
            }
        }

        /**
         * Gets the corner properties of this element. This only affects the drawing of this element
         * if it has a background color or border. If not defined, defaults to having a square
         * corner. Intended for testing purposes only.
         */
        @Nullable
        public Corner getCorner() {
            if (mImpl.hasCorner()) {
                return Corner.fromProto(mImpl.getCorner());
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

        @NonNull
        static Background fromProto(@NonNull ModifiersProto.Background proto) {
            return new Background(proto, null);
        }

        @NonNull
        ModifiersProto.Background toProto() {
            return mImpl;
        }

        /** Builder for {@link Background} */
        public static final class Builder {
            private final ModifiersProto.Background.Builder mImpl =
                    ModifiersProto.Background.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(374507572);

            public Builder() {}

            /**
             * Sets the background color for this element. If not defined, defaults to being
             * transparent.
             */
            @NonNull
            public Builder setColor(@NonNull ColorBuilders.ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the corner properties of this element. This only affects the drawing of this
             * element if it has a background color or border. If not defined, defaults to having a
             * square corner.
             */
            @NonNull
            public Builder setCorner(@NonNull Corner corner) {
                mImpl.setCorner(corner.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(corner.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Background build() {
                return new Background(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Metadata about an element. For use by libraries building higher-level components only. This
     * can be used to track component metadata.
     */
    public static final class ElementMetadata {
        private final ModifiersProto.ElementMetadata mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ElementMetadata(ModifiersProto.ElementMetadata impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets property describing the element with which it is associated. For use by libraries
         * building higher-level components only. This can be used to track component metadata.
         */
        @NonNull
        public byte[] getTagData() {
            return mImpl.getTagData().toByteArray();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @NonNull
        static ElementMetadata fromProto(@NonNull ModifiersProto.ElementMetadata proto) {
            return new ElementMetadata(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.ElementMetadata toProto() {
            return mImpl;
        }

        /** Builder for {@link ElementMetadata} */
        public static final class Builder {
            private final ModifiersProto.ElementMetadata.Builder mImpl =
                    ModifiersProto.ElementMetadata.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-589294723);

            public Builder() {}

            /**
             * Sets property describing the element with which it is associated. For use by
             * libraries building higher-level components only. This can be used to track component
             * metadata.
             */
            @NonNull
            public Builder setTagData(@NonNull byte[] tagData) {
                mImpl.setTagData(ByteString.copyFrom(tagData));
                mFingerprint.recordPropertyUpdate(1, Arrays.hashCode(tagData));
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ElementMetadata build() {
                return new ElementMetadata(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} for an element. These may change the way they are drawn (e.g. {@link
     * Padding} or {@link Background}), or change their behaviour (e.g. {@link Clickable}, or {@link
     * Semantics}).
     */
    public static final class Modifiers {
        private final ModifiersProto.Modifiers mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Modifiers(ModifiersProto.Modifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the clickable property of the modified element. It allows its wrapped element to
         * have actions associated with it, which will be executed when the element is tapped.
         * Intended for testing purposes only.
         */
        @Nullable
        public Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
            } else {
                return null;
            }
        }

        /**
         * Gets the semantics of the modified element. This can be used to add metadata to the
         * modified element (eg. screen reader content descriptions). Intended for testing purposes
         * only.
         */
        @Nullable
        public Semantics getSemantics() {
            if (mImpl.hasSemantics()) {
                return Semantics.fromProto(mImpl.getSemantics());
            } else {
                return null;
            }
        }

        /** Gets the padding of the modified element. Intended for testing purposes only. */
        @Nullable
        public Padding getPadding() {
            if (mImpl.hasPadding()) {
                return Padding.fromProto(mImpl.getPadding());
            } else {
                return null;
            }
        }

        /** Gets the border of the modified element. Intended for testing purposes only. */
        @Nullable
        public Border getBorder() {
            if (mImpl.hasBorder()) {
                return Border.fromProto(mImpl.getBorder());
            } else {
                return null;
            }
        }

        /**
         * Gets the background (with optional corner radius) of the modified element. Intended for
         * testing purposes only.
         */
        @Nullable
        public Background getBackground() {
            if (mImpl.hasBackground()) {
                return Background.fromProto(mImpl.getBackground());
            } else {
                return null;
            }
        }

        /**
         * Gets metadata about an element. For use by libraries building higher-level components
         * only. This can be used to track component metadata.
         */
        @Nullable
        public ElementMetadata getMetadata() {
            if (mImpl.hasMetadata()) {
                return ElementMetadata.fromProto(mImpl.getMetadata());
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
        public static Modifiers fromProto(
                @NonNull ModifiersProto.Modifiers proto, @Nullable Fingerprint fingerprint) {
            return new Modifiers(proto, fingerprint);
        }

        /**
         * Creates a new wrapper instance from the proto. Intended for testing purposes only. An
         * object created using this method can't be added to any other wrapper.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Modifiers fromProto(@NonNull ModifiersProto.Modifiers proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Modifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link Modifiers} */
        public static final class Builder {
            private final ModifiersProto.Modifiers.Builder mImpl =
                    ModifiersProto.Modifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-170942531);

            public Builder() {}

            /**
             * Sets the clickable property of the modified element. It allows its wrapped element to
             * have actions associated with it, which will be executed when the element is tapped.
             */
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the semantics of the modified element. This can be used to add metadata to the
             * modified element (eg. screen reader content descriptions).
             */
            @NonNull
            public Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(semantics.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the padding of the modified element. */
            @NonNull
            public Builder setPadding(@NonNull Padding padding) {
                mImpl.setPadding(padding.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(padding.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the border of the modified element. */
            @NonNull
            public Builder setBorder(@NonNull Border border) {
                mImpl.setBorder(border.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(border.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the background (with optional corner radius) of the modified element. */
            @NonNull
            public Builder setBackground(@NonNull Background background) {
                mImpl.setBackground(background.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(background.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets metadata about an element. For use by libraries building higher-level components
             * only. This can be used to track component metadata.
             */
            @NonNull
            public Builder setMetadata(@NonNull ElementMetadata metadata) {
                mImpl.setMetadata(metadata.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(metadata.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Modifiers build() {
                return new Modifiers(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with ArcLayoutElements. These may change the way they are
     * drawn, or change their behaviour.
     */
    public static final class ArcModifiers {
        private final ModifiersProto.ArcModifiers mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ArcModifiers(ModifiersProto.ArcModifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets allows its wrapped element to have actions associated with it, which will be
         * executed when the element is tapped. Intended for testing purposes only.
         */
        @Nullable
        public Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
            } else {
                return null;
            }
        }

        /**
         * Gets adds metadata for the modified element, for example, screen reader content
         * descriptions. Intended for testing purposes only.
         */
        @Nullable
        public Semantics getSemantics() {
            if (mImpl.hasSemantics()) {
                return Semantics.fromProto(mImpl.getSemantics());
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

        @NonNull
        static ArcModifiers fromProto(@NonNull ModifiersProto.ArcModifiers proto) {
            return new ArcModifiers(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.ArcModifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link ArcModifiers} */
        public static final class Builder {
            private final ModifiersProto.ArcModifiers.Builder mImpl =
                    ModifiersProto.ArcModifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1648736168);

            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @NonNull
            public Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(semantics.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ArcModifiers build() {
                return new ArcModifiers(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with {@link
     * androidx.wear.tiles.LayoutElementBuilders.Span} elements. These may change the way they are
     * drawn, or change their behaviour.
     */
    public static final class SpanModifiers {
        private final ModifiersProto.SpanModifiers mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SpanModifiers(ModifiersProto.SpanModifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets allows its wrapped element to have actions associated with it, which will be
         * executed when the element is tapped. Intended for testing purposes only.
         */
        @Nullable
        public Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
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

        @NonNull
        static SpanModifiers fromProto(@NonNull ModifiersProto.SpanModifiers proto) {
            return new SpanModifiers(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.SpanModifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link SpanModifiers} */
        public static final class Builder {
            private final ModifiersProto.SpanModifiers.Builder mImpl =
                    ModifiersProto.SpanModifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1318656482);

            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpanModifiers build() {
                return new SpanModifiers(mImpl.build(), mFingerprint);
            }
        }
    }
}
