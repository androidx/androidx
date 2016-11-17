/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.lifecycle;

import static com.android.support.lifecycle.Lifecycle.DESTROYED;
import static com.android.support.lifecycle.Lifecycle.STARTED;

import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

/**
 * LiveData is a data reference that can be observed withing a given lifecycle.
 * <p>
 * The Observers of LiveData must specify their LifecycleProvider, which allows LiveData to observe
 * the provider's state changes and unsubscribe the observer when necessary.
 *
 * @param <T> The type of data hold by this instance
 */
@SuppressWarnings({"WeakerAccess", "unused"})
// TODO the usage of ObserverSet needs to be cleaned. Maybe we should simplify the rules.
public class LiveData<T> {
    private static final int START_VERSION = -1;
    private static final Object NOT_SET = new Object();
    private boolean mPendingActiveChanges = false;

    @VisibleForTesting
    ObserverSet<LifecycleBoundObserver> mObservers =
            new ObserverSet<LifecycleBoundObserver>() {
                @Override
                protected boolean checkEquality(LifecycleBoundObserver existing,
                        LifecycleBoundObserver added) {
                    if (existing.observer == added.observer) {
                        if (existing.provider != added.provider) {
                            throw new IllegalArgumentException("Cannot add the same observer twice"
                                    + " to the LiveData");
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                protected void onAdded(LifecycleBoundObserver observer) {
                    observer.onAdded();
                    mObserverCount++;
                    if (observer.active) {
                        mActiveCount++;
                        if (mActiveCount == 1) {
                            onActive();
                        }
                    }
                    if (mData != NOT_SET) {
                        //noinspection unchecked
                        observer.considerNotify((T) mData, mVersion);
                    }
                }

                @Override
                protected void onRemoved(LifecycleBoundObserver observer) {
                    if (observer.active) {
                        mActiveCount--;
                        if (mActiveCount == 0) {
                            onInactive();
                        }
                    }
                    mObserverCount--;
                    observer.onRemoved();
                }

                @Override
                protected void onSync() {
                    if (!mPendingActiveChanges) {
                        return;
                    }
                    forEach(mUpdateActiveCount);
                    mPendingActiveChanges = false;
                }
            };

    // how many observers are in active state
    private int mActiveCount = 0;
    // how many observers do we have
    private int mObserverCount = 0;

    private Object mData = NOT_SET;
    private int mVersion = START_VERSION;

    private ObserverSet.Callback<LifecycleBoundObserver> mDispatchCallback =
            new ObserverSet.Callback<LifecycleBoundObserver>() {
                @Override
                public void run(LifecycleBoundObserver observer) {
                    //noinspection unchecked
                    observer.considerNotify((T) mData, mVersion);
                }
            };

    private ObserverSet.Callback<LifecycleBoundObserver> mUpdateActiveCount =
            new ObserverSet.Callback<LifecycleBoundObserver>() {
                @Override
                public void run(LifecycleBoundObserver observer) {
                    if (observer.pendingActiveStateChange == null) {
                        return;
                    }
                    boolean newValue = observer.pendingActiveStateChange;
                    observer.pendingActiveStateChange = null;
                    observer.active = newValue;
                    if (newValue) {
                        mActiveCount++;
                        if (mActiveCount == 1) {
                            onActive();
                        }
                        if (mData != NOT_SET) {
                            //noinspection unchecked
                            observer.considerNotify((T) mData, mVersion);
                        }
                    } else {
                        mActiveCount--;
                        if (mActiveCount == 0) {
                            onInactive();
                        }
                    }
                }
            };
    /**
     * Adds the given observer to the observers list within the lifespan of the given provider. The
     * events are dispatched on the main thread. If LiveData already has data set, it is instantly
     * delivered to the observer before this call returns.
     * <p>
     * The observer will only receive events if the provider is in {@link Lifecycle#STARTED} or
     * {@link Lifecycle#RESUMED} state (active).
     * <p>
     * If the provider moves to the {@link Lifecycle#DESTROYED} state, the observer will
     * automatically be removed.
     * <p>
     * When data changes while the {@code provider} is not active, it will not receive any updates.
     * If it becomes active again, it will receive the last available data automatically.
     * <p>
     * LiveData keeps a strong reference to the observer and the provider as long as the given
     * LifecycleProvider is not destroyed. When it is destroyed, LiveData removes references to
     * the observer & the provider.
     * <p>
     * If the given provider is already in {@link Lifecycle#DESTROYED} state, LiveData ignores the
     * call.
     * <p>
     * If the given provider, observer tuple is already in the list, the call is ignored.
     * If the observer is already in the list with another provider, LiveData throws an
     * {@link IllegalArgumentException}.
     *
     * @param provider The LifecycleProvider which controls the observer
     * @param observer The observer that will receive the events
     */
    @MainThread
    public void observe(LifecycleProvider provider, Observer<T> observer) {
        if (provider.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        final LifecycleBoundObserver wrapper = new LifecycleBoundObserver(provider, observer);
        mObservers.add(wrapper);
    }

    /**
     * Removes the given observer from the observers list.
     *
     * @param observer The Observer to receive events.
     */
    @MainThread
    public void removeObserver(final Observer<T> observer) {
        // TODO make it efficient
        mObservers.forEach(new ObserverSet.Callback<LifecycleBoundObserver>() {
            @Override
            public void run(LifecycleBoundObserver key) {
                if (key.observer == observer) {
                    mObservers.remove(key);
                }
            }
        });
    }

    /**
     * Removes all observers that are tied to the given LifecycleProvider.
     *
     * @param provider The provider scope for the observers to be removed.
     */
    @MainThread
    public void removeObservers(final LifecycleProvider provider) {
        // TODO make it efficient
        mObservers.forEach(new ObserverSet.Callback<LifecycleBoundObserver>() {
            @Override
            public void run(LifecycleBoundObserver key) {
                if (key.provider == provider) {
                    mObservers.remove(key);
                }
            }
        });
    }

    /**
     * Sets the value. If there are active observers, the value will be dispatched to them.
     *
     * @param value The new value
     */
    @MainThread
    public void setValue(T value) {
        mVersion++;
        mData = value;
        mObservers.forEach(mDispatchCallback);
    }

    /**
     * Returns the current value.
     * Note that calling this method on a background thread does not guarantee that the latest
     * value set will be received.
     *
     * @return the current value
     */
    @Nullable
    public T getValue() {
        Object data = mData;
        if (mData != NOT_SET) {
            //noinspection unchecked
            return (T) mData;
        }
        return null;
    }

    /**
     * Called when the number of active observers change to 1 from 0.
     * <p>
     * This callback can be used to know that this LiveData is being used thus should be kept
     * up to date.
     */
    protected void onActive() {

    }

    /**
     * Called when the number of active observers change from 1 to 0.
     * <p>
     * This does not mean that there are no observers left, there may still be observers but their
     * lifecycle states is not {@link Lifecycle#STARTED} or {@link Lifecycle#STOPPED} (like an
     * Activity in the back stack).
     * <p>
     * You can get the number of observers via {@link #getObserverCount()}.
     */
    protected void onInactive() {

    }

    /**
     * Returns the number of observers.
     *
     * @return The number of observers
     */
    @MainThread
    public int getObserverCount() {
        return mObserverCount;
    }

    /**
     * Returns the number of active observers.
     *
     * @return The number of active observers
     */
    public int getActiveObserverCount() {
        return mActiveCount;
    }

    class LifecycleBoundObserver implements LifecycleObserver {
        public final LifecycleProvider provider;
        public final Observer<T> observer;
        public boolean active;
        public Boolean pendingActiveStateChange;
        public int lastVersion = START_VERSION;

        LifecycleBoundObserver(LifecycleProvider provider, Observer<T> observer) {
            this.provider = provider;
            this.observer = observer;
        }

        private void onAdded() {
            active = isActiveState(provider.getLifecycle().getCurrentState());
            provider.getLifecycle().addObserver(this);
        }

        public void onRemoved() {
            provider.getLifecycle().removeObserver(this);
        }

        void considerNotify(T data, int version) {
            if (!active) {
                return;
            }
            if (lastVersion >= version) {
                return;
            }
            lastVersion = version;
            observer.onChanged(data);
        }

        @SuppressWarnings("unused")
        @OnLifecycleEvent(Lifecycle.ANY)
        void onStateChange() {
            if (provider.getLifecycle().getCurrentState() == DESTROYED) {
                removeInternal(this);
                return;
            }
            boolean activeNow = isActiveState(provider.getLifecycle().getCurrentState());
            if (pendingActiveStateChange == null) {
                if (activeNow != active) {
                    pendingActiveStateChange = activeNow;
                    onActiveStateChanged(this);
                }
            } else if (activeNow != pendingActiveStateChange) {
                pendingActiveStateChange = null;
            }
        }
    }

    private void removeInternal(LifecycleBoundObserver observer) {
        mObservers.remove(observer);
    }

    @SuppressWarnings("unchecked")
    private void onActiveStateChanged(LifecycleBoundObserver observer) {
        if (mObservers.isLocked()) {
            mPendingActiveChanges = true;
            mObservers.invokeSyncOnUnlock();
            return;
        }
        mUpdateActiveCount.run(observer);
    }

    static boolean isActiveState(@Lifecycle.State int state) {
        return state >= STARTED;
    }
}
