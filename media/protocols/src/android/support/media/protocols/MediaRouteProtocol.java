/*
 * Copyright (C) 2014 The Android Open Source Project
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
package android.support.media.protocols;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.lang.ref.WeakReference;

/**
 * Base class for media route protocols.
 * <p>
 * A media route protocol expresses an interface contract between an application and
 * a media route that it would like to communicate with and control.  By using
 * a protocol to send messages to a media route, an application can
 * ask the media route to perform functions such as creating a playlist of music
 * to be played on behalf of the application.
 * </p><p>
 * Subclasses should extend this class to offer specialized protocols.
 * </p><p>
 * Instances of this class are thread-safe but event will only be received
 * on the handler that was specified when the callback was registered.
 * </p>
 *
 * <h3>Overview</h3>
 * <p>
 * A media route protocol is essentially just a binder-based messaging interface.
 * Messages sent from the application to the media route service are called "requests"
 * whereas messages sent from the media route service back to the application are
 * called "events" or "errors" depending on their purpose.
 * </p><p>
 * All communication through a protocol is asynchronous and is dispatched to a
 * a {@link android.os.Looper} of the application or the media route service's choice
 * (separate for each end).  Arguments are transferred through key/value pairs in
 * {@link Bundle bundles}.
 * </p><p>
 * The overall interface is somewhat simpler than directly using AIDL and Binder which
 * requires additional care to extend and maintain binary compatibility and to
 * perform thread synchronization on either end of the communication channel.
 * Media route protocols also support bidirectional asynchronous communication
 * requests, events, and errors between the application and the media route service.
 * </p>
 *
 * <h3>Using Protocols</h3>
 * <p>
 * To use a protocol, an application must do the following.
 * </p><ul>
 * <li>Create a {@link android.media.routing.MediaRouter media router}.
 * <li>Add a {@link android.media.routing.MediaRouteSelector media route selector}
 * that specifies the protocol as required or optional.
 * <li>Show a media route button in the application's action bar to enable the
 * user to choose a destination to connect to.
 * <li>Once the connection has been established, obtain the protocol's
 * binder from the {@link android.media.routing.MediaRouter.ConnectionInfo route connection}
 * information and {@link MediaRouteProtocol#MediaRouteProtocol(IBinder) create} the protocol
 * object.  There is also a convenience method called
 * {@link android.media.routing.MediaRouter.ConnectionInfo#getProtocolObject getProtocolObject}
 * to do this all in one step.
 * <li>Set a {@link Callback} on the protocol object to receive events.
 * <li>At this point, the application can begin sending requests to the media route
 * and receiving events in response via the protocol object.
 * </ul>
 *
 * <h3>Providing Protocols</h3>
 * <p>
 * The provide a protocol, a media route service must do the following.
 * </p><ul>
 * <li>Upon receiving a
 * {@link android.media.routing.MediaRouter.DiscoveryRequest discovery request}
 * from an application that contains a
 * {@link android.media.routing.MediaRouteSelector media route selector}
 * which asks to find routes that support known protocols during discovery, the media
 * route service should indicate that it supports this protocol by adding it to the list
 * of supported protocols in the
 * {@link android.media.routing.MediaRouter.RouteInfo route information} for those
 * routes that support them.
 * <li>Upon receiving a
 * {@link android.media.routing.MediaRouter.ConnectionRequest connection request}
 * from an application that requests to connect to a route for which the application
 * previously requested support of known protocols, the media route service should
 * {@link MediaRouteProtocol.Stub#MediaRouteProtocol.Stub(Handler) create} a subclass of the stub
 * object that implements the protocol then add it to the list of protocol binders
 * in the {@link android.media.routing.MediaRouter.ConnectionInfo route connection}
 * object it returns to the application.
 * <li>Once the route is connected, the media route service should handle incoming
 * protocol requests from the client and respond accordingly.
 * <li>Once the route is disconnected, the media route service should cease to
 * handle incoming protocol requests from the client and should clean up its state
 * accordingly.
 * </ul>
 *
 * <h3>Creating Custom Protocols</h3>
 * <p>
 * Although the framework provides standard media route protocols to encourage
 * interoperability, it may be useful to create and publish custom protocols to
 * access extended functionality only supported by certain routes.
 * </p><p>
 * To create a custom protocol, create a subclass of the {@link MediaRouteProtocol}
 * class to declare the new request methods and marshal their arguments.  Also create
 * a subclass of the {@link MediaRouteProtocol.Callback} class to decode any new kinds
 * of events and subclass the {@link MediaRouteProtocol.Stub} class to decode
 * incoming requests.
 * </p><p>
 * It may help to refer to the source code of the <code>android.support.media.protocol.jar</code>
 * library for details.
 * </p><p>
 * Here is a simple example:
 * </p><pre>
 * public abstract class CustomProtocol extends MediaRouteProtocol {
 *     public CustomProtocol(IBinder binder) {
 *         super(binder);
 *     }
 *
 *     // declare custom request
 *     public void tuneRadio(int station) {
 *         Bundle args = new Bundle();
 *         args.putInt("station", station);
 *         sendRequest("tuneRadio", args);
 *     }
 *
 *     public static abstract class Callback extends MediaRouteProtocol.Callback {
 *         // declare custom event
 *         public void onStationTuned(int station, boolean hifi) { }
 *
 *         &#064;Override
 *         public void onEvent(String event, Bundle args) {
 *             switch (event) {
 *                 case "radioTuned":
 *                     onRadioTuned(args.getInt("station"), args.getBoolean("hifi"));
 *                     return;
 *             }
 *             super.onEvent(event, args);
 *         }
 *     }
 *
 *     public static abstract class Stub extends MediaRouteProtocol.Stub {
 *         // declare custom request stub
 *         public abstract void onTuneRadio(int station);
 *
 *         &#064;Override
 *         public void onRequest(String request, Bundle args) {
 *             switch (request) {
 *                 case "tuneRadio":
 *                     onTuneRadio(args.getInt("station"));
 *                     return;
 *             }
 *             super.onRequest(request, args);
 *         }
 *     }
 * }
 * </pre>
 */
