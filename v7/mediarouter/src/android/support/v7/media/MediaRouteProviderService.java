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

package android.support.v7.media;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static android.support.v7.media.MediaRouteProviderProtocol.*;

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
    private static final String TAG = "MediaRouteProviderSrv"; // max. 23 chars
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ArrayList<ClientRecord> mClients = new ArrayList<ClientRecord>();
    private final ReceiveHandler mReceiveHandler;
    private final Messenger mReceiveMessenger;
    private final PrivateHandler mPrivateHandler;
    private final ProviderCallback mProviderCallback;

    private MediaRouteProvider mProvider;
    private MediaRouteDiscoveryRequest mCompositeDiscoveryRequest;

    /**
     * The {@link Intent} that must be declared as handled by the service.
     * Put this in your manifest.
     */
    public static final String SERVICE_INTERFACE = MediaRouteProviderProtocol.SERVICE_INTERFACE;

    /*
     * Private messages used internally.  (Yes, you can renumber these.)
     */

    private static final int PRIVATE_MSG_CLIENT_DIED = 1;

    /**
     * Creates a media route provider service.
     */
    public MediaRouteProviderService() {
        mReceiveHandler = new ReceiveHandler(this);
        mReceiveMessenger = new Messenger(mReceiveHandler);
        mPrivateHandler = new PrivateHandler();
        mProviderCallback = new ProviderCallback();
    }

    /**
     * Called by the system when it is time to create the media route provider.
     *
     * @return The media route provider offered by this service, or null if
     * this service has decided not to offer a media route provider.
     */
    public abstract MediaRouteProvider onCreateMediaRouteProvider();

    /**
     * Gets the media route provider offered by this service.
     *
     * @return The media route provider offered by this service, or null if
     * it has not yet been created.
     *
     * @see #onCreateMediaRouteProvider()
     */
    public MediaRouteProvider getMediaRouteProvider() {
        return mProvider;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent.getAction().equals(SERVICE_INTERFACE)) {
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
            if (mProvider != null) {
                return mReceiveMessenger.getBinder();
            }
        }
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mProvider != null) {
            mProvider.setCallback(null);
        }
        return super.onUnbind(intent);
    }

    private boolean onRegisterClient(Messenger messenger, int requestId, int version) {
        if (version >= CLIENT_VERSION_1) {
            int index = findClient(messenger);
            if (index < 0) {
                ClientRecord client = new ClientRecord(messenger, version);
                if (client.register()) {
                    mClients.add(client);
                    if (DEBUG) {
                        Log.d(TAG, client + ": Registered, version=" + version);
                    }
                    if (requestId != 0) {
                        MediaRouteProviderDescriptor descriptor = mProvider.getDescriptor();
                        sendReply(messenger, SERVICE_MSG_REGISTERED,
                                requestId, SERVICE_VERSION_CURRENT,
                                descriptor != null ? descriptor.asBundle() : null, null);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean onUnregisterClient(Messenger messenger, int requestId) {
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

    private void onBinderDied(Messenger messenger) {
        int index = findClient(messenger);
        if (index >= 0) {
            ClientRecord client = mClients.remove(index);
            if (DEBUG) {
                Log.d(TAG, client + ": Binder died");
            }
            client.dispose();
        }
    }

    private boolean onCreateRouteController(Messenger messenger, int requestId,
            int controllerId, String routeId) {
        ClientRecord client = getClient(messenger);
        if (client != null) {
            if (client.createRouteController(routeId, controllerId)) {
                if (DEBUG) {
                    Log.d(TAG, client + ": Route controller created"
                            + ", controllerId=" + controllerId + ", routeId=" + routeId);
                }
                sendGenericSuccess(messenger, requestId);
                return true;
            }
        }
        return false;
    }

    private boolean onReleaseRouteController(Messenger messenger, int requestId,
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

    private boolean onSelectRoute(Messenger messenger, int requestId,
            int controllerId) {
        ClientRecord client = getClient(messenger);
        if (client != null) {
            MediaRouteProvider.RouteController controller =
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

    private boolean onUnselectRoute(Messenger messenger, int requestId,
            int controllerId, int reason) {
        ClientRecord client = getClient(messenger);
        if (client != null) {
            MediaRouteProvider.RouteController controller =
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

    private boolean onSetRouteVolume(Messenger messenger, int requestId,
            int controllerId, int volume) {
        ClientRecord client = getClient(messenger);
        if (client != null) {
            MediaRouteProvider.RouteController controller =
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

    private boolean onUpdateRouteVolume(Messenger messenger, int requestId,
            int controllerId, int delta) {
        ClientRecord client = getClient(messenger);
        if (client != null) {
            MediaRouteProvider.RouteController controller =
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

    private boolean onRouteControlRequest(final Messenger messenger, final int requestId,
            final int controllerId, final Intent intent) {
        final ClientRecord client = getClient(messenger);
        if (client != null) {
            MediaRouteProvider.RouteController controller =
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
                                sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED,
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
                                    sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
                                            requestId, 0, data, bundle);
                                } else {
                                    sendReply(messenger, SERVICE_MSG_CONTROL_REQUEST_FAILED,
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

    private boolean onSetDiscoveryRequest(Messenger messenger, int requestId,
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

    private void sendDescriptorChanged(MediaRouteProviderDescriptor descriptor) {
        Bundle descriptorBundle = descriptor != null ? descriptor.asBundle() : null;
        final int count = mClients.size();
        for (int i = 0; i < count; i++) {
            ClientRecord client = mClients.get(i);
            sendReply(client.mMessenger, SERVICE_MSG_DESCRIPTOR_CHANGED, 0, 0,
                    descriptorBundle, null);
            if (DEBUG) {
                Log.d(TAG, client + ": Sent descriptor change event, descriptor=" + descriptor);
            }
        }
    }

    private boolean updateCompositeDiscoveryRequest() {
        MediaRouteDiscoveryRequest composite = null;
        MediaRouteSelector.Builder selectorBuilder = null;
        boolean activeScan = false;
        final int count = mClients.size();
        for (int i = 0; i < count; i++) {
            MediaRouteDiscoveryRequest request = mClients.get(i).mDiscoveryRequest;
            if (request != null
                    && (!request.getSelector().isEmpty() || request.isActiveScan())) {
                activeScan |= request.isActiveScan();
                if (composite == null) {
                    composite = request;
                } else {
                    if (selectorBuilder == null) {
                        selectorBuilder = new MediaRouteSelector.Builder(composite.getSelector());
                    }
                    selectorBuilder.addSelector(request.getSelector());
                }
            }
        }
        if (selectorBuilder != null) {
            composite = new MediaRouteDiscoveryRequest(selectorBuilder.build(), activeScan);
        }
        if (mCompositeDiscoveryRequest != composite
                && (mCompositeDiscoveryRequest == null
                        || !mCompositeDiscoveryRequest.equals(composite))) {
            mCompositeDiscoveryRequest = composite;
            mProvider.setDiscoveryRequest(composite);
            return true;
        }
        return false;
    }

    private ClientRecord getClient(Messenger messenger) {
        int index = findClient(messenger);
        return index >= 0 ? mClients.get(index) : null;
    }

    private int findClient(Messenger messenger) {
        final int count = mClients.size();
        for (int i = 0; i < count; i++) {
            ClientRecord client = mClients.get(i);
            if (client.hasMessenger(messenger)) {
                return i;
            }
        }
        return -1;
    }

    private static void sendGenericFailure(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendReply(messenger, SERVICE_MSG_GENERIC_FAILURE, requestId, 0, null, null);
        }
    }

    private static void sendGenericSuccess(Messenger messenger, int requestId) {
        if (requestId != 0) {
            sendReply(messenger, SERVICE_MSG_GENERIC_SUCCESS, requestId, 0, null, null);
        }
    }

    private static void sendReply(Messenger messenger, int what,
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

    private static String getClientId(Messenger messenger) {
        return "Client connection " + messenger.getBinder().toString();
    }

    private final class PrivateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PRIVATE_MSG_CLIENT_DIED:
                    onBinderDied((Messenger)msg.obj);
                    break;
            }
        }
    }

    private final class ProviderCallback extends MediaRouteProvider.Callback {
        @Override
        public void onDescriptorChanged(MediaRouteProvider provider,
                MediaRouteProviderDescriptor descriptor) {
            sendDescriptorChanged(descriptor);
        }
    }

    private final class ClientRecord implements DeathRecipient {
        public final Messenger mMessenger;
        public final int mVersion;
        public MediaRouteDiscoveryRequest mDiscoveryRequest;

        private final SparseArray<MediaRouteProvider.RouteController> mControllers =
                new SparseArray<MediaRouteProvider.RouteController>();

        public ClientRecord(Messenger messenger, int version) {
            mMessenger = messenger;
            mVersion = version;
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

        public boolean createRouteController(String routeId, int controllerId) {
            if (mControllers.indexOfKey(controllerId) < 0) {
                MediaRouteProvider.RouteController controller =
                        mProvider.onCreateRouteController(routeId);
                if (controller != null) {
                    mControllers.put(controllerId, controller);
                    return true;
                }
            }
            return false;
        }

        public boolean releaseRouteController(int controllerId) {
            MediaRouteProvider.RouteController controller = mControllers.get(controllerId);
            if (controller != null) {
                mControllers.remove(controllerId);
                controller.onRelease();
                return true;
            }
            return false;
        }

        public MediaRouteProvider.RouteController getRouteController(int controllerId) {
            return mControllers.get(controllerId);
        }

        public boolean setDiscoveryRequest(MediaRouteDiscoveryRequest request) {
            if (mDiscoveryRequest != request
                    && (mDiscoveryRequest == null || !mDiscoveryRequest.equals(request))) {
                mDiscoveryRequest = request;
                return updateCompositeDiscoveryRequest();
            }
            return false;
        }

        // Runs on a binder thread.
        @Override
        public void binderDied() {
            mPrivateHandler.obtainMessage(PRIVATE_MSG_CLIENT_DIED, mMessenger).sendToTarget();
        }

        @Override
        public String toString() {
            return getClientId(mMessenger);
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
                if (!processMessage(what, messenger, requestId, arg, obj, data)) {
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

        private boolean processMessage(int what,
                Messenger messenger, int requestId, int arg, Object obj, Bundle data) {
            MediaRouteProviderService service = mServiceRef.get();
            if (service != null) {
                switch (what) {
                    case CLIENT_MSG_REGISTER:
                        return service.onRegisterClient(messenger, requestId, arg);

                    case CLIENT_MSG_UNREGISTER:
                        return service.onUnregisterClient(messenger, requestId);

                    case CLIENT_MSG_CREATE_ROUTE_CONTROLLER: {
                        String routeId = data.getString(CLIENT_DATA_ROUTE_ID);
                        if (routeId != null) {
                            return service.onCreateRouteController(
                                    messenger, requestId, arg, routeId);
                        }
                        break;
                    }

                    case CLIENT_MSG_RELEASE_ROUTE_CONTROLLER:
                        return service.onReleaseRouteController(messenger, requestId, arg);

                    case CLIENT_MSG_SELECT_ROUTE:
                        return service.onSelectRoute(messenger, requestId, arg);

                    case CLIENT_MSG_UNSELECT_ROUTE:
                        int reason = data == null ?
                                MediaRouter.UNSELECT_REASON_UNKNOWN
                                : data.getInt(CLIENT_DATA_UNSELECT_REASON,
                                        MediaRouter.UNSELECT_REASON_UNKNOWN);
                        return service.onUnselectRoute(messenger, requestId, arg, reason);

                    case CLIENT_MSG_SET_ROUTE_VOLUME: {
                        int volume = data.getInt(CLIENT_DATA_VOLUME, -1);
                        if (volume >= 0) {
                            return service.onSetRouteVolume(
                                    messenger, requestId, arg, volume);
                        }
                        break;
                    }

                    case CLIENT_MSG_UPDATE_ROUTE_VOLUME: {
                        int delta = data.getInt(CLIENT_DATA_VOLUME, 0);
                        if (delta != 0) {
                            return service.onUpdateRouteVolume(
                                    messenger, requestId, arg, delta);
                        }
                        break;
                    }

                    case CLIENT_MSG_ROUTE_CONTROL_REQUEST:
                        if (obj instanceof Intent) {
                            return service.onRouteControlRequest(
                                    messenger, requestId, arg, (Intent)obj);
                        }
                        break;

                    case CLIENT_MSG_SET_DISCOVERY_REQUEST: {
                        if (obj == null || obj instanceof Bundle) {
                            MediaRouteDiscoveryRequest request =
                                    MediaRouteDiscoveryRequest.fromBundle((Bundle)obj);
                            return service.onSetDiscoveryRequest(
                                    messenger, requestId,
                                    request != null && request.isValid() ? request : null);
                        }
                    }
                }
            }
            return false;
        }
    }
}
