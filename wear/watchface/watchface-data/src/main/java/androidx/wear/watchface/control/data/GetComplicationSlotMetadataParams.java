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

package androidx.wear.watchface.control.data;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Parameters for {@link IWatchFaceControlService#getComplicationSlotMetadata}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class GetComplicationSlotMetadataParams implements VersionedParcelable, Parcelable {

    /** The {@link ComponentName} of the watchface family to create. */
    @ParcelField(1)
    @NonNull
    ComponentName mWatchFaceName;

    /** Used by VersionedParcelable. */
    GetComplicationSlotMetadataParams() {}

    public GetComplicationSlotMetadataParams(@NonNull ComponentName watchFaceName) {
        mWatchFaceName = watchFaceName;
    }

    @NonNull
    public ComponentName getWatchFaceName() {
        return mWatchFaceName;
    }

    /** Serializes this InteractiveWatchFaceInstanceParams to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<GetComplicationSlotMetadataParams> CREATOR =
            new Parcelable.Creator<GetComplicationSlotMetadataParams>() {
                @Override
                public GetComplicationSlotMetadataParams createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public GetComplicationSlotMetadataParams[] newArray(int size) {
                    return new GetComplicationSlotMetadataParams[size];
                }
            };
}
