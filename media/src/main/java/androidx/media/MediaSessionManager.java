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

import android.content.Context;
import android.os.Build;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.BuildCompat;

/**
 * Provides support for interacting with {@link MediaSessionCompat media sessions} that
 * applications have published to express their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaControllerCompat
 */
public final class MediaSessionManager {
    static final String TAG = "MediaSessionManager";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final Object sLock = new Object();
    private static volatile MediaSessionManager sSessionManager;

    MediaSessionManagerImpl mImpl;

    /**
     * Gets an instance of the media session manager associated with the context.
     *
     * @return The MediaSessionManager instance for this context.
     */
    public static @NonNull MediaSessionManager getSessionManager(@NonNull Context context) {
        MediaSessionManager manager = sSessionManager;
        if (manager == null) {
            synchronized (sLock) {
                manager = sSessionManager;
                if (manager == null) {
                    sSessionManager = new MediaSessionManager(context.getApplicationContext());
                    manager = sSessionManager;
                }
            }
        }
        return manager;
    }

    private MediaSessionManager(Context context) {
        if (BuildCompat.isAtLeastP()) {
            mImpl = new MediaSessionManagerImplApi28(context);
        } else if (Build.VERSION.SDK_INT >= 21) {
            mImpl = new MediaSessionManagerImplApi21(context);
        } else {
            mImpl = new MediaSessionManagerImplBase(context);
        }
    }

    /**
     * Checks whether the remote user is a trusted app.
     * <p>
     * An app is trusted if the app holds the android.Manifest.permission.MEDIA_CONTENT_CONTROL
     * permission or has an enabled notification listener.
     *
     * @param userInfo The remote user info from either
     *            {@link MediaSessionCompat#getCurrentControllerInfo()} and
     *            {@link MediaBrowserServiceCompat#getCurrentBrowserInfo()}.
     * @return {@code true} if the remote user is trusted and its package name matches with the UID.
     *            {@code false} otherwise.
     */
    public boolean isTrustedForMediaControl(@NonNull RemoteUserInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("userInfo should not be null");
        }
        return mImpl.isTrustedForMediaControl(userInfo.mImpl);
    }

    Context getContext() {
        return mImpl.getContext();
    }

    interface MediaSessionManagerImpl {
        Context getContext();
        boolean isTrustedForMediaControl(RemoteUserInfoImpl userInfo);
    }

    interface RemoteUserInfoImpl {
        String getPackageName();
        int getPid();
        int getUid();
    }

    /**
     * Information of a remote user of {@link android.support.v4.media.session.MediaSessionCompat}
     * or {@link MediaBrowserServiceCompat}.
     * This can be used to decide whether the remote user is trusted app.
     *
     * @see #isTrustedForMediaControl(RemoteUserInfo)
     */
    public static final class RemoteUserInfo {
        /**
         * Used by {@link #getPackageName()} when the session is connected to the legacy controller
         * whose exact package name cannot be obtained.
         */
        public static String LEGACY_CONTROLLER = "android.media.session.MediaController";

        RemoteUserInfoImpl mImpl;

        public RemoteUserInfo(@NonNull String packageName, int pid, int uid) {
            if (BuildCompat.isAtLeastP()) {
                mImpl = new MediaSessionManagerImplApi28.RemoteUserInfo(packageName, pid, uid);
            } else {
                mImpl = new MediaSessionManagerImplBase.RemoteUserInfo(packageName, pid, uid);
            }
        }

        /**
         * @return package name of the controller. Can be {@link #LEGACY_CONTROLLER} if the package
         *         name cannot be obtained.
         */
        public @NonNull String getPackageName() {
            return mImpl.getPackageName();
        }

        /**
         * @return pid of the controller
         */
        public int getPid() {
            return mImpl.getPid();
        }

        /**
         * @return uid of the controller
         */
        public int getUid() {
            return mImpl.getUid();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return mImpl.equals(obj);
        }

        @Override
        public int hashCode() {
            return mImpl.hashCode();
        }
    }
}
