/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.localstorage.stats;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for Optimize.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class OptimizeStats extends BaseStats {

    /**
     * The cause of IcingSearchEngine recovering from a previous bad state during initialization.
     */
    @IntDef(value = {
            // It needs to be sync with RecoveryCause in
            // external/icing/proto/icing/proto/logging.proto#InitializeStatsProto
            INDEX_TRANSLATION,
            FULL_INDEX_REBUILD,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IndexRestorationMode {
    }

    // The index has been translated in place to match the optimized document
    // store.
    public static final int INDEX_TRANSLATION  = 0;
    // The index has been rebuilt from scratch during optimization. This could
    // happen when we received a DATA_LOSS error from OptimizeDocumentStore,
    // Index::Optimize failed, or rebuilding could be faster.
    public static final int FULL_INDEX_REBUILD = 1;

    /**
     * The status code returned by {@link AppSearchResult#getResultCode()} for the call or
     * internal state.
     */
    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    private final int mNativeLatencyMillis;

    // Time used to optimize the document store in millis.
    private final int mNativeDocumentStoreOptimizeLatencyMillis;

    // Time used to restore the index in millis.
    private final int mNativeIndexRestorationLatencyMillis;

    // Number of documents before the optimization.
    private final int mNativeOriginalDocumentCount;

    // Number of documents deleted during the optimization.
    private final int mNativeDeletedDocumentCount;

    // Number of documents expired during the optimization.
    private final int mNativeExpiredDocumentCount;

    // Size of storage in bytes before the optimization.
    private final long mNativeStorageSizeBeforeBytes;

    // Size of storage in bytes after the optimization.
    private final long mNativeStorageSizeAfterBytes;

    // The amount of time in millis since the last optimization ran calculated using wall clock time
    private final long mNativeTimeSinceLastOptimizeMillis;

    // The mode of index restoration if there is any.
    @IndexRestorationMode
    private final int mIndexRestorationMode;

    // Number of namespaces before the optimization.
    private final int mNumOriginalNamespaces;

    //Number of namespaces deleted.
    private final int mNumDeletedNamespaces;

    OptimizeStats(@NonNull Builder builder) {
        super(builder);
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mNativeLatencyMillis = builder.mNativeLatencyMillis;
        mNativeDocumentStoreOptimizeLatencyMillis =
                builder.mNativeDocumentStoreOptimizeLatencyMillis;
        mNativeIndexRestorationLatencyMillis = builder.mNativeIndexRestorationLatencyMillis;
        mNativeOriginalDocumentCount = builder.mNativeOriginalDocumentCount;
        mNativeDeletedDocumentCount = builder.mNativeDeletedDocumentCount;
        mNativeExpiredDocumentCount = builder.mNativeExpiredDocumentCount;
        mNativeStorageSizeBeforeBytes = builder.mNativeStorageSizeBeforeBytes;
        mNativeStorageSizeAfterBytes = builder.mNativeStorageSizeAfterBytes;
        mNativeTimeSinceLastOptimizeMillis = builder.mNativeTimeSinceLastOptimizeMillis;
        mIndexRestorationMode = builder.mIndexRestorationMode;
        mNumOriginalNamespaces = builder.mNumOriginalNamespaces;
        mNumDeletedNamespaces = builder.mNumDeletedNamespaces;
    }

    /** Returns status code for this optimization. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns total latency of this optimization in millis. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns how much time in millis spent in the native code. */
    public int getNativeLatencyMillis() {
        return mNativeLatencyMillis;
    }

    /** Returns time used to optimize the document store in millis. */
    public int getDocumentStoreOptimizeLatencyMillis() {
        return mNativeDocumentStoreOptimizeLatencyMillis;
    }

    /** Returns time used to restore the index in millis. */
    public int getIndexRestorationLatencyMillis() {
        return mNativeIndexRestorationLatencyMillis;
    }

    /** Returns number of documents before the optimization. */
    public int getOriginalDocumentCount() {
        return mNativeOriginalDocumentCount;
    }

    /** Returns number of documents deleted during the optimization. */
    public int getDeletedDocumentCount() {
        return mNativeDeletedDocumentCount;
    }

    /** Returns number of documents expired during the optimization. */
    public int getExpiredDocumentCount() {
        return mNativeExpiredDocumentCount;
    }

    /** Returns size of storage in bytes before the optimization. */
    public long getStorageSizeBeforeBytes() {
        return mNativeStorageSizeBeforeBytes;
    }

    /** Returns size of storage in bytes after the optimization. */
    public long getStorageSizeAfterBytes() {
        return mNativeStorageSizeAfterBytes;
    }

    /**
     * Returns the amount of time in millis since the last optimization ran calculated using wall
     * clock time.
     */
    public long getTimeSinceLastOptimizeMillis() {
        return mNativeTimeSinceLastOptimizeMillis;
    }

    /**  Returns the index restoration mode. */
    @IndexRestorationMode
    public int getIndexRestorationMode() {
        return mIndexRestorationMode;
    }

    /** Returns number of namespaces before the optimization. */
    public int getNumOriginalNamespaces() {
        return mNumOriginalNamespaces;
    }

    /** Returns number of namespaces deleted. */
    public int getNumDeletedNamespaces() {
        return mNumDeletedNamespaces;
    }

    /** Builder for {@link RemoveStats}. */
    public static class Builder extends BaseStats.Builder<OptimizeStats.Builder> {
        /**
         * The status code returned by {@link AppSearchResult#getResultCode()} for the call or
         * internal state.
         */
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mNativeLatencyMillis;
        int mNativeDocumentStoreOptimizeLatencyMillis;
        int mNativeIndexRestorationLatencyMillis;
        int mNativeOriginalDocumentCount;
        int mNativeDeletedDocumentCount;
        int mNativeExpiredDocumentCount;
        long mNativeStorageSizeBeforeBytes;
        long mNativeStorageSizeAfterBytes;
        long mNativeTimeSinceLastOptimizeMillis;
        @IndexRestorationMode
        int mIndexRestorationMode;
        int mNumOriginalNamespaces;
        int mNumDeletedNamespaces;

        /** Sets the status code. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency in millis. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets native latency in millis. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeLatencyMillis(int nativeLatencyMillis) {
            mNativeLatencyMillis = nativeLatencyMillis;
            return this;
        }

        /** Sets time used to optimize the document store. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDocumentStoreOptimizeLatencyMillis(
                int documentStoreOptimizeLatencyMillis) {
            mNativeDocumentStoreOptimizeLatencyMillis = documentStoreOptimizeLatencyMillis;
            return this;
        }

        /** Sets time used to restore the index. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIndexRestorationLatencyMillis(
                int indexRestorationLatencyMillis) {
            mNativeIndexRestorationLatencyMillis = indexRestorationLatencyMillis;
            return this;
        }

        /** Sets number of documents before the optimization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setOriginalDocumentCount(int originalDocumentCount) {
            mNativeOriginalDocumentCount = originalDocumentCount;
            return this;
        }

        /** Sets number of documents deleted during the optimization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDeletedDocumentCount(int deletedDocumentCount) {
            mNativeDeletedDocumentCount = deletedDocumentCount;
            return this;
        }

        /** Sets number of documents expired during the optimization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setExpiredDocumentCount(int expiredDocumentCount) {
            mNativeExpiredDocumentCount = expiredDocumentCount;
            return this;
        }

        /** Sets Storage size in bytes before optimization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStorageSizeBeforeBytes(long storageSizeBeforeBytes) {
            mNativeStorageSizeBeforeBytes = storageSizeBeforeBytes;
            return this;
        }

        /** Sets storage size in bytes after optimization. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStorageSizeAfterBytes(long storageSizeAfterBytes) {
            mNativeStorageSizeAfterBytes = storageSizeAfterBytes;
            return this;
        }

        /**
         * Sets the amount the time since the last optimize ran calculated using wall clock time.
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setTimeSinceLastOptimizeMillis(long timeSinceLastOptimizeMillis) {
            mNativeTimeSinceLastOptimizeMillis = timeSinceLastOptimizeMillis;
            return this;
        }

        /**  Sets the index restoration mode. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIndexRestorationMode(
                @IndexRestorationMode int indexRestorationMode) {
            mIndexRestorationMode = indexRestorationMode;
            return this;
        }

        /**  Sets the number of namespaces before the optimization.  */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumOriginalNamespaces(int numOriginalNamespaces) {
            mNumOriginalNamespaces = numOriginalNamespaces;
            return this;
        }

        /**  Sets the number of namespaces  deleted.  */
        @CanIgnoreReturnValue
        public @NonNull Builder setNumDeletedNamespaces(int numDeletedNamespaces) {
            mNumDeletedNamespaces = numDeletedNamespaces;
            return this;
        }

        /** Creates a {@link OptimizeStats}. */
        @Override
        public @NonNull OptimizeStats build() {
            return new OptimizeStats(/* builder= */ this);
        }
    }
}
