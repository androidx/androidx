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

package androidx.appsearch.cts.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.testutil.AppSearchTestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class AppSearchBlobHandleCtsTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testCreateBlobHandle() throws Exception {
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package1", "db1", "ns");
        assertThat(blobHandle.getPackageName()).isEqualTo("package1");
        assertThat(blobHandle.getDatabaseName()).isEqualTo("db1");
        assertThat(blobHandle.getNamespace()).isEqualTo("ns");
        assertThat(blobHandle.getSha256Digest()).isEqualTo(digest);
    }

    @Test
    public void testBlobHandleIdentical() throws Exception {
        byte[] data = {(byte) 1};
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db", "ns");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db", "ns");
        assertThat(blobHandle1).isEqualTo(blobHandle2);
        assertThat(blobHandle1.hashCode()).isEqualTo(blobHandle2.hashCode());
    }

    @Test
    public void testBlobHandleNotIdentical() throws Exception {
        byte[] data1 = {(byte) 1};
        byte[] data2 = {(byte) 2};
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(
                digest1, "package1", "db1", "ns1");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(
                digest2, "package1", "db1", "ns1");
        AppSearchBlobHandle blobHandle3 = AppSearchBlobHandle.createWithSha256(
                digest1, "package2", "db1", "ns1");
        AppSearchBlobHandle blobHandle4 = AppSearchBlobHandle.createWithSha256(
                digest1, "package1", "db2", "ns1");
        AppSearchBlobHandle blobHandle5 = AppSearchBlobHandle.createWithSha256(
                digest1, "package1", "db1", "ns2");
        assertThat(blobHandle1).isNotEqualTo(blobHandle2);
        assertThat(blobHandle1).isNotEqualTo(blobHandle3);
        assertThat(blobHandle1).isNotEqualTo(blobHandle4);
        assertThat(blobHandle1).isNotEqualTo(blobHandle5);
        assertThat(blobHandle2).isNotEqualTo(blobHandle3);
        assertThat(blobHandle2).isNotEqualTo(blobHandle4);
        assertThat(blobHandle2).isNotEqualTo(blobHandle5);
        assertThat(blobHandle3).isNotEqualTo(blobHandle4);
        assertThat(blobHandle3).isNotEqualTo(blobHandle5);
        assertThat(blobHandle4).isNotEqualTo(blobHandle5);
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle2.hashCode());
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle3.hashCode());
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle4.hashCode());
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle5.hashCode());
        assertThat(blobHandle2.hashCode()).isNotEqualTo(blobHandle3.hashCode());
        assertThat(blobHandle2.hashCode()).isNotEqualTo(blobHandle4.hashCode());
        assertThat(blobHandle2.hashCode()).isNotEqualTo(blobHandle5.hashCode());
        assertThat(blobHandle3.hashCode()).isNotEqualTo(blobHandle4.hashCode());
        assertThat(blobHandle3.hashCode()).isNotEqualTo(blobHandle5.hashCode());
        assertThat(blobHandle4.hashCode()).isNotEqualTo(blobHandle5.hashCode());
    }

    @Test
    public void testCreateBlobHandle_invalidDigest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppSearchBlobHandle.createWithSha256(
                        new byte[10], "package1", "db1", "ns"));
        assertThat(exception).hasMessageThat().contains("The digest is not a SHA-256 digest");
    }

    @Test
    public void testBlobHandleToString() throws Exception {
        byte[] data = new byte[]{(byte) 1, (byte) 3, (byte) 5, (byte) 7};
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db", "ns");
        String expectedString = "{\n"
                + "  packageName: \"package\",\n"
                + "  databaseName: \"db\",\n"
                + "  namespace: \"ns\",\n"
                + "  digest: \"e6e8cb429864b8e8d7fe95e53360dbd00756316813fb1e4a03a778d4632dbde6\","
                + "\n}";

        assertThat(blobHandle.toString()).isEqualTo(expectedString);
    }
}
