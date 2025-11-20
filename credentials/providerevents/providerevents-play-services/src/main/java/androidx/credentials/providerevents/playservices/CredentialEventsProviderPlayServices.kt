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

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RestrictTo
import androidx.core.os.OutcomeReceiverCompat
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.publickeycredential.SignalCredentialStateException
import androidx.credentials.providerevents.CredentialEventsProvider
import androidx.credentials.providerevents.playservices.ConversionUtils.Companion.convertToGmsResponse
import androidx.credentials.providerevents.playservices.ConversionUtils.Companion.convertToJetpackRequest
import androidx.credentials.providerevents.service.CredentialProviderEventsService
import androidx.credentials.providerevents.signal.ProviderSignalCredentialStateCallback
import com.google.android.gms.common.util.UidVerifier
import com.google.android.gms.identitycredentials.CallingAppInfoParcelable
import com.google.android.gms.identitycredentials.CreateCredentialRequest
import com.google.android.gms.identitycredentials.ExportCredentialsToDeviceSetupRequest
import com.google.android.gms.identitycredentials.GetCredentialTransferCapabilitiesRequest
import com.google.android.gms.identitycredentials.ImportCredentialsForDeviceSetupRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateRequest
import com.google.android.gms.identitycredentials.SignalCredentialStateResponse
import com.google.android.gms.identitycredentials.provider.ICreateCredentialCallbacks
import com.google.android.gms.identitycredentials.provider.ICredentialProviderService
import com.google.android.gms.identitycredentials.provider.ICredentialTransferCapabilitiesCallbacks
import com.google.android.gms.identitycredentials.provider.IExportCredentialsCallbacks
import com.google.android.gms.identitycredentials.provider.IImportCredentialsCallbacks
import com.google.android.gms.identitycredentials.provider.ISignalCredentialStateCallbacks
import java.lang.ref.WeakReference

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CredentialEventsProviderPlayServices() : CredentialEventsProvider {

    override fun getStubImplementation(service: CredentialProviderEventsService): IBinder? {
        val binderInterface = ServiceWrapper(service, Handler(Looper.getMainLooper()))
        return binderInterface.asBinder()
    }

    @Suppress("RestrictedApiAndroidX")
    private class ServiceWrapper(service: CredentialProviderEventsService, val handler: Handler) :
        ICredentialProviderService.Stub() {

        private val context: Context = service.applicationContext

        var serviceRef: WeakReference<CredentialProviderEventsService> = WeakReference(service)

        @Suppress("RestrictedApiAndroidX")
        override fun onCreateCredentialRequest(
            request: CreateCredentialRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ICreateCredentialCallbacks,
        ) {
            if (!UidVerifier.isGooglePlayServicesUid(context, getCallingUid())) {
                return
            }
            // TODO(b/385394695): Fix being able to create CallingAppInfo with GMS
            //  CallingAppInfoParcelable
            val jetpackRequest =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    convertToJetpackRequest(request)
                } else {
                    null
                }
            if (jetpackRequest == null) {
                callback.onFailure(
                    com.google.android.gms.identitycredentials.CreateCredentialException
                        .ERROR_TYPE_UNKNOWN,
                    "Request could not be constructed",
                )
                return
            }

            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }

                service.onCreateCredentialRequest(
                    jetpackRequest,
                    CancellationSignal(),
                    object :
                        OutcomeReceiverCompat<CreateCredentialResponse, CreateCredentialException> {
                        override fun onResult(result: CreateCredentialResponse?) {
                            val response = convertToGmsResponse(result)
                            if (response != null) {
                                // TODO(b/385394695): Remove place holder pending intent after
                                //  exposing a callback method that does not need pending intent
                                val placeHolderPendingIntent =
                                    PendingIntent.getService(
                                        context,
                                        0,
                                        Intent(),
                                        PendingIntent.FLAG_IMMUTABLE,
                                    )
                                callback.onSuccessV2(response, placeHolderPendingIntent)
                            } else {
                                callback.onFailure(
                                    com.google.android.gms.identitycredentials
                                        .CreateCredentialException
                                        .ERROR_TYPE_UNKNOWN,
                                    "Response could not be constructed",
                                )
                            }
                        }

                        override fun onError(error: CreateCredentialException) {
                            callback.onFailure(error.type, error.message.toString())
                        }
                    },
                )
            }
        }

        /** Called when the provider needs to ingest credentials */
        override fun onExportCredentials(
            request: ExportCredentialsToDeviceSetupRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: IExportCredentialsCallbacks,
        ) {}

        override fun onGetCredentialTransferCapabilities(
            request: GetCredentialTransferCapabilitiesRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ICredentialTransferCapabilitiesCallbacks,
        ) {}

        /** Called when the provider needs to return credentials */
        override fun onImportCredentials(
            request: ImportCredentialsForDeviceSetupRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: IImportCredentialsCallbacks,
        ) {}

        override fun onSignalCredentialStateRequest(
            request: SignalCredentialStateRequest,
            callingAppInfo: CallingAppInfoParcelable,
            callback: ISignalCredentialStateCallbacks,
        ) {
            if (!UidVerifier.isGooglePlayServicesUid(context, getCallingUid())) {
                return
            }

            val jetpackRequest = convertToJetpackRequest(request)
            if (jetpackRequest == null) {
                callback.onFailure(
                    com.google.android.gms.identitycredentials.SignalCredentialStateException
                        .ERROR_TYPE_UNKNOWN,
                    "Request could not be constructed",
                )
                return
            }

            handler.post {
                val service = serviceRef.get()
                if (service == null) {
                    return@post
                }
                service.onSignalCredentialStateRequest(
                    jetpackRequest,
                    object : ProviderSignalCredentialStateCallback {
                        override fun onSignalConsumed() {
                            callback.onSuccess(SignalCredentialStateResponse())
                        }
                    },
                )
            }
        }
    }

    private companion object {
        const val GMS_PACKAGE_NAME: String = "com.google.android.gms"
        const val TAG = "EventsPlayServices"
        const val EXTRA_CREDENTIAL_CALLING_APP_INFO =
            "androidx.credentials.providerevents.extra.CALLING_APP_INFO"
    }
}
