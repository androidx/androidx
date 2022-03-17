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

package androidx.wear.watchface.style.data;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;
import androidx.wear.watchface.complications.data.DefaultComplicationDataSourcePolicyWireFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class UserStyleFlavorWireFormat implements VersionedParcelable, Parcelable {
    @ParcelField(1)
    @NonNull
    /** User style identifier. */
    public String mId = "";

    @ParcelField(2)
    @NonNull
    /** User style definition of the flavor. */
    public UserStyleWireFormat mStyle = new UserStyleWireFormat();

    @ParcelField(3)
    @NonNull
    /** Map of ComplicationSlot id to complication's default for the flavor. */
    public Map<Integer, DefaultComplicationDataSourcePolicyWireFormat> mComplications =
            new HashMap<>();

    UserStyleFlavorWireFormat() {}

    public UserStyleFlavorWireFormat(
            @NonNull String id,
            @NonNull UserStyleWireFormat style,
            @NonNull Map<Integer, DefaultComplicationDataSourcePolicyWireFormat> complications) {
        mId = id;
        mStyle = style;
        mComplications = complications;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Serializes this UserStyleSchemaWireFormat to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Creator<UserStyleFlavorWireFormat> CREATOR =
            new Creator<UserStyleFlavorWireFormat>() {
                @SuppressWarnings("deprecation")
                @Override
                public UserStyleFlavorWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public UserStyleFlavorWireFormat[] newArray(int size) {
                    return new UserStyleFlavorWireFormat[size];
                }
            };
}
