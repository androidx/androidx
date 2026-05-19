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

package androidx.xr.scenecore.testing

import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.BoundsComponent
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
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
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class BoundsComponentTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()
    private lateinit var session: Session
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val boundsComponent = BoundsComponent.create(session)
        val tester1 = BoundsComponentTester.create(boundsComponent)
        val tester2 = BoundsComponentTester.create(boundsComponent)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun onBoundsUpdate_triggersListener() = runBlocking {
        @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        val gltfModelEntity = GltfModelEntity.create(session, gltfModel)

        val boundsComponent = BoundsComponent.create(session)
        val tester = testRule.createTester<BoundsComponentTester>(boundsComponent)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()

        var capturedEntity: Entity? = null
        var capturedBoundingBox: BoundingBox? = null
        boundsComponent.addBoundsUpdateListener { entity, box ->
            capturedEntity = entity
            capturedBoundingBox = box
        }

        val newBoundingBox =
            BoundingBox.fromCenterAndHalfExtents(Vector3(1f, 2f, 3f), FloatSize3d(0.5f, 0.5f, 0.5f))
        tester.triggerOnBoundsUpdate(newBoundingBox)
        ShadowLooper.idleMainLooper()

        assertThat(capturedEntity).isEqualTo(gltfModelEntity)
        assertThat(capturedBoundingBox).isEqualTo(newBoundingBox)
    }

    @Test
    fun onBoundsUpdate_withCustomExecutor_triggersListener() = runBlocking {
        @Suppress("NewApi") val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        val gltfModelEntity = GltfModelEntity.create(session, gltfModel)

        val boundsComponent = BoundsComponent.create(session)
        val tester = testRule.createTester<BoundsComponentTester>(boundsComponent)

        assertThat(gltfModelEntity.addComponent(boundsComponent)).isTrue()

        var capturedEntity: Entity? = null
        var capturedBoundingBox: BoundingBox? = null
        val executor = com.google.common.util.concurrent.MoreExecutors.directExecutor()

        boundsComponent.addBoundsUpdateListener(executor) { entity, box ->
            capturedEntity = entity
            capturedBoundingBox = box
        }

        val newBoundingBox =
            BoundingBox.fromCenterAndHalfExtents(Vector3(4f, 5f, 6f), FloatSize3d(1f, 1f, 1f))
        tester.triggerOnBoundsUpdate(newBoundingBox)

        assertThat(capturedEntity).isEqualTo(gltfModelEntity)
        assertThat(capturedBoundingBox).isEqualTo(newBoundingBox)
    }
}
