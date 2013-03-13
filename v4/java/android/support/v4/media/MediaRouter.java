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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.hardware.display.DisplayManagerCompat;
import android.support.v4.media.MediaRouteProvider.RouteDescriptor;
import android.support.v4.media.MediaRouteProvider.RouteProviderDescriptor;
import android.util.Log;
import android.view.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MediaRouter allows applications to control the routing of media channels
 * and streams from the current device to external speakers and destination devices.
 * <p>
 * A MediaRouter instance is retrieved through {@link #getInstance}.  Applications
 * can query the media router about the currently selected route and its capabilities
 * to determine how to send content to the route's destination.  Applications can
 * also {@link RouteInfo#sendControlRequest send control requests} to the route
 * to ask the route's destination to perform certain remote control functions
 * such as playing media.
 * </p><p>
 * See also {@link MediaRouteProvider} for information on how an application
 * can publish new media routes to the media router.
 * </p><p>
 * The media router API is not thread-safe; all interactions with it must be
 * done from the main thread of the process.
 * </p>
 */
public final class MediaRouter {
    private static final String TAG = "MediaRouter";

    // Maintains global media router state for the process.
    // This field is initialized in MediaRouter.getInstance() before any
    // MediaRouter objects are instantiated so it is guaranteed to be
    // valid whenever any instance method is invoked.
    static GlobalMediaRouter sGlobal;

    // Context-bound state of the media router.
    final Context mContext;
    final CopyOnWriteArrayList<Callback> mCallbacks = new CopyOnWriteArrayList<Callback>();

    MediaRouter(Context context) {
        mContext = context;
    }

    /**
     * Gets an instance of the media router service from the context.
     */
    public static MediaRouter getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        checkCallingThread();

        if (sGlobal == null) {
            sGlobal = new GlobalMediaRouter(context.getApplicationContext());
            sGlobal.start();
        }
        return sGlobal.getRouter(context);
    }

    /**
     * Gets the {@link MediaRouter.RouteInfo routes} currently known to this MediaRouter.
     */
    public List<RouteInfo> getRoutes() {
        checkCallingThread();
        return sGlobal.getRoutes();
    }

    /**
     * Gets the default route for playing media content on the system.
     * <p>
     * The system always provides a default route.
     * </p>
     *
     * @return The default route, which is guaranteed to never be null.
     */
    public RouteInfo getDefaultRoute() {
        checkCallingThread();
        return sGlobal.getDefaultRoute();
    }

    /**
     * Gets the currently selected route.
     * <p>
     * The application should examine the route's
     * {@link RouteInfo#getControlFilters media control intent filters} to assess the
     * capabilities of the route before attempting to use it.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>
     * public boolean playMovie() {
     *     MediaRouter mediaRouter = MediaRouter.getInstance(context);
     *     MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute();
     *
     *     // First try using the remote playback interface, if supported.
     *     if (route.supportsControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)) {
     *         // The route supports remote playback.
     *         // Try to send it the Uri of the movie to play.
     *         Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
     *         intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
     *         intent.setDataAndType("http://example.com/videos/movie.mp4", "video/mp4");
     *         if (route.supportsControlRequest(intent)) {
     *             route.sendControlRequest(intent, null);
     *             return true; // sent the request to play the movie
     *         }
     *     }
     *
     *     // If remote playback was not possible, then play locally.
     *     if (route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)) {
     *         // The route supports live video streaming.
     *         // Prepare to play content locally in a window or in a presentation.
     *         return playMovieInWindow();
     *     }
     *
     *     // Neither interface is supported, so we can't play the movie to this route.
     *     return false;
     * }
     * </pre>
     *
     * @return The selected route, which is guaranteed to never be null.
     *
     * @see RouteInfo#getControlFilters
     * @see RouteInfo#supportsControlCategory
     * @see RouteInfo#supportsControlRequest
     */
    public RouteInfo getSelectedRoute() {
        checkCallingThread();
        return sGlobal.getSelectedRoute();
    }

    /**
     * Selects the specified route.
     *
     * @param route The route to select.
     */
    public void selectRoute(RouteInfo route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        checkCallingThread();

        sGlobal.selectRoute(route);
    }

    /**
     * Adds a callback to listen to changes to media routes.
     *
     * @param callback The callback to add.
     */
    public void addCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();

        if (!mCallbacks.contains(callback)) {
            mCallbacks.add(callback);
        }
    }

    /**
     * Removes the specified callback.  It will no longer receive information about
     * changes to media routes.
     *
     * @param callback The callback to remove.
     */
    public void removeCallback(Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();

        mCallbacks.remove(callback);
    }

    /**
     * Registers a media route provider globally for this application process.
     *
     * @param provider The media route provider to add.
     *
     * @see MediaRouteProvider
     */
    public void addProvider(MediaRouteProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        checkCallingThread();

        sGlobal.addProvider(provider);
    }

    /**
     * Unregisters a media route provider globally for this application process.
     *
     * @param provider The media route provider to remove.
     *
     * @see MediaRouteProvider
     */
    public void removeProvider(MediaRouteProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider must not be null");
        }
        checkCallingThread();

        sGlobal.removeProvider(provider);
    }

    /**
     * Ensures that calls into the media router are on the correct thread.
     * It pays to be a little paranoid when global state invariants are at risk.
     */
    static void checkCallingThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("The media router service must only be "
                    + "accessed on the application's main thread.");
        }
    }

    static <T> boolean equal(T a, T b) {
        return a == b || (a != null && b != null && a.equals(b));
    }

    /**
     * Provides information about a media route.
     * <p>
     * Each media route has a list of {@link MediaControlIntent media control}
     * {@link #getControlFilters intent filters} that describe the capabilities of the
     * route and the manner in which it is used and controlled.
     * </p>
     */
    public static final class RouteInfo {
        private final ProviderRecord mProviderRecord;
        private final String mDescriptorId;
        private String mName;
        private String mStatus;
        private Drawable mIconDrawable;
        private int mIconResource;
        private boolean mEnabled;
        private final ArrayList<IntentFilter> mControlFilters = new ArrayList<IntentFilter>();
        private int mPlaybackType;
        private int mPlaybackStream;
        private int mVolumeHandling;
        private int mVolume;
        private int mVolumeMax;
        private Display mPresentationDisplay;
        private int mPresentationDisplayId = -1;
        private Bundle mExtras;
        private RouteDescriptor mDescriptor;

        /**
         * The default playback type, "local", indicating the presentation of the media
         * is happening on the same device (e.g. a phone, a tablet) as where it is
         * controlled from.
         *
         * @see #getPlaybackType
         */
        public static final int PLAYBACK_TYPE_LOCAL = 0;

        /**
         * A playback type indicating the presentation of the media is happening on
         * a different device (i.e. the remote device) than where it is controlled from.
         *
         * @see #getPlaybackType
         */
        public static final int PLAYBACK_TYPE_REMOTE = 1;

        /**
         * Playback information indicating the playback volume is fixed, i.e. it cannot be
         * controlled from this object. An example of fixed playback volume is a remote player,
         * playing over HDMI where the user prefers to control the volume on the HDMI sink, rather
         * than attenuate at the source.
         *
         * @see #getVolumeHandling
         */
        public static final int PLAYBACK_VOLUME_FIXED = 0;

        /**
         * Playback information indicating the playback volume is variable and can be controlled
         * from this object.
         *
         * @see #getVolumeHandling
         */
        public static final int PLAYBACK_VOLUME_VARIABLE = 1;

        static final int CHANGE_GENERAL = 1 << 0;
        static final int CHANGE_VOLUME = 1 << 1;
        static final int CHANGE_PRESENTATION_DISPLAY = 1 << 2;

        RouteInfo(ProviderRecord providerRecord, String descriptorId) {
            mProviderRecord = providerRecord;
            mDescriptorId = descriptorId;
        }

        int updateDescriptor(RouteDescriptor descriptor) {
            int changes = 0;
            if (mDescriptor != descriptor) {
                mDescriptor = descriptor;
                if (descriptor != null) {
                    if (!equal(mName, descriptor.getName())) {
                        mName = descriptor.getName();
                        changes |= CHANGE_GENERAL;
                    }
                    if (!equal(mStatus, descriptor.getStatus())) {
                        mStatus = descriptor.getStatus();
                        changes |= CHANGE_GENERAL;
                    }
                    if (mIconResource != descriptor.getIconResource()) {
                        mIconResource = descriptor.getIconResource();
                        mIconDrawable = null;
                        changes |= CHANGE_GENERAL;
                    }
                    if (mIconResource == 0
                            && mIconDrawable != descriptor.getIconDrawable()) {
                        mIconDrawable = descriptor.getIconDrawable();
                        changes |= CHANGE_GENERAL;
                    }
                    if (mEnabled != descriptor.isEnabled()) {
                        mEnabled = descriptor.isEnabled();
                        changes |= CHANGE_GENERAL;
                    }
                    IntentFilter[] descriptorControlFilters = descriptor.getControlFilters();
                    if (!hasSameControlFilters(descriptorControlFilters)) {
                        mControlFilters.clear();
                        for (IntentFilter f : descriptorControlFilters) {
                            mControlFilters.add(f);
                        }
                        changes |= CHANGE_GENERAL;
                    }
                    if (mPlaybackType != descriptor.getPlaybackType()) {
                        mPlaybackType = descriptor.getPlaybackType();
                        changes |= CHANGE_GENERAL;
                    }
                    if (mPlaybackStream != descriptor.getPlaybackStream()) {
                        mPlaybackStream = descriptor.getPlaybackStream();
                        changes |= CHANGE_GENERAL;
                    }
                    if (mVolumeHandling != descriptor.getVolumeHandling()) {
                        mVolumeHandling = descriptor.getVolumeHandling();
                        changes |= CHANGE_GENERAL | CHANGE_VOLUME;
                    }
                    if (mVolume != descriptor.getVolume()) {
                        mVolume = descriptor.getVolume();
                        changes |= CHANGE_GENERAL | CHANGE_VOLUME;
                    }
                    if (mVolumeMax != descriptor.getVolumeMax()) {
                        mVolumeMax = descriptor.getVolumeMax();
                        changes |= CHANGE_GENERAL | CHANGE_VOLUME;
                    }
                    if (mPresentationDisplayId != descriptor.getPresentationDisplayId()) {
                        mPresentationDisplayId = descriptor.getPresentationDisplayId();
                        mPresentationDisplay = null;
                        changes |= CHANGE_GENERAL | CHANGE_PRESENTATION_DISPLAY;
                    }
                    if (!equal(mExtras, descriptor.getExtras())) {
                        mExtras = descriptor.getExtras();
                        changes |= CHANGE_GENERAL;
                    }
                }
            }
            return changes;
        }

        boolean hasSameControlFilters(IntentFilter[] controlFilters) {
            final int count = mControlFilters.size();
            if (count != controlFilters.length) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                if (!mControlFilters.get(i).equals(controlFilters[i])) {
                    return false;
                }
            }
            return true;
        }

        MediaRouteProvider getProvider() {
            return mProviderRecord.mProvider;
        }

        String getDescriptorId() {
            return mDescriptorId;
        }

        void select() {
            sGlobal.selectRoute(this);
        }

        /**
         * Gets the name of this route.
         *
         * @return The user-friendly name of a media route. This is the string presented
         * to users who may select this as the active route.
         */
        public String getName() {
            return mName;
        }

        /**
         * Gets the status of this route.
         *
         * @return The user-friendly status for a media route. This may include a description
         * of the currently playing media, if available.
         */
        public String getStatus() {
            return mStatus;
        }

        /**
         * Gets the package name of the provider of this route.
         *
         * @return The package name of the provider of this route.
         */
        public String getProviderPackageName() {
            return mProviderRecord.getProviderPackageName();
        }

        /**
         * Get the icon representing this route.
         * This icon will be used in picker UIs if available.
         *
         * @return The icon representing this route or null if no icon is available.
         */
        public Drawable getIconDrawable() {
            if (mIconDrawable == null) {
                if (mIconResource != 0) {
                    Context context = mProviderRecord.getProviderContext();
                    if (context != null) {
                        try {
                            mIconDrawable = context.getResources().getDrawable(mIconResource);
                        } catch (Resources.NotFoundException ex) {
                            Log.w(TAG, "Unable to load media route icon drawable resource.", ex);
                        }
                    }
                }
            }
            return mIconDrawable;
        }

        /**
         * Returns true if this route is enabled and may be selected.
         *
         * @return true if this route is enabled and may be selected.
         */
        public boolean isEnabled() {
            return mEnabled;
        }

        /**
         * Returns true if this route is currently selected.
         *
         * @return true if this route is currently selected.
         *
         * @see MediaRouter#getSelectedRoute
         */
        public boolean isSelected() {
            checkCallingThread();
            return sGlobal.getSelectedRoute() == this;
        }

        /**
         * Returns true if this route is the default route.
         *
         * @return true if this route is the default route.
         *
         * @see MediaRouter#getDefaultRoute
         */
        public boolean isDefault() {
            checkCallingThread();
            return sGlobal.getDefaultRoute() == this;
        }

        /**
         * Gets a list of {@link MediaControlIntent media control intent} filters that
         * describe the capabilities of this route and the media control actions that
         * it supports.
         *
         * @return A list of intent filters that specifies the media control intents that
         * this route supports.
         *
         * @see MediaControlIntent
         * @see #supportsControlCategory
         * @see #supportsControlRequest
         */
        public List<IntentFilter> getControlFilters() {
            return mControlFilters;
        }

        /**
         * Returns true if the route supports the specified
         * {@link MediaControlIntent media control} category.
         * <p>
         * Media control categories describe the capabilities of this route
         * such as whether it supports live audio streaming or remote playback.
         * </p>
         *
         * @param category A {@link MediaControlIntent media control} category
         * such as {@link MediaControlIntent#CATEGORY_LIVE_AUDIO},
         * {@link MediaControlIntent#CATEGORY_LIVE_VIDEO},
         * {@link MediaControlIntent#CATEGORY_REMOTE_PLAYBACK}, or a provider-defined
         * media control category.
         *
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        public boolean supportsControlCategory(String category) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            checkCallingThread();

            int count = mControlFilters.size();
            for (int i = 0; i < count; i++) {
                if (mControlFilters.get(i).hasCategory(category)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns true if the route supports the specified
         * {@link MediaControlIntent media control} request.
         * <p>
         * Media control requests are used to request the route to perform
         * actions such as starting remote playback of a content stream.
         * </p>
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @return True if the route can handle the specified intent.
         *
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        public boolean supportsControlRequest(Intent intent) {
            if (intent == null) {
                throw new IllegalArgumentException("intent must not be null");
            }
            checkCallingThread();

            ContentResolver contentResolver = sGlobal.getContentResolver();
            int count = mControlFilters.size();
            for (int i = 0; i < count; i++) {
                if (mControlFilters.get(i).match(contentResolver, intent, true, TAG) >= 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Sends a {@link MediaControlIntent media control} request to be performed
         * asynchronously by the route's destination.
         * <p>
         * Media control requests are used to request the route to perform
         * actions such as starting remote playback of a content stream.
         * </p><p>
         * This function may only be called on a selected route.  Control requests
         * sent to unselected routes will fail.
         * </p>
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @param callback A {@link ControlRequestCallback} to invoke with the result
         * of the request, or null if no result is required.
         *
         * @see MediaControlIntent
         */
        public void sendControlRequest(Intent intent, ControlRequestCallback callback) {
            if (intent == null) {
                throw new IllegalArgumentException("intent must not be null");
            }
            checkCallingThread();

            sGlobal.sendControlRequest(this, intent, callback);
        }

        /**
         * Gets the type of playback associated with this route.
         *
         * @return The type of playback associated with this route: {@link #PLAYBACK_TYPE_LOCAL}
         * or {@link #PLAYBACK_TYPE_REMOTE}.
         */
        public int getPlaybackType() {
            return mPlaybackType;
        }

        /**
         * Gets the the stream over which the playback associated with this route is performed.
         *
         * @return The stream over which the playback associated with this route is performed.
         */
        public int getPlaybackStream() {
            return mPlaybackStream;
        }

        /**
         * Gets information about how volume is handled on the route.
         *
         * @return How volume is handled on the route: {@link #PLAYBACK_VOLUME_FIXED}
         * or {@link #PLAYBACK_VOLUME_VARIABLE}.
         */
        public int getVolumeHandling() {
            return mVolumeHandling;
        }

        /**
         * Gets the current volume for this route. Depending on the route, this may only
         * be valid if the route is currently selected.
         *
         * @return The volume at which the playback associated with this route is performed.
         */
        public int getVolume() {
            return mVolume;
        }

        /**
         * Gets the maximum volume at which the playback associated with this route is performed.
         *
         * @return The maximum volume at which the playback associated with
         * this route is performed.
         */
        public int getVolumeMax() {
            return mVolumeMax;
        }

        /**
         * Requests a volume change for this route asynchronously.
         * <p>
         * This function may only be called on a selected route.  It will have
         * no effect if the route is currently unselected.
         * </p>
         *
         * @param volume The new volume value between 0 and {@link #getVolumeMax}.
         */
        public void requestSetVolume(int volume) {
            checkCallingThread();
            sGlobal.requestSetVolume(this, Math.min(mVolumeMax, Math.max(0, volume)));
        }

        /**
         * Requests an incremental volume update for this route asynchronously.
         * <p>
         * This function may only be called on a selected route.  It will have
         * no effect if the route is currently unselected.
         * </p>
         *
         * @param delta The delta to add to the current volume.
         */
        public void requestUpdateVolume(int delta) {
            checkCallingThread();
            if (delta != 0) {
                sGlobal.requestUpdateVolume(this, delta);
            }
        }

        /**
         * Gets the {@link Display} that should be used by the application to show
         * a {@link android.app.Presentation} on an external display when this route is selected.
         * Depending on the route, this may only be valid if the route is currently
         * selected.
         * <p>
         * The preferred presentation display may change independently of the route
         * being selected or unselected.  For example, the presentation display
         * of the default system route may change when an external HDMI display is connected
         * or disconnected even though the route itself has not changed.
         * </p><p>
         * This method may return null if there is no external display associated with
         * the route or if the display is not ready to show UI yet.
         * </p><p>
         * The application should listen for changes to the presentation display
         * using the {@link Callback#onRoutePresentationDisplayChanged} callback and
         * show or dismiss its {@link android.app.Presentation} accordingly when the display
         * becomes available or is removed.
         * </p><p>
         * This method only makes sense for
         * {@link MediaControlIntent#CATEGORY_LIVE_VIDEO live video} routes.
         * </p>
         *
         * @return The preferred presentation display to use when this route is
         * selected or null if none.
         *
         * @see MediaControlIntent#CATEGORY_LIVE_VIDEO
         * @see android.app.Presentation
         */
        public Display getPresentationDisplay() {
            if (mPresentationDisplayId >= 0 && mPresentationDisplay == null) {
                mPresentationDisplay = sGlobal.getDisplay(mPresentationDisplayId);
            }
            return mPresentationDisplay;
        }

        /**
         * Gets a collection of extra properties about this route that were supplied
         * by its media route provider, or null if none.
         */
        public Bundle getExtras() {
            return mExtras;
        }

        @Override
        public String toString() {
            return "MediaRouter.RouteInfo{ name=" + mName
                    + ", status=" + mStatus
                    + ", enabled=" + mEnabled
                    + ", playbackType=" + mPlaybackType
                    + ", playbackStream=" + mPlaybackStream
                    + ", volumeHandling=" + mVolumeHandling
                    + ", volume=" + mVolume
                    + ", volumeMax=" + mVolumeMax
                    + ", presentationDisplayId=" + mPresentationDisplayId
                    + ", extras=" + mExtras
                    + " }";
        }
    }

    /**
     * Interface for receiving events about media routing changes.
     * All methods of this interface will be called from the application's main thread.
     * <p>
     * A Callback will only receive events relevant to routes that the callback
     * was registered for.
     * </p>
     *
     * @see MediaRouter#addCallback(Callback)
     * @see MediaRouter#removeCallback(Callback)
     */
    public static abstract class Callback {
        /**
         * Called when the supplied route becomes selected as the active route.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route that has been selected.
         */
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when the supplied route becomes unselected as the active route.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route that has been unselected.
         */
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when a route has been added.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route that has become available for use.
         */
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when a route has been removed.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route that has been removed from availability.
         */
        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when a property of the indicated route has changed.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route that was changed.
         */
        public void onRouteChanged(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when a route's volume changes.
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route whose volume changed.
         */
        public void onRouteVolumeChanged(MediaRouter router, RouteInfo route) {
        }

        /**
         * Called when a route's presentation display changes.
         * <p>
         * This method is called whenever the route's presentation display becomes
         * available, is removes or has changes to some of its properties (such as its size).
         * </p>
         *
         * @param router The MediaRouter reporting the event.
         * @param route The route whose presentation display changed.
         *
         * @see RouteInfo#getPresentationDisplay()
         */
        public void onRoutePresentationDisplayChanged(MediaRouter router, RouteInfo route) {
        }
    }

    /**
     * Callback which is invoked with the result of a media control request.
     *
     * @see RouteInfo#sendControlRequest
     */
    public static abstract class ControlRequestCallback {
        /**
         * Result code: The media control action succeeded.
         */
        public static final int REQUEST_SUCCEEDED = 0;

        /**
         * Result code: The media control action failed.
         */
        public static final int REQUEST_FAILED = -1;

        /**
         * Called with the result of the media control request.
         *
         * @param result The result code: {@link #REQUEST_SUCCEEDED}, or {@link #REQUEST_FAILED}.
         * @param data Additional result data.  Contents depend on the media control action.
         */
        public void onResult(int result, Bundle data) {
        }
    }

    /**
     * State associated with a media route provider.
     */
    private static final class ProviderRecord {
        public final MediaRouteProvider mProvider;
        public final ArrayList<RouteInfo> mRoutes = new ArrayList<RouteInfo>();
        public RouteProviderDescriptor mDescriptor;

        private String mProviderPackageName;
        private Context mProviderContext;

        public ProviderRecord(MediaRouteProvider provider) {
            mProvider = provider;
        }

        String getProviderPackageName() {
            if (mProviderPackageName == null) {
                // This should never happen in practice because we only query the
                // package name after routes have been created, which implies that
                // the provider has published a descriptor.  Check it anyway for the
                // sake of paranoia.
                throw new IllegalStateException("The provider's package name "
                        + "is not available because the package has not published "
                        + "a descriptor yet.");
            }
            return mProviderPackageName;
        }

        Context getProviderContext() {
            if (mProviderContext == null) {
                mProviderContext = sGlobal.getProviderContext(getProviderPackageName());
            }
            return mProviderContext;
        }

        boolean updateDescriptor(RouteProviderDescriptor descriptor) {
            if (mDescriptor != descriptor) {
                mDescriptor = descriptor;
                if (descriptor != null && mProviderPackageName == null) {
                    mProviderPackageName = descriptor.getPackageName();
                }
                return true;
            }
            return false;
        }

        int findRouteByDescriptorId(String id) {
            final int count = mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (mRoutes.get(i).mDescriptorId.equals(id)) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Global state for the media router.
     * <p>
     * Media routes and media route providers are global to the process; their
     * state and the bulk of the media router implementation lives here.
     * </p>
     */
    private static final class GlobalMediaRouter implements SystemMediaRouteProvider.SyncCallback {
        private final Context mApplicationContext;
        private final MediaRouter mApplicationRouter;
        private final WeakHashMap<Context, MediaRouter> mRouters =
                new WeakHashMap<Context, MediaRouter>();
        private final ArrayList<ProviderRecord> mProviderRecords =
                new ArrayList<ProviderRecord>();
        private final ArrayList<RouteInfo> mRoutes = new ArrayList<RouteInfo>();
        private final ProviderCallback mProviderCallback = new ProviderCallback();
        private final CallbackHandler mCallbackHandler = new CallbackHandler();
        private final DisplayManagerCompat mDisplayManager;
        private final SystemMediaRouteProvider mSystemProvider;

        private RegisteredMediaRouteProviderWatcher mRegisteredProviderWatcher;
        private RouteInfo mDefaultRoute;
        private RouteInfo mSelectedRoute;
        private MediaRouteProvider.RouteController mSelectedRouteController;

        GlobalMediaRouter(Context applicationContext) {
            mApplicationContext = applicationContext;
            mDisplayManager = DisplayManagerCompat.getInstance(applicationContext);
            mApplicationRouter = getRouter(applicationContext);

            // Add the system media route provider for interoperating with
            // the framework media router.  This one is special and receives
            // synchronization messages from the media router.
            mSystemProvider = SystemMediaRouteProvider.obtain(applicationContext, this);
            addProvider(mSystemProvider);
        }

        public void start() {
            // Start watching for routes published by registered media route
            // provider services.
            mRegisteredProviderWatcher = new RegisteredMediaRouteProviderWatcher(
                    mApplicationContext, mApplicationRouter);
            mRegisteredProviderWatcher.start();
        }

        public MediaRouter getRouter(Context context) {
            MediaRouter router = mRouters.get(context);
            if (router == null) {
                router = new MediaRouter(context);
                mRouters.put(context, router);
            }
            return router;
        }

        public ContentResolver getContentResolver() {
            return mApplicationContext.getContentResolver();
        }

        public Context getProviderContext(String packageName) {
            if (packageName.equals(SystemMediaRouteProvider.PACKAGE_NAME)) {
                return mApplicationContext;
            }
            try {
                return mApplicationContext.createPackageContext(
                        packageName, Context.CONTEXT_RESTRICTED);
            } catch (NameNotFoundException ex) {
                return null;
            }
        }

        public Display getDisplay(int displayId) {
            return mDisplayManager.getDisplay(displayId);
        }

        public void sendControlRequest(RouteInfo route,
                Intent intent, ControlRequestCallback callback) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                if (mSelectedRouteController.sendControlRequest(intent, callback)) {
                    return;
                }
            }
            if (callback != null) {
                callback.onResult(ControlRequestCallback.REQUEST_FAILED, null);
            }
        }

        public void requestSetVolume(RouteInfo route, int volume) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                mSelectedRouteController.setVolume(volume);
            }
        }

        public void requestUpdateVolume(RouteInfo route, int delta) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                mSelectedRouteController.updateVolume(delta);
            }
        }

        public List<RouteInfo> getRoutes() {
            return mRoutes;
        }

        public RouteInfo getDefaultRoute() {
            if (mDefaultRoute == null) {
                // This should never happen once the media router has been fully
                // initialized but it is good to check for the error in case there
                // is a bug in provider initialization.
                throw new IllegalStateException("There is no default route.  "
                        + "The media router has not yet been fully initialized.");
            }
            return mDefaultRoute;
        }

        public RouteInfo getSelectedRoute() {
            if (mSelectedRoute == null) {
                // This should never happen once the media router has been fully
                // initialized but it is good to check for the error in case there
                // is a bug in provider initialization.
                throw new IllegalStateException("There is no currently selected route.  "
                        + "The media router has not yet been fully initialized.");
            }
            return mSelectedRoute;
        }

        public void selectRoute(RouteInfo route) {
            if (!mRoutes.contains(route)) {
                Log.w(TAG, "Ignoring attempt to select removed route: " + route);
                return;
            }
            if (!route.mEnabled) {
                Log.w(TAG, "Ignoring attempt to select disabled route: " + route);
                return;
            }

            setSelectedRouteInternal(route);
        }

        public void addProvider(MediaRouteProvider provider) {
            int index = findProviderRecord(provider);
            if (index < 0) {
                // 1. Add the provider to the list.
                ProviderRecord providerRecord = new ProviderRecord(provider);
                mProviderRecords.add(providerRecord);
                // 2. Create the provider's contents.
                updateProviderContents(providerRecord, provider.getDescriptor());
                // 3. Register the provider callback.
                provider.addCallback(mProviderCallback);
                // 4. Update the selected route if needed.
                updateSelectedRoute();
            }
        }

        public void removeProvider(MediaRouteProvider provider) {
            int index = findProviderRecord(provider);
            if (index >= 0) {
                // 1. Unregister the provider callback.
                provider.removeCallback(mProviderCallback);
                // 2. Delete the provider's contents.
                ProviderRecord providerRecord = mProviderRecords.get(index);
                updateProviderContents(providerRecord, null);
                // 3. Remove the provider from the list.
                mProviderRecords.remove(index);
                // 4. Update the selected route if needed.
                updateSelectedRoute();
            }
        }

        private void updateProviderDescriptor(MediaRouteProvider provider,
                RouteProviderDescriptor descriptor) {
            int index = findProviderRecord(provider);
            if (index >= 0) {
                // 1. Update the provider's contents.
                ProviderRecord providerRecord = mProviderRecords.get(index);
                updateProviderContents(providerRecord, descriptor);
                // 2. Update the selected route if needed.
                updateSelectedRoute();
            }
        }

        private int findProviderRecord(MediaRouteProvider provider) {
            final int count = mProviderRecords.size();
            for (int i = 0; i < count; i++) {
                if (mProviderRecords.get(i).mProvider == provider) {
                    return i;
                }
            }
            return -1;
        }

        private void updateProviderContents(ProviderRecord providerRecord,
                RouteProviderDescriptor providerDescriptor) {
            if (providerRecord.updateDescriptor(providerDescriptor)) {
                // Update all existing routes and reorder them to match
                // the order of their descriptors.
                int targetIndex = 0;
                if (providerDescriptor != null) {
                    if (providerDescriptor.isValid()) {
                        final RouteDescriptor[] routeDescriptors = providerDescriptor.getRoutes();
                        for (int i = 0; i < routeDescriptors.length; i++) {
                            final RouteDescriptor routeDescriptor = routeDescriptors[i];
                            final String id = routeDescriptor.getId();
                            final int sourceIndex = providerRecord.findRouteByDescriptorId(id);
                            if (sourceIndex < 0) {
                                // 1. Add the route to the list.
                                RouteInfo route = new RouteInfo(providerRecord, id);
                                providerRecord.mRoutes.add(targetIndex++, route);
                                mRoutes.add(route);
                                // 2. Create the route's contents.
                                route.updateDescriptor(routeDescriptor);
                                // 3. Notify clients.
                                mCallbackHandler.post(CallbackHandler.MSG_ROUTE_ADDED, route);
                            } else if (sourceIndex < targetIndex) {
                                Log.w(TAG, "Ignoring route descriptor with duplicate id: "
                                        + routeDescriptor);
                            } else {
                                // 1. Reorder the route within the list.
                                RouteInfo route = providerRecord.mRoutes.get(sourceIndex);
                                Collections.swap(providerRecord.mRoutes,
                                        sourceIndex, targetIndex++);
                                // 2. Update the route's contents.
                                int changes = route.updateDescriptor(routeDescriptor);
                                // 3. Notify clients.
                                if ((changes & RouteInfo.CHANGE_GENERAL) != 0) {
                                    mCallbackHandler.post(
                                            CallbackHandler.MSG_ROUTE_CHANGED, route);
                                }
                                if ((changes & RouteInfo.CHANGE_VOLUME) != 0) {
                                    mCallbackHandler.post(
                                            CallbackHandler.MSG_ROUTE_VOLUME_CHANGED, route);
                                }
                                if ((changes & RouteInfo.CHANGE_PRESENTATION_DISPLAY) != 0) {
                                    mCallbackHandler.post(CallbackHandler.
                                            MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED, route);
                                }
                            }
                        }
                    } else {
                        Log.w(TAG, "Ignoring invalid provider descriptor: " + providerDescriptor);
                    }
                }

                // Dispose all remaining routes that do not have matching descriptors.
                for (int i = providerRecord.mRoutes.size() - 1; i >= targetIndex; i--) {
                    // 1. Notify clients.
                    RouteInfo route = providerRecord.mRoutes.get(i);
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_REMOVED, route);
                    // 2. Delete the route's contents.
                    route.updateDescriptor(null);
                    // 3. Remove the route from the list.
                    mRoutes.remove(providerRecord);
                    providerRecord.mRoutes.remove(i);
                }
            }
        }

        private void updateSelectedRoute() {
            // Update the default route.
            if (mDefaultRoute != null && !isRouteSelectable(mDefaultRoute)) {
                Log.i(TAG, "Choosing a new default route because the current one "
                        + "is no longer selectable: " + mDefaultRoute);
                mDefaultRoute = null;
            }
            if (mDefaultRoute == null && !mRoutes.isEmpty()) {
                for (RouteInfo route : mRoutes) {
                    if (isSystemDefaultRoute(route) && isRouteSelectable(route)) {
                        mDefaultRoute = route;
                        break;
                    }
                }
            }

            // Update the selected route.
            if (mSelectedRoute != null && !isRouteSelectable(mSelectedRoute)) {
                Log.i(TAG, "Choosing a new selected route because the current one "
                        + "is no longer selectable: " + mSelectedRoute);
                setSelectedRouteInternal(null);
            }
            if (mSelectedRoute == null) {
                setSelectedRouteInternal(mDefaultRoute);
            }
        }

        private boolean isRouteSelectable(RouteInfo route) {
            // This tests whether the route is still valid and enabled.
            // The route descriptor field is set to null when the route is removed.
            return route.mDescriptor != null && route.mEnabled;
        }

        private boolean isSystemDefaultRoute(RouteInfo route) {
            return route.getProvider() == mSystemProvider
                    && route.mDescriptorId.equals(
                            SystemMediaRouteProvider.DEFAULT_ROUTE_ID);
        }

        private void setSelectedRouteInternal(RouteInfo route) {
            if (mSelectedRoute != route) {
                if (mSelectedRoute != null) {
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_UNSELECTED, mSelectedRoute);
                    if (mSelectedRouteController != null) {
                        mSelectedRouteController.unselect();
                        mSelectedRouteController.release();
                        mSelectedRouteController = null;
                    }
                }

                mSelectedRoute = route;

                if (mSelectedRoute != null) {
                    mSelectedRouteController = route.getProvider().onCreateRouteController(
                            route.mDescriptorId);
                    if (mSelectedRouteController != null) {
                        mSelectedRouteController.select();
                    }
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_SELECTED, mSelectedRoute);
                }
            }
        }

        @Override
        public RouteInfo getSystemRouteByDescriptorId(String id) {
            int providerIndex = findProviderRecord(mSystemProvider);
            if (providerIndex >= 0) {
                ProviderRecord providerRecord = mProviderRecords.get(providerIndex);
                int routeIndex = providerRecord.findRouteByDescriptorId(id);
                if (routeIndex >= 0) {
                    return providerRecord.mRoutes.get(routeIndex);
                }
            }
            return null;
        }

        private final class ProviderCallback extends MediaRouteProvider.Callback {
            @Override
            public void onDescriptorChanged(MediaRouteProvider provider,
                    RouteProviderDescriptor descriptor) {
                updateProviderDescriptor(provider, descriptor);
            }
        }

        private final class CallbackHandler extends Handler {
            private final ArrayList<MediaRouter> mTempMediaRouters =
                    new ArrayList<MediaRouter>();

            public static final int MSG_ROUTE_ADDED = 1;
            public static final int MSG_ROUTE_REMOVED = 2;
            public static final int MSG_ROUTE_CHANGED = 3;
            public static final int MSG_ROUTE_VOLUME_CHANGED = 4;
            public static final int MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED = 5;
            public static final int MSG_ROUTE_SELECTED = 6;
            public static final int MSG_ROUTE_UNSELECTED = 7;

            public void post(int msg, RouteInfo route) {
                obtainMessage(msg, route).sendToTarget();
            }

            @Override
            public void handleMessage(Message msg) {
                final int what = msg.what;
                final RouteInfo route = (RouteInfo)msg.obj;

                // Synchronize state with the system media router.
                syncWithSystemProvider(what, route);

                // Invoke all registered callbacks.
                mTempMediaRouters.addAll(mRouters.values());
                try {
                    final int routerCount = mTempMediaRouters.size();
                    for (int i = 0; i < routerCount; i++) {
                        final MediaRouter router = mTempMediaRouters.get(i);
                        if (!router.mCallbacks.isEmpty()) {
                            for (MediaRouter.Callback callback : router.mCallbacks) {
                                invokeCallback(router, callback, what, route);
                            }
                        }
                    }
                } finally {
                    mTempMediaRouters.clear();
                }
            }

            private void syncWithSystemProvider(int what, RouteInfo route) {
                switch (what) {
                    case MSG_ROUTE_ADDED:
                        mSystemProvider.onSyncRouteAdded(route);
                        break;
                    case MSG_ROUTE_REMOVED:
                        mSystemProvider.onSyncRouteRemoved(route);
                        break;
                    case MSG_ROUTE_CHANGED:
                        mSystemProvider.onSyncRouteChanged(route);
                        break;
                    case MSG_ROUTE_SELECTED:
                        mSystemProvider.onSyncRouteSelected(route);
                        break;
                }
            }

            private void invokeCallback(MediaRouter router, MediaRouter.Callback callback,
                    int what, RouteInfo route) {
                switch (what) {
                    case MSG_ROUTE_ADDED:
                        callback.onRouteAdded(router, route);
                        break;
                    case MSG_ROUTE_REMOVED:
                        callback.onRouteRemoved(router, route);
                        break;
                    case MSG_ROUTE_CHANGED:
                        callback.onRouteChanged(router, route);
                        break;
                    case MSG_ROUTE_VOLUME_CHANGED:
                        callback.onRouteVolumeChanged(router, route);
                        break;
                    case MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED:
                        callback.onRoutePresentationDisplayChanged(router, route);
                        break;
                    case MSG_ROUTE_SELECTED:
                        callback.onRouteSelected(router, route);
                        break;
                    case MSG_ROUTE_UNSELECTED:
                        callback.onRouteUnselected(router, route);
                        break;
                }
            }
        }
    }
}
