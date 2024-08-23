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

import android.os.Bundle
import android.os.Parcelable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.CoroutineScope

/**
 * A class that represents a test subject for DataStore multi-process tests. Each test subject is
 * given a [datastoreScope] as well as a [data] so they can keep state.
 *
 * Subjects execute [IpcAction]s which contain the actual test logic.
 */
internal class TwoWayIpcSubject(val datastoreScope: CoroutineScope) {
    val bus: TwoWayIpcBus =
        TwoWayIpcBus(executionScope = datastoreScope, handler = this::handleIncomingAction)
    val data = CompositeServiceSubjectModel()

    private suspend fun handleIncomingAction(bundle: Bundle?): Bundle {
        @Suppress("DEPRECATION") val ipcAction = bundle?.getParcelable<IpcAction<*>>(KEY_ACTION)
        checkNotNull(ipcAction) { "no ipc action in bundle" }
        IpcLogger.log("executing action: ${ipcAction::class.java}")

        val response = ipcAction.invokeInRemoteProcess(this)
        IpcLogger.log("executed action: ${ipcAction::class.java}")
        return Bundle().also { it.putParcelable(KEY_RESPONSE, response) }
    }

    suspend fun <T : Parcelable> invokeInRemoteProcess(action: IpcAction<T>): T {
        val response = bus.sendMessage(Bundle().also { it.putParcelable(KEY_ACTION, action) })
        checkNotNull(response) { "No response received for $action" }
        @Suppress("DEPRECATION")
        return response.getParcelable(KEY_RESPONSE)
            ?: error("didn't get a response from remote process")
    }

    companion object {
        private const val KEY_ACTION = "ipc_action"
        private const val KEY_RESPONSE = "ipc_response"
    }
}

/**
 * A property delegate to stash values into the [CompositeServiceSubjectModel] of a
 * [TwoWayIpcSubject].
 */
internal class SubjectReadWriteProperty<T>(private val key: CompositeServiceSubjectModel.Key<T>) :
    ReadWriteProperty<TwoWayIpcSubject, T> {
    override fun getValue(thisRef: TwoWayIpcSubject, property: KProperty<*>): T {
        return thisRef.data[key]
    }

    override fun setValue(thisRef: TwoWayIpcSubject, property: KProperty<*>, value: T) {
        thisRef.data[key] = value
    }
}
