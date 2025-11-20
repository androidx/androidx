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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.NetworkOnMainThreadException;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseBooleanArray;

import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.collection.SimpleArrayMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
@RestrictTo(LIBRARY_GROUP_PREFIX)
public abstract class VersionedParcel {

    private static final @NonNull String TAG = "VersionedParcel";

    // These constants cannot change once shipped.
    private static final int EX_SECURITY = -1;
    private static final int EX_BAD_PARCELABLE = -2;
    private static final int EX_ILLEGAL_ARGUMENT = -3;
    private static final int EX_NULL_POINTER = -4;
    private static final int EX_ILLEGAL_STATE = -5;
    private static final int EX_NETWORK_MAIN_THREAD = -6;
    private static final int EX_UNSUPPORTED_OPERATION = -7;
    private static final int EX_PARCELABLE = -9;

    private static final int TYPE_VERSIONED_PARCELABLE = 1;
    private static final int TYPE_PARCELABLE = 2;
    private static final int TYPE_SERIALIZABLE = 3;
    private static final int TYPE_STRING = 4;
    private static final int TYPE_BINDER = 5;
    private static final int TYPE_INTEGER = 7;
    private static final int TYPE_FLOAT = 8;

    final @NonNull SimpleArrayMap<String, Method> mReadCache;
    final @NonNull SimpleArrayMap<String, Method> mWriteCache;
    final @NonNull SimpleArrayMap<String, Class<?>> mParcelizerCache;

    VersionedParcel(
            @NonNull SimpleArrayMap<String, Method> readCache,
            @NonNull SimpleArrayMap<String, Method> writeCache,
            @NonNull SimpleArrayMap<String, Class<?>> parcelizerCache
    ) {
        mReadCache = readCache;
        mWriteCache = writeCache;
        mParcelizerCache = parcelizerCache;
    }

    /**
     * Whether this VersionedParcel is serializing into a stream and will not accept Parcelables.
     */
    public boolean isStream() {
        return false;
    }

    /**
     * Closes the last field when done parceling.
     */
    protected abstract void closeField();

    /**
     * Create a sub-parcel to be used for a child VersionedParcelable
     */
    protected abstract @NonNull VersionedParcel createSubParcel();

    /**
     * Write a byte array into the parcel.
     *
     * @param b Bytes to place into the parcel.
     */
    protected abstract void writeByteArray(byte @Nullable [] b);

    /**
     * Write a byte array into the parcel.
     *
     * @param b      Bytes to place into the parcel.
     * @param offset Index of first byte to be written.
     * @param len    Number of bytes to write.
     */
    protected abstract void writeByteArray(byte @Nullable [] b, int offset, int len);

    /**
     * Write a CharSequence into the parcel.
     */
    protected abstract void writeCharSequence(@Nullable CharSequence charSequence);

    /**
     * Write an integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeInt(int val);

    /**
     * Write a long integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeLong(long val);

    /**
     * Write a floating point value into the parcel at the current
     * dataPosition(), growing dataCapacity() if needed.
     */
    protected abstract void writeFloat(float val);

    /**
     * Write a double precision floating point value into the parcel at the
     * current dataPosition(), growing dataCapacity() if needed.
     */
    protected abstract void writeDouble(double val);

    /**
     * Write a string value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeString(@Nullable String val);

    /**
     * Write an object into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeStrongBinder(@Nullable IBinder val);

    /**
     * Flatten the name of the class of the VersionedParcelable and its contents
     * into the parcel.
     *
     * @param p The VersionedParcelable object to be written.
     *          {@link Parcelable#writeToParcel(Parcel, int) Parcelable.writeToParcel()}.
     */
    protected abstract void writeParcelable(@Nullable Parcelable p);

    /**
     * Write a boolean value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeBoolean(boolean val);

    /**
     * Write an object into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeStrongInterface(@Nullable IInterface val);

    /**
     * Flatten a Bundle into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    protected abstract void writeBundle(@Nullable Bundle val);

    /**
     * Read an integer value from the parcel at the current dataPosition().
     */
    protected abstract int readInt();

    /**
     * Read a long integer value from the parcel at the current dataPosition().
     */
    protected abstract long readLong();

    /**
     * Read a floating point value from the parcel at the current
     * dataPosition().
     */
    protected abstract float readFloat();

    /**
     * Read a double precision floating point value from the parcel at the
     * current dataPosition().
     */
    protected abstract double readDouble();

    /**
     * Read a string value from the parcel at the current dataPosition().
     */
    protected abstract @Nullable String readString();

    /**
     * Read an object from the parcel at the current dataPosition().
     */
    protected abstract @Nullable IBinder readStrongBinder();

