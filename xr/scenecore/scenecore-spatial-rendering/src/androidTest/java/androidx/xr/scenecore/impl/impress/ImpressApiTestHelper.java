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

/** Helper class that provides JNI hooks for testing the Impress API bindings. */
final class ImpressApiTestHelper {
    private ImpressApiTestHelper() {}

    // Hooks for test state management.
    static native long nativeCreateTestView();

    static native void nativeDestroyTestView(long viewHandle);

    static native void nativeResetTestState();

    // Hooks for glTF operations.
    static native void nativeSetExpectedLoadGltfPath(String path);

    static native void nativeSetLoadGltfAssetSuccess(long token);

    static native void nativeSetLoadGltfAssetFailure(String message);

    static native void nativeSetExpectedLoadGltfAssetTestPattern(int size, String key);

    static native void nativeSetExpectedReleaseGltfAsset(long token);

    static native void nativeSetExpectedInstanceGltfModel(long token, boolean enableCollider);

    static native void nativeSetInstanceGltfModelSuccess(int nodeId);

    static native void nativeSetExpectedSetGltfModelColliderEnabled(
            int nodeId, boolean enableCollider);

    static native void nativeSetExpectedSetGltfReformAffordanceEnabled(
            int impressNodeId, boolean enabled, boolean systemMovable);

    static native void nativeSetExpectedAnimateGltfModel(
            int nodeId,
            String animationName,
            boolean loop,
            float speed,
            float startTime,
            int channelId);

    static native void nativeSetAnimateGltfModelSuccess();

    static native void nativeSetAnimateGltfModelFailure(String message);

    static native void nativeSetExpectedStopGltfModelAnimation(int nodeId, int channelId);

    static native void nativeSetExpectedToggleGltfModelAnimation(
            int nodeId, boolean toggle, int channelId);

    static native void nativeSetExpectedGetGltfModelAnimationDurationSeconds(int nodeId, int index);

    static native void nativeSetGetGltfModelAnimationDurationSecondsSuccess(float duration);

    static native void nativeSetExpectedSetGltfModelAnimationSpeed(
            int nodeId, float speed, int channelId);

    static native void nativeSetExpectedSetGltfModelAnimationPlaybackTime(
            int nodeId, float playbackTime, int channelId);

    static native void nativeSetExpectedGetGltfModelAnimationCount(int nodeId);

    static native void nativeSetGetGltfModelAnimationCountSuccess(int count);

    static native void nativeSetExpectedGetGltfModelAnimationName(int nodeId, int index);

    static native void nativeSetGetGltfModelAnimationNameSuccess(String name);

    static native void nativeSetExpectedGetGltfModelLocalBounds(int nodeId);

    static native void nativeSetGetGltfModelLocalBoundsSuccess(float[] center, float[] halfExtents);

    // Hooks for the skybox operations.
    static native void nativeSetExpectedLoadIblPath(String path);

    static native void nativeSetLoadIblAssetSuccess(long token);

    static native void nativeSetLoadIblAssetFailure(String message);

    static native void nativeSetExpectedLoadIblAssetTestPattern(int size, String key);

    static native void nativeSetExpectedReleaseIblAsset(long token);

    static native void nativeSetExpectedSetEnvironmentLight(long token);

    static native void nativeSetExpectedClearEnvironmentLight();

    // Hooks for the texture operations.
    static native void nativeSetExpectedLoadTexturePath(String path);

    static native void nativeSetLoadTextureAssetSuccess(long token);

    static native void nativeSetLoadTextureAssetFailure(String message);

    static native void nativeSetExpectedBorrowReflectionTexture();

    static native void nativeSetBorrowReflectionTextureSuccessToken(long token);

    static native void nativeSetExpectedGetReflectionTextureFromIbl(long iblToken);

    static native void nativeSetGetReflectionTextureFromIblSuccessToken(long textureToken);

    static native void nativeSetExpectedBorrowTexture(long textureHandle);
}
