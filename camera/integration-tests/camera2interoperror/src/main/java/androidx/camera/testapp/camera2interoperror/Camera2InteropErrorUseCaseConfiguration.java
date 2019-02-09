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

package androidx.camera.testapp.camera2interoperror;

import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.Configuration;
import androidx.camera.core.ImageOutputConfiguration;
import androidx.camera.core.MutableConfiguration;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;
import androidx.camera.core.UseCaseConfiguration;

/** Configuration for the camera 2 interop case configuration */
public class Camera2InteropErrorUseCaseConfiguration
    implements UseCaseConfiguration<Camera2InteropErrorUseCase>,
        CameraDeviceConfiguration,
        ImageOutputConfiguration {

  private final Configuration config;

  private Camera2InteropErrorUseCaseConfiguration(Configuration config) {
    this.config = config;
  }

  @Override
  public Configuration getConfiguration() {
    return config;
  }

  /** Builder for an empty Configuration */
  public static final class Builder
      implements UseCaseConfiguration.Builder<
              Camera2InteropErrorUseCase, Camera2InteropErrorUseCaseConfiguration, Builder>,
          CameraDeviceConfiguration.Builder<Camera2InteropErrorUseCaseConfiguration, Builder>,
          ImageOutputConfiguration.Builder<Camera2InteropErrorUseCaseConfiguration, Builder> {

    private final MutableOptionsBundle optionsBundle;

    public Builder() {
      optionsBundle = MutableOptionsBundle.create();
      setTargetClass(Camera2InteropErrorUseCase.class);
    }

    @Override
    public MutableConfiguration getMutableConfiguration() {
      return optionsBundle;
    }

    @Override
    public Builder builder() {
      return this;
    }

    @Override
    public Camera2InteropErrorUseCaseConfiguration build() {
      return new Camera2InteropErrorUseCaseConfiguration(OptionsBundle.from(optionsBundle));
    }
  }

}
