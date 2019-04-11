/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.camera2;

import android.content.Context;

import androidx.camera.core.AppConfiguration;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfiguration;
import androidx.camera.core.UseCase;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCaptureConfiguration;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

/** JUnit test cases for UseCaseSurfaceOccupancyManager class. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public final class UseCaseSurfaceOccupancyManagerTest {

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AppConfiguration appConfig = Camera2AppConfiguration.create(context);
        CameraX.init(context, appConfig);
    }

    @Test(expected = IllegalArgumentException.class)
    public void failedWhenBindTooManyImageCapture() {
        ImageCaptureConfiguration configuration = new ImageCaptureConfiguration.Builder().build();
        ImageCapture useCase1 = new ImageCapture(configuration);
        ImageCapture useCase2 = new ImageCapture(configuration);

        // Should throw IllegalArgumentException
        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<UseCase>singletonList(useCase1),
                Collections.<UseCase>singletonList(useCase2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failedWhenBindTooManyVideoCapture() {
        VideoCaptureConfiguration configuration = new VideoCaptureConfiguration.Builder().build();
        VideoCapture useCase1 = new VideoCapture(configuration);
        VideoCapture useCase2 = new VideoCapture(configuration);

        // Should throw IllegalArgumentException
        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<UseCase>singletonList(useCase1),
                Collections.<UseCase>singletonList(useCase2));
    }

    @Test
    public void passWhenNotBindTooManyImageVideoCapture() {
        ImageCaptureConfiguration imageCaptureConfiguration =
                new ImageCaptureConfiguration.Builder().build();
        ImageCapture imageCapture = new ImageCapture(imageCaptureConfiguration);

        VideoCaptureConfiguration videoCaptureConfiguration =
                new VideoCaptureConfiguration.Builder().build();
        VideoCapture videoCapture = new VideoCapture(videoCaptureConfiguration);

        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<UseCase>singletonList(imageCapture),
                Collections.<UseCase>singletonList(videoCapture));
    }
}
