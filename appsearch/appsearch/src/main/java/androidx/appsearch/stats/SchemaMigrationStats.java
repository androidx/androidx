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

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.util.BundleUtil;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class holds detailed stats for Schema migration.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SchemaMigrationStats {

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

    private static final String PACKAGE_NAME_FIELD = "packageName";
    private static final String DATABASE_FIELD = "database";
    private static final String STATUS_CODE_FIELD = "StatusCode";
    private static final String EXECUTOR_ACQUISITION_MILLIS_FIELD =
            "ExecutorAcquisitionLatencyMillis";
    private static final String TOTAL_LATENCY_MILLIS_FIELD = "totalLatencyMillis";
    private static final String GET_SCHEMA_LATENCY_MILLIS_FIELD = "getSchemaLatencyMillis";
    private static final String QUERY_AND_TRANSFORM_LATENCY_MILLIS_FIELD =
            "queryAndTransformLatencyMillis";
    private static final String FIRST_SET_SCHEMA_LATENCY_MILLIS_FIELD =
            "firstSetSchemaLatencyMillis";
    private static final String IS_FIRST_SET_SCHEMA_SUCCESS_FIELD = "isFirstSetSchemaSuccess";
    private static final String SECOND_SET_SCHEMA_LATENCY_MILLIS_FIELD =
            "secondSetSchemaLatencyMillis";
    private static final String SAVE_DOCUMENT_LATENCY_MILLIS_FIELD = "saveDocumentLatencyMillis";
    private static final String TOTAL_NEED_MIGRATED_DOCUMENT_COUNT_FIELD =
            "totalNeedMigratedDocumentCount";
    private static final String MIGRATION_FAILURE_COUNT_FIELD = "migrationFailureCount";
    private static final String TOTAL_SUCCESS_MIGRATED_DOCUMENT_COUNT_FIELD =
            "totalSuccessMigratedDocumentCount";

    /**
     * Contains all {@link SchemaMigrationStats} information in a packaged format.
     *
     * <p>Keys are the {@code *_FIELD} constants in this class.
     */
    @NonNull
    final Bundle mBundle;

    /** Build a {@link SchemaMigrationStats} from the given bundle. */
    public SchemaMigrationStats(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     *
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
    }

    /** Returns calling package name. */
    @NonNull
    public String getPackageName() {
        return mBundle.getString(PACKAGE_NAME_FIELD);
    }

    /** Returns calling database name. */
    @NonNull
    public String getDatabase() {
        return mBundle.getString(DATABASE_FIELD);
    }

    /** Returns status of the schema migration action. */
    @AppSearchResult.ResultCode
    public int getStatusCode() {
        return mBundle.getInt(STATUS_CODE_FIELD);
    }

    /** Gets the latency for waiting the executor. */
    public int getExecutorAcquisitionLatencyMillis() {
        return mBundle.getInt(EXECUTOR_ACQUISITION_MILLIS_FIELD);
    }

    /** Gets total latency for the schema migration action in milliseconds. */
    public int getTotalLatencyMillis() {
        return mBundle.getInt(TOTAL_LATENCY_MILLIS_FIELD);
    }

    /** Returns GetSchema latency in milliseconds. */
    public int getGetSchemaLatencyMillis() {
        return mBundle.getInt(GET_SCHEMA_LATENCY_MILLIS_FIELD);
    }

    /**
     * Returns latency of querying all documents that need to be migrated to new version and
     * transforming documents to new version in milliseconds.
     */
    public int getQueryAndTransformLatencyMillis() {
        return mBundle.getInt(QUERY_AND_TRANSFORM_LATENCY_MILLIS_FIELD);
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
        return mBundle.getInt(FIRST_SET_SCHEMA_LATENCY_MILLIS_FIELD);
    }

    /** Returns whether the first SetSchema action success. */
    public boolean isFirstSetSchemaSuccess() {
        return mBundle.getBoolean(IS_FIRST_SET_SCHEMA_SUCCESS_FIELD);
    }

    /**
     * Returns latency of second SetSchema action in milliseconds.
     *
     * <p>If all schema fields are backward compatible, the schema will be successful set to
     * Icing in the first setSchema action and this value will be 0. Otherwise, schema types will
     * be set to Icing by this action.
     */
    public int getSecondSetSchemaLatencyMillis() {
        return mBundle.getInt(SECOND_SET_SCHEMA_LATENCY_MILLIS_FIELD);
    }

    /** Returns latency of putting migrated document to Icing lib in milliseconds. */
    public int getSaveDocumentLatencyMillis() {
        return mBundle.getInt(SAVE_DOCUMENT_LATENCY_MILLIS_FIELD);
    }

    /** Returns number of document that need to be migrated to another version. */
    public int getTotalNeedMigratedDocumentCount() {
        return mBundle.getInt(TOTAL_NEED_MIGRATED_DOCUMENT_COUNT_FIELD);
    }

    /** Returns number of {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}. */
    public int getMigrationFailureCount() {
        return mBundle.getInt(MIGRATION_FAILURE_COUNT_FIELD);
    }

    /** Returns number of successfully migrated and saved in Icing. */
    public int getTotalSuccessMigratedDocumentCount() {
        return mBundle.getInt(TOTAL_SUCCESS_MIGRATED_DOCUMENT_COUNT_FIELD);
    }

    /** Builder for {@link SchemaMigrationStats}. */
    public static class Builder {

        private final Bundle mBundle;

        /** Creates a {@link SchemaMigrationStats.Builder}. */
        public Builder(@NonNull String packageName, @NonNull String database) {
            mBundle = new Bundle();
            mBundle.putString(PACKAGE_NAME_FIELD, packageName);
            mBundle.putString(DATABASE_FIELD, database);
        }

        /**
         * Creates a {@link SchemaMigrationStats.Builder} from a given {@link SchemaMigrationStats}.
         *
         * <p>The returned builder is a deep copy whose data is separate from this
         * SchemaMigrationStats.
         */
        public Builder(@NonNull SchemaMigrationStats stats) {
            mBundle = BundleUtil.deepCopy(stats.mBundle);
        }

        /**
         * Creates a new {@link SchemaMigrationStats.Builder} from the given Bundle
         *
         * <p>The bundle is NOT copied.
         */
        public Builder(@NonNull Bundle bundle) {
            mBundle = Preconditions.checkNotNull(bundle);
        }

        /** Sets status code for the schema migration action. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setStatusCode(@AppSearchResult.ResultCode int statusCode) {
            mBundle.putInt(STATUS_CODE_FIELD, statusCode);
            return this;
        }

        /** Sets the latency for waiting the executor. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setExecutorAcquisitionLatencyMillis(int executorAcquisitionLatencyMillis) {
            mBundle.putInt(EXECUTOR_ACQUISITION_MILLIS_FIELD, executorAcquisitionLatencyMillis);
            return this;
        }


        /** Sets total latency for the schema migration action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalLatencyMillis(int totalLatencyMillis) {
            mBundle.putInt(TOTAL_LATENCY_MILLIS_FIELD, totalLatencyMillis);
            return this;
        }

        /** Sets latency for the GetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setGetSchemaLatencyMillis(int getSchemaLatencyMillis) {
            mBundle.putInt(GET_SCHEMA_LATENCY_MILLIS_FIELD, getSchemaLatencyMillis);
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
            mBundle.putInt(QUERY_AND_TRANSFORM_LATENCY_MILLIS_FIELD,
                    queryAndTransformLatencyMillis);
            return this;
        }

        /** Sets latency of first SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setFirstSetSchemaLatencyMillis(
                int firstSetSchemaLatencyMillis) {
            mBundle.putInt(FIRST_SET_SCHEMA_LATENCY_MILLIS_FIELD, firstSetSchemaLatencyMillis);
            return this;
        }

        /** Returns status of the first SetSchema action. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setIsFirstSetSchemaSuccess(boolean isFirstSetSchemaSuccess) {
            mBundle.putBoolean(IS_FIRST_SET_SCHEMA_SUCCESS_FIELD, isFirstSetSchemaSuccess);
            return this;
        }

        /** Sets latency of second SetSchema action in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSecondSetSchemaLatencyMillis(
                int secondSetSchemaLatencyMillis) {
            mBundle.putInt(SECOND_SET_SCHEMA_LATENCY_MILLIS_FIELD, secondSetSchemaLatencyMillis);
            return this;
        }

        /** Sets latency for putting migrated document to Icing lib in milliseconds. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setSaveDocumentLatencyMillis(
                int saveDocumentLatencyMillis) {
            mBundle.putInt(SAVE_DOCUMENT_LATENCY_MILLIS_FIELD, saveDocumentLatencyMillis);
            return this;
        }

        /** Sets number of document that need to be migrated to another version. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalNeedMigratedDocumentCount(int migratedDocumentCount) {
            mBundle.putInt(TOTAL_NEED_MIGRATED_DOCUMENT_COUNT_FIELD, migratedDocumentCount);
            return this;
        }

        /** Sets total document count of successfully migrated and saved in Icing. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTotalSuccessMigratedDocumentCount(
                int totalSuccessMigratedDocumentCount) {
            mBundle.putInt(TOTAL_SUCCESS_MIGRATED_DOCUMENT_COUNT_FIELD,
                    totalSuccessMigratedDocumentCount);
            return this;
        }

        /** Sets number of {@link androidx.appsearch.app.SetSchemaResponse.MigrationFailure}. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setMigrationFailureCount(int migrationFailureCount) {
            mBundle.putInt(MIGRATION_FAILURE_COUNT_FIELD, migrationFailureCount);
            return this;
        }

        /**
         * Builds a new {@link SchemaMigrationStats} from the {@link SchemaMigrationStats.Builder}.
         */
        @NonNull
        public SchemaMigrationStats build() {
            return new SchemaMigrationStats(mBundle);
        }
    }
}