    /**
     * Read a byte[] object from the parcel.
     */
    protected abstract byte @Nullable [] readByteArray();

    /**
     * Read a CharSequence from the parcel
     */
    protected abstract @Nullable CharSequence readCharSequence();

    /**
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    protected abstract <T extends Parcelable> @Nullable T readParcelable();

    /**
     * Read and return a new Bundle object from the parcel at the current
     * dataPosition().  Returns null if the previously written Bundle object was
     * null.
     */
    protected abstract @Nullable Bundle readBundle();

    /**
     * Read a boolean value from the parcel at the current dataPosition().
     */
    protected abstract boolean readBoolean();

    /**
     * Prepares to read data from a specific field for the following read
     * calls.
     */
    protected abstract boolean readField(int fieldId);

    /**
     * Sets the output of write methods to be tagged as part of the specified
     * fieldId.
     */
    protected abstract void setOutputField(int fieldId);

    /**
     * Configure the VersionedParcel for current serialization method.
     */
    public void setSerializationFlags(boolean allowSerialization, boolean ignoreParcelables) {
        // Don't care except in VersionedParcelStream
    }

    /**
     * Write an object into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeStrongInterface(@Nullable IInterface val, int fieldId) {
        setOutputField(fieldId);
        writeStrongInterface(val);
    }

    /**
     * Flatten a Bundle into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeBundle(@Nullable Bundle val, int fieldId) {
        setOutputField(fieldId);
        writeBundle(val);
    }

    /**
     * Write a boolean value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeBoolean(boolean val, int fieldId) {
        setOutputField(fieldId);
        writeBoolean(val);
    }

    /**
     * Write a byte array into the parcel.
     *
     * @param b Bytes to place into the parcel.
     */
    public void writeByteArray(byte @Nullable [] b, int fieldId) {
        setOutputField(fieldId);
        writeByteArray(b);
    }

    /**
     * Write a byte array into the parcel.
     *
     * @param b      Bytes to place into the parcel.
     * @param offset Index of first byte to be written.
     * @param len    Number of bytes to write.
     */
    public void writeByteArray(byte @Nullable [] b, int offset, int len, int fieldId) {
        setOutputField(fieldId);
        writeByteArray(b, offset, len);
    }

    /**
     * Write a CharSequence into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeCharSequence(@Nullable CharSequence val, int fieldId) {
        setOutputField(fieldId);
        writeCharSequence(val);
    }

    /**
     * Write an integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeInt(int val, int fieldId) {
        setOutputField(fieldId);
        writeInt(val);
    }

    /**
     * Write a long integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeLong(long val, int fieldId) {
        setOutputField(fieldId);
        writeLong(val);
    }

    /**
     * Write a floating point value into the parcel at the current
     * dataPosition(), growing dataCapacity() if needed.
     */
    public void writeFloat(float val, int fieldId) {
        setOutputField(fieldId);
        writeFloat(val);
    }

    /**
     * Write a double precision floating point value into the parcel at the
     * current dataPosition(), growing dataCapacity() if needed.
     */
    public void writeDouble(double val, int fieldId) {
        setOutputField(fieldId);
        writeDouble(val);
    }

    /**
     * Write a string value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeString(@Nullable String val, int fieldId) {
        setOutputField(fieldId);
        writeString(val);
    }

    /**
     * Write an object into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeStrongBinder(@Nullable IBinder val, int fieldId) {
        setOutputField(fieldId);
        writeStrongBinder(val);
    }

    /**
     * Flatten the name of the class of the Parcelable and its contents
     * into the parcel.
     *
     * @param p The Parcelable object to be written.
     *          {@link Parcelable#writeToParcel(Parcel, int) Parcelable.writeToParcel()}.
     */
    public void writeParcelable(@Nullable Parcelable p, int fieldId) {
        setOutputField(fieldId);
        writeParcelable(p);
    }

    /**
     * Read a boolean value from the parcel at the current dataPosition().
     */
    public boolean readBoolean(boolean def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readBoolean();
    }

    /**
     * Read an integer value from the parcel at the current dataPosition().
     */
    public int readInt(int def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readInt();
    }

    /**
     * Read a long integer value from the parcel at the current dataPosition().
     */
    public long readLong(long def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readLong();
    }

    /**
     * Read a floating point value from the parcel at the current
     * dataPosition().
     */
    public float readFloat(float def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readFloat();
    }

    /**
     * Read a double precision floating point value from the parcel at the
     * current dataPosition().
     */
    public double readDouble(double def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readDouble();
    }

