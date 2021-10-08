/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.android.supportv7.media;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.mediarouter.media.MediaRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderDescriptor;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;
import androidx.mediarouter.media.MediaRouter.RouteInfo;

import com.example.android.supportv7.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demonstrates how to create a custom media route provider.
 *
 * @see SampleDynamicGroupMrpService
 */
final class SampleDynamicGroupMrp extends SampleMediaRouteProvider {
    private static final String TAG = "SampleDynamicGroupMrp";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final String FIXED_VOLUME_ROUTE_ID = "fixed";
    private static final String VARIABLE_VOLUME_BASIC_ROUTE_ID = "variable_basic";
    private static final String STATIC_GROUP_ROUTE_ID = "static_group";
    private static final int MAX_GROUPABLE_TV_COUNT = 1;

    private SampleDynamicGroupRouteController mDynamicRouteController;

    SampleDynamicGroupMrp(Context context) {
        super(context);
    }

    @Override
    public RouteController onCreateRouteController(String routeId) {
        if (!checkDrawOverlay()) return null;

        MediaRouteDescriptor routeDescriptor = mRouteDescriptors.get(routeId);
        if (routeDescriptor == null) {
            Log.w(TAG, "onCreateRouteController: Unknown route ID " + routeId);
            return null;
        }

        return new SampleRouteController(routeId);
    }

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

    private String generateUniqueId() {
        String routeId;
        do {
            routeId = UUID.randomUUID().toString();
        } while (mRouteDescriptors.containsKey(routeId));
        return routeId;
    }

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

        String groupId = generateUniqueId();
        mDynamicRouteController = new SampleDynamicGroupRouteController(groupId, memberIds);

        return mDynamicRouteController;
    }

    @Override
    protected void initializeRoutes() {
        Resources r = getContext().getResources();
        Intent settingsIntent = new Intent(Intent.ACTION_MAIN);
        settingsIntent.setClass(getContext(), SampleMediaRouteSettingsActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        IntentSender is = PendingIntent.getActivity(getContext(), 99, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE).getIntentSender();

        mVolumes.put(VARIABLE_VOLUME_BASIC_ROUTE_ID + "1", VOLUME_DEFAULT);
        mVolumes.put(VARIABLE_VOLUME_BASIC_ROUTE_ID + "2", VOLUME_DEFAULT);
        mVolumes.put(VARIABLE_VOLUME_BASIC_ROUTE_ID + "3", VOLUME_DEFAULT);
        mVolumes.put(VARIABLE_VOLUME_BASIC_ROUTE_ID + "4", VOLUME_DEFAULT);
        mVolumes.put(FIXED_VOLUME_ROUTE_ID, VOLUME_DEFAULT);
        mVolumes.put(STATIC_GROUP_ROUTE_ID, VOLUME_DEFAULT);

        MediaRouteDescriptor routeDescriptor1 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_BASIC_ROUTE_ID + "1",
                r.getString(R.string.dg_tv_route_name1))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_TV)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_DEFAULT)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor2 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_BASIC_ROUTE_ID + "2",
                r.getString(R.string.dg_tv_route_name2))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_TV)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_DEFAULT)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor3 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_BASIC_ROUTE_ID + "3",
                r.getString(R.string.dg_speaker_route_name3))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_DEFAULT)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor4 = new MediaRouteDescriptor.Builder(
                VARIABLE_VOLUME_BASIC_ROUTE_ID + "4",
                r.getString(R.string.dg_speaker_route_name4))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setDeviceType(MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_VARIABLE)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_DEFAULT)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor5 = new MediaRouteDescriptor.Builder(
                FIXED_VOLUME_ROUTE_ID,
                r.getString(R.string.dg_not_unselectable_route_name5))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_MAX)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        MediaRouteDescriptor routeDescriptor6 = new MediaRouteDescriptor.Builder(
                STATIC_GROUP_ROUTE_ID,
                r.getString(R.string.dg_static_group_route_name6))
                .setDescription(r.getString(R.string.sample_route_description))
                .addControlFilters(CONTROL_FILTERS_BASIC)
                .addGroupMemberId(routeDescriptor1.getId())
                .addGroupMemberId(routeDescriptor3.getId())
                .setPlaybackStream(AudioManager.STREAM_MUSIC)
                .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE)
                .setVolumeHandling(MediaRouter.RouteInfo.PLAYBACK_VOLUME_FIXED)
                .setVolumeMax(VOLUME_MAX)
                .setVolume(VOLUME_MAX)
                .setCanDisconnect(true)
                .setSettingsActivity(is)
                .build();

        mRouteDescriptors.put(routeDescriptor1.getId(), routeDescriptor1);
        mRouteDescriptors.put(routeDescriptor2.getId(), routeDescriptor2);
        mRouteDescriptors.put(routeDescriptor3.getId(), routeDescriptor3);
        mRouteDescriptors.put(routeDescriptor4.getId(), routeDescriptor4);
        mRouteDescriptors.put(routeDescriptor5.getId(), routeDescriptor5);
        mRouteDescriptors.put(routeDescriptor6.getId(), routeDescriptor6);
    }

    @Override
    protected void publishRoutes() {
        MediaRouteProviderDescriptor providerDescriptor = new MediaRouteProviderDescriptor.Builder()
                .setSupportsDynamicGroupRoute(true)
                .addRoutes(mRouteDescriptors.values())
                .build();
        setDescriptor(providerDescriptor);
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

            // Initialize DynamicRouteDescriptor with all the route descriptors.
            List<MediaRouteDescriptor> routeDescriptors = getDescriptor().getRoutes();
            if (routeDescriptors != null && !routeDescriptors.isEmpty()) {
                for (MediaRouteDescriptor descriptor: routeDescriptors) {
                    String routeId = descriptor.getId();
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

            if (routeDescriptor.getDeviceType() == MediaRouter.RouteInfo.DEVICE_TYPE_TV) {
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
            if (routeDescriptor.getDeviceType() == MediaRouter.RouteInfo.DEVICE_TYPE_TV) {
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
        public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
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
                return (routeDescriptor.getDeviceType() == MediaRouter.RouteInfo.DEVICE_TYPE_TV)
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
