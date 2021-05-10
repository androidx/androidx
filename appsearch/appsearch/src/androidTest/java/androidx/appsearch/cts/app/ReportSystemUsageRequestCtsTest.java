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

import androidx.appsearch.app.ReportSystemUsageRequest;

import org.junit.Test;

public class ReportSystemUsageRequestCtsTest {
    @Test
    public void testGettersAndSetters() {
        ReportSystemUsageRequest request = new ReportSystemUsageRequest.Builder(
                "package1", "database1", "namespace1", "id1")
                .setUsageTimestampMillis(32)
                .build();
        assertThat(request.getPackageName()).isEqualTo("package1");
        assertThat(request.getDatabaseName()).isEqualTo("database1");
        assertThat(request.getNamespace()).isEqualTo("namespace1");
        assertThat(request.getDocumentId()).isEqualTo("id1");
        assertThat(request.getUsageTimestampMillis()).isEqualTo(32);
    }

    @Test
    public void testUsageTimestampDefault() {
        long startTs = System.currentTimeMillis();
        ReportSystemUsageRequest request =
                new ReportSystemUsageRequest.Builder("package1", "database1", "namespace1", "id1")
                        .build();
        assertThat(request.getUsageTimestampMillis()).isAtLeast(startTs);
    }
}
