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

import androidx.work.multiprocess.IWorkManagerImplCallback;

/**
 * Implementation for {@link IWorkManager}.
 *
 * @hide
 */
oneway interface IWorkManagerImpl {
    // Enqueues WorkRequests
    void enqueueWorkRequests(in byte[] request, IWorkManagerImplCallback callback);
    void updateUniquePeriodicWorkRequest(
        String name, in byte[] request, IWorkManagerImplCallback callback);
    // Enqueues WorkContinuations
    void enqueueContinuation(in byte[] request, IWorkManagerImplCallback callback);
    // Cancel APIs
    void cancelWorkById(String id, IWorkManagerImplCallback callback);
    void cancelAllWorkByTag(String tag, IWorkManagerImplCallback callback);
    void cancelUniqueWork(String name, IWorkManagerImplCallback callback);
    void cancelAllWork(IWorkManagerImplCallback callback);
    // Query APIs
    void queryWorkInfo(in byte[] request, IWorkManagerImplCallback callback);
    // Progress APIs
    void setProgress(in byte[] request, IWorkManagerImplCallback callback);
    // Foreground Info APIs
    void setForegroundAsync(in byte[] request, IWorkManagerImplCallback callback);
}
