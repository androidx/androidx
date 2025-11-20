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

package androidx.xr.scenecore.impl.impress;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.concurrent.TimeUnit.SECONDS;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.xr.runtime.math.BoundingBox;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

/** JNI Marshaling Tests for the Impress API bindings. */
@SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4.class)
public class GltfJniMarshallingTest extends BaseJniMarshallingTest {
    private static final String TEST_GLTF_PATH = "models/model.glb";
    private static final String TEST_GLTF_KEY = "model_key";
    private static final long TEST_NATIVE_TOKEN = 12345L;

    private static final int TEST_NODE_ID = 1;

    private static final String TEST_ANIM_NAME = "dance";

    private static final String TEST_MESH_NODE_NAME = "hair";
    private static final int TEST_PRIMITIVE_INDEX = 1;

    private static final String TEST_ERROR_MESSAGE = "Test C++ Failure From Marshalling Test";

    @Test
    public void loadGltfAsset_marshalsPath_invokesCallbackOnSuccess() throws Exception {
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(TEST_GLTF_PATH);
        ImpressApiTestHelper.nativeSetLoadGltfAssetSuccess(TEST_NATIVE_TOKEN);

        ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(TEST_GLTF_PATH);
        GltfModel actualModel = future.get(5, SECONDS);

        Long actualToken = actualModel.getNativeHandle();
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void loadGltfAsset_fromByteArray_invokesCallbackOnSuccess() throws Exception {
        int testSize = 1024;
        byte[] testData = generateTestPattern(testSize);
        ImpressApiTestHelper.nativeSetExpectedLoadGltfAssetTestPattern(testSize, TEST_GLTF_KEY);
        ImpressApiTestHelper.nativeSetLoadGltfAssetSuccess(TEST_NATIVE_TOKEN);

        ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(testData, TEST_GLTF_KEY);
        GltfModel actualModel = future.get(5, SECONDS);

        Long actualToken = actualModel.getNativeHandle();
        assertThat(actualToken).isEqualTo(TEST_NATIVE_TOKEN);
    }

    @Test
    public void loadGltfAsset_marshalsPath_invokesCallbackOnFailure() {
        ImpressApiTestHelper.nativeSetExpectedLoadGltfPath(TEST_GLTF_PATH);
        ImpressApiTestHelper.nativeSetLoadGltfAssetFailure(TEST_ERROR_MESSAGE);

        ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(TEST_GLTF_PATH);
        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));

        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void loadGltfAsset_fromByteArray_invokesCallbackOnFailure() {
        int testSize = 1024;
        byte[] testData = generateTestPattern(testSize);
        ImpressApiTestHelper.nativeSetExpectedLoadGltfAssetTestPattern(testSize, TEST_GLTF_KEY);
        ImpressApiTestHelper.nativeSetLoadGltfAssetFailure(TEST_ERROR_MESSAGE);

        ListenableFuture<GltfModel> future = mImpressApi.loadGltfAsset(testData, TEST_GLTF_KEY);
        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));

        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void releaseGltfAsset_marshalsToken() {
        ImpressApiTestHelper.nativeSetExpectedReleaseGltfAsset(TEST_NATIVE_TOKEN);

        mImpressApi.releaseGltfAsset(TEST_NATIVE_TOKEN);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void instanceGltfModel_marshalsParams_returnsId() {
        boolean expectedCollider = true;
        ImpressApiTestHelper.nativeSetExpectedInstanceGltfModel(
                TEST_NATIVE_TOKEN, expectedCollider);
        ImpressApiTestHelper.nativeSetInstanceGltfModelSuccess(TEST_NODE_ID);

        ImpressNode node = mImpressApi.instanceGltfModel(TEST_NATIVE_TOKEN, expectedCollider);

        assertThat(node.getHandle()).isEqualTo(TEST_NODE_ID);
    }

    @Test
    public void setGltfModelColliderEnabled_marshalsParams() {
        boolean expectedCollider = false;
        ImpressApiTestHelper.nativeSetExpectedSetGltfModelColliderEnabled(
                TEST_NODE_ID, expectedCollider);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        mImpressApi.setGltfModelColliderEnabled(node, expectedCollider);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void animateGltfModel_marshalsParams_invokesOnComplete() throws Exception {
        boolean expectedLoop = true;
        ImpressApiTestHelper.nativeSetExpectedAnimateGltfModel(
                TEST_NODE_ID, TEST_ANIM_NAME, expectedLoop);
        ImpressApiTestHelper.nativeSetAnimateGltfModelSuccess();
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        ListenableFuture<Void> future =
                mImpressApi.animateGltfModel(node, TEST_ANIM_NAME, expectedLoop);

        // If calling get() on the future does not throw an exception it means onComplete was
        // called.
        future.get(5, SECONDS);
    }

    @Test
    public void animateGltfModel_marshalsParams_invokesOnFailure() {
        boolean expectedLoop = false;
        ImpressApiTestHelper.nativeSetExpectedAnimateGltfModel(
                TEST_NODE_ID, TEST_ANIM_NAME, expectedLoop);
        ImpressApiTestHelper.nativeSetAnimateGltfModelFailure(TEST_ERROR_MESSAGE);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        ListenableFuture<Void> future =
                mImpressApi.animateGltfModel(node, TEST_ANIM_NAME, expectedLoop);

        ExecutionException exception =
                assertThrows(ExecutionException.class, () -> future.get(5, SECONDS));
        assertThat(exception).hasCauseThat().isInstanceOf(Exception.class);
        assertThat(exception).hasMessageThat().contains(TEST_ERROR_MESSAGE);
    }

    @Test
    public void stopGltfModelAnimation_marshalsNodeId() {
        ImpressApiTestHelper.nativeSetExpectedStopGltfModelAnimation(TEST_NODE_ID);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        mImpressApi.stopGltfModelAnimation(node);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void getGltfModelBoundingBox_marshalsNodeId_returnsBox() {
        float[] expectedCenter = {1.0f, 2.0f, 3.0f};
        float[] expectedHalfExtents = {4.0f, 5.0f, 6.0f};
        ImpressApiTestHelper.nativeSetExpectedGetGltfModelLocalBounds(TEST_NODE_ID);
        ImpressApiTestHelper.nativeSetGetGltfModelLocalBoundsSuccess(
                expectedCenter, expectedHalfExtents);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        BoundingBox box = mImpressApi.getGltfModelBoundingBox(node);

        assertThat(box.getCenter().getX()).isEqualTo(expectedCenter[0]);
        assertThat(box.getCenter().getY()).isEqualTo(expectedCenter[1]);
        assertThat(box.getCenter().getZ()).isEqualTo(expectedCenter[2]);
        assertThat(box.getHalfExtents().getWidth()).isEqualTo(expectedHalfExtents[0]);
        assertThat(box.getHalfExtents().getHeight()).isEqualTo(expectedHalfExtents[1]);
        assertThat(box.getHalfExtents().getDepth()).isEqualTo(expectedHalfExtents[2]);
    }

    @Test
    public void setMaterialOverride_marshalsParams() {
        ImpressApiTestHelper.nativeSetExpectedSetMaterialOverride(
                TEST_NODE_ID, TEST_NATIVE_TOKEN, TEST_MESH_NODE_NAME, TEST_PRIMITIVE_INDEX);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        mImpressApi.setMaterialOverride(
                node, TEST_NATIVE_TOKEN, TEST_MESH_NODE_NAME, TEST_PRIMITIVE_INDEX);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }

    @Test
    public void clearMaterialOverride_marshalsParams() {
        ImpressApiTestHelper.nativeSetExpectedClearMaterialOverride(
                TEST_NODE_ID, TEST_MESH_NODE_NAME, TEST_PRIMITIVE_INDEX);
        ImpressNode node = new ImpressNode(TEST_NODE_ID);

        mImpressApi.clearMaterialOverride(node, TEST_MESH_NODE_NAME, TEST_PRIMITIVE_INDEX);

        // This JNI call does not return any data, so the only assertion is on the native side.
    }
}
