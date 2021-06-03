/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.CallSuper;
import androidx.annotation.GuardedBy;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config;
import androidx.camera.core.impl.Config.Option;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableOptionsBundle;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.UseCaseConfigFactory;
import androidx.camera.core.internal.TargetConfig;
import androidx.camera.core.internal.utils.UseCaseConfigUtil;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The use case which all other use cases are built on top of.
 *
 * <p>A UseCase provides functionality to map the set of arguments in a use case to arguments
 * that are usable by a camera. UseCase also will communicate of the active/inactive state to
 * the Camera.
 */
public abstract class UseCase {

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime constant] - Stays constant for the lifetime of the UseCase. Which means
    // they could be created in the constructor.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The set of {@link StateChangeCallback} that are currently listening state transitions of this
     * use case.
     */
    private final Set<StateChangeCallback> mStateChangeCallbacks = new HashSet<>();

    private final Object mCameraLock = new Object();

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase lifetime dynamic] - Dynamic variables which could change during anytime during
    // the UseCase lifetime.
    ////////////////////////////////////////////////////////////////////////////////////////////

    private State mState = State.INACTIVE;

    /** Extended config, applied on top of the app defined Config (mUseCaseConfig). */
    @Nullable
    private UseCaseConfig<?> mExtendedConfig;

    /**
     * Store the app defined {@link UseCaseConfig} used to create the use case.
     */
    @NonNull
    private UseCaseConfig<?> mUseCaseConfig;

    /**
     * The currently used Config.
     *
     * <p> This is the combination of the extended Config, app provided Config, and camera
     * implementation Config (with decreasing priority).
     */
    @NonNull
    private UseCaseConfig<?> mCurrentConfig;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached constant] - Is only valid when the UseCase is attached to a camera.
    ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * The resolution assigned to the {@link UseCase} based on the attached camera.
     */
    private Size mAttachedResolution;

    /**
     * The camera implementation provided Config. Its options has lowest priority and will be
     * overwritten by any app defined or extended configs.
     */
    @Nullable
    private UseCaseConfig<?> mCameraConfig;

    /**
     * The crop rect calculated at the time of binding based on {@link ViewPort}.
     */
    @Nullable
    private Rect mViewPortCropRect;

    @GuardedBy("mCameraLock")
    private CameraInternal mCamera;

    ////////////////////////////////////////////////////////////////////////////////////////////
    // [UseCase attached dynamic] - Can change but is only available when the UseCase is attached.
    ////////////////////////////////////////////////////////////////////////////////////////////

    // The currently attached session config
    private SessionConfig mAttachedSessionConfig = SessionConfig.defaultEmptySessionConfig();

    /**
     * Creates a named instance of the use case.
     *
     * @param currentConfig the configuration object used for this use case
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCase(@NonNull UseCaseConfig<?> currentConfig) {
        mUseCaseConfig = currentConfig;
        mCurrentConfig = currentConfig;
    }

    /**
     * Retrieve the default {@link UseCaseConfig} for the UseCase.
     *
     * @param applyDefaultConfig true if this is the base config applied to a UseCase.
     * @param factory the factory that contains the default UseCases.
     * @return The UseCaseConfig or null if there is no default Config.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public abstract UseCaseConfig<?> getDefaultConfig(boolean applyDefaultConfig,
            @NonNull UseCaseConfigFactory factory);

    /**
     * Create a {@link UseCaseConfig.Builder} for the UseCase.
     *
     * @param config the Config to initialize the builder
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public abstract UseCaseConfig.Builder<?, ?, ?> getUseCaseConfigBuilder(@NonNull Config config);

    /**
     * Create a merged {@link UseCaseConfig} from the UseCase, camera, and an extended config.
     *
     * @param cameraInfo          info about the camera which may be used to resolve conflicts.
     * @param extendedConfig      configs that take priority over the UseCase's default config
     * @param cameraDefaultConfig configs that have lower priority than the UseCase's default.
     *                            This Config comes from the camera implementation.
     *
     * @throws IllegalArgumentException if there exists conflicts in the merged config that can
     * not be resolved
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public UseCaseConfig<?> mergeConfigs(
            @NonNull CameraInfoInternal cameraInfo,
            @Nullable UseCaseConfig<?> extendedConfig,
            @Nullable UseCaseConfig<?> cameraDefaultConfig) {
        MutableOptionsBundle mergedConfig;

        if (cameraDefaultConfig != null) {
            mergedConfig = MutableOptionsBundle.from(cameraDefaultConfig);
            mergedConfig.removeOption(TargetConfig.OPTION_TARGET_NAME);
        } else {
            mergedConfig = MutableOptionsBundle.create();
        }

        // If any options need special handling, this is the place to do it. For now we'll just copy
        // over all options.
        for (Option<?> opt : mUseCaseConfig.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Option<Object> objectOpt = (Option<Object>) opt;

            mergedConfig.insertOption(objectOpt,
                    mUseCaseConfig.getOptionPriority(opt),
                    mUseCaseConfig.retrieveOption(objectOpt));
        }

        if (extendedConfig != null) {
            // If any options need special handling, this is the place to do it. For now we'll
            // just copy over all options.
            for (Option<?> opt : extendedConfig.listOptions()) {
                @SuppressWarnings("unchecked") // Options/values are being copied directly
                        Option<Object> objectOpt = (Option<Object>) opt;
                if (objectOpt.getId().equals(TargetConfig.OPTION_TARGET_NAME.getId())) {
                    continue;
                }
                mergedConfig.insertOption(objectOpt,
                        extendedConfig.getOptionPriority(opt),
                        extendedConfig.retrieveOption(objectOpt));
            }
        }

        // If OPTION_TARGET_RESOLUTION has been set by the user, remove
        // OPTION_TARGET_ASPECT_RATIO from defaultConfigBuilder because these two settings cannot be
        // set at the same time.
        if (mergedConfig.containsOption(ImageOutputConfig.OPTION_TARGET_RESOLUTION)
                && mergedConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)) {
            mergedConfig.removeOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO);
        }

        return onMergeConfig(cameraInfo, getUseCaseConfigBuilder(mergedConfig));
    }

    /**
     * Called when a set of configs are merged so the UseCase can do additional handling.
     *
     * <p> This can be overridden by a UseCase which need to do additional verification of the
     * configs to make sure there are no conflicting options.
     *
     * @param cameraInfo info about the camera which may be used to resolve conflicts.
     * @param builder    the builder containing the merged configs requiring addition conflict
     *                   resolution
     * @return the conflict resolved config
     * @throws IllegalArgumentException if there exists conflicts in the merged config that can
     * not be resolved
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected UseCaseConfig<?> onMergeConfig(@NonNull CameraInfoInternal cameraInfo,
            @NonNull UseCaseConfig.Builder<?, ?, ?> builder) {
        return builder.getUseCaseConfig();
    }

    /**
     * Updates the target rotation of the use case config.
     *
     * @param targetRotation Target rotation of the output image, expressed as one of
     *                       {@link Surface#ROTATION_0}, {@link Surface#ROTATION_90},
     *                       {@link Surface#ROTATION_180}, or {@link Surface#ROTATION_270}.
     * @return true if the target rotation was changed.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected boolean setTargetRotationInternal(
            @ImageOutputConfig.RotationValue int targetRotation) {
        ImageOutputConfig oldConfig = (ImageOutputConfig) getCurrentConfig();
        int oldRotation = oldConfig.getTargetRotation(ImageOutputConfig.INVALID_ROTATION);
        if (oldRotation == ImageOutputConfig.INVALID_ROTATION || oldRotation != targetRotation) {
            UseCaseConfig.Builder<?, ?, ?> builder = getUseCaseConfigBuilder(mUseCaseConfig);
            UseCaseConfigUtil.updateTargetRotationAndRelatedConfigs(builder, targetRotation);
            mUseCaseConfig = builder.getUseCaseConfig();

            // Only merge configs if currently attached to a camera. Otherwise, set the current
            // config to the use case config and mergeConfig() will be called once the use case
            // is attached to a camera.
            CameraInternal camera = getCamera();
            if (camera == null) {
                mCurrentConfig = mUseCaseConfig;
            } else {
                mCurrentConfig = mergeConfigs(camera.getCameraInfoInternal(), mExtendedConfig,
                        mCameraConfig);
            }

            return true;
        }
        return false;
    }

    /**
     * Returns the rotation that the intended target resolution is expressed in.
     *
     * @return The rotation of the intended target.
     * @hide
     */
    @SuppressLint("WrongConstant")
    @RestrictTo(Scope.LIBRARY_GROUP)
    @ImageOutputConfig.RotationValue
    protected int getTargetRotationInternal() {
        return ((ImageOutputConfig) mCurrentConfig).getTargetRotation(Surface.ROTATION_0);
    }

    /**
     * Gets the relative rotation degrees based on the target rotation.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @IntRange(from = 0, to = 359)
    protected int getRelativeRotation(@NonNull CameraInternal cameraInternal) {
        return cameraInternal.getCameraInfoInternal().getSensorRotationDegrees(
                getTargetRotationInternal());
    }

    /**
     * Sets the {@link SessionConfig} that will be used by the attached {@link Camera}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void updateSessionConfig(@NonNull SessionConfig sessionConfig) {
        mAttachedSessionConfig = sessionConfig;
    }

    /**
     * Add a {@link StateChangeCallback}, which listens to this UseCase's active and inactive
     * transition events.
     */
    private void addStateChangeCallback(@NonNull StateChangeCallback callback) {
        mStateChangeCallbacks.add(callback);
    }

    /**
     * Remove a {@link StateChangeCallback} from listening to this UseCase's active and inactive
     * transition events.
     *
     * <p>If the listener isn't currently listening to the UseCase then this call does nothing.
     */
    private void removeStateChangeCallback(@NonNull StateChangeCallback callback) {
        mStateChangeCallbacks.remove(callback);
    }

    /**
     * Get the current {@link SessionConfig}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public SessionConfig getSessionConfig() {
        return mAttachedSessionConfig;
    }

    /**
     * Notify all {@link StateChangeCallback} that are listening to this UseCase that it has
     * transitioned to an active state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyActive() {
        mState = State.ACTIVE;
        notifyState();
    }

    /**
     * Notify all {@link StateChangeCallback} that are listening to this UseCase that it has
     * transitioned to an inactive state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyInactive() {
        mState = State.INACTIVE;
        notifyState();
    }

    /**
     * Notify all {@link StateChangeCallback} that are listening to this UseCase that the
     * settings have been updated.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyUpdated() {
        for (StateChangeCallback stateChangeCallback : mStateChangeCallbacks) {
            stateChangeCallback.onUseCaseUpdated(this);
        }
    }

    /**
     * Notify all {@link StateChangeCallback} that are listening to this UseCase that the use
     * case needs to be completely reset.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyReset() {
        for (StateChangeCallback stateChangeCallback : mStateChangeCallbacks) {
            stateChangeCallback.onUseCaseReset(this);
        }
    }

    /**
     * Notify all {@link StateChangeCallback} that are listening to this UseCase of its current
     * state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public final void notifyState() {
        switch (mState) {
            case INACTIVE:
                for (StateChangeCallback stateChangeCallback : mStateChangeCallbacks) {
                    stateChangeCallback.onUseCaseInactive(this);
                }
                break;
            case ACTIVE:
                for (StateChangeCallback stateChangeCallback : mStateChangeCallbacks) {
                    stateChangeCallback.onUseCaseActive(this);
                }
                break;
        }
    }

    /**
     * Returns the camera ID for the currently attached camera, or throws an exception if no
     * camera is attached.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected String getCameraId() {
        return Preconditions.checkNotNull(getCamera(),
                "No camera attached to use case: " + this).getCameraInfoInternal().getCameraId();
    }

    /**
     * Checks whether the provided camera ID is the currently attached camera ID.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected boolean isCurrentCamera(@NonNull String cameraId) {
        if (getCamera() == null) {
            return false;
        }
        return Objects.equals(cameraId, getCameraId());
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public String getName() {
        return mCurrentConfig.getTargetName("<UnknownUseCase-" + this.hashCode() + ">");
    }

    /**
     * Retrieves the configuration used by this use case.
     *
     * @return the configuration used by this use case.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public UseCaseConfig<?> getCurrentConfig() {
        return mCurrentConfig;
    }

    /**
     * Returns the currently attached {@link Camera} or {@code null} if none is attached.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public CameraInternal getCamera() {
        synchronized (mCameraLock) {
            return mCamera;
        }
    }

    /**
     * Retrieves the currently attached surface resolution.
     *
     * @return the currently attached surface resolution for the given camera id.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Size getAttachedSurfaceResolution() {
        return mAttachedResolution;
    }

    /**
     * Offers suggested resolution for the UseCase.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void updateSuggestedResolution(@NonNull Size suggestedResolution) {
        mAttachedResolution = onSuggestedResolutionUpdated(suggestedResolution);
    }

    /**
     * Called when binding new use cases via {@code CameraX#bindToLifecycle(LifecycleOwner,
     * CameraSelector, UseCase...)}.
     *
     * <p>Override to create necessary objects like {@link ImageReader} depending
     * on the resolution.
     *
     * @param suggestedResolution The suggested resolution that depends on camera device
     *                            capability and what and how many use cases will be bound.
     * @return The resolution that finally used to create the SessionConfig to
     * attach to the camera device.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected abstract Size onSuggestedResolutionUpdated(@NonNull Size suggestedResolution);

    /**
     * Called when CameraControlInternal is attached into the UseCase. UseCase may need to
     * override this method to configure the CameraControlInternal here. Ex. Setting correct flash
     * mode by CameraControlInternal.setFlashMode to enable correct AE mode and flash state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void onCameraControlReady() {
    }

    /**
     * Called when use case is attaching to a camera.
     *
     * @hide
     */
    @SuppressLint("WrongConstant")
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onAttach(@NonNull CameraInternal camera,
            @Nullable UseCaseConfig<?> extendedConfig,
            @Nullable UseCaseConfig<?> cameraConfig) {
        synchronized (mCameraLock) {
            mCamera = camera;
            addStateChangeCallback(camera);
        }

        mExtendedConfig = extendedConfig;
        mCameraConfig = cameraConfig;
        mCurrentConfig = mergeConfigs(camera.getCameraInfoInternal(), mExtendedConfig,
                mCameraConfig);

        EventCallback eventCallback = mCurrentConfig.getUseCaseEventCallback(null);
        if (eventCallback != null) {
            eventCallback.onAttach(camera.getCameraInfoInternal());
        }
        onAttached();
    }

    /**
     * Called in the end of onAttach().
     *
     * <p>Called after the use case is attached to a camera. After the use case is attached, the
     * default config settings are also applied to the use case config. The sub classes should
     * create the necessary objects to make the use case work correctly.
     *
     * <p>When onAttached is called, then UseCase should run setup to make sure that the UseCase
     * sets up the pipeline to receive data from the camera.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onAttached() {
    }

    /**
     * Called when use case is detaching from a camera.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void onDetach(@NonNull CameraInternal camera) {
        // Do any cleanup required by the UseCase implementation
        onDetached();

        // Cleanup required for any type of UseCase
        EventCallback eventCallback = mCurrentConfig.getUseCaseEventCallback(null);
        if (eventCallback != null) {
            eventCallback.onDetach();
        }

        synchronized (mCameraLock) {
            Preconditions.checkArgument(camera == mCamera);
            removeStateChangeCallback(mCamera);
            mCamera = null;
        }

        mAttachedResolution = null;
        mViewPortCropRect = null;

        // Resets the mUseCaseConfig to the initial status when the use case was created to make
        // the use case reusable.
        mCurrentConfig = mUseCaseConfig;
        mExtendedConfig = null;
        mCameraConfig = null;
    }

    /**
     * Clears internal state of this use case.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onDetached() {
    }

    /**
     * Called when use case is attached to the camera. This method is called on main thread.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @CallSuper
    public void onStateAttached() {
        onCameraControlReady();
    }

    /**
     * Called when use case is detached from the camera. This method is called on main thread.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onStateDetached() {
    }

    /**
     * Retrieves a previously attached {@link CameraControlInternal}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected CameraControlInternal getCameraControl() {
        synchronized (mCameraLock) {
            if (mCamera == null) {
                return CameraControlInternal.DEFAULT_EMPTY_INSTANCE;
            }
            return mCamera.getCameraControlInternal();
        }
    }

    /**
     * Sets the view port crop rect calculated at the time of binding.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void setViewPortCropRect(@NonNull Rect viewPortCropRect) {
        mViewPortCropRect = viewPortCropRect;
    }

    /**
     * Gets the view port crop rect.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public Rect getViewPortCropRect() {
        return mViewPortCropRect;
    }

    /**
     * Get image format for the use case.
     *
     * @return image format for the use case
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getImageFormat() {
        return mCurrentConfig.getInputFormat();
    }

    /**
     * Returns {@link ResolutionInfo} of the use case.
     *
     * <p>The resolution information might change if the use case is unbound and then rebound or
     * the target rotation setting is changed. The application needs to call
     * {@link #getResolutionInfo()} again to get the latest {@link ResolutionInfo} for the changes.
     *
     * @return the resolution information if the use case has been bound by the
     * {@link androidx.camera.lifecycle.ProcessCameraProvider#bindToLifecycle(LifecycleOwner
     * , CameraSelector, UseCase...)} API, or null if the use case is not bound yet.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    public ResolutionInfo getResolutionInfo() {
        return getResolutionInfoInternal();
    }

    /**
     * Returns a new {@link ResolutionInfo} according to the latest settings of the use case, or
     * null if the use case is not bound yet.
     *
     * <p>This allows the subclasses to return different {@link ResolutionInfo} according to its
     * different design.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    protected ResolutionInfo getResolutionInfoInternal() {
        CameraInternal camera = getCamera();
        Size resolution = getAttachedSurfaceResolution();

        if (camera == null || resolution == null) {
            return null;
        }

        Rect cropRect = getViewPortCropRect();

        if (cropRect == null) {
            cropRect = new Rect(0, 0, resolution.getWidth(), resolution.getHeight());
        }

        int rotationDegrees = getRelativeRotation(camera);

        return ResolutionInfo.create(resolution, cropRect, rotationDegrees);
    }

    enum State {
        /** Currently waiting for image data. */
        ACTIVE,
        /** Currently not waiting for image data. */
        INACTIVE
    }

    /**
     * Callback for when a {@link UseCase} transitions between active/inactive states.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface StateChangeCallback {
        /**
         * Called when a {@link UseCase} becomes active.
         *
         * <p>When a UseCase is active it expects that all data producers attached to itself
         * should start producing data for it to consume. In addition the UseCase will start
         * producing data that other classes can be consumed.
         */
        void onUseCaseActive(@NonNull UseCase useCase);

        /**
         * Called when a {@link UseCase} becomes inactive.
         *
         * <p>When a UseCase is inactive it no longer expects data to be produced for it. In
         * addition the UseCase will stop producing data for other classes to consume.
         */
        void onUseCaseInactive(@NonNull UseCase useCase);

        /**
         * Called when a {@link UseCase} has updated settings.
         *
         * <p>When a {@link UseCase} has updated settings, it is expected that the listener will
         * use these updated settings to reconfigure the listener's own state. A settings update is
         * orthogonal to the active/inactive state change.
         */
        void onUseCaseUpdated(@NonNull UseCase useCase);

        /**
         * Called when a {@link UseCase} has updated settings that require complete reset of the
         * camera.
         *
         * <p>Updating certain parameters of the use case require a full reset of the camera. This
         * includes updating the {@link Surface} used by the use case.
         */
        void onUseCaseReset(@NonNull UseCase useCase);
    }

    /**
     * Callback for when a {@link UseCase} transitions between attach/detach states.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface EventCallback {

        /**
         * Called when use case is attached to a camera.
         *
         * @param cameraInfo that current used.
         */
        void onAttach(@NonNull CameraInfo cameraInfo);

        /**
         * Called when use case is detached from the camera to clear additional resources used
         * for the UseCase.
         */
        void onDetach();
    }
}
