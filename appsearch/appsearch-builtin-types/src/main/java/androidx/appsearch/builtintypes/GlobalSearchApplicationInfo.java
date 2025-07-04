/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appsearch.builtintypes;

import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS;
import static androidx.appsearch.app.AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassFactoryRegistry;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.SearchResult;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An indexable document class which indicates that an app is capable of producing, or capable of
 * consuming, certain AppSearch schema types.
 *
 * <p>This class represents optional metadata about an app's data interchange, and is not part of
 * the schema.org types that model object schemas.
 *
 * <p>A producer can check for the existence of ready consumers by querying for
 * {@link GlobalSearchApplicationInfo} documents that ask for a type that producer produces.
 * Once a matching {@link androidx.appsearch.app.SearchResult} is retrieved, the producer can set a
 * schema with access permissions granted to the package that owns this
 * {@link GlobalSearchApplicationInfo} and index documents of that type.
 *
 * <p>The package name of the app which is producing or consuming data is not part of this class --
 * it can be obtained from {@link SearchResult#getPackageName()}.
 */
@ExperimentalAppSearchApi
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.VERBATIM_SEARCH)
@Document(name = "builtin:GlobalSearchApplicationInfo")
public final class GlobalSearchApplicationInfo {
    /**
     * Used with {@link #getApplicationType()} to indicate this application is ready and able to
     * index a certain type of data.
     */
    public static final int APPLICATION_TYPE_PRODUCER = 0;

    /**
     * Used with {@link #getApplicationType()} to indicate this application is able and interested
     * in querying a certain type of data.
     */
    public static final int APPLICATION_TYPE_CONSUMER = 1;

    @IntDef(
            value = {
                APPLICATION_TYPE_PRODUCER,
                APPLICATION_TYPE_CONSUMER,
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface ApplicationType {}

    @Document.Namespace private final String mNamespace;

    @Document.Id private final String mId;

    @Document.LongProperty @ApplicationType private final int mApplicationType;

    /**
     * The list of all unprefixed schema types this app can produce (if #mApplicationType ==
     * {@link #APPLICATION_TYPE_PRODUCER}) or consume (if #mApplicationType ==
     * {@link #APPLICATION_TYPE_CONSUMER}).
     */
    @Document.StringProperty(
            indexingType = INDEXING_TYPE_EXACT_TERMS,
            tokenizerType = TOKENIZER_TYPE_VERBATIM)
    private final List<String> mSchemaTypes;

    /** Constructs a new {@link GlobalSearchApplicationInfo}. */
    GlobalSearchApplicationInfo(
            @NonNull String namespace,
            @NonNull String id,
            @ApplicationType int applicationType,
            @NonNull List<String> schemaTypes) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mApplicationType = applicationType;
        mSchemaTypes = Collections.unmodifiableList(schemaTypes);
    }

    /** Returns the namespace (or logical grouping) for this {@link GlobalSearchApplicationInfo}. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier for this {@link GlobalSearchApplicationInfo}. */
    public @NonNull String getId() {
        return mId;
    }

    /**
     * Returns whether this app is interested in either producing or consuming the schema types
     * listed in {@link #getSchemaTypes()}.
     */
    @ApplicationType
    public int getApplicationType() {
        return mApplicationType;
    }

    /**
     * Returns the list of schema type names this app is capable of producing (if {@link
     * #getApplicationType()} is {@link #APPLICATION_TYPE_PRODUCER}) or interested in consuming (if
     * {@link #getApplicationType() is #APPLICATION_TYPE_CONSUMER}).
     */
    public @NonNull List<@NonNull String> getSchemaTypes() {
        return mSchemaTypes;
    }

    /** Builder class for {@link GlobalSearchApplicationInfo}. */
    public static final class Builder {
        private final String mNamespace;
        private final String mId;
        private final @ApplicationType int mApplicationType;
        private List<String> mSchemaTypes = Collections.emptyList();

        /**
         * Constructs a new {@link Builder}.
         *
         * @param namespace       an arbitrary string corresponding to a logical grouping for this
         *                        {@link GlobalSearchApplicationInfo}. All AppSearch documents
         *                        must have a namespace.
         * @param id              the identifier for this GlobalSearchApplicationInfo. AppSearch
         *                        IDs are unique within their combination of package, database and
         *                        namespace.
         * @param applicationType whether the application is capable of producing, or interested in
         *                        consuming, the schema types configured by {@link #setSchemaTypes}
         *                        or {@link #setDocumentClasses}.
         */
        public Builder(
                @NonNull String namespace,
                @NonNull String id,
                @ApplicationType int applicationType) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);
            mApplicationType = Preconditions.checkArgumentInRange(
                    applicationType,
                    APPLICATION_TYPE_PRODUCER,
                    APPLICATION_TYPE_CONSUMER,
                    "applicationType");
        }

        /**
         * Sets the list of schema type names this app is capable of producing (if {@link
         * #getApplicationType()} is {@link #APPLICATION_TYPE_PRODUCER}) or interested in consuming
         * (if {@link #getApplicationType() is #APPLICATION_TYPE_CONSUMER}).
         *
         * <p>This method is equivalent to {@link #setDocumentClasses} but accepts schema name
         * strings instead of extracting schema information from document classes.
         */
        public @NonNull Builder setSchemaTypes(@NonNull List<@NonNull String> schemaTypes) {
            Preconditions.checkNotNull(schemaTypes);
            for (int i = 0; i < schemaTypes.size(); i++) {
                Preconditions.checkNotNull(schemaTypes.get(i));
            }
            mSchemaTypes = schemaTypes;
            return this;
        }

        /**
         * Sets the list of schemas this app is capable of producing (if {@link
         * #getApplicationType()} is {@link #APPLICATION_TYPE_PRODUCER}) or interested in consuming
         * (if {@link #getApplicationType() is #APPLICATION_TYPE_CONSUMER}).
         *
         * <p>This method is equivalent to {@link #setSchemaTypes} but accepts document classes
         * instead of schema name strings.
         *
         * @param schemaClasses A list of classes annotated with the @{@link Document} annotation
         *                      which correspond to the schemas this app produces or consumes.
         * @throws AppSearchException if the schemaClasses are not @{@link Document}-annotated
         *                            classes or extraction of the schema name fails.
         */
        @SuppressWarnings("MissingGetterMatchingBuilder")  // See getSchemaTypes
        public @NonNull Builder setDocumentClasses(@NonNull List<@NonNull Class<?>> schemaClasses)
                throws AppSearchException {
            Preconditions.checkNotNull(schemaClasses);
            DocumentClassFactoryRegistry documentClassFactoryRegistry =
                    DocumentClassFactoryRegistry.getInstance();
            List<String> schemaTypes = new ArrayList<>(schemaClasses.size());
            for (int i = 0; i < schemaClasses.size(); i++) {
                Class<?> schemaClass = Preconditions.checkNotNull(schemaClasses.get(i));
                DocumentClassFactory<?> documentClassFactory =
                        documentClassFactoryRegistry.getOrCreateFactory(schemaClass);
                schemaTypes.add(documentClassFactory.getSchemaName());
            }
            mSchemaTypes = schemaTypes;
            return this;
        }

        /** Constructs a new {@link GlobalSearchApplicationInfo}. */
        public @NonNull GlobalSearchApplicationInfo build() {
            return new GlobalSearchApplicationInfo(mNamespace, mId, mApplicationType, mSchemaTypes);
        }
    }
}
