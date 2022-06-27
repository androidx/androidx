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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.impl.utils.Threads;
import androidx.core.util.Preconditions;

import java.util.Collections;
import java.util.concurrent.Executor;

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
public class SurfaceEffectNode implements Node<SurfaceIn, SurfaceOut> {
    private final SurfaceEffect mSurfaceEffect;
    private final Executor mExecutor;
    // TODO(b/233680187): keep track of the state of the node so that the pipeline can be
    //  recreated without restarting.
    private SurfaceIn mSurfaceIn;
    private SurfaceOut mSurfaceOut;
    /**
     * @param surfaceEffect the interface to wrap around.
     * @param executor      the executor on which the {@link SurfaceEffect} methods are invoked.
     */
    public SurfaceEffectNode(@NonNull SurfaceEffect surfaceEffect, @NonNull Executor executor) {
        mSurfaceEffect = surfaceEffect;
        mExecutor = executor;
    }
    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    @MainThread
    public SurfaceOut transform(@NonNull SurfaceIn surfaceIn) {
        Threads.checkMainThread();
        SettableSurface inputSurface = surfaceIn.getSurface();
        // TODO(b/233627260): invoke mSurfaceEffect#onInputSurface with the value of inputSurface.
        Preconditions.checkState(surfaceIn.getOutputOptions().size() == 1);
        SurfaceOption surfaceOption = surfaceIn.getOutputOptions().get(0);
        // TODO(b/233628734): calculate SurfaceInfo and outputSurface based on inputSurface and
        //  outputOption.
        // No transform output as placeholder. The correct outputSurface needs to be calculated
        // based on inputSurface and outputOption.
        SettableSurface outputSurface = new SettableSurface(
                inputSurface.getTargets(),
                inputSurface.getSize(),
                inputSurface.getFormat(),
                inputSurface.getSensorToBufferTransform(),
                // TODO(b/233628734): the hasEmbeddedTransform value should be false, as
                //  buffer-copying always removes the value.
                inputSurface.hasEmbeddedTransform(),
                inputSurface.getCropRect(),
                inputSurface.getRotationDegrees(),
                inputSurface.getMirroring());
        // TODO(b/233627260): invoke mSurfaceEffect#onOutput with the value of outputSurface.
        return SurfaceOut.create(Collections.singletonList(outputSurface));
    }
}
