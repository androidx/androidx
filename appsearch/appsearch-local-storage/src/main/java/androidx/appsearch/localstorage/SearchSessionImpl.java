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

import static androidx.appsearch.app.AppSearchResult.RESULT_INTERNAL_ERROR;
import static androidx.appsearch.app.AppSearchResult.RESULT_INVALID_SCHEMA;
import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.CommitBlobResponse;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.OpenBlobForReadResponse;
import androidx.appsearch.app.OpenBlobForWriteResponse;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveBlobResponse;
import androidx.appsearch.app.RemoveByDocumentIdRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetBlobVisibilityRequest;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.stats.SchemaMigrationStats;
import androidx.appsearch.util.SchemaMigrationUtil;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.PersistType;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * An implementation of {@link AppSearchSession} which stores data locally in the app's storage
 * space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class SearchSessionImpl implements AppSearchSession {
    private static final String TAG = "AppSearchSessionImpl";

    private final AppSearchImpl mAppSearchImpl;
    private final Executor mExecutor;
    private final Features mFeatures;
    private final String mDatabaseName;
    private final @Nullable AppSearchLogger mLogger;

    private final String mPackageName;
    private final CallerAccess mSelfCallerAccess;

    private volatile boolean mIsMutated = false;
    private volatile boolean mIsClosed = false;

    SearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull Executor executor,
            @NonNull Features features,
            @NonNull Context context,
            @NonNull String databaseName,
            @Nullable AppSearchLogger logger) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutor = Preconditions.checkNotNull(executor);
        mFeatures = Preconditions.checkNotNull(features);
        Preconditions.checkNotNull(context);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mLogger = logger;

        mPackageName = context.getPackageName();
        mSelfCallerAccess = new CallerAccess(/*callingPackageName=*/mPackageName);
    }

    @Override
    public @NonNull ListenableFuture<SetSchemaResponse> setSchemaAsync(
            @NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");

        long startMillis = SystemClock.elapsedRealtime();
        long waitExecutorStartLatencyMillis = SystemClock.elapsedRealtime();
        ListenableFuture<SetSchemaResponse> future = execute(() -> {
            long waitExecutorEndLatencyMillis = SystemClock.elapsedRealtime();
            SetSchemaStats.Builder firstSetSchemaStatsBuilder = null;
            SetSchemaStats.Builder secondSetSchemaStatsBuilder = null;
            if (mLogger != null) {
                firstSetSchemaStatsBuilder = new SetSchemaStats.Builder(
                        mPackageName, mDatabaseName);
            }

            List<InternalVisibilityConfig> visibilityConfigs =
                    InternalVisibilityConfig.toInternalVisibilityConfigs(request);

            Map<String, Migrator> migrators = request.getMigrators();
            // No need to trigger migration if user never set migrator.
            if (migrators.isEmpty()) {
                SetSchemaResponse setSchemaResponse = setSchemaNoMigrations(request,
                        visibilityConfigs,
                        firstSetSchemaStatsBuilder);

                long dispatchNotificationStartTimeMillis = SystemClock.elapsedRealtime();
                // Schedule a task to dispatch change notifications. See requirements for where the
                // method is called documented in the method description.
                dispatchChangeNotifications();
                long dispatchNotificationEndTimeMillis = SystemClock.elapsedRealtime();

                // We will have only one SetSchemaStats for non-migration cases.
                if (firstSetSchemaStatsBuilder != null) {
                    firstSetSchemaStatsBuilder
                            .setTotalLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime() - startMillis))
                            .setDispatchChangeNotificationsLatencyMillis(
                                    (int) (dispatchNotificationEndTimeMillis
                                            - dispatchNotificationStartTimeMillis));
                    mLogger.logStats(firstSetSchemaStatsBuilder.build());
                }

                return setSchemaResponse;
            }

            // Migration process
            // 1. Validate and retrieve all active migrators.
            SchemaMigrationStats.Builder schemaMigrationStatsBuilder = null;
            if (mLogger != null) {
                schemaMigrationStatsBuilder = new SchemaMigrationStats.Builder(mPackageName,
                        mDatabaseName);
            }
            long getSchemaLatencyStartTimeMillis = SystemClock.elapsedRealtime();
            GetSchemaResponse getSchemaResponse = mAppSearchImpl.getSchema(
                    mPackageName, mDatabaseName, mSelfCallerAccess);
            long getSchemaLatencyEndTimeMillis = SystemClock.elapsedRealtime();
            int currentVersion = getSchemaResponse.getVersion();
            int finalVersion = request.getVersion();
            Map<String, Migrator> activeMigrators = SchemaMigrationUtil.getActiveMigrators(
                    getSchemaResponse.getSchemas(), migrators, currentVersion, finalVersion);
            // No need to trigger migration if no migrator is active.
            if (activeMigrators.isEmpty()) {
                SetSchemaResponse setSchemaResponse = setSchemaNoMigrations(request,
                        visibilityConfigs, firstSetSchemaStatsBuilder);
                if (firstSetSchemaStatsBuilder != null) {
                    firstSetSchemaStatsBuilder.setTotalLatencyMillis(
                            (int) (SystemClock.elapsedRealtime() - startMillis));
                    mLogger.logStats(firstSetSchemaStatsBuilder.build());
                }
                return setSchemaResponse;
            }

            // 2. SetSchema with forceOverride=false, to retrieve the list of incompatible/deleted
            // types.
            long firstSetSchemaLatencyStartMillis = SystemClock.elapsedRealtime();
            if (firstSetSchemaStatsBuilder != null) {
                firstSetSchemaStatsBuilder.setSchemaMigrationCallType(
                        SchemaMigrationStats.FIRST_CALL_GET_INCOMPATIBLE);
            }
            InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                    mPackageName,
                    mDatabaseName,
                    new ArrayList<>(request.getSchemas()),
                    visibilityConfigs,
                    /*forceOverride=*/false,
                    request.getVersion(),
                    firstSetSchemaStatsBuilder);
            long firstSetSchemaLatencyEndTimeMillis = SystemClock.elapsedRealtime();
            if (schemaMigrationStatsBuilder != null) {
                schemaMigrationStatsBuilder
                        .setIsFirstSetSchemaSuccess(internalSetSchemaResponse.isSuccess());
            }

            // 3. If forceOverride is false, check that all incompatible types will be migrated.
            // If some aren't we must throw an error, rather than proceeding and deleting those
            // types.
            long queryAndTransformLatencyStartMillis = SystemClock.elapsedRealtime();
            SchemaMigrationUtil.checkDeletedAndIncompatibleAfterMigration(
                    internalSetSchemaResponse, activeMigrators.keySet());

            try (AppSearchMigrationHelper migrationHelper = new AppSearchMigrationHelper(
                    mAppSearchImpl, mPackageName, mDatabaseName, request.getSchemas())) {
                // 4. Trigger migration for all activity migrators.
                migrationHelper.queryAndTransform(activeMigrators, currentVersion, finalVersion,
                        schemaMigrationStatsBuilder);
                long queryAndTransformLatencyEndTimeMillis = SystemClock.elapsedRealtime();

                // 5. SetSchema a second time with forceOverride=true if the first attempted failed
                // due to backward incompatible changes.
                long secondSetSchemaLatencyStartMillis = SystemClock.elapsedRealtime();
                if (!internalSetSchemaResponse.isSuccess()) {
                    if (mLogger != null) {
                        // Create a new stats builder for the second set schema call.
                        secondSetSchemaStatsBuilder =
                                new SetSchemaStats.Builder(mPackageName, mDatabaseName)
                                        .setSchemaMigrationCallType(
                                                SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA);
                    }
                    internalSetSchemaResponse = mAppSearchImpl.setSchema(
                            mPackageName,
                            mDatabaseName,
                            new ArrayList<>(request.getSchemas()),
                            visibilityConfigs,
                            /*forceOverride=*/ true,
                            request.getVersion(),
                            secondSetSchemaStatsBuilder);
                    if (!internalSetSchemaResponse.isSuccess()) {
                        // Impossible case, we just set forceOverride to be true, we should never
                        // fail in incompatible changes. And all other cases should failed during
                        // the first call.
                        throw new AppSearchException(RESULT_INTERNAL_ERROR,
                                internalSetSchemaResponse.getErrorMessage());
                    }
                }
                long secondSetSchemaLatencyEndTimeMillis = SystemClock.elapsedRealtime();
                SetSchemaResponse.Builder responseBuilder = new SetSchemaResponse.Builder(
                        internalSetSchemaResponse.getSetSchemaResponse())
                        .addMigratedTypes(activeMigrators.keySet());
                mIsMutated = true;

                // 6. Put all the migrated documents into the index, now that the new schema is set.
                long saveDocumentLatencyStartMillis = SystemClock.elapsedRealtime();
                SetSchemaResponse finalSetSchemaResponse =
                        migrationHelper.readAndPutDocuments(responseBuilder,
                                schemaMigrationStatsBuilder);
                long saveDocumentLatencyEndMillis = SystemClock.elapsedRealtime();

                // Schedule a task to dispatch change notifications. See requirements for where the
                // method is called documented in the method description.
                long dispatchNotificationStartTimeMillis = SystemClock.elapsedRealtime();
                dispatchChangeNotifications();
                long dispatchNotificationEndTimeMillis = SystemClock.elapsedRealtime();

                long endMillis = SystemClock.elapsedRealtime();
                if (firstSetSchemaStatsBuilder != null) {
                    firstSetSchemaStatsBuilder
                            .setExecutorAcquisitionLatencyMillis(
                                    (int) (waitExecutorEndLatencyMillis
                                            - waitExecutorStartLatencyMillis))
                            .setTotalLatencyMillis(
                                    (int) (firstSetSchemaLatencyEndTimeMillis
                                            - firstSetSchemaLatencyStartMillis))
                            .setDispatchChangeNotificationsLatencyMillis(
                                    (int) (dispatchNotificationEndTimeMillis
                                            - dispatchNotificationStartTimeMillis));
                    mLogger.logStats(firstSetSchemaStatsBuilder.build());
                }
                if (secondSetSchemaStatsBuilder != null) {
                    secondSetSchemaStatsBuilder
                            .setExecutorAcquisitionLatencyMillis(
                                    (int) (waitExecutorEndLatencyMillis
                                            - waitExecutorStartLatencyMillis))
                            .setTotalLatencyMillis(
                                    (int) (secondSetSchemaLatencyEndTimeMillis
                                            - secondSetSchemaLatencyStartMillis))
                            .setDispatchChangeNotificationsLatencyMillis(
                                    (int) (dispatchNotificationEndTimeMillis
                                            - dispatchNotificationStartTimeMillis));
                    mLogger.logStats(secondSetSchemaStatsBuilder.build());
                }
                if (schemaMigrationStatsBuilder != null) {
                    schemaMigrationStatsBuilder
                            .setExecutorAcquisitionLatencyMillis(
                                    (int) (waitExecutorEndLatencyMillis
                                            - waitExecutorStartLatencyMillis))
                            .setGetSchemaLatencyMillis(
                                    (int) (getSchemaLatencyEndTimeMillis
                                            - getSchemaLatencyStartTimeMillis))
                            .setQueryAndTransformLatencyMillis(
                                    (int) (queryAndTransformLatencyEndTimeMillis
                                            - queryAndTransformLatencyStartMillis))
                            .setSaveDocumentLatencyMillis(
                                    (int) (saveDocumentLatencyEndMillis
                                            - saveDocumentLatencyStartMillis))
                            .setFirstSetSchemaLatencyMillis(
                                    (int) (firstSetSchemaLatencyEndTimeMillis
                                            - firstSetSchemaLatencyStartMillis))
                            .setSecondSetSchemaLatencyMillis(
                                    (int) (secondSetSchemaLatencyEndTimeMillis
                                            - secondSetSchemaLatencyStartMillis))
                            .setTotalLatencyMillis(
                                    (int) (endMillis - startMillis));
                    mLogger.logStats(schemaMigrationStatsBuilder.build());
                }
                return finalSetSchemaResponse;
            }
        });

        // setSchema will sync the schemas in the request to AppSearch, any existing schemas which
        // is not included in the request will be delete if we force override incompatible schemas.
        // And all documents of these types will be deleted as well. We should checkForOptimize for
        // these deletion.
        checkForOptimize();
        return future;
    }

    @Override
    public @NonNull ListenableFuture<GetSchemaResponse> getSchemaAsync() {
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(
                () -> mAppSearchImpl.getSchema(mPackageName, mDatabaseName, mSelfCallerAccess));
    }

    @Override
    public @NonNull ListenableFuture<Set<String>> getNamespacesAsync() {
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            List<String> namespaces = mAppSearchImpl.getNamespaces(mPackageName, mDatabaseName);
            return new ArraySet<>(namespaces);
        });
    }

    @Override
    public @NonNull ListenableFuture<AppSearchBatchResult<String, Void>> putAsync(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");

        List<GenericDocument> documents = request.getGenericDocuments();
        List<GenericDocument> takenActions = request.getTakenActionGenericDocuments();

        ListenableFuture<AppSearchBatchResult<String, Void>> future = execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();

            // Normal documents.
            mAppSearchImpl.batchPutDocuments(
                    mPackageName,
                    mDatabaseName,
                    documents,
                    resultBuilder,
                    /*sendChangeNotifications=*/ true,
                    mLogger,
                    // PersistToDisk is not necessary to call here, since it will be called in
                    // the next batch put request below.
                    PersistType.Code.UNKNOWN);

            // TakenAction documents.
            mAppSearchImpl.batchPutDocuments(
                    mPackageName,
                    mDatabaseName,
                    takenActions,
                    resultBuilder,
                    /*sendChangeNotifications=*/ true,
                    mLogger,
                    // Persist the newly written data.
                    mAppSearchImpl.getConfig().getLightweightPersistType());

            mIsMutated = true;

            // Schedule a task to dispatch change notifications. See requirements for where the
            // method is called documented in the method description.
            dispatchChangeNotifications();

            return resultBuilder.build();
        });

        // The existing documents with same ID will be deleted, so there may be some resources that
        // could be released after optimize().
        checkForOptimize(/*mutateBatchSize=*/ documents.size() + takenActions.size());
        return future;
    }

    @Override
    public @NonNull ListenableFuture<AppSearchBatchResult<String, GenericDocument>>
            getByDocumentIdAsync(@NonNull GetByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() ->
                mAppSearchImpl.batchGetDocuments(
                        mPackageName, mDatabaseName, request, /*callerAccess=*/ null));
    }

    @Override
    @ExperimentalAppSearchApi
    // TODO(b/273591938) support change notification for put blob.
    public @NonNull ListenableFuture<OpenBlobForWriteResponse>
            openBlobForWriteAsync(@NonNull Set<AppSearchBlobHandle> handles) {
        Preconditions.checkNotNull(handles);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (AppSearchBlobHandle handle : handles) {
                try {
                    // We pass the caller mPackageName and mDatabaseName to AppSearchImpl and let it
                    // to compare with given handle to reduce code export delta.
                    ParcelFileDescriptor pfd =
                            mAppSearchImpl.openWriteBlob(mPackageName, mDatabaseName, handle);
                    resultBuilder.setSuccess(handle, pfd);
                } catch (Throwable t) {
                    resultBuilder.setResult(handle, throwableToFailedResult(t));
                }
            }
            return new OpenBlobForWriteResponse(resultBuilder.build());
        });
    }

    @Override
    @ExperimentalAppSearchApi
    public @NonNull ListenableFuture<RemoveBlobResponse> removeBlobAsync(
            @NonNull Set<AppSearchBlobHandle> handles) {
        Preconditions.checkNotNull(handles);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<AppSearchBlobHandle, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (AppSearchBlobHandle handle : handles) {
                try {
                    mAppSearchImpl.removeBlob(mPackageName, mDatabaseName, handle);
                    resultBuilder.setSuccess(handle, null);
                } catch (Throwable t) {
                    resultBuilder.setResult(handle, throwableToFailedResult(t));
                }
            }
            return new RemoveBlobResponse(resultBuilder.build());
        });
    }

    @Override
    @ExperimentalAppSearchApi
    // TODO(b/273591938) support change notification for put blob.
    public @NonNull ListenableFuture<CommitBlobResponse> commitBlobAsync(
            @NonNull Set<AppSearchBlobHandle> handles) {
        Preconditions.checkNotNull(handles);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<AppSearchBlobHandle, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (AppSearchBlobHandle handle : handles) {
                try {
                    // We pass the caller mPackageName and mDatabaseName to AppSearchImpl and let it
                    // to compare with given handle to reduce code export delta.
                    mAppSearchImpl.commitBlob(mPackageName, mDatabaseName, handle);
                    resultBuilder.setSuccess(handle, null);
                } catch (Throwable t) {
                    resultBuilder.setResult(handle, throwableToFailedResult(t));
                }
            }
            return new CommitBlobResponse(resultBuilder.build());
        });
    }


    @Override
    @ExperimentalAppSearchApi
    public @NonNull ListenableFuture<OpenBlobForReadResponse> openBlobForReadAsync(
            @NonNull Set<AppSearchBlobHandle> handles) {
        Preconditions.checkNotNull(handles);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (AppSearchBlobHandle handle : handles) {
                try {
                    // We pass the caller mPackageName and mDatabaseName to AppSearchImpl and let it
                    // to compare with given handle to reduce code export delta.
                    ParcelFileDescriptor pfd =
                            mAppSearchImpl.openReadBlob(mPackageName, mDatabaseName, handle);
                    resultBuilder.setSuccess(handle, pfd);
                } catch (Throwable t) {
                    resultBuilder.setResult(handle, throwableToFailedResult(t));
                }
            }
            return new OpenBlobForReadResponse(resultBuilder.build());
        });
    }

    @Override
    @ExperimentalAppSearchApi
    public @NonNull ListenableFuture<Void> setBlobVisibilityAsync(
            @NonNull SetBlobVisibilityRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            List<InternalVisibilityConfig> visibilityConfigs =
                    InternalVisibilityConfig.toInternalVisibilityConfigs(request);
            mAppSearchImpl.setBlobNamespaceVisibility(
                    mPackageName,
                    mDatabaseName,
                    visibilityConfigs);
            mIsMutated = true;
            return null;
        });
    }

    @Override
    public @NonNull SearchResults search(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutor,
                mPackageName,
                mDatabaseName,
                queryExpression,
                searchSpec,
                mLogger);
    }

    @Override
    public @NonNull ListenableFuture<List<SearchSuggestionResult>> searchSuggestionAsync(
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec) {
        Preconditions.checkNotNull(suggestionQueryExpression);
        Preconditions.checkStringNotEmpty(suggestionQueryExpression);
        Preconditions.checkNotNull(searchSuggestionSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> mAppSearchImpl.searchSuggestion(
                mPackageName,
                mDatabaseName,
                suggestionQueryExpression,
                searchSuggestionSpec));
    }

    @Override
    public @NonNull ListenableFuture<Void> reportUsageAsync(@NonNull ReportUsageRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            mAppSearchImpl.reportUsage(
                    mPackageName,
                    mDatabaseName,
                    request.getNamespace(),
                    request.getDocumentId(),
                    request.getUsageTimestampMillis(),
                    /*systemUsage=*/ false);
            mIsMutated = true;
            return null;
        });
    }

    @Override
    public @NonNull ListenableFuture<AppSearchBatchResult<String, Void>> removeAsync(
            @NonNull RemoveByDocumentIdRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        ListenableFuture<AppSearchBatchResult<String, Void>> future = execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (String id : request.getIds()) {
                RemoveStats.Builder removeStatsBuilder = null;
                if (mLogger != null) {
                    removeStatsBuilder = new RemoveStats.Builder(mPackageName, mDatabaseName);
                }

                try {
                    mAppSearchImpl.remove(mPackageName, mDatabaseName, request.getNamespace(), id,
                            removeStatsBuilder);
                    resultBuilder.setSuccess(id, /*value=*/null);
                } catch (Throwable t) {
                    resultBuilder.setResult(id, throwableToFailedResult(t));
                } finally {
                    if (mLogger != null) {
                        mLogger.logStats(removeStatsBuilder.build());
                    }
                }
            }
            // Now that the batch has been written. Persist the newly written data.
            mAppSearchImpl.persistToDisk(mAppSearchImpl.getConfig().getLightweightPersistType());
            mIsMutated = true;
            // Schedule a task to dispatch change notifications. See requirements for where the
            // method is called documented in the method description.
            dispatchChangeNotifications();
            return resultBuilder.build();
        });
        checkForOptimize(/*mutateBatchSize=*/ request.getIds().size());
        return future;
    }

    @Override
    public @NonNull ListenableFuture<Void> removeAsync(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);

        if (searchSpec.getJoinSpec() != null) {
            throw new IllegalArgumentException("JoinSpec not allowed in removeByQuery, but "
                    + "JoinSpec was provided.");
        }

        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        ListenableFuture<Void> future = execute(() -> {
            RemoveStats.Builder removeStatsBuilder = null;
            if (mLogger != null) {
                removeStatsBuilder = new RemoveStats.Builder(mPackageName, mDatabaseName);
            }
            mAppSearchImpl.removeByQuery(mPackageName, mDatabaseName, queryExpression,
                    searchSpec, removeStatsBuilder);
            // Now that the batch has been written. Persist the newly written data.
            mAppSearchImpl.persistToDisk(mAppSearchImpl.getConfig().getLightweightPersistType());
            mIsMutated = true;
            // Schedule a task to dispatch change notifications. See requirements for where the
            // method is called documented in the method description.
            dispatchChangeNotifications();
            if (mLogger != null) {
                mLogger.logStats(removeStatsBuilder.build());
            }
            return null;
        });
        checkForOptimize();
        return future;
    }

    @Override
    @ExperimentalAppSearchApi
    public @NonNull ListenableFuture<StorageInfo> getStorageInfoAsync() {
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> mAppSearchImpl.getStorageInfoForDatabase(mPackageName, mDatabaseName));
    }

    @Override
    public @NonNull ListenableFuture<Void> requestFlushAsync() {
        return execute(() -> {
            mAppSearchImpl.persistToDisk(PersistType.Code.FULL);
            return null;
        });
    }

    @Override
    public @NonNull Features getFeatures() {
        return mFeatures;
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        if (mIsMutated && !mIsClosed) {
            // No future is needed here since the method is void.
            FutureUtil.execute(mExecutor, () -> {
                mAppSearchImpl.persistToDisk(PersistType.Code.FULL);
                mIsClosed = true;
                return null;
            });
        }
    }

    private <T> ListenableFuture<T> execute(Callable<T> callable) {
        return FutureUtil.execute(mExecutor, callable);
    }

    /**
     * Set schema to Icing for no-migration scenario.
     *
     * <p>We only need one time {@link #setSchemaAsync} call for no-migration scenario by using the
     * forceoverride in the request.
     */
    private SetSchemaResponse setSchemaNoMigrations(@NonNull SetSchemaRequest request,
            @NonNull List<InternalVisibilityConfig> visibilityConfigs,
            SetSchemaStats.@Nullable Builder setSchemaStatsBuilder)
            throws AppSearchException {
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setSchemaMigrationCallType(SchemaMigrationStats.NO_MIGRATION);
        }
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                mPackageName,
                mDatabaseName,
                new ArrayList<>(request.getSchemas()),
                visibilityConfigs,
                request.isForceOverride(),
                request.getVersion(),
                setSchemaStatsBuilder);
        if (!internalSetSchemaResponse.isSuccess()) {
            // check is the set schema call failed because incompatible changes.
            // That's the only case we swallowed in the AppSearchImpl#setSchema().
            throw new AppSearchException(RESULT_INVALID_SCHEMA,
                    internalSetSchemaResponse.getErrorMessage());
        }
        mIsMutated = true;
        return internalSetSchemaResponse.getSetSchemaResponse();
    }

    /**
     * Dispatches change notifications if there are any to dispatch.
     *
     * <p>This method is async; notifications are dispatched onto their own registered executors.
     *
     * <p>IMPORTANT: You must always call this within the background task that contains the
     * operation that mutated the index. If you called it outside of that task, it could start
     * before the task completes, causing notifications to be missed.
     */
    @WorkerThread
    private void dispatchChangeNotifications() {
        mAppSearchImpl.dispatchAndClearChangeNotifications();
    }

    private void checkForOptimize(int mutateBatchSize) {
        mExecutor.execute(() -> {
            long totalLatencyStartMillis = SystemClock.elapsedRealtime();
            OptimizeStats.Builder builder = null;
            try {
                if (mLogger != null) {
                    builder = new OptimizeStats.Builder();
                }
                mAppSearchImpl.checkForOptimize(mutateBatchSize, builder);
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            } finally {
                if (builder != null) {
                    OptimizeStats oStats = builder
                            .setTotalLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis))
                            .build();
                    if (mLogger != null && oStats.getOriginalDocumentCount() > 0) {
                        // see if optimize has been run by checking originalDocumentCount
                        mLogger.logStats(oStats);
                    }
                }
            }
        });
    }

    private void checkForOptimize() {
        mExecutor.execute(() -> {
            long totalLatencyStartMillis = SystemClock.elapsedRealtime();
            OptimizeStats.Builder builder = null;
            try {
                if (mLogger != null) {
                    builder = new OptimizeStats.Builder();
                }
                mAppSearchImpl.checkForOptimize(builder);
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            } finally {
                if (builder != null) {
                    OptimizeStats oStats = builder
                            .setTotalLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis))
                            .build();
                    if (mLogger != null && oStats.getOriginalDocumentCount() > 0) {
                        // see if optimize has been run by checking originalDocumentCount
                        mLogger.logStats(oStats);
                    }
                }
            }
        });
    }
}
