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

package androidx.work;

import android.arch.persistence.room.TypeConverter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistable set of key/value pairs which are passed as inputs and outputs for {@link Worker}s.
 * This is a lightweight container, and should not be considered your data store.  As such, there is
 * an enforced {@link #MAX_DATA_BYTES} limit on the serialized (byte array) size of the payloads.
 * This class will throw {@link IllegalStateException}s if you try to serialize or deserialize past
 * this limit.
 */

public final class Data {

    public static final Data EMPTY = new Data.Builder().build();
    public static final int MAX_DATA_BYTES = 10 * 1024;    // 10KB

    private static final String TAG = "Data";

    private Map<String, Object> mValues;

    Data() {    // stub required for room
    }

    Data(Map<String, ?> values) {
        mValues = new HashMap<>(values);
    }

    /**
     * Get the boolean value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the boolean array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @NonNull boolean[] getBooleanArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Boolean[]) {
            Boolean[] array = (Boolean[]) value;
            boolean[] returnArray = new boolean[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }


    /**
     * Get the integer value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public int getInt(@NonNull String key, int defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Integer) {
            return (int) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the integer array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @NonNull int[] getIntArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Integer[]) {
            Integer[] array = (Integer[]) value;
            int[] returnArray = new int[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Get the long value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public long getLong(@NonNull String key, long defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Long) {
            return (long) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the long array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable long[] getLongArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Long[]) {
            Long[] array = (Long[]) value;
            long[] returnArray = new long[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Get the float value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Float) {
            return (float) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the float array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable float[] getFloatArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Float[]) {
            Float[] array = (Float[]) value;
            float[] returnArray = new float[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Get the double value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public double getDouble(@NonNull String key, double defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Double) {
            return (double) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the double array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable double[] getDoubleArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Double[]) {
            Double[] array = (Double[]) value;
            double[] returnArray = new double[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Get the String value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public @Nullable String getString(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            return null;
        }
    }

    /**
     * Get the String array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable String[] getStringArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof String[]) {
            return (String[]) value;
        } else {
            return null;
        }
    }

    /**
     * Gets all the values in this Data object.
     *
     * @return A {@link Map} of key-value pairs for this object; this Map is unmodifiable and should
     * be used for reads only.
     */
    public @NonNull Map<String, Object> getKeyValueMap() {
        return Collections.unmodifiableMap(mValues);
    }

    /**
     * @return The number of arguments
     */
    @VisibleForTesting
    public int size() {
        return mValues.size();
    }

    /**
     * Converts {@link Data} to a byte array for persistent storage.
     *
     * @param data The {@link Data} object to convert
     * @return The byte array representation of the input
     * @throws IllegalStateException if the serialized payload is bigger than
     *         {@link #MAX_DATA_BYTES}
     */
    @TypeConverter
    public static @NonNull byte[] toByteArray(@NonNull Data data) throws IllegalStateException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(data.size());
            for (Map.Entry<String, Object> entry : data.mValues.entrySet()) {
                objectOutputStream.writeUTF(entry.getKey());
                objectOutputStream.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (objectOutputStream != null) {
                try {
                    objectOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (outputStream.size() > MAX_DATA_BYTES) {
            throw new IllegalStateException(
                    "Data cannot occupy more than " + MAX_DATA_BYTES + "KB when serialized");
        }
        return outputStream.toByteArray();
    }

    /**
     * Converts a byte array to {@link Data}.
     *
     * @param bytes The byte array representation to convert
     * @return An {@link Data} object built from the input
     * @throws IllegalStateException if bytes is bigger than {@link #MAX_DATA_BYTES}
     */
    @TypeConverter
    public static @NonNull Data fromByteArray(@NonNull byte[] bytes) throws IllegalStateException {
        if (bytes.length > MAX_DATA_BYTES) {
            throw new IllegalStateException(
                    "Data cannot occupy more than " + MAX_DATA_BYTES + "KB when serialized");
        }

        Map<String, Object> map = new HashMap<>();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            for (int i = objectInputStream.readInt(); i > 0; i--) {
                map.put(objectInputStream.readUTF(), objectInputStream.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Data(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Data other = (Data) o;
        return mValues.equals(other.mValues);
    }

    @Override
    public int hashCode() {
        return 31 * mValues.hashCode();
    }

    private static Boolean[] convertPrimitiveBooleanArray(boolean[] value) {
        Boolean[] returnValue = new Boolean[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    private static Integer[] convertPrimitiveIntArray(int[] value) {
        Integer[] returnValue = new Integer[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    private static Long[] convertPrimitiveLongArray(long[] value) {
        Long[] returnValue = new Long[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    private static Float[] convertPrimitiveFloatArray(float[] value) {
        Float[] returnValue = new Float[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    private static Double[] convertPrimitiveDoubleArray(double[] value) {
        Double[] returnValue = new Double[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    /**
     * A builder for {@link Data}.
     */
    public static final class Builder {

        private Map<String, Object> mValues = new HashMap<>();

        /**
         * Puts a boolean into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putBoolean(@NonNull String key, boolean value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a boolean array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putBooleanArray(@NonNull String key, boolean[] value) {
            mValues.put(key, convertPrimitiveBooleanArray(value));
            return this;
        }

        /**
         * Puts an integer into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putInt(@NonNull String key, int value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts an integer array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putIntArray(@NonNull String key, int[] value) {
            mValues.put(key, convertPrimitiveIntArray(value));
            return this;
        }

        /**
         * Puts a long into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putLong(@NonNull String key, long value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a long array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putLongArray(@NonNull String key, long[] value) {
            mValues.put(key, convertPrimitiveLongArray(value));
            return this;
        }

        /**
         * Puts a float into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putFloat(@NonNull String key, float value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a float array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putFloatArray(String key, float[] value) {
            mValues.put(key, convertPrimitiveFloatArray(value));
            return this;
        }

        /**
         * Puts a double into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putDouble(@NonNull String key, double value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a double array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putDoubleArray(@NonNull String key, double[] value) {
            mValues.put(key, convertPrimitiveDoubleArray(value));
            return this;
        }

        /**
         * Puts a String into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putString(@NonNull String key, String value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a String array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putStringArray(@NonNull String key, String[] value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts all input key-value pairs from the {@link Data} into the Builder.
         * Any non-valid types will be logged and ignored.  Valid types are: Boolean, Integer,
         * Long, Double, String, and array versions of each of those types.
         * Any {@code null} values will also be ignored.
         *
         * @param data {@link Data} containing key-value pairs to add
         * @return The {@link Builder}
         */
        public @NonNull Builder putAll(@NonNull Data data) {
            putAll(data.mValues);
            return this;
        }

        /**
         * Puts all input key-value pairs into the Builder. Valid types are: Boolean, Integer,
         * Long, Float, Double, String, and array versions of each of those types.
         * Invalid types throw an {@link IllegalArgumentException}.
         *
         * @param values A {@link Map} of key-value pairs to add
         * @return The {@link Builder}
         */
        public @NonNull Builder putAll(@NonNull Map<String, Object> values) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    mValues.put(key, null);
                    continue;
                }
                Class valueType = value.getClass();
                if (valueType == Boolean.class
                        || valueType == Integer.class
                        || valueType == Long.class
                        || valueType == Float.class
                        || valueType == Double.class
                        || valueType == String.class
                        || valueType == Boolean[].class
                        || valueType == Integer[].class
                        || valueType == Long[].class
                        || valueType == Float[].class
                        || valueType == Double[].class
                        || valueType == String[].class) {
                    mValues.put(key, value);
                } else if (valueType == boolean[].class) {
                    mValues.put(key, convertPrimitiveBooleanArray((boolean[]) value));
                } else if (valueType == int[].class) {
                    mValues.put(key, convertPrimitiveIntArray((int[]) value));
                } else if (valueType == long[].class) {
                    mValues.put(key, convertPrimitiveLongArray((long[]) value));
                } else if (valueType == float[].class) {
                    mValues.put(key, convertPrimitiveFloatArray((float[]) value));
                } else if (valueType == double[].class) {
                    mValues.put(key, convertPrimitiveDoubleArray((double[]) value));
                } else {
                    throw new IllegalArgumentException(
                            String.format("Key %s has invalid type %s", key, valueType));
                }
            }
            return this;
        }

        /**
         * Builds an {@link Data} object.
         *
         * @return The {@link Data} object containing all key-value pairs specified by this
         *         {@link Builder}.
         */
        public @NonNull Data build() {
            return new Data(mValues);
        }
    }
}
