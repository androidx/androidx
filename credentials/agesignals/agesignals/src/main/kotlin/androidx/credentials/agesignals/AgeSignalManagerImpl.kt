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
import androidx.annotation.VisibleForTesting
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.agesignals.exceptions.GetAgeRangeException
import androidx.credentials.agesignals.exceptions.GetAgeRangeProviderConfigurationException
import java.util.concurrent.Executor

internal class AgeSignalManagerImpl
@VisibleForTesting
internal constructor(private val providerFactory: AgeSignalProviderFactory) : AgeSignalManager {

    internal constructor(context: Context) : this(AgeSignalProviderFactory(context))

    override fun getAgeRangeAsync(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        val provider: AgeSignalProvider? = providerFactory.getBestAvailableProvider()
        if (provider == null) {
            executor.execute {
                callback.onError(
                    GetAgeRangeProviderConfigurationException(
                        "getAgeRange: no provider dependencies found - please ensure the " +
                            "desired provider dependencies are added"
                    )
                )
            }
            return
        }

        provider.onGetAgeRange(context, request, cancellationSignal, executor, callback)
    }
}
