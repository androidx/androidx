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

package androidx.compose.remote.creation.compose.modifier

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsibleRowLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.Custom
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import androidx.compose.remote.core.operations.layout.managers.FlowLayout
import androidx.compose.remote.core.operations.layout.managers.ImageLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.managers.StateLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.RemoteComposeCaptureState
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAllStatesLayout
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleColumn
import androidx.compose.remote.creation.compose.layout.RemoteCollapsibleRow
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.layout.RemoteFitBox
import androidx.compose.remote.creation.compose.layout.RemoteFlowRow
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.modifiers.PaddingModifier as CorePaddingModifier
import androidx.compose.remote.creation.modifiers.WidthModifier as CoreWidthModifier
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.platform.RCLogger
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class ComponentIdModifierTest {

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val loggedMessages = mutableListOf<Pair<RcPlatformServices.LogCategory, String>>()
    private lateinit var creationState: RemoteComposeCaptureState

    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    private val experimentalProfile: Profile
        get() =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                AndroidxRcPlatformServices(),
            ) { creationDisplayInfo, profile, callback ->
                RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
            }

    @Before
    fun setUp() {
        loggedMessages.clear()
        val fakeBuffer = TestRemoteComposeBuffer()
        val platform =
            AndroidxRcPlatformServices(
                object : RCLogger {
                    override fun log(category: RcPlatformServices.LogCategory, message: String) {
                        loggedMessages.add(category to message)
                    }
                }
            )
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, RcProfiles.PROFILE_ANDROIDX, platform) {
                creationDisplayInfo,
                profile,
                _ ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
                )
            }
        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)
    }

    @Test
    fun standaloneComponentId_setsRecordingModifierIdWithoutWireElements() {
        val modifier = RemoteModifier.componentId(42)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.componentId).isEqualTo(42)
        assertThat(recordingModifier.list).isEmpty()
        assertThat(loggedMessages).isEmpty()
    }

    @Test
    fun toRecordingModifierElement_throwsUnsupportedOperationException() {
        val element = ComponentIdModifier(42)
        assertFailsWith<UnsupportedOperationException> {
            with(element) { creationState.toRecordingModifierElement() }
        }
    }

    @Test
    fun minimumPositiveComponentId_isAllowed() {
        val modifier = RemoteModifier.componentId(1)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.componentId).isEqualTo(1)
        assertThat(recordingModifier.list).isEmpty()
    }

    @Test
    fun nonPositiveComponentIds_throwIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> { RemoteModifier.componentId(0) }
        assertFailsWith<IllegalArgumentException> { RemoteModifier.componentId(-1) }
        assertFailsWith<IllegalArgumentException> { RemoteModifier.componentId(-2) }
        assertFailsWith<IllegalArgumentException> { RemoteModifier.componentId(Int.MIN_VALUE) }
    }

    @Test
    fun componentIdChainedWithOtherModifiers_preservesIdAndElements() {
        val modifier = RemoteModifier.padding(8.rdp).componentId(101).width(50.rdp)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.componentId).isEqualTo(101)
        assertThat(recordingModifier.list).hasSize(2)
        assertIs<CorePaddingModifier>(recordingModifier.list[0])
        assertIs<CoreWidthModifier>(recordingModifier.list[1])
        assertThat(loggedMessages).isEmpty()
    }

    @Test
    fun multipleComponentIds_firstOneWinsAndLogsWarning() {
        val modifier = RemoteModifier.componentId(10).padding(4.rdp).componentId(20)
        val recordingModifier = creationState.toRecordingModifier(modifier)

        assertThat(recordingModifier.componentId).isEqualTo(10)
        assertThat(recordingModifier.list).hasSize(1)
        assertIs<CorePaddingModifier>(recordingModifier.list[0])
        assertThat(loggedMessages)
            .containsExactly(
                RcPlatformServices.LogCategory.WARN to
                    "Ignoring duplicate ComponentIdModifier (ComponentIdModifier(id=20)); " +
                        "only the first instance is applied"
            )

        loggedMessages.clear()
        val minFirstModifier = RemoteModifier.componentId(1).componentId(20)
        assertThat(creationState.toRecordingModifier(minFirstModifier).componentId).isEqualTo(1)
        assertThat(loggedMessages)
            .containsExactly(
                RcPlatformServices.LogCategory.WARN to
                    "Ignoring duplicate ComponentIdModifier (ComponentIdModifier(id=20)); " +
                        "only the first instance is applied"
            )
    }

    @Test
    fun componentId_appliedToAllStandardComponents() = runTest {
        val bitmap =
            RemoteImageBitmap(Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).asImageBitmap())

        val doc =
            captureRule.captureDocument(context) {
                RemoteColumn(modifier = RemoteModifier.componentId(100)) {
                    RemoteBox(modifier = RemoteModifier.componentId(101)) {
                        RemoteText("Hello".rs, modifier = RemoteModifier.componentId(102))
                    }
                    RemoteBox(modifier = RemoteModifier.componentId(103))
                    RemoteRow(modifier = RemoteModifier.componentId(104)) {
                        RemoteImage(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = RemoteModifier.componentId(105),
                        )
                        RemoteCanvas(modifier = RemoteModifier.componentId(106).size(20.rdp)) {}
                    }
                    RemoteFitBox(modifier = RemoteModifier.componentId(107)) {
                        RemoteBox(modifier = RemoteModifier.size(10.rdp))
                    }
                    RemoteCollapsibleColumn(modifier = RemoteModifier.componentId(108)) {
                        RemoteBox(modifier = RemoteModifier.size(10.rdp))
                    }
                    RemoteCollapsibleRow(modifier = RemoteModifier.componentId(109)) {
                        RemoteBox(modifier = RemoteModifier.size(10.rdp))
                    }
                    RemoteAllStatesLayout(
                        currentState = 0.ri,
                        stateCount = 2,
                        modifier = RemoteModifier.componentId(110),
                    ) { _ ->
                        RemoteBox(modifier = RemoteModifier.size(10.rdp))
                    }
                }
            }

        assertIs<ColumnLayout>(doc.getComponent(100))
        assertIs<BoxLayout>(doc.getComponent(101))
        assertIs<CoreText>(doc.getComponent(102))
        assertIs<BoxLayout>(doc.getComponent(103))
        assertIs<RowLayout>(doc.getComponent(104))
        assertIs<ImageLayout>(doc.getComponent(105))
        assertIs<CanvasLayout>(doc.getComponent(106))
        assertIs<FitBoxLayout>(doc.getComponent(107))
        assertIs<CollapsibleColumnLayout>(doc.getComponent(108))
        assertIs<CollapsibleRowLayout>(doc.getComponent(109))
        assertIs<StateLayout>(doc.getComponent(110))
    }

    @Test
    fun componentId_minimumPositiveId_appliedInDocument() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.componentId(1).size(20.rdp))
            }

        val box = assertIs<BoxLayout>(doc.getComponent(1))
        assertThat(box.componentId).isEqualTo(1)
    }

    @Test
    fun componentId_multipleChained_firstWinsInDocument() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.componentId(10).componentId(20).size(20.rdp))
            }

        assertIs<BoxLayout>(doc.getComponent(10))
        assertThat(doc.getComponent(20)).isNull()
    }

    @Test
    fun componentId_appliedToExperimentalComponents() = runTest {
        val doc =
            captureRule.captureDocument(context, profile = experimentalProfile) {
                RemoteFlowRow(modifier = RemoteModifier.componentId(300)) {
                    RemoteCustomComponent(
                        name = "test_custom",
                        modifier = RemoteModifier.componentId(301).size(40.rdp),
                    )
                }
            }

        val flow = assertIs<FlowLayout>(doc.getComponent(300))
        assertThat(flow.componentId).isEqualTo(300)
        val custom = assertIs<Custom>(doc.getComponent(301))
        assertThat(custom.componentId).isEqualTo(301)
    }

    @Test
    fun componentId_appliedToCanvasCustomAndStateLayout() = runTest {
        val doc =
            captureRule.captureDocument(context, profile = experimentalProfile) {
                RemoteCanvas(modifier = RemoteModifier.componentId(400).size(100.rdp)) {
                    remoteCanvas.custom(
                        config = "canvas_custom",
                        modifier = RemoteModifier.componentId(401).size(20.rdp),
                    )
                    remoteCanvas.startStateLayout(
                        modifier = RemoteModifier.componentId(402),
                        currentStateId = 0,
                    )
                    remoteCanvas.endStateLayout()
                }
            }

        assertIs<CanvasLayout>(doc.getComponent(400))
        assertIs<Custom>(doc.getComponent(401))
        assertIs<StateLayout>(doc.getComponent(402))
    }

    @Test
    fun withoutComponentId_autoGeneratesNegativeId() = runTest {
        val doc =
            captureRule.captureDocument(context) {
                RemoteBox(modifier = RemoteModifier.size(20.rdp))
            }

        val root = assertNotNull(doc.rootLayoutComponent)
        val box = root.list.filterIsInstance<BoxLayout>().single()
        assertThat(box.componentId).isLessThan(-1)
    }
}
