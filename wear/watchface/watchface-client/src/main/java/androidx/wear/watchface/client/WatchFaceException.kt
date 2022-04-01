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

package androidx.wear.watchface.client

import android.os.DeadObjectException
import android.os.RemoteException
import android.os.TransactionTooLargeException
import androidx.annotation.IntDef

/**
 * Why the remote watch face query failed.
 * @hide
 **/
@Retention(AnnotationRetention.SOURCE)
@IntDef(
    WatchFaceException.WATCHFACE_DIED,
    WatchFaceException.TRANSACTION_TOO_LARGE,
    WatchFaceException.UNKNOWN
)
annotation class WatchFaceExceptionReason

/**
 * The watch face threw an exception while trying to service the request.
 *
 * @property reason The [WatchFaceExceptionReason] for the exception.
 */
public class WatchFaceException(
    e: Exception,
    @WatchFaceExceptionReason val reason: Int
) : Exception(e) {

    companion object {
        /**
         * The watchface process died. Connecting again might work, but this isn't guaranteed.
         */
        const val WATCHFACE_DIED = 1

        /**
         * The watchface tried to send us too much data. Currently the limit on binder
         * transactions is 1mb. See [TransactionTooLargeException] for more details.
         */
        const val TRANSACTION_TOO_LARGE = 2

        /**
         * The watch face threw an exception, typically during initialization. Depending on the
         * nature of the problem this might be a transient issue or it might occur every time
         * for that particular watch face.
         */
        const val UNKNOWN = 3
    }
}

@Throws(WatchFaceException::class)
internal fun <R> callRemote(task: () -> R): R =
    try {
        task()
    } catch (e: DeadObjectException) {
        throw WatchFaceException(e, WatchFaceException.WATCHFACE_DIED)
    } catch (e: TransactionTooLargeException) {
        throw WatchFaceException(e, WatchFaceException.TRANSACTION_TOO_LARGE)
    } catch (e: RemoteException) {
        throw WatchFaceException(e, WatchFaceException.UNKNOWN)
    }
