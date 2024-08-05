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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Pair;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Stub advanced extender implementation for auto.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices.
 *
 * @since 1.2
 */
public class AutoAdvancedExtenderImpl implements AdvancedExtenderImpl {
    public AutoAdvancedExtenderImpl() {
    }

    @Override
    public boolean isExtensionAvailable(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public void init(@NonNull String cameraId,
            @NonNull Map<String, CameraCharacteristics> characteristicsMap) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @Nullable
    public Range<Long> getEstimatedCaptureLatencyRange(
            @NonNull String cameraId, @Nullable Size size, int imageFormat) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedPreviewOutputResolutions(
            @NonNull String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedCaptureOutputResolutions(
            @NonNull String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public Map<Integer, List<Size>> getSupportedPostviewResolutions(
            @NonNull Size captureSize) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @Nullable
    public List<Size> getSupportedYuvAnalysisResolutions(
            @NonNull String cameraId) {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public SessionProcessorImpl createSessionProcessor() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public List<CaptureRequest.Key> getAvailableCaptureRequestKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    @NonNull
    public List<CaptureResult.Key> getAvailableCaptureResultKeys() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public boolean isCaptureProcessProgressAvailable() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @Override
    public boolean isPostviewAvailable() {
        throw new RuntimeException("Stub, replace with implementation.");
    }

    @NonNull
    @Override
    public List<Pair<CameraCharacteristics.Key, Object>> getAvailableCharacteristicsKeyValues() {
        throw new RuntimeException("Stub, replace with implementation.");
    }
}