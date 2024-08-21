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

package androidx.credentials.playservices.controllers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.ResultReceiver
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.common.api.CommonStatusCodes

/** Holds all non type specific details shared by the controllers. */
internal open class CredentialProviderBaseController(private val context: Context) {
    companion object {

        // Common retryable status codes from the play modules found
        // https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
        val retryables: Set<Int> =
            setOf(
                CommonStatusCodes.NETWORK_ERROR,
                CommonStatusCodes.CONNECTION_SUSPENDED_DURING_CALL
            )

        // Generic controller request code used by all controllers
        @JvmStatic internal val CONTROLLER_REQUEST_CODE: Int = 1

        /** -- Used to avoid reflection, these constants map errors from HiddenActivity -- */
        const val GET_CANCELED = "GET_CANCELED_TAG"
        const val GET_INTERRUPTED = "GET_INTERRUPTED"
        const val GET_NO_CREDENTIALS = "GET_NO_CREDENTIALS"
        const val GET_UNKNOWN = "GET_UNKNOWN"

        const val CREATE_CANCELED = "CREATE_CANCELED"
        const val CREATE_INTERRUPTED = "CREATE_INTERRUPTED"
        const val CREATE_UNKNOWN = "CREATE_UNKNOWN"

        /** ---- Data Constants to pass between the controllers and the hidden activity---- */

        // Key to indicate type sent from controller to hidden activity
        const val TYPE_TAG = "TYPE"

        // Value for the specific begin sign in type
        const val BEGIN_SIGN_IN_TAG = "BEGIN_SIGN_IN"

        // Key for the Sign-in Intent flow
        const val SIGN_IN_INTENT_TAG = "SIGN_IN_INTENT"

        // Value for the specific create password type
        const val CREATE_PASSWORD_TAG = "CREATE_PASSWORD"

        // Value for the specific create public key credential type
        const val CREATE_PUBLIC_KEY_CREDENTIAL_TAG = "CREATE_PUBLIC_KEY_CREDENTIAL"

        // Key for the actual parcelable type sent to the hidden activity
        const val REQUEST_TAG = "REQUEST_TYPE"

        // Key for the result intent to send back to the controller
        const val RESULT_DATA_TAG = "RESULT_DATA"

        // Key for the actual parcelable type sent to the hidden activity
        const val EXTRA_GET_CREDENTIAL_INTENT = "EXTRA_GET_CREDENTIAL_INTENT"

        // Key for the failure boolean sent back from hidden activity to controller
        const val FAILURE_RESPONSE_TAG = "FAILURE_RESPONSE"

        // Key for the exception type sent back from hidden activity to controllers if error
        const val EXCEPTION_TYPE_TAG = "EXCEPTION_TYPE"

        // Key for an error message propagated from hidden activity to controllers
        const val EXCEPTION_MESSAGE_TAG = "EXCEPTION_MESSAGE"

        // Key for the activity request code from controllers to activity
        const val ACTIVITY_REQUEST_CODE_TAG = "ACTIVITY_REQUEST_CODE"

        // Key for the result receiver sent from controller to activity
        const val RESULT_RECEIVER_TAG = "RESULT_RECEIVER"

        /** Shuttles back exceptions only related to the hidden activity that can't be parceled */
        internal fun getCredentialExceptionTypeToException(
            typeName: String?,
            msg: String?
        ): GetCredentialException {
            return when (typeName) {
                GET_CANCELED -> {
                    GetCredentialCancellationException(msg)
                }
                GET_INTERRUPTED -> {
                    GetCredentialInterruptedException(msg)
                }
                GET_NO_CREDENTIALS -> {
                    NoCredentialException(msg)
                }
                else -> {
                    GetCredentialUnknownException(msg)
                }
            }
        }

        internal fun ResultReceiver.reportError(errName: String, errMsg: String) {
            val bundle = Bundle()
            bundle.putBoolean(FAILURE_RESPONSE_TAG, true)
            bundle.putString(EXCEPTION_TYPE_TAG, errName)
            bundle.putString(EXCEPTION_MESSAGE_TAG, errMsg)
            this.send(Integer.MAX_VALUE, bundle)
        }

        internal fun ResultReceiver.reportResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val bundle = Bundle()
            bundle.putBoolean(FAILURE_RESPONSE_TAG, false)
            bundle.putInt(ACTIVITY_REQUEST_CODE_TAG, requestCode)
            bundle.putParcelable(RESULT_DATA_TAG, data)
            this.send(resultCode, bundle)
        }

        internal fun createCredentialExceptionTypeToException(
            typeName: String?,
            msg: String?
        ): CreateCredentialException {
            return when (typeName) {
                CREATE_CANCELED -> {
                    CreateCredentialCancellationException(msg)
                }
                CREATE_INTERRUPTED -> {
                    CreateCredentialInterruptedException(msg)
                }
                else -> {
                    CreateCredentialUnknownException(msg)
                }
            }
        }
    }

    fun <T : ResultReceiver?> toIpcFriendlyResultReceiver(resultReceiver: T): ResultReceiver? {
        val parcel: Parcel = Parcel.obtain()
        resultReceiver!!.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val ipcFriendly = ResultReceiver.CREATOR.createFromParcel(parcel)
        parcel.recycle()
        return ipcFriendly
    }

    protected fun generateHiddenActivityIntent(
        resultReceiver: ResultReceiver,
        hiddenIntent: Intent,
        typeTag: String
    ) {
        hiddenIntent.putExtra(TYPE_TAG, typeTag)
        hiddenIntent.putExtra(ACTIVITY_REQUEST_CODE_TAG, CONTROLLER_REQUEST_CODE)
        hiddenIntent.putExtra(RESULT_RECEIVER_TAG, toIpcFriendlyResultReceiver(resultReceiver))
        hiddenIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    }
}
