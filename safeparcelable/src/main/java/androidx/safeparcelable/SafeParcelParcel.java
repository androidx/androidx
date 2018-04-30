/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.safeparcelable;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseIntArray;

import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SafeParcelParcel extends SafeParcel {

    private final Parcel mParcel;
    private final SparseIntArray mPositionLookup = new SparseIntArray();
    private final int mOffset;
    private int mCurrentField = -1;
    private int mNextRead = 0;

    public SafeParcelParcel(Parcel p) {
        this(p, p.dataPosition());
    }

    public SafeParcelParcel(Parcel p, int offset) {
        mParcel = p;
        mOffset = offset;
        mNextRead = mOffset;
    }

    private int readUntilField(int fieldId) {
        while (mNextRead < mParcel.dataSize()) {
            mParcel.setDataPosition(mNextRead);
            int fieldInfo = mParcel.readInt();

            int fid = (fieldInfo >> 16) & 0xffff;
            int size = fieldInfo & 0xffff;

            mPositionLookup.put(fid, mParcel.dataPosition());
            mNextRead = mNextRead + size;
            if (fid == fieldId) return mParcel.dataPosition();
        }
        return -1;
    }

    @Override
    public boolean readField(int fieldId) {
        int position = mPositionLookup.get(fieldId, -1);
        if (position == -1) {
            // Try reading ahead in the parcel, because maybe we haven't gotten there yet.
            position = readUntilField(fieldId);
            if (position == -1) {
                return false;
            }
        }
        mParcel.setDataPosition(position);
        return true;
    }

    @Override
    public void setOutputField(int fieldId) {
        closeField();
        mCurrentField = fieldId;
        mPositionLookup.put(fieldId, mParcel.dataPosition());
        writeInt(0); // Placeholder for field id/size
    }

    @Override
    public void closeField() {
        if (mCurrentField >= 0) {
            int currentFieldPosition = mPositionLookup.get(mCurrentField);
            int position = mParcel.dataPosition();
            int size = position - currentFieldPosition;
            mParcel.setDataPosition(currentFieldPosition);
            mParcel.writeInt((mCurrentField << 16) | size);
            mParcel.setDataPosition(position);
        }
    }

    @Override
    protected SafeParcel createSubParcel() {
        return new SafeParcelParcel(mParcel, mParcel.dataPosition());
    }

    @Override
    public void writeByteArray(byte[] b) {
        if (b != null) {
            mParcel.writeInt(b.length);
            mParcel.writeByteArray(b);
        } else {
            mParcel.writeInt(-1);
        }
    }

    @Override
    public void writeByteArray(byte[] b, int offset, int len) {
        if (b != null) {
            mParcel.writeInt(b.length);
            mParcel.writeByteArray(b, offset, len);
        } else {
            mParcel.writeInt(-1);
        }
    }

    @Override
    public void writeInt(int val) {
        mParcel.writeInt(val);
    }

    @Override
    public void writeLong(long val) {
        mParcel.writeLong(val);
    }

    @Override
    public void writeFloat(float val) {
        mParcel.writeFloat(val);
    }

    @Override
    public void writeDouble(double val) {
        mParcel.writeDouble(val);
    }

    @Override
    public void writeString(String val) {
        mParcel.writeString(val);
    }

    @Override
    public void writeStrongBinder(IBinder val) {
        mParcel.writeStrongBinder(val);
    }

    @Override
    public void writeParcelable(Parcelable p) {
        mParcel.writeParcelable(p, 0);
    }

    @Override
    public void writeBoolean(boolean val) {
        mParcel.writeInt(val ? 1 : 0);
    }

    @Override
    public void writeStrongInterface(IInterface val) {
        mParcel.writeStrongInterface(val);
    }

    @Override
    public void writeBundle(Bundle val) {
        mParcel.writeBundle(val);
    }

    @Override
    public int readInt() {
        return mParcel.readInt();
    }

    @Override
    public long readLong() {
        return mParcel.readLong();
    }

    @Override
    public float readFloat() {
        return mParcel.readFloat();
    }

    @Override
    public double readDouble() {
        return mParcel.readDouble();
    }

    @Override
    public String readString() {
        return mParcel.readString();
    }

    @Override
    public IBinder readStrongBinder() {
        return mParcel.readStrongBinder();
    }

    @Override
    public byte[] readByteArray() {
        int len = mParcel.readInt();
        if (len < 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        mParcel.readByteArray(bytes);
        return bytes;
    }

    @Override
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T extends Parcelable> T readParcelable() {
        return mParcel.readParcelable(getClass().getClassLoader());
    }

    @Override
    public Bundle readBundle() {
        return mParcel.readBundle(getClass().getClassLoader());
    }

    @Override
    public boolean readBoolean() {
        return mParcel.readInt() != 0;
    }
}

