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

package androidx.security.app.authenticator;

import androidx.annotation.NonNull;

/**
 * This {@code Exception} is thrown when the XML configuration file is not in the proper format
 * to instantiate a new {@link AppAuthenticator}.
 */
public class AppAuthenticatorXmlException extends Exception {
    /**
     * Creates a new {@code AppAuthenticatorXmlException} with the provided {@code message}.
     */
    AppAuthenticatorXmlException(@NonNull String message) {
        super(message);
    }

    /**
     * Creates a new {@code AppAuthenticationXmlException} with the provided {@code message} and
     * the specified {@code cause}.
     */
    AppAuthenticatorXmlException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }
}
