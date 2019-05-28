/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.activity;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Dispatcher that can be used to register {@link OnBackPressedCallback} instances for handling
 * the {@link ComponentActivity#onBackPressed()} callback via composition.
 * <pre>
 * public class FormEntryFragment extends Fragment {
 *     {@literal @}Override
 *     public void onAttach({@literal @}NonNull Context context) {
 *         super.onAttach(context);
 *         OnBackPressedCallback callback = new OnBackPressedCallback(
 *             true // default to enabled
 *         ) {
 *             {@literal @}Override
 *             public void handleOnBackPressed() {
 *                 showAreYouSureDialog();
 *             }
 *         };
 *         requireActivity().getOnBackPressedDispatcher().addCallback(
 *             this, // LifecycleOwner
 *             callback);
 *     }
 * }
 * </pre>
 */
public final class OnBackPressedDispatcher {

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ArrayDeque<OnBackPressedCallback> mOnBackPressedCallbacks = new ArrayDeque<>();

    OnBackPressedDispatcher() {
    }

    /**
     * Add a new {@link OnBackPressedCallback}. Callbacks are invoked in the reverse order in which
     * they are added, so this newly added {@link OnBackPressedCallback} will be the first
     * callback to receive a callback if {@link #onBackPressed()} is called.
     * <p>
     * This method is <strong>not</strong> {@link Lifecycle} aware - if you'd like to ensure that
     * you only get callbacks when at least {@link Lifecycle.State#STARTED started}, use
     * {@link #addCallback(LifecycleOwner, OnBackPressedCallback)}. It is expected that you
     * call {@link OnBackPressedCallback#remove()} to manually remove your callback.
     *
     * @param onBackPressedCallback The callback to add
     *
     * @see #onBackPressed()
     */
    @MainThread
    public void addCallback(@NonNull OnBackPressedCallback onBackPressedCallback) {
        addCancellableCallback(onBackPressedCallback);
    }

    /**
     * Internal implementation of {@link #addCallback(OnBackPressedCallback)} that gives
     * access to the {@link Cancellable} that specifically removes this callback from
     * the dispatcher without relying on {@link OnBackPressedCallback#remove()} which
     * is what external developers should be using.
     *
     * @param onBackPressedCallback The callback to add
     * @return a {@link Cancellable} which can be used to {@link Cancellable#cancel() cancel}
     * the callback and remove it from the set of OnBackPressedCallbacks.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @MainThread
    @NonNull
    Cancellable addCancellableCallback(@NonNull OnBackPressedCallback onBackPressedCallback) {
        mOnBackPressedCallbacks.add(onBackPressedCallback);
        OnBackPressedCancellable cancellable = new OnBackPressedCancellable(onBackPressedCallback);
        onBackPressedCallback.addCancellable(cancellable);
        return cancellable;
    }

    /**
     * Receive callbacks to a new {@link OnBackPressedCallback} when the given
     * {@link LifecycleOwner} is at least {@link Lifecycle.State#STARTED started}.
     * <p>
     * This will automatically call {@link #addCallback(OnBackPressedCallback)} and
     * remove the callback as the lifecycle state changes.
     * As a corollary, if your lifecycle is already at least
     * {@link Lifecycle.State#STARTED started}, calling this method will result in an immediate
     * call to {@link #addCallback(OnBackPressedCallback)}.
     * <p>
     * When the {@link LifecycleOwner} is {@link Lifecycle.State#DESTROYED destroyed}, it will
     * automatically be removed from the list of callbacks. The only time you would need to
     * manually call {@link OnBackPressedCallback#remove()} is if
     * you'd like to remove the callback prior to destruction of the associated lifecycle.
     *
     * <p>
     * If the Lifecycle is already {@link Lifecycle.State#DESTROYED destroyed}
     * when this method is called, the callback will not be added.
     *
     * @param owner The LifecycleOwner which controls when the callback should be invoked
     * @param onBackPressedCallback The callback to add
     *
     * @see #onBackPressed()
     */
    @MainThread
    public void addCallback(@NonNull LifecycleOwner owner,
            @NonNull OnBackPressedCallback onBackPressedCallback) {
        Lifecycle lifecycle = owner.getLifecycle();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        onBackPressedCallback.addCancellable(
                new LifecycleOnBackPressedCancellable(lifecycle, onBackPressedCallback));
    }

    /**
     * Checks if there is at least one {@link OnBackPressedCallback#isEnabled enabled}
     * callback registered with this dispatcher.
     *
     * @return True if there is at least one enabled callback.
     */
    @MainThread
    public boolean hasEnabledCallbacks() {
        Iterator<OnBackPressedCallback> iterator =
                mOnBackPressedCallbacks.descendingIterator();
        while (iterator.hasNext()) {
            if (iterator.next().isEnabled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Trigger a call to the currently added {@link OnBackPressedCallback callbacks} in reverse
     * order in which they were added. Only if the most recently added callback is not
     * {@link OnBackPressedCallback#isEnabled() enabled}
     * will any previously added callback be called.
     * <p>
     * It is strongly recommended to call {@link #hasEnabledCallbacks()} prior to calling
     * this method to determine if there are any enabled callbacks that will be triggered
     * by this method as calling this method.
     */
    @MainThread
    public void onBackPressed() {
        Iterator<OnBackPressedCallback> iterator =
                mOnBackPressedCallbacks.descendingIterator();
        while (iterator.hasNext()) {
            OnBackPressedCallback callback = iterator.next();
            if (callback.isEnabled()) {
                callback.handleOnBackPressed();
                return;
            }
        }
    }

    private class OnBackPressedCancellable implements Cancellable {
        private final OnBackPressedCallback mOnBackPressedCallback;
        OnBackPressedCancellable(OnBackPressedCallback onBackPressedCallback) {
            mOnBackPressedCallback = onBackPressedCallback;
        }

        @Override
        public void cancel() {
            mOnBackPressedCallbacks.remove(mOnBackPressedCallback);
            mOnBackPressedCallback.removeCancellable(this);
        }
    }

    private class LifecycleOnBackPressedCancellable implements GenericLifecycleObserver,
            Cancellable {
        private final Lifecycle mLifecycle;
        private final OnBackPressedCallback mOnBackPressedCallback;

        @Nullable
        private Cancellable mCurrentCancellable;

        LifecycleOnBackPressedCancellable(@NonNull Lifecycle lifecycle,
                @NonNull OnBackPressedCallback onBackPressedCallback) {
            mLifecycle = lifecycle;
            mOnBackPressedCallback = onBackPressedCallback;
            lifecycle.addObserver(this);
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source,
                @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_START) {
                mCurrentCancellable = addCancellableCallback(mOnBackPressedCallback);
            } else if (event == Lifecycle.Event.ON_STOP) {
                // Should always be non-null
                if (mCurrentCancellable != null) {
                    mCurrentCancellable.cancel();
                }
            } else if (event == Lifecycle.Event.ON_DESTROY) {
                cancel();
            }
        }

        @Override
        public void cancel() {
            mLifecycle.removeObserver(this);
            mOnBackPressedCallback.removeCancellable(this);
            if (mCurrentCancellable != null) {
                mCurrentCancellable.cancel();
                mCurrentCancellable = null;
            }
        }
    }
}
