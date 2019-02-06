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

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.util.Log;
import android.util.Size;
import androidx.camera.core.Configuration.Option;
import androidx.camera.core.UseCaseConfiguration.Builder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The use case which all other use cases are built on top of.
 *
 * <p>A BaseUseCase provides functionality to map a {@link BaseCamera} to a {@link
 * SessionConfiguration} and the communication of the active/inactive state to the Camera.
 */
public abstract class BaseUseCase {
  private static final String TAG = "BaseUseCase";

  /**
   * The set of {@link StateChangeListener} that are currently listening state transitions of this
   * use case.
   */
  private final Set<StateChangeListener> listeners = new HashSet<>();

  /**
   * A map of camera id and CameraControl. A CameraControl will be attached into the usecase after
   * usecase is bound to lifecycle. It is used for controlling zoom/focus/flash/triggering Af or AE.
   */
  private final Map<String, CameraControl> attachedCameraControlMap = new HashMap<>();

  /**
   * A map of the names of the {@link android.hardware.camera2.CameraDevice} to the {@link
   * SessionConfiguration} that have been attached to this BaseUseCase
   */
  private final Map<String, SessionConfiguration> attachedCameraIdToSessionConfigurationMap =
      new HashMap<>();

  /**
   * A map of the names of the {@link android.hardware.camera2.CameraDevice} to the surface
   * resolution that have been attached to this BaseUseCase
   */
  private final Map<String, Size> attachedSurfaceResolutionMap = new HashMap<>();

  private State state = State.INACTIVE;

  private UseCaseConfiguration<?> useCaseConfiguration;

  /**
   * Except for ImageFormat.JPEG or ImageFormat.YUV, other image formats like SurfaceTexture or
   * MediaCodec classes will be mapped to internal format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED
   * (0x22) in StreamConfigurationMap.java. 0x22 is also the code for ImageFormat.PRIVATE. But there
   * is no ImageFormat.PRIVATE supported before Android level 23. There is same internal code 0x22
   * for internal corresponding format HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED. Therefore, setting
   * 0x22 as default image format.
   */
  private int imageFormat = ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;

  enum State {
    /** Currently waiting for image data. */
    ACTIVE,
    /** Currently not waiting for image data. */
    INACTIVE
  }

  /**
   * Creates a named instance of the use case.
   *
   * @param useCaseConfiguration the configuration object used for this use case
   */
  protected BaseUseCase(UseCaseConfiguration<?> useCaseConfiguration) {
    updateUseCaseConfiguration(useCaseConfiguration);
  }

  /**
   * Returns a {@link UseCaseConfiguration.Builder} pre-populated with default configuration
   * options.
   *
   * <p>This is used to generate a final configuration by combining the user-supplied configuration
   * with the default configuration. Subclasses can override this method to provide the
   * pre-populated builder. If <code>null</code> is returned, then the user-supplied configuration
   * will be used directly.
   *
   * @return A builder pre-populated with use case default options.
   */
  @Nullable
  protected UseCaseConfiguration.Builder<?, ?, ?> getDefaultBuilder() {
    return null;
  }

  /**
   * Updates the stored use case configuration.
   *
   * <p>This configuration will be combined with the default configuration that is contained in the
   * pre-populated builder supplied by {@link #getDefaultBuilder()}, if it exists and the behavior
   * of {@link #applyDefaults(UseCaseConfiguration, Builder)} is not overridden. Once this method
   * returns, the combined use case configuration can be retrieved with {@link
   * #getUseCaseConfiguration()}.
   *
   * <p>This method alone will not make any changes to the {@link SessionConfiguration}, it is up to
   * the use case to decide when to modify the session configuration.
   *
   * @param useCaseConfiguration Configuration which will be applied on top of use case defaults, if
   *     a default builder is provided by {@link #getDefaultBuilder()}.
   */
  protected void updateUseCaseConfiguration(UseCaseConfiguration<?> useCaseConfiguration) {
    UseCaseConfiguration.Builder<?, ?, ?> defaultBuilder = getDefaultBuilder();
    if (defaultBuilder == null) {
      Log.w(TAG, "No default configuration available. Relying solely on user-supplied options.");
      this.useCaseConfiguration = useCaseConfiguration;
    } else {
      this.useCaseConfiguration = applyDefaults(useCaseConfiguration, defaultBuilder);
    }
  }

