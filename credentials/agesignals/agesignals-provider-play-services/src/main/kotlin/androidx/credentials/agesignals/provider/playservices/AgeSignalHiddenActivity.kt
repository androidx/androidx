/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals.provider.playservices

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.content.IntentCompat
import androidx.credentials.agesignals.AgeSignalResultHandler
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException

/**
 * An activity used to trampoline interactive flows for Google Play Services age signal requests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("ForbiddenSuperClass")
public class AgeSignalHiddenActivity : Activity() {

    private var resultReceiver: ResultReceiver? = null
    private var waitingForActivityResult = false

    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        resultReceiver =
            IntentCompat.getParcelableExtra(
                intent,
                EXTRA_RESULT_RECEIVER,
                ResultReceiver::class.java,
            )
        if (resultReceiver == null) {
            finish()
            return
        }

        waitingForActivityResult =
            savedInstanceState?.getBoolean(KEY_AWAITING_RESULT, false) ?: false
        if (waitingForActivityResult) {
            return
        }

        val pendingIntent: PendingIntent? =
            IntentCompat.getParcelableExtra(intent, EXTRA_PENDING_INTENT, PendingIntent::class.java)
        if (pendingIntent != null) {
            waitingForActivityResult = true
            try {
                startIntentSenderForResult(
                    pendingIntent.intentSender,
                    /* requestCode = */ REQUEST_CODE,
                    /* fillInIntent = */ null,
                    /* flagsMask = */ 0,
                    /* flagsValues = */ 0,
                    /* extraFlags = */ 0,
                    /* options = */ null,
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start intent sender", e)
                reportFailureAndFinish("Failed to start intent sender: ${e.message}")
            }
        } else {
            reportFailureAndFinish("Failed to launch UI flow: missing PendingIntent.")
        }
    }

    private fun reportFailureAndFinish(errMsg: String) {
        waitingForActivityResult = false
        resultReceiver?.send(
            RESULT_CANCELED,
            AgeSignalResultHandler.toExceptionBundle(GetAgeRangeUnknownException(errMsg)),
        )
        resultReceiver = null
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AWAITING_RESULT, waitingForActivityResult)
        super.onSaveInstanceState(outState)
    }

    @Suppress("deprecation")
    override fun finish() {
        notifyCanceledIfWaiting()
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        if (isFinishing) {
            notifyCanceledIfWaiting()
        }
        super.onDestroy()
    }

    private fun notifyCanceledIfWaiting() {
        if (waitingForActivityResult) {
            waitingForActivityResult = false
            resultReceiver?.send(RESULT_CANCELED, null)
            resultReceiver = null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivityResult(requestCode, resultCode, data)
    }

    @VisibleForTesting
    internal fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        waitingForActivityResult = false
        if (requestCode != REQUEST_CODE) {
            Log.w(TAG, "Received onActivityResult with unrecognized requestCode: $requestCode")
            reportFailureAndFinish(
                "Received onActivityResult with unrecognized requestCode: $requestCode"
            )
            return
        }
        resultReceiver?.send(resultCode, data?.extras)
        resultReceiver = null
        finish()
    }

    internal companion object {
        private const val TAG = "AgeSignalHiddenActivity"
        private const val KEY_AWAITING_RESULT =
            "androidx.credentials.agesignals.provider.playservices.AWAITING_RESULT"
        internal const val REQUEST_CODE = 1

        internal const val EXTRA_RESULT_RECEIVER =
            "androidx.credentials.agesignals.provider.playservices.EXTRA_RESULT_RECEIVER"

        internal const val EXTRA_PENDING_INTENT =
            "androidx.credentials.agesignals.provider.playservices.EXTRA_PENDING_INTENT"
    }
}
