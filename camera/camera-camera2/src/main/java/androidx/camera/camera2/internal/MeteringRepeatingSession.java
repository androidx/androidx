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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.camera2.internal.compat.workaround.SupportedRepeatingSurfaceSize;
import androidx.camera.core.Logger;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A SessionConfig to act a Metering repeating use case.
 *
 * <p> When ImageCapture only to do the action of takePicture, the MeteringRepeating is
 * created in Camera2 layer to make Camera2 have the repeating surface to metering the auto 3A or
 * wait for 3A converged.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class MeteringRepeatingSession {
    private static final String TAG = "MeteringRepeating";

    private static final int IMAGE_FORMAT =
            ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

    private DeferrableSurface mDeferrableSurface;

    @NonNull
    private SessionConfig mSessionConfig;

    @NonNull
    private final MeteringRepeatingConfig mConfigWithDefaults;

    @NonNull
    private final Size mMeteringRepeatingSize;

    @NonNull
    private final SupportedRepeatingSurfaceSize mSupportedRepeatingSurfaceSize =
            new SupportedRepeatingSurfaceSize();

    interface SurfaceResetCallback {
        void onSurfaceReset();
    }

    @Nullable
    private final SurfaceResetCallback mSurfaceResetCallback;

    /** Creates a new instance of a {@link MeteringRepeatingSession}. */
    MeteringRepeatingSession(@NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat,
            @NonNull DisplayInfoManager displayInfoManager,
            @Nullable SurfaceResetCallback surfaceResetCallback) {
        mConfigWithDefaults = new MeteringRepeatingConfig();
        mSurfaceResetCallback = surfaceResetCallback;

        mMeteringRepeatingSize = getProperPreviewSize(
                cameraCharacteristicsCompat, displayInfoManager);
        Logger.d(TAG, "MeteringSession SurfaceTexture size: " + mMeteringRepeatingSize);

        mSessionConfig = createSessionConfig();
    }

    @NonNull
    SessionConfig createSessionConfig() {
        // Create the metering DeferrableSurface
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);

        surfaceTexture.setDefaultBufferSize(mMeteringRepeatingSize.getWidth(),
                mMeteringRepeatingSize.getHeight());
        Surface surface = new Surface(surfaceTexture);

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(mConfigWithDefaults,
                mMeteringRepeatingSize);
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        mDeferrableSurface = new ImmediateSurface(surface);

        Futures.addCallback(mDeferrableSurface.getTerminationFuture(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                surface.release();
                surfaceTexture.release();
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                throw new IllegalStateException("Future should never "
                        + "fail. Did it get completed by GC?", t);
            }
        }, CameraXExecutors.directExecutor());

        builder.addSurface(mDeferrableSurface);

        builder.addErrorListener((sessionConfig, error) -> {
            mSessionConfig = createSessionConfig();
            if (mSurfaceResetCallback != null) {
                mSurfaceResetCallback.onSurfaceReset();
            }
        });

        return builder.build();
    }

    @NonNull
    UseCaseConfig<?> getUseCaseConfig() {
        return mConfigWithDefaults;
    }

    @NonNull
    SessionConfig getSessionConfig() {
        return mSessionConfig;
    }

    @NonNull
    Size getMeteringRepeatingSize() {
        return mMeteringRepeatingSize;
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
            mutableOptionsBundle.insertOption(OPTION_INPUT_FORMAT, IMAGE_FORMAT);
            setTargetConfigs(mutableOptionsBundle);
            mConfig = mutableOptionsBundle;
        }

        @NonNull
        @Override
        public Config getConfig() {
            return mConfig;
        }

        @NonNull
        @Override
        public UseCaseConfigFactory.CaptureType getCaptureType() {
            return UseCaseConfigFactory.CaptureType.METERING_REPEATING;
        }

        private void setTargetConfigs(MutableOptionsBundle mutableOptionsBundle) {
            mutableOptionsBundle.insertOption(OPTION_TARGET_CLASS, MeteringRepeatingSession.class);

            String targetName =
                    MeteringRepeatingSession.class.getCanonicalName() + "-" + UUID.randomUUID();
            mutableOptionsBundle.insertOption(OPTION_TARGET_NAME, targetName);
        }
    }

    @NonNull
    private Size getProperPreviewSize(@NonNull CameraCharacteristicsCompat
            cameraCharacteristicsCompat, @NonNull DisplayInfoManager displayInfoManager) {
        StreamConfigurationMapCompat mapCompat =
                cameraCharacteristicsCompat.getStreamConfigurationMapCompat();
        Size[] outputSizes = mapCompat.getOutputSizes(IMAGE_FORMAT);
        if (outputSizes == null) {
            Logger.e(TAG, "Can not get output size list.");
            return new Size(0, 0);
        }

        outputSizes = mSupportedRepeatingSurfaceSize.getSupportedSizes(outputSizes);

        List<Size> outSizesList = Arrays.asList(outputSizes);
        Collections.sort(outSizesList, (o1, o2) -> {
                    int result = Long.signum((long) o1.getWidth() * o1.getHeight()
                            - (long) o2.getWidth() * o2.getHeight());
                    return result;
                });

        // First, find minimum supported resolution that is >=  min(VGA, display resolution)
        // Using minimum supported size could cause some issue on certain devices.
        Size previewMaxSize = displayInfoManager.getPreviewSize();
        long maxSizeProduct =
                Math.min((long) previewMaxSize.getWidth() * (long) previewMaxSize.getHeight(),
                        640L * 480L);
        Size previousSize = null;
        for (Size outputSize : outputSizes) {
            long product = (long) outputSize.getWidth() * (long) outputSize.getHeight();
            if (product == maxSizeProduct) {
                return outputSize;
            } else if (product > maxSizeProduct) {
                if (previousSize != null) {
                    return previousSize;
                } else {
                    break; // fallback to minimum size.
                }
            }
            previousSize = outputSize;
        }

        // If not found, return the minimum size.
        return outSizesList.get(0);
    }

}
