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

import static androidx.appsearch.app.AppSearchResult.RESULT_OK;
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
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.PutDocumentsRequest;
import androidx.appsearch.app.RemoveByUriRequest;
import androidx.appsearch.app.ReportUsageRequest;
import androidx.appsearch.app.SearchResults;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.SetSchemaResult;
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
        return execute(() -> {
            // Convert the inner set into a List since Binder can't handle Set.
            Map<String, Set<PackageIdentifier>> schemasPackageAccessible =
                    request.getSchemasVisibleToPackagesInternal();
            Map<String, List<PackageIdentifier>> copySchemasPackageAccessible = new ArrayMap<>();
            for (Map.Entry<String, Set<PackageIdentifier>> entry :
                    schemasPackageAccessible.entrySet()) {
                copySchemasPackageAccessible.put(entry.getKey(),
                        new ArrayList<>(entry.getValue()));
            }

            Map<String, AppSearchSchema.Migrator> migratorMap = request.getMigrators();

            // No need to trigger migration if user never set migrator
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
            SetSchemaResult setSchemaResult = mAppSearchImpl.setSchema(
                    mPackageName,
                    mDatabaseName,
                    new ArrayList<>(request.getSchemas()),
                    new ArrayList<>(request.getSchemasNotVisibleToSystemUi()),
                    copySchemasPackageAccessible,
                    /*forceOverride=*/false);

            // 3. If forceOverride is false, check that all incompatible types will be migrated.
            // If some aren't we must throw an error, rather than proceeding and deleting those
            // types.
            if (!request.isForceOverride()) {
                List<String> unmigratedTypes = getUnmigratedIncompatibleTypes(
                        setSchemaResult.getIncompatibleSchemaTypes(),
                        currentVersionMap,
                        finalVersionMap);
                // check is there any unmigrated types or deleted types. If there is, we will throw
                // exception and stop from here.
                // Since the force override is false, we shouldn't worry about the schema has
                // already been set to system.
                checkDeletedAndIncompatible(setSchemaResult.getDeletedSchemaTypes(),
                        unmigratedTypes);
            }

            AppSearchMigrationHelperImpl migrationHelper =
                    new AppSearchMigrationHelperImpl(mAppSearchImpl, currentVersionMap,
                     finalVersionMap, mPackageName, mDatabaseName);

            SetSchemaResponse.Builder responseBuilder = new SetSchemaResponse.Builder();

            // 4. Trigger migration for all migrators.
            for (Map.Entry<String, AppSearchSchema.Migrator> entry : migratorMap.entrySet()) {
                if (triggerMigration(/*schemaType=*/entry.getKey(), /*migrator=*/entry.getValue(),
                        currentVersionMap, finalVersionMap, migrationHelper)) {
                    responseBuilder.addMigratedType(/*migratedType=*/entry.getKey());
                }
            }

            // 5. SetSchema a second time with forceOverride=true if the first attempted failed.
            if (setSchemaResult.getResultCode() != RESULT_OK) {
                // only trigger second setSchema() call if the first one is fail.
                setSchemaResult = mAppSearchImpl.setSchema(
                        mPackageName,
                        mDatabaseName,
                        new ArrayList<>(request.getSchemas()),
                        new ArrayList<>(request.getSchemasNotVisibleToSystemUi()),
                        copySchemasPackageAccessible,
                        /*forceOverride=*/ true);
            }
            responseBuilder.addDeletedType(setSchemaResult.getDeletedSchemaTypes());
            responseBuilder.addIncompatibleType(setSchemaResult.getIncompatibleSchemaTypes());
            mIsMutated = true;

            // 6. Put all the migrated documents into the index, now that the new schema is set.
            return migrationHelper.readAndPutDocuments(responseBuilder);
        });
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
    public ListenableFuture<AppSearchBatchResult<String, Void>> putDocuments(
            @NonNull PutDocumentsRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            AppSearchBatchResult.Builder<String, Void> resultBuilder =
                    new AppSearchBatchResult.Builder<>();
            for (int i = 0; i < request.getDocuments().size(); i++) {
                GenericDocument document = request.getDocuments().get(i);
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
    public SearchResults query(
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
    public ListenableFuture<AppSearchBatchResult<String, Void>> removeByUri(
            @NonNull RemoveByUriRequest request) {
        Preconditions.checkNotNull(request);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
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
    }

    @Override
    @NonNull
    public ListenableFuture<Void> removeByQuery(
            @NonNull String queryExpression, @NonNull SearchSpec searchSpec) {
        Preconditions.checkNotNull(queryExpression);
        Preconditions.checkNotNull(searchSpec);
        Preconditions.checkState(!mIsClosed, "AppSearchSession has already been closed");
        return execute(() -> {
            mAppSearchImpl.removeByQuery(mPackageName, mDatabaseName, queryExpression, searchSpec);
            mIsMutated = true;
            return null;
        });
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
    private void checkDeletedAndIncompatible(List<String> deletedList,
            List<String> incompatibleList)
            throws AppSearchException {
        if (deletedList.size() > 0
                || incompatibleList.size() > 0) {
            String newMessage = "Schema is incompatible."
                    + "\n  Deleted types: " + deletedList
                    + "\n  Incompatible types: " + incompatibleList;
            throw new AppSearchException(AppSearchResult.RESULT_INVALID_SCHEMA, newMessage);
        }
    }

    /**
     * Set schema to Icing for no-migration scenario.
     *
     * <p>We only need one time {@link #setSchema} call for no-migration scenario by using the
     * forceoverride in the request.
     */
    private SetSchemaResponse setSchemaNoMigrations(SetSchemaRequest request,
            Map<String, List<PackageIdentifier>> copySchemasPackageAccessible)
            throws AppSearchException {
        SetSchemaResult setSchemaResult = mAppSearchImpl.setSchema(
                mPackageName,
                mDatabaseName,
                new ArrayList<>(request.getSchemas()),
                new ArrayList<>(request.getSchemasNotVisibleToSystemUi()),
                copySchemasPackageAccessible,
                request.isForceOverride());
        if (setSchemaResult.getResultCode() != RESULT_OK) {
            // check both deleted types and incompatible types are empty
            checkDeletedAndIncompatible(setSchemaResult.getDeletedSchemaTypes(),
                    setSchemaResult.getIncompatibleSchemaTypes());
        }
        mIsMutated = true;
        return new SetSchemaResponse.Builder().build();
    }

    /**
     * Finds out which incompatible schema type won't be migrated by comparing its current and
     * final version number.
     */
    private List<String> getUnmigratedIncompatibleTypes(List<String> incompatibleSchemaTypes,
            Map<String, Integer> currentVersionMap, Map<String, Integer> finalVersionMap)
            throws AppSearchException {
        List<String> unmigratedSchemaTypes = new ArrayList<>();
        for (String unmigratedSchemaType : incompatibleSchemaTypes) {
            Integer currentVersion = currentVersionMap.get(unmigratedSchemaType);
            Integer finalVersion = finalVersionMap.get(unmigratedSchemaType);
            if (currentVersion == null) {
                // impossible, we must do something wrong.
                throw new AppSearchException(AppSearchResult.RESULT_UNKNOWN_ERROR,
                        "Cannot find the current version number for schema type: "
                                + unmigratedSchemaType);
            }
            if (finalVersion == null || finalVersion.equals(currentVersion)) {
                // There is no migration for this incompatible schema type.
                unmigratedSchemaTypes.add(unmigratedSchemaType);
            }
        }
        return Collections.unmodifiableList(unmigratedSchemaTypes);
    }

    /**
     * Triggers upgrade or downgrade migration for the given schema type if its version stored in
     * AppSearch is different with the version in the request.
     * @return ture if the migration has been triggered.
     */
    private boolean triggerMigration(String schemaType, AppSearchSchema.Migrator migrator,
            Map<String, Integer> currentVersionMap, Map<String, Integer> finalVersionMap,
            AppSearchMigrationHelperImpl migrationHelper)
            throws Exception {
        try {
            Integer currentVersion = currentVersionMap.get(schemaType);
            Integer finalVersion = finalVersionMap.get(schemaType);
            if (currentVersion == null) {
                Log.d(TAG, "The SchemaType: " + schemaType + " not present in AppSearch.");
                return false;
            }
            if (finalVersion == null) {
                throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "Receive a migrator for schema type : " + schemaType
                                + ", but the schema is not present in the request.");
            }
            if (currentVersion < finalVersion) {
                migrator.onUpgrade(currentVersion, finalVersion, migrationHelper);
            } else if (currentVersion > finalVersion) {
                migrator.onDowngrade(currentVersion, finalVersion, migrationHelper);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            migrationHelper.deleteTempFile();
            throw e;
        }
    }
}
