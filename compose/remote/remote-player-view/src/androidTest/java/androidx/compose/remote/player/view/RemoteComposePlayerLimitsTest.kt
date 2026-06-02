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
package androidx.compose.remote.player.view

import androidx.compose.remote.core.Limits
import androidx.compose.remote.testing.LimitsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteComposePlayerLimitsTest {

    /**
     * Rule to automatically save and restore static [Limits] values. Even though it appears unused
     * in the test methods, it is active for every test and ensures that modifications to static
     * [Limits] fields (made via the player) are cleaned up after each test.
     */
    @get:Rule val limitsRule = LimitsRule()

    @Test
    fun testSetMaxOpCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val player = RemoteComposePlayer(context)
        player.setMaxOpCount(5000)
        assertEquals(5000, Limits.MAX_OP_COUNT)
    }

    @Test
    fun testSetMaxImageDimension() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val player = RemoteComposePlayer(context)
        player.setMaxImageDimension(1000)
        assertEquals(1000, Limits.MAX_IMAGE_DIMENSION)
    }

    @Test
    fun testSetMaxBitmapMemory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val player = RemoteComposePlayer(context)
        player.setMaxBitmapMemory(10 * 1024 * 1024)
        assertEquals(10 * 1024 * 1024, Limits.MAX_BITMAP_MEMORY)
    }

    @Test
    fun testSetMaxFps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val player = RemoteComposePlayer(context)
        player.setMaxFps(144)
        assertEquals(144, Limits.MAX_FPS)
    }

    @Test
    fun testSetDefaultMaxFps() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val player = RemoteComposePlayer(context)
        player.setDefaultMaxFps(30)
        assertEquals(30, Limits.DEFAULT_MAX_FPS)
    }
}
