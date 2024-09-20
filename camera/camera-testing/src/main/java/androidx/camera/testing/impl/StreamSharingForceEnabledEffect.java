/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.testing.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.UseCaseGroup;
import androidx.lifecycle.LifecycleOwner;

/**
 * An effect that is used to simulate the stream sharing is enabled automatically.
 *
 * <p>To simulate stream sharing is enabled automatically, create and add the effect to
 * {@link UseCaseGroup.Builder#addEffect(CameraEffect)} and then bind UseCases via
 * {@linkplain androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(
 * LifecycleOwner, CameraSelector, UseCaseGroup)}.
 *
 * <p>To test stream sharing with real effects, use {@link CameraEffect} API instead.
 */
public class StreamSharingForceEnabledEffect extends CameraEffect {

    public StreamSharingForceEnabledEffect() {
        this(0);
    }

    public StreamSharingForceEnabledEffect(@Targets int extraTargets) {
        super(PREVIEW | VIDEO_CAPTURE | extraTargets, TRANSFORMATION_PASSTHROUGH, command -> {
        }, new SurfaceProcessor() {
            @Override
            public void onInputSurface(@NonNull SurfaceRequest request) {
                request.willNotProvideSurface();
            }

            @Override
            public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
                surfaceOutput.close();
            }
        }, t -> {
        });
    }
}
