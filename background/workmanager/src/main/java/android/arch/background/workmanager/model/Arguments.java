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
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Persistable set of key/value pairs which are passed to {@link Worker}s.
 */

public final class Arguments {

    private Map<String, Object> mValues;

    public static final Arguments EMPTY = new Arguments.Builder().build();

    public Arguments() {    // stub required for room
    }

    Arguments(Map<? extends String, ?> values) {
        mValues = new HashMap<>(values);
    }

    /**
     * Get the boolean value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public boolean getBoolean(String key, boolean defaultValue) {
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
    public int getInt(String key, int defaultValue) {
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
    public int[] getIntArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof int[]) {
            return (int[]) value;
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
    public long getLong(String key, long defaultValue) {
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
    public long[] getLongArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof long[]) {
            return (long[]) value;
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
    public double getDouble(String key, double defaultValue) {
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
    public double[] getDoubleArray(String key) {
        Object value = mValues.get(key);
        if (value instanceof double[]) {
            return (double[]) value;
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
     * Merges the input {@link Arguments} into this object, clobbering existing keys if needed.
     *
     * @param input The {@link Arguments} to merge
     */
    public void merge(@NonNull Arguments input) {
        mValues.putAll(input.mValues);
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
        public Builder putBoolean(String key, boolean value) {
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
        public Builder putInt(String key, int value) {
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
        public Builder putIntArray(String key, int[] value) {
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
        public Builder putLong(String key, long value) {
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
        public Builder putLongArray(String key, long[] value) {
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
        public Builder putDouble(String key, double value) {
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
        public Builder putDoubleArray(String key, double[] value) {
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
