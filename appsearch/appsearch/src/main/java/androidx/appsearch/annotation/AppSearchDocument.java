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

package androidx.appsearch.annotation;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchSchema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a data class known to AppSearch.
 *
 * <p>Each field annotated with {@link Property @Property} will become an AppSearch searchable
 * property. Fields annotated with other annotations included here (like {@link Uri @Uri}) will have
 * the special behaviour described in that annotation. All other members (those which do not have
 * any of these annotations) will be ignored by AppSearch and will not be persisted or set.
 *
 * <p>Each AppSearch field, whether marked by {@link Property @Property} or by one of the other
 * annotations here, must meet at least one the following conditions:
 * <ol>
 *     <li>There must be a getter named get&lt;Fieldname&gt; in the class (with package-private
 *     visibility or greater), or
 *     <li>The field itself must have package-private visibility or greater.
 * </ol>
 *
 * <p>The field must also meet at least one of the following conditions:
 * <ol>
 *     <li>There must be a setter named set&lt;Fieldname&gt; in the class (with package-private
 *     visibility or greater), or
 *     <li>The field itself must be mutable (non-final) and have package-private visibility or
 *     greater, or
 *     <li>There must be a constructor that accepts all fields not meeting condition 1. and 2. as
 *     parameters. That constructor must have package-private visibility or greater. It may
 *     also accept fields that do meet conditions 1 and 2, in which case the constructor will be
 *     used to populate those fields instead of methods 1 and 2.
 * </ol>
 *
 * <p>The class must also have exactly one member annotated with {@link Uri @Uri}.
 *
 * @hide
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public @interface AppSearchDocument {
    /**
     * The maximum number of indexed properties a document can have.
     *
     * <p>Indexed properties are properties where the {@link Property#indexingType()} constant is
     * anything other than {@link
     * androidx.appsearch.app.AppSearchSchema.PropertyConfig.IndexingType#INDEXING_TYPE_NONE}.
     */
    int MAX_INDEXED_PROPERTIES = 16;

    /**
     * Marks a member field of a document as the document's URI.
     *
     * <p>Indexing a document with a particular {@link java.net.URI} replaces any existing
     * documents with the same URI in that namespace.
     *
     * <p>A document must have exactly one such field, and it must be of type {@link String} or
     * {@link android.net.Uri}.
     *
     * <p>See the class description of {@link AppSearchDocument} for other requirements (i.e. it
     * must be visible, or have a visible getter and setter, or be exposed through a visible
     * constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Uri {}

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
     * <p>See the class description of {@link AppSearchDocument} for other requirements (i.e. if
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
     * <p>See the class description of {@link AppSearchDocument} for other requirements (i.e. if
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
     * <p>See the class description of {@link AppSearchDocument} for other requirements (i.e. if
     * present it must be visible, or have a visible getter and setter, or be exposed through a
     * visible constructor).
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Score {}

    /**
     * Configures a member field of a class as a property known to Appsearch.
     *
     * <p>Properties contain the document's data. They may be indexed or non-indexed (the default).
     * Only indexed properties can be searched for in queries. There is a limit of {@link
     * AppSearchDocument#MAX_INDEXED_PROPERTIES} indexed properties in one document.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface Property {
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
         * AppSearchSchema.PropertyConfig#TOKENIZER_TYPE_PLAIN} (the field will be tokenized
         * along word boundaries as plain text).
         */
        @AppSearchSchema.PropertyConfig.TokenizerType int tokenizerType()
                default AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN;

        /**
         * Configures how a property should be indexed so that it can be retrieved by queries.
         *
         * <p>If not specified, defaults to {@link
         * AppSearchSchema.PropertyConfig#INDEXING_TYPE_NONE} (the field will not be indexed and
         * cannot be queried).
         */
        @AppSearchSchema.PropertyConfig.IndexingType int indexingType()
                default AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE;

        /**
         * Configures whether this property must be specified for the document to be valid.
         *
         * <p>This attribute does not apply to properties of a repeated type (e.g. a list).
         *
         * <p>Please make sure you understand the consequences of required fields on
         * {@link androidx.appsearch.app.AppSearchManager#setSchema schema migration} before setting
         * this attribute to {@code true}.
         */
        boolean required() default false;
    }

    /**
     * The schema name of this type.
     *
     * <p>This string is the key to which the complete schema definition is associated in the
     * AppSearch database. It can be specified to replace an existing type with a new definition.
     *
     * <p>If not specified, it will be automatically set to the simple name of the annotated class.
     */
    String name() default "";
}
