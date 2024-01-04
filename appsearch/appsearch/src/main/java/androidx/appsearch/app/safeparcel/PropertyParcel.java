/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.app.safeparcel;


import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.PropertyParcelCreator;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@link SafeParcelable} to hold the value of a property in {@code GenericDocument#mProperties}.
 *
 * <p>This resembles PropertyProto in IcingLib.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PropertyParcelCreator")
public final class PropertyParcel extends AbstractSafeParcelable {
    @NonNull public static final PropertyParcelCreator CREATOR = new PropertyParcelCreator();

    @NonNull
    @Field(id = 1, getter = "getPropertyName")
    private final String mPropertyName;

    @Nullable
    @Field(id = 2, getter = "getStringValues")
    private final String[] mStringValues;

    @Nullable
    @Field(id = 3, getter = "getLongValues")
    private final long[] mLongValues;

    @Nullable
    @Field(id = 4, getter = "getDoubleValues")
    private final double[] mDoubleValues;

    @Nullable
    @Field(id = 5, getter = "getBooleanValues")
    private final boolean[] mBooleanValues;

    @Nullable
    @Field(id = 6, getter = "getBytesValues")
    private final byte[][] mBytesValues;

    @Nullable
    @Field(id = 7, getter = "getDocumentValues")
    private final GenericDocumentParcel[] mDocumentValues;

    @Nullable private Integer mHashCode;

    @Constructor
    PropertyParcel(
            @Param(id = 1) @NonNull String propertyName,
            @Param(id = 2) @Nullable String[] stringValues,
            @Param(id = 3) @Nullable long[] longValues,
            @Param(id = 4) @Nullable double[] doubleValues,
            @Param(id = 5) @Nullable boolean[] booleanValues,
            @Param(id = 6) @Nullable byte[][] bytesValues,
            @Param(id = 7) @Nullable GenericDocumentParcel[] documentValues) {
        mPropertyName = Objects.requireNonNull(propertyName);
        mStringValues = stringValues;
        mLongValues = longValues;
        mDoubleValues = doubleValues;
        mBooleanValues = booleanValues;
        mBytesValues = bytesValues;
        mDocumentValues = documentValues;
        checkOnlyOneArrayCanBeSet();
    }

    /** Returns the name of the property. */
    @NonNull
    public String getPropertyName() {
        return mPropertyName;
    }

    /** Returns {@code String} values in an array. */
    @Nullable
    public String[] getStringValues() {
        return mStringValues;
    }

    /** Returns {@code long} values in an array. */
    @Nullable
    public long[] getLongValues() {
        return mLongValues;
    }

    /** Returns {@code double} values in an array. */
    @Nullable
    public double[] getDoubleValues() {
        return mDoubleValues;
    }

    /** Returns {@code boolean} values in an array. */
    @Nullable
    public boolean[] getBooleanValues() {
        return mBooleanValues;
    }

    /** Returns a two-dimension {@code byte} array. */
    @Nullable
    public byte[][] getBytesValues() {
        return mBytesValues;
    }

    /** Returns {@link GenericDocumentParcel}s in an array. */
    @Nullable
    public GenericDocumentParcel[] getDocumentValues() {
        return mDocumentValues;
    }

    /**
     * Returns the held values in an array for this property.
     *
     * <p>Different from other getter methods, this one will return an {@link Object}.
     */
    @Nullable
    public Object getValues() {
        if (mStringValues != null) {
            return mStringValues;
        }
        if (mLongValues != null) {
            return mLongValues;
        }
        if (mDoubleValues != null) {
            return mDoubleValues;
        }
        if (mBooleanValues != null) {
            return mBooleanValues;
        }
        if (mBytesValues != null) {
            return mBytesValues;
        }
        if (mDocumentValues != null) {
            return mDocumentValues;
        }
        return null;
    }

    /**
     * Checks there is one and only one array can be set for the property.
     *
     * @throws IllegalArgumentException if 0, or more than 1 arrays are set.
     */
    private void checkOnlyOneArrayCanBeSet() {
        int notNullCount = 0;
        if (mStringValues != null) {
            ++notNullCount;
        }
        if (mLongValues != null) {
            ++notNullCount;
        }
        if (mDoubleValues != null) {
            ++notNullCount;
        }
        if (mBooleanValues != null) {
            ++notNullCount;
        }
        if (mBytesValues != null) {
            ++notNullCount;
        }
        if (mDocumentValues != null) {
            ++notNullCount;
        }
        if (notNullCount == 0 || notNullCount > 1) {
            throw new IllegalArgumentException(
                    "One and only one type array can be set in PropertyParcel");
        }
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            int hashCode = 0;
            if (mStringValues != null) {
                hashCode = Arrays.hashCode(mStringValues);
            } else if (mLongValues != null) {
                hashCode = Arrays.hashCode(mLongValues);
            } else if (mDoubleValues != null) {
                hashCode = Arrays.hashCode(mDoubleValues);
            } else if (mBooleanValues != null) {
                hashCode = Arrays.hashCode(mBooleanValues);
            } else if (mBytesValues != null) {
                hashCode = Arrays.deepHashCode(mBytesValues);
            } else if (mDocumentValues != null) {
                hashCode = Arrays.hashCode(mDocumentValues);
            }
            mHashCode = Objects.hash(mPropertyName, hashCode);
        }
        return mHashCode;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyParcel)) {
            return false;
        }
        PropertyParcel otherPropertyParcel = (PropertyParcel) other;
        if (!mPropertyName.equals(otherPropertyParcel.mPropertyName)) {
            return false;
        }
        return Arrays.equals(mStringValues, otherPropertyParcel.mStringValues)
                && Arrays.equals(mLongValues, otherPropertyParcel.mLongValues)
                && Arrays.equals(mDoubleValues, otherPropertyParcel.mDoubleValues)
                && Arrays.equals(mBooleanValues, otherPropertyParcel.mBooleanValues)
                && Arrays.deepEquals(mBytesValues, otherPropertyParcel.mBytesValues)
                && Arrays.equals(mDocumentValues, otherPropertyParcel.mDocumentValues);
    }

    /** Builder for {@link PropertyParcel}. */
    public static final class Builder {
        private String mPropertyName;
        private String[] mStringValues;
        private long[] mLongValues;
        private double[] mDoubleValues;
        private boolean[] mBooleanValues;
        private byte[][] mBytesValues;
        private GenericDocumentParcel[] mDocumentValues;

        public Builder(@NonNull String propertyName) {
            mPropertyName = Objects.requireNonNull(propertyName);
        }

        /** Sets String values. */
        @NonNull
        public Builder setStringValues(@NonNull String[] stringValues) {
            mStringValues = Objects.requireNonNull(stringValues);
            return this;
        }

        /** Sets long values. */
        @NonNull
        public Builder setLongValues(@NonNull long[] longValues) {
            mLongValues = Objects.requireNonNull(longValues);
            return this;
        }

        /** Sets double values. */
        @NonNull
        public Builder setDoubleValues(@NonNull double[] doubleValues) {
            mDoubleValues = Objects.requireNonNull(doubleValues);
            return this;
        }

        /** Sets boolean values. */
        @NonNull
        public Builder setBooleanValues(@NonNull boolean[] booleanValues) {
            mBooleanValues = Objects.requireNonNull(booleanValues);
            return this;
        }

        /** Sets a two dimension byte array. */
        @NonNull
        public Builder setBytesValues(@NonNull byte[][] bytesValues) {
            mBytesValues = Objects.requireNonNull(bytesValues);
            return this;
        }

        /** Sets document values. */
        @NonNull
        public Builder setDocumentValues(@NonNull GenericDocumentParcel[] documentValues) {
            mDocumentValues = Objects.requireNonNull(documentValues);
            return this;
        }

        /** Builds a {@link PropertyParcel}. */
        @NonNull
        public PropertyParcel build() {
            return new PropertyParcel(
                    mPropertyName,
                    mStringValues,
                    mLongValues,
                    mDoubleValues,
                    mBooleanValues,
                    mBytesValues,
                    mDocumentValues);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PropertyParcelCreator.writeToParcel(this, dest, flags);
    }
}
