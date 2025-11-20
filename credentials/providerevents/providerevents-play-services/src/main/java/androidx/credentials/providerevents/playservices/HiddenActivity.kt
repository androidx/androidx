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

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.RestrictTo
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.reportError
import androidx.credentials.providerevents.playservices.controller.ProviderEventsBaseController.Companion.reportResult

/** An activity used to ensure all required API versions work as intended. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("ForbiddenSuperClass")
public open class HiddenActivity : Activity() {

    private var resultReceiver: ResultReceiver? = null
    private var mWaitingForActivityResult = false

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        resultReceiver =
            intent.getParcelableExtra(ProviderEventsBaseController.Companion.EXTRA_RESULT_RECEIVER)
        if (resultReceiver == null) {
            finish()
        }

        restoreState(savedInstanceState)
        if (mWaitingForActivityResult) {
            return
            // Past call still active
        }
        val pendingIntent: PendingIntent? =
            intent.getParcelableExtra(
                ProviderEventsBaseController.Companion.EXTRA_CREDENTIAL_TRANSFER_INTENT
            )
        if (pendingIntent != null) {
            mWaitingForActivityResult = true
            startIntentSenderForResult(
                pendingIntent.intentSender,
                /* requestCode= */ ProviderEventsBaseController.Companion.CONTROLLER_REQUEST_CODE,
                /* fillInIntent= */ null,
                /* flagsMask= */ 0,
                /* flagsValues= */ 0,
                /* extraFlags= */ 0,
                /* options = */ null,
            )
        } else {
            resultReceiver?.reportError(
                errName = ProviderEventsBaseController.Companion.IMPORT_UNKNOWN,
                errMsg = "Internal error",
            )
            finish()
        }
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mWaitingForActivityResult = savedInstanceState.getBoolean(KEY_AWAITING_RESULT, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AWAITING_RESULT, mWaitingForActivityResult)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultReceiver?.reportResult(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data,
        )
        mWaitingForActivityResult = false
        finish()
    }

    private companion object {
        private const val KEY_AWAITING_RESULT = "androidx.credentials.playservices.AWAITING_RESULT"
    }
}
