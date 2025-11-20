/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.scenecore.GroupEntity
import androidx.xr.scenecore.runtime.SceneRuntime
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SpatialComposeElementTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    lateinit var mockSceneRuntime: SceneRuntime

    @Before
    fun setUp() {
        mockSceneRuntime = mock<SceneRuntime>()
    }

    @Test
    fun spatialComposeScene_constructor_initializesPropertiesWithDefaultValues() {
        lateinit var scene: SpatialComposeScene

        composeTestRule.setContent {
            val session = remember { createFakeSession(composeTestRule.activity) }
            val context = rememberCompositionContext()
            scene =
                SpatialComposeScene(
                    lifecycleOwner = composeTestRule.activity,
                    context = composeTestRule.activity,
                    jxrSession = session,
                    parentCompositionContext = context,
                )
        }

        assertThat(scene.rootElement.spatialComposeScene).isEqualTo(scene)
        assertThat(scene.rootElement.rootCoreEntity).isNull()
    }

    @Test
    fun spatialComposeElement_constructor_initializesPropertiesWithCustomValues() {
        lateinit var scene: SpatialComposeScene
        lateinit var composition: androidx.compose.runtime.CompositionContext
        lateinit var coreEntity: CoreEntity

        composeTestRule.setContent {
            val fakeRuntime = createFakeRuntime(composeTestRule.activity)
            val session = remember { createFakeSession(composeTestRule.activity, fakeRuntime) }

            val entity = GroupEntity.create(session, "test")
            coreEntity = CoreGroupEntity(entity)

            composition = rememberCompositionContext()

            scene =
                SpatialComposeScene(
                    lifecycleOwner = composeTestRule.activity,
                    context = composeTestRule.activity,
                    jxrSession = session,
                    parentCompositionContext = composition,
                    rootEntity = coreEntity,
                )
        }

        assertThat(scene.rootElement.spatialComposeScene).isEqualTo(scene)
        assertThat(scene.rootElement.compositionContext).isEqualTo(composition)
        assertThat(scene.rootElement.rootCoreEntity).isEqualTo(coreEntity)
    }
}
