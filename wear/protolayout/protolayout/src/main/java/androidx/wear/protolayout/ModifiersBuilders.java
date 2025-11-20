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

import static androidx.wear.protolayout.DimensionBuilders.dp;
import static androidx.wear.protolayout.expression.Preconditions.checkNotNull;

import android.annotation.SuppressLint;
import android.app.PendingIntent;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.ActionBuilders.Action;
import androidx.wear.protolayout.ActionBuilders.PendingIntentAction;
import androidx.wear.protolayout.ColorBuilders.Brush;
import androidx.wear.protolayout.ColorBuilders.ColorProp;
import androidx.wear.protolayout.ColorBuilders.LinearGradient;
import androidx.wear.protolayout.DimensionBuilders.DegreesProp;
import androidx.wear.protolayout.DimensionBuilders.DpProp;
import androidx.wear.protolayout.DimensionBuilders.PivotDimension;
import androidx.wear.protolayout.TypeBuilders.BoolProp;
import androidx.wear.protolayout.TypeBuilders.FloatProp;
import androidx.wear.protolayout.TypeBuilders.StringProp;
import androidx.wear.protolayout.expression.AnimationParameterBuilders.AnimationSpec;
import androidx.wear.protolayout.expression.Fingerprint;
import androidx.wear.protolayout.expression.ProtoLayoutExperimental;
import androidx.wear.protolayout.expression.RequiresSchemaVersion;
import androidx.wear.protolayout.proto.ModifiersProto;
import androidx.wear.protolayout.protobuf.ByteString;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
        @RequiresSchemaVersion(major = 1, minor = 200)
        private static final EnterTransition FADE_IN_ENTER_TRANSITION =
                new EnterTransition.Builder().setFadeIn(FADE_IN_TRANSITION).build();

        /**
         * Slide in transition animation that slides in element when entering the layout into its
         * position from the parent edge in the given direction.
         *
         * @param direction The direction for sliding in transition.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        private static SlideInTransition slideInTransition(@SlideDirection int direction) {
            return new SlideInTransition.Builder().setDirection(direction).build();
        }

        /**
         * Enter content transition animation that fades in element when entering the layout, from
         * fully invisible to fully visible.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull EnterTransition fadeIn() {
            return FADE_IN_ENTER_TRANSITION;
        }

        /**
         * Enter content transition animation that slides in element when entering the layout into
         * its position from the parent edge in the given direction.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull EnterTransition slideIn(@SlideDirection int slideDirection) {
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
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull EnterTransition fadeInSlideIn(@SlideDirection int slideDirection) {
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
        @RequiresSchemaVersion(major = 1, minor = 200)
        private static final ExitTransition FADE_OUT_EXIT_TRANSITION =
                new ExitTransition.Builder().setFadeOut(FADE_OUT_TRANSITION).build();

        /**
         * Slide out transition animation that slides out element when exiting the layout from its
         * position to the parent edge in the given direction.
         *
         * @param direction The direction for sliding out transition.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        private static SlideOutTransition slideOutTransition(@SlideDirection int direction) {
            return new SlideOutTransition.Builder().setDirection(direction).build();
        }

        /**
         * Exit content transition animation that fades out element when exiting the layout, from
         * fully visible to fully invisible.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull ExitTransition fadeOut() {
            return FADE_OUT_EXIT_TRANSITION;
        }

        /**
         * Exit content transition animation that slides out element when exiting the layout from
         * its position to the parent edge in the given direction.
         */
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull ExitTransition slideOut(@SlideDirection int slideDirection) {
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
        @RequiresSchemaVersion(major = 1, minor = 200)
        public static @NonNull ExitTransition fadeOutSlideOut(@SlideDirection int slideDirection) {
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
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
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

    /** Role is undefined. It may be automatically populated. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_NONE = 0;

    /** The element is an image. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_IMAGE = 1;

    /** The element is a Button control. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_BUTTON = 2;

    /**
     * The element is a Checkbox which is a component that represents two states (checked /
     * unchecked).
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_CHECKBOX = 3;

    /**
     * The element is a Switch which is a two state toggleable component that provides on/off like
     * options.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_SWITCH = 4;

    /**
     * This element is a RadioButton which is a component to represent two states, selected and not
     * selected.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    public static final int SEMANTICS_ROLE_RADIOBUTTON = 5;

    /** The snap options to use when sliding using parent boundaries. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
        SLIDE_PARENT_SNAP_UNDEFINED,
        SLIDE_PARENT_SNAP_TO_INSIDE,
        SLIDE_PARENT_SNAP_TO_OUTSIDE
    })
    @Retention(RetentionPolicy.SOURCE)
    @ProtoLayoutExperimental
    public @interface SlideParentSnapOption {}

    /** The undefined snapping option. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_PARENT_SNAP_UNDEFINED = 0;

    /** The option that snaps insides of the element and its parent at start/end. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_PARENT_SNAP_TO_INSIDE = 1;

    /** The option that snaps outsides of the element and its parent at start/end. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_PARENT_SNAP_TO_OUTSIDE = 2;

    /**
     * The slide direction used for slide animations on any element, from the specified point to its
     * destination in the layout for in animation or reverse for out animation.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
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

    /** The undefined sliding orientation. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_DIRECTION_UNDEFINED = 0;

    /** The sliding orientation that moves an element horizontally from left to the right. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_DIRECTION_LEFT_TO_RIGHT = 1;

    /** The sliding orientation that moves an element horizontally from right to the left. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_DIRECTION_RIGHT_TO_LEFT = 2;

    /** The sliding orientation that moves an element vertically from top to the bottom. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_DIRECTION_TOP_TO_BOTTOM = 3;

    /** The sliding orientation that moves an element vertically from bottom to the top. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final int SLIDE_DIRECTION_BOTTOM_TO_TOP = 4;

    /**
     * A modifier for an element which can have associated Actions for click events. When an element
     * with a ClickableModifier is clicked it will fire the associated action.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Clickable {
        private final ModifiersProto.Clickable mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Clickable(ModifiersProto.Clickable impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the ID associated with this action. */
        public @NonNull String getId() {
            return mImpl.getId();
        }

        /** Gets the action to perform when the element this modifier is attached to is clicked. */
        public @Nullable Action getOnClick() {
            if (mImpl.hasOnClick()) {
                return ActionBuilders.actionFromProto(mImpl.getOnClick());
            } else {
                return null;
            }
        }

        /**
         * Gets the minimum width of the clickable area.
         *
         * <p>The default value is 48dp, following the Material design accessibility guideline. Note
         * that this value does not affect the layout, so the minimum clickable width is not
         * guaranteed unless there is enough space around the element within its parent bounds.
         */
        public @NonNull DpProp getMinimumClickableWidth() {
            if (mImpl.hasMinimumClickableWidth()) {
                return DpProp.fromProto(mImpl.getMinimumClickableWidth());
            } else {
                return new DpProp.Builder(48f).build();
            }
        }

        /**
         * Gets the minimum height of the clickable area.
         *
         * <p>The default value is 48dp, following the Material design accessibility guideline. Note
         * that this value does not affect the layout, so the minimum clickable height is not
         * guaranteed unless there is enough space around the element within its parent bounds.
         */
        public @NonNull DpProp getMinimumClickableHeight() {
            if (mImpl.hasMinimumClickableHeight()) {
                return DpProp.fromProto(mImpl.getMinimumClickableHeight());
            } else {
                return new DpProp.Builder(48f).build();
            }
        }

        /**
         * Gets whether the click visual feedback (such as a ripple) should be enabled. Defaults to
         * true.
         */
        public boolean isVisualFeedbackEnabled() {
            if (mImpl.hasVisualFeedbackEnabled()) {
                return mImpl.getVisualFeedbackEnabled();
            } else {
                return true;
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Clickable fromProto(
                ModifiersProto.@NonNull Clickable proto, @Nullable Fingerprint fingerprint) {
            return new Clickable(proto, fingerprint);
        }

        static @NonNull Clickable fromProto(ModifiersProto.@NonNull Clickable proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Clickable toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Clickable{"
                    + "id="
                    + getId()
                    + ", onClick="
                    + getOnClick()
                    + ", minimumClickableWidth="
                    + getMinimumClickableWidth()
                    + ", minimumClickableHeight="
                    + getMinimumClickableHeight()
                    + ", disableVisualFeedback="
                    + isVisualFeedbackEnabled()
                    + "}";
        }

        /** Builder for {@link Clickable} */
        public static final class Builder {
            private final ModifiersProto.Clickable.Builder mImpl =
                    ModifiersProto.Clickable.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(812136104);

            /** Sets the ID associated with this action. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setId(@NonNull String id) {
                mImpl.setId(id);
                mFingerprint.recordPropertyUpdate(1, id.hashCode());
                return this;
            }

            /**
             * Sets the minimum width of the clickable area.
             *
             * <p>The default value is 48dp, following the Material design accessibility guideline.
             * Note that this value does not affect the layout, so the minimum clickable width is
             * not guaranteed unless there is enough space around the element within its parent
             * bounds.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setMinimumClickableWidth(
                    @NonNull DpProp minimumClickableWidth) {
                if (minimumClickableWidth.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Clickable.Builder.setMinimumClickableWidth doesn't support dynamic"
                                    + " values.");
                }
                mImpl.setMinimumClickableWidth(minimumClickableWidth.toProto());
                mFingerprint.recordPropertyUpdate(
                        3,
                        checkNotNull(minimumClickableWidth.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the minimum height of the clickable area.
             *
             * <p>The default value is 48dp, following the Material design accessibility guideline.
             * Note that this value does not affect the layout, so the minimum clickable height is
             * not guaranteed unless there is enough space around the element within its parent
             * bounds.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setMinimumClickableHeight(
                    @NonNull DpProp minimumClickableHeight) {
                if (minimumClickableHeight.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Clickable.Builder.setMinimumClickableHeight doesn't support dynamic"
                                    + " values.");
                }
                mImpl.setMinimumClickableHeight(minimumClickableHeight.toProto());
                mFingerprint.recordPropertyUpdate(
                        4,
                        checkNotNull(minimumClickableHeight.getFingerprint())
                                .aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the click visual feedback (such as a ripple) should be enabled. Defaults
             * to true.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setVisualFeedbackEnabled(boolean visualFeedbackEnabled) {
                mImpl.setVisualFeedbackEnabled(visualFeedbackEnabled);
                mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(visualFeedbackEnabled));
                return this;
            }

            private final @Nullable ProtoLayoutScope mScope;
            private @Nullable PendingIntent mPendingIntent = null;

            /**
             * Creates an instance of {@link Builder}.
             *
             * <p>Note that, builder created with this constructor can not be used to invoke {@link
             * #setOnClick(PendingIntent)}, otherwise an exception will be thrown. Instead, create
             * the builder with {@link #Builder(ProtoLayoutScope, String)} for setting a {@link
             * PendingIntent} as onClick action.
             */
            public Builder() {
                mScope = null;
            }

            /**
             * Creates an instance of {@link Builder} which is able to accept a {@link
             * PendingIntent} as onClick action. Builder instance created with this constructor
             * works with other type of actions as well.
             *
             * <p>Note that, calling {@code #setId} would override the clickable Id set here.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public Builder(@NonNull ProtoLayoutScope scope, @NonNull String clickableId) {
                this.mScope = scope;
                setId(clickableId);
            }

            /**
             * Sets the action to perform when the element this modifier is attached to is clicked.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setOnClick(@NonNull Action onClick) {
                mImpl.setOnClick(onClick.toActionProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(onClick.getFingerprint()).aggregateValueAsInt());
                mPendingIntent = null;
                return this;
            }

            /**
             * Sets the {@link PendingIntent} to perform when the element this modifier is attached
             * to is clicked.
             *
             * <p>Note that this method is mutually exclusive with {@code #setOnClick(Action)}, the
             * later method call will override the previous one.
             *
             * @throws IllegalArgumentException if the builder is not constructed with {@link
             *     #Builder(ProtoLayoutScope, String)}.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            public @NonNull Builder setOnClick(@NonNull PendingIntent pendingIntent) {
                if (mScope == null) {
                    throw new IllegalStateException(
                            "Clickable.Builder.setOnClick(PendingIntent) needs to be called with"
                                    + " constructor that accepts ProtoLayoutScope.");
                }
                setOnClick(new PendingIntentAction.Builder().build());
                mPendingIntent = pendingIntent;
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Clickable build() {
                ModifiersProto.Clickable protoImpl = mImpl.build();
                if (mPendingIntent != null && mScope != null) {
                    // register the pending intent to the scope for later collection.
                    mScope.registerPendingIntent(protoImpl.getId(), mPendingIntent);
                }
                return new Clickable(protoImpl, mFingerprint);
            }
        }
    }

    /**
     * A modifier for an element which has accessibility semantics associated with it. This should
     * generally be used sparingly, and in most cases should only be applied to the top-level layout
     * element or to Clickables.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Semantics {
        private final ModifiersProto.Semantics mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Semantics(ModifiersProto.Semantics impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the content description associated with this element. This will be dictated when the
         * element is focused by the screen reader.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable StringProp getContentDescription() {
            if (mImpl.hasContentDescription()) {
                return StringProp.fromProto(mImpl.getContentDescription());
            } else {
                return null;
            }
        }

        /**
         * Gets the type of user interface element. Accessibility services might use this to
         * describe the element or do customizations.
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
         */
        public @Nullable StringProp getStateDescription() {
            if (mImpl.hasStateDescription()) {
                return StringProp.fromProto(mImpl.getStateDescription());
            } else {
                return null;
            }
        }

        /** Gets whether this element should be marked as heading for accessibility. */
        public boolean isHeading() {
            return mImpl.getHeading();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Semantics fromProto(
                ModifiersProto.@NonNull Semantics proto, @Nullable Fingerprint fingerprint) {
            return new Semantics(proto, fingerprint);
        }

        static @NonNull Semantics fromProto(ModifiersProto.@NonNull Semantics proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Semantics toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Semantics{"
                    + "contentDescription="
                    + getContentDescription()
                    + ", role="
                    + getRole()
                    + ", stateDescription="
                    + getStateDescription()
                    + ", heading="
                    + isHeading()
                    + "}";
        }

        /** Builder for {@link Semantics} */
        public static final class Builder {
            private final ModifiersProto.Semantics.Builder mImpl =
                    ModifiersProto.Semantics.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1679805809);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the type of user interface element. Accessibility services might use this to
             * describe the element or do customizations.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setRole(@SemanticsRole int role) {
                mImpl.setRole(ModifiersProto.SemanticsRole.forNumber(role));
                mFingerprint.recordPropertyUpdate(2, role);
                return this;
            }

            /**
             * Sets the localized state description of the semantics node. For example: "on" or
             * "off". This will be dictated when the element is focused by the screen reader.
             *
             * <p>This field is bindable and will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setStateDescription(@NonNull StringProp stateDescription) {
                mImpl.setStateDescription(stateDescription.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(stateDescription.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether this element should be marked as heading for accessibility. Defaults to
             * false.
             */
            @RequiresSchemaVersion(major = 1, minor = 600)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setHeading(boolean heading) {
                mImpl.setHeading(heading);
                mFingerprint.recordPropertyUpdate(5, Boolean.hashCode(heading));
                return this;
            }

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressWarnings(
                    "deprecation") // Updating a deprecated field for backward compatibility
            public @NonNull Builder setContentDescription(@NonNull String contentDescription) {
                return setContentDescription(new StringProp.Builder(contentDescription).build());
            }

            /**
             * Sets the content description associated with this element. This will be dictated when
             * the element is focused by the screen reader.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressWarnings(
                    "deprecation") // Updating a deprecated field for backward compatibility
            public @NonNull Builder setContentDescription(@NonNull StringProp contentDescription) {
                mImpl.setObsoleteContentDescription(contentDescription.getValue());
                mImpl.setContentDescription(contentDescription.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(contentDescription.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Semantics build() {
                return new Semantics(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply padding around an element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Padding {
        private final ModifiersProto.Padding mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Padding(ModifiersProto.Padding impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the padding on the end of the content, depending on the layout direction, in DP and
         * the value of "rtl_aware".
         *
         * <p>Note that negative values are supported on schema versions of 1.6 or higher.
         */
        public @Nullable DpProp getEnd() {
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
         * <p>Note that negative values are supported on schema versions of 1.6 or higher.
         */
        public @Nullable DpProp getStart() {
            if (mImpl.hasStart()) {
                return DpProp.fromProto(mImpl.getStart());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding at the top, in DP.
         *
         * <p>Note that negative values are supported on schema versions of 1.6 or higher.
         */
        public @Nullable DpProp getTop() {
            if (mImpl.hasTop()) {
                return DpProp.fromProto(mImpl.getTop());
            } else {
                return null;
            }
        }

        /**
         * Gets the padding at the bottom, in DP.
         *
         * <p>Note that negative values are supported on schema versions of 1.6 or higher.
         */
        public @Nullable DpProp getBottom() {
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
         */
        @Nullable BoolProp isRtlAware() {
            if (mImpl.hasRtlAware()) {
                return BoolProp.fromProto(mImpl.getRtlAware());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the start/end padding is aware of RTL support. If true, the values for
         * start/end will follow the layout direction (i.e. start will refer to the right hand side
         * of the container if the device is using an RTL locale). If false, start/end will always
         * map to left/right, accordingly.
         */
        public @Nullable BoolProp getRtlAware() {
            return isRtlAware();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Padding fromProto(
                ModifiersProto.@NonNull Padding proto, @Nullable Fingerprint fingerprint) {
            return new Padding(proto, fingerprint);
        }

        static @NonNull Padding fromProto(ModifiersProto.@NonNull Padding proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Padding toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Padding{"
                    + "end="
                    + getEnd()
                    + ", start="
                    + getStart()
                    + ", top="
                    + getTop()
                    + ", bottom="
                    + getBottom()
                    + ", rtlAware="
                    + getRtlAware()
                    + "}";
        }

        /** Builder for {@link Padding} */
        public static final class Builder {
            private final ModifiersProto.Padding.Builder mImpl =
                    ModifiersProto.Padding.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(375605427);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the padding on the end of the content, depending on the layout direction, in DP
             * and the value of "rtl_aware".
             *
             * <p>Note that negative values are supported on schema versions of 1.6 or higher.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setEnd(@NonNull DpProp end) {
                if (end.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Padding.Builder.setEnd doesn't support dynamic values.");
                }
                mImpl.setEnd(end.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(end.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding on the start of the content, depending on the layout direction, in
             * DP and the value of "rtl_aware".
             *
             * <p>Note that negative values are supported on schema versions of 1.6 or higher.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setStart(@NonNull DpProp start) {
                if (start.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Padding.Builder.setStart doesn't support dynamic values.");
                }
                mImpl.setStart(start.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(start.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding at the top, in DP.
             *
             * <p>Note that negative values are supported on schema versions of 1.6 or higher.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setTop(@NonNull DpProp top) {
                if (top.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Padding.Builder.setTop doesn't support dynamic values.");
                }
                mImpl.setTop(top.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(top.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the padding at the bottom, in DP.
             *
             * <p>Note that negative values are supported on schema versions of 1.6 or higher.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setBottom(@NonNull DpProp bottom) {
                if (bottom.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Padding.Builder.setBottom doesn't support dynamic values.");
                }
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
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setRtlAware(@NonNull BoolProp rtlAware) {
                if (rtlAware.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Padding.Builder.setRtlAware doesn't support dynamic values.");
                }
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setRtlAware(boolean rtlAware) {
                return setRtlAware(new BoolProp.Builder(rtlAware).build());
            }

            /**
             * Sets the padding for all sides of the content, in DP.
             *
             * <p>Note that negative values are supported on schema versions of 1.6 or higher.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setAll(@NonNull DpProp value) {
                return setStart(value).setEnd(value).setTop(value).setBottom(value);
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Padding build() {
                return new Padding(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply a border around an element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Border {
        private final ModifiersProto.Border mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Border(ModifiersProto.Border impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the width of the border, in DP. */
        public @Nullable DpProp getWidth() {
            if (mImpl.hasWidth()) {
                return DpProp.fromProto(mImpl.getWidth());
            } else {
                return null;
            }
        }

        /**
         * Gets the color of the border.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         */
        public @Nullable ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
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
        public static @NonNull Border fromProto(
                ModifiersProto.@NonNull Border proto, @Nullable Fingerprint fingerprint) {
            return new Border(proto, fingerprint);
        }

        static @NonNull Border fromProto(ModifiersProto.@NonNull Border proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Border toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Border{" + "width=" + getWidth() + ", color=" + getColor() + "}";
        }

        /** Builder for {@link Border} */
        public static final class Builder {
            private final ModifiersProto.Border.Builder mImpl = ModifiersProto.Border.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(157094687);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the width of the border, in DP.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setWidth(@NonNull DpProp width) {
                if (width.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Border.Builder.setWidth doesn't support dynamic values.");
                }
                mImpl.setWidth(width.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(width.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the color of the border.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Border build() {
                return new Border(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A radius for either circular or elliptical shapes. */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public static final class CornerRadius {
        private final ModifiersProto.CornerRadius mImpl;
        private final @Nullable Fingerprint mFingerprint;

        CornerRadius(ModifiersProto.CornerRadius impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the radius value in dp on the horizontal axis. */
        public @NonNull DpProp getX() {
            return DpProp.fromProto(mImpl.getX());
        }

        /** Gets the radius value in dp on the vertical axis. */
        public @NonNull DpProp getY() {
            return DpProp.fromProto(mImpl.getY());
        }

        /**
         * A radius with values on both horizontal and vertical axes set to zero. It can be used to
         * have right-angle corners.
         */
        @RequiresSchemaVersion(major = 1, minor = 400)
        private static final CornerRadius ZERO =
                new CornerRadius.Builder(
                                new DpProp.Builder(0f).build(), new DpProp.Builder(0f).build())
                        .build();

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull CornerRadius fromProto(
                ModifiersProto.@NonNull CornerRadius proto, @Nullable Fingerprint fingerprint) {
            return new CornerRadius(proto, fingerprint);
        }

        static @NonNull CornerRadius fromProto(ModifiersProto.@NonNull CornerRadius proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull CornerRadius toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "CornerRadius{" + "x=" + getX() + ", y=" + getY() + "}";
        }

        /** Builder for {@link CornerRadius} */
        public static final class Builder {
            private final ModifiersProto.CornerRadius.Builder mImpl =
                    ModifiersProto.CornerRadius.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-2143429106);

            /**
             * Creates an instance of {@link Builder}.
             *
             * @param x the radius value in dp on the horizontal axis.
             * @param y the radius value in dp on the vertical axis.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @SuppressLint("CheckResult") // (b/247804720)
            public Builder(@NonNull DpProp x, @NonNull DpProp y) {
                setX(x);
                setY(y);
            }

            @RequiresSchemaVersion(major = 1, minor = 400)
            Builder() {}

            /**
             * Sets the radius value in dp on the horizontal axis.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setX(@NonNull DpProp x) {
                if (x.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "CornerRadius.Builder.setX doesn't support dynamic values.");
                }
                mImpl.setX(x.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(x.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius value in dp on the vertical axis.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            @NonNull Builder setY(@NonNull DpProp y) {
                if (y.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "CornerRadius.Builder.setY doesn't support dynamic values.");
                }
                mImpl.setY(y.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(y.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull CornerRadius build() {
                return new CornerRadius(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The corner of a {@link androidx.wear.protolayout.LayoutElementBuilders.Box} element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Corner {
        private final ModifiersProto.Corner mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Corner(ModifiersProto.Corner impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the radius of the corner in DP. */
        public @Nullable DpProp getRadius() {
            if (mImpl.hasRadius()) {
                return DpProp.fromProto(mImpl.getRadius());
            } else {
                return null;
            }
        }

        /** Gets the radius for the top-left corner of either circular or elliptical shapes. */
        public @NonNull CornerRadius getTopLeftRadius() {
            if (mImpl.hasTopLeftRadius()) {
                return CornerRadius.fromProto(mImpl.getTopLeftRadius());
            } else {
                return toCornerRadius(getRadius());
            }
        }

        /** Gets the radius for the top-right corner of either circular or elliptical shapes. */
        public @NonNull CornerRadius getTopRightRadius() {
            if (mImpl.hasTopRightRadius()) {
                return CornerRadius.fromProto(mImpl.getTopRightRadius());
            } else {
                return toCornerRadius(getRadius());
            }
        }

        /** Gets the radius for the bottom-right corner of either circular or elliptical shapes. */
        public @NonNull CornerRadius getBottomRightRadius() {
            if (mImpl.hasBottomRightRadius()) {
                return CornerRadius.fromProto(mImpl.getBottomRightRadius());
            } else {
                return toCornerRadius(getRadius());
            }
        }

        /** Gets the radius for the bottom-left corner of either circular or elliptical shapes. */
        public @NonNull CornerRadius getBottomLeftRadius() {
            if (mImpl.hasBottomLeftRadius()) {
                return CornerRadius.fromProto(mImpl.getBottomLeftRadius());
            } else {
                return toCornerRadius(getRadius());
            }
        }

        @SuppressLint("ProtoLayoutMinSchema")
        private @NonNull CornerRadius toCornerRadius(@Nullable DpProp radius) {
            return radius == null
                    ? CornerRadius.ZERO
                    : new CornerRadius.Builder(dp(radius.getValue()), dp(radius.getValue()))
                            .build();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Corner fromProto(
                ModifiersProto.@NonNull Corner proto, @Nullable Fingerprint fingerprint) {
            return new Corner(proto, fingerprint);
        }

        static @NonNull Corner fromProto(ModifiersProto.@NonNull Corner proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Corner toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Corner{"
                    + "radius="
                    + getRadius()
                    + ", topLeftRadius="
                    + getTopLeftRadius()
                    + ", topRightRadius="
                    + getTopRightRadius()
                    + ", bottomRightRadius="
                    + getBottomRightRadius()
                    + ", bottomLeftRadius="
                    + getBottomLeftRadius()
                    + "}";
        }

        /** Builder for {@link Corner} */
        public static final class Builder {
            private final ModifiersProto.Corner.Builder mImpl = ModifiersProto.Corner.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-532589910);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the radius of the corner in DP.
             *
             * <p>The shape for a specific corner can be overridden by setting that corner
             * separately.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setRadius(@NonNull DpProp radius) {
                if (radius.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Corner.Builder.setRadius doesn't support dynamic values.");
                }
                mImpl.setRadius(radius.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(radius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius for the top-left corner of either circular or elliptical shapes. If
             * not set, defaults to radius for both horizontal and vertical axes when radius is set;
             * or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTopLeftRadius(@NonNull CornerRadius topLeftRadius) {
                mImpl.setTopLeftRadius(topLeftRadius.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(topLeftRadius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius for the top-right corner of either circular or elliptical shapes. If
             * not set, defaults to radius for both horizontal and vertical axes when radius is set;
             * or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTopRightRadius(@NonNull CornerRadius topRightRadius) {
                mImpl.setTopRightRadius(topRightRadius.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(topRightRadius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius for the bottom-right corner of either circular or elliptical shapes.
             * If not set, defaults to radius for both horizontal and vertical axes when radius is
             * set; or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setBottomRightRadius(@NonNull CornerRadius bottomRightRadius) {
                mImpl.setBottomRightRadius(bottomRightRadius.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(bottomRightRadius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius for the bottom-left corner of either circular or elliptical shapes.
             * If not set, defaults to radius for both horizontal and vertical axes when radius is
             * set; or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setBottomLeftRadius(@NonNull CornerRadius bottomLeftRadius) {
                mImpl.setBottomLeftRadius(bottomLeftRadius.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(bottomLeftRadius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the radius for the top-left corner of either circular or elliptical shapes. If
             * not set, defaults to radius for both horizontal and vertical axes when radius is set;
             * or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTopLeftRadius(
                    @NonNull DpProp xRadius, @NonNull DpProp yRadius) {
                return setTopLeftRadius(new CornerRadius.Builder(xRadius, yRadius).build());
            }

            /**
             * Sets the radius for the top-right corner of either circular or elliptical shapes. If
             * not set, defaults to radius for both horizontal and vertical axes when radius is set;
             * or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTopRightRadius(
                    @NonNull DpProp xRadius, @NonNull DpProp yRadius) {
                return setTopRightRadius(new CornerRadius.Builder(xRadius, yRadius).build());
            }

            /**
             * Sets the radius for the bottom-right corner of either circular or elliptical shapes.
             * If not set, defaults to radius for both horizontal and vertical axes when radius is
             * set; or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setBottomRightRadius(
                    @NonNull DpProp xRadius, @NonNull DpProp yRadius) {
                return setBottomRightRadius(new CornerRadius.Builder(xRadius, yRadius).build());
            }

            /**
             * Sets the radius for the bottom-left corner of either circular or elliptical shapes.
             * If not set, defaults to radius for both horizontal and vertical axes when radius is
             * set; or defaults to zeros when radius is also not set.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setBottomLeftRadius(
                    @NonNull DpProp xRadius, @NonNull DpProp yRadius) {
                return setBottomLeftRadius(new CornerRadius.Builder(xRadius, yRadius).build());
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Corner build() {
                return new Corner(mImpl.build(), mFingerprint);
            }
        }
    }

    /** A modifier to apply a background to an element. */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Background {
        private final ModifiersProto.Background mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Background(ModifiersProto.Background impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the background color for this element. If not defined, defaults to being
         * transparent.
         *
         * <p>While this field is statically accessible from 1.0, it's only bindable since version
         * 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
         *
         * <p>If a brush is set, this color will only be used if brush is not supported by the
         * renderer (versions below 1.5).
         */
        public @Nullable ColorProp getColor() {
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
         */
        public @Nullable Corner getCorner() {
            if (mImpl.hasCorner()) {
                return Corner.fromProto(mImpl.getCorner());
            } else {
                return null;
            }
        }

        /**
         * Gets a brush used to draw the background. If set, the brush will be used instead of the
         * color provided in {@code setColor()}.
         */
        public @Nullable Brush getBrush() {
            if (mImpl.hasBrush()) {
                return ColorBuilders.brushFromProto(mImpl.getBrush());
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
        public static @NonNull Background fromProto(
                ModifiersProto.@NonNull Background proto, @Nullable Fingerprint fingerprint) {
            return new Background(proto, fingerprint);
        }

        static @NonNull Background fromProto(ModifiersProto.@NonNull Background proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Background toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Background{"
                    + "color="
                    + getColor()
                    + ", corner="
                    + getCorner()
                    + ", brush="
                    + getBrush()
                    + "}";
        }

        /** Builder for {@link Background} */
        public static final class Builder {
            private final ModifiersProto.Background.Builder mImpl =
                    ModifiersProto.Background.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1234051555);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the background color for this element. If not defined, defaults to being
             * transparent.
             *
             * <p>While this field is statically accessible from 1.0, it's only bindable since
             * version 1.2 and renderers supporting version 1.2 will use the dynamic value (if set).
             *
             * <p>If a brush is set, this color will only be used if brush is not supported by the
             * renderer (versions below 1.5).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
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
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setCorner(@NonNull Corner corner) {
                mImpl.setCorner(corner.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(corner.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets a brush used to draw the background. If set and supported, the brush will be
             * used instead of the color provided in {@code setColor()}.
             *
             * @throws IllegalArgumentException if the brush is not a {@link LinearGradient}.
             */
            @RequiresSchemaVersion(major = 1, minor = 500)
            public @NonNull Builder setBrush(@NonNull Brush brush) {
                if (!(brush instanceof LinearGradient)) {
                    throw new IllegalArgumentException(
                            "Only LinearGradient is supported for Background.");
                }
                mImpl.setBrush(brush.toBrushProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(brush.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Background build() {
                return new Background(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * Metadata about an element. For use by libraries building higher-level components only. This
     * can be used to track component metadata.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ElementMetadata {
        private final ModifiersProto.ElementMetadata mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ElementMetadata(ModifiersProto.ElementMetadata impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets property describing the element with which it is associated. For use by libraries
         * building higher-level components only. This can be used to track component metadata.
         */
        public byte @NonNull [] getTagData() {
            return mImpl.getTagData().toByteArray();
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull ElementMetadata fromProto(
                ModifiersProto.@NonNull ElementMetadata proto, @Nullable Fingerprint fingerprint) {
            return new ElementMetadata(proto, fingerprint);
        }

        static @NonNull ElementMetadata fromProto(ModifiersProto.@NonNull ElementMetadata proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull ElementMetadata toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ElementMetadata{" + "tagData=" + Arrays.toString(getTagData()) + "}";
        }

        /** Builder for {@link ElementMetadata} */
        public static final class Builder {
            private final ModifiersProto.ElementMetadata.Builder mImpl =
                    ModifiersProto.ElementMetadata.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1401175352);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets property describing the element with which it is associated. For use by
             * libraries building higher-level components only. This can be used to track component
             * metadata.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setTagData(byte @NonNull [] tagData) {
                mImpl.setTagData(ByteString.copyFrom(tagData));
                mFingerprint.recordPropertyUpdate(1, Arrays.hashCode(tagData));
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ElementMetadata build() {
                return new ElementMetadata(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * A modifier to apply transformations to the element. All of these transformations can be
     * animated by setting dynamic values. This modifier is not layout affecting.
     */
    @RequiresSchemaVersion(major = 1, minor = 400)
    public static final class Transformation {
        private final ModifiersProto.Transformation mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Transformation(ModifiersProto.Transformation impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the horizontal offset of this element relative to the location where the element's
         * layout placed it.
         */
        public @NonNull DpProp getTranslationX() {
            if (mImpl.hasTranslationX()) {
                return DpProp.fromProto(mImpl.getTranslationX());
            } else {
                return new DpProp.Builder(0f).build();
            }
        }

        /**
         * Gets the vertical offset of this element in addition to the location where the element's
         * layout placed it.
         */
        public @NonNull DpProp getTranslationY() {
            if (mImpl.hasTranslationY()) {
                return DpProp.fromProto(mImpl.getTranslationY());
            } else {
                return new DpProp.Builder(0f).build();
            }
        }

        /**
         * Gets the scale of this element in the x direction around the pivot point, as a proportion
         * of the element's unscaled width.
         */
        public @NonNull FloatProp getScaleX() {
            if (mImpl.hasScaleX()) {
                return FloatProp.fromProto(mImpl.getScaleX());
            } else {
                return new FloatProp.Builder(1f).build();
            }
        }

        /**
         * Gets the scale of this element in the y direction around the pivot point, as a proportion
         * of the element's unscaled height.
         */
        public @NonNull FloatProp getScaleY() {
            if (mImpl.hasScaleY()) {
                return FloatProp.fromProto(mImpl.getScaleY());
            } else {
                return new FloatProp.Builder(1f).build();
            }
        }

        /** Gets the clockwise Degrees that the element is rotated around the pivot point. */
        public @NonNull DegreesProp getRotation() {
            if (mImpl.hasRotation()) {
                return DegreesProp.fromProto(mImpl.getRotation());
            } else {
                return new DegreesProp.Builder(0f).build();
            }
        }

        /**
         * Gets the horizontal location of the point around which the element is rotated and scaled.
         * With type {@link DpProp}, it is the offset from the element center; otherwise with type
         * {@link BoundingBoxRatio}, it is the location proportional to the bounding box width.
         */
        public @NonNull PivotDimension getPivotX() {
            if (mImpl.hasPivotX()) {
                return DimensionBuilders.pivotDimensionFromProto(mImpl.getPivotX());
            } else {
                return new DpProp.Builder(0f).build();
            }
        }

        /**
         * Gets the vertical location of the point around which the element is rotated and scaled.
         * With type {@link DpProp}, it is the offset from the element center; otherwise with type
         * {@link BoundingBoxRatio}, it is the location proportional to the bounding box height.
         */
        public @NonNull PivotDimension getPivotY() {
            if (mImpl.hasPivotY()) {
                return DimensionBuilders.pivotDimensionFromProto(mImpl.getPivotY());
            } else {
                return new DpProp.Builder(0f).build();
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Transformation fromProto(
                ModifiersProto.@NonNull Transformation proto, @Nullable Fingerprint fingerprint) {
            return new Transformation(proto, fingerprint);
        }

        static @NonNull Transformation fromProto(ModifiersProto.@NonNull Transformation proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Transformation toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Transformation{"
                    + "translationX="
                    + getTranslationX()
                    + ", translationY="
                    + getTranslationY()
                    + ", scaleX="
                    + getScaleX()
                    + ", scaleY="
                    + getScaleY()
                    + ", rotation="
                    + getRotation()
                    + ", pivotX="
                    + getPivotX()
                    + ", pivotY="
                    + getPivotY()
                    + "}";
        }

        /** Builder for {@link Transformation} */
        public static final class Builder {
            private final ModifiersProto.Transformation.Builder mImpl =
                    ModifiersProto.Transformation.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(369448770);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the horizontal offset of this element relative to the location where the
             * element's layout placed it. If not set, defaults to zero.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTranslationX(@NonNull DpProp translationX) {
                mImpl.setTranslationX(translationX.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(translationX.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical offset of this element in addition to the location where the
             * element's layout placed it. If not set, defaults to zero.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTranslationY(@NonNull DpProp translationY) {
                mImpl.setTranslationY(translationY.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(translationY.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the scale of this element in the x direction around the pivot point, as a
             * proportion of the element's unscaled width. If not set, defaults to one.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setScaleX(@NonNull FloatProp scaleX) {
                mImpl.setScaleX(scaleX.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(scaleX.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the scale of this element in the y direction around the pivot point, as a
             * proportion of the element's unscaled height. If not set, defaults to one.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setScaleY(@NonNull FloatProp scaleY) {
                mImpl.setScaleY(scaleY.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(scaleY.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the clockwise degrees that the element is rotated around the pivot point. If not
             * set, defaults to zero.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setRotation(@NonNull DegreesProp rotation) {
                mImpl.setRotation(rotation.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(rotation.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the horizontal location of the point around which the element is rotated and
             * scaled. With type {@link DpProp}, it is the offset from the element center; otherwise
             * with type {@link BoundingBoxRatio}, it is the location proportional to the bounding
             * box width. Dynamic value is supported. If not set, defaults to the element center.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setPivotX(@NonNull PivotDimension pivotX) {
                mImpl.setPivotX(pivotX.toPivotDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(pivotX.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the vertical location of the point around which the element is rotated and
             * scaled. With type {@link DpProp}, it is the offset from the element center; otherwise
             * with type {@link BoundingBoxRatio}, it is the location proportional to the bounding
             * box height. Dynamic value is supported. If not set, defaults to the element center.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setPivotY(@NonNull PivotDimension pivotY) {
                mImpl.setPivotY(pivotY.toPivotDimensionProto());
                mFingerprint.recordPropertyUpdate(
                        7, checkNotNull(pivotY.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Transformation build() {
                return new Transformation(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} for an element. These may change the way they are drawn (e.g. {@link
     * Padding} or {@link Background}), or change their behaviour (e.g. {@link Clickable}, or {@link
     * Semantics}).
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class Modifiers {
        private final ModifiersProto.Modifiers mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Modifiers(ModifiersProto.Modifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the clickable property of the modified element. It allows its wrapped element to
         * have actions associated with it, which will be executed when the element is tapped.
         */
        public @Nullable Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
            } else {
                return null;
            }
        }

        /**
         * Gets the semantics of the modified element. This can be used to add metadata to the
         * modified element (eg. screen reader content descriptions).
         */
        public @Nullable Semantics getSemantics() {
            if (mImpl.hasSemantics()) {
                return Semantics.fromProto(mImpl.getSemantics());
            } else {
                return null;
            }
        }

        /** Gets the padding of the modified element. */
        public @Nullable Padding getPadding() {
            if (mImpl.hasPadding()) {
                return Padding.fromProto(mImpl.getPadding());
            } else {
                return null;
            }
        }

        /** Gets the border of the modified element. */
        public @Nullable Border getBorder() {
            if (mImpl.hasBorder()) {
                return Border.fromProto(mImpl.getBorder());
            } else {
                return null;
            }
        }

        /** Gets the background (with optional corner radius) of the modified element. */
        public @Nullable Background getBackground() {
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
        public @Nullable ElementMetadata getMetadata() {
            if (mImpl.hasMetadata()) {
                return ElementMetadata.fromProto(mImpl.getMetadata());
            } else {
                return null;
            }
        }

        /**
         * Gets the content transition of an element. Any update to the element or its children will
         * trigger this animation for this element and everything underneath it.
         */
        @ProtoLayoutExperimental
        public @Nullable AnimatedVisibility getContentUpdateAnimation() {
            if (mImpl.hasContentUpdateAnimation()) {
                return AnimatedVisibility.fromProto(mImpl.getContentUpdateAnimation());
            } else {
                return null;
            }
        }

        /**
         * Gets whether the attached element is visible, or hidden. If the element is hidden, then
         * it will still consume space in the layout, but will not render any contents, nor will any
         * children render any contents. Defaults to visible.
         */
        @ProtoLayoutExperimental
        public @NonNull BoolProp isVisible() {
            if (mImpl.hasVisible()) {
                return BoolProp.fromProto(mImpl.getVisible());
            } else {
                return new BoolProp.Builder(true).build();
            }
        }

        /** Gets the transformation applied to the element post-layout. */
        public @Nullable Transformation getTransformation() {
            if (mImpl.hasTransformation()) {
                return Transformation.fromProto(mImpl.getTransformation());
            } else {
                return null;
            }
        }

        /**
         * Gets the opacity of the element with a value from 0 to 1, where 0 means the view is the
         * element is completely transparent and 1 means the element is completely opaque.
         */
        public @Nullable FloatProp getOpacity() {
            if (mImpl.hasOpacity()) {
                return FloatProp.fromProto(mImpl.getOpacity());
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
        public static @NonNull Modifiers fromProto(
                ModifiersProto.@NonNull Modifiers proto, @Nullable Fingerprint fingerprint) {
            return new Modifiers(proto, fingerprint);
        }

        /**
         * Creates a new wrapper instance from the proto. Intended for testing purposes only. An
         * object created using this method can't be added to any other wrapper.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Modifiers fromProto(ModifiersProto.@NonNull Modifiers proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Modifiers toProto() {
            return mImpl;
        }

        @Override
        @OptIn(markerClass = ProtoLayoutExperimental.class)
        public @NonNull String toString() {
            return "Modifiers{"
                    + "clickable="
                    + getClickable()
                    + ", semantics="
                    + getSemantics()
                    + ", padding="
                    + getPadding()
                    + ", border="
                    + getBorder()
                    + ", background="
                    + getBackground()
                    + ", metadata="
                    + getMetadata()
                    + ", contentUpdateAnimation="
                    + getContentUpdateAnimation()
                    + ", visible="
                    + isVisible()
                    + ", transformation="
                    + getTransformation()
                    + ", opacity="
                    + getOpacity()
                    + "}";
        }

        /** Builder for {@link Modifiers} */
        public static final class Builder {
            private final ModifiersProto.Modifiers.Builder mImpl =
                    ModifiersProto.Modifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1165106749);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the clickable property of the modified element. It allows its wrapped element to
             * have actions associated with it, which will be executed when the element is tapped.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the semantics of the modified element. This can be used to add metadata to the
             * modified element (eg. screen reader content descriptions).
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(semantics.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the padding of the modified element. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setPadding(@NonNull Padding padding) {
                mImpl.setPadding(padding.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(padding.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the border of the modified element. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setBorder(@NonNull Border border) {
                mImpl.setBorder(border.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(border.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the background (with optional corner radius) of the modified element. */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setBackground(@NonNull Background background) {
                mImpl.setBackground(background.toProto());
                mFingerprint.recordPropertyUpdate(
                        5, checkNotNull(background.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets metadata about an element. For use by libraries building higher-level components
             * only. This can be used to track component metadata.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setMetadata(@NonNull ElementMetadata metadata) {
                mImpl.setMetadata(metadata.toProto());
                mFingerprint.recordPropertyUpdate(
                        6, checkNotNull(metadata.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the content transition of an element. Any update to the element or its children
             * will trigger this animation for this element and everything underneath it.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            @ProtoLayoutExperimental
            public @NonNull Builder setContentUpdateAnimation(
                    @NonNull AnimatedVisibility contentUpdateAnimation) {
                mImpl.setContentUpdateAnimation(contentUpdateAnimation.toProto());
                mFingerprint.recordPropertyUpdate(
                        7,
                        checkNotNull(contentUpdateAnimation.getFingerprint())
                                .aggregateValueAsInt());
                return this;
            }

            /**
             * Sets whether the attached element is visible, or hidden. If the element is hidden,
             * then it will still consume space in the layout, but will not render any contents, nor
             * will any children render any contents. Defaults to visible.
             *
             * <p>Note that a hidden element also cannot be clickable (i.e. a {@link Clickable}
             * modifier would be ignored).
             *
             * <p>This field is bindable and will use the dynamic value (if set).
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            @ProtoLayoutExperimental
            @SuppressLint("MissingGetterMatchingBuilder")
            public @NonNull Builder setVisible(@NonNull BoolProp visible) {
                mImpl.setVisible(visible.toProto());
                mFingerprint.recordPropertyUpdate(
                        10, checkNotNull(visible.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the transformation applied to the element post-layout. */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setTransformation(@NonNull Transformation transformation) {
                mImpl.setTransformation(transformation.toProto());
                mFingerprint.recordPropertyUpdate(
                        11, checkNotNull(transformation.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the opacity of the element with a value from 0 to 1, where 0 means the element
             * is completely transparent and 1 means the element is completely opaque. Dynamic value
             * is supported.
             */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setOpacity(@NonNull FloatProp opacity) {
                mImpl.setOpacity(opacity.toProto());
                mFingerprint.recordPropertyUpdate(
                        12, checkNotNull(opacity.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Modifiers build() {
                return new Modifiers(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The content transition of an element. Any update to the element or its children will trigger
     * this animation for this element and everything underneath it.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class AnimatedVisibility {
        private final ModifiersProto.AnimatedVisibility mImpl;
        private final @Nullable Fingerprint mFingerprint;

        AnimatedVisibility(
                ModifiersProto.AnimatedVisibility impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /** Gets the content transition that is triggered when element enters the layout. */
        public @Nullable EnterTransition getEnterTransition() {
            if (mImpl.hasEnterTransition()) {
                return EnterTransition.fromProto(mImpl.getEnterTransition());
            } else {
                return null;
            }
        }

        /**
         * Gets the content transition that is triggered when element exits the layout. Note that
         * indefinite exit animations are ignored.
         */
        public @Nullable ExitTransition getExitTransition() {
            if (mImpl.hasExitTransition()) {
                return ExitTransition.fromProto(mImpl.getExitTransition());
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
        public static @NonNull AnimatedVisibility fromProto(
                ModifiersProto.@NonNull AnimatedVisibility proto,
                @Nullable Fingerprint fingerprint) {
            return new AnimatedVisibility(proto, fingerprint);
        }

        static @NonNull AnimatedVisibility fromProto(
                ModifiersProto.@NonNull AnimatedVisibility proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull AnimatedVisibility toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /** Sets the content transition that is triggered when element enters the layout. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setEnterTransition(@NonNull EnterTransition enterTransition) {
                mImpl.setEnterTransition(enterTransition.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(enterTransition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the content transition that is triggered when element exits the layout. Note
             * that indefinite exit animations are ignored.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setExitTransition(@NonNull ExitTransition exitTransition) {
                mImpl.setExitTransition(exitTransition.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(exitTransition.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull AnimatedVisibility build() {
                return new AnimatedVisibility(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The content transition that is triggered when element enters the layout. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class EnterTransition {
        private final ModifiersProto.EnterTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        EnterTransition(ModifiersProto.EnterTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fading in animation for content transition of an element and its children
         * happening when entering the layout.
         */
        public @Nullable FadeInTransition getFadeIn() {
            if (mImpl.hasFadeIn()) {
                return FadeInTransition.fromProto(mImpl.getFadeIn());
            } else {
                return null;
            }
        }

        /**
         * Gets the sliding in animation for content transition of an element and its children
         * happening when entering the layout.
         */
        public @Nullable SlideInTransition getSlideIn() {
            if (mImpl.hasSlideIn()) {
                return SlideInTransition.fromProto(mImpl.getSlideIn());
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
        public static @NonNull EnterTransition fromProto(
                ModifiersProto.@NonNull EnterTransition proto, @Nullable Fingerprint fingerprint) {
            return new EnterTransition(proto, fingerprint);
        }

        static @NonNull EnterTransition fromProto(ModifiersProto.@NonNull EnterTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull EnterTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "EnterTransition{" + "fadeIn=" + getFadeIn() + ", slideIn=" + getSlideIn() + "}";
        }

        /** Builder for {@link EnterTransition} */
        public static final class Builder {
            private final ModifiersProto.EnterTransition.Builder mImpl =
                    ModifiersProto.EnterTransition.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1732205279);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the fading in animation for content transition of an element and its children
             * happening when entering the layout.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setFadeIn(@NonNull FadeInTransition fadeIn) {
                mImpl.setFadeIn(fadeIn.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(fadeIn.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the sliding in animation for content transition of an element and its children
             * happening when entering the layout.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setSlideIn(@NonNull SlideInTransition slideIn) {
                mImpl.setSlideIn(slideIn.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(slideIn.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull EnterTransition build() {
                return new EnterTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The fading animation for content transition of an element and its children, from the
     * specified starting alpha to fully visible.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class FadeInTransition {
        private final ModifiersProto.FadeInTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FadeInTransition(ModifiersProto.FadeInTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the starting alpha of the fade in transition. It should be between 0 and 1. If not
         * set, defaults to fully transparent, i.e. 0.
         */
        @FloatRange(from = 0.0, to = 1.0)
        public float getInitialAlpha() {
            return mImpl.getInitialAlpha();
        }

        /** Gets the animation parameters for duration, delay, etc. */
        public @Nullable AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static @NonNull FadeInTransition fromProto(
                ModifiersProto.@NonNull FadeInTransition proto, @Nullable Fingerprint fingerprint) {
            return new FadeInTransition(proto, fingerprint);
        }

        static @NonNull FadeInTransition fromProto(ModifiersProto.@NonNull FadeInTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull FadeInTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the starting alpha of the fade in transition. It should be between 0 and 1. If
             * not set, defaults to fully transparent, i.e. 0.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setInitialAlpha(
                    @FloatRange(from = 0.0, to = 1.0) float initialAlpha) {
                mImpl.setInitialAlpha(initialAlpha);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(initialAlpha));
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull FadeInTransition build() {
                return new FadeInTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The sliding in animation for content transition of an element and its children. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class SlideInTransition {
        private final ModifiersProto.SlideInTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SlideInTransition(
                ModifiersProto.SlideInTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the slide direction used for slide animations on any element, from the specified
         * point to its destination in the layout. If not set, defaults to horizontal from left to
         * the right.
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
         */
        public @Nullable SlideBound getInitialSlideBound() {
            if (mImpl.hasInitialSlideBound()) {
                return ModifiersBuilders.slideBoundFromProto(mImpl.getInitialSlideBound());
            } else {
                return null;
            }
        }

        /** Gets the animation parameters for duration, delay, etc. */
        public @Nullable AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static @NonNull SlideInTransition fromProto(
                ModifiersProto.@NonNull SlideInTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new SlideInTransition(proto, fingerprint);
        }

        static @NonNull SlideInTransition fromProto(
                ModifiersProto.@NonNull SlideInTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull SlideInTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the slide direction used for slide animations on any element, from the specified
             * point to its destination in the layout. If not set, defaults to horizontal from left
             * to the right.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setDirection(@SlideDirection int direction) {
                mImpl.setDirection(ModifiersProto.SlideDirection.forNumber(direction));
                mFingerprint.recordPropertyUpdate(1, direction);
                return this;
            }

            /**
             * Sets the initial offset for animation. By default the transition starts from the left
             * parent boundary for horizontal orientation and from the top for vertical orientation.
             * Note that sliding from the screen boundaries can only be achieved if all parent's
             * sizes are big enough to accommodate it.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setInitialSlideBound(@NonNull SlideBound initialSlideBound) {
                mImpl.setInitialSlideBound(initialSlideBound.toSlideBoundProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(initialSlideBound.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull SlideInTransition build() {
                return new SlideInTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The content transition that is triggered when element exits the layout. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class ExitTransition {
        private final ModifiersProto.ExitTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ExitTransition(ModifiersProto.ExitTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the fading out animation for content transition of an element and its children
         * happening when exiting the layout.
         */
        public @Nullable FadeOutTransition getFadeOut() {
            if (mImpl.hasFadeOut()) {
                return FadeOutTransition.fromProto(mImpl.getFadeOut());
            } else {
                return null;
            }
        }

        /**
         * Gets the sliding out animation for content transition of an element and its children
         * happening when exiting the layout.
         */
        public @Nullable SlideOutTransition getSlideOut() {
            if (mImpl.hasSlideOut()) {
                return SlideOutTransition.fromProto(mImpl.getSlideOut());
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
        public static @NonNull ExitTransition fromProto(
                ModifiersProto.@NonNull ExitTransition proto, @Nullable Fingerprint fingerprint) {
            return new ExitTransition(proto, fingerprint);
        }

        static @NonNull ExitTransition fromProto(ModifiersProto.@NonNull ExitTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull ExitTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the fading out animation for content transition of an element and its children
             * happening when exiting the layout.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setFadeOut(@NonNull FadeOutTransition fadeOut) {
                mImpl.setFadeOut(fadeOut.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(fadeOut.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the sliding out animation for content transition of an element and its children
             * happening when exiting the layout.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setSlideOut(@NonNull SlideOutTransition slideOut) {
                mImpl.setSlideOut(slideOut.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(slideOut.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ExitTransition build() {
                return new ExitTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * The fading animation for content transition of an element and its children, from fully
     * visible to the specified target alpha.
     */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class FadeOutTransition {
        private final ModifiersProto.FadeOutTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        FadeOutTransition(
                ModifiersProto.FadeOutTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the target alpha of the fade out transition. It should be between 0 and 1. If not
         * set, defaults to fully invisible, i.e. 0.
         */
        @FloatRange(from = 0.0, to = 1.0)
        public float getTargetAlpha() {
            return mImpl.getTargetAlpha();
        }

        /** Gets the animation parameters for duration, delay, etc. */
        public @Nullable AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static @NonNull FadeOutTransition fromProto(
                ModifiersProto.@NonNull FadeOutTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new FadeOutTransition(proto, fingerprint);
        }

        static @NonNull FadeOutTransition fromProto(
                ModifiersProto.@NonNull FadeOutTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull FadeOutTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the target alpha of the fade out transition. It should be between 0 and 1. If
             * not set, defaults to fully invisible, i.e. 0.
             */
            public @NonNull Builder setTargetAlpha(
                    @FloatRange(from = 0.0, to = 1.0) float targetAlpha) {
                mImpl.setTargetAlpha(targetAlpha);
                mFingerprint.recordPropertyUpdate(1, Float.floatToIntBits(targetAlpha));
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull FadeOutTransition build() {
                return new FadeOutTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The sliding out animation for content transition of an element and its children. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class SlideOutTransition {
        private final ModifiersProto.SlideOutTransition mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SlideOutTransition(
                ModifiersProto.SlideOutTransition impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the slide direction used for slide animations on any element, from its destination
         * in the layout to the specified point. If not set, defaults to horizontal from right to
         * the left.
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
         */
        public @Nullable SlideBound getTargetSlideBound() {
            if (mImpl.hasTargetSlideBound()) {
                return ModifiersBuilders.slideBoundFromProto(mImpl.getTargetSlideBound());
            } else {
                return null;
            }
        }

        /** Gets the animation parameters for duration, delay, etc. */
        public @Nullable AnimationSpec getAnimationSpec() {
            if (mImpl.hasAnimationSpec()) {
                return AnimationSpec.fromProto(mImpl.getAnimationSpec());
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
        public static @NonNull SlideOutTransition fromProto(
                ModifiersProto.@NonNull SlideOutTransition proto,
                @Nullable Fingerprint fingerprint) {
            return new SlideOutTransition(proto, fingerprint);
        }

        static @NonNull SlideOutTransition fromProto(
                ModifiersProto.@NonNull SlideOutTransition proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull SlideOutTransition toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
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

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the slide direction used for slide animations on any element, from its
             * destination in the layout to the specified point. If not set, defaults to horizontal
             * from right to the left.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setDirection(@SlideDirection int direction) {
                mImpl.setDirection(ModifiersProto.SlideDirection.forNumber(direction));
                mFingerprint.recordPropertyUpdate(1, direction);
                return this;
            }

            /**
             * Sets the target offset for animation. By default the transition will end at the left
             * parent boundary for horizontal orientation and at the top for vertical orientation.
             * Note that sliding from the screen boundaries can only be achieved if all parent's
             * sizes are big enough to accommodate it.
             */
            public @NonNull Builder setTargetSlideBound(@NonNull SlideBound targetSlideBound) {
                mImpl.setTargetSlideBound(targetSlideBound.toSlideBoundProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(targetSlideBound.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the animation parameters for duration, delay, etc. */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setAnimationSpec(@NonNull AnimationSpec animationSpec) {
                mImpl.setAnimationSpec(animationSpec.toProto());
                mFingerprint.recordPropertyUpdate(
                        3, checkNotNull(animationSpec.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull SlideOutTransition build() {
                return new SlideOutTransition(mImpl.build(), mFingerprint);
            }
        }
    }

    /** Interface defining the boundary that a Slide animation will use for start/end. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public interface SlideBound {
        /** Get the protocol buffer representation of this object. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ModifiersProto.@NonNull SlideBound toSlideBoundProto();

        /** Get the fingerprint for this object or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Nullable Fingerprint getFingerprint();

        /** Builder to create {@link SlideBound} objects. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        interface Builder {

            /** Builds an instance with values accumulated in this Builder. */
            @NonNull SlideBound build();
        }
    }

    /** Creates a new wrapper instance from the proto. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ProtoLayoutExperimental
    public static @NonNull SlideBound slideBoundFromProto(
            ModifiersProto.@NonNull SlideBound proto, @Nullable Fingerprint fingerprint) {
        if (proto.hasParentBound()) {
            return SlideParentBound.fromProto(proto.getParentBound(), fingerprint);
        }
        throw new IllegalStateException("Proto was not a recognised instance of SlideBound");
    }

    @ProtoLayoutExperimental
    static @NonNull SlideBound slideBoundFromProto(ModifiersProto.@NonNull SlideBound proto) {
        return slideBoundFromProto(proto, null);
    }

    /** The slide animation will animate from/to the parent elements boundaries. */
    @RequiresSchemaVersion(major = 1, minor = 200)
    @ProtoLayoutExperimental
    public static final class SlideParentBound implements SlideBound {
        private final ModifiersProto.SlideParentBound mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SlideParentBound(ModifiersProto.SlideParentBound impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the snap options to use when sliding using parent boundaries. Defaults to
         * SLIDE_PARENT_SNAP_TO_INSIDE if not specified.
         */
        @SlideParentSnapOption
        public int getSnapTo() {
            return mImpl.getSnapTo().getNumber();
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull SlideParentBound fromProto(
                ModifiersProto.@NonNull SlideParentBound proto, @Nullable Fingerprint fingerprint) {
            return new SlideParentBound(proto, fingerprint);
        }

        static @NonNull SlideParentBound fromProto(ModifiersProto.@NonNull SlideParentBound proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        ModifiersProto.@NonNull SlideParentBound toProto() {
            return mImpl;
        }

        @Override
        @RestrictTo(Scope.LIBRARY_GROUP)
        @ProtoLayoutExperimental
        public ModifiersProto.@NonNull SlideBound toSlideBoundProto() {
            return ModifiersProto.SlideBound.newBuilder().setParentBound(mImpl).build();
        }

        @Override
        public @NonNull String toString() {
            return "SlideParentBound{" + "snapTo=" + getSnapTo() + "}";
        }

        /** Builder for {@link SlideParentBound}. */
        @SuppressWarnings("HiddenSuperclass")
        public static final class Builder implements SlideBound.Builder {
            private final ModifiersProto.SlideParentBound.Builder mImpl =
                    ModifiersProto.SlideParentBound.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-516388675);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the snap options to use when sliding using parent boundaries. Defaults to
             * SLIDE_PARENT_SNAP_TO_INSIDE if not specified.
             */
            @RequiresSchemaVersion(major = 1, minor = 200)
            public @NonNull Builder setSnapTo(@SlideParentSnapOption int snapTo) {
                mImpl.setSnapTo(ModifiersProto.SlideParentSnapOption.forNumber(snapTo));
                mFingerprint.recordPropertyUpdate(1, snapTo);
                return this;
            }

            /** Builds an instance from accumulated values. */
            @Override
            public @NonNull SlideParentBound build() {
                return new SlideParentBound(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with ArcLayoutElements. These may change the way they are
     * drawn, or change their behaviour.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class ArcModifiers {
        private final ModifiersProto.ArcModifiers mImpl;
        private final @Nullable Fingerprint mFingerprint;

        ArcModifiers(ModifiersProto.ArcModifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets allows its wrapped element to have actions associated with it, which will be
         * executed when the element is tapped.
         */
        public @Nullable Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
            } else {
                return null;
            }
        }

        /**
         * Gets adds metadata for the modified element, for example, screen reader content
         * descriptions.
         */
        public @Nullable Semantics getSemantics() {
            if (mImpl.hasSemantics()) {
                return Semantics.fromProto(mImpl.getSemantics());
            } else {
                return null;
            }
        }

        /**
         * Gets the opacity of the element with a value from 0 to 1, where 0 means the element is
         * completely transparent and 1 means the element is completely opaque. Dynamic value is
         * supported.
         */
        public @Nullable FloatProp getOpacity() {
            if (mImpl.hasOpacity()) {
                return FloatProp.fromProto(mImpl.getOpacity());
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
        public static @NonNull ArcModifiers fromProto(
                ModifiersProto.@NonNull ArcModifiers proto, @Nullable Fingerprint fingerprint) {
            return new ArcModifiers(proto, fingerprint);
        }

        static @NonNull ArcModifiers fromProto(ModifiersProto.@NonNull ArcModifiers proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull ArcModifiers toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "ArcModifiers{"
                    + "clickable="
                    + getClickable()
                    + ", semantics="
                    + getSemantics()
                    + ", opacity="
                    + getOpacity()
                    + "}";
        }

        /** Builder for {@link ArcModifiers} */
        public static final class Builder {
            private final ModifiersProto.ArcModifiers.Builder mImpl =
                    ModifiersProto.ArcModifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(1342182166);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets adds metadata for the modified element, for example, screen reader content
             * descriptions.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setSemantics(@NonNull Semantics semantics) {
                mImpl.setSemantics(semantics.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(semantics.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Sets the opacity of the element. */
            @RequiresSchemaVersion(major = 1, minor = 400)
            public @NonNull Builder setOpacity(@NonNull FloatProp opacity) {
                mImpl.setOpacity(opacity.toProto());
                mFingerprint.recordPropertyUpdate(
                        4, checkNotNull(opacity.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull ArcModifiers build() {
                return new ArcModifiers(mImpl.build(), mFingerprint);
            }
        }
    }

    /**
     * {@link Modifiers} that can be used with {@link
     * androidx.wear.protolayout.LayoutElementBuilders.Span} elements. These may change the way they
     * are drawn, or change their behaviour.
     */
    @RequiresSchemaVersion(major = 1, minor = 0)
    public static final class SpanModifiers {
        private final ModifiersProto.SpanModifiers mImpl;
        private final @Nullable Fingerprint mFingerprint;

        SpanModifiers(ModifiersProto.SpanModifiers impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets allows its wrapped element to have actions associated with it, which will be
         * executed when the element is tapped.
         */
        public @Nullable Clickable getClickable() {
            if (mImpl.hasClickable()) {
                return Clickable.fromProto(mImpl.getClickable());
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
        public static @NonNull SpanModifiers fromProto(
                ModifiersProto.@NonNull SpanModifiers proto, @Nullable Fingerprint fingerprint) {
            return new SpanModifiers(proto, fingerprint);
        }

        static @NonNull SpanModifiers fromProto(ModifiersProto.@NonNull SpanModifiers proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull SpanModifiers toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "SpanModifiers{" + "clickable=" + getClickable() + "}";
        }

        /** Builder for {@link SpanModifiers} */
        public static final class Builder {
            private final ModifiersProto.SpanModifiers.Builder mImpl =
                    ModifiersProto.SpanModifiers.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-815102194);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets allows its wrapped element to have actions associated with it, which will be
             * executed when the element is tapped.
             */
            @RequiresSchemaVersion(major = 1, minor = 0)
            public @NonNull Builder setClickable(@NonNull Clickable clickable) {
                mImpl.setClickable(clickable.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(clickable.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull SpanModifiers build() {
                return new SpanModifiers(mImpl.build(), mFingerprint);
            }
        }
    }

    /** The shadow definition. The shadow is drawn as a blur region around the element. */
    @RequiresSchemaVersion(major = 1, minor = 300)
    public static final class Shadow {
        private final ModifiersProto.Shadow mImpl;
        private final @Nullable Fingerprint mFingerprint;

        Shadow(ModifiersProto.Shadow impl, @Nullable Fingerprint fingerprint) {
            this.mImpl = impl;
            this.mFingerprint = fingerprint;
        }

        /**
         * Gets the blur radius of the shadow. It controls the size of the blur that is drawn. When
         * set to zero, the shadow is not drawn. Defaults to zero.
         */
        public @NonNull DpProp getBlurRadius() {
            if (mImpl.hasBlurRadius()) {
                return DpProp.fromProto(mImpl.getBlurRadius());
            } else {
                return new DpProp.Builder(0).build();
            }
        }

        /** Gets the color used in the shadow. Defaults to Black. */
        public @NonNull ColorProp getColor() {
            if (mImpl.hasColor()) {
                return ColorProp.fromProto(mImpl.getColor());
            } else {
                return new ColorProp.Builder(0xFF000000).build();
            }
        }

        /** Get the fingerprint for this object, or null if unknown. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @Nullable Fingerprint getFingerprint() {
            return mFingerprint;
        }

        /** Creates a new wrapper instance from the proto. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public static @NonNull Shadow fromProto(
                ModifiersProto.@NonNull Shadow proto, @Nullable Fingerprint fingerprint) {
            return new Shadow(proto, fingerprint);
        }

        static @NonNull Shadow fromProto(ModifiersProto.@NonNull Shadow proto) {
            return fromProto(proto, null);
        }

        /** Returns the internal proto instance. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public ModifiersProto.@NonNull Shadow toProto() {
            return mImpl;
        }

        @Override
        public @NonNull String toString() {
            return "Shadow{" + "blurRadius=" + getBlurRadius() + ", color=" + getColor() + "}";
        }

        /** Builder for {@link Shadow} */
        public static final class Builder {
            private final ModifiersProto.Shadow.Builder mImpl = ModifiersProto.Shadow.newBuilder();
            private final Fingerprint mFingerprint = new Fingerprint(-1267428773);

            /** Creates an instance of {@link Builder}. */
            public Builder() {}

            /**
             * Sets the blur radius of the shadow. It controls the size of the blur that is drawn.
             * When set to zero, the shadow is not drawn. Defaults to zero.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setBlurRadius(@NonNull DpProp blurRadius) {
                if (blurRadius.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Shadow.Builder.setBlurRadius doesn't support dynamic values.");
                }
                mImpl.setBlurRadius(blurRadius.toProto());
                mFingerprint.recordPropertyUpdate(
                        1, checkNotNull(blurRadius.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /**
             * Sets the color used in the shadow. Defaults to Black.
             *
             * <p>Note that this field only supports static values.
             */
            @RequiresSchemaVersion(major = 1, minor = 300)
            public @NonNull Builder setColor(@NonNull ColorProp color) {
                if (color.getDynamicValue() != null) {
                    throw new IllegalArgumentException(
                            "Shadow.Builder.setColor doesn't support dynamic values.");
                }
                mImpl.setColor(color.toProto());
                mFingerprint.recordPropertyUpdate(
                        2, checkNotNull(color.getFingerprint()).aggregateValueAsInt());
                return this;
            }

            /** Builds an instance from accumulated values. */
            public @NonNull Shadow build() {
                return new Shadow(mImpl.build(), mFingerprint);
            }
        }
    }
}
