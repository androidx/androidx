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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An AppSearch storage system which stores data locally in the app's storage space using a bundled
 * version of the search native library.
 *
 * <p>The search native library is an on-device searching library that allows apps to define
 * {@link androidx.appsearch.app.AppSearchSchema}s, save and query a variety of
 * {@link Document}s. The library needs to be initialized
 * before using, which will create a folder to save data in the app's storage space.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
public class LocalStorage {
    private static final String TAG = "AppSearchLocalStorage";

    private static final String ICING_LIB_ROOT_DIR = "appsearch";

    /** Contains information about how to create the search session. */
    public static final class SearchContext {
        final Context mContext;
        final String mDatabaseName;
        final Executor mExecutor;

        SearchContext(@NonNull Context context, @NonNull String databaseName,
                @NonNull Executor executor) {
            mContext = Preconditions.checkNotNull(context);
            mDatabaseName = Preconditions.checkNotNull(databaseName);
            mExecutor = Preconditions.checkNotNull(executor);
        }

        /**
         * Returns the name of the database to create or open.
         */
        @NonNull
        public String getDatabaseName() {
            return mDatabaseName;
        }

        /**
         * Returns the worker executor associated with {@link AppSearchSession}.
         *
         * <p>If an executor is not provided to {@link Builder}, the AppSearch default executor will
         * be returned. You should never cast the executor to
         * {@link java.util.concurrent.ExecutorService} and call
         * {@link ExecutorService#shutdownNow()}. It will cancel the futures it's returned. And
         * since {@link Executor#execute} won't return anything, we will hang forever waiting for
         * the execution.
         */
        @NonNull
        public Executor getWorkerExecutor() {
            return mExecutor;
        }

        /** Builder for {@link SearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private final String mDatabaseName;
            private Executor mExecutor;

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

            /**
             * Sets the worker executor associated with {@link AppSearchSession}.
             *
             * <p>If an executor is not provided, the AppSearch default executor will be used.
             *
             * @param executor the worker executor used to run heavy background tasks.
             */
            @NonNull
            public Builder setWorkerExecutor(@NonNull Executor executor) {
                mExecutor = Preconditions.checkNotNull(executor);
                return this;
            }

            /** Builds a {@link SearchContext} instance. */
            @NonNull
            public SearchContext build() {
                if (mExecutor == null) {
                    mExecutor = EXECUTOR;
                }
                return new SearchContext(mContext, mDatabaseName, mExecutor);
            }
        }
    }

    /**
     * Contains information relevant to creating a global search session.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class GlobalSearchContext {
        final Context mContext;
        final Executor mExecutor;

        GlobalSearchContext(@NonNull Context context, @NonNull Executor executor) {
            mContext = Preconditions.checkNotNull(context);
            mExecutor = Preconditions.checkNotNull(executor);
        }

        /**
         * Returns the worker executor associated with {@link GlobalSearchSession}.
         *
         * <p>If an executor is not provided to {@link Builder}, the AppSearch default executor will
         * be returned. You should never cast the executor to
         * {@link java.util.concurrent.ExecutorService} and call
         * {@link ExecutorService#shutdownNow()}. It will cancel the futures it's returned. And
         * since {@link Executor#execute} won't return anything, we will hang forever waiting for
         * the execution.
         */
        @NonNull
        public Executor getWorkerExecutor() {
            return mExecutor;
        }

        /** Builder for {@link GlobalSearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private Executor mExecutor;

            public Builder(@NonNull Context context) {
                mContext = Preconditions.checkNotNull(context);
            }

            /**
             * Sets the worker executor associated with {@link GlobalSearchSession}.
             *
             * <p>If an executor is not provided, the AppSearch default executor will be used.
             *
             * @param executor the worker executor used to run heavy background tasks.
             */
            @NonNull
            public Builder setWorkerExecutor(@NonNull Executor executor) {
                Preconditions.checkNotNull(executor);
                mExecutor = executor;
                return this;
            }

            /** Builds a {@link GlobalSearchContext} instance. */
            @NonNull
            public GlobalSearchContext build() {
                if (mExecutor == null) {
                    mExecutor = EXECUTOR;
                }
                return new GlobalSearchContext(mContext, mExecutor);
            }
        }
    }

    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    static final Executor EXECUTOR = Executors.newCachedThreadPool();

    private static volatile LocalStorage sInstance;

    private final AppSearchImpl mAppSearchImpl;

    /**
     * Opens a new {@link AppSearchSession} on this storage with executor.
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
        return FutureUtil.execute(context.mExecutor, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext, context.mExecutor);
            return instance.doCreateSearchSession(context);
        });
    }

    /**
     * Opens a new {@link GlobalSearchSession} on this storage.
     *
     * <p>This process requires a native search library. If it's not created, the initialization
     * process will create one.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public static ListenableFuture<GlobalSearchSession> createGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        return FutureUtil.execute(context.mExecutor, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext, context.mExecutor);
            return instance.doCreateGlobalSearchSession(context);
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
    static LocalStorage getOrCreateInstance(@NonNull Context context, @NonNull Executor executor)
            throws AppSearchException {
        Preconditions.checkNotNull(context);
        if (sInstance == null) {
            synchronized (LocalStorage.class) {
                if (sInstance == null) {
                    sInstance = new LocalStorage(context, executor);
                }
            }
        }
        return sInstance;
    }

    @WorkerThread
    private LocalStorage(@NonNull Context context, @NonNull Executor executor)
            throws AppSearchException {
        Preconditions.checkNotNull(context);
        File icingDir = new File(context.getFilesDir(), ICING_LIB_ROOT_DIR);

        // There is no global querier for a local storage instance.
        mAppSearchImpl = AppSearchImpl.create(icingDir, context, VisibilityStore.NO_OP_USER_ID,
                /*logger=*/ null, new JetpackOptimizeStrategy());

        executor.execute(() -> {
            try {
                mAppSearchImpl.checkForOptimize();
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            }
        });
    }

    @NonNull
    private AppSearchSession doCreateSearchSession(@NonNull SearchContext context) {
        return new SearchSessionImpl(mAppSearchImpl, context.mExecutor,
                context.mContext.getPackageName(), context.mDatabaseName, /*logger=*/ null);
    }

    @NonNull
    private GlobalSearchSession doCreateGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        return new GlobalSearchSessionImpl(mAppSearchImpl, context.mExecutor, context.mContext);
    }
}
