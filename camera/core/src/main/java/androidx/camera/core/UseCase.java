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

import android.util.Log;
import android.util.Size;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraX.LensFacing;
import androidx.camera.core.Config.Option;
import androidx.camera.core.UseCaseConfig.Builder;
import androidx.lifecycle.LifecycleOwner;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The use case which all other use cases are built on top of.
 *
 * <p>A UseCase provides functionality to map the set of arguments in a use case to arguments
 * that are usable by a camera. UseCase also will communicate of the active/inactive state to
 * the Camera.
 */
public abstract class UseCase {
    private static final String TAG = "UseCase";

    /**
     * The set of {@link StateChangeListener} that are currently listening state transitions of this
     * use case.
     */
    private final Set<StateChangeListener> mListeners = new HashSet<>();

    /**
     * A map of camera id and CameraControlInternal. A CameraControlInternal will be attached
     * into the usecase after usecase is bound to lifecycle. It is used for controlling
     * zoom/focus/flash/triggering Af or AE.
     */
    private final Map<String, CameraControlInternal> mAttachedCameraControlMap = new HashMap<>();

    /**
     * A map of the names of the {@link android.hardware.camera2.CameraDevice} to the {@link
     * SessionConfig} that have been attached to this UseCase
     */
    private final Map<String, SessionConfig> mAttachedCameraIdToSessionConfigMap = new HashMap<>();

    /**
     * A map of the names of the {@link android.hardware.camera2.CameraDevice} to the surface
     * resolution that have been attached to this UseCase
     */
    private final Map<String, Size> mAttachedSurfaceResolutionMap = new HashMap<>();

    private State mState = State.INACTIVE;

    private UseCaseConfig<?> mUseCaseConfig;

    /**
     * Except for ImageFormat.JPEG or ImageFormat.YUV, other image formats like SurfaceTexture or
     * MediaCodec classes will be mapped to internal format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED
     * (0x22) in StreamConfigurationMap.java. 0x22 is also the code for ImageFormat.PRIVATE. But
     * there is no ImageFormat.PRIVATE supported before Android level 23. There is same internal
     * code 0x22 for internal corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED.
     * Therefore, setting 0x22 as default image format.
     */
    private int mImageFormat = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

