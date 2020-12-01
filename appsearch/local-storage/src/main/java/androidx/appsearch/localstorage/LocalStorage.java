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
// @exportToFramework:skipFile()
package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.newSuccessfulResult;
import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.content.Context;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An AppSearch storage system which stores data locally in the app's storage space using a bundled
 * version of the search native library.
 *
 * <p>The search native library is an on-device searching library that allows apps to define
 * {@link androidx.appsearch.app.AppSearchSchema}s, save and query a variety of
 * {@link androidx.appsearch.annotation.AppSearchDocument}s. The library needs to be initialized
 * before using, which will create a folder to save data in the app's storage space.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
public class LocalStorage {
    /** The default empty database name.*/
    private static final String DEFAULT_DATABASE_NAME = "";

    private static volatile ListenableFuture<AppSearchResult<LocalStorage>> sInstance;

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
         *
         * <p>Databases with different names are fully separate with distinct types, namespaces,
         * and data.
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
             * <p>Databases with different names are fully separate with distinct types, namespaces,
             * and data.
             *
             * <p>Database name cannot contain {@code '/'}.
             *
             * <p>If not specified, defaults to the empty string.
             *
             * @param databaseName The name of the database.
             * @throws IllegalArgumentException if the databaseName contains {@code '/'}.
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

    /**
     * Contains information relevant to creating a global search session.
     */
    public static final class GlobalSearchContext {
        final Context mContext;

        GlobalSearchContext(@NonNull Context context) {
            mContext = Preconditions.checkNotNull(context);
        }

        /** Builder for {@link GlobalSearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private boolean mBuilt = false;

            public Builder(@NonNull Context context) {
                mContext = Preconditions.checkNotNull(context);
            }

            /** Builds a {@link GlobalSearchContext} instance. */
            @NonNull
            public GlobalSearchContext build() {
                Preconditions.checkState(!mBuilt, "Builder has already been used");
                mBuilt = true;
                return new GlobalSearchContext(mContext);
            }
        }
    }

    // Never call Executor.shutdownNow(), it will cancel the futures it's returned. And since
    // execute() won't return anything, we will hang forever waiting for the execution.
    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    private final ExecutorService mExecutorService = Executors.newCachedThreadPool();

    private static final String ICING_LIB_ROOT_DIR = "appsearch";

    // Package-level visibility to allow SearchResultsImpl to access it without a synthetic
    // accessor.
    volatile AppSearchImpl mAppSearchImpl;


    /**
     * Opens a new {@link AppSearchSession} on this storage.
     *
     * <p>This process requires a native search library. If it's not created, the initialization
     * process will create one.
     *
     * @param context The {@link SearchContext} contains all information to create a new
     *                {@link AppSearchSession}
     */
    @NonNull
    public static ListenableFuture<AppSearchResult<AppSearchSession>> createSearchSession(
            @NonNull SearchContext context) {
        Preconditions.checkNotNull(context);
        ListenableFuture<AppSearchResult<LocalStorage>> instFuture = getInstance(context.mContext);
        return FutureUtil.map(instFuture, (instance) -> {
            if (!instance.isSuccess()) {
                return AppSearchResult.newFailedResult(
                        instance.getResultCode(), instance.getErrorMessage());
            }
            AppSearchSession searchSession =
                    instance.getResultValue().doCreateSearchSession(context);
            return AppSearchResult.newSuccessfulResult(searchSession);
        });
    }

    /**
     * Opens a new {@link GlobalSearchSession} on this storage.
     *
     * <p>This process requires a native search library. If it's not created, the initialization
     * process will create one.
     *
     * @param context The {@link GlobalSearchContext} contains all information to create a new
     *                {@link GlobalSearchSession}
     * @hide
     */
    @NonNull
    public static ListenableFuture<AppSearchResult<GlobalSearchSession>> createGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        ListenableFuture<AppSearchResult<LocalStorage>> instFuture = getInstance(context.mContext);
        return FutureUtil.map(instFuture, (instance) -> {
            if (!instance.isSuccess()) {
                return AppSearchResult.newFailedResult(
                        instance.getResultCode(), instance.getErrorMessage());
            }
            GlobalSearchSession searchSession =
                    instance.getResultValue().doCreateGlobalSearchSession();
            return AppSearchResult.newSuccessfulResult(searchSession);
        });
    }

    /**
     * Returns the singleton instance of {@link LocalStorage}.
     *
     * <p>If the system is not initialized, it will be initialized using the provided
     * {@code context}.
     */
    @NonNull
    @VisibleForTesting
    static ListenableFuture<AppSearchResult<LocalStorage>> getInstance(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (sInstance == null) {
            synchronized (LocalStorage.class) {
                if (sInstance == null) {
                    sInstance = new LocalStorage().initialize(context);
                }
            }
        }
        return sInstance;
    }

    private LocalStorage() {}

    // NOTE: No instance of this class should be created or returned except via initialize().
    // Once the ListenableFuture returned here is populated, the class is ready to use.
    @GuardedBy("LocalStorage.class")
    private ListenableFuture<AppSearchResult<LocalStorage>> initialize(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        ResolvableFuture<AppSearchResult<LocalStorage>> future = ResolvableFuture.create();
        mExecutorService.execute(() -> {
            if (!future.isCancelled()) {

                File icingDir = new File(context.getFilesDir(), ICING_LIB_ROOT_DIR);
                try {
                    mAppSearchImpl = AppSearchImpl.create(icingDir);
                } catch (Throwable t) {
                    future.set(throwableToFailedResult(t));
                }

                future.set(newSuccessfulResult(this));
            }
        });
        return future;
    }

    AppSearchSession doCreateSearchSession(@NonNull SearchContext context) {
        return new SearchSessionImpl(mAppSearchImpl, mExecutorService, context.mDatabaseName);
    }

    GlobalSearchSession doCreateGlobalSearchSession() {
        return new GlobalSearchSessionImpl(mAppSearchImpl, mExecutorService);
    }
}
