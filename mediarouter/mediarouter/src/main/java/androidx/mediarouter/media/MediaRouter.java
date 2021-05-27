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
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.display.DisplayManagerCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Pair;
import androidx.media.VolumeProviderCompat;
import androidx.mediarouter.app.MediaRouteDiscoveryFragment;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider.ProviderMetadata;
import androidx.mediarouter.media.MediaRouteProvider.RouteController;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
// TODO: Add the javadoc for manifest requirements about 'Package visibility' in Android 11
public final class MediaRouter {
    static final String TAG = "MediaRouter";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @IntDef({UNSELECT_REASON_UNKNOWN, UNSELECT_REASON_DISCONNECTED, UNSELECT_REASON_STOPPED,
            UNSELECT_REASON_ROUTE_CHANGED})
    @Retention(RetentionPolicy.SOURCE)
    @interface UnselectReason {}

    /**
     * Passed to {@link MediaRouteProvider.RouteController#onUnselect(int)},
     * {@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and
     * {@link Callback#onRouteSelected(MediaRouter, RouteInfo, int)} when the reason the route
     * was unselected is unknown.
     */
    public static final int UNSELECT_REASON_UNKNOWN = 0;
    /**
     * Passed to {@link MediaRouteProvider.RouteController#onUnselect(int)},
     * {@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and
     * {@link Callback#onRouteSelected(MediaRouter, RouteInfo, int)} when the user pressed
     * the disconnect button to disconnect and keep playing.
     * <p>
     *
     * @see MediaRouteDescriptor#canDisconnectAndKeepPlaying()
     */
    public static final int UNSELECT_REASON_DISCONNECTED = 1;
    /**
     * Passed to {@link MediaRouteProvider.RouteController#onUnselect(int)},
     * {@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and
     * {@link Callback#onRouteSelected(MediaRouter, RouteInfo, int)} when the user pressed
     * the stop casting button.
     * <p>
     * Media should stop when this reason is passed.
     */
    public static final int UNSELECT_REASON_STOPPED = 2;
    /**
     * Passed to {@link MediaRouteProvider.RouteController#onUnselect(int)},
     * {@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and
     * {@link Callback#onRouteSelected(MediaRouter, RouteInfo, int)} when the user selected
     * a different route.
     */
    public static final int UNSELECT_REASON_ROUTE_CHANGED = 3;

    // Maintains global media router state for the process.
    // This field is initialized lazily when it is necessary.
    // Access this field directly only when you don't want to initialize it.
    // Use {@link #getGlobalRouter()} to get a valid instance.
    static GlobalMediaRouter sGlobal;

    // Context-bound state of the media router.
    final Context mContext;
    final ArrayList<CallbackRecord> mCallbackRecords = new ArrayList<CallbackRecord>();

    @IntDef(flag = true,
            value = {
                    CALLBACK_FLAG_PERFORM_ACTIVE_SCAN,
                    CALLBACK_FLAG_REQUEST_DISCOVERY,
                    CALLBACK_FLAG_UNFILTERED_EVENTS,
                    CALLBACK_FLAG_FORCE_DISCOVERY
            }
    )
    @Retention(RetentionPolicy.SOURCE)
    private @interface CallbackFlags {}

    /**
     * Flag for {@link #addCallback}: Actively scan for routes while this callback
     * is registered.
     * <p>
     * When this flag is specified, the media router will actively scan for new
     * routes.  Certain routes, such as wifi display routes, may not be discoverable
     * except when actively scanning.  This flag is typically used when the route picker
     * dialog has been opened by the user to ensure that the route information is
     * up to date.
     * </p><p>
     * Active scanning may consume a significant amount of power and may have intrusive
     * effects on wireless connectivity.  Therefore it is important that active scanning
     * only be requested when it is actually needed to satisfy a user request to
     * discover and select a new route.
     * </p><p>
     * This flag implies {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} but performing
     * active scans is much more expensive than a normal discovery request.
     * </p>
     *
     * @see #CALLBACK_FLAG_REQUEST_DISCOVERY
     */
    public static final int CALLBACK_FLAG_PERFORM_ACTIVE_SCAN = 1 << 0;

    /**
     * Flag for {@link #addCallback}: Do not filter route events.
     * <p>
     * When this flag is specified, the callback will be invoked for events that affect any
     * route even if they do not match the callback's filter.
     * </p>
     */
    public static final int CALLBACK_FLAG_UNFILTERED_EVENTS = 1 << 1;

    /**
     * Flag for {@link #addCallback}: Request passive route discovery while this
     * callback is registered, except on {@link ActivityManager#isLowRamDevice low-RAM devices}.
     * <p>
     * When this flag is specified, the media router will try to discover routes.
     * Although route discovery is intended to be efficient, checking for new routes may
     * result in some network activity and could slowly drain the battery.  Therefore
     * applications should only specify {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} when
     * they are running in the foreground and would like to provide the user with the
     * option of connecting to new routes.
     * </p><p>
     * Applications should typically add a callback using this flag in the
     * {@link android.app.Activity activity's} {@link android.app.Activity#onStart onStart}
     * method and remove it in the {@link android.app.Activity#onStop onStop} method.
     * The {@link MediaRouteDiscoveryFragment} fragment may
     * also be used for this purpose.
     * </p><p class="note">
     * On {@link ActivityManager#isLowRamDevice low-RAM devices} this flag
     * will be ignored.  Refer to
     * {@link #addCallback(MediaRouteSelector, Callback, int) addCallback} for details.
     * </p>
     *
     * @see MediaRouteDiscoveryFragment
     */
    public static final int CALLBACK_FLAG_REQUEST_DISCOVERY = 1 << 2;

    /**
     * Flag for {@link #addCallback}: Request passive route discovery while this
     * callback is registered, even on {@link ActivityManager#isLowRamDevice low-RAM devices}.
     * <p class="note">
     * This flag has a significant performance impact on low-RAM devices
     * since it may cause many media route providers to be started simultaneously.
     * It is much better to use {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} instead to avoid
     * performing passive discovery on these devices altogether.  Refer to
     * {@link #addCallback(MediaRouteSelector, Callback, int) addCallback} for details.
     * </p>
     *
     * @see MediaRouteDiscoveryFragment
     */
    public static final int CALLBACK_FLAG_FORCE_DISCOVERY = 1 << 3;

    /**
     * Flag for {@link #isRouteAvailable}: Ignore the default route.
     * <p>
     * This flag is used to determine whether a matching non-default route is available.
     * This constraint may be used to decide whether to offer the route chooser dialog
     * to the user.  There is no point offering the chooser if there are no
     * non-default choices.
     * </p>
     */
    public static final int AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE = 1 << 0;

    /**
     * Flag for {@link #isRouteAvailable}: Require an actual route to be matched.
     * <p>
     * If this flag is not set, then {@link #isRouteAvailable} will return true
     * if it is possible to discover a matching route even if discovery is not in
     * progress or if no matching route has yet been found.  This feature is used to
     * save resources by removing the need to perform passive route discovery on
     * {@link ActivityManager#isLowRamDevice low-RAM devices}.
     * </p><p>
     * If this flag is set, then {@link #isRouteAvailable} will only return true if
     * a matching route has actually been discovered.
     * </p>
     */
    public static final int AVAILABILITY_FLAG_REQUIRE_MATCH = 1 << 1;

    MediaRouter(Context context) {
        mContext = context;
    }

    /**
     * Gets an instance of the media router service associated with the context.
     * <p>
     * The application is responsible for holding a strong reference to the returned
     * {@link MediaRouter} instance, such as by storing the instance in a field of
     * the {@link android.app.Activity}, to ensure that the media router remains alive
     * as long as the application is using its features.
     * </p><p>
     * In other words, the support library only holds a {@link WeakReference weak reference}
     * to each media router instance.  When there are no remaining strong references to the
     * media router instance, all of its callbacks will be removed and route discovery
     * will no longer be performed on its behalf.
     * </p>
     *
     * @return The media router instance for the context.  The application must hold
     * a strong reference to this object as long as it is in use.
     */
    @NonNull
    public static MediaRouter getInstance(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        checkCallingThread();

        if (sGlobal == null) {
            sGlobal = new GlobalMediaRouter(context.getApplicationContext());
        }
        // Use sGlobal directly to avoid initialization.
        return sGlobal.getRouter(context);
    }

    /**
     * Gets the initialized global router.
     * Please make sure this is called in the main thread.
     */
    @MainThread
    static GlobalMediaRouter getGlobalRouter() {
        if (sGlobal == null) {
            return null;
        }
        sGlobal.ensureInitialized();
        return sGlobal;
    }

    /**
     * Gets information about the {@link MediaRouter.RouteInfo routes} currently known to
     * this media router.
     */
    @NonNull
    public List<RouteInfo> getRoutes() {
        checkCallingThread();
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? Collections.<RouteInfo>emptyList() :
                globalMediaRouter.getRoutes();
    }

    @Nullable
    RouteInfo getRoute(String uniqueId) {
        checkCallingThread();
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? null : globalMediaRouter.getRoute(uniqueId);
    }

    /**
     * Gets information about the {@link MediaRouter.ProviderInfo route providers}
     * currently known to this media router.
     */
    @NonNull
    public List<ProviderInfo> getProviders() {
        checkCallingThread();
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? Collections.<ProviderInfo>emptyList() :
                globalMediaRouter.getProviders();
    }

    /**
     * Gets the default route for playing media content on the system.
     * <p>
     * The system always provides a default route.
     * </p>
     *
     * @return The default route, which is guaranteed to never be null.
     */
    @NonNull
    public RouteInfo getDefaultRoute() {
        checkCallingThread();
        return getGlobalRouter().getDefaultRoute();
    }

