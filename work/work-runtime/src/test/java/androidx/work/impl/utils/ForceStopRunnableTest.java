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

package androidx.work.impl.utils;

import static androidx.work.impl.model.SystemIdInfoKt.systemIdInfo;
import static androidx.work.impl.model.WorkSpecKt.generationalId;
import static androidx.work.impl.utils.ForceStopRunnable.MAX_ATTEMPTS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteException;
import android.os.Build;
import android.os.UserManager;

import androidx.core.util.Consumer;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpec;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, minSdk = 16)
public class ForceStopRunnableTest {
    private Context mContext;
    private WorkManagerImpl mWorkManager;
    private Scheduler mScheduler;
    private Configuration mConfiguration;
    private WorkDatabase mWorkDatabase;
    private PreferenceUtils mPreferenceUtils;
    private ForceStopRunnable mRunnable;

    private ActivityManager mActivityManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext().getApplicationContext();
        mWorkManager = mock(WorkManagerImpl.class);
        mActivityManager = mock(ActivityManager.class);
        mPreferenceUtils = mock(PreferenceUtils.class);
        mScheduler = mock(Scheduler.class);
        Executor executor = new SynchronousExecutor();
        mConfiguration =
                new Configuration.Builder().setExecutor(executor).setTaskExecutor(executor).build();
        mWorkDatabase =
                WorkDatabase.create(
                        mContext,
                        mConfiguration.getTaskExecutor(),
                        mConfiguration.getClock(),
                        true);
        when(mWorkManager.getWorkDatabase()).thenReturn(mWorkDatabase);
        when(mWorkManager.getSchedulers()).thenReturn(Collections.singletonList(mScheduler));
        when(mWorkManager.getPreferenceUtils()).thenReturn(mPreferenceUtils);
        when(mWorkManager.getConfiguration()).thenReturn(mConfiguration);
        mRunnable = new ForceStopRunnable(mContext, mWorkManager);
    }

    @After
    public void tearDown() {
        mWorkDatabase.close();
    }

    @Test
    public void testIntent() {
        Intent intent = ForceStopRunnable.getIntent(mContext);
        ComponentName componentName = intent.getComponent();
        assertThat(componentName.getClassName())
                .isEqualTo(ForceStopRunnable.BroadcastReceiver.class.getName());
        assertThat(intent.getAction()).isEqualTo(ForceStopRunnable.ACTION_FORCE_STOP_RESCHEDULE);
    }

    @Test
    public void testReschedulesOnForceStop() {
        ForceStopRunnable runnable = spy(mRunnable);
        doReturn(false).when(runnable).shouldRescheduleWorkers();
        doReturn(true).when(runnable).isForceStopped();
        runnable.run();
        verify(mWorkManager, times(1)).rescheduleEligibleWork();
        verify(mWorkManager, times(1)).onForceStopRunnableCompleted();
    }

    @Test
    public void test_doNothingWhenNotForceStopped() {
        ForceStopRunnable runnable = spy(mRunnable);
        doReturn(false).when(runnable).shouldRescheduleWorkers();
        doReturn(false).when(runnable).isForceStopped();
        runnable.run();
        verify(mWorkManager, times(0)).rescheduleEligibleWork();
        verify(mWorkManager, times(1)).onForceStopRunnableCompleted();
    }

    @Test
    public void test_rescheduleWorkers_updatesSharedPreferences() {
        ForceStopRunnable runnable = spy(mRunnable);
        when(runnable.shouldRescheduleWorkers()).thenReturn(true);
        runnable.run();
        verify(mPreferenceUtils, times(1)).setNeedsReschedule(false);
    }

    @Test
    public void test_UnfinishedWork_getsScheduled() {
        ForceStopRunnable runnable = spy(mRunnable);
        doReturn(false).when(runnable).shouldRescheduleWorkers();
        doReturn(false).when(runnable).isForceStopped();
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(TestWorker.class)
                        .setInitialState(WorkInfo.State.RUNNING)
                        .build();
        WorkSpec workSpec = request.getWorkSpec();
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        runnable.run();
        WorkSpec updatedWorkSpec = mWorkDatabase.workSpecDao().getWorkSpec(workSpec.id);
        assertThat(updatedWorkSpec.scheduleRequestedAt).isGreaterThan(0L);
        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mScheduler, times(1)).schedule(captor.capture());
        assertThat(workSpec.id).isEqualTo(captor.getValue().id);
    }

    @Test
    public void testReconcileJobs() {
        ForceStopRunnable runnable = spy(mRunnable);
        doReturn(false).when(runnable).shouldRescheduleWorkers();
        doReturn(false).when(runnable).isForceStopped();
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(TestWorker.class)
                        .setInitialState(WorkInfo.State.ENQUEUED)
                        .build();
        WorkSpec workSpec = request.getWorkSpec();
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        mWorkDatabase
                .systemIdInfoDao()
                .insertSystemIdInfo(systemIdInfo(generationalId(workSpec), 0));
        runnable.run();
        ArgumentCaptor<WorkSpec> captor = ArgumentCaptor.forClass(WorkSpec.class);
        verify(mScheduler, times(1)).schedule(captor.capture());
        assertThat(workSpec.id).isEqualTo(captor.getValue().id);
    }

    @Test(expected = IllegalStateException.class)
    public void test_rethrowForNonRecoverableSqliteExceptions() {
        ForceStopRunnable runnable = spy(mRunnable);
        doNothing().when(runnable).sleep(anyLong());
        when(runnable.cleanUp())
                .thenThrow(new SQLiteCantOpenDatabaseException("Cannot open database."));
        runnable.run();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_initializationExceptionHandler() {
        Consumer<Throwable> handler = mock(Consumer.class);
        Configuration configuration =
                new Configuration.Builder(mConfiguration)
                        .setInitializationExceptionHandler(handler)
                        .build();

        when(mWorkManager.getConfiguration()).thenReturn(configuration);
        ForceStopRunnable runnable = spy(mRunnable);
        doNothing().when(runnable).sleep(anyLong());
        when(runnable.cleanUp())
                .thenThrow(new SQLiteCantOpenDatabaseException("Cannot open database."));
        runnable.run();
        verify(runnable, times(MAX_ATTEMPTS - 1)).sleep(anyLong());
        verify(runnable, times(MAX_ATTEMPTS)).forceStopRunnable();
        verify(handler, times(1)).accept(any(Throwable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_InitializationExceptionHandler_migrationFailures() {
        mContext = mock(Context.class);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(Context.ACTIVITY_SERVICE)).thenReturn(mActivityManager);
        // UserManager.isUserUnlocked() is API 24+. Stubbing it on API < 24 throws NoSuchMethodError
        // under Robolectric because the method is missing from the runtime class. It works on API
        // < 24 because UserManagerCompat.isUserUnlocked() returns true without querying the
        // service.
        if (Build.VERSION.SDK_INT >= 24) {
            UserManager mockUserManager = mock(UserManager.class);
            when(mockUserManager.isUserUnlocked()).thenReturn(true);
            doReturn(Context.USER_SERVICE).when(mContext).getSystemServiceName(UserManager.class);
            doReturn(mockUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
            doReturn(mockUserManager).when(mContext).getSystemService(UserManager.class);
        }
        mWorkDatabase =
                WorkDatabase.create(
                        mContext,
                        mConfiguration.getTaskExecutor(),
                        mConfiguration.getClock(),
                        true);
        when(mWorkManager.getWorkDatabase()).thenReturn(mWorkDatabase);
        mRunnable = new ForceStopRunnable(mContext, mWorkManager);

        Consumer<Throwable> handler = mock(Consumer.class);
        Configuration configuration =
                new Configuration.Builder(mConfiguration)
                        .setInitializationExceptionHandler(handler)
                        .build();

        when(mWorkManager.getConfiguration()).thenReturn(configuration);
        // This is what WorkDatabasePathHelper uses under the hood to migrate the database.
        when(mContext.getDatabasePath(anyString()))
                .thenThrow(new SQLiteException("Unable to migrate database"));

        ForceStopRunnable runnable = spy(mRunnable);
        doNothing().when(runnable).sleep(anyLong());
        runnable.run();
        verify(handler, times(1)).accept(any(Throwable.class));
    }

    @Test
    public void test_completeOnMultiProcessChecks() {
        ForceStopRunnable runnable = spy(mRunnable);
        doReturn(false).when(runnable).multiProcessChecks();
        runnable.run();
        verify(mWorkManager).onForceStopRunnableCompleted();
    }

    @Test
    public void test_retriesForGenericSqliteExceptions() {
        ForceStopRunnable runnable = spy(mRunnable);
        doNothing().when(runnable).sleep(anyLong());
        doReturn(true).when(runnable).multiProcessChecks();
        doThrow(new SQLiteException("no such table: SystemIdInfo"))
                .when(runnable)
                .forceStopRunnable();

        assertThrows(SQLiteException.class, runnable::run);
        verify(runnable, times(MAX_ATTEMPTS)).forceStopRunnable();
        verify(runnable, times(MAX_ATTEMPTS - 1)).sleep(anyLong());
    }

    @Test
    @Config(minSdk = 24)
    public void test_directBoot_throwsIllegalStateException() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        shadowOf(userManager).setUserUnlocked(false);

        ForceStopRunnable runnable = spy(new ForceStopRunnable(mContext, mWorkManager));
        doReturn(true).when(runnable).multiProcessChecks();

        assertThrows(IllegalStateException.class, runnable::run);
    }

    @Test
    @Config(minSdk = 24)
    @SuppressWarnings("unchecked")
    public void test_directBoot_callsInitializationExceptionHandler() {
        UserManager userManager = mContext.getSystemService(UserManager.class);
        shadowOf(userManager).setUserUnlocked(false);

        Consumer<Throwable> handler = mock(Consumer.class);
        Configuration configuration =
                new Configuration.Builder(mConfiguration)
                        .setInitializationExceptionHandler(handler)
                        .build();
        when(mWorkManager.getConfiguration()).thenReturn(configuration);

        ForceStopRunnable runnable = spy(new ForceStopRunnable(mContext, mWorkManager));
        doReturn(true).when(runnable).multiProcessChecks();

        runnable.run();
        verify(handler, times(1)).accept(any(Throwable.class));
    }

    public static class TestWorker extends Worker {
        public TestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            return Result.success();
        }
    }
}
