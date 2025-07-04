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

import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.layout.CoreEntity
import androidx.xr.compose.subspace.layout.CoreGroupEntity
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.createFakeRuntime
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.scenecore.GroupEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SpatialComposeElementTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    lateinit var mockJxrPlatformAdapter: JxrPlatformAdapter

    @Before
    fun setUp() {
        mockJxrPlatformAdapter = mock<JxrPlatformAdapter>()
    }

    @Ignore // b/427806050
    @Test
    fun spatialComposeScene_constructor_initializesPropertiesWithDefaultValues() {
        lateinit var scene: SpatialComposeScene

        composeTestRule.setContent {
            val session = createFakeSession(composeTestRule.activity)
            scene =
                SpatialComposeScene(ownerActivity = composeTestRule.activity, jxrSession = session)
        }

        assertThat(scene.rootElement.spatialComposeScene).isEqualTo(scene)
        assertThat(scene.rootElement.rootCoreEntity).isNull()
        assertThat(scene.rootElement.compositionContext).isNull()
        assertThat(scene.rootElement.compositionOwner.rootVolumeConstraints)
            .isEqualTo(VolumeConstraints())
    }

    @Ignore // b/427806050
    @Test
    fun spatialComposeElement_constructor_initializesPropertiesWithCustomValues() {
        lateinit var scene: SpatialComposeScene
        lateinit var composition: androidx.compose.runtime.CompositionContext
        lateinit var coreEntity: CoreEntity
        lateinit var testConstraints: VolumeConstraints

        composeTestRule.setContent {
            val fakeRuntime = createFakeRuntime(composeTestRule.activity)
            val session = createFakeSession(composeTestRule.activity, fakeRuntime)

            val entity = GroupEntity.create(session, "test")
            coreEntity = CoreGroupEntity(entity)

            composition = rememberCompositionContext()
            testConstraints = VolumeConstraints(10, 20, 30, 40, 50, 60)

            scene =
                SpatialComposeScene(
                    ownerActivity = composeTestRule.activity,
                    jxrSession = session,
                    parentCompositionContext = composition,
                    rootEntity = coreEntity,
                )
            scene.rootVolumeConstraints = testConstraints
        }

        assertThat(scene.rootElement.spatialComposeScene).isEqualTo(scene)
        assertThat(scene.rootElement.compositionContext).isEqualTo(composition)
        assertThat(scene.rootElement.rootCoreEntity).isEqualTo(coreEntity)
        assertThat(scene.rootElement.compositionOwner.rootVolumeConstraints)
            .isEqualTo(testConstraints)
    }
}
