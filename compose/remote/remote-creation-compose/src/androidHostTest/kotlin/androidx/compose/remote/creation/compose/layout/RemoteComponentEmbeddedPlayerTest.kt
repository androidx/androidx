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

@file:Suppress("RestrictedApiAndroidX")
@file:OptIn(
    ExperimentalRemoteCreationComposeApi::class,
    ExperimentalRemotePlayerApi::class,
)

package androidx.compose.remote.creation.compose.layout

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteWriterEvents
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.border
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.graphicsLayer
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.asRemoteDp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.toRemoteDp
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Embedded player (`RcPlayer`) tests verifying composition-scoped [remoteComponent] and
 * [rememberNamedRemoteFloat] resolution via Compose testing APIs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComponentEmbeddedPlayerTest {
    @get:Rule val rule = RcPlayerTestRule()

    private fun displayInfo(
        widthDp: Float = 240f,
        heightDp: Float = 240f,
    ): RemoteCreationDisplayInfo {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val density = context.resources.displayMetrics.density
        return createCreationDisplayInfo(
            context = context,
            size = Size(widthDp * density, heightDp * density),
        )
    }

    private fun dpLabel(pxValue: RemoteFloat): RemoteString =
        (pxValue.toRemoteDp().value + 0.5f).toRemoteInt().toRemoteString()

    @Test
    fun remoteComponent_afterPrecedingSibling_resolvesToEnclosingContainer() {
        rule.setRemoteContent(remoteCreationDisplayInfo = displayInfo()) {
            RemoteColumn(modifier = RemoteModifier.size(160.rdp, 200.rdp)) {
                // Preceding sibling that would hijack naive lastComponentId lookups.
                RemoteBox(modifier = RemoteModifier.size(24.rdp, 16.rdp))

                val parent = remoteComponent()
                val namedQuarterHeight =
                    rememberNamedRemoteFloat("parent_quarter_h", parent.height * 0.25f)

                RemoteBox(
                    modifier =
                        RemoteModifier.width((parent.width * 0.5f).toRemoteDp())
                            .height((parent.height * 0.25f).toRemoteDp())
                            .semantics { contentDescription = "HalfParentBox".rs }
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.width((parent.centerX).toRemoteDp())
                            .height(namedQuarterHeight.toRemoteDp())
                            .semantics { contentDescription = "NamedHalfBox".rs }
                )
                RemoteText(
                    "dim=".rs +
                        dpLabel(parent.width) +
                        "x".rs +
                        dpLabel(parent.height) +
                        " c=".rs +
                        dpLabel(parent.centerX) +
                        ",".rs +
                        dpLabel(parent.centerY)
                )
            }
        }

        rule
            .onNodeWithContentDescription("HalfParentBox")
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(50.dp)
        rule
            .onNodeWithContentDescription("NamedHalfBox")
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(50.dp)
        rule.onNodeWithText("dim=160x200 c=80,100").assertExists()
    }

    @Test
    fun remoteComponent_nestedContainersAndLazyCanvasRead_preserveDistinctScopes() {
        rule.setRemoteContent(remoteCreationDisplayInfo = displayInfo(240f, 240f)) {
            RemoteBox(
                modifier =
                    RemoteModifier.size(200.rdp, 180.rdp).padding(start = 20.rdp, top = 12.rdp)
            ) {
                // Captured without reading properties in composition; read lazily inside child
                // RemoteCanvas and descendant modifiers.
                val lazyOuter = remoteComponent()

                RemoteColumn(
                    modifier = RemoteModifier.size(140.rdp, 140.rdp),
                    horizontalAlignment = RemoteAlignment.End,
                ) {
                    RemoteBox(modifier = RemoteModifier.size(18.rdp, 10.rdp))
                    val middle = remoteComponent()

                    RemoteCanvas(
                        modifier =
                            RemoteModifier.size(50.rdp, 30.rdp).semantics {
                                contentDescription = "InnerCanvas".rs
                            }
                    ) {
                        val paint = RemotePaint { color = Color.Blue.rc }
                        drawRect(
                            paint = paint,
                            topLeft = RemoteOffset(0f.rf, 0f.rf),
                            size =
                                RemoteSize(
                                    lazyOuter.width * 0.1f + remote.component.width * 0.2f,
                                    middle.height * 0.1f + remote.component.height * 0.2f,
                                ),
                        )
                    }

                    RemoteBox(
                        modifier =
                            RemoteModifier.width((lazyOuter.width * 0.5f).toRemoteDp())
                                .height((middle.height * 0.25f).toRemoteDp())
                                .semantics { contentDescription = "ScopedDescendantBox".rs }
                    ) {
                        val shifted = remoteComponent()
                        val pos = shifted.positionInParent
                        val root = shifted.positionInRoot
                        RemoteText(
                            "pos=".rs +
                                dpLabel(pos.x) +
                                ",".rs +
                                dpLabel(pos.y) +
                                " root=".rs +
                                dpLabel(root.x) +
                                ",".rs +
                                dpLabel(root.y)
                        )
                    }
                }
            }
        }

        rule
            .onNodeWithContentDescription("ScopedDescendantBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(35.dp)
        rule
            .onNodeWithContentDescription("InnerCanvas")
            .assertLeftPositionInRootIsEqualTo(110.dp)
            .assertTopPositionInRootIsEqualTo(22.dp)
        // ScopedDescendantBox (100dp wide) is End-aligned in 140dp Column after 10dp+30dp siblings
        // => pos=(40,40) in Column, root=(20+40, 12+40)=(60,52)
        rule.onNodeWithText("pos=40,40 root=60,52").assertExists()
    }

    @Test
    fun remoteComponent_distinctSiblingsWithIdenticalExpressions_doNotCollideInCache() {
        rule.setRemoteContent(remoteCreationDisplayInfo = displayInfo()) {
            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 200.rdp)) {
                RemoteBox(modifier = RemoteModifier.size(120.rdp, 40.rdp)) {
                    val comp = remoteComponent()
                    RemoteBox(
                        modifier =
                            RemoteModifier.width((comp.width * 0.5f).toRemoteDp())
                                .height((comp.height * 0.5f).toRemoteDp())
                                .semantics { contentDescription = "FirstHalfBox".rs }
                    )
                }
                RemoteBox(modifier = RemoteModifier.size(60.rdp, 100.rdp)) {
                    val comp = remoteComponent()
                    RemoteBox(
                        modifier =
                            RemoteModifier.width((comp.width * 0.5f).toRemoteDp())
                                .height((comp.height * 0.5f).toRemoteDp())
                                .semantics { contentDescription = "SecondHalfBox".rs }
                    )
                }
            }
        }

        rule
            .onNodeWithContentDescription("FirstHalfBox")
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(20.dp)
        rule
            .onNodeWithContentDescription("SecondHalfBox")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun remoteComponent_hostResize_updatesDependentBoundsAndTextReactively() {
        var hostSize by mutableStateOf(DpSize(120.dp, 80.dp))

        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(200f, 200f),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(hostSize)) { content() }
            },
        ) {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(modifier = RemoteModifier.size(20.rdp, 10.rdp))
                val comp = remoteComponent()
                RemoteBox(
                    modifier =
                        RemoteModifier.width((comp.width * 0.5f).toRemoteDp())
                            .height((comp.height * 0.5f).toRemoteDp())
                            .semantics { contentDescription = "ReactiveHalfBox".rs }
                )
                RemoteText("host=".rs + dpLabel(comp.width) + "x".rs + dpLabel(comp.height))
            }
        }

        rule
            .onNodeWithContentDescription("ReactiveHalfBox")
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(40.dp)
        rule.onNodeWithText("host=120x80").assertExists()

        hostSize = DpSize(200.dp, 140.dp)
        rule.waitForIdle()

        rule
            .onNodeWithContentDescription("ReactiveHalfBox")
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(70.dp)
        rule.onNodeWithText("host=200x140").assertExists()
    }

    @Test
    fun remoteComponent_independentChildRecomposition_preservesParentTokenViaCompositionLocal() =
        runTest {
            val previousCleanRecomp =
                RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
            try {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val showDynamicChild = mutableStateOf(false)
                val info = displayInfo()

                val flow =
                    captureRemoteDocument(
                        creationDisplayInfo = info,
                        writerEvents = RemoteWriterEvents(),
                        context = context,
                        coroutineContext = coroutineContext,
                    ) {
                        RemoteColumn(
                            modifier =
                                RemoteModifier.size(140.rdp, 120.rdp).background(Color.White.rc)
                        ) {
                            RemoteBox(modifier = RemoteModifier.size(20.rdp, 10.rdp))
                            IndependentlyRecomposingChild(show = showDynamicChild.value)
                        }
                    }

                val documents = mutableListOf<ByteArray>()
                val collectJob = launch { flow.take(2).toList(documents) }

                testScheduler.advanceUntilIdle()
                Snapshot.withMutableSnapshot { showDynamicChild.value = true }
                Snapshot.sendApplyNotifications()
                testScheduler.advanceUntilIdle()
                collectJob.join()

                val recomposedDoc =
                    CoreDocument().apply {
                        initFromBuffer(
                            RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(documents[1]))
                        )
                    }

                rule.setContent {
                    Box(modifier = Modifier.size(240.dp)) { RcPlayer(document = recomposedDoc) }
                }
                rule.waitForIdle()

                rule
                    .onNodeWithContentDescription("RecomposedChildBox")
                    .assertWidthIsEqualTo(70.dp)
                    .assertHeightIsEqualTo(60.dp)
                rule.onNodeWithText("recomposed=140x120").assertExists()
            } finally {
                RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled =
                    previousCleanRecomp
            }
        }

    @Composable
    @RemoteComposable
    private fun IndependentlyRecomposingChild(show: Boolean) {
        if (show) {
            val parent = remoteComponent()
            val namedWidth = rememberNamedRemoteFloat("recomposed_w", parent.width)
            RemoteBox(
                modifier =
                    RemoteModifier.width((namedWidth * 0.5f).toRemoteDp())
                        .height((parent.height * 0.5f).toRemoteDp())
                        .semantics { contentDescription = "RecomposedChildBox".rs }
            )
            RemoteText("recomposed=".rs + dpLabel(parent.width) + "x".rs + dpLabel(parent.height))
        }
    }

    @Test
    fun componentValues_withoutEnclosingLayoutNode_failFastWhileNonComponentExpressionsSucceed() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()

            // Expressions created outside composition continue to work with normal forms.
            val base = MutableRemoteFloat(20f)
            val outsideMutable = MutableRemoteFloat(base + 20f)
            val outsideNamed = createNamedRemoteFloat("outside_named", base + 30f)

            rule.setRemoteContent(remoteCreationDisplayInfo = displayInfo()) {
                val rootNamed = rememberNamedRemoteFloat("root_named", 30f.rf)
                RemoteColumn(modifier = RemoteModifier.fillMaxWidth()) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(outsideMutable.asRemoteDp(), rootNamed.asRemoteDp())
                                .semantics { contentDescription = "OutsideMutableBox".rs }
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(
                                    (base + 40f).asRemoteDp(),
                                    outsideNamed.asRemoteDp(),
                                )
                                .semantics { contentDescription = "OutsideExprBox".rs }
                    )
                }
            }

            rule
                .onNodeWithContentDescription("OutsideMutableBox")
                .assertWidthIsEqualTo(40.dp)
                .assertHeightIsEqualTo(30.dp)
            rule
                .onNodeWithContentDescription("OutsideExprBox")
                .assertWidthIsEqualTo(60.dp)
                .assertHeightIsEqualTo(50.dp)

            // Calling remoteComponent() at root without an enclosing layout component fails fast.
            val rootRemoteComponentError =
                assertFailsWith<IllegalStateException> {
                    captureSingleRemoteDocument(context = context) {
                        val invalid = remoteComponent()
                        RemoteBox(modifier = RemoteModifier.width(invalid.width.toRemoteDp()))
                    }
                }
            assertThat(rootRemoteComponentError.message)
                .contains("remoteComponent() must be called inside a RemoteCompose layout")
        }

    @Test
    fun remoteStateScope_eagerModifiersAndActionsAcrossNodeTypes_resolveEachNodesOwnComponent() {
        val density =
            ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.density
        val recordedPx = mutableMapOf<String, Float>()

        fun eagerRecordOwnSizeAction(): RemoteAction =
            object : RemoteAction {
                override fun RemoteStateScope.toRemoteAction(): CreationAction {
                    // Invoked synchronously inside ClickableModifier.toRecordingModifierElement().
                    val comp = remoteComponent()
                    return combinedAction(
                            hostAction("w".rs, comp.width),
                            hostAction("h".rs, comp.height),
                        )
                        .run { toRemoteAction() }
                }
            }

        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(),
            onNamedAction = { name, value, _ -> recordedPx[name] = value as Float },
        ) {
            val bitmap = remember { ImageBitmap(16, 16).rb }
            val pillShape = RemoteRoundedCornerShape(percent = 50)

            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 220.rdp)) {
                // Preceding sibling with different dimensions.
                RemoteBox(modifier = RemoteModifier.size(20.rdp, 12.rdp))

                RemoteBox(
                    modifier =
                        RemoteModifier.size(80.rdp, 40.rdp)
                            .clip(RemoteCircleShape)
                            .border(2.rdp, Color.Black.rc, pillShape)
                            .graphicsLayer(shape = pillShape)
                            .clickable(eagerRecordOwnSizeAction())
                            .semantics { contentDescription = "ScopedModifierBox".rs }
                )
                RemoteText(
                    text = "Txt".rs,
                    modifier =
                        RemoteModifier.size(60.rdp, 30.rdp)
                            .clip(pillShape)
                            .border(1.rdp, Color.DarkGray.rc, pillShape)
                            .clickable(eagerRecordOwnSizeAction())
                            .semantics { contentDescription = "ScopedModifierText".rs },
                )
                RemoteImage(
                    bitmap = bitmap,
                    contentDescription = "ScopedModifierImage".rs,
                    modifier =
                        RemoteModifier.size(48.rdp, 24.rdp)
                            .clip(RemoteCircleShape)
                            .clickable(eagerRecordOwnSizeAction())
                            .semantics { contentDescription = "ScopedModifierImage".rs },
                    contentScale = RemoteContentScale.Fit,
                )
                RemoteCanvas(
                    modifier =
                        RemoteModifier.size(36.rdp, 18.rdp)
                            .clip(pillShape)
                            .clickable(eagerRecordOwnSizeAction())
                            .semantics { contentDescription = "ScopedModifierCanvas".rs }
                ) {}
            }
        }

        rule.onNodeWithContentDescription("ScopedModifierBox").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(80f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(40f * density)

        rule.onNodeWithContentDescription("ScopedModifierText").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(60f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(30f * density)

        rule.onNodeWithContentDescription("ScopedModifierImage").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(48f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(24f * density)

        rule.onNodeWithContentDescription("ScopedModifierCanvas").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(36f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(18f * density)
    }

    @Test
    fun remoteStateScope_deferredCombinedActionAcrossNodeTypes_resolvesClickedComponentsDimensions() {
        val density =
            ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.density
        val recordedPx = mutableMapOf<String, Float>()

        fun deferredRecordOwnSizeAction(): RemoteAction =
            // Nested combinedAction ensures inner CombinedAction.toRemoteAction() executes lazily
            // during RecordingModifier.Element.write(writer) after component start.
            combinedAction(
                combinedAction(
                    object : RemoteAction {
                        override fun RemoteStateScope.toRemoteAction(): CreationAction {
                            val comp = remoteComponent()
                            return combinedAction(
                                    hostAction("w".rs, comp.width),
                                    hostAction("h".rs, comp.height),
                                )
                                .run { toRemoteAction() }
                        }
                    }
                )
            )

        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(),
            onNamedAction = { name, value, _ -> recordedPx[name] = value as Float },
        ) {
            val bitmap = remember { ImageBitmap(16, 16).rb }

            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 200.rdp)) {
                RemoteBox(modifier = RemoteModifier.size(14.rdp, 8.rdp))
                RemoteBox(
                    modifier =
                        RemoteModifier.size(90.rdp, 40.rdp)
                            .clickable(deferredRecordOwnSizeAction())
                            .semantics { contentDescription = "ClickBox".rs }
                )
                RemoteText(
                    text = "TapText".rs,
                    modifier =
                        RemoteModifier.size(70.rdp, 30.rdp)
                            .clickable(deferredRecordOwnSizeAction()),
                )
                RemoteImage(
                    bitmap = bitmap,
                    contentDescription = "ClickImage".rs,
                    modifier =
                        RemoteModifier.size(50.rdp, 22.rdp)
                            .clickable(deferredRecordOwnSizeAction())
                            .semantics { contentDescription = "ClickImage".rs },
                    contentScale = RemoteContentScale.Fit,
                )
                RemoteCanvas(
                    modifier =
                        RemoteModifier.size(44.rdp, 18.rdp)
                            .clickable(deferredRecordOwnSizeAction())
                            .semantics { contentDescription = "ClickCanvas".rs }
                ) {}
            }
        }

        rule.onNodeWithContentDescription("ClickBox").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(90f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(40f * density)

        rule.onNodeWithText("TapText").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(70f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(30f * density)

        rule.onNodeWithContentDescription("ClickImage").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(50f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(22f * density)

        rule.onNodeWithContentDescription("ClickCanvas").performClick()
        rule.waitForIdle()
        assertThat(recordedPx["w"]).isWithin(0.5f).of(44f * density)
        assertThat(recordedPx["h"]).isWithin(0.5f).of(18f * density)
    }

    @Test
    fun remoteStateScope_drawWithContentAndCanvas_preserveActiveComponentBounds() {
        val density =
            ApplicationProvider.getApplicationContext<Context>().resources.displayMetrics.density
        val recordedPx = mutableMapOf<String, Float>()
        var capturedBoxWidth: RemoteFloat? = null
        var capturedCanvasWidth: RemoteFloat? = null
        var capturedCanvasHeight: RemoteFloat? = null

        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(),
            onNamedAction = { name, value, _ -> recordedPx[name] = value as Float },
        ) {
            RemoteColumn(modifier = RemoteModifier.size(200.rdp, 200.rdp)) {
                RemoteBox(modifier = RemoteModifier.size(16.rdp, 10.rdp))

                // Childless RemoteBox using drawWithContent + RemoteStateScope.remoteComponent().
                RemoteBox(
                    modifier =
                        RemoteModifier.size(110.rdp, 44.rdp).drawWithContent {
                            val boxComp = remoteComponent()
                            capturedBoxWidth = boxComp.width
                            drawRect(
                                paint = RemotePaint { color = Color.Cyan.rc },
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(boxComp.width, RemoteComponent(this).height),
                            )
                            drawContent()
                        }
                )

                // RemoteCanvas reading RemoteStateScope.remoteComponent() and
                // RemoteComponent(this).
                RemoteCanvas(modifier = RemoteModifier.size(86.rdp, 52.rdp)) {
                    val canvasComp = remoteComponent()
                    val dynamicComp = RemoteComponent(this)
                    capturedCanvasWidth = canvasComp.width
                    capturedCanvasHeight = dynamicComp.height
                    drawRect(
                        paint = RemotePaint { color = Color.Magenta.rc },
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(canvasComp.width, dynamicComp.height),
                    )
                }

                // Subsequent sibling renders after drawWithContent and RemoteCanvas, so its action
                // can read the captured draw-scope RemoteFloats.
                RemoteBox(
                    modifier =
                        RemoteModifier.size(24.rdp, 24.rdp)
                            .clickable(
                                object : RemoteAction {
                                    override fun RemoteStateScope.toRemoteAction(): CreationAction =
                                        combinedAction(
                                                hostAction(
                                                    "boxW".rs,
                                                    checkNotNull(capturedBoxWidth),
                                                ),
                                                hostAction(
                                                    "canvasW".rs,
                                                    checkNotNull(capturedCanvasWidth),
                                                ),
                                                hostAction(
                                                    "canvasH".rs,
                                                    checkNotNull(capturedCanvasHeight),
                                                ),
                                            )
                                            .run { toRemoteAction() }
                                }
                            )
                            .semantics { contentDescription = "ProbeDrawValues".rs }
                )
            }
        }

        rule.onNodeWithContentDescription("ProbeDrawValues").assertExists().performClick()
        rule.waitForIdle()
        assertThat(recordedPx["boxW"]).isWithin(0.5f).of(110f * density)
        assertThat(recordedPx["canvasW"]).isWithin(0.5f).of(86f * density)
        assertThat(recordedPx["canvasH"]).isWithin(0.5f).of(52f * density)
    }

    @Test
    fun remoteComponent_equalsAndHashCode_matchAcrossCompositionAndModifierScopes() {
        var outerComp1: RemoteComponent? = null
        var outerComp2: RemoteComponent? = null
        var outerDrawComp: RemoteComponent? = null
        var innerComp: RemoteComponent? = null

        rule.setRemoteContent(remoteCreationDisplayInfo = displayInfo()) {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                RemoteBox(
                    modifier =
                        RemoteModifier.size(120.rdp, 60.rdp).drawWithContent {
                            outerDrawComp = remoteComponent()
                            drawContent()
                        }
                ) {
                    outerComp1 = remoteComponent()
                    outerComp2 = remoteComponent()
                    RemoteBox(modifier = RemoteModifier.size(40.rdp, 20.rdp)) {
                        innerComp = remoteComponent()
                    }
                }
            }
        }
        rule.waitForIdle()

        assertThat(outerComp1).isNotNull()
        assertThat(outerComp1).isEqualTo(outerComp2)
        assertThat(outerComp1.hashCode()).isEqualTo(outerComp2.hashCode())
        assertThat(outerComp1).isEqualTo(outerDrawComp)
        assertThat(outerComp1.hashCode()).isEqualTo(outerDrawComp.hashCode())
        assertThat(outerComp1).isNotEqualTo(innerComp)
    }

    @Test
    fun remoteComponent_sharedExpressionInCanvasAndChildModifier_doesNotReadStaleDataPassId() {
        var hostSize by mutableStateOf(DpSize(200.dp, 120.dp))

        rule.setRemoteContent(
            remoteCreationDisplayInfo = displayInfo(240f, 240f),
            playComposableWrapper = { content ->
                Box(modifier = Modifier.size(hostSize)) { content() }
            },
        ) {
            RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
                val columnComp = remoteComponent()

                // 1. Container whose drawWithContent uses remoteComponent().width / 2f.rf multiple
                //    times (hoisting a CSE FloatExpression inside CanvasOperations) while its child
                //    uses the identical expression in Modifier.widthIn(max = ...).
                RemoteBox(
                    modifier =
                        RemoteModifier.fillMaxWidth().height(50.rdp).drawWithContent {
                            val halfWidth = remoteComponent().width / 2f.rf
                            val paint = RemotePaint { color = Color.Blue.rc }
                            drawRect(
                                paint = paint,
                                topLeft = RemoteOffset.Zero,
                                size = RemoteSize(halfWidth, 10f.rf),
                            )
                            drawRect(
                                paint = paint,
                                topLeft = RemoteOffset(halfWidth, 10f.rf),
                                size = RemoteSize(halfWidth, 10f.rf),
                            )
                            drawContent()
                        }
                ) {
                    val childMaxDp = (remoteComponent().width / 2f.rf).toRemoteDp()
                    RemoteBox(
                        modifier =
                            RemoteModifier.widthIn(max = childMaxDp)
                                .fillMaxWidth()
                                .height(20.rdp)
                                .semantics { contentDescription = "DrawWithContentChild".rs }
                    )
                }

                // 2. Sibling RemoteCanvas that hoists columnComp.width / 2f.rf multiple times
                //    inside CanvasContent before a sibling layout node uses the same expression.
                RemoteCanvas(modifier = RemoteModifier.size(80.rdp, 20.rdp)) {
                    val halfColWidth = columnComp.width / 2f.rf
                    val paint = RemotePaint { color = Color.Green.rc }
                    drawRect(
                        paint = paint,
                        topLeft = RemoteOffset.Zero,
                        size = RemoteSize(halfColWidth, 8f.rf),
                    )
                    drawRect(
                        paint = paint,
                        topLeft = RemoteOffset(halfColWidth, 8f.rf),
                        size = RemoteSize(halfColWidth, 8f.rf),
                    )
                }

                RemoteBox(
                    modifier =
                        RemoteModifier.width((columnComp.width / 2f.rf).toRemoteDp())
                            .height(20.rdp)
                            .semantics { contentDescription = "AfterCanvasSibling".rs }
                )
            }
        }

        rule.onNodeWithContentDescription("DrawWithContentChild").assertWidthIsEqualTo(100.dp)
        rule.onNodeWithContentDescription("AfterCanvasSibling").assertWidthIsEqualTo(100.dp)

        hostSize = DpSize(160.dp, 120.dp)
        rule.waitForIdle()

        rule.onNodeWithContentDescription("DrawWithContentChild").assertWidthIsEqualTo(80.dp)
        rule.onNodeWithContentDescription("AfterCanvasSibling").assertWidthIsEqualTo(80.dp)
    }
}
