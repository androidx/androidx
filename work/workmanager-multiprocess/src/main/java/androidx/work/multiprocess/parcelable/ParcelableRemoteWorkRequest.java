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
import androidx.work.WorkerParameters;

/**
 * Everything you need to run a {@link androidx.work.multiprocess.RemoteListenableWorker}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableRemoteWorkRequest implements Parcelable {
    // We are holding on to parcelables here instead of the actual deserialized representation.
    // This is because, to create an instance of WorkerParameters we need the application context
    // using which we can determine the configuration, taskExecutor to use etc.

    private final String mWorkerClassName;
    private final ParcelableWorkerParameters mParcelableWorkerParameters;

    public ParcelableRemoteWorkRequest(
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {

        mWorkerClassName = workerClassName;
        mParcelableWorkerParameters = new ParcelableWorkerParameters(workerParameters);
    }

    protected ParcelableRemoteWorkRequest(@NonNull Parcel in) {
        // workerClassName
        mWorkerClassName = in.readString();
        // parcelableWorkerParameters
        mParcelableWorkerParameters = new ParcelableWorkerParameters(in);
    }

    public static final Creator<ParcelableRemoteWorkRequest> CREATOR =
            new Creator<ParcelableRemoteWorkRequest>() {
                @Override
                public ParcelableRemoteWorkRequest createFromParcel(Parcel in) {
                    return new ParcelableRemoteWorkRequest(in);
                }

                @Override
                public ParcelableRemoteWorkRequest[] newArray(int size) {
                    return new ParcelableRemoteWorkRequest[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // workerClassName
        parcel.writeString(mWorkerClassName);
        // parcelableWorkerParameters
        mParcelableWorkerParameters.writeToParcel(parcel, flags);
    }

    @NonNull
    public String getWorkerClassName() {
        return mWorkerClassName;
    }

    @NonNull
    public ParcelableWorkerParameters getParcelableWorkerParameters() {
        return mParcelableWorkerParameters;
    }
}
