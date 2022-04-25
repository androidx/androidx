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

package androidx.camera.extensions.impl;

import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Map;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class NoOpCaptureProcessorImpl implements CaptureProcessorImpl {
    @Override
    public void onOutputSurface(@NonNull Surface surface, int imageFormat) {

    }

    @Override
    public void process(@NonNull Map<Integer, Pair<Image, TotalCaptureResult>> results) {

    }

    @Override
    public void onResolutionUpdate(@NonNull Size size) {

    }

    @Override
    public void onImageFormatUpdate(int imageFormat) {

    }
}
