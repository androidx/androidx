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

import java.util.ArrayList;
import java.util.List;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public class UserStyleSchemaWireFormat implements VersionedParcelable, Parcelable {
    @ParcelField(1)
    @NonNull
    /** List of user style categories comprising the style schema. */
    public List<UserStyleSettingWireFormat> mSchema = new ArrayList<UserStyleSettingWireFormat>();

    UserStyleSchemaWireFormat() {}

    public UserStyleSchemaWireFormat(@NonNull List<UserStyleSettingWireFormat> schema) {
        mSchema = schema;
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

    public static final Parcelable.Creator<UserStyleSchemaWireFormat> CREATOR =
            new Parcelable.Creator<UserStyleSchemaWireFormat>() {
                @Override
                public UserStyleSchemaWireFormat createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public UserStyleSchemaWireFormat[] newArray(int size) {
                    return new UserStyleSchemaWireFormat[size];
                }
            };
}
