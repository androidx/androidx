/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.media.RouteDiscoveryPreference;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Describes the properties of a route.
 * <p>
 * Each route is uniquely identified by an opaque id string.  This token
 * may take any form as long as it is unique within the media route provider.
 * </p><p>
 * This object is immutable once created using a {@link Builder} instance.
 * </p>
 */
public final class MediaRouteDescriptor {
    static final String KEY_ID = "id";
    static final String KEY_GROUP_MEMBER_IDS = "groupMemberIds";
    static final String KEY_NAME = "name";
    static final String KEY_DESCRIPTION = "status";
    static final String KEY_ICON_URI = "iconUri";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_IS_SYSTEM_ROUTE = "isSystemRoute";
    static final String IS_DYNAMIC_GROUP_ROUTE = "isDynamicGroupRoute";
    static final String KEY_CONNECTING = "connecting";
    static final String KEY_CONNECTION_STATE = "connectionState";
    static final String KEY_CONTROL_FILTERS = "controlFilters";
    static final String KEY_PLAYBACK_TYPE = "playbackType";
    static final String KEY_PLAYBACK_STREAM = "playbackStream";
    static final String KEY_DEVICE_TYPE = "deviceType";
    static final String KEY_VOLUME = "volume";
    static final String KEY_VOLUME_MAX = "volumeMax";
    static final String KEY_VOLUME_HANDLING = "volumeHandling";
    static final String KEY_PRESENTATION_DISPLAY_ID = "presentationDisplayId";
    static final String KEY_EXTRAS = "extras";
    static final String KEY_CAN_DISCONNECT = "canDisconnect";
    static final String KEY_SETTINGS_INTENT = "settingsIntent";
    static final String KEY_MIN_CLIENT_VERSION = "minClientVersion";
    static final String KEY_MAX_CLIENT_VERSION = "maxClientVersion";
    static final String KEY_DEDUPLICATION_IDS = "deduplicationIds";
    static final String KEY_IS_VISIBILITY_PUBLIC = "isVisibilityPublic";
    static final String KEY_ALLOWED_PACKAGES = "allowedPackages";

    final Bundle mBundle;

    MediaRouteDescriptor(Bundle bundle) {
        mBundle = bundle;
    }

    /**
     * Gets the unique id of the route.
     * <p>
     * The route id associated with a route descriptor functions as a stable
     * identifier for the route and must be unique among all routes offered
     * by the provider.
     * </p>
     */
    @NonNull
    public String getId() {
        return mBundle.getString(KEY_ID);
    }

    /**
     * Gets the group member ids of the route.
     * <p>
     * A route descriptor that has one or more group member route ids
     * represents a route group. A member route may belong to another group.
     * </p>
     */
    @RestrictTo(LIBRARY)
    @NonNull
    public List<String> getGroupMemberIds() {
        if (!mBundle.containsKey(KEY_GROUP_MEMBER_IDS)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mBundle.getStringArrayList(KEY_GROUP_MEMBER_IDS));
    }

    /**
     * Gets the user-visible name of the route.
     * <p>
     * The route name identifies the destination represented by the route.
     * It may be a user-supplied name, an alias, or device serial number.
     * </p>
     */
    @NonNull
    public String getName() {
        return mBundle.getString(KEY_NAME);
    }

    /**
     * Gets the user-visible description of the route.
     * <p>
     * The route description describes the kind of destination represented by the route.
     * It may be a user-supplied string, a model number or brand of device.
     * </p>
     */
    @Nullable
    public String getDescription() {
        return mBundle.getString(KEY_DESCRIPTION);
    }

    /**
     * Gets the URI of the icon representing this route.
     * <p>
     * This icon will be used in picker UIs if available.
     * </p>
     */
    @Nullable
    public Uri getIconUri() {
        String iconUri = mBundle.getString(KEY_ICON_URI);
        return iconUri == null ? null : Uri.parse(iconUri);
    }

