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
import android.content.IntentSender
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.CREATE_INTERRUPTED
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.CREATE_UNKNOWN
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.GET_INTERRUPTED
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.GET_NO_CREDENTIALS
import androidx.credentials.playservices.controllers.CredentialProviderBaseController.Companion.GET_UNKNOWN
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.CredentialSavingClient
import com.google.android.gms.auth.api.identity.SavePasswordRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions

/** A util class for interacting with GmsCore. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("Deprecation", "ForbiddenSuperClass")
open class GmsCoreUtils {

  class GmsCoreUtilsResult(var waitingForActivityResult: Boolean, var hasFinished: Boolean)

  internal companion object {
    private const val TAG = "GmsCoreUtils"

    const val DEFAULT_REQUEST_CODE = 1

    class FragmentCreationException() : Exception("Failed to create exception")

    fun handleCreatePublicKeyCredential(
      apiClient: Fido2ApiClient,
      resultReceiver: ResultReceiver,
      fidoRegistrationRequest: PublicKeyCredentialCreationOptions?,
      requestCode: Int,
      activity: Activity
    ): GmsCoreUtilsResult {
      var waitingForActivityResult = false
      var hasActivityFinished = false
      var fragment = setupFragmentActivity(activity, resultReceiver)

      fidoRegistrationRequest?.let {
        apiClient
          .getRegisterPendingIntent(fidoRegistrationRequest)
          .addOnSuccessListener { result: PendingIntent ->
            try {
              startIntentSender(
                activity,
                result.intentSender,
                requestCode,
                fragment,
              )
            } catch (e: IntentSender.SendIntentException) {
              setupFailure(
                resultReceiver,
                CREATE_UNKNOWN,
                "During public key credential, found IntentSender " +
                  "failure on public key creation: ${e.message}"
              )
            }
          }
          .addOnFailureListener { e: Exception ->
            var errName: String = CREATE_UNKNOWN
            if (e is ApiException && e.statusCode in CredentialProviderBaseController.retryables) {
              errName = CREATE_INTERRUPTED
            }
            setupFailure(
              resultReceiver,
              errName,
              "During create public key credential, fido registration " + "failure: ${e.message}"
            )
          }
      }
        ?: run {
          Log.w(
            TAG,
            "During create public key credential, request is null, so nothing to " +
              "launch for public key credentials"
          )
          hasActivityFinished = true
        }
      return GmsCoreUtilsResult(waitingForActivityResult, hasActivityFinished)
    }

    fun handleBeginSignIn(
      apiClient: SignInClient,
      resultReceiver: ResultReceiver,
      params: BeginSignInRequest?,
      requestCode: Int,
      activity: Activity
    ): GmsCoreUtilsResult {
      var waitingForActivityResult = false
      var hasFinished = false
      var fragment = setupFragmentActivity(activity, resultReceiver)

      params?.let {
        apiClient
          .beginSignIn(params)
          .addOnSuccessListener {
            try {
              waitingForActivityResult = true
              startIntentSender(
                activity,
                it.pendingIntent.intentSender,
                requestCode,
                fragment,
              )
            } catch (e: IntentSender.SendIntentException) {
              setupFailure(
                resultReceiver,
                GET_UNKNOWN,
                "During begin sign in, one tap ui intent sender " + "failure: ${e.message}"
              )
            }
          }
          .addOnFailureListener { e: Exception ->
            var errName: String = GET_NO_CREDENTIALS
            if (e is ApiException && e.statusCode in CredentialProviderBaseController.retryables) {
              errName = GET_INTERRUPTED
            }
            setupFailure(
              resultReceiver,
              errName,
              "During begin sign in, failure response from one tap: ${e.message}"
            )
          }
      }
        ?: run {
          Log.i(
            TAG,
            "During begin sign in, params is null, nothing to launch for " + "begin sign in"
          )
          hasFinished = true
        }
      return GmsCoreUtilsResult(waitingForActivityResult, hasFinished)
    }

    fun handleCreatePassword(
      apiClient: CredentialSavingClient,
      resultReceiver: ResultReceiver,
      params: SavePasswordRequest?,
      requestCode: Int,
      activity: Activity
    ): GmsCoreUtilsResult {
      var waitingForActivityResult = false
      var hasFinished = false
      var fragment = setupFragmentActivity(activity, resultReceiver)

      params?.let {
        apiClient
          .savePassword(params)
          .addOnSuccessListener {
            try {
              waitingForActivityResult = true
              startIntentSender(
                activity,
                it.pendingIntent.intentSender,
                requestCode,
                fragment,
              )
            } catch (e: IntentSender.SendIntentException) {
              setupFailure(
                resultReceiver,
                CREATE_UNKNOWN,
                "During save password, found UI intent sender " + "failure: ${e.message}"
              )
            }
          }
          .addOnFailureListener { e: Exception ->
            var errName: String = CREATE_UNKNOWN
            if (e is ApiException && e.statusCode in CredentialProviderBaseController.retryables) {
              errName = CREATE_INTERRUPTED
            }
            setupFailure(
              resultReceiver,
              errName,
              "During save password, found " + "password failure response from one tap ${e.message}"
            )
          }
      }
        ?: run {
          Log.i(
            TAG,
            "During save password, params is null, nothing to launch for create" + " password"
          )
          hasFinished = true
        }
      return GmsCoreUtilsResult(waitingForActivityResult, hasFinished)
    }

    private fun setupFragmentActivity(
      activity: Activity,
      resultReceiver: ResultReceiver
    ): Fragment? {
      if (activity is FragmentActivity) {
        val fragment = CredentialProviderFragment.createFrom(resultReceiver)
        val manager = activity.getSupportFragmentManager()
        manager.beginTransaction().add(fragment, "credman").commit()

        if (!fragment.isAdded()) {
          throw FragmentCreationException()
        }

        return fragment
      }

      return null
    }

    private fun startIntentSender(
      activity: Activity,
      intentSender: IntentSender,
      requestCode: Int,
      fragment: Fragment?,
    ) {
      if (fragment != null && fragment.isAdded() && activity is FragmentActivity) {
        activity.startIntentSenderFromFragment(
          fragment,
          intentSender,
          requestCode,
          null,
          0,
          0,
          0,
          null,
        )
        return
      }

      activity.startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0, null)
    }

    private fun setupFailure(resultReceiver: ResultReceiver, errName: String, errMsg: String) {
      val bundle = Bundle()
      bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE_TAG, true)
      bundle.putString(CredentialProviderBaseController.EXCEPTION_TYPE_TAG, errName)
      bundle.putString(CredentialProviderBaseController.EXCEPTION_MESSAGE_TAG, errMsg)
      resultReceiver.send(Integer.MAX_VALUE, bundle)
    }
  }
}
