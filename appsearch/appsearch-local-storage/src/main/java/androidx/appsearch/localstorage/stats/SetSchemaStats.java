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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.stats.SchemaMigrationStats;
import androidx.core.util.Preconditions;

/**
 * Class holds detailed stats for
 * {@link androidx.appsearch.app.AppSearchSession#setSchema(SetSchemaRequest)}.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SetSchemaStats {

    @NonNull
    private final String mPackageName;

    @NonNull
    private final String mDatabase;

    @AppSearchResult.ResultCode
    private final int mStatusCode;
    private final int mTotalLatencyMillis;
    private final int mNewTypeCount;
    private final int mDeletedTypeCount;
    private final int mCompatibleTypeChangeCount;
    private final int mIndexIncompatibleTypeChangeCount;
    private final int mBackwardsIncompatibleTypeChangeCount;
    private final int mVerifyIncomingCallLatencyMillis;
    private final int mExecutorAcquisitionLatencyMillis;
    private final int mRebuildFromBundleLatencyMillis;
    private final int mJavaLockAcquisitionLatencyMillis;
    private final int mRewriteSchemaLatencyMillis;
    private final int mTotalNativeLatencyMillis;
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

    SetSchemaStats(@NonNull Builder builder) {
        Preconditions.checkNotNull(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mStatusCode = builder.mStatusCode;
        mTotalLatencyMillis = builder.mTotalLatencyMillis;
        mNewTypeCount = builder.mNewTypeCount;
        mDeletedTypeCount = builder.mDeletedTypeCount;
        mCompatibleTypeChangeCount = builder.mCompatibleTypeChangeCount;
        mIndexIncompatibleTypeChangeCount = builder.mIndexIncompatibleTypeChangeCount;
        mBackwardsIncompatibleTypeChangeCount = builder.mBackwardsIncompatibleTypeChangeCount;
        mVerifyIncomingCallLatencyMillis = builder.mVerifyIncomingCallLatencyMillis;
        mExecutorAcquisitionLatencyMillis = builder.mExecutorAcquisitionLatencyMillis;
        mRebuildFromBundleLatencyMillis = builder.mRebuildFromBundleLatencyMillis;
        mJavaLockAcquisitionLatencyMillis = builder.mJavaLockAcquisitionLatencyMillis;
        mRewriteSchemaLatencyMillis = builder.mRewriteSchemaLatencyMillis;
        mTotalNativeLatencyMillis = builder.mTotalNativeLatencyMillis;
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
    }

    /** Returns calling package name. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns calling database name. */
    @NonNull
    public String getDatabase() {
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
     * Returns number of index-incompatible type change.
     *
     * <p>An index-incompatible type change is one that affects how pre-existing data should be
     * searched over, such as modifying the {@code IndexingType} of an existing property.
     */
    public int getIndexIncompatibleTypeChangeCount() {
        return mIndexIncompatibleTypeChangeCount;
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

    /** Gets time used for verifying the incoming call. */
    public int getVerifyIncomingCallLatencyMillis() {
        return mVerifyIncomingCallLatencyMillis;
    }

    /** Gets time passed while waiting to acquire the lock during Java function calls. */
    public int getJavaLockAcquisitionLatencyMillis() {
        return mJavaLockAcquisitionLatencyMillis;
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

    /** Builder for {@link SetSchemaStats}. */
    public static class Builder {
        @NonNull
        final String mPackageName;
        @NonNull
        final String mDatabase;
        @AppSearchResult.ResultCode
        int mStatusCode;
        int mTotalLatencyMillis;
        int mNewTypeCount;
        int mDeletedTypeCount;
        int mCompatibleTypeChangeCount;
        int mIndexIncompatibleTypeChangeCount;
        int mBackwardsIncompatibleTypeChangeCount;
        int mVerifyIncomingCallLatencyMillis;
        int mExecutorAcquisitionLatencyMillis;
        int mRebuildFromBundleLatencyMillis;
        int mJavaLockAcquisitionLatencyMillis;
        int mRewriteSchemaLatencyMillis;
        int mTotalNativeLatencyMillis;
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

        /** Constructor for the {@link Builder}. */
        public Builder(@NonNull String packageName, @NonNull String database) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabase = Preconditions.checkNotNull(database);
        }

        /** Sets the status of the SetSchema action. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets total latency for the SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets number of new types. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNewTypeCount(int newTypeCount) {
            mNewTypeCount = newTypeCount;
            return this;
        }

        /** Sets number of deleted types. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDeletedTypeCount(int deletedTypeCount) {
            mDeletedTypeCount = deletedTypeCount;
            return this;
        }

        /** Sets number of compatible type changes. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setCompatibleTypeChangeCount(int compatibleTypeChangeCount) {
            mCompatibleTypeChangeCount = compatibleTypeChangeCount;
            return this;
        }

        /** Sets number of index-incompatible type changes. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIndexIncompatibleTypeChangeCount(int indexIncompatibleTypeChangeCount) {
            mIndexIncompatibleTypeChangeCount = indexIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets number of backwards-incompatible type changes. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setBackwardsIncompatibleTypeChangeCount(
                int backwardsIncompatibleTypeChangeCount) {
            mBackwardsIncompatibleTypeChangeCount = backwardsIncompatibleTypeChangeCount;
            return this;
        }

        /** Sets total latency for the SetSchema in native action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVerifyIncomingCallLatencyMillis(int verifyIncomingCallLatencyMillis) {
            mVerifyIncomingCallLatencyMillis = verifyIncomingCallLatencyMillis;
            return this;
        }

        /** Sets total latency for the SetSchema in native action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setExecutorAcquisitionLatencyMillis(int executorAcquisitionLatencyMillis) {
            mExecutorAcquisitionLatencyMillis = executorAcquisitionLatencyMillis;
            return this;
        }

        /** Sets latency for the rebuild schema object from bundle action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRebuildFromBundleLatencyMillis(int rebuildFromBundleLatencyMillis) {
            mRebuildFromBundleLatencyMillis = rebuildFromBundleLatencyMillis;
            return this;
        }

        /**
         * Sets latency for waiting to acquire the lock during Java function calls in milliseconds.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setJavaLockAcquisitionLatencyMillis(int javaLockAcquisitionLatencyMillis) {
            mJavaLockAcquisitionLatencyMillis = javaLockAcquisitionLatencyMillis;
            return this;
        }

        /** Sets latency for the rewrite the schema proto action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setRewriteSchemaLatencyMillis(int rewriteSchemaLatencyMillis) {
            mRewriteSchemaLatencyMillis = rewriteSchemaLatencyMillis;
            return this;
        }

        /** Sets total latency for a single set schema in native action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalNativeLatencyMillis(int totalNativeLatencyMillis) {
            mTotalNativeLatencyMillis = totalNativeLatencyMillis;
            return this;
        }

        /** Sets latency for the apply visibility settings action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setVisibilitySettingLatencyMillis(int visibilitySettingLatencyMillis) {
            mVisibilitySettingLatencyMillis = visibilitySettingLatencyMillis;
            return this;
        }

        /** Sets latency for converting to SetSchemaResponseInternal object in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setConvertToResponseLatencyMillis(int convertToResponseLatencyMillis) {
            mConvertToResponseLatencyMillis = convertToResponseLatencyMillis;
            return this;
        }

        /** Sets latency for the dispatch change notification action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDispatchChangeNotificationsLatencyMillis(
                int dispatchChangeNotificationsLatencyMillis) {
            mDispatchChangeNotificationsLatencyMillis = dispatchChangeNotificationsLatencyMillis;
            return this;
        }

        /** Sets latency for the optimization action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setOptimizeLatencyMillis(int optimizeLatencyMillis) {
            mOptimizeLatencyMillis = optimizeLatencyMillis;
            return this;
        }

        /** Sets whether this package is observed and we should prepare change notifications. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIsPackageObserved(boolean isPackageObserved) {
            mIsPackageObserved = isPackageObserved;
            return this;
        }

        /** Sets latency for the old schema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setGetOldSchemaLatencyMillis(int getOldSchemaLatencyMillis) {
            mGetOldSchemaLatencyMillis = getOldSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for the registered observer action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setGetObserverLatencyMillis(int getObserverLatencyMillis) {
            mGetObserverLatencyMillis = getObserverLatencyMillis;
            return this;
        }

        /** Sets latency for the preparing change notification action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setPreparingChangeNotificationLatencyMillis(
                int preparingChangeNotificationLatencyMillis) {
            mPreparingChangeNotificationLatencyMillis = preparingChangeNotificationLatencyMillis;
            return this;
        }

        /** Sets the type indicate how this set schema call relative to schema migration cases */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSchemaMigrationCallType(
                @SchemaMigrationStats.SchemaMigrationCallType int schemaMigrationCallType) {
            Preconditions.checkArgumentInRange(schemaMigrationCallType, NO_MIGRATION,
                    SECOND_CALL_APPLY_NEW_SCHEMA, "schemaMigrationCallType");
            mSchemaMigrationCallType = schemaMigrationCallType;
            return this;
        }

        /** Builds a new {@link SetSchemaStats} from the {@link Builder}. */
        @NonNull
        public SetSchemaStats build() {
            return new SetSchemaStats(/* builder= */ this);
        }
    }
}
