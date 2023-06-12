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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
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

/**
 * Class contains helper functions for logging.
 *
 * <p>E.g. we need to have helper functions to copy numbers from IcingLib to stats classes.
 *
 * @hide
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
            @NonNull PutDocumentStats.Builder toStatsBuilder) {
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
                        fromNativeStats.getTokenizationStats().getNumTokensIndexed());
    }

    /**
     * Copies native Initialize stats to builder.
     *
     * @param fromNativeStats stats copied from
     * @param toStatsBuilder  stats copied to
     */
    static void copyNativeStats(@NonNull InitializeStatsProto fromNativeStats,
            @NonNull InitializeStats.Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDocumentStoreRecoveryCause(
                        fromNativeStats.getDocumentStoreRecoveryCause().getNumber())
                .setIndexRestorationCause(
                        fromNativeStats.getIndexRestorationCause().getNumber())
                .setSchemaStoreRecoveryCause(
                        fromNativeStats.getSchemaStoreRecoveryCause().getNumber())
                .setDocumentStoreRecoveryLatencyMillis(
                        fromNativeStats.getDocumentStoreRecoveryLatencyMs())
                .setIndexRestorationLatencyMillis(
                        fromNativeStats.getIndexRestorationLatencyMs())
                .setSchemaStoreRecoveryLatencyMillis(
                        fromNativeStats.getSchemaStoreRecoveryLatencyMs())
                .setDocumentStoreDataStatus(
                        fromNativeStats.getDocumentStoreDataStatus().getNumber())
                .setDocumentCount(fromNativeStats.getNumDocuments())
                .setSchemaTypeCount(fromNativeStats.getNumSchemaTypes());
    }

    /**
     * Copies native Query stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull QueryStatsProto fromNativeStats,
            @NonNull SearchStats.Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setTermCount(fromNativeStats.getNumTerms())
                .setQueryLength(fromNativeStats.getQueryLength())
                .setFilteredNamespaceCount(fromNativeStats.getNumNamespacesFiltered())
                .setFilteredSchemaTypeCount(fromNativeStats.getNumSchemaTypesFiltered())
                .setRequestedPageSize(fromNativeStats.getRequestedPageSize())
                .setCurrentPageReturnedResultCount(
                        fromNativeStats.getNumResultsReturnedCurrentPage())
                .setIsFirstPage(fromNativeStats.getIsFirstPage())
                .setParseQueryLatencyMillis(fromNativeStats.getParseQueryLatencyMs())
                .setRankingStrategy(fromNativeStats.getRankingStrategy().getNumber())
                .setScoredDocumentCount(fromNativeStats.getNumDocumentsScored())
                .setScoringLatencyMillis(fromNativeStats.getScoringLatencyMs())
                .setRankingLatencyMillis(fromNativeStats.getRankingLatencyMs())
                .setResultWithSnippetsCount(fromNativeStats.getNumResultsWithSnippets())
                .setDocumentRetrievingLatencyMillis(
                        fromNativeStats.getDocumentRetrievalLatencyMs())
                .setNativeLockAcquisitionLatencyMillis(
                        fromNativeStats.getLockAcquisitionLatencyMs())
                .setJavaToNativeJniLatencyMillis(
                        fromNativeStats.getJavaToNativeJniLatencyMs())
                .setNativeToJavaJniLatencyMillis(
                        fromNativeStats.getNativeToJavaJniLatencyMs())
                .setNativeNumJoinedResultsCurrentPage(
                        fromNativeStats.getNumJoinedResultsReturnedCurrentPage())
                .setNativeJoinLatencyMillis(fromNativeStats.getJoinLatencyMs());
    }

    /**
     * Copies native Delete stats to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull DeleteStatsProto fromNativeStats,
            @NonNull RemoveStats.Builder toStatsBuilder) {
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
            @NonNull RemoveStats.Builder toStatsBuilder) {
        Preconditions.checkNotNull(fromNativeStats);
        Preconditions.checkNotNull(toStatsBuilder);

        @SuppressWarnings("deprecation")
        int deleteType = DeleteStatsProto.DeleteType.Code.DEPRECATED_QUERY.getNumber();
        toStatsBuilder
                .setNativeLatencyMillis(fromNativeStats.getLatencyMs())
                .setDeleteType(deleteType)
                .setDeletedDocumentCount(fromNativeStats.getNumDocumentsDeleted());
    }

    /**
     * Copies native {@link OptimizeStatsProto} to builder.
     *
     * @param fromNativeStats Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull OptimizeStatsProto fromNativeStats,
            @NonNull OptimizeStats.Builder toStatsBuilder) {
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
                .setTimeSinceLastOptimizeMillis(fromNativeStats.getTimeSinceLastOptimizeMs());
    }

    /*
     * Copy SetSchema result stats to builder.
     *
     * @param fromProto Stats copied from.
     * @param toStatsBuilder Stats copied to.
     */
    static void copyNativeStats(@NonNull SetSchemaResultProto fromProto,
            @NonNull SetSchemaStats.Builder toStatsBuilder) {
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
