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

package androidx.appsearch.localstorage;

import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.InternalPutDocumentResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.QueryStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.stats.BaseStats;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsDisabled;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.collection.ArrayMap;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.MaintainAnnIndexOptions;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.StorageInfoProto;
import com.google.android.icing.proto.TermMatchType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link AppSearchImpl#optimize} and {@link AppSearchImpl#checkForOptimize}.
 *
 * <p>Subclasses decide whether the {@link AppSearchImpl} under test uses the 2-stage optimize
 * (see {@link AppSearchConfig#enableUpgradableLockAndTwoStageOptimize()}), so that every test here
 * runs against both the single-stage and the 2-stage optimize.
 */
@SuppressWarnings({"GuardedBy", "deprecation"})
public abstract class AppSearchImplOptimizeTestBase {
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    protected static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    protected final Context mContext = ApplicationProvider.getApplicationContext();

    protected File mAppSearchDir;
    protected AppSearchImpl mAppSearchImpl;
    protected AppSearchConfig mUnlimitedConfig;
    protected final CallerAccess mSelfCallerAccess =
            new CallerAccess(mContext.getPackageName());

    /** Returns whether the {@link AppSearchImpl} under test should use the 2-stage optimize. */
    protected abstract boolean enableUpgradableLockAndTwoStageOptimize();

    /**
     * Creates an {@link AppSearchConfig} with unlimited limits, the given
     * {@link IcingOptionsConfig}, and {@link #enableUpgradableLockAndTwoStageOptimize()}.
     */
    protected @NonNull AppSearchConfig createConfig(
            @NonNull IcingOptionsConfig icingOptionsConfig) {
        return createConfig(new UnlimitedLimitConfig(), icingOptionsConfig);
    }

    /**
     * Creates an {@link AppSearchConfig} with the given {@link LimitConfig} and
     * {@link IcingOptionsConfig}, and {@link #enableUpgradableLockAndTwoStageOptimize()}.
     */
    protected @NonNull AppSearchConfig createConfig(@NonNull LimitConfig limitConfig,
            @NonNull IcingOptionsConfig icingOptionsConfig) {
        return new AppSearchConfigImpl(
                limitConfig,
                icingOptionsConfig,
                /* storeParentInfoAsSyntheticProperty= */ false,
                /* shouldRetrieveParentInfo= */ false,
                /* persistToDiskRecoveryProof= */ false,
                enableUpgradableLockAndTwoStageOptimize());
    }

    @Before
    public void setUp() throws Exception {
        mUnlimitedConfig = createConfig(new LocalStorageIcingOptionsConfig());
        mAppSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                AppSearchUserPlugins.EMPTY,
                ALWAYS_OPTIMIZE);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testConfigEnableTwoStageOptimize() {
        assertThat(mUnlimitedConfig.enableUpgradableLockAndTwoStageOptimize())
                .isEqualTo(enableUpgradableLockAndTwoStageOptimize());
    }

    @Test
    public void testOptimize_removesDeletedDocumentsAndKeepsAliveDocuments() throws Exception {
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id1", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.remove("package", "database", "namespace", "id1",
                /*removeStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null);

        GetOptimizeInfoResultProto optimizeInfo =
                mAppSearchImpl.getOptimizeInfoResultLocked(/*callStatsBuilder=*/ null);
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(1);

        OptimizeStats.Builder optimizeStatsBuilder = new OptimizeStats.Builder();
        mAppSearchImpl.optimize(optimizeStatsBuilder, /*callStatsBuilder=*/ null);

        // The deleted document has been compacted away.
        optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked(/*callStatsBuilder=*/ null);
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(0);
        assertThat(optimizeInfo.getEstimatedOptimizableBytes()).isEqualTo(0);

        // The alive document is still retrievable and the deleted one is still gone.
        assertThat(mAppSearchImpl.getDocument("package", "database", "namespace", "id2",
                /*typePropertyPaths=*/ Collections.emptyMap(),
                /*callStatsBuilder=*/ null)).isEqualTo(document2);
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument("package", "database", "namespace", "id1",
                        /*typePropertyPaths=*/ Collections.emptyMap(),
                        /*callStatsBuilder=*/ null));
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        // The native stats have been populated.
        OptimizeStats optimizeStats = optimizeStatsBuilder.build();
        assertThat(optimizeStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(optimizeStats.getOriginalDocumentCount()).isEqualTo(2);
        assertThat(optimizeStats.getDeletedDocumentCount()).isEqualTo(1);

        // AppSearchImpl is still writable after optimize.
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document3,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        assertThat(mAppSearchImpl.getDocument("package", "database", "namespace", "id3",
                /*typePropertyPaths=*/ Collections.emptyMap(),
                /*callStatsBuilder=*/ null)).isEqualTo(document3);
    }

    @Test
    public void testTriggerCheckOptimizeByMutationSize() throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a document and then remove it to generate garbage.
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.remove("package", "database", "namespace", "id",
                /*removeStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null);

        // Verify there is garbage documents.
        GetOptimizeInfoResultProto optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked(
                /*callStatsBuilder=*/null);
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(1);

        // Increase mutation counter and stop before reach the threshold
        mAppSearchImpl.checkForOptimize(AppSearchImpl.CHECK_OPTIMIZE_INTERVAL - 1,
                /*optimizeStatsBuilder=*/null, /*callStatsBuilder=*/ null);

        // Verify the optimize() isn't triggered.
        optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked(/*callStatsBuilder=*/null);
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(1);

        // Increase the counter and reach the threshold, optimize() should be triggered.
        OptimizeStats.Builder builder = new OptimizeStats.Builder();
        mAppSearchImpl.checkForOptimize(/*mutateBatchSize=*/ 1, builder,
                /*callStatsBuilder=*/ null);

        // Verify optimize() is triggered.
        optimizeInfo = mAppSearchImpl.getOptimizeInfoResultLocked(/*callStatsBuilder=*/null);
        assertThat(optimizeInfo.getOptimizableDocs()).isEqualTo(0);
        assertThat(optimizeInfo.getEstimatedOptimizableBytes()).isEqualTo(0);

        // Verify the stats have been set.
        OptimizeStats oStats = builder.build();
        assertThat(oStats.getOriginalDocumentCount()).isEqualTo(1);
        assertThat(oStats.getDeletedDocumentCount()).isEqualTo(1);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_OPTIMIZE_RESULT_STATES)
    public void testGetNextPageAfterOptimize_flagEnabledShouldRetrievePage() throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();
        assertThat(nextPageToken).isNotEqualTo(0);

        // Call Optimize.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        // getNextPage should still be able to retrieve pages after Optimize.
        SearchResultPage searchResultPage2 =
                mAppSearchImpl.getNextPage("package1", nextPageToken,
                        /*maxResults=*/ Integer.MAX_VALUE,
                        /*statsBuilder=*/ null,
                        /*callStatsBuilder=*/null);
        // Known limitation: 2-stage optimize still evicts result states.
        // TODO(b/446719537): remove this branch once fixed.
        if (enableUpgradableLockAndTwoStageOptimize()) {
            assertThat(searchResultPage2.getResults()).isEmpty();
        } else {
            assertThat(searchResultPage2.getResults()).hasSize(1);
            assertThat(searchResultPage2.getResults().get(0).getGenericDocument())
                    .isEqualTo(document1);
        }
        assertThat(searchResultPage2.getNextPageToken()).isEqualTo(0);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_OPTIMIZE_RESULT_STATES)
    public void testGetNextPageAfterOptimize_flagDisabledShouldReturnEmptyResult()
            throws Exception {
        // Insert package1 schema
        List<AppSearchSchema> schema1 =
                ImmutableList.of(new AppSearchSchema.Builder("schema1").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package1",
                "database1",
                schema1,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert two package1 documents
        GenericDocument document1 = new GenericDocument.Builder<>("namespace", "id1",
                "schema1").build();
        GenericDocument document2 = new GenericDocument.Builder<>("namespace", "id2",
                "schema1").build();
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.putDocument(
                "package1",
                "database1",
                document2,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Query for only 1 result per page
        SearchSpec searchSpec = new SearchSpec.Builder()
                .setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                .setResultCountPerPage(1)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.globalQuery(
                /*queryExpression=*/ "",
                searchSpec,
                new CallerAccess(/*callingPackageName=*/"package1"),
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Document2 will come first because it was inserted last and default return order is
        // most recent.
        assertThat(searchResultPage.getResults()).hasSize(1);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);

        long nextPageToken = searchResultPage.getNextPageToken();
        assertThat(nextPageToken).isNotEqualTo(0);

        // Call Optimize.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        // All page tokens are evicted after optimize. getNextPage should return an empty page if
        // the flag is disabled.
        SearchResultPage searchResultPage2 =
                mAppSearchImpl.getNextPage("package1", nextPageToken,
                        /*maxResults=*/ Integer.MAX_VALUE,
                        /*statsBuilder=*/ null,
                        /*callStatsBuilder=*/null);
        assertThat(searchResultPage2.getResults()).isEmpty();
        assertThat(searchResultPage2.getNextPageToken()).isEqualTo(0);
    }

    @Test
    public void testOptimizeBlob() throws Exception {
        // Create a new AppSearchImpl with lower orphan blob time to live.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, createConfig(
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public long getOrphanBlobTimeToLiveMs() {
                                // 0 will make it non-expire
                                return 1L;
                            }
                        }),
                new AppSearchUserPlugins.Builder()
                        .setRevocableFileDescriptorStore(
                                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig)).build(),
                ALWAYS_OPTIMIZE);

        // Write the blob and commit it.
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "namespace");
        ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /*callStatsBuilder=*/null);
        try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        writePfd.close();
        mAppSearchImpl.commitBlob("package", "db1", handle,
                /*callStatsBuilder=*/null);

        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_COMMIT_BLOB,
                PersistType.Code.FULL, /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Optimize remove the expired orphan blob.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);
        AppSearchException e = assertThrows(AppSearchException.class, () -> {
            mAppSearchImpl.openReadBlob("package", "db1", handle,
                new CallerAccess("package"),
                /*callStatsBuilder=*/null);
        });
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    public void testOptimizeBlobWithDocument() throws Exception {
        // Create a new AppSearchImpl with lower orphan blob time to live.
        mAppSearchImpl.close();
        File tempFolder = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                tempFolder, createConfig(
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public long getOrphanBlobTimeToLiveMs() {
                                // 0 will make it non-expire
                                return 1L;
                            }
                        }),
                new AppSearchUserPlugins.Builder()
                        .setRevocableFileDescriptorStore(
                                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig)).build(),
                ALWAYS_OPTIMIZE);

        // Write the blob and commit it.
        byte[] data = generateRandomBytes(20); // 20 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                digest, "package", "db1", "namespace");
        ParcelFileDescriptor writePfd = mAppSearchImpl.openWriteBlob("package", "db1", handle,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /*callStatsBuilder=*/null);
        try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(writePfd)) {
            outputStream.write(data);
            outputStream.flush();
        }
        writePfd.close();
        mAppSearchImpl.commitBlob("package", "db1", handle,
                /*callStatsBuilder=*/null);

        // Put a document link that blob handle.
        AppSearchSchema schema = new AppSearchSchema.Builder("Type")
                .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build())
                .build();
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "db1",
                ImmutableList.of(schema),
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ true,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument document = new GenericDocument.Builder<>("namespace", "id", "Type")
                .setPropertyBlobHandle("blob", handle)
                .build();
        mAppSearchImpl.putDocument(
                "package",
                "db1",
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.FULL, /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Optimize won't remove the blob since it has reference document.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);
        byte[] readBytes = new byte[20];
        try (ParcelFileDescriptor readPfd =  mAppSearchImpl.openReadBlob("package", "db1", handle,
                new CallerAccess("package"),
                /*callStatsBuilder=*/null);
                InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(readPfd)) {
            inputStream.read(readBytes);
        }
        assertThat(readBytes).isEqualTo(data);

        mAppSearchImpl.remove("package", "db1",  "namespace", "id", /*statsBuilder=*/ null,
                /*callStatsBuilder=*/null);

        // The blob is orphan now and optimize will remove it.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);
        AppSearchException e = assertThrows(AppSearchException.class, () -> {
            mAppSearchImpl.openReadBlob("package", "db1", handle,
                new CallerAccess("package"),
                /*callStatsBuilder=*/null);
        });
        assertThat(e.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        assertThat(e.getMessage()).contains("Cannot find the blob for handle");
    }

    @Test
    public void testLastBlockingOperationStats() throws Exception {
        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mUnlimitedConfig,
                new AppSearchUserPlugins.Builder()
                        .setRevocableFileDescriptorStore(
                                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig)).build(),
                ALWAYS_OPTIMIZE);

        // Set a schema and check last write operation is Initialize
        CallStats.Builder callStatsBuilder = new CallStats.Builder();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        SetSchemaStats.Builder setSchemaStatsBuilder = new SetSchemaStats.Builder(
                "package", "database");
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                setSchemaStatsBuilder,
                callStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        SetSchemaStats setSchemaStats = setSchemaStatsBuilder.build();
        assertThat(setSchemaStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_INITIALIZE);
        CallStats callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_INITIALIZE);

        // Put a document and check last write operation is SetSchema
        AppSearchLogger fakeLogger = new AppSearchLogger() {
            @Override
            public void logStats(@NonNull SetSchemaStats stats) {
                assertThat(stats.getLastBlockingOperation())
                        .isEqualTo(BaseStats.CALL_TYPE_SET_SCHEMA);
            }
        };
        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "type").build();
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_SET_SCHEMA);

        // Batch put a document and check last write operation is PUT_DOCUMENT
        fakeLogger = new AppSearchLogger() {
            @Override
            public void logStats(@NonNull PutDocumentStats stats) {
                assertThat(stats.getLastBlockingOperation()).isEqualTo(
                        BaseStats.CALL_TYPE_PUT_DOCUMENT);
            }
        };
        List<GenericDocument> documents = new ArrayList<>();
        documents.add(document);
        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.batchPutDocuments(
                "package",
                "database",
                documents,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                PersistType.Code.LITE,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_PUT_DOCUMENT);

        // Search document and check last write operation is PUT_DOCUMENTS
        QueryStats.Builder queryStatsBuilder =
                new QueryStats.Builder(QueryStats.VISIBILITY_SCOPE_LOCAL, "package")
                        .setDatabase("database");
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.query(
                "package",
                "database",
                "",
                new SearchSpec.Builder().build(),
                /*callerAccess=*/ null,
                queryStatsBuilder,
                callStatsBuilder);
        assertThat(queryStatsBuilder.build().getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_PUT_DOCUMENTS);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_PUT_DOCUMENTS);

        // Global query will only blocked by write operation
        callStatsBuilder = new CallStats.Builder();
        QueryStats.Builder globalQueryStatsBuilder =
                new QueryStats.Builder(QueryStats.VISIBILITY_SCOPE_GLOBAL, "package");
        mAppSearchImpl.globalQuery(
                "",
                new SearchSpec.Builder().build(),
                mSelfCallerAccess,
                globalQueryStatsBuilder,
                callStatsBuilder);
        assertThat(globalQueryStatsBuilder.build().getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_PUT_DOCUMENTS);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_PUT_DOCUMENTS);

        // Report usage write operation will be blocked by read and write operation
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id", /*usageTimestampMillis=*/ 10, /*systemUsage=*/ false,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_GLOBAL_SEARCH);

        // Get document and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.getDocument("package", "database", "namespace",
                "id",
                Collections.emptyMap(),
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REPORT_USAGE);

        // Batch get document and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.batchGetDocuments(
                "package",
                "database",
                new GetByDocumentIdRequest.Builder("namespace").addIds("id").build(),
                mSelfCallerAccess,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REPORT_USAGE);

        // Remove document and check last write operation is REPORT_USAGE
        RemoveStats.Builder removeStatsBuilder = new RemoveStats.Builder(
                "package", "database");
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.remove("package", "database", "namespace",
                "id", removeStatsBuilder,
                callStatsBuilder);
        RemoveStats removeStats = removeStatsBuilder.build();
        assertThat(removeStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_GET_DOCUMENTS);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_GET_DOCUMENTS);

        // RemoveByQuery and check last write operation is REMOVE_DOCUMENT_BY_ID
        removeStatsBuilder = new RemoveStats.Builder(
                "package", "database");
        callStatsBuilder = new CallStats.Builder();
        Map<String, Set<String>> deletedIds = new ArrayMap<>();
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().build(), deletedIds, removeStatsBuilder,
                callStatsBuilder);
        assertThat(deletedIds).isEmpty();
        removeStats = removeStatsBuilder.build();
        assertThat(removeStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REMOVE_DOCUMENT_BY_ID);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REMOVE_DOCUMENT_BY_ID);

        // Optimize to check the last blocking operation is REMOVE_DOCUMENTS_BY_SEARCH
        OptimizeStats.Builder optimizeStatsBuilder = new OptimizeStats.Builder();
        mAppSearchImpl.optimize(optimizeStatsBuilder, /*callStatsBuilder=*/ null);
        OptimizeStats optimizeStats = optimizeStatsBuilder.build();
        assertThat(optimizeStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH);

        // Flush and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.FULL, /*logger=*/ null,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_OPTIMIZE);
        // Second call to check last blocking operation is flush
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.FULL, /*logger=*/ null,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_FLUSH);

        // Clear package data
        mAppSearchImpl.clearPackageData("nonExistPackage");

        // Write blob and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        AppSearchBlobHandle handle = AppSearchBlobHandle.createWithSha256(
                /*digest=*/ new byte[32], "package", "db1", "ns");
        try {
            mAppSearchImpl.openWriteBlob("package", "database", handle,
                    /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.INTERNAL_CALL_TYPE_PRUNE_PACKAGE_DATA);

        // Commit blob and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.commitBlob("package", "database", handle,
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_OPEN_WRITE_BLOB);

        // Remove blob and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.removeBlob("package", "database", handle,
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_COMMIT_BLOB);

        // Set blob visibility and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.setBlobNamespaceVisibility("package", "database", ImmutableList.of(),
                    /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_REMOVE_BLOB);

        // Read blob visibility and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.openReadBlob("package", "database", handle,
                    new CallerAccess("package"),
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_SET_BLOB_VISIBILITY);

        // Global read blob visibility and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.globalOpenReadBlob(handle, mSelfCallerAccess,
                    callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether the write blob is success or not, just want to verify the last
            // write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_SET_BLOB_VISIBILITY);

        // Handle expired documents and check the last blocking operation
        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.handleExpiredDocuments(callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether handle expired documents is success or not, just want to verify
            // the last write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.CALL_TYPE_GLOBAL_OPEN_READ_BLOB);

        callStatsBuilder = new CallStats.Builder();
        try {
            mAppSearchImpl.maintainAnnIndex(
                    MaintainAnnIndexOptions.getDefaultInstance(), callStatsBuilder);
        } catch (Exception e) {
            // We don't care whether maintain ann index is success or not, just want to verify
            // the last write operation.
        }
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.INTERNAL_CALL_TYPE_HANDLE_EXPIRED_DOCUMENTS_JOB);

        // Call another API to make sure the last blocking operation was set correctly.
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.globalQuery(
                "",
                new SearchSpec.Builder().build(),
                mSelfCallerAccess,
                /* logger= */ null,
                callStatsBuilder);
        callStats = callStatsBuilder.build();
        assertThat(callStats.getLastBlockingOperation())
                .isEqualTo(BaseStats.INTERNAL_CALL_TYPE_MAINTAIN_ANN_INDEX_JOB);
    }

    @Test
    public void testGetVmLatency() throws Exception {
        CallStats.Builder callStatsBuilder = new CallStats.Builder();

        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                createConfig(new LimitConfig() {
                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return 80;
                    }

                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxAccumulatedResultBytes() {
                        return Integer.MAX_VALUE;
                    }
                }, new LocalStorageIcingOptionsConfig()),
                new AppSearchUserPlugins.Builder()
                        .setCallStatsBuilder(callStatsBuilder)
                        .setRevocableFileDescriptorStore(
                                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig)).build(),
                ALWAYS_OPTIMIZE);

        // initialize + GetSchema + GetStorage + SetVisibilitySchema + SetBlobVisibilitySchema
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(5);

        // Set a schema
        callStatsBuilder = new CallStats.Builder();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /*setSchemaStatsBuilder=*/ null,
                callStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // Put a document and check last write operation is SetSchema
        AppSearchLogger fakeLogger = new AppSearchLogger() {};
        callStatsBuilder = new CallStats.Builder();
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // Batch put 3 documents, the batch size is set to 80 Byte, and each doc will have ~66 Byte
        // This will have 3 batches
        callStatsBuilder = new CallStats.Builder();
        List<GenericDocument> documents = new ArrayList<>();
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);
        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package",
                "database",
                documents,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                PersistType.Code.LITE,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(3);

        // Search document
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.query(
                "package", "database", "",
                new SearchSpec.Builder().build(), /*callerAccess=*/ null,
                /*queryStatsBuilder=*/ null, callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // Report usage
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id", /*usageTimestampMillis=*/ 10, /*systemUsage=*/ false,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // Remove document
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.remove("package", "database", "namespace",
                "id", /*removeStatsBuilder=*/ null, callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // RemoveByQuery
        Map<String, Set<String>> deletedIds = new ArrayMap<>();
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().build(), deletedIds, /*removeStatsBuilder=*/ null,
                callStatsBuilder);
        assertThat(deletedIds).containsKey("namespace");
        assertThat(deletedIds.get("namespace")).containsExactly("id2", "id3");
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);

        // Optimize
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, callStatsBuilder);
        // 2-stage optimize records optimizeSwapToDestDirectory, plus persistToDisk (FULL) unless
        // the optimize improvements are enabled.
        // TODO(b/448657720): Also expect optimizePrepareDestDirectory (3, or 2 if the optimize
        //  improvements are enabled) once AppSearchImpl records it.
        int expectedNumIcingCalls =
                enableUpgradableLockAndTwoStageOptimize() && !Flags.enableOptimizeImprovements()
                        ? 2 : 1;
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(expectedNumIcingCalls);

        // Flush
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.FULL, /*logger=*/ null,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getNumIcingCalls()).isEqualTo(1);
    }

    @Test
    public void testGetCallStatsBytes() throws Exception {
        CallStats.Builder callStatsBuilder = new CallStats.Builder();

        mAppSearchImpl.close();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                createConfig(new LimitConfig() {
                    @Override
                    public int getMaxByteLimitForBatchPut() {
                        return 80;
                    }

                    @Override
                    public int getMaxDocumentSizeBytes() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getPerPackageDocumentCountLimit() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getDocumentCountLimitStartThreshold() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxSuggestionCount() {
                        return Integer.MAX_VALUE;
                    }

                    @Override
                    public int getMaxOpenBlobCount() {
                        return 2;
                    }

                    @Override
                    public int getMaxAccumulatedResultBytes() {
                        return Integer.MAX_VALUE;
                    }
                }, new LocalStorageIcingOptionsConfig()),
                new AppSearchUserPlugins.Builder()
                        .setCallStatsBuilder(callStatsBuilder)
                        .setRevocableFileDescriptorStore(
                                new JetpackRevocableFileDescriptorStore(mUnlimitedConfig)).build(),
                ALWAYS_OPTIMIZE);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Set a schema
        callStatsBuilder = new CallStats.Builder();
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /*setSchemaStatsBuilder=*/ null,
                callStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Put a document
        AppSearchLogger fakeLogger = new AppSearchLogger() {};
        callStatsBuilder = new CallStats.Builder();
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type").build();
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type").build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                document1,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Batch put 3 documents, the batch size is set to 80 Byte, and each doc will have ~66 Byte
        // This will have 3 batches
        callStatsBuilder = new CallStats.Builder();
        List<GenericDocument> documents = new ArrayList<>();
        documents.add(document1);
        documents.add(document2);
        documents.add(document3);
        AppSearchBatchResult.Builder<String, InternalPutDocumentResponse> resultBuilder =
                new AppSearchBatchResult.Builder<>();
        mAppSearchImpl.batchPutDocuments(
                "package",
                "database",
                documents,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                resultBuilder,
                /*sendChangeNotifications=*/ false,
                fakeLogger,
                PersistType.Code.LITE,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Search document
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.query(
                "package", "database", "",
                new SearchSpec.Builder().build(), /*callerAccess=*/ null,
                /*queryStatsBuilder=*/ null, callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Report usage
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.reportUsage("package", "database", "namespace",
                "id", /*usageTimestampMillis=*/ 10, /*systemUsage=*/ false,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Remove document
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.remove("package", "database", "namespace",
                "id", /*removeStatsBuilder=*/ null, callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // RemoveByQuery
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.removeByQuery("package", "database", "",
                new SearchSpec.Builder().build(), /*deletedIds=*/null, /*removeStatsBuilder=*/ null,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineRequestBytes())
                .isGreaterThan(0);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Optimize
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);

        // Flush
        callStatsBuilder = new CallStats.Builder();
        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.FULL, /*logger=*/ null,
                callStatsBuilder);
        assertThat(callStatsBuilder.build().getIcingSearchEngineResponseBytes())
                .isGreaterThan(0);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMPRESSION_THRESHOLD)
    public void testCompressionThreshold() throws Exception {
        mAppSearchImpl.close();
        // Initialize AppSearch with a small compression threshold, which should force
        // compression for large documents.
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                createConfig(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig() {
                            @Override
                            public int getCompressionThresholdBytes() {
                                return 10;
                            }
                        }),
                AppSearchUserPlugins.EMPTY,
                ALWAYS_OPTIMIZE);

        // Set schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("Type")
                        .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("largeString")
                                .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                                .setTokenizerType(
                                        AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                                .build())
                        .build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                "package",
                "database",
                schemas,
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* callingUid= */ InternalVisibilityConfig.INVALID_UID,
                /* setSchemaStatsBuilder= */ null,
                /*callStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Add a large document
        GenericDocument largeDoc = new GenericDocument.Builder<>("namespace", "id1", "Type")
                .setPropertyString(
                        "largeString",
                        // A string of 10000 'A' characters.
                        new String(new char[10000]).replace('\0', 'A')
                )
                .build();
        mAppSearchImpl.putDocument(
                "package",
                "database",
                largeDoc,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);
        mAppSearchImpl.persistToDisk("package", BaseStats.CALL_TYPE_PUT_DOCUMENT,
                PersistType.Code.LITE, /*logger=*/ null,
                /*callStatsBuilder=*/ null);

        // Record storage size (the document should be compressed)
        StorageInfoProto storageInfo = mAppSearchImpl.getRawStorageInfoProto(
                /*callStatsBuilder=*/ null);
        long compressedSize = storageInfo.getDocumentStorageInfo().getDocumentLogSize();
        assertThat(compressedSize).isGreaterThan(0);

        // Close and re-open AppSearchImpl with a larger threshold, which should disable
        // compression for the document.
        mAppSearchImpl.close();
        AppSearchConfig configLargeThreshold = createConfig(
                new UnlimitedLimitConfig(),
                new LocalStorageIcingOptionsConfig() {
                    @Override
                    public int getCompressionThresholdBytes() {
                        return 100000;
                    }
                });
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                configLargeThreshold,
                AppSearchUserPlugins.EMPTY,
                ALWAYS_OPTIMIZE);

        // Run optimize, and test that the document is decompressed based on the new threshold.
        mAppSearchImpl.optimize(/*optimizeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        // Record storage size again (should be uncompressed)
        storageInfo = mAppSearchImpl.getRawStorageInfoProto(
                /*callStatsBuilder=*/ null);
        long uncompressedSize = storageInfo.getDocumentStorageInfo().getDocumentLogSize();
        assertThat(uncompressedSize).isGreaterThan(0);

        // Check that previous size (compressed) is smaller than the latter (uncompressed)
        assertThat(compressedSize).isLessThan(uncompressedSize);
    }
}
