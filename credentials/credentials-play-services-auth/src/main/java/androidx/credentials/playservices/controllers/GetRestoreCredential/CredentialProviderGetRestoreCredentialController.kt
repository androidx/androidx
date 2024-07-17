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

package androidx.credentials.playservices.controllers.GetRestoreCredential

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.Credential
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetRestoreCredentialOption
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.blockstore.restorecredential.GetRestoreCredentialRequest
import com.google.android.gms.auth.blockstore.restorecredential.GetRestoreCredentialResponse
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredential
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredentialStatusCodes
import com.google.android.gms.common.api.ApiException
import java.util.concurrent.Executor

/** A controller to handle the GetRestoreCredential flow with play services. */
internal class CredentialProviderGetRestoreCredentialController(private val context: Context) :
    CredentialProviderController<
        GetCredentialRequest,
        GetRestoreCredentialRequest,
        GetRestoreCredentialResponse,
        GetCredentialResponse,
        GetCredentialException
    >(context) {

    override fun invokePlayServices(
        request: GetCredentialRequest,
        callback: CredentialManagerCallback<GetCredentialResponse, GetCredentialException>,
        executor: Executor,
        cancellationSignal: CancellationSignal?
    ) {
        if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
            return
        }

        val convertedRequest = this.convertRequestToPlayServices(request)
        RestoreCredential.getRestoreCredentialClient(context)
            .getRestoreCredential(convertedRequest)
            .addOnSuccessListener {
                try {
                    val response = this.convertResponseToCredentialManager(it)
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute { callback.onResult(response) }
                    }
                } catch (e: Exception) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute {
                            callback.onError(
                                if (e is NoCredentialException) e
                                else GetCredentialUnknownException(e.message)
                            )
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                var getException: GetCredentialException =
                    GetCredentialUnknownException(
                        "Get restore credential failed for unknown reason, failure: ${e.message}"
                    )
                if (e is ApiException) {
                    when (e.statusCode) {
                        RestoreCredentialStatusCodes.RESTORE_CREDENTIAL_INTERNAL_FAILURE -> {
                            getException =
                                GetCredentialUnknownException(
                                    "The restore credential internal service had a failure, failure: ${e.message}"
                                )
                        }
                        else -> {
                            getException =
                                GetCredentialUnknownException(
                                    "The restore credential service failed with unsupported status code, failure: ${e.message}, " +
                                        "status code: ${e.statusCode}"
                                )
                        }
                    }
                }
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute { callback.onError(getException) }
                }
            }
    }

    public override fun convertRequestToPlayServices(
        request: GetCredentialRequest
    ): GetRestoreCredentialRequest {
        lateinit var credentialOption: GetRestoreCredentialOption
        for (option in request.credentialOptions) {
            if (option is GetRestoreCredentialOption) {
                // there should be a single restore credential option
                credentialOption = option
                break
            }
        }
        return GetRestoreCredentialRequest(credentialOption.requestData)
    }

    public override fun convertResponseToCredentialManager(
        response: GetRestoreCredentialResponse
    ): GetCredentialResponse {
        return GetCredentialResponse(
            Credential.createFrom(
                androidx.credentials.RestoreCredential.TYPE_RESTORE_CREDENTIAL,
                response.responseBundle
            )
        )
    }
}
