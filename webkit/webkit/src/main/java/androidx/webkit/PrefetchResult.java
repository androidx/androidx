/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.annotation.RestrictTo;

/**
 * Represents the outcome of a successful prefetch operation.
 *
 * <p>
 * A {@link PrefetchResult} is returned via {@link PrefetchCache} prefetch operations
 * when a prefetch request has been handled in a non-failing way.
 */
@Profile.ExperimentalUrlPrefetch
public final class PrefetchResult {
    private final boolean mWasDuplicate;

    /**
     * @param wasDuplicate whether the request was deduplicated.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public PrefetchResult(boolean wasDuplicate) {
        mWasDuplicate = wasDuplicate;
    }

    /**
     * Returns whether the prefetch request was deduplicated against an existing entry in the
     * prefetch cache.
     *
     * <p>WebView performs deduplication when a prefetch request matches an existing
     * in-flight or completed prefetch request. Matching is based on the URL and, if provided in
     * {@link PrefetchParameters}, the expected No-Vary-Search (NVS) data.
     *
     * @return {@code true} if the request was deduplicated, {@code false} otherwise.
     */
    public boolean wasDuplicate() {
        return mWasDuplicate;
    }
}
