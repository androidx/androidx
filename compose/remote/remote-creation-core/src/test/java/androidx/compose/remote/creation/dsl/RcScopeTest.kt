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

@file:OptIn(ExperimentalRemoteCreationApi::class)

package androidx.compose.remote.creation.dsl

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.ExperimentalRemoteCreationApi
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.profile.Profile
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        val text = f.format(2, 2)
        val padded =
            f.format(2, 2, RcTextFromFloatSpec.of(padPre = RcTextFromFloatSpec.PadPre.Zero))
        val scoped =
            scope.createTextFromFloat(
                f,
                2,
                2,
                RcTextFromFloatSpec.of(padAfter = RcTextFromFloatSpec.PadAfter.Zero),
            )

        assertEquals(true, text.id >= 0)
        assertEquals(true, padded.id >= 0)
        assertEquals(true, scoped.id >= 0)
    }

    @Test
    fun testExtensionRcFloatToText() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val f = 12.34f.rf
            val text = f.format(2, 2)
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

                drawPath(path.toPath())

                // Test the new lambda overload
                val path2 = remotePath(0f, 0f)
                path2.lineTo(100f, 100f)
                path2.quadTo(150f, 50f, 200f, 100f)
                path2.cubicTo(220f, 120f, 240f, 80f, 250f, 100f)
                path2.arcTo(0f, 0f, 100f, 100f, 0f, 90f, false)
                path2.arcTo(25f, 25f, 0f, false, true, 50f, 50f)

                drawPath(path2.toPath())
            }
        }
    }

    @Test
    fun testRcPathDataAndRemotePathData() {
        val path = RcPathData()
        assertTrue(path.isEmpty)
        path.moveTo(0f, 0f)
        path.arcTo(0f, 0f, 100f, 100f, 0f, 90f, false)
        assertFalse(path.isEmpty)
        assertTrue(path.base.createFloatArray().isNotEmpty())

        val pathAvd =
            parseAvdPathData(
                "M 0 0 h 20 v 10 l -5 5 c 1 2 3 4 5 6 s 1 2 3 4 q 1 2 3 4 t 2 2 a 25 25 0 0 1 50 50 z"
            )
        assertTrue(pathAvd.base.createFloatArray().isNotEmpty())

        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)
        scope.apply {
            Canvas {
                val p1 = remotePathData(path)
                val p2 = remotePathData("M 0 0 L 10 10 Z")
                val p3 = remotePathData {
                    moveTo(0f, 0f)
                    lineTo(25f, 25f)
                    close()
                }
                drawPath(p1)
                drawPath(p2)
                drawPath(p3)
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
        val scope = RcCanvasScopeImpl(writer)

        scope.apply {
            paint {
                color(0xFFFF0000.toInt())
                strokeWidth(2f)
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
        val bytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                val style =
                    remoteTextStyle(
                        color = 0xFF00FF00.rcColor(),
                        fontSize = 20.rsp,
                        fontStyle = RcFontStyle.Italic,
                        fontWeight = RcFontWeight.Bold,
                        underline = true,
                    )
                assertTrue(style.id >= 0)
                Text(
                    "Styled Text",
                    fontSize = 20.rsp,
                    fontStyle = RcFontStyle.Italic,
                    underline = true,
                    strikethrough = true,
                    style = style,
                )
            }
        CoreDocument()
            .initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(bytes)
                )
            )
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testSoundPathFillDataMapAndPathEffects() {
        val dash1 = RcPathEffect.dash(floatArrayOf(10f, 5f), phase = 2f)
        val dash2 = RcPathEffect.dash(floatArrayOf(10f, 5f), phase = 2f)
        assertEquals(dash1, dash2)
        assertEquals(dash1.hashCode(), dash2.hashCode())

        val soundBytes =
            createRcBuffer(RcProfile.core(isExperimental = true)) {
                val sound = remoteSound(byteArrayOf(1, 2, 3, 4))
                playSound(sound)
            }
        CoreDocument()
            .initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(soundBytes)
                )
            )
        assertTrue(soundBytes.isNotEmpty())

        val bytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                val dataMap = remoteDataMap {
                    string("label", "Hello")
                    float("value", 3.5f)
                    int("count", 7)
                    long("timestamp", 123456L)
                    boolean("enabled", true)
                }
                val label = dataMap.lookupText("label")
                val value = dataMap.lookupFloat("value")
                val count = dataMap.lookupInt("count")
                val enabled = dataMap.lookupBoolean("enabled")
                assertTrue(label.id >= 0)
                assertTrue(java.lang.Float.isNaN(value.toFloat()))
                assertTrue(count.id != 0L)
                assertEquals(enabled, dataMap.lookupBoolean("enabled"))

                val star =
                    remotePathData(fillType = RcPathFillType.EvenOdd) {
                        moveTo(0f, 0f)
                        lineTo(50f, 50f)
                        lineTo(0f, 50f)
                        close()
                    }
                val chained =
                    RcPathEffect.chain(
                        dash1,
                        RcPathEffect.discrete(segmentLength = 4f, deviation = 1.5f),
                    )
                val stamped =
                    RcPathEffect.pathDash(
                        shape = star,
                        advance = 12f,
                        phase = 0f,
                        style = RcStampedPathStyle.Rotate,
                    )
                val combined = RcPathEffect.sum(chained, stamped)

                Canvas(Modifier.fillMaxSize()) {
                    paint { pathEffect(combined) }
                    drawPath(star)
                    paint { clearPathEffect() }
                }
            }
        CoreDocument()
            .initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(bytes)
                )
            )
        assertTrue(bytes.isNotEmpty())
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
            val rawBitmap = remoteBitmap(4, 4, byteArrayOf(1, 2, 3))
            val namedRawBitmap = remoteNamedBitmap("raw_image", 4, 4, byteArrayOf(1, 2, 3))
            val offscreen = createOffscreenBitmap()
            val font = remoteFont(byteArrayOf(0, 1, 0, 0))
            val flag = remoteBoolean(true)

            assertEquals(true, bitmap.id >= 0)
            assertEquals(true, namedBitmap.id >= 0)
            assertEquals(true, rawBitmap.id >= 0)
            assertEquals(true, namedRawBitmap.id >= 0)
            assertEquals(true, offscreen.id >= 0)
            assertEquals(true, font.id >= 0)
            assertEquals(true, flag.handle >= 0L)
        }
    }

    @Test
    fun testLoops() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            Canvas {
                loop(0.rf, 10.rf, 100.rf) { i -> drawCircle(i, 50.rf, 5.rf) }
                loop(0.rf, 10f, 100.rf) { i -> drawCircle(i, 60.rf, 5.rf) }
            }
        }
    }

    @Test
    fun testModifiers() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val visAnim = defineVisibilityAnimation { _, _, _, _, _ -> }
            Box(
                modifier =
                    Modifier.size(100f)
                        .padding(10f)
                        .background(0xFFFF0000.toInt())
                        .clip(RcShape.roundedCorner(5f))
                        .animationSpec(
                            enterAnimation = RcEnterExitAnimation.FadeIn,
                            exitAnimation = RcEnterExitAnimation.SlideBottom,
                            enterSequence = RcAnimationSequence.After,
                            enterAnimationFunction = visAnim,
                        )
                        .semantics {
                            role(RcRole.Button)
                            mode(RcSemanticsMode.Merge)
                        }
            ) {
                Text("With Modifier", modifier = Modifier.clip(RcShape.Rectangle))
            }
        }
    }

    @Test
    fun testHeaderTagsGradientsAndCircleText() {
        val supportedOps =
            requireNotNull(
                    androidx.compose.remote.core.Operations.getOperations(
                        CoreDocument.DOCUMENT_API_LEVEL,
                        RcProfiles.PROFILE_ANDROIDX,
                    )
                )
                .keySet() + setOf(androidx.compose.remote.core.Operations.DRAW_TEXT_ON_CIRCLE)
        val circleProfile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX,
                RcPlatformServices.None,
                { supportedOps },
            ) { _, profile, _ ->
                RemoteComposeWriter(profile)
            }
        val bytes =
            createRcBuffer(
                RcProfile(circleProfile),
                RcHeaderTag.docWidth(200),
                RcHeaderTag.docHeight(150),
                RcHeaderTag.desiredFps(60),
                RcHeaderTag.contentDescription("Header test"),
                RcHeaderTag.densityBehavior(RcDensityBehavior.Dp),
                RcHeaderTag.debug(0),
            ) {
                val c1 = remoteColor(0xFFFF0000.toInt())
                val c2 = remoteColor(0xFF0000FF.toInt())
                val glyphImg = createBitmap(8, 8)
                val font =
                    createBitmapFont(
                        listOf(bitmapFontGlyph("A", glyphImg, 0, 0, 0, 0, 8, 8)),
                        kerningTable = mapOf("AV" to -2),
                    )
                assertTrue(font.id >= 0)
                val sampleText = remoteText("A")
                val bitmapWidth = sampleText.measure(font, RcBitmapTextMeasure.Width)
                val textWidth =
                    textMeasure(
                        sampleText,
                        RcTextMeasureMode.Width or RcTextMeasureMode.MonospaceWidth,
                    )
                assertTrue(java.lang.Float.isNaN(bitmapWidth.toFloat()))
                assertTrue(java.lang.Float.isNaN(textWidth.toFloat()))

                Canvas {
                    paint {
                        linearGradient(0f, 0f, 100f, 100f, listOf(c1, RcColor.None, c2))
                        radialGradient(50f, 50f, 40f, listOf(c1, c2))
                        sweepGradient(50f, 50f, listOf(c1, c2))
                    }
                    drawTextOnCircle(
                        text = remoteText("Arc"),
                        centerX = 100f,
                        centerY = 75f,
                        radius = 50f,
                        startAngle = 0f,
                        warpRadiusOffset = 0f,
                        alignment = RcCircleTextAlignment.Center,
                        placement = RcCircleTextPlacement.Outside,
                    )
                }
            }
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testDataAndStateManagement() {
        val writer = RemoteComposeWriter(testProfile)
        val scope = RcScopeImpl(writer)

        scope.apply {
            val intArray = remoteIntArray(intArrayOf(1, 2, 3))
            val dynamicArray = remoteDynamicFloatArray(10f)
            val namedFloatArray = remoteNamedFloatArray("arr", floatArrayOf(1.0f, 2.0f))
            val floatList = remoteFloatList(floatArrayOf(1.0f, 2.0f))
            val floatMap = remoteFloatMap(listOf("key1", "key2"), floatArrayOf(1.0f, 2.0f))

            assertEquals(true, java.lang.Float.isNaN(intArray.toFloat()))
            assertEquals(true, java.lang.Float.isNaN(dynamicArray.toFloat()))
            assertEquals(true, java.lang.Float.isNaN(namedFloatArray.toFloat()))
            assertEquals(true, java.lang.Float.isNaN(floatList.toFloat()))
            assertEquals(true, java.lang.Float.isNaN(floatMap.toFloat()))

            setArrayValue(dynamicArray, 0.rf, 5.rf)
        }
    }

    @Test
    fun testDefineMacroAndInflateWithValueAndBlockArgs() {
        val bytes =
            createRcBuffer(RcProfile(testProfile, isExperimental = true)) {
                Box {
                    val cardMacro =
                        defineMacro("card", listOf("title", "tint", "slot")) { args ->
                            Column(modifier = Modifier.background(args["tint"]!!.asColor())) {
                                Text(args["title"]!!.asText())
                                args["slot"]!!.insertArgument()
                            }
                        }

                    val titleText = remoteText("Card Title")
                    val tintColor = remoteColor(0xFF112233.toInt())
                    cardMacro.inflate {
                        text("title", titleText)
                        color("tint", tintColor)
                        block("slot") {
                            Text("Slot Body")
                            CustomComponent(
                                "custom_widget",
                                properties = listOf(RcCustomProperty.int(1, 5)),
                            )
                        }
                    }
                }
            }

        val doc = CoreDocument()
        val buffer =
            androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                java.io.ByteArrayInputStream(bytes)
            )
        doc.initFromBuffer(buffer)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testBeginEndGlobalMatchesScopedGlobal() {
        val directBytes =
            createRcBuffer(RcProfile(testProfile)) {
                Box {
                    beginGlobal()
                    val firstColor = remoteNamedColor("color.first", 0xFFFF0000.toInt())
                    val secondColor = remoteNamedColor("color.second", 0xFF00FF00.toInt())
                    endGlobal()

                    Text("First", color = firstColor)
                    Text("Second", color = secondColor)
                }
            }

        val scopedBytes =
            createRcBuffer(RcProfile(testProfile)) {
                Box {
                    val (firstColor, secondColor) =
                        global {
                            val c1 = remoteNamedColor("color.first", 0xFFFF0000.toInt())
                            val c2 = remoteNamedColor("color.second", 0xFF00FF00.toInt())
                            c1 to c2
                        }

                    Text("First", color = firstColor)
                    Text("Second", color = secondColor)
                }
            }

        assertArrayEquals(scopedBytes, directBytes)
    }

    @Test
    fun testRcProfileFlagsAndCorePresets() {
        val combined = RcProfileFlags.AndroidX or RcProfileFlags.Widgets
        assertTrue(RcProfileFlags.AndroidX in combined)
        assertTrue(RcProfileFlags.Widgets in combined)
        assertFalse(RcProfileFlags.Widgets in RcProfileFlags.AndroidX)

        assertEquals(CoreDocument.DOCUMENT_API_LEVEL, RcProfile.CoreBaseline.docApiLevel)
        assertEquals(RcProfileFlags.Baseline, RcProfile.CoreBaseline.profileFlags)
        assertFalse(RcProfile.CoreBaseline.isExperimental)

        assertEquals(CoreDocument.DOCUMENT_API_LEVEL, RcProfile.CoreAndroidX.docApiLevel)
        assertEquals(RcProfileFlags.AndroidX, RcProfile.CoreAndroidX.profileFlags)
        assertFalse(RcProfile.CoreAndroidX.isExperimental)

        val baselineBytes = createRcBuffer(RcProfile.CoreBaseline) { Box { Text("Baseline") } }
        assertTrue(baselineBytes.isNotEmpty())

        val customCore =
            RcProfile.core(
                docApiLevel = 7,
                profileFlags = combined,
                supportedOperations = intArrayOf(42, 99),
                isExperimental = true,
            )
        assertEquals(7, customCore.docApiLevel)
        assertEquals(combined, customCore.profileFlags)
        assertTrue(customCore.isExperimental)
        assertEquals(setOf(42, 99), customCore.profile.supportedOperations)
        assertEquals(RcProfile.CoreAndroidX, RcProfile.core())
    }

    @Test
    fun testValueSemanticsAndImmutability() {
        assertEquals(0f rcAt 0f, RcPoint.Zero)
        assertEquals((10f rcAt 20f).hashCode(), (10f rcAt 20f).hashCode())
        assertEquals(RcRect.ltrb(1f, 2f, 3f, 4f), RcRect.xywh(1f, 2f, 2f, 2f))
        assertEquals(RcShape.roundedCorner(8.rdp), RcShape.roundedCorner(8.rdp))
        assertEquals(RcHeaderTag.docWidth(320), RcHeaderTag.docWidth(320))
        assertEquals(
            Modifier.padding(8.rdp).fillMaxWidth(),
            Modifier.padding(8f).fillMaxWidth(1f),
        )
        assertEquals("RcPositioning.Center", RcPositioning.Center.toString())

        val bytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                val base = remoteFloat(10f)
                val beforeHash = base.hashCode()
                val animated = base.anim(remoteFloat(0.25f))
                assertEquals(beforeHash, base.hashCode())
                assertFalse(base === animated)

                val t0 = remoteLong(1000L)
                val t1 = remoteLong(2000L)
                val elapsed = timeAttribute(t1, RcTimeAttr.TimeFromArgSec, t0)

                val icon = remoteBitmap(1, 1, byteArrayOf(0))
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX(animated)
                            alpha(elapsed)
                        }
                ) {
                    Image(icon, contentDescription = "Sample Icon")
                }
            }
        CoreDocument()
            .initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(bytes)
                )
            )
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testMathParamOrderScopedParticlesAndDynamicGradients() {
        // Verify clamp(value, min, max) emits RPN [min, max, value, CLAMP]
        val clamped = clamp(value = RcFloat(5f), min = RcFloat(0f), max = RcFloat(10f))
        assertEquals(0f, clamped.toArray()[0], 0f)
        assertEquals(10f, clamped.toArray()[1], 0f)
        assertEquals(5f, clamped.toArray()[2], 0f)

        // Verify cubic(x1, y1, x2, y2, value) emits RPN [x1, y1, x2, y2, value, CUBIC]
        val eased = cubic(RcFloat(0.1f), RcFloat(0.2f), RcFloat(0.3f), RcFloat(0.4f), RcFloat(0.5f))
        assertEquals(0.1f, eased.toArray()[0], 0f)
        assertEquals(0.2f, eased.toArray()[1], 0f)
        assertEquals(0.3f, eased.toArray()[2], 0f)
        assertEquals(0.4f, eased.toArray()[3], 0f)
        assertEquals(0.5f, eased.toArray()[4], 0f)

        // Verify 0-arg RcShape.roundedCorner() is unambiguous & RectShape ignores unused coords
        assertEquals(RcShape.roundedCorner(0.rdp), RcShape.roundedCorner())
        val customRect =
            RcShape(androidx.compose.remote.creation.modifiers.RectShape(1f, 2f, 3f, 4f))
        assertEquals(RcShape.Rectangle, customRect)
        assertEquals(RcShape.Rectangle.hashCode(), customRect.hashCode())

        // Verify Modifier.clip(RcShape.Circle) emits a rounded clip modifier (both px and dp)
        for (density in listOf(RcDensityBehavior.Pixels, RcDensityBehavior.Dp)) {
            val circleClipBytes =
                createRcBuffer(RcProfile.CoreAndroidX, RcHeaderTag.densityBehavior(density)) {
                    Box(modifier = Modifier.size(100f, 40f).clip(RcShape.Circle)) {}
                }
            val circleDoc = CoreDocument()
            circleDoc.initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(circleClipBytes)
                )
            )
            assertTrue(circleDoc.displayHierarchy().contains("ROUNDED_CLIP_RECT"))
        }

        val bytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                val a = remoteFloatList(floatArrayOf(1f, 2f, 3f))
                val b = remoteFloatList(floatArrayOf(4f, 5f, 6f))
                val dot = arraySumXy(a, b)
                val l10 = log10(dot + 1f)

                val c = remoteColor(0xFF336699.toInt())
                val hue = colorAttribute(c, RcColorAttr.Hue)
                val style = remoteTextStyle(color = 0xFF112233.toInt(), fontSize = 18.rsp)

                Text("Colored", c, fontSize = 16.rsp, style = style)
                Text("Static", 0xFF445566.rcColor(), Modifier.padding(4.rdp))

                val curve =
                    remoteXyPath(
                        remoteFunction { t -> t * 100f },
                        remoteFunction { t -> sin(t) * 50f },
                        0f,
                        1f,
                        16,
                    )

                Canvas(Modifier.fillMaxSize()) {
                    val touch = addTouch(0.5f, 0f..1f, touchPosX())
                    val vars = FloatArray(2)
                    val ps = createParticles(vars, listOf(0f.rf, 0f.rf), 4)
                    val (px, py) = rcFloats(vars)
                    particlesComparison(
                        id = ps,
                        min = (-1f).rf,
                        max = (-1f).rf,
                        condition = {
                            10f.rf - hypot(first(px) - second(px), first(py) - second(py))
                        },
                        then1 = { listOf(second(px), first(py)) },
                        then2 = { listOf(first(px), second(py)) },
                    ) {
                        drawCircle(px, py, 4f.rf)
                    }
                    paint {
                        linearGradient(
                            startX = 0f.rf,
                            startY = 0f.rf,
                            endX = touch,
                            endY = l10,
                            colors = intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()),
                        )
                    }
                    drawPath(curve)
                    paint {
                        radialGradient(
                            startX = hue,
                            startY = touch,
                            startRadius = 0f.rf,
                            endX = 50f.rf,
                            endY = 50f.rf,
                            endRadius = 40f.rf,
                            colors = listOf(c, RcColor.None),
                        )
                    }
                    drawCircle(50f.rf, 50f.rf, 40f.rf)
                }
            }
        CoreDocument()
            .initFromBuffer(
                androidx.compose.remote.core.RemoteComposeBuffer.fromInputStream(
                    java.io.ByteArrayInputStream(bytes)
                )
            )
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testRcColorValueOverloadsMatchColorIntWireOutput() {
        val redInt = 0xFFFF0000.toInt()
        val greenInt = 0xFF00FF00.toInt()
        val blueInt = 0xFF0000FF.toInt()
        val tintInt = 0x88112233.toInt()

        val intBytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                remoteColor(redInt)
                remoteNamedColor("accent", greenInt)
                remoteThemedColor(redInt, blueInt)
                remoteThemedColor("light", redInt, "dark", blueInt)
                Canvas(Modifier.fillMaxSize()) {
                    paint {
                        colorFilter(tintInt, RcBlendMode.SrcIn)
                        linearGradient(0f, 0f, 100f, 100f, intArrayOf(redInt, blueInt))
                        linearGradient(0f.rf, 0f.rf, 100f.rf, 100f.rf, intArrayOf(redInt, blueInt))
                        radialGradient(50f, 50f, 40f, intArrayOf(redInt, blueInt))
                        radialGradient(50f.rf, 50f.rf, 40f.rf, intArrayOf(redInt, blueInt))
                        radialGradient(40f, 40f, 5f, 50f, 50f, 40f, intArrayOf(redInt, blueInt))
                        radialGradient(
                            40f.rf,
                            40f.rf,
                            5f.rf,
                            50f.rf,
                            50f.rf,
                            40f.rf,
                            intArrayOf(redInt, blueInt),
                        )
                        sweepGradient(50f, 50f, intArrayOf(redInt, greenInt, blueInt))
                        sweepGradient(50f.rf, 50f.rf, intArrayOf(redInt, greenInt, blueInt))
                    }
                    drawRect(0f, 0f, 100f, 100f)
                }
            }

        val red = RcColorValue(0xFFFF0000)
        val green = RcColorValue(0xFF00FF00)
        val blue = RcColorValue(0xFF0000FF)
        val tint = RcColorValue(0x88112233)

        val valueBytes =
            createRcBuffer(RcProfile.CoreAndroidX) {
                remoteColor(red)
                remoteNamedColor("accent", green)
                remoteThemedColor(red, blue)
                remoteThemedColor("light", red, "dark", blue)
                Canvas(Modifier.fillMaxSize()) {
                    paint {
                        colorFilter(tint, RcBlendMode.SrcIn)
                        linearGradient(0f, 0f, 100f, 100f, listOf(red, blue))
                        linearGradient(0f.rf, 0f.rf, 100f.rf, 100f.rf, listOf(red, blue))
                        radialGradient(50f, 50f, 40f, listOf(red, blue))
                        radialGradient(50f.rf, 50f.rf, 40f.rf, listOf(red, blue))
                        radialGradient(40f, 40f, 5f, 50f, 50f, 40f, listOf(red, blue))
                        radialGradient(
                            40f.rf,
                            40f.rf,
                            5f.rf,
                            50f.rf,
                            50f.rf,
                            40f.rf,
                            listOf(red, blue),
                        )
                        sweepGradient(50f, 50f, listOf(red, green, blue))
                        sweepGradient(50f.rf, 50f.rf, listOf(red, green, blue))
                    }
                    drawRect(0f, 0f, 100f, 100f)
                }
            }

        org.junit.Assert.assertArrayEquals(intBytes, valueBytes)
    }

    @Test
    fun testLongColorOverloadsMatchColorIntWireOutput() {
        val experimentalProfile = RcProfile.core(isExperimental = true)
        val intBytes =
            createRcBuffer(experimentalProfile) {
                remoteColor(0xFFFF0000.toInt())
                remoteNamedColor("accent", 0xFF00FF00.toInt())
                remoteThemedColor(0xFFFF0000.toInt(), 0xFF0000FF.toInt())
                remoteThemedColor("light", 0xFFFF0000.toInt(), "dark", 0xFF0000FF.toInt())
                val style = remoteTextStyle(color = 0xFF112233.toInt(), fontSize = 18.rsp)
                val label = remoteText("hi")
                Box(
                    modifier =
                        Modifier.background(0xFFEEEEEE.toInt())
                            .border(2.rdp, 4.rdp, RcColorValue(0xFF222222))
                ) {
                    Text("Hello", color = 0xFFFFFFFF.toInt(), style = style)
                    Text(label, color = 0xFFCCCCCC.toInt())
                    EditableText(label, color = 0xFFAAAAAA.toInt())
                }
                Canvas(Modifier.fillMaxSize()) {
                    paint {
                        color(0xFF0000AA.toInt())
                        colorFilter(0x88112233.toInt(), RcBlendMode.SrcIn)
                    }
                    drawRect(0f, 0f, 100f, 100f)
                }
            }

        val longBytes =
            createRcBuffer(experimentalProfile) {
                remoteColor(0xFFFF0000)
                remoteNamedColor("accent", 0xFF00FF00)
                remoteThemedColor(0xFFFF0000, 0xFF0000FF)
                remoteThemedColor("light", 0xFFFF0000, "dark", 0xFF0000FF)
                val style = remoteTextStyle(color = 0xFF112233, fontSize = 18.rsp)
                val label = remoteText("hi")
                Box(modifier = Modifier.background(0xFFEEEEEE).border(2.rdp, 4.rdp, 0xFF222222)) {
                    Text("Hello", color = 0xFFFFFFFF, style = style)
                    Text(label, color = 0xFFCCCCCC)
                    EditableText(label, color = 0xFFAAAAAA)
                }
                Canvas(Modifier.fillMaxSize()) {
                    paint {
                        color(0xFF0000AA)
                        colorFilter(0x88112233, RcBlendMode.SrcIn)
                    }
                    drawRect(0f, 0f, 100f, 100f)
                }
            }

        org.junit.Assert.assertArrayEquals(intBytes, longBytes)
    }
}
