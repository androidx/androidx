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

package androidx.privacysandbox.databridge.integration.testsdk

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.integration.testutils.fromKeyResult
import androidx.privacysandbox.databridge.integration.testutils.toKeyValuePair
import androidx.privacysandbox.databridge.sdkprovider.DataBridgeSdkProvider

class TestSdkImpl(context: Context) : TestSdk {
    private val dataBridgeSdkProvider = DataBridgeSdkProvider.getInstance(context)

    override suspend fun getValues(keyNames: List<String>, keyTypes: List<String>): List<Bundle> {
        val keyValueMap = dataBridgeSdkProvider.getValues(getKeyList(keyNames, keyTypes).toSet())
        return keyValueMap.map { keyValuePair ->
            Bundle().fromKeyResult(keyValuePair.key, keyValuePair.value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun setValues(
        keyNames: List<String>,
        keyTypes: List<String>,
        values: List<Bundle>,
    ) {

        val keyNameToKeyType = keyNames.zip(keyTypes)
        val keyValue =
            values.zip(keyNameToKeyType).associate { (value, key) ->
                value.toKeyValuePair(key.first, key.second)
            }
        dataBridgeSdkProvider.setValues(keyValue)
    }

    override suspend fun removeValues(keyNames: List<String>, keyTypes: List<String>) {
        dataBridgeSdkProvider.removeValues(getKeyList(keyNames, keyTypes).toSet())
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
}
