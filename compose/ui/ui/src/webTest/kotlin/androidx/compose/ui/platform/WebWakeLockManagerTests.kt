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

package androidx.compose.ui.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.keepScreenOn
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlin.js.unsafeCast
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine

class WebWakeLockManagerTests : OnCanvasTests {

    @Test
    fun testWakeLockRequestAndRelease() = runApplicationTest {
        var keepScreenOn by mutableStateOf(false)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box(
                modifier = Modifier.fillMaxSize().then(
                    if (keepScreenOn) Modifier.keepScreenOn()
                    else Modifier
                )
            ) {
                Text(if (keepScreenOn) "Release Wake Lock" else "Request Wake Lock")
            }
        }
        // Initially no wake lock
        assertFalse(manager.isWakeLockActive(), "Wake lock should not be active initially")
        assertFalse(manager.requestingLock, "Should not be requesting lock initially")

        // Request wake lock by setting keepScreenOn to true
        keepScreenOn = true
        awaitIdle()

        assertTrue(manager.isWakeLockActive(), "Wake lock should be active after requesting")

        // Release wake lock by setting keepScreenOn to false
        keepScreenOn = false
        awaitIdle()

        assertFalse(
            manager.isWakeLockActive(),
            "Wake lock should be released after setting to false"
        )
        assertFalse(manager.requestingLock, "Should not be requesting lock after release")
        manager.reset()
    }

    @Test
    fun testWakeLockRemainsWhenOneOfTwoRemoved() = runApplicationTest {
        var attachFirst by mutableStateOf(true)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box {
                if (attachFirst) {
                    Box(Modifier.keepScreenOn())
                }
                Box(Modifier.keepScreenOn())
            }
        }

        awaitIdle()
        assertTrue(manager.isWakeLockActive(), "Wake lock should be active with two modifiers")

        // Remove one modifier
        attachFirst = false
        awaitIdle()

        assertTrue(
            manager.isWakeLockActive(),
            "Wake lock should remain active when one of two modifiers removed"
        )
        manager.reset()
    }

    @Test
    fun testWakeLockReleasedWhenAllTwoRemoved() = runApplicationTest {
        var attach by mutableStateOf(true)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box {
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
        }

        awaitIdle()
        assertTrue(manager.isWakeLockActive(), "Wake lock should be active with two modifiers")

        // Remove both modifiers
        attach = false
        awaitIdle()

        assertFalse(
            manager.isWakeLockActive(),
            "Wake lock should be released when all modifiers removed"
        )
        assertFalse(manager.requestingLock, "Should not be requesting lock after all removed")
        manager.reset()
    }

    @Test
    fun testWakeLockWithThreeModifiersRemoveTwo() = runApplicationTest {
        var attachFirst by mutableStateOf(true)
        var attachSecond by mutableStateOf(true)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box {
                if (attachFirst) {
                    Box(Modifier.keepScreenOn())
                }
                if (attachSecond) {
                    Box(Modifier.keepScreenOn())
                }
                Box(Modifier.keepScreenOn())
            }
        }

        awaitIdle()
        assertTrue(manager.isWakeLockActive(), "Wake lock should be active with three modifiers")

        // Remove two modifiers
        attachFirst = false
        attachSecond = false
        awaitIdle()

        assertTrue(
            manager.isWakeLockActive(),
            "Wake lock should remain active with one modifier"
        )
        manager.reset()
    }

    @Test
    fun testWakeLockWithThreeModifiersRemoveAll() = runApplicationTest {
        var attach by mutableStateOf(true)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box {
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
        }

        awaitIdle()
        assertTrue(manager.isWakeLockActive(), "Wake lock should be active with three modifiers")

        // Remove all modifiers
        attach = false
        awaitIdle()

        assertFalse(
            manager.isWakeLockActive(),
            "Wake lock should be released when all three modifiers removed"
        )
        assertFalse(manager.requestingLock, "Should not be requesting lock after all removed")
        manager.reset()
    }


    //TODO: https://youtrack.jetbrains.com/issue/CMP-10088/Web-Unignore-testWakeLockRequestBlurUnBlurRelease-by-considering-Wake-Lock-Promise-timing-as-arbitrary
    @Test
    @Ignore
    fun testWakeLockRequestBlur_UnBlurRelease() = runApplicationTest {
        var keepScreenOn by mutableStateOf(false)
        val manager = WebWakeLockManager

        createComposeWindow {
            Box(
                modifier = Modifier.fillMaxSize().then(
                    if (keepScreenOn) Modifier.keepScreenOn()
                    else Modifier
                )
            ) {
                Text(if (keepScreenOn) "Release Wake Lock" else "Request Wake Lock")
            }
        }
        val canvas = getCanvas()
        canvas.focus()
        // Initially no wake lock
        assertFalse(manager.isWakeLockActive(), "Wake lock should not be active initially")
        assertFalse(manager.requestingLock, "Should not be requesting lock initially")

        // Request wake lock by setting keepScreenOn to true
        keepScreenOn = true
        awaitIdle()

        assertTrue(manager.isWakeLockActive(), "Wake lock should be active after requesting")

        val anotherWindow = window.open("https://www.google.com/")
        assertTrue(anotherWindow != null)
        awaitIdle()
        assertFalse(
            manager.isWakeLockActive(),
            "Wake lock should be released when window is blurred"
        )

        anotherWindow.close()


        //Workaround to avoid request lock Promise timeout due to new tabs and their arbitrary timing of Promises
        var waitingIterations = 100
        do {
            awaitAnimationFrame()
        } while (!manager.isWakeLockActive() && waitingIterations-- > 0)

        assertTrue(
            manager.isWakeLockActive(),
            "Wake lock should be active again when window is focused"
        )

        // Release wake lock by setting keepScreenOn to false
        keepScreenOn = false
        awaitIdle()

        assertFalse(
            manager.isWakeLockActive(),
            "Wake lock should be released after setting to false"
        )
        assertFalse(manager.requestingLock, "Should not be requesting lock after release")
        manager.reset()
    }
}