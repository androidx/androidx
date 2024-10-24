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

import android.net.Uri;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link SignInTemplate.SignInMethod} that presents a QR Code that the user can use to sign-in.
 */
@RequiresCarApi(4)
@CarProtocol
@KeepFields
@SuppressWarnings("AcronymName")
public final class QRCodeSignInMethod implements SignInTemplate.SignInMethod {
    private final @Nullable Uri mUri;

    /**
     * Returns a {@link QRCodeSignInMethod} instance.
     *
     * @param uri the URL to be used in creating a QR Code.
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public QRCodeSignInMethod(@NonNull Uri uri) {
        mUri = requireNonNull(uri);
    }

    /** Returns the {@link Uri} to use to create a QR Code to present to the user. */
    public @NonNull Uri getUri() {
        return requireNonNull(mUri);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof QRCodeSignInMethod)) {
            return false;
        }

        QRCodeSignInMethod that = (QRCodeSignInMethod) other;
        return Objects.equals(mUri, that.mUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUri);
    }

    /** Constructs an empty instance, used by serialization code. */
    private QRCodeSignInMethod() {
        mUri = null;
    }
}
