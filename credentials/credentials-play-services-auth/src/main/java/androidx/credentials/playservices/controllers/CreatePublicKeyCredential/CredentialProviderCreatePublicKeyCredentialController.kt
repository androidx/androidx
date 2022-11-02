/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices.controllers.CreatePublicKeyCredential

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.util.Log
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialInterruptedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialUnknownException
import androidx.credentials.playservices.controllers.CredentialProviderController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import java.util.concurrent.Executor

/**
 * A controller to handle the CreatePublicKeyCredential flow with play services.
 *
 * @hide
 */
@Suppress("deprecation")
class CredentialProviderCreatePublicKeyCredentialController :
        CredentialProviderController<
            CreatePublicKeyCredentialRequest,
            PublicKeyCredentialCreationOptions,
            PublicKeyCredential,
            CreateCredentialResponse,
            CreateCredentialException>() {

    /**
     * The callback object state, used in the protected handleResponse method.
     */
    private lateinit var callback: CredentialManagerCallback<CreateCredentialResponse,
        CreateCredentialException>

    /**
     * The callback requires an executor to invoke it.
     */
    private lateinit var executor: Executor

    @SuppressLint("ClassVerificationFailure")
    override fun invokePlayServices(
        request: CreatePublicKeyCredentialRequest,
        callback: CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>,
        executor: Executor
    ) {
        this.callback = callback
        this.executor = executor
        val fidoRegistrationRequest: PublicKeyCredentialCreationOptions =
            this.convertRequestToPlayServices(request)
        Fido.getFido2ApiClient(getActivity())
            .getRegisterPendingIntent(fidoRegistrationRequest)
            .addOnSuccessListener { result: PendingIntent ->
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        startIntentSenderForResult(
                            result.intentSender,
                            REQUEST_CODE_GIS_CREATE_PUBLIC_KEY_CREDENTIAL,
                            null, /* fillInIntent= */
                            0, /* flagsMask= */
                            0, /* flagsValue= */
                            0, /* extraFlags= */
                            null /* options= */
                        )
                    }
                } catch (e: IntentSender.SendIntentException) {
                    Log.i(
                        TAG,
                        "Failed to send pending intent for fido client " +
                            " : " + e.message
                    )
                    val exception: CreatePublicKeyCredentialException =
                        CreatePublicKeyCredentialUnknownException()
                    executor.execute { ->
                        callback.onError(
                            exception
                        )
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                var exception: CreatePublicKeyCredentialException =
                    CreatePublicKeyCredentialUnknownException()
                if (e is ApiException && e.statusCode in this.retryables) {
                    exception = CreatePublicKeyCredentialInterruptedException()
                }
                Log.i(TAG, "Fido Registration failed with error: " + e.message)
                executor.execute { ->
                    callback.onError(
                        exception
                    )
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleResponse(requestCode, resultCode, data)
    }

    private fun handleResponse(uniqueRequestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "$uniqueRequestCode $resultCode $data")
        if (uniqueRequestCode != REQUEST_CODE_GIS_CREATE_PUBLIC_KEY_CREDENTIAL) {
            return
        }
        if (resultCode != Activity.RESULT_OK) {
            var exception: CreateCredentialException =
                CreatePublicKeyCredentialUnknownException()
            if (resultCode == Activity.RESULT_CANCELED) {
                exception = CreateCredentialCancellationException()
            }
            this.executor.execute { -> this.callback.onError(exception) }
            return
        }
        val bytes: ByteArray? = data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        if (bytes == null) {
            this.executor.execute { this.callback.onError(
                CreatePublicKeyCredentialUnknownException(
                "Internal error fido module giving null bytes")
            ) }
            return
        }
        val cred: PublicKeyCredential = PublicKeyCredential.deserializeFromBytes(bytes)
        if (PublicKeyCredentialControllerUtility.publicKeyCredentialResponseContainsError(
                this.callback, this.executor, cred)) {
            return
        }
        val response = this.convertResponseToCredentialManager(cred)
        this.executor.execute { this.callback.onResult(response) }
    }

    override fun convertRequestToPlayServices(request: CreatePublicKeyCredentialRequest):
        PublicKeyCredentialCreationOptions {
        return PublicKeyCredentialControllerUtility.convert(request)
    }

    override fun convertResponseToCredentialManager(response: PublicKeyCredential):
        CreateCredentialResponse {
        return CreatePublicKeyCredentialResponse(PublicKeyCredentialControllerUtility
            .toCreatePasskeyResponseJson(response))
    }

    companion object {
        private val TAG = CredentialProviderCreatePublicKeyCredentialController::class.java.name
        private const val REQUEST_CODE_GIS_CREATE_PUBLIC_KEY_CREDENTIAL: Int = 1
        // TODO("Ensure this works with the lifecycle")

        /**
         * This finds a past version of the
         * [CredentialProviderCreatePublicKeyCredentialController] if it exists, otherwise
         * it generates a new instance.
         *
         * @param fragmentManager a fragment manager pulled from an android activity
         * @return a credential provider controller for CreatePublicKeyCredential
         */
        @JvmStatic
        fun getInstance(fragmentManager: android.app.FragmentManager):
            CredentialProviderCreatePublicKeyCredentialController {
            var controller = findPastController(
                REQUEST_CODE_GIS_CREATE_PUBLIC_KEY_CREDENTIAL,
                fragmentManager)
            if (controller == null) {
                controller = CredentialProviderCreatePublicKeyCredentialController()
                fragmentManager.beginTransaction().add(controller,
                    REQUEST_CODE_GIS_CREATE_PUBLIC_KEY_CREDENTIAL.toString())
                    .commitAllowingStateLoss()
                fragmentManager.executePendingTransactions()
            }
            return controller
        }

        internal fun findPastController(
            requestCode: Int,
            fragmentManager: android.app.FragmentManager
        ): CredentialProviderCreatePublicKeyCredentialController? {
            val oldFragment = fragmentManager.findFragmentByTag(requestCode.toString())
            try {
                return oldFragment as CredentialProviderCreatePublicKeyCredentialController
            } catch (e: Exception) {
                Log.i(
                    TAG,
                    "Error with old fragment or null - replacement required")
                if (oldFragment != null) {
                    fragmentManager.beginTransaction().remove(oldFragment).commitAllowingStateLoss()
                }
                // TODO("Ensure this is well tested for fragment issues")
                return null
            }
        }
    }
}