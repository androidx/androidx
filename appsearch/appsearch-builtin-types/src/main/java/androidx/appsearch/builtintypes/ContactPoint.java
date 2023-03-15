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
import androidx.annotation.Nullable;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group of contact information corresponding to a label such as "Home" or "Work".
 */
@Document(name = "builtin:ContactPoint")
public class ContactPoint extends Thing {
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
            @Nullable String name,
            @Nullable List<String> alternateNames,
            @Nullable String description,
            @Nullable String image,
            @Nullable String url,
            @NonNull List<PotentialAction> potentialActions,
            @NonNull String label,
            @NonNull List<String> addresses,
            @NonNull List<String> emails,
            @NonNull List<String> telephones) {
        super(namespace, id, documentScore, creationTimestampMillis, documentTtlMillis, name,
                alternateNames, description, image, url, potentialActions);
        mLabel = label;
        mAddresses = Collections.unmodifiableList(addresses);
        mEmails = Collections.unmodifiableList(emails);
        mTelephones = Collections.unmodifiableList(telephones);
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
    public static final class Builder extends BuilderImpl<Builder> {
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
            super(namespace, id, label);
        }

        /**
         * Constructor for {@link Builder} with all the existing values of a {@link ContactPoint}.
         */
        public Builder(@NonNull ContactPoint contactPoint) {
            super(contactPoint);
        }
    }

    @SuppressWarnings("unchecked")
    static class BuilderImpl<T extends BuilderImpl<T>> extends Thing.BuilderImpl<T> {
        private String mLabel;
        // Initialization to guarantee those won't be null
        private List<String> mAddresses = Collections.emptyList();
        private List<String> mEmails = Collections.emptyList();
        private List<String> mTelephones = Collections.emptyList();

        BuilderImpl(@NonNull String namespace, @NonNull String id, @NonNull String label) {
            super(namespace, id);
            mLabel = Preconditions.checkNotNull(label);
        }

        BuilderImpl(@NonNull ContactPoint contactPoint) {
            super(new Thing.Builder(contactPoint).build());
            mLabel = contactPoint.getLabel();
            mAddresses = contactPoint.getAddresses();
            mEmails = contactPoint.getEmails();
            mTelephones = contactPoint.getTelephones();
        }

        /** Sets the flattened postal addresses. */
        @NonNull
        public T setAddresses(@NonNull List<String> addresses) {
            mAddresses = Preconditions.checkNotNull(addresses);
            return (T) this;
        }

        /** Sets the email addresses. */
        @NonNull
        public T setEmails(@NonNull List<String> emails) {
            mEmails = Preconditions.checkNotNull(emails);
            return (T) this;
        }

        /** Sets the telephone numbers. */
        @NonNull
        public T setTelephones(@NonNull List<String> telephones) {
            mTelephones = Preconditions.checkNotNull(telephones);
            return (T) this;
        }

        /** Builds the {@link ContactPoint}. */
        @NonNull
        @Override
        public ContactPoint build() {
            return new ContactPoint(
                    /*namespace=*/ mNamespace,
                    /*id=*/ mId,
                    /*documentScore=*/mDocumentScore,
                    /*creationTimestampMillis=*/ mCreationTimestampMillis,
                    /*documentTtlMillis=*/ mDocumentTtlMillis,
                    /*name=*/ mName,
                    /*alternateNames=*/ mAlternateNames,
                    /*description=*/ mDescription,
                    /*image=*/ mImage,
                    /*url=*/ mUrl,
                    /*potentialActions=*/ mPotentialActions,
                    /*label=*/ mLabel,
                    /*addresses=*/ new ArrayList<>(mAddresses),
                    /*emails=*/ new ArrayList<>(mEmails),
                    /*telephones=*/ new ArrayList<>(mTelephones));
        }
    }
}
