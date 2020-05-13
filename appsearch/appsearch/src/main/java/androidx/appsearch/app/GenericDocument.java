/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.app;

import android.util.Log;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

import com.google.android.icing.proto.DocumentProto;
import com.google.android.icing.proto.PropertyProto;
import com.google.android.icing.protobuf.ByteString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a document unit.
 *
 * <p>Documents are constructed via {@link GenericDocument.Builder}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class GenericDocument {
    private static final String TAG = "GenericDocument";

    /**
     * The maximum number of elements in a repeatable field. Will reject the request if exceed
     * this limit.
     */
    private static final int MAX_REPEATED_PROPERTY_LENGTH = 100;

    /**
     * The maximum {@link String#length} of a {@link String} field. Will reject the request if
     * {@link String}s longer than this.
     */
    private static final int MAX_STRING_LENGTH = 20_000;

    /**
     * Contains {@link GenericDocument} basic information (uri, schemaType etc) and properties
     * ordered by keys.
     */
    @NonNull
    private final DocumentProto mProto;

    /** Contains all properties in {@link #mProto} to support getting properties via keys. */
    @NonNull
    private final Map<String, Object> mProperties;

    /**
     * Creates a new {@link GenericDocument}.
     * @param proto Contains {@link GenericDocument} basic information (uri, schemaType etc) and
     *               properties ordered by keys.
     * @param propertiesMap Contains all properties in {@link #mProto} to support get properties
     *                      via keys.
     */
    GenericDocument(@NonNull DocumentProto proto,
            @NonNull Map<String, Object> propertiesMap) {
        mProto = proto;
        mProperties = propertiesMap;
    }

    /**
     * Creates a new {@link GenericDocument} from an existing instance.
     *
     * <p>This method should be only used by constructor of a subclass.
     */
    protected GenericDocument(@NonNull GenericDocument document) {
        this(document.mProto, document.mProperties);
    }

    GenericDocument(@NonNull DocumentProto documentProto) {
        this(documentProto, new ArrayMap<>());
        for (int i = 0; i < documentProto.getPropertiesCount(); i++) {
            PropertyProto property = documentProto.getProperties(i);
            String name = property.getName();
            if (property.getStringValuesCount() > 0) {
                String[] values = new String[property.getStringValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getStringValues(j);
                }
                mProperties.put(name, values);
            } else if (property.getInt64ValuesCount() > 0) {
                long[] values = new long[property.getInt64ValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getInt64Values(j);
                }
                mProperties.put(property.getName(), values);
            } else if (property.getDoubleValuesCount() > 0) {
                double[] values = new double[property.getDoubleValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getDoubleValues(j);
                }
                mProperties.put(property.getName(), values);
            } else if (property.getBooleanValuesCount() > 0) {
                boolean[] values = new boolean[property.getBooleanValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getBooleanValues(j);
                }
                mProperties.put(property.getName(), values);
            } else if (property.getBytesValuesCount() > 0) {
                byte[][] values = new byte[property.getBytesValuesCount()][];
                for (int j = 0; j < values.length; j++) {
                    values[j] = property.getBytesValues(j).toByteArray();
                }
                mProperties.put(name, values);
            } else if (property.getDocumentValuesCount() > 0) {
                GenericDocument[] values =
                        new GenericDocument[property.getDocumentValuesCount()];
                for (int j = 0; j < values.length; j++) {
                    values[j] = new GenericDocument(property.getDocumentValues(j));
                }
                mProperties.put(name, values);
            } else {
                throw new IllegalStateException("Unknown type of value: " + name);
            }
        }
    }

    /**
     * Get the {@link DocumentProto} of the {@link GenericDocument}.
     *
     * <p>The {@link DocumentProto} contains {@link GenericDocument}'s basic information and all
     *    properties ordered by keys.
     */
    @NonNull
    DocumentProto getProto() {
        return mProto;
    }

    /** Returns the URI of the {@link GenericDocument}. */
    @NonNull
    public String getUri() {
        return mProto.getUri();
    }

    /** Returns the schema type of the {@link GenericDocument}. */
    @NonNull
    public String getSchemaType() {
        return mProto.getSchema();
    }

    /** Returns the creation timestamp of the {@link GenericDocument}, in milliseconds. */
    public long getCreationTimestampMillis() {
        return mProto.getCreationTimestampMs();
    }

    /**
     * Returns the TTL (Time To Live) of the {@link GenericDocument}, in milliseconds.
     *
     * <p>The default value is 0, which means the document is permanent and won't be auto-deleted
     *    until the app is uninstalled.
     */
    public long getTtlMillis() {
        return mProto.getTtlMs();
    }

    /**
     * Returns the score of the {@link GenericDocument}.
     *
     * <p>The score is a query-independent measure of the document's quality, relative to other
     * {@link GenericDocument}s of the same type.
     *
     * <p>The default value is 0.
     */
    public int getScore() {
        return mProto.getScore();
    }

    /**
     * Retrieves a {@link String} value by key.
     *
     * @param key The key to look for.
     * @return The first {@link String} associated with the given key or {@code null} if there
     *         is no such key or the value is of a different type.
     */
    @Nullable
    public String getPropertyString(@NonNull String key) {
        String[] propertyArray = getPropertyStringArray(key);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("String", key, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code long} value by key.
     *
     * @param key The key to look for.
     * @return The first {@code long} associated with the given key or default value {@code 0} if
     *         there is no such key or the value is of a different type.
     */
    public long getPropertyLong(@NonNull String key) {
        long[] propertyArray = getPropertyLongArray(key);
        if (propertyArray == null || propertyArray.length == 0) {
            return 0;
        }
        warnIfSinglePropertyTooLong("Long", key, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code double} value by key.
     *
     * @param key The key to look for.
     * @return The first {@code double} associated with the given key or default value {@code 0.0}
     *         if there is no such key or the value is of a different type.
     */
    public double getPropertyDouble(@NonNull String key) {
        double[] propertyArray = getPropertyDoubleArray(key);
        // TODO(tytytyww): Add support double array to ArraysUtils.isEmpty().
        if (propertyArray == null || propertyArray.length == 0) {
            return 0.0;
        }
        warnIfSinglePropertyTooLong("Double", key, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code boolean} value by key.
     *
     * @param key The key to look for.
     * @return The first {@code boolean} associated with the given key or default value
     *         {@code false} if there is no such key or the value is of a different type.
     */
    public boolean getPropertyBoolean(@NonNull String key) {
        boolean[] propertyArray = getPropertyBooleanArray(key);
        if (propertyArray == null || propertyArray.length == 0) {
            return false;
        }
        warnIfSinglePropertyTooLong("Boolean", key, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@code byte[]} value by key.
     *
     * @param key The key to look for.
     * @return The first {@code byte[]} associated with the given key or {@code null} if there
     *         is no such key or the value is of a different type.
     */
    @Nullable
    public byte[] getPropertyBytes(@NonNull String key) {
        byte[][] propertyArray = getPropertyBytesArray(key);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("ByteArray", key, propertyArray.length);
        return propertyArray[0];
    }

    /**
     * Retrieves a {@link GenericDocument} value by key.
     *
     * @param key The key to look for.
     * @return The first {@link GenericDocument} associated with the given key or {@code null} if
     *         there is no such key or the value is of a different type.
     */
    @Nullable
    public GenericDocument getPropertyDocument(@NonNull String key) {
        GenericDocument[] propertyArray = getPropertyDocumentArray(key);
        if (propertyArray == null || propertyArray.length == 0) {
            return null;
        }
        warnIfSinglePropertyTooLong("Document", key, propertyArray.length);
        return propertyArray[0];
    }

    /** Prints a warning to logcat if the given propertyLength is greater than 1. */
    private static void warnIfSinglePropertyTooLong(
            @NonNull String propertyType, @NonNull String key, int propertyLength) {
        if (propertyLength > 1) {
            Log.w(TAG, "The value for \"" + key + "\" contains " + propertyLength
                    + " elements. Only the first one will be returned from "
                    + "getProperty" + propertyType + "(). Try getProperty" + propertyType
                    + "Array().");
        }
    }

    /**
     * Retrieves a repeated {@code String} property by key.
     *
     * @param key The key to look for.
     * @return The {@code String[]} associated with the given key, or {@code null} if no value
     *         is set or the value is of a different type.
     */
    @Nullable
    public String[] getPropertyStringArray(@NonNull String key) {
        return getAndCastPropertyArray(key, String[].class);
    }

    /**
     * Retrieves a repeated {@link String} property by key.
     *
     * @param key The key to look for.
     * @return The {@code long[]} associated with the given key, or {@code null} if no value is
     *         set or the value is of a different type.
     */
    @Nullable
    public long[] getPropertyLongArray(@NonNull String key) {
        return getAndCastPropertyArray(key, long[].class);
    }

    /**
     * Retrieves a repeated {@code double} property by key.
     *
     * @param key The key to look for.
     * @return The {@code double[]} associated with the given key, or {@code null} if no value
     *         is set or the value is of a different type.
     */
    @Nullable
    public double[] getPropertyDoubleArray(@NonNull String key) {
        return getAndCastPropertyArray(key, double[].class);
    }

    /**
     * Retrieves a repeated {@code boolean} property by key.
     *
     * @param key The key to look for.
     * @return The {@code boolean[]} associated with the given key, or {@code null} if no value
     *         is set or the value is of a different type.
     */
    @Nullable
    public boolean[] getPropertyBooleanArray(@NonNull String key) {
        return getAndCastPropertyArray(key, boolean[].class);
    }

    /**
     * Retrieves a {@code byte[][]} property by key.
     *
     * @param key The key to look for.
     * @return The {@code byte[][]} associated with the given key, or {@code null} if no value
     *         is set or the value is of a different type.
     */
    @Nullable
    public byte[][] getPropertyBytesArray(@NonNull String key) {
        return getAndCastPropertyArray(key, byte[][].class);
    }

    /**
     * Retrieves a repeated {@link GenericDocument} property by key.
     *
     * @param key The key to look for.
     * @return The {@link GenericDocument[]} associated with the given key, or {@code null} if no
     *         value is set or the value is of a different type.
     */
    @Nullable
    public GenericDocument[] getPropertyDocumentArray(@NonNull String key) {
        return getAndCastPropertyArray(key, GenericDocument[].class);
    }

    /**
     * Gets a repeated property of the given key, and casts it to the given class type, which
     * must be an array class type.
     */
    @Nullable
    private <T> T getAndCastPropertyArray(@NonNull String key, @NonNull Class<T> tClass) {
        Object value = mProperties.get(key);
        if (value == null) {
            return null;
        }
        try {
            return tClass.cast(value);
        } catch (ClassCastException e) {
            Log.w(TAG, "Error casting to requested type for key \"" + key + "\"", e);
            return null;
        }
    }

    @Override
    public boolean equals(@Nullable Object other) {
        // Check only proto's equality is sufficient here since all properties in
        // mProperties are ordered by keys and stored in proto.
        if (this == other) {
            return true;
        }
        if (!(other instanceof GenericDocument)) {
            return false;
        }
        GenericDocument otherDocument = (GenericDocument) other;
        return this.mProto.equals(otherDocument.mProto);
    }

    @Override
    public int hashCode() {
        // Hash only proto is sufficient here since all properties in mProperties are ordered by
        // keys and stored in proto.
        return mProto.hashCode();
    }

    @Override
    @NonNull
    public String toString() {
        return mProto.toString();
    }

    /**
     * The builder class for {@link GenericDocument}.
     *
     * @param <BuilderType> Type of subclass who extend this.
     */
    public static class Builder<BuilderType extends Builder> {

        private final Map<String, Object> mProperties = new ArrayMap<>();
        private final DocumentProto.Builder mProtoBuilder = DocumentProto.newBuilder();
        private final BuilderType mBuilderTypeInstance;

        /**
         * Create a new {@link GenericDocument.Builder}.
         *
         * @param uri The uri of {@link GenericDocument}.
         * @param schemaType The schema type of the {@link GenericDocument}. The passed-in
         *        {@code schemaType} must be defined using {@link AppSearchManager#setSchema} prior
         *        to inserting a document of this {@code schemaType} into the AppSearch index using
         *        {@link AppSearchManager#putDocuments}. Otherwise, the document will be
         *        rejected by {@link AppSearchManager#putDocuments}.
         */
        @SuppressWarnings("unchecked")
        public Builder(@NonNull String uri, @NonNull String schemaType) {
            mBuilderTypeInstance = (BuilderType) this;
            mProtoBuilder.setUri(uri).setSchema(schemaType);
            // Set current timestamp for creation timestamp by default.
            setCreationTimestampMillis(System.currentTimeMillis());
        }

        /**
         * Sets the score of the {@link GenericDocument}.
         *
         * <p>The score is a query-independent measure of the document's quality, relative to
         * other {@link GenericDocument}s of the same type.
         *
         * @throws IllegalArgumentException If the provided value is negative.
         */
        @NonNull
        public BuilderType setScore(@IntRange(from = 0, to = Integer.MAX_VALUE) int score) {
            if (score < 0) {
                throw new IllegalArgumentException("Document score cannot be negative.");
            }
            mProtoBuilder.setScore(score);
            return mBuilderTypeInstance;
        }

        /**
         * Set the creation timestamp in milliseconds of the {@link GenericDocument}. Should be
         * set using a value obtained from the {@link System#currentTimeMillis()} time base.
         */
        @NonNull
        public BuilderType setCreationTimestampMillis(long creationTimestampMillis) {
            mProtoBuilder.setCreationTimestampMs(creationTimestampMillis);
            return mBuilderTypeInstance;
        }

        /**
         * Set the TTL (Time To Live) of the {@link GenericDocument}, in milliseconds.
         *
         * <p>After this many milliseconds since the {@link #setCreationTimestampMillis creation
         * timestamp}, the document is deleted.
         *
         * @param ttlMillis A non-negative duration in milliseconds.
         * @throws IllegalArgumentException If the provided value is negative.
         */
        @NonNull
        public BuilderType setTtlMillis(long ttlMillis) {
            if (ttlMillis < 0) {
                throw new IllegalArgumentException("Document ttlMillis cannot be negative.");
            }
            mProtoBuilder.setTtlMs(ttlMillis);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code String} values for a property, replacing its previous
         * values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@code String} values of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull String... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code boolean} values for a property, replacing its previous
         * values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@code boolean} values of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull boolean... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code long} values for a property, replacing its previous
         * values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@code long} values of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull long... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code double} values for a property, replacing its previous
         * values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@code double} values of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull double... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@code byte[]} for a property, replacing its previous values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@code byte[]} of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull byte[]... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        /**
         * Sets one or multiple {@link GenericDocument} values for a property, replacing its
         * previous values.
         *
         * @param key The key associated with the {@code values}.
         * @param values The {@link GenericDocument} values of the property.
         */
        @NonNull
        public BuilderType setProperty(@NonNull String key, @NonNull GenericDocument... values) {
            putInPropertyMap(key, values);
            return mBuilderTypeInstance;
        }

        private void putInPropertyMap(@NonNull String key, @NonNull String[] values)
                throws IllegalArgumentException {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            validateRepeatedPropertyLength(key, values.length);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The String at " + i + " is null.");
                } else if (values[i].length() > MAX_STRING_LENGTH) {
                    throw new IllegalArgumentException("The String at " + i + " length is: "
                            + values[i].length()  + ", which exceeds length limit: "
                            + MAX_STRING_LENGTH + ".");
                }
            }
            mProperties.put(key, values);
        }

        private void putInPropertyMap(@NonNull String key, @NonNull boolean[] values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            validateRepeatedPropertyLength(key, values.length);
            mProperties.put(key, values);
        }

        private void putInPropertyMap(@NonNull String key, @NonNull double[] values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            validateRepeatedPropertyLength(key, values.length);
            mProperties.put(key, values);
        }

        private void putInPropertyMap(@NonNull String key, @NonNull long[] values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            validateRepeatedPropertyLength(key, values.length);
            mProperties.put(key, values);
        }

        private void putInPropertyMap(@NonNull String key, @NonNull byte[][] values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            validateRepeatedPropertyLength(key, values.length);
            mProperties.put(key, values);
        }

        private void putInPropertyMap(@NonNull String key, @NonNull GenericDocument[] values) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(values);
            for (int i = 0; i < values.length; i++) {
                if (values[i] == null) {
                    throw new IllegalArgumentException("The document at " + i + " is null.");
                }
            }
            validateRepeatedPropertyLength(key, values.length);
            mProperties.put(key, values);
        }

        private static void validateRepeatedPropertyLength(@NonNull String key, int length) {
            if (length == 0) {
                throw new IllegalArgumentException("The input array is empty.");
            } else if (length > MAX_REPEATED_PROPERTY_LENGTH) {
                throw new IllegalArgumentException(
                        "Repeated property \"" + key + "\" has length " + length
                                + ", which exceeds the limit of "
                                + MAX_REPEATED_PROPERTY_LENGTH);
            }
        }

        /** Builds the {@link GenericDocument} object. */
        @NonNull
        public GenericDocument build() {
            // Build proto by sorting the keys in mProperties to exclude the influence of
            // order. Therefore documents will generate same proto as long as the contents are
            // same. Note that the order of repeated fields is still preserved.
            ArrayList<String> keys = new ArrayList<>(mProperties.keySet());
            Collections.sort(keys);
            for (int i = 0; i < keys.size(); i++) {
                String name = keys.get(i);
                Object values = mProperties.get(name);
                PropertyProto.Builder propertyProto = PropertyProto.newBuilder().setName(name);
                if (values instanceof boolean[]) {
                    for (boolean value : (boolean[]) values) {
                        propertyProto.addBooleanValues(value);
                    }
                } else if (values instanceof long[]) {
                    for (long value : (long[]) values) {
                        propertyProto.addInt64Values(value);
                    }
                } else if (values instanceof double[]) {
                    for (double value : (double[]) values) {
                        propertyProto.addDoubleValues(value);
                    }
                } else if (values instanceof String[]) {
                    for (String value : (String[]) values) {
                        propertyProto.addStringValues(value);
                    }
                } else if (values instanceof GenericDocument[]) {
                    for (GenericDocument value : (GenericDocument[]) values) {
                        propertyProto.addDocumentValues(value.getProto());
                    }
                } else if (values instanceof byte[][]) {
                    for (byte[] value : (byte[][]) values) {
                        propertyProto.addBytesValues(ByteString.copyFrom(value));
                    }
                } else {
                    throw new IllegalStateException(
                            "Property \"" + name + "\" has unsupported value type \""
                                    + values.getClass().getSimpleName() + "\"");
                }
                mProtoBuilder.addProperties(propertyProto);
            }
            return new GenericDocument(mProtoBuilder.build(), mProperties);
        }
    }
}
