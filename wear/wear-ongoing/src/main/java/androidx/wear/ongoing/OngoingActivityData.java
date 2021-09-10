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
package androidx.wear.ongoing;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.LocusIdCompat;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * This class is used internally by the library to represent the data of an OngoingActivity and
 * serialize/deserialize using VersionedParcelable.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@VersionedParcelize
class OngoingActivityData implements VersionedParcelable {
    @Nullable
    @ParcelField(value = 1, defaultValue = "null")
    Icon mAnimatedIcon;

    @NonNull
    @ParcelField(value = 2)
    Icon mStaticIcon;

    @Nullable
    @ParcelField(value = 3, defaultValue = "null")
    OngoingActivityStatus mStatus;

    @NonNull
    @ParcelField(value = 4)
    PendingIntent mTouchIntent;

    @Nullable
    @ParcelField(value = 5, defaultValue = "null")
    String mLocusId;

    @ParcelField(value = 6, defaultValue = "-1")
    int mOngoingActivityId;

    @Nullable
    @ParcelField(value = 7, defaultValue = "null")
    String mCategory;

    @ParcelField(value = 8)
    long mTimestamp;

    @Nullable
    @ParcelField(value = 9, defaultValue = "null")
    String mTitle;

    // Required by VersionedParcelable
    OngoingActivityData() {
    }

    OngoingActivityData(
            @Nullable Icon animatedIcon,
            @NonNull Icon staticIcon,
            @Nullable OngoingActivityStatus status,
            @NonNull PendingIntent touchIntent,
            @Nullable String locusId,
            int ongoingActivityId,
            @Nullable String category,
            long timestamp,
            @Nullable String title
    ) {
        mAnimatedIcon = animatedIcon;
        mStaticIcon = staticIcon;
        mStatus = status;
        mTouchIntent = touchIntent;
        mLocusId = locusId;
        mOngoingActivityId = ongoingActivityId;
        mCategory = category;
        mTimestamp = timestamp;
        mTitle = title;
    }

    @Nullable
    Icon getAnimatedIcon() {
        return mAnimatedIcon;
    }

    @NonNull
    Icon getStaticIcon() {
        return mStaticIcon;
    }

    @Nullable
    OngoingActivityStatus getStatus() {
        return mStatus;
    }

    @NonNull
    PendingIntent getTouchIntent() {
        return mTouchIntent;
    }

    @Nullable
    LocusIdCompat getLocusId() {
        return mLocusId == null ? null : new LocusIdCompat(mLocusId);
    }

    int getOngoingActivityId() {
        return mOngoingActivityId;
    }

    @Nullable
    String getCategory() {
        return mCategory;
    }

    long getTimestamp() {
        return mTimestamp;
    }

    @Nullable
    String getTitle() {
        return mTitle;
    }

    // Status is mutable, by the library.
    void setStatus(@NonNull OngoingActivityStatus status) {
        mStatus = status;
    }
}

