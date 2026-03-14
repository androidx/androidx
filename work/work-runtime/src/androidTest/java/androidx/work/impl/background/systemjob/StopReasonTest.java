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

package androidx.work.impl.background.systemjob;

import static android.app.job.JobParameters.STOP_REASON_TIMEOUT;
import static android.app.job.JobParameters.STOP_REASON_UNDEFINED;
import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.WorkInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StopReasonTest {

    @Test
    public void stopReason_undefinedMappedToUnknown() {
        assertThat(SystemJobService.stopReason(STOP_REASON_UNDEFINED),
                is(WorkInfo.STOP_REASON_UNKNOWN));
    }

    @Test
    public void stopReason_validReasonMappedCorrectly() {
        assertThat(SystemJobService.stopReason(STOP_REASON_TIMEOUT),
                is(WorkInfo.STOP_REASON_TIMEOUT));
        assertThat(SystemJobService.stopReason(STOP_REASON_CONSTRAINT_CONNECTIVITY),
                is(WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY));
    }

    @Test
    public void stopReason_unrecognizedReasonMappedToUnknown() {
        assertThat(SystemJobService.stopReason(999),
                is(WorkInfo.STOP_REASON_UNKNOWN));
    }
}
