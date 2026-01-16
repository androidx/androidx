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

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.runtime.Config;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.HitTestResult;
import androidx.xr.scenecore.Scene;
import androidx.xr.scenecore.ScenePose.HitTestFilter;
import androidx.xr.scenecore.SessionExt;
import androidx.xr.scenecore.testing.FakeActivitySpace;
import androidx.xr.scenecore.testing.FakeSceneRuntime;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class GuavaScenePoseTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;

    private FakeActivitySpace mFakeRtActivitySpace;

    private Scene mScene;

    private void createTestSessionAndRunTest(TestBodyRunnable testBody) {

        try (ActivityScenario<ComponentActivity> scenario =
                ActivityScenario.launch(ComponentActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        mTestDispatcher =
                                StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                        mSession =
                                ((SessionCreateSuccess) Session.create(activity, mTestDispatcher))
                                        .getSession();
                        mSession.configure(new Config());
                        FakeSceneRuntime fakeSceneRt =
                                (FakeSceneRuntime)
                                        mSession.getRuntimes().stream()
                                                .filter(rt -> rt instanceof FakeSceneRuntime)
                                                .findAny()
                                                .get();
                        mFakeRtActivitySpace = fakeSceneRt.getActivitySpace();
                        mScene = SessionExt.getScene(mSession);

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
    public void hitTestAsync_returnsExpectedHitTestResult() {
        createTestSessionAndRunTest(
                () -> {
                    Vector3 origin = new Vector3(1f, 2f, 3f);
                    Vector3 direction = new Vector3(4f, 5f, 6f);
                    Vector3 hitPosition = new Vector3(7f, 8f, 9f);
                    float distance = 7f;
                    Vector3 surfaceNormal = new Vector3(10f, 11f, 12f);
                    @androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceTypeValue
                    int surfaceType =
                            androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType
                                    .HIT_TEST_RESULT_SURFACE_TYPE_PLANE;

                    androidx.xr.scenecore.runtime.HitTestResult rtHitTestResult =
                            new androidx.xr.scenecore.runtime.HitTestResult(
                                    hitPosition, surfaceNormal, surfaceType, distance);
                    mFakeRtActivitySpace.setHitTestResult(rtHitTestResult);
                    HitTestResult expectedHitTestResult =
                            new HitTestResult(hitPosition, surfaceNormal, surfaceType, distance);

                    ListenableFuture<HitTestResult> hitTestResultFuture =
                            GuavaScenePose.hitTestAsync(
                                    mScene.getActivitySpace(),
                                    mSession,
                                    origin,
                                    direction,
                                    HitTestFilter.SELF_SCENE);

                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    HitTestResult hitTestResult = hitTestResultFuture.get();
                    assertThat(hitTestResult).isNotNull();
                    assertHitTestResult(hitTestResult, expectedHitTestResult);
                });
    }

    @Test
    public void hitTestAsync_returnsNullHitTestResult() {
        createTestSessionAndRunTest(
                () -> {
                    Vector3 origin = new Vector3(1f, 2f, 3f);
                    Vector3 direction = new Vector3(4f, 5f, 6f);
                    float distance = 7f;
                    Vector3 surfaceNormal = new Vector3(10f, 11f, 12f);
                    @androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceTypeValue
                    int surfaceType =
                            androidx.xr.scenecore.runtime.HitTestResult.HitTestSurfaceType
                                    .HIT_TEST_RESULT_SURFACE_TYPE_PLANE;

                    androidx.xr.scenecore.runtime.HitTestResult rtHitTestResult =
                            new androidx.xr.scenecore.runtime.HitTestResult(
                                    null, surfaceNormal, surfaceType, distance);
                    mFakeRtActivitySpace.setHitTestResult(rtHitTestResult);

                    ListenableFuture<HitTestResult> headHitTestResultFuture =
                            hitTestAsync(
                                    mScene.getActivitySpace(),
                                    mSession,
                                    origin,
                                    direction,
                                    HitTestFilter.SELF_SCENE);

                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    HitTestResult headHitTestResult = headHitTestResultFuture.get();
                    assertThat(headHitTestResult).isNull();
                });
    }

    @FunctionalInterface
    private interface TestBodyRunnable {
        void run() throws Exception;
    }
}
