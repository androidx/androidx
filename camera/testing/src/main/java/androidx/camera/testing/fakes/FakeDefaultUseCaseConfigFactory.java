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

import android.support.annotation.Nullable;
import androidx.camera.core.FakeUseCaseConfiguration;
import androidx.camera.core.UseCaseConfiguration;
import androidx.camera.core.UseCaseConfigurationFactory;

/**
 * A {@link androidx.camera.core.UseCaseConfigurationFactory} implementation that returns fake
 * configurations.
 */
public class FakeDefaultUseCaseConfigFactory implements UseCaseConfigurationFactory {

  /** Returns a fake configuration for fake use cases. */
  @Nullable
  @Override
  public <C extends UseCaseConfiguration<?>> C getConfiguration(Class<C> configType) {

    if (configType == FakeUseCaseConfiguration.class) {
      @SuppressWarnings("unchecked")
      C config = (C) new FakeUseCaseConfiguration.Builder().build();
      return config;
    }

    return null;
  }
}