public abstract class MediaRouteProtocol {
    private static final String TAG = "MediaRouteProtocol";

    private static final int REQUEST_MSG_SUBSCRIBE = 1;
    private static final int REQUEST_MSG_COMMAND = 2;

    private static final int REPLY_MSG_ERROR = 1;
    private static final int REPLY_MSG_EVENT = 2;

    private final Object mLock = new Object();
    private final Messenger mRequestMessenger;
    private Messenger mReplyMessenger;
    private volatile Callback mCallback;
    private Looper mCallbackLooper;
    private Handler mCallbackHandler;

    /**
     * Error code: Some other unknown error occurred.
     */
    public static final String ERROR_UNKNOWN =
            "android.support.errors.UNKNOWN";

    /**
     * Error code: The media route has been disconnected.
     */
    public static final String ERROR_DISCONNECTED =
            "android.support.errors.DISCONNECTED";

    /**
     * Error code: The application issued an unsupported request.
     */
    public static final String ERROR_UNSUPPORTED_OPERATION =
            "android.support.errors.UNSUPPORTED_OPERATION";

    /**
     * Creates the protocol client object for an application to use to send
     * messages to a media route.
     * <p>
     * This constructor is called automatically if you use
     * {@link android.media.routing.MediaRouter.ConnectionInfo#getProtocolObject getProtocolObject}
     * to obtain a protocol object from a media route connection.
     * </p>
     *
     * @param binder The remote binder supplied by the media route service.  May be
     * obtained using {@link android.media.routing.MediaRouter.ConnectionInfo#getProtocolBinder}
     * on a route connection.
     */
    public MediaRouteProtocol(@NonNull IBinder binder) {
        if (binder == null) {
            throw new IllegalArgumentException("binder must not be null");
        }

        mRequestMessenger = new Messenger(binder);
    }

    /**
     * Sets the callback interface and handler on which to receive events and errors.
     *
     * @param callback The callback interface, or null if none.
     * @param handler The handler on which to receive events and errors, or null to use
     * the current looper thread.
     */
    public void setCallback(@Nullable Callback callback, @Nullable Handler handler) {
        synchronized (mLock) {
            Looper looper = callback != null ?
                    (handler != null ? handler.getLooper() : Looper.myLooper()) : null;
            if (mCallback != callback || mCallbackLooper != looper) {
                mCallback = callback;
                if (mCallback != null) {
                    mCallbackLooper = looper;
                    mCallbackHandler = handler != null ? handler : new Handler();
                    mReplyMessenger = new Messenger(new ReplyHandler(this, looper));
                } else {
                    mCallbackLooper = null;
                    mCallbackHandler = null;
                    mReplyMessenger = null;
                }

                Message msg = Message.obtain();
                msg.what = REQUEST_MSG_SUBSCRIBE;
                msg.replyTo = mReplyMessenger;
                sendSafelyLocked(msg);
            }
        }
    }

