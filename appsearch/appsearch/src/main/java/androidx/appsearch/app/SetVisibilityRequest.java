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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * Encapsulates a request to update the visibility settings of an {@link AppSearchManager} database.
 *
 * // TODO(b/169883602): Move these comments to the actual setVisibilityRequest(request) API.
 * <p>Visibility settings are not carried over from previous {@code SetVisibilityRequest}s. The
 * entire set of visibility settings must be specified on each {@code SetVisibilityRequest}.
 *
 * <p>The visibility settings apply to the schema instance that currently exists. If a schema is
 * deleted and then re-added, the visibility setting will no longer apply to the new instance of
 * the schema.
 *
 * <p>An {@link AppSearchException} will be thrown if a specified schema doesn't exist.
 *
 * <p>The default visibility settings are that all documents can be shown on platform surfaces.
 * Documents can be opted out of being shown on platform surfaces by specifying their schema type
 * in {@link SetVisibilityRequest.Builder#setHiddenFromPlatformSurfaces}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SetVisibilityRequest {
    private final Set<AppSearchSchema> mSchemasHiddenFromPlatformSurfaces;

    SetVisibilityRequest(Set<AppSearchSchema> schemasHiddenFromPlatformSurfaces) {
        mSchemasHiddenFromPlatformSurfaces = schemasHiddenFromPlatformSurfaces;
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Set<AppSearchSchema> getSchemasHiddenFromPlatformSurfaces() {
        return mSchemasHiddenFromPlatformSurfaces;
    }

    /** Builder for {@link SetVisibilityRequest} objects. */
    public static final class Builder {
        private final Set<AppSearchSchema> mSchemasHiddenFromPlatformSurfaces = new ArraySet<>();
        private boolean mBuilt = false;

        /** Set documents of type {@code schemas} to be hidden from platform surfaces. */
        @NonNull
        public Builder setHiddenFromPlatformSurfaces(@NonNull AppSearchSchema... schemas) {
            Preconditions.checkNotNull(schemas);
            return setHiddenFromPlatformSurfaces(Arrays.asList(schemas));
        }

        /** Set documents of type {@code schemas} to be hidden from platform surfaces. */
        @NonNull
        public Builder setHiddenFromPlatformSurfaces(@NonNull Collection<AppSearchSchema> schemas) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(schemas);
            mSchemasHiddenFromPlatformSurfaces.addAll(schemas);
            return this;
        }

        /** Builds a new {@link SetVisibilityRequest}. */
        @NonNull
        public SetVisibilityRequest build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;
            return new SetVisibilityRequest(mSchemasHiddenFromPlatformSurfaces);
        }
    }
}
