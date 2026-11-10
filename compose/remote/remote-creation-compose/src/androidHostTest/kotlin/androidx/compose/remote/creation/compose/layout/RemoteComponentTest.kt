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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.ComponentValue
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.managers.CanvasLayout
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.actions.Action as CreationAction
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.RemoteComposeCaptureState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class RemoteComponentTest {
    private val fakeBuffer = TestRemoteComposeBuffer()
    private lateinit var creationState: RemoteComposeCaptureState
    private lateinit var remoteCanvas: RemoteCanvas
    private lateinit var drawScope: RemoteDrawScope

    @Before
    fun setUp() {
        val platform = AndroidxRcPlatformServices()
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
            RemoteComposeCaptureState(
                RemoteCreationDisplayInfo(100, 50, 160, 1f),
                null,
                profile,
            )
        remoteCanvas = RemoteCanvas(creationState)
        drawScope = RemoteDrawScope(remoteCanvas)
    }

    @Test
    fun componentOffsetProperties_emitComponentValuesAndCacheKeys() {
        val component = drawScope.remote.component
        val componentId = creationState.document.buffer.lastComponentId

        val posInParent = component.positionInParent
        val posInRoot = component.positionInRoot

        assertThat(posInParent.x.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "x"))
        assertThat(posInParent.y.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "y"))
        assertThat(posInRoot.x.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "rootX"))
        assertThat(posInRoot.y.cacheKey).isEqualTo(RemoteComponentCacheKey(componentId, "rootY"))

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addComponentValue(42, ${ComponentValue.POS_X})",
                "addComponentValue(43, ${ComponentValue.POS_Y})",
                "addComponentValue(44, ${ComponentValue.POS_ROOT_X})",
                "addComponentValue(45, ${ComponentValue.POS_ROOT_Y})",
            )
            .inOrder()
    }

    @Test
    fun remoteCanvasNode_afterCustomComponent_bindsScopeComponentValueToCanvasComponentId() {
        val platform = AndroidxRcPlatformServices()
        val experimentalProfile =
            Profile(
                CoreDocument.DOCUMENT_API_LEVEL,
                RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
                platform,
            ) { creationDisplayInfo, profile, callback ->
                RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
            }
        val expState =
            RemoteComposeCaptureState(
                RemoteCreationDisplayInfo(200, 200, 160, 1f),
                null,
                experimentalProfile,
            )
        val expCanvas = RemoteCanvas(expState)
        var capturedWidth: Float = 0f

        val rootNode =
            RemoteRootNode().apply {
                val canvasNode =
                    RemoteCanvasNode().apply {
                        modifier = RemoteModifier.size(86.rdp, 52.rdp)
                        onDraw = {
                            remoteCanvas.custom(
                                "decoy_custom",
                                modifier = RemoteModifier.size(12.rdp),
                            )
                            val comp = remoteComponent()
                            capturedWidth = comp.width.getFloatIdForCreationState(expState)
                            remoteCanvas.drawRect(0f.rf, 0f.rf, comp.width, comp.height, null)
                        }
                    }
                children.add(canvasNode)
            }
        rootNode.render(expState, expCanvas)

        assertThat(capturedWidth.isNaN()).isTrue()
        val coreDoc =
            CoreDocument().apply {
                initFromBuffer(
                    RemoteComposeBuffer.fromInputStream(
                        ByteArrayInputStream(
                            expState.document.buffer(),
                            0,
                            expState.document.bufferSize(),
                        )
                    )
                )
            }
        val rootLayout = checkNotNull(coreDoc.rootLayoutComponent)
        val canvasLayout = rootLayout.list.filterIsInstance<CanvasLayout>().single()
        val canvasContentOps = (canvasLayout as LayoutComponent).list
        val componentValues = canvasContentOps.filterIsInstance<ComponentValue>()
        assertThat(componentValues).isNotEmpty()
        for (cv in componentValues) {
            assertThat(cv.componentId).isEqualTo(canvasLayout.componentId)
        }
    }

    @Test
    fun remoteCanvasNode_modifierAndOnDraw_shareSingleComponentTokenAndVariableIds() {
        var modifierWidthNan = 0f
        var modifierHeightNan = 0f
        var drawWidthNan = 0f
        var drawHeightNan = 0f

        val rootNode =
            RemoteRootNode().apply {
                val canvasNode =
                    RemoteCanvasNode().apply {
                        modifier =
                            RemoteModifier.size(86.rdp, 52.rdp)
                                .clip(RemoteRoundedCornerShape(percent = 50))
                                .clickable(
                                    object : RemoteAction {
                                        override fun RemoteStateScope.toRemoteAction():
                                            CreationAction {
                                            val comp = remoteComponent()
                                            modifierWidthNan =
                                                comp.width.getFloatIdForCreationState(creationState)
                                            modifierHeightNan =
                                                comp.height.getFloatIdForCreationState(
                                                    creationState
                                                )
                                            return hostAction("w".rs, comp.width).run {
                                                toRemoteAction()
                                            }
                                        }
                                    }
                                )
                        onDraw = {
                            val comp = remoteComponent()
                            drawWidthNan = comp.width.getFloatIdForCreationState(creationState)
                            drawHeightNan = comp.height.getFloatIdForCreationState(creationState)
                            remoteCanvas.drawRect(0f.rf, 0f.rf, comp.width, comp.height, null)
                        }
                    }
                children.add(canvasNode)
            }
        rootNode.render(creationState, remoteCanvas)

        assertThat(modifierWidthNan.isNaN()).isTrue()
        assertThat(modifierHeightNan.isNaN()).isTrue()
        assertThat(Utils.idFromNan(drawWidthNan)).isEqualTo(Utils.idFromNan(modifierWidthNan))
        assertThat(Utils.idFromNan(drawHeightNan)).isEqualTo(Utils.idFromNan(modifierHeightNan))

        val widthValueCalls = fakeBuffer.calls.filter { it.endsWith(", ${ComponentValue.WIDTH})") }
        val heightValueCalls =
            fakeBuffer.calls.filter { it.endsWith(", ${ComponentValue.HEIGHT})") }
        assertThat(widthValueCalls)
            .containsExactly(
                "addComponentValue(${Utils.idFromNan(modifierWidthNan)}, ${ComponentValue.WIDTH})"
            )
        assertThat(heightValueCalls)
            .containsExactly(
                "addComponentValue(${Utils.idFromNan(modifierHeightNan)}, ${ComponentValue.HEIGHT})"
            )
    }

    @Test
    fun remoteComponent_equalsAndHashCode_consistentAcrossRepeatedScopeCalls() {
        val fallback1 = drawScope.remoteComponent()
        val fallback2 = drawScope.remoteComponent()
        assertThat(fallback1).isEqualTo(fallback2)
        assertThat(fallback1.hashCode()).isEqualTo(fallback2.hashCode())

        lateinit var actionComp1: RemoteComponent
        lateinit var actionComp2: RemoteComponent
        lateinit var drawComp1: RemoteComponent
        lateinit var drawComp2: RemoteComponent
        lateinit var siblingComp: RemoteComponent

        val rootNode =
            RemoteRootNode().apply {
                val canvasNode =
                    RemoteCanvasNode().apply {
                        modifier =
                            RemoteModifier.size(86.rdp, 52.rdp)
                                .clickable(
                                    object : RemoteAction {
                                        override fun RemoteStateScope.toRemoteAction():
                                            CreationAction {
                                            actionComp1 = remoteComponent()
                                            actionComp2 = remoteComponent()
                                            return hostAction("w".rs, actionComp1.width).run {
                                                toRemoteAction()
                                            }
                                        }
                                    }
                                )
                        onDraw = {
                            drawComp1 = remoteComponent()
                            drawComp2 = remoteComponent()
                            remoteCanvas.drawRect(
                                0f.rf,
                                0f.rf,
                                drawComp1.width,
                                drawComp2.height,
                                null,
                            )
                        }
                    }
                val siblingCanvasNode =
                    RemoteCanvasNode().apply {
                        modifier = RemoteModifier.size(40.rdp, 40.rdp)
                        onDraw = { siblingComp = remoteComponent() }
                    }
                children.add(canvasNode)
                children.add(siblingCanvasNode)
            }
        rootNode.render(creationState, remoteCanvas)

        assertThat(actionComp1).isEqualTo(actionComp2)
        assertThat(actionComp1.hashCode()).isEqualTo(actionComp2.hashCode())
        assertThat(drawComp1).isEqualTo(drawComp2)
        assertThat(drawComp1.hashCode()).isEqualTo(drawComp2.hashCode())
        assertThat(actionComp1).isEqualTo(drawComp1)
        assertThat(actionComp1.hashCode()).isEqualTo(drawComp1.hashCode())
        assertThat(actionComp1).isNotEqualTo(siblingComp)
        assertThat(actionComp1).isNotEqualTo(fallback1)
    }
}
