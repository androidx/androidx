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

package androidx.compose.remote.creation

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcColumnVerticalPositioning
import androidx.compose.remote.creation.dsl.RcHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcRowHorizontalPositioning
import androidx.compose.remote.creation.dsl.RcText
import androidx.compose.remote.creation.dsl.RcVerticalPositioning
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.dsl.rsp
import androidx.compose.remote.creation.dsl.size
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DslComparisonTest {

    private val testProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            0,
            RcPlatformServices.None,
            { info, profile, _ ->
                RemoteComposeWriter(
                    profile,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, info.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, info.height),
                    RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
                )
            },
        )

    @Test
    fun testSimpleDocumentComparison() {
        val width = 400
        val height = 600
        val contentDesc = "test"

        // 1. Create document using legacy API
        val legacyContext = RemoteComposeContext(width, height, "", testProfile)
        legacyContext.mRemoteWriter.root {
            legacyContext.column(
                RecordingModifier().background(0xFFEEEEEE.toInt()).padding(20f),
                2 /* CENTER */,
                4, /* TOP */
            ) {
                val textId = mRemoteWriter.addText("Hello from New DSL!")
                mRemoteWriter.textComponent(
                    RecordingModifier(),
                    textId,
                    0xFF333333.toInt(),
                    24f,
                    0,
                    400f,
                    null,
                    1,
                    1,
                    1,
                ) {}
            }
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. Create document using new DSL
        val dslBytes =
            createRcBuffer(
                RcProfile(testProfile),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                Column(
                    modifier = Modifier.background(0xFFEEEEEE.toInt()).padding(20f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Top,
                ) {
                    Text("Hello from New DSL!", fontSize = 24.rsp, color = 0xFF333333.toInt())
                }
            }

        // 3. Compare
        assertThat(dslBytes).isEqualTo(legacyBytes)
    }

    @Test
    fun testComplexDocumentComparison() {
        val width = 500
        val height = 800

        // 1. Create document using legacy API
        val legacyContext = RemoteComposeContext(width, height, "", testProfile)
        legacyContext.mRemoteWriter.root {
            val countId = legacyContext.mRemoteWriter.addNamedFloat("count", 0f)
            val textVarId =
                RcText(legacyContext.mRemoteWriter.addNamedString("message", "Click me!"))

            legacyContext.column(
                RecordingModifier().background(0xFF111111.toInt()).width(500f).height(800f),
                2 /* CENTER */,
                4, /* TOP */
            ) {
                // Header
                val titleId = mRemoteWriter.addText("Complex Layout")
                mRemoteWriter.textComponent(
                    RecordingModifier().padding(10f),
                    titleId,
                    0xFFFFFFFF.toInt(),
                    32f,
                    0,
                    700f,
                    null,
                    3 /* CENTER */,
                    1,
                    1,
                ) {}

                // Nested Row
                legacyContext.row(
                    RecordingModifier().padding(20f).background(0xFF333333.toInt()),
                    6 /* SPACE_BETWEEN */,
                    2, /* CENTER */
                ) {
                    val label1Id = mRemoteWriter.addText("Left")
                    mRemoteWriter.textComponent(
                        RecordingModifier(),
                        label1Id,
                        0xFF00FF00.toInt(),
                        18f,
                        0,
                        400f,
                        null,
                        1,
                        1,
                        1,
                    ) {}

                    val label2Id = mRemoteWriter.addText("Right")
                    mRemoteWriter.textComponent(
                        RecordingModifier(),
                        label2Id,
                        0xFF0000FF.toInt(),
                        18f,
                        0,
                        400f,
                        null,
                        1,
                        1,
                        1,
                    ) {}
                }

                // Nested Box
                legacyContext.box(
                    RecordingModifier()
                        .width(200f)
                        .height(200f)
                        .background(0xFFFF0000.toInt())
                        .onClick(
                            ValueFloatChange(Utils.idFromNan(countId), 1f),
                            ValueStringChange(textVarId.id, "Button Clicked!"),
                        ),
                    2 /* CENTER */,
                    2, /* CENTER */
                ) {
                    val boxTextId = mRemoteWriter.addText("Inside Box")
                    mRemoteWriter.textComponent(
                        RecordingModifier(),
                        boxTextId,
                        0xFFFFFFFF.toInt(),
                        14f,
                        0,
                        400f,
                        null,
                        3 /* CENTER */,
                        1,
                        1,
                    ) {}
                }
            }
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. Create document using new DSL
        val dslBytes =
            createRcBuffer(
                RcProfile(testProfile),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                val count = addNamedFloat("count", 0f)
                val textVar = addNamedText("message", "Click me!")

                Column(
                    modifier = Modifier.background(0xFF111111.toInt()).size(500f, 800f),
                    horizontal = RcHorizontalPositioning.Center,
                    vertical = RcColumnVerticalPositioning.Top,
                ) {
                    Text(
                        "Complex Layout",
                        modifier = Modifier.padding(10f),
                        fontSize = 32.rsp,
                        color = 0xFFFFFFFF.toInt(),
                        fontWeight = 700f,
                        textAlign = 3, // CENTER
                    )

                    Row(
                        modifier = Modifier.padding(20f).background(0xFF333333.toInt()),
                        horizontal = RcRowHorizontalPositioning.SpaceBetween,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text("Left", color = 0xFF00FF00.toInt(), fontSize = 18.rsp)
                        Text("Right", color = 0xFF0000FF.toInt(), fontSize = 18.rsp)
                    }

                    Box(
                        modifier =
                            Modifier.size(200f, 200f).background(0xFFFF0000.toInt()).onClick {
                                setValue(count, 1f)
                                setValue(textVar, "Button Clicked!")
                            },
                        horizontal = RcHorizontalPositioning.Center,
                        vertical = RcVerticalPositioning.Center,
                    ) {
                        Text(
                            "Inside Box",
                            fontSize = 14.rsp,
                            color = 0xFFFFFFFF.toInt(),
                            textAlign = 3,
                        )
                    }
                }
            }

        // 3. Compare
        assertThat(dslBytes).isEqualTo(legacyBytes)
    }

    @Test
    fun testCanvasDocumentComparison() {
        val width = 400
        val height = 400

        // 1. Create document using legacy API
        val legacyContext = RemoteComposeContext(width, height, "", testProfile)
        legacyContext.mRemoteWriter.root {
            legacyContext.canvas(
                RecordingModifier().fillMaxSize(),
                object : androidx.compose.remote.creation.RemoteComposeWriterInterface {
                    override fun run() {
                        val w = legacyContext.mRemoteWriter.addComponentWidthValue()
                        val h = legacyContext.mRemoteWriter.addComponentHeightValue()
                        legacyContext.mRemoteWriter.drawRect(0f, 0f, w, h)
                    }
                },
            )
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. Create document using new DSL
        val dslBytes =
            createRcBuffer(
                RcProfile(testProfile),
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, height),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(0.rf, 0.rf, this.width, this.height)
                }
            }

        // 3. Compare
        assertThat(dslBytes).isEqualTo(legacyBytes)
    }

    @Test
    fun testAdditionalDrawingComparison() {
        val profile = RcProfile(testProfile)

        // 1. Legacy
        val legacyContext = RemoteComposeContext(100, 100, "", testProfile)
        legacyContext.mRemoteWriter.root {
            legacyContext.mRemoteWriter.drawLine(0f, 0f, 100f, 100f)
            legacyContext.mRemoteWriter.drawOval(10f, 10f, 90f, 90f)
            legacyContext.mRemoteWriter.drawRoundRect(20f, 20f, 80f, 80f, 5f, 5f)
            legacyContext.mRemoteWriter.drawArc(0f, 0f, 100f, 100f, 0f, 90f)
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. DSL
        val dslBytes =
            createRcBuffer(
                profile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 100),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 100),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                drawLine(0f, 0f, 100f, 100f)
                drawOval(10f, 10f, 90f, 90f)
                drawRoundRect(20f, 20f, 80f, 80f, 5f, 5f)
                drawArc(0f, 0f, 100f, 100f, 0f, 90f)
            }

        assertThat(dslBytes).isEqualTo(legacyBytes)
    }

    @Test
    fun testLongColorComparison() {
        val profile = RcProfile(testProfile)
        val colorInt = 0xFFEEEEEE.toInt()
        val colorLong = 0xFFEEEEEE

        val dslBytesInt =
            createRcBuffer(profile) { Box(modifier = Modifier.background(colorInt)) {} }

        val dslBytesLong =
            createRcBuffer(profile) { Box(modifier = Modifier.background(colorLong)) {} }

        assertThat(dslBytesLong).isEqualTo(dslBytesInt)
    }

    @Test
    fun testLinearGradientComparison() {
        val profile = RcProfile(testProfile)
        val colorsArray = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val positions = floatArrayOf(0f, 1f)

        // 1. Legacy
        val legacyContext = RemoteComposeContext(100, 100, "", testProfile)
        legacyContext.mRemoteWriter.root {
            legacyContext.mRemoteWriter.rcPaint
                .setLinearGradient(0f, 0f, 100f, 100f, colorsArray, 0, positions, 0)
                .commit()
            legacyContext.mRemoteWriter.drawRect(0f, 0f, 100f, 100f)
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. DSL
        val dslBytes =
            createRcBuffer(
                profile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 100),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 100),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                applyPaint { setLinearGradient(0f, 0f, 100f, 100f, colorsArray, 0, positions, 0) }
                drawRect(0f, 0f, 100f, 100f)
            }

        assertThat(dslBytes).isEqualTo(legacyBytes)
    }

    @Test
    fun testLinearGradientWithMaskComparison() {
        val profile = RcProfile(testProfile)
        val colorId = 0x10000001
        val colorsArray = intArrayOf(colorId, 0x00)
        val positions: FloatArray? = null

        // 1. Legacy
        val legacyContext = RemoteComposeContext(100, 100, "", testProfile)
        legacyContext.mRemoteWriter.root {
            legacyContext.mRemoteWriter.rcPaint
                .setLinearGradient(0f, 0f, 0f, 100f, colorsArray, 1, null, 0)
                .commit()
            legacyContext.mRemoteWriter.drawRect(0f, 0f, 100f, 100f)
        }
        val legacyBytes =
            legacyContext.mRemoteWriter.let { it.buffer().copyOfRange(0, it.bufferSize()) }

        // 2. DSL
        val dslBytes =
            createRcBuffer(
                profile,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, 100),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, 100),
                RemoteComposeWriter.hTag(Header.DOC_CONTENT_DESCRIPTION, ""),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, 0),
            ) {
                applyPaint { setLinearGradient(0f, 0f, 0f, 100f, colorsArray, 1, null, 0) }
                drawRect(0f, 0f, 100f, 100f)
            }

        assertThat(dslBytes).isEqualTo(legacyBytes)
    }
}
