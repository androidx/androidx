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

import android.os.Bundle
import androidx.privacysandbox.tools.PrivacySandboxService

@PrivacySandboxService
interface TestSdk {
    // This method returns a Bundle which contains information about the key and the values for the
    // input
    // The Bundle object contains the following:
    // "keyName" -> Name of the key
    // "keyType" -> Type of the key
    // "isSuccess" -> Boolean which specifies if fetching value for the key was a success or not
    // "value" -> Value for the key passed as input
    // "isValueNull" -> Boolean which specifies if the value is null
    // "exceptionName" -> In case of failure, the exception name
    // "exceptionMessage" -> In case of failure, the exception message
    suspend fun getValues(keyNames: List<String>, keyTypes: List<String>): List<Bundle>

    suspend fun setValues(keyNames: List<String>, keyTypes: List<String>, values: List<Bundle>)

    suspend fun removeValues(keyNames: List<String>, keyTypes: List<String>)
}
