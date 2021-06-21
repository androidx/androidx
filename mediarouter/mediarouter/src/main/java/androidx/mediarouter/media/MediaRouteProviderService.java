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

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_MEMBER_ROUTE_ID;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_MEMBER_ROUTE_IDS;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_ID;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_LIBRARY_GROUP;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_UNSELECT_REASON;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_ADD_MEMBER_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_CREATE_DYNAMIC_GROUP_ROUTE_CONTROLLER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_CREATE_ROUTE_CONTROLLER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_REGISTER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_RELEASE_ROUTE_CONTROLLER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_REMOVE_MEMBER_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_ROUTE_CONTROL_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SELECT_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SET_DISCOVERY_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SET_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UNREGISTER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UNSELECT_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UPDATE_MEMBER_ROUTES;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UPDATE_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_VERSION_1;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_VERSION_4;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.DATA_KEY_DYNAMIC_ROUTE_DESCRIPTORS;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.DATA_KEY_GROUPABLE_SECION_TITLE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.DATA_KEY_GROUP_ROUTE_DESCRIPTOR;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.DATA_KEY_TRANSFERABLE_SECTION_TITLE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_DATA_ERROR;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROLLER_RELEASED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_FAILED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DESCRIPTOR_CHANGED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DYNAMIC_ROUTE_CREATED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DYNAMIC_ROUTE_DESCRIPTORS_CHANGED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_GENERIC_FAILURE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_GENERIC_SUCCESS;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_REGISTERED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_VERSION_CURRENT;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.isValidRemoteMessenger;
import static androidx.mediarouter.media.MediaRouter.UNSELECT_REASON_UNKNOWN;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.OnDynamicRoutesChangedListener;
import androidx.mediarouter.media.MediaRouteProvider.RouteController;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for media route provider services.
 * <p>
 * A media router will bind to media route provider services when a callback is added via
 * {@link MediaRouter#addCallback(MediaRouteSelector, MediaRouter.Callback, int)} with a discovery
 * flag: {@link MediaRouter#CALLBACK_FLAG_REQUEST_DISCOVERY},
 * {@link MediaRouter#CALLBACK_FLAG_FORCE_DISCOVERY}, or
 * {@link MediaRouter#CALLBACK_FLAG_PERFORM_ACTIVE_SCAN}, and will unbind when the callback
 * is removed via {@link MediaRouter#removeCallback(MediaRouter.Callback)}.
 * </p><p>
 * To implement your own media route provider service, extend this class and
 * override the {@link #onCreateMediaRouteProvider} method to return an
 * instance of your {@link MediaRouteProvider}.
 * </p><p>
 * Declare your media route provider service in your application manifest
 * like this:
 * </p>
 * <pre>
 *   &lt;service android:name=".MyMediaRouteProviderService"
 *           android:label="@string/my_media_route_provider_service">
 *       &lt;intent-filter>
 *           &lt;action android:name="android.media.MediaRouteProviderService" />
 *       &lt;/intent-filter>
 *   &lt;/service>
 * </pre>
 */
public abstract class MediaRouteProviderService extends Service {
    static final String TAG = "MediaRouteProviderSrv"; // max. 23 chars
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ReceiveHandler mReceiveHandler;
    final Messenger mReceiveMessenger;
    final PrivateHandler mPrivateHandler;
    private final MediaRouteProvider.Callback mProviderCallback;

    MediaRouteProvider mProvider;
    final MediaRouteProviderServiceImpl mImpl;

    /**
     * The {@link Intent} that must be declared as handled by the service.
     * Put this in your manifest.
     */
    public static final String SERVICE_INTERFACE = MediaRouteProviderProtocol.SERVICE_INTERFACE;

    /*
     * Private messages used internally.  (Yes, you can renumber these.)
     */

    static final int PRIVATE_MSG_CLIENT_DIED = 1;

    interface MediaRouteProviderServiceImpl {
        IBinder onBind(Intent intent);
        void attachBaseContext(Context context);
        void onBinderDied(Messenger messenger);
        boolean onRegisterClient(Messenger messenger, int requestId, int version,
                String packageName);
        boolean onUnregisterClient(Messenger messenger, int requestId);
        boolean onCreateRouteController(Messenger messenger, int requestId,
                int controllerId, String routeId, String routeGroupId);
        boolean onCreateDynamicGroupRouteController(Messenger messenger, int requestId,
                int controllerId, String initialMemberRouteId);
        boolean onAddMemberRoute(Messenger messenger, int requestId, int controllerId,
                String memberId);
        boolean onRemoveMemberRoute(Messenger messenger, int requestId, int controllerId,
                String memberId);
        boolean onUpdateMemberRoutes(Messenger messenger, int requestId, int controllerId,
                List<String> memberIds);
        boolean onReleaseRouteController(Messenger messenger, int requestId,
                int controllerId);
        boolean onSelectRoute(Messenger messenger, int requestId,
                int controllerId);
        boolean onUnselectRoute(Messenger messenger, int requestId,
                int controllerId, @MediaRouter.UnselectReason int reason);
        boolean onSetRouteVolume(Messenger messenger, int requestId,
                int controllerId, int volume);
        boolean onUpdateRouteVolume(Messenger messenger, int requestId,
                int controllerId, int delta);
        boolean onRouteControlRequest(Messenger messenger, int requestId,
                int controllerId, Intent intent);
        boolean onSetDiscoveryRequest(Messenger messenger, int requestId,
                MediaRouteDiscoveryRequest request);
        MediaRouteProvider.Callback getProviderCallback();
    }

    /**
     * Creates a media route provider service.
     */
    public MediaRouteProviderService() {
        mReceiveHandler = new ReceiveHandler(this);
        mReceiveMessenger = new Messenger(mReceiveHandler);
        mPrivateHandler = new PrivateHandler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mImpl = new MediaRouteProviderServiceImplApi30(this);
        } else {
            mImpl = new MediaRouteProviderServiceImplBase(this);
        }
        mProviderCallback = mImpl.getProviderCallback();
    }

    /**
     * Called by the system when it is time to create the media route provider.
     *
     * @return The media route provider offered by this service, or null if
     * this service has decided not to offer a media route provider.
     */
    @Nullable
    public abstract MediaRouteProvider onCreateMediaRouteProvider();

    @Override
    @Nullable
    public IBinder onBind(@NonNull Intent intent) {
        return mImpl.onBind(intent);
    }

    @Override
    protected void attachBaseContext(@NonNull Context context) {
        super.attachBaseContext(context);
        mImpl.attachBaseContext(context);
    }

    /**
     * Gets the media route provider offered by this service.
     *
     * @return The media route provider offered by this service, or null if
     * it has not yet been created.
     *
     * @see #onCreateMediaRouteProvider()
     */
    @Nullable
    public MediaRouteProvider getMediaRouteProvider() {
        return mProvider;
    }

    void ensureProvider() {
        if (mProvider == null) {
            MediaRouteProvider provider = onCreateMediaRouteProvider();
            if (provider != null) {
                String providerPackage = provider.getMetadata().getPackageName();
                if (!providerPackage.equals(getPackageName())) {
                    throw new IllegalStateException("onCreateMediaRouteProvider() returned "
                            + "a provider whose package name does not match the package "
                            + "name of the service.  A media route provider service can "
                            + "only export its own media route providers.  "
                            + "Provider package name: " + providerPackage
                            + ".  Service package name: " + getPackageName() + ".");
                }
                mProvider = provider;
                mProvider.setCallback(mProviderCallback);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mProvider != null) {
            mProvider.setCallback(null);
        }
        super.onDestroy();
    }

    @VisibleForTesting
    static Bundle createDescriptorBundleForClientVersion(MediaRouteProviderDescriptor descriptor,
            int clientVersion) {
        if (descriptor == null) {
            return null;
        }
        MediaRouteProviderDescriptor.Builder builder =
                new MediaRouteProviderDescriptor.Builder(descriptor);
        builder.setRoutes(null);
        // Disable dynamic grouping for old clients
        if (clientVersion < CLIENT_VERSION_4) {
            builder.setSupportsDynamicGroupRoute(false);
        }
        for (MediaRouteDescriptor route : descriptor.getRoutes()) {
            if (clientVersion >= route.getMinClientVersion()
                    && clientVersion <= route.getMaxClientVersion()) {
                builder.addRoute(route);
            }
        }
        return builder.build().asBundle();
    }


    static void sendGenericFailure(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendMessage(messenger, SERVICE_MSG_GENERIC_FAILURE, requestId, 0, null, null);
        }
    }

    static void sendGenericSuccess(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendMessage(messenger, SERVICE_MSG_GENERIC_SUCCESS, requestId, 0, null, null);
        }
    }

    static void sendMessage(Messenger messenger, int what,
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
            Log.e(TAG, "Could not send message to " + getClientId(messenger), ex);
        }
    }

    static String getClientId(Messenger messenger) {
        return "Client connection " + messenger.getBinder().toString();
    }

    private final class PrivateHandler extends Handler {
        PrivateHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PRIVATE_MSG_CLIENT_DIED:
                    mImpl.onBinderDied((Messenger) msg.obj);
                    break;
            }
        }
    }

    /**
     * Handler that receives messages from clients.
     * <p>
     * This inner class is static and only retains a weak reference to the service
     * to prevent the service from being leaked in case one of the clients is holding an
     * active reference to the server's messenger.
     * </p><p>
     * This handler should not be used to handle any messages other than those
     * that come from the client.
     * </p>
     */
    private static final class ReceiveHandler extends Handler {
        private final WeakReference<MediaRouteProviderService> mServiceRef;

        public ReceiveHandler(MediaRouteProviderService service) {
            mServiceRef = new WeakReference<MediaRouteProviderService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            final Messenger messenger = msg.replyTo;
            if (isValidRemoteMessenger(messenger)) {
                final int what = msg.what;
                final int requestId = msg.arg1;
                final int arg = msg.arg2;
                final Object obj = msg.obj;
                final Bundle data = msg.peekData();

                String packageName = null;
                if (what == CLIENT_MSG_REGISTER && Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.LOLLIPOP) {
                    String[] packages = mServiceRef.get().getPackageManager()
                            .getPackagesForUid(msg.sendingUid);
                    if (packages != null && packages.length > 0) {
                        packageName = packages[0];
                    }
                }

                if (!processMessage(what, messenger, requestId, arg, obj, data, packageName)) {
                    if (DEBUG) {
                        Log.d(TAG, getClientId(messenger) + ": Message failed, what=" + what
                                + ", requestId=" + requestId + ", arg=" + arg
                                + ", obj=" + obj + ", data=" + data);
                    }
                    sendGenericFailure(messenger, requestId);
                }
            } else {
                if (DEBUG) {
                    Log.d(TAG, "Ignoring message without valid reply messenger.");
                }
            }
        }

        private boolean processMessage(int what, Messenger messenger,
                int requestId, int arg, Object obj, Bundle data, String packageName) {
            MediaRouteProviderService service = mServiceRef.get();
            if (service != null) {
                switch (what) {
                    case CLIENT_MSG_REGISTER:
                        return service.mImpl.onRegisterClient(messenger, requestId, arg,
                                packageName);

                    case CLIENT_MSG_UNREGISTER:
                        return service.mImpl.onUnregisterClient(messenger, requestId);

                    case CLIENT_MSG_CREATE_ROUTE_CONTROLLER: {
                        String routeId = data.getString(CLIENT_DATA_ROUTE_ID);
                        String routeGroupId =
                                data.getString(CLIENT_DATA_ROUTE_LIBRARY_GROUP);
                        if (routeId != null) {
                            return service.mImpl.onCreateRouteController(
                                    messenger, requestId, arg, routeId, routeGroupId);
                        }
                        break;
                    }

                    case CLIENT_MSG_CREATE_DYNAMIC_GROUP_ROUTE_CONTROLLER: {
                        String initialMemberId = data.getString(CLIENT_DATA_MEMBER_ROUTE_ID);
                        if (initialMemberId != null) {
                            return service.mImpl.onCreateDynamicGroupRouteController(
                                    messenger, requestId, arg, initialMemberId);
                        }
                        break;
                    }

                    case CLIENT_MSG_ADD_MEMBER_ROUTE: {
                        String memberId = data.getString(CLIENT_DATA_MEMBER_ROUTE_ID);
                        if (memberId != null) {
                            return service.mImpl.onAddMemberRoute(messenger, requestId, arg,
                                    memberId);
                        }
                        break;
                    }

                    case CLIENT_MSG_REMOVE_MEMBER_ROUTE: {
                        String memberId = data.getString(CLIENT_DATA_MEMBER_ROUTE_ID);
                        if (memberId != null) {
                            return service.mImpl.onRemoveMemberRoute(messenger, requestId, arg,
                                    memberId);
                        }
                        break;
                    }

                    case CLIENT_MSG_UPDATE_MEMBER_ROUTES: {
                        ArrayList<String> memberIds =
                                data.getStringArrayList(CLIENT_DATA_MEMBER_ROUTE_IDS);
                        if (memberIds != null) {
                            return service.mImpl.onUpdateMemberRoutes(
                                    messenger, requestId, arg, memberIds);
                        }
                        break;
                    }

                    case CLIENT_MSG_RELEASE_ROUTE_CONTROLLER:
                        return service.mImpl.onReleaseRouteController(messenger, requestId, arg);

                    case CLIENT_MSG_SELECT_ROUTE:
                        return service.mImpl.onSelectRoute(messenger, requestId, arg);

                    case CLIENT_MSG_UNSELECT_ROUTE:
                        int reason = data == null ?
                                UNSELECT_REASON_UNKNOWN
                                : data.getInt(CLIENT_DATA_UNSELECT_REASON,
                                        UNSELECT_REASON_UNKNOWN);
                        return service.mImpl.onUnselectRoute(messenger, requestId, arg, reason);

                    case CLIENT_MSG_SET_ROUTE_VOLUME: {
                        int volume = data.getInt(CLIENT_DATA_VOLUME, -1);
                        if (volume >= 0) {
                            return service.mImpl.onSetRouteVolume(
                                    messenger, requestId, arg, volume);
                        }
                        break;
                    }

                    case CLIENT_MSG_UPDATE_ROUTE_VOLUME: {
                        int delta = data.getInt(CLIENT_DATA_VOLUME, 0);
                        if (delta != 0) {
                            return service.mImpl.onUpdateRouteVolume(
                                    messenger, requestId, arg, delta);
                        }
                        break;
                    }

                    case CLIENT_MSG_ROUTE_CONTROL_REQUEST:
                        if (obj instanceof Intent) {
                            return service.mImpl.onRouteControlRequest(
                                    messenger, requestId, arg, (Intent)obj);
                        }
                        break;

                    case CLIENT_MSG_SET_DISCOVERY_REQUEST: {
                        if (obj == null || obj instanceof Bundle) {
                            MediaRouteDiscoveryRequest request =
                                    MediaRouteDiscoveryRequest.fromBundle((Bundle)obj);
                            return service.mImpl.onSetDiscoveryRequest(
                                    messenger, requestId,
                                    request != null && request.isValid() ? request : null);
                        }
                    }
                }
            }
            return false;
        }
    }

    static class MediaRouteProviderServiceImplBase implements MediaRouteProviderServiceImpl {
        final MediaRouteProviderService mService;
        final ArrayList<ClientRecord> mClients = new ArrayList<ClientRecord>();
        MediaRouteDiscoveryRequest mCompositeDiscoveryRequest;
        MediaRouteDiscoveryRequest mBaseDiscoveryRequest;
        long mBaseDiscoveryRequestTimestamp;
        private final MediaRouterActiveScanThrottlingHelper mActiveScanThrottlingHelper =
                new MediaRouterActiveScanThrottlingHelper(new Runnable() {
                    @Override
                    public void run() {
                        updateCompositeDiscoveryRequest();
                    }
                });

        MediaRouteProviderServiceImplBase(MediaRouteProviderService service) {
            mService = service;
        }

        public MediaRouteProviderService getService() {
            return mService;
        }

        @Override
        public IBinder onBind(Intent intent) {
            if (intent.getAction().equals(SERVICE_INTERFACE)) {
                mService.ensureProvider();
                if (mService.getMediaRouteProvider() != null) {
                    return mService.mReceiveMessenger.getBinder();
                }
            }
            return null;
        }

        @Override
        public void attachBaseContext(Context context) {}

        @Override
        public MediaRouteProvider.Callback getProviderCallback() {
            return new ProviderCallbackBase();
        }

        @Override
        public boolean onRegisterClient(Messenger messenger, int requestId, int version,
                String packageName) {
            if (version >= CLIENT_VERSION_1) {
                int index = findClient(messenger);
                if (index < 0) {
                    ClientRecord client = createClientRecord(messenger, version, packageName);
                    if (client.register()) {
                        mClients.add(client);
                        if (DEBUG) {
                            Log.d(TAG, client + ": Registered, version=" + version);
                        }
                        if (requestId != 0) {
                            MediaRouteProviderDescriptor descriptor =
                                    mService.getMediaRouteProvider().getDescriptor();
                            sendMessage(messenger, SERVICE_MSG_REGISTERED,
                                    requestId, SERVICE_VERSION_CURRENT,
                                    createDescriptorBundleForClientVersion(descriptor,
                                            client.mVersion), null);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onUnregisterClient(Messenger messenger, int requestId) {
            int index = findClient(messenger);
            if (index >= 0) {
                ClientRecord client = mClients.remove(index);
                if (DEBUG) {
                    Log.d(TAG, client + ": Unregistered");
                }
                client.dispose();
                sendGenericSuccess(messenger, requestId);
                return true;
            }
            return false;
        }

        @Override
        public void onBinderDied(Messenger messenger) {
            int index = findClient(messenger);
            if (index >= 0) {
                ClientRecord client = mClients.remove(index);
                if (DEBUG) {
                    Log.d(TAG, client + ": Binder died");
                }
                client.dispose();
            }
        }

        @Override
        public boolean onCreateRouteController(Messenger messenger, int requestId,
                int controllerId, String routeId, String routeGroupId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                if (client.createRouteController(routeId, routeGroupId, controllerId)) {
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route controller created, controllerId="
                                + controllerId + ", routeId=" + routeId
                                + ", routeGroupId=" + routeGroupId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateDynamicGroupRouteController(Messenger messenger, int requestId,
                int controllerId, String initialMemberRouteId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                Bundle bundle = client.createDynamicGroupRouteController(
                        initialMemberRouteId, controllerId);
                if (bundle != null) {
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route controller created, controllerId="
                                + controllerId
                                + ", initialMemberRouteId=" + initialMemberRouteId);
                    }
                    sendMessage(messenger, SERVICE_MSG_DYNAMIC_ROUTE_CREATED,
                            requestId, SERVICE_VERSION_CURRENT,
                            bundle, null);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onAddMemberRoute(Messenger messenger, int requestId, int controllerId,
                String memberId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller instanceof MediaRouteProvider.DynamicGroupRouteController) {
                    ((MediaRouteProvider.DynamicGroupRouteController) controller)
                            .onAddMemberRoute(memberId);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Added a member route"
                                + ", controllerId=" + controllerId + ", memberId=" + memberId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onRemoveMemberRoute(Messenger messenger, int requestId, int controllerId,
                String memberId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller instanceof MediaRouteProvider.DynamicGroupRouteController) {
                    ((MediaRouteProvider.DynamicGroupRouteController) controller)
                            .onRemoveMemberRoute(memberId);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Removed a member route"
                                + ", controllerId=" + controllerId + ", memberId=" + memberId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onUpdateMemberRoutes(Messenger messenger, int requestId, int controllerId,
                List<String> memberIds) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller instanceof MediaRouteProvider.DynamicGroupRouteController) {
                    ((MediaRouteProvider.DynamicGroupRouteController) controller)
                            .onUpdateMemberRoutes(memberIds);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Updated list of member routes"
                                + ", controllerId=" + controllerId + ", memberIds=" + memberIds);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onReleaseRouteController(Messenger messenger, int requestId,
                int controllerId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                if (client.releaseRouteController(controllerId)) {
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route controller released"
                                + ", controllerId=" + controllerId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSelectRoute(Messenger messenger, int requestId,
                int controllerId) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller != null) {
                    controller.onSelect();
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route selected"
                                + ", controllerId=" + controllerId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onUnselectRoute(Messenger messenger, int requestId,
                int controllerId, @MediaRouter.UnselectReason int reason) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller != null) {
                    controller.onUnselect(reason);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route unselected"
                                + ", controllerId=" + controllerId);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onSetRouteVolume(Messenger messenger, int requestId,
                int controllerId, int volume) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller != null) {
                    controller.onSetVolume(volume);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route volume changed"
                                + ", controllerId=" + controllerId + ", volume=" + volume);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onUpdateRouteVolume(Messenger messenger, int requestId,
                int controllerId, int delta) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller != null) {
                    controller.onUpdateVolume(delta);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Route volume updated"
                                + ", controllerId=" + controllerId + ", delta=" + delta);
                    }
                    sendGenericSuccess(messenger, requestId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onRouteControlRequest(final Messenger messenger, final int requestId,
                final int controllerId, final Intent intent) {
            final ClientRecord client = getClient(messenger);
            if (client != null) {
                RouteController controller =
                        client.getRouteController(controllerId);
                if (controller != null) {
                    MediaRouter.ControlRequestCallback callback = null;
                    if (requestId != 0) {
                        callback = new MediaRouter.ControlRequestCallback() {
                            @Override
                            public void onResult(Bundle data) {
                                if (DEBUG) {
                                    Log.d(TAG, client + ": Route control request succeeded"
                                            + ", controllerId=" + controllerId
                                            + ", intent=" + intent
                                            + ", data=" + data);
                                }
                                if (findClient(messenger) >= 0) {
                                    sendMessage(messenger, SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED,
                                            requestId, 0, data, null);
                                }
                            }

                            @Override
                            public void onError(String error, Bundle data) {
                                if (DEBUG) {
                                    Log.d(TAG, client + ": Route control request failed"
                                            + ", controllerId=" + controllerId
                                            + ", intent=" + intent
                                            + ", error=" + error + ", data=" + data);
                                }
                                if (findClient(messenger) >= 0) {
                                    if (error != null) {
                                        Bundle bundle = new Bundle();
                                        bundle.putString(SERVICE_DATA_ERROR, error);
                                        sendMessage(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
                                                requestId, 0, data, bundle);
                                    } else {
                                        sendMessage(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
                                                requestId, 0, data, null);
                                    }
                                }
                            }
                        };
                    }
                    if (controller.onControlRequest(intent, callback)) {
                        if (DEBUG) {
                            Log.d(TAG, client + ": Route control request delivered"
                                    + ", controllerId=" + controllerId + ", intent=" + intent);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean onSetDiscoveryRequest(Messenger messenger, int requestId,
                MediaRouteDiscoveryRequest request) {
            ClientRecord client = getClient(messenger);
            if (client != null) {
                boolean actuallyChanged = client.setDiscoveryRequest(request);
                if (DEBUG) {
                    Log.d(TAG, client + ": Set discovery request, request=" + request
                            + ", actuallyChanged=" + actuallyChanged
                            + ", compositeDiscoveryRequest=" + mCompositeDiscoveryRequest);
                }
                sendGenericSuccess(messenger, requestId);
                return true;
            }
            return false;
        }

        void sendDescriptorChanged(MediaRouteProviderDescriptor descriptor) {
            final int count = mClients.size();
            for (int i = 0; i < count; i++) {
                ClientRecord client = mClients.get(i);
                sendMessage(client.mMessenger, SERVICE_MSG_DESCRIPTOR_CHANGED, 0, 0,
                        client.createDescriptorBundle(descriptor), null);
                if (DEBUG) {
                    Log.d(TAG, client + ": Sent descriptor change event, descriptor=" + descriptor);
                }
            }
        }

        boolean setBaseDiscoveryRequest(MediaRouteDiscoveryRequest request) {
            long timestamp = SystemClock.elapsedRealtime();
            if (!ObjectsCompat.equals(mBaseDiscoveryRequest, request) || request.isActiveScan()) {
                mBaseDiscoveryRequest = request;
                mBaseDiscoveryRequestTimestamp = timestamp;

                return updateCompositeDiscoveryRequest();
            }
            return false;
        }

        boolean updateCompositeDiscoveryRequest() {
            MediaRouteSelector.Builder selectorBuilder = null;
            mActiveScanThrottlingHelper.reset();

            if (mBaseDiscoveryRequest != null) {
                mActiveScanThrottlingHelper.requestActiveScan(
                        mBaseDiscoveryRequest.isActiveScan(),
                        mBaseDiscoveryRequestTimestamp);
                selectorBuilder = new MediaRouteSelector.Builder(
                        mBaseDiscoveryRequest.getSelector());
            }

            final int count = mClients.size();
            for (int i = 0; i < count; i++) {
                ClientRecord clientRecord = mClients.get(i);
                MediaRouteDiscoveryRequest request = clientRecord.mDiscoveryRequest;
                if (request != null
                        && (!request.getSelector().isEmpty() || request.isActiveScan())) {
                    mActiveScanThrottlingHelper.requestActiveScan(request.isActiveScan(),
                            clientRecord.mDiscoveryRequestTimestamp);
                    if (selectorBuilder == null) {
                        selectorBuilder = new MediaRouteSelector.Builder(request.getSelector());
                    } else {
                        selectorBuilder.addSelector(request.getSelector());
                    }
                }
            }

            boolean activeScan =
                    mActiveScanThrottlingHelper
                            .finalizeActiveScanAndScheduleSuppressActiveScanRunnable();
            MediaRouteDiscoveryRequest composite = (selectorBuilder == null) ? null
                    : new MediaRouteDiscoveryRequest(selectorBuilder.build(), activeScan);
            if (!ObjectsCompat.equals(mCompositeDiscoveryRequest, composite)) {
                mCompositeDiscoveryRequest = composite;
                mService.getMediaRouteProvider().setDiscoveryRequest(composite);
                return true;
            }
            return false;
        }

        private ClientRecord getClient(Messenger messenger) {
            int index = findClient(messenger);
            return index >= 0 ? mClients.get(index) : null;
        }

        int findClient(Messenger messenger) {
            final int count = mClients.size();
            for (int i = 0; i < count; i++) {
                ClientRecord client = mClients.get(i);
                if (client.hasMessenger(messenger)) {
                    return i;
                }
            }
            return -1;
        }

        class ClientRecord implements DeathRecipient {
            public final Messenger mMessenger;
            public final int mVersion;
            public final String mPackageName;
            public MediaRouteDiscoveryRequest mDiscoveryRequest;
            public long mDiscoveryRequestTimestamp;

            final SparseArray<RouteController> mControllers = new SparseArray<>();

            final OnDynamicRoutesChangedListener mDynamicRoutesChangedListener =
                    new OnDynamicRoutesChangedListener() {
                        @Override
                        public void onRoutesChanged(
                                @NonNull DynamicGroupRouteController controller,
                                @NonNull MediaRouteDescriptor groupRoute,
                                @NonNull Collection<DynamicRouteDescriptor> routes) {
                            sendDynamicRouteDescriptors(controller, groupRoute, routes);
                        }
                    };

            ClientRecord(Messenger messenger, int version, String packageName) {
                mMessenger = messenger;
                mVersion = version;
                mPackageName = packageName;
            }

            public boolean register() {
                try {
                    mMessenger.getBinder().linkToDeath(this, 0);
                    return true;
                } catch (RemoteException ex) {
                    binderDied();
                }
                return false;
            }

            public void dispose() {
                int count = mControllers.size();
                for (int i = 0; i < count; i++) {
                    mControllers.valueAt(i).onRelease();
                }
                mControllers.clear();

                mMessenger.getBinder().unlinkToDeath(this, 0);

                setDiscoveryRequest(null);
            }

            public boolean hasMessenger(Messenger other) {
                return mMessenger.getBinder() == other.getBinder();
            }

            public boolean createRouteController(String routeId, String routeGroupId,
                    int controllerId) {
                if (mControllers.indexOfKey(controllerId) < 0) {
                    RouteController controller = routeGroupId == null
                            ? mService.getMediaRouteProvider().onCreateRouteController(routeId)
                            : mService.getMediaRouteProvider()
                                    .onCreateRouteController(routeId, routeGroupId);
                    if (controller != null) {
                        mControllers.put(controllerId, controller);
                        return true;
                    }
                }
                return false;
            }

            public Bundle createDynamicGroupRouteController(
                    String initialMemberRouteId, int controllerId) {
                if (mControllers.indexOfKey(controllerId) < 0) {
                    MediaRouteProvider.DynamicGroupRouteController controller =
                            mService.getMediaRouteProvider()
                                    .onCreateDynamicGroupRouteController(initialMemberRouteId);
                    if (controller != null) {
                        controller.setOnDynamicRoutesChangedListener(
                                ContextCompat.getMainExecutor(mService.getApplicationContext()),
                                mDynamicRoutesChangedListener);
                        mControllers.put(controllerId, controller);
                        Bundle bundle = new Bundle();
                        bundle.putString(DATA_KEY_GROUPABLE_SECION_TITLE,
                                controller.getGroupableSelectionTitle());
                        bundle.putString(DATA_KEY_TRANSFERABLE_SECTION_TITLE,
                                controller.getTransferableSectionTitle());
                        return bundle;
                    }
                }
                return null;
            }

            public boolean releaseRouteController(int controllerId) {
                RouteController controller = mControllers.get(controllerId);
                if (controller != null) {
                    mControllers.remove(controllerId);
                    controller.onRelease();
                    return true;
                }
                return false;
            }

            public RouteController getRouteController(int controllerId) {
                return mControllers.get(controllerId);
            }

            public boolean setDiscoveryRequest(MediaRouteDiscoveryRequest request) {
                long timestamp = SystemClock.elapsedRealtime();
                if (!ObjectsCompat.equals(mDiscoveryRequest, request)) {
                    mDiscoveryRequest = request;
                    mDiscoveryRequestTimestamp = timestamp;
                    return updateCompositeDiscoveryRequest();
                }

                return false;
            }

            /**
             * Creates a bundle of the given provider descriptor for this client.
             */
            public Bundle createDescriptorBundle(MediaRouteProviderDescriptor descriptor) {
                return createDescriptorBundleForClientVersion(descriptor, mVersion);
            }

            // Runs on a binder thread.
            @Override
            public void binderDied() {
                mService.mPrivateHandler
                        .obtainMessage(PRIVATE_MSG_CLIENT_DIED, mMessenger).sendToTarget();
            }

            @Override
            public String toString() {
                return getClientId(mMessenger);
            }

            void sendDynamicRouteDescriptors(
                    DynamicGroupRouteController controller,
                    MediaRouteDescriptor groupRoute,
                    Collection<DynamicRouteDescriptor> descriptors) {
                int index = mControllers.indexOfValue(controller);
                if (index < 0) {
                    Log.w(TAG, "Ignoring unknown dynamic group route controller: " + controller);
                    return;
                }
                int controllerId = mControllers.keyAt(index);

                ArrayList<Bundle> dynamicRouteBundles = new ArrayList<Bundle>();
                for (DynamicRouteDescriptor descriptor: descriptors) {
                    dynamicRouteBundles.add(descriptor.toBundle());
                }
                Bundle bundle = new Bundle();
                if (groupRoute != null) {
                    bundle.putParcelable(DATA_KEY_GROUP_ROUTE_DESCRIPTOR, groupRoute.asBundle());
                }
                bundle.putParcelableArrayList(DATA_KEY_DYNAMIC_ROUTE_DESCRIPTORS,
                        dynamicRouteBundles);
                sendMessage(mMessenger, SERVICE_MSG_DYNAMIC_ROUTE_DESCRIPTORS_CHANGED,
                        0, controllerId, bundle, null);
            }
        }

        ClientRecord createClientRecord(Messenger messenger, int version, String packageName) {
            return new ClientRecord(messenger, version, packageName);
        }

        class ProviderCallbackBase extends MediaRouteProvider.Callback {
            @Override
            public void onDescriptorChanged(@NonNull MediaRouteProvider provider,
                    MediaRouteProviderDescriptor descriptor) {
                sendDescriptorChanged(descriptor);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    static class MediaRouteProviderServiceImplApi30 extends MediaRouteProviderServiceImplBase {
        MediaRoute2ProviderServiceAdapter mMR2ProviderServiceAdapter;
        final OnDynamicRoutesChangedListener mDynamicRoutesChangedListener =
                (controller, groupRoute, routes) -> mMR2ProviderServiceAdapter
                        .setDynamicRouteDescriptor(controller, groupRoute, routes);

        MediaRouteProviderServiceImplApi30(MediaRouteProviderService instance) {
            super(instance);
        }

        @Override
        public IBinder onBind(Intent intent) {
            mService.ensureProvider();
            if (mMR2ProviderServiceAdapter == null) {
                mMR2ProviderServiceAdapter = new MediaRoute2ProviderServiceAdapter(this);
                if (mService.getBaseContext() != null) {
                    mMR2ProviderServiceAdapter.attachBaseContext(mService);
                }
            }
            IBinder binder = super.onBind(intent);
            if (binder != null) {
                return binder;
            }
            return mMR2ProviderServiceAdapter.onBind(intent);
        }

        @Override
        public void attachBaseContext(Context context) {
            if (mMR2ProviderServiceAdapter != null) {
                mMR2ProviderServiceAdapter.attachBaseContext(context);
            }
        }

        @Override
        void sendDescriptorChanged(MediaRouteProviderDescriptor descriptor) {
            super.sendDescriptorChanged(descriptor);
            mMR2ProviderServiceAdapter.setProviderDescriptor(descriptor);
        }


        @Override
        MediaRouteProviderServiceImplBase.ClientRecord createClientRecord(
                Messenger messenger, int version, String packageName) {
            return new ClientRecord(messenger, version, packageName);
        }

        void setDynamicRoutesChangedListener(DynamicGroupRouteController controller) {
            controller.setOnDynamicRoutesChangedListener(
                    ContextCompat.getMainExecutor(mService.getApplicationContext()),
                    mDynamicRoutesChangedListener);
        }

        class ClientRecord extends MediaRouteProviderServiceImplBase.ClientRecord {
            // Maps the route ID to member route controller.
            private final Map<String, RouteController> mRouteIdToControllerMap = new ArrayMap<>();
            private final Handler mClientHandler = new Handler(Looper.getMainLooper());

            // Maps disabled route ID to ID of controllers released by provider.
            private final Map<String, Integer> mReleasedControllerIds;
            private static final long DISABLE_ROUTE_FOR_RELEASED_CONTROLLER_TIMEOUT_MS = 5_000;

            ClientRecord(Messenger messenger, int version, String packageName) {
                super(messenger, version, packageName);
                // Only required by clients that don't support SERVICE_MSG_CONTROLLER_RELEASED.
                if (version < CLIENT_VERSION_4) {
                    mReleasedControllerIds = new ArrayMap<>();
                } else {
                    mReleasedControllerIds = Collections.emptyMap();
                }
            }

            @Override
            public void dispose() {
                int count = mControllers.size();
                for (int i = 0; i < count; i++) {
                    int controllerId = mControllers.keyAt(i);
                    mMR2ProviderServiceAdapter.notifyRouteControllerRemoved(controllerId);
                }
                mRouteIdToControllerMap.clear();
                super.dispose();
            }

            @Override
            public boolean createRouteController(String routeId, String routeGroupId,
                    int controllerId) {
                RouteController controller = mRouteIdToControllerMap.get(routeId);
                if (controller != null) {
                    mControllers.put(controllerId, controller);
                    return true;
                }

                boolean result = super.createRouteController(routeId, routeGroupId,
                        controllerId);
                // Don't add route controllers of member routes.
                if (routeGroupId == null && result && mPackageName != null) {
                    mMR2ProviderServiceAdapter.notifyRouteControllerAdded(
                            this, mControllers.get(controllerId),
                            controllerId, mPackageName, routeId);
                }
                if (result) {
                    mRouteIdToControllerMap.put(routeId, mControllers.get(controllerId));
                }
                return result;
            }

            @Override
            public Bundle createDynamicGroupRouteController(
                    String initialMemberRouteId, int controllerId) {
                Bundle result =
                        super.createDynamicGroupRouteController(initialMemberRouteId, controllerId);
                if (result != null && mPackageName != null) {
                    mMR2ProviderServiceAdapter.notifyRouteControllerAdded(
                            this, mControllers.get(controllerId),
                            controllerId, mPackageName, initialMemberRouteId);
                }
                return result;
            }

            @Override
            public boolean releaseRouteController(int controllerId) {
                mMR2ProviderServiceAdapter.notifyRouteControllerRemoved(controllerId);
                RouteController controller = mControllers.get(controllerId);
                if (controller != null) {
                    for (Map.Entry<String, RouteController> entry :
                            mRouteIdToControllerMap.entrySet()) {
                        if (entry.getValue() == controller) {
                            mRouteIdToControllerMap.remove(entry.getKey());
                            break;
                        }
                    }
                }
                for (Map.Entry<String, Integer> entry : mReleasedControllerIds.entrySet()) {
                    if (entry.getValue() == controllerId) {
                        enableRouteForReleasedRouteController(/*routeId=*/entry.getKey());
                        break;
                    }
                }
                return super.releaseRouteController(controllerId);
            }

            @Override
            void sendDynamicRouteDescriptors(
                    DynamicGroupRouteController controller,
                    MediaRouteDescriptor groupRoute,
                    Collection<DynamicRouteDescriptor> descriptors) {
                super.sendDynamicRouteDescriptors(controller, groupRoute, descriptors);
                if (mMR2ProviderServiceAdapter != null) {
                    mMR2ProviderServiceAdapter.setDynamicRouteDescriptor(controller,
                            groupRoute, descriptors);
                }
            }

            @Override
            public Bundle createDescriptorBundle(MediaRouteProviderDescriptor descriptor) {
                if (mReleasedControllerIds.isEmpty()) {
                    return super.createDescriptorBundle(descriptor);
                }

                List<MediaRouteDescriptor> routes = new ArrayList<>();
                for (MediaRouteDescriptor routeDescriptor : descriptor.getRoutes()) {
                    if (mReleasedControllerIds.containsKey(routeDescriptor.getId())) {
                        routes.add(new MediaRouteDescriptor.Builder(routeDescriptor)
                                .setEnabled(false).build());
                    } else {
                        routes.add(routeDescriptor);
                    }
                }

                MediaRouteProviderDescriptor newDescriptor =
                        new MediaRouteProviderDescriptor.Builder(descriptor)
                                .setRoutes(routes).build();
                return super.createDescriptorBundle(newDescriptor);
            }

            void releaseControllerByProvider(RouteController controller, String routeId) {
                int controllerId = findControllerIdByController(controller);
                releaseRouteController(controllerId);

                // Let the client unselect the corresponding route.
                if (mVersion >= CLIENT_VERSION_4) {
                    if (controllerId < 0) {
                        Log.w(TAG, "releaseControllerByProvider: Can't find the controller."
                                + " route ID=" + routeId);
                        return;
                    }
                    sendMessage(mMessenger, SERVICE_MSG_CONTROLLER_RELEASED, 0, controllerId,
                            null, null);
                } else {
                    // Use a workaround for old versions.
                    disableRouteForReleasedRouteController(routeId, controllerId);
                }
            }

            /**
             * Sets the route that corresponds to the given released route controller as disabled
             * so that media router client unselects the route and releases the route controller
             * on the client side.
             * The route becomes enabled when the route controller on the client side is released
             * or it times out.
             */
            private void disableRouteForReleasedRouteController(String routeId, int controllerId) {
                mReleasedControllerIds.put(routeId, controllerId);

                mClientHandler.postDelayed(() -> enableRouteForReleasedRouteController(routeId),
                        DISABLE_ROUTE_FOR_RELEASED_CONTROLLER_TIMEOUT_MS);
                sendDescriptor();
            }

            private void enableRouteForReleasedRouteController(String routeId) {
                if (mReleasedControllerIds.remove(routeId) == null) {
                    return;
                }
                sendDescriptor();
            }

            /**
             * Sends the lastly known provider descriptor to the client.
             */
            void sendDescriptor() {
                MediaRouteProviderDescriptor providerDescriptor =
                        getService().getMediaRouteProvider().getDescriptor();
                if (providerDescriptor != null) {
                    sendMessage(mMessenger, SERVICE_MSG_DESCRIPTOR_CHANGED, 0, 0,
                            createDescriptorBundle(providerDescriptor), null);
                }
            }

            public RouteController findControllerByRouteId(String routeId) {
                return mRouteIdToControllerMap.get(routeId);
            }

            public int findControllerIdByController(RouteController controller) {
                int index = mControllers.indexOfValue(controller);
                if (index < 0) return -1;
                return mControllers.keyAt(index);
            }
        }
    }
}
