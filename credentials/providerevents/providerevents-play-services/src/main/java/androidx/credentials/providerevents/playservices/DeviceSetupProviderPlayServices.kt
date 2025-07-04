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

package androidx.credentials.providerevents.playservices

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.providerevents.DeviceSetupProvider
import androidx.credentials.providerevents.exception.ExportCredentialsException
import androidx.credentials.providerevents.exception.GetCredentialTransferCapabilitiesException
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.playservices.ConversionUtils.Companion.convertToJetpackRequest
import androidx.credentials.providerevents.service.DeviceSetupService
import androidx.credentials.providerevents.transfer.CredentialTransferCapabilities
import androidx.credentials.providerevents.transfer.ExportCredentialsRequest
import androidx.credentials.providerevents.transfer.ExportCredentialsResponse
import androidx.credentials.providerevents.transfer.ImportCredentialsResponse
import com.google.android.gms.common.util.UidVerifier
import com.google.android.gms.identitycredentials.CallingAppInfoParcelable
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupResponse
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupResponse
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.provider.ICreateCredentialCallbacks
import com.google.android.gms.identitycredentials.provider.ICredentialProviderService
import com.google.android.gms.identitycredentials.provider.ICredentialTransferCapabilitiesCallbacks
import com.google.android.gms.identitycredentials.provider.IExportCredentialsCallbacks
import com.google.android.gms.identitycredentials.provider.IImportCredentialsCallbacks
import com.google.android.gms.identitycredentials.provider.ISignalCredentialStateCallbacks
import java.io.IOException
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DeviceSetupProviderPlayServices : DeviceSetupProvider {

    override fun getStubImplementation(service: DeviceSetupService): IBinder? {
        val binderInterface = ServiceWrapper(service, Handler(Looper.getMainLooper()))
        return binderInterface.asBinder()
    }

    private class ServiceWrapper(service: DeviceSetupService, val handler: Handler) :
        ICredentialProviderService.Stub() {

        private val context: Context = service.applicationContext

        var serviceRef: WeakReference<DeviceSetupService> = WeakReference(service)

        @Suppress("RestrictedApiAndroidX")
        override fun onCreateCredentialRequest(
            request: CreateCredentialRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ICreateCredentialCallbacks,
        ) {}

        /** Called when the provider needs to ingest credentials */
        override fun onExportCredentials(
            request: ExportCredentialsToDeviceSetupRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: IExportCredentialsCallbacks,
        ) {
            if (!UidVerifier.isGooglePlayServicesUid(context, getCallingUid())) {
                callback.onFailure(
                    ExportCredentialsException.SYSTEM_ERROR_TYPE,
                    "Not authorized to invoke this API",
                )
                return
                // TODO(b/416798373): revisit the error types
            }
            var jetpackRequest: ExportCredentialsRequest?
            try {
                // TODO(b/385394695): Fix being able to create CallingAppInfo with GMS
                //  CallingAppInfoParcelable
                jetpackRequest = convertToJetpackRequest(request, context)
            } catch (e: IOException) {
                Log.e(TAG, "Exception thrown while reading the response from the file", e)
                callback.onFailure(
                    ExportCredentialsException.SYSTEM_ERROR_TYPE,
                    "Error while reading the response from the file",
                )
                return
            }
            val callingAppInfoBundle =
                request.requestData.getBundle(EXTRA_CREDENTIAL_CALLING_APP_INFO)
            val constructedCallingAppInfo = constructCallingAppInfo(callingAppInfoBundle)
            if (constructedCallingAppInfo == null) {
                callback.onFailure(
                    ExportCredentialsException.UNKNOWN_ERROR_TYPE,
                    "The request did not contain the calling app info.",
                )
                return
            }

            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }

                service.onExportCredentialsRequest(
                    jetpackRequest,
                    constructedCallingAppInfo,
                    object :
                        OutcomeReceiverCompat<
                            ExportCredentialsResponse,
                            ExportCredentialsException,
                        > {
                        override fun onResult(result: ExportCredentialsResponse) {
                            callback.onSuccess(
                                ExportCredentialsToDeviceSetupResponse(
                                    ExportCredentialsResponse.asBundle(result)
                                )
                            )
                        }

                        override fun onError(error: ExportCredentialsException) {
                            callback.onFailure(error.type, error.message.toString())
                        }
                    },
                )
            }
        }

        override fun onGetCredentialTransferCapabilities(
            request: GetCredentialTransferCapabilitiesRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ICredentialTransferCapabilitiesCallbacks,
        ) {
            if (!UidVerifier.isGooglePlayServicesUid(context, getCallingUid())) {
                callback.onFailure(
                    GetCredentialTransferCapabilitiesException.SYSTEM_ERROR_TYPE,
                    "Not authorized to invoke this API",
                )
                return
            }
            val callingAppInfoBundle =
                request.requestData.getBundle(EXTRA_CREDENTIAL_CALLING_APP_INFO)
            val constructedCallingAppInfo = constructCallingAppInfo(callingAppInfoBundle)
            if (constructedCallingAppInfo == null) {
                callback.onFailure(
                    GetCredentialTransferCapabilitiesException.UNKNOWN_ERROR_TYPE,
                    "The request did not contain the calling app info.",
                )
                return
            }

            // TODO(b/385394695): Fix being able to create CallingAppInfo with GMS
            //  CallingAppInfoParcelable
            val jetpackRequest = convertToJetpackRequest(request)
            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }
                service.onGetCredentialTransferCapabilities(
                    jetpackRequest,
                    constructedCallingAppInfo,
                    object :
                        OutcomeReceiverCompat<
                            CredentialTransferCapabilities,
                            GetCredentialTransferCapabilitiesException,
                        > {
                        override fun onResult(result: CredentialTransferCapabilities) {
                            callback.onSuccess(
                                com.google.android.gms.identitycredentials
                                    .CredentialTransferCapabilities(
                                        CredentialTransferCapabilities.asBundle(result)
                                    )
                            )
                        }

                        override fun onError(error: GetCredentialTransferCapabilitiesException) {
                            callback.onFailure(error.type, error.message.toString())
                        }
                    },
                )
            }
        }

        /** Called when the provider needs to return credentials */
        override fun onImportCredentials(
            request: ImportCredentialsForDeviceSetupRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: IImportCredentialsCallbacks,
        ) {
            if (!UidVerifier.isGooglePlayServicesUid(context, getCallingUid())) {
                callback.onFailure(
                    ImportCredentialsException.SYSTEM_ERROR_TYPE,
                    "Not authorized to invoke this API",
                )
                return
            }
            val callingAppInfoBundle =
                request.requestData.getBundle(EXTRA_CREDENTIAL_CALLING_APP_INFO)
            val constructedCallingAppInfo = constructCallingAppInfo(callingAppInfoBundle)
            if (constructedCallingAppInfo == null) {
                callback.onFailure(
                    GetCredentialTransferCapabilitiesException.UNKNOWN_ERROR_TYPE,
                    "The request did not contain the calling app info.",
                )
                return
            }
            // TODO(b/385394695): Fix being able to create CallingAppInfo with GMS
            //  CallingAppInfoParcelable
            val jetpackRequest = convertToJetpackRequest(request)

            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }
                service.onImportCredentialsRequest(
                    jetpackRequest,
                    constructedCallingAppInfo,
                    object :
                        OutcomeReceiverCompat<
                            ImportCredentialsResponse,
                            ImportCredentialsException,
                        > {
                        override fun onResult(result: ImportCredentialsResponse) {
                            try {
                                UriUtils.writeToUri(request.uri, result.responseJson, context)
                                callback.onSuccess(
                                    ImportCredentialsForDeviceSetupResponse(
                                        ImportCredentialsResponse.asBundle(result)
                                    )
                                )
                            } catch (e: IOException) {
                                Log.e(
                                    TAG,
                                    "Exception thrown while writing the response to the file",
                                    e,
                                )
                                callback.onFailure(
                                    ImportCredentialsException.SYSTEM_ERROR_TYPE,
                                    "Error while writing the response to file",
                                )
                            }
                        }

                        override fun onError(error: ImportCredentialsException) {
                            callback.onFailure(error.type, error.message.toString())
                        }
                    },
                )
            }
        }

        override fun onSignalCredentialStateRequest(
            p0: SignalCredentialStateRequest,
            p1: CallingAppInfoParcelable,
            p2: ISignalCredentialStateCallbacks,
        ) {}
    }

    private companion object {
        const val GMS_PACKAGE_NAME: String = "com.google.android.gms"
        const val TAG = "EventsPlayServices"
        const val EXTRA_CREDENTIAL_CALLING_APP_INFO =
            "androidx.credentials.providerevents.extra.CALLING_APP_INFO"

        @Suppress("RestrictedApiAndroidX")
        private fun constructCallingAppInfo(bundle: Bundle?): CallingAppInfo? {
            return bundle?.let { CallingAppInfo.extractCallingAppInfo(it) }
        }
    }
}
