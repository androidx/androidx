/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.manager;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.wear.tiles.manager.UpdateScheduler.MIN_INTER_UPDATE_INTERVAL_MILLIS;

import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.content.ComponentName;

import androidx.wear.tiles.TilesTestRunner;

import com.google.common.truth.Expect;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.ArrayList;
import java.util.List;

@RunWith(TilesTestRunner.class)
@DoNotInstrument
public class UpdateSchedulerTest {
    private static final int TILE_ID = 42;
    private static final ComponentName TILE_COMPONENT = new ComponentName("my.package", "foo.bar");
    @Rule public Expect expect = Expect.create();

    private UpdateScheduler mUpdateSchedulerUnderTest;
    private long mCurrentTime;
    private ShadowAlarmManager mShadowAlarmManager;
    private List<Boolean> mFired;

    @Before
    public void setUp() {
        mFired = new ArrayList<>();

        // Set the current time to the min interval. This is because the "last update time" inits to
        // zero, so we can get some weird results if the starting time is lower than the min update
        // interval (i.e. alarms being postponed where they shouldn't be)
        mCurrentTime = MIN_INTER_UPDATE_INTERVAL_MILLIS;

        AlarmManager alarmManager = getApplicationContext().getSystemService(AlarmManager.class);
        mShadowAlarmManager = shadowOf(alarmManager);

        mShadowAlarmManager =
                shadowOf(getApplicationContext().getSystemService(AlarmManager.class));
        mUpdateSchedulerUnderTest = new UpdateScheduler(alarmManager, () -> mCurrentTime);
        mUpdateSchedulerUnderTest.setUpdateReceiver(() -> mFired.add(true));
    }

    @Test
    public void enableUpdate_doesntScheduleJobIfNothingScheduled() {
        mUpdateSchedulerUnderTest.enableUpdates();

        expect.that(mFired).isEmpty();
        expect.that(mShadowAlarmManager.getScheduledAlarms()).isEmpty();
    }

    @Test
    public void scheduleUpdateAtTime_firesAtExpectedTime() {
        mUpdateSchedulerUnderTest.enableUpdates();

        long startTime = mCurrentTime;
        long triggerAt = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS * 2;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerAt);

        // Advance a little
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS);
        expect.that(mFired).isEmpty();

        // And now to the time
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS * 2);
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void scheduleUpdateAtTime_enforcesMinPeriod() {
        mUpdateSchedulerUnderTest.enableUpdates();

        long startTime = mCurrentTime;
        long triggerAt = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS * 2;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerAt);

        // Advance a little
        advanceToTime(triggerAt);
        mFired.clear();

        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(
                triggerAt + (MIN_INTER_UPDATE_INTERVAL_MILLIS / 2));
        advanceToTime(triggerAt + (MIN_INTER_UPDATE_INTERVAL_MILLIS / 2));

        // Shouldn't have fired...
        expect.that(mFired).isEmpty();

        advanceToTime(triggerAt + (MIN_INTER_UPDATE_INTERVAL_MILLIS));

        expect.that(mFired).hasSize(1);
    }

    @Test
    public void updateNow_firesUpdate() {
        mUpdateSchedulerUnderTest.enableUpdates();
        mUpdateSchedulerUnderTest.updateNow(false);

        expect.that(mFired).hasSize(1);
    }

    @Test
    public void updateNow_respectsMinPeriod() {
        long startTime = mCurrentTime;

        mUpdateSchedulerUnderTest.enableUpdates();
        mUpdateSchedulerUnderTest.updateNow(false);
        mFired.clear();

        // Seek forwards a little.
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);

        mUpdateSchedulerUnderTest.updateNow(false);
        expect.that(mFired).isEmpty();

        // The update should have been scheduled.
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS);
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void updateNow_canBeForced() {
        long startTime = mCurrentTime;

        mUpdateSchedulerUnderTest.enableUpdates();
        mUpdateSchedulerUnderTest.updateNow(false);
        mFired.clear();

        // Seek forwards a little.
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);

        mUpdateSchedulerUnderTest.updateNow(true);
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void disableUpdates_inhibitsAlarms() {
        mUpdateSchedulerUnderTest.enableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);
        mUpdateSchedulerUnderTest.disableUpdates();
        advanceToTime(triggerTime);

        expect.that(mFired).isEmpty();
    }

    @Test
    public void disableUpdates_inhibitsImmediateUpdates() {
        mUpdateSchedulerUnderTest.disableUpdates();

        mUpdateSchedulerUnderTest.updateNow(false);
        mUpdateSchedulerUnderTest.updateNow(true);

        expect.that(mFired).isEmpty();
    }

    @Test
    public void disableUpdates_preventsScheduledUpdates() {
        mUpdateSchedulerUnderTest.disableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(triggerTime);

        expect.that(mFired).isEmpty();
    }

    @Test
    public void enableUpdates_reschedulesUpdatesRequestedWhileSchedulerDisabled() {
        mUpdateSchedulerUnderTest.disableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);

        mUpdateSchedulerUnderTest.enableUpdates();
        expect.that(mFired).isEmpty();

        advanceToTime(triggerTime);
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void enableUpdates_reschedulesUpdatesRequestedWhileSchedulerEnabled() {
        mUpdateSchedulerUnderTest.enableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        // Advance a little...
        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 4);
        mUpdateSchedulerUnderTest.disableUpdates();

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);
        mUpdateSchedulerUnderTest.enableUpdates();
        expect.that(mFired).isEmpty();

        advanceToTime(triggerTime);
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void enableUpdates_shouldFireImmediatelyIfJobInPast() {
        mUpdateSchedulerUnderTest.disableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS * 2);
        expect.that(mFired).isEmpty();

        mUpdateSchedulerUnderTest.enableUpdates();
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void enableUpdates_shouldFireRequestedUpdate() {
        mUpdateSchedulerUnderTest.disableUpdates();
        mUpdateSchedulerUnderTest.updateNow(false);

        advanceToTime(mCurrentTime + MIN_INTER_UPDATE_INTERVAL_MILLIS * 2);
        expect.that(mFired).isEmpty();

        mUpdateSchedulerUnderTest.enableUpdates();
        expect.that(mFired).hasSize(1);
    }

    @Test
    public void cancelUpdates_cancelsUpdates() {
        mUpdateSchedulerUnderTest.enableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);
        mUpdateSchedulerUnderTest.cancelScheduledUpdates();

        advanceToTime(triggerTime);
        expect.that(mFired).isEmpty();
    }

    @Test
    public void cancelUpdate_cancelsUpdatesWhileDisabled() {
        mUpdateSchedulerUnderTest.disableUpdates();

        long startTime = mCurrentTime;
        long triggerTime = startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS;
        mUpdateSchedulerUnderTest.scheduleUpdateAtTime(triggerTime);

        advanceToTime(startTime + MIN_INTER_UPDATE_INTERVAL_MILLIS / 2);
        mUpdateSchedulerUnderTest.cancelScheduledUpdates();

        advanceToTime(triggerTime);
        mUpdateSchedulerUnderTest.enableUpdates();
        expect.that(mFired).isEmpty();
    }

    private void advanceToTime(Long targetTime) {
        while (mShadowAlarmManager.peekNextScheduledAlarm() != null
                && mShadowAlarmManager.peekNextScheduledAlarm().triggerAtTime <= targetTime) {
            ShadowAlarmManager.ScheduledAlarm alarm = mShadowAlarmManager.getNextScheduledAlarm();
            mCurrentTime = alarm.triggerAtTime;
            alarm.onAlarmListener.onAlarm();
        }

        mCurrentTime = targetTime;
    }
}
