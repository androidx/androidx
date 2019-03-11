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

import androidx.annotation.Nullable;
import androidx.camera.core.CameraDeviceConfiguration;
import androidx.camera.core.CameraX;
import androidx.camera.core.Configuration;
import androidx.camera.core.MutableConfiguration;
import androidx.camera.core.MutableOptionsBundle;
import androidx.camera.core.OptionsBundle;
import androidx.camera.core.SessionConfiguration;
import androidx.camera.core.UseCaseConfiguration;

import java.util.Set;
import java.util.UUID;

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
            setLensFacing(CameraX.LensFacing.BACK);
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

        // Start of the default implementation of Configuration.Builder
        // *****************************************************************************************

        // Implementations of Configuration.Builder default methods

        @Override
        public <ValueT> Builder insertOption(Option<ValueT> opt, ValueT value) {
            getMutableConfiguration().insertOption(opt, value);
            return builder();
        }

        @Override
        @Nullable
        public <ValueT> Builder removeOption(Option<ValueT> opt) {
            getMutableConfiguration().removeOption(opt);
            return builder();
        }

        // Implementations of TargetConfiguration.Builder default methods

        @Override
        public Builder setTargetClass(Class<FakeUseCase> targetClass) {
            getMutableConfiguration().insertOption(OPTION_TARGET_CLASS, targetClass);

            // If no name is set yet, then generate a unique name
            if (null == getMutableConfiguration().retrieveOption(OPTION_TARGET_NAME, null)) {
                String targetName = targetClass.getCanonicalName() + "-" + UUID.randomUUID();
                setTargetName(targetName);
            }

            return builder();
        }

        @Override
        public Builder setTargetName(String targetName) {
            getMutableConfiguration().insertOption(OPTION_TARGET_NAME, targetName);
            return builder();
        }

        // Implementations of CameraDeviceConfiguration.Builder default methods

        @Override
        public Builder setLensFacing(CameraX.LensFacing lensFacing) {
            getMutableConfiguration().insertOption(OPTION_LENS_FACING, lensFacing);
            return builder();
        }

        // Implementations of UseCaseConfiguration.Builder default methods

        @Override
        public Builder setDefaultSessionConfiguration(SessionConfiguration sessionConfig) {
            getMutableConfiguration().insertOption(OPTION_DEFAULT_SESSION_CONFIG, sessionConfig);
            return builder();
        }

        @Override
        public Builder setOptionUnpacker(SessionConfiguration.OptionUnpacker optionUnpacker) {
            getMutableConfiguration().insertOption(OPTION_CONFIG_UNPACKER, optionUnpacker);
            return builder();
        }

        @Override
        public Builder setSurfaceOccupancyPriority(int priority) {
            getMutableConfiguration().insertOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, priority);
            return builder();
        }

        // End of the default implementation of Configuration.Builder
        // *****************************************************************************************
    }

    // Start of the default implementation of Configuration
    // *********************************************************************************************

    // Implementations of Configuration.Reader default methods

    @Override
    public boolean containsOption(Option<?> id) {
        return getConfiguration().containsOption(id);
    }

    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id) {
        return getConfiguration().retrieveOption(id);
    }

    @Override
    @Nullable
    public <ValueT> ValueT retrieveOption(Option<ValueT> id, @Nullable ValueT valueIfMissing) {
        return getConfiguration().retrieveOption(id, valueIfMissing);
    }

    @Override
    public void findOptions(String idStem, OptionMatcher matcher) {
        getConfiguration().findOptions(idStem, matcher);
    }

    @Override
    public Set<Option<?>> listOptions() {
        return getConfiguration().listOptions();
    }

    // Implementations of TargetConfiguration default methods

    @Override
    @Nullable
    public Class<FakeUseCase> getTargetClass(
            @Nullable Class<FakeUseCase> valueIfMissing) {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<FakeUseCase> storedClass = (Class<FakeUseCase>) retrieveOption(
                OPTION_TARGET_CLASS,
                valueIfMissing);
        return storedClass;
    }

    @Override
    public Class<FakeUseCase> getTargetClass() {
        @SuppressWarnings("unchecked") // Value should only be added via Builder#setTargetClass()
                Class<FakeUseCase> storedClass = (Class<FakeUseCase>) retrieveOption(
                OPTION_TARGET_CLASS);
        return storedClass;
    }

    @Override
    @Nullable
    public String getTargetName(@Nullable String valueIfMissing) {
        return retrieveOption(OPTION_TARGET_NAME, valueIfMissing);
    }

    @Override
    public String getTargetName() {
        return retrieveOption(OPTION_TARGET_NAME);
    }

    // Implementations of CameraDeviceConfiguration default methods

    @Override
    @Nullable
    public CameraX.LensFacing getLensFacing(@Nullable CameraX.LensFacing valueIfMissing) {
        return retrieveOption(OPTION_LENS_FACING, valueIfMissing);
    }

    @Override
    public CameraX.LensFacing getLensFacing() {
        return retrieveOption(OPTION_LENS_FACING);
    }

    // Implementations of UseCaseConfiguration default methods

    @Override
    @Nullable
    public SessionConfiguration getDefaultSessionConfiguration(
            @Nullable SessionConfiguration valueIfMissing) {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG, valueIfMissing);
    }

    @Override
    public SessionConfiguration getDefaultSessionConfiguration() {
        return retrieveOption(OPTION_DEFAULT_SESSION_CONFIG);
    }

    @Override
    @Nullable
    public SessionConfiguration.OptionUnpacker getOptionUnpacker(
            @Nullable SessionConfiguration.OptionUnpacker valueIfMissing) {
        return retrieveOption(OPTION_CONFIG_UNPACKER, valueIfMissing);
    }

    @Override
    public SessionConfiguration.OptionUnpacker getOptionUnpacker() {
        return retrieveOption(OPTION_CONFIG_UNPACKER);
    }

    @Override
    public int getSurfaceOccupancyPriority(int valueIfMissing) {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY, valueIfMissing);
    }

    @Override
    public int getSurfaceOccupancyPriority() {
        return retrieveOption(OPTION_SURFACE_OCCUPANCY_PRIORITY);
    }

    // End of the default implementation of Configuration
    // *********************************************************************************************
}
