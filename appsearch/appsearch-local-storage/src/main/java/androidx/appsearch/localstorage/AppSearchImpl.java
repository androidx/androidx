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

package androidx.appsearch.localstorage;

import static androidx.appsearch.app.AppSearchResult.RESULT_INTERNAL_ERROR;
import static androidx.appsearch.app.AppSearchResult.RESULT_SECURITY_ERROR;
import static androidx.appsearch.app.InternalSetSchemaResponse.newFailedSetSchemaResponse;
import static androidx.appsearch.app.InternalSetSchemaResponse.newSuccessfulSetSchemaResponse;
import static androidx.appsearch.localstorage.util.PrefixUtil.addPrefixToDocument;
import static androidx.appsearch.localstorage.util.PrefixUtil.createPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.getDatabaseName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;
import static androidx.appsearch.localstorage.util.PrefixUtil.getPrefix;
import static androidx.appsearch.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetByDocumentIdRequest;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.SearchSuggestionResult;
import androidx.appsearch.app.SearchSuggestionSpec;
import androidx.appsearch.app.SetSchemaResponse;
import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.converter.GenericDocumentToProtoConverter;
import androidx.appsearch.localstorage.converter.ResultCodeToProtoConverter;
import androidx.appsearch.localstorage.converter.SchemaToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchResultToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchSpecToProtoConverter;
import androidx.appsearch.localstorage.converter.SearchSuggestionSpecToProtoConverter;
import androidx.appsearch.localstorage.converter.SetSchemaResponseToProtoConverter;
import androidx.appsearch.localstorage.converter.TypePropertyPathToProtoConverter;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityUtil;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.util.LogUtil;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.proto.DebugInfoProto;
import com.google.android.icing.proto.DebugInfoResultProto;
import com.google.android.icing.proto.DebugInfoVerbosity;
import com.google.android.icing.proto.DeleteByQueryResultProto;
import com.google.android.icing.proto.DeleteResultProto;
import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.DocumentStorageInfoProto;
import com.google.android.icing.proto.GetAllNamespacesResultProto;
import com.google.android.icing.proto.GetOptimizeInfoResultProto;
import com.google.android.icing.proto.GetResultProto;
import com.google.android.icing.proto.GetResultSpecProto;
import com.google.android.icing.proto.GetSchemaResultProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.InitializeResultProto;
import com.google.android.icing.proto.LogSeverity;
import com.google.android.icing.proto.NamespaceStorageInfoProto;
import com.google.android.icing.proto.OptimizeResultProto;
import com.google.android.icing.proto.PersistToDiskResultProto;
import com.google.android.icing.proto.PersistType;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.PutResultProto;
import com.google.android.icing.proto.ReportUsageResultProto;
import com.google.android.icing.proto.ResetResultProto;
import com.google.android.icing.proto.ResultSpecProto;
import com.google.android.icing.proto.SchemaProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.ScoringSpecProto;
import com.google.android.icing.proto.SearchResultProto;
import com.google.android.icing.proto.SearchSpecProto;
import com.google.android.icing.proto.SetSchemaResultProto;
import com.google.android.icing.proto.StatusProto;
import com.google.android.icing.proto.StorageInfoProto;
import com.google.android.icing.proto.StorageInfoResultProto;
import com.google.android.icing.proto.SuggestionResponse;
import com.google.android.icing.proto.TypePropertyMask;
import com.google.android.icing.proto.UsageReport;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages interaction with the native IcingSearchEngine and other components to implement AppSearch
 * functionality.
 *
 * <p>Never create two instances using the same folder.
 *
 * <p>A single instance of {@link AppSearchImpl} can support all packages and databases.
 * This is done by combining the package and database name into a unique prefix and
 * prefixing the schemas and documents stored under that owner. Schemas and documents are
 * physically saved together in {@link IcingSearchEngine}, but logically isolated:
 * <ul>
 *      <li>Rewrite SchemaType in SchemaProto by adding the package-database prefix and save into
 *          SchemaTypes set in {@link #setSchema}.
 *      <li>Rewrite namespace and SchemaType in DocumentProto by adding package-database prefix and
 *          save to namespaces set in {@link #putDocument}.
 *      <li>Remove package-database prefix when retrieving documents in {@link #getDocument} and
 *          {@link #query}.
 *      <li>Rewrite filters in {@link SearchSpecProto} to have all namespaces and schema types of
 *          the queried database when user using empty filters in {@link #query}.
 * </ul>
 *
 * <p>Methods in this class belong to two groups, the query group and the mutate group.
 * <ul>
 *     <li>All methods are going to modify global parameters and data in Icing are executed under
 *         WRITE lock to keep thread safety.
 *     <li>All methods are going to access global parameters or query data from Icing are executed
 *         under READ lock to improve query performance.
 * </ul>
 *
 * <p>This class is thread safe.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@WorkerThread
public final class AppSearchImpl implements Closeable {
    private static final String TAG = "AppSearchImpl";

    /** A value 0 means that there're no more pages in the search results. */
    private static final long EMPTY_PAGE_TOKEN = 0;
    @VisibleForTesting
    static final int CHECK_OPTIMIZE_INTERVAL = 100;

    /** A GetResultSpec that uses projection to skip all properties. */
    private static final GetResultSpecProto GET_RESULT_SPEC_NO_PROPERTIES =
            GetResultSpecProto.newBuilder().addTypePropertyMasks(
                    TypePropertyMask.newBuilder().setSchemaType(
                            GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD)).build();

    private final ReadWriteLock mReadWriteLock = new ReentrantReadWriteLock();
    private final OptimizeStrategy mOptimizeStrategy;
    private final LimitConfig mLimitConfig;

    @GuardedBy("mReadWriteLock")
    @VisibleForTesting
    final IcingSearchEngine mIcingSearchEngineLocked;

    // This map contains schema types and SchemaTypeConfigProtos for all package-database
    // prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
    // prefixed schema type to its respective SchemaTypeConfigProto.
    @GuardedBy("mReadWriteLock")
    private final Map<String, Map<String, SchemaTypeConfigProto>> mSchemaMapLocked =
            new ArrayMap<>();

    // This map contains namespaces for all package-database prefixes. All values in the map are
    // prefixed with the package-database prefix.
    // TODO(b/172360376): Check if this can be replaced with an ArrayMap
    @GuardedBy("mReadWriteLock")
    private final Map<String, Set<String>> mNamespaceMapLocked = new HashMap<>();

    /** Maps package name to active document count. */
    @GuardedBy("mReadWriteLock")
    private final Map<String, Integer> mDocumentCountMapLocked = new ArrayMap<>();

    // Maps packages to the set of valid nextPageTokens that the package can manipulate. A token
    // is unique and constant per query (i.e. the same token '123' is used to iterate through
    // pages of search results). The tokens themselves are generated and tracked by
    // IcingSearchEngine. IcingSearchEngine considers a token valid and won't be reused
    // until we call invalidateNextPageToken on the token.
    //
    // Note that we synchronize on itself because the nextPageToken cache is checked at
    // query-time, and queries are done in parallel with a read lock. Ideally, this would be
    // guarded by the normal mReadWriteLock.writeLock, but ReentrantReadWriteLocks can't upgrade
    // read to write locks. This lock should be acquired at the smallest scope possible.
    // mReadWriteLock is a higher-level lock, so calls shouldn't be made out
    // to any functions that grab the lock.
    @GuardedBy("mNextPageTokensLocked")
    private final Map<String, Set<Long>> mNextPageTokensLocked = new ArrayMap<>();

    private final ObserverManager mObserverManager = new ObserverManager();

    /**
     * VisibilityStore will be used in {@link #setSchema} and {@link #getSchema} to store and query
     * visibility information. But to create a {@link VisibilityStore}, it will call
     * {@link #setSchema} and {@link #getSchema} to get the visibility schema. Make it nullable to
     * avoid call it before we actually create it.
     */
    @Nullable
    @VisibleForTesting
    @GuardedBy("mReadWriteLock")
    final VisibilityStore mVisibilityStoreLocked;

    @Nullable
    @GuardedBy("mReadWriteLock")
    private final VisibilityChecker mVisibilityCheckerLocked;

    /**
     * The counter to check when to call {@link #checkForOptimize}. The
     * interval is
     * {@link #CHECK_OPTIMIZE_INTERVAL}.
     */
    @GuardedBy("mReadWriteLock")
    private int mOptimizeIntervalCountLocked = 0;

    /** Whether this instance has been closed, and therefore unusable. */
    @GuardedBy("mReadWriteLock")
    private boolean mClosedLocked = false;

    /**
     * Creates and initializes an instance of {@link AppSearchImpl} which writes data to the given
     * folder.
     *
     * <p>Clients can pass a {@link AppSearchLogger} here through their AppSearchSession, but it
     * can't be saved inside {@link AppSearchImpl}, because the impl will be shared by all the
     * sessions for the same package in JetPack.
     *
     * <p>Instead, logger instance needs to be passed to each individual method, like create, query
     * and putDocument.
     *
     * @param initStatsBuilder collects stats for initialization if provided.
     * @param visibilityChecker The {@link VisibilityChecker} that check whether the caller has
     *                          access to aa specific schema. Pass null will lost that ability and
     *                          global querier could only get their own data.
     */
    @NonNull
    public static AppSearchImpl create(
            @NonNull File icingDir,
            @NonNull LimitConfig limitConfig,
            @NonNull IcingOptionsConfig icingOptionsConfig,
            @Nullable InitializeStats.Builder initStatsBuilder,
            @NonNull OptimizeStrategy optimizeStrategy,
            @Nullable VisibilityChecker visibilityChecker)
            throws AppSearchException {
        return new AppSearchImpl(icingDir, limitConfig, icingOptionsConfig, initStatsBuilder,
                optimizeStrategy, visibilityChecker);
    }

    /**
     * @param initStatsBuilder collects stats for initialization if provided.
     */
    private AppSearchImpl(
            @NonNull File icingDir,
            @NonNull LimitConfig limitConfig,
            @NonNull IcingOptionsConfig icingOptionsConfig,
            @Nullable InitializeStats.Builder initStatsBuilder,
            @NonNull OptimizeStrategy optimizeStrategy,
            @Nullable VisibilityChecker visibilityChecker)
            throws AppSearchException {
        Preconditions.checkNotNull(icingDir);
        Preconditions.checkNotNull(icingOptionsConfig);
        mLimitConfig = Preconditions.checkNotNull(limitConfig);
        mOptimizeStrategy = Preconditions.checkNotNull(optimizeStrategy);
        mVisibilityCheckerLocked = visibilityChecker;

        mReadWriteLock.writeLock().lock();
        try {
            // We synchronize here because we don't want to call IcingSearchEngine.initialize() more
            // than once. It's unnecessary and can be a costly operation.
            IcingSearchEngineOptions options = IcingSearchEngineOptions.newBuilder()
                    .setBaseDir(icingDir.getAbsolutePath())
                    .setMaxTokenLength(icingOptionsConfig.getMaxTokenLength())
                    .setIndexMergeSize(icingOptionsConfig.getIndexMergeSize())
                    .setDocumentStoreNamespaceIdFingerprint(
                            icingOptionsConfig.getDocumentStoreNamespaceIdFingerprint())
                    .setOptimizeRebuildIndexThreshold(
                            icingOptionsConfig.getOptimizeRebuildIndexThreshold())
                    .setCompressionLevel(icingOptionsConfig.getCompressionLevel())
                    .build();
            LogUtil.piiTrace(TAG, "Constructing IcingSearchEngine, request", options);
            mIcingSearchEngineLocked = new IcingSearchEngine(options);
            LogUtil.piiTrace(
                    TAG,
                    "Constructing IcingSearchEngine, response",
                    ObjectsCompat.hashCode(mIcingSearchEngineLocked));

            // The core initialization procedure. If any part of this fails, we bail into
            // resetLocked(), deleting all data (but hopefully allowing AppSearchImpl to come up).
            try {
                LogUtil.piiTrace(TAG, "icingSearchEngine.initialize, request");
                InitializeResultProto initializeResultProto = mIcingSearchEngineLocked.initialize();
                LogUtil.piiTrace(
                        TAG,
                        "icingSearchEngine.initialize, response",
                        initializeResultProto.getStatus(),
                        initializeResultProto);

                if (initStatsBuilder != null) {
                    initStatsBuilder
                            .setStatusCode(
                                    statusProtoToResultCode(initializeResultProto.getStatus()))
                            // TODO(b/173532925) how to get DeSyncs value
                            .setHasDeSync(false);
                    AppSearchLoggerHelper.copyNativeStats(
                            initializeResultProto.getInitializeStats(), initStatsBuilder);
                }
                checkSuccess(initializeResultProto.getStatus());

                // Read all protos we need to construct AppSearchImpl's cache maps
                long prepareSchemaAndNamespacesLatencyStartMillis = SystemClock.elapsedRealtime();
                SchemaProto schemaProto = getSchemaProtoLocked();

                LogUtil.piiTrace(TAG, "init:getAllNamespaces, request");
                GetAllNamespacesResultProto getAllNamespacesResultProto =
                        mIcingSearchEngineLocked.getAllNamespaces();
                LogUtil.piiTrace(
                        TAG,
                        "init:getAllNamespaces, response",
                        getAllNamespacesResultProto.getNamespacesCount(),
                        getAllNamespacesResultProto);

                StorageInfoProto storageInfoProto = getRawStorageInfoProto();

                // Log the time it took to read the data that goes into the cache maps
                if (initStatsBuilder != null) {
                    // In case there is some error for getAllNamespaces, we can still
                    // set the latency for preparation.
                    // If there is no error, the value will be overridden by the actual one later.
                    initStatsBuilder.setStatusCode(
                            statusProtoToResultCode(getAllNamespacesResultProto.getStatus()))
                            .setPrepareSchemaAndNamespacesLatencyMillis(
                                    (int) (SystemClock.elapsedRealtime()
                                            - prepareSchemaAndNamespacesLatencyStartMillis));
                }
                checkSuccess(getAllNamespacesResultProto.getStatus());

                // Populate schema map
                List<SchemaTypeConfigProto> schemaProtoTypesList = schemaProto.getTypesList();
                for (int i = 0; i < schemaProtoTypesList.size(); i++) {
                    SchemaTypeConfigProto schema = schemaProtoTypesList.get(i);
                    String prefixedSchemaType = schema.getSchemaType();
                    addToMap(mSchemaMapLocked, getPrefix(prefixedSchemaType), schema);
                }

                // Populate namespace map
                List<String> prefixedNamespaceList =
                        getAllNamespacesResultProto.getNamespacesList();
                for (int i = 0; i < prefixedNamespaceList.size(); i++) {
                    String prefixedNamespace = prefixedNamespaceList.get(i);
                    addToMap(mNamespaceMapLocked, getPrefix(prefixedNamespace), prefixedNamespace);
                }

                // Populate document count map
                rebuildDocumentCountMapLocked(storageInfoProto);

                // logging prepare_schema_and_namespaces latency
                if (initStatsBuilder != null) {
                    initStatsBuilder.setPrepareSchemaAndNamespacesLatencyMillis(
                            (int) (SystemClock.elapsedRealtime()
                                    - prepareSchemaAndNamespacesLatencyStartMillis));
                }

                LogUtil.piiTrace(TAG, "Init completed successfully");

            } catch (AppSearchException e) {
                // Some error. Reset and see if it fixes it.
                Log.e(TAG, "Error initializing, resetting IcingSearchEngine.", e);
                if (initStatsBuilder != null) {
                    initStatsBuilder.setStatusCode(e.getResultCode());
                }
                resetLocked(initStatsBuilder);
            }

            long prepareVisibilityStoreLatencyStartMillis = SystemClock.elapsedRealtime();
            mVisibilityStoreLocked = new VisibilityStore(this);
            long prepareVisibilityStoreLatencyEndMillis = SystemClock.elapsedRealtime();
            if (initStatsBuilder != null) {
                initStatsBuilder.setPrepareVisibilityStoreLatencyMillis((int)
                        (prepareVisibilityStoreLatencyEndMillis
                                - prepareVisibilityStoreLatencyStartMillis));
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    @GuardedBy("mReadWriteLock")
    private void throwIfClosedLocked() {
        if (mClosedLocked) {
            throw new IllegalStateException("Trying to use a closed AppSearchImpl instance.");
        }
    }

    /**
     * Persists data to disk and closes the instance.
     *
     * <p>This instance is no longer usable after it's been closed. Call {@link #create} to
     * create a new, usable instance.
     */
    @Override
    public void close() {
        mReadWriteLock.writeLock().lock();
        try {
            if (mClosedLocked) {
                return;
            }
            persistToDisk(PersistType.Code.FULL);
            LogUtil.piiTrace(TAG, "icingSearchEngine.close, request");
            mIcingSearchEngineLocked.close();
            LogUtil.piiTrace(TAG, "icingSearchEngine.close, response");
            mClosedLocked = true;
        } catch (AppSearchException e) {
            Log.w(TAG, "Error when closing AppSearchImpl.", e);
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Updates the AppSearch schema for this app.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName                 The package name that owns the schemas.
     * @param databaseName                The name of the database where this schema lives.
     * @param schemas                     Schemas to set for this app.
     * @param visibilityDocuments         {@link VisibilityDocument}s that contain all
     *                                    visibility setting information for those schemas
     *                                    has user custom settings. Other schemas in the list
     *                                    that don't has a {@link VisibilityDocument}
     *                                    will be treated as having the default visibility,
     *                                    which is accessible by the system and no other packages.
     * @param forceOverride               Whether to force-apply the schema even if it is
     *                                    incompatible. Documents
     *                                    which do not comply with the new schema will be deleted.
     * @param version                     The overall version number of the request.
     * @param setSchemaStatsBuilder       Builder for {@link SetSchemaStats} to hold stats for
     *                                    setSchema
     * @return A success {@link InternalSetSchemaResponse} with a {@link SetSchemaResponse}. Or a
     * failed {@link InternalSetSchemaResponse} if this call contains incompatible change. The
     * {@link SetSchemaResponse} in the failed {@link InternalSetSchemaResponse} contains which type
     * is incompatible. You need to check the status by
     * {@link InternalSetSchemaResponse#isSuccess()}.
     *
     * @throws AppSearchException On IcingSearchEngine error. If the status code is
     *                            FAILED_PRECONDITION for the incompatible change, the
     *                            exception will be converted to the SetSchemaResponse.
     */
    @NonNull
    public InternalSetSchemaResponse setSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<VisibilityDocument> visibilityDocuments,
            boolean forceOverride,
            int version,
            @Nullable SetSchemaStats.Builder setSchemaStatsBuilder) throws AppSearchException {
        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            if (setSchemaStatsBuilder != null) {
                setSchemaStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            if (mObserverManager.isPackageObserved(packageName)) {
                return doSetSchemaWithChangeNotificationLocked(
                        packageName,
                        databaseName,
                        schemas,
                        visibilityDocuments,
                        forceOverride,
                        version,
                        setSchemaStatsBuilder);
            } else {
                return doSetSchemaNoChangeNotificationLocked(
                        packageName,
                        databaseName,
                        schemas,
                        visibilityDocuments,
                        forceOverride,
                        version,
                        setSchemaStatsBuilder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Updates the AppSearch schema for this app, dispatching change notifications.
     *
     * @see #setSchema
     * @see #doSetSchemaNoChangeNotificationLocked
     */
    @GuardedBy("mReadWriteLock")
    @NonNull
    private InternalSetSchemaResponse doSetSchemaWithChangeNotificationLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<VisibilityDocument> visibilityDocuments,
            boolean forceOverride,
            int version,
            @Nullable SetSchemaStats.Builder setSchemaStatsBuilder) throws AppSearchException {
        // First, capture the old state of the system. This includes the old schema as well as
        // whether each registered observer can access each type. Once VisibilityStore is updated
        // by the setSchema call, the information of which observers could see which types will be
        // lost.
        long getOldSchemaStartTimeMillis = SystemClock.elapsedRealtime();
        GetSchemaResponse oldSchema = getSchema(
                packageName,
                databaseName,
                // A CallerAccess object for internal use that has local access to this database.
                new CallerAccess(/*callingPackageName=*/packageName));
        long getOldSchemaEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setIsPackageObserved(true)
                    .setGetOldSchemaLatencyMillis(
                            (int) (getOldSchemaEndTimeMillis - getOldSchemaStartTimeMillis));
        }

        int getOldSchemaObserverStartTimeMillis =
                (int) (SystemClock.elapsedRealtime() - getOldSchemaEndTimeMillis);
        // Cache some lookup tables to help us work with the old schema
        Set<AppSearchSchema> oldSchemaTypes = oldSchema.getSchemas();
        Map<String, AppSearchSchema> oldSchemaNameToType = new ArrayMap<>(oldSchemaTypes.size());
        // Maps unprefixed schema name to the set of listening packages that had visibility into
        // that type under the old schema.
        Map<String, Set<String>> oldSchemaNameToVisibleListeningPackage =
                new ArrayMap<>(oldSchemaTypes.size());
        for (AppSearchSchema oldSchemaType : oldSchemaTypes) {
            String oldSchemaName = oldSchemaType.getSchemaType();
            oldSchemaNameToType.put(oldSchemaName, oldSchemaType);
            oldSchemaNameToVisibleListeningPackage.put(
                    oldSchemaName,
                    mObserverManager.getObserversForSchemaType(
                            packageName,
                            databaseName,
                            oldSchemaName,
                            mVisibilityStoreLocked,
                            mVisibilityCheckerLocked));
        }
        int getOldSchemaObserverLatencyMillis =
                (int) (SystemClock.elapsedRealtime() - getOldSchemaObserverStartTimeMillis);

        // Apply the new schema
        InternalSetSchemaResponse internalSetSchemaResponse = doSetSchemaNoChangeNotificationLocked(
                packageName,
                databaseName,
                schemas,
                visibilityDocuments,
                forceOverride,
                version,
                setSchemaStatsBuilder);

        // This check is needed wherever setSchema is called to detect soft errors which do not
        // throw an exception but also prevent the schema from actually being applied.
        if (!internalSetSchemaResponse.isSuccess()) {
            return internalSetSchemaResponse;
        }

        long getNewSchemaObserverStartTimeMillis = SystemClock.elapsedRealtime();
        // Cache some lookup tables to help us work with the new schema
        Map<String, AppSearchSchema> newSchemaNameToType = new ArrayMap<>(schemas.size());
        // Maps unprefixed schema name to the set of listening packages that have visibility into
        // that type under the new schema.
        Map<String, Set<String>> newSchemaNameToVisibleListeningPackage =
                new ArrayMap<>(schemas.size());
        for (AppSearchSchema newSchemaType : schemas) {
            String newSchemaName = newSchemaType.getSchemaType();
            newSchemaNameToType.put(newSchemaName, newSchemaType);
            newSchemaNameToVisibleListeningPackage.put(
                    newSchemaName,
                    mObserverManager.getObserversForSchemaType(
                            packageName,
                            databaseName,
                            newSchemaName,
                            mVisibilityStoreLocked,
                            mVisibilityCheckerLocked));
        }
        long getNewSchemaObserverEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setGetObserverLatencyMillis(getOldSchemaObserverLatencyMillis
                    + (int) (getNewSchemaObserverEndTimeMillis
                    - getNewSchemaObserverStartTimeMillis));
        }

        long preparingChangeNotificationStartTimeMillis = SystemClock.elapsedRealtime();
        // Create a unified set of all schema names mentioned in either the old or new schema.
        Set<String> allSchemaNames = new ArraySet<>(oldSchemaNameToType.keySet());
        allSchemaNames.addAll(newSchemaNameToType.keySet());

        // Perform the diff between the old and new schema.
        for (String schemaName : allSchemaNames) {
            final AppSearchSchema contentBefore = oldSchemaNameToType.get(schemaName);
            final AppSearchSchema contentAfter = newSchemaNameToType.get(schemaName);

            final boolean existBefore = (contentBefore != null);
            final boolean existAfter = (contentAfter != null);

            // This should never happen
            if (!existBefore && !existAfter) {
                continue;
            }

            boolean contentsChanged = true;
            if (contentBefore != null
                    && contentBefore.equals(contentAfter)) {
                contentsChanged = false;
            }

            Set<String> oldVisibleListeners =
                    oldSchemaNameToVisibleListeningPackage.get(schemaName);
            Set<String> newVisibleListeners =
                    newSchemaNameToVisibleListeningPackage.get(schemaName);
            Set<String> allListeningPackages = new ArraySet<>(oldVisibleListeners);
            if (newVisibleListeners != null) {
                allListeningPackages.addAll(newVisibleListeners);
            }

            // Now that we've computed the relationship between the old and new schema, we go
            // observer by observer and consider the observer's own personal view of the schema.
            for (String listeningPackageName : allListeningPackages) {
                // Figure out the visibility
                final boolean visibleBefore = (
                        existBefore
                                && oldVisibleListeners != null
                                && oldVisibleListeners.contains(listeningPackageName));
                final boolean visibleAfter = (
                        existAfter
                                && newVisibleListeners != null
                                && newVisibleListeners.contains(listeningPackageName));

                // Now go through the truth table of all the relevant flags.
                // visibleBefore and visibleAfter take into account existBefore and existAfter, so
                // we can stop worrying about existBefore and existAfter.
                boolean sendNotification = false;
                if (visibleBefore && visibleAfter && contentsChanged) {
                    sendNotification = true;  // Type configuration was modified
                } else if (!visibleBefore && visibleAfter) {
                    sendNotification = true;  // Newly granted visibility or type was created
                } else if (visibleBefore && !visibleAfter) {
                    sendNotification = true;  // Revoked visibility or type was deleted
                } else {
                    // No visibility before and no visibility after. Nothing to dispatch.
                }

                if (sendNotification) {
                    mObserverManager.onSchemaChange(
                            /*listeningPackageName=*/listeningPackageName,
                            /*targetPackageName=*/packageName,
                            /*databaseName=*/databaseName,
                            /*schemaName=*/schemaName);
                }
            }
        }
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setPreparingChangeNotificationLatencyMillis(
                    (int) (SystemClock.elapsedRealtime()
                            - preparingChangeNotificationStartTimeMillis));
        }

        return internalSetSchemaResponse;
    }

    /**
     * Updates the AppSearch schema for this app, without dispatching change notifications.
     *
     * <p>This method can be used only when no one is observing {@code packageName}.
     *
     * @see #setSchema
     * @see #doSetSchemaWithChangeNotificationLocked
     */
    @GuardedBy("mReadWriteLock")
    @NonNull
    private InternalSetSchemaResponse doSetSchemaNoChangeNotificationLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull List<AppSearchSchema> schemas,
            @NonNull List<VisibilityDocument> visibilityDocuments,
            boolean forceOverride,
            int version,
            @Nullable SetSchemaStats.Builder setSchemaStatsBuilder) throws AppSearchException {
        long setRewriteSchemaLatencyMillis = SystemClock.elapsedRealtime();
        SchemaProto.Builder existingSchemaBuilder = getSchemaProtoLocked().toBuilder();

        SchemaProto.Builder newSchemaBuilder = SchemaProto.newBuilder();
        for (int i = 0; i < schemas.size(); i++) {
            AppSearchSchema schema = schemas.get(i);
            SchemaTypeConfigProto schemaTypeProto =
                    SchemaToProtoConverter.toSchemaTypeConfigProto(schema, version);
            newSchemaBuilder.addTypes(schemaTypeProto);
        }

        String prefix = createPrefix(packageName, databaseName);
        // Combine the existing schema (which may have types from other prefixes) with this
        // prefix's new schema. Modifies the existingSchemaBuilder.
        RewrittenSchemaResults rewrittenSchemaResults = rewriteSchema(prefix,
                existingSchemaBuilder,
                newSchemaBuilder.build());

        long rewriteSchemaEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setRewriteSchemaLatencyMillis(
                    (int) (rewriteSchemaEndTimeMillis - setRewriteSchemaLatencyMillis));
        }

        // Apply schema
        long nativeLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        SchemaProto finalSchema = existingSchemaBuilder.build();
        LogUtil.piiTrace(TAG, "setSchema, request", finalSchema.getTypesCount(), finalSchema);
        SetSchemaResultProto setSchemaResultProto =
                mIcingSearchEngineLocked.setSchema(finalSchema, forceOverride);
        LogUtil.piiTrace(
                TAG, "setSchema, response", setSchemaResultProto.getStatus(), setSchemaResultProto);
        long nativeLatencyEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder
                    .setTotalNativeLatencyMillis(
                            (int) (nativeLatencyEndTimeMillis - nativeLatencyStartTimeMillis))
                    .setStatusCode(statusProtoToResultCode(
                            setSchemaResultProto.getStatus()));
            AppSearchLoggerHelper.copyNativeStats(setSchemaResultProto,
                    setSchemaStatsBuilder);
        }

        boolean isFailedPrecondition = setSchemaResultProto.getStatus().getCode()
                == StatusProto.Code.FAILED_PRECONDITION;
        // Determine whether it succeeded.
        try {
            checkSuccess(setSchemaResultProto.getStatus());
        } catch (AppSearchException e) {
            // Swallow the exception for the incompatible change case. We will generate a failed
            // InternalSetSchemaResponse for this case.
            int deletedTypes = setSchemaResultProto.getDeletedSchemaTypesCount();
            int incompatibleTypes = setSchemaResultProto.getIncompatibleSchemaTypesCount();
            boolean isIncompatible = deletedTypes > 0 || incompatibleTypes > 0;
            if (isFailedPrecondition && !forceOverride  && isIncompatible) {
                SetSchemaResponse setSchemaResponse = SetSchemaResponseToProtoConverter
                        .toSetSchemaResponse(setSchemaResultProto, prefix);
                String errorMessage = "Schema is incompatible."
                        + "\n  Deleted types: " + setSchemaResponse.getDeletedTypes()
                        + "\n  Incompatible types: " + setSchemaResponse.getIncompatibleTypes();
                return newFailedSetSchemaResponse(setSchemaResponse, errorMessage);
            } else {
                throw e;
            }
        }

        long saveVisibilitySettingStartTimeMillis = SystemClock.elapsedRealtime();
        // Update derived data structures.
        for (SchemaTypeConfigProto schemaTypeConfigProto :
                rewrittenSchemaResults.mRewrittenPrefixedTypes.values()) {
            addToMap(mSchemaMapLocked, prefix, schemaTypeConfigProto);
        }

        for (String schemaType : rewrittenSchemaResults.mDeletedPrefixedTypes) {
            removeFromMap(mSchemaMapLocked, prefix, schemaType);
        }
        // Since the constructor of VisibilityStore will set schema. Avoid call visibility
        // store before we have already created it.
        if (mVisibilityStoreLocked != null) {
            // Add prefix to all visibility documents.
            List<VisibilityDocument> prefixedVisibilityDocuments =
                    new ArrayList<>(visibilityDocuments.size());
            // Find out which Visibility document is deleted or changed to all-default settings.
            // We need to remove them from Visibility Store.
            Set<String> deprecatedVisibilityDocuments =
                    new ArraySet<>(rewrittenSchemaResults.mRewrittenPrefixedTypes.keySet());
            for (int i = 0; i < visibilityDocuments.size(); i++) {
                VisibilityDocument unPrefixedDocument = visibilityDocuments.get(i);
                // The VisibilityDocument is controlled by the client and it's untrusted but we
                // make it safe by appending a prefix.
                // We must control the package-database prefix. Therefore even if the client
                // fake the id, they can only mess their own app. That's totally allowed and
                // they can do this via the public API too.
                String prefixedSchemaType = prefix + unPrefixedDocument.getId();
                prefixedVisibilityDocuments.add(new VisibilityDocument(
                        unPrefixedDocument.toBuilder()
                                .setId(prefixedSchemaType)
                                .build()));
                // This schema has visibility settings. We should keep it from the removal list.
                deprecatedVisibilityDocuments.remove(prefixedSchemaType);
            }
            // Now deprecatedVisibilityDocuments contains those existing schemas that has
            // all-default visibility settings, add deleted schemas. That's all we need to
            // remove.
            deprecatedVisibilityDocuments.addAll(rewrittenSchemaResults.mDeletedPrefixedTypes);
            mVisibilityStoreLocked.removeVisibility(deprecatedVisibilityDocuments);
            mVisibilityStoreLocked.setVisibility(prefixedVisibilityDocuments);
        }
        long saveVisibilitySettingEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setVisibilitySettingLatencyMillis(
                    (int) (saveVisibilitySettingEndTimeMillis
                            - saveVisibilitySettingStartTimeMillis));
        }

        long convertToResponseStartTimeMillis = SystemClock.elapsedRealtime();
        InternalSetSchemaResponse setSchemaResponse = newSuccessfulSetSchemaResponse(
                SetSchemaResponseToProtoConverter
                        .toSetSchemaResponse(setSchemaResultProto, prefix));
        long convertToResponseEndTimeMillis = SystemClock.elapsedRealtime();
        if (setSchemaStatsBuilder != null) {
            setSchemaStatsBuilder.setConvertToResponseLatencyMillis(
                    (int) (convertToResponseEndTimeMillis
                            - convertToResponseStartTimeMillis));
        }
        return setSchemaResponse;
    }

    /**
     * Retrieves the AppSearch schema for this package name, database.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName  Package that owns the requested {@link AppSearchSchema} instances.
     * @param databaseName Database that owns the requested {@link AppSearchSchema} instances.
     * @param callerAccess Visibility access info of the calling app
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @NonNull
    public GetSchemaResponse getSchema(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull CallerAccess callerAccess)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            SchemaProto fullSchema = getSchemaProtoLocked();
            String prefix = createPrefix(packageName, databaseName);
            GetSchemaResponse.Builder responseBuilder = new GetSchemaResponse.Builder();
            for (int i = 0; i < fullSchema.getTypesCount(); i++) {
                // Check that this type belongs to the requested app and that the caller has
                // access to it.
                SchemaTypeConfigProto typeConfig = fullSchema.getTypes(i);
                String prefixedSchemaType = typeConfig.getSchemaType();
                String typePrefix = getPrefix(prefixedSchemaType);
                if (!prefix.equals(typePrefix)) {
                    // This schema type doesn't belong to the database we're querying for.
                    continue;
                }
                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        callerAccess,
                        packageName,
                        prefixedSchemaType,
                        mVisibilityStoreLocked,
                        mVisibilityCheckerLocked)) {
                    // Caller doesn't have access to this type.
                    continue;
                }

                // Rewrite SchemaProto.types.schema_type
                SchemaTypeConfigProto.Builder typeConfigBuilder = typeConfig.toBuilder();
                PrefixUtil.removePrefixesFromSchemaType(typeConfigBuilder);
                AppSearchSchema schema = SchemaToProtoConverter.toAppSearchSchema(
                        typeConfigBuilder);

                responseBuilder.setVersion(typeConfig.getVersion());
                responseBuilder.addSchema(schema);

                // Populate visibility info. Since the constructor of VisibilityStore will get
                // schema. Avoid call visibility store before we have already created it.
                if (mVisibilityStoreLocked != null) {
                    String typeName = typeConfig.getSchemaType().substring(typePrefix.length());
                    VisibilityDocument visibilityDocument =
                            mVisibilityStoreLocked.getVisibility(prefixedSchemaType);
                    if (visibilityDocument != null) {
                        if (visibilityDocument.isNotDisplayedBySystem()) {
                            responseBuilder
                                    .addSchemaTypeNotDisplayedBySystem(typeName);
                        }
                        String[] packageNames = visibilityDocument.getPackageNames();
                        byte[][] sha256Certs = visibilityDocument.getSha256Certs();
                        if (packageNames.length != sha256Certs.length) {
                            throw new AppSearchException(RESULT_INTERNAL_ERROR,
                                    "The length of package names and sha256Crets are different!");
                        }
                        if (packageNames.length != 0) {
                            Set<PackageIdentifier> packageIdentifier = new ArraySet<>();
                            for (int j = 0; j < packageNames.length; j++) {
                                packageIdentifier.add(new PackageIdentifier(
                                        packageNames[j], sha256Certs[j]));
                            }
                            responseBuilder.setSchemaTypeVisibleToPackages(typeName,
                                    packageIdentifier);
                        }
                        Set<Set<Integer>> visibleToPermissions =
                                visibilityDocument.getVisibleToPermissions();
                        if (visibleToPermissions != null) {
                            responseBuilder.setRequiredPermissionsForSchemaTypeVisibility(
                                    typeName,  visibleToPermissions);
                        }
                    }
                }
            }
            return responseBuilder.build();

        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Retrieves the list of namespaces with at least one document for this package name, database.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName  Package name that owns this schema
     * @param databaseName The name of the database where this schema lives.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @NonNull
    public List<String> getNamespaces(
            @NonNull String packageName, @NonNull String databaseName) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getAllNamespaces, request");
            // We can't just use mNamespaceMap here because we have no way to prune namespaces from
            // mNamespaceMap when they have no more documents (e.g. after setting schema to empty or
            // using deleteByQuery).
            GetAllNamespacesResultProto getAllNamespacesResultProto =
                    mIcingSearchEngineLocked.getAllNamespaces();
            LogUtil.piiTrace(
                    TAG,
                    "getAllNamespaces, response",
                    getAllNamespacesResultProto.getNamespacesCount(),
                    getAllNamespacesResultProto);
            checkSuccess(getAllNamespacesResultProto.getStatus());
            String prefix = createPrefix(packageName, databaseName);
            List<String> results = new ArrayList<>();
            for (int i = 0; i < getAllNamespacesResultProto.getNamespacesCount(); i++) {
                String prefixedNamespace = getAllNamespacesResultProto.getNamespaces(i);
                if (prefixedNamespace.startsWith(prefix)) {
                    results.add(prefixedNamespace.substring(prefix.length()));
                }
            }
            return results;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Adds a document to the AppSearch index.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName             The package name that owns this document.
     * @param databaseName            The databaseName this document resides in.
     * @param document                The document to index.
     * @param sendChangeNotifications Whether to dispatch
     *                                {@link androidx.appsearch.observer.DocumentChangeInfo}
     *                                messages to observers for this change.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public void putDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull GenericDocument document,
            boolean sendChangeNotifications,
            @Nullable AppSearchLogger logger)
            throws AppSearchException {
        PutDocumentStats.Builder pStatsBuilder = null;
        if (logger != null) {
            pStatsBuilder = new PutDocumentStats.Builder(packageName, databaseName);
        }
        long totalStartTimeMillis = SystemClock.elapsedRealtime();

        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            // Generate Document Proto
            long generateDocumentProtoStartTimeMillis = SystemClock.elapsedRealtime();
            DocumentProto.Builder documentBuilder = GenericDocumentToProtoConverter.toDocumentProto(
                    document).toBuilder();
            long generateDocumentProtoEndTimeMillis = SystemClock.elapsedRealtime();

            // Rewrite Document Type
            long rewriteDocumentTypeStartTimeMillis = SystemClock.elapsedRealtime();
            String prefix = createPrefix(packageName, databaseName);
            addPrefixToDocument(documentBuilder, prefix);
            long rewriteDocumentTypeEndTimeMillis = SystemClock.elapsedRealtime();
            DocumentProto finalDocument = documentBuilder.build();

            // Check limits
            int newDocumentCount = enforceLimitConfigLocked(
                    packageName, finalDocument.getUri(), finalDocument.getSerializedSize());

            // Insert document
            LogUtil.piiTrace(TAG, "putDocument, request", finalDocument.getUri(), finalDocument);
            PutResultProto putResultProto = mIcingSearchEngineLocked.put(finalDocument);
            LogUtil.piiTrace(
                    TAG, "putDocument, response", putResultProto.getStatus(), putResultProto);

            // Update caches
            addToMap(mNamespaceMapLocked, prefix, finalDocument.getNamespace());
            mDocumentCountMapLocked.put(packageName, newDocumentCount);

            // Logging stats
            if (pStatsBuilder != null) {
                pStatsBuilder
                        .setStatusCode(statusProtoToResultCode(putResultProto.getStatus()))
                        .setGenerateDocumentProtoLatencyMillis(
                                (int) (generateDocumentProtoEndTimeMillis
                                        - generateDocumentProtoStartTimeMillis))
                        .setRewriteDocumentTypesLatencyMillis(
                                (int) (rewriteDocumentTypeEndTimeMillis
                                        - rewriteDocumentTypeStartTimeMillis));
                AppSearchLoggerHelper.copyNativeStats(putResultProto.getPutDocumentStats(),
                        pStatsBuilder);
            }

            checkSuccess(putResultProto.getStatus());

            // Prepare notifications
            if (sendChangeNotifications) {
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        document.getNamespace(),
                        document.getSchemaType(),
                        document.getId(),
                        mVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();

            if (pStatsBuilder != null && logger != null) {
                long totalEndTimeMillis = SystemClock.elapsedRealtime();
                pStatsBuilder.setTotalLatencyMillis(
                        (int) (totalEndTimeMillis - totalStartTimeMillis));
                logger.logStats(pStatsBuilder.build());
            }
        }
    }

    /**
     * Checks that a new document can be added to the given packageName with the given serialized
     * size without violating our {@link LimitConfig}.
     *
     * @return the new count of documents for the given package, including the new document.
     * @throws AppSearchException with a code of {@link AppSearchResult#RESULT_OUT_OF_SPACE} if the
     *                            limits are violated by the new document.
     */
    @GuardedBy("mReadWriteLock")
    private int enforceLimitConfigLocked(String packageName, String newDocUri, int newDocSize)
            throws AppSearchException {
        // Limits check: size of document
        if (newDocSize > mLimitConfig.getMaxDocumentSizeBytes()) {
            throw new AppSearchException(
                    AppSearchResult.RESULT_OUT_OF_SPACE,
                    "Document \"" + newDocUri + "\" for package \"" + packageName
                            + "\" serialized to " + newDocSize + " bytes, which exceeds "
                            + "limit of " + mLimitConfig.getMaxDocumentSizeBytes() + " bytes");
        }

        // Limits check: number of documents
        Integer oldDocumentCount = mDocumentCountMapLocked.get(packageName);
        int newDocumentCount;
        if (oldDocumentCount == null) {
            newDocumentCount = 1;
        } else {
            newDocumentCount = oldDocumentCount + 1;
        }
        if (newDocumentCount > mLimitConfig.getMaxDocumentCount()) {
            // Our management of mDocumentCountMapLocked doesn't account for document
            // replacements, so our counter might have overcounted if the app has replaced docs.
            // Rebuild the counter from StorageInfo in case this is so.
            // TODO(b/170371356):  If Icing lib exposes something in the result which says
            //  whether the document was a replacement, we could subtract 1 again after the put
            //  to keep the count accurate. That would allow us to remove this code.
            rebuildDocumentCountMapLocked(getRawStorageInfoProto());
            oldDocumentCount = mDocumentCountMapLocked.get(packageName);
            if (oldDocumentCount == null) {
                newDocumentCount = 1;
            } else {
                newDocumentCount = oldDocumentCount + 1;
            }
        }
        if (newDocumentCount > mLimitConfig.getMaxDocumentCount()) {
            // Now we really can't fit it in, even accounting for replacements.
            throw new AppSearchException(
                    AppSearchResult.RESULT_OUT_OF_SPACE,
                    "Package \"" + packageName + "\" exceeded limit of "
                            + mLimitConfig.getMaxDocumentCount() + " documents. Some documents "
                            + "must be removed to index additional ones.");
        }

        return newDocumentCount;
    }

    /**
     * Retrieves a document from the AppSearch index by namespace and document ID from any
     * application the caller is allowed to view
     *
     * <p>This method will handle both Icing engine errors as well as permission errors by
     * throwing an obfuscated RESULT_NOT_FOUND exception. This is done so the caller doesn't
     * receive information on whether or not a file they are not allowed to access exists or not.
     * This is different from the behavior of {@link #getDocument}.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @param callerAccess      Visibility access info of the calling app
     * @return The Document contents
     * @throws AppSearchException on IcingSearchEngine error or invalid permissions
     */
    @NonNull
    public GenericDocument globalGetDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths,
            @NonNull CallerAccess callerAccess) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            // We retrieve the document before checking for access, as we do not know which
            // schema the document is under. Schema is required for checking access
            DocumentProto documentProto;
            try {
                documentProto = getDocumentProtoByIdLocked(packageName, databaseName,
                        namespace, id, typePropertyPaths);

                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        callerAccess,
                        packageName,
                        documentProto.getSchema(),
                        mVisibilityStoreLocked,
                        mVisibilityCheckerLocked)) {
                    throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND);
                }
            } catch (AppSearchException e) {
                // Not passing cause in AppSearchException as that violates privacy guarantees as
                // user could differentiate between document not existing and not having access.
                throw new AppSearchException(AppSearchResult.RESULT_NOT_FOUND,
                        "Document (" + namespace + ", " + id + ") not found.");
            }

            DocumentProto.Builder documentBuilder = documentProto.toBuilder();
            removePrefixesFromDocument(documentBuilder);
            String prefix = createPrefix(packageName, databaseName);
            Map<String, SchemaTypeConfigProto> schemaTypeMap =
                    Preconditions.checkNotNull(mSchemaMapLocked.get(prefix));
            return GenericDocumentToProtoConverter.toGenericDocument(documentBuilder.build(),
                    prefix, schemaTypeMap);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Retrieves a document from the AppSearch index by namespace and document ID.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @return The Document contents
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @NonNull
    public GenericDocument getDocument(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            DocumentProto documentProto = getDocumentProtoByIdLocked(packageName, databaseName,
                    namespace, id, typePropertyPaths);
            DocumentProto.Builder documentBuilder = documentProto.toBuilder();
            removePrefixesFromDocument(documentBuilder);

            String prefix = createPrefix(packageName, databaseName);
            // The schema type map cannot be null at this point. It could only be null if no
            // schema had ever been set for that prefix. Given we have retrieved a document from
            // the index, we know a schema had to have been set.
            Map<String, SchemaTypeConfigProto> schemaTypeMap =
                    Preconditions.checkNotNull(mSchemaMapLocked.get(prefix));
            return GenericDocumentToProtoConverter.toGenericDocument(documentBuilder.build(),
                    prefix, schemaTypeMap);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns a DocumentProto from Icing.
     *
     * @param packageName       The package that owns this document.
     * @param databaseName      The databaseName this document resides in.
     * @param namespace         The namespace this document resides in.
     * @param id                The ID of the document to get.
     * @param typePropertyPaths A map of schema type to a list of property paths to return in the
     *                          result.
     * @return the DocumentProto object
     * @throws AppSearchException on IcingSearchEngine error
     */
    @NonNull
    @GuardedBy("mReadWriteLock")
    private DocumentProto getDocumentProtoByIdLocked(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String id,
            @NonNull Map<String, List<String>> typePropertyPaths)
            throws AppSearchException {
        String prefix = createPrefix(packageName, databaseName);
        List<TypePropertyMask.Builder> nonPrefixedPropertyMaskBuilders =
                TypePropertyPathToProtoConverter
                        .toTypePropertyMaskBuilderList(typePropertyPaths);
        List<TypePropertyMask> prefixedPropertyMasks =
                new ArrayList<>(nonPrefixedPropertyMaskBuilders.size());
        for (int i = 0; i < nonPrefixedPropertyMaskBuilders.size(); ++i) {
            String nonPrefixedType = nonPrefixedPropertyMaskBuilders.get(i).getSchemaType();
            String prefixedType = nonPrefixedType.equals(
                    GetByDocumentIdRequest.PROJECTION_SCHEMA_TYPE_WILDCARD)
                    ? nonPrefixedType : prefix + nonPrefixedType;
            prefixedPropertyMasks.add(
                    nonPrefixedPropertyMaskBuilders.get(i).setSchemaType(prefixedType).build());
        }
        GetResultSpecProto getResultSpec =
                GetResultSpecProto.newBuilder().addAllTypePropertyMasks(prefixedPropertyMasks)
                        .build();

        String finalNamespace = createPrefix(packageName, databaseName) + namespace;
        if (LogUtil.isPiiTraceEnabled()) {
            LogUtil.piiTrace(
                    TAG, "getDocument, request", finalNamespace + ", " + id + "," + getResultSpec);
        }
        GetResultProto getResultProto =
                mIcingSearchEngineLocked.get(finalNamespace, id, getResultSpec);
        LogUtil.piiTrace(TAG, "getDocument, response", getResultProto.getStatus(), getResultProto);
        checkSuccess(getResultProto.getStatus());

        return getResultProto.getDocument();
    }

    /**
     * Executes a query against the AppSearch index and returns results.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName     The package name that is performing the query.
     * @param databaseName    The databaseName this query for.
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @param logger          logger to collect query stats
     * @return The results of performing this search. It may contain an empty list of results if
     * no documents matched the query.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @NonNull
    public SearchResultPage query(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @Nullable AppSearchLogger logger) throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchStats.Builder sStatsBuilder = null;
        if (logger != null) {
            sStatsBuilder =
                    new SearchStats.Builder(SearchStats.VISIBILITY_SCOPE_LOCAL,
                            packageName).setDatabase(databaseName);
        }

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            List<String> filterPackageNames = searchSpec.getFilterPackageNames();
            if (!filterPackageNames.isEmpty() && !filterPackageNames.contains(packageName)) {
                // Client wanted to query over some packages that weren't its own. This isn't
                // allowed through local query so we can return early with no results.
                if (sStatsBuilder != null && logger != null) {
                    sStatsBuilder.setStatusCode(AppSearchResult.RESULT_SECURITY_ERROR);
                }
                return new SearchResultPage(Bundle.EMPTY);
            }

            String prefix = createPrefix(packageName, databaseName);
            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec,
                            Collections.singleton(prefix), mNamespaceMapLocked, mSchemaMapLocked);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new SearchResultPage(Bundle.EMPTY);
            }

            SearchResultPage searchResultPage =
                    doQueryLocked(
                            searchSpecToProtoConverter,
                            sStatsBuilder);
            addNextPageToken(packageName, searchResultPage.getNextPageToken());
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();
            if (sStatsBuilder != null && logger != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
                logger.logStats(sStatsBuilder.build());
            }
        }
    }

    /**
     * Executes a global query, i.e. over all permitted prefixes, against the AppSearch index and
     * returns results.
     *
     * <p>This method belongs to query group.
     *
     * @param queryExpression Query String to search.
     * @param searchSpec      Spec for setting filters, raw query etc.
     * @param callerAccess    Visibility access info of the calling app
     * @param logger          logger to collect globalQuery stats
     * @return The results of performing this search. It may contain an empty list of results if
     * no documents matched the query.
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @NonNull
    public SearchResultPage globalQuery(
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @NonNull CallerAccess callerAccess,
            @Nullable AppSearchLogger logger) throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchStats.Builder sStatsBuilder = null;
        if (logger != null) {
            sStatsBuilder =
                    new SearchStats.Builder(
                            SearchStats.VISIBILITY_SCOPE_GLOBAL,
                            callerAccess.getCallingPackageName());
        }

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            long aclLatencyStartMillis = SystemClock.elapsedRealtime();
            // Convert package filters to prefix filters
            Set<String> packageFilters = new ArraySet<>(searchSpec.getFilterPackageNames());
            Set<String> prefixFilters = new ArraySet<>();
            if (packageFilters.isEmpty()) {
                // Client didn't restrict their search over packages. Try to query over all
                // packages/prefixes
                prefixFilters = mNamespaceMapLocked.keySet();
            } else {
                // Client did restrict their search over packages. Only include the prefixes that
                // belong to the specified packages.
                for (String prefix : mNamespaceMapLocked.keySet()) {
                    String packageName = getPackageName(prefix);
                    if (packageFilters.contains(packageName)) {
                        prefixFilters.add(prefix);
                    }
                }
            }
            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec, prefixFilters,
                            mNamespaceMapLocked, mSchemaMapLocked);
            // Remove those inaccessible schemas.
            searchSpecToProtoConverter.removeInaccessibleSchemaFilter(
                    callerAccess, mVisibilityStoreLocked, mVisibilityCheckerLocked);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new SearchResultPage(Bundle.EMPTY);
            }
            if (sStatsBuilder != null) {
                sStatsBuilder.setAclCheckLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - aclLatencyStartMillis));
            }
            SearchResultPage searchResultPage =
                    doQueryLocked(
                            searchSpecToProtoConverter,
                            sStatsBuilder);
            addNextPageToken(
                    callerAccess.getCallingPackageName(), searchResultPage.getNextPageToken());
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();

            if (sStatsBuilder != null && logger != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
                logger.logStats(sStatsBuilder.build());
            }
        }
    }

    @GuardedBy("mReadWriteLock")
    private SearchResultPage doQueryLocked(
            @NonNull SearchSpecToProtoConverter searchSpecToProtoConverter,
            @Nullable SearchStats.Builder sStatsBuilder)
            throws AppSearchException {
        // Rewrite the given SearchSpec into SearchSpecProto, ResultSpecProto and ScoringSpecProto.
        // All processes are counted in rewriteSearchSpecLatencyMillis
        long rewriteSearchSpecLatencyStartMillis = SystemClock.elapsedRealtime();
        SearchSpecProto finalSearchSpec = searchSpecToProtoConverter.toSearchSpecProto();
        ResultSpecProto finalResultSpec = searchSpecToProtoConverter.toResultSpecProto(
                mNamespaceMapLocked, mSchemaMapLocked);
        ScoringSpecProto scoringSpec = searchSpecToProtoConverter.toScoringSpecProto();
        if (sStatsBuilder != null) {
            sStatsBuilder.setRewriteSearchSpecLatencyMillis((int)
                    (SystemClock.elapsedRealtime() - rewriteSearchSpecLatencyStartMillis));
        }

        // Send request to Icing.
        SearchResultProto searchResultProto = searchInIcingLocked(
                finalSearchSpec, finalResultSpec, scoringSpec, sStatsBuilder);

        long rewriteSearchResultLatencyStartMillis = SystemClock.elapsedRealtime();
        // Rewrite search result before we return.
        SearchResultPage searchResultPage = SearchResultToProtoConverter
                .toSearchResultPage(searchResultProto, mSchemaMapLocked);
        if (sStatsBuilder != null) {
            sStatsBuilder.setRewriteSearchResultLatencyMillis(
                    (int) (SystemClock.elapsedRealtime()
                            - rewriteSearchResultLatencyStartMillis));
        }
        return searchResultPage;
    }

    @GuardedBy("mReadWriteLock")
    private SearchResultProto searchInIcingLocked(
            @NonNull SearchSpecProto searchSpec,
            @NonNull ResultSpecProto resultSpec,
            @NonNull ScoringSpecProto scoringSpec,
            @Nullable SearchStats.Builder sStatsBuilder) throws AppSearchException {
        if (LogUtil.isPiiTraceEnabled()) {
            LogUtil.piiTrace(
                    TAG,
                    "search, request",
                    searchSpec.getQuery(),
                    searchSpec + ", " + scoringSpec + ", " + resultSpec);
        }
        SearchResultProto searchResultProto = mIcingSearchEngineLocked.search(
                searchSpec, scoringSpec, resultSpec);
        LogUtil.piiTrace(
                TAG, "search, response", searchResultProto.getResultsCount(), searchResultProto);
        if (sStatsBuilder != null) {
            sStatsBuilder.setStatusCode(statusProtoToResultCode(searchResultProto.getStatus()));
            AppSearchLoggerHelper.copyNativeStats(searchResultProto.getQueryStats(), sStatsBuilder);
        }
        checkSuccess(searchResultProto.getStatus());
        return searchResultProto;
    }

    /**
     * Generates suggestions based on the given search prefix.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName               The package name that is performing the query.
     * @param databaseName              The databaseName this query for.
     * @param suggestionQueryExpression The non-empty query expression used to be completed.
     * @param searchSuggestionSpec      Spec for setting filters.
     * @return a List of {@link SearchSuggestionResult}. The returned {@link SearchSuggestionResult}
     *      are order by the number of {@link androidx.appsearch.app.SearchResult} you could get
     *      by using that suggestion in {@link #query}.
     * @throws AppSearchException if the suggestionQueryExpression is empty.
     */
    @NonNull
    public List<SearchSuggestionResult> searchSuggestion(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String suggestionQueryExpression,
            @NonNull SearchSuggestionSpec searchSuggestionSpec) throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            if (suggestionQueryExpression.isEmpty()) {
                throw new AppSearchException(
                        AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "suggestionQueryExpression cannot be empty.");
            }
            if (searchSuggestionSpec.getMaximumResultCount()
                    > mLimitConfig.getMaxSuggestionCount()) {
                throw new AppSearchException(
                        AppSearchResult.RESULT_INVALID_ARGUMENT,
                        "Trying to get " + searchSuggestionSpec.getMaximumResultCount()
                                + " suggestion results, which exceeds limit of "
                                + mLimitConfig.getMaxSuggestionCount());
            }

            String prefix = createPrefix(packageName, databaseName);
            SearchSuggestionSpecToProtoConverter searchSuggestionSpecToProtoConverter =
                    new SearchSuggestionSpecToProtoConverter(suggestionQueryExpression,
                            searchSuggestionSpec,
                            Collections.singleton(prefix),
                            mNamespaceMapLocked,
                            mSchemaMapLocked);

            if (searchSuggestionSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return an
                // empty SearchResult and skip sending request to Icing.
                return new ArrayList<>();
            }

            SuggestionResponse response = mIcingSearchEngineLocked.searchSuggestions(
                    searchSuggestionSpecToProtoConverter.toSearchSuggestionSpecProto());

            checkSuccess(response.getStatus());
            List<SearchSuggestionResult> suggestions =
                    new ArrayList<>(response.getSuggestionsCount());
            for (int i = 0; i < response.getSuggestionsCount(); i++) {
                suggestions.add(new SearchSuggestionResult.Builder()
                        .setSuggestedResult(response.getSuggestions(i).getQuery())
                        .build());
            }
            return suggestions;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns a mapping of package names to all the databases owned by that package.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    @NonNull
    public Map<String, Set<String>> getPackageToDatabases() {
        mReadWriteLock.readLock().lock();
        try {
            Map<String, Set<String>> packageToDatabases = new ArrayMap<>();
            for (String prefix : mSchemaMapLocked.keySet()) {
                String packageName = getPackageName(prefix);

                Set<String> databases = packageToDatabases.get(packageName);
                if (databases == null) {
                    databases = new ArraySet<>();
                    packageToDatabases.put(packageName, databases);
                }

                String databaseName = getDatabaseName(prefix);
                databases.add(databaseName);
            }

            return packageToDatabases;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Fetches the next page of results of a previously executed query. Results can be empty if
     * next-page token is invalid or all pages have been returned.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName   Package name of the caller.
     * @param nextPageToken The token of pre-loaded results of previously executed query.
     * @return The next page of results of previously executed query.
     * @throws AppSearchException on IcingSearchEngine error or if can't advance on nextPageToken.
     */
    @NonNull
    public SearchResultPage getNextPage(@NonNull String packageName, long nextPageToken,
            @Nullable SearchStats.Builder sStatsBuilder)
            throws AppSearchException {
        long totalLatencyStartMillis = SystemClock.elapsedRealtime();

        long javaLockAcquisitionLatencyStartMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.readLock().lock();
        try {
            if (sStatsBuilder != null) {
                sStatsBuilder.setJavaLockAcquisitionLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - javaLockAcquisitionLatencyStartMillis));
            }
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "getNextPage, request", nextPageToken);
            checkNextPageToken(packageName, nextPageToken);
            SearchResultProto searchResultProto = mIcingSearchEngineLocked.getNextPage(
                    nextPageToken);

            if (sStatsBuilder != null) {
                sStatsBuilder.setStatusCode(statusProtoToResultCode(searchResultProto.getStatus()));
                AppSearchLoggerHelper.copyNativeStats(searchResultProto.getQueryStats(),
                        sStatsBuilder);
            }

            LogUtil.piiTrace(
                    TAG,
                    "getNextPage, response",
                    searchResultProto.getResultsCount(),
                    searchResultProto);
            checkSuccess(searchResultProto.getStatus());
            if (nextPageToken != EMPTY_PAGE_TOKEN
                    && searchResultProto.getNextPageToken() == EMPTY_PAGE_TOKEN) {
                // At this point, we're guaranteed that this nextPageToken exists for this package,
                // otherwise checkNextPageToken would've thrown an exception.
                // Since the new token is 0, this is the last page. We should remove the old token
                // from our cache since it no longer refers to this query.
                synchronized (mNextPageTokensLocked) {
                    Set<Long> nextPageTokensForPackage =
                            Preconditions.checkNotNull(mNextPageTokensLocked.get(packageName));
                    nextPageTokensForPackage.remove(nextPageToken);
                }
            }
            long rewriteSearchResultLatencyStartMillis = SystemClock.elapsedRealtime();
            // Rewrite search result before we return.
            SearchResultPage searchResultPage = SearchResultToProtoConverter
                    .toSearchResultPage(searchResultProto, mSchemaMapLocked);
            if (sStatsBuilder != null) {
                sStatsBuilder.setRewriteSearchResultLatencyMillis(
                        (int) (SystemClock.elapsedRealtime()
                                - rewriteSearchResultLatencyStartMillis));
            }
            return searchResultPage;
        } finally {
            mReadWriteLock.readLock().unlock();
            if (sStatsBuilder != null) {
                sStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartMillis));
            }
        }
    }

    /**
     * Invalidates the next-page token so that no more results of the related query can be returned.
     *
     * <p>This method belongs to query group.
     *
     * @param packageName   Package name of the caller.
     * @param nextPageToken The token of pre-loaded results of previously executed query to be
     *                      Invalidated.
     * @throws AppSearchException if nextPageToken is unusable.
     */
    public void invalidateNextPageToken(@NonNull String packageName, long nextPageToken)
            throws AppSearchException {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // (b/208305352) Directly return here since we are no longer caching EMPTY_PAGE_TOKEN
            // in the cached token set. So no need to remove it anymore.
            return;
        }

        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "invalidateNextPageToken, request", nextPageToken);
            checkNextPageToken(packageName, nextPageToken);
            mIcingSearchEngineLocked.invalidateNextPageToken(nextPageToken);

            synchronized (mNextPageTokensLocked) {
                Set<Long> tokens = mNextPageTokensLocked.get(packageName);
                if (tokens != null) {
                    tokens.remove(nextPageToken);
                } else {
                    Log.e(TAG, "Failed to invalidate token " + nextPageToken + ": tokens are not "
                            + "cached.");
                }
            }
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /** Reports a usage of the given document at the given timestamp. */
    public void reportUsage(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String documentId,
            long usageTimestampMillis,
            boolean systemUsage) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            String prefixedNamespace = createPrefix(packageName, databaseName) + namespace;
            UsageReport.UsageType usageType = systemUsage
                    ? UsageReport.UsageType.USAGE_TYPE2 : UsageReport.UsageType.USAGE_TYPE1;
            UsageReport report = UsageReport.newBuilder()
                    .setDocumentNamespace(prefixedNamespace)
                    .setDocumentUri(documentId)
                    .setUsageTimestampMs(usageTimestampMillis)
                    .setUsageType(usageType)
                    .build();

            LogUtil.piiTrace(TAG, "reportUsage, request", report.getDocumentUri(), report);
            ReportUsageResultProto result = mIcingSearchEngineLocked.reportUsage(report);
            LogUtil.piiTrace(TAG, "reportUsage, response", result.getStatus(), result);
            checkSuccess(result.getStatus());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Removes the given document by id.
     *
     * <p>This method belongs to mutate group.
     *
     * @param packageName        The package name that owns the document.
     * @param databaseName       The databaseName the document is in.
     * @param namespace          Namespace of the document to remove.
     * @param documentId         ID of the document to remove.
     * @param removeStatsBuilder builder for {@link RemoveStats} to hold stats for remove
     * @throws AppSearchException on IcingSearchEngine error.
     */
    public void remove(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String documentId,
            @Nullable RemoveStats.Builder removeStatsBuilder) throws AppSearchException {
        long totalLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            String prefixedNamespace = createPrefix(packageName, databaseName) + namespace;
            String schemaType = null;
            if (mObserverManager.isPackageObserved(packageName)) {
                // Someone might be observing the type this document is under, but we have no way to
                // know its type without retrieving it. Do so now.
                // TODO(b/193494000): If Icing Lib can return information about the deleted
                //  document's type we can remove this code.
                if (LogUtil.isPiiTraceEnabled()) {
                    LogUtil.piiTrace(
                            TAG, "removeById, getRequest", prefixedNamespace + ", " + documentId);
                }
                GetResultProto getResult = mIcingSearchEngineLocked.get(
                        prefixedNamespace, documentId, GET_RESULT_SPEC_NO_PROPERTIES);
                LogUtil.piiTrace(TAG, "removeById, getResponse", getResult.getStatus(), getResult);
                checkSuccess(getResult.getStatus());
                schemaType = PrefixUtil.removePrefix(getResult.getDocument().getSchema());
            }

            if (LogUtil.isPiiTraceEnabled()) {
                LogUtil.piiTrace(TAG, "removeById, request", prefixedNamespace + ", " + documentId);
            }
            DeleteResultProto deleteResultProto =
                    mIcingSearchEngineLocked.delete(prefixedNamespace, documentId);
            LogUtil.piiTrace(
                    TAG, "removeById, response", deleteResultProto.getStatus(), deleteResultProto);

            if (removeStatsBuilder != null) {
                removeStatsBuilder.setStatusCode(statusProtoToResultCode(
                        deleteResultProto.getStatus()));
                AppSearchLoggerHelper.copyNativeStats(deleteResultProto.getDeleteStats(),
                        removeStatsBuilder);
            }
            checkSuccess(deleteResultProto.getStatus());

            // Update derived maps
            updateDocumentCountAfterRemovalLocked(packageName, /*numDocumentsDeleted=*/ 1);

            // Prepare notifications
            if (schemaType != null) {
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        namespace,
                        schemaType,
                        documentId,
                        mVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
            if (removeStatsBuilder != null) {
                removeStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartTimeMillis));
            }
        }
    }

    /**
     * Removes documents by given query.
     *
     * <p>This method belongs to mutate group.
     *
     * <p> {@link SearchSpec} objects containing a {@link JoinSpec} are not allowed here.
     *
     * @param packageName        The package name that owns the documents.
     * @param databaseName       The databaseName the document is in.
     * @param queryExpression    Query String to search.
     * @param searchSpec         Defines what and how to remove
     * @param removeStatsBuilder builder for {@link RemoveStats} to hold stats for remove
     * @throws AppSearchException on IcingSearchEngine error.
     * @throws IllegalArgumentException if the {@link SearchSpec} contains a {@link JoinSpec}.
     */
    public void removeByQuery(@NonNull String packageName, @NonNull String databaseName,
            @NonNull String queryExpression,
            @NonNull SearchSpec searchSpec,
            @Nullable RemoveStats.Builder removeStatsBuilder)
            throws AppSearchException {
        if (searchSpec.getJoinSpec() != null) {
            throw new IllegalArgumentException("JoinSpec not allowed in removeByQuery, but "
                    + "JoinSpec was provided");
        }

        long totalLatencyStartTimeMillis = SystemClock.elapsedRealtime();
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            List<String> filterPackageNames = searchSpec.getFilterPackageNames();
            if (!filterPackageNames.isEmpty() && !filterPackageNames.contains(packageName)) {
                // We're only removing documents within the parameter `packageName`. If we're not
                // restricting our remove-query to this package name, then there's nothing for us to
                // remove.
                return;
            }

            String prefix = createPrefix(packageName, databaseName);
            if (!mNamespaceMapLocked.containsKey(prefix)) {
                // The target database is empty so we can return early and skip sending request to
                // Icing.
                return;
            }

            SearchSpecToProtoConverter searchSpecToProtoConverter =
                    new SearchSpecToProtoConverter(queryExpression, searchSpec,
                            Collections.singleton(prefix), mNamespaceMapLocked, mSchemaMapLocked);
            if (searchSpecToProtoConverter.hasNothingToSearch()) {
                // there is nothing to search over given their search filters, so we can return
                // early and skip sending request to Icing.
                return;
            }

            SearchSpecProto finalSearchSpec = searchSpecToProtoConverter.toSearchSpecProto();

            Set<String> prefixedObservedSchemas = null;
            if (mObserverManager.isPackageObserved(packageName)) {
                prefixedObservedSchemas = new ArraySet<>();
                List<String> prefixedTargetSchemaTypes =
                        finalSearchSpec.getSchemaTypeFiltersList();
                for (int i = 0; i < prefixedTargetSchemaTypes.size(); i++) {
                    String prefixedType = prefixedTargetSchemaTypes.get(i);
                    String shortTypeName = PrefixUtil.removePrefix(prefixedType);
                    if (mObserverManager.isSchemaTypeObserved(packageName, shortTypeName)) {
                        prefixedObservedSchemas.add(prefixedType);
                    }
                }
            }

            doRemoveByQueryLocked(
                    packageName, finalSearchSpec, prefixedObservedSchemas, removeStatsBuilder);

        } finally {
            mReadWriteLock.writeLock().unlock();
            if (removeStatsBuilder != null) {
                removeStatsBuilder.setTotalLatencyMillis(
                        (int) (SystemClock.elapsedRealtime() - totalLatencyStartTimeMillis));
            }
        }
    }

    /**
     * Executes removeByQuery.
     *
     * <p>Change notifications will be created if prefixedObservedSchemas is not null.
     *
     * @param packageName             The package name that owns the documents.
     * @param finalSearchSpec         The final search spec that has been written through
     *                                {@link SearchSpecToProtoConverter}.
     * @param prefixedObservedSchemas The set of prefixed schemas that have valid registered
     *                                observers. Only changes to schemas in this set will be queued.
     */
    @GuardedBy("mReadWriteLock")
    private void doRemoveByQueryLocked(
            @NonNull String packageName,
            @NonNull SearchSpecProto finalSearchSpec,
            @Nullable Set<String> prefixedObservedSchemas,
            @Nullable RemoveStats.Builder removeStatsBuilder) throws AppSearchException {
        LogUtil.piiTrace(TAG, "removeByQuery, request", finalSearchSpec);
        boolean returnDeletedDocumentInfo =
                prefixedObservedSchemas != null && !prefixedObservedSchemas.isEmpty();
        DeleteByQueryResultProto deleteResultProto =
                mIcingSearchEngineLocked.deleteByQuery(finalSearchSpec,
                        returnDeletedDocumentInfo);
        LogUtil.piiTrace(
                TAG, "removeByQuery, response", deleteResultProto.getStatus(), deleteResultProto);

        if (removeStatsBuilder != null) {
            removeStatsBuilder.setStatusCode(statusProtoToResultCode(
                    deleteResultProto.getStatus()));
            // TODO(b/187206766) also log query stats here once IcingLib returns it
            AppSearchLoggerHelper.copyNativeStats(deleteResultProto.getDeleteByQueryStats(),
                    removeStatsBuilder);
        }

        // It seems that the caller wants to get success if the data matching the query is
        // not in the DB because it was not there or was successfully deleted.
        checkCodeOneOf(deleteResultProto.getStatus(),
                StatusProto.Code.OK, StatusProto.Code.NOT_FOUND);

        // Update derived maps
        int numDocumentsDeleted =
                deleteResultProto.getDeleteByQueryStats().getNumDocumentsDeleted();
        updateDocumentCountAfterRemovalLocked(packageName, numDocumentsDeleted);

        if (prefixedObservedSchemas != null && !prefixedObservedSchemas.isEmpty()) {
            dispatchChangeNotificationsAfterRemoveByQueryLocked(packageName,
                    deleteResultProto, prefixedObservedSchemas);
        }
    }

    @GuardedBy("mReadWriteLock")
    private void updateDocumentCountAfterRemovalLocked(
            @NonNull String packageName, int numDocumentsDeleted) {
        if (numDocumentsDeleted > 0) {
            Integer oldDocumentCount = mDocumentCountMapLocked.get(packageName);
            // This should always be true: how can we delete documents for a package without
            // having seen that package during init? This is just a safeguard.
            if (oldDocumentCount != null) {
                // This should always be >0; how can we remove more documents than we've indexed?
                // This is just a safeguard.
                int newDocumentCount = Math.max(oldDocumentCount - numDocumentsDeleted, 0);
                mDocumentCountMapLocked.put(packageName, newDocumentCount);
            }
        }
    }

    @GuardedBy("mReadWriteLock")
    private void dispatchChangeNotificationsAfterRemoveByQueryLocked(
            @NonNull String packageName,
            @NonNull DeleteByQueryResultProto deleteResultProto,
            @NonNull Set<String> prefixedObservedSchemas
    ) throws AppSearchException {
        for (int i = 0; i < deleteResultProto.getDeletedDocumentsCount(); ++i) {
            DeleteByQueryResultProto.DocumentGroupInfo group =
                    deleteResultProto.getDeletedDocuments(i);
            if (!prefixedObservedSchemas.contains(group.getSchema())) {
                continue;
            }
            String databaseName = PrefixUtil.getDatabaseName(group.getNamespace());
            String namespace = PrefixUtil.removePrefix(group.getNamespace());
            String schemaType = PrefixUtil.removePrefix(group.getSchema());
            for (int j = 0; j < group.getUrisCount(); ++j) {
                String uri = group.getUris(j);
                mObserverManager.onDocumentChange(
                        packageName,
                        databaseName,
                        namespace,
                        schemaType,
                        uri,
                        mVisibilityStoreLocked,
                        mVisibilityCheckerLocked);
            }
        }
    }

    /** Estimates the storage usage info for a specific package. */
    @NonNull
    public StorageInfo getStorageInfoForPackage(@NonNull String packageName)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            Map<String, Set<String>> packageToDatabases = getPackageToDatabases();
            Set<String> databases = packageToDatabases.get(packageName);
            if (databases == null) {
                // Package doesn't exist, no storage info to report
                return new StorageInfo.Builder().build();
            }

            // Accumulate all the namespaces we're interested in.
            Set<String> wantedPrefixedNamespaces = new ArraySet<>();
            for (String database : databases) {
                Set<String> prefixedNamespaces = mNamespaceMapLocked.get(createPrefix(packageName,
                        database));
                if (prefixedNamespaces != null) {
                    wantedPrefixedNamespaces.addAll(prefixedNamespaces);
                }
            }
            if (wantedPrefixedNamespaces.isEmpty()) {
                return new StorageInfo.Builder().build();
            }

            return getStorageInfoForNamespaces(getRawStorageInfoProto(),
                    wantedPrefixedNamespaces);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /** Estimates the storage usage info for a specific database in a package. */
    @NonNull
    public StorageInfo getStorageInfoForDatabase(@NonNull String packageName,
            @NonNull String databaseName)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();

            Map<String, Set<String>> packageToDatabases = getPackageToDatabases();
            Set<String> databases = packageToDatabases.get(packageName);
            if (databases == null) {
                // Package doesn't exist, no storage info to report
                return new StorageInfo.Builder().build();
            }
            if (!databases.contains(databaseName)) {
                // Database doesn't exist, no storage info to report
                return new StorageInfo.Builder().build();
            }

            Set<String> wantedPrefixedNamespaces =
                    mNamespaceMapLocked.get(createPrefix(packageName, databaseName));
            if (wantedPrefixedNamespaces == null || wantedPrefixedNamespaces.isEmpty()) {
                return new StorageInfo.Builder().build();
            }

            return getStorageInfoForNamespaces(getRawStorageInfoProto(),
                    wantedPrefixedNamespaces);
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Returns the native storage info capsuled in {@link StorageInfoResultProto} directly from
     * IcingSearchEngine.
     */
    @NonNull
    public StorageInfoProto getRawStorageInfoProto() throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getStorageInfo, request");
            StorageInfoResultProto storageInfoResult = mIcingSearchEngineLocked.getStorageInfo();
            LogUtil.piiTrace(
                    TAG,
                    "getStorageInfo, response", storageInfoResult.getStatus(), storageInfoResult);
            checkSuccess(storageInfoResult.getStatus());
            return storageInfoResult.getStorageInfo();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Extracts and returns {@link StorageInfo} from {@link StorageInfoProto} based on
     * prefixed namespaces.
     */
    @NonNull
    private static StorageInfo getStorageInfoForNamespaces(
            @NonNull StorageInfoProto storageInfoProto,
            @NonNull Set<String> prefixedNamespaces) {
        if (!storageInfoProto.hasDocumentStorageInfo()) {
            return new StorageInfo.Builder().build();
        }

        long totalStorageSize = storageInfoProto.getTotalStorageSize();
        DocumentStorageInfoProto documentStorageInfo =
                storageInfoProto.getDocumentStorageInfo();
        int totalDocuments =
                documentStorageInfo.getNumAliveDocuments()
                        + documentStorageInfo.getNumExpiredDocuments();

        if (totalStorageSize == 0 || totalDocuments == 0) {
            // Maybe we can exit early and also avoid a divide by 0 error.
            return new StorageInfo.Builder().build();
        }

        // Accumulate stats across the package's namespaces.
        int aliveDocuments = 0;
        int expiredDocuments = 0;
        int aliveNamespaces = 0;
        List<NamespaceStorageInfoProto> namespaceStorageInfos =
                documentStorageInfo.getNamespaceStorageInfoList();
        for (int i = 0; i < namespaceStorageInfos.size(); i++) {
            NamespaceStorageInfoProto namespaceStorageInfo = namespaceStorageInfos.get(i);
            // The namespace from icing lib is already the prefixed format
            if (prefixedNamespaces.contains(namespaceStorageInfo.getNamespace())) {
                if (namespaceStorageInfo.getNumAliveDocuments() > 0) {
                    aliveNamespaces++;
                    aliveDocuments += namespaceStorageInfo.getNumAliveDocuments();
                }
                expiredDocuments += namespaceStorageInfo.getNumExpiredDocuments();
            }
        }
        int namespaceDocuments = aliveDocuments + expiredDocuments;

        // Since we don't have the exact size of all the documents, we do an estimation. Note
        // that while the total storage takes into account schema, index, etc. in addition to
        // documents, we'll only calculate the percentage based on number of documents a
        // client has.
        return new StorageInfo.Builder()
                .setSizeBytes((long) (namespaceDocuments * 1.0 / totalDocuments * totalStorageSize))
                .setAliveDocumentsCount(aliveDocuments)
                .setAliveNamespacesCount(aliveNamespaces)
                .build();
    }

    /**
     * Returns the native debug info capsuled in {@link DebugInfoResultProto} directly from
     * IcingSearchEngine.
     *
     * @param verbosity The verbosity of the debug info. {@link DebugInfoVerbosity.Code#BASIC}
     *                  will return the simplest debug information.
     *                  {@link DebugInfoVerbosity.Code#DETAILED} will return more detailed
     *                  debug information as indicated in the comments in debug.proto
     */
    @NonNull
    public DebugInfoProto getRawDebugInfoProto(@NonNull DebugInfoVerbosity.Code verbosity)
            throws AppSearchException {
        mReadWriteLock.readLock().lock();
        try {
            throwIfClosedLocked();
            LogUtil.piiTrace(TAG, "getDebugInfo, request");
            DebugInfoResultProto debugInfoResult = mIcingSearchEngineLocked.getDebugInfo(
                    verbosity);
            LogUtil.piiTrace(TAG, "getDebugInfo, response", debugInfoResult.getStatus(),
                    debugInfoResult);
            checkSuccess(debugInfoResult.getStatus());
            return debugInfoResult.getDebugInfo();
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Persists all update/delete requests to the disk.
     *
     * <p>If the app crashes after a call to PersistToDisk with {@link PersistType.Code#FULL}, Icing
     * would be able to fully recover all data written up to this point without a costly recovery
     * process.
     *
     * <p>If the app crashes after a call to PersistToDisk with {@link PersistType.Code#LITE}, Icing
     * would trigger a costly recovery process in next initialization. After that, Icing would still
     * be able to recover all written data - excepting Usage data. Usage data is only guaranteed
     * to be safe after a call to PersistToDisk with {@link PersistType.Code#FULL}
     *
     * <p>If the app crashes after an update/delete request has been made, but before any call to
     * PersistToDisk, then all data in Icing will be lost.
     *
     * @param persistType the amount of data to persist. {@link PersistType.Code#LITE} will only
     *                    persist the minimal amount of data to ensure all data can be recovered.
     *                    {@link PersistType.Code#FULL} will persist all data necessary to
     *                    prevent data loss without needing data recovery.
     * @throws AppSearchException on any error that AppSearch persist data to disk.
     */
    public void persistToDisk(@NonNull PersistType.Code persistType) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();

            LogUtil.piiTrace(TAG, "persistToDisk, request", persistType);
            PersistToDiskResultProto persistToDiskResultProto =
                    mIcingSearchEngineLocked.persistToDisk(persistType);
            LogUtil.piiTrace(
                    TAG,
                    "persistToDisk, response",
                    persistToDiskResultProto.getStatus(),
                    persistToDiskResultProto);
            checkSuccess(persistToDiskResultProto.getStatus());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Remove all {@link AppSearchSchema}s and {@link GenericDocument}s under the given package.
     *
     * @param packageName The name of package to be removed.
     * @throws AppSearchException if we cannot remove the data.
     */
    public void clearPackageData(@NonNull String packageName) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            if (LogUtil.DEBUG) {
                Log.d(TAG, "Clear data for package: " + packageName);
            }
            // TODO(b/193494000): We are calling getPackageToDatabases here and in several other
            //  places within AppSearchImpl. This method is not efficient and does a lot of string
            //  manipulation. We should find a way to cache the package to database map so it can
            //  just be obtained from a local variable instead of being parsed out of the prefixed
            //  map.
            Set<String> existingPackages = getPackageToDatabases().keySet();
            if (existingPackages.contains(packageName)) {
                existingPackages.remove(packageName);
                prunePackageData(existingPackages);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Remove all {@link AppSearchSchema}s and {@link GenericDocument}s that doesn't belong to any
     * of the given installed packages
     *
     * @param installedPackages The name of all installed package.
     * @throws AppSearchException if we cannot remove the data.
     */
    public void prunePackageData(@NonNull Set<String> installedPackages) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            throwIfClosedLocked();
            Map<String, Set<String>> packageToDatabases = getPackageToDatabases();
            if (installedPackages.containsAll(packageToDatabases.keySet())) {
                // No package got removed. We are good.
                return;
            }

            // Prune schema proto
            SchemaProto existingSchema = getSchemaProtoLocked();
            SchemaProto.Builder newSchemaBuilder = SchemaProto.newBuilder();
            for (int i = 0; i < existingSchema.getTypesCount(); i++) {
                String packageName = getPackageName(existingSchema.getTypes(i).getSchemaType());
                if (installedPackages.contains(packageName)) {
                    newSchemaBuilder.addTypes(existingSchema.getTypes(i));
                }
            }

            SchemaProto finalSchema = newSchemaBuilder.build();

            // Apply schema, set force override to true to remove all schemas and documents that
            // doesn't belong to any of these installed packages.
            LogUtil.piiTrace(
                    TAG,
                    "clearPackageData.setSchema, request",
                    finalSchema.getTypesCount(),
                    finalSchema);
            SetSchemaResultProto setSchemaResultProto = mIcingSearchEngineLocked.setSchema(
                    finalSchema, /*ignoreErrorsAndDeleteDocuments=*/ true);
            LogUtil.piiTrace(
                    TAG,
                    "clearPackageData.setSchema, response",
                    setSchemaResultProto.getStatus(),
                    setSchemaResultProto);

            // Determine whether it succeeded.
            checkSuccess(setSchemaResultProto.getStatus());

            // Prune cached maps
            for (Map.Entry<String, Set<String>> entry : packageToDatabases.entrySet()) {
                String packageName = entry.getKey();
                Set<String> databaseNames = entry.getValue();
                if (!installedPackages.contains(packageName) && databaseNames != null) {
                    mDocumentCountMapLocked.remove(packageName);
                    synchronized (mNextPageTokensLocked) {
                        mNextPageTokensLocked.remove(packageName);
                    }
                    for (String databaseName : databaseNames) {
                        String removedPrefix = createPrefix(packageName, databaseName);
                        Map<String, SchemaTypeConfigProto> removedSchemas =
                                Preconditions.checkNotNull(mSchemaMapLocked.remove(removedPrefix));
                        if (mVisibilityStoreLocked != null) {
                            mVisibilityStoreLocked.removeVisibility(removedSchemas.keySet());
                        }

                        mNamespaceMapLocked.remove(removedPrefix);
                    }
                }
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Clears documents and schema across all packages and databaseNames.
     *
     * <p>This method belongs to mutate group.
     *
     * @throws AppSearchException on IcingSearchEngine error.
     */
    @GuardedBy("mReadWriteLock")
    private void resetLocked(@Nullable InitializeStats.Builder initStatsBuilder)
            throws AppSearchException {
        LogUtil.piiTrace(TAG, "icingSearchEngine.reset, request");
        ResetResultProto resetResultProto = mIcingSearchEngineLocked.reset();
        LogUtil.piiTrace(
                TAG,
                "icingSearchEngine.reset, response",
                resetResultProto.getStatus(),
                resetResultProto);
        mOptimizeIntervalCountLocked = 0;
        mSchemaMapLocked.clear();
        mNamespaceMapLocked.clear();
        mDocumentCountMapLocked.clear();
        synchronized (mNextPageTokensLocked) {
            mNextPageTokensLocked.clear();
        }
        if (initStatsBuilder != null) {
            initStatsBuilder
                    .setHasReset(true)
                    .setResetStatusCode(statusProtoToResultCode(resetResultProto.getStatus()));
        }

        checkSuccess(resetResultProto.getStatus());
    }

    @GuardedBy("mReadWriteLock")
    private void rebuildDocumentCountMapLocked(@NonNull StorageInfoProto storageInfoProto) {
        mDocumentCountMapLocked.clear();
        List<NamespaceStorageInfoProto> namespaceStorageInfoProtoList =
                storageInfoProto.getDocumentStorageInfo().getNamespaceStorageInfoList();
        for (int i = 0; i < namespaceStorageInfoProtoList.size(); i++) {
            NamespaceStorageInfoProto namespaceStorageInfoProto =
                    namespaceStorageInfoProtoList.get(i);
            String packageName = getPackageName(namespaceStorageInfoProto.getNamespace());
            Integer oldCount = mDocumentCountMapLocked.get(packageName);
            int newCount;
            if (oldCount == null) {
                newCount = namespaceStorageInfoProto.getNumAliveDocuments();
            } else {
                newCount = oldCount + namespaceStorageInfoProto.getNumAliveDocuments();
            }
            mDocumentCountMapLocked.put(packageName, newCount);
        }
    }

    /** Wrapper around schema changes */
    @VisibleForTesting
    static class RewrittenSchemaResults {
        // Any prefixed types that used to exist in the schema, but are deleted in the new one.
        final Set<String> mDeletedPrefixedTypes = new ArraySet<>();

        // Map of prefixed schema types to SchemaTypeConfigProtos that were part of the new schema.
        final Map<String, SchemaTypeConfigProto> mRewrittenPrefixedTypes = new ArrayMap<>();
    }

    /**
     * Rewrites all types mentioned in the given {@code newSchema} to prepend {@code prefix}.
     * Rewritten types will be added to the {@code existingSchema}.
     *
     * @param prefix         The full prefix to prepend to the schema.
     * @param existingSchema A schema that may contain existing types from across all prefixes.
     *                       Will be mutated to contain the properly rewritten schema
     *                       types from {@code newSchema}.
     * @param newSchema      Schema with types to add to the {@code existingSchema}.
     * @return a RewrittenSchemaResults that contains all prefixed schema type names in the given
     * prefix as well as a set of schema types that were deleted.
     */
    @VisibleForTesting
    static RewrittenSchemaResults rewriteSchema(@NonNull String prefix,
            @NonNull SchemaProto.Builder existingSchema,
            @NonNull SchemaProto newSchema) throws AppSearchException {
        HashMap<String, SchemaTypeConfigProto> newTypesToProto = new HashMap<>();
        // Rewrite the schema type to include the typePrefix.
        for (int typeIdx = 0; typeIdx < newSchema.getTypesCount(); typeIdx++) {
            SchemaTypeConfigProto.Builder typeConfigBuilder =
                    newSchema.getTypes(typeIdx).toBuilder();

            // Rewrite SchemaProto.types.schema_type
            String newSchemaType = prefix + typeConfigBuilder.getSchemaType();
            typeConfigBuilder.setSchemaType(newSchemaType);

            // Rewrite SchemaProto.types.properties.schema_type
            for (int propertyIdx = 0;
                    propertyIdx < typeConfigBuilder.getPropertiesCount();
                    propertyIdx++) {
                PropertyConfigProto.Builder propertyConfigBuilder =
                        typeConfigBuilder.getProperties(propertyIdx).toBuilder();
                if (!propertyConfigBuilder.getSchemaType().isEmpty()) {
                    String newPropertySchemaType =
                            prefix + propertyConfigBuilder.getSchemaType();
                    propertyConfigBuilder.setSchemaType(newPropertySchemaType);
                    typeConfigBuilder.setProperties(propertyIdx, propertyConfigBuilder);
                }
            }

            newTypesToProto.put(newSchemaType, typeConfigBuilder.build());
        }

        // newTypesToProto is modified below, so we need a copy first
        RewrittenSchemaResults rewrittenSchemaResults = new RewrittenSchemaResults();
        rewrittenSchemaResults.mRewrittenPrefixedTypes.putAll(newTypesToProto);

        // Combine the existing schema (which may have types from other prefixes) with this
        // prefix's new schema. Modifies the existingSchemaBuilder.
        // Check if we need to replace any old schema types with the new ones.
        for (int i = 0; i < existingSchema.getTypesCount(); i++) {
            String schemaType = existingSchema.getTypes(i).getSchemaType();
            SchemaTypeConfigProto newProto = newTypesToProto.remove(schemaType);
            if (newProto != null) {
                // Replacement
                existingSchema.setTypes(i, newProto);
            } else if (prefix.equals(getPrefix(schemaType))) {
                // All types existing before but not in newSchema should be removed.
                existingSchema.removeTypes(i);
                --i;
                rewrittenSchemaResults.mDeletedPrefixedTypes.add(schemaType);
            }
        }
        // We've been removing existing types from newTypesToProto, so everything that remains is
        // new.
        existingSchema.addAllTypes(newTypesToProto.values());

        return rewrittenSchemaResults;
    }

    @VisibleForTesting
    @GuardedBy("mReadWriteLock")
    SchemaProto getSchemaProtoLocked() throws AppSearchException {
        LogUtil.piiTrace(TAG, "getSchema, request");
        GetSchemaResultProto schemaProto = mIcingSearchEngineLocked.getSchema();
        LogUtil.piiTrace(TAG, "getSchema, response", schemaProto.getStatus(), schemaProto);
        // TODO(b/161935693) check GetSchemaResultProto is success or not. Call reset() if it's not.
        // TODO(b/161935693) only allow GetSchemaResultProto NOT_FOUND on first run
        checkCodeOneOf(schemaProto.getStatus(), StatusProto.Code.OK, StatusProto.Code.NOT_FOUND);
        return schemaProto.getSchema();
    }

    private void addNextPageToken(String packageName, long nextPageToken) {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // There is no more pages. No need to add it.
            return;
        }
        synchronized (mNextPageTokensLocked) {
            Set<Long> tokens = mNextPageTokensLocked.get(packageName);
            if (tokens == null) {
                tokens = new ArraySet<>();
                mNextPageTokensLocked.put(packageName, tokens);
            }
            tokens.add(nextPageToken);
        }
    }

    private void checkNextPageToken(String packageName, long nextPageToken)
            throws AppSearchException {
        if (nextPageToken == EMPTY_PAGE_TOKEN) {
            // Swallow the check for empty page token, token = 0 means there is no more page and it
            // won't return anything from Icing.
            return;
        }
        synchronized (mNextPageTokensLocked) {
            Set<Long> nextPageTokens = mNextPageTokensLocked.get(packageName);
            if (nextPageTokens == null || !nextPageTokens.contains(nextPageToken)) {
                throw new AppSearchException(RESULT_SECURITY_ERROR,
                        "Package \"" + packageName + "\" cannot use nextPageToken: "
                                + nextPageToken);
            }
        }
    }

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * <p>Note that this method does not take the standard read/write lock that guards I/O, so it
     * will not queue behind I/O. Therefore it is safe to call from any thread including UI or
     * binder threads.
     *
     * @param listeningPackageAccess Visibility information about the app that wants to receive
     *                               notifications.
     * @param targetPackageName      The package that owns the data the observer wants to be
     *                               notified for.
     * @param spec                   Describes the kind of data changes the observer should trigger
     *                               for.
     * @param executor               The executor on which to trigger the observer callback to
     *                               deliver notifications.
     * @param observer               The callback to trigger on notifications.
     */
    public void registerObserverCallback(
            @NonNull CallerAccess listeningPackageAccess,
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observer) {
        // This method doesn't consult mSchemaMap or mNamespaceMap, and it will register
        // observers for types that don't exist. This is intentional because we notify for types
        // being created or removed. If we only registered observer for existing types, it would
        // be impossible to ever dispatch a notification of a type being added.
        mObserverManager.registerObserverCallback(
                listeningPackageAccess, targetPackageName, spec, executor, observer);
    }

    /**
     * Removes an {@link ObserverCallback} from watching the databases owned by
     * {@code targetPackageName}.
     *
     * <p>All observers which compare equal to the given observer via
     * {@link ObserverCallback#equals} are removed. This may be 0, 1, or many observers.
     *
     * <p>Note that this method does not take the standard read/write lock that guards I/O, so it
     * will not queue behind I/O. Therefore it is safe to call from any thread including UI or
     * binder threads.
     */
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer) {
        mObserverManager.unregisterObserverCallback(targetPackageName, observer);
    }

    /**
     * Dispatches the pending change notifications one at a time.
     *
     * <p>The notifications are dispatched on the respective executors that were provided at the
     * time of observer registration. This method does not take the standard read/write lock that
     * guards I/O, so it is safe to call from any thread including UI or binder threads.
     *
     * <p>Exceptions thrown from notification dispatch are logged but otherwise suppressed.
     */
    public void dispatchAndClearChangeNotifications() {
        mObserverManager.dispatchAndClearPendingNotifications();
    }

    private static void addToMap(Map<String, Set<String>> map, String prefix,
            String prefixedValue) {
        Set<String> values = map.get(prefix);
        if (values == null) {
            values = new ArraySet<>();
            map.put(prefix, values);
        }
        values.add(prefixedValue);
    }

    private static void addToMap(Map<String, Map<String, SchemaTypeConfigProto>> map, String prefix,
            SchemaTypeConfigProto schemaTypeConfigProto) {
        Map<String, SchemaTypeConfigProto> schemaTypeMap = map.get(prefix);
        if (schemaTypeMap == null) {
            schemaTypeMap = new ArrayMap<>();
            map.put(prefix, schemaTypeMap);
        }
        schemaTypeMap.put(schemaTypeConfigProto.getSchemaType(), schemaTypeConfigProto);
    }

    private static void removeFromMap(Map<String, Map<String, SchemaTypeConfigProto>> map,
            String prefix, String schemaType) {
        Map<String, SchemaTypeConfigProto> schemaTypeMap = map.get(prefix);
        if (schemaTypeMap != null) {
            schemaTypeMap.remove(schemaType);
        }
    }

    /**
     * Checks the given status code and throws an {@link AppSearchException} if code is an error.
     *
     * @throws AppSearchException on error codes.
     */
    private static void checkSuccess(StatusProto statusProto) throws AppSearchException {
        checkCodeOneOf(statusProto, StatusProto.Code.OK);
    }

    /**
     * Checks the given status code is one of the provided codes, and throws an
     * {@link AppSearchException} if it is not.
     */
    private static void checkCodeOneOf(StatusProto statusProto, StatusProto.Code... codes)
            throws AppSearchException {
        for (int i = 0; i < codes.length; i++) {
            if (codes[i] == statusProto.getCode()) {
                // Everything's good
                return;
            }
        }

        if (statusProto.getCode() == StatusProto.Code.WARNING_DATA_LOSS) {
            // TODO: May want to propagate WARNING_DATA_LOSS up to AppSearchSession so they can
            //  choose to log the error or potentially pass it on to clients.
            Log.w(TAG, "Encountered WARNING_DATA_LOSS: " + statusProto.getMessage());
            return;
        }

        throw new AppSearchException(
                ResultCodeToProtoConverter.toResultCode(statusProto.getCode()),
                statusProto.getMessage());
    }

    /**
     * Checks whether {@link IcingSearchEngine#optimize()} should be called to release resources.
     *
     * <p>This method should be only called after a mutation to local storage backend which
     * deletes a mass of data and could release lots resources after
     * {@link IcingSearchEngine#optimize()}.
     *
     * <p>This method will trigger {@link IcingSearchEngine#getOptimizeInfo()} to check
     * resources that could be released for every {@link #CHECK_OPTIMIZE_INTERVAL} mutations.
     *
     * <p>{@link IcingSearchEngine#optimize()} should be called only if
     * {@link GetOptimizeInfoResultProto} shows there is enough resources could be released.
     *
     * @param mutationSize The number of how many mutations have been executed for current request.
     *                     An inside counter will accumulates it. Once the counter reaches
     *                     {@link #CHECK_OPTIMIZE_INTERVAL},
     *                     {@link IcingSearchEngine#getOptimizeInfo()} will be triggered and the
     *                     counter will be reset.
     */
    public void checkForOptimize(int mutationSize, @Nullable OptimizeStats.Builder builder)
            throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            mOptimizeIntervalCountLocked += mutationSize;
            if (mOptimizeIntervalCountLocked >= CHECK_OPTIMIZE_INTERVAL) {
                checkForOptimize(builder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Checks whether {@link IcingSearchEngine#optimize()} should be called to release resources.
     *
     * <p>This method will directly trigger {@link IcingSearchEngine#getOptimizeInfo()} to check
     * resources that could be released.
     *
     * <p>{@link IcingSearchEngine#optimize()} should be called only if
     * {@link OptimizeStrategy#shouldOptimize(GetOptimizeInfoResultProto)} return true.
     */
    public void checkForOptimize(@Nullable OptimizeStats.Builder builder)
            throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            GetOptimizeInfoResultProto optimizeInfo = getOptimizeInfoResultLocked();
            checkSuccess(optimizeInfo.getStatus());
            mOptimizeIntervalCountLocked = 0;
            if (mOptimizeStrategy.shouldOptimize(optimizeInfo)) {
                optimize(builder);
            }
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
        // TODO(b/147699081): Return OptimizeResultProto & log lost data detail once we add
        //  a field to indicate lost_schema and lost_documents in OptimizeResultProto.
        //  go/icing-library-apis.
    }

    /** Triggers {@link IcingSearchEngine#optimize()} directly. */
    public void optimize(@Nullable OptimizeStats.Builder builder) throws AppSearchException {
        mReadWriteLock.writeLock().lock();
        try {
            LogUtil.piiTrace(TAG, "optimize, request");
            OptimizeResultProto optimizeResultProto = mIcingSearchEngineLocked.optimize();
            LogUtil.piiTrace(
                    TAG,
                    "optimize, response", optimizeResultProto.getStatus(), optimizeResultProto);
            if (builder != null) {
                builder.setStatusCode(statusProtoToResultCode(optimizeResultProto.getStatus()));
                AppSearchLoggerHelper.copyNativeStats(optimizeResultProto.getOptimizeStats(),
                        builder);
            }
            checkSuccess(optimizeResultProto.getStatus());
        } finally {
            mReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * Sync the current Android logging level to Icing for the entire process. No lock required.
     */
    public static void syncLoggingLevelToIcing() {
        String icingTag = IcingSearchEngine.getLoggingTag();
        if (icingTag == null) {
            Log.e(TAG, "Received null logging tag from Icing");
            return;
        }
        if (LogUtil.DEBUG) {
            if (Log.isLoggable(icingTag, Log.VERBOSE)) {
                IcingSearchEngine.setLoggingLevel(LogSeverity.Code.VERBOSE, /*verbosity=*/
                        (short) 1);
                return;
            } else if (Log.isLoggable(icingTag, Log.DEBUG)) {
                IcingSearchEngine.setLoggingLevel(LogSeverity.Code.DBG);
                return;
            }
        }
        if (Log.isLoggable(icingTag, Log.INFO)) {
            IcingSearchEngine.setLoggingLevel(LogSeverity.Code.INFO);
        } else if (Log.isLoggable(icingTag, Log.WARN)) {
            IcingSearchEngine.setLoggingLevel(LogSeverity.Code.WARNING);
        } else if (Log.isLoggable(icingTag, Log.ERROR)) {
            IcingSearchEngine.setLoggingLevel(LogSeverity.Code.ERROR);
        } else {
            IcingSearchEngine.setLoggingLevel(LogSeverity.Code.FATAL);
        }
    }

    @GuardedBy("mReadWriteLock")
    @VisibleForTesting
    GetOptimizeInfoResultProto getOptimizeInfoResultLocked() {
        LogUtil.piiTrace(TAG, "getOptimizeInfo, request");
        GetOptimizeInfoResultProto result = mIcingSearchEngineLocked.getOptimizeInfo();
        LogUtil.piiTrace(TAG, "getOptimizeInfo, response", result.getStatus(), result);
        return result;
    }

    /**
     * Returns all prefixed schema types saved in AppSearch.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    @NonNull
    public List<String> getAllPrefixedSchemaTypes() {
        mReadWriteLock.readLock().lock();
        try {
            List<String> cachedPrefixedSchemaTypes = new ArrayList<>();
            for (Map<String, SchemaTypeConfigProto> value : mSchemaMapLocked.values()) {
                cachedPrefixedSchemaTypes.addAll(value.keySet());
            }
            return cachedPrefixedSchemaTypes;
        } finally {
            mReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Converts an erroneous status code from the Icing status enums to the AppSearchResult enums.
     *
     * <p>Callers should ensure that the status code is not OK or WARNING_DATA_LOSS.
     *
     * @param statusProto StatusProto with error code to translate into an
     *                    {@link AppSearchResult} code.
     * @return {@link AppSearchResult} error code
     */
    private static @AppSearchResult.ResultCode int statusProtoToResultCode(
            @NonNull StatusProto statusProto) {
        return ResultCodeToProtoConverter.toResultCode(statusProto.getCode());
    }
}
