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

package androidx.appsearch.app;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.InternalSetSchemaResponseCreator;
import androidx.core.util.Preconditions;

/**
 * An internal wrapper class of {@link SetSchemaResponse}.
 *
 * <p>For public users, if the {@link androidx.appsearch.app.AppSearchSession#setSchemaAsync}
 * failed, we will directly throw an Exception. But AppSearch internal need to divert the
 * incompatible changes form other call flows. This class adds a {@link #isSuccess()} to indicate
 * if the call fails because of incompatible change.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SafeParcelable.Class(creator = "InternalSetSchemaResponseCreator")
public class InternalSetSchemaResponse extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull public static final Parcelable.Creator<InternalSetSchemaResponse> CREATOR =
            new InternalSetSchemaResponseCreator();

    @Field(id = 1, getter = "isSuccess")
    private final boolean mIsSuccess;

    @Field(id = 2, getter = "getSetSchemaResponse")
    private final SetSchemaResponse mSetSchemaResponse;
    @Field(id = 3, getter = "getErrorMessage")
    @Nullable private final String mErrorMessage;

    @Constructor
    public InternalSetSchemaResponse(
            @Param(id = 1) boolean isSuccess,
            @Param(id = 2) @NonNull SetSchemaResponse setSchemaResponse,
            @Param(id = 3) @Nullable String errorMessage) {
        Preconditions.checkNotNull(setSchemaResponse);
        mIsSuccess = isSuccess;
        mSetSchemaResponse = setSchemaResponse;
        mErrorMessage = errorMessage;
    }

    /**
     * Creates a new successful {@link InternalSetSchemaResponse}.
     *
     * @param setSchemaResponse  The object this internal object represents.
     */
    @NonNull
    public static InternalSetSchemaResponse newSuccessfulSetSchemaResponse(
            @NonNull SetSchemaResponse setSchemaResponse) {
        return new InternalSetSchemaResponse(/*isSuccess=*/ true, setSchemaResponse,
                /*errorMessage=*/null);
    }

    /**
     * Creates a new failed {@link InternalSetSchemaResponse}.
     *
     * @param setSchemaResponse  The object this internal object represents.
     * @param errorMessage       An string describing the reason or nature of the failure.
     */
    @NonNull
    public static InternalSetSchemaResponse newFailedSetSchemaResponse(
            @NonNull SetSchemaResponse setSchemaResponse,
            @NonNull String errorMessage) {
        return new InternalSetSchemaResponse(/*isSuccess=*/ false, setSchemaResponse,
                errorMessage);
    }

    /** Returns {@code true} if the schema request is proceeded successfully. */
    public boolean isSuccess() {
        return mIsSuccess;
    }

    /**
     * Returns the {@link SetSchemaResponse} of the set schema call.
     *
     * <p>The call may or may not success. Check {@link #isSuccess()} before call this method.
     */
    @NonNull
    public SetSchemaResponse getSetSchemaResponse() {
        return mSetSchemaResponse;
    }


    /**
     * Returns the error message associated with this response.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}.
     */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        InternalSetSchemaResponseCreator.writeToParcel(this, dest, flags);
    }
}
