/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may a copy of the License at
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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.math.assertPose
import androidx.xr.runtime.testing.math.assertVector3
import androidx.xr.scenecore.runtime.AnchorEntity
import androidx.xr.scenecore.runtime.GltfFeature
import androidx.xr.scenecore.runtime.NodeHolder
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.impl.PerceptionSpaceScenePoseImpl
import androidx.xr.scenecore.testing.FakeScheduledExecutorService
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeRepository
import com.google.common.truth.Truth
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@SuppressLint("NewApi") // TODO: b/413661481 - Remove this suppression prior to JXR stable release.
class AnchorEntityImplTest : SystemSpaceEntityImplTest() {
    override val xrExtensions = SpatialCoreXrExtensionsHolderProvider.extensionsLegacy
    private val anchorStateListener = mock<AnchorEntity.OnStateChangedListener>()
    override val activity: Activity =
        Robolectric.buildActivity(Activity::class.java).create().start().get()
    override val fakeExecutor = FakeScheduledExecutorService()
    override val sceneNodeRegistry = SceneNodeRegistry()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant("android.permission.SCENE_UNDERSTANDING")

    @get:Rule val arCoreTestRule = ArCoreTestRule()

    private lateinit var activitySpace: ActivitySpaceImpl

    override fun createEntity(node: Node): AndroidXrEntity {
        return AnchorEntityImpl.create(
            activity,
            node,
            activitySpace,
            xrExtensions,
            sceneNodeRegistry,
            fakeExecutor,
        )
    }

    private lateinit var session: Session

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun doBeforeEachTest() {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.create().start().get()
        session =
            (Session.create(activity, testDispatcher, TestLifecycleOwner(Lifecycle.State.RESUMED))
                    as SessionCreateSuccess)
                .session
        val taskNode = xrExtensions.createNode()
        activitySpace =
            ActivitySpaceImpl(
                taskNode,
                activity,
                xrExtensions,
                sceneNodeRegistry,
                { xrExtensions.getSpatialState(activity) },
                fakeExecutor,
            )
        val currentTimeMillis = 1000000000L
        SystemClock.setCurrentTimeMillis(currentTimeMillis)
        sceneNodeRegistry.addSystemSpaceScenePose(PerceptionSpaceScenePoseImpl(activitySpace))
    }

    /**
     * Returns the anchor entity impl. Used in the base SystemSpaceEntityImplTest to ensure that the
     * anchor entity complies with all the expected behaviors of a system space entity.
     */
    override val systemSpaceEntityImpl: SystemSpaceEntityImpl
        get() = createAnchorEntityWithArCoreAnchor()

    override val defaultFakeExecutor: FakeScheduledExecutorService
        get() = fakeExecutor

    override fun createChildAndroidXrEntity(): AndroidXrEntity {
        return createGltfEntity()
    }

    override val activitySpaceEntity: ActivitySpaceImpl
        get() = activitySpace

    private fun createGltfEntity(): GltfEntityImpl {
        val mockGltfFeature = mock<GltfFeature>()
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.create().start().get()

        val nodeHolder = NodeHolder<Node>(xrExtensions.createNode(), Node::class.java)
        whenever(mockGltfFeature.getNodeHolder()).thenReturn(nodeHolder)

        return GltfEntityImpl(
            activity,
            mockGltfFeature,
            activitySpace,
            xrExtensions,
            sceneNodeRegistry,
            fakeExecutor,
        )
    }

    private fun createAnchorEntityWithArCoreAnchor(): AnchorEntityImpl {
        return createAnchorEntityWithArCoreAnchorAndReturnBoth().first
    }

    private fun createAnchorEntityWithArCoreAnchorAndReturnBoth(): Pair<AnchorEntityImpl, Anchor> {
        val node = xrExtensions.createNode()
        val anchorEntity =
            AnchorEntityImpl.create(
                activity,
                node,
                activitySpace,
                xrExtensions,
                sceneNodeRegistry,
                fakeExecutor,
            )
        val anchor = (Anchor.create(session, Pose.Identity) as AnchorCreateSuccess).anchor
        anchorEntity.setAnchor(anchor)
        return Pair(anchorEntity, anchor)
    }

    private fun createUnanchoredAnchorEntity(): AnchorEntityImpl {
        val node = (xrExtensions).createNode()
        return AnchorEntityImpl.create(
            activity,
            node,
            activitySpace,
            xrExtensions,
            sceneNodeRegistry,
            fakeExecutor,
        )
    }

