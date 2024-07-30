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

package androidx.appsearch.localstorage.converter;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchResult;

import com.google.android.icing.proto.StatusProto;

/**
 * Translates an {@link StatusProto.Code} into a {@link AppSearchResult.ResultCode}
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ResultCodeToProtoConverter {

    private static final String TAG = "AppSearchResultCodeToPr";
    private ResultCodeToProtoConverter() {}

    /** Converts an {@link StatusProto.Code} into a {@link AppSearchResult.ResultCode}. */
    @AppSearchResult.ResultCode public static int toResultCode(
            @NonNull StatusProto.Code statusCode) {
        switch (statusCode) {
            case OK:
                return AppSearchResult.RESULT_OK;
            case OUT_OF_SPACE:
                return AppSearchResult.RESULT_OUT_OF_SPACE;
            case INTERNAL:
                return AppSearchResult.RESULT_INTERNAL_ERROR;
            case UNKNOWN:
                return AppSearchResult.RESULT_UNKNOWN_ERROR;
            case NOT_FOUND:
                return AppSearchResult.RESULT_NOT_FOUND;
            case INVALID_ARGUMENT:
                return AppSearchResult.RESULT_INVALID_ARGUMENT;
            case ALREADY_EXISTS:
                return AppSearchResult.RESULT_ALREADY_EXISTS;
            default:
                // Some unknown/unsupported error
                Log.e(TAG, "Cannot convert IcingSearchEngine status code: "
                        + statusCode + " to AppSearchResultCode.");
                return AppSearchResult.RESULT_INTERNAL_ERROR;
        }
    }
}
