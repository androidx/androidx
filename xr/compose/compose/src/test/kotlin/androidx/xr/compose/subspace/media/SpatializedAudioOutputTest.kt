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

package androidx.xr.compose.subspace.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.scenecore.PointSourceParams
import androidx.xr.scenecore.PositionalAudioComponent
import androidx.xr.scenecore.SoundEffectPool
import androidx.xr.scenecore.SoundEffectPoolComponent
import androidx.xr.scenecore.SoundFieldAttributes
import androidx.xr.scenecore.SoundFieldAudioComponent
import androidx.xr.scenecore.SpatializerConstants.AmbisonicsOrder
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpatializedAudioOutputTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatializedAudioOutput_pointSource_attachesComponent() {
        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    PointSourceExoplayerAudioOutput(session, PointSourceParams())
                }
                SpatialPanel(
                    modifier = SubspaceModifier.testTag("panel").spatializedAudioOutput(audioOutput)
                ) {}
            }
        }

        val components =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<PositionalAudioComponent>(components[0])
    }

    @Test
    fun spatializedAudioOutput_soundField_attachesComponent() {
        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    SoundFieldExoplayerAudioOutput(
                        session,
                        SoundFieldAttributes(AmbisonicsOrder.FIRST_ORDER),
                    )
                }
                SpatialPanel(
                    modifier = SubspaceModifier.testTag("panel").spatializedAudioOutput(audioOutput)
                ) {}
            }
        }

        val components =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<SoundFieldAudioComponent>(components[0])
    }

    @Test
    fun spatializedAudioOutput_soundEffect_attachesComponent() {
        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val soundEffectPool = remember { SoundEffectPool.create(session, 1) }
                val soundEffectPoolComponent = remember {
                    SoundEffectPoolComponent.create(session, soundEffectPool, PointSourceParams())
                }
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .spatializedAudioOutput(
                                soundEffectPoolComponent.asSpatializedAudioOutput()
                            )
                ) {}
            }
        }

        val components =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<SoundEffectPoolComponent>(components[0])
    }

    @Test
    fun spatializedAudioOutput_sharedOutput_detachesFromAll() {
        var showSecondPanel by mutableStateOf(false)

        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    PointSourceExoplayerAudioOutput(session, PointSourceParams())
                }

                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel1").spatializedAudioOutput(audioOutput)
                ) {}

                if (showSecondPanel) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("panel2").spatializedAudioOutput(audioOutput)
                    ) {}
                }
            }
        }

        // Initially attached to panel 1
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )

        // Show second panel, should detach from all
        showSecondPanel = true
        composeTestRule.waitForIdle()
        assertEquals(
            0,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )
        assertEquals(
            0,
            composeTestRule.onSubspaceNodeWithTag("panel2").fetchSemanticsNode().components?.size,
        )

        // Hide second panel, should re-attach to panel 1
        showSecondPanel = false
        composeTestRule.waitForIdle()
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )
    }

    @Test
    fun spatializedAudioOutput_recompositionWithUnrememberedWrapper_keepsComponentAttached() {
        var recomposeTrigger by mutableStateOf(0)
        var firstWrapper: SpatializedAudioOutput? = null
        var secondWrapper: SpatializedAudioOutput? = null

        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val soundEffectPool = remember { SoundEffectPool.create(session, 1) }
                val soundEffectPoolComponent =
                    remember(soundEffectPool) {
                        SoundEffectPoolComponent.create(
                            session,
                            soundEffectPool,
                            PointSourceParams(),
                        )
                    }

                val trigger = recomposeTrigger
                val audioOutput = soundEffectPoolComponent.asSpatializedAudioOutput()
                if (trigger == 0) {
                    firstWrapper = audioOutput
                } else {
                    secondWrapper = audioOutput
                }

                SpatialPanel(
                    modifier = SubspaceModifier.testTag("panel").spatializedAudioOutput(audioOutput)
                ) {}
            }
        }

        // Verify initial state: component is attached
        val componentsBefore =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(componentsBefore)
        assertEquals(1, componentsBefore.size)
        assertIs<SoundEffectPoolComponent>(componentsBefore[0])
        assertNotNull(firstWrapper)

        // Trigger recomposition
        recomposeTrigger++
        composeTestRule.waitForIdle()

        // Verify post-recomposition state, component still remains attached
        val componentsAfter =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(componentsAfter)
        assertEquals(1, componentsAfter.size)
        assertIs<SoundEffectPoolComponent>(componentsAfter[0])

        // Verify equality between first and second components/wrappers.
        assertEquals(componentsBefore[0], componentsAfter[0])
        assertNotNull(secondWrapper)
        assertEquals(firstWrapper, secondWrapper)
        assertEquals(firstWrapper.hashCode(), secondWrapper.hashCode())
    }

    @Test
    fun spatializedAudioOutput_backToBackChain_keepsComponentAttached() {
        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    PointSourceExoplayerAudioOutput(session, PointSourceParams())
                }
                SpatialPanel(
                    modifier =
                        SubspaceModifier.testTag("panel")
                            .spatializedAudioOutput(audioOutput)
                            .spatializedAudioOutput(audioOutput)
                ) {}
            }
        }

        val components =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<PositionalAudioComponent>(components[0])
    }

    @Test
    fun spatializedAudioOutput_sharedBackToBackChain_detachesFromAll() {
        var showSecondPanel by mutableStateOf(false)

        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    PointSourceExoplayerAudioOutput(session, PointSourceParams())
                }
                val chainedModifier =
                    SubspaceModifier.spatializedAudioOutput(audioOutput)
                        .spatializedAudioOutput(audioOutput)

                SpatialPanel(modifier = SubspaceModifier.testTag("panel1").then(chainedModifier)) {}

                if (showSecondPanel) {
                    SpatialPanel(
                        modifier = SubspaceModifier.testTag("panel2").then(chainedModifier)
                    ) {}
                }
            }
        }

        // Initially attached to panel 1
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )

        // Show second panel, should detach from all
        showSecondPanel = true
        composeTestRule.waitForIdle()
        assertEquals(
            0,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )
        assertEquals(
            0,
            composeTestRule.onSubspaceNodeWithTag("panel2").fetchSemanticsNode().components?.size,
        )

        // Hide second panel, should re-attach to panel 1
        showSecondPanel = false
        composeTestRule.waitForIdle()
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel1").fetchSemanticsNode().components?.size,
        )
    }

    @Test
    fun spatializedAudioOutput_backToBackChain_removeOneModifier_keepsComponentAttached() {
        var attachTwoModifiers by mutableStateOf(true)

        composeTestRule.setContent {
            Subspace {
                val session = LocalSession.current!!
                val audioOutput = remember {
                    PointSourceExoplayerAudioOutput(session, PointSourceParams())
                }
                var modifier = SubspaceModifier.spatializedAudioOutput(audioOutput)
                if (attachTwoModifiers) {
                    modifier = modifier.spatializedAudioOutput(audioOutput)
                }
                SpatialPanel(modifier = SubspaceModifier.testTag("panel").then(modifier)) {}
            }
        }

        // Verify initial state: component is attached
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components?.size,
        )

        // Remove one modifier
        attachTwoModifiers = false
        composeTestRule.waitForIdle()

        // Verify component remains attached
        assertEquals(
            1,
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().components?.size,
        )
    }
}
