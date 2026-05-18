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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.BoundingBox
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ExperimentalGltfComposeMethod
import androidx.xr.scenecore.GltfAnimationStartOptions
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import java.nio.file.Paths
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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class GltfModelEntityTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var session: Session
    private lateinit var gltfModelEntity: GltfModelEntity
    private lateinit var tester: GltfModelEntityTester

    @RequiresApi(Build.VERSION_CODES.O)
    @Before
    fun setUp() = runBlocking {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.create().start().get()
        val result =
            Session.create(
                context = activity,
                coroutineContext = testDispatcher,
                lifecycleOwner = activity as LifecycleOwner,
            )

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session

        val gltfModel = GltfModel.create(session, Paths.get("test.glb"))
        gltfModelEntity =
            GltfModelEntity.create(session, gltfModel, parent = session.scene.activitySpace)
        tester = testRule.createTester<GltfModelEntityTester>(gltfModelEntity)
    }

    @After
    fun tearDown() {
        if (::activityController.isInitialized) {
            activityController.destroy()
        }
    }

    @Test
    fun addNode_getNodes_returnsNodes() {
        val node1 = TestGltfModelNode(name = "node1")
        val node2 = TestGltfModelNode(name = "node2")
        tester.addNode(node1)
        tester.addNode(node2)

        val nodes = gltfModelEntity.nodes

        assertThat(nodes).hasSize(2)
        assertThat(nodes[0].name).isEqualTo("node1")
        assertThat(nodes[1].name).isEqualTo("node2")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun addAnimation_getAnimations_returnsAnimations() {
        val animation1 = TestGltfAnimation(animationName = "anim1")
        val animation2 = TestGltfAnimation(animationName = "anim2")
        tester.addAnimation(animation1)
        tester.addAnimation(animation2)

        val animations = gltfModelEntity.animations

        assertThat(animations).hasSize(2)
        assertThat(animations[0].name).isEqualTo("anim1")
        assertThat(animations[1].name).isEqualTo("anim2")
    }

    @OptIn(ExperimentalGltfComposeMethod::class)
    @Test
    fun setGltfModelBoundingBox_getGltfModelBoundingBox_returnsGltfModelBoundingBox() {
        assertThat(gltfModelEntity.getGltfModelBoundingBox())
            .isEqualTo(BoundingBox.fromMinMax(Vector3.Zero, Vector3.One))

        val expectedBoundingBox =
            BoundingBox.fromMinMax(Vector3(1.0f, 2.0f, 3.0f), Vector3(4.0f, 5.0f, 6.0f))
        tester.gltfModelBoundingBox = expectedBoundingBox

        assertThat(gltfModelEntity.getGltfModelBoundingBox()).isEqualTo(expectedBoundingBox)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAnimation_startWithOptions_updatesProperties() {
        // Arrange
        val animation = TestGltfAnimation(animationName = "anim")
        tester.addAnimation(animation)

        // Act
        val gltfAnimation = gltfModelEntity.animations[0]
        gltfAnimation.start(
            GltfAnimationStartOptions(
                shouldLoop = true,
                speed = 2.5f,
                seekStartTime = 1.5.seconds.toJavaDuration(),
            )
        )

        // Assert
        assertThat(animation.shouldLoop).isTrue()
        assertThat(animation.speed).isEqualTo(2.5f)
        assertThat(animation.seekStartTime).isEqualTo(1.5f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAnimation_setSpeed_updatesSpeedProperty() {
        // Arrange
        val animation = TestGltfAnimation(animationName = "anim")
        tester.addAnimation(animation)

        // Act
        val gltfAnimation = gltfModelEntity.animations[0]
        gltfAnimation.start()
        gltfAnimation.setSpeed(3.0f)

        // Assert
        assertThat(animation.speed).isEqualTo(3.0f)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testAnimation_seekTo_updatesSeekStartTimeProperty() {
        // Arrange
        val animation = TestGltfAnimation(animationName = "anim")
        tester.addAnimation(animation)

        // Act
        val gltfAnimation = gltfModelEntity.animations[0]
        gltfAnimation.start()
        gltfAnimation.seekTo(2.0.seconds.toJavaDuration())

        // Assert
        assertThat(animation.seekStartTime).isEqualTo(2.0f)
    }

    @Test(expected = IllegalStateException::class)
    fun testAnimation_accessPropertiesBeforeAdding_throwsException() {
        val animation = TestGltfAnimation(animationName = "anim")
        animation.shouldLoop
    }
}
