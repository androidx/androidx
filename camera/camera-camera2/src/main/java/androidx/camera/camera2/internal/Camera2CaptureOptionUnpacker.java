/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.OptionsBundle;
import androidx.camera.core.impl.UseCaseConfig;

/**
 * A {@link CaptureConfig.OptionUnpacker} implementation for unpacking Camera2 options into
 * {@link CaptureConfig.Builder}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class Camera2CaptureOptionUnpacker implements CaptureConfig.OptionUnpacker {

    static final Camera2CaptureOptionUnpacker INSTANCE = new Camera2CaptureOptionUnpacker();

    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @Override
    public void unpack(@NonNull UseCaseConfig<?> config,
            @NonNull final CaptureConfig.Builder builder) {
        CaptureConfig defaultCaptureConfig =
                config.getDefaultCaptureConfig(/*valueIfMissing=*/ null);

        Config implOptions = OptionsBundle.emptyBundle();
        int templateType = CaptureConfig.defaultEmptyCaptureConfig().getTemplateType();

        // Apply/extract defaults from session config
        if (defaultCaptureConfig != null) {
            templateType = defaultCaptureConfig.getTemplateType();
            builder.addAllCameraCaptureCallbacks(defaultCaptureConfig.getCameraCaptureCallbacks());
            implOptions = defaultCaptureConfig.getImplementationOptions();
        }

        // Set the any additional implementation options
        builder.setImplementationOptions(implOptions);

        // Get Camera2 extended options
        final Camera2ImplConfig camera2Config = new Camera2ImplConfig(config);

        // Apply template type
        builder.setTemplateType(camera2Config.getCaptureRequestTemplate(templateType));

        // Add extension callbacks
        builder.addCameraCaptureCallback(
                CaptureCallbackContainer.create(
                        camera2Config.getSessionCaptureCallback(
                                Camera2CaptureCallbacks.createNoOpCallback())));

        // Copy extension keys
        builder.addImplementationOptions(camera2Config.getCaptureRequestOptions());
    }
}