  /**
   * Combines user-supplied configuration with use case default configuration.
   *
   * <p>This is called during initialization of the class. Subclassess can override this method to
   * modify the behavior of combining user-supplied values and default values.
   *
   * @param userConfiguration The user-supplied configuration.
   * @param defaultConfigBuilder A builder containing use-case default values.
   * @return The configuration that will be used by this use case.
   */
  protected UseCaseConfiguration<?> applyDefaults(
      UseCaseConfiguration<?> userConfiguration,
      UseCaseConfiguration.Builder<?, ?, ?> defaultConfigBuilder) {

    // If any options need special handling, this is the place to do it. For now we'll just copy
    // over all options.
    for (Option<?> opt : userConfiguration.listOptions()) {
      @SuppressWarnings("unchecked") // Options/values are being copied directly
      Option<Object> objectOpt = (Option<Object>) opt;
      defaultConfigBuilder.insertOption(objectOpt, userConfiguration.retrieveOption(objectOpt));
    }

    @SuppressWarnings(
        "unchecked") // Since builder is a UseCaseConfiguration.Builder, it should produce a
    // UseCaseConfiguration
    UseCaseConfiguration<?> defaultConfig = (UseCaseConfiguration<?>) defaultConfigBuilder.build();
    return defaultConfig;
  }

  /**
   * Get the names of the cameras which are attached to this use case.
   *
   * <p>The names will correspond to those of the camera as defined by {@link
   * android.hardware.camera2.CameraManager}.
   */
  Set<String> getAttachedCameraIds() {
    return attachedCameraIdToSessionConfigurationMap.keySet();
  }

  /**
   * Attaches the BaseUseCase to a {@link android.hardware.camera2.CameraDevice} with the
   * corresponding name.
   *
   * @param cameraId The name of the camera as defined by {@link
   *     android.hardware.camera2.CameraManager#getCameraIdList()}.
   */
  protected void attachToCamera(String cameraId, SessionConfiguration sessionConfiguration) {
    attachedCameraIdToSessionConfigurationMap.put(cameraId, sessionConfiguration);
  }

  /**
   * Add a {@link StateChangeListener}, which listens to this BaseUseCase's active and inactive
   * transition events.
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public void addStateChangeListener(StateChangeListener listener) {
    listeners.add(listener);
  }

  /**
   * Attach a CameraControl to this use case.
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public final void attachCameraControl(String cameraId, CameraControl cameraControl) {
    attachedCameraControlMap.put(cameraId, cameraControl);
    onCameraControlReady(cameraId);
  }

  /** Detach a CameraControl from this use case. */
  final void detachCameraControl(String cameraId) {
    attachedCameraControlMap.remove(cameraId);
  }

