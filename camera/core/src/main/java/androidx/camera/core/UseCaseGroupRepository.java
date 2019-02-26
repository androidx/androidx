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
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Lifecycle.State;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A repository of {@link UseCaseGroupLifecycleController} instances.
 *
 * <p>Each {@link UseCaseGroupLifecycleController} is associated with a {@link LifecycleOwner} that
 * regulates the common lifecycle shared by all the use cases in the group.
 */
final class UseCaseGroupRepository {
    final Object mUseCasesLock = new Object();

    @GuardedBy("mUseCasesLock")
    final Map<LifecycleOwner, UseCaseGroupLifecycleController>
            mLifecycleToUseCaseGroupControllerMap =
            new HashMap<>();

    /**
     * Gets an existing {@link UseCaseGroupLifecycleController} associated with the given {@link
     * LifecycleOwner}, or creates a new {@link UseCaseGroupLifecycleController} if a group does not
     * already exist.
     *
     * <p>The {@link UseCaseGroupLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the group
     */
    UseCaseGroupLifecycleController getOrCreateUseCaseGroup(LifecycleOwner lifecycleOwner) {
        return getOrCreateUseCaseGroup(lifecycleOwner, new UseCaseGroupSetup() {
            @Override
            public void setup(UseCaseGroup useCaseGroup) {
            }
        });
    }

    /**
     * Gets an existing {@link UseCaseGroupLifecycleController} associated with the given {@link
     * LifecycleOwner}, or creates a new {@link UseCaseGroupLifecycleController} if a group does not
     * already exist.
     *
     * <p>The {@link UseCaseGroupLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the group
     * @param groupSetup     additional setup to do on the group if a new instance is created
     */
    UseCaseGroupLifecycleController getOrCreateUseCaseGroup(
            LifecycleOwner lifecycleOwner, UseCaseGroupSetup groupSetup) {
        UseCaseGroupLifecycleController useCaseGroupLifecycleController;
        synchronized (mUseCasesLock) {
            useCaseGroupLifecycleController = mLifecycleToUseCaseGroupControllerMap.get(
                    lifecycleOwner);
            if (useCaseGroupLifecycleController == null) {
                useCaseGroupLifecycleController = createUseCaseGroup(lifecycleOwner);
                groupSetup.setup(useCaseGroupLifecycleController.getUseCaseGroup());
            }
        }
        return useCaseGroupLifecycleController;
    }

    /**
     * Creates a new {@link UseCaseGroupLifecycleController} associated with the given {@link
     * LifecycleOwner} and adds the group to the repository.
     *
     * <p>The {@link UseCaseGroupLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the group
     * @return a new {@link UseCaseGroupLifecycleController}
     * @throws IllegalArgumentException if the {@link androidx.lifecycle.Lifecycle} of
     *                                  lifecycleOwner is already
     *                                  {@link androidx.lifecycle.Lifecycle.State.DESTROYED}.
     */
    private UseCaseGroupLifecycleController createUseCaseGroup(LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == State.DESTROYED) {
            throw new IllegalArgumentException(
                    "Trying to create use case group with destroyed lifecycle.");
        }

        UseCaseGroupLifecycleController useCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle());
        lifecycleOwner.getLifecycle().addObserver(createRemoveOnDestroyObserver());
        synchronized (mUseCasesLock) {
            mLifecycleToUseCaseGroupControllerMap.put(lifecycleOwner,
                    useCaseGroupLifecycleController);
        }
        return useCaseGroupLifecycleController;
    }

    /**
     * Creates a {@link LifecycleObserver} which removes any {@link
     * UseCaseGroupLifecycleController} associated with a {@link LifecycleOwner} from this
     * repository when that lifecycle is destroyed.
     *
     * @return a new {@link LifecycleObserver}
     */
    private LifecycleObserver createRemoveOnDestroyObserver() {
        return new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy(LifecycleOwner lifecycleOwner) {
                synchronized (mUseCasesLock) {
                    mLifecycleToUseCaseGroupControllerMap.remove(lifecycleOwner);
                }
                lifecycleOwner.getLifecycle().removeObserver(this);
            }
        };
    }

    Collection<UseCaseGroupLifecycleController> getUseCaseGroups() {
        synchronized (mUseCasesLock) {
            return Collections.unmodifiableCollection(
                    mLifecycleToUseCaseGroupControllerMap.values());
        }
    }

    @VisibleForTesting
    Map<LifecycleOwner, UseCaseGroupLifecycleController> getUseCasesMap() {
        synchronized (mUseCasesLock) {
            return mLifecycleToUseCaseGroupControllerMap;
        }
    }

    /**
     * The interface for doing additional setup work on a newly created {@link UseCaseGroup}
     * instance.
     */
    public interface UseCaseGroupSetup {
        void setup(UseCaseGroup useCaseGroup);
    }
}
