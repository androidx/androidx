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

package androidx.lifecycle;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.service.TestService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ServiceLifecycleTest {

    private static final int RETRY_NUMBER = 5;
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(1);

    private Intent mServiceIntent;

    private volatile List<Event> mLoggerEvents;
    private EventLogger mLogger;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        mServiceIntent = new Intent(context, TestService.class);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TestService.ACTION_LOG_EVENT);

        // Overcautiousness: each EventLogger has its own events list, so one bad test won't spoil
        // others.
        mLoggerEvents = new ArrayList<>();
        mLogger = new EventLogger(mLoggerEvents);
        localBroadcastManager.registerReceiver(mLogger, intentFilter);

    }

    @After
    public void tearDown() {
        Context context = InstrumentationRegistry.getTargetContext();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(mLogger);
        mLogger = null;
        mLoggerEvents = null;
    }

    @Test
    public void testUnboundedService() throws TimeoutException, InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.startService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START);
        context.stopService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY);
    }

    @Test
    public void testBoundedService() throws TimeoutException, InterruptedException {
        ServiceConnection connection = bindToService();
        awaitAndAssertEvents(ON_CREATE, ON_START);
        InstrumentationRegistry.getTargetContext().unbindService(connection);
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY);
    }

    @Test
    public void testStartBindUnbindStop() throws InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.startService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START);

        ServiceConnection connection = bindToService();
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.unbindService(connection);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // service is still started (stopServices/stopSelf weren't called)
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.stopService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY);
    }

    @Test
    public void testStartBindStopUnbind() throws InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.startService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START);

        ServiceConnection connection = bindToService();
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.stopService(mServiceIntent);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // service is still bound
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.unbindService(connection);
        awaitAndAssertEvents(ON_CREATE, ON_START,
                ON_STOP, ON_DESTROY);
    }

    @Test
    public void testBindStartUnbindStop() throws InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        ServiceConnection connection = bindToService();
        awaitAndAssertEvents(ON_CREATE, ON_START);


        context.startService(mServiceIntent);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.unbindService(connection);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // service is still started (stopServices/stopSelf weren't called)
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.stopService(mServiceIntent);
        awaitAndAssertEvents(ON_CREATE, ON_START,
                ON_STOP, ON_DESTROY);
    }

    @Test
    public void testBindStartStopUnbind() throws InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        ServiceConnection connection = bindToService();
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.startService(mServiceIntent);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.stopService(mServiceIntent);
        // Precaution: give a chance to dispatch events
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // service is still bound
        awaitAndAssertEvents(ON_CREATE, ON_START);

        context.unbindService(connection);
        awaitAndAssertEvents(ON_CREATE, ON_START,
                ON_STOP, ON_DESTROY);
    }

    // can't use ServiceTestRule because it proxies connection, so we can't use unbindService method
    private ServiceConnection bindToService() throws InterruptedException {
        Context context = InstrumentationRegistry.getTargetContext();
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                latch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        boolean success = context.bindService(mServiceIntent, connection, Context.BIND_AUTO_CREATE);
        assertThat(success, is(true));
        boolean awaited = latch.await(TIMEOUT, TimeUnit.MILLISECONDS);
        assertThat(awaited, is(true));
        return connection;
    }

    private void awaitAndAssertEvents(Event... events) throws InterruptedException {
        //noinspection SynchronizeOnNonFinalField
        synchronized (mLoggerEvents) {
            int retryCount = 0;
            while (mLoggerEvents.size() < events.length && retryCount++ < RETRY_NUMBER) {
                mLoggerEvents.wait(TIMEOUT);
            }
            assertThat(mLoggerEvents, is(Arrays.asList(events)));
        }
    }

    private static class EventLogger extends BroadcastReceiver {
        private final List<Event> mLoggerEvents;

        private EventLogger(List<Event> loggerEvents) {
            mLoggerEvents = loggerEvents;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mLoggerEvents) {
                mLoggerEvents.add((Event) intent.getSerializableExtra(TestService.EXTRA_KEY_EVENT));
                mLoggerEvents.notifyAll();
            }
        }
    }
}
