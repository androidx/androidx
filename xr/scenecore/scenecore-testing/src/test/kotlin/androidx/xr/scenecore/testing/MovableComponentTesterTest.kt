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
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.AnchorPlacement
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.EntityMoveListener
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertThrows
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
class MovableComponentTesterTest {
    @get:Rule val testRule = SceneCoreTestRule()
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
        val component = MovableComponent.createSystemMovable(session)
        val tester1 = MovableComponentTester.create(component)
        val tester2 = MovableComponentTester.create(component)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun properties_reflectConfiguration() {
        // System Movable
        val systemMovable = MovableComponent.createSystemMovable(session, scaleInZ = true)
        val systemTester = testRule.createTester<MovableComponentTester>(systemMovable)
        assertThat(systemTester.isSystemMovable).isTrue()
        assertThat(systemTester.isScaleInZ).isTrue()
        assertThat(systemTester.isAnchorable).isFalse()

        // Custom Movable
        val customMovable =
            MovableComponent.createCustomMovable(
                session,
                scaleInZ = false,
                null,
                object : EntityMoveListener {},
            )
        val customTester = testRule.createTester<MovableComponentTester>(customMovable)
        assertThat(customTester.isSystemMovable).isFalse()
        assertThat(customTester.isScaleInZ).isFalse()
        assertThat(customTester.isAnchorable).isFalse()

        // Anchorable
        val anchorable =
            MovableComponent.createAnchorable(session, setOf(AnchorPlacement.createForPlanes()))
        val anchorableTester = testRule.createTester<MovableComponentTester>(anchorable)
        assertThat(anchorableTester.isSystemMovable).isTrue()
        assertThat(anchorableTester.isScaleInZ).isFalse()
        assertThat(anchorableTester.isAnchorable).isTrue()
    }

    @Test
    fun onMoveEvents_triggerListenerInSequence() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val entity = Entity.create(session)
        assertThat(entity.addComponent(component)).isTrue()

        var onMoveStartCalled = false
        var onMoveUpdateCalled = false
        var onMoveEndCalled = false

