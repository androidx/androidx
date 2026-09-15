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

internal class FakeAgeSignalProvider(
    private val context: Context,
    private val isAvailableResponse: Boolean = true,
    private val response: GetAgeRangeResponse? = null,
    private val exception: GetAgeRangeException? = null,
) : AgeSignalProvider {

    // Required for reflection instantiation tests
    public constructor(context: Context) : this(context, true, null, null)

    var lastReceivedContext: Context? = null
    var lastReceivedRequest: GetAgeRangeRequest? = null
    var lastReceivedCancellationSignal: CancellationSignal? = null

    override fun onGetAgeRange(
        context: Context,
        request: GetAgeRangeRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: OutcomeReceiverCompat<GetAgeRangeResponse, GetAgeRangeException>,
    ) {
        lastReceivedContext = context
        lastReceivedRequest = request
        lastReceivedCancellationSignal = cancellationSignal

        if (cancellationSignal?.isCanceled == true) {
            return
        }

        executor.execute {
            if (exception != null) {
                callback.onError(exception)
            } else if (response != null) {
                callback.onResult(response)
            }
        }
    }

    override fun isAvailable(): Boolean = isAvailableResponse
}
