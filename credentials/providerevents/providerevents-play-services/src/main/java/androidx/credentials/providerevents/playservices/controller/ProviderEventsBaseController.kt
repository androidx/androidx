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

package androidx.credentials.providerevents.playservices.controller

import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver

/** Holds all non type specific details shared by the controllers. */
internal class ProviderEventsBaseController() {
    companion object {

        // Generic controller request code used by all controllers
        @JvmStatic internal val CONTROLLER_REQUEST_CODE: Int = 1
        /** ---- Data Constants to pass between the controllers and the hidden activity---- */

        // error name for unknown import error
        const val IMPORT_UNKNOWN = "IMPORT_UNKNOWN"

        // Key for the result intent to send back to the controller
        const val RESULT_DATA_TAG = "RESULT_DATA"

        // Key for the actual parcelable type sent to the hidden activity
        const val EXTRA_CREDENTIAL_TRANSFER_INTENT = "EXTRA_CREDENTIAL_TRANSFER_INTENT"

        // Key for the failure boolean sent back from hidden activity to controller
        const val FAILURE_RESPONSE_TAG = "FAILURE_RESPONSE"

        // Key for the exception type sent back from hidden activity to controllers if error
        const val EXCEPTION_TYPE_TAG = "EXCEPTION_TYPE"

        // Key for an error message propagated from hidden activity to controllers
        const val EXCEPTION_MESSAGE_TAG = "EXCEPTION_MESSAGE"

        // Key for the activity request code from controllers to activity
        const val ACTIVITY_REQUEST_CODE_TAG = "ACTIVITY_REQUEST_CODE"

        // Key for the result receiver sent from controller to activity
        const val EXTRA_RESULT_RECEIVER = "RESULT_RECEIVER"

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
    }
}
