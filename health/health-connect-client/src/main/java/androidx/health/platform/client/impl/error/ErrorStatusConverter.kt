/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.impl.error

import android.os.Build
import android.os.RemoteException
import androidx.health.platform.client.error.ErrorCode
import androidx.health.platform.client.error.ErrorStatus
import java.io.IOException
import java.lang.IllegalArgumentException

val errorCodeExceptionMap =
    mapOf(
        ErrorCode.PROVIDER_NOT_INSTALLED to java.lang.UnsupportedOperationException::class,
        ErrorCode.PROVIDER_NOT_ENABLED to java.lang.UnsupportedOperationException::class,
        ErrorCode.PROVIDER_NEEDS_UPDATE to java.lang.UnsupportedOperationException::class,
        ErrorCode.NO_PERMISSION to SecurityException::class,
        ErrorCode.INVALID_OWNERSHIP to SecurityException::class,
        ErrorCode.NOT_ALLOWED to SecurityException::class,
        ErrorCode.EMPTY_PERMISSION_LIST to IllegalArgumentException::class,
        ErrorCode.PERMISSION_NOT_DECLARED to SecurityException::class,
        ErrorCode.INVALID_PERMISSION_RATIONALE_DECLARATION to SecurityException::class,
        ErrorCode.INVALID_UID to RemoteException::class,
        ErrorCode.DATABASE_ERROR to IOException::class,
        ErrorCode.INTERNAL_ERROR to RemoteException::class,
        ErrorCode.CHANGES_TOKEN_OUTDATED to RemoteException::class
    )

@Suppress("ObsoleteSdkInt") // We want to target lower down to 14 in the future.
fun ErrorStatus.toException(): Exception {
    errorCodeExceptionMap[this.errorCode]?.let {
        return when (it) {
            SecurityException::class -> SecurityException(this.errorMessage)
            RemoteException::class -> {
                if (Build.VERSION.SDK_INT > 24) RemoteException(this.errorMessage)
                else RemoteException()
            }
            IllegalArgumentException::class -> IllegalArgumentException(this.errorMessage)
            IOException::class -> IOException(this.errorMessage)
            else -> UnsupportedOperationException(this.errorMessage)
        }
    }
    return UnsupportedOperationException(this.errorMessage)
}