    /**
     * Sends an asynchronous request to the media route service.
     * <p>
     * If an error occurs, it will be reported to the callback's {@link Callback#onError}
     * method.
     * </p>
     *
     * @param request The request name.
     * @param args The request arguments, or null if none.
     */
    public void sendRequest(@NonNull String request, @Nullable Bundle args) {
        if (TextUtils.isEmpty(request)) {
            throw new IllegalArgumentException("request must not be null or empty");
        }

        synchronized (mLock) {
            Message msg = Message.obtain();
            msg.what = REQUEST_MSG_COMMAND;
            msg.obj = request;
            msg.setData(args);
            sendSafelyLocked(msg);
        }
    }

    private void sendSafelyLocked(Message msg) {
        if (mRequestMessenger != null) {
            try {
                mRequestMessenger.send(msg);
            } catch (RemoteException ex) {
                postErrorLocked(ERROR_DISCONNECTED, null);
            }
        } else {
            postErrorLocked(ERROR_DISCONNECTED, null);
        }
    }

    private void postErrorLocked(final String error, final Bundle args) {
        final Callback callback = mCallback;
        if (callback != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onError(error, args);
                    }
                }
            });
        }
    }

    private void handleReply(Message msg) {
        Callback callback = mCallback; // note: racy
        if (callback != null) {
            // ignore unrecognized messages in case of future protocol extension
            if (msg.what == REPLY_MSG_ERROR && msg.obj instanceof String) {
                mCallback.onError((String)msg.obj, msg.peekData());
            } else if (msg.what == REPLY_MSG_EVENT && msg.obj instanceof String) {
                mCallback.onEvent((String)msg.obj, msg.peekData());
            }
        }
    }

    /*
     * Only use this handler to handle replies coming back from the media route service
     * because the service can send any message it wants to it.
     * Validate arguments carefully.
     */
    private static final class ReplyHandler extends Handler {
        // break hard reference cycles through binder
        private final WeakReference<MediaRouteProtocol> mProtocolRef;

        public ReplyHandler(MediaRouteProtocol protocol, Looper looper) {
            super(looper);
            mProtocolRef = new WeakReference<MediaRouteProtocol>(protocol);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaRouteProtocol protocol = mProtocolRef.get();
            if (protocol != null) {
                protocol.handleReply(msg);
            }
        }
    }

    /**
     * Base class for application callbacks from the media route service.
     * <p>
     * Subclasses should extend this class to offer events for specialized protocols.
     * </p>
     */
    public static abstract class Callback {
        /**
         * Called when an event is received from the media route service.
         *
         * @param event The event name.
         * @param args The event arguments, or null if none.
         */
        public void onEvent(@NonNull String event, @Nullable Bundle args) { }

        /**
         * Called when an error occurs in the media route service.
         *
         * @param error The error name.
         * @param args The error arguments, or null if none.
         */
        public void onError(@NonNull String error, @Nullable Bundle args) { }
    }

    /**
     * Base class for a media route protocol stub implemented by a media route service.
     * <p>
     * Subclasses should extend this class to offer implementation for specialized
     * protocols.
     * </p><p>
     * Instances of this class are thread-safe but requests will only be received
     * on the handler that was specified in the constructor.
     * </p>
     */
    public static abstract class Stub implements IInterface, Closeable {
        private final Object mLock = new Object();

        private final Messenger mRequestMessenger;
        private Messenger mReplyMessenger;
        private volatile boolean mClosed;

        /**
         * Creates an implementation of a media route protocol.
         *
         * @param handler The handler on which to receive requests, or null to use
         * the current looper thread.
         */
        public Stub(@Nullable Handler handler) {
            mRequestMessenger = new Messenger(new RequestHandler(this,
                    handler != null ? handler.getLooper() : Looper.myLooper()));
        }

        /**
         * Gets the underlying binder object for the stub.
         */
        @Override
        public @NonNull IBinder asBinder() {
            return mRequestMessenger.getBinder();
        }

        /**
         * Closes the stub and prevents it from receiving any additional
         * messages from the application.
         */
        @Override
        public void close() {
            synchronized (mLock) {
                mClosed = true;
                mReplyMessenger = null;
            }
        }

        /**
         * Called when the application sends a request to the media route service
         * through this protocol.
         * <p>
         * The default implementation throws {@link UnsupportedOperationException}
         * which is reported back to the application's error callback as
         * {@link #ERROR_UNSUPPORTED_OPERATION}.
         * </p>
         *
         * @param request The request name.
         * @param args The request arguments, or null if none.
         */
        public void onRequest(@NonNull String request, @Nullable Bundle args)
                throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Called when the application attaches a callback to receive events and errors.
         */
        public void onClientAttached() {
        }

        /**
         * Called when the application removes its callback and can no longer receive
         * events and errors.
         */
        public void onClientDetached() {
        }

        /**
         * Sends an error to the application.
         *
         * @param error The error name.
         * @param args The error arguments, or null if none.
         * @return True if the message was sent, or false if the client is not
         * attached, cannot be reached, or if the stub has been closed.
         */
        public boolean sendError(@NonNull String error, @Nullable Bundle args) {
            if (TextUtils.isEmpty(error)) {
                throw new IllegalArgumentException("error must not be null or empty");
            }

            synchronized (mLock) {
                Message msg = Message.obtain();
                msg.what = REPLY_MSG_ERROR;
                msg.obj = error;
                msg.setData(args);
                return replySafelyLocked(msg);
            }
        }

        /**
         * Sends an event to the application.
         *
         * @param event The event name.
         * @param args The event arguments, or null if none.
         * @return True if the message was sent, or false if the client is not
         * attached, cannot be reached, or if the stub has been closed.
         */
        public boolean sendEvent(@NonNull String event, @Nullable Bundle args) {
            if (TextUtils.isEmpty(event)) {
                throw new IllegalArgumentException("event must not be null or empty");
            }

            synchronized (mLock) {
                Message msg = Message.obtain();
                msg.what = REPLY_MSG_EVENT;
                msg.obj = event;
                msg.setData(args);
                return replySafelyLocked(msg);
            }
        }

        private boolean replySafelyLocked(Message msg) {
            if (mClosed) {
                Log.w(TAG, "Could not send reply message because the stub has been closed: "
                        + msg + ", in: " + getClass().getName());
                return false;
            }
            if (mReplyMessenger == null) {
                Log.w(TAG, "Could not send reply message because the client has not yet "
                        + "attached a callback: " + msg + ", in: " + getClass().getName());
                return false;
            }

            try {
                mReplyMessenger.send(msg);
                return true;
            } catch (RemoteException ex) {
                Log.w(TAG, "Could not send reply message because the client died: "
                        + msg + ", in: " + getClass().getName());
                return false;
            }
        }

        private void handleRequest(Message msg) {
            if (mClosed) { // note: racy
                Log.w(TAG, "Dropping request because the media route service has "
                        + "closed its end of the protocol: " + msg + ", in: " + getClass());
                return;
            }

            // ignore unrecognized messages in case of future protocol extension
            if (msg.what == REQUEST_MSG_COMMAND && msg.obj instanceof String) {
                String command = (String)msg.obj;
                try {
                    onRequest(command, msg.peekData());
                } catch (UnsupportedOperationException ex) {
                    Log.w(TAG, "Client sent unsupported command request: "
                            + msg + ", in: " + getClass());
                    sendError(ERROR_UNSUPPORTED_OPERATION, null);
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Stub threw runtime exception while processing command "
                            + "request: " + msg + ", in: " + getClass());
                    sendError(ERROR_UNKNOWN, null);
                }
            } else if (msg.what == REQUEST_MSG_SUBSCRIBE) {
                synchronized (mLock) {
                    if (mClosed) {
                        return; // fix possible race if close() is called on another thread
                    }
                    mReplyMessenger = msg.replyTo;
                }
                if (msg.replyTo != null) {
                    onClientAttached();
                } else {
                    onClientDetached();
                }
            }
        }

        /*
         * Use this handler only to handle requests coming from the application
         * because the application can send any message it wants to it.
         */
        private static final class RequestHandler extends Handler {
            // break hard reference cycles through binder
            private final WeakReference<Stub> mStubRef;

            public RequestHandler(Stub stub, Looper looper) {
                super(looper);
                mStubRef = new WeakReference<Stub>(stub);
            }

            @Override
            public void handleMessage(Message msg) {
                Stub stub = mStubRef.get();
                if (stub != null) {
                    stub.handleRequest(msg);
                }
            }
        }
    }
}