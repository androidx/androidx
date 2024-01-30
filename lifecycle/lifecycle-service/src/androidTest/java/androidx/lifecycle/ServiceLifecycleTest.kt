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
package androidx.lifecycle

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.ON_CREATE
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.service.TestService
import androidx.lifecycle.service.TestService.Companion.ACTION_LOG_EVENT
import androidx.lifecycle.service.TestService.Companion.EXTRA_KEY_EVENT
import androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("deprecation")
@MediumTest
@RunWith(AndroidJUnit4::class)
class ServiceLifecycleTest {
    private lateinit var serviceIntent: Intent

    @Volatile
    private var loggerEvents = mutableListOf<Event?>()
    private lateinit var logger: EventLogger

    @Before
    fun setUp() {
        val context = getApplicationContext<Context>()
        serviceIntent = Intent(context, TestService::class.java)
        val localBroadcastManager = getInstance(context)
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_LOG_EVENT)

        // Over-cautiousness: each EventLogger has its own events list, so one bad test won't spoil
        // others.
        loggerEvents = ArrayList()
        logger = EventLogger(loggerEvents)
        localBroadcastManager.registerReceiver(logger, intentFilter)
    }

    @After
    fun tearDown() {
        val context = getApplicationContext<Context>()
        val localBroadcastManager = getInstance(context)
        localBroadcastManager.unregisterReceiver(logger)
        loggerEvents.clear()
    }

    @Test
    @Throws(TimeoutException::class, InterruptedException::class)
    fun testUnboundedService() {
        val context = getApplicationContext<Context>()
        context.startService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.stopService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    @Test
    @Throws(TimeoutException::class, InterruptedException::class)
    fun testBoundedService() {
        val connection = bindToService()
        awaitAndAssertEvents(ON_CREATE, ON_START)
        getApplicationContext<Context>().unbindService(connection)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testStartBindUnbindStop() {
        val context = getApplicationContext<Context>()
        context.startService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START)
        val connection = bindToService()
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.unbindService(connection)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // service is still started (stopServices/stopSelf weren't called)
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.stopService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testStartBindStopUnbind() {
        val context = getApplicationContext<Context>()
        context.startService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START)
        val connection = bindToService()
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.stopService(serviceIntent)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // service is still bound
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.unbindService(connection)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBindStartUnbindStop() {
        val context = getApplicationContext<Context>()
        val connection = bindToService()
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.startService(serviceIntent)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.unbindService(connection)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // service is still started (stopServices/stopSelf weren't called)
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.stopService(serviceIntent)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testBindStartStopUnbind() {
        val context = getApplicationContext<Context>()
        val connection = bindToService()
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.startService(serviceIntent)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // still the same events
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.stopService(serviceIntent)
        // Precaution: give a chance to dispatch events
        getInstrumentation().waitForIdleSync()
        // service is still bound
        awaitAndAssertEvents(ON_CREATE, ON_START)
        context.unbindService(connection)
        awaitAndAssertEvents(ON_CREATE, ON_START, ON_STOP, ON_DESTROY)
    }

    // can't use ServiceTestRule because it proxies connection, so we can't use unbindService method
    @Throws(InterruptedException::class)
    private fun bindToService(): ServiceConnection {
        val context = getApplicationContext<Context>()
        val latch = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                latch.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        val success = context.bindService(serviceIntent, connection, BIND_AUTO_CREATE)
        assertThat(success, `is`(true))
        val awaited = latch.await(TIMEOUT, MILLISECONDS)
        assertThat(awaited, `is`(true))
        return connection
    }

    @Throws(InterruptedException::class)
    private fun awaitAndAssertEvents(vararg events: Event) {
        lock.withLock {
            var retryCount = 0
            while (loggerEvents.size < events.size && retryCount++ < RETRY_NUMBER) {
                condition.await(TIMEOUT, SECONDS)
            }
            assertThat(loggerEvents, `is`(listOf(*events)))
        }
    }

    private class EventLogger(private val loggerEvents: MutableList<Event?>) :
        BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lock.withLock {
                loggerEvents.add(intent.getSerializableExtra(EXTRA_KEY_EVENT) as Event)
                condition.signalAll()
            }
        }
    }

    companion object {
        private const val RETRY_NUMBER = 5
        private val TIMEOUT = SECONDS.toMillis(1)
        private val lock = ReentrantLock()
        private val condition = lock.newCondition()
    }
}
