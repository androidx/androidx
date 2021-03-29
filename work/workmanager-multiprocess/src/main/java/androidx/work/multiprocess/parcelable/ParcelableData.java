/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.work.multiprocess.parcelable;

import static androidx.work.Data.convertPrimitiveBooleanArray;
import static androidx.work.Data.convertPrimitiveByteArray;
import static androidx.work.Data.convertPrimitiveDoubleArray;
import static androidx.work.Data.convertPrimitiveFloatArray;
import static androidx.work.Data.convertPrimitiveIntArray;
import static androidx.work.Data.convertPrimitiveLongArray;
import static androidx.work.Data.convertToPrimitiveArray;
import static androidx.work.multiprocess.parcelable.ParcelUtils.readBooleanValue;
import static androidx.work.multiprocess.parcelable.ParcelUtils.writeBooleanValue;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.work.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link androidx.work.Data} but {@link android.os.Parcelable}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class ParcelableData implements Parcelable {

    // The list of supported types.
    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_BOOLEAN = 1;
    private static final byte TYPE_BYTE = 2;
    private static final byte TYPE_INTEGER = 3;
    private static final byte TYPE_LONG = 4;
    private static final byte TYPE_FLOAT = 5;
    private static final byte TYPE_DOUBLE = 6;
    private static final byte TYPE_STRING = 7;
    private static final byte TYPE_BOOLEAN_ARRAY = 8;
    private static final byte TYPE_BYTE_ARRAY = 9;
    private static final byte TYPE_INTEGER_ARRAY = 10;
    private static final byte TYPE_LONG_ARRAY = 11;
    private static final byte TYPE_FLOAT_ARRAY = 12;
    private static final byte TYPE_DOUBLE_ARRAY = 13;
    private static final byte TYPE_STRING_ARRAY = 14;

    private final Data mData;

    public ParcelableData(@NonNull Data data) {
        mData = data;
    }

    protected ParcelableData(@NonNull Parcel in) {
        Map<String, Object> values = new HashMap<>();
        // size
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            // entries
            addEntry(in, values);
        }
        mData = new Data(values);
    }

    public static final Creator<ParcelableData> CREATOR =
            new Creator<ParcelableData>() {
                @Override
                public ParcelableData createFromParcel(@NonNull Parcel in) {
                    return new ParcelableData(in);
                }

                @Override
                public ParcelableData[] newArray(int size) {
                    return new ParcelableData[size];
                }
            };

    @NonNull
    public Data getData() {
        return mData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        Map<String, Object> keyValueMap = mData.getKeyValueMap();
        // size
        parcel.writeInt(keyValueMap.size());
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            writeToParcel(parcel, entry.getKey(), entry.getValue());
        }
    }

    private void writeToParcel(
            @NonNull Parcel parcel,
            @NonNull String key,
            @Nullable Object value) {

        // type + value
        if (value == null) {
            parcel.writeByte(TYPE_NULL);
        } else {
            Class<?> valueType = value.getClass();
            if (valueType == Boolean.class) {
                parcel.writeByte(TYPE_BOOLEAN);
                boolean booleanValue = (Boolean) value;
                writeBooleanValue(parcel, booleanValue);
            } else if (valueType == Byte.class) {
                parcel.writeByte(TYPE_BYTE);
                byte byteValue = (Byte) value;
                parcel.writeByte(byteValue);
            } else if (valueType == Integer.class) {
                parcel.writeByte(TYPE_INTEGER);
                int intValue = (Integer) value;
                parcel.writeInt(intValue);
            } else if (valueType == Long.class) {
                parcel.writeByte(TYPE_LONG);
                Long longValue = (Long) value;
                parcel.writeLong(longValue);
            } else if (valueType == Float.class) {
                parcel.writeByte(TYPE_FLOAT);
                float floatValue = (Float) value;
                parcel.writeFloat(floatValue);
            } else if (valueType == Double.class) {
                parcel.writeByte(TYPE_DOUBLE);
                double doubleValue = (Double) value;
                parcel.writeDouble(doubleValue);
            } else if (valueType == String.class) {
                parcel.writeByte(TYPE_STRING);
                String stringValue = (String) value;
                parcel.writeString(stringValue);
            } else if (valueType == Boolean[].class) {
                parcel.writeByte(TYPE_BOOLEAN_ARRAY);
                Boolean[] booleanArray = (Boolean[]) value;
                parcel.writeBooleanArray(convertToPrimitiveArray(booleanArray));
            } else if (valueType == Byte[].class) {
                parcel.writeByte(TYPE_BYTE_ARRAY);
                Byte[] byteArray = (Byte[]) value;
                parcel.writeByteArray(convertToPrimitiveArray(byteArray));
            } else if (valueType == Integer[].class) {
                parcel.writeByte(TYPE_INTEGER_ARRAY);
                Integer[] integerArray = (Integer[]) value;
                parcel.writeIntArray(convertToPrimitiveArray(integerArray));
            } else if (valueType == Long[].class) {
                parcel.writeByte(TYPE_LONG_ARRAY);
                Long[] longArray = (Long[]) value;
                parcel.writeLongArray(convertToPrimitiveArray(longArray));
            } else if (valueType == Float[].class) {
                parcel.writeByte(TYPE_FLOAT_ARRAY);
                Float[] floatArray = (Float[]) value;
                parcel.writeFloatArray(convertToPrimitiveArray(floatArray));
            } else if (valueType == Double[].class) {
                parcel.writeByte(TYPE_DOUBLE_ARRAY);
                Double[] doubleArray = (Double[]) value;
                parcel.writeDoubleArray(convertToPrimitiveArray(doubleArray));
            } else if (valueType == String[].class) {
                parcel.writeByte(TYPE_STRING_ARRAY);
                String[] stringArray = (String[]) value;
                parcel.writeStringArray(stringArray);
            } else {
                // Exhaustive check
                String message = String.format("Unsupported value type %s", valueType.getName());
                throw new IllegalArgumentException(message);
            }
        }
        // key
        parcel.writeString(key);
    }

    private void addEntry(@NonNull Parcel parcel, @NonNull Map<String, Object> values) {
        // type
        int type = parcel.readByte();
        Object value = null;
        switch (type) {
            case TYPE_NULL:
                break;
            case TYPE_BYTE:
                value = parcel.readByte();
                break;
            case TYPE_BOOLEAN:
                value = readBooleanValue(parcel);
                break;
            case TYPE_INTEGER:
                value = parcel.readInt();
                break;
            case TYPE_LONG:
                value = parcel.readLong();
                break;
            case TYPE_FLOAT:
                value = parcel.readFloat();
                break;
            case TYPE_DOUBLE:
                value = parcel.readDouble();
                break;
            case TYPE_STRING:
                value = parcel.readString();
                break;
            case TYPE_BOOLEAN_ARRAY:
                value = convertPrimitiveBooleanArray(parcel.createBooleanArray());
                break;
            case TYPE_BYTE_ARRAY:
                value = convertPrimitiveByteArray(parcel.createByteArray());
                break;
            case TYPE_INTEGER_ARRAY:
                value = convertPrimitiveIntArray(parcel.createIntArray());
                break;
            case TYPE_LONG_ARRAY:
                value = convertPrimitiveLongArray(parcel.createLongArray());
                break;
            case TYPE_FLOAT_ARRAY:
                value = convertPrimitiveFloatArray(parcel.createFloatArray());
                break;
            case TYPE_DOUBLE_ARRAY:
                value = convertPrimitiveDoubleArray(parcel.createDoubleArray());
                break;
            case TYPE_STRING_ARRAY:
                value = parcel.createStringArray();
                break;
            default:
                String message = String.format("Unsupported type %s", type);
                throw new IllegalStateException(message);
        }
        String key = parcel.readString();
        values.put(key, value);
    }
}
