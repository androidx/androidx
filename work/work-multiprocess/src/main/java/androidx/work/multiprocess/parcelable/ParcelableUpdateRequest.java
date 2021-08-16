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

import java.util.UUID;

/**
 * A {@link android.os.Parcelable} which has the necessary context to update progress for a
 * {@link androidx.work.ListenableWorker}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableUpdateRequest implements Parcelable {

    private final String mId;
    private final ParcelableData mParcelableData;

    public ParcelableUpdateRequest(@NonNull UUID id, @NonNull Data data) {
        mId = id.toString();
        mParcelableData = new ParcelableData(data);
    }

    protected ParcelableUpdateRequest(@NonNull Parcel in) {
        // id
        mId = in.readString();
        // data
        mParcelableData = new ParcelableData(in);
    }

    public static final Parcelable.Creator<ParcelableUpdateRequest> CREATOR =
            new Parcelable.Creator<ParcelableUpdateRequest>() {
                @Override
                public ParcelableUpdateRequest createFromParcel(@NonNull Parcel in) {
                    return new ParcelableUpdateRequest(in);
                }

                @Override
                public ParcelableUpdateRequest[] newArray(int size) {
                    return new ParcelableUpdateRequest[size];
                }
            };

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public Data getData() {
        return mParcelableData.getData();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // id
        parcel.writeString(mId);
        // data
        mParcelableData.writeToParcel(parcel, flags);
    }
}
