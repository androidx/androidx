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

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.CommitBlobResponse;
import androidx.appsearch.testutil.AppSearchTestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class CommitBlobResponseTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildAndGet() throws Exception {
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] data2 = generateRandomBytes(10); // 10 Bytes
        byte[] digest2 = calculateDigest(data2);
        byte[] data3 = generateRandomBytes(10); // 10 Bytes
        byte[] digest3 = calculateDigest(data3);
        byte[] data4 = generateRandomBytes(10); // 10 Bytes
        byte[] digest4 = calculateDigest(data4);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(
                digest1, "package1", "db1", "ns");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(
                digest2, "package1", "db1", "ns");
        AppSearchBlobHandle blobHandle3 = AppSearchBlobHandle.createWithSha256(
                digest3, "package1", "db1", "ns");
        AppSearchBlobHandle blobHandle4 = AppSearchBlobHandle.createWithSha256(
                digest4, "package1", "db1", "ns");

        AppSearchResult<Void> failureResult = AppSearchResult.newFailedResult(
                AppSearchResult.RESULT_ALREADY_EXISTS, "already exists");
        AppSearchResult<Void> successResult = AppSearchResult.newSuccessfulResult(null);

        AppSearchBatchResult<AppSearchBlobHandle, Void> batchResult =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, Void>()
                        .setSuccess(blobHandle1, null)
                        .setFailure(blobHandle2, AppSearchResult.RESULT_ALREADY_EXISTS,
                                "already exists")
                        .setResult(blobHandle3, successResult)
                        .setResult(blobHandle4, failureResult)
                        .build();

        CommitBlobResponse commitBlobResponse =
                new CommitBlobResponse(batchResult);

        AppSearchBatchResult<AppSearchBlobHandle, Void> outResult =
                commitBlobResponse.getResult();
        assertThat(outResult.getSuccesses()).containsExactly(
                blobHandle1, null, blobHandle3, null);
        assertThat(outResult.getFailures()).containsExactly(
                blobHandle2, failureResult, blobHandle4, failureResult);
        assertThat(outResult.getAll()).containsExactly(
                blobHandle1, successResult, blobHandle2, failureResult,
                blobHandle3, successResult, blobHandle4, failureResult);
    }
}
