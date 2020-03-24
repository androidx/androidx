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
import androidx.camera.core.CameraInfo;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.testing.fakes.FakeUseCase;
import androidx.camera.testing.fakes.FakeUseCaseConfig;

/**
 * A fake {@link FakeUseCase} which contain a repeating surface.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY)
public class FakeRepeatingUseCase extends FakeUseCase {

    private DeferrableSurface mDeferrableSurface;

    public FakeRepeatingUseCase(@NonNull FakeUseCaseConfig configuration) {
        super(configuration);
    }

    @Override
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(@Nullable CameraInfo cameraInfo) {
        return new FakeUseCaseConfig.Builder()
                .setSessionOptionUnpacker(
                        (useCaseConfig, sessionConfigBuilder) -> {
                            // Set the template since it is currently required by implementation
                            sessionConfigBuilder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);
                        });
    }

    @Override
    public void clear() {
        super.clear();
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = null;
    }

    @Override
    @NonNull
    protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
        FakeUseCaseConfig configWithDefaults = (FakeUseCaseConfig) getUseCaseConfig();

        ImageReader imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 2);

        imageReader.setOnImageAvailableListener(
                reader -> {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        image.close();
                    }
                },
                new Handler(Looper.getMainLooper()));

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(configWithDefaults);
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = new ImmediateSurface(imageReader.getSurface());
        mDeferrableSurface.getTerminationFuture().addListener(imageReader::close,
                CameraXExecutors.mainThreadExecutor());
        builder.addSurface(mDeferrableSurface);

        updateSessionConfig(builder.build());
        notifyActive();

        return new Size(640, 480);
    }
}
