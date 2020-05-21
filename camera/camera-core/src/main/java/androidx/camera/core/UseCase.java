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

import android.graphics.Rect;
import android.media.ImageReader;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraControlInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.Config.Option;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.SessionConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.core.util.Preconditions;

import java.util.Collections;
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
    /**
     * The set of {@link StateChangeCallback} that are currently listening state transitions of this
     * use case.
     */
    private final Set<StateChangeCallback> mStateChangeCallbacks = new HashSet<>();

    // The currently attached session config
    private SessionConfig mAttachedSessionConfig = SessionConfig.defaultEmptySessionConfig();

    /**
     * The resolution assigned to the {@link UseCase} based on the attached camera.
     */
    private Size mAttachedResolution;

    /**
     * The crop rect calculated at the time of binding based on {@link ViewPort}.
     */
    @Nullable
    private Rect mViewPortCropRect;

    private State mState = State.INACTIVE;

    private UseCaseConfig<?> mUseCaseConfig;

    private final Object mCameraLock = new Object();
    @GuardedBy("mCameraLock")
    private CameraInternal mCamera;

    /**
     * Creates a named instance of the use case.
     *
     * @param useCaseConfig the configuration object used for this use case
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCase(@NonNull UseCaseConfig<?> useCaseConfig) {
        updateUseCaseConfig(useCaseConfig);
    }

    /**
     * Returns a use case configuration pre-populated with default configuration
     * options.
     *
     * <p>This is used to generate a final configuration by combining the user-supplied
     * configuration with the default configuration. Subclasses can override this method to provide
     * the pre-populated builder. If <code>null</code> is returned, then the user-supplied
     * configuration will be used directly.
     *
     * @param cameraInfo The {@link CameraInfo} of the camera that the default builder will
     *                   target to, null if it doesn't target to any camera.
     * @return A builder pre-populated with use case default options.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(@Nullable CameraInfo cameraInfo) {
        return null;
    }

    /**
     * Updates the stored use case configuration.
     *
     * <p>This configuration will be combined with the default configuration that is contained in
     * the pre-populated builder supplied by {@link #getDefaultBuilder}, if it exists and the
     * behavior of {@link #applyDefaults(UseCaseConfig, UseCaseConfig.Builder)} is not overridden.
     * Once this method returns, the combined use case configuration can be retrieved with
     * {@link #getUseCaseConfig()}.
     *
     * <p>This method alone will not make any changes to the {@link SessionConfig}, it is up to
     * the use case to decide when to modify the session configuration.
     *
     * @param useCaseConfig Configuration which will be applied on top of use case defaults, if a
     *                      default builder is provided by {@link #getDefaultBuilder}.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void updateUseCaseConfig(@NonNull UseCaseConfig<?> useCaseConfig) {
        // Attempt to retrieve builder containing defaults for this use case's config
        UseCaseConfig.Builder<?, ?, ?> defaultBuilder =
                getDefaultBuilder(getCamera() == null ? null : getCamera().getCameraInfo());

        // Combine with default configuration.
        mUseCaseConfig = applyDefaults(useCaseConfig, defaultBuilder);
    }

    /**
     * Combines user-supplied configuration with use case default configuration.
     *
     * <p>Subclasses can override this method to
     * modify the behavior of combining user-supplied values and default values.
     *
     * @param userConfig           The user-supplied configuration.
     * @param defaultConfigBuilder A builder containing use-case default values, or {@code null}
     *                             if no default values exist.
     * @return The configuration that will be used by this use case.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    protected UseCaseConfig<?> applyDefaults(
            @NonNull UseCaseConfig<?> userConfig,
            @Nullable UseCaseConfig.Builder<?, ?, ?> defaultConfigBuilder) {
        if (defaultConfigBuilder == null) {
            // No default builder was retrieved, return config directly
            return userConfig;
        }

        MutableConfig defaultMutableConfig = defaultConfigBuilder.getMutableConfig();

        // If OPTION_TARGET_ASPECT_RATIO has been set by the user, remove
        // OPTION_TARGET_ASPECT_RATIO_CUSTOM from defaultConfigBuilder. Otherwise, it may cause
        // aspect ratio mismatched issue.
        if (userConfig.containsOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO)
                && defaultMutableConfig.containsOption(
                ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO_CUSTOM)) {
            defaultMutableConfig.removeOption(ImageOutputConfig.OPTION_TARGET_ASPECT_RATIO_CUSTOM);
        }

        // If any options need special handling, this is the place to do it. For now we'll just copy
        // over all options.
        for (Option<?> opt : userConfig.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Option<Object> objectOpt = (Option<Object>) opt;

            defaultMutableConfig.insertOption(objectOpt,
                    userConfig.getOptionPriority(opt), userConfig.retrieveOption(objectOpt));
        }

        return defaultConfigBuilder.getUseCaseConfig();
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
    protected final void notifyState() {
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

    /**
     * Clears internal state of this use case.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void clear() {
    }

    /**
     * Called use case is unbound from lifecycle or the bound lifecycle is destroyed.
     *
     * TODO(b/152430679): remove this once UseCase can be reused. UseCase holds reference to user
     * callbacks which causes memory leak. (see https://issuetracker.google.com/141188637) As
     * long as the UseCase is never reused, it's safe to clear user callbacks when the lifecycle
     * ends or unbinds. The proper fix should be breaking reference between Camera->UseCase
     * when camera is detached (ON_STOP).
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onDestroy() {
    }

    /** @hide */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @NonNull
    public String getName() {
        return mUseCaseConfig.getTargetName("<UnknownUseCase-" + this.hashCode() + ">");
    }

    /**
     * Retrieves the configuration used by this use case.
     *
     * @return the configuration used by this use case.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public UseCaseConfig<?> getUseCaseConfig() {
        return mUseCaseConfig;
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
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void onAttach(@NonNull CameraInternal camera) {
        synchronized (mCameraLock) {
            mCamera = camera;
            addStateChangeCallback(camera);
        }
        updateUseCaseConfig(mUseCaseConfig);
        EventCallback eventCallback = mUseCaseConfig.getUseCaseEventCallback(null);
        if (eventCallback != null) {
            eventCallback.onBind(camera.getCameraInfoInternal().getCameraId());
        }
        onCameraControlReady();
    }

    /**
     * Called when use case is detaching from a camera.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public void onDetach() {
        // Do any cleanup required by the UseCase implementation
        clear();

        // Cleanup required for any type of UseCase
        EventCallback eventCallback = mUseCaseConfig.getUseCaseEventCallback(null);
        if (eventCallback != null) {
            eventCallback.onUnbind();
        }

        synchronized (mCameraLock) {
            if (mCamera != null) {
                mCamera.detachUseCases(Collections.singleton(this));
                removeStateChangeCallback(mCamera);
                mCamera = null;
            }
        }
    }

    /**
     * Called when use case is attached to the camera. This method is called on main thread.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void onStateAttached() {
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
    protected void setViewPortCropRect(@Nullable Rect viewPortCropRect) {
        mViewPortCropRect = viewPortCropRect;
    }

    /**
     * Gets the view port crop rect.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    protected Rect getViewPortCropRect() {
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
        return mUseCaseConfig.getInputFormat();
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
     * Callback for when a {@link UseCase} transitions between bind/unbind states.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface EventCallback {

        /**
         * Called when use case was bound to the life cycle.
         *
         * @param cameraId that current used.
         */
        void onBind(@NonNull String cameraId);

        /**
         * Called when use case was unbind from the life cycle and clear the resource of the use
         * case.
         */
        void onUnbind();
    }
}
