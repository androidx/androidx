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

/**
 * An interface which helps surface user-visible notifications for a
 * {@link androidx.work.WorkRequest} when {@link WorkRequest.Builder#setRunInForeground(boolean)}
 * is set to {@code true}.
 */
public interface NotificationProvider {

    /**
     * @return The {@link NotificationMetadata} which can be used to surface user-visible
     * notifications associated with a {@link androidx.work.WorkRequest} when
     * {@link WorkRequest.Builder#setRunInForeground(boolean)} is set to {@code true}.
     * <p>
     * {@link androidx.work.WorkManager} will request {@link NotificationMetadata} when it starts
     * executing a {@link androidx.work.WorkRequest} and once per progress update when running in
     * foreground mode.  Please keep in mind that a {@link ListenableWorker} may be used for
     * multiple WorkRequests and will not be asked to show notifications if
     * {@link WorkRequest.Builder#setRunInForeground(boolean)} is set to {@code false}.
     * <p>
     * All processing in this method should be lightweight. There are no contractual guarantees
     * about which thread will invoke this call, so this should not be a long-running or blocking
     * operation.
     */
    @NonNull
    NotificationMetadata getNotification();
}
