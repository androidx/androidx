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

import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.processing.TargetUtils.checkSupportedTargets;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.graphics.ImageFormat;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.processing.SurfaceProcessorInternal;
import androidx.camera.core.processing.SurfaceProcessorWithExecutor;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * An effect for one or multiple camera outputs.
 *
 * <p>This API allows the implementer to inject code into CameraX pipeline and apply visual
 * effects, such as a portrait effect. The {@link CameraEffect} class contains two types of
 * information, the processor and the configuration.
 * <ul>
 * <li>The processor is an implementation of either {@link SurfaceProcessor} or
 * {@link ImageProcessor}. It consumes original camera frames from CameraX, applies the effect,
 * and returns the processed frames back to CameraX.
 * <li>The configuration provides information on how the processor should be injected into the
 * pipeline. For example, the target {@link UseCase}s where the effect should be applied. It may
 * also contain information about camera configuration. For example, the exposure level.
 * </ul>
 *
 * <p>If CameraX fails to send frames to the {@link CameraEffect}, the error will be
 * delivered to the app via error callbacks such as
 * {@link ImageCapture.OnImageCapturedCallback#onError}. If {@link CameraEffect} fails to
 * process and return the frames, for example, unable to allocate the resources for image
 * processing, it must throw {@link Exception} in the processor implementation. The
 * {@link Exception} will be caught and forwarded to the app via error callbacks. Please see the
 * Javadoc of the processor interfaces for details.
 *
 * <p>Extend this class to create specific effects. The {@link Executor} provided in the
 * constructors will be used by CameraX to call the processors.
 *
 * <p>Code sample for a portrait effect that targets the {@link Preview} {@link UseCase}:
 *
 * <pre><code>
 * class PortraitPreviewEffect extends CameraEffect {
 *     PortraitPreviewEffect() {
 *         super(PREVIEW, getExecutor(), getSurfaceProcessor());
 *     }
 *
 *     private static Executor getExecutor() {
 *         // Returns an executor for calling the SurfaceProcessor
 *     }
 *
 *     private static SurfaceProcessor getSurfaceProcessor() {
 *         // Return a SurfaceProcessor implementation that applies a portrait effect.
 *     }
 * }
 * </code></pre>
 */
@RequiresApi(21)
public abstract class CameraEffect {

    /**
     * Bitmask options for the effect targets.
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(flag = true, value = {PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE})
    public @interface Targets {
    }

    /**
     * Bitmask options for the effect targets.
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(flag = true, value = {INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE, ImageFormat.JPEG})
    public @interface Formats {
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

    // Allowed targets for SurfaceProcessor
    private static final List<Integer> SURFACE_PROCESSOR_TARGETS = Arrays.asList(
            PREVIEW,
            VIDEO_CAPTURE,
            PREVIEW | VIDEO_CAPTURE,
            PREVIEW | VIDEO_CAPTURE | IMAGE_CAPTURE);

    @Targets
    private final int mTargets;
    @NonNull
    private final Executor mExecutor;
    @Nullable
    private final SurfaceProcessor mSurfaceProcessor;
    @Nullable
    private final ImageProcessor mImageProcessor;

    /**
     * @param targets        the target {@link UseCase} to which this effect should be applied.
     *                       Currently, {@link ImageProcessor} can only target
     *                       {@link #IMAGE_CAPTURE}. Targeting other {@link UseCase} will throw
     *                       {@link IllegalArgumentException}.
     * @param executor       the {@link Executor} on which the {@param imageProcessor} will be
     *                       invoked.
     * @param imageProcessor a {@link ImageProcessor} implementation. Once the effect is active,
     *                       CameraX will send frames to the {@link ImageProcessor} on the
     *                       {@param executor}, and deliver the processed frames to the app.
     */
    protected CameraEffect(
            @Targets int targets,
            @NonNull Executor executor,
            @NonNull ImageProcessor imageProcessor) {
        checkArgument(targets == IMAGE_CAPTURE,
                "Currently ImageProcessor can only target IMAGE_CAPTURE.");
        mTargets = targets;
        mExecutor = executor;
        mSurfaceProcessor = null;
        mImageProcessor = imageProcessor;
    }

    /**
     * @param targets          the target {@link UseCase} to which this effect should be applied.
     *                         Currently {@link SurfaceProcessor} can target the following
     *                         combinations:
     *                         <ul>
     *                         <li>{@link #PREVIEW}
     *                         <li>{@link #PREVIEW} | {@link #VIDEO_CAPTURE}
     *                         <li>{@link #PREVIEW} | {@link #VIDEO_CAPTURE} |
     *                         {@link #IMAGE_CAPTURE}
     *                         </ul>
     *                         Targeting other {@link UseCase} combinations will throw
     *                         {@link IllegalArgumentException}.
     * @param executor         the {@link Executor} on which the {@param imageProcessor} will be
     *                         invoked.
     * @param surfaceProcessor a {@link SurfaceProcessor} implementation. Once the effect is
     *                         active, CameraX will send frames to the {@link SurfaceProcessor}
     *                         on the {@param executor}, and deliver the processed frames to the
     *                         app.
     */
    protected CameraEffect(
            @Targets int targets,
            @NonNull Executor executor,
            @NonNull SurfaceProcessor surfaceProcessor) {
        checkSupportedTargets(SURFACE_PROCESSOR_TARGETS, targets);
        mTargets = targets;
        mExecutor = executor;
        mSurfaceProcessor = surfaceProcessor;
        mImageProcessor = null;
    }

    /**
     * Ges the target {@link UseCase}s of this effect.
     */
    @Targets
    public int getTargets() {
        return mTargets;
    }

    /**
     * Gets the {@link Executor} associated with this effect.
     *
     * <p>This method returns the value set in the constructor.
     */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Gets the {@link SurfaceProcessor} associated with this effect.
     */
    @Nullable
    public SurfaceProcessor getSurfaceProcessor() {
        return mSurfaceProcessor;
    }

    /**
     * Gets the {@link ImageProcessor} associated with this effect.
     */
    @Nullable
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ImageProcessor getImageProcessor() {
        return mImageProcessor;
    }

    // --- Internal methods ---

    /**
     * Creates a {@link SurfaceProcessorInternal} instance.
     *
     * <p>Throws {@link IllegalArgumentException} if the effect does not contain a
     * {@link SurfaceProcessor}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public SurfaceProcessorInternal createSurfaceProcessorInternal() {
        return new SurfaceProcessorWithExecutor(requireNonNull(getSurfaceProcessor()),
                getExecutor());
    }
}
