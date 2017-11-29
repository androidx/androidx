/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.background.workmanager.model;

import android.arch.background.workmanager.Worker;
import android.arch.persistence.room.TypeConverter;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistable set of key/value pairs which are passed to {@link Worker}s.
 */

public final class Arguments {

    private static final String TAG = "Arguments";

    private Map<String, Object> mValues;

    public static final Arguments EMPTY = new Arguments.Builder().build();

    Arguments() {    // stub required for room
    }

    Arguments(Map<String, ?> values) {
        mValues = new HashMap<>(values);
    }

    /**
     * Get the boolean value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the integer value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public Integer getInt(String key, Integer defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
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
    public Integer[] getIntArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof Integer[]) {
            return (Integer[]) value;
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
    public Long getLong(String key, Long defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Long) {
            return (Long) value;
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
    public Long[] getLongArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof Long[]) {
            return (Long[]) value;
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
    public Double getDouble(String key, Double defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Double) {
            return (Double) value;
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
    public Double[] getDoubleArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof Double[]) {
            return (Double[]) value;
        } else {
            return null;
        }
    }

    /**
     * Get the String value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public String getString(String key, String defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Get the String array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public String[] getStringArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof String[]) {
            return (String[]) value;
        } else {
            return null;
        }
    }

    /**
     * Gets all the values in this Arguments object.
     *
     * @return A {@link Map} of key-value pairs for this object; this Map is unmodifiable and should
     * be used for reads only.
     */
    public Map<String, Object> getKeyValueMap() {
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
     * Converts {@link Arguments} to a byte array for persistent storage.
     *
     * @param arguments The {@link Arguments} object to convert
     * @return The byte array representation of the input
     */
    @TypeConverter
    public static byte[] toByteArray(Arguments arguments) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(arguments.size());
            for (Map.Entry<String, Object> entry : arguments.mValues.entrySet()) {
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
        return outputStream.toByteArray();
    }

    /**
     * Converts a byte array to {@link Arguments}.
     *
     * @param bytes The byte array representation to convert
     * @return An {@link Arguments} object built from the input
     */
    @TypeConverter
    public static Arguments fromByteArray(byte[] bytes) {
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
        return new Arguments(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Arguments other = (Arguments) o;
        return mValues.equals(other.mValues);
    }

    @Override
    public int hashCode() {
        return 31 * mValues.hashCode();
    }

    /**
     * A builder for {@link Arguments}.
     */
    public static class Builder {

        private Map<String, Object> mValues = new HashMap<>();

        /**
         * Puts a boolean into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public Builder putBoolean(String key, Boolean value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts an integer into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public Builder putInt(String key, Integer value) {
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
        public Builder putIntArray(String key, Integer[] value) {
            mValues.put(key, value);
            return this;
        }


        /**
         * Puts a long into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public Builder putLong(String key, Long value) {
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
        public Builder putLongArray(String key, Long[] value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a double into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public Builder putDouble(String key, Double value) {
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
        public Builder putDoubleArray(String key, Double[] value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a String into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public Builder putString(String key, String value) {
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
        public Builder putStringArray(String key, String[] value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts all input key-value pairs into the Builder.  Any non-valid types will be logged and
         * ignored.  Valid types are: Boolean, Integer, Long, Double, String, and array versions of
         * each of those types.  Any {@code null} values will also be ignored.
         *
         * @param values A {@link Map} of key-value pairs to add
         */
        public void putAll(Map<String, Object> values) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    Log.w(TAG, "Ignoring null value for key " + key);
                    continue;
                }
                Class valueType = value.getClass();
                if (valueType == Boolean.class
                        || valueType == Integer.class
                        || valueType == Long.class
                        || valueType == Double.class
                        || valueType == String.class
                        || valueType == Boolean[].class
                        || valueType == Integer[].class
                        || valueType == Long[].class
                        || valueType == Double[].class
                        || valueType == String[].class) {
                    mValues.put(key, value);
                } else {
                    Log.w(TAG, "Ignoring key " + key + " because of invalid type " + valueType);
                }
            }
        }

        /**
         * Builds an {@link Arguments} object.
         *
         * @return The {@link Arguments} object containing all key-value pairs specified by this
         *         {@link Builder}.
         */
        public Arguments build() {
            return new Arguments(mValues);
        }
    }
}
