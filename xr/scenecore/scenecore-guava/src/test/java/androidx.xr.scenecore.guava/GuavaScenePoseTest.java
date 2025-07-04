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

package androidx.xr.scenecore.guava;

import static androidx.xr.scenecore.guava.GuavaScenePose.hitTestAsync;
import static androidx.xr.scenecore.SessionExt.getScene;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.CameraView;
import androidx.xr.scenecore.Head;
import androidx.xr.scenecore.HitTestResult;
import androidx.xr.scenecore.PerceptionSpace;
import androidx.xr.scenecore.ScenePose.HitTestFilter;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GuavaScenePoseTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;
    private Head mHead;
    private CameraView mCamera;
    private PerceptionSpace mPerceptionSpace;

    private void createTestSessionAndRunTest(TestBodyRunnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario = ActivityScenario.launch(
                ComponentActivity.class)) {
            scenario.onActivity(activity -> {
                mTestDispatcher = StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                mSession = ((SessionCreateSuccess) Session.create(activity,
                        mTestDispatcher)).getSession();
                mHead = getScene(mSession).getSpatialUser().getHead();
                mCamera = getScene(mSession).getSpatialUser().getCameraViews().get(
                        CameraView.CameraType.LEFT_EYE);
                mPerceptionSpace = getScene(mSession).getPerceptionSpace();

                try {
                    testBody.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error during ActivityScenario setup or teardown", e);
        }
    }

    private void assertHitTestResult(HitTestResult result1, HitTestResult result2) {
        assertThat(result1.getHitPosition()).isEqualTo(result2.getHitPosition());
        assertThat(result1.getSurfaceType()).isEqualTo(result2.getSurfaceType());
        assertThat(result1.getSurfaceNormal()).isEqualTo(result2.getSurfaceNormal());
        assertThat(result1.getDistance()).isEqualTo(result2.getDistance());
    }

    @Test
    public void hitTestAsync_head_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            int hitTestFilter = HitTestFilter.SELF_SCENE;
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> headHitTestResultFuture =
                    hitTestAsync(mHead, mSession, origin, direction, hitTestFilter);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult headHitTestResult = headHitTestResultFuture.get();
            assertHitTestResult(headHitTestResult, expectedHitTestResult);
        });
    }

    @Test
    public void hitTestAsync_camera_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            int hitTestFilter = HitTestFilter.SELF_SCENE;
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> cameraHitTestResultFuture =
                    hitTestAsync(mCamera, mSession, origin, direction,
                            hitTestFilter);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult cameraHitTestResult = cameraHitTestResultFuture.get();
            assertHitTestResult(cameraHitTestResult, expectedHitTestResult);
        });
    }

    @Test
    public void hitTestAsync_perceptionSpace_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            int hitTestFilter = HitTestFilter.SELF_SCENE;
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> perceptionSpaceHitTestResultFuture =
                    hitTestAsync(mPerceptionSpace, mSession, origin, direction,
                            hitTestFilter);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult perceptionSpaceHitTestResult = perceptionSpaceHitTestResultFuture.get();
            assertHitTestResult(perceptionSpaceHitTestResult, expectedHitTestResult);
        });
    }

    @Test
    public void hitTestAsync_head_withDefaultHitTestFilter_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> headHitTestResultFuture =
                    hitTestAsync(mHead, mSession, origin, direction);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult headHitTestResult = headHitTestResultFuture.get();
            assertHitTestResult(headHitTestResult, expectedHitTestResult);
        });
    }

    @Test
    public void hitTestAsync_camera_withDefaultHitTestFilter_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> cameraHitTestResultFuture =
                    hitTestAsync(mCamera, mSession, origin, direction);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult cameraHitTestResult = cameraHitTestResultFuture.get();
            assertHitTestResult(cameraHitTestResult, expectedHitTestResult);
        });
    }

    @Test
    public void hitTestAsync_perceptionSpace_withDefaultHitTestFilter_returnsExpectedHTR() {
        createTestSessionAndRunTest(() -> {
            Vector3 origin = new Vector3(1f, 2f, 3f);
            Vector3 direction = new Vector3(4f, 5f, 6f);
            // TODO: b/424171690 - update once FakeActivityPose is more robust
            HitTestResult expectedHitTestResult =
                    new HitTestResult(/* hitPosition= */ null, /* surfaceNormal= */ null,
                            /* surfaceType= */ 0, /* distance= */ 0f);

            ListenableFuture<HitTestResult> perceptionSpaceHitTestResultFuture =
                    hitTestAsync(mPerceptionSpace, mSession, origin, direction);

            mTestDispatcher.getScheduler().advanceUntilIdle();
            HitTestResult perceptionSpaceHitTestResult = perceptionSpaceHitTestResultFuture.get();
            assertHitTestResult(perceptionSpaceHitTestResult, expectedHitTestResult);
        });
    }

    @FunctionalInterface
    private interface TestBodyRunnable {
        void run() throws Exception;
    }
}
