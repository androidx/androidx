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
import androidx.work.WorkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link android.os.Parcelable} representation of a {@link java.util.List} of
 * {@link androidx.work.WorkInfo}s.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkInfos implements Parcelable {
    private final List<WorkInfo> mInfos;

    public ParcelableWorkInfos(@NonNull List<WorkInfo> infos) {
        mInfos = infos;
    }

    @NonNull
    public List<WorkInfo> getWorkInfos() {
        return mInfos;
    }

    protected ParcelableWorkInfos(@NonNull Parcel parcel) {
        Parcelable[] parcelledArray = parcel.readParcelableArray(getClass().getClassLoader());
        mInfos = new ArrayList<>(parcelledArray.length);
        for (Parcelable parcelable : parcelledArray) {
            ParcelableWorkInfo parcelled = (ParcelableWorkInfo) parcelable;
            mInfos.add(parcelled.getWorkInfo());
        }
    }

    public static final Creator<ParcelableWorkInfos> CREATOR = new Creator<ParcelableWorkInfos>() {
        @Override
        public ParcelableWorkInfos createFromParcel(Parcel in) {
            return new ParcelableWorkInfos(in);
        }

        @Override
        public ParcelableWorkInfos[] newArray(int size) {
            return new ParcelableWorkInfos[size];
        }
    };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        ParcelableWorkInfo[] parcelledArray = new ParcelableWorkInfo[mInfos.size()];
        for (int i = 0; i < mInfos.size(); i++) {
            parcelledArray[i] = new ParcelableWorkInfo(mInfos.get(i));
        }
        parcel.writeParcelableArray(parcelledArray, flags);
    }
}
