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

package androidx.compose.integration.hero.pokedex.macrobenchmark

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2

/**
 * Wait for the [condition] to become true for [timeoutMillis] ms or throw an exception and dump the
 * window hierarchy if the condition is not met.
 *
 * @param condition The search condition to wait for
 * @param timeoutMillis The maximum time in milliseconds to wait for the condition to become true
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped before throwing
 *   the exception if the object is not found.
 * @param lazyMessage The message to attach to the exception if the condition is not met
 */
internal fun UiDevice.waitOrThrow(
    condition: SearchCondition<Boolean>,
    timeoutMillis: Long,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = {
        "Waited for $condition, was not fulfilled after $timeoutMillis ms."
    },
) {
    val waitResult = wait(condition, timeoutMillis)
    if (waitResult != true && dumpWindowHierarchyOnFailure) {
        dumpWindowHierarchy(System.out)
    }
    require(waitResult == true, lazyMessage)
}

/**
 * Find an object by a [selector] and return it, or throw if it can not be found.
 *
 * @param selector The selector to use to find the object
 * @param dumpWindowHierarchyOnFailure Whether the window hierarchy should be dumped before throwing
 *   the exception if the object is not found.
 * @param lazyMessage The message to attach to the exception if the object is not found.
 * @return The found object
 */
internal fun UiDevice.findObjectOrThrow(
    selector: BySelector,
    dumpWindowHierarchyOnFailure: Boolean = true,
    lazyMessage: () -> String = { "Did not find $selector." },
): UiObject2 {
    val findObjectResult = findObject(selector)
    if (findObjectResult == null && dumpWindowHierarchyOnFailure) {
        dumpWindowHierarchy(System.out)
    }
    requireNotNull(findObjectResult, lazyMessage)
    return findObjectResult
}
