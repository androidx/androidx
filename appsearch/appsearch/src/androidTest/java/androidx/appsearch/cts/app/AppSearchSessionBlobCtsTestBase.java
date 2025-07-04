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

import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_ARGUMENT;
import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.os.ParcelFileDescriptor;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.CommitBlobResponse;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.OpenBlobForReadResponse;
import androidx.appsearch.app.OpenBlobForWriteResponse;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SetBlobVisibilityRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AppSearchSessionBlobCtsTestBase {
    static final String DB_NAME_1 = "";

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();
    private final String mPackageName =
            ApplicationProvider.getApplicationContext().getPackageName();

    private AppSearchSession mDb1;
    private AppSearchBlobHandle mHandle1;
    private AppSearchBlobHandle mHandle2;
    private byte[] mData1;
    private byte[] mData2;

    protected abstract ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName) throws Exception;

    @Before
    public void setUp() throws Exception {
        mDb1 = createSearchSessionAsync(DB_NAME_1).get();

        mData1 = generateRandomBytes(10); // 10 Bytes
        mData2 = generateRandomBytes(20); // 20 Bytes
        byte[] digest1 = calculateDigest(mData1);
        byte[] digest2 = calculateDigest(mData2);
        mHandle1 = AppSearchBlobHandle.createWithSha256(
                digest1, mPackageName, DB_NAME_1, "namespace");
        mHandle2 = AppSearchBlobHandle.createWithSha256(
                digest2, mPackageName, DB_NAME_1, "namespace");

        // Cleanup whatever documents may still exist in these databases. This is needed in
        // addition to tearDown in case a test exited without completing properly.
        cleanup();
    }

    @After
    public void tearDown() throws Exception {
        // Cleanup whatever documents may still exist in these databases.
        cleanup();
    }

    private void cleanup() throws Exception {
        mDb1.setSchemaAsync(
                new SetSchemaRequest.Builder().setForceOverride(true).build()).get();
        if (Flags.enableBlobStore()
                && mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE)) {
            // Only clear blobs if the flags and feature is on.
            mDb1.removeBlobAsync(ImmutableSet.of(mHandle1, mHandle2)).get();
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testWriteAndReadBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1, mHandle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
                outputStream.write(mData1);
                outputStream.flush();
            }

            ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(mHandle2);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
                outputStream.write(mData2);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1, mHandle2)).get().getResult()
                .isSuccess());

        byte[] readBytes1 = new byte[10]; // 10 Bytes
        byte[] readBytes2 = new byte[20]; // 20 Bytes

        try (OpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1, mHandle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertTrue(readResult.isSuccess());

            ParcelFileDescriptor readPfd1 = readResult.getSuccesses().get(mHandle1);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readPfd1)) {
                inputStream.read(readBytes1);
            }
            assertThat(readBytes1).isEqualTo(mData1);

            ParcelFileDescriptor readPfd2 = readResult.getSuccesses().get(mHandle2);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readPfd2)) {
                inputStream.read(readBytes2);
            }
            assertThat(readBytes2).isEqualTo(mData2);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testWriteAfterCommit() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        // Write data without close the pfd for write
        ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
        try (FileOutputStream outputStream = new FileOutputStream(writePfd.getFileDescriptor())) {
            outputStream.write(mData1);
            outputStream.flush();
        }

        // Commit the blob will revoke the pfd for write.
        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get().getResult()
                .isSuccess());

        // Cannot keep writing to the blob after commit.
        assertThrows(IOException.class, () -> {
            try (FileOutputStream outputStream =
                         new FileOutputStream(writePfd.getFileDescriptor())) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemovePendingBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        }

        // Remove the blob
        assertTrue(mDb1.removeBlobAsync(ImmutableSet.of(mHandle1)).get().getResult().isSuccess());

        // Commit will return NOT_FOUND
        CommitBlobResponse commitBlobResponse =
                mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get();
        AppSearchBatchResult<AppSearchBlobHandle, Void> commitResult =
                commitBlobResponse.getResult();
        assertFalse(commitResult.isSuccess());
        assertThat(commitResult.getFailures().keySet()).containsExactly(mHandle1);
        assertThat(commitResult.getFailures().get(mHandle1).getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(commitResult.getFailures().get(mHandle1).getErrorMessage())
                .contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemoveCommittedBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        try (OpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get().getResult()
                .isSuccess());

        // Remove the committed blob
        assertTrue(mDb1.removeBlobAsync(ImmutableSet.of(mHandle1)).get().getResult().isSuccess());

        // Read will return NOT_FOUND
        OpenBlobForReadResponse readBlobResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                readBlobResponse.getResult();
        assertFalse(readResult.isSuccess());
        assertThat(readResult.getFailures().keySet()).containsExactly(mHandle1);
        assertThat(readResult.getFailures().get(mHandle1).getResultCode())
                .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(readResult.getFailures().get(mHandle1).getErrorMessage())
                .contains("Cannot find the blob for handle");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRemoveAndReWriteBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        try (OpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            byte[] wrongData = generateRandomBytes(10); // 10 Bytes
            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(wrongData);
                outputStream.flush();
            }
        }

        // Remove the blob
        assertTrue(mDb1.removeBlobAsync(ImmutableSet.of(mHandle1)).get().getResult().isSuccess());

        try (OpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get().getResult().isSuccess());

        byte[] readBytes = new byte[10]; // 10 Bytes

        try (OpenBlobForReadResponse readResponse =
                     mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertTrue(readResult.isSuccess());

            ParcelFileDescriptor readPfd = readResult.getSuccesses().get(mHandle1);
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readPfd)) {
                inputStream.read(readBytes);
            }
            assertThat(readBytes).isEqualTo(mData1);
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testWriteAndReadBlob_withoutCommit() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().setForceOverride(true).build()).get();

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        }

        // Read blob without commit the blob first.
        try (OpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertFalse(readResult.isSuccess());

            assertThat(readResult.getFailures().keySet()).containsExactly(mHandle1);
            assertThat(readResult.getFailures().get(mHandle1).getResultCode())
                    .isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
            assertThat(readResult.getFailures().get(mHandle1).getErrorMessage())
                    .contains("Cannot find the blob for handle");
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testRewrite_notAllowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            // write wrong data into it.
            byte[] wrongData = generateRandomBytes(10); // 10 Bytes
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(wrongData);
                outputStream.flush();
            }
        }

        // Open a new write session and rewrite is allowed before commit.
        try (OpenBlobForWriteResponse reWriteResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> reWriteResult =
                    reWriteResponse.getResult();
            assertTrue(reWriteResult.isSuccess());
            ParcelFileDescriptor rewritePfd = reWriteResult.getSuccesses().get(mHandle1);

            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(rewritePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }

            // Commit the blob
            assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get()
                    .getResult().isSuccess());

            // Rewrite is not allowed once committed.
            reWriteResult = mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get().getResult();
            assertThat(reWriteResult.isSuccess()).isFalse();
            assertThat(reWriteResult.getFailures().get(mHandle1).getResultCode())
                    .isEqualTo(AppSearchResult.RESULT_ALREADY_EXISTS);
            assertThat(reWriteResult.getFailures().get(mHandle1).getErrorMessage())
                    .contains("Rewriting the committed blob is not allowed");
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenWriteForRead_allowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);

            // Read on openWrite is allowed since openWriteBlob returns read and write fd.
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    writePfd)) {
                inputStream.read(mData1);
            }
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testOpenReadForWrite_notAllowed() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());
            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData1);
                outputStream.flush();
            }
        }

        // Commit the blob
        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get().getResult().isSuccess());

        try (OpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                    readResponse.getResult();
            assertTrue(readResult.isSuccess());
            ParcelFileDescriptor readPfd = readResult.getSuccesses().get(mHandle1);
            // Cannot write on openRead since openRead returns read only fd.
            assertThrows(IOException.class, () -> {
                try (OutputStream outputStream =
                             new ParcelFileDescriptor.AutoCloseOutputStream(readPfd)) {
                    outputStream.write(mData1);
                    outputStream.flush();
                }
            });
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testCommitBlobWithWrongDigest() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());
            ParcelFileDescriptor writePfd = writeResult.getSuccesses().get(mHandle1);

            // Write data2 to pfd which is opened by blob handle for data1.
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
                outputStream.write(mData2);
                outputStream.flush();
            }
        }

        // Commit the blob
        CommitBlobResponse commitBlobResponse =
                mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)).get();
        AppSearchBatchResult<AppSearchBlobHandle, Void> commitResult =
                commitBlobResponse.getResult();
        assertThat(commitResult.isSuccess()).isFalse();
        assertThat(commitResult.getFailures().get(mHandle1).getResultCode())
                .isEqualTo(RESULT_INVALID_ARGUMENT);
        assertThat(commitResult.getFailures().get(mHandle1).getErrorMessage())
                .contains("The blob content doesn't match to the digest");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetStorageInfo() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        StorageInfo before = mDb1.getStorageInfoAsync().get();

        OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1, mHandle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(mHandle1);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
            outputStream.write(mData1);
            outputStream.flush();
        }

        ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(mHandle2);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
            outputStream.write(mData2);
            outputStream.flush();
        }
        writeResponse.close();

        StorageInfo after = mDb1.getStorageInfoAsync().get();
        assertThat(after.getBlobsCount()).isEqualTo(before.getBlobsCount() + 2);
        assertThat(after.getBlobsSizeBytes()).isEqualTo(
                before.getBlobsSizeBytes() + mData1.length + mData2.length);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testGetStorageInfoAfterRemoveBlob() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        StorageInfo before = mDb1.getStorageInfoAsync().get();

        OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1, mHandle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(mHandle1);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
            outputStream.write(mData1);
            outputStream.flush();
        }

        ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(mHandle2);
        try (OutputStream outputStream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
            outputStream.write(mData2);
            outputStream.flush();
        }
        writeResponse.close();

        StorageInfo after = mDb1.getStorageInfoAsync().get();
        assertThat(after.getBlobsCount()).isEqualTo(before.getBlobsCount() + 2);
        assertThat(after.getBlobsSizeBytes()).isEqualTo(
                before.getBlobsSizeBytes() + mData1.length + mData2.length);

        // remove blob 1
        mDb1.removeBlobAsync(ImmutableSet.of(mHandle1)).get();
        StorageInfo afterRemove1 = mDb1.getStorageInfoAsync().get();
        assertThat(afterRemove1.getBlobsCount()).isEqualTo(before.getBlobsCount() + 1);
        assertThat(afterRemove1.getBlobsSizeBytes()).isEqualTo(
                before.getBlobsSizeBytes() + mData2.length);

        // remove blob 2
        mDb1.removeBlobAsync(ImmutableSet.of(mHandle2)).get();
        StorageInfo afterRemove2 = mDb1.getStorageInfoAsync().get();
        assertThat(afterRemove2.getBlobsCount()).isEqualTo(before.getBlobsCount());
        assertThat(afterRemove2.getBlobsSizeBytes()).isEqualTo(before.getBlobsSizeBytes());
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testCloseWriteResponse() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        OpenBlobForWriteResponse writeResponse =
                mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1, mHandle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                writeResponse.getResult();
        assertTrue(writeResult.isSuccess());

        // Close the response will also close all fds.
        writeResponse.close();

        assertThrows(IOException.class, () -> {
            try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                    writeResult.getSuccesses().get(mHandle1))) {
                outputStream.write(mData1);
            }
        });
        assertThrows(IOException.class, () -> {
            try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(
                    writeResult.getSuccesses().get(mHandle2))) {
                outputStream.write(mData2);
            }
        });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testCloseReadResponse() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        try (OpenBlobForWriteResponse writeResponse =
                     mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1, mHandle2)).get()) {
            AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> writeResult =
                    writeResponse.getResult();
            assertTrue(writeResult.isSuccess());

            ParcelFileDescriptor writePfd1 = writeResult.getSuccesses().get(mHandle1);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd1)) {
                outputStream.write(mData1);
                outputStream.flush();
            }

            ParcelFileDescriptor writePfd2 = writeResult.getSuccesses().get(mHandle2);
            try (OutputStream outputStream =
                         new ParcelFileDescriptor.AutoCloseOutputStream(writePfd2)) {
                outputStream.write(mData2);
                outputStream.flush();
            }
        }

        assertTrue(mDb1.commitBlobAsync(ImmutableSet.of(mHandle1, mHandle2)).get().getResult()
                .isSuccess());

        byte[] readBytes1 = new byte[10]; // 10 Bytes
        byte[] readBytes2 = new byte[20]; // 20 Bytes

        OpenBlobForReadResponse readResponse =
                mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1, mHandle2)).get();
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> readResult =
                readResponse.getResult();
        assertTrue(readResult.isSuccess());

        // Close the response will also close all fds.
        readResponse.close();

        assertThrows(IOException.class, () -> {
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readResult.getSuccesses().get(mHandle1))) {
                inputStream.read(readBytes1);
            }
        });
        assertThrows(IOException.class, () -> {
            try (InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(
                    readResult.getSuccesses().get(mHandle2))) {
                inputStream.read(readBytes2);
            }
        });
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testSetBlobSchema() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setDescription("this is a blob.")
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        GetSchemaResponse getSchemaResponse = mDb1.getSchemaAsync().get();
        assertThat(getSchemaResponse.getSchemas()).containsExactly(schema);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testPutDocumentWithBlobProperty() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setDescription("this is a blob.")
                        .build())
                .build();
        mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build()).get();

        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyBlobHandle("blob", handle)
                .build();

        mDb1.putAsync(new PutDocumentsRequest.Builder().addGenericDocuments(document).build())
                .get();

        AppSearchBatchResult<String, GenericDocument> getResult = mDb1.getByDocumentIdAsync(
                new GetByDocumentIdRequest.Builder("namespace")
                        .addIds("id")
                        .build()).get();
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getSuccesses().get("id")).isEqualTo(document);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testSetBlobVisibility() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        if (mDb1.getFeatures().isFeatureSupported(Features.ISOLATED_STORAGE)) {
            assumeTrue(Flags.enableAppSearchManageBlobFiles());
        }

        mDb1.setBlobVisibilityAsync(new SetBlobVisibilityRequest.Builder()
                .setNamespaceDisplayedBySystem("namespace1", /*displayed=*/false)
                .setNamespaceDisplayedBySystem("namespace1", /*displayed=*/true)
                .setNamespaceDisplayedBySystem("namespace2", /*displayed=*/false)
                .addNamespaceVisibleToConfig("namespace3",
                        new SchemaVisibilityConfig.Builder().build())
                .clearNamespaceVisibleToConfigs("namespace3")
                .addNamespaceVisibleToConfig("namespace3",
                        new SchemaVisibilityConfig.Builder().build())
                .build()).get();
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testBlobApis_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.openBlobForWriteAsync(ImmutableSet.of(mHandle1)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.commitBlobAsync(ImmutableSet.of(mHandle1)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.openBlobForReadAsync(ImmutableSet.of(mHandle1)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.removeBlobAsync(ImmutableSet.of(mHandle1)));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setBlobVisibilityAsync(new SetBlobVisibilityRequest.Builder()
                        .setNamespaceDisplayedBySystem("namespace", /*displayed=*/false)
                        .build()).get());
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testSetBlobSchema_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setSchemaAsync(new SetSchemaRequest.Builder().addSchemas(schema).build())
                        .get());
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_BLOB_STORE)
    public void testPutDocumentWithBlobProperty_notSupported() throws Exception {
        assumeFalse(mDb1.getFeatures().isFeatureSupported(Features.BLOB_STORAGE));

        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, mPackageName, DB_NAME_1, "namespace");
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyBlobHandle("blob", handle)
                .build();

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.putAsync(new PutDocumentsRequest.Builder()
                                .addGenericDocuments(document).build()).get());
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
        exception = assertThrows(UnsupportedOperationException.class,
                () -> mDb1.setBlobVisibilityAsync(new SetBlobVisibilityRequest.Builder().build()));
        assertThat(exception).hasMessageThat().contains(
                Features.BLOB_STORAGE + " is not available on this AppSearch implementation.");
    }
}
