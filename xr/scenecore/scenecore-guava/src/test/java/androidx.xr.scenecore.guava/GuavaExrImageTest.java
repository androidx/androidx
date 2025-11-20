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

import static androidx.xr.scenecore.guava.GuavaExrImage.createExrImageFromZipAsync;

import static com.google.common.truth.Truth.assertThat;

import static kotlinx.coroutines.test.TestCoroutineDispatchersKt.StandardTestDispatcher;

import static org.junit.Assert.fail;

import android.os.Build;

import androidx.activity.ComponentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.xr.runtime.Session;
import androidx.xr.runtime.SessionCreateSuccess;
import androidx.xr.scenecore.ExrImage;

import com.google.common.util.concurrent.ListenableFuture;

import kotlinx.coroutines.test.TestDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class GuavaExrImageTest {
    private Session mSession;
    private TestDispatcher mTestDispatcher;

    @Test
    public void exrImage_createExrImageFromZipAsync_failsForExrFile() {
        createTestSessionAndRunTest(
                () -> {
                    ListenableFuture<ExrImage> exrImageFuture = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        exrImageFuture =
                                createExrImageFromZipAsync(mSession, Paths.get("test.exr"));
                    }
                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    ExecutionException outerException = null;

                    try {
                        exrImageFuture.get();
                        fail("Expected ExecutionException to be thrown");
                    } catch (ExecutionException e) {
                        outerException = e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    assertThat(outerException).isNotNull();
                    assertThat(outerException.getCause())
                            .isInstanceOf(IllegalArgumentException.class);
                    assertThat(outerException.getCause().getMessage())
                            .contains(
                                    "Only preprocessed skybox files with the .zip extension are"
                                            + " supported.");
                });
    }

    @Test
    public void exrImage_createExrImageFromZipAsync_withZipExtension_passes() {
        createTestSessionAndRunTest(
                () -> {
                    // TODO: b/424171690 - update once JxrPlatformAdapter.loadExrImageByByteArray
                    //  is more robust
                    ListenableFuture<ExrImage> exrImageFuture = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        exrImageFuture =
                                createExrImageFromZipAsync(mSession, Paths.get("test.zip"));
                    }

                    mTestDispatcher.getScheduler().advanceUntilIdle();
                    try {
                        ExrImage exrImageResult = exrImageFuture.get();

                        assertThat(exrImageResult).isInstanceOf(ExrImage.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void createTestSessionAndRunTest(Runnable testBody) {
        try (ActivityScenario<ComponentActivity> scenario =
                ActivityScenario.launch(ComponentActivity.class)) {
            scenario.onActivity(
                    activity -> {
                        mTestDispatcher =
                                StandardTestDispatcher(/* scheduler= */ null, /* name= */ null);
                        mSession =
                                ((SessionCreateSuccess) Session.create(activity, mTestDispatcher))
                                        .getSession();

                        try {
                            testBody.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
