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

package androidx.biometric.internal.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.fragment.app.Fragment

/**
 * A headless [Fragment] that is responsible for launching the confirm device credential Settings
 * activity and handling its result.
 *
 * This fragment is used as a compatibility layer for `BiometricPrompt`'s `authenticate` method,
 * allowing it to be called late in the lifecycle (e.g., in an `onClick` listener) where a direct
 * call to `registerForActivityResult` would not be permitted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ConfirmCredentialCallerFragment : Fragment() {

    internal companion object {
        private const val ARG_HOST_ACTIVITY: String = "host_activity"
        private const val ARG_CONFIRM_CREDENTIAL_INTENT = "confirm_credential_intent"

        /**
         * Creates a new instance of [ConfirmCredentialCallerFragment].
         *
         * @param hostedInActivity `true` if this fragment is hosted directly by an activity.
         * @param credentialIntent The `Intent` to launch the confirm credential screen.
         * @return A new fragment instance.
         */
        fun newInstance(
            hostedInActivity: Boolean,
            credentialIntent: Intent,
        ): ConfirmCredentialCallerFragment {
            val fragment = ConfirmCredentialCallerFragment()
            val args = Bundle()
            args.putBoolean(ARG_HOST_ACTIVITY, hostedInActivity)
            args.putParcelable(ARG_CONFIRM_CREDENTIAL_INTENT, credentialIntent)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var viewModel: AuthenticationViewModel

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleConfirmCredentialResult(requireContext(), viewModel, result.resultCode)
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getCallerViewModelForFragment(isHostedInActivity())

        if (savedInstanceState != null) {
            return
        }
        getCredentialIntent().flags =
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        launcher.launch(getCredentialIntent())
    }

    private fun isHostedInActivity(): Boolean {
        return requireArguments().getBoolean(ARG_HOST_ACTIVITY, true)
    }

    @Suppress("Deprecation")
    private fun getCredentialIntent(): Intent {
        return requireArguments().getParcelable(ARG_CONFIRM_CREDENTIAL_INTENT)
            ?: throw IllegalStateException("Confirm credential intent cannot be null")
    }
}
