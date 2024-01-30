/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.mediarouter.media;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Allows applications to customize the list of routes used for media routing (for example, in the
 * System UI Output Switcher).
 *
 * @see MediaRouter#setRouteListingPreference
 * @see RouteListingPreference.Item
 */
public final class RouteListingPreference {

    /**
     * {@link Intent} action that the system uses to take the user the app when the user selects an
     * {@link RouteListingPreference.Item} whose {@link
     * RouteListingPreference.Item#getSelectionBehavior() selection behavior} is {@link
     * RouteListingPreference.Item#SELECTION_BEHAVIOR_GO_TO_APP}.
     *
     * <p>The launched intent will identify the selected item using the extra identified by {@link
     * #EXTRA_ROUTE_ID}.
     *
     * @see #getLinkedItemComponentName()
     * @see RouteListingPreference.Item#SELECTION_BEHAVIOR_GO_TO_APP
     */
    @SuppressLint("ActionValue") // Field & value copied from android.media.RouteListingPreference.
    public static final String ACTION_TRANSFER_MEDIA =
            android.media.RouteListingPreference.ACTION_TRANSFER_MEDIA;

    /**
     * {@link Intent} string extra key that contains the {@link
     * RouteListingPreference.Item#getRouteId() id} of the route to transfer to, as part of an
     * {@link #ACTION_TRANSFER_MEDIA} intent.
     *
     * @see #getLinkedItemComponentName()
     * @see RouteListingPreference.Item#SELECTION_BEHAVIOR_GO_TO_APP
     */
    @SuppressLint("ActionValue") // Field & value copied from android.media.RouteListingPreference.
    public static final String EXTRA_ROUTE_ID = android.media.RouteListingPreference.EXTRA_ROUTE_ID;

    @NonNull private final List<RouteListingPreference.Item> mItems;
    private final boolean mIsSystemOrderingEnabled;
    @Nullable private final ComponentName mLinkedItemComponentName;

    // Must be package private to avoid a synthetic accessor for the builder.
    /* package */ RouteListingPreference(RouteListingPreference.Builder builder) {
        mItems = builder.mItems;
        mIsSystemOrderingEnabled = builder.mIsSystemOrderingEnabled;
        mLinkedItemComponentName = builder.mLinkedItemComponentName;
    }

    /**
     * Returns an unmodifiable list containing the {@link RouteListingPreference.Item items} that
     * the app wants to be listed for media routing.
     */
    @NonNull
    public List<RouteListingPreference.Item> getItems() {
        return mItems;
    }

    /**
     * Returns true if the application would like media route listing to use the system's ordering
     * strategy, or false if the application would like route listing to respect the ordering
     * obtained from {@link #getItems()}.
     *
     * <p>The system's ordering strategy is implementation-dependent, but may take into account each
     * route's recency or frequency of use in order to rank them.
     */
    public boolean isSystemOrderingEnabled() {
        return mIsSystemOrderingEnabled;
    }

    /**
     * Returns a {@link ComponentName} for navigating to the application.
     *
     * <p>Must not be null if any of the {@link #getItems() items} of this route listing preference
     * has {@link RouteListingPreference.Item#getSelectionBehavior() selection behavior} {@link
     * RouteListingPreference.Item#SELECTION_BEHAVIOR_GO_TO_APP}.
     *
     * <p>The system navigates to the application when the user selects {@link
     * RouteListingPreference.Item} with {@link
     * RouteListingPreference.Item#SELECTION_BEHAVIOR_GO_TO_APP} by launching an intent to the
     * returned {@link ComponentName}, using action {@link #ACTION_TRANSFER_MEDIA}, with the extra
     * {@link #EXTRA_ROUTE_ID}.
     */
    @Nullable
    public ComponentName getLinkedItemComponentName() {
        return mLinkedItemComponentName;
    }

