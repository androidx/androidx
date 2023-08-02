/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.extensions.internal;

import android.util.Pair;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Preview;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.ConfigProvider;
import androidx.camera.core.impl.PreviewConfig;
import androidx.camera.extensions.ExtensionMode;

import java.util.List;

/**
 * For providing extensions config for preview.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class PreviewConfigProvider implements ConfigProvider<PreviewConfig> {
    private static final String TAG = "PreviewConfigProvider";
    static final Config.Option<Integer> OPTION_PREVIEW_CONFIG_PROVIDER_MODE = Config.Option.create(
            "camerax.extensions.previewConfigProvider.mode", Integer.class);
    private final VendorExtender mVendorExtender;
    @ExtensionMode.Mode
    private final int mEffectMode;

    public PreviewConfigProvider(
            @ExtensionMode.Mode int mode,
            @NonNull VendorExtender vendorExtender) {
        mEffectMode = mode;
        mVendorExtender = vendorExtender;
    }

    @NonNull
    @Override
    public PreviewConfig getConfig() {
        Preview.Builder builder = new Preview.Builder();
        updateBuilderConfig(builder, mEffectMode, mVendorExtender);
        return builder.getUseCaseConfig();
    }

    /**
     * Update extension related configs to the builder.
     */
    void updateBuilderConfig(@NonNull Preview.Builder builder,
            @ExtensionMode.Mode int effectMode, @NonNull VendorExtender vendorExtender) {
        builder.getMutableConfig().insertOption(OPTION_PREVIEW_CONFIG_PROVIDER_MODE, effectMode);
        List<Pair<Integer, Size[]>> supportedResolutions =
                vendorExtender.getSupportedPreviewOutputResolutions();
        builder.setSupportedResolutions(supportedResolutions);
        builder.setHighResolutionDisabled(true);
    }
}
