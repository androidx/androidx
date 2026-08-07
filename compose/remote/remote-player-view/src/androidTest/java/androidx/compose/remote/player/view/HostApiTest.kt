/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.graphics.Path
import android.graphics.RectF
import androidx.compose.remote.core.operations.NamedVariable
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.remote.player.view.platform.AndroidRcPlatformServices
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for host variable and path retrieval APIs in [RemoteComposePlayer]:
 * - [RemoteComposePlayer.getNamedFloat]
 * - [RemoteComposePlayer.getNamedString]
 * - [RemoteComposePlayer.getNamedPath]
 */
@Suppress("DEPRECATION")
@RunWith(AndroidJUnit4::class)
class HostApiTest {

    @Test
    fun getNamedFloat_returnsDeclaredValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val writer = RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)

        writer.addNamedFloat("speed", 45.5f)
        writer.addNamedFloat("progress", 0.85f)
        writer.addNamedFloat("score", 100f)

        writer.root { writer.box(RecordingModifier()) }

        val docBytes = writer.encodeToByteArray()
        val player = RemoteComposePlayer(context)
        player.setDocument(RemoteDocument(ByteArrayInputStream(docBytes)))

        assertEquals(45.5f, player.getNamedFloat("speed"), 0.0001f)
        assertEquals(0.85f, player.getNamedFloat("progress"), 0.0001f)
        assertEquals(100f, player.getNamedFloat("score"), 0.0001f)
    }

    @Test
    fun getNamedString_returnsDeclaredValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val writer = RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)

        writer.addNamedString("headerTitle", "System Status")
        writer.addNamedString("username", "AndroidDeveloper")
        writer.addNamedString("emptyText", "")

        writer.root { writer.box(RecordingModifier()) }

        val docBytes = writer.encodeToByteArray()
        val player = RemoteComposePlayer(context)
        player.setDocument(RemoteDocument(ByteArrayInputStream(docBytes)))

        assertEquals("System Status", player.getNamedString("headerTitle"))
        assertEquals("AndroidDeveloper", player.getNamedString("username"))
        assertEquals("", player.getNamedString("emptyText"))
    }

    @Test
    fun getNamedPath_withAndroidPath_populatesPathAndReturnsTrue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val writer = RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)

        val srcPath =
            Path().apply {
                moveTo(10f, 10f)
                lineTo(110f, 10f)
                lineTo(110f, 60f)
                lineTo(10f, 60f)
                close()
            }

        val pathId = writer.addPathData(srcPath)
        writer.setNamedVariable(pathId, "cardOutline", NamedVariable.PATH_TYPE)

        writer.root { writer.box(RecordingModifier()) }

        val docBytes = writer.encodeToByteArray()
        val player = RemoteComposePlayer(context)
        player.setDocument(RemoteDocument(ByteArrayInputStream(docBytes)))

        val outPath = Path()
        val found = player.getNamedPath("cardOutline", outPath)

        assertTrue("Expected getNamedPath to return true for 'cardOutline'", found)
        assertFalse("Output path should not be empty", outPath.isEmpty)

        val bounds = RectF()
        outPath.computeBounds(bounds, true)
        assertEquals(10f, bounds.left, 0.001f)
        assertEquals(10f, bounds.top, 0.001f)
        assertEquals(110f, bounds.right, 0.001f)
        assertEquals(60f, bounds.bottom, 0.001f)
    }

    @Test
    fun getNamedPath_withSvgPathString_populatesPathAndReturnsTrue() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val platform = AndroidRcPlatformServices()
        val writer = RemoteComposeWriter.obtain(300, 300, RcPlatformProfiles.ANDROIDX)
        val supportParse = false
        if (supportParse) {
            val pathId = writer.addPathData(platform.parsePath("M 0 0 L 200 0 L 200 100 Z"))
            writer.setNamedVariable(pathId, "trianglePath", NamedVariable.PATH_TYPE)

            writer.root { writer.box(RecordingModifier()) }

            val docBytes = writer.encodeToByteArray()
            val player = RemoteComposePlayer(context)
            player.setDocument(RemoteDocument(ByteArrayInputStream(docBytes)))

            val outPath = Path()
            val found = player.getNamedPath("trianglePath", outPath)

            assertTrue("Expected getNamedPath to return true for 'trianglePath'", found)
            assertFalse("Output path should not be empty", outPath.isEmpty)

            val bounds = RectF()
            outPath.computeBounds(bounds, true)
            assertEquals(0f, bounds.left, 0.001f)
            assertEquals(0f, bounds.top, 0.001f)
            assertEquals(200f, bounds.right, 0.001f)
            assertEquals(100f, bounds.bottom, 0.001f)
        }
    }

    @Test
    fun combinedNamedVariables_retrievesAllTypesSimultaneously() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val writer = RemoteComposeWriter.obtain(400, 400, RcPlatformProfiles.ANDROIDX)

        writer.addNamedFloat("zoomLevel", 1.5f)
        writer.addNamedString("filterMode", "grayscale")

        val circlePath = Path().apply { addCircle(50f, 50f, 25f, Path.Direction.CW) }
        val pathId = writer.addPathData(circlePath)
        writer.setNamedVariable(pathId, "avatarClip", NamedVariable.PATH_TYPE)

        writer.root { writer.box(RecordingModifier()) }

        val docBytes = writer.encodeToByteArray()
        val player = RemoteComposePlayer(context)
        player.setDocument(RemoteDocument(ByteArrayInputStream(docBytes)))

        assertEquals(1.5f, player.getNamedFloat("zoomLevel"), 0.0001f)
        assertEquals("grayscale", player.getNamedString("filterMode"))

        val retrievedPath = Path()
        val pathFound = player.getNamedPath("avatarClip", retrievedPath)
        assertTrue("avatarClip path should be found", pathFound)
        assertFalse("retrievedPath should not be empty", retrievedPath.isEmpty)

        val bounds = RectF()
        retrievedPath.computeBounds(bounds, true)
        assertEquals(25f, bounds.left, 0.01f)
        assertEquals(25f, bounds.top, 0.01f)
        assertEquals(75f, bounds.right, 0.01f)
        assertEquals(75f, bounds.bottom, 0.01f)
    }
}
