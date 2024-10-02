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

import static androidx.mediarouter.media.MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE;
import static androidx.mediarouter.media.MediaRouter.AVAILABILITY_FLAG_REQUIRE_MATCH;
import static androidx.mediarouter.media.MediaRouter.CALLBACK_FLAG_FORCE_DISCOVERY;
import static androidx.mediarouter.media.MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN;
import static androidx.mediarouter.media.MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY;
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_ROUTE_CHANGED;
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_STOPPED;
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_UNKNOWN;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.display.DisplayManagerCompat;
import androidx.core.util.Pair;
import androidx.media.VolumeProviderCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Global state for the media router.
 *
 * <p>Media routes and media route providers are global to the process; their state and the bulk of
 * the media router implementation lives here.
 */
/* package */ final class GlobalMediaRouter
        implements PlatformMediaRouter1RouteProvider.SyncCallback,
                RegisteredMediaRouteProviderWatcher.Callback {

    static final String TAG = MediaRouter.TAG;
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    final CallbackHandler mCallbackHandler = new CallbackHandler();
    // A map from unique route ID to RouteController for the member routes in the currently
    // selected route group.
    final Map<String, MediaRouteProvider.RouteController> mRouteControllerMap = new HashMap<>();

    @VisibleForTesting
    RegisteredMediaRouteProviderWatcher mRegisteredProviderWatcher;
    MediaRouter.RouteInfo mSelectedRoute;
    MediaRouteProvider.RouteController mSelectedRouteController;
    MediaRouter.OnPrepareTransferListener mOnPrepareTransferListener;
    MediaRouter.PrepareTransferNotifier mTransferNotifier;

    private final Context mApplicationContext;
    private final ArrayList<WeakReference<MediaRouter>> mRouters = new ArrayList<>();
    private final ArrayList<MediaRouter.RouteInfo> mRoutes = new ArrayList<>();
    private final Map<Pair<String, String>, String> mUniqueIdMap = new HashMap<>();
    private final ArrayList<MediaRouter.ProviderInfo> mProviders = new ArrayList<>();
    private final ArrayList<RemoteControlClientRecord> mRemoteControlClients = new ArrayList<>();
    private final RemoteControlClientCompat.PlaybackInfo mPlaybackInfo =
            new RemoteControlClientCompat.PlaybackInfo();
    private final ProviderCallback mProviderCallback = new ProviderCallback();
    private final boolean mLowRam;
    private final boolean mTransferReceiverDeclared;

    private boolean mUseMediaRouter2ForSystemRouting;
    private MediaRoute2Provider mMr2Provider;
    private PlatformMediaRouter1RouteProvider mPlatformMediaRouter1RouteProvider;
    private DisplayManagerCompat mDisplayManager;
    private MediaRouterActiveScanThrottlingHelper mActiveScanThrottlingHelper;
    private MediaRouterParams mRouterParams;
    private MediaRouter.RouteInfo mDefaultRoute;
    private MediaRouter.RouteInfo mBluetoothRoute;
    // Represents a route that are requested to be selected asynchronously.
    private MediaRouter.RouteInfo mRequestedRoute;
    private MediaRouteProvider.RouteController mRequestedRouteController;
    private MediaRouteDiscoveryRequest mDiscoveryRequest;
    private MediaRouteDiscoveryRequest mDiscoveryRequestForMr2Provider;
    private int mCallbackCount;
    private MediaSessionRecord mMediaSession;
    private MediaSessionCompat mCompatSession;

    /* package */ GlobalMediaRouter(Context applicationContext) {
        mApplicationContext = applicationContext;
        mLowRam =
                ActivityManagerCompat.isLowRamDevice(
                        (ActivityManager)
                                applicationContext.getSystemService(Context.ACTIVITY_SERVICE));

        mTransferReceiverDeclared =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        && MediaTransferReceiver.isDeclared(mApplicationContext);
        mUseMediaRouter2ForSystemRouting =
                SystemRoutingUsingMediaRouter2Receiver.isDeclared(mApplicationContext);

        if (DEBUG && mUseMediaRouter2ForSystemRouting) {
            // This is only added to skip the presubmit check for UnusedVariable
            // TODO: Remove it once mUseMediaRouter2ForSystemRouting is actually used
            Log.d(TAG, "Using MediaRouter2 for system routing");
        }

        mMr2Provider =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && mTransferReceiverDeclared
                        ? new MediaRoute2Provider(mApplicationContext, new Mr2ProviderCallback())
                        : null;

        // Add the platform media router 1 route provider for interoperating with the framework
        // android.media.MediaRouter. This one is special and receives synchronization messages
        // from the media router.
        mPlatformMediaRouter1RouteProvider =
                PlatformMediaRouter1RouteProvider.obtain(mApplicationContext, this);
        start();
    }

    private void start() {
        mActiveScanThrottlingHelper =
                new MediaRouterActiveScanThrottlingHelper(this::updateDiscoveryRequest);
        addProvider(
                mPlatformMediaRouter1RouteProvider, /* treatRouteDescriptorIdsAsUnique= */ true);
        if (mMr2Provider != null) {
            addProvider(mMr2Provider, /* treatRouteDescriptorIdsAsUnique= */ true);
        }

        // Start watching for routes published by registered media route
        // provider services.
        mRegisteredProviderWatcher =
                new RegisteredMediaRouteProviderWatcher(mApplicationContext, this);
        mRegisteredProviderWatcher.start();
    }

    /* package */ void reset() {
        mActiveScanThrottlingHelper.reset();

        setRouteListingPreference(null);
        setMediaSessionCompat(null);

        mRegisteredProviderWatcher.stop();

        for (RemoteControlClientRecord record : mRemoteControlClients) {
            record.disconnect();
        }

        List<MediaRouter.ProviderInfo> providers = new ArrayList<>(mProviders);
        for (MediaRouter.ProviderInfo providerInfo : providers) {
            removeProvider(providerInfo.mProviderInstance);
        }
        mCallbackHandler.removeCallbacksAndMessages(null);
    }

    /* package */ MediaRouter getRouter(Context context) {
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
        mRouters.add(new WeakReference<>(router));
        return router;
    }

    /* package */ ContentResolver getContentResolver() {
        return mApplicationContext.getContentResolver();
    }

    /* package */ Display getDisplay(int displayId) {
        if (mDisplayManager == null) {
            mDisplayManager = DisplayManagerCompat.getInstance(mApplicationContext);
        }
        return mDisplayManager.getDisplay(displayId);
    }

    /* package */ void sendControlRequest(
            MediaRouter.RouteInfo route,
            Intent intent,
            MediaRouter.ControlRequestCallback callback) {
        if (route == mSelectedRoute && mSelectedRouteController != null) {
            if (mSelectedRouteController.onControlRequest(intent, callback)) {
                return;
            }
        }
        if (mTransferNotifier != null
                && route == mTransferNotifier.mToRoute
                && mTransferNotifier.mToRouteController != null) {
            if (mTransferNotifier.mToRouteController.onControlRequest(intent, callback)) {
                return;
            }
        }
        if (callback != null) {
            callback.onError(null, null);
        }
    }

    /* package */ void requestSetVolume(MediaRouter.RouteInfo route, int volume) {
        if (route == mSelectedRoute && mSelectedRouteController != null) {
            mSelectedRouteController.onSetVolume(volume);
        } else {
            MediaRouteProvider.RouteController controller =
                    mRouteControllerMap.get(route.mUniqueId);
            if (controller != null) {
                controller.onSetVolume(volume);
            }
        }
    }

    /* package */ void requestUpdateVolume(MediaRouter.RouteInfo route, int delta) {
        if (route == mSelectedRoute && mSelectedRouteController != null) {
            mSelectedRouteController.onUpdateVolume(delta);
        } else {
            MediaRouteProvider.RouteController controller =
                    mRouteControllerMap.get(route.mUniqueId);
            if (controller != null) {
                controller.onUpdateVolume(delta);
            }
        }
    }

    /* package */ MediaRouter.RouteInfo getRoute(String uniqueId) {
        for (MediaRouter.RouteInfo info : mRoutes) {
            if (info.mUniqueId.equals(uniqueId)) {
                return info;
            }
        }
        return null;
    }

    /* package */ List<MediaRouter.RouteInfo> getRoutes() {
        return mRoutes;
    }

    @Nullable
        /* package */ MediaRouterParams getRouterParams() {
        return mRouterParams;
    }

    // isMediaTransferEnabled() is true only on R+ device.
    @SuppressLint("NewApi")
    /* package */ void setRouterParams(@Nullable MediaRouterParams params) {
        MediaRouterParams oldParams = mRouterParams;
        mRouterParams = params;

        if (isMediaTransferEnabled()) {
            if (mMr2Provider == null) {
                mMr2Provider =
                        new MediaRoute2Provider(mApplicationContext, new Mr2ProviderCallback());
                addProvider(mMr2Provider, /* treatRouteDescriptorIdsAsUnique= */ true);
                // Make sure mDiscoveryRequestForMr2Provider is updated
                updateDiscoveryRequest();
                mRegisteredProviderWatcher.rescan();
            }

            boolean oldTransferToLocalEnabled =
                    oldParams != null && oldParams.isTransferToLocalEnabled();
            boolean newTransferToLocalEnabled = params != null && params.isTransferToLocalEnabled();

            if (oldTransferToLocalEnabled != newTransferToLocalEnabled) {
                // Since the discovery request itself is not changed,
                // call setDiscoveryRequestInternal to avoid the equality check.
                mMr2Provider.setDiscoveryRequestInternal(mDiscoveryRequestForMr2Provider);
            }
        } else {
            if (mMr2Provider != null) {
                removeProvider(mMr2Provider);
                mMr2Provider = null;
                mRegisteredProviderWatcher.rescan();
            }
        }
        mCallbackHandler.post(CallbackHandler.MSG_ROUTER_PARAMS_CHANGED, params);
    }

    /* package */ void setRouteListingPreference(
            @Nullable RouteListingPreference routeListingPreference) {
        if (mMr2Provider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mMr2Provider.setRouteListingPreference(routeListingPreference);
        }
    }

    @NonNull
        /* package */ List<MediaRouter.ProviderInfo> getProviders() {
        return mProviders;
    }

    @NonNull
        /* package */ MediaRouter.RouteInfo getDefaultRoute() {
        if (mDefaultRoute == null) {
            // This should never happen once the media router has been fully
            // initialized but it is good to check for the error in case there
            // is a bug in provider initialization.
            throw new IllegalStateException(
                    "There is no default route.  "
                            + "The media router has not yet been fully initialized.");
        }
        return mDefaultRoute;
    }

    /* package */ MediaRouter.RouteInfo getBluetoothRoute() {
        return mBluetoothRoute;
    }

    @NonNull
        /* package */ MediaRouter.RouteInfo getSelectedRoute() {
        if (mSelectedRoute == null) {
            // This should never happen once the media router has been fully
            // initialized but it is good to check for the error in case there
            // is a bug in provider initialization.
            throw new IllegalStateException(
                    "There is no currently selected route.  "
                            + "The media router has not yet been fully initialized.");
        }
        return mSelectedRoute;
    }

    @Nullable
        /* package */ MediaRouter.RouteInfo.DynamicGroupState getDynamicGroupState(
            MediaRouter.RouteInfo route) {
        return mSelectedRoute.getDynamicGroupState(route);
    }

    /* package */ void addMemberToDynamicGroup(@NonNull MediaRouter.RouteInfo route) {
        if (!(mSelectedRouteController instanceof MediaRouteProvider.DynamicGroupRouteController)) {
            throw new IllegalStateException("There is no currently selected dynamic group route.");
        }
        MediaRouter.RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
        if (mSelectedRoute.getMemberRoutes().contains(route)
                || state == null
                || !state.isGroupable()) {
            Log.w(TAG, "Ignoring attempt to add a non-groupable route to dynamic group : " + route);
            return;
        }
        ((MediaRouteProvider.DynamicGroupRouteController) mSelectedRouteController)
                .onAddMemberRoute(route.getDescriptorId());
    }

    /* package */ void removeMemberFromDynamicGroup(@NonNull MediaRouter.RouteInfo route) {
        if (!(mSelectedRouteController instanceof MediaRouteProvider.DynamicGroupRouteController)) {
            throw new IllegalStateException("There is no currently selected dynamic group route.");
        }
        MediaRouter.RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
        if (!mSelectedRoute.getMemberRoutes().contains(route)
                || state == null
                || !state.isUnselectable()) {
            Log.w(TAG, "Ignoring attempt to remove a non-unselectable member route : " + route);
            return;
        }
        if (mSelectedRoute.getMemberRoutes().size() <= 1) {
            Log.w(TAG, "Ignoring attempt to remove the last member route.");
            return;
        }
        ((MediaRouteProvider.DynamicGroupRouteController) mSelectedRouteController)
                .onRemoveMemberRoute(route.getDescriptorId());
    }

    /* package */ void transferToRoute(@NonNull MediaRouter.RouteInfo route) {
        if (!(mSelectedRouteController instanceof MediaRouteProvider.DynamicGroupRouteController)) {
            throw new IllegalStateException("There is no currently selected dynamic group route.");
        }
        MediaRouter.RouteInfo.DynamicGroupState state = getDynamicGroupState(route);
        if (state == null || !state.isTransferable()) {
            Log.w(TAG, "Ignoring attempt to transfer to a non-transferable route.");
            return;
        }
        ((MediaRouteProvider.DynamicGroupRouteController) mSelectedRouteController)
                .onUpdateMemberRoutes(Collections.singletonList(route.getDescriptorId()));
    }

    /* package */ void selectRoute(
            @NonNull MediaRouter.RouteInfo route,
            @MediaRouter.UnselectReason int unselectReason,
            boolean syncMediaRoute1Provider) {
        if (!mRoutes.contains(route)) {
            Log.w(TAG, "Ignoring attempt to select removed route: " + route);
            return;
        }
        if (!route.mEnabled) {
            Log.w(TAG, "Ignoring attempt to select disabled route: " + route);
            return;
        }

        // Check whether the route comes from MediaRouter2. The SDK check is required to avoid a
        // lint error but is not needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && route.getProviderInstance() == mMr2Provider
                && mSelectedRoute != route) {
            mMr2Provider.transferTo(route.getDescriptorId());
        } else {
            selectRouteInternal(route, unselectReason, syncMediaRoute1Provider);
        }
    }

    /* package */ boolean isRouteAvailable(MediaRouteSelector selector, int flags) {
        if (selector.isEmpty()) {
            return false;
        }

        // On low-RAM devices, do not rely on actual discovery results unless asked to.
        if ((flags & AVAILABILITY_FLAG_REQUIRE_MATCH) == 0 && mLowRam) {
            return true;
        }

        boolean useOutputSwitcher =
                mRouterParams != null
                        && mRouterParams.isOutputSwitcherEnabled()
                        && isMediaTransferEnabled();
        // Check whether any existing routes match the selector.
        final int routeCount = mRoutes.size();
        for (int i = 0; i < routeCount; i++) {
            MediaRouter.RouteInfo route = mRoutes.get(i);
            if ((flags & AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE) != 0
                    && route.isDefaultOrBluetooth()) {
                continue;
            }
            // When using the output switcher, we only care about MR2 routes and system routes.
            if (useOutputSwitcher
                    && !route.isDefaultOrBluetooth()
                    && route.getProviderInstance() != mMr2Provider) {
                continue;
            }
            if (route.matchesSelector(selector)) {
                return true;
            }
        }

        // It doesn't look like we can find a matching route right now.
        return false;
    }

    /* package */ void updateDiscoveryRequest() {
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
                    MediaRouter.CallbackRecord callback = router.mCallbackRecords.get(j);
                    builder.addSelector(callback.mSelector);
                    boolean callbackRequestingActiveScan =
                            (callback.mFlags & CALLBACK_FLAG_PERFORM_ACTIVE_SCAN) != 0;
                    mActiveScanThrottlingHelper.requestActiveScan(
                            callbackRequestingActiveScan, callback.mTimestamp);
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
            Log.i(
                    TAG,
                    "Forcing passive route discovery on a low-RAM device, "
                            + "system performance may be affected.  Please consider using "
                            + "CALLBACK_FLAG_REQUEST_DISCOVERY instead of "
                            + "CALLBACK_FLAG_FORCE_DISCOVERY.");
        }

        // Notify providers.
        for (MediaRouter.ProviderInfo providerInfo : mProviders) {
            MediaRouteProvider provider = providerInfo.mProviderInstance;
            if (provider == mMr2Provider) {
                // MediaRoute2Provider is handled by updateMr2ProviderDiscoveryRequest().
                continue;
            }
            provider.setDiscoveryRequest(mDiscoveryRequest);
        }
    }

    private void updateMr2ProviderDiscoveryRequest(
            @NonNull MediaRouteSelector selector, boolean activeScan) {
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
            mDiscoveryRequestForMr2Provider = new MediaRouteDiscoveryRequest(selector, activeScan);
        }
        if (DEBUG) {
            Log.d(
                    TAG,
                    "Updated MediaRoute2Provider's discovery request: "
                            + mDiscoveryRequestForMr2Provider);
        }

        mMr2Provider.setDiscoveryRequest(mDiscoveryRequestForMr2Provider);
    }

    /* package */ int getCallbackCount() {
        return mCallbackCount;
    }

    /* package */ boolean isMediaTransferEnabled() {
        // The default value for isMediaTransferReceiverEnabled() is {@code true}.
        return mTransferReceiverDeclared
                && (mRouterParams == null || mRouterParams.isMediaTransferReceiverEnabled());
    }

    /* package */ boolean isTransferToLocalEnabled() {
        if (mRouterParams == null) {
            return false;
        }
        return mRouterParams.isTransferToLocalEnabled();
    }

    /**  */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    /* package */ boolean isGroupVolumeUxEnabled() {
        return mRouterParams == null
                || mRouterParams.mExtras == null
                || mRouterParams.mExtras.getBoolean(MediaRouterParams.ENABLE_GROUP_VOLUME_UX, true);
    }

    @Override
    public void addProvider(@NonNull MediaRouteProvider providerInstance) {
        addProvider(providerInstance, /* treatRouteDescriptorIdsAsUnique= */ false);
    }

    private void addProvider(
            @NonNull MediaRouteProvider providerInstance, boolean treatRouteDescriptorIdsAsUnique) {
        if (findProviderInfo(providerInstance) == null) {
            // 1. Add the provider to the list.
            MediaRouter.ProviderInfo provider =
                    new MediaRouter.ProviderInfo(providerInstance, treatRouteDescriptorIdsAsUnique);
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
    public void removeProvider(@NonNull MediaRouteProvider providerInstance) {
        MediaRouter.ProviderInfo provider = findProviderInfo(providerInstance);
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
    public void releaseProviderController(
            @NonNull RegisteredMediaRouteProvider provider,
            @NonNull MediaRouteProvider.RouteController controller) {
        if (mSelectedRouteController == controller) {
            selectRoute(
                    chooseFallbackRoute(),
                    UNSELECT_REASON_STOPPED,
                    /* syncMediaRoute1Provider= */ true);
        }
        // TODO: Maybe release a member route controller if the given controller is a member of
        // the selected route.
    }

    /* package */ void updateProviderDescriptor(
            MediaRouteProvider providerInstance, MediaRouteProviderDescriptor descriptor) {
        MediaRouter.ProviderInfo provider = findProviderInfo(providerInstance);
        if (provider != null) {
            // Update the provider's contents.
            updateProviderContents(provider, descriptor);
        }
    }

    private MediaRouter.ProviderInfo findProviderInfo(MediaRouteProvider providerInstance) {
        for (MediaRouter.ProviderInfo providerInfo : mProviders) {
            if (providerInfo.mProviderInstance == providerInstance) {
                return providerInfo;
            }
        }
        return null;
    }

    private void updateProviderContents(
            MediaRouter.ProviderInfo provider, MediaRouteProviderDescriptor providerDescriptor) {
        if (!provider.updateDescriptor(providerDescriptor)) {
            // Nothing to update.
            return;
        }
        // Update all existing routes and reorder them to match
        // the order of their descriptors.
        int targetIndex = 0;
        boolean selectedRouteDescriptorChanged = false;
        if (providerDescriptor != null
                && (providerDescriptor.isValid()
                        || providerDescriptor
                                == mPlatformMediaRouter1RouteProvider.getDescriptor())) {
            final List<MediaRouteDescriptor> routeDescriptors = providerDescriptor.getRoutes();
            // Updating route group's contents requires all member routes' information.
            // Add the groups to the lists and update them later.
            List<Pair<MediaRouter.RouteInfo, MediaRouteDescriptor>> addedGroups = new ArrayList<>();
            List<Pair<MediaRouter.RouteInfo, MediaRouteDescriptor>> updatedGroups =
                    new ArrayList<>();
            for (MediaRouteDescriptor routeDescriptor : routeDescriptors) {
                // PlatformMediaRouter1RouteProvider may have invalid routes
                if (routeDescriptor == null || !routeDescriptor.isValid()) {
                    Log.w(TAG, "Ignoring invalid route descriptor: " + routeDescriptor);
                    continue;
                }
                final String id = routeDescriptor.getId();
                final int sourceIndex = provider.findRouteIndexByDescriptorId(id);

                if (sourceIndex < 0) {
                    // 1. Add the route to the list.
                    String uniqueId = assignRouteUniqueId(provider, id);
                    MediaRouter.RouteInfo route =
                            new MediaRouter.RouteInfo(
                                    provider, id, uniqueId, routeDescriptor.isSystemRoute());

                    provider.mRoutes.add(targetIndex++, route);
                    mRoutes.add(route);
                    // 2. Create the route's contents.
                    if (!routeDescriptor.getGroupMemberIds().isEmpty()) {
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
                    Log.w(TAG, "Ignoring route descriptor with duplicate id: " + routeDescriptor);
                } else {
                    MediaRouter.RouteInfo route = provider.mRoutes.get(sourceIndex);
                    // 1. Reorder the route within the list.
                    Collections.swap(provider.mRoutes, sourceIndex, targetIndex++);
                    // 2. Update the route's contents.
                    if (!routeDescriptor.getGroupMemberIds().isEmpty()) {
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
            for (Pair<MediaRouter.RouteInfo, MediaRouteDescriptor> pair : addedGroups) {
                MediaRouter.RouteInfo route = pair.first;
                route.maybeUpdateDescriptor(pair.second);
                if (DEBUG) {
                    Log.d(TAG, "Route added: " + route);
                }
                mCallbackHandler.post(CallbackHandler.MSG_ROUTE_ADDED, route);
            }
            for (Pair<MediaRouter.RouteInfo, MediaRouteDescriptor> pair : updatedGroups) {
                MediaRouter.RouteInfo route = pair.first;
                if (updateRouteDescriptorAndNotify(route, pair.second) != 0) {
                    if (route == mSelectedRoute) {
                        selectedRouteDescriptorChanged = true;
                    }
                }
            }
        } else {
            String message =
                    providerDescriptor != null
                            ? "Ignoring invalid provider descriptor: " + providerDescriptor
                            : "Ignoring null provider descriptor from "
                                    + provider.getComponentName();
            Log.w(TAG, message);
        }

        // Dispose all remaining routes that do not have matching descriptors.
        for (int i = provider.mRoutes.size() - 1; i >= targetIndex; i--) {
            // 1. Delete the route's contents.
            MediaRouter.RouteInfo route = provider.mRoutes.get(i);
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
            MediaRouter.RouteInfo route = provider.mRoutes.remove(i);
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

    /* package */ int updateRouteDescriptorAndNotify(
            MediaRouter.RouteInfo route, MediaRouteDescriptor routeDescriptor) {
        int changes = route.maybeUpdateDescriptor(routeDescriptor);
        if (changes != 0) {
            if ((changes & MediaRouter.RouteInfo.CHANGE_GENERAL) != 0) {
                if (DEBUG) {
                    Log.d(TAG, "Route changed: " + route);
                }
                mCallbackHandler.post(CallbackHandler.MSG_ROUTE_CHANGED, route);
            }
            if ((changes & MediaRouter.RouteInfo.CHANGE_VOLUME) != 0) {
                if (DEBUG) {
                    Log.d(TAG, "Route volume changed: " + route);
                }
                mCallbackHandler.post(CallbackHandler.MSG_ROUTE_VOLUME_CHANGED, route);
            }
            if ((changes & MediaRouter.RouteInfo.CHANGE_PRESENTATION_DISPLAY) != 0) {
                if (DEBUG) {
                    Log.d(TAG, "Route presentation display changed: " + route);
                }
                mCallbackHandler.post(
                        CallbackHandler.MSG_ROUTE_PRESENTATION_DISPLAY_CHANGED, route);
            }
        }
        return changes;
    }

    /* package */ String assignRouteUniqueId(
            MediaRouter.ProviderInfo provider, String routeDescriptorId) {
        // Although route descriptor ids are unique within a provider, it's
        // possible for there to be two providers with the same package name.
        // Therefore we must dedupe the composite id.
        String componentName = provider.getComponentName().flattenToShortString();
        String uniqueId =
                provider.mTreatRouteDescriptorIdsAsUnique
                        ? routeDescriptorId
                        : (componentName + ":" + routeDescriptorId);
        if (provider.mTreatRouteDescriptorIdsAsUnique || findRouteByUniqueId(uniqueId) < 0) {
            mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), uniqueId);
            return uniqueId;
        }
        Log.w(
                TAG,
                "Either "
                        + routeDescriptorId
                        + " isn't unique in "
                        + componentName
                        + " or we're trying to assign a unique ID for an already added route");
        int i = 2;
        while (true) {
            String newUniqueId = String.format(Locale.US, "%s_%d", uniqueId, i);
            if (findRouteByUniqueId(newUniqueId) < 0) {
                mUniqueIdMap.put(new Pair<>(componentName, routeDescriptorId), newUniqueId);
                return newUniqueId;
            }
            i++;
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

    /* package */ String getUniqueId(MediaRouter.ProviderInfo provider, String routeDescriptorId) {
        String componentName = provider.getComponentName().flattenToShortString();
        return mUniqueIdMap.get(new Pair<>(componentName, routeDescriptorId));
    }

    /* package */ void updateSelectedRouteIfNeeded(boolean selectedRouteDescriptorChanged) {
        // Update default route.
        if (mDefaultRoute != null && !mDefaultRoute.isSelectable()) {
            Log.i(
                    TAG,
                    "Clearing the default route because it "
                            + "is no longer selectable: "
                            + mDefaultRoute);
            mDefaultRoute = null;
        }
        if (mDefaultRoute == null) {
            for (MediaRouter.RouteInfo route : mRoutes) {
                if (isSystemDefaultRoute(route) && route.isSelectable()) {
                    mDefaultRoute = route;
                    Log.i(TAG, "Found default route: " + mDefaultRoute);
                    break;
                }
            }
        }

        // Update bluetooth route.
        if (mBluetoothRoute != null && !mBluetoothRoute.isSelectable()) {
            Log.i(
                    TAG,
                    "Clearing the bluetooth route because it "
                            + "is no longer selectable: "
                            + mBluetoothRoute);
            mBluetoothRoute = null;
        }
        if (mBluetoothRoute == null) {
            for (MediaRouter.RouteInfo route : mRoutes) {
                if (isSystemLiveAudioOnlyRoute(route) && route.isSelectable()) {
                    mBluetoothRoute = route;
                    Log.i(TAG, "Found bluetooth route: " + mBluetoothRoute);
                    break;
                }
            }
        }

        // Update selected route.
        if (mSelectedRoute == null || !mSelectedRoute.isEnabled()) {
            Log.i(
                    TAG,
                    "Unselecting the current route because it "
                            + "is no longer selectable: "
                            + mSelectedRoute);
            // TODO: b/294968421 - Consider passing a false syncMediaRoute1Provider. This could help
            // with the prevention of setBluetoothA2dpOn(false) bugs, but it could also leave the
            // platform MediaRouter in an inconsistent state. In order to change
            // syncMediaRoute1Provider to false, we need to assess the impact of not calling
            // android.media.MediaRouter.selectRoute as a result of this method call.
            selectRouteInternal(
                    chooseFallbackRoute(),
                    UNSELECT_REASON_UNKNOWN,
                    /* syncMediaRoute1Provider= */ true);
        } else if (selectedRouteDescriptorChanged) {
            // In case the selected route is a route group, select/unselect route controllers
            // for the added/removed route members.
            maybeUpdateMemberRouteControllers();
            updatePlaybackInfoFromSelectedRoute();
        }
    }

    /* package */ MediaRouter.RouteInfo chooseFallbackRoute() {
        // When the current route is removed or no longer selectable,
        // we want to revert to a live audio route if there is
        // one (usually Bluetooth A2DP).  Failing that, use
        // the default route.
        for (MediaRouter.RouteInfo route : mRoutes) {
            if (route != mDefaultRoute
                    && isSystemLiveAudioOnlyRoute(route)
                    && route.isSelectable()) {
                return route;
            }
        }
        return mDefaultRoute;
    }

    private boolean isSystemLiveAudioOnlyRoute(MediaRouter.RouteInfo route) {
        return route.getProviderInstance() == mPlatformMediaRouter1RouteProvider
                && route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                && !route.supportsControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO);
    }

    private boolean isSystemDefaultRoute(MediaRouter.RouteInfo route) {
        return route.getProviderInstance() == mPlatformMediaRouter1RouteProvider
                && route.mDescriptorId.equals(PlatformMediaRouter1RouteProvider.DEFAULT_ROUTE_ID);
    }

    /* package */ void selectRouteInternal(
            @NonNull MediaRouter.RouteInfo route,
            @MediaRouter.UnselectReason int unselectReason,
            boolean syncMediaRoute1Provider) {
        if (mSelectedRoute == route) {
            return;
        }

        // TODO: b/294968421 - Remove the following logging.
        // We don't call isDefaultRoute or getDefaultRoute as those rely on the global media router
        // being initialized, which is not guaranteed to have happened yet.
        boolean targetIsDefaultRoute = route == mDefaultRoute;
        if (mBluetoothRoute != null && targetIsDefaultRoute) {
            StackTraceElement[] callStack = Thread.currentThread().getStackTrace();
            StringBuilder readableStacktraceBuilder = new StringBuilder();
            readableStacktraceBuilder.append("- Stracktrace: [");
            // callStack[3] is the caller of this method.
            for (int i = 3; i < callStack.length; i++) {
                StackTraceElement caller = callStack[i];
                readableStacktraceBuilder
                        .append(caller.getClassName())
                        .append(".")
                        .append(caller.getMethodName())
                        .append(":")
                        .append(caller.getLineNumber());
                if (i + 1 < callStack.length) {
                    readableStacktraceBuilder.append(", ");
                }
            }
            readableStacktraceBuilder.append("]");
            String selectedRouteString =
                    mSelectedRoute != null
                            ? String.format(
                                    Locale.US,
                                    "%s(BT=%b)",
                                    mSelectedRoute.getName(),
                                    mSelectedRoute.isBluetooth())
                            : null;
            Log.w(
                    TAG,
                    "Changing selection("
                            + selectedRouteString
                            + ") to default while BT is "
                            + "available: pkgName="
                            + mApplicationContext.getPackageName()
                            + readableStacktraceBuilder);
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

        // TODO: determine how to enable dynamic grouping on pre-R devices.
        if (isMediaTransferEnabled() && route.getProvider().supportsDynamicGroup()) {
            MediaRouteProvider.DynamicGroupRouteController dynamicGroupRouteController =
                    route.getProviderInstance()
                            .onCreateDynamicGroupRouteController(
                                    route.mDescriptorId, /* controlHints= */ null);
            // Select route asynchronously.
            if (dynamicGroupRouteController != null) {
                dynamicGroupRouteController.setOnDynamicRoutesChangedListener(
                        ContextCompat.getMainExecutor(mApplicationContext), mDynamicRoutesListener);
                mRequestedRoute = route;
                mRequestedRouteController = dynamicGroupRouteController;
                mRequestedRouteController.onSelect();
                return;
            } else {
                Log.w(
                        TAG,
                        "setSelectedRouteInternal: Failed to create dynamic group route "
                                + "controller. route="
                                + route);
            }
        }

        MediaRouteProvider.RouteController routeController =
                route.getProviderInstance().onCreateRouteController(route.mDescriptorId);
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
            mCallbackHandler.postRouteSelectedMessage(
                    /* fromRoute= */ null,
                    /* targetRoute= */ route,
                    unselectReason,
                    syncMediaRoute1Provider);
        } else {
            notifyTransfer(
                    this,
                    route,
                    routeController,
                    unselectReason,
                    syncMediaRoute1Provider,
                    /* requestedRoute= */ null,
                    /* memberRoutes= */ null);
        }
    }

    /* package */ void maybeUpdateMemberRouteControllers() {
        if (!mSelectedRoute.isGroup()) {
            return;
        }
        List<MediaRouter.RouteInfo> routes = mSelectedRoute.getMemberRoutes();
        // Build a set of descriptor IDs for the new route group.
        Set<String> idSet = new HashSet<>();
        for (MediaRouter.RouteInfo route : routes) {
            idSet.add(route.mUniqueId);
        }
        // Unselect route controllers for the removed routes.
        Iterator<Map.Entry<String, MediaRouteProvider.RouteController>> iter =
                mRouteControllerMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, MediaRouteProvider.RouteController> entry = iter.next();
            if (!idSet.contains(entry.getKey())) {
                MediaRouteProvider.RouteController controller = entry.getValue();
                controller.onUnselect(UNSELECT_REASON_UNKNOWN);
                controller.onRelease();
                iter.remove();
            }
        }
        // Select route controllers for the added routes.
        for (MediaRouter.RouteInfo route : routes) {
            if (!mRouteControllerMap.containsKey(route.mUniqueId)) {
                MediaRouteProvider.RouteController controller =
                        route.getProviderInstance()
                                .onCreateRouteController(
                                        route.mDescriptorId, mSelectedRoute.mDescriptorId);
                if (controller != null) {
                    controller.onSelect();
                    mRouteControllerMap.put(route.mUniqueId, controller);
                }
            }
        }
    }

    /* package */ void notifyTransfer(
            GlobalMediaRouter router,
            MediaRouter.RouteInfo route,
            @Nullable MediaRouteProvider.RouteController routeController,
            @MediaRouter.UnselectReason int reason,
            boolean syncMediaRoute1Provider,
            @Nullable MediaRouter.RouteInfo requestedRoute,
            @Nullable
                    Collection<
                                    MediaRouteProvider.DynamicGroupRouteController
                                            .DynamicRouteDescriptor>
                            memberRoutes) {
        if (mTransferNotifier != null) {
            mTransferNotifier.cancel();
            mTransferNotifier = null;
        }
        mTransferNotifier =
                new MediaRouter.PrepareTransferNotifier(
                        router,
                        route,
                        routeController,
                        reason,
                        syncMediaRoute1Provider,
                        requestedRoute,
                        memberRoutes);

        if (mTransferNotifier.mReason != UNSELECT_REASON_ROUTE_CHANGED
                || mOnPrepareTransferListener == null) {
            mTransferNotifier.finishTransfer();
        } else {
            ListenableFuture<Void> future =
                    mOnPrepareTransferListener.onPrepareTransfer(
                            mSelectedRoute, mTransferNotifier.mToRoute);
            if (future == null) {
                mTransferNotifier.finishTransfer();
            } else {
                mTransferNotifier.setFuture(future);
            }
        }
    }

    /* package */ MediaRouteProvider.DynamicGroupRouteController.OnDynamicRoutesChangedListener
            mDynamicRoutesListener =
                    new MediaRouteProvider.DynamicGroupRouteController
                            .OnDynamicRoutesChangedListener() {
                        @Override
                        public void onRoutesChanged(
                                @NonNull MediaRouteProvider.DynamicGroupRouteController controller,
                                @Nullable MediaRouteDescriptor groupRouteDescriptor,
                                @NonNull
                                        Collection<
                                                        MediaRouteProvider
                                                                .DynamicGroupRouteController
                                                                .DynamicRouteDescriptor>
                                                routes) {
                            if (controller == mRequestedRouteController
                                    && groupRouteDescriptor != null) {
                                MediaRouter.ProviderInfo provider = mRequestedRoute.getProvider();
                                String groupId = groupRouteDescriptor.getId();

                                String uniqueId = assignRouteUniqueId(provider, groupId);
                                MediaRouter.RouteInfo route =
                                        new MediaRouter.RouteInfo(provider, groupId, uniqueId);
                                route.maybeUpdateDescriptor(groupRouteDescriptor);

                                if (mSelectedRoute == route) {
                                    return;
                                }

                                notifyTransfer(
                                        GlobalMediaRouter.this,
                                        route,
                                        mRequestedRouteController,
                                        UNSELECT_REASON_ROUTE_CHANGED,
                                        /* syncMediaRoute1Provider= */ true,
                                        mRequestedRoute,
                                        routes);

                                mRequestedRoute = null;
                                mRequestedRouteController = null;
                            } else if (controller == mSelectedRouteController) {
                                if (groupRouteDescriptor != null) {
                                    updateRouteDescriptorAndNotify(
                                            mSelectedRoute, groupRouteDescriptor);
                                }
                                mSelectedRoute.updateDynamicDescriptors(routes);
                            }
                        }
                    };

    @Override
    public void onPlatformRouteSelectedByDescriptorId(@NonNull String id) {
        // System route is selected, do not sync the route we selected before.
        mCallbackHandler.removeMessages(CallbackHandler.MSG_ROUTE_SELECTED);
        MediaRouter.ProviderInfo provider = findProviderInfo(mPlatformMediaRouter1RouteProvider);
        if (provider != null) {
            MediaRouter.RouteInfo route = provider.findRouteByDescriptorId(id);
            if (route != null) {
                route.select();
            }
        }
    }

    /* package */ void addRemoteControlClient(android.media.RemoteControlClient rcc) {
        int index = findRemoteControlClientRecord(rcc);
        if (index < 0) {
            RemoteControlClientRecord record = new RemoteControlClientRecord(rcc);
            mRemoteControlClients.add(record);
        }
    }

    /* package */ void removeRemoteControlClient(android.media.RemoteControlClient rcc) {
        int index = findRemoteControlClientRecord(rcc);
        if (index >= 0) {
            RemoteControlClientRecord record = mRemoteControlClients.remove(index);
            record.disconnect();
        }
    }

    /* package */ void setMediaSession(Object session) {
        setMediaSessionRecord(session != null ? new MediaSessionRecord(session) : null);
    }

    /* package */ void setMediaSessionCompat(final MediaSessionCompat session) {
        mCompatSession = session;
        setMediaSessionRecord(session != null ? new MediaSessionRecord(session) : null);
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

    /* package */ MediaSessionCompat.Token getMediaSessionToken() {
        if (mMediaSession != null) {
            return mMediaSession.getToken();
        } else if (mCompatSession != null) {
            return mCompatSession.getSessionToken();
        }
        return null;
    }

    private int findRemoteControlClientRecord(android.media.RemoteControlClient rcc) {
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
            if (isMediaTransferEnabled() && mSelectedRoute.getProviderInstance() == mMr2Provider) {
                mPlaybackInfo.volumeControlId =
                        MediaRoute2Provider.getSessionIdForRouteController(
                                mSelectedRouteController);
            } else {
                mPlaybackInfo.volumeControlId = null;
            }

            for (RemoteControlClientRecord remoteControlClientRecord : mRemoteControlClients) {
                remoteControlClientRecord.updatePlaybackInfo();
            }
            if (mMediaSession != null) {
                if (mSelectedRoute == getDefaultRoute() || mSelectedRoute == getBluetoothRoute()) {
                    // Local route
                    mMediaSession.clearVolumeHandling();
                } else {
                    @VolumeProviderCompat.ControlType
                    int controlType = VolumeProviderCompat.VOLUME_CONTROL_FIXED;
                    if (mPlaybackInfo.volumeHandling
                            == MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE) {
                        controlType = VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE;
                    }
                    mMediaSession.configureVolume(
                            controlType,
                            mPlaybackInfo.volumeMax,
                            mPlaybackInfo.volume,
                            mPlaybackInfo.volumeControlId);
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
        public void onDescriptorChanged(
                @NonNull MediaRouteProvider provider, MediaRouteProviderDescriptor descriptor) {
            updateProviderDescriptor(provider, descriptor);
        }
    }

    /* package */ final class Mr2ProviderCallback extends MediaRoute2Provider.Callback {
        @Override
        public void onSelectRoute(
                @NonNull String routeDescriptorId, @MediaRouter.UnselectReason int reason) {
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
                Log.w(
                        TAG,
                        "onSelectRoute: The target RouteInfo is not found for descriptorId="
                                + routeDescriptorId);
                return;
            }

            // TODO: b/294968421 - Consider passing a false syncMediaRoute1Provider. This could help
            // with the prevention of setBluetoothA2dpOn(false) bugs, but it could also leave the
            // platform MediaRouter in an inconsistent state. In order to change
            // syncMediaRoute1Provider to false, we need to assess the impact of not calling
            // android.media.MediaRouter.selectRoute as a result of this method call.
            selectRouteInternal(routeToSelect, reason, /* syncMediaRoute1Provider */ true);
        }

        @Override
        public void onSelectFallbackRoute(@MediaRouter.UnselectReason int reason) {
            selectRouteToFallbackRoute(reason);
        }

        @Override
        public void onReleaseController(@NonNull MediaRouteProvider.RouteController controller) {
            if (controller == mSelectedRouteController) {
                // Stop casting
                selectRouteToFallbackRoute(UNSELECT_REASON_STOPPED);
            } else if (DEBUG) {
                // 'Cast -> Phone' / 'Cast -> Cast(old)' cases triggered by selectRoute().
                // Nothing to do.
                Log.d(
                        TAG,
                        "A RouteController unrelated to the selected route is released."
                                + " controller="
                                + controller);
            }
        }

        /* package */ void selectRouteToFallbackRoute(@MediaRouter.UnselectReason int reason) {
            MediaRouter.RouteInfo fallbackRoute = chooseFallbackRoute();
            if (getSelectedRoute() != fallbackRoute) {
                selectRouteInternal(fallbackRoute, reason, /* syncMediaRoute1Provider */ true);
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

        /* package */ void configureVolume(
                @VolumeProviderCompat.ControlType int controlType,
                int max,
                int current,
                @Nullable String volumeControlId) {
            if (mMsCompat != null) {
                if (mVpCompat != null && controlType == mControlType && max == mMaxVolume) {
                    // If we haven't changed control type or max just set the
                    // new current volume
                    mVpCompat.setCurrentVolume(current);
                } else {
                    // Otherwise create a new provider and update
                    mVpCompat =
                            new VolumeProviderCompat(controlType, max, current, volumeControlId) {
                                @Override
                                public void onSetVolumeTo(final int volume) {
                                    mCallbackHandler.post(
                                            () -> {
                                                if (mSelectedRoute != null) {
                                                    mSelectedRoute.requestSetVolume(volume);
                                                }
                                            });
                                }

                                @Override
                                public void onAdjustVolume(final int direction) {
                                    mCallbackHandler.post(
                                            () -> {
                                                if (mSelectedRoute != null) {
                                                    mSelectedRoute.requestUpdateVolume(direction);
                                                }
                                            });
                                }
                            };
                    mMsCompat.setPlaybackToRemote(mVpCompat);
                }
            }
        }

        /* package */ void clearVolumeHandling() {
            if (mMsCompat != null) {
                mMsCompat.setPlaybackToLocal(mPlaybackInfo.playbackStream);
                mVpCompat = null;
            }
        }

        /* package */ MediaSessionCompat.Token getToken() {
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

        RemoteControlClientRecord(android.media.RemoteControlClient rcc) {
            mRccCompat = RemoteControlClientCompat.obtain(mApplicationContext, rcc);
            mRccCompat.setVolumeCallback(this);
            updatePlaybackInfo();
        }

        /* package */ android.media.RemoteControlClient getRemoteControlClient() {
            return mRccCompat.getRemoteControlClient();
        }

        /* package */ void disconnect() {
            mDisconnected = true;
            mRccCompat.setVolumeCallback(null);
        }

        /* package */ void updatePlaybackInfo() {
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    /* package */ final class CallbackHandler extends Handler {
        private final ArrayList<MediaRouter.CallbackRecord> mTempCallbackRecords =
                new ArrayList<>();
        private final List<MediaRouter.RouteInfo> mDynamicGroupRoutes = new ArrayList<>();

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

        /* package */ void postRouteSelectedMessage(
                @Nullable MediaRouter.RouteInfo fromRoute,
                @NonNull MediaRouter.RouteInfo targetRoute,
                int reason,
                boolean syncMediaRoute1Provider) {
            RouteSelectedMessageParams params =
                    new RouteSelectedMessageParams(fromRoute, targetRoute, syncMediaRoute1Provider);
            Message message = obtainMessage(MSG_ROUTE_SELECTED, params);
            message.arg1 = reason;
            message.sendToTarget();
        }

        /* package */ void postAnotherRouteSelectedMessage(
                @Nullable MediaRouter.RouteInfo requestedRoute,
                @NonNull MediaRouter.RouteInfo targetRoute,
                int reason,
                boolean syncMediaRoute1Provider) {
            RouteSelectedMessageParams params =
                    new RouteSelectedMessageParams(
                            requestedRoute, targetRoute, syncMediaRoute1Provider);
            Message message = obtainMessage(MSG_ROUTE_ANOTHER_SELECTED, params);
            message.arg1 = reason;
            message.sendToTarget();
        }

        /* package */ void post(int msg, Object obj) {
            obtainMessage(msg, obj).sendToTarget();
        }

        /* package */ void post(int msg, Object obj, int arg) {
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
                    && getSelectedRoute().getId().equals(((MediaRouter.RouteInfo) obj).getId())) {
                updateSelectedRouteIfNeeded(true);
            }

            // Synchronize state with the platform media router.
            syncWithPlatformMediaRouter1RouteProvider(what, obj);

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

                for (MediaRouter.CallbackRecord tempCallbackRecord : mTempCallbackRecords) {
                    invokeCallback(tempCallbackRecord, what, obj, arg);
                }
            } finally {
                mTempCallbackRecords.clear();
            }
        }

        // Using Pair<RouteInfo, RouteInfo>
        @SuppressWarnings({"unchecked"})
        private void syncWithPlatformMediaRouter1RouteProvider(int what, Object obj) {
            switch (what) {
                case MSG_ROUTE_ADDED:
                    mPlatformMediaRouter1RouteProvider.onSyncRouteAdded(
                            (MediaRouter.RouteInfo) obj);
                    break;
                case MSG_ROUTE_REMOVED:
                    mPlatformMediaRouter1RouteProvider.onSyncRouteRemoved(
                            (MediaRouter.RouteInfo) obj);
                    break;
                case MSG_ROUTE_CHANGED:
                    mPlatformMediaRouter1RouteProvider.onSyncRouteChanged(
                            (MediaRouter.RouteInfo) obj);
                    break;
                case MSG_ROUTE_SELECTED: {
                    RouteSelectedMessageParams params = (RouteSelectedMessageParams) obj;
                    MediaRouter.RouteInfo selectedRoute = params.mTargetRoute;
                    if (params.mSyncMediaRoute1Provider) {
                        mPlatformMediaRouter1RouteProvider.onSyncRouteSelected(selectedRoute);
                    }
                    // TODO(b/166794092): Remove this nullness check
                    if (mDefaultRoute != null && selectedRoute.isDefaultOrBluetooth()) {
                        for (MediaRouter.RouteInfo prevGroupRoute : mDynamicGroupRoutes) {
                            mPlatformMediaRouter1RouteProvider.onSyncRouteRemoved(prevGroupRoute);
                        }
                        mDynamicGroupRoutes.clear();
                    }
                    break;
                }
                case MSG_ROUTE_ANOTHER_SELECTED: {
                    RouteSelectedMessageParams params = (RouteSelectedMessageParams) obj;
                    MediaRouter.RouteInfo groupRoute = params.mTargetRoute;
                    mDynamicGroupRoutes.add(groupRoute);
                    mPlatformMediaRouter1RouteProvider.onSyncRouteAdded(groupRoute);
                    if (params.mSyncMediaRoute1Provider) {
                        mPlatformMediaRouter1RouteProvider.onSyncRouteSelected(groupRoute);
                    }
                    break;
                }
            }
        }

        private void invokeCallback(
                MediaRouter.CallbackRecord record, int what, Object obj, int arg) {
            final MediaRouter router = record.mRouter;
            final MediaRouter.Callback callback = record.mCallback;
            switch (what & MSG_TYPE_MASK) {
                case MSG_TYPE_ROUTE: {
                    RouteSelectedMessageParams selectedMessageParams =
                                what == MSG_ROUTE_ANOTHER_SELECTED || what == MSG_ROUTE_SELECTED
                                        ? ((RouteSelectedMessageParams) obj)
                                        : null;
                    final MediaRouter.RouteInfo route =
                                selectedMessageParams != null
                                        ? selectedMessageParams.mTargetRoute
                                        : (MediaRouter.RouteInfo) obj;
                    final MediaRouter.RouteInfo optionalRoute =
                                selectedMessageParams != null
                                        ? selectedMessageParams.mFromOrRequestedRoute
                                        : null;
                    if (route == null
                            || !record.filterRouteEvent(route, what, optionalRoute, arg)) {
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
                    final MediaRouter.ProviderInfo provider = (MediaRouter.ProviderInfo) obj;
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

    /**
     * Holds the parameters of {@link CallbackHandler#MSG_ROUTE_SELECTED} and {@link
     * CallbackHandler#MSG_ROUTE_ANOTHER_SELECTED}.
     */
    private static final class RouteSelectedMessageParams {
        /**
         * Holds the origin route for {@link CallbackHandler#MSG_ROUTE_SELECTED}, or the originally
         * requested route for {@link CallbackHandler#MSG_ROUTE_ANOTHER_SELECTED}.
         */
        @Nullable public final MediaRouter.RouteInfo mFromOrRequestedRoute;

        @NonNull public final MediaRouter.RouteInfo mTargetRoute;

        public final boolean mSyncMediaRoute1Provider;

        private RouteSelectedMessageParams(
                @Nullable MediaRouter.RouteInfo fromOrRequestedRoute,
                @NonNull MediaRouter.RouteInfo targetRoute,
                boolean syncMediaRoute1Provider) {
            mFromOrRequestedRoute = fromOrRequestedRoute;
            mTargetRoute = targetRoute;
            mSyncMediaRoute1Provider = syncMediaRoute1Provider;
        }
    }
}
