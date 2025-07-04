/*
 * Copyright 2024 The Android Open Source Project
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

package com.example.androidx.mediarouting.providers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteDiscoveryRequest;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Wraps routes from a different provider and publishes the resulting wrapper routes. */
public class WrapperMediaRouteProvider extends MediaRouteProvider {
    private static final String TAG = "WrapperMrp";

    /**
     * A custom media control intent category for special requests that are supported by this
     * provider's routes.
     */
    public static final String CATEGORY_WRAPPER_ROUTE =
            "com.example.androidx.media.CATEGORY_WRAPPER_ROUTE";

    private static final List<IntentFilter> WRAPPER_CONTROL_FILTERS;
    private static final String ROUTE_ID_SEPARATOR = ":";
    private static final String KEY_ORIGINAL_ROUTE_ID = "key_original_route_id";
    private static final String WRAPPER_MARKER = "wrapper";
    private static final String WRAPPER_ID_SUFFIX = "-" + WRAPPER_MARKER;
    private static final long MIN_INTERVAL_BETWEEN_ROUTE_PUBLICATIONS_MS =
            DateUtils.SECOND_IN_MILLIS / 10; // 0.1 second.
    private static final String[] ACTION_LIST =
            new String[] {
                MediaControlIntent.ACTION_PLAY,
                MediaControlIntent.ACTION_PAUSE,
                MediaControlIntent.ACTION_RESUME,
                MediaControlIntent.ACTION_STOP,
                MediaControlIntent.ACTION_SEEK,
                MediaControlIntent.ACTION_GET_STATUS,
                MediaControlIntent.ACTION_START_SESSION,
                MediaControlIntent.ACTION_GET_SESSION_STATUS,
                MediaControlIntent.ACTION_END_SESSION
            };

    private final MediaRouter mMediaRouter;
    private final OriginalMediaRouterCallback mOriginalMediaRouterCallback;

    private final Map<String, MediaRouteDescriptor> mOriginalDescriptorIdToWrapperRoutes =
            new HashMap<>();
    private final Map<String, WrapperDynamicGroupRouteController>
            mOriginalDescriptorIdToWrapperControllers = new HashMap<>();
    private final Handler mHandler;
    private Runnable mPublishRoutesRunnable;
    private long mLastPublishTimestampMs;

    static {
        WRAPPER_CONTROL_FILTERS = new ArrayList<>();
        IntentFilter wrapperControlFilter = new IntentFilter();
        wrapperControlFilter.addCategory(CATEGORY_WRAPPER_ROUTE);
        WRAPPER_CONTROL_FILTERS.add(wrapperControlFilter);
        for (String action : ACTION_LIST) {
            IntentFilter remotePlaybackFilter = new IntentFilter();
            remotePlaybackFilter.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
            remotePlaybackFilter.addAction(action);
            WRAPPER_CONTROL_FILTERS.add(remotePlaybackFilter);
        }
    }

    /**
     * Creates a media route provider to provide wrapper routes.
     *
     * @param context the context
     */
    public WrapperMediaRouteProvider(@NonNull Context context) {
        super(context);
        mMediaRouter = MediaRouter.getInstance(context);
        mOriginalMediaRouterCallback = new OriginalMediaRouterCallback();
        mHandler = new Handler();
    }

