/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.safeparcel;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An implemented creator for {@link PackageIdentifierParcel}.
 *
 * <p>In Jetpack, {@link androidx.appsearch.app.PackageIdentifier} is serialized in a bundle for
 * {@link androidx.appsearch.app.GetSchemaResponse}, and therefore needs to implement a real
 * {@link Parcelable}.
 */
// @exportToFramework:skipFile()
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PackageIdentifierParcelCreator implements Parcelable.Creator<PackageIdentifierParcel> {
    private static final String PACKAGE_NAME_FIELD = "packageName";
    private static final String SHA256_CERTIFICATE_FIELD = "sha256Certificate";
    private static final String SHA256_CERTIFICATES_FIELD = "sha256Certificates";

    public PackageIdentifierParcelCreator() {
    }

    /**
     * Creates a {@link PackageIdentifierParcel} from a {@link Bundle}
     */
    private static @NonNull PackageIdentifierParcel createPackageIdentifierFromBundle(
            @NonNull Bundle packageIdentifierBundle) {
        Objects.requireNonNull(packageIdentifierBundle);
        String packageName =
                Objects.requireNonNull(packageIdentifierBundle.getString(PACKAGE_NAME_FIELD));
        @SuppressWarnings("deprecation")
        List<Bundle> certBundles =
                packageIdentifierBundle.getParcelableArrayList(SHA256_CERTIFICATES_FIELD);
        if (certBundles != null && !certBundles.isEmpty()) {
            byte[][] sha256Certificates = new byte[certBundles.size()][];
            for (int i = 0; i < certBundles.size(); i++) {
                Bundle certBundle = certBundles.get(i);
                if (certBundle != null) {
                    sha256Certificates[i] = certBundle.getByteArray(SHA256_CERTIFICATE_FIELD);
                }
            }
            return new PackageIdentifierParcel(packageName, sha256Certificates);
        }
        byte[] sha256Certificate =
                Objects.requireNonNull(
                        packageIdentifierBundle.getByteArray(SHA256_CERTIFICATE_FIELD));

        return new PackageIdentifierParcel(packageName, sha256Certificate);
    }

    /** Creates a {@link Bundle} from a {@link PackageIdentifierParcel}. */
    private static @NonNull Bundle createBundleFromPackageIdentifier(
            @NonNull PackageIdentifierParcel packageIdentifierParcel) {
        Objects.requireNonNull(packageIdentifierParcel);
        Bundle packageIdentifierBundle = new Bundle();
        packageIdentifierBundle.putString(PACKAGE_NAME_FIELD,
                packageIdentifierParcel.getPackageName());
        packageIdentifierBundle.putByteArray(SHA256_CERTIFICATE_FIELD,
                packageIdentifierParcel.getSha256Certificate());

        byte[][] multiSignerSha256Certificates =
                packageIdentifierParcel.getMultiSignerSha256Certificates();
        if (multiSignerSha256Certificates != null) {
            ArrayList<Bundle> bundles = new ArrayList<>(multiSignerSha256Certificates.length);
            for (int i = 0; i < multiSignerSha256Certificates.length; i++) {
                Bundle byteArray = new Bundle();
                byteArray.putByteArray(
                        SHA256_CERTIFICATE_FIELD, multiSignerSha256Certificates[i]);
                bundles.add(byteArray);
            }
            packageIdentifierBundle.putParcelableArrayList(SHA256_CERTIFICATES_FIELD, bundles);
        }

        return packageIdentifierBundle;
    }

    @Override
    public @NonNull PackageIdentifierParcel createFromParcel(Parcel parcel) {
        Bundle bundle = Objects.requireNonNull(parcel.readBundle(getClass().getClassLoader()));
        return createPackageIdentifierFromBundle(bundle);
    }

    @Override
    public PackageIdentifierParcel @NonNull [] newArray(int size) {
        return new PackageIdentifierParcel[size];
    }

    /** Writes a {@link PackageIdentifierParcel} to a {@link Parcel}. */
    public static void writeToParcel(@NonNull PackageIdentifierParcel packageIdentifierParcel,
            android.os.@NonNull Parcel parcel, int flags) {
        parcel.writeBundle(createBundleFromPackageIdentifier(packageIdentifierParcel));
    }
}
