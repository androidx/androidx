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

/**
 * A wrapper class for {@link Class2BiometricAuthPrompt}, {@link Class3BiometricAuthPrompt}
 * {@link Class2BiometricOrCredentialAuthPrompt}, {@link Class3BiometricOrCredentialAuthPrompt}
 * and {@link CredentialAuthPrompt} that allows for cancellation and dismissal of the current
 * authentication prompt.
 */
public interface AuthPrompt {
    /**
     * Cancels authentication of the current BiometricPrompt.
     */
    void cancelAuthentication();
}
