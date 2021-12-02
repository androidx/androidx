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

package androidx.camera.camera2.internal;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import java.util.Arrays;
import java.util.Collections;

/**
 * A SessionConfig to act a Metering repeating use case.
 *
 * <p> When ImageCapture only to do the action of takePicture, the MeteringRepeating is
 * created in Camera2 layer to make Camera2 have the repeating surface to metering the auto 3A or
 * wait for 3A converged.
 *
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class MeteringRepeatingSession {
    private static final String TAG = "MeteringRepeating";
    private DeferrableSurface mDeferrableSurface;

    @NonNull
    private final SessionConfig mSessionConfig;

    /** Creates a new instance of a {@link MeteringRepeatingSession}. */
    MeteringRepeatingSession(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        MeteringRepeatingConfig configWithDefaults = new MeteringRepeatingConfig();

        // Create the metering DeferrableSurface
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        Size meteringSurfaceSize = getMinimumPreviewSize(cameraCharacteristicsCompat);
        Logger.d(TAG, "MerteringSession SurfaceTexture size: " + meteringSurfaceSize);
        surfaceTexture.setDefaultBufferSize(meteringSurfaceSize.getWidth(),
                meteringSurfaceSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(configWithDefaults);
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        mDeferrableSurface = new ImmediateSurface(surface);

        Futures.addCallback(mDeferrableSurface.getTerminationFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                surface.release();
                surfaceTexture.release();
            }

            @Override
            public void onFailure(Throwable t) {
                throw new IllegalStateException("Future should never "
                        + "fail. Did it get completed by GC?", t);
            }
        }, CameraXExecutors.directExecutor());

        builder.addSurface(mDeferrableSurface);

        mSessionConfig = builder.build();
    }

    @NonNull
    SessionConfig getSessionConfig() {
        return mSessionConfig;
    }

    @NonNull
    String getName() {
        return "MeteringRepeating";
    }

    /**
     * The {@link MeteringRepeatingSession} should only be used once and afterwards should be
     * cleared.
     */
    void clear() {
        Logger.d(TAG, "MeteringRepeating clear!");
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = null;
    }

    /**
     * A minimal config that contains a {@link SessionConfig.OptionUnpacker} in order to unpack
     * the camera2 related options.
     */
    private static class MeteringRepeatingConfig implements UseCaseConfig<UseCase> {
        @NonNull
        private final Config mConfig;
        MeteringRepeatingConfig() {
            MutableOptionsBundle mutableOptionsBundle = MutableOptionsBundle.create();
            mutableOptionsBundle.insertOption(UseCaseConfig.OPTION_SESSION_CONFIG_UNPACKER,
                    new Camera2SessionOptionUnpacker());
            mConfig = mutableOptionsBundle;
        }

        @NonNull
        @Override
        public Config getConfig() {
            return mConfig;
        }
    }

    @NonNull private Size getMinimumPreviewSize(@NonNull CameraCharacteristicsCompat
            cameraCharacteristicsCompat) {
        Size[] outputSizes;
        StreamConfigurationMap map = cameraCharacteristicsCompat.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            Logger.e(TAG, "Can not retrieve SCALER_STREAM_CONFIGURATION_MAP.");
            return new Size(0, 0);
        }

        if (Build.VERSION.SDK_INT < 23) {
            // ImageFormat.PRIVATE is only public after Android level 23. Therefore, using
            // SurfaceTexture.class to get the supported output sizes before Android level 23.
            outputSizes = map.getOutputSizes(SurfaceTexture.class);
        } else {
            outputSizes = map.getOutputSizes(ImageFormat.PRIVATE);
        }
        if (outputSizes == null) {
            Logger.e(TAG, "Can not get output size list.");
            return new Size(0, 0);
        }

        return Collections.min(
                Arrays.asList(outputSizes), (o1, o2) -> {
                    int result = Long.signum((long) o1.getWidth() * o1.getHeight()
                            - (long) o2.getWidth() * o2.getHeight());

                    return result;
                });
    }

}