    /**
     * Creates a named instance of the use case.
     *
     * @param useCaseConfig the configuration object used for this use case
     */
    protected UseCase(UseCaseConfig<?> useCaseConfig) {
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
     * @param lensFacing The {@link LensFacing} that the default builder will target to.
     * @return A builder pre-populated with use case default options.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Nullable
    protected UseCaseConfig.Builder<?, ?, ?> getDefaultBuilder(
            CameraX.LensFacing lensFacing) {
        return null;
    }

    /**
     * Updates the stored use case configuration.
     *
     * <p>This configuration will be combined with the default configuration that is contained in
     * the pre-populated builder supplied by {@link #getDefaultBuilder}, if it exists and the
     * behavior of {@link #applyDefaults(UseCaseConfig, Builder)} is not overridden. Once this
     * method returns, the combined use case configuration can be retrieved with
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
    protected void updateUseCaseConfig(UseCaseConfig<?> useCaseConfig) {
        CameraX.LensFacing lensFacing = ((CameraDeviceConfig) useCaseConfig).getLensFacing(null);

        UseCaseConfig.Builder<?, ?, ?> defaultBuilder = getDefaultBuilder(lensFacing);
        if (defaultBuilder == null) {
            Log.w(
                    TAG,
                    "No default configuration available. Relying solely on user-supplied options.");
            mUseCaseConfig = useCaseConfig;
        } else {
            mUseCaseConfig = applyDefaults(useCaseConfig, defaultBuilder);
        }
    }

    /**
     * Combines user-supplied configuration with use case default configuration.
     *
     * <p>This is called during initialization of the class. Subclassess can override this method to
     * modify the behavior of combining user-supplied values and default values.
     *
     * @param userConfig           The user-supplied configuration.
     * @param defaultConfigBuilder A builder containing use-case default values.
     * @return The configuration that will be used by this use case.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected UseCaseConfig<?> applyDefaults(
            UseCaseConfig<?> userConfig,
            UseCaseConfig.Builder<?, ?, ?> defaultConfigBuilder) {

        // If any options need special handling, this is the place to do it. For now we'll just copy
        // over all options.
        for (Option<?> opt : userConfig.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Option<Object> objectOpt = (Option<Object>) opt;

            defaultConfigBuilder.getMutableConfig().insertOption(
                    objectOpt, userConfig.retrieveOption(objectOpt));
        }

        @SuppressWarnings(
                "unchecked") // Since builder is a UseCaseConfig.Builder, it should produce a
                // UseCaseConfig
                UseCaseConfig<?> defaultConfig = defaultConfigBuilder.build();
        return defaultConfig;
    }

    /**
     * Get the names of the cameras which are attached to this use case.
     *
     * <p>The names will correspond to those of the camera as defined by {@link
     * android.hardware.camera2.CameraManager}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Set<String> getAttachedCameraIds() {
        return mAttachedCameraIdToSessionConfigMap.keySet();
    }

    /**
     * Attaches the UseCase to a {@link android.hardware.camera2.CameraDevice} with the
     * corresponding name.
     *
     * @param cameraId The name of the camera as defined by {@link
     *                 android.hardware.camera2.CameraManager#getCameraIdList()}.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void attachToCamera(String cameraId, SessionConfig sessionConfig) {
        mAttachedCameraIdToSessionConfigMap.put(cameraId, sessionConfig);
    }

    /**
     * Add a {@link StateChangeListener}, which listens to this UseCase's active and inactive
     * transition events.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void addStateChangeListener(StateChangeListener listener) {
        mListeners.add(listener);
    }

    /**
     * Attach a CameraControlInternal to this use case.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public final void attachCameraControl(String cameraId, CameraControlInternal cameraControl) {
        mAttachedCameraControlMap.put(cameraId, cameraControl);
        onCameraControlReady(cameraId);
    }

    /** Detach a CameraControlInternal from this use case. */
    final void detachCameraControl(String cameraId) {
        mAttachedCameraControlMap.remove(cameraId);
    }

    /**
     * Remove a {@link StateChangeListener} from listening to this UseCase's active and inactive
     * transition events.
     *
     * <p>If the listener isn't currently listening to the UseCase then this call does nothing.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void removeStateChangeListener(StateChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Get the {@link SessionConfig} for the specified camera id.
     *
     * @param cameraId the id of the camera as referred to be {@link
     *                 android.hardware.camera2.CameraManager}
     * @throws IllegalArgumentException if no camera with the specified cameraId is attached
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public SessionConfig getSessionConfig(String cameraId) {
        SessionConfig sessionConfig = mAttachedCameraIdToSessionConfigMap.get(cameraId);
        if (sessionConfig == null) {
            throw new IllegalArgumentException("Invalid camera: " + cameraId);
        } else {
            return sessionConfig;
        }
    }

    /**
     * Notify all {@link StateChangeListener} that are listening to this UseCase that it has
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
     * Notify all {@link StateChangeListener} that are listening to this UseCase that it has
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
     * Notify all {@link StateChangeListener} that are listening to this UseCase that the
     * settings have been updated.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyUpdated() {
        for (StateChangeListener listener : mListeners) {
            listener.onUseCaseUpdated(this);
        }
    }

    /**
     * Notify all {@link StateChangeListener} that are listening to this UseCase that the use
     * case needs to be completely reset.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyReset() {
        for (StateChangeListener listener : mListeners) {
            listener.onUseCaseReset(this);
        }
    }

    /**
     * Notify all {@link StateChangeListener} that are listening to this UseCase of its current
     * state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected final void notifyState() {
        switch (mState) {
            case INACTIVE:
                for (StateChangeListener listener : mListeners) {
                    listener.onUseCaseInactive(this);
                }
                break;
            case ACTIVE:
                for (StateChangeListener listener : mListeners) {
                    listener.onUseCaseActive(this);
                }
                break;
        }
    }

    /** Clears internal state of this use case. */
    @CallSuper
    protected void clear() {
        EventListener eventListener = mUseCaseConfig.getUseCaseEventListener(null);
        if (eventListener != null) {
            eventListener.onUnbind();
        }

        mListeners.clear();
    }

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
     * Retrieves the currently attached surface resolution.
     *
     * @param cameraId the camera id for the desired surface.
     * @return the currently attached surface resolution for the given camera id.
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public Size getAttachedSurfaceResolution(String cameraId) {
        return mAttachedSurfaceResolutionMap.get(cameraId);
    }

    /**
     * Offers suggested resolutions.
     *
     * <p>The keys of suggestedResolutionMap should only be cameraIds that are valid for this use
     * case.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void updateSuggestedResolution(Map<String, Size> suggestedResolutionMap) {
        Map<String, Size> resolutionMap = onSuggestedResolutionUpdated(suggestedResolutionMap);

        for (Entry<String, Size> entry : resolutionMap.entrySet()) {
            mAttachedSurfaceResolutionMap.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Called when binding new use cases via {@link CameraX#bindToLifecycle(LifecycleOwner,
     * UseCase...)}. Override to create necessary objects like {@link android.media.ImageReader}
     * depending on the resolution.
     *
     * @param suggestedResolutionMap A map of the names of the {@link
     *                               android.hardware.camera2.CameraDevice} to the suggested
     *                               resolution that depends on camera
     *                               device capability and what and how many use cases will be
     *                               bound.
     * @return The map with the resolutions that finally used to create the SessionConfig to
     * attach to the camera device.
     */
    protected abstract Map<String, Size> onSuggestedResolutionUpdated(
            Map<String, Size> suggestedResolutionMap);

    /**
     * Called when CameraControlInternal is attached into the UseCase. UseCase may need to
     * override this method to configure the CameraControlInternal here. Ex. Setting correct flash
     * mode by CameraControlInternal.setFlashMode to enable correct AE mode and flash state.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void onCameraControlReady(String cameraId) {
    }

    /**
     * Called when use case is binding to life cycle via
     * {@link CameraX#bindToLifecycle(LifecycleOwner, UseCase...)}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected void onBind() {
        EventListener eventListener = mUseCaseConfig.getUseCaseEventListener(null);
        if (eventListener != null) {
            eventListener.onBind(getCameraIdUnchecked());
        }
    }

    /**
     * Retrieves a previously attached {@link CameraControlInternal}.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected CameraControlInternal getCameraControl(String cameraId) {
        CameraControlInternal cameraControl = mAttachedCameraControlMap.get(cameraId);
        if (cameraControl == null) {
            return CameraControlInternal.DEFAULT_EMPTY_INSTANCE;
        }
        return cameraControl;
    }

    /**
     * Get image format for the use case.
     *
     * @return image format for the use case
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getImageFormat() {
        return mImageFormat;
    }

    protected void setImageFormat(int imageFormat) {
        mImageFormat = imageFormat;
    }

    enum State {
        /** Currently waiting for image data. */
        ACTIVE,
        /** Currently not waiting for image data. */
        INACTIVE
    }

