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

import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;

import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.os.ParcelFileDescriptor;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.OpenBlobForWriteResponse;
import androidx.appsearch.testutil.AppSearchTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.File;

public class OpenBlobForWriteResponseTest {
    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    ParcelFileDescriptor mPfd;
    private AppSearchResult<ParcelFileDescriptor> mSuccessResult;
    private AppSearchResult<ParcelFileDescriptor> mFailureResult;

    @Before
    public void setUp() throws Exception {
        File file = File.createTempFile(/*prefix=*/"appsearch", /*suffix=*/null);
        mPfd = ParcelFileDescriptor.open(file, MODE_WRITE_ONLY);
        mSuccessResult = AppSearchResult.newSuccessfulResult(mPfd);
        mFailureResult = AppSearchResult.newFailedResult(
                AppSearchResult.RESULT_ALREADY_EXISTS, "already exists");
    }

    @After
    public void tearDown() throws Exception {
        mPfd.close();
    }

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

        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> batchResult =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor>()
                        .setSuccess(blobHandle1, mPfd)
                        .setFailure(blobHandle2, AppSearchResult.RESULT_ALREADY_EXISTS,
                                "already exists")
                        .setResult(blobHandle3, mSuccessResult)
                        .setResult(blobHandle4, mFailureResult)
                        .build();

        try (OpenBlobForWriteResponse response =
                new OpenBlobForWriteResponse(batchResult)) {

            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> outResult =
                    response.getResult();
            assertThat(outResult.getSuccesses()).containsExactly(
                    blobHandle1, mPfd, blobHandle3, mPfd);
            assertThat(outResult.getFailures()).containsExactly(
                    blobHandle2, mFailureResult, blobHandle4, mFailureResult);
            assertThat(outResult.getAll()).containsExactly(
                    blobHandle1, mSuccessResult, blobHandle2, mFailureResult,
                    blobHandle3, mSuccessResult, blobHandle4, mFailureResult);
        }
    }

    @Test
    public void testAccessPfdAfterClose() throws Exception {
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle = AppSearchBlobHandle.createWithSha256(
                digest, "package1", "db1", "ns");
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> batchResult =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor>()
                        .setResult(blobHandle, mSuccessResult)
                        .build();
        try (OpenBlobForWriteResponse ignored =
                    new OpenBlobForWriteResponse(batchResult)) {
            // Pfd is accessible now
            mPfd.detachFd();
        }
        // Pfd is NOT accessible after close()
        assertThrows(IllegalStateException.class, () -> mPfd.detachFd());
    }
}
