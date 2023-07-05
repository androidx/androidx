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
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SavePasswordRequest
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions

/** An activity used to ensure all required API versions work as intended. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("Deprecation", "ForbiddenSuperClass")
open class HiddenActivity : Activity() {

  private var resultReceiver: ResultReceiver? = null
  private var mWaitingForActivityResult = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    overridePendingTransition(0, 0)
    val type: String? = intent.getStringExtra(CredentialProviderBaseController.TYPE_TAG)
    resultReceiver = intent.getParcelableExtra(CredentialProviderBaseController.RESULT_RECEIVER_TAG)

    if (resultReceiver == null) {
      finish()
    }

    restoreState(savedInstanceState)
    if (mWaitingForActivityResult) {
      return
      // Past call still active
    }

    when (type) {
      CredentialProviderBaseController.BEGIN_SIGN_IN_TAG -> {
        handleBeginSignIn(intent, resultReceiver)
      }
      CredentialProviderBaseController.CREATE_PASSWORD_TAG -> {
        handleCreatePassword(intent, resultReceiver)
      }
      CredentialProviderBaseController.CREATE_PUBLIC_KEY_CREDENTIAL_TAG -> {
        handleCreatePublicKeyCredential(intent, resultReceiver)
      }
      else -> {
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

  private fun handleBeginSignIn(intent: Intent, resultReceiver: ResultReceiver?) {
    val params: BeginSignInRequest? =
      intent.getParcelableExtra(CredentialProviderBaseController.REQUEST_TAG)
    val requestCode: Int =
      intent.getIntExtra(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, DEFAULT_VALUE)

    if (intent.hasExtra(CredentialProviderBaseController.REQUEST_TAG) && resultReceiver != null) {
      val result =
        GmsCoreUtils.handleBeginSignIn(
          Identity.getSignInClient(this),
          resultReceiver,
          params!!,
          requestCode,
          this
        )
      mWaitingForActivityResult = result.waitingForActivityResult
      if (result.hasFinished) {
        finish()
      }
    }
  }

  private fun handleCreatePassword(intent: Intent, resultReceiver: ResultReceiver?) {
    val params: SavePasswordRequest? =
      intent.getParcelableExtra(CredentialProviderBaseController.REQUEST_TAG)
    val requestCode: Int =
      intent.getIntExtra(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, DEFAULT_VALUE)

    if (intent.hasExtra(CredentialProviderBaseController.REQUEST_TAG) && resultReceiver != null) {
      val result =
        GmsCoreUtils.handleCreatePassword(
          Identity.getCredentialSavingClient(this),
          resultReceiver,
          params!!,
          requestCode,
          this
        )
      mWaitingForActivityResult = result.waitingForActivityResult
      if (result.hasFinished) {
        finish()
      }
    }
  }

  private fun handleCreatePublicKeyCredential(intent: Intent, resultReceiver: ResultReceiver?) {
    val fidoRegistrationRequest: PublicKeyCredentialCreationOptions? =
      intent.getParcelableExtra(CredentialProviderBaseController.REQUEST_TAG)
    val requestCode: Int =
      intent.getIntExtra(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, DEFAULT_VALUE)

    if (intent.hasExtra(CredentialProviderBaseController.REQUEST_TAG) && resultReceiver != null) {
      val result =
        GmsCoreUtils.handleCreatePublicKeyCredential(
          Fido.getFido2ApiClient(this),
          resultReceiver,
          fidoRegistrationRequest!!,
          requestCode,
          this
        )
      mWaitingForActivityResult = result.waitingForActivityResult
      if (result.hasFinished) {
        finish()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putBoolean(KEY_AWAITING_RESULT, mWaitingForActivityResult)
    super.onSaveInstanceState(outState)
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
    private const val TAG = "HiddenActivity"
    private const val KEY_AWAITING_RESULT = "androidx.credentials.playservices.AWAITING_RESULT"
  }
}
