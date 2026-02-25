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
import androidx.xr.runtime.NodeHolder
import androidx.xr.runtime.math.Matrix4.Companion.fromScale
import androidx.xr.runtime.math.Matrix4.Companion.fromTrs
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion.Companion.fromEulerAngles
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.extensions.XrExtensionsProvider.getXrExtensions
import androidx.xr.scenecore.testing.FakeGltfFeature
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.Mat4f
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeTransform
import com.android.extensions.xr.node.ShadowNode
import com.android.extensions.xr.node.ShadowNodeTransform
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PerceptionSpaceScenePoseImplTest {
    private val xrExtensions = getXrExtensions()!!
    private val fakeScheduledExecutor = FakeScheduledExecutorService()
    private val entityManager = EntityManager()
    private val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().start().get()
    private val activitySpace =
        ActivitySpaceImpl(
            xrExtensions.createNode(),
            activity,
            xrExtensions,
            entityManager,
            { xrExtensions.getSpatialState(activity) },
            fakeScheduledExecutor,
        )

    private var mPerceptionSpaceScenePose: PerceptionSpaceScenePoseImpl? = null

    private fun sendTransformEvent(nodeTransform: NodeTransform?) {
        val shadowNode = ShadowNode.extract(activitySpace.getNode())
        shadowNode.transformExecutor.execute { shadowNode.transformListener.accept(nodeTransform) }
    }

    /** Creates a generic glTF entity. */
    private fun createGltfEntity(): GltfEntityImpl {
        val node: NodeHolder<*> = NodeHolder<Node?>(xrExtensions.createNode(), Node::class.java)
        return GltfEntityImpl(
            activity,
            FakeGltfFeature(node),
            activitySpace,
            xrExtensions,
            entityManager,
            fakeScheduledExecutor,
        )
    }

    @Before
    fun setUp() {
        mPerceptionSpaceScenePose = PerceptionSpaceScenePoseImpl(activitySpace)
    }

    @Test
    fun getPoseInActivitySpace_returnsInverseOfActivitySpacePose() {
        val activitySpaceMatrix =
            fromTrs(
                Vector3(1.0f, 2.0f, 3.0f),
                fromEulerAngles(Vector3(0f, 0f, 90f)),
                Vector3(1.0f, 1.0f, 1.0f),
            )
        sendTransformEvent(ShadowNodeTransform.create(Mat4f(activitySpaceMatrix.data)))
        fakeScheduledExecutor.runAll()

        val poseInActivitySpace = mPerceptionSpaceScenePose!!.poseInActivitySpace

        val expectedPose = activitySpaceMatrix.inverse.pose

        assertPose(poseInActivitySpace, expectedPose)
    }

    @Test
    fun transformPoseTo_returnsCorrectPose() {
        val activitySpaceMatrix =
            fromTrs(
                Vector3(4.0f, 5.0f, 6.0f),
                fromEulerAngles(Vector3(90f, 0f, 0f)),
                Vector3(1.0f, 1.0f, 1.0f),
            )
        sendTransformEvent(ShadowNodeTransform.create(Mat4f(activitySpaceMatrix.data)))
        fakeScheduledExecutor.runAll()

        val transformedPose = mPerceptionSpaceScenePose!!.transformPoseTo(Pose(), activitySpace)

        val expectedPose = activitySpaceMatrix.inverse.pose

        assertPose(transformedPose, expectedPose)
    }

    @Test
    fun transformPoseTo_toScaledEntity_returnsCorrectPose() {
        val activitySpaceMatrix =
            fromTrs(
                Vector3(4.0f, 5.0f, 6.0f),
                fromEulerAngles(Vector3(90f, 0f, 0f)),
                Vector3(1.0f, 1.0f, 1.0f),
            )
        sendTransformEvent(ShadowNodeTransform.create(Mat4f(activitySpaceMatrix.data)))
        fakeScheduledExecutor.runAll()
        val gltfEntity = createGltfEntity()
        gltfEntity.setScale(Vector3(2.0f, 2.0f, 2.0f))

        val transformedPose = mPerceptionSpaceScenePose!!.transformPoseTo(Pose(), gltfEntity)

        val unscaledPose = activitySpaceMatrix.inverse.pose
        val expectedPose =
            Pose(unscaledPose.translation.scale(Vector3(0.5f, 0.5f, 0.5f)), unscaledPose.rotation)

        assertPose(transformedPose, expectedPose)
    }

    @Test
    fun getActivitySpaceScale_returnsInverseOfActivitySpaceWorldScale() {
        val activitySpaceScale = 5f
        activitySpace.setOpenXrReferenceSpaceTransform(fromScale(activitySpaceScale))

        assertVector3(mPerceptionSpaceScenePose!!.activitySpaceScale, Vector3.One)
    }
}
