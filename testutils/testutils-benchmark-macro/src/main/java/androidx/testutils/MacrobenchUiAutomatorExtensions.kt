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

package androidx.testutils

import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.By.res
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern

/**
 * Waits for [condition] to be fulfilled within [timeoutMillis], or throws [IllegalStateException]
 * and dumps the accessibility window hierarchy to the logs. Inspecting this can help you verify if
 * your benchmark target was in the expected state.
 *
 * @param condition The search condition to wait for.
 * @param timeoutMillis The maximum time in milliseconds to wait for [condition] to be fulfilled.
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped to the logs
 *   before throwing if [condition] is not fulfilled.
 * @param lazyMessage Supplies the exception message if [condition] is not fulfilled.
 */
fun UiDevice.waitOrThrow(
    condition: SearchCondition<Boolean>,
    timeoutMillis: Long = DEFAULT_UIAUTOMATOR_TIMEOUT_MS,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = {
        "Condition $condition was not satisfied within ${timeoutMillis}ms."
    },
) {
    val result = wait(condition, timeoutMillis)
    if (result != true) {
        if (dumpWindowHierarchyOnFailure) {
            dumpHierarchyToLogs()
        }
        throw IllegalStateException(lazyMessage())
    }
}

/**
 * Finds an object by [selector], waiting up to [timeoutMillis] for it to appear.
 *
 * Throws [IllegalStateException] and dumps the accessibility window hierarchy to the logs if the
 * object is not found. Inspecting this can help you verify if your benchmark target was in the
 * expected state.
 *
 * @param selector The [BySelector] used to locate the target UI element.
 * @param timeoutMillis The maximum time in milliseconds to wait for the object to appear.
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped to the logs
 *   before throwing if the object is not found.
 * @param lazyMessage Supplies the exception message if the object is not found.
 * @return The found [UiObject2] (non-null).
 */
fun UiDevice.findObjectOrThrow(
    selector: BySelector,
    timeoutMillis: Long = DEFAULT_UIAUTOMATOR_TIMEOUT_MS,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = {
        "UiObject matching selector '$selector' was not found within ${timeoutMillis}ms."
    },
): UiObject2 {
    val searchCondition = Until.findObject(selector)
    val result = wait(searchCondition, timeoutMillis)
    if (result == null) {
        if (dumpWindowHierarchyOnFailure) {
            dumpHierarchyToLogs()
        }
        throw IllegalStateException(lazyMessage())
    }
    return result
}

/**
 * Waits until an object matching [selector] is gone, or throws [IllegalStateException] and dumps
 * the accessibility window hierarchy to the logs. Inspecting this can help you verify if your
 * benchmark target was in the expected state.
 *
 * @param selector The [BySelector] for the UI element expected to disappear.
 * @param timeoutMillis The maximum time in milliseconds to wait for the object to disappear.
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped to the logs
 *   before throwing if the object remains visible.
 * @param lazyMessage Supplies the exception message if the object remains visible.
 */
fun UiDevice.waitUntilGoneOrThrow(
    selector: BySelector,
    timeoutMillis: Long = DEFAULT_UIAUTOMATOR_TIMEOUT_MS,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = {
        "UiObject matching selector '$selector' remained visible after ${timeoutMillis}ms."
    },
) {
    waitOrThrow(
        condition = Until.gone(selector),
        timeoutMillis = timeoutMillis,
        dumpWindowHierarchyOnFailure = dumpWindowHierarchyOnFailure,
        lazyMessage = lazyMessage,
    )
}

/**
 * Strict idleness check for Compose recomposition.
 *
 * Waits until the [COMPOSE_IDLE_DESC] (`COMPOSE-IDLE`) semantics node appears in the accessibility
 * hierarchy. Throws [IllegalStateException] and dumps the accessibility window hierarchy to the
 * logs if Compose does not reach idle state within [timeoutMillis]. Inspecting this can help you
 * verify if your benchmark target was in the expected state.
 *
 * Note: Relying on [waitForComposeIdleOrThrow] is discouraged because an idle Compose hierarchy
 * does not guarantee the expected UI change happened. Instead, verify the expected UI state
 * directly:
 * ```kotlin
 * val list = device.findObjectOrThrow(By.res(LIST_CONTENT_TAG))
 * list.setGestureMargin(device.displayWidth / 5)
 * list.drag(Point(list.visibleCenter.x, list.visibleTop))
 * device.findObjectOrThrow(By.text("Item 20"))
 * ```
 *
 * @param timeoutMillis The maximum time in milliseconds to wait for Compose to reach idle state.
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped to the logs
 *   before throwing if Compose does not reach idle state.
 * @param lazyMessage Supplies the exception message if Compose does not reach idle state.
 */
fun UiDevice.waitForComposeIdleOrThrow(
    timeoutMillis: Long = DEFAULT_UIAUTOMATOR_TIMEOUT_MS,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = {
        "Compose did not reach idle state ($COMPOSE_IDLE_DESC) within ${timeoutMillis}ms."
    },
) {
    waitOrThrow(
        condition = Until.hasObject(By.desc(COMPOSE_IDLE_DESC)),
        timeoutMillis = timeoutMillis,
        dumpWindowHierarchyOnFailure = dumpWindowHierarchyOnFailure,
        lazyMessage = lazyMessage,
    )
}

/**
 * Matches [resourceId] (case-sensitive) as a substring of the resource name.
 *
 * This is useful when matching an element that can be present in either a View hierarchy or a
 * Compose hierarchy, where the resource name might or might not contain the package name.
 *
 * @param resourceId The case-sensitive resource ID substring to match.
 * @return A [BySelector] that matches the resource name as a substring.
 */
fun byResContains(resourceId: String): BySelector =
    res(Pattern.compile(String.format("^.*%s.*$", Pattern.quote(resourceId)), Pattern.DOTALL))

/** Safely dumps the current window hierarchy to system logs for debugging. */
private fun UiDevice.dumpHierarchyToLogs() {
    try {
        val output = ByteArrayOutputStream()
        dumpWindowHierarchy(output)
        val hierarchyDump = output.toString("UTF-8")
        Log.e(TAG, "Window hierarchy dump on benchmark assertion failure:")
        for (chunk in hierarchyDump.chunked(MAX_LOGCAT_CHUNK_LENGTH)) {
            Log.e(TAG, chunk)
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Failed to dump window hierarchy", t)
    }
}

private const val TAG = "MacrobenchUiAutomator"
private const val MAX_LOGCAT_CHUNK_LENGTH = 3_500

/** Default timeout in milliseconds for UIAutomator wait and lookup helpers. */
const val DEFAULT_UIAUTOMATOR_TIMEOUT_MS = 3_000L

/** Accessibility content description emitted by Compose macrobenchmark targets when idle. */
const val COMPOSE_IDLE_DESC = "COMPOSE-IDLE"
