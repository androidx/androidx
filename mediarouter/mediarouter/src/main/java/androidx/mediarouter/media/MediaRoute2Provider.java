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
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_ROUTE_CHANGED;

import android.content.Context;
import android.content.Intent;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Provides routes and RouteController by using MediaRouter2.
 * This provider is added only when {@link MediaRouter#enableTransfer()} is called.
 */
@RequiresApi(Build.VERSION_CODES.R)
@SuppressWarnings({"unused", "ClassCanBeStatic"}) // TODO: Remove this.
class MediaRoute2Provider extends MediaRouteProvider {
    static final String TAG = "MR2Provider";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    final MediaRouter2 mMediaRouter2;
    final Callback mCallback;
    final Map<MediaRouter2.RoutingController, DynamicMediaRoute2Controller> mControllerMap =
            new ArrayMap<>();
    private final MediaRouter2.RouteCallback mRouteCallback = new RouteCallback();
    private final MediaRouter2.TransferCallback mTransferCallback = new TransferCallback();
    private final Handler mHandler;
    private final Executor mHandlerExecutor;

    private List<MediaRoute2Info> mRoutes = new ArrayList<>();

    MediaRoute2Provider(@NonNull Context context, @NonNull Callback callback) {
        super(context);
        mMediaRouter2 = MediaRouter2.getInstance(context);
        mCallback = callback;

        mHandler = new Handler();
        mHandlerExecutor = mHandler::post;
    }

    @Override
    public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
        if (MediaRouter.getGlobalCallbackCount() > 0) {
            mMediaRouter2.registerRouteCallback(mHandlerExecutor, mRouteCallback,
                    MediaRouter2Utils.toDiscoveryPreference(request));
            mMediaRouter2.registerTransferCallback(mHandlerExecutor, mTransferCallback);
        } else {
            mMediaRouter2.unregisterRouteCallback(mRouteCallback);
            mMediaRouter2.unregisterTransferCallback(mTransferCallback);
        }
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId) {
        return new MediaRoute2Controller(routeId, null);
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId,
            @NonNull String routeGroupId) {
        return new MediaRoute2Controller(routeId, routeGroupId);
    }

    @Nullable
    @Override
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId) {
        for (Map.Entry<MediaRouter2.RoutingController, DynamicMediaRoute2Controller> entry
                : mControllerMap.entrySet()) {
            DynamicMediaRoute2Controller controller = entry.getValue();
            if (TextUtils.equals(initialMemberRouteId, controller.mInitialMemberRouteId)) {
                return controller;
            }
        }
        return null;
    }

    public void transferTo(@NonNull String routeId) {
        MediaRoute2Info route = getRouteById(routeId);
        if (route == null) {
            Log.w(TAG, "Specified route not found. routeId=" + routeId);
            return;
        }
        mMediaRouter2.transferTo(route);
    }

    protected void refreshRoutes() {
        // Syetem routes should not be published by this provider.
        List<MediaRoute2Info> newRoutes = mMediaRouter2.getRoutes().stream().distinct()
                .filter(r -> !r.isSystemRoute())
                .collect(Collectors.toList());

        if (newRoutes.equals(mRoutes)) {
            return;
        }
        mRoutes = newRoutes;

        List<MediaRouteDescriptor> routeDescriptors = mRoutes.stream()
                .map(MediaRouter2Utils::toMediaRouteDescriptor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    abstract static class Callback {
        public abstract void onSelectRoute(@NonNull String routeDescriptorId,
                @MediaRouter.UnselectReason int reason);

        public abstract void onSelectFallbackRoute(@MediaRouter.UnselectReason int reason);

        public abstract void onReleaseController(@NonNull RouteController controller);
    }

    @RequiresApi(30)
    final class TransferCallback extends MediaRouter2.TransferCallback {
        @Override
        public void onTransfer(@NonNull MediaRouter2.RoutingController oldController,
                @NonNull MediaRouter2.RoutingController newController) {
            // TODO: Call onPrepareTransfer() when the API is added.
            mControllerMap.remove(oldController);
            if (newController == mMediaRouter2.getSystemController()) {
                mCallback.onSelectFallbackRoute(UNSELECT_REASON_ROUTE_CHANGED);
            } else {
                List<MediaRoute2Info> selectedRoutes = newController.getSelectedRoutes();
                if (selectedRoutes.isEmpty()) {
                    Log.w(TAG, "Selected routes are empty. This shouldn't happen.");
                    return;
                }
                // TODO: Select a group route when dynamic grouping.
                String routeId = selectedRoutes.get(0).getId();
                DynamicMediaRoute2Controller controller =
                        new DynamicMediaRoute2Controller(routeId, newController);
                mControllerMap.put(newController, controller);
                mCallback.onSelectRoute(routeId, UNSELECT_REASON_ROUTE_CHANGED);
            }
        }

        @Override
        public void onTransferFailure(@NonNull MediaRoute2Info requestedRoute) {
            Log.w(TAG, "Transfer failed. requestedRoute=" + requestedRoute);
        }

        @Override
        public void onStop(@NonNull MediaRouter2.RoutingController routingController) {
            RouteController routeController = mControllerMap.remove(routingController);
            mCallback.onReleaseController(routeController);
        }
    }

    // TODO: Implement this class by overriding every public method in RouteController.
    private class MediaRoute2Controller extends RouteController {
        final String mRouteId;
        final String mRouteGroupId;

        MediaRoute2Controller(@NonNull String routeId, @Nullable String routeGroupId) {
            mRouteId = routeId;
            mRouteGroupId = routeGroupId;
        }
    }

    // TODO: Implement this class by overriding every public method in DynamicGroupRouteController.
    private class DynamicMediaRoute2Controller extends DynamicGroupRouteController {
        final String mInitialMemberRouteId;
        final MediaRouter2.RoutingController mRoutingController;
        @Nullable
        final Messenger mServiceMessenger;
        @Nullable
        final Messenger mReceiveMessenger;
        final SparseArray<ControlRequestCallback> mPendingCallbacks = new SparseArray<>();

        int mNextRequestId = 0;

        DynamicMediaRoute2Controller(@NonNull String initialMemberRouteId,
                @NonNull MediaRouter2.RoutingController routingController) {
            mInitialMemberRouteId = initialMemberRouteId;
            mRoutingController = routingController;
            mServiceMessenger = getMessengerFromRoutingController(routingController);
            mReceiveMessenger = mServiceMessenger == null ? null :
                    new Messenger(new ReceiveHandler());
        }

        @Override
        public boolean onControlRequest(Intent intent, @Nullable ControlRequestCallback callback) {
            if (mRoutingController == null || mRoutingController.isReleased()
                    || mServiceMessenger == null) {
                return false;
            }

            int requestId = mNextRequestId++;
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
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {}

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {}

        @Override
        public void onRemoveMemberRoute(String routeId) {}

        class ReceiveHandler extends Handler {
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
}
