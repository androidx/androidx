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

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.GuardedBy;
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
    private MediaNotificationHandler mNotificationHandler;

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
            if (session == null) {
                throw new RuntimeException("Session shouldn't be null");
            }
            synchronized (mLock) {
                mSession = session;
            }
        }
        mNotificationHandler = new MediaNotificationHandler(service);
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case Intent.ACTION_MEDIA_BUTTON: {
                final MediaSession2 session = getSession();
                if (session == null) {
                    Log.w(TAG, "Session hasn't created");
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
    public MediaNotification onUpdateNotification() {
        return mNotificationHandler.onUpdateNotification();
    }

    @Override
    public MediaSession2 getSession() {
        synchronized (mLock) {
            return mSession;
        }
    }
}
