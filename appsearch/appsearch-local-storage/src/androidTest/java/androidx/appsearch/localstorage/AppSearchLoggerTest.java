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

package androidx.appsearch.localstorage;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.testutil.SimpleTestLogger;

import com.google.android.icing.proto.DeleteStatsProto;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.OptimizeStatsProto;
import com.google.android.icing.proto.PutDocumentStatsProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.QueryStatsProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SetSchemaResultProto;
import com.google.android.icing.proto.StatusProto;
import com.google.android.icing.proto.TermMatchType;
import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AppSearchLoggerTest {
    private static final String PACKAGE_NAME = "packageName";
    private static final String DATABASE = "database";
    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private AppSearchImpl mAppSearchImpl;
    private SimpleTestLogger mLogger;

    @Before
    public void setUp() throws Exception {
        mAppSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        mLogger = new SimpleTestLogger();
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_initialize() {
        int nativeLatencyMillis = 3;
        int nativeDocumentStoreRecoveryCause = InitializeStatsProto.RecoveryCause.DATA_LOSS_VALUE;
        int nativeIndexRestorationCause =
                InitializeStatsProto.RecoveryCause.INCONSISTENT_WITH_GROUND_TRUTH_VALUE;
        int nativeSchemaStoreRecoveryCause =
                InitializeStatsProto.RecoveryCause.SCHEMA_CHANGES_OUT_OF_SYNC_VALUE;
        int nativeDocumentStoreRecoveryLatencyMillis = 7;
        int nativeIndexRestorationLatencyMillis = 8;
        int nativeSchemaStoreRecoveryLatencyMillis = 9;
        int nativeDocumentStoreDataStatus =
                InitializeStatsProto.DocumentStoreDataStatus.NO_DATA_LOSS_VALUE;
        int nativeNumDocuments = 11;
        int nativeNumSchemaTypes = 12;
        InitializeStatsProto.Builder nativeInitBuilder = InitializeStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDocumentStoreRecoveryCause(InitializeStatsProto.RecoveryCause.forNumber(
                        nativeDocumentStoreRecoveryCause))
                .setIndexRestorationCause(
                        InitializeStatsProto.RecoveryCause.forNumber(nativeIndexRestorationCause))
                .setSchemaStoreRecoveryCause(
                        InitializeStatsProto.RecoveryCause.forNumber(
                                nativeSchemaStoreRecoveryCause))
                .setDocumentStoreRecoveryLatencyMs(nativeDocumentStoreRecoveryLatencyMillis)
                .setIndexRestorationLatencyMs(nativeIndexRestorationLatencyMillis)
                .setSchemaStoreRecoveryLatencyMs(nativeSchemaStoreRecoveryLatencyMillis)
                .setDocumentStoreDataStatus(InitializeStatsProto.DocumentStoreDataStatus.forNumber(
                        nativeDocumentStoreDataStatus))
                .setNumDocuments(nativeNumDocuments)
                .setNumSchemaTypes(nativeNumSchemaTypes);
        InitializeStats.Builder initBuilder = new InitializeStats.Builder();

        AppSearchLoggerHelper.copyNativeStats(nativeInitBuilder.build(), initBuilder);

        InitializeStats iStats = initBuilder.build();
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(iStats.getDocumentStoreRecoveryCause()).isEqualTo(
                nativeDocumentStoreRecoveryCause);
        assertThat(iStats.getIndexRestorationCause()).isEqualTo(nativeIndexRestorationCause);
        assertThat(iStats.getSchemaStoreRecoveryCause()).isEqualTo(
                nativeSchemaStoreRecoveryCause);
        assertThat(iStats.getDocumentStoreRecoveryLatencyMillis()).isEqualTo(
                nativeDocumentStoreRecoveryLatencyMillis);
        assertThat(iStats.getIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(iStats.getSchemaStoreRecoveryLatencyMillis()).isEqualTo(
                nativeSchemaStoreRecoveryLatencyMillis);
        assertThat(iStats.getDocumentStoreDataStatus()).isEqualTo(
                nativeDocumentStoreDataStatus);
        assertThat(iStats.getDocumentCount()).isEqualTo(nativeNumDocuments);
        assertThat(iStats.getSchemaTypeCount()).isEqualTo(nativeNumSchemaTypes);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_putDocument() {
        final int nativeLatencyMillis = 3;
        final int nativeDocumentStoreLatencyMillis = 4;
        final int nativeIndexLatencyMillis = 5;
        final int nativeIndexMergeLatencyMillis = 6;
        final int nativeDocumentSize = 7;
        final int nativeNumTokensIndexed = 8;
        final boolean nativeExceededMaxNumTokens = true;
        PutDocumentStatsProto nativePutDocumentStats = PutDocumentStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDocumentStoreLatencyMs(nativeDocumentStoreLatencyMillis)
                .setIndexLatencyMs(nativeIndexLatencyMillis)
                .setIndexMergeLatencyMs(nativeIndexMergeLatencyMillis)
                .setDocumentSize(nativeDocumentSize)
                .setTokenizationStats(PutDocumentStatsProto.TokenizationStats.newBuilder()
                        .setNumTokensIndexed(nativeNumTokensIndexed)
                        .build())
                .build();
        PutDocumentStats.Builder pBuilder = new PutDocumentStats.Builder(PACKAGE_NAME, DATABASE);

        AppSearchLoggerHelper.copyNativeStats(nativePutDocumentStats, pBuilder);

        PutDocumentStats pStats = pBuilder.build();
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getNativeDocumentStoreLatencyMillis()).isEqualTo(
                nativeDocumentStoreLatencyMillis);
        assertThat(pStats.getNativeIndexLatencyMillis()).isEqualTo(nativeIndexLatencyMillis);
        assertThat(pStats.getNativeIndexMergeLatencyMillis()).isEqualTo(
                nativeIndexMergeLatencyMillis);
        assertThat(pStats.getNativeDocumentSizeBytes()).isEqualTo(nativeDocumentSize);
        assertThat(pStats.getNativeNumTokensIndexed()).isEqualTo(nativeNumTokensIndexed);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_search() {
        int nativeLatencyMillis = 4;
        int nativeNumTerms = 5;
        int nativeQueryLength = 6;
        int nativeNumNamespacesFiltered = 7;
        int nativeNumSchemaTypesFiltered = 8;
        int nativeRequestedPageSize = 9;
        int nativeNumResultsReturnedCurrentPage = 10;
        boolean nativeIsFirstPage = true;
        int nativeParseQueryLatencyMillis = 11;
        int nativeRankingStrategy = ScoringSpecProto.RankingStrategy.Code.CREATION_TIMESTAMP_VALUE;
        int nativeNumDocumentsScored = 13;
        int nativeScoringLatencyMillis = 14;
        int nativeRankingLatencyMillis = 15;
        int nativeNumResultsWithSnippets = 16;
        int nativeDocumentRetrievingLatencyMillis = 17;
        int nativeLockAcquisitionLatencyMillis = 18;
        int javaToNativeJniLatencyMillis = 19;
        int nativeToJavaJniLatencyMillis = 20;
        QueryStatsProto nativeQueryStats = QueryStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setNumTerms(nativeNumTerms)
                .setQueryLength(nativeQueryLength)
                .setNumNamespacesFiltered(nativeNumNamespacesFiltered)
                .setNumSchemaTypesFiltered(nativeNumSchemaTypesFiltered)
                .setRequestedPageSize(nativeRequestedPageSize)
                .setNumResultsReturnedCurrentPage(nativeNumResultsReturnedCurrentPage)
                .setIsFirstPage(nativeIsFirstPage)
                .setParseQueryLatencyMs(nativeParseQueryLatencyMillis)
                .setRankingStrategy(
                        ScoringSpecProto.RankingStrategy.Code.forNumber(nativeRankingStrategy))
                .setNumDocumentsScored(nativeNumDocumentsScored)
                .setScoringLatencyMs(nativeScoringLatencyMillis)
                .setRankingLatencyMs(nativeRankingLatencyMillis)
                .setNumResultsWithSnippets(nativeNumResultsWithSnippets)
                .setDocumentRetrievalLatencyMs(nativeDocumentRetrievingLatencyMillis)
                .setLockAcquisitionLatencyMs(nativeLockAcquisitionLatencyMillis)
                .setJavaToNativeJniLatencyMs(javaToNativeJniLatencyMillis)
                .setNativeToJavaJniLatencyMs(nativeToJavaJniLatencyMillis)
                .build();
        SearchStats.Builder qBuilder = new SearchStats.Builder(SearchStats.VISIBILITY_SCOPE_LOCAL,
                PACKAGE_NAME).setDatabase(DATABASE);

        AppSearchLoggerHelper.copyNativeStats(nativeQueryStats, qBuilder);

        SearchStats sStats = qBuilder.build();
        assertThat(sStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(sStats.getTermCount()).isEqualTo(nativeNumTerms);
        assertThat(sStats.getQueryLength()).isEqualTo(nativeQueryLength);
        assertThat(sStats.getFilteredNamespaceCount()).isEqualTo(nativeNumNamespacesFiltered);
        assertThat(sStats.getFilteredSchemaTypeCount()).isEqualTo(
                nativeNumSchemaTypesFiltered);
        assertThat(sStats.getRequestedPageSize()).isEqualTo(nativeRequestedPageSize);
        assertThat(sStats.getCurrentPageReturnedResultCount()).isEqualTo(
                nativeNumResultsReturnedCurrentPage);
        assertThat(sStats.isFirstPage()).isTrue();
        assertThat(sStats.getParseQueryLatencyMillis()).isEqualTo(
                nativeParseQueryLatencyMillis);
        assertThat(sStats.getRankingStrategy()).isEqualTo(nativeRankingStrategy);
        assertThat(sStats.getScoredDocumentCount()).isEqualTo(nativeNumDocumentsScored);
        assertThat(sStats.getScoringLatencyMillis()).isEqualTo(nativeScoringLatencyMillis);
        assertThat(sStats.getRankingLatencyMillis()).isEqualTo(nativeRankingLatencyMillis);
        assertThat(sStats.getResultWithSnippetsCount()).isEqualTo(nativeNumResultsWithSnippets);
        assertThat(sStats.getDocumentRetrievingLatencyMillis()).isEqualTo(
                nativeDocumentRetrievingLatencyMillis);
        assertThat(sStats.getNativeLockAcquisitionLatencyMillis()).isEqualTo(
                nativeLockAcquisitionLatencyMillis);
        assertThat(sStats.getJavaToNativeJniLatencyMillis()).isEqualTo(
                javaToNativeJniLatencyMillis);
        assertThat(sStats.getNativeToJavaJniLatencyMillis()).isEqualTo(
                nativeToJavaJniLatencyMillis);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_remove() {
        final int nativeLatencyMillis = 1;
        final int nativeDeleteType = 2;
        final int nativeNumDocumentDeleted = 3;
        DeleteStatsProto nativeDeleteStatsProto = DeleteStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDeleteType(DeleteStatsProto.DeleteType.Code.forNumber(nativeDeleteType))
                .setNumDocumentsDeleted(nativeNumDocumentDeleted)
                .build();
        RemoveStats.Builder rBuilder = new RemoveStats.Builder(
                "packageName",
                "database");

        AppSearchLoggerHelper.copyNativeStats(nativeDeleteStatsProto, rBuilder);

        RemoveStats rStats = rBuilder.build();
        assertThat(rStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(rStats.getDeleteType()).isEqualTo(nativeDeleteType);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(nativeNumDocumentDeleted);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_optimize() {
        int nativeLatencyMillis = 1;
        int nativeDocumentStoreOptimizeLatencyMillis = 2;
        int nativeIndexRestorationLatencyMillis = 3;
        int nativeNumOriginalDocuments = 4;
        int nativeNumDeletedDocuments = 5;
        int nativeNumExpiredDocuments = 6;
        long nativeStorageSizeBeforeBytes = Integer.MAX_VALUE + 1;
        long nativeStorageSizeAfterBytes = Integer.MAX_VALUE + 2;
        long nativeTimeSinceLastOptimizeMillis = Integer.MAX_VALUE + 3;
        OptimizeStatsProto optimizeStatsProto = OptimizeStatsProto.newBuilder()
                .setLatencyMs(nativeLatencyMillis)
                .setDocumentStoreOptimizeLatencyMs(nativeDocumentStoreOptimizeLatencyMillis)
                .setIndexRestorationLatencyMs(nativeIndexRestorationLatencyMillis)
                .setNumOriginalDocuments(nativeNumOriginalDocuments)
                .setNumDeletedDocuments(nativeNumDeletedDocuments)
                .setNumExpiredDocuments(nativeNumExpiredDocuments)
                .setStorageSizeBefore(nativeStorageSizeBeforeBytes)
                .setStorageSizeAfter(nativeStorageSizeAfterBytes)
                .setTimeSinceLastOptimizeMs(nativeTimeSinceLastOptimizeMillis)
                .build();
        OptimizeStats.Builder oBuilder = new OptimizeStats.Builder();

        AppSearchLoggerHelper.copyNativeStats(optimizeStatsProto, oBuilder);

        OptimizeStats oStats = oBuilder.build();
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(oStats.getDocumentStoreOptimizeLatencyMillis()).isEqualTo(
                nativeDocumentStoreOptimizeLatencyMillis);
        assertThat(oStats.getIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(oStats.getOriginalDocumentCount()).isEqualTo(nativeNumOriginalDocuments);
        assertThat(oStats.getDeletedDocumentCount()).isEqualTo(nativeNumDeletedDocuments);
        assertThat(oStats.getExpiredDocumentCount()).isEqualTo(nativeNumExpiredDocuments);
        assertThat(oStats.getStorageSizeBeforeBytes()).isEqualTo(nativeStorageSizeBeforeBytes);
        assertThat(oStats.getStorageSizeAfterBytes()).isEqualTo(nativeStorageSizeAfterBytes);
        assertThat(oStats.getTimeSinceLastOptimizeMillis()).isEqualTo(
                nativeTimeSinceLastOptimizeMillis);
    }

    @Test
    public void testAppSearchLoggerHelper_testCopyNativeStats_setSchema() {
        ImmutableList<String> newSchemaTypeChangeList = ImmutableList.of("new1");
        ImmutableList<String> deletedSchemaTypesList = ImmutableList.of("deleted1", "deleted2");
        ImmutableList<String> compatibleTypesList = ImmutableList.of("compatible1", "compatible2");
        ImmutableList<String> indexIncompatibleTypeChangeList = ImmutableList.of("index1");
        ImmutableList<String> backwardsIncompatibleTypeChangeList = ImmutableList.of("backwards1");
        SetSchemaResultProto setSchemaResultProto = SetSchemaResultProto.newBuilder()
                .addAllNewSchemaTypes(newSchemaTypeChangeList)
                .addAllDeletedSchemaTypes(deletedSchemaTypesList)
                .addAllFullyCompatibleChangedSchemaTypes(compatibleTypesList)
                .addAllIndexIncompatibleChangedSchemaTypes(indexIncompatibleTypeChangeList)
                .addAllIncompatibleSchemaTypes(backwardsIncompatibleTypeChangeList)
                .build();
        SetSchemaStats.Builder sBuilder = new SetSchemaStats.Builder(PACKAGE_NAME, DATABASE);

        AppSearchLoggerHelper.copyNativeStats(setSchemaResultProto, sBuilder);

        SetSchemaStats sStats = sBuilder.build();
        assertThat(sStats.getNewTypeCount()).isEqualTo(newSchemaTypeChangeList.size());
        assertThat(sStats.getDeletedTypeCount()).isEqualTo(deletedSchemaTypesList.size());
        assertThat(sStats.getCompatibleTypeChangeCount()).isEqualTo(compatibleTypesList.size());
        assertThat(sStats.getIndexIncompatibleTypeChangeCount()).isEqualTo(
                indexIncompatibleTypeChangeList.size());
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(
                backwardsIncompatibleTypeChangeList.size());
    }

    //
    // Testing actual logging
    //
    @Test
    public void testLoggingStats_initializeWithoutDocuments_success() throws Exception {
        // Create an unused AppSearchImpl to generated an InitializeStats.
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        AppSearchImpl appSearchImpl = AppSearchImpl.create(
                mTemporaryFolder.newFolder(),
                new UnlimitedLimitConfig(),
                initStatsBuilder,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        InitializeStats iStats = initStatsBuilder.build();
        appSearchImpl.close();

        assertThat(iStats).isNotNull();
        // If the process goes really fast, the total latency could be 0. Since the default of total
        // latency is also 0, we just remove the assert about NativeLatencyMillis.
        assertThat(iStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // Total latency captured in LocalStorage
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(0);
        assertThat(iStats.hasDeSync()).isFalse();
        assertThat(iStats.getDocumentStoreDataStatus()).isEqualTo(
                InitializeStatsProto.DocumentStoreDataStatus.NO_DATA_LOSS_VALUE);
        assertThat(iStats.getDocumentCount()).isEqualTo(0);
        assertThat(iStats.getSchemaTypeCount()).isEqualTo(0);
        assertThat(iStats.hasReset()).isEqualTo(false);
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
    }

    @Test
    public void testLoggingStats_initializeWithDocuments_success() throws Exception {
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        final File folder = mTemporaryFolder.newFolder();

        AppSearchImpl appSearchImpl = AppSearchImpl.create(
                folder,
                new UnlimitedLimitConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Type1").build();
        GenericDocument doc2 =
                new GenericDocument.Builder<>("namespace", "id2", "Type1").build();
        appSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                doc1,
                /*sendChangeNotifications=*/ false,
                mLogger);
        appSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                doc2,
                /*sendChangeNotifications=*/ false,
                mLogger);
        appSearchImpl.close();

        // Create another appsearchImpl on the same folder
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        appSearchImpl = AppSearchImpl.create(
                folder, new UnlimitedLimitConfig(), initStatsBuilder, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        InitializeStats iStats = initStatsBuilder.build();

        assertThat(iStats).isNotNull();
        // If the process goes really fast, the total latency could be 0. Since the default of total
        // latency is also 0, we just remove the assert about NativeLatencyMillis.
        assertThat(iStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // Total latency captured in LocalStorage
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(0);
        assertThat(iStats.hasDeSync()).isFalse();
        assertThat(iStats.getDocumentStoreDataStatus()).isEqualTo(
                InitializeStatsProto.DocumentStoreDataStatus.NO_DATA_LOSS_VALUE);
        assertThat(iStats.getDocumentCount()).isEqualTo(2);
        assertThat(iStats.getSchemaTypeCount()).isEqualTo(4); // +2 for VisibilitySchema
        assertThat(iStats.hasReset()).isEqualTo(false);
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        appSearchImpl.close();
    }

    @Test
    public void testLoggingStats_initialize_failure() throws Exception {
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        final File folder = mTemporaryFolder.newFolder();

        AppSearchImpl appSearchImpl = AppSearchImpl.create(
                folder, new UnlimitedLimitConfig(), /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Insert a valid doc
        GenericDocument doc1 =
                new GenericDocument.Builder<>("namespace", "id1", "Type1").build();
        appSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                doc1,
                /*sendChangeNotifications=*/ false,
                mLogger);

        // Insert the invalid doc with an invalid namespace right into icing
        DocumentProto invalidDoc = DocumentProto.newBuilder()
                .setNamespace("invalidNamespace")
                .setUri("id2")
                .setSchema(String.format("%s$%s/Type1", testPackageName, testDatabase))
                .build();
        PutResultProto putResultProto = appSearchImpl.mIcingSearchEngineLocked.put(invalidDoc);
        assertThat(putResultProto.getStatus().getCode()).isEqualTo(StatusProto.Code.OK);
        appSearchImpl.close();

        // Create another appsearchImpl on the same folder
        InitializeStats.Builder initStatsBuilder = new InitializeStats.Builder();
        appSearchImpl = AppSearchImpl.create(
                folder, new UnlimitedLimitConfig(), initStatsBuilder, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        InitializeStats iStats = initStatsBuilder.build();

        // Some of other fields are already covered by AppSearchImplTest#testReset()
        assertThat(iStats).isNotNull();
        assertThat(iStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_INTERNAL_ERROR);
        assertThat(iStats.hasReset()).isTrue();
        appSearchImpl.close();
    }

    @Test
    public void testLoggingStats_putDocument_success() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        AppSearchSchema testSchema = new AppSearchSchema.Builder("type")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        List<AppSearchSchema> schemas = Collections.singletonList(testSchema);
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "type")
                        .setPropertyString("subject", "testPut example1")
                        .build();

        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document,
                /*sendChangeNotifications=*/ false,
                mLogger);

        PutDocumentStats pStats = mLogger.mPutDocumentStats;
        assertThat(pStats).isNotNull();
        assertThat(pStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(pStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(pStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // The latency related native stats have been tested in testCopyNativeStats
        assertThat(pStats.getNativeDocumentSizeBytes()).isGreaterThan(0);
        assertThat(pStats.getNativeNumTokensIndexed()).isGreaterThan(0);
    }

    @Test
    public void testLoggingStats_putDocument_failure() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        AppSearchSchema testSchema = new AppSearchSchema.Builder("type")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        List<AppSearchSchema> schemas = Collections.singletonList(testSchema);
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        GenericDocument document =
                new GenericDocument.Builder<>("namespace", "id", "type")
                        .setPropertyString("nonExist", "testPut example1")
                        .build();

        AppSearchException exception = Assert.assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.putDocument(
                        testPackageName,
                        testDatabase,
                        document,
                        /*sendChangeNotifications=*/ false,
                        mLogger));
        assertThat(exception.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        PutDocumentStats pStats = mLogger.mPutDocumentStats;
        assertThat(pStats).isNotNull();
        assertThat(pStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(pStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(pStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
    }

    @Test
    public void testLoggingStats_search_success() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        AppSearchSchema testSchema = new AppSearchSchema.Builder("type")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        List<AppSearchSchema> schemas = Collections.singletonList(testSchema);
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument document1 =
                new GenericDocument.Builder<>("namespace", "id1", "type")
                        .setPropertyString("subject", "testPut example1")
                        .build();
        GenericDocument document2 =
                new GenericDocument.Builder<>("namespace", "id2", "type")
                        .setPropertyString("subject", "testPut example2")
                        .build();
        GenericDocument document3 =
                new GenericDocument.Builder<>("namespace", "id3", "type")
                        .setPropertyString("subject", "testPut 3")
                        .build();
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document1,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document2,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document3,
                /*sendChangeNotifications=*/ false,
                mLogger);


        // No query filters specified. package2 should only get its own documents back.
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                        .build();
        String queryStr = "testPut e";
        SearchResultPage searchResultPage = mAppSearchImpl.query(testPackageName, testDatabase,
                queryStr, searchSpec, /*logger=*/ mLogger);

        assertThat(searchResultPage.getResults()).hasSize(2);
        // The ranking strategy is LIFO
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(document2);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(document1);

        SearchStats sStats = mLogger.mSearchStats;

        assertThat(sStats).isNotNull();
        // If the process goes really fast, the total latency could be 0. Since the default of total
        // latency is also 0, we just remove the assert about TotalLatencyMillis.
        assertThat(sStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(sStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(sStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(sStats.getVisibilityScope()).isEqualTo(SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(sStats.getTermCount()).isEqualTo(2);
        assertThat(sStats.getQueryLength()).isEqualTo(queryStr.length());
        assertThat(sStats.getFilteredNamespaceCount()).isEqualTo(1);
        assertThat(sStats.getFilteredSchemaTypeCount()).isEqualTo(1);
        assertThat(sStats.getCurrentPageReturnedResultCount()).isEqualTo(2);
        assertThat(sStats.isFirstPage()).isTrue();
        assertThat(sStats.getRankingStrategy()).isEqualTo(
                SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP);
        assertThat(sStats.getScoredDocumentCount()).isEqualTo(2);
        assertThat(sStats.getResultWithSnippetsCount()).isEqualTo(0);
    }

    @Test
    public void testLoggingStats_search_failure() throws Exception {
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        List<AppSearchSchema> schemas = ImmutableList.of(
                new AppSearchSchema.Builder("Type1").build(),
                new AppSearchSchema.Builder("Type2").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE)
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_CREATION_TIMESTAMP)
                        .addFilterPackageNames("anotherPackage")
                        .build();

        mAppSearchImpl.query(testPackageName,
                testPackageName,
                /* queryExpression= */ "",
                searchSpec, /*logger=*/ mLogger);

        SearchStats sStats = mLogger.mSearchStats;
        assertThat(sStats).isNotNull();
        assertThat(sStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(sStats.getDatabase()).isEqualTo(testPackageName);
        assertThat(sStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_SECURITY_ERROR);
    }

    @Test
    public void testLoggingStats_search_join() throws Exception {
        AppSearchSchema actionSchema = new AppSearchSchema.Builder("ViewAction")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("entityId")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                        .setJoinableValueType(AppSearchSchema.StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        AppSearchSchema entitySchema = new AppSearchSchema.Builder("entity")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        List<AppSearchSchema> schemas = Arrays.asList(actionSchema, entitySchema);

        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);

        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument entity1 =
                new GenericDocument.Builder<>("namespace", "id1", "entity")
                        .setPropertyString("subject", "an entity")
                        .build();
        GenericDocument entity2 =
                new GenericDocument.Builder<>("namespace", "id2", "entity")
                        .setPropertyString("subject", "another entity")
                        .build();

        GenericDocument action1 =
                new GenericDocument.Builder<>("namespace", "action1", "ViewAction")
                        .setPropertyString("entityId",
                                "testPackage$testDatabase/namespace#id1")
                        .build();
        GenericDocument action2 =
                new GenericDocument.Builder<>("namespace", "action2", "ViewAction")
                        .setPropertyString("entityId",
                                "testPackage$testDatabase/namespace#id1")
                        .build();
        GenericDocument action3 =
                new GenericDocument.Builder<>("namespace", "action3", "ViewAction")
                        .setPropertyString("entityId",
                                "testPackage$testDatabase/namespace#id1")
                        .build();
        GenericDocument action4 =
                new GenericDocument.Builder<>("namespace", "action4", "ViewAction")
                        .setPropertyString("entityId",
                                "testPackage$testDatabase/namespace#id2")
                        .build();

        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                entity1,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                entity2,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                action1,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                action2,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                action3,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                action4,
                /*sendChangeNotifications=*/ false,
                mLogger);

        SearchSpec nestedSearchSpec =
                new SearchSpec.Builder()
                        .setRankingStrategy(SearchSpec.RANKING_STRATEGY_DOCUMENT_SCORE)
                        .setOrder(SearchSpec.ORDER_ASCENDING)
                        .build();

        JoinSpec js = new JoinSpec.Builder("entityId")
                .setNestedSearch("", nestedSearchSpec)
                .setAggregationScoringStrategy(JoinSpec.AGGREGATION_SCORING_RESULT_COUNT)
                .build();

        SearchSpec searchSpec = new SearchSpec.Builder()
                .setRankingStrategy(SearchSpec.RANKING_STRATEGY_JOIN_AGGREGATE_SCORE)
                .setJoinSpec(js)
                .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                .build();

        String queryStr = "entity";
        SearchResultPage searchResultPage = mAppSearchImpl.query(testPackageName, testDatabase,
                queryStr, searchSpec, /*logger=*/ mLogger);

        assertThat(searchResultPage.getResults()).hasSize(2);
        assertThat(searchResultPage.getResults().get(0).getGenericDocument()).isEqualTo(entity1);
        assertThat(searchResultPage.getResults().get(1).getGenericDocument()).isEqualTo(entity2);

        SearchStats sStats = mLogger.mSearchStats;

        assertThat(sStats).isNotNull();

        assertThat(sStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(sStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(sStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(sStats.getVisibilityScope()).isEqualTo(SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(sStats.getTermCount()).isEqualTo(1);
        assertThat(sStats.getQueryLength()).isEqualTo(queryStr.length());
        assertThat(sStats.getFilteredNamespaceCount()).isEqualTo(1);
        assertThat(sStats.getFilteredSchemaTypeCount()).isEqualTo(2);
        assertThat(sStats.getCurrentPageReturnedResultCount()).isEqualTo(2);
        assertThat(sStats.isFirstPage()).isTrue();
        assertThat(sStats.getRankingStrategy()).isEqualTo(
                ScoringSpecProto.RankingStrategy.Code.JOIN_AGGREGATE_SCORE_VALUE);
        assertThat(sStats.getScoredDocumentCount()).isEqualTo(2);
        assertThat(sStats.getResultWithSnippetsCount()).isEqualTo(0);
        // Join-specific stats. If the process goes really fast, the total latency could be 0.
        // Since the default of total latency is also 0, we just remove the assertion on
        // JoinLatencyMillis.
        assertThat(sStats.getJoinType()).isEqualTo(
                AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
        assertThat(sStats.getNumJoinedResultsCurrentPage()).isEqualTo(4);
    }

    @Test
    public void testLoggingStats_remove_success() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        final String testNamespace = "testNameSpace";
        final String testId = "id";
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument document =
                new GenericDocument.Builder<>(testNamespace, testId, "type").build();
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        RemoveStats.Builder rStatsBuilder = new RemoveStats.Builder(testPackageName, testDatabase);
        mAppSearchImpl.remove(testPackageName, testDatabase, testNamespace, testId, rStatsBuilder);
        RemoveStats rStats = rStatsBuilder.build();

        assertThat(rStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(rStats.getDatabase()).isEqualTo(testDatabase);
        // delete by namespace + id
        assertThat(rStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(rStats.getDeleteType()).isEqualTo(DeleteStatsProto.DeleteType.Code.SINGLE_VALUE);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(1);
    }

    @Test
    public void testLoggingStats_remove_failure() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        final String testNamespace = "testNameSpace";
        final String testId = "id";
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        GenericDocument document =
                new GenericDocument.Builder<>(testNamespace, testId, "type").build();
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document,
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null);

        RemoveStats.Builder rStatsBuilder = new RemoveStats.Builder(testPackageName, testDatabase);

        AppSearchException exception = Assert.assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.remove(testPackageName, testDatabase, testNamespace,
                        "invalidId", rStatsBuilder));
        assertThat(exception.getResultCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);

        RemoveStats rStats = rStatsBuilder.build();
        assertThat(rStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(rStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(rStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_NOT_FOUND);
        // delete by namespace + id
        assertThat(rStats.getDeleteType()).isEqualTo(DeleteStatsProto.DeleteType.Code.SINGLE_VALUE);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("deprecation") // DEPRECATED_QUERY_VALUE
    public void testLoggingStats_removeByQuery_success() throws Exception {
        // Insert schema
        final String testPackageName = "testPackage";
        final String testDatabase = "testDatabase";
        final String testNamespace = "testNameSpace";
        List<AppSearchSchema> schemas =
                Collections.singletonList(new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                testPackageName,
                testDatabase,
                schemas,
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        GenericDocument document1 =
                new GenericDocument.Builder<>(testNamespace, "id1", "type").build();
        GenericDocument document2 =
                new GenericDocument.Builder<>(testNamespace, "id2", "type").build();
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document1,
                /*sendChangeNotifications=*/ false,
                mLogger);
        mAppSearchImpl.putDocument(
                testPackageName,
                testDatabase,
                document2,
                /*sendChangeNotifications=*/ false,
                mLogger);
        // No query filters specified. package2 should only get its own documents back.
        SearchSpec searchSpec =
                new SearchSpec.Builder().setTermMatch(TermMatchType.Code.PREFIX_VALUE).build();

        RemoveStats.Builder rStatsBuilder = new RemoveStats.Builder(testPackageName, testDatabase);
        mAppSearchImpl.removeByQuery(testPackageName, testDatabase,
                /*queryExpression=*/"", searchSpec,
                rStatsBuilder);
        RemoveStats rStats = rStatsBuilder.build();

        assertThat(rStats.getPackageName()).isEqualTo(testPackageName);
        assertThat(rStats.getDatabase()).isEqualTo(testDatabase);
        assertThat(rStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        // delete by query
        assertThat(rStats.getDeleteType())
                .isEqualTo(DeleteStatsProto.DeleteType.Code.DEPRECATED_QUERY_VALUE);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(2);
    }

    @Test
    public void testLoggingStats_setSchema() throws Exception {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("testSchema")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .build();
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                PACKAGE_NAME,
                DATABASE,
                Collections.singletonList(schema1),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // create a backwards incompatible schema
        SetSchemaStats.Builder sStatsBuilder = new SetSchemaStats.Builder(PACKAGE_NAME, DATABASE);
        AppSearchSchema schema2 = new AppSearchSchema.Builder("testSchema").build();
        internalSetSchemaResponse = mAppSearchImpl.setSchema(
                PACKAGE_NAME,
                DATABASE,
                Collections.singletonList(schema2),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /* setSchemaStatsBuilder= */ sStatsBuilder);
        assertThat(internalSetSchemaResponse.isSuccess()).isFalse();

        SetSchemaStats sStats = sStatsBuilder.build();
        assertThat(sStats.getPackageName()).isEqualTo(PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(DATABASE);
        assertThat(sStats.getNewTypeCount()).isEqualTo(0);
        assertThat(sStats.getCompatibleTypeChangeCount()).isEqualTo(0);
        assertThat(sStats.getIndexIncompatibleTypeChangeCount()).isEqualTo(1);
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(1);
    }
}
