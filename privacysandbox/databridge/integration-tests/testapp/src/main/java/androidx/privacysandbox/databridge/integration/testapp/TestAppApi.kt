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

package androidx.privacysandbox.databridge.integration.testapp

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.databridge.client.DataBridgeClient
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.integration.testsdk.TestSdk
import androidx.privacysandbox.databridge.integration.testsdk.TestSdkFactory.wrapToTestSdk
import androidx.privacysandbox.databridge.integration.testutils.fromKeyValue
import androidx.privacysandbox.databridge.integration.testutils.toKeyResultPair
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat

class TestAppApi(appContext: Context) {

    /** Name of the Test SDK to be loaded. */
    private val TEST_SDK_NAME = "androidx.privacysandbox.databridge.integration.sdk"

    private val sdkSandboxManager = SdkSandboxManagerCompat.from(appContext)
    private val dataBridgeClient = DataBridgeClient.getInstance(appContext)
    internal var sdk: TestSdk? = null

    suspend fun loadTestSdk() {
        if (sdk != null) return
        val sandboxedSdk = sdkSandboxManager.loadSdk(TEST_SDK_NAME, Bundle.EMPTY)
        sdk = sandboxedSdk.getInterface()?.let { wrapToTestSdk(it) }
    }

    fun unloadTestSdk() = sdkSandboxManager.unloadSdk(TEST_SDK_NAME)

    suspend fun getValuesFromApp(keys: Set<Key>): Map<Key, Result<Any?>> {
        return dataBridgeClient.getValues(keys)
    }

    suspend fun getValuesFromSdk(keys: Set<Key>): Map<Key, Result<Any?>> {
        val (keyNames, keyTypes) = keys.map { it.name to it.type.toString() }.unzip()
        val result = sdk!!.getValues(keyNames, keyTypes)
        return result.associate { it.toKeyResultPair() }
    }

    suspend fun setValuesFromApp(keyValueMap: Map<Key, Any?>) {
        dataBridgeClient.setValues(keyValueMap)
    }

    suspend fun setValuesFromSdk(keyValueMap: Map<Key, Any?>) {
        val keyValueData = keyValueMap.map { Bundle().fromKeyValue(it.key, it.value) }
        sdk!!.setValues(
            keyValueMap.keys.map { it.name },
            keyValueMap.keys.map { it.type.toString() },
            keyValueData,
        )
    }

    suspend fun removeValuesFromApp(keys: Set<Key>) {
        return dataBridgeClient.removeValues(keys)
    }

    suspend fun removeValuesFromSdk(keys: Set<Key>) {
        val (keyNames, keyTypes) = keys.map { it.name to it.type.toString() }.unzip()
        sdk!!.removeValues(keyNames, keyTypes)
    }
}
