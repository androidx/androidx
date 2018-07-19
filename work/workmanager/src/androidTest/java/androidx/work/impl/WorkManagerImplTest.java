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

package androidx.work.impl;

import static androidx.work.ExistingWorkPolicy.APPEND;
import static androidx.work.ExistingWorkPolicy.KEEP;
import static androidx.work.ExistingWorkPolicy.REPLACE;
import static androidx.work.NetworkType.METERED;
import static androidx.work.NetworkType.NOT_REQUIRED;
import static androidx.work.State.BLOCKED;
import static androidx.work.State.CANCELLED;
import static androidx.work.State.ENQUEUED;
import static androidx.work.State.FAILED;
import static androidx.work.State.RUNNING;
import static androidx.work.State.SUCCEEDED;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.isOneOf;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.arch.core.executor.ArchTaskExecutor;
import android.arch.core.executor.TaskExecutor;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.ContentUriTriggers;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.TestLifecycleOwner;
import androidx.work.WorkContinuation;
import androidx.work.WorkRequest;
import androidx.work.WorkStatus;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTag;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.Preferences;
import androidx.work.impl.utils.taskexecutor.InstantTaskExecutorRule;
import androidx.work.impl.workers.ConstraintTrackingWorker;
import androidx.work.worker.InfiniteTestWorker;
import androidx.work.worker.TestWorker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkManagerImplTest {

    private WorkDatabase mDatabase;
    private WorkManagerImpl mWorkManagerImpl;

    @Rule
    public InstantTaskExecutorRule mRule = new InstantTaskExecutorRule();

    @Before
    public void setUp() {
        ArchTaskExecutor.getInstance().setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public void postToMainThread(@NonNull Runnable runnable) {
                runnable.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });

        Context context = InstrumentationRegistry.getTargetContext();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .build();
        mWorkManagerImpl = new WorkManagerImpl(context, configuration);
        WorkManagerImpl.setDelegate(mWorkManagerImpl);
        mDatabase = mWorkManagerImpl.getWorkDatabase();
    }

    @After
    public void tearDown() {
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWork() {
        final int workCount = 3;
        final OneTimeWorkRequest[] workArray = new OneTimeWorkRequest[workCount];
        for (int i = 0; i < workCount; ++i) {
            workArray[i] = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        }

        mWorkManagerImpl.beginWith(workArray[0]).then(workArray[1])
                .then(workArray[2])
                .synchronous().enqueueSync();

        for (int i = 0; i < workCount; ++i) {
            String id = workArray[i].getStringId();
            assertThat(mDatabase.workSpecDao().getWorkSpec(id), is(notNullValue()));
            assertThat(
                    "index " + i + " does not have expected number of dependencies!",
                    mDatabase.dependencyDao().getPrerequisites(id).size() > 0,
                    is(i > 0));
        }
    }

    @Test
    @SmallTest
    public void testEnqueue_AddsImplicitTags() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.synchronous().enqueueSync(work);

        WorkTagDao workTagDao = mDatabase.workTagDao();
        List<String> tags = workTagDao.getTagsForWorkSpecId(work.getStringId());
        assertThat(tags, is(notNullValue()));
        assertThat(tags, contains(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertMultipleWork() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.synchronous().enqueueSync(work1, work2, work3);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3.getStringId()), is(notNullValue()));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertMultipleWork_continuationBlocking() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.beginWith(work1, work2, work3)
                .synchronous()
                .enqueueSync();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3.getStringId()), is(notNullValue()));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWithDependencies() {
        OneTimeWorkRequest work1a = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1b = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3a = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3b = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.beginWith(work1a, work1b).then(work2)
                .then(work3a, work3b)
                .synchronous()
                .enqueueSync();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1a.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work1b.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3a.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3b.getStringId()), is(notNullValue()));

        DependencyDao dependencyDao = mDatabase.dependencyDao();
        assertThat(dependencyDao.getPrerequisites(work1a.getStringId()),
                is(emptyCollectionOf(String.class)));
        assertThat(dependencyDao.getPrerequisites(work1b.getStringId()),
                is(emptyCollectionOf(String.class)));

        List<String> prerequisites = dependencyDao.getPrerequisites(work2.getStringId());
        assertThat(prerequisites, containsInAnyOrder(work1a.getStringId(), work1b.getStringId()));

        prerequisites = dependencyDao.getPrerequisites(work3a.getStringId());
        assertThat(prerequisites, containsInAnyOrder(work2.getStringId()));

        prerequisites = dependencyDao.getPrerequisites(work3b.getStringId());
        assertThat(prerequisites, containsInAnyOrder(work2.getStringId()));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWithCompletedDependencies_isNotStatusBlocked() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.synchronous().enqueueSync();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(SUCCEEDED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        workContinuation.then(work2).synchronous().enqueueSync();
        assertThat(workSpecDao.getState(work2.getStringId()), isOneOf(ENQUEUED, RUNNING));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWithFailedDependencies_isStatusFailed() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(FAILED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.synchronous().enqueueSync();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(FAILED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        workContinuation.then(work2).synchronous().enqueueSync();
        assertThat(workSpecDao.getState(work2.getStringId()), is(FAILED));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWithCancelledDependencies_isStatusCancelled() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(CANCELLED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.synchronous().enqueueSync();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        workContinuation.then(work2).synchronous().enqueueSync();
        assertThat(workSpecDao.getState(work2.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testEnqueue_insertWorkConstraints() {
        Uri testUri1 = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri testUri2 = MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(
                        new Constraints.Builder()
                                .setRequiresCharging(true)
                                .setRequiresDeviceIdle(true)
                                .setRequiredNetworkType(METERED)
                                .setRequiresBatteryNotLow(true)
                                .setRequiresStorageNotLow(true)
                                .addContentUriTrigger(testUri1, true)
                                .addContentUriTrigger(testUri2, false)
                                .build())
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).synchronous().enqueueSync();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        ContentUriTriggers expectedTriggers = new ContentUriTriggers();
        expectedTriggers.add(testUri1, true);
        expectedTriggers.add(testUri2, false);

        Constraints constraints = workSpec0.constraints;
        assertThat(constraints, is(notNullValue()));
        assertThat(constraints.requiresCharging(), is(true));
        assertThat(constraints.requiresDeviceIdle(), is(true));
        assertThat(constraints.requiresBatteryNotLow(), is(true));
        assertThat(constraints.requiresStorageNotLow(), is(true));
        assertThat(constraints.getRequiredNetworkType(), is(METERED));
        if (Build.VERSION.SDK_INT >= 24) {
            assertThat(constraints.getContentUriTriggers(), is(expectedTriggers));
        } else {
            assertThat(constraints.getContentUriTriggers(), is(new ContentUriTriggers()));
        }

        constraints = workSpec1.constraints;
        assertThat(constraints, is(notNullValue()));
        assertThat(constraints.requiresCharging(), is(false));
        assertThat(constraints.requiresDeviceIdle(), is(false));
        assertThat(constraints.requiresBatteryNotLow(), is(false));
        assertThat(constraints.requiresStorageNotLow(), is(false));
        assertThat(constraints.getRequiredNetworkType(), is(NOT_REQUIRED));
        assertThat(constraints.getContentUriTriggers().size(), is(0));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWorkInitialDelay() {
        final long expectedInitialDelay = 5000L;
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialDelay(expectedInitialDelay, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).synchronous().enqueueSync();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        assertThat(workSpec0.initialDelay, is(expectedInitialDelay));
        assertThat(workSpec1.initialDelay, is(0L));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWorkBackoffPolicy() {
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 50000, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).synchronous().enqueueSync();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        assertThat(workSpec0.backoffPolicy, is(BackoffPolicy.LINEAR));
        assertThat(workSpec0.backoffDelayDuration, is(50000L));

        assertThat(workSpec1.backoffPolicy, is(BackoffPolicy.EXPONENTIAL));
        assertThat(workSpec1.backoffDelayDuration,
                is(WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertWorkTags() {
        final String firstTag = "first_tag";
        final String secondTag = "second_tag";
        final String thirdTag = "third_tag";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .addTag(secondTag)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).then(work2).synchronous().enqueueSync();

        WorkTagDao workTagDao = mDatabase.workTagDao();
        assertThat(workTagDao.getWorkSpecIdsWithTag(firstTag),
                containsInAnyOrder(work0.getStringId(), work1.getStringId()));
        assertThat(workTagDao.getWorkSpecIdsWithTag(secondTag),
                containsInAnyOrder(work0.getStringId()));
        assertThat(workTagDao.getWorkSpecIdsWithTag(thirdTag), emptyCollectionOf(String.class));
    }

    @Test
    @SmallTest
    public void testEnqueue_insertPeriodicWork() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();

        mWorkManagerImpl.synchronous().enqueueSync(periodicWork);

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(periodicWork.getStringId());
        assertThat(workSpec.isPeriodic(), is(true));
        assertThat(workSpec.intervalDuration, is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(workSpec.flexDuration, is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Test
    @SmallTest
    public void testEnqueued_work_setsPeriodStartTime() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        assertThat(work.getWorkSpec().periodStartTime, is(0L));

        long beforeEnqueueTime = System.currentTimeMillis();

        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.periodStartTime, is(greaterThanOrEqualTo(beforeEnqueueTime)));
    }

    @Test
    @SmallTest
    public void testEnqueued_periodicWork_setsPeriodStartTime() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(periodicWork.getWorkSpec().periodStartTime, is(0L));

        long beforeEnqueueTime = System.currentTimeMillis();

        mWorkManagerImpl.synchronous().enqueueSync(periodicWork);

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(periodicWork.getStringId());
        assertThat(workSpec.periodStartTime, is(greaterThanOrEqualTo(beforeEnqueueTime)));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_setsUniqueName() {
        final String uniqueName = "myname";

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginUniqueWork(uniqueName, REPLACE)
                .then(work)
                .synchronous()
                .enqueueSync();

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(work.getStringId(), isIn(workSpecIds));
    }

    @Test
    @SmallTest
    public void testEnqueueUniquePeriodicWork_setsUniqueName() {
        final String uniqueName = "myname";

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                15L,
                TimeUnit.MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWorkSync(
                uniqueName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(periodicWork.getStringId(), isIn(workSpecIds));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_deletesOldWorkOnReplace() {
        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        OneTimeWorkRequest replacementWork1 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest replacementWork2 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, REPLACE, replacementWork1)
                .then(replacementWork2)
                .synchronous()
                .enqueueSync();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(
                workSpecIds,
                containsInAnyOrder(replacementWork1.getStringId(), replacementWork2.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork2.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testEnqueueUniquePeriodicWork_deletesOldWorkOnReplace() {
        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                TimeUnit.MINUTES)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                TimeUnit.MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWorkSync(
                uniqueName,
                ExistingPeriodicWorkPolicy.REPLACE,
                replacementWork);

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(replacementWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_keepsExistingWorkOnKeep() {
        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        OneTimeWorkRequest replacementWork1 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest replacementWork2 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, KEEP, replacementWork1)
                .then(replacementWork2)
                .synchronous()
                .enqueueSync();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(originalWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork1.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork2.getStringId()), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testEnqueueUniquePeriodicWork_keepsExistingWorkOnKeep() {
        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                TimeUnit.MINUTES)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                TimeUnit.MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWorkSync(
                uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                replacementWork);

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(originalWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(nullValue()));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_replacesExistingWorkOnKeepWhenExistingWorkIsDone() {
        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        OneTimeWorkRequest replacementWork1 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest replacementWork2 =
                new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, KEEP, replacementWork1)
                .then(replacementWork2)
                .synchronous()
                .enqueueSync();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(replacementWork1.getStringId(), replacementWork2.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork2.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testEnqueueUniquePeriodicWork_replacesExistingWorkOnKeepWhenExistingWorkIsDone() {
        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                TimeUnit.MINUTES)
                .setInitialState(SUCCEEDED)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                TimeUnit.MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWorkSync(
                uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                replacementWork);

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(replacementWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_appendsExistingWorkOnAppend() {
        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        OneTimeWorkRequest appendWork1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest appendWork2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, APPEND, appendWork1)
                .then(appendWork2)
                .synchronous()
                .enqueueSync();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(
                        originalWork.getStringId(),
                        appendWork1.getStringId(),
                        appendWork2.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getState(appendWork1.getStringId()), is(BLOCKED));
        assertThat(workSpecDao.getState(appendWork2.getStringId()), is(BLOCKED));

        assertThat(mDatabase.dependencyDao().getDependentWorkIds(originalWork.getStringId()),
                containsInAnyOrder(appendWork1.getStringId()));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_appendsExistingWorkToOnlyLeavesOnAppend() {
        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork1 =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        OneTimeWorkRequest originalWork2 =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        OneTimeWorkRequest originalWork3 =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        OneTimeWorkRequest originalWork4 =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();

        insertNamedWorks(uniqueName, originalWork1, originalWork2, originalWork3, originalWork4);
        insertDependency(originalWork4, originalWork2);
        insertDependency(originalWork3, originalWork2);
        insertDependency(originalWork2, originalWork1);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(
                        originalWork1.getStringId(),
                        originalWork2.getStringId(),
                        originalWork3.getStringId(),
                        originalWork4.getStringId()));

        OneTimeWorkRequest appendWork1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest appendWork2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, APPEND, appendWork1)
                .then(appendWork2)
                .synchronous()
                .enqueueSync();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(
                        originalWork1.getStringId(),
                        originalWork2.getStringId(),
                        originalWork3.getStringId(),
                        originalWork4.getStringId(),
                        appendWork1.getStringId(),
                        appendWork2.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(originalWork2.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(originalWork3.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(originalWork4.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getState(appendWork1.getStringId()), is(BLOCKED));
        assertThat(workSpecDao.getState(appendWork2.getStringId()), is(BLOCKED));

        DependencyDao dependencyDao = mDatabase.dependencyDao();
        assertThat(dependencyDao.getPrerequisites(appendWork1.getStringId()),
                containsInAnyOrder(originalWork3.getStringId(), originalWork4.getStringId()));
        assertThat(dependencyDao.getPrerequisites(appendWork2.getStringId()),
                containsInAnyOrder(appendWork1.getStringId()));
    }

    @Test
    @SmallTest
    public void testBeginUniqueWork_insertsExistingWorkWhenNothingToAppendTo() {
        final String uniqueName = "myname";

        OneTimeWorkRequest appendWork1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest appendWork2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, APPEND, appendWork1)
                .then(appendWork2)
                .synchronous()
                .enqueueSync();

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(appendWork1.getStringId(), appendWork2.getStringId()));
    }

    @Test
    @SmallTest
    public void testGetStatusByIdSync() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        insertWorkSpecAndTags(work);

        WorkStatus workStatus = mWorkManagerImpl.getStatusByIdSync(work.getId());
        assertThat(workStatus.getId().toString(), is(work.getStringId()));
        assertThat(workStatus.getState(), is(SUCCEEDED));
    }

    @Test
    @SmallTest
    public void testGetStatusByIdSync_returnsNullIfNotInDatabase() {
        WorkStatus workStatus = mWorkManagerImpl.getStatusByIdSync(UUID.randomUUID());
        assertThat(workStatus, is(nullValue()));
    }

    @Test
    @SmallTest
    @SuppressWarnings("unchecked")
    public void testGetStatusesById() {
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);

        Observer<List<WorkStatus>> mockObserver = mock(Observer.class);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<List<WorkStatus>> liveData = mWorkManagerImpl.getStatusesById(
                Arrays.asList(work0.getStringId(), work1.getStringId()));
        liveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<List<WorkStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        WorkStatus workStatus0 = new WorkStatus(
                work0.getId(),
                ENQUEUED,
                Data.EMPTY,
                Collections.singletonList(TestWorker.class.getName()));
        WorkStatus workStatus1 = new WorkStatus(
                work1.getId(),
                ENQUEUED,
                Data.EMPTY,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        workSpecDao.setState(RUNNING, work0.getStringId());

        verify(mockObserver, times(2)).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        workStatus0 = new WorkStatus(
                work0.getId(),
                RUNNING,
                Data.EMPTY,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1));

        clearInvocations(mockObserver);
        workSpecDao.setState(RUNNING, work1.getStringId());

        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        workStatus1 = new WorkStatus(
                work1.getId(),
                RUNNING,
                Data.EMPTY,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1));

        liveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @SmallTest
    public void testGetStatusesByTagSync() {
        final String firstTag = "first_tag";
        final String secondTag = "second_tag";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .addTag(secondTag)
                .setInitialState(RUNNING)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(secondTag)
                .setInitialState(SUCCEEDED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        WorkStatus workStatus0 = new WorkStatus(
                work0.getId(),
                RUNNING,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), firstTag, secondTag));
        WorkStatus workStatus1 = new WorkStatus(
                work1.getId(),
                BLOCKED,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), firstTag));
        WorkStatus workStatus2 = new WorkStatus(
                work2.getId(),
                SUCCEEDED,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), secondTag));

        List<WorkStatus> workStatuses = mWorkManagerImpl.getStatusesByTagSync(firstTag);
        assertThat(workStatuses, containsInAnyOrder(workStatus0, workStatus1));

        workStatuses = mWorkManagerImpl.getStatusesByTagSync(secondTag);
        assertThat(workStatuses, containsInAnyOrder(workStatus0, workStatus2));

        workStatuses = mWorkManagerImpl.getStatusesByTagSync("dummy");
        assertThat(workStatuses.size(), is(0));
    }

    @Test
    @SmallTest
    @SuppressWarnings("unchecked")
    public void testGetStatusesByTag() {
        final String firstTag = "first_tag";
        final String secondTag = "second_tag";
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .addTag(secondTag)
                .setInitialState(RUNNING)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(firstTag)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(secondTag)
                .setInitialState(SUCCEEDED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        Observer<List<WorkStatus>> mockObserver = mock(Observer.class);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<List<WorkStatus>> liveData = mWorkManagerImpl.getStatusesByTag(firstTag);
        liveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<List<WorkStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        WorkStatus workStatus0 = new WorkStatus(
                work0.getId(),
                RUNNING,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), firstTag, secondTag));
        WorkStatus workStatus1 = new WorkStatus(
                work1.getId(),
                BLOCKED,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), firstTag));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1));

        workSpecDao.setState(ENQUEUED, work0.getStringId());

        verify(mockObserver, times(2)).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        workStatus0 = new WorkStatus(
                work0.getId(),
                ENQUEUED,
                Data.EMPTY,
                Arrays.asList(TestWorker.class.getName(), firstTag, secondTag));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1));

        liveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @SmallTest
    public void getStatusByNameSync() {
        final String uniqueName = "myname";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(RUNNING)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        insertNamedWorks(uniqueName, work0, work1, work2);
        insertDependency(work1, work0);
        insertDependency(work2, work1);

        WorkStatus workStatus0 = new WorkStatus(
                work0.getId(),
                RUNNING,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkStatus workStatus1 = new WorkStatus(
                work1.getId(),
                BLOCKED,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkStatus workStatus2 = new WorkStatus(
                work2.getId(),
                BLOCKED,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));

        List<WorkStatus> workStatuses = mWorkManagerImpl.getStatusesForUniqueWorkSync(uniqueName);
        assertThat(workStatuses, containsInAnyOrder(workStatus0, workStatus1, workStatus2));

        workStatuses = mWorkManagerImpl.getStatusesForUniqueWorkSync("dummy");
        assertThat(workStatuses.size(), is(0));
    }

    @Test
    @SmallTest
    @SuppressWarnings("unchecked")
    public void testGetStatusesByName() {
        final String uniqueName = "myname";
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(RUNNING)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        insertNamedWorks(uniqueName, work0, work1, work2);
        insertDependency(work1, work0);
        insertDependency(work2, work1);

        Observer<List<WorkStatus>> mockObserver = mock(Observer.class);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<List<WorkStatus>> liveData = mWorkManagerImpl.getStatusesForUniqueWork(uniqueName);
        liveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<List<WorkStatus>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(3));

        WorkStatus workStatus0 = new WorkStatus(
                work0.getId(),
                RUNNING,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkStatus workStatus1 = new WorkStatus(
                work1.getId(),
                BLOCKED,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkStatus workStatus2 = new WorkStatus(
                work2.getId(),
                BLOCKED,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1, workStatus2));

        workSpecDao.setState(ENQUEUED, work0.getStringId());

        verify(mockObserver, times(2)).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(3));

        workStatus0 = new WorkStatus(
                work0.getId(),
                ENQUEUED,
                Data.EMPTY,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workStatus0, workStatus1, workStatus2));

        liveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @SmallTest
    public void testCancelWorkById() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);

        mWorkManagerImpl.synchronous().cancelWorkByIdSync(work0.getId());
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(not(CANCELLED)));
    }

    @Test
    @SmallTest
    public void testCancelWorkById_cancelsDependentWork() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertDependency(work1, work0);

        mWorkManagerImpl.synchronous().cancelWorkByIdSync(work0.getId());

        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testCancelWorkById_cancelsUnfinishedWorkOnly() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(ENQUEUED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertDependency(work1, work0);

        mWorkManagerImpl.synchronous().cancelWorkByIdSync(work0.getId());

        assertThat(workSpecDao.getState(work0.getStringId()), is(SUCCEEDED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testCancelAllWorkByTag() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        final String tagToClear = "tag_to_clear";
        final String tagNotToClear = "tag_not_to_clear";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(tagToClear)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(tagToClear)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(tagNotToClear)
                .build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(tagNotToClear)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);
        insertWorkSpecAndTags(work3);

        mWorkManagerImpl.synchronous().cancelAllWorkByTagSync(tagToClear);

        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(not(CANCELLED)));
        assertThat(workSpecDao.getState(work3.getStringId()), is(not(CANCELLED)));
    }

    @Test
    @SmallTest
    public void testCancelAllWorkByTag_cancelsDependentWork() {
        String tag = "tag";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .addTag(tag)
                .build();

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work4 = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);
        insertWorkSpecAndTags(work3);
        insertWorkSpecAndTags(work4);

        // Dependency graph:
        //                             0
        //                             |
        //                       |------------|
        //            3          1            4
        //            |          |
        //            ------------
        //                 |
        //                 2

        insertDependency(work2, work1);
        insertDependency(work2, work3);
        insertDependency(work1, work0);
        insertDependency(work4, work0);

        mWorkManagerImpl.synchronous().cancelAllWorkByTagSync(tag);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work3.getStringId()), is(not(CANCELLED)));
        assertThat(workSpecDao.getState(work4.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testCancelWorkByName() {
        final String uniqueName = "myname";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, work0, work1);

        mWorkManagerImpl.synchronous().cancelUniqueWorkSync(uniqueName);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @LargeTest
    public void testCancelWorkByName_ignoresFinishedWork() {
        final String uniqueName = "myname";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, work0, work1);

        mWorkManagerImpl.synchronous().cancelUniqueWorkSync(uniqueName);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(SUCCEEDED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testCancelAllWork() {
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(ENQUEUED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(ENQUEUED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(SUCCEEDED));

        mWorkManagerImpl.synchronous().cancelAllWorkSync();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(SUCCEEDED));
    }

    @Test
    @MediumTest
    public void testCancelAllWork_updatesLastCancelAllTime() {
        Preferences preferences = new Preferences(InstrumentationRegistry.getTargetContext());
        preferences.setLastCancelAllTimeMillis(0L);

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work);

        CancelWorkRunnable.forAll(mWorkManagerImpl).run();

        assertThat(preferences.getLastCancelAllTimeMillis(), is(greaterThan(0L)));
    }

    @Test
    @SmallTest
    @SuppressWarnings("unchecked")
    public void testCancelAllWork_updatesLastCancelAllTimeLiveData() throws InterruptedException {
        Preferences preferences = new Preferences(InstrumentationRegistry.getTargetContext());
        preferences.setLastCancelAllTimeMillis(0L);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<Long> cancelAllTimeLiveData = mWorkManagerImpl.getLastCancelAllTimeMillis();
        Observer<Long> mockObserver = mock(Observer.class);
        cancelAllTimeLiveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(0L));

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work);

        clearInvocations(mockObserver);
        CancelWorkRunnable.forAll(mWorkManagerImpl).run();

        Thread.sleep(1000L);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(greaterThan(0L)));

        cancelAllTimeLiveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @SmallTest
    public void pruneFinishedWork() {
        OneTimeWorkRequest enqueuedWork = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest finishedWork =
                new OneTimeWorkRequest.Builder(TestWorker.class).setInitialState(SUCCEEDED).build();
        OneTimeWorkRequest finishedWorkWithUnfinishedDependent =
                new OneTimeWorkRequest.Builder(TestWorker.class).setInitialState(SUCCEEDED).build();
        OneTimeWorkRequest finishedWorkWithLongKeepForAtLeast =
                new OneTimeWorkRequest.Builder(TestWorker.class)
                        .setInitialState(SUCCEEDED)
                        .keepResultsForAtLeast(999, TimeUnit.DAYS)
                        .build();

        insertWorkSpecAndTags(enqueuedWork);
        insertWorkSpecAndTags(finishedWork);
        insertWorkSpecAndTags(finishedWorkWithUnfinishedDependent);
        insertWorkSpecAndTags(finishedWorkWithLongKeepForAtLeast);

        insertDependency(enqueuedWork, finishedWorkWithUnfinishedDependent);

        mWorkManagerImpl.synchronous().pruneWorkSync();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWorkWithUnfinishedDependent.getStringId()),
                is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWorkWithLongKeepForAtLeast.getStringId()),
                is(nullValue()));
    }

    @Test
    @SmallTest
    public void testSynchronousCancelAndGetStatus() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getStringId()), is(ENQUEUED));

        mWorkManagerImpl.synchronous().cancelWorkByIdSync(work.getId());
        assertThat(mWorkManagerImpl.getStatusByIdSync(work.getId()).getState(), is(CANCELLED));
    }

    @Test
    @SmallTest
    public void testGenerateCleanupCallback_resetsRunningWorkStatuses() {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(RUNNING)
                .build();
        workSpecDao.insertWorkSpec(work.getWorkSpec());

        assertThat(workSpecDao.getState(work.getStringId()), is(RUNNING));

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();
        WorkDatabase.generateCleanupCallback().onOpen(db);

        assertThat(workSpecDao.getState(work.getStringId()), is(ENQUEUED));
        assertThat(work.getWorkSpec().scheduleRequestedAt, is(SCHEDULE_NOT_REQUESTED_YET));
    }

    @Test
    @SmallTest
    public void testGenerateCleanupCallback_deletesOldFinishedWork() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setPeriodStartTime(WorkDatabase.getPruneDate() - 1L, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setPeriodStartTime(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                .build();

        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();
        WorkDatabase.generateCleanupCallback().onOpen(db);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    public void testGenerateCleanupCallback_doesNotDeleteOldFinishedWorkWithActiveDependents() {
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setPeriodStartTime(WorkDatabase.getPruneDate() - 1L, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setPeriodStartTime(WorkDatabase.getPruneDate() - 1L, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(ENQUEUED)
                .setPeriodStartTime(WorkDatabase.getPruneDate() - 1L, TimeUnit.MILLISECONDS)
                .build();

        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        // Dependency graph: 0 -> 1 -> 2
        insertDependency(work1, work0);
        insertDependency(work2, work1);

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();
        WorkDatabase.generateCleanupCallback().onOpen(db);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work0.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(not(nullValue())));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testEnqueueApi22OrLower_withBatteryNotLowConstraint_expectsOriginalWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 22)
    public void testEnqueueApi22OrLower_withStorageNotLowConstraint_expectsOriginalWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testEnqueueApi23To25_withBatteryNotLowConstraint_expectsConstraintTrackingWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(ConstraintTrackingWorker.class.getName()));
        assertThat(workSpec.input.getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME),
                is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testEnqueueApi23To25_withStorageNotLowConstraint_expectsConstraintTrackingWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(ConstraintTrackingWorker.class.getName()));
        assertThat(workSpec.input.getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME),
                is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testEnqueueApi26OrHigher_withBatteryNotLowConstraint_expectsOriginalWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testEnqueueApi26OrHigher_withStorageNotLowConstraint_expectsOriginalWorker() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).synchronous().enqueueSync();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    private void insertWorkSpecAndTags(WorkRequest work) {
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
        for (String tag : work.getTags()) {
            mDatabase.workTagDao().insert(new WorkTag(tag, work.getStringId()));
        }
    }

    private void insertNamedWorks(String name, WorkRequest... works) {
        for (WorkRequest work : works) {
            insertWorkSpecAndTags(work);
            mDatabase.workNameDao().insert(new WorkName(name, work.getStringId()));
        }
    }

    private void insertDependency(OneTimeWorkRequest work, OneTimeWorkRequest prerequisiteWork) {
        mDatabase.dependencyDao().insertDependency(
                new Dependency(work.getStringId(), prerequisiteWork.getStringId()));
    }
}
