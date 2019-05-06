/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.camera2.impl.compat.params;

import android.hardware.camera2.params.OutputConfiguration;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of the OutputConfiguration compat methods for API 24 and above.
 */
@RequiresApi(24)
class OutputConfigurationCompatApi24Impl extends OutputConfigurationCompatBaseImpl{

    OutputConfigurationCompatApi24Impl(@NonNull Surface surface) {
        super(new OutputConfiguration(surface));
    }

    OutputConfigurationCompatApi24Impl(@NonNull Object outputConfiguration) {
        super(outputConfiguration);
    }

    @Override
    @Nullable
    public Surface getSurface() {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        return outputConfig.getSurface();
    }

    @Override
    @NonNull
    public List<Surface> getSurfaces() {
        return Collections.singletonList(getSurface());
    }

    @Override
    public int getSurfaceGroupId() {
        OutputConfiguration outputConfig = (OutputConfiguration) mObject;
        return outputConfig.getSurfaceGroupId();
    }

    @Nullable
    @Override
    public Object getOutputConfiguration() {
        Preconditions.checkArgument(mObject instanceof OutputConfiguration);
        return mObject;
    }
}

