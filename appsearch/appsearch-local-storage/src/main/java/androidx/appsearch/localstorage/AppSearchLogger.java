/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchSessionStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.stats.SchemaMigrationStats;

import java.util.List;

/**
 * An interface for implementing client-defined logging AppSearch operations stats.
 *
 * <p>Any implementation needs to provide general information on how to log all the stats types.
 * (for example {@link CallStats})
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchLogger {
    /**
     * Logs {@link CallStats}
     */
    default void logStats(@NonNull CallStats stats) {
        // no-op
    }

    /**
     * Logs {@link PutDocumentStats}
     */
    default void logStats(@NonNull PutDocumentStats stats) {
        // no-op
    }

    /**
     * Logs {@link InitializeStats}
     */
    default void logStats(@NonNull InitializeStats stats) {
        // no-op
    }

    /**
     * Logs {@link SearchStats}
     */
    default void logStats(@NonNull SearchStats stats) {
        // no-op
    }

    /** Logs {@link RemoveStats} */
    default void logStats(@NonNull RemoveStats stats) {
        // no-op
    }

    /**
     * Logs {@link OptimizeStats}
     */
    default void logStats(@NonNull OptimizeStats stats) {
        // no-op
    }

    /**
     * Logs {@link SetSchemaStats}
     */
    default void logStats(@NonNull SetSchemaStats stats) {
        // no-op
    }

    /**
     * Logs {@link SchemaMigrationStats}
     */
    default void logStats(@NonNull SchemaMigrationStats stats) {
        // no-op
    }

    /**
     * Logs a list of {@link SearchSessionStats}.
     *
     * <p>Since the client app may report search intents belonging to different search sessions in a
     * single taken action reporting request, the stats extractor will separate them into multiple
     * search sessions. Therefore, we need a list of {@link SearchSessionStats} here.
     *
     * <p>For example, the client app reports the following search intent sequence:
     *
     * <ul>
     *   <li>t = 1, the user searches "a" with some clicks.
     *   <li>t = 5, the user searches "app" with some clicks.
     *   <li>t = 10000, the user searches "email" with some clicks.
     * </ul>
     *
     * The extractor will detect "email" belongs to a completely independent search session, and
     * creates 2 {@link SearchSessionStats} with search intents ["a", "app"] and ["email"]
     * respectively.
     */
    default void logStats(@NonNull List<SearchSessionStats> searchSessionsStats) {
        // no-op
    }

    // TODO(b/173532925) Add remaining logStats once we add all the stats.
}
