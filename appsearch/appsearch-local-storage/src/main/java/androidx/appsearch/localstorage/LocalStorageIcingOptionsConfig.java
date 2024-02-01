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
// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/LocalStorageIcingOptionsConfig.java)
package androidx.appsearch.localstorage;

import androidx.annotation.RestrictTo;

/**
 * Icing options for AppSearch local-storage. Note, these values are not necessarily the defaults
 * set in {@link com.google.android.icing.proto.IcingSearchEngineOptions} proto.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class LocalStorageIcingOptionsConfig implements IcingOptionsConfig {
    @Override
    public int getMaxTokenLength() {
        return DEFAULT_MAX_TOKEN_LENGTH;
    }

    @Override
    public int getIndexMergeSize() {
        return DEFAULT_INDEX_MERGE_SIZE;
    }

    @Override
    public boolean getDocumentStoreNamespaceIdFingerprint() {
        return true;
    }

    @Override
    public float getOptimizeRebuildIndexThreshold() {
        return 0.9f;
    }

    @Override
    public int getCompressionLevel() {
        return DEFAULT_COMPRESSION_LEVEL;
    }

    @Override
    public boolean getAllowCircularSchemaDefinitions() {
        return true;
    }

    @Override
    public boolean getUseReadOnlySearch() {
        return true;
    }

    @Override
    public boolean getUsePreMappingWithFileBackedVector() {
        return false;
    }

    @Override
    public boolean getUsePersistentHashMap() {
        return true;
    }

    @Override
    public int getMaxPageBytesLimit() {
        return DEFAULT_MAX_PAGE_BYTES_LIMIT;
    }

    @Override
    public int getIntegerIndexBucketSplitThreshold() {
        return DEFAULT_INTEGER_INDEX_BUCKET_SPLIT_THRESHOLD;
    }

    @Override
    public boolean getLiteIndexSortAtIndexing() {
        return DEFAULT_LITE_INDEX_SORT_AT_INDEXING;
    }

    @Override
    public int getLiteIndexSortSize() {
        return DEFAULT_LITE_INDEX_SORT_SIZE;
    }

    @Override
    public boolean getUseNewQualifiedIdJoinIndex() {
        return DEFAULT_USE_NEW_QUALIFIED_ID_JOIN_INDEX;
    }

    @Override
    public boolean getBuildPropertyExistenceMetadataHits() {
        return DEFAULT_BUILD_PROPERTY_EXISTENCE_METADATA_HITS;
    }
}
