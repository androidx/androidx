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
import androidx.appsearch.app.GetWrappedBlobEncryptionKeysRequest;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import com.google.common.collect.ImmutableSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Collection;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
public class GetWrappedBlobEncryptionKeysRequestCtsTest {
    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildAndGet() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "package", "database", "namespace");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "package", "database", "namespace");

        byte[] serverPublicKey = generateRandomBytes(32);
        byte[] serverAttestation = generateRandomBytes(16);

        GetWrappedBlobEncryptionKeysRequest request =
                new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, serverAttestation)
                        .addBlobHandles(handle1)
                        .addBlobHandles(ImmutableSet.of(handle2))
                        .build();

        assertThat(request.getBlobHandles()).containsExactly(handle1, handle2);
        assertThat(request.getTrustedComponentPublicKey()).isEqualTo(serverPublicKey);
        assertThat(request.getTrustedComponentAttestation()).isEqualTo(serverAttestation);
    }

    @Test
    public void testRebuild() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "package", "database", "namespace");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "package", "database", "namespace");

        byte[] serverPublicKey = generateRandomBytes(32);
        byte[] serverAttestation = generateRandomBytes(16);

        GetWrappedBlobEncryptionKeysRequest.Builder builder =
                new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, serverAttestation)
                        .addBlobHandles(handle1);
        GetWrappedBlobEncryptionKeysRequest original = builder.build();

        GetWrappedBlobEncryptionKeysRequest rebuild = builder.addBlobHandles(handle2).build();

        assertThat(original.getBlobHandles()).containsExactly(handle1);
        assertThat(rebuild.getBlobHandles()).containsExactly(handle1, handle2);
    }

    @Test
    public void testNullArguments_throwsNullPointerException() {
        byte[] serverPublicKey = generateRandomBytes(32);
        byte[] serverAttestation = generateRandomBytes(16);

        assertThrows(
                NullPointerException.class,
                () -> new GetWrappedBlobEncryptionKeysRequest.Builder(null, serverAttestation));
        assertThrows(
                NullPointerException.class,
                () -> new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, null));

        GetWrappedBlobEncryptionKeysRequest.Builder builder =
                new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, serverAttestation);
        assertThrows(
                NullPointerException.class,
                () -> builder.addBlobHandles((AppSearchBlobHandle[]) null));
        assertThrows(
                NullPointerException.class,
                () -> builder.addBlobHandles((Collection<AppSearchBlobHandle>) null));
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        byte[] digest = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle =
                AppSearchBlobHandle.createWithSha256(digest, "package", "database", "namespace");
        byte[] serverPublicKey = generateRandomBytes(32);
        byte[] serverAttestation = generateRandomBytes(16);

        GetWrappedBlobEncryptionKeysRequest request1 =
                new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, serverAttestation)
                        .addBlobHandles(handle)
                        .build();
        GetWrappedBlobEncryptionKeysRequest request2 =
                new GetWrappedBlobEncryptionKeysRequest.Builder(serverPublicKey, serverAttestation)
                        .addBlobHandles(handle)
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }
}
