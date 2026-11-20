/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.aidl.AppSearchBatchResultParcelV2;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.GetWrappedBlobEncryptionKeysResponseCreator;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * The response to provide batch operation results of {@link
 * AppSearchSession#getWrappedBlobEncryptionKeysAsync}.
 *
 * <p>This class is used to retrieve the wrapped blob encryption keys of a collection of blob
 * handles.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@FlaggedApi(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
// NonCanonicalType: Field, Constructor and Param are declared in AbstractSafeParcelable in
// Jetpack and in SafeParcelable in the framework.
@SuppressWarnings({"HiddenSuperclass", "NonCanonicalType"})
@SafeParcelable.Class(creator = "GetWrappedBlobEncryptionKeysResponseCreator")
public final class GetWrappedBlobEncryptionKeysResponse implements SafeParcelable {

    public static final @NonNull Creator<GetWrappedBlobEncryptionKeysResponse> CREATOR =
            new GetWrappedBlobEncryptionKeysResponseCreator();

    @AbstractSafeParcelable.Field(id = 1, getter = "getResponseParcel")
    private final AppSearchBatchResultParcelV2<AppSearchBlobHandle, WrappedBlobEncryptionKey>
            mResultParcel;

    /**
     * Creates a {@link GetWrappedBlobEncryptionKeysResponse} with given {@link
     * AppSearchBatchResult}.
     */
    public GetWrappedBlobEncryptionKeysResponse(
            @NonNull AppSearchBatchResult<AppSearchBlobHandle, WrappedBlobEncryptionKey> result) {
        this(AppSearchBatchResultParcelV2.fromBlobHandleToWrappedBlobEncryptionKey(result));
    }

    @AbstractSafeParcelable.Constructor
    GetWrappedBlobEncryptionKeysResponse(
            @AbstractSafeParcelable.Param(id = 1)
                    @NonNull
                            AppSearchBatchResultParcelV2<
                                    AppSearchBlobHandle, WrappedBlobEncryptionKey>
                            resultParcel) {
        mResultParcel = Objects.requireNonNull(resultParcel);
    }

    /**
     * Returns the {@link AppSearchBatchResult} object containing the result for each {@link
     * AppSearchBlobHandle}.
     *
     * @return A {@link AppSearchBatchResult} mapping each {@link AppSearchBlobHandle} to its {@link
     *     WrappedBlobEncryptionKey} if successful, or an {@link AppSearchResult} with details of
     *     the failure.
     */
    public @NonNull AppSearchBatchResult<AppSearchBlobHandle, WrappedBlobEncryptionKey>
            getResult() {
        return mResultParcel.getResult();
    }

    /** Retrieves the underlying parcel representation of the batch result. */
    @HideInPlatform
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @NonNull AppSearchBatchResultParcelV2<AppSearchBlobHandle, WrappedBlobEncryptionKey>
            getResponseParcel() {
        return mResultParcel;
    }

    /**
     * Always returns 0 because this class does not contain file descriptors.
     *
     * <p>Defined here instead of inheriting it from {@code AbstractSafeParcelable}, whose final
     * {@code describeContents()} carries an unrelated, already finalized API flag.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Writes this object to the given {@link Parcel}. */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        GetWrappedBlobEncryptionKeysResponseCreator.writeToParcel(this, dest, flags);
    }
}
