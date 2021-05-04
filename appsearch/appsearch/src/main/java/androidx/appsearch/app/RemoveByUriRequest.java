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
import java.util.Collections;
import java.util.Set;

/**
 * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Deprecated
public final class RemoveByUriRequest {
    private final String mNamespace;
    private final Set<String> mIds;

    RemoveByUriRequest(String namespace, Set<String> ids) {
        mNamespace = namespace;
        mIds = ids;
    }

    /** Returns the namespace to remove documents from. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the set of document IDs attached to the request. */
    @NonNull
    public Set<String> getUris() {
        return Collections.unmodifiableSet(mIds);
    }

    /**
     * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Deprecated
    @NonNull
    public RemoveByDocumentIdRequest toRemoveByDocumentIdRequest() {
        return new RemoveByDocumentIdRequest.Builder(mNamespace).addIds(mIds).build();
    }

    /**
     * Builder for {@link RemoveByUriRequest} objects.
     *
     * <p>Once {@link #build} is called, the instance can no longer be used.
     */
    public static final class Builder {
        private final String mNamespace;
        private final Set<String> mIds = new ArraySet<>();
        private boolean mBuilt = false;

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        public Builder(@NonNull String namespace) {
            mNamespace = Preconditions.checkNotNull(namespace);
        }

        /**
         * Adds one or more document IDs to the request.
         *
         * @throws IllegalStateException if the builder has already been used.
         */
        @NonNull
        public Builder addUris(@NonNull String... ids) {
            Preconditions.checkNotNull(ids);
            return addUris(Arrays.asList(ids));
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public Builder addUris(@NonNull Collection<String> ids) {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            Preconditions.checkNotNull(ids);
            mIds.addAll(ids);
            return this;
        }

        /**
         * @deprecated TODO(b/181887768): Exists for dogfood transition; must be removed.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Deprecated
        /*@exportToFramework:UnsupportedAppUsage*/
        @NonNull
        public RemoveByUriRequest build() {
            Preconditions.checkState(!mBuilt, "Builder has already been used");
            mBuilt = true;
            return new RemoveByUriRequest(mNamespace, mIds);
        }
    }
}
