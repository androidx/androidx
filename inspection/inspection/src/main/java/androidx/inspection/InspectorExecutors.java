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

package androidx.inspection;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Inspectors should use these executors instead of creating any threads themselves.
 *
 * {@code InspectorExecutors} provide:
 * <ul>
 * <li>threads named with "Studio:" prefix, that apps developers and tools use to track the
 * origin of these threads;
 * </li>
 * <li>better exception handling, meaning crashes will neither be silently swallowed
 * nor crash the inspected app. All unhandled exceptions will be reported to Studio and
 * will result in the shutdown of the inspector;
 * </li>
 * <li> load management, allowing inspectors to have a smaller performance effect on the inspected
 * app.
 * </li>
 * </ul>
 */
public interface InspectorExecutors {
    /**
     * Handler that backs the {@link #primary()} executor.
     * <p>
     * Generally, {@link #primary()} should be used as main entry point. {@code handler} provides
     * an escape hatch when tasks need to be scheduled with delays, e.g in implementation of
     * throttling or polling.
     * <p>
     * Implementation note: {@link Handler} was chosen over
     * {@link java.util.concurrent.ScheduledExecutorService}, because proper handling of
     * exceptions that occur on the primary executor requires a blocking {@code get()} on the future
     * returned by its {@code schedule} call. This makes it very hard to correctly handle
     * exceptions, because these futures can be accidentally dropped by inspector developers.
     * Finally, plain {@link java.util.concurrent.ScheduledThreadPoolExecutor} can't be used
     * as {@link Executor}, because its execute method silently swallows exceptions.
     * Even when a future wasn't dropped or lost, developers would still need to block one
     * of the threads.
     */
    @NonNull
    Handler handler();

    /**
     * Primary single threaded executor for the given inspector.
     * <p>
     * All inspector methods, such as
     * {@link Inspector#onReceiveCommand(byte[], Inspector.CommandCallback)} are called on this
     * thread.
     * <p>
     * It is important to keep this executor responsive, so it can quickly process incoming
     * messages.
     */
    @NonNull
    Executor primary();

    /**
     * An executor for offloading blocking IO tasks to a shared pool of threads.
     */
    @NonNull
    Executor io();
}
