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

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchAccount;
import androidx.appsearch.app.ExperimentalAppSearchApi;

import com.google.android.appsearch.proto.AccountProto;

import org.jspecify.annotations.NonNull;

/**
 * Translates a {@link AppSearchAccount} into {@link AccountProto}.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public class AccountToProtoConverter {
    private AccountToProtoConverter() {}

    /**  Converters a {@link AppSearchAccount} into {@link AccountProto}. */
    @NonNull
    public static AccountProto toAccountProto(@NonNull AppSearchAccount account) {
        AccountProto.Builder protoBuilder = AccountProto.newBuilder();
        String accountType = account.getAccountType();
        if (accountType != null) {
            protoBuilder.setAccountType(accountType);
        }
        String accountName = account.getAccountName();
        if (accountName != null) {
            protoBuilder.setAccountName(accountName);
        }
        String accountId = account.getAccountId();
        if (accountId != null) {
            protoBuilder.setAccountId(accountId);
        }
        return protoBuilder.build();
    }
}
