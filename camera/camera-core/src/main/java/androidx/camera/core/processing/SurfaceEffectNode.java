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

import static androidx.core.util.Preconditions.checkArgument;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.SurfaceEffect;
import androidx.camera.core.impl.utils.Threads;

import java.util.Collections;

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

    private final SurfaceEffectInternal mSurfaceEffect;
    // TODO(b/233680187): keep track of the state of the node so that the pipeline can be
    //  recreated without restarting.

    private SurfaceEdge mOutputEdge;
    private SurfaceEdge mInputEdge;

    /**
     * TODO(b/233628734): overload the constructor to pass-in instructions on how the node should
     *  transform the input. Based on the instructions, we need to calculate the SettableSurface
     *  in the output edge and the 4x4 matrix passing to the GL renderer.
     *
     * @param surfaceEffect the interface to wrap around.
     */
    public SurfaceEffectNode(@NonNull SurfaceEffectInternal surfaceEffect) {
        mSurfaceEffect = surfaceEffect;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    @MainThread
    public SurfaceEdge transform(@NonNull SurfaceEdge inputEdge) {
        Threads.checkMainThread();
        checkArgument(inputEdge.getSurfaces().size() == 1,
                "Multiple input stream not supported yet.");
        mInputEdge = inputEdge;
        SettableSurface inputSurface = inputEdge.getSurfaces().get(0);
        // TODO(b/233627260): invoke mSurfaceEffect#onInputSurface with the value of inputSurface.

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
        // TODO(b/233627260): invoke mSurfaceEffect#onOutput with the value of outputSurface.
        mOutputEdge = SurfaceEdge.create(Collections.singletonList(outputSurface));
        return mOutputEdge;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        // TODO: Call #close() on the output SurfaceOut#getSurface
        mSurfaceEffect.release();
    }
}
