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

package androidx.media;

import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.media.MediaSessionService2.MediaNotification;

/**
 * Implementation of {@link MediaSessionService2}.
 */
class MediaSessionService2ImplBase implements MediaSessionService2.SupportLibraryImpl {
    private static final String TAG = "MSS2ImplBase";
    private static final boolean DEBUG = true;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private MediaSession2 mSession;

    MediaSessionService2ImplBase() {
    }

    @Override
    public void onCreate(MediaSessionService2 service) {
        SessionToken2 token = new SessionToken2(service,
                new ComponentName(service, service.getClass().getName()));
        if (token.getType() != getSessionType()) {
            throw new RuntimeException("Expected session type " + getSessionType()
                    + " but was " + token.getType());
        }
        MediaSession2 session = service.onCreateSession(token.getId());
        synchronized (mLock) {
            mSession = session;
            if (mSession == null || !token.getId().equals(mSession.getToken().getId())
                    || mSession.getToken().getType() != getSessionType()) {
                mSession = null;
                throw new RuntimeException("Expected session with id " + token.getId()
                        + " and type " + token.getType() + ", but got " + mSession);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (MediaSessionService2.SERVICE_INTERFACE.equals(intent.getAction())) {
            synchronized (mLock) {
                if (mSession != null) {
                    return mSession.getSessionBinder();
                } else if (DEBUG) {
                    Log.d(TAG, "Session hasn't created");
                }
            }
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

    @Override
    public int getSessionType() {
        return SessionToken2.TYPE_SESSION_SERVICE;
    }
}
