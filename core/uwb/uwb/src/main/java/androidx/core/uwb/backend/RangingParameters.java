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
import androidx.annotation.RestrictTo;

/** Gms Reference: com.google.android.gms.nearby.uwb.RangingParameters
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint({"ParcelNotFinal", "BanParcelableUsage"})
public class RangingParameters implements android.os.Parcelable
{
    @SuppressLint("MutableBareField")
    public int uwbConfigId = 0;
    @SuppressLint("MutableBareField")
    public int sessionId = 0;
    @SuppressLint("MutableBareField")
    public int subSessionId = 0;
    @Nullable
    @SuppressLint("MutableBareField")
    public byte[] sessionKeyInfo;
    @Nullable
    @SuppressLint("MutableBareField")
    public byte[] subSessionKeyInfo;
    @Nullable
    @SuppressLint("MutableBareField")
    public androidx.core.uwb.backend.UwbComplexChannel complexChannel;
    @Nullable
    @SuppressLint({"MutableBareField", "NullableCollection"})
    public java.util.List<androidx.core.uwb.backend.UwbDevice> peerDevices;
    @SuppressLint("MutableBareField")
    public int rangingUpdateRate = 0;
    @NonNull
    public static final android.os.Parcelable.Creator<RangingParameters> CREATOR = new android.os.Parcelable.Creator<RangingParameters>() {
        @Override
        public RangingParameters createFromParcel(android.os.Parcel _aidl_source) {
            RangingParameters _aidl_out = new RangingParameters();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }
        @Override
        public RangingParameters[] newArray(int _aidl_size) {
            return new RangingParameters[_aidl_size];
        }
    };
    @Override public final void writeToParcel(@NonNull Parcel _aidl_parcel, int _aidl_flag)
    {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(uwbConfigId);
        _aidl_parcel.writeInt(sessionId);
        _aidl_parcel.writeInt(subSessionId);
        _aidl_parcel.writeByteArray(sessionKeyInfo);
        _aidl_parcel.writeByteArray(subSessionKeyInfo);
        _aidl_parcel.writeTypedObject(complexChannel, _aidl_flag);
        _aidl_parcel.writeTypedList(peerDevices);
        _aidl_parcel.writeInt(rangingUpdateRate);
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
            uwbConfigId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            sessionId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            subSessionId = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            sessionKeyInfo = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            subSessionKeyInfo = _aidl_parcel.createByteArray();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            complexChannel = _aidl_parcel.readTypedObject(androidx.core.uwb.backend.UwbComplexChannel.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            peerDevices = _aidl_parcel.createTypedArrayList(androidx.core.uwb.backend.UwbDevice.CREATOR);
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            rangingUpdateRate = _aidl_parcel.readInt();
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
        _mask |= describeContents(complexChannel);
        _mask |= describeContents(peerDevices);
        return _mask;
    }
    private int describeContents(Object _v) {
        if (_v == null) return 0;
        if (_v instanceof java.util.Collection) {
            int _mask = 0;
            for (Object o : (java.util.Collection) _v) {
                _mask |= describeContents(o);
            }
            return _mask;
        }
        if (_v instanceof android.os.Parcelable) {
            return ((android.os.Parcelable) _v).describeContents();
        }
        return 0;
    }
}