    /**
     * Read a string value from the parcel at the current dataPosition().
     */
    public @Nullable String readString(@Nullable String def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readString();
    }

    /**
     * Read an object from the parcel at the current dataPosition().
     */
    public @Nullable IBinder readStrongBinder(@Nullable IBinder def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readStrongBinder();
    }

    /**
     * Read a byte[] object from the parcel and copy it into the
     * given byte array.
     */
    public byte @Nullable [] readByteArray(byte @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readByteArray();
    }

    /**
     */
    public <T extends Parcelable> @Nullable T readParcelable(@Nullable T def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readParcelable();
    }

    /**
     * Read and return a new Bundle object from the parcel at the current
     * dataPosition().  Returns null if the previously written Bundle object was
     * null.
     */
    public @Nullable Bundle readBundle(@Nullable Bundle def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readBundle();
    }

    /**
     * Write a byte value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeByte(byte val, int fieldId) {
        setOutputField(fieldId);
        writeInt(val);
    }

    /**
     * Flatten a Size into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeSize(@Nullable Size val, int fieldId) {
        setOutputField(fieldId);
        Api21Impl.writeSize(this, val);
    }

    /**
     * Flatten a SizeF into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public void writeSizeF(@Nullable SizeF val, int fieldId) {
        setOutputField(fieldId);
        Api21Impl.writeSizeF(this, val);
    }

    /**
     */
    public void writeSparseBooleanArray(@Nullable SparseBooleanArray val, int fieldId) {
        setOutputField(fieldId);
        if (val == null) {
            writeInt(-1);
            return;
        }
        int n = val.size();
        writeInt(n);
        int i = 0;
        while (i < n) {
            writeInt(val.keyAt(i));
            writeBoolean(val.valueAt(i));
            i++;
        }
    }

    /**
     */
    public void writeBooleanArray(boolean @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeBooleanArray(val);
    }