    @Test
    fun anchorEntityAddChildren_addsChildren() {
        val childEntity1 = createGltfEntity()
        val childEntity2 = createGltfEntity()
        val parentEntity = createAnchorEntityWithArCoreAnchor()

        parentEntity.addChild(childEntity1)

        Truth.assertThat(parentEntity.children).containsExactly(childEntity1)

        parentEntity.addChildren(listOf(childEntity2))

        Truth.assertThat(childEntity1.parent).isEqualTo(parentEntity)
        Truth.assertThat(childEntity2.parent).isEqualTo(parentEntity)
        Truth.assertThat(parentEntity.children).containsExactly(childEntity1, childEntity2)
    }

    @Test
    fun anchorEntitySetPose_throwsException() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose()
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setPose(pose)
        }
    }

    @Test
    fun anchorEntityGetPoseRelativeToParentSpace_throwsException() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        Assert.assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.getPose(Space.PARENT)
        }
    }

    @Test
    fun anchorEntityGetPoseRelativeToActivitySpace_returnsActivitySpacePose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        assertPose(anchorEntity.getPose(Space.ACTIVITY), anchorEntity.activitySpacePose)
    }

    @Test
    fun anchorEntityGetPoseRelativeToRealWorldSpace_returnsPerceptionSpacePose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        assertPose(anchorEntity.getPose(Space.REAL_WORLD), anchorEntity.getPoseInPerceptionSpace())
    }

    @Test
    fun anchorEntitySetScale_throwsException() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val scale = Vector3(1f, 1f, 1f)
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.setScale(scale)
        }
    }

    @Test
    fun anchorEntityGetScale_throwsException() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        Assert.assertThrows(UnsupportedOperationException::class.java) { anchorEntity.getScale() }
    }

    @Test
    fun anchorEntityGetWorldSpaceScale_returnsIdentityScale() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        assertVector3(anchorEntity.worldSpaceScale, Vector3(1f, 1f, 1f))
    }

    @Test
    fun anchorEntityGetActivitySpaceScale_returnsInverseOfActivitySpace() {
        val activitySpaceScale = 5f
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromScale(activitySpaceScale))
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        assertVector3(anchorEntity.activitySpaceScale, Vector3.One)
    }

    @Test
    fun getActivitySpacePose_unanchored_returnsIdentityPose() {
        val anchorEntity = createUnanchoredAnchorEntity()
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        assertPose(anchorEntity.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_noActivitySpaceOpenXrReferenceSpacePose_returnsIdentityPose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))
        assertPose(anchorEntity.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_noAnchorOpenXrReferenceSpacePose_returnsIdentityPose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        // anchorEntity.setOpenXrReferenceSpacePose(..) is not called to set the underlying pose.
        assertPose(anchorEntity.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_whenAtSamePose_returnsIdentityPose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(pose.translation, pose.rotation, Vector3(2f, 2f, 2f))
        )
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        assertPose(anchorEntity.activitySpacePose, Pose())
    }

    @Test
    fun getActivitySpacePose_returnsDifferencePose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion(0f, 1f, 0f, 1f))
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        assertPose(anchorEntity.activitySpacePose, pose)
    }

    @Test
    fun getActivitySpacePose_withScaledAndRotatedActivitySpace_returnsDifferencePose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val activitySpaceQuaternion = Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f))
        val pose = Pose(Vector3(1f, 1f, 1f), Quaternion.Identity)
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))
        activitySpace.setOpenXrReferenceSpaceTransform(
            Matrix4.fromTrs(
                Vector3(2f, 3f, 4f),
                activitySpaceQuaternion,
                /* scale= */ Vector3(2f, 2f, 2f),
            )
        )
        // A 90-degree rotation around the z axis is a clockwise rotation of the XY plane.
        // ActivitySpace ignores scale, so we calculate difference pose assuming scale 1.
        val expectedPose =
            Pose(Vector3(-2.0f, 1.0f, -3.0f), Quaternion.fromEulerAngles(Vector3(0f, 0f, -90f)))

        assertPose(anchorEntity.activitySpacePose, expectedPose)
    }

    @Test
    fun transformPoseTo_withActivitySpace_returnsTransformedPose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)
        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        val anchorOffset =
            Pose(Vector3(10f, 0f, 0f), Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f)))
        val transformedPose = anchorEntity.transformPoseTo(anchorOffset, activitySpace)

        assertPose(
            transformedPose,
            Pose(Vector3(11f, 2f, 3f), Quaternion.fromEulerAngles(Vector3(0f, 0f, 90f))),
        )
    }

    @Test
    fun transformPoseTo_fromActivitySpaceChild_returnsAnchorSpacePose() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        val childEntity1 = createGltfEntity()
        val pose = Pose(Vector3(1f, 2f, 3f), Quaternion.Identity)
        val childPose = Pose(Vector3(-1f, -2f, -3f), Quaternion.Identity)

        activitySpace.setOpenXrReferenceSpaceTransform(Matrix4.Identity)
        activitySpace.addChild(childEntity1)
        childEntity1.setPose(childPose)
        anchorEntity.setOpenXrReferenceSpaceTransform(Matrix4.fromPose(pose))

        assertPose(
            activitySpace.transformPoseTo(Pose(), anchorEntity),
            Pose(Vector3(-1f, -2f, -3f), Quaternion.Identity),
        )

        val transformedPose = childEntity1.transformPoseTo(Pose(), anchorEntity)
        assertPose(transformedPose, Pose(Vector3(-2f, -4f, -6f), Quaternion.Identity))
    }

    @Test
    fun disposeAnchor_detachesAnchor() {
        val anchorEntityInfo = createAnchorEntityWithArCoreAnchorAndReturnBoth()
        val anchorEntity = anchorEntityInfo.first
        val anchor = anchorEntityInfo.second
        anchorEntity.setOnStateChangedListener(anchorStateListener)
        verify(anchorStateListener, never()).onStateChanged(AnchorEntity.State.ERROR)
        Truth.assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)

        // Verify the parent of the anchor is the root node (ActivitySpace) before disposing it.
        val anchorNode = anchorEntity.getNode()
        val rootNode = activitySpace.getNode()
        Truth.assertThat(NodeRepository.getInstance().getParent(anchorNode)).isEqualTo(rootNode)
        Truth.assertThat(NodeRepository.getInstance().getAnchorId(anchorNode))
            .isEqualTo(anchor.anchorToken)

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose()

        verify(anchorStateListener).onStateChanged(AnchorEntity.State.ERROR)
        Truth.assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ERROR)
        Truth.assertThat(NodeRepository.getInstance().getParent(anchorNode)).isNull()
        Truth.assertThat(NodeRepository.getInstance().getAnchorId(anchorNode)).isNull()
    }

    @Test
    fun disposeAnchorTwice_callsCallbackOnce() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()
        anchorEntity.setOnStateChangedListener(anchorStateListener)
        verify(anchorStateListener, never()).onStateChanged(AnchorEntity.State.ERROR)

        // Dispose the entity and verify that the state was updated.
        anchorEntity.dispose()

        verify(anchorStateListener).onStateChanged(AnchorEntity.State.ERROR)

        // Dispose anchor again and verify onStateChanged was called only once.
        anchorEntity.dispose()

        verify(anchorStateListener).onStateChanged(AnchorEntity.State.ERROR)
    }

    @Test
    fun getScaleRelativeToParentSpace_throwsException() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        Assert.assertThrows(UnsupportedOperationException::class.java) {
            anchorEntity.getScale(Space.PARENT)
        }
    }

    @Test
    fun getScaleRelativeToActivitySpace_returnsActivitySpaceScale() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        assertVector3(anchorEntity.getScale(Space.ACTIVITY), anchorEntity.activitySpaceScale)
    }

    @Test
    fun getScaleRelativeToRealWorldSpace_returnsVector3One() {
        val anchorEntity = createAnchorEntityWithArCoreAnchor()

        assertVector3(anchorEntity.getScale(Space.REAL_WORLD), Vector3(1f, 1f, 1f))
    }

    @Test
    fun setAnchor_unanchoredAnchorEntity_updatesState() {
        val anchorEntity = createUnanchoredAnchorEntity()
        Truth.assertThat(anchorEntity).isNotNull()
        Truth.assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.UNANCHORED)
        Truth.assertThat(NodeRepository.getInstance().getName(anchorEntity.getNode()))
            .isEqualTo(AnchorEntityImpl.ANCHOR_NODE_NAME)
        Truth.assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode())).isNull()
        Truth.assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode())).isNull()

        val anchor = (Anchor.create(session, Pose.Identity) as AnchorCreateSuccess).anchor
        anchorEntity.setAnchor(anchor)

        Truth.assertThat(anchorEntity.state).isEqualTo(AnchorEntity.State.ANCHORED)
        Truth.assertThat(NodeRepository.getInstance().getAnchorId(anchorEntity.getNode()))
            .isEqualTo(anchor.anchorToken)
        Truth.assertThat(NodeRepository.getInstance().getParent(anchorEntity.getNode()))
            .isEqualTo(activitySpace.getNode())
    }
}
