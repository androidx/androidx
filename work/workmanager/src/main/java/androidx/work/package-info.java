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

/**
 * WorkManager is a library used to enqueue deferrable work that is guaranteed to execute sometime
 * after its {@link androidx.work.Constraints} are met.  WorkManager allows observation of work
 * status and the ability to create complex chains of work.
 * <p>
 * WorkManager uses an underlying job dispatching service when available based on the following
 * criteria:
 * <p><ul>
 * <li>Uses JobScheduler for API 23+
 * <li>Uses a custom AlarmManager + BroadcastReceiver implementation for API 14-22</ul>
 * <p>
 * All work must be done in a {@link androidx.work.ListenableWorker} class.  A simple
 * implementation, {@link androidx.work.Worker}, is recommended as the starting point for most
 * developers.  With the optional dependencies, you can also use {@code CoroutineWorker} or
 * {@code RxWorker}.  All background work is given a maximum of ten minutes to finish its execution.
 * After this time has expired, the worker will be signalled to stop.
 * <p>
 * There are two types of work supported by WorkManager: {@link androidx.work.OneTimeWorkRequest}
 * and {@link androidx.work.PeriodicWorkRequest}.  OneTimeWorkRequests can be chained together into
 * acyclic graphs.  Work is eligible for execution when all of its prerequisites are complete.
 * If any of its prerequisites fail or are cancelled, the work will never run.
 * <p>
 * WorkRequests can accept {@link androidx.work.Constraints}, inputs (see
 * {@link androidx.work.Data}), and backoff criteria.  WorkRequests can be tagged with
 * human-readable Strings (see {@link androidx.work.WorkRequest.Builder#addTag(String)}), and
 * chains of work can be given a uniquely-identifiable name with conflict policies. *
 * <p>
 * <b>Manually initializing WorkManager</b>
 * <p>
 * You can manually initialize WorkManager and provide a custom {@link androidx.work.Configuration}
 * for it.  Please see {@link androidx.work.WorkManager#initialize(Context, Configuration)}.
 */
package androidx.work;

import android.content.Context;
