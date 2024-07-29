/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_ID;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_ROUTE_CONTROL_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SET_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UPDATE_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_DATA_ERROR;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_FAILED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED;
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_ROUTE_CHANGED;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides non-system routes (and related RouteControllers) by using MediaRouter2.
 * This provider is added only when media transfer feature is enabled.
 */
@RequiresApi(Build.VERSION_CODES.R)
@SuppressWarnings({"unused", "ClassCanBeStatic"}) // TODO: Remove this.
class MediaRoute2Provider extends MediaRouteProvider {
    static final String TAG = "MR2Provider";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    final MediaRouter2 mMediaRouter2;
    final Callback mCallback;
    final Map<MediaRouter2.RoutingController, GroupRouteController> mControllerMap =
            new ArrayMap<>();
    private final MediaRouter2.RouteCallback mRouteCallback;
    private final MediaRouter2.TransferCallback mTransferCallback = new TransferCallback();
    private final MediaRouter2.ControllerCallback mControllerCallback = new ControllerCallback();
    private final Handler mHandler;
    private final Executor mHandlerExecutor;

    private List<MediaRoute2Info> mRoutes = new ArrayList<>();
    private Map<String, String> mRouteIdToOriginalRouteIdMap = new ArrayMap<>();
    @SuppressWarnings({"SyntheticAccessor"})
    MediaRoute2Provider(@NonNull Context context, @NonNull Callback callback) {
        super(context);
        mMediaRouter2 = MediaRouter2.getInstance(context);
        mCallback = callback;

        mHandler = new Handler(Looper.getMainLooper());
        mHandlerExecutor = mHandler::post;

        if (Build.VERSION.SDK_INT >= 34) {
            mRouteCallback = new RouteCallbackUpsideDownCake();
        } else {
            mRouteCallback = new RouteCallback();
        }
    }