    /**
     * Gets a bluetooth route for playing media content on the system.
     *
     * @return A bluetooth route, if exist, otherwise null.
     */
    @Nullable
    public RouteInfo getBluetoothRoute() {
        checkCallingThread();
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? null : globalMediaRouter.getBluetoothRoute();
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
    @NonNull
    public RouteInfo getSelectedRoute() {
        checkCallingThread();
        return getGlobalRouter().getSelectedRoute();
    }

    /**
     * Returns the selected route if it matches the specified selector, otherwise
     * selects the default route and returns it. If there is one live audio route
     * (usually Bluetooth A2DP), it will be selected instead of default route.
     *
     * @param selector The selector to match.
     * @return The previously selected route if it matched the selector, otherwise the
     * newly selected default route which is guaranteed to never be null.
     *
     * @see MediaRouteSelector
     * @see RouteInfo#matchesSelector
     */
    @NonNull
    public RouteInfo updateSelectedRoute(@NonNull MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "updateSelectedRoute: " + selector);
        }
        GlobalMediaRouter globalRouter = getGlobalRouter();
        RouteInfo route = globalRouter.getSelectedRoute();
        if (!route.isDefaultOrBluetooth() && !route.matchesSelector(selector)) {
            route = globalRouter.chooseFallbackRoute();
            globalRouter.selectRoute(route, MediaRouter.UNSELECT_REASON_ROUTE_CHANGED);
        }
        return route;
    }

    /**
     * Selects the specified route.
     *
     * @param route The route to select.
     */
    public void selectRoute(@NonNull RouteInfo route) {
        if (route == null) {
            throw new IllegalArgumentException("route must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "selectRoute: " + route);
        }
        getGlobalRouter().selectRoute(route, MediaRouter.UNSELECT_REASON_ROUTE_CHANGED);
    }

    /**
     * Unselects the current route and selects the default route instead.
     * <p>
     * The reason given must be one of:
     * <ul>
     * <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}</li>
     * <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}</li>
     * <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}</li>
     * <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}</li>
     * </ul>
     *
     * @param reason The reason for disconnecting the current route.
     */
    public void unselect(@UnselectReason int reason) {
        if (reason < MediaRouter.UNSELECT_REASON_UNKNOWN ||
                reason > MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
            throw new IllegalArgumentException("Unsupported reason to unselect route");
        }
        checkCallingThread();

        // Choose the fallback route if it's not already selected.
        // Otherwise, select the default route.
        GlobalMediaRouter globalRouter = getGlobalRouter();
        RouteInfo fallbackRoute = globalRouter.chooseFallbackRoute();
        if (globalRouter.getSelectedRoute() != fallbackRoute) {
            globalRouter.selectRoute(fallbackRoute, reason);
        }
    }

    /**
     * Adds the specified route as a member to the current dynamic group.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void addMemberToDynamicGroup(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().addMemberToDynamicGroup(route);
    }

    /**
     * Removes the specified route from the current dynamic group.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void removeMemberFromDynamicGroup(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().removeMemberFromDynamicGroup(route);
    }

    /**
     * Transfers the current dynamic group to the specified route.
     * @hide
     */
    @RestrictTo(LIBRARY)
    public void transferToRoute(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().transferToRoute(route);
    }

    /**
     * Returns true if there is a route that matches the specified selector.
     * <p>
     * This method returns true if there are any available routes that match the
     * selector regardless of whether they are enabled or disabled. If the
     * {@link #AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE} flag is specified, then
     * the method will only consider non-default routes.
     * </p>
     * <p class="note">
     * On {@link ActivityManager#isLowRamDevice low-RAM devices} this method
     * will return true if it is possible to discover a matching route even if
     * discovery is not in progress or if no matching route has yet been found.
     * Use {@link #AVAILABILITY_FLAG_REQUIRE_MATCH} to require an actual match.
     * </p>
     *
     * @param selector The selector to match.
     * @param flags Flags to control the determination of whether a route may be
     *            available. May be zero or some combination of
     *            {@link #AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE} and
     *            {@link #AVAILABILITY_FLAG_REQUIRE_MATCH}.
     * @return True if a matching route may be available.
     */
    public boolean isRouteAvailable(@NonNull MediaRouteSelector selector, int flags) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        checkCallingThread();
        return getGlobalRouter().isRouteAvailable(selector, flags);
    }

    /**
     * Registers a callback to discover routes that match the selector and to receive
     * events when they change.
     * <p>
     * This is a convenience method that has the same effect as calling
     * {@link #addCallback(MediaRouteSelector, Callback, int)} without flags.
     * </p>
     *
     * @param selector A route selector that indicates the kinds of routes that the
     * callback would like to discover.
     * @param callback The callback to add.
     * @see #removeCallback
     */
    public void addCallback(@NonNull MediaRouteSelector selector, @NonNull Callback callback) {
        addCallback(selector, callback, 0);
    }

    /**
     * Registers a callback to discover routes that match the selector and to receive
     * events when they change.
     * <p>
     * The selector describes the kinds of routes that the application wants to
     * discover.  For example, if the application wants to use
     * live audio routes then it should include the
     * {@link MediaControlIntent#CATEGORY_LIVE_AUDIO live audio media control intent category}
     * in its selector when it adds a callback to the media router.
     * The selector may include any number of categories.
     * </p><p>
     * If the callback has already been registered, then the selector is added to
     * the set of selectors being monitored by the callback.
     * </p><p>
     * By default, the callback will only be invoked for events that affect routes
     * that match the specified selector.  Event filtering may be disabled by specifying
     * the {@link #CALLBACK_FLAG_UNFILTERED_EVENTS} flag when the callback is registered.
     * </p><p>
     * Applications should use the {@link #isRouteAvailable} method to determine
     * whether is it possible to discover a route with the desired capabilities
     * and therefore whether the media route button should be shown to the user.
     * </p><p>
     * The {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} flag should be used while the application
     * is in the foreground to request that passive discovery be performed if there are
     * sufficient resources to allow continuous passive discovery.
     * On {@link ActivityManager#isLowRamDevice low-RAM devices} this flag will be
     * ignored to conserve resources.
     * </p><p>
     * The {@link #CALLBACK_FLAG_FORCE_DISCOVERY} flag should be used when
     * passive discovery absolutely must be performed, even on low-RAM devices.
     * This flag has a significant performance impact on low-RAM devices
     * since it may cause many media route providers to be started simultaneously.
     * It is much better to use {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} instead to avoid
     * performing passive discovery on these devices altogether.
     * </p><p>
     * The {@link #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} flag should be used when the
     * media route chooser dialog is showing to confirm the presence of available
     * routes that the user may connect to.  This flag may use substantially more
     * power. Once active scan is requested, it will be effective for 30 seconds and will be
     * suppressed after the delay. If you need active scan after this duration, you have to add
     * your callback again with the {@link #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} flag.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>
     * public class MyActivity extends Activity {
     *     private MediaRouter mRouter;
     *     private MediaRouter.Callback mCallback;
     *     private MediaRouteSelector mSelector;
     *
     *     // Add the callback on start to tell the media router what kinds of routes
     *     // the application is interested in so that it can get events about media routing changes
     *     // from the system.
     *     protected void onCreate(Bundle savedInstanceState) {
     *         super.onCreate(savedInstanceState);
     *
     *         mRouter = MediaRouter.getInstance(this);
     *         mCallback = new MyCallback();
     *         mSelector = new MediaRouteSelector.Builder()
     *                 .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
     *                 .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
     *                 .build();
     *         mRouter.addCallback(mSelector, mCallback, &#47;* flags= *&#47; 0);
     *     }
     *
     *     // Add the callback flag CALLBACK_FLAG_REQUEST_DISCOVERY on start by calling
     *     // addCallback() again so that the media router can try to discover suitable ones.
     *     public void onStart() {
     *         super.onStart();
     *
     *         mRouter.addCallback(mSelector, mCallback,
     *                 MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
     *
     *         MediaRouter.RouteInfo route = mRouter.updateSelectedRoute(mSelector);
     *         // do something with the route...
     *     }
     *
     *     // Remove the callback flag CALLBACK_FLAG_REQUEST_DISCOVERY on stop by calling
     *     // addCallback() again in order to tell the media router that it no longer
     *     // needs to invest effort trying to discover routes of these kinds for now.
     *     public void onStop() {
     *         mRouter.addCallback(mSelector, mCallback, &#47;* flags= *&#47; 0);
     *
     *         super.onStop();
     *     }
     *
     *     // Remove the callback when the activity is destroyed.
     *     public void onDestroy() {
     *         mRouter.removeCallback(mCallback);
     *
     *         super.onDestroy();
     *     }
     *
     *     private final class MyCallback extends MediaRouter.Callback {
     *         // Implement callback methods as needed.
     *     }
     * }
     * </pre>
     *
     * @param selector A route selector that indicates the kinds of routes that the
     * callback would like to discover.
     * @param callback The callback to add.
     * @param flags Flags to control the behavior of the callback.
     * May be zero or a combination of {@link #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} and
     * {@link #CALLBACK_FLAG_UNFILTERED_EVENTS}.
     * @see #removeCallback
     */
    // TODO: Change the usages of addCallback() for changing flags when setCallbackFlags() is added.
    public void addCallback(@NonNull MediaRouteSelector selector, @NonNull Callback callback,
            @CallbackFlags int flags) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "addCallback: selector=" + selector
                    + ", callback=" + callback + ", flags=" + Integer.toHexString(flags));
        }

        CallbackRecord record;
        int index = findCallbackRecord(callback);
        if (index < 0) {
            record = new CallbackRecord(this, callback);
            mCallbackRecords.add(record);
        } else {
            record = mCallbackRecords.get(index);
        }
        boolean updateNeeded = false;
        if (flags != record.mFlags) {
            record.mFlags = flags;
            updateNeeded = true;
        }
        long currentTime = SystemClock.elapsedRealtime();
        if ((flags & CALLBACK_FLAG_PERFORM_ACTIVE_SCAN) != 0) {
            // If the flag has active scan, the active scan might be suppressed previously if the
            // previous change is too long ago. In this case, the discovery request needs to be
            // updated so that the active scan state can be true again.
            updateNeeded = true;
        }
        record.mTimestamp = currentTime;

        if (!record.mSelector.contains(selector)) {
            record.mSelector = new MediaRouteSelector.Builder(record.mSelector)
                    .addSelector(selector)
                    .build();
            updateNeeded = true;
        }
        if (updateNeeded) {
            getGlobalRouter().updateDiscoveryRequest();
        }
    }

    /**
     * Removes the specified callback.  It will no longer receive events about
     * changes to media routes.
     *
     * @param callback The callback to remove.
     * @see #addCallback
     */
    public void removeCallback(@NonNull Callback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "removeCallback: callback=" + callback);
        }

        int index = findCallbackRecord(callback);
        if (index >= 0) {
            mCallbackRecords.remove(index);
            getGlobalRouter().updateDiscoveryRequest();
        }
    }

    private int findCallbackRecord(Callback callback) {
        final int count = mCallbackRecords.size();
        for (int i = 0; i < count; i++) {
            if (mCallbackRecords.get(i).mCallback == callback) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Sets a listener for receiving events when the selected route is about to be changed.
     */
    @MainThread
    public void setOnPrepareTransferListener(@Nullable OnPrepareTransferListener listener) {
        checkCallingThread();
        getGlobalRouter().mOnPrepareTransferListener = listener;
    }

    /**
     * Registers a media route provider within this application process.
     * <p>
     * The provider will be added to the list of providers that all {@link MediaRouter}
     * instances within this process can use to discover routes.
     * </p>
     *
     * @param providerInstance The media route provider instance to add.
     *
     * @see MediaRouteProvider
     * @see #removeCallback
     */
    public void addProvider(@NonNull MediaRouteProvider providerInstance) {
        if (providerInstance == null) {
            throw new IllegalArgumentException("providerInstance must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "addProvider: " + providerInstance);
        }
        getGlobalRouter().addProvider(providerInstance);
    }

    /**
     * Unregisters a media route provider within this application process.
     * <p>
     * The provider will be removed from the list of providers that all {@link MediaRouter}
     * instances within this process can use to discover routes.
     * </p>
     *
     * @param providerInstance The media route provider instance to remove.
     *
     * @see MediaRouteProvider
     * @see #addCallback
     */
    public void removeProvider(@NonNull MediaRouteProvider providerInstance) {
        if (providerInstance == null) {
            throw new IllegalArgumentException("providerInstance must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "removeProvider: " + providerInstance);
        }
        getGlobalRouter().removeProvider(providerInstance);
    }

    /**
     * Adds a remote control client to enable remote control of the volume
     * of the selected route.
     * <p>
     * The remote control client must have previously been registered with
     * the audio manager using the {@link android.media.AudioManager#registerRemoteControlClient
     * AudioManager.registerRemoteControlClient} method.
     * </p>
     *
     * @param remoteControlClient The {@link android.media.RemoteControlClient} to register.
     */
    public void addRemoteControlClient(@NonNull Object remoteControlClient) {
        if (remoteControlClient == null) {
            throw new IllegalArgumentException("remoteControlClient must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "addRemoteControlClient: " + remoteControlClient);
        }
        getGlobalRouter().addRemoteControlClient(remoteControlClient);
    }

    /**
     * Removes a remote control client.
     *
     * @param remoteControlClient The {@link android.media.RemoteControlClient}
     *            to unregister.
     */
    public void removeRemoteControlClient(@NonNull Object remoteControlClient) {
        if (remoteControlClient == null) {
            throw new IllegalArgumentException("remoteControlClient must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "removeRemoteControlClient: " + remoteControlClient);
        }
        getGlobalRouter().removeRemoteControlClient(remoteControlClient);
    }

    /**
     * Sets the media session to enable remote control of the volume of the
     * selected route. This should be used instead of
     * {@link #addRemoteControlClient} when using media sessions. Set the
     * session to null to clear it.
     *
     * @param mediaSession The {@link android.media.session.MediaSession} to use.
     */
    public void setMediaSession(@Nullable Object mediaSession) {
        checkCallingThread();
        if (DEBUG) {
            Log.d(TAG, "setMediaSession: " + mediaSession);
        }
        getGlobalRouter().setMediaSession(mediaSession);
    }

    /**
     * Sets a compat media session to enable remote control of the volume of the
     * selected route. This should be used instead of
     * {@link #addRemoteControlClient} when using {@link MediaSessionCompat}.
     * Set the session to null to clear it.
     *
     * @param mediaSession The {@link MediaSessionCompat} to use.
     */
    public void setMediaSessionCompat(@Nullable MediaSessionCompat mediaSession) {
        checkCallingThread();
        if (DEBUG) {
            Log.d(TAG, "setMediaSessionCompat: " + mediaSession);
        }
        getGlobalRouter().setMediaSessionCompat(mediaSession);
    }

    @Nullable
    public MediaSessionCompat.Token getMediaSessionToken() {
        return sGlobal == null ? null : sGlobal.getMediaSessionToken();
        // Use sGlobal exceptionally due to unchecked thread.
    }

    /**
     * Gets {@link MediaRouterParams parameters} of the media router service associated with this
     * media router.
     */
    @Nullable
    public MediaRouterParams getRouterParams() {
        checkCallingThread();
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? null : globalMediaRouter.getRouterParams();
    }

    /**
     * Sets {@link MediaRouterParams parameters} of the media router service associated with this
     * media router.
     *
     * @param params The parameter to set
     */
    public void setRouterParams(@Nullable MediaRouterParams params) {
        checkCallingThread();
        getGlobalRouter().setRouterParams(params);
    }

    /**
     * Ensures that calls into the media router are on the correct thread.
     */
    static void checkCallingThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("The media router service must only be "
                    + "accessed on the application's main thread.");
        }
    }

    /**
     * Returns whether the media transfer feature is enabled.
     *
     * @see MediaRouter
     * @hide
     */
    @RestrictTo(LIBRARY)
    public static boolean isMediaTransferEnabled() {
        if (sGlobal == null) {
            return false;
        }
        return getGlobalRouter().isMediaTransferEnabled();
    }

    /**
     * Returns how many {@link MediaRouter.Callback callbacks} are registered throughout the all
     * {@link MediaRouter media routers} in this process.
     */
    static int getGlobalCallbackCount() {
        if (sGlobal == null) {
            return 0;
        }
        return getGlobalRouter().getCallbackCount();
    }

    /**
     * Returns whether transferring media from remote to local is enabled.
     */
    static boolean isTransferToLocalEnabled() {
        GlobalMediaRouter globalMediaRouter = getGlobalRouter();
        return globalMediaRouter == null ? false : globalMediaRouter.isTransferToLocalEnabled();
    }

    /**
     * Provides information about a media route.
     * <p>
     * Each media route has a list of {@link MediaControlIntent media control}
     * {@link #getControlFilters intent filters} that describe the capabilities of the
     * route and the manner in which it is used and controlled.
     * </p>
     */
    public static class RouteInfo {
        private final ProviderInfo mProvider;
        final String mDescriptorId;
        final String mUniqueId;
        private String mName;
        private String mDescription;
        private Uri mIconUri;
        boolean mEnabled;
        private @ConnectionState int mConnectionState;
        private boolean mCanDisconnect;
        private final ArrayList<IntentFilter> mControlFilters = new ArrayList<>();
        private int mPlaybackType;
        private int mPlaybackStream;
        private @DeviceType int mDeviceType;
        private int mVolumeHandling;
        private int mVolume;
        private int mVolumeMax;
        private Display mPresentationDisplay;
        private int mPresentationDisplayId = PRESENTATION_DISPLAY_ID_NONE;
        private Bundle mExtras;
        private IntentSender mSettingsIntent;
        MediaRouteDescriptor mDescriptor;

        private List<RouteInfo> mMemberRoutes = new ArrayList<>();
        private Map<String, DynamicRouteDescriptor> mDynamicGroupDescriptors;

        @IntDef({CONNECTION_STATE_DISCONNECTED, CONNECTION_STATE_CONNECTING,
                CONNECTION_STATE_CONNECTED})
        @Retention(RetentionPolicy.SOURCE)
        private @interface ConnectionState {}

        /**
         * The default connection state indicating the route is disconnected.
         *
         * @see #getConnectionState
         */
        public static final int CONNECTION_STATE_DISCONNECTED = 0;

        /**
         * A connection state indicating the route is in the process of connecting and is not yet
         * ready for use.
         *
         * @see #getConnectionState
         */
        public static final int CONNECTION_STATE_CONNECTING = 1;

        /**
         * A connection state indicating the route is connected.
         *
         * @see #getConnectionState
         */
        public static final int CONNECTION_STATE_CONNECTED = 2;

        @IntDef({PLAYBACK_TYPE_LOCAL,PLAYBACK_TYPE_REMOTE})
        @Retention(RetentionPolicy.SOURCE)
        private @interface PlaybackType {}

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

        @IntDef({DEVICE_TYPE_UNKNOWN, DEVICE_TYPE_TV, DEVICE_TYPE_SPEAKER, DEVICE_TYPE_BLUETOOTH})
        @Retention(RetentionPolicy.SOURCE)
        private @interface DeviceType {}

        /**
         * The default receiver device type of the route indicating the type is unknown.
         *
         * @see #getDeviceType
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int DEVICE_TYPE_UNKNOWN = 0;

        /**
         * A receiver device type of the route indicating the presentation of the media is happening
         * on a TV.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_TV = 1;

        /**
         * A receiver device type of the route indicating the presentation of the media is happening
         * on a speaker.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_SPEAKER = 2;

        /**
         * A receiver device type of the route indicating the presentation of the media is happening
         * on a bluetooth device such as a bluetooth speaker.
         *
         * @see #getDeviceType
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int DEVICE_TYPE_BLUETOOTH = 3;

        @IntDef({PLAYBACK_VOLUME_FIXED,PLAYBACK_VOLUME_VARIABLE})
        @Retention(RetentionPolicy.SOURCE)
        private @interface PlaybackVolume {}

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

        /**
         * The default presentation display id indicating no presentation display is associated
         * with the route.
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final int PRESENTATION_DISPLAY_ID_NONE = -1;

        static final int CHANGE_GENERAL = 1 << 0;
        static final int CHANGE_VOLUME = 1 << 1;
        static final int CHANGE_PRESENTATION_DISPLAY = 1 << 2;

        // Should match to SystemMediaRouteProvider.PACKAGE_NAME.
        static final String SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME = "android";

        RouteInfo(ProviderInfo provider, String descriptorId, String uniqueId) {
            mProvider = provider;
            mDescriptorId = descriptorId;
            mUniqueId = uniqueId;
        }

        /**
         * Gets information about the provider of this media route.
         */
        @NonNull
        public ProviderInfo getProvider() {
            return mProvider;
        }

        /**
         * Gets the unique id of the route.
         * <p>
         * The route unique id functions as a stable identifier by which the route is known.
         * For example, an application can use this id as a token to remember the
         * selected route across restarts or to communicate its identity to a service.
         * </p>
         *
         * @return The unique id of the route, never null.
         */
        @NonNull
        public String getId() {
            return mUniqueId;
        }

        /**
         * Gets the user-visible name of the route.
         * <p>
         * The route name identifies the destination represented by the route.
         * It may be a user-supplied name, an alias, or device serial number.
         * </p>
         *
         * @return The user-visible name of a media route.  This is the string presented
         * to users who may select this as the active route.
         */
        @NonNull
        public String getName() {
            return mName;
        }

        /**
         * Gets the user-visible description of the route.
         * <p>
         * The route description describes the kind of destination represented by the route.
         * It may be a user-supplied string, a model number or brand of device.
         * </p>
         *
         * @return The description of the route, or null if none.
         */
        @Nullable
        public String getDescription() {
            return mDescription;
        }

        /**
         * Gets the URI of the icon representing this route.
         * <p>
         * This icon will be used in picker UIs if available.
         * </p>
         *
         * @return The URI of the icon representing this route, or null if none.
         */
        @Nullable
        public Uri getIconUri() {
            return mIconUri;
        }

        /**
         * Returns true if this route is enabled and may be selected.
         *
         * @return True if this route is enabled.
         */
        public boolean isEnabled() {
            return mEnabled;
        }

        /**
         * Returns true if the route is in the process of connecting and is not
         * yet ready for use.
         *
         * @return True if this route is in the process of connecting.
         * @deprecated use {@link #getConnectionState} instead.
         */
        @Deprecated
        public boolean isConnecting() {
            return mConnectionState == CONNECTION_STATE_CONNECTING;
        }

        /**
         * Gets the connection state of the route.
         *
         * @return The connection state of this route: {@link #CONNECTION_STATE_DISCONNECTED},
         * {@link #CONNECTION_STATE_CONNECTING}, or {@link #CONNECTION_STATE_CONNECTED}.
         */
        @ConnectionState
        public int getConnectionState() {
            return mConnectionState;
        }


        /**
         * Returns true if this route is currently selected.
         *
         * @return True if this route is currently selected.
         *
         * @see MediaRouter#getSelectedRoute
         */
        // Note: Only one representative route can return true. For instance:
        //   - If this route is a selected (non-group) route, it returns true.
        //   - If this route is a selected group route, it returns true.
        //   - If this route is a selected member route of a group, it returns false.
        public boolean isSelected() {
            checkCallingThread();
            return getGlobalRouter().getSelectedRoute() == this;
        }

        /**
         * Returns true if this route is the default route.
         *
         * @return True if this route is the default route.
         *
         * @see MediaRouter#getDefaultRoute
         */
        public boolean isDefault() {
            checkCallingThread();
            return getGlobalRouter().getDefaultRoute() == this;
        }

        /**
         * Returns true if this route is a bluetooth route.
         *
         * @return True if this route is a bluetooth route.
         *
         * @see MediaRouter#getBluetoothRoute
         */
        public boolean isBluetooth() {
            checkCallingThread();
            return getGlobalRouter().getBluetoothRoute() == this;
        }

        /**
         * Returns true if this route is the default route and the device speaker.
         *
         * @return True if this route is the default route and the device speaker.
         */
        public boolean isDeviceSpeaker() {
            int defaultAudioRouteNameResourceId = Resources.getSystem().getIdentifier(
                    "default_audio_route_name", "string", "android");
            return isDefault() && TextUtils.equals(
                    Resources.getSystem().getText(defaultAudioRouteNameResourceId), mName);
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
        @NonNull
        public List<IntentFilter> getControlFilters() {
            return mControlFilters;
        }

        /**
         * Returns true if the route supports at least one of the capabilities
         * described by a media route selector.
         *
         * @param selector The selector that specifies the capabilities to check.
         * @return True if the route supports at least one of the capabilities
         * described in the media route selector.
         */
        public boolean matchesSelector(@NonNull MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }
            checkCallingThread();
            return selector.matchesControlFilters(mControlFilters);
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
         * @return True if the route supports the specified intent category.
         *
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        public boolean supportsControlCategory(@NonNull String category) {
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
         * {@link MediaControlIntent media control} category and action.
         * <p>
         * Media control actions describe specific requests that an application
         * can ask a route to perform.
         * </p>
         *
         * @param category A {@link MediaControlIntent media control} category
         * such as {@link MediaControlIntent#CATEGORY_LIVE_AUDIO},
         * {@link MediaControlIntent#CATEGORY_LIVE_VIDEO},
         * {@link MediaControlIntent#CATEGORY_REMOTE_PLAYBACK}, or a provider-defined
         * media control category.
         * @param action A {@link MediaControlIntent media control} action
         * such as {@link MediaControlIntent#ACTION_PLAY}.
         * @return True if the route supports the specified intent action.
         *
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        public boolean supportsControlAction(@NonNull String category, @NonNull String action) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            if (action == null) {
                throw new IllegalArgumentException("action must not be null");
            }
            checkCallingThread();

            int count = mControlFilters.size();
            for (int i = 0; i < count; i++) {
                IntentFilter filter = mControlFilters.get(i);
                if (filter.hasCategory(category) && filter.hasAction(action)) {
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
         * actions such as starting remote playback of a media item.
         * </p>
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @return True if the route can handle the specified intent.
         *
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        public boolean supportsControlRequest(@NonNull Intent intent) {
            if (intent == null) {
                throw new IllegalArgumentException("intent must not be null");
            }
            checkCallingThread();

            ContentResolver contentResolver = getGlobalRouter().getContentResolver();
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
         * actions such as starting remote playback of a media item.
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
        public void sendControlRequest(@NonNull Intent intent,
                @Nullable ControlRequestCallback callback) {
            if (intent == null) {
                throw new IllegalArgumentException("intent must not be null");
            }
            checkCallingThread();

            getGlobalRouter().sendControlRequest(this, intent, callback);
        }

        /**
         * Gets the type of playback associated with this route.
         *
         * @return The type of playback associated with this route: {@link #PLAYBACK_TYPE_LOCAL}
         * or {@link #PLAYBACK_TYPE_REMOTE}.
         */
        @PlaybackType
        public int getPlaybackType() {
            return mPlaybackType;
        }

        /**
         * Gets the audio stream over which the playback associated with this route is performed.
         *
         * @return The stream over which the playback associated with this route is performed.
         */
        public int getPlaybackStream() {
            return mPlaybackStream;
        }

        /**
         * Gets the type of the receiver device associated with this route.
         *
         * @return The type of the receiver device associated with this route:
         * {@link #DEVICE_TYPE_TV} or {@link #DEVICE_TYPE_SPEAKER}.
         */
        public int getDeviceType() {
            return mDeviceType;
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isDefaultOrBluetooth() {
            if (isDefault() || mDeviceType == DEVICE_TYPE_BLUETOOTH) {
                return true;
            }
            // This is a workaround for platform version 23 or below where the system route
            // provider doesn't specify device type for bluetooth media routes.
            return isSystemMediaRouteProvider(this)
                    && supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    && !supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        /**
         * Returns {@code true} if the route is selectable.
         */
        boolean isSelectable() {
            // This tests whether the route is still valid and enabled.
            // The route descriptor field is set to null when the route is removed.
            return mDescriptor != null && mEnabled;
        }

        private static boolean isSystemMediaRouteProvider(MediaRouter.RouteInfo route) {
            return TextUtils.equals(route.getProviderInstance().getMetadata().getPackageName(),
                    SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME);
        }

        /**
         * Gets information about how volume is handled on the route.
         *
         * @return How volume is handled on the route: {@link #PLAYBACK_VOLUME_FIXED}
         * or {@link #PLAYBACK_VOLUME_VARIABLE}.
         */
        @PlaybackVolume
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
         * Gets whether this route supports disconnecting without interrupting
         * playback.
         *
         * @return True if this route can disconnect without stopping playback,
         *         false otherwise.
         */
        public boolean canDisconnect() {
            return mCanDisconnect;
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
            getGlobalRouter().requestSetVolume(this, Math.min(mVolumeMax, Math.max(0, volume)));
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
                getGlobalRouter().requestUpdateVolume(this, delta);
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
        @Nullable
        public Display getPresentationDisplay() {
            checkCallingThread();
            if (mPresentationDisplayId >= 0 && mPresentationDisplay == null) {
                mPresentationDisplay = getGlobalRouter().getDisplay(mPresentationDisplayId);
            }
            return mPresentationDisplay;
        }

        /**
         * Gets the route's presentation display id, or -1 if none.
         * @hide
         */
        @RestrictTo(LIBRARY)
        public int getPresentationDisplayId() {
            return mPresentationDisplayId;
        }

        /**
         * Gets a collection of extra properties about this route that were supplied
         * by its media route provider, or null if none.
         */
        @Nullable
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Gets an intent sender for launching a settings activity for this
         * route.
         */
        @Nullable
        public IntentSender getSettingsIntent() {
            return mSettingsIntent;
        }

        /**
         * Selects this media route.
         */
        public void select() {
            checkCallingThread();
            getGlobalRouter().selectRoute(this, MediaRouter.UNSELECT_REASON_ROUTE_CHANGED);
        }

        /**
         * Returns true if the route has one or more members
         * @hide
         */
        @RestrictTo(LIBRARY)
        public boolean isGroup() {
            return getMemberRoutes().size() >= 1;
        }

        /**
         * Gets the dynamic group state of the given route.
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Nullable
        public DynamicGroupState getDynamicGroupState(@NonNull RouteInfo route) {
            if (route == null) {
                throw new NullPointerException("route must not be null");
            }
            if (mDynamicGroupDescriptors != null
                    && mDynamicGroupDescriptors.containsKey(route.mUniqueId)) {
                return new DynamicGroupState(mDynamicGroupDescriptors.get(route.mUniqueId));
            }
            return null;
        }

        /**
         * Returns the routes in this group
         *
         * @hide
         * @return The list of the routes in this group
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public List<RouteInfo> getMemberRoutes() {
            return Collections.unmodifiableList(mMemberRoutes);
        }

        /**
         *
         * @hide
         */
        @RestrictTo(LIBRARY)
        @Nullable
        public DynamicGroupRouteController getDynamicGroupController() {
            checkCallingThread();
            //TODO: handle multiple controllers case
            RouteController controller = getGlobalRouter().mSelectedRouteController;
            if (controller instanceof DynamicGroupRouteController) {
                return (DynamicGroupRouteController) controller;
            }
            return null;
        }

        @Override
        @NonNull
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("MediaRouter.RouteInfo{ uniqueId=" + mUniqueId
                    + ", name=" + mName
                    + ", description=" + mDescription
                    + ", iconUri=" + mIconUri
                    + ", enabled=" + mEnabled
                    + ", connectionState=" + mConnectionState
                    + ", canDisconnect=" + mCanDisconnect
                    + ", playbackType=" + mPlaybackType
                    + ", playbackStream=" + mPlaybackStream
                    + ", deviceType=" + mDeviceType
                    + ", volumeHandling=" + mVolumeHandling
                    + ", volume=" + mVolume
                    + ", volumeMax=" + mVolumeMax
                    + ", presentationDisplayId=" + mPresentationDisplayId
                    + ", extras=" + mExtras
                    + ", settingsIntent=" + mSettingsIntent
                    + ", providerPackageName=" + mProvider.getPackageName());
            if (isGroup()) {
                sb.append(", members=[");
                final int count = mMemberRoutes.size();
                for (int i = 0; i < count; i++) {
                    if (i > 0) sb.append(", ");
                    if (mMemberRoutes.get(i) != this) {
                        sb.append(mMemberRoutes.get(i).getId());
                    }
                }
                sb.append(']');
            }
            sb.append(" }");
            return sb.toString();
        }

        int maybeUpdateDescriptor(MediaRouteDescriptor descriptor) {
            int changes = 0;
            if (mDescriptor != descriptor) {
                changes = updateDescriptor(descriptor);
            }
            return changes;
        }

        private boolean isSameControlFilters(List<IntentFilter> filters1,
                List<IntentFilter> filters2) {
            if (filters1 == filters2) {
                return true;
            }
            if (filters1 == null || filters2 == null) {
                return false;
            }

            ListIterator<IntentFilter> e1 = filters1.listIterator();
            ListIterator<IntentFilter> e2 = filters2.listIterator();

            while (e1.hasNext() && e2.hasNext()) {
                if (!isSameControlFilter(e1.next(), e2.next())) {
                    return false;
                }
            }
            return !(e1.hasNext() || e2.hasNext());
        }

        private boolean isSameControlFilter(IntentFilter filter1, IntentFilter filter2) {
            if (filter1 == filter2) {
                return true;
            }
            if (filter1 == null || filter2 == null) {
                return false;
            }
            // Check only actions and categories for a brief comparison
            int countActions = filter1.countActions();
            if (countActions != filter2.countActions()) {
                return false;
            }
            for (int i = 0; i < countActions; ++i) {
                if (!filter1.getAction(i).equals(filter2.getAction(i))) {
                    return false;
                }
            }
            int countCategories = filter1.countCategories();
            if (countCategories != filter2.countCategories()) {
                return false;
            }
            for (int i = 0; i < countCategories; ++i) {
                if (!filter1.getCategory(i).equals(filter2.getCategory(i))) {
                    return false;
                }
            }
            return true;
        }

        int updateDescriptor(MediaRouteDescriptor descriptor) {
            int changes = 0;
            mDescriptor = descriptor;
            if (descriptor != null) {
                if (!ObjectsCompat.equals(mName, descriptor.getName())) {
                    mName = descriptor.getName();
                    changes |= CHANGE_GENERAL;
                }
                if (!ObjectsCompat.equals(mDescription, descriptor.getDescription())) {
                    mDescription = descriptor.getDescription();
                    changes |= CHANGE_GENERAL;
                }
                if (!ObjectsCompat.equals(mIconUri, descriptor.getIconUri())) {
                    mIconUri = descriptor.getIconUri();
                    changes |= CHANGE_GENERAL;
                }
                if (mEnabled != descriptor.isEnabled()) {
                    mEnabled = descriptor.isEnabled();
                    changes |= CHANGE_GENERAL;
                }
                if (mConnectionState != descriptor.getConnectionState()) {
                    mConnectionState = descriptor.getConnectionState();
                    changes |= CHANGE_GENERAL;
                }
                // Use custom method to compare two control filters to confirm it is changed.
                if (!isSameControlFilters(mControlFilters, descriptor.getControlFilters())) {
                    mControlFilters.clear();
                    mControlFilters.addAll(descriptor.getControlFilters());
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
                if (mDeviceType != descriptor.getDeviceType()) {
                    mDeviceType = descriptor.getDeviceType();
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
                if (!ObjectsCompat.equals(mExtras, descriptor.getExtras())) {
                    mExtras = descriptor.getExtras();
                    changes |= CHANGE_GENERAL;
                }
                if (!ObjectsCompat.equals(mSettingsIntent, descriptor.getSettingsActivity())) {
                    mSettingsIntent = descriptor.getSettingsActivity();
                    changes |= CHANGE_GENERAL;
                }
                if (mCanDisconnect != descriptor.canDisconnectAndKeepPlaying()) {
                    mCanDisconnect = descriptor.canDisconnectAndKeepPlaying();
                    changes |= CHANGE_GENERAL | CHANGE_PRESENTATION_DISPLAY;
                }

                boolean memberChanged = false;

                List<String> groupMemberIds = descriptor.getGroupMemberIds();
                List<RouteInfo> routes = new ArrayList<>();
                if (groupMemberIds.size() != mMemberRoutes.size()) {
                    memberChanged = true;
                }
                //TODO: Clean this up not to reference the global router
                if (!groupMemberIds.isEmpty()) {
                    GlobalMediaRouter globalRouter = getGlobalRouter();
                    for (String groupMemberId : groupMemberIds) {
                        String uniqueId = globalRouter.getUniqueId(getProvider(), groupMemberId);
                        RouteInfo groupMember = globalRouter.getRoute(uniqueId);
                        if (groupMember != null) {
                            routes.add(groupMember);
                            if (!memberChanged && !mMemberRoutes.contains(groupMember)) {
                                memberChanged = true;
                            }
                        }
                    }
                }
                if (memberChanged) {
                    mMemberRoutes = routes;
                    changes |= CHANGE_GENERAL;
                }
            }
            return changes;
        }

        String getDescriptorId() {
            return mDescriptorId;
        }

        /** @hide */
        @RestrictTo(LIBRARY)
        @NonNull
        public MediaRouteProvider getProviderInstance() {
            return mProvider.getProviderInstance();
        }

        void updateDynamicDescriptors(Collection<DynamicRouteDescriptor> dynamicDescriptors) {
            mMemberRoutes.clear();
            if (mDynamicGroupDescriptors == null) {
                mDynamicGroupDescriptors = new ArrayMap<>();
            }
            mDynamicGroupDescriptors.clear();

            for (DynamicRouteDescriptor dynamicDescriptor : dynamicDescriptors) {
                RouteInfo route = findRouteByDynamicRouteDescriptor(dynamicDescriptor);
                if (route == null) {
                    continue;
                }
                mDynamicGroupDescriptors.put(route.mUniqueId, dynamicDescriptor);

                if ((dynamicDescriptor.getSelectionState() == DynamicRouteDescriptor.SELECTING)
                        || (dynamicDescriptor.getSelectionState()
                        == DynamicRouteDescriptor.SELECTED)) {
                    mMemberRoutes.add(route);
                }
            }
            getGlobalRouter().mCallbackHandler.post(
                    GlobalMediaRouter.CallbackHandler.MSG_ROUTE_CHANGED, this);
        }

        RouteInfo findRouteByDynamicRouteDescriptor(DynamicRouteDescriptor dynamicDescriptor) {
            String descriptorId = dynamicDescriptor.getRouteDescriptor().getId();
            return getProvider().findRouteByDescriptorId(descriptorId);
        }

        /**
         * Represents the dynamic group state of the {@link RouteInfo}.
         * @hide
         */
        @RestrictTo(LIBRARY)
        public static final class DynamicGroupState {
            final DynamicRouteDescriptor mDynamicDescriptor;

            DynamicGroupState(DynamicRouteDescriptor descriptor) {
                mDynamicDescriptor = descriptor;
            }
            /**
             * Gets the selection state of the route when the {@link MediaRouteProvider} of the
             * route supports
             * {@link MediaRouteProviderDescriptor#supportsDynamicGroupRoute() dynamic group}.
             *
             * @return The selection state of the route: {@link DynamicRouteDescriptor#UNSELECTED},
             * {@link DynamicRouteDescriptor#SELECTING}, or {@link DynamicRouteDescriptor#SELECTED}.
             * @hide
             */
            @RestrictTo(LIBRARY)
            public int getSelectionState() {
                return (mDynamicDescriptor != null) ? mDynamicDescriptor.getSelectionState()
                        : DynamicRouteDescriptor.UNSELECTED;
            }

            /**
             * @hide
             */
            @RestrictTo(LIBRARY)
            public boolean isUnselectable() {
                return mDynamicDescriptor == null || mDynamicDescriptor.isUnselectable();
            }

            /**
             * @hide
             */
            @RestrictTo(LIBRARY)
            public boolean isGroupable() {
                return mDynamicDescriptor != null && mDynamicDescriptor.isGroupable();
            }

            /**
             * @hide
             */
            @RestrictTo(LIBRARY)
            public boolean isTransferable() {
                return mDynamicDescriptor != null && mDynamicDescriptor.isTransferable();
            }
        }
    }

    /**
     * Provides information about a media route provider.
     * <p>
     * This object may be used to determine which media route provider has
     * published a particular route.
     * </p>
     */
    public static final class ProviderInfo {
        final MediaRouteProvider mProviderInstance;
        final List<RouteInfo> mRoutes = new ArrayList<>();

        private final ProviderMetadata mMetadata;
        private MediaRouteProviderDescriptor mDescriptor;

        ProviderInfo(MediaRouteProvider provider) {
            mProviderInstance = provider;
            mMetadata = provider.getMetadata();
        }

        /**
         * Gets the provider's underlying {@link MediaRouteProvider} instance.
         */
        @NonNull
        public MediaRouteProvider getProviderInstance() {
            checkCallingThread();
            return mProviderInstance;
        }

        /**
         * Gets the package name of the media route provider.
         */
        @NonNull
        public String getPackageName() {
            return mMetadata.getPackageName();
        }

        /**
         * Gets the component name of the media route provider.
         */
        @NonNull
        public ComponentName getComponentName() {
            return mMetadata.getComponentName();
        }

        /**
         * Gets the {@link MediaRouter.RouteInfo routes} published by this route provider.
         */
        @NonNull
        public List<RouteInfo> getRoutes() {
            checkCallingThread();
            return Collections.unmodifiableList(mRoutes);
        }

        boolean updateDescriptor(MediaRouteProviderDescriptor descriptor) {
            if (mDescriptor != descriptor) {
                mDescriptor = descriptor;
                return true;
            }
            return false;
        }

        int findRouteIndexByDescriptorId(String id) {
            final int count = mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (mRoutes.get(i).mDescriptorId.equals(id)) {
                    return i;
                }
            }
            return -1;
        }

        RouteInfo findRouteByDescriptorId(String id) {
            final int count = mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (mRoutes.get(i).mDescriptorId.equals(id)) {
                    return mRoutes.get(i);
                }
            }
            return null;
        }

        boolean supportsDynamicGroup() {
            return mDescriptor != null && mDescriptor.supportsDynamicGroupRoute();
        }

        @Override
        public String toString() {
            return "MediaRouter.RouteProviderInfo{ packageName=" + getPackageName()
                    + " }";
        }
    }

    /**
     * Interface for receiving events about media routing changes.
     * All methods of this interface will be called from the application's main thread.
     * <p>
     * A Callback will only receive events relevant to routes that the callback
     * was registered for unless the {@link MediaRouter#CALLBACK_FLAG_UNFILTERED_EVENTS}
     * flag was specified in {@link MediaRouter#addCallback(MediaRouteSelector, Callback, int)}.
     * </p>
     *
     * @see MediaRouter#addCallback(MediaRouteSelector, Callback, int)
     * @see MediaRouter#removeCallback(Callback)
     */
    public static abstract class Callback {
        /**
         * Called when the supplied media route becomes selected as the active route.
         *
         * @param router The media router reporting the event.
         * @param route The route that has been selected.
         * @deprecated Use {@link #onRouteSelected(MediaRouter, RouteInfo, int)} instead.
         */
        @Deprecated
        public void onRouteSelected(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when the supplied media route becomes selected as the active route.
         * <p>
         * The reason provided will be one of the following:
         * <ul>
         * <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}</li>
         * </ul>
         *
         * @param router The media router reporting the event.
         * @param route The route that has been selected.
         * @param reason The reason for unselecting the previous route.
         */
        public void onRouteSelected(@NonNull MediaRouter router, @NonNull RouteInfo route,
                @UnselectReason int reason) {
            onRouteSelected(router, route);
        }

        //TODO: Revise the comment when we have a feature that enables dynamic grouping on pre-R
        // devices.
        /**
         * Called when the supplied media route becomes selected as the active route, which
         * may be different from the route requested by {@link #selectRoute(RouteInfo)}.
         * That can happen when {@link MediaTransferReceiver media transfer feature} is enabled.
         * The default implementation calls {@link #onRouteSelected(MediaRouter, RouteInfo, int)}
         * with the actually selected route.
         *
         * @param router The media router reporting the event.
         * @param selectedRoute The route that has been selected.
         * @param reason The reason for unselecting the previous route.
         * @param requestedRoute The route that was requested to be selected.
         */
        public void onRouteSelected(@NonNull MediaRouter router,
                @NonNull RouteInfo selectedRoute, @UnselectReason int reason,
                @NonNull RouteInfo requestedRoute) {
            onRouteSelected(router, selectedRoute, reason);
        }

        /**
         * Called when the supplied media route becomes unselected as the active route.
         * For detailed reason, override {@link #onRouteUnselected(MediaRouter, RouteInfo, int)}
         * instead.
         *
         * @param router The media router reporting the event.
         * @param route The route that has been unselected.
         * @deprecated Use {@link #onRouteUnselected(MediaRouter, RouteInfo, int)} instead.
         */
        @Deprecated
        public void onRouteUnselected(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when the supplied media route becomes unselected as the active route.
         * The default implementation calls {@link #onRouteUnselected}.
         * <p>
         * The reason provided will be one of the following:
         * <ul>
         * <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}</li>
         * </ul>
         *
         * @param router The media router reporting the event.
         * @param route The route that has been unselected.
         * @param reason The reason for unselecting the route.
         */
        public void onRouteUnselected(@NonNull MediaRouter router, @NonNull RouteInfo route,
                @UnselectReason int reason) {
            onRouteUnselected(router, route);
        }

        /**
         * Called when a media route has been added.
         *
         * @param router The media router reporting the event.
         * @param route The route that has become available for use.
         */
        public void onRouteAdded(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when a media route has been removed.
         *
         * @param router The media router reporting the event.
         * @param route The route that has been removed from availability.
         */
        public void onRouteRemoved(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when a property of the indicated media route has changed.
         *
         * @param router The media router reporting the event.
         * @param route The route that was changed.
         */
        public void onRouteChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when a media route's volume changes.
         *
         * @param router The media router reporting the event.
         * @param route The route whose volume changed.
         */
        public void onRouteVolumeChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {
        }

        /**
         * Called when a media route's presentation display changes.
         * <p>
         * This method is called whenever the route's presentation display becomes
         * available, is removed or has changes to some of its properties (such as its size).
         * </p>
         *
         * @param router The media router reporting the event.
         * @param route The route whose presentation display changed.
         *
         * @see RouteInfo#getPresentationDisplay()
         */
        public void onRoutePresentationDisplayChanged(@NonNull MediaRouter router,
                @NonNull RouteInfo route) {
        }

        /**
         * Called when a media route provider has been added.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that has become available for use.
         */
        public void onProviderAdded(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
        }

        /**
         * Called when a media route provider has been removed.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that has been removed from availability.
         */
        public void onProviderRemoved(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
        }

        /**
         * Called when a property of the indicated media route provider has changed.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that was changed.
         */
        public void onProviderChanged(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {
        }

        /**
         * @hide
         */
        @RestrictTo(LIBRARY)
        public void onRouterParamsChanged(@NonNull MediaRouter router,
                @Nullable MediaRouterParams params) {
        }
    }

    /**
     * Listener for receiving events when the selected route is about to be changed.
     *
     * @see #setOnPrepareTransferListener(OnPrepareTransferListener)
     */
    public interface OnPrepareTransferListener {
        /**
         * Implement this to handle transfer seamlessly.
         * <p>
         * Setting the listener will defer stopping the previous route, from which you may
         * get the media status to resume media seamlessly on the new route.
         * When the transfer is prepared, set the returned future to stop media being played
         * on the previous route and release resources.
         * This method is called on the main thread.
         * <p>
         * {@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and
         * {@link Callback#onRouteSelected(MediaRouter, RouteInfo, int)} are called after
         * the future is done.
         *
         * @param fromRoute The route that is about to be unselected.
         * @param toRoute The route that is about to be selected.
         * @return A {@link ListenableFuture} whose completion indicates that the
         * transfer is prepared or {@code null} to indicate that no preparation is needed.
         * If a future is returned, until the future is completed,
         * the media continues to be played on the previous route.
         */
        @Nullable
        @MainThread
        ListenableFuture<Void> onPrepareTransfer(@NonNull RouteInfo fromRoute,
                @NonNull RouteInfo toRoute);
    }

    /**
     * Callback which is invoked with the result of a media control request.
     *
     * @see RouteInfo#sendControlRequest
     */
    public static abstract class ControlRequestCallback {
        /**
         * Called when a media control request succeeds.
         *
         * @param data Result data, or null if none.
         * Contents depend on the {@link MediaControlIntent media control action}.
         */
        public void onResult(@Nullable Bundle data) {
        }

        /**
         * Called when a media control request fails.
         *
         * @param error A localized error message which may be shown to the user, or null
         * if the cause of the error is unclear.
         * @param data Error data, or null if none.
         * Contents depend on the {@link MediaControlIntent media control action}.
         */
        public void onError(@Nullable String error, @Nullable Bundle data) {
        }
    }

    private static final class CallbackRecord {
        public final MediaRouter mRouter;
        public final Callback mCallback;
        public MediaRouteSelector mSelector;
        public int mFlags;
        public long mTimestamp;

        public CallbackRecord(MediaRouter router, Callback callback) {
            mRouter = router;
            mCallback = callback;
            mSelector = MediaRouteSelector.EMPTY;
        }

        public boolean filterRouteEvent(RouteInfo route, int what, RouteInfo optionalRoute,
                int reason) {
            if ((mFlags & CALLBACK_FLAG_UNFILTERED_EVENTS) != 0
                    || route.matchesSelector(mSelector)) {
                return true;
            }

            // In order to notify the app of cast-to-phone event, the onRouteSelected(phone)
            // should be called regaredless of the callbakck's control category.
            if (isTransferToLocalEnabled() && route.isDefaultOrBluetooth()
                    && what == GlobalMediaRouter.CallbackHandler.MSG_ROUTE_SELECTED
                    && reason == UNSELECT_REASON_ROUTE_CHANGED
                    && optionalRoute != null) {
                // Check the previously selected route is remote route.
                return !optionalRoute.isDefaultOrBluetooth();
            }

            return false;
        }
    }

    /**
     * Global state for the media router.
     * <p>
     * Media routes and media route providers are global to the process; their
     * state and the bulk of the media router implementation lives here.
     * </p>
     */
    static final class GlobalMediaRouter
            implements SystemMediaRouteProvider.SyncCallback,
            RegisteredMediaRouteProviderWatcher.Callback {
        final Context mApplicationContext;
        boolean mIsInitialized;

        SystemMediaRouteProvider mSystemProvider;
        @VisibleForTesting
        RegisteredMediaRouteProviderWatcher mRegisteredProviderWatcher;
        boolean mMediaTransferEnabled;
        MediaRoute2Provider mMr2Provider;

        final ArrayList<WeakReference<MediaRouter>> mRouters = new ArrayList<>();
        private final ArrayList<RouteInfo> mRoutes = new ArrayList<>();
        private final Map<Pair<String, String>, String> mUniqueIdMap = new HashMap<>();
        private final ArrayList<ProviderInfo> mProviders = new ArrayList<>();
        private final ArrayList<RemoteControlClientRecord> mRemoteControlClients =
                new ArrayList<>();
        final RemoteControlClientCompat.PlaybackInfo mPlaybackInfo =
                new RemoteControlClientCompat.PlaybackInfo();
        private final ProviderCallback mProviderCallback = new ProviderCallback();
        final CallbackHandler mCallbackHandler = new CallbackHandler();
        private DisplayManagerCompat mDisplayManager;
        private final boolean mLowRam;
        private MediaRouterActiveScanThrottlingHelper mActiveScanThrottlingHelper;

        private MediaRouterParams mRouterParams;
        RouteInfo mDefaultRoute;
        private RouteInfo mBluetoothRoute;
        RouteInfo mSelectedRoute;
        RouteController mSelectedRouteController;
        // Represents a route that are requested to be selected asynchronously.
        RouteInfo mRequestedRoute;
        RouteController mRequestedRouteController;
        // A map from unique route ID to RouteController for the member routes in the currently
        // selected route group.
        final Map<String, RouteController> mRouteControllerMap = new HashMap<>();
        private MediaRouteDiscoveryRequest mDiscoveryRequest;
        private MediaRouteDiscoveryRequest mDiscoveryRequestForMr2Provider;
        private int mCallbackCount;
        OnPrepareTransferListener mOnPrepareTransferListener;
        PrepareTransferNotifier mTransferNotifier;
        RouteInfo mTransferredRoute;
        RouteController mTransferredRouteController;

        private MediaSessionRecord mMediaSession;
        MediaSessionCompat mRccMediaSession;
        private MediaSessionCompat mCompatSession;
        private final MediaSessionCompat.OnActiveChangeListener mSessionActiveListener =
                new MediaSessionCompat.OnActiveChangeListener() {
            @Override
            public void onActiveChanged() {
                if(mRccMediaSession != null) {
                    if (mRccMediaSession.isActive()) {
                        addRemoteControlClient(mRccMediaSession.getRemoteControlClient());
                    } else {
                        removeRemoteControlClient(mRccMediaSession.getRemoteControlClient());
                    }
                }
            }
        };

        GlobalMediaRouter(Context applicationContext) {
            mApplicationContext = applicationContext;
            mLowRam = ActivityManagerCompat.isLowRamDevice(
                    (ActivityManager)applicationContext.getSystemService(
                            Context.ACTIVITY_SERVICE));
        }

        @SuppressLint({"NewApi", "SyntheticAccessor"})
        void ensureInitialized() {
            if (mIsInitialized) {
                return;
            }
            mIsInitialized = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                mMediaTransferEnabled = MediaTransferReceiver.isDeclared(mApplicationContext);
            } else {
                mMediaTransferEnabled = false;
            }

            if (mMediaTransferEnabled) {
                mMr2Provider = new MediaRoute2Provider(
                        mApplicationContext, new Mr2ProviderCallback());
            } else {
                mMr2Provider = null;
            }

            // Add the system media route provider for interoperating with
            // the framework media router.  This one is special and receives
            // synchronization messages from the media router.
            mSystemProvider = SystemMediaRouteProvider.obtain(mApplicationContext, this);
            start();
        }

        private void start() {
            // Using lambda would break some apps.
            mActiveScanThrottlingHelper = new MediaRouterActiveScanThrottlingHelper(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateDiscoveryRequest();
                        }
                    });
            addProvider(mSystemProvider);
            if (mMr2Provider != null) {
                addProvider(mMr2Provider);
            }

            // Start watching for routes published by registered media route
            // provider services.
            mRegisteredProviderWatcher = new RegisteredMediaRouteProviderWatcher(
                    mApplicationContext, this);
            mRegisteredProviderWatcher.start();
        }

        public MediaRouter getRouter(Context context) {
            MediaRouter router;
            for (int i = mRouters.size(); --i >= 0; ) {
                router = mRouters.get(i).get();
                if (router == null) {
                    mRouters.remove(i);
                } else if (router.mContext == context) {
                    return router;
                }
            }
            router = new MediaRouter(context);
            mRouters.add(new WeakReference<MediaRouter>(router));
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
            if (mDisplayManager == null) {
                mDisplayManager = DisplayManagerCompat.getInstance(mApplicationContext);
            }
            return mDisplayManager.getDisplay(displayId);
        }

        public void sendControlRequest(RouteInfo route,
                Intent intent, ControlRequestCallback callback) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                if (mSelectedRouteController.onControlRequest(intent, callback)) {
                    return;
                }
            }
            if (mTransferNotifier != null && route == mTransferNotifier.mToRoute
                    && mTransferNotifier.mToRouteController != null) {
                if (mTransferNotifier.mToRouteController.onControlRequest(intent, callback)) {
                    return;
                }
            }
            if (callback != null) {
                callback.onError(null, null);
            }
        }

        public void requestSetVolume(RouteInfo route, int volume) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                mSelectedRouteController.onSetVolume(volume);
            } else if (!mRouteControllerMap.isEmpty()) {
                RouteController controller = mRouteControllerMap.get(route.mUniqueId);
                if (controller != null) {
                    controller.onSetVolume(volume);
                }
            }
        }

        public void requestUpdateVolume(RouteInfo route, int delta) {
            if (route == mSelectedRoute && mSelectedRouteController != null) {
                mSelectedRouteController.onUpdateVolume(delta);
            } else if (!mRouteControllerMap.isEmpty()) {
                RouteController controller = mRouteControllerMap.get(route.mUniqueId);
                if (controller != null) {
                    controller.onUpdateVolume(delta);
                }
            }
        }

        public RouteInfo getRoute(String uniqueId) {
            for (RouteInfo info : mRoutes) {
                if (info.mUniqueId.equals(uniqueId)) {
                    return info;
                }
            }
            return null;
        }

        public List<RouteInfo> getRoutes() {
            return mRoutes;
        }

        @Nullable
        MediaRouterParams getRouterParams() {
            return mRouterParams;
        }

        void setRouterParams(@Nullable MediaRouterParams params) {
            MediaRouterParams oldParams = mRouterParams;
            mRouterParams = params;

            if (isMediaTransferEnabled()) {
                boolean oldTransferToLocalEnabled = oldParams == null ? false :
                        oldParams.isTransferToLocalEnabled();
                boolean newTransferToLocalEnabled = params == null ? false :
                        params.isTransferToLocalEnabled();

                if (oldTransferToLocalEnabled != newTransferToLocalEnabled) {
                    // Since the discovery request itself is not changed,
                    // call setDiscoveryRequestInternal to avoid the equality check.
                    mMr2Provider.setDiscoveryRequestInternal(mDiscoveryRequestForMr2Provider);
                }
            }
            mCallbackHandler.post(CallbackHandler.MSG_ROUTER_PARAMS_CHANGED, params);
        }

        @Nullable
        List<ProviderInfo> getProviders() {
            return mProviders;
        }

        @NonNull RouteInfo getDefaultRoute() {
            if (mDefaultRoute == null) {
                // This should never happen once the media router has been fully
                // initialized but it is good to check for the error in case there
                // is a bug in provider initialization.
                throw new IllegalStateException("There is no default route.  "
                        + "The media router has not yet been fully initialized.");
            }
            return mDefaultRoute;
        }

        RouteInfo getBluetoothRoute() {
            return mBluetoothRoute;
        }

        @NonNull RouteInfo getSelectedRoute() {
            if (mSelectedRoute == null) {
                // This should never happen once the media router has been fully
                // initialized but it is good to check for the error in case there
                // is a bug in provider initialization.
                throw new IllegalStateException("There is no currently selected route.  "
                        + "The media router has not yet been fully initialized.");
            }
            return mSelectedRoute;
        }

        @Nullable
        RouteInfo.DynamicGroupState getDynamicGroupState(RouteInfo route) {
            return mSelectedRoute.getDynamicGroupState(route);
        }

        void addMemberToDynamicGroup(@NonNull RouteInfo route) {
            if (!(mSelectedRouteController instanceof DynamicGroupRouteController)) {
                throw new IllegalStateException("There is no currently selected "
                        + "dynamic group route.");
            }
            RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
            if (mSelectedRoute.getMemberRoutes().contains(route)
                    || state == null || !state.isGroupable()) {
                Log.w(TAG, "Ignoring attempt to add a non-groupable route to dynamic group : "
                        + route);
                return;
            }
            ((DynamicGroupRouteController) mSelectedRouteController)
                    .onAddMemberRoute(route.getDescriptorId());
        }

        void removeMemberFromDynamicGroup(@NonNull RouteInfo route) {
            if (!(mSelectedRouteController instanceof DynamicGroupRouteController)) {
                throw new IllegalStateException("There is no currently selected "
                        + "dynamic group route.");
            }
            RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
            if (!mSelectedRoute.getMemberRoutes().contains(route)
                    || state == null || !state.isUnselectable()) {
                Log.w(TAG, "Ignoring attempt to remove a non-unselectable member route : "
                        + route);
                return;
            }
            if (mSelectedRoute.getMemberRoutes().size() <= 1) {
                Log.w(TAG, "Ignoring attempt to remove the last member route.");
                return;
            }
            ((DynamicGroupRouteController) mSelectedRouteController)
                    .onRemoveMemberRoute(route.getDescriptorId());
        }

        void transferToRoute(@NonNull RouteInfo route) {
            if (!(mSelectedRouteController instanceof DynamicGroupRouteController)) {
                throw new IllegalStateException("There is no currently selected dynamic group "
                        + "route.");
            }
            RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
            if (state == null || !state.isTransferable()) {
                Log.w(TAG, "Ignoring attempt to transfer to a non-transferable route.");
                return;
            }
            ((DynamicGroupRouteController) mSelectedRouteController)
                    .onUpdateMemberRoutes(Collections.singletonList(route.getDescriptorId()));
        }

        void selectRoute(@NonNull RouteInfo route, @UnselectReason int unselectReason) {
            if (!mRoutes.contains(route)) {
                Log.w(TAG, "Ignoring attempt to select removed route: " + route);
                return;
            }
            if (!route.mEnabled) {
                Log.w(TAG, "Ignoring attempt to select disabled route: " + route);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                    && route.getProviderInstance() == mMr2Provider && mSelectedRoute != route) {
                // Asynchronously select the route
                mMr2Provider.transferTo(route.getDescriptorId());
                return;
            }
            selectRouteInternal(route, unselectReason);
        }

        public boolean isRouteAvailable(MediaRouteSelector selector, int flags) {
            if (selector.isEmpty()) {
                return false;
            }

            // On low-RAM devices, do not rely on actual discovery results unless asked to.
            if ((flags & AVAILABILITY_FLAG_REQUIRE_MATCH) == 0 && mLowRam) {
                return true;
            }

            // Check whether any existing routes match the selector.
            final int routeCount = mRoutes.size();
            for (int i = 0; i < routeCount; i++) {
                RouteInfo route = mRoutes.get(i);
                if ((flags & AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE) != 0
                        && route.isDefaultOrBluetooth()) {
                    continue;
                }
                if (route.matchesSelector(selector)) {
                    return true;
                }
            }

            // It doesn't look like we can find a matching route right now.
            return false;
        }

        public void updateDiscoveryRequest() {
            // Combine all of the callback selectors and active scan flags.
            boolean discover = false;
            MediaRouteSelector.Builder builder = new MediaRouteSelector.Builder();
            mActiveScanThrottlingHelper.reset();

            int callbackCount = 0;
            for (int i = mRouters.size(); --i >= 0; ) {
                MediaRouter router = mRouters.get(i).get();
                if (router == null) {
                    mRouters.remove(i);
                } else {
                    final int count = router.mCallbackRecords.size();
                    callbackCount += count;
                    for (int j = 0; j < count; j++) {
                        CallbackRecord callback = router.mCallbackRecords.get(j);
                        builder.addSelector(callback.mSelector);
                        boolean callbackRequestingActiveScan =
                                (callback.mFlags & CALLBACK_FLAG_PERFORM_ACTIVE_SCAN) != 0;
                        mActiveScanThrottlingHelper.requestActiveScan(
                                callbackRequestingActiveScan,
                                callback.mTimestamp);
                        if (callbackRequestingActiveScan) {
                            discover = true; // perform active scan implies request discovery
                        }
                        if ((callback.mFlags & CALLBACK_FLAG_REQUEST_DISCOVERY) != 0) {
                            if (!mLowRam) {
                                discover = true;
                            }
                        }
                        if ((callback.mFlags & CALLBACK_FLAG_FORCE_DISCOVERY) != 0) {
                            discover = true;
                        }
                    }
                }
            }

            boolean activeScan =
                    mActiveScanThrottlingHelper
                    .finalizeActiveScanAndScheduleSuppressActiveScanRunnable();

            mCallbackCount = callbackCount;
            MediaRouteSelector selector = discover ? builder.build() : MediaRouteSelector.EMPTY;

            // MediaRoute2Provider should keep registering discovery preference
            // even when the callback flag is zero.
            updateMr2ProviderDiscoveryRequest(builder.build(), activeScan);

            // Create a new discovery request.
            if (mDiscoveryRequest != null
                    && mDiscoveryRequest.getSelector().equals(selector)
                    && mDiscoveryRequest.isActiveScan() == activeScan) {
                return; // no change
            }
            if (selector.isEmpty() && !activeScan) {
                // Discovery is not needed.
                if (mDiscoveryRequest == null) {
                    return; // no change
                }
                mDiscoveryRequest = null;
            } else {
                // Discovery is needed.
                mDiscoveryRequest = new MediaRouteDiscoveryRequest(selector, activeScan);
            }
            if (DEBUG) {
                Log.d(TAG, "Updated discovery request: " + mDiscoveryRequest);
            }
            if (discover && !activeScan && mLowRam) {
                Log.i(TAG, "Forcing passive route discovery on a low-RAM device, "
                        + "system performance may be affected.  Please consider using "
                        + "CALLBACK_FLAG_REQUEST_DISCOVERY instead of "
                        + "CALLBACK_FLAG_FORCE_DISCOVERY.");
            }

            // Notify providers.
            final int providerCount = mProviders.size();
            for (int i = 0; i < providerCount; i++) {
                MediaRouteProvider provider = mProviders.get(i).mProviderInstance;
                if (provider == mMr2Provider) {
                    // MediaRoute2Provider is handled by updateMr2ProviderDiscoveryRequest().
                    continue;
                }
                provider.setDiscoveryRequest(mDiscoveryRequest);
            }
        }

        private void updateMr2ProviderDiscoveryRequest(@NonNull MediaRouteSelector selector,
                boolean activeScan) {
            if (!isMediaTransferEnabled()) {
                return;
            }

            if (mDiscoveryRequestForMr2Provider != null
                    && mDiscoveryRequestForMr2Provider.getSelector().equals(selector)
                    && mDiscoveryRequestForMr2Provider.isActiveScan() == activeScan) {
                return; // no change
            }
            if (selector.isEmpty() && !activeScan) {
                // Discovery is not needed.
                if (mDiscoveryRequestForMr2Provider == null) {
                    return; // no change
                }
                mDiscoveryRequestForMr2Provider = null;
            } else {
                // Discovery is needed.
                mDiscoveryRequestForMr2Provider =
                        new MediaRouteDiscoveryRequest(selector, activeScan);
            }
            if (DEBUG) {
                Log.d(TAG, "Updated MediaRoute2Provider's discovery request: "
                        + mDiscoveryRequestForMr2Provider);
            }

            mMr2Provider.setDiscoveryRequest(mDiscoveryRequestForMr2Provider);
        }

        int getCallbackCount() {
            return mCallbackCount;
        }

        boolean isMediaTransferEnabled() {
            return mMediaTransferEnabled;
        }

        boolean isTransferToLocalEnabled() {
            if (mRouterParams == null) {
                return false;
            }
            return mRouterParams.isTransferToLocalEnabled();
        }

        @Override
        public void addProvider(@NonNull MediaRouteProvider providerInstance) {
            if (findProviderInfo(providerInstance) == null) {
                // 1. Add the provider to the list.
                ProviderInfo provider = new ProviderInfo(providerInstance);
                mProviders.add(provider);
                if (DEBUG) {
                    Log.d(TAG, "Provider added: " + provider);
                }
                mCallbackHandler.post(CallbackHandler.MSG_PROVIDER_ADDED, provider);
                // 2. Create the provider's contents.
                updateProviderContents(provider, providerInstance.getDescriptor());
                // 3. Register the provider callback.
                providerInstance.setCallback(mProviderCallback);
                // 4. Set the discovery request.
                providerInstance.setDiscoveryRequest(mDiscoveryRequest);
            }
        }

        @Override
        public void removeProvider(MediaRouteProvider providerInstance) {
            ProviderInfo provider = findProviderInfo(providerInstance);
            if (provider != null) {
                // 1. Unregister the provider callback.
                providerInstance.setCallback(null);
                // 2. Clear the discovery request.
                providerInstance.setDiscoveryRequest(null);
                // 3. Delete the provider's contents.
                updateProviderContents(provider, null);
                // 4. Remove the provider from the list.
                if (DEBUG) {
                    Log.d(TAG, "Provider removed: " + provider);
                }
                mCallbackHandler.post(CallbackHandler.MSG_PROVIDER_REMOVED, provider);
                mProviders.remove(provider);
            }
        }

        @Override
        public void releaseProviderController(@NonNull RegisteredMediaRouteProvider provider,
                @NonNull RouteController controller) {
            if (mSelectedRouteController == controller) {
                selectRoute(chooseFallbackRoute(), UNSELECT_REASON_STOPPED);
            }
            //TODO: Maybe release a member route controller if the given controller is a member of
            // the selected route.
        }

        void updateProviderDescriptor(MediaRouteProvider providerInstance,
                MediaRouteProviderDescriptor descriptor) {
            ProviderInfo provider = findProviderInfo(providerInstance);
            if (provider != null) {
                // Update the provider's contents.
                updateProviderContents(provider, descriptor);
            }
        }

        private ProviderInfo findProviderInfo(MediaRouteProvider providerInstance) {
            final int count = mProviders.size();
            for (int i = 0; i < count; i++) {
                if (mProviders.get(i).mProviderInstance == providerInstance) {
                    return mProviders.get(i);
                }
            }
            return null;
        }

        private void updateProviderContents(ProviderInfo provider,
                MediaRouteProviderDescriptor providerDescriptor) {
            if (!provider.updateDescriptor(providerDescriptor)) {
                // Nothing to update.
                return;
            }
            // Update all existing routes and reorder them to match
            // the order of their descriptors.
            int targetIndex = 0;
            boolean selectedRouteDescriptorChanged = false;
            if (providerDescriptor != null && (providerDescriptor.isValid()
                    || providerDescriptor == mSystemProvider.getDescriptor())) {
                final List<MediaRouteDescriptor> routeDescriptors = providerDescriptor.getRoutes();
                // Updating route group's contents requires all member routes' information.
                // Add the groups to the lists and update them later.
                List<Pair<RouteInfo, MediaRouteDescriptor>> addedGroups = new ArrayList<>();
                List<Pair<RouteInfo, MediaRouteDescriptor>> updatedGroups = new ArrayList<>();
                for (MediaRouteDescriptor routeDescriptor : routeDescriptors) {
                    // SystemMediaRouteProvider may have invalid routes
                    if (routeDescriptor == null || !routeDescriptor.isValid()) {
                        Log.w(TAG, "Ignoring invalid system route descriptor: "
                                + routeDescriptor);
                        continue;
                    }
                    final String id = routeDescriptor.getId();
                    final int sourceIndex = provider.findRouteIndexByDescriptorId(id);

                    if (sourceIndex < 0) {
                        // 1. Add the route to the list.
                        String uniqueId = assignRouteUniqueId(provider, id);
                        RouteInfo route = new RouteInfo(provider, id, uniqueId);

                        provider.mRoutes.add(targetIndex++, route);
                        mRoutes.add(route);
                        // 2. Create the route's contents.
                        if (routeDescriptor.getGroupMemberIds().size() > 0) {
                            addedGroups.add(new Pair<>(route, routeDescriptor));
                        } else {
                            route.maybeUpdateDescriptor(routeDescriptor);
                            // 3. Notify clients about addition.
                            if (DEBUG) {
                                Log.d(TAG, "Route added: " + route);
                            }
                            mCallbackHandler.post(CallbackHandler.MSG_ROUTE_ADDED, route);
                        }
                    } else if (sourceIndex < targetIndex) {
                        Log.w(TAG, "Ignoring route descriptor with duplicate id: "
                                + routeDescriptor);
                    } else {
                        RouteInfo route = provider.mRoutes.get(sourceIndex);
                        // 1. Reorder the route within the list.
                        Collections.swap(provider.mRoutes, sourceIndex, targetIndex++);
                        // 2. Update the route's contents.
                        if (routeDescriptor.getGroupMemberIds().size() > 0) {
                            updatedGroups.add(new Pair<>(route, routeDescriptor));
                        } else {
                            // 3. Notify clients about changes.
                            if (updateRouteDescriptorAndNotify(route, routeDescriptor) != 0) {
                                if (route == mSelectedRoute) {
                                    selectedRouteDescriptorChanged = true;
                                }
                            }
                        }
                    }
                }
                // Update the new and/or existing groups.
                for (Pair<RouteInfo, MediaRouteDescriptor> pair : addedGroups) {
                    RouteInfo route = pair.first;
                    route.maybeUpdateDescriptor(pair.second);
                    if (DEBUG) {
                        Log.d(TAG, "Route added: " + route);
                    }
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_ADDED, route);
                }
                for (Pair<RouteInfo, MediaRouteDescriptor> pair : updatedGroups) {
                    RouteInfo route = pair.first;
                    if (updateRouteDescriptorAndNotify(route, pair.second) != 0) {
                        if (route == mSelectedRoute) {
                            selectedRouteDescriptorChanged = true;
                        }
                    }
                }
            } else {
                Log.w(TAG, "Ignoring invalid provider descriptor: " + providerDescriptor);
            }

            // Dispose all remaining routes that do not have matching descriptors.
            for (int i = provider.mRoutes.size() - 1; i >= targetIndex; i--) {
                // 1. Delete the route's contents.
                RouteInfo route = provider.mRoutes.get(i);
                route.maybeUpdateDescriptor(null);
                // 2. Remove the route from the list.
                mRoutes.remove(route);
            }

            // Update the selected route if needed.
            updateSelectedRouteIfNeeded(selectedRouteDescriptorChanged);

            // Now notify clients about routes that were removed.
            // We do this after updating the selected route to ensure
            // that the framework media router observes the new route
            // selection before the removal since removing the currently
            // selected route may have side-effects.
            for (int i = provider.mRoutes.size() - 1; i >= targetIndex; i--) {
                RouteInfo route = provider.mRoutes.remove(i);
                if (DEBUG) {
                    Log.d(TAG, "Route removed: " + route);
                }
                mCallbackHandler.post(CallbackHandler.MSG_ROUTE_REMOVED, route);
            }

            // Notify provider changed.
            if (DEBUG) {
                Log.d(TAG, "Provider changed: " + provider);
            }
            mCallbackHandler.post(CallbackHandler.MSG_PROVIDER_CHANGED, provider);
        }

        int updateRouteDescriptorAndNotify(RouteInfo route,
                MediaRouteDescriptor routeDescriptor) {
            int changes = route.maybeUpdateDescriptor(routeDescriptor);
            if (changes != 0) {
                if ((changes & RouteInfo.CHANGE_GENERAL) != 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Route changed: " + route);
                    }
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_CHANGED, route);
                }
                if ((changes & RouteInfo.CHANGE_VOLUME) != 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Route volume changed: " + route);
                    }
                    mCallbackHandler.post(CallbackHandler.MSG_ROUTE_VOLUME_CHANGED, route);
                }
                if ((changes & RouteInfo.CHANGE_PRESENTATION_DISPLAY) != 0) {
                    if (DEBUG) {
                        Log.d(TAG, "Route presentation display changed: "
                                + route);
                    }
                    mCallbackHandler.post(CallbackHandler.
                            MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED, route);
                }
            }
            return changes;
        }

        String assignRouteUniqueId(ProviderInfo provider, String routeDescriptorId) {
            // Although route descriptor ids are unique within a provider, it's
            // possible for there to be two providers with the same package name.
            // Therefore we must dedupe the composite id.
            String componentName = provider.getComponentName().flattenToShortString();
            String uniqueId = componentName + ":" + routeDescriptorId;
            if (findRouteByUniqueId(uniqueId) < 0) {
                mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), uniqueId);
                return uniqueId;
            }
            Log.w(TAG, "Either " + routeDescriptorId + " isn't unique in " + componentName
                    + " or we're trying to assign a unique ID for an already added route");
            for (int i = 2; ; i++) {
                String newUniqueId = String.format(Locale.US, "%s_%d", uniqueId, i);
                if (findRouteByUniqueId(newUniqueId) < 0) {
                    mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), newUniqueId);
                    return newUniqueId;
                }
            }
        }

        private int findRouteByUniqueId(String uniqueId) {
            final int count = mRoutes.size();
            for (int i = 0; i < count; i++) {
                if (mRoutes.get(i).mUniqueId.equals(uniqueId)) {
                    return i;
                }
            }
            return -1;
        }

        String getUniqueId(ProviderInfo provider, String routeDescriptorId) {
            String componentName = provider.getComponentName().flattenToShortString();
            return mUniqueIdMap.get(new Pair<>(componentName, routeDescriptorId));
        }

        void updateSelectedRouteIfNeeded(boolean selectedRouteDescriptorChanged) {
            // Update default route.
            if (mDefaultRoute != null && !mDefaultRoute.isSelectable()) {
                Log.i(TAG, "Clearing the default route because it "
                        + "is no longer selectable: " + mDefaultRoute);
                mDefaultRoute = null;
            }
            if (mDefaultRoute == null && !mRoutes.isEmpty()) {
                for (RouteInfo route : mRoutes) {
                    if (isSystemDefaultRoute(route) && route.isSelectable()) {
                        mDefaultRoute = route;
                        Log.i(TAG, "Found default route: " + mDefaultRoute);
                        break;
                    }
                }
            }

            // Update bluetooth route.
            if (mBluetoothRoute != null && !mBluetoothRoute.isSelectable()) {
                Log.i(TAG, "Clearing the bluetooth route because it "
                        + "is no longer selectable: " + mBluetoothRoute);
                mBluetoothRoute = null;
            }
            if (mBluetoothRoute == null && !mRoutes.isEmpty()) {
                for (RouteInfo route : mRoutes) {
                    if (isSystemLiveAudioOnlyRoute(route) && route.isSelectable()) {
                        mBluetoothRoute = route;
                        Log.i(TAG, "Found bluetooth route: " + mBluetoothRoute);
                        break;
                    }
                }
            }

            // Update selected route.
            if (mSelectedRoute == null || !mSelectedRoute.isEnabled()) {
                Log.i(TAG, "Unselecting the current route because it "
                        + "is no longer selectable: " + mSelectedRoute);
                selectRouteInternal(chooseFallbackRoute(),
                        MediaRouter.UNSELECT_REASON_UNKNOWN);
            } else if (selectedRouteDescriptorChanged) {
                // In case the selected route is a route group, select/unselect route controllers
                // for the added/removed route members.
                maybeUpdateMemberRouteControllers();
                updatePlaybackInfoFromSelectedRoute();
            }
        }

        RouteInfo chooseFallbackRoute() {
            // When the current route is removed or no longer selectable,
            // we want to revert to a live audio route if there is
            // one (usually Bluetooth A2DP).  Failing that, use
            // the default route.
            for (RouteInfo route : mRoutes) {
                if (route != mDefaultRoute
                        && isSystemLiveAudioOnlyRoute(route)
                        && route.isSelectable()) {
                    return route;
                }
            }
            return mDefaultRoute;
        }

        private boolean isSystemLiveAudioOnlyRoute(RouteInfo route) {
            return route.getProviderInstance() == mSystemProvider
                    && route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                    && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
        }

        private boolean isSystemDefaultRoute(RouteInfo route) {
            return route.getProviderInstance() == mSystemProvider
                    && route.mDescriptorId.equals(
                            SystemMediaRouteProvider.DEFAULT_ROUTE_ID);
        }

        void selectRouteInternal(@NonNull RouteInfo route,
                @UnselectReason int unselectReason) {
            // TODO: Remove the following logging when no longer needed.
            if (sGlobal == null || (mBluetoothRoute != null && route.isDefault())) {
                final StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
                StringBuilder sb = new StringBuilder();
                // callStack[3] is the caller of this method.
                for (int i = 3; i < callStack.length; i++) {
                    StackTraceElement caller = callStack[i];
                    sb.append(caller.getClassName())
                            .append(".")
                            .append(caller.getMethodName())
                            .append(":")
                            .append(caller.getLineNumber())
                            .append("  ");
                }
                if (sGlobal == null) {
                    Log.w(TAG, "setSelectedRouteInternal is called while sGlobal is null: pkgName="
                            + mApplicationContext.getPackageName() + ", callers=" + sb.toString());
                } else {
                    Log.w(TAG, "Default route is selected while a BT route is available: pkgName="
                            + mApplicationContext.getPackageName() + ", callers=" + sb.toString());
                }
            }

            if (mSelectedRoute == route) {
                return;
            }

            // Cancel the previous asynchronous select if exists.
            if (mRequestedRoute != null) {
                mRequestedRoute = null;
                if (mRequestedRouteController != null) {
                    mRequestedRouteController.onUnselect(UNSELECT_REASON_ROUTE_CHANGED);
                    mRequestedRouteController.onRelease();
                    mRequestedRouteController = null;
                }
            }

            //TODO: determine how to enable dynamic grouping on pre-R devices.
            if (isMediaTransferEnabled() && route.getProvider().supportsDynamicGroup()) {
                MediaRouteProvider.DynamicGroupRouteController dynamicGroupRouteController =
                        route.getProviderInstance().onCreateDynamicGroupRouteController(
                                route.mDescriptorId);
                // Select route asynchronously.
                if (dynamicGroupRouteController != null) {
                    dynamicGroupRouteController.setOnDynamicRoutesChangedListener(
                            ContextCompat.getMainExecutor(mApplicationContext),
                            mDynamicRoutesListener);
                    mRequestedRoute = route;
                    mRequestedRouteController = dynamicGroupRouteController;
                    mRequestedRouteController.onSelect();
                    return;
                } else {
                    Log.w(TAG, "setSelectedRouteInternal: Failed to create dynamic group route "
                            + "controller. route=" + route);
                }
            }

            RouteController routeController = route.getProviderInstance().onCreateRouteController(
                    route.mDescriptorId);
            if (routeController != null) {
                routeController.onSelect();
            }

            if (DEBUG) {
                Log.d(TAG, "Route selected: " + route);
            }

            // Don't notify during the initialization.
            if (mSelectedRoute == null) {
                mSelectedRoute = route;
                mSelectedRouteController = routeController;
                mCallbackHandler.post(GlobalMediaRouter.CallbackHandler.MSG_ROUTE_SELECTED,
                            new Pair<>(null, route), unselectReason);
                return;
            } else {
                notifyTransfer(this, route, routeController, unselectReason,
                                /*requestedRoute=*/null, /*memberRoutes=*/null);
            }
        }

        void maybeUpdateMemberRouteControllers() {
            if (!mSelectedRoute.isGroup()) {
                return;
            }
            List<RouteInfo> routes = mSelectedRoute.getMemberRoutes();
            // Build a set of descriptor IDs for the new route group.
            Set<String> idSet = new HashSet<>();
            for (RouteInfo route : routes) {
                idSet.add(route.mUniqueId);
            }
            // Unselect route controllers for the removed routes.
            Iterator<Map.Entry<String, RouteController>> iter =
                    mRouteControllerMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, RouteController> entry = iter.next();
                if (!idSet.contains(entry.getKey())) {
                    RouteController controller = entry.getValue();
                    controller.onUnselect(UNSELECT_REASON_UNKNOWN);
                    controller.onRelease();
                    iter.remove();
                }
            }
            // Select route controllers for the added routes.
            for (RouteInfo route : routes) {
                if (!mRouteControllerMap.containsKey(route.mUniqueId)) {
                    RouteController controller = route.getProviderInstance()
                            .onCreateRouteController(
                                    route.mDescriptorId, mSelectedRoute.mDescriptorId);
                    controller.onSelect();
                    mRouteControllerMap.put(route.mUniqueId, controller);
                }
            }
        }

        void notifyTransfer(GlobalMediaRouter router, RouteInfo route,
                @Nullable RouteController routeController, @UnselectReason int reason,
                @Nullable RouteInfo requestedRoute,
                @Nullable Collection<DynamicRouteDescriptor> memberRoutes) {
            if (mTransferNotifier != null) {
                mTransferNotifier.cancel();
                mTransferNotifier = null;
            }
            mTransferNotifier = new PrepareTransferNotifier(router, route, routeController,
                    reason, requestedRoute, memberRoutes);

            if (mTransferNotifier.mReason != UNSELECT_REASON_ROUTE_CHANGED
                    || mOnPrepareTransferListener == null) {
                mTransferNotifier.finishTransfer();
            } else {
                ListenableFuture<Void> future =
                        mOnPrepareTransferListener.onPrepareTransfer(mSelectedRoute,
                                mTransferNotifier.mToRoute);
                if (future == null) {
                    mTransferNotifier.finishTransfer();
                } else {
                    mTransferNotifier.setFuture(future);
                }
            }
        }

        DynamicGroupRouteController.OnDynamicRoutesChangedListener mDynamicRoutesListener =
                new DynamicGroupRouteController.OnDynamicRoutesChangedListener() {
                    @Override
                    public void onRoutesChanged(
                            @NonNull DynamicGroupRouteController controller,
                            @Nullable MediaRouteDescriptor groupRouteDescriptor,
                            @NonNull Collection<DynamicGroupRouteController.DynamicRouteDescriptor>
                                    routes) {
                        if (controller == mRequestedRouteController
                                && groupRouteDescriptor != null) {
                            ProviderInfo provider = mRequestedRoute.getProvider();
                            String groupId = groupRouteDescriptor.getId();

                            String uniqueId = assignRouteUniqueId(provider, groupId);
                            RouteInfo route = new RouteInfo(provider, groupId, uniqueId);
                            route.maybeUpdateDescriptor(groupRouteDescriptor);

                            if (mSelectedRoute == route) {
                                return;
                            }

                            notifyTransfer(GlobalMediaRouter.this, route, mRequestedRouteController,
                                    UNSELECT_REASON_ROUTE_CHANGED, mRequestedRoute, routes);

                            mRequestedRoute = null;
                            mRequestedRouteController = null;
                        } else if (controller == mSelectedRouteController) {
                            if (groupRouteDescriptor != null) {
                                updateRouteDescriptorAndNotify(mSelectedRoute,
                                        groupRouteDescriptor);
                            }
                            mSelectedRoute.updateDynamicDescriptors(routes);
                        }
                    }
                };

        @Override
        public void onSystemRouteSelectedByDescriptorId(String id) {
            // System route is selected, do not sync the route we selected before.
            mCallbackHandler.removeMessages(CallbackHandler.MSG_ROUTE_SELECTED);
            ProviderInfo provider = findProviderInfo(mSystemProvider);
            if (provider != null) {
                RouteInfo route = provider.findRouteByDescriptorId(id);
                if (route != null) {
                    route.select();
                }
            }
        }

        public void addRemoteControlClient(Object rcc) {
            int index = findRemoteControlClientRecord(rcc);
            if (index < 0) {
                RemoteControlClientRecord record = new RemoteControlClientRecord(rcc);
                mRemoteControlClients.add(record);
            }
        }

        public void removeRemoteControlClient(Object rcc) {
            int index = findRemoteControlClientRecord(rcc);
            if (index >= 0) {
                RemoteControlClientRecord record = mRemoteControlClients.remove(index);
                record.disconnect();
            }
        }

        public void setMediaSession(Object session) {
            setMediaSessionRecord(session != null ? new MediaSessionRecord(session) : null);
        }

        public void setMediaSessionCompat(final MediaSessionCompat session) {
            mCompatSession = session;
            if (Build.VERSION.SDK_INT >= 21) {
                setMediaSessionRecord(session != null ? new MediaSessionRecord(session) : null);
            } else {
                if (mRccMediaSession != null) {
                    removeRemoteControlClient(mRccMediaSession.getRemoteControlClient());
                    mRccMediaSession.removeOnActiveChangeListener(mSessionActiveListener);
                }
                mRccMediaSession = session;
                if (session != null) {
                    session.addOnActiveChangeListener(mSessionActiveListener);
                    if (session.isActive()) {
                        addRemoteControlClient(session.getRemoteControlClient());
                    }
                }
            }
        }

        private void setMediaSessionRecord(MediaSessionRecord mediaSessionRecord) {
            if (mMediaSession != null) {
                mMediaSession.clearVolumeHandling();
            }
            mMediaSession = mediaSessionRecord;
            if (mediaSessionRecord != null) {
                updatePlaybackInfoFromSelectedRoute();
            }
        }

        public MediaSessionCompat.Token getMediaSessionToken() {
            if (mMediaSession != null) {
                return mMediaSession.getToken();
            } else if (mCompatSession != null) {
                return mCompatSession.getSessionToken();
            }
            return null;
        }

        private int findRemoteControlClientRecord(Object rcc) {
            final int count = mRemoteControlClients.size();
            for (int i = 0; i < count; i++) {
                RemoteControlClientRecord record = mRemoteControlClients.get(i);
                if (record.getRemoteControlClient() == rcc) {
                    return i;
                }
            }
            return -1;
        }

        @SuppressLint("NewApi")
        void updatePlaybackInfoFromSelectedRoute() {
            if (mSelectedRoute != null) {
                mPlaybackInfo.volume = mSelectedRoute.getVolume();
                mPlaybackInfo.volumeMax = mSelectedRoute.getVolumeMax();
                mPlaybackInfo.volumeHandling = mSelectedRoute.getVolumeHandling();
                mPlaybackInfo.playbackStream = mSelectedRoute.getPlaybackStream();
                mPlaybackInfo.playbackType = mSelectedRoute.getPlaybackType();
                if (mMediaTransferEnabled
                        && mSelectedRoute.getProviderInstance() == mMr2Provider) {
                    mPlaybackInfo.volumeControlId = MediaRoute2Provider
                            .getSessionIdForRouteController(mSelectedRouteController);
                } else {
                    mPlaybackInfo.volumeControlId = null;
                }

                final int count = mRemoteControlClients.size();
                for (int i = 0; i < count; i++) {
                    RemoteControlClientRecord record = mRemoteControlClients.get(i);
                    record.updatePlaybackInfo();
                }
                if (mMediaSession != null) {
                    if (mSelectedRoute == getDefaultRoute()
                            || mSelectedRoute == getBluetoothRoute()) {
                        // Local route
                        mMediaSession.clearVolumeHandling();
                    } else {
                        @VolumeProviderCompat.ControlType int controlType =
                                VolumeProviderCompat.VOLUME_CONTROL_FIXED;
                        if (mPlaybackInfo.volumeHandling
                                == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE) {
                            controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
                        }
                        mMediaSession.configureVolume(controlType, mPlaybackInfo.volumeMax,
                                mPlaybackInfo.volume, mPlaybackInfo.volumeControlId);
                    }
                }
            } else {
                if (mMediaSession != null) {
                    mMediaSession.clearVolumeHandling();
                }
            }
        }

        private final class ProviderCallback extends MediaRouteProvider.Callback {
            ProviderCallback() {
            }

            @Override
            public void onDescriptorChanged(@NonNull MediaRouteProvider provider,
                    MediaRouteProviderDescriptor descriptor) {
                updateProviderDescriptor(provider, descriptor);
            }
        }

        final class Mr2ProviderCallback extends MediaRoute2Provider.Callback {
            @Override
            public void onSelectRoute(@NonNull String routeDescriptorId,
                    @UnselectReason int reason) {
                MediaRouter.RouteInfo routeToSelect = null;
                for (MediaRouter.RouteInfo routeInfo : getRoutes()) {
                    if (routeInfo.getProviderInstance() != mMr2Provider) {
                        continue;
                    }
                    if (TextUtils.equals(routeDescriptorId, routeInfo.getDescriptorId())) {
                        routeToSelect = routeInfo;
                        break;
                    }
                }

                if (routeToSelect == null) {
                    Log.w(TAG, "onSelectRoute: The target RouteInfo is not found for descriptorId="
                            + routeDescriptorId);
                    return;
                }

                selectRouteInternal(routeToSelect, reason);
            }

            @Override
            public void onSelectFallbackRoute(@UnselectReason int reason) {
                selectRouteToFallbackRoute(reason);
            }

            @Override
            public void onReleaseController(@NonNull RouteController controller) {
                if (controller == mSelectedRouteController) {
                    // Stop casting
                    selectRouteToFallbackRoute(UNSELECT_REASON_STOPPED);
                } else if (DEBUG) {
                    // 'Cast -> Phone' / 'Cast -> Cast(old)' cases triggered by selectRoute().
                    // Nothing to do.
                    Log.d(TAG, "A RouteController unrelated to the selected route is released."
                            + " controller=" + controller);
                }
            }

            void selectRouteToFallbackRoute(@UnselectReason int reason) {
                RouteInfo fallbackRoute = chooseFallbackRoute();
                if (getSelectedRoute() != fallbackRoute) {
                    selectRouteInternal(fallbackRoute, reason);
                }
                // Does nothing when the selected route is same with fallback route.
                // This is the difference between this and unselect().
            }
        }

        private final class MediaSessionRecord {
            private final MediaSessionCompat mMsCompat;

            private @VolumeProviderCompat.ControlType int mControlType;
            private int mMaxVolume;
            private VolumeProviderCompat mVpCompat;

            MediaSessionRecord(Object mediaSession) {
                this(MediaSessionCompat.fromMediaSession(mApplicationContext, mediaSession));
            }

            MediaSessionRecord(MediaSessionCompat mediaSessionCompat) {
                mMsCompat = mediaSessionCompat;
            }

            public void configureVolume(@VolumeProviderCompat.ControlType int controlType,
                    int max, int current, @Nullable String volumeControlId) {
                if (mMsCompat != null) {
                    if (mVpCompat != null && controlType == mControlType && max == mMaxVolume) {
                        // If we haven't changed control type or max just set the
                        // new current volume
                        mVpCompat.setCurrentVolume(current);
                    } else {
                        // Otherwise create a new provider and update
                        mVpCompat = new VolumeProviderCompat(controlType, max, current,
                                volumeControlId) {
                            @Override
                            public void onSetVolumeTo(final int volume) {
                                mCallbackHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mSelectedRoute != null) {
                                            mSelectedRoute.requestSetVolume(volume);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onAdjustVolume(final int direction) {
                                mCallbackHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mSelectedRoute != null) {
                                            mSelectedRoute.requestUpdateVolume(direction);
                                        }
                                    }
                                });
                            }
                        };
                        mMsCompat.setPlaybackToRemote(mVpCompat);
                    }
                }
            }

            public void clearVolumeHandling() {
                if (mMsCompat != null) {
                    mMsCompat.setPlaybackToLocal(mPlaybackInfo.playbackStream);
                    mVpCompat = null;
                }
            }

            public MediaSessionCompat.Token getToken() {
                if (mMsCompat != null) {
                    return mMsCompat.getSessionToken();
                }
                return null;
            }
        }

        private final class RemoteControlClientRecord
                implements RemoteControlClientCompat.VolumeCallback {
            private final RemoteControlClientCompat mRccCompat;
            private boolean mDisconnected;

            public RemoteControlClientRecord(Object rcc) {
                mRccCompat = RemoteControlClientCompat.obtain(mApplicationContext, rcc);
                mRccCompat.setVolumeCallback(this);
                updatePlaybackInfo();
            }

            public Object getRemoteControlClient() {
                return mRccCompat.getRemoteControlClient();
            }

            public void disconnect() {
                mDisconnected = true;
                mRccCompat.setVolumeCallback(null);
            }

            public void updatePlaybackInfo() {
                mRccCompat.setPlaybackInfo(mPlaybackInfo);
            }

            @Override
            public void onVolumeSetRequest(int volume) {
                if (!mDisconnected && mSelectedRoute != null) {
                    mSelectedRoute.requestSetVolume(volume);
                }
            }

            @Override
            public void onVolumeUpdateRequest(int direction) {
                if (!mDisconnected && mSelectedRoute != null) {
                    mSelectedRoute.requestUpdateVolume(direction);
                }
            }
        }

        private final class CallbackHandler extends Handler {
            private final ArrayList<CallbackRecord> mTempCallbackRecords =
                    new ArrayList<CallbackRecord>();
            private final List<RouteInfo> mDynamicGroupRoutes = new ArrayList<>();

            private static final int MSG_TYPE_MASK = 0xff00;
            private static final int MSG_TYPE_ROUTE = 0x0100;
            private static final int MSG_TYPE_PROVIDER = 0x0200;
            private static final int MSG_TYPE_ROUTER = 0x0300;

            public static final int MSG_ROUTE_ADDED = MSG_TYPE_ROUTE | 1;
            public static final int MSG_ROUTE_REMOVED = MSG_TYPE_ROUTE | 2;
            public static final int MSG_ROUTE_CHANGED = MSG_TYPE_ROUTE | 3;
            public static final int MSG_ROUTE_VOLUME_CHANGED = MSG_TYPE_ROUTE | 4;
            public static final int MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED = MSG_TYPE_ROUTE | 5;
            public static final int MSG_ROUTE_SELECTED = MSG_TYPE_ROUTE | 6;
            public static final int MSG_ROUTE_UNSELECTED = MSG_TYPE_ROUTE | 7;
            public static final int MSG_ROUTE_ANOTHER_SELECTED = MSG_TYPE_ROUTE | 8;

            public static final int MSG_PROVIDER_ADDED = MSG_TYPE_PROVIDER | 1;
            public static final int MSG_PROVIDER_REMOVED = MSG_TYPE_PROVIDER | 2;
            public static final int MSG_PROVIDER_CHANGED = MSG_TYPE_PROVIDER | 3;

            public static final int MSG_ROUTER_PARAMS_CHANGED = MSG_TYPE_ROUTER | 1;

            CallbackHandler() {
            }

            public void post(int msg, Object obj) {
                obtainMessage(msg, obj).sendToTarget();
            }

            public void post(int msg, Object obj, int arg) {
                Message message = obtainMessage(msg, obj);
                message.arg1 = arg;
                message.sendToTarget();
            }

            @Override
            public void handleMessage(Message msg) {
                final int what = msg.what;
                final Object obj = msg.obj;
                final int arg = msg.arg1;

                if (what == MSG_ROUTE_CHANGED
                        && getSelectedRoute().getId().equals(((RouteInfo) obj).getId())) {
                    updateSelectedRouteIfNeeded(true);
                }

                // Synchronize state with the system media router.
                syncWithSystemProvider(what, obj);

                // Invoke all registered callbacks.
                // Build a list of callbacks before invoking them in case callbacks
                // are added or removed during dispatch.
                try {
                    for (int i = mRouters.size(); --i >= 0; ) {
                        MediaRouter router = mRouters.get(i).get();
                        if (router == null) {
                            mRouters.remove(i);
                        } else {
                            mTempCallbackRecords.addAll(router.mCallbackRecords);
                        }
                    }

                    final int callbackCount = mTempCallbackRecords.size();
                    for (int i = 0; i < callbackCount; i++) {
                        invokeCallback(mTempCallbackRecords.get(i), what, obj, arg);
                    }
                } finally {
                    mTempCallbackRecords.clear();
                }
            }

            // Using Pair<RouteInfo, RouteInfo>
            @SuppressWarnings({"unchecked"})
            private void syncWithSystemProvider(int what, Object obj) {
                switch (what) {
                    case MSG_ROUTE_ADDED:
                        mSystemProvider.onSyncRouteAdded((RouteInfo) obj);
                        break;
                    case MSG_ROUTE_REMOVED:
                        mSystemProvider.onSyncRouteRemoved((RouteInfo) obj);
                        break;
                    case MSG_ROUTE_CHANGED:
                        mSystemProvider.onSyncRouteChanged((RouteInfo) obj);
                        break;
                    case MSG_ROUTE_SELECTED: {
                        RouteInfo selectedRoute = ((Pair<RouteInfo, RouteInfo>) obj).second;
                        mSystemProvider.onSyncRouteSelected(selectedRoute);
                        // TODO(b/166794092): Remove this nullness check
                        if (mDefaultRoute != null && selectedRoute.isDefaultOrBluetooth()) {
                            for (RouteInfo prevGroupRoute : mDynamicGroupRoutes) {
                                mSystemProvider.onSyncRouteRemoved(prevGroupRoute);
                            }
                            mDynamicGroupRoutes.clear();
                        }
                        break;
                    }
                    case MSG_ROUTE_ANOTHER_SELECTED: {
                        RouteInfo groupRoute = ((Pair<RouteInfo, RouteInfo>) obj).second;
                        mDynamicGroupRoutes.add(groupRoute);
                        mSystemProvider.onSyncRouteAdded(groupRoute);
                        mSystemProvider.onSyncRouteSelected(groupRoute);
                        break;
                    }
                }
            }

            @SuppressWarnings("unchecked") // Using Pair<RouteInfo, RouteInfo>
            private void invokeCallback(CallbackRecord record, int what, Object obj, int arg) {
                final MediaRouter router = record.mRouter;
                final MediaRouter.Callback callback = record.mCallback;
                switch (what & MSG_TYPE_MASK) {
                    case MSG_TYPE_ROUTE: {
                        final RouteInfo route =
                                (what == MSG_ROUTE_ANOTHER_SELECTED || what == MSG_ROUTE_SELECTED)
                                ? ((Pair<RouteInfo, RouteInfo>) obj).second : (RouteInfo) obj;
                        final RouteInfo optionalRoute =
                                (what == MSG_ROUTE_ANOTHER_SELECTED || what == MSG_ROUTE_SELECTED)
                                ? ((Pair<RouteInfo, RouteInfo>) obj).first : null;
                        if (route == null || !record.filterRouteEvent(
                                route, what, optionalRoute, arg)) {
                            break;
                        }
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
                                callback.onRouteSelected(router, route, arg, route);
                                break;
                            case MSG_ROUTE_UNSELECTED:
                                callback.onRouteUnselected(router, route, arg);
                                break;
                            case MSG_ROUTE_ANOTHER_SELECTED:
                                callback.onRouteSelected(router, route, arg, optionalRoute);
                                break;
                        }
                        break;
                    }
                    case MSG_TYPE_PROVIDER: {
                        final ProviderInfo provider = (ProviderInfo)obj;
                        switch (what) {
                            case MSG_PROVIDER_ADDED:
                                callback.onProviderAdded(router, provider);
                                break;
                            case MSG_PROVIDER_REMOVED:
                                callback.onProviderRemoved(router, provider);
                                break;
                            case MSG_PROVIDER_CHANGED:
                                callback.onProviderChanged(router, provider);
                                break;
                        }
                        break;
                    }
                    case MSG_TYPE_ROUTER: {
                        switch (what) {
                            case MSG_ROUTER_PARAMS_CHANGED:
                                final MediaRouterParams params = (MediaRouterParams) obj;
                                callback.onRouterParamsChanged(router, params);
                                break;
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Class to notify events about transfer.
     */
    static final class PrepareTransferNotifier {
        private static final long TRANSFER_TIMEOUT_MS = 15_000;

        final RouteController mToRouteController;
        final @UnselectReason int mReason;
        private final RouteInfo mFromRoute;
        final RouteInfo mToRoute;
        private final RouteInfo mRequestedRoute;
        @Nullable
        final List<DynamicRouteDescriptor> mMemberRoutes;
        private final WeakReference<GlobalMediaRouter> mRouter;

        private ListenableFuture<Void> mFuture = null;
        private boolean mFinished = false;
        private boolean mCanceled = false;

        PrepareTransferNotifier(GlobalMediaRouter router, RouteInfo route,
                @Nullable RouteController routeController, @UnselectReason int reason,
                @Nullable RouteInfo requestedRoute,
                @Nullable Collection<DynamicRouteDescriptor> memberRoutes) {
            mRouter = new WeakReference<>(router);

            mToRoute = route;
            mToRouteController = routeController;
            mReason = reason;
            mFromRoute = router.mSelectedRoute;
            mRequestedRoute = requestedRoute;
            mMemberRoutes = (memberRoutes == null) ? null : new ArrayList<>(memberRoutes);

            // For the case it's not handled properly
            router.mCallbackHandler.postDelayed(this::finishTransfer,
                    TRANSFER_TIMEOUT_MS);
        }

        void setFuture(ListenableFuture<Void> future) {
            GlobalMediaRouter router = mRouter.get();
            if (router == null || router.mTransferNotifier != this) {
                Log.w(TAG, "Router is released. Cancel transfer");
                cancel();
                return;
            }

            if (mFuture != null) {
                throw new IllegalStateException("future is already set");
            }

            mFuture = future;
            future.addListener(this::finishTransfer, router.mCallbackHandler::post);
        }

        /**
         * Notifies that preparation for transfer is finished.
         */
        void finishTransfer() {
            checkCallingThread();

            if (mFinished || mCanceled) {
                return;
            }

            GlobalMediaRouter router = mRouter.get();
            if (router == null || router.mTransferNotifier != this
                    || (mFuture != null && mFuture.isCancelled())) {
                cancel();
                return;
            }

            mFinished = true;
            router.mTransferNotifier = null;

            unselectFromRouteAndNotify();
            selectToRouteAndNotify();
        }

        void cancel() {
            if (mFinished || mCanceled) {
                return;
            }
            mCanceled = true;

            if (mToRouteController != null) {
                mToRouteController.onUnselect(UNSELECT_REASON_UNKNOWN);
                mToRouteController.onRelease();
            }
        }

        private void unselectFromRouteAndNotify() {
            GlobalMediaRouter router = mRouter.get();
            if (router == null || router.mSelectedRoute != mFromRoute) {
                return;
            }

            router.mCallbackHandler.post(GlobalMediaRouter.CallbackHandler.MSG_ROUTE_UNSELECTED,
                    mFromRoute, mReason);

            if (router.mSelectedRouteController != null) {
                router.mSelectedRouteController.onUnselect(mReason);
                router.mSelectedRouteController.onRelease();
            }
            // Release member route controllers
            if (!router.mRouteControllerMap.isEmpty()) {
                for (RouteController controller : router.mRouteControllerMap.values()) {
                    controller.onUnselect(mReason);
                    controller.onRelease();
                }
                router.mRouteControllerMap.clear();
            }
            router.mSelectedRouteController = null;
        }

        private void selectToRouteAndNotify() {
            GlobalMediaRouter router = mRouter.get();
            if (router == null) {
                return;
            }

            router.mSelectedRoute = mToRoute;
            router.mSelectedRouteController = mToRouteController;

            if (mRequestedRoute == null) {
                router.mCallbackHandler.post(GlobalMediaRouter.CallbackHandler.MSG_ROUTE_SELECTED,
                        new Pair<>(mFromRoute, mToRoute), mReason);
            } else {
                router.mCallbackHandler.post(
                        GlobalMediaRouter.CallbackHandler.MSG_ROUTE_ANOTHER_SELECTED,
                        new Pair<>(mRequestedRoute, mToRoute), mReason);
            }

            router.mRouteControllerMap.clear();
            router.maybeUpdateMemberRouteControllers();
            router.updatePlaybackInfoFromSelectedRoute();
            if (mMemberRoutes != null) {
                router.mSelectedRoute.updateDynamicDescriptors(mMemberRoutes);
            }
        }
    }
}
