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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.utils.SynchronousExecutor;
import androidx.work.impl.utils.taskexecutor.WorkManagerTaskExecutor;
import androidx.work.worker.TestWorker;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;

@RunWith(AndroidJUnit4.class)
public class DefaultWorkerFactoryTest extends DatabaseTest {

    private Context mContext;
    private WorkerFactory mDefaultWorkerFactory;
    private ProgressUpdater mProgressUpdater;
    private ForegroundUpdater mForegroundUpdater;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mDefaultWorkerFactory = WorkerFactory.getDefaultWorkerFactory();
        mProgressUpdater = mock(ProgressUpdater.class);
        mForegroundUpdater = mock(ForegroundUpdater.class);
    }

    @Test
    @SmallTest
    public void testCreateWorker_isCreatedSuccessfully() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        Executor executor = new SynchronousExecutor();
        ListenableWorker worker = mDefaultWorkerFactory.createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                TestWorker.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        executor,
                        new WorkManagerTaskExecutor(executor),
                        mDefaultWorkerFactory,
                        mProgressUpdater,
                        mForegroundUpdater));
        assertThat(worker, is(notNullValue()));
        assertThat(worker,
                is(CoreMatchers.<ListenableWorker>instanceOf(TestWorker.class)));
        assertThat(worker.getId(), is(work.getId()));
    }

    @Test
    @SmallTest
    public void testCreateWorker_throwsException() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        insertWork(work);

        Executor executor = new SynchronousExecutor();
        ListenableWorker worker = mDefaultWorkerFactory.createWorkerWithDefaultFallback(
                mContext.getApplicationContext(),
                DefaultWorkerFactoryTest.class.getName(),
                new WorkerParameters(
                        work.getId(),
                        Data.EMPTY,
                        work.getTags(),
                        new WorkerParameters.RuntimeExtras(),
                        1,
                        executor,
                        new WorkManagerTaskExecutor(executor),
                        mDefaultWorkerFactory,
                        mProgressUpdater,
                        mForegroundUpdater));
        assertThat(worker, is(nullValue()));
    }
}
