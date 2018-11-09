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
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

class MediaSessionManagerImplBase implements MediaSessionManager.MediaSessionManagerImpl {
    private static final String TAG = MediaSessionManager.TAG;
    private static final boolean DEBUG = MediaSessionManager.DEBUG;

    private static final String PERMISSION_STATUS_BAR_SERVICE =
            "android.permission.STATUS_BAR_SERVICE";
    private static final String PERMISSION_MEDIA_CONTENT_CONTROL =
            "android.permission.MEDIA_CONTENT_CONTROL";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";

    Context mContext;
    ContentResolver mContentResolver;

    MediaSessionManagerImplBase(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public boolean isTrustedForMediaControl(
            @NonNull MediaSessionManager.RemoteUserInfoImpl userInfo) {
        try {
            ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(
                    userInfo.getPackageName(), 0);
            if (applicationInfo == null) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            if (DEBUG) {
                Log.d(TAG, "Package " + userInfo.getPackageName() + " doesn't exist");
            }
            return false;
        }
        return isPermissionGranted(userInfo, PERMISSION_STATUS_BAR_SERVICE)
                || isPermissionGranted(userInfo, PERMISSION_MEDIA_CONTENT_CONTROL)
                || userInfo.getUid() == Process.SYSTEM_UID
                || isEnabledNotificationListener(userInfo);
    }

    private boolean isPermissionGranted(MediaSessionManager.RemoteUserInfoImpl userInfo,
            String permission) {
        if (userInfo.getPid() < 0) {
            // This may happen for the MediaBrowserServiceCompat#onGetRoot().
            return mContext.getPackageManager().checkPermission(
                    permission, userInfo.getPackageName()) == PackageManager.PERMISSION_GRANTED;
        }
        return mContext.checkPermission(permission, userInfo.getPid(), userInfo.getUid())
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This checks if the component is an enabled notification listener for the
     * specified user. Enabled components may only operate on behalf of the user
     * they're running as.
     *
     * @return True if the component is enabled, false otherwise
     */
    @SuppressWarnings("StringSplitter")
    boolean isEnabledNotificationListener(
            @NonNull MediaSessionManager.RemoteUserInfoImpl userInfo) {
        final String enabledNotifListeners = Settings.Secure.getString(mContentResolver,
                ENABLED_NOTIFICATION_LISTENERS);
        if (enabledNotifListeners != null) {
            final String[] components = enabledNotifListeners.split(":");
            for (int i = 0; i < components.length; i++) {
                final ComponentName component =
                        ComponentName.unflattenFromString(components[i]);
                if (component != null) {
                    if (component.getPackageName().equals(userInfo.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static class RemoteUserInfoImplBase implements MediaSessionManager.RemoteUserInfoImpl {
        private String mPackageName;
        private int mPid;
        private int mUid;

        RemoteUserInfoImplBase(String packageName, int pid, int uid) {
            mPackageName = packageName;
            mPid = pid;
            mUid = uid;
        }

        @Override
        public String getPackageName() {
            return mPackageName;
        }

        @Override
        public int getPid() {
            return mPid;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RemoteUserInfoImplBase)) {
                return false;
            }
            RemoteUserInfoImplBase otherUserInfo = (RemoteUserInfoImplBase) obj;
            return TextUtils.equals(mPackageName, otherUserInfo.mPackageName)
                    && mPid == otherUserInfo.mPid
                    && mUid == otherUserInfo.mUid;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mPackageName, mPid, mUid);
        }
    }
}

