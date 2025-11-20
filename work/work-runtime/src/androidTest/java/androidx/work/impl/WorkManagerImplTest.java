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
import static androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE;
import static androidx.work.ExistingWorkPolicy.KEEP;
import static androidx.work.ExistingWorkPolicy.REPLACE;
import static androidx.work.NetworkType.CONNECTED;
import static androidx.work.NetworkType.METERED;
import static androidx.work.NetworkType.NOT_REQUIRED;
import static androidx.work.WorkInfo.State.BLOCKED;
import static androidx.work.WorkInfo.State.CANCELLED;
import static androidx.work.WorkInfo.State.ENQUEUED;
import static androidx.work.WorkInfo.State.FAILED;
import static androidx.work.WorkInfo.State.RUNNING;
import static androidx.work.WorkInfo.State.SUCCEEDED;
import static androidx.work.impl.WorkManagerImplExtKt.createWorkManager;
import static androidx.work.impl.WorkManagerImplExtKt.schedulers;
import static androidx.work.impl.model.WorkSpec.SCHEDULE_NOT_REQUESTED_YET;
import static androidx.work.impl.workers.ConstraintTrackingWorkerKt.ARGUMENT_CLASS_NAME;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.testutils.RepeatRule;
import androidx.work.BackoffPolicy;
import androidx.work.Configuration;
import androidx.work.Constraints;
import androidx.work.Constraints.ContentUriTrigger;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.WorkerFactory;
import androidx.work.impl.background.greedy.GreedyScheduler;
import androidx.work.impl.background.systemalarm.RescheduleReceiver;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.Dependency;
import androidx.work.impl.model.DependencyDao;
import androidx.work.impl.model.WorkName;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.model.WorkTag;
import androidx.work.impl.model.WorkTagDao;
import androidx.work.impl.testutils.TestOverrideClock;
import androidx.work.impl.utils.CancelWorkRunnable;
import androidx.work.impl.utils.ForceStopRunnable;
import androidx.work.impl.utils.PreferenceUtils;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.impl.workers.ConstraintTrackingWorker;
import androidx.work.impl.workers.ConstraintTrackingWorkerKt;
import androidx.work.worker.InfiniteTestWorker;
import androidx.work.worker.StopAwareWorker;
import androidx.work.worker.TestWorker;