    /**
     */
    protected void writeBooleanArray(boolean @Nullable [] val) {
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeInt(val[i] ? 1 : 0);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public boolean @Nullable [] readBooleanArray(boolean @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readBooleanArray();
    }

    /**
     */
    protected boolean @Nullable [] readBooleanArray() {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        boolean[] val = new boolean[n];
        for (int i = 0; i < n; i++) {
            val[i] = readInt() != 0;
        }
        return val;
    }

    /**
     */
    public void writeCharArray(char @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeInt((int) val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public @Nullable CharSequence readCharSequence(@Nullable CharSequence def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readCharSequence();
    }

    /**
     */
    public char @Nullable [] readCharArray(char @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        int n = readInt();
        if (n < 0) {
            return null;
        }
        char[] val = new char[n];
        for (int i = 0; i < n; i++) {
            val[i] = (char) readInt();
        }
        return val;
    }

    /**
     */
    public void writeIntArray(int @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeIntArray(val);
    }

    /**
     */
    protected void writeIntArray(int @Nullable [] val) {
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeInt(val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public int @Nullable [] readIntArray(int @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readIntArray();
    }

    /**
     */
    protected int @Nullable [] readIntArray() {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        int[] val = new int[n];
        for (int i = 0; i < n; i++) {
            val[i] = readInt();
        }
        return val;
    }

    /**
     */
    public void writeLongArray(long @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeLongArray(val);
    }

    /**
     */
    protected void writeLongArray(long @Nullable [] val) {
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeLong(val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public long @Nullable [] readLongArray(long @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readLongArray();
    }

    /**
     */
    protected long @Nullable [] readLongArray() {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        long[] val = new long[n];
        for (int i = 0; i < n; i++) {
            val[i] = readLong();
        }
        return val;
    }

    /**
     */
    public void writeFloatArray(float @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeFloatArray(val);
    }

    /**
     */
    protected void writeFloatArray(float @Nullable [] val) {
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeFloat(val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public float @Nullable [] readFloatArray(float @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readFloatArray();
    }

    /**
     */
    protected float @Nullable [] readFloatArray() {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        float[] val = new float[n];
        for (int i = 0; i < n; i++) {
            val[i] = readFloat();
        }
        return val;
    }

    /**
     */
    public void writeDoubleArray(double @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeDoubleArray(val);
    }

    /**
     */
    protected void writeDoubleArray(double @Nullable [] val) {
        if (val != null) {
            int n = val.length;
            writeInt(n);
            for (int i = 0; i < n; i++) {
                writeDouble(val[i]);
            }
        } else {
            writeInt(-1);
        }
    }

    /**
     */
    public double @Nullable [] readDoubleArray(double @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readDoubleArray();
    }

    /**
     */
    protected double @Nullable [] readDoubleArray() {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        double[] val = new double[n];
        for (int i = 0; i < n; i++) {
            val[i] = readDouble();
        }
        return val;
    }

    /**
     * Flatten a Set containing a particular object type into the parcel, at
     * the current dataPosition() and growing dataCapacity() if needed.  The
     * type of the objects in the list must be one that implements VersionedParcelable,
     * Parcelable, String, or Serializable.
     *
     * @param val The list of objects to be written.
     * @see #readSet
     * @see VersionedParcelable
     */
    public <T> void writeSet(@Nullable Set<T> val, int fieldId) {
        writeCollection(val, fieldId);
    }

    /**
     * Flatten a List containing a particular object type into the parcel, at
     * the current dataPosition() and growing dataCapacity() if needed.  The
     * type of the objects in the list must be one that implements VersionedParcelable,
     * Parcelable, String, Float, Integer or Serializable.
     *
     * @param val The list of objects to be written.
     * @see #readList
     * @see VersionedParcelable
     */
    public <T> void writeList(@Nullable List<T> val, int fieldId) {
        writeCollection(val, fieldId);
    }

    /**
     * Flatten a Map containing a particular object type into the parcel, at
     * the current dataPosition() and growing dataCapacity() if needed.  The
     * type of the objects in the list must be one that implements VersionedParcelable,
     * Parcelable, String, Float, Integer or Serializable.
     *
     * @param val The list of objects to be written.
     * @see #readMap
     * @see VersionedParcelable
     */
    public <K, V> void writeMap(@Nullable Map<K, V> val, int fieldId) {
        setOutputField(fieldId);
        if (val == null) {
            writeInt(-1);
            return;
        }
        int size = val.size();
        writeInt(size);
        if (size == 0) {
            return;
        }
        List<K> keySet = new ArrayList<>(size);
        List<V> valueSet = new ArrayList<>(size);
        for (Map.Entry<K, V> entry : val.entrySet()) {
            keySet.add(entry.getKey());
            valueSet.add(entry.getValue());
        }
        writeCollection(keySet);
        writeCollection(valueSet);
    }

    private <T> void writeCollection(@Nullable Collection<T> val, int fieldId) {
        setOutputField(fieldId);
        writeCollection(val);
    }

    private <T> void writeCollection(@Nullable Collection<T> val) {
        if (val == null) {
            writeInt(-1);
            return;
        }

        int n = val.size();
        writeInt(n);
        if (n > 0) {
            int type = getType(val.iterator().next());
            writeInt(type);
            switch (type) {
                case TYPE_STRING:
                    for (T v : val) {
                        writeString((String) v);
                    }
                    break;
                case TYPE_PARCELABLE:
                    for (T v : val) {
                        writeParcelable((Parcelable) v);
                    }
                    break;
                case TYPE_VERSIONED_PARCELABLE:
                    for (T v : val) {
                        writeVersionedParcelable((VersionedParcelable) v);
                    }
                    break;
                case TYPE_SERIALIZABLE:
                    for (T v : val) {
                        writeSerializable((Serializable) v);
                    }
                    break;
                case TYPE_BINDER:
                    for (T v : val) {
                        writeStrongBinder((IBinder) v);
                    }
                    break;
                case TYPE_INTEGER:
                    for (T v : val) {
                        writeInt((Integer) v);
                    }
                    break;
                case TYPE_FLOAT:
                    for (T v : val) {
                        writeFloat((Float) v);
                    }
                    break;
            }
        }
    }

    /**
     * Flatten an Array containing a particular object type into the parcel, at
     * the current dataPosition() and growing dataCapacity() if needed.  The
     * type of the objects in the array must be one that implements VersionedParcelable,
     * Parcelable, String, or Serializable.
     *
     * @param val The list of objects to be written.
     * @see #readList
     * @see VersionedParcelable
     */
    public <T> void writeArray(T @Nullable [] val, int fieldId) {
        setOutputField(fieldId);
        writeArray(val);
    }

    /**
     */
    protected <T> void writeArray(T @Nullable [] val) {
        if (val == null) {
            writeInt(-1);
            return;
        }

        int n = val.length;
        int i = 0;
        writeInt(n);
        if (n > 0) {
            int type = getType(val[0]);
            writeInt(type);
            switch (type) {
                case TYPE_STRING:
                    while (i < n) {
                        writeString((String) val[i]);
                        i++;
                    }
                    break;
                case TYPE_PARCELABLE:
                    while (i < n) {
                        writeParcelable((Parcelable) val[i]);
                        i++;
                    }
                    break;
                case TYPE_VERSIONED_PARCELABLE:
                    while (i < n) {
                        writeVersionedParcelable((VersionedParcelable) val[i]);
                        i++;
                    }
                    break;
                case TYPE_SERIALIZABLE:
                    while (i < n) {
                        writeSerializable((Serializable) val[i]);
                        i++;
                    }
                    break;
                case TYPE_BINDER:
                    while (i < n) {
                        writeStrongBinder((IBinder) val[i]);
                        i++;
                    }
                    break;
            }
        }
    }

    private <T> int getType(@NonNull T t) {
        if (t instanceof String) {
            return TYPE_STRING;
        } else if (t instanceof Parcelable) {
            return TYPE_PARCELABLE;
        } else if (t instanceof VersionedParcelable) {
            return TYPE_VERSIONED_PARCELABLE;
        } else if (t instanceof Serializable) {
            return TYPE_SERIALIZABLE;
        } else if (t instanceof IBinder) {
            return TYPE_BINDER;
        } else if (t instanceof Integer) {
            return TYPE_INTEGER;
        } else if (t instanceof Float) {
            return TYPE_FLOAT;
        }
        throw new IllegalArgumentException(t.getClass().getName()
                + " cannot be VersionedParcelled");
    }

    /**
     * Flatten the name of the class of the VersionedParcelable and its contents
     * into the parcel.
     *
     * @param p The VersionedParcelable object to be written.
     */
    public void writeVersionedParcelable(@Nullable VersionedParcelable p, int fieldId) {
        setOutputField(fieldId);
        writeVersionedParcelable(p);
    }

    /**
     */
    protected void writeVersionedParcelable(@Nullable VersionedParcelable p) {
        if (p == null) {
            writeString(null);
            return;
        }
        writeVersionedParcelableCreator(p);

        VersionedParcel subParcel = createSubParcel();
        writeToParcel(p, subParcel);
        subParcel.closeField();
    }

    private void writeVersionedParcelableCreator(@NonNull VersionedParcelable p) {
        Class<?> name;
        try {
            name = findParcelClass(p.getClass());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(p.getClass().getSimpleName() + " does not have a Parcelizer",
                    e);
        }
        writeString(name.getName());
    }

    /**
     * Write a generic serializable object in to a VersionedParcel.  It is strongly
     * recommended that this method be avoided, since the serialization
     * overhead is extremely large, and this approach will be much slower than
     * using the other approaches to writing data in to a VersionedParcel.
     */
    public void writeSerializable(@Nullable Serializable s, int fieldId) {
        setOutputField(fieldId);
        writeSerializable(s);
    }

    private void writeSerializable(@Nullable Serializable s) {
        if (s == null) {
            writeString(null);
            return;
        }
        String name = s.getClass().getName();
        writeString(name);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(s);
            oos.close();

            writeByteArray(baos.toByteArray());
        } catch (IOException ioe) {
            throw new RuntimeException("VersionedParcelable encountered "
                    + "IOException writing serializable object (name = " + name
                    + ")", ioe);
        }
    }

    /**
     * Special function for writing an exception result at the header of
     * a parcel, to be used when returning an exception from a transaction.
     * Note that this currently only supports a few exception types; any other
     * exception will be re-thrown by this function as a RuntimeException
     * (to be caught by the system's last-resort exception handling when
     * dispatching a transaction).
     *
     * <p>The supported exception types are:
     * <ul>
     * <li>{@link BadParcelableException}
     * <li>{@link IllegalArgumentException}
     * <li>{@link IllegalStateException}
     * <li>{@link NullPointerException}
     * <li>{@link SecurityException}
     * <li>{@link UnsupportedOperationException}
     * <li>{@link NetworkOnMainThreadException}
     * </ul>
     *
     * @param e The Exception to be written.
     * @see #writeNoException
     * @see #readException
     */
    public void writeException(@Nullable Exception e, int fieldId) {
        setOutputField(fieldId);
        if (e == null) {
            writeNoException();
            return;
        }
        int code = 0;
        if (e instanceof Parcelable
                && (e.getClass().getClassLoader() == Parcelable.class.getClassLoader())) {
            // We only send VersionedParcelable exceptions that are in the
            // BootClassLoader to ensure that the receiver can unpack them
            code = EX_PARCELABLE;
        } else if (e instanceof SecurityException) {
            code = EX_SECURITY;
        } else if (e instanceof BadParcelableException) {
            code = EX_BAD_PARCELABLE;
        } else if (e instanceof IllegalArgumentException) {
            code = EX_ILLEGAL_ARGUMENT;
        } else if (e instanceof NullPointerException) {
            code = EX_NULL_POINTER;
        } else if (e instanceof IllegalStateException) {
            code = EX_ILLEGAL_STATE;
        } else if (e instanceof NetworkOnMainThreadException) {
            code = EX_NETWORK_MAIN_THREAD;
        } else if (e instanceof UnsupportedOperationException) {
            code = EX_UNSUPPORTED_OPERATION;
        }
        writeInt(code);
        if (code == 0) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        writeString(e.getMessage());
        switch (code) {
            case EX_PARCELABLE:
                // Write parceled exception prefixed by length
                writeParcelable((Parcelable) e);
                break;
        }
    }

    /**
     * Special function for writing information at the front of the VersionedParcel
     * indicating that no exception occurred.
     *
     * @see #writeException
     * @see #readException
     */
    protected void writeNoException() {
        writeInt(0);
    }

    /**
     * Special function for reading an exception result from the header of
     * a parcel, to be used after receiving the result of a transaction.  This
     * will throw the exception for you if it had been written to the VersionedParcel,
     * otherwise return and let you read the normal result data from the VersionedParcel.
     *
     * @see #writeException
     * @see #writeNoException
     */
    public @Nullable Exception readException(@Nullable Exception def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        int code = readExceptionCode();
        if (code != 0) {
            String msg = readString();
            return createException(code, msg);
        }
        return def;
    }

    /**
     * Parses the header of a Binder call's response VersionedParcel and
     * returns the exception code.  Deals with lite or fat headers.
     * In the common successful case, this header is generally zero.
     * In less common cases, it's a small negative number and will be
     * followed by an error string.
     *
     * This exists purely for android.database.DatabaseUtils and
     * insulating it from having to handle fat headers as returned by
     * e.g. StrictMode-induced RPC responses.
     */
    private int readExceptionCode() {
        int code = readInt();
        return code;
    }

    /**
     * Gets the root {@link Throwable#getCause() cause} of {@code t}
     */
    protected static @NonNull Throwable getRootCause(@NonNull Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }

    /**
     * Creates an exception with the given message.
     *
     * @param code Used to determine which exception class to throw.
     * @param msg  The exception message.
     */
    private @NonNull Exception createException(int code, String msg) {
        switch (code) {
            case EX_PARCELABLE:
                return (Exception) readParcelable();
            case EX_SECURITY:
                return new SecurityException(msg);
            case EX_BAD_PARCELABLE:
                return new BadParcelableException(msg);
            case EX_ILLEGAL_ARGUMENT:
                return new IllegalArgumentException(msg);
            case EX_NULL_POINTER:
                return new NullPointerException(msg);
            case EX_ILLEGAL_STATE:
                return new IllegalStateException(msg);
            case EX_NETWORK_MAIN_THREAD:
                return new NetworkOnMainThreadException();
            case EX_UNSUPPORTED_OPERATION:
                return new UnsupportedOperationException(msg);
        }
        return new RuntimeException("Unknown exception code: " + code
                + " msg " + msg);
    }

    /**
     * Read a byte value from the parcel at the current dataPosition().
     */
    public byte readByte(byte def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return (byte) (readInt() & 0xff);
    }

    /**
     * Read a Size from the parcel at the current dataPosition().
     */
    public @Nullable Size readSize(@Nullable Size def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return Api21Impl.readSize(this);
    }

    /**
     * Read a SizeF from the parcel at the current dataPosition().
     */
    public @Nullable SizeF readSizeF(@Nullable SizeF def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return Api21Impl.readSizeF(this);
    }

    /**
     * Read and return a new SparseBooleanArray object from the parcel at the current
     * dataPosition().  Returns null if the previously written list object was
     * null.
     */
    public @Nullable SparseBooleanArray readSparseBooleanArray(
            @Nullable SparseBooleanArray def,
            int fieldId
    ) {
        if (!readField(fieldId)) {
            return def;
        }
        int n = readInt();
        if (n < 0) {
            return null;
        }
        SparseBooleanArray sa = new SparseBooleanArray(n);
        int i = 0;
        while (i < n) {
            sa.put(readInt(), readBoolean());
            i++;
        }
        return sa;
    }

    /**
     * Read and return a new ArraySet containing a particular object type from
     * the parcel that was written with {@link #writeSet} at the
     * current dataPosition().  Returns null if the
     * previously written list object was null.  The list <em>must</em> have
     * previously been written via {@link #writeSet} with the same object
     * type.
     *
     * @return A newly created ArraySet containing objects with the same data
     * as those that were previously written.
     * @see #writeSet
     */
    public <T> @Nullable Set<T> readSet(@Nullable Set<T> def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readCollection(new ArraySet<T>());
    }

    /**
     * Read and return a new ArrayList containing a particular object type from
     * the parcel that was written with {@link #writeList} at the
     * current dataPosition().  Returns null if the
     * previously written list object was null.  The list <em>must</em> have
     * previously been written via {@link #writeList} with the same object
     * type.
     *
     * @return A newly created ArrayList containing objects with the same data
     * as those that were previously written.
     * @see #writeList
     */
    public <T> @Nullable List<T> readList(@Nullable List<T> def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readCollection(new ArrayList<T>());
    }

    @SuppressWarnings("unchecked")
    private <T, S extends Collection<T>> @Nullable S readCollection(@NonNull S list) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        if (n != 0) {
            int type = readInt();
            if (n < 0) {
                return null;
            }
            switch (type) {
                case TYPE_STRING:
                    while (n > 0) {
                        list.add((T) readString());
                        n--;
                    }
                    break;
                case TYPE_PARCELABLE:
                    while (n > 0) {
                        list.add((T) readParcelable());
                        n--;
                    }
                    break;
                case TYPE_VERSIONED_PARCELABLE:
                    while (n > 0) {
                        list.add((T) readVersionedParcelable());
                        n--;
                    }
                    break;
                case TYPE_SERIALIZABLE:
                    while (n > 0) {
                        list.add((T) readSerializable());
                        n--;
                    }
                    break;
                case TYPE_BINDER:
                    while (n > 0) {
                        list.add((T) readStrongBinder());
                        n--;
                    }
                    break;
            }
        }
        return list;
    }

    /**
     * Read and return a new ArrayMap containing a particular object type from
     * the parcel that was written with {@link #writeMap} at the
     * current dataPosition().  Returns null if the
     * previously written list object was null.  The list <em>must</em> have
     * previously been written via {@link #writeMap} with the same object type.
     *
     * @return A newly created ArrayMap containing objects with the same data
     * as those that were previously written.
     * @see #writeMap
     */
    public <K, V> @Nullable Map<K, V> readMap(@Nullable Map<K, V> def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        int size = readInt();
        if (size < 0) {
            return null;
        }
        Map<K, V> map = new ArrayMap<>(size);
        if (size == 0) {
            return map;
        }
        List<K> keyList = new ArrayList<>(size);
        List<V> valueList = new ArrayList<>(size);
        readCollection(keyList);
        readCollection(valueList);
        for (int i = 0; i < size; i++) {
            map.put(keyList.get(i), valueList.get(i));
        }
        return map;
    }

    /**
     * Read and return a new ArrayList containing a particular object type from
     * the parcel that was written with {@link #writeArray} at the
     * current dataPosition().  Returns null if the
     * previously written list object was null.  The list <em>must</em> have
     * previously been written via {@link #writeArray} with the same object
     * type.
     *
     * @return A newly created ArrayList containing objects with the same data
     * as those that were previously written.
     * @see #writeArray
     */
    public <T> T @Nullable [] readArray(T @Nullable [] def, int fieldId) {
        if (!readField(fieldId)) {
            return def;
        }
        return readArray(def);
    }

    /**
     */
    @SuppressWarnings("unchecked")
    protected <T> T @Nullable [] readArray(T @Nullable [] def) {
        int n = readInt();
        if (n < 0) {
            return null;
        }
        ArrayList<T> list = new ArrayList<T>(n);
        if (n != 0) {
            int type = readInt();
            if (n < 0) {
                return null;
            }
            switch (type) {
                case TYPE_STRING:
                    while (n > 0) {
                        list.add((T) readString());
                        n--;
                    }
                    break;
                case TYPE_PARCELABLE:
                    while (n > 0) {
                        list.add((T) readParcelable());
                        n--;
                    }
                    break;
                case TYPE_VERSIONED_PARCELABLE:
                    while (n > 0) {
                        list.add((T) readVersionedParcelable());
                        n--;
                    }
                    break;
                case TYPE_SERIALIZABLE:
                    while (n > 0) {
                        list.add((T) readSerializable());
                        n--;
                    }
                    break;
                case TYPE_BINDER:
                    while (n > 0) {
                        list.add((T) readStrongBinder());
                        n--;
                    }
                    break;
            }
        }
        return list.toArray(def);
    }

    /**
     */
    public <T extends VersionedParcelable> @Nullable T readVersionedParcelable(
            @Nullable T def,
            int fieldId
    ) {
        if (!readField(fieldId)) {
            return def;
        }
        return readVersionedParcelable();
    }

    /**
     * Read and return a new VersionedParcelable from the parcel.
     *
     * @return Returns the newly created VersionedParcelable, or null if a null
     * object has been written.
     * @throws BadParcelableException Throws BadVersionedParcelableException if there
     *                                was an error trying to instantiate the VersionedParcelable.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    protected <T extends VersionedParcelable> @Nullable T readVersionedParcelable() {
        String name = readString();
        if (name == null) {
            return null;
        }
        return readFromParcel(name, createSubParcel());
    }

    /**
     * Read and return a new Serializable object from the parcel.
     *
     * @return the Serializable object, or null if the Serializable name
     * wasn't found in the parcel.
     */
    protected @Nullable Serializable readSerializable() {
        String name = readString();
        if (name == null) {
            // For some reason we were unable to read the name of the Serializable (either there
            // is nothing left in the VersionedParcel to read, or the next value wasn't a String)
            // , so
            // return null, which indicates that the name wasn't found in the parcel.
            return null;
        }

        byte[] serializedData = readByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
        try {
            ObjectInputStream ois = new ObjectInputStream(bais) {
                @Override
                protected Class<?> resolveClass(ObjectStreamClass osClass)
                        throws IOException, ClassNotFoundException {
                    Class<?> c = Class.forName(osClass.getName(), false,
                            getClass().getClassLoader());
                    if (c != null) {
                        return c;
                    }
                    return super.resolveClass(osClass);
                }
            };
            return (Serializable) ois.readObject();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to read Serializable object (name = " + name + ")",
                    ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Unable to read Serializable object (name = " + name + ")",
                    cnfe);
        }
    }

    /**
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    protected <T extends VersionedParcelable> @NonNull T readFromParcel(
            @NonNull String parcelCls,
            @NonNull VersionedParcel versionedParcel
    ) {
        try {
            Method m = getReadMethod(parcelCls);
            return (T) m.invoke(null, versionedParcel);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     */
    protected <T extends VersionedParcelable> void writeToParcel(
            @NonNull T val,
            @NonNull VersionedParcel versionedParcel
    ) {
        try {
            Method m = getWriteMethod(val.getClass());
            m.invoke(null, val, versionedParcel);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull Method getReadMethod(@NonNull String parcelCls) throws IllegalAccessException,
            NoSuchMethodException, ClassNotFoundException {
        Method m = mReadCache.get(parcelCls);
        if (m == null) {
            Class<?> cls = Class.forName(parcelCls, false, VersionedParcel.class.getClassLoader());
            m = cls.getDeclaredMethod("read", VersionedParcel.class);
            mReadCache.put(parcelCls, m);
        }
        return m;
    }

    private @NonNull Method getWriteMethod(@NonNull Class<?> baseCls) throws IllegalAccessException,
            NoSuchMethodException, ClassNotFoundException {
        Method m = mWriteCache.get(baseCls.getName());
        if (m == null) {
            Class<?> cls = findParcelClass(baseCls);
            m = cls.getDeclaredMethod("write", baseCls, VersionedParcel.class);
            mWriteCache.put(baseCls.getName(), m);
        }
        return m;
    }

    private @NonNull Class<?> findParcelClass(@NonNull Class<?> cls)
            throws ClassNotFoundException {
        Class<?> ret = mParcelizerCache.get(cls.getName());
        if (ret == null) {
            String pkg = cls.getPackage().getName();
            String c = String.format("%s.%sParcelizer", pkg, cls.getSimpleName());
            ret = Class.forName(c, false, cls.getClassLoader());
            mParcelizerCache.put(cls.getName(), ret);
        }
        return ret;
    }

    /**
     */
    public static class ParcelException extends RuntimeException {
        public ParcelException(@Nullable Throwable source) {
            super(source);
        }
    }

    private static final class Api21Impl {
        static void writeSize(@NonNull VersionedParcel self, @Nullable Size val) {
            self.writeBoolean(val != null);
            if (val != null) {
                self.writeInt(val.getWidth());
                self.writeInt(val.getHeight());
            }
        }

        static void writeSizeF(@NonNull VersionedParcel self, @Nullable SizeF val) {
            self.writeBoolean(val != null);
            if (val != null) {
                self.writeFloat(val.getWidth());
                self.writeFloat(val.getHeight());
            }
        }

        static @Nullable Size readSize(@NonNull VersionedParcel self) {
            if (self.readBoolean()) {
                int width = self.readInt();
                int height = self.readInt();
                return new Size(width, height);
            }
            return null;
        }

        static @Nullable SizeF readSizeF(@NonNull VersionedParcel self) {
            if (self.readBoolean()) {
                float width = self.readFloat();
                float height = self.readFloat();
                return new SizeF(width, height);
            }
            return null;
        }
    }
}
