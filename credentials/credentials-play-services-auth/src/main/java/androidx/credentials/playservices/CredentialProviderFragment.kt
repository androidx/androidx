/*
 * Copyright 2023 The Android Open Source Project
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

import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import androidx.annotation.RestrictTo
import androidx.credentials.playservices.controllers.CredentialProviderBaseController
import androidx.fragment.app.Fragment

/** A fragment used if we are passed a fragment activity. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@Suppress("Deprecation")
open class CredentialProviderFragment : Fragment() {

  private var resultReceiver: ResultReceiver? = null
  private var mWaitingForActivityResult = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    resultReceiver = getResultReceiver()
    if (resultReceiver == null) {
      return
    }

    restoreState(savedInstanceState)
    if (mWaitingForActivityResult) {
      return // Past call still active
    }
  }

  private fun getResultReceiver(): ResultReceiver? {
    if (getArguments() == null) {
      return null
    }

    return getArguments()!!.getParcelable(CredentialProviderBaseController.RESULT_RECEIVER_TAG)
      as? ResultReceiver
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
    val bundle = Bundle()
    bundle.putBoolean(CredentialProviderBaseController.FAILURE_RESPONSE_TAG, false)
    bundle.putInt(CredentialProviderBaseController.ACTIVITY_REQUEST_CODE_TAG, requestCode)
    bundle.putParcelable(CredentialProviderBaseController.RESULT_DATA_TAG, data)
    resultReceiver?.send(resultCode, bundle)
    mWaitingForActivityResult = false
  }

  companion object {
    private const val TAG = "CredentialProviderFragment"
    private const val KEY_AWAITING_RESULT = "androidx.credentials.playservices.AWAITING_RESULT"

    fun createFrom(resultReceiver: ResultReceiver): CredentialProviderFragment {
      val f = CredentialProviderFragment()

      // Supply index input as an argument.
      val args = Bundle()
      args.putParcelable(CredentialProviderBaseController.RESULT_RECEIVER_TAG, resultReceiver)
      f.setArguments(args)

      return f
    }
  }
}
