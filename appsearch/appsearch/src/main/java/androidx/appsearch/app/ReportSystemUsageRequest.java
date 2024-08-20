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

package androidx.appsearch.app;

import androidx.annotation.NonNull;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.CurrentTimeMillisLong;
import androidx.core.util.Preconditions;

/**
 * A request to report usage of a document owned by another app from a system UI surface.
 *
 * <p>Usage reported in this way is measured separately from usage reported via
 * {@link AppSearchSession#reportUsageAsync}.
 *
 * <p>See {@link GlobalSearchSession#reportSystemUsageAsync} for a detailed description of usage
 * reporting.
 */
public final class ReportSystemUsageRequest {
    private final String mPackageName;
    private final String mDatabase;
    private final String mNamespace;
    private final String mDocumentId;
    private final long mUsageTimestampMillis;

    ReportSystemUsageRequest(
            @NonNull String packageName,
            @NonNull String database,
            @NonNull String namespace,
            @NonNull String documentId,
            long usageTimestampMillis) {
        mPackageName = Preconditions.checkNotNull(packageName);
        mDatabase = Preconditions.checkNotNull(database);
        mNamespace = Preconditions.checkNotNull(namespace);
        mDocumentId = Preconditions.checkNotNull(documentId);
        mUsageTimestampMillis = usageTimestampMillis;
    }

    /** Returns the package name of the app which owns the document that was used. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the database in which the document that was used resides. */
    @NonNull
    public String getDatabaseName() {
        return mDatabase;
    }

    /** Returns the namespace of the document that was used. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the ID of document that was used. */
    @NonNull
    public String getDocumentId() {
        return mDocumentId;
    }

    /**
     * Returns the timestamp in milliseconds of the usage report (the time at which the document
     * was used).
     *
     * <p>The value is in the {@link System#currentTimeMillis} time base.
     */
    @CurrentTimeMillisLong
    public long getUsageTimestampMillis() {
        return mUsageTimestampMillis;
    }

    /** Builder for {@link ReportSystemUsageRequest} objects. */
    public static final class Builder {
        private final String mPackageName;
        private final String mDatabase;
        private final String mNamespace;
        private final String mDocumentId;
        private Long mUsageTimestampMillis;

        /**
         * Creates a {@link ReportSystemUsageRequest.Builder} instance.
         *
         * @param packageName  The package name of the app which owns the document that was used
         *                     (such as from {@link SearchResult#getPackageName}).
         * @param databaseName The database in which the document that was used resides (such as
         *                     from {@link SearchResult#getDatabaseName}).
         * @param namespace    The namespace of the document that was used (such as from
         *                     {@link GenericDocument#getNamespace}.
         * @param documentId   The ID of document that was used (such as from
         *                     {@link GenericDocument#getId}.
         */
        public Builder(
                @NonNull String packageName,
                @NonNull String databaseName,
                @NonNull String namespace,
                @NonNull String documentId) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabase = Preconditions.checkNotNull(databaseName);
            mNamespace = Preconditions.checkNotNull(namespace);
            mDocumentId = Preconditions.checkNotNull(documentId);
        }

        /**
         * Sets the timestamp in milliseconds of the usage report (the time at which the document
         * was used).
         *
         * <p>The value is in the {@link System#currentTimeMillis} time base.
         *
         * <p>If unset, this defaults to the current timestamp at the time that the
         * {@link ReportSystemUsageRequest} is constructed.
         */
        @CanIgnoreReturnValue
        @NonNull
        public ReportSystemUsageRequest.Builder setUsageTimestampMillis(
                @CurrentTimeMillisLong long usageTimestampMillis) {
            mUsageTimestampMillis = usageTimestampMillis;
            return this;
        }

        /** Builds a new {@link ReportSystemUsageRequest}. */
        @NonNull
        public ReportSystemUsageRequest build() {
            if (mUsageTimestampMillis == null) {
                mUsageTimestampMillis = System.currentTimeMillis();
            }
            return new ReportSystemUsageRequest(
                    mPackageName, mDatabase, mNamespace, mDocumentId, mUsageTimestampMillis);
        }
    }
}
