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

package androidx.xr.scenecore.testrule

import android.os.Build
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.AnchorSpace
import androidx.xr.scenecore.BoundsComponent
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import androidx.xr.scenecore.SurfaceEntity
import androidx.xr.scenecore.testing.BoundsComponentTester
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.function.BiConsumer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class TestRuleBoundsComponentTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()
    private lateinit var session: Session
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activity: ComponentActivity
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setup(): Unit = runBlocking {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result = Session.create(activity, testDispatcher, lifecycleOwner = activity)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))

        gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity = GltfModelEntity.create(session, gltfModel)
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun createBoundsComponent_callsRuntimeCreateBoundsComponent() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent))
            .isNotNull()
    }

    @Test
    fun addBoundsComponent_nonGltfModelEntity_failedToAdd() {
        val panelEntity =
            PanelEntity.create(
                session,
                view = TextView(activity),
                pixelDimensions = IntSize2d(720, 480),
                name = "test",
            )
        val anchorSpace =
            AnchorSpace.create(
                session,
                FloatSize2d(),
                PlaneOrientation.ALL,
                PlaneSemanticType.ALL,
                10.seconds.toJavaDuration(),
            )
        val activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test")
        val entity = Entity.create(session, "test")
        val surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            )
        val boundsComponent = BoundsComponent.create(session)

        assertThat(panelEntity.addComponent(boundsComponent)).isFalse()
        assertThat(anchorSpace.addComponent(boundsComponent)).isFalse()
        assertThat(activityPanelEntity.addComponent(boundsComponent)).isFalse()
        assertThat(entity.addComponent(boundsComponent)).isFalse()
        assertThat(surfaceEntity.addComponent(boundsComponent)).isFalse()
    }

    @Test
    fun addBoundsComponent_gltfModelEntity_succeedsToAdd() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
    }

    @Test
    fun addBoundsComponent_gltfModelEntityTwice_failedInSecondTime() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat(gltfModelEntity.addComponent(boundsComponent)).isFalse()
    }

    @Test
    fun removeBoundsComponent_addAfterRemove_succeedsToAdd() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()

        gltfModelEntity.removeComponent(boundsComponent)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
    }

    @Test
    fun addBoundsComponent_addsRuntimeBoundsComponent() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat(gltfModelEntity.getComponents()).contains(boundsComponent)

        // Verifies addition by ensuring a tester can be successfully created for the component.
        assertThat(scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent))
            .isNotNull()
    }

    @Test
    fun removeBoundsComponent_removesRuntimeBoundsComponent() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat(scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent))
            .isNotNull()
        assertThat(gltfModelEntity.getComponents()).contains(boundsComponent)

        gltfModelEntity.removeComponent(boundsComponent)

        assertThat(gltfModelEntity.getComponents()).doesNotContain(boundsComponent)

        // Verifies removal by ensuring the component can be added to another entity.
        // This indirectly confirms it's no longer attached to the original entity.
        val anotherGltfModelEntity = GltfModelEntity.create(session, gltfModel)
        assertThat(anotherGltfModelEntity.addComponent(boundsComponent)).isTrue()
    }

    @Test
    fun addBoundsUpdateListener_addsListener() {
        val boundsComponent = BoundsComponent.create(session)
        var callCount = 0
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> callCount++ }
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)

        // Verifies listener registration by ensuring the tester can trigger it successfully.
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)
        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))

        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun addBoundsUpdateListener_removeBoundsComponent_listenersAreNotCleared() {
        val boundsComponent = BoundsComponent.create(session)
        var callCount = 0
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> callCount++ }
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        gltfModelEntity.removeComponent(boundsComponent)

        // Listeners should persist even if component is removed from entity.
        // Re-adding it should allow the listener to be triggered again.
        gltfModelEntity.addComponent(boundsComponent)
        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun addBoundsUpdateListener_addSameListenerTwice_isIgnored() {
        val boundsComponent = BoundsComponent.create(session)
        var callCount = 0
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> callCount++ }

        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)

        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun addBoundsUpdateListener_removeBoundsComponent_clearsRuntimeListener() {
        var callCount = 0
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> callCount++ }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)

        gltfModelEntity.removeComponent(boundsComponent)

        // Triggering the update now should not call the listener as it should have been
        // removed from the runtime during detach.
        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun removeBoundsUpdateListener_invokesRuntimeRemoveBoundsUpdateListener() {
        var callCount = 0
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> callCount++ }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)

        boundsComponent.removeBoundsUpdateListener(listener)

        tester.triggerOnBoundsUpdate(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))
        assertThat(callCount).isEqualTo(1)
    }

    @Test
    fun addBoundsUpdateListener_invokeListener_correctCallback() {
        var receivedBoundingBox: BoundingBox? = null
        var receivedEntity: Entity? = null
        val listener =
            BiConsumer<Entity, BoundingBox> { entity, boundingBox ->
                receivedEntity = entity
                receivedBoundingBox = boundingBox
            }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
        tester.triggerOnBoundsUpdate(expectedBoundingBox)

        assertThat(receivedBoundingBox).isEqualTo(expectedBoundingBox)
        assertThat(receivedEntity).isEqualTo(gltfModelEntity)
    }

    @Test
    fun addBoundsUpdateListener_invokesMultipleListeners_correctCallbacks() {
        var box1: BoundingBox? = null
        val listener1 = BiConsumer<Entity, BoundingBox> { _, box -> box1 = box }
        var box2: BoundingBox? = null
        val listener2 = BiConsumer<Entity, BoundingBox> { _, box -> box2 = box }

        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener1)
        boundsComponent.addBoundsUpdateListener({ it.run() }, listener2)
        gltfModelEntity.addComponent(boundsComponent)
        val tester = scenecoreTestRule.createTester<BoundsComponentTester>(boundsComponent)

        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
        tester.triggerOnBoundsUpdate(expectedBoundingBox)

        assertThat(box1).isEqualTo(expectedBoundingBox)
        assertThat(box2).isEqualTo(expectedBoundingBox)
    }
}
