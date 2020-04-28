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
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImmediateSurface;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

/**
 * A use case that only used in Camera2 to act a Metering repeating use case.
 *
 * <p> When ImageCapture only to do the action of takePicture, the MeteringRepeatingUseCase is
 * created in Camera2 layer to make Camera2 have the repeating surface to metering the auto 3A or
 * wait for 3A converged.
 *
 */
public class MeteringRepeating extends UseCase {

    private static final String TAG = "MeteringRepeating";
    private DeferrableSurface mDeferrableSurface;
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     * Creates a new instance of a {@link MeteringRepeating} with a given configuration.
     * @param config for this use case instance
     */
    public MeteringRepeating(@NonNull MeteringRepeatingConfig config) {
        super(config);
    }


    /**
     * * Creates a new instance of a {@link MeteringRepeating} with a CameraInternal and
     * default configuration.
     *
     * @param cameraInternal for this use case instance
     *
     */
    public MeteringRepeating(@NonNull CameraInternal cameraInternal) {

        this(new MeteringRepeatingConfig.Builder().getUseCaseConfig());

        // attach to the Camera
        onAttach(cameraInternal);

        updateSuggestedResolution(new Size(640, 480));
    }

    @NonNull
    @Override
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(@Nullable CameraInfo cameraInfo) {
        return new MeteringRepeatingConfig.Builder()
                .setSessionOptionUnpacker(new Camera2SessionOptionUnpacker());
    }

    @Override
    public void clear() {
        notifyInactive();
        if (DEBUG) {
            Log.d(TAG, "MeteringRepeating clear!");
        }
        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }
        mDeferrableSurface = null;
        super.clear();
    }

    @Override
    @NonNull
    protected Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution) {
        MeteringRepeatingConfig configWithDefaults = (MeteringRepeatingConfig) getUseCaseConfig();

        // Create the metering DeferrableSurface
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(0, 0);
        Surface surface = new Surface(surfaceTexture);

        SessionConfig.Builder builder = SessionConfig.Builder.createFrom(configWithDefaults);
        builder.setTemplateType(CameraDevice.TEMPLATE_PREVIEW);

        if (mDeferrableSurface != null) {
            mDeferrableSurface.close();
        }

        mDeferrableSurface = new ImmediateSurface(surface);
        mDeferrableSurface.getTerminationFuture().addListener(() -> {
            if (DEBUG) {
                Log.d(TAG, "Release metering surface and surface texture");
            }
            surface.release();
            surfaceTexture.release();
        }, CameraXExecutors.directExecutor());
        builder.addSurface(mDeferrableSurface);

        updateSessionConfig(builder.build());
        // TODO(b/153826101): Try to set the MeteringRepeating use case to be active when a
        //  pre-capture sequence is needed.This could significantly reduce battery consumption
        //  when pictures are not being taken.
        notifyActive();

        return new Size(0, 0);
    }

}


