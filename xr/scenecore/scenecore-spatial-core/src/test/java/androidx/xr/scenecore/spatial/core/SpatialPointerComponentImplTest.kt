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
package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.hardware.display.DisplayManager
import android.view.View
import android.view.ViewGroup
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.PixelDimensions
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.runtime.SpatialPointerIcon
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SpatialPointerComponentImplTest {
    private val xrExtensions = requireNotNull(getXrExtensions())
    private val activity = Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val fakeExecutor = FakeScheduledExecutorService()
    private val entityManager = EntityManager()
    private lateinit var runtime: SceneRuntime

    @Before
    fun setUp() {
        runtime = SpatialSceneRuntime.create(activity, fakeExecutor, xrExtensions, EntityManager())
    }

    @After
    fun tearDown() {
        // Destroy the runtime between test cases to clean up lingering references.
        runtime.destroy()
    }

    private fun createTestPanelEntity(): PanelEntityImpl {
        val display = activity.getSystemService(DisplayManager::class.java).displays[0]
        val displayContext = activity.createDisplayContext(display!!)
        val view = View(displayContext)
        view.setLayoutParams(ViewGroup.LayoutParams(640, 480))
        val node = xrExtensions.createNode()

        val panelEntity =
            PanelEntityImpl(
                displayContext,
                node,
                view,
                xrExtensions,
                entityManager,
                PixelDimensions(sVgaResolutionPx.width.toInt(), sVgaResolutionPx.height.toInt()),
                "panel",
                fakeExecutor,
            )

        panelEntity.parent = runtime.activitySpace
        return panelEntity
    }

    @Test
    fun addComponentToTwoEntity_fails() {
        val entity1 = createTestPanelEntity()
        val entity2 = createTestPanelEntity()
        val component = SpatialPointerComponentImpl(xrExtensions)
        assertThat(component).isNotNull()
        assertThat(entity1.addComponent(component)).isTrue()
        assertThat(entity2.addComponent(component)).isFalse()
    }

    @Test
    fun onAttach_setsSpatialPointerIconToDefault() {
        val entity = createTestPanelEntity()
        val component = SpatialPointerComponentImpl(xrExtensions)
        assertThat(component.onAttach(entity)).isTrue()
        assertThat(component.spatialPointerIcon).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT)
    }

    @Test
    fun onDetach_setsSpatialPointerIconToDefault() {
        val entity = createTestPanelEntity()
        val component = SpatialPointerComponentImpl(xrExtensions)
        assertThat(component.onAttach(entity)).isTrue()
        component.spatialPointerIcon = SpatialPointerIcon.TYPE_NONE
        component.onDetach(entity)
        assertThat(component.spatialPointerIcon).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT)
    }

    @Test
    fun setSpatialPointerIcon_setsSpatialPointerIcon() {
        val entity = createTestPanelEntity()
        val component = SpatialPointerComponentImpl(xrExtensions)
        assertThat(component.onAttach(entity)).isTrue()
        component.spatialPointerIcon = SpatialPointerIcon.TYPE_NONE
        assertThat(component.spatialPointerIcon).isEqualTo(SpatialPointerIcon.TYPE_NONE)
        component.spatialPointerIcon = (SpatialPointerIcon.TYPE_CIRCLE)
        assertThat(component.spatialPointerIcon).isEqualTo(SpatialPointerIcon.TYPE_CIRCLE)
        component.spatialPointerIcon = (SpatialPointerIcon.TYPE_DEFAULT)
        assertThat(component.spatialPointerIcon).isEqualTo(SpatialPointerIcon.TYPE_DEFAULT)
    }

    companion object {
        private val sVgaResolutionPx = Dimensions(640f, 480f, 0f)
    }
}
