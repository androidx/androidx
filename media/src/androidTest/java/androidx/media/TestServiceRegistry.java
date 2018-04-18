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

import static org.junit.Assert.fail;

import android.os.Handler;

import androidx.annotation.GuardedBy;
import androidx.media.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media.TestUtils.SyncHandler;

/**
 * Keeps the instance of currently running {@link MockMediaSessionService2}. And also provides
 * a way to control them in one place.
 * <p>
 * It only support only one service at a time.
 */
public class TestServiceRegistry {
    @GuardedBy("TestServiceRegistry.class")
    private static TestServiceRegistry sInstance;
    @GuardedBy("TestServiceRegistry.class")
    private MediaSessionService2 mService;
    @GuardedBy("TestServiceRegistry.class")
    private SyncHandler mHandler;
    @GuardedBy("TestServiceRegistry.class")
    private MediaLibrarySessionCallback mSessionCallback;
    @GuardedBy("TestServiceRegistry.class")
    private SessionServiceCallback mSessionServiceCallback;

    /**
     * Callback for session service's lifecyle (onCreate() / onDestroy())
     */
    public interface SessionServiceCallback {
        void onCreated();
        void onDestroyed();
    }

    public static TestServiceRegistry getInstance() {
        synchronized (TestServiceRegistry.class) {
            if (sInstance == null) {
                sInstance = new TestServiceRegistry();
            }
            return sInstance;
        }
    }

    public void setHandler(Handler handler) {
        synchronized (TestServiceRegistry.class) {
            mHandler = new SyncHandler(handler.getLooper());
        }
    }

    public Handler getHandler() {
        synchronized (TestServiceRegistry.class) {
            return mHandler;
        }
    }

    public void setSessionServiceCallback(SessionServiceCallback sessionServiceCallback) {
        synchronized (TestServiceRegistry.class) {
            mSessionServiceCallback = sessionServiceCallback;
        }
    }

    public void setSessionCallback(MediaLibrarySessionCallback sessionCallback) {
        synchronized (TestServiceRegistry.class) {
            mSessionCallback = sessionCallback;
        }
    }

    public MediaLibrarySessionCallback getSessionCallback() {
        synchronized (TestServiceRegistry.class) {
            return mSessionCallback;
        }
    }

    public void setServiceInstance(MediaSessionService2 service) {
        synchronized (TestServiceRegistry.class) {
            if (mService != null) {
                fail("Previous service instance is still running. Clean up manually to ensure"
                        + " previoulsy running service doesn't break current test");
            }
            mService = service;
            if (mSessionServiceCallback != null) {
                mSessionServiceCallback.onCreated();
            }
        }
    }

    public MediaSessionService2 getServiceInstance() {
        synchronized (TestServiceRegistry.class) {
            return mService;
        }
    }

    public void cleanUp() {
        synchronized (TestServiceRegistry.class) {
            if (mService != null) {
                // TODO(jaewan): Remove this, and override SessionService#onDestroy() to do this
                mService.getSession().close();
                // stopSelf() would not kill service while the binder connection established by
                // bindService() exists, and close() above will do the job instead.
                // So stopSelf() isn't really needed, but just for sure.
                mService.stopSelf();
                mService = null;
            }
            if (mHandler != null) {
                mHandler.removeCallbacksAndMessages(null);
            }
            mSessionCallback = null;
            if (mSessionServiceCallback != null) {
                mSessionServiceCallback.onDestroyed();
                mSessionServiceCallback = null;
            }
        }
    }
}
