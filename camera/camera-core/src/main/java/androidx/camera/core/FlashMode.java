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

package androidx.camera.core;

/**
 * The flash mode options when taking a picture using ImageCapture.
 *
 * <p>Applications can check if flash is available and update UI component if necessary via
 * {@link CameraInfo#isFlashAvailable()}. If no flash is available, then the FlashMode set to
 * {@link ImageCapture#setFlashMode(FlashMode)} will take no effect for the subsequent photo
 * capture requests and they will act like {@link FlashMode#OFF}.
 *
 * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
 * will remain enabled during photo capture regardless of flash mode setting. When
 * the torch is disabled, flash will function as specified by
 * {@link ImageCapture#setFlashMode(FlashMode)}.
 */
public enum FlashMode {
    /**
     * Auto flash. The flash will be used according to the camera system's determination when taking
     * a picture.
     */
    AUTO,
    /** Always flash. The flash will always be used when taking a picture. */
    ON,
    /** No flash. The flash will never be used when taking a picture. */
    OFF
}
