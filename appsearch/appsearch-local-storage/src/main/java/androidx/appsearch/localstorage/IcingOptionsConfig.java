/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;

import com.google.android.icing.proto.IcingSearchEngineOptions;

/**
 * An interface exposing the optional config flags in {@link IcingSearchEngineOptions} used to
 * instantiate {@link com.google.android.icing.IcingSearchEngine}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IcingOptionsConfig {
    // Defaults from IcingSearchEngineOptions proto
    int DEFAULT_MAX_TOKEN_LENGTH = 30;

    int DEFAULT_INDEX_MERGE_SIZE = 1048576; // 1 MiB

    boolean DEFAULT_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT = false;

    float DEFAULT_OPTIMIZE_REBUILD_INDEX_THRESHOLD = 0.0f;

    /**
     * The default compression level in IcingSearchEngineOptions proto matches the
     * previously-hardcoded document compression level in Icing (which is 3).
     */
    int DEFAULT_COMPRESSION_LEVEL = 3;

    /**
     * The maximum allowable token length. All tokens in excess of this size will be truncated to
     * max_token_length before being indexed.
     *
     * <p>Clients may use this option to prevent unnecessary indexing of long tokens.
     * Depending on the use case, indexing all of
     * 'Supercalifragilisticexpialidocious' may be unnecessary - a user is
     * unlikely to type that entire query. So only indexing the first n bytes may
     * still provide the desired behavior without wasting resources.
     */
    int getMaxTokenLength();

    /**
     * The size (measured in bytes) at which Icing's internal indices should be
     * merged. Icing buffers changes together before merging them into a more
     * compact format. When the buffer exceeds index_merge_size during a Put
     * operation, the buffer is merged into the larger, more compact index.
     *
     * <p>This more compact index is more efficient to search over as the index
     * grows larger and has smaller system health impact.
     *
     * <p>Setting a low index_merge_size increases the frequency of merges -
     * increasing indexing-time latency and flash wear. Setting a high
     * index_merge_size leads to larger resource usage and higher query latency.
     */
    int getIndexMergeSize();

    /**
     * Whether to use namespace id or namespace name to build up fingerprint for
     * document_key_mapper_ and corpus_mapper_ in document store.
     */
    boolean getDocumentStoreNamespaceIdFingerprint();

    /**
     * The threshold of the percentage of invalid documents at which to rebuild index
     * during optimize.
     *
     * <p>We rebuild index if and only if |invalid_documents| / |all_documents| >= threshold.
     *
     * <p>Rebuilding the index could be faster than optimizing the index if we have
     * removed most of the documents. Based on benchmarks, 85%~95% seems to be a good threshold
     * for most cases.
     */
    float getOptimizeRebuildIndexThreshold();

    /**
     * The level of gzip compression for documents in the Icing document store.
     *
     * <p>NO_COMPRESSION = 0, BEST_SPEED = 1, BEST_COMPRESSION = 9
     */
    int getCompressionLevel();
}
