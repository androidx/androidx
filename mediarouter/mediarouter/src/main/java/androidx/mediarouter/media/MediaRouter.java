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
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
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
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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
    // The "Ax" prefix disambiguates from the platform's MediaRouter.
    static final String TAG = "AxMediaRouter";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @IntDef({
        UNSELECT_REASON_UNKNOWN,
        UNSELECT_REASON_DISCONNECTED,
        UNSELECT_REASON_STOPPED,
        UNSELECT_REASON_ROUTE_CHANGED
    })
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

    /** Maintains global media router state for the process. */
    static GlobalMediaRouter sGlobal;

    // Context-bound state of the media router.
    final Context mContext;
    final ArrayList<CallbackRecord> mCallbackRecords = new ArrayList<>();

    @IntDef(
            flag = true,
            value = {
                CALLBACK_FLAG_PERFORM_ACTIVE_SCAN,
                CALLBACK_FLAG_REQUEST_DISCOVERY,
                CALLBACK_FLAG_UNFILTERED_EVENTS,
                CALLBACK_FLAG_FORCE_DISCOVERY
            })
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
     * applications should only specify this flag when
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

    /* package */ MediaRouter(Context context) {
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
     * <p>Must be called on the main thread.
     *
     * @return The media router instance for the context.  The application must hold
     * a strong reference to this object as long as it is in use.
     */
    @MainThread
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
     * Resets all internal state for testing. Should be only used for testing purpose.
     * <p>
     * After calling this method, the caller should stop using the existing media router instances.
     * Instead, the caller should create a new media router instance again by calling
     * {@link #getInstance(Context)}.
     * <p>
     * Note that the following classes' instances need to be recreated after calling this method,
     * as these classes store the media router instance on their constructor:
     * <ul>
     *     <li>{@link androidx.mediarouter.app.MediaRouteActionProvider}
     *     <li>{@link androidx.mediarouter.app.MediaRouteButton}
     *     <li>{@link androidx.mediarouter.app.MediaRouteChooserDialog}
     *     <li>{@link androidx.mediarouter.app.MediaRouteControllerDialog}
     *     <li>{@link androidx.mediarouter.app.MediaRouteDiscoveryFragment}
     * </ul>
     */
    @RestrictTo(LIBRARY_GROUP)
    public static void resetGlobalRouter() {
        if (sGlobal == null) {
            return;
        }
        sGlobal.reset();
        sGlobal = null;
    }

    /** Gets the initialized global router. */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    static GlobalMediaRouter getGlobalRouter() {
        if (sGlobal == null) {
            throw new IllegalStateException(
                    "getGlobalRouter cannot be called when sGlobal is " + "null");
        }
        return sGlobal;
    }

    /**
     * Gets information about the {@link MediaRouter.RouteInfo routes} currently known to
     * this media router.
     *
     * <p>Must be called on the main thread.
     */
    @MainThread
    @NonNull
    public List<RouteInfo> getRoutes() {
        checkCallingThread();
        return getGlobalRouter().getRoutes();
    }

    /**
     * Gets information about the {@link MediaRouter.ProviderInfo route providers}
     * currently known to this media router.
     *
     * <p>Must be called on the main thread.
     */
    @MainThread
    @NonNull
    public List<ProviderInfo> getProviders() {
        checkCallingThread();
        return getGlobalRouter().getProviders();
    }

    /**
     * Gets the default route for playing media content on the system.
     * <p>
     * The system always provides a default route.
     * </p>
     *
     * <p>Must be called on the main thread.
     *
     * @return The default route, which is guaranteed to never be null.
     */
    @MainThread
    @NonNull
    public RouteInfo getDefaultRoute() {
        checkCallingThread();
        return getGlobalRouter().getDefaultRoute();
    }

    /**
     * Gets a bluetooth route for playing media content on the system.
     *
     * <p>Must be called on the main thread.
     *
     * @return A bluetooth route, if exist, otherwise null.
     */
    @MainThread
    @Nullable
    public RouteInfo getBluetoothRoute() {
        checkCallingThread();
        return getGlobalRouter().getBluetoothRoute();
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
     * <p>Must be called on the main thread.
     *
     * @return The selected route, which is guaranteed to never be null.
     * @see RouteInfo#getControlFilters
     * @see RouteInfo#supportsControlCategory
     * @see RouteInfo#supportsControlRequest
     */
    @MainThread
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
     * <p>Must be called on the main thread.
     *
     * @param selector The selector to match.
     * @return The previously selected route if it matched the selector, otherwise the
     * newly selected default route which is guaranteed to never be null.
     * @see MediaRouteSelector
     * @see RouteInfo#matchesSelector
     */
    @MainThread
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
            globalRouter.selectRoute(
                    route,
                    MediaRouter.UNSELECT_REASON_ROUTE_CHANGED,
                    /* syncMediaRoute1Provider= */ true);
        }
        return route;
    }

    /**
     * Selects the specified route.
     *
     * <p>Must be called on the main thread.
     *
     * @param route The route to select.
     */
    @MainThread
    public void selectRoute(@NonNull RouteInfo route) {
        route.select();
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
     * <p>Must be called on the main thread.
     *
     * @param reason The reason for disconnecting the current route.
     */
    @MainThread
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
            globalRouter.selectRoute(fallbackRoute, reason, /* syncMediaRoute1Provider= */ true);
        }
    }

    /**
     * Adds the specified route as a member to the current dynamic group.
     */
    @RestrictTo(LIBRARY)
    @MainThread
    public void addMemberToDynamicGroup(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().addMemberToDynamicGroup(route);
    }

    /**
     * Removes the specified route from the current dynamic group.
     */
    @RestrictTo(LIBRARY)
    @MainThread
    public void removeMemberFromDynamicGroup(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().removeMemberFromDynamicGroup(route);
    }

    /**
     * Transfers the current dynamic group to the specified route.
     */
    @RestrictTo(LIBRARY)
    @MainThread
    public void transferToRoute(@NonNull RouteInfo route) {
        if (route == null) {
            throw new NullPointerException("route must not be null");
        }
        checkCallingThread();
        getGlobalRouter().transferToRoute(route);
    }

    /**
     * Returns true if there is a route that matches the specified selector.
     *
     * <p>This method returns true if there are any available routes that match the selector
     * regardless of whether they are enabled or disabled. If the {@link
     * #AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE} flag is specified, then the method will only
     * consider non-default routes.
     *
     * <p class="note">On {@link ActivityManager#isLowRamDevice low-RAM devices} this method will
     * return true if it is possible to discover a matching route even if discovery is not in
     * progress or if no matching route has yet been found. Use {@link
     * #AVAILABILITY_FLAG_REQUIRE_MATCH} to require an actual match.
     *
     * <p>Must be called on the main thread.
     *
     * @param selector The selector to match.
     * @param flags Flags to control the determination of whether a route may be available. May be
     *     zero or some combination of {@link #AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE} and {@link
     *     #AVAILABILITY_FLAG_REQUIRE_MATCH}.
     * @return True if a matching route may be available.
     */
    @MainThread
    public boolean isRouteAvailable(@NonNull MediaRouteSelector selector, int flags) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }
        checkCallingThread();
        return getGlobalRouter().isRouteAvailable(selector, flags);
    }

    /**
     * Registers a callback to discover routes that match the selector and to receive events when
     * they change.
     *
     * <p>This is a convenience method that has the same effect as calling {@link
     * #addCallback(MediaRouteSelector, Callback, int)} without flags.
     *
     * <p>Must be called on the main thread.
     *
     * @param selector A route selector that indicates the kinds of routes that the callback would
     *     like to discover.
     * @param callback The callback to add.
     * @see #removeCallback
     */
    @MainThread
    public void addCallback(@NonNull MediaRouteSelector selector, @NonNull Callback callback) {
        addCallback(selector, callback, 0);
    }

    /**
     * Registers a callback to discover routes that match the selector and to receive events when
     * they change.
     *
     * <p>The selector describes the kinds of routes that the application wants to discover. For
     * example, if the application wants to use live audio routes then it should include the {@link
     * MediaControlIntent#CATEGORY_LIVE_AUDIO live audio media control intent category} in its
     * selector when it adds a callback to the media router. The selector may include any number of
     * categories.
     *
     * <p>If the callback has already been registered, then the selector is added to the set of
     * selectors being monitored by the callback.
     *
     * <p>By default, the callback will only be invoked for events that affect routes that match the
     * specified selector. Event filtering may be disabled by specifying the {@link
     * #CALLBACK_FLAG_UNFILTERED_EVENTS} flag when the callback is registered.
     *
     * <p>Applications should use the {@link #isRouteAvailable} method to determine whether is it
     * possible to discover a route with the desired capabilities and therefore whether the media
     * route button should be shown to the user.
     *
     * <p>The {@link #CALLBACK_FLAG_REQUEST_DISCOVERY} flag should be used while the application is
     * in the foreground to request that passive discovery be performed if there are sufficient
     * resources to allow continuous passive discovery. On {@link ActivityManager#isLowRamDevice
     * low-RAM devices} this flag will be ignored to conserve resources.
     *
     * <p>The {@link #CALLBACK_FLAG_FORCE_DISCOVERY} flag should be used when passive discovery
     * absolutely must be performed, even on low-RAM devices. This flag has a significant
     * performance impact on low-RAM devices since it may cause many media route providers to be
     * started simultaneously. It is much better to use {@link #CALLBACK_FLAG_REQUEST_DISCOVERY}
     * instead to avoid performing passive discovery on these devices altogether.
     *
     * <p>The {@link #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} flag should be used when the media route
     * chooser dialog is showing to confirm the presence of available routes that the user may
     * connect to. This flag may use substantially more power. Once active scan is requested, it
     * will be effective for 30 seconds and will be suppressed after the delay. If you need active
     * scan after this duration, you have to add your callback again with the {@link
     * #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} flag.
     *
     * <h3>Example</h3>
     *
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
     * <p>Must be called on the main thread.
     *
     * @param selector A route selector that indicates the kinds of routes that the callback would
     *     like to discover.
     * @param callback The callback to add.
     * @param flags Flags to control the behavior of the callback. May be zero or a combination of
     *     {@link #CALLBACK_FLAG_PERFORM_ACTIVE_SCAN} and {@link #CALLBACK_FLAG_UNFILTERED_EVENTS}.
     * @see #removeCallback
     */
    // TODO: Change the usages of addCallback() for changing flags when setCallbackFlags() is added.
    @MainThread
    public void addCallback(
            @NonNull MediaRouteSelector selector,
            @NonNull Callback callback,
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
     * <p>Must be called on the main thread.
     *
     * @param callback The callback to remove.
     * @see #addCallback
     */
    @MainThread
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
     *
     * <p>Must be called on the main thread.
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
     * <p>Must be called on the main thread.
     *
     * @param providerInstance The media route provider instance to add.
     * @see MediaRouteProvider
     * @see #removeCallback
     */
    @MainThread
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
     * <p>Must be called on the main thread.
     *
     * @param providerInstance The media route provider instance to remove.
     * @see MediaRouteProvider
     * @see #addCallback
     */
    @MainThread
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
     * Adds a remote control client to enable remote control of the volume of the selected route.
     *
     * <p>The remote control client must have previously been registered with the audio manager
     * using the {@link android.media.AudioManager#registerRemoteControlClient
     * AudioManager.registerRemoteControlClient} method.
     *
     * <p>Must be called on the main thread.
     *
     * @param remoteControlClient The {@link android.media.RemoteControlClient} to register.
     * @deprecated Use {@link #setMediaSessionCompat} instead.
     */
    @MainThread
    @Deprecated
    public void addRemoteControlClient(@NonNull Object remoteControlClient) {
        if (remoteControlClient == null) {
            throw new IllegalArgumentException("remoteControlClient must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "addRemoteControlClient: " + remoteControlClient);
        }
        getGlobalRouter()
                .addRemoteControlClient((android.media.RemoteControlClient) remoteControlClient);
    }

    /**
     * Removes a remote control client.
     *
     * <p>Must be called on the main thread.
     *
     * @param remoteControlClient The {@link android.media.RemoteControlClient} to unregister.
     * @deprecated Call {@link #setMediaSessionCompat(MediaSessionCompat)} instead of
     * {@link #addRemoteControlClient(Object)} so that there is no need to call this method.
     */
    @MainThread
    @Deprecated
    public void removeRemoteControlClient(@NonNull Object remoteControlClient) {
        if (remoteControlClient == null) {
            throw new IllegalArgumentException("remoteControlClient must not be null");
        }
        checkCallingThread();

        if (DEBUG) {
            Log.d(TAG, "removeRemoteControlClient: " + remoteControlClient);
        }
        getGlobalRouter()
                .removeRemoteControlClient((android.media.RemoteControlClient) remoteControlClient);
    }

    /**
     * Equivalent to {@link #setMediaSessionCompat}, except it takes an {@link
     * android.media.session.MediaSession}.
     */
    @MainThread
    public void setMediaSession(@Nullable Object mediaSession) {
        checkCallingThread();
        if (DEBUG) {
            Log.d(TAG, "setMediaSession: " + mediaSession);
        }
        getGlobalRouter().setMediaSession(mediaSession);
    }

    /**
     * Associates the provided {@link MediaSessionCompat} to this router.
     *
     * <p>Maintains the internal state of the provided session to signal it's linked to the
     * currently selected route at any given time. This guarantees that the system UI shows the
     * correct route name when applicable.
     *
     * <p>Must be called on the main thread.
     *
     * @param mediaSession The {@link MediaSessionCompat} to associate to this media router, or null
     *     to clear the existing association.
     */
    @MainThread
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
     *
     * <p>Must be called on the main thread.
     */
    @MainThread
    @Nullable
    public MediaRouterParams getRouterParams() {
        checkCallingThread();
        return getGlobalRouter().getRouterParams();
    }

    /**
     * Sets {@link MediaRouterParams parameters} of the media router service associated with this
     * media router.
     *
     * <p>Must be called on the main thread.
     *
     * @param params The parameter to set
     */
    @MainThread
    public void setRouterParams(@Nullable MediaRouterParams params) {
        checkCallingThread();
        getGlobalRouter().setRouterParams(params);
    }

    /**
     * Sets the {@link RouteListingPreference} of the app associated to this media router.
     *
     * <p>This method does nothing on devices running API 33 or older.
     *
     * <p>Use this method to inform the system UI of the routes that you would like to list for
     * media routing, via the Output Switcher.
     *
     * <p>You should call this method immediately after creating an instance and immediately after
     * receiving any {@link Callback route list changes} in order to keep the system UI in a
     * consistent state. You can also call this method at any other point to update the listing
     * preference dynamically (which reflect in the system's Output Switcher).
     *
     * <p>Notes:
     *
     * <ul>
     *   <li>You should not include the ids of two or more routes with a match in their {@link
     *       MediaRouteDescriptor#getDeduplicationIds() deduplication ids}. If you do, the system
     *       will deduplicate them using its own criteria.
     *   <li>You can use this method to rank routes in the output switcher, placing the more
     *       important routes first. The system might override the proposed ranking.
     *   <li>You can use this method to change how routes are listed using dynamic criteria. For
     *       example, you can disable routing while an {@link
     *       RouteListingPreference.Item#SUBTEXT_AD_ROUTING_DISALLOWED ad is playing}).
     * </ul>
     *
     * @param routeListingPreference The {@link RouteListingPreference} for the system to use for
     *     route listing. When null, the system uses its default listing criteria.
     */
    @MainThread
    public void setRouteListingPreference(@Nullable RouteListingPreference routeListingPreference) {
        checkCallingThread();
        getGlobalRouter().setRouteListingPreference(routeListingPreference);
    }

    /**
     * Throws an {@link IllegalStateException} if the calling thread is not the main thread.
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
     */
    @RestrictTo(LIBRARY)
    public static boolean isMediaTransferEnabled() {
        if (sGlobal == null) {
            return false;
        }
        return getGlobalRouter().isMediaTransferEnabled();
    }

    @RestrictTo(LIBRARY)
    public static boolean isGroupVolumeUxEnabled() {
        if (sGlobal == null) {
            return false;
        }
        return getGlobalRouter().isGroupVolumeUxEnabled();
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
        return getGlobalRouter().isTransferToLocalEnabled();
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
        private final boolean mIsSystemRoute;
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

        @IntDef({
            CONNECTION_STATE_DISCONNECTED,
            CONNECTION_STATE_CONNECTING,
            CONNECTION_STATE_CONNECTED
        })
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

        @IntDef({PLAYBACK_TYPE_LOCAL, PLAYBACK_TYPE_REMOTE})
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

        @RestrictTo(LIBRARY)
        @IntDef({
            DEVICE_TYPE_UNKNOWN,
            DEVICE_TYPE_TV,
            DEVICE_TYPE_SPEAKER,
            DEVICE_TYPE_REMOTE_SPEAKER,
            DEVICE_TYPE_BLUETOOTH_A2DP,
            DEVICE_TYPE_AUDIO_VIDEO_RECEIVER,
            DEVICE_TYPE_TABLET,
            DEVICE_TYPE_TABLET_DOCKED,
            DEVICE_TYPE_COMPUTER,
            DEVICE_TYPE_GAME_CONSOLE,
            DEVICE_TYPE_CAR,
            DEVICE_TYPE_SMARTWATCH,
            DEVICE_TYPE_SMARTPHONE,
            DEVICE_TYPE_BUILTIN_SPEAKER,
            DEVICE_TYPE_WIRED_HEADSET,
            DEVICE_TYPE_WIRED_HEADPHONES,
            DEVICE_TYPE_HDMI,
            DEVICE_TYPE_USB_DEVICE,
            DEVICE_TYPE_USB_ACCESSORY,
            DEVICE_TYPE_DOCK,
            DEVICE_TYPE_USB_HEADSET,
            DEVICE_TYPE_HEARING_AID,
            DEVICE_TYPE_BLE_HEADSET,
            DEVICE_TYPE_HDMI_ARC,
            DEVICE_TYPE_HDMI_EARC,
            DEVICE_TYPE_GROUP
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DeviceType {}

        /**
         * The default receiver device type of the route indicating the type is unknown.
         *
         * @see #getDeviceType
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
         * @deprecated use {@link #DEVICE_TYPE_BUILTIN_SPEAKER} and
         * {@link #DEVICE_TYPE_REMOTE_SPEAKER} instead.
         */
        @Deprecated
        public static final int DEVICE_TYPE_SPEAKER = 2;

        /**
         * A receiver device type of the route indicating the presentation of the media is happening
         * on a remote speaker.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_REMOTE_SPEAKER = 2;

        /**
         * A receiver device type of the route indicating the presentation of the media is happening
         * on a bluetooth device such as a bluetooth speaker.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_BLUETOOTH_A2DP = 3;

        /**
         * A receiver device type indicating that the presentation of the media is happening on an
         * Audio/Video receiver (AVR).
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_AUDIO_VIDEO_RECEIVER = 4;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * tablet.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_TABLET = 5;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * docked tablet.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_TABLET_DOCKED = 6;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * computer.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_COMPUTER = 7;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * gaming console.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_GAME_CONSOLE = 8;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * car.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_CAR = 9;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * smartwatch.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_SMARTWATCH = 10;
        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * smartphone.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_SMARTPHONE = 11;

        /**
         * A receiver device type indicating the presentation of the media is happening on a
         * speaker system (i.e. a mono speaker or stereo speakers) built into the device.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_BUILTIN_SPEAKER = 12;

        /**
         * A receiver device type indicating the presentation of the media is happening on a
         * headset, which is the combination of a headphones and a microphone.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_WIRED_HEADSET = 13;

        /**
         * A receiver device type indicating the presentation of the media is happening on a pair
         * of wired headphones.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_WIRED_HEADPHONES = 14;

        /**
         * A receiver device type indicating the presentation of the media is happening on an
         * HDMI connection.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_HDMI = 16;

        /**
         * A receiver device type indicating the presentation of the media is happening on a USB
         * audio device.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_USB_DEVICE = 17;

        /**
         * A receiver device type indicating the presentation of the media is happening on a USB
         * audio device in accessory mode.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_USB_ACCESSORY = 18;

        /**
         * A receiver device type indicating the presentation of the media is happening on an
         * audio device associated on a dock.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_DOCK = 19;

        /**
         * A receiver device type indicating the presentation of the media is happening on a USB
         * audio headset.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_USB_HEADSET = 20;

        /**
         * A receiver device type indicating the presentation of the media is happening on a
         * hearing aid device.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_HEARING_AID = 21;

        /**
         * A receiver device type indicating the presentation of the media is happening on a
         * Bluetooth Low Energy (BLE) HEADSET.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_BLE_HEADSET = 22;

        /**
         * A receiver device type indicating the presentation of the media is happening on an
         * Audio Return Channel of an HDMI connection
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_HDMI_ARC = 23;

        /**
         * A receiver device type indicating the presentation of the media is happening on an
         * Enhanced Audio Return Channel of an HDMI connection
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_HDMI_EARC = 24;

        /**
         * A receiver device type indicating that the presentation of the media is happening on a
         * group of devices.
         *
         * @see #getDeviceType
         */
        public static final int DEVICE_TYPE_GROUP = 1000;

        @IntDef({PLAYBACK_VOLUME_FIXED, PLAYBACK_VOLUME_VARIABLE})
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
         */
        @RestrictTo(LIBRARY)
        public static final int PRESENTATION_DISPLAY_ID_NONE = -1;

        static final int CHANGE_GENERAL = 1 << 0;
        static final int CHANGE_VOLUME = 1 << 1;
        static final int CHANGE_PRESENTATION_DISPLAY = 1 << 2;

        // Should match to SystemMediaRouteProvider.PACKAGE_NAME.
        static final String SYSTEM_MEDIA_ROUTE_PROVIDER_PACKAGE_NAME = "android";

        /* package */ RouteInfo(ProviderInfo provider, String descriptorId, String uniqueId) {
            this(provider, descriptorId, uniqueId, /* isSystemRoute */ false);
        }

        /* package */ RouteInfo(
                ProviderInfo provider,
                String descriptorId,
                String uniqueId,
                boolean isSystemRoute) {
            mProvider = provider;
            mDescriptorId = descriptorId;
            mUniqueId = uniqueId;
            mIsSystemRoute = isSystemRoute;
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
         * Returns {@code true} if this route is a system route.
         *
         * <p>System routes are routes controlled by the system, like the device's built-in
         * speakers, wired headsets, and bluetooth devices.
         *
         * <p>To use system routes, your application should write media sample data to a media
         * framework API, typically via {@link androidx.media3.exoplayer.ExoPlayer ExoPlayer}.
         */
        public boolean isSystemRoute() {
            return mIsSystemRoute;
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
         * <p>Must be called on the main thread.
         *
         * @return True if this route is currently selected.
         * @see MediaRouter#getSelectedRoute
         */
        // Note: Only one representative route can return true. For instance:
        //   - If this route is a selected (non-group) route, it returns true.
        //   - If this route is a selected group route, it returns true.
        //   - If this route is a selected member route of a group, it returns false.
        @MainThread
        public boolean isSelected() {
            checkCallingThread();
            return getGlobalRouter().getSelectedRoute() == this;
        }

        /**
         * Returns true if this route is the default route.
         *
         * <p>Must be called on the main thread.
         *
         * @return True if this route is the default route.
         * @see MediaRouter#getDefaultRoute
         */
        @MainThread
        public boolean isDefault() {
            checkCallingThread();
            return getGlobalRouter().getDefaultRoute() == this;
        }

        /**
         * Returns true if this route is a bluetooth route.
         *
         * <p>Must be called on the main thread.
         *
         * @return True if this route is a bluetooth route.
         * @see MediaRouter#getBluetoothRoute
         */
        @MainThread
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
         * <p>Must be called on the main thread.
         *
         * @param selector The selector that specifies the capabilities to check.
         * @return True if the route supports at least one of the capabilities
         * described in the media route selector.
         */
        @MainThread
        public boolean matchesSelector(@NonNull MediaRouteSelector selector) {
            if (selector == null) {
                throw new IllegalArgumentException("selector must not be null");
            }
            checkCallingThread();
            return selector.matchesControlFilters(mControlFilters);
        }

        /**
         * Returns true if the route supports the specified {@link MediaControlIntent media control}
         * category.
         *
         * <p>Media control categories describe the capabilities of this route such as whether it
         * supports live audio streaming or remote playback.
         *
         * <p>Must be called on the main thread.
         *
         * @param category A {@link MediaControlIntent media control} category such as {@link
         *     MediaControlIntent#CATEGORY_LIVE_AUDIO}, {@link
         *     MediaControlIntent#CATEGORY_LIVE_VIDEO}, {@link
         *     MediaControlIntent#CATEGORY_REMOTE_PLAYBACK}, or a provider-defined media control
         *     category.
         * @return True if the route supports the specified intent category.
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        @MainThread
        public boolean supportsControlCategory(@NonNull String category) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            checkCallingThread();

            for (IntentFilter intentFilter : mControlFilters) {
                if (intentFilter.hasCategory(category)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns true if the route supports the specified {@link MediaControlIntent media control}
         * category and action.
         *
         * <p>Media control actions describe specific requests that an application can ask a route
         * to perform.
         *
         * <p>Must be called on the main thread.
         *
         * @param category A {@link MediaControlIntent media control} category such as {@link
         *     MediaControlIntent#CATEGORY_LIVE_AUDIO}, {@link
         *     MediaControlIntent#CATEGORY_LIVE_VIDEO}, {@link
         *     MediaControlIntent#CATEGORY_REMOTE_PLAYBACK}, or a provider-defined media control
         *     category.
         * @param action A {@link MediaControlIntent media control} action such as {@link
         *     MediaControlIntent#ACTION_PLAY}.
         * @return True if the route supports the specified intent action.
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        @MainThread
        public boolean supportsControlAction(@NonNull String category, @NonNull String action) {
            if (category == null) {
                throw new IllegalArgumentException("category must not be null");
            }
            if (action == null) {
                throw new IllegalArgumentException("action must not be null");
            }
            checkCallingThread();

            for (IntentFilter intentFilter : mControlFilters) {
                if (intentFilter.hasCategory(category) && intentFilter.hasAction(action)) {
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
         * <p>Must be called on the main thread.
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @return True if the route can handle the specified intent.
         * @see MediaControlIntent
         * @see #getControlFilters
         */
        @MainThread
        public boolean supportsControlRequest(@NonNull Intent intent) {
            if (intent == null) {
                throw new IllegalArgumentException("intent must not be null");
            }
            checkCallingThread();

            ContentResolver contentResolver = getGlobalRouter().getContentResolver();
            for (IntentFilter intentFilter : mControlFilters) {
                if (intentFilter.match(contentResolver, intent, true, TAG) >= 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Sends a {@link MediaControlIntent media control} request to be performed asynchronously
         * by the route's destination.
         *
         * <p>Media control requests are used to request the route to perform actions such as
         * starting remote playback of a media item.
         *
         * <p>This function may only be called on a selected route. Control requests sent to
         * unselected routes will fail.
         *
         * <p>Must be called on the main thread.
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @param callback A {@link ControlRequestCallback} to invoke with the result of the
         *     request, or null if no result is required.
         * @see MediaControlIntent
         */
        @MainThread
        public void sendControlRequest(
                @NonNull Intent intent, @Nullable ControlRequestCallback callback) {
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
         * @return The type of the receiver device associated with this route.
         */
        @DeviceType
        public int getDeviceType() {
            return mDeviceType;
        }

        /** */
        @RestrictTo(LIBRARY)
        public boolean isDefaultOrBluetooth() {
            if (isDefault() || mDeviceType == DEVICE_TYPE_BLUETOOTH_A2DP) {
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
            if (isGroup() && !isGroupVolumeUxEnabled()) {
                return PLAYBACK_VOLUME_FIXED;
            }
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
         * Gets whether this route supports disconnecting without interrupting playback.
         *
         * @return True if this route can disconnect without stopping playback, false otherwise.
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
         * <p>Must be called on the main thread.
         *
         * @param volume The new volume value between 0 and {@link #getVolumeMax}.
         */
        @MainThread
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
         * <p>Must be called on the main thread.
         *
         * @param delta The delta to add to the current volume.
         */
        @MainThread
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
         * <p>Must be called on the main thread.
         *
         * @return The preferred presentation display to use when this route is
         * selected or null if none.
         * @see MediaControlIntent#CATEGORY_LIVE_VIDEO
         * @see android.app.Presentation
         */
        @MainThread
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
         *
         * <p>Must be called on the main thread.
         */
        @MainThread
        public void select() {
            select(/* syncMediaRoute1Provider= */ true);
        }

        /**
         * Selects this media route.
         *
         * @param syncMediaRoute1Provider Whether this selection should be passed through to {@link
         *     PlatformMediaRouter1RouteProvider}. Should be false when this call is the result of a
         *     {@link MediaRouter.Callback#onRouteSelected} call.
         */
        @RestrictTo(LIBRARY)
        @MainThread
        public void select(boolean syncMediaRoute1Provider) {
            checkCallingThread();
            getGlobalRouter()
                    .selectRoute(
                            this,
                            MediaRouter.UNSELECT_REASON_ROUTE_CHANGED,
                            syncMediaRoute1Provider);
        }


        /**
         * Returns true if the route has one or more members
         */
        @RestrictTo(LIBRARY)
        public boolean isGroup() {
            return !mMemberRoutes.isEmpty();
        }

        /**
         * Gets the dynamic group state of the given route.
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
         * @return The list of the routes in this group
         */
        @RestrictTo(LIBRARY)
        @NonNull
        public List<RouteInfo> getMemberRoutes() {
            return Collections.unmodifiableList(mMemberRoutes);
        }

        /**
         *
         */
        @MainThread
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

            sb.append("MediaRouter.RouteInfo{ uniqueId=").append(mUniqueId)
                    .append(", name=").append(mName)
                    .append(", description=").append(mDescription)
                    .append(", iconUri=").append(mIconUri)
                    .append(", enabled=").append(mEnabled)
                    .append(", isSystemRoute=").append(mIsSystemRoute)
                    .append(", connectionState=").append(mConnectionState)
                    .append(", canDisconnect=").append(mCanDisconnect)
                    .append(", playbackType=").append(mPlaybackType)
                    .append(", playbackStream=").append(mPlaybackStream)
                    .append(", deviceType=").append(mDeviceType)
                    .append(", volumeHandling=").append(mVolumeHandling)
                    .append(", volume=").append(mVolume)
                    .append(", volumeMax=").append(mVolumeMax)
                    .append(", presentationDisplayId=").append(mPresentationDisplayId)
                    .append(", extras=").append(mExtras)
                    .append(", settingsIntent=").append(mSettingsIntent)
                    .append(", providerPackageName=").append(mProvider.getPackageName());
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
             */
            @RestrictTo(LIBRARY)
            public int getSelectionState() {
                return (mDynamicDescriptor != null) ? mDynamicDescriptor.getSelectionState()
                        : DynamicRouteDescriptor.UNSELECTED;
            }

            @RestrictTo(LIBRARY)
            public boolean isUnselectable() {
                return mDynamicDescriptor == null || mDynamicDescriptor.isUnselectable();
            }

            @RestrictTo(LIBRARY)
            public boolean isGroupable() {
                return mDynamicDescriptor != null && mDynamicDescriptor.isGroupable();
            }

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
        // Package private fields to avoid use of a synthetic accessor.
        final MediaRouteProvider mProviderInstance;
        final List<RouteInfo> mRoutes = new ArrayList<>();
        final boolean mTreatRouteDescriptorIdsAsUnique;

        private final ProviderMetadata mMetadata;
        private MediaRouteProviderDescriptor mDescriptor;

        ProviderInfo(MediaRouteProvider provider, boolean treatRouteDescriptorIdsAsUnique) {
            mProviderInstance = provider;
            mMetadata = provider.getMetadata();
            mTreatRouteDescriptorIdsAsUnique = treatRouteDescriptorIdsAsUnique;
        }

        /**
         * Gets the provider's underlying {@link MediaRouteProvider} instance.
         *
         * <p>Must be called on the main thread.
         */
        @NonNull
        @MainThread
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
         *
         * <p>Must be called on the main thread.
         */
        @MainThread
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
            for (RouteInfo route : mRoutes) {
                if (route.mDescriptorId.equals(id)) {
                    return route;
                }
            }
            return null;
        }

        boolean supportsDynamicGroup() {
            return mDescriptor != null && mDescriptor.supportsDynamicGroupRoute();
        }

        @NonNull
        @Override
        public String toString() {
            return "MediaRouter.RouteProviderInfo{ packageName=" + getPackageName() + " }";
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
        public void onRouteSelected(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when the supplied media route becomes selected as the active route.
         *
         * <p>The reason provided will be one of the following:
         *
         * <ul>
         *   <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}
         *   <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}
         *   <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}
         *   <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}
         * </ul>
         *
         * @param router The media router reporting the event.
         * @param route The route that has been selected.
         * @param reason The reason for unselecting the previous route.
         */
        public void onRouteSelected(
                @NonNull MediaRouter router, @NonNull RouteInfo route, @UnselectReason int reason) {
            onRouteSelected(router, route);
        }

        // TODO: Revise the comment when we have a feature that enables dynamic grouping on pre-R
        // devices.

        /**
         * Called when the supplied media route becomes selected as the active route, which may be
         * different from the route requested by {@link #selectRoute(RouteInfo)}. That can happen
         * when {@link MediaTransferReceiver media transfer feature} is enabled. The default
         * implementation calls {@link #onRouteSelected(MediaRouter, RouteInfo, int)} with the
         * actually selected route.
         *
         * @param router The media router reporting the event.
         * @param selectedRoute The route that has been selected.
         * @param reason The reason for unselecting the previous route.
         * @param requestedRoute The route that was requested to be selected.
         */
        public void onRouteSelected(
                @NonNull MediaRouter router,
                @NonNull RouteInfo selectedRoute,
                @UnselectReason int reason,
                @NonNull RouteInfo requestedRoute) {
            onRouteSelected(router, selectedRoute, reason);
        }

        /**
         * Called when the supplied media route becomes unselected as the active route. For detailed
         * reason, override {@link #onRouteUnselected(MediaRouter, RouteInfo, int)} instead.
         *
         * @param router The media router reporting the event.
         * @param route The route that has been unselected.
         * @deprecated Use {@link #onRouteUnselected(MediaRouter, RouteInfo, int)} instead.
         */
        @Deprecated
        public void onRouteUnselected(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when the supplied media route becomes unselected as the active route. The default
         * implementation calls {@link #onRouteUnselected}.
         *
         * <p>The reason provided will be one of the following:
         *
         * <ul>
         *   <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}
         *   <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}
         *   <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}
         *   <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}
         * </ul>
         *
         * @param router The media router reporting the event.
         * @param route The route that has been unselected.
         * @param reason The reason for unselecting the route.
         */
        public void onRouteUnselected(
                @NonNull MediaRouter router, @NonNull RouteInfo route, @UnselectReason int reason) {
            onRouteUnselected(router, route);
        }

        /**
         * Called when a media route has been added.
         *
         * @param router The media router reporting the event.
         * @param route The route that has become available for use.
         */
        public void onRouteAdded(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when a media route has been removed.
         *
         * @param router The media router reporting the event.
         * @param route The route that has been removed from availability.
         */
        public void onRouteRemoved(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when a property of the indicated media route has changed.
         *
         * @param router The media router reporting the event.
         * @param route The route that was changed.
         */
        public void onRouteChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when a media route's volume changes.
         *
         * @param router The media router reporting the event.
         * @param route The route whose volume changed.
         */
        public void onRouteVolumeChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when a media route's presentation display changes.
         *
         * <p>This method is called whenever the route's presentation display becomes available, is
         * removed or has changes to some of its properties (such as its size).
         *
         * @param router The media router reporting the event.
         * @param route The route whose presentation display changed.
         * @see RouteInfo#getPresentationDisplay()
         */
        public void onRoutePresentationDisplayChanged(
                @NonNull MediaRouter router, @NonNull RouteInfo route) {}

        /**
         * Called when a media route provider has been added.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that has become available for use.
         */
        public void onProviderAdded(@NonNull MediaRouter router, @NonNull ProviderInfo provider) {}

        /**
         * Called when a media route provider has been removed.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that has been removed from availability.
         */
        public void onProviderRemoved(
                @NonNull MediaRouter router, @NonNull ProviderInfo provider) {}

        /**
         * Called when a property of the indicated media route provider has changed.
         *
         * @param router The media router reporting the event.
         * @param provider The provider that was changed.
         */
        public void onProviderChanged(
                @NonNull MediaRouter router, @NonNull ProviderInfo provider) {}

        /** */
        @RestrictTo(LIBRARY)
        public void onRouterParamsChanged(
                @NonNull MediaRouter router, @Nullable MediaRouterParams params) {}
    }

    /**
     * Listener for receiving events when the selected route is about to be changed.
     *
     * @see #setOnPrepareTransferListener(OnPrepareTransferListener)
     */
    public interface OnPrepareTransferListener {
        /**
         * Implement this to handle transfer seamlessly.
         *
         * <p>Setting the listener will defer stopping the previous route, from which you may get
         * the media status to resume media seamlessly on the new route. When the transfer is
         * prepared, set the returned future to stop media being played on the previous route and
         * release resources. This method is called on the main thread.
         *
         * <p>{@link Callback#onRouteUnselected(MediaRouter, RouteInfo, int)} and {@link
         * Callback#onRouteSelected(MediaRouter, RouteInfo, int)} are called after the future is
         * done.
         *
         * @param fromRoute The route that is about to be unselected.
         * @param toRoute The route that is about to be selected.
         * @return A {@link ListenableFuture} whose completion indicates that the transfer is
         *     prepared or {@code null} to indicate that no preparation is needed. If a future is
         *     returned, until the future is completed, the media continues to be played on the
         *     previous route.
         */
        @MainThread
        @Nullable
        ListenableFuture<Void> onPrepareTransfer(
                @NonNull RouteInfo fromRoute, @NonNull RouteInfo toRoute);
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
         * @param data Result data, or null if none. Contents depend on the {@link
         *     MediaControlIntent media control action}.
         */
        public void onResult(@Nullable Bundle data) {}

        /**
         * Called when a media control request fails.
         *
         * @param error A localized error message which may be shown to the user, or null if the
         *     cause of the error is unclear.
         * @param data Error data, or null if none. Contents depend on the {@link MediaControlIntent
         *     media control action}.
         */
        public void onError(@Nullable String error, @Nullable Bundle data) {}
    }

    @RestrictTo(LIBRARY_GROUP)
    static final class CallbackRecord {
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
     * Class to notify events about transfer.
     */
    static final class PrepareTransferNotifier {
        private static final long TRANSFER_TIMEOUT_MS = 15_000;

        final RouteController mToRouteController;
        final @UnselectReason int mReason;
        private final boolean mSyncMediaRoute1Provider;
        private final RouteInfo mFromRoute;
        final RouteInfo mToRoute;
        private final RouteInfo mRequestedRoute;
        @Nullable
        final List<DynamicRouteDescriptor> mMemberRoutes;
        private final WeakReference<GlobalMediaRouter> mRouter;

        private ListenableFuture<Void> mFuture = null;
        private boolean mFinished = false;
        private boolean mCanceled = false;

        PrepareTransferNotifier(
                GlobalMediaRouter router,
                RouteInfo route,
                @Nullable RouteController routeController,
                @UnselectReason int reason,
                boolean syncMediaRoute1Provider,
                @Nullable RouteInfo requestedRoute,
                @Nullable Collection<DynamicRouteDescriptor> memberRoutes) {
            mRouter = new WeakReference<>(router);

            mToRoute = route;
            mToRouteController = routeController;
            mReason = reason;
            mSyncMediaRoute1Provider = syncMediaRoute1Provider;
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
        @MainThread
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
                router.mCallbackHandler.postRouteSelectedMessage(
                        mFromRoute, mToRoute, mReason, mSyncMediaRoute1Provider);
            } else {
                router.mCallbackHandler.postAnotherRouteSelectedMessage(
                        mRequestedRoute, mToRoute, mReason, mSyncMediaRoute1Provider);
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
