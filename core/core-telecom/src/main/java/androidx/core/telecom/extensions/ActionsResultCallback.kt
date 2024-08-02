/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.extensions

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallException
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
@RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
public class ActionsResultCallback : IActionsResultCallback.Stub() {
    internal lateinit var result: CallControlResult
    internal var errorMessage = "No error occurred"

    internal val waitForActionResultLatch = CountDownLatch(1)

    public companion object {
        private val TAG = ActionsResultCallback::class.simpleName
        internal const val ACTION_RESULT_RESPONSE_TIMEOUT = 1000L
    }

    public suspend fun waitForResponse(): CallControlResult {
        try {
            withTimeout(ACTION_RESULT_RESPONSE_TIMEOUT) {
                // Wait for VOIP app to return the result
                if (
                    waitForActionResultLatch.await(
                        ACTION_RESULT_RESPONSE_TIMEOUT,
                        TimeUnit.MILLISECONDS
                    )
                ) {
                    Log.i(TAG, "waitForResponse: VoIP app returned a result")
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.i(TAG, "waitForResponse: timeout reached")
            result = CallControlResult.Error(CallException.ERROR_OPERATION_TIMED_OUT)
        }
        return result
    }

    override fun onFailure(errorCode: Int, msg: String?) {
        result = CallControlResult.Error(errorCode)
        if (msg != null) {
            errorMessage = msg
            Log.i(TAG, "onFailure: $errorMessage")
        }

        // Todo: define some sort of retry logic that passes the errorCode back to the ICS app in
        //  order to allow them to determine how to handle the failure.

        waitForActionResultLatch.countDown()
    }

    override fun onSuccess() {
        result = CallControlResult.Success()
        waitForActionResultLatch.countDown()
    }
}