        component.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    onMoveStartCalled = true
                    assertThat(initialParent).isEqualTo(entity)
                }

                override fun onMoveUpdate(
                    entity: Entity,
                    currentInputRay: Ray,
                    currentPose: Pose,
                    currentScale: Float,
                ) {
                    onMoveUpdateCalled = true
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    onMoveEndCalled = true
                }
            }
        )

        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        // Start -> Update -> End sequence
        tester.triggerOnMoveStart(ray, pose, scale, entity)
        ShadowLooper.idleMainLooper()
        assertThat(onMoveStartCalled).isTrue()

        tester.triggerOnMoveUpdate(ray, pose, scale)
        ShadowLooper.idleMainLooper()
        assertThat(onMoveUpdateCalled).isTrue()

        tester.triggerOnMoveEnd(ray, pose, scale)
        ShadowLooper.idleMainLooper()
        assertThat(onMoveEndCalled).isTrue()
    }

    @Test
    fun triggerOnMoveUpdate_withoutStart_throwsIllegalStateException() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        assertThrows(IllegalStateException::class.java) {
            tester.triggerOnMoveUpdate(ray, pose, scale)
        }
    }

    @Test
    fun triggerOnMoveEnd_immediatelyAfterStart_worksCorrectly() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val entity = Entity.create(session)
        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        var onMoveStartCalled = false
        var onMoveEndCalled = false
        component.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    onMoveStartCalled = true
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    onMoveEndCalled = true
                }
            }
        )

        assertThat(entity.addComponent(component)).isTrue()

        tester.triggerOnMoveStart(ray, pose, scale, entity)
        ShadowLooper.idleMainLooper()
        assertThat(onMoveStartCalled).isTrue()

        // Should not throw anymore, supporting click scenarios.
        tester.triggerOnMoveEnd(ray, pose, scale)
        ShadowLooper.idleMainLooper()
        assertThat(onMoveEndCalled).isTrue()
    }

    @Test
    fun onMoveEvents_propagatesCorrectValues() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val entity = Entity.create(session)
        assertThat(entity.addComponent(component)).isTrue()

        val startRay = Ray(Vector3(1f, 2f, 3f), Vector3(0f, 1f, 0f))
        val startPose = Pose(Vector3(4f, 5f, 6f), Quaternion(0f, 0f, 0f, 1f))
        val startScale = Vector3(1.1f, 1.2f, 1.3f)
        val initialParent = session.scene.activitySpace

        val updateRay = Ray(Vector3(2f, 3f, 4f), Vector3(1f, 0f, 0f))
        val updatePose = Pose(Vector3(5f, 6f, 7f), Quaternion(0f, 1f, 0f, 0f))
        val updateScale = Vector3(2.1f, 2.2f, 2.3f)

        val endRay = Ray(Vector3(3f, 4f, 5f), Vector3(0f, 0f, 1f))
        val endPose = Pose(Vector3(6f, 7f, 8f), Quaternion(1f, 0f, 0f, 0f))
        val endScale = Vector3(3.1f, 3.2f, 3.3f)
        val updatedParent = entity

        var capturedStartRay: Ray? = null
        var capturedStartPose: Pose? = null
        var capturedStartScale: Float? = null
        var capturedInitialParent: Entity? = null

        var capturedUpdateRay: Ray? = null
        var capturedUpdatePose: Pose? = null
        var capturedUpdateScale: Float? = null

        var capturedEndRay: Ray? = null
        var capturedEndPose: Pose? = null
        var capturedEndScale: Float? = null
        var capturedUpdatedParent: Entity? = null

        component.addMoveListener(
            object : EntityMoveListener {
                override fun onMoveStart(
                    entity: Entity,
                    initialInputRay: Ray,
                    initialPose: Pose,
                    initialScale: Float,
                    initialParent: Entity,
                ) {
                    capturedStartRay = initialInputRay
                    capturedStartPose = initialPose
                    capturedStartScale = initialScale
                    capturedInitialParent = initialParent
                }

                override fun onMoveUpdate(
                    entity: Entity,
                    currentInputRay: Ray,
                    currentPose: Pose,
                    currentScale: Float,
                ) {
                    capturedUpdateRay = currentInputRay
                    capturedUpdatePose = currentPose
                    capturedUpdateScale = currentScale
                }

                override fun onMoveEnd(
                    entity: Entity,
                    finalInputRay: Ray,
                    finalPose: Pose,
                    finalScale: Float,
                    updatedParent: Entity?,
                ) {
                    capturedEndRay = finalInputRay
                    capturedEndPose = finalPose
                    capturedEndScale = finalScale
                    capturedUpdatedParent = updatedParent
                }
            }
        )

        tester.triggerOnMoveStart(startRay, startPose, startScale, initialParent)
        ShadowLooper.idleMainLooper()
        assertThat(capturedStartRay).isEqualTo(startRay)
        assertThat(capturedStartPose).isEqualTo(startPose)
        assertThat(capturedStartScale).isEqualTo(startScale.x)
        assertThat(capturedInitialParent).isEqualTo(initialParent)

        tester.triggerOnMoveUpdate(updateRay, updatePose, updateScale)
        ShadowLooper.idleMainLooper()
        assertThat(capturedUpdateRay).isEqualTo(updateRay)
        assertThat(capturedUpdatePose).isEqualTo(updatePose)
        assertThat(capturedUpdateScale).isEqualTo(updateScale.x)

        tester.triggerOnMoveEnd(endRay, endPose, endScale, updatedParent)
        ShadowLooper.idleMainLooper()
        assertThat(capturedEndRay).isEqualTo(endRay)
        assertThat(capturedEndPose).isEqualTo(endPose)
        assertThat(capturedEndScale).isEqualTo(endScale.x)
        assertThat(capturedUpdatedParent).isEqualTo(updatedParent)

        // Test with null updated parent
        tester.triggerOnMoveStart(startRay, startPose, startScale, initialParent)
        tester.triggerOnMoveUpdate(updateRay, updatePose, updateScale)
        tester.triggerOnMoveEnd(endRay, endPose, endScale, null)
        ShadowLooper.idleMainLooper()
        assertThat(capturedUpdatedParent).isEqualTo(null)
    }

    @Test
    fun triggerOnMoveEnd_withoutStart_throwsIllegalStateException() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        assertThrows(IllegalStateException::class.java) {
            tester.triggerOnMoveEnd(ray, pose, scale)
        }
    }

    @Test
    fun triggerOnMoveStart_duringActiveMove_throwsIllegalStateException() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val entity = Entity.create(session)
        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        tester.triggerOnMoveStart(ray, pose, scale, entity)

        assertThrows(IllegalStateException::class.java) {
            tester.triggerOnMoveStart(ray, pose, scale, entity)
        }
    }

    @Test
    fun triggerOnMoveEnd_resetsState_allowsNewMove() {
        val component = MovableComponent.createSystemMovable(session)
        val tester = testRule.createTester<MovableComponentTester>(component)
        val entity = Entity.create(session)
        val ray = Ray(Vector3.Zero, Vector3.Forward)
        val pose = Pose.Identity
        val scale = Vector3.One

        // Complete first move
        tester.triggerOnMoveStart(ray, pose, scale, entity)
        tester.triggerOnMoveUpdate(ray, pose, scale)
        tester.triggerOnMoveEnd(ray, pose, scale)

        // Start second move should not throw
        tester.triggerOnMoveStart(ray, pose, scale, entity)
    }
}
