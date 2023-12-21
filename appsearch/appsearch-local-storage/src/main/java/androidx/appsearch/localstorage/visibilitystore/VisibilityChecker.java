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

/**
 * An interface for classes that validate document visibility data.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface VisibilityChecker {
    /**
     * Checks whether the given caller has access to the given prefixed schemas.
     *
     * @param callerAccess      Visibility access info of the calling app
     * @param packageName Package of app that owns the schemas.
     * @param prefixedSchema The prefixed schema type that the caller want to access.
     * @param visibilityStore The {@link VisibilityStore} that store all visibility information.
     */
    boolean isSchemaSearchableByCaller(
            @NonNull CallerAccess callerAccess,
            @NonNull String packageName,
            @NonNull String prefixedSchema,
            @NonNull VisibilityStore visibilityStore);
}