    // Equals and hashCode.

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RouteListingPreference)) {
            return false;
        }
        RouteListingPreference that = (RouteListingPreference) other;
        return mItems.equals(that.mItems)
                && mIsSystemOrderingEnabled == that.mIsSystemOrderingEnabled
                && Objects.equals(mLinkedItemComponentName, that.mLinkedItemComponentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mItems, mIsSystemOrderingEnabled, mLinkedItemComponentName);
    }

    // Internal methods.

    @RequiresApi(api = 34)
    @NonNull /* package */
    android.media.RouteListingPreference toPlatformRouteListingPreference() {
        return Api34Impl.toPlatformRouteListingPreference(this);
    }

    // Inner classes.

    /** Builder for {@link RouteListingPreference}. */
    public static final class Builder {

        // The builder fields must be package private to avoid synthetic accessors.
        /* package */ List<RouteListingPreference.Item> mItems;
        /* package */ boolean mIsSystemOrderingEnabled;
        /* package */ ComponentName mLinkedItemComponentName;

        /** Creates a new instance with default values (documented in the setters). */
        public Builder() {
            mItems = Collections.emptyList();
            mIsSystemOrderingEnabled = true;
        }

        /**
         * See {@link #getItems()}
         *
         * <p>The default value is an empty list.
         */
        @NonNull
        public RouteListingPreference.Builder setItems(
                @NonNull List<RouteListingPreference.Item> items) {
            mItems = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(items)));
            return this;
        }

        /**
         * See {@link #isSystemOrderingEnabled()}
         *
         * <p>The default value is {@code true}.
         */
        @NonNull
        public RouteListingPreference.Builder setSystemOrderingEnabled(
                boolean systemOrderingEnabled) {
            mIsSystemOrderingEnabled = systemOrderingEnabled;
            return this;
        }

        /**
         * See {@link #getLinkedItemComponentName()}.
         *
         * <p>The default value is {@code null}.
         */
        @NonNull
        public RouteListingPreference.Builder setLinkedItemComponentName(
                @Nullable ComponentName linkedItemComponentName) {
            mLinkedItemComponentName = linkedItemComponentName;
            return this;
        }

        /**
         * Creates and returns a new {@link RouteListingPreference} instance with the given
         * parameters.
         */
        @NonNull
        public RouteListingPreference build() {
            return new RouteListingPreference(this);
        }
    }

    /** Holds preference information for a specific route in a {@link RouteListingPreference}. */
    public static final class Item {

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    SELECTION_BEHAVIOR_NONE,
                    SELECTION_BEHAVIOR_TRANSFER,
                    SELECTION_BEHAVIOR_GO_TO_APP
                })
        public @interface SelectionBehavior {}

        /** The corresponding route is not selectable by the user. */
        public static final int SELECTION_BEHAVIOR_NONE = 0;
        /** If the user selects the corresponding route, the media transfers to the said route. */
        public static final int SELECTION_BEHAVIOR_TRANSFER = 1;
        /**
         * If the user selects the corresponding route, the system takes the user to the
         * application.
         *
         * <p>The system uses {@link #getLinkedItemComponentName()} in order to navigate to the app.
         */
        public static final int SELECTION_BEHAVIOR_GO_TO_APP = 2;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                flag = true,
                value = {FLAG_ONGOING_SESSION, FLAG_ONGOING_SESSION_MANAGED, FLAG_SUGGESTED})
        public @interface Flags {}

        /**
         * The corresponding route is already hosting a session with the app that owns this listing
         * preference.
         */
        public static final int FLAG_ONGOING_SESSION = 1;

        /**
         * Signals that the ongoing session on the corresponding route is managed by the current
         * user of the app.
         *
         * <p>The system can use this flag to provide visual indication that the route is not only
         * hosting a session, but also that the user has ownership over said session.
         *
         * <p>This flag is ignored if {@link #FLAG_ONGOING_SESSION} is not set, or if the
         * corresponding route is not currently selected.
         *
         * <p>This flag does not affect volume adjustment (see {@link
         * androidx.media.VolumeProviderCompat}, and {@link
         * MediaRouteDescriptor#getVolumeHandling()}), or any aspect other than the visual
         * representation of the corresponding item.
         */
        public static final int FLAG_ONGOING_SESSION_MANAGED = 1 << 1;

        /**
         * The corresponding route is specially likely to be selected by the user.
         *
         * <p>A UI reflecting this preference may reserve a specific space for suggested routes,
         * making it more accessible to the user. If the number of suggested routes exceeds the
         * number supported by the UI, the routes listed first in {@link
         * RouteListingPreference#getItems()} will take priority.
         */
        public static final int FLAG_SUGGESTED = 1 << 2;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(
                value = {
                    SUBTEXT_NONE,
                    SUBTEXT_ERROR_UNKNOWN,
                    SUBTEXT_SUBSCRIPTION_REQUIRED,
                    SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED,
                    SUBTEXT_AD_ROUTING_DISALLOWED,
                    SUBTEXT_DEVICE_LOW_POWER,
                    SUBTEXT_UNAUTHORIZED,
                    SUBTEXT_TRACK_UNSUPPORTED,
                    SUBTEXT_CUSTOM
                })
        public @interface SubText {}

        /** The corresponding route has no associated subtext. */
        public static final int SUBTEXT_NONE =
                android.media.RouteListingPreference.Item.SUBTEXT_NONE;
        /**
         * The corresponding route's subtext must indicate that it is not available because of an
         * unknown error.
         */
        public static final int SUBTEXT_ERROR_UNKNOWN =
                android.media.RouteListingPreference.Item.SUBTEXT_ERROR_UNKNOWN;
        /**
         * The corresponding route's subtext must indicate that it requires a special subscription
         * in order to be available for routing.
         */
        public static final int SUBTEXT_SUBSCRIPTION_REQUIRED =
                android.media.RouteListingPreference.Item.SUBTEXT_SUBSCRIPTION_REQUIRED;
        /**
         * The corresponding route's subtext must indicate that downloaded content cannot be routed
         * to it.
         */
        public static final int SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED =
                android.media.RouteListingPreference.Item
                        .SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED;
        /**
         * The corresponding route's subtext must indicate that it is not available because an ad is
         * in progress.
         */
        public static final int SUBTEXT_AD_ROUTING_DISALLOWED =
                android.media.RouteListingPreference.Item.SUBTEXT_AD_ROUTING_DISALLOWED;
        /**
         * The corresponding route's subtext must indicate that it is not available because the
         * device is in low-power mode.
         */
        public static final int SUBTEXT_DEVICE_LOW_POWER =
                android.media.RouteListingPreference.Item.SUBTEXT_DEVICE_LOW_POWER;
        /**
         * The corresponding route's subtext must indicate that it is not available because the user
         * is not authorized to route to it.
         */
        public static final int SUBTEXT_UNAUTHORIZED =
                android.media.RouteListingPreference.Item.SUBTEXT_UNAUTHORIZED;
        /**
         * The corresponding route's subtext must indicate that it is not available because the
         * device does not support the current media track.
         */
        public static final int SUBTEXT_TRACK_UNSUPPORTED =
                android.media.RouteListingPreference.Item.SUBTEXT_TRACK_UNSUPPORTED;
        /**
         * The corresponding route's subtext must be obtained from {@link
         * #getCustomSubtextMessage()}.
         *
         * <p>Applications should strongly prefer one of the other disable reasons (for the full
         * list, see {@link #getSubText()}) in order to guarantee correct localization and rendering
         * across all form factors.
         */
        public static final int SUBTEXT_CUSTOM =
                android.media.RouteListingPreference.Item.SUBTEXT_CUSTOM;

        @NonNull private final String mRouteId;
        @SelectionBehavior private final int mSelectionBehavior;
        @Flags private final int mFlags;
        @SubText private final int mSubText;

        @Nullable private final CharSequence mCustomSubtextMessage;

        // Must be package private to avoid a synthetic accessor for the builder.
        /* package */ Item(@NonNull RouteListingPreference.Item.Builder builder) {
            mRouteId = builder.mRouteId;
            mSelectionBehavior = builder.mSelectionBehavior;
            mFlags = builder.mFlags;
            mSubText = builder.mSubText;
            mCustomSubtextMessage = builder.mCustomSubtextMessage;
            validateCustomMessageSubtext();
        }

        /**
         * Returns the id of the route that corresponds to this route listing preference item.
         *
         * @see MediaRouter.RouteInfo#getId()
         */
        @NonNull
        public String getRouteId() {
            return mRouteId;
        }

        /**
         * Returns the behavior that the corresponding route has if the user selects it.
         *
         * @see #SELECTION_BEHAVIOR_NONE
         * @see #SELECTION_BEHAVIOR_TRANSFER
         * @see #SELECTION_BEHAVIOR_GO_TO_APP
         */
        public int getSelectionBehavior() {
            return mSelectionBehavior;
        }

        /**
         * Returns the flags associated to the route that corresponds to this item.
         *
         * @see #FLAG_ONGOING_SESSION
         * @see #FLAG_ONGOING_SESSION_MANAGED
         * @see #FLAG_SUGGESTED
         */
        @Flags
        public int getFlags() {
            return mFlags;
        }

        /**
         * Returns the type of subtext associated to this route.
         *
         * <p>Subtext types other than {@link #SUBTEXT_NONE} and {@link #SUBTEXT_CUSTOM} must not
         * have {@link #SELECTION_BEHAVIOR_TRANSFER}.
         *
         * <p>If this method returns {@link #SUBTEXT_CUSTOM}, then the subtext is obtained form
         * {@link #getCustomSubtextMessage()}.
         *
         * @see #SUBTEXT_NONE
         * @see #SUBTEXT_ERROR_UNKNOWN
         * @see #SUBTEXT_SUBSCRIPTION_REQUIRED
         * @see #SUBTEXT_DOWNLOADED_CONTENT_ROUTING_DISALLOWED
         * @see #SUBTEXT_AD_ROUTING_DISALLOWED
         * @see #SUBTEXT_DEVICE_LOW_POWER
         * @see #SUBTEXT_UNAUTHORIZED
         * @see #SUBTEXT_TRACK_UNSUPPORTED
         * @see #SUBTEXT_CUSTOM
         */
        @SubText
        public int getSubText() {
            return mSubText;
        }

        /**
         * Returns a human-readable {@link CharSequence} providing the subtext for the corresponding
         * route.
         *
         * <p>This value is ignored if the {@link #getSubText() subtext} for this item is not {@link
         * #SUBTEXT_CUSTOM}..
         *
         * <p>Applications must provide a localized message that matches the system's locale. See
         * {@link Locale#getDefault()}.
         *
         * <p>Applications should avoid using custom messages (and instead use one of non-custom
         * subtexts listed in {@link #getSubText()} in order to guarantee correct visual
         * representation and localization on all form factors.
         */
        @Nullable
        public CharSequence getCustomSubtextMessage() {
            return mCustomSubtextMessage;
        }

        // Equals and hashCode.

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RouteListingPreference.Item)) {
                return false;
            }
            RouteListingPreference.Item item = (RouteListingPreference.Item) other;
            return mRouteId.equals(item.mRouteId)
                    && mSelectionBehavior == item.mSelectionBehavior
                    && mFlags == item.mFlags
                    && mSubText == item.mSubText
                    && TextUtils.equals(mCustomSubtextMessage, item.mCustomSubtextMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    mRouteId, mSelectionBehavior, mFlags, mSubText, mCustomSubtextMessage);
        }

        // Internal methods.

        private void validateCustomMessageSubtext() {
            Preconditions.checkArgument(
                    mSubText != SUBTEXT_CUSTOM || mCustomSubtextMessage != null,
                    "The custom subtext message cannot be null if subtext is SUBTEXT_CUSTOM.");
        }

        // Internal classes.

        /** Builder for {@link RouteListingPreference.Item}. */
        public static final class Builder {

            // The builder fields must be package private to avoid synthetic accessors.
            /* package */ final String mRouteId;
            /* package */ int mSelectionBehavior;
            /* package */ int mFlags;
            /* package */ int mSubText;
            /* package */ CharSequence mCustomSubtextMessage;

            /**
             * Constructor.
             *
             * @param routeId See {@link RouteListingPreference.Item#getRouteId()}.
             */
            public Builder(@NonNull String routeId) {
                Preconditions.checkArgument(!TextUtils.isEmpty(routeId));
                mRouteId = routeId;
                mSelectionBehavior = SELECTION_BEHAVIOR_TRANSFER;
                mSubText = SUBTEXT_NONE;
            }

            /**
             * See {@link RouteListingPreference.Item#getSelectionBehavior()}.
             *
             * <p>The default value is {@link #ACTION_TRANSFER_MEDIA}.
             */
            @NonNull
            public RouteListingPreference.Item.Builder setSelectionBehavior(int selectionBehavior) {
                mSelectionBehavior = selectionBehavior;
                return this;
            }

            /**
             * See {@link RouteListingPreference.Item#getFlags()}.
             *
             * <p>The default value is zero (no flags).
             */
            @NonNull
            public RouteListingPreference.Item.Builder setFlags(int flags) {
                mFlags = flags;
                return this;
            }

            /**
             * See {@link RouteListingPreference.Item#getSubText()}.
             *
             * <p>The default value is {@link #SUBTEXT_NONE}.
             */
            @NonNull
            public RouteListingPreference.Item.Builder setSubText(int subText) {
                mSubText = subText;
                return this;
            }

            /**
             * See {@link RouteListingPreference.Item#getCustomSubtextMessage()}.
             *
             * <p>The default value is {@code null}.
             */
            @NonNull
            public RouteListingPreference.Item.Builder setCustomSubtextMessage(
                    @Nullable CharSequence customSubtextMessage) {
                mCustomSubtextMessage = customSubtextMessage;
                return this;
            }

            /**
             * Creates and returns a new {@link RouteListingPreference.Item} with the given
             * parameters.
             */
            @NonNull
            public RouteListingPreference.Item build() {
                return new RouteListingPreference.Item(this);
            }
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        @NonNull
        public static android.media.RouteListingPreference toPlatformRouteListingPreference(
                RouteListingPreference routeListingPreference) {
            ArrayList<android.media.RouteListingPreference.Item> platformRlpItems =
                    new ArrayList<>();
            for (Item item : routeListingPreference.getItems()) {
                platformRlpItems.add(toPlatformItem(item));
            }

            return new android.media.RouteListingPreference.Builder()
                    .setItems(platformRlpItems)
                    .setLinkedItemComponentName(routeListingPreference.getLinkedItemComponentName())
                    .setUseSystemOrdering(routeListingPreference.isSystemOrderingEnabled())
                    .build();
        }

        @DoNotInline
        @NonNull
        public static android.media.RouteListingPreference.Item toPlatformItem(Item item) {
            return new android.media.RouteListingPreference.Item.Builder(item.getRouteId())
                    .setFlags(item.getFlags())
                    .setSubText(item.getSubText())
                    .setCustomSubtextMessage(item.getCustomSubtextMessage())
                    .setSelectionBehavior(item.getSelectionBehavior())
                    .build();
        }
    }
}
