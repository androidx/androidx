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

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.GetSchemaResponse;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.app.VisibilityPermissionDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.PersistType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores all visibility settings for all databases that AppSearchImpl knows about.
 * Persists the visibility settings and reloads them on initialization.
 *
 * <p>The VisibilityStore creates a {@link VisibilityDocument} for each schema. This document holds
 * the visibility settings that apply to that schema. The VisibilityStore also creates a
 * schema for these documents and has its own package and database so that its data doesn't
 * interfere with any clients' data. It persists the document and schema through AppSearchImpl.
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

    public static final String VISIBILITY_DATABASE_NAME = "VS#Db";

    /**
     * Map of PrefixedSchemaType and VisibilityDocument stores visibility information for each
     * schema type.
     */
    private final Map<String, VisibilityDocument> mVisibilityDocumentMap = new ArrayMap<>();

    private final AppSearchImpl mAppSearchImpl;

    public VisibilityStore(@NonNull AppSearchImpl appSearchImpl)
            throws AppSearchException {
        mAppSearchImpl = Preconditions.checkNotNull(appSearchImpl);

        GetSchemaResponse getSchemaResponse = mAppSearchImpl.getSchema(
                VISIBILITY_PACKAGE_NAME,
                VISIBILITY_DATABASE_NAME,
                new CallerAccess(/*callingPackageName=*/VISIBILITY_PACKAGE_NAME));
        List<VisibilityDocumentV1> visibilityDocumentsV1s = null;
        switch (getSchemaResponse.getVersion()) {
            case VisibilityDocument.SCHEMA_VERSION_DOC_PER_PACKAGE:
                // TODO (b/202194495) add VisibilityDocument in version 0 back instead of using
                //  GenericDocument.
                List<GenericDocument> visibilityDocumentsV0s =
                        VisibilityStoreMigrationHelperFromV0.getVisibilityDocumentsInVersion0(
                                getSchemaResponse, mAppSearchImpl);
                visibilityDocumentsV1s = VisibilityStoreMigrationHelperFromV0
                        .toVisibilityDocumentV1(visibilityDocumentsV0s);
                // fall through
            case VisibilityDocument.SCHEMA_VERSION_DOC_PER_SCHEMA:
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
            case VisibilityDocument.SCHEMA_VERSION_LATEST:
                Set<AppSearchSchema> existingVisibilitySchema = getSchemaResponse.getSchemas();
                if (existingVisibilitySchema.contains(VisibilityDocument.SCHEMA)
                        && existingVisibilitySchema.contains(VisibilityPermissionDocument.SCHEMA)) {
                    // The latest Visibility schema is in AppSearch, we must find our schema type.
                    // Extract all stored Visibility Document into mVisibilityDocumentMap.
                    loadVisibilityDocumentMap();
                } else {
                    // We must have a broken schema. Reset it to the latest version.
                    // Do NOT set forceOverride to be true here, see comment below.
                    InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                            VISIBILITY_PACKAGE_NAME,
                            VISIBILITY_DATABASE_NAME,
                            Arrays.asList(VisibilityDocument.SCHEMA,
                                    VisibilityPermissionDocument.SCHEMA),
                            /*visibilityDocuments=*/ Collections.emptyList(),
                            /*forceOverride=*/ false,
                            /*version=*/ VisibilityDocument.SCHEMA_VERSION_LATEST,
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
                break;
            default:
                // We must did something wrong.
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Found unsupported visibility version: " + getSchemaResponse.getVersion());
        }
    }

    /**
     * Sets visibility settings for the given {@link VisibilityDocument}s. Any previous
     * {@link VisibilityDocument}s with same prefixed schema type will be overwritten.
     *
     * @param prefixedVisibilityDocuments List of prefixed {@link VisibilityDocument} which
     *                                    contains schema type's visibility information.
     * @throws AppSearchException on AppSearchImpl error.
     */
    public void setVisibility(@NonNull List<VisibilityDocument> prefixedVisibilityDocuments)
            throws AppSearchException {
        Preconditions.checkNotNull(prefixedVisibilityDocuments);
        // Save new setting.
        for (int i = 0; i < prefixedVisibilityDocuments.size(); i++) {
            // put VisibilityDocument to AppSearchImpl and mVisibilityDocumentMap. If there is a
            // VisibilityDocument with same prefixed schema exists, it will be replaced by new
            // VisibilityDocument in both AppSearch and memory look up map.
            VisibilityDocument prefixedVisibilityDocument = prefixedVisibilityDocuments.get(i);
            mAppSearchImpl.putDocument(
                    VISIBILITY_PACKAGE_NAME,
                    VISIBILITY_DATABASE_NAME,
                    prefixedVisibilityDocument.toGenericDocument(),
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/ null);
            mVisibilityDocumentMap.put(prefixedVisibilityDocument.getId(),
                    prefixedVisibilityDocument);
        }
        // Now that the visibility document has been written. Persist the newly written data.
        mAppSearchImpl.persistToDisk(PersistType.Code.LITE);
    }

    /**
     * Remove the visibility setting for the given prefixed schema type from both AppSearch and
     * memory look up map.
     */
    public void removeVisibility(@NonNull Set<String> prefixedSchemaTypes)
            throws AppSearchException {
        for (String prefixedSchemaType : prefixedSchemaTypes) {
            if (mVisibilityDocumentMap.remove(prefixedSchemaType) != null) {
                // The deleted schema is not all-default setting, we need to remove its
                // VisibilityDocument from Icing.
                try {
                    mAppSearchImpl.remove(VISIBILITY_PACKAGE_NAME, VISIBILITY_DATABASE_NAME,
                            VisibilityDocument.NAMESPACE, prefixedSchemaType,
                            /*removeStatsBuilder=*/null);
                } catch (AppSearchException e) {
                    if (e.getResultCode() == RESULT_NOT_FOUND) {
                        // We are trying to remove this visibility setting, so it's weird but seems
                        // to be fine if we cannot find it.
                        Log.e(TAG, "Cannot find visibility document for " + prefixedSchemaType
                                + " to remove.");
                        return;
                    }
                    throw e;
                }
            }
        }
    }

    /** Gets the {@link VisibilityDocument} for the given prefixed schema type.     */
    @Nullable
    public VisibilityDocument getVisibility(@NonNull String prefixedSchemaType) {
        return mVisibilityDocumentMap.get(prefixedSchemaType);
    }

    /**
     * Loads all stored latest {@link VisibilityDocument} from Icing, and put them into
     * {@link #mVisibilityDocumentMap}.
     */
    private void loadVisibilityDocumentMap() throws AppSearchException {
        // Populate visibility settings set
        List<String> cachedSchemaTypes = mAppSearchImpl.getAllPrefixedSchemaTypes();
        for (int i = 0; i < cachedSchemaTypes.size(); i++) {
            String prefixedSchemaType = cachedSchemaTypes.get(i);
            String packageName = PrefixUtil.getPackageName(prefixedSchemaType);
            if (packageName.equals(VISIBILITY_PACKAGE_NAME)) {
                continue; // Our own package. Skip.
            }

            VisibilityDocument visibilityDocument;
            try {
                // Note: We use the other clients' prefixed schema type as ids
                visibilityDocument = new VisibilityDocument.Builder(
                        mAppSearchImpl.getDocument(
                                VISIBILITY_PACKAGE_NAME,
                                VISIBILITY_DATABASE_NAME,
                                VisibilityDocument.NAMESPACE,
                                /*id=*/ prefixedSchemaType,
                                /*typePropertyPaths=*/ Collections.emptyMap())).build();
            } catch (AppSearchException e) {
                if (e.getResultCode() == RESULT_NOT_FOUND) {
                    // The schema has all default setting and we won't have a VisibilityDocument for
                    // it.
                    continue;
                }
                // Otherwise, this is some other error we should pass up.
                throw e;
            }
            mVisibilityDocumentMap.put(prefixedSchemaType, visibilityDocument);
        }
    }

    /**
     * Set the latest version of {@link VisibilityDocument} and its schema to AppSearch.
     */
    private void setLatestSchemaAndDocuments(@NonNull List<VisibilityDocument> migratedDocuments)
            throws AppSearchException {
        // The latest schema type doesn't exist yet. Add it. Set forceOverride true to
        // delete old schema.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                VISIBILITY_PACKAGE_NAME,
                VISIBILITY_DATABASE_NAME,
                Arrays.asList(VisibilityDocument.SCHEMA,
                        VisibilityPermissionDocument.SCHEMA),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ VisibilityDocument.SCHEMA_VERSION_LATEST,
                /*setSchemaStatsBuilder=*/ null);
        if (!internalSetSchemaResponse.isSuccess()) {
            // Impossible case, we just set forceOverride to be true, we should never
            // fail in incompatible changes.
            throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                    internalSetSchemaResponse.getErrorMessage());
        }
        for (int i = 0; i < migratedDocuments.size(); i++) {
            VisibilityDocument migratedDocument = migratedDocuments.get(i);
            mVisibilityDocumentMap.put(migratedDocument.getId(), migratedDocument);
            mAppSearchImpl.putDocument(
                    VISIBILITY_PACKAGE_NAME,
                    VISIBILITY_DATABASE_NAME,
                    migratedDocument.toGenericDocument(),
                    /*sendChangeNotifications=*/ false,
                    /*logger=*/ null);
        }
    }
}
