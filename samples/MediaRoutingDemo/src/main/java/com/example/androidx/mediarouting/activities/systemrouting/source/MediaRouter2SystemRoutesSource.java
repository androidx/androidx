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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter2}. */
@RequiresApi(Build.VERSION_CODES.R)
public final class MediaRouter2SystemRoutesSource extends SystemRoutesSource {

    @NonNull private final Context mContext;
    @NonNull private final MediaRouter2 mMediaRouter2;
    @Nullable private final Method mSuitabilityStatusMethod;
    @Nullable private final Method mWasTransferInitiatedBySelfMethod;
    @Nullable private final Method mTransferReasonMethod;
    @NonNull private final ArrayList<SystemRouteItem> mRouteItems = new ArrayList<>();

    @NonNull
    private final MediaRouter2.RouteCallback mRouteCallback =
            new MediaRouter2.RouteCallback() {
                @Override
                public void onRoutesUpdated(@NonNull List<MediaRoute2Info> routes) {
                    populateRouteItems(routes);
                }
            };

    @NonNull
    private final MediaRouter2.ControllerCallback mControllerCallback =
            new MediaRouter2.ControllerCallback() {
                @Override
                public void onControllerUpdated(
                        @NonNull MediaRouter2.RoutingController unusedController) {
                    populateRouteItems(mMediaRouter2.getRoutes());
                }
            };

    /** Returns a new instance. */
    @NonNull
    public static MediaRouter2SystemRoutesSource create(@NonNull Context context) {
        MediaRouter2 mediaRouter2 = MediaRouter2.getInstance(context);
        return new MediaRouter2SystemRoutesSource(context, mediaRouter2);
    }

    MediaRouter2SystemRoutesSource(@NonNull Context context,
            @NonNull MediaRouter2 mediaRouter2) {
        mContext = context;
        mMediaRouter2 = mediaRouter2;

        Method suitabilityStatusMethod = null;
        Method wasTransferInitiatedBySelfMethod = null;
        Method transferReasonMethod = null;
        // TODO: b/336510942 - Remove reflection once these APIs are available in
        // androidx-platform-dev.
        try {
            suitabilityStatusMethod =
                    MediaRoute2Info.class.getDeclaredMethod("getSuitabilityStatus");
            wasTransferInitiatedBySelfMethod =
                    MediaRouter2.RoutingController.class.getDeclaredMethod(
                            "wasTransferInitiatedBySelf");
            transferReasonMethod = RoutingSessionInfo.class.getDeclaredMethod("getTransferReason");
        } catch (NoSuchMethodException | IllegalAccessError e) {
        }
        mSuitabilityStatusMethod = suitabilityStatusMethod;
        mWasTransferInitiatedBySelfMethod = wasTransferInitiatedBySelfMethod;
        mTransferReasonMethod = transferReasonMethod;
    }

    @Override
    public void start() {
        RouteDiscoveryPreference routeDiscoveryPreference =
                new RouteDiscoveryPreference.Builder(
                                /* preferredFeatures= */ Arrays.asList(
                                        MediaRoute2Info.FEATURE_LIVE_AUDIO,
                                        MediaRoute2Info.FEATURE_LIVE_VIDEO),
                                /* activeScan= */ false)
                        .build();

        Executor mainExecutor = mContext.getMainExecutor();
        mMediaRouter2.registerRouteCallback(mainExecutor, mRouteCallback, routeDiscoveryPreference);
        mMediaRouter2.registerControllerCallback(mainExecutor, mControllerCallback);
        populateRouteItems(mMediaRouter2.getRoutes());
    }

    @Override
    public void stop() {
        mMediaRouter2.unregisterControllerCallback(mControllerCallback);
        mMediaRouter2.unregisterRouteCallback(mRouteCallback);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem(/* name= */ "MediaRouter2");
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        return mRouteItems;
    }

    @Override
    public boolean select(@NonNull SystemRouteItem item) {
        Optional<MediaRoute2Info> route =
                mMediaRouter2.getRoutes().stream()
                        .filter(it -> it.getId().equals(item.mId))
                        .findFirst();
        if (!route.isPresent()) {
            return false;
        } else {
            mMediaRouter2.transferTo(route.get());
            return true;
        }
    }

