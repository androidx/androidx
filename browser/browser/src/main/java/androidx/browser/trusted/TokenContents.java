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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Transforms a package name and a list of fingerprints (each {@code byte[]}) into a single
 * {@code byte[]} and back for serialization.
 *
 * It serializes into the byte[] eagerly, but deserializes lazily as that may not be needed.
 *
 * This class is used by {@link Token} which also takes care of how to determine and how to compare
 * signatures on different versions of Android.
 */
final class TokenContents {
    /**
     * The serialization format of a Token is as follows:
     * - UTF string containing the package name.
     * - Integer containing the number of signatures.
     * - For each signature:
     *   - Integer containing the signature length.
     *   - The signature as a byte array.
     *
     * The signatures are stored in lexicographically sorted order. This allows us to compare two
     * Tokens by doing a raw comparison of the underlying byte array.
     */
    @NonNull private final byte[] mContents;

    @Nullable private String mPackageName;
    @Nullable private List<byte[]> mFingerprints;

    @NonNull
    static TokenContents deserialize(@NonNull byte[] serialized) {
        return new TokenContents(serialized);
    }

    private TokenContents(@NonNull byte[] contents) {
        mContents = contents;
    }

    @NonNull
    static TokenContents create(String packageName, List<byte[]> fingerprints)
            throws IOException {
        return new TokenContents(
                createToken(packageName, fingerprints), packageName, fingerprints);
    }

    private TokenContents(@NonNull byte[] contents, @NonNull String packageName,
            @NonNull List<byte[]> fingerprints) {
        mContents = contents;
        mPackageName = packageName;
        mFingerprints = new ArrayList<>(fingerprints.size());

        // Defensive copy.
        for (byte[] fingerprint : fingerprints) {
            mFingerprints.add(Arrays.copyOf(fingerprint, fingerprint.length));
        }
    }

    @NonNull
    public String getPackageName() throws IOException {
        parseIfNeeded();
        if (mPackageName == null) throw new IllegalStateException();  // Required for NullAway.
        return mPackageName;
    }

    public int getFingerprintCount() throws IOException {
        parseIfNeeded();
        if (mFingerprints == null) throw new IllegalStateException();  // Required for NullAway.
        return mFingerprints.size();
    }

    @NonNull
    public byte[] getFingerprint(int i) throws IOException {
        parseIfNeeded();
        if (mFingerprints == null) throw new IllegalStateException();  // Required for NullAway.
        return Arrays.copyOf(mFingerprints.get(i), mFingerprints.get(i).length);
    }

    @NonNull
    public byte[] serialize() {
        return Arrays.copyOf(mContents, mContents.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenContents that = (TokenContents) o;
        return Arrays.equals(mContents, that.mContents);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mContents);
    }

    @NonNull
    private static byte[] createToken(@NonNull String packageName,
            @NonNull List<byte[]> fingerprints) throws IOException {
        // The entries of signatures may be in any order, we sort them so that we can just do
        // a byte by byte comparison on the resulting byte array.
        Collections.sort(fingerprints, TokenContents::compareByteArrays);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream writer = new DataOutputStream(baos);

        writer.writeUTF(packageName);
        writer.writeInt(fingerprints.size());
        for (byte[] fingerprint : fingerprints) {
            writer.writeInt(fingerprint.length);
            writer.write(fingerprint);
        }
        writer.flush();

        return baos.toByteArray();
    }

    /**
     * Compares two byte arrays lexicographically.
     */
    private static int compareByteArrays(byte[] a, byte[] b) {
        if (a == b) return 0;

        // null is less than a non-null array.
        if (a == null) return -1;
        if (b == null) return 1;

        // Compare each element.
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (a[i] != b[i]) return a[i] - b[i];
        }

        // Shorter array is less than a longer array.
        if (a.length != b.length) return a.length - b.length;

        // Arrays are equal.
        return 0;
    }

    private void parseIfNeeded() throws IOException {
        if (mPackageName != null) return;

        DataInputStream reader = new DataInputStream(new ByteArrayInputStream(mContents));
        mPackageName = reader.readUTF();

        int numFingerprints = reader.readInt();
        mFingerprints = new ArrayList<>(numFingerprints);
        for (int i = 0; i < numFingerprints; i++) {
            int size = reader.readInt();
            byte[] fingerprint = new byte[size];
            int bytesRead = reader.read(fingerprint);
            if (bytesRead != size) throw new IllegalStateException("Could not read fingerprint");
            mFingerprints.add(fingerprint);
        }
    }
}
