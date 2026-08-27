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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Arrays;
import java.util.List;

public class PackageIdentifierCtsTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testGetters() {
        byte[] cert = new byte[] {100};
        PackageIdentifier packageIdentifier =
                new PackageIdentifier("com.packageName", /* sha256Certificate= */ cert);
        assertThat(packageIdentifier.getPackageName()).isEqualTo("com.packageName");
        assertThat(packageIdentifier.getSha256Certificate()).isEqualTo(cert);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testGetters_singleCertInMultiCert() {
        byte[] cert = new byte[] {100};
        PackageIdentifier packageIdentifier =
                new PackageIdentifier("com.packageName", /* sha256Certificate= */ cert);
        assertThat(packageIdentifier.getPackageName()).isEqualTo("com.packageName");
        assertThat(packageIdentifier.getSha256Certificate()).isEqualTo(cert);
        assertThat(packageIdentifier.getMultiSignerSha256Certificates()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testGetters_multiCert() {
        byte[] cert1 = new byte[] {100};
        byte[] cert2 = new byte[] {101};
        PackageIdentifier packageIdentifier =
                new PackageIdentifier("com.packageName", List.of(cert1, cert2));
        assertThat(packageIdentifier.getPackageName()).isEqualTo("com.packageName");
        assertThat(packageIdentifier.getSha256Certificate()).isEqualTo(cert1);
        assertThat(packageIdentifier.getMultiSignerSha256Certificates()).hasSize(2);
        assertThat(packageIdentifier.getMultiSignerSha256Certificates().get(0)).isEqualTo(cert1);
        assertThat(packageIdentifier.getMultiSignerSha256Certificates().get(1)).isEqualTo(cert2);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testPackageIdentifier_emptyCertList_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PackageIdentifier("com.packageName", List.of()));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testPackageIdentifier_nullCertInList_throwsException() {
        List<byte[]> certsWithNull = Arrays.asList(new byte[] {100}, null);
        assertThrows(
                NullPointerException.class,
                () -> new PackageIdentifier("com.packageName", certsWithNull));
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testEqualsAndHashCode_multiCert() {
        byte[] cert1 = new byte[] {100};
        byte[] cert2 = new byte[] {101};
        PackageIdentifier pkg1 = new PackageIdentifier("com.pkg", List.of(cert1, cert2));
        PackageIdentifier pkg2 = new PackageIdentifier("com.pkg", List.of(cert1, cert2));
        PackageIdentifier pkg3 = new PackageIdentifier("com.pkg", List.of(cert1));

        assertThat(pkg1).isEqualTo(pkg2);
        assertThat(pkg1.hashCode()).isEqualTo(pkg2.hashCode());
        assertThat(pkg1).isNotEqualTo(pkg3);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PACKAGE_IDENTIFIER_MULTI_CERT)
    public void testPackageIdentifier_nullList_throwsException() {
        assertThrows(
                NullPointerException.class,
                () -> new PackageIdentifier("com.pkg", (List<byte[]>) null));
    }
}
