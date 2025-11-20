/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.UseCase;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.StreamSpec;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType;
import androidx.core.util.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A fake {@link UseCase}.
 */
public class FakeUseCase extends UseCase {

    private static final int DEFAULT_SURFACE_OCCUPANCY_PRIORITY = 0;

    private volatile boolean mIsDetached = false;
    private final AtomicInteger mStateAttachedCount = new AtomicInteger(0);
    private final CaptureType mCaptureType;
    private boolean mMergedConfigRetrieved = false;
    private int mPipelineCreationCount = 0;
    private Supplier<SessionConfig> mSessionConfigSupplier;
    private Set<Integer> mEffectTargets = Collections.emptySet();
    private RuntimeException mMergedConfigException = null;

    /**
     * Creates a new instance of a {@link FakeUseCase} with a given configuration and capture type.
     */
    public FakeUseCase(@NonNull FakeUseCaseConfig config, @NonNull CaptureType captureType) {
        super(config);
        mCaptureType = captureType;
    }

    /**
     * Creates a new instance of a {@link FakeUseCase} with a given configuration.
     */
    public FakeUseCase(@NonNull FakeUseCaseConfig config) {
        this(config, CaptureType.PREVIEW);
    }

    /**
     * Creates a new instance of a {@link FakeUseCase} with a default configuration.
     */
    public FakeUseCase() {
        this(new FakeUseCaseConfig.Builder()
                .setSurfaceOccupancyPriority(DEFAULT_SURFACE_OCCUPANCY_PRIORITY)
                .setCaptureType(CaptureType.PREVIEW)
                .getUseCaseConfig());
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public UseCaseConfig.@NonNull Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config) {
        return new FakeUseCaseConfig.Builder(config)
                .setCaptureType(mCaptureType)
                .setSessionOptionUnpacker((resolution, useCaseConfig, sessionConfigBuilder) -> {
                });
    }

    /**
     * {@inheritDoc}
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public @Nullable UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory) {
        Config config = factory.getConfig(
                mCaptureType,
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY);
        return config == null ? null : getUseCaseConfigBuilder(config).getUseCaseConfig();
    }

    @Override
    protected @NonNull UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            UseCaseConfig.@NonNull Builder<?, ?, ?> builder) {
        mMergedConfigRetrieved = true;
        if (mMergedConfigException != null) {
            throw mMergedConfigException;
        }
        return builder.getUseCaseConfig();
    }

    @Override
    public void onUnbind() {
        super.onUnbind();
        mIsDetached = true;
    }

    @Override
    @MainThread
    public void onSessionStart() {
        super.onSessionStart();
        mStateAttachedCount.incrementAndGet();
    }

    @Override
    @MainThread
    public void onSessionStop() {
        super.onSessionStop();
        mStateAttachedCount.decrementAndGet();
    }

    @Override
    protected @NonNull StreamSpec onSuggestedStreamSpecUpdated(
            @NonNull StreamSpec primaryStreamSpec,
            @Nullable StreamSpec secondaryStreamSpec) {
        SessionConfig sessionConfig = createPipeline();
        if (sessionConfig != null) {
            updateSessionConfig(List.of(sessionConfig));
        }
        return primaryStreamSpec;
    }

    @Nullable SessionConfig createPipeline() {
        mPipelineCreationCount++;
        if (mSessionConfigSupplier != null) {
            return mSessionConfigSupplier.get();
        } else {
            return null;
        }
    }

    /**
     * Sets effect targets.
     */
    public void setSupportedEffectTargets(@NonNull Set<Integer> effectTargets) {
        mEffectTargets = effectTargets;
    }

    /**
     * @inheritDoc
     */
    @Override
    public @NonNull Set<Integer> getSupportedEffectTargets() {
        return mEffectTargets;
    }


    /**
     * Returns true if {@link #onUnbind()} has been called previously.
     */
    public boolean isDetached() {
        return mIsDetached;
    }

    /**
     * Returns true if {@link #onSessionStart()} has been called previously.
     */
    public int getStateAttachedCount() {
        return mStateAttachedCount.get();
    }

    /**
     * Returns true if {@link #mergeConfigs} have been invoked.
     */
    public boolean getMergedConfigRetrieved() {
        return mMergedConfigRetrieved;
    }

    /**
     * Returns how many times the pipeline has been created.
     */
    public int getPipelineCreationCount() {
        return mPipelineCreationCount;
    }

    /**
     * Returns {@link CameraCaptureResult} received by this use case.
     */
    public void setSessionConfigSupplier(@NonNull Supplier<SessionConfig> sessionConfigSupplier) {
        mSessionConfigSupplier = sessionConfigSupplier;
    }

    /**
     * Calls the protected method {@link UseCase#updateSessionConfig}.
     */
    public void updateSessionConfigForTesting(@NonNull SessionConfig sessionConfig) {
        updateSessionConfig(List.of(sessionConfig));
    }

    /**
     * Calls the protected method {@link UseCase#notifyActive()}.
     */
    public void notifyActiveForTesting() {
        notifyActive();
    }

    /**
     * Calls the protected method {@link UseCase#notifyInactive()}.
     */
    public void notifyInactiveForTesting() {
        notifyInactive();
    }

    /**
     * Calls the protected method {@link UseCase#notifyUpdated()}.
     */
    public void notifyUpdatedForTesting() {
        notifyUpdated();
    }

    /**
     * Calls the protected method {@link UseCase#notifyReset()}.
     */
    public void notifyResetForTesting() {
        notifyReset();
    }

    public void setMergedConfigException(@Nullable RuntimeException exception) {
        mMergedConfigException = exception;
    }
}
