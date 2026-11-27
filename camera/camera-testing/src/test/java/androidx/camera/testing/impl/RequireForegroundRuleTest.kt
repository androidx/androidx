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

package androidx.camera.testing.impl

import android.app.Activity
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Looper
import android.view.Display
import android.view.Surface
import androidx.camera.testing.impl.RequireForegroundRule.Companion.ForegroundOccupiedError
import androidx.camera.testing.impl.activity.ForegroundTestActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertThrows
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowDisplayManager

/** Unit tests for [RequireForegroundRule]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class RequireForegroundRuleTest {

    @Test
    fun isOplusDevice_recognizesOppoOnePlusAndRealme() {
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "OPPO", brand = "OPPO"))
            .isTrue()
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "OnePlus", brand = "OnePlus"))
            .isTrue()
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "realme", brand = "realme"))
            .isTrue()
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "OPPO", brand = "OnePlus"))
            .isTrue()
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "Google", brand = "google"))
            .isFalse()
        assertThat(RequireForegroundRule.isOplusDevice(manufacturer = "samsung", brand = "samsung"))
            .isFalse()
    }

    @Test
    fun isForeignForegroundPackage_allowsTestLauncherAndSystemPackages() {
        val testPkg = "androidx.camera.integration.view.test"
        val targetPkg = "androidx.camera.integration.view"
        val launcherPkg = "com.android.launcher3"

        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = null,
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = testPkg,
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = targetPkg,
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = launcherPkg,
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.oppo.launcher",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = null,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.miui.home",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = null,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.android.systemui",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.google.android.permissioncontroller",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isFalse()
    }

    @Test
    fun isForeignForegroundPackage_detectsThirdPartyAndOemStorePackages() {
        val testPkg = "androidx.camera.integration.view.test"
        val targetPkg = "androidx.camera.integration.view"
        val launcherPkg = "com.oppo.launcher"

        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.heytap.market",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isTrue()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.transsnet.store",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isTrue()
        assertThat(
                RequireForegroundRule.isForeignForegroundPackage(
                    currentPackage = "com.android.vending",
                    testPackage = testPkg,
                    targetPackage = targetPkg,
                    launcherPackage = launcherPkg,
                )
            )
            .isTrue()
    }

    @Test
    fun mapFailureOnForegroundLoss_inLabEnvironment_forceStopsAndReturnsForegroundOccupiedError() {
        val originalError = AssertionError("PreviewView failed to reach STREAMING")
        val forceStoppedPackages = mutableListOf<String>()

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "com.heytap.market",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = true,
                onForceStopPackage = { forceStoppedPackages.add(it) },
            )

        assertThat(forceStoppedPackages).containsExactly("com.heytap.market")
        assertThat(mapped).isInstanceOf(ForegroundOccupiedError::class.java)
        assertThat(mapped)
            .hasMessageThat()
            .contains("CameraX_fail_to_start_foreground (occupied by com.heytap.market)")
        assertThat(mapped).hasCauseThat().isSameInstanceAs(originalError)
    }

    @Test
    fun mapFailureOnForegroundLoss_outsideLabEnvironment_returnsAssumptionViolatedException() {
        val originalError = AssertionError("PreviewView failed to reach STREAMING")
        val forceStoppedPackages = mutableListOf<String>()

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "com.heytap.market",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = false,
                onForceStopPackage = { forceStoppedPackages.add(it) },
            )

        assertThat(forceStoppedPackages).isEmpty()
        assertThat(mapped).isInstanceOf(AssumptionViolatedException::class.java)
        assertThat(mapped)
            .hasMessageThat()
            .contains("CameraX_fail_to_start_foreground (occupied by com.heytap.market)")
        assertThat(mapped).hasCauseThat().isSameInstanceAs(originalError)
    }

    @Test
    fun mapFailureOnForegroundLoss_whenTargetInForeground_preservesOriginalFailure() {
        val originalError = AssertionError("Real CameraX failure")
        val forceStoppedPackages = mutableListOf<String>()

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "androidx.camera.integration.view",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = true,
                onForceStopPackage = { forceStoppedPackages.add(it) },
            )

        assertThat(forceStoppedPackages).isEmpty()
        assertThat(mapped).isSameInstanceAs(originalError)
    }

    @Test
    fun mapFailureOnForegroundLoss_rejectsUnsafePackageNameForForceStop() {
        val originalError = AssertionError("PreviewView failed to reach STREAMING")
        val forceStoppedPackages = mutableListOf<String>()

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "com.evil; rm -rf /",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = true,
                onForceStopPackage = { forceStoppedPackages.add(it) },
            )

        assertThat(forceStoppedPackages).isEmpty()
        assertThat(mapped).isInstanceOf(ForegroundOccupiedError::class.java)
    }

    @Test
    fun mapFailureOnForegroundLoss_doesNotForceStopGooglePlayServicesOrVending() {
        val originalError = AssertionError("PreviewView failed to reach STREAMING")
        val forceStoppedPackages = mutableListOf<String>()

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "com.android.vending",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = true,
                onForceStopPackage = { forceStoppedPackages.add(it) },
            )

        assertThat(forceStoppedPackages).isEmpty()
        assertThat(mapped).isInstanceOf(ForegroundOccupiedError::class.java)
        assertThat(mapped)
            .hasMessageThat()
            .contains("CameraX_fail_to_start_foreground (occupied by com.android.vending)")
        assertThat(mapped).hasCauseThat().isSameInstanceAs(originalError)
    }

    @Test
    fun mapFailureOnForegroundLoss_whenForceStopThrowsException_preservesForegroundOccupiedError() {
        val originalError = AssertionError("PreviewView failed to reach STREAMING")

        val mapped =
            RequireForegroundRule.mapFailureOnForegroundLoss(
                testError = originalError,
                currentPackage = "com.heytap.market",
                testPackage = "androidx.camera.integration.view.test",
                targetPackage = "androidx.camera.integration.view",
                launcherPackage = "com.oppo.launcher",
                isLabEnvironment = true,
                onForceStopPackage = { throw IllegalStateException("UiAutomation disconnected") },
            )

        assertThat(mapped).isInstanceOf(ForegroundOccupiedError::class.java)
        assertThat(mapped).hasCauseThat().isSameInstanceAs(originalError)
    }

    @Test
    @Suppress("DEPRECATION")
    fun mapFailureOnForegroundLoss_preservesAssumptionViolatedAndForegroundOccupiedErrors() {
        val junit4Skip = AssumptionViolatedException("Skipped by assumeTrue")
        val internalSkip = org.junit.internal.AssumptionViolatedException("Legacy skip")
        val existingForegroundError = ForegroundOccupiedError("Already mapped")

        for (nonRemappable in listOf(junit4Skip, internalSkip, existingForegroundError)) {
            val mapped =
                RequireForegroundRule.mapFailureOnForegroundLoss(
                    testError = nonRemappable,
                    currentPackage = "com.heytap.market",
                    testPackage = "androidx.camera.integration.view.test",
                    targetPackage = "androidx.camera.integration.view",
                    launcherPackage = "com.oppo.launcher",
                    isLabEnvironment = true,
                    onForceStopPackage = {},
                )
            assertThat(mapped).isSameInstanceAs(nonRemappable)
        }
    }

    @Test
    fun onActivityLifecycleChanged_whenActivityResumesAfterBriefTheft_clearsOccupier() {
        val midTestOccupier = AtomicReference<String?>(null)
        val resumeGeneration = AtomicLong(0)
        val pendingCheck = AtomicReference<Future<*>?>(null)
        val scheduledTasks = mutableListOf<FutureTask<Unit>>()
        val rawRunnables = mutableListOf<Runnable>()
        var activePkg: String? = "com.heytap.market"

        // 1. Activity is paused by a foreign popup -> background check is scheduled and records it.
        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.PAUSED,
            isFinishing = false,
            isChangingConfigurations = false,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = { runnable ->
                rawRunnables.add(runnable)
                FutureTask(runnable, Unit).also { scheduledTasks.add(it) }
            },
            activePackageProvider = { activePkg },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )
        assertThat(scheduledTasks).hasSize(1)
        scheduledTasks.last().run()
        assertThat(midTestOccupier.get()).isEqualTo("com.heytap.market")

        // 2. Test Activity regains RESUMED -> recorded occupier is cleared so a subsequent real
        // test failure is not misclassified as ForegroundOccupiedError.
        activePkg = "androidx.camera.integration.view"
        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.RESUMED,
            isFinishing = false,
            isChangingConfigurations = false,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = { runnable ->
                rawRunnables.add(runnable)
                FutureTask(runnable, Unit).also { scheduledTasks.add(it) }
            },
            activePackageProvider = { activePkg },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )
        assertThat(midTestOccupier.get()).isNull()

        // 3. If a second PAUSED schedules a check, but RESUMED arrives before the delayed check
        // executes, the pending check is cancelled AND the generation guard prevents the raw
        // runnable from writing a stale occupier if it was already in flight.
        activePkg = "com.heytap.market"
        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.PAUSED,
            isFinishing = false,
            isChangingConfigurations = false,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = { runnable ->
                rawRunnables.add(runnable)
                FutureTask(runnable, Unit).also { scheduledTasks.add(it) }
            },
            activePackageProvider = { activePkg },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )
        val staleTask = scheduledTasks.last()
        val staleRunnable = rawRunnables.last()
        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.RESUMED,
            isFinishing = false,
            isChangingConfigurations = false,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = { null },
            activePackageProvider = { activePkg },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )
        assertThat(staleTask.isCancelled).isTrue()
        staleRunnable.run()
        assertThat(midTestOccupier.get()).isNull()
    }

    @Test
    fun onActivityLifecycleChanged_ignoresFinishingAndConfigurationChangeTransitions() {
        val midTestOccupier = AtomicReference<String?>(null)
        val resumeGeneration = AtomicLong(0)
        val pendingCheck = AtomicReference<Future<*>?>(null)
        var scheduledCount = 0

        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.STOPPED,
            isFinishing = true,
            isChangingConfigurations = false,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = {
                scheduledCount++
                null
            },
            activePackageProvider = { "com.heytap.market" },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )
        RequireForegroundRule.onActivityLifecycleChanged(
            stage = Stage.PAUSED,
            isFinishing = false,
            isChangingConfigurations = true,
            midTestOccupier = midTestOccupier,
            resumeGeneration = resumeGeneration,
            pendingCheck = pendingCheck,
            scheduleDelayedCheck = {
                scheduledCount++
                null
            },
            activePackageProvider = { "com.heytap.market" },
            testPackage = "androidx.camera.integration.view.test",
            targetPackage = "androidx.camera.integration.view",
            launcherPackage = "com.oppo.launcher",
        )

        assertThat(scheduledCount).isEqualTo(0)
        assertThat(midTestOccupier.get()).isNull()
    }

    @Test
    fun foregroundTestActivity_awaitDestroyed_timesOutUntilOnDestroyRuns() {
        val controller = Robolectric.buildActivity(ForegroundTestActivity::class.java).setup()
        val activity = controller.get()

        // Pausing and stopping alone must not release awaitDestroyed().
        controller.pause().stop()
        assertThat(activity.awaitDestroyed(20L)).isFalse()

        // Destroying signals mDestroyedLatch immediately.
        controller.destroy()
        assertThat(activity.awaitDestroyed(0L)).isTrue()
    }

    @Test
    fun foregroundTestActivity_awaitDestroyed_unblocksAsyncWaiterOnDestroy() {
        val controller = Robolectric.buildActivity(ForegroundTestActivity::class.java).setup()
        val activity = controller.get()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val awaitFuture = executor.submit<Boolean> { activity.awaitDestroyed(5000L) }
            controller.pause().stop().destroy()
            assertThat(awaitFuture.get()).isTrue()
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun foregroundTestActivity_awaitDestroyed_throwsInterruptedExceptionWhenInterrupted() {
        val controller = Robolectric.buildActivity(ForegroundTestActivity::class.java).setup()
        val activity = controller.get()

        Thread.currentThread().interrupt()
        try {
            assertThrows(InterruptedException::class.java) { activity.awaitDestroyed(5000L) }
        } finally {
            Thread.interrupted()
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun lockAndSettleNaturalOrientation_fallsBackToUiAutomationOnApi30FailureOrTimeout() {
        // API 30+ happy path: display-scoped lock settles without calling UiAutomation fallback.
        var displayCalls = 0
        var uiAutomationCalls = 0
        var awaitCalls = 0
        assertThat(
                RequireForegroundRule.lockAndSettleNaturalOrientation(
                    lockDisplayScoped = { displayCalls++ },
                    lockUiAutomation = { uiAutomationCalls++ },
                    awaitSettled = {
                        awaitCalls++
                        true
                    },
                )
            )
            .isTrue()
        assertThat(displayCalls).isEqualTo(1)
        assertThat(uiAutomationCalls).isEqualTo(0)
        assertThat(awaitCalls).isEqualTo(1)

        // API 30+ exception path: falls back once and does not re-invoke UiAutomation on timeout.
        uiAutomationCalls = 0
        awaitCalls = 0
        assertThat(
                RequireForegroundRule.lockAndSettleNaturalOrientation(
                    lockDisplayScoped = { throw RuntimeException("shell failed") },
                    lockUiAutomation = { uiAutomationCalls++ },
                    awaitSettled = {
                        awaitCalls++
                        false
                    },
                    isInterrupted = { false },
                )
            )
            .isFalse()
        assertThat(uiAutomationCalls).isEqualTo(1)
        assertThat(awaitCalls).isEqualTo(1)

        // API 30+ silent-ignore path: falls back lazily when display-scoped lock fails to settle.
        displayCalls = 0
        uiAutomationCalls = 0
        awaitCalls = 0
        assertThat(
                RequireForegroundRule.lockAndSettleNaturalOrientation(
                    lockDisplayScoped = { displayCalls++ },
                    lockUiAutomation = { uiAutomationCalls++ },
                    awaitSettled = { ++awaitCalls >= 2 },
                    isInterrupted = { false },
                )
            )
            .isTrue()
        assertThat(displayCalls).isEqualTo(1)
        assertThat(uiAutomationCalls).isEqualTo(1)
        assertThat(awaitCalls).isEqualTo(2)

        // Interrupted caller skips fallback so interruption returns promptly.
        uiAutomationCalls = 0
        awaitCalls = 0
        assertThat(
                RequireForegroundRule.lockAndSettleNaturalOrientation(
                    lockDisplayScoped = {},
                    lockUiAutomation = { uiAutomationCalls++ },
                    awaitSettled = {
                        awaitCalls++
                        false
                    },
                    isInterrupted = { true },
                )
            )
            .isFalse()
        assertThat(uiAutomationCalls).isEqualTo(0)
        assertThat(awaitCalls).isEqualTo(1)

        // Pre-API 30 path: invokes UiAutomation directly without double fallback.
        uiAutomationCalls = 0
        awaitCalls = 0
        assertThat(
                RequireForegroundRule.lockAndSettleNaturalOrientation(
                    lockDisplayScoped = null,
                    lockUiAutomation = { uiAutomationCalls++ },
                    awaitSettled = {
                        awaitCalls++
                        false
                    },
                    isInterrupted = { false },
                )
            )
            .isFalse()
        assertThat(uiAutomationCalls).isEqualTo(1)
        assertThat(awaitCalls).isEqualTo(1)
    }

    @Test
    @Suppress("DEPRECATION")
    fun awaitDefaultDisplayRotation_handlesFastPathTransitionTimeoutAndInterrupt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val defaultDisplay = requireNotNull(displayManager.getDisplay(Display.DEFAULT_DISPLAY))

        // Fast-path: already at ROTATION_0, returns true immediately even on the main thread.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
        assertThat(
                RequireForegroundRule.awaitDefaultDisplayRotation(
                    displayManager = displayManager,
                    expectedRotation = Surface.ROTATION_0,
                    timeoutMs = 50L,
                )
            )
            .isTrue()

        // Guard: when rotation has not settled, blocking the main thread is rejected.
        shadowOf(defaultDisplay).setRotation(Surface.ROTATION_90)
        assertThrows(IllegalStateException::class.java) {
            RequireForegroundRule.awaitDefaultDisplayRotation(
                displayManager = displayManager,
                expectedRotation = Surface.ROTATION_0,
                timeoutMs = 50L,
            )
        }

        val executor = Executors.newSingleThreadExecutor()
        try {
            // Timeout path: stays at ROTATION_90 -> returns false cleanly.
            val timeoutFuture =
                executor.submit<Boolean> {
                    RequireForegroundRule.awaitDefaultDisplayRotation(
                        displayManager = displayManager,
                        expectedRotation = Surface.ROTATION_0,
                        timeoutMs = 25L,
                    )
                }
            pumpMainLooperUntilDone(timeoutFuture)
            assertThat(timeoutFuture.get(2, TimeUnit.SECONDS)).isFalse()

            // Pre-interrupted caller thread: returns false and preserves interrupt flag.
            val interruptedPreservedFuture =
                executor.submit<Boolean> {
                    Thread.currentThread().interrupt()
                    try {
                        val settled =
                            RequireForegroundRule.awaitDefaultDisplayRotation(
                                displayManager = displayManager,
                                expectedRotation = Surface.ROTATION_0,
                                timeoutMs = 5000L,
                            )
                        assertThat(settled).isFalse()
                        Thread.currentThread().isInterrupted
                    } finally {
                        Thread.interrupted()
                    }
                }
            pumpMainLooperUntilDone(interruptedPreservedFuture)
            assertThat(interruptedPreservedFuture.get(2, TimeUnit.SECONDS)).isTrue()

            // Event-driven transition path: wait until the worker is blocked in settledLatch.await
            // (after registering DisplayListener) before triggering ROTATION_0. Use a 60s timeout
            // well above the 5s looper-pump window so removing DisplayListener fails by timeout.
            val workerThreadRef = AtomicReference<Thread?>(null)
            val settleFuture =
                executor.submit<Boolean> {
                    workerThreadRef.set(Thread.currentThread())
                    RequireForegroundRule.awaitDefaultDisplayRotation(
                        displayManager = displayManager,
                        expectedRotation = Surface.ROTATION_0,
                        timeoutMs = 60_000L,
                    )
                }
            val awaitBlockedDeadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (
                workerThreadRef.get()?.state != Thread.State.TIMED_WAITING &&
                    System.nanoTime() < awaitBlockedDeadlineNs
            ) {
                Thread.yield()
            }
            assertThat(workerThreadRef.get()?.state).isEqualTo(Thread.State.TIMED_WAITING)
            assertThat(settleFuture.isDone).isFalse()

            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            ShadowDisplayManager.changeDisplay(Display.DEFAULT_DISPLAY, "")
            pumpMainLooperUntilDone(settleFuture)
            assertThat(settleFuture.get(2, TimeUnit.SECONDS)).isTrue()
        } finally {
            shadowOf(defaultDisplay).setRotation(Surface.ROTATION_0)
            executor.shutdownNow()
        }
    }

    @Test
    fun hasVisibleActivity_treatsPausedAsVisibleAndStoppedAsNotVisible() {
        val controller = Robolectric.buildActivity(ForegroundTestActivity::class.java).setup()
        val activity = controller.get()
        val monitor = FakeActivityLifecycleMonitor()
        try {
            for (visibleStage in
                listOf(Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.RESTARTED)) {
                monitor.setActivityStage(activity, visibleStage)
                assertThat(RequireForegroundRule.hasVisibleActivity(monitor)).isTrue()
            }

            for (nonVisibleStage in
                listOf(Stage.PRE_ON_CREATE, Stage.CREATED, Stage.STOPPED, Stage.DESTROYED)) {
                monitor.setActivityStage(activity, nonVisibleStage)
                assertThat(RequireForegroundRule.hasVisibleActivity(monitor)).isFalse()
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun awaitTargetActivitiesStopped_waitsUntilPausedActivityReachesStopped() {
        val controller = Robolectric.buildActivity(ForegroundTestActivity::class.java).setup()
        val activity = controller.get()
        val monitor = FakeActivityLifecycleMonitor()

        val executor = Executors.newSingleThreadExecutor()
        try {
            // Guard: blocking the main thread is rejected.
            monitor.setActivityStage(activity, Stage.PAUSED)
            assertThrows(IllegalStateException::class.java) {
                RequireForegroundRule.awaitTargetActivitiesStopped(
                    timeoutMs = 50L,
                    monitor = monitor,
                )
            }
            monitor.setActivityStage(activity, Stage.STOPPED)

            // Fast-path when already STOPPED on background thread.
            val fastFuture =
                executor.submit<Boolean> {
                    RequireForegroundRule.awaitTargetActivitiesStopped(
                        timeoutMs = 500L,
                        monitor = monitor,
                    )
                }
            pumpMainLooperUntilDone(fastFuture)
            assertThat(fastFuture.get(2, TimeUnit.SECONDS)).isTrue()
            assertThat(monitor.callbacks).isEmpty()

            // Slow-path when PAUSED during Home transition: blocks until STOPPED arrives.
            val slowCallbackLatch = monitor.resetCallbackAddedLatch()
            monitor.setActivityStage(activity, Stage.PAUSED)
            val awaitFuture =
                executor.submit<Boolean> {
                    RequireForegroundRule.awaitTargetActivitiesStopped(
                        timeoutMs = 5000L,
                        monitor = monitor,
                    )
                }
            assertThat(slowCallbackLatch.await(2, TimeUnit.SECONDS)).isTrue()
            shadowOf(Looper.getMainLooper()).idle()
            assertThat(awaitFuture.isDone).isFalse()

            monitor.dispatchStage(activity, Stage.STOPPED)
            pumpMainLooperUntilDone(awaitFuture)
            assertThat(awaitFuture.get(2, TimeUnit.SECONDS)).isTrue()
            assertThat(monitor.callbacks).isEmpty()

            // Pre-interrupted caller thread: returns false, preserves interrupt, removes callback.
            monitor.setActivityStage(activity, Stage.PAUSED)
            val interruptPreservedFuture =
                executor.submit<Boolean> {
                    Thread.currentThread().interrupt()
                    try {
                        val stopped =
                            RequireForegroundRule.awaitTargetActivitiesStopped(
                                timeoutMs = 5000L,
                                monitor = monitor,
                            )
                        assertThat(stopped).isFalse()
                        Thread.currentThread().isInterrupted
                    } finally {
                        Thread.interrupted()
                    }
                }
            pumpMainLooperUntilDone(interruptPreservedFuture)
            assertThat(interruptPreservedFuture.get(2, TimeUnit.SECONDS)).isTrue()
            assertThat(monitor.callbacks).isEmpty()
        } finally {
            executor.shutdownNow()
            controller.pause().stop().destroy()
        }
    }

    private fun pumpMainLooperUntilDone(future: Future<*>) {
        val deadlineNs = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!future.isDone && System.nanoTime() < deadlineNs) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.yield()
        }
    }

    private class FakeActivityLifecycleMonitor : ActivityLifecycleMonitor {
        val callbacks = CopyOnWriteArrayList<ActivityLifecycleCallback>()
        private val callbackAddedLatchRef = AtomicReference(CountDownLatch(1))
        private val stages = mutableMapOf<Activity, Stage>()

        fun resetCallbackAddedLatch(): CountDownLatch =
            CountDownLatch(1).also { callbackAddedLatchRef.set(it) }

        fun setActivityStage(activity: Activity, stage: Stage) {
            stages[activity] = stage
        }

        fun dispatchStage(activity: Activity, stage: Stage) {
            stages[activity] = stage
            for (callback in callbacks) {
                callback.onActivityLifecycleChanged(activity, stage)
            }
        }

        override fun addLifecycleCallback(callback: ActivityLifecycleCallback) {
            callbacks.add(callback)
            callbackAddedLatchRef.get().countDown()
        }

        override fun removeLifecycleCallback(callback: ActivityLifecycleCallback) {
            callbacks.remove(callback)
        }

        override fun getLifecycleStageOf(activity: Activity): Stage =
            stages[activity] ?: Stage.PRE_ON_CREATE

        override fun getActivitiesInStage(stage: Stage): Collection<Activity> =
            stages.filterValues { it == stage }.keys
    }
}
