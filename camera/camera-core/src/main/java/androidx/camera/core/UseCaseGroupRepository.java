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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    @GuardedBy("mUseCasesLock")
    final List<LifecycleOwner> mActiveLifecycleOwnerList = new ArrayList<>();
    @GuardedBy("mUseCasesLock")
    LifecycleOwner mCurrentActiveLifecycleOwner = null;

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

        // Need to add observer before creating UseCaseGroupLifecycleController to make sure
        // UseCaseGroups can be stopped before the latest active one is started.
        lifecycleOwner.getLifecycle().addObserver(createLifecycleObserver());
        UseCaseGroupLifecycleController useCaseGroupLifecycleController =
                new UseCaseGroupLifecycleController(lifecycleOwner.getLifecycle());
        synchronized (mUseCasesLock) {
            mLifecycleToUseCaseGroupControllerMap.put(lifecycleOwner,
                    useCaseGroupLifecycleController);
        }
        return useCaseGroupLifecycleController;
    }

    /**
     * Creates a {@link LifecycleObserver} to monitor state change of {@link LifecycleOwner}.
     *
     * @return a new {@link LifecycleObserver}
     */
    private LifecycleObserver createLifecycleObserver() {
        return new LifecycleObserver() {
            /**
             * Monitors which {@link LifecycleOwner} receives an ON_START event and then stop
             * others {@link UseCaseGroup} to keep only one active at a time.
             */
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStart(LifecycleOwner lifecycleOwner) {
                synchronized (mUseCasesLock) {
                    // Only keep the last {@link LifecycleOwner} active. Stop the others.
                    for (Map.Entry<LifecycleOwner, UseCaseGroupLifecycleController> entry :
                            mLifecycleToUseCaseGroupControllerMap.entrySet()) {
                        if (entry.getKey() != lifecycleOwner) {
                            UseCaseGroup useCaseGroup = entry.getValue().getUseCaseGroup();
                            if (useCaseGroup.isActive()) {
                                useCaseGroup.stop();
                            }
                        }
                    }

                    mCurrentActiveLifecycleOwner = lifecycleOwner;
                    mActiveLifecycleOwnerList.add(0, mCurrentActiveLifecycleOwner);
                }
            }

            /**
             * Monitors which {@link LifecycleOwner} receives an ON_STOP event.
             */
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop(LifecycleOwner lifecycleOwner) {
                synchronized (mUseCasesLock) {
                    // Removes stopped lifecycleOwner from active list.
                    mActiveLifecycleOwnerList.remove(lifecycleOwner);
                    if (mCurrentActiveLifecycleOwner == lifecycleOwner) {
                        // If stopped lifecycleOwner is original active one, check whether there
                        // is other active lifecycleOwner and start UseCaseGroup belong to it.
                        if (mActiveLifecycleOwnerList.size() > 0) {
                            mCurrentActiveLifecycleOwner = mActiveLifecycleOwnerList.get(0);
                            mLifecycleToUseCaseGroupControllerMap.get(
                                    mCurrentActiveLifecycleOwner).getUseCaseGroup().start();
                        } else {
                            mCurrentActiveLifecycleOwner = null;
                        }
                    }
                }
            }

            /**
             * Monitors which {@link LifecycleOwner} receives an ON_DESTROY event and then
             * removes any {@link UseCaseGroupLifecycleController} associated with it from this
             * repository when that lifecycle is destroyed.
             */
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
