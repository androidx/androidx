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

package androidx.camera.testing.fakes;

import android.content.Context;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.AppConfig;
import androidx.camera.core.CameraDeviceSurfaceManager;
import androidx.camera.core.CameraFactory;
import androidx.camera.core.CameraX;
import androidx.camera.core.ExtendableUseCaseConfigFactory;
import androidx.camera.core.UseCaseConfigFactory;

/**
 * Convenience class for generating a fake {@link AppConfig}.
 *
 * <p>This {@link AppConfig} contains all fake CameraX implementation components.
 * @hide Hidden until {@link CameraX#init(Context, AppConfig)} is public.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeAppConfig {

    /** Generates a fake {@link AppConfig}. */
    public static AppConfig create() {
        CameraFactory cameraFactory = new FakeCameraFactory();
        CameraDeviceSurfaceManager surfaceManager = new FakeCameraDeviceSurfaceManager();
        UseCaseConfigFactory defaultConfigFactory = new ExtendableUseCaseConfigFactory();

        AppConfig.Builder appConfigBuilder =
                new AppConfig.Builder()
                        .setCameraFactory(cameraFactory)
                        .setDeviceSurfaceManager(surfaceManager)
                        .setUseCaseConfigFactory(defaultConfigFactory);

        return appConfigBuilder.build();
    }
}
