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

package androidx.biometric;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.Signature;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * A class that manages a system-provided biometric prompt. On devices running Android 9.0 (API 28)
 * and above, this will show a system-provided authentication prompt, using one of the device's
 * supported biometric modalities (fingerprint, iris, face, etc). Prior to Android 9.0, this will
 * instead show a custom fingerprint authentication dialog. The prompt will persist across
 * configuration changes unless explicitly canceled. For security reasons, the prompt will be
 * dismissed when the client application is no longer in the foreground.
 *
 * <p>To persist authentication across configuration changes, developers should (re)create the
 * prompt every time the activity/fragment is created. Instantiating the prompt with a new
 * callback early in the fragment/activity lifecycle (e.g. in {@code onCreate()}) will allow the
 * ongoing authentication session's callbacks to be received by the new fragment/activity instance.
 * Note that {@code cancelAuthentication()} should not be called, and {@code authenticate()} does
 * not need to be invoked during activity/fragment creation.
 */
public class BiometricPrompt implements BiometricConstants {
    private static final String TAG = "BiometricPromptCompat";

    /**
     * An error code that may be returned during authentication.
     */
    @IntDef({
            ERROR_HW_UNAVAILABLE,
            ERROR_UNABLE_TO_PROCESS,
            ERROR_TIMEOUT,
            ERROR_NO_SPACE,
            ERROR_CANCELED,
            ERROR_LOCKOUT,
            ERROR_VENDOR,
            ERROR_LOCKOUT_PERMANENT,
            ERROR_USER_CANCELED,
            ERROR_NO_BIOMETRICS,
            ERROR_HW_NOT_PRESENT,
            ERROR_NEGATIVE_BUTTON,
            ERROR_NO_DEVICE_CREDENTIAL
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface BiometricError {}

    /**
     * Tag used to identify the {@link BiometricFragment} attached to the client activity/fragment.
     */
    private static final String BIOMETRIC_FRAGMENT_TAG = "androidx.biometric.BiometricFragment";

    // Bundle keys used to store and retrieve PromptInfo options.
    private static final String KEY_TITLE = "title";
    private static final String KEY_SUBTITLE = "subtitle";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_NEGATIVE_TEXT = "negative_text";
    private static final String KEY_REQUIRE_CONFIRMATION = "require_confirmation";
    private static final String KEY_ALLOW_DEVICE_CREDENTIAL = "allow_device_credential";

    /**
     * A wrapper class for the crypto objects supported by {@link BiometricPrompt}. Currently, the
     * framework supports {@link Signature}, {@link Cipher}, and {@link Mac} objects.
     */
    public static class CryptoObject {
        private final Signature mSignature;
        private final Cipher mCipher;
        private final Mac mMac;

        /**
         * Creates a {@link CryptoObject} that wraps the given {@link Signature} object.
         *
         * @param signature The {@link Signature} to be associated with this {@link CryptoObject}.
         */
        public CryptoObject(@NonNull Signature signature) {
            mSignature = signature;
            mCipher = null;
            mMac = null;
        }

        /**
         * Creates a {@link CryptoObject} that wraps the given {@link Cipher} object.
         *
         * @param cipher The {@link Cipher} to be associated with this {@link CryptoObject}.
         */
        public CryptoObject(@NonNull Cipher cipher) {
            mCipher = cipher;
            mSignature = null;
            mMac = null;
        }

        /**
         * Creates a {@link CryptoObject} that wraps the given {@link Mac} object.
         *
         * @param mac The {@link Mac} to be associated with this {@link CryptoObject}.
         */
        public CryptoObject(@NonNull Mac mac) {
            mMac = mac;
            mCipher = null;
            mSignature = null;
        }

        /**
         * Gets the {@link Signature} object associated with this crypto object.
         *
         * @return The {@link Signature}, or {@code null} if none is associated with this object.
         */
        @Nullable
        public Signature getSignature() {
            return mSignature;
        }

        /**
         * Gets the {@link Cipher} object associated with this crypto object.
         *
         * @return The {@link Cipher}, or {@code null} if none is associated with this object.
         */
        @Nullable
        public Cipher getCipher() {
            return mCipher;
        }

        /**
         * Gets the {@link Mac} object associated with this crypto object.
         *
         * @return The {@link Mac}, or {@code null} if none is associated with this object.
         */
        @Nullable
        public Mac getMac() {
            return mMac;
        }
    }

    /**
     * A container for data passed to {@link AuthenticationCallback#onAuthenticationSucceeded(
     * AuthenticationResult)} when the user has successfully authenticated.
     */
    public static class AuthenticationResult {
        private final CryptoObject mCryptoObject;

        AuthenticationResult(CryptoObject crypto) {
            mCryptoObject = crypto;
        }

        /**
         * Gets the {@link CryptoObject} associated with this transaction.
         *
         * @return The {@link CryptoObject} provided to {@code authenticate()}.
         */
        @Nullable
        public CryptoObject getCryptoObject() {
            return mCryptoObject;
        }
    }

    /**
     * A collection of methods that may be invoked by {@link BiometricPrompt} during authentication.
     */
    public abstract static class AuthenticationCallback {
        /**
         * Called when an unrecoverable error has been encountered and authentication has stopped.
         *
         * <p>After this method is called, no further events will be sent for the current
         * authentication session.
         *
         * @param errorCode An integer ID associated with the error.
         * @param errString A human-readable string that describes the error.
         */
        public void onAuthenticationError(
                @BiometricError int errorCode, @NonNull CharSequence errString) {}

        /**
         * Called when a biometric (e.g. fingerprint, face, etc.) is recognized, indicating that the
         * user has successfully authenticated.
         *
         * <p>After this method is called, no further events will be sent for the current
         * authentication session.
         *
         * @param result An object containing authentication-related data.
         */
        public void onAuthenticationSucceeded(@NonNull AuthenticationResult result) {}

        /**
         * Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as
         * belonging to the user.
         */
        public void onAuthenticationFailed() {}
    }

    /**
     * A set of configurable options for how the {@link BiometricPrompt} should appear and behave.
     */
    public static class PromptInfo {
        /**
         * A builder used to set individual options for the {@link PromptInfo} class.
         */
        public static class Builder {
            private final Bundle mBundle = new Bundle();

            /**
             * Required: Sets the title for the prompt.
             *
             * @param title The title to be displayed on the prompt.
             * @return This builder.
             */
            @NonNull
            public Builder setTitle(@NonNull CharSequence title) {
                mBundle.putCharSequence(KEY_TITLE, title);
                return this;
            }

            /**
             * Optional: Sets the subtitle for the prompt.
             *
             * @param subtitle The subtitle to be displayed on the prompt.
             * @return This builder.
             */
            @NonNull
            public Builder setSubtitle(@Nullable CharSequence subtitle) {
                mBundle.putCharSequence(KEY_SUBTITLE, subtitle);
                return this;
            }

            /**
             * Optional: Sets the description for the prompt.
             *
             * @param description The description to be displayed on the prompt.
             * @return This builder.
             */
            @NonNull
            public Builder setDescription(@Nullable CharSequence description) {
                mBundle.putCharSequence(KEY_DESCRIPTION, description);
                return this;
            }

            /**
             * Required: Sets the text for the negative button on the prompt.
             *
             * <p>Note that this option is incompatible with
             * {@link PromptInfo.Builder#setDeviceCredentialAllowed(boolean)} and must NOT be set
             * if the latter is enabled.
             *
             * @param negativeButtonText The label to be used for the negative button on the prompt.
             * @return This builder.
             */
            @NonNull
            public Builder setNegativeButtonText(@NonNull CharSequence negativeButtonText) {
                mBundle.putCharSequence(KEY_NEGATIVE_TEXT, negativeButtonText);
                return this;
            }

            /**
             * Optional: Sets a system hint for whether to require explicit user confirmation after
             * a passive biometric (e.g. iris or face) has been recognized but before
             * {@link AuthenticationCallback#onAuthenticationSucceeded(AuthenticationResult)} is
             * called. Defaults to {@code true}.
             *
             * <p>Disabling this option is generally only appropriate for frequent, low-value
             * transactions, such as re-authenticating for a previously authorized application.
             *
             * <p>Also note that, as it is merely a hint, this option may be ignored by the system.
             * For example, the system may choose to instead always require confirmation if the user
             * has disabled passive authentication for their device in Settings. Additionally, this
             * option will be ignored on devices running OS versions prior to Android 10 (API 29).
             *
             * @param confirmationRequired Whether this option should be enabled.
             * @return This builder.
             */
            @NonNull
            public Builder setConfirmationRequired(boolean confirmationRequired) {
                mBundle.putBoolean(KEY_REQUIRE_CONFIRMATION, confirmationRequired);
                return this;
            }

            /**
             * Optional: Sets whether the user should be given the option to authenticate with
             * their device PIN, pattern, or password instead of a biometric. Defaults to
             * {@code false}.
             *
             * <p>Note that this option is incompatible with
             * {@link PromptInfo.Builder#setNegativeButtonText(CharSequence)} and must NOT be
             * enabled if the latter is set.
             *
             * <p>Before enabling this option, developers should check whether the device is secure
             * by calling {@link android.app.KeyguardManager#isDeviceSecure()}. If the device is not
             * secure, authentication will fail with {@link #ERROR_NO_DEVICE_CREDENTIAL}.
             *
             * <p>On versions prior to Android 10 (API 29), calls to
             * {@link #cancelAuthentication()} will not work as expected after the
             * user has chosen to authenticate with their device credential. This is because the
             * library internally launches a separate activity (by calling
             * {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence,
             * CharSequence)}) that does not have a public API for cancellation.
             *
             * @param deviceCredentialAllowed Whether this option should be enabled.
             * @return This builder.
             */
            @SuppressWarnings("deprecation")
            @NonNull
            public Builder setDeviceCredentialAllowed(boolean deviceCredentialAllowed) {
                mBundle.putBoolean(KEY_ALLOW_DEVICE_CREDENTIAL, deviceCredentialAllowed);
                return this;
            }

            /**
             * Creates a {@link PromptInfo} object with the specified options.
             *
             * @return The {@link PromptInfo} object.
             * @throws IllegalArgumentException If any required option is not set, or if any
             *  illegal combination of options is present.
             */
            @NonNull
            public PromptInfo build() {
                final CharSequence title = mBundle.getCharSequence(KEY_TITLE);
                final CharSequence negativeText = mBundle.getCharSequence(KEY_NEGATIVE_TEXT);
                final boolean allowDeviceCredential =
                        mBundle.getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL);

                if (TextUtils.isEmpty(title)) {
                    throw new IllegalArgumentException("Title must be set and non-empty.");
                }
                if (TextUtils.isEmpty(negativeText) && !allowDeviceCredential) {
                    throw new IllegalArgumentException("Negative text must be set and non-empty.");
                }
                if (!TextUtils.isEmpty(negativeText) && allowDeviceCredential) {
                    throw new IllegalArgumentException("Negative text must not be set if device "
                            + "credential authentication is allowed.");
                }
                return new PromptInfo(mBundle);
            }
        }

        @NonNull private Bundle mBundle;

        // Prevent direct instantiation.
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        PromptInfo(@NonNull Bundle bundle) {
            mBundle = bundle;
        }

        /**
         * Gets a bundle containing the options that have been set for the prompt.
         *
         * @return A bundle of set options.
         */
        @NonNull
        Bundle getBundle() {
            return mBundle;
        }

        /**
         * See {@link Builder#setTitle(CharSequence)}.
         *
         * @return The title to be displayed on the prompt.
         */
        @NonNull
        public CharSequence getTitle() {
            return mBundle.getCharSequence(KEY_TITLE, "");
        }

        /**
         * See {@link Builder#setSubtitle(CharSequence)}.
         *
         * @return The subtitle to be displayed on the prompt.
         */
        @Nullable
        public CharSequence getSubtitle() {
            return mBundle.getCharSequence(KEY_SUBTITLE);
        }

        /**
         * See {@link Builder#setDescription(CharSequence)}.
         *
         * @return The description to be displayed on the prompt.
         */
        @Nullable
        public CharSequence getDescription() {
            return mBundle.getCharSequence(KEY_DESCRIPTION);
        }

        /**
         * See {@link Builder#setNegativeButtonText(CharSequence)}.
         *
         * @return The label to be used for the negative button on the prompt.
         */
        @NonNull
        public CharSequence getNegativeButtonText() {
            return mBundle.getCharSequence(KEY_NEGATIVE_TEXT, "");
        }

        /**
         * See {@link Builder#setConfirmationRequired(boolean)}.
         *
         * @return Whether this option is enabled.
         */
        public boolean isConfirmationRequired() {
            return mBundle.getBoolean(KEY_REQUIRE_CONFIRMATION);
        }

        /**
         * See {@link Builder#setDeviceCredentialAllowed(boolean)}.
         *
         * @return Whether this option is enabled.
         */
        public boolean isDeviceCredentialAllowed() {
            return mBundle.getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL);
        }
    }

