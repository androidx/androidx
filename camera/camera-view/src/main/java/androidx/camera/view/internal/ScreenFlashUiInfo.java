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

package androidx.camera.view.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.view.CameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.ScreenFlashView;

import java.util.Objects;

/**
 * Internal data class that encapsulates an {@link ImageCapture.ScreenFlashUiControl} and its
 * provider.
 */
public class ScreenFlashUiInfo {
    /**
     * Since {@link ImageCapture.ScreenFlashUiControl} can be created from either the
     * {@link ScreenFlashView} set by user or the one internally used in {@link PreviewView},
     * {@link CameraController} needs to know where exactly the control is from so that it can
     * prioritize the user-set one when both are available.
     */
    public enum ProviderType {
        PREVIEW_VIEW,
        SCREEN_FLASH_VIEW
    }

    @NonNull
    private final ProviderType mProviderType;

    @Nullable
    private final ImageCapture.ScreenFlashUiControl mScreenFlashUiControl;

    public ScreenFlashUiInfo(@NonNull ProviderType providerType,
            @Nullable ImageCapture.ScreenFlashUiControl screenFlashUiControl) {
        mProviderType = providerType;
        mScreenFlashUiControl = screenFlashUiControl;
    }

    @NonNull
    public ProviderType getProviderType() {
        return mProviderType;
    }

    @Nullable
    public ImageCapture.ScreenFlashUiControl getScreenFlashUiControl() {
        return mScreenFlashUiControl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScreenFlashUiInfo)) return false;
        ScreenFlashUiInfo that = (ScreenFlashUiInfo) o;
        return mProviderType == that.mProviderType && Objects.equals(mScreenFlashUiControl,
                that.mScreenFlashUiControl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mProviderType, mScreenFlashUiControl);
    }
}
