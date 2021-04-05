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

package androidx.wear.tiles.builders;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.tiles.builders.ActionBuilders.Action;
import androidx.wear.tiles.builders.ColorBuilders.ColorProp;
import androidx.wear.tiles.builders.DimensionBuilders.DpProp;
import androidx.wear.tiles.proto.ModifiersProto;
import androidx.wear.tiles.proto.TypesProto;

/** Builders for modifiers for composable layout elements. */
public final class ModifiersBuilders {
    private ModifiersBuilders() {}

    /**
     * A modifier for an element which can have associated Actions for click events. When an element
     * with a ClickableModifier is clicked it will fire the associated action.
     */
    public static final class Clickable {
        private final ModifiersProto.Clickable mImpl;

        private Clickable(ModifiersProto.Clickable impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Clickable fromProto(@NonNull ModifiersProto.Clickable proto) {
            return new Clickable(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Clickable toProto() {
            return mImpl;
        }

        /** Builder for {@link Clickable} */
        public static final class Builder {
            private final ModifiersProto.Clickable.Builder mImpl =
                    ModifiersProto.Clickable.newBuilder();

            Builder() {}

            /** Sets the ID associated with this action. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setId(@NonNull String id) {
                mImpl.setId(id);
                return this;
            }

            /**
             * Sets the action to perform when the element this modifier is attached to is clicked.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOnClick(@NonNull Action onClick) {
                mImpl.setOnClick(onClick.toActionProto());
                return this;
            }

            /**
             * Sets the action to perform when the element this modifier is attached to is clicked.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setOnClick(@NonNull Action.Builder onClickBuilder) {
                mImpl.setOnClick(onClickBuilder.build().toActionProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Clickable build() {
                return Clickable.fromProto(mImpl.build());
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

        private Semantics(ModifiersProto.Semantics impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Semantics fromProto(@NonNull ModifiersProto.Semantics proto) {
            return new Semantics(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Semantics toProto() {
            return mImpl;
        }

        /** Builder for {@link Semantics} */
        public static final class Builder {
            private final ModifiersProto.Semantics.Builder mImpl =
                    ModifiersProto.Semantics.newBuilder();

            Builder() {}

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setContentDescription(@NonNull String contentDescription) {
                mImpl.setContentDescription(contentDescription);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Semantics build() {
                return Semantics.fromProto(mImpl.build());
            }
        }
    }

    /** A modifier to apply padding around an element. */
    public static final class Padding {
        private final ModifiersProto.Padding mImpl;

        private Padding(ModifiersProto.Padding impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Padding fromProto(@NonNull ModifiersProto.Padding proto) {
            return new Padding(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Padding toProto() {
            return mImpl;
        }

        /** Builder for {@link Padding} */
        public static final class Builder {
            private final ModifiersProto.Padding.Builder mImpl =
                    ModifiersProto.Padding.newBuilder();

            Builder() {}

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setEnd(@NonNull DpProp end) {
                mImpl.setEnd(end.toProto());
                return this;
            }

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setEnd(@NonNull DpProp.Builder endBuilder) {
                mImpl.setEnd(endBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStart(@NonNull DpProp start) {
                mImpl.setStart(start.toProto());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setStart(@NonNull DpProp.Builder startBuilder) {
                mImpl.setStart(startBuilder.build().toProto());
                return this;
            }

            /** Sets the padding at the top, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTop(@NonNull DpProp top) {
                mImpl.setTop(top.toProto());
                return this;
            }

            /** Sets the padding at the top, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setTop(@NonNull DpProp.Builder topBuilder) {
                mImpl.setTop(topBuilder.build().toProto());
                return this;
            }

            /** Sets the padding at the bottom, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBottom(@NonNull DpProp bottom) {
                mImpl.setBottom(bottom.toProto());
                return this;
            }

            /** Sets the padding at the bottom, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBottom(@NonNull DpProp.Builder bottomBuilder) {
                mImpl.setBottom(bottomBuilder.build().toProto());
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
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Padding build() {
                return Padding.fromProto(mImpl.build());
            }
        }
    }

    /** A modifier to apply a border around an element. */
    public static final class Border {
        private final ModifiersProto.Border mImpl;

        private Border(ModifiersProto.Border impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Border fromProto(@NonNull ModifiersProto.Border proto) {
            return new Border(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Border toProto() {
            return mImpl;
        }

        /** Builder for {@link Border} */
        public static final class Builder {
            private final ModifiersProto.Border.Builder mImpl = ModifiersProto.Border.newBuilder();

            Builder() {}

            /** Sets the width of the border, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp width) {
                mImpl.setWidth(width.toProto());
                return this;
            }

            /** Sets the width of the border, in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWidth(@NonNull DpProp.Builder widthBuilder) {
                mImpl.setWidth(widthBuilder.build().toProto());
                return this;
            }

            /** Sets the color of the border. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /** Sets the color of the border. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Border build() {
                return Border.fromProto(mImpl.build());
            }
        }
    }

    /** The corner of a {@link androidx.wear.tiles.builders.LayoutElementBuilders.Box} element. */
    public static final class Corner {
        private final ModifiersProto.Corner mImpl;

        private Corner(ModifiersProto.Corner impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Corner fromProto(@NonNull ModifiersProto.Corner proto) {
            return new Corner(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Corner toProto() {
            return mImpl;
        }

        /** Builder for {@link Corner} */
        public static final class Builder {
            private final ModifiersProto.Corner.Builder mImpl = ModifiersProto.Corner.newBuilder();

            Builder() {}

            /** Sets the radius of the corner in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRadius(@NonNull DpProp radius) {
                mImpl.setRadius(radius.toProto());
                return this;
            }

            /** Sets the radius of the corner in DP. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setRadius(@NonNull DpProp.Builder radiusBuilder) {
                mImpl.setRadius(radiusBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Corner build() {
                return Corner.fromProto(mImpl.build());
            }
        }
    }

    /** A modifier to apply a background to an element. */
    public static final class Background {
        private final ModifiersProto.Background mImpl;

        private Background(ModifiersProto.Background impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Background fromProto(@NonNull ModifiersProto.Background proto) {
            return new Background(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Background toProto() {
            return mImpl;
        }

        /** Builder for {@link Background} */
        public static final class Builder {
            private final ModifiersProto.Background.Builder mImpl =
                    ModifiersProto.Background.newBuilder();

            Builder() {}

            /**
             * Sets the background color for this element. If not defined, defaults to being
             * transparent.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                return this;
            }

            /**
             * Sets the background color for this element. If not defined, defaults to being
             * transparent.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setColor(@NonNull ColorProp.Builder colorBuilder) {
                mImpl.setColor(colorBuilder.build().toProto());
                return this;
            }

            /**
             * Sets the corner properties of this element. This only affects the drawing of this
             * element if it has a background color or border. If not defined, defaults to having a
             * square corner.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setCorner(@NonNull Corner corner) {
                mImpl.setCorner(corner.toProto());
                return this;
            }

            /**
             * Sets the corner properties of this element. This only affects the drawing of this
             * element if it has a background color or border. If not defined, defaults to having a
             * square corner.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setCorner(@NonNull Corner.Builder cornerBuilder) {
                mImpl.setCorner(cornerBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Background build() {
                return Background.fromProto(mImpl.build());
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

        private Modifiers(ModifiersProto.Modifiers impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Modifiers fromProto(@NonNull ModifiersProto.Modifiers proto) {
            return new Modifiers(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Modifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link Modifiers} */
        public static final class Builder {
            private final ModifiersProto.Modifiers.Builder mImpl =
                    ModifiersProto.Modifiers.newBuilder();

            Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                return this;
            }

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable.Builder clickableBuilder) {
                mImpl.setClickable(clickableBuilder.build().toProto());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSemantics(@NonNull Semantics.Builder semanticsBuilder) {
                mImpl.setSemantics(semanticsBuilder.build().toProto());
                return this;
            }

            /** Sets adds padding to the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setPadding(@NonNull Padding padding) {
                mImpl.setPadding(padding.toProto());
                return this;
            }

            /** Sets adds padding to the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setPadding(@NonNull Padding.Builder paddingBuilder) {
                mImpl.setPadding(paddingBuilder.build().toProto());
                return this;
            }

            /** Sets draws a border around the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBorder(@NonNull Border border) {
                mImpl.setBorder(border.toProto());
                return this;
            }

            /** Sets draws a border around the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBorder(@NonNull Border.Builder borderBuilder) {
                mImpl.setBorder(borderBuilder.build().toProto());
                return this;
            }

            /** Sets adds a background (with optional corner radius) to the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBackground(@NonNull Background background) {
                mImpl.setBackground(background.toProto());
                return this;
            }

            /** Sets adds a background (with optional corner radius) to the modified element. */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setBackground(@NonNull Background.Builder backgroundBuilder) {
                mImpl.setBackground(backgroundBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Modifiers build() {
                return Modifiers.fromProto(mImpl.build());
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with ArcLayoutElements. These may change the way they are
     * drawn, or change their behaviour.
     */
    public static final class ArcModifiers {
        private final ModifiersProto.ArcModifiers mImpl;

        private ArcModifiers(ModifiersProto.ArcModifiers impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcModifiers fromProto(@NonNull ModifiersProto.ArcModifiers proto) {
            return new ArcModifiers(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.ArcModifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link ArcModifiers} */
        public static final class Builder {
            private final ModifiersProto.ArcModifiers.Builder mImpl =
                    ModifiersProto.ArcModifiers.newBuilder();

            Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                return this;
            }

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable.Builder clickableBuilder) {
                mImpl.setClickable(clickableBuilder.build().toProto());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setSemantics(@NonNull Semantics.Builder semanticsBuilder) {
                mImpl.setSemantics(semanticsBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ArcModifiers build() {
                return ArcModifiers.fromProto(mImpl.build());
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with {@link
     * androidx.wear.tiles.builders.LayoutElementBuilders.Span} elements. These may change the way
     * they are drawn, or change their behaviour.
     */
    public static final class SpanModifiers {
        private final ModifiersProto.SpanModifiers mImpl;

        private SpanModifiers(ModifiersProto.SpanModifiers impl) {
            this.mImpl = impl;
        }

        /** Returns a new {@link Builder}. */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SpanModifiers fromProto(@NonNull ModifiersProto.SpanModifiers proto) {
            return new SpanModifiers(proto);
        }

        /** @hide */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.SpanModifiers toProto() {
            return mImpl;
        }

        /** Builder for {@link SpanModifiers} */
        public static final class Builder {
            private final ModifiersProto.SpanModifiers.Builder mImpl =
                    ModifiersProto.SpanModifiers.newBuilder();

            Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                return this;
            }

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setClickable(@NonNull Clickable.Builder clickableBuilder) {
                mImpl.setClickable(clickableBuilder.build().toProto());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SpanModifiers build() {
                return SpanModifiers.fromProto(mImpl.build());
            }
        }
    }
}