  /**
   * Remove a {@link StateChangeListener} from listening to this BaseUseCase's active and inactive
   * transition events.
   *
   * <p>If the listener isn't currently listening to the BaseUseCase then this call does nothing.
   */
  void removeStateChangeListener(StateChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * Get the {@link SessionConfiguration} for the specified camera id.
   *
   * @param cameraId the id of the camera as referred to be {@link
   *     android.hardware.camera2.CameraManager}
   * @throws IllegalArgumentException if no camera with the specified cameraId is attached
   */
  public SessionConfiguration getSessionConfiguration(String cameraId) {
    SessionConfiguration sessionConfiguration =
        attachedCameraIdToSessionConfigurationMap.get(cameraId);
    if (sessionConfiguration == null) {
      throw new IllegalArgumentException("Invalid camera: " + cameraId);
    } else {
      return sessionConfiguration;
    }
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase that it has
   * transitioned to an active state.
   */
  protected final void notifyActive() {
    state = State.ACTIVE;
    notifyState();
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase that it has
   * transitioned to an inactive state.
   */
  protected final void notifyInactive() {
    state = State.INACTIVE;
    notifyState();
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase that it has a
   * single capture request.
   */
  protected final void notifySingleCapture(CaptureRequestConfiguration requestConfiguration) {
    for (StateChangeListener listener : listeners) {
      listener.onUseCaseSingleRequest(this, requestConfiguration);
    }
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase that the settings
   * have been updated.
   */
  protected final void notifyUpdated() {
    for (StateChangeListener listener : listeners) {
      listener.onUseCaseUpdated(this);
    }
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase that the use case
   * needs to be completely reset.
   */
  protected final void notifyReset() {
    for (StateChangeListener listener : listeners) {
      listener.onUseCaseReset(this);
    }
  }

  /**
   * Notify all {@link StateChangeListener} that are listening to this BaseUseCase of its current
   * state.
   */
  protected final void notifyState() {
    switch (state) {
      case INACTIVE:
        for (StateChangeListener listener : listeners) {
          listener.onUseCaseInactive(this);
        }
        break;
      case ACTIVE:
        for (StateChangeListener listener : listeners) {
          listener.onUseCaseActive(this);
        }
        break;
    }
  }

  /** Clear out all {@link StateChangeListener} from listening to this BaseUseCase. */
  @CallSuper
  protected void clear() {
    listeners.clear();
  }

  public String getName() {
    return useCaseConfiguration.getTargetName("<UnknownUseCase-" + this.hashCode() + ">");
  }

  /**
   * Retrieves the configuration used by this use case.
   *
   * @return the configuration used by this use case.
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public UseCaseConfiguration<?> getUseCaseConfiguration() {
    return useCaseConfiguration;
  }

  /**
   * Retrieves the currently attached surface resolution.
   *
   * @param cameraId the camera id for the desired surface.
   * @return the currently attached surface resolution for the given camera id.
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public Size getAttachedSurfaceResolution(String cameraId) {
    return attachedSurfaceResolutionMap.get(cameraId);
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
      attachedSurfaceResolutionMap.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Called when binding new use cases via {@link CameraX#bindToLifecycle(LifecycleOwner,
   * BaseUseCase...)}. Need to override this function to create {@link SessionConfiguration} or
   * other necessary objects like {@link android.media.ImageReader} depending on the resolution.
   *
   * @param suggestedResolutionMap A map of the names of the {@link
   *     android.hardware.camera2.CameraDevice} to the suggested resolution that depends on camera
   *     device capability and what and how many use cases will be bound.
   * @return The map with the resolutions that finally used to create the SessionConfiguration to
   *     attach to the camera device.
   */
  protected abstract Map<String, Size> onSuggestedResolutionUpdated(
      Map<String, Size> suggestedResolutionMap);

  /**
   * Called when CameraControl is attached into the UseCase. UseCase may need to override this
   * method to configure the CameraControl here. Ex. Setting correct flash mode by
   * CameraControl.setFlashMode to enable correct AE mode and flash state.
   * @hide
   */
  protected void onCameraControlReady(String cameraId) {}

  /**
   * Retrieves a previously attached {@link CameraControl}.
   * @hide
   */
  protected CameraControl getCameraControl(String cameraId) {
    CameraControl cameraControl = attachedCameraControlMap.get(cameraId);
    if (cameraControl == null) {
      return CameraControl.defaultEmptyInstance();
    }
    return cameraControl;
  }

  protected void setImageFormat(int imageFormat) {
    this.imageFormat = imageFormat;
  }

  /**
   * Get image format for the use case.
   *
   * @return image format for the use case
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public int getImageFormat() {
    return imageFormat;
  }

  /**
   * Listener called when a {@link BaseUseCase} transitions between active/inactive states.
   *
   * @hide
   */
  @RestrictTo(Scope.LIBRARY_GROUP)
  public interface StateChangeListener {
    /**
     * Called when a {@link BaseUseCase} becomes active.
     *
     * <p>When a BaseUseCase is active it expects that all data producers attached to itself should
     * start producing data for it to consume. In addition the BaseUseCase will start producing data
     * that other classes can be consumed.
     */
    void onUseCaseActive(BaseUseCase useCase);

    /**
     * Called when a {@link BaseUseCase} becomes inactive.
     *
     * <p>When a BaseUseCase is inactive it no longer expects data to be produced for it. In
     * addition the BaseUseCase will stop producing data for other classes to consume.
     */
    void onUseCaseInactive(BaseUseCase useCase);

    /**
     * Called when a {@link BaseUseCase} has updated settings.
     *
     * <p>When a {@link BaseUseCase} has updated settings, it is expected that the listener will use
     * these updated settings to reconfigure the listener's own state. A settings update is
     * orthogonal to the active/inactive state change.
     */
    void onUseCaseUpdated(BaseUseCase useCase);

    /**
     * Called when a {@link BaseUseCase} has updated settings that require complete reset of the
     * camera.
     *
     * <p>Updating certain parameters of the use case require a full reset of the camera. This
     * includes updating the {@link android.view.Surface} used by the use case.
     */
    void onUseCaseReset(BaseUseCase useCase);

    /**
     * Called when a {@link BaseUseCase} need a single capture request
     *
     * @param captureRequestConfiguration used to construct the single capture request
     */
    void onUseCaseSingleRequest(
        BaseUseCase useCase, CaptureRequestConfiguration captureRequestConfiguration);
  }
}
