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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

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

    public PackageIdentifierParcelCreator() {
    }

    /**
     * Creates a {@link PackageIdentifierParcel} from a {@link Bundle}
     */
    @NonNull
    private static PackageIdentifierParcel createPackageIdentifierFromBundle(
            @NonNull Bundle packageIdentifierBundle) {
        Objects.requireNonNull(packageIdentifierBundle);
        String packageName =
                Preconditions.checkNotNull(packageIdentifierBundle.getString(PACKAGE_NAME_FIELD));
        byte[] sha256Certificate =
                Preconditions.checkNotNull(
                        packageIdentifierBundle.getByteArray(SHA256_CERTIFICATE_FIELD));

        return new PackageIdentifierParcel(packageName, sha256Certificate);
    }

    /** Creates a {@link Bundle} from a {@link PackageIdentifierParcel}. */
    @NonNull
    private static Bundle createBundleFromPackageIdentifier(
            @NonNull PackageIdentifierParcel packageIdentifierParcel) {
        Objects.requireNonNull(packageIdentifierParcel);
        Bundle packageIdentifierBundle = new Bundle();
        packageIdentifierBundle.putString(PACKAGE_NAME_FIELD,
                packageIdentifierParcel.getPackageName());
        packageIdentifierBundle.putByteArray(SHA256_CERTIFICATE_FIELD,
                packageIdentifierParcel.getSha256Certificate());

        return packageIdentifierBundle;
    }

    @NonNull
    @Override
    public PackageIdentifierParcel createFromParcel(Parcel parcel) {
        Bundle bundle = Preconditions.checkNotNull(parcel.readBundle(getClass().getClassLoader()));
        return createPackageIdentifierFromBundle(bundle);
    }

    @NonNull
    @Override
    public PackageIdentifierParcel[] newArray(int size) {
        return new PackageIdentifierParcel[size];
    }

    /** Writes a {@link PackageIdentifierParcel} to a {@link Parcel}. */
    public static void writeToParcel(@NonNull PackageIdentifierParcel packageIdentifierParcel,
            @NonNull android.os.Parcel parcel, int flags) {
        parcel.writeBundle(createBundleFromPackageIdentifier(packageIdentifierParcel));
    }
}
