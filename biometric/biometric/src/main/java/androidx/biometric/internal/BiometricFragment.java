/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.biometric.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.R;
import androidx.biometric.utils.AuthenticatorUtils;
import androidx.biometric.utils.CryptoObjectUtils;
import androidx.biometric.utils.DeviceUtils;
import androidx.biometric.utils.ErrorUtils;
import androidx.biometric.utils.KeyguardUtils;
import androidx.biometric.utils.PackageUtils;
import androidx.biometric.utils.PromptContentViewUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * A fragment that hosts the system-dependent UI for {@link BiometricPrompt} and coordinates logic
 * for the ongoing authentication session across device configuration changes.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class BiometricFragment extends Fragment {
    private static final String TAG = "BiometricFragment";

    private static final String ARG_HOST_ACTIVITY = "host_activity";
    private static final String ARG_HAS_FINGERPRINT = "has_fingerprint";
    private static final String ARG_HAS_FACE = "has_face";
    private static final String ARG_HAS_IRIS = "has_iris";

    /**
     * Authentication was canceled by the library or framework.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CANCELED_FROM_INTERNAL = 0;

    /**
     * Authentication was canceled by the user (e.g. by pressing the system back button).
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CANCELED_FROM_USER = 1;

    /**
     * Authentication was canceled by the user by pressing the negative button on the prompt.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CANCELED_FROM_NEGATIVE_BUTTON = 2;

    /**
     * Authentication was canceled by the client application via
     * {@link BiometricPrompt#cancelAuthentication()}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CANCELED_FROM_CLIENT = 3;

    /**
     * Authentication was canceled by the user by pressing the more options button on the prompt
     * content.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int CANCELED_FROM_MORE_OPTIONS_BUTTON = 4;

    /**
     * Where authentication was canceled from.
     */
    @IntDef({
        CANCELED_FROM_INTERNAL,
        CANCELED_FROM_USER,
        CANCELED_FROM_NEGATIVE_BUTTON,
        CANCELED_FROM_CLIENT,
        CANCELED_FROM_MORE_OPTIONS_BUTTON
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface CanceledFrom {}

    /**
     * Tag used to identify the {@link FingerprintDialogFragment} attached to the client
     * activity/fragment.
     */
    private static final String FINGERPRINT_DIALOG_FRAGMENT_TAG =
            "androidx.biometric.internal.FingerprintDialogFragment";

    /**
     * The amount of time (in milliseconds) before the flag indicating whether to dismiss the
     * fingerprint dialog instantly can be changed.
     */
    private static final int DISMISS_INSTANTLY_DELAY_MS = 500;

    /**
     * The amount of time (in milliseconds) to wait before dismissing the fingerprint dialog after
     * encountering an error. Ignored if
     * {@link DeviceUtils#shouldHideFingerprintDialog(Context, String)} is {@code true}.
     */
    private static final int HIDE_DIALOG_DELAY_MS = 2000;

    /**
     * The amount of time (in milliseconds) to wait before showing the authentication UI if
     * {@link BiometricViewModel#isDelayingPrompt()} is {@code true}.
     */
    private static final int SHOW_PROMPT_DELAY_MS = 600;

    /**
     * Request code used when launching the confirm device credential Settings activity.
     */
    private static final int REQUEST_CONFIRM_CREDENTIAL = 1;

    /**
     * An executor used by {@link android.hardware.biometrics.BiometricPrompt} to run framework
     * code.
     */
    private static class PromptExecutor implements Executor {
        private final Handler mPromptHandler = new Handler(Looper.getMainLooper());

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PromptExecutor() {}

        @Override
        public void execute(@NonNull Runnable runnable) {
            mPromptHandler.post(runnable);
        }
    }

    /**
     * A runnable with a weak reference to this fragment that can be used to invoke
     * {@link #showPromptForAuthentication()}.
     */
    private static class ShowPromptForAuthenticationRunnable implements Runnable {
        private final @NonNull WeakReference<BiometricFragment> mFragmentRef;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        ShowPromptForAuthenticationRunnable(@Nullable BiometricFragment fragment) {
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void run() {
            if (mFragmentRef.get() != null) {
                mFragmentRef.get().showPromptForAuthentication();
            }
        }
    }

    /**
     * A runnable with a weak reference to a {@link BiometricViewModel} that can be used to invoke
     * {@link BiometricViewModel#setDelayingPrompt(boolean)} with a value of {@code false}.
     */
    private static class StopDelayingPromptRunnable implements Runnable {
        private final @NonNull WeakReference<BiometricViewModel> mViewModelRef;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        StopDelayingPromptRunnable(@Nullable BiometricViewModel viewModel) {
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        public void run() {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get().setDelayingPrompt(false);
            }
        }
    }

    /**
     * A runnable with a weak reference to a {@link BiometricViewModel} that can be used to invoke
     * {@link BiometricViewModel#setIgnoringCancel(boolean)} with a value of {@code false}.
     */
    private static class StopIgnoringCancelRunnable implements Runnable {
        private final @NonNull WeakReference<BiometricViewModel> mViewModelRef;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        StopIgnoringCancelRunnable(@Nullable BiometricViewModel viewModel) {
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        public void run() {
            if (mViewModelRef.get() != null) {
                mViewModelRef.get().setIgnoringCancel(false);
            }
        }
    }

    // The view model for the ongoing authentication session (non-null after onCreate).
    private @Nullable BiometricViewModel mViewModel;
    private @NonNull Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Creates a new instance of {@link BiometricFragment}.
     *
     * @return A {@link BiometricFragment}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static BiometricFragment newInstance(boolean hostedInActivity) {
        final BiometricFragment fragment = new BiometricFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_HOST_ACTIVITY, hostedInActivity);
        fragment.setArguments(args);
        return fragment;
    }

    @VisibleForTesting
    static BiometricFragment newInstance(@NonNull Handler handler,
            @NonNull BiometricViewModel viewModel,
            boolean hostedInActivity, boolean hasFingerprint, boolean hasFace, boolean hasIris) {
        final BiometricFragment fragment = new BiometricFragment();
        final Bundle args = new Bundle();
        fragment.mHandler = handler;
        fragment.mViewModel = viewModel;
        args.putBoolean(ARG_HOST_ACTIVITY, hostedInActivity);
        args.putBoolean(ARG_HAS_FINGERPRINT, hasFingerprint);
        args.putBoolean(ARG_HAS_FACE, hasFace);
        args.putBoolean(ARG_HAS_IRIS, hasIris);
        fragment.setArguments(args);
        return fragment;
    }

    private boolean isHostedInActivity() {
        return getArguments().getBoolean(ARG_HOST_ACTIVITY, true);
    }

    private boolean hasFingerprint() {
        return getArguments().getBoolean(ARG_HAS_FINGERPRINT,
                PackageUtils.hasSystemFeatureFingerprint(getContext()));
    }

    private boolean hasFace() {
        return getArguments().getBoolean(ARG_HAS_FACE,
                PackageUtils.hasSystemFeatureFace(getContext()));
    }

    private boolean hasIris() {
        return getArguments().getBoolean(ARG_HAS_IRIS,
                PackageUtils.hasSystemFeatureIris(getContext()));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mViewModel == null) {
            mViewModel = BiometricPrompt.getViewModel(this, isHostedInActivity());
        }
        connectViewModel();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Some device credential implementations in API 29 cause the prompt to receive a cancel
        // signal immediately after it's shown (b/162022588).
        // TODO(b/162022588): mViewModel.info hasn't been set. So isDeviceCredentialAllowed()
        //  check will always be false. Reproduce the bug and fix it.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                && AuthenticatorUtils.isDeviceCredentialAllowed(
                mViewModel.getAllowedAuthenticators())) {
            mViewModel.setIgnoringCancel(true);
            mHandler.postDelayed(new StopIgnoringCancelRunnable(mViewModel), 250L);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // TODO(b/349214064): When removing BiometricFragment, leverage the client's lifecycle
        //  observer to cancel authentication when the client enters the background.
        if (mViewModel.isPromptShowing() && !mViewModel.isConfirmingDeviceCredential()
                && isPermanentRemoving()) {
            cancelAuthentication(BiometricFragment.CANCELED_FROM_INTERNAL);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    // TODO(b/178855209): Move to AuthenticationInternalHelper
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONFIRM_CREDENTIAL) {
            mViewModel.setConfirmingDeviceCredential(false);
            handleConfirmCredentialResult(resultCode);
        }
    }

    /**
     * Connects the {@link BiometricViewModel} for the ongoing authentication session to this
     * fragment.
     */
    private void connectViewModel() {
        mViewModel.getAuthenticationResult().observe(this,
                authenticationResult -> {
                    if (authenticationResult != null) {
                        onAuthenticationSucceeded(authenticationResult);
                        mViewModel.setAuthenticationResult(null);
                    }
                });

        mViewModel.getAuthenticationError().observe(this,
                authenticationError -> {
                    if (authenticationError != null) {
                        onAuthenticationError(
                                authenticationError.getErrorCode(),
                                authenticationError.getErrorMessage());
                        mViewModel.setAuthenticationError(null);
                    }
                });

        mViewModel.getAuthenticationHelpMessage().observe(this,
                authenticationHelpMessage -> {
                    if (authenticationHelpMessage != null) {
                        onAuthenticationHelp(authenticationHelpMessage);
                        mViewModel.setAuthenticationError(null);
                    }
                });

        mViewModel.isAuthenticationFailurePending().observe(this,
                authenticationFailurePending -> {
                    if (authenticationFailurePending) {
                        onAuthenticationFailed();
                        mViewModel.setAuthenticationFailurePending(false);
                    }
                });

        mViewModel.isNegativeButtonPressPending().observe(this,
                negativeButtonPressPending -> {
                    if (negativeButtonPressPending) {
                        if (isManagingDeviceCredentialButton()) {
                            onDeviceCredentialButtonPressed();
                        } else {
                            onCancelButtonPressed();
                        }
                        mViewModel.setNegativeButtonPressPending(false);
                    }
                });

        mViewModel.isMoreOptionsButtonPressPending().observe(this,
                moreOptionsButtonPressPending -> {
                    if (moreOptionsButtonPressPending) {
                        onMoreOptionsButtonPressed();
                        mViewModel.setMoreOptionsButtonPressPending(false);
                    }
                }
        );

        mViewModel.isFingerprintDialogCancelPending().observe(this,
                fingerprintDialogCancelPending -> {
                    if (fingerprintDialogCancelPending) {
                        cancelAuthentication(BiometricFragment.CANCELED_FROM_USER);
                        dismiss();
                        mViewModel.setFingerprintDialogCancelPending(false);
                    }
                });
    }

    /**
     * Shows the prompt UI to the user and begins an authentication session.
     *
     * @param info   An object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void authenticate(
            BiometricPrompt.@NonNull PromptInfo info,
            BiometricPrompt.@Nullable CryptoObject crypto) {
        // PromptInfo has to be set prior to others.
        mViewModel.setPromptInfo(info);

        mViewModel.setIsIdentityCheckAvailable(
                BiometricManager.from(requireContext()).isIdentityCheckAvailable());

        mViewModel.setCryptoObject(crypto);

        if (isManagingDeviceCredentialButton()) {
            mViewModel.setNegativeButtonTextOverride(
                    getString(R.string.confirm_device_credential_password));
        } else {
            // Don't override the negative button text from the client.
            mViewModel.setNegativeButtonTextOverride(null);
        }

        // Fall back to device credential immediately if no known biometrics are available.
        if (isKeyguardManagerNeededForNoBiometric()) {
            mViewModel.setAwaitingResult(true);
            launchConfirmCredentialActivity();
            return;
        }

        // Check if we should delay showing the authentication prompt.
        if (mViewModel.isDelayingPrompt()) {
            mHandler.postDelayed(
                    new ShowPromptForAuthenticationRunnable(this), SHOW_PROMPT_DELAY_MS);
        } else {
            showPromptForAuthentication();
        }
    }

    /**
     * Shows either the framework biometric prompt or fingerprint UI dialog to the user and begins
     * authentication.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void showPromptForAuthentication() {
        if (!mViewModel.isPromptShowing()) {
            if (getContext() == null) {
                Log.w(TAG, "Not showing biometric prompt. Context is null.");
                return;
            }

            mViewModel.setPromptShowing(true);
            mViewModel.setAwaitingResult(true);
            if (isKeyguardManagerNeededForBiometricAndCredential()) {
                launchConfirmCredentialActivity();
            } else if (isUsingFingerprintDialog()) {
                showFingerprintDialogForAuthentication();
            } else {
                showBiometricPromptForAuthentication();
            }
        }
    }

    /**
     * Shows the fingerprint dialog UI to the user and begins authentication.
     */
    @SuppressWarnings("deprecation")
    private void showFingerprintDialogForAuthentication() {
        final Context context = requireContext().getApplicationContext();
        androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManagerCompat =
                androidx.core.hardware.fingerprint.FingerprintManagerCompat.from(context);
        final int errorCode = checkForFingerprintPreAuthenticationErrors(fingerprintManagerCompat);
        if (errorCode != BiometricPrompt.BIOMETRIC_SUCCESS) {
            sendErrorAndDismiss(
                    errorCode, ErrorUtils.getFingerprintErrorString(context, errorCode));
            return;
        }

        if (isAdded()) {
            mViewModel.setFingerprintDialogDismissedInstantly(true);
            if (!DeviceUtils.shouldHideFingerprintDialog(context, Build.MODEL)) {
                mHandler.postDelayed(
                        () -> mViewModel.setFingerprintDialogDismissedInstantly(false),
                        DISMISS_INSTANTLY_DELAY_MS);

                final FingerprintDialogFragment dialog =
                        FingerprintDialogFragment.newInstance(isHostedInActivity());
                dialog.show(getParentFragmentManager(), FINGERPRINT_DIALOG_FRAGMENT_TAG);
            }

            mViewModel.setCanceledFrom(CANCELED_FROM_INTERNAL);

            authenticateWithFingerprint(fingerprintManagerCompat, context);
        }
    }

    /**
     * Shows the framework {@link android.hardware.biometrics.BiometricPrompt} UI to the user and
     * begins authentication.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private void showBiometricPromptForAuthentication() {
        final android.hardware.biometrics.BiometricPrompt.Builder builder =
                Api28Impl.createPromptBuilder(requireContext().getApplicationContext());

        final CharSequence title = mViewModel.getTitle();
        final CharSequence subtitle = mViewModel.getSubtitle();
        final CharSequence description = mViewModel.getDescription();
        if (title != null) {
            Api28Impl.setTitle(builder, title);
        }
        if (subtitle != null) {
            Api28Impl.setSubtitle(builder, subtitle);
        }
        if (description != null) {
            Api28Impl.setDescription(builder, description);
        }

        final CharSequence negativeButtonText = mViewModel.getNegativeButtonText();
        if (!TextUtils.isEmpty(negativeButtonText)) {
            Api28Impl.setNegativeButton(
                    builder,
                    negativeButtonText,
                    mViewModel.getClientExecutor(),
                    mViewModel.getNegativeButtonListener());
        }

        // Set the confirmation required option introduced in Android 10 (API 29).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.setConfirmationRequired(builder, mViewModel.isConfirmationRequired());
        }

        // Set or emulate the allowed authenticators option introduced in Android 11 (API 30).
        @BiometricManager.AuthenticatorTypes final int authenticators =
                mViewModel.getAllowedAuthenticators();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Api30Impl.setAllowedAuthenticators(builder, authenticators);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Api29Impl.setDeviceCredentialAllowed(
                    builder, AuthenticatorUtils.isDeviceCredentialAllowed(authenticators));
        }

        // Set the custom biometric prompt features introduced in Android 15 (API 35).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            final int logoRes = mViewModel.getLogoRes();
            final Bitmap logoBitmap = mViewModel.getLogoBitmap();
            final String logoDescription = mViewModel.getLogoDescription();
            final android.hardware.biometrics.PromptContentView contentView =
                    PromptContentViewUtils.wrapForBiometricPrompt(mViewModel.getContentView(),
                            mViewModel.getClientExecutor(),
                            mViewModel.getMoreOptionsButtonListener());
            if (logoRes != -1) {
                Api35Impl.setLogoRes(builder, logoRes);
            }
            if (logoBitmap != null) {
                Api35Impl.setLogoBitmap(builder, logoBitmap);
            }
            if (logoDescription != null && !logoDescription.isEmpty()) {
                Api35Impl.setLogoDescription(builder, logoDescription);
            }
            if (contentView != null) {
                Api35Impl.setContentView(builder, contentView);
            }
        }

        authenticateWithBiometricPrompt(Api28Impl.buildPrompt(builder), getContext());
    }

    /**
     * Requests user authentication with the given fingerprint manager.
     *
     * @param fingerprintManager The fingerprint manager that will be used for authentication.
     * @param context            The application context.
     */
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    void authenticateWithFingerprint(
            androidx.core.hardware.fingerprint.@NonNull FingerprintManagerCompat fingerprintManager,
            @NonNull Context context) {
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.CryptoObject crypto =
                CryptoObjectUtils.wrapForFingerprintManager(mViewModel.getCryptoObject());
        final androidx.core.os.CancellationSignal cancellationSignal =
                mViewModel.getCancellationSignalProvider().getFingerprintCancellationSignal();
        final androidx.core.hardware.fingerprint.FingerprintManagerCompat.AuthenticationCallback
                callback = mViewModel.getAuthenticationCallbackProvider()
                .getFingerprintCallback();

        try {
            fingerprintManager.authenticate(
                    crypto, 0 /* flags */, cancellationSignal, callback, null /* handler */);
        } catch (NullPointerException e) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with fingerprint.", e);
            final int errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE;
            sendErrorAndDismiss(
                    errorCode, ErrorUtils.getFingerprintErrorString(context, errorCode));
        }
    }

    /**
     * Requests user authentication with the given framework biometric prompt.
     *
     * @param biometricPrompt The biometric prompt that will be used for authentication.
     * @param context         An application or activity context.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    @VisibleForTesting
    void authenticateWithBiometricPrompt(
            android.hardware.biometrics.@NonNull BiometricPrompt biometricPrompt,
            @Nullable Context context) {
        final android.hardware.biometrics.BiometricPrompt.CryptoObject cryptoObject =
                CryptoObjectUtils.wrapForBiometricPrompt(mViewModel.getCryptoObject());
        final android.os.CancellationSignal cancellationSignal =
                mViewModel.getCancellationSignalProvider().getBiometricCancellationSignal();
        final Executor executor = new PromptExecutor();
        final android.hardware.biometrics.BiometricPrompt.AuthenticationCallback callback =
                mViewModel.getAuthenticationCallbackProvider().getBiometricCallback();

        try {
            if (cryptoObject == null) {
                Api28Impl.authenticate(biometricPrompt, cancellationSignal, executor, callback);
            } else {
                Api28Impl.authenticate(
                        biometricPrompt, cryptoObject, cancellationSignal, executor, callback);
            }
        } catch (NullPointerException e) {
            // Catch and handle NPE if thrown by framework call to authenticate() (b/151316421).
            Log.e(TAG, "Got NPE while authenticating with biometric prompt.", e);
            final int errorCode = BiometricPrompt.ERROR_HW_UNAVAILABLE;
            final String errorString = context != null
                    ? context.getString(R.string.default_error_msg)
                    : "";
            sendErrorAndDismiss(errorCode, errorString);
        }
    }

    /**
     * Cancels the ongoing authentication session and sends an error to the client callback.
     *
     * @param canceledFrom Where authentication was canceled from.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void cancelAuthentication(@CanceledFrom int canceledFrom) {
        if (canceledFrom != CANCELED_FROM_CLIENT && mViewModel.isIgnoringCancel()) {
            return;
        }

        if (isUsingFingerprintDialog()) {
            mViewModel.setCanceledFrom(canceledFrom);
            if (canceledFrom == CANCELED_FROM_USER) {
                final int errorCode = BiometricPrompt.ERROR_USER_CANCELED;
                sendErrorToClient(
                        errorCode, ErrorUtils.getFingerprintErrorString(getContext(), errorCode));
            }
        }

        mViewModel.getCancellationSignalProvider().cancel();
    }

    /**
     * Removes this fragment and any associated UI from the client activity/fragment.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void dismiss() {
        dismissFingerprintDialog();

        mViewModel.setPromptShowing(false);

        if (!mViewModel.isConfirmingDeviceCredential() && isAdded()) {
            getParentFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }

        // Wait before showing again to work around a dismissal logic issue on API 29 (b/157783075).
        final Context context = getContext();
        if (context != null && DeviceUtils.shouldDelayShowingPrompt(context, Build.MODEL)) {
            mViewModel.setDelayingPrompt(true);

            mHandler.postDelayed(new StopDelayingPromptRunnable(mViewModel), SHOW_PROMPT_DELAY_MS);
        }
    }

    /**
     * Removes the fingerprint dialog UI from the client activity/fragment.
     */
    private void dismissFingerprintDialog() {
        mViewModel.setPromptShowing(false);

        if (isAdded()) {
            final FragmentManager fragmentManager = getParentFragmentManager();
            final FingerprintDialogFragment fingerprintDialog =
                    (FingerprintDialogFragment) fragmentManager.findFragmentByTag(
                            FINGERPRINT_DIALOG_FRAGMENT_TAG);
            if (fingerprintDialog != null) {
                if (fingerprintDialog.isAdded()) {
                    fingerprintDialog.dismissAllowingStateLoss();
                } else {
                    fragmentManager.beginTransaction().remove(fingerprintDialog)
                            .commitAllowingStateLoss();
                }
            }
        }
    }

    /**
     * Callback that is run when the view model receives a successful authentication result.
     *
     * @param result An object containing authentication-related data.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @VisibleForTesting
    void onAuthenticationSucceeded(BiometricPrompt.@NonNull AuthenticationResult result) {
        sendSuccessAndDismiss(result);
    }

    /**
     * Callback that is run when the view model receives an unrecoverable error result.
     *
     * @param errorCode    An integer ID associated with the error.
     * @param errorMessage A human-readable string that describes the error.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @VisibleForTesting
    void onAuthenticationError(int errorCode, @Nullable CharSequence errorMessage) {
        // Ensure we're only sending publicly defined errors.
        final int knownErrorCode = ErrorUtils.toKnownErrorCodeForAuthenticate(errorCode);

        final Context context = getContext();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ErrorUtils.isLockoutError(
                knownErrorCode) && context != null && KeyguardUtils.isDeviceSecuredWithCredential(
                context) && AuthenticatorUtils.isDeviceCredentialAllowed(
                mViewModel.getAllowedAuthenticators())) {
            launchConfirmCredentialActivity();
            return;
        }

        if (isUsingFingerprintDialog()) {
            // Avoid passing a null error string to the client callback.
            final CharSequence errorString = errorMessage != null
                    ? errorMessage
                    : ErrorUtils.getFingerprintErrorString(getContext(), knownErrorCode);

            if (knownErrorCode == BiometricPrompt.ERROR_CANCELED) {
                // User-initiated cancellation errors should already be handled.
                @CanceledFrom final int canceledFrom = mViewModel.getCanceledFrom();
                if (canceledFrom == CANCELED_FROM_INTERNAL
                        || canceledFrom == CANCELED_FROM_CLIENT) {
                    sendErrorToClient(knownErrorCode, errorString);
                }

                dismiss();
            } else {
                if (mViewModel.isFingerprintDialogDismissedInstantly()) {
                    sendErrorAndDismiss(knownErrorCode, errorString);
                } else {
                    showFingerprintErrorMessage(errorString);
                    mHandler.postDelayed(
                            () -> sendErrorAndDismiss(knownErrorCode, errorString),
                            getDismissDialogDelay());
                }

                // Always set this to true. In case the user tries to authenticate again
                // the UI will not be shown.
                mViewModel.setFingerprintDialogDismissedInstantly(true);
            }
        } else {
            final CharSequence errorString = errorMessage != null
                    ? errorMessage
                    : getString(R.string.default_error_msg) + " " + knownErrorCode;
            sendErrorAndDismiss(knownErrorCode, errorString);
        }
    }

    /**
     * Callback that is run when the view model receives a recoverable error or help message.
     *
     * @param helpMessage A human-readable error/help message.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAuthenticationHelp(@NonNull CharSequence helpMessage) {
        if (isUsingFingerprintDialog()) {
            showFingerprintErrorMessage(helpMessage);
        }
    }

    /**
     * Callback that is run when the view model reports a failed authentication attempt.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAuthenticationFailed() {
        if (isUsingFingerprintDialog()) {
            showFingerprintErrorMessage(getString(R.string.fingerprint_not_recognized));
        }
        sendFailureToClient();
    }

    /**
     * Callback that is run when the view model reports that the device credential fallback
     * button has been pressed on the prompt.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onDeviceCredentialButtonPressed() {
        launchConfirmCredentialActivity();
    }

    /**
     * Callback that is run when the view model reports that the cancel button has been pressed on
     * the prompt.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onCancelButtonPressed() {
        final CharSequence negativeButtonText = mViewModel.getNegativeButtonText();

        sendErrorAndDismiss(
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                negativeButtonText != null
                        ? negativeButtonText
                        : getString(R.string.default_error_msg));

        cancelAuthentication(BiometricFragment.CANCELED_FROM_NEGATIVE_BUTTON);
    }

    /**
     * Callback that is run when the view model reports that the more options button has been
     * pressed on the prompt content.
     */
    void onMoreOptionsButtonPressed() {
        sendErrorAndDismiss(BiometricPrompt.ERROR_CONTENT_VIEW_MORE_OPTIONS_BUTTON,
                getString(R.string.content_view_more_options_button_clicked));
        cancelAuthentication(BiometricFragment.CANCELED_FROM_MORE_OPTIONS_BUTTON);
    }

    /**
     * Launches the confirm device credential Settings activity, where the user can authenticate
     * using their PIN, pattern, or password.
     */
    @SuppressWarnings("deprecation")
    private void launchConfirmCredentialActivity() {
        final Context context = getContext();

        // Get the KeyguardManager service in whichever way the platform supports.
        final KeyguardManager keyguardManager =
                context != null ? KeyguardUtils.getKeyguardManager(context) : null;
        if (keyguardManager == null) {
            sendErrorAndDismiss(
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    getString(R.string.generic_error_no_keyguard));
            return;
        }

        // Pass along the title and subtitle/description from the biometric prompt.
        final CharSequence title = mViewModel.getTitle();
        final CharSequence subtitle = mViewModel.getSubtitle();
        final CharSequence description = mViewModel.getDescription();
        final CharSequence credentialDescription = subtitle != null ? subtitle : description;

        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(title,
                credentialDescription);

        // A null intent from KeyguardManager means that the device is not secure.
        if (intent == null) {
            sendErrorAndDismiss(
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                    getString(R.string.generic_error_no_device_credential));
            return;
        }

        mViewModel.setConfirmingDeviceCredential(true);

        // Dismiss the fingerprint dialog before launching the activity.
        if (isUsingFingerprintDialog()) {
            dismissFingerprintDialog();
        }

        // Launch a new instance of the confirm device credential Settings activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivityForResult(intent, REQUEST_CONFIRM_CREDENTIAL);
    }

    /**
     * Processes the result returned by the confirm device credential Settings activity.
     *
     * @param resultCode The result code from the Settings activity.
     */
    // TODO(b/178855209): Move to AuthenticationInternalHelper
    private void handleConfirmCredentialResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            @BiometricPrompt.AuthenticationResultType final int authenticationType;
            if (mViewModel.isUsingKeyguardManagerForBiometricAndCredential()) {
                // If using KeyguardManager for biometric and credential auth, we don't know which
                // actual authentication type was used.
                authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_UNKNOWN;
                mViewModel.setUsingKeyguardManagerForBiometricAndCredential(false);
            } else {
                authenticationType = BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL;
            }

            // Device credential auth succeeded. This is incompatible with crypto for API <30.
            sendSuccessAndDismiss(
                    new BiometricPrompt.AuthenticationResult(
                            null /* crypto */, authenticationType));
        } else {
            // Device credential auth failed. Assume this is due to the user canceling.
            sendErrorAndDismiss(
                    BiometricPrompt.ERROR_USER_CANCELED,
                    getString(R.string.generic_error_user_canceled));
        }
    }

    /**
     * Updates the fingerprint dialog to show an error message to the user.
     *
     * @param errorMessage The error message to show on the dialog.
     */
    private void showFingerprintErrorMessage(@Nullable CharSequence errorMessage) {
        final CharSequence helpMessage = errorMessage != null
                ? errorMessage
                : getString(R.string.default_error_msg);
        mViewModel.setFingerprintDialogState(FingerprintDialogFragment.STATE_FINGERPRINT_ERROR);
        mViewModel.setFingerprintDialogHelpMessage(helpMessage);
    }

    /**
     * Sends a successful authentication result to the client and dismisses the prompt.
     *
     * @param result An object containing authentication-related data.
     *
     * @see #sendSuccessToClient(BiometricPrompt.AuthenticationResult)
     */
    private void sendSuccessAndDismiss(BiometricPrompt.@NonNull AuthenticationResult result) {
        sendSuccessToClient(result);
        dismiss();
    }

    /**
     * Sends an unrecoverable error result to the client and dismisses the prompt.
     *
     * @param errorCode An integer ID associated with the error.
     * @param errorString A human-readable string that describes the error.
     *
     * @see #sendErrorToClient(int, CharSequence)
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void sendErrorAndDismiss(int errorCode, @NonNull CharSequence errorString) {
        sendErrorToClient(errorCode, errorString);
        dismiss();
    }

    /**
     * Sends a successful authentication result to the client callback.
     *
     * @param result An object containing authentication-related data.
     *
     * @see #sendSuccessAndDismiss(BiometricPrompt.AuthenticationResult)
     * @see BiometricPrompt.AuthenticationCallback#onAuthenticationSucceeded(
     *      BiometricPrompt.AuthenticationResult)
     */
    private void sendSuccessToClient(final BiometricPrompt.@NonNull AuthenticationResult result) {
        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Success not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.setAwaitingResult(false);
        mViewModel.getClientExecutor().execute(
                () -> mViewModel.getClientCallback().onAuthenticationSucceeded(result));
    }

    /**
     * Sends an unrecoverable error result to the client callback.
     *
     * @param errorCode   An integer ID associated with the error.
     * @param errorString A human-readable string that describes the error.
     *
     * @see #sendErrorAndDismiss(int, CharSequence)
     * @see BiometricPrompt.AuthenticationCallback#onAuthenticationError(int, CharSequence)
     */
    private void sendErrorToClient(final int errorCode, final @NonNull CharSequence errorString) {
        if (mViewModel.isConfirmingDeviceCredential()) {
            Log.v(TAG, "Error not sent to client. User is confirming their device credential.");
            return;
        }

        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Error not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.setAwaitingResult(false);
        mViewModel.getClientExecutor().execute(
                () -> mViewModel.getClientCallback().onAuthenticationError(errorCode, errorString));
    }

    /**
     * Sends an authentication failure event to the client callback.
     *
     * @see BiometricPrompt.AuthenticationCallback#onAuthenticationFailed()
     */
    private void sendFailureToClient() {
        if (!mViewModel.isAwaitingResult()) {
            Log.w(TAG, "Failure not sent to client. Client is not awaiting a result.");
            return;
        }

        mViewModel.getClientExecutor().execute(
                () -> mViewModel.getClientCallback().onAuthenticationFailed());
    }

    /**
     * Checks for possible error conditions prior to starting fingerprint authentication.
     *
     * @return 0 if there is no error, or a nonzero integer identifying the specific error.
     */
    @SuppressWarnings("deprecation")
    private static int checkForFingerprintPreAuthenticationErrors(
            androidx.core.hardware.fingerprint.FingerprintManagerCompat fingerprintManager) {
        if (!fingerprintManager.isHardwareDetected()) {
            return BiometricPrompt.ERROR_HW_NOT_PRESENT;
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            return BiometricPrompt.ERROR_NO_BIOMETRICS;
        }
        return BiometricPrompt.BIOMETRIC_SUCCESS;
    }

    /**
     * Checks if this fragment is responsible for drawing and handling the result of a device
     * credential fallback button on the prompt.
     *
     * @return Whether this fragment is managing a device credential button for the prompt.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean isManagingDeviceCredentialButton() {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && AuthenticatorUtils.isDeviceCredentialAllowed(
                    mViewModel.getAllowedAuthenticators());
    }

    /**
     * Checks if this fragment should display the fingerprint dialog authentication UI to the user,
     * rather than delegate to the framework {@link android.hardware.biometrics.BiometricPrompt}.
     *
     * @return Whether this fragment should display the fingerprint dialog UI.
     */
    private boolean isUsingFingerprintDialog() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                || isFingerprintDialogNeededForCrypto()
                || isFingerprintDialogNeededForErrorHandling();
    }

    /**
     * Checks if this fragment should display the fingerprint dialog authentication UI for an
     * ongoing crypto-based authentication attempt.
     *
     * @return Whether this fragment should display the fingerprint dialog UI.
     *
     * @see DeviceUtils#shouldUseFingerprintForCrypto(Context, String, String)
     */
    private boolean isFingerprintDialogNeededForCrypto() {
        final Context context = getContext();
        return context != null
                && mViewModel.getCryptoObject() != null
                && DeviceUtils.shouldUseFingerprintForCrypto(
                    context, Build.MANUFACTURER, Build.MODEL);
    }

    /**
     * Checks if this fragment should invoke the fingerprint dialog, rather than the framework
     * biometric prompt, to handle an authentication error.
     *
     * @return Whether this fragment should invoke the fingerprint dialog.
     */
    private boolean isFingerprintDialogNeededForErrorHandling() {
        // On API 28, BiometricPrompt internally calls FingerprintManager#getErrorString(), which
        // requires fingerprint hardware to be present (b/151443237).
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !hasFingerprint();
    }

    private boolean isKeyguardManagerNeededForNoBiometric() {
        final Context context = getContext();

        // On API 29, BiometricPrompt fails to launch the confirm device credential Settings
        // activity if no biometric hardware is present.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q
                && !hasFingerprint()
                && !hasFace()
                && !hasIris()) {
            return true;
        }

        // Launch CDC activity if managing the credential button and if no biometrics are
        // available on the device.
        return isManagingDeviceCredentialButton()
                && BiometricManager.from(context).canAuthenticate(Authenticators.BIOMETRIC_WEAK)
                != BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Checks if this fragment should invoke {@link
     * KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence, CharSequence)} directly to
     * start authentication, rather than explicitly showing a dialog.
     *
     * @return Whether this fragment should use {@link KeyguardManager} directly.
     */
    private boolean isKeyguardManagerNeededForBiometricAndCredential() {
        // Devices from some vendors should use KeyguardManager for authentication if both biometric
        // and credential authenticator types are allowed (on API 29).
        final Context context = getContext();
        if (context == null) {
            return false;
        }

        if (DeviceUtils.isWearOS(context)) {
            // Always use KeyguardManager API for wear OS.
            return true;
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {

            @BiometricManager.AuthenticatorTypes int allowedAuthenticators =
                    mViewModel.getAllowedAuthenticators();

            if (AuthenticatorUtils.isWeakBiometricAllowed(allowedAuthenticators)
                    && AuthenticatorUtils.isDeviceCredentialAllowed(allowedAuthenticators)) {
                mViewModel.setUsingKeyguardManagerForBiometricAndCredential(true);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the client activity is being destroyed and will not be recreated.  This is
     * distinct from a configuration change (like screen rotation), where the activity is
     * destroyed but immediately recreated.
     *
     * @return {@code true} if the activity is being permanently destroyed; {@code false} otherwise.
     */
    // TODO(b/178855209): Check this in helper, which uses ProcessLifecycleOwner instead
    private boolean isPermanentRemoving() {
        final FragmentActivity activity = getActivity();
        return isRemoving() && (activity == null || !activity.isChangingConfigurations());
    }

    /**
     * Gets the amount of time to wait after receiving an unrecoverable error before dismissing the
     * fingerprint dialog and forwarding the error to the client.
     *
     * <p>This method respects the result of
     * {@link DeviceUtils#shouldHideFingerprintDialog(Context, String)} and returns 0 if the latter
     * is {@code true}.
     *
     * @return The delay (in milliseconds) to apply before hiding the fingerprint dialog.
     */
    private int getDismissDialogDelay() {
        Context context = getContext();
        return context != null && DeviceUtils.shouldHideFingerprintDialog(context, Build.MODEL)
                ? 0
                : HIDE_DIALOG_DELAY_MS;
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 15.0 (API 35).
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @SuppressLint("MissingPermission")
    private static class Api35Impl {
        // Prevent instantiation.
        private Api35Impl() {
        }

        /**
         * Sets the prompt content view for the given framework prompt builder.
         *
         * @param builder An instance of
         *                {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param logoRes A drawable resource of the logo that will be shown on the prompt.
         */
        @DoNotInline
        static void setLogoRes(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder, int logoRes) {
            builder.setLogoRes(logoRes);
        }

        /**
         * Sets the prompt content view for the given framework prompt builder.
         *
         * @param builder    An instance of
         *                   {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param logoBitmap A bitmap drawable of the logo that will be shown on the prompt.
         */
        @DoNotInline
        static void setLogoBitmap(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @NonNull Bitmap logoBitmap) {
            builder.setLogoBitmap(logoBitmap);
        }

        /**
         * Sets the prompt content view for the given framework prompt builder.
         *
         * @param builder         An instance of
         *                        {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param logoDescription The content view for the prompt.
         */
        @DoNotInline
        static void setLogoDescription(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                String logoDescription) {
            builder.setLogoDescription(logoDescription);
        }

        /**
         * Sets the prompt content view for the given framework prompt builder.
         *
         * @param builder     An instance of
         *                    {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param contentView The content view for the prompt.
         */
        @DoNotInline
        static void setContentView(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                android.hardware.biometrics.@NonNull PromptContentView contentView) {
            builder.setContentView(contentView);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 11 (API 30).
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private static class Api30Impl {
        // Prevent instantiation.
        private Api30Impl() {}

        /**
         * Sets the allowed authenticator type(s) for the given framework prompt builder.
         *
         * @param builder               An instance of
         *                              {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param allowedAuthenticators A bit field representing allowed authenticator types.
         */
        @DoNotInline
        // This is expected because AndroidX and framework annotation are not identical
        @SuppressWarnings("WrongConstant")
        static void setAllowedAuthenticators(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @BiometricManager.AuthenticatorTypes int allowedAuthenticators) {
            builder.setAllowedAuthenticators(allowedAuthenticators);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 10 (API 29).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private static class Api29Impl {
        // Prevent instantiation.
        private Api29Impl() {}

        /**
         * Sets the "confirmation required" option for the given framework prompt builder.
         *
         * @param builder              An instance of
         *                             {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param confirmationRequired The value for the "confirmation required" option.
         */
        @DoNotInline
        static void setConfirmationRequired(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                boolean confirmationRequired) {
            builder.setConfirmationRequired(confirmationRequired);
        }

        /**
         * Sets the "device credential allowed" option for the given framework prompt builder.
         *
         * @param builder                 An instance of {@link
         *                                android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param deviceCredentialAllowed The value for the "device credential allowed" option.
         */
        @SuppressWarnings("deprecation")
        @DoNotInline
        static void setDeviceCredentialAllowed(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                boolean deviceCredentialAllowed) {
            builder.setDeviceCredentialAllowed(deviceCredentialAllowed);
        }
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 9.0 (API 28).
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private static class Api28Impl {
        // Prevent instantiation.
        private Api28Impl() {}

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         *
         * @param context The application or activity context.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         */
        @DoNotInline
        static android.hardware.biometrics.BiometricPrompt.@NonNull Builder createPromptBuilder(
                @NonNull Context context) {
            return new android.hardware.biometrics.BiometricPrompt.Builder(context);
        }

        /**
         * Sets the title for the given framework prompt builder.
         *
         * @param builder An instance of
         *                {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param title   The title for the prompt.
         */
        @DoNotInline
        static void setTitle(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @NonNull CharSequence title) {
            builder.setTitle(title);
        }

        /**
         * Sets the subtitle for the given framework prompt builder.
         *
         * @param builder  An instance of
         *                 {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param subtitle The subtitle for the prompt.
         */
        @DoNotInline
        static void setSubtitle(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @NonNull CharSequence subtitle) {
            builder.setSubtitle(subtitle);
        }

        /**
         * Sets the description for the given framework prompt builder.
         *
         * @param builder     An instance of
         *                    {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param description The description for the prompt.
         */
        @DoNotInline
        static void setDescription(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @NonNull CharSequence description) {
            builder.setDescription(description);
        }

        /**
         * Sets the negative button text and behavior for the given framework prompt builder.
         *
         * @param builder  An instance of
         *                 {@link android.hardware.biometrics.BiometricPrompt.Builder}.
         * @param text     The text for the negative button.
         * @param executor An executor for the negative button callback.
         * @param listener A listener for the negative button press event.
         */
        @DoNotInline
        static void setNegativeButton(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder,
                @NonNull CharSequence text,
                @NonNull Executor executor,
                DialogInterface.@NonNull OnClickListener listener) {
            builder.setNegativeButton(text, executor, listener);
        }

        /**
         * Creates an instance of the framework class
         * {@link android.hardware.biometrics.BiometricPrompt} from the given builder.
         *
         * @param builder The builder for the prompt.
         * @return An instance of {@link android.hardware.biometrics.BiometricPrompt}.
         */
        @DoNotInline
        static android.hardware.biometrics.@NonNull BiometricPrompt buildPrompt(
                android.hardware.biometrics.BiometricPrompt.@NonNull Builder builder) {
            return builder.build();
        }

        /**
         * Starts (non-crypto) authentication for the given framework biometric prompt.
         *
         * @param biometricPrompt    An instance of
         *                           {@link android.hardware.biometrics.BiometricPrompt}.
         * @param cancellationSignal A cancellation signal object for the prompt.
         * @param executor           An executor for authentication callbacks.
         * @param callback           An object that will receive authentication events.
         */
        @DoNotInline
        static void authenticate(
                android.hardware.biometrics.@NonNull BiometricPrompt biometricPrompt,
                android.os.@NonNull CancellationSignal cancellationSignal,
                @NonNull Executor executor,
                android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationCallback
                        callback) {
            biometricPrompt.authenticate(cancellationSignal, executor, callback);
        }

        /**
         * Starts (crypto-based) authentication for the given framework biometric prompt.
         *
         * @param biometricPrompt    An instance of
         *                           {@link android.hardware.biometrics.BiometricPrompt}.
         * @param crypto             A crypto object associated with the given authentication.
         * @param cancellationSignal A cancellation signal object for the prompt.
         * @param executor           An executor for authentication callbacks.
         * @param callback           An object that will receive authentication events.
         */
        @DoNotInline
        static void authenticate(
                android.hardware.biometrics.@NonNull BiometricPrompt biometricPrompt,
                android.hardware.biometrics.BiometricPrompt.@NonNull CryptoObject crypto,
                android.os.@NonNull CancellationSignal cancellationSignal,
                @NonNull Executor executor,
                android.hardware.biometrics.BiometricPrompt.@NonNull AuthenticationCallback
                        callback) {
            biometricPrompt.authenticate(crypto, cancellationSignal, executor, callback);
        }
    }
}
