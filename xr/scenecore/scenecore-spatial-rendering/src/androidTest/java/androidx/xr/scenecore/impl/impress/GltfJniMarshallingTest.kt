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

package androidx.xr.scenecore.impl.impress

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** JNI Marshaling Tests for the Impress API bindings. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class GltfJniMarshallingTest : BaseJniMarshallingTest() {

    companion object {
        private const val TEST_GLTF_PATH = "models/model.glb"
        private const val TEST_GLTF_KEY = "model_key"
        private const val TEST_NATIVE_TOKEN = 12345L

        private const val TEST_NODE_ID = 1

        private const val TEST_ANIM_NAME = "dance"

        private const val TEST_MESH_NODE_NAME = "hair"
        private const val TEST_PRIMITIVE_INDEX = 1

        private const val TEST_ERROR_MESSAGE = "Test C++ Failure From Marshalling Test"
    }

    @Test
    fun loadGltfAsset_marshalsPath_invokesCallbackOnSuccess() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(TEST_GLTF_PATH)
        ImpressApiTestHelper.nativeSetLoadGltfAssetSuccess(TEST_NATIVE_TOKEN)

        val actualModel = mImpressApi.loadGltfAsset(TEST_GLTF_PATH)

        val actualToken = actualModel.nativeHandle
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun loadGltfAsset_fromByteArray_invokesCallbackOnSuccess() = runBlocking {
        val testSize = 1024
        val testData = generateTestPattern(testSize)
        ImpressApiTestHelper.nativeSetExpectedLoadGltfAssetTestPattern(testSize, TEST_GLTF_KEY)
        ImpressApiTestHelper.nativeSetLoadGltfAssetSuccess(TEST_NATIVE_TOKEN)

        val actualModel = mImpressApi.loadGltfAsset(testData, TEST_GLTF_KEY)

        val actualToken = actualModel.nativeHandle
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN)
    }

    @Test
    fun loadGltfAsset_marshalsPath_invokesCallbackOnFailure() = runBlocking {
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(TEST_GLTF_PATH)
        ImpressApiTestHelper.nativeSetLoadGltfAssetFailure(TEST_ERROR_MESSAGE)

        val exception = assertFailsWith<Exception> { mImpressApi.loadGltfAsset(TEST_GLTF_PATH) }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun loadGltfAsset_fromByteArray_invokesCallbackOnFailure() = runBlocking {
        val testSize = 1024
        val testData = generateTestPattern(testSize)
        ImpressApiTestHelper.nativeSetExpectedLoadGltfAssetTestPattern(testSize, TEST_GLTF_KEY)
        ImpressApiTestHelper.nativeSetLoadGltfAssetFailure(TEST_ERROR_MESSAGE)

        val exception =
            assertFailsWith<Exception> { mImpressApi.loadGltfAsset(testData, TEST_GLTF_KEY) }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun releaseGltfAsset_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedReleaseGltfAsset(TEST_NATIVE_TOKEN)

        mImpressApi.releaseGltfAsset(TEST_NATIVE_TOKEN)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun instanceGltfModel_marshalsParams_returnsId() {
        val expectedCollider = true
        ImpressApiTestHelper.nativeSetExpectedInstanceGltfModel(TEST_NATIVE_TOKEN, expectedCollider)
        ImpressApiTestHelper.nativeSetInstanceGltfModelSuccess(TEST_NODE_ID)

        val node = mImpressApi.instanceGltfModel(TEST_NATIVE_TOKEN, expectedCollider)

        assertThat(node.handle).isEqualTo(TEST_NODE_ID)
    }

    @Test
    fun setGltfModelColliderEnabled_marshalsParams() {
        val expectedCollider = false
        ImpressApiTestHelper.nativeSetExpectedSetGltfModelColliderEnabled(
            TEST_NODE_ID,
            expectedCollider,
        )
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.setGltfModelColliderEnabled(node, expectedCollider)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun setGltfReformAffordanceEnabled_marshalsParams() {
        val expectedEnabled = false
        val systemMovable = false
        ImpressApiTestHelper.nativeSetExpectedSetGltfReformAffordanceEnabled(
            TEST_NODE_ID,
            expectedEnabled,
            systemMovable,
        )
        val impressNode = ImpressNode(TEST_NODE_ID)

        mImpressApi.setGltfReformAffordanceEnabled(impressNode, expectedEnabled, systemMovable)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun animateGltfModelNew_marshalsParams_invokesOnComplete() {
        runBlocking {
            val expectedLoop = true
            ImpressApiTestHelper.nativeSetExpectedAnimateGltfModelNew(
                TEST_NODE_ID,
                TEST_ANIM_NAME,
                expectedLoop,
                0f,
                0f,
                0,
            )
            ImpressApiTestHelper.nativeSetAnimateGltfModelSuccess()
            val node = ImpressNode(TEST_NODE_ID)

            withTimeout(5000) {
                mImpressApi.animateGltfModelNew(node, TEST_ANIM_NAME, expectedLoop, 0f, 0f, 0)
            }
        }
    }

    // TODO: b/465818627 - Remove old animation APIs once all clients are
    // migrated to new animation system.
    @Test
    fun animateGltfModel_marshalsParams_invokesOnComplete() {
        runBlocking {
            val expectedLoop = true
            ImpressApiTestHelper.nativeSetExpectedAnimateGltfModel(
                TEST_NODE_ID,
                TEST_ANIM_NAME,
                expectedLoop,
            )
            ImpressApiTestHelper.nativeSetAnimateGltfModelSuccess()
            val node = ImpressNode(TEST_NODE_ID)

            withTimeout(5000) { mImpressApi.animateGltfModel(node, TEST_ANIM_NAME, expectedLoop) }
        }
    }

    @Test
    fun animateGltfModelNew_marshalsParams_invokesOnFailure() {
        val expectedLoop = false
        ImpressApiTestHelper.nativeSetExpectedAnimateGltfModelNew(
            TEST_NODE_ID,
            TEST_ANIM_NAME,
            expectedLoop,
            0f,
            0f,
            0,
        )
        ImpressApiTestHelper.nativeSetAnimateGltfModelFailure(TEST_ERROR_MESSAGE)
        val node = ImpressNode(TEST_NODE_ID)

        val exception =
            assertThrows(Exception::class.java) {
                runBlocking {
                    mImpressApi.animateGltfModelNew(node, TEST_ANIM_NAME, expectedLoop, 0f, 0f, 0)
                }
            }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    // TODO: b/465818627 - Remove old animation APIs once all clients are
    // migrated to new animation system.
    @Test
    fun animateGltfModel_marshalsParams_invokesOnFailure() {
        val expectedLoop = false
        ImpressApiTestHelper.nativeSetExpectedAnimateGltfModel(
            TEST_NODE_ID,
            TEST_ANIM_NAME,
            expectedLoop,
        )
        ImpressApiTestHelper.nativeSetAnimateGltfModelFailure(TEST_ERROR_MESSAGE)
        val node = ImpressNode(TEST_NODE_ID)

        val exception =
            assertThrows(Exception::class.java) {
                runBlocking { mImpressApi.animateGltfModel(node, TEST_ANIM_NAME, expectedLoop) }
            }

        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE)
    }

    @Test
    fun stopGltfModelAnimationNew_marshalsNodeId() {
        ImpressApiTestHelper.nativeSetExpectedStopGltfModelAnimationNew(TEST_NODE_ID, 0)
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.stopGltfModelAnimationNew(node, 0)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    // TODO: b/465818627 - Remove old animation APIs once all clients are
    // migrated to new animation system.
    @Test
    fun stopGltfModelAnimation_marshalsNodeId() {
        ImpressApiTestHelper.nativeSetExpectedStopGltfModelAnimation(TEST_NODE_ID)
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.stopGltfModelAnimation(node)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun toggleGltfModelAnimationNew_marshalsParams_invokesOnPause() {
        // Set toggle as false to pause the animation.
        val expectedToggle = false
        ImpressApiTestHelper.nativeSetExpectedToggleGltfModelAnimationNew(
            TEST_NODE_ID,
            expectedToggle,
            0,
        )
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.toggleGltfModelAnimationNew(node, expectedToggle, 0)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    // TODO: b/465818627 - Remove old animation APIs once all clients are
    // migrated to new animation system.
    @Test
    fun toggleGltfModelAnimation_marshalsParams_invokesOnPause() {
        // Set toggle as false to pause the animation.
        val expectedToggle = false
        ImpressApiTestHelper.nativeSetExpectedToggleGltfModelAnimation(TEST_NODE_ID, expectedToggle)
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.toggleGltfModelAnimation(node, expectedToggle)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun toggleGltfModelAnimationNew_marshalsParams_invokesOnResume() {
        // Set toggle as false to pause the animation.
        val expectedToggle = true
        ImpressApiTestHelper.nativeSetExpectedToggleGltfModelAnimationNew(
            TEST_NODE_ID,
            expectedToggle,
            0,
        )
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.toggleGltfModelAnimationNew(node, expectedToggle, 0)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    // TODO: b/465818627 - Remove old animation APIs once all clients are
    // migrated to new animation system.
    @Test
    fun toggleGltfModelAnimation_marshalsParams_invokesOnResume() {
        // Set toggle as false to pause the animation.
        val expectedToggle = true
        ImpressApiTestHelper.nativeSetExpectedToggleGltfModelAnimation(TEST_NODE_ID, expectedToggle)
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.toggleGltfModelAnimation(node, expectedToggle)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun getGltfModelAnimationDurationSeconds_marshalsNodeI_returnsDurationSeconds() {
        val expectedIndex = 1
        val expectedDurationSeconds = 3f
        ImpressApiTestHelper.nativeSetExpectedGetGltfModelAnimationDurationSeconds(
            TEST_NODE_ID,
            expectedIndex,
        )
        ImpressApiTestHelper.nativeSetGetGltfModelAnimationDurationSecondsSuccess(
            expectedDurationSeconds
        )
        val node = ImpressNode(TEST_NODE_ID)
        val animationDurationSeconds =
            mImpressApi.getGltfModelAnimationDurationSeconds(node, expectedIndex)

        assertThat(animationDurationSeconds).isEqualTo(expectedDurationSeconds)
    }

    @Test
    fun setGltfModelAnimationSpeed_marshalsParams() {
        val expectedSpeed = 1.0f
        val expectedChannelId = 1
        ImpressApiTestHelper.nativeSetExpectedSetGltfModelAnimationSpeed(
            TEST_NODE_ID,
            expectedSpeed,
            expectedChannelId,
        )
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.setGltfModelAnimationSpeed(node, expectedSpeed, expectedChannelId)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun setGltfModelAnimationPlaybackTime_marshalsParams() {
        val expectedPlaybackTime = 1.0f
        val expectedChannelId = 1
        ImpressApiTestHelper.nativeSetExpectedSetGltfModelAnimationPlaybackTime(
            TEST_NODE_ID,
            expectedPlaybackTime,
            expectedChannelId,
        )
        val node = ImpressNode(TEST_NODE_ID)

        mImpressApi.setGltfModelAnimationPlaybackTime(node, expectedPlaybackTime, expectedChannelId)

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    fun getGltfModelAnimationCount_marshalsNodeId() {
        ImpressApiTestHelper.nativeSetExpectedGetGltfModelAnimationCount(TEST_NODE_ID)
        ImpressApiTestHelper.nativeSetGetGltfModelAnimationCountSuccess(3)
        val node = ImpressNode(TEST_NODE_ID)
        val animationCount = mImpressApi.getGltfModelAnimationCount(node)

        assertThat(animationCount).isEqualTo(3)
    }

    @Test
    fun getGltfModelAnimationName_marshalsNodeId_returnsName() {
        val expectedIndex = 1
        ImpressApiTestHelper.nativeSetExpectedGetGltfModelAnimationName(TEST_NODE_ID, expectedIndex)
        ImpressApiTestHelper.nativeSetGetGltfModelAnimationNameSuccess(TEST_ANIM_NAME)
        val node = ImpressNode(TEST_NODE_ID)
        val animationName = mImpressApi.getGltfModelAnimationName(node, expectedIndex)

        assertThat(animationName).isEqualTo(TEST_ANIM_NAME)
    }

    @Test
    fun getGltfModelBoundingBox_marshalsNodeId_returnsBox() {
        val expectedCenter = floatArrayOf(1.0f, 2.0f, 3.0f)
        val expectedHalfExtents = floatArrayOf(4.0f, 5.0f, 6.0f)
        ImpressApiTestHelper.nativeSetExpectedGetGltfModelLocalBounds(TEST_NODE_ID)
        ImpressApiTestHelper.nativeSetGetGltfModelLocalBoundsSuccess(
            expectedCenter,
            expectedHalfExtents,
        )
        val node = ImpressNode(TEST_NODE_ID)

        val box = mImpressApi.getGltfModelBoundingBox(node)

        assertThat(box.center.x).isEqualTo(expectedCenter[0])
        assertThat(box.center.y).isEqualTo(expectedCenter[1])
        assertThat(box.center.z).isEqualTo(expectedCenter[2])
        assertThat(box.halfExtents.width).isEqualTo(expectedHalfExtents[0])
        assertThat(box.halfExtents.height).isEqualTo(expectedHalfExtents[1])
        assertThat(box.halfExtents.depth).isEqualTo(expectedHalfExtents[2])
    }
}
