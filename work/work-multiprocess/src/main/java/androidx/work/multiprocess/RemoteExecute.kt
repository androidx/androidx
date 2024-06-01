/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.work.multiprocess

import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.IInterface
import androidx.concurrent.futures.SuspendToFutureAdapter.launchFuture
import androidx.concurrent.futures.await
import androidx.work.Logger
import androidx.work.multiprocess.ListenableWorkerImplClient.TAG
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher

internal fun <T : IInterface> execute(
    executor: Executor,
    iInterface: ListenableFuture<T>,
    dispatcher: RemoteDispatcher<T>,
): ListenableFuture<ByteArray> {
    return launchFuture(executor.asCoroutineDispatcher() + Job(), launchUndispatched = false) {
        val worker =
            try {
                iInterface.await()
            } catch (throwable: Throwable) {
                if (throwable !is CancellationException) {
                    Logger.get().error(TAG, "Unable to bind to service", throwable)
                }
                throw throwable
            }
        execute(worker, dispatcher)
    }
}

internal suspend fun <T : IInterface> execute(
    iInterface: T,
    dispatcher: RemoteDispatcher<T>
): ByteArray {
    var deathRecipient: DeathRecipient? = null
    val binder = iInterface.asBinder()
    return try {
        suspendCoroutine { continuation ->
            val localRecipient = DeathRecipient {
                continuation.resumeWithException(RuntimeException("Binder died"))
            }
            deathRecipient = localRecipient
            binder.linkToDeath(localRecipient, 0)
            dispatcher.execute(
                iInterface,
                object : IWorkManagerImplCallback.Stub() {

                    override fun onSuccess(response: ByteArray) = continuation.resume(response)

                    override fun onFailure(error: String?) =
                        continuation.resumeWithException(RuntimeException(error))
                }
            )
        }
    } catch (throwable: Throwable) {
        if (throwable !is CancellationException) {
            Logger.get().error(TAG, "Unable to execute", throwable)
        }
        throw throwable
    } finally {
        deathRecipient?.let { binder.unlinkToDeathSafely(it) }
    }
}

private fun IBinder.unlinkToDeathSafely(recipient: DeathRecipient) {
    try {
        unlinkToDeath(recipient, 0)
    } catch (_: NoSuchElementException) {
        // Sometimes trying to link a death recipient to a binder itself might fail
        // because the designated process might have crashed.
        // In such cases trying to unlink will fail because there may not be a registered
        // recipient
    }
}
