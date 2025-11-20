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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricPrompt
import androidx.biometric.R
import androidx.biometric.internal.viewmodel.AuthenticationViewModel
import androidx.biometric.internal.viewmodel.AuthenticationViewModelFactory
import androidx.biometric.utils.BiometricErrorData
import androidx.biometric.utils.KeyguardUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Runnable

private const val TAG = "ConfirmCredentialUiLauncher"

/*
 * --------------------1.**AuthenticationLauncher API (Preferred)**--------------------
 * These functions use the modern `registerForActivityResult` API. They must be called early
 * in the lifecycle (e.g., property initializer or `onCreate`) before the `STARTED` state.
 * Ideal for consumers who can prepare authentication logic early.
 * -------------------------------------------------------------------------------------
 */
/**
 * Creates a `Runnable` that launches the confirm device credential activity using the preferred,
 * direct `registerForActivityResult` API.
 *
 * **Note:** This function must be called before the Fragment's lifecycle reaches `STARTED`. It is
 * best used as a property initializer or in `onCreate`.
 *
 * @return A `Runnable` that, when executed, will launch the credential confirmation screen.
 */
internal fun Fragment.getConfirmCredentialActivityLauncher(): Runnable {
    val launcher =
        registerForActivityResult(StartActivityForResult()) { result ->
            handleConfirmCredentialResult(requireContext(), this, result.resultCode)
        }

    return Runnable {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            requireContext().launchConfirmCredentialActivity(this, launcher)
        } else {
            Log.w(
                TAG,
                "Attempted to launch ConfirmCredentialActivity when Fragment is not STARTED.",
            )
        }
    }
}

/**
 * Creates a `Runnable` that launches the confirm device credential activity using the preferred,
 * direct `registerForActivityResult` API.
 *
 * **Note:** This function must be called before the Activity's lifecycle reaches `STARTED`. It is
 * best used as a property initializer or in `onCreate`.
 *
 * @return A `Runnable` that, when executed, will launch the credential confirmation screen.
 */
internal fun ComponentActivity.getConfirmCredentialActivityLauncher(): Runnable {
    val launcher =
        registerForActivityResult(StartActivityForResult()) { result ->
            handleConfirmCredentialResult(this, this, result.resultCode)
        }
    return Runnable {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            launchConfirmCredentialActivity(this, launcher)
        } else {
            Log.w(
                TAG,
                "Attempted to launch ConfirmCredentialActivity when Fragment is not STARTED.",
            )
        }
    }
}

/*
 * --------------------2.**BiometricPrompt API (Legacy Compatibility)**--------------------
 * These functions provide a workaround for `registerForActivityResult` lifecycle constraints.
 * They add a temporary, hidden `Fragment` to legally call `registerForActivityResult` late
 * in the lifecycle (e.g., in an `onClick` listener).
 * ----------------------------------------------------------------------------------------
 */

/**
 * Creates a `Runnable` that launches the confirm credential activity via a temporary, hidden
 * fragment.
 *
 * This is a compatibility launcher for cases where authentication might be started "late" (e.g., in
 * an `onClick` listener), after `registerForActivityResult` can no longer be called directly on the
 * host.
 *
 * @return A `Runnable` that, when executed, will launch the credential confirmation screen.
 */
internal fun Fragment.getConfirmCredentialFragmentLauncher(): Runnable {
    return Runnable {
        val intent = requireContext().getConfirmCredentialIntent(this)
        if (intent != null) {
            findOrAddCallerFragment(
                fragmentManager = childFragmentManager,
                fragmentTag = ConfirmCredentialCallerFragment::class.java.name,
            ) {
                ConfirmCredentialCallerFragment.newInstance(false, intent)
            }
        }
    }
}

/**
 * Creates a `Runnable` that launches the confirm credential activity via a temporary, hidden
 * fragment.
 *
 * This is a compatibility launcher for cases where authentication might be started "late" (e.g., in
 * an `onClick` listener), after `registerForActivityResult` can no longer be called directly on the
 * host.
 *
 * @return A `Runnable` that, when executed, will launch the credential confirmation screen.
 */
internal fun FragmentActivity.getConfirmCredentialFragmentLauncher(): Runnable {
    return Runnable {
        val intent = getConfirmCredentialIntent(this)
        if (intent != null) {
            findOrAddCallerFragment(
                fragmentManager = supportFragmentManager,
                fragmentTag = ConfirmCredentialCallerFragment::class.java.name,
            ) {
                ConfirmCredentialCallerFragment.newInstance(true, intent)
            }
        }
    }
}

/*
 * --------------------3.**Compose Component API (Public)**--------------------
 * These functions are for modules like Jetpack Compose that cannot directly use the extension
 * function launchers due to different lifecycle or composition models.
 * ----------------------------------------------------------------------------
 */
