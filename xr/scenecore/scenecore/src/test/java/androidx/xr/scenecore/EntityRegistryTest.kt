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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import android.os.Build
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.runtime.SceneRuntime
import androidx.xr.scenecore.testing.FakeEntity
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class EntityRegistryTest {
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var sceneRuntime: SceneRuntime
    private lateinit var renderingRuntime: RenderingRuntime

    private lateinit var entityRegistry: EntityRegistry
    private lateinit var session: Session
    private lateinit var activitySpace: ActivitySpace
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var panelEntity: PanelEntity
    private lateinit var anchorEntity: AnchorEntity
    private lateinit var activityPanelEntity: ActivityPanelEntity
    private lateinit var entity: Entity

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        sceneRuntime = session.sceneRuntime
        renderingRuntime = session.renderingRuntime
        entityRegistry = session.scene.entityRegistry
        activitySpace = ActivitySpace.create(sceneRuntime, entityRegistry)
    }

    @Test
    fun creatingEntity_addsEntityToEntityRegistry() {
        createEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        // The entityRegistry contains activity space.
        assertThat(entityRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(entityRegistry.getAllEntities())
            .containsAtLeast(
                entity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
    }

    @Test
    fun getEntityByType_returnsEntityOfType() {
        createEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()

        assertThat(entityRegistry.getEntities<Entity>())
            .containsAtLeast(
                entity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )
        assertThat(entityRegistry.getEntities<PanelEntity>()).contains(panelEntity)
        assertThat(entityRegistry.getEntities<AnchorEntity>()).containsExactly(anchorEntity)
        assertThat(entityRegistry.getEntities<ActivityPanelEntity>())
            .containsExactly(activityPanelEntity)
        assertThat(entityRegistry.getEntities<GltfModelEntity>()).containsExactly(gltfModelEntity)
    }

    @Test
    fun disposeEntity_removesEntityfromEntityRegistry() {
        createEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(entityRegistry.getAllEntities())
            .containsAtLeast(
                entity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        (entity as BaseEntity<*>).disposeInternal()

        assertThat(entityRegistry.getAllEntities().size).isAtLeast(4)
        assertThat(entityRegistry.getAllEntities()).doesNotContain(entity)
    }

    @Test
    fun clearEntityRegistry_removesAllEntityfromEntityRegistry() {
        createEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(entityRegistry.getAllEntities())
            .containsAtLeast(
                entity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        entityRegistry.clear()

        assertThat(entityRegistry.getAllEntities()).isEmpty()
    }

    @Test
    fun removeRtEntity_removesEntityFromEntityRegistry() {
        createEntity()
        createPanelEntity()
        createAnchorEntity()
        createActivityPanelEntity()
        createGltfEntity()
        assertThat(entityRegistry.getAllEntities().size).isAtLeast(5)
        assertThat(entityRegistry.getAllEntities())
            .containsAtLeast(
                entity,
                panelEntity,
                anchorEntity,
                activityPanelEntity,
                gltfModelEntity,
            )

        entityRegistry.removeEntity(panelEntity.rtEntity as FakeEntity)

        assertThat(entityRegistry.getAllEntities().size).isAtLeast(4)
        assertThat(entityRegistry.getAllEntities()).doesNotContain(panelEntity)
    }

    private fun createPanelEntity() {
        panelEntity =
            PanelEntity.create(
                activity,
                sceneRuntime,
                session.scene.perceptionSpace,
                entityRegistry,
                TextView(activity),
                IntSize2d(720, 480),
                "test",
                parent = session.scene.activitySpace,
            )
    }

    private fun createGltfEntity() = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        }
        gltfModelEntity =
            GltfModelEntity.create(
                sceneRuntime,
                renderingRuntime,
                entityRegistry,
                gltfModel,
                parent = session.scene.activitySpace,
            )
    }

    private fun createAnchorEntity() {
        anchorEntity =
            AnchorEntity.create(
                session,
                entityRegistry,
                FloatSize2d(),
                PlaneOrientation.ALL,
                PlaneSemanticType.ALL,
                10.seconds.toJavaDuration(),
            )
    }

    private fun createActivityPanelEntity() {
        activityPanelEntity =
            ActivityPanelEntity.create(
                sceneRuntime,
                session.scene.perceptionSpace,
                entityRegistry,
                IntSize2d(640, 480),
                "test",
                activity,
                parent = session.scene.activitySpace,
            )
    }

    private fun createEntity() {
        entity = EntityImpl.create(sceneRuntime, entityRegistry, "test")
    }
}
