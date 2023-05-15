/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * 
 * This file is auto-generated.  DO NOT MODIFY.
 */
package androidx.core.uwb.backend;

import android.annotation.SuppressLint;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingCapabilities
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint({"ParcelNotFinal", "BanParcelableUsage"})
public class RangingCapabilities implements android.os.Parcelable
{
    @SuppressLint("MutableBareField")
    public boolean supportsDistance = false;
    @SuppressLint("MutableBareField")
    public boolean supportsAzimuthalAngle = false;
    @SuppressLint("MutableBareField")
    public boolean supportsElevationAngle = false;
    @SuppressLint("MutableBareField")
    public int minRangingInterval = 0;
    @NonNull
    @SuppressLint("MutableBareField")
    public int[] supportedChannels;
    @NonNull
    @SuppressLint("MutableBareField")
    public int[] supportedNtfConfigs;
    @NonNull
    @SuppressLint("MutableBareField")
    public int[] supportedConfigIds;
    @NonNull
    public static final android.os.Parcelable.Creator<RangingCapabilities> CREATOR = new android.os.Parcelable.Creator<RangingCapabilities>() {
        @Override
        public RangingCapabilities createFromParcel(android.os.Parcel _aidl_source) {
            RangingCapabilities _aidl_out = new RangingCapabilities();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }
        @Override
        public RangingCapabilities[] newArray(int _aidl_size) {
            return new RangingCapabilities[_aidl_size];
        }
    };
    @Override public final void writeToParcel(@NonNull Parcel _aidl_parcel, int _aidl_flag)
    {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeBoolean(supportsDistance);
        _aidl_parcel.writeBoolean(supportsAzimuthalAngle);
        _aidl_parcel.writeBoolean(supportsElevationAngle);
        _aidl_parcel.writeInt(minRangingInterval);
        _aidl_parcel.writeIntArray(supportedChannels);
        _aidl_parcel.writeIntArray(supportedNtfConfigs);
        _aidl_parcel.writeIntArray(supportedConfigIds);
        int _aidl_end_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.setDataPosition(_aidl_start_pos);
        _aidl_parcel.writeInt(_aidl_end_pos - _aidl_start_pos);
        _aidl_parcel.setDataPosition(_aidl_end_pos);
    }
    @SuppressLint("Finally")
    public final void readFromParcel(@NonNull Parcel _aidl_parcel)
    {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        int _aidl_parcelable_size = _aidl_parcel.readInt();
        try {
            if (_aidl_parcelable_size < 4) throw new android.os.BadParcelableException("Parcelable too small");;
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportsDistance = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportsAzimuthalAngle = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportsElevationAngle = _aidl_parcel.readBoolean();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            minRangingInterval = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportedChannels = _aidl_parcel.createIntArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportedNtfConfigs = _aidl_parcel.createIntArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            supportedConfigIds = _aidl_parcel.createIntArray();
        } finally {
            if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
                throw new android.os.BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
        }
    }
    @Override
    public int describeContents() {
        return 0;
    }
}
