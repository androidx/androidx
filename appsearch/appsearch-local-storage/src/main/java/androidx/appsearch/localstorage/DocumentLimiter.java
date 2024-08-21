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

package androidx.appsearch.localstorage;

import static androidx.appsearch.localstorage.util.PrefixUtil.getPackageName;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.util.MapUtil;
import androidx.collection.ArrayMap;
import androidx.core.util.Preconditions;

import com.google.android.icing.proto.NamespaceStorageInfoProto;

import java.util.List;
import java.util.Map;

/**
 * A class that encapsulates per-package document count tracking and limit enforcement.
 *
 * This class is configured with a {@link #mDocumentLimitStartThreshold}. While the total number of
 * documents in the system is below that threshold, all packages will be allowed to put as many
 * documents into the index as they wish. Once the total number of documents exceed
 * {@link #mDocumentLimitStartThreshold}, then each package will be limited to no more than
 * {@link #mPerPackageDocumentCountLimit} documents.
 *
 *  <p>This class is not thread safe.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DocumentLimiter {
    private final int mDocumentLimitStartThreshold;
    private final int mPerPackageDocumentCountLimit;
    private int mTotalDocumentCount;
    private final Map<String, Integer> mDocumentCountMap;

    /**
     * @param documentLimitStartThreshold the total number of documents in the system at which the
     *                                    limiter should begin applying the
     *                                    perPackageDocumentCountLimit limit.
     * @param perPackageDocumentCountLimit the maximum number of documents that each package is
     *                                   allowed to have once the total number of documents in the
     *                                   system exceeds documentLimitStartThreshold.
     * @param namespaceStorageInfoProtoList a list of NamespaceStorageInfoProtos that reflects the
     *                                     state of the index when this DocumentLimiter was created.
     */
    public DocumentLimiter(int documentLimitStartThreshold, int perPackageDocumentCountLimit,
            @NonNull List<NamespaceStorageInfoProto> namespaceStorageInfoProtoList) {
        mDocumentLimitStartThreshold = documentLimitStartThreshold;
        mPerPackageDocumentCountLimit = perPackageDocumentCountLimit;
        mTotalDocumentCount = 0;
        mDocumentCountMap = new ArrayMap<>(namespaceStorageInfoProtoList.size());
        buildDocumentCountMap(Preconditions.checkNotNull(namespaceStorageInfoProtoList));
    }

    /**
     * Checks whether the package identified by packageName should be allowed to add another
     * document.
     *
     * @param packageName the name of the package attempting to add the document
     *
     * @throws AppSearchException if the document limit is in force (because the total number of
     * documents in the system exceeds {@link #mDocumentLimitStartThreshold}) and the package
     * identified by packageName has already added more documents than
     * {@link #mPerPackageDocumentCountLimit}.
     */
    public void enforceDocumentCountLimit(@NonNull String packageName) throws AppSearchException {
        Preconditions.checkNotNull(packageName);
        if (mTotalDocumentCount < mDocumentLimitStartThreshold) {
            return;
        }
        Integer newDocumentCount = MapUtil.getOrDefault(mDocumentCountMap, packageName, 0) + 1;
        if (newDocumentCount > mPerPackageDocumentCountLimit) {
            // Now we really can't fit it in, even accounting for replacements.
            throw new AppSearchException(
                    AppSearchResult.RESULT_OUT_OF_SPACE,
                    "Package \"" + packageName + "\" exceeded limit of "
                            + mPerPackageDocumentCountLimit + " documents. Some documents "
                            + "must be removed to index additional ones.");
        }
    }

    /**
     * Informs the DocumentLimiter that another document has been added for the package identified
     * by package.
     *
     * @param packageName the name of the package that owns the added document.
     */
    public void reportDocumentAdded(@NonNull String packageName) {
        Preconditions.checkNotNull(packageName);
        ++mTotalDocumentCount;
        Integer newDocumentCount = MapUtil.getOrDefault(mDocumentCountMap, packageName, 0) + 1;
        mDocumentCountMap.put(packageName, newDocumentCount);
    }

    /**
     * Informs the DocumentLimiter that numDocumentsDeleted documents, owned by the package
     * identified by packageName, have been deleted.
     *
     * @param packageName the name of the package that owns the deleted documents.
     * @param numDocumentsDeleted the number of documents that were deleted.
     */
    public void reportDocumentsRemoved(@NonNull String packageName, int numDocumentsDeleted) {
        Preconditions.checkNotNull(packageName);
        if (numDocumentsDeleted <= 0) {
            return;
        }
        mTotalDocumentCount -= numDocumentsDeleted;
        Integer oldDocumentCount = mDocumentCountMap.get(packageName);
        // This should always be true: how can we delete documents for a package without
        // having seen that package during init? This is just a safeguard.
        if (oldDocumentCount != null) {
            if (numDocumentsDeleted >= oldDocumentCount) {
                mDocumentCountMap.remove(packageName);
            } else {
                mDocumentCountMap.put(packageName, oldDocumentCount - numDocumentsDeleted);
            }
        }
    }

    /**
     * Informs the DocumentLimiter that the package identified by packageName has been removed from
     * the system entirely.
     *
     * @param packageName the name of the package that was removed.
     */
    public void reportPackageRemoved(@NonNull String packageName) {
        Preconditions.checkNotNull(packageName);
        Integer oldDocumentCount = mDocumentCountMap.remove(packageName);
        if (oldDocumentCount != null) {
            // This should always be true: how can we remove a package without having seen that
            // package during init? This is just a safeguard.
            mTotalDocumentCount -= oldDocumentCount;
        }
    }

    private void buildDocumentCountMap(
            @NonNull List<NamespaceStorageInfoProto> namespaceStorageInfoProtoList) {
        mDocumentCountMap.clear();
        mTotalDocumentCount = 0;
        for (int i = 0; i < namespaceStorageInfoProtoList.size(); i++) {
            NamespaceStorageInfoProto namespaceStorageInfoProto =
                    namespaceStorageInfoProtoList.get(i);
            mTotalDocumentCount += namespaceStorageInfoProto.getNumAliveDocuments();
            String packageName = getPackageName(namespaceStorageInfoProto.getNamespace());
            Integer newCount =
                    MapUtil.getOrDefault(mDocumentCountMap, packageName, 0)
                            + namespaceStorageInfoProto.getNumAliveDocuments();
            mDocumentCountMap.put(packageName, newCount);
        }
    }
}