/**
 * A public, non-extension function for launching the confirm credential activity.
 *
 * This is provided for use in modules that cannot directly use the extension function launchers,
 * such as Compose.
 *
 * @param viewModelStoreOwner The owner of the ViewModel store.
 * @param confirmCredentialActivityLauncher The launcher for the confirm credential activity.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // public for Compose module
public fun Context.launchConfirmCredentialActivity(
    viewModelStoreOwner: ViewModelStoreOwner,
    confirmCredentialActivityLauncher: ActivityResultLauncher<Intent>,
) {
    val credentialIntent = getConfirmCredentialIntent(viewModelStoreOwner)
    if (credentialIntent != null) {
        credentialIntent.flags =
            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        confirmCredentialActivityLauncher.launch(credentialIntent)
    }
}

/**
 * A public, non-extension function for handling the result from the confirm credential activity.
 *
 * This is provided for use in modules that cannot directly use the extension function launchers,
 * such as Compose.
 *
 * @param context The application context.
 * @param viewModelStoreOwner The owner of the ViewModel store.
 * @param resultCode The result code from the confirm credential activity.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) // public for Compose module
public fun handleConfirmCredentialResult(
    context: Context,
    viewModelStoreOwner: ViewModelStoreOwner,
    resultCode: Int,
) {
    handleConfirmCredentialResult(context, getViewModel(viewModelStoreOwner), resultCode)
}

// -------------------- Result Handlers --------------------
/**
 * Handles the result from the confirm device credential activity.
 *
 * @param context The application context.
 * @param callerViewModel The [AuthenticationViewModel] of the calling component.
 * @param resultCode The result code from the activity.
 */
internal fun handleConfirmCredentialResult(
    context: Context,
    callerViewModel: AuthenticationViewModel,
    resultCode: Int,
) {
    if (resultCode == Activity.RESULT_OK) {
        @BiometricPrompt.AuthenticationResultType val authenticationType: Int
        if (callerViewModel.isUsingKeyguardManagerForBiometricAndCredential) {
            authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN
            callerViewModel.isUsingKeyguardManagerForBiometricAndCredential = false
        } else {
            authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL
        }
        callerViewModel.setAuthenticationResult(
            BiometricPrompt.AuthenticationResult(null, authenticationType)
        )
    } else {
        callerViewModel.setAuthenticationError(
            BiometricErrorData(
                BiometricPrompt.ERROR_USER_CANCELED,
                context.getString(R.string.generic_error_user_canceled),
            )
        )
    }
}

// -------------------- Generic Helpers --------------------
/**
 * Finds or adds a headless fragment to a [FragmentManager] to act as a caller for an activity
 * result.
 *
 * @param fragmentManager The [FragmentManager] to add the fragment to.
 * @param fragmentTag The tag for the fragment.
 * @param fragmentFactory A lambda to create a new instance of the fragment.
 * @return The found or created fragment, or `null` if the fragment manager is unavailable.
 */
private fun <F : Fragment> findOrAddCallerFragment(
    fragmentManager: FragmentManager?,
    fragmentTag: String,
    fragmentFactory: () -> F,
): F? {
    if (fragmentManager == null) {
        Log.e(
            TAG,
            "$fragmentTag: Unable to start authentication . Client fragment manager was null",
        )
        return null
    }
    if (fragmentManager.isStateSaved) {
        Log.e(
            TAG,
            "$fragmentTag: Unable to start authentication. Called after onSaveInstanceState().",
        )
        return null
    }

    @Suppress("UNCHECKED_CAST")
    var biometricFragment: F? = fragmentManager.findFragmentByTag(fragmentTag) as F?

    if (biometricFragment == null) {
        biometricFragment = fragmentFactory()
        fragmentManager
            .beginTransaction()
            .add(biometricFragment, fragmentTag)
            .commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()
    }

    return biometricFragment
}

/**
 * Gets the [AuthenticationViewModel] for a fragment that is acting as a caller.
 *
 * @param hostedInActivity `true` if the fragment is hosted directly by an activity.
 * @return The [AuthenticationViewModel] instance.
 */
internal fun Fragment.getCallerViewModelForFragment(
    hostedInActivity: Boolean
): AuthenticationViewModel {
    val owner: ViewModelStoreOwner =
        if (hostedInActivity) {
            requireActivity()
        } else {
            requireParentFragment()
        }
    return getViewModel(owner)
}

private fun getViewModel(viewModelStoreOwner: ViewModelStoreOwner): AuthenticationViewModel =
    ViewModelProvider(viewModelStoreOwner, AuthenticationViewModelFactory())[
        AuthenticationViewModel::class.java]

/**
 * Creates an [Intent] to launch the confirm device credential activity.
 *
 * @param viewModelStoreOwner The owner of the ViewModel store.
 * @return An [Intent] to launch the confirm credential screen, or `null` if it's not available.
 */
@Suppress("deprecation")
@VisibleForTesting
internal fun Context.getConfirmCredentialIntent(viewModelStoreOwner: ViewModelStoreOwner): Intent? {
    val viewModel: AuthenticationViewModel = getViewModel(viewModelStoreOwner)
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

    val title: CharSequence? = viewModel.title
    val subtitle: CharSequence? = viewModel.subtitle
    val description: CharSequence? = viewModel.description
    val credentialDescription = subtitle ?: description

    val credentialIntent =
        keyguardManager.createConfirmDeviceCredentialIntent(title, credentialDescription)

    if (credentialIntent == null) {
        viewModel.setAuthenticationError(
            BiometricErrorData(
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                getString(R.string.generic_error_no_device_credential),
            )
        )
        return null
    }

    viewModel.isConfirmingDeviceCredential = true
    return credentialIntent
}
