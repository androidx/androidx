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

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_ID;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_LIBRARY_GROUP;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_UNSELECT_REASON;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_DATA_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .CLIENT_MSG_CREATE_ROUTE_CONTROLLER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_REGISTER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .CLIENT_MSG_RELEASE_ROUTE_CONTROLLER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .CLIENT_MSG_ROUTE_CONTROL_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SELECT_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .CLIENT_MSG_SET_DISCOVERY_REQUEST;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_SET_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UNREGISTER;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UNSELECT_ROUTE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_MSG_UPDATE_ROUTE_VOLUME;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_VERSION_CURRENT;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_DATA_ERROR;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .SERVICE_MSG_CONTROL_REQUEST_FAILED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol
        .SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DESCRIPTOR_CHANGED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_GENERIC_FAILURE;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_GENERIC_SUCCESS;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_REGISTERED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_VERSION_1;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.isValidRemoteMessenger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a connection to a particular media route provider service.
 */
final class RegisteredMediaRouteProvider extends MediaRouteProvider
        implements ServiceConnection {
    static final String TAG = "MediaRouteProviderProxy";  // max. 23 chars
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final ComponentName mComponentName;
    final PrivateHandler mPrivateHandler;
    private final ArrayList<Controller> mControllers = new ArrayList<Controller>();

    private boolean mStarted;
    private boolean mBound;
    private Connection mActiveConnection;
    private boolean mConnectionReady;

    public RegisteredMediaRouteProvider(Context context, ComponentName componentName) {
        super(context, new ProviderMetadata(componentName));

        mComponentName = componentName;
        mPrivateHandler = new PrivateHandler();
    }

    @Override
    public RouteController onCreateRouteController(@NonNull String routeId) {
        if (routeId == null) {
            throw new IllegalArgumentException("routeId cannot be null");
        }
        return createRouteController(routeId, null);
    }

    @Override
    public RouteController onCreateRouteController(
            @NonNull String routeId, @NonNull String routeGroupId) {
        if (routeId == null) {
            throw new IllegalArgumentException("routeId cannot be null");
        }
        if (routeGroupId == null) {
            throw new IllegalArgumentException("routeGroupId cannot be null");
        }
        return createRouteController(routeId, routeGroupId);
    }

    @Override
    public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest request) {
        if (mConnectionReady) {
            mActiveConnection.setDiscoveryRequest(request);
        }
        updateBinding();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (DEBUG) {
            Log.d(TAG, this + ": Connected");
        }

        if (mBound) {
            disconnect();

            Messenger messenger = (service != null ? new Messenger(service) : null);
            if (isValidRemoteMessenger(messenger)) {
                Connection connection = new Connection(messenger);
                if (connection.register()) {
                    mActiveConnection = connection;
                } else {
                    if (DEBUG) {
                        Log.d(TAG, this + ": Registration failed");
                    }
                }
            } else {
                Log.e(TAG, this + ": Service returned invalid messenger binder");
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (DEBUG) {
            Log.d(TAG, this + ": Service disconnected");
        }
        disconnect();
    }

    @Override
    public String toString() {
        return "Service connection " + mComponentName.flattenToShortString();
    }

    public boolean hasComponentName(String packageName, String className) {
        return mComponentName.getPackageName().equals(packageName)
                && mComponentName.getClassName().equals(className);
    }

    public void start() {
        if (!mStarted) {
            if (DEBUG) {
                Log.d(TAG, this + ": Starting");
            }

            mStarted = true;
            updateBinding();
        }
    }

    public void stop() {
        if (mStarted) {
            if (DEBUG) {
                Log.d(TAG, this + ": Stopping");
            }

            mStarted = false;
            updateBinding();
        }
    }

    public void rebindIfDisconnected() {
        if (mActiveConnection == null && shouldBind()) {
            unbind();
            bind();
        }
    }

    private void updateBinding() {
        if (shouldBind()) {
            bind();
        } else {
            unbind();
        }
    }

    private boolean shouldBind() {
        if (mStarted) {
            // Bind whenever there is a discovery request.
            if (getDiscoveryRequest() != null) {
                return true;
            }

            // Bind whenever the application has an active route controller.
            // This means that one of this provider's routes is selected.
            if (!mControllers.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void bind() {
        if (!mBound) {
            if (DEBUG) {
                Log.d(TAG, this + ": Binding");
            }

            Intent service = new Intent(MediaRouteProviderProtocol.SERVICE_INTERFACE);
            service.setComponent(mComponentName);
            try {
                mBound = getContext().bindService(service, this, Context.BIND_AUTO_CREATE);
                if (!mBound && DEBUG) {
                    Log.d(TAG, this + ": Bind failed");
                }
            } catch (SecurityException ex) {
                if (DEBUG) {
                    Log.d(TAG, this + ": Bind failed", ex);
                }
            }
        }
    }

    private void unbind() {
        if (mBound) {
            if (DEBUG) {
                Log.d(TAG, this + ": Unbinding");
            }

            mBound = false;
            disconnect();
            getContext().unbindService(this);
        }
    }

    private RouteController createRouteController(String routeId, String routeGroupId) {
        MediaRouteProviderDescriptor descriptor = getDescriptor();
        if (descriptor != null) {
            List<MediaRouteDescriptor> routes = descriptor.getRoutes();
            final int count = routes.size();
            for (int i = 0; i < count; i++) {
                final MediaRouteDescriptor route = routes.get(i);
                if (route.getId().equals(routeId)) {
                    Controller controller = new Controller(routeId, routeGroupId);
                    mControllers.add(controller);
                    if (mConnectionReady) {
                        controller.attachConnection(mActiveConnection);
                    }
                    updateBinding();
                    return controller;
                }
            }
        }
        return null;
    }

    void onConnectionReady(Connection connection) {
        if (mActiveConnection == connection) {
            mConnectionReady = true;
            attachControllersToConnection();

            MediaRouteDiscoveryRequest request = getDiscoveryRequest();
            if (request != null) {
                mActiveConnection.setDiscoveryRequest(request);
            }
        }
    }

    void onConnectionDied(Connection connection) {
        if (mActiveConnection == connection) {
            if (DEBUG) {
                Log.d(TAG, this + ": Service connection died");
            }
            disconnect();
        }
    }

    void onConnectionError(Connection connection, String error) {
        if (mActiveConnection == connection) {
            if (DEBUG) {
                Log.d(TAG, this + ": Service connection error - " + error);
            }
            unbind();
        }
    }

    void onConnectionDescriptorChanged(Connection connection,
            MediaRouteProviderDescriptor descriptor) {
        if (mActiveConnection == connection) {
            if (DEBUG) {
                Log.d(TAG, this + ": Descriptor changed, descriptor=" + descriptor);
            }
            setDescriptor(descriptor);
        }
    }

    private void disconnect() {
        if (mActiveConnection != null) {
            setDescriptor(null);
            mConnectionReady = false;
            detachControllersFromConnection();
            mActiveConnection.dispose();
            mActiveConnection = null;
        }
    }

    void onControllerReleased(Controller controller) {
        mControllers.remove(controller);
        controller.detachConnection();
        updateBinding();
    }

    private void attachControllersToConnection() {
        int count = mControllers.size();
        for (int i = 0; i < count; i++) {
            mControllers.get(i).attachConnection(mActiveConnection);
        }
    }

    private void detachControllersFromConnection() {
        int count = mControllers.size();
        for (int i = 0; i < count; i++) {
            mControllers.get(i).detachConnection();
        }
    }

    private final class Controller extends RouteController {
        private final String mRouteId;
        private final String mRouteGroupId;

        private boolean mSelected;
        private int mPendingSetVolume = -1;
        private int mPendingUpdateVolumeDelta;

        private Connection mConnection;
        private int mControllerId;

        public Controller(String routeId, String routeGroupId) {
            mRouteId = routeId;
            mRouteGroupId = routeGroupId;
        }

        public void attachConnection(Connection connection) {
            mConnection = connection;
            mControllerId = connection.createRouteController(mRouteId, mRouteGroupId);
            if (mSelected) {
                connection.selectRoute(mControllerId);
                if (mPendingSetVolume >= 0) {
                    connection.setVolume(mControllerId, mPendingSetVolume);
                    mPendingSetVolume = -1;
                }
                if (mPendingUpdateVolumeDelta != 0) {
                    connection.updateVolume(mControllerId, mPendingUpdateVolumeDelta);
                    mPendingUpdateVolumeDelta = 0;
                }
            }
        }

        public void detachConnection() {
            if (mConnection != null) {
                mConnection.releaseRouteController(mControllerId);
                mConnection = null;
                mControllerId = 0;
            }
        }

        @Override
        public void onRelease() {
            onControllerReleased(this);
        }

        @Override
        public void onSelect() {
            mSelected = true;
            if (mConnection != null) {
                mConnection.selectRoute(mControllerId);
            }
        }

        @Override
        public void onUnselect() {
            onUnselect(MediaRouter.UNSELECT_REASON_UNKNOWN);
        }

        @Override
        public void onUnselect(int reason) {
            mSelected = false;
            if (mConnection != null) {
                mConnection.unselectRoute(mControllerId, reason);
            }
        }

        @Override
        public void onSetVolume(int volume) {
            if (mConnection != null) {
                mConnection.setVolume(mControllerId, volume);
            } else {
                mPendingSetVolume = volume;
                mPendingUpdateVolumeDelta = 0;
            }
        }

        @Override
        public void onUpdateVolume(int delta) {
            if (mConnection != null) {
                mConnection.updateVolume(mControllerId, delta);
            } else {
                mPendingUpdateVolumeDelta += delta;
            }
        }

        @Override
        public boolean onControlRequest(Intent intent, ControlRequestCallback callback) {
            if (mConnection != null) {
                return mConnection.sendControlRequest(mControllerId, intent, callback);
            }
            return false;
        }
    }

    private final class Connection implements DeathRecipient {
        private final Messenger mServiceMessenger;
        private final ReceiveHandler mReceiveHandler;
        private final Messenger mReceiveMessenger;

        private int mNextRequestId = 1;
        private int mNextControllerId = 1;
        private int mServiceVersion; // non-zero when registration complete

        private int mPendingRegisterRequestId;
        private final SparseArray<ControlRequestCallback> mPendingCallbacks =
                new SparseArray<ControlRequestCallback>();

        public Connection(Messenger serviceMessenger) {
            mServiceMessenger = serviceMessenger;
            mReceiveHandler = new ReceiveHandler(this);
            mReceiveMessenger = new Messenger(mReceiveHandler);
        }

        public boolean register() {
            mPendingRegisterRequestId = mNextRequestId++;
            if (!sendRequest(CLIENT_MSG_REGISTER,
                    mPendingRegisterRequestId,
                    CLIENT_VERSION_CURRENT, null, null)) {
                return false;
            }

            try {
                mServiceMessenger.getBinder().linkToDeath(this, 0);
                return true;
            } catch (RemoteException ex) {
                binderDied();
            }
            return false;
        }

        public void dispose() {
            sendRequest(CLIENT_MSG_UNREGISTER, 0, 0, null, null);
            mReceiveHandler.dispose();
            mServiceMessenger.getBinder().unlinkToDeath(this, 0);

            mPrivateHandler.post(new Runnable() {
                @Override
                public void run() {
                    failPendingCallbacks();
                }
            });
        }

        void failPendingCallbacks() {
            int count = 0;
            for (int i = 0; i < mPendingCallbacks.size(); i++) {
                mPendingCallbacks.valueAt(i).onError(null, null);
            }
            mPendingCallbacks.clear();
        }

        public boolean onGenericFailure(int requestId) {
            if (requestId == mPendingRegisterRequestId) {
                mPendingRegisterRequestId = 0;
                onConnectionError(this, "Registration failed");
            }
            ControlRequestCallback callback = mPendingCallbacks.get(requestId);
            if (callback != null) {
                mPendingCallbacks.remove(requestId);
                callback.onError(null, null);
            }
            return true;
        }

        public boolean onGenericSuccess(int requestId) {
            return true;
        }

        public boolean onRegistered(int requestId, int serviceVersion,
                Bundle descriptorBundle) {
            if (mServiceVersion == 0
                    && requestId == mPendingRegisterRequestId
                    && serviceVersion >= SERVICE_VERSION_1) {
                mPendingRegisterRequestId = 0;
                mServiceVersion = serviceVersion;
                onConnectionDescriptorChanged(this,
                        MediaRouteProviderDescriptor.fromBundle(descriptorBundle));
                onConnectionReady(this);
                return true;
            }
            return false;
        }

        public boolean onDescriptorChanged(Bundle descriptorBundle) {
            if (mServiceVersion != 0) {
                onConnectionDescriptorChanged(this,
                        MediaRouteProviderDescriptor.fromBundle(descriptorBundle));
                return true;
            }
            return false;
        }

        public boolean onControlRequestSucceeded(int requestId, Bundle data) {
            ControlRequestCallback callback = mPendingCallbacks.get(requestId);
            if (callback != null) {
                mPendingCallbacks.remove(requestId);
                callback.onResult(data);
                return true;
            }
            return false;
        }

        public boolean onControlRequestFailed(int requestId, String error, Bundle data) {
            ControlRequestCallback callback = mPendingCallbacks.get(requestId);
            if (callback != null) {
                mPendingCallbacks.remove(requestId);
                callback.onError(error, data);
                return true;
            }
            return false;
        }

        @Override
        public void binderDied() {
            mPrivateHandler.post(new Runnable() {
                @Override
                public void run() {
                    onConnectionDied(Connection.this);
                }
            });
        }

        public int createRouteController(String routeId, String routeGroupId) {
            int controllerId = mNextControllerId++;
            Bundle data = new Bundle();
            data.putString(CLIENT_DATA_ROUTE_ID, routeId);
            data.putString(CLIENT_DATA_ROUTE_LIBRARY_GROUP, routeGroupId);
            sendRequest(CLIENT_MSG_CREATE_ROUTE_CONTROLLER,
                    mNextRequestId++, controllerId, null, data);
            return controllerId;
        }

        public void releaseRouteController(int controllerId) {
            sendRequest(CLIENT_MSG_RELEASE_ROUTE_CONTROLLER,
                    mNextRequestId++, controllerId, null, null);
        }

        public void selectRoute(int controllerId) {
            sendRequest(CLIENT_MSG_SELECT_ROUTE,
                    mNextRequestId++, controllerId, null, null);
        }

        public void unselectRoute(int controllerId, int reason) {
            Bundle extras = new Bundle();
            extras.putInt(CLIENT_DATA_UNSELECT_REASON, reason);
            sendRequest(CLIENT_MSG_UNSELECT_ROUTE,
                    mNextRequestId++, controllerId, null, extras);
        }

        public void setVolume(int controllerId, int volume) {
            Bundle data = new Bundle();
            data.putInt(CLIENT_DATA_VOLUME, volume);
            sendRequest(CLIENT_MSG_SET_ROUTE_VOLUME,
                    mNextRequestId++, controllerId, null, data);
        }

        public void updateVolume(int controllerId, int delta) {
            Bundle data = new Bundle();
            data.putInt(CLIENT_DATA_VOLUME, delta);
            sendRequest(CLIENT_MSG_UPDATE_ROUTE_VOLUME,
                    mNextRequestId++, controllerId, null, data);
        }

        public boolean sendControlRequest(int controllerId, Intent intent,
                ControlRequestCallback callback) {
            int requestId = mNextRequestId++;
            if (sendRequest(CLIENT_MSG_ROUTE_CONTROL_REQUEST,
                    requestId, controllerId, intent, null)) {
                if (callback != null) {
                    mPendingCallbacks.put(requestId, callback);
                }
                return true;
            }
            return false;
        }

        public void setDiscoveryRequest(MediaRouteDiscoveryRequest request) {
            sendRequest(CLIENT_MSG_SET_DISCOVERY_REQUEST,
                    mNextRequestId++, 0, request != null ? request.asBundle() : null, null);
        }

        private boolean sendRequest(int what, int requestId, int arg, Object obj, Bundle data) {
            Message msg = Message.obtain();
            msg.what = what;
            msg.arg1 = requestId;
            msg.arg2 = arg;
            msg.obj = obj;
            msg.setData(data);
            msg.replyTo = mReceiveMessenger;
            try {
                mServiceMessenger.send(msg);
                return true;
            } catch (DeadObjectException ex) {
                // The service died.
            } catch (RemoteException ex) {
                if (what != CLIENT_MSG_UNREGISTER) {
                    Log.e(TAG, "Could not send message to service.", ex);
                }
            }
            return false;
        }
    }

    private static final class PrivateHandler extends Handler {
        PrivateHandler() {
        }
    }

    /**
     * Handler that receives messages from the server.
     * <p>
     * This inner class is static and only retains a weak reference to the connection
     * to prevent the client from being leaked in case the service is holding an
     * active reference to the client's messenger.
     * </p><p>
     * This handler should not be used to handle any messages other than those
     * that come from the service.
     * </p>
     */
    private static final class ReceiveHandler extends Handler {
        private final WeakReference<Connection> mConnectionRef;

        public ReceiveHandler(Connection connection) {
            mConnectionRef = new WeakReference<Connection>(connection);
        }

        public void dispose() {
            mConnectionRef.clear();
        }

        @Override
        public void handleMessage(Message msg) {
            Connection connection = mConnectionRef.get();
            if (connection != null) {
                final int what = msg.what;
                final int requestId = msg.arg1;
                final int arg = msg.arg2;
                final Object obj = msg.obj;
                final Bundle data = msg.peekData();
                if (!processMessage(connection, what, requestId, arg, obj, data)) {
                    if (DEBUG) {
                        Log.d(TAG, "Unhandled message from server: " + msg);
                    }
                }
            }
        }

        private boolean processMessage(Connection connection,
                int what, int requestId, int arg, Object obj, Bundle data) {
            switch (what) {
                case SERVICE_MSG_GENERIC_FAILURE:
                    connection.onGenericFailure(requestId);
                    return true;

                case SERVICE_MSG_GENERIC_SUCCESS:
                    connection.onGenericSuccess(requestId);
                    return true;

                case SERVICE_MSG_REGISTERED:
                    if (obj == null || obj instanceof Bundle) {
                        return connection.onRegistered(requestId, arg, (Bundle)obj);
                    }
                    break;

                case SERVICE_MSG_DESCRIPTOR_CHANGED:
                    if (obj == null || obj instanceof Bundle) {
                        return connection.onDescriptorChanged((Bundle)obj);
                    }
                    break;

                case SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED:
                    if (obj == null || obj instanceof Bundle) {
                        return connection.onControlRequestSucceeded(
                                requestId, (Bundle)obj);
                    }
                    break;

                case SERVICE_MSG_CONTROL_REQUEST_FAILED:
                    if (obj == null || obj instanceof Bundle) {
                        String error = (data == null ? null :
                                data.getString(SERVICE_DATA_ERROR));
                        return connection.onControlRequestFailed(
                                requestId, error, (Bundle)obj);
                    }
                    break;
            }
            return false;
        }
    }
}
