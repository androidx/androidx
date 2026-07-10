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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialExternalSurface
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.StereoMode
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent
import androidx.xr.scenecore.InteractableComponent
import androidx.xr.scenecore.testing.InteractableComponentTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [InteractionPolicy] class for SpatialPanels. */
@RunWith(AndroidJUnit4::class)
class InteractionPolicyTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun interactionPolicy_noComponentByDefault() {
        composeTestRule.setContent {
            Subspace {
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel"),
                    stereoMode = StereoMode.SideBySide,
                ) {}
            }
        }

        assertTrue(
            composeTestRule
                .onSubspaceNodeWithTag("panel")
                .fetchSemanticsNode()
                .components
                .isNullOrEmpty()
        )
    }

    @Test
    fun interactionPolicy_componentIsNotNullAndOnlyContainsSingleInteractable() {
        composeTestRule.setContent {
            Subspace {
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel"),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = true

                            override fun onInputEvent(event: SpatialInputEvent) {}
                        },
                ) {}
            }
        }

        assertSingleInteractableComponentExist()
    }

    @Test
    fun interactionPolicy_modifierIsDisabledAndComponentDoesNotExist() {
        composeTestRule.setContent {
            Subspace {
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel"),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = false

                            override fun onInputEvent(event: SpatialInputEvent) {}
                        },
                ) {}
            }
        }

        assertInteractableComponentDoesNotExist()
    }

    @Test
    fun interactionPolicy_modifierDoesNotChangeAndOnlyOneComponentExist() {
        composeTestRule.setContent {
            Subspace {
                var panelWidth by remember { mutableStateOf(50.dp) }
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel").width(panelWidth),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = true

                            override fun onInputEvent(event: SpatialInputEvent) {}
                        },
                ) {
                    SpatialPanel {
                        Button(
                            modifier = Modifier.testTag("button"),
                            onClick = { panelWidth += 50.dp },
                        ) {
                            Text(text = "Sample button for testing")
                        }
                    }
                }
            }
        }

        val componentBefore = assertSingleInteractableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose there should still only exist one Component.
        val componentAfter = assertSingleInteractableComponentExist()
        assertEquals(componentBefore, componentAfter)
    }

    @Test
    fun interactionPolicy_modifierEnabledToDisabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var interactableEnabled by remember { mutableStateOf(true) }
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel"),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = interactableEnabled

                            override fun onInputEvent(event: SpatialInputEvent) {}
                        },
                ) {
                    SpatialPanel {
                        Button(
                            modifier = Modifier.testTag("button"),
                            onClick = { interactableEnabled = !interactableEnabled },
                        ) {
                            Text(text = "Sample button for testing")
                        }
                    }
                }
            }
        }

        assertSingleInteractableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After recompose no Components should exist.
        assertInteractableComponentDoesNotExist()
    }

    @Test
    fun interactionPolicy_modifierDisabledThenEnabledAndComponentUpdates() {
        composeTestRule.setContent {
            Subspace {
                var interactableEnabled by remember { mutableStateOf(true) }
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("panel"),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = interactableEnabled

                            override fun onInputEvent(event: SpatialInputEvent) {}
                        },
                ) {
                    SpatialPanel {
                        Button(
                            modifier = Modifier.testTag("button"),
                            onClick = { interactableEnabled = !interactableEnabled },
                        ) {
                            Text(text = "Sample button for testing")
                        }
                    }
                }
            }
        }

        assertSingleInteractableComponentExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After disabled, recompose Component should not exist.
        assertInteractableComponentDoesNotExist()
        composeTestRule.onNodeWithTag("button").performClick()
        // After enabled, recompose Component should be attached.
        assertSingleInteractableComponentExist()
    }

    @Test
    fun interactionPolicy_touchDownOnChild_ignoresEventsUntilTouchUp() {
        composeTestRule.configureFakeSession()
        val events = mutableListOf<SpatialInputEvent>()
        composeTestRule.setContent {
            Subspace {
                SpatialExternalSurface(
                    modifier = SubspaceModifier.testTag("surface"),
                    stereoMode = StereoMode.SideBySide,
                    interactionPolicy =
                        object : InteractionPolicy {
                            override val isEnabled = true

                            override fun onInputEvent(event: SpatialInputEvent) {
                                events.add(event)
                            }
                        },
                ) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("panel")) {}
                }
            }
        }

        val surfaceNode = composeTestRule.onSubspaceNodeWithTag("surface").fetchSemanticsNode()
        val component =
            requireNotNull(surfaceNode.components).first { it is InteractableComponent }
                as InteractableComponent
        val tester = SceneCoreTestRule().createTester<InteractableComponentTester>(component)
        val surfaceEntity = checkNotNull(surfaceNode.semanticsEntity)
        val childEntity =
            checkNotNull(
                composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().semanticsEntity
            )

        // 1. Touch DOWN on childEntity -> should be ignored
        val downOnChild =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                100L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.DOWN,
                listOf(InputEvent.HitInfo(childEntity, Vector3.One, Matrix4.Identity)),
            )
        tester.triggerOnInputEvent(downOnChild)
        composeTestRule.waitForIdle()

        // 2. Touch MOVE over surfaceEntity -> should be ignored because touch DOWN was on
        // childEntity
        val moveOverSurface =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                101L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.MOVE,
                listOf(InputEvent.HitInfo(surfaceEntity, Vector3.One, Matrix4.Identity)),
            )
        tester.triggerOnInputEvent(moveOverSurface)
        composeTestRule.waitForIdle()

        // 3. Touch UP on surfaceEntity -> should be ignored, resetting ignore state
        val upOnSurface =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                102L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.UP,
                listOf(InputEvent.HitInfo(surfaceEntity, Vector3.One, Matrix4.Identity)),
            )
        tester.triggerOnInputEvent(upOnSurface)
        composeTestRule.waitForIdle()

        assertTrue(events.isEmpty())

        // 4. Touch DOWN on surfaceEntity -> should NOT be ignored
        val downOnSurface =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                200L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.DOWN,
                listOf(InputEvent.HitInfo(surfaceEntity, Vector3.One, Matrix4.Identity)),
            )
        tester.triggerOnInputEvent(downOnSurface)
        composeTestRule.waitForIdle()

        assertEquals(1, events.size)
        assertEquals(InputEvent.Action.DOWN, events[0].action)

        // 5. Touch UP on surfaceEntity -> should NOT be ignored
        val upOnSurface2 =
            InputEvent(
                InputEvent.Source.HANDS,
                InputEvent.Pointer.RIGHT,
                201L,
                Vector3.Zero,
                Vector3.One,
                InputEvent.Action.UP,
                listOf(InputEvent.HitInfo(surfaceEntity, Vector3.One, Matrix4.Identity)),
            )
        tester.triggerOnInputEvent(upOnSurface2)
        composeTestRule.waitForIdle()

        assertEquals(2, events.size)
        assertEquals(InputEvent.Action.UP, events[1].action)
    }

    private fun assertSingleInteractableComponentExist(
        testTag: String = "panel"
    ): InteractableComponent {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(1, components.size)
        assertIs<InteractableComponent>(components[0])
        return components[0] as InteractableComponent
    }

    private fun assertInteractableComponentDoesNotExist(testTag: String = "panel") {
        val components =
            composeTestRule.onSubspaceNodeWithTag(testTag).fetchSemanticsNode().components
        assertNotNull(components)
        assertEquals(0, components.size)
    }
}
