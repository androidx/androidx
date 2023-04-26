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

/**
 * Icing options for AppSearch local-storage.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class JetpackIcingOptionsConfig implements IcingOptionsConfig {
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
}
