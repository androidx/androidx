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

import android.annotation.SuppressLint;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ActionBuilders.Action;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.TypeBuilders.BoolProp;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.proto.ModifiersProto;
import androidx.wear.protolayout.proto.TypesProto;
import androidx.wear.protolayout.protobuf.ByteString;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

/** Builders for modifiers for composable layout elements. */
public final class ModifiersBuilders {
    private ModifiersBuilders() {}

    /** Prebuilt default objects for animated visibility transition animations. */
    @ProtoLayoutExperimental
    public static final class DefaultContentTransitions {
        /**
         * Fade in transition animation that fades in element when entering the layout, from fully
         * invisible to fully visible.
         */
        private static final FadeInTransition FADE_IN_TRANSITION =
                new FadeInTransition.Builder().build();

        /**
         * Fade in enter animation that fades in element when entering the layout, from fully
         * invisible to fully visible.
         */
        private static final EnterTransition FADE_IN_ENTER_TRANSITION =
                new EnterTransition.Builder().setFadeIn(FADE_IN_TRANSITION).build();

        /**
         * Slide in transition animation that slides in element when entering the layout into its
         * position from the parent edge in the given direction.
         *
         * @param direction The direction for sliding in transition.
         */
        private static SlideInTransition slideInTransition(@SlideDirection int direction) {
            return new SlideInTransition.Builder().setDirection(direction).build();
        }

        /**
         * Enter content transition animation that fades in element when entering the layout, from
         * fully invisible to fully visible.
         */
        @NonNull
        public static EnterTransition fadeIn() {
            return FADE_IN_ENTER_TRANSITION;
        }

        /**
         * Enter content transition animation that slides in element when entering the layout into
         * its position from the parent edge in the given direction.
         */
        @NonNull
        public static EnterTransition slideIn(@SlideDirection int slideDirection) {
            return new EnterTransition.Builder()
                    .setSlideIn(slideInTransition(slideDirection))
                    .build();
        }

        /**
         * Enter content transition animation that fades in element when entering the layout, from
         * fully invisible to fully visible and slides it in into its position from the parent edge
         * in the given direction.
         *
         * @param slideDirection The direction for sliding in part of transition.
         */
        @NonNull
        public static EnterTransition fadeInSlideIn(@SlideDirection int slideDirection) {
            return new EnterTransition.Builder()
                    .setFadeIn(FADE_IN_TRANSITION)
                    .setSlideIn(slideInTransition(slideDirection))
                    .build();
        }

        /**
         * Fade out transition animation that fades out element when exiting the layout, from fully
         * visible to fully invisible.
         */
        private static final FadeOutTransition FADE_OUT_TRANSITION =
                new FadeOutTransition.Builder().build();

        /**
         * Fade out exit animation that fades out element when exiting the layout, from fully
         * visible to fully invisible.
         */
        private static final ExitTransition FADE_OUT_EXIT_TRANSITION =
                new ExitTransition.Builder().setFadeOut(FADE_OUT_TRANSITION).build();

        /**
         * Slide out transition animation that slides out element when exiting the layout from its
         * position to the parent edge in the given direction.
         *
         * @param direction The direction for sliding out transition.
         */
        private static SlideOutTransition slideOutTransition(@SlideDirection int direction) {
            return new SlideOutTransition.Builder().setDirection(direction).build();
        }

        /**
         * Exit content transition animation that fades out element when exiting the layout, from
         * fully visible to fully invisible.
         */
        @NonNull
        public static ExitTransition fadeOut() {
            return FADE_OUT_EXIT_TRANSITION;
        }

        /**
         * Exit content transition animation that slides out element when exiting the layout from
         * its position to the parent edge in the given direction.
         */
        @NonNull
        public static ExitTransition slideOut(@SlideDirection int slideDirection) {
            return new ExitTransition.Builder()
                    .setSlideOut(slideOutTransition(slideDirection))
                    .build();
        }

        /**
         * Exit content transition animation that fades out element when exiting the layout, from
         * fully visible to fully invisible and slides it out from its position to the parent edge
         * in the given direction.
         *
         * @param slideDirection The direction for sliding in part of transition.
         */
        @NonNull
        public static ExitTransition fadeOutSlideOut(@SlideDirection int slideDirection) {
            return new ExitTransition.Builder()
                    .setFadeOut(FADE_OUT_TRANSITION)
                    .setSlideOut(slideOutTransition(slideDirection))
                    .build();
        }

        private DefaultContentTransitions() {}
    }

    /**
     * The type of user interface element. Accessibility services might use this to describe the
     * element or do customizations.
     *
     * @since 1.2
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SEMANTICS_ROLE_NONE,
        SEMANTICS_ROLE_IMAGE,
        SEMANTICS_ROLE_BUTTON,
        SEMANTICS_ROLE_CHECKBOX,
        SEMANTICS_ROLE_SWITCH,
        SEMANTICS_ROLE_RADIOBUTTON
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SemanticsRole {}

    /**
     * Role is undefined. It may be automatically populated.
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_NONE = 0;

    /**
     * The element is an image.
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_IMAGE = 1;

    /**
     * The element is a Button control.
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_BUTTON = 2;

    /**
     * The element is a Checkbox which is a component that represents two states (checked /
     * unchecked).
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_CHECKBOX = 3;

    /**
     * The element is a Switch which is a two state toggleable component that provides on/off like
     * options.
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_SWITCH = 4;

    /**
     * This element is a RadioButton which is a component to represent two states, selected and not
     * selected.
     *
     * @since 1.2
     */
    public static final int SEMANTICS_ROLE_RADIOBUTTON = 5;

