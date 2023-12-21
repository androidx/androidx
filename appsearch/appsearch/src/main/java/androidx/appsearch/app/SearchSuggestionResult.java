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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.util.BundleUtil;
import androidx.core.util.Preconditions;

/**
 * The result class of the {@link AppSearchSession#searchSuggestionAsync}.
 */
public final class SearchSuggestionResult {

    private static final String SUGGESTED_RESULT_FIELD = "suggestedResult";
    private final Bundle mBundle;
    @Nullable
    private Integer mHashCode;

    SearchSuggestionResult(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /**
     * Returns the suggested result that could be used as query expression in the
     * {@link AppSearchSession#search}.
     *
     * <p>The suggested result will never be empty.
     *
     * <p>The suggested result only contains lowercase or special characters.
     */
    @NonNull
    public String getSuggestedResult() {
        return Preconditions.checkNotNull(mBundle.getString(SUGGESTED_RESULT_FIELD));
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchSuggestionResult)) {
            return false;
        }
        SearchSuggestionResult otherResult = (SearchSuggestionResult) other;
        return BundleUtil.deepEquals(this.mBundle, otherResult.mBundle);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = BundleUtil.deepHashCode(mBundle);
        }
        return mHashCode;
    }

    /** The Builder class of {@link SearchSuggestionResult}. */
    public static final class Builder {
        private String mSuggestedResult = "";

        /**
         * Sets the suggested result that could be used as query expression in the
         * {@link AppSearchSession#search}.
         *
         * <p>The suggested result should only contain lowercase or special characters.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSuggestedResult(@NonNull String suggestedResult) {
            Preconditions.checkNotNull(suggestedResult);
            Preconditions.checkStringNotEmpty(suggestedResult);
            mSuggestedResult = suggestedResult;
            return this;
        }

        /** Build a {@link SearchSuggestionResult} object*/
        @NonNull
        public SearchSuggestionResult build() {
            Bundle bundle = new Bundle();
            bundle.putString(SUGGESTED_RESULT_FIELD, mSuggestedResult);
            return new SearchSuggestionResult(bundle);
        }
    }
}
