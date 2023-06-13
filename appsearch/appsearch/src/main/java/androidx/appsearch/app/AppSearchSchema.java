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

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.exceptions.IllegalSchemaException;
import androidx.appsearch.util.BundleUtil;
import androidx.appsearch.util.IndentingStringBuilder;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public final class AppSearchSchema {
    private static final String SCHEMA_TYPE_FIELD = "schemaType";
    private static final String PROPERTIES_FIELD = "properties";

    private final Bundle mBundle;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AppSearchSchema(@NonNull Bundle bundle) {
        Preconditions.checkNotNull(bundle);
        mBundle = bundle;
    }

    /**
     * Returns the {@link Bundle} populated by this builder.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public Bundle getBundle() {
        return mBundle;
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
     * @param builder     the builder to append to.
     */
    private void appendAppSearchSchemaString(@NonNull IndentingStringBuilder builder) {
        Preconditions.checkNotNull(builder);

        builder.append("{\n");
        builder.increaseIndentLevel();
        builder.append("schemaType: \"").append(getSchemaType()).append("\",\n");
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
        return mBundle.getString(SCHEMA_TYPE_FIELD, "");
    }

    /**
     * Returns the list of {@link PropertyConfig}s that are part of this schema.
     *
     * <p>This method creates a new list when called.
     */
    @NonNull
    @SuppressWarnings({"MixedMutabilityReturnType", "deprecation"})
    public List<PropertyConfig> getProperties() {
        ArrayList<Bundle> propertyBundles =
                mBundle.getParcelableArrayList(AppSearchSchema.PROPERTIES_FIELD);
        if (propertyBundles == null || propertyBundles.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertyConfig> ret = new ArrayList<>(propertyBundles.size());
        for (int i = 0; i < propertyBundles.size(); i++) {
            ret.add(PropertyConfig.fromBundle(propertyBundles.get(i)));
        }
        return ret;
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
        return getProperties().equals(otherSchema.getProperties());
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(getSchemaType(), getProperties());
    }

    /** Builder for {@link AppSearchSchema objects}. */
    public static final class Builder {
        private final String mSchemaType;
        private ArrayList<Bundle> mPropertyBundles = new ArrayList<>();
        private final Set<String> mPropertyNames = new ArraySet<>();
        private boolean mBuilt = false;

        /** Creates a new {@link AppSearchSchema.Builder}. */
        public Builder(@NonNull String schemaType) {
            Preconditions.checkNotNull(schemaType);
            mSchemaType = schemaType;
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
            mPropertyBundles.add(propertyConfig.mBundle);
            return this;
        }

        /** Constructs a new {@link AppSearchSchema} from the contents of this builder. */
        @NonNull
        public AppSearchSchema build() {
            Bundle bundle = new Bundle();
            bundle.putString(AppSearchSchema.SCHEMA_TYPE_FIELD, mSchemaType);
            bundle.putParcelableArrayList(AppSearchSchema.PROPERTIES_FIELD, mPropertyBundles);
            mBuilt = true;
            return new AppSearchSchema(bundle);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mPropertyBundles = new ArrayList<>(mPropertyBundles);
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
        static final String NAME_FIELD = "name";
        static final String DATA_TYPE_FIELD = "dataType";
        static final String CARDINALITY_FIELD = "cardinality";

        /**
         * Physical data-types of the contents of the property.
         *
         * <p>NOTE: The integer values of these constants must match the proto enum constants in
         * com.google.android.icing.proto.PropertyConfigProto.DataType.Code.
         *
         * @hide
         */
        @IntDef(value = {
                DATA_TYPE_STRING,
                DATA_TYPE_LONG,
                DATA_TYPE_DOUBLE,
                DATA_TYPE_BOOLEAN,
                DATA_TYPE_BYTES,
                DATA_TYPE_DOCUMENT,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface DataType {}

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_STRING = 1;

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_LONG = 2;

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_DOUBLE = 3;

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public static final int DATA_TYPE_BOOLEAN = 4;

        /**
         * Unstructured BLOB.
         * @hide
         */
        public static final int DATA_TYPE_BYTES = 5;

        /**
         * Indicates that the property is itself a {@link GenericDocument}, making it part of a
         * hierarchical schema. Any property using this DataType MUST have a valid
         * {@link PropertyConfig#getSchemaType}.
         * @hide
         */
        public static final int DATA_TYPE_DOCUMENT = 6;

        /**
         * The cardinality of the property (whether it is required, optional or repeated).
         *
         * <p>NOTE: The integer values of these constants must match the proto enum constants in
         * com.google.android.icing.proto.PropertyConfigProto.Cardinality.Code.
         *
         * @hide
         */
        @IntDef(value = {
                CARDINALITY_REPEATED,
                CARDINALITY_OPTIONAL,
                CARDINALITY_REQUIRED,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface Cardinality {}

        /** Any number of items (including zero) [0...*]. */
        public static final int CARDINALITY_REPEATED = 1;

        /** Zero or one value [0,1]. */
        public static final int CARDINALITY_OPTIONAL = 2;

        /** Exactly one value [1]. */
        public static final int CARDINALITY_REQUIRED = 3;

        final Bundle mBundle;

        @Nullable
        private Integer mHashCode;

        PropertyConfig(@NonNull Bundle bundle) {
            mBundle = Preconditions.checkNotNull(bundle);
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
         * @param builder        the builder to append to.
         */
        void appendPropertyConfigString(@NonNull IndentingStringBuilder builder) {
            Preconditions.checkNotNull(builder);

            builder.append("{\n");
            builder.increaseIndentLevel();
            builder.append("name: \"").append(getName()).append("\",\n");

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
                default:
                    builder.append("dataType: DATA_TYPE_UNKNOWN,\n");
            }
            builder.decreaseIndentLevel();
            builder.append("}");
        }

        /** Returns the name of this property. */
        @NonNull
        public String getName() {
            return mBundle.getString(NAME_FIELD, "");
        }

        /**
         * Returns the type of data the property contains (such as string, int, bytes, etc).
         *
         * @hide
         */
        @DataType
        public int getDataType() {
            return mBundle.getInt(DATA_TYPE_FIELD, -1);
        }

        /**
         * Returns the cardinality of the property (whether it is optional, required or repeated).
         */
        @Cardinality
        public int getCardinality() {
            return mBundle.getInt(CARDINALITY_FIELD, CARDINALITY_OPTIONAL);
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
            return BundleUtil.deepEquals(this.mBundle, otherProperty.mBundle);
        }

        @Override
        public int hashCode() {
            if (mHashCode == null) {
                mHashCode = BundleUtil.deepHashCode(mBundle);
            }
            return mHashCode;
        }

        /**
         * Converts a {@link Bundle} into a {@link PropertyConfig} depending on its internal data
         * type.
         *
         * <p>The bundle is not cloned.
         *
         * @throws IllegalArgumentException if the bundle does no contain a recognized
         * value in its {@code DATA_TYPE_FIELD}.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        public static PropertyConfig fromBundle(@NonNull Bundle propertyBundle) {
            switch (propertyBundle.getInt(PropertyConfig.DATA_TYPE_FIELD)) {
                case PropertyConfig.DATA_TYPE_STRING:
                    return new StringPropertyConfig(propertyBundle);
                case PropertyConfig.DATA_TYPE_LONG:
                    return new LongPropertyConfig(propertyBundle);
                case PropertyConfig.DATA_TYPE_DOUBLE:
                    return new DoublePropertyConfig(propertyBundle);
                case PropertyConfig.DATA_TYPE_BOOLEAN:
                    return new BooleanPropertyConfig(propertyBundle);
                case PropertyConfig.DATA_TYPE_BYTES:
                    return new BytesPropertyConfig(propertyBundle);
                case PropertyConfig.DATA_TYPE_DOCUMENT:
                    return new DocumentPropertyConfig(propertyBundle);
                default:
                    throw new IllegalArgumentException(
                            "Unsupported property bundle of type "
                                    + propertyBundle.getInt(PropertyConfig.DATA_TYPE_FIELD)
                                    + "; contents: " + propertyBundle);
            }
        }
    }

    /** Configuration for a property of type String in a Document. */
    public static final class StringPropertyConfig extends PropertyConfig {
        private static final String INDEXING_TYPE_FIELD = "indexingType";
        private static final String TOKENIZER_TYPE_FIELD = "tokenizerType";
        private static final String JOINABLE_VALUE_TYPE_FIELD = "joinableValueType";
        private static final String DELETION_PROPAGATION_FIELD = "deletionPropagation";

        /**
         * Encapsulates the configurations on how AppSearch should query/index these terms.
         * @hide
         */
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_EXACT_TERMS,
                INDEXING_TYPE_PREFIXES,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {}

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
         * @hide
         */
        @IntDef(value = {
                TOKENIZER_TYPE_NONE,
                TOKENIZER_TYPE_PLAIN,
                TOKENIZER_TYPE_VERBATIM,
                TOKENIZER_TYPE_RFC822
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface TokenizerType {}

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
// @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.VERBATIM_SEARCH)
// @exportToFramework:endStrip()
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
// @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.TOKENIZER_TYPE_RFC822)
// @exportToFramework:endStrip()
        public static final int TOKENIZER_TYPE_RFC822 = 3;

        /**
         * The joinable value type of the property. By setting the appropriate joinable value type
         * for a property, the client can use the property for joining documents from other schema
         * types using Search API (see {@link JoinSpec}).
         * @hide
         */
        // NOTE: The integer values of these constants must match the proto enum constants in
        // com.google.android.icing.proto.JoinableConfig.ValueType.Code.
        @IntDef(value = {
                JOINABLE_VALUE_TYPE_NONE,
                JOINABLE_VALUE_TYPE_QUALIFIED_ID,
        })
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        public @interface JoinableValueType {}

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
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.JOIN_SPEC_AND_QUALIFIED_ID)
        // @exportToFramework:endStrip()
        public static final int JOINABLE_VALUE_TYPE_QUALIFIED_ID = 1;

        StringPropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Returns how the property is indexed. */
        @IndexingType
        public int getIndexingType() {
            return mBundle.getInt(INDEXING_TYPE_FIELD);
        }

        /** Returns how this property is tokenized (split into words). */
        @TokenizerType
        public int getTokenizerType() {
            return mBundle.getInt(TOKENIZER_TYPE_FIELD);
        }

        /**
         * Returns how this property is going to be used to join documents from other schema types.
         */
        @JoinableValueType
        public int getJoinableValueType() {
            return mBundle.getInt(JOINABLE_VALUE_TYPE_FIELD, JOINABLE_VALUE_TYPE_NONE);
        }

        /**
         * Returns whether or not documents in this schema should be deleted when the document
         * referenced by this field is deleted.
         *
         * @see JoinSpec
         * @<!--@exportToFramework:ifJetpack()--><!--@exportToFramework:else()hide-->
         */
        public boolean getDeletionPropagation() {
            return mBundle.getBoolean(DELETION_PROPAGATION_FIELD, false);
        }

        /** Builder for {@link StringPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;
            @IndexingType private int mIndexingType = INDEXING_TYPE_NONE;
            @TokenizerType private int mTokenizerType = TOKENIZER_TYPE_NONE;
            @JoinableValueType private int mJoinableValueType = JOINABLE_VALUE_TYPE_NONE;
            private boolean mDeletionPropagation = false;

            /** Creates a new {@link StringPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
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
            public StringPropertyConfig.Builder setIndexingType(@IndexingType int indexingType) {
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
             * Configures whether or not documents in this schema will be removed when the document
             * referred to by this property is deleted.
             *
             * <p> Requires that a joinable value type is set.
             * @<!--@exportToFramework:ifJetpack()--><!--@exportToFramework:else()hide-->
             */
            @SuppressWarnings("MissingGetterMatchingBuilder")  // getDeletionPropagation
            @NonNull
            // @exportToFramework:startStrip()
            @RequiresFeature(
                    enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                    name = Features.SCHEMA_SET_DELETION_PROPAGATION)
            // @exportToFramework:endStrip()
            public Builder setDeletionPropagation(boolean deletionPropagation) {
                mDeletionPropagation = deletionPropagation;
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
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_STRING);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                bundle.putInt(INDEXING_TYPE_FIELD, mIndexingType);
                bundle.putInt(TOKENIZER_TYPE_FIELD, mTokenizerType);
                bundle.putInt(JOINABLE_VALUE_TYPE_FIELD, mJoinableValueType);
                bundle.putBoolean(DELETION_PROPAGATION_FIELD, mDeletionPropagation);
                return new StringPropertyConfig(bundle);
            }
        }

        /**
         * Appends a debug string for the {@link StringPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link StringPropertyConfig} instance.
         *
         * @param builder        the builder to append to.
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
        private static final String INDEXING_TYPE_FIELD = "indexingType";

        /**
         * Encapsulates the configurations on how AppSearch should query/index these 64-bit
         * integers.
         * @hide
         */
        @IntDef(value = {
                INDEXING_TYPE_NONE,
                INDEXING_TYPE_RANGE
        })
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        public @interface IndexingType {}

        /** Content in this property will not be indexed. */
        public static final int INDEXING_TYPE_NONE = 0;

        /**
         * Content in this property will be indexed and can be fetched via numeric search range
         * query.
         *
         * <p>For example, a property with 1024 should match numeric search range query [0, 2000].
         */
        // @exportToFramework:startStrip()
        @RequiresFeature(
                enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
                name = Features.NUMERIC_SEARCH)
        // @exportToFramework:endStrip()
        public static final int INDEXING_TYPE_RANGE = 1;

        LongPropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Returns how the property is indexed. */
        @IndexingType
        public int getIndexingType() {
            return mBundle.getInt(INDEXING_TYPE_FIELD, INDEXING_TYPE_NONE);
        }

        /** Builder for {@link LongPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;
            @IndexingType private int mIndexingType = INDEXING_TYPE_NONE;

            /** Creates a new {@link LongPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
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
            public LongPropertyConfig.Builder setIndexingType(@IndexingType int indexingType) {
                Preconditions.checkArgumentInRange(
                        indexingType, INDEXING_TYPE_NONE, INDEXING_TYPE_RANGE, "indexingType");
                mIndexingType = indexingType;
                return this;
            }

            /** Constructs a new {@link LongPropertyConfig} from the contents of this builder. */
            @NonNull
            public LongPropertyConfig build() {
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_LONG);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                bundle.putInt(INDEXING_TYPE_FIELD, mIndexingType);
                return new LongPropertyConfig(bundle);
            }
        }

        /**
         * Appends a debug string for the {@link LongPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link LongPropertyConfig} instance.
         *
         * @param builder        the builder to append to.
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
        DoublePropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Builder for {@link DoublePropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link DoublePropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
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
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_DOUBLE);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                return new DoublePropertyConfig(bundle);
            }
        }
    }

    /** Configuration for a property containing a boolean. */
    public static final class BooleanPropertyConfig extends PropertyConfig {
        BooleanPropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Builder for {@link BooleanPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link BooleanPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
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
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_BOOLEAN);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                return new BooleanPropertyConfig(bundle);
            }
        }
    }

    /** Configuration for a property containing a byte array. */
    public static final class BytesPropertyConfig extends PropertyConfig {
        BytesPropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Builder for {@link BytesPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;

            /** Creates a new {@link BytesPropertyConfig.Builder}. */
            public Builder(@NonNull String propertyName) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
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
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_BYTES);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                return new BytesPropertyConfig(bundle);
            }
        }
    }

    /** Configuration for a property containing another Document. */
    public static final class DocumentPropertyConfig extends PropertyConfig {
        private static final String SCHEMA_TYPE_FIELD = "schemaType";
        private static final String INDEX_NESTED_PROPERTIES_FIELD = "indexNestedProperties";

        DocumentPropertyConfig(@NonNull Bundle bundle) {
            super(bundle);
        }

        /** Returns the logical schema-type of the contents of this document property. */
        @NonNull
        public String getSchemaType() {
            return Preconditions.checkNotNull(mBundle.getString(SCHEMA_TYPE_FIELD));
        }

        /**
         * Returns whether fields in the nested document should be indexed according to that
         * document's schema.
         *
         * <p>If false, the nested document's properties are not indexed regardless of its own
         * schema.
         */
        public boolean shouldIndexNestedProperties() {
            return mBundle.getBoolean(INDEX_NESTED_PROPERTIES_FIELD);
        }

        /** Builder for {@link DocumentPropertyConfig}. */
        public static final class Builder {
            private final String mPropertyName;
            private final String mSchemaType;
            @Cardinality private int mCardinality = CARDINALITY_OPTIONAL;
            private boolean mShouldIndexNestedProperties = false;

            /**
             * Creates a new {@link DocumentPropertyConfig.Builder}.
             *
             * @param propertyName The logical name of the property in the schema, which will be
             *                     used as the key for this property in
             *                     {@link GenericDocument.Builder#setPropertyDocument}.
             * @param schemaType The type of documents which will be stored in this property.
             *                   Documents of different types cannot be mixed into a single
             *                   property.
             */
            public Builder(@NonNull String propertyName, @NonNull String schemaType) {
                mPropertyName = Preconditions.checkNotNull(propertyName);
                mSchemaType = Preconditions.checkNotNull(schemaType);
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
             * Configures whether fields in the nested document should be indexed according to that
             * document's schema.
             *
             * <p>If false, the nested document's properties are not indexed regardless of its own
             * schema.
             */
            @CanIgnoreReturnValue
            @NonNull
            public DocumentPropertyConfig.Builder setShouldIndexNestedProperties(
                    boolean indexNestedProperties) {
                mShouldIndexNestedProperties = indexNestedProperties;
                return this;
            }

            /** Constructs a new {@link PropertyConfig} from the contents of this builder. */
            @NonNull
            public DocumentPropertyConfig build() {
                Bundle bundle = new Bundle();
                bundle.putString(NAME_FIELD, mPropertyName);
                bundle.putInt(DATA_TYPE_FIELD, DATA_TYPE_DOCUMENT);
                bundle.putInt(CARDINALITY_FIELD, mCardinality);
                bundle.putBoolean(INDEX_NESTED_PROPERTIES_FIELD, mShouldIndexNestedProperties);
                bundle.putString(SCHEMA_TYPE_FIELD, mSchemaType);
                return new DocumentPropertyConfig(bundle);
            }
        }

        /**
         * Appends a debug string for the {@link DocumentPropertyConfig} instance to the given
         * string builder.
         *
         * <p>This appends fields specific to a {@link DocumentPropertyConfig} instance.
         *
         * @param builder        the builder to append to.
         */
        void appendDocumentPropertyConfigFields(@NonNull IndentingStringBuilder builder) {
            builder
                    .append("shouldIndexNestedProperties: ")
                    .append(shouldIndexNestedProperties())
                    .append(",\n");

            builder.append("schemaType: \"").append(getSchemaType()).append("\",\n");
        }
    }
}
