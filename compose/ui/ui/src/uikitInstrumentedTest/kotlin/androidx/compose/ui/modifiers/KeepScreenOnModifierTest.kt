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

package androidx.compose.ui.modifiers

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.test.UIKitInstrumentedTest
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.window.Dialog
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.UIKit.UIApplication

internal class KeepScreenOnModifierTest {

    val isKeepScreenOnEnabled: Boolean get() = UIApplication.sharedApplication.idleTimerDisabled

    @Test
    fun testFlagOnWhenModifierAdded() = runUIKitInstrumentedTest {
        cleanupMemory()

        setContent {
            Box(Modifier.keepScreenOn())
        }

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOnWhenModifierAddedAfterInitialComposition() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(false)

        setContent {
            if (attach) {
                Box(Modifier.keepScreenOn())
            }
        }

        assertFalse(isKeepScreenOnEnabled)

        attach = true

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOffWhenModifierRemoved() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            if (attach) {
                Box(Modifier.keepScreenOn())
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOffWhenModifierRemovedWithoutRecreatingElement() = runUIKitInstrumentedTest {
        cleanupMemory()

        var enabled by mutableStateOf(true)

        setContent {
            Box(
                modifier = if (enabled) {
                    Modifier.keepScreenOn()
                } else {
                    Modifier
                }
            )
        }

        assertTrue(isKeepScreenOnEnabled)

        enabled = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOnWhenModifierReattachedAfterRemoval() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            if (attach) {
                Box(Modifier.keepScreenOn())
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)

        attach = true

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOffWhenParentRemovedAndModifierInChild() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            Box {
                if (attach) {
                    Box {
                        Box(Modifier.keepScreenOn())
                    }
                }
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOnWhenParentRemovedAndModifierInSibling() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            Box {
                if (attach) {
                    Box {
                        Box(Modifier.keepScreenOn())
                    }
                }
                Box(Modifier.keepScreenOn())
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOnWhenModifierRemovedInChild() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            Box {
                Box(Modifier.keepScreenOn())
                Box {
                    if (attach) {
                        Box(Modifier.keepScreenOn())
                    }
                }
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOnWhenModifierRemovedInSibling() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            Box {
                Box(Modifier.keepScreenOn())
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOffWhenAllModifiersRemoved() = runUIKitInstrumentedTest {
        cleanupMemory()

        var attach by mutableStateOf(true)

        setContent {
            Box {
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
                if (attach) {
                    Box(Modifier.keepScreenOn())
                }
            }
        }

        assertTrue(isKeepScreenOnEnabled)

        attach = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }

    @Test
    fun testFlagOffWhenComposeSceneDisposed() = runUIKitInstrumentedTest {
        cleanupMemory()

        setContent {
            Box(Modifier.keepScreenOn())
        }

        assertTrue(isKeepScreenOnEnabled)

        stopComposeScene()

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }

    @Test
    fun testKeepScreenOnInDialog() = runUIKitInstrumentedTest {
        cleanupMemory()

        var showDialog by mutableStateOf(false)

        setContent {
            Box {
                if (showDialog) {
                    Dialog(onDismissRequest = {}) {
                        Box(Modifier.keepScreenOn())
                    }
                }
            }
        }

        assertFalse(isKeepScreenOnEnabled)

        showDialog = true

        waitForIdle()

        assertTrue(isKeepScreenOnEnabled)

        showDialog = false

        waitForIdle()

        assertFalse(isKeepScreenOnEnabled)
    }
}

@OptIn(NativeRuntimeApi::class)
private fun UIKitInstrumentedTest.cleanupMemory() {
    repeat(6) {
        delay(100)
        GC.collect()
    }
}
