/*
 * Copyright 2022 The Android Open Source Project
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
import android.os.Bundle;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the specification logic for AppSearch. It can be used to set the filter
 * and settings of search a suggestions.
 *
 * @see AppSearchSession#searchSuggestionAsync(String, SearchSuggestionSpec)
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SearchSuggestionSpec {
    static final String NAMESPACE_FIELD = "namespace";
    static final String MAXIMUM_RESULT_COUNT_FIELD = "maximumResultCount";

    private final Bundle mBundle;
    private final int mMaximumResultCount;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public SearchSuggestionSpec(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
        mMaximumResultCount = bundle.getInt(MAXIMUM_RESULT_COUNT_FIELD);
        Preconditions.checkArgument(mMaximumResultCount >= 1,
                "MaximumResultCount must be positive.");
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Returns the maximum number of wanted suggestion that will be returned in the result object.
     */
    public int getMaximumResultCount() {
        return mMaximumResultCount;
    }

    /**
     * Returns the list of namespaces to search over.
     *
     * <p>If empty, will search over all namespaces.
     */
    @NonNull
    public List<String> getFilterNamespaces() {
        List<String> namespaces = mBundle.getStringArrayList(NAMESPACE_FIELD);
        if (namespaces == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(namespaces);
    }

    /** Builder for {@link SearchSuggestionSpec objects}. */
    public static final class Builder {
        private ArrayList<String> mNamespaces = new ArrayList<>();
        private final int mTotalResultCount;
        private boolean mBuilt = false;

        /**
         * Creates an {@link SearchSuggestionSpec.Builder} object.
         *
         * @param maximumResultCount Sets the maximum number of suggestion in the returned object.
         */
        public Builder(@IntRange(from = 1) int maximumResultCount) {
            Preconditions.checkArgument(maximumResultCount >= 1,
                    "maximumResultCount must be positive.");
            mTotalResultCount = maximumResultCount;
        }

        /**
         * Adds a namespace filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @NonNull
        public Builder addFilterNamespaces(@NonNull String... namespaces) {
            Preconditions.checkNotNull(namespaces);
            resetIfBuilt();
            return addFilterNamespaces(Arrays.asList(namespaces));
        }

        /**
         * Adds a namespace filter to {@link SearchSuggestionSpec} Entry. Only search for
         * suggestions that has documents under the specified namespaces.
         *
         * <p>If unset, the query will search over all namespaces.
         */
        @NonNull
        public Builder addFilterNamespaces(@NonNull Collection<String> namespaces) {
            Preconditions.checkNotNull(namespaces);
            resetIfBuilt();
            mNamespaces.addAll(namespaces);
            return this;
        }

        /** Constructs a new {@link SearchSpec} from the contents of this builder. */
        @NonNull
        public SearchSuggestionSpec build() {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(NAMESPACE_FIELD, mNamespaces);
            bundle.putInt(MAXIMUM_RESULT_COUNT_FIELD, mTotalResultCount);
            mBuilt = true;
            return new SearchSuggestionSpec(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mNamespaces = new ArrayList<>(mNamespaces);
                mBuilt = false;
            }
        }
    }
}
