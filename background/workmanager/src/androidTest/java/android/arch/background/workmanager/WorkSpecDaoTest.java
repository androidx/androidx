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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.State.SUCCEEDED;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import android.arch.background.workmanager.impl.model.Dependency;
import android.arch.background.workmanager.impl.model.DependencyDao;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.background.workmanager.impl.model.WorkSpecDao;
import android.arch.background.workmanager.worker.TestWorker;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkSpecDaoTest extends DatabaseTest {

    @Test
    @SmallTest
    public void testPruneLeaves() {
        Work enqueuedWork = new Work.Builder(TestWorker.class).build();
        Work finishedPrerequisiteWork1A =
                new Work.Builder(TestWorker.class).withInitialState(SUCCEEDED).build();
        Work finishedPrerequisiteWork1B =
                new Work.Builder(TestWorker.class).withInitialState(SUCCEEDED).build();
        Work finishedPrerequisiteWork2 =
                new Work.Builder(TestWorker.class).withInitialState(SUCCEEDED).build();
        Work finishedFinalWork =
                new Work.Builder(TestWorker.class).withInitialState(SUCCEEDED).build();

        insertWork(enqueuedWork);
        insertWork(finishedPrerequisiteWork1A);
        insertWork(finishedPrerequisiteWork1B);
        insertWork(finishedPrerequisiteWork2);
        insertWork(finishedFinalWork);

        Dependency dependency21A = new Dependency(
                finishedPrerequisiteWork2.getId(), finishedPrerequisiteWork1A.getId());
        Dependency dependency21B = new Dependency(
                finishedPrerequisiteWork2.getId(), finishedPrerequisiteWork1B.getId());
        Dependency dependencyFinal2 = new Dependency(
                finishedFinalWork.getId(), finishedPrerequisiteWork2.getId());

        DependencyDao dependencyDao = mDatabase.dependencyDao();
        dependencyDao.insertDependency(dependency21A);
        dependencyDao.insertDependency(dependency21B);
        dependencyDao.insertDependency(dependencyFinal2);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        int result = workSpecDao.pruneLeaves();
        assertThat(result, is(1));
        assertThat(workSpecDao.getWorkSpec(finishedFinalWork.getId()), is(nullValue()));
        assertThat(
                workSpecDao.getWorkSpec(finishedPrerequisiteWork2.getId()), is(not(nullValue())));
        assertThat(
                workSpecDao.getWorkSpec(finishedPrerequisiteWork1A.getId()), is(not(nullValue())));
        assertThat(
                workSpecDao.getWorkSpec(finishedPrerequisiteWork1B.getId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getId()), is(not(nullValue())));

        result = workSpecDao.pruneLeaves();
        assertThat(result, is(1));
        assertThat(workSpecDao.getWorkSpec(finishedPrerequisiteWork2.getId()), is(nullValue()));
        assertThat(
                workSpecDao.getWorkSpec(finishedPrerequisiteWork1A.getId()), is(not(nullValue())));
        assertThat(
                workSpecDao.getWorkSpec(finishedPrerequisiteWork1B.getId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getId()), is(not(nullValue())));

        result = workSpecDao.pruneLeaves();
        assertThat(result, is(2));
        assertThat(workSpecDao.getWorkSpec(finishedPrerequisiteWork1A.getId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedPrerequisiteWork1B.getId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getId()), is(not(nullValue())));

        result = workSpecDao.pruneLeaves();
        assertThat(result, is(0));
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testSystemAlarmEligibleWorkSpecs() {
        long startTime = System.currentTimeMillis();
        Work work = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(startTime + TimeUnit.HOURS.toMillis(1))
                .build();
        Work succeeded = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(startTime)
                .withInitialState(SUCCEEDED)
                .build();
        Work enqueued = new Work.Builder(TestWorker.class)
                .withPeriodStartTime(startTime)
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
