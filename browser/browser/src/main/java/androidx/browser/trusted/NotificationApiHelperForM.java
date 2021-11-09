/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted;

import android.app.NotificationManager;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

/**
 *
 * Utility class to use new APIs that were added in M (API level 23). These need to exist in a
 * separate class so that Android framework can successfully verify classes without
 * encountering the new APIs.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.M)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class NotificationApiHelperForM {

    /**
     * Returns the active notifications as an array of Parcelables. Since StatusBarNotification was
     * added in API 18, returning the result as StatusBarNotification[] would prevent classes from
     * being verified on earlier Jellybean builds.
     */
    @NonNull
    static Parcelable[] getActiveNotifications(NotificationManager manager) {
        return manager.getActiveNotifications();
    }

    private NotificationApiHelperForM() {}
}