    /**
     * Gets whether the route is enabled.
     */
    public boolean isEnabled() {
        return mBundle.getBoolean(KEY_ENABLED, true);
    }

    /**
     * Returns {@code true} if this route is a system route.
     *
     * <p>System routes are routes controlled by the system, like the device's built-in speakers,
     * wired headsets, and bluetooth devices.
     *
     * <p>To use system routes, your application should write media sample data to a media framework
     * API, typically via <a
     * href="https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer">Exoplayer</a>.
     */
    public boolean isSystemRoute() {
        return mBundle.getBoolean(KEY_IS_SYSTEM_ROUTE, false);
    }

    /**
     * Returns if this route is a dynamic group route.
     *
     * <p>{@link MediaRouteProvider} creates a dynamic group route when {@link
     * MediaRouteProvider#onCreateDynamicGroupRouteController(String, Bundle)} is called. It happens
     * when a single route or a single static group is selected.
     *
     * <p>If a single device or a static group is selected, the associated dynamic group route
     * should not be seen by any client app because there is already one for the device. After user
     * added more devices into the session, it should be seen by the client app. The provider can
     * treat this by not setting the media intent for the dynamic group route if it contains only
     * one member.
     *
     * @return {@code true} if this route is a dynamic group route.
     */
    public boolean isDynamicGroupRoute() {
        return mBundle.getBoolean(IS_DYNAMIC_GROUP_ROUTE, false);
    }

    /**
     * Gets whether the route is connecting.
     * @deprecated Use {@link #getConnectionState} instead
     */
    @Deprecated
    public boolean isConnecting() {
        return mBundle.getBoolean(KEY_CONNECTING, false);
    }

    /**
     * Gets the connection state of the route.
     *
     * @return The connection state of this route:
     * {@link MediaRouter.RouteInfo#CONNECTION_STATE_DISCONNECTED},
     * {@link MediaRouter.RouteInfo#CONNECTION_STATE_CONNECTING}, or
     * {@link MediaRouter.RouteInfo#CONNECTION_STATE_CONNECTED}.
     */
    public int getConnectionState() {
        return mBundle.getInt(KEY_CONNECTION_STATE,
                MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED);
    }

    /**
     * Gets whether the route can be disconnected without stopping playback.
     * <p>
     * The route can normally be disconnected without stopping playback when
     * the destination device on the route is connected to two or more source
     * devices. The route provider should update the route immediately when the
     * number of connected devices changes.
     * </p><p>
     * To specify that the route should disconnect without stopping use
     * {@link MediaRouter#unselect(int)} with
     * {@link MediaRouter#UNSELECT_REASON_DISCONNECTED}.
     * </p>
     */
    public boolean canDisconnectAndKeepPlaying() {
        return mBundle.getBoolean(KEY_CAN_DISCONNECT, false);
    }

    /**
     * Gets an {@link IntentSender} for starting a settings activity for this
     * route. The activity may have specific route settings or general settings
     * for the connected device or route provider.
     *
     * @return An {@link IntentSender} to start a settings activity.
     */
    @Nullable
    public IntentSender getSettingsActivity() {
        return mBundle.getParcelable(KEY_SETTINGS_INTENT);
    }

    /**
     * Gets the route's {@link MediaControlIntent media control intent} filters.
     */
    @NonNull
    public List<IntentFilter> getControlFilters() {
        if (!mBundle.containsKey(KEY_CONTROL_FILTERS)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mBundle.getParcelableArrayList(KEY_CONTROL_FILTERS));
    }

    /**
     * Gets the type of playback associated with this route.
     *
     * @return The type of playback associated with this route:
     * {@link MediaRouter.RouteInfo#PLAYBACK_TYPE_LOCAL} or
     * {@link MediaRouter.RouteInfo#PLAYBACK_TYPE_REMOTE}.
     */
    public int getPlaybackType() {
        return mBundle.getInt(KEY_PLAYBACK_TYPE, MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);
    }

