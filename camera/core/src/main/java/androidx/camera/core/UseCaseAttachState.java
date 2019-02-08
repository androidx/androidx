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

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Collection of use cases which are attached to a specific camera.
 *
 * <p>This class tracks the current state of activity for each use case. There are two states that
 * the use case can be in: online and active. Online means the use case is currently ready for the
 * camera capture, but not currently capturing. Active means the use case is either currently
 * issuing a capture request or one has already been issued.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class UseCaseAttachState {
  private static final String TAG = "UseCaseAttachState";

  /** The set of state and configuration information for an attached use case. */
  private static final class UseCaseAttachInfo {
    UseCaseAttachInfo(SessionConfiguration sessionConfiguration) {
      this.sessionConfiguration = sessionConfiguration;
    }
    /**
     * True if the use case is currently online (i.e. camera should have a capture session
     * configured for it).
     */
    boolean online = false;

    /**
     * True if the use case is currently active (i.e. camera should be issuing capture requests for
     * it).
     */
    boolean active = false;

    /** The configurations required of the camera for the use case. */
    final SessionConfiguration sessionConfiguration;
  }

  /** The name of the camera the use cases are attached to. */
  private final String cameraId;

  /** A map of the use cases to the corresponding state information. */
  private final Map<BaseUseCase, UseCaseAttachInfo> attachedUseCasesToInfoMap = new HashMap<>();

  /** Constructs an instance of the attach state which corresponds to the named camera. */
  public UseCaseAttachState(String cameraId) {
    this.cameraId = cameraId;
  }

  /**
   * Sets the use case to an active state.
   *
   * <p>Adds the use case to the collection if not already in it.
   */
  public void setUseCaseActive(BaseUseCase useCase) {
    UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCase);
    useCaseAttachInfo.active = true;
  }

  /**
   * Sets the use case to an inactive state.
   *
   * <p>Removes the use case from the collection if also offline.
   */
  public void setUseCaseInactive(BaseUseCase useCase) {
    if (!attachedUseCasesToInfoMap.containsKey(useCase)) {
      return;
    }

    UseCaseAttachInfo useCaseAttachInfo = attachedUseCasesToInfoMap.get(useCase);
    useCaseAttachInfo.active = false;
    if (!useCaseAttachInfo.online) {
      attachedUseCasesToInfoMap.remove(useCase);
    }
  }

  /**
   * Sets the use case to an online state.
   *
   * <p>Adds the use case to the collection if not already in it.
   */
  public void setUseCaseOnline(BaseUseCase useCase) {
    UseCaseAttachInfo useCaseAttachInfo = getOrCreateUseCaseAttachInfo(useCase);
    useCaseAttachInfo.online = true;
  }

  /**
   * Sets the use case to an offline state.
   *
   * <p>Removes the use case from the collection if also inactive.
   */
  public void setUseCaseOffline(BaseUseCase useCase) {
    if (!attachedUseCasesToInfoMap.containsKey(useCase)) {
      return;
    }
    UseCaseAttachInfo useCaseAttachInfo = attachedUseCasesToInfoMap.get(useCase);
    useCaseAttachInfo.online = false;
    if (!useCaseAttachInfo.active) {
      attachedUseCasesToInfoMap.remove(useCase);
    }
  }

  public Collection<BaseUseCase> getOnlineUseCases() {
    return Collections.unmodifiableCollection(
        getUseCases(useCaseAttachInfo -> useCaseAttachInfo.online));
  }

  public Collection<BaseUseCase> getActiveAndOnlineUseCases() {
    return Collections.unmodifiableCollection(
        getUseCases(useCaseAttachInfo -> useCaseAttachInfo.active && useCaseAttachInfo.online));
  }

  /**
   * Updates the session configuration for a use case.
   *
   * <p>If the use case is not already in the collection, nothing is done.
   */
  public void updateUseCase(BaseUseCase useCase) {
    if (!attachedUseCasesToInfoMap.containsKey(useCase)) {
      return;
    }

    // Rebuild the attach info from scratch to get the updated SessionConfiguration.
    UseCaseAttachInfo newUseCaseAttachInfo =
        new UseCaseAttachInfo(useCase.getSessionConfiguration(cameraId));

    // Retain the online and active flags.
    UseCaseAttachInfo oldUseCaseAttachInfo = attachedUseCasesToInfoMap.get(useCase);
    newUseCaseAttachInfo.online = oldUseCaseAttachInfo.online;
    newUseCaseAttachInfo.active = oldUseCaseAttachInfo.active;
    attachedUseCasesToInfoMap.put(useCase, newUseCaseAttachInfo);
  }

  /** Returns a session configuration builder for use cases which are both active and online. */
  public SessionConfiguration.ValidatingBuilder getActiveAndOnlineBuilder() {
    SessionConfiguration.ValidatingBuilder validatingBuilder =
        new SessionConfiguration.ValidatingBuilder();

    List<String> list = new ArrayList<>();
    for (Entry<BaseUseCase, UseCaseAttachInfo> attachedUseCase :
        attachedUseCasesToInfoMap.entrySet()) {
      UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
      if (useCaseAttachInfo.active && useCaseAttachInfo.online) {
        BaseUseCase baseUseCase = attachedUseCase.getKey();
        validatingBuilder.add(useCaseAttachInfo.sessionConfiguration);
        list.add(baseUseCase.getName());
      }
    }
    Log.d(TAG, "Active and online use case: " + list + " for camera: " + cameraId);
    return validatingBuilder;
  }

  /** Returns a session configuration builder for use cases which are online. */
  public SessionConfiguration.ValidatingBuilder getOnlineBuilder() {
    SessionConfiguration.ValidatingBuilder validatingBuilder =
        new SessionConfiguration.ValidatingBuilder();
    List<String> list = new ArrayList<>();
    for (Entry<BaseUseCase, UseCaseAttachInfo> attachedUseCase :
        attachedUseCasesToInfoMap.entrySet()) {
      UseCaseAttachInfo useCaseAttachInfo = attachedUseCase.getValue();
      if (useCaseAttachInfo.online) {
        validatingBuilder.add(useCaseAttachInfo.sessionConfiguration);
        BaseUseCase baseUseCase = attachedUseCase.getKey();
        list.add(baseUseCase.getName());
      }
    }
    Log.d(TAG, "All use case: " + list + " for camera: " + cameraId);
    return validatingBuilder;
  }

  private UseCaseAttachInfo getOrCreateUseCaseAttachInfo(BaseUseCase useCase) {
    UseCaseAttachInfo useCaseAttachInfo = attachedUseCasesToInfoMap.get(useCase);
    if (useCaseAttachInfo == null) {
      useCaseAttachInfo = new UseCaseAttachInfo(useCase.getSessionConfiguration(cameraId));
      attachedUseCasesToInfoMap.put(useCase, useCaseAttachInfo);
    }
    return useCaseAttachInfo;
  }

  private Collection<BaseUseCase> getUseCases(AttachStateFilter attachStateFilter) {
    List<BaseUseCase> useCases = new ArrayList<>();
    for (Entry<BaseUseCase, UseCaseAttachInfo> attachedUseCase :
        attachedUseCasesToInfoMap.entrySet()) {
      if (attachStateFilter == null || attachStateFilter.filter(attachedUseCase.getValue())) {
        useCases.add(attachedUseCase.getKey());
      }
    }
    return useCases;
  }

  private interface AttachStateFilter {
    boolean filter(UseCaseAttachInfo attachInfo);
  }
}
