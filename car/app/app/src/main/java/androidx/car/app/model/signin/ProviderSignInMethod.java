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
import androidx.car.app.model.Action;
import androidx.car.app.model.ForegroundCarColorSpan;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link SignInTemplate.SignInMethod} that allows the user to initiate sign-in with a
 * authentication provider.
 *
 * <p>Not all providers will be available on all devices. It is the developer's responsibility to
 * verify the presence of the corresponding provider by using the provider's own APIs. For
 * example, for Google Sign In, check
 * <a href="https://developers.google.com/identity/sign-in/android/sign-in">Integrating Google
 * Sign-In into Your Android App</a>).
 */
@RequiresCarApi(2)
@CarProtocol
@KeepFields
public final class ProviderSignInMethod implements SignInTemplate.SignInMethod {
    private final @Nullable Action mAction;

    /**
     * Creates a {@link ProviderSignInMethod} instance with the given provider {@link Action}.
     *
     * <h4>Requirements</h4>
     *
     * The provider action must not be a standard action, and it must use a
     * {@link androidx.car.app.model.ParkedOnlyOnClickListener}.
     *
     * <p>The action's title color can be customized with {@link ForegroundCarColorSpan}
     * instances, any other spans will be ignored by the host.
     *
     * @throws IllegalArgumentException if {@code action} does not meet the requirements
     * @throws NullPointerException     if {@code action} is {@code null}
     * @see Action
     * @see androidx.car.app.model.ParkedOnlyOnClickListener
     */
    public ProviderSignInMethod(@NonNull Action action) {
        if (requireNonNull(action).getType() != Action.TYPE_CUSTOM) {
            throw new IllegalArgumentException("The action must not be a standard action");
        }
        if (!requireNonNull(action.getOnClickDelegate()).isParkedOnly()) {
            throw new IllegalArgumentException("The action must use a "
                    + "ParkedOnlyOnClickListener");
        }
        mAction = action;
    }

    /**
     * Returns the {@link Action} the user can use to initiate the sign-in with a given provider.
     */
    public @NonNull Action getAction() {
        return requireNonNull(mAction);
    }

    @Override
    public @NonNull String toString() {
        return "[action:" + mAction + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ProviderSignInMethod)) {
            return false;
        }

        ProviderSignInMethod that = (ProviderSignInMethod) other;
        return Objects.equals(mAction, that.mAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAction);
    }

    /** Constructs an empty instance, used by serialization code. */
    private ProviderSignInMethod() {
        mAction = null;
    }
}
