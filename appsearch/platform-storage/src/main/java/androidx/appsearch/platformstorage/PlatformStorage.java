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
package androidx.appsearch.platformstorage;

import android.app.appsearch.AppSearchManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.platformstorage.converter.SearchContextToPlatformConverter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An AppSearch storage system which stores data in the central AppSearch service, available on
 * Android S+.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class PlatformStorage {
    /**
     * The default empty database name.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public static final String DEFAULT_DATABASE_NAME = "";

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
            private String mDatabaseName = DEFAULT_DATABASE_NAME;
            private boolean mBuilt = false;

            public Builder(@NonNull Context context) {
                mContext = Preconditions.checkNotNull(context);
            }

            /**
             * Sets the name of the database associated with {@link AppSearchSession}.
             *
             * <p>{@link AppSearchSession} will create or open a database under the given name.
             *
             * <p>Databases with different names are fully separate with distinct schema types,
             * namespaces, and documents.
             *
             * <p>The database name cannot contain {@code '/'}.
             *
             * <p>If not specified, the database name is set to an empty string..
             *
             * <p>The database name will be visible to all system UI or third-party applications
             * that have been granted access to any of the database's documents (for example,
             * using {@link
             * androidx.appsearch.app.SetSchemaRequest.Builder#setSchemaTypeVisibilityForPackage}).
             *
             * @param databaseName The name of the database.
             * @throws IllegalArgumentException if the databaseName contains {@code '/'}.
             * @throws IllegalStateException    if the builder has already been used.
             */
            @NonNull
            public Builder setDatabaseName(@NonNull String databaseName) {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                Preconditions.checkNotNull(databaseName);
                if (databaseName.contains("/")) {
                    throw new IllegalArgumentException("Database name cannot contain '/'");
                }
                mDatabaseName = databaseName;
                return this;
            }

            /** Builds a {@link SearchContext} instance. */
            @NonNull
            public SearchContext build() {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                mBuilt = true;
                return new SearchContext(mContext, mDatabaseName);
            }
        }
    }

    // Never call Executor.shutdownNow(), it will cancel the futures it's returned. And since
    // execute() won't return anything, we will hang forever waiting for the execution.
    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private PlatformStorage() {
    }

    /**
     * Opens a new {@link AppSearchSession} on this storage.
     *
     * @param context The {@link SearchContext} contains all information to create a new
     *                {@link AppSearchSession}
     */
    @NonNull
    public static ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull SearchContext context) {
        Preconditions.checkNotNull(context);
        AppSearchManager appSearchManager =
                context.mContext.getSystemService(AppSearchManager.class);
        ResolvableFuture<AppSearchSession> future = ResolvableFuture.create();
        appSearchManager.createSearchSession(
                SearchContextToPlatformConverter.toPlatformSearchContext(context),
                EXECUTOR_SERVICE,
                result -> {
                    if (result.isSuccess()) {
                        future.set(
                                new SearchSessionImpl(result.getResultValue(), EXECUTOR_SERVICE));
                    } else {
                        future.setException(
                                new AppSearchException(
                                        result.getResultCode(), result.getErrorMessage()));
                    }
                });
        return future;
    }
}
