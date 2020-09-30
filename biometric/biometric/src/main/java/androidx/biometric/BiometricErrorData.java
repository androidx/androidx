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

package androidx.biometric;

import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * A container for data associated with a biometric authentication error, which may be handled by
 * the client application's callback.
 */
class BiometricErrorData {
    /**
     * An integer ID associated with this error.
     */
    @BiometricPrompt.AuthenticationError private final int mErrorCode;

    /**
     * A human-readable message that describes the error.
     */
    @Nullable private final CharSequence mErrorMessage;

    BiometricErrorData(int errorCode, @Nullable CharSequence errorMessage) {
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
    }

    @BiometricPrompt.AuthenticationError
    int getErrorCode() {
        return mErrorCode;
    }

    @Nullable
    CharSequence getErrorMessage() {
        return mErrorMessage;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {mErrorCode, convertToString(mErrorMessage)});
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof BiometricErrorData) {
            final BiometricErrorData other = (BiometricErrorData) obj;
            return mErrorCode == other.mErrorCode && isErrorMessageEqualTo(other.mErrorMessage);
        }
        return false;
    }

    /**
     * Checks if a given error message is equivalent to the one for this object.
     *
     * @param otherMessage A message to compare to the error message for this object.
     * @return Whether the error message for this object and {@code otherMessage} are equivalent.
     */
    private boolean isErrorMessageEqualTo(@Nullable CharSequence otherMessage) {
        final String errorString = convertToString(mErrorMessage);
        final String otherString = convertToString(otherMessage);
        return (errorString == null && otherString == null)
                || (errorString != null && errorString.equals(otherString));
    }

    /**
     * Converts a nullable {@link CharSequence} message to an equivalent {@link String}.
     *
     * @param message The message to be converted.
     * @return A string matching the given message, or {@code null} if message is {@code null}.
     */
    @Nullable
    private static String convertToString(@Nullable CharSequence message) {
        return message != null ? message.toString() : null;
    }
}
