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

import static androidx.appsearch.stats.SchemaMigrationStats.NO_MIGRATION;
import static androidx.appsearch.stats.SchemaMigrationStats.SECOND_CALL_APPLY_NEW_SCHEMA;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.BaseStats;
import androidx.appsearch.stats.SchemaMigrationStats;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SetSchemaStats extends BaseStats {

    private final @NonNull String mPackageName;

    private final @NonNull String mDatabase;

    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    private final int mNewTypeCount;
    private final int mDeletedTypeCount;
    private final int mCompatibleTypeChangeCount;
    private final int mIndexIncompatibleTypeChangeCount;
    private final int mJoinIndexIncompatibleTypeChangeCount;
    private final int mScorablePropertyIncompatibleTypeChangeCount;
    private final int mBackwardsIncompatibleTypeChangeCount;
    private final int mDeletedDocumentCount;
    private final boolean mIsTermIndexRestored;
    private final boolean mIsIntegerIndexRestored;
    private final boolean mIsEmbeddingIndexRestored;
    private final boolean mIsQualifiedIdJoinIndexRestored;
    private final int mVerifyIncomingCallLatencyMillis;
    private final int mExecutorAcquisitionLatencyMillis;
    private final int mRebuildFromBundleLatencyMillis;
    private final int mRewriteSchemaLatencyMillis;
    private final int mTotalNativeLatencyMillis;
    private final int mNativeSchemaStoreSetSchemaLatencyMillis;
    private final int mNativeDocumentStoreUpdateSchemaLatencyMillis;
    private final int mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis;
    private final int mNativeIndexRestorationLatencyMillis;
    private final int mNativeScorablePropertyCacheRegenerationLatencyMillis;
    private final int mVisibilitySettingLatencyMillis;
    private final int mConvertToResponseLatencyMillis;
    private final int mDispatchChangeNotificationsLatencyMillis;
    private final int mOptimizeLatencyMillis;
    private final boolean mIsPackageObserved;
    private final int mGetOldSchemaLatencyMillis;
    private final int mGetObserverLatencyMillis;
    private final int mPreparingChangeNotificationLatencyMillis;
    @SchemaMigrationStats.SchemaMigrationCallType
    private final int mSchemaMigrationCallType;
    private final boolean mSkippedIcingInteraction;

    SetSchemaStats(@NonNull Builder builder) {
        super(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mNewTypeCount = builder.mNewTypeCount;
        mDeletedTypeCount = builder.mDeletedTypeCount;
        mCompatibleTypeChangeCount = builder.mCompatibleTypeChangeCount;
        mIndexIncompatibleTypeChangeCount = builder.mIndexIncompatibleTypeChangeCount;
        mJoinIndexIncompatibleTypeChangeCount = builder.mJoinIndexIncompatibleTypeChangeCount;
        mScorablePropertyIncompatibleTypeChangeCount =
                builder.mScorablePropertyIncompatibleTypeChangeCount;
        mBackwardsIncompatibleTypeChangeCount = builder.mBackwardsIncompatibleTypeChangeCount;
        mDeletedDocumentCount = builder.mDeletedDocumentCount;
        mIsTermIndexRestored = builder.mIsTermIndexRestored;
        mIsIntegerIndexRestored = builder.mIsIntegerIndexRestored;
        mIsEmbeddingIndexRestored = builder.mIsEmbeddingIndexRestored;
        mIsQualifiedIdJoinIndexRestored = builder.mIsQualifiedIdJoinIndexRestored;
        mVerifyIncomingCallLatencyMillis = builder.mVerifyIncomingCallLatencyMillis;
        mExecutorAcquisitionLatencyMillis = builder.mExecutorAcquisitionLatencyMillis;
        mRebuildFromBundleLatencyMillis = builder.mRebuildFromBundleLatencyMillis;
        mRewriteSchemaLatencyMillis = builder.mRewriteSchemaLatencyMillis;
        mTotalNativeLatencyMillis = builder.mTotalNativeLatencyMillis;
        mNativeSchemaStoreSetSchemaLatencyMillis = builder.mNativeSchemaStoreSetSchemaLatencyMillis;
        mNativeDocumentStoreUpdateSchemaLatencyMillis =
                builder.mNativeDocumentStoreUpdateSchemaLatencyMillis;
        mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis =
                builder.mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis;
        mNativeIndexRestorationLatencyMillis = builder.mNativeIndexRestorationLatencyMillis;
        mNativeScorablePropertyCacheRegenerationLatencyMillis =
                builder.mNativeScorablePropertyCacheRegenerationLatencyMillis;
        mVisibilitySettingLatencyMillis = builder.mVisibilitySettingLatencyMillis;
        mConvertToResponseLatencyMillis = builder.mConvertToResponseLatencyMillis;
        mDispatchChangeNotificationsLatencyMillis =
                builder.mDispatchChangeNotificationsLatencyMillis;
        mOptimizeLatencyMillis = builder.mOptimizeLatencyMillis;
        mIsPackageObserved = builder.mIsPackageObserved;
        mGetOldSchemaLatencyMillis = builder.mGetOldSchemaLatencyMillis;
        mGetObserverLatencyMillis = builder.mGetObserverLatencyMillis;
        mPreparingChangeNotificationLatencyMillis =
                builder.mPreparingChangeNotificationLatencyMillis;
        mSchemaMigrationCallType = builder.mSchemaMigrationCallType;
        mSkippedIcingInteraction = builder.mSkippedIcingInteraction;
    }

    /** Returns calling package name. */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /** Returns calling database name. */
    public @NonNull String getDatabase() {
        return mDatabase;
    }

    /** Returns status of the SetSchema action. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Returns the total latency of the SetSchema action. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns number of newly added schema types. */
    public int getNewTypeCount() {
        return mNewTypeCount;
    }

    /** Returns number of deleted schema types. */
    public int getDeletedTypeCount() {
        return mDeletedTypeCount;
    }

    /** Returns number of compatible type changes. */
    public int getCompatibleTypeChangeCount() {
        return mCompatibleTypeChangeCount;
    }

    /**
     * Returns number of index-incompatible type changes.
     *
     * <p>An index-incompatible type change is one that affects how pre-existing data should be
     * searched over, such as modifying the {@code IndexingType} of an existing property.
     */
    public int getIndexIncompatibleTypeChangeCount() {
        return mIndexIncompatibleTypeChangeCount;
    }

    /**
     * Returns number of join index-incompatible type changes.
     *
     * <p>A join index-incompatible type change is one that affects how pre-existing document
     * join data should be searched over, such as modifying the {@code AppSearchSchema
     * #JoinableValueType} of an existing property.
     */
    public int getJoinIndexIncompatibleTypeChangeCount() {
        return mJoinIndexIncompatibleTypeChangeCount;
    }

    /**
     * Returns number of scorable property-incompatible type changes.
     *
     * <p>A scorable property-incompatible type change is one that affects how pre-existing document
     * properties have their values cached for scoring, such as modifying the {@code
     * AppSearchSchema#setScoringEnabled} field of an existing property.
     */
    public int getScorablePropertyIncompatibleTypeChangeCount() {
        return mScorablePropertyIncompatibleTypeChangeCount;
    }

    /**
     * Returns number of backwards-incompatible type change.
     *
     * <p>For details on what constitutes a backward-incompatible type change, please see
     * {@link androidx.appsearch.app.SetSchemaRequest}.
     */
    public int getBackwardsIncompatibleTypeChangeCount() {
        return mBackwardsIncompatibleTypeChangeCount;
    }

    /**
     * Returns number of documents deleted due to an incompatible schema change made using
     * force-override.
     */
    public int getDeletedDocumentCount() {
        return mDeletedDocumentCount;
    }

    /** Whether the term index was restored. */
    public boolean isTermIndexRestored() {
        return mIsTermIndexRestored;
    }

    /** Whether the integer index was restored. */
    public boolean isIntegerIndexRestored() {
        return mIsIntegerIndexRestored;
    }

    /** Whether the embedding index was restored. */
    public boolean isEmbeddingIndexRestored() {
        return mIsEmbeddingIndexRestored;
    }

    /** Whether the qualified-id join index was restored. */
    public boolean isQualifiedIdJoinIndexRestored() {
        return mIsQualifiedIdJoinIndexRestored;
    }

    /** Gets time used for verifying the incoming call. */
    public int getVerifyIncomingCallLatencyMillis() {
        return mVerifyIncomingCallLatencyMillis;
    }

    /** Gets latency for the rebuild schema object from bundle action in milliseconds. */
    public int getRebuildFromBundleLatencyMillis() {
        return mRebuildFromBundleLatencyMillis;
    }

    /** Gets total latency for creating or waiting the user executor. */
    public int getExecutorAcquisitionLatencyMillis() {
        return mExecutorAcquisitionLatencyMillis;
    }

    /** Gets latency for the rewrite the schema proto action in milliseconds. */
    public int getRewriteSchemaLatencyMillis() {
        return mRewriteSchemaLatencyMillis;
    }

    /** Gets total latency for the SetSchema in native action in milliseconds. */
    public int getTotalNativeLatencyMillis() {
        return mTotalNativeLatencyMillis;
    }

    /** Gets latency for the native schema store set schema action. */
    public int getNativeSchemaStoreSetSchemaLatencyMillis() {
        return mNativeSchemaStoreSetSchemaLatencyMillis;
    }

    /** Gets latency for the native document store update schema action. */
    public int getNativeDocumentStoreUpdateSchemaLatencyMillis() {
        return mNativeDocumentStoreUpdateSchemaLatencyMillis;
    }

    /** Gets latency for the native document store optimized update schema action. */
    public int getNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis() {
        return mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis;
    }

    /** Gets latency for the native index restoration action. */
    public int getNativeIndexRestorationLatencyMillis() {
        return mNativeIndexRestorationLatencyMillis;
    }

    /** Gets latency for the native document store's update schema action. */
    public int getNativeScorablePropertyCacheRegenerationLatencyMillis() {
        return mNativeScorablePropertyCacheRegenerationLatencyMillis;
    }

    /** Gets latency for the dispatch change notification action in milliseconds. */
    public int getDispatchChangeNotificationsLatencyMillis() {
        return mDispatchChangeNotificationsLatencyMillis;
    }

    /** Gets latency for the apply visibility settings action in milliseconds. */
    public int getVisibilitySettingLatencyMillis() {
        return mVisibilitySettingLatencyMillis;
    }

    /** Gets latency for converting to SetSchemaResponseInternal object in milliseconds. */
    public int getConvertToResponseLatencyMillis() {
        return mConvertToResponseLatencyMillis;
    }

    /** Gets latency for the optimization action in milliseconds. */
    public int getOptimizeLatencyMillis() {
        return mOptimizeLatencyMillis;
    }

    /** Whether this package is observed and we should prepare change notifications */
    public boolean isPackageObserved() {
        return mIsPackageObserved;
    }

    /** Gets latency for the old schema action in milliseconds. */
    public int getGetOldSchemaLatencyMillis() {
        return mGetOldSchemaLatencyMillis;
    }

    /** Gets latency for the registered observer action in milliseconds. */
    public int getGetObserverLatencyMillis() {
        return mGetObserverLatencyMillis;
    }

    /** Gets latency for the preparing change notification action in milliseconds. */
    public int getPreparingChangeNotificationLatencyMillis() {
        return mPreparingChangeNotificationLatencyMillis;
    }

    /** Gets the type indicate how this set schema call relative to schema migration cases */
    @SchemaMigrationStats.SchemaMigrationCallType
    public int getSchemaMigrationCallType() {
        return mSchemaMigrationCallType;
    }

    /** Whether or not AppSearch skipped sending the schema to Icing because it didn't change. */
    public boolean getSkippedIcingInteraction() {
        return mSkippedIcingInteraction;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(
                "SetSchemaStats {\n"
                        + "  packageName=%s,\n"
                        + "  database=%s,\n"
                        + "  statusCode=%d,\n"
                        + "  totalLatencyMillis=%d,\n"
                        + "  newTypeCount=%d,\n"
                        + "  deletedTypeCount=%d,\n"
                        + "  compatibleTypeChangeCount=%d,\n"
                        + "  indexIncompatibleTypeChangeCount=%d,\n"
                        + "  joinIndexIncompatibleTypeChangeCount=%d,\n"
                        + "  scorablePropertyIncompatibleTypeChangeCount=%d,\n"
                        + "  backwardsIncompatibleTypeChangeCount=%d,\n"
                        + "  deletedDocumentCount=%d,\n"
                        + "  isTermIndexRestored=%b,\n"
                        + "  isIntegerIndexRestored=%b,\n"
                        + "  isEmbeddingIndexRestored=%b,\n"
                        + "  isQualifiedIdJoinIndexRestored=%b,\n"
                        + "  verifyIncomingCallLatencyMillis=%d,\n"
                        + "  executorAcquisitionLatencyMillis=%d,\n"
                        + "  rebuildFromBundleLatencyMillis=%d,\n"
                        + "  rewriteSchemaLatencyMillis=%d,\n"
                        + "  totalNativeLatencyMillis=%d,\n"
                        + "  nativeSchemaStoreSetSchemaLatencyMillis=%d,\n"
                        + "  nativeDocumentStoreUpdateSchemaLatencyMillis=%d,\n"
                        + "  nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis=%d,\n"
                        + "  nativeIndexRestorationLatencyMillis=%d,\n"
                        + "  nativeScorablePropertyCacheRegenerationLatencyMillis=%d,\n"
                        + "  visibilitySettingLatencyMillis=%d,\n"
                        + "  convertToResponseLatencyMillis=%d,\n"
                        + "  dispatchChangeNotificationsLatencyMillis=%d,\n"
                        + "  optimizeLatencyMillis=%d,\n"
                        + "  isPackageObserved=%b,\n"
                        + "  getOldSchemaLatencyMillis=%d,\n"
                        + "  getObserverLatencyMillis=%d,\n"
                        + "  preparingChangeNotificationLatencyMillis=%d,\n"
                        + "  schemaMigrationCallType=%d,\n"
                        + "  skippedIcingInteraction=%b,\n"
                        // Include BaseStats fields
                        + super.toString()
                        + "}",
                mPackageName,
                mDatabase,
                mStatusCode,
                mTotalLatencyMillis,
                mNewTypeCount,
                mDeletedTypeCount,
                mCompatibleTypeChangeCount,
                mIndexIncompatibleTypeChangeCount,
                mJoinIndexIncompatibleTypeChangeCount,
                mScorablePropertyIncompatibleTypeChangeCount,
                mBackwardsIncompatibleTypeChangeCount,
                mDeletedDocumentCount,
                mIsTermIndexRestored,
                mIsIntegerIndexRestored,
                mIsEmbeddingIndexRestored,
                mIsQualifiedIdJoinIndexRestored,
                mVerifyIncomingCallLatencyMillis,
                mExecutorAcquisitionLatencyMillis,
                mRebuildFromBundleLatencyMillis,
                mRewriteSchemaLatencyMillis,
                mTotalNativeLatencyMillis,
                mNativeSchemaStoreSetSchemaLatencyMillis,
                mNativeDocumentStoreUpdateSchemaLatencyMillis,
                mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis,
                mNativeIndexRestorationLatencyMillis,
                mNativeScorablePropertyCacheRegenerationLatencyMillis,
                mVisibilitySettingLatencyMillis,
                mConvertToResponseLatencyMillis,
                mDispatchChangeNotificationsLatencyMillis,
                mOptimizeLatencyMillis,
                mIsPackageObserved,
                mGetOldSchemaLatencyMillis,
                mGetObserverLatencyMillis,
                mPreparingChangeNotificationLatencyMillis,
                mSchemaMigrationCallType,
                mSkippedIcingInteraction);
    }

    /** Builder for {@link SetSchemaStats}. */
    public static class Builder extends BaseStats.Builder<SetSchemaStats.Builder> {
        final @NonNull String mPackageName;
        final @NonNull String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mNewTypeCount;
        int mDeletedTypeCount;
        int mCompatibleTypeChangeCount;
        int mIndexIncompatibleTypeChangeCount;
        int mJoinIndexIncompatibleTypeChangeCount;
        int mScorablePropertyIncompatibleTypeChangeCount;
        int mBackwardsIncompatibleTypeChangeCount;
        int mDeletedDocumentCount;
        boolean mIsTermIndexRestored;
        boolean mIsIntegerIndexRestored;
        boolean mIsEmbeddingIndexRestored;
        boolean mIsQualifiedIdJoinIndexRestored;
        int mVerifyIncomingCallLatencyMillis;
        int mExecutorAcquisitionLatencyMillis;
        int mRebuildFromBundleLatencyMillis;
        int mRewriteSchemaLatencyMillis;
        int mTotalNativeLatencyMillis;
        int mNativeSchemaStoreSetSchemaLatencyMillis;
        int mNativeDocumentStoreUpdateSchemaLatencyMillis;
        int mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis;
        int mNativeIndexRestorationLatencyMillis;
        int mNativeScorablePropertyCacheRegenerationLatencyMillis;
        int mVisibilitySettingLatencyMillis;
        int mConvertToResponseLatencyMillis;
        int mDispatchChangeNotificationsLatencyMillis;
        int mOptimizeLatencyMillis;
        boolean mIsPackageObserved;
        int mGetOldSchemaLatencyMillis;
        int mGetObserverLatencyMillis;
        int mPreparingChangeNotificationLatencyMillis;
        @SchemaMigrationStats.SchemaMigrationCallType
        int mSchemaMigrationCallType;
        boolean mSkippedIcingInteraction;

        /** Constructor for the {@link Builder}. */
        public Builder(@NonNull String packageName, @NonNull String database) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabase = Preconditions.checkNotNull(database);
        }

        /** Sets the status of the SetSchema action. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency for the SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets number of new types. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNewTypeCount(int newTypeCount) {
            mNewTypeCount = newTypeCount;
            return this;
        }

        /** Sets number of deleted types. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDeletedTypeCount(int deletedTypeCount) {
            mDeletedTypeCount = deletedTypeCount;
            return this;
        }

        /** Sets number of compatible type changes. */
        @CanIgnoreReturnValue
        public @NonNull Builder setCompatibleTypeChangeCount(int compatibleTypeChangeCount) {
            mCompatibleTypeChangeCount = compatibleTypeChangeCount;
            return this;
        }

        /** Sets number of index-incompatible type changes. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIndexIncompatibleTypeChangeCount(
                int indexIncompatibleTypeChangeCount) {
            mIndexIncompatibleTypeChangeCount = indexIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets number of join-index-incompatible type changes. */
        @CanIgnoreReturnValue
        public @NonNull Builder setJoinIndexIncompatibleTypeChangeCount(
                int joinIndexIncompatibleTypeChangeCount) {
            mJoinIndexIncompatibleTypeChangeCount = joinIndexIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets number of scorable property-incompatible type changes. */
        @CanIgnoreReturnValue
        public @NonNull Builder setScorablePropertyIncompatibleTypeChangeCount(
                int scorablePropertyIncompatibleTypeChangeCount) {
            mScorablePropertyIncompatibleTypeChangeCount =
                    scorablePropertyIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets number of backwards-incompatible type changes. */
        @CanIgnoreReturnValue
        public @NonNull Builder setBackwardsIncompatibleTypeChangeCount(
                int backwardsIncompatibleTypeChangeCount) {
            mBackwardsIncompatibleTypeChangeCount = backwardsIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets number of deleted documents due to force-setting an incompatible schema. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDeletedDocumentCount(int deletedDocumentCount) {
            mDeletedDocumentCount = deletedDocumentCount;
            return this;
        }

        /** Sets whether the term index was restored. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsTermIndexRestored(boolean isTermIndexRestored) {
            mIsTermIndexRestored = isTermIndexRestored;
            return this;
        }

        /** Sets whether the integer index was restored. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsIntegerIndexRestored(boolean isIntegerIndexRestored) {
            mIsIntegerIndexRestored = isIntegerIndexRestored;
            return this;
        }

        /** Sets whether the embedding index was restored. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsEmbeddingIndexRestored(boolean isEmbeddingIndexRestored) {
            mIsEmbeddingIndexRestored = isEmbeddingIndexRestored;
            return this;
        }

        /** Sets whether the qualified ID join index was restored. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsQualifiedIdJoinIndexRestored(
                boolean isQualifiedIdJoinIndexRestored) {
            mIsQualifiedIdJoinIndexRestored = isQualifiedIdJoinIndexRestored;
            return this;
        }

        /** Sets total latency for the SetSchema in native action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setVerifyIncomingCallLatencyMillis(
                int verifyIncomingCallLatencyMillis) {
            mVerifyIncomingCallLatencyMillis = verifyIncomingCallLatencyMillis;
            return this;
        }

        /** Sets total latency for the SetSchema in native action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setExecutorAcquisitionLatencyMillis(
                int executorAcquisitionLatencyMillis) {
            mExecutorAcquisitionLatencyMillis = executorAcquisitionLatencyMillis;
            return this;
        }

        /** Sets latency for the rebuild schema object from bundle action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRebuildFromBundleLatencyMillis(
                int rebuildFromBundleLatencyMillis) {
            mRebuildFromBundleLatencyMillis = rebuildFromBundleLatencyMillis;
            return this;
        }

        /** Sets latency for the rewrite the schema proto action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setRewriteSchemaLatencyMillis(int rewriteSchemaLatencyMillis) {
            mRewriteSchemaLatencyMillis = rewriteSchemaLatencyMillis;
            return this;
        }

        /** Sets total latency for a single set schema in native action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setTotalNativeLatencyMillis(int totalNativeLatencyMillis) {
            mTotalNativeLatencyMillis = totalNativeLatencyMillis;
            return this;
        }

        /** Sets latency for setting schema in native SchemaStore in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeSchemaStoreSetSchemaLatencyMillis(
                int nativeSchemaStoreSetSchemaLatencyMillis) {
            mNativeSchemaStoreSetSchemaLatencyMillis = nativeSchemaStoreSetSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for updating schema in native DocumentStore in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreUpdateSchemaLatencyMillis(
                int nativeDocumentStoreUpdateSchemaLatencyMillis) {
            mNativeDocumentStoreUpdateSchemaLatencyMillis =
                    nativeDocumentStoreUpdateSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for optimized schema update in native DocumentStore in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis(
                int nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis) {
            mNativeDocumentStoreOptimizedUpdateSchemaLatencyMillis =
                    nativeDocumentStoreOptimizedUpdateSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for native index restoration in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeIndexRestorationLatencyMillis(
                int nativeIndexRestorationLatencyMillis) {
            mNativeIndexRestorationLatencyMillis = nativeIndexRestorationLatencyMillis;
            return this;
        }

        /**
         * Sets latency for scorable property cache regeneration in native layer in milliseconds
         * .
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setNativeScorablePropertyCacheRegenerationLatencyMillis(
                int nativeScorablePropertyCacheRegenerationLatencyMillis) {
            mNativeScorablePropertyCacheRegenerationLatencyMillis =
                    nativeScorablePropertyCacheRegenerationLatencyMillis;
            return this;
        }

        /** Sets latency for the apply visibility settings action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setVisibilitySettingLatencyMillis(
                int visibilitySettingLatencyMillis) {
            mVisibilitySettingLatencyMillis = visibilitySettingLatencyMillis;
            return this;
        }

        /** Sets latency for converting to SetSchemaResponseInternal object in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setConvertToResponseLatencyMillis(
                int convertToResponseLatencyMillis) {
            mConvertToResponseLatencyMillis = convertToResponseLatencyMillis;
            return this;
        }

        /** Sets latency for the dispatch change notification action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDispatchChangeNotificationsLatencyMillis(
                int dispatchChangeNotificationsLatencyMillis) {
            mDispatchChangeNotificationsLatencyMillis = dispatchChangeNotificationsLatencyMillis;
            return this;
        }

        /** Sets latency for the optimization action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setOptimizeLatencyMillis(int optimizeLatencyMillis) {
            mOptimizeLatencyMillis = optimizeLatencyMillis;
            return this;
        }

        /** Sets whether this package is observed and we should prepare change notifications. */
        @CanIgnoreReturnValue
        public @NonNull Builder setIsPackageObserved(boolean isPackageObserved) {
            mIsPackageObserved = isPackageObserved;
            return this;
        }

        /** Sets latency for the old schema action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setGetOldSchemaLatencyMillis(int getOldSchemaLatencyMillis) {
            mGetOldSchemaLatencyMillis = getOldSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for the registered observer action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setGetObserverLatencyMillis(int getObserverLatencyMillis) {
            mGetObserverLatencyMillis = getObserverLatencyMillis;
            return this;
        }

        /** Sets latency for the preparing change notification action in milliseconds. */
        @CanIgnoreReturnValue
        public @NonNull Builder setPreparingChangeNotificationLatencyMillis(
                int preparingChangeNotificationLatencyMillis) {
            mPreparingChangeNotificationLatencyMillis = preparingChangeNotificationLatencyMillis;
            return this;
        }

        /** Sets the type indicate how this set schema call relative to schema migration cases */
        @CanIgnoreReturnValue
        public @NonNull Builder setSchemaMigrationCallType(
                @SchemaMigrationStats.SchemaMigrationCallType int schemaMigrationCallType) {
            Preconditions.checkArgumentInRange(schemaMigrationCallType, NO_MIGRATION,
                    SECOND_CALL_APPLY_NEW_SCHEMA, "schemaMigrationCallType");
            mSchemaMigrationCallType = schemaMigrationCallType;
            return this;
        }

        @CanIgnoreReturnValue
        public @NonNull Builder setSkippedIcingInteraction(boolean skippedIcingInteraction) {
            mSkippedIcingInteraction = skippedIcingInteraction;
            return this;
        }

        /** Builds a new {@link SetSchemaStats} from the {@link Builder}. */
        @Override
        public @NonNull SetSchemaStats build() {
            return new SetSchemaStats(/* builder= */ this);
        }
    }
}
