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

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;

/**
 * Transitional structure to unblock porting to new SearchResults interface without API review.
 *
 * TODO(b/162450968): Delete this class. SearchResults should have this role.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SearchResultsHack extends Closeable {
    /**
     * Gets a whole page of {@link SearchResults.Result}s.
     *
     * <p>Re-call this method to get next page of {@link SearchResults.Result}, until it returns an
     * empty list.
     *
     * <p>The page size is set by {@link SearchSpec.Builder#setNumPerPage}.
     *
     * @return The pending result of performing this operation.
     */
    @NonNull
    ListenableFuture<AppSearchResult<List<SearchResults.Result>>> getNextPage();

    @Override
    void close();
}