    /**
     * Gets the route's playback stream.
     */
    public int getPlaybackStream() {
        return mBundle.getInt(KEY_PLAYBACK_STREAM, -1);
    }

    /**
     * Gets the type of the receiver device associated with this route.
     *
     * @return The type of the receiver device associated with this route.
     */
    @MediaRouter.RouteInfo.DeviceType
    public int getDeviceType() {
        return mBundle.getInt(KEY_DEVICE_TYPE);
    }

    /**
     * Gets the route's current volume, or 0 if unknown.
     */
    public int getVolume() {
        return mBundle.getInt(KEY_VOLUME);
    }

    /**
     * Gets the route's maximum volume, or 0 if unknown.
     */
    public int getVolumeMax() {
        return mBundle.getInt(KEY_VOLUME_MAX);
    }

    /**
     * Gets information about how volume is handled on the route.
     *
     * @return How volume is handled on the route:
     * {@link MediaRouter.RouteInfo#PLAYBACK_VOLUME_FIXED} or
     * {@link MediaRouter.RouteInfo#PLAYBACK_VOLUME_VARIABLE}.
     */
    public int getVolumeHandling() {
        return mBundle.getInt(KEY_VOLUME_HANDLING,
                MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED);
    }

    /**
     * Gets the route's deduplication ids.
     *
     * <p>Two routes are considered to come from the same receiver device if any of their respective
     * deduplication ids match.
     */
    @NonNull
    public Set<String> getDeduplicationIds() {
        ArrayList<String> deduplicationIds = mBundle.getStringArrayList(KEY_DEDUPLICATION_IDS);
        return deduplicationIds != null
                ? Collections.unmodifiableSet(new HashSet<>(deduplicationIds))
                : Collections.emptySet();
    }

    /**
     * Gets the route's presentation display id, or -1 if none.
     */
    public int getPresentationDisplayId() {
        return mBundle.getInt(
                KEY_PRESENTATION_DISPLAY_ID, MediaRouter.RouteInfo.PRESENTATION_DISPLAY_ID_NONE);
    }

    /**
     * Gets a bundle of extras for this route descriptor.
     * The extras will be ignored by the media router but they may be used
     * by applications.
     */
    @Nullable
    public Bundle getExtras() {
        return mBundle.getBundle(KEY_EXTRAS);
    }

    /**
     * Gets the minimum client version required for this route.
     */
    @RestrictTo(LIBRARY)
    public int getMinClientVersion() {
        return mBundle.getInt(KEY_MIN_CLIENT_VERSION,
                MediaRouteProviderProtocol.CLIENT_VERSION_START);
    }

    /**
     * Gets the maximum client version required for this route.
     */
    @RestrictTo(LIBRARY)
    public int getMaxClientVersion() {
        return mBundle.getInt(KEY_MAX_CLIENT_VERSION, Integer.MAX_VALUE);
    }

    /**
     * Gets whether the route visibility is public or not.
     */
    public boolean isVisibilityPublic() {
        return mBundle.getBoolean(KEY_IS_VISIBILITY_PUBLIC, /* defaultValue= */ true);
    }

    /**
     * Gets the set of allowed packages which are able to see the route or an empty set if only
     * the route provider's package is allowed to see this route. This applies only when
     * {@link #isVisibilityPublic} returns {@code false}.
     */
    @NonNull
    public Set<String> getAllowedPackages() {
        if (!mBundle.containsKey(KEY_ALLOWED_PACKAGES)) {
            return new HashSet<>();
        }
        return new HashSet<>(mBundle.getStringArrayList(KEY_ALLOWED_PACKAGES));
    }

