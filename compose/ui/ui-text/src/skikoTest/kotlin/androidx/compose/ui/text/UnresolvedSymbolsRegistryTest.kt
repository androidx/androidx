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

package androidx.compose.ui.text

import androidx.compose.runtime.InternalComposeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(InternalComposeApi::class)
class UnresolvedSymbolsRegistryTest {

    @Test
    fun addListener_notifiedOnNewCodepoints() {
        val registry = UnresolvedSymbolsRegistry()
        val received = mutableListOf<Set<Int>>()
        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) {
                received += codepoints.toSet()
            }
        }
        registry.addListener(listener)
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(1, received.size)
        assertTrue(0x4E2D in received[0])
    }

    @Test
    fun addUnresolvedCodepoints_deduplicates_acrossCalls() {
        val registry = UnresolvedSymbolsRegistry()
        var callCount = 0
        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) { callCount++ }
        }
        registry.addListener(listener)
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(1, callCount, "Duplicate codepoints must not trigger listener again")
    }

    @Test
    fun addUnresolvedCodepoints_accumulatesAcrossCalls() {
        val registry = UnresolvedSymbolsRegistry()
        val received = mutableListOf<Set<Int>>()
        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) {
                received += codepoints.toSet()
            }
        }
        registry.addListener(listener)
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        registry.addUnresolvedCodepoints(intArrayOf(0x6C34))
        assertEquals(2, received.size)
        assertEquals(setOf(0x4E2D, 0x6C34), received[1])
    }

    @Test
    fun removeListener_stopsNotification() {
        val registry = UnresolvedSymbolsRegistry()
        var callCount = 0
        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) { callCount++ }
        }
        registry.addListener(listener)
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(1, callCount)

        registry.removeListener(listener)
        registry.addUnresolvedCodepoints(intArrayOf(0x6C34))
        assertEquals(1, callCount, "Removed listener must not be notified")
    }

    @Test
    fun onNewFontInstalled_clearsCachedCodepoints_andNotifiesListeners() {
        val registry = UnresolvedSymbolsRegistry()
        val received = mutableListOf<Set<Int>>()
        var fontInstalledCount = 0
        val listener = object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) {
                received += codepoints.toSet()
            }
            override fun onNewFontInstalled() { fontInstalledCount++ }
        }
        registry.addListener(listener)

        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(1, received.size)

        registry.onNewFontInstalled()
        assertEquals(1, fontInstalledCount)

        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(2, received.size, "Codepoints must be re-accepted after onNewFontInstalled")
    }

    @Test
    fun onNewFontInstalled_notifiesAllListeners() {
        val registry = UnresolvedSymbolsRegistry()
        var count1 = 0
        var count2 = 0
        registry.addListener(object : UnresolvedSymbolsRegistry.Listener {
            override fun onNewFontInstalled() { count1++ }
        })
        registry.addListener(object : UnresolvedSymbolsRegistry.Listener {
            override fun onNewFontInstalled() { count2++ }
        })
        registry.onNewFontInstalled()
        assertEquals(1, count1)
        assertEquals(1, count2)
    }

    @Test
    fun removeNonAddedListener_doesNotCrash() {
        val registry = UnresolvedSymbolsRegistry()
        val listener = object : UnresolvedSymbolsRegistry.Listener {}
        registry.removeListener(listener)
    }

    @Test
    fun multipleListeners_allNotified() {
        val registry = UnresolvedSymbolsRegistry()
        var count1 = 0
        var count2 = 0
        registry.addListener(object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) { count1++ }
        })
        registry.addListener(object : UnresolvedSymbolsRegistry.Listener {
            override fun onUnresolvedCodepoints(codepoints: Set<Int>) { count2++ }
        })
        registry.addUnresolvedCodepoints(intArrayOf(0x4E2D))
        assertEquals(1, count1)
        assertEquals(1, count2)
    }
}
