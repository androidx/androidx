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

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_ROUTE_CONTROL_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_DATA_ERROR;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_FAILED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProviderService.MediaRouteProviderServiceImplApi30;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRoute2ProviderServiceStub extends MediaRoute2ProviderService {
    private static final String TAG = "MR2ProviderService";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();

    final MediaRouteProviderService mService;
    final MediaRouteProviderServiceImplApi30 mImpl;
    @GuardedBy("mLock")
    final Map<String, DynamicGroupRouteController> mControllers = new HashMap<>();
    //TODO: Remove these when xMR is finished
    final Map<String, Messenger> mMessengers = new HashMap<>();
    private static final String KEY_MESSENGER_BINDER = "binder";

    private volatile MediaRouteProviderDescriptor mProviderDescriptor;

    @SuppressLint("InlinedApi")
    public static final String SERVICE_INTERFACE = MediaRoute2ProviderService.SERVICE_INTERFACE;

    MediaRoute2ProviderServiceStub(MediaRouteProviderServiceImplApi30 impl) {
        mImpl = impl;
        mService = impl.getService();
    }

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
    }

    @Override
    public void onSetRouteVolume(long requestId, @NonNull String routeId, int volume) {
        //TODO: Implement this when necessary APIs are added.
    }

    @Override
    public void onSetSessionVolume(long requestId, @NonNull String sessionId, int volume) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onSetSessionVolume: Couldn't find a session");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }

        DynamicGroupRouteController controller = getController(sessionId);
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
        MediaRouteProvider provider = mService.getMediaRouteProvider();
        MediaRouteDescriptor selectedRoute = getRouteDescriptor(routeId, "onCreateSession");
        if (selectedRoute == null) {
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }

        DynamicGroupRouteController controller;
        if (mProviderDescriptor.supportsDynamicGroupRoute()) {
            controller = provider.onCreateDynamicGroupRouteController(routeId);
            if (controller == null) {
                Log.w(TAG, "onCreateSession: Couldn't create a dynamic controller");
                notifyRequestFailed(requestId, REASON_REJECTED);
                return;
            }
        } else {
            MediaRouteProvider.RouteController routeController =
                    provider.onCreateRouteController(routeId);
            if (routeController == null) {
                Log.w(TAG, "onCreateSession: Couldn't create a controller");
                notifyRequestFailed(requestId, REASON_REJECTED);
                return;
            }
            controller = new DynamicGroupRouteControllerProxy(routeController);
        }

        String sessionId;
        synchronized (mLock) {
            do {
                //TODO: Consider a better way to create a session ID.
                sessionId = UUID.randomUUID().toString();
            } while (mControllers.containsKey(sessionId));
            mControllers.put(sessionId, controller);
        }

        controller.onSelect();

        //TODO: Set the client package name of the selected route.

        RoutingSessionInfo.Builder builder =
                new RoutingSessionInfo.Builder(sessionId, packageName)
                        .addSelectedRoute(routeId)
                        .setName(selectedRoute.getName())
                        .setVolumeHandling(selectedRoute.getVolumeHandling())
                        .setVolume(selectedRoute.getVolume())
                        .setVolumeMax(selectedRoute.getVolumeMax());

        Messenger messenger = new Messenger(new IncomingHandler(this, sessionId));
        mMessengers.put(sessionId, messenger);

        Bundle controlHints = new Bundle();
        controlHints.putBinder(KEY_MESSENGER_BINDER, messenger.getBinder());
        builder.setControlHints(controlHints).build();

        // Dynamic grouping info will be notified by the provider.
        RoutingSessionInfo sessionInfo = builder.build();
        notifySessionCreated(requestId, sessionInfo);
    }

    @Override
    public void onReleaseSession(long requestId, @NonNull String sessionId) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            return;
        }
        DynamicGroupRouteController controller;
        synchronized (mLock) {
            controller = mControllers.remove(sessionId);
            mMessengers.remove(sessionId);
        }
        if (controller == null) {
            Log.w(TAG, "onReleaseSession: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_INVALID_COMMAND);
            return;
        }
        controller.onUnselect();
        controller.onRelease();
        notifySessionReleased(sessionId);
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

        DynamicGroupRouteController controller = getController(sessionId);
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

        DynamicGroupRouteController controller = getController(sessionId);
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

        DynamicGroupRouteController controller = getController(sessionId);
        if (controller == null) {
            Log.w(TAG, "onTransferToRoute: Couldn't find a controller");
            notifyRequestFailed(requestId, REASON_ROUTE_NOT_AVAILABLE);
            return;
        }
        controller.onUpdateMemberRoutes(Collections.singletonList(routeId));
    }

    @Override
    public void onDiscoveryPreferenceChanged(@NonNull RouteDiscoveryPreference preference) {
        MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategories(preference.getPreferredFeatures().stream()
                        .map(MediaRouter2Utils::toControlCategory)
                        .collect(Collectors.toList())).build();
        mImpl.setBaseDiscoveryRequest(new MediaRouteDiscoveryRequest(selector,
                preference.shouldPerformActiveScan()));
    }

    public void setProviderDescriptor(@Nullable MediaRouteProviderDescriptor descriptor) {
        mProviderDescriptor = descriptor;
        List<MediaRouteDescriptor> routeDescriptors =
                (descriptor == null) ? Collections.emptyList() : descriptor.getRoutes();
        // Handle duplicated IDs
        notifyRoutes(routeDescriptors.stream().map(MediaRouter2Utils::toFwkMediaRoute2Info)
                .collect(Collectors.toMap(r -> r.getId(), r -> r, (a, b) -> b)).values());
    }

    private DynamicGroupRouteController getController(String sessionId) {
        synchronized (mLock) {
            return mControllers.get(sessionId);
        }
    }

    private MediaRouteDescriptor getRouteDescriptor(String routeId, String description) {
        MediaRouteProvider provider = mService.getMediaRouteProvider();
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

    public void setDynamicRouteDescriptor(DynamicGroupRouteController controller,
            Collection<DynamicRouteDescriptor> descriptors) {
        String sessionId = null;
        synchronized (mLock) {
            for (Map.Entry<String, DynamicGroupRouteController> entry : mControllers.entrySet()) {
                if (entry.getValue() == controller) {
                    sessionId = entry.getKey();
                    break;
                }
            }
        }
        if (sessionId == null) {
            Log.w(TAG, "setDynamicRouteDescriptor: Couldn't find a routing session");
            return;
        }
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "setDynamicRouteDescriptor: Couldn't find a routing session");
            return;
        }

        RoutingSessionInfo.Builder builder = new RoutingSessionInfo.Builder(sessionInfo)
                .clearSelectedRoutes()
                .clearSelectableRoutes()
                .clearDeselectableRoutes()
                .clearTransferableRoutes();

        for (DynamicRouteDescriptor descriptor : descriptors) {
            String routeId = descriptor.getRouteDescriptor().getId();
            if (descriptor.mSelectionState == DynamicRouteDescriptor.SELECTING
                    || descriptor.mSelectionState == DynamicRouteDescriptor.SELECTED) {
                builder.addSelectedRoute(routeId);
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
        notifySessionUpdated(builder.build());
    }

    //TODO: Remove this
    void onControlRequest(Messenger messenger, int requestId, String sessionId,
            Intent intent) {
        RoutingSessionInfo sessionInfo = getSessionInfo(sessionId);
        if (sessionInfo == null) {
            Log.w(TAG, "onCustomCommand: Couldn't find a session");
            return;
        }

        DynamicGroupRouteController controller = getController(sessionId);
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

    private static class DynamicGroupRouteControllerProxy
            extends DynamicGroupRouteController {
        private final MediaRouteProvider.RouteController mRouteController;

        DynamicGroupRouteControllerProxy(MediaRouteProvider.RouteController routeController) {
            mRouteController = routeController;
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
        public void onUnselect() {
            mRouteController.onUnselect();
        }

        @Override
        public void onUnselect(int reason) {
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
        public boolean onControlRequest(Intent intent,
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
        public void onRemoveMemberRoute(String routeId) {
            // Do nothing.
        }
    }

    //TODO: Remove this and use the existing messenger
    static class IncomingHandler extends Handler {
        private final MediaRoute2ProviderServiceStub mStub;
        private final String mSessionId;

        IncomingHandler(MediaRoute2ProviderServiceStub stub, String sessionId) {
            super(Looper.myLooper());
            mStub = stub;
            mSessionId = sessionId;
        }

        @Override
        public void handleMessage(Message msg) {
            final Messenger messenger = msg.replyTo;
            final int what = msg.what;
            final int requestId = msg.arg1;
            final Object obj = msg.obj;

            switch (what) {
                case CLIENT_MSG_ROUTE_CONTROL_REQUEST:
                    if (obj instanceof Intent) {
                        mStub.onControlRequest(messenger, requestId, mSessionId, (Intent) obj);
                    }
                    break;
            }
        }
    }
}
