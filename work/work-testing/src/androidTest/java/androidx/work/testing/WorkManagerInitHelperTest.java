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

package androidx.work.testing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.work.Configuration;
import androidx.work.InputMergerFactory;
import androidx.work.WorkManager;
import androidx.work.WorkerFactory;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.SerialExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class WorkManagerInitHelperTest {

    private Context mContext;
    private Executor mExecutor;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        // Clear delegates after every single test.
        WorkManagerImpl.setDelegate(null);
    }

    @Test
    public void testWorkManagerIsInitialized() {
        Configuration configuration = new Configuration.Builder()
                .setExecutor(mExecutor)
                .setTaskExecutor(mExecutor)
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        WorkManagerImpl workManager = (WorkManagerImpl) WorkManager.getInstance(mContext);
        assertThat(workManager, is(notNullValue()));
        SerialExecutor serialExecutor = workManager.getWorkTaskExecutor().getBackgroundExecutor();
        assertThat(serialExecutor.getDelegatedExecutor(), is(mExecutor));
    }

    @Test
    public void testWorkManagerInitialized_withSynchronousTaskExecutor() {
        Configuration configuration = new Configuration.Builder()
                .setExecutor(mExecutor)
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        WorkManagerImpl workManager = (WorkManagerImpl) WorkManager.getInstance(mContext);
        assertThat(workManager, is(notNullValue()));
        SerialExecutor serialExecutor = workManager.getWorkTaskExecutor().getBackgroundExecutor();
        assertThat(serialExecutor.getDelegatedExecutor(), instanceOf(SynchronousExecutor.class));
    }

    @Test
    public void testWorkManagerInitialized_withFullConfiguration() {
        Configuration configuration = new Configuration.Builder()
                .setExecutor(mExecutor)
                .setInputMergerFactory(InputMergerFactory.getDefaultInputMergerFactory())
                .setWorkerFactory(WorkerFactory.getDefaultWorkerFactory())
                .setJobSchedulerJobIdRange(1000, 2000)
                .setMaxSchedulerLimit(50)
                .setMinimumLoggingLevel(Log.DEBUG)
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(mContext, configuration);
        WorkManagerImpl workManager = (WorkManagerImpl) WorkManager.getInstance(mContext);
        assertThat(workManager, is(notNullValue()));
        SerialExecutor serialExecutor = workManager.getWorkTaskExecutor().getBackgroundExecutor();
        assertThat(serialExecutor.getDelegatedExecutor(), instanceOf(SynchronousExecutor.class));
        Configuration used = workManager.getConfiguration();

        assertThat(configuration.getInputMergerFactory(), is(used.getInputMergerFactory()));
        assertThat(configuration.getWorkerFactory(), is(used.getWorkerFactory()));
        assertThat(configuration.getMinJobSchedulerId(), is(used.getMinJobSchedulerId()));
        assertThat(configuration.getMaxJobSchedulerId(), is(used.getMaxJobSchedulerId()));
        assertThat(configuration.getMaxSchedulerLimit(), is(used.getMaxSchedulerLimit()));
        assertThat(configuration.getMinimumLoggingLevel(), is(used.getMinimumLoggingLevel()));
    }
}