    /**
     * Returns true if the route descriptor has all of the required fields.
     */
    public boolean isValid() {
        if (TextUtils.isEmpty(getId())
                || TextUtils.isEmpty(getName())
                || getControlFilters().contains(null)) {
            return false;
        }
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return "MediaRouteDescriptor{ "
                + "id=" + getId()
                + ", groupMemberIds=" + getGroupMemberIds()
                + ", name=" + getName()
                + ", description=" + getDescription()
                + ", iconUri=" + getIconUri()
                + ", isEnabled=" + isEnabled()
                + ", isSystemRoute=" + isSystemRoute()
                + ", connectionState=" + getConnectionState()
                + ", controlFilters=" + Arrays.toString(getControlFilters().toArray())
                + ", playbackType=" + getPlaybackType()
                + ", playbackStream=" + getPlaybackStream()
                + ", deviceType=" + getDeviceType()
                + ", volume=" + getVolume()
                + ", volumeMax=" + getVolumeMax()
                + ", volumeHandling=" + getVolumeHandling()
                + ", presentationDisplayId=" + getPresentationDisplayId()
                + ", extras=" + getExtras()
                + ", isValid=" + isValid()
                + ", minClientVersion=" + getMinClientVersion()
                + ", maxClientVersion=" + getMaxClientVersion()
                + ", isVisibilityPublic=" + isVisibilityPublic()
                + ", allowedPackages=" + Arrays.toString(getAllowedPackages().toArray())
                + " }";
    }

    /**
     * Converts this object to a bundle for serialization.
     *
     * @return The contents of the object represented as a bundle.
     */
    @NonNull
    public Bundle asBundle() {
        return mBundle;
    }

    /**
     * Creates an instance from a bundle.
     *
     * @param bundle The bundle, or null if none.
     * @return The new instance, or null if the bundle was null.
     */
    @Nullable
    public static MediaRouteDescriptor fromBundle(@Nullable Bundle bundle) {
        return bundle != null ? new MediaRouteDescriptor(bundle) : null;
    }

    /**
     * Builder for {@link MediaRouteDescriptor media route descriptors}.
     */
    public static final class Builder {
        private final Bundle mBundle;

        private List<String> mGroupMemberIds = new ArrayList<>();
        private List<IntentFilter> mControlFilters = new ArrayList<>();
        private Set<String> mAllowedPackages = new HashSet<>();

        /**
         * Creates a media route descriptor builder.
         *
         * @param id The unique id of the route.
         * @param name The user-visible name of the route.
         */
        public Builder(@NonNull String id, @NonNull String name) {
            mBundle = new Bundle();
            setId(id);
            setName(name);
        }

        /**
         * Creates a media route descriptor builder whose initial contents are
         * copied from an existing descriptor.
         */
        public Builder(@NonNull MediaRouteDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("descriptor must not be null");
            }

            mBundle = new Bundle(descriptor.mBundle);

            mGroupMemberIds = descriptor.getGroupMemberIds();
            mControlFilters = descriptor.getControlFilters();
            mAllowedPackages = descriptor.getAllowedPackages();
        }

