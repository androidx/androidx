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

package androidx.appsearch.builtintypes.properties;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.builtintypes.Account;

import org.junit.Test;

public class AccountTest {

    @Test
    public void testCreateAccount() throws Exception {
        Account account = new Account("namespace", "id", "type",
                "accountName", "accountId");
        assertThat(account.getNamespace()).isEqualTo("namespace");
        assertThat(account.getId()).isEqualTo("id");
        assertThat(account.getAccountType()).isEqualTo("type");
        assertThat(account.getAccountName()).isEqualTo("accountName");
        assertThat(account.getAccountId()).isEqualTo("accountId");
    }

    @Test
    public void testEqualAccount() throws Exception {
        Account account1 = new Account("namespace", "id", "accountType",
                "accountName", "accountId");
        Account account2 = new Account("namespace", "id", "accountType",
                "accountName", "accountId");
        assertThat(account1).isEqualTo(account2);
        assertThat(account1.hashCode()).isEqualTo(account2.hashCode());
    }

    @Test
    public void testNotEqualAccount() throws Exception {
        Account account1 = new Account("namespace", "id", "accountType",
                "accountName", "accountId");

        Account account2 = new Account("diff", "id", "accountType",
                "accountName", "accountId");
        Account account3 = new Account("namespace", "diff", "accountType",
                "accountName", "accountId");
        Account account4 = new Account("namespace", "id", "diff",
                "accountName", "accountId");
        Account account5 = new Account("namespace", "id", "accountType",
                "diff", "accountId");
        Account account6 = new Account("namespace", "id", "accountType",
                "accountName", "diff");

        assertThat(account1).isNotEqualTo(account2);
        assertThat(account1.hashCode()).isNotEqualTo(account2.hashCode());
        assertThat(account1).isNotEqualTo(account3);
        assertThat(account1.hashCode()).isNotEqualTo(account3.hashCode());
        assertThat(account1).isNotEqualTo(account4);
        assertThat(account1.hashCode()).isNotEqualTo(account4.hashCode());
        assertThat(account1).isNotEqualTo(account5);
        assertThat(account1.hashCode()).isNotEqualTo(account5.hashCode());
        assertThat(account1).isNotEqualTo(account6);
        assertThat(account1.hashCode()).isNotEqualTo(account6.hashCode());
    }

    @Test
    public void testEmptyType() throws Exception {
        assertThrows(NullPointerException.class, () -> new Account(
                "namespace", "id", /*accountType=*/ null, "accountName", "accountId"));
    }

    @Test
    public void testEmptyAccountName() throws Exception {
        Account account = new Account("namespace", "id", "accountType",
                /*accountName=*/ "", "accountId");
        GenericDocument doc = GenericDocument.fromDocumentClass(account);
        assertThat(doc.getSchemaType()).isEqualTo("builtin:Account");
        assertThat(doc.getNamespace()).isEqualTo("namespace");
        assertThat(doc.getId()).isEqualTo("id");
        assertThat(doc.getPropertyString("accountType")).isEqualTo("accountType");
        assertThat(doc.getPropertyString("accountName")).isEmpty();
        assertThat(doc.getPropertyString("accountId")).isEqualTo("accountId");
    }

    @Test
    public void testEmptyAccountId() throws Exception {
        Account account = new Account("namespace", "id", "accountType",
                "accountName", /*accountId=*/ "");
        GenericDocument doc = GenericDocument.fromDocumentClass(account);
        assertThat(doc.getSchemaType()).isEqualTo("builtin:Account");
        assertThat(doc.getNamespace()).isEqualTo("namespace");
        assertThat(doc.getId()).isEqualTo("id");
        assertThat(doc.getPropertyString("accountType")).isEqualTo("accountType");
        assertThat(doc.getPropertyString("accountName")).isEqualTo("accountName");
        assertThat(doc.getPropertyString("accountId")).isEmpty();
    }

    @Test
    public void testToGenericDocument() throws Exception {
        Account account = new Account("namespace", "id", "accountType",
                "accountName", "accountId");
        GenericDocument doc = GenericDocument.fromDocumentClass(account);
        assertThat(doc.getSchemaType()).isEqualTo("builtin:Account");
        assertThat(doc.getNamespace()).isEqualTo("namespace");
        assertThat(doc.getId()).isEqualTo("id");
        assertThat(doc.getPropertyString("accountType")).isEqualTo("accountType");
        assertThat(doc.getPropertyString("accountName")).isEqualTo("accountName");
        assertThat(doc.getPropertyString("accountId")).isEqualTo("accountId");
    }
}
