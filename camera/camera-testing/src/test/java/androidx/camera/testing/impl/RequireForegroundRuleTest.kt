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

import androidx.camera.testing.impl.RequireForegroundRule.Companion.ForegroundOccupiedError
import androidx.test.runner.lifecycle.Stage
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

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
}
