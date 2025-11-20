/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.test.uiautomator.internal

import android.content.Context.DISPLAY_SERVICE
import android.graphics.Bitmap
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Environment
import android.os.SystemClock
import android.view.KeyCharacterMap
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

internal val instrumentation = InstrumentationRegistry.getInstrumentation()
internal val uiAutomation = instrumentation.uiAutomation
internal val uiDevice = UiDevice.getInstance(instrumentation)
internal val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
internal val displayManager =
    instrumentation.context.getSystemService(DISPLAY_SERVICE) as DisplayManager

internal fun nowMs() = SystemClock.uptimeMillis()

/**
 * Asserts that the receiver is not null, otherwise throws the given [t] exception.
 *
 * @param t the exception to throw if this receiver is null.
 */
internal fun <T> T?.notNull(t: Throwable): T = this ?: throw t

/**
 * Deprecation is suppressed because this is the folder Android Studio uses to read instrumentation
 * test results.
 */
@Suppress("DEPRECATION")
internal val instrumentationPackageMediaDir =
    InstrumentationRegistry.getInstrumentation().targetContext.externalMediaDirs.firstOrNull {
        Environment.getExternalStorageState(it) == Environment.MEDIA_MOUNTED
    } ?: throw IllegalStateException("Cannot get external storage because it's not mounted")

internal fun joinLines(vararg lines: String) =
    lines.joinToString(separator = System.lineSeparator())

internal fun takeScreenshotBitmap(nodeBounds: Rect): Bitmap {
    val screenBitmap = uiAutomation.takeScreenshot()
    val screenBounds =
        Rect().apply {
            left = 0
            top = 0
            right = screenBitmap.width
            bottom = screenBitmap.height
        }
    nodeBounds.intersect(screenBounds)

    val x = nodeBounds.left.coerceIn(0, screenBounds.right)
    val y = nodeBounds.top.coerceIn(0, screenBounds.top)
    val w = nodeBounds.width().coerceIn(0, screenBounds.width() - x)
    val h = nodeBounds.height().coerceIn(0, screenBounds.height() - y)
    val cropped = Bitmap.createBitmap(screenBitmap, x, y, w, h)
    return cropped
}

internal fun takeViewNodeTree(root: AccessibilityNodeInfo, displayRect: Rect) =
    ElementNode.fromAccessibilityNodeInfo(depth = 0, node = root, displayRect = displayRect)