    /**
     * Listener called when a {@link UseCase} transitions between active/inactive states.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface StateChangeListener {
        /**
         * Called when a {@link UseCase} becomes active.
         *
         * <p>When a UseCase is active it expects that all data producers attached to itself
         * should start producing data for it to consume. In addition the UseCase will start
         * producing data that other classes can be consumed.
         */
        void onUseCaseActive(UseCase useCase);

        /**
         * Called when a {@link UseCase} becomes inactive.
         *
         * <p>When a UseCase is inactive it no longer expects data to be produced for it. In
         * addition the UseCase will stop producing data for other classes to consume.
         */
        void onUseCaseInactive(UseCase useCase);

        /**
         * Called when a {@link UseCase} has updated settings.
         *
         * <p>When a {@link UseCase} has updated settings, it is expected that the listener will
         * use these updated settings to reconfigure the listener's own state. A settings update is
         * orthogonal to the active/inactive state change.
         */
        void onUseCaseUpdated(UseCase useCase);

        /**
         * Called when a {@link UseCase} has updated settings that require complete reset of the
         * camera.
         *
         * <p>Updating certain parameters of the use case require a full reset of the camera. This
         * includes updating the {@link android.view.Surface} used by the use case.
         */
        void onUseCaseReset(UseCase useCase);
    }

    /**
     * Listener called when a {@link UseCase} transitions between bind/unbind states.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface EventListener {

        /**
         * Called when use case was bound to the life cycle.
         *
         * @param cameraId that current used.
         */
        void onBind(String cameraId);

        /**
         * Called when use case was unbind from the life cycle and clear the resource of the use
         * case.
         */
        void onUnbind();
    }

    private String getCameraIdUnchecked() {
        String cameraId = null;
        LensFacing lensFacing = mUseCaseConfig.retrieveOption(
                CameraDeviceConfig.OPTION_LENS_FACING);
        try {
            cameraId = CameraX.getCameraWithLensFacing(lensFacing);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid camera lens facing: " + lensFacing, e);
        }

        return cameraId;
    }

}
