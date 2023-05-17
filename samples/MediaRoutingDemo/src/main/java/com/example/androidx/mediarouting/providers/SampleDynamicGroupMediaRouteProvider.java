/*
 * Copyright 2022 The Android Open Source Project
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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.media.MediaRouter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import com.example.androidx.mediarouting.RoutesManager;
import com.example.androidx.mediarouting.activities.SettingsActivity;
import com.example.androidx.mediarouting.data.RouteItem;
import com.example.androidx.mediarouting.services.SampleDynamicGroupMediaRouteProviderService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates how to create a custom media route provider.
 *
 * @see SampleDynamicGroupMediaRouteProviderService
 */
public final class SampleDynamicGroupMediaRouteProvider extends SampleMediaRouteProvider {
    private static final String TAG = "SampleDynamicGroupMrp";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private static final String STATIC_GROUP_ROUTE_ID = "static_group";
    private static final int MAX_GROUPABLE_TV_COUNT = 1;

    private SampleDynamicGroupRouteController mDynamicRouteController;

    public SampleDynamicGroupMediaRouteProvider(@NonNull Context context) {
        super(context);
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId) {
        if (!checkDrawOverlay()) return null;

        MediaRouteDescriptor routeDescriptor = mRouteDescriptors.get(routeId);
        if (routeDescriptor == null) {
            Log.w(TAG, "onCreateRouteController: Unknown route ID " + routeId);
            return null;
        }

        return new SampleRouteController(routeId);
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId,
            @NonNull String groupId) {
        // Handle a static group exceptionally
        if (groupId.equals(STATIC_GROUP_ROUTE_ID)) {
            return onCreateRouteController(routeId);
        }

        //TODO: Handle multiple dynamic route controllers
        // The below check filters invalid group IDs
        if (mDynamicRouteController == null
                || !groupId.equals(mDynamicRouteController.getGroupDescriptor().getId())) {
            return null;
        }
        RouteController controller = onCreateRouteController(routeId);
        if (controller != null) {
            mDynamicRouteController.addAndSyncMemberController(routeId,
                    (SampleRouteController) controller);
        }
        return controller;
    }

