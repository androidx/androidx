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

package androidx.work.impl.background.systemalarm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.work.DatabaseTest;
import androidx.work.Work;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.model.AlarmInfo;
import androidx.work.worker.TestWorker;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AlarmsTest extends DatabaseTest {

    private Context mContext;
    private WorkManagerImpl mWorkManager;
    private long mTriggerAt;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mWorkManager = mock(WorkManagerImpl.class);
        // Set it to sometime in the future so as to avoid triggering real alarms.
        mTriggerAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        when(mWorkManager.getWorkDatabase()).thenReturn(mDatabase);
    }

    @Test
    public void testSetAlarm_noPreExistingAlarms() {
        Work work = new Work.Builder(TestWorker.class).build();
        insertWork(work);
        String workSpecId = work.getId();

        Alarms.setAlarm(mContext, mWorkManager, workSpecId, mTriggerAt);
        AlarmInfo alarmInfo = mDatabase.alarmInfoDao().getAlarmInfo(workSpecId);
        assertThat(alarmInfo, is(notNullValue()));
    }

    @Test
    public void testSetAlarm_withPreExistingAlarms() {
        Work work = new Work.Builder(TestWorker.class).build();
        insertWork(work);
        String workSpecId = work.getId();

        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setWorkSpecId(workSpecId);
        alarmInfo.setAlarmId(1);

        mDatabase.alarmInfoDao().insertAlarmInfo(alarmInfo);

        Alarms.setAlarm(mContext, mWorkManager, workSpecId, mTriggerAt);
        AlarmInfo updatedAlarmInfo = mDatabase.alarmInfoDao().getAlarmInfo(workSpecId);
        assertThat(updatedAlarmInfo, is(notNullValue()));
        assertThat(updatedAlarmInfo.getAlarmId(), is(alarmInfo.getAlarmId()));
    }

    @Test
    public void testCancelAlarm() {
        Work work = new Work.Builder(TestWorker.class).build();
        insertWork(work);
        String workSpecId = work.getId();

        AlarmInfo alarmInfo = new AlarmInfo();
        alarmInfo.setWorkSpecId(workSpecId);
        alarmInfo.setAlarmId(1);

        mDatabase.alarmInfoDao().insertAlarmInfo(alarmInfo);

        Alarms.cancelAlarm(mContext, mWorkManager, workSpecId);
        AlarmInfo updatedAlarmInfo = mDatabase.alarmInfoDao().getAlarmInfo(workSpecId);
        assertThat(updatedAlarmInfo, is(nullValue()));
    }
}
