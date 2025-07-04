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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.SchemaMigrationStats;

import org.junit.Test;

public class AppSearchStatsTest {
    static final String TEST_PACKAGE_NAME = "com.google.test";
    static final String TEST_DATA_BASE = "testDataBase";
    static final int TEST_STATUS_CODE = AppSearchResult.RESULT_INTERNAL_ERROR;
    static final int TEST_TOTAL_LATENCY_MILLIS = 20;

    @Test
    public void testAppSearchStats_CallStats() {
        final int estimatedBinderLatencyMillis = 1;
        final int numOperationsSucceeded = 2;
        final int numOperationsFailed = 3;
        final @CallStats.CallType int callType =
                CallStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setCallType(callType)
                .setEstimatedBinderLatencyMillis(estimatedBinderLatencyMillis)
                .setNumOperationsSucceeded(numOperationsSucceeded)
                .setNumOperationsFailed(numOperationsFailed)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEstimatedBinderLatencyMillis())
                .isEqualTo(estimatedBinderLatencyMillis);
        assertThat(cStats.getCallType()).isEqualTo(callType);
        assertThat(cStats.getNumOperationsSucceeded()).isEqualTo(numOperationsSucceeded);
        assertThat(cStats.getNumOperationsFailed()).isEqualTo(numOperationsFailed);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(1);
    }

    @Test
    public void testAppSearchStats_setLaunchVMEnabled_false() {
        final CallStats cStats = new CallStats.Builder()
                .setPackageName(TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setLaunchVMEnabled(false)
                .build();

        assertThat(cStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(cStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(cStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(cStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(cStats.getEnabledFeatures()).isEqualTo(0);
    }

    @Test
    public void testAppSearchCallStats_nullValues() {
        final @CallStats.CallType int callType =
                CallStats.CALL_TYPE_PUT_DOCUMENTS;

        final CallStats.Builder cStatsBuilder = new CallStats.Builder()
                .setCallType(callType);

        final CallStats cStats = cStatsBuilder.build();

        assertThat(cStats.getPackageName()).isNull();
        assertThat(cStats.getDatabase()).isNull();
        assertThat(cStats.getCallType()).isEqualTo(callType);
    }

    @Test
    public void testAppSearchStats_PutDocumentStats() {
        final int generateDocumentProtoLatencyMillis = 1;
        final int rewriteDocumentTypesLatencyMillis = 2;
        final int nativeLatencyMillis = 3;
        final int nativeDocumentStoreLatencyMillis = 4;
        final int nativeIndexLatencyMillis = 5;
        final int nativeIndexMergeLatencyMillis = 6;
        final int nativeDocumentSize = 7;
        final int nativeNumTokensIndexed = 8;
        final boolean nativeExceededMaxNumTokens = true;
        final int nativeTermIndexLatencyMillis = 9;
        final int nativeIntegerIndexLatencyMillis = 10;
        final int nativeQualifiedIdJoinIndexLatencyMillis = 11;
        final int nativeLiteIndexSortLatencyMillis = 12;
        final int enabledFeatures = 1;
        int metadataTermIndexLatencyMillis = 13;
        int embeddingIndexLatencyMillis = 14;
        final PutDocumentStats.Builder pStatsBuilder =
                new PutDocumentStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                        .setStatusCode(TEST_STATUS_CODE)
                        .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                        .setGenerateDocumentProtoLatencyMillis(generateDocumentProtoLatencyMillis)
                        .setRewriteDocumentTypesLatencyMillis(rewriteDocumentTypesLatencyMillis)
                        .setNativeLatencyMillis(nativeLatencyMillis)
                        .setNativeDocumentStoreLatencyMillis(nativeDocumentStoreLatencyMillis)
                        .setNativeIndexLatencyMillis(nativeIndexLatencyMillis)
                        .setNativeIndexMergeLatencyMillis(nativeIndexMergeLatencyMillis)
                        .setNativeDocumentSizeBytes(nativeDocumentSize)
                        .setNativeNumTokensIndexed(nativeNumTokensIndexed)
                        .setNativeTermIndexLatencyMillis(nativeTermIndexLatencyMillis)
                        .setNativeIntegerIndexLatencyMillis(nativeIntegerIndexLatencyMillis)
                        .setNativeQualifiedIdJoinIndexLatencyMillis(
                                nativeQualifiedIdJoinIndexLatencyMillis)
                        .setNativeLiteIndexSortLatencyMillis(nativeLiteIndexSortLatencyMillis)
                        .setLaunchVMEnabled(true)
                        .setMetadataTermIndexLatencyMillis(metadataTermIndexLatencyMillis)
                        .setEmbeddingIndexLatencyMillis(embeddingIndexLatencyMillis);

        final PutDocumentStats pStats = pStatsBuilder.build();

        assertThat(pStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(pStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(pStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(pStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(pStats.getGenerateDocumentProtoLatencyMillis()).isEqualTo(
                generateDocumentProtoLatencyMillis);
        assertThat(pStats.getRewriteDocumentTypesLatencyMillis()).isEqualTo(
                rewriteDocumentTypesLatencyMillis);
        assertThat(pStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(pStats.getNativeDocumentStoreLatencyMillis()).isEqualTo(
                nativeDocumentStoreLatencyMillis);
        assertThat(pStats.getNativeIndexLatencyMillis()).isEqualTo(nativeIndexLatencyMillis);
        assertThat(pStats.getNativeIndexMergeLatencyMillis()).isEqualTo(
                nativeIndexMergeLatencyMillis);
        assertThat(pStats.getNativeDocumentSizeBytes()).isEqualTo(nativeDocumentSize);
        assertThat(pStats.getNativeNumTokensIndexed()).isEqualTo(nativeNumTokensIndexed);
        assertThat(pStats.getNativeTermIndexLatencyMillis()).isEqualTo(
                nativeTermIndexLatencyMillis);
        assertThat(pStats.getNativeIntegerIndexLatencyMillis()).isEqualTo(
                nativeIntegerIndexLatencyMillis);
        assertThat(pStats.getNativeQualifiedIdJoinIndexLatencyMillis()).isEqualTo(
                nativeQualifiedIdJoinIndexLatencyMillis);
        assertThat(pStats.getNativeLiteIndexSortLatencyMillis()).isEqualTo(
                nativeLiteIndexSortLatencyMillis);
        assertThat(pStats.getEnabledFeatures()).isEqualTo(
                enabledFeatures);
        assertThat(pStats.getMetadataTermIndexLatencyMillis()).isEqualTo(
                metadataTermIndexLatencyMillis);
        assertThat(pStats.getEmbeddingIndexLatencyMillis()).isEqualTo(
                embeddingIndexLatencyMillis);
    }

    @Test
    public void testAppSearchStats_InitializeStats() {
        int enabledFeatures = 1;
        int prepareSchemaAndNamespacesLatencyMillis = 1;
        int prepareVisibilityFileLatencyMillis = 2;
        int nativeLatencyMillis = 3;
        int nativeDocumentStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_DEPENDENCIES_CHANGED;
        int nativeIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_FEATURE_FLAG_CHANGED;
        int nativeSchemaStoreRecoveryCause = InitializeStats.RECOVERY_CAUSE_IO_ERROR;
        int nativeDocumentStoreRecoveryLatencyMillis = 4;
        int nativeIndexRestorationLatencyMillis = 5;
        int nativeSchemaStoreRecoveryLatencyMillis = 6;
        int nativeDocumentStoreDataStatus = 7;
        int nativeNumDocuments = 8;
        int nativeNumSchemaTypes = 9;
        int numPreviousInitFailures = 10;
        int integerIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int qualifiedIdJoinIndexRestorationCause =
                InitializeStats.RECOVERY_CAUSE_INCONSISTENT_WITH_GROUND_TRUTH;
        int embeddingIndexRestorationCause = InitializeStats.RECOVERY_CAUSE_DATA_LOSS;
        int initializeIcuDataStatusCode = 11;
        int numFailedReindexedDocuments = 12;

        final InitializeStats.Builder iStatsBuilder = new InitializeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setHasDeSync(/* hasDeSyncs= */ true)
                .setPrepareSchemaAndNamespacesLatencyMillis(prepareSchemaAndNamespacesLatencyMillis)
                .setPrepareVisibilityStoreLatencyMillis(prepareVisibilityFileLatencyMillis)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setNativeDocumentStoreRecoveryCause(nativeDocumentStoreRecoveryCause)
                .setNativeIndexRestorationCause(nativeIndexRestorationCause)
                .setNativeSchemaStoreRecoveryCause(nativeSchemaStoreRecoveryCause)
                .setNativeDocumentStoreRecoveryLatencyMillis(
                        nativeDocumentStoreRecoveryLatencyMillis)
                .setNativeIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setNativeSchemaStoreRecoveryLatencyMillis(nativeSchemaStoreRecoveryLatencyMillis)
                .setNativeDocumentStoreDataStatus(nativeDocumentStoreDataStatus)
                .setNativeDocumentCount(nativeNumDocuments)
                .setNativeSchemaTypeCount(nativeNumSchemaTypes)
                .setHasReset(true)
                .setResetStatusCode(AppSearchResult.RESULT_INVALID_SCHEMA)
                .setLaunchVMEnabled(true)
                .setNativeNumPreviousInitFailures(numPreviousInitFailures)
                .setNativeIntegerIndexRestorationCause(integerIndexRestorationCause)
                .setNativeQualifiedIdJoinIndexRestorationCause(qualifiedIdJoinIndexRestorationCause)
                .setNativeEmbeddingIndexRestorationCause(embeddingIndexRestorationCause)
                .setNativeInitializeIcuDataStatusCode(initializeIcuDataStatusCode)
                .setNativeNumFailedReindexedDocuments(numFailedReindexedDocuments);
        final InitializeStats iStats = iStatsBuilder.build();

        assertThat(iStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(iStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(iStats.hasDeSync()).isTrue();
        assertThat(iStats.getPrepareSchemaAndNamespacesLatencyMillis()).isEqualTo(
                prepareSchemaAndNamespacesLatencyMillis);
        assertThat(iStats.getPrepareVisibilityStoreLatencyMillis()).isEqualTo(
                prepareVisibilityFileLatencyMillis);
        assertThat(iStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreRecoveryCause()).isEqualTo(
                nativeDocumentStoreRecoveryCause);
        assertThat(iStats.getNativeIndexRestorationCause()).isEqualTo(nativeIndexRestorationCause);
        assertThat(iStats.getNativeSchemaStoreRecoveryCause()).isEqualTo(
                nativeSchemaStoreRecoveryCause);
        assertThat(iStats.getNativeDocumentStoreRecoveryLatencyMillis()).isEqualTo(
                nativeDocumentStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeIndexRestorationLatencyMillis()).isEqualTo(
                nativeIndexRestorationLatencyMillis);
        assertThat(iStats.getNativeSchemaStoreRecoveryLatencyMillis()).isEqualTo(
                nativeSchemaStoreRecoveryLatencyMillis);
        assertThat(iStats.getNativeDocumentStoreDataStatus()).isEqualTo(
                nativeDocumentStoreDataStatus);
        assertThat(iStats.getNativeDocumentCount()).isEqualTo(nativeNumDocuments);
        assertThat(iStats.getNativeSchemaTypeCount()).isEqualTo(nativeNumSchemaTypes);
        assertThat(iStats.hasReset()).isTrue();
        assertThat(iStats.getResetStatusCode()).isEqualTo(AppSearchResult.RESULT_INVALID_SCHEMA);
        assertThat(iStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(iStats.getNativeNumPreviousInitFailures()).isEqualTo(numPreviousInitFailures);
        assertThat(iStats.getNativeIntegerIndexRestorationCause())
                .isEqualTo(integerIndexRestorationCause);
        assertThat(iStats.getNativeQualifiedIdJoinIndexRestorationCause())
                .isEqualTo(qualifiedIdJoinIndexRestorationCause);
        assertThat(iStats.getNativeEmbeddingIndexRestorationCause())
                .isEqualTo(embeddingIndexRestorationCause);
        assertThat(iStats.getNativeInitializeIcuDataStatusCode())
                .isEqualTo(initializeIcuDataStatusCode);
        assertThat(iStats.getNativeNumFailedReindexedDocuments())
                .isEqualTo(numFailedReindexedDocuments);
    }

    @Test
    public void testAppSearchStats_SearchStats() {
        int nativeQueryLength = 1;
        int nativeNumTerms = 2;
        int nativeNumNamespacesFiltered = 3;
        int nativeNumSchemaTypesFiltered = 4;
        int nativeRankingStrategy = 5;
        int nativeNumDocumentsScored = 6;
        int nativeParseQueryLatencyMillis = 7;
        int nativeScoringLatencyMillis = 8;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 9;
        int numFetchedHitsMainIndex = 10;
        int numFetchedHitsIntegerIndex = 11;
        int queryProcessorLexerExtractTokenLatencyMillis = 12;
        int queryProcessorParserConsumeQueryLatencyMillis = 13;
        int queryProcessorQueryVisitorLatencyMillis = 14;

        final SearchStats.Builder sStatsBuilder = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis);
        final SearchStats sStats = sStatsBuilder.build();

        assertThat(sStats.getNativeQueryLength()).isEqualTo(nativeQueryLength);
        assertThat(sStats.getNativeTermCount()).isEqualTo(nativeNumTerms);
        assertThat(sStats.getNativeFilteredNamespaceCount()).isEqualTo(nativeNumNamespacesFiltered);
        assertThat(sStats.getNativeFilteredSchemaTypeCount()).isEqualTo(
                nativeNumSchemaTypesFiltered);
        assertThat(sStats.getNativeRankingStrategy()).isEqualTo(nativeRankingStrategy);
        assertThat(sStats.getNativeParseQueryLatencyMillis()).isEqualTo(
                nativeParseQueryLatencyMillis);
        assertThat(sStats.getNativeScoredDocumentCount()).isEqualTo(nativeNumDocumentsScored);
        assertThat(sStats.getNativeScoringLatencyMillis()).isEqualTo(nativeScoringLatencyMillis);
        assertThat(sStats.isNativeNumericQuery()).isEqualTo(isNumericQuery);
        assertThat(sStats.getNativeNumFetchedHitsLiteIndex()).isEqualTo(numFetchedHitsLiteIndex);
        assertThat(sStats.getNativeNumFetchedHitsMainIndex()).isEqualTo(numFetchedHitsMainIndex);
        assertThat(sStats.getNativeNumFetchedHitsIntegerIndex())
                .isEqualTo(numFetchedHitsIntegerIndex);
        assertThat(sStats.getNativeQueryProcessorLexerExtractTokenLatencyMillis())
                .isEqualTo(queryProcessorLexerExtractTokenLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorParserConsumeQueryLatencyMillis())
                .isEqualTo(queryProcessorParserConsumeQueryLatencyMillis);
        assertThat(sStats.getNativeQueryProcessorQueryVisitorLatencyMillis())
                .isEqualTo(queryProcessorQueryVisitorLatencyMillis);
        String expectedString = "SearchStats {\n"
                + "query_length=1, num_terms=2, num_namespaces_filtered=3, "
                + "num_schema_types_filtered=4,\n"
                + "ranking_strategy=5, num_docs_scored=6, parse_query_latency=7, "
                + "scoring_latency=8, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=9, num_fetched_hits_main_index=10, "
                + "num_fetched_hits_integer_index=11,\n"
                + "query_processor_lexer_extract_token_latency=12, "
                + "query_processor_parser_consume_query_latency=13,\n"
                + "query_processor_query_visitor_latency=14}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_QueryStats() {
        int nativeQueryLength = 101;
        int nativeNumTerms = 102;
        int nativeNumNamespacesFiltered = 103;
        int nativeNumSchemaTypesFiltered = 104;
        int nativeRankingStrategy = 105;
        int nativeNumDocumentsScored = 106;
        int nativeParseQueryLatencyMillis = 107;
        int nativeScoringLatencyMillis = 108;
        boolean isNumericQuery = true;
        int numFetchedHitsLiteIndex = 109;
        int numFetchedHitsMainIndex = 110;
        int numFetchedHitsIntegerIndex = 111;
        int queryProcessorLexerExtractTokenLatencyMillis = 112;
        int queryProcessorParserConsumeQueryLatencyMillis = 113;
        int queryProcessorQueryVisitorLatencyMillis = 114;

        SearchStats searchStats = new SearchStats.Builder()
                .setNativeQueryLength(nativeQueryLength)
                .setNativeTermCount(nativeNumTerms)
                .setNativeFilteredNamespaceCount(nativeNumNamespacesFiltered)
                .setNativeFilteredSchemaTypeCount(nativeNumSchemaTypesFiltered)
                .setNativeParseQueryLatencyMillis(nativeParseQueryLatencyMillis)
                .setNativeRankingStrategy(nativeRankingStrategy)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        nativeParseQueryLatencyMillis)
                .setNativeScoredDocumentCount(nativeNumDocumentsScored)
                .setNativeScoringLatencyMillis(nativeScoringLatencyMillis)
                .setNativeIsNumericQuery(isNumericQuery)
                .setNativeNumFetchedHitsLiteIndex(numFetchedHitsLiteIndex)
                .setNativeNumFetchedHitsMainIndex(numFetchedHitsMainIndex)
                .setNativeNumFetchedHitsIntegerIndex(numFetchedHitsIntegerIndex)
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        queryProcessorLexerExtractTokenLatencyMillis)
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        queryProcessorParserConsumeQueryLatencyMillis)
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        queryProcessorQueryVisitorLatencyMillis).build();

        int enabledFeatures = 1;
        int rewriteSearchSpecLatencyMillis = 202;
        int rewriteSearchResultLatencyMillis = 203;
        int javaLockAcquisitionLatencyMillis = 204;
        int aclCheckLatencyMillis = 205;
        int visibilityScope = QueryStats.VISIBILITY_SCOPE_LOCAL;
        String searchSourceLogTag = "tag";
        boolean nativeIsFirstPage = true;
        int nativeRequestedPageSize = 206;
        int nativeNumResultsReturnedCurrentPage = 207;
        int nativeLatencyMillis = 208;
        int nativeRankingLatencyMillis = 209;
        int nativeDocumentRetrievingLatencyMillis = 210;
        int nativeNumResultsSnippeted = 211;
        int nativeLockAcquisitionLatencyMillis = 212;
        int javaToNativeJniLatencyMillis = 213;
        int nativeToJavaJniLatencyMillis = 214;
        long liteIndexHitBufferByteSize = 215;
        long liteIndexHitBufferUnsortedByteSize = 216;
        int pageTypeToken = QueryStats.PAGE_TOKEN_TYPE_EMPTY;
        int numResultStatesEvicted = 217;
        int additionalPageCount = 218;
        int numResultsReturnedAdditionalPages = 219;
        int additionalPagesRetrievalLatency = 220;
        int firstNativeCallLatencyMillis = 221;

        final QueryStats.Builder sStatsBuilder = new QueryStats.Builder(visibilityScope,
                TEST_PACKAGE_NAME)
                .setDatabase(TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setLaunchVMEnabled(true)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setRewriteSearchSpecLatencyMillis(rewriteSearchSpecLatencyMillis)
                .setRewriteSearchResultLatencyMillis(rewriteSearchResultLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setAclCheckLatencyMillis(aclCheckLatencyMillis)
                .setSearchSourceLogTag(searchSourceLogTag)
                .setIsFirstPage(nativeIsFirstPage)
                .setRequestedPageSize(nativeRequestedPageSize)
                .setCurrentPageReturnedResultCount(nativeNumResultsReturnedCurrentPage)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setRankingLatencyMillis(nativeRankingLatencyMillis)
                .setDocumentRetrievingLatencyMillis(nativeDocumentRetrievingLatencyMillis)
                .setResultWithSnippetsCount(nativeNumResultsSnippeted)
                .setNativeLockAcquisitionLatencyMillis(nativeLockAcquisitionLatencyMillis)
                .setJavaToNativeJniLatencyMillis(javaToNativeJniLatencyMillis)
                .setNativeToJavaJniLatencyMillis(nativeToJavaJniLatencyMillis)
                .setChildSearchStats(searchStats)
                .setParentSearchStats(searchStats)
                .setLiteIndexHitBufferByteSize(liteIndexHitBufferByteSize)
                .setLiteIndexHitBufferUnsortedByteSize(liteIndexHitBufferUnsortedByteSize)
                .setPageTokenType(pageTypeToken)
                .setNumResultStatsEvicted(numResultStatesEvicted)
                .setAdditionalPageCount(additionalPageCount)
                .setAdditionalPagesReturnedResultCount(numResultsReturnedAdditionalPages)
                .setAdditionalPageRetrievalLatencyMillis(additionalPagesRetrievalLatency)
                .setFirstNativeCallLatency(firstNativeCallLatencyMillis);
        final QueryStats sStats = sStatsBuilder.build();

        assertThat(sStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getRewriteSearchSpecLatencyMillis()).isEqualTo(
                rewriteSearchSpecLatencyMillis);
        assertThat(sStats.getRewriteSearchResultLatencyMillis()).isEqualTo(
                rewriteSearchResultLatencyMillis);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getAclCheckLatencyMillis()).isEqualTo(
                aclCheckLatencyMillis);
        assertThat(sStats.getVisibilityScope()).isEqualTo(visibilityScope);
        assertThat(sStats.getSearchSourceLogTag()).isEqualTo(searchSourceLogTag);
        assertThat(sStats.isFirstPage()).isTrue();
        assertThat(sStats.getRequestedPageSize()).isEqualTo(nativeRequestedPageSize);
        assertThat(sStats.getCurrentPageReturnedResultCount()).isEqualTo(
                nativeNumResultsReturnedCurrentPage);
        assertThat(sStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(sStats.getFirstNativeCallLatencyMillis()).isEqualTo(
                firstNativeCallLatencyMillis);
        assertThat(sStats.getRankingLatencyMillis()).isEqualTo(nativeRankingLatencyMillis);
        assertThat(sStats.getResultWithSnippetsCount()).isEqualTo(nativeNumResultsSnippeted);
        assertThat(sStats.getDocumentRetrievingLatencyMillis()).isEqualTo(
                nativeDocumentRetrievingLatencyMillis);
        assertThat(sStats.getNativeLockAcquisitionLatencyMillis()).isEqualTo(
                nativeLockAcquisitionLatencyMillis);
        assertThat(sStats.getJavaToNativeJniLatencyMillis()).isEqualTo(
                javaToNativeJniLatencyMillis);
        assertThat(sStats.getNativeToJavaJniLatencyMillis()).isEqualTo(
                nativeToJavaJniLatencyMillis);
        assertThat(sStats.getParentSearchStats()).isEqualTo(searchStats);
        assertThat(sStats.getChildSearchStats()).isEqualTo(searchStats);
        assertThat(sStats.getLiteIndexHitBufferByteSize()).isEqualTo(liteIndexHitBufferByteSize);
        assertThat(sStats.getLiteIndexHitBufferUnsortedByteSize())
                .isEqualTo(liteIndexHitBufferUnsortedByteSize);
        assertThat(sStats.getPageTokenType()).isEqualTo(pageTypeToken);
        assertThat(sStats.getNumResultStatesEvicted()).isEqualTo(numResultStatesEvicted);
        assertThat(sStats.getAdditionalPageCount()).isEqualTo(additionalPageCount);
        assertThat(sStats.getAdditionalPagesReturnedResultCount()).isEqualTo(
                numResultsReturnedAdditionalPages);
        assertThat(sStats.getAdditionalPageRetrievalLatencyMillis()).isEqualTo(
                additionalPagesRetrievalLatency);
        String expectedString = "QueryStats {\n"
                + "package=com.google.test, database=testDataBase, status=2, total_latency=20, "
                + "rewrite_search_spec_latency=202,\n"
                + "rewrite_search_result_latency=203, java_lock_acquisition_latency=204, "
                + "acl_check_latency=205, visibility_score=1,\n"
                + "search_source_log_tag=tag, is_first_page=true, requested_page_size=206, "
                + "num_results_returned_current_page=207,\n"
                + "native_latency=208, ranking_latency=209, document_retrieving_latency=210, "
                + "num_results_with_snippets=211,\n"
                + "native_lock_acquisition_latency=212, java_to_native_jni_latency=213, "
                + "native_to_java_jni_latency=214,\n"
                + "join_latency_ms=0, num_joined_results_current_page=0, join_type=0, "
                + "lite_index_hit_buffer_byte_size=215,\n"
                + "lite_index_hit_buffer_unsorted_byte_size=216\n"
                + "page_token_type=3, num_result_states_evicted=217\n"
                + "parent_search_stats=SearchStats {\n"
                + "query_length=101, num_terms=102, num_namespaces_filtered=103, "
                + "num_schema_types_filtered=104,\n"
                + "ranking_strategy=105, num_docs_scored=106, parse_query_latency=107, "
                + "scoring_latency=108, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=109, num_fetched_hits_main_index=110, "
                + "num_fetched_hits_integer_index=111,\n"
                + "query_processor_lexer_extract_token_latency=112, "
                + "query_processor_parser_consume_query_latency=113,\n"
                + "query_processor_query_visitor_latency=114},\n"
                + " child_search_stats=SearchStats {\n"
                + "query_length=101, num_terms=102, num_namespaces_filtered=103, "
                + "num_schema_types_filtered=104,\n"
                + "ranking_strategy=105, num_docs_scored=106, parse_query_latency=107, "
                + "scoring_latency=108, is_numeric_query=true,\n"
                + "num_fetched_hits_lite_index=109, num_fetched_hits_main_index=110, "
                + "num_fetched_hits_integer_index=111,\n"
                + "query_processor_lexer_extract_token_latency=112, "
                + "query_processor_parser_consume_query_latency=113,\n"
                + "query_processor_query_visitor_latency=114}}";
        assertThat(sStats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testAppSearchStats_SetSchemaStats() {
        int newTypeCount = 1;
        int deletedTypeCount = 2;
        int compatibleTypeChangeCount = 3;
        int indexIncompatibleTypeChangeCount = 4;
        int backwardsIncompatibleTypeChangeCount = 5;
        int verifyIncomingCallLatencyMillis = 6;
        int executorAcquisitionLatencyMillis = 7;
        int rebuildFromBundleLatencyMillis = 8;
        int javaLockAcquisitionLatencyMillis = 9;
        int totalNativeLatencyMillis = 10;
        int rewriteSchemaLatencyMillis = 11;
        int visibilitySettingLatencyMillis = 12;
        int convertToResponseLatencyMillis = 13;
        int dispatchChangeNotificationsLatencyMillis = 14;
        int optimizeLatencyMillis = 15;
        boolean isPackageObserved = true;
        int getOldSchemaLatencyMillis = 16;
        int getObserverLatencyMillis = 17;
        int sendNotificationLatencyMillis = 18;
        int enabledFeatures = 1;
        SetSchemaStats sStats = new SetSchemaStats.Builder(TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNewTypeCount(newTypeCount)
                .setDeletedTypeCount(deletedTypeCount)
                .setCompatibleTypeChangeCount(compatibleTypeChangeCount)
                .setIndexIncompatibleTypeChangeCount(indexIncompatibleTypeChangeCount)
                .setBackwardsIncompatibleTypeChangeCount(backwardsIncompatibleTypeChangeCount)
                .setVerifyIncomingCallLatencyMillis(verifyIncomingCallLatencyMillis)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setRebuildFromBundleLatencyMillis(rebuildFromBundleLatencyMillis)
                .setJavaLockAcquisitionLatencyMillis(javaLockAcquisitionLatencyMillis)
                .setRewriteSchemaLatencyMillis(rewriteSchemaLatencyMillis)
                .setTotalNativeLatencyMillis(totalNativeLatencyMillis)
                .setVisibilitySettingLatencyMillis(visibilitySettingLatencyMillis)
                .setConvertToResponseLatencyMillis(convertToResponseLatencyMillis)
                .setDispatchChangeNotificationsLatencyMillis(
                        dispatchChangeNotificationsLatencyMillis)
                .setOptimizeLatencyMillis(optimizeLatencyMillis)
                .setIsPackageObserved(isPackageObserved)
                .setGetOldSchemaLatencyMillis(getOldSchemaLatencyMillis)
                .setGetObserverLatencyMillis(getObserverLatencyMillis)
                .setPreparingChangeNotificationLatencyMillis(sendNotificationLatencyMillis)
                .setSchemaMigrationCallType(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA)
                .setLaunchVMEnabled(true)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getNewTypeCount()).isEqualTo(newTypeCount);
        assertThat(sStats.getDeletedTypeCount()).isEqualTo(deletedTypeCount);
        assertThat(sStats.getCompatibleTypeChangeCount()).isEqualTo(compatibleTypeChangeCount);
        assertThat(sStats.getIndexIncompatibleTypeChangeCount()).isEqualTo(
                indexIncompatibleTypeChangeCount);
        assertThat(sStats.getBackwardsIncompatibleTypeChangeCount()).isEqualTo(
                backwardsIncompatibleTypeChangeCount);
        assertThat(sStats.getVerifyIncomingCallLatencyMillis()).isEqualTo(
                verifyIncomingCallLatencyMillis);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis()).isEqualTo(
                executorAcquisitionLatencyMillis);
        assertThat(sStats.getRebuildFromBundleLatencyMillis()).isEqualTo(
                rebuildFromBundleLatencyMillis);
        assertThat(sStats.getJavaLockAcquisitionLatencyMillis()).isEqualTo(
                javaLockAcquisitionLatencyMillis);
        assertThat(sStats.getRewriteSchemaLatencyMillis()).isEqualTo(rewriteSchemaLatencyMillis);
        assertThat(sStats.getTotalNativeLatencyMillis()).isEqualTo(totalNativeLatencyMillis);
        assertThat(sStats.getVisibilitySettingLatencyMillis()).isEqualTo(
                visibilitySettingLatencyMillis);
        assertThat(sStats.getConvertToResponseLatencyMillis()).isEqualTo(
                convertToResponseLatencyMillis);
        assertThat(sStats.getDispatchChangeNotificationsLatencyMillis()).isEqualTo(
                dispatchChangeNotificationsLatencyMillis);
        assertThat(sStats.getOptimizeLatencyMillis()).isEqualTo(optimizeLatencyMillis);
        assertThat(sStats.isPackageObserved()).isEqualTo(isPackageObserved);
        assertThat(sStats.getGetOldSchemaLatencyMillis()).isEqualTo(getOldSchemaLatencyMillis);
        assertThat(sStats.getGetObserverLatencyMillis()).isEqualTo(getObserverLatencyMillis);
        assertThat(sStats.getPreparingChangeNotificationLatencyMillis())
                .isEqualTo(sendNotificationLatencyMillis);
        assertThat(sStats.getSchemaMigrationCallType())
                .isEqualTo(SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA);
        assertThat(sStats.getEnabledFeatures())
                .isEqualTo(enabledFeatures);
    }

    @Test
    public void testAppSearchStats_SchemaMigrationStats() {
        int executorAcquisitionLatencyMillis = 1;
        int getSchemaLatency = 2;
        int queryAndTransformLatency = 3;
        int firstSetSchemaLatency = 4;
        boolean isFirstSetSchemaSuccess = true;
        int secondSetSchemaLatency = 5;
        int saveDocumentLatency = 6;
        int migratedDocumentCount = 7;
        int savedDocumentCount = 8;
        int migrationFailureCount = 9;
        SchemaMigrationStats sStats = new SchemaMigrationStats.Builder(
                TEST_PACKAGE_NAME, TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setExecutorAcquisitionLatencyMillis(executorAcquisitionLatencyMillis)
                .setGetSchemaLatencyMillis(getSchemaLatency)
                .setQueryAndTransformLatencyMillis(queryAndTransformLatency)
                .setFirstSetSchemaLatencyMillis(firstSetSchemaLatency)
                .setIsFirstSetSchemaSuccess(isFirstSetSchemaSuccess)
                .setSecondSetSchemaLatencyMillis(secondSetSchemaLatency)
                .setSaveDocumentLatencyMillis(saveDocumentLatency)
                .setTotalNeedMigratedDocumentCount(migratedDocumentCount)
                .setTotalSuccessMigratedDocumentCount(savedDocumentCount)
                .setMigrationFailureCount(migrationFailureCount)
                .build();

        assertThat(sStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(sStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(sStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(sStats.getTotalLatencyMillis()).isEqualTo(
                TEST_TOTAL_LATENCY_MILLIS);
        assertThat(sStats.getExecutorAcquisitionLatencyMillis())
                .isEqualTo(executorAcquisitionLatencyMillis);
        assertThat(sStats.getGetSchemaLatencyMillis()).isEqualTo(getSchemaLatency);
        assertThat(sStats.getQueryAndTransformLatencyMillis()).isEqualTo(queryAndTransformLatency);
        assertThat(sStats.getFirstSetSchemaLatencyMillis()).isEqualTo(firstSetSchemaLatency);
        assertThat(sStats.isFirstSetSchemaSuccess()).isEqualTo(isFirstSetSchemaSuccess);
        assertThat(sStats.getSecondSetSchemaLatencyMillis()).isEqualTo(secondSetSchemaLatency);
        assertThat(sStats.getSaveDocumentLatencyMillis()).isEqualTo(saveDocumentLatency);
        assertThat(sStats.getTotalNeedMigratedDocumentCount()).isEqualTo(migratedDocumentCount);
        assertThat(sStats.getTotalSuccessMigratedDocumentCount()).isEqualTo(savedDocumentCount);
        assertThat(sStats.getMigrationFailureCount()).isEqualTo(migrationFailureCount);
    }

    @Test
    public void testAppSearchStats_RemoveStats() {
        int nativeLatencyMillis = 1;
        @RemoveStats.DeleteType int deleteType = 2;
        int documentDeletedCount = 3;
        int enabledFeatures = 1;
        int queryLength = 4;
        int numTerms = 5;
        int numNamespacesFiltered = 6;
        int numSchemaTypesFiltered = 7;
        int parseQueryLatencyMillis = 8;
        int documentRemovalLatencyMillis = 9;

        final RemoveStats rStats = new RemoveStats.Builder(TEST_PACKAGE_NAME,
                TEST_DATA_BASE)
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDeleteType(deleteType)
                .setDeletedDocumentCount(documentDeletedCount)
                .setLaunchVMEnabled(true)
                .setQueryLength(queryLength)
                .setNumTerms(numTerms)
                .setNumNamespacesFiltered(numNamespacesFiltered)
                .setNumSchemaTypesFiltered(numSchemaTypesFiltered)
                .setParseQueryLatencyMillis(parseQueryLatencyMillis)
                .setDocumentRemovalLatencyMillis(documentRemovalLatencyMillis)
                .build();


        assertThat(rStats.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(rStats.getDatabase()).isEqualTo(TEST_DATA_BASE);
        assertThat(rStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(rStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(rStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
        assertThat(rStats.getDeleteType()).isEqualTo(deleteType);
        assertThat(rStats.getDeletedDocumentCount()).isEqualTo(documentDeletedCount);
        assertThat(rStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(rStats.getQueryLength()).isEqualTo(queryLength);
        assertThat(rStats.getNumTerms()).isEqualTo(numTerms);
        assertThat(rStats.getNumNamespacesFiltered()).isEqualTo(numNamespacesFiltered);
        assertThat(rStats.getNumSchemaTypesFiltered()).isEqualTo(numSchemaTypesFiltered);
        assertThat(rStats.getParseQueryLatencyMillis()).isEqualTo(parseQueryLatencyMillis);
        assertThat(rStats.getDocumentRemovalLatencyMillis())
                .isEqualTo(documentRemovalLatencyMillis);
    }

    @Test
    public void testAppSearchStats_OptimizeStats() {
        int nativeLatencyMillis = 1;
        int nativeDocumentStoreOptimizeLatencyMillis = 2;
        int nativeIndexRestorationLatencyMillis = 3;
        int nativeNumOriginalDocuments = 4;
        int nativeNumDeletedDocuments = 5;
        int nativeNumExpiredDocuments = 6;
        int enabledFeatures = 1;
        long nativeStorageSizeBeforeBytes = Integer.MAX_VALUE + 1;
        long nativeStorageSizeAfterBytes = Integer.MAX_VALUE + 2;
        long nativeTimeSinceLastOptimizeMillis = Integer.MAX_VALUE + 3;
        int indexRestorationMode = 1;
        int numOriginalNamespaces = 7;
        int numDeletedNamespaces = 8;

        final OptimizeStats oStats = new OptimizeStats.Builder()
                .setStatusCode(TEST_STATUS_CODE)
                .setTotalLatencyMillis(TEST_TOTAL_LATENCY_MILLIS)
                .setNativeLatencyMillis(nativeLatencyMillis)
                .setDocumentStoreOptimizeLatencyMillis(nativeDocumentStoreOptimizeLatencyMillis)
                .setIndexRestorationLatencyMillis(nativeIndexRestorationLatencyMillis)
                .setOriginalDocumentCount(nativeNumOriginalDocuments)
                .setDeletedDocumentCount(nativeNumDeletedDocuments)
                .setExpiredDocumentCount(nativeNumExpiredDocuments)
                .setStorageSizeBeforeBytes(nativeStorageSizeBeforeBytes)
                .setStorageSizeAfterBytes(nativeStorageSizeAfterBytes)
                .setTimeSinceLastOptimizeMillis(nativeTimeSinceLastOptimizeMillis)
                .setLaunchVMEnabled(true)
                .setIndexRestorationMode(indexRestorationMode)
                .setNumOriginalNamespaces(numOriginalNamespaces)
                .setNumDeletedNamespaces(numDeletedNamespaces)
                .build();

        assertThat(oStats.getStatusCode()).isEqualTo(TEST_STATUS_CODE);
        assertThat(oStats.getTotalLatencyMillis()).isEqualTo(TEST_TOTAL_LATENCY_MILLIS);
        assertThat(oStats.getNativeLatencyMillis()).isEqualTo(nativeLatencyMillis);
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
        assertThat(oStats.getEnabledFeatures()).isEqualTo(enabledFeatures);
        assertThat(oStats.getIndexRestorationMode()).isEqualTo(indexRestorationMode);
        assertThat(oStats.getNumOriginalNamespaces()).isEqualTo(numOriginalNamespaces);
        assertThat(oStats.getNumDeletedNamespaces()).isEqualTo(numDeletedNamespaces);
    }
}
