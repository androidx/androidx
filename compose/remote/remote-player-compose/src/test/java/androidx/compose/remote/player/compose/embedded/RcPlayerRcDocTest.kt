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
@file:OptIn(ExperimentalRemotePlayerApi::class)

package androidx.compose.remote.player.compose.embedded

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.MapRcDocVariables
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RcDocLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.actions.ValueIntegerChange
import androidx.compose.remote.creation.actions.ValueStringChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w400dp-h400dp-mdpi")
class RcPlayerRcDocTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private fun buildBytes(
        width: Int = 200,
        height: Int = 100,
        block: RemoteComposeWriter.() -> Unit,
    ): ByteArray {
        val writer =
            RemoteComposeWriterAndroid(
                width,
                height,
                "EmbeddedChild",
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            )
        writer.block()
        return writer.encodeToByteArray()
    }

    private fun RemoteComposeWriter.addCoreText(
        textId: Int,
        modifier: RecordingModifier = RecordingModifier(),
        color: Int = AndroidColor.BLACK,
        fontSize: Float = 16f,
    ) {
        textComponent(
            modifier,
            textId,
            color,
            fontSize,
            0,
            400f,
            null,
            CoreText.TEXT_ALIGN_LEFT,
            CoreText.OVERFLOW_CLIP,
            1,
        ) {}
    }

    private fun RemoteComposeWriter.addCoreText(
        text: String,
        modifier: RecordingModifier = RecordingModifier(),
        color: Int = AndroidColor.BLACK,
        fontSize: Float = 16f,
    ) {
        addCoreText(addText(text), modifier, color, fontSize)
    }

    @Test
    fun rcDoc_viaRcResolver_rendersChildAndAdoptsIntrinsicSize() {
        val childBytes =
            buildBytes(width = 160, height = 80) {
                root {
                    box(
                        RecordingModifier().fillMaxSize().background(AndroidColor.GREEN),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        addCoreText("ChildHello")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 400, height = 400) {
                root {
                    box(
                        RecordingModifier().padding(30, 50, 0, 0),
                        BoxLayout.START,
                        BoxLayout.TOP,
                    ) {
                        rcDoc("widget://card")
                    }
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(400.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "widget://card") childBytes else null
                        },
                )
            }
        }

        rule.onNodeWithText("ChildHello").assertIsDisplayed()
        // At mdpi (density = 1), 160x80 px == 160x80 dp, placed inside the (30, 50) padding.
        val bitmap = rule.captureRootBitmap()
        // Inside the embedded green child (30..190, 50..130):
        assertThat(bitmap.getPixel(35, 55)).isEqualTo(AndroidColor.GREEN)
        assertThat(bitmap.getPixel(185, 125)).isEqualTo(AndroidColor.GREEN)
        // Outside the embedded child:
        assertThat(bitmap.getPixel(20, 55)).isNotEqualTo(AndroidColor.GREEN)
        assertThat(bitmap.getPixel(200, 125)).isNotEqualTo(AndroidColor.GREEN)
    }

    @Test
    fun rcDoc_unresolvedAndLateRegisteredRcResolver() {
        val childBytes =
            buildBytes(width = 120, height = 60) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("ResolvedLate")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 300, height = 300) {
                root { rcDoc("host://late", RecordingModifier().size(120, 60)) }
            }

        var resolver by mutableStateOf<RcDocumentResolver?>(null)
        var nullResolveCalls = 0

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(document = hostDoc, documentResolver = resolver)
            }
        }

        // 1. Without a resolver, renders cleanly with no child text.
        rule.onNodeWithText("ResolvedLate").assertDoesNotExist()

        // 2. Registering a null-returning resolver resolves once and does not spam.
        resolver = RcDocumentResolver { _ ->
            nullResolveCalls++
            null
        }
        rule.waitForIdle()
        assertThat(nullResolveCalls).isEqualTo(1)
        rule.onNodeWithText("ResolvedLate").assertDoesNotExist()

        // 3. Swapping in a resolver that returns valid bytes loads and renders the child.
        resolver = RcDocumentResolver { url -> if (url == "host://late") childBytes else null }
        rule.waitForIdle()
        rule.onNodeWithText("ResolvedLate").assertIsDisplayed()
    }

    @Test
    fun rcDoc_suspendingDocumentResolver_loadsAsynchronously() {
        val childBytes =
            buildBytes(width = 140, height = 60) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("LoadedAsync")
                    }
                }
            }
        val hostDoc =
            buildWriterDocument(width = 300, height = 300) {
                root { rcDoc("async://card", RecordingModifier().size(140, 60)) }
            }
        val deferred = CompletableDeferred<ByteArray?>()

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "async://card") deferred.await() else null
                        },
                )
            }
        }

        rule.onNodeWithText("LoadedAsync").assertDoesNotExist()
        deferred.complete(childBytes)
        rule.waitForIdle()
        rule.onNodeWithText("LoadedAsync").assertIsDisplayed()
    }

    @Test
    fun rcDoc_embeddedBinaryDataById_loadsWithoutResolver() {
        val childBytes =
            buildBytes(width = 160, height = 60) {
                val titleId = addNamedString("title", "default")
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText(titleId)
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 300, height = 300) {
                val hostTitle = addNamedString("hostTitle", "InlineTitle")
                val embeddedId = addEmbeddedDocument(childBytes)
                root {
                    val docId = rcDoc(embeddedId, RecordingModifier().size(160, 60))
                    mapRcDocVariables(docId, RemoteComposeWriter.MAP_FORWARD, hostTitle, "title")
                }
            }

        // No RcResolver provided at all.
        rule.setContent { Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = hostDoc) } }

        rule.onNodeWithText("InlineTitle").assertIsDisplayed()
    }

    @Test
    fun rcDoc_embeddedUrlScheme_switchesBetweenEmbeddedAndResolved() {
        fun childWithLabel(label: String): ByteArray =
            buildBytes(width = 120, height = 50) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText(label)
                    }
                }
            }

        val childA = childWithLabel("EmbeddedA")
        val childB = childWithLabel("EmbeddedB")
        val childC = childWithLabel("ResolvedC")

        var embeddedBId = -1
        val hostDoc =
            buildWriterDocument(width = 300, height = 300) {
                val embeddedA = addEmbeddedDocument(childA)
                embeddedBId = addEmbeddedDocument(childB)
                val urlVar = addNamedString("docUrl", RcDocLayout.EMBEDDED_URL_PREFIX + embeddedA)
                root { rcDoc(urlVar, RecordingModifier().size(120, 50)) }
            }

        val resolvedUrls = mutableListOf<String>()
        val playerState = RcPlayerState(hostDoc, defaultPrefix = null)
        var docUrl by playerState.stringState("docUrl")

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(
                    state = playerState,
                    documentResolver =
                        RcDocumentResolver { url ->
                            resolvedUrls.add(url)
                            if (url == "host://c") childC else null
                        },
                )
            }
        }

        rule.onNodeWithText("EmbeddedA").assertIsDisplayed()
        assertThat(resolvedUrls).isEmpty()

        // Switch to second embedded blob: resolver is still not consulted.
        docUrl = RcDocLayout.EMBEDDED_URL_PREFIX + embeddedBId
        rule.waitForIdle()
        rule.onNodeWithText("EmbeddedB").assertIsDisplayed()
        assertThat(resolvedUrls).isEmpty()

        // Switch to external URL: resolver is consulted.
        docUrl = "host://c"
        rule.waitForIdle()
        rule.onNodeWithText("ResolvedC").assertIsDisplayed()
        assertThat(resolvedUrls).containsExactly("host://c")

        // Switch to missing embedded blob id: falls through cleanly and renders nothing.
        docUrl = RcDocLayout.EMBEDDED_URL_PREFIX + "9999"
        rule.waitForIdle()
        rule.onNodeWithText("ResolvedC").assertDoesNotExist()
    }

    @Test
    fun rcDoc_dynamicUrlVariableReloadsDocumentOnClick() {
        val childA =
            buildBytes(width = 120, height = 50) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("DocA")
                    }
                }
            }
        val childB =
            buildBytes(width = 120, height = 50) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("DocB")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 150) {
                val urlVarId = addNamedString("docUrl", "widget://a")
                root {
                    column(
                        RecordingModifier().size(120, 100),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        box(
                            RecordingModifier()
                                .size(120, 50)
                                .onClick(ValueStringChange(urlVarId, "widget://b")),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("Switch")
                        }
                        rcDoc(urlVarId, RecordingModifier().size(120, 50))
                    }
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            when (url) {
                                "widget://a" -> childA
                                "widget://b" -> childB
                                else -> null
                            }
                        },
                )
            }
        }

        rule.onNodeWithText("DocA").assertIsDisplayed()
        rule.onNodeWithText("Switch").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("DocB").assertIsDisplayed()
    }

    @Test
    fun rcDoc_defaultNestingDepthIsOne_andConfigurableViaLocal() {
        val leafBytes =
            buildBytes(width = 100, height = 40) {
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("LeafDoc")
                    }
                }
            }
        val middleBytes =
            buildBytes(width = 150, height = 80) {
                val leafId = addEmbeddedDocument(leafBytes)
                root {
                    column(
                        RecordingModifier().size(150, 80),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addCoreText("MiddleDoc")
                        rcDoc(leafId, RecordingModifier().size(100, 40))
                    }
                }
            }
        val hostDoc =
            buildWriterDocument(width = 300, height = 300) {
                val middleId = addEmbeddedDocument(middleBytes)
                root { rcDoc(middleId, RecordingModifier().size(150, 80)) }
            }

        var maxDepth by mutableStateOf(DEFAULT_MAX_RC_DOC_NESTING_DEPTH)

        rule.setContent {
            CompositionLocalProvider(LocalRcDocMaxNestingDepth provides maxDepth) {
                Box(modifier = Modifier.size(300.dp)) { RcPlayer(document = hostDoc) }
            }
        }

        // Default depth (1): direct child renders, grandchild is skipped.
        rule.onNodeWithText("MiddleDoc").assertIsDisplayed()
        rule.onNodeWithText("LeafDoc").assertDoesNotExist()

        // Increasing maxDepth to 2 enables grandchildren.
        maxDepth = 2
        rule.waitForIdle()
        rule.onNodeWithText("MiddleDoc").assertIsDisplayed()
        rule.onNodeWithText("LeafDoc").assertIsDisplayed()

        // Setting maxDepth to 0 disables embedded documents altogether.
        maxDepth = 0
        rule.waitForIdle()
        rule.onNodeWithText("MiddleDoc").assertDoesNotExist()
        rule.onNodeWithText("LeafDoc").assertDoesNotExist()
    }

    @Test
    fun rcDoc_cyclicResolver_stopsAtNestingCapWithoutCrashing() {
        val cyclicBytes =
            buildBytes(width = 100, height = 50) {
                root { rcDoc("loop://self", RecordingModifier().size(100, 50)) }
            }
        val hostDoc =
            buildWriterDocument(width = 200, height = 200) {
                root {
                    column(
                        RecordingModifier().size(200, 200),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addCoreText("HostStillAlive")
                        rcDoc("loop://self", RecordingModifier().size(100, 50))
                    }
                }
            }

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "loop://self") cyclicBytes else null
                        },
                )
            }
        }

        rule.onNodeWithText("HostStillAlive").assertIsDisplayed()
    }

    @Test
    fun mapRcDocVariables_forwardReverseAndBidirectional() {
        val childBytes =
            buildBytes(width = 200, height = 100) {
                val inTitle = addNamedString("inTitle", "defaultTitle")
                val inCount = addNamedInt("inCount", 0).toInt()
                val outStatus = addNamedString("outStatus", "childReady")
                val biCounter = addNamedFloat("biCounter", 10f)
                val biCounterText = createTextFromFloat(biCounter, 3, 0, TextFromFloat.PAD_PRE_NONE)
                val inCountFloat = floatExpression(Utils.asNan(inCount))
                val inCountText =
                    createTextFromFloat(inCountFloat, 3, 0, TextFromFloat.PAD_PRE_NONE)

                root {
                    column(
                        RecordingModifier()
                            .size(200, 100)
                            .onClick(
                                ValueFloatChange(Utils.idFromNan(biCounter), 42f),
                                ValueStringChange(outStatus, "childClicked"),
                            ),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addCoreText(inTitle)
                        addCoreText(biCounterText)
                        addCoreText(inCountText)
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 300, height = 200) {
                val hostTitle = addNamedString("hostTitle", "ParentTitle")
                val hostCount = addNamedInt("hostCount", 5).toInt()
                val hostStatus = addNamedString("hostStatus", "initial")
                val hostCounter = addNamedFloat("hostCounter", 7f)

                root {
                    column(
                        RecordingModifier().size(200, 200),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addCoreText(hostStatus)
                        val docId = rcDoc("widget://vars", RecordingModifier().size(200, 100))
                        mapRcDocVariables(
                            docId,
                            RemoteComposeWriter.MAP_FORWARD,
                            hostTitle,
                            "inTitle",
                        )
                        mapRcDocVariables(
                            docId,
                            RemoteComposeWriter.MAP_FORWARD,
                            hostCount,
                            "inCount",
                        )
                        mapRcDocVariables(
                            docId,
                            RemoteComposeWriter.MAP_REVERSE,
                            hostStatus,
                            "outStatus",
                        )
                        mapRcDocVariables(
                            docId,
                            RemoteComposeWriter.MAP_BIDIRECTIONAL,
                            hostCounter,
                            "biCounter",
                        )
                    }
                }
            }

        val playerState = RcPlayerState(hostDoc, defaultPrefix = null)
        var hostTitle by playerState.stringState("hostTitle")
        var hostCount by playerState.intState("hostCount")
        val hostStatus by playerState.stringState("hostStatus")
        var hostCounter by playerState.floatState("hostCounter")

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(
                    state = playerState,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "widget://vars") childBytes else null
                        },
                )
            }
        }

        // 1. Initial sync: forward pushed "ParentTitle" & 5, reverse pulled "childReady",
        // bidirectional pushed initial host value 7.
        rule.onNodeWithText("ParentTitle").assertIsDisplayed()
        rule.onNodeWithText("7").assertIsDisplayed()
        rule.onNodeWithText("5").assertIsDisplayed()
        rule.onNodeWithText("childReady").assertIsDisplayed()
        assertThat(hostStatus).isEqualTo("childReady")

        // 2. Host updates propagate forward and bidirectionally into the child.
        hostTitle = "UpdatedFromHost"
        hostCount = 9
        hostCounter = 15f
        rule.waitForIdle()
        rule.onNodeWithText("UpdatedFromHost").assertIsDisplayed()
        rule.onNodeWithText("9").assertIsDisplayed()
        rule.onNodeWithText("15").assertIsDisplayed()

        // 3. Clicking inside the child updates biCounter -> 42 and outStatus -> "childClicked",
        // which sync back to the host.
        rule.onNodeWithText("UpdatedFromHost").performClick()
        rule.waitForIdle()
        assertThat(hostCounter).isEqualTo(42f)
        assertThat(hostStatus).isEqualTo("childClicked")
        rule.onNodeWithText("42").assertIsDisplayed()
        rule.onNodeWithText("childClicked").assertIsDisplayed()
    }

    @Test
    fun mapRcDocVariables_hostActionMapsKnownIdAndForwardsUnknownId() {
        val mappedActionId = 1001
        val unmappedActionId = 2002

        val childBytes =
            buildBytes(width = 200, height = 100) {
                val mappedMetaId = addText("mapped_payload")
                val unmappedMetaId = addText("unmapped_payload")
                root {
                    column(
                        RecordingModifier().size(200, 100),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        box(
                            RecordingModifier()
                                .size(200, 50)
                                .onClick(HostAction(mappedActionId, mappedMetaId)),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("MappedBtn")
                        }
                        box(
                            RecordingModifier()
                                .size(200, 50)
                                .onClick(HostAction(unmappedActionId, unmappedMetaId)),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("UnmappedBtn")
                        }
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 300, height = 200) {
                val actionResultVar = addNamedString("actionResult", "none")
                root {
                    column(
                        RecordingModifier().size(200, 160),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        addCoreText(actionResultVar)
                        val docId = rcDoc("widget://actions", RecordingModifier().size(200, 100))
                        mapRcDocVariables(
                            docId,
                            MapRcDocVariables.MAP_HOST_ACTION,
                            actionResultVar,
                            mappedActionId,
                        )
                    }
                }
            }

        val hostReceivedActions = mutableListOf<Pair<Int, String?>>()

        rule.setContent {
            Box(modifier = Modifier.size(300.dp)) {
                RcPlayer(
                    document = hostDoc,
                    documentResolver =
                        RcDocumentResolver { url ->
                            if (url == "widget://actions") childBytes else null
                        },
                    onAction = { id, value -> hostReceivedActions.add(id to value) },
                )
            }
        }

        rule.onNodeWithText("none").assertIsDisplayed()

        // 1. Mapped action writes metadata to host string variable and is NOT forwarded to host.
        rule.onNodeWithText("MappedBtn").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("mapped_payload").assertIsDisplayed()
        assertThat(hostReceivedActions).isEmpty()

        // 2. Unmapped action IS forwarded to host onAction callback.
        rule.onNodeWithText("UnmappedBtn").performClick()
        rule.waitForIdle()
        assertThat(hostReceivedActions).containsExactly(unmappedActionId to "unmapped_payload")
    }

    @Test
    fun rcDoc_twoChildrenInteractViaParentBusAndGrandchildExpression() {
        val childABytes =
            buildBytes(width = 160, height = 50) {
                val outVal = addNamedInt("outVal", 10).toInt()
                root {
                    box(
                        RecordingModifier().size(160, 50).onClick(ValueIntegerChange(outVal, 30)),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        addCoreText("BumpA")
                    }
                }
            }

        val grandchildBytes =
            buildBytes(width = 160, height = 50) {
                val level = addNamedFloat("level", 0f)
                val levelText = createTextFromFloat(level, 3, 0, TextFromFloat.PAD_PRE_NONE)
                root {
                    box(RecordingModifier().fillMaxSize(), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText(levelText)
                    }
                }
            }

        val middleBytes =
            buildBytes(width = 160, height = 50) {
                val inLevel = addNamedFloat("inLevel", 0f)
                val complement = floatExpression(100f, inLevel, AnimatedFloatExpression.SUB)
                root {
                    val gcId = rcDoc("child://grandchild", RecordingModifier().size(160, 50))
                    mapRcDocVariables(gcId, RemoteComposeWriter.MAP_FORWARD, complement, "level")
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 200) {
                val sharedInt = addNamedInt("shared", 10).toInt()
                val sharedFloat = floatExpression(Utils.asNan(sharedInt))
                root {
                    column(
                        RecordingModifier().size(160, 120),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        val aId = rcDoc("child://a", RecordingModifier().size(160, 50))
                        mapRcDocVariables(aId, RemoteComposeWriter.MAP_REVERSE, sharedInt, "outVal")

                        val midId = rcDoc("child://middle", RecordingModifier().size(160, 50))
                        mapRcDocVariables(
                            midId,
                            RemoteComposeWriter.MAP_FORWARD,
                            sharedFloat,
                            "inLevel",
                        )
                    }
                }
            }

        rule.setContent {
            CompositionLocalProvider(LocalRcDocMaxNestingDepth provides 2) {
                Box(modifier = Modifier.size(200.dp)) {
                    RcPlayer(
                        document = hostDoc,
                        documentResolver =
                            RcDocumentResolver { url ->
                                when (url) {
                                    "child://a" -> childABytes
                                    "child://middle" -> middleBytes
                                    "child://grandchild" -> grandchildBytes
                                    else -> null
                                }
                            },
                    )
                }
            }
        }

        // Initial: Child A outVal = 10 -> host shared = 10 -> middle complement = 90 -> grandchild
        // 90
        rule.onNodeWithText("90").assertIsDisplayed()

        // Click Child A -> outVal = 30 -> host shared = 30 -> middle complement = 70 -> grandchild
        // 70
        rule.onNodeWithText("BumpA").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("70").assertIsDisplayed()
    }

    @Test
    fun scrollOverride_verticalActionUpdatesComposeScrollPosition() {
        val doc =
            buildWriterDocument(width = 200, height = 300) {
                val pos = addNamedFloat("pos", 0f)
                val step = floatExpression(pos, 100f, AnimatedFloatExpression.ADD)
                root {
                    column(
                        RecordingModifier().size(200, 300),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        column(
                            RecordingModifier().size(200, 100).verticalScroll(pos),
                            ColumnLayout.START,
                            ColumnLayout.TOP,
                        ) {
                            for (i in 0 until 5) {
                                box(
                                    RecordingModifier().size(200, 100),
                                    BoxLayout.CENTER,
                                    BoxLayout.CENTER,
                                ) {
                                    addCoreText("Item$i")
                                }
                            }
                        }
                        box(
                            RecordingModifier()
                                .size(200, 100)
                                .onClick(
                                    ValueFloatExpressionChange(
                                        Utils.idFromNan(pos),
                                        Utils.idFromNan(step),
                                    )
                                ),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("ScrollNext")
                        }
                    }
                }
            }

        val state = RcPlayerState(doc, defaultPrefix = null)
        val pos by state.floatState("pos")

        rule.setContent {
            Box(modifier = Modifier.size(200.dp, 300.dp).testTag("root")) {
                RcPlayer(state = state)
            }
        }

        assertThat(pos).isEqualTo(0f)
        val initialItem0Top = rule.onNodeWithText("Item0").getUnclippedBoundsInRoot().top

        rule.onNodeWithText("ScrollNext").performClick()
        rule.waitForIdle()
        assertThat(pos).isEqualTo(100f)
        val afterFirstClickTop = rule.onNodeWithText("Item0").getUnclippedBoundsInRoot().top
        assertThat(initialItem0Top.value - afterFirstClickTop.value).isWithin(2f).of(100f)

        rule.onNodeWithText("ScrollNext").performClick()
        rule.waitForIdle()
        assertThat(pos).isEqualTo(200f)
        val afterSecondClickTop = rule.onNodeWithText("Item0").getUnclippedBoundsInRoot().top
        assertThat(initialItem0Top.value - afterSecondClickTop.value).isWithin(2f).of(200f)
    }

    @Test
    fun scrollOverride_horizontalActionUpdatesComposeScrollPosition() {
        val doc =
            buildWriterDocument(width = 200, height = 200) {
                val pos = addNamedFloat("pos", 0f)
                val step = floatExpression(pos, 100f, AnimatedFloatExpression.ADD)
                root {
                    column(
                        RecordingModifier().size(200, 200),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        row(
                            RecordingModifier().size(200, 100).horizontalScroll(pos),
                            RowLayout.START,
                            RowLayout.TOP,
                        ) {
                            for (i in 0 until 5) {
                                box(
                                    RecordingModifier().size(100, 100),
                                    BoxLayout.CENTER,
                                    BoxLayout.CENTER,
                                ) {
                                    addCoreText("HItem$i")
                                }
                            }
                        }
                        box(
                            RecordingModifier()
                                .size(200, 100)
                                .onClick(
                                    ValueFloatExpressionChange(
                                        Utils.idFromNan(pos),
                                        Utils.idFromNan(step),
                                    )
                                ),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("ScrollRight")
                        }
                    }
                }
            }

        val state = RcPlayerState(doc, defaultPrefix = null)
        val pos by state.floatState("pos")

        rule.setContent {
            Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = state) }
        }

        assertThat(pos).isEqualTo(0f)
        val initialLeft = rule.onNodeWithText("HItem0").getUnclippedBoundsInRoot().left

        rule.onNodeWithText("ScrollRight").performClick()
        rule.waitForIdle()
        assertThat(pos).isEqualTo(100f)
        val afterClickLeft = rule.onNodeWithText("HItem0").getUnclippedBoundsInRoot().left
        assertThat(initialLeft.value - afterClickLeft.value).isWithin(2f).of(100f)
    }

    @Test
    fun featureSet_marksEmbeddedDocOpcodesAsBuiltIn() {
        val experimental = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val unsupported = RcPlayerFeatureSet.Empty.unsupportedOpcodes(experimental)

        assertThat(Operations.LAYOUT_RC_DOC in unsupported).isFalse()
        assertThat(Operations.MAP_RC_DOC_VARIABLES in unsupported).isFalse()
        assertThat(Operations.BINARY_DATA in unsupported).isFalse()
    }

    @Test
    fun hostClick_doesNotRevertReverseMappedInlineVariable() {
        val childBytes =
            buildBytes(width = 160, height = 80) {
                val childScore = addNamedFloat("score", 10f)
                root {
                    box(
                        RecordingModifier()
                            .size(160, 80)
                            .onClick(ValueFloatChange(Utils.idFromNan(childScore), 99f)),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        addCoreText("BumpChild")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 200) {
                val hostScore = addNamedFloat("hostScore", 0f)
                val hostOther = addNamedFloat("hostOther", 0f)
                val blobId = addEmbeddedDocument(childBytes)
                root {
                    column(
                        RecordingModifier().size(200, 200),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        val docId = rcDoc(blobId, RecordingModifier().size(160, 80))
                        mapRcDocVariables(
                            docId,
                            MapRcDocVariables.MAP_REVERSE,
                            Utils.idFromNan(hostScore),
                            "score",
                        )
                        box(
                            RecordingModifier()
                                .size(160, 80)
                                .onClick(ValueFloatChange(Utils.idFromNan(hostOther), 1f)),
                            BoxLayout.CENTER,
                            BoxLayout.CENTER,
                        ) {
                            addCoreText("HostClick")
                        }
                    }
                }
            }

        val state = RcPlayerState(hostDoc, defaultPrefix = null)
        val hostScore by state.floatState("hostScore")

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = state) } }
        rule.waitForIdle()
        assertThat(hostScore).isEqualTo(10f)

        rule.onNodeWithText("BumpChild").performClick()
        rule.waitForIdle()
        assertThat(hostScore).isEqualTo(99f)

        // Clicking an unrelated host control invokes updateVariablesWithoutRcDoc; it must not
        // parse a phantom child document or overwrite hostScore back to 10f.
        rule.onNodeWithText("HostClick").performClick()
        rule.waitForIdle()
        assertThat(hostScore).isEqualTo(99f)
    }

    @Test
    fun measureProbe_skipsVariableSyncAndDoesNotOverwriteHost() {
        val childBytes =
            buildBytes(width = 120, height = 60) {
                addNamedFloat("score", 77f)
                root {
                    box(RecordingModifier().size(120, 60), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("ProbeChild")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 100) {
                val hostScore = addNamedFloat("hostScore", 5f)
                val blobId = addEmbeddedDocument(childBytes)
                root {
                    val docId = rcDoc(blobId, RecordingModifier().size(120, 60))
                    mapRcDocVariables(
                        docId,
                        MapRcDocVariables.MAP_REVERSE,
                        Utils.idFromNan(hostScore),
                        "score",
                    )
                }
            }

        val state = RcPlayerState(hostDoc, defaultPrefix = null)
        val hostScore by state.floatState("hostScore")

        rule.setContent {
            CompositionLocalProvider(LocalRcIsMeasureProbe provides true) {
                Box(modifier = Modifier.size(200.dp, 100.dp)) { RcPlayer(state = state) }
            }
        }
        rule.waitForIdle()
        assertThat(hostScore).isEqualTo(5f)
    }

    @Test
    fun urlSwitch_cancellingSlowFetchDoesNotClobberFastResolution() {
        val childBBytes =
            buildBytes(width = 160, height = 80) {
                root {
                    box(RecordingModifier().size(160, 80), BoxLayout.CENTER, BoxLayout.CENTER) {
                        addCoreText("FastChildB")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 100) {
                val urlId = addNamedString("docUrl", "rc://slow-a")
                root { rcDoc(urlId, RecordingModifier().size(160, 80)) }
            }

        val state = RcPlayerState(hostDoc, defaultPrefix = null)
        var docUrl by state.stringState("docUrl")

        val resolver = RcDocumentResolver { url ->
            when (url) {
                "rc://slow-a" -> awaitCancellation()
                "rc://fast-b" -> childBBytes
                else -> null
            }
        }

        rule.setContent {
            Box(modifier = Modifier.size(200.dp, 100.dp)) {
                RcPlayer(state = state, documentResolver = resolver)
            }
        }
        rule.waitForIdle()

        // Switch URL while "rc://slow-a" is still suspended; cancelling it must not clobber B.
        docUrl = "rc://fast-b"
        rule.waitForIdle()
        rule.onNodeWithText("FastChildB").assertIsDisplayed()
    }

    @Test
    fun scrollOverride_outOfBoundsClampsPositionVariable() {
        val doc =
            buildWriterDocument(width = 200, height = 200) {
                val pos = addNamedFloat("pos", 0f)
                root {
                    column(
                        RecordingModifier().size(200, 100).verticalScroll(pos),
                        ColumnLayout.START,
                        ColumnLayout.TOP,
                    ) {
                        for (i in 0 until 5) {
                            box(
                                RecordingModifier().size(200, 100),
                                BoxLayout.CENTER,
                                BoxLayout.CENTER,
                            ) {
                                addCoreText("ClampItem$i")
                            }
                        }
                    }
                }
            }

        val state = RcPlayerState(doc, defaultPrefix = null)
        var pos by state.floatState("pos")

        rule.setContent { Box(modifier = Modifier.size(200.dp)) { RcPlayer(state = state) } }
        rule.waitForIdle()
        assertThat(pos).isEqualTo(0f)

        // Negative override should clamp back to 0f.
        pos = -100f
        rule.waitForIdle()
        assertThat(pos).isEqualTo(0f)

        // Past-end override should clamp back to maxValue (400f for 5x100 in 100 viewport).
        pos = 999f
        rule.waitForIdle()
        assertThat(pos).isEqualTo(400f)
    }

    @Test
    fun reverseColorMapping_continuesUpdatingAfterHostColorOverride() {
        val childBytes =
            buildBytes(width = 160, height = 80) {
                val progress = addNamedFloat("progress", 0f)
                val childColor =
                    addColorExpression(AndroidColor.BLUE, AndroidColor.GREEN, progress).toInt()
                setColorName(childColor, "accent")
                root {
                    box(
                        RecordingModifier()
                            .size(160, 80)
                            .onClick(ValueFloatChange(Utils.idFromNan(progress), 1f)),
                        BoxLayout.CENTER,
                        BoxLayout.CENTER,
                    ) {
                        addCoreText("TurnGreen")
                    }
                }
            }

        val hostDoc =
            buildWriterDocument(width = 200, height = 100) {
                val hostColor = addNamedColor("hostAccent", AndroidColor.RED)
                val blobId = addEmbeddedDocument(childBytes)
                root {
                    val docId = rcDoc(blobId, RecordingModifier().size(160, 80))
                    mapRcDocVariables(docId, MapRcDocVariables.MAP_REVERSE, hostColor, "accent")
                }
            }

        val state = RcPlayerState(hostDoc, defaultPrefix = null)
        var hostAccent by state.colorState("hostAccent")

        rule.setContent {
            Box(modifier = Modifier.size(200.dp, 100.dp)) { RcPlayer(state = state) }
        }
        rule.waitForIdle()
        assertThat(hostAccent).isEqualTo(Color(AndroidColor.BLUE))

        // Host writes an override (setting mColorOverride on hostAccent).
        hostAccent = Color(AndroidColor.MAGENTA)
        rule.waitForIdle()
        assertThat(hostAccent).isEqualTo(Color(AndroidColor.MAGENTA))

        // Child updates the mapped color; reverse sync must still propagate through overrideColor.
        rule.onNodeWithText("TurnGreen").performClick()
        rule.waitForIdle()
        assertThat(hostAccent).isEqualTo(Color(AndroidColor.GREEN))
    }
}
