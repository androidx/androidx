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
import androidx.annotation.Nullable;

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingPosition
 *
 * @hide
 */
@SuppressLint({"ParcelNotFinal", "BanParcelableUsage"})
public class RangingPosition implements android.os.Parcelable
{
    @Nullable
    @SuppressLint("MutableBareField")
    public androidx.core.uwb.backend.RangingMeasurement distance;
    @Nullable
    @SuppressLint("MutableBareField")
    public androidx.core.uwb.backend.RangingMeasurement azimuth;
    @Nullable
    @SuppressLint("MutableBareField")
    public androidx.core.uwb.backend.RangingMeasurement elevation;
    @SuppressLint("MutableBareField")
    public long elapsedRealtimeNanos = 0L;
    @NonNull
    public static final android.os.Parcelable.Creator<RangingPosition> CREATOR = new android.os.Parcelable.Creator<RangingPosition>() {
        @Override
        public RangingPosition createFromParcel(android.os.Parcel _aidl_source) {
            RangingPosition _aidl_out = new RangingPosition();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }
        @Override
        public RangingPosition[] newArray(int _aidl_size) {
            return new RangingPosition[_aidl_size];
        }
    };
    @Override public final void writeToParcel(@NonNull Parcel _aidl_parcel, int _aidl_flag)
    {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeTypedObject(distance, _aidl_flag);
        _aidl_parcel.writeTypedObject(azimuth, _aidl_flag);
        _aidl_parcel.writeTypedObject(elevation, _aidl_flag);
        _aidl_parcel.writeLong(elapsedRealtimeNanos);
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
            distance = _aidl_parcel.readTypedObject(androidx.core.uwb.backend.RangingMeasurement.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            azimuth = _aidl_parcel.readTypedObject(androidx.core.uwb.backend.RangingMeasurement.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            elevation = _aidl_parcel.readTypedObject(androidx.core.uwb.backend.RangingMeasurement.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            elapsedRealtimeNanos = _aidl_parcel.readLong();
        } finally {
            if (_aidl_start_pos > (Integer.MAX_VALUE - _aidl_parcelable_size)) {
                throw new android.os.BadParcelableException("Overflow in the size of parcelable");
            }
            _aidl_parcel.setDataPosition(_aidl_start_pos + _aidl_parcelable_size);
        }
    }
    @Override
    public int describeContents() {
        int _mask = 0;
        _mask |= describeContents(distance);
        _mask |= describeContents(azimuth);
        _mask |= describeContents(elevation);
        return _mask;
    }
    private int describeContents(Object _v) {
        if (_v == null) return 0;
        if (_v instanceof android.os.Parcelable) {
            return ((android.os.Parcelable) _v).describeContents();
        }
        return 0;
    }
}
