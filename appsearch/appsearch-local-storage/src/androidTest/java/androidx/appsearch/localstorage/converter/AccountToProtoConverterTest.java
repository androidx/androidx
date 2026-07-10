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

package androidx.appsearch.localstorage.converter;


import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchAccount;

import com.google.android.appsearch.proto.AccountProto;

import org.junit.Test;

public class AccountToProtoConverterTest {
    private static final String NAMESPACE = "namespace";
    private static final String ID = "id";
    private static final String TEST_ACCOUNT_TYPE = "com.example.account.type";
    private static final String TEST_ACCOUNT_NAME = "test.user@example.com";
    private static final String TEST_ACCOUNT_ID = "123456789";

    @Test
    public void testFullConversion() {
        // Build an AppSearchAccount with all three properties set.
        AppSearchAccount appSearchAccount = new AppSearchAccount.Builder(NAMESPACE, ID)
                .setAccountType(TEST_ACCOUNT_TYPE)
                .setAccountName(TEST_ACCOUNT_NAME)
                .setAccountId(TEST_ACCOUNT_ID)
                .build();

        // Convert to Protobuf.
        AccountProto proto = AccountToProtoConverter.toAccountProto(appSearchAccount);

        // Verify all fields are present and match.
        assertThat(proto.hasAccountType()).isTrue();
        assertThat(proto.getAccountType()).isEqualTo(TEST_ACCOUNT_TYPE);

        assertThat(proto.hasAccountName()).isTrue();
        assertThat(proto.getAccountName()).isEqualTo(TEST_ACCOUNT_NAME);

        assertThat(proto.hasAccountId()).isTrue();
        assertThat(proto.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    }

    @Test
    public void testPartialConversion_missingOptionalFields() {
        // Build an AppSearchAccount with only the required AccountType.
        AppSearchAccount appSearchAccount = new AppSearchAccount.Builder(NAMESPACE, ID)
                .setAccountType(TEST_ACCOUNT_TYPE)
                .build();

        // Convert to Protobuf.
        AccountProto proto = AccountToProtoConverter.toAccountProto(appSearchAccount);

        // Verify only AccountType is present.
        assertThat(proto.hasAccountType()).isTrue();
        assertThat(proto.getAccountType()).isEqualTo(TEST_ACCOUNT_TYPE);

        // Verify optional fields are NOT present.
        assertThat(proto.hasAccountName()).isFalse();
        assertThat(proto.hasAccountId()).isFalse();
    }

    @Test
    public void testPartialConversion_missingType() {
        // Build an AppSearchAccount with Name and ID, but missing Type (which is technically
        // required by the AppSearchAccount schema, but the GenericDocument layer allows null).
        AppSearchAccount appSearchAccount = new AppSearchAccount.Builder(NAMESPACE, ID)
                .setAccountName(TEST_ACCOUNT_NAME)
                .setAccountId(TEST_ACCOUNT_ID)
                .build();

        // Convert to Protobuf.
        AccountProto proto = AccountToProtoConverter.toAccountProto(appSearchAccount);

        // Verify Name and ID are present.
        assertThat(proto.hasAccountName()).isTrue();
        assertThat(proto.getAccountName()).isEqualTo(TEST_ACCOUNT_NAME);
        assertThat(proto.hasAccountId()).isTrue();
        assertThat(proto.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);

        // Verify Type is NOT present.
        assertThat(proto.hasAccountType()).isFalse();
    }
}
