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

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Collections.singletonList;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.utils.Threads;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

/**
 * A {@link Node} implementation that wraps around the public {@link SurfaceEffect} interface.
 *
 * <p>Responsibilities:
 * <ul>
 * <li>Calculating transformation and passing it to the {@link SurfaceEffect}.
 * <li>Tracking the state of previously calculate specification and only recreate the pipeline
 * when necessary.
 * </ul>
 */
@RequiresApi(api = 21)
// TODO(b/233627260): remove once implemented.
@SuppressWarnings("UnusedVariable")
public class SurfaceEffectNode implements Node<SurfaceEdge, SurfaceEdge> {

    @NonNull
    final SurfaceEffectInternal mSurfaceEffect;
    @NonNull
    final CameraInternal mCameraInternal;
    // Guarded by main thread.
    @Nullable
    private SurfaceEdge mOutputEdge;
    @Nullable
    private SurfaceEdge mInputEdge;

    /**
     * @param surfaceEffect the interface to wrap around.
     */
    public SurfaceEffectNode(@NonNull CameraInternal cameraInternal,
            @NonNull SurfaceEffectInternal surfaceEffect) {
        mCameraInternal = cameraInternal;
        mSurfaceEffect = surfaceEffect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    @MainThread
    public SurfaceEdge transform(@NonNull SurfaceEdge inputEdge) {
        Threads.checkMainThread();
        checkArgument(inputEdge.getSurfaces().size() == 1,
                "Multiple input stream not supported yet.");
        mInputEdge = inputEdge;
        SettableSurface inputSurface = inputEdge.getSurfaces().get(0);

        // No transform output as placeholder. The correct outputSurface needs to be calculated
        // based on inputSurface and outputOption.
        SettableSurface outputSurface = new SettableSurface(
                inputSurface.getTargets(),
                inputSurface.getSize(),
                inputSurface.getFormat(),
                inputSurface.getSensorToBufferTransform(),
                // The Surface transform cannot be carried over during buffer copy.
                /*hasEmbeddedTransform=*/false,
                inputSurface.getCropRect(),
                inputSurface.getRotationDegrees(),
                inputSurface.getMirroring());

        sendSurfacesToEffectWhenReady(inputSurface, outputSurface);

        mOutputEdge = SurfaceEdge.create(singletonList(outputSurface));
        return mOutputEdge;
    }

    private void sendSurfacesToEffectWhenReady(@NonNull SettableSurface input,
            @NonNull SettableSurface output) {
        SurfaceRequest surfaceRequest = input.createSurfaceRequest(mCameraInternal);
        Futures.addCallback(output.createSurfaceOutputFuture(/*applyGlTransform=*/false,
                        input.getSize(), input.getCropRect(), input.getRotationDegrees(),
                        input.getMirroring()),
                new FutureCallback<SurfaceOutput>() {
                    @Override
                    public void onSuccess(@Nullable SurfaceOutput surfaceOutput) {
                        Preconditions.checkNotNull(surfaceOutput);
                        mSurfaceEffect.onOutputSurface(surfaceOutput);
                        mSurfaceEffect.onInputSurface(surfaceRequest);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        // Do not send surfaces to effect if the downstream provider (e.g. the app)
                        // fails to provide a Surface. Instead, notify the consumer that the
                        // Surface will not be provided.
                        surfaceRequest.willNotProvideSurface();
                    }
                }, mainThreadExecutor());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        mSurfaceEffect.release();
        mainThreadExecutor().execute(() -> {
            if (mOutputEdge != null) {
                for (SettableSurface surface : mOutputEdge.getSurfaces()) {
                    // The output DeferrableSurface will later be terminated by the effect.
                    surface.close();
                }
            }
        });
    }
}
