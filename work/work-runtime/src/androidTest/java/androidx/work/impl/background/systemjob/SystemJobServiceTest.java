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

package androidx.work.impl.background.systemjob;

import static android.app.job.JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.work.impl.WorkManagerImplExtKt.createWorkManager;
import static androidx.work.impl.WorkManagerImplExtKt.schedulers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Instrumentation;
import android.app.job.JobParameters;
import android.content.Context;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.RequiresApi;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManagerTest;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.Processor;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.constraints.trackers.Trackers;
import androidx.work.impl.model.WorkSpecDao;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.worker.InfiniteTestWorker;
import androidx.work.worker.NeverResolvedWorker;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class SystemJobServiceTest extends WorkManagerTest {

    private Instrumentation mInstrumentation = InstrumentationRegistry.getInstrumentation();
    private WorkManagerImpl mWorkManagerImpl;
    private Processor mProcessor;
    private WorkDatabase mDatabase;
    private SystemJobService mSystemJobServiceSpy;
    private Scheduler mScheduler;

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

        Context context = ApplicationProvider.getApplicationContext();
        Configuration configuration = new Configuration.Builder()
                .setExecutor(Executors.newSingleThreadExecutor())
                .build();
        mDatabase = WorkDatabase.create(
                context, Executors.newCachedThreadPool(), configuration.getClock(), true);
        InstantWorkTaskExecutor taskExecutor = new InstantWorkTaskExecutor();
        mScheduler = mock(Scheduler.class);
        mProcessor = new Processor(
                context,
                configuration,
                taskExecutor,
                mDatabase);

        mWorkManagerImpl = createWorkManager(context, configuration, taskExecutor,
                mDatabase, new Trackers(context, taskExecutor), mProcessor, schedulers(mScheduler));
        WorkManagerImpl.setDelegate(mWorkManagerImpl);
        mSystemJobServiceSpy = spy(new SystemJobService());
        doReturn(context).when(mSystemJobServiceSpy).getApplicationContext();
        doNothing().when(mSystemJobServiceSpy).onExecuted(any(), anyBoolean());
        mSystemJobServiceSpy.onCreate();
    }

    @After
    public void tearDown() {
        mSystemJobServiceSpy.onDestroy();
        mWorkManagerImpl.closeDatabase();
        WorkManagerImpl.setDelegate(null);
        ArchTaskExecutor.getInstance().setDelegate(null);
    }

    @Test
    @LargeTest
    public void testOnStopJob_ResetsWorkStatus() throws InterruptedException {
        OneTimeWorkRequest work = new OneTimeWorkRequest
                .Builder(StopReasonLoggingWorker.class).build();
        insertWork(work);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        mInstrumentation.runOnMainSync(() -> mSystemJobServiceSpy.onStartJob(mockParams));

        // TODO(sumir): Remove later.  Put here because WorkerWrapper sets state to RUNNING.
        Thread.sleep(5000L);

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        assertThat(workSpecDao.getState(work.getStringId()), is(WorkInfo.State.RUNNING));

        mInstrumentation.runOnMainSync(() -> mSystemJobServiceSpy.onStopJob(mockParams));
        // TODO(rahulrav): Figure out why this test is flaky.
        Thread.sleep(5000L);
        assertThat(workSpecDao.getState(work.getStringId()), is(WorkInfo.State.ENQUEUED));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertThat(StopReasonLoggingWorker.sReason, is(STOP_REASON_CONSTRAINT_CONNECTIVITY));
        }
    }

    @Test
    @LargeTest
    public void testOnStopJob_ReschedulesWhenNotCancelled() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        mInstrumentation.runOnMainSync(() -> {
            JobParameters mockParams = createMockJobParameters(work.getStringId());
            assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
            assertThat(mSystemJobServiceSpy.onStopJob(mockParams), is(true));
        });
    }

    @Test
    @LargeTest
    public void testOnStopJob_DoesNotRescheduleWhenCancelled() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        mInstrumentation.runOnMainSync(() -> {
            JobParameters mockParams = createMockJobParameters(work.getStringId());
            assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
            mWorkManagerImpl.cancelWorkById(work.getId());
            assertThat(mSystemJobServiceSpy.onStopJob(mockParams), is(false));
        });
    }

    @Test
    @LargeTest
    public void testStartJob_ReturnsFalseWithDuplicateJob() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class).build();
        insertWork(work);

        mInstrumentation.runOnMainSync(() -> {
            JobParameters mockParams = createMockJobParameters(work.getStringId());
            assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true));
            assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(false));
        });
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 24)
    public void testStartJob_PassesContentUriTriggers() throws InterruptedException {
        OneTimeWorkRequest work =
                new OneTimeWorkRequest.Builder(ContentUriTriggerLoggingWorker.class).build();
        insertWork(work);

        final String[] testContentAuthorities = new String[] {
                work.getStringId(),
                "yet another " + work.getStringId()
        };

        final Uri[] testContentUris = new Uri[] {
                Uri.parse("http://www.android.com"),
                Uri.parse("http://www.google.com")
        };

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        when(mockParams.getTriggeredContentAuthorities()).thenReturn(testContentAuthorities);
        when(mockParams.getTriggeredContentUris()).thenReturn(testContentUris);

        assertThat(ContentUriTriggerLoggingWorker.sTimesUpdated, is(0));
        mInstrumentation.runOnMainSync(() ->
                assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true)));

        Thread.sleep(1000L);

        assertThat(ContentUriTriggerLoggingWorker.sTimesUpdated, is(1));
        assertThat(ContentUriTriggerLoggingWorker.sTriggeredContentAuthorities,
                containsInAnyOrder(testContentAuthorities));
        assertThat(ContentUriTriggerLoggingWorker.sTriggeredContentUris,
                containsInAnyOrder(testContentUris));
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 28)
    public void testStartJob_passesNetwork() throws InterruptedException {
        WorkRequest work = new OneTimeWorkRequest.Builder(NetworkLoggingWorker.class).build();
        insertWork(work);

        Network mockNetwork = mock(Network.class);

        JobParameters mockParams = createMockJobParameters(work.getStringId());
        when(mockParams.getNetwork()).thenReturn(mockNetwork);

        assertThat(NetworkLoggingWorker.sTimesUpdated, is(0));
        mInstrumentation.runOnMainSync(() ->
                assertThat(mSystemJobServiceSpy.onStartJob(mockParams), is(true)));

        Thread.sleep(1000L);

        assertThat(NetworkLoggingWorker.sTimesUpdated, is(1));
        assertThat(NetworkLoggingWorker.sNetwork, is(mockNetwork));
    }

    private JobParameters createMockJobParameters(String id) {
        JobParameters jobParameters = mock(JobParameters.class);
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putString(SystemJobInfoConverter.EXTRA_WORK_SPEC_ID, id);
        when(jobParameters.getExtras()).thenReturn(persistableBundle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            when(jobParameters.getStopReason())
                    .thenReturn(STOP_REASON_CONSTRAINT_CONNECTIVITY);
        }
        return jobParameters;
    }

    private void insertWork(WorkRequest work) {
        mDatabase.workSpecDao().insertWorkSpec(work.getWorkSpec());
    }

    @RequiresApi(24)
    public static class ContentUriTriggerLoggingWorker extends Worker {

        static int sTimesUpdated = 0;
        static List<String> sTriggeredContentAuthorities;
        static List<Uri> sTriggeredContentUris;

        public ContentUriTriggerLoggingWorker(@NonNull Context context,
                @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @Override
        public @NonNull Result doWork() {
            synchronized (ContentUriTriggerLoggingWorker.class) {
                ++sTimesUpdated;
                sTriggeredContentAuthorities = getTriggeredContentAuthorities();
                sTriggeredContentUris = getTriggeredContentUris();
            }
            return Result.success();
        }
    }

    @RequiresApi(28)
    public static class NetworkLoggingWorker extends Worker {

        static int sTimesUpdated = 0;
        static Network sNetwork;

        public NetworkLoggingWorker(@NonNull Context context,
                @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @Override
        public @NonNull Result doWork() {
            synchronized (NetworkLoggingWorker.class) {
                ++sTimesUpdated;
                sNetwork = getNetwork();
            }
            return Result.success();
        }
    }

    public static class StopReasonLoggingWorker extends NeverResolvedWorker {

        static int sReason = 0;

        public StopReasonLoggingWorker(@NonNull Context appContext,
                @NonNull WorkerParameters workerParams) {
            super(appContext, workerParams);
        }

        @Override
        public void onStopped() {
            super.onStopped();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sReason = getStopReason();
            }
        }
    }
}
