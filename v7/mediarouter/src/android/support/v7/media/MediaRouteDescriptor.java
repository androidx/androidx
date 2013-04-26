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

import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ICON_RESOURCE = "iconResource";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_CONNECTING = "connecting";
    private static final String KEY_CONTROL_FILTERS = "controlFilters";
    private static final String KEY_PLAYBACK_TYPE = "playbackType";
    private static final String KEY_PLAYBACK_STREAM = "playbackStream";
    private static final String KEY_VOLUME = "volume";
    private static final String KEY_VOLUME_MAX = "volumeMax";
    private static final String KEY_VOLUME_HANDLING = "volumeHandling";
    private static final String KEY_PRESENTATION_DISPLAY_ID = "presentationDisplayId";
    private static final String KEY_EXTRAS = "extras";

    private final Bundle mBundle;
    private final Drawable mIconDrawable;
    private List<IntentFilter> mControlFilters;

    private MediaRouteDescriptor(Bundle bundle, Drawable iconDrawable,
            List<IntentFilter> controlFilters) {
        mBundle = bundle;
        mIconDrawable = iconDrawable;
        mControlFilters = controlFilters;
    }

    /**
     * Gets the unique id of the route.
     */
    public String getId() {
        return mBundle.getString(KEY_ID);
    }

    /**
     * Gets the user-friendly name of the route.
     */
    public String getName() {
        return mBundle.getString(KEY_NAME);
    }

    /**
     * Gets the user-friendly status of the route.
     */
    public String getStatus() {
        return mBundle.getString(KEY_STATUS);
    }

    /**
     * Gets a drawable to display as the route's icon.
     * <p>
     * Because drawables cannot be transferred to other processes, the icon resource
     * is usually passed in {@link #getIconResource} instead.
     * </p>
     */
    public Drawable getIconDrawable() {
        return mIconDrawable;
    }

    /**
     * Gets the id of a drawable resource to display as the route's icon.
     * <p>
     * The specified drawable resource id will be loaded from the media route
     * provider's package.
     * </p>
     */
    public int getIconResource() {
        return mBundle.getInt(KEY_ICON_RESOURCE);
    }

    /**
     * Gets whether the route is enabled.
     */
    public boolean isEnabled() {
        return mBundle.getBoolean(KEY_ENABLED, true);
    }

    /**
     * Gets whether the route is connecting.
     */
    public boolean isConnecting() {
        return mBundle.getBoolean(KEY_CONNECTING, false);
    }

    /**
     * Gets the route's {@link MediaControlIntent media control intent} filters.
     */
    public List<IntentFilter> getControlFilters() {
        ensureControlFilters();
        return mControlFilters;
    }

    private void ensureControlFilters() {
        if (mControlFilters == null) {
            mControlFilters = mBundle.<IntentFilter>getParcelableArrayList(KEY_CONTROL_FILTERS);
            if (mControlFilters == null) {
                mControlFilters = Collections.<IntentFilter>emptyList();
            }
        }
    }

    /**
     * Gets the route's playback type.
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
     * Gets the route's volume handling.
     */
    public int getVolumeHandling() {
        return mBundle.getInt(KEY_VOLUME_HANDLING,
                MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED);
    }

    /**
     * Gets the route's presentation display id, or -1 if none.
     */
    public int getPresentationDisplayId() {
        return mBundle.getInt(KEY_PRESENTATION_DISPLAY_ID, -1);
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
        result.append(", name=").append(getName());
        result.append(", status=").append(getStatus());
        result.append(", isEnabled=").append(isEnabled());
        result.append(", isConnecting=").append(isConnecting());
        result.append(", controlFilters=").append(Arrays.toString(getControlFilters().toArray()));
        result.append(", iconDrawable=").append(getIconDrawable());
        result.append(", iconResource=").append(getIconResource());
        result.append(", playbackType=").append(getPlaybackType());
        result.append(", playbackStream=").append(getPlaybackStream());
        result.append(", volume=").append(getVolume());
        result.append(", volumeMax=").append(getVolumeMax());
        result.append(", volumeHandling=").append(getVolumeHandling());
        result.append(", presentationDisplayId=").append(getPresentationDisplayId());
        result.append(", extras=").append(getExtras());
        result.append(", isValid=").append(isValid());
        result.append("}");
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
        return bundle != null ? new MediaRouteDescriptor(bundle, null, null) : null;
    }

    /**
     * Builder for {@link MediaRouteDescriptor media route descriptors}.
     */
    public static final class Builder {
        private final Bundle mBundle;
        private Drawable mIconDrawable;
        private ArrayList<IntentFilter> mControlFilters;

        /**
         * Creates a media route descriptor builder.
         *
         * @param id The unique id of the route.
         * @param name The user-friendly name of the route.
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
            mIconDrawable = descriptor.mIconDrawable;

            descriptor.ensureControlFilters();
            if (!descriptor.mControlFilters.isEmpty()) {
                mControlFilters = new ArrayList<IntentFilter>(descriptor.mControlFilters);
            }
        }

        /**
         * Sets the unique id of the route.
         */
        public Builder setId(String id) {
            mBundle.putString(KEY_ID, id);
            return this;
        }

        /**
         * Sets the user-friendly name of the route.
         */
        public Builder setName(String name) {
            mBundle.putString(KEY_NAME, name);
            return this;
        }

        /**
         * Sets the user-friendly status of the route.
         */
        public Builder setStatus(String status) {
            mBundle.putString(KEY_STATUS, status);
            return this;
        }

        /**
         * Sets a drawable to display as the route's icon.
         * <p>
         * Because drawables cannot be transferred to other processes, this method may
         * only be used by media route providers that reside in the same process
         * as the application.  When implementing a media route provider service, use
         * {@link #setIconResource} instead.
         * </p>
         */
        public Builder setIconDrawable(Drawable drawable) {
            mIconDrawable = drawable;
            return this;
        }

        /**
         * Sets the id of a drawable resource to display as the route's icon.
         * <p>
         * The specified drawable resource id will be loaded from the media route
         * provider's package.
         * </p>
         */
        public Builder setIconResource(int id) {
            mBundle.putInt(KEY_ICON_RESOURCE, id);
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
         */
        public Builder setConnecting(boolean connecting) {
            mBundle.putBoolean(KEY_CONNECTING, connecting);
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
        public Builder addControlFilters(List<IntentFilter> filters) {
            if (filters == null) {
                throw new IllegalArgumentException("filters must not be null");
            }

            final int count = filters.size();
            for (int i = 0; i < count; i++) {
                addControlFilter(filters.get(i));
            }
            return this;
        }

        /**
         * Sets the route's playback type.
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
         * Builds the {@link MediaRouteDescriptor media route descriptor}.
         */
        public MediaRouteDescriptor build() {
            if (mControlFilters != null) {
                mBundle.putParcelableArrayList(KEY_CONTROL_FILTERS, mControlFilters);
            }
            return new MediaRouteDescriptor(mBundle, mIconDrawable, mControlFilters);
        }
    }
}