        /**
         * Sets the unique id of the route.
         * <p>
         * The route id associated with a route descriptor functions as a stable
         * identifier for the route and must be unique among all routes offered
         * by the provider.
         * </p>
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            if (id == null) {
                throw new NullPointerException("id must not be null");
            }
            mBundle.putString(KEY_ID, id);
            return this;
        }

        /**
         * Clears the group member IDs of the route.
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder clearGroupMemberIds() {
            mGroupMemberIds.clear();
            return this;
        }

        /**
         * Adds a group member id of the route.
         * <p>
         * A route descriptor that has one or more group member route ids
         * represents a route group. A member route may belong to another group.
         * </p>
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder addGroupMemberId(@NonNull String groupMemberId) {
            if (TextUtils.isEmpty(groupMemberId)) {
                throw new IllegalArgumentException("groupMemberId must not be empty");
            }

            if (!mGroupMemberIds.contains(groupMemberId)) {
                mGroupMemberIds.add(groupMemberId);
            }
            return this;
        }

        /**
         * Adds a list of group member ids of the route.
         * <p>
         * A route descriptor that has one or more group member route ids
         * represents a route group. A member route may belong to another group.
         * </p>
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder addGroupMemberIds(@NonNull Collection<String> groupMemberIds) {
            if (groupMemberIds == null) {
                throw new IllegalArgumentException("groupMemberIds must not be null");
            }

            if (!groupMemberIds.isEmpty()) {
                for (String groupMemberId : groupMemberIds) {
                    addGroupMemberId(groupMemberId);
                }
            }
            return this;
        }

        /**
         * Removes a group member id from the route's member list.
         * <p>
         * A route descriptor that has one or more group member route ids
         * represents a route group. A member route may belong to another group.
         * </p>
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder removeGroupMemberId(@NonNull String memberRouteId) {
            if (TextUtils.isEmpty(memberRouteId)) {
                throw new IllegalArgumentException("memberRouteId must not be empty");
            }
            mGroupMemberIds.remove(memberRouteId);
            return this;
        }

        /**
         * Sets the user-visible name of the route.
         * <p>
         * The route name identifies the destination represented by the route.
         * It may be a user-supplied name, an alias, or device serial number.
         * </p>
         */
        @NonNull
        public Builder setName(@NonNull String name) {
            if (name == null) {
                throw new NullPointerException("name must not be null");
            }
            mBundle.putString(KEY_NAME, name);
            return this;
        }

        /**
         * Sets the user-visible description of the route.
         * <p>
         * The route description describes the kind of destination represented by the route.
         * It may be a user-supplied string, a model number or brand of device.
         * </p>
         */
        @NonNull
        public Builder setDescription(@Nullable String description) {
            mBundle.putString(KEY_DESCRIPTION, description);
            return this;
        }

        /**
         * Sets the URI of the icon representing this route.
         * <p>
         * This icon will be used in picker UIs if available.
         * </p><p>
         * The URI must be one of the following formats:
         * <ul>
         * <li>content ({@link android.content.ContentResolver#SCHEME_CONTENT})</li>
         * <li>android.resource ({@link android.content.ContentResolver#SCHEME_ANDROID_RESOURCE})
         * </li>
         * <li>file ({@link android.content.ContentResolver#SCHEME_FILE})</li>
         * </ul>
         * </p>
         */
        @NonNull
        public Builder setIconUri(@NonNull Uri iconUri) {
            if (iconUri == null) {
                throw new IllegalArgumentException("iconUri must not be null");
            }
            mBundle.putString(KEY_ICON_URI, iconUri.toString());
            return this;
        }

        /**
         * Sets whether the route is enabled.
         * <p>
         * Disabled routes represent routes that a route provider knows about, such as paired
         * Wifi Display receivers, but that are not currently available for use.
         * </p>
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            mBundle.putBoolean(KEY_ENABLED, enabled);
            return this;
        }

        /**
         * Sets whether the route is a system route.
         *
         * @see MediaRouteDescriptor#isSystemRoute()
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder setIsSystemRoute(boolean isSystemRoute) {
            mBundle.putBoolean(KEY_IS_SYSTEM_ROUTE, isSystemRoute);
            return this;
        }

        /**
         * Sets whether the route is a dynamic group route.
         * @see #isDynamicGroupRoute()
         */
        @NonNull
        public Builder setIsDynamicGroupRoute(boolean isDynamicGroupRoute) {
            mBundle.putBoolean(IS_DYNAMIC_GROUP_ROUTE, isDynamicGroupRoute);
            return this;
        }

        /**
         * Sets whether the route is in the process of connecting and is not yet
         * ready for use.
         * @deprecated Use {@link #setConnectionState} instead.
         */
        @Deprecated
        @NonNull
        public Builder setConnecting(boolean connecting) {
            mBundle.putBoolean(KEY_CONNECTING, connecting);
            return this;
        }

