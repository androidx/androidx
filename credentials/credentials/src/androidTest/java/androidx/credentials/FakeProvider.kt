/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.credentials

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import java.util.concurrent.Executor

class FakeProvider(private val success: Boolean) : CredentialProvider {
    override fun onGetCredential(
        context: Context,
        request: GetCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>
    ) {
        TODO("Not yet implemented")
    }

    override fun onCreateCredential(
        context: Context,
        request: CreateCredentialRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>
    ) {
        TODO("Not yet implemented")
    }

    override fun isAvailableOnDevice(): Boolean {
        return success
    }

    override fun onClearCredential(
        request: ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal?,
        executor: Executor,
        callback: CredentialManagerCallback<Void?, ClearCredentialException>
    ) {
        TODO("Not yet implemented")
    }
}