    /**
     * The fragment manager that will be used to attach the prompt to the client activity.
     */
    @Nullable private FragmentManager mClientFragmentManager;

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link Executor} and {@link AuthenticationCallback} for the current session.
     * Thus, this method should be called by the client activity each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param activity The activity of the client application that will host the prompt.
     * @param executor The executor that will be used to run {@link AuthenticationCallback} methods.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(Fragment, Executor, AuthenticationCallback)
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull FragmentActivity activity,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (activity == null) {
            throw new IllegalArgumentException("FragmentActivity must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }

        init(activity, activity.getSupportFragmentManager(), executor, callback);
    }

    /**
     * Constructs a {@link BiometricPrompt}, which can be used to prompt the user to authenticate
     * with a biometric such as fingerprint or face. The prompt can be shown to the user by calling
     * {@code authenticate()} and persists across device configuration changes by default.
     *
     * <p>If authentication is in progress, calling this constructor to recreate the prompt will
     * also update the {@link Executor} and {@link AuthenticationCallback} for the current session.
     * Thus, this method should be called by the client fragment each time the configuration changes
     * (e.g. in {@code onCreate()}).
     *
     * @param fragment The fragment of the client application that will host the prompt.
     * @param executor The executor that will be used to run {@link AuthenticationCallback} methods.
     * @param callback The object that will receive and process authentication events.
     *
     * @see #BiometricPrompt(FragmentActivity, Executor, AuthenticationCallback)
     */
    @SuppressLint("LambdaLast")
    @SuppressWarnings("ConstantConditions")
    public BiometricPrompt(
            @NonNull Fragment fragment,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        if (fragment == null) {
            throw new IllegalArgumentException("Fragment must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Executor must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("AuthenticationCallback must not be null");
        }

        init(fragment.getActivity(), fragment.getChildFragmentManager(), executor, callback);
    }

    /**
     * Initializes or updates the data needed by the prompt.
     *
     * @param activity The client activity that will host the prompt.
     * @param fragmentManager The fragment manager that will be used to attach the prompt.
     * @param executor The executor that will be used to run {@link AuthenticationCallback} methods.
     * @param callback The object that will receive and process authentication events.
     */
    private void init(
            @Nullable FragmentActivity activity,
            @Nullable FragmentManager fragmentManager,
            @NonNull Executor executor,
            @NonNull AuthenticationCallback callback) {

        mClientFragmentManager = fragmentManager;

        if (activity != null) {
            final BiometricViewModel viewModel =
                    new ViewModelProvider(activity).get(BiometricViewModel.class);
            viewModel.setClientExecutor(executor);
            viewModel.setClientCallback(callback);
        }
    }

    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use {@link #cancelAuthentication()}.
     *
     * @param info A {@link PromptInfo} object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     *
     * @see #authenticate(PromptInfo)
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info, @NonNull CryptoObject crypto) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo cannot be null.");
        }
        if (crypto == null) {
            throw new IllegalArgumentException("CryptoObject cannot be null.");
        }
        if (info.getBundle().getBoolean(KEY_ALLOW_DEVICE_CREDENTIAL)) {
            throw new IllegalArgumentException("Device credential not supported with crypto.");
        }

        authenticateInternal(info, crypto);
    }

    /**
     * Shows the biometric prompt to the user. The prompt survives lifecycle changes by default. To
     * cancel authentication and dismiss the prompt, use {@link #cancelAuthentication()}.
     *
     * @param info A {@link PromptInfo} object describing the appearance and behavior of the prompt.
     *
     * @see #authenticate(PromptInfo, CryptoObject)
     */
    @SuppressWarnings("ConstantConditions")
    public void authenticate(@NonNull PromptInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("PromptInfo cannot be null.");
        }

        authenticateInternal(info, null /* crypto */);
    }

    /**
     * Shows the biometric prompt to the user and begins authentication.
     *
     * @param info A {@link PromptInfo} object describing the appearance and behavior of the prompt.
     * @param crypto A crypto object to be associated with this authentication.
     */
    private void authenticateInternal(@NonNull PromptInfo info, @Nullable CryptoObject crypto) {
        if (mClientFragmentManager == null) {
            Log.e(TAG, "Unable to start authentication. Client fragment manager was null.");
            return;
        }
        if (mClientFragmentManager.isStateSaved()) {
            Log.e(TAG, "Unable to start authentication. Called after onSaveInstanceState().");
            return;
        }

        final BiometricFragment biometricFragment =
                findOrAddBiometricFragment(mClientFragmentManager);
        biometricFragment.authenticate(info, crypto);
    }

    /**
     * Cancels the ongoing authentication session and dismisses the prompt.
     *
     * <p>On versions prior to Android 10 (API 29), calling this method while the user is
     * authenticating with their device credential will NOT work as expected. See
     * {@link PromptInfo.Builder#setDeviceCredentialAllowed(boolean)} for more details.
     */
    public void cancelAuthentication() {
        if (mClientFragmentManager == null) {
            Log.e(TAG, "Unable to start authentication. Client fragment manager was null.");
            return;
        }

        final BiometricFragment biometricFragment = findBiometricFragment(mClientFragmentManager);
        if (biometricFragment == null) {
            Log.e(TAG, "Unable to cancel authentication. BiometricFragment not found.");
            return;
        }

        biometricFragment.cancelAuthentication(BiometricFragment.CANCELED_FROM_NONE);
    }

    /**
     * Searches for a {@link BiometricFragment} instance that has been added to an activity or
     * fragment.
     *
     * @param fragmentManager The fragment manager that will be used to search for the fragment.
     * @return An instance of {@link BiometricFragment} found by the fragment manager, or {@code
     *  null} if no such fragment is found.
     */
    @Nullable
    private static BiometricFragment findBiometricFragment(
            @NonNull FragmentManager fragmentManager) {
        return (BiometricFragment) fragmentManager.findFragmentByTag(
                BiometricPrompt.BIOMETRIC_FRAGMENT_TAG);
    }

    /**
     * Returns a {@link BiometricFragment} instance that has been added to an activity or fragment,
     * adding one if necessary.
     *
     * @param fragmentManager The fragment manager used to search for and/or add the fragment.
     * @return An instance of {@link BiometricFragment} associated with the fragment manager.
     */
    @NonNull
    private static BiometricFragment findOrAddBiometricFragment(
            @NonNull FragmentManager fragmentManager) {

        BiometricFragment biometricFragment = findBiometricFragment(fragmentManager);

        // If the fragment hasn't been added before, add it.
        if (biometricFragment == null) {
            biometricFragment = BiometricFragment.newInstance();
            fragmentManager.beginTransaction()
                    .add(biometricFragment, BiometricPrompt.BIOMETRIC_FRAGMENT_TAG)
                    .commitAllowingStateLoss();

            // For the case when onResume() is being called right after authenticate,
            // we need to make sure that all fragment transactions have been committed.
            fragmentManager.executePendingTransactions();
        }

        return biometricFragment;
    }
}