    @Override
    public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
        Log.i(TAG, "onDiscoveryRequestChanged with request: " + request);
        boolean shouldDiscoverDevices =
                request != null && request.getSelector().hasControlCategory(CATEGORY_WRAPPER_ROUTE);
        if (shouldDiscoverDevices) {
            int flags = request.isActiveScan() ? MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY : 0;
            mOriginalMediaRouterCallback.startScanning(flags);
        } else {
            mOriginalMediaRouterCallback.stopScanning();
        }
    }

    private void publishRoutes() {
        long timeSinceLastPublish = SystemClock.elapsedRealtime() - mLastPublishTimestampMs;
        if (timeSinceLastPublish < MIN_INTERVAL_BETWEEN_ROUTE_PUBLICATIONS_MS) {
            if (mPublishRoutesRunnable == null) {
                long delayToPublish =
                        MIN_INTERVAL_BETWEEN_ROUTE_PUBLICATIONS_MS - timeSinceLastPublish;
                mPublishRoutesRunnable = this::publishRoutesInternal;
                mHandler.postDelayed(mPublishRoutesRunnable, delayToPublish);
            }
            return;
        }
        publishRoutesInternal();
    }

    private void publishRoutesInternal() {
        mLastPublishTimestampMs = SystemClock.elapsedRealtime();
        mPublishRoutesRunnable = null;

        Collection<MediaRouteDescriptor> wrapperRoutes =
                mOriginalDescriptorIdToWrapperRoutes.values();
        MediaRouteProviderDescriptor.Builder builder =
                new MediaRouteProviderDescriptor.Builder()
                        .addRoutes(wrapperRoutes)
                        .setSupportsDynamicGroupRoute(true);
        setDescriptor(builder.build());
        logMediaRouteDescriptors(wrapperRoutes);
    }

    @Override
    public @Nullable RouteController onCreateRouteController(@NonNull String routeId) {
        Log.i(TAG, "onCreateRouteController with routeId: " + routeId);
        return new WrapperDynamicGroupMemberRouteController(routeId);
    }

    @Override
    public @Nullable DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId,
            @NonNull RouteControllerOptions routeControllerOptions) {
        Log.i(TAG, "onCreateDynamicGroupRouteController with routeId: " + initialMemberRouteId);
        MediaRouter.RouteInfo selectedRoute = getRouteWithDescriptorId(initialMemberRouteId);
        if (selectedRoute == null) {
            Log.i(TAG, "fail to create a wrapper route controller as selectedRoute is null");
            return null;
        }
        Bundle extraBundle = selectedRoute.getExtras();
        if (extraBundle == null) {
            Log.i(TAG, "fail to create a wrapper route controller as extra Bundle is null");
            return null;
        }
        String originalRouteId = extraBundle.getString(KEY_ORIGINAL_ROUTE_ID);
        if (originalRouteId == null) {
            Log.i(TAG, "fail to create a wrapper route controller as original route ID is null");
            return null;
        }
        MediaRouter.RouteInfo originalRoute = getRouteWithRouteId(originalRouteId);
        if (originalRoute == null) {
            Log.i(TAG, "fail to create a wrapper route controller as original route is null");
            return null;
        }

        Log.i(TAG, "create a wrapper route controller with original route: " + originalRoute);
        WrapperDynamicGroupRouteController controller =
                new WrapperDynamicGroupRouteController(originalRoute, initialMemberRouteId);
        String originalDescriptorId = getDescriptorId(originalRoute);
        mOriginalDescriptorIdToWrapperControllers.put(originalDescriptorId, controller);
        return controller;
    }

    private MediaRouter.@Nullable RouteInfo getRouteWithDescriptorId(String targetDescriptorId) {
        for (MediaRouter.RouteInfo route : mMediaRouter.getRoutes()) {
            String descriptorId = getDescriptorId(route);
            if (TextUtils.equals(descriptorId, targetDescriptorId)) {
                return route;
            }
        }
        return null;
    }

    private MediaRouter.@Nullable RouteInfo getRouteWithRouteId(String targetRouteId) {
        for (MediaRouter.RouteInfo route : mMediaRouter.getRoutes()) {
            if (TextUtils.equals(route.getId(), targetRouteId)) {
                return route;
            }
        }
        return null;
    }

    /** Logs a list of {@link MediaRouteDescriptor}. */
    private static void logMediaRouteDescriptors(Collection<MediaRouteDescriptor> routes) {
        Log.i(TAG, "Published " + routes.size() + " routes");
        int index = 0;
        for (MediaRouteDescriptor route : routes) {
            Log.d(TAG, "[" + index + "] " + routeLogString(route));
            index++;
        }
    }

    private static String routeLogString(MediaRouteDescriptor route) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(
                String.format(
                        Locale.ROOT,
                        "Route: %s (%s) [%s] ",
                        route.getName(),
                        route.getId(),
                        route.getDescription()));
        String controlFiltersString = controlFiltersToString(route.getControlFilters());
        if (!TextUtils.isEmpty(controlFiltersString)) {
            stringBuilder.append(controlFiltersString);
        }
        return stringBuilder.toString();
    }

    private static String controlFiltersToString(List<IntentFilter> controlFilters) {
        StringBuilder stringBuilder = new StringBuilder();
        for (IntentFilter filter : controlFilters) {
            Iterator<String> categoryIterator = filter.categoriesIterator();
            while (categoryIterator.hasNext()) {
                String category = categoryIterator.next();
                stringBuilder.append("/").append(category);
            }
        }
        return stringBuilder.toString();
    }

    private void logDynamicPublishedRoutes(List<DynamicRouteDescriptor> dynamicRoutes) {
        Log.i(TAG, "Published " + dynamicRoutes.size() + " dynamic routes");
        int index = 0;
        for (DynamicRouteDescriptor dynamicRoute : dynamicRoutes) {
            Log.i(TAG, "No. " + index + " " + dynamicRouteLogString(dynamicRoute));
            index++;
        }
    }

    private String dynamicRouteLogString(DynamicRouteDescriptor dynamicRoute) {
        MediaRouteDescriptor route = dynamicRoute.getRouteDescriptor();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(
                String.format(
                        Locale.ROOT,
                        "[%s] [%s] [%s] [%s] ",
                        routeInfoSelectionStateToString(dynamicRoute.getSelectionState()),
                        dynamicRoute.isTransferable() ? "Transferable" : "",
                        dynamicRoute.isGroupable() ? "Groupable" : "",
                        dynamicRoute.isUnselectable() ? "Unselectable" : ""));
        stringBuilder.append(WrapperMediaRouteProvider.routeLogString(route));
        return stringBuilder.toString();
    }

    private String routeInfoSelectionStateToString(int selectionState) {
        switch (selectionState) {
            case DynamicRouteDescriptor.UNSELECTING:
                return "UNSELECTING";
            case DynamicRouteDescriptor.UNSELECTED:
                return "UNSELECTED";
            case DynamicRouteDescriptor.SELECTING:
                return "SELECTING";
            case DynamicRouteDescriptor.SELECTED:
                return "SELECTED";
            default:
                return String.format(Locale.ROOT, "Unknown selection state(%d)", selectionState);
        }
    }

    private static String getDescriptorId(MediaRouter.RouteInfo route) {
        String routeId = route.getId();
        return routeId.contains(ROUTE_ID_SEPARATOR)
                ? routeId.substring(routeId.lastIndexOf(ROUTE_ID_SEPARATOR) + 1)
                : routeId;
    }

    private static String getWrapperDescriptorId(String originalDescriptorId) {
        return originalDescriptorId + WRAPPER_ID_SUFFIX;
    }

    private static String getOriginalDescriptorId(String wrapperDescriptorId) {
        return wrapperDescriptorId.endsWith(WRAPPER_ID_SUFFIX)
                ? wrapperDescriptorId.substring(
                        0, wrapperDescriptorId.length() - WRAPPER_ID_SUFFIX.length())
                : wrapperDescriptorId;
    }

    private static String getWrapperRouteName(String originalRouteName) {
        return originalRouteName + " [" + WRAPPER_MARKER + "]";
    }

    private @Nullable MediaRouteDescriptor getWrapperRouteDescriptor(
            MediaRouter.@NonNull RouteInfo originalRoute) {
        MediaRouteDescriptor originalRouteDescriptor = originalRoute.getMediaRouteDescriptor();
        if (originalRouteDescriptor == null) {
            return null;
        }
        MediaRouteDescriptor.Builder builder =
                getWrapperRouteDescriptorBuilder(originalRouteDescriptor);
        Bundle extrasBundle = new Bundle();
        extrasBundle.putString(KEY_ORIGINAL_ROUTE_ID, originalRoute.getId());
        builder.setExtras(extrasBundle);
        return builder.build();
    }

    private MediaRouteDescriptor.Builder getWrapperRouteDescriptorBuilder(
            MediaRouteDescriptor originalRouteDescriptor) {
        String wrapperDescriptorId = getWrapperDescriptorId(originalRouteDescriptor.getId());
        String wrapperRouteName = getWrapperRouteName(originalRouteDescriptor.getName());
        return new MediaRouteDescriptor.Builder(originalRouteDescriptor)
                .setId(wrapperDescriptorId)
                .setName(wrapperRouteName)
                .clearControlFilters()
                .addControlFilters(WRAPPER_CONTROL_FILTERS);
    }

    /** Tracks original route events. */
    private class OriginalMediaRouterCallback extends MediaRouter.Callback {

        private final MediaRouteSelector mSelector;

        OriginalMediaRouterCallback() {
            mSelector =
                    new MediaRouteSelector.Builder()
                            .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                            .build();
        }

        void startScanning(int flags) {
            mMediaRouter.addCallback(mSelector, this, flags);
            for (MediaRouter.RouteInfo route : mMediaRouter.getRoutes()) {
                if (route.matchesSelector(mSelector)) {
                    addOriginalRoute(route);
                }
            }
            publishRoutes();
        }

        void stopScanning() {
            mMediaRouter.addCallback(mSelector, this, /* flags= */ 0);
        }

        @Override
        public void onRouteAdded(
                @NonNull MediaRouter router, MediaRouter.@NonNull RouteInfo route) {
            addOriginalRoute(route);
            publishRoutes();
        }

        @Override
        public void onRouteChanged(
                @NonNull MediaRouter router, MediaRouter.@NonNull RouteInfo route) {
            addOriginalRoute(route);
            publishRoutes();

            for (WrapperDynamicGroupRouteController controller :
                    mOriginalDescriptorIdToWrapperControllers.values()) {
                controller.onOriginalRouteChanged(route);
            }
        }

        @Override
        public void onRouteRemoved(
                @NonNull MediaRouter router, MediaRouter.@NonNull RouteInfo route) {
            String originalDescriptorId = getDescriptorId(route);
            mOriginalDescriptorIdToWrapperRoutes.remove(originalDescriptorId);
            publishRoutes();
        }

        void addOriginalRoute(MediaRouter.@NonNull RouteInfo route) {
            if (route.isSelected() || route instanceof MediaRouter.GroupRouteInfo) {
                // The wrapper route provider only wraps discovered routes and it wouldn't wrap
                // selected routes or group routes.
                return;
            }
            if (route.getId().endsWith(WRAPPER_ID_SUFFIX)) {
                // Only wrap original routes.
                return;
            }
            String originalDescriptorId = getDescriptorId(route);
            MediaRouteDescriptor wrapperDescriptor = getWrapperRouteDescriptor(route);
            mOriginalDescriptorIdToWrapperRoutes.put(originalDescriptorId, wrapperDescriptor);
        }

        @Override
        public void onRouteConnected(
                @NonNull MediaRouter router,
                MediaRouter.@NonNull RouteInfo connectedRoute,
                MediaRouter.@NonNull RouteInfo requestedRoute) {
            String originalDescriptorId = getDescriptorId(requestedRoute);
            WrapperDynamicGroupRouteController controller =
                    mOriginalDescriptorIdToWrapperControllers.get(originalDescriptorId);
            if (controller != null) {
                controller.onOriginalRouteConnected(connectedRoute, requestedRoute);
            }
        }

        @Override
        public void onRouteDisconnected(
                @NonNull MediaRouter router,
                MediaRouter.@Nullable RouteInfo disconnectedRoute,
                MediaRouter.@NonNull RouteInfo requestedRoute,
                int reason) {
            String originalDescriptorId = getDescriptorId(requestedRoute);
            WrapperDynamicGroupRouteController controller =
                    mOriginalDescriptorIdToWrapperControllers.get(originalDescriptorId);
            if (controller != null) {
                controller.onOriginalRouteDisconnected(disconnectedRoute, requestedRoute, reason);
                mOriginalDescriptorIdToWrapperControllers.remove(originalDescriptorId);
            }
        }
    }

    /** Provides a {@link DynamicGroupRouteController} for the wrapper media route provider. */
    private class WrapperDynamicGroupRouteController extends DynamicGroupRouteController {
        private final MediaRouter.@NonNull RouteInfo mOriginalRoute;
        private final String mRequestedWrapperDescriptorId;
        private MediaRouter.@Nullable GroupRouteInfo mGroupRoute;
        private @Nullable MediaRouteDescriptor mWrapperGroupRouteDescriptor;

        WrapperDynamicGroupRouteController(
                MediaRouter.@NonNull RouteInfo originalRoute, String requestedWrapperDescriptorId) {
            mOriginalRoute = originalRoute;
            mRequestedWrapperDescriptorId = requestedWrapperDescriptorId;
        }

        /** Called when the dynamic group route controller is selected. */
        @Override
        public void onSelect() {
            Log.i(
                    TAG,
                    "onSelect the original route = "
                            + mOriginalRoute
                            + ", mRequestedWrapperDescriptorId = "
                            + mRequestedWrapperDescriptorId);

            // Notify the initial wrapper group route descriptor and wrapper dynamic routes.
            MediaRouter.RouteInfo requestedWrapperRoute =
                    getRouteWithDescriptorId(mRequestedWrapperDescriptorId);
            if (requestedWrapperRoute == null) {
                Log.i(TAG, "requestedWrapperRoute is null");
                return;
            }
            mWrapperGroupRouteDescriptor = requestedWrapperRoute.getMediaRouteDescriptor();
            if (mWrapperGroupRouteDescriptor == null) {
                Log.i(TAG, "mWrapperGroupRouteDescriptor is null");
                return;
            }
            notifyDynamicRoutesChanged(mWrapperGroupRouteDescriptor, new ArrayList<>());

            mOriginalRoute.connect();
        }

        /** Called when the dynamic group route controller is unselected. */
        @Override
        public void onUnselect(int reason) {
            Log.i(TAG, "onUnselect with reason = " + reason);
            mOriginalRoute.disconnect();
        }

        /** Called when the dynamic group route controller is released. */
        @Override
        public void onRelease() {
            Log.i(TAG, "onRelease with original route = " + mOriginalRoute);
        }

        @Override
        public boolean onControlRequest(
                @NonNull Intent intent, MediaRouter.@Nullable ControlRequestCallback callback) {
            if (mGroupRoute == null) {
                return false;
            }
            Log.i(
                    TAG,
                    "group onControlRequest with intent = " + intent + ", callback = " + callback);
            mGroupRoute.sendControlRequest(intent, callback);
            return true;
        }

        /** Called when the group volume of the dynamic group route is set to the given value. */
        @Override
        public void onSetVolume(int volume) {
            if (mGroupRoute == null) {
                return;
            }
            Log.i(TAG, "onSetVolume with volume = " + volume);
            mGroupRoute.requestSetVolume(volume);
        }

        /**
         * Called when the group volume of the dynamic group route is updated with the given value.
         */
        @Override
        public void onUpdateVolume(int delta) {
            if (mGroupRoute == null) {
                return;
            }
            Log.i(TAG, "onUpdateVolume with delta = " + delta);
            mGroupRoute.requestSetVolume(delta);
        }

        void onOriginalRouteConnected(
                MediaRouter.@NonNull RouteInfo connectedRoute,
                MediaRouter.@NonNull RouteInfo requestedRoute) {
            if (!(connectedRoute instanceof MediaRouter.GroupRouteInfo)) {
                return;
            }
            mGroupRoute = (MediaRouter.GroupRouteInfo) connectedRoute;
            MediaRouter.RouteInfo selectedRoute = mMediaRouter.getSelectedRoute();
            Log.i(
                    TAG,
                    "onOriginalRouteConnected with mGroupRoute = "
                            + mGroupRoute
                            + ", requestedRoute = "
                            + requestedRoute
                            + ", selectedRoute= "
                            + selectedRoute);
            updateDynamicGroupRoute();
        }

        void onOriginalRouteDisconnected(
                MediaRouter.@Nullable RouteInfo disconnectedRoute,
                MediaRouter.@NonNull RouteInfo requestedRoute,
                int reason) {
            if (mOriginalRoute != requestedRoute) {
                return;
            }
            Log.i(
                    TAG,
                    "onOriginalRouteDisconnected with disconnectedRoute = "
                            + disconnectedRoute
                            + ", requestedRoute = "
                            + requestedRoute
                            + ", reason = "
                            + reason);
            mGroupRoute = null;
        }

        void onOriginalRouteChanged(MediaRouter.@NonNull RouteInfo route) {
            if (!(route instanceof MediaRouter.GroupRouteInfo)) {
                return;
            }
            MediaRouter.GroupRouteInfo groupRoute = (MediaRouter.GroupRouteInfo) route;
            if (mGroupRoute == null
                    || !groupRoute.isConnected()
                    || !TextUtils.equals(groupRoute.getId(), mGroupRoute.getId())) {
                return;
            }
            mGroupRoute = groupRoute;
            Log.i(TAG, "onOriginalRouteChanged with route = " + route);
            updateDynamicGroupRoute();
        }

        private void updateDynamicGroupRoute() {
            if (mGroupRoute == null) {
                return;
            }
            MediaRouteDescriptor originalGroupRouteDescriptor =
                    mGroupRoute.getMediaRouteDescriptor();
            if (originalGroupRouteDescriptor == null) {
                return;
            }
            mWrapperGroupRouteDescriptor =
                    getWrapperRouteDescriptorBuilder(originalGroupRouteDescriptor).build();

            List<DynamicRouteDescriptor> wrapperDynamicRoutes = getWrapperDynamicRoutes();
            notifyDynamicRoutesChanged(mWrapperGroupRouteDescriptor, wrapperDynamicRoutes);
            logDynamicPublishedRoutes(wrapperDynamicRoutes);
        }

        private List<DynamicRouteDescriptor> getWrapperDynamicRoutes() {
            if (mGroupRoute == null) {
                return new ArrayList<>();
            }
            List<DynamicRouteDescriptor> wrapperDynamicRoutes = new ArrayList<>();
            for (MediaRouter.RouteInfo route : mGroupRoute.getRoutesInGroup()) {
                MediaRouteDescriptor originalRouteDescriptor = route.getMediaRouteDescriptor();
                if (originalRouteDescriptor == null) {
                    continue;
                }
                String originalDescriptorId = originalRouteDescriptor.getId();
                MediaRouteDescriptor wrapperRouteDescriptor =
                        mOriginalDescriptorIdToWrapperRoutes.get(originalDescriptorId);
                if (wrapperRouteDescriptor == null) {
                    continue;
                }
                DynamicRouteDescriptor wrapperDynamicRouteDescriptor =
                        new DynamicRouteDescriptor.Builder(wrapperRouteDescriptor)
                                .setSelectionState(mGroupRoute.getSelectionState(route))
                                .setIsGroupable(mGroupRoute.isGroupable(route))
                                .setIsUnselectable(mGroupRoute.isUnselectable(route))
                                .build();
                wrapperDynamicRoutes.add(wrapperDynamicRouteDescriptor);
            }
            return wrapperDynamicRoutes;
        }

        /** Called when a user selects a new set of routes they want the session to be played. */
        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
            // This will not be invoked because the routes from this provider are not transferable.
            throw new UnsupportedOperationException("onUpdateMemberRoutes should never be called");
        }

        /** Called when a user adds a route into the session. */
        @Override
        public void onAddMemberRoute(@NonNull String routeId) {
            if (mGroupRoute == null) {
                return;
            }
            Log.i(TAG, "onAddMemberRoute with routeId = " + routeId);
            String originalDescriptorId = getOriginalDescriptorId(routeId);
            MediaRouter.RouteInfo originalRoute = getRouteWithDescriptorId(originalDescriptorId);
            if (originalRoute == null) {
                return;
            }
            mGroupRoute.addRoute(originalRoute);
        }

        /** Called when a user removes a route from the session. */
        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {
            if (mGroupRoute == null) {
                return;
            }
            Log.i(TAG, "onRemoveMemberRoute with routeId = " + routeId);
            String originalDescriptorId = getOriginalDescriptorId(routeId);
            MediaRouter.RouteInfo originalRoute = getRouteWithDescriptorId(originalDescriptorId);
            if (originalRoute == null) {
                return;
            }
            mGroupRoute.removeRoute(originalRoute);
        }
    }

    /** Provides a {@link RouteController} for controlling a wrapper member route. */
    private class WrapperDynamicGroupMemberRouteController extends RouteController {
        private final String mRouteId;
        private MediaRouter.RouteInfo mMemberRoute;

        WrapperDynamicGroupMemberRouteController(String routeId) {
            mRouteId = routeId;
        }

        @Override
        public void onSelect() {
            Log.i(TAG, "onSelect member route with ID: " + mRouteId);
            String memberDescriptorId = getOriginalDescriptorId(mRouteId);
            mMemberRoute = getRouteWithDescriptorId(memberDescriptorId);
        }

        @Override
        public void onUnselect() {
            Log.i(TAG, "onUnselect member route with ID: " + mRouteId);
            mMemberRoute = null;
        }

        @Override
        public boolean onControlRequest(
                @NonNull Intent intent, MediaRouter.@Nullable ControlRequestCallback callback) {
            if (mMemberRoute != null) {
                Log.i(
                        TAG,
                        "member onControlRequest with intent = "
                                + intent
                                + ", callback = "
                                + callback);
                mMemberRoute.sendControlRequest(intent, callback);
                return true;
            }
            return false;
        }

        @Override
        public void onSetVolume(int volume) {
            if (mMemberRoute != null) {
                Log.i(
                        TAG,
                        "onSetVolume to volume = " + volume + " for member route ID: " + mRouteId);
                mMemberRoute.requestSetVolume(volume);
            }
        }

        @Override
        public void onUpdateVolume(int delta) {
            if (mMemberRoute != null) {
                Log.i(
                        TAG,
                        "onUpdateVolume to delta = " + delta + " for member route ID: " + mRouteId);
                mMemberRoute.requestUpdateVolume(delta);
            }
        }
    }
}
