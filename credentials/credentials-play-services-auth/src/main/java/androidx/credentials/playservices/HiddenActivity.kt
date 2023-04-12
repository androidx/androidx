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
import androidx.credentials.exceptions.NoCredentialException
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
    private var mWaitingForActivityResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        val type: String? = intent.getStringExtra(CredentialProviderBaseController.TYPE_TAG)
        resultReceiver = intent.getParcelableExtra(
            CredentialProviderBaseController.RESULT_RECEIVER_TAG)

        if (resultReceiver == null) {
            finish()
        }

        restoreState(savedInstanceState)
        if (mWaitingForActivityResult) {
            return; // Past call still active
        }

        when (type) {
            CredentialProviderBaseController.BEGIN_SIGN_IN_TAG -> {
                handleBeginSignIn()
            }
            CredentialProviderBaseController.CREATE_PASSWORD_TAG -> {
                handleCreatePassword()
            }
            CredentialProviderBaseController.CREATE_PUBLIC_KEY_CREDENTIAL_TAG -> {
                handleCreatePublicKeyCredential()
            } else -> {
                Log.w(TAG, "Activity handed an unsupported type")
                finish()
            }
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mWaitingForActivityResult = savedInstanceState.getBoolean(KEY_AWAITING_RESULT, false)
        }
    }

    private fun handleCreatePublicKeyCredential() {
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
                        mWaitingForActivityResult = true
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
                        setupFailure(resultReceiver!!,
                            CreateCredentialUnknownException::class.java.name,
                            "During public key credential, found IntentSender " +
                                "failure on public key creation: ${e.message}")
                    }
                }
                .addOnFailureListener { e: Exception ->
                    var errName: String = CreateCredentialUnknownException::class.java.name
                    if (e is ApiException && e.statusCode in
                        CredentialProviderBaseController.retryables) {
                        errName = CreateCredentialInterruptedException::class.java.name
                    }
                    setupFailure(resultReceiver!!, errName,
                        "During create public key credential, fido registration " +
                            "failure: ${e.message}")
                }
        } ?: run {
            Log.w(TAG, "During create public key credential, request is null, so nothing to " +
                "launch for public key credentials")
            finish()
        }
    }

    private fun setupFailure(resultReceiver: ResultReceiver, errName: String, errMsg: String) {
        val bundle = Bundle()
        bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE_TAG, true)
        bundle.putString(CredentialProviderBaseController.EXCEPTION_TYPE_TAG, errName)
        bundle.putString(CredentialProviderBaseController.EXCEPTION_MESSAGE_TAG, errMsg)
        resultReceiver.send(Integer.MAX_VALUE, bundle)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AWAITING_RESULT, mWaitingForActivityResult)
        super.onSaveInstanceState(outState)
    }

    private fun handleBeginSignIn() {
        val params: BeginSignInRequest? = intent.getParcelableExtra(
            CredentialProviderBaseController.REQUEST_TAG)
        val requestCode: Int = intent.getIntExtra(
            CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
            DEFAULT_VALUE)
        params?.let {
            Identity.getSignInClient(this).beginSignIn(params).addOnSuccessListener {
                try {
                    mWaitingForActivityResult = true
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
                    setupFailure(resultReceiver!!,
                        GetCredentialUnknownException::class.java.name,
                            "During begin sign in, one tap ui intent sender " +
                                "failure: ${e.message}")
                }
            }.addOnFailureListener { e: Exception ->
                var errName: String = NoCredentialException::class.java.name
                if (e is ApiException && e.statusCode in
                    CredentialProviderBaseController.retryables) {
                    errName = GetCredentialInterruptedException::class.java.name
                }
                setupFailure(resultReceiver!!, errName,
                    "During begin sign in, failure response from one tap: ${e.message}")
            }
        } ?: run {
            Log.i(TAG, "During begin sign in, params is null, nothing to launch for " +
                "begin sign in")
            finish()
        }
    }

    private fun handleCreatePassword() {
        val params: SavePasswordRequest? = intent.getParcelableExtra(
            CredentialProviderBaseController.REQUEST_TAG)
        val requestCode: Int = intent.getIntExtra(
            CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
            DEFAULT_VALUE)
        params?.let {
            Identity.getCredentialSavingClient(this).savePassword(params)
                .addOnSuccessListener {
                    try {
                        mWaitingForActivityResult = true
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
                        setupFailure(resultReceiver!!,
                            CreateCredentialUnknownException::class.java.name,
                                "During save password, found UI intent sender " +
                                    "failure: ${e.message}")
                    }
            }.addOnFailureListener { e: Exception ->
                    var errName: String = CreateCredentialUnknownException::class.java.name
                    if (e is ApiException && e.statusCode in
                        CredentialProviderBaseController.retryables) {
                        errName = CreateCredentialInterruptedException::class.java.name
                    }
                    setupFailure(resultReceiver!!, errName, "During save password, found " +
                        "password failure response from one tap ${e.message}")
            }
        } ?: run {
            Log.i(TAG, "During save password, params is null, nothing to launch for create" +
                " password")
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val bundle = Bundle()
        bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE_TAG, false)
        bundle.putInt(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, requestCode)
        bundle.putParcelable(CredentialProviderBaseController.RESULT_DATA_TAG, data)
        resultReceiver?.send(resultCode, bundle)
        mWaitingForActivityResult = false
        finish()
    }

    companion object {
        private const val DEFAULT_VALUE: Int = 1
        private val TAG: String = HiddenActivity::class.java.name
        private const val KEY_AWAITING_RESULT = "androidx.credentials.playservices.AWAITING_RESULT"
    }
}