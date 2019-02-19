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

import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.Configuration;
import androidx.camera.core.MutableConfiguration;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;
import androidx.camera.core.UseCaseConfiguration;

/** A fake configuration for {@link FakeUseCase}. */
public class FakeUseCaseConfiguration
        implements UseCaseConfiguration<FakeUseCase>, CameraDeviceConfiguration {

    private final Configuration mConfig;

    FakeUseCaseConfiguration(Configuration config) {
        mConfig = config;
    }

    @Override
    public Configuration getConfiguration() {
        return mConfig;
    }

    /** Builder for an empty Configuration */
    public static final class Builder
            implements UseCaseConfiguration.Builder<FakeUseCase, FakeUseCaseConfiguration, Builder>,
            CameraDeviceConfiguration.Builder<FakeUseCaseConfiguration, Builder> {

        private final MutableOptionsBundle mOptionsBundle;

        public Builder() {
            mOptionsBundle = MutableOptionsBundle.create();
            setTargetClass(FakeUseCase.class);
        }

        @Override
        public MutableConfiguration getMutableConfiguration() {
            return mOptionsBundle;
        }

        @Override
        public Builder builder() {
            return this;
        }

        @Override
        public FakeUseCaseConfiguration build() {
            return new FakeUseCaseConfiguration(OptionsBundle.from(mOptionsBundle));
        }
    }
}
