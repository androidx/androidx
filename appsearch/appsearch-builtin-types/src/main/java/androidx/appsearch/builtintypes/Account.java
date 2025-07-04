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

import androidx.annotation.OptIn;
import androidx.appsearch.annotation.Document;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Represents an account entity within the document property.
 */
@Document(name = "builtin:Account")
@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class Account {

    @Document.Namespace
    private @NonNull String mNamespace;

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
     * @param accountType The type of the account.
     * @param accountName The human-readable name of the account.
     * @param accountId   The account identifier.
     * @throws IllegalArgumentException If {@code accountType} or both {@code namespace} and
     * {@code id} are empty.
     */
    public Account(@NonNull String namespace, @NonNull String id, @NonNull String accountType,
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
        if (o == null || getClass() != o.getClass()) return false;
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
}
