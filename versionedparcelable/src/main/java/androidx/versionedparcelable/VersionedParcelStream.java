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
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class VersionedParcelStream extends VersionedParcel {

    private static final Charset UTF_16 = Charset.forName("UTF-16");

    // Supported types held inside a bundle. These cannot be added to or changed once shipped.
    private static final int TYPE_NULL = 0;
    private static final int TYPE_SUB_BUNDLE = 1;
    private static final int TYPE_SUB_PERSISTABLE_BUNDLE = 2;
    private static final int TYPE_STRING = 3;
    private static final int TYPE_STRING_ARRAY = 4;
    private static final int TYPE_BOOLEAN = 5;
    private static final int TYPE_BOOLEAN_ARRAY = 6;
    private static final int TYPE_DOUBLE = 7;
    private static final int TYPE_DOUBLE_ARRAY = 8;
    private static final int TYPE_INT = 9;
    private static final int TYPE_INT_ARRAY = 10;
    private static final int TYPE_LONG = 11;
    private static final int TYPE_LONG_ARRAY = 12;
    private static final int TYPE_FLOAT = 13;
    private static final int TYPE_FLOAT_ARRAY = 14;

    private final DataInputStream mMasterInput;
    private final DataOutputStream mMasterOutput;

    private DataInputStream mCurrentInput;
    private DataOutputStream mCurrentOutput;
    private FieldBuffer mFieldBuffer;
    private boolean mIgnoreParcelables;

    int mCount = 0;
    private int mFieldId = -1;
    int mFieldSize = -1;

    public VersionedParcelStream(InputStream input, OutputStream output) {
        this(input, output, new ArrayMap<String, Method>(), new ArrayMap<String, Method>(),
                new ArrayMap<String, Class<?>>());
    }

    private VersionedParcelStream(InputStream input, OutputStream output,
            ArrayMap<String, Method> readCache,
            ArrayMap<String, Method> writeCache,
            ArrayMap<String, Class<?>> parcelizerCache) {
        super(readCache, writeCache, parcelizerCache);
        mMasterInput = input != null ? new DataInputStream(new FilterInputStream(input) {
            @Override
            public int read() throws IOException {
                if (mFieldSize != -1 && mCount >= mFieldSize) {
                    throw new IOException();
                }
                int read = super.read();
                mCount += 1;
                return read;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (mFieldSize != -1 && mCount >= mFieldSize) {
                    throw new IOException();
                }
                int read = super.read(b, off, len);
                if (read > 0) {
                    mCount += read;
                }
                return read;
            }

            @Override
            public long skip(long n) throws IOException {
                if (mFieldSize != -1 && mCount >= mFieldSize) {
                    throw new IOException();
                }
                long skip = super.skip(n);
                if (skip > 0) {
                    mCount += (int) skip;
                }
                return skip;
            }
        }) : null;
        mMasterOutput = output != null ? new DataOutputStream(output) : null;
        mCurrentInput = mMasterInput;
        mCurrentOutput = mMasterOutput;
    }

    @Override
    public boolean isStream() {
        return true;
    }

    /**
     */
    @Override
    public void setSerializationFlags(boolean allowSerialization, boolean ignoreParcelables) {
        if (!allowSerialization) {
            throw new RuntimeException("Serialization of this object is not allowed");
        }
        mIgnoreParcelables = ignoreParcelables;
    }

    @Override
    public void closeField() {
        if (mFieldBuffer != null) {
            try {
                if (mFieldBuffer.mOutput.size() != 0) {
                    mFieldBuffer.flushField();
                }
            } catch (IOException e) {
                throw new ParcelException(e);
            }
            mFieldBuffer = null;
        }
    }

    @Override
    protected VersionedParcel createSubParcel() {
        return new VersionedParcelStream(mCurrentInput, mCurrentOutput, mReadCache, mWriteCache,
                mParcelizerCache);
    }

    @Override
    public boolean readField(int fieldId) {
        try {
            while (true) {
                if (mFieldId == fieldId) {
                    return true;
                }
                if (String.valueOf(mFieldId).compareTo(String.valueOf(fieldId)) > 0) {
                    return false;
                }
                if (mCount < mFieldSize) {
                    mMasterInput.skip(mFieldSize - mCount);
                }
                mFieldSize = -1;
                int fieldInfo = mMasterInput.readInt();
                mCount = 0;
                int size = fieldInfo & 0xffff;
                if (size == 0xffff) {
                    size = mMasterInput.readInt();
                }
                int id = (fieldInfo >> 16) & 0xffff;
                mFieldId = id;
                mFieldSize = size;
            }
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public void setOutputField(int fieldId) {
        closeField();
        mFieldBuffer = new FieldBuffer(fieldId, mMasterOutput);
        mCurrentOutput = mFieldBuffer.mDataStream;
    }

    @Override
    public void writeByteArray(byte[] b) {
        try {
            if (b != null) {
                mCurrentOutput.writeInt(b.length);
                mCurrentOutput.write(b);
            } else {
                mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public void writeByteArray(byte[] b, int offset, int len) {
        try {
            if (b != null) {
                mCurrentOutput.writeInt(len);
                mCurrentOutput.write(b, offset, len);
            } else {
                mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    protected void writeCharSequence(CharSequence charSequence) {
        if (!mIgnoreParcelables) {
            throw new RuntimeException("CharSequence cannot be written to an OutputStream");
        }
    }

    @Override
    public void writeInt(int val) {
        try {
            mCurrentOutput.writeInt(val);
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public void writeLong(long val) {
        try {
            mCurrentOutput.writeLong(val);
        } catch (IOException e) {
            throw new ParcelException(e);
        }

    }

    @Override
    public void writeFloat(float val) {
        try {
            mCurrentOutput.writeFloat(val);
        } catch (IOException e) {
            throw new ParcelException(e);
        }

    }

    @Override
    public void writeDouble(double val) {
        try {
            mCurrentOutput.writeDouble(val);
        } catch (IOException e) {
            throw new ParcelException(e);
        }

    }

    @Override
    public void writeString(String val) {
        try {
            if (val != null) {
                byte[] bytes = val.getBytes(UTF_16);
                mCurrentOutput.writeInt(bytes.length);
                mCurrentOutput.write(bytes);
            } else {
                mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public void writeBoolean(boolean val) {
        try {
            mCurrentOutput.writeBoolean(val);
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public void writeStrongBinder(IBinder val) {
        if (!mIgnoreParcelables) {
            throw new RuntimeException("Binders cannot be written to an OutputStream");
        }
    }

    @Override
    public void writeParcelable(Parcelable p) {
        if (!mIgnoreParcelables) {
            throw new RuntimeException("Parcelables cannot be written to an OutputStream");
        }
    }

    @Override
    public void writeStrongInterface(IInterface val) {
        if (!mIgnoreParcelables) {
            throw new RuntimeException("Binders cannot be written to an OutputStream");
        }
    }

    @Override
    public IBinder readStrongBinder() {
        return null;
    }

    @Override
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T extends Parcelable> T readParcelable() {
        return null;
    }

    @Override
    public int readInt() {
        try {
            return mCurrentInput.readInt();
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public long readLong() {
        try {
            return mCurrentInput.readLong();
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            return mCurrentInput.readFloat();
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            return mCurrentInput.readDouble();
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public String readString() {
        try {
            int len = mCurrentInput.readInt();
            if (len > 0) {
                byte[] bytes = new byte[len];
                mCurrentInput.readFully(bytes);
                return new String(bytes, UTF_16);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public byte[] readByteArray() {
        try {
            int len = mCurrentInput.readInt();
            if (len > 0) {
                byte[] bytes = new byte[len];
                mCurrentInput.readFully(bytes);
                return bytes;
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    protected CharSequence readCharSequence() {
        return null;
    }

    @Override
    public boolean readBoolean() {
        try {
            return mCurrentInput.readBoolean();
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public void writeBundle(Bundle val) {
        try {
            if (val != null) {
                Set<String> keys = val.keySet();
                mCurrentOutput.writeInt(keys.size());
                for (String key : keys) {
                    writeString(key);
                    Object o = val.get(key);
                    writeObject(o);
                }
            } else {
                mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new ParcelException(e);
        }
    }

    @Override
    public Bundle readBundle() {
        int size = readInt();
        if (size < 0) {
            return null;
        }
        Bundle b = new Bundle();
        for (int i = 0; i < size; i++) {
            String key = readString();
            readObject(readInt(), key, b);
        }
        return b;
    }

    private void writeObject(Object o) {
        if (o == null) {
            writeInt(TYPE_NULL);
        } else if (o instanceof Bundle) {
            writeInt(TYPE_SUB_BUNDLE);
            writeBundle((Bundle) o);
        } else if (o instanceof String) {
            writeInt(TYPE_STRING);
            writeString((String) o);
        } else if (o instanceof String[]) {
            writeInt(TYPE_STRING_ARRAY);
            writeArray((String[]) o);
        } else if (o instanceof Boolean) {
            writeInt(TYPE_BOOLEAN);
            writeBoolean((Boolean) o);
        } else if (o instanceof boolean[]) {
            writeInt(TYPE_BOOLEAN_ARRAY);
            writeBooleanArray((boolean[]) o);
        } else if (o instanceof Double) {
            writeInt(TYPE_DOUBLE);
            writeDouble((Double) o);
        } else if (o instanceof double[]) {
            writeInt(TYPE_DOUBLE_ARRAY);
            writeDoubleArray((double[]) o);
        } else if (o instanceof Integer) {
            writeInt(TYPE_INT);
            writeInt((Integer) o);
        } else if (o instanceof int[]) {
            writeInt(TYPE_INT_ARRAY);
            writeIntArray((int[]) o);
        } else if (o instanceof Long) {
            writeInt(TYPE_LONG);
            writeLong((Long) o);
        } else if (o instanceof long[]) {
            writeInt(TYPE_LONG_ARRAY);
            writeLongArray((long[]) o);
        } else if (o instanceof Float) {
            writeInt(TYPE_FLOAT);
            writeFloat((Float) o);
        } else if (o instanceof float[]) {
            writeInt(TYPE_FLOAT_ARRAY);
            writeFloatArray((float[]) o);
        } else {
            throw new IllegalArgumentException("Unsupported type " + o.getClass());
        }
    }

    private void readObject(int type, String key, Bundle b) {
        switch (type) {
            case TYPE_NULL:
                b.putParcelable(key, null);
                break;
            case TYPE_SUB_BUNDLE:
                b.putBundle(key, readBundle());
                break;
            case TYPE_SUB_PERSISTABLE_BUNDLE:
                b.putBundle(key, readBundle());
                break;
            case TYPE_STRING:
                b.putString(key, readString());
                break;
            case TYPE_STRING_ARRAY:
                b.putStringArray(key, readArray(new String[0]));
                break;
            case TYPE_BOOLEAN:
                b.putBoolean(key, readBoolean());
                break;
            case TYPE_BOOLEAN_ARRAY:
                b.putBooleanArray(key, readBooleanArray());
                break;
            case TYPE_DOUBLE:
                b.putDouble(key, readDouble());
                break;
            case TYPE_DOUBLE_ARRAY:
                b.putDoubleArray(key, readDoubleArray());
                break;
            case TYPE_INT:
                b.putInt(key, readInt());
                break;
            case TYPE_INT_ARRAY:
                b.putIntArray(key, readIntArray());
                break;
            case TYPE_LONG:
                b.putLong(key, readLong());
                break;
            case TYPE_LONG_ARRAY:
                b.putLongArray(key, readLongArray());
                break;
            case TYPE_FLOAT:
                b.putFloat(key, readFloat());
                break;
            case TYPE_FLOAT_ARRAY:
                b.putFloatArray(key, readFloatArray());
                break;
            default:
                throw new RuntimeException("Unknown type " + type);
        }
    }

    // This uses extra buffers at the moment, but makes the code really clean.
    // TODO: Use less buffers
    private static class FieldBuffer {

        final ByteArrayOutputStream mOutput = new ByteArrayOutputStream();
        final DataOutputStream mDataStream = new DataOutputStream(mOutput);
        private final int mFieldId;
        private final DataOutputStream mTarget;

        FieldBuffer(int fieldId, DataOutputStream target) {
            mFieldId = fieldId;
            mTarget = target;
        }

        void flushField() throws IOException {
            mDataStream.flush();
            int size = mOutput.size();
            int fieldInfo = (mFieldId << 16) | (size >= 0xffff ? 0xffff : size);
            mTarget.writeInt(fieldInfo);
            if (size >= 0xffff) {
                mTarget.writeInt(size);
            }
            mOutput.writeTo(mTarget);
        }
    }

}
