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

package androidx.appsearch.safeparcel;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.OptIn;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.annotation.HideInPlatform;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.ExperimentalAppSearchApi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@link SafeParcelable} to hold the value of a property in {@code GenericDocument#mProperties}.
 *
 * <p>This resembles PropertyProto in IcingLib.
 */
@HideInPlatform
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PropertyParcelCreator")
// This won't be used to send data over binder, and we have to use Parcelable for code sync purpose.
@SuppressLint("BanParcelableUsage")
public final class PropertyParcel extends AbstractSafeParcelable implements Parcelable {
    public static final Parcelable.@NonNull Creator<PropertyParcel> CREATOR =
            new PropertyParcelCreator();

    @Field(id = 1, getter = "getPropertyName")
    private final @NonNull String mPropertyName;

    @Field(id = 2, getter = "getStringValues")
    private final String @Nullable [] mStringValues;

    @Field(id = 3, getter = "getLongValues")
    private final long @Nullable [] mLongValues;

    @Field(id = 4, getter = "getDoubleValues")
    private final double @Nullable [] mDoubleValues;

    @Field(id = 5, getter = "getBooleanValues")
    private final boolean @Nullable [] mBooleanValues;

    @Field(id = 6, getter = "getBytesValues")
    private final byte @Nullable [][] mBytesValues;

    @Field(id = 7, getter = "getDocumentValues")
    private final GenericDocumentParcel @Nullable [] mDocumentValues;

    @Field(id = 8, getter = "getEmbeddingValues")
    private final EmbeddingVector @Nullable [] mEmbeddingValues;

    @Field(id = 9, getter = "getBlobHandleValues")
    @ExperimentalAppSearchApi
    private final AppSearchBlobHandle @Nullable [] mBlobHandleValues;

    private @Nullable Integer mHashCode;

    @Constructor
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    PropertyParcel(
            @Param(id = 1) @NonNull String propertyName,
            @Param(id = 2) String @Nullable [] stringValues,
            @Param(id = 3) long @Nullable [] longValues,
            @Param(id = 4) double @Nullable [] doubleValues,
            @Param(id = 5) boolean @Nullable [] booleanValues,
            @Param(id = 6) byte @Nullable [][] bytesValues,
            @Param(id = 7) GenericDocumentParcel @Nullable [] documentValues,
            @Param(id = 8) EmbeddingVector @Nullable [] embeddingValues,
            @Param(id = 9) AppSearchBlobHandle @Nullable [] blobHandleValues) {
        mPropertyName = Objects.requireNonNull(propertyName);
        mStringValues = stringValues;
        mLongValues = longValues;
        mDoubleValues = doubleValues;
        mBooleanValues = booleanValues;
        mBytesValues = bytesValues;
        mDocumentValues = documentValues;
        mEmbeddingValues = embeddingValues;
        mBlobHandleValues = blobHandleValues;
        checkOnlyOneArrayCanBeSet();
    }

    /** Returns the name of the property. */
    public @NonNull String getPropertyName() {
        return mPropertyName;
    }

    /** Returns {@code String} values in an array. */
    public String @Nullable [] getStringValues() {
        return mStringValues;
    }

    /** Returns {@code long} values in an array. */
    public long @Nullable [] getLongValues() {
        return mLongValues;
    }

    /** Returns {@code double} values in an array. */
    public double @Nullable [] getDoubleValues() {
        return mDoubleValues;
    }

    /** Returns {@code boolean} values in an array. */
    public boolean @Nullable [] getBooleanValues() {
        return mBooleanValues;
    }

    /** Returns a two-dimension {@code byte} array. */
    public byte @Nullable [][] getBytesValues() {
        return mBytesValues;
    }

    /** Returns {@link GenericDocumentParcel}s in an array. */
    public GenericDocumentParcel @Nullable [] getDocumentValues() {
        return mDocumentValues;
    }

    /** Returns {@link EmbeddingVector}s in an array. */
    public EmbeddingVector @Nullable [] getEmbeddingValues() {
        return mEmbeddingValues;
    }

    /** Returns {@link AppSearchBlobHandle}s in an array. */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public AppSearchBlobHandle @Nullable [] getBlobHandleValues() {
        return mBlobHandleValues;
    }

    /**
     * Returns the held values in an array for this property.
     *
     * <p>Different from other getter methods, this one will return an {@link Object}.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public @Nullable Object getValues() {
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
        if (mEmbeddingValues != null) {
            return mEmbeddingValues;
        }
        if (mBlobHandleValues != null) {
            return mBlobHandleValues;
        }
        return null;
    }

    /**
     * Checks there is one and only one array can be set for the property.
     *
     * @throws IllegalArgumentException if 0, or more than 1 arrays are set.
     */
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
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
        if (mEmbeddingValues != null) {
            ++notNullCount;
        }
        if (mBlobHandleValues != null) {
            ++notNullCount;
        }
        if (notNullCount == 0 || notNullCount > 1) {
            throw new IllegalArgumentException(
                    "One and only one type array can be set in PropertyParcel");
        }
    }

    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
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
            } else if (mEmbeddingValues != null) {
                hashCode = Arrays.deepHashCode(mEmbeddingValues);
            } else if (mBlobHandleValues != null) {
                hashCode = Arrays.deepHashCode(mBlobHandleValues);
            }
            mHashCode = Objects.hash(mPropertyName, hashCode);
        }
        return mHashCode;
    }

    @Override
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
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
                && Arrays.equals(mDocumentValues, otherPropertyParcel.mDocumentValues)
                && Arrays.deepEquals(mEmbeddingValues, otherPropertyParcel.mEmbeddingValues)
                && Arrays.deepEquals(mBlobHandleValues, otherPropertyParcel.mBlobHandleValues);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PropertyParcelCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link PropertyParcel}. */
    @ExperimentalAppSearchApi
    public static final class Builder {
        private String mPropertyName;
        private String[] mStringValues;
        private long[] mLongValues;
        private double[] mDoubleValues;
        private boolean[] mBooleanValues;
        private byte[][] mBytesValues;
        private GenericDocumentParcel[] mDocumentValues;
        private EmbeddingVector[] mEmbeddingValues;
        private AppSearchBlobHandle[] mBlobHandleValues;

        public Builder(@NonNull String propertyName) {
            mPropertyName = Objects.requireNonNull(propertyName);
        }

        /** Sets String values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setStringValues(String @NonNull [] stringValues) {
            mStringValues = Objects.requireNonNull(stringValues);
            return this;
        }

        /** Sets long values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setLongValues(long @NonNull [] longValues) {
            mLongValues = Objects.requireNonNull(longValues);
            return this;
        }

        /** Sets double values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDoubleValues(double @NonNull [] doubleValues) {
            mDoubleValues = Objects.requireNonNull(doubleValues);
            return this;
        }

        /** Sets boolean values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setBooleanValues(boolean @NonNull [] booleanValues) {
            mBooleanValues = Objects.requireNonNull(booleanValues);
            return this;
        }

        /** Sets a two dimension byte array. */
        @CanIgnoreReturnValue
        public @NonNull Builder setBytesValues(byte @NonNull [][] bytesValues) {
            mBytesValues = Objects.requireNonNull(bytesValues);
            return this;
        }

        /** Sets document values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setDocumentValues(
                GenericDocumentParcel @NonNull [] documentValues) {
            mDocumentValues = Objects.requireNonNull(documentValues);
            return this;
        }

        /** Sets embedding values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setEmbeddingValues(EmbeddingVector @NonNull [] embeddingValues) {
            mEmbeddingValues = Objects.requireNonNull(embeddingValues);
            return this;
        }

        /** Sets {@link AppSearchBlobHandle} values. */
        @CanIgnoreReturnValue
        public @NonNull Builder setBlobHandleValues(
                AppSearchBlobHandle @NonNull [] blobHandleValues) {
            mBlobHandleValues = Objects.requireNonNull(blobHandleValues);
            return this;
        }

        /** Builds a {@link PropertyParcel}. */
        public @NonNull PropertyParcel build() {
            return new PropertyParcel(
                    mPropertyName,
                    mStringValues,
                    mLongValues,
                    mDoubleValues,
                    mBooleanValues,
                    mBytesValues,
                    mDocumentValues,
                    mEmbeddingValues,
                    mBlobHandleValues);
        }
    }
}