    @Nullable
    @Override
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId) {
        if (!checkDrawOverlay()) return null;

        List<String> memberIds = new ArrayList<>();
        MediaRouteDescriptor initialRoute = mRouteDescriptors.get(initialMemberRouteId);
        if (initialRoute == null || !initialRoute.isValid()) {
            Log.w(TAG, "initial route doesn't exist or isn't valid : " + initialMemberRouteId);
            return null;
        }

        // Check if the selected route is a group or not.
        if (initialRoute.getGroupMemberIds().isEmpty()) {
            memberIds.add(initialMemberRouteId);
        } else {
            memberIds.addAll(initialRoute.getGroupMemberIds());
        }

        String groupId = UUID.randomUUID().toString();
        mDynamicRouteController = new SampleDynamicGroupRouteController(groupId, memberIds);

        return mDynamicRouteController;
    }

    /** Reload the provider routes. */
    public void reloadRoutes() {
        initializeRoutes();
        publishRoutes();
    }

    /** Reload the isDynamicRouteEnabled flag. */
    public void reloadDynamicRoutesEnabled() {
        boolean isDynamicRoutesEnabled = RoutesManager.getInstance(getContext())
                .isDynamicRoutingEnabled();
        MediaRouteProviderDescriptor providerDescriptor =
                new MediaRouteProviderDescriptor.Builder(getDescriptor())
                        .setSupportsDynamicGroupRoute(isDynamicRoutesEnabled)
                        .build();
        setDescriptor(providerDescriptor);
    }

    @Override
    protected void initializeRoutes() {
        mVolumes = new ArrayMap<>();
        mRouteDescriptors = new HashMap<>();
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN);
        settingsIntent
                .setClass(getContext(), SettingsActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSender is = PendingIntent.getActivity(getContext(), 99, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE).getIntentSender();

        List<RouteItem> routeItems = RoutesManager.getInstance(getContext()).getRouteItems();
        for (RouteItem routeItem : routeItems) {
            MediaRouteDescriptor routeDescriptor = buildRouteDescriptor(routeItem, is);
            mVolumes.put(routeItem.getId(), routeItem.getVolume());
            mRouteDescriptors.put(routeItem.getId(), routeDescriptor);
        }
    }

    @Override
    protected void publishRoutes() {
        MediaRouteProviderDescriptor providerDescriptor =
                new MediaRouteProviderDescriptor.Builder()
                        .setSupportsDynamicGroupRoute(true)
                        .addRoutes(mRouteDescriptors.values())
                        .build();
        setDescriptor(providerDescriptor);
    }

    private MediaRouteDescriptor buildRouteDescriptor(RouteItem routeItem, IntentSender is) {
        return new MediaRouteDescriptor.Builder(routeItem.getId(), routeItem.getName())
                .setDescription(routeItem.getDescription())
                .addControlFilters(getControlFilters(routeItem.getControlFilter()))
                .setPlaybackStream(routeItem.getPlaybackStream().mIntConstant)
                .setPlaybackType(routeItem.getPlaybackType().mIntConstant)
                .setVolumeHandling(routeItem.getVolumeHandling().mIntConstant)
                .setDeviceType(routeItem.getDeviceType().mIntConstant)
                .setVolumeMax(routeItem.getVolumeMax())
                .setVolume(routeItem.getVolume())
                .setCanDisconnect(routeItem.isCanDisconnect())
                .setSettingsActivity(is)
                .build();
    }

    private static List<IntentFilter> getControlFilters(RouteItem.ControlFilter controlFilter) {
        switch (controlFilter) {
            case BASIC:
                return CONTROL_FILTERS_BASIC;
            case QUEUE:
                return CONTROL_FILTERS_QUEUING;
            case SESSION:
                return CONTROL_FILTERS_SESSION;
        }
        return new ArrayList<>();
    }

    final class SampleDynamicGroupRouteController
            extends MediaRouteProvider.DynamicGroupRouteController {
        private final String mRouteId;
        private final Map<String, WeakReference<SampleRouteController>> mControllerMap =
                new ArrayMap<>();

        private List<String> mMemberRouteIds = new ArrayList<>();
        private Map<String, DynamicRouteDescriptor> mDynamicRouteDescriptors = new ArrayMap<>();
        private int mTvSelectedCount;
        private MediaRouteDescriptor mGroupDescriptor;

        SampleDynamicGroupRouteController(String groupRouteId, List<String> memberIds) {
            mRouteId = groupRouteId;
            MediaRouteDescriptor initialMember = mRouteDescriptors.get(memberIds.get(0));
            MediaRouteDescriptor.Builder groupRouteBuilder =
                    new MediaRouteDescriptor.Builder(groupRouteId, initialMember.getName())
                            .setVolume(VOLUME_DEFAULT)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolumeHandling(RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                            .setConnectionState(RouteInfo.CONNECTION_STATE_CONNECTED)
                            .addGroupMemberIds(memberIds);
            groupRouteBuilder.addControlFilters(initialMember.getControlFilters());
            mGroupDescriptor = groupRouteBuilder.build();
            mTvSelectedCount = countTvFromRoute(mGroupDescriptor);

            RoutesManager routesManager = RoutesManager.getInstance(getContext());

            // Initialize DynamicRouteDescriptor with all the non-sender-driven descriptors.
            List<MediaRouteDescriptor> routeDescriptors = getDescriptor().getRoutes();
            if (routeDescriptors != null && !routeDescriptors.isEmpty()) {
                for (MediaRouteDescriptor descriptor: routeDescriptors) {
                    String routeId = descriptor.getId();
                    RouteItem item = routesManager.getRouteWithId(routeId);
                    if (item != null && item.isSenderDriven()) {
                        continue;
                    }
                    boolean selected = memberIds.contains(routeId);
                    DynamicRouteDescriptor.Builder builder =
                            new DynamicRouteDescriptor.Builder(descriptor)
                                    .setIsGroupable(true)
                                    .setIsTransferable(true)
                                    .setIsUnselectable(true)
                                    .setSelectionState(selected ? DynamicRouteDescriptor.SELECTED
                                            : DynamicRouteDescriptor.UNSELECTED);
                    mDynamicRouteDescriptors.put(routeId, builder.build());
                }
            }
            if (memberIds != null) {
                mMemberRouteIds.addAll(memberIds);
            }

            Log.d(TAG, mRouteId + ": Controller created.");
        }

        //////////////////////////////////////////////
        // Overrides DynamicGroupRouteController
        //////////////////////////////////////////////

        @Override
        public String getGroupableSelectionTitle() {
            return "Add a device";
        }

        @Override
        public String getTransferableSectionTitle() {
            return "Play on tv";
        }

        @Override
        public void onUpdateMemberRoutes(List<String> routeIds) {
            List<String> routeIdsToRemove = new ArrayList<>(mMemberRouteIds);
            for (String routeId : routeIdsToRemove) {
                removeMember(routeId);
            }
            for (String routeId : routeIds) {
                updateDynamicRouteDescriptors(/*shouldNotify=*/false);
                addMember(routeId);
            }
            updateGroupRouteDescriptor();
            updateDynamicRouteDescriptors(/*shouldNotify=*/true);
        }

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {
            if (!addMember(routeId)) {
                Log.w(TAG, "onAddMemberRoute: Failed to add a member route:" + " " + routeId);
                return;
            }

            // Add each member route to the dynamic group
            MediaRouteDescriptor routeDescriptor = mRouteDescriptors.get(routeId);
            for (String memberRouteId : routeDescriptor.getGroupMemberIds()) {
                addMember(memberRouteId);
            }
            updateGroupRouteDescriptor();
            updateDynamicRouteDescriptors(/*shouldNotify=*/true);
        }

        private void updateGroupRouteDescriptor() {
            MediaRouteDescriptor.Builder groupDescriptorBuilder =
                    new MediaRouteDescriptor.Builder(mGroupDescriptor)
                            .clearGroupMemberIds();
            for (String memberRouteId : mMemberRouteIds) {
                groupDescriptorBuilder.addGroupMemberId(memberRouteId);
            }

            mGroupDescriptor = groupDescriptorBuilder.build();
        }

        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {
            if (!removeMember(routeId)) {
                Log.w(TAG, "onRemoveMemberRoute: Failed to remove a member route : " + routeId);
                return;
            }
            updateGroupRouteDescriptor();
            updateDynamicRouteDescriptors(/*shouldNotify=*/true);
        }

        private boolean addMember(String routeId) {
            DynamicRouteDescriptor dynamicDescriptor = mDynamicRouteDescriptors.get(routeId);
            if (dynamicDescriptor == null || !dynamicDescriptor.isGroupable()
                    || mMemberRouteIds.contains(routeId)) {
                return false;
            }
            mMemberRouteIds.add(routeId);

            MediaRouteDescriptor routeDescriptor =
                    mRouteDescriptors.get(dynamicDescriptor.getRouteDescriptor().getId());
            DynamicRouteDescriptor.Builder builder =
                    new DynamicRouteDescriptor.Builder(dynamicDescriptor);
            builder.setSelectionState(DynamicRouteDescriptor.SELECTED);

            //TODO: Depending on getVolumeHandling() doesn't look good to identify the route. It
            // could be a signal that the current structure should be refactored.

            // Use a unique feature of the not unselectable route.
            if (routeDescriptor.getVolumeHandling()
                    == MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED) {
                builder.setIsUnselectable(false);
            } else {
                builder.setIsUnselectable(true);
            }

            mDynamicRouteDescriptors.put(routeId, builder.build());

            if (routeDescriptor.getDeviceType() == RouteInfo.DEVICE_TYPE_TV) {
                mTvSelectedCount++;
            }
            return true;
        }

        private boolean removeMember(String routeId) {
            DynamicRouteDescriptor dynamicDescriptor = mDynamicRouteDescriptors.get(routeId);
            if (dynamicDescriptor == null || !dynamicDescriptor.isUnselectable()
                    || !mMemberRouteIds.remove(routeId)) {
                return false;
            }

            DynamicRouteDescriptor.Builder builder =
                    new DynamicRouteDescriptor.Builder(dynamicDescriptor);
            builder.setSelectionState(DynamicRouteDescriptor.UNSELECTED);
            mDynamicRouteDescriptors.put(routeId, builder.build());

            MediaRouteDescriptor routeDescriptor =
                    mRouteDescriptors.get(dynamicDescriptor.getRouteDescriptor().getId());
            if (routeDescriptor.getDeviceType() == RouteInfo.DEVICE_TYPE_TV) {
                mTvSelectedCount--;
            }
            return true;
        }

        //////////////////////////////////////////////
        // Overrides RouteController
        //////////////////////////////////////////////

        @Override
        public void onRelease() {
            Log.d(TAG, mRouteId + ": Controller released");
            for (RouteController controller : getValidMemberControllers()) {
                controller.onRelease();
            }
        }

        @Override
        public void onSelect() {
            Log.d(TAG, mRouteId + ": Selected");
            updateDynamicRouteDescriptors(/*shouldNotify=*/true);

            for (RouteController controller : getValidMemberControllers()) {
                controller.onSelect();
            }
        }

        @Override
        public void onUnselect(int reason) {
            Log.d(TAG, mRouteId + ": Unselected. reason=" + reason);

            for (RouteController controller : getValidMemberControllers()) {
                controller.onUnselect(reason);
            }

            if (mGroupDescriptor != null) {
                mGroupDescriptor = new MediaRouteDescriptor.Builder(mGroupDescriptor)
                        .setConnectionState(RouteInfo.CONNECTION_STATE_DISCONNECTED)
                        .build();
                publishRoutes();
            }
        }

        @Override
        public void onSetVolume(int volume) {
            Log.d(TAG, mRouteId + ": Set volume to " + volume);
            mGroupDescriptor = new MediaRouteDescriptor.Builder(mGroupDescriptor)
                    .setVolume(volume)
                    .build();
            for (RouteController controller : getValidMemberControllers()) {
                controller.onSetVolume(volume);
            }
            notifyDynamicRoutesChanged(mGroupDescriptor, mDynamicRouteDescriptors.values());
        }

        @Override
        public void onUpdateVolume(int delta) {
            Log.d(TAG, mRouteId + ": Update volume by " + delta);
            int updatedVolume = mGroupDescriptor.getVolume() + delta;
            updatedVolume = Math.max(Math.min(updatedVolume, mGroupDescriptor.getVolumeMax()), 0);
            mGroupDescriptor = new MediaRouteDescriptor.Builder(mGroupDescriptor)
                    .setVolume(updatedVolume)
                    .build();
            for (RouteController controller : getValidMemberControllers()) {
                controller.onSetVolume(updatedVolume);
            }
            notifyDynamicRoutesChanged(mGroupDescriptor, mDynamicRouteDescriptors.values());
        }

        @Override
        public boolean onControlRequest(@NonNull Intent intent, ControlRequestCallback callback) {
            Log.d(TAG, mRouteId + ": Received control request " + intent);
            for (RouteController controller : getValidMemberControllers()) {
                controller.onControlRequest(intent, callback);
                // Let the first member handles the callback
                callback = null;
            }
            return true;
        }

        void addAndSyncMemberController(String routeId, SampleRouteController newController) {
            SampleRouteController previousRoutingController = null;
            for (WeakReference<SampleRouteController> controllerRef : mControllerMap.values()) {
                SampleRouteController controller = controllerRef.get();
                if (controller != null && !controller.isReleased()) {
                    previousRoutingController = controller;
                    break;
                }
            }
            if (previousRoutingController != null) {
                newController.getSessionManager().syncWithManager(
                        previousRoutingController.getSessionManager());
            }
            mControllerMap.put(routeId, new WeakReference<>(newController));
        }

        List<SampleRouteController> getValidMemberControllers() {
            List<SampleRouteController> controllers = new ArrayList<>();
            for (Iterator<WeakReference<SampleRouteController>> it =
                    mControllerMap.values().iterator(); it.hasNext(); ) {
                SampleRouteController controller = it.next().get();
                if (controller == null || controller.isReleased()) {
                    it.remove();
                } else {
                    controllers.add(controller);
                }
            }
            return controllers;
        }

        MediaRouteDescriptor getGroupDescriptor() {
            return mGroupDescriptor;
        }

        // Updates each route's dynamic route descriptor when a route is added to / removed from
        // a dynamic group. More specifically, groupable properties should be updated, in this demo.
        // Rule 1. TV devices are exclusively selected. So, if a TV device is already selected
        //         (added) as a member of dynamic group, other TV devices cannot be groupable.
        // Rout 2. A static group route, whose members are all already selected, should be
        //         ungroupable.
        private void updateDynamicRouteDescriptors(boolean shouldNotify) {
            for (DynamicRouteDescriptor dynamicDescriptor : mDynamicRouteDescriptors.values()) {
                String routeId = dynamicDescriptor.getRouteDescriptor().getId();
                MediaRouteDescriptor routeDescriptor = mRouteDescriptors.get(routeId);
                if (mMemberRouteIds.contains(routeId)) {
                    // Skip selected routes.
                    continue;
                }
                boolean isGroupable = true;
                boolean isTransferable = true;

                if (mRouteId.equals(routeId)) {
                    isGroupable = false;
                    isTransferable = false;
                }
                // This route is a group and its member routes are already all selected.
                if (!routeDescriptor.getGroupMemberIds().isEmpty()
                        && mMemberRouteIds.containsAll(routeDescriptor.getGroupMemberIds())) {
                    isGroupable = false;
                }
                if ((countTvFromRoute(routeDescriptor) + mTvSelectedCount)
                        > MAX_GROUPABLE_TV_COUNT) {
                    isGroupable = false;
                }
                if (mMemberRouteIds.contains(routeId)) {
                    isGroupable = false;
                    isTransferable = false;
                }

                if (isGroupable != dynamicDescriptor.isGroupable()
                        || isTransferable != dynamicDescriptor.isTransferable()) {
                    DynamicRouteDescriptor.Builder builder =
                            new DynamicRouteDescriptor.Builder(dynamicDescriptor)
                                    .setIsGroupable(isGroupable)
                                    .setIsTransferable(isTransferable);

                    mDynamicRouteDescriptors.put(routeId, builder.build());
                }
            }
            if (shouldNotify) {
                notifyDynamicRoutesChanged(mGroupDescriptor, mDynamicRouteDescriptors.values());
            }
        }

        private int countTvFromRoute(MediaRouteDescriptor routeDescriptor) {
            if (routeDescriptor.getGroupMemberIds().isEmpty()) {
                return (routeDescriptor.getDeviceType() == RouteInfo.DEVICE_TYPE_TV)
                        ? 1 : 0;
            }
            int count = 0;
            for (String routeId : routeDescriptor.getGroupMemberIds()) {
                MediaRouteDescriptor descriptor = mRouteDescriptors.get(routeId);
                count += countTvFromRoute(descriptor);
            }
            return count;
        }
    }
}
