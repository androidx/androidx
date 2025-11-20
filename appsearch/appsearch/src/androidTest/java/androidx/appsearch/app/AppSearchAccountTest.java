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

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.builtintypes.Account;

import org.junit.Test;

public class AppSearchAccountTest {
    private static final String NAMESPACE = "namespace";
    private static final String ID = "id";
    private static final String TEST_ACCOUNT_TYPE = "com.example.account.type";
    private static final String TEST_ACCOUNT_NAME = "test.user@example.com";
    private static final String TEST_ACCOUNT_ID = "123456789";

    @Test
    public void testBuilderAndGetters() {
        // Use the Builder to construct the AppSearchAccount
        AppSearchAccount account = new AppSearchAccount.Builder(NAMESPACE, ID)
                .setAccountType(TEST_ACCOUNT_TYPE)
                .setAccountName(TEST_ACCOUNT_NAME)
                .setAccountId(TEST_ACCOUNT_ID)
                .build();

        // Verify Getters retrieve the correct values
        assertThat(account.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getSchemaType()).isEqualTo(AppSearchAccount.SCHEMA_TYPE);
        assertThat(account.getAccountType()).isEqualTo(TEST_ACCOUNT_TYPE);
        assertThat(account.getAccountName()).isEqualTo(TEST_ACCOUNT_NAME);
        assertThat(account.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        // Verify underlying GenericDocument properties match
        assertThat(account.getPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_TYPE))
                .isEqualTo(TEST_ACCOUNT_TYPE);
        assertThat(account.getPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_NAME))
                .isEqualTo(TEST_ACCOUNT_NAME);
        assertThat(account.getPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_ID))
                .isEqualTo(TEST_ACCOUNT_ID);
    }

    @Test
    public void testConstructorFromGenericDocument() {
        // Build a GenericDocument manually with the expected properties
        GenericDocument genericDocument = new GenericDocument.Builder<>(
                NAMESPACE, ID, AppSearchAccount.SCHEMA_TYPE)
                .setPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_TYPE, TEST_ACCOUNT_TYPE)
                .setPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_NAME, TEST_ACCOUNT_NAME)
                .setPropertyString(AppSearchAccount.PROPERTY_ACCOUNT_ID, TEST_ACCOUNT_ID)
                .build();

        // Construct AppSearchAccount from the GenericDocument
        AppSearchAccount account = new AppSearchAccount(genericDocument);

        // Verify Getters work on the constructed object
        assertThat(account.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(account.getId()).isEqualTo(ID);
        assertThat(account.getSchemaType()).isEqualTo(AppSearchAccount.SCHEMA_TYPE);
        assertThat(account.getAccountType()).isEqualTo(TEST_ACCOUNT_TYPE);
        assertThat(account.getAccountName()).isEqualTo(TEST_ACCOUNT_NAME);
        assertThat(account.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    }

    @Test
    public void testGettersReturnNullForMissingProperties() {
        // Construct an AppSearchAccount without setting any optional account fields
        AppSearchAccount account = new AppSearchAccount.Builder(NAMESPACE, ID)
                .build();

        // Verify Getters return null
        assertThat(account.getAccountType()).isNull();
        assertThat(account.getAccountName()).isNull();
        assertThat(account.getAccountId()).isNull();
    }

    // @exportToFramework:startStrip()
    @Test
    public void testSchemaTypeMatchBuiltInAccount() throws Exception {
        DocumentClassFactoryRegistry registry = DocumentClassFactoryRegistry.getInstance();
        DocumentClassFactory<Account> factory = registry.getOrCreateFactory(Account.class);
        assertThat(factory.getSchema()).isEqualTo(AppSearchAccount.SCHEMA);

        Account account = new Account.Builder("namespace", "id")
                .setAccountType("accountType")
                .setAccountName("accountName")
                .setAccountId("accountId")
                .build();
        GenericDocument expectedDocument = factory.toGenericDocument(account);

        AppSearchAccount actualAccount = new AppSearchAccount.Builder("namespace", "id")
                .setAccountType("accountType")
                .setAccountName("accountName")
                .setAccountId("accountId")
                .build();
        assertThat(expectedDocument).isEqualTo(actualAccount);
    }
    // @exportToFramework:endStrip()
}
