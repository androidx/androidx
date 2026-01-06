/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.sysui.macrobenchmark

import android.content.Intent
import android.util.DisplayMetrics
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.android.compose.animation.scene.demo.calculateWindowSizeClass
import com.android.compose.animation.scene.demo.shouldUseSplitScenes
import kotlin.math.roundToInt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * This file contains utilities to perform benchmark tests for the demo app of SceneTransitionLayout
 * given a [SceneTransitionLayoutBenchmarkScope].
 *
 * These abstractions are necessary to share test code between AndroidX tests written with the
 * MacrobenchmarkRule and Platform tests written with Platform helpers.
 */
interface SceneTransitionLayoutBenchmarkScope {
    /** Start an activity using [intent]. */
    fun startActivity(intent: Intent)
}

fun SceneTransitionLayoutBenchmarkScope.startDemoActivity(
    initialScene: String,
    notificationsInShade: Int? = null,
    enableOverlays: Boolean = false,
) {
    val intent =
        (context().packageManager.getLaunchIntentForPackage(StlDemoConstants.PACKAGE)
                ?: error("Unable to acquire intent for package ${StlDemoConstants.PACKAGE}"))
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(StlDemoConstants.INITIAL_SCENE_EXTRA, initialScene)
                putExtra(StlDemoConstants.FULLSCREEN_EXTRA, true)
                putExtra(StlDemoConstants.DISABLE_RIPPLE_EXTRA, true)
                if (enableOverlays) {
                    putExtra(StlDemoConstants.OVERLAYS_EXTRA, true)
                }

                notificationsInShade?.let { putExtra(StlDemoConstants.NOTIFICATIONS_IN_SHADE, it) }
            }

    val device = device()
    device.pressHome()
    startActivity(intent)
    device.waitForObject(sceneSelector(initialScene))
}

fun SceneTransitionLayoutBenchmarkScope.setupSwipeFromScene(
    fromScene: String,
    toScene: String,
    toContentIsOverlay: Boolean,
) {
    startDemoActivity(initialScene = fromScene, enableOverlays = toContentIsOverlay)

    // Wait for the root SceneTransitionLayout to be there. Note that startDemoActivity already
    // waited for fromScene, so we know it's there.
    val device = device()
    device.waitForObject(StlDemoConstants.ROOT_STL_SELECTOR_IDLE)

    // Verify that toScene is not there yet.
    device.waitUntilGone(sceneSelector(toScene))
}

fun swipeFromScene(
    fromScene: String,
    toContent: String,
    direction: Direction,
    toContentIsOverlay: Boolean,
    swipeOn: BySelector? = null,
) {
    // Swipe in the given direction.
    val densityDpi = context().resources.configuration.densityDpi
    val density = densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT
    val swipeSpeed = 1_500 // in dp/s
    val device = device()

    val swipeOn = swipeOn ?: StlDemoConstants.ROOT_STL_SELECTOR_IDLE
    device.waitForObject(swipeOn)
    device
        .findObject(swipeOn)
        .swipe(direction, /* percent= */ 0.9f, /* speed= */ (swipeSpeed * density).roundToInt())

    if (!toContentIsOverlay) {
        // Wait for fromScene to disappear.
        device.waitUntilGone(sceneSelector(fromScene))
    }

    // Check that we are at toContent.
    device.waitForObject(contentSelector(toContent, toContentIsOverlay))

    // Wait for the layout to be idle.
    device.waitForObject(StlDemoConstants.ROOT_STL_SELECTOR_IDLE)
}

/**
 * Navigate back to [previousScene] assuming that we are currently on [currentContent] and that
 * going back will land us at [previousScene].
 */
fun navigateBackToPreviousScene(
    previousScene: String,
    currentContent: String,
    currentContentIsOverlay: Boolean,
) {
    val device = device()
    val currentContentSelector = contentSelector(currentContent, currentContentIsOverlay)
    device.waitUntilGone(sceneSelector(previousScene))
    device.waitForObject(currentContentSelector)

    device.pressBack()
    device.waitUntilGone(currentContentSelector)
    device.waitForObject(sceneSelector(previousScene))
    device.waitForObject(StlDemoConstants.ROOT_STL_SELECTOR_IDLE)
}

private fun instrumentation() = InstrumentationRegistry.getInstrumentation()

private fun context() = instrumentation().targetContext

private fun device() = UiDevice.getInstance(instrumentation())

private fun UiDevice.waitForObject(selector: BySelector, timeout: Long = 5_000) {
    if (!wait(Until.hasObject(selector), timeout)) {
        error("Did not find $selector within $timeout ms")
    }
}

private fun UiDevice.waitUntilGone(selector: BySelector, timeout: Long = 5_000) {
    if (!wait(Until.gone(selector), timeout)) {
        error("$selector is still there after waiting $timeout ms")
    }
}

private fun sceneSelector(scene: String) = By.res("scene:$scene")

private fun overlaySelector(overlay: String) = By.res("overlay:$overlay")

private fun contentSelector(toContent: String, toContentIsOverlay: Boolean): BySelector {
    return if (toContentIsOverlay) overlaySelector(toContent) else sceneSelector(toContent)
}

object StlDemoConstants {
    const val PACKAGE = "androidx.compose.integration.hero.sysui.macrobenchmark.target"
    val LOCKSCREEN_SCENE by AdaptiveScene("Lockscreen", "SplitLockscreen")
    val SHADE_SCENE by AdaptiveScene("Shade", "SplitShade")
    const val QUICK_SETTINGS_SCENE = "QuickSettings"
    const val NOTIFICATIONS_OVERLAY = "NotificationsOverlay"
    const val QUICK_SETTINGS_OVERLAY = "QuickSettingsOverlay"

    internal const val INITIAL_SCENE_EXTRA = "initial_scene"
    internal const val FULLSCREEN_EXTRA = "fullscreen"
    internal const val DISABLE_RIPPLE_EXTRA = "disable_ripple"
    internal const val NOTIFICATIONS_IN_SHADE = "notifications_in_shade"
    internal const val OVERLAYS_EXTRA = "overlays"
    internal val ROOT_STL_SELECTOR_IDLE = By.res("SystemUiSceneTransitionLayout:idle")
    val STL_START_HALF_SELECTOR = By.res("StlStartHalf")
    val STL_END_HALF_SELECTOR = By.res("StlEndHalf")
}

/** A scene whose key depends on whether we are using split scenes or not. */
private class AdaptiveScene(private val normalScene: String, private val splitScene: String) :
    ReadOnlyProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        val context = context()
        val density = Density(context)
        return if (shouldUseSplitScenes(calculateWindowSizeClass(context, density))) {
            splitScene
        } else {
            normalScene
        }
    }
}

internal fun MacrobenchmarkScope.sysuiHeroBenchmarkScope(): SceneTransitionLayoutBenchmarkScope {
    return object : SceneTransitionLayoutBenchmarkScope {
        override fun startActivity(intent: Intent) = startActivityAndWait(intent)
    }
}
