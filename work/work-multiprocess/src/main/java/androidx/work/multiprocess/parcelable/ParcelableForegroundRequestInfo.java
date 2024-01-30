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

package androidx.work.multiprocess.parcelable;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.ForegroundInfo;

/**
 * ForegroundInfo but parcelable.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableForegroundRequestInfo implements Parcelable {

    private final String mId;
    private final ForegroundInfo mForegroundInfo;

    public ParcelableForegroundRequestInfo(
            @NonNull String id,
            @NonNull ForegroundInfo foregroundInfo) {
        mId = id;
        mForegroundInfo = foregroundInfo;
    }

    public ParcelableForegroundRequestInfo(@NonNull Parcel in) {
        // id
        mId = in.readString();
        // notificationId
        int notificationId = in.readInt();
        // foregroundServiceType
        int foregroundServiceType = in.readInt();
        // notification
        Notification notification = in.readParcelable(getClass().getClassLoader());
        mForegroundInfo = new ForegroundInfo(notificationId, notification, foregroundServiceType);
    }

    public static final Creator<ParcelableForegroundRequestInfo> CREATOR =
            new Creator<ParcelableForegroundRequestInfo>() {
                @Override
                public ParcelableForegroundRequestInfo createFromParcel(Parcel in) {
                    return new ParcelableForegroundRequestInfo(in);
                }

                @Override
                public ParcelableForegroundRequestInfo[] newArray(int size) {
                    return new ParcelableForegroundRequestInfo[size];
                }
            };


    @NonNull
    public ForegroundInfo getForegroundInfo() {
        return mForegroundInfo;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @Override
    public int describeContents() {
        // No file descriptors being returned.
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        // id
        parcel.writeString(mId);
        // notificationId
        parcel.writeInt(mForegroundInfo.getNotificationId());
        // foregroundServiceType
        parcel.writeInt(mForegroundInfo.getForegroundServiceType());
        // notification
        parcel.writeParcelable(mForegroundInfo.getNotification(), flags);
    }
}
