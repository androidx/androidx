/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.appsearch.app.CommitEncryptedBlobRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
public class CommitEncryptedBlobRequestCtsTest {
    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildAndGet() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "pkg", "db", "ns");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "pkg", "db", "ns");
        byte[] symmetricKey = generateRandomBytes(32);
        byte[] ownerTag = new byte[] {7, 8, 9};

        CommitEncryptedBlobRequest request =
                new CommitEncryptedBlobRequest.Builder(symmetricKey, ownerTag)
                        .addBlobHandle(handle1, new byte[] {1, 2, 3})
                        .addBlobHandle(handle2)
                        .build();

        assertThat(request.getSymmetricKey()).isEqualTo(symmetricKey);
        assertThat(request.getOwnerTag()).isEqualTo(ownerTag);
        assertThat(request.getBlobHandles()).containsExactly(handle1, handle2);
        assertThat(request.getAeadParams().keySet()).containsExactly(handle1);
        assertThat(request.getAeadParams().get(handle1)).isEqualTo(new byte[] {1, 2, 3});
    }

    @Test
    public void testRebuild() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "pkg", "db", "ns");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "pkg", "db", "ns");

        CommitEncryptedBlobRequest.Builder builder =
                new CommitEncryptedBlobRequest.Builder(generateRandomBytes(32), new byte[] {7})
                        .addBlobHandle(handle1, new byte[] {1, 2, 3});
        CommitEncryptedBlobRequest original = builder.build();

        CommitEncryptedBlobRequest rebuild =
                builder.addBlobHandle(handle2, new byte[] {4, 5, 6}).build();

        assertThat(original.getBlobHandles()).containsExactly(handle1);
        assertThat(original.getAeadParams().keySet()).containsExactly(handle1);
        assertThat(rebuild.getBlobHandles()).containsExactly(handle1, handle2);
        assertThat(rebuild.getAeadParams().keySet()).containsExactly(handle1, handle2);
    }

    @Test
    public void testNullArguments_throwsNullPointerException() throws Exception {
        byte[] digest = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle =
                AppSearchBlobHandle.createWithSha256(digest, "pkg", "db", "ns");
        byte[] symmetricKey = generateRandomBytes(32);
        byte[] ownerTag = new byte[] {7, 8, 9};

        assertThrows(
                NullPointerException.class,
                () -> new CommitEncryptedBlobRequest.Builder(/* symmetricKey= */ null, ownerTag));
        assertThrows(
                NullPointerException.class,
                () -> new CommitEncryptedBlobRequest.Builder(symmetricKey, /* ownerTag= */ null));
        CommitEncryptedBlobRequest.Builder builder =
                new CommitEncryptedBlobRequest.Builder(symmetricKey, ownerTag);
        assertThrows(NullPointerException.class, () -> builder.addBlobHandle(/* handle= */ null));
        assertThrows(
                NullPointerException.class,
                () -> builder.addBlobHandle(/* handle= */ null, new byte[] {1, 2, 3}));
        assertThrows(
                NullPointerException.class,
                () -> builder.addBlobHandle(handle, /* aeadParams= */ null));
    }

    @Test
    public void testAddBlobHandle_sameHandleWithoutAeadParams_removesAeadParams() throws Exception {
        byte[] digest = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle =
                AppSearchBlobHandle.createWithSha256(digest, "pkg", "db", "ns");

        CommitEncryptedBlobRequest request =
                new CommitEncryptedBlobRequest.Builder(generateRandomBytes(32), new byte[] {7})
                        .addBlobHandle(handle, new byte[] {1, 2, 3})
                        .addBlobHandle(handle)
                        .build();

        assertThat(request.getBlobHandles()).containsExactly(handle);
        assertThat(request.getAeadParams()).isEmpty();
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "pkg", "db", "ns");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "pkg", "db", "ns");
        byte[] symmetricKey = generateRandomBytes(32);

        CommitEncryptedBlobRequest request1 =
                new CommitEncryptedBlobRequest.Builder(symmetricKey, new byte[] {7, 8, 9})
                        .addBlobHandle(handle1, new byte[] {10, 20, 30})
                        .addBlobHandle(handle2)
                        .build();
        CommitEncryptedBlobRequest request2 =
                new CommitEncryptedBlobRequest.Builder(symmetricKey, new byte[] {7, 8, 9})
                        .addBlobHandle(handle2)
                        .addBlobHandle(handle1, new byte[] {10, 20, 30})
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    public void testEquals_differentSymmetricKey_notEqual() throws Exception {
        CommitEncryptedBlobRequest request1 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {1}, new byte[] {7}).build();
        CommitEncryptedBlobRequest request2 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {2}, new byte[] {7}).build();

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    public void testEquals_differentOwnerTag_notEqual() throws Exception {
        CommitEncryptedBlobRequest request1 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {1}, new byte[] {7}).build();
        CommitEncryptedBlobRequest request2 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {1}, new byte[] {8}).build();

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    public void testEquals_differentAeadParams_notEqual() throws Exception {
        byte[] digest = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle =
                AppSearchBlobHandle.createWithSha256(digest, "pkg", "db", "ns");

        CommitEncryptedBlobRequest request1 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {1}, new byte[] {7})
                        .addBlobHandle(handle, new byte[] {10, 20, 30})
                        .build();
        CommitEncryptedBlobRequest request2 =
                new CommitEncryptedBlobRequest.Builder(new byte[] {1}, new byte[] {7})
                        .addBlobHandle(handle)
                        .build();

        assertThat(request1).isNotEqualTo(request2);
    }

    @Test
    public void testBuilder_copiesTheGivenArrays() throws Exception {
        byte[] digest = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle =
                AppSearchBlobHandle.createWithSha256(digest, "pkg", "db", "ns");
        byte[] symmetricKey = new byte[] {1, 2, 3};
        byte[] ownerTag = new byte[] {4, 5, 6};
        byte[] aeadParams = new byte[] {7, 8, 9};
        CommitEncryptedBlobRequest request =
                new CommitEncryptedBlobRequest.Builder(symmetricKey, ownerTag)
                        .addBlobHandle(handle, aeadParams)
                        .build();

        symmetricKey[0] = 100;
        ownerTag[0] = 100;
        aeadParams[0] = 100;

        assertThat(request.getSymmetricKey()).isEqualTo(new byte[] {1, 2, 3});
        assertThat(request.getOwnerTag()).isEqualTo(new byte[] {4, 5, 6});
        assertThat(request.getAeadParams().get(handle)).isEqualTo(new byte[] {7, 8, 9});
    }
}
