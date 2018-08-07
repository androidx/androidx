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

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media2.MediaSessionService2.MediaNotification;
import androidx.media2.MediaSessionService2.MediaSessionService2Impl;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2Impl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaSession2 mSession;

    MediaSessionService2ImplBase() {
    }

    @Override
    public void onCreate(final MediaSessionService2 service) {
        MediaSession2 session;
        synchronized (mLock) {
            session = mSession;
        }
        if (session == null) {
            session = service.onCreateSession();
            synchronized (mLock) {
                mSession = session;
            }
        }

        session.getCallback().setOnHandleForegroundServiceListener(
                new MediaSession2.SessionCallback.OnHandleForegroundServiceListener() {
                    @Override
                    public void onHandleForegroundService(int state) {
                        if (state == MediaPlayerConnector.PLAYER_STATE_IDLE
                                || state == MediaPlayerConnector.PLAYER_STATE_ERROR) {
                            service.stopForeground(false /* removeNotification */);
                            return;
                        }

                        // state is PLAYER_STATE_PLAYING or PLAYER_STATE_PAUSE.
                        MediaNotification mediaNotification = service.onUpdateNotification();
                        if (mediaNotification == null) {
                            return;
                        }

                        int notificationId = mediaNotification.getNotificationId();
                        Notification notification = mediaNotification.getNotification();

                        NotificationManagerCompat manager = NotificationManagerCompat.from(service);
                        manager.notify(notificationId, notification);
                        service.startForeground(notificationId, notification);
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        final MediaSession2 session = getSession();
        if (session == null) {
            Log.w(TAG, "Session hasn't created");
            return null;
        }
        switch (intent.getAction()) {
            case MediaSessionService2.SERVICE_INTERFACE:
                return session.getSessionBinder();
            case MediaBrowserServiceCompat.SERVICE_INTERFACE:
                return session.getLegacyBrowerServiceBinder();
        }
        return null;
    }

    @Override
    public MediaNotification onUpdateNotification() {
        // May supply default implementation later
        return null;
    }

    @Override
    public MediaSession2 getSession() {
        synchronized (mLock) {
            return mSession;
        }
    }
}
