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

// Parcelize object is testing internal implementation of datastore-core library
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.datastore.testapp.multiprocess.ipcActions

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.datastore.testapp.twoWayIpc.InterProcessCompletable
import androidx.datastore.testapp.twoWayIpc.IpcAction
import androidx.datastore.testapp.twoWayIpc.IpcUnit
import androidx.datastore.testapp.twoWayIpc.TwoWayIpcSubject
import kotlinx.parcelize.Parcelize

@SuppressLint("BanParcelableUsage")
@Parcelize
internal class SetTextAction(
    private val value: String,
    private val transactionStartedLatch: InterProcessCompletable<IpcUnit>? = null,
    private val commitTransactionLatch: InterProcessCompletable<IpcUnit>? = null,
    private val actionStartedLatch: InterProcessCompletable<IpcUnit>? = null,
) : IpcAction<IpcUnit>(), Parcelable {
    override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): IpcUnit {
        actionStartedLatch?.complete(subject, IpcUnit)
        subject.datastore.updateData {
            transactionStartedLatch?.complete(subject, IpcUnit)
            commitTransactionLatch?.await(subject)
            it.toBuilder().setText(value).build()
        }
        return IpcUnit
    }
}
