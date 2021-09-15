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

package androidx.wear.watchface.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Pair of a ComplicationSlotID and a TapEvent, the wire format of Map<Int, TapEvent>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize(allowSerialization = true)
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class IdAndTapEventWireFormat implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    @Px
    int mId;

    @ParcelField(2)
    @Px
    int mX;

    @ParcelField(3)
    @Px
    int mY;

    @ParcelField(4)
    @Px
    long mCalendarTapTimeMillis;

    IdAndTapEventWireFormat() {
    }

    public IdAndTapEventWireFormat(
            int id,
            int x,
            int y,
            long calendarTapTimeMillis) {
        mId = id;
        mX = x;
        mY = y;
        mCalendarTapTimeMillis = calendarTapTimeMillis;
    }

    public int getId() {
        return mId;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public long getCalendarTapTimeMillis() {
        return mCalendarTapTimeMillis;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Serializes this IdAndTapEventWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Parcelable.Creator<IdAndTapEventWireFormat> CREATOR =
            new Parcelable.Creator<IdAndTapEventWireFormat>() {
                @Override
                public IdAndTapEventWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public IdAndTapEventWireFormat[] newArray(int size) {
                    return new IdAndTapEventWireFormat[size];
                }
            };
}
