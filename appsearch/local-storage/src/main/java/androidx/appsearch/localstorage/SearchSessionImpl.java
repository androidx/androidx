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

import static androidx.appsearch.app.AppSearchResult.throwableToFailedResult;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchBatchResult;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByUriRequest;
import androidx.appsearch.app.Migrator;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.FutureUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * An implementation of {@link AppSearchSession} which stores data locally
 * in the app's storage space using a bundled version of the search native library.
 *
 * <p>Queries are executed multi-threaded, but a single thread is used for mutate requests (put,
 * delete, etc..).
 */
class SearchSessionImpl implements AppSearchSession {
    private static final String TAG = "AppSearchSessionImpl";
    private final AppSearchImpl mAppSearchImpl;
    private final ExecutorService mExecutorService;
    private final Context mContext;
    private final String mPackageName;
    private final String mDatabaseName;
    private volatile boolean mIsMutated = false;
    private volatile boolean mIsClosed = false;

    SearchSessionImpl(
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull ExecutorService executorService,
            @NonNull Context context,
            @NonNull String packageName,
            @NonNull String databaseName) {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mExecutorService = Preconditions.checkNotNull(executorService);
        mContext = Preconditions.checkNotNull(context);
        mPackageName = packageName;
        mDatabaseName = Preconditions.checkNotNull(databaseName);
    }

