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

package androidx.work;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * An interface which helps surface user visible notifications for a
 * {@link androidx.work.WorkRequest} when running in the context of a foreground
 * {@link android.app.Service}.
 *
 * TODO Unhide for foreground service support
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface NotificationProvider {

    /***
     * @return The {@link NotificationMetadata} which can be used to surface user visible
     * notifications associated with a {@link androidx.work.WorkRequest}.
     * <p>
     * {@link androidx.work.WorkManager} will request {@link NotificationMetadata} when it starts
     * executing a {@link androidx.work.WorkRequest} and once per progress update when running in
     * the context of a foreground {@link android.app.Service}.
     * <p>
     * All processing in this method should be lightweight. There are no contractual guarantees
     * about which thread will invoke this call, so this should not be a long-running or blocking
     * operation.
     */
    @NonNull
    NotificationMetadata getNotification();
}
