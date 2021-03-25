/*
 * Copyright 2021 The Android Open Source Project
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
 * Implementation for a multi-process {@link ListenableWorker}.
 *
 * @hide
 */
oneway interface IListenableWorkerImpl {
   // request is a ParcelablelRemoteRequest instance.
   // callback gets a parcelized representation of Result
   oneway void startWork(in byte[] request, IWorkManagerImplCallback callback);

   // interrupt request.
   // request is a ParcelableWorkerParameters instance.
   // callback gets an empty result
   oneway void interrupt(in byte[] request, IWorkManagerImplCallback callback);
}
