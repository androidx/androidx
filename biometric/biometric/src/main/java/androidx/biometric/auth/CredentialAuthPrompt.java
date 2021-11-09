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

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

/**
 * An authentication prompt that requires the user to present the screen lock credential (i.e. PIN,
 * pattern, or password) for the device.
 *
 * @see Authenticators#DEVICE_CREDENTIAL
 * @see Class2BiometricAuthPrompt
 * @see Class2BiometricOrCredentialAuthPrompt
 * @see Class3BiometricAuthPrompt
 * @see Class3BiometricOrCredentialAuthPrompt
 */
@RequiresApi(Build.VERSION_CODES.R)
public class CredentialAuthPrompt {
    @NonNull private final BiometricPrompt.PromptInfo mPromptInfo;

    /**
     * Constructs an authentication prompt with the given parameters.
     *
     * @param promptInfo A set of options describing how the prompt should appear and behave.
     */
    CredentialAuthPrompt(@NonNull BiometricPrompt.PromptInfo promptInfo) {
        mPromptInfo = promptInfo;
    }

    /**
     * Shows an authentication prompt to the user.
     *
     * @param host     A wrapper for the component that will host the prompt.
     * @param crypto   A cryptographic object to be associated with this authentication.
     * @param callback The callback object that will receive and process authentication events. Each
     *                 callback method will be run on the main thread.
     * @return A handle to the shown prompt.
     *
     * @see #startAuthentication(AuthPromptHost, BiometricPrompt.CryptoObject, Executor,
     * AuthPromptCallback)
     */
    @NonNull
    public AuthPrompt startAuthentication(
            @NonNull AuthPromptHost host,
            @Nullable BiometricPrompt.CryptoObject crypto,
            @NonNull AuthPromptCallback callback) {
        return AuthPromptUtils.startAuthentication(
                host, mPromptInfo, crypto, null /* executor */, callback);
    }

    /**
     * Shows an authentication prompt to the user.
     *
     * @param host     A wrapper for the component that will host the prompt.
     * @param crypto   A cryptographic object to be associated with this authentication.
     * @param executor The executor that will be used to run authentication callback methods.
     * @param callback The callback object that will receive and process authentication events.
     * @return A handle to the shown prompt.
     *
     * @see #startAuthentication(AuthPromptHost, BiometricPrompt.CryptoObject, AuthPromptCallback)
     */
    @NonNull
    public AuthPrompt startAuthentication(
            @NonNull AuthPromptHost host,
            @Nullable BiometricPrompt.CryptoObject crypto,
            @NonNull Executor executor,
            @NonNull AuthPromptCallback callback) {
        return AuthPromptUtils.startAuthentication(
                host, mPromptInfo, crypto, executor, callback);
    }

    /**
     * Gets the title to be displayed on the prompt.
     *
     * @return The title for the prompt.
     */
    @NonNull
    public CharSequence getTitle() {
        return mPromptInfo.getTitle();
    }

    /**
     * Gets the description to be displayed on the prompt, if set.
     *
     * @return The description for the prompt.
     *
     * @see Builder#setDescription(CharSequence)
     */
    @Nullable
    public CharSequence getDescription() {
        return mPromptInfo.getDescription();
    }

    /**
     * Builder for a {@link CredentialAuthPrompt} with configurable options.
     */
    public static final class Builder {
        // Required fields.
        @NonNull private final CharSequence mTitle;

        // Optional fields.
        @Nullable private CharSequence mDescription = null;

        /**
         * Constructs a prompt builder with the given required options.
         *
         * @param title The title to be displayed on the prompt.
         */
        @SuppressLint("ExecutorRegistration")
        public Builder(@NonNull CharSequence title) {
            mTitle = title;
        }

        /**
         * Sets a description that should be displayed on the prompt. Defaults to {@code null}.
         *
         * @param description A description for the prompt.
         * @return This builder.
         */
        @NonNull
        public CredentialAuthPrompt.Builder setDescription(@NonNull CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Creates a new prompt with the specified options.
         *
         * @return An instance of {@link CredentialAuthPrompt}.
         */
        @NonNull
        public CredentialAuthPrompt build() {
            final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(mTitle)
                    .setDescription(mDescription)
                    .setAllowedAuthenticators(Authenticators.DEVICE_CREDENTIAL)
                    .build();
            return new CredentialAuthPrompt(promptInfo);
        }
    }
}
