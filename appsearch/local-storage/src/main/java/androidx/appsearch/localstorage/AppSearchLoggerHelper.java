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
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.InitializeStatsProto;
import com.google.android.icing.proto.PutDocumentStatsProto;

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
                        fromNativeStats.getTokenizationStats().getNumTokensIndexed())
                .setNativeExceededMaxNumTokens(
                        fromNativeStats.getTokenizationStats().getExceededMaxTokenNum());
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
}
