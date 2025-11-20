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
package androidx.appsearch.app;

import android.os.Parcel;
import android.os.Parcelable.Creator;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.aidl.AppSearchBatchResultParcelV2;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.RemoveBlobResponseCreator;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

/**
 * Results of {@link AppSearchSession#removeBlobAsync}, containing the outcome of the removal of
 * each handles.
 *
 * <p> This class is used to retrieve the result of a batch removal operation on a collection of
 * blob handles.
 */
@FlaggedApi(Flags.FLAG_ENABLE_BLOB_STORE)
@SuppressWarnings("HiddenSuperclass")
@SafeParcelable.Class(creator = "RemoveBlobResponseCreator")
public final class RemoveBlobResponse extends AbstractSafeParcelable {

    public static final @NonNull Creator<RemoveBlobResponse> CREATOR =
            new RemoveBlobResponseCreator();

    @Field(id = 1, getter = "getResponseParcel")
    private final AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> mResultParcel;

    /**
     * Creates a {@link RemoveBlobResponse} with given {@link AppSearchBatchResult}.
     */
    public RemoveBlobResponse(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> result) {
        this(AppSearchBatchResultParcelV2.fromBlobHandleToVoid(result));
    }

    @Constructor
    RemoveBlobResponse(
            @Param(id = 1) @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void>
                    resultParcel) {
        mResultParcel = Preconditions.checkNotNull(resultParcel);
    }

    /**
     * Returns the {@link AppSearchBatchResult} object containing the results of the removal
     * operation for each {@link AppSearchBlobHandle}.
     *
     * @return A {@link AppSearchBatchResult} maps {@link AppSearchBlobHandle}s which is a unique
     * identifier for a specific blob being removed to the outcome of that commit. If the
     * operation was successful, the result for that handle is {@code null}; if there was an error,
     * the result contains an {@link AppSearchResult} with details of the failure.
     */
    public @NonNull AppSearchBatchResult<AppSearchBlobHandle, Void> getResult() {
        return mResultParcel.getResult();
    }

    /**
     * Retrieves the underlying parcel representation of the batch result.
     * @exportToFramework:hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, Void> getResponseParcel() {
        return mResultParcel;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        RemoveBlobResponseCreator.writeToParcel(this, dest, flags);
    }
}
