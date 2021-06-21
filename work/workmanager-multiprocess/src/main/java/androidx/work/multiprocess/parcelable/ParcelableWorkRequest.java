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

import static androidx.work.impl.model.WorkTypeConverters.backoffPolicyToInt;
import static androidx.work.impl.model.WorkTypeConverters.intToBackoffPolicy;
import static androidx.work.impl.model.WorkTypeConverters.intToState;
import static androidx.work.impl.model.WorkTypeConverters.stateToInt;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkRequestHolder;
import androidx.work.impl.model.WorkSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@link WorkRequest}, but parcelable.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkRequest implements Parcelable {
    private final WorkRequest mWorkRequest;

    public ParcelableWorkRequest(@NonNull WorkRequest workRequest) {
        mWorkRequest = workRequest;
    }

    protected ParcelableWorkRequest(@NonNull Parcel in) {
        // id
        String id = in.readString();
        // tags
        Set<String> tagsSet = new HashSet<>(in.createStringArrayList());
        // workerClassName
        String workerClassName = in.readString();
        WorkSpec workSpec = new WorkSpec(id, workerClassName);
        // inputMergerClassName
        workSpec.inputMergerClassName = in.readString();
        // state
        workSpec.state = intToState(in.readInt());
        // inputData
        ParcelableData parcelableInputData = new ParcelableData(in);
        workSpec.input = parcelableInputData.getData();
        // outputData
        ParcelableData parcelableOutputData = new ParcelableData(in);
        workSpec.output = parcelableOutputData.getData();
        // initialDelay
        workSpec.initialDelay = in.readLong();
        // intervalDuration
        workSpec.intervalDuration = in.readLong();
        // flexDuration
        workSpec.flexDuration = in.readLong();
        // runAttemptCount
        workSpec.runAttemptCount = in.readInt();
        // constraints
        ParcelableConstraints parcelableConstraints =
                in.readParcelable(getClass().getClassLoader());
        workSpec.constraints = parcelableConstraints.getConstraints();
        // backOffPolicy
        workSpec.backoffPolicy = intToBackoffPolicy(in.readInt());
        // backOffDelayDuration
        workSpec.backoffDelayDuration = in.readLong();
        // minimumRetentionDuration
        workSpec.minimumRetentionDuration = in.readLong();
        // scheduleRequestedAt
        workSpec.scheduleRequestedAt = in.readLong();
        mWorkRequest = new WorkRequestHolder(UUID.fromString(id), workSpec, tagsSet);
    }

    public static final Creator<ParcelableWorkRequest> CREATOR =
            new Creator<ParcelableWorkRequest>() {
                @Override
                public ParcelableWorkRequest createFromParcel(Parcel in) {
                    return new ParcelableWorkRequest(in);
                }

                @Override
                public ParcelableWorkRequest[] newArray(int size) {
                    return new ParcelableWorkRequest[size];
                }
            };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // id
        parcel.writeString(mWorkRequest.getStringId());
        // tags
        List<String> tags = new ArrayList<>(mWorkRequest.getTags());
        parcel.writeStringList(tags);
        WorkSpec workSpec = mWorkRequest.getWorkSpec();
        // workerClassName
        parcel.writeString(workSpec.workerClassName);
        // inputMergerClassName
        parcel.writeString(workSpec.inputMergerClassName);
        // state
        parcel.writeInt(stateToInt(workSpec.state));
        // inputData
        ParcelableData parcelableInputData = new ParcelableData(workSpec.input);
        parcelableInputData.writeToParcel(parcel, flags);
        // outputData
        ParcelableData parcelableOutputData = new ParcelableData(workSpec.output);
        parcelableOutputData.writeToParcel(parcel, flags);
        // initialDelay
        parcel.writeLong(workSpec.initialDelay);
        // intervalDuration
        parcel.writeLong(workSpec.intervalDuration);
        // flexDuration
        parcel.writeLong(workSpec.flexDuration);
        // runAttemptCount
        parcel.writeInt(workSpec.runAttemptCount);
        // constraints
        parcel.writeParcelable(new ParcelableConstraints(workSpec.constraints), flags);
        // backOffPolicy
        parcel.writeInt(backoffPolicyToInt(workSpec.backoffPolicy));
        // backOffDelayDuration
        parcel.writeLong(workSpec.backoffDelayDuration);
        // minimumRetentionDuration
        parcel.writeLong(workSpec.minimumRetentionDuration);
        // scheduleRequestedAt
        parcel.writeLong(workSpec.scheduleRequestedAt);
    }

    @NonNull
    public WorkRequest getWorkRequest() {
        return mWorkRequest;
    }
}
