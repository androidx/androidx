/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.credentials.agesignals.provider.playservices

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.credentials.agesignals.AgeSignalResultHandler
import androidx.credentials.agesignals.GetAgeRangeResponse
import androidx.credentials.agesignals.exceptions.GetAgeRangeUnknownException
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AgeSignalHiddenActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun activityTheme_isThemeAgeSignalHidden() {
        val componentName = ComponentName(context, AgeSignalHiddenActivity::class.java)
        val activityInfo = context.packageManager.getActivityInfo(componentName, 0)

        assertThat(activityInfo.theme).isEqualTo(R.style.Theme_AgeSignalHidden)
    }

    @Test
    fun launch_withoutResultReceiver_finishesImmediately() {
        val intent = Intent(context, AgeSignalHiddenActivity::class.java)

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun launch_withoutPendingIntent_reportsFailureAndFinishes() {
        val latch = CountDownLatch(1)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = null

        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }

        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_CANCELED)
            val exception =
                AgeSignalResultHandler.retrieveGetAgeRangeException(capturedResultData!!)
            assertThat(exception).isInstanceOf(GetAgeRangeUnknownException::class.java)
        }
    }

    @Test
    fun launch_withCanceledPendingIntent_reportsFailureAndFinishes() {
        val latch = CountDownLatch(1)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = null

        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }

        val pendingIntent = createInertPendingIntent()
        pendingIntent.cancel()

        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_PENDING_INTENT, pendingIntent)

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_CANCELED)
            val exception =
                AgeSignalResultHandler.retrieveGetAgeRangeException(capturedResultData!!)
            assertThat(exception).isInstanceOf(GetAgeRangeUnknownException::class.java)
            assertThat(exception!!.message).startsWith("Failed to start intent sender:")
        }
    }

    @Test
    fun recreate_whileWaitingForActivityResult_preservesStateAndForwardsResult() {
        val latch = CountDownLatch(1)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = null
        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }
        val pendingIntent =
            createInertPendingIntent(
                requestCode = 1,
                flags =
                    PendingIntent.FLAG_CANCEL_CURRENT or
                        PendingIntent.FLAG_ONE_SHOT or
                        PendingIntent.FLAG_IMMUTABLE,
            )
        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_PENDING_INTENT, pendingIntent)

        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 18,
                upperAgeBound = null,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val dataIntent =
            Intent().putExtras(AgeSignalResultHandler.toResponseBundle(expectedResponse))

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.handleActivityResult(
                    AgeSignalHiddenActivity.REQUEST_CODE,
                    Activity.RESULT_OK,
                    dataIntent,
                )
            }
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(capturedResultData!!))
                .isEqualTo(expectedResponse)
        }
    }

    @Test
    fun onActivityResult_withWrongRequestCode_reportsFailureAndFinishes() {
        val latch = CountDownLatch(1)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = null
        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }
        val pendingIntent = createInertPendingIntent()
        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_PENDING_INTENT, pendingIntent)

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.handleActivityResult(999, Activity.RESULT_OK, null)
            }
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(AgeSignalResultHandler.retrieveGetAgeRangeException(capturedResultData!!))
                .isInstanceOf(GetAgeRangeUnknownException::class.java)
        }
    }

    @Test
    fun onActivityResult_withMatchingRequestCode_forwardsResultExtrasAndFinishes() {
        val latch = CountDownLatch(1)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = null
        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }
        val pendingIntent = createInertPendingIntent()
        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_PENDING_INTENT, pendingIntent)

        val expectedResponse =
            GetAgeRangeResponse(
                lowerAgeBound = 18,
                upperAgeBound = null,
                assuranceTier = GetAgeRangeResponse.ASSURANCE_TIER_A,
            )
        val dataIntent =
            Intent().putExtras(AgeSignalResultHandler.toResponseBundle(expectedResponse))

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.handleActivityResult(
                    AgeSignalHiddenActivity.REQUEST_CODE,
                    Activity.RESULT_OK,
                    dataIntent,
                )
            }
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_OK)
            assertThat(AgeSignalResultHandler.retrieveGetAgeRangeResponse(capturedResultData!!))
                .isEqualTo(expectedResponse)
        }
    }

    @Test
    fun finish_whileWaitingForActivityResult_sendsResultCanceledOnce() {
        val latch = CountDownLatch(1)
        val receiveCount = AtomicInteger(0)
        var capturedResultCode: Int? = null
        var capturedResultData: Bundle? = Bundle()
        val receiver =
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    receiveCount.incrementAndGet()
                    capturedResultCode = resultCode
                    capturedResultData = resultData
                    latch.countDown()
                }
            }
        val pendingIntent = createInertPendingIntent()
        val intent = Intent(context, AgeSignalHiddenActivity::class.java)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_RESULT_RECEIVER, receiver)
        intent.putExtra(AgeSignalHiddenActivity.EXTRA_PENDING_INTENT, pendingIntent)

        ActivityScenario.launch<AgeSignalHiddenActivity>(intent).use { scenario ->
            scenario.onActivity { activity -> activity.finish() }
            assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue()
            scenario.waitUntilDestroyed()
            assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            assertThat(receiveCount.get()).isEqualTo(1)
            assertThat(capturedResultCode).isEqualTo(Activity.RESULT_CANCELED)
            assertThat(capturedResultData).isNull()
        }
    }

    private fun createInertPendingIntent(
        requestCode: Int = 0,
        flags: Int = PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent("androidx.credentials.agesignals.provider.playservices.test.ACTION_INERT")
                .setPackage(context.packageName),
            flags,
        )

    private fun ActivityScenario<AgeSignalHiddenActivity>.waitUntilDestroyed() {
        val destroyedLatch = CountDownLatch(1)
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        val callback = ActivityLifecycleCallback { activity, stage ->
            if (activity is AgeSignalHiddenActivity && stage == Stage.DESTROYED) {
                destroyedLatch.countDown()
            }
        }
        monitor.addLifecycleCallback(callback)
        try {
            if (state != Lifecycle.State.DESTROYED) {
                destroyedLatch.await(3, TimeUnit.SECONDS)
            }
        } finally {
            monitor.removeLifecycleCallback(callback)
        }
    }
}
