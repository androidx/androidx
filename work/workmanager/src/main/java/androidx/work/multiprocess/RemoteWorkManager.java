/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.work.multiprocess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkManagerImpl;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A subset of {@link androidx.work.WorkManager} APIs that are available for apps that use
 * multiple processes.
 */
public abstract class RemoteWorkManager {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected RemoteWorkManager() {
        // Does nothing
    }

    /**
     * Enqueues one item for background processing.
     *
     * @param request The {@link WorkRequest} to enqueue
     * @return An {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue(@NonNull WorkRequest request);

    /**
     * Enqueues one or more items for background processing.
     *
     * @param requests One or more {@link WorkRequest} to enqueue
     * @return An {@link ListenableFuture} that can be used to determine when the enqueue has
     * completed
     */
    @NonNull
    public abstract ListenableFuture<Void> enqueue(@NonNull List<WorkRequest> requests);

    /**
     * Gets the instance of {@link RemoteWorkManager} which provides a subset of
     * {@link WorkManager} APIs that are safe to use for apps that use multiple processes.
     *
     * @param context The application context.
     * @return The instance of {@link RemoteWorkManager}.
     */
    @NonNull
    public static RemoteWorkManager getInstance(@NonNull Context context) {
        WorkManagerImpl workManager = WorkManagerImpl.getInstance(context);
        RemoteWorkManager remoteWorkManager = workManager.getRemoteWorkManager();
        if (remoteWorkManager == null) {
            // Should never really happen.
            throw new IllegalStateException("Unable to initialize RemoteWorkManager");
        }
        return remoteWorkManager;
    }
}
