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

package androidx.xr.scenecore

import android.os.Build
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrRuntime
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.RenderingRuntime
import androidx.xr.scenecore.testing.FakeBoundsComponent
import androidx.xr.scenecore.testing.FakeRenderingRuntimeFactory
import androidx.xr.scenecore.testing.FakeSceneRuntime
import androidx.xr.scenecore.testing.FakeSceneRuntimeFactory
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class BoundsComponentTest {
    private val mFakePerceptionRuntimeFactory = FakePerceptionRuntimeFactory()
    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var fakeSceneRuntime: FakeSceneRuntime
    private lateinit var fakeRenderingRuntime: RenderingRuntime
    private lateinit var session: Session

    private lateinit var gltfModel: GltfModel
    private lateinit var gltfModelEntity: GltfModelEntity

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setup() = runBlocking {
        val runtimes = mutableListOf<JxrRuntime>()
        val fakeRuntimeFactory = FakeSceneRuntimeFactory()
        fakeSceneRuntime = fakeRuntimeFactory.create(activity)
        runtimes.add(fakeSceneRuntime)
        val fakeRenderingRuntimeFactory = FakeRenderingRuntimeFactory()
        fakeRenderingRuntime = fakeRenderingRuntimeFactory.create(runtimes, activity)
        runtimes.add(fakeRenderingRuntime)

        session =
            Session(
                activity,
                runtimes =
                    listOf(
                        mFakePerceptionRuntimeFactory.createRuntime(activity),
                        fakeSceneRuntime,
                        fakeRenderingRuntime,
                    ),
                lifecycleOwner = activity,
            )
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))

        gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(
                fakeSceneRuntime,
                fakeRenderingRuntime,
                session.scene.entityManager,
                gltfModel,
            )
    }

    @Test
    fun createBoundsComponent_callsRuntimeCreateBoundsComponent() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(boundsComponent.rtBoundsComponent).isInstanceOf(FakeBoundsComponent::class.java)
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
        val anchorEntity =
            AnchorEntity.create(
                session,
                FloatSize2d(),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
                10.seconds.toJavaDuration(),
            )
        val activityPanelEntity = ActivityPanelEntity.create(session, IntSize2d(640, 480), "test")
        val groupEntity = GroupEntity.create(session, "test")
        val surfaceEntity =
            SurfaceEntity.create(
                session,
                Pose.Identity,
                SurfaceEntity.Shape.Quad(FloatSize2d(1.0f, 1.0f)),
                SurfaceEntity.StereoMode.SIDE_BY_SIDE,
            )
        val boundsComponent = BoundsComponent.create(session)

        assertThat(panelEntity.addComponent(boundsComponent)).isFalse()
        assertThat(anchorEntity.addComponent(boundsComponent)).isFalse()
        assertThat(activityPanelEntity.addComponent(boundsComponent)).isFalse()
        assertThat(groupEntity.addComponent(boundsComponent)).isFalse()
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
        assertThat(gltfModelEntity.rtEntity!!.getComponents()[0])
            .isSameInstanceAs(boundsComponent.rtBoundsComponent)
    }

    @Test
    fun removeBoundsComponent_removesRuntimeBoundsComponent() {
        val boundsComponent = BoundsComponent.create(session)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat(gltfModelEntity.rtEntity!!.getComponents()[0])
            .isSameInstanceAs(boundsComponent.rtBoundsComponent)

        gltfModelEntity.removeComponent(boundsComponent)

        assertThat(gltfModelEntity.rtEntity!!.getComponents())
            .doesNotContain(boundsComponent.rtBoundsComponent)
    }

    @Test
    fun addOnBoundsUpdateListener_addsListener() {
        val boundsComponent = BoundsComponent.create(session)
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> }
        boundsComponent.addOnBoundsUpdateListener(listener)
        gltfModelEntity.addComponent(boundsComponent)

        assertThat(boundsComponent.boundsUpdateListenerMap).containsKey(listener)
    }

    @Test
    fun addOnBoundsUpdateListener_removeOnBoundsUpdateListener_listenersAreNotCleared() {
        val boundsComponent = BoundsComponent.create(session)
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> }
        boundsComponent.addOnBoundsUpdateListener(listener)
        gltfModelEntity.addComponent(boundsComponent)

        assertThat(boundsComponent.boundsUpdateListenerMap).containsKey(listener)

        gltfModelEntity.removeComponent(boundsComponent)

        assertThat(boundsComponent.boundsUpdateListenerMap).containsKey(listener)
    }

    @Test
    fun addOnBoundsUpdateListener_invokesRuntimeAddOnBoundsUpdateListener() {
        val onBoundsUpdateListener1 = BiConsumer<Entity, BoundingBox> { _, _ -> }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(onBoundsUpdateListener1)
        val rtBoundsUpdateEventListener1 =
            boundsComponent.boundsUpdateListenerMap[onBoundsUpdateListener1]
        val rtBoundsComponent = boundsComponent.rtBoundsComponent as FakeBoundsComponent

        assertThat(rtBoundsComponent.listeners.keys.toList()[0])
            .isSameInstanceAs(rtBoundsUpdateEventListener1)
    }

    @Test
    fun addOnBoundsUpdateListener_addSameListenerTwice_isIgnored() {
        val boundsComponent = BoundsComponent.create(session)
        val listener = BiConsumer<Entity, BoundingBox> { _, _ -> }
        boundsComponent.addOnBoundsUpdateListener(listener)
        val rtBoundsUpdateEventListener = boundsComponent.boundsUpdateListenerMap[listener]
        val rtBoundsComponent = boundsComponent.rtBoundsComponent as FakeBoundsComponent

        assertThat(boundsComponent.boundsUpdateListenerMap).hasSize(1)
        assertThat(rtBoundsComponent.listeners).hasSize(1)
        assertThat(rtBoundsComponent.listeners.keys.toList()[0])
            .isSameInstanceAs(rtBoundsUpdateEventListener)

        boundsComponent.addOnBoundsUpdateListener(listener)

        assertThat(boundsComponent.boundsUpdateListenerMap).hasSize(1)
        assertThat(rtBoundsComponent.listeners).hasSize(1)
        assertThat(rtBoundsComponent.listeners.keys.toList()[0])
            .isSameInstanceAs(rtBoundsUpdateEventListener)
    }

    @Test
    fun addOnBoundsUpdateListener_multiple_runtimeAddOnBoundsUpdateListenerMultiple() {
        val onBoundsUpdateListener1 = BiConsumer<Entity, BoundingBox> { _, _ -> }
        val onBoundsUpdateListener2 = BiConsumer<Entity, BoundingBox> { _, _ -> }
        val boundsComponent = BoundsComponent.create(session)
        val rtBoundsComponent = boundsComponent.rtBoundsComponent as FakeBoundsComponent

        boundsComponent.addOnBoundsUpdateListener(onBoundsUpdateListener1)
        val rtBoundsUpdateEventListener1 =
            boundsComponent.boundsUpdateListenerMap[onBoundsUpdateListener1]

        assertThat(rtBoundsComponent.listeners).containsKey(rtBoundsUpdateEventListener1)

        boundsComponent.addOnBoundsUpdateListener(onBoundsUpdateListener2)
        val rtBoundsUpdateEventListener2 =
            boundsComponent.boundsUpdateListenerMap[onBoundsUpdateListener2]

        assertThat(rtBoundsComponent.listeners).containsKey(rtBoundsUpdateEventListener2)
    }

    @Test
    fun addOnBoundsUpdateListener_removeBoundsComponent_notClearRuntimeListener() {
        val onBoundsUpdateListener1 = BiConsumer<Entity, BoundingBox> { _, _ -> }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(onBoundsUpdateListener1)
        val rtBoundsComponent = boundsComponent.rtBoundsComponent as FakeBoundsComponent

        assertThat(rtBoundsComponent.listeners.count()).isEqualTo(1)

        gltfModelEntity.removeComponent(boundsComponent)

        assertThat(rtBoundsComponent.listeners.count()).isEqualTo(1)
    }

    @Test
    fun removeOnBoundsUpdateListener_invokesRuntimeRemoveOnBoundsUpdateListener() {
        val onBoundsUpdateListener1 = BiConsumer<Entity, BoundingBox> { _, _ -> }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(onBoundsUpdateListener1)
        val rtBoundsComponent = boundsComponent.rtBoundsComponent as FakeBoundsComponent

        assertThat(rtBoundsComponent.listeners.count()).isEqualTo(1)

        boundsComponent.removeOnBoundsUpdateListener(onBoundsUpdateListener1)

        assertThat(rtBoundsComponent.listeners.count()).isEqualTo(0)
    }

    @Test
    fun addOnBoundsUpdateListener_invokeListener_correctCallback() {
        var boundingBox1: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero)
        val onBoundsUpdateListener1 =
            BiConsumer<Entity, BoundingBox> { _, boundingBox -> boundingBox1 = boundingBox }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(DirectExecutor(), onBoundsUpdateListener1)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat((boundsComponent.rtBoundsComponent as FakeBoundsComponent).listeners.count())
            .isEqualTo(1)

        // Invoke listener
        val expectedBoundingBox1 = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
        (boundsComponent.rtBoundsComponent as FakeBoundsComponent).onBoundsUpdate(
            expectedBoundingBox1
        )

        assertThat(boundingBox1).isEqualTo(expectedBoundingBox1)
    }

    @Test
    fun addOnBoundsUpdateListener_invokesMultipleListeners_correctCallbacks() {
        var boundingBox1: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero)
        val onBoundsUpdateListener1 =
            BiConsumer<Entity, BoundingBox> { _, boundingBox -> boundingBox1 = boundingBox }
        var boundingBox2: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero)
        val onBoundsUpdateListener2 =
            BiConsumer<Entity, BoundingBox> { _, boundingBox -> boundingBox2 = boundingBox }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(DirectExecutor(), onBoundsUpdateListener1)
        boundsComponent.addOnBoundsUpdateListener(DirectExecutor(), onBoundsUpdateListener2)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat((boundsComponent.rtBoundsComponent as FakeBoundsComponent).listeners.count())
            .isEqualTo(2)

        val expectedBoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
        (boundsComponent.rtBoundsComponent as FakeBoundsComponent).onBoundsUpdate(
            expectedBoundingBox
        )

        assertThat(boundingBox1).isEqualTo(expectedBoundingBox)
        assertThat(boundingBox2).isEqualTo(expectedBoundingBox)
    }

    @Test
    fun removeOnBoundsUpdateListener_invokeListener_noCallback() {
        var boundingBox1: BoundingBox = BoundingBox.fromMinMax(Vector3.Zero, Vector3.Zero)
        val onBoundsUpdateListener1 =
            BiConsumer<Entity, BoundingBox> { _, boundingBox -> boundingBox1 = boundingBox }
        val boundsComponent = BoundsComponent.create(session)
        boundsComponent.addOnBoundsUpdateListener(DirectExecutor(), onBoundsUpdateListener1)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()
        assertThat((boundsComponent.rtBoundsComponent as FakeBoundsComponent).listeners.count())
            .isEqualTo(1)

        // Invoke listener
        val expectedBoundingBox1 = BoundingBox.fromMinMax(Vector3.Zero, Vector3.One)
        (boundsComponent.rtBoundsComponent as FakeBoundsComponent).onBoundsUpdate(
            expectedBoundingBox1
        )

        assertThat(boundingBox1).isEqualTo(expectedBoundingBox1)

        // Remove listener
        boundsComponent.removeOnBoundsUpdateListener(onBoundsUpdateListener1)

        assertThat((boundsComponent.rtBoundsComponent as FakeBoundsComponent).listeners.count())
            .isEqualTo(0)

        // Invoke listener
        val expectedBoundingBox2 = BoundingBox.fromMinMax(Vector3.Zero, Vector3(1f, 0f, 0f))
        (boundsComponent.rtBoundsComponent as FakeBoundsComponent).onBoundsUpdate(
            expectedBoundingBox2
        )

        assertThat(boundingBox1).isNotEqualTo(expectedBoundingBox2)
    }

    private class DirectExecutor : Executor {
        override fun execute(r: Runnable) {
            r.run()
        }
    }
}
