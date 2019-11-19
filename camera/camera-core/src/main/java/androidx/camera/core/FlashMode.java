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

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The flash mode options when taking a picture using ImageCapture.
 *
 * <p>Applications can check if there is a flash unit via {@link CameraInfo#hasFlashUnit()} and
 * update UI component if necessary. If there is no flash unit, then the FlashMode set to
 * {@link ImageCapture#setFlashMode(int)} will take no effect for the subsequent photo capture
 * requests and they will act like {@link FlashMode#OFF}.
 *
 * <p>When the torch is enabled via {@link CameraControl#enableTorch(boolean)}, the torch
 * will remain enabled during photo capture regardless of flash mode setting. When
 * the torch is disabled, flash will function as specified by
 * {@link ImageCapture#setFlashMode(int)}.
 */
@IntDef({FlashMode.AUTO, FlashMode.ON, FlashMode.OFF})
@Retention(RetentionPolicy.SOURCE)
public @interface FlashMode {
    /**
     * Auto flash. The flash will be used according to the camera system's determination when taking
     * a picture.
     */
    int AUTO = 0;
    /** Always flash. The flash will always be used when taking a picture. */
    int ON = 1;
    /** No flash. The flash will never be used when taking a picture. */
    int OFF = 2;
}