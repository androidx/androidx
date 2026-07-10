/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Represents an account entity within the document property.
 */
@Document(name = "builtin:Account")
@ExperimentalAppSearchApi
public class Account {

    @Document.Namespace
    private final @NonNull String mNamespace;

    @Document.Id
    private final @NonNull String mId;

    @Document.StringProperty(tokenizerType = TOKENIZER_TYPE_VERBATIM,
            indexingType = INDEXING_TYPE_EXACT_TERMS)
    private final @NonNull String mAccountType;

    @Document.StringProperty(tokenizerType = TOKENIZER_TYPE_VERBATIM,
            indexingType = INDEXING_TYPE_EXACT_TERMS)
    private final @NonNull String mAccountName;

    @Document.StringProperty(tokenizerType = TOKENIZER_TYPE_VERBATIM,
            indexingType = INDEXING_TYPE_EXACT_TERMS)
    private final @NonNull String mAccountId;

    /**
     * Constructs a new {@code Account} instance.
     *
     * @param namespace   The namespace (or logical grouping) for this item.
     * @param id          The document id of this item.
     * @param accountType The type of the account. This is typically the package name or a unique
     *                    identifier of the app or system that owns and manages this account.
     *                    Example: "com.yourapp.foo".
     * @param accountName The human-readable name of the account.
     * @param accountId   The account identifier. This is the unique ID assigned to the account by
     *                    the service provider (the Authenticator) itself (for example, a
     *                    Service-Specific User ID). It is used for authentication and service
     *                    interaction.
     */
    Account(@NonNull String namespace, @NonNull String id, @NonNull String accountType,
            @NonNull String accountName, @NonNull String accountId) {
        mNamespace = Preconditions.checkNotNull(namespace);
        mId = Preconditions.checkNotNull(id);
        mAccountType = Preconditions.checkNotNull(accountType);
        mAccountName = Preconditions.checkNotNull(accountName);
        mAccountId = Preconditions.checkNotNull(accountId);
    }

    /** Returns the namespace (or logical grouping) for this item.     */
    public @NonNull String getNamespace() {
        return mNamespace;
    }

    /** Returns the appsearch document id for this item. */
    public @NonNull String getId() {
        return mId;
    }

    /** Returns the type of this account.     */
    public @NonNull String getAccountType() {
        return mAccountType;
    }

    /** Returns the human-readable name of this account. */
    public @NonNull String getAccountName() {
        return mAccountName;
    }

    /**  Returns the account identifier. */
    public @NonNull String getAccountId() {
        return mAccountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account otherAccount = (Account) o;
        return Objects.equals(mNamespace, otherAccount.mNamespace)
                && Objects.equals(mId, otherAccount.mId)
                && Objects.equals(mAccountType, otherAccount.mAccountType)
                && Objects.equals(mAccountName, otherAccount.mAccountName)
                && Objects.equals(mAccountId, otherAccount.mAccountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mNamespace, mId, mAccountType, mAccountName, mAccountId);
    }

    /** Builder for {@link Account}. */
    @Document.BuilderProducer
    public static final class Builder {
        private final @NonNull String mNamespace;
        private final @NonNull String mId;
        private @NonNull String mAccountType = "";
        private @NonNull String mAccountName = "";
        private @NonNull String mAccountId = "";

        /**
         * Constructor for {@link Builder}.
         *
         * @param namespace Namespace for the Document. See
         * {@link Document.Namespace}.
         * @param id Unique identifier for the Document. See {@link Document.Id}.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            Preconditions.checkNotNull(namespace);
            Preconditions.checkNotNull(id);
            mNamespace = namespace;
            mId = id;
        }

        /**
         * Constructor with all the existing values.
         */
        public Builder(@NonNull Account account) {
            mNamespace = account.mNamespace;
            mId = account.mId;
            mAccountType = account.mAccountType;
            mAccountName = account.mAccountName;
            mAccountId = account.mAccountId;
        }

        /**
         * Sets the type of the account. This is typically the package name or a unique identifier
         * of the app or system that owns and manages this account. Example: "com.your.app".
         */
        public @NonNull Builder setAccountType(@NonNull String accountType) {
            Preconditions.checkNotNull(accountType);
            mAccountType = accountType;
            return this;
        }

        /**
         * Sets the human-readable name of the account.
         */
        public @NonNull Builder setAccountName(@NonNull String accountName) {
            Preconditions.checkNotNull(accountName);
            mAccountName = accountName;
            return this;
        }

        /**
         * Sets the opaque, optional identifier for the account. This is the unique ID assigned to
         * the account by service provider (the Authenticator) itself (for example, a
         * Service-Specific User ID). It is used for authentication and service interaction.
         */
        public @NonNull Builder setAccountId(@NonNull String accountId) {
            Preconditions.checkNotNull(accountId);
            mAccountId = accountId;
            return this;
        }

        /**
         * Builds a new {@code Account} instance.
         *
         * @throws IllegalArgumentException If the validation checks fail. This occurs if:
         * <ul>
         * <li>**{@code accountType} is an empty string.**</li>
         * <li>**Both {@code accountName} and {@code accountId} are empty strings.**</li>
         * </ul>
         * **The constructor requires a non-empty {@code accountType} and at least one of**
         * **{@code accountName} or {@code accountId} to be non-empty.**
         */
        public @NonNull Account build() {
            if (mAccountType.isEmpty()) {
                throw new IllegalArgumentException("accountType cannot be empty");
            }
            if (mAccountId.isEmpty() && mAccountName.isEmpty()) {
                throw new IllegalArgumentException(
                        "at least one of accountName or accountId must be non-empty");
            }
            return new Account(mNamespace, mId, mAccountType, mAccountName, mAccountId);
        }
    }
}
