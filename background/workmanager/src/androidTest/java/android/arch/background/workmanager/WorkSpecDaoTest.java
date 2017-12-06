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

import static android.arch.background.workmanager.impl.BaseWork.STATUS_SUCCEEDED;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.arch.background.workmanager.worker.TestWorker;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkSpecDaoTest extends DatabaseTest {

    @Test
    @SmallTest
    public void testPruneLeaves() {
        Work enqueuedWork = new Work.Builder(TestWorker.class).build();
        Work finishedPrerequisiteWork1A =
                new Work.Builder(TestWorker.class).withInitialStatus(STATUS_SUCCEEDED).build();
        Work finishedPrerequisiteWork1B =
                new Work.Builder(TestWorker.class).withInitialStatus(STATUS_SUCCEEDED).build();
        Work finishedPrerequisiteWork2 =
                new Work.Builder(TestWorker.class).withInitialStatus(STATUS_SUCCEEDED).build();
        Work finishedFinalWork =
                new Work.Builder(TestWorker.class).withInitialStatus(STATUS_SUCCEEDED).build();

        insertBaseWork(enqueuedWork);
        insertBaseWork(finishedPrerequisiteWork1A);
        insertBaseWork(finishedPrerequisiteWork1B);
        insertBaseWork(finishedPrerequisiteWork2);
        insertBaseWork(finishedFinalWork);

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
}
