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

package androidx.biometric.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.R
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.KeyguardUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import java.util.concurrent.Executor
import kotlinx.coroutines.Runnable

/**
 * An [AuthenticationHandler] implementation that manages authentication flows primarily delegating
 * to an internal [AuthenticationManager] instance.
 *
 * This handler is tailored for scenarios involving device credentials (like PIN/Pattern/Password),
 * using the provided [confirmCredentialActivityLauncher] to initiate the system's confirm device
 * credential screen
 */
internal class AuthenticationHandlerKeyguardManager(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    viewModel: BiometricViewModel,
    val confirmCredentialActivityLauncher: Runnable,
    clientExecutor: Executor,
    clientAuthenticationCallback: AuthenticationCallback,
) : AuthenticationHandler {
    private val authenticationManager =
        AuthenticationManager(
            context,
            lifecycleOwner,
            viewModel,
            confirmCredentialActivityLauncher,
            clientExecutor,
            clientAuthenticationCallback,
        )

    override fun authenticate(
        info: BiometricPrompt.PromptInfo,
        crypto: BiometricPrompt.CryptoObject?,
    ) {
        authenticationManager.authenticate(info, crypto) { confirmCredentialActivityLauncher.run() }
    }

    override fun cancelAuthentication(canceledFrom: CanceledFrom) {
        authenticationManager.cancelAuthentication(canceledFrom)
    }
}

/**
 * Processes the result returned by the confirm device credential Settings activity.
 *
 * @param resultCode The result code from the Settings activity.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // public for Compose module
public fun handleConfirmCredentialResult(
    context: Context,
    viewModelStoreOwner: ViewModelStoreOwner,
    resultCode: Int,
) {
    val viewModel: BiometricViewModel =
        ViewModelProvider(viewModelStoreOwner)[BiometricViewModel::class.java]
    handleConfirmCredentialResult(context, viewModel, resultCode)
}

@VisibleForTesting
internal fun handleConfirmCredentialResult(
    context: Context,
    viewModel: BiometricViewModel,
    resultCode: Int,
) {
    if (resultCode == Activity.RESULT_OK) {
        @BiometricPrompt.AuthenticationResultType val authenticationType: Int
        if (viewModel.isUsingKeyguardManagerForBiometricAndCredential) {
            // If using KeyguardManager for biometric and credential auth, we don't know which
            // actual authentication type was used.
            authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
            viewModel.setUsingKeyguardManagerForBiometricAndCredential(false)
        } else {
            authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
        }

        // Device credential auth succeeded. This is incompatible with crypto for API <30.
        viewModel.setAuthenticationResult(
            BiometricPrompt.AuthenticationResult(null, /* crypto */ authenticationType)
        )
    } else {
        // Device credential auth failed. Assume this is due to the user canceling.
        viewModel.setAuthenticationError(
            BiometricErrorData(
                BiometricPrompt.ERROR_USER_CANCELED,
                context.getString(R.string.generic_error_user_canceled),
            )
        )
    }
}

internal fun Fragment.getConfirmCredentialActivityLauncher(): Runnable {
    val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleConfirmCredentialResult(requireContext(), this, result.resultCode)
        }
    return Runnable { requireContext().launchConfirmCredentialActivity(this, launcher) }
}

internal fun ComponentActivity.getConfirmCredentialActivityLauncher(): Runnable {
    val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleConfirmCredentialResult(this, this, result.resultCode)
        }
    return Runnable { launchConfirmCredentialActivity(this, launcher) }
}

@RestrictTo(RestrictTo.Scope.LIBRARY) // public for Compose module
public fun Context.launchConfirmCredentialActivity(
    viewModelStoreOwner: ViewModelStoreOwner,
    confirmCredentialActivityLauncher: ActivityResultLauncher<Intent>,
) {
    val credentialIntent = getConfirmCredentialIntent(viewModelStoreOwner)
    if (credentialIntent != null) {
        // Launch a new instance of the confirm device credential Settings activity.
        credentialIntent.setFlags(
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        )
        confirmCredentialActivityLauncher.launch(credentialIntent)
    }
}

internal fun Fragment.getConfirmCredentialFragmentLauncher(): Runnable {
    return Runnable {
        this.requireContext()
            .launchConfirmCredentialActivity(
                viewModelStoreOwner = this,
                fragmentManager = childFragmentManager,
                hostedInActivity = false,
            )
    }
}

internal fun FragmentActivity.getConfirmCredentialFragmentLauncher(): Runnable {
    return Runnable {
        launchConfirmCredentialActivity(
            viewModelStoreOwner = this,
            fragmentManager = supportFragmentManager,
            hostedInActivity = true,
        )
    }
}

