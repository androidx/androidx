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

package androidx.appsearch.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class VmStartAttemptStatsTest {
    @Test
    public void testBuilder() {
        int statusCode = VmStartAttemptStats.VM_START_STATUS_ERROR;
        long latencyMillis = 12_345;

        final VmStartAttemptStats stats =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(statusCode)
                        .setLatencyMillis(latencyMillis)
                        .build();

        assertThat(stats.getStatusCode()).isEqualTo(statusCode);
        assertThat(stats.getLatencyMillis()).isEqualTo(latencyMillis);
        String expectedString = "VmStartAttemptStats {\n"
                + "  statusCode=3,\n"
                + "  latencyMillis=12345\n"
                + "}";
        assertThat(stats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testBuilder_defaultValue() {
        final VmStartAttemptStats stats =
                new VmStartAttemptStats.Builder().build();

        assertThat(stats.getStatusCode()).isEqualTo(VmStartAttemptStats.VM_START_STATUS_UNKNOWN);
        assertThat(stats.getLatencyMillis()).isEqualTo(0);
    }
}
