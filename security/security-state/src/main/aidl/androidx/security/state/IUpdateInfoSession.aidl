/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.security.state;

import androidx.security.state.UpdateCheckResult;

/**
 * A private, stateful session for a specific client to query security update information.
 *
 * <p>This interface represents an active connection between a client application and an
 * update provider (such as the System Updater or Google Play Store). It is obtained by
 * calling {@link IUpdateInfoService#openSession}.
 *
 * <p>The session maintains the verified identity of the calling client, ensuring that
 * all queries and telemetry are correctly attributed. Clients <b>must</b> call
 * {@link #close()} when they are finished to release resources on the provider side.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IUpdateInfoSession {
    /**
     * Retrieves available updates and the time they were last synchronized.
     *
     * <p>This method returns the latest available update information known to the provider.
     *
     * <h3>Freshness Contract</h3>
     * The provider is responsible for maintaining data freshness by periodically synchronizing
     * with its backend. Clients rely on this contract to display timely status information.
     *
     * <h3>Behavior & Latency</h3>
     * <ul>
     *   <li><b>Cached Data:</b> If the provider determines its local data is sufficiently fresh,
     *       it will immediately return the cached data.</li>
     *   <li><b>Real-Time Fetch:</b> If the data is stale, the provider may attempt to fetch
     *       new data from the backend. This means this IPC call <b>may block</b> the calling
     *       thread for the duration of the network request. Clients should invoke this from
     *       a background thread or suspending coroutine.</li>
     *   <li><b>Rate Limiting:</b> Providers enforce their own rate-limiting logic to protect
     *       their backend infrastructure. If a request is throttled, the provider will not
     *       throw an exception; instead, it will gracefully return the currently cached data.</li>
     * </ul>
     *
     * @return An {@link UpdateCheckResult} containing the list of available updates and the
     *         timestamp of the last successful synchronization with the backend.
     */
    UpdateCheckResult listAvailableUpdates();

    /**
     * Closes the session and releases associated resources.
     *
     * <p>Clients must call this method when they no longer need to query the provider.
     * Invoking this method notifies the provider that the session has ended, allowing
     * it to log accurate session duration telemetry and clean up internal state.
     *
     * <p>This is a {@code oneway} (asynchronous) call, meaning the client will return
     * immediately without blocking to wait for the provider to complete the teardown.
     *
     * <p>Once closed, any further calls to {@link #listAvailableUpdates()} on this
     * session instance will fail.
     */
    oneway void close();
}