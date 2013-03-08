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

package android.support.v4.media;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.media.MediaRouter.ControlRequestCallback;
import android.text.TextUtils;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Media route providers are used to publish additional media routes for
 * use within an application.  Media route providers may also be declared
 * as a service to publish additional media routes to all applications
 * in the system.
 * <p>
 * Applications and services should extend this class to publish additional media routes
 * to the {@link MediaRouter}.  To make additional media routes available within
 * your application, call {@link MediaRouter#addProvider} to add your provider to
 * the media router.  To make additional media routes available to all applications
 * in the system, register a media route provider service in your manifest.
 * </p><p>
 * This object must only be accessed on the main thread.
 * </p>
 */
public abstract class MediaRouteProvider {
    private static final int MSG_DELIVER_DESCRIPTOR_CHANGED = 1;

    private final Context mContext;
    private final ProviderMetadata mMetadata;
    private final ProviderHandler mHandler = new ProviderHandler();
    private final CopyOnWriteArrayList<Callback> mCallbacks =
            new CopyOnWriteArrayList<Callback>();

    private ProviderDescriptor mDescriptor;
    private boolean mPendingDescriptorChange;

    /**
     * Creates a media route provider.
     *
     * @param context The context.
     */
    public MediaRouteProvider(Context context) {
        this(context, null);
    }

    MediaRouteProvider(Context context, ProviderMetadata metadata) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        mContext = context;
        if (metadata == null) {
            mMetadata = new ProviderMetadata(context.getPackageName());
        } else {
            mMetadata = metadata;
        }
    }

    /**
     * Gets the context of the media route provider.
     */
    public final Context getContext() {
        return mContext;
    }

    final ProviderMetadata getMetadata() {
        return mMetadata;
    }

    /**
     * Gets the provider's descriptor.
     * <p>
     * The descriptor describes the state of the media route provider and
     * the routes that it publishes.  Watch for changes to the descriptor
     * by registering a {@link Callback callback} with {@link #addCallback}.
     * </p>
     *
     * @return The media route provider descriptor, or null if none.  This object
     * and all of its contents should be treated as if it were immutable so that it is
     * safe for clients to cache it.
     */
    public final ProviderDescriptor getDescriptor() {
        return mDescriptor;
    }

    /**
     * Sets the provider's descriptor.
     * <p>
     * Asynchronously notifies all registered {@link Callback callbacks} about the change.
     * </p>
     *
     * @param descriptor The updated route provider descriptor, or null if none.
     * This object and all of its contents should be treated as if it were immutable
     * so that it is safe for clients to cache it.
     */
    public final void setDescriptor(ProviderDescriptor descriptor) {
        MediaRouter.checkCallingThread();

        if (mDescriptor != descriptor) {
            mDescriptor = descriptor;
            if (!mPendingDescriptorChange) {
                mPendingDescriptorChange = true;
                mHandler.sendEmptyMessage(MSG_DELIVER_DESCRIPTOR_CHANGED);
            }
        }
    }

    private void deliverDescriptorChanged() {
        mPendingDescriptorChange = false;

        if (!mCallbacks.isEmpty()) {
            final ProviderDescriptor currentDescriptor = mDescriptor;
            for (Callback callback : mCallbacks) {
                callback.onDescriptorChanged(this, currentDescriptor);
            }
        }
    }

    /**
     * Adds a callback to be invoked on the main thread when information about a route
     * provider and its routes changes.
     *
     * @param callback The callback to add.
     */
    public final void addCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * Removes a callback.
     *
     * @param callback The callback to remove.
     */
    public final void removeCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        mCallbacks.remove(callback);
    }

    /**
     * Called by the media router to obtain a route controller for a particular route.
     * <p>
     * The media router will invoke the {@link RouteController#release} method of the route
     * controller when it is no longer needed to allow it to free its resources.
     * </p>
     *
     * @param routeId The unique id of the route.
     * @return The route controller.  Returns null if there is no such route or if the route
     * cannot be controlled using the route controller interface.
     */
    public RouteController onCreateRouteController(String routeId) {
        return null;
    }

    /**
     * Describes immutable properties of the route provider itself.
     */
    static final class ProviderMetadata {
        private final String mPackageName;

        public ProviderMetadata(String packageName) {
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException("packageName must not be null or empty");
            }
            mPackageName = packageName;
        }

        public String getPackageName() {
            return mPackageName;
        }
    }

    /**
     * Describes the state of a media route provider and the routes that it publishes.
     */
    public static final class ProviderDescriptor {
        private static final String KEY_ROUTES = "routes";

        private final Bundle mBundle;
        private RouteDescriptor[] mRoutes;

        /**
         * Creates a route provider descriptor.
         */
        public ProviderDescriptor() {
            mBundle = new Bundle();
        }

        /**
         * Creates a copy of another route provider descriptor.
         */
        public ProviderDescriptor(ProviderDescriptor other) {
            mBundle = new Bundle(other.mBundle);
        }

        ProviderDescriptor(Bundle bundle) {
            mBundle = bundle;
        }

        /**
         * Gets the list of all routes that this provider has published.
         */
        public RouteDescriptor[] getRoutes() {
            if (mRoutes == null) {
                mRoutes = RouteDescriptor.fromParcelableArray(
                        mBundle.getParcelableArray(KEY_ROUTES));
            }
            return mRoutes;
        }

        /**
         * Sets the list of all routes that this provider has published.
         */
        public void setRoutes(RouteDescriptor[] routes) {
            if (routes == null) {
                throw new IllegalArgumentException("routes must not be null");
            }
            mRoutes = routes;
            mBundle.putParcelableArray(KEY_ROUTES, RouteDescriptor.toParcelableArray(routes));
        }

        /**
         * Returns true if the route provider descriptor and all of the routes that
         * it contains have all of the required fields.
         * <p>
         * This verification is deep.  If the provider descriptor is known to be
         * valid then it is not necessary to call {@link #isValid} on each of its routes.
         * </p>
         */
        public boolean isValid() {
            for (RouteDescriptor route : getRoutes()) {
                if (route == null || !route.isValid()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "RouteProviderDescriptor{" + mBundle.toString() + "}";
        }

        Bundle asBundle() {
            return mBundle;
        }

        static ProviderDescriptor fromBundle(Bundle bundle) {
            return bundle != null ? new ProviderDescriptor(bundle) : null;
        }
    }

    /**
     * Describes the properties of a route.
     * <p>
     * Each route is uniquely identified by an opaque id string.  This token
     * may take any form as long as it is unique within the media route provider.
     * </p>
     */
    public static final class RouteDescriptor {
        static final RouteDescriptor[] EMPTY_ROUTE_ARRAY = new RouteDescriptor[0];
        static final IntentFilter[] EMTPY_FILTER_ARRAY = new IntentFilter[0];

        private static final String KEY_ID = "id";
        private static final String KEY_NAME = "name";
        private static final String KEY_STATUS = "status";
        private static final String KEY_ICON_RESOURCE = "iconId";
        private static final String KEY_ENABLED = "enabled";
        private static final String KEY_CONTROL_FILTERS = "controlFilters";
        private static final String KEY_PLAYBACK_TYPE = "playbackType";
        private static final String KEY_PLAYBACK_STREAM = "playbackStream";
        private static final String KEY_VOLUME = "volume";
        private static final String KEY_VOLUME_MAX = "volumeMax";
        private static final String KEY_VOLUME_HANDLING = "volumeHandling";
        private static final String KEY_PRESENTATION_DISPLAY_ID = "presentationDisplayId";
        private static final String KEY_EXTRAS = "extras";

        private final Bundle mBundle;
        private IntentFilter[] mControlFilters;
        private Drawable mIconDrawable;

        /**
         * Creates a route descriptor.
         *
         * @param id The unique id of the route.
         * @param name The user-friendly name of the route.
         */
        public RouteDescriptor(String id, String name) {
            mBundle = new Bundle();
            setId(id);
            setName(name);
        }

        /**
         * Creates a copy of another route descriptor.
         */
        public RouteDescriptor(RouteDescriptor other) {
            mBundle = new Bundle(other.mBundle);
        }

        RouteDescriptor(Bundle bundle) {
            mBundle = bundle;
        }

        /**
         * Gets the unique id of the route.
         */
        public String getId() {
            return mBundle.getString(KEY_ID);
        }

        /**
         * Sets the unique id of the route.
         */
        public void setId(String id) {
            mBundle.putString(KEY_ID, id);
        }

        /**
         * Gets the user-friendly name of the route.
         */
        public String getName() {
            return mBundle.getString(KEY_NAME);
        }

        /**
         * Sets the user-friendly name of the route.
         */
        public void setName(String name) {
            mBundle.putString(KEY_NAME, name);
        }

        /**
         * Gets the user-friendly status of the route.
         */
        public String getStatus() {
            return mBundle.getString(KEY_STATUS);
        }

        /**
         * Sets the user-friendly status of the route.
         */
        public void setStatus(String status) {
            mBundle.putString(KEY_STATUS, status);
        }

        /**
         * Gets a drawable to display as the route's icon.
         * <p>
         * Because drawables cannot be transferred to other processes, this method may
         * only be used by media route providers that reside in the same process
         * as the application.  When implementing a media route provider service, use
         * {@link #getIconResource} instead.
         * </p>
         */
        public Drawable getIconDrawable() {
            return mIconDrawable;
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
        public void setIconDrawable(Drawable drawable) {
            mIconDrawable = drawable;
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
         * Sets the id of a drawable resource to display as the route's icon.
         * <p>
         * The specified drawable resource id will be loaded from the media route
         * provider's package.
         * </p>
         */
        public void setIconResource(int id) {
            mBundle.putInt(KEY_ICON_RESOURCE, id);
        }

        /**
         * Gets whether the route is enabled.
         */
        public boolean isEnabled() {
            return mBundle.getBoolean(KEY_ENABLED, true);
        }

        /**
         * Sets whether the route is enabled.
         */
        public void setEnabled(boolean enabled) {
            mBundle.putBoolean(KEY_ENABLED, enabled);
        }

        /**
         * Gets the route's {@link MediaControlIntent media control intent} filters.
         */
        public IntentFilter[] getControlFilters() {
            if (mControlFilters == null) {
                Parcelable[] filters = mBundle.getParcelableArray(KEY_CONTROL_FILTERS);
                if (filters instanceof IntentFilter[]) {
                    mControlFilters = (IntentFilter[])filters;
                } else if (filters != null && filters.length > 0) {
                    mControlFilters = new IntentFilter[filters.length];
                    System.arraycopy(filters, 0, mControlFilters, 0, filters.length);
                } else {
                    mControlFilters = EMTPY_FILTER_ARRAY;
                }
            }
            return mControlFilters;
        }

        /**
         * Sets the route's {@link MediaControlIntent media control intent} filters.
         */
        public void setControlFilters(IntentFilter[] controlFilters) {
            if (controlFilters == null) {
                throw new IllegalArgumentException("controlFilters must not be null");
            }
            mControlFilters = controlFilters;
            mBundle.putParcelableArray(KEY_CONTROL_FILTERS, controlFilters);
        }

        /**
         * Gets the route's playback type.
         */
        public int getPlaybackType() {
            return mBundle.getInt(KEY_PLAYBACK_TYPE, MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);
        }

        /**
         * Sets the route's playback type.
         */
        public void setPlaybackType(int playbackType) {
            mBundle.putInt(KEY_PLAYBACK_TYPE, playbackType);
        }

        /**
         * Gets the route's playback stream.
         */
        public int getPlaybackStream() {
            return mBundle.getInt(KEY_PLAYBACK_STREAM, -1);
        }

        /**
         * Sets the route's playback stream.
         */
        public void setPlaybackStream(int playbackStream) {
            mBundle.putInt(KEY_PLAYBACK_STREAM, playbackStream);
        }

        /**
         * Gets the route's current volume, or 0 if unknown.
         */
        public int getVolume() {
            return mBundle.getInt(KEY_VOLUME);
        }

        /**
         * Sets the route's current volume, or 0 if unknown.
         */
        public void setVolume(int volume) {
            mBundle.putInt(KEY_VOLUME, volume);
        }

        /**
         * Gets the route's maximum volume, or 0 if unknown.
         */
        public int getVolumeMax() {
            return mBundle.getInt(KEY_VOLUME_MAX);
        }

        /**
         * Sets the route's maximum volume, or 0 if unknown.
         */
        public void setVolumeMax(int volumeMax) {
            mBundle.putInt(KEY_VOLUME_MAX, volumeMax);
        }

        /**
         * Gets the route's volume handling.
         */
        public int getVolumeHandling() {
            return mBundle.getInt(KEY_VOLUME_HANDLING,
                    MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED);
        }

        /**
         * Sets the route's volume handling.
         */
        public void setVolumeHandling(int volumeHandling) {
            mBundle.putInt(KEY_VOLUME_HANDLING, volumeHandling);
        }

        /**
         * Gets the route's presentation display id, or -1 if none.
         */
        public int getPresentationDisplayId() {
            return mBundle.getInt(KEY_PRESENTATION_DISPLAY_ID, -1);
        }

        /**
         * Sets the route's presentation display id, or -1 if none.
         */
        public void setPresentationDisplayId(int presentationDisplayId) {
            mBundle.putInt(KEY_PRESENTATION_DISPLAY_ID, presentationDisplayId);
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
         * Sets a bundle of extras for this route descriptor.
         * The extras will be ignored by the media router but they may be used
         * by applications.
         */
        public void setExtras(Bundle extras) {
            mBundle.putBundle(KEY_EXTRAS, extras);
        }

        /**
         * Returns true if the route descriptor has all of the required fields.
         */
        public boolean isValid() {
            if (TextUtils.isEmpty(getId())
                    || TextUtils.isEmpty(getName())) {
                return false;
            }
            for (IntentFilter filter : getControlFilters()) {
                if (filter == null) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "RouteDescriptor{" + mBundle.toString() + "}";
        }

        Bundle asBundle() {
            return mBundle;
        }

        static Parcelable[] toParcelableArray(RouteDescriptor[] descriptors) {
            if (descriptors != null && descriptors.length > 0) {
                Parcelable[] bundles = new Parcelable[descriptors.length];
                for (int i = 0; i < descriptors.length; i++) {
                    bundles[i] = descriptors[i].asBundle();
                }
                return bundles;
            }
            return null;
        }

        static RouteDescriptor[] fromParcelableArray(Parcelable[] bundles) {
            if (bundles != null && bundles.length > 0) {
                RouteDescriptor[] descriptors = new RouteDescriptor[bundles.length];
                for (int i = 0; i < bundles.length; i++) {
                    descriptors[i] = new RouteDescriptor((Bundle)bundles[i]);
                }
                return descriptors;
            }
            return EMPTY_ROUTE_ARRAY;
        }
    }

    /**
     * Provides control over a particular route.
     * <p>
     * The media router obtains a route controller for a route whenever it needs
     * to control a route.  When a route is selected, the media router invokes
     * the {@link #select} method of its route controller.  While selected,
     * the media router may call other methods of the route controller to
     * request that it perform certain actions to the route.  When a route is
     * unselected, the media router invokes the {@link #unselect} method of its
     * route controller.  When the media route no longer needs the route controller
     * it will invoke the {@link #release} method to allow the route controller
     * to free its resources.
     * </p><p>
     * There may be multiple route controllers simultaneously active for the
     * same route.  Each route controller will be released separately.
     * </p><p>
     * All operations on the route controller are asynchronous and
     * results are communicated via callbacks.
     * </p>
     */
    public static abstract class RouteController {
        /**
         * Releases the route controller, allowing it to free its resources.
         */
        public void release() {
        }

        /**
         * Selects the route.
         */
        public void select() {
        }

        /**
         * Unselects the route.
         */
        public void unselect() {
        }

        /**
         * Requests to set the volume of the route.
         *
         * @param volume The new volume value between 0 and {@link RouteDescriptor#getVolumeMax}.
         */
        public void setVolume(int volume) {
        }

        /**
         * Requests an incremental volume update for the route.
         *
         * @param delta The delta to add to the current volume.
         */
        public void updateVolume(int delta) {
        }

        /**
         * Sends a {@link MediaControlIntent media control} request to be performed
         * asynchronously by the route's destination.
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @param callback A {@link ControlRequestCallback} to invoke with the result
         * of the request, or null if no result is required.
         * @return True if the controller intends to handle the request and will
         * invoke the callback when finished.  False if the contorller will not
         * handle the request and will not invoke the callback.
         *
         * @see MediaControlIntent
         */
        public boolean sendControlRequest(Intent intent, ControlRequestCallback callback) {
            return false;
        }
    }

    /**
     * Callback which is invoked when route information becomes available or changes.
     */
    public static abstract class Callback {
        /**
         * Called when information about a route provider and its routes changes.
         *
         * @param provider The media route provider that changed.
         * and all of its contents should be treated as if it were immutable so that it is
         * safe for clients to cache it.
         */
        public void onDescriptorChanged(MediaRouteProvider provider,
                ProviderDescriptor descriptor) {
        }
    }

    private final class ProviderHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELIVER_DESCRIPTOR_CHANGED:
                    deliverDescriptorChanged();
                    break;
            }
        }
    }
}
