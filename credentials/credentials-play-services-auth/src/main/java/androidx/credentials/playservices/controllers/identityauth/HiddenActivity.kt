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

package androidx.credentials.playservices.controllers.identityauth

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.reportError
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.reportResult

/** An activity used to ensure all required API versions work as intended. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("ForbiddenSuperClass")
open class HiddenActivity : Activity() {

    private var resultReceiver: ResultReceiver? = null
    private var mWaitingForActivityResult = false

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        val type: String? = intent.getStringExtra(CredentialProviderBaseController.TYPE_TAG)
        resultReceiver =
            intent.getParcelableExtra(CredentialProviderBaseController.RESULT_RECEIVER_TAG)

        if (resultReceiver == null) {
            finish()
        }

        restoreState(savedInstanceState)
        if (mWaitingForActivityResult) {
            return
            // Past call still active
        }

        if (type == null) {
            Log.w(TAG, "Activity handed an unsupported type")
            finish()
            return
        }

        handleCredentialFlow(type)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mWaitingForActivityResult = savedInstanceState.getBoolean(KEY_AWAITING_RESULT, false)
        }
    }

    private fun setupFailure(resultReceiver: ResultReceiver, errName: String, errMsg: String) {
        resultReceiver.reportError(errName, errMsg)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AWAITING_RESULT, mWaitingForActivityResult)
        super.onSaveInstanceState(outState)
    }

    @Suppress("deprecation")
    private fun handleCredentialFlow(type: String) {
        val pendingIntent: PendingIntent? =
            intent.getParcelableExtra(CredentialProviderBaseController.EXTRA_FLOW_PENDING_INTENT)

        val requestCode: Int =
            intent.getIntExtra(
                CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG,
                DEFAULT_VALUE,
            )

        if (pendingIntent != null) {
            try {
                mWaitingForActivityResult = true
                startIntentSenderForResult(
                    pendingIntent.intentSender,
                    requestCode,
                    null,
                    0,
                    0,
                    0,
                    null,
                )
            } catch (e: IntentSender.SendIntentException) {
                setupIntentSenderFailureByType(type, e)
            }
        } else {
            setupPendingIntentFailureByType(type)
        }
    }

    private fun setupIntentSenderFailureByType(type: String, e: IntentSender.SendIntentException) {
        when (type) {
            CredentialProviderBaseController.BEGIN_SIGN_IN_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.GET_UNKNOWN,
                    "During begin sign in, one tap ui intent sender " + "failure: " + "${e.message}",
                )
            }
            CredentialProviderBaseController.CREATE_PASSWORD_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.CREATE_UNKNOWN,
                    "During save password, found UI intent sender " + "failure: ${e.message}",
                )
            }
            CredentialProviderBaseController.CREATE_PUBLIC_KEY_CREDENTIAL_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.CREATE_UNKNOWN,
                    "During public key credential, found IntentSender " +
                        "failure on public key creation: ${e.message}",
                )
            }
            CredentialProviderBaseController.SIGN_IN_INTENT_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.GET_UNKNOWN,
                    "During get sign-in intent, one tap ui intent sender " + "failure: ${e.message}",
                )
            }
        }
    }

    private fun setupPendingIntentFailureByType(type: String) {
        when (type) {
            CredentialProviderBaseController.BEGIN_SIGN_IN_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.GET_UNKNOWN,
                    "internal error during the begin sign in operation",
                )
            }
            CredentialProviderBaseController.CREATE_PASSWORD_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.CREATE_UNKNOWN,
                    "internal error during password creation",
                )
            }
            CredentialProviderBaseController.CREATE_PUBLIC_KEY_CREDENTIAL_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.CREATE_UNKNOWN,
                    "internal error during public key credential creation",
                )
            }
            CredentialProviderBaseController.SIGN_IN_INTENT_TAG -> {
                setupFailure(
                    resultReceiver!!,
                    CredentialProviderBaseController.Companion.GET_UNKNOWN,
                    "internal error during the sign-in intent operation",
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultReceiver?.reportResult(
            requestCode = requestCode,
            data = data,
            resultCode = resultCode,
        )
        mWaitingForActivityResult = false
        finish()
    }

    companion object {
        private const val DEFAULT_VALUE: Int = 1
        private const val TAG = "HiddenActivity"
        private const val KEY_AWAITING_RESULT = "androidx.credentials.playservices.AWAITING_RESULT"
    }
}
