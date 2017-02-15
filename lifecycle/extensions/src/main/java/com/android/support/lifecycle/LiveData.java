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

import com.android.support.apptoolkit.internal.ObserverSet;
import com.android.support.executors.AppToolkitTaskExecutor;

/**
 * LiveData is a data holder class that can be observed within a given lifecycle.
 * This means that an {@link Observer} can be added in a pair with a {@link LifecycleProvider}, and
 * this observer will be notified about modifications of the wrapped data only if the paired
 * LifecycleProvider is in active state. LifecycleProvider is considered as active, if its state is
 * {@link Lifecycle#STARTED} or {@link Lifecycle#RESUMED}. An observer added without a
 * LifecycleProvider is considered as always active and thus will be always notified about
 * modifications. For those observers, you should manually call {@link #removeObserver(Observer)}.
 *
 * <p> An observer added with a Lifecycle will be automatically removed if the corresponding
 * Lifecycle moves to {@link Lifecycle#DESTROYED} state. This is especially useful for activities
 * and fragments where they can safely observe LiveData and not worry about leaks: they will be
 * instantly unsubscribed when they are destroyed.
 *
 * <p>
 * In addition, LiveData has {@link LiveData#onActive()} and {@link LiveData#onInactive()} methods
 * to get notified when number of active {@link Observer}s change between 0 and 1.
 * This allows LiveData to release any heavy resources when it does not have any Observers that
 * are actively observing.
 * <p>
 * This class is designed to hold individual data fields of {@link ViewModel},
 * but can also be used for sharing data between different modules in your application
 * in a decoupled fashion.
 *
 * @param <T> The type of data hold by this instance
 * @see ViewModel
 */
@SuppressWarnings({"WeakerAccess", "unused"})
// TODO the usage of ObserverSet needs to be cleaned. Maybe we should simplify the rules.
// Thread checks are too strict right now, we may consider automatically moving them to main
// thread.
public class LiveData<T> {
    private final Object mDataLock = new Object();
    private static final int START_VERSION = -1;
    private static final Object NOT_SET = new Object();
    private boolean mPendingActiveChanges = false;

    private static final LifecycleProvider ALWAYS_ON = new LifecycleProvider() {

        private LifecycleRegistry mRegistry = init();

        private LifecycleRegistry init() {
            LifecycleRegistry registry = new LifecycleRegistry(this);
            registry.handleLifecycleEvent(Lifecycle.ON_CREATE);
            registry.handleLifecycleEvent(Lifecycle.ON_START);
            registry.handleLifecycleEvent(Lifecycle.ON_RESUME);
            return registry;
        }

        @Override
        public Lifecycle getLifecycle() {
            return mRegistry;
        }
    };

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
    // when setData is called, we set the pending data and actual data swap happens on the main
    // thread
    private volatile Object mPendingData = NOT_SET;
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
        assertMainThread("observe");
        if (provider.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        final LifecycleBoundObserver wrapper = new LifecycleBoundObserver(provider, observer);
        mObservers.add(wrapper);
    }

    /**
     * Adds the given observer to the observers list. This call is similar to
     * {@link LiveData#observe(LifecycleProvider, Observer)} with a LifecycleProvider, which
     * is always active. This means that the given observer will receive all events and will never
     * be automatically removed. You should manually call {@link #removeObserver(Observer)} to stop
     * observing this LiveData.
     * While LiveData has one of such observers, it will be considered
     * as active.
     * <p>
     * If the observer was already added with a provider to this LiveData, LiveData throws an
     * {@link IllegalArgumentException}.

     * @param observer The observer that will receive the events
     */
    @MainThread
    public void observe(Observer<T> observer) {
        observe(ALWAYS_ON, observer);
    }

    /**
     * Removes the given observer from the observers list.
     *
     * @param observer The Observer to receive events.
     */
    @MainThread
    public void removeObserver(final Observer<T> observer) {
        assertMainThread("removeObserver");
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
        assertMainThread("removeObservers");
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
     * <p>
     * If this method is called on a background thread, the call will be forwarded to the main
     * thread so calling {@link #getValue()} right after calling {@code setValue} may
     * not return the value that was set.
     *
     * @param value The new value
     */
    public void setValue(final T value) {
        // we keep it in pending data so that last set data wins (e.g. we won't be in a case where
        // data is set on the main thread at a later time is overridden by data that was set on a
        // background thread.
        synchronized (mDataLock) {
            mPendingData = value;
        }
        AppToolkitTaskExecutor.getInstance().executeOnMainThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mDataLock) {
                    if (mPendingData != NOT_SET) {
                        mVersion++;
                        mData = mPendingData;
                        mPendingData = NOT_SET;
                    }
                }
                mObservers.forEach(mDispatchCallback);
            }
        });
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
        // we do not return pending data here to be able to serve a consistent view to the main
        // thread.
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
     * <p>
     * If called on a background thread, the value might be unreliable.
     *
     * @return The number of observers
     */
    public int getObserverCount() {
        return mObserverCount;
    }

    /**
     * Returns the number of active observers.
     * <p>
     * If called on a background thread, the value might be unreliable.
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

    private void assertMainThread(String methodName) {
        if (!AppToolkitTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread. You can easily move the call to main thread using"
                    + " AppToolkitTaskExecutor.");
        }
    }
}
