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
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.UseCaseMediator;
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
 * A repository of {@link UseCaseMediatorLifecycleController} instances.
 *
 * <p>Each {@link UseCaseMediatorLifecycleController} is associated with a {@link LifecycleOwner}
 * that regulates the common lifecycle shared by all the use cases in the mediator.
 */
final class UseCaseMediatorRepository {
    final Object mUseCasesLock = new Object();

    @GuardedBy("mUseCasesLock")
    final Map<LifecycleOwner, UseCaseMediatorLifecycleController>
            mLifecycleToUseCaseMediatorControllerMap =
            new HashMap<>();
    @GuardedBy("mUseCasesLock")
    final List<LifecycleOwner> mActiveLifecycleOwnerList = new ArrayList<>();
    @GuardedBy("mUseCasesLock")
    LifecycleOwner mCurrentActiveLifecycleOwner = null;

    /**
     * Gets an existing {@link UseCaseMediatorLifecycleController} associated with the given {@link
     * LifecycleOwner}, or creates a new {@link UseCaseMediatorLifecycleController} if a mediator
     * does not already exist.
     *
     * <p>The {@link UseCaseMediatorLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the mediator
     */
    UseCaseMediatorLifecycleController getOrCreateUseCaseMediator(LifecycleOwner lifecycleOwner) {
        return getOrCreateUseCaseMediator(lifecycleOwner, useCaseMediator -> {
        });
    }

    /**
     * Gets an existing {@link UseCaseMediatorLifecycleController} associated with the given {@link
     * LifecycleOwner}, or creates a new {@link UseCaseMediatorLifecycleController} if a mediator
     * does not already exist.
     *
     * <p>The {@link UseCaseMediatorLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the mediator
     * @param mediatorSetup  additional setup to do on the mediator if a new instance is created
     */
    UseCaseMediatorLifecycleController getOrCreateUseCaseMediator(
            LifecycleOwner lifecycleOwner, UseCaseMediatorSetup mediatorSetup) {
        UseCaseMediatorLifecycleController useCaseMediatorLifecycleController;
        synchronized (mUseCasesLock) {
            useCaseMediatorLifecycleController = mLifecycleToUseCaseMediatorControllerMap.get(
                    lifecycleOwner);
            if (useCaseMediatorLifecycleController == null) {
                useCaseMediatorLifecycleController = createUseCaseMediator(lifecycleOwner);
                mediatorSetup.setup(useCaseMediatorLifecycleController.getUseCaseMediator());
            }
        }
        return useCaseMediatorLifecycleController;
    }

    /**
     * Creates a new {@link UseCaseMediatorLifecycleController} associated with the given {@link
     * LifecycleOwner} and adds the mediator to the repository.
     *
     * <p>The {@link UseCaseMediatorLifecycleController} is set to be an observer of the {@link
     * LifecycleOwner}.
     *
     * @param lifecycleOwner to associate with the mediator
     * @return a new {@link UseCaseMediatorLifecycleController}
     * @throws IllegalArgumentException if the {@link androidx.lifecycle.Lifecycle} of
     *                                  lifecycleOwner is already
     *                                  {@link androidx.lifecycle.Lifecycle.State.DESTROYED}.
     */
    private UseCaseMediatorLifecycleController createUseCaseMediator(
            LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == State.DESTROYED) {
            throw new IllegalArgumentException(
                    "Trying to create use case mediator with destroyed lifecycle.");
        }

        // Need to add observer before creating UseCaseMediatorLifecycleController to make sure
        // UseCaseMediators can be stopped before the latest active one is started.
        lifecycleOwner.getLifecycle().addObserver(createLifecycleObserver());
        UseCaseMediatorLifecycleController useCaseMediatorLifecycleController =
                new UseCaseMediatorLifecycleController(lifecycleOwner.getLifecycle());
        synchronized (mUseCasesLock) {
            mLifecycleToUseCaseMediatorControllerMap.put(lifecycleOwner,
                    useCaseMediatorLifecycleController);
        }
        return useCaseMediatorLifecycleController;
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
             * others {@link UseCaseMediator} to keep only one active at a time.
             */
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStart(LifecycleOwner lifecycleOwner) {
                synchronized (mUseCasesLock) {
                    // Only keep the last {@link LifecycleOwner} active. Stop the others.
                    for (Map.Entry<LifecycleOwner, UseCaseMediatorLifecycleController> entry :
                            mLifecycleToUseCaseMediatorControllerMap.entrySet()) {
                        if (entry.getKey() != lifecycleOwner) {
                            UseCaseMediator useCaseMediator = entry.getValue().getUseCaseMediator();
                            if (useCaseMediator.isActive()) {
                                useCaseMediator.stop();
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
                        // is other active lifecycleOwner and start UseCaseMediator belong to it.
                        if (mActiveLifecycleOwnerList.size() > 0) {
                            mCurrentActiveLifecycleOwner = mActiveLifecycleOwnerList.get(0);
                            mLifecycleToUseCaseMediatorControllerMap.get(
                                    mCurrentActiveLifecycleOwner).getUseCaseMediator().start();
                        } else {
                            mCurrentActiveLifecycleOwner = null;
                        }
                    }
                }
            }

            /**
             * Monitors which {@link LifecycleOwner} receives an ON_DESTROY event and then
             * removes any {@link UseCaseMediatorLifecycleController} associated with it from this
             * repository when that lifecycle is destroyed.
             */
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy(LifecycleOwner lifecycleOwner) {
                synchronized (mUseCasesLock) {
                    mLifecycleToUseCaseMediatorControllerMap.remove(lifecycleOwner);
                }
                lifecycleOwner.getLifecycle().removeObserver(this);
            }
        };
    }

    Collection<UseCaseMediatorLifecycleController> getUseCaseMediators() {
        synchronized (mUseCasesLock) {
            return Collections.unmodifiableCollection(
                    mLifecycleToUseCaseMediatorControllerMap.values());
        }
    }

    @VisibleForTesting
    Map<LifecycleOwner, UseCaseMediatorLifecycleController> getUseCasesMap() {
        synchronized (mUseCasesLock) {
            return mLifecycleToUseCaseMediatorControllerMap;
        }
    }

    /**
     * The interface for doing additional setup work on a newly created {@link UseCaseMediator}
     * instance.
     */
    public interface UseCaseMediatorSetup {
        void setup(@NonNull UseCaseMediator useCaseMediator);
    }
}
