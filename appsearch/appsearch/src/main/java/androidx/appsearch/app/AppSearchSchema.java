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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.exceptions.IllegalSchemaException;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.appsearch.safeparcel.PropertyConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.DocumentIndexingConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.JoinableConfigParcel;
import androidx.appsearch.safeparcel.PropertyConfigParcel.StringIndexingConfigParcel;
import androidx.appsearch.safeparcel.SafeParcelable;
import androidx.appsearch.safeparcel.stub.StubCreators.AppSearchSchemaCreator;
import androidx.appsearch.util.IndentingStringBuilder;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The AppSearch Schema for a particular type of document.
 *
 * <p>For example, an e-mail message or a music recording could be a schema type.
 *
 * <p>The schema consists of type information, properties, and config (like tokenization type).
 *
 * @see AppSearchSession#setSchemaAsync
 */
@SafeParcelable.Class(creator = "AppSearchSchemaCreator")
@SuppressWarnings("HiddenSuperclass")
public final class AppSearchSchema extends AbstractSafeParcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @NonNull
    public static final Parcelable.Creator<AppSearchSchema> CREATOR = new AppSearchSchemaCreator();

    @Field(id = 1, getter = "getSchemaType")
    private final String mSchemaType;

    @Field(id = 2)
    final List<PropertyConfigParcel> mPropertyConfigParcels;

    @Field(id = 3, getter = "getParentTypes")
    private final List<String> mParentTypes;

    @Field(id = 4, getter = "getDescription")
    private final String mDescription;

    @Constructor
    AppSearchSchema(
            @Param(id = 1) @NonNull String schemaType,
            @Param(id = 2) @NonNull List<PropertyConfigParcel> propertyConfigParcels,
            @Param(id = 3) @NonNull List<String> parentTypes,
            @Param(id = 4) @NonNull String description) {
        mSchemaType = Preconditions.checkNotNull(schemaType);
        mPropertyConfigParcels = Preconditions.checkNotNull(propertyConfigParcels);
        mParentTypes = Preconditions.checkNotNull(parentTypes);
        mDescription = Preconditions.checkNotNull(description);
    }

    @Override
    @NonNull
    public String toString() {
        IndentingStringBuilder stringBuilder = new IndentingStringBuilder();
        appendAppSearchSchemaString(stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * Appends a debugging string for the {@link AppSearchSchema} instance to the given string
     * builder.
     *
     * @param builder the builder to append to.
     */
    private void appendAppSearchSchemaString(@NonNull IndentingStringBuilder builder) {
        Preconditions.checkNotNull(builder);

        builder.append("{\n");
        builder.increaseIndentLevel();
        builder.append("schemaType: \"").append(getSchemaType()).append("\",\n");
        builder.append("description: \"").append(getDescription()).append("\",\n");
        builder.append("properties: [\n");

        AppSearchSchema.PropertyConfig[] sortedProperties = getProperties()
                .toArray(new AppSearchSchema.PropertyConfig[0]);
        Arrays.sort(sortedProperties, (o1, o2) -> o1.getName().compareTo(o2.getName()));

        for (int i = 0; i < sortedProperties.length; i++) {
            AppSearchSchema.PropertyConfig propertyConfig = sortedProperties[i];
            builder.increaseIndentLevel();
            propertyConfig.appendPropertyConfigString(builder);
            if (i != sortedProperties.length - 1) {
                builder.append(",\n");
            }
            builder.decreaseIndentLevel();
        }

        builder.append("\n");
        builder.append("]\n");
        builder.decreaseIndentLevel();
        builder.append("}");
    }

    /** Returns the name of this schema type, such as Email. */
    @NonNull
    public String getSchemaType() {
        return mSchemaType;
    }

    /**
     * Returns a natural language description of this schema type.
     *
     * <p>Ex. The description for an Email type could be "A type of electronic message".
     *
     * <p>This information is purely to help apps consuming this type to understand its semantic
     * meaning. This field has no effect in AppSearch - it is just stored with the AppSearchSchema.
     * If {@link Builder#setDescription} is uncalled, then this method will return an empty string.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
    @NonNull
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the list of {@link PropertyConfig}s that are part of this schema.
     *
     * <p>This method creates a new list when called.
     */
    @NonNull
    @SuppressWarnings({"MixedMutabilityReturnType"})
    public List<PropertyConfig> getProperties() {
        if (mPropertyConfigParcels.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertyConfig> ret = new ArrayList<>(mPropertyConfigParcels.size());
        for (int i = 0; i < mPropertyConfigParcels.size(); i++) {
            ret.add(PropertyConfig.fromParcel(mPropertyConfigParcels.get(i)));
        }
        return ret;
    }

    /**
     * Returns the list of parent types of this schema for polymorphism.
     */
    @FlaggedApi(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
    @NonNull
    public List<String> getParentTypes() {
        return Collections.unmodifiableList(mParentTypes);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AppSearchSchema)) {
            return false;
        }
        AppSearchSchema otherSchema = (AppSearchSchema) other;
        if (!getSchemaType().equals(otherSchema.getSchemaType())) {
            return false;
        }
        if (!getDescription().equals(otherSchema.getDescription())) {
            return false;
        }
        if (!getParentTypes().equals(otherSchema.getParentTypes())) {
            return false;
        }
        return getProperties().equals(otherSchema.getProperties());
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
            getSchemaType(),
            getProperties(),
            getParentTypes(),
            getDescription());
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @FlaggedApi(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        AppSearchSchemaCreator.writeToParcel(this, dest, flags);
    }

    /** Builder for {@link AppSearchSchema objects}. */
    public static final class Builder {
        private String mSchemaType;
        private String mDescription = "";
        private ArrayList<PropertyConfigParcel> mPropertyConfigParcels = new ArrayList<>();
        private LinkedHashSet<String> mParentTypes = new LinkedHashSet<>();
        private final Set<String> mPropertyNames = new ArraySet<>();
        private boolean mBuilt = false;

        /** Creates a new {@link AppSearchSchema.Builder}. */
        public Builder(@NonNull String schemaType) {
            mSchemaType = Preconditions.checkNotNull(schemaType);
        }

        /**
         * Creates a new {@link AppSearchSchema.Builder} from the given {@link AppSearchSchema}.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        public Builder(@NonNull AppSearchSchema schema) {
            mSchemaType = schema.getSchemaType();
            mDescription = schema.getDescription();
            mPropertyConfigParcels.addAll(schema.mPropertyConfigParcels);
            mParentTypes.addAll(schema.mParentTypes);
            for (int i = 0; i < mPropertyConfigParcels.size(); i++) {
                mPropertyNames.add(mPropertyConfigParcels.get(i).getName());
            }
        }

        /** Sets the schema type name. */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public AppSearchSchema.Builder setSchemaType(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            resetIfBuilt();
            mSchemaType = schemaType;
            return this;
        }

        /**
         * Sets a natural language description of this schema type.
         *
         * <p> For more details about the description field, see {@link
         * AppSearchSchema#getDescription}.
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_SET_DESCRIPTION)
        @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
        @CanIgnoreReturnValue
        @NonNull
        public AppSearchSchema.Builder setDescription(@NonNull String description) {
            Preconditions.checkNotNull(description);
            resetIfBuilt();
            mDescription = description;
            return this;
        }

        /** Adds a property to the given type. */
        @CanIgnoreReturnValue
        @NonNull
        public AppSearchSchema.Builder addProperty(@NonNull PropertyConfig propertyConfig) {
            Preconditions.checkNotNull(propertyConfig);
            resetIfBuilt();
            String name = propertyConfig.getName();
            if (!mPropertyNames.add(name)) {
                throw new IllegalSchemaException("Property defined more than once: " + name);
            }
            mPropertyConfigParcels.add(propertyConfig.mPropertyConfigParcel);
            return this;
        }

        /**
         * Clears all properties from the given type.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public AppSearchSchema.Builder clearProperties() {
            resetIfBuilt();
            mPropertyConfigParcels.clear();
            mPropertyNames.clear();
            return this;
        }

        /**
         * Adds a parent type to the given type for polymorphism, so that the given type will be
         * considered as a subtype of {@code parentSchemaType}.
         *
         * <p>Subtype relations are automatically considered transitive, so callers are only
         * required to provide direct parents. Specifically, if T1 &lt;: T2 and T2 &lt;: T3 are
         * known, then T1 &lt;: T3 will be inferred automatically, where &lt;: is the subtype
         * symbol.
         *
         * <p>Polymorphism is currently supported in the following ways:
         * <ul>
         *     <li>Search filters on a parent type will automatically be extended to the child
         *     types as well. For example, if Artist &lt;: Person, then a search with a filter on
         *     type Person (by calling {@link SearchSpec.Builder#addFilterSchemas}) will also
         *     include documents of type Artist in the search result.
         *     <li>In the projection API, the property paths to project specified for a
         *     parent type will automatically be extended to the child types as well. If both a
         *     parent type and one of its child type are specified in the projection API, the
         *     parent type's paths will be merged into the child's. For more details on
         *     projection, see {@link SearchSpec.Builder#addProjection}.
         *     <li>A document property defined as type U is allowed to be set with a document of
         *     type T, as long as T &lt;: U, but note that index will only be based on the defined
         *     type, which is U. For example, consider a document of type "Company" with a
         *     repeated "employees" field of type "Person". We can add employees of either
         *     type "Person" or type "Artist" or both to this property, as long as "Artist" is a
         *     subtype of "Person". However, the index of the "employees" property will be based
         *     on what's defined in "Person", even for an added document of type "Artist".
         * </ul>
         *
         * <p>Subtypes must meet the following requirements. A violation of the requirements will
         * cause {@link AppSearchSession#setSchemaAsync} to throw an {@link AppSearchException}
         * with the result code of {@link AppSearchResult#RESULT_INVALID_ARGUMENT}. Consider a
         * type Artist and a type Person, and Artist claims to be a subtype of Person, then:
         * <ul>
         *     <li>Every property in Person must have a corresponding property in Artist with the
         *     same name.
         *     <li>Every non-document property in Person must have the same type as the type of
         *     the corresponding property in Artist. For example, if "age" is an integer property
         *     in Person, then "age" must also be an integer property in Artist, instead of a
         *     string.
         *     <li>The schema type of every document property in Artist must be a subtype of the
         *     schema type of the corresponding document property in Person, if such a property
         *     exists in Person. For example, if "awards" is a document property of type Award in
         *     Person, then the type of the "awards" property in Artist must be a subtype of
         *     Award, say ArtAward. Note that every type is a subtype of itself.
         *     <li>Every property in Artist must have a cardinality stricter than or equal to the
         *     cardinality of the corresponding property in Person, if such a property exists in
         *     Person. For example, if "awards" is a property in Person of cardinality OPTIONAL,
         *     then the cardinality of the "awards" property in Artist can only be REQUIRED or
         *     OPTIONAL. Rule: REQUIRED &lt; OPTIONAL &lt; REPEATED.
         *     <li>There are no other enforcements on the corresponding properties in Artist,
         *     such as index type, tokenizer type, etc. These settings can be safely overridden.
         * </ul>
         *
         * <p>A type can be defined to have multiple parents, but it must be compatible with each
         * of its parents based on the above rules. For example, if LocalBusiness is defined as a
         * subtype of both Place and Organization, then the compatibility of LocalBusiness with
         * Place and the compatibility of LocalBusiness with Organization will both be checked.
         */
        @CanIgnoreReturnValue
        @NonNull
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.SCHEMA_ADD_PARENT_TYPE)
        public AppSearchSchema.Builder addParentType(@NonNull String parentSchemaType) {
            Preconditions.checkNotNull(parentSchemaType);
            resetIfBuilt();
            mParentTypes.add(parentSchemaType);
            return this;
        }

        /**
         * Clears all parent types from the given type.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_ADDITIONAL_BUILDER_COPY_CONSTRUCTORS)
        @CanIgnoreReturnValue
        @NonNull
        public AppSearchSchema.Builder clearParentTypes() {
            resetIfBuilt();
            mParentTypes.clear();
            return this;
        }

        /** Constructs a new {@link AppSearchSchema} from the contents of this builder. */
        @NonNull
        public AppSearchSchema build() {
            mBuilt = true;
            return new AppSearchSchema(mSchemaType,
                    mPropertyConfigParcels,
                    new ArrayList<>(mParentTypes),
                    mDescription);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mPropertyConfigParcels = new ArrayList<>(mPropertyConfigParcels);
                mParentTypes = new LinkedHashSet<>(mParentTypes);
                mBuilt = false;
            }
        }
    }

    /**
     * Common configuration for a single property (field) in a Document.
     *
     * <p>For example, an {@code EmailMessage} would be a type and the {@code subject} would be
     * a property.
     */
    public abstract static class PropertyConfig {
        /**
         * Physical data-types of the contents of the property.
         *
         * <p>NOTE: The integer values of these constants must match the proto enum constants in
         * com.google.android.icing.proto.PropertyConfigProto.DataType.Code.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(value = {
                DATA_TYPE_STRING,
                DATA_TYPE_LONG,
                DATA_TYPE_DOUBLE,
                DATA_TYPE_BOOLEAN,
                DATA_TYPE_BYTES,
                DATA_TYPE_DOCUMENT,
                DATA_TYPE_EMBEDDING,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataType {
        }

        /**
         * Constant value for String data type.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_STRING = 1;

        /**
         * Constant value for Long data type.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_LONG = 2;

        /**
         * Constant value for Double data type.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_DOUBLE = 3;

        /**
         * Constant value for Boolean data type.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_BOOLEAN = 4;

        /**
         * Unstructured BLOB.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_BYTES = 5;

        /**
         * Indicates that the property is itself a {@link GenericDocument}, making it part of a
         * hierarchical schema. Any property using this DataType MUST have a valid
         * {@link PropertyConfig#getSchemaType}.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_DOCUMENT = 6;

        /**
         * Indicates that the property is an {@link EmbeddingVector}.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_EMBEDDING = 7;

        /**
         * The cardinality of the property (whether it is required, optional or repeated).
         *
         * <p>NOTE: The integer values of these constants must match the proto enum constants in
         * com.google.android.icing.proto.PropertyConfigProto.Cardinality.Code.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(value = {
                CARDINALITY_REPEATED,
                CARDINALITY_OPTIONAL,
                CARDINALITY_REQUIRED,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Cardinality {
        }

        /** Any number of items (including zero) [0...*]. */
        public static final int CARDINALITY_REPEATED = 1;

        /** Zero or one value [0,1]. */
        public static final int CARDINALITY_OPTIONAL = 2;

        /** Exactly one value [1]. */
        public static final int CARDINALITY_REQUIRED = 3;

        final PropertyConfigParcel mPropertyConfigParcel;

        PropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            mPropertyConfigParcel = Preconditions.checkNotNull(propertyConfigParcel);
        }

        @Override
        @NonNull
        public String toString() {
            IndentingStringBuilder stringBuilder = new IndentingStringBuilder();
            appendPropertyConfigString(stringBuilder);
            return stringBuilder.toString();
        }

        /**
         * Appends a debug string for the {@link AppSearchSchema.PropertyConfig} instance to the
         * given string builder.
         *
         * @param builder the builder to append to.
         */
        void appendPropertyConfigString(@NonNull IndentingStringBuilder builder) {
            Preconditions.checkNotNull(builder);

            builder.append("{\n");
            builder.increaseIndentLevel();
            builder.append("name: \"").append(getName()).append("\",\n");
            builder.append("description: \"").append(getDescription()).append("\",\n");

            if (this instanceof AppSearchSchema.StringPropertyConfig) {
                ((StringPropertyConfig) this)
                        .appendStringPropertyConfigFields(builder);
            } else if (this instanceof AppSearchSchema.DocumentPropertyConfig) {
                ((DocumentPropertyConfig) this)
                        .appendDocumentPropertyConfigFields(builder);
            } else if (this instanceof AppSearchSchema.LongPropertyConfig) {
                ((LongPropertyConfig) this)
                        .appendLongPropertyConfigFields(builder);
            }

            switch (getCardinality()) {
                case AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED:
                    builder.append("cardinality: CARDINALITY_REPEATED,\n");
                    break;
                case AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL:
                    builder.append("cardinality: CARDINALITY_OPTIONAL,\n");
                    break;
                case AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED:
                    builder.append("cardinality: CARDINALITY_REQUIRED,\n");
                    break;
                default:
                    builder.append("cardinality: CARDINALITY_UNKNOWN,\n");
            }

            switch (getDataType()) {
                case AppSearchSchema.PropertyConfig.DATA_TYPE_STRING:
                    builder.append("dataType: DATA_TYPE_STRING,\n");
                    break;
                case AppSearchSchema.PropertyConfig.DATA_TYPE_LONG:
                    builder.append("dataType: DATA_TYPE_LONG,\n");
                    break;
                case AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE:
                    builder.append("dataType: DATA_TYPE_DOUBLE,\n");
                    break;
                case AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN:
                    builder.append("dataType: DATA_TYPE_BOOLEAN,\n");
                    break;
                case AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES:
                    builder.append("dataType: DATA_TYPE_BYTES,\n");
                    break;
                case AppSearchSchema.PropertyConfig.DATA_TYPE_DOCUMENT:
                    builder.append("dataType: DATA_TYPE_DOCUMENT,\n");
                    break;
                case PropertyConfig.DATA_TYPE_EMBEDDING:
                    builder.append("dataType: DATA_TYPE_EMBEDDING,\n");
                    break;
                default:
                    builder.append("dataType: DATA_TYPE_UNKNOWN,\n");
            }
            builder.decreaseIndentLevel();
            builder.append("}");
        }

        /** Returns the name of this property. */
        @NonNull
        public String getName() {
            return mPropertyConfigParcel.getName();
        }

        /**
         * Returns a natural language description of this property.
         *
         * <p>Ex. The description for the "homeAddress" property of a "Person" type could be "the
         * address at which this person lives".
         *
         * <p>This information is purely to help apps consuming this type the semantic meaning of
         * its properties. This field has no effect in AppSearch - it is just stored with the
         * AppSearchSchema. If the description is not set, then this method will return an empty
         * string.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
        @NonNull
        public String getDescription() {
            return mPropertyConfigParcel.getDescription();
        }

        /**
         * Returns the type of data the property contains (such as string, int, bytes, etc).
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @DataType
        public int getDataType() {
            return mPropertyConfigParcel.getDataType();
        }

        /**
         * Returns the cardinality of the property (whether it is optional, required or repeated).
         */
        @Cardinality
        public int getCardinality() {
            return mPropertyConfigParcel.getCardinality();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof PropertyConfig)) {
                return false;
            }
            PropertyConfig otherProperty = (PropertyConfig) other;
            return ObjectsCompat.equals(mPropertyConfigParcel, otherProperty.mPropertyConfigParcel);
        }

        @Override
        public int hashCode() {
            return mPropertyConfigParcel.hashCode();
        }

        /**
         * Converts a {@link Bundle} into a {@link PropertyConfig} depending on its internal data
         * type.
         *
         * <p>The bundle is not cloned.
         *
         * @throws IllegalArgumentException if the bundle does no contain a recognized
         *                                  value in its {@code DATA_TYPE_FIELD}.
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static PropertyConfig fromParcel(
                @NonNull PropertyConfigParcel propertyConfigParcel) {
            Preconditions.checkNotNull(propertyConfigParcel);
            switch (propertyConfigParcel.getDataType()) {
                case PropertyConfig.DATA_TYPE_STRING:
                    return new StringPropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_LONG:
                    return new LongPropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_DOUBLE:
                    return new DoublePropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_BOOLEAN:
                    return new BooleanPropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_BYTES:
                    return new BytesPropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_DOCUMENT:
                    return new DocumentPropertyConfig(propertyConfigParcel);
                case PropertyConfig.DATA_TYPE_EMBEDDING:
                    return new EmbeddingPropertyConfig(propertyConfigParcel);
                default:
                    throw new IllegalArgumentException(
                            "Unsupported property bundle of type "
                                    + propertyConfigParcel.getDataType()
                                    + "; contents: " + propertyConfigParcel);
            }
        }
    }

    /** Configuration for a property of type String in a Document. */
    public static final class StringPropertyConfig extends PropertyConfig {
        /**
         * Encapsulates the configurations on how AppSearch should query/index these terms.
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_EXACT_TERMS,
                INDEXING_TYPE_PREFIXES,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {
        }

        /** Content in this property will not be tokenized or indexed. */
        public static final int INDEXING_TYPE_NONE = 0;

        /**
         * Content in this property should only be returned for queries matching the exact tokens
         * appearing in this property.
         *
         * <p>For example, a property with "fool" should NOT match a query for "foo".
         */
        public static final int INDEXING_TYPE_EXACT_TERMS = 1;

        /**
         * Content in this property should be returned for queries that are either exact matches or
         * query matches of the tokens appearing in this property.
         *
         * <p>For example, a property with "fool" <b>should</b> match a query for "foo".
         */
        public static final int INDEXING_TYPE_PREFIXES = 2;

        /**
         * Configures how tokens should be extracted from this property.
         *
         * <p>NOTE: The integer values of these constants must match the proto enum constants in
         * com.google.android.icing.proto.IndexingConfig.TokenizerType.Code.
         *
         * @exportToFramework:hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(value = {
                TOKENIZER_TYPE_NONE,
                TOKENIZER_TYPE_PLAIN,
                TOKENIZER_TYPE_VERBATIM,
                TOKENIZER_TYPE_RFC822
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface TokenizerType {
        }

        /**
         * This value indicates that no tokens should be extracted from this property.
         *
         * <p>It is only valid for tokenizer_type to be 'NONE' if {@link #getIndexingType} is
         * {@link #INDEXING_TYPE_NONE}.
         */
        public static final int TOKENIZER_TYPE_NONE = 0;

        /**
         * Tokenization for plain text. This value indicates that tokens should be extracted from
         * this property based on word breaks. Segments of whitespace and punctuation are not
         * considered tokens.
         *
         * <p>For example, a property with "foo bar. baz." will produce tokens for "foo", "bar" and
         * "baz". The segments " " and "." will not be considered tokens.
         *
         * <p>It is only valid for tokenizer_type to be 'PLAIN' if {@link #getIndexingType} is
         * {@link #INDEXING_TYPE_EXACT_TERMS} or {@link #INDEXING_TYPE_PREFIXES}.
         */
        public static final int TOKENIZER_TYPE_PLAIN = 1;

        /**
         * This value indicates that no normalization or segmentation should be applied to string
         * values that are tokenized using this type. Therefore, the output token is equivalent
         * to the raw string value.
         *
         * <p>For example, a property with "Hello, world!" will produce the token "Hello, world!",
         * preserving punctuation and capitalization, and not creating separate tokens between the
         * space.
         *
         * <p>It is only valid for tokenizer_type to be 'VERBATIM' if {@link #getIndexingType} is
         * {@link #INDEXING_TYPE_EXACT_TERMS} or {@link #INDEXING_TYPE_PREFIXES}.
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.VERBATIM_SEARCH)
        public static final int TOKENIZER_TYPE_VERBATIM = 2;

        /**
         * Tokenization for emails. This value indicates that tokens should be extracted from
         * this property based on email structure.
         *
         * <p>For example, a property with "alex.sav@google.com" will produce tokens for "alex",
         * "sav", "alex.sav", "google", "com", and "alexsav@google.com"
         *
         * <p>It is only valid for tokenizer_type to be 'RFC822' if {@link #getIndexingType} is
         * {@link #INDEXING_TYPE_EXACT_TERMS} or {@link #INDEXING_TYPE_PREFIXES}.
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.TOKENIZER_TYPE_RFC822)
        public static final int TOKENIZER_TYPE_RFC822 = 3;

        /**
         * The joinable value type of the property. By setting the appropriate joinable value type
         * for a property, the client can use the property for joining documents from other schema
         * types using Search API (see {@link JoinSpec}).
         * @exportToFramework:hide
         */
        // NOTE: The integer values of these constants must match the proto enum constants in
        // com.google.android.icing.proto.JoinableConfig.ValueType.Code.
        @IntDef(value = {
                JOINABLE_VALUE_TYPE_NONE,
                JOINABLE_VALUE_TYPE_QUALIFIED_ID,
        })
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        public @interface JoinableValueType {
        }

        /** Content in this property is not joinable. */
        public static final int JOINABLE_VALUE_TYPE_NONE = 0;

        /**
         * Content in this string property will be used as a qualified id to join documents.
         * <ul>
         *     <li>Qualified id: a unique identifier for a document, and this joinable value type is
         *     similar to primary and foreign key in relational database. See
         *     {@link androidx.appsearch.util.DocumentIdUtil} for more details.
         *     <li>Currently we only support single string joining, so it should only be used with
         *     {@link PropertyConfig#CARDINALITY_OPTIONAL} and
         *     {@link PropertyConfig#CARDINALITY_REQUIRED}.
         * </ul>
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.JOIN_SPEC_AND_QUALIFIED_ID)
        public static final int JOINABLE_VALUE_TYPE_QUALIFIED_ID = 1;

        StringPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Returns how the property is indexed. */
        @StringPropertyConfig.IndexingType
        public int getIndexingType() {
            StringIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getStringIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return INDEXING_TYPE_NONE;
            }

            return indexingConfigParcel.getIndexingType();
        }

        /** Returns how this property is tokenized (split into words). */
        @TokenizerType
        public int getTokenizerType() {
            StringIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getStringIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return TOKENIZER_TYPE_NONE;
            }

            return indexingConfigParcel.getTokenizerType();
        }

        /**
         * Returns how this property is going to be used to join documents from other schema types.
         */
        @JoinableValueType
        public int getJoinableValueType() {
            JoinableConfigParcel joinableConfigParcel = mPropertyConfigParcel
                    .getJoinableConfigParcel();
            if (joinableConfigParcel == null) {
                return JOINABLE_VALUE_TYPE_NONE;
            }

            return joinableConfigParcel.getJoinableValueType();
        }

        /** Builder for {@link StringPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;
            @StringPropertyConfig.IndexingType
            private int mIndexingType = INDEXING_TYPE_NONE;
            @TokenizerType
            private int mTokenizerType = TOKENIZER_TYPE_NONE;
            @JoinableValueType
            private int mJoinableValueType = JOINABLE_VALUE_TYPE_NONE;
            private boolean mDeletionPropagation = false;

            /** Creates a new {@link StringPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public StringPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public StringPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /**
             * Configures how a property should be indexed so that it can be retrieved by queries.
             *
             * <p>If this method is not called, the default indexing type is
             * {@link StringPropertyConfig#INDEXING_TYPE_NONE}, so that it cannot be matched by
             * queries.
             */
            @CanIgnoreReturnValue
            @NonNull
            public StringPropertyConfig.Builder setIndexingType(
                    @StringPropertyConfig.IndexingType int indexingType) {
                Preconditions.checkArgumentInRange(
                        indexingType, INDEXING_TYPE_NONE, INDEXING_TYPE_PREFIXES, "indexingType");
                mIndexingType = indexingType;
                return this;
            }

            /**
             * Configures how this property should be tokenized (split into words).
             *
             * <p>If this method is not called, the default indexing type is
             * {@link StringPropertyConfig#TOKENIZER_TYPE_NONE}, so that it is not tokenized.
             *
             * <p>This method must be called with a value other than
             * {@link StringPropertyConfig#TOKENIZER_TYPE_NONE} if the property is indexed (that is,
             * if {@link #setIndexingType} has been called with a value other than
             * {@link StringPropertyConfig#INDEXING_TYPE_NONE}).
             */
            @CanIgnoreReturnValue
            @NonNull
            public StringPropertyConfig.Builder setTokenizerType(@TokenizerType int tokenizerType) {
                Preconditions.checkArgumentInRange(
                        tokenizerType, TOKENIZER_TYPE_NONE, TOKENIZER_TYPE_RFC822, "tokenizerType");
                mTokenizerType = tokenizerType;
                return this;
            }

            /**
             * Configures how this property should be used as a joining matcher.
             *
             * <p>If this method is not called, the default joinable value type is
             * {@link StringPropertyConfig#JOINABLE_VALUE_TYPE_NONE}, so that it is not joinable.
             *
             * <p>At most, 64 properties can be set as joinable per schema.
             */
            @CanIgnoreReturnValue
            @NonNull
            public StringPropertyConfig.Builder setJoinableValueType(
                    @JoinableValueType int joinableValueType) {
                Preconditions.checkArgumentInRange(
                        joinableValueType,
                        JOINABLE_VALUE_TYPE_NONE,
                        JOINABLE_VALUE_TYPE_QUALIFIED_ID,
                        "joinableValueType");
                mJoinableValueType = joinableValueType;
                return this;
            }

            /**
             * Constructs a new {@link StringPropertyConfig} from the contents of this builder.
             */
            @NonNull
            public StringPropertyConfig build() {
                if (mTokenizerType == TOKENIZER_TYPE_NONE) {
                    Preconditions.checkState(mIndexingType == INDEXING_TYPE_NONE, "Cannot set "
                            + "TOKENIZER_TYPE_NONE with an indexing type other than "
                            + "INDEXING_TYPE_NONE.");
                } else {
                    Preconditions.checkState(mIndexingType != INDEXING_TYPE_NONE, "Cannot set "
                            + "TOKENIZER_TYPE_PLAIN with INDEXING_TYPE_NONE.");
                }
                if (mJoinableValueType == JOINABLE_VALUE_TYPE_QUALIFIED_ID) {
                    Preconditions.checkState(mCardinality != CARDINALITY_REPEATED, "Cannot set "
                            + "JOINABLE_VALUE_TYPE_QUALIFIED_ID with CARDINALITY_REPEATED.");
                } else {
                    Preconditions.checkState(!mDeletionPropagation, "Cannot set deletion "
                            + "propagation without setting a joinable value type");
                }
                PropertyConfigParcel.StringIndexingConfigParcel stringConfigParcel =
                        new StringIndexingConfigParcel(mIndexingType, mTokenizerType);
                JoinableConfigParcel joinableConfigParcel =
                        new JoinableConfigParcel(mJoinableValueType, mDeletionPropagation);
                return new StringPropertyConfig(
                        PropertyConfigParcel.createForString(
                                mPropertyName,
                                mDescription,
                                mCardinality,
                                stringConfigParcel,
                                joinableConfigParcel));
            }
        }

        /**
         * Appends a debug string for the {@link StringPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link StringPropertyConfig} instance.
         *
         * @param builder the builder to append to.
         */
        void appendStringPropertyConfigFields(@NonNull IndentingStringBuilder builder) {
            switch (getIndexingType()) {
                case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE:
                    builder.append("indexingType: INDEXING_TYPE_NONE,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS:
                    builder.append("indexingType: INDEXING_TYPE_EXACT_TERMS,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES:
                    builder.append("indexingType: INDEXING_TYPE_PREFIXES,\n");
                    break;
                default:
                    builder.append("indexingType: INDEXING_TYPE_UNKNOWN,\n");
            }

            switch (getTokenizerType()) {
                case AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE:
                    builder.append("tokenizerType: TOKENIZER_TYPE_NONE,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN:
                    builder.append("tokenizerType: TOKENIZER_TYPE_PLAIN,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM:
                    builder.append("tokenizerType: TOKENIZER_TYPE_VERBATIM,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_RFC822:
                    builder.append("tokenizerType: TOKENIZER_TYPE_RFC822,\n");
                    break;
                default:
                    builder.append("tokenizerType: TOKENIZER_TYPE_UNKNOWN,\n");
            }

            switch (getJoinableValueType()) {
                case AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE:
                    builder.append("joinableValueType: JOINABLE_VALUE_TYPE_NONE,\n");
                    break;
                case AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID:
                    builder.append("joinableValueType: JOINABLE_VALUE_TYPE_QUALIFIED_ID,\n");
                    break;
                default:
                    builder.append("joinableValueType: JOINABLE_VALUE_TYPE_UNKNOWN,\n");
            }
        }
    }

    /** Configuration for a property containing a 64-bit integer. */
    public static final class LongPropertyConfig extends PropertyConfig {
        /**
         * Encapsulates the configurations on how AppSearch should query/index these 64-bit
         * integers.
         *
         * @exportToFramework:hide
         */
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_RANGE
        })
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {
        }

        /** Content in this property will not be indexed. */
        public static final int INDEXING_TYPE_NONE = 0;

        /**
         * Content in this property will be indexed and can be fetched via numeric search range
         * query.
         *
         * <p>For example, a property with 1024 should match numeric search range query [0, 2000].
         */
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.NUMERIC_SEARCH)
        public static final int INDEXING_TYPE_RANGE = 1;

        LongPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Returns how the property is indexed. */
        @LongPropertyConfig.IndexingType
        public int getIndexingType() {
            PropertyConfigParcel.IntegerIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getIntegerIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return INDEXING_TYPE_NONE;
            }
            return indexingConfigParcel.getIndexingType();
        }

        /** Builder for {@link LongPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;
            @LongPropertyConfig.IndexingType
            private int mIndexingType = INDEXING_TYPE_NONE;

            /** Creates a new {@link LongPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public LongPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public LongPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /**
             * Configures how a property should be indexed so that it can be retrieved by queries.
             *
             * <p>If this method is not called, the default indexing type is
             * {@link LongPropertyConfig#INDEXING_TYPE_NONE}, so that it will not be indexed
             * and cannot be matched by queries.
             */
            @CanIgnoreReturnValue
            @NonNull
            public LongPropertyConfig.Builder setIndexingType(
                    @LongPropertyConfig.IndexingType int indexingType) {
                Preconditions.checkArgumentInRange(
                        indexingType, INDEXING_TYPE_NONE, INDEXING_TYPE_RANGE, "indexingType");
                mIndexingType = indexingType;
                return this;
            }

            /** Constructs a new {@link LongPropertyConfig} from the contents of this builder. */
            @NonNull
            public LongPropertyConfig build() {
                return new LongPropertyConfig(
                        PropertyConfigParcel.createForLong(
                                mPropertyName, mDescription, mCardinality, mIndexingType));
            }
        }

        /**
         * Appends a debug string for the {@link LongPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link LongPropertyConfig} instance.
         *
         * @param builder the builder to append to.
         */
        void appendLongPropertyConfigFields(@NonNull IndentingStringBuilder builder) {
            switch (getIndexingType()) {
                case AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE:
                    builder.append("indexingType: INDEXING_TYPE_NONE,\n");
                    break;
                case AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE:
                    builder.append("indexingType: INDEXING_TYPE_RANGE,\n");
                    break;
                default:
                    builder.append("indexingType: INDEXING_TYPE_UNKNOWN,\n");
            }
        }
    }

    /** Configuration for a property containing a double-precision decimal number. */
    public static final class DoublePropertyConfig extends PropertyConfig {
        DoublePropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Builder for {@link DoublePropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link DoublePropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public DoublePropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public DoublePropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /** Constructs a new {@link DoublePropertyConfig} from the contents of this builder. */
            @NonNull
            public DoublePropertyConfig build() {
                return new DoublePropertyConfig(
                        PropertyConfigParcel.createForDouble(
                                mPropertyName, mDescription, mCardinality));
            }
        }
    }

    /** Configuration for a property containing a boolean. */
    public static final class BooleanPropertyConfig extends PropertyConfig {
        BooleanPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Builder for {@link BooleanPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link BooleanPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
              Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public BooleanPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public BooleanPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /** Constructs a new {@link BooleanPropertyConfig} from the contents of this builder. */
            @NonNull
            public BooleanPropertyConfig build() {
                return new BooleanPropertyConfig(
                        PropertyConfigParcel.createForBoolean(
                                mPropertyName, mDescription, mCardinality));
            }
        }
    }

    /** Configuration for a property containing a byte array. */
    public static final class BytesPropertyConfig extends PropertyConfig {
        BytesPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Builder for {@link BytesPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link BytesPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public BytesPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public BytesPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /**
             * Constructs a new {@link BytesPropertyConfig} from the contents of this builder.
             */
            @NonNull
            public BytesPropertyConfig build() {
                return new BytesPropertyConfig(
                        PropertyConfigParcel.createForBytes(
                                mPropertyName, mDescription, mCardinality));
            }
        }
    }

    /** Configuration for a property containing another Document. */
    public static final class DocumentPropertyConfig extends PropertyConfig {
        DocumentPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Returns the logical schema-type of the contents of this document property. */
        @NonNull
        public String getSchemaType() {
            return Preconditions.checkNotNull(mPropertyConfigParcel.getSchemaType());
        }

        /**
         * Returns whether properties in the nested document should be indexed according to that
         * document's schema.
         *
         * <p>If false, the nested document's properties are not indexed regardless of its own
         * schema.
         *
         * @see DocumentPropertyConfig.Builder#addIndexableNestedProperties(Collection) for
         * indexing a subset of properties from the nested document.
         */
        public boolean shouldIndexNestedProperties() {
            DocumentIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getDocumentIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return false;
            }

            return indexingConfigParcel.shouldIndexNestedProperties();
        }

        /**
         * Returns the list of indexable nested properties for the nested document.
         */
        @FlaggedApi(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
        @NonNull
        public List<String> getIndexableNestedProperties() {
            DocumentIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getDocumentIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return Collections.emptyList();
            }

            List<String> indexableNestedPropertiesList =
                    indexingConfigParcel.getIndexableNestedPropertiesList();
            if (indexableNestedPropertiesList == null) {
                return Collections.emptyList();
            }

            return Collections.unmodifiableList(indexableNestedPropertiesList);
        }

        /** Builder for {@link DocumentPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private final String mSchemaType;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;
            private boolean mShouldIndexNestedProperties = false;
            private final Set<String> mIndexableNestedPropertiesList = new ArraySet<>();

            /**
             * Creates a new {@link DocumentPropertyConfig.Builder}.
             *
             * @param propertyName The logical name of the property in the schema, which will be
             *                     used as the key for this property in
             *                     {@link GenericDocument.Builder#setPropertyDocument}.
             * @param schemaType   The type of documents which will be stored in this property.
             *                     Documents of different types cannot be mixed into a single
             *                     property.
             */
            public Builder(@NonNull String propertyName, @NonNull String schemaType) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
                mSchemaType = Preconditions.checkNotNull(schemaType);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public DocumentPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public DocumentPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /**
             * Configures whether properties in the nested document should be indexed according to
             * that document's schema.
             *
             * <p>If false, the nested document's properties are not indexed regardless of its own
             * schema.
             *
             * <p>To index a subset of properties from the nested document, set this to false and
             * use {@link #addIndexableNestedProperties(Collection)}.
             */
            @CanIgnoreReturnValue
            @NonNull
            public DocumentPropertyConfig.Builder setShouldIndexNestedProperties(
                    boolean indexNestedProperties) {
                mShouldIndexNestedProperties = indexNestedProperties;
                return this;
            }

            /**
             * Adds one or more properties for indexing from the nested document property.
             *
             * @see #addIndexableNestedProperties(Collection)
             */
            @FlaggedApi(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
            @CanIgnoreReturnValue
            @NonNull
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES)
            public DocumentPropertyConfig.Builder addIndexableNestedProperties(
                    @NonNull String... indexableNestedProperties) {
                Preconditions.checkNotNull(indexableNestedProperties);
                return addIndexableNestedProperties(Arrays.asList(indexableNestedProperties));
            }

            /**
             * Adds one or more property paths for indexing from the nested document property.
             *
             * @see #addIndexableNestedProperties(Collection)
             */
            @FlaggedApi(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
            @CanIgnoreReturnValue
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES)
            public DocumentPropertyConfig.Builder addIndexableNestedPropertyPaths(
                    @NonNull PropertyPath... indexableNestedPropertyPaths) {
                Preconditions.checkNotNull(indexableNestedPropertyPaths);
                return addIndexableNestedPropertyPaths(Arrays.asList(indexableNestedPropertyPaths));
            }

            /**
             * Adds one or more properties for indexing from the nested document. The added property
             * will be indexed according to that property's indexing configurations in the
             * document's schema definition. All properties in this list will consume a sectionId
             * regardless of its actual indexing config -- this includes properties added that
             * do not actually exist, as well as properties that are not set as indexable in the
             * nested schema type.
             *
             * <p>Input strings should follow the format of the property path for the nested
             * property, with '.' as the path separator. This nested document's property name
             * should not be included in the property path.
             *
             * <p>Ex. Consider an 'Organization' schema type which defines a nested document
             * property 'address' (Address schema type), where Address has a nested document
             * property 'country' (Country schema type with string 'name' property), and a string
             * 'street' property. The 'street' and 'country's name' properties from the 'address'
             * document property can be indexed for the 'Organization' schema type by calling:
             * <pre>{@code
             * OrganizationSchema.addProperty(
             *                 new DocumentPropertyConfig.Builder("address", "Address")
             *                         .addIndexableNestedProperties("street", "country.name")
             *                         .build()).
             * }</pre>
             *
             * <p>{@link DocumentPropertyConfig.Builder#setShouldIndexNestedProperties} is
             * required to be false if any indexable nested property is added this way for the
             * document property. Attempting to build a DocumentPropertyConfig when this is not
             * true throws {@link IllegalArgumentException}.
             */
            @CanIgnoreReturnValue
            @NonNull
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES)
            public DocumentPropertyConfig.Builder addIndexableNestedProperties(
                    @NonNull Collection<String> indexableNestedProperties) {
                Preconditions.checkNotNull(indexableNestedProperties);
                mIndexableNestedPropertiesList.addAll(indexableNestedProperties);
                return this;
            }

            /**
             * Adds one or more property paths for indexing from the nested document property.
             *
             * @see #addIndexableNestedProperties(Collection)
             */
            @FlaggedApi(Flags.FLAG_ENABLE_GET_PARENT_TYPES_AND_INDEXABLE_NESTED_PROPERTIES)
            @CanIgnoreReturnValue
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_ADD_INDEXABLE_NESTED_PROPERTIES)
            public DocumentPropertyConfig.Builder addIndexableNestedPropertyPaths(
                    @NonNull Collection<PropertyPath> indexableNestedPropertyPaths) {
                Preconditions.checkNotNull(indexableNestedPropertyPaths);
                List<PropertyPath> propertyPathList = new ArrayList<>(indexableNestedPropertyPaths);
                for (int i = 0; i < indexableNestedPropertyPaths.size(); i++) {
                    mIndexableNestedPropertiesList.add(propertyPathList.get(i).toString());
                }
                return this;
            }

            /**
             * Constructs a new {@link PropertyConfig} from the contents of this builder.
             *
             * @throws IllegalArgumentException if the provided PropertyConfig sets
             * {@link #shouldIndexNestedProperties()} to true and has one or more properties
             * defined using {@link #addIndexableNestedProperties(Collection)}.
             */
            @NonNull
            public DocumentPropertyConfig build() {
                if (mShouldIndexNestedProperties && !mIndexableNestedPropertiesList.isEmpty()) {
                    throw new IllegalArgumentException(
                            "DocumentIndexingConfig#shouldIndexNestedProperties is required "
                                    + "to be false when one or more indexableNestedProperties are "
                                    + "provided.");
                }
                return new DocumentPropertyConfig(
                        PropertyConfigParcel.createForDocument(
                                mPropertyName,
                                mDescription,
                                mCardinality,
                                mSchemaType,
                                new DocumentIndexingConfigParcel(mShouldIndexNestedProperties,
                                        new ArrayList<>(mIndexableNestedPropertiesList))));
            }
        }

        /**
         * Appends a debug string for the {@link DocumentPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link DocumentPropertyConfig} instance.
         *
         * @param builder the builder to append to.
         */
        void appendDocumentPropertyConfigFields(@NonNull IndentingStringBuilder builder) {
            builder
                    .append("shouldIndexNestedProperties: ")
                    .append(shouldIndexNestedProperties())
                    .append(",\n");

            builder.append("indexableNestedProperties: ")
                    .append(getIndexableNestedProperties())
                    .append(",\n");

            builder.append("schemaType: \"").append(getSchemaType()).append("\",\n");
        }
    }

    /**
     * Configuration for a property of type {@link EmbeddingVector} in a Document.
     */
    @RequiresFeature(
            enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
            name = Features.SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
    public static final class EmbeddingPropertyConfig extends PropertyConfig {
        /**
         * Encapsulates the configurations on how AppSearch should query/index these embedding
         * vectors.
         *
         * @exportToFramework:hide
         */
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_SIMILARITY
        })
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {
        }

        /** Content in this property will not be indexed. */
        public static final int INDEXING_TYPE_NONE = 0;

        /**
         * Embedding vectors in this property will be indexed.
         *
         * <p>The index offers 100% accuracy, but has linear time complexity based on the number
         * of embedding vectors within the index.
         */
        public static final int INDEXING_TYPE_SIMILARITY = 1;

        EmbeddingPropertyConfig(@NonNull PropertyConfigParcel propertyConfigParcel) {
            super(propertyConfigParcel);
        }

        /** Returns how the property is indexed. */
        @EmbeddingPropertyConfig.IndexingType
        public int getIndexingType() {
            PropertyConfigParcel.EmbeddingIndexingConfigParcel indexingConfigParcel =
                    mPropertyConfigParcel.getEmbeddingIndexingConfigParcel();
            if (indexingConfigParcel == null) {
                return INDEXING_TYPE_NONE;
            }
            return indexingConfigParcel.getIndexingType();
        }

        /** Builder for {@link EmbeddingPropertyConfig}. */
        @FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
        public static final class Builder {
            private final String mPropertyName;
            private String mDescription = "";
            @Cardinality
            private int mCardinality = CARDINALITY_OPTIONAL;
            @EmbeddingPropertyConfig.IndexingType
            private int mIndexingType = INDEXING_TYPE_NONE;

            /** Creates a new {@link EmbeddingPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
            }

            /**
             * Sets a natural language description of this property.
             *
             * <p> For more details about the description field, see {@link
             * AppSearchSchema.PropertyConfig#getDescription}.
             */
            @CanIgnoreReturnValue
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DESCRIPTION)
            @FlaggedApi(Flags.FLAG_ENABLE_APP_FUNCTIONS)
            @SuppressWarnings("MissingGetterMatchingBuilder") // getter defined in superclass
            @NonNull
            public EmbeddingPropertyConfig.Builder setDescription(@NonNull String description) {
                mDescription = Preconditions.checkNotNull(description);
                return this;
            }

            /**
             * Sets the cardinality of the property (whether it is optional, required or repeated).
             *
             * <p>If this method is not called, the default cardinality is
             * {@link PropertyConfig#CARDINALITY_OPTIONAL}.
             */
            @CanIgnoreReturnValue
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getter defined in superclass
            @NonNull
            public EmbeddingPropertyConfig.Builder setCardinality(@Cardinality int cardinality) {
                Preconditions.checkArgumentInRange(
                        cardinality, CARDINALITY_REPEATED, CARDINALITY_REQUIRED, "cardinality");
                mCardinality = cardinality;
                return this;
            }

            /**
             * Configures how a property should be indexed so that it can be retrieved by queries.
             *
             * <p>If this method is not called, the default indexing type is
             * {@link EmbeddingPropertyConfig#INDEXING_TYPE_NONE}, so that it will not be indexed
             * and cannot be matched by queries.
             */
            @CanIgnoreReturnValue
            @NonNull
            public EmbeddingPropertyConfig.Builder setIndexingType(
                    @EmbeddingPropertyConfig.IndexingType int indexingType) {
                Preconditions.checkArgumentInRange(
                        indexingType, INDEXING_TYPE_NONE, INDEXING_TYPE_SIMILARITY,
                        "indexingType");
                mIndexingType = indexingType;
                return this;
            }

            /**
             * Constructs a new {@link EmbeddingPropertyConfig} from the contents of this
             * builder.
             */
            @NonNull
            public EmbeddingPropertyConfig build() {
                return new EmbeddingPropertyConfig(
                        PropertyConfigParcel.createForEmbedding(
                                mPropertyName, mDescription, mCardinality, mIndexingType));
            }
        }
    }
}
