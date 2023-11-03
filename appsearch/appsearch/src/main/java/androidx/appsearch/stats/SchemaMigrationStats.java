/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appsearch.stats;

import android.os.Parcel;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.SchemaMigrationStatsCreator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Class holds detailed stats for Schema migration.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "SchemaMigrationStatsCreator")
public final class SchemaMigrationStats extends AbstractSafeParcelable {
    @NonNull public static final SchemaMigrationStatsCreator CREATOR =
            new SchemaMigrationStatsCreator();

    // Indicate the how a SetSchema call relative to SchemaMigration case.
    @IntDef(
            value = {
                    NO_MIGRATION,
                    FIRST_CALL_GET_INCOMPATIBLE,
                    SECOND_CALL_APPLY_NEW_SCHEMA,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SchemaMigrationCallType {}

    /**  This SetSchema call is not relative to a SchemaMigration case. */
    public static final int NO_MIGRATION = 0;
    /**  This is the first SetSchema call in Migration cases to get all incompatible changes. */
    public static final int FIRST_CALL_GET_INCOMPATIBLE = 1;
    /**  This is the second SetSchema call in Migration cases to apply new schema changes */
    public static final int SECOND_CALL_APPLY_NEW_SCHEMA = 2;

    @Field(id = 1, getter = "getPackageName")
    @NonNull
    private final String mPackageName;

    @Field(id = 2, getter = "getDatabase")
    @NonNull
    private final String mDatabase;

    @Field(id = 3, getter = "getStatusCode")
    private final int mStatusCode;

    @Field(id = 4, getter = "getExecutorAcquisitionLatencyMillis")
    private final int mExecutorAcquisitionLatencyMillis;

    @Field(id = 5, getter = "getTotalLatencyMillis")
    private final int mTotalLatencyMillis;

    @Field(id = 6, getter = "getGetSchemaLatencyMillis")
    private final int mGetSchemaLatencyMillis;

    @Field(id = 7, getter = "getQueryAndTransformLatencyMillis")
    private final int mQueryAndTransformLatencyMillis;

    @Field(id = 8, getter = "getFirstSetSchemaLatencyMillis")
    private final int mFirstSetSchemaLatencyMillis;

    @Field(id = 9, getter = "isFirstSetSchemaSuccess")
    private final boolean mIsFirstSetSchemaSuccess;

    @Field(id = 10, getter = "getSecondSetSchemaLatencyMillis")
    private final int mSecondSetSchemaLatencyMillis;

    @Field(id = 11, getter = "getSaveDocumentLatencyMillis")
    private final int mSaveDocumentLatencyMillis;

    @Field(id = 12, getter = "getTotalNeedMigratedDocumentCount")
    private final int mTotalNeedMigratedDocumentCount;

    @Field(id = 13, getter = "getMigrationFailureCount")
    private final int mMigrationFailureCount;

    @Field(id = 14, getter = "getTotalSuccessMigratedDocumentCount")
    private final int mTotalSuccessMigratedDocumentCount;

    /** Build a {@link SchemaMigrationStats} from the given parameters. */
    @Constructor
    public SchemaMigrationStats(
            @Param(id = 1) @NonNull String packageName,
            @Param(id = 2) @NonNull String database,
            @Param(id = 3) int statusCode,
            @Param(id = 4) int executorAcquisitionLatencyMillis,
            @Param(id = 5) int totalLatencyMillis,
            @Param(id = 6) int getSchemaLatencyMillis,
            @Param(id = 7) int queryAndTransformLatencyMillis,
            @Param(id = 8) int firstSetSchemaLatencyMillis,
            @Param(id = 9) boolean isFirstSetSchemaSuccess,
            @Param(id = 10) int secondSetSchemaLatencyMillis,
            @Param(id = 11) int saveDocumentLatencyMillis,
            @Param(id = 12) int totalNeedMigratedDocumentCount,
            @Param(id = 13) int migrationFailureCount,
            @Param(id = 14) int totalSuccessMigratedDocumentCount) {
        mPackageName = packageName;
        mDatabase = database;
        mStatusCode = statusCode;
        mExecutorAcquisitionLatencyMillis = executorAcquisitionLatencyMillis;
        mTotalLatencyMillis = totalLatencyMillis;
        mGetSchemaLatencyMillis = getSchemaLatencyMillis;
        mQueryAndTransformLatencyMillis = queryAndTransformLatencyMillis;
        mFirstSetSchemaLatencyMillis = firstSetSchemaLatencyMillis;
        mIsFirstSetSchemaSuccess = isFirstSetSchemaSuccess;
        mSecondSetSchemaLatencyMillis = secondSetSchemaLatencyMillis;
        mSaveDocumentLatencyMillis = saveDocumentLatencyMillis;
        mTotalNeedMigratedDocumentCount = totalNeedMigratedDocumentCount;
        mMigrationFailureCount = migrationFailureCount;
        mTotalSuccessMigratedDocumentCount = totalSuccessMigratedDocumentCount;
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

    /** Returns status of the schema migration action. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mStatusCode;
    }

    /** Gets the latency for waiting the executor. */
    public int getExecutorAcquisitionLatencyMillis() {
        return mExecutorAcquisitionLatencyMillis;
    }

    /** Gets total latency for the schema migration action in milliseconds. */
    public int getTotalLatencyMillis() {
        return mTotalLatencyMillis;
    }

    /** Returns GetSchema latency in milliseconds. */
    public int getGetSchemaLatencyMillis() {
        return mGetSchemaLatencyMillis;
    }

    /**
     * Returns latency of querying all documents that need to be migrated to new version and
     * transforming documents to new version in milliseconds.
     */
    public int getQueryAndTransformLatencyMillis() {
        return mQueryAndTransformLatencyMillis;
    }

    /**
     * Returns latency of first SetSchema action in milliseconds.
     *
     * <p>If all schema fields are backward compatible, the schema will be successful set to Icing.
     * Otherwise, we will retrieve incompatible types here.
     *
     * <p>Please see {@link SetSchemaRequest} for what is "incompatible".
     */
    public int getFirstSetSchemaLatencyMillis() {
        return mFirstSetSchemaLatencyMillis;
    }

    /** Returns whether the first SetSchema action success. */
    public boolean isFirstSetSchemaSuccess() {
        return mIsFirstSetSchemaSuccess;
    }

    /**
     * Returns latency of second SetSchema action in milliseconds.
     *
     * <p>If all schema fields are backward compatible, the schema will be successful set to
     * Icing in the first setSchema action and this value will be 0. Otherwise, schema types will
     * be set to Icing by this action.
     */
    public int getSecondSetSchemaLatencyMillis() {
        return mSecondSetSchemaLatencyMillis;
    }

    /** Returns latency of putting migrated document to Icing lib in milliseconds. */
    public int getSaveDocumentLatencyMillis() {
        return mSaveDocumentLatencyMillis;
    }

    /** Returns number of document that need to be migrated to another version. */
    public int getTotalNeedMigratedDocumentCount() {
        return mTotalNeedMigratedDocumentCount;
    }

    /** Returns number of {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}. */
    public int getMigrationFailureCount() {
        return mMigrationFailureCount;
    }

    /** Returns number of successfully migrated and saved in Icing. */
    public int getTotalSuccessMigratedDocumentCount() {
        return mTotalSuccessMigratedDocumentCount;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SchemaMigrationStatsCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link SchemaMigrationStats}. */
    public static class Builder {

        String mPackageName;
        String mDatabase;
        int mStatusCode;
        int mExecutorAcquisitionLatencyMillis;
        int mTotalLatencyMillis;
        int mGetSchemaLatencyMillis;
        int mQueryAndTransformLatencyMillis;
        int mFirstSetSchemaLatencyMillis;
        boolean mIsFirstSetSchemaSuccess;
        int mSecondSetSchemaLatencyMillis;
        int mSaveDocumentLatencyMillis;
        int mTotalNeedMigratedDocumentCount;
        int mMigrationFailureCount;
        int mTotalSuccessMigratedDocumentCount;

        /** Creates a {@link SchemaMigrationStats.Builder}. */
        public Builder(@NonNull String packageName, @NonNull String database) {
            mPackageName = Objects.requireNonNull(packageName);
            mDatabase = Objects.requireNonNull(database);
        }

        /**
         * Creates a {@link SchemaMigrationStats.Builder} from a given {@link SchemaMigrationStats}.
         *
         * <p>The returned builder is a deep copy whose data is separate from this
         * SchemaMigrationStats.
         */
        public Builder(@NonNull SchemaMigrationStats stats) {
            Objects.requireNonNull(stats);

            mPackageName = stats.mPackageName;
            mDatabase = stats.mDatabase;
            mStatusCode = stats.mStatusCode;
            mExecutorAcquisitionLatencyMillis = stats.mExecutorAcquisitionLatencyMillis;
            mTotalLatencyMillis = stats.mTotalLatencyMillis;
            mGetSchemaLatencyMillis = stats.mGetSchemaLatencyMillis;
            mQueryAndTransformLatencyMillis = stats.mQueryAndTransformLatencyMillis;
            mFirstSetSchemaLatencyMillis = stats.mFirstSetSchemaLatencyMillis;
            mIsFirstSetSchemaSuccess = stats.mIsFirstSetSchemaSuccess;
            mSecondSetSchemaLatencyMillis = stats.mSecondSetSchemaLatencyMillis;
            mSaveDocumentLatencyMillis = stats.mSaveDocumentLatencyMillis;
            mTotalNeedMigratedDocumentCount = stats.mTotalNeedMigratedDocumentCount;
            mMigrationFailureCount = stats.mMigrationFailureCount;
            mTotalSuccessMigratedDocumentCount = stats.mTotalSuccessMigratedDocumentCount;
        }

        /** Sets status code for the schema migration action. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mStatusCode = statusCode;
            return this;
        }

        /** Sets the latency for waiting the executor. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setExecutorAcquisitionLatencyMillis(int executorAcquisitionLatencyMillis) {
            mExecutorAcquisitionLatencyMillis = executorAcquisitionLatencyMillis;
            return this;
        }


        /** Sets total latency for the schema migration action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mTotalLatencyMillis = totalLatencyMillis;
            return this;
        }

        /** Sets latency for the GetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setGetSchemaLatencyMillis(int getSchemaLatencyMillis) {
            mGetSchemaLatencyMillis = getSchemaLatencyMillis;
            return this;
        }

        /**
         * Sets latency for querying all documents that need to be migrated to new version and
         * transforming documents to new version in milliseconds.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setQueryAndTransformLatencyMillis(
                int queryAndTransformLatencyMillis) {
            mQueryAndTransformLatencyMillis = queryAndTransformLatencyMillis;
            return this;
        }

        /** Sets latency of first SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setFirstSetSchemaLatencyMillis(
                int firstSetSchemaLatencyMillis) {
            mFirstSetSchemaLatencyMillis = firstSetSchemaLatencyMillis;
            return this;
        }

        /** Returns status of the first SetSchema action. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIsFirstSetSchemaSuccess(boolean isFirstSetSchemaSuccess) {
            mIsFirstSetSchemaSuccess = isFirstSetSchemaSuccess;
            return this;
        }

        /** Sets latency of second SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSecondSetSchemaLatencyMillis(
                int secondSetSchemaLatencyMillis) {
            mSecondSetSchemaLatencyMillis = secondSetSchemaLatencyMillis;
            return this;
        }

        /** Sets latency for putting migrated document to Icing lib in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSaveDocumentLatencyMillis(
                int saveDocumentLatencyMillis) {
            mSaveDocumentLatencyMillis = saveDocumentLatencyMillis;
            return this;
        }

        /** Sets number of document that need to be migrated to another version. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalNeedMigratedDocumentCount(int migratedDocumentCount) {
            mTotalNeedMigratedDocumentCount = migratedDocumentCount;
            return this;
        }

        /** Sets total document count of successfully migrated and saved in Icing. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalSuccessMigratedDocumentCount(
                int totalSuccessMigratedDocumentCount) {
            mTotalSuccessMigratedDocumentCount = totalSuccessMigratedDocumentCount;
            return this;
        }

        /** Sets number of {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setMigrationFailureCount(int migrationFailureCount) {
            mMigrationFailureCount = migrationFailureCount;
            return this;
        }

        /**
         * Builds a new {@link SchemaMigrationStats} from the {@link SchemaMigrationStats.Builder}.
         */
        @NonNull
        public SchemaMigrationStats build() {
            return new SchemaMigrationStats(
                    mPackageName,
                    mDatabase,
                    mStatusCode,
                    mExecutorAcquisitionLatencyMillis,
                    mTotalLatencyMillis,
                    mGetSchemaLatencyMillis,
                    mQueryAndTransformLatencyMillis,
                    mFirstSetSchemaLatencyMillis,
                    mIsFirstSetSchemaSuccess,
                    mSecondSetSchemaLatencyMillis,
                    mSaveDocumentLatencyMillis,
                    mTotalNeedMigratedDocumentCount,
                    mMigrationFailureCount,
                    mTotalSuccessMigratedDocumentCount);
        }
    }
}
