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

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import static org.junit.Assert.fail;

import android.os.Build;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.scenecore.GltfModel;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class GuavaGltfModelTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;

    private void createTestSessionAndRunTest(GuavaGltfModelTest.TestBodyRunnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario =
                ActivityScenario.launch(ComponentActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        mTestDispatcher = StandardTestDispatcher(null, null);
                        mSession =
                                ((SessionCreateSuccess) Session.create(activity, mTestDispatcher))
                                        .getSession();

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

    @Test
    public void gltfModel_createAsync_fromAbsolutePathFailed() {
        createTestSessionAndRunTest(
                () -> {
                    ListenableFuture<GltfModel> gltfModelFuture = null;

                    String hardcodedPathString = "/data/data/com.example.myapp/myfolder/myfile.txt";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Path absolutePath = Paths.get(hardcodedPathString);
                        gltfModelFuture =
                                GuavaGltfModel.createGltfModelAsync(mSession, absolutePath);
                    }

                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    ExecutionException outerException = null;
                    try {
                        gltfModelFuture.get();
                        fail("Expected ExecutionException to be thrown");
                    } catch (ExecutionException e) {
                        outerException = e;
                    }

                    assertThat(outerException).isNotNull();
                    assertThat(outerException.getCause())
                            .isInstanceOf(IllegalArgumentException.class);
                    assertThat(outerException.getCause().getMessage())
                            .contains(
                                    "GltfModel.create() expects a path relative to `assets/`,"
                                            + " received absolute");
                });
    }

    @Test
    public void gltfModel_createExtAsync() {
        createTestSessionAndRunTest(
                () -> {
                    ListenableFuture<GltfModel> gltfModelFuture = null;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        gltfModelFuture =
                                GuavaGltfModel.createGltfModelAsync(
                                        mSession, Paths.get("FakeAsset.glb"));
                    }

                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    GltfModel gltfModelResult = gltfModelFuture.get();

                    assertThat(gltfModelResult).isInstanceOf(GltfModel.class);
                });
    }

    @FunctionalInterface
    private interface TestBodyRunnable {
        void run() throws Exception;
    }
}
