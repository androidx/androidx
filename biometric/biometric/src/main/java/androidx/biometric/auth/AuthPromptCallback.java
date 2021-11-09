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
import androidx.biometric.BiometricPrompt.AuthenticationError;
import androidx.fragment.app.FragmentActivity;

/**
 * A collection of methods that may be invoked by an auth prompt during authentication.
 *
 * <p>Each method receives a reference to the (possibly {@code null}) activity instance that is
 * currently hosting the prompt. This reference should be used to fetch or update any necessary
 * activity state in order for changes to be reflected across configuration changes.
 */
public abstract class AuthPromptCallback {
    /**
     * Called when an unrecoverable error has been encountered and authentication has stopped.
     *
     * <p>After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param activity  The activity that is currently hosting the prompt.
     * @param errorCode An integer ID associated with the error.
     * @param errString A human-readable string that describes the error.
     */
    public void onAuthenticationError(
            @Nullable FragmentActivity activity,
            @AuthenticationError int errorCode,
            @NonNull CharSequence errString) {}

    /**
     * Called when the user has successfully authenticated.
     *
     * <p>After this method is called, no further events will be sent for the current
     * authentication session.
     *
     * @param activity The activity that is currently hosting the prompt.
     * @param result   An object containing authentication-related data.
     */
    public void onAuthenticationSucceeded(
            @Nullable FragmentActivity activity,
            @NonNull BiometricPrompt.AuthenticationResult result) {}

    /**
     * Called when an authentication attempt by the user has been rejected.
     *
     * @param activity The activity that is currently hosting the prompt.
     */
    public void onAuthenticationFailed(@Nullable FragmentActivity activity) {}
}
