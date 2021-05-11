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
// @exportToFramework:skipFile()
package androidx.appsearch.annotation;

import androidx.appsearch.app.AppSearchSchema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an entity known to AppSearch containing a data record.
 *
 * <p>Each field annotated with one of the Property annotations will become an AppSearch searchable
 * property. Fields annotated with other annotations included here (like {@link Id @Id}) will have
 * the special behaviour described in that annotation. All other members (those which do not have
 * any of these annotations) will be ignored by AppSearch and will not be persisted or set.
 *
 * <p>Each AppSearch annotated field must meet at least one the following conditions:
 * <ol>
 *     <li>There must be a getter named get&lt;Fieldname&gt; in the class (with package-private
 *     visibility or greater), or
 *     <li>The field itself must have package-private visibility or greater.
 * </ol>
 *
 * <p>The field must also meet at least one of the following conditions:
 * <ol>
 *     <li>There must be a setter named {@code set<FieldName>(arg)} in the class (with
 *     package-private visibility or greater), or
 *     <li>There must be a setter named {@code fieldname(arg)} in the class (with package-private
 *     visibility or greater), or
 *     <li>The field itself must be mutable (non-final) and have package-private visibility or
 *     greater, or
 *     <li>There must be a constructor that accepts all fields not meeting condition 1. and 2. as
 *     parameters. That constructor must have package-private visibility or greater. It may
 *     also accept fields that do meet conditions 1 and 2, in which case the constructor will be
 *     used to populate those fields instead of methods 1 and 2.
 * </ol>
 *
 * <p>Fields may be named according to any of the following conventions:
 * <ul>
 *   <li>exampleName
 *   <li>mExampleName
 *   <li>_exampleName
 *   <li>exampleName_
 * </ul>
 *
 * <p>In all of the above cases, the default property name will be "exampleName", the allowed
 * getters are {@code getExampleName()} or {@code exampleName()}, the allowed setters are {@code
 * setExampleName(arg)} or {@code exampleName(arg)}, and the expected constructor parameter for
 * the field is "exampleName".
 *
 * <p>The class must also have exactly one member annotated with {@link Id @Id}.
 *
 * <p>Properties contain the document's data. They may be indexed or non-indexed (the default).
 * Only indexed properties can be searched for in queries. There is a limit of
 * {@link androidx.appsearch.app.GenericDocument#getMaxIndexedProperties} indexed properties in
 * one document.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Document {
    /**
     * The schema name of this type.
     *
     * <p>This string is the key to which the complete schema definition is associated in the
     * AppSearch database. It can be specified to replace an existing type with a new definition.
     *
     * <p>If not specified, it will be automatically set to the simple name of the annotated class.
     */
    String name() default "";

    /**
     * Marks a member field of a document as the document's unique identifier (ID).
     *
     * <p>Indexing a document with a particular ID replaces any existing documents with the same
     * ID in that namespace.
     *
     * <p>A document must have exactly one such field, and it must be of type {@link String}.
     *
     * <p>See the class description of {@link Document} for other requirements (i.e. it
     * must be visible, or have a visible getter and setter, or be exposed through a visible
     * constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Id {}

    /**
     * Marks a member field of a document as the document's namespace.
     *
     * <p>The namespace is an arbitrary user-provided string that can be used to group documents
     * during querying or deletion. Indexing a document with a particular ID replaces any existing
     * documents with the same ID in that namespace.
     *
     * <p>A document must have exactly one such field, and it must be of type {@link String}.
     *
     * <p>See the class description of {@link Document} for other requirements (i.e. if
     * present it must be visible, or have a visible getter and setter, or be exposed through a
     * visible constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Namespace {}

    /**
     * Marks a member field of a document as the document's creation timestamp.
     *
     * <p>The creation timestamp is used for document expiry (see {@link TtlMillis}) and as one
     * of the sort options for queries.
     *
     * <p>This field is not required. If not present or not set, the document will be assigned
     * the current timestamp as its creation timestamp.
     *
     * <p>If present, the field must be of type {@code long} or {@link Long}.
     *
     * <p>See the class description of {@link Document} for other requirements (i.e. if
     * present it must be visible, or have a visible getter and setter, or be exposed through a
     * visible constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface CreationTimestampMillis {}

    /**
     * Marks a member field of a document as the document's time-to-live (TTL).
     *
     * <p>The document will be automatically deleted {@link TtlMillis} milliseconds after
     * {@link CreationTimestampMillis}.
     *
     * <p>This field is not required. If not present or not set, the document will never expire.
     *
     * <p>If present, the field must be of type {@code long} or {@link Long}.
     *
     * <p>See the class description of {@link Document} for other requirements (i.e. if
     * present it must be visible, or have a visible getter and setter, or be exposed through a
     * visible constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface TtlMillis {}

    /**
     * Marks a member field of a document as the document's query-independent score.
     *
     * <p>The score is a query-independent measure of the document's quality, relative to other
     * documents of the same type. It is one of the sort options for queries.
     *
     * <p>This field is not required. If not present or not set, the document will have a score
     * of 0.
     *
     * <p>If present, the field must be of type {@code int} or {@link Integer}.
     *
     * <p>See the class description of {@link Document} for other requirements (i.e. if
     * present it must be visible, or have a visible getter and setter, or be exposed through a
     * visible constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Score {}

    /** Configures a string member field of a class as a property known to AppSearch. */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface StringProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures how tokens should be extracted from this property.
         *
         * <p>If not specified, defaults to {@link
         * AppSearchSchema.StringPropertyConfig#TOKENIZER_TYPE_PLAIN} (the field will be tokenized
         * along word boundaries as plain text).
         */
        @AppSearchSchema.StringPropertyConfig.TokenizerType int tokenizerType()
                default AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN;

        /**
         * Configures how a property should be indexed so that it can be retrieved by queries.
         *
         * <p>If not specified, defaults to {@link
         * AppSearchSchema.StringPropertyConfig#INDEXING_TYPE_NONE} (the field will not be indexed
         * and cannot be queried).
         * TODO(b/171857731) renamed to TermMatchType when using String-specific indexing config.
         */
        @AppSearchSchema.StringPropertyConfig.IndexingType int indexingType()
                default AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE;

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /**
     * Configures a member field of a class as a property known to AppSearch.
     *
     * <p>Field's data class is required to be annotated with {@link Document}.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface DocumentProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures whether fields in the nested document should be indexed.
         *
         * <p>If false, the nested document's properties are not indexed regardless of its own
         * schema.
         */
        boolean indexNestedProperties() default false;

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /** Configures a 64-bit integer field of a class as a property known to AppSearch. */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface LongProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /**
     * Configures a double-precision decimal number field of a class as a property known to
     * AppSearch.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface DoubleProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /** Configures a boolean member field of a class as a property known to AppSearch. */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface BooleanProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /** Configures a byte array member field of a class as a property known to AppSearch. */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface BytesProperty {
        /**
         * The name of this property. This string is used to query against this property.
         *
         * <p>If not specified, the name of the field in the code will be used instead.
         */
        String name() default "";

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchSession#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }
}
