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

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class RcScopeTest {

    private val testProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            RcPlatformServices.None,
        ) { _, profile, _ ->
            RemoteComposeWriter(profile)
        }

    @Test
    fun testResourceAddition() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val text = scope.remoteText("test")
        val color = scope.remoteColor(0xFFFF0000.toInt())
        val float = scope.remoteFloat(10f)
        val int = scope.remoteInteger(5)

        // Basic verification that IDs are assigned (starts from 0 or 1 usually)
        // We don't necessarily care about the exact ID in this unit test,
        // but that the scope call delegates to writer.
        assertEquals(true, text.id >= 0)
        assertEquals(true, color.id >= 0)
        assertEquals(true, java.lang.Float.isNaN(float.toFloat()))
        assertEquals(true, int.id >= 0)
    }

    @Test
    fun testNamedResources() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val namedFloat = scope.remoteNamedFloat("var1", 1.0f)
        val namedInt = scope.remoteNamedInteger("var2", 2)

        assertEquals(true, java.lang.Float.isNaN(namedFloat.toFloat()))
        assertEquals(true, namedInt.id >= 0)
    }

    @Test
    fun testRcFloatToText() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        val f = scope.remoteFloat(12.34f)
        val text = f.format(2, 2, 0)

        assertEquals(true, text.id >= 0)
    }

    @Test
    fun testExtensionRcFloatToText() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val f = 12.34f.rf
            val text = f.format(2, 2, 0)
            assertEquals(true, text.id >= 0)
        }
    }

    @Test
    fun testPathOperations() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            Canvas {
                val path = remotePath(0f, 0f)
                path.lineTo(10f, 10f)
                path.moveTo(20f, 20f)
                path.close()

                drawPath(path.getPath())

                // Test the new lambda overload
                val path2 = remotePath(0f, 0f)
                path2.lineTo(100f, 100f)
                path2.quadTo(150f, 50f, 200f, 100f)

                drawPath(path2.getPath())
            }
        }
    }

    @Test
    fun testCanvasOperations() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            Canvas {
                save()
                clipRect(0f, 0f, 100f, 100f)
                drawRect(10f, 10f, 50f, 50f)
                restore()

                save {
                    scale(2f, 2f)
                    drawCircle(0f, 0f, 10f)
                }
            }
        }
    }

    @Test
    fun testLayouts() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            Box {
                Column {
                    Text("Hello")
                    Spacer(Modifier.height(10f))
                    Row { Text("World") }
                }
            }
        }
    }

    @Test
    fun testDrawingAndPaint() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            applyPaint {
                setColor(0xFFFF0000.toInt())
                setStrokeWidth(2f)
            }
            drawRect(0f, 0f, 100f, 100f)
            drawCircle(50f, 50f, 25f)
            drawLine(0f, 0f, 100f, 100f)

            val textId = remoteText("Hello")
            drawTextAnchored(textId, 50f, 50f, 0.5f, 0.5f)
        }
    }

    @Test
    fun testTextStyles() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val style = remoteTextStyle(fontSize = 20.rsp, color = 0xFF00FF00.toInt())
            assertEquals(true, style.id >= 0)
            Text("Styled Text", fontSize = 20.rsp)
        }
    }

    @Test
    fun testColorsAndTheming() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val color = remoteColor(0xFFFF0000.toInt())
            val themed = remoteThemedColor(0xFFFFFFFF.toInt(), 0xFF000000.toInt())
            val named = remoteNamedColor("my_color", 0xFF00FF00.toInt())

            assertEquals(true, color.id >= 0)
            assertEquals(true, themed.id >= 0)
            assertEquals(true, named.id >= 0)
        }
    }

    @Test
    fun testBitmaps() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val bitmap = remoteBitmapUrl("https://example.com/image.png")
            val namedBitmap = remoteNamedBitmapUrl("my_image", "https://example.com/image2.png")

            assertEquals(true, bitmap.id >= 0)
            assertEquals(true, namedBitmap.id >= 0)
        }
    }

    @Test
    fun testLoops() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply { Canvas { loop(0.rf, 10f, 100.rf) { i -> drawCircle(i, 50.rf, 5.rf) } } }
    }

    @Test
    fun testModifiers() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            Box(
                modifier =
                    Modifier.size(100f)
                        .padding(10f)
                        .background(0xFFFF0000.toInt())
                        .clip(RoundedRectShape(5f, 5f, 5f, 5f))
            ) {
                Text("With Modifier")
            }
        }
    }
}
