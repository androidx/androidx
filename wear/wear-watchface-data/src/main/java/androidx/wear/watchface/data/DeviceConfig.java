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
 * Data sent over AIDL during watch face creation for device characteristics that cannot change
 * dynamically.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@VersionedParcelize(allowSerialization = true)
@SuppressLint("BanParcelableUsage") // TODO(b/169214666): Remove Parcelable
public final class DeviceConfig implements VersionedParcelable, Parcelable {

    @ParcelField(1)
    boolean mHasLowBitAmbient;

    @ParcelField(2)
    boolean mHasBurnInProtection;

    @ParcelField(4)
    long mAnalogPreviewReferenceTimeMillis;

    @ParcelField(5)
    long mDigitalPreviewReferenceTimeMillis;

    /** Used by VersionedParcelable. */
    DeviceConfig() {}

    public DeviceConfig(
            boolean hasLowBitAmbient,
            boolean hasBurnInProtection,
            long analogPreviewReferenceTimeMillis,
            long digitalPreviewReferenceTimeMillis) {
        mHasLowBitAmbient = hasLowBitAmbient;
        mHasBurnInProtection = hasBurnInProtection;
        mAnalogPreviewReferenceTimeMillis = analogPreviewReferenceTimeMillis;
        mDigitalPreviewReferenceTimeMillis = digitalPreviewReferenceTimeMillis;
    }

    public boolean getHasLowBitAmbient() {
        return mHasLowBitAmbient;
    }

    public boolean getHasBurnInProtection() {
        return mHasBurnInProtection;
    }

    public long getAnalogPreviewReferenceTimeMillis() {
        return mAnalogPreviewReferenceTimeMillis;
    }

    public long getDigitalPreviewReferenceTimeMillis() {
        return mDigitalPreviewReferenceTimeMillis;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** Serializes this ImmutableSystemState to the specified {@link Parcel}. */
    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeParcelable(ParcelUtils.toParcelable(this), flags);
    }

    public static final Parcelable.Creator<DeviceConfig> CREATOR =
            new Parcelable.Creator<DeviceConfig>() {
                @Override
                public DeviceConfig createFromParcel(Parcel source) {
                    return ParcelUtils.fromParcelable(
                            source.readParcelable(getClass().getClassLoader()));
                }

                @Override
                public DeviceConfig[] newArray(int size) {
                    return new DeviceConfig[size];
                }
            };
}
