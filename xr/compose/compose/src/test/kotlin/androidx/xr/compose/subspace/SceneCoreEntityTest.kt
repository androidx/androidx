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

package androidx.xr.compose.subspace

import android.view.View
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.PanelEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SceneCoreEntity]. */
@RunWith(AndroidJUnit4::class)
class SceneCoreEntityTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun sceneCoreEntity_childrenAreComposed() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(factory = { GroupEntity.create(session, "TestEntity") }) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel1").assertExists()
        composeTestRule.onSubspaceNodeWithTag("panel2").assertExists()
    }

    @Test
    fun sceneCoreEntity_childrenAreCentered() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(factory = { GroupEntity.create(session, "TestEntity") }) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun sceneCoreEntity_size_modifierSizeIsAppliedToEntity() {
        var testEntity by mutableStateOf<PanelEntity?>(null)
        var targetSize by mutableStateOf(500.dp)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    testEntity = remember {
                        PanelEntity.create(
                            session,
                            View(composeTestRule.activity),
                            IntSize2d(100, 100),
                            "TestPanel",
                        )
                    }
                    SceneCoreEntity(
                        factory = { testEntity!! },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                }
                            ),
                        modifier = SubspaceModifier.size(targetSize).testTag("mainPanel"),
                    )
                }
            }
        }

        // Subsequent tests assume that density is 1.0
        assertThat(composeTestRule.density.density).isEqualTo(1.0f)
        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(500.dp)
            .assertHeightIsEqualTo(500.dp)
        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("mainPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .sizeInPixels
            )
            .isEqualTo(IntSize2d(500, 500))

        targetSize = 1000.dp

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(1000.dp)
            .assertHeightIsEqualTo(1000.dp)
        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("mainPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .sizeInPixels
            )
            .isEqualTo(IntSize2d(1000, 1000))
    }

    @Test
    fun sceneCoreEntity_size_modifierSizeChangesWithDensity() {
        var testEntity by mutableStateOf<PanelEntity?>(null)
        var targetSize by mutableStateOf(500.dp)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2.0f)) {
                TestSetup {
                    Subspace {
                        val session = checkNotNull(LocalSession.current)
                        testEntity = remember {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        }
                        SceneCoreEntity(
                            factory = { testEntity!! },
                            sizeAdapter =
                                SceneCoreEntitySizeAdapter({
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                }),
                            modifier = SubspaceModifier.size(targetSize).testTag("mainPanel"),
                        )
                    }
                }
            }
        }

        // Subsequent tests assume that density is 2.0
        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(1000.dp)
            .assertHeightIsEqualTo(1000.dp)
        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("mainPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .sizeInPixels
            )
            .isEqualTo(IntSize2d(1000, 1000))

        targetSize = 1000.dp

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(2000.dp)
            .assertHeightIsEqualTo(2000.dp)
        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("mainPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .sizeInPixels
            )
            .isEqualTo(IntSize2d(2000, 2000))
    }

    @Test
    fun sceneCoreEntity_size_usesInitialSizeIfNoModifierOrChildren() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                },
                                intrinsicSize = {
                                    IntVolumeSize(sizeInPixels.width, sizeInPixels.height, 0)
                                },
                            ),
                        modifier = SubspaceModifier.testTag("mainPanel"),
                    )
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun sceneCoreEntity_size_isZeroIfNoModifierOrChildrenOrIntrinsicSize() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                }
                            ),
                        modifier = SubspaceModifier.testTag("mainPanel"),
                    )
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun sceneCoreEntity_size_usesInitialSizeIfChildrenAreSmaller() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                },
                                intrinsicSize = {
                                    IntVolumeSize(sizeInPixels.width, sizeInPixels.height, 0)
                                },
                            ),
                        modifier = SubspaceModifier.testTag("mainPanel"),
                    ) {
                        SpatialPanel(SubspaceModifier.size(50.dp)) {}
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun sceneCoreEntity_size_doesNotThrowExceptionIfSetterAndGetter() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(0, 0),
                                "TestPanel",
                            )
                        },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                },
                                intrinsicSize = {
                                    IntVolumeSize(sizeInPixels.width, sizeInPixels.height, 0)
                                },
                            ),
                        modifier = SubspaceModifier.testTag("mainPanel"),
                    )
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("mainPanel").assertExists()
    }

    @Test
    fun sceneCoreEntity_size_matchesSizeOfChildrenIfLarger() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    val session = checkNotNull(LocalSession.current)
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        sizeAdapter =
                            SceneCoreEntitySizeAdapter(
                                onLayoutSizeChanged = {
                                    sizeInPixels = IntSize2d(it.width, it.height)
                                },
                                intrinsicSize = {
                                    IntVolumeSize(sizeInPixels.width, sizeInPixels.height, 0)
                                },
                            ),
                        modifier = SubspaceModifier.testTag("mainPanel"),
                    ) {
                        SpatialPanel(SubspaceModifier.size(200.dp)) {}
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("mainPanel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)
    }

    @Ignore // b/427806050
    @Test
    fun sceneCoreEntity_factoryAndUpdate_areCalledTheAppropriateNumberOfTimes() {
        var factoryCalled = 0
        var updateCalled = 0
        val cornerRadius = mutableStateOf(0.5f)

        composeTestRule.setContent {
            TestSetup {
                val session = LocalSession.current ?: error("No session")
                Subspace {
                    SceneCoreEntity(
                        factory = {
                            factoryCalled += 1
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        update = {
                            updateCalled += 1
                            it.cornerRadius = cornerRadius.value
                        },
                    ) {
                        SpatialPanel(SubspaceModifier.testTag("TestPanel")) {}
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(factoryCalled).isEqualTo(1)
        assertThat(updateCalled).isEqualTo(1)
        cornerRadius.value = 0.3f
        composeTestRule.waitForIdle()
        assertThat(factoryCalled).isEqualTo(1)
        assertThat(updateCalled).isEqualTo(2)
        cornerRadius.value = 0.2f
        composeTestRule.waitForIdle()
        assertThat(factoryCalled).isEqualTo(1)
        assertThat(updateCalled).isEqualTo(3)
        cornerRadius.value = 0.1f
        composeTestRule.waitForIdle()
        assertThat(factoryCalled).isEqualTo(1)
        assertThat(updateCalled).isEqualTo(4)
    }

    @Test
    fun sceneCoreEntity_update_getsMutableStateChanges() {
        val cornerRadius = mutableStateOf(0.5f)

        composeTestRule.setContent {
            TestSetup {
                val session = LocalSession.current ?: error("No session")
                Subspace {
                    SceneCoreEntity(
                        factory = {
                            PanelEntity.create(
                                session,
                                View(composeTestRule.activity),
                                IntSize2d(100, 100),
                                "TestPanel",
                            )
                        },
                        update = { it.cornerRadius = cornerRadius.value },
                        modifier = SubspaceModifier.testTag("TestPanel"),
                    )
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("TestPanel").assertExists()
        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("TestPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .cornerRadius
            )
            .isEqualTo(0.5f)

        cornerRadius.value = 0.4f

        assertThat(
                (composeTestRule
                        .onSubspaceNodeWithTag("TestPanel")
                        .fetchSemanticsNode()
                        .semanticsEntity as PanelEntity)
                    .cornerRadius
            )
            .isEqualTo(0.4f)
    }
}
