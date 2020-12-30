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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager.Authenticators;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

/**
 * An authentication prompt that requires the user to present a <strong>Class 3</strong> biometric
 * (e.g. fingerprint, face, or iris).
 *
 * @see Authenticators#BIOMETRIC_STRONG
 * @see Class2BiometricAuthPrompt
 * @see Class2BiometricOrCredentialAuthPrompt
 * @see Class3BiometricOrCredentialAuthPrompt
 * @see CredentialAuthPrompt
 */
public class Class3BiometricAuthPrompt {
    @NonNull private final BiometricPrompt.PromptInfo mPromptInfo;

    /**
     * Constructs an authentication prompt with the given parameters.
     *
     * @param promptInfo A set of options describing how the prompt should appear and behave.
     */
    Class3BiometricAuthPrompt(@NonNull BiometricPrompt.PromptInfo promptInfo) {
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
     * Gets the label text for the negative button on the prompt.
     *
     * @return The negative button text for the prompt.
     */
    @NonNull
    public CharSequence getNegativeButtonText() {
        return mPromptInfo.getTitle();
    }

    /**
     * Gets the subtitle to be displayed on the prompt, if set.
     *
     * @return The subtitle for the prompt.
     *
     * @see Builder#setSubtitle(CharSequence)
     */
    @Nullable
    public CharSequence getSubtitle() {
        return mPromptInfo.getSubtitle();
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
     * Checks if the prompt should require explicit user confirmation after a passive biometric
     * (e.g. iris or face) has been recognized but before
     * {@link AuthPromptCallback#onAuthenticationSucceeded(androidx.fragment.app.FragmentActivity,
     * BiometricPrompt.AuthenticationResult)} is called.
     *
     * @return Whether the prompt should require explicit user confirmation for passive biometrics.
     *
     * @see Builder#setConfirmationRequired(boolean)
     */
    public boolean isConfirmationRequired() {
        return mPromptInfo.isConfirmationRequired();
    }

    /**
     * Builder for a {@link Class3BiometricAuthPrompt} with configurable options.
     */
    public static final class Builder {
        // Required fields.
        @NonNull private final CharSequence mTitle;
        @NonNull private final CharSequence mNegativeButtonText;

        // Optional fields.
        @Nullable private CharSequence mSubtitle = null;
        @Nullable private CharSequence mDescription = null;
        private boolean mIsConfirmationRequired = true;

        /**
         * Constructs a prompt builder with the given required options.
         *
         * @param title              The title to be displayed on the prompt.
         * @param negativeButtonText The label for the negative button on the prompt.
         */
        public Builder(@NonNull CharSequence title, @NonNull CharSequence negativeButtonText) {
            mTitle = title;
            mNegativeButtonText = negativeButtonText;
        }

        /**
         * Sets a subtitle that should be displayed on the prompt. Defaults to {@code null}
         *
         * @param subtitle A subtitle for the prompt.
         * @return This builder.
         */
        @NonNull
        public Builder setSubtitle(@NonNull CharSequence subtitle) {
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Sets a description that should be displayed on the prompt. Defaults to {@code null}
         *
         * @param description A description for the prompt.
         * @return This builder.
         */
        @NonNull
        public Builder setDescription(@NonNull CharSequence description) {
            mDescription = description;
            return this;
        }

        /**
         * Sets a hint indicating whether the prompt should require explicit user confirmation
         * after a passive biometric (e.g. iris or face) has been recognized but before
         * {@link AuthPromptCallback#onAuthenticationSucceeded(
         * androidx.fragment.app.FragmentActivity, BiometricPrompt.AuthenticationResult)} is
         * called. Defaults to {@code true}.
         *
         * <p>Setting this option to {@code false} is generally only appropriate for frequent,
         * low-value transactions, such as re-authenticating for a previously authorized app.
         *
         * <p>As a hint, the value of this option may be ignored by the system. For example,
         * explicit confirmation may always be required if the user has toggled a system-wide
         * setting to disallow pure passive authentication. This option will also be ignored on any
         * device with an OS version prior to Android 10 (API 29).
         *
         * @param confirmationRequired Whether the prompt should require explicit user confirmation
         *                             for passive biometrics.
         * @return This builder.
         */
        @NonNull
        public Builder setConfirmationRequired(boolean confirmationRequired) {
            mIsConfirmationRequired = confirmationRequired;
            return this;
        }

        /**
         * Creates a new prompt with the specified options.
         *
         * @return An instance of {@link Class3BiometricAuthPrompt}.
         */
        @NonNull
        public Class3BiometricAuthPrompt build() {
            final BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(mTitle)
                    .setSubtitle(mSubtitle)
                    .setDescription(mDescription)
                    .setNegativeButtonText(mNegativeButtonText)
                    .setConfirmationRequired(mIsConfirmationRequired)
                    .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG)
                    .build();
            return new Class3BiometricAuthPrompt(promptInfo);
        }
    }
}
