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

public class VmInitializationStatsTest {
    @Test
    public void testBuilder() {
        final VmStartAttemptStats vmStartAttemptStats0 =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(VmStartAttemptStats.VM_START_STATUS_TIMEOUT)
                        .setLatencyMillis(30_000)
                        .build();

        final VmStartAttemptStats vmStartAttemptStats1 =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(VmStartAttemptStats.VM_START_STATUS_SUCCESS)
                        .setLatencyMillis(5_000)
                        .build();

        final VmInitializationStats stats =
                new VmInitializationStats.Builder(VmInitializationStats.VM_INIT_TYPE_RECONNECTING)
                        .addStartAttemptStats(vmStartAttemptStats0)
                        .addStartAttemptStats(vmStartAttemptStats1)
                        .build();
        assertThat(stats.getVmInitType())
                .isEqualTo(VmInitializationStats.VM_INIT_TYPE_RECONNECTING);
        assertThat(stats.getVmStartAttemptsStats())
                .containsExactly(vmStartAttemptStats0, vmStartAttemptStats1);
        String expectedString = "VmInitializationStats {\n"
                + "  vmInitType=2,\n"
                + "  vmStartAttemptsStats=[\n"
                + "    VmStartAttemptStats {\n"
                + "      statusCode=2,\n"
                + "      latencyMillis=30000\n"
                + "    },\n"
                + "    VmStartAttemptStats {\n"
                + "      statusCode=1,\n"
                + "      latencyMillis=5000\n"
                + "    },\n"
                + "  ]\n"
                + "}";
        assertThat(stats.toString()).isEqualTo(expectedString);
    }

    @Test
    public void testBuilder_builderReuse() {
        final VmStartAttemptStats vmStartAttemptStats0 =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(VmStartAttemptStats.VM_START_STATUS_TIMEOUT)
                        .setLatencyMillis(30_000)
                        .build();

        final VmStartAttemptStats vmStartAttemptStats1 =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(VmStartAttemptStats.VM_START_STATUS_ERROR)
                        .setLatencyMillis(10_000)
                        .build();

        VmInitializationStats.Builder builder =
                new VmInitializationStats.Builder(VmInitializationStats.VM_INIT_TYPE_RECONNECTING)
                        .addStartAttemptStats(vmStartAttemptStats0)
                        .addStartAttemptStats(vmStartAttemptStats1);

        final VmInitializationStats stats0 = builder.build();

        final VmStartAttemptStats vmStartAttemptStats2 =
                new VmStartAttemptStats.Builder()
                        .setStatusCode(VmStartAttemptStats.VM_START_STATUS_SUCCESS)
                        .setLatencyMillis(5_000)
                        .build();
        builder.addStartAttemptStats(vmStartAttemptStats2);

        final VmInitializationStats stats1 = builder.build();

        // Check that stats0 wasn't altered.
        assertThat(stats0.getVmInitType())
                .isEqualTo(VmInitializationStats.VM_INIT_TYPE_RECONNECTING);
        assertThat(stats0.getVmStartAttemptsStats())
                .containsExactly(vmStartAttemptStats0, vmStartAttemptStats1);

        // Check that stats1 has the new values.
        assertThat(stats1.getVmInitType())
                .isEqualTo(VmInitializationStats.VM_INIT_TYPE_RECONNECTING);
        assertThat(stats1.getVmStartAttemptsStats())
                .containsExactly(vmStartAttemptStats0, vmStartAttemptStats1, vmStartAttemptStats2);
    }
}
