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

import static androidx.camera.core.SurfaceEffect.PREVIEW;
import static androidx.core.util.Preconditions.checkArgument;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A bundle of {@link CameraEffect}s and their targets.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class EffectBundle {

    private final Map<Integer, CameraEffect> mEffects;

    private final Executor mExecutor;

    EffectBundle(@NonNull Map<Integer, CameraEffect> effects, @NonNull Executor executor) {
        mEffects = effects;
        mExecutor = executor;
    }

    /**
     * Gets the {@link CameraEffect} and their targets.
     */
    @NonNull
    public Map<Integer, CameraEffect> getEffects() {
        return new HashMap<>(mEffects);
    }

    /**
     * Gets the {@link Executor} used for calling the {@link CameraEffect}.
     */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Builder class for {@link EffectBundle}.
     */
    public static class Builder {

        private final Map<Integer, CameraEffect> mEffects;
        private final Executor mExecutor;

        /**
         * Creates a {@link EffectBundle} builder.
         *
         * @param executor on which the {@link CameraEffect}s will be invoked.
         */
        public Builder(@NonNull Executor executor) {
            mEffects = new HashMap<>();
            mExecutor = executor;
        }

        /**
         * Adds a {@link CameraEffect} with its targets.
         *
         * TODO: finish Javadoc once {@link ImageEffect} is supported.
         *
         * @param targets      on which the effect will be applied. CameraX only supports
         *                     {@link SurfaceEffect#PREVIEW} for now.
         * @param cameraEffect the effect implementation.
         * @throws IllegalArgumentException if the configuration is illegal.
         */
        @NonNull
        public Builder addEffect(
                @CameraEffect.Targets int targets,
                @NonNull CameraEffect cameraEffect) {
            checkArgument(!mEffects.containsKey(targets), "The target already has an effect");
            checkArgument(targets == PREVIEW, "Only allows PREVIEW target.");
            if (cameraEffect instanceof SurfaceEffect) {
                mEffects.put(targets, cameraEffect);
            } else {
                throw new UnsupportedOperationException(
                        "CameraX only supports SurfaceEffect for now.");
            }
            return this;
        }

        /**
         * Builds the {@link EffectBundle}.
         *
         * @throws IllegalArgumentException if the bundle contains no effect.
         */
        @NonNull
        public EffectBundle build() {
            checkArgument(mEffects.size() > 0, "The bundle cannot be empty");
            return new EffectBundle(new HashMap<>(mEffects), mExecutor);
        }
    }
}
