/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The No-Vary-Search data specifies a set of rules that define how a URL's
 * query parameters will affect cache matching. These rules dictate whether
 * the same URL with different URL parameters should be saved as separate
 * browser cache entries.
 * <p>
 * See
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/No-Vary-Search">Doc here</a>
 * to learn more about No-Vary-Search.
 */
@Profile.ExperimentalUrlPrefetch
public class NoVarySearchData {

    /**
     * If {@code true} this indicates that differences in the order of
     * parameters between otherwise identical URLs will cause them to
     * be cached as separate entries.
     * <p>
     * However, differences in the parameters present will cause them to
     * be cached separately regardless of this flag's value.
     * <p>
     * To ignore any differences in parameters present for caching
     * see {@link #ignoreDifferencesInParameters}
     * and {@link #ignoredQueryParameters}. {@code false} otherwise.
     */
    public final boolean varyOnKeyOrder;

    /**
     * A {@code true} to indicate the differences in parameters present
     * between otherwise identical URLs will not cause them to be cached as
     * separate entries. {@code false} otherwise.
     */
    public final boolean ignoreDifferencesInParameters;

    /**
     * A {@link List} of parameters that if present,
     * will not cause otherwise identical URLs to be cached as separate
     * entries. Any parameters present in the URLs that are not included in
     * this list will affect cache matching.
     * <p>
     * This list is irrelevant and not used if
     * {@link #ignoreDifferencesInParameters} is {@code true}.
     */
    public final @NonNull List<String> ignoredQueryParameters;

    /**
     * A {@link List} of parameters that if present, will cause otherwise
     * identical URLs to be cached as separate entries. Any parameters present
     * in the URLs that are not included in this list will not affect cache
     * matching.
     * <p>
     * This list is irrelevant and not used if
     * {@link #ignoreDifferencesInParameters} is {@code false}.
     */
    public final @NonNull List<String> consideredQueryParameters;

    /**
     * Private constructor to prevent constructing invalid No-Vary-Search
     * data. Static methods should be used instead e.g. {@link #neverVaryData}.
     */
    private NoVarySearchData(boolean varyOnKeyOrder,
            boolean ignoreDifferencesInParameters,
            @NonNull List<String> ignoredQueryParameters,
            @NonNull List<String> consideredQueryParameters) {
        this.varyOnKeyOrder = varyOnKeyOrder;
        this.ignoreDifferencesInParameters = ignoreDifferencesInParameters;
        this.ignoredQueryParameters = ignoredQueryParameters;
        this.consideredQueryParameters = consideredQueryParameters;
    }


    /**
     * Returns No-Vary-Search data that doesn't consider any differences in
     * query parameters (i.e. presence or ordering) between otherwise
     * identical URLs in cache matching.
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull NoVarySearchData neverVaryData() {
        return new NoVarySearchData(false, true, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Returns No-Vary-Search data that considers all differences in
     * query parameters (i.e. presence and ordering) between otherwise
     * identical URLs in cache matching.
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull NoVarySearchData alwaysVaryData() {
        return new NoVarySearchData(true, false, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Returns No-Vary-Search data that doesn't consider differences in
     * in query parameters present between otherwise identical URLs
     * in cache matching with the exception of the ones provided
     * in {@link #consideredQueryParameters}.
     * <p>
     *
     * @param varyOnOrdering               true if the ordering of query parameters should be
     *                                  considered in cache matching, false otherwise.
     * @param consideredQueryParameters the query parameters to consider
     *                                  in cache matching.
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull NoVarySearchData neverVaryExcept(
            boolean varyOnOrdering,
            @NonNull List<String> consideredQueryParameters) {
        return new NoVarySearchData(varyOnOrdering, true, new ArrayList<>(),
                consideredQueryParameters);
    }

    /**
     * Returns No-Vary-Search data that considers differences in
     * in query parameters present between otherwise identical URLs
     * in cache matching with the exception of the ones provided
     * in {@link #ignoredQueryParameters}.
     * <p>
     *
     * @param varyOnOrdering            true if the ordering of query parameters should be
     *                               considered in cache matching, false otherwise.
     * @param ignoredQueryParameters the query parameters to ignore
     *                               in cache matching.
     */
    @Profile.ExperimentalUrlPrefetch
    public static @NonNull NoVarySearchData varyExcept(
            boolean varyOnOrdering,
            @NonNull List<String> ignoredQueryParameters) {
        return new NoVarySearchData(varyOnOrdering, false,
                ignoredQueryParameters, new ArrayList<>());
    }

}
