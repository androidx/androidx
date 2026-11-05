/*
 * Copyright 2022 The Android Open Source Project
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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform

import android.health.connect.HealthConnectException
import android.os.RemoteException
import androidx.annotation.RequiresApi
import androidx.health.connect.client.exceptions.RateLimitExceededException
import androidx.health.connect.client.exceptions.RecordTooLargeException
import androidx.health.connect.client.exceptions.RequestTooLargeException
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

// Error codes added in HealthConnectException (b/429109397) not yet in compileSdk prebuilt.
internal const val ERROR_REQUEST_TOO_LARGE = 10
internal const val ERROR_RECORD_TOO_LARGE = 11

// Fallback message prefixes from platform RateLimiter.java. Used when the platform returns
// ERROR_RATE_LIMIT_EXCEEDED (7) for oversized payloads (e.g. on older Mainline module trains or
// when the caller's targetSdkVersion does not enable RETURN_PAYLOAD_SIZE_ERROR_CODES), ensuring
// oversized payloads are not misclassified as RateLimitExceededException.
private const val CHUNK_SIZE_LIMIT_MESSAGE = "Records chunk size exceeded the max chunk limit"
private const val RECORD_SIZE_LIMIT_MESSAGE = "Record size exceeded the single record size limit"

/** Converts exception returned by the platform to one of standard exception class hierarchy. */
internal fun HealthConnectException.toKtException(): Exception {
    return when (errorCode) {
        HealthConnectException.ERROR_IO -> IOException(this)
        HealthConnectException.ERROR_REMOTE -> RemoteException(message)
        HealthConnectException.ERROR_SECURITY -> SecurityException(this)
        HealthConnectException.ERROR_INVALID_ARGUMENT -> IllegalArgumentException(this)
        ERROR_REQUEST_TOO_LARGE -> RequestTooLargeException(message, this)
        ERROR_RECORD_TOO_LARGE -> RecordTooLargeException(message, this)
        HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED -> {
            val msg = message
            when {
                msg != null && msg.contains(CHUNK_SIZE_LIMIT_MESSAGE) ->
                    RequestTooLargeException(msg, this)
                msg != null && msg.contains(RECORD_SIZE_LIMIT_MESSAGE) ->
                    RecordTooLargeException(msg, this)
                else -> RateLimitExceededException(msg, this)
            }
        }
        else -> IllegalStateException(this)
    }
}
