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
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricViewModel;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;

/**
 * Utilities used by various auth prompt classes.
 */
class AuthPromptUtils {
    // Prevent instantiation.
    private AuthPromptUtils() {}

    /**
     * Shows an authentication prompt to the user.
     *
     * @param host       A wrapper for the component that will host the prompt.
     * @param promptInfo A set of options describing how the prompt should appear and behave.
     * @param crypto     A cryptographic object to be associated with this authentication.
     * @param executor   A custom executor that will be used to run callback methods. If
     *                   {@code null}, callback methods will be run on the main thread.
     * @param callback   The object that will receive and process authentication events.
     * @return A handle to the shown prompt.
     */
    @NonNull
    static AuthPrompt startAuthentication(
            @NonNull AuthPromptHost host,
            @NonNull BiometricPrompt.PromptInfo promptInfo,
            @Nullable BiometricPrompt.CryptoObject crypto,
            @Nullable Executor executor,
            @NonNull AuthPromptCallback callback) {

        final BiometricPrompt biometricPrompt =
                AuthPromptUtils.createBiometricPrompt(host, executor, callback);

        if (crypto == null) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            biometricPrompt.authenticate(promptInfo, crypto);
        }

        return new AuthPromptWrapper(biometricPrompt);
    }

    /**
     * Creates a {@link BiometricPrompt} with the given parameters.
     *
     * @param host     A wrapper for the component that will host the prompt.
     * @param executor A custom executor that will be used to run callback methods. If {@code null},
     *                 callback methods will be run on the main thread.
     * @param callback The object that will receive and process authentication events.
     * @return An instance of {@link BiometricPrompt}.
     *
     * @throws IllegalArgumentException If the given host wrapper does not contain an activity or a
     * fragment that is associated with an activity.
     */
    @NonNull
    private static BiometricPrompt createBiometricPrompt(
            @NonNull AuthPromptHost host,
            @Nullable Executor executor,
            @NonNull AuthPromptCallback callback) {

        final Executor executorOrDefault = executor != null ? executor : new DefaultExecutor();

        final BiometricPrompt prompt;
        if (host.getActivity() != null) {
            final ViewModelProvider provider = new ViewModelProvider(host.getActivity());
            final AuthenticationCallbackWrapper wrappedCallback = wrapCallback(callback, provider);
            prompt = new BiometricPrompt(host.getActivity(), executorOrDefault, wrappedCallback);
        } else if (host.getFragment() != null && host.getFragment().getActivity() != null) {
            final FragmentActivity activity = host.getFragment().getActivity();
            final ViewModelProvider provider = new ViewModelProvider(activity);
            final AuthenticationCallbackWrapper wrappedCallback = wrapCallback(callback, provider);
            prompt = new BiometricPrompt(host.getFragment(), executorOrDefault, wrappedCallback);
        } else {
            throw new IllegalArgumentException("AuthPromptHost must contain a FragmentActivity or"
                    + " an attached Fragment.");
        }

        return prompt;
    }

    /**
     * Wraps the given callback in a new {@link AuthenticationCallbackWrapper} instance, for
     * compatibility with {@link BiometricPrompt}.
     *
     * @param callback A callback object that is compatible with {@link BiometricPrompt}.
     * @param provider A provider that can be used to get a {@link BiometricViewModel} instance.
     * @return An instance of {@link AuthenticationCallbackWrapper} that wraps the given callback.
     */
    private static AuthenticationCallbackWrapper wrapCallback(
            @NonNull AuthPromptCallback callback, @NonNull ViewModelProvider provider) {
        return new AuthenticationCallbackWrapper(callback, provider.get(BiometricViewModel.class));
    }

    /**
     * A wrapper class that provides an {@link AuthPrompt} interface for a {@link BiometricPrompt}.
     */
    private static class AuthPromptWrapper implements AuthPrompt {
        @NonNull private final WeakReference<BiometricPrompt> mBiometricPromptRef;

        /**
         * Constructs an {@link AuthPromptWrapper} interface for the given prompt.
         *
         * @param biometricPrompt An instance of {@link BiometricPrompt}.
         */
        AuthPromptWrapper(@NonNull BiometricPrompt biometricPrompt) {
            mBiometricPromptRef = new WeakReference<>(biometricPrompt);
        }

        @Override
        public void cancelAuthentication() {
            if (mBiometricPromptRef.get() != null) {
                mBiometricPromptRef.get().cancelAuthentication();
            }
        }
    }

    /**
     * The default executor class used to run authentication callback methods on the main thread.
     */
    private static class DefaultExecutor implements Executor {
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        DefaultExecutor() {}

        @Override
        public void execute(@NonNull Runnable runnable) {
            mHandler.post(runnable);
        }
    }

    /**
     * A wrapper class that provides a {@link BiometricPrompt.AuthenticationCallback} interface for
     * an {@link AuthPromptCallback}.
     */
    private static class AuthenticationCallbackWrapper
            extends BiometricPrompt.AuthenticationCallback {

        @NonNull private final AuthPromptCallback mClientCallback;
        @NonNull private final WeakReference<BiometricViewModel> mViewModelRef;

        /**
         * Creates an {@link AuthenticationCallbackWrapper} with the given parameters.
         *
         * @param callback  A callback object that is compatible with {@link BiometricPrompt}.
         * @param viewModel A {@link BiometricViewModel} that maintains a reference to the host
         *                  activity across configuration changes.
         */
        @SuppressWarnings("WeakerAccess") /* synthetic access */
        AuthenticationCallbackWrapper(
                @NonNull AuthPromptCallback callback,
                @NonNull BiometricViewModel viewModel) {
            mClientCallback = callback;
            mViewModelRef = new WeakReference<>(viewModel);
        }

        @Override
        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
            mClientCallback.onAuthenticationError(getActivity(mViewModelRef), errorCode, errString);
        }

        @Override
        public void onAuthenticationSucceeded(
                @NonNull BiometricPrompt.AuthenticationResult result) {
            mClientCallback.onAuthenticationSucceeded(getActivity(mViewModelRef), result);
        }

        @Override
        public void onAuthenticationFailed() {
            mClientCallback.onAuthenticationFailed(getActivity(mViewModelRef));
        }

        @Nullable
        private static FragmentActivity getActivity(
                @NonNull WeakReference<BiometricViewModel> viewModelRef) {
            return viewModelRef.get() != null ? viewModelRef.get().getClientActivity() : null;
        }
    }
}
