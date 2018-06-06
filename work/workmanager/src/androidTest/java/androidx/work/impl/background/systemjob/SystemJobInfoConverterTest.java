/*
 * Copyright 2017 The Android Open Source Project
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

import static androidx.work.NetworkType.CONNECTED;
import static androidx.work.NetworkType.METERED;
import static androidx.work.NetworkType.NOT_REQUIRED;
import static androidx.work.NetworkType.NOT_ROAMING;
import static androidx.work.NetworkType.UNMETERED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;

import android.app.job.JobInfo;
import android.net.Uri;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManagerTest;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class SystemJobInfoConverterTest extends WorkManagerTest {

    private static final long TEST_INTERVAL_DURATION =
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 1232L;
    private static final long TEST_FLEX_DURATION =
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS + 112L;
    private static final int JOB_ID = 101;

    private SystemJobInfoConverter mConverter;

    @Before
    public void setUp() {
        mConverter = new SystemJobInfoConverter(
                InstrumentationRegistry.getTargetContext());
    }

    @Test
    @SmallTest
    public void testConvert_ids() {
        final String expectedWorkSpecId = "026e3422-9cd1-11e7-abc4-cec278b6b50a";
        WorkSpec workSpec = new WorkSpec(expectedWorkSpecId, TestWorker.class.getName());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        String actualWorkSpecId = jobInfo.getExtras().getString(
                SystemJobInfoConverter.EXTRA_WORK_SPEC_ID);
        assertThat(actualWorkSpecId, is(expectedWorkSpecId));
        assertThat(jobInfo.getId(), is(JOB_ID));
    }

    @Test
    @SmallTest
    public void testConvert_setPersistedByDefault() {
        JobInfo jobInfo = mConverter.convert(
                new WorkSpec("id", TestWorker.class.getName()), JOB_ID);
        assertThat(jobInfo.isPersisted(), is(false));
    }

    /**
     * Due to b/6771687, calling {@link JobInfo.Builder#build} with no constraints throws an
     * {@link IllegalArgumentException}. This is testing that {@link SystemJobInfoConverter#convert}
     * sets some dummy constraint to toggle some internal boolean flags in {@link JobInfo.Builder}
     * to allow {@link OneTimeWorkRequest} with no constraints to be converted without affecting its
     * runtime, e.g. calling builder.setMinLatencyMillis(0L).
     */
    @Test
    @SmallTest
    public void testConvert_noConstraints_doesNotThrowException() {
        mConverter.convert(new WorkSpec("id", TestWorker.class.getName()), JOB_ID);
    }

    @Test
    @SmallTest
    public void testConvert_retryPolicy() {
        long expectedBackoffDelayDuration = 50000;
        WorkSpec workSpec = new WorkSpec("id", TestWorker.class.getName());
        workSpec.setBackoffDelayDuration(expectedBackoffDelayDuration);
        workSpec.backoffPolicy = BackoffPolicy.LINEAR;
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.getInitialBackoffMillis(), is(expectedBackoffDelayDuration));
        assertThat(jobInfo.getBackoffPolicy(), is(JobInfo.BACKOFF_POLICY_LINEAR));
    }

    @Test
    @SmallTest
    public void testConvert_initialDelay() {
        final long expectedInitialDelay = 12123L;
        WorkSpec workSpec = new WorkSpec("id", TestWorker.class.getName());
        workSpec.initialDelay = expectedInitialDelay;
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.getMinLatencyMillis(), is(expectedInitialDelay));
    }

    @Test
    @SmallTest
    public void testConvert_periodicWithNoFlex() {
        WorkSpec workSpec = new WorkSpec("id", TestWorker.class.getName());
        workSpec.setPeriodic(TEST_INTERVAL_DURATION);
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.getIntervalMillis(), is(TEST_INTERVAL_DURATION));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_periodicWithFlex() {
        WorkSpec workSpec = new WorkSpec("id", TestWorker.class.getName());
        workSpec.setPeriodic(TEST_INTERVAL_DURATION, TEST_FLEX_DURATION);
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.getIntervalMillis(), is(TEST_INTERVAL_DURATION));
        assertThat(jobInfo.getFlexMillis(), is(TEST_FLEX_DURATION));
    }

    @Test
    @SmallTest
    public void testConvert_requireCharging() {
        final boolean expectedRequireCharging = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresCharging(expectedRequireCharging).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.isRequireCharging(), is(expectedRequireCharging));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvert_requireContentUriTrigger() {
        final Uri expectedUri = Uri.parse("TEST_URI");
        final JobInfo.TriggerContentUri expectedTriggerContentUri =
                new JobInfo.TriggerContentUri(
                        expectedUri, JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS);
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .addContentUriTrigger(expectedUri, true).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);

        JobInfo.TriggerContentUri[] triggerContentUris = jobInfo.getTriggerContentUris();
        assertThat(triggerContentUris, is(arrayContaining(expectedTriggerContentUri)));
    }

    @Test
    @SmallTest
    public void testConvert_requireDeviceIdle() {
        final boolean expectedRequireDeviceIdle = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresDeviceIdle(expectedRequireDeviceIdle).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.isRequireDeviceIdle(), is(expectedRequireDeviceIdle));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireBatteryNotLow() {
        final boolean expectedRequireBatteryNotLow = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresBatteryNotLow(expectedRequireBatteryNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.isRequireBatteryNotLow(), is(expectedRequireBatteryNotLow));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvert_requireStorageNotLow() {
        final boolean expectedRequireStorageNotLow = true;
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiresStorageNotLow(expectedRequireStorageNotLow).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        assertThat(jobInfo.isRequireStorageNotLow(), is(expectedRequireStorageNotLow));
    }

    @Test
    @SmallTest
    public void testConvert_networkTypeNotRoamingRequiresApi24() {
        convertWithRequiredNetworkType(NOT_ROAMING, JobInfo.NETWORK_TYPE_NOT_ROAMING, 24);
    }

    @Test
    @SmallTest
    public void testConvert_networkTypeMeteredRequiresApi26() {
        convertWithRequiredNetworkType(METERED, JobInfo.NETWORK_TYPE_METERED, 26);
    }

    private void convertWithRequiredNetworkType(NetworkType networkType,
                                                int jobInfoNetworkType,
                                                int minSdkVersion) {
        WorkSpec workSpec = getTestWorkSpecWithConstraints(new Constraints.Builder()
                .setRequiredNetworkType(networkType).build());
        JobInfo jobInfo = mConverter.convert(workSpec, JOB_ID);
        if (Build.VERSION.SDK_INT >= minSdkVersion) {
            assertThat(jobInfo.getNetworkType(), is(jobInfoNetworkType));
        } else {
            assertThat(jobInfo.getNetworkType(), is(JobInfo.NETWORK_TYPE_ANY));
        }
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_none() {
        assertThat(SystemJobInfoConverter.convertNetworkType(NOT_REQUIRED),
                is(JobInfo.NETWORK_TYPE_NONE));
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_any() {
        assertThat(SystemJobInfoConverter.convertNetworkType(CONNECTED),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    public void testConvertNetworkType_unmetered() {
        assertThat(SystemJobInfoConverter.convertNetworkType(UNMETERED),
                is(JobInfo.NETWORK_TYPE_UNMETERED));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 23)
    public void testConvertNetworkType_notRoaming_returnAnyBeforeApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 24)
    public void testConvertNetworkType_notRoaming_returnsNotRoamingAtOrAfterApi24() {
        assertThat(SystemJobInfoConverter.convertNetworkType(NOT_ROAMING),
                is(JobInfo.NETWORK_TYPE_NOT_ROAMING));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL, maxSdkVersion = 25)
    public void testConvertNetworkType_metered_returnsAnyBeforeApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(METERED),
                is(JobInfo.NETWORK_TYPE_ANY));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testConvertNetworkType_metered_returnsMeteredAtOrAfterApi26() {
        assertThat(SystemJobInfoConverter.convertNetworkType(METERED),
                is(JobInfo.NETWORK_TYPE_METERED));
    }

    private WorkSpec getTestWorkSpecWithConstraints(Constraints constraints) {
        return getWorkSpec(new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build());
    }
}
