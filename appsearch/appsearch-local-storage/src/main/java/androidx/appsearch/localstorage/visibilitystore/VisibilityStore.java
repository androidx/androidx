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
package androidx.appsearch.localstorage.visibilitystore;

import static androidx.appsearch.app.AppSearchResult.RESULT_NOT_FOUND;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityToDocumentConverter.ANDROID_V_OVERLAY_NAMESPACE;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_NAMESPACE;

import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.app.SearchResultPage;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.app.VisibilityPermissionConfig;
import androidx.appsearch.checker.initialization.qual.UnderInitialization;
import androidx.appsearch.checker.initialization.qual.UnknownInitialization;
import androidx.appsearch.checker.nullness.qual.RequiresNonNull;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.util.LogUtil;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.PersistType;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores all visibility settings for all databases that AppSearchImpl knows about.
 * Persists the visibility settings and reloads them on initialization.
 *
 * <p>The VisibilityStore creates a {@link InternalVisibilityConfig} for each schema. This config
 * holds the visibility settings that apply to that schema. The VisibilityStore also creates a
 * schema and documents for these {@link InternalVisibilityConfig} and has its own
 * package and database so that its data doesn't interfere with any clients' data. It persists
 * the document and schema through AppSearchImpl.
 *
 * <p>These visibility settings won't be used in AppSearch Jetpack, we only store them for clients
 * to look up.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class VisibilityStore {
    private static final String TAG = "AppSearchVisibilityStor";
    /**
     * These cannot have any of the special characters used by AppSearchImpl (e.g. {@code
     * AppSearchImpl#PACKAGE_DELIMITER} or {@code AppSearchImpl#DATABASE_DELIMITER}.
     */
    public static final String VISIBILITY_PACKAGE_NAME = "VS#Pkg";

    public static final String DOCUMENT_VISIBILITY_DATABASE_NAME = "VS#Db";
    public static final String DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME = "VS#AndroidVDb";

    public static final String BLOB_VISIBILITY_DATABASE_NAME = "VSBlob#Db";
    public static final String BLOB_ANDROID_V_OVERLAY_DATABASE_NAME = "VSBlob#AndroidVDb";
    public static final int QUERY_RESULT_COUNT_PER_PAGE = 100;

    /**
     * Map of PrefixedSchemaType to InternalVisibilityConfig stores visibility information for each
     * schema type.
     */
    private final Map<String, InternalVisibilityConfig> mVisibilityConfigMap = new ArrayMap<>();
    private final AppSearchImpl mAppSearchImpl;
    private final String mDatabaseName;
    private final String mAndroidVOverlayDatabaseName;

    /** Create a {@link VisibilityStore} instance to store document visibility settings. */
    public static @NonNull VisibilityStore createDocumentVisibilityStore(
            @NonNull AppSearchImpl appSearchImpl) throws AppSearchException {
        List<String> cachedSchemaTypes = appSearchImpl.getAllPrefixedSchemaTypes();
        return new VisibilityStore(appSearchImpl, DOCUMENT_VISIBILITY_DATABASE_NAME,
                DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME, cachedSchemaTypes);
    }

    /** Create a {@link VisibilityStore} instance to store blob visibility settings. */
    public static @NonNull VisibilityStore createBlobVisibilityStore(
            @NonNull AppSearchImpl appSearchImpl) throws AppSearchException {
        List<String> cachedBlobNamespaces = appSearchImpl.getAllPrefixedBlobNamespaces();
        return new VisibilityStore(appSearchImpl, BLOB_VISIBILITY_DATABASE_NAME,
                BLOB_ANDROID_V_OVERLAY_DATABASE_NAME, cachedBlobNamespaces);
    }

    /**
     * Create a {@link VisibilityStore} instance to store visibility settings for given database.
     *
     * <p> We have 2 types of {@link VisibilityStore}, will base on the given database names to
     * create the specific {@link VisibilityStore}.
     *
     * <p> To create a {@link VisibilityStore} to store document visibility settings, use
     * {@link #DOCUMENT_VISIBILITY_DATABASE_NAME} and
     * {@link #DOCUMENT_ANDROID_V_OVERLAY_DATABASE_NAME}.
     *
     * <p> To create a {@link VisibilityStore} to store blob visibility settings, use
     * {@link #BLOB_VISIBILITY_DATABASE_NAME} and {@link #BLOB_ANDROID_V_OVERLAY_DATABASE_NAME}.
     *
     * @param appSearchImpl               The {@link AppSearchImpl} instance to use to store
     *                                    visibility settings.
     * @param databaseName                The database name to store visibility settings.
     * @param androidVOverlayDatabaseName The database name to store Android V overlay visibility
     *                                    settings.
     * @param allVisibilityDocumentIds    The list of all visibility document ids stored in the
     *                                    given database.
     * @throws AppSearchException         On internal error.
     */
    private VisibilityStore(@NonNull AppSearchImpl appSearchImpl, @NonNull String databaseName,
            @NonNull String androidVOverlayDatabaseName,
            @NonNull List<String> allVisibilityDocumentIds)
            throws AppSearchException {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);
        mDatabaseName = Preconditions.checkNotNull(databaseName);
        mAndroidVOverlayDatabaseName = Preconditions.checkNotNull(androidVOverlayDatabaseName);
        GetSchemaResponse getSchemaResponse = getVisibilitySchemaResponse(mDatabaseName);
        List<VisibilityDocumentV1> visibilityDocumentsV1s = null;
        switch (getSchemaResponse.getVersion()) {
            case VisibilityToDocumentConverter.SCHEMA_VERSION_DOC_PER_PACKAGE:
                // TODO (b/202194495) add VisibilityDocument in version 0 back instead of using
                //  GenericDocument.
                List<GenericDocument> visibilityDocumentsV0s =
                        VisibilityStoreMigrationHelperFromV0.getVisibilityDocumentsInVersion0(
                                getSchemaResponse, mAppSearchImpl);
                visibilityDocumentsV1s = VisibilityStoreMigrationHelperFromV0
                        .toVisibilityDocumentV1(visibilityDocumentsV0s);
                // fall through
            case VisibilityToDocumentConverter.SCHEMA_VERSION_DOC_PER_SCHEMA:
                if (visibilityDocumentsV1s == null) {
                    // We need to read VisibilityDocument in Version 1 from AppSearch instead of
                    // taking from the above step.
                    visibilityDocumentsV1s =
                            VisibilityStoreMigrationHelperFromV1.getVisibilityDocumentsInVersion1(
                                    mAppSearchImpl);
                }
                setLatestSchemaAndDocuments(VisibilityStoreMigrationHelperFromV1
                        .toVisibilityDocumentsV2(visibilityDocumentsV1s));
                break;
            case VisibilityToDocumentConverter.SCHEMA_VERSION_LATEST:
                verifyOrSetLatestVisibilitySchema(getSchemaResponse);
                // Check the version for visibility overlay database.
                migrateVisibilityOverlayDatabase();
                // Now we have the latest schema, load visibility config map.
                loadVisibilityConfigMap(allVisibilityDocumentIds);
                break;
            default:
                // We must did something wrong.
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Found unsupported visibility version: " + getSchemaResponse.getVersion());
        }
    }

    /**
     * Sets visibility settings for the given {@link InternalVisibilityConfig}s. Any previous
     * {@link InternalVisibilityConfig}s with same prefixed schema type will be overwritten.
     *
     * @param prefixedVisibilityConfigs List of prefixed {@link InternalVisibilityConfig}s which
     *                                  contains schema type's visibility information.
     * @throws AppSearchException on AppSearchImpl error.
     */
    @SuppressWarnings("deprecation")
    public void setVisibility(@NonNull List<InternalVisibilityConfig> prefixedVisibilityConfigs)
            throws AppSearchException {
        Preconditions.checkNotNull(prefixedVisibilityConfigs);
        // Save new setting.
        List<GenericDocument> visibilityDocuments = new ArrayList<>();
        List<GenericDocument> overlayDocuments = new ArrayList<>();
        for (int i = 0; i < prefixedVisibilityConfigs.size(); i++) {
            // put VisibilityConfig to AppSearchImpl and mVisibilityConfigMap. If there is a
            // VisibilityConfig with same prefixed schema exists, it will be replaced by new
            // VisibilityConfig in both AppSearch and memory look up map.
            InternalVisibilityConfig prefixedVisibilityConfig = prefixedVisibilityConfigs.get(i);
            InternalVisibilityConfig oldVisibilityConfig =
                    mVisibilityConfigMap.get(prefixedVisibilityConfig.getSchemaType());
            if (Flags.enableBatchPutVisibilityDocuments()) {
                if (prefixedVisibilityConfig.equals(oldVisibilityConfig)) {
                    // This schema has no visibility changes.
                    continue;
                }
                visibilityDocuments.add(VisibilityToDocumentConverter
                        .createVisibilityDocument(prefixedVisibilityConfig));
                GenericDocument androidVOverlay = VisibilityToDocumentConverter
                        .createAndroidVOverlay(prefixedVisibilityConfig);
                if (androidVOverlay != null) {
                    overlayDocuments.add(androidVOverlay);
                } else {
                    maybeRemoveOverlayDocument(prefixedVisibilityConfig.getSchemaType(),
                            oldVisibilityConfig);
                }
            } else {
                mAppSearchImpl.putDocument(
                        VISIBILITY_PACKAGE_NAME,
                        mDatabaseName,
                        VisibilityToDocumentConverter.createVisibilityDocument(
                                prefixedVisibilityConfig),
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/ null);

                // Put the android V visibility overlay document to AppSearchImpl.
                GenericDocument androidVOverlay = VisibilityToDocumentConverter
                        .createAndroidVOverlay(prefixedVisibilityConfig);
                if (androidVOverlay != null) {
                    mAppSearchImpl.putDocument(
                            VISIBILITY_PACKAGE_NAME,
                            mAndroidVOverlayDatabaseName,
                            androidVOverlay,
                            /*sendChangeNotifications=*/ false,
                            /*logger=*/ null);
                } else {
                    maybeRemoveOverlayDocument(prefixedVisibilityConfig.getSchemaType(),
                            oldVisibilityConfig);
                }
            }

            // Put the VisibilityConfig to memory look up map.
            mVisibilityConfigMap.put(prefixedVisibilityConfig.getSchemaType(),
                    prefixedVisibilityConfig);
        }

        if (Flags.enableBatchPutVisibilityDocuments() && !visibilityDocuments.isEmpty()) {
            if (!overlayDocuments.isEmpty()) {
                mAppSearchImpl.batchPutDocuments(
                        VISIBILITY_PACKAGE_NAME,
                        mAndroidVOverlayDatabaseName,
                        overlayDocuments,
                        /*batchResultBuilder=*/null,
                        /*sendChangeNotifications=*/ false,
                        /*logger=*/null,
                        PersistType.Code.UNKNOWN);
            }
            // Now that the both main and overlay visibility document has been written. Persist the
            // newly written data.
            mAppSearchImpl.batchPutDocuments(
                    VISIBILITY_PACKAGE_NAME,
                    mDatabaseName,
                    visibilityDocuments,
                    /*batchResultBuilder=*/null,
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/null,
                    PersistType.Code.LITE);
        }
    }

    /**
     * Remove the visibility setting for the given prefixed schema type from both AppSearch and
     * memory look up map.
     */
    public void removeVisibility(@NonNull Set<String> prefixedSchemaTypes)
            throws AppSearchException {
        for (String prefixedSchemaType : prefixedSchemaTypes) {
            if (mVisibilityConfigMap.remove(prefixedSchemaType) != null) {
                // The deleted schema is not all-default setting, we need to remove its
                // VisibilityDocument from Icing.
                try {
                    mAppSearchImpl.remove(VISIBILITY_PACKAGE_NAME, mDatabaseName,
                            VISIBILITY_DOCUMENT_NAMESPACE,
                            prefixedSchemaType,
                            /*removeStatsBuilder=*/null);
                } catch (AppSearchException e) {
                    if (e.getResultCode() == RESULT_NOT_FOUND) {
                        // We are trying to remove this visibility setting, so it's weird but seems
                        // to be fine if we cannot find it.
                        Log.e(TAG, "Cannot find visibility document for " + prefixedSchemaType
                                + " to remove.");
                    } else {
                        throw e;
                    }
                }

                try {
                    mAppSearchImpl.remove(VISIBILITY_PACKAGE_NAME,
                            mAndroidVOverlayDatabaseName,
                            ANDROID_V_OVERLAY_NAMESPACE,
                            prefixedSchemaType,
                            /*removeStatsBuilder=*/null);
                } catch (AppSearchException e) {
                    if (e.getResultCode() == RESULT_NOT_FOUND) {
                        // It's possible no overlay was set, so this this is fine.
                        if (LogUtil.DEBUG) {
                            Log.d(TAG, "Cannot find Android V overlay document for "
                                    + prefixedSchemaType + " to remove.");
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    /** Gets the {@link InternalVisibilityConfig} for the given prefixed schema type.     */
    public @Nullable InternalVisibilityConfig getVisibility(@NonNull String prefixedSchemaType) {
        return mVisibilityConfigMap.get(prefixedSchemaType);
    }

    /**
     * Loads all stored latest {@link InternalVisibilityConfig} from Icing, and put them into
     * {@link #mVisibilityConfigMap}.
     *
     * @param allVisibilityDocumentIds all of document ids that we should have visibility settings
     *                                 stored in this database. The Id should be either
     *                                 prefixedSchemaType for document visibility settings or
     *                                 prefixedBlobNamespace for blob visibility settings.
     */
    @RequiresNonNull("mAppSearchImpl")
    private void loadVisibilityConfigMap(@UnderInitialization VisibilityStore this,
            @NonNull List<String> allVisibilityDocumentIds)
            throws AppSearchException {
        // Populate visibility settings set
        if (Flags.enableQueryVisibilityDocuments()) {
            // query all overlay document first and convert to id->doc map.
            List<GenericDocument> androidVOverlayDocuments = queryVisibilityDocument(
                    mAndroidVOverlayDatabaseName, ANDROID_V_OVERLAY_NAMESPACE);
            Map<String, GenericDocument> androidVOverlapMap = new ArrayMap<>(
                    androidVOverlayDocuments.size());
            for (int i = 0; i < androidVOverlayDocuments.size(); i++) {
                GenericDocument androidVOverlayDocument = androidVOverlayDocuments.get(i);
                androidVOverlapMap.put(androidVOverlayDocument.getId(), androidVOverlayDocument);
            }

            // It's ok to have null overlay document, and we should never have overlay document that
            // doesn't have main visibility document.
            List<GenericDocument> visibilityDocuments = queryVisibilityDocument(
                    mDatabaseName, VISIBILITY_DOCUMENT_NAMESPACE);
            for (int i = 0; i < visibilityDocuments.size(); i++) {
                GenericDocument visibilityDocument = visibilityDocuments.get(i);
                GenericDocument visibilityAndroidVOverlay =
                        androidVOverlapMap.get(visibilityDocument.getId());
                mVisibilityConfigMap.put(
                        visibilityDocument.getId(),
                        VisibilityToDocumentConverter.createInternalVisibilityConfig(
                                visibilityDocument, visibilityAndroidVOverlay));
            }
        } else {
            for (int i = 0; i < allVisibilityDocumentIds.size(); i++) {
                String visibilityDocumentId = allVisibilityDocumentIds.get(i);
                String packageName = PrefixUtil.getPackageName(visibilityDocumentId);
                if (packageName.equals(VISIBILITY_PACKAGE_NAME)) {
                    continue; // Our own package. Skip.
                }

                GenericDocument visibilityDocument;
                GenericDocument visibilityAndroidVOverlay = null;
                try {
                    // Note: We use the other clients' prefixed schema type as ids
                    visibilityDocument = mAppSearchImpl.getDocument(
                            VISIBILITY_PACKAGE_NAME,
                            mDatabaseName,
                            VISIBILITY_DOCUMENT_NAMESPACE,
                            /*id=*/ visibilityDocumentId,
                            /*typePropertyPaths=*/ Collections.emptyMap());
                } catch (AppSearchException e) {
                    if (e.getResultCode() == RESULT_NOT_FOUND) {
                        // The schema has all default setting and we won't have a VisibilityDocument
                        // for it.
                        continue;
                    }
                    // Otherwise, this is some other error we should pass up.
                    throw e;
                }

                try {
                    visibilityAndroidVOverlay = mAppSearchImpl.getDocument(
                            VISIBILITY_PACKAGE_NAME,
                            mAndroidVOverlayDatabaseName,
                            ANDROID_V_OVERLAY_NAMESPACE,
                            /*id=*/ visibilityDocumentId,
                            /*typePropertyPaths=*/ Collections.emptyMap());
                } catch (AppSearchException e) {
                    if (e.getResultCode() != RESULT_NOT_FOUND) {
                        // This is some other error we should pass up.
                        throw e;
                    }
                    // Otherwise we continue inserting into visibility document map as the overlay
                    // map can be null
                }

                mVisibilityConfigMap.put(
                        visibilityDocumentId,
                        VisibilityToDocumentConverter.createInternalVisibilityConfig(
                                visibilityDocument, visibilityAndroidVOverlay));
            }
        }
    }

    @NonNull
    private List<GenericDocument> queryVisibilityDocument(
            @NonNull String databaseName, @NonNull String namespace) throws AppSearchException {
        SearchSpec searchSpec = new SearchSpec.Builder()
                .addFilterNamespaces(namespace)
                .setResultCountPerPage(QUERY_RESULT_COUNT_PER_PAGE)
                .build();
        SearchResultPage searchResultPage = mAppSearchImpl.query(
                VISIBILITY_PACKAGE_NAME,
                databaseName,
                /*queryExpression=*/ "",
                searchSpec,
                /*logger=*/null);
        List<GenericDocument> visibilityDocuments = new ArrayList<>();
        List<SearchResult> searchResults = searchResultPage.getResults();
        while (!searchResults.isEmpty()) {
            for (int i = 0; i < searchResults.size(); i++) {
                visibilityDocuments.add(searchResults.get(i).getGenericDocument());
            }
            searchResultPage = mAppSearchImpl.getNextPage(
                    VISIBILITY_PACKAGE_NAME,
                    searchResultPage.getNextPageToken(),
                    /*statsBuilder=*/null);
            searchResults = searchResultPage.getResults();
        }
        return visibilityDocuments;
    }

    /**
     * Set the latest version of {@link InternalVisibilityConfig} and its schema to AppSearch.
     */
    @RequiresNonNull("mAppSearchImpl")
    @SuppressWarnings("deprecation")
    private void setLatestSchemaAndDocuments(
            @UnderInitialization VisibilityStore this,
            @NonNull List<InternalVisibilityConfig> migratedDocuments)
            throws AppSearchException {
        // The latest schema type doesn't exist yet. Add it. Set forceOverride true to
        // delete old schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                VISIBILITY_PACKAGE_NAME,
                mDatabaseName,
                Arrays.asList(VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA,
                        VisibilityPermissionConfig.SCHEMA),
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ VisibilityToDocumentConverter.SCHEMA_VERSION_LATEST,
                /*setSchemaStatsBuilder=*/ null);
        if (!internalSetSchemaResponse.isSuccess()) {
            // Impossible case, we just set forceOverride to be true, we should never
            // fail in incompatible changes.
            throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                    internalSetSchemaResponse.getErrorMessage());
        }
        InternalSetSchemaResponse internalSetAndroidVOverlaySchemaResponse =
                mAppSearchImpl.setSchema(
                        VISIBILITY_PACKAGE_NAME,
                        mAndroidVOverlayDatabaseName,
                        Collections.singletonList(
                                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA),
                        /*visibilityConfigs=*/ Collections.emptyList(),
                        /*forceOverride=*/ true,
                        /*version=*/ VisibilityToDocumentConverter
                                .ANDROID_V_OVERLAY_SCHEMA_VERSION_LATEST,
                        /*setSchemaStatsBuilder=*/ null);
        if (!internalSetAndroidVOverlaySchemaResponse.isSuccess()) {
            // Impossible case, we just set forceOverride to be true, we should never
            // fail in incompatible changes.
            throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                    internalSetAndroidVOverlaySchemaResponse.getErrorMessage());
        }
        List<GenericDocument> migratedVisibilityDocuments =
                new ArrayList<>(migratedDocuments.size());
        for (int i = 0; i < migratedDocuments.size(); i++) {
            InternalVisibilityConfig migratedConfig = migratedDocuments.get(i);
            mVisibilityConfigMap.put(migratedConfig.getSchemaType(), migratedConfig);
            if (Flags.enableBatchPutVisibilityDocuments()) {
                migratedVisibilityDocuments.add(
                        VisibilityToDocumentConverter.createVisibilityDocument(migratedConfig));
            } else {
            mAppSearchImpl.putDocument(
                    VISIBILITY_PACKAGE_NAME,
                    mDatabaseName,
                    VisibilityToDocumentConverter.createVisibilityDocument(migratedConfig),
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/ null);
            }
        }
        if (Flags.enableBatchPutVisibilityDocuments() && !migratedVisibilityDocuments.isEmpty()) {
            mAppSearchImpl.batchPutDocuments(
                    VISIBILITY_PACKAGE_NAME,
                    mDatabaseName,
                    migratedVisibilityDocuments,
                    /*batchResultBuilder=*/null,
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/null,
                    PersistType.Code.UNKNOWN);
        }
    }

    /**
     * Check and migrate visibility schemas in {@link #mAndroidVOverlayDatabaseName} to
     * {@link VisibilityToDocumentConverter#ANDROID_V_OVERLAY_SCHEMA_VERSION_LATEST}.
     */
    @RequiresNonNull("mAppSearchImpl")
    private void migrateVisibilityOverlayDatabase(@UnderInitialization VisibilityStore this)
            throws AppSearchException {
        GetSchemaResponse getSchemaResponse =
                getVisibilitySchemaResponse(mAndroidVOverlayDatabaseName);
        switch (getSchemaResponse.getVersion()) {
            case VisibilityToDocumentConverter.OVERLAY_SCHEMA_VERSION_PUBLIC_ACL_VISIBLE_TO_CONFIG:
                // Force override to next version. This version hasn't released to any public
                // version. There shouldn't have any public device in this state, so we don't
                // actually need to migrate any document.
                InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                        VISIBILITY_PACKAGE_NAME,
                        mAndroidVOverlayDatabaseName,
                        Collections.singletonList(
                                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA),
                        /*visibilityConfigs=*/ Collections.emptyList(),
                        /*forceOverride=*/ true,  // force update to nest version.
                        VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA_VERSION_LATEST,
                        /*setSchemaStatsBuilder=*/ null);
                if (!internalSetSchemaResponse.isSuccess()) {
                    // Impossible case, we just set forceOverride to be true, we should never
                    // fail in incompatible changes.
                    throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                            internalSetSchemaResponse.getErrorMessage());
                }
                break;
            case VisibilityToDocumentConverter.OVERLAY_SCHEMA_VERSION_ALL_IN_PROTO:
                verifyOrSetLatestVisibilityOverlaySchema(getSchemaResponse);
                break;
            default:
                // We must did something wrong.
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Found unsupported visibility version: " + getSchemaResponse.getVersion());
        }
    }

    /**
     * Verify the existing visibility schema, set the latest visibility schema if it's missing.
     */
    @RequiresNonNull("mAppSearchImpl")
    private void verifyOrSetLatestVisibilitySchema(
            @UnderInitialization VisibilityStore this, @NonNull GetSchemaResponse getSchemaResponse)
            throws AppSearchException {
        // We cannot change the schema version past 2 as detecting version "3" would hit the
        // default block and throw an AppSearchException. This is why we added
        // VisibilityOverlay.

        // Check Visibility schema first.
        Set<AppSearchSchema> existingVisibilitySchema = getSchemaResponse.getSchemas();
        // Force to override visibility schema if it contains DEPRECATED_PUBLIC_ACL_OVERLAY_SCHEMA.
        // The DEPRECATED_PUBLIC_ACL_OVERLAY_SCHEMA was added to VISIBILITY_DATABASE_NAME and
        // removed to ANDROID_V_OVERLAY_DATABASE_NAME. We need to force update the schema to
        // migrate devices that have already store public acl schema.
        // TODO(b/321326441) remove this method when we no longer to migrate devices in this state.
        if (existingVisibilitySchema.contains(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA)
                && existingVisibilitySchema.contains(VisibilityPermissionConfig.SCHEMA)
                && existingVisibilitySchema.contains(
                VisibilityToDocumentConverter.DEPRECATED_PUBLIC_ACL_OVERLAY_SCHEMA)) {
            InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                    VISIBILITY_PACKAGE_NAME,
                    mDatabaseName,
                    Arrays.asList(VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA,
                            VisibilityPermissionConfig.SCHEMA),
                    /*visibilityConfigs=*/ Collections.emptyList(),
                    /*forceOverride=*/ true,
                    /*version=*/ VisibilityToDocumentConverter.SCHEMA_VERSION_LATEST,
                    /*setSchemaStatsBuilder=*/ null);
            if (!internalSetSchemaResponse.isSuccess()) {
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Fail to force override deprecated visibility schema with public acl.");
            }
        } else if (!(existingVisibilitySchema.contains(
                VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA)
                && existingVisibilitySchema.contains(VisibilityPermissionConfig.SCHEMA))) {
            // We must have a broken schema. Reset it to the latest version.
            // Do NOT set forceOverride to be true here, see comment below.
            InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                    VISIBILITY_PACKAGE_NAME,
                    mDatabaseName,
                    Arrays.asList(VisibilityToDocumentConverter.VISIBILITY_DOCUMENT_SCHEMA,
                            VisibilityPermissionConfig.SCHEMA),
                    /*visibilityConfigs=*/ Collections.emptyList(),
                    /*forceOverride=*/ false,
                    /*version=*/ VisibilityToDocumentConverter.SCHEMA_VERSION_LATEST,
                    /*setSchemaStatsBuilder=*/ null);
            if (!internalSetSchemaResponse.isSuccess()) {
                // If you hit problem here it means you made a incompatible change in
                // Visibility Schema without update the version number. You should bump
                // the version number and create a VisibilityStoreMigrationHelper which
                // can analyse the different between the old version and the new version
                // to migration user's visibility settings.
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Fail to set the latest visibility schema to AppSearch. "
                                + "You may need to update the visibility schema version "
                                + "number.");
            }
        }
    }

    /**
     * Verify the existing visibility overlay schema, set the latest overlay schema if it's missing.
     */
    @RequiresNonNull("mAppSearchImpl")
    private void verifyOrSetLatestVisibilityOverlaySchema(
            @UnknownInitialization VisibilityStore this,
            @NonNull GetSchemaResponse getAndroidVOverlaySchemaResponse)
            throws AppSearchException {
        // Check Android V overlay schema.
        Set<AppSearchSchema> existingAndroidVOverlaySchema =
                getAndroidVOverlaySchemaResponse.getSchemas();
        if (!existingAndroidVOverlaySchema.contains(
                VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA)) {
            // We must have a broken schema. Reset it to the latest version.
            // Do NOT set forceOverride to be true here, see comment below.
            InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                    VISIBILITY_PACKAGE_NAME,
                    mAndroidVOverlayDatabaseName,
                    Collections.singletonList(
                            VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA),
                    /*visibilityConfigs=*/ Collections.emptyList(),
                    /*forceOverride=*/ false,
                    VisibilityToDocumentConverter.ANDROID_V_OVERLAY_SCHEMA_VERSION_LATEST,
                    /*setSchemaStatsBuilder=*/ null);
            if (!internalSetSchemaResponse.isSuccess()) {
                // If you hit problem here it means you made a incompatible change in
                // Visibility Schema. You should create new overlay schema
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Fail to set the overlay visibility schema to AppSearch. "
                                + "You may need to create new overlay schema.");
            }
        }
    }

    /**  Remove the overlay document if the old visibility config exists and contains overlay.  */
    private void maybeRemoveOverlayDocument(@NonNull String prefixedSchemaType,
            @Nullable InternalVisibilityConfig oldVisibilityConfig) throws AppSearchException {
        if (isConfigContainsAndroidVOverlay(oldVisibilityConfig)) {
            try {
                mAppSearchImpl.remove(VISIBILITY_PACKAGE_NAME,
                        mAndroidVOverlayDatabaseName,
                        ANDROID_V_OVERLAY_NAMESPACE,
                        prefixedSchemaType,
                        /*removeStatsBuilder=*/null);
            } catch (AppSearchException e) {
                // If it already doesn't exist, that is fine
                if (e.getResultCode() != RESULT_NOT_FOUND) {
                    throw e;
                }
            }
        }
    }

    /** Get the {@link GetSchemaResponse} for the given visibility database. */
    @NonNull
    private GetSchemaResponse getVisibilitySchemaResponse(@NonNull String databaseName)
            throws AppSearchException {
        GetSchemaResponse getSchemaResponse;
        if (Flags.enableBatchPutVisibilityDocuments()) {
            getSchemaResponse = mAppSearchImpl.getVisibilitySchema(
                    PrefixUtil.createPrefix(VISIBILITY_PACKAGE_NAME, databaseName));
        } else {
            getSchemaResponse = mAppSearchImpl.getSchema(
                    VISIBILITY_PACKAGE_NAME,
                    databaseName,
                    new CallerAccess(/*callingPackageName=*/VISIBILITY_PACKAGE_NAME));
        }
        return getSchemaResponse;
    }

    /**
     * Whether the given {@link InternalVisibilityConfig} contains Android V overlay settings.
     *
     * <p> Android V overlay {@link VisibilityToDocumentConverter#ANDROID_V_OVERLAY_SCHEMA}
     * contains public acl and visible to config.
     */
    private static boolean isConfigContainsAndroidVOverlay(
            @Nullable InternalVisibilityConfig config) {
        return config != null
                && (config.getVisibilityConfig().getPubliclyVisibleTargetPackage() != null
                || !config.getVisibleToConfigs().isEmpty());
    }
}
