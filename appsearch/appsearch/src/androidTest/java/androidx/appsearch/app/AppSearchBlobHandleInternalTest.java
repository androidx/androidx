/*
 * Copyright 2024 The Android Open Source Project
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

import static androidx.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static androidx.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import org.junit.Test;
//TODO(b/273591938) move this to cts test once it's public.
public class AppSearchBlobHandleInternalTest {

    @Test
    public void testCreateBlobHandle() throws Exception {
        byte[] data = generateRandomBytes(10); // 10 Bytes
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle = AppSearchBlobHandle.createWithSha256(digest, "label123");
        assertThat(blobHandle.getLabel()).isEqualTo("label123");
        assertThat(blobHandle.getSha256Digest()).isEqualTo(digest);
    }

    @Test
    public void testBlobHandleIdentical() throws Exception {
        byte[] data1 = {(byte) 1};
        byte[] data2 = {(byte) 2};
        byte[] digest1 = calculateDigest(data1);
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(digest1, "label123");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(digest1, "label123");
        AppSearchBlobHandle blobHandle3 = AppSearchBlobHandle.createWithSha256(digest1, "321lebal");
        AppSearchBlobHandle blobHandle4 = AppSearchBlobHandle.createWithSha256(digest2, "label123");
        assertThat(blobHandle1).isEqualTo(blobHandle2);
        assertThat(blobHandle1).isNotEqualTo(blobHandle3);
        assertThat(blobHandle1).isNotEqualTo(blobHandle4);
        assertThat(blobHandle3).isNotEqualTo(blobHandle4);
        assertThat(blobHandle1.hashCode()).isEqualTo(blobHandle2.hashCode());
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle3.hashCode());
        assertThat(blobHandle1.hashCode()).isNotEqualTo(blobHandle4.hashCode());
        assertThat(blobHandle3.hashCode()).isNotEqualTo(blobHandle4.hashCode());
    }

    @Test
    public void testCreateBlobHandle_invalidDigest() throws Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AppSearchBlobHandle.createWithSha256(new byte[10], "label123"));
        assertThat(exception).hasMessageThat().contains("The digest is not a SHA-256 digest");
    }

    @Test
    public void testCreateBlobHandle_emptyLabel() throws Exception {
        byte[] data = {(byte) 1};
        byte[] digest = calculateDigest(data);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(digest);
        AppSearchBlobHandle blobHandle2 =
                AppSearchBlobHandle.createWithSha256(digest, /*label=*/"");
        assertThat(blobHandle1).isEqualTo(blobHandle2);
        assertThat(blobHandle1.hashCode()).isEqualTo(blobHandle2.hashCode());
    }
}
