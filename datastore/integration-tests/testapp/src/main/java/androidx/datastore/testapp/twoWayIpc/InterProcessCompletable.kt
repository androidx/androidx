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

package androidx.datastore.testapp.twoWayIpc

import android.annotation.SuppressLint
import android.os.Parcelable
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.parcelize.Parcelize

/** A [Parcelable] [CompletableDeferred] implementation that can be shared across processes. */
@SuppressLint("BanParcelableUsage")
@Parcelize
internal class InterProcessCompletable<T : Parcelable>(
    private val key: String = UUID.randomUUID().toString(),
) : Parcelable {
    suspend fun complete(subject: TwoWayIpcSubject, value: T) {
        IpcLogger.log("will complete $key")
        subject.crossProcessCompletableController.complete(key, value)
    }

    suspend fun await(subject: TwoWayIpcSubject): T {
        return subject.crossProcessCompletableController.obtainInCurrentProcess<T>(key).await()
    }

    override fun toString(): String {
        return "completable[$key]"
    }
}

/**
 * Manages [InterProcessCompletable] instances across processes. When an instance is completed in
 * one process, its value will be dispatched to the other process as well.
 */
private class CrossProcessCompletableController(private val subject: TwoWayIpcSubject) {
    private val completables = mutableMapOf<String, CompletableDeferred<*>>()

    private fun <T> get(key: String) =
        synchronized(this) {
            @Suppress("UNCHECKED_CAST")
            completables.getOrPut(key) { CompletableDeferred<T>() } as CompletableDeferred<T>
        }

    private fun <T : Parcelable> completeInCurrentProcess(key: String, value: T) {
        IpcLogger.log("complete internal $key")
        get<T>(key).complete(value)
    }

    fun <T : Parcelable> obtainInCurrentProcess(key: String): CompletableDeferred<T> {
        return get(key)
    }

    suspend fun <T : Parcelable> complete(key: String, value: T) {
        completeInCurrentProcess(key, value)
        IpcLogger.log("will complete $key in remote process")
        subject.invokeInRemoteProcess(CompleteCompletableAction(key = key, value = value))
        IpcLogger.log("completed $key in remote process")
    }

    @Parcelize
    private data class CompleteCompletableAction<T : Parcelable>(
        private val key: String,
        private val value: T
    ) : IpcAction<IpcUnit>() {
        override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
            subject.crossProcessCompletableController.completeInCurrentProcess(key, value)
            return IpcUnit
        }
    }
}

private val COMPLETABLE_CONTROLLER_KEY =
    CompositeServiceSubjectModel.Key<CrossProcessCompletableController>()
private val TwoWayIpcSubject.crossProcessCompletableController: CrossProcessCompletableController
    get() {
        return data.getOrPut(COMPLETABLE_CONTROLLER_KEY) { CrossProcessCompletableController(this) }
    }
