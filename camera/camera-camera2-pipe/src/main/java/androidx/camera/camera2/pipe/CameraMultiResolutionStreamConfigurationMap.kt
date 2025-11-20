/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.hardware.camera2.params.MultiResolutionStreamInfo
import androidx.annotation.RestrictTo

/**
 * A compatibility wrapper for
 * [android.hardware.camera2.params.MultiResolutionStreamConfigurationMap].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface CameraMultiResolutionStreamConfigurationMap : UnsafeWrapper {
    /**
     * @see android.hardware.camera2.params.MultiResolutionStreamConfigurationMap.getOutputFormats
     */
    public fun getOutputFormats(): List<StreamFormat>

    /**
     * @see android.hardware.camera2.params.MultiResolutionStreamConfigurationMap.getInputFormats
     */
    public fun getInputFormats(): List<StreamFormat>

    /** @see android.hardware.camera2.params.MultiResolutionStreamConfigurationMap.getOutputInfo */
    public fun getOutputInfo(format: StreamFormat): List<MultiResolutionStreamInfo>

    /** @see android.hardware.camera2.params.MultiResolutionStreamConfigurationMap.getInputInfo */
    public fun getInputInfo(format: StreamFormat): List<MultiResolutionStreamInfo>
}
