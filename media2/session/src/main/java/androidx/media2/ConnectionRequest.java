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

import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Created by {@link MediaController} to send its state to the {@link MediaSession} to request
 * to connect. It's intentionally {@link VersionedParcelable} for future extension.
 * <p>
 * All fields here are effectively final. Do not modify.
 */
@VersionedParcelize
class ConnectionRequest implements VersionedParcelable {
    @ParcelField(0)
    int mVersion;
    @ParcelField(1)
    String mPackageName;
    @ParcelField(2)
    int mPid;

    // For versioned parcelable.
    ConnectionRequest() {
        // no-op
    }

    ConnectionRequest(String packageName, int pid) {
        mVersion = MediaUtils.CURRENT_VERSION;
        mPackageName = packageName;
        mPid = pid;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getVersion() {
        return mVersion;
    }

    public int getPid() {
        return mPid;
    }
}
