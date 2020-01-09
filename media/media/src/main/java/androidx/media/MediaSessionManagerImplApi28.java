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

import androidx.annotation.RequiresApi;

@RequiresApi(28)
class MediaSessionManagerImplApi28 extends MediaSessionManagerImplApi21 {
    android.media.session.MediaSessionManager mObject;

    MediaSessionManagerImplApi28(Context context) {
        super(context);
        mObject = (android.media.session.MediaSessionManager) context
                .getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    @Override
    public boolean isTrustedForMediaControl(MediaSessionManager.RemoteUserInfoImpl userInfo) {
        // Don't use framework's isTrustedForMediaControl().
        // In P, framework's isTrustedForMediaControl() does the sanity check whether the UID, PID,
        // and package name match. In MediaSession/MediaController, Context#getPackageName() is
        // used by MediaController to tell MediaSession the package name.
        // However, UID, PID and Context#getPackageName() may not match if a activity/service runs
        // on the another app's process by specifying android:process in the AndroidManifest.xml.
        // In that case, sanity check will always fail.
        // Alternative way is to use Context#getOpPackageName() for sending the package name,
        // but it's hidden so we cannot use it.
        return super.isTrustedForMediaControl(userInfo);
    }

    /**
     * This extends {@link RemoteUserInfoImplBase} on purpose not to use frameworks' equals() and
     * hashCode() implementation for two reasons:
     *
     *   1. To override PID checks when one of them are unknown.
     *      PID can be unknown between MediaBrowserCompat / MediaBrowserServiceCompat
     *   2. To skip checking hidden binder.
     *      Framework's {@link android.media.session.MediaSessionManager.RemoteUserInfo} also checks
     *      internal binder to distinguish multiple {@link android.media.session.MediaController}
     *      and {@link android.media.browse.MediaBrowser} in a process. However, when the binders in
     *      both RemoteUserInfos are {@link null}, framework's equal() specially handles the case
     *      and returns {@code false}. This cause two issues that we need to workaround.
     *         Issue a) RemoteUserInfos created by key events are considered as all different.
     *         issue b) RemoteUserInfos created with public constructors are considers as all
     *                  different.
     */
    static final class RemoteUserInfoImplApi28 extends RemoteUserInfoImplBase {
        final android.media.session.MediaSessionManager.RemoteUserInfo mObject;

        RemoteUserInfoImplApi28(String packageName, int pid, int uid) {
            super(packageName, pid, uid);
            mObject = new android.media.session.MediaSessionManager.RemoteUserInfo(
                    packageName, pid, uid);
        }

        RemoteUserInfoImplApi28(
                android.media.session.MediaSessionManager.RemoteUserInfo remoteUserInfo) {
            super(remoteUserInfo.getPackageName(), remoteUserInfo.getPid(),
                    remoteUserInfo.getUid());
            mObject = remoteUserInfo;
        }
    }
}
