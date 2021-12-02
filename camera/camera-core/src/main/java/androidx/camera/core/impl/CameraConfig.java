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

package androidx.camera.core.impl;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Configuration for a {@link androidx.camera.core.Camera}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraConfig extends ReadableConfig {

    // Option Declarations:
    // *********************************************************************************************
    Option<UseCaseConfigFactory> OPTION_USECASE_CONFIG_FACTORY =
            Option.create("camerax.core.camera.useCaseConfigFactory",
                    UseCaseConfigFactory.class);

    Option<Identifier> OPTION_COMPATIBILITY_ID =
            Option.create("camerax.core.camera.compatibilityId",
                    Identifier.class);

    Option<Integer> OPTION_USE_CASE_COMBINATION_REQUIRED_RULE =
            Option.create("camerax.core.camera.useCaseCombinationRequiredRule", Integer.class);

    Option<SessionProcessor> OPTION_SESSION_PROCESSOR =
            Option.create("camerax.core.camera.SessionProcessor", SessionProcessor.class);

    /**
     * No rule is required when the camera is opened by the camera config.
     */
    int REQUIRED_RULE_NONE = 0;

    /**
     * Both {@link Preview} and {@link ImageCapture} use cases are needed when the camera is
     * opened by the camera config. An extra {@link Preview} or {@link ImageCapture} will be
     * added only if one use case is lacking. If both {@link Preview} and {@link ImageCapture}
     * are not bound, no extra {@link Preview} and {@link ImageCapture} will be added.
     */
    int REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE = 1;

    @IntDef({REQUIRED_RULE_NONE,
            REQUIRED_RULE_COEXISTING_PREVIEW_AND_IMAGE_CAPTURE})
    @Retention(RetentionPolicy.SOURCE)
    @interface RequiredRule {
    }

    /**
     * Retrieves the use case config factory instance.
     */
    @NonNull
    default UseCaseConfigFactory getUseCaseConfigFactory() {
        return retrieveOption(OPTION_USECASE_CONFIG_FACTORY, UseCaseConfigFactory.EMPTY_INSTANCE);
    }

    /**
     * Retrieves the compatibility {@link Identifier}.
     *
     * <p>If camera configs have the same compatibility identifier, they will allow to bind a new
     * use case without unbinding all use cases first.
     */
    @NonNull
    Identifier getCompatibilityId();

    /**
     * Returns the use case combination required rule when the camera is opened by the camera
     * config.
     */
    @RequiredRule
    default int getUseCaseCombinationRequiredRule() {
        return retrieveOption(OPTION_USE_CASE_COMBINATION_REQUIRED_RULE, REQUIRED_RULE_NONE);
    }

    /**
     * Returns the session processor which will transform the stream configurations and
     * will perform the repeating request and still capture request when being requested by CameraX.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     */
    @Nullable
    default SessionProcessor getSessionProcessor(@Nullable SessionProcessor valueIfMissing) {
        return retrieveOption(OPTION_SESSION_PROCESSOR, valueIfMissing);
    }

    /**
     * Returns the session processor which will transform the stream configurations and will
     * perform the repeating request and still capture request when being requested by CameraX.
     *
     * @return The stored value, if it exists in this configuration.
     * @throws IllegalArgumentException if the option does not exist in this configuration.
     */
    @NonNull
    default SessionProcessor getSessionProcessor() {
        return retrieveOption(OPTION_SESSION_PROCESSOR);
    }

    /**
     * Builder for creating a {@link CameraConfig}.
     *
     * @param <B> the top level builder type for which this builder is composed with.
     */
    interface Builder<B> {
        /**
         * Sets a {@link UseCaseConfigFactory} for the camera config.
         */
        @NonNull
        B setUseCaseConfigFactory(@NonNull UseCaseConfigFactory factory);

        /**
         * Sets compatibility {@link Identifier} for the camera config.
         */
        @NonNull
        B setCompatibilityId(@NonNull Identifier identifier);

        /**
         * Sets use case combination required rule to this configuration.
         */
        @NonNull
        B setUseCaseCombinationRequiredRule(@RequiredRule int useCaseCombinationRequiredRule);

        /**
         * Sets the session processor which will transform the stream configurations and will
         * perform the repeating request and still capture request when being requested by CameraX.
         */
        @NonNull
        B setSessionProcessor(@NonNull SessionProcessor sessionProcessor);
    }
}
