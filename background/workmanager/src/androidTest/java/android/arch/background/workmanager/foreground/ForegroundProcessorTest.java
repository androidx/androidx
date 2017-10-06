/*
 * Copyright (C) 2017 The Android Open Source Project
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
package android.arch.background.workmanager.foreground;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.InfiniteTestWorker;
import android.arch.background.workmanager.Scheduler;
import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.Work;
import android.arch.background.workmanager.WorkDatabase;
import android.arch.background.workmanager.WorkSpecs;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ForegroundProcessorTest {
    private static final long DEFAULT_SLEEP_TIME_MS = 1000L;
    private WorkDatabase mWorkDatabase;
    private ForegroundProcessor mForegroundProcessor;
    private TestLifecycleOwner mLifecycleOwner;

    @Before
    public void setUp() {
        Context appContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mLifecycleOwner = new TestLifecycleOwner();
        mWorkDatabase = WorkDatabase.create(appContext, true);
        mForegroundProcessor = new ForegroundProcessor(
                appContext,
                mWorkDatabase,
                mock(Scheduler.class),
                mLifecycleOwner);
    }

    @After
    public void tearDown() {
        mWorkDatabase.close();
    }

    @Test
    public void testIsActive_onStart_returnsTrue() {
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertThat(mForegroundProcessor.isActive(), is(true));
    }

    @Test
    public void testIsActive_onStop_returnsFalse() {
        assertThat(mForegroundProcessor.isActive(), is(true));
        mLifecycleOwner.mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        assertThat(mForegroundProcessor.isActive(), is(false));
    }

    @Test
    public void testOnChanged() throws InterruptedException {
        WorkSpec workSpec = WorkSpecs.getWorkSpec(InfiniteTestWorker.class);
        mWorkDatabase.workSpecDao().insertWorkSpec(workSpec);
        Thread.sleep(DEFAULT_SLEEP_TIME_MS);
        assertThat(mWorkDatabase.workSpecDao().getWorkSpecStatus(workSpec.getId()),
                is(Work.STATUS_RUNNING));
    }
}