private fun Context.launchConfirmCredentialActivity(
    viewModelStoreOwner: ViewModelStoreOwner,
    fragmentManager: FragmentManager?,
    hostedInActivity: Boolean,
) {
    val credentialIntent = getConfirmCredentialIntent(viewModelStoreOwner)
    if (credentialIntent != null) {
        // Launch a new instance of the confirm device credential Settings activity.
        findOrAddConfirmCredentialCallerFragment(
            fragmentManager,
            hostedInActivity,
            credentialIntent,
        )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ConfirmCredentialCallerFragment : Fragment() {

    internal companion object {
        private const val ARG_HOST_ACTIVITY: String = "host_activity"
        private const val ARG_CONFIRM_CREDENTIAL_INTENT = "confirm_credential_intent"

        /**
         * Creates a new instance of [ConfirmCredentialCallerFragment].
         *
         * @return A [ConfirmCredentialCallerFragment].
         */
        fun newInstance(
            hostedInActivity: Boolean,
            credentialIntent: Intent,
        ): ConfirmCredentialCallerFragment {
            val fragment = ConfirmCredentialCallerFragment()
            val args = Bundle()
            args.putBoolean(ARG_HOST_ACTIVITY, hostedInActivity)
            args.putParcelable(ARG_CONFIRM_CREDENTIAL_INTENT, credentialIntent)
            fragment.setArguments(args)
            return fragment
        }
    }

    // The view model for the ongoing authentication session (non-null after onCreate).
    private lateinit var viewModel: BiometricViewModel

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleConfirmCredentialResult(requireContext(), viewModel, result.resultCode)
            parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(isHostedInActivity())

        if (savedInstanceState != null) {
            return
        }
        // Launch a new instance of the confirm device credential Settings activity.
        getCredentialIntent()
            .setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        launcher.launch(getCredentialIntent())
    }

    private fun isHostedInActivity(): Boolean {
        return requireArguments().getBoolean(ARG_HOST_ACTIVITY, true)
    }

    @Suppress("Deprecation")
    private fun getCredentialIntent(): Intent {
        return requireArguments().getParcelable(ARG_CONFIRM_CREDENTIAL_INTENT)!!
    }
}

@Suppress("deprecation")
private fun Context.getConfirmCredentialIntent(viewModelStoreOwner: ViewModelStoreOwner): Intent? {
    val viewModel: BiometricViewModel =
        ViewModelProvider(viewModelStoreOwner)[BiometricViewModel::class.java]
    val keyguardManager = KeyguardUtils.getKeyguardManager(this)
    if (keyguardManager == null) {
        viewModel.setAuthenticationError(
            BiometricErrorData(
                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                getString(R.string.generic_error_no_keyguard),
            )
        )
        return null
    }

    // Pass along the title and subtitle/description from the biometric prompt.
    val title: CharSequence? = viewModel.title
    val subtitle: CharSequence? = viewModel.subtitle
    val description: CharSequence? = viewModel.description
    val credentialDescription = subtitle ?: description

    val credentialIntent =
        keyguardManager.createConfirmDeviceCredentialIntent(title, credentialDescription)

    // A null intent from KeyguardManager means that the device is not secure.
    if (credentialIntent == null) {
        viewModel.setAuthenticationError(
            BiometricErrorData(
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                getString(R.string.generic_error_no_device_credential),
            )
        )
        return null
    }

    viewModel.setConfirmingDeviceCredential(true)
    return credentialIntent
}

/**
 * Returns a [ConfirmCredentialCallerFragment] instance that has been added to an activity or
 * fragment, adding one if necessary.
 *
 * @return An instance of [ConfirmCredentialCallerFragment] associated with the fragment manager.
 */
private fun findOrAddConfirmCredentialCallerFragment(
    fragmentManager: FragmentManager?,
    hostedInActivity: Boolean,
    credentialIntent: Intent,
): ConfirmCredentialCallerFragment? {
    if (fragmentManager == null) {
        Log.e(
            "CDCACallerFragment",
            "Unable to start authentication. Client fragment manager was null.",
        )
        return null
    }
    if (fragmentManager.isStateSaved) {
        Log.e(
            "CDCACallerFragment",
            "Unable to start authentication. Called after onSaveInstanceState().",
        )
        return null
    }
    var biometricFragment: ConfirmCredentialCallerFragment? =
        fragmentManager.findFragmentByTag(ConfirmCredentialCallerFragment::class.java.name)
            as ConfirmCredentialCallerFragment?

    // If the fragment hasn't been added before, add it.
    if (biometricFragment == null) {
        biometricFragment =
            ConfirmCredentialCallerFragment.newInstance(hostedInActivity, credentialIntent)
        fragmentManager
            .beginTransaction()
            .add(biometricFragment, ConfirmCredentialCallerFragment::class.java.name)
            .commitAllowingStateLoss()

        // For the case when onResume() is being called right after authenticate,
        // we need to make sure that all fragment transactions have been committed.
        fragmentManager.executePendingTransactions()
    }

    return biometricFragment
}

/**
 * Gets the biometric view model instance using the host activity or fragment that was given in the
 * constructor.
 *
 * @param hostedInActivity If one of the activity-based constructors was used.
 * @return A biometric view model tied to the lifecycle owner of the fragment.
 */
private fun Fragment.getViewModel(hostedInActivity: Boolean): BiometricViewModel {
    var owner: ViewModelStoreOwner? = if (hostedInActivity) activity else null
    if (owner == null) {
        owner = parentFragment
    }
    checkNotNull(owner) { "view model not found" }
    return ViewModelProvider(owner)[BiometricViewModel::class.java]
}
