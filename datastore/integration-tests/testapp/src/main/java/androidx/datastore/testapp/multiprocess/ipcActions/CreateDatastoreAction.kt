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
import androidx.datastore.core.CorruptionHandler
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreImpl
import androidx.datastore.core.FileStorage
import androidx.datastore.core.MultiProcessCoordinator
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.NoOpCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.testapp.ProtoOkioSerializer
import androidx.datastore.testapp.ProtoSerializer
import androidx.datastore.testapp.twoWayIpc.CompositeServiceSubjectModel
import androidx.datastore.testapp.twoWayIpc.IpcAction
import androidx.datastore.testapp.twoWayIpc.SubjectReadWriteProperty
import androidx.datastore.testapp.twoWayIpc.TwoWayIpcSubject
import androidx.datastore.testing.TestMessageProto.FooProto
import com.google.protobuf.ExtensionRegistryLite
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.Parcelize
import okio.FileSystem
import okio.Path.Companion.toPath

private val PROTO_SERIALIZER: Serializer<FooProto> =
    ProtoSerializer<FooProto>(
        FooProto.getDefaultInstance(),
        ExtensionRegistryLite.getEmptyRegistry()
    )
private val PROTO_OKIO_SERIALIZER: OkioSerializer<FooProto> =
    ProtoOkioSerializer<FooProto>(
        FooProto.getDefaultInstance(),
        ExtensionRegistryLite.getEmptyRegistry()
    )

internal enum class StorageVariant {
    FILE,
    OKIO
}

/** Creates the same datastore in current process as well as in the other given [subjects]. */
internal suspend fun createMultiProcessTestDatastore(
    filePath: String,
    storageVariant: StorageVariant,
    hostDatastoreScope: CoroutineScope,
    corruptionHandler: Class<out CorruptionHandler<FooProto>>? = null,
    vararg subjects: TwoWayIpcSubject
): DataStore<FooProto> {
    val currentProcessDatastore =
        createDatastore(
            filePath = filePath,
            storageVariant = storageVariant,
            datastoreScope = hostDatastoreScope,
            corruptionHandler = corruptionHandler,
        )
    subjects.forEach {
        it.invokeInRemoteProcess(
            CreateDatastoreAction(
                filePath = filePath,
                storageVariant = storageVariant,
                corruptionHandler = corruptionHandler,
            )
        )
    }
    return currentProcessDatastore
}

private fun createDatastore(
    filePath: String,
    storageVariant: StorageVariant,
    datastoreScope: CoroutineScope,
    corruptionHandler: Class<out CorruptionHandler<FooProto>>?
): DataStoreImpl<FooProto> {
    val file = File(filePath)
    val produceFile = { file }
    val storage =
        if (storageVariant == StorageVariant.FILE) {
            FileStorage(
                PROTO_SERIALIZER,
                { MultiProcessCoordinator(Dispatchers.Default, it) },
                produceFile
            )
        } else {
            OkioStorage(
                FileSystem.SYSTEM,
                PROTO_OKIO_SERIALIZER,
                { path, _ -> MultiProcessCoordinator(Dispatchers.Default, path.toFile()) },
                { file.absolutePath.toPath() }
            )
        }
    val corruptionHandlerInstance =
        corruptionHandler?.getDeclaredConstructor()?.also { it.isAccessible = true }?.newInstance()
            ?: NoOpCorruptionHandler()
    return DataStoreImpl(
        storage = storage,
        scope = datastoreScope,
        corruptionHandler = corruptionHandlerInstance
    )
}

@SuppressLint("BanParcelableUsage")
@Parcelize
private class CreateDatastoreAction(
    private val filePath: String,
    private val storageVariant: StorageVariant,
    private val corruptionHandler: Class<out CorruptionHandler<FooProto>>?
) : IpcAction<CreateDatastoreAction>(), Parcelable {
    override suspend fun invokeInRemoteProcess(subject: TwoWayIpcSubject): CreateDatastoreAction {
        val store =
            createDatastore(filePath, storageVariant, subject.datastoreScope, corruptionHandler)
        subject.datastore = store
        return this
    }
}

private val DATASTORE_KEY = CompositeServiceSubjectModel.Key<DataStoreImpl<FooProto>>()

internal var TwoWayIpcSubject.datastore by SubjectReadWriteProperty(DATASTORE_KEY)
