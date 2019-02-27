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
import androidx.camera.core.BaseUseCase;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCaptureUseCase;
import androidx.camera.core.ImageCaptureUseCaseConfiguration;
import androidx.camera.core.VideoCaptureUseCase;
import androidx.camera.core.VideoCaptureUseCaseConfiguration;
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
    public void failedWhenBindTooManyImageCaptureUseCase() {
        ImageCaptureUseCaseConfiguration configuration =
                new ImageCaptureUseCaseConfiguration.Builder().build();
        ImageCaptureUseCase useCase1 = new ImageCaptureUseCase(configuration);
        ImageCaptureUseCase useCase2 = new ImageCaptureUseCase(configuration);

        // Should throw IllegalArgumentException
        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<BaseUseCase>singletonList(useCase1),
                Collections.<BaseUseCase>singletonList(useCase2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void failedWhenBindTooManyVideoCaptureUseCase() {
        VideoCaptureUseCaseConfiguration configuration =
                new VideoCaptureUseCaseConfiguration.Builder().build();
        VideoCaptureUseCase useCase1 = new VideoCaptureUseCase(configuration);
        VideoCaptureUseCase useCase2 = new VideoCaptureUseCase(configuration);

        // Should throw IllegalArgumentException
        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<BaseUseCase>singletonList(useCase1),
                Collections.<BaseUseCase>singletonList(useCase2));
    }

    @Test
    public void passWhenNotBindTooManyImageVideoCaptureUseCase() {
        ImageCaptureUseCaseConfiguration imageCaptureConfiguration =
                new ImageCaptureUseCaseConfiguration.Builder().build();
        ImageCaptureUseCase imageCaptureUseCase =
                new ImageCaptureUseCase(imageCaptureConfiguration);

        VideoCaptureUseCaseConfiguration videoCaptureConfiguration =
                new VideoCaptureUseCaseConfiguration.Builder().build();
        VideoCaptureUseCase videoCaptureUseCase =
                new VideoCaptureUseCase(videoCaptureConfiguration);

        UseCaseSurfaceOccupancyManager.checkUseCaseLimitNotExceeded(
                Collections.<BaseUseCase>singletonList(imageCaptureUseCase),
                Collections.<BaseUseCase>singletonList(videoCaptureUseCase));
    }
}
