/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.work.WorkRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Parcelable} representation of a list of {@link androidx.work.WorkRequest}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkRequests implements Parcelable {
    private final List<WorkRequest> mRequests;

    public ParcelableWorkRequests(@NonNull List<WorkRequest> requests) {
        mRequests = requests;
    }

    protected ParcelableWorkRequests(@NonNull Parcel in) {
        Parcelable[] parcelledArray = in.readParcelableArray(getClass().getClassLoader());
        mRequests = new ArrayList<>(parcelledArray.length);
        for (Parcelable parcelable : parcelledArray) {
            ParcelableWorkRequest parcelled = (ParcelableWorkRequest) parcelable;
            mRequests.add(parcelled.getWorkRequest());
        }
    }

    public static final Creator<ParcelableWorkRequests> CREATOR =
            new Creator<ParcelableWorkRequests>() {
                @Override
                public ParcelableWorkRequests createFromParcel(Parcel in) {
                    return new ParcelableWorkRequests(in);
                }

                @Override
                public ParcelableWorkRequests[] newArray(int size) {
                    return new ParcelableWorkRequests[size];
                }
            };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        ParcelableWorkRequest[] parcelledArray = new ParcelableWorkRequest[mRequests.size()];
        for (int i = 0; i < mRequests.size(); i++) {
            parcelledArray[i] = new ParcelableWorkRequest(mRequests.get(i));
        }
        parcel.writeParcelableArray(parcelledArray, flags);
    }

    @NonNull
    public List<WorkRequest> getRequests() {
        return mRequests;
    }
}
