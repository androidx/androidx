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

package androidx.wear.watchface.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Wire format to encode a pair of id to {@link ComplicationStateWireFormat}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class IdAndComplicationStateWireFormat implements VersionedParcelable, Parcelable {
    /** The watch's ID for the complication. */
    @ParcelField(1)
    int mId;

    /** The {@link ComplicationStateWireFormat} for the complication. */
    @ParcelField(2)
    @NonNull
    ComplicationStateWireFormat mComplicationState;

    /** Used by VersionedParcelable. */
    IdAndComplicationStateWireFormat() {
    }

    public IdAndComplicationStateWireFormat(
            int id, @NonNull ComplicationStateWireFormat complicationState) {
        mId = id;
        mComplicationState = complicationState;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public ComplicationStateWireFormat getComplicationState() {
        return mComplicationState;
    }

    /** Serializes this IdAndComplicationStateWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<IdAndComplicationStateWireFormat> CREATOR =
            new Parcelable.Creator<IdAndComplicationStateWireFormat>() {
                @Override
                public IdAndComplicationStateWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public IdAndComplicationStateWireFormat[] newArray(int size) {
                    return new IdAndComplicationStateWireFormat[size];
                }
            };
}
