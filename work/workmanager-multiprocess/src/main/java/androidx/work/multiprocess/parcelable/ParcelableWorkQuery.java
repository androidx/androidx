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

import static androidx.work.impl.model.WorkTypeConverters.intToState;
import static androidx.work.impl.model.WorkTypeConverters.stateToInt;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.WorkInfo;
import androidx.work.WorkQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A parcelable {@link WorkQuery} for multiprocess WorkManager.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkQuery implements Parcelable {

    private final WorkQuery mWorkQuery;

    public ParcelableWorkQuery(@NonNull WorkQuery workQuery) {
        mWorkQuery = workQuery;
    }

    protected ParcelableWorkQuery(@NonNull Parcel parcel) {
        // ids
        List<UUID> ids = Collections.emptyList();
        int length = parcel.readInt();
        if (length > 0) {
            ids = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                ids.add(UUID.fromString(parcel.readString()));
            }
        }
        // uniqueNames
        List<String> uniqueWorkNames = parcel.createStringArrayList();
        // tags
        List<String> tags = parcel.createStringArrayList();
        // states
        List<WorkInfo.State> states = Collections.emptyList();
        length = parcel.readInt();
        if (length > 0) {
            states = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                states.add(intToState(parcel.readInt()));
            }
        }

        mWorkQuery = WorkQuery.Builder.fromIds(ids)
                .addUniqueWorkNames(uniqueWorkNames)
                .addTags(tags)
                .addStates(states)
                .build();
    }

    @NonNull
    public WorkQuery getWorkQuery() {
        return mWorkQuery;
    }

    public static final Creator<ParcelableWorkQuery> CREATOR = new Creator<ParcelableWorkQuery>() {
        @Override
        public ParcelableWorkQuery createFromParcel(Parcel in) {
            return new ParcelableWorkQuery(in);
        }

        @Override
        public ParcelableWorkQuery[] newArray(int size) {
            return new ParcelableWorkQuery[size];
        }
    };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // ids
        List<UUID> ids = mWorkQuery.getIds();
        parcel.writeInt(ids.size());
        if (!ids.isEmpty()) {
            for (UUID id : ids) {
                parcel.writeString(id.toString());
            }
        }
        // uniqueNames
        List<String> uniqueNames = mWorkQuery.getUniqueWorkNames();
        parcel.writeStringList(uniqueNames);
        // tags
        List<String> tags = mWorkQuery.getTags();
        parcel.writeStringList(tags);
        // states
        List<WorkInfo.State> states = mWorkQuery.getStates();
        parcel.writeInt(states.size());
        if (!states.isEmpty()) {
            for (WorkInfo.State state : states) {
                parcel.writeInt(stateToInt(state));
            }
        }
    }
}
