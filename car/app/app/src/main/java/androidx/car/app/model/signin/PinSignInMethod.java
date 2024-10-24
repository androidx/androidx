/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.model.signin;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link SignInTemplate.SignInMethod} that presents a PIN or activation code that the user can
 * use to sign-in.
 */
@RequiresCarApi(2)
@CarProtocol
@KeepFields
public final class PinSignInMethod implements SignInTemplate.SignInMethod {
    /** Maximum length, in characters, for a PIN. */
    private static final int MAX_PIN_LENGTH = 12;

    private final @Nullable CarText mPinCode;

    /**
     * Returns a {@link PinSignInMethod} instance.
     *
     * <h4>Requirements</h4>
     *
     * <p>The provided pin must be no more than 12 characters long. To facilitate typing this
     * code, it is recommended restricting the string to a limited set (for example, numbers,
     * upper-case letters, hexadecimal, etc.).
     *
     * <p>Spans are not supported in the pin and will be ignored.
     *
     * @param pinCode the PIN to display is empty.
     * @throws IllegalArgumentException if {@code pin} is empty or longer than 12 characters.
     * @throws NullPointerException     if {@code pin} is {@code null}
     */
    // TODO(b/183750545): check that no spans are present in the input pin.
    public PinSignInMethod(@NonNull CharSequence pinCode) {
        int pinLength = requireNonNull(pinCode).length();
        if (pinLength == 0) {
            throw new IllegalArgumentException("PIN must not be empty");
        }
        if (pinLength > MAX_PIN_LENGTH) {
            throw new IllegalArgumentException(
                    "PIN must not be longer than " + MAX_PIN_LENGTH + " characters");
        }
        mPinCode = CarText.create(pinCode);
    }

    /** Returns the PIN or activation code to present to the user. */
    public @NonNull CarText getPinCode() {
        return requireNonNull(mPinCode);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PinSignInMethod)) {
            return false;
        }

        PinSignInMethod that = (PinSignInMethod) other;
        return Objects.equals(mPinCode, that.mPinCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPinCode);
    }

    /** Constructs an empty instance, used by serialization code. */
    private PinSignInMethod() {
        mPinCode = null;
    }
}
