/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.playservicesstorage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.playservicesstorage.util.AppSearchTaskFutures;
import androidx.core.util.Preconditions;

import com.google.android.gms.appsearch.AppSearch;
import com.google.android.gms.appsearch.AppSearchClient;
import com.google.android.gms.appsearch.GlobalSearchClient;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * An AppSearch storage system which stores data in the central AppSearch service in Google
 * Play Services.
 */
public final class PlayServicesStorage {

    private PlayServicesStorage() {
    }

    /** Contains information about how to create the search session. */
    public static final class SearchContext {
        final Context mContext;
        final String mDatabaseName;

        SearchContext(@NonNull Context context, @NonNull String databaseName) {
            mContext = Preconditions.checkNotNull(context);
            mDatabaseName = Preconditions.checkNotNull(databaseName);
        }

        /**
         * Returns the name of the database to create or open.
         */
        @NonNull
        public String getDatabaseName() {
            return mDatabaseName;
        }

        /** Builder for {@link SearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private final String mDatabaseName;

            /**
             * Creates a {@link SearchContext.Builder} instance.
             *
             * <p>{@link AppSearchSession} will create or open a database under the given name.
             *
             * <p>Databases with different names are fully separate with distinct schema types,
             * namespaces, and documents.
             *
             * <p>The database name cannot contain {@code '/'}.
             *
             * <p>The database name will be visible to all system UI or third-party applications
             * that have been granted access to any of the database's documents (for example,
             * using {@link
             * androidx.appsearch.app.SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage}).
             *
             * @param databaseName The name of the database.
             * @throws IllegalArgumentException if the databaseName contains {@code '/'}.
             */
            public Builder(@NonNull Context context, @NonNull String databaseName) {
                mContext = Preconditions.checkNotNull(context);
                Preconditions.checkNotNull(databaseName);
                if (databaseName.contains("/")) {
                    throw new IllegalArgumentException("Database name cannot contain '/'");
                }
                mDatabaseName = databaseName;
            }

            /** Builds a {@link SearchContext} instance. */
            @NonNull
            public SearchContext build() {
                return new SearchContext(mContext, mDatabaseName);
            }
        }
    }

    /** Contains information relevant to creating a global search session. */
    public static final class GlobalSearchContext {
        final Context mContext;

        GlobalSearchContext(@NonNull Context context) {
            mContext = Preconditions.checkNotNull(context);
        }

        /** Builder for {@link GlobalSearchContext} objects. */
        public static final class Builder {
            private final Context mContext;

            public Builder(@NonNull Context context) {
                mContext = Preconditions.checkNotNull(context);
            }

            /** Builds a {@link GlobalSearchContext} instance. */
            @NonNull
            public GlobalSearchContext build() {
                return new GlobalSearchContext(mContext);
            }
        }
    }

    /**
     * Opens a new {@link AppSearchSession} on this storage.
     *
     * @param context The {@link SearchContext} contains all information to create a new
     *                {@link AppSearchSession}
     */
    @NonNull
    public static ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull SearchContext context) {
        Preconditions.checkNotNull(context);
        Task<AppSearchClient> appSearchClientTask = AppSearch
                .createAppSearchClient(context.mContext);
        return AppSearchTaskFutures.toListenableFuture(
                appSearchClientTask,
                task -> new SearchSessionImpl(task, new FeaturesImpl()));
    }

    /**
     * Opens a new {@link GlobalSearchSession} on this storage.
     *
     * @param context The {@link GlobalSearchContext} contains all information to create a new
     *                {@link GlobalSearchSession}
     */
    @NonNull
    public static ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        Task<GlobalSearchClient> globalSearchClientTask = AppSearch
                .createGlobalSearchClient(context.mContext);
        return AppSearchTaskFutures.toListenableFuture(
                globalSearchClientTask,
                task -> new GlobalSearchSessionImpl(task, new FeaturesImpl()));
    }
}