        /**
         * Sets the route's connection state.
         *
         * @param connectionState The connection state of the route:
         * {@link MediaRouter.RouteInfo#CONNECTION_STATE_DISCONNECTED},
         * {@link MediaRouter.RouteInfo#CONNECTION_STATE_CONNECTING}, or
         * {@link MediaRouter.RouteInfo#CONNECTION_STATE_CONNECTED}.
         */
        @NonNull
        public Builder setConnectionState(int connectionState) {
            mBundle.putInt(KEY_CONNECTION_STATE, connectionState);
            return this;
        }

        /**
         * Sets whether the route can be disconnected without stopping playback.
         */
        @NonNull
        public Builder setCanDisconnect(boolean canDisconnect) {
            mBundle.putBoolean(KEY_CAN_DISCONNECT, canDisconnect);
            return this;
        }

        /**
         * Sets an intent sender for launching the settings activity for this
         * route.
         */
        @NonNull
        public Builder setSettingsActivity(@Nullable IntentSender is) {
            mBundle.putParcelable(KEY_SETTINGS_INTENT, is);
            return this;
        }

        /**
         * Clears {@link MediaControlIntent media control intent} filters for the route.
         */
        @NonNull
        public Builder clearControlFilters() {
            mControlFilters.clear();
            return this;
        }

        /**
         * Adds a {@link MediaControlIntent media control intent} filter for the route.
         */
        @NonNull
        public Builder addControlFilter(@NonNull IntentFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must not be null");
            }

