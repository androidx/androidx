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
package androidx.xr.arcore.testapp.common

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.testapp.MainActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.QrCodeTrackingMode
import androidx.xr.testutils.TestAppTest.CatchUncaughtExceptionsRule
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SessionLifecycleHelperTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.FACE_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
            )
        )

    @get:Rule
    val uncaughtExceptionsRule: CatchUncaughtExceptionsRule = CatchUncaughtExceptionsRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application = instrumentation.targetContext.applicationContext as Application
    private val launchedActivity = AtomicReference<MainActivity?>()
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    @After
    fun tearDown() {
        lifecycleCallbacks?.let { application.unregisterActivityLifecycleCallbacks(it) }
        launchedActivity.get()?.let { activity ->
            instrumentation.runOnMainSync {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.finish()
                }
            }
            waitUntil("MainActivity to destroy during teardown", timeout = testTimeout) {
                activity.isDestroyed
            }
        }
    }

    @Test
    fun tryCreateSession_configurationRejected_finishesActivityWithoutCrash() {
        val sessionAvailableCalled = AtomicBoolean(false)
        val helperRef = AtomicReference<SessionLifecycleHelper?>()

        launchMainActivity { activity ->
            val helper =
                SessionLifecycleHelper(
                    activity,
                    invalidQrCodeConfig,
                    onSessionAvailable = { sessionAvailableCalled.set(true) },
                    onSessionCreateActionRequired = {
                        error("Unexpected session create action required: $it")
                    },
                    onSessionCalibrationRequired = {
                        error("Unexpected session calibration required: $it")
                    },
                )
            helperRef.set(helper)
            helper.tryCreateSession()
        }

        waitUntil("MainActivity to destroy after rejected configuration", timeout = testTimeout) {
            isActivityDestroyed()
        }
        val helper = checkNotNull(helperRef.get())
        assertThat(sessionAvailableCalled.get()).isFalse()
        assertThat(helper.lastErrorMessage).isEqualTo("Session configuration not supported.")
    }

    @Test
    fun tryCreateSession_unsupportedOperation_finishesActivityWithoutCrash() {
        val sessionAvailableCalled = AtomicBoolean(false)
        val helperRef = AtomicReference<SessionLifecycleHelper?>()

        launchMainActivity { activity ->
            val helper =
                SessionLifecycleHelper(
                    activity,
                    unsupportedOperationConfig,
                    onSessionAvailable = { sessionAvailableCalled.set(true) },
                    onSessionCreateActionRequired = {
                        error("Unexpected session create action required: $it")
                    },
                    onSessionCalibrationRequired = {
                        error("Unexpected session calibration required: $it")
                    },
                )
            helperRef.set(helper)
            helper.tryCreateSession()
        }

        waitUntil(
            "MainActivity to destroy after unsupported configuration",
            timeout = testTimeout,
        ) {
            isActivityDestroyed()
        }
        val helper = checkNotNull(helperRef.get())
        assertThat(sessionAvailableCalled.get()).isFalse()
        assertThat(helper.lastErrorMessage).isEqualTo("Session configuration not supported.")
    }

    @Test
    fun tryUpdateConfig_configurationRejected_finishesActivityWithoutCrash() {
        val sessionAvailable = CountDownLatch(1)
        val helperRef = AtomicReference<SessionLifecycleHelper?>()
        launchMainActivity { activity ->
            val helper =
                SessionLifecycleHelper(
                    activity,
                    validConfig,
                    onSessionAvailable = { sessionAvailable.countDown() },
                )
            helperRef.set(helper)
            helper.tryCreateSession()
        }
        check(sessionAvailable.await(testTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            "Session was not created with a valid config"
        }
        val helper = checkNotNull(helperRef.get())

        instrumentation.runOnMainSync { helper.tryUpdateConfig(invalidQrCodeConfig) }

        waitUntil("MainActivity to destroy after rejected config update", timeout = testTimeout) {
            isActivityDestroyed()
        }
        assertThat(helper.lastErrorMessage).isEqualTo("Session configuration not supported.")
    }

    @Test
    fun tryUpdateConfig_callbackThrowsIllegalArgumentException_propagatesException() {
        val sessionAvailable = CountDownLatch(1)
        val invocationCount = AtomicInteger(0)
        val helperRef = AtomicReference<SessionLifecycleHelper?>()
        launchMainActivity { activity ->
            val helper =
                SessionLifecycleHelper(
                    activity,
                    validConfig,
                    onSessionAvailable = {
                        if (invocationCount.incrementAndGet() == 1) {
                            sessionAvailable.countDown()
                        } else {
                            throw IllegalArgumentException(CALLBACK_ERROR_MESSAGE)
                        }
                    },
                )
            helperRef.set(helper)
            helper.tryCreateSession()
        }
        check(sessionAvailable.await(testTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            "Session was not created with a valid config"
        }
        val helper = checkNotNull(helperRef.get())

        val failure =
            assertThrows(IllegalArgumentException::class.java) {
                instrumentation.runOnMainSync { helper.tryUpdateConfig(validConfig) }
            }

        assertThat(failure).hasMessageThat().isEqualTo(CALLBACK_ERROR_MESSAGE)
        assertThat(helper.lastErrorMessage).isNull()
        assertThat(checkNotNull(launchedActivity.get()).isFinishing).isFalse()
    }

    /**
     * Starts [MainActivity] and runs [onCreated] on the main thread while the activity is being
     * created, before it is started, so that [SessionLifecycleHelper] can register its activity
     * result launcher.
     */
    private fun launchMainActivity(onCreated: (MainActivity) -> Unit) {
        val callbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    if (
                        activity is MainActivity && launchedActivity.compareAndSet(null, activity)
                    ) {
                        onCreated(activity)
                    }
                }

                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {}

                override fun onActivityPaused(activity: Activity) {}

                override fun onActivityStopped(activity: Activity) {}

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {}
            }
        lifecycleCallbacks = callbacks
        application.registerActivityLifecycleCallbacks(callbacks)
        instrumentation.targetContext.startActivity(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun isActivityDestroyed(): Boolean = launchedActivity.get()?.isDestroyed == true

    private companion object {
        val testTimeout = 20.seconds
        const val CALLBACK_ERROR_MESSAGE = "Simulated callback failure"

        val validConfig: Config =
            Config.Builder().setDeviceTracking(DeviceTrackingMode.SPATIAL).build()

        // OpenXrRuntime.configure() rejects a negative QR code size with IllegalArgumentException.
        val invalidQrCodeConfig: Config =
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setQrCodeTracking(QrCodeTrackingMode.DYNAMIC)
                .setQrCodeSizeMeters(-0.1f)
                .build()

        // OpenXrRuntime.configure() rejects simultaneous smooth and raw depth with
        // UnsupportedOperationException.
        val unsupportedOperationConfig: Config =
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setDepthEstimation(DepthEstimationMode.SMOOTH_AND_RAW)
                .build()
    }
}
