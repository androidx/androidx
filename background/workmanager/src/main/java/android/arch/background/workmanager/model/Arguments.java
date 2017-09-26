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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Persistable set of Key/Value pairs which are passed to each {@link Worker}.
 */

public final class Arguments {
    private Map<String, Object> mValues;

    public Arguments() {
        mValues = new HashMap<>();
    }

    Arguments(Map<? extends String, ?> values) {
        mValues = new HashMap<>(values);
    }

    /**
     * Insert boolean into arguments.
     *
     * @param key   String
     * @param value boolean
     */
    public void putBoolean(String key, boolean value) {
        mValues.put(key, value);
    }

    /**
     * Get boolean matching key from arguments. If not found, use default value specified.
     *
     * @param key String
     * @return boolean
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
     * Insert int into arguments.
     *
     * @param key   String
     * @param value int
     */
    public void putInt(String key, int value) {
        mValues.put(key, value);
    }

    /**
     * Get int matching key from arguments. If not found, use default value specified.
     *
     * @param key String
     * @return int
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
     * Insert int array into arguments.
     *
     * @param key   String
     * @param value int array
     */
    public void putIntArray(String key, int[] value) {
        mValues.put(key, value);
    }

    /**
     * Get int array matching key from arguments. If not found, return null.
     *
     * @param key String
     * @return int array
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
     * Insert long into arguments.
     *
     * @param key   String
     * @param value long
     */
    public void putLong(String key, long value) {
        mValues.put(key, value);
    }

    /**
     * Get long matching key from arguments. If not found, use default value specified.
     *
     * @param key String
     * @return long
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
     * Insert long array into arguments.
     *
     * @param key   String
     * @param value long array
     */
    public void putLongArray(String key, long[] value) {
        mValues.put(key, value);
    }

    /**
     * Get long array matching key from arguments. If not found, return null.
     *
     * @param key String
     * @return long array
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
     * Insert double into arguments.
     *
     * @param key   String
     * @param value double
     */
    public void putDouble(String key, double value) {
        mValues.put(key, value);
    }

    /**
     * Get double matching key from arguments. If not found, use default value specified.
     *
     * @param key String
     * @return long array
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
     * Insert double array into arguments.
     *
     * @param key   String
     * @param value double array
     */
    public void putDoubleArray(String key, double[] value) {
        mValues.put(key, value);
    }

    /**
     * Get double array matching key from arguments. If not found, return null.
     *
     * @param key String
     * @return double array
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
     * Insert String into arguments.
     *
     * @param key   String
     * @param value String
     */
    public void putString(String key, String value) {
        mValues.put(key, value);
    }

    /**
     * Get String matching key from arguments. If not found, use default value specified.
     *
     * @param key String
     * @return String
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
     * Insert String array into arguments.
     *
     * @param key   String
     * @param value String array
     */
    public void putStringArray(String key, String[] value) {
        mValues.put(key, value);
    }

    /**
     * Get String array matching key from arguments. If not found, return null.
     *
     * @param key String
     * @return String
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
     * Clears all arguments.
     */
    public void clear() {
        mValues.clear();
    }

    /**
     * Determine if key is present in arguments.
     *
     * @param key String
     * @return true if key is present, false otherwise
     */
    public boolean containsKey(String key) {
        return mValues.containsKey(key);
    }

    /**
     * Determine if arguments are empty.
     *
     * @return true if arguments are empty, false otherwise
     */
    public boolean isEmpty() {
        return mValues.isEmpty();
    }

    /**
     * Get set of keys for arguments.
     *
     * @return Set<String> of keys
     */
    public Set<String> keySet() {
        return mValues.keySet();
    }

    /**
     * Removes a key/value pair from arguments.
     *
     * @param key String
     */
    public void remove(String key) {
        mValues.remove(key);
    }

    /**
     * Get number of arguments.
     *
     * @return int
     */
    public int size() {
        return mValues.size();
    }

    /**
     * Get set of key/value pairs for arguments.
     *
     * @return Set<Entry> of keys
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return mValues.entrySet();
    }

    /**
     * Converts {@link Arguments} to Byte Array for persistent storage.
     *
     * @param arguments {@link Arguments} object to convert
     * @return byte array representation
     */
    @TypeConverter
    public static byte[] toByteArray(Arguments arguments) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(arguments.size());
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
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
     * Converts Byte Array to {@link Arguments}.
     *
     * @param bytes byte array representation to convert
     * @return {@link Arguments} object
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
}
