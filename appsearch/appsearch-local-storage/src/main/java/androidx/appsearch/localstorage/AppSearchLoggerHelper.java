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

import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.QueryStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.DeleteByQueryStatsProto;
import com.google.android.icing.proto.DeleteStatsProto;
import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.OptimizeStatsProto;
import com.google.android.icing.proto.PutDocumentStatsProto;
import com.google.android.icing.proto.QueryStatsProto;
import com.google.android.icing.proto.SetSchemaResultProto;

import org.jspecify.annotations.NonNull;

/**
 * Class contains helper functions for logging.
 *
 * <p>E.g. we need to have helper functions to copy numbers from IcingLib to stats classes.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AppSearchLoggerHelper {
    private AppSearchLoggerHelper() {
    }

    /**
     * Copies native PutDocument stats to builder.
     *
     * @param fromNativeStats stats copied from
     * @param toStatsBuilder  stats copied to
     */
    static void copyNativeStats(@NonNull PutDocumentStatsProto fromNativeStats,
            PutDocumentStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setNativeDocumentStoreLatencyMillis(
                        fromNativeStats.getDocumentStoreLatencyMs())
                .setNativeIndexLatencyMillis(fromNativeStats.getIndexLatencyMs())
                .setNativeIndexMergeLatencyMillis(fromNativeStats.getIndexMergeLatencyMs())
                .setNativeDocumentSizeBytes(fromNativeStats.getDocumentSize())
                .setNativeNumTokensIndexed(
                        fromNativeStats.getTokenizationStats().getNumTokensIndexed())
                .setNativeTermIndexLatencyMillis(fromNativeStats.getTermIndexLatencyMs())
                .setNativeIntegerIndexLatencyMillis(fromNativeStats.getIntegerIndexLatencyMs())
                .setNativeQualifiedIdJoinIndexLatencyMillis(
                        fromNativeStats.getQualifiedIdJoinIndexLatencyMs())
                .setNativeLiteIndexSortLatencyMillis(
                        fromNativeStats.getLiteIndexSortLatencyMs())
                .setMetadataTermIndexLatencyMillis(fromNativeStats.getMetadataTermIndexLatencyMs())
                .setEmbeddingIndexLatencyMillis(fromNativeStats.getEmbeddingIndexLatencyMs());
    }

    /**
     * Copies native Initialize stats to builder.
     *
     * @param fromNativeStats stats copied from
     * @param toStatsBuilder  stats copied to
     */
    static void copyNativeStats(@NonNull InitializeStatsProto fromNativeStats,
            InitializeStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setNativeDocumentStoreRecoveryCause(
                        fromNativeStats.getDocumentStoreRecoveryCause().getNumber())
                .setNativeIndexRestorationCause(
                        fromNativeStats.getIndexRestorationCause().getNumber())
                .setNativeSchemaStoreRecoveryCause(
                        fromNativeStats.getSchemaStoreRecoveryCause().getNumber())
                .setNativeDocumentStoreRecoveryLatencyMillis(
                        fromNativeStats.getDocumentStoreRecoveryLatencyMs())
                .setNativeIndexRestorationLatencyMillis(
                        fromNativeStats.getIndexRestorationLatencyMs())
                .setNativeSchemaStoreRecoveryLatencyMillis(
                        fromNativeStats.getSchemaStoreRecoveryLatencyMs())
                .setNativeDocumentStoreDataStatus(
                        fromNativeStats.getDocumentStoreDataStatus().getNumber())
                .setNativeDocumentCount(fromNativeStats.getNumDocuments())
                .setNativeSchemaTypeCount(fromNativeStats.getNumSchemaTypes())
                .setNativeNumPreviousInitFailures(fromNativeStats.getNumPreviousInitFailures())
                .setNativeIntegerIndexRestorationCause(
                        fromNativeStats.getIntegerIndexRestorationCause().getNumber())
                .setNativeQualifiedIdJoinIndexRestorationCause(
                        fromNativeStats.getQualifiedIdJoinIndexRestorationCause().getNumber())
                .setNativeEmbeddingIndexRestorationCause(
                        fromNativeStats.getEmbeddingIndexRestorationCause().getNumber())
                .setNativeInitializeIcuDataStatusCode(
                        fromNativeStats.getInitializeIcuDataStatus().getCode().getNumber())
                .setNativeNumFailedReindexedDocuments(
                        fromNativeStats.getNumFailedReindexedDocuments());
    }

    /**
     * Copies native Query stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull QueryStatsProto fromNativeStats,
            QueryStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setIsFirstPage(fromNativeStats.getIsFirstPage())
                .setRequestedPageSize(fromNativeStats.getRequestedPageSize())
                .setCurrentPageReturnedResultCount(
                        fromNativeStats.getNumResultsReturnedCurrentPage())
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setRankingLatencyMillis(fromNativeStats.getRankingLatencyMs())
                .setDocumentRetrievingLatencyMillis(
                        fromNativeStats.getDocumentRetrievalLatencyMs())
                .setResultWithSnippetsCount(fromNativeStats.getNumResultsWithSnippets())
                .setNativeLockAcquisitionLatencyMillis(
                        fromNativeStats.getLockAcquisitionLatencyMs())
                .setJavaToNativeJniLatencyMillis(
                        fromNativeStats.getJavaToNativeJniLatencyMs())
                .setNativeToJavaJniLatencyMillis(
                        fromNativeStats.getNativeToJavaJniLatencyMs())
                .setNativeJoinLatencyMillis(fromNativeStats.getJoinLatencyMs())
                .setNativeNumJoinedResultsCurrentPage(
                        fromNativeStats.getNumJoinedResultsReturnedCurrentPage())
                .setParentSearchStats(copyNativeStats(fromNativeStats.getParentSearchStats()))
                .setChildSearchStats(copyNativeStats(fromNativeStats.getChildSearchStats()))
                .setLiteIndexHitBufferByteSize(fromNativeStats.getLiteIndexHitBufferByteSize())
                .setLiteIndexHitBufferUnsortedByteSize(
                        fromNativeStats.getLiteIndexHitBufferUnsortedByteSize())
                .setPageTokenType(fromNativeStats.getPageTokenType().getNumber())
                .setNumResultStatsEvicted(fromNativeStats.getNumResultStatesEvicted());
    }

    /**
     * Copies native Search stats to {@link SearchStats}.
     *
     * @param fromNativeStats Stats copied from.
     */
    static @NonNull SearchStats copyNativeStats(
            QueryStatsProto.@NonNull SearchStats fromNativeStats) {
        Preconditions.checkNotNull(fromNativeStats);
        return new SearchStats.Builder()
                .setNativeQueryLength(fromNativeStats.getQueryLength())
                .setNativeTermCount(fromNativeStats.getNumTerms())
                .setNativeFilteredNamespaceCount(fromNativeStats.getNumNamespacesFiltered())
                .setNativeFilteredSchemaTypeCount(fromNativeStats.getNumSchemaTypesFiltered())
                .setNativeRankingStrategy(fromNativeStats.getRankingStrategy().getNumber())
                .setNativeScoredDocumentCount(fromNativeStats.getNumDocumentsScored())
                .setNativeParseQueryLatencyMillis(fromNativeStats.getParseQueryLatencyMs())
                .setNativeScoringLatencyMillis(fromNativeStats.getScoringLatencyMs())
                .setNativeIsNumericQuery(fromNativeStats.getIsNumericQuery())
                .setNativeNumFetchedHitsLiteIndex(fromNativeStats.getNumFetchedHitsLiteIndex())
                .setNativeNumFetchedHitsMainIndex(fromNativeStats.getNumFetchedHitsMainIndex())
                .setNativeNumFetchedHitsIntegerIndex(
                        fromNativeStats.getNumFetchedHitsIntegerIndex())
                .setNativeQueryProcessorLexerExtractTokenLatencyMillis(
                        fromNativeStats.getQueryProcessorLexerExtractTokenLatencyMs())
                .setNativeQueryProcessorParserConsumeQueryLatencyMillis(
                        fromNativeStats.getQueryProcessorParserConsumeQueryLatencyMs())
                .setNativeQueryProcessorQueryVisitorLatencyMillis(
                        fromNativeStats.getQueryProcessorQueryVisitorLatencyMs())
                .build();
    }

    /**
     * Copies native Delete stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull DeleteStatsProto fromNativeStats,
            RemoveStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDeleteType(fromNativeStats.getDeleteType().getNumber())
                .setDeletedDocumentCount(fromNativeStats.getNumDocumentsDeleted());
    }

    /**
     * Copies native DeleteByQuery stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull DeleteByQueryStatsProto fromNativeStats,
            RemoveStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);

        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDeleteType(RemoveStats.QUERY)
                .setDeletedDocumentCount(fromNativeStats.getNumDocumentsDeleted())
                .setQueryLength(fromNativeStats.getQueryLength())
                .setNumTerms(fromNativeStats.getNumTerms())
                .setNumNamespacesFiltered(fromNativeStats.getNumNamespacesFiltered())
                .setNumSchemaTypesFiltered(fromNativeStats.getNumSchemaTypesFiltered())
                .setParseQueryLatencyMillis(fromNativeStats.getParseQueryLatencyMs())
                .setDocumentRemovalLatencyMillis(fromNativeStats.getDocumentRemovalLatencyMs());
    }

    /**
     * Copies native {@link OptimizeStatsProto} to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull OptimizeStatsProto fromNativeStats,
            OptimizeStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDocumentStoreOptimizeLatencyMillis(
                        fromNativeStats.getDocumentStoreOptimizeLatencyMs())
                .setIndexRestorationLatencyMillis(fromNativeStats.getIndexRestorationLatencyMs())
                .setOriginalDocumentCount(fromNativeStats.getNumOriginalDocuments())
                .setDeletedDocumentCount(fromNativeStats.getNumDeletedDocuments())
                .setExpiredDocumentCount(fromNativeStats.getNumExpiredDocuments())
                .setStorageSizeBeforeBytes(fromNativeStats.getStorageSizeBefore())
                .setStorageSizeAfterBytes(fromNativeStats.getStorageSizeAfter())
                .setTimeSinceLastOptimizeMillis(fromNativeStats.getTimeSinceLastOptimizeMs())
                .setIndexRestorationMode(fromNativeStats.getIndexRestorationMode().getNumber())
                .setNumOriginalNamespaces(fromNativeStats.getNumOriginalNamespaces())
                .setNumDeletedNamespaces(fromNativeStats.getNumDeletedNamespaces());
    }

    /*
     * Copy SetSchema result stats to builder.
     *
     * @param fromProto Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull SetSchemaResultProto fromProto,
            SetSchemaStats.@NonNull Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromProto);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNewTypeCount(fromProto.getNewSchemaTypesCount())
                .setDeletedTypeCount(fromProto.getDeletedSchemaTypesCount())
                .setCompatibleTypeChangeCount(fromProto.getFullyCompatibleChangedSchemaTypesCount())
                .setIndexIncompatibleTypeChangeCount(
                        fromProto.getIndexIncompatibleChangedSchemaTypesCount())
                .setBackwardsIncompatibleTypeChangeCount(
                        fromProto.getIncompatibleSchemaTypesCount());
    }
}