    @Override
    public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
        if (MediaRouter.getGlobalCallbackCount() > 0) {
            request = updateDiscoveryRequest(request, MediaRouter.isTransferToLocalEnabled());

            mMediaRouter2.registerRouteCallback(mHandlerExecutor, mRouteCallback,
                    MediaRouter2Utils.toDiscoveryPreference(request));
            mMediaRouter2.registerTransferCallback(mHandlerExecutor, mTransferCallback);
            mMediaRouter2.registerControllerCallback(mHandlerExecutor, mControllerCallback);
        } else {
            mMediaRouter2.unregisterRouteCallback(mRouteCallback);
            mMediaRouter2.unregisterTransferCallback(mTransferCallback);
            mMediaRouter2.unregisterControllerCallback(mControllerCallback);
        }
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId) {
        String originalRouteId = mRouteIdToOriginalRouteIdMap.get(routeId);
        return new MemberRouteController(originalRouteId, null);
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId,
            @NonNull String routeGroupId) {
        String originalRouteId = mRouteIdToOriginalRouteIdMap.get(routeId);

        for (GroupRouteController groupRouteController : mControllerMap.values()) {
            if (TextUtils.equals(routeGroupId, groupRouteController.getGroupRouteId())) {
                return new MemberRouteController(originalRouteId, groupRouteController);
            }
        }
        Log.w(TAG, "Could not find the matching GroupRouteController. routeId=" + routeId
                + ", routeGroupId=" + routeGroupId);
        return new MemberRouteController(originalRouteId, null);
    }

    @Nullable
    @Override
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId, @Nullable Bundle controlHints) {
        // The parent implementation of onCreateDynamicGroupRouteController(String, Bundle) calls
        // onCreateDynamicGroupRouteController(String). We only need to override either one of
        // the onCreateDynamicGroupRouteController methods.
        for (Map.Entry<MediaRouter2.RoutingController, GroupRouteController> entry
                : mControllerMap.entrySet()) {
            GroupRouteController controller = entry.getValue();
            if (TextUtils.equals(initialMemberRouteId, controller.mInitialMemberRouteId)) {
                return controller;
            }
        }
        return null;
    }

    public void transferTo(@NonNull String routeId) {
        MediaRoute2Info route = getRouteById(routeId);
        if (route == null) {
            Log.w(TAG, "transferTo: Specified route not found. routeId=" + routeId);
            return;
        }
        mMediaRouter2.transferTo(route);
    }

    protected void refreshRoutes() {
        // Syetem routes should not be published by this provider.
        List<MediaRoute2Info> newRoutes = new ArrayList<>();
        Set<MediaRoute2Info> route2InfoSet = new ArraySet<>();
        for (MediaRoute2Info route : mMediaRouter2.getRoutes()) {
            // A route should be unique
            if (route == null || route2InfoSet.contains(route) || route.isSystemRoute()) {
                continue;
            }
            route2InfoSet.add(route);

            // Not using new ArrayList(route2InfoSet) here for preserving the order.
            newRoutes.add(route);
        }

        if (newRoutes.equals(mRoutes)) {
            return;
        }
        mRoutes = newRoutes;

        mRouteIdToOriginalRouteIdMap.clear();
        for (MediaRoute2Info route : mRoutes) {
            Bundle extras = route.getExtras();
            if (extras == null
                    || extras.getString(MediaRouter2Utils.KEY_ORIGINAL_ROUTE_ID) == null) {
                Log.w(TAG, "Cannot find the original route Id. route=" + route);
                continue;
            }
            mRouteIdToOriginalRouteIdMap.put(route.getId(),
                    extras.getString(MediaRouter2Utils.KEY_ORIGINAL_ROUTE_ID));
        }

        List<MediaRouteDescriptor> routeDescriptors = new ArrayList<>();
        for (MediaRoute2Info route : mRoutes) {
            MediaRouteDescriptor descriptor = MediaRouter2Utils.toMediaRouteDescriptor(route);
            if (descriptor != null) {
                routeDescriptors.add(descriptor);
            }
        }
        MediaRouteProviderDescriptor descriptor = new MediaRouteProviderDescriptor.Builder()
                .setSupportsDynamicGroupRoute(true)
                .addRoutes(routeDescriptors)
                .build();
        setDescriptor(descriptor);
    }

    @Nullable
    MediaRoute2Info getRouteById(@Nullable String routeId) {
        if (routeId == null) {
            return null;
        }
        for (MediaRoute2Info route : mRoutes) {
            if (TextUtils.equals(route.getId(), routeId)) {
                return route;
            }
        }
        return null;
    }

    @Nullable
    static Messenger getMessengerFromRoutingController(
            @Nullable MediaRouter2.RoutingController controller) {
        if (controller == null) {
            return null;
        }

        Bundle controlHints = controller.getControlHints();
        return controlHints == null ? null : controlHints.getParcelable(
                MediaRouter2Utils.KEY_MESSENGER);
    }

    @Nullable
    static String getSessionIdForRouteController(@Nullable RouteController controller) {
        if (!(controller instanceof GroupRouteController)) {
            return null;
        }
        MediaRouter2.RoutingController routingController =
                ((GroupRouteController) controller).mRoutingController;
        return (routingController == null) ? null : routingController.getId();
    }

    void setDynamicRouteDescriptors(MediaRouter2.RoutingController routingController) {
        GroupRouteController controller = mControllerMap.get(routingController);
        if (controller == null) {
            Log.w(TAG, "setDynamicRouteDescriptors: No matching routeController found. "
                    + "routingController=" + routingController);
            return;
        }

        List<MediaRoute2Info> selectedRoutes = routingController.getSelectedRoutes();
        if (selectedRoutes.isEmpty()) {
            Log.w(TAG, "setDynamicRouteDescriptors: No selected routes. This may happen "
                    + "when the selected routes become invalid."
                    + "routingController=" + routingController);
            return;
        }
        List<String> selectedRouteIds = MediaRouter2Utils.getRouteIds(selectedRoutes);
        MediaRouteDescriptor initialRouteDescriptor =
                MediaRouter2Utils.toMediaRouteDescriptor(selectedRoutes.get(0));

        MediaRouteDescriptor groupDescriptor = null;
        // TODO: Add RoutingController#getName() and use it in Android S+
        Bundle controlHints = routingController.getControlHints();
        String groupRouteName = getContext().getString(R.string.mr_dialog_default_group_name);
        try {
            if (controlHints != null) {
                String sessionName = controlHints.getString(MediaRouter2Utils.KEY_SESSION_NAME);
                if (!TextUtils.isEmpty(sessionName)) {
                    groupRouteName = sessionName;
                }
                Bundle groupRouteBundle = controlHints.getBundle(MediaRouter2Utils.KEY_GROUP_ROUTE);
                if (groupRouteBundle != null) {
                    groupDescriptor = MediaRouteDescriptor.fromBundle(groupRouteBundle);
                }
            }
        } catch (Exception ex) {
            Log.w(TAG, "Exception while unparceling control hints.", ex);
        }

        // Create or update the group route descriptor.
        MediaRouteDescriptor.Builder groupDescriptorBuilder;
        if (groupDescriptor == null) {
            groupDescriptorBuilder = new MediaRouteDescriptor.Builder(
                    routingController.getId(), groupRouteName)
                    .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED)
                    .setPlaybackType(MediaRouter.RouteInfo.PLAYBACK_TYPE_REMOTE);
        } else {
            groupDescriptorBuilder = new MediaRouteDescriptor.Builder(groupDescriptor);
        }
        groupDescriptor = groupDescriptorBuilder
                .setVolume(routingController.getVolume())
                .setVolumeMax(routingController.getVolumeMax())
                .setVolumeHandling(routingController.getVolumeHandling())
                .clearControlFilters()
                .addControlFilters(initialRouteDescriptor.getControlFilters())
                .clearGroupMemberIds()
                .addGroupMemberIds(selectedRouteIds)
                .build();

        // Create dynamic route descriptors
        List<String> selectableRouteIds =
                MediaRouter2Utils.getRouteIds(routingController.getSelectableRoutes());
        List<String> deselectableRouteIds =
                MediaRouter2Utils.getRouteIds(routingController.getDeselectableRoutes());

        MediaRouteProviderDescriptor providerDescriptor = getDescriptor();
        if (providerDescriptor == null) {
            Log.w(TAG, "setDynamicRouteDescriptors: providerDescriptor is not set.");
            return;
        }

        List<DynamicRouteDescriptor> dynamicRouteDescriptors = new ArrayList<>();
        List<MediaRouteDescriptor> routeDescriptors = providerDescriptor.getRoutes();
        if (!routeDescriptors.isEmpty()) {
            for (MediaRouteDescriptor descriptor: routeDescriptors) {
                String routeId = descriptor.getId();
                DynamicRouteDescriptor.Builder builder =
                        new DynamicRouteDescriptor.Builder(descriptor)
                                .setSelectionState(selectedRouteIds.contains(routeId)
                                        ? DynamicRouteDescriptor.SELECTED
                                        : DynamicRouteDescriptor.UNSELECTED)
                                .setIsGroupable(selectableRouteIds.contains(routeId))
                                .setIsUnselectable(deselectableRouteIds.contains(routeId))
                                .setIsTransferable(true);
                dynamicRouteDescriptors.add(builder.build());
            }
        }

        controller.setGroupRouteDescriptor(groupDescriptor);
        controller.notifyDynamicRoutesChanged(groupDescriptor, dynamicRouteDescriptors);
    }

    /**
     * Returns a new discovery request where {@link MediaControlIntent#CATEGORY_LIVE_AUDIO}
     * is added to (or removed from) the given request, based on whether the 'transfer to local'
     * feature is enabled.
     */
    private MediaRouteDiscoveryRequest updateDiscoveryRequest(
            @Nullable MediaRouteDiscoveryRequest request, boolean transferToLocalEnabled) {
        if (request == null) {
            request = new MediaRouteDiscoveryRequest(MediaRouteSelector.EMPTY, false);
        }

        List<String> controlCategories = request.getSelector().getControlCategories();

        if (transferToLocalEnabled) {
            // CATEGORY_LIVE_AUDIO should be added.
            if (!controlCategories.contains(MediaControlIntent.CATEGORY_LIVE_AUDIO)) {
                controlCategories.add(MediaControlIntent.CATEGORY_LIVE_AUDIO);
            }
        } else {
            // CATEGORY_LIVE_AUDIO should be removed.
            controlCategories.remove(MediaControlIntent.CATEGORY_LIVE_AUDIO);
        }

        MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategories(controlCategories)
                .build();
        return new MediaRouteDiscoveryRequest(selector, request.isActiveScan());
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    /* package */ void setRouteListingPreference(
            @Nullable RouteListingPreference routeListingPreference) {
        Api34Impl.setPlatformRouteListingPreference(
                mMediaRouter2,
                routeListingPreference != null
                        ? routeListingPreference.toPlatformRouteListingPreference()
                        : null);
    }

    abstract static class Callback {
        public abstract void onSelectRoute(@NonNull String routeDescriptorId,
                @MediaRouter.UnselectReason int reason);
        public abstract void onSelectFallbackRoute(@MediaRouter.UnselectReason int reason);

        public abstract void onReleaseController(@NonNull RouteController controller);
    }

    private class RouteCallback extends MediaRouter2.RouteCallback {
        RouteCallback() {}

        @Override
        public void onRoutesAdded(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }

        @Override
        public void onRoutesRemoved(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }

        @Override
        public void onRoutesChanged(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }
    }

    private class RouteCallbackUpsideDownCake extends MediaRouter2.RouteCallback {

        @Override
        public void onRoutesUpdated(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }
    }

    private class TransferCallback extends MediaRouter2.TransferCallback {

        @Override
        public void onTransfer(@NonNull MediaRouter2.RoutingController oldController,
                @NonNull MediaRouter2.RoutingController newController) {
            mControllerMap.remove(oldController);
            if (newController == mMediaRouter2.getSystemController()) {
                mCallback.onSelectFallbackRoute(UNSELECT_REASON_ROUTE_CHANGED);
            } else {
                List<MediaRoute2Info> selectedRoutes = newController.getSelectedRoutes();
                if (selectedRoutes.isEmpty()) {
                    Log.w(TAG, "Selected routes are empty. This shouldn't happen.");
                    return;
                }
                // TODO: Handle the case that the initial member is a group
                String routeId = selectedRoutes.get(0).getId();
                GroupRouteController controller = new GroupRouteController(newController, routeId);
                mControllerMap.put(newController, controller);
                mCallback.onSelectRoute(routeId, UNSELECT_REASON_ROUTE_CHANGED);
                setDynamicRouteDescriptors(newController);
            }
        }

        @Override
        public void onTransferFailure(@NonNull MediaRoute2Info requestedRoute) {
            Log.w(TAG, "Transfer failed. requestedRoute=" + requestedRoute);
        }

        @Override
        public void onStop(@NonNull MediaRouter2.RoutingController routingController) {
            RouteController routeController = mControllerMap.remove(routingController);
            if (routeController != null) {
                mCallback.onReleaseController(routeController);
            } else {
                Log.w(TAG, "onStop: No matching routeController found. routingController="
                        + routingController);
            }
        }
    }

    private class ControllerCallback extends MediaRouter2.ControllerCallback {
        ControllerCallback() {}

        @Override
        public void onControllerUpdated(@NonNull MediaRouter2.RoutingController routingController) {
            setDynamicRouteDescriptors(routingController);
        }
    }

    private class MemberRouteController extends RouteController {
        final String mOriginalRouteId;
        final GroupRouteController mGroupRouteController;

        MemberRouteController(@Nullable String originalRouteId,
                @Nullable GroupRouteController groupRouteController) {
            mOriginalRouteId = originalRouteId;
            mGroupRouteController = groupRouteController;
        }

        @Override
        public void onSetVolume(int volume) {
            // TODO: Unhide MediaRouter2#setRouteVolume() and use it in Android S+
            if (mOriginalRouteId == null || mGroupRouteController == null) {
                return;
            }
            mGroupRouteController.setMemberRouteVolume(mOriginalRouteId, volume);
        }

        @Override
        public void onUpdateVolume(int delta) {
            // TODO: Unhide MediaRouter2#setRouteVolume() and use it in Android S+
            if (mOriginalRouteId == null || mGroupRouteController == null) {
                return;
            }
            mGroupRouteController.updateMemberRouteVolume(mOriginalRouteId, delta);
        }
    }

    private class GroupRouteController extends DynamicGroupRouteController {
        // Time to clear mOptimisticVolume
        private static final long OPTIMISTIC_VOLUME_TIMEOUT_MS = 1_000;

        final String mInitialMemberRouteId;
        final MediaRouter2.RoutingController mRoutingController;
        @Nullable
        final Messenger mServiceMessenger;
        @Nullable
        final Messenger mReceiveMessenger;
        final SparseArray<ControlRequestCallback> mPendingCallbacks = new SparseArray<>();
        final Handler mControllerHandler;
        AtomicInteger mNextRequestId = new AtomicInteger(1);

        private final Runnable mClearOptimisticVolumeRunnable = () -> mOptimisticVolume = -1;
        // The possible current volume set by the user recently or -1 if not.
        int mOptimisticVolume = -1;
        @Nullable
        MediaRouteDescriptor mGroupRouteDescriptor;

        GroupRouteController(@NonNull MediaRouter2.RoutingController routingController,
                @NonNull String initialMemberRouteId) {
            mRoutingController = routingController;
            mInitialMemberRouteId = initialMemberRouteId;
            mServiceMessenger = getMessengerFromRoutingController(routingController);
            mReceiveMessenger = mServiceMessenger == null ? null :
                    new Messenger(new ReceiveHandler());
            mControllerHandler = new Handler(Looper.getMainLooper());
        }

        public String getGroupRouteId() {
            return (mGroupRouteDescriptor != null) ? mGroupRouteDescriptor.getId()
                    : mRoutingController.getId();
        }

        @Override
        public void onSetVolume(int volume) {
            if (mRoutingController == null) {
                return;
            }
            mRoutingController.setVolume(volume);
            mOptimisticVolume = volume;
            scheduleClearOptimisticVolume();
        }

        @Override
        public void onUpdateVolume(int delta) {
            if (mRoutingController == null) {
                return;
            }
            int volumeBefore = mOptimisticVolume < 0 ? mRoutingController.getVolume() :
                    mOptimisticVolume;
            mOptimisticVolume = Math.max(0, Math.min(volumeBefore + delta,
                    mRoutingController.getVolumeMax()));
            mRoutingController.setVolume(mOptimisticVolume);
            scheduleClearOptimisticVolume();
        }

        @Override
        public boolean onControlRequest(@NonNull Intent intent,
                @Nullable ControlRequestCallback callback) {
            if (mRoutingController == null || mRoutingController.isReleased()
                    || mServiceMessenger == null) {
                return false;
            }

            int requestId = mNextRequestId.getAndIncrement();
            Message msg = Message.obtain();
            msg.what = CLIENT_MSG_ROUTE_CONTROL_REQUEST;
            msg.arg1 = requestId;
            msg.obj = intent;
            msg.replyTo = mReceiveMessenger;
            try {
                mServiceMessenger.send(msg);
                // TODO: Clear callbacks for unresponsive requests
                if (callback != null) {
                    mPendingCallbacks.put(requestId, callback);
                }
                return true;
            } catch (DeadObjectException ex) {
                // The service died.
            } catch (RemoteException ex) {
                Log.e(TAG, "Could not send control request to service.", ex);
            }
            return false;
        }

        @Override
        public void onRelease() {
            mRoutingController.release();
        }

        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
            // Assuming only one ID exist in the list
            if (routeIds == null || routeIds.isEmpty()) {
                Log.w(TAG, "onUpdateMemberRoutes: Ignoring null or empty routeIds.");
                return;
            }

            String routeId = routeIds.get(0);
            MediaRoute2Info route = getRouteById(routeId);
            if (route == null) {
                Log.w(TAG, "onUpdateMemberRoutes: Specified route not found. routeId=" + routeId);
                return;
            }

            mMediaRouter2.transferTo(route);
        }

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {
            if (routeId == null || routeId.isEmpty()) {
                Log.w(TAG, "onAddMemberRoute: Ignoring null or empty routeId.");
                return;
            }

            MediaRoute2Info route = getRouteById(routeId);
            if (route == null) {
                Log.w(TAG, "onAddMemberRoute: Specified route not found. routeId=" + routeId);
                return;
            }

            mRoutingController.selectRoute(route);
        }

        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {
            if (routeId == null || routeId.isEmpty()) {
                Log.w(TAG, "onRemoveMemberRoute: Ignoring null or empty routeId.");
                return;
            }

            MediaRoute2Info route = getRouteById(routeId);
            if (route == null) {
                Log.w(TAG, "onRemoveMemberRoute: Specified route not found. routeId=" + routeId);
                return;
            }

            mRoutingController.deselectRoute(route);
        }

        private void scheduleClearOptimisticVolume() {
            mControllerHandler.removeCallbacks(mClearOptimisticVolumeRunnable);
            mControllerHandler.postDelayed(mClearOptimisticVolumeRunnable,
                    OPTIMISTIC_VOLUME_TIMEOUT_MS);
        }

        void setMemberRouteVolume(@NonNull String memberRouteOriginalId, int volume) {
            if (mRoutingController == null || mRoutingController.isReleased()
                    || mServiceMessenger == null) {
                return;
            }

            int requestId = mNextRequestId.getAndIncrement();
            Message msg = Message.obtain();
            msg.what = CLIENT_MSG_SET_ROUTE_VOLUME;
            msg.arg1 = requestId;

            Bundle data = new Bundle();
            data.putInt(CLIENT_DATA_VOLUME, volume);
            data.putString(CLIENT_DATA_ROUTE_ID, memberRouteOriginalId);
            msg.setData(data);

            msg.replyTo = mReceiveMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (DeadObjectException ex) {
                // The service died.
            } catch (RemoteException ex) {
                Log.e(TAG, "Could not send control request to service.", ex);
            }
        }

        void updateMemberRouteVolume(@NonNull String memberRouteOriginalId, int delta) {
            if (mRoutingController == null || mRoutingController.isReleased()
                    || mServiceMessenger == null) {
                return;
            }

            int requestId = mNextRequestId.getAndIncrement();
            Message msg = Message.obtain();
            msg.what = CLIENT_MSG_UPDATE_ROUTE_VOLUME;
            msg.arg1 = requestId;

            Bundle data = new Bundle();
            data.putInt(CLIENT_DATA_VOLUME, delta);
            data.putString(CLIENT_DATA_ROUTE_ID, memberRouteOriginalId);
            msg.setData(data);

            msg.replyTo = mReceiveMessenger;
            try {
                mServiceMessenger.send(msg);
            } catch (DeadObjectException ex) {
                // The service died.
            } catch (RemoteException ex) {
                Log.e(TAG, "Could not send control request to service.", ex);
            }
        }

        void setGroupRouteDescriptor(@NonNull MediaRouteDescriptor descriptor) {
            mGroupRouteDescriptor = descriptor;
        }

        class ReceiveHandler extends Handler {
            ReceiveHandler() {
                super(Looper.getMainLooper());
            }

            @Override
            public void handleMessage(Message msg) {
                final int what = msg.what;
                final int requestId = msg.arg1;
                final int arg = msg.arg2;
                final Object obj = msg.obj;
                final Bundle data = msg.peekData();

                ControlRequestCallback callback = mPendingCallbacks.get(requestId);
                if (callback == null) {
                    Log.w(TAG, "Pending callback not found for control request.");
                    return;
                }
                mPendingCallbacks.remove(requestId);

                switch (what) {
                    case SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED:
                        callback.onResult((Bundle) obj);
                        break;
                    case SERVICE_MSG_CONTROL_REQUEST_FAILED:
                        String error = data == null ? null : data.getString(SERVICE_DATA_ERROR);
                        callback.onError(error, (Bundle) obj);
                        break;
                }
            }
        }
    }

    @RequiresApi(34)
    private static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        static void setPlatformRouteListingPreference(
                @NonNull MediaRouter2 mediaRouter2,
                @Nullable android.media.RouteListingPreference routeListingPreference) {
            mediaRouter2.setRouteListingPreference(routeListingPreference);
        }
    }
}
