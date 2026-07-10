/*
 * Copyright (C) 2025 The Android Open Source Project
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

import androidx.annotation.RequiresFeature;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.JoinSpec;
import androidx.appsearch.util.DocumentIdUtil;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an nickname document for another {@link GenericDocument}.
 *
 * <p>A nickname document has an indexed string field and a qualified id type that allows us to add
 * aliases to the document referenced to by the qualified id type. This way we can match that
 * document by a string that does not even appear in the original document.
 *
 * <p>Example: Adding a nickname "config" to the settings {@link MobileApplication} document
 *
 * <pre>{@code
 * // Assume that the MobileApplication schema and Nickname schema have been set and settings is
 * // indexed as a MobileApplication document.
 *
 * // Create the qualified ID for a settings app.
 * String settingsAppQualifiedId = DocumentIdUtil.createQualifiedId(
 *     "android",
 *     "apps-db",
 *     "apps",
 *     "com.android.settings");
 *
 * // Index the "config" nickname document, linking it to the document for settings.
 * Nickname configNickname = new Nickname.Builder("configNs", "configId", settingsAppQualifiedId)
 *     .setAlternateNames(Collections.singletonList("config"))
 *     .build();
 *
 * AppSearchBatchResult<String, Void> putResult = appSearchSession.putAsync(
 *     new PutDocumentsRequest.Builder().addDocuments(configNickname).build()).get();
 *
 * // Assert the nickname has been indexed
 * if (!putResult.isSuccess()) {
 *    return;
 * }
 *
 * SearchSpec nestedSearchSpec = new SearchSpec.Builder()
 *     .addFilterDocumentClass(Nickname.class)
 *     .build();
 *
 * JoinSpec joinSpec = new JoinSpec.Builder("referencedQualifiedId")
 *     .setNestedSearch("config", nestedSearchSpec)
 *     .build();
 *
 * SearchSpec searchSpec = new SearchSpec.Builder()
 *     .addFilterDocumentClass(MobileApplication.class)
 *     .setJoinSpec(joinSpec)
 *     .setRankingStrategy("len(this.childrenRankingSignals()) > 0")
 *     .build();
 *
 * SearchResults searchResults = appSearchSession.search("", searchSpec);
 *
 * // This will contain one SearchResult for the settings MobileApplication document.
 * List<SearchResult> results = searchResults.getNextPageAsync().get();
 * }</pre>
 *
 * @see JoinSpec
 */
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.JOIN_SPEC_AND_QUALIFIED_ID)
@Document(name = "builtin:Nickname")
@ExperimentalAppSearchApi
public class Nickname {

    @Document.Namespace
    private final @NonNull String mNamespace;

    @Document.Id
    private final @NonNull String mId;

    @Document.StringProperty(joinableValueType =
            StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
    private final @NonNull String mReferencedQualifiedId;

    @Document.StringProperty(
            indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES
    )
    private final @NonNull List<String> mAlternateNames;


    /**
     * Constructs the {@link Nickname}.
     *
     * @param namespace The namespace for this document.
     * @param id The appsearch document id for this document.
     * @param alternateNames Alternative names for a document. May be null, in which case an empty
     * list is used. The list must not contain null elements.
     * @param referencedQualifiedId The qualified id of the document these nicknames are for.
     */
    Nickname(
            @NonNull String namespace,
            @NonNull String id,
            @Nullable List<String> alternateNames,
            @NonNull String referencedQualifiedId) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mReferencedQualifiedId = Preconditions.checkNotNull(referencedQualifiedId);
        if (alternateNames == null) {
            mAlternateNames = Collections.emptyList();
        } else {
            mAlternateNames = Collections.unmodifiableList(alternateNames);
        }
    }

    /** Returns the namespace (or logical grouping) for this item. */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the appsearch document id for this item. */
    public @NonNull String getId() {
        return mId;
    }

    /**
     * Returns alternative names for a document. These are indexed. For example, you might have the
     * alternative name "pay" for a wallet app.
     */
    public @NonNull List<String> getAlternateNames() {
        return mAlternateNames;
    }

    /**
     * Returns the qualified id of the document these nicknames are for.
     *
     * <p>A qualified id is a string generated by package, database, namespace, and document id.
     *
     * @see DocumentIdUtil#createQualifiedId
     */
    public @NonNull String getReferencedQualifiedId() {
        return mReferencedQualifiedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nickname)) return false;
        Nickname otherNickname = (Nickname) o;
        return ObjectsCompat.equals(mNamespace, otherNickname.mNamespace)
                && ObjectsCompat.equals(mId, otherNickname.mId)
                && ObjectsCompat.equals(mAlternateNames, otherNickname.mAlternateNames)
                && ObjectsCompat.equals(mReferencedQualifiedId,
                otherNickname.mReferencedQualifiedId);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mNamespace, mId, mAlternateNames, mReferencedQualifiedId);
    }

    /** Builder for {@link Nickname}. */
    public static final class Builder extends BuilderImpl<Builder> {
        /** Constructor for {@link Nickname.Builder}. */
        public Builder(@NonNull String namespace, @NonNull String id,
                @NonNull String referencedQualifiedId) {
            super(namespace, id, referencedQualifiedId);
        }

        /**
         * Constructor for {@link Nickname.Builder} with all the existing values of a {@link
         * Nickname}.
         */
        public Builder(@NonNull Nickname nickname) {
            super(nickname);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> {
        protected final String mNamespace;
        protected final String mId;
        protected final String mReferencedQualifiedId;
        protected List<String> mAlternateNames = new ArrayList<>();
        private boolean mBuilt = false;

        BuilderImpl(@NonNull String namespace, @NonNull String id,
                @NonNull String referencedQualifiedId) {
            mNamespace = Preconditions.checkNotNull(namespace);
            mId = Preconditions.checkNotNull(id);
            mReferencedQualifiedId = Preconditions.checkNotNull(referencedQualifiedId);
        }

        BuilderImpl(@NonNull Nickname nickname) {
            Preconditions.checkNotNull(nickname);
            mNamespace = nickname.mNamespace;
            mId = nickname.mId;
            mReferencedQualifiedId = nickname.mReferencedQualifiedId;
            mAlternateNames = new ArrayList<>(nickname.mAlternateNames);
        }

        /** Sets a list of aliases for the item. */
        public @NonNull T setAlternateNames(@Nullable List<String> alternateNames) {
            resetIfBuilt();
            if (alternateNames == null) {
                mAlternateNames = Collections.emptyList();
            } else {
                mAlternateNames = new ArrayList<>(alternateNames);
            }
            return (T) this;
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        private void resetIfBuilt() {
            if (mBuilt) {
                mAlternateNames = new ArrayList<>(mAlternateNames);
                mBuilt = false;
            }
        }

        /** Builds the {@link Nickname}. */
        public @NonNull Nickname build() {
            mBuilt = true;
            return new Nickname(mNamespace, mId, mAlternateNames, mReferencedQualifiedId);
        }
    }
}
