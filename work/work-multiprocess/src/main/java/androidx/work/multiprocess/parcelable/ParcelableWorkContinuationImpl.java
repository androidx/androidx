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

import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkRequest;
import androidx.work.impl.WorkContinuationImpl;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.WorkRequestHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link androidx.work.impl.WorkContinuationImpl}, but parcelable.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableWorkContinuationImpl implements Parcelable {

    private static final ExistingWorkPolicy[] sValues = ExistingWorkPolicy.values();

    private WorkContinuationImplInfo mInfo;

    public ParcelableWorkContinuationImpl(@NonNull WorkContinuationImpl continuation) {
        mInfo = new WorkContinuationImplInfo(continuation);
    }

    public ParcelableWorkContinuationImpl(@NonNull WorkContinuationImplInfo info) {
        mInfo = info;
    }

    @NonNull
    public WorkContinuationImplInfo getInfo() {
        return mInfo;
    }

    @SuppressWarnings("unchecked")
    protected ParcelableWorkContinuationImpl(@NonNull Parcel parcel) {
        // name
        String name = null;
        boolean hasName = readBooleanValue(parcel);
        if (hasName) {
            name = parcel.readString();
        }
        // workPolicy
        int ordinal = parcel.readInt();
        ExistingWorkPolicy workPolicy = sValues[ordinal];
        // workRequests
        int requestSize = parcel.readInt();
        List<WorkRequestHolder> requests = new ArrayList<>(requestSize);
        ClassLoader loader = getClass().getClassLoader();
        for (int i = 0; i < requestSize; i++) {
            ParcelableWorkRequest parcelledRequest = parcel.readParcelable(loader);
            // We always serialize to a WorkRequestHolder
            WorkRequestHolder requestHolder = (WorkRequestHolder) parcelledRequest.getWorkRequest();
            requests.add(requestHolder);
        }
        // parents
        List<WorkContinuationImplInfo> parents = null;
        boolean hasParents = readBooleanValue(parcel);
        if (hasParents) {
            int parentsSize = parcel.readInt();
            parents = new ArrayList<>(parentsSize);
            for (int i = 0; i < parentsSize; i++) {
                ParcelableWorkContinuationImpl parcelledContinuation =
                        parcel.readParcelable(loader);
                parents.add(parcelledContinuation.getInfo());
            }
        }
        mInfo = new WorkContinuationImplInfo(name, workPolicy, requests, parents);
    }

    public static final Creator<ParcelableWorkContinuationImpl> CREATOR =
            new Creator<ParcelableWorkContinuationImpl>() {
                @Override
                public ParcelableWorkContinuationImpl createFromParcel(@NonNull Parcel in) {
                    return new ParcelableWorkContinuationImpl(in);
                }

                @Override
                public ParcelableWorkContinuationImpl[] newArray(int size) {
                    return new ParcelableWorkContinuationImpl[size];
                }
            };

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // name
        String name = mInfo.getName();
        boolean hasName = !TextUtils.isEmpty(name);
        writeBooleanValue(parcel, hasName);
        if (hasName) {
            parcel.writeString(name);
        }
        // workPolicy
        ExistingWorkPolicy policy = mInfo.getExistingWorkPolicy();
        parcel.writeInt(policy.ordinal());
        // workRequests
        List<? extends WorkRequest> requests = mInfo.getWork();
        parcel.writeInt(requests.size());
        if (!requests.isEmpty()) {
            for (int i = 0; i < requests.size(); i++) {
                WorkRequest request = requests.get(i);
                ParcelableWorkRequest parcelledRequest = new ParcelableWorkRequest(request);
                parcel.writeParcelable(parcelledRequest, flags);
            }
        }
        // parents
        List<WorkContinuationImplInfo> parents = mInfo.getParentInfos();
        boolean hasParents = parents != null && !parents.isEmpty();
        writeBooleanValue(parcel, hasParents);
        if (hasParents) {
            parcel.writeInt(parents.size());
            for (int i = 0; i < parents.size(); i++) {
                ParcelableWorkContinuationImpl parcelledContinuationImpl =
                        new ParcelableWorkContinuationImpl(parents.get(i));
                parcel.writeParcelable(parcelledContinuationImpl, flags);
            }
        }
    }

    /**
     * Converts an instance of {@link ParcelableWorkContinuationImpl} to a
     * {@link WorkContinuationImpl}.
     *
     * @param workManager The {@link  WorkManagerImpl} instance.
     * @return The {@link WorkContinuationImpl} instance
     */
    @NonNull
    public WorkContinuationImpl toWorkContinuationImpl(@NonNull WorkManagerImpl workManager) {
        return mInfo.toWorkContinuationImpl(workManager);
    }

    /**
     * Holds all the information that needs to be tracked inside a
     * {@link androidx.work.WorkContinuation}.
     */
    public static class WorkContinuationImplInfo {
        private final String mName;
        private final ExistingWorkPolicy mWorkPolicy;
        private final List<? extends WorkRequest> mRequests;
        private List<WorkContinuationImplInfo> mParents;

        public WorkContinuationImplInfo(@NonNull WorkContinuationImpl continuation) {
            mName = continuation.getName();
            mWorkPolicy = continuation.getExistingWorkPolicy();
            mRequests = continuation.getWork();
            List<WorkContinuationImpl> continuations = continuation.getParents();
            mParents = null;
            if (continuations != null) {
                mParents = new ArrayList<>(continuations.size());
                for (WorkContinuationImpl workContinuation : continuations) {
                    mParents.add(new WorkContinuationImplInfo(workContinuation));
                }
            }
        }

        public WorkContinuationImplInfo(
                @Nullable String name,
                @NonNull ExistingWorkPolicy workPolicy,
                @NonNull List<? extends WorkRequest> requests,
                @Nullable List<WorkContinuationImplInfo> parents) {
            mName = name;
            mWorkPolicy = workPolicy;
            mRequests = requests;
            mParents = parents;
        }

        @Nullable
        public String getName() {
            return mName;
        }

        @NonNull
        public ExistingWorkPolicy getExistingWorkPolicy() {
            return mWorkPolicy;
        }

        @NonNull
        public List<? extends WorkRequest> getWork() {
            return mRequests;
        }

        @Nullable
        public List<WorkContinuationImplInfo> getParentInfos() {
            return mParents;
        }

        /**
         * Converts an instance of {@link WorkContinuationImplInfo} to a
         * {@link androidx.work.WorkContinuation}.
         *
         * @param workManager The {@link  WorkManagerImpl} instance.
         * @return The {@link WorkContinuationImpl} instance
         */
        @NonNull
        public WorkContinuationImpl toWorkContinuationImpl(@NonNull WorkManagerImpl workManager) {
            return new WorkContinuationImpl(
                    workManager,
                    getName(),
                    getExistingWorkPolicy(),
                    getWork(),
                    parents(workManager, getParentInfos())
            );
        }

        @Nullable
        private static List<WorkContinuationImpl> parents(
                @NonNull WorkManagerImpl workManager,
                @Nullable List<WorkContinuationImplInfo> parentInfos) {

            if (parentInfos == null) {
                return null;
            }

            List<WorkContinuationImpl> continuations = new ArrayList<>(parentInfos.size());
            for (WorkContinuationImplInfo info : parentInfos) {
                continuations.add(
                        new WorkContinuationImpl(
                                workManager,
                                info.getName(),
                                info.getExistingWorkPolicy(),
                                info.getWork(),
                                parents(workManager, info.getParentInfos()))
                );
            }
            return continuations;
        }
    }
}
