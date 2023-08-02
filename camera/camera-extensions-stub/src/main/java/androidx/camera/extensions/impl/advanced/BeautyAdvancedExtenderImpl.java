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
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Range;
import android.util.Size;

import java.util.List;
import java.util.Map;

/**
 * Stub advanced extender implementation for beauty.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices.
 *
 * @since 1.2
 */
@SuppressLint("UnknownNullness")
public class BeautyAdvancedExtenderImpl implements AdvancedExtenderImpl {
    public BeautyAdvancedExtenderImpl() {
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            Map<String, CameraCharacteristics> characteristicsMap) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void init(String cameraId,
            Map<String, CameraCharacteristics> characteristicsMap) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public Range<Long> getEstimatedCaptureLatencyRange(
            String cameraId, Size size, int imageFormat) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public Map<Integer, List<Size>> getSupportedPreviewOutputResolutions(
            String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public Map<Integer, List<Size>> getSupportedCaptureOutputResolutions(
            String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public List<Size> getSupportedYuvAnalysisResolutions(
            String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public SessionProcessorImpl createSessionProcessor() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }
}
