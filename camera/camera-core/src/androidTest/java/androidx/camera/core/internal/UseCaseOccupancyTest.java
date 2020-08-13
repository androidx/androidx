/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.internal;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.VideoCapture;
import androidx.camera.testing.fakes.FakeAppConfig;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** JUnit test cases for {@link UseCaseOccupancy} class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseOccupancyTest {

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CameraXConfig cameraXConfig = CameraXConfig.Builder.fromConfig(
                FakeAppConfig.create()).build();
        CameraX.initialize(context, cameraXConfig).get();
    }

    @After
    public void tearDown() throws ExecutionException, InterruptedException, TimeoutException {
        CameraX.shutdown().get(10000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void failedWhenBindTooManyImageCapture() {
        ImageCapture useCase1 = createImageCapture();
        ImageCapture useCase2 = createImageCapture();

        assertThat(UseCaseOccupancy.checkUseCaseLimitNotExceeded(
                Arrays.asList(useCase1, useCase2))).isFalse();
    }

    @Test
    public void failedWhenBindTooManyVideoCapture() {
        VideoCapture useCase1 = new VideoCapture.Builder().build();
        VideoCapture useCase2 = new VideoCapture.Builder().build();

        assertThat(UseCaseOccupancy.checkUseCaseLimitNotExceeded(
                Arrays.asList(useCase1, useCase2))).isFalse();
    }

    @Test
    public void passWhenNotBindTooManyImageVideoCapture() {
        ImageCapture imageCapture = createImageCapture();
        VideoCapture videoCapture = new VideoCapture.Builder().build();

        assertThat(UseCaseOccupancy.checkUseCaseLimitNotExceeded(
                Arrays.asList(imageCapture, videoCapture))).isTrue();
    }

    // TODO remove when UseCase does not require
    private ImageCapture createImageCapture() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .setCaptureOptionUnpacker((config, builder) -> { })
                .setSessionOptionUnpacker((config, builder) -> { })
                .build();
    }
}
