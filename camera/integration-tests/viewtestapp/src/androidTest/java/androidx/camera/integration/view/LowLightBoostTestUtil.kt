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

package androidx.camera.integration.view

import android.annotation.SuppressLint
import android.app.Activity
import android.app.UiAutomation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.view.TransformExperimental
import androidx.camera.view.internal.compat.quirk.DeviceQuirks
import androidx.camera.view.internal.compat.quirk.SurfaceViewNotCroppedByParentQuirk
import androidx.camera.view.internal.compat.quirk.SurfaceViewStretchedQuirk
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertWithMessage
import java.util.Collections
import kotlin.math.hypot
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue

/**
 * Shared instrumentation test utilities for [LowLightBoostEffectDeviceTest] and
 * [LowLightBoostEffectLifecycleTest].
 */
internal object LowLightBoostTestUtil {
    private const val TAG = "LowLightBoostTestUtil"

    private val ACTIVE_STAGES =
        listOf(
            Stage.RESUMED,
            Stage.STARTED,
            Stage.PAUSED,
            Stage.STOPPED,
            Stage.CREATED,
            Stage.PRE_ON_CREATE,
        )

    /**
     * Locates an active [Activity] instance of [clazz] across lifecycle stages via
     * [ActivityLifecycleMonitorRegistry].
     *
     * Note: These integration tests launch activities via explicit `Intent`s, navigate between
     * [MainActivity] and [LowLightBoostEffectActivity], and trigger Home/Resume and screen-off
     * transitions via `UiDevice`, so querying [ActivityLifecycleMonitorRegistry] is required to
     * inspect whichever activity instance is currently alive.
     */
    internal fun <T : Activity> findActiveActivity(clazz: Class<T>): T? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            for (stage in ACTIVE_STAGES) {
                if (stage == Stage.PRE_ON_CREATE) continue
                val match =
                    monitor.getActivitiesInStage(stage).filterIsInstance(clazz).firstOrNull()
                if (match != null) return match
            }
            return null
        }
        var result: T? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = findActiveActivity(clazz)
        }
        return result
    }

    private fun <T : Activity> findAllNonDestroyedActivities(clazz: Class<T>): List<T> {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        return ACTIVE_STAGES.flatMap { stage ->
            monitor.getActivitiesInStage(stage).filterIsInstance(clazz)
        }
    }

    internal fun findActiveFragment(): LowLightBoostEffectFragment? {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            val mainActivity = findActiveActivity(MainActivity::class.java) ?: return null
            return mainActivity.supportFragmentManager.fragments
                .filterIsInstance<LowLightBoostEffectFragment>()
                .firstOrNull()
        }
        var result: LowLightBoostEffectFragment? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = findActiveFragment()
        }
        return result
    }

    /**
     * Waits until the active test [Activity] ([LowLightBoostEffectActivity] when [isActivity] is
     * true, or [MainActivity] when false) reaches one of [expectedStages] in
     * [ActivityLifecycleMonitorRegistry].
     */
    internal fun waitForActivityStage(
        isActivity: Boolean,
        vararg expectedStages: Stage,
        timeoutMs: Long = 5000L,
    ): Boolean {
        val clazz =
            if (isActivity) {
                LowLightBoostEffectActivity::class.java
            } else {
                MainActivity::class.java
            }
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        return waitForCondition(timeoutMs) {
            var matched = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                matched = expectedStages.any { stage ->
                    monitor.getActivitiesInStage(stage).any { clazz.isInstance(it) }
                }
            }
            matched
        }
    }

    internal fun clickView(isActivity: Boolean, viewId: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view =
                if (isActivity) {
                    findActiveActivity(LowLightBoostEffectActivity::class.java)
                        ?.findViewById<View>(viewId)
                } else {
                    findActiveFragment()?.view?.findViewById<View>(viewId)
                }
            requireNotNull(view) { "View $viewId not found in hierarchy" }
            view.performClick()
        }
    }

    internal fun clickRadioButton(isActivity: Boolean, radioButtonId: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view =
                if (isActivity) {
                    findActiveActivity(LowLightBoostEffectActivity::class.java)
                        ?.findViewById<RadioButton>(radioButtonId)
                } else {
                    findActiveFragment()?.view?.findViewById<RadioButton>(radioButtonId)
                }
            requireNotNull(view) { "RadioButton $radioButtonId not found in hierarchy" }
            view.performClick()
        }
    }

    internal fun getStatusText(isActivity: Boolean): String {
        var text = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val textView =
                if (isActivity) {
                    findActiveActivity(LowLightBoostEffectActivity::class.java)
                        ?.findViewById<TextView>(R.id.status_text)
                } else {
                    findActiveFragment()?.view?.findViewById<TextView>(R.id.status_text)
                }
            text = textView?.text?.toString() ?: ""
        }
        return text
    }

    internal fun waitForStatusContains(
        isActivity: Boolean,
        expectedSubstring: String,
        timeoutMs: Long = 10000L,
    ): Boolean {
        return waitForCondition(timeoutMs) {
            getStatusText(isActivity).contains(expectedSubstring)
        }
    }

    @SuppressLint("BanThreadSleep")
    internal fun waitForCondition(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val start = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            if (condition()) {
                return true
            }
            Thread.sleep(200)
        }
        return false
    }

    /**
     * Waits until [frameCountProvider], e.g. a count of camera capture results, increased by at
     * least [delta] from [initialCount]. It shows that the camera stream keeps running, not that
     * frames are rendered.
     */
    @SuppressLint("BanThreadSleep")
    internal fun waitForFrames(
        frameCountProvider: () -> Long,
        initialCount: Long,
        delta: Long,
        timeoutMs: Long,
    ): Boolean {
        val start = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            if (frameCountProvider() - initialCount >= delta) {
                return true
            }
            Thread.sleep(200)
        }
        return false
    }

    /**
     * Asserts that the viewfinder keeps showing new frames, i.e. frames reach the output of the
     * effect, not only the camera.
     *
     * Downscaled copies of the viewfinder's [SurfaceView] or [TextureView] are sampled every
     * [VIEWFINDER_SAMPLE_INTERVAL_MS], and [VIEWFINDER_MIN_CHANGES] samples must differ from the
     * previous one within [timeoutMs]. A frozen viewfinder shows the same buffer, so its samples
     * are identical. Requiring several changes ignores a one-off change, e.g. from a layout or
     * transform update after a rotation.
     *
     * The camera must see a lit or textured scene, and the device must be able to copy the
     * viewfinder content. A uniform viewfinder, e.g. black with the lens covered, or a solid color
     * when screenshots don't include the camera surface (seen on emulators for `SurfaceView`,
     * including after dynamic `SurfaceControl` content rotation in `CameraXViewfinder`), can't be
     * told apart from an output that receives no frames. So a uniform viewfinder skips this check,
     * with a warning, only if camera capture results confirm the stream is advancing and either the
     * content for the current host, facing, and surface type hasn't changed since
     * [resetViewfinderBaseline], or it is a `SurfaceView` on an emulator. Otherwise, it fails. Each
     * test should call this right after the camera starts, before any transition, to take that
     * baseline.
     */
    @SuppressLint("BanThreadSleep")
    @RequiresApi(Build.VERSION_CODES.R)
    internal fun assertViewfinderUpdating(
        isActivity: Boolean,
        message: String,
        timeoutMs: Long = 10000L,
    ) {
        val captureResultCount = {
            if (isActivity) {
                LowLightBoostEffectActivity.captureResultCount
            } else {
                LowLightBoostEffectFragment.captureResultCount
            }
        }
        var baselineCaptureResultCount = captureResultCount()
        val hostKey = if (isActivity) "Activity" else "Fragment"
        val facingKey = if (getStatusText(isActivity).contains("Facing: Front")) "Front" else "Back"
        var previous: IntArray? = null
        var lastSurfaceType: String? = null
        var samples = 0
        var changes = 0
        val start = SystemClock.elapsedRealtime()
        while (
            changes < VIEWFINDER_MIN_CHANGES && SystemClock.elapsedRealtime() - start < timeoutMs
        ) {
            val sample = captureViewfinderPixels(isActivity)
            if (sample != null) {
                val surfaceType = sample.surfaceType
                if (surfaceType != lastSurfaceType) {
                    // Reset baseline when switching surfaces so TextureView <-> SurfaceView swaps
                    // or earlier frames don't satisfy the check for the newly active surface.
                    if (lastSurfaceType != null) {
                        baselineCaptureResultCount = captureResultCount()
                    }
                    samples = 1
                    changes = 0
                    previous = sample.pixels
                    lastSurfaceType = surfaceType
                } else {
                    samples++
                    if (previous != null && !previous.contentEquals(sample.pixels)) {
                        changes++
                    }
                    previous = sample.pixels
                }
                if (
                    isEmulator() &&
                        surfaceType == "SurfaceView" &&
                        samples >= 5 &&
                        changes == 0 &&
                        isUniform(sample.pixels) &&
                        captureResultCount() - baselineCaptureResultCount >= VIEWFINDER_MIN_CHANGES
                ) {
                    break
                }
            }
            Thread.sleep(VIEWFINDER_SAMPLE_INTERVAL_MS)
        }
        val surfaceKey = "$hostKey:$facingKey:${lastSurfaceType ?: "Unknown"}"
        if (changes >= VIEWFINDER_MIN_CHANGES) {
            viewfinderChangedSurfaces.add(surfaceKey)
            return
        }
        val lastSample = previous
        val isUniformSample = lastSample != null && isUniform(lastSample)
        val captureResults = captureResultCount() - baselineCaptureResultCount
        val isEmulatorSurfaceView = isEmulator() && lastSurfaceType == "SurfaceView"
        if (
            isUniformSample &&
                captureResults >= VIEWFINDER_MIN_CHANGES &&
                (surfaceKey !in viewfinderChangedSurfaces || isEmulatorSurfaceView)
        ) {
            Log.w(
                TAG,
                "$message: skipping the viewfinder check because the $surfaceKey viewfinder is " +
                    "uniform (mean luma ${meanLuma(lastSample)}; capture results: " +
                    "$captureResults), e.g. because the lens is covered or screenshots don't " +
                    "include the camera surface.",
            )
            return
        }
        assertWithMessage(
                "$message: viewfinder content changed $changes times in $samples samples " +
                    "(last sample: mean luma ${lastSample?.let { meanLuma(it) }}, uniform " +
                    "${lastSample?.let { isUniform(it) }}; capture results: $captureResults; " +
                    "status: ${getStatusText(isActivity)})"
            )
            .that(changes)
            .isAtLeast(VIEWFINDER_MIN_CHANGES)
    }

    /** Resets the viewfinder baseline of [assertViewfinderUpdating]. Call it before each test. */
    internal fun resetViewfinderBaseline() {
        viewfinderChangedSurfaces.clear()
    }

    private class ViewfinderSample(val pixels: IntArray, val surfaceType: String)

    /**
     * Returns the pixels and view type of a downscaled copy of the current viewfinder frame, or
     * null if it isn't available yet.
     */
    private fun captureViewfinderPixels(isActivity: Boolean): ViewfinderSample? {
        var isSurfaceViewReady = false
        var surfaceType: String? = null
        var bitmap: Bitmap? = null
        val screenRect = Rect()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root =
                if (isActivity) {
                    findActiveActivity(LowLightBoostEffectActivity::class.java)?.window?.decorView
                } else {
                    findActiveFragment()?.view
                }
            val view = root?.let { findLargestViewfinderSurface(it) }
            if (view != null && view.isShown) {
                // In Activity, CameraXViewfinder scales its SurfaceView via a Compose graphicsLayer
                // to fill compose_viewfinder, so read the visible screen bounds from the container.
                val boundsView =
                    if (isActivity) {
                        root.findViewById<View>(R.id.compose_viewfinder) ?: view
                    } else {
                        view
                    }
                screenRect.set(visibleScreenRectOf(boundsView))
                // Exclude the action bar (and its drop shadow) and the bottom control panel drawn
                // over the viewfinder.
                val actionBar =
                    view.rootView.findViewById<View>(androidx.appcompat.R.id.action_bar_container)
                if (actionBar != null && actionBar.isShown) {
                    val actionBarRect = visibleScreenRectOf(actionBar)
                    if (!actionBarRect.isEmpty) {
                        val shadowPx = (actionBar.elevation * 2f).toInt()
                        screenRect.top = maxOf(screenRect.top, actionBarRect.bottom + shadowPx)
                    }
                }
                val panel = root.findViewById<View>(R.id.bottom_panel)
                if (panel != null && panel.isShown) {
                    val panelRect = visibleScreenRectOf(panel)
                    if (!panelRect.isEmpty) {
                        screenRect.bottom = minOf(screenRect.bottom, panelRect.top)
                    }
                }
            }
            if (view is TextureView && view.isAvailable) {
                surfaceType = "TextureView"
                bitmap = view.getBitmap(VIEWFINDER_SAMPLE_SIZE, VIEWFINDER_SAMPLE_SIZE)
            } else if (view is SurfaceView && view.holder.surface.isValid) {
                surfaceType = "SurfaceView"
                isSurfaceViewReady = true
            }
        }
        if (isSurfaceViewReady && !screenRect.isEmpty) {
            // Both CameraXViewfinder and PreviewView (when an effect removes the camera transform
            // on API 29+) render into a child SurfaceControl of their SurfaceView, which
            // PixelCopy(SurfaceView) can't read, so take a screenshot of the viewfinder area.
            // Only its centre is used, so notifications or toasts drawn over its edges don't count
            // as new frames.
            val centerRect = Rect(screenRect).apply { inset(width() / 5, height() / 5) }
            bitmap = captureScreenRegion(centerRect)
        }
        val rawCopy = bitmap ?: return null
        val type = surfaceType ?: return null
        try {
            val softwareCopy =
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        rawCopy.config == Bitmap.Config.HARDWARE
                ) {
                    rawCopy.copy(Bitmap.Config.ARGB_8888, /* isMutable= */ false) ?: return null
                } else {
                    rawCopy
                }
            try {
                val pixels = IntArray(softwareCopy.width * softwareCopy.height)
                softwareCopy.getPixels(
                    pixels,
                    0,
                    softwareCopy.width,
                    0,
                    0,
                    softwareCopy.width,
                    softwareCopy.height,
                )
                return ViewfinderSample(pixels, type)
            } finally {
                if (softwareCopy !== rawCopy) {
                    softwareCopy.recycle()
                }
            }
        } finally {
            rawCopy.recycle()
        }
    }

    /**
     * Returns the visible bounds of [view] in screen coordinates, accounting for view scale,
     * translation, and parent clipping, or an empty [Rect] if [view] is not visible.
     */
    private fun visibleScreenRectOf(view: View): Rect {
        val rect = Rect()
        if (!view.getGlobalVisibleRect(rect)) {
            return Rect()
        }
        val rootLocation = IntArray(2)
        view.rootView.getLocationOnScreen(rootLocation)
        rect.offset(rootLocation[0], rootLocation[1])
        return rect
    }

    /** Returns a downscaled screenshot of [rect] in screen coordinates, or null if unavailable. */
    private fun captureScreenRegion(rect: Rect): Bitmap? {
        if (rect.isEmpty) {
            return null
        }
        val screenshot =
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                ?: return null
        var cropped: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            val region = Rect(rect)
            if (!region.intersect(0, 0, screenshot.width, screenshot.height)) {
                return null
            }
            cropped =
                Bitmap.createBitmap(
                    screenshot,
                    region.left,
                    region.top,
                    region.width(),
                    region.height(),
                )
            scaled =
                Bitmap.createScaledBitmap(
                    cropped,
                    VIEWFINDER_SAMPLE_SIZE,
                    VIEWFINDER_SAMPLE_SIZE,
                    /* filter= */ true,
                )
            return scaled
        } finally {
            if (cropped != null && cropped !== scaled && cropped !== screenshot) {
                cropped.recycle()
            }
            if (screenshot !== scaled) {
                screenshot.recycle()
            }
        }
    }

    /** Returns the largest [SurfaceView] or [TextureView] in the hierarchy of [view]. */
    private fun findLargestViewfinderSurface(view: View): View? {
        var largest: View? = if (view is SurfaceView || view is TextureView) view else null
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                val candidate = findLargestViewfinderSurface(view.getChildAt(index)) ?: continue
                if (
                    largest == null ||
                        candidate.width * candidate.height > largest.width * largest.height
                ) {
                    largest = candidate
                }
            }
        }
        return largest
    }

    private fun luma(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun meanLuma(pixels: IntArray): Int = pixels.sumOf { luma(it) } / pixels.size

    /** Returns true if all [pixels] have almost the same luma. */
    private fun isUniform(pixels: IntArray): Boolean {
        val lumas = pixels.map { luma(it) }
        return lumas.max() - lumas.min() < VIEWFINDER_UNIFORM_LUMA_RANGE
    }

    /**
     * Waits until the Low Light Boost state is INACTIVE or ACTIVE. While the Google Play Services
     * module is being installed, e.g. on a freshly set up device, it waits up to [installTimeoutMs]
     * and skips the test if the module is still pending. Later tests of the same run then skip
     * right away. It also skips the test if the module installation failed, e.g. without network
     * access or a Play account, which is an issue of the test environment.
     */
    internal fun waitForLlbEnabledState(
        isActivity: Boolean,
        timeoutMs: Long = 10000L,
        installTimeoutMs: Long = 60000L,
    ): Boolean {
        val isEnabledState = {
            val status = getStatusText(isActivity)
            status.contains("LLB: INACTIVE") || status.contains("LLB: ACTIVE")
        }
        val isPendingInstallation = {
            getStatusText(isActivity).contains("LLB: PENDING_INSTALLATION")
        }
        var sawPendingInstallation = false
        if (
            waitForCondition(timeoutMs) {
                val pending = isPendingInstallation()
                if (pending) {
                    sawPendingInstallation = true
                }
                isEnabledState() || (isModuleInstallationPending && pending)
            } && isEnabledState()
        ) {
            isModuleInstallationPending = false
            return true
        }
        assumeNoModuleInstallationFailure(isActivity)
        if (!sawPendingInstallation) {
            isModuleInstallationPending = false
            return false
        }
        assumeFalse(
            "Skipping test because the Low Light Boost module was still being installed in an " +
                "earlier test",
            isModuleInstallationPending,
        )
        waitForCondition(installTimeoutMs) { !isPendingInstallation() }
        if (isPendingInstallation()) {
            isModuleInstallationPending = true
            assumeTrue(
                "Skipping test because the Low Light Boost module is still being installed " +
                    "(${getStatusText(isActivity)})",
                false,
            )
        }
        isModuleInstallationPending = false
        val reachedEnabledState = waitForCondition(timeoutMs, isEnabledState)
        if (!reachedEnabledState) {
            assumeNoModuleInstallationFailure(isActivity)
        }
        return reachedEnabledState
    }

    internal fun assumeNoModuleInstallationFailure(isActivity: Boolean) {
        assumeNoModuleInstallationFailure(getStatusText(isActivity))
    }

    internal fun assumeNoModuleInstallationFailure(statusText: String) {
        assumeFalse(
            "Skipping test because the Low Light Boost module installation failed ($statusText)",
            statusText.contains("LLB: ERROR") && statusText.contains(MODULE_INSTALL_ERROR),
        )
    }

    internal fun wakeUpAndUnlock(uiDevice: UiDevice) {
        uiDevice.executeShellCommand("input keyevent KEYCODE_WAKEUP")
        uiDevice.executeShellCommand("wm dismiss-keyguard")
    }

    internal fun wakeUpAndWaitForResumed(
        uiDevice: UiDevice,
        isActivity: Boolean,
        timeoutMs: Long = 10000L,
    ): Boolean {
        val start = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            wakeUpAndUnlock(uiDevice)
            if (waitForActivityStage(isActivity, Stage.RESUMED, timeoutMs = 1000L)) {
                return true
            }
        }
        return false
    }

    internal fun navigateHome(uiDevice: UiDevice) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        try {
            // Launch via targetContext so the calling package matches the app UID on API 30.
            instrumentation.targetContext.startActivity(intent)
        } catch (_: Exception) {
            uiDevice.pressHome()
        }
        uiDevice.waitForIdle()
    }

    internal fun navigateHomeAndWaitStopped(
        uiDevice: UiDevice,
        isActivity: Boolean,
        timeoutMs: Long = 10000L,
    ): Boolean {
        navigateHome(uiDevice)
        if (waitForActivityStage(isActivity, Stage.STOPPED, timeoutMs = minOf(3000L, timeoutMs))) {
            return true
        }
        // Fallback to keyevent Home if background-activity restrictions ignored the Home intent.
        uiDevice.pressHome()
        uiDevice.waitForIdle()
        return waitForActivityStage(isActivity, Stage.STOPPED, timeoutMs = timeoutMs)
    }

    internal fun prepareDeviceForActivityLaunch(uiDevice: UiDevice) {
        wakeUpAndUnlock(uiDevice)
        setDeviceRotation(uiDevice, Surface.ROTATION_0)
        waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == Surface.ROTATION_0 }
        navigateHome(uiDevice)
    }

    internal fun ensureNaturalRotationAfterLaunch(uiDevice: UiDevice) {
        if (uiDevice.displayRotation != Surface.ROTATION_0) {
            setDeviceRotation(uiDevice, Surface.ROTATION_0)
            waitForCondition(timeoutMs = 5000L) { uiDevice.displayRotation == Surface.ROTATION_0 }
        }
    }

    /**
     * Verifies via transform and geometry metadata (rather than pixel buffer comparison) that the
     * active viewfinder reports an upright, properly mirrored, and isotropic sensor-to-view
     * transformation for [expectedRotation], and that the underlying [SurfaceView] (`PERFORMANCE`)
     * or [TextureView] (`COMPATIBLE`, or `PreviewView` fallback) matches the active implementation
     * mode.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApiAndroidX")
    internal fun assertPreviewOrientationAndGeometry(
        isActivity: Boolean,
        expectedRotation: Int,
        message: String,
        timeoutMs: Long = 5000L,
    ) {
        var lastFailureReason = "Condition not evaluated"
        val settled =
            waitForCondition(timeoutMs) {
                var ok = false
                InstrumentationRegistry.getInstrumentation().runOnMainSync {
                    val failure =
                        evaluateOrientationAndGeometryOnMain(
                            isActivity = isActivity,
                            expectedRotation = expectedRotation,
                        )
                    if (failure == null) {
                        ok = true
                    } else {
                        lastFailureReason = failure
                    }
                }
                ok
            }
        assertWithMessage(
                "$message: preview orientation/geometry mismatch ($lastFailureReason; " +
                    "status=${getStatusText(isActivity)})"
            )
            .that(settled)
            .isTrue()
    }

    private fun resolveExpectCompatible(statusText: String?): Boolean? =
        when {
            statusText == null -> null
            statusText.contains("Mode: COMPATIBLE") -> true
            statusText.contains("Mode: PERFORMANCE") -> false
            else -> null
        }

    @androidx.annotation.OptIn(markerClass = [TransformExperimental::class])
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("RestrictedApiAndroidX")
    private fun evaluateOrientationAndGeometryOnMain(
        isActivity: Boolean,
        expectedRotation: Int,
    ): String? {
        if (isActivity) {
            val act =
                findActiveActivity(LowLightBoostEffectActivity::class.java)
                    ?: return "Activity not active"
            val expectCompatible =
                resolveExpectCompatible(
                    act.findViewById<TextView>(R.id.status_text)?.text?.toString()
                ) ?: return "Implementation mode not yet reported in Activity status"
            val cam = act.activeCamera ?: return "Activity camera not bound"
            val composeView =
                act.findViewById<View>(R.id.compose_viewfinder)
                    ?: return "compose_viewfinder not found"
            if (!composeView.isShown || composeView.width <= 0 || composeView.height <= 0) {
                return "compose_viewfinder not laid out " +
                    "(${composeView.width}x${composeView.height})"
            }
            val req = act.activeSurfaceRequest ?: return "SurfaceRequest is null"
            val transformInfo = req.transformationInfo ?: return "TransformationInfo is null"
            val bufferToSensor = Matrix()
            if (!transformInfo.sensorToBufferTransform.invert(bufferToSensor)) {
                return "sensorToBufferTransform not invertible"
            }

            val width = composeView.width
            val height = composeView.height
            val step = maxOf(10f, minOf(width, height) * 0.1f)
            val uiCenter = Offset(width / 2f, height / 2f)
            val uiRight = Offset(width / 2f + step, height / 2f)
            val uiDown = Offset(width / 2f, height / 2f + step)
            val bufCenter = with(act.coordinateTransformer) { uiCenter.transform() }
            val bufRight = with(act.coordinateTransformer) { uiRight.transform() }
            val bufDown = with(act.coordinateTransformer) { uiDown.transform() }
            val pts =
                floatArrayOf(
                    bufCenter.x,
                    bufCenter.y,
                    bufRight.x,
                    bufRight.y,
                    bufDown.x,
                    bufDown.y,
                )
            bufferToSensor.mapPoints(pts)
            return verifyDisplaySurfaceAndVectors(
                isActivity = true,
                viewfinderRoot = composeView,
                displayRotation = composeView.display?.rotation,
                expectedRotation = expectedRotation,
                expectCompatible = expectCompatible,
                camera = cam,
                sensorPts = pts,
            )
        } else {
            val frag = findActiveFragment() ?: return "Fragment not active"
            val expectCompatible =
                resolveExpectCompatible(
                    frag.view?.findViewById<TextView>(R.id.status_text)?.text?.toString()
                ) ?: return "Implementation mode not yet reported in Fragment status"
            val cam = frag.activeCamera ?: return "Fragment camera not bound"
            val pv = frag.activePreviewView ?: return "PreviewView not available"
            if (!pv.isShown || pv.width <= 0 || pv.height <= 0) {
                return "PreviewView not laid out (${pv.width}x${pv.height})"
            }
            val sensorToView =
                pv.sensorToViewTransform ?: return "PreviewView.sensorToViewTransform is null"
            val viewToSensor = Matrix()
            if (!sensorToView.invert(viewToSensor)) {
                return "PreviewView.sensorToViewTransform not invertible"
            }

            val width = pv.width
            val height = pv.height
            val step = maxOf(10f, minOf(width, height) * 0.1f)
            val pts =
                floatArrayOf(
                    width / 2f,
                    height / 2f,
                    width / 2f + step,
                    height / 2f,
                    width / 2f,
                    height / 2f + step,
                )
            viewToSensor.mapPoints(pts)
            return verifyDisplaySurfaceAndVectors(
                isActivity = false,
                viewfinderRoot = pv,
                displayRotation = pv.display?.rotation,
                expectedRotation = expectedRotation,
                expectCompatible = expectCompatible,
                camera = cam,
                sensorPts = pts,
            )
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private fun isPreviewViewForcedTextureView(camera: Camera): Boolean {
        val isLegacyDevice =
            camera.cameraInfo.implementationType == CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY
        val hasSurfaceViewQuirk =
            DeviceQuirks.get(SurfaceViewStretchedQuirk::class.java) != null ||
                DeviceQuirks.get(SurfaceViewNotCroppedByParentQuirk::class.java) != null
        return isLegacyDevice || hasSurfaceViewQuirk
    }

    private fun verifyDisplaySurfaceAndVectors(
        isActivity: Boolean,
        viewfinderRoot: View,
        displayRotation: Int?,
        expectedRotation: Int,
        expectCompatible: Boolean,
        camera: Camera,
        sensorPts: FloatArray,
    ): String? {
        if (displayRotation != expectedRotation) {
            return "displayRotation=$displayRotation expected=$expectedRotation"
        }
        val surfaceView =
            findLargestViewfinderSurface(viewfinderRoot) ?: return "No viewfinder surface child"
        if (!surfaceView.isShown) {
            return "Viewfinder surface not shown"
        }
        val expectTextureView =
            expectCompatible || (!isActivity && isPreviewViewForcedTextureView(camera))
        if (expectTextureView) {
            if (surfaceView !is TextureView || !surfaceView.isAvailable) {
                return "Expected available TextureView " +
                    "(expectCompatible=$expectCompatible, isActivity=$isActivity), got " +
                    surfaceView.javaClass.simpleName
            }
        } else {
            if (surfaceView !is SurfaceView || !surfaceView.holder.surface.isValid) {
                return "Expected valid SurfaceView " +
                    "(expectCompatible=$expectCompatible, isActivity=$isActivity), got " +
                    surfaceView.javaClass.simpleName
            }
        }

        val vRightX = sensorPts[2] - sensorPts[0]
        val vRightY = sensorPts[3] - sensorPts[1]
        val vDownX = sensorPts[4] - sensorPts[0]
        val vDownY = sensorPts[5] - sensorPts[1]
        val lenRight = hypot(vRightX, vRightY)
        val lenDown = hypot(vDownX, vDownY)
        if (lenRight <= 1e-3f || lenDown <= 1e-3f) {
            return "Degenerate sensor step lengths: lenRight=$lenRight, lenDown=$lenDown"
        }
        val scaleRatio = lenRight / lenDown
        if (scaleRatio < 0.97f || scaleRatio > 1.03f) {
            return "Non-isotropic aspect ratio scaleRatio=$scaleRatio " +
                "(lenRight=$lenRight, lenDown=$lenDown)"
        }

        val relDegrees = camera.cameraInfo.getSensorRotationDegrees(expectedRotation)
        val isFront = camera.cameraInfo.lensFacing == CameraSelector.LENS_FACING_FRONT
        val mirrorSign = if (isFront) -1f else 1f
        val (expRightX, expRightY, expDownX, expDownY) =
            when (relDegrees) {
                0 -> floatArrayOf(mirrorSign, 0f, 0f, 1f)
                90 -> floatArrayOf(0f, -mirrorSign, 1f, 0f)
                180 -> floatArrayOf(-mirrorSign, 0f, 0f, -1f)
                270 -> floatArrayOf(0f, mirrorSign, -1f, 0f)
                else -> return "Unexpected sensor rotation degrees: $relDegrees"
            }
        val dotRight = (vRightX / lenRight) * expRightX + (vRightY / lenRight) * expRightY
        val dotDown = (vDownX / lenDown) * expDownX + (vDownY / lenDown) * expDownY
        if (dotRight <= 0.99f || dotDown <= 0.99f) {
            return "Vector mismatch at relDegrees=$relDegrees, isFront=$isFront: " +
                "dotRight=$dotRight, dotDown=$dotDown, " +
                "vRight=($vRightX,$vRightY) vs exp=($expRightX,$expRightY), " +
                "vDown=($vDownX,$vDownY) vs exp=($expDownX,$expDownY)"
        }
        return null
    }

    internal fun setDeviceRotation(uiDevice: UiDevice, rotation: Int) {
        when (rotation) {
            Surface.ROTATION_0 -> uiDevice.setOrientationNatural()
            Surface.ROTATION_90 -> uiDevice.setOrientationLeft()
            Surface.ROTATION_180 -> {
                // UiDevice does not provide a dedicated 180-degree reverse-portrait method, so
                // freeze UiAutomation rotation to ROTATION_180 directly.
                val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
                uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_180)
            }
            Surface.ROTATION_270 -> uiDevice.setOrientationRight()
        }
        val wmRotation =
            when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 1
                Surface.ROTATION_180 -> 2
                Surface.ROTATION_270 -> 3
                else -> null
            }
        if (wmRotation != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                uiDevice.executeShellCommand(
                    "cmd window user-rotation -d ${Display.DEFAULT_DISPLAY} lock $wmRotation"
                )
            } else {
                uiDevice.executeShellCommand(
                    "cmd window set-user-rotation lock -d ${Display.DEFAULT_DISPLAY} $wmRotation"
                )
            }
        }
        uiDevice.waitForIdle()
    }

    internal fun tearDownDeviceAndActivities(uiDevice: UiDevice, finishActivities: Boolean = true) {
        try {
            wakeUpAndUnlock(uiDevice)
        } catch (e: Exception) {
            // Ignore
        }
        try {
            // Reset rotation while any fullUser Activity is still in the foreground so a
            // portrait-locked Home launcher doesn't mask a non-zero rotation across tests.
            if (uiDevice.displayRotation != Surface.ROTATION_0) {
                setDeviceRotation(uiDevice, Surface.ROTATION_0)
                waitForCondition(timeoutMs = 3000L) {
                    uiDevice.displayRotation == Surface.ROTATION_0
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        if (finishActivities) {
            try {
                waitForCondition(timeoutMs = 5000L) {
                    var cleared = false
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        val preCreate =
                            ActivityLifecycleMonitorRegistry.getInstance()
                                .getActivitiesInStage(Stage.PRE_ON_CREATE)
                                .toSet()
                        val active =
                            findAllNonDestroyedActivities(LowLightBoostEffectActivity::class.java) +
                                findAllNonDestroyedActivities(MainActivity::class.java)
                        active.forEach {
                            if (it !in preCreate && !it.isFinishing) {
                                it.finish()
                            }
                        }
                        cleared = active.isEmpty()
                    }
                    cleared
                }
                uiDevice.waitForIdle()
            } catch (e: Exception) {
                // Ignore
            }
        }
        try {
            // Reset and unfreeze rotation via UiDevice and WindowManager user-rotation to leave
            // the device in auto-rotate for later test suites.
            setDeviceRotation(uiDevice, Surface.ROTATION_0)
            uiDevice.unfreezeRotation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                uiDevice.executeShellCommand(
                    "cmd window user-rotation -d ${Display.DEFAULT_DISPLAY} free"
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                uiDevice.executeShellCommand(
                    "cmd window set-user-rotation free -d ${Display.DEFAULT_DISPLAY}"
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    internal fun assumeLowLightBoostSupported(isActivity: Boolean) {
        assumeFalse(
            "Google Play Services Low Light Boost active session requires physical camera hardware",
            isEmulator(),
        )
        val supportChecked =
            waitForCondition(timeoutMs = 10000L) {
                !getStatusText(isActivity).contains("checking...")
            }
        assertWithMessage("Support check didn't complete: ${getStatusText(isActivity)}")
            .that(supportChecked)
            .isTrue()
        val status = getStatusText(isActivity)
        val isSupported =
            !status.contains("Supported: false") && !status.contains(NOT_SUPPORTED_ERROR)
        assumeTrue(
            "Skipping test because Low Light Boost is not supported on this device/camera " +
                "($status)",
            isSupported,
        )
    }

    /**
     * Asserts the Low Light Boost state of the effect created for a new configuration: INACTIVE or
     * ACTIVE if the camera supports it, and no unexpected ERROR otherwise.
     */
    internal fun assertLlbStateAfterSwitch(isActivity: Boolean, message: String) {
        assertWithMessage("$message: support check didn't complete: ${getStatusText(isActivity)}")
            .that(
                waitForCondition(timeoutMs = 10000L) {
                    !getStatusText(isActivity).contains("checking...")
                }
            )
            .isTrue()
        val status = getStatusText(isActivity)
        if (status.contains("Supported: true")) {
            assertWithMessage(
                    "$message: LLB didn't become INACTIVE/ACTIVE: ${getStatusText(isActivity)}"
                )
                .that(waitForLlbEnabledState(isActivity))
                .isTrue()
        } else {
            assertNoUnexpectedLlbError(status, message)
        }
    }

    private const val VIEWFINDER_SAMPLE_SIZE = 64
    private const val VIEWFINDER_SAMPLE_INTERVAL_MS = 200L
    private const val VIEWFINDER_MIN_CHANGES = 3
    private const val VIEWFINDER_UNIFORM_LUMA_RANGE = 2

    // Error messages of the library, shown in the status text.
    private const val NOT_SUPPORTED_ERROR = "Low Light Boost is not supported"
    private const val MODULE_INSTALL_ERROR = "Module install"
    private val PIPELINE_BUG_ERRORS =
        listOf("Unsupported resolution", "Unsupported dataspace", "OpenGL")

    private val viewfinderChangedSurfaces: MutableSet<String> =
        Collections.synchronizedSet(HashSet())

    // Set once the module was still being installed after the install timeout, so later tests of
    // the same run skip right away instead of waiting again.
    @Volatile private var isModuleInstallationPending = false

    internal fun assertNoUnexpectedLlbError(statusText: String, message: String) {
        assumeNoModuleInstallationFailure(statusText)
        if (statusText.contains("LLB: ERROR")) {
            for (pipelineError in PIPELINE_BUG_ERRORS) {
                assertWithMessage("$message hit pipeline LLB ERROR ($statusText)")
                    .that(statusText)
                    .doesNotContain(pipelineError)
            }
        }
        if (!statusText.contains(NOT_SUPPORTED_ERROR) && !statusText.contains("Supported: false")) {
            assertWithMessage("$message hit unexpected LLB ERROR ($statusText)")
                .that(statusText)
                .doesNotContain("LLB: ERROR")
        }
    }
}
