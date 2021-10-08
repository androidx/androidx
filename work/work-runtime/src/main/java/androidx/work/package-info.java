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
 * <b>Initializing WorkManager</b>
 * <p>
 * By default, WorkManager is initialized using a {@code ContentProvider} with a default
 * {@link androidx.work.Configuration}.  ContentProviders are created and run before the
 * {@code Application} object, so this allows the WorkManager singleton to be setup before your
 * code can run in most cases.  This is suitable for most developers.  However, you can provide a
 * custom {@link androidx.work.Configuration} by using {@link androidx.work.Configuration.Provider}
 * or
 * {@link androidx.work.WorkManager#initialize(android.content.Context, androidx.work.Configuration)}.
 * <p>
 * <b>WorkManager and its Interactions with the OS</b>
 * <p>
 * WorkManager {@code BroadcastReceiver}s to monitor {@link androidx.work.Constraints} on devices
 * before API 23.  The BroadcastReceivers are disabled on API 23 and up.  In particular, WorkManager
 * listens to the following {@code Intent}s:
 * <p><ul>
 *     <li>{@code android.intent.action.ACTION_POWER_CONNECTED}</li>
 *     <li>{@code android.intent.action.ACTION_POWER_DISCONNECTED}</li>
 *     <li>{@code android.intent.action.BATTERY_OKAY}</li>
 *     <li>{@code android.intent.action.BATTERY_LOW}</li>
 *     <li>{@code android.intent.action.DEVICE_STORAGE_LOW}</li>
 *     <li>{@code android.intent.action.DEVICE_STORAGE_OK}</li>
 *     <li>{@code android.net.conn.CONNECTIVITY_CHANGE}</li>
 * </ul>
 * In addition, WorkManager listens to system time changes and reboots to properly reschedule work
 * in certain situations.  For this, it listens to the following Intents:
 * <p><ul>
 *     <li>{@code android.intent.action.BOOT_COMPLETED}</li>
 *     <li>{@code android.intent.action.TIME_SET}</li>
 *     <li>{@code android.intent.action.TIMEZONE_CHANGED}</li>
 * </ul>
 * <p>
 * WorkManager uses the following permissions:
 * <p><ul>
 *     <li>{@code android.permission.WAKE_LOCK} to make it can keep the device awake to complete
 *     work before API 23</li>
 *     <li>{@code android.permission.ACCESS_NETWORK_STATE} to listen to network changes before API
 *     23 and monitor network {@link androidx.work.Constraints}</li>
 *     <li>{@code android.permission.RECEIVE_BOOT_COMPLETED} to listen to reboots and reschedule
 *     work properly.</li>
 * </ul>
 * <p>
 * Note that WorkManager may enable or disable some of its BroadcastReceivers at runtime as needed.
 * This has the side-effect of the system sending {@code ACTION_PACKAGE_CHANGED} broadcasts to your
 * app.  Please be aware of this use case and architect your app appropriately (especially if you
 * are using widgets - see https://issuetracker.google.com/115575872).
 */
package androidx.work;

