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

import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A collection of {@link BaseUseCase}.
 *
 * <p>The group of {@link BaseUseCase} instances have synchronized interactions with the {@link
 * BaseCamera}.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class UseCaseGroup {
  private static final String TAG = "UseCaseGroup";

  /**
   * The lock for the single {@link StateChangeListener} held by the group.
   *
   * <p>This lock is always acquired prior to acquiring the useCasesLock so that there is no
   * lock-ordering deadlock.
   */
  private final Object listenerLock = new Object();

  @GuardedBy("listenerLock")
  private StateChangeListener listener;

  /**
   * The lock for accessing the map of use case types to use case instances.
   *
   * <p>This lock is always acquired after acquiring the listenerLock so that there is no
   * lock-ordering deadlock.
   */
  private final Object useCasesLock = new Object();

  @GuardedBy("useCasesLock")
  private final Set<BaseUseCase> useCases = new HashSet<>();

  /** Starts all the use cases so that they are brought into an online state. */
  void start() {
    synchronized (listenerLock) {
      if (listener != null) {
        listener.onGroupActive(this);
      }
    }
  }

  /** Stops all the use cases so that they are brought into an offline state. */
  void stop() {
    synchronized (listenerLock) {
      if (listener != null) {
        listener.onGroupInactive(this);
      }
    }
  }

  void setListener(StateChangeListener listener) {
    synchronized (listenerLock) {
      this.listener = listener;
    }
  }

  /**
   * Adds the {@link BaseUseCase} to the group.
   *
   * @return true if the use case is added, or false if the use case already exists in the group.
   */
  public boolean addUseCase(BaseUseCase useCase) {
    synchronized (useCasesLock) {
      return useCases.add(useCase);
    }
  }

  /** Returns true if the {@link BaseUseCase} is contained in the group. */
  boolean contains(BaseUseCase useCase) {
    synchronized (useCasesLock) {
      return useCases.contains(useCase);
    }
  }

  /**
   * Removes the {@link BaseUseCase} from the group.
   *
   * @return Returns true if the use case is removed. Otherwise returns false (if the use case did
   *     not exist in the group).
   */
  boolean removeUseCase(BaseUseCase useCase) {
    synchronized (useCasesLock) {
      return useCases.remove(useCase);
    }
  }

  /** Clears all use cases from this group. */
  public void clear() {
    List<BaseUseCase> useCasesToClear = new ArrayList<>();
    synchronized (useCasesLock) {
      useCasesToClear.addAll(useCases);
      useCases.clear();
    }
    for (BaseUseCase useCase : useCasesToClear) {
      Log.d(TAG, "Clearing use case: " + useCase.getName());
      useCase.clear();
    }
  }

  /** Returns the collection of all the use cases currently contained by the UseCaseGroup. */
  Collection<BaseUseCase> getUseCases() {
    synchronized (useCasesLock) {
      return Collections.unmodifiableCollection(useCases);
    }
  }

  Map<String, Set<BaseUseCase>> getCameraIdToUseCaseMap() {
    Map<String, Set<BaseUseCase>> cameraIdToUseCases = new HashMap<>();
    synchronized (useCasesLock) {
      for (BaseUseCase useCase : useCases) {
        for (String cameraId : useCase.getAttachedCameraIds()) {
          Set<BaseUseCase> useCaseSet = cameraIdToUseCases.get(cameraId);
          if (useCaseSet == null) {
            useCaseSet = new HashSet<>();
          }
          useCaseSet.add(useCase);
          cameraIdToUseCases.put(cameraId, useCaseSet);
        }
      }
    }
    return Collections.unmodifiableMap(cameraIdToUseCases);
  }

  /** Listener called when a {@link UseCaseGroup} transitions between active/inactive states. */
  interface StateChangeListener {
    /**
     * Called when a {@link UseCaseGroup} becomes active.
     *
     * <p>When a UseCaseGroup is active then all the contained {@link BaseUseCase} become online.
     * This means that the {@link BaseCamera} should transition to a state as close as possible to
     * producing, but prior to actually producing data for the use case.
     */
    void onGroupActive(UseCaseGroup useCaseGroup);

    /**
     * Called when a {@link UseCaseGroup} becomes inactive.
     *
     * <p>When a UseCaseGroup is active then all the contained {@link BaseUseCase} become offline.
     */
    void onGroupInactive(UseCaseGroup useCaseGroup);
  }
}
