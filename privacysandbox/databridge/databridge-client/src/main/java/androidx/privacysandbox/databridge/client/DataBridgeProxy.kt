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

import androidx.annotation.RestrictTo
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.aidl.IDataBridgeProxy
import androidx.privacysandbox.databridge.core.aidl.IGetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ISetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal
import java.lang.IllegalStateException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DataBridgeProxy(val dataBridgeClient: DataBridgeClient) : IDataBridgeProxy.Stub() {
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
            var keys = getKeyList(keyNames, keyTypes)
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
