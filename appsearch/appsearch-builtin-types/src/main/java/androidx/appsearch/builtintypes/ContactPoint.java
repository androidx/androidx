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

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

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

    /**
     * Constructor for {@link ContactPoint}.
     *
     * @param builder The builder to construct the {@link ContactPoint} from.
     */
    @ExperimentalAppSearchApi
    public ContactPoint(@NonNull BuilderBase<?> builder) {
        super(builder);
        mLabel = builder.mLabel;
        mAddresses = Collections.unmodifiableList(new ArrayList<>(builder.mAddresses));
        mEmails = Collections.unmodifiableList(new ArrayList<>(builder.mEmails));
        mTelephones = Collections.unmodifiableList(new ArrayList<>(builder.mTelephones));
    }

    /**
     * Returns the label of this {@link ContactPoint}.
     *
     * <p>Possible values are "Home", "Work", "Other", or any user defined custom label.
     */
    public @NonNull String getLabel() {
        return mLabel;
    }

    /**
     * Returns a list of flattened postal addresses associated with this contact point. For
     * example, "123 Main St, Any town, USA".
     */
    public @NonNull List<String> getAddresses() {
        return mAddresses;
    }

    /** Returns a list of the email addresses of this {@link ContactPoint}. */
    public @NonNull List<String> getEmails() {
        return mEmails;
    }

    /** Returns a list of the telephone numbers of this {@link ContactPoint}. */
    public @NonNull List<String> getTelephones() {
        return mTelephones;
    }

    /** Builder for {@link ContactPoint}. */
    @Document.BuilderProducer
    @OptIn(markerClass = ExperimentalAppSearchApi.class)
    public static final class Builder extends BuilderBase<Builder> {
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
    @ExperimentalAppSearchApi
    public static class BuilderBase<T extends BuilderBase<T>> extends Thing.BuilderBase<T> {
        private String mLabel;
        // Initialization to guarantee those won't be null
        private List<String> mAddresses = Collections.emptyList();
        private List<String> mEmails = Collections.emptyList();
        private List<String> mTelephones = Collections.emptyList();

        /**
         * Constructor for {@link ContactPoint.BuilderBase}.
         *
         * @param namespace Namespace for the {@link ContactPoint} Document. See
         *                  {@link Document.Namespace}.
         * @param id        Unique identifier for the {@link ContactPoint} Document. See
         *                  {@link Document.Id}.
         * @param label     Label of this {@link ContactPoint} document. It could be "Home",
         *                  "Work" or anything user defined.
         */
        public BuilderBase(@NonNull String namespace, @NonNull String id,
                @NonNull String label) {
            super(namespace, id);
            mLabel = Preconditions.checkNotNull(label);
        }

        /**
         * Constructor for {@link ContactPoint.BuilderBase} with all the existing values of a
         * {@link ContactPoint}.
         *
         * @param contactPoint The existing {@link ContactPoint} to copy values from.
         */
        public BuilderBase(@NonNull ContactPoint contactPoint) {
            super(contactPoint);
            mLabel = contactPoint.getLabel();
            mAddresses = contactPoint.getAddresses();
            mEmails = contactPoint.getEmails();
            mTelephones = contactPoint.getTelephones();
        }

        /** Sets the flattened postal addresses. */
        public @NonNull T setAddresses(@NonNull List<String> addresses) {
            mAddresses = Preconditions.checkNotNull(addresses);
            return (T) this;
        }

        /** Sets the email addresses. */
        public @NonNull T setEmails(@NonNull List<String> emails) {
            mEmails = Preconditions.checkNotNull(emails);
            return (T) this;
        }

        /** Sets the telephone numbers. */
        public @NonNull T setTelephones(@NonNull List<String> telephones) {
            mTelephones = Preconditions.checkNotNull(telephones);
            return (T) this;
        }

        /** Builds the {@link ContactPoint}. */
        @Override
        public @NonNull ContactPoint build() {
            return new ContactPoint(this);
        }
    }
}