    @Override
    @NonNull
    // TODO(b/151178558) return the batch result for migration documents.
    public ListenableFuture<SetSchemaResponse> setSchema(
            @NonNull SetSchemaRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");

        ListenableFuture<SetSchemaResponse> future = execute(() -> {
            // Convert the inner set into a List since Binder can't handle Set.
            Map<String, Set<PackageIdentifier>> schemasPackageAccessible =
                    request.getSchemasVisibleToPackagesInternal();
            Map<String, List<PackageIdentifier>> copySchemasPackageAccessible = new ArrayMap<>();
            for (Map.Entry<String, Set<PackageIdentifier>> entry :
                    schemasPackageAccessible.entrySet()) {
                copySchemasPackageAccessible.put(entry.getKey(),
                        new ArrayList<>(entry.getValue()));
            }

            Map<String, Migrator> migratorMap = request.getMigrators();

            // No need to trigger migration if user never set migrator.
            if (migratorMap.size() == 0) {
                return setSchemaNoMigrations(request, copySchemasPackageAccessible);
            }

            // Migration process
            // 1. Generate the current and the final version map.
            List<AppSearchSchema> schemas = mAppSearchImpl.getSchema(mPackageName, mDatabaseName);
            Map<String, Integer> currentVersionMap = new ArrayMap<>(schemas.size());
            for (int i = 0; i < schemas.size(); i++) {
                currentVersionMap.put(schemas.get(i).getSchemaType(),
                        schemas.get(i).getVersion());
            }
            Map<String, Integer> finalVersionMap = new ArrayMap<>(request.getSchemas().size());
            for (AppSearchSchema newSchema : request.getSchemas()) {
                finalVersionMap.put(newSchema.getSchemaType(), newSchema.getVersion());
            }

            // 2. SetSchema with forceOverride=false, to retrieve the list of incompatible/deleted
            // types.
            SetSchemaResponse setSchemaResponse = mAppSearchImpl.setSchema(
                    mPackageName,
                    mDatabaseName,
                    new ArrayList<>(request.getSchemas()),
                    new ArrayList<>(request.getSchemasNotDisplayedBySystem()),
                    copySchemasPackageAccessible,
                    /*forceOverride=*/false);

            // 3. If forceOverride is false, check that all incompatible types will be migrated.
            // If some aren't we must throw an error, rather than proceeding and deleting those
            // types.
            if (!request.isForceOverride()) {
                Set<String> unmigratedTypes = getUnmigratedIncompatibleTypes(
                        setSchemaResponse.getIncompatibleTypes(),
                        migratorMap,
                        currentVersionMap,
                        finalVersionMap);
                // check if there are any unmigrated types or deleted types. If there are, we will
                // throw an exception. That's the only case we swallowed in the
                // AppSearchImpl#setSchema().
                // Since the force override is false, the schema will not have been set if there are
                // any incompatible or deleted types.
                checkDeletedAndIncompatible(setSchemaResponse.getDeletedTypes(),
                        unmigratedTypes);
            }

            try (AppSearchMigrationHelper migrationHelper =
                         new AppSearchMigrationHelper(mAppSearchImpl, currentVersionMap,
                                 finalVersionMap, mPackageName, mDatabaseName)) {
                // 4. Trigger migration for all migrators.
                List<String> migratedTypes = new ArrayList<>();
                for (Map.Entry<String, Migrator> entry : migratorMap.entrySet()) {
                    if (triggerMigration(/*schemaType=*/entry.getKey(),
                            /*migrator=*/entry.getValue(), currentVersionMap, finalVersionMap,
                            migrationHelper)) {
                        migratedTypes.add(/*migratedType=*/entry.getKey());
                    }
                }

                // 5. SetSchema a second time with forceOverride=true if the first attempted failed
                // due to backward incompatible changes.
                if (!setSchemaResponse.getIncompatibleTypes().isEmpty()
                        || !setSchemaResponse.getDeletedTypes().isEmpty()) {
                    setSchemaResponse = mAppSearchImpl.setSchema(
                            mPackageName,
                            mDatabaseName,
                            new ArrayList<>(request.getSchemas()),
                            new ArrayList<>(request.getSchemasNotDisplayedBySystem()),
                            copySchemasPackageAccessible,
                            /*forceOverride=*/ true);
                }
                SetSchemaResponse.Builder responseBuilder = setSchemaResponse.toBuilder()
                        .addMigratedTypes(migratedTypes);
                mIsMutated = true;

                // 6. Put all the migrated documents into the index, now that the new schema is set.
                return migrationHelper.readAndPutDocuments(responseBuilder);
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
    @NonNull
    public ListenableFuture<Set<AppSearchSchema>> getSchema() {
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            List<AppSearchSchema> schemas = mAppSearchImpl.getSchema(mPackageName, mDatabaseName);
            return new ArraySet<>(schemas);
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> put(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        ListenableFuture<AppSearchBatchResult<String, Void>> future = execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.getGenericDocuments().size(); i++) {
                GenericDocument document = request.getGenericDocuments().get(i);
                try {
                    mAppSearchImpl.putDocument(mPackageName, mDatabaseName, document);
                    resultBuilder.setSuccess(document.getUri(), /*result=*/ null);
                } catch (Throwable t) {
                    resultBuilder.setResult(document.getUri(), throwableToFailedResult(t));
                }
            }
            mIsMutated = true;
            return resultBuilder.build();
        });

        // The existing documents with same URI will be deleted, so there maybe some
        // resources could be released after optimize().
        checkForOptimize(/*mutateBatchSize=*/ request.getGenericDocuments().size());
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, GenericDocument>> getByUri(
            @NonNull GetByUriRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<String, GenericDocument> resultBuilder =
                    new AppSearchBatchResult.Builder<>();

            Map<String, List<String>> typePropertyPaths = request.getProjectionsInternal();
            for (String uri : request.getUris()) {
                try {
                    GenericDocument document =
                            mAppSearchImpl.getDocument(mPackageName, mDatabaseName,
                                    request.getNamespace(), uri, typePropertyPaths);
                    resultBuilder.setSuccess(uri, document);
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            return resultBuilder.build();
        });
    }

    @Override
    @NonNull
    public SearchResults search(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return new SearchResultsImpl(
                mAppSearchImpl,
                mExecutorService,
                mContext,
                mPackageName,
                mDatabaseName,
                queryExpression,
                searchSpec);
    }

    @Override
    @NonNull
    public ListenableFuture<Void> reportUsage(@NonNull ReportUsageRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            mAppSearchImpl.reportUsage(
                    mPackageName,
                    mDatabaseName,
                    request.getNamespace(),
                    request.getUri(),
                    request.getUsageTimeMillis());
            mIsMutated = true;
            return null;
        });
    }

    @Override
    @NonNull
    public ListenableFuture<AppSearchBatchResult<String, Void>> remove(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        ListenableFuture<AppSearchBatchResult<String, Void>> future = execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (String uri : request.getUris()) {
                try {
                    mAppSearchImpl.remove(mPackageName, mDatabaseName, request.getNamespace(), uri);
                    resultBuilder.setSuccess(uri, /*result=*/null);
                } catch (Throwable t) {
                    resultBuilder.setResult(uri, throwableToFailedResult(t));
                }
            }
            mIsMutated = true;
            return resultBuilder.build();
        });
        checkForOptimize(/*mutateBatchSize=*/ request.getUris().size());
        return future;
    }

    @Override
    @NonNull
    public ListenableFuture<Void> remove(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        ListenableFuture<Void> future = execute(() -> {
            mAppSearchImpl.removeByQuery(mPackageName, mDatabaseName, queryExpression, searchSpec);
            mIsMutated = true;
            return null;
        });
        checkForOptimize();
        return future;
    }

    @NonNull
    @Override
    public ListenableFuture<Void> maybeFlush() {
        return execute(() -> {
            mAppSearchImpl.persistToDisk();
            return null;
        });
    }

    @Override
    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        if (mIsMutated && !mIsClosed) {
            // No future is needed here since the method is void.
            FutureUtil.execute(mExecutorService, () -> {
                mAppSearchImpl.persistToDisk();
                mIsClosed = true;
                return null;
            });
        }
    }

    private <T> ListenableFuture<T> execute(Callable<T> callable) {
        return FutureUtil.execute(mExecutorService, callable);
    }

    /**  Checks the setSchema() call won't delete any types or has incompatible types. */
    private void checkDeletedAndIncompatible(Set<String> deletedTypes,
            Set<String> incompatibleTypes)
            throws AppSearchException {
        if (deletedTypes.size() > 0
                || incompatibleTypes.size() > 0) {
            String newMessage = "Schema is incompatible."
                    + "\n  Deleted types: " + deletedTypes
                    + "\n  Incompatible types: " + incompatibleTypes;
            throw new AppSearchException(AppSearchResult.RESULT_INVALID_SCHEMA, newMessage);
        }
    }

    /**
     * Set schema to Icing for no-migration scenario.
     *
     * <p>We only need one time {@link #setSchema} call for no-migration scenario by using the
     * forceoverride in the request.
     */
    private SetSchemaResponse setSchemaNoMigrations(@NonNull SetSchemaRequest request,
            @NonNull Map<String, List<PackageIdentifier>> copySchemasPackageAccessible)
            throws AppSearchException {
        SetSchemaResponse setSchemaResponse = mAppSearchImpl.setSchema(
                mPackageName,
                mDatabaseName,
                new ArrayList<>(request.getSchemas()),
                new ArrayList<>(request.getSchemasNotDisplayedBySystem()),
                copySchemasPackageAccessible,
                request.isForceOverride());
        if (!request.isForceOverride()) {
            // check both deleted types and incompatible types are empty. That's the only case we
            // swallowed in the AppSearchImpl#setSchema().
            checkDeletedAndIncompatible(setSchemaResponse.getDeletedTypes(),
                    setSchemaResponse.getIncompatibleTypes());
        }
        mIsMutated = true;
        return setSchemaResponse;
    }

    /**
     * Finds out which incompatible schema type won't be migrated by comparing its current and
     * final version number.
     */
    private Set<String> getUnmigratedIncompatibleTypes(
            @NonNull Set<String> incompatibleSchemaTypes,
            @NonNull Map<String, Migrator> migrators,
            @NonNull Map<String, Integer> currentVersionMap,
            @NonNull Map<String, Integer> finalVersionMap)
            throws AppSearchException {
        Set<String> unmigratedSchemaTypes = new ArraySet<>();
        for (String unmigratedSchemaType : incompatibleSchemaTypes) {
            Integer currentVersion = currentVersionMap.get(unmigratedSchemaType);
            Integer finalVersion = finalVersionMap.get(unmigratedSchemaType);
            if (currentVersion == null) {
                // impossible, we have done something wrong.
                throw new AppSearchException(AppSearchResult.RESULT_UNKNOWN_ERROR,
                        "Cannot find the current version number for schema type: "
                                + unmigratedSchemaType);
            }
            if (finalVersion == null) {
                // The schema doesn't exist in the SetSchemaRequest.
                unmigratedSchemaTypes.add(unmigratedSchemaType);
                continue;
            }
            // we don't have migrator or won't trigger migration for this schema type.
            Migrator migrator = migrators.get(unmigratedSchemaType);
            if (migrator == null || !migrator.shouldMigrateToFinalVersion(currentVersion,
                    finalVersion)) {
                unmigratedSchemaTypes.add(unmigratedSchemaType);
            }
        }
        return Collections.unmodifiableSet(unmigratedSchemaTypes);
    }

    /**
     * Triggers upgrade or downgrade migration for the given schema type if its version stored in
     * AppSearch is different with the version in the request.
     * @return {@code True} if the migration has been triggered.
     */
    private boolean triggerMigration(@NonNull String schemaType,
            @NonNull Migrator migrator,
            @NonNull Map<String, Integer> currentVersionMap,
            @NonNull Map<String, Integer> finalVersionMap,
            @NonNull AppSearchMigrationHelper migrationHelper)
            throws Exception {
        Integer currentVersion = currentVersionMap.get(schemaType);
        Integer finalVersion = finalVersionMap.get(schemaType);
        if (currentVersion == null) {
            Log.d(TAG, "The SchemaType: " + schemaType + " not present in AppSearch.");
            return false;
        }
        if (!migrator.shouldMigrateToFinalVersion(currentVersion, finalVersion)) {
            return false;
        }
        if (finalVersion == null) {
            throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                    "Receive a migrator for schema type : " + schemaType
                            + ", but the schema is not present in the request.");
        }
        migrationHelper.queryAndTransform(schemaType, migrator);
        return true;
    }

    private void checkForOptimize(int mutateBatchSize) {
        mExecutorService.execute(() -> {
            try {
                mAppSearchImpl.checkForOptimize(mutateBatchSize);
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            }
        });
    }

    private void checkForOptimize() {
        mExecutorService.execute(() -> {
            try {
                mAppSearchImpl.checkForOptimize();
            } catch (AppSearchException e) {
                Log.w(TAG, "Error occurred when check for optimize", e);
            }
        });
    }
}
