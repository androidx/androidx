/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.internal.compat.params;

import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

/**
 * Implementation of the OutputConfiguration compat methods for API 33 and above.
 */
@RequiresApi(33)
public class OutputConfigurationCompatApi33Impl extends OutputConfigurationCompatApi28Impl {

    OutputConfigurationCompatApi33Impl(@NonNull Surface surface) {
        super(new OutputConfiguration(surface));
    }

    OutputConfigurationCompatApi33Impl(int surfaceGroupId, @NonNull Surface surface) {
        this(new OutputConfiguration(surfaceGroupId, surface));
    }

    OutputConfigurationCompatApi33Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    @RequiresApi(33)
    static OutputConfigurationCompatApi33Impl wrap(
            @NonNull OutputConfiguration outputConfiguration) {
        return new OutputConfigurationCompatApi33Impl(outputConfiguration);
    }

    @Override
    public long getDynamicRangeProfile() {
        return ((OutputConfiguration) getOutputConfiguration()).getDynamicRangeProfile();
    }

    @Override
    public void setDynamicRangeProfile(long profile) {
        ((OutputConfiguration) getOutputConfiguration()).setDynamicRangeProfile(profile);
    }

    @NonNull
    @Override
    public Object getOutputConfiguration() {
        Preconditions.checkArgument(mObject instanceof OutputConfiguration);
        return mObject;
    }

    @Override
    public void setStreamUseCase(long streamUseCase) {
        if (streamUseCase == OutputConfigurationCompat.STREAM_USE_CASE_NONE) {
            return;
        }
        ((OutputConfiguration) getOutputConfiguration()).setStreamUseCase(streamUseCase);
    }

    /**
     * Get the current stream use case for this OutputConfiguration.
     */
    @Override
    public long getStreamUseCase() {
        return ((OutputConfiguration) getOutputConfiguration()).getStreamUseCase();
    }
}
