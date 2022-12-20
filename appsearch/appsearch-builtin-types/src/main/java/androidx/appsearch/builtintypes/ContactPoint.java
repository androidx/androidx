/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.AppSearchSession;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group of contact information corresponding to a label such as "Home" or "Work".
 */
@Document(name = "builtin:ContactPoint")
public class ContactPoint {
    @Document.Namespace
    private final String mNamespace;

    @Document.Id
    private final String mId;

    @Document.Score
    private final int mDocumentScore;

    @Document.CreationTimestampMillis
    private final long mCreationTimestampMillis;

    @Document.TtlMillis
    private final long mDocumentTtlMillis;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES)
    private final String mLabel;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES, name =
            "address")
    private final List<String> mAddresses;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES, name =
            "email")
    private final List<String> mEmails;

    @Document.StringProperty(indexingType = StringPropertyConfig.INDEXING_TYPE_PREFIXES, name =
            "telephone")
    private final List<String> mTelephones;

    ContactPoint(
            @NonNull String namespace,
            @NonNull String id,
            int documentScore,
            long creationTimestampMillis,
            long documentTtlMillis,
            @NonNull String label,
            @NonNull List<String> addresses,
            @NonNull List<String> emails,
            @NonNull List<String> telephones) {
        mNamespace = namespace;
        mId = id;
        mDocumentScore = documentScore;
        mCreationTimestampMillis = creationTimestampMillis;
        mDocumentTtlMillis = documentTtlMillis;
        mLabel = label;
        mAddresses = Collections.unmodifiableList(addresses);
        mEmails = Collections.unmodifiableList(emails);
        mTelephones = Collections.unmodifiableList(telephones);
    }

    /** Returns the namespace of this {@link ContactPoint}. */
    @NonNull
    public String getNamespace() {
        return mNamespace;
    }

    /** Returns the unique identifier of this {@link ContactPoint} in its namespace. */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Returns the user-provided opaque document score of the {@link ContactPoint}, which can be
     * used for ranking using
     * {@link androidx.appsearch.app.SearchSpec.RankingStrategy#RANKING_STRATEGY_DOCUMENT_SCORE}.
     */
    public int getDocumentScore() {
        return mDocumentScore;
    }

    /**
     * Returns the creation timestamp for the current AppSearch entity, in milliseconds using the
     * {@link System#currentTimeMillis()} time base.
     *
     * <p>This timestamp refers to the creation time of the AppSearch entity, not when the
     * document is written into AppSearch.
     *
     * <p>See {@link androidx.appsearch.annotation.Document.CreationTimestampMillis} for more
     * information on creation timestamp.
     */
    public long getCreationTimestampMillis() {
        return mCreationTimestampMillis;
    }

    /**
     * Returns the TTL (time-to-live) of the {@link ContactPoint}, in milliseconds.
     *
     * <p>The TTL is measured against {@link #getCreationTimestampMillis}. At the timestamp of
     * {@code creationTimestampMillis + ttlMillis}, measured in the
     * {@link System#currentTimeMillis} time base, the document will be auto-deleted.
     *
     * <p>The default value is 0, which means the document is permanent and won't be
     * auto-deleted until the app is uninstalled or {@link AppSearchSession#remove} is
     * called.
     */
    public long getDocumentTtlMillis() {
        return mDocumentTtlMillis;
    }

    /**
     * Returns the label of this {@link ContactPoint}.
     *
     * <p>Possible values are "Home", "Work", "Other", or any user defined custom label.
     */
    @NonNull
    public String getLabel() {
        return mLabel;
    }

    /**
     * Returns a list of flattened postal addresses associated with this contact point. For
     * example, "123 Main St, Any town, USA".
     */
    @NonNull
    public List<String> getAddresses() {
        return mAddresses;
    }

    /** Returns a list of the email addresses of this {@link ContactPoint}. */
    @NonNull
    public List<String> getEmails() {
        return mEmails;
    }

    /** Returns a list of the telephone numbers of this {@link ContactPoint}. */
    @NonNull
    public List<String> getTelephones() {
        return mTelephones;
    }

    /** Builder for {@link ContactPoint}. */
    public static final class Builder extends BaseBuiltinTypeBuilder<ContactPoint.Builder> {
        private String mLabel;
        // Initialization to guarantee those won't be null
        private List<String> mAddresses = Collections.emptyList();
        private List<String> mEmails = Collections.emptyList();
        private List<String> mTelephones = Collections.emptyList();

        /**
         * Constructor for {@link ContactPoint.Builder}.
         *
         * @param namespace Namespace for the {@link ContactPoint} Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the {@link ContactPoint} Document. See
         *                  {@link Document.Id}.
         * @param label     Label of this {@link ContactPoint} document. It could be "Home",
         *                  "Work" or anything user defined.
         */
        public Builder(@NonNull String namespace, @NonNull String id, @NonNull String label) {
            super(namespace, id);
            mLabel = Preconditions.checkNotNull(label);
        }

        /**
         * Constructor for {@link Builder} with all the existing values of a {@link ContactPoint}.
         */
        public Builder(@NonNull ContactPoint contactPoint) {
            this(Preconditions.checkNotNull(contactPoint).getNamespace(),
                    contactPoint.getId(),
                    contactPoint.getLabel());
            mDocumentScore = contactPoint.getDocumentScore();
            mCreationTimestampMillis = contactPoint.getCreationTimestampMillis();
            mDocumentTtlMillis = contactPoint.getDocumentTtlMillis();
            mAddresses = contactPoint.getAddresses();
            mEmails = contactPoint.getEmails();
            mTelephones = contactPoint.getTelephones();
        }

        /** Sets the flattened postal addresses. */
        @NonNull
        public Builder setAddresses(@NonNull List<String> addresses) {
            mAddresses = Preconditions.checkNotNull(addresses);
            return this;
        }

        /** Sets the email addresses. */
        @NonNull
        public Builder setEmails(@NonNull List<String> emails) {
            mEmails = Preconditions.checkNotNull(emails);
            return this;
        }

        /** Sets the telephone numbers. */
        @NonNull
        public Builder setTelephones(@NonNull List<String> telephones) {
            mTelephones = Preconditions.checkNotNull(telephones);
            return this;
        }

        /** Builds the {@link ContactPoint}. */
        @NonNull
        public ContactPoint build() {
            return new ContactPoint(
                    /*namespace=*/ mNamespace,
                    /*id=*/ mId,
                    /*documentScore=*/ mDocumentScore,
                    /*creationTimestampMillis=*/ mCreationTimestampMillis,
                    /*documentTtlMillis=*/ mDocumentTtlMillis,
                    /*label=*/ mLabel,
                    /*addresses=*/ new ArrayList<>(mAddresses),
                    /*emails=*/ new ArrayList<>(mEmails),
                    /*telephones=*/ new ArrayList<>(mTelephones));
        }
    }
}
