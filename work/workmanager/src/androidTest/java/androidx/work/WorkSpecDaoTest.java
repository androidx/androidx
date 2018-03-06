/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;

import static androidx.work.State.SUCCEEDED;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.worker.TestWorker;

@RunWith(AndroidJUnit4.class)
public class WorkSpecDaoTest extends DatabaseTest {

    @Test
    @SmallTest
    public void testSystemAlarmEligibleWorkSpecs() {
        long startTime = System.currentTimeMillis();
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(
                        startTime + TimeUnit.HOURS.toMillis(1),
                        TimeUnit.MILLISECONDS)
                .build();
        Work succeeded = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .withInitialState(SUCCEEDED)
                .build();
        Work enqueued = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(startTime, TimeUnit.MILLISECONDS)
                .build();

        insertWork(work);
        insertWork(succeeded);
        insertWork(enqueued);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        List<WorkSpec> eligibleWorkSpecs = workSpecDao.getSystemAlarmEligibleWorkSpecs(startTime);
        assertThat(eligibleWorkSpecs.size(), equalTo(1));
        assertThat(eligibleWorkSpecs.get(0).getId(), equalTo(enqueued.getId()));
    }
}
