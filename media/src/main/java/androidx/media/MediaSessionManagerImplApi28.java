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
        if (userInfo instanceof RemoteUserInfo) {
            return mObject.isTrustedForMediaControl(((RemoteUserInfo) userInfo).mObject);
        }
        return false;
    }

    static final class RemoteUserInfo implements MediaSessionManager.RemoteUserInfoImpl {
        android.media.session.MediaSessionManager.RemoteUserInfo mObject;

        RemoteUserInfo(String packageName, int pid, int uid) {
            mObject = new android.media.session.MediaSessionManager.RemoteUserInfo(
                    packageName, pid, uid);
        }

        @Override
        public String getPackageName() {
            return mObject.getPackageName();
        }

        @Override
        public int getPid() {
            return mObject.getPid();
        }

        @Override
        public int getUid() {
            return mObject.getUid();
        }
    }
}
