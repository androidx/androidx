/*
 * Copyright 2024 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.Parcel;

import org.junit.Test;

public class PackageIdentifierParcelTest {
    @Test
    public void testGetters_singleCert() {
        byte[] cert = new byte[] {10, 20};
        PackageIdentifierParcel parcel =
                new PackageIdentifierParcel("com.example.pkg", cert);
        assertThat(parcel.getPackageName()).isEqualTo("com.example.pkg");
        assertThat(parcel.getSha256Certificate()).isEqualTo(cert);
        assertThat(parcel.getMultiSignerSha256Certificates()).isNull();
    }

    @Test
    public void testGetters_multiCert() {
        byte[] cert1 = new byte[] {10, 20};
        byte[] cert2 = new byte[] {30, 40};
        byte[][] certs = new byte[][] {cert1, cert2};
        PackageIdentifierParcel parcel =
                new PackageIdentifierParcel("com.example.pkg", certs);
        assertThat(parcel.getPackageName()).isEqualTo("com.example.pkg");
        assertThat(parcel.getSha256Certificate()).isEqualTo(cert1);
        assertThat(parcel.getMultiSignerSha256Certificates()).isEqualTo(certs);
    }

    @Test
    public void testConstructor_nulls_throwsException() {
        assertThrows(
                NullPointerException.class,
                () -> new PackageIdentifierParcel(null, new byte[] {1}));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageIdentifierParcel("com.example.pkg", (byte[]) null, null));
        assertThrows(
                NullPointerException.class,
                () -> new PackageIdentifierParcel("com.example.pkg", new byte[][] {null}));
    }

    @Test
    public void testEqualsAndHashCode() {
        byte[] cert1 = new byte[] {10, 20};
        byte[] cert2 = new byte[] {30, 40};
        PackageIdentifierParcel parcel1 =
                new PackageIdentifierParcel("com.example.pkg", new byte[][] {cert1, cert2});
        PackageIdentifierParcel parcel2 =
                new PackageIdentifierParcel("com.example.pkg", new byte[][] {cert1, cert2});
        PackageIdentifierParcel parcel3 =
                new PackageIdentifierParcel("com.example.pkg", cert1);

        assertThat(parcel1).isEqualTo(parcel2);
        assertThat(parcel1.hashCode()).isEqualTo(parcel2.hashCode());
        assertThat(parcel1).isNotEqualTo(parcel3);
    }

    @Test
    public void testParcelAndUnparcel_singleCert() {
        byte[] cert = new byte[] {10, 20};
        PackageIdentifierParcel expected =
                new PackageIdentifierParcel("com.example.pkg", cert);

        Parcel parcel = Parcel.obtain();
        try {
            expected.writeToParcel(parcel, /* flags= */ 0);
            parcel.setDataPosition(0);
            @SuppressWarnings("deprecation")
            PackageIdentifierParcel actual =
                    PackageIdentifierParcel.CREATOR.createFromParcel(parcel);
            assertThat(actual).isEqualTo(expected);
            assertThat(actual.getSha256Certificate()).isEqualTo(cert);
            assertThat(actual.getMultiSignerSha256Certificates()).isNull();
        } finally {
            parcel.recycle();
        }
    }

    @Test
    public void testParcelAndUnparcel_multiCert() {
        byte[] cert1 = new byte[] {10, 20};
        byte[] cert2 = new byte[] {30, 40};
        byte[][] certs = new byte[][] {cert1, cert2};
        PackageIdentifierParcel expected =
                new PackageIdentifierParcel("com.example.pkg", certs);

        Parcel parcel = Parcel.obtain();
        try {
            expected.writeToParcel(parcel, /* flags= */ 0);
            parcel.setDataPosition(0);
            @SuppressWarnings("deprecation")
            PackageIdentifierParcel actual =
                    PackageIdentifierParcel.CREATOR.createFromParcel(parcel);
            assertThat(actual).isEqualTo(expected);
            assertThat(actual.getSha256Certificate()).isEqualTo(cert1);
            assertThat(actual.getMultiSignerSha256Certificates()).isEqualTo(certs);
        } finally {
            parcel.recycle();
        }
    }
}
