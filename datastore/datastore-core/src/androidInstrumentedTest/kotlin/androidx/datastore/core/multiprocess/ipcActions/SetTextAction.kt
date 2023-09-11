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

package androidx.datastore.core.multiprocess.ipcActions

import android.os.Parcelable
import androidx.datastore.core.twoWayIpc.InterProcessCompletable
import androidx.datastore.core.twoWayIpc.IpcAction
import androidx.datastore.core.twoWayIpc.IpcUnit
import androidx.datastore.core.twoWayIpc.TwoWayIpcSubject
import kotlinx.parcelize.Parcelize

@Parcelize
internal class SetTextAction(
    private val value: String,
    private val transactionStartedLatch: InterProcessCompletable<IpcUnit>? = null,
    private val commitTransactionLatch: InterProcessCompletable<IpcUnit>? = null,
) : IpcAction<IpcUnit>(), Parcelable {
    override suspend fun invokeInRemoteProcess(
        subject: TwoWayIpcSubject
    ): IpcUnit {
        subject.datastore.updateData {
            transactionStartedLatch?.complete(subject, IpcUnit)
            commitTransactionLatch?.await(subject)
            it.toBuilder().setText(value).build()
        }
        return IpcUnit
    }
}
