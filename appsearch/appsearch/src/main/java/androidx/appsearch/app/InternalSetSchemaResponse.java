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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
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
public class InternalSetSchemaResponse {

    private static final String IS_SUCCESS_FIELD = "isSuccess";
    private static final String SET_SCHEMA_RESPONSE_BUNDLE_FIELD = "setSchemaResponseBundle";
    private static final String ERROR_MESSAGE_FIELD = "errorMessage";

    private final Bundle mBundle;

    public InternalSetSchemaResponse(@NonNull Bundle bundle) {
        mBundle = Preconditions.checkNotNull(bundle);
    }

    private InternalSetSchemaResponse(boolean isSuccess,
            @NonNull SetSchemaResponse setSchemaResponse,
            @Nullable String errorMessage) {
        Preconditions.checkNotNull(setSchemaResponse);
        mBundle = new Bundle();
        mBundle.putBoolean(IS_SUCCESS_FIELD, isSuccess);
        mBundle.putBundle(SET_SCHEMA_RESPONSE_BUNDLE_FIELD, setSchemaResponse.getBundle());
        mBundle.putString(ERROR_MESSAGE_FIELD, errorMessage);
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     * @exportToFramework:hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Bundle getBundle() {
        return mBundle;
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
        return mBundle.getBoolean(IS_SUCCESS_FIELD);
    }

    /**
     * Returns the {@link SetSchemaResponse} of the set schema call.
     *
     * <p>The call may or may not success. Check {@link #isSuccess()} before call this method.
     */
    @NonNull
    public SetSchemaResponse getSetSchemaResponse() {
        return new SetSchemaResponse(mBundle.getBundle(SET_SCHEMA_RESPONSE_BUNDLE_FIELD));
    }


    /**
     * Returns the error message associated with this response.
     *
     * <p>If {@link #isSuccess} is {@code true}, the error message is always {@code null}.
     */
    @Nullable
    public String getErrorMessage() {
        return mBundle.getString(ERROR_MESSAGE_FIELD);
    }
}