            if (!mControlFilters.contains(filter)) {
                mControlFilters.add(filter);
            }
            return this;
        }

        /**
         * Adds a list of {@link MediaControlIntent media control intent} filters for the route.
         */
        @NonNull
        public Builder addControlFilters(@NonNull Collection<IntentFilter> filters) {
            if (filters == null) {
                throw new IllegalArgumentException("filters must not be null");
            }

            if (!filters.isEmpty()) {
                for (IntentFilter filter : filters) {
                    if (filter != null) {
                        addControlFilter(filter);
                    }
                }
            }
            return this;
        }

        /**
         * Sets the route's playback type.
         *
         * @param playbackType The playback type of the route:
         * {@link MediaRouter.RouteInfo#PLAYBACK_TYPE_LOCAL} or
         * {@link MediaRouter.RouteInfo#PLAYBACK_TYPE_REMOTE}.
         */
        @NonNull
        public Builder setPlaybackType(int playbackType) {
            mBundle.putInt(KEY_PLAYBACK_TYPE, playbackType);
            return this;
        }

        /**
         * Sets the route's playback stream.
         */
        @NonNull
        public Builder setPlaybackStream(int playbackStream) {
            mBundle.putInt(KEY_PLAYBACK_STREAM, playbackStream);
            return this;
        }

        /**
         * Sets the route's receiver device type.
         *
         * @param deviceType The type of the receiver device.
         */
        @NonNull
        public Builder setDeviceType(@MediaRouter.RouteInfo.DeviceType int deviceType) {
            mBundle.putInt(KEY_DEVICE_TYPE, deviceType);
            return this;
        }

        /**
         * Sets the route's current volume, or 0 if unknown.
         */
        @NonNull
        public Builder setVolume(int volume) {
            mBundle.putInt(KEY_VOLUME, volume);
            return this;
        }

        /**
         * Sets the route's maximum volume, or 0 if unknown.
         */
        @NonNull
        public Builder setVolumeMax(int volumeMax) {
            mBundle.putInt(KEY_VOLUME_MAX, volumeMax);
            return this;
        }

        /**
         * Sets the route's volume handling.
         *
         * @param volumeHandling how volume is handled on the route:
         * {@link MediaRouter.RouteInfo#PLAYBACK_VOLUME_FIXED} or
         * {@link MediaRouter.RouteInfo#PLAYBACK_VOLUME_VARIABLE}.
         */
        @NonNull
        public Builder setVolumeHandling(int volumeHandling) {
            mBundle.putInt(KEY_VOLUME_HANDLING, volumeHandling);
            return this;
        }

        /**
         * Sets the route's deduplication ids.
         *
         * <p>Two routes are considered to come from the same receiver device if any of their
         * respective deduplication ids match.
         *
         * @param deduplicationIds A set of strings that uniquely identify the receiver device that
         *     backs this route.
         */
        @NonNull
        public Builder setDeduplicationIds(@NonNull Set<String> deduplicationIds) {
            mBundle.putStringArrayList(KEY_DEDUPLICATION_IDS, new ArrayList<>(deduplicationIds));
            return this;
        }

        /**
         * Sets the route's presentation display id, or -1 if none.
         */
        @NonNull
        public Builder setPresentationDisplayId(int presentationDisplayId) {
            mBundle.putInt(KEY_PRESENTATION_DISPLAY_ID, presentationDisplayId);
            return this;
        }

        /**
         * Sets a bundle of extras for this route descriptor.
         * The extras will be ignored by the media router but they may be used
         * by applications.
         */
        @NonNull
        public Builder setExtras(@Nullable Bundle extras) {
            if (extras == null) {
                mBundle.putBundle(KEY_EXTRAS, null);
            } else {
                mBundle.putBundle(KEY_EXTRAS, new Bundle(extras));
            }
            return this;
        }

        /**
         * Sets the route's minimum client version.
         * A router whose version is lower than this will not be able to connect to this route.
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder setMinClientVersion(int minVersion) {
            mBundle.putInt(KEY_MIN_CLIENT_VERSION, minVersion);
            return this;
        }

        /**
         * Sets the route's maximum client version.
         * A router whose version is higher than this will not be able to connect to this route.
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public Builder setMaxClientVersion(int maxVersion) {
            mBundle.putInt(KEY_MAX_CLIENT_VERSION, maxVersion);
            return this;
        }

        /**
         * Sets the visibility of this route to public.
         *
         * <p>By default, unless you call {@link #setVisibilityRestricted}, the new route will be
         * public.
         *
         * <p>Public routes are visible to any application with a matching {@link
         * RouteDiscoveryPreference#getPreferredFeatures feature}.
         *
         * <p>Calls to this method override previous calls to {@link #setVisibilityPublic} and
         * {@link #setVisibilityRestricted}.
         */
        @NonNull
        @SuppressLint({"MissingGetterMatchingBuilder"})
        public Builder setVisibilityPublic() {
            mBundle.putBoolean(KEY_IS_VISIBILITY_PUBLIC, true);
            mAllowedPackages.clear();
            return this;
        }

        /**
         * Sets the visibility of this route to restricted.
         *
         * <p>Routes with restricted visibility are only visible to its publisher application and
         * applications whose package name is included in the provided {@code allowedPackages} set
         * with a matching {@link RouteDiscoveryPreference#getPreferredFeatures feature}.
         *
         * <p>Calls to this method override previous calls to {@link #setVisibilityPublic} and
         * {@link #setVisibilityRestricted}.
         *
         * @see #setVisibilityPublic
         * @param allowedPackages set of package names which are allowed to see this route.
         */
        @NonNull
        @SuppressLint({"MissingGetterMatchingBuilder"})
        public Builder setVisibilityRestricted(@NonNull Set<String> allowedPackages) {
            mBundle.putBoolean(KEY_IS_VISIBILITY_PUBLIC, false);
            mAllowedPackages = new HashSet<>(allowedPackages);
            return this;
        }

        /**
         * Builds the {@link MediaRouteDescriptor media route descriptor}.
         */
        @NonNull
        public MediaRouteDescriptor build() {
            mBundle.putParcelableArrayList(KEY_CONTROL_FILTERS, new ArrayList<>(mControlFilters));
            mBundle.putStringArrayList(KEY_GROUP_MEMBER_IDS, new ArrayList<>(mGroupMemberIds));
            mBundle.putStringArrayList(KEY_ALLOWED_PACKAGES, new ArrayList<>(mAllowedPackages));
            return new MediaRouteDescriptor(mBundle);
        }
    }
}
