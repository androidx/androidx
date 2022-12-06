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

import android.content.Intent
import android.os.Parcel
import android.os.ResultReceiver
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialInterruptedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialUnknownException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * Holds all non type specific details shared by the controllers.
 * @hide
 */
open class CredentialProviderBaseController(private val activity: android.app.Activity) {
    companion object {

        val retryables: Set<Int> = setOf(
            CommonStatusCodes.INTERNAL_ERROR,
            CommonStatusCodes.NETWORK_ERROR
        )

        // Generic controller request code used by all controllers
        @JvmStatic protected val CONTROLLER_REQUEST_CODE: Int = 1

        /** ---- Data Constants to pass between the controllers and the hidden activity---- **/

        // Key to indicate type sent from controller to hidden activity
        const val TYPE_TAG = "type_tag"
        // Value for the specific begin sign in type
        const val BEGIN_SIGN_IN_TAG = "begin_sign_in"
        // Value for the specific create password type
        const val CREATE_PASSWORD_TAG = "create_password"
        // Value for the specific create public key credential type
        const val CREATE_PUBLIC_KEY_CREDENTIAL_TAG = "create_public_key_credential"

        // Key for the actual parcelable type sent to the hidden activity
        const val REQUEST_TAG = "request_type"
        // Key for the result intent to send back to the controller
        const val RESULT_DATA_TAG = "result_data"

        // Key for the failure boolean sent back from hidden activity to controller
        const val FAILURE_RESPONSE = "failed"
        // Key for the exception type sent back from hidden activity to controllers if error
        const val EXCEPTION_TYPE_TAG = "exception_type"

        // Key for the activity request code from controllers to activity
        const val ACTIVITY_REQUEST_CODE_TAG = "activity_tag"
        // Key for the result receiver sent from controller to activity
        const val RESULT_RECEIVER_TAG = "result_receiver"

        // Shuttles back exceptions only related to the hidden activity that can't be parceled
        internal val publicKeyCredentialExceptionTypeToException:
            Map<String, CreatePublicKeyCredentialException> = linkedMapOf(
            CreatePublicKeyCredentialUnknownException::class.java.name to
                CreatePublicKeyCredentialUnknownException(),
            CreatePublicKeyCredentialInterruptedException::class.java.name to
                CreatePublicKeyCredentialInterruptedException()
        )

        internal val getCredentialExceptionTypeToException: Map<String, GetCredentialException> =
            linkedMapOf(
                GetCredentialUnknownException::class.java.name to
                    GetCredentialUnknownException(),
                GetCredentialInterruptedException::class.java.name to
                    GetCredentialInterruptedException()
            )

        internal val createCredentialExceptionTypeToException:
            Map<String, CreateCredentialException> =
            linkedMapOf(
                CreateCredentialUnknownException::class.java.name to
                    CreateCredentialUnknownException(),
                CreateCredentialInterruptedException::class.java.name to
                    CreateCredentialInterruptedException()
            )
    }

    fun <T : ResultReceiver?> toIpcFriendlyResultReceiver(
        resultReceiver: T
    ): ResultReceiver? {
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
        hiddenIntent.putExtra(ACTIVITY_REQUEST_CODE_TAG,
            CONTROLLER_REQUEST_CODE
        )
        hiddenIntent.putExtra(
            RESULT_RECEIVER_TAG,
            toIpcFriendlyResultReceiver(resultReceiver))
        hiddenIntent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
    }
}