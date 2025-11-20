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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig.Cardinality;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig.DataType;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.DeletePropagationType;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.JoinableValueType;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TokenizerType;
import androidx.appsearch.safeparcel.stub.StubCreators.DocumentIndexingConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.EmbeddingIndexingConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.IntegerIndexingConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.JoinableConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.PropertyConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.StringIndexingConfigParcelCreator;
import androidx.core.util.ObjectsCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Class to hold property configuration for one property defined in {@link AppSearchSchema}.
 *
 * <p>It is defined as same as PropertyConfigProto for the native code to handle different property
 * types in one class.
 *
 * <p>Currently it can handle String, long, double, boolean, bytes and document type.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SafeParcelable.Class(creator = "PropertyConfigParcelCreator")
public final class PropertyConfigParcel extends AbstractSafeParcelable {
    public static final Parcelable.@NonNull Creator<PropertyConfigParcel> CREATOR =
            new PropertyConfigParcelCreator();

    @Field(id = 1, getter = "getName")
    private final String mName;

    @AppSearchSchema.PropertyConfig.DataType
    @Field(id = 2, getter = "getDataType")
    private final int mDataType;

    @AppSearchSchema.PropertyConfig.Cardinality
    @Field(id = 3, getter = "getCardinality")
    private final int mCardinality;

    @Field(id = 4, getter = "getSchemaType")
    private final @Nullable String mSchemaType;

    @Field(id = 5, getter = "getStringIndexingConfigParcel")
    private final @Nullable StringIndexingConfigParcel mStringIndexingConfigParcel;

    @Field(id = 6, getter = "getDocumentIndexingConfigParcel")
    private final @Nullable DocumentIndexingConfigParcel mDocumentIndexingConfigParcel;

    @Field(id = 7, getter = "getIntegerIndexingConfigParcel")
    private final @Nullable IntegerIndexingConfigParcel mIntegerIndexingConfigParcel;

    @Field(id = 8, getter = "getJoinableConfigParcel")
    private final @Nullable JoinableConfigParcel mJoinableConfigParcel;

    @Field(id = 9, getter = "getDescription")
    private final String mDescription;

    @Field(id = 10, getter = "getEmbeddingIndexingConfigParcel")
    @Nullable
    private final EmbeddingIndexingConfigParcel mEmbeddingIndexingConfigParcel;

    @Field(id = 11, getter = "isScoringEnabled")
    private final boolean mScoringEnabled;

    private @Nullable Integer mHashCode;

    /** Constructor for {@link PropertyConfigParcel}. */
    @Constructor
    PropertyConfigParcel(
            @Param(id = 1) @NonNull String name,
            @Param(id = 2) @DataType int dataType,
            @Param(id = 3) @Cardinality int cardinality,
            @Param(id = 4) @Nullable String schemaType,
            @Param(id = 5) @Nullable StringIndexingConfigParcel stringIndexingConfigParcel,
            @Param(id = 6) @Nullable DocumentIndexingConfigParcel documentIndexingConfigParcel,
            @Param(id = 7) @Nullable IntegerIndexingConfigParcel integerIndexingConfigParcel,
            @Param(id = 8) @Nullable JoinableConfigParcel joinableConfigParcel,
            @Param(id = 9) @NonNull String description,
            @Param(id = 10) @Nullable EmbeddingIndexingConfigParcel embeddingIndexingConfigParcel,
            @Param(id = 11) boolean scoringEnabled) {
        mName = Objects.requireNonNull(name);
        mDataType = dataType;
        mCardinality = cardinality;
        mSchemaType = schemaType;
        mStringIndexingConfigParcel = stringIndexingConfigParcel;
        mDocumentIndexingConfigParcel = documentIndexingConfigParcel;
        mIntegerIndexingConfigParcel = integerIndexingConfigParcel;
        mJoinableConfigParcel = joinableConfigParcel;
        mDescription = Objects.requireNonNull(description);
        mEmbeddingIndexingConfigParcel = embeddingIndexingConfigParcel;
        mScoringEnabled = scoringEnabled;
    }

