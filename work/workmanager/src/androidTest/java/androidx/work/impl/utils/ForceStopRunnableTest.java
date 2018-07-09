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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.WorkSpecDao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ForceStopRunnableTest {

    private Context mContext;
    private WorkManagerImpl mWorkManager;
    private WorkDatabase mWorkDatabase;
    private WorkSpecDao mWorkSpecDao;
    private Preferences mPreferences;
    private ForceStopRunnable mRunnable;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext().getApplicationContext();
        mWorkManager = mock(WorkManagerImpl.class);
        mWorkDatabase = mock(WorkDatabase.class);
        mWorkSpecDao = mock(WorkSpecDao.class);
        mPreferences = mock(Preferences.class);
        when(mWorkManager.getWorkDatabase()).thenReturn(mWorkDatabase);
        when(mWorkDatabase.workSpecDao()).thenReturn(mWorkSpecDao);
        when(mWorkManager.getPreferences()).thenReturn(mPreferences);
        mRunnable = new ForceStopRunnable(mContext, mWorkManager);
    }

    @Test
    public void testIntent() {
        Intent intent = mRunnable.getIntent();
        ComponentName componentName = intent.getComponent();
        assertThat(componentName.getClassName(),
                is(ForceStopRunnable.BroadcastReceiver.class.getName()));
        assertThat(intent.getAction(), is(ForceStopRunnable.ACTION_FORCE_STOP_RESCHEDULE));
    }

    @Test
    public void testReschedulesOnForceStop() {
        ForceStopRunnable runnable = spy(mRunnable);
        when(runnable.shouldRescheduleWorkers()).thenReturn(false);
        when(runnable.isForceStopped()).thenReturn(true);
        runnable.run();
        verify(mWorkManager, times(1)).rescheduleEligibleWork();
        verify(mWorkManager, times(1)).onForceStopRunnableCompleted();
    }

    @Test
    public void test_doNothingWhenNotForceStopped() {
        ForceStopRunnable runnable = spy(mRunnable);
        when(runnable.shouldRescheduleWorkers()).thenReturn(false);
        when(runnable.isForceStopped()).thenReturn(false);
        runnable.run();
        verify(mWorkManager, times(0)).rescheduleEligibleWork();
        verify(mWorkManager, times(1)).onForceStopRunnableCompleted();
    }

    @Test
    public void test_rescheduleWorkers_updatesSharedPreferences() {
        ForceStopRunnable runnable = spy(mRunnable);
        when(runnable.shouldRescheduleWorkers()).thenReturn(true);
        runnable.run();
        verify(mPreferences, times(1)).setNeedsReschedule(false);
    }
}
