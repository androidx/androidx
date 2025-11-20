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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.content.Context;
import android.os.Build;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides support for interacting with {@link MediaSessionCompat media sessions} that applications
 * have published to express their ongoing media playback state.
 *
 * @see MediaSessionCompat
 * @see MediaControllerCompat
 * @deprecated androidx.media is deprecated. Please migrate to <a
 *     href="https://developer.android.com/media/media3">androidx.media3</a>.
 */
@Deprecated
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
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        synchronized (sLock) {
            if (sSessionManager == null) {
                sSessionManager = new MediaSessionManager(context.getApplicationContext());
            }
            return sSessionManager;
        }
    }

    private MediaSessionManager(Context context) {
        if (Build.VERSION.SDK_INT >= 28) {
            mImpl = new MediaSessionManagerImplApi28(context);
        } else {
            mImpl = new MediaSessionManagerImplApi21(context);
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
     * Information of a remote user of {@link MediaSessionCompat} or {@link
     * MediaBrowserServiceCompat}. This can be used to decide whether the remote user is trusted
     * app, and also differentiate caller of {@link MediaSessionCompat} and {@link
     * MediaBrowserServiceCompat} callbacks.
     *
     * <p>See {@link #equals(Object)} to take a look at how it differentiate media controller.
     *
     * @see #isTrustedForMediaControl(RemoteUserInfo)
     * @deprecated androidx.media is deprecated. Please migrate to <a
     *     href="https://developer.android.com/media/media3">androidx.media3</a>.
     */
    @Deprecated
    public static final class RemoteUserInfo {
        /**
         * Used by {@link #getPackageName()} when the session is connected to the legacy controller
         * whose exact package name cannot be obtained.
         */
        public static final String LEGACY_CONTROLLER = "android.media.session.MediaController";

        /**
         * Represents an unknown pid of an application.
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public static final int UNKNOWN_PID = -1;

        /**
         * Represents an unknown uid of an application.
         */
        @RestrictTo(LIBRARY_GROUP_PREFIX)
        public static final int UNKNOWN_UID = -1;

        RemoteUserInfoImpl mImpl;

        /**
         * Public constructor.
         * <p>
         * Can be used for {@link MediaSessionManager#isTrustedForMediaControl(RemoteUserInfo)}}.
         *
         * @param packageName package name of the remote user
         * @param pid pid of the remote user
         * @param uid uid of the remote user
         * @throws IllegalArgumentException if package name is empty
         */
        public RemoteUserInfo(@NonNull String packageName, int pid, int uid) {
            if (packageName == null) {
                throw new NullPointerException("package shouldn't be null");
            } else if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException("packageName should be nonempty");
            }
            if (Build.VERSION.SDK_INT >= 28) {
                mImpl = new MediaSessionManagerImplApi28.RemoteUserInfoImplApi28(
                        packageName, pid, uid);
            } else {
                // Note: We need to include IBinder to distinguish controllers in a process.
                mImpl = new MediaSessionManagerImplBase.RemoteUserInfoImplBase(
                        packageName, pid, uid);
            }
        }

        /**
         * Public constructor for internal uses.
         * <p>
         * Internal code MUST use this on SDK >= 28 to distinguish individual RemoteUserInfos in a
         * process.
         *
         * @param remoteUserInfo Framework RemoteUserInfo
         * @throws IllegalArgumentException if package name is empty
         */
        @RestrictTo(LIBRARY)
        @RequiresApi(28)
        public RemoteUserInfo(
                android.media.session.MediaSessionManager.RemoteUserInfo remoteUserInfo) {
            // Framework RemoteUserInfo doesn't ensure non-null nor non-empty package name,
            // so ensure package name here instead.
            String packageName =
                    MediaSessionManagerImplApi28.RemoteUserInfoImplApi28.getPackageName(
                            remoteUserInfo);
            if (packageName == null) {
                throw new NullPointerException("package shouldn't be null");
            } else if (TextUtils.isEmpty(packageName)) {
                throw new IllegalArgumentException("packageName should be nonempty");
            }
            mImpl = new MediaSessionManagerImplApi28.RemoteUserInfoImplApi28(remoteUserInfo);
        }

        /**
         * @return package name of the controller. Can be {@link #LEGACY_CONTROLLER} if the package
         *         name cannot be obtained.
         */
        public @NonNull String getPackageName() {
            return mImpl.getPackageName();
        }

        /**
         * @return pid of the controller. Can be a negative value if the pid cannot be obtained.
         */
        public int getPid() {
            return mImpl.getPid();
        }

        /**
         * @return uid of the controller. Can be a negative value if the uid cannot be obtained.
         */
        public int getUid() {
            return mImpl.getUid();
        }

        /**
         * Returns equality of two RemoteUserInfo by comparing their package name, UID, and PID.
         * <p>
         * On P and before (API <= 28), two RemoteUserInfo objects equal if following conditions are
         * met:
         * <ol>
         * <li>UID and package name are the same</li>
         * <li>One of the RemoteUserInfo's PID is UNKNOWN_PID or both of RemoteUserInfo's
         *     PID are the same</li>
         * </ol>
         *
         * @param obj the reference object with which to compare.
         * @return {@code true} if equals, {@code false} otherwise
         */
        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RemoteUserInfo)) {
                return false;
            }
            return mImpl.equals(((RemoteUserInfo) obj).mImpl);
        }

        @Override
        public int hashCode() {
            return mImpl.hashCode();
        }
    }
}
