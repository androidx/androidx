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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.ApplyTouchDown
import androidx.compose.remote.core.layout.ApplyTouchDrag
import androidx.compose.remote.core.layout.ApplyTouchUp
import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.LayoutTestPlayer
import androidx.compose.remote.core.layout.ResizeDocument
import androidx.compose.remote.core.layout.TestComponentVisibility
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.TestParameters
import androidx.compose.remote.core.operations.layout.Component
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.TextLayout.TEXT_ALIGN_START
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc.Time.CONTINUOUS_SEC
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.computeMeasure
import androidx.compose.remote.creation.computePosition
import androidx.compose.remote.creation.modifiers.RecordingModifier
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName

class LayoutTest : LayoutTestPlayer() {
    val GENERATE_GOLD_FILES: Boolean = false
    var platform: RcPlatformServices = RcPlatformServices.None

    @Rule @JvmField var name = TestName()

    class TestClock(val time: Long) : RemoteClock() {
        override fun nanoTime(): Long {
            return time
        }

        override fun getZone(): ZoneId? {
            return ZoneId.of("UTC")
        }

        override fun withZone(zone: ZoneId?): Clock? {
            return null
        }

        override fun instant(): Instant? {
            return Instant.ofEpochMilli(time)
        }
    }

    private fun checkLayout(
        w: Int,
        h: Int,
        apiLevel: Int,
        profile: Int,
        description: String,
        ops: ArrayList<TestOperation?>,
        testClock: RemoteClock = TestClock(1234),
    ) {
        if (ops.size == 0) {
            return
        }
        if (ops[0] !is TestLayout) {
            return
        }
        val function = (ops[0] as TestLayout).layout
        val testParameters = TestParameters(name.getMethodName(), GENERATE_GOLD_FILES, testClock)
        val writer =
            RemoteComposeContext(
                    w,
                    h,
                    description,
                    apiLevel,
                    profile,
                    platform,
                    { root { function.invoke(this) } },
                )
                .writer
        play(writer, ops, testParameters)
    }

    data class TestLayout(var layout: RemoteComposeContext.() -> Unit) : TestOperation() {
        override fun apply(
            context: RemoteContext,
            document: CoreDocument,
            testParameters: TestParameters,
            commands: List<Map<String?, Any?>?>?,
        ): Boolean {
            // Nothing here
            return false
        }
    }

