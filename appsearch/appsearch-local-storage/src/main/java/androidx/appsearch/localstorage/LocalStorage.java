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
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchEnvironmentFactory;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

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

    /** Contains information about how to create the search session. */
    public static final class SearchContext {
        final Context mContext;
        final String mDatabaseName;
        final Executor mExecutor;
        final @Nullable AppSearchLogger mLogger;
        final boolean mPersistToDiskRecoveryProof;

        SearchContext(@NonNull Context context, @NonNull String databaseName,
                @NonNull Executor executor, @Nullable AppSearchLogger logger,
                boolean persistToDiskRecoveryProof) {
            mContext = Preconditions.checkNotNull(context);
            mDatabaseName = Preconditions.checkNotNull(databaseName);
            mExecutor = Preconditions.checkNotNull(executor);
            mLogger = logger;
            mPersistToDiskRecoveryProof = persistToDiskRecoveryProof;
        }

        /**
         * Returns the {@link Context} associated with the {@link AppSearchSession}
         */
        public @NonNull Context getContext() {
            return mContext;
        }

        /**
         * Returns the name of the database to create or open.
         */
        public @NonNull String getDatabaseName() {
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
        public @NonNull Executor getWorkerExecutor() {
            return mExecutor;
        }

        /** Builder for {@link SearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private final String mDatabaseName;
            private Executor mExecutor;
            private @Nullable AppSearchLogger mLogger;
            private boolean mPersistToDiskRecoveryProof;

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
             * @param context The context used as the parent of the created SearchContext
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
            public @NonNull Builder setWorkerExecutor(@NonNull Executor executor) {
                mExecutor = Preconditions.checkNotNull(executor);
                return this;
            }


            /**
             * Sets the custom logger used to get the details stats from AppSearch.
             *
             * <p>If no logger is provided, nothing would be returned/logged. There is no default
             * logger implementation in AppSearch.
             *
             * @exportToFramework:hide
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public @NonNull Builder setLogger(@NonNull AppSearchLogger logger) {
                mLogger = Preconditions.checkNotNull(logger);
                return this;
            }

            /**
             * Sets whether AppSearch should call persistToDisk LITE or persistToDisk RECOVERY_PROOF
             * after mutations ({@link AppSearchSession#putAsync} and
             * {@link AppSearchSession#removeAsync}). LITE guarantees no data loss on initialization
             * but with a recovery. RECOVERY_PROOF will guarantee no data loss and no recovery.
             *
             * <p>Note: This api is only added to facilitate early opt-ins by clients. It will be
             * deprecated and then deleted (with the new 'true' behavior enabled) once this change
             * has had sufficient time to soak.
             *
             * @exportToFramework:hide
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            @NonNull
            public Builder setPersistToDiskRecoveryProof(boolean persistToDiskRecoveryProof) {
                mPersistToDiskRecoveryProof = persistToDiskRecoveryProof;
                return this;
            }

            /** Builds a {@link SearchContext} instance. */
            public @NonNull SearchContext build() {
                if (mExecutor == null) {
                    mExecutor = EXECUTOR;
                }
                return new SearchContext(
                        mContext, mDatabaseName, mExecutor, mLogger, mPersistToDiskRecoveryProof);
            }
        }
    }

    /** Contains information relevant to creating a global search session. */
    public static final class GlobalSearchContext {
        final Context mContext;
        final Executor mExecutor;
        final @Nullable AppSearchLogger mLogger;

        GlobalSearchContext(@NonNull Context context, @NonNull Executor executor,
                @Nullable AppSearchLogger logger) {
            mContext = Preconditions.checkNotNull(context);
            mExecutor = Preconditions.checkNotNull(executor);
            mLogger = logger;
        }

        /**
         * Returns the {@link Context} associated with the {@link GlobalSearchSession}
         */
        public @NonNull Context getContext() {
            return mContext;
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
        public @NonNull Executor getWorkerExecutor() {
            return mExecutor;
        }

        /** Builder for {@link GlobalSearchContext} objects. */
        public static final class Builder {
            private final Context mContext;
            private Executor mExecutor;
            private @Nullable AppSearchLogger mLogger;

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
            public @NonNull Builder setWorkerExecutor(@NonNull Executor executor) {
                Preconditions.checkNotNull(executor);
                mExecutor = executor;
                return this;
            }

            /**
             * Sets the custom logger used to get the details stats from AppSearch.
             *
             * <p>If no logger is provided, nothing would be returned/logged. There is no default
             * logger implementation in AppSearch.
             *
             * @exportToFramework:hide
             */
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            public @NonNull Builder setLogger(@NonNull AppSearchLogger logger) {
                mLogger = Preconditions.checkNotNull(logger);
                return this;
            }

            /** Builds a {@link GlobalSearchContext} instance. */
            public @NonNull GlobalSearchContext build() {
                if (mExecutor == null) {
                    mExecutor = EXECUTOR;
                }
                return new GlobalSearchContext(mContext, mExecutor, mLogger);
            }
        }
    }

    // AppSearch multi-thread execution is guarded by Read & Write Lock in AppSearchImpl, all
    // mutate requests will need to gain write lock and query requests need to gain read lock.
    static final Executor EXECUTOR = AppSearchEnvironmentFactory.getEnvironmentInstance()
            .createCachedThreadPoolExecutor();

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
    public static @NonNull ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull SearchContext context) {
        Preconditions.checkNotNull(context);
        return FutureUtil.execute(context.mExecutor, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext, context.mExecutor,
                    context.mLogger, context.mPersistToDiskRecoveryProof);
            return instance.doCreateSearchSession(context);
        });
    }

    /**
     * Opens a new {@link GlobalSearchSession} on this storage.
     *
     * <p>The {@link GlobalSearchSession} opened from this {@link LocalStorage} allows the user to
     * search across all local databases within the {@link LocalStorage} of this app, however
     * cross-app search is not possible with {@link LocalStorage}.
     *
     * <p>This process requires a native search library. If it's not created, the initialization
     * process will create one.
     */
    public static @NonNull ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync(
            @NonNull GlobalSearchContext context) {
        Preconditions.checkNotNull(context);
        return FutureUtil.execute(context.mExecutor, () -> {
            LocalStorage instance = getOrCreateInstance(context.mContext, context.mExecutor,
                    context.mLogger, /*persistToDiskRecoveryProof=*/false);
            return instance.doCreateGlobalSearchSession(context);
        });
    }

    /**
     * Returns the {@link Features} to check for the availability of certain features for this
     * AppSearch storage.
     */
    @ExperimentalAppSearchApi
    public static @NonNull Features getFeatures() {
        return new FeaturesImpl();
    }

    /**
     * Returns the singleton instance of {@link LocalStorage}.
     *
     * <p>If the system is not initialized, it will be initialized using the provided
     * {@code context}.
     */
    @WorkerThread
    @VisibleForTesting
    static @NonNull LocalStorage getOrCreateInstance(
            @NonNull Context context, @NonNull Executor executor,
            @Nullable AppSearchLogger logger, boolean persistToDiskRecoveryProof)
            throws AppSearchException {
        Preconditions.checkNotNull(context);
        if (sInstance == null) {
            synchronized (LocalStorage.class) {
                if (sInstance == null) {
                    sInstance =
                            new LocalStorage(context, executor, logger, persistToDiskRecoveryProof);
                }
            }
        }
        return sInstance;
    }

    @VisibleForTesting
    static @NonNull AppSearchConfig getConfig(@NonNull SearchContext context)
            throws AppSearchException {
        LocalStorage instance = getOrCreateInstance(context.mContext, context.mExecutor,
                context.mLogger, context.mPersistToDiskRecoveryProof);
        return instance.mAppSearchImpl.getConfig();
    }

    @VisibleForTesting
    static void resetInstance() {
        sInstance = null;
    }

    @WorkerThread
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    private LocalStorage(
            @NonNull Context context,
            @NonNull Executor executor,
            @Nullable AppSearchLogger logger,
            boolean persistToDiskRecoveryProof)
            throws AppSearchException {
        Preconditions.checkNotNull(context);
        File icingDir = AppSearchEnvironmentFactory.getEnvironmentInstance()
                .getAppSearchDir(context, /* userHandle= */ null);

        long totalLatencyStartMillis = SystemClock.elapsedRealtime();
        InitializeStats.Builder initStatsBuilder = null;
        if (logger != null) {
            initStatsBuilder = new InitializeStats.Builder();
        }

        // Syncing the current logging level to Icing before creating the AppSearch object, so that
        // the correct logging level will cover the period of Icing initialization.
        AppSearchImpl.syncLoggingLevelToIcing();
        AppSearchConfig config = new AppSearchConfigImpl(
                new UnlimitedLimitConfig(),
                new LocalStorageIcingOptionsConfig(),
                /* storeParentInfoAsSyntheticProperty= */ false,
                /* shouldRetrieveParentInfo= */ true,
                persistToDiskRecoveryProof
        );
        RevocableFileDescriptorStore revocableFileDescriptorStore = null;
        if (Flags.enableBlobStore()) {
            revocableFileDescriptorStore = new JetpackRevocableFileDescriptorStore(config);
        }
        mAppSearchImpl = AppSearchImpl.create(
                icingDir,
                config,
                initStatsBuilder,
                /*callStatsBuilder=*/null,
                /*visibilityChecker=*/ null,
                revocableFileDescriptorStore,
                /*icingSearchEngine=*/ null,
                new JetpackOptimizeStrategy());

        if (logger != null) {
            initStatsBuilder.setTotalLatencyMillis(
                    (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
            logger.logStats(initStatsBuilder.build());
        }

        executor.execute(() -> {
            long totalOptimizeLatencyStartMillis = SystemClock.elapsedRealtime();
            OptimizeStats.Builder optimizeStatsBuilder = null;
            try {
                if (logger != null) {
                    optimizeStatsBuilder = new OptimizeStats.Builder();
                }
                mAppSearchImpl.checkForOptimize(optimizeStatsBuilder, /*callStatsBuilder=*/ null);
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            } finally {
                if (optimizeStatsBuilder != null) {
                    OptimizeStats oStats = optimizeStatsBuilder
                            .setTotalLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime()
                                            - totalOptimizeLatencyStartMillis))
                            .build();
                    if (logger != null && oStats.getOriginalDocumentCount() > 0) {
                        // see if optimize has been run by checking originalDocumentCount
                        logger.logStats(optimizeStatsBuilder.build());
                    }
                }
            }
        });
    }

    private @NonNull AppSearchSession doCreateSearchSession(@NonNull SearchContext context) {
        return new SearchSessionImpl(
                mAppSearchImpl,
                context.mExecutor,
                new FeaturesImpl(),
                context.mContext,
                context.mDatabaseName,
                context.mLogger);
    }

    private @NonNull GlobalSearchSession doCreateGlobalSearchSession(
            @NonNull GlobalSearchContext context) {
        return new GlobalSearchSessionImpl(mAppSearchImpl, context.mExecutor,
                new FeaturesImpl(), context.mContext, context.mLogger);
    }
}