    /**
     * The snap options to use when sliding using parent boundaries.
     *
     * @since 1.2
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SLIDE_PARENT_SNAP_UNDEFINED,
        SLIDE_PARENT_SNAP_TO_INSIDE,
        SLIDE_PARENT_SNAP_TO_OUTSIDE
    })
    @Retention(RetentionPolicy.SOURCE)
    @ProtoLayoutExperimental
    public @interface SlideParentSnapOption {}

    /**
     * The undefined snapping option.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_PARENT_SNAP_UNDEFINED = 0;

    /**
     * The option that snaps insides of the element and its parent at start/end.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_PARENT_SNAP_TO_INSIDE = 1;

    /**
     * The option that snaps outsides of the element and its parent at start/end.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_PARENT_SNAP_TO_OUTSIDE = 2;

    /**
     * The slide direction used for slide animations on any element, from the specified point to its
     * destination in the layout for in animation or reverse for out animation.
     *
     * @since 1.2
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SLIDE_DIRECTION_UNDEFINED,
        SLIDE_DIRECTION_LEFT_TO_RIGHT,
        SLIDE_DIRECTION_RIGHT_TO_LEFT,
        SLIDE_DIRECTION_TOP_TO_BOTTOM,
        SLIDE_DIRECTION_BOTTOM_TO_TOP
    })
    @Retention(RetentionPolicy.SOURCE)
    @ProtoLayoutExperimental
    public @interface SlideDirection {}

    /**
     * The undefined sliding orientation.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_DIRECTION_UNDEFINED = 0;

    /**
     * The sliding orientation that moves an element horizontally from left to the right.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_DIRECTION_LEFT_TO_RIGHT = 1;

    /**
     * The sliding orientation that moves an element horizontally from right to the left.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_DIRECTION_RIGHT_TO_LEFT = 2;

    /**
     * The sliding orientation that moves an element vertically from top to the bottom.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_DIRECTION_TOP_TO_BOTTOM = 3;

    /**
     * The sliding orientation that moves an element vertically from bottom to the top.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental public static final int SLIDE_DIRECTION_BOTTOM_TO_TOP = 4;

    /**
     * A modifier for an element which can have associated Actions for click events. When an element
     * with a ClickableModifier is clicked it will fire the associated action.
     *
     * @since 1.0
     */
    public static final class Clickable {
        private final ModifiersProto.Clickable mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Clickable(ModifiersProto.Clickable impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the ID associated with this action.
         *
         * @since 1.0
         */
        @NonNull
        public String getId() {
            return mImpl.getId();
        }

        /**
         * Gets the action to perform when the element this modifier is attached to is clicked.
         *
         * @since 1.0
         */
        @Nullable
        public Action getOnClick() {
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

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Clickable fromProto(
                @NonNull ModifiersProto.Clickable proto, @Nullable Fingerprint fingerprint) {
            return new Clickable(proto, fingerprint);
        }

        @NonNull
        static Clickable fromProto(@NonNull ModifiersProto.Clickable proto) {
            return fromProto(proto, null);
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
            private final Fingerprint mFingerprint = new Fingerprint(812136104);

            public Builder() {}

            /**
             * Sets the ID associated with this action.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setId(@NonNull String id) {
                mImpl.setId(id);
                mFingerprint.recordPropertyUpdate(1, id.hashCode());
                return this;
            }

            /**
             * Sets the action to perform when the element this modifier is attached to is clicked.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setOnClick(@NonNull Action onClick) {
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
     *
     * @since 1.0
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
         * element is focused by the screen reader.
         *
         * <p>This field is made bindable from version 1.2 and will use the dynamic value (if set).
         * Older renderers will still consider this field as non-bindable and will use the static
         * value.
         *
         * @since 1.0
         */
        @Nullable
        public StringProp getContentDescription() {
            if (mImpl.hasContentDescription()) {
                return StringProp.fromProto(mImpl.getContentDescription());
            } else {
                return null;
            }
        }

        /**
         * Gets the type of user interface element. Accessibility services might use this to
         * describe the element or do customizations.
         *
         * @since 1.2
         */
        @SemanticsRole
        public int getRole() {
            return mImpl.getRole().getNumber();
        }

        /**
         * Gets the localized state description of the semantics node. For example: "on" or "off".
         * This will be dictated when the element is focused by the screen reader.
         *
         * <p>This field is bindable and will use the dynamic value (if set).
         *
         * @since 1.2
         */
        @Nullable
        public StringProp getStateDescription() {
            if (mImpl.hasStateDescription()) {
                return StringProp.fromProto(mImpl.getStateDescription());
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
        public static Semantics fromProto(
                @NonNull ModifiersProto.Semantics proto, @Nullable Fingerprint fingerprint) {
            return new Semantics(proto, fingerprint);
        }

        @NonNull
        static Semantics fromProto(@NonNull ModifiersProto.Semantics proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Semantics toProto() {
            return mImpl;
        }

        /** Builder for {@link Semantics} */
        public static final class Builder {
            private final ModifiersProto.Semantics.Builder mImpl =
                    ModifiersProto.Semantics.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1679805809);

            public Builder() {}

            /**
             * Sets the type of user interface element. Accessibility services might use this to
             * describe the element or do customizations.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setRole(@SemanticsRole int role) {
                mImpl.setRole(ModifiersProto.SemanticsRole.forNumber(role));
                mFingerprint.recordPropertyUpdate(2, role);
                return this;
            }

            /**
             * Sets the localized state description of the semantics node. For example: "on" or
             * "off". This will be dictated when the element is focused by the screen reader.
             *
             * <p>This field is bindable and will use the dynamic value (if set).
             *
             * @since 1.2
             */
            @NonNull
            public Builder setStateDescription(@NonNull StringProp stateDescription) {
                mImpl.setStateDescription(stateDescription.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(stateDescription.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             *
             * @since 1.0
             */
            @NonNull
            @SuppressWarnings(
                    "deprecation") // Updating a deprecated field for backward compatibility
            public Builder setContentDescription(@NonNull String contentDescription) {
                mImpl.setObsoleteContentDescription(contentDescription);
                mImpl.mergeContentDescription(
                        TypesProto.StringProp.newBuilder().setValue(contentDescription).build());
                mFingerprint.recordPropertyUpdate(4, contentDescription.hashCode());
                return this;
            }

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             *
             * <p>This field is made bindable and will use the dynamic value (if set) from version
             * 1.2 Older renderers will still consider this field as non-bindable and will use the
             * static value.
             *
             * @since 1.0
             */
            @NonNull
            @SuppressWarnings(
                    "deprecation") // Updating a deprecated field for backward compatibility
            public Builder setContentDescription(@NonNull StringProp contentDescription) {
                mImpl.setObsoleteContentDescription(contentDescription.getValue());
                mImpl.setContentDescription(contentDescription.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(contentDescription.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Semantics build() {
                return new Semantics(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A modifier to apply padding around an element.
     *
     * @since 1.0
     */
    public static final class Padding {
        private final ModifiersProto.Padding mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Padding(ModifiersProto.Padding impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the padding on the end of the content, depending on the layout direction, in DP and
         * the value of "rtl_aware".
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getEnd() {
            if (mImpl.hasEnd()) {
                return DpProp.fromProto(mImpl.getEnd());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding on the start of the content, depending on the layout direction, in DP
         * and the value of "rtl_aware".
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getStart() {
            if (mImpl.hasStart()) {
                return DpProp.fromProto(mImpl.getStart());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding at the top, in DP.
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getTop() {
            if (mImpl.hasTop()) {
                return DpProp.fromProto(mImpl.getTop());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding at the bottom, in DP.
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getBottom() {
            if (mImpl.hasBottom()) {
                return DpProp.fromProto(mImpl.getBottom());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the start/end padding is aware of RTL support. If true, the values for
         * start/end will follow the layout direction (i.e. start will refer to the right hand side
         * of the container if the device is using an RTL locale). If false, start/end will always
         * map to left/right, accordingly.
         *
         * @since 1.0
         */
        @Nullable
        public BoolProp getRtlAware() {
            if (mImpl.hasRtlAware()) {
                return BoolProp.fromProto(mImpl.getRtlAware());
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
        public static Padding fromProto(
                @NonNull ModifiersProto.Padding proto, @Nullable Fingerprint fingerprint) {
            return new Padding(proto, fingerprint);
        }

        @NonNull
        static Padding fromProto(@NonNull ModifiersProto.Padding proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Padding toProto() {
            return mImpl;
        }

        /** Builder for {@link Padding} */
        public static final class Builder {
            private final ModifiersProto.Padding.Builder mImpl =
                    ModifiersProto.Padding.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(375605427);

            public Builder() {}

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             *
             * @since 1.0
             */
            @NonNull
            public Builder setEnd(@NonNull DpProp end) {
                mImpl.setEnd(end.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(end.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             *
             * @since 1.0
             */
            @NonNull
            public Builder setStart(@NonNull DpProp start) {
                mImpl.setStart(start.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(start.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding at the top, in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setTop(@NonNull DpProp top) {
                mImpl.setTop(top.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(top.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding at the bottom, in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setBottom(@NonNull DpProp bottom) {
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
             *
             * @since 1.0
             */
            @NonNull
            public Builder setRtlAware(@NonNull BoolProp rtlAware) {
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

            /**
             * Sets the padding for all sides of the content, in DP.
             *
             * @since 1.0
             */
            @NonNull
            @SuppressLint("MissingGetterMatchingBuilder")
            public Builder setAll(@NonNull DpProp value) {
                return setStart(value).setEnd(value).setTop(value).setBottom(value);
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public Padding build() {
                return new Padding(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A modifier to apply a border around an element.
     *
     * @since 1.0
     */
    public static final class Border {
        private final ModifiersProto.Border mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Border(ModifiersProto.Border impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the width of the border, in DP.
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getWidth() {
            if (mImpl.hasWidth()) {
                return DpProp.fromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the color of the border.
         *
         * @since 1.0
         */
        @Nullable
        public ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
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
        public static Border fromProto(
                @NonNull ModifiersProto.Border proto, @Nullable Fingerprint fingerprint) {
            return new Border(proto, fingerprint);
        }

        @NonNull
        static Border fromProto(@NonNull ModifiersProto.Border proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Border toProto() {
            return mImpl;
        }

        /** Builder for {@link Border} */
        public static final class Builder {
            private final ModifiersProto.Border.Builder mImpl = ModifiersProto.Border.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(157094687);

            public Builder() {}

            /**
             * Sets the width of the border, in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setWidth(@NonNull DpProp width) {
                mImpl.setWidth(width.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the color of the border.
             *
             * <p>This field is made bindable and will use the dynamic value (if set) from version
             * 1.2 Older renderers will still consider this field as non-bindable and will use the
             * static value.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
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

    /**
     * The corner of a {@link androidx.wear.protolayout.LayoutElementBuilders.Box} element.
     *
     * @since 1.0
     */
    public static final class Corner {
        private final ModifiersProto.Corner mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Corner(ModifiersProto.Corner impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the radius of the corner in DP.
         *
         * @since 1.0
         */
        @Nullable
        public DpProp getRadius() {
            if (mImpl.hasRadius()) {
                return DpProp.fromProto(mImpl.getRadius());
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
        public static Corner fromProto(
                @NonNull ModifiersProto.Corner proto, @Nullable Fingerprint fingerprint) {
            return new Corner(proto, fingerprint);
        }

        @NonNull
        static Corner fromProto(@NonNull ModifiersProto.Corner proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Corner toProto() {
            return mImpl;
        }

        /** Builder for {@link Corner} */
        public static final class Builder {
            private final ModifiersProto.Corner.Builder mImpl = ModifiersProto.Corner.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-532589910);

            public Builder() {}

            /**
             * Sets the radius of the corner in DP.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setRadius(@NonNull DpProp radius) {
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

    /**
     * A modifier to apply a background to an element.
     *
     * @since 1.0
     */
    public static final class Background {
        private final ModifiersProto.Background mImpl;
        @Nullable private final Fingerprint mFingerprint;

        Background(ModifiersProto.Background impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the background color for this element. If not defined, defaults to being
         * transparent.
         *
         * @since 1.0
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
         * Gets the corner properties of this element. This only affects the drawing of this element
         * if it has a background color or border. If not defined, defaults to having a square
         * corner.
         *
         * @since 1.0
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

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static Background fromProto(
                @NonNull ModifiersProto.Background proto, @Nullable Fingerprint fingerprint) {
            return new Background(proto, fingerprint);
        }

        @NonNull
        static Background fromProto(@NonNull ModifiersProto.Background proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.Background toProto() {
            return mImpl;
        }

        /** Builder for {@link Background} */
        public static final class Builder {
            private final ModifiersProto.Background.Builder mImpl =
                    ModifiersProto.Background.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1234051555);

            public Builder() {}

            /**
             * Sets the background color for this element. If not defined, defaults to being
             * transparent.
             *
             * <p>This field is made bindable and supports dynamic colors from version 1.2 Older
             * renderers will still consider this field as non-bindable and will use the static
             * value.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the corner properties of this element. This only affects the drawing of this
             * element if it has a background color or border. If not defined, defaults to having a
             * square corner.
             *
             * @since 1.0
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
     *
     * @since 1.0
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
         *
         * @since 1.0
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

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ElementMetadata fromProto(
                @NonNull ModifiersProto.ElementMetadata proto, @Nullable Fingerprint fingerprint) {
            return new ElementMetadata(proto, fingerprint);
        }

        @NonNull
        static ElementMetadata fromProto(@NonNull ModifiersProto.ElementMetadata proto) {
            return fromProto(proto, null);
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
            private final Fingerprint mFingerprint = new Fingerprint(-1401175352);

            public Builder() {}

            /**
             * Sets property describing the element with which it is associated. For use by
             * libraries building higher-level components only. This can be used to track component
             * metadata.
             *
             * @since 1.0
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
     *
     * @since 1.0
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
         *
         * @since 1.0
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
         * modified element (eg. screen reader content descriptions).
         *
         * @since 1.0
         */
        @Nullable
        public Semantics getSemantics() {
            if (mImpl.hasSemantics()) {
                return Semantics.fromProto(mImpl.getSemantics());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding of the modified element.
         *
         * @since 1.0
         */
        @Nullable
        public Padding getPadding() {
            if (mImpl.hasPadding()) {
                return Padding.fromProto(mImpl.getPadding());
            } else {
                return null;
            }
        }

        /**
         * Gets the border of the modified element.
         *
         * @since 1.0
         */
        @Nullable
        public Border getBorder() {
            if (mImpl.hasBorder()) {
                return Border.fromProto(mImpl.getBorder());
            } else {
                return null;
            }
        }

        /**
         * Gets the background (with optional corner radius) of the modified element.
         *
         * @since 1.0
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
         *
         * @since 1.0
         */
        @Nullable
        public ElementMetadata getMetadata() {
            if (mImpl.hasMetadata()) {
                return ElementMetadata.fromProto(mImpl.getMetadata());
            } else {
                return null;
            }
        }

        /**
         * Gets the content transition of an element. Any update to the element or its children will
         * trigger this animation for this element and everything underneath it.
         *
         * @since 1.2
         */
        @ProtoLayoutExperimental
        @Nullable
        public AnimatedVisibility getContentUpdateAnimation() {
            if (mImpl.hasContentUpdateAnimation()) {
                return AnimatedVisibility.fromProto(mImpl.getContentUpdateAnimation());
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
            private final Fingerprint mFingerprint = new Fingerprint(-1165106749);

            public Builder() {}

            /**
             * Sets the clickable property of the modified element. It allows its wrapped element to
             * have actions associated with it, which will be executed when the element is tapped.
             *
             * @since 1.0
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
             *
             * @since 1.0
             */
            @NonNull
            public Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(semantics.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding of the modified element.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setPadding(@NonNull Padding padding) {
                mImpl.setPadding(padding.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(padding.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the border of the modified element.
             *
             * @since 1.0
             */
            @NonNull
            public Builder setBorder(@NonNull Border border) {
                mImpl.setBorder(border.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(border.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the background (with optional corner radius) of the modified element.
             *
             * @since 1.0
             */
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
             *
             * @since 1.0
             */
            @NonNull
            public Builder setMetadata(@NonNull ElementMetadata metadata) {
                mImpl.setMetadata(metadata.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(metadata.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the content transition of an element. Any update to the element or its children
             * will trigger this animation for this element and everything underneath it.
             *
             * @since 1.2
             */
            @ProtoLayoutExperimental
            @NonNull
            public Builder setContentUpdateAnimation(
                    @NonNull AnimatedVisibility contentUpdateAnimation) {
                mImpl.setContentUpdateAnimation(contentUpdateAnimation.toProto());
                mFingerprint.recordPropertyUpdate(
                        7,
                        checkNotNull(contentUpdateAnimation.getFingerprint())
                                .aggregateValueAsInt());
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
     * The content transition of an element. Any update to the element or its children will trigger
     * this animation for this element and everything underneath it.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class AnimatedVisibility {
        private final ModifiersProto.AnimatedVisibility mImpl;
        @Nullable private final Fingerprint mFingerprint;

        AnimatedVisibility(
                ModifiersProto.AnimatedVisibility impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the content transition that is triggered when element enters the layout.
         *
         * @since 1.2
         */
        @Nullable
        public EnterTransition getEnterTransition() {
            if (mImpl.hasEnterTransition()) {
                return EnterTransition.fromProto(mImpl.getEnterTransition());
            } else {
                return null;
            }
        }

        /**
         * Gets the content transition that is triggered when element exits the layout. Note that
         * indefinite exit animations are ignored.
         *
         * @since 1.2
         */
        @Nullable
        public ExitTransition getExitTransition() {
            if (mImpl.hasExitTransition()) {
                return ExitTransition.fromProto(mImpl.getExitTransition());
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
        public static AnimatedVisibility fromProto(
                @NonNull ModifiersProto.AnimatedVisibility proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatedVisibility(proto, fingerprint);
        }

        @NonNull
        static AnimatedVisibility fromProto(@NonNull ModifiersProto.AnimatedVisibility proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.AnimatedVisibility toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "AnimatedVisibility{"
                    + "enterTransition="
                    + getEnterTransition()
                    + ", exitTransition="
                    + getExitTransition()
                    + "}";
        }

        /** Builder for {@link AnimatedVisibility} */
        public static final class Builder {
            private final ModifiersProto.AnimatedVisibility.Builder mImpl =
                    ModifiersProto.AnimatedVisibility.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1372451979);

            public Builder() {}

            /**
             * Sets the content transition that is triggered when element enters the layout.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setEnterTransition(@NonNull EnterTransition enterTransition) {
                mImpl.setEnterTransition(enterTransition.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(enterTransition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the content transition that is triggered when element exits the layout. Note
             * that indefinite exit animations are ignored.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setExitTransition(@NonNull ExitTransition exitTransition) {
                mImpl.setExitTransition(exitTransition.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(exitTransition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public AnimatedVisibility build() {
                return new AnimatedVisibility(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The content transition that is triggered when element enters the layout.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class EnterTransition {
        private final ModifiersProto.EnterTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        EnterTransition(ModifiersProto.EnterTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fading in animation for content transition of an element and its children
         * happening when entering the layout.
         *
         * @since 1.2
         */
        @Nullable
        public FadeInTransition getFadeIn() {
            if (mImpl.hasFadeIn()) {
                return FadeInTransition.fromProto(mImpl.getFadeIn());
            } else {
                return null;
            }
        }

        /**
         * Gets the sliding in animation for content transition of an element and its children
         * happening when entering the layout.
         *
         * @since 1.2
         */
        @Nullable
        public SlideInTransition getSlideIn() {
            if (mImpl.hasSlideIn()) {
                return SlideInTransition.fromProto(mImpl.getSlideIn());
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
        public static EnterTransition fromProto(
                @NonNull ModifiersProto.EnterTransition proto, @Nullable Fingerprint fingerprint) {
            return new EnterTransition(proto, fingerprint);
        }

        @NonNull
        static EnterTransition fromProto(@NonNull ModifiersProto.EnterTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.EnterTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "EnterTransition{" + "fadeIn=" + getFadeIn() + ", slideIn=" + getSlideIn() + "}";
        }

        /** Builder for {@link EnterTransition} */
        public static final class Builder {
            private final ModifiersProto.EnterTransition.Builder mImpl =
                    ModifiersProto.EnterTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1732205279);

            public Builder() {}

            /**
             * Sets the fading in animation for content transition of an element and its children
             * happening when entering the layout.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setFadeIn(@NonNull FadeInTransition fadeIn) {
                mImpl.setFadeIn(fadeIn.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(fadeIn.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the sliding in animation for content transition of an element and its children
             * happening when entering the layout.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setSlideIn(@NonNull SlideInTransition slideIn) {
                mImpl.setSlideIn(slideIn.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(slideIn.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public EnterTransition build() {
                return new EnterTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The fading animation for content transition of an element and its children, from the
     * specified starting alpha to fully visible.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class FadeInTransition {
        private final ModifiersProto.FadeInTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FadeInTransition(ModifiersProto.FadeInTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the starting alpha of the fade in transition. It should be between 0 and 1. If not
         * set, defaults to fully transparent, i.e. 0.
         *
         * @since 1.2
         */
        @FloatRange(from = 0.0, to = 1.0)
        public float getInitialAlpha() {
            return mImpl.getInitialAlpha();
        }

        /**
         * Gets the animation parameters for duration, delay, etc.
         *
         * @since 1.2
         */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static FadeInTransition fromProto(
                @NonNull ModifiersProto.FadeInTransition proto, @Nullable Fingerprint fingerprint) {
            return new FadeInTransition(proto, fingerprint);
        }

        @NonNull
        static FadeInTransition fromProto(@NonNull ModifiersProto.FadeInTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.FadeInTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "FadeInTransition{"
                    + "initialAlpha="
                    + getInitialAlpha()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link FadeInTransition} */
        public static final class Builder {
            private final ModifiersProto.FadeInTransition.Builder mImpl =
                    ModifiersProto.FadeInTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1430024488);

            public Builder() {}

            /**
             * Sets the starting alpha of the fade in transition. It should be between 0 and 1. If
             * not set, defaults to fully transparent, i.e. 0.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setInitialAlpha(@FloatRange(from = 0.0, to = 1.0) float initialAlpha) {
                mImpl.setInitialAlpha(initialAlpha);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(initialAlpha));
                return this;
            }

            /**
             * Sets the animation parameters for duration, delay, etc.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FadeInTransition build() {
                return new FadeInTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The sliding in animation for content transition of an element and its children.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class SlideInTransition {
        private final ModifiersProto.SlideInTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SlideInTransition(
                ModifiersProto.SlideInTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the slide direction used for slide animations on any element, from the specified
         * point to its destination in the layout. If not set, defaults to horizontal from left to
         * the right.
         *
         * @since 1.2
         */
        @SlideDirection
        public int getDirection() {
            return mImpl.getDirection().getNumber();
        }

        /**
         * Gets the initial offset for animation. By default the transition starts from the left
         * parent boundary for horizontal orientation and from the top for vertical orientation.
         * Note that sliding from the screen boundaries can only be achieved if all parent's sizes
         * are big enough to accommodate it.
         *
         * @since 1.2
         */
        @Nullable
        public SlideBound getInitialSlideBound() {
            if (mImpl.hasInitialSlideBound()) {
                return ModifiersBuilders.slideBoundFromProto(mImpl.getInitialSlideBound());
            } else {
                return null;
            }
        }

        /**
         * Gets the animation parameters for duration, delay, etc.
         *
         * @since 1.2
         */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static SlideInTransition fromProto(
                @NonNull ModifiersProto.SlideInTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new SlideInTransition(proto, fingerprint);
        }

        @NonNull
        static SlideInTransition fromProto(@NonNull ModifiersProto.SlideInTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.SlideInTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "SlideInTransition{"
                    + "direction="
                    + getDirection()
                    + ", initialSlideBound="
                    + getInitialSlideBound()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link SlideInTransition} */
        public static final class Builder {
            private final ModifiersProto.SlideInTransition.Builder mImpl =
                    ModifiersProto.SlideInTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-991346238);

            public Builder() {}

            /**
             * Sets the slide direction used for slide animations on any element, from the specified
             * point to its destination in the layout. If not set, defaults to horizontal from left
             * to the right.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDirection(@SlideDirection int direction) {
                mImpl.setDirection(ModifiersProto.SlideDirection.forNumber(direction));
                mFingerprint.recordPropertyUpdate(1, direction);
                return this;
            }

            /**
             * Sets the initial offset for animation. By default the transition starts from the left
             * parent boundary for horizontal orientation and from the top for vertical orientation.
             * Note that sliding from the screen boundaries can only be achieved if all parent's
             * sizes are big enough to accommodate it.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setInitialSlideBound(@NonNull SlideBound initialSlideBound) {
                mImpl.setInitialSlideBound(initialSlideBound.toSlideBoundProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(initialSlideBound.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the animation parameters for duration, delay, etc.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SlideInTransition build() {
                return new SlideInTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The content transition that is triggered when element exits the layout.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class ExitTransition {
        private final ModifiersProto.ExitTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        ExitTransition(ModifiersProto.ExitTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fading out animation for content transition of an element and its children
         * happening when exiting the layout.
         *
         * @since 1.2
         */
        @Nullable
        public FadeOutTransition getFadeOut() {
            if (mImpl.hasFadeOut()) {
                return FadeOutTransition.fromProto(mImpl.getFadeOut());
            } else {
                return null;
            }
        }

        /**
         * Gets the sliding out animation for content transition of an element and its children
         * happening when exiting the layout.
         *
         * @since 1.2
         */
        @Nullable
        public SlideOutTransition getSlideOut() {
            if (mImpl.hasSlideOut()) {
                return SlideOutTransition.fromProto(mImpl.getSlideOut());
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
        public static ExitTransition fromProto(
                @NonNull ModifiersProto.ExitTransition proto, @Nullable Fingerprint fingerprint) {
            return new ExitTransition(proto, fingerprint);
        }

        @NonNull
        static ExitTransition fromProto(@NonNull ModifiersProto.ExitTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.ExitTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "ExitTransition{"
                    + "fadeOut="
                    + getFadeOut()
                    + ", slideOut="
                    + getSlideOut()
                    + "}";
        }

        /** Builder for {@link ExitTransition} */
        public static final class Builder {
            private final ModifiersProto.ExitTransition.Builder mImpl =
                    ModifiersProto.ExitTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-99296494);

            public Builder() {}

            /**
             * Sets the fading out animation for content transition of an element and its children
             * happening when exiting the layout.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setFadeOut(@NonNull FadeOutTransition fadeOut) {
                mImpl.setFadeOut(fadeOut.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(fadeOut.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the sliding out animation for content transition of an element and its children
             * happening when exiting the layout.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setSlideOut(@NonNull SlideOutTransition slideOut) {
                mImpl.setSlideOut(slideOut.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(slideOut.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public ExitTransition build() {
                return new ExitTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The fading animation for content transition of an element and its children, from fully
     * visible to the specified target alpha.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class FadeOutTransition {
        private final ModifiersProto.FadeOutTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        FadeOutTransition(
                ModifiersProto.FadeOutTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the target alpha of the fade out transition. It should be between 0 and 1. If not
         * set, defaults to fully invisible, i.e. 0.
         *
         * @since 1.2
         */
        @FloatRange(from = 0.0, to = 1.0)
        public float getTargetAlpha() {
            return mImpl.getTargetAlpha();
        }

        /**
         * Gets the animation parameters for duration, delay, etc.
         *
         * @since 1.2
         */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static FadeOutTransition fromProto(
                @NonNull ModifiersProto.FadeOutTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new FadeOutTransition(proto, fingerprint);
        }

        @NonNull
        static FadeOutTransition fromProto(@NonNull ModifiersProto.FadeOutTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.FadeOutTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "FadeOutTransition{"
                    + "targetAlpha="
                    + getTargetAlpha()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link FadeOutTransition} */
        public static final class Builder {
            private final ModifiersProto.FadeOutTransition.Builder mImpl =
                    ModifiersProto.FadeOutTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-545572295);

            public Builder() {}

            /**
             * Sets the target alpha of the fade out transition. It should be between 0 and 1. If
             * not set, defaults to fully invisible, i.e. 0.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setTargetAlpha(@FloatRange(from = 0.0, to = 1.0) float targetAlpha) {
                mImpl.setTargetAlpha(targetAlpha);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(targetAlpha));
                return this;
            }

            /**
             * Sets the animation parameters for duration, delay, etc.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public FadeOutTransition build() {
                return new FadeOutTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The sliding out animation for content transition of an element and its children.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class SlideOutTransition {
        private final ModifiersProto.SlideOutTransition mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SlideOutTransition(
                ModifiersProto.SlideOutTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the slide direction used for slide animations on any element, from its destination
         * in the layout to the specified point. If not set, defaults to horizontal from right to
         * the left.
         *
         * @since 1.2
         */
        @SlideDirection
        public int getDirection() {
            return mImpl.getDirection().getNumber();
        }

        /**
         * Gets the target offset for animation. By default the transition will end at the left
         * parent boundary for horizontal orientation and at the top for vertical orientation. Note
         * that sliding from the screen boundaries can only be achieved if all parent's sizes are
         * big enough to accommodate it.
         *
         * @since 1.2
         */
        @Nullable
        public SlideBound getTargetSlideBound() {
            if (mImpl.hasTargetSlideBound()) {
                return ModifiersBuilders.slideBoundFromProto(mImpl.getTargetSlideBound());
            } else {
                return null;
            }
        }

        /**
         * Gets the animation parameters for duration, delay, etc.
         *
         * @since 1.2
         */
        @Nullable
        public AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static SlideOutTransition fromProto(
                @NonNull ModifiersProto.SlideOutTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new SlideOutTransition(proto, fingerprint);
        }

        @NonNull
        static SlideOutTransition fromProto(@NonNull ModifiersProto.SlideOutTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public ModifiersProto.SlideOutTransition toProto() {
            return mImpl;
        }

        @Override
        @NonNull
        public String toString() {
            return "SlideOutTransition{"
                    + "direction="
                    + getDirection()
                    + ", targetSlideBound="
                    + getTargetSlideBound()
                    + ", animationSpec="
                    + getAnimationSpec()
                    + "}";
        }

        /** Builder for {@link SlideOutTransition} */
        public static final class Builder {
            private final ModifiersProto.SlideOutTransition.Builder mImpl =
                    ModifiersProto.SlideOutTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(3732844);

            public Builder() {}

            /**
             * Sets the slide direction used for slide animations on any element, from its
             * destination in the layout to the specified point. If not set, defaults to horizontal
             * from right to the left.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setDirection(@SlideDirection int direction) {
                mImpl.setDirection(ModifiersProto.SlideDirection.forNumber(direction));
                mFingerprint.recordPropertyUpdate(1, direction);
                return this;
            }

            /**
             * Sets the target offset for animation. By default the transition will end at the left
             * parent boundary for horizontal orientation and at the top for vertical orientation.
             * Note that sliding from the screen boundaries can only be achieved if all parent's
             * sizes are big enough to accommodate it.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setTargetSlideBound(@NonNull SlideBound targetSlideBound) {
                mImpl.setTargetSlideBound(targetSlideBound.toSlideBoundProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(targetSlideBound.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the animation parameters for duration, delay, etc.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            @NonNull
            public SlideOutTransition build() {
                return new SlideOutTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Interface defining the boundary that a Slide animation will use for start/end.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public interface SlideBound {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        ModifiersProto.SlideBound toSlideBoundProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable
        Fingerprint getFingerprint();

        /** Builder to create {@link SlideBound} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull
            SlideBound build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    @ProtoLayoutExperimental
    public static SlideBound slideBoundFromProto(
            @NonNull ModifiersProto.SlideBound proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasParentBound()) {
            return SlideParentBound.fromProto(proto.getParentBound(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of SlideBound");
    }

    @NonNull
    @ProtoLayoutExperimental
    static SlideBound slideBoundFromProto(@NonNull ModifiersProto.SlideBound proto) {
        return slideBoundFromProto(proto, null);
    }

    /**
     * The slide animation will animate from/to the parent elements boundaries.
     *
     * @since 1.2
     */
    @ProtoLayoutExperimental
    public static final class SlideParentBound implements SlideBound {
        private final ModifiersProto.SlideParentBound mImpl;
        @Nullable private final Fingerprint mFingerprint;

        SlideParentBound(ModifiersProto.SlideParentBound impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the snap options to use when sliding using parent boundaries. Defaults to
         * SLIDE_PARENT_SNAP_TO_INSIDE if not specified.
         *
         * @since 1.2
         */
        @SlideParentSnapOption
        public int getSnapTo() {
            return mImpl.getSnapTo().getNumber();
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
        public static SlideParentBound fromProto(
                @NonNull ModifiersProto.SlideParentBound proto, @Nullable Fingerprint fingerprint) {
            return new SlideParentBound(proto, fingerprint);
        }

        @NonNull
        static SlideParentBound fromProto(@NonNull ModifiersProto.SlideParentBound proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        ModifiersProto.SlideParentBound toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        @ProtoLayoutExperimental
        public ModifiersProto.SlideBound toSlideBoundProto() {
            return ModifiersProto.SlideBound.newBuilder().setParentBound(mImpl).build();
        }

        @Override
        @NonNull
        public String toString() {
            return "SlideParentBound{" + "snapTo=" + getSnapTo() + "}";
        }

        /** Builder for {@link SlideParentBound}. */
        public static final class Builder implements SlideBound.Builder {
            private final ModifiersProto.SlideParentBound.Builder mImpl =
                    ModifiersProto.SlideParentBound.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-516388675);

            public Builder() {}

            /**
             * Sets the snap options to use when sliding using parent boundaries. Defaults to
             * SLIDE_PARENT_SNAP_TO_INSIDE if not specified.
             *
             * @since 1.2
             */
            @NonNull
            public Builder setSnapTo(@SlideParentSnapOption int snapTo) {
                mImpl.setSnapTo(ModifiersProto.SlideParentSnapOption.forNumber(snapTo));
                mFingerprint.recordPropertyUpdate(1, snapTo);
                return this;
            }

            @Override
            @NonNull
            public SlideParentBound build() {
                return new SlideParentBound(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with ArcLayoutElements. These may change the way they are
     * drawn, or change their behaviour.
     *
     * @since 1.0
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
         * executed when the element is tapped.
         *
         * @since 1.0
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
         * descriptions.
         *
         * @since 1.0
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

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static ArcModifiers fromProto(
                @NonNull ModifiersProto.ArcModifiers proto, @Nullable Fingerprint fingerprint) {
            return new ArcModifiers(proto, fingerprint);
        }

        @NonNull
        static ArcModifiers fromProto(@NonNull ModifiersProto.ArcModifiers proto) {
            return fromProto(proto, null);
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
            private final Fingerprint mFingerprint = new Fingerprint(1342182166);

            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             *
             * @since 1.0
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
             *
             * @since 1.0
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
     * androidx.wear.protolayout.LayoutElementBuilders.Span} elements. These may change the way they
     * are drawn, or change their behaviour.
     *
     * @since 1.0
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
         * executed when the element is tapped.
         *
         * @since 1.0
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

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @NonNull
        public static SpanModifiers fromProto(
                @NonNull ModifiersProto.SpanModifiers proto, @Nullable Fingerprint fingerprint) {
            return new SpanModifiers(proto, fingerprint);
        }

        @NonNull
        static SpanModifiers fromProto(@NonNull ModifiersProto.SpanModifiers proto) {
            return fromProto(proto, null);
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
            private final Fingerprint mFingerprint = new Fingerprint(-815102194);

            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             *
             * @since 1.0
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
