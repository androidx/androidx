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

/** Gms Reference: com.google.android.gms.nearby.uwb.UwbComplexChannel
 *
 * @hide
 */
@SuppressLint({"ParcelNotFinal", "BanParcelableUsage"})
public class UwbComplexChannel implements android.os.Parcelable
{
    @SuppressLint("MutableBareField")
    public int channel = 0;
    @SuppressLint("MutableBareField")
    public int preambleIndex = 0;
    @NonNull
    public static final android.os.Parcelable.Creator<UwbComplexChannel> CREATOR = new android.os.Parcelable.Creator<UwbComplexChannel>() {
        @Override
        public UwbComplexChannel createFromParcel(android.os.Parcel _aidl_source) {
            UwbComplexChannel _aidl_out = new UwbComplexChannel();
            _aidl_out.readFromParcel(_aidl_source);
            return _aidl_out;
        }
        @Override
        public UwbComplexChannel[] newArray(int _aidl_size) {
            return new UwbComplexChannel[_aidl_size];
        }
    };
    @Override public final void writeToParcel(@NonNull Parcel _aidl_parcel, int _aidl_flag)
    {
        int _aidl_start_pos = _aidl_parcel.dataPosition();
        _aidl_parcel.writeInt(0);
        _aidl_parcel.writeInt(channel);
        _aidl_parcel.writeInt(preambleIndex);
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
            channel = _aidl_parcel.readInt();
            if (_aidl_parcel.dataPosition() - _aidl_start_pos >= _aidl_parcelable_size) return;
            preambleIndex = _aidl_parcel.readInt();
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
