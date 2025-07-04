/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.internal;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.SessionConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * No-Op implementation for {@link ZslControl}.
 */
public class ZslControlNoOpImpl implements ZslControl {
    @Override
    public void addZslConfig(SessionConfig.@NonNull Builder sessionConfigBuilder) {
    }

    @Override
    public void clearZslConfig() {
    }

    @Override
    public void setZslDisabledByUserCaseConfig(boolean disabled) {
    }

    @Override
    public boolean isZslDisabledByUserCaseConfig() {
        return false;
    }

    @Override
    public void setZslDisabledByFlashMode(boolean disabled) {
    }

    @Override
    public boolean isZslDisabledByFlashMode() {
        return false;
    }

    @Override
    public @Nullable ImageProxy dequeueImageFromBuffer() {
        return null;
    }

    @Override
    public boolean enqueueImageToImageWriter(@NonNull ImageProxy imageProxy) {
        return false;
    }
}
