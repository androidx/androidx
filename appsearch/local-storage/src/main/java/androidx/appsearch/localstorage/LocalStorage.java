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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.FutureUtil;
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
    /**
     * The default empty database name.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public static final String DEFAULT_DATABASE_NAME = "";

    private static final String ICING_LIB_ROOT_DIR = "appsearch";

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
             * <p>If not specified, the database name is set to an empty string.
             *
             * @param databaseName The name of the database.
             * @throws IllegalArgumentException if the databaseName contains {@code '/'}.
             * @throws IllegalStateException if the builder has already been used.
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
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static volatile LocalStorage sInstance;

    private final AppSearchImpl mAppSearchImpl;

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
    public static ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull SearchContext context) {
        Preconditions.checkNotNull(context);
        return createSearchSession(context, EXECUTOR_SERVICE);
    }

    /**
     * Opens a new {@link AppSearchSession} on this storage with executor.
     *
     * <p>This process requires a native search library. If it's not created, the initialization
     * process will create one.
     *
     * @param context  The {@link SearchContext} contains all information to create a new
     *                 {@link AppSearchSession}
     * @param executor The executor of where tasks will execute.
     * @hide
     */
    @NonNull
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static ListenableFuture<AppSearchSession> createSearchSession(
            @NonNull SearchContext context, @NonNull ExecutorService executor) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(executor);
        return FutureUtil.execute(executor, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext);
            return instance.doCreateSearchSession(context, executor);
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
    public static ListenableFuture<GlobalSearchSession> createGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        return FutureUtil.execute(EXECUTOR_SERVICE, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext);
            return instance.doCreateGlobalSearchSession(EXECUTOR_SERVICE);
        });
    }

    /**
     * Returns the singleton instance of {@link LocalStorage}.
     *
     * <p>If the system is not initialized, it will be initialized using the provided
     * {@code context}.
     */
    @NonNull
    @WorkerThread
    @VisibleForTesting
    static LocalStorage getOrCreateInstance(@NonNull Context context) throws AppSearchException {
        Preconditions.checkNotNull(context);
        if (sInstance == null) {
            synchronized (LocalStorage.class) {
                if (sInstance == null) {
                    sInstance = new LocalStorage(context);
                }
            }
        }
        return sInstance;
    }

    @WorkerThread
    private LocalStorage(@NonNull Context context) throws AppSearchException {
        Preconditions.checkNotNull(context);
        File icingDir = new File(context.getFilesDir(), ICING_LIB_ROOT_DIR);
        mAppSearchImpl = AppSearchImpl.create(icingDir);
    }

    @NonNull
    private AppSearchSession doCreateSearchSession(@NonNull SearchContext context,
            @NonNull ExecutorService executor) {
        return new SearchSessionImpl(mAppSearchImpl, executor,
                context.mContext.getPackageName(), context.mDatabaseName);
    }

    @NonNull
    private GlobalSearchSession doCreateGlobalSearchSession(@NonNull ExecutorService executor) {
        return new GlobalSearchSessionImpl(mAppSearchImpl, executor);
    }
}
