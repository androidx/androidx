/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.extensions.impl.advanced;

import android.annotation.SuppressLint;

import java.util.List;

/**
 * A config representing a {@link android.hardware.camera2.params.OutputConfiguration} where
 * Surface will be created by the information in this config.
 */
@SuppressLint("UnknownNullness")
public interface Camera2OutputConfigImpl {
    /**
     * Gets thd id of this output config. The id can be used to identify the stream in vendor
     * implementations.
     */
    int getId();

    /**
     * Gets the surface group id. Vendor can use the surface group id to share memory between
     * Surfaces.
     */
    int getSurfaceGroupId();

    /**
     * Gets the physical camera id. Returns null if not specified.
     */
    String getPhysicalCameraId();

    /**
     * If non-null, enable surface sharing and add the surface constructed by the return
     * Camera2OutputConfig.
     */
    List<Camera2OutputConfigImpl> getSurfaceSharingOutputConfigs();
}
