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

package androidx.credentials.agesignals

import android.content.Context
import android.os.CancellationSignal
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import java.util.concurrent.Executor

/**
 * Provider interface to be implemented by age signal providers that will fulfill [AgeSignalManager]
 * requests.
 *
 * Implementations **must** provide a public constructor that accepts a single [Context] parameter.
 *
 * Providers are discovered via metadata declared inside a `<service>` in `AndroidManifest.xml`:
 * ```xml
 * <service
 *     android:name=".MyAgeSignalProviderService"
 *     android:exported="false">
 *     <meta-data
 *         android:name="androidx.credentials.agesignals.AGE_SIGNAL_PROVIDER_KEY"
 *         android:value="com.example.provider.MyAgeSignalProvider" />
 * </service>
 * ```
 */
public interface AgeSignalProvider {

    /**
     * Invoked when an application requests age range information.
     *
     * @param context the client calling context used to launch any interactive UI (e.g. user
     *   consent or parental verification dialogs)
     * @param request the request for getting the user's age range
     * @param cancellationSignal an optional signal allowing the caller to cancel the in-flight
     *   operation
     * @param executor the executor on which the [callback] must be invoked
     * @param callback the callback invoked upon successful retrieval or failure
     */
    public fun onGetAgeRange(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    )

    /**
     * Determines whether the provider is supported and available on the current device.
     *
     * @return `true` if this provider can fulfill requests on this device; `false` otherwise
     */
    public fun isAvailable(): Boolean
}
