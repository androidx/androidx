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

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.GetWrappedBlobEncryptionKeysResponse;
import androidx.appsearch.app.WrappedBlobEncryptionKey;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
public class GetWrappedBlobEncryptionKeysResponseCtsTest {
    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testCreate() throws Exception {
        byte[] digest1 = calculateDigest(generateRandomBytes(10));
        byte[] digest2 = calculateDigest(generateRandomBytes(10));
        AppSearchBlobHandle handle1 =
                AppSearchBlobHandle.createWithSha256(digest1, "package", "database", "namespace");
        AppSearchBlobHandle handle2 =
                AppSearchBlobHandle.createWithSha256(digest2, "package", "database", "namespace");

        WrappedBlobEncryptionKey wrappedKey =
                new WrappedBlobEncryptionKey(generateRandomBytes(80), new byte[] {1, 2, 3});
        AppSearchBatchResult<AppSearchBlobHandle, WrappedBlobEncryptionKey> batchResult =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, WrappedBlobEncryptionKey>()
                        .setSuccess(handle1, wrappedKey)
                        .setFailure(handle2, AppSearchResult.RESULT_NOT_FOUND, "not found")
                        .build();

        GetWrappedBlobEncryptionKeysResponse response =
                new GetWrappedBlobEncryptionKeysResponse(batchResult);
        assertThat(response.getResult().getSuccesses()).containsExactly(handle1, wrappedKey);
        assertThat(response.getResult().getFailures().keySet()).containsExactly(handle2);
        assertThat(response.getResult().getFailures().get(handle2).getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }
}
