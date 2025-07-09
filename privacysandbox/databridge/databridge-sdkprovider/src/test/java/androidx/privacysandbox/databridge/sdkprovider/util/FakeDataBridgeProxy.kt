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

package androidx.privacysandbox.databridge.sdkprovider.util

import androidx.privacysandbox.databridge.core.aidl.IDataBridgeProxy
import androidx.privacysandbox.databridge.core.aidl.IGetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.IKeyUpdateInternalCallback
import androidx.privacysandbox.databridge.core.aidl.IRemoveValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ISetValuesResultCallback
import androidx.privacysandbox.databridge.core.aidl.ResultInternal
import androidx.privacysandbox.databridge.core.aidl.ValueInternal

class FakeDataBridgeProxy(
    private val shouldThrowException: Boolean = false,
    private val exceptionName: String? = null,
    private val exceptionMessage: String? = null,
    private val resultInternals: List<ResultInternal> = emptyList(),
) : IDataBridgeProxy.Stub() {

    var keyUpdateInternalCallback: IKeyUpdateInternalCallback? = null

    override fun getValues(
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IGetValuesResultCallback,
    ) {
        callback.getValuesResult(resultInternals)
    }

    override fun setValues(
        keyNames: List<String>,
        data: List<ValueInternal>,
        callback: ISetValuesResultCallback,
    ) {
        if (shouldThrowException) {
            callback.setValuesResult(exceptionName, exceptionMessage)
        } else {
            callback.setValuesResult(/* exceptionName= */ null, /* exceptionMessage= */ null)
        }
    }

    override fun removeValues(
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IRemoveValuesResultCallback,
    ) {
        if (shouldThrowException) {
            callback.removeValuesResult(exceptionName, exceptionMessage)
        } else {
            callback.removeValuesResult(/* exceptionName= */ null, /* exceptionMessage= */ null)
        }
    }

    override fun addKeysForUpdates(
        uuid: String,
        keyNames: List<String>,
        keyTypes: List<String>,
        callback: IKeyUpdateInternalCallback,
    ) {
        // TODO(b/423805466) Implement this as part of DataBridgeSdkProvider
    }

    override fun removeKeysFromUpdates(
        uuid: String,
        keyNames: List<String>,
        keyTypes: List<String>,
        unregisterCallback: Boolean,
    ) {
        // TODO(b/423805466) Implement this as part of DataBridgeSdkProvider
    }
}
