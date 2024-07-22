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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.SearchResultPageCreator;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a page of {@link SearchResult}s
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "SearchResultPageCreator")
public class SearchResultPage extends AbstractSafeParcelable {
    @NonNull public static final Parcelable.Creator<SearchResultPage> CREATOR =
            new SearchResultPageCreator();

    @Field(id = 1, getter = "getNextPageToken")
    private final long mNextPageToken;
    @Nullable
    @Field(id = 2, getter = "getResults")
    private final List<SearchResult> mResults;

    @Constructor
    public SearchResultPage(
            @Param(id = 1) long nextPageToken,
            @Param(id = 2) @Nullable List<SearchResult> results) {
        mNextPageToken = nextPageToken;
        mResults = results;
    }

    /** Default constructor for {@link SearchResultPage}. */
    public SearchResultPage() {
        mNextPageToken = 0;
        mResults = Collections.emptyList();
    }

    /** Returns the Token to get next {@link SearchResultPage}. */
    public long getNextPageToken() {
        return mNextPageToken;
    }

    /** Returns all {@link androidx.appsearch.app.SearchResult}s of this page */
    @NonNull
    public List<SearchResult> getResults() {
        if (mResults == null) {
            return Collections.emptyList();
        }
        return mResults;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchResultPageCreator.writeToParcel(this, dest, flags);
    }
}
