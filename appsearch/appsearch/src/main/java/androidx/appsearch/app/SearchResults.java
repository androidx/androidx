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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.Closeable;
import java.util.List;

/**
 * Encapsulates results of a search operation.
 *
 * <p>Each {@link AppSearchSession#search} operation returns a list of {@link SearchResult}
 * objects, referred to as a "page", limited by the size configured by
 * {@link SearchSpec.Builder#setResultCountPerPage}.
 *
 * <p>To fetch a page of results, call {@link #getNextPageAsync()}.
 *
 * <p>All instances of {@link SearchResults} must call {@link SearchResults#close()} after the
 * results are fetched.
 *
 * <p>This class is not thread safe.
 */
public interface SearchResults extends Closeable {
    /**
     * Retrieves the next page of {@link SearchResult} objects.
     *
     * <p>The page size is configured by {@link SearchSpec.Builder#setResultCountPerPage}.
     *
     * <p>Continue calling this method to access results until it returns an empty list,
     * signifying there are no more results.
     *
     * @return a {@link ListenableFuture} which resolves to a list of {@link SearchResult}
     * objects.
     */
    @NonNull
    ListenableFuture<List<SearchResult>> getNextPageAsync();

    /**
     * @deprecated use {@link #getNextPageAsync}.
     * @return a {@link ListenableFuture} which resolves to a list of {@link SearchResult}
     * objects.
     */
    @NonNull
    @Deprecated
    default ListenableFuture<List<SearchResult>> getNextPage() {
        return getNextPageAsync();
    }

    @Override
    void close();
}
