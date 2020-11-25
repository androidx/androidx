/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric.auth;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * This class is used to build and configure a {@link BiometricPrompt} for authentication that
 * permits Class 3 biometric modalities (fingerprint, iris, face, etc), or device credential
 * modalities (device PIN, pattern, or password), and then start authentication.
 *
 * Class 3 (formerly known as Strong) refers to the strength of the biometric sensor, as specified
 * in the Android 11 CDD. Class 3 authentication can be used for applications that use
 * cryptographic operations.
 */
public class Class3BiometricOrCredentialAuthPrompt {

    /**
     * The default executor provided when not provided in the
     * {@link Class3BiometricOrCredentialAuthPrompt} constructor.
     */
    private static class DefaultExecutor implements Executor {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        DefaultExecutor() {}

        @Override
        public void execute(Runnable runnable) {
            mHandler.post(runnable);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @NonNull BiometricPrompt mBiometricPrompt;
    @NonNull private BiometricPrompt.PromptInfo mPromptInfo;
    private boolean mIsConfirmationRequired;

    @Nullable private BiometricPrompt.CryptoObject mCrypto;
    @Nullable private CharSequence mSubtitle;
    @Nullable private CharSequence mDescription;

    /**
     * Constructs a {@link Class3BiometricOrCredentialAuthPrompt}, which can be used to begin
     * authentication.
     * @param biometricPrompt Manages a system-provided biometric prompt for authentication
     * @param promptInfo A set of configurable options for how the {@link BiometricPrompt}
     *                   should appear and behave.
     * @param crypto A crypto object to be associated with this authentication.
     * @param subtitle The subtitle to be displayed on the prompt.
     * @param description The description to be displayed on the prompt.
     * @param confirmationRequired Whether explicit user confirmation is required after a
     *                             passive biometric
     */
    Class3BiometricOrCredentialAuthPrompt(@NonNull BiometricPrompt biometricPrompt,
            @NonNull BiometricPrompt.PromptInfo promptInfo,
            @Nullable BiometricPrompt.CryptoObject crypto,
            @NonNull CharSequence subtitle,
            @NonNull CharSequence description,
            boolean confirmationRequired) {
        mBiometricPrompt = biometricPrompt;
        mPromptInfo = promptInfo;
        mCrypto = crypto;
        mSubtitle = subtitle;
        mDescription = description;
        mIsConfirmationRequired = confirmationRequired;
    }

    /**
     * Begins authentication using the configured biometric prompt, and returns an
     * {@link AuthPrompt} wrapper that can be used for cancellation and dismissal of the biometric
     * prompt.
     * @return {@link AuthPrompt} wrapper that can be used for cancellation and dismissal of the
     * biometric prompt using {@link AuthPrompt#cancelAuthentication()}
     */
    @NonNull
    public AuthPrompt startAuthentication() {
        if (mCrypto == null) {
            mBiometricPrompt.authenticate(mPromptInfo);
        } else {
            mBiometricPrompt.authenticate(mPromptInfo, mCrypto);
        }

        return new AuthPrompt() {
            @Override
            public void cancelAuthentication() {
                mBiometricPrompt.cancelAuthentication();
            }
        };
    }

    /**
     * Gets the subtitle for the prompt.
     * @return subtitle for the prompt
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /**
     * Gets the description for the prompt. Defaults to null.
     * @return description for the prompt
     */
    @Nullable
    public CharSequence getDescription() {
        return mDescription;
    }

    /**
     * Indicates whether prompt requires explicit user confirmation after a passive biometric (e.g.
     * iris or face) has been recognized but before {@link
     * AuthPromptCallback#onAuthenticationSucceeded(androidx.fragment.app.FragmentActivity,
     * BiometricPrompt.AuthenticationResult)} is called.
     * @return whether prompt requires explicit user confirmation after a passive biometric.
     */
    public boolean isConfirmationRequired() {
        return mIsConfirmationRequired;
    }

    /**
     * Gets the crypto object for the prompt authentication.
     * @return Crypto object associated with this authentication.
     */
    @Nullable
    public BiometricPrompt.CryptoObject getCrypto() {
        return mCrypto;
    }

    /**
     * Builder to configure a {@link BiometricPrompt} object for class 3 biometric or device
     * credential authentication with specified options.
     */
    public static final class Builder {
        // Nullable options on the builder
        @Nullable private BiometricPrompt.CryptoObject mCrypto = null;
        @Nullable private CharSequence mSubtitle = null;
        @Nullable private CharSequence mDescription = null;

        // Non-null options on the builder
        @NonNull private final AuthPromptHost mAuthPromptHost;
        @NonNull private final CharSequence mTitle;
        @NonNull private final Executor mClientExecutor;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull final AuthPromptCallback mClientCallback;

        private boolean mIsConfirmationRequired = true;

        /**
         * A builder used to set individual options for the
         * {@link Class3BiometricOrCredentialAuthPrompt} class to construct a
         * {@link BiometricPrompt} for class 3 biometric or device credential authentication.
         *
         * @param authPromptHost {@link androidx.fragment.app.Fragment} or
         * {@link androidx.fragment.app.FragmentActivity} to host the authentication prompt
         * @param title The title to be displayed on the prompt.
         * @param clientExecutor The executor that will run authentication callback methods.
         * @param clientCallback The object that will receive and process authentication events.
         */
        public Builder(
                @NonNull AuthPromptHost authPromptHost,
                @NonNull CharSequence title,
                @NonNull Executor clientExecutor,
                @NonNull AuthPromptCallback clientCallback) {
            mAuthPromptHost = authPromptHost;
            mTitle = title;
            mClientExecutor = clientExecutor;
            mClientCallback = clientCallback;
        }

        /**
         * A builder used to set individual options for the
         * {@link Class3BiometricOrCredentialAuthPrompt} class to construct a
         * {@link BiometricPrompt} for class 3 biometric or device credential authentication.
         * Sets mClientExecutor to new DefaultExecutor() object.
         *
         * @param authPromptHost {@link androidx.fragment.app.Fragment} or
         * {@link androidx.fragment.app.FragmentActivity} to host the authentication prompt
         * @param title The title to be displayed on the prompt.
         * @param clientCallback The object that will receive and process authentication events.
         */
        public Builder(
                @NonNull AuthPromptHost authPromptHost,
                @NonNull CharSequence title,
                @NonNull AuthPromptCallback clientCallback) {
            mAuthPromptHost = authPromptHost;
            mTitle = title;
            mClientExecutor = new DefaultExecutor();
            mClientCallback = clientCallback;
        }

        /**
         * Optional: Sets the crypto object for the prompt.
         * @param crypto A crypto object to be associated with this authentication.
         */
        @NonNull
        public Builder setCrypto(@NonNull BiometricPrompt.CryptoObject crypto) {
            mCrypto = crypto;
            return this;
        }

        /**
         * Optional: Sets the subtitle for the prompt.
         *
         * @param subtitle The subtitle to be displayed on the prompt.
         * @return This builder.
         */
        @NonNull
        public Builder setSubtitle(
                @NonNull CharSequence subtitle) {
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Optional: Sets the description for the prompt.
         *
         * @param description The description to be displayed on the prompt.
         * @return This builder.
         */
        @NonNull
        public Builder setDescription(
                @NonNull CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Optional: Sets a system hint for whether to require explicit user confirmation after
         * a passive biometric (e.g. iris or face) has been recognized but before
         * {@link AuthPromptCallback#onAuthenticationSucceeded(
         * androidx.fragment.app.FragmentActivity, BiometricPrompt.AuthenticationResult)} is
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
        public Builder setConfirmationRequired(
                boolean confirmationRequired) {
            mIsConfirmationRequired = confirmationRequired;
            return this;
        }

        /**
         * Configures a {@link BiometricPrompt} object with the specified options, and returns a
         * {@link Class3BiometricOrCredentialAuthPrompt} instance that can be used for starting
         * authentication.
         * @return {@link Class3BiometricOrCredentialAuthPrompt} instance for starting
         * authentication.
         */
        @NonNull
        public Class3BiometricOrCredentialAuthPrompt build() {
            final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(mTitle)
                    .setSubtitle(mSubtitle)
                    .setDescription(mDescription)
                    .setConfirmationRequired(mIsConfirmationRequired)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build();

            final BiometricPrompt biometricPrompt;
            final BiometricPrompt.AuthenticationCallback wrappedCallback;

            if (mAuthPromptHost.getActivity() != null) {
                wrappedCallback = new WrappedAuthPromptCallback(mClientCallback,
                        new ViewModelProvider(mAuthPromptHost.getActivity())
                                .get(BiometricViewModel.class));
                biometricPrompt = new BiometricPrompt(mAuthPromptHost.getActivity(),
                        mClientExecutor, wrappedCallback);
            } else if (mAuthPromptHost.getFragment() != null) {
                wrappedCallback = new WrappedAuthPromptCallback(mClientCallback,
                        new ViewModelProvider(mAuthPromptHost.getFragment().getActivity())
                                .get(BiometricViewModel.class));
                biometricPrompt = new BiometricPrompt(mAuthPromptHost.getFragment(),
                        mClientExecutor, wrappedCallback);
            } else {
                throw new IllegalArgumentException("Invalid AuthPromptHost provided. Must "
                        + "provide AuthPromptHost containing Fragment or FragmentActivity for"
                        + " hosting the BiometricPrompt.");
            }
            return new Class3BiometricOrCredentialAuthPrompt(biometricPrompt, promptInfo, mCrypto,
                    mSubtitle, mDescription, mIsConfirmationRequired);
        }

        /**
         * Wraps AuthPromptCallback in BiometricPrompt.AuthenticationCallback for BiometricPrompt
         * construction
         */
        private static class WrappedAuthPromptCallback
                extends BiometricPrompt.AuthenticationCallback {
            @NonNull private final AuthPromptCallback mClientCallback;
            @NonNull private final WeakReference<BiometricViewModel> mViewModelRef;

            WrappedAuthPromptCallback(@NonNull AuthPromptCallback callback,
                    @NonNull BiometricViewModel viewModel) {
                mClientCallback = callback;
                mViewModelRef = new WeakReference<>(viewModel);
            }

            @Override
            public void onAuthenticationError(int errorCode,
                    @NonNull CharSequence errString) {
                if (mViewModelRef != null) {
                    mClientCallback.onAuthenticationError(
                            mViewModelRef.get().getClientActivity(),
                            errorCode,
                            errString
                    );
                }
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                if (mViewModelRef != null) {
                    mClientCallback.onAuthenticationSucceeded(
                            mViewModelRef.get().getClientActivity(),
                            result
                    );
                }
            }

            @Override
            public void onAuthenticationFailed() {
                if (mViewModelRef != null) {
                    mClientCallback.onAuthenticationFailed(
                            mViewModelRef.get().getClientActivity()
                    );
                }
            }
        }
    }
}
