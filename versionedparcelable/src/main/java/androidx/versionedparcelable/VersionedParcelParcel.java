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

package androidx.versionedparcelable;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import java.lang.reflect.Method;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class VersionedParcelParcel extends VersionedParcel {
    private static final boolean DEBUG = false;
    private static final String TAG = "VersionedParcelParcel";

    private final SparseIntArray mPositionLookup = new SparseIntArray();
    private final Parcel mParcel;
    private final int mOffset;
    private final int mEnd;
    private final String mPrefix;
    private int mCurrentField = -1;
    private int mNextRead = 0;
    private int mFieldId = -1;

    VersionedParcelParcel(Parcel p) {
        this(p, p.dataPosition(), p.dataSize(), "", new ArrayMap<String, Method>(),
                new ArrayMap<String, Method>(),
                new ArrayMap<String, Class<?>>());
    }

    private VersionedParcelParcel(Parcel p, int offset, int end, String prefix,
            ArrayMap<String, Method> readCache,
            ArrayMap<String, Method> writeCache,
            ArrayMap<String, Class<?>> parcelizerCache) {
        super(readCache, writeCache, parcelizerCache);
        mParcel = p;
        mOffset = offset;
        mEnd = end;
        mNextRead = mOffset;
        mPrefix = prefix;
    }

    @Override
    public boolean readField(int fieldId) {
        while (mNextRead < mEnd) {
            if (mFieldId == fieldId) {
                return true;
            }
            if (String.valueOf(mFieldId).compareTo(String.valueOf(fieldId)) > 0) {
                return false;
            }
            mParcel.setDataPosition(mNextRead);
            int size = mParcel.readInt();
            mFieldId = mParcel.readInt();
            if (DEBUG) Log.d(TAG, mPrefix + "Found field " + fieldId + " : " + size);

            mNextRead = mNextRead + size;
        }
        return mFieldId == fieldId;
    }

    @Override
    public void setOutputField(int fieldId) {
        closeField();
        mCurrentField = fieldId;
        mPositionLookup.put(fieldId, mParcel.dataPosition());
        if (DEBUG) Log.d(TAG, mPrefix + "Starting " + fieldId + " : " + mParcel.dataPosition());
        writeInt(0); // Placeholder for field size
        writeInt(fieldId);
    }

    @Override
    public void closeField() {
        if (mCurrentField >= 0) {
            int currentFieldPosition = mPositionLookup.get(mCurrentField);
            int position = mParcel.dataPosition();
            int size = position - currentFieldPosition;
            if (DEBUG) {
                Log.d(TAG, mPrefix + "Closing " + mCurrentField + " : "
                        + mParcel.dataPosition() + " " + size);
            }
            mParcel.setDataPosition(currentFieldPosition);
            mParcel.writeInt(size);
            mParcel.setDataPosition(position);
        }
    }

    @Override
    protected VersionedParcel createSubParcel() {
        if (DEBUG) {
            Log.d(TAG, mPrefix + "Creating subparcel " + mCurrentField + " : "
                    + mParcel.dataPosition() + " - " + (mNextRead == mOffset ? mEnd : mNextRead));
        }
        return new VersionedParcelParcel(mParcel, mParcel.dataPosition(),
                mNextRead == mOffset ? mEnd : mNextRead, mPrefix + "  ", mReadCache, mWriteCache,
                mParcelizerCache);
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
    protected void writeCharSequence(CharSequence charSequence) {
        TextUtils.writeToParcel(charSequence, mParcel, 0);
    }

    @Override
    protected CharSequence readCharSequence() {
        return TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(mParcel);
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