    // BanUncheckedReflection: See b/336510942 for details on why reflection is needed.
    // NewApi: We don't need to check the API level because the transfer reason method is only
    // available on API 35, which is greater than API 34, where getRoutingSessionInfo was added.
    @SuppressLint({"BanUncheckedReflection", "NewApi"})
    private void populateRouteItems(List<MediaRoute2Info> routes) {
        MediaRouter2.RoutingController systemController = mMediaRouter2.getSystemController();
        Set<String> selectedRoutesIds =
                systemController.getSelectedRoutes().stream()
                        .map(MediaRoute2Info::getId)
                        .collect(Collectors.toSet());
        Boolean selectionInitiatedBySelf = null;
        Integer sessionTransferReason = null;
        try {
            if (mSuitabilityStatusMethod != null) {
                selectionInitiatedBySelf =
                        (Boolean) mWasTransferInitiatedBySelfMethod.invoke(systemController);
            }
            if (mTransferReasonMethod != null) {
                sessionTransferReason =
                        (Integer)
                                mTransferReasonMethod.invoke(
                                        Api34Impl.getRoutingSessionInfo(systemController));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
        // We need to filter out non-system routes, which might be reported as a result of other
        // callbacks with non-system route features being registered in the router.
        List<MediaRoute2Info> systemRoutes =
                routes.stream().filter(MediaRoute2Info::isSystemRoute).collect(Collectors.toList());

        mRouteItems.clear();
        for (MediaRoute2Info route : systemRoutes) {
            boolean isSelectedRoute = selectedRoutesIds.contains(route.getId());
            Boolean wasTransferredBySelf = isSelectedRoute ? selectionInitiatedBySelf : null;
            Integer routeTransferReason = isSelectedRoute ? sessionTransferReason : null;
            mRouteItems.add(
                    createRouteItemFor(
                            route, isSelectedRoute, wasTransferredBySelf, routeTransferReason));
        }
        mOnRoutesChangedListener.run();
    }

    @NonNull
    private SystemRouteItem createRouteItemFor(
            @NonNull MediaRoute2Info routeInfo,
            boolean isSelectedRoute,
            @Nullable Boolean wasTransferredBySelf,
            @Nullable Integer transferReason) {
        SystemRouteItem.Builder builder =
                new SystemRouteItem.Builder(getSourceId(), routeInfo.getId())
                        .setName(String.valueOf(routeInfo.getName()))
                        .setSelectionSupportState(
                                isSelectedRoute
                                        ? SystemRouteItem.SelectionSupportState.RESELECTABLE
                                        : SystemRouteItem.SelectionSupportState.SELECTABLE)
                        .setDescription(String.valueOf(routeInfo.getDescription()))
                        .setTransferInitiatedBySelf(wasTransferredBySelf)
                        .setTransferReason(getHumanReadableTransferReason(transferReason));
        try {
            if (mSuitabilityStatusMethod != null) {
                // See b/336510942 for details on why reflection is needed.
                @SuppressLint("BanUncheckedReflection")
                int status = (Integer) mSuitabilityStatusMethod.invoke(routeInfo);
                builder.setSuitabilityStatus(getHumanReadableSuitabilityStatus(status));
                // TODO: b/319645714 - Populate wasTransferInitiatedBySelf. For that we need to
                // change the implementation of this class to use the routing controller instead
                // of a route callback.
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
        return builder.build();
    }

    @NonNull
    private String getHumanReadableSuitabilityStatus(@Nullable Integer status) {
        if (status == null) {
            // The route is not selected, or this Android version doesn't support suitability
            // status.
            return null;
        }
        switch (status) {
            case 0:
                return "SUITABLE_FOR_DEFAULT_TRANSFER";
            case 1:
                return "SUITABLE_FOR_MANUAL_TRANSFER";
            case 2:
                return "NOT_SUITABLE_FOR_TRANSFER";
            default:
                return "UNKNOWN(" + status + ")";
        }
    }

    @NonNull
    private String getHumanReadableTransferReason(@Nullable Integer transferReason) {
        if (transferReason == null) {
            // The route is not selected, or this Android version doesn't support transfer reason.
            return null;
        }
        switch (transferReason) {
            case 0:
                return "FALLBACK";
            case 1:
                return "SYSTEM_REQUEST";
            case 2:
                return "APP";
            default:
                return "UNKNOWN(" + transferReason + ")";
        }
    }

    @RequiresApi(34)
    private static final class Api34Impl {
        private Api34Impl() {}

        @DoNotInline
        static RoutingSessionInfo getRoutingSessionInfo(
                MediaRouter2.RoutingController routingController) {
            return routingController.getRoutingSessionInfo();
        }
    }
}
