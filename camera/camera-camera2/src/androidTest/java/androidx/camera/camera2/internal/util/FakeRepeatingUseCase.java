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

package androidx.camera.camera2.internal.util;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.SessionConfig;
import androidx.camera.core.UseCaseConfig;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;

import java.util.Map;

/**
 * A fake {@link FakeUseCase} which contain a repeating surface.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class FakeRepeatingUseCase extends FakeUseCase {

    /** The repeating surface. */
    private final ImageReader mImageReader =
            ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);

    public FakeRepeatingUseCase(@NonNull FakeUseCaseConfig configuration,
            @NonNull CameraSelector cameraSelector) {
        super(configuration);

        FakeUseCaseConfig configWithDefaults = (FakeUseCaseConfig) getUseCaseConfig();
        mImageReader.setOnImageAvailableListener(
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader imageReader) {
                        Image image = imageReader.acquireLatestImage();
                        if (image != null) {
                            image.close();
                        }
                    }
                },
                new Handler(Looper.getMainLooper()));

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(configWithDefaults);
        builder.addSurface(new ImmediateSurface(mImageReader.getSurface()));

        String cameraId = CameraX.getCameraWithCameraSelector(cameraSelector);
        attachToCamera(cameraId, builder.build());
        notifyActive();
    }

    @Override
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(@Nullable Integer lensFacing) {
        return new FakeUseCaseConfig.Builder()
                .setSessionOptionUnpacker(
                        new SessionConfig.OptionUnpacker() {
                            @Override
                            public void unpack(@NonNull UseCaseConfig<?> useCaseConfig,
                                    @NonNull SessionConfig.Builder sessionConfigBuilder) {
                                // Set the template since it is currently required by implementation
                                sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
                            }
                        });
    }

    @Override
    public void clear() {
        super.clear();
        mImageReader.close();
    }

    @Override
    @NonNull
    protected Map<String, Size> onSuggestedResolutionUpdated(
            @NonNull Map<String, Size> suggestedResolutionMap) {
        return suggestedResolutionMap;
    }
}
