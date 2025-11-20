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
import android.content.pm.PackageManager;

import org.jspecify.annotations.NonNull;

class MediaSessionManagerImplApi21 extends MediaSessionManagerImplBase {
    MediaSessionManagerImplApi21(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isTrustedForMediaControl(
            MediaSessionManager.@NonNull RemoteUserInfoImpl userInfo) {

        return hasMediaControlPermission(userInfo) || super.isTrustedForMediaControl(userInfo);
    }

    /**
     * Checks the caller has android.Manifest.permission.MEDIA_CONTENT_CONTROL permission.
     */
    private boolean hasMediaControlPermission(
            MediaSessionManager.@NonNull RemoteUserInfoImpl userInfo) {
        return getContext().checkPermission(
                android.Manifest.permission.MEDIA_CONTENT_CONTROL,
                userInfo.getPid(), userInfo.getUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
