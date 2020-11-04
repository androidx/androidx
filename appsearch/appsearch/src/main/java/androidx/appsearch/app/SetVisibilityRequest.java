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
 * Encapsulates a request to update the visibility settings of an {@link AppSearchSession} database.
 *
 * @see AppSearchSession#setVisibility
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
        public Builder addHiddenFromPlatformSurfaces(@NonNull AppSearchSchema... schemas) {
            Preconditions.checkNotNull(schemas);
            return addHiddenFromPlatformSurfaces(Arrays.asList(schemas));
        }

        /** Set documents of type {@code schemas} to be hidden from platform surfaces. */
        @NonNull
        public Builder addHiddenFromPlatformSurfaces(@NonNull Collection<AppSearchSchema> schemas) {
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
