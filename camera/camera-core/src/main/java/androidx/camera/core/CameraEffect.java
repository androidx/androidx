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
package androidx.camera.core;

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor;
import static androidx.core.util.Preconditions.checkState;

import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * A CameraX post-processing effects.
 *
 * <p>A {@link CameraEffect} class contains two types of information, the processor and the
 * configuration.
 * <ul>
 * <li> The processor is an implementation of a CameraX interface e.g. {@link SurfaceProcessor}.
 * It consumes original camera frames from CameraX, applies the effect, and returns the processed
 * frames back to CameraX.
 * <li> The configuration provides information on how the processor should be injected into the
 * CameraX pipeline. For example, the target {@link UseCase}s where the effect should be applied.
 * </ul>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraEffect {

    /**
     * Bitmask options for the effect targets.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE})
    public @interface Targets {
    }

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@link Preview}.
     */
    public static final int PREVIEW = 1;

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@code VideoCapture}.
     */
    public static final int VIDEO_CAPTURE = 1 << 1;

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@link ImageCapture}.
     */
    public static final int IMAGE_CAPTURE = 1 << 2;

    @Targets
    private final int mTargets;
    @NonNull
    private final Executor mProcessorExecutor;
    @Nullable
    private final SurfaceProcessor mSurfaceProcessor;

    /**
     * Private constructor as a workaround to allow @Nullable annotation on final fields.
     */
    @SuppressWarnings("UnusedMethod") // TODO: remove once we add {@link ImageProcessor}.
    private CameraEffect(@Targets int targets) {
        mTargets = targets;
        mProcessorExecutor = mainThreadExecutor();
        mSurfaceProcessor = null;
    }

    /**
     * @param targets           the target {@link UseCase} to which this effect should be applied.
     * @param processorExecutor the {@link Executor} on which the processor will be invoked.
     * @param surfaceProcessor  a {@link SurfaceProcessor} implementation.
     */
    protected CameraEffect(
            @Targets int targets,
            @NonNull Executor processorExecutor,
            @NonNull SurfaceProcessor surfaceProcessor) {
        mTargets = targets;
        mProcessorExecutor = processorExecutor;
        mSurfaceProcessor = surfaceProcessor;
    }

    /**
     * Ges the target {@link UseCase}s of this effect.
     */
    @Targets
    public int getTargets() {
        return mTargets;
    }

    /**
     * Gets the {@link Executor} for calling processors.
     *
     * <p>This method returns the value set via {@link Builder#setSurfaceProcessor}.
     */
    @NonNull
    public Executor getProcessorExecutor() {
        return mProcessorExecutor;
    }

    /**
     * Gets the {@link SurfaceProcessor} associated with this effect.
     *
     * <p>This method returns the value set via {@link Builder#setSurfaceProcessor}.
     */
    @Nullable
    public SurfaceProcessor getSurfaceProcessor() {
        return mSurfaceProcessor;
    }

    /**
     * Builder class for {@link CameraEffect}.
     */
    public static class Builder {
        @Targets
        private final int mTargets;
        @Nullable
        private Executor mProcessorExecutor;
        @Nullable
        private SurfaceProcessor mSurfaceProcessor;

        /**
         * @param targets the target {@link UseCase} of the Effect. e.g. if the
         *                value is {@link #PREVIEW}, CameraX will apply the effect to
         *                {@link Preview}.
         */
        public Builder(@Targets int targets) {
            mTargets = targets;
        }

        /**
         * Sets a {@link SurfaceProcessor} for the effect.
         *
         * <p>Once the effect is active, CameraX will send original camera frames to the
         * {@link SurfaceProcessor} on the {@link Executor}, and deliver the processed output
         * frames to the app.
         *
         * @param executor  on which the {@link SurfaceProcessor} will be invoked.
         * @param processor the post processor to be injected into CameraX pipeline.
         */
        @NonNull
        public Builder setSurfaceProcessor(@NonNull Executor executor,
                @NonNull SurfaceProcessor processor) {
            mProcessorExecutor = executor;
            mSurfaceProcessor = processor;
            return this;
        }

        /**
         * Builds a {@link CameraEffect} instance.
         *
         * <p>CameraX supports a selected set of configuration/processor combinations. This method
         * throws a {@link UnsupportedOperationException} if the current combination is not
         * supported.
         */
        @NonNull
        public CameraEffect build() {
            checkState(mProcessorExecutor != null && mSurfaceProcessor != null,
                    "Must set a processor.");
            return new CameraEffect(mTargets, mProcessorExecutor, mSurfaceProcessor);
        }
    }
}
