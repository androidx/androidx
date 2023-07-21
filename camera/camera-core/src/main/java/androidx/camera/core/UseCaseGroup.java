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

package androidx.camera.core;

import static androidx.camera.core.CameraEffect.IMAGE_CAPTURE;
import static androidx.camera.core.CameraEffect.PREVIEW;
import static androidx.camera.core.CameraEffect.VIDEO_CAPTURE;
import static androidx.camera.core.processing.TargetUtils.checkSupportedTargets;
import static androidx.camera.core.processing.TargetUtils.getHumanReadableName;
import static androidx.core.util.Preconditions.checkArgument;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Represents a collection of {@link UseCase}.
 *
 * When the {@link UseCaseGroup} is bound to {@link Lifecycle}, it binds all the
 * {@link UseCase}s to the same {@link Lifecycle}. {@link UseCase}s inside of a
 * {@link UseCaseGroup} usually share some common properties like the FOV defined by
 * {@link ViewPort}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class UseCaseGroup {
    @Nullable
    private final ViewPort mViewPort;
    @NonNull
    private final List<UseCase> mUseCases;
    @NonNull
    private final List<CameraEffect> mEffects;

    UseCaseGroup(@Nullable ViewPort viewPort, @NonNull List<UseCase> useCases,
            @NonNull List<CameraEffect> effects) {
        mViewPort = viewPort;
        mUseCases = useCases;
        mEffects = effects;
    }

    /**
     * Gets the {@link ViewPort} shared by the {@link UseCase} collection.
     */
    @Nullable
    public ViewPort getViewPort() {
        return mViewPort;
    }

    /**
     * Gets the {@link UseCase}s.
     */
    @NonNull
    public List<UseCase> getUseCases() {
        return mUseCases;
    }

    /**
     * Gets the {@link CameraEffect}s.
     */
    @NonNull
    public List<CameraEffect> getEffects() {
        return mEffects;
    }

    /**
     * A builder for generating {@link UseCaseGroup}.
     */
    public static final class Builder {

        // Allow-list effect targets supported by CameraX.
        private static final List<Integer> SUPPORTED_TARGETS = Arrays.asList(
                PREVIEW,
                VIDEO_CAPTURE,
                IMAGE_CAPTURE,
                PREVIEW | VIDEO_CAPTURE,
                PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE);

        private ViewPort mViewPort;
        private final List<UseCase> mUseCases;
        private final List<CameraEffect> mEffects;


        public Builder() {
            mUseCases = new ArrayList<>();
            mEffects = new ArrayList<>();
        }

        /**
         * Sets {@link ViewPort} shared by the {@link UseCase}s.
         */
        @NonNull
        public Builder setViewPort(@NonNull ViewPort viewPort) {
            mViewPort = viewPort;
            return this;
        }

        /**
         * Adds a {@link CameraEffect} to the collection.
         *
         * <p>The value of {@link CameraEffect#getTargets()} must be one of the supported values
         * below:
         * <ul>
         * <li>{@link CameraEffect#PREVIEW}
         * <li>{@link CameraEffect#VIDEO_CAPTURE}
         * <li>{@link CameraEffect#IMAGE_CAPTURE}
         * <li>{@link CameraEffect#VIDEO_CAPTURE} | {@link CameraEffect#PREVIEW}
         * <li>{@link CameraEffect#VIDEO_CAPTURE} | {@link CameraEffect#PREVIEW} |
         * {@link CameraEffect#IMAGE_CAPTURE}
         * </ul>
         *
         * <p>The targets must be mutually exclusive of each other, otherwise, the {@link #build()}
         * method will throw {@link IllegalArgumentException}. For example, it's invalid to have
         * one {@link CameraEffect} with target {@link CameraEffect#PREVIEW} and another
         * {@link CameraEffect} with target {@link CameraEffect#PREVIEW} |
         * {@link CameraEffect#VIDEO_CAPTURE}, since they both target {@link Preview}.
         *
         * <p>Once added, CameraX will use the {@link CameraEffect}s to process the outputs of
         * the {@link UseCase}s.
         */
        @NonNull
        public Builder addEffect(@NonNull CameraEffect cameraEffect) {
            mEffects.add(cameraEffect);
            return this;
        }

        /**
         * Checks effect targets and throw {@link IllegalArgumentException}.
         *
         * <p>Throws exception if the effects 1) contains conflicting targets or 2) contains
         * effects that is not in the allowlist.
         */
        private void checkEffectTargets() {
            int existingTargets = 0;
            for (CameraEffect effect : mEffects) {
                int targets = effect.getTargets();
                checkSupportedTargets(SUPPORTED_TARGETS, targets);
                int overlappingTargets = existingTargets & targets;
                if (overlappingTargets > 0) {
                    throw new IllegalArgumentException(String.format(Locale.US,
                            "More than one effects has targets %s.",
                            getHumanReadableName(overlappingTargets)));
                }
                existingTargets |= targets;
            }
        }


        /**
         * Adds {@link UseCase} to the collection.
         */
        @NonNull
        public Builder addUseCase(@NonNull UseCase useCase) {
            mUseCases.add(useCase);
            return this;
        }

        /**
         * Builds a {@link UseCaseGroup} from the current state.
         */
        @NonNull
        public UseCaseGroup build() {
            checkArgument(!mUseCases.isEmpty(), "UseCase must not be empty.");
            checkEffectTargets();
            return new UseCaseGroup(mViewPort, mUseCases, mEffects);
        }
    }
}
