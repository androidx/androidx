/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.registry.provider.playservices

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.registry.provider.UiDelegationFulfillmentProvider
import androidx.credentials.registry.provider.UiDelegationFulfillmentService
import com.google.android.gms.common.util.UidVerifier
import com.google.android.gms.identitycredentials.CallingAppInfoParcelable as GmsCallingAppInfoParcelable
import com.google.android.gms.identitycredentials.Credential as GmsCredential
import com.google.android.gms.identitycredentials.GetCredentialRequest as GmsGetCredentialRequest
import com.google.android.gms.identitycredentials.GetCredentialResponse as GmsGetCredentialResponse
import com.google.android.gms.identitycredentials.provider.IDelegatedCredentialService
import com.google.android.gms.identitycredentials.provider.IGetCredentialCallbacks
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UiDelegationFulfillmentProviderPlayServices : UiDelegationFulfillmentProvider {

    override fun getStubImplementation(service: UiDelegationFulfillmentService): IBinder? {
        val binderInterface = ServiceWrapper(service, Handler(Looper.getMainLooper()))
        return binderInterface.asBinder()
    }

    private class ServiceWrapper(
        service: UiDelegationFulfillmentService,
        private val handler: Handler,
    ) : IDelegatedCredentialService.Stub() {

        private val serviceRef: WeakReference<UiDelegationFulfillmentService> =
            WeakReference(service)

        override fun onGetCredentialRequest(
            request: GmsGetCredentialRequest,
            callingAppInfoParcelable: GmsCallingAppInfoParcelable,
            callback: IGetCredentialCallbacks,
        ) {
            val service = serviceRef.get()
            if (service == null) {
                val exception =
                    GetCredentialUnknownException("Service instance is no longer available")
                callback.safeOnFailure(exception.type, exception.message.orEmpty())
                return
            }

            if (!UidVerifier.isGooglePlayServicesUid(service, getCallingUid())) {
                val exception =
                    GetCredentialProviderConfigurationException(
                        "Caller is not Google Play Services"
                    )
                callback.safeOnFailure(exception.type, exception.message.orEmpty())
                return
            }

            handler.post {
                val callingAppInfo = constructCallingAppInfo(service, callingAppInfoParcelable)
                if (callingAppInfo == null) {
                    val exception =
                        GetCredentialUnknownException(
                            "Failed to construct CallingAppInfo for: ${callingAppInfoParcelable.packageName}"
                        )
                    callback.safeOnFailure(exception.type, exception.message.orEmpty())
                    return@post
                }

                val options =
                    request.credentialOptions.map { gmsOption ->
                        CredentialOption.createFrom(
                            gmsOption.type,
                            gmsOption.credentialRetrievalData,
                            gmsOption.candidateQueryData,
                            requireSystemProvider = false,
                            allowedProviders = emptySet(),
                        )
                    }

                val jetpackRequest =
                    ProviderGetCredentialRequest(
                        options,
                        callingAppInfo,
                        biometricPromptResult = null,
                        sourceBundle = request.data,
                    )

                val outcomeReceiver =
                    object : OutcomeReceiverCompat<GetCredentialResponse, GetCredentialException> {
                        override fun onResult(result: GetCredentialResponse) {
                            try {
                                val gmsCredentials =
                                    result.credentials.map { credential ->
                                        GmsCredential(credential.type, credential.data)
                                    }
                                val gmsResponse = GmsGetCredentialResponse(gmsCredentials)
                                callback.safeOnSuccess(gmsResponse)
                            } catch (e: Exception) {
                                val exception =
                                    GetCredentialUnknownException(
                                        e.message ?: "Error building GMS response"
                                    )
                                callback.safeOnFailure(exception.type, exception.message.orEmpty())
                            }
                        }

                        override fun onError(error: GetCredentialException) {
                            callback.safeOnFailure(error.type, error.message.orEmpty())
                        }
                    }

                service.onGetCredentialRequest(jetpackRequest, outcomeReceiver)
            }
        }

        private companion object {
            private const val TAG = "UiDelegationFulfillmentProviderPlayServices"

            private fun constructCallingAppInfo(
                context: Context,
                callingAppInfoParcelable: GmsCallingAppInfoParcelable,
            ): CallingAppInfo? {
                return try {
                    if (Build.VERSION.SDK_INT >= 28) {
                        val packageInfo =
                            context.packageManager.getPackageInfo(
                                callingAppInfoParcelable.packageName,
                                PackageManager.GET_SIGNING_CERTIFICATES,
                            )
                        val signingInfo = packageInfo.signingInfo ?: return null
                        CallingAppInfo.create(
                            callingAppInfoParcelable.packageName,
                            signingInfo,
                            callingAppInfoParcelable.origin,
                        )
                    } else {
                        val signatures =
                            callingAppInfoParcelable.packageCertificates.map { Signature(it) }
                        @Suppress("DEPRECATION")
                        CallingAppInfo.create(
                            callingAppInfoParcelable.packageName,
                            signatures,
                            callingAppInfoParcelable.origin,
                        )
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }

            private fun IGetCredentialCallbacks.safeOnSuccess(response: GmsGetCredentialResponse) {
                try {
                    onSuccess(response)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver onSuccess callback to remote caller", e)
                }
            }

            private fun IGetCredentialCallbacks.safeOnFailure(type: String, message: String) {
                try {
                    onFailure(type, message)
                } catch (e: RemoteException) {
                    Log.w(TAG, "Failed to deliver onFailure callback to remote caller", e)
                }
            }
        }
    }
}
