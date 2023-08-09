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
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig.Cardinality;
import androidx.appsearch.app.AppSearchSchema.PropertyConfig.DataType;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.JoinableValueType;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TokenizerType;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.DocumentIndexingConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.IntegerIndexingConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.JoinableConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.PropertyConfigParcelCreator;
import androidx.appsearch.safeparcel.stub.StubCreators.StringIndexingConfigParcelCreator;

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
    @NonNull
    public static final PropertyConfigParcelCreator CREATOR = new PropertyConfigParcelCreator();

    @Field(id = 1, getter = "getName")
    private final String mName;

    @AppSearchSchema.PropertyConfig.DataType
    @Field(id = 2, getter = "getDataType")
    private final int mDataType;

    @AppSearchSchema.PropertyConfig.Cardinality
    @Field(id = 3, getter = "getCardinality")
    private final int mCardinality;

    @Field(id = 4, getter = "getSchemaType")
    private final String mSchemaType;

    @Field(id = 5, getter = "getStringIndexingConfigParcel")
    private final StringIndexingConfigParcel mStringIndexingConfigParcel;

    @Field(id = 6, getter = "getDocumentIndexingConfigParcel")
    private final DocumentIndexingConfigParcel mDocumentIndexingConfigParcel;

    @Field(id = 7, getter = "getIntegerIndexingConfigParcel")
    private final IntegerIndexingConfigParcel mIntegerIndexingConfigParcel;

    @Field(id = 8, getter = "getJoinableConfigParcel")
    private final JoinableConfigParcel mJoinableConfigParcel;

    /** Constructor for {@link PropertyConfigParcel}. */
    @Constructor
    public PropertyConfigParcel(
            @Param(id = 1) @NonNull String name,
            @Param(id = 2) @DataType int dataType,
            @Param(id = 3) @Cardinality int cardinality,
            @Param(id = 4) @Nullable String schemaType,
            @Param(id = 5) @Nullable StringIndexingConfigParcel stringIndexingConfigParcel,
            @Param(id = 6) @Nullable DocumentIndexingConfigParcel documentIndexingConfigParcel,
            @Param(id = 7) @Nullable IntegerIndexingConfigParcel integerIndexingConfigParcel,
            @Param(id = 8) @Nullable JoinableConfigParcel joinableConfigParcel) {
        mName = Objects.requireNonNull(name);
        mDataType = dataType;
        mCardinality = cardinality;
        mSchemaType = schemaType;
        mStringIndexingConfigParcel = stringIndexingConfigParcel;
        mDocumentIndexingConfigParcel = documentIndexingConfigParcel;
        mIntegerIndexingConfigParcel = integerIndexingConfigParcel;
        mJoinableConfigParcel = joinableConfigParcel;
    }

    /** Gets name for the property. */
    @NonNull
    public String getName() {
        return mName;
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
    @Nullable
    public String getSchemaType() {
        return mSchemaType;
    }

    /** Gets the {@link StringIndexingConfigParcel}. */
    @Nullable
    public StringIndexingConfigParcel getStringIndexingConfigParcel() {
        return mStringIndexingConfigParcel;
    }

    /** Gets the {@link DocumentIndexingConfigParcel}. */
    @Nullable
    public DocumentIndexingConfigParcel getDocumentIndexingConfigParcel() {
        return mDocumentIndexingConfigParcel;
    }

    /** Gets the {@link IntegerIndexingConfigParcel}. */
    @Nullable
    public IntegerIndexingConfigParcel getIntegerIndexingConfigParcel() {
        return mIntegerIndexingConfigParcel;
    }

    /** Gets the {@link JoinableConfigParcel}. */
    @Nullable
    public JoinableConfigParcel getJoinableConfigParcel() {
        return mJoinableConfigParcel;
    }

    /** Class to hold join configuration for a String type. */
    @SafeParcelable.Class(creator = "JoinableConfigParcelCreator")
    public static class JoinableConfigParcel extends AbstractSafeParcelable {
        @NonNull
        public static final JoinableConfigParcelCreator CREATOR = new JoinableConfigParcelCreator();

        @JoinableValueType
        @Field(id = 1, getter = "getJoinableValueType")
        private final int mJoinableValueType;

        @Field(id = 2, getter = "getDeletionPropagation")
        private final boolean mDeletionPropagation;

        /** Constructor for {@link JoinableConfigParcel}. */
        @Constructor
        public JoinableConfigParcel(
                @Param(id = 1) @JoinableValueType int joinableValueType,
                @Param(id = 2) boolean deletionPropagation) {
            mJoinableValueType = joinableValueType;
            mDeletionPropagation = deletionPropagation;
        }

        /** Gets {@link JoinableValueType} of the join. */
        @JoinableValueType
        public int getJoinableValueType() {
            return mJoinableValueType;
        }

        /** Gets whether delete will be propagated. */
        public boolean getDeletionPropagation() {
            return mDeletionPropagation;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            JoinableConfigParcelCreator.writeToParcel(this, dest, flags);
        }
    }

    /** Class to hold configuration a string type. */
    @SafeParcelable.Class(creator = "StringIndexingConfigParcelCreator")
    public static class StringIndexingConfigParcel extends AbstractSafeParcelable {
        @NonNull
        public static final StringIndexingConfigParcelCreator CREATOR =
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
    }

    /** Class to hold configuration for integer property type. */
    @SafeParcelable.Class(creator = "IntegerIndexingConfigParcelCreator")
    public static class IntegerIndexingConfigParcel extends AbstractSafeParcelable {
        @NonNull
        public static final IntegerIndexingConfigParcelCreator CREATOR =
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
    }

    /** Class to hold configuration for document property type. */
    @SafeParcelable.Class(creator = "DocumentIndexingConfigParcelCreator")
    public static class DocumentIndexingConfigParcel extends AbstractSafeParcelable {
        @NonNull
        public static final DocumentIndexingConfigParcelCreator CREATOR =
                new DocumentIndexingConfigParcelCreator();

        @Field(id = 1, getter = "shouldIndexNestedProperties")
        private final boolean mIndexNestedProperties;

        @NonNull
        @Field(id = 2, getter = "getIndexableNestedPropertiesList")
        private final List<String> mIndexableNestedPropertiesList;

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
        @NonNull
        public List<String> getIndexableNestedPropertiesList() {
            return mIndexableNestedPropertiesList;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            DocumentIndexingConfigParcelCreator.writeToParcel(this, dest, flags);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        PropertyConfigParcelCreator.writeToParcel(this, dest, flags);
    }
}
