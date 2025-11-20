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

package androidx.appsearch.app;

import androidx.annotation.RestrictTo;
import androidx.appsearch.annotation.CanIgnoreReturnValue;
import androidx.appsearch.flags.FlaggedApi;
import androidx.appsearch.flags.Flags;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Encapsulates a {@link GenericDocument} that represent an Account.
 *
 * <p>This class is a higher level implement of {@link GenericDocument}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
@FlaggedApi(Flags.FLAG_ENABLE_SCHEMAS_WIPEOUT_ACCOUNT_PROPERTY_PATHS)
public class AppSearchAccount extends GenericDocument {
    /** The name of the schema type for {@link AppSearchAccount} documents.*/
    public static final String SCHEMA_TYPE = "builtin:Account";

    public static final String PROPERTY_ACCOUNT_TYPE = "accountType";
    public static final String PROPERTY_ACCOUNT_NAME = "accountName";
    public static final String PROPERTY_ACCOUNT_ID = "accountId";

    public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_ACCOUNT_TYPE)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .build()
            ).addProperty(new AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_ACCOUNT_NAME)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .build()
            ).addProperty(new AppSearchSchema.StringPropertyConfig.Builder(PROPERTY_ACCOUNT_ID)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_VERBATIM)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
                    .build()

            ).build();

    /**
     * Creates a new {@link AppSearchAccount} from the contents of an existing
     * {@link GenericDocument}.
     *
     * @param document The {@link GenericDocument} containing the Account content.
     */
    public AppSearchAccount(@NonNull GenericDocument document) {
        super(document);
    }

    /**
     * Gets the type of {@link AppSearchAccount} or {@code null} if it's not been set yet.
     */
    public @Nullable String getAccountType() {
        return getPropertyString(PROPERTY_ACCOUNT_TYPE);
    }

    /**
     * Gets the name of {@link AppSearchAccount} or {@code null} if it's not been set yet.
     */
    public @Nullable String getAccountName() {
        return getPropertyString(PROPERTY_ACCOUNT_NAME);
    }

    /**
     * Gets the account id of {@link AppSearchAccount} or {@code null} if it's not been set yet.
     */
    public @Nullable String getAccountId() {
        return getPropertyString(PROPERTY_ACCOUNT_ID);
    }

    /**
     * The builder class for {@link AppSearchAccount}.
     */
    public static class Builder extends GenericDocument.Builder<Builder> {
        /**
         * Creates a new {@link AppSearchAccount.Builder}
         *
         * @param namespace The namespace of the account.
         * @param id        The document ID of the account.
         */
        public Builder(@NonNull String namespace, @NonNull String id) {
            super(namespace, id, SCHEMA_TYPE);
        }

        /**
         * Sets the type of {@link AppSearchAccount}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAccountType(@NonNull String accountType) {
            return setPropertyString(PROPERTY_ACCOUNT_TYPE, accountType);
        }

        /**
         * Sets the name of {@link AppSearchAccount}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAccountName(@NonNull String accountName) {
            return setPropertyString(PROPERTY_ACCOUNT_NAME, accountName);
        }

        /**
         * Sets the account id of {@link AppSearchAccount}
         */
        @CanIgnoreReturnValue
        public @NonNull Builder setAccountId(@NonNull String accountId) {
            return setPropertyString(PROPERTY_ACCOUNT_ID, accountId);
        }

        /** Builds the {@link AppSearchAccount} object. */
        @Override
        public @NonNull AppSearchAccount build() {
            return new AppSearchAccount(super.build());
        }
    }
}