import com.google.common.util.concurrent.Futures;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkManagerImplTest {

    private static final long SLEEP_DURATION_SMALL_MILLIS = 500L;

    private Context mContext;
    private Configuration mConfiguration;
    private TestOverrideClock mClock = new TestOverrideClock();
    private WorkDatabase mDatabase;
    private Scheduler mScheduler;
    private WorkManagerImpl mWorkManagerImpl;

    @Rule
    public RepeatRule mRepeatRule = new RepeatRule();

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
        mContext = ApplicationProvider.getApplicationContext();
        mConfiguration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .setClock(mClock)
                .setMinimumLoggingLevel(Log.DEBUG)
                .setWorkerFactory(spy(WorkerFactory.class))
                .build();
        InstantWorkTaskExecutor workTaskExecutor = new InstantWorkTaskExecutor();
        mWorkManagerImpl = spy(createWorkManager(mContext, mConfiguration, workTaskExecutor));
        WorkLauncher workLauncher = new WorkLauncherImpl(mWorkManagerImpl.getProcessor(),
                workTaskExecutor);
        mScheduler =
                spy(new GreedyScheduler(
                        mContext,
                        mWorkManagerImpl.getConfiguration(),
                        mWorkManagerImpl.getTrackers(),
                        mWorkManagerImpl.getProcessor(), workLauncher, workTaskExecutor));
        // Don't return any scheduler. We don't need to actually execute work for most of our tests.
        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.<Scheduler>emptyList());
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
    public void testIsInitialized() {
        assertThat(WorkManager.isInitialized(), is(true));
    }

    @Test
    @SmallTest
    public void testGetConfiguration() {
        assertThat(mWorkManagerImpl.getConfiguration(), is(mConfiguration));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWork() throws ExecutionException, InterruptedException {
        final int workCount = 3;
        final OneTimeWorkRequest[] workArray = new OneTimeWorkRequest[workCount];
        for (int i = 0; i < workCount; ++i) {
            workArray[i] = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        }

        mWorkManagerImpl.beginWith(workArray[0]).then(workArray[1])
                .then(workArray[2])
                .enqueue().getResult()
                .get();

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
    @MediumTest
    public void testEnqueue_AddsImplicitTags() throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.enqueue(Collections.singletonList(work)).getResult().get();

        WorkTagDao workTagDao = mDatabase.workTagDao();
        List<String> tags = workTagDao.getTagsForWorkSpecId(work.getStringId());
        assertThat(tags, is(notNullValue()));
        assertThat(tags, contains(TestWorker.class.getName()));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertMultipleWork() throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.enqueue(Arrays.asList(work1, work2, work3)).getResult().get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3.getStringId()), is(notNullValue()));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertMultipleWork_continuationBlocking()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.beginWith(Arrays.asList(work1, work2, work3))
                .enqueue()
                .getResult()
                .get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(work3.getStringId()), is(notNullValue()));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWithDependencies()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work1a = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1b = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3a = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work3b = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.beginWith(Arrays.asList(work1a, work1b)).then(work2)
                .then(Arrays.asList(work3a, work3b))
                .enqueue()
                .getResult()
                .get();

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
    @MediumTest
    public void testEnqueue_insertWithCompletedDependencies_isNotStatusBlocked()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.enqueue().getResult().get();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(SUCCEEDED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        workContinuation.then(work2).enqueue().getResult().get();
        assertThat(workSpecDao.getState(work2.getStringId()), isOneOf(ENQUEUED, RUNNING));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWithFailedDependencies_isStatusFailed()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(FAILED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.enqueue().getResult().get();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(FAILED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        workContinuation.then(work2).enqueue().getResult().get();
        assertThat(workSpecDao.getState(work2.getStringId()), is(FAILED));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWithCancelledDependencies_isStatusCancelled()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(CANCELLED)
                .build();

        WorkContinuation workContinuation = mWorkManagerImpl.beginWith(work1);
        workContinuation.enqueue().getResult().get();
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));

        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        workContinuation.then(work2).enqueue().getResult().get();
        assertThat(workSpecDao.getState(work2.getStringId()), is(CANCELLED));
    }

    @Test
    @MediumTest
    // TODO:(b/191892569): Investigate why this passes lintDebug with minSdkVersion = 23.
    @SdkSuppress(minSdkVersion = 24)
    public void testEnqueue_insertWorkConstraints()
            throws ExecutionException, InterruptedException {

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
        mWorkManagerImpl.beginWith(work0).then(work1).enqueue().getResult().get();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        Set<ContentUriTrigger> expectedTriggers = new HashSet<>();
        expectedTriggers.add(new ContentUriTrigger(testUri1, true));
        expectedTriggers.add(new ContentUriTrigger(testUri2, false));

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
            assertThat(constraints.getContentUriTriggers(), is(new HashSet<ContentUriTrigger>()));
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
    @MediumTest
    public void testEnqueue_insertWorkInitialDelay()
            throws ExecutionException, InterruptedException {

        final long expectedInitialDelayMillis = 5000L;
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialDelay(expectedInitialDelayMillis, MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).enqueue().getResult().get();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        assertThat(workSpec0.initialDelay, is(expectedInitialDelayMillis));
        assertThat(workSpec1.initialDelay, is(0L));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWorkBackoffPolicy()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 50000, MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.beginWith(work0).then(work1).enqueue().getResult().get();

        WorkSpec workSpec0 = mDatabase.workSpecDao().getWorkSpec(work0.getStringId());
        WorkSpec workSpec1 = mDatabase.workSpecDao().getWorkSpec(work1.getStringId());

        assertThat(workSpec0.backoffPolicy, is(BackoffPolicy.LINEAR));
        assertThat(workSpec0.backoffDelayDuration, is(50000L));

        assertThat(workSpec1.backoffPolicy, is(BackoffPolicy.EXPONENTIAL));
        assertThat(workSpec1.backoffDelayDuration,
                is(WorkRequest.DEFAULT_BACKOFF_DELAY_MILLIS));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertWorkTags() throws ExecutionException, InterruptedException {
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
        mWorkManagerImpl.beginWith(work0).then(work1).then(work2).enqueue().getResult().get();

        WorkTagDao workTagDao = mDatabase.workTagDao();
        assertThat(workTagDao.getWorkSpecIdsWithTag(firstTag),
                containsInAnyOrder(work0.getStringId(), work1.getStringId()));
        assertThat(workTagDao.getWorkSpecIdsWithTag(secondTag),
                containsInAnyOrder(work0.getStringId()));
        assertThat(workTagDao.getWorkSpecIdsWithTag(thirdTag), emptyCollectionOf(String.class));
    }

    @Test
    @MediumTest
    public void testEnqueue_insertPeriodicWork() throws ExecutionException, InterruptedException {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                MILLISECONDS)
                .build();

        mWorkManagerImpl.enqueue(Collections.singletonList(periodicWork))
                .getResult()
                .get();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(periodicWork.getStringId());
        assertThat(workSpec.isPeriodic(), is(true));
        assertThat(workSpec.intervalDuration, is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(workSpec.flexDuration, is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Test
    @MediumTest
    public void testEnqueued_work_setsPeriodStartTime()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        assertThat(work.getWorkSpec().lastEnqueueTime, is(-1L));

        long beforeEnqueueTime = System.currentTimeMillis();

        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.lastEnqueueTime, is(greaterThanOrEqualTo(beforeEnqueueTime)));
    }

    @Test
    @MediumTest
    public void testEnqueued_periodicWork_setsPeriodStartTime()
            throws ExecutionException, InterruptedException {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                MILLISECONDS)
                .build();
        assertThat(periodicWork.getWorkSpec().lastEnqueueTime, is(-1L));
        // Disable the greedy scheduler in this test. This is because sometimes the Worker
        // finishes instantly after enqueue(), and the periodStartTime gets updated.
        doNothing().when(mScheduler).schedule(any(WorkSpec.class));
        long beforeEnqueueTime = System.currentTimeMillis();

        mWorkManagerImpl.enqueue(Collections.singletonList(periodicWork))
                .getResult()
                .get();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(periodicWork.getStringId());
        assertThat(workSpec.lastEnqueueTime, is(greaterThanOrEqualTo(beforeEnqueueTime)));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_setsUniqueName()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(TestWorker.class).build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, REPLACE, work)
                .then(next)
                .enqueue().getResult()
                .get();

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(work.getStringId(), isIn(workSpecIds));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_setsUniqueName()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                15L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork).getResult().get();

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(periodicWork.getStringId(), isIn(workSpecIds));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_deletesOldWorkOnReplace()
            throws ExecutionException, InterruptedException {

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
                .enqueue().getResult().get();

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
    @MediumTest
    public void testEnqueueUniquePeriodicWork_deletesOldWorkOnReplace()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                MINUTES)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.REPLACE,
                replacementWork).getResult().get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(replacementWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(not(nullValue())));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_keepsExistingWorkOnKeep()
            throws ExecutionException, InterruptedException {

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
                .enqueue()
                .getResult()
                .get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(originalWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork1.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork2.getStringId()), is(nullValue()));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_keepsExistingWorkOnKeep()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                MINUTES)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                replacementWork).getResult().get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(originalWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(nullValue()));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_replacesExistingWorkOnKeepWhenExistingWorkIsDone()
            throws ExecutionException, InterruptedException {

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
                .enqueue().getResult().get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(replacementWork1.getStringId(), replacementWork2.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(replacementWork2.getStringId()), is(not(nullValue())));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_replacesExistingWorkOnKeepWhenExistingWorkIsDone()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                MINUTES)
                .setInitialState(SUCCEEDED)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                replacementWork).getResult().get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(replacementWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(originalWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(replacementWork.getStringId()), is(not(nullValue())));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_update()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "myname";
        long enqueueTime = System.currentTimeMillis();
        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                MINUTES)
                .setLastEnqueueTime(enqueueTime, MILLISECONDS)
                .setInitialState(ENQUEUED)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                replacementWork).getResult().get();

        workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, contains(originalWork.getStringId()));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        WorkSpec workSpec = workSpecDao.getWorkSpec(originalWork.getStringId());
        assertThat(workSpec.lastEnqueueTime, is(enqueueTime));
        assertThat(workSpec.intervalDuration, is(MINUTES.toMillis(30)));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_updateCancelled()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "myname";
        PeriodicWorkRequest originalWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                15L,
                MINUTES)
                .setInitialState(CANCELLED)
                .build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                replacementWork).getResult().get();
        assertThat(mWorkManagerImpl.getWorkDatabase().workSpecDao()
                .getWorkSpec(replacementWork.getStringId()).state, is(ENQUEUED));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_updateNonExistent()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "myname";
        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        mWorkManagerImpl.enqueueUniquePeriodicWork(
                uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                replacementWork).getResult().get();
        assertThat(mWorkManagerImpl.getWorkDatabase().workSpecDao()
                .getWorkSpec(replacementWork.getStringId()).state, is(ENQUEUED));
    }

    @Test
    @MediumTest
    public void testEnqueueUniquePeriodicWork_updateOneTimeWork()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "myname";
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.enqueueUniqueWork(uniqueName, KEEP, request).getResult().get();

        PeriodicWorkRequest replacementWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                30L,
                MINUTES)
                .build();
        try {
            mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                    ExistingPeriodicWorkPolicy.UPDATE, replacementWork).getResult().get();
            throw new AssertionError("Update should have failed");
        } catch (ExecutionException e) {
            assertThat(e.getCause(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test
    @SmallTest
    public void testOverrideNextSchedule_selfRescheduleWorker_update()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "TestWorker";
        ListenableWorker testWorker = mock(ListenableWorker.class);
        setupTestOverrideScheduleWorker(uniqueName, testWorker,
                ExistingPeriodicWorkPolicy.UPDATE);

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mConfiguration.getWorkerFactory().createWorker(any(),
                eq(ListenableWorker.class.getName()), any())).thenReturn(testWorker);
        mClock.currentTimeMillis = TimeUnit.HOURS.toMillis(1);

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(ListenableWorker.class, 1, TimeUnit.HOURS)
                        .build();

        // Runs immediately (periodic worker first run is instant)
        mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE, periodicWorkRequest).getResult().get();

        // Next run should be scheduled for T=10 hours, according to the Worker's override.
        // Uses the same Id as before.
        WorkInfo workInfo = mWorkManagerImpl
                .getWorkInfoById(periodicWorkRequest.getId()).get();
        assertThat(
                workInfo.getNextScheduleTimeMillis(),
                equalTo(TimeUnit.HOURS.toMillis(10)));

        // Previous run completed successfully.
        assertThat(
                workInfo.getRunAttemptCount(),
                equalTo(0));
    }

    @Test
    @SmallTest
    public void testOverrideNextSchedule_cancelAndReenqueue_fails()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "TestWorker";
        ListenableWorker testWorker = mock(ListenableWorker.class);

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mConfiguration.getWorkerFactory().createWorker(any(),
                eq(ListenableWorker.class.getName()), any())).thenReturn(testWorker);
        mClock.currentTimeMillis = TimeUnit.HOURS.toMillis(1);

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(ListenableWorker.class, 1, TimeUnit.HOURS)
                        .setNextScheduleTimeOverride(123456)
                        .build();

        assertThrows(IllegalArgumentException.class, () ->
                mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                        periodicWorkRequest).getResult().get());

        assertThrows(IllegalArgumentException.class, () ->
                mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        periodicWorkRequest).getResult().get());
    }
    @Test
    @SmallTest
    public void testOverrideNextSchedule_update_allowed()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "TestWorker";
        ListenableWorker testWorker = mock(ListenableWorker.class);

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mConfiguration.getWorkerFactory().createWorker(any(),
                eq(ListenableWorker.class.getName()), any())).thenReturn(testWorker);
        mClock.currentTimeMillis = TimeUnit.HOURS.toMillis(1);

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(ListenableWorker.class, 1, TimeUnit.HOURS)
                        .setNextScheduleTimeOverride(123456)
                        .build();

        mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest).getResult().get();
    }

    @Test
    @SmallTest
    public void testOverrideNextSchedule_keep_allowed()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "TestWorker";
        ListenableWorker testWorker = mock(ListenableWorker.class);

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mConfiguration.getWorkerFactory().createWorker(any(),
                eq(ListenableWorker.class.getName()), any())).thenReturn(testWorker);
        mClock.currentTimeMillis = TimeUnit.HOURS.toMillis(1);

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(ListenableWorker.class, 1, TimeUnit.HOURS)
                        .setNextScheduleTimeOverride(123456)
                        .build();

        // KEEP isn't very useful, but it works.
        mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest).getResult().get();
    }

    @Test
    @SmallTest
    public void testOverrideNextSchedule_selfRescheduleWorker_keep()
            throws ExecutionException, InterruptedException {
        final String uniqueName = "TestWorker";
        ListenableWorker testWorker = mock(ListenableWorker.class);
        setupTestOverrideScheduleWorker(uniqueName, testWorker,
                ExistingPeriodicWorkPolicy.KEEP);

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mConfiguration.getWorkerFactory().createWorker(any(),
                eq(ListenableWorker.class.getName()), any())).thenReturn(testWorker);
        mClock.currentTimeMillis = TimeUnit.HOURS.toMillis(1);

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(ListenableWorker.class, 1, TimeUnit.HOURS)
                        .build();

        // Runs immediately (periodic worker first run is instant)
        mWorkManagerImpl.enqueueUniquePeriodicWork(uniqueName,
                ExistingPeriodicWorkPolicy.UPDATE, periodicWorkRequest).getResult().get();

        // KEEP meant that the override wasn't changed. It was cleared when the Worker finished.
        WorkInfo workInfo = mWorkManagerImpl
                .getWorkInfoById(periodicWorkRequest.getId()).get();
        assertThat(
                workInfo.getNextScheduleTimeMillis(),
                equalTo(TimeUnit.HOURS.toMillis(2)));

        // Previous run completed successfully.
        assertThat(
                workInfo.getRunAttemptCount(),
                equalTo(0));
    }

    private void setupTestOverrideScheduleWorker(String uniqueName, ListenableWorker testWorker,
            ExistingPeriodicWorkPolicy existingPolicy) {
        when(testWorker.startWork()).thenAnswer(
                invocation -> {
                    PeriodicWorkRequest request =
                            new PeriodicWorkRequest.Builder(
                                    ListenableWorker.class, 1, TimeUnit.HOURS)
                                    .setNextScheduleTimeOverride(
                                            TimeUnit.HOURS.toMillis(10)).build();
                    return Futures.transform(
                            WorkManager.getInstance(mContext)
                                    .enqueueUniquePeriodicWork(
                                            uniqueName,
                                            existingPolicy,
                                            request
                                    ).getResult(),
                            (input) -> ListenableWorker.Result.Success.success(),
                            new SynchronousExecutor()
                    );
                }
        );
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_appendsExistingWorkOnAppend()
            throws ExecutionException, InterruptedException {

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
                .enqueue().getResult().get();

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
    @MediumTest
    public void testEnqueueUniqueWork_appendsExistingWorkOnAppend()
            throws ExecutionException, InterruptedException {
        // Not duplicating other enqueueUniqueWork with different work policies as they
        // call the same underlying continuation which have tests. This test exists to ensure
        // we delegate to the underlying continuation correctly.

        final String uniqueName = "myname";

        OneTimeWorkRequest originalWork =
                new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, originalWork);

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds, containsInAnyOrder(originalWork.getStringId()));

        OneTimeWorkRequest appendWork1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest appendWork2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl.enqueueUniqueWork(
                uniqueName,
                APPEND,
                Arrays.asList(appendWork1, appendWork2)).getResult().get();
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
                containsInAnyOrder(appendWork1.getStringId(), appendWork2.getStringId()));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_appendsExistingWorkToOnlyLeavesOnAppend()
            throws ExecutionException, InterruptedException {

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
                .enqueue().getResult().get();

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
    @MediumTest
    public void testBeginUniqueWork_insertsWithAppendOrReplaceWithFailedPreRequisites()
            throws ExecutionException, InterruptedException {

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.<Scheduler>emptyList());
        final String uniqueName = "myname";
        OneTimeWorkRequest preRequisiteRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(FAILED)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, preRequisiteRequest)
                .enqueue()
                .getResult()
                .get();

        OneTimeWorkRequest appendRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, appendRequest)
                .enqueue()
                .getResult()
                .get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        DependencyDao dependencyDao = mDatabase.dependencyDao();

        WorkSpec deleted = workSpecDao.getWorkSpec(preRequisiteRequest.getStringId());
        assertThat(deleted, is(nullValue()));
        WorkSpec appended = workSpecDao.getWorkSpec(appendRequest.getStringId());
        List<String> preRequisites = dependencyDao.getPrerequisites(appendRequest.getStringId());
        assertThat(appended.state, is(ENQUEUED));
        assertThat(preRequisites.size(), is(0));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_insertsWithAppendOrReplaceWithCancelledPreRequisites()
            throws ExecutionException, InterruptedException {

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.<Scheduler>emptyList());
        final String uniqueName = "myname";
        OneTimeWorkRequest preRequisiteRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(CANCELLED)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, preRequisiteRequest)
                .enqueue()
                .getResult()
                .get();

        OneTimeWorkRequest appendRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, appendRequest)
                .enqueue()
                .getResult()
                .get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        DependencyDao dependencyDao = mDatabase.dependencyDao();

        WorkSpec deleted = workSpecDao.getWorkSpec(preRequisiteRequest.getStringId());
        assertThat(deleted, is(nullValue()));
        WorkSpec appended = workSpecDao.getWorkSpec(appendRequest.getStringId());
        List<String> preRequisites = dependencyDao.getPrerequisites(appendRequest.getStringId());
        assertThat(appended.state, is(ENQUEUED));
        assertThat(preRequisites.size(), is(0));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_appendsWork_whenUsingAppendOrReplace()
            throws ExecutionException, InterruptedException {

        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.<Scheduler>emptyList());
        final String uniqueName = "myname";
        OneTimeWorkRequest preRequisiteRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, preRequisiteRequest)
                .enqueue()
                .getResult()
                .get();

        OneTimeWorkRequest appendRequest = new OneTimeWorkRequest.Builder(TestWorker.class)
                .build();

        mWorkManagerImpl.beginUniqueWork(uniqueName, APPEND_OR_REPLACE, appendRequest)
                .enqueue()
                .getResult()
                .get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        DependencyDao dependencyDao = mDatabase.dependencyDao();

        WorkSpec preRequisite = workSpecDao.getWorkSpec(preRequisiteRequest.getStringId());
        WorkSpec appended = workSpecDao.getWorkSpec(appendRequest.getStringId());
        List<String> preRequisites = dependencyDao.getPrerequisites(appendRequest.getStringId());
        assertThat(appended.state, is(BLOCKED));
        assertThat(preRequisites.size(), is(1));
        assertThat(preRequisites, containsInAnyOrder(preRequisite.id));
    }

    @Test
    @MediumTest
    public void testBeginUniqueWork_insertsExistingWorkWhenNothingToAppendTo()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        OneTimeWorkRequest appendWork1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest appendWork2 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        mWorkManagerImpl
                .beginUniqueWork(uniqueName, APPEND, appendWork1)
                .then(appendWork2)
                .enqueue().getResult().get();

        List<String> workSpecIds = mDatabase.workNameDao().getWorkSpecIdsWithName(uniqueName);
        assertThat(workSpecIds,
                containsInAnyOrder(appendWork1.getStringId(), appendWork2.getStringId()));
    }

    @Test
    @MediumTest
    public void testGetWorkInfoByIdSync() throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        insertWorkSpecAndTags(work);

        WorkInfo workInfo = mWorkManagerImpl.getWorkInfoById(work.getId()).get();
        assertThat(workInfo.getId().toString(), is(work.getStringId()));
        assertThat(workInfo.getState(), is(SUCCEEDED));
    }

    @Test
    @MediumTest
    public void testGetWorkInfoByIdSync_constraints() throws Exception {
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(CONNECTED)
                .build();
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setConstraints(constraints)
                .build();
        insertWorkSpecAndTags(work);

        WorkInfo workInfo = mWorkManagerImpl.getWorkInfoById(work.getId()).get();
        assertThat(workInfo.getId().toString(), is(work.getStringId()));
        assertThat(workInfo.getConstraints(), equalTo(constraints));
    }

    @Test
    @MediumTest
    public void testGetWorkInfoByIdSync_oneTime_schedules() throws Exception {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setInitialDelay(1234, MILLISECONDS)
                .build();
        insertWorkSpecAndTags(work);

        WorkInfo workInfo = mWorkManagerImpl.getWorkInfoById(work.getId()).get();
        assertThat(workInfo.getId().toString(), is(work.getStringId()));
        assertThat(workInfo.getInitialDelayMillis(), equalTo(1234L));
        assertThat(workInfo.getPeriodicityInfo(), is(nullValue()));
    }

    @Test
    @MediumTest
    public void testGetWorkInfoByIdSync_periodic_schedules() throws Exception {
        long repeatIntervalMillis = MINUTES.toMillis(60);
        long flexIntervalMillis = MINUTES.toMillis(30);

        PeriodicWorkRequest work =
                new PeriodicWorkRequest.Builder(TestWorker.class,
                        repeatIntervalMillis, MILLISECONDS,
                        flexIntervalMillis, MILLISECONDS)
                        .setInitialState(SUCCEEDED)
                        .setInitialDelay(1234, MILLISECONDS)
                        .build();
        insertWorkSpecAndTags(work);

        WorkInfo workInfo = mWorkManagerImpl.getWorkInfoById(work.getId()).get();
        assertThat(workInfo.getId().toString(), is(work.getStringId()));
        assertThat(workInfo.getInitialDelayMillis(), equalTo(1234L));
        assertThat(workInfo.getPeriodicityInfo().getRepeatIntervalMillis(), equalTo(
                repeatIntervalMillis));
        assertThat(workInfo.getPeriodicityInfo().getFlexIntervalMillis(), equalTo(
                flexIntervalMillis));
    }

    @Test
    @MediumTest
    public void testGetWorkInfoByIdSync_returnsNullIfNotInDatabase()
            throws ExecutionException, InterruptedException {

        WorkInfo workInfo = mWorkManagerImpl.getWorkInfoById(UUID.randomUUID()).get();
        assertThat(workInfo, is(nullValue()));
    }

    @Test
    @MediumTest
    @SuppressWarnings("unchecked")
    public void testGetWorkInfoById() {
        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);

        Observer<List<WorkInfo>> mockObserver = mock(Observer.class);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<List<WorkInfo>> liveData = mWorkManagerImpl.getWorkInfosById(
                Arrays.asList(work0.getStringId(), work1.getStringId()));
        liveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<List<WorkInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        WorkInfo workInfo0 = createWorkInfo(
                work0.getId(),
                ENQUEUED,
                Collections.singletonList(TestWorker.class.getName()));
        WorkInfo workInfo1 = createWorkInfo(
                work1.getId(),
                ENQUEUED,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workInfo0, workInfo1));

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        workSpecDao.setState(RUNNING, work0.getStringId());

        verify(mockObserver, times(2)).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        workInfo0 = createWorkInfo(
                work0.getId(),
                RUNNING,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workInfo0, workInfo1));

        clearInvocations(mockObserver);
        workSpecDao.setState(RUNNING, work1.getStringId());

        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(2));

        workInfo1 = createWorkInfo(
                work1.getId(),
                RUNNING,
                Collections.singletonList(TestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workInfo0, workInfo1));

        liveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @SmallTest
    public void testGetWorkInfoById_nextScheduleTime_notEnqueued()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().state = RUNNING;
        work.getWorkSpec().lastEnqueueTime = 1000L;
        insertWorkSpecAndTags(work);

        WorkInfo info = mWorkManagerImpl.getWorkInfoById(work.getId()).get();

        assertThat(info.getState(), equalTo(RUNNING));
        assertThat(info.getNextScheduleTimeMillis(), equalTo(Long.MAX_VALUE));
    }

    @Test
    @SmallTest
    public void testGetWorkInfoById_nextScheduleTime_enqueued()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        work.getWorkSpec().lastEnqueueTime = 1000L;
        insertWorkSpecAndTags(work);

        WorkInfo info = mWorkManagerImpl.getWorkInfoById(work.getId()).get();

        assertThat(info.getState(), equalTo(ENQUEUED));
        assertThat(info.getNextScheduleTimeMillis(),
                equalTo(1000L));
    }

    @Test
    @SmallTest
    public void testGetWorkInfoById_nextScheduleTime_onetime_initialDelay()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).setInitialDelay(
                2000, MILLISECONDS).build();
        work.getWorkSpec().lastEnqueueTime = 1000L;
        insertWorkSpecAndTags(work);

        WorkInfo info = mWorkManagerImpl.getWorkInfoById(work.getId()).get();

        assertThat(info.getState(), equalTo(ENQUEUED));
        assertThat(info.getNextScheduleTimeMillis(),
                equalTo(3000L));
    }

    @Test
    @SmallTest
    public void testGetWorkInfoById_nextScheduleTime_periodic_period()
            throws ExecutionException, InterruptedException {
        long periodMillis = MINUTES.toMillis(15);
        long initialDelayMillis = 2000L;
        long lastEnqueueTimeMillis = 1000L;

        PeriodicWorkRequest work0 = new PeriodicWorkRequest.Builder(
                TestWorker.class, periodMillis, MILLISECONDS)
                .setInitialDelay(initialDelayMillis, MILLISECONDS)
                .build();

        work0.getWorkSpec().lastEnqueueTime = lastEnqueueTimeMillis;
        work0.getWorkSpec().setPeriodCount(3);
        insertWorkSpecAndTags(work0);

        WorkInfo info = mWorkManagerImpl.getWorkInfoById(work0.getId()).get();

        assertThat(info.getState(), equalTo(ENQUEUED));
        assertThat(info.getNextScheduleTimeMillis(),
                equalTo(lastEnqueueTimeMillis + periodMillis));
        assertThat(info.getInitialDelayMillis(), equalTo(initialDelayMillis));
    }

    @Test
    @MediumTest
    public void testGetWorkInfosByTagSync() throws ExecutionException, InterruptedException {
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

        WorkInfo workInfo0 = createWorkInfo(
                work0.getId(),
                RUNNING,
                Arrays.asList(TestWorker.class.getName(), firstTag, secondTag));
        WorkInfo workInfo1 = createWorkInfo(
                work1.getId(),
                BLOCKED,
                Arrays.asList(TestWorker.class.getName(), firstTag));
        WorkInfo workInfo2 = createWorkInfo(
                work2.getId(),
                SUCCEEDED,
                Arrays.asList(TestWorker.class.getName(), secondTag));

        List<WorkInfo> workInfos = mWorkManagerImpl.getWorkInfosByTag(firstTag).get();
        assertThat(workInfos, containsInAnyOrder(workInfo0, workInfo1));

        workInfos = mWorkManagerImpl.getWorkInfosByTag(secondTag).get();
        assertThat(workInfos, containsInAnyOrder(workInfo0, workInfo2));

        workInfos = mWorkManagerImpl.getWorkInfosByTag("dummy").get();
        assertThat(workInfos.size(), is(0));
    }

    @Test
    @MediumTest
    public void getWorkInfosByNameSync() throws ExecutionException, InterruptedException {
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

        WorkInfo workInfo0 = createWorkInfo(
                work0.getId(),
                RUNNING,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkInfo workInfo1 = createWorkInfo(
                work1.getId(),
                BLOCKED,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkInfo workInfo2 = createWorkInfo(
                work2.getId(),
                BLOCKED,
                Collections.singletonList(InfiniteTestWorker.class.getName()));

        List<WorkInfo> workInfos = mWorkManagerImpl.getWorkInfosForUniqueWork(uniqueName).get();
        assertThat(workInfos, containsInAnyOrder(workInfo0, workInfo1, workInfo2));

        workInfos = mWorkManagerImpl.getWorkInfosForUniqueWork("dummy").get();
        assertThat(workInfos.size(), is(0));
    }

    @Test
    @MediumTest
    @SuppressWarnings("unchecked")
    public void testGetWorkInfosByName() {
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

        Observer<List<WorkInfo>> mockObserver = mock(Observer.class);

        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();
        LiveData<List<WorkInfo>> liveData =
                mWorkManagerImpl.getWorkInfosForUniqueWorkLiveData(uniqueName);
        liveData.observe(testLifecycleOwner, mockObserver);

        ArgumentCaptor<List<WorkInfo>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockObserver).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(3));

        WorkInfo workInfo0 = createWorkInfo(
                work0.getId(),
                RUNNING,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkInfo workInfo1 = createWorkInfo(
                work1.getId(),
                BLOCKED,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        WorkInfo workInfo2 = createWorkInfo(
                work2.getId(),
                BLOCKED,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workInfo0, workInfo1, workInfo2));

        workSpecDao.setState(ENQUEUED, work0.getStringId());

        verify(mockObserver, times(2)).onChanged(captor.capture());
        assertThat(captor.getValue(), is(not(nullValue())));
        assertThat(captor.getValue().size(), is(3));

        workInfo0 = createWorkInfo(
                work0.getId(),
                ENQUEUED,
                Collections.singletonList(InfiniteTestWorker.class.getName()));
        assertThat(captor.getValue(), containsInAnyOrder(workInfo0, workInfo1, workInfo2));

        liveData.removeObservers(testLifecycleOwner);
    }

    @Test
    @MediumTest
    public void testCancelWorkById() throws ExecutionException, InterruptedException {
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);

        mWorkManagerImpl.cancelWorkById(work0.getId()).getResult().get();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(not(CANCELLED)));
    }

    @Test
    @SmallTest
    public void testCancelWorkById_cancelsDependentWork()
            throws ExecutionException, InterruptedException {

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(BLOCKED)
                .build();
        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertDependency(work1, work0);

        mWorkManagerImpl.cancelWorkById(work0.getId()).getResult().get();

        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @MediumTest
    public void testCancelWorkById_cancelsUnfinishedWorkOnly()
            throws ExecutionException, InterruptedException {

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

        mWorkManagerImpl.cancelWorkById(work0.getId()).getResult().get();

        assertThat(workSpecDao.getState(work0.getStringId()), is(SUCCEEDED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @LargeTest
    public void testCancelAllWorkByTag() throws ExecutionException, InterruptedException {
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

        mWorkManagerImpl.cancelAllWorkByTag(tagToClear).getResult().get();

        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(not(CANCELLED)));
        assertThat(workSpecDao.getState(work3.getStringId()), is(not(CANCELLED)));
    }

    @Test
    @LargeTest
    public void testCancelAllWorkByTag_cancelsDependentWork()
            throws ExecutionException, InterruptedException {

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

        mWorkManagerImpl.cancelAllWorkByTag(tag).getResult().get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work3.getStringId()), is(not(CANCELLED)));
        assertThat(workSpecDao.getState(work4.getStringId()), is(CANCELLED));
    }

    @Test
    @MediumTest
    public void testCancelWorkByName() throws ExecutionException, InterruptedException {
        final String uniqueName = "myname";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, work0, work1);

        mWorkManagerImpl.cancelUniqueWork(uniqueName).getResult().get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @LargeTest
    public void testCancelWorkByName_ignoresFinishedWork()
            throws ExecutionException, InterruptedException {

        final String uniqueName = "myname";

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialState(SUCCEEDED)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertNamedWorks(uniqueName, work0, work1);

        mWorkManagerImpl.cancelUniqueWork(uniqueName).getResult().get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work0.getStringId()), is(SUCCEEDED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
    }

    @Test
    @LargeTest
    public void testCancelAllWork() throws ExecutionException, InterruptedException {
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

        mWorkManagerImpl.cancelAllWork().getResult().get();
        assertThat(workSpecDao.getState(work0.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work1.getStringId()), is(CANCELLED));
        assertThat(workSpecDao.getState(work2.getStringId()), is(SUCCEEDED));
    }

    @Test
    @LargeTest
    public void testCancelAllWork_updatesLastCancelAllTime() {
        PreferenceUtils preferenceUtils = new PreferenceUtils(mWorkManagerImpl.getWorkDatabase());
        preferenceUtils.setLastCancelAllTimeMillis(0L);

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work);

        CancelWorkRunnable.forAll(mWorkManagerImpl);
        assertThat(preferenceUtils.getLastCancelAllTimeMillis(), is(greaterThan(0L)));
    }

    @Test
    @MediumTest
    public void pruneFinishedWork() throws InterruptedException, ExecutionException {
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

        mWorkManagerImpl.pruneWork().getResult().get();

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(enqueuedWork.getStringId()), is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWork.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWorkWithUnfinishedDependent.getStringId()),
                is(notNullValue()));
        assertThat(workSpecDao.getWorkSpec(finishedWorkWithLongKeepForAtLeast.getStringId()),
                is(nullValue()));
    }

    @Test
    @MediumTest
    public void testSynchronousCancelAndGetWorkInfo()
            throws ExecutionException, InterruptedException {

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWorkSpecAndTags(work);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getStringId()), is(ENQUEUED));

        mWorkManagerImpl.cancelWorkById(work.getId()).getResult().get();
        assertThat(mWorkManagerImpl.getWorkInfoById(work.getId()).get().getState(), is(CANCELLED));
    }

    @Test
    @MediumTest
    public void testForceStopRunnable_resetsRunningWorkStatuses() {
        when(mWorkManagerImpl.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        WorkSpecDao workSpecDao = mDatabase.workSpecDao();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(RUNNING)
                .build();
        workSpecDao.insertWorkSpec(work.getWorkSpec());

        assertThat(workSpecDao.getState(work.getStringId()), is(RUNNING));

        ForceStopRunnable runnable = new ForceStopRunnable(mContext, mWorkManagerImpl);
        runnable.run();

        assertThat(workSpecDao.getWorkSpec(work.getStringId()).scheduleRequestedAt,
                is(not(SCHEDULE_NOT_REQUESTED_YET)));
    }

    @Test
    @MediumTest
    public void testGenerateCleanupCallback_deletesOldFinishedWork() {
        long nowMillis = TimeUnit.DAYS.toMillis(30);
        mClock.currentTimeMillis = nowMillis;

        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setLastEnqueueTime(nowMillis - WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS - 1L,
                        MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setLastEnqueueTime(nowMillis - WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS + 1L,
                        MILLISECONDS)
                .build();

        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();

        new CleanupCallback(mClock).onOpen(db);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(not(nullValue())));
    }

    @Test
    @MediumTest
    public void testGenerateCleanupCallback_doesNotDeleteOldFinishedWorkWithActiveDependents() {
        long nowMillis = TimeUnit.DAYS.toMillis(30);
        mClock.currentTimeMillis = nowMillis;

        OneTimeWorkRequest work0 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setLastEnqueueTime(nowMillis - WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS - 1L,
                        MILLISECONDS)
                .build();
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(SUCCEEDED)
                .setLastEnqueueTime(nowMillis - WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS - 1L,
                        MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setInitialState(ENQUEUED)
                .setLastEnqueueTime(nowMillis - WorkDatabaseKt.PRUNE_THRESHOLD_MILLIS - 1L,
                        MILLISECONDS)
                .build();

        insertWorkSpecAndTags(work0);
        insertWorkSpecAndTags(work1);
        insertWorkSpecAndTags(work2);

        // Dependency graph: 0 -> 1 -> 2
        insertDependency(work1, work0);
        insertDependency(work2, work1);

        SupportSQLiteOpenHelper openHelper = mDatabase.getOpenHelper();
        SupportSQLiteDatabase db = openHelper.getWritableDatabase();

        new CleanupCallback(mClock).onOpen(db);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getWorkSpec(work0.getStringId()), is(nullValue()));
        assertThat(workSpecDao.getWorkSpec(work1.getStringId()), is(not(nullValue())));
        assertThat(workSpecDao.getWorkSpec(work2.getStringId()), is(not(nullValue())));
    }

    @Test
    @LargeTest
    public void testEnableDisableRescheduleReceiver()
            throws ExecutionException, InterruptedException {

        final PackageManager packageManager = mock(PackageManager.class);
        mContext = new ContextWrapper(ApplicationProvider.getApplicationContext()) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public PackageManager getPackageManager() {
                return packageManager;
            }
        };
        InstantWorkTaskExecutor workTaskExecutor = new InstantWorkTaskExecutor();
        Processor processor = new Processor(mContext, mConfiguration, workTaskExecutor, mDatabase);
        WorkLauncherImpl launcher = new WorkLauncherImpl(processor, workTaskExecutor);

        Trackers trackers = mWorkManagerImpl.getTrackers();
        Scheduler scheduler = new GreedyScheduler(mContext, mWorkManagerImpl.getConfiguration(),
                        trackers, processor, launcher, workTaskExecutor);
        mWorkManagerImpl =  createWorkManager(mContext, mConfiguration, workTaskExecutor,
                mDatabase, trackers, processor, schedulers(scheduler));

        WorkManagerImpl.setDelegate(mWorkManagerImpl);
        mDatabase = mWorkManagerImpl.getWorkDatabase();
        // Initialization of WM enables SystemJobService which needs to be discounted.
        reset(packageManager);
        OneTimeWorkRequest stopAwareWorkRequest =
                new OneTimeWorkRequest.Builder(StopAwareWorker.class)
                        .build();

        mWorkManagerImpl.enqueue(Collections.singletonList(stopAwareWorkRequest))
                .getResult().get();

        ComponentName componentName = new ComponentName(mContext, RescheduleReceiver.class);
        verify(packageManager, times(1))
                .setComponentEnabledSetting(eq(componentName),
                        eq(PackageManager.COMPONENT_ENABLED_STATE_ENABLED),
                        eq(PackageManager.DONT_KILL_APP));

        reset(packageManager);

        // Scheduling work involved enabling RescheduleReceiver.class.
        // Mark the component state as enabled (so we can check for disablement).
        // This is necessary because we are using a PackageManager mock here.
        when(packageManager.getComponentEnabledSetting(eq(componentName)))
                .thenReturn(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        mWorkManagerImpl.cancelWorkById(stopAwareWorkRequest.getId())
                .getResult()
                .get();
        // Sleeping for a little bit, to give the listeners a chance to catch up.
        Thread.sleep(SLEEP_DURATION_SMALL_MILLIS);
        // There is a small chance that we will call this method twice. Once when the Worker was
        // cancelled, and once after the StopAwareWorker realizes that it has been stopped
        // and returns a Result.SUCCESS
        verify(packageManager, atLeastOnce())
                .setComponentEnabledSetting(eq(componentName),
                        eq(PackageManager.COMPONENT_ENABLED_STATE_DISABLED),
                        eq(PackageManager.DONT_KILL_APP));

    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 25)
    public void testEnqueueApi23To25_withBatteryNotLowConstraint_expectsConstraintTrackingWorker()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(ConstraintTrackingWorker.class.getName()));
        assertThat(workSpec.input.getString(
                        ConstraintTrackingWorkerKt.ARGUMENT_CLASS_NAME),
                is(TestWorker.class.getName()));
    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 25)
    public void testEnqueueApi23To25_withStorageNotLowConstraint_expectsConstraintTrackingWorker()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(ConstraintTrackingWorker.class.getName()));
        assertThat(workSpec.input.getString(ARGUMENT_CLASS_NAME),
                is(TestWorker.class.getName()));
    }

    @Test
    @MediumTest
    @SdkSuppress(maxSdkVersion = 25)
    public void testEnqueueApi23To25_withConstraintTrackingWorker_expectsOriginalWorker()
            throws ExecutionException, InterruptedException {
        Data data = new Data.Builder()
                .put(ARGUMENT_CLASS_NAME, TestWorker.class.getName())
                .build();

        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ConstraintTrackingWorker.class)
                .setInputData(data)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();

        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(ConstraintTrackingWorker.class.getName()));
        assertThat(workSpec.input.getString(ARGUMENT_CLASS_NAME),
                is(TestWorker.class.getName()));
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = 26)
    public void testEnqueueApi26OrHigher_withBatteryNotLowConstraint_expectsOriginalWorker()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 26)
    public void testEnqueueApi26OrHigher_withStorageNotLowConstraint_expectsOriginalWorker()
            throws ExecutionException, InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(new Constraints.Builder()
                        .setRequiresStorageNotLow(true)
                        .build())
                .build();
        mWorkManagerImpl.beginWith(work).enqueue().getResult().get();
        WorkSpec workSpec = mDatabase.workSpecDao().getWorkSpec(work.getStringId());
        assertThat(workSpec.workerClassName, is(TestWorker.class.getName()));
    }

    // TODO (rahulrav@)  Before this test can be added to this test suite, we need to clean up our
    // TaskExecutor so it's not a singleton.
    // Right now, these tests fail because we don't seem to clean up correctly.

    /*
    @Test
    @MediumTest
    @RepeatRule.Repeat(times = 10)
    @SdkSuppress(maxSdkVersion = 22)    // We can't force JobScheduler to run quicker than 15 mins.
    public void testPeriodicWork_ExecutesRepeatedly() throws InterruptedException {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                15,
                TimeUnit.MINUTES)
                .build();
        WorkSpec workSpec = work.getWorkSpec();
        workSpec.intervalDuration = 100L; // Manually override this to a smaller value for tests.
        workSpec.flexDuration = 0L;         // Manually override this to a smaller value for tests.

        final CountDownLatch latch = new CountDownLatch(3);
        TestLifecycleOwner testLifecycleOwner = new TestLifecycleOwner();

        LiveData<WorkInfo> status = mWorkManagerImpl.getWorkInfoByIdLiveData(work.getId());
        status.observe(testLifecycleOwner, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workStatus) {
                if (workStatus != null) {
                    if (workStatus.getState() == RUNNING) {
                        latch.countDown();
                    }
                }
            }
        });

        mWorkManagerImpl.enqueueSync(work);
        // latch.await();
        latch.await(20, TimeUnit.SECONDS);
        assertThat(latch.getCount(), is(0L));
        status.removeObservers(testLifecycleOwner);
        mWorkManagerImpl.cancelWorkById(work.getId());
    }
    */

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

    private static @NonNull WorkInfo createWorkInfo(UUID id, WorkInfo.State state,
            List<String> tags) {
        return new WorkInfo(
                id, state, new HashSet<>(tags), Data.EMPTY, Data.EMPTY, 0, 0,
                Constraints.NONE, 0, null,
                Long.MAX_VALUE // Documented error value.
        );
    }
}
