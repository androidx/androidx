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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.exceptions.AppSearchException;

import java.util.List;
import java.util.Set;

/**
 * An interface for classes that store and validate document visibility data.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface VisibilityStore {
    /**
     * These cannot have any of the special characters used by AppSearchImpl (e.g. {@code
     * AppSearchImpl#PACKAGE_DELIMITER} or {@code AppSearchImpl#DATABASE_DELIMITER}.
     */
    String VISIBILITY_PACKAGE_NAME = "VS#Pkg";

    String VISIBILITY_DATABASE_NAME = "VS#Db";

    /**
     * Sets visibility settings for the given {@link VisibilityDocument}s. Any previous
     * {@link VisibilityDocument}s with same prefixed schema type will be overwritten.
     *
     * @param prefixedVisibilityDocuments List of prefixed {@link VisibilityDocument} which
     *                                    contains schema type's visibility information.
     * @throws AppSearchException on AppSearchImpl error.
     */
    void setVisibility(
            @NonNull List<VisibilityDocument> prefixedVisibilityDocuments)
            throws AppSearchException;

    /**
     * Checks whether the given caller has access to the given schemas.
     *
     * @param packageName Package of app that owns the schemas.
     * @param prefixedSchema The prefixed schema type that the caller want to access.
     * @param callerUid UID of the app that wants to see the data.
     * @param callerHasSystemAccess whether the caller has system access.
     */
    boolean isSchemaSearchableByCaller(
            @NonNull String packageName,
            @NonNull String prefixedSchema,
            int callerUid,
            boolean callerHasSystemAccess);

    /**
     * Remove the visibility setting for the given prefixed schema type from both AppSearch and
     * memory look up map.
     */
    void removeVisibility(@NonNull Set<String> prefixedSchemaTypes)
            throws AppSearchException;
}
