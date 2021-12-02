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
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.platformstorage.converter.SearchContextToPlatformConverter;
import androidx.concurrent.futures.ResolvableFuture;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An AppSearch storage system which stores data in the central AppSearch service, available on
 * Android S+.
 */
@RequiresApi(Build.VERSION_CODES.S)
public final class PlatformStorage {

    private PlatformStorage() {
    }

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

    /** Contains information relevant to creating a global search session. */
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

    // Never call Executor.shutdownNow(), it will cancel the futures it's returned. And since
    // execute() won't return anything, we will hang forever waiting for the execution.
    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    static final Executor EXECUTOR = Executors.newCachedThreadPool();

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
                context.mExecutor,
                result -> {
                    if (result.isSuccess()) {
                        future.set(
                                new SearchSessionImpl(result.getResultValue(), context.mExecutor,
                                new CapabilitiesImpl()));
                    } else {
                        future.setException(
                                new AppSearchException(
                                        result.getResultCode(), result.getErrorMessage()));
                    }
                });
        return future;
    }

    /**
     * Opens a new {@link GlobalSearchSession} on this storage.
     */
    @NonNull
    public static ListenableFuture<GlobalSearchSession> createGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        AppSearchManager appSearchManager =
                context.mContext.getSystemService(AppSearchManager.class);
        ResolvableFuture<GlobalSearchSession> future = ResolvableFuture.create();
        appSearchManager.createGlobalSearchSession(
                context.mExecutor,
                result -> {
                    if (result.isSuccess()) {
                        future.set(new GlobalSearchSessionImpl(
                                result.getResultValue(), context.mExecutor,
                                new CapabilitiesImpl()));
                    } else {
                        future.setException(
                                new AppSearchException(
                                        result.getResultCode(), result.getErrorMessage()));
                    }
                });
        return future;
    }
}
