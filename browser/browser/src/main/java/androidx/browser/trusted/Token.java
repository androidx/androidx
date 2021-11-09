/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.trusted;

import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Stores a package's identity, a combination of its package name and signing certificate.
 *
 * It is designed to be persistable as a {@code byte[]}, so you can do this:
 *
 * <pre>
 * {@code
 * Token token = Token.create(packageName, packageManager);
 * byte[] serialized = token.serialize();
 * // Persist serialized.
 *
 * // Some time later...
 * Token verified = Token.deserialize(serialized);
 * verified.matches(packageName, packageManager);
 * }
 * </pre>
 */
public final class Token {
    private static final String TAG = "Token";

    @NonNull
    private final TokenContents mContents;

    /**
     * Creates a {@link Token} for the given package, taking into account the package name
     * and its signing signature.
     * @param packageName The name of the package.
     * @param packageManager A {@link PackageManager} to determine the signing information.
     * @return A {@link Token}. {@code null} if the provided package cannot be found (the
     *         app is not installed).
     */
    @Nullable
    public static Token create(@NonNull String packageName,
            @NonNull PackageManager packageManager) {
        List<byte[]> fingerprints =
                PackageIdentityUtils.getFingerprintsForPackage(packageName, packageManager);
        if (fingerprints == null) return null;

        try {
            return new Token(TokenContents.create(packageName, fingerprints));
        } catch (IOException e) {
            Log.e(TAG, "Exception when creating token.", e);
            return null;
        }
    }

    /**
     * Reconstructs a {@link Token} from the output of its {@link #serialize}.
     * @param serialized The result of a {@link Token#serialize} call.
     * @return The deserialized Token.
     */
    @NonNull
    public static Token deserialize(@NonNull byte[] serialized) {
        return new Token(TokenContents.deserialize(serialized));
    }

    private Token(@NonNull TokenContents contents) {
        mContents = contents;
    }

    /**
     * Serializes the {@link Token} to a form that can later be restored by the {@link #deserialize}
     * method.
     * @return A serialization of the {@link Token}.
     */
    @NonNull
    public byte[] serialize() {
        return mContents.serialize();
    }

    /**
     * Whether the given package matches the package that was used to create the original Token.
     * @param packageName The name of the package.
     * @param packageManager A {@link PackageManager} to get information about the package.
     * @return Whether the given package currently is the same as the one used to create the Token.
     */
    public boolean matches(@NonNull String packageName, @NonNull PackageManager packageManager) {
        return PackageIdentityUtils.packageMatchesToken(packageName, packageManager, mContents);
    }

}
