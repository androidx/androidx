/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.profileinstaller;

import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import androidx.annotation.RestrictTo;

/**
 * Contains methods to handle android multiuser.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UserInfo {

    private static final String TAG = "ProfileInstaller";

    private UserInfo() { }

    /**
     * Returns the current selected user id. Internally the value is read from
     * {@link android.os.Process#myUserHandle()}.
     *
     * @return the current selected user id.
     */
    static int getCurrentUserId() {
        try {
            Parcel parcel = Parcel.obtain();
            Process.myUserHandle().writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return parcel.readInt();
        } catch (Throwable e) {
            Log.d(TAG, "Error when reading current user id. Selected default user id `0`.");
            return 0;
        }
    }
}
