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

package androidx.work.multiprocess.parcelable;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Data;
import androidx.work.ListenableWorker;

/**
 * {@link androidx.work.ListenableWorker.Result}, but parcelable.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableResult implements Parcelable {
    private final ListenableWorker.Result mResult;

    public ParcelableResult(@NonNull ListenableWorker.Result result) {
        mResult = result;
    }

    public ParcelableResult(@NonNull Parcel in) {
        // resultType
        int resultType = in.readInt();
        // outputData
        ParcelableData parcelableOutputData = new ParcelableData(in);
        mResult = intToResultType(resultType, parcelableOutputData.getData());
    }

    public static final Creator<ParcelableResult> CREATOR =
            new Creator<ParcelableResult>() {
                @Override
                @NonNull
                public ParcelableResult createFromParcel(Parcel in) {
                    return new ParcelableResult(in);
                }

                @Override
                public ParcelableResult[] newArray(int size) {
                    return new ParcelableResult[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        int resultType = resultTypeOf(mResult);
        // resultType
        parcel.writeInt(resultType);
        // outputData
        Data outputData = mResult.getOutputData();
        ParcelableData parcelableOutputData = new ParcelableData(outputData);
        parcelableOutputData.writeToParcel(parcel, flags);
    }

    @NonNull
    public ListenableWorker.Result getResult() {
        return mResult;
    }

    private static int resultTypeOf(ListenableWorker.Result result) {
        if (result instanceof ListenableWorker.Result.Retry) {
            return 1;
        } else if (result instanceof ListenableWorker.Result.Success) {
            return 2;
        } else if (result instanceof ListenableWorker.Result.Failure) {
            return 3;
        } else {
            // Exhaustive check
            throw new IllegalStateException(String.format("Unknown Result %s", result));
        }
    }

    @NonNull
    private static ListenableWorker.Result intToResultType(int resultType, @NonNull Data data) {
        ListenableWorker.Result result = null;
        if (resultType == 1) {
            result = ListenableWorker.Result.retry();
        } else if (resultType == 2) {
            result = ListenableWorker.Result.success(data);
        } else if (resultType == 3) {
            result = ListenableWorker.Result.failure(data);
        } else {
            // Exhaustive check
            throw new IllegalStateException(String.format("Unknown result type %s", resultType));
        }
        return result;
    }
}
