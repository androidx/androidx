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
// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/AppSearchConfigImpl.java)
package androidx.appsearch.localstorage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * An implementation of AppSearchConfig that returns configurations based what is specified in
 * constructor.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AppSearchConfigImpl implements AppSearchConfig {
    private final LimitConfig mLimitConfig;
    private final IcingOptionsConfig mIcingOptionsConfig;
    private final boolean mStoreParentInfoAsSyntheticProperty;
    private final boolean mShouldRetrieveParentInfo;

    public AppSearchConfigImpl(@NonNull LimitConfig limitConfig,
            @NonNull IcingOptionsConfig icingOptionsConfig) {
        this(limitConfig,
                icingOptionsConfig,
                /* storeParentInfoAsSyntheticProperty= */ false,
                /* shouldRetrieveParentInfo= */ false);
    }

    public AppSearchConfigImpl(@NonNull LimitConfig limitConfig,
            @NonNull IcingOptionsConfig icingOptionsConfig,
            boolean storeParentInfoAsSyntheticProperty,
            boolean shouldRetrieveParentInfo) {
        mLimitConfig = limitConfig;
        mIcingOptionsConfig = icingOptionsConfig;
        mStoreParentInfoAsSyntheticProperty = storeParentInfoAsSyntheticProperty;
        mShouldRetrieveParentInfo = shouldRetrieveParentInfo;
    }

    @Override
    public int getMaxTokenLength() {
        return mIcingOptionsConfig.getMaxTokenLength();
    }

    @Override
    public int getIndexMergeSize() {
        return mIcingOptionsConfig.getIndexMergeSize();
    }

    @Override
    public boolean getDocumentStoreNamespaceIdFingerprint() {
        return mIcingOptionsConfig.getDocumentStoreNamespaceIdFingerprint();
    }

    @Override
    public float getOptimizeRebuildIndexThreshold() {
        return mIcingOptionsConfig.getOptimizeRebuildIndexThreshold();
    }

    @Override
    public int getCompressionLevel() {
        return mIcingOptionsConfig.getCompressionLevel();
    }

    @Override
    public boolean getAllowCircularSchemaDefinitions() {
        return mIcingOptionsConfig.getAllowCircularSchemaDefinitions();
    }

    @Override
    public boolean getUseReadOnlySearch() {
        return mIcingOptionsConfig.getUseReadOnlySearch();
    }

    @Override
    public boolean getUsePreMappingWithFileBackedVector() {
        return mIcingOptionsConfig.getUsePreMappingWithFileBackedVector();
    }

    @Override
    public boolean getUsePersistentHashMap() {
        return mIcingOptionsConfig.getUsePersistentHashMap();
    }

    @Override
    public int getMaxPageBytesLimit() {
        return mIcingOptionsConfig.getMaxPageBytesLimit();
    }

    @Override
    public int getIntegerIndexBucketSplitThreshold() {
        return mIcingOptionsConfig.getIntegerIndexBucketSplitThreshold();
    }

    @Override
    public boolean getLiteIndexSortAtIndexing() {
        return mIcingOptionsConfig.getLiteIndexSortAtIndexing();
    }

    @Override
    public int getLiteIndexSortSize() {
        return mIcingOptionsConfig.getLiteIndexSortSize();
    }

    @Override
    public boolean getUseNewQualifiedIdJoinIndex() {
        return mIcingOptionsConfig.getUseNewQualifiedIdJoinIndex();
    }

    @Override
    public boolean getBuildPropertyExistenceMetadataHits() {
        return mIcingOptionsConfig.getBuildPropertyExistenceMetadataHits();
    }

    @Override
    public int getMaxDocumentSizeBytes() {
        return mLimitConfig.getMaxDocumentSizeBytes();
    }

    @Override
    public int getPerPackageDocumentCountLimit() {
        return mLimitConfig.getPerPackageDocumentCountLimit();
    }

    @Override
    public int getDocumentCountLimitStartThreshold() {
        return mLimitConfig.getDocumentCountLimitStartThreshold();
    }

    @Override
    public int getMaxSuggestionCount() {
        return mLimitConfig.getMaxSuggestionCount();
    }

    @Override
    public boolean shouldStoreParentInfoAsSyntheticProperty() {
        return mStoreParentInfoAsSyntheticProperty;
    }

    @Override
    public boolean shouldRetrieveParentInfo() {
        return mShouldRetrieveParentInfo;
    }
}
