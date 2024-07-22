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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.SearchSuggestionResultCreator;
import androidx.core.util.Preconditions;

/**
 * The result class of the {@link AppSearchSession#searchSuggestionAsync}.
 */
@SafeParcelable.Class(creator = "SearchSuggestionResultCreator")
@SuppressWarnings("HiddenSuperclass")
public final class SearchSuggestionResult extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull
    public static final Parcelable.Creator<SearchSuggestionResult> CREATOR =
            new SearchSuggestionResultCreator();

    @Field(id = 1, getter = "getSuggestedResult")
    private final String mSuggestedResult;
    @Nullable
    private Integer mHashCode;

    @Constructor
    SearchSuggestionResult(@Param(id = 1) String suggestedResult) {
        mSuggestedResult = Preconditions.checkNotNull(suggestedResult);
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
        return mSuggestedResult;
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
        return mSuggestedResult.equals(otherResult.mSuggestedResult);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = mSuggestedResult.hashCode();
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

        /** Build a {@link SearchSuggestionResult} object */
        @NonNull
        public SearchSuggestionResult build() {
            return new SearchSuggestionResult(mSuggestedResult);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchSuggestionResultCreator.writeToParcel(this, dest, flags);
    }
}
