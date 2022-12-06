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

package androidx.credentials.playservices

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialInterruptedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialUnknownException
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SavePasswordRequest
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions

/**
 * An activity used to ensure all required API versions work as intended.
 * @hide
 */
@Suppress("Deprecation", "ForbiddenSuperClass")
open class HiddenActivity : Activity() {

    private var resultReceiver: ResultReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        val type: String? = intent.getStringExtra(CredentialProviderBaseController.TYPE_TAG)
        resultReceiver = intent.getParcelableExtra(
            CredentialProviderBaseController.RESULT_RECEIVER_TAG)

        if (resultReceiver == null) {
            Log.i(TAG, "resultreceiver is null")
            finish()
        } else {
            Log.i(TAG, "resultreceiver is NOT null")
        }

        when (type) {
            CredentialProviderBaseController.BEGIN_SIGN_IN_TAG -> {
                handleBeginSignIn(resultReceiver!!)
            }
            CredentialProviderBaseController.CREATE_PASSWORD_TAG -> {
                handleCreatePassword(resultReceiver!!)
            }
            CredentialProviderBaseController.CREATE_PUBLIC_KEY_CREDENTIAL_TAG -> {
                handleCreatePublicKeyCredential(resultReceiver!!)
            } else -> {
                Log.i(TAG, "Unknown type")
                finish()
            }
        }
    }

    private fun handleCreatePublicKeyCredential(resultReceiver: ResultReceiver) {
        val fidoRegistrationRequest: PublicKeyCredentialCreationOptions? = intent
            .getParcelableExtra(CredentialProviderBaseController.REQUEST_TAG)
        val requestCode: Int = intent.getIntExtra(
            CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
                DEFAULT_VALUE)
        fidoRegistrationRequest?.let {
            Fido.getFido2ApiClient(this)
                .getRegisterPendingIntent(fidoRegistrationRequest)
                .addOnSuccessListener { result: PendingIntent ->
                    try {
                        startIntentSenderForResult(
                            result.intentSender,
                            requestCode,
                            null, /* fillInIntent= */
                            0, /* flagsMask= */
                            0, /* flagsValue= */
                            0, /* extraFlags= */
                            null /* options= */
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.i(
                            TAG,
                            "Failed to send pending intent for fido client " +
                                " : " + e.message
                        )
                        setupFailure(resultReceiver,
                            CreatePublicKeyCredentialUnknownException::class.java.name)
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.i(TAG, "Fido Registration failed with error: " + e.message)
                    var errName: String = CreatePublicKeyCredentialUnknownException::class.java.name
                    if (e is ApiException && e.statusCode in
                        CredentialProviderBaseController.retryables) {
                        errName = CreatePublicKeyCredentialInterruptedException::class.java.name
                    }
                    setupFailure(resultReceiver, errName)
                }
        } ?: run {
            Log.i(TAG, "request is null, nothing to launch for public key credentials")
            finish()
        }
    }

    private fun setupFailure(resultReceiver: ResultReceiver, errName: String) {
        val bundle = Bundle()
        bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE, true)
        bundle.putString(CredentialProviderBaseController.EXCEPTION_TYPE_TAG, errName)
        resultReceiver.send(Integer.MAX_VALUE, bundle)
        finish()
    }

    private fun handleBeginSignIn(resultReceiver: ResultReceiver) {
        val params: BeginSignInRequest? = intent.getParcelableExtra(
            CredentialProviderBaseController.REQUEST_TAG)
        val requestCode: Int = intent.getIntExtra(
            CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
            DEFAULT_VALUE)
        params?.let {
            Log.i(TAG, "Id: $params")
            Identity.getSignInClient(this).beginSignIn(params).addOnSuccessListener {
                Log.i(TAG, "On success")
                try {
                    startIntentSenderForResult(
                        it.pendingIntent.intentSender,
                        requestCode,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(
                        TAG, "Couldn't start One Tap UI in " +
                            "beginSignIn: " + e.localizedMessage
                    )
                    setupFailure(resultReceiver,
                        GetCredentialUnknownException::class.java.name)
                }
            }.addOnFailureListener { e: Exception ->
                Log.i(TAG, "On Begin Sign In Failure:  " + e.message)
                var errName: String = GetCredentialUnknownException::class.java.name
                if (e is ApiException && e.statusCode in
                    CredentialProviderBaseController.retryables) {
                    errName = GetCredentialInterruptedException::class.java.name
                }
                setupFailure(resultReceiver, errName)
            }
        } ?: run {
            Log.i(TAG, "params is null, nothing to launch for begin sign in")
            finish()
        }
    }

    private fun handleCreatePassword(resultReceiver: ResultReceiver) {
        val params: SavePasswordRequest? = intent.getParcelableExtra(
            CredentialProviderBaseController.REQUEST_TAG)
        val requestCode: Int = intent.getIntExtra(
            CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
            DEFAULT_VALUE)
        params?.let {
            Log.i(TAG, "Id: $params")

            Identity.getCredentialSavingClient(this).savePassword(params)
                .addOnSuccessListener {
                Log.i(TAG, "On success")
                    try {
                        startIntentSenderForResult(
                            it.pendingIntent.intentSender,
                            requestCode,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.e(
                            TAG, "Couldn't start save password UI in " +
                                "create password: " + e.localizedMessage
                        )
                        setupFailure(resultReceiver,
                            GetCredentialUnknownException::class.java.name)
                    }
            }.addOnFailureListener { e: Exception ->
                    Log.i(TAG, "On Create Password Failure:  " + e.message)
                    var errName: String = CreateCredentialUnknownException::class.java.name
                    if (e is ApiException && e.statusCode in
                        CredentialProviderBaseController.retryables) {
                        errName = CreateCredentialInterruptedException::class.java.name
                    }
                    setupFailure(resultReceiver, errName)
            }
        } ?: run {
            Log.i(TAG, "params is null, nothing to launch for create password")
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult : $requestCode , $resultCode")
        val bundle = Bundle()
        bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE, false)
        bundle.putInt(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, requestCode)
        bundle.putParcelable(CredentialProviderBaseController.RESULT_DATA_TAG, data)
        resultReceiver?.send(resultCode, bundle)
        finish()
    }

    companion object {
        private const val DEFAULT_VALUE: Int = 1
        private val TAG: String = HiddenActivity::class.java.name
    }
}