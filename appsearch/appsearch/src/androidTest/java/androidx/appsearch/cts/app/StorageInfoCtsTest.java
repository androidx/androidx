/*
 * Copyright 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.StorageInfo;
import androidx.appsearch.testutil.AppSearchTestUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class StorageInfoCtsTest {

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Test
    public void testBuildStorageInfo() {
        StorageInfo storageInfo =
                new StorageInfo.Builder()
                        .setAliveDocumentsCount(10)
                        .setSizeBytes(1L)
                        .setAliveNamespacesCount(10)
                        .build();

        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(10);
        assertThat(storageInfo.getSizeBytes()).isEqualTo(1L);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(10);
    }

    @Test
    public void testBuildStorageInfo_withDefaults() {
        StorageInfo storageInfo = new StorageInfo.Builder().build();

        assertThat(storageInfo.getAliveDocumentsCount()).isEqualTo(0);
        assertThat(storageInfo.getSizeBytes()).isEqualTo(0L);
        assertThat(storageInfo.getAliveNamespacesCount()).isEqualTo(0);
    }

    @Test
    public void testBuildStorageInfo_withBlob() {
        StorageInfo storageInfo =
                new StorageInfo.Builder()
                        .setBlobsSizeBytes(12L)
                        .setBlobsCount(20)
                        .build();

        assertThat(storageInfo.getBlobsCount()).isEqualTo(20);
        assertThat(storageInfo.getBlobsSizeBytes()).isEqualTo(12L);
    }

    @Test
    public void testBuildStorageInfo_withBlobDefaults() {
        StorageInfo storageInfo = new StorageInfo.Builder().build();

        assertThat(storageInfo.getBlobsCount()).isEqualTo(0);
        assertThat(storageInfo.getBlobsSizeBytes()).isEqualTo(0L);
    }
}
