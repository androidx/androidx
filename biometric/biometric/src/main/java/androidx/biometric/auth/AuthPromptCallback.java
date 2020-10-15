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
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

/**
 * A collection of methods that may be invoked by {@link Class2BiometricAuthPrompt},
 * {@link Class3BiometricAuthPrompt}, {@link Class2BiometricOrCredentialAuthPrompt},
 * {@link Class3BiometricOrCredentialAuthPrompt}, or {@link CredentialAuthPrompt} during
 * authentication, returning the {@link androidx.fragment.app.FragmentActivity} the
 * prompt is attached to.
 */
public abstract class AuthPromptCallback {
    /**
     * Called when an unrecoverable error has been encountered and authentication has stopped.
     *
     * <p>After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param activity {@link androidx.fragment.app.FragmentActivity} the prompt is attached to
     * @param errorCode An integer ID associated with the error.
     * @param errString A human-readable string that describes the error.
     */
    public void onAuthenticationError(@Nullable FragmentActivity activity,
            @BiometricPrompt.AuthenticationError int errorCode, @NonNull CharSequence errString) {}

    /**
     * Called when a biometric (e.g. fingerprint, face, etc.) is recognized, indicating that the
     * user has successfully authenticated.
     *
     * <p>After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param activity {@link androidx.fragment.app.FragmentActivity} the prompt is attached to
     * @param result An object containing authentication-related data.
     */
    public void onAuthenticationSucceeded(@Nullable FragmentActivity activity,
            @NonNull BiometricPrompt.AuthenticationResult result) {}

    /**
     * Called when a biometric (e.g. fingerprint, face, etc.) is presented but not recognized as
     * belonging to the user.
     *
     * @param activity {@link androidx.fragment.app.FragmentActivity} the prompt is attached to
     */
    public void onAuthenticationFailed(@Nullable FragmentActivity activity) {}
}
