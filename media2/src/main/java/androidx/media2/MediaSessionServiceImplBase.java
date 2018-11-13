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

package androidx.media2;

import static android.app.Service.START_STICKY;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.GuardedBy;
import androidx.collection.ArrayMap;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaSessionService.MediaNotification;
import androidx.media2.MediaSessionService.MediaSessionServiceImpl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link MediaSessionService}.
 */
class MediaSessionServiceImplBase implements MediaSessionServiceImpl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    MediaSessionServiceStub mStub;
    @GuardedBy("mLock")
    MediaSessionService mInstance;
    @GuardedBy("mLock")
    private Map<String, MediaSession> mSessions = new ArrayMap<>();
    @GuardedBy("mLock")
    private MediaNotificationHandler mNotificationHandler;

    MediaSessionServiceImplBase() {
    }

    @Override
    public void onCreate(MediaSessionService service) {
        synchronized (mLock) {
            mInstance = service;
            mStub = new MediaSessionServiceStub(this);
            mNotificationHandler = new MediaNotificationHandler(service);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        final MediaSessionService service = getInstance();
        if (service == null) {
            Log.w(TAG, "Service hasn't created before onBind()");
            return null;
        }
        switch (intent.getAction()) {
            case MediaSessionService.SERVICE_INTERFACE: {
                return getServiceBinder();
            }
            case MediaBrowserServiceCompat.SERVICE_INTERFACE: {
                final MediaSession session = service.onGetSession();
                addSession(session);
                // Return a specific session's legacy binder although the Android framework caches
                // the returned binder here and next binding request may reuse cached binder even
                // after the session is closed.
                // Disclaimer: Although MediaBrowserCompat can only get the session that initially
                // set, it doesn't make things bad. Such limitation had been there between
                // MediaBrowserCompat and MediaBrowserServiceCompat.
                return session.getLegacyBrowerServiceBinder();
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        synchronized (mLock) {
            mInstance = null;
            if (mStub != null) {
                mStub.close();
                mStub = null;
            }
        }
    }

    @Override
    public void addSession(final MediaSession session) {
        final MediaSession old;
        synchronized (mLock) {
            old = mSessions.get(session.getId());
            if (old != null && old != session) {
                // TODO(b/112114183): Also check the uniqueness before sessions're returned by
                //                    onGetSession.
                throw new IllegalArgumentException("Session ID should be unique.");
            }
            mSessions.put(session.getId(), session);
        }
        if (old == null) {
            // Session has returned for the first time. Register callbacks.
            // TODO: Check whether the session is registered in multiple sessions.
            final MediaNotificationHandler handler;
            synchronized (mLock) {
                handler = mNotificationHandler;
            }
            handler.onPlayerStateChanged(session, session.getPlayer().getPlayerState());
            session.getCallback().setForegroundServiceEventCallback(handler);
        }
    }

    @Override
    public void removeSession(MediaSession session) {
        synchronized (mLock) {
            mSessions.remove(session.getId());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        switch (intent.getAction()) {
            case Intent.ACTION_MEDIA_BUTTON: {
                final MediaSessionService instance = getInstance();
                if (instance == null) {
                    Log.wtf(TAG, "Service hasn't created");
                }
                final MediaSession session = instance.onGetSession();
                if (session == null) {
                    Log.w(TAG, "No session for handling media key");
                    break;
                }
                KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (keyEvent != null) {
                    session.getSessionCompat().getController().dispatchMediaButtonEvent(keyEvent);
                }
                break;
            }
        }
        return START_STICKY;
    }

    @Override
    public MediaNotification onUpdateNotification(MediaSession session) {
        final MediaNotificationHandler handler;
        synchronized (mLock) {
            handler = mNotificationHandler;
        }
        if (handler == null) {
            throw new IllegalStateException("Service hasn't created");
        }
        return handler.onUpdateNotification(session);
    }

    @Override
    public List<MediaSession> getSessions() {
        List<MediaSession> list = new ArrayList<>();
        synchronized (mLock) {
            list.addAll(mSessions.values());
        }
        return list;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaSessionService getInstance() {
        synchronized (mLock) {
            return mInstance;
        }
    }

    IBinder getServiceBinder() {
        synchronized (mLock) {
            return (mStub != null) ? mStub.asBinder() : null;
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    private static final class MediaSessionServiceStub extends IMediaSessionService.Stub
            implements AutoCloseable {
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final WeakReference<MediaSessionServiceImplBase> mServiceImpl;
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        final Handler mHandler;

        MediaSessionServiceStub(final MediaSessionServiceImplBase serviceImpl) {
            mServiceImpl = new WeakReference<>(serviceImpl);
            mHandler = new Handler(serviceImpl.getInstance().getMainLooper());
        }

        @Override
        public void connect(final IMediaController caller, final String packageName) {
            final MediaSessionServiceImplBase serviceImpl = mServiceImpl.get();
            if (serviceImpl == null) {
                if (DEBUG) {
                    Log.d(TAG, "ServiceImpl isn't available");
                }
                return;
            }
            final int pid = Binder.getCallingPid();
            final int uid = Binder.getCallingUid();
            final long token = Binder.clearCallingIdentity();
            try {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        boolean shouldNotifyDisconnected = true;
                        try {
                            final MediaSessionServiceImplBase serviceImpl = mServiceImpl.get();
                            if (serviceImpl == null) {
                                if (DEBUG) {
                                    Log.d(TAG, "ServiceImpl isn't available");
                                }
                                return;
                            }
                            final MediaSessionService service = serviceImpl.getInstance();
                            if (service == null) {
                                if (DEBUG) {
                                    Log.d(TAG, "Service isn't available");
                                }
                                return;
                            }
                            if (DEBUG) {
                                Log.d(TAG, "Handling incoming connection request from the"
                                        + " controller, controller=" + packageName);

                            }
                            final MediaSession session;
                            try {
                                session = service.onGetSession();
                                service.addSession(session);
                                shouldNotifyDisconnected = false;

                                session.handleControllerConnectionFromService(caller, packageName,
                                        pid, uid);
                            } catch (Exception e) {
                                // Don't propagate exception in service to the controller.
                                Log.w(TAG, "Failed to add a session to session service", e);
                            }
                        } finally {
                            // Trick to call onDisconnected() in one place.
                            if (shouldNotifyDisconnected) {
                                if (DEBUG) {
                                    Log.d(TAG, "Service has destroyed prematurely."
                                            + " Rejecting connection");
                                }
                                try {
                                    caller.onDisconnected();
                                } catch (RemoteException e) {
                                    // Controller may be died prematurely.
                                    // Not an issue because we'll ignore it anyway.
                                }
                            }
                        }
                    }
                });
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public void close() {
            mServiceImpl.clear();
            mHandler.removeCallbacksAndMessages(null);
        }
    }
}