    @Test
    fun testTouchDownVisibilityChange() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val visibilityId = writer.addInteger(Component.Visibility.GONE)
                    row(
                        Modifier.componentId(1).fillMaxHeight().background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.CENTER,
                    ) {
                        box(
                            Modifier.componentId(2)
                                .size(200)
                                .visibility(visibilityId.toInt())
                                .background(Color.GREEN)
                        )
                        box(
                            Modifier.componentId(3)
                                .size(100)
                                .background(Color.RED)
                                .onTouchDown(
                                    ValueIntegerChange(
                                        visibilityId.toInt(),
                                        Component.Visibility.VISIBLE,
                                    )
                                )
                        )
                    }
                },
                TestComponentVisibility(2, Component.Visibility.GONE),
                ApplyTouchDown(50f, 50f),
                TestComponentVisibility(2, Component.Visibility.VISIBLE),
                CaptureComponentTree(),
            )
        checkLayout(1000, 1000, 7, RcProfiles.PROFILE_ANDROIDX, "Layout", ops)
    }

    @Test
    fun testBaselineRowLayout() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    row(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.SPACE_EVENLY,
                        vertical = RowLayout.CENTER,
                    ) {
                        text("the", modifier = Modifier.alignByBaseline())
                        text("quick", fontSize = 100f, modifier = Modifier.alignByBaseline())
                        text("brown", fontSize = 30f, modifier = Modifier.alignByBaseline())
                        text("fox", modifier = Modifier.alignByBaseline())
                    }
                }
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }

    @Test
    fun testLayoutComputeModifier() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    box(Modifier.fillMaxSize().padding(8)) {
                        val size = 50f
                        box(
                            Modifier.componentId(2)
                                .computeMeasure { width = size * 3f }
                                .height(size.toInt())
                                .background(Color.GREEN)
                        )
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }

    @Test
    fun testLayoutComputeModifier2() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    box(Modifier.fillMaxSize().padding(8)) {
                        val size = 50f
                        box(
                            Modifier.componentId(2)
                                .computeMeasure { width = size * 3f }
                                .computePosition {
                                    x = (parentWidth - width) / 2f
                                    y = (parentHeight - height) / 2f
                                }
                                .height(size.toInt())
                                .background(Color.GREEN)
                        )
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }

    @Test
    fun testLayoutTextFromFloat1() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    box(
                        RecordingModifier().fillMaxSize().background(Color.YELLOW),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        val textId = createTextFromFloat(CONTINUOUS_SEC, 5, 5, 3)
                        textComponent(
                            RecordingModifier().fillMaxWidth().background(Color.GREEN),
                            textId,
                            0xFF000000.toInt(),
                            20f,
                            0,
                            500f,
                            "Sans Serif",
                            TEXT_ALIGN_START,
                            0,
                            1,
                        ) {}
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testLayoutTextOverflow() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    row(
                        Modifier.background(Color.GREEN).padding(8).fillMaxWidth(),
                        vertical = RowLayout.CENTER,
                    ) {
                        column(Modifier.horizontalWeight(1f).background(Color.YELLOW)) {
                            text(
                                "New Arsenal Game",
                                maxLines = 1,
                                overflow = CoreText.OVERFLOW_ELLIPSIS,
                            )
                            text(
                                "Arsenal vs Bayern Munich",
                                fontSize = 64f,
                                maxLines = 3,
                                overflow = CoreText.OVERFLOW_ELLIPSIS,
                            )
                            text(
                                "UEFA Champions League Group Stage",
                                maxLines = 2,
                                overflow = CoreText.OVERFLOW_ELLIPSIS,
                            )
                            text(
                                "Wednesday 26th November",
                                maxLines = 1,
                                overflow = CoreText.OVERFLOW_ELLIPSIS,
                            )
                        }
                        column(
                            Modifier.size(130).background(Color.BLUE).padding(8),
                            ColumnLayout.CENTER,
                            ColumnLayout.CENTER,
                        ) {
                            box(
                                Modifier.size(100).background(Color.YELLOW),
                                BoxLayout.CENTER,
                                BoxLayout.CENTER,
                            ) {
                                text("IMG")
                            }
                        }
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(800, 1000),
                CaptureComponentTree(),
                ResizeDocument(500, 1000),
                CaptureComponentTree(),
                ResizeDocument(200, 1000),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testLayoutInfiniteDrawContent() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    box(
                        RecordingModifier().fillMaxSize().background(Color.YELLOW),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        startCanvasOperations()
                        drawComponentContent()
                        endCanvasOperations()
                        drawComponentContent()
                        endCanvasOperations()
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(1000, 1000, 7, RcProfiles.PROFILE_ANDROIDX, "Layout", ops, TestClock(1234))
    }

    @Test
    fun testCanvasComponents() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize().background(Color.YELLOW).padding(16)) {
                        canvas(Modifier.fillMaxSize().background(Color.BLUE)) {
                            val w = ComponentWidth()
                            val h = ComponentHeight()
                            drawLine(0f, 0f, w.toFloat(), h.toFloat())
                            drawLine(0f, h.toFloat(), w.toFloat(), 0f)
                            box(
                                Modifier.background(Color.YELLOW).size(300, 200).computePosition {
                                    x = w / 2f - width as RFloat / 2f
                                    y = h / 2f - height as RFloat / 2f
                                }
                            ) {
                                text(
                                    "Hello, World!",
                                    autosize = true,
                                    textAlign = CoreText.TEXT_ALIGN_CENTER,
                                )
                            }
                        }
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testScrollComponents() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16).verticalScroll()
                    ) {
                        canvas(Modifier.fillMaxWidth().height(2000).background(Color.BLUE)) {
                            val w = ComponentWidth()
                            val h = ComponentHeight()
                            drawLine(0f, 0f, w.toFloat(), h.toFloat())
                            drawLine(0f, h.toFloat(), w.toFloat(), 0f)
                            box(
                                Modifier.background(Color.YELLOW).size(300, 200).computePosition {
                                    x = w / 2f - width as RFloat / 2f
                                    y = h / 2f - height as RFloat / 2f
                                }
                            ) {
                                text(
                                    "Hello, World!",
                                    autosize = true,
                                    textAlign = CoreText.TEXT_ALIGN_CENTER,
                                )
                            }
                        }
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
                CaptureComponentTree(),
                ApplyTouchDown(200f, 400f),
                ApplyTouchDrag(200f, 200f),
                ApplyTouchUp(200f, 200f),
                CaptureComponentTree(),
                ResizeDocument(800, 1000),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testMeasure1() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED))
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
            )
        checkLayout(
            100,
            100,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testMeasure2() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxWidth().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED))
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
            )
        checkLayout(
            100,
            100,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testMeasure3() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxHeight().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED))
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
            )
        checkLayout(
            100,
            100,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testMeasure4() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED))
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(600, 800),
            )
        checkLayout(
            100,
            100,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testComponentsValues1() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED)) {
                            val width = ComponentWidth()
                            val height = ComponentHeight()
                            box(
                                Modifier.size(10).computePosition {
                                    x = width
                                    y = height
                                }
                            )
                        }
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testComponentsValues2() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        box(Modifier.size(300, 200).background(Color.RED)) {
                            val cx = ComponentX()
                            val cy = ComponentY()
                            val rootX = ComponentRootX()
                            val rootY = ComponentRootY()
                            box(
                                Modifier.size(10).computePosition {
                                    x = cx
                                    y = cy
                                }
                            )
                            box(
                                Modifier.size(10).computePosition {
                                    x = rootX
                                    y = rootY
                                }
                            )
                        }
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testComponentsValues3() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        column(Modifier.size(300, 200).background(Color.RED).verticalScroll()) {
                            for (i in 0..10) {
                                box(RecordingModifier().size(100))
                            }
                            val componentHeight = ComponentHeight()
                            val contentHeight = ComponentContentHeight()
                            box(
                                Modifier.size(10).computePosition {
                                    x = componentHeight
                                    y = contentHeight
                                }
                            )
                        }
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }

    @Test
    fun testComponentsValues4() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(16),
                        horizontal = ColumnLayout.CENTER,
                        vertical = ColumnLayout.CENTER,
                    ) {
                        row(Modifier.size(300, 200).background(Color.RED).horizontalScroll()) {
                            for (i in 0..10) {
                                box(RecordingModifier().size(100))
                            }
                            val componentHeight = ComponentHeight()
                            val contentHeight = ComponentContentHeight()
                            box(
                                Modifier.size(10).computePosition {
                                    x = componentHeight
                                    y = contentHeight
                                }
                            )
                        }
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
            TestClock(1234),
        )
    }
}
