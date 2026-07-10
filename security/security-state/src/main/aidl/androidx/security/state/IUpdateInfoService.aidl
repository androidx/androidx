/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.security.state;

import androidx.security.state.IUpdateInfoSession;

/**
 * Factory interface for establishing a security update information session.
 *
 * <p>This interface serves as the primary IPC entry point for the Security State library.
 * Trusted system applications (such as the System Updater or Google Play Store)
 * implement this interface to expose their pending security update information to the
 * {@link androidx.security.state.SecurityPatchState} client library.
 *
 * <p>Host applications must declare a bound service in their manifest that handles the
 * {@code "androidx.security.state.provider.UPDATE_INFO_SERVICE"} action and returns
 * an implementation of this factory.
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IUpdateInfoService {

    /**
     * Creates a unique, stateful session for the calling client.
     *
     * <p>This method initiates the handshake between the client application and the
     * update provider. The provider uses this method to capture the caller's identity
     * securely on the Binder thread before establishing the session.
     *
     * <h3>Security & Validation</h3>
     * To prevent Intent spoofing, the provider must validate that the provided {@code packageName}
     * is officially owned by the Kernel-verified Linux UID of the calling process.
     * If the validation fails, the provider will throw a {@link SecurityException}.
     *
     * @param packageName The package name of the calling application. This is used for
     *                    accurate attribution, telemetry, and debugging on the provider side.
     * @param clientToken An anonymous Binder token provided by the client. The provider
     *                    uses this token to register a {@link android.os.IBinder.DeathRecipient},
     *                    allowing it to clean up resources and log disconnection telemetry
     *                    if the client process crashes unexpectedly.
     * @return An {@link IUpdateInfoSession} instance dedicated to this specific client,
     *         which can be used to query the latest update information.
     */
    IUpdateInfoSession openSession(String packageName, IBinder clientToken);
}