    /** Creates a {@link PropertyConfigParcel} for String. */
    public static @NonNull PropertyConfigParcel createForString(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            @NonNull StringIndexingConfigParcel stringIndexingConfigParcel,
            @NonNull JoinableConfigParcel joinableConfigParcel) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_STRING,
                cardinality,
                /*schemaType=*/ null,
                Objects.requireNonNull(stringIndexingConfigParcel),
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                Objects.requireNonNull(joinableConfigParcel),
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                /*scoringEnabled=*/false);
    }

    /** Creates a {@link PropertyConfigParcel} for Long. */
    public static @NonNull PropertyConfigParcel createForLong(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            @AppSearchSchema.LongPropertyConfig.IndexingType int indexingType,
            boolean scoringEnabled) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_LONG,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                new IntegerIndexingConfigParcel(indexingType),
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                scoringEnabled);
    }

    /** Creates a {@link PropertyConfigParcel} for Double. */
    public static @NonNull PropertyConfigParcel createForDouble(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            boolean scoringEnabled) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                scoringEnabled);
    }

    /** Creates a {@link PropertyConfigParcel} for Boolean. */
    public static @NonNull PropertyConfigParcel createForBoolean(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            boolean scoringEnabled) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                scoringEnabled);
    }

    /** Creates a {@link PropertyConfigParcel} for Bytes. */
    public static @NonNull PropertyConfigParcel createForBytes(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                /*scoringEnabled=*/false);
    }

    /** Creates a {@link PropertyConfigParcel} for Document. */
    public static @NonNull PropertyConfigParcel createForDocument(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            @NonNull String schemaType,
            @NonNull DocumentIndexingConfigParcel documentIndexingConfigParcel) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_DOCUMENT,
                cardinality,
                Objects.requireNonNull(schemaType),
                /*stringIndexingConfigParcel=*/ null,
                Objects.requireNonNull(documentIndexingConfigParcel),
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                /*scoringEnabled=*/false);
    }

    /** Creates a {@link PropertyConfigParcel} for Embedding. */
    public static @NonNull PropertyConfigParcel createForEmbedding(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality,
            @AppSearchSchema.EmbeddingPropertyConfig.IndexingType int indexingType,
            @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType int quantizationType) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_EMBEDDING,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                new EmbeddingIndexingConfigParcel(indexingType, quantizationType),
                /*scoringEnabled=*/false);
    }

    /** Creates a {@link PropertyConfigParcel} for {@link AppSearchBlobHandle}. */
    public static @NonNull PropertyConfigParcel createForBlobHandle(
            @NonNull String propertyName,
            @NonNull String description,
            @Cardinality int cardinality) {
        return new PropertyConfigParcel(
                Objects.requireNonNull(propertyName),
                AppSearchSchema.PropertyConfig.DATA_TYPE_BLOB_HANDLE,
                cardinality,
                /*schemaType=*/ null,
                /*stringIndexingConfigParcel=*/ null,
                /*documentIndexingConfigParcel=*/ null,
                /*integerIndexingConfigParcel=*/ null,
                /*joinableConfigParcel=*/ null,
                Objects.requireNonNull(description),
                /*embeddingIndexingConfigParcel=*/ null,
                /*scoringEnabled=*/false);
    }

    /** Gets name for the property. */
    public @NonNull String getName() {
        return mName;
    }

    /** Gets description for the property. */
    public @NonNull String getDescription() {
        return mDescription;
    }

    /** Gets data type for the property. */
    @DataType
    public int getDataType() {
        return mDataType;
    }

    /** Gets cardinality for the property. */
    @Cardinality
    public int getCardinality() {
        return mCardinality;
    }

    /** Gets schema type. */
    public @Nullable String getSchemaType() {
        return mSchemaType;
    }

    /** Gets the {@link StringIndexingConfigParcel}. */
    public @Nullable StringIndexingConfigParcel getStringIndexingConfigParcel() {
        return mStringIndexingConfigParcel;
    }

    /** Gets the {@link DocumentIndexingConfigParcel}. */
    public @Nullable DocumentIndexingConfigParcel getDocumentIndexingConfigParcel() {
        return mDocumentIndexingConfigParcel;
    }

    /** Gets the {@link IntegerIndexingConfigParcel}. */
    public @Nullable IntegerIndexingConfigParcel getIntegerIndexingConfigParcel() {
        return mIntegerIndexingConfigParcel;
    }

    /** Gets the {@link JoinableConfigParcel}. */
    public @Nullable JoinableConfigParcel getJoinableConfigParcel() {
        return mJoinableConfigParcel;
    }

    /** Gets the {@link EmbeddingIndexingConfigParcel}. */
    public @Nullable EmbeddingIndexingConfigParcel getEmbeddingIndexingConfigParcel() {
        return mEmbeddingIndexingConfigParcel;
    }

    /** Gets ScorableType for the property. */
    public boolean isScoringEnabled() {
        return mScoringEnabled;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PropertyConfigParcelCreator.writeToParcel(this, dest, flags);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyConfigParcel)) {
            return false;
        }
        PropertyConfigParcel otherProperty = (PropertyConfigParcel) other;
        return ObjectsCompat.equals(mName, otherProperty.mName)
                && Objects.equals(mDescription, otherProperty.mDescription)
                && ObjectsCompat.equals(mDataType, otherProperty.mDataType)
                && ObjectsCompat.equals(mCardinality, otherProperty.mCardinality)
                && ObjectsCompat.equals(mSchemaType, otherProperty.mSchemaType)
                && ObjectsCompat.equals(
                mStringIndexingConfigParcel, otherProperty.mStringIndexingConfigParcel)
                && ObjectsCompat.equals(
                mDocumentIndexingConfigParcel, otherProperty.mDocumentIndexingConfigParcel)
                && ObjectsCompat.equals(
                mIntegerIndexingConfigParcel, otherProperty.mIntegerIndexingConfigParcel)
                && ObjectsCompat.equals(
                mJoinableConfigParcel, otherProperty.mJoinableConfigParcel)
                && ObjectsCompat.equals(
                mEmbeddingIndexingConfigParcel, otherProperty.mEmbeddingIndexingConfigParcel)
                && mScoringEnabled == otherProperty.mScoringEnabled;
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode =
                ObjectsCompat.hash(
                        mName,
                        mDescription,
                        mDataType,
                        mCardinality,
                        mSchemaType,
                        mStringIndexingConfigParcel,
                        mDocumentIndexingConfigParcel,
                        mIntegerIndexingConfigParcel,
                        mJoinableConfigParcel,
                        mEmbeddingIndexingConfigParcel,
                        mScoringEnabled);
        }
        return mHashCode;
    }

    @Override
    public @NonNull String toString() {
        return "{name: " + mName
                + ", description: " + mDescription
                + ", dataType: " + mDataType
                + ", cardinality: " + mCardinality
                + ", schemaType: " + mSchemaType
                + ", stringIndexingConfigParcel: " + mStringIndexingConfigParcel
                + ", documentIndexingConfigParcel: " + mDocumentIndexingConfigParcel
                + ", integerIndexingConfigParcel: " + mIntegerIndexingConfigParcel
                + ", joinableConfigParcel: " + mJoinableConfigParcel
                + ", embeddingIndexingConfigParcel: " + mEmbeddingIndexingConfigParcel
                + ", isScoringEnabled: " + mScoringEnabled
                + "}";
    }

    /** Class to hold join configuration for a String type. */
    @SafeParcelable.Class(creator = "JoinableConfigParcelCreator")
    public static class JoinableConfigParcel extends AbstractSafeParcelable {
        public static final Parcelable.@NonNull Creator<JoinableConfigParcel> CREATOR =
                new JoinableConfigParcelCreator();

        @JoinableValueType
        @Field(id = 1, getter = "getJoinableValueType")
        private final int mJoinableValueType;

        @Field(id = 3, getter = "getDeletePropagationType")
        private final int mDeletePropagationType;

        /** Constructor for {@link JoinableConfigParcel}. */
        @Constructor
        public JoinableConfigParcel(
                @Param(id = 1) @JoinableValueType int joinableValueType,
                @Param(id = 3) @DeletePropagationType int deletePropagationType) {
            mJoinableValueType = joinableValueType;
            mDeletePropagationType = deletePropagationType;
        }

        /** Gets {@link JoinableValueType} of the join. */
        @JoinableValueType
        public int getJoinableValueType() {
            return mJoinableValueType;
        }

        /** Gets {@link DeletePropagationType} of the join. */
        @DeletePropagationType
        public int getDeletePropagationType() {
            return mDeletePropagationType;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            JoinableConfigParcelCreator.writeToParcel(this, dest, flags);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mJoinableValueType, mDeletePropagationType);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof JoinableConfigParcel)) {
                return false;
            }
            JoinableConfigParcel otherObject = (JoinableConfigParcel) other;
            return ObjectsCompat.equals(mJoinableValueType, otherObject.mJoinableValueType)
                    && mDeletePropagationType == otherObject.mDeletePropagationType;
        }

        @Override
        public @NonNull String toString() {
            return "{joinableValueType: " + mJoinableValueType
                    + ", deletePropagationType: " + mDeletePropagationType + "}";
        }
    }

    /** Class to hold configuration a string type. */
    @SafeParcelable.Class(creator = "StringIndexingConfigParcelCreator")
    public static class StringIndexingConfigParcel extends AbstractSafeParcelable {
        public static final Parcelable.@NonNull Creator<StringIndexingConfigParcel> CREATOR =
                new StringIndexingConfigParcelCreator();

        @AppSearchSchema.StringPropertyConfig.IndexingType
        @Field(id = 1, getter = "getIndexingType")
        private final int mIndexingType;

        @TokenizerType
        @Field(id = 2, getter = "getTokenizerType")
        private final int mTokenizerType;

        /** Constructor for {@link StringIndexingConfigParcel}. */
        @Constructor
        public StringIndexingConfigParcel(
                @Param(id = 1) @AppSearchSchema.StringPropertyConfig.IndexingType int indexingType,
                @Param(id = 2) @TokenizerType int tokenizerType) {
            mIndexingType = indexingType;
            mTokenizerType = tokenizerType;
        }

        /** Gets the indexing type for this property. */
        @AppSearchSchema.StringPropertyConfig.IndexingType
        public int getIndexingType() {
            return mIndexingType;
        }

        /** Gets the tokenization type for this property. */
        @TokenizerType
        public int getTokenizerType() {
            return mTokenizerType;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            StringIndexingConfigParcelCreator.writeToParcel(this, dest, flags);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mIndexingType, mTokenizerType);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof StringIndexingConfigParcel)) {
                return false;
            }
            StringIndexingConfigParcel otherObject = (StringIndexingConfigParcel) other;
            return mIndexingType == otherObject.mIndexingType
                    && ObjectsCompat.equals(mTokenizerType, otherObject.mTokenizerType);
        }

        @Override
        public @NonNull String toString() {
            return "{indexingType: " + mIndexingType
                    + ", tokenizerType: " + mTokenizerType + "}";
        }
    }

    /** Class to hold configuration for integer property type. */
    @SafeParcelable.Class(creator = "IntegerIndexingConfigParcelCreator")
    public static class IntegerIndexingConfigParcel extends AbstractSafeParcelable {
        public static final Parcelable.@NonNull Creator<IntegerIndexingConfigParcel> CREATOR =
                new IntegerIndexingConfigParcelCreator();

        @AppSearchSchema.LongPropertyConfig.IndexingType
        @Field(id = 1, getter = "getIndexingType")
        private final int mIndexingType;

        /** Constructor for {@link IntegerIndexingConfigParcel}. */
        @Constructor
        public IntegerIndexingConfigParcel(
                @Param(id = 1) @AppSearchSchema.LongPropertyConfig.IndexingType int indexingType) {
            mIndexingType = indexingType;
        }

        /** Gets the indexing type for this integer property. */
        @AppSearchSchema.LongPropertyConfig.IndexingType
        public int getIndexingType() {
            return mIndexingType;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            IntegerIndexingConfigParcelCreator.writeToParcel(this, dest, flags);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hashCode(mIndexingType);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof IntegerIndexingConfigParcel)) {
                return false;
            }
            IntegerIndexingConfigParcel otherObject = (IntegerIndexingConfigParcel) other;
            return mIndexingType == otherObject.mIndexingType;
        }

        @Override
        public @NonNull String toString() {
            return "{indexingType: " + mIndexingType + "}";
        }
    }

    /** Class to hold configuration for document property type. */
    @SafeParcelable.Class(creator = "DocumentIndexingConfigParcelCreator")
    public static class DocumentIndexingConfigParcel extends AbstractSafeParcelable {
        public static final Parcelable.@NonNull Creator<DocumentIndexingConfigParcel> CREATOR =
                new DocumentIndexingConfigParcelCreator();

        @Field(id = 1, getter = "shouldIndexNestedProperties")
        private final boolean mIndexNestedProperties;

        @Field(id = 2, getter = "getIndexableNestedPropertiesList")
        private final @NonNull List<String> mIndexableNestedPropertiesList;

        /** Constructor for {@link DocumentIndexingConfigParcel}. */
        @Constructor
        public DocumentIndexingConfigParcel(
                @Param(id = 1) boolean indexNestedProperties,
                @Param(id = 2) @NonNull List<String> indexableNestedPropertiesList) {
            mIndexNestedProperties = indexNestedProperties;
            mIndexableNestedPropertiesList = Objects.requireNonNull(indexableNestedPropertiesList);
        }

        /** Nested properties should be indexed. */
        public boolean shouldIndexNestedProperties() {
            return mIndexNestedProperties;
        }

        /** Gets the list for nested property list. */
        public @NonNull List<String> getIndexableNestedPropertiesList() {
            return mIndexableNestedPropertiesList;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            DocumentIndexingConfigParcelCreator.writeToParcel(this, dest, flags);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mIndexNestedProperties, mIndexableNestedPropertiesList);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DocumentIndexingConfigParcel)) {
                return false;
            }
            DocumentIndexingConfigParcel otherObject = (DocumentIndexingConfigParcel) other;
            return ObjectsCompat.equals(mIndexNestedProperties, otherObject.mIndexNestedProperties)
                    && ObjectsCompat.equals(mIndexableNestedPropertiesList,
                    otherObject.mIndexableNestedPropertiesList);
        }

        @Override
        public @NonNull String toString() {
            return "{indexNestedProperties: " + mIndexNestedProperties
                    + ", indexableNestedPropertiesList: " + mIndexableNestedPropertiesList
                    + "}";
        }
    }

    /** Class to hold configuration for embedding property. */
    @SafeParcelable.Class(creator = "EmbeddingIndexingConfigParcelCreator")
    public static class EmbeddingIndexingConfigParcel extends AbstractSafeParcelable {
        public static final Parcelable.@NonNull Creator<EmbeddingIndexingConfigParcel> CREATOR =
                new EmbeddingIndexingConfigParcelCreator();

        @AppSearchSchema.EmbeddingPropertyConfig.IndexingType
        @Field(id = 1, getter = "getIndexingType")
        private final int mIndexingType;

        @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType
        @Field(id = 2, getter = "getQuantizationType")
        private final int mQuantizationType;

        /** Constructor for {@link EmbeddingIndexingConfigParcel}. */
        @Constructor
        public EmbeddingIndexingConfigParcel(
                @Param(id = 1) @AppSearchSchema.EmbeddingPropertyConfig.IndexingType
                int indexingType,
                @Param(id = 2) @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType
                int quantizationType) {
            mIndexingType = indexingType;
            mQuantizationType = quantizationType;
        }

        /** Gets the indexing type for this embedding property. */
        @AppSearchSchema.EmbeddingPropertyConfig.IndexingType
        public int getIndexingType() {
            return mIndexingType;
        }

        /** Gets the quantization type for this embedding property. */
        @AppSearchSchema.EmbeddingPropertyConfig.QuantizationType
        public int getQuantizationType() {
            return mQuantizationType;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            EmbeddingIndexingConfigParcelCreator.writeToParcel(this, dest, flags);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mIndexingType, mQuantizationType);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EmbeddingIndexingConfigParcel)) {
                return false;
            }
            EmbeddingIndexingConfigParcel otherObject = (EmbeddingIndexingConfigParcel) other;
            return mIndexingType == otherObject.mIndexingType
                    && mQuantizationType == otherObject.mQuantizationType;
        }

        @Override
        public @NonNull String toString() {
            return "{indexingType: " + mIndexingType
                    + ", quantizationType: " + mQuantizationType + "}";
        }
    }
}
