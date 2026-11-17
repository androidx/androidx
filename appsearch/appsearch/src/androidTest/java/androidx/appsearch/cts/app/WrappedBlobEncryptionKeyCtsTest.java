/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.WrappedBlobEncryptionKey;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.AppSearchTestUtils;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_COMMIT_ENCRYPTED_BLOB)
public class WrappedBlobEncryptionKeyCtsTest {
    @Rule public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testCreateWrappedBlobEncryptionKey() {
        byte[] wrappedDek = generateRandomBytes(80);
        byte[] aeadParams = new byte[] {1, 2, 3, 4, 5};
        WrappedBlobEncryptionKey key = new WrappedBlobEncryptionKey(wrappedDek, aeadParams);

        assertThat(key.getWrappedDek()).isEqualTo(wrappedDek);
        assertThat(key.getAeadParams()).isEqualTo(aeadParams);
    }

    @Test
    public void testWrappedBlobEncryptionKey_nullWrappedDek_throwsNullPointerException() {
        byte[] aeadParams = new byte[] {1, 2, 3};

        assertThrows(
                NullPointerException.class,
                () -> new WrappedBlobEncryptionKey(/* wrappedDek= */ null, aeadParams));
    }

    @Test
    public void testWrappedBlobEncryptionKey_nullAeadParams_returnsNull() {
        WrappedBlobEncryptionKey wrappedBlobEncryptionKey =
                new WrappedBlobEncryptionKey(generateRandomBytes(80), /* aeadParams= */ null);

        assertThat(wrappedBlobEncryptionKey.getAeadParams()).isNull();
    }

    @Test
    public void testWrappedBlobEncryptionKey_equalsAndHashCode() {
        byte[] dek1 = new byte[] {1, 2, 3, 4};
        byte[] dek2 = new byte[] {5, 6, 7, 8};
        byte[] aeadParams1 = new byte[] {10, 20};
        byte[] aeadParams2 = new byte[] {30, 40};

        WrappedBlobEncryptionKey key1 =
                new WrappedBlobEncryptionKey(dek1.clone(), aeadParams1.clone());
        WrappedBlobEncryptionKey key2 =
                new WrappedBlobEncryptionKey(dek1.clone(), aeadParams1.clone());
        WrappedBlobEncryptionKey differentDek = new WrappedBlobEncryptionKey(dek2, aeadParams1);
        WrappedBlobEncryptionKey differentAeadParams =
                new WrappedBlobEncryptionKey(dek1, aeadParams2);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        assertThat(key1).isNotEqualTo(differentDek);
        assertThat(key1).isNotEqualTo(differentAeadParams);
    }
}
