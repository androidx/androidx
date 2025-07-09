/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.privacysandbox.databridge.client

import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.KeyUpdateCallback
import androidx.privacysandbox.databridge.core.aidl.IDataBridgeProxy
import androidx.privacysandbox.databridge.core.aidl.IGetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.IKeyUpdateInternalCallback
import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ISetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import java.lang.IllegalStateException
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DataBridgeProxy(val dataBridgeClient: DataBridgeClient) : IDataBridgeProxy.Stub() {
    private val lock = Any()
    @GuardedBy("lock") private val keyToUuidMap = mutableMapOf<Key, MutableSet<String>>()

    @GuardedBy("lock")
    private val uuidToKeyUpdateInternalCallbackMap =
        mutableMapOf<String, IKeyUpdateInternalCallback>()

    private val keyUpdateCallback: KeyUpdateCallback =
        object : KeyUpdateCallback {
            override fun onKeyUpdated(key: Key, value: Any?) {
                val data = ValueInternal(key.type.toString(), value == null, value)
                synchronized(lock) {
                    keyToUuidMap[key]?.forEach { uuid ->
                        uuidToKeyUpdateInternalCallbackMap[uuid]?.onKeyUpdated(key.name, data)
                    }
                }
            }
        }
    private val executor: Executor = Executors.newCachedThreadPool()

    override fun getValues(
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IGetValuesResultCallback,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val keys = getKeyList(keyNames, keyTypes)
            val res: Map<Key, Result<Any?>> = dataBridgeClient.getValues(keys.toSet())

            val resultInternalList =
                res.map { (key, result) ->
                    if (result.isFailure) {
                        processFailedResultEntry(key, result)
                    } else {
                        processSuccessResultEntry(key, result)
                    }
                }
            callback.getValuesResult(resultInternalList)
        }
    }

    override fun setValues(
        keyNames: List<String>,
        data: List<ValueInternal>,
        callback: ISetValuesResultCallback,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val keys = getKeyList(keyNames, data.map { it.type })
            val keyValueMap =
                keys
                    .zip(data) { key, valueInternal -> key to valueInternal.value }
                    .associate { it.first to it.second }
            try {
                dataBridgeClient.setValues(keyValueMap)
                callback.setValuesResult(/* exceptionName= */ null, /* exceptionMessage= */ null)
            } catch (exception: Exception) {
                callback.setValuesResult(
                    /*exceptionName =*/ exception::class.java.canonicalName,
                    /*exceptionMessage =*/ exception.message,
                )
            }
        }
    }

    override fun removeValues(
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IRemoveValuesResultCallback,
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val keys = getKeyList(keyNames, keyTypes)
            try {
                dataBridgeClient.removeValues(keys.toSet())
                callback.removeValuesResult(/* exceptionName= */ null, /* exceptionMessage= */ null)
            } catch (exception: Exception) {
                callback.removeValuesResult(
                    /*exceptionName =*/ exception::class.java.canonicalName,
                    /*exceptionMessage =*/ exception.message,
                )
            }
        }
    }

    override fun addKeysForUpdates(
        uuid: String,
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IKeyUpdateInternalCallback,
    ) {
        val keys = getKeyList(keyNames, keyTypes).toSet()
        synchronized(lock) {
            if (!uuidToKeyUpdateInternalCallbackMap.containsKey(uuid)) {
                uuidToKeyUpdateInternalCallbackMap[uuid] = callback
            }
            keys.forEach { key -> keyToUuidMap.getOrPut(key) { mutableSetOf() }.add(uuid) }
        }

        // This call can be made with different keys with the same callback multiple times
        dataBridgeClient.registerKeyUpdateCallback(keys, executor, keyUpdateCallback)
    }

    override fun removeKeysFromUpdates(
        uuid: String,
        keyNames: List<String>,
        keyTypes: List<String>,
        unregisterCallback: Boolean,
    ) {
        val keys = getKeyList(keyNames, keyTypes)
        synchronized(lock) {
            if (unregisterCallback) {
                uuidToKeyUpdateInternalCallbackMap.remove(uuid)
            }
            keys.forEach { key -> { keyToUuidMap[key]?.remove(uuid) } }
        }
    }

    private fun processSuccessResultEntry(key: Key, result: Result<Any?>): ResultInternal {
        val value = result.getOrNull()
        return ResultInternal(
            keyName = key.name,
            exceptionName = null,
            exceptionMessage = null,
            valueInternal =
                ValueInternal(
                    type = key.type.toString(),
                    isValueNull = value == null,
                    value = value,
                ),
        )
    }

    private fun getKeyList(keyNames: List<String>, keyTypes: List<String>): List<Key> {
        return keyNames.zip(keyTypes).map { (name, typeString) ->
            when (typeString) {
                "INT" -> Key.createIntKey(name)
                "LONG" -> Key.createLongKey(name)
                "FLOAT" -> Key.createFloatKey(name)
                "DOUBLE" -> Key.createDoubleKey(name)
                "BOOLEAN" -> Key.createBooleanKey(name)
                "STRING" -> Key.createStringKey(name)
                "STRING_SET" -> Key.createStringSetKey(name)
                "BYTE_ARRAY" -> Key.createByteArrayKey(name)
                else -> throw IllegalStateException("$typeString is not a valid key type")
            }
        }
    }

    private fun processFailedResultEntry(key: Key, result: Result<Any?>): ResultInternal {
        val exception: Throwable? = result.exceptionOrNull()
        return ResultInternal(
            keyName = key.name,
            exceptionName = exception!!::class.java.canonicalName,
            exceptionMessage = exception.message,
            valueInternal = null,
        )
    }
}
