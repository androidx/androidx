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

package androidx.credentials.playservices.controllers.CreateRestoreCredential

import android.content.Context
import android.os.CancellationSignal
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreateRestoreCredentialRequest
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.domerrors.DataError
import androidx.credentials.exceptions.restorecredential.CreateRestoreCredentialDomException
import androidx.credentials.exceptions.restorecredential.E2eeUnavailableException
import androidx.credentials.playservices.CredentialProviderPlayServicesImpl
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.auth.blockstore.restorecredential.CreateRestoreCredentialResponse
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredential
import com.google.android.gms.auth.blockstore.restorecredential.RestoreCredentialStatusCodes
import com.google.android.gms.common.api.ApiException
import java.util.concurrent.Executor

/** A controller to handle the CreateRestoreCredential flow with play services. */
internal class CredentialProviderCreateRestoreCredentialController(private val context: Context) :
    CredentialProviderController<
        CreateRestoreCredentialRequest,
        com.google.android.gms.auth.blockstore.restorecredential.CreateRestoreCredentialRequest,
        CreateRestoreCredentialResponse,
        CreateCredentialResponse,
        CreateCredentialException
    >(context) {

    override fun invokePlayServices(
        request: CreateRestoreCredentialRequest,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
        executor: Executor,
        cancellationSignal: CancellationSignal?
    ) {
        if (CredentialProviderPlayServicesImpl.cancellationReviewer(cancellationSignal)) {
            return
        }

        val convertedRequest = this.convertRequestToPlayServices(request)
        RestoreCredential.getRestoreCredentialClient(context)
            .createRestoreCredential(convertedRequest)
            .addOnSuccessListener {
                try {
                    val response = this.convertResponseToCredentialManager(it)
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute { callback.onResult(response) }
                    }
                } catch (e: Exception) {
                    cancelOrCallbackExceptionOrResult(cancellationSignal) {
                        executor.execute {
                            callback.onError(CreateCredentialUnknownException(e.message))
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                var createException: CreateCredentialException =
                    CreateCredentialUnknownException(
                        "Create restore credential failed for unknown reason, failure: ${e.message}"
                    )
                if (e is ApiException) {
                    when (e.statusCode) {
                        RestoreCredentialStatusCodes.RESTORE_CREDENTIAL_E2EE_UNAVAILABLE -> {
                            createException =
                                E2eeUnavailableException(
                                    "E2ee is not available on the device. Check whether the backup and screen lock are enabled."
                                )
                        }
                        RestoreCredentialStatusCodes.RESTORE_CREDENTIAL_FIDO_FAILURE -> {
                            createException =
                                CreateRestoreCredentialDomException(
                                    DataError(),
                                    "The request did not match the fido spec, failure: ${e.message}"
                                )
                        }
                        RestoreCredentialStatusCodes.RESTORE_CREDENTIAL_INTERNAL_FAILURE -> {
                            createException =
                                CreateCredentialUnknownException(
                                    "The restore credential internal service had a failure, failure: ${e.message}"
                                )
                        }
                        else -> {
                            createException =
                                CreateCredentialUnknownException(
                                    "The restore credential service failed with unsupported status code, failure: ${e.message}, " +
                                        "status code: ${e.statusCode}"
                                )
                        }
                    }
                }
                cancelOrCallbackExceptionOrResult(cancellationSignal) {
                    executor.execute { callback.onError(createException) }
                }
            }
    }

    public override fun convertRequestToPlayServices(
        request: CreateRestoreCredentialRequest
    ): com.google.android.gms.auth.blockstore.restorecredential.CreateRestoreCredentialRequest {
        return com.google.android.gms.auth.blockstore.restorecredential
            .CreateRestoreCredentialRequest(request.credentialData)
    }

    public override fun convertResponseToCredentialManager(
        response: CreateRestoreCredentialResponse
    ): CreateCredentialResponse {
        return androidx.credentials.CreateRestoreCredentialResponse.createFrom(
            response.responseBundle
        )
    }
}
