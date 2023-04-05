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
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRoute2Info;
import android.media.MediaRoute2ProviderService;
import android.media.RouteDiscoveryPreference;
import android.media.RoutingSessionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider.RouteController;
import androidx.mediarouter.media.MediaRouteProviderService.MediaRouteProviderServiceImplApi30;
import androidx.mediarouter.media.MediaRouteProviderService.MediaRouteProviderServiceImplApi30.ClientRecord;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRoute2ProviderServiceAdapter extends MediaRoute2ProviderService {
    private static final String TAG = "MR2ProviderService";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();

    final MediaRouteProviderServiceImplApi30 mServiceImpl;
    // Maps session ID to SessionRecord.
    @GuardedBy("mLock")
    final Map<String, SessionRecord> mSessionRecords = new ArrayMap<>();
    // Maps controller ID to Session ID.
    final SparseArray<String> mSessionIdMap = new SparseArray<>();

    private volatile MediaRouteProviderDescriptor mProviderDescriptor;

    @SuppressLint("InlinedApi")
    public static final String SERVICE_INTERFACE = MediaRoute2ProviderService.SERVICE_INTERFACE;

    MediaRoute2ProviderServiceAdapter(MediaRouteProviderServiceImplApi30 serviceImpl) {
        mServiceImpl = serviceImpl;
    }

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
    }

    @Override
    public void onSetRouteVolume(long requestId, @NonNull String routeId, int volume) {
        RouteController controller = findControllerByRouteId(routeId);

        if (controller == null) {
            Log.w(TAG, "onSetRouteVolume: Couldn't find a controller for routeId=" + routeId);
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onSetVolume(volume);
    }

    @Override
    public void onSetSessionVolume(long requestId, @NonNull String sessionId, int volume) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onSetSessionVolume: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }

        DynamicGroupRouteController controller = findControllerBySessionId(sessionId);
        if (controller == null) {
            Log.w(TAG, "onSetSessionVolume: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onSetVolume(volume);
    }

    @Override
    public void onCreateSession(long requestId, @NonNull String packageName,
            @NonNull String routeId, @Nullable Bundle sessionHints) {
        MediaRouteProvider provider = getMediaRouteProvider();
        MediaRouteDescriptor selectedRoute = getRouteDescriptor(routeId, "onCreateSession");
        if (selectedRoute == null) {
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        int sessionFlags = SessionRecord.SESSION_FLAG_MR2;
        DynamicGroupRouteController controller;
        if (mProviderDescriptor.supportsDynamicGroupRoute()) {
            controller = provider.onCreateDynamicGroupRouteController(routeId);
            sessionFlags |= SessionRecord.SESSION_FLAG_GROUP | SessionRecord.SESSION_FLAG_DYNAMIC;
            if (controller == null) {
                Log.w(TAG, "onCreateSession: Couldn't create a dynamic controller");
                notifyRequestFailed(requestId, REASON_REJECTED);
                return;
            }
        } else {
            RouteController routeController = provider.onCreateRouteController(routeId);
            if (routeController == null) {
                Log.w(TAG, "onCreateSession: Couldn't create a controller");
                notifyRequestFailed(requestId, REASON_REJECTED);
                return;
            }
            if (!selectedRoute.getGroupMemberIds().isEmpty()) {
                sessionFlags |= SessionRecord.SESSION_FLAG_GROUP;
            }
            controller = new DynamicGroupRouteControllerProxy(routeId, routeController);
        }
        controller.onSelect();

        SessionRecord sessionRecord = new SessionRecord(controller, requestId, sessionFlags);
        String sessionId = assignSessionId(sessionRecord);

        RoutingSessionInfo.Builder builder =
                new RoutingSessionInfo.Builder(sessionId, packageName)
                        .setName(selectedRoute.getName())
                        .setVolumeHandling(selectedRoute.getVolumeHandling())
                        .setVolume(selectedRoute.getVolume())
                        .setVolumeMax(selectedRoute.getVolumeMax());

        if (selectedRoute.getGroupMemberIds().isEmpty()) {
            builder.addSelectedRoute(routeId);
        } else {
            for (String memberId : selectedRoute.getGroupMemberIds()) {
                builder.addSelectedRoute(memberId);
            }
        }

        RoutingSessionInfo sessionInfo = builder.build();
        sessionRecord.setSessionInfo(sessionInfo);

        if ((sessionFlags & SessionRecord.SESSION_FLAG_DYNAMIC) == 0) {
            if ((sessionFlags & SessionRecord.SESSION_FLAG_GROUP) != 0) {
                // Create member route controllers if it's a static group. Member route controllers
                // for a dynamic group will be created after the group route is created.
                // (DynamicGroupRouteController#notifyDynamicRoutesChanged is called).
                sessionRecord.updateMemberRouteControllers(
                        routeId, /* oldSession= */ null, sessionInfo);
            } else {
                // The session has a non-group static route controller, whose proxy route
                // controller has already been created. We just need to map the route id to said
                // controller, for the controller to be found by its corresponding route id via
                // findControllerByRouteId (needed, for example, for route volume adjustment).
                sessionRecord.setStaticMemberRouteId(routeId);
            }
        }

        mServiceImpl.setDynamicRoutesChangedListener(controller);
    }

    @Override
    public void onReleaseSession(long requestId, @NonNull String sessionId) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            return;
        }

        SessionRecord sessionRecord;
        synchronized (mLock) {
            sessionRecord = mSessionRecords.remove(sessionId);
        }
        if (sessionRecord == null) {
            Log.w(TAG, "onReleaseSession: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }
        sessionRecord.release(/*shouldUnselect=*/true);
    }

    @Override
    public void onSelectRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onSelectRoute: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }
        if (getRouteDescriptor(routeId, "onSelectRoute") == null) {
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        DynamicGroupRouteController controller = findControllerBySessionId(sessionId);
        if (controller == null) {
            Log.w(TAG, "onSelectRoute: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onAddMemberRoute(routeId);
    }

    @Override
    public void onDeselectRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onDeselectRoute: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }
        if (getRouteDescriptor(routeId, "onDeselectRoute") == null) {
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        DynamicGroupRouteController controller = findControllerBySessionId(sessionId);
        if (controller == null) {
            Log.w(TAG, "onDeselectRoute: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onRemoveMemberRoute(routeId);
    }

    @Override
    public void onTransferToRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onTransferToRoute: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }
        if (getRouteDescriptor(routeId, "onTransferToRoute") == null) {
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        DynamicGroupRouteController controller = findControllerBySessionId(sessionId);
        if (controller == null) {
            Log.w(TAG, "onTransferToRoute: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onUpdateMemberRoutes(Collections.singletonList(routeId));
    }

    @Override
    public void onDiscoveryPreferenceChanged(@NonNull RouteDiscoveryPreference preference) {
        mServiceImpl.setBaseDiscoveryRequest(
                MediaRouter2Utils.toMediaRouteDiscoveryRequest(preference));
    }

    public void setProviderDescriptor(@Nullable MediaRouteProviderDescriptor descriptor) {
        mProviderDescriptor = descriptor;
        List<MediaRouteDescriptor> routeDescriptors =
                (descriptor == null) ? Collections.emptyList() : descriptor.getRoutes();

        Map<String, MediaRouteDescriptor> descriptorMap = new ArrayMap<>();
        for (MediaRouteDescriptor desc : routeDescriptors) {
            // If duplicate ids exist, the last one survives.
            // Aligned with MediaRouter implementation.
            if (desc == null) {
                continue;
            }
            descriptorMap.put(desc.getId(), desc);
        }

        updateStaticSessions(descriptorMap);

        List<MediaRoute2Info> routes = new ArrayList<>();
        for (MediaRouteDescriptor desc : descriptorMap.values()) {
            MediaRoute2Info fwkMediaRouteInfo = MediaRouter2Utils.toFwkMediaRoute2Info(desc);
            if (fwkMediaRouteInfo != null) {
                routes.add(fwkMediaRouteInfo);
            }
        }
        notifyRoutes(routes);
    }

    private DynamicGroupRouteController findControllerBySessionId(String sessionId) {
        synchronized (mLock) {
            SessionRecord sessionRecord = mSessionRecords.get(sessionId);
            return (sessionRecord == null) ? null : sessionRecord.getGroupController();
        }
    }

    private MediaRouteDescriptor getRouteDescriptor(String routeId, String description) {
        MediaRouteProvider provider = getMediaRouteProvider();
        if (provider == null || mProviderDescriptor == null) {
            Log.w(TAG, description + ": no provider info");
            return null;
        }

        List<MediaRouteDescriptor> routes = mProviderDescriptor.getRoutes();
        for (MediaRouteDescriptor route : routes) {
            if (TextUtils.equals(route.getId(), routeId)) {
                return route;
            }
        }
        Log.w(TAG, description + ": Couldn't find a route : " + routeId);
        return null;
    }

    private SessionRecord findSessionRecordByController(DynamicGroupRouteController controller) {
        synchronized (mLock) {
            for (Map.Entry<String, SessionRecord> entry : mSessionRecords.entrySet()) {
                SessionRecord sessionRecord = entry.getValue();
                if (sessionRecord.getGroupController() == controller) {
                    return sessionRecord;
                }
            }
        }
        return null;
    }

    public void setDynamicRouteDescriptor(DynamicGroupRouteController controller,
            MediaRouteDescriptor groupRoute,
            Collection<DynamicRouteDescriptor> descriptors) {
        SessionRecord sessionRecord = findSessionRecordByController(controller);
        if (sessionRecord == null) {
            Log.w(TAG, "setDynamicRouteDescriptor: Ignoring unknown controller");
            return;
        }

        sessionRecord.updateSessionInfo(groupRoute, descriptors);
    }

    void updateStaticSessions(Map<String, MediaRouteDescriptor> routeDescriptors) {
        List<SessionRecord> staticSessions = new ArrayList<>();
        synchronized (mLock) {
            for (SessionRecord sessionRecord : mSessionRecords.values()) {
                if ((sessionRecord.getFlags() & SessionRecord.SESSION_FLAG_DYNAMIC) == 0) {
                    staticSessions.add(sessionRecord);
                }
            }
        }
        for (SessionRecord sessionRecord : staticSessions) {
            DynamicGroupRouteControllerProxy controller =
                    (DynamicGroupRouteControllerProxy) sessionRecord.getGroupController();
            if (routeDescriptors.containsKey(controller.getRouteId())) {
                sessionRecord.updateSessionInfo(routeDescriptors.get(controller.getRouteId()),
                        /*dynamicRouteDescriptors=*/null);
            }
        }
    }

    //TODO: Remove this
    void onControlRequest(Messenger messenger, int requestId, String sessionId,
            Intent intent) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onCustomCommand: Couldn't find a session");
            return;
        }

        DynamicGroupRouteController controller = findControllerBySessionId(sessionId);
        if (controller == null) {
            Log.w(TAG, "onControlRequest: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        MediaRouter.ControlRequestCallback callback = new MediaRouter.ControlRequestCallback() {
            @Override
            public void onResult(Bundle data) {
                if (DEBUG) {
                    Log.d(TAG, "Route control request succeeded"
                            + ", sessionId=" + sessionId
                            + ", intent=" + intent
                            + ", data=" + data);
                }

                sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED,
                        requestId, 0, data, null);
            }

            @Override
            public void onError(String error, Bundle data) {
                if (DEBUG) {
                    Log.d(TAG, "Route control request failed"
                            + ", sessionId=" + sessionId
                            + ", intent=" + intent
                            + ", error=" + error + ", data=" + data);
                }
                if (error != null) {
                    Bundle bundle = new Bundle();
                    bundle.putString(SERVICE_DATA_ERROR, error);
                    sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
                            requestId, 0, data, bundle);
                } else {
                    sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
                            requestId, 0, data, null);
                }
            }

            void sendReply(Messenger messenger, int what,
                    int requestId, int arg, Object obj, Bundle data) {
                Message msg = Message.obtain();
                msg.what = what;
                msg.arg1 = requestId;
                msg.arg2 = arg;
                msg.obj = obj;
                msg.setData(data);
                try {
                    messenger.send(msg);
                } catch (DeadObjectException ex) {
                    // The client died.
                } catch (RemoteException ex) {
                    Log.e(TAG, "Could not send message to the client.", ex);
                }
            }
        };

        controller.onControlRequest(intent, callback);
    }

    void setRouteVolume(@NonNull String routeId, int volume) {
        RouteController controller = findControllerByRouteId(routeId);
        if (controller == null) {
            Log.w(TAG, "setRouteVolume: Couldn't find a controller for routeId=" + routeId);
            return;
        }
        controller.onSetVolume(volume);
    }

    void updateRouteVolume(@NonNull String routeId, int delta) {
        RouteController controller = findControllerByRouteId(routeId);
        if (controller == null) {
            Log.w(TAG, "updateRouteVolume: Couldn't find a controller for routeId=" + routeId);
            return;
        }
        controller.onUpdateVolume(delta);
    }

    void notifyRouteControllerAdded(ClientRecord clientRecord,
            RouteController routeController, int controllerId, String packageName, String routeId) {
        MediaRouteDescriptor descriptor = getRouteDescriptor(routeId, "notifyRouteControllerAdded");
        if (descriptor == null) {
            return;
        }

        int sessionFlags = 0;
        DynamicGroupRouteController controller;
        if (routeController instanceof DynamicGroupRouteController) {
            sessionFlags |= SessionRecord.SESSION_FLAG_DYNAMIC | SessionRecord.SESSION_FLAG_GROUP;
            controller = (DynamicGroupRouteController) routeController;
        } else {
            if (!descriptor.getGroupMemberIds().isEmpty()) {
                sessionFlags |= SessionRecord.SESSION_FLAG_GROUP;
            }
            controller = new DynamicGroupRouteControllerProxy(routeId, routeController);
        }

        SessionRecord sessionRecord = new SessionRecord(controller, REQUEST_ID_NONE,
                sessionFlags, clientRecord);
        //TODO: Reconsider the logic if dynamic grouping is enabled for clients < CLIENT_VERSION_4
        sessionRecord.mRouteId = routeId;

        String sessionId = assignSessionId(sessionRecord);
        mSessionIdMap.put(controllerId, sessionId);

        RoutingSessionInfo.Builder builder =
                new RoutingSessionInfo.Builder(sessionId, packageName)
                        .setName(descriptor.getName())
                        .setVolumeHandling(descriptor.getVolumeHandling())
                        .setVolume(descriptor.getVolume())
                        .setVolumeMax(descriptor.getVolumeMax());

        if (descriptor.getGroupMemberIds().isEmpty()) {
            builder.addSelectedRoute(routeId);
        } else {
            for (String memberId : descriptor.getGroupMemberIds()) {
                builder.addSelectedRoute(memberId);
            }
        }
        sessionRecord.setSessionInfo(builder.build());
    }

    void notifyRouteControllerRemoved(int controllerId) {
        String sessionId = mSessionIdMap.get(controllerId);
        if (sessionId == null) {
            return;
        }
        mSessionIdMap.remove(controllerId);

        SessionRecord sessionRecord;
        synchronized (mLock) {
            sessionRecord = mSessionRecords.remove(sessionId);
        }
        if (sessionRecord != null) {
            sessionRecord.release(/*shouldUnselect=*/false);
        }
    }

    private RouteController findControllerByRouteId(String routeId) {
        List<SessionRecord> sessionRecords;
        synchronized (mLock) {
            sessionRecords = new ArrayList<>(mSessionRecords.values());
        }
        for (SessionRecord sessionRecord : sessionRecords) {
            RouteController controller = sessionRecord.findControllerByRouteId(routeId);
            if (controller != null) {
                return controller;
            }
        }
        return null;
    }

    MediaRouteProvider getMediaRouteProvider() {
        MediaRouteProviderService service = mServiceImpl.getService();
        if (service == null) {
            return null;
        }
        return service.getMediaRouteProvider();
    }

    private String assignSessionId(SessionRecord sessionRecord) {
        String sessionId;
        synchronized (mLock) {
            do {
                //TODO: Consider a better way to create a session ID.
                sessionId = UUID.randomUUID().toString();
            } while (mSessionRecords.containsKey(sessionId));
            sessionRecord.mSessionId = sessionId;
            mSessionRecords.put(sessionId, sessionRecord);
            return sessionId;
        }
    }

    private static class DynamicGroupRouteControllerProxy
            extends DynamicGroupRouteController {
        private final String mRouteId;
        final RouteController mRouteController;

        DynamicGroupRouteControllerProxy(String routeId, RouteController routeController) {
            mRouteId = routeId;
            mRouteController = routeController;
        }

        public String getRouteId() {
            return mRouteId;
        }

        @Override
        public void onRelease() {
            mRouteController.onRelease();
        }

        @Override
        public void onSelect() {
            mRouteController.onSelect();
        }

        @Override
        public void onUnselect(@MediaRouter.UnselectReason int reason) {
            mRouteController.onUnselect(reason);
        }

        @Override
        public void onSetVolume(int volume) {
            mRouteController.onSetVolume(volume);
        }

        @Override
        public void onUpdateVolume(int delta) {
            mRouteController.onUpdateVolume(delta);
        }

        @Override
        public boolean onControlRequest(@NonNull Intent intent,
                MediaRouter.ControlRequestCallback callback) {
            return mRouteController.onControlRequest(intent, callback);
        }

        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
            // Do nothing.
        }

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {
            // Do nothing.
        }

        @Override
        public void onRemoveMemberRoute(@NonNull String routeId) {
            // Do nothing.
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    final class SessionRecord {
        /**
         * A flag indicating whether the session is created from
         * {@link MediaRoute2ProviderService#onCreateSession(long, String, String, Bundle)}.
         */
        static final int SESSION_FLAG_MR2 = 1 << 0;

        /**
         * A flag indicating whether the session is a group.
         * If true, route controllers for members should be managed.
         */
        static final int SESSION_FLAG_GROUP = 1 << 1;

        /**
         * A flag indicating whether the members of session can be dynamically changed.
         */
        static final int SESSION_FLAG_DYNAMIC = 1 << 2;

        private final Map<String, RouteController> mRouteIdToControllerMap = new ArrayMap<>();
        private final DynamicGroupRouteController mController;
        private final long mRequestId;
        private final int mFlags;
        private final WeakReference<ClientRecord> mClientRecord;

        private boolean mIsCreated = false;
        private boolean mIsReleased;
        private RoutingSessionInfo mSessionInfo;
        String mSessionId;
        // The ID of the route describing the session.
        String mRouteId;

        SessionRecord(DynamicGroupRouteController controller, long requestId, int flags) {
            this(controller, requestId, flags, null);
        }

        SessionRecord(DynamicGroupRouteController controller, long requestId, int flags,
                ClientRecord clientRecord) {
            mController = controller;
            mRequestId = requestId;
            mFlags = flags;
            mClientRecord = new WeakReference<>(clientRecord);
        }

        /**
         * Maps the provided {@code routeId} to the top level route controller of this session.
         *
         * <p>This method can be used for mapping a route id to a non-group static route controller.
         * The session record takes care of the creation of the member route controllers, but not of
         * the top level route controller, which is provided via the constructor. In the case of
         * non-group static routes, the top level route controller is the single route controller,
         * and has already been created, so we only need to map the corresponding route id to it.
         */
        public void setStaticMemberRouteId(String routeId) {
            mRouteIdToControllerMap.put(routeId, mController);
        }

        public int getFlags() {
            return mFlags;
        }

        DynamicGroupRouteController getGroupController() {
            return mController;
        }

        RouteController findControllerByRouteId(String routeId) {
            ClientRecord clientRecord = mClientRecord.get();
            // Controllers are managed by the client.
            if (clientRecord != null) {
                return clientRecord.findControllerByRouteId(routeId);
            }
            return mRouteIdToControllerMap.get(routeId);
        }

        void setSessionInfo(@NonNull RoutingSessionInfo sessionInfo) {
            if (mSessionInfo != null) {
                Log.w(TAG, "setSessionInfo: This shouldn't be called after sessionInfo is set");
                return;
            }
            Messenger messenger = new Messenger(new IncomingHandler(
                    MediaRoute2ProviderServiceAdapter.this, mSessionId));

            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(sessionInfo);

            Bundle controlHints = new Bundle();
            controlHints.putParcelable(MediaRouter2Utils.KEY_MESSENGER, messenger);
            controlHints.putString(MediaRouter2Utils.KEY_SESSION_NAME,
                    sessionInfo.getName() != null ? sessionInfo.getName().toString() : null);

            mSessionInfo = builder.setControlHints(controlHints).build();
        }

        public void updateSessionInfo(@Nullable MediaRouteDescriptor groupRoute,
                @Nullable Collection<DynamicRouteDescriptor> dynamicRouteDescriptors) {
            RoutingSessionInfo sessionInfo = mSessionInfo;

            if (sessionInfo == null) {
                Log.w(TAG, "updateSessionInfo: mSessionInfo is null. This shouldn't happen.");
                return;
            }

            if (groupRoute != null && !groupRoute.isEnabled()) {
                onReleaseSession(REQUEST_ID_NONE, mSessionId);
                return;
            }

            RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(sessionInfo);
            if (groupRoute != null) {
                mRouteId = groupRoute.getId();
                builder.setName(groupRoute.getName())
                        .setVolume(groupRoute.getVolume())
                        .setVolumeMax(groupRoute.getVolumeMax())
                        .setVolumeHandling(groupRoute.getVolumeHandling());

                builder.clearSelectedRoutes();

                if (groupRoute.getGroupMemberIds().isEmpty()) {
                    builder.addSelectedRoute(mRouteId);
                } else {
                    for (String memberRouteId : groupRoute.getGroupMemberIds()) {
                        builder.addSelectedRoute(memberRouteId);
                    }
                }

                Bundle controlHints = sessionInfo.getControlHints();
                if (controlHints == null) {
                    Log.w(TAG, "updateSessionInfo: controlHints is null. "
                            + "This shouldn't happen.");
                    controlHints = new Bundle();
                }
                controlHints.putString(MediaRouter2Utils.KEY_SESSION_NAME, groupRoute.getName());
                controlHints.putBundle(MediaRouter2Utils.KEY_GROUP_ROUTE, groupRoute.asBundle());
                builder.setControlHints(controlHints);
            }

            mSessionInfo = builder.build();

            if (dynamicRouteDescriptors != null && !dynamicRouteDescriptors.isEmpty()) {
                boolean hasSelectedRoute = false;

                builder.clearSelectedRoutes();
                builder.clearSelectableRoutes();
                builder.clearDeselectableRoutes();
                builder.clearTransferableRoutes();

                for (DynamicRouteDescriptor descriptor : dynamicRouteDescriptors) {
                    String routeId = descriptor.getRouteDescriptor().getId();
                    if (descriptor.mSelectionState == DynamicRouteDescriptor.SELECTING
                            || descriptor.mSelectionState == DynamicRouteDescriptor.SELECTED) {
                        builder.addSelectedRoute(routeId);
                        hasSelectedRoute = true;
                    }
                    if (descriptor.isGroupable()) {
                        builder.addSelectableRoute(routeId);
                    }
                    if (descriptor.isUnselectable()) {
                        builder.addDeselectableRoute(routeId);
                    }
                    if (descriptor.isTransferable()) {
                        builder.addTransferableRoute(routeId);
                    }
                }
                // Update the session info only when we have a selected route to prevent
                // IllegalArgumentException.
                if (hasSelectedRoute) {
                    mSessionInfo = builder.build();
                }
            }

            if (DEBUG) {
                Log.d(TAG, "updateSessionInfo: groupRoute=" + groupRoute
                        + ", sessionInfo=" + mSessionInfo);
            }

            if ((mFlags & (SESSION_FLAG_MR2 | SESSION_FLAG_DYNAMIC))
                    == (SESSION_FLAG_MR2 | SESSION_FLAG_DYNAMIC) && groupRoute != null) {
                updateMemberRouteControllers(groupRoute.getId(), sessionInfo, mSessionInfo);
            }

            if (!mIsCreated) {
                notifySessionCreated();
            } else {
                notifySessionUpdated(mSessionInfo);
            }
        }

        public void release(boolean shouldUnselect) {
            if (!mIsReleased) {
                // Release member controllers
                if ((mFlags & (SESSION_FLAG_MR2 | SESSION_FLAG_GROUP))
                        == (SESSION_FLAG_MR2 | SESSION_FLAG_GROUP)) {
                    updateMemberRouteControllers(null, mSessionInfo, null);
                }

                if (shouldUnselect) {
                    mController.onUnselect(MediaRouter.UNSELECT_REASON_STOPPED);
                    mController.onRelease();
                    if ((mFlags & SESSION_FLAG_MR2) == 0) {
                        ClientRecord clientRecord = mClientRecord.get();
                        if (clientRecord != null) {
                            RouteController controller = mController;
                            if (mController instanceof DynamicGroupRouteControllerProxy) {
                                controller = ((DynamicGroupRouteControllerProxy) mController)
                                        .mRouteController;
                            }
                            clientRecord.releaseControllerByProvider(controller, mRouteId);
                        }
                    }
                }
                mIsReleased = true;
                notifySessionReleased(mSessionId);
            }
        }

        public void updateMemberRouteControllers(String groupId, RoutingSessionInfo oldSession,
                RoutingSessionInfo newSession) {
            List<String> oldRouteIds = (oldSession == null) ? Collections.emptyList() :
                    oldSession.getSelectedRoutes();
            List<String> newRouteIds = (newSession == null) ? Collections.emptyList() :
                    newSession.getSelectedRoutes();

            for (String routeId : newRouteIds) {
                RouteController controller = findControllerByRouteId(routeId);
                if (controller == null) {
                    controller = getOrCreateRouteController(routeId, groupId);
                    controller.onSelect();
                }
            }
            for (String routeId : oldRouteIds) {
                if (!newRouteIds.contains(routeId)) {
                    releaseRouteControllerByRouteId(routeId);
                }
            }
        }

        private void notifySessionCreated() {
            if (mIsCreated) {
                Log.w(TAG, "notifySessionCreated: Routing session is already created.");
                return;
            }
            mIsCreated = true;
            MediaRoute2ProviderServiceAdapter.this.notifySessionCreated(mRequestId, mSessionInfo);
        }

        private RouteController getOrCreateRouteController(String routeId, String routeGroupId) {
            RouteController controller = mRouteIdToControllerMap.get(routeId);
            if (controller != null) {
                return controller;
            }

            controller = routeGroupId == null
                    ? getMediaRouteProvider().onCreateRouteController(routeId)
                    : getMediaRouteProvider().onCreateRouteController(routeId, routeGroupId);
            if (controller != null) {
                mRouteIdToControllerMap.put(routeId, controller);
            }
            return controller;
        }

        private boolean releaseRouteControllerByRouteId(String routeId) {
            RouteController controller = mRouteIdToControllerMap.remove(routeId);
            if (controller != null) {
                controller.onUnselect(UNSELECT_REASON_UNKNOWN);
                controller.onRelease();
                return true;
            }
            return false;
        }
    }

    static class IncomingHandler extends Handler {
        private final MediaRoute2ProviderServiceAdapter mServiceAdapter;
        private final String mSessionId;

        IncomingHandler(MediaRoute2ProviderServiceAdapter serviceAdapter, String sessionId) {
            super(Looper.myLooper());
            mServiceAdapter = serviceAdapter;
            mSessionId = sessionId;
        }

        @Override
        public void handleMessage(Message msg) {
            final Messenger messenger = msg.replyTo;
            final int what = msg.what;
            final int requestId = msg.arg1;
            final Object obj = msg.obj;
            final Bundle data = msg.getData();

            switch (what) {
                case CLIENT_MSG_ROUTE_CONTROL_REQUEST:
                    if (obj instanceof Intent) {
                        mServiceAdapter.onControlRequest(messenger, requestId,
                                mSessionId, (Intent) obj);
                    }
                    break;

                case CLIENT_MSG_SET_ROUTE_VOLUME: {
                    int volume = data.getInt(CLIENT_DATA_VOLUME, -1);
                    String routeId = data.getString(CLIENT_DATA_ROUTE_ID);
                    if (volume >= 0 && routeId != null) {
                        mServiceAdapter.setRouteVolume(routeId, volume);
                    }
                    break;
                }

                case CLIENT_MSG_UPDATE_ROUTE_VOLUME: {
                    int delta = data.getInt(CLIENT_DATA_VOLUME, 0);
                    String routeId = data.getString(CLIENT_DATA_ROUTE_ID);
                    if (delta != 0 && routeId != null) {
                        mServiceAdapter.updateRouteVolume(routeId, delta);
                    }
                    break;
                }
            }
        }
    }
}
