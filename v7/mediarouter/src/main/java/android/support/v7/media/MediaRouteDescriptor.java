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
package android.support.v7.media;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    final Bundle mBundle;
    List<IntentFilter> mControlFilters;

    MediaRouteDescriptor(Bundle bundle, List<IntentFilter> controlFilters) {
        mBundle = bundle;
        mControlFilters = controlFilters;
    }

    /**
     * Gets the unique id of the route.
     * <p>
     * The route id associated with a route descriptor functions as a stable
     * identifier for the route and must be unique among all routes offered
     * by the provider.
     * </p>
     */
    public String getId() {
        return mBundle.getString(KEY_ID);
    }

    /**
     * Gets the group member ids of the route.
     * <p>
     * A route descriptor that has one or more group member route ids
     * represents a route group. A member route may belong to another group.
     * </p>
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public List<String> getGroupMemberIds() {
        return mBundle.getStringArrayList(KEY_GROUP_MEMBER_IDS);
    }

    /**
     * Gets the user-visible name of the route.
     * <p>
     * The route name identifies the destination represented by the route.
     * It may be a user-supplied name, an alias, or device serial number.
     * </p>
     */
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
    public String getDescription() {
        return mBundle.getString(KEY_DESCRIPTION);
    }

    /**
     * Gets the URI of the icon representing this route.
     * <p>
     * This icon will be used in picker UIs if available.
     * </p>
     */
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
    public IntentSender getSettingsActivity() {
        return mBundle.getParcelable(KEY_SETTINGS_INTENT);
    }

    /**
     * Gets the route's {@link MediaControlIntent media control intent} filters.
     */
    public List<IntentFilter> getControlFilters() {
        ensureControlFilters();
        return mControlFilters;
    }

    void ensureControlFilters() {
        if (mControlFilters == null) {
            mControlFilters = mBundle.<IntentFilter>getParcelableArrayList(KEY_CONTROL_FILTERS);
            if (mControlFilters == null) {
                mControlFilters = Collections.<IntentFilter>emptyList();
            }
        }
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
     * @return The type of the receiver device associated with this route:
     * {@link MediaRouter.RouteInfo#DEVICE_TYPE_TV} or
     * {@link MediaRouter.RouteInfo#DEVICE_TYPE_SPEAKER}.
     */
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
    public Bundle getExtras() {
        return mBundle.getBundle(KEY_EXTRAS);
    }

    /**
     * Gets the minimum client version required for this route.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public int getMinClientVersion() {
        return mBundle.getInt(KEY_MIN_CLIENT_VERSION,
                MediaRouteProviderProtocol.CLIENT_VERSION_START);
    }

    /**
     * Gets the maximum client version required for this route.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public int getMaxClientVersion() {
        return mBundle.getInt(KEY_MAX_CLIENT_VERSION, Integer.MAX_VALUE);
    }

    /**
     * Returns true if the route descriptor has all of the required fields.
     */
    public boolean isValid() {
        ensureControlFilters();
        if (TextUtils.isEmpty(getId())
                || TextUtils.isEmpty(getName())
                || mControlFilters.contains(null)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("MediaRouteDescriptor{ ");
        result.append("id=").append(getId());
        result.append(", groupMemberIds=").append(getGroupMemberIds());
        result.append(", name=").append(getName());
        result.append(", description=").append(getDescription());
        result.append(", iconUri=").append(getIconUri());
        result.append(", isEnabled=").append(isEnabled());
        result.append(", isConnecting=").append(isConnecting());
        result.append(", connectionState=").append(getConnectionState());
        result.append(", controlFilters=").append(Arrays.toString(getControlFilters().toArray()));
        result.append(", playbackType=").append(getPlaybackType());
        result.append(", playbackStream=").append(getPlaybackStream());
        result.append(", deviceType=").append(getDeviceType());
        result.append(", volume=").append(getVolume());
        result.append(", volumeMax=").append(getVolumeMax());
        result.append(", volumeHandling=").append(getVolumeHandling());
        result.append(", presentationDisplayId=").append(getPresentationDisplayId());
        result.append(", extras=").append(getExtras());
        result.append(", isValid=").append(isValid());
        result.append(", minClientVersion=").append(getMinClientVersion());
        result.append(", maxClientVersion=").append(getMaxClientVersion());
        result.append(" }");
        return result.toString();
    }

    /**
     * Converts this object to a bundle for serialization.
     *
     * @return The contents of the object represented as a bundle.
     */
    public Bundle asBundle() {
        return mBundle;
    }

    /**
     * Creates an instance from a bundle.
     *
     * @param bundle The bundle, or null if none.
     * @return The new instance, or null if the bundle was null.
     */
    public static MediaRouteDescriptor fromBundle(Bundle bundle) {
        return bundle != null ? new MediaRouteDescriptor(bundle, null) : null;
    }

    /**
     * Builder for {@link MediaRouteDescriptor media route descriptors}.
     */
    public static final class Builder {
        private final Bundle mBundle;
        private ArrayList<String> mGroupMemberIds;
        private ArrayList<IntentFilter> mControlFilters;

        /**
         * Creates a media route descriptor builder.
         *
         * @param id The unique id of the route.
         * @param name The user-visible name of the route.
         */
        public Builder(String id, String name) {
            mBundle = new Bundle();
            setId(id);
            setName(name);
        }

        /**
         * Creates a media route descriptor builder whose initial contents are
         * copied from an existing descriptor.
         */
        public Builder(MediaRouteDescriptor descriptor) {
            if (descriptor == null) {
                throw new IllegalArgumentException("descriptor must not be null");
            }

            mBundle = new Bundle(descriptor.mBundle);

            descriptor.ensureControlFilters();
            if (!descriptor.mControlFilters.isEmpty()) {
                mControlFilters = new ArrayList<IntentFilter>(descriptor.mControlFilters);
            }
        }

        /**
         * Sets the unique id of the route.
         * <p>
         * The route id associated with a route descriptor functions as a stable
         * identifier for the route and must be unique among all routes offered
         * by the provider.
         * </p>
         */
        public Builder setId(String id) {
            mBundle.putString(KEY_ID, id);
            return this;
        }

        /**
         * Adds a group member id of the route.
         * <p>
         * A route descriptor that has one or more group member route ids
         * represents a route group. A member route may belong to another group.
         * </p>
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder addGroupMemberId(String groupMemberId) {
            if (TextUtils.isEmpty(groupMemberId)) {
                throw new IllegalArgumentException("groupMemberId must not be empty");
            }

            if (mGroupMemberIds == null) {
                mGroupMemberIds = new ArrayList<>();
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
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder addGroupMemberIds(Collection<String> groupMemberIds) {
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
         * Sets the user-visible name of the route.
         * <p>
         * The route name identifies the destination represented by the route.
         * It may be a user-supplied name, an alias, or device serial number.
         * </p>
         */
        public Builder setName(String name) {
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
        public Builder setDescription(String description) {
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
        public Builder setIconUri(Uri iconUri) {
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
        public Builder setEnabled(boolean enabled) {
            mBundle.putBoolean(KEY_ENABLED, enabled);
            return this;
        }

        /**
         * Sets whether the route is in the process of connecting and is not yet
         * ready for use.
         * @deprecated Use {@link #setConnectionState} instead.
         */
        @Deprecated
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
        public Builder setConnectionState(int connectionState) {
            mBundle.putInt(KEY_CONNECTION_STATE, connectionState);
            return this;
        }

        /**
         * Sets whether the route can be disconnected without stopping playback.
         */
        public Builder setCanDisconnect(boolean canDisconnect) {
            mBundle.putBoolean(KEY_CAN_DISCONNECT, canDisconnect);
            return this;
        }

        /**
         * Sets an intent sender for launching the settings activity for this
         * route.
         */
        public Builder setSettingsActivity(IntentSender is) {
            mBundle.putParcelable(KEY_SETTINGS_INTENT, is);
            return this;
        }

        /**
         * Adds a {@link MediaControlIntent media control intent} filter for the route.
         */
        public Builder addControlFilter(IntentFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must not be null");
            }

            if (mControlFilters == null) {
                mControlFilters = new ArrayList<IntentFilter>();
            }
            if (!mControlFilters.contains(filter)) {
                mControlFilters.add(filter);
            }
            return this;
        }

        /**
         * Adds a list of {@link MediaControlIntent media control intent} filters for the route.
         */
        public Builder addControlFilters(Collection<IntentFilter> filters) {
            if (filters == null) {
                throw new IllegalArgumentException("filters must not be null");
            }

            if (!filters.isEmpty()) {
                for (IntentFilter filter : filters) {
                    addControlFilter(filter);
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
        public Builder setPlaybackType(int playbackType) {
            mBundle.putInt(KEY_PLAYBACK_TYPE, playbackType);
            return this;
        }

        /**
         * Sets the route's playback stream.
         */
        public Builder setPlaybackStream(int playbackStream) {
            mBundle.putInt(KEY_PLAYBACK_STREAM, playbackStream);
            return this;
        }

        /**
         * Sets the route's receiver device type.
         *
         * @param deviceType The receive device type of the route:
         * {@link MediaRouter.RouteInfo#DEVICE_TYPE_TV} or
         * {@link MediaRouter.RouteInfo#DEVICE_TYPE_SPEAKER}.
         */
        public Builder setDeviceType(int deviceType) {
            mBundle.putInt(KEY_DEVICE_TYPE, deviceType);
            return this;
        }

        /**
         * Sets the route's current volume, or 0 if unknown.
         */
        public Builder setVolume(int volume) {
            mBundle.putInt(KEY_VOLUME, volume);
            return this;
        }

        /**
         * Sets the route's maximum volume, or 0 if unknown.
         */
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
        public Builder setVolumeHandling(int volumeHandling) {
            mBundle.putInt(KEY_VOLUME_HANDLING, volumeHandling);
            return this;
        }

        /**
         * Sets the route's presentation display id, or -1 if none.
         */
        public Builder setPresentationDisplayId(int presentationDisplayId) {
            mBundle.putInt(KEY_PRESENTATION_DISPLAY_ID, presentationDisplayId);
            return this;
        }

        /**
         * Sets a bundle of extras for this route descriptor.
         * The extras will be ignored by the media router but they may be used
         * by applications.
         */
        public Builder setExtras(Bundle extras) {
            mBundle.putBundle(KEY_EXTRAS, extras);
            return this;
        }

        /**
         * Sets the route's minimum client version.
         * A router whose version is lower than this will not be able to connect to this route.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setMinClientVersion(int minVersion) {
            mBundle.putInt(KEY_MIN_CLIENT_VERSION, minVersion);
            return this;
        }

        /**
         * Sets the route's maximum client version.
         * A router whose version is higher than this will not be able to connect to this route.
         * @hide
         */
        @RestrictTo(LIBRARY_GROUP)
        public Builder setMaxClientVersion(int maxVersion) {
            mBundle.putInt(KEY_MAX_CLIENT_VERSION, maxVersion);
            return this;
        }

        /**
         * Builds the {@link MediaRouteDescriptor media route descriptor}.
         */
        public MediaRouteDescriptor build() {
            if (mControlFilters != null) {
                mBundle.putParcelableArrayList(KEY_CONTROL_FILTERS, mControlFilters);
            }
            if (mGroupMemberIds != null) {
                mBundle.putStringArrayList(KEY_GROUP_MEMBER_IDS, mGroupMemberIds);
            }
            return new MediaRouteDescriptor(mBundle, mControlFilters);
        }
    }
}
