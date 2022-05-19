/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.core.processing;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A default implementation of {@link SurfaceEffect}.
 *
 * <p> This implementation simply copies the frame from the source to the destination with the
 * transformation defined in {@link SurfaceOutput#updateTransformMatrix}.
 */
@RequiresApi(21)
public class DefaultSurfaceEffect implements SurfaceEffect,
        SurfaceTexture.OnFrameAvailableListener {

    // TODO(b/233627260): Under the hood, both the mGlExecutor and the mGlHandler should be based
    //  on the same Thread object. This thread is used for GL access as well as synchronization.
    Executor mGlExecutor;
    Handler mGlHandler;

    // Map of current set of available outputs. Only access this on GL thread.
    final Map<SurfaceOutput, Surface> mOutputSurfaces = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInputSurface(@NonNull SurfaceRequest surfaceRequest) {
        // TODO(b/233627260): attach this SurfaceTexture to an OpenGL texture.
        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        surfaceTexture.setDefaultBufferSize(surfaceRequest.getResolution().getWidth(),
                surfaceRequest.getResolution().getHeight());
        Surface surface = new Surface(surfaceTexture);
        surfaceRequest.provideSurface(surface, mGlExecutor, result -> {
            surfaceTexture.release();
            surface.release();
        });
        surfaceTexture.setOnFrameAvailableListener(this, mGlHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        // Remove and close the Surface on GL thread to avoid race condition.
        mOutputSurfaces.put(surfaceOutput, surfaceOutput.getSurface(mGlExecutor, () -> {
            surfaceOutput.close();
            mOutputSurfaces.remove(surfaceOutput);
        }));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {

    }
}
