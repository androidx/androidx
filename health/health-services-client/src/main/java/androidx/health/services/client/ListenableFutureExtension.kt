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

package androidx.health.services.client

import android.os.RemoteException
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture
import kotlin.jvm.Throws

/**
 * Extension function on ListenableFuture performs [ListenableFuture.await] operation and if
 * any exception thrown by the asynchronous API, converts [android.os.RemoteException] into
 * [HealthServicesException]
 *
 * @throws HealthServicesException if remote operation fails
 */
@Throws(HealthServicesException::class)
suspend fun <T> ListenableFuture<T>.awaitWithException(): T {
    val t: T = try {
        await()
    } catch (e: RemoteException) {
        if (e.message != null)
            throw HealthServicesException(e.message!!)
        else
            throw HealthServicesException("An unknown error has occurred")
    }
    return t
}
