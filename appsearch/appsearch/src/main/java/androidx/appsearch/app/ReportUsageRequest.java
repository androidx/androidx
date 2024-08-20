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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.CurrentTimeMillisLong;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.ReportUsageRequestCreator;
import androidx.core.util.Preconditions;

import java.util.Objects;

/**
 * A request to report usage of a document.
 *
 * <p>See {@link AppSearchSession#reportUsageAsync} for a detailed description of usage reporting.
 *
 * @see AppSearchSession#reportUsageAsync
 */
@SuppressWarnings("HiddenSuperclass")
@SafeParcelable.Class(creator = "ReportUsageRequestCreator")
public final class ReportUsageRequest extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull public static final Parcelable.Creator<ReportUsageRequest> CREATOR =
            new ReportUsageRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getNamespace")
    private final String mNamespace;
    @NonNull
    @Field(id = 2, getter = "getDocumentId")
    private final String mDocumentId;
    @Field(id = 3, getter = "getUsageTimestampMillis")
    private final  long mUsageTimestampMillis;

    @Constructor
    ReportUsageRequest(
            @Param(id = 1) @NonNull String namespace,
            @Param(id = 2) @NonNull String documentId,
            @Param(id = 3) long usageTimestampMillis) {
        mNamespace = Objects.requireNonNull(namespace);
        mDocumentId = Objects.requireNonNull(documentId);
        mUsageTimestampMillis = usageTimestampMillis;
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

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        ReportUsageRequestCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link ReportUsageRequest} objects. */
    public static final class Builder {
        private final String mNamespace;
        private final String mDocumentId;
        private Long mUsageTimestampMillis;

        /**
         * Creates a new {@link ReportUsageRequest.Builder} instance.
         *
         * @param namespace    The namespace of the document that was used (such as from
         *                     {@link GenericDocument#getNamespace}.
         * @param documentId   The ID of document that was used (such as from
         *                     {@link GenericDocument#getId}.
         */
        public Builder(@NonNull String namespace, @NonNull String documentId) {
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
         * {@link ReportUsageRequest} is constructed.
         */
        @CanIgnoreReturnValue
        @NonNull
        public ReportUsageRequest.Builder setUsageTimestampMillis(
                @CurrentTimeMillisLong long usageTimestampMillis) {
            mUsageTimestampMillis = usageTimestampMillis;
            return this;
        }

        /** Builds a new {@link ReportUsageRequest}. */
        @NonNull
        public ReportUsageRequest build() {
            if (mUsageTimestampMillis == null) {
                mUsageTimestampMillis = System.currentTimeMillis();
            }
            return new ReportUsageRequest(mNamespace, mDocumentId, mUsageTimestampMillis);
        }
    }
}
