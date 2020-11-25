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
 * only permits device credential modalities (device PIN, pattern, or password), and then start
 * authentication.
 */
public class CredentialAuthPrompt {

    /**
     * The default executor provided when not provided in the {@link CredentialAuthPrompt}
     * constructor.
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

    @Nullable private BiometricPrompt.CryptoObject mCrypto;
    @Nullable private CharSequence mDescription;

    /**
     * Constructs a {@link CredentialAuthPrompt}, which can be used to begin authentication.
     * @param biometricPrompt Manages a system-provided biometric prompt for authentication
     * @param promptInfo A set of configurable options for how the {@link BiometricPrompt}
     *                   should appear and behave.
     * @param crypto A crypto object to be associated with this authentication.
     * @param description The description to be displayed on the prompt.
     */
    CredentialAuthPrompt(@NonNull BiometricPrompt biometricPrompt,
            @NonNull BiometricPrompt.PromptInfo promptInfo,
            @Nullable BiometricPrompt.CryptoObject crypto,
            @NonNull CharSequence description) {
        mBiometricPrompt = biometricPrompt;
        mPromptInfo = promptInfo;
        mCrypto = crypto;
        mDescription = description;
    }

    /**
     * Begins authentication using the configured authentication prompt, and returns an
     * {@link AuthPrompt} wrapper that can be used for cancellation and dismissal of the
     * authentication prompt.
     * @return {@link AuthPrompt} wrapper that can be used for cancellation and dismissal of the
     * authentication prompt using {@link AuthPrompt#cancelAuthentication()}
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
     * Returns the crypto object for the prompt.
     * @return mCrypto A crypto object to be associated with this authentication.
     */
    @Nullable
    public BiometricPrompt.CryptoObject getCrypto() {
        return mCrypto;
    }

    /**
     * Returns the description for the prompt.
     * @return mDescription The description to be displayed on the prompt.
     */
    @Nullable
    public CharSequence getDescription() {
        return mDescription;
    }

    /**
     * Builder to configure a {@link BiometricPrompt} object for device credential only
     * authentication with specified options.
     */
    public static final class Builder {

        // Mutable options to be set on the builder.
        @Nullable private BiometricPrompt.CryptoObject mCrypto = null;
        @Nullable private CharSequence mDescription = null;

        @NonNull private final AuthPromptHost mAuthPromptHost;
        @NonNull private final CharSequence mTitle;
        @NonNull private final Executor mClientExecutor;

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        @NonNull final AuthPromptCallback mClientCallback;

        /**
         * A builder used to set individual options for the {@link CredentialAuthPrompt}
         * class to construct a {@link BiometricPrompt} for device credential authentication.
         *
         * @param authPromptHost {@link androidx.fragment.app.Fragment} or
         * {@link androidx.fragment.app.FragmentActivity} to host the authentication prompt
         * @param title The title to be displayed on the prompt.
         * @param clientExecutor The executor that will run authentication callback methods.
         * @param clientCallback The object that will receive and process authentication events.
         * @return Builder object for setting remaining optional variables of the
         * {@link CredentialAuthPrompt} class.
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
         * A builder used to set individual options for the {@link CredentialAuthPrompt}
         * class to construct a {@link BiometricPrompt} for device credential authentication.
         * Sets mClientExecutor to new DefaultExecutor() object.
         *
         * @param authPromptHost {@link androidx.fragment.app.Fragment} or
         * {@link androidx.fragment.app.FragmentActivity} to host the authentication prompt
         * @param title The title to be displayed on the prompt.
         * @param clientCallback The object that will receive and process authentication events.
         * @return Builder object for setting remaining optional variables of the
         * {@link CredentialAuthPrompt} class.
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
        public CredentialAuthPrompt.Builder setCrypto(
                @NonNull BiometricPrompt.CryptoObject crypto
        ) {
            mCrypto = crypto;
            return this;
        }

        /**
         * Optional: Sets the description for the prompt. Defaults to null.
         *
         * @param description The description to be displayed on the prompt.
         * @return This builder.
         */
        @NonNull
        public CredentialAuthPrompt.Builder setDescription(@NonNull CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Configures a {@link BiometricPrompt} object with the specified options, and returns
         * a {@link CredentialAuthPrompt} instance that can be used for starting authentication.
         * @return {@link CredentialAuthPrompt} instance for starting authentication.
         */
        @NonNull
        public CredentialAuthPrompt build() {
            final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo
                    .Builder()
                    .setTitle(mTitle)
                    .setDescription(mDescription)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
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
            return new CredentialAuthPrompt(biometricPrompt, promptInfo, mCrypto, mDescription);